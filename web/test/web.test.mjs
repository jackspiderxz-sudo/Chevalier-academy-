import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { buildWeb } from '../build.mjs';
import { createWebServer } from '../server.mjs';
import { getStrings, getAdminPpmStrings, PARENT_MODULES, ADMIN_PPM_MODULES } from '../js/i18n.js';
import { ChevalierStore, SCHOOL_POST_TYPES, SCHOOL_POST_STATUS, MAX_POST_PHOTOS, HIGHLIGHT_TTL_MILLIS } from '../js/store.js';
import {
  parseYouTubeUrl,
  isValidYouTubeUrl,
  isValidYouTubeShortsUrl,
  buildYouTubeEmbedSpec,
  computeScaledDimensions,
  estimateDataUrlBytes,
  compressImageFileForStorage
} from '../js/mediaUtils.js';
import { renderHomeView, renderAcademicsHubView, renderParentDashboardView, renderParentModuleDetailView } from '../js/publicViews.js';
import { renderAdminDashboardView, renderAdminPpmModuleView } from '../js/adminViews.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(__dirname, '..');

test('Web build reuses Android brand drawables and fonts into web/dist/', () => {
  const result = buildWeb();
  assert.equal(result.copiedAssets, 5);
  assert.equal(result.copiedScripts, 7);

  const requiredFiles = [
    'index.html',
    '404.html',
    'styles.css',
    'manifest.webmanifest',
    'build-manifest.json',
    'assets/chevalier_emblem.png',
    'assets/chevalier_launcher_logo.png',
    'assets/chevalier_splash_artwork.png',
    'assets/playfair_display.ttf',
    'assets/plus_jakarta_sans.ttf',
    'js/app.js',
    'js/mediaUtils.js',
    'js/publicViews.js',
    'js/adminViews.js'
  ];
  for (const rel of requiredFiles) {
    assert.ok(fs.existsSync(path.join(result.distDir, rel)), `Expected dist file: ${rel}`);
  }
  const prodHtml = fs.readFileSync(path.join(result.distDir, 'index.html'), 'utf8');
  assert.match(prodHtml, /rel="preload" href="\.\/assets\/playfair_display\.ttf"/);
  assert.match(prodHtml, /rel="preload" href="\.\/assets\/plus_jakarta_sans\.ttf"/);
  assert.match(prodHtml, /href="\.\/styles\.css\?v=[a-f0-9]{10}"/);
  assert.match(prodHtml, /src="\.\/js\/app\.js\?v=[a-f0-9]{10}"/);
});

test('Responsive CSS defines Mobile, Tablet, Laptop, and Desktop breakpoints & Dark theme', () => {
  const css = fs.readFileSync(path.join(webRoot, 'styles.css'), 'utf8');
  assert.match(css, /@media \(min-width: 600px\) and \(max-width: 1023px\)/);
  assert.match(css, /@media \(min-width: 1024px\)/);
  assert.match(css, /@media \(min-width: 1400px\)/);
  assert.match(css, /:root\[data-theme="dark"\]/);
  assert.match(css, /#8D2029/);
  assert.match(css, /#EABF16/);
  assert.match(css, /#087A45/);
  assert.match(css, /#FFF9E8/);
});

test('Localization supports EN, TA, HI while keeping official school branding verbatim', () => {
  for (const lang of ['en', 'ta', 'hi']) {
    const s = getStrings(lang);
    assert.equal(s.officialSchoolName, 'CHEVALIER ACADEMY MATRIC HIGHER SECONDARY SCHOOL');
    assert.equal(s.officialMotto, 'Learn to Live & Love');
    assert.equal(s.officialAddress, 'Siluvathur Road, Dindigul - 624 306');
    assert.equal(s.poweredByAttribution, 'POWERED BY JK LABS');

    const ppm = getAdminPpmStrings(lang);
    assert.equal(Object.keys(ppm.modules).length, 11);
  }
  assert.equal(PARENT_MODULES.length, 11);
  assert.equal(ADMIN_PPM_MODULES.length, 11);
});

test('Web Store persists CMS edits, PIN authentication, and JSON backup roundtrip', async () => {
  const store = new ChevalierStore();
  assert.equal(store.hasAdminPin(), false);
  await store.setupAdminPin('2468', 'Principal Admin');
  assert.equal(store.hasAdminPin(), true);
  assert.equal(store.isAdminAuthenticated, true);

  store.logoutAdmin();
  assert.equal(store.isAdminAuthenticated, false);
  const bad = await store.verifyAdminPin('0000');
  assert.equal(bad.ok, false);
  const good = await store.verifyAdminPin('2468');
  assert.equal(good.ok, true);

  store.updateProfile({ phone: '+91 451 2400000', welcomeIntroduction: 'Welcome to our campus.' });
  store.addAnnouncement({ title: 'Term Exam Notice', message: 'Timetable published.' });

  const exported = store.exportBackupJson();
  assert.ok(!exported.includes('pinHash'), 'Backup must not leak salted PIN hash');

  const store2 = new ChevalierStore();
  store2.importBackupJson(exported);
  assert.equal(store2.getState().profile.phone, '+91 451 2400000');
  assert.equal(store2.getState().announcements.length, 1);
});

test('Public Home, Parent Dashboard (11 modules), and Admin Parent Portal Management (11 submenus) render cleanly', () => {
  const store = new ChevalierStore();
  const state = store.getState();
  const s = getStrings('en');

  const homeHtml = renderHomeView(state, s, { heroSlideIndex: 0 });
  assert.match(homeHtml, /home_section_hero/);
  assert.match(homeHtml, /home_section_welcome/);
  assert.doesNotMatch(homeHtml, /home_section_quick_access/);
  assert.match(homeHtml, /home_section_updates/);
  assert.match(homeHtml, /home_section_gallery/);
  assert.doesNotMatch(homeHtml, /home_section_contact_social/);

  const academicsHubHtml = renderAcademicsHubView(state, s);
  assert.match(academicsHubHtml, /academics_hub_screen_list/);
  for (const cardId of [
    'academics_card_academics',
    'academics_card_courses',
    'academics_card_about',
    'academics_card_facilities',
    'academics_card_transport',
    'academics_card_news_announcements',
    'academics_card_gallery'
  ]) {
    assert.match(academicsHubHtml, new RegExp(cardId));
  }

  const parentHtml = renderParentDashboardView(state, s, { parentSearchOpen: false, parentSearchQuery: '' });
  assert.match(parentHtml, /parent_student_info_card/);
  assert.match(parentHtml, /Sample Preview/);
  for (const mod of PARENT_MODULES) {
    assert.match(parentHtml, new RegExp(`parent_module_card_${mod.key}`));
    const detailHtml = renderParentModuleDetailView(state, s, mod.key);
    assert.match(detailHtml, /parent_module_back_dashboard_btn/);
  }

  const adminHtml = renderAdminDashboardView(state, s, { ppmExpanded: true });
  assert.match(adminHtml, /admin_parent_portal_mgmt_header/);
  for (const mod of ADMIN_PPM_MODULES) {
    assert.match(adminHtml, new RegExp(`ppm_submenu_card_${mod.key}`));
    const subHtml = renderAdminPpmModuleView(state, s, { ppmScopes: {} }, mod.key);
    assert.match(subHtml, new RegExp(`ppm_screen_${mod.key}`));
    assert.match(subHtml, /ppm_back_to_dashboard_btn/);
  }
});

test('Local HTTP server serves index.html and reused Android emblem PNG with HTTP 200', async () => {
  const server = createWebServer({ useDist: true });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const { port } = server.address();
  try {
    const htmlRes = await fetch(`http://127.0.0.1:${port}/`);
    assert.equal(htmlRes.status, 200);
    const body = await htmlRes.text();
    assert.match(body, /Chevalier Academy/);

    const imgRes = await fetch(`http://127.0.0.1:${port}/assets/chevalier_emblem.png`);
    assert.equal(imgRes.status, 200);
    assert.equal(imgRes.headers.get('content-type'), 'image/png');
  } finally {
    server.close();
  }
});

test('Web YouTube URL parser supports watch, youtu.be, shorts, embed and image compression helpers work safely', async () => {
  const id = 'dQw4w9WgXcQ';
  const validUrls = [
    [`https://www.youtube.com/watch?v=${id}&t=15s`, false],
    [`https://youtu.be/${id}?si=xyz`, false],
    [`https://www.youtube.com/shorts/${id}`, true],
    [`https://www.youtube.com/embed/${id}`, false]
  ];

  for (const [url, expectShort] of validUrls) {
    const parsed = parseYouTubeUrl(url);
    assert.ok(parsed, `Expected valid parse for ${url}`);
    assert.equal(parsed.videoId, id);
    assert.equal(parsed.isShort, expectShort);
    assert.equal(parsed.thumbnailUrl, `https://img.youtube.com/vi/${id}/hqdefault.jpg`);
    assert.match(parsed.clickToPlayEmbedUrl, /youtube-nocookie\.com\/embed\/dQw4w9WgXcQ.*autoplay=1/);
    assert.ok(parsed.externalFallbackUrl.includes(id));
    assert.equal(isValidYouTubeUrl(url), true);

    const spec = buildYouTubeEmbedSpec(url);
    assert.equal(spec.videoId, id);
    assert.equal(spec.externalFallbackUrl, parsed.externalFallbackUrl);
  }

  assert.equal(isValidYouTubeUrl('https://evil-youtube.com/watch?v=dQw4w9WgXcQ'), false);
  assert.equal(isValidYouTubeUrl('https://www.youtube.com/watch?v=bad'), false);
  assert.equal(isValidYouTubeShortsUrl(`https://www.youtube.com/watch?v=${id}`), false);
  assert.equal(isValidYouTubeShortsUrl(`https://www.youtube.com/shorts/${id}`), true);

  // Image scaling & byte estimation helpers
  const scaled = computeScaledDimensions(2560, 1440, 1280);
  assert.deepEqual(scaled, { width: 1280, height: 720, scaled: true });
  const unscaled = computeScaledDimensions(800, 600, 1280);
  assert.deepEqual(unscaled, { width: 800, height: 600, scaled: false });
  assert.equal(estimateDataUrlBytes('data:image/jpeg;base64,AAAA'), 3);

  // Node environment without DOM canvas preserves original file payload via FileReader fallback
  const origFileReader = globalThis.FileReader;
  globalThis.FileReader = class {
    readAsDataURL() {
      this.result = 'data:image/jpeg;base64,AAAA';
      this.onload();
    }
  };
  try {
    const out = await compressImageFileForStorage({ type: 'image/jpeg' });
    assert.equal(out.dataUrl, 'data:image/jpeg;base64,AAAA');
    assert.equal(out.compressed, false);
  } finally {
    globalThis.FileReader = origFileReader;
  }
});

test('Web Store supports School Posts (3 types, max 10 photos, status/pinning) and 24-hour Highlights expiry', () => {
  const store = new ChevalierStore();

  // Max 10 photos enforcement
  assert.throws(() => {
    store.saveSchoolPost({
      postType: SCHOOL_POST_TYPES.PHOTO,
      title: 'Empty',
      photoUrls: []
    });
  });
  assert.throws(() => {
    store.saveSchoolPost({
      postType: SCHOOL_POST_TYPES.PHOTO,
      title: 'Eleven',
      photoUrls: Array.from({ length: MAX_POST_PHOTOS + 1 }, (_, i) => `data:image/jpeg;base64,img${i}`)
    });
  });

  const photoPost = store.saveSchoolPost(
    {
      postType: SCHOOL_POST_TYPES.PHOTO,
      category: 'Sports',
      title: 'Sports Meet Photos',
      photoUrls: Array.from({ length: MAX_POST_PHOTOS }, (_, i) => `data:image/jpeg;base64,img${i}`),
      status: SCHOOL_POST_STATUS.PUBLISHED
    },
    20_000
  );
  const ytPost = store.saveSchoolPost(
    {
      postType: SCHOOL_POST_TYPES.YOUTUBE_DIRECT,
      category: 'Events',
      title: 'Annual Day Video',
      youtubeUrl: 'https://youtu.be/dQw4w9WgXcQ',
      status: SCHOOL_POST_STATUS.PUBLISHED
    },
    10_000
  );
  const customPost = store.saveSchoolPost(
    {
      postType: SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO,
      category: 'Academics',
      title: 'Lab Walkthrough',
      youtubeUrl: 'https://www.youtube.com/watch?v=M7lc1UVf-VE',
      customThumbnailUrl: 'data:image/jpeg;base64,thumb',
      status: SCHOOL_POST_STATUS.UNPUBLISHED
    },
    5_000
  );

  assert.deepEqual(store.getPublishedSchoolPosts().map((p) => p.id), [photoPost.id, ytPost.id]);

  store.setSchoolPostStatus(customPost.id, SCHOOL_POST_STATUS.PUBLISHED, 8_000);
  store.setSchoolPostFeatured(customPost.id, true, 9_000);
  assert.deepEqual(store.getPublishedSchoolPosts().map((p) => p.id), [customPost.id, photoPost.id, ytPost.id]);

  store.setSchoolPostStatus(ytPost.id, SCHOOL_POST_STATUS.ARCHIVED, 25_000);
  assert.deepEqual(store.getPublishedSchoolPosts().map((p) => p.id), [customPost.id, photoPost.id]);

  // 24-hour Shorts Highlight
  assert.throws(() => {
    store.saveSchoolHighlight({ youtubeShortsUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' });
  });
  const hl = store.saveSchoolHighlight(
    { title: 'Assembly Short', youtubeShortsUrl: 'https://www.youtube.com/shorts/dQw4w9WgXcQ' },
    100_000
  );
  assert.equal(store.getActiveHighlights(100_000 + HIGHLIGHT_TTL_MILLIS - 1).length, 1);
  assert.equal(store.getActiveHighlights(100_000 + HIGHLIGHT_TTL_MILLIS).length, 0);
  assert.equal(store.getState().schoolHighlights.length, 1);
  assert.equal(store.getState().schoolHighlights[0].id, hl.id);

  // Backup roundtrip preserves schoolPosts and schoolHighlights
  const json = store.exportBackupJson();
  const restored = new ChevalierStore();
  restored.importBackupJson(json);
  assert.equal(restored.getState().schoolPosts.length, 3);
  assert.equal(restored.getState().schoolHighlights.length, 1);
});

test('Two-Way Live Sync (Android App <-> Firebase Firestore <-> Website) synchronizes all 13 modules', async () => {
  // In-memory Firestore v1 REST mock simulating the live Firestore database & rules queries
  const cloudDb = new Map(); // key: `${collection}/${remoteId}` -> { name, fields }

  const mockFetch = async (url, options = {}) => {
    const method = (options.method || 'GET').toUpperCase();
    const u = new URL(url);

    if (method === 'PATCH') {
      const match = u.pathname.match(/\/schools\/chevalier_academy_dindigul\/([^/]+)\/([^/]+)$/);
      assert.ok(match, `Unexpected PATCH path: ${u.pathname}`);
      const [, collection, remoteId] = match;
      const body = JSON.parse(options.body);
      const docObj = {
        name: `projects/gen-lang-client-0028096876/databases/ai-studio-android-chevalie-d85b2a3e-45dc-4e49-9c97-6ab207063b96/documents/schools/chevalier_academy_dindigul/${collection}/${remoteId}`,
        fields: body.fields
      };
      cloudDb.set(`${collection}/${remoteId}`, docObj);
      return { ok: true, status: 200, json: async () => docObj };
    }

    if (method === 'DELETE') {
      const match = u.pathname.match(/\/schools\/chevalier_academy_dindigul\/([^/]+)\/([^/]+)$/);
      assert.ok(match, `Unexpected DELETE path: ${u.pathname}`);
      const [, collection, remoteId] = match;
      cloudDb.delete(`${collection}/${remoteId}`);
      return { ok: true, status: 200, json: async () => ({}) };
    }

    if (method === 'GET') {
      const match = u.pathname.match(/\/schools\/chevalier_academy_dindigul\/([^/]+)\/([^/]+)$/);
      if (match) {
        const [, collection, remoteId] = match;
        const docObj = cloudDb.get(`${collection}/${remoteId}`);
        if (docObj) return { ok: true, status: 200, json: async () => docObj };
      }
      return { ok: false, status: 404, json: async () => ({}) };
    }

    if (method === 'POST' && u.pathname.endsWith(':runQuery')) {
      const body = JSON.parse(options.body);
      const collectionId = body.structuredQuery.from[0].collectionId;
      if (collectionId !== 'school_profile') {
        const filters = body.structuredQuery.where?.compositeFilter?.filters || [];
        const hasUnaryNullFilter = filters.some(
          (f) => f.unaryFilter?.field?.fieldPath === 'deletedAtMillis' && f.unaryFilter?.op === 'IS_NULL'
        );
        assert.ok(hasUnaryNullFilter, `Expected unaryFilter IS_NULL on deletedAtMillis for ${collectionId}`);
      }
      const results = [];
      for (const [key, docObj] of cloudDb.entries()) {
        if (key.startsWith(`${collectionId}/`)) {
          results.push({ document: docObj });
        }
      }
      return { ok: true, status: 200, json: async () => results };
    }

    return { ok: false, status: 404, json: async () => ({}) };
  };

  // 1. Website -> Firebase -> Android App
  const webAdminStore = new ChevalierStore();
  await webAdminStore.enableCloudSync({ fetchImpl: mockFetch, pollIntervalMs: 0 });

  webAdminStore.updateProfile({
    phone: '+91 451 2422200',
    email: 'office@chevalieracademy.edu.in',
    principalName: 'Rev. Fr. Principal MSC',
    welcomeIntroduction: 'Live synced welcome from Website Admin.'
  });
  webAdminStore.addHeroPhoto({ imageUrl: 'data:image/jpeg;base64,HERO1', caption: 'Main Block' });
  webAdminStore.addAcademicProgram({ title: 'STEM Robotics', category: 'Middle School', description: 'Hands-on robotics lab' });
  webAdminStore.updateCourse(1, {
    title: 'Bio-Maths Stream',
    medium: 'English Medium',
    subjects: 'Physics, Chemistry, Biology, Mathematics',
    verified: true
  });
  webAdminStore.addFacility({ name: 'Smart Digital Library', category: 'Academics', description: '15,000+ volumes' });
  webAdminStore.addTransportRoute({ routeNumber: 'R-07', routeName: 'Palani Road Express', stops: 'Palani Bypass, Collectorate', timings: '7:40 AM' });
  webAdminStore.addNews({ title: 'District Science Fair Champions', summary: 'First place in district exhibition.', isFeatured: true });
  webAdminStore.addAnnouncement({ title: 'PTA Meeting Saturday', message: 'All parents invited at 10 AM.', priorityLabel: 'Urgent' });
  webAdminStore.addGalleryMedia({ album: 'Annual Sports Meet', category: 'Sports', caption: 'March Past', imageUrl: 'data:image/jpeg;base64,GAL1' });
  webAdminStore.updateSocialLink(3, { handle: '@ChevalierAcademyOfficial', url: 'https://youtube.com/@ChevalierAcademyOfficial', active: true });
  const createdPost = webAdminStore.saveSchoolPost({
    postType: SCHOOL_POST_TYPES.PHOTO,
    category: 'Events',
    title: 'Annual Cultural Fest Photos',
    description: 'Multi-image offloaded photo post',
    photoUrls: ['data:image/jpeg;base64,PHOTO_A', 'data:image/jpeg;base64,PHOTO_B'],
    status: SCHOOL_POST_STATUS.PUBLISHED,
    isFeatured: true
  });
  webAdminStore.saveSchoolHighlight({
    title: 'Morning Assembly Short',
    youtubeShortsUrl: 'https://www.youtube.com/shorts/dQw4w9WgXcQ',
    status: SCHOOL_POST_STATUS.PUBLISHED
  });

  await webAdminStore.flushCloudSync();

  // Verify PIN gate and all 13 collections were written to Firestore, with multi-photo offload keeping school_posts lightweight
  assert.ok(cloudDb.has('management_accounts/pin_session_v1_verified'));
  assert.ok(cloudDb.has('school_profile/school_profile_1'));
  const postDocInCloud = cloudDb.get(`school_posts/${createdPost.remoteId}`);
  assert.ok(postDocInCloud, 'Expected school_posts document in cloud');
  const cloudPhotoUris = postDocInCloud.fields.photoUris.arrayValue.values.map((v) => v.stringValue);
  assert.ok(cloudPhotoUris.every((u) => u.startsWith('firebase-media://post_photo_')));

  // 2. Android / Secondary Client pulls from Firebase Firestore -> receives all Website updates automatically
  const peerClientStore = new ChevalierStore();
  let listenerNotified = 0;
  peerClientStore.subscribe(() => {
    listenerNotified += 1;
  });
  await peerClientStore.enableCloudSync({ fetchImpl: mockFetch, pollIntervalMs: 0 });

  const peerState = peerClientStore.getState();
  assert.ok(listenerNotified >= 1, 'Expected live subscriber notification on inbound cloud sync');
  assert.equal(peerState.profile.phone, '+91 451 2422200');
  assert.equal(peerState.profile.principalName, 'Rev. Fr. Principal MSC');
  assert.equal(peerState.heroPhotos.length, 1);
  assert.equal(peerState.heroPhotos[0].caption, 'Main Block');
  assert.equal(peerState.academics.length, 1);
  assert.equal(peerState.academics[0].title, 'STEM Robotics');
  assert.equal(peerState.courses.find((c) => c.groupCode === 'Group I').title, 'Bio-Maths Stream');
  assert.equal(peerState.facilities[0].name, 'Smart Digital Library');
  assert.equal(peerState.transportRoutes[0].routeNumber, 'R-07');
  assert.equal(peerState.news[0].title, 'District Science Fair Champions');
  assert.equal(peerState.announcements[0].title, 'PTA Meeting Saturday');
  assert.equal(peerState.galleryMedia.length, 1, 'Offloaded post_photo_* docs must not leak into public Gallery');
  assert.equal(peerState.galleryMedia[0].caption, 'March Past');
  assert.equal(peerState.socialLinks.find((s) => s.platformName === 'YouTube').url, 'https://youtube.com/@ChevalierAcademyOfficial');
  assert.equal(peerClientStore.getPublishedSchoolPosts().length, 1);
  assert.equal(peerClientStore.getPublishedSchoolPosts()[0].title, 'Annual Cultural Fest Photos');
  assert.deepEqual(peerClientStore.getPublishedSchoolPosts()[0].photoUrls, [
    'data:image/jpeg;base64,PHOTO_A',
    'data:image/jpeg;base64,PHOTO_B'
  ]);
  assert.equal(peerClientStore.getActiveHighlights().length, 1);
  assert.equal(peerClientStore.getActiveHighlights()[0].title, 'Morning Assembly Short');

  // 3. Deletion sync: deleting post on Admin removes it from Firestore and peer client on next pull
  webAdminStore.deleteSchoolPost(createdPost.id);
  await webAdminStore.flushCloudSync();
  await peerClientStore.pullFromCloud();
  assert.equal(peerClientStore.getPublishedSchoolPosts().length, 0);
});


