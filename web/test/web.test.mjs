import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { buildWeb } from '../build.mjs';
import { createWebServer } from '../server.mjs';
import { getStrings, getAdminPpmStrings, PARENT_MODULES, ADMIN_PPM_MODULES, ACADEMICS_V2_CATEGORIES, ACADEMY_12_CARDS } from '../js/i18n.js';
import {
  ChevalierStore,
  SCHOOL_POST_TYPES,
  SCHOOL_POST_STATUS,
  MAX_POST_PHOTOS,
  HIGHLIGHT_TTL_MILLIS,
  MANAGEMENT_ROLES,
  PERMISSION_ACTIONS,
  MANAGEMENT_SECTIONS,
  ADMIN_DASHBOARD_CARDS,
  ADMIN_INACTIVITY_TIMEOUT_MILLIS,
  ACADEMICS_V2_RECORD_STATUS,
  SCHOOL_OWNER_SECTIONS,
  PinSecurityManager,
  buildDefaultPermissionsForRole
} from '../js/store.js';
import {
  parseYouTubeUrl,
  isValidYouTubeUrl,
  isValidYouTubeShortsUrl,
  buildYouTubeEmbedSpec,
  computeScaledDimensions,
  estimateDataUrlBytes,
  compressImageFileForStorage
} from '../js/mediaUtils.js';
import {
  renderHomeView,
  renderBookmarksView,
  renderAcademicsHubView,
  renderAcademyCategoryDetailView,
  renderParentDashboardView,
  renderParentModuleDetailView,
  renderAboutView,
  renderContactView,
  renderCoursesView,
  renderFacilitiesView,
  renderTransportView,
  renderUpdatesView,
  renderGalleryView
} from '../js/publicViews.js';
import {
  renderAdminGateView,
  renderAdminDashboardView,
  renderAdminHubView,
  renderAdminSearchView,
  renderAdminNotificationsView,
  renderAdminStaffManagementView,
  renderAdminSectionView,
  renderAdminAccessDeniedView,
  renderAdminPpmModuleView,
  renderAdminV2AcademicsHubView,
  renderAdminV2CategoryManagementView,
  renderAdminV2PreviewAndPublishView,
  renderSchoolOwnerControlCenterView
} from '../js/adminViews.js';

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
  assert.equal(PARENT_MODULES.length, 12);
  assert.equal(ADMIN_PPM_MODULES.length, 11);
  assert.equal(ACADEMICS_V2_CATEGORIES.length, 13);
  assert.equal(ACADEMY_12_CARDS.length, 12);
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

test('Public Home, Highlights, Social Feed, Bookmarks, 13-Category Academy V2, Parent Dashboard (12 modules + Student Feed), and Admin PPM render cleanly', () => {
  const store = new ChevalierStore();
  const s = getStrings('en');

  // Empty state rendering
  const emptyHomeHtml = renderHomeView(store.getState(), s, { heroSlideIndex: 0 });
  assert.match(emptyHomeHtml, /home_section_hero/);
  assert.match(emptyHomeHtml, /home_section_highlights/);
  assert.match(emptyHomeHtml, /home_highlights_empty_card/);
  assert.match(emptyHomeHtml, /home_section_social_feed/);
  assert.match(emptyHomeHtml, /home_social_feed_empty_card/);
  assert.match(emptyHomeHtml, /home_section_welcome/);
  assert.match(emptyHomeHtml, /home_section_updates/);
  assert.match(emptyHomeHtml, /home_section_gallery/);

  // Empty Bookmarks screen
  const emptyBookmarksHtml = renderBookmarksView(store.getState(), s, {});
  assert.match(emptyBookmarksHtml, /bookmarks_empty_state/);
  assert.match(emptyBookmarksHtml, /bookmarks_explore_home_btn/);

  // Populate Highlights + Social Feed Posts
  const hl = store.saveSchoolHighlight({
    title: 'Campus Tour Short',
    youtubeShortsUrl: 'https://www.youtube.com/shorts/dQw4w9WgXcQ',
    status: SCHOOL_POST_STATUS.PUBLISHED
  });
  const post1 = store.saveSchoolPost({
    postType: SCHOOL_POST_TYPES.YOUTUBE_DIRECT,
    category: 'Academics',
    title: 'Physics Lab Demo',
    description: 'Optics experiment walkthrough',
    youtubeUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
    status: SCHOOL_POST_STATUS.PUBLISHED,
    isFeatured: true
  });
  const post2 = store.saveSchoolPost({
    postType: SCHOOL_POST_TYPES.PHOTO,
    category: 'Sports',
    title: 'Annual Sports Meet',
    description: 'Track & field highlights',
    photoUrls: ['data:image/jpeg;base64,P1', 'data:image/jpeg;base64,P2'],
    status: SCHOOL_POST_STATUS.PUBLISHED
  });

  // Like & Bookmark interactions
  assert.equal(store.isPostLiked(post1.id), false);
  assert.equal(store.toggleLikePost(post1.id), true);
  assert.equal(store.isPostLiked(post1.id), true);

  assert.equal(store.isPostBookmarked(post1.id), false);
  assert.equal(store.toggleBookmarkPost(post1.id), true);
  assert.equal(store.isPostBookmarked(post1.id), true);
  assert.equal(store.getBookmarkedPosts().length, 1);

  const populatedHomeHtml = renderHomeView(store.getState(), s, { heroSlideIndex: 0 });
  assert.match(populatedHomeHtml, new RegExp(`highlight_card_${hl.id}`));
  assert.match(populatedHomeHtml, /highlight_more_card/);
  assert.match(populatedHomeHtml, new RegExp(`social_post_card_${post1.id}`));
  assert.match(populatedHomeHtml, new RegExp(`social_post_card_${post2.id}`));
  assert.match(populatedHomeHtml, new RegExp(`post_like_btn_${post1.id}`));
  assert.match(populatedHomeHtml, new RegExp(`post_bookmark_btn_${post1.id}`));

  // Category filter on Home Social Feed
  const sportsFilteredHomeHtml = renderHomeView(store.getState(), s, { selectedFeedCategory: 'Sports' });
  assert.doesNotMatch(sportsFilteredHomeHtml, new RegExp(`social_post_card_${post1.id}`));
  assert.match(sportsFilteredHomeHtml, new RegExp(`social_post_card_${post2.id}`));

  // Populated Bookmarks screen
  const populatedBookmarksHtml = renderBookmarksView(store.getState(), s, {});
  assert.match(populatedBookmarksHtml, /bookmarks_list/);
  assert.match(populatedBookmarksHtml, new RegExp(`social_post_card_${post1.id}`));
  assert.doesNotMatch(populatedBookmarksHtml, new RegExp(`social_post_card_${post2.id}`));

  // Unbookmark removes post immediately from Bookmarks
  store.toggleBookmarkPost(post1.id);
  assert.equal(store.getBookmarkedPosts().length, 0);

  // Academy Hub (12-card 3x4 grid + 13 categories + search + Academy Social Feed + 7 quick cards)
  const state = store.getState();
  const academicsHubHtml = renderAcademicsHubView(state, s, { academySearchQuery: '' });
  assert.match(academicsHubHtml, /academy_search_input/);
  assert.match(academicsHubHtml, /academy_12_cards_grid/);
  assert.match(academicsHubHtml, /academics_hub_screen_list/);
  assert.match(academicsHubHtml, /academy_social_feed_section/);
  assert.match(academicsHubHtml, new RegExp(`social_post_card_${post1.id}`));
  for (const cat of ACADEMICS_V2_CATEGORIES) {
    assert.match(academicsHubHtml, new RegExp(`academy_card_${cat.routeId}`));
    const catDetailHtml = renderAcademyCategoryDetailView(state, s, {}, cat.routeId);
    assert.match(catDetailHtml, new RegExp(`academy_category_detail_${cat.routeId}`));
    for (const sub of cat.subSections) {
      assert.match(catDetailHtml, new RegExp(`academy_section_card_${sub.id}`));
    }
  }
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

  // Parent Dashboard (12 modules including parent_guidance + read-only Student Social Feed without Like/Bookmark buttons)
  const emptyParentStore = new ChevalierStore();
  const emptyParentHtml = renderParentDashboardView(emptyParentStore.getState(), s, { parentSearchOpen: false, parentSearchQuery: '' });
  assert.match(emptyParentHtml, /parent_screen_list/);
  assert.match(emptyParentHtml, /parent_social_feed_empty_card/);
  assert.match(emptyParentHtml, /Published school photos, videos, and student activity updates will appear here once shared by Chevalier Academy\./);

  const parentHtml = renderParentDashboardView(state, s, { parentSearchOpen: true, parentSearchQuery: '' });
  assert.match(parentHtml, /parent_screen_list/);
  assert.match(parentHtml, /parent_dashboard_search_field/);
  assert.match(parentHtml, /parent_student_info_card/);
  assert.match(parentHtml, /parent_student_avatar/);
  assert.match(parentHtml, /Sample Preview/);
  assert.match(parentHtml, /Class &amp; Section: Grade VIII · Section A \(Sample\)/);
  assert.match(parentHtml, /Academic Year: 2025–2026/);
  assert.match(parentHtml, /parent_glance_section/);
  assert.match(parentHtml, /parent_glance_attendance_card/);
  assert.match(parentHtml, /parent_glance_exams_card/);
  assert.match(parentHtml, /parent_latest_updates_section/);
  assert.match(parentHtml, /parent_services_section/);
  assert.match(parentHtml, /parent_student_social_feed_section/);
  assert.match(parentHtml, /parent_student_social_feed_list/);
  assert.doesNotMatch(parentHtml, new RegExp(`post_like_btn_${post1.id}`));
  assert.doesNotMatch(parentHtml, new RegExp(`post_bookmark_btn_${post1.id}`));

  // Search filtering and empty state on Parent Dashboard
  const filteredGuidanceHtml = renderParentDashboardView(state, s, { parentSearchOpen: true, parentSearchQuery: 'Guidance' });
  assert.match(filteredGuidanceHtml, /parent_search_clear_button/);
  assert.match(filteredGuidanceHtml, /parent_module_card_parent_guidance/);
  assert.doesNotMatch(filteredGuidanceHtml, /parent_module_card_attendance/);

  const noMatchSearchHtml = renderParentDashboardView(state, s, { parentSearchOpen: true, parentSearchQuery: 'zzzz_no_match' });
  assert.match(noMatchSearchHtml, /parent_search_empty_state/);

  // Verify all 12 modules and detail screens across EN, TA, and HI
  store.updateProfile({ phone: '+91 451 2400222', email: 'office@chevalieracademydgl.in' });
  store.addAnnouncement({ title: 'PTA General Body Notice', message: 'Saturday 10:00 AM in the auditorium.', priorityLabel: 'Circular' });
  const updatedState = store.getState();

  for (const lang of ['en', 'ta', 'hi']) {
    const langStrings = getStrings(lang);
    const localizedParentHtml = renderParentDashboardView(updatedState, langStrings, {});
    assert.match(localizedParentHtml, new RegExp(langStrings.parentSampleStudentName));
    assert.match(localizedParentHtml, new RegExp(langStrings.parentModuleParentGuidance));

    for (const mod of PARENT_MODULES) {
      assert.ok(langStrings[mod.titleKey], `Missing ${mod.titleKey} in ${lang}`);
      assert.ok(langStrings[mod.descKey], `Missing ${mod.descKey} in ${lang}`);
      assert.match(localizedParentHtml, new RegExp(`parent_module_card_${mod.key}`));
      const detailHtml = renderParentModuleDetailView(updatedState, langStrings, mod.key);
      assert.match(detailHtml, new RegExp(`parent_module_screen_${mod.key}`));
      assert.match(detailHtml, new RegExp(`parent_module_detail_${mod.key}`));
      assert.match(detailHtml, /parent_module_back_dashboard_btn/);
      if (mod.key === 'school_circulars' || mod.key === 'school_communication') {
        assert.match(detailHtml, /parent_module_open_announcements_btn/);
        assert.match(detailHtml, /PTA General Body Notice/);
      }
      if (mod.key === 'emergency_info') {
        assert.match(detailHtml, /parent_module_open_contact_btn/);
        assert.match(detailHtml, /\+91 451 2400222/);
      }
    }
  }

  // Secondary Detail Views render all Android public fields
  store.updateProfile({
    affiliationNumber: 'TN-MATRIC-624306',
    mediumAndGradesNote: 'English Medium · Pre-KG to Class XII'
  });
  store.addFacility({ name: 'Chemistry Lab', category: 'Laboratories', description: 'Modern lab', capacityOrSpec: '45 Students' });
  store.addTransportRoute({ routeNumber: '12', routeName: 'Siluvathur Main', stopsSummary: 'Stop A, Stop B', morningTiming: '7:30 AM', afternoonTiming: '4:30 PM', coordinatorContact: '+91 451 2400111' });

  const aboutHtml = renderAboutView(store.getState(), s);
  assert.match(aboutHtml, /TN-MATRIC-624306/);
  assert.match(aboutHtml, /English Medium · Pre-KG to Class XII/);

  const facilitiesHtml = renderFacilitiesView(store.getState(), s);
  assert.match(facilitiesHtml, /45 Students/);

  const transportHtml = renderTransportView(store.getState(), s);
  assert.match(transportHtml, /7:30 AM/);
  assert.match(transportHtml, /4:30 PM/);
  assert.match(transportHtml, /\+91 451 2400111/);

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
      const docMatch = u.pathname.match(/\/schools\/chevalier_academy_dindigul\/([^/]+)\/([^/]+)$/);
      if (docMatch) {
        const [, collection, remoteId] = docMatch;
        const docObj = cloudDb.get(`${collection}/${remoteId}`);
        if (docObj) return { ok: true, status: 200, json: async () => docObj };
        return { ok: false, status: 404, json: async () => ({}) };
      }

      const listMatch = u.pathname.match(/\/schools\/chevalier_academy_dindigul\/([^/]+)$/);
      if (listMatch) {
        const [, collectionId] = listMatch;
        const documents = [];
        for (const [key, docObj] of cloudDb.entries()) {
          if (key.startsWith(`${collectionId}/`)) {
            documents.push(docObj);
          }
        }
        return { ok: true, status: 200, json: async () => ({ documents }) };
      }

      return { ok: false, status: 404, json: async () => ({}) };
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

test('Base 3 — PinSecurityManager, Master Admin Setup, Recovery Code Rotation, Progressive Lockout & 5-Min Inactivity Lock', () => {
  // 1. PinSecurityManager rules match Android PinSecurityManager.kt
  assert.equal(PinSecurityManager.isValidPinFormat('12345'), false);
  assert.equal(PinSecurityManager.isValidPinFormat('12345a'), false);
  assert.equal(PinSecurityManager.isValidPinFormat('123456'), true);
  assert.equal(PinSecurityManager.isValidPinFormat('98765432'), true);

  assert.equal(PinSecurityManager.computeLockoutDurationMillis(0), 0);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(2), 0);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(3), 15_000);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(4), 30_000);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(5), 60_000);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(6), 300_000);
  assert.equal(PinSecurityManager.computeLockoutDurationMillis(10), 300_000);

  const recovery = PinSecurityManager.generateRecoveryCode();
  assert.match(recovery, /^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/);
  const salt = PinSecurityManager.generateSaltBase64();
  const verifier = PinSecurityManager.deriveVerifierBase64('135790', salt);
  assert.equal(PinSecurityManager.verifySecretConstantTime('135790', salt, verifier), true);
  assert.equal(PinSecurityManager.verifySecretConstantTime('135791', salt, verifier), false);

  // 2. First-time Master Admin Setup & Recovery Key Confirmation Gate
  const store = new ChevalierStore();
  const s = getStrings('en');
  assert.equal(store.hasMasterAdmin(), false);

  const setupHtml = renderAdminGateView(store, s, {});
  assert.match(setupHtml, /master_admin_setup_screen/);
  assert.match(setupHtml, /create_master_admin_btn/);

  assert.throws(() => store.setupMasterAdmin('Rev. Fr. Principal', 'master_admin', '1234', '1234'), /6 numeric digits/);
  assert.throws(() => store.setupMasterAdmin('Rev. Fr. Principal', 'master_admin', '123456', '654321'), /do not match/);

  const setupRes = store.setupMasterAdmin('Rev. Fr. Principal', 'master_admin', '246810', '246810');
  assert.equal(store.hasMasterAdmin(), true);
  assert.equal(store.isAdminAuthenticated, true);
  assert.equal(store.getCurrentRole(), MANAGEMENT_ROLES.MASTER_ADMIN);
  assert.ok(store.pendingRecoveryCode);

  const recoveryCardHtml = renderAdminGateView(store, s, {});
  assert.match(recoveryCardHtml, /master_recovery_key_card/);
  assert.match(recoveryCardHtml, new RegExp(setupRes.recoveryCode));

  store.confirmRecoveryKeySaved();
  assert.equal(store.pendingRecoveryCode, null);

  // 3. Progressive Lockout after 3, 4, 5, 6 failed PIN attempts
  store.logoutAdmin();
  assert.equal(store.isAdminAuthenticated, false);

  const t0 = 1_000_000;
  assert.equal(store.loginWithPin('master_admin', '000000', t0).ok, false);
  assert.equal(store.loginWithPin('master_admin', '000000', t0 + 10).ok, false);
  const fail3 = store.loginWithPin('master_admin', '000000', t0 + 20);
  assert.equal(fail3.ok, false);
  assert.match(fail3.error, /15s/);

  // Blocked while locked, even with valid PIN
  const blockedDuringLock = store.loginWithPin('master_admin', '246810', t0 + 5_000);
  assert.equal(blockedDuringLock.ok, false);
  assert.match(blockedDuringLock.error, /Account temporarily locked/);

  // 4th failure after 15s lock expires -> 30s lock
  const fail4 = store.loginWithPin('master_admin', '000000', t0 + 16_000);
  assert.equal(fail4.ok, false);
  assert.match(fail4.error, /30s/);

  // 5th failure after 30s lock expires -> 60s lock
  const fail5 = store.loginWithPin('master_admin', '000000', t0 + 47_000);
  assert.equal(fail5.ok, false);
  assert.match(fail5.error, /60s/);

  // 6th failure after 60s lock expires -> 300s lock
  const fail6 = store.loginWithPin('master_admin', '000000', t0 + 108_000);
  assert.equal(fail6.ok, false);
  assert.match(fail6.error, /300s/);

  // 4. Master Admin PIN Recovery clears lockout and rotates recovery code
  assert.throws(
    () => store.recoverMasterAdminPin('XXXX-YYYY-ZZZZ', '654321', '654321'),
    /Invalid Master Recovery Code/
  );
  const rotated = store.recoverMasterAdminPin(setupRes.recoveryCode, '654321', '654321');
  assert.notEqual(rotated.recoveryCode, setupRes.recoveryCode);
  // Old recovery code is invalidated immediately
  assert.throws(
    () => store.recoverMasterAdminPin(setupRes.recoveryCode, '112233', '112233'),
    /Invalid Master Recovery Code/
  );

  // Login succeeds immediately with new PIN
  const loginAfterRecover = store.loginWithPin('master_admin', '654321', t0 + 110_000);
  assert.equal(loginAfterRecover.ok, true);
  assert.equal(store.isAdminAuthenticated, true);

  // 5. 5-Minute Inactivity Auto-Lock
  store.onAdminInteraction(2_000_000);
  assert.equal(store.checkInactivityLock(2_000_000 + ADMIN_INACTIVITY_TIMEOUT_MILLIS - 1), false);
  assert.equal(store.isAdminAuthenticated, true);
  assert.equal(store.checkInactivityLock(2_000_000 + ADMIN_INACTIVITY_TIMEOUT_MILLIS + 1), true);
  assert.equal(store.isAdminAuthenticated, false);
});

test('Base 3 — 3 Roles (MASTER_ADMIN, SUPER_OWNER max 2, STAFF), 20x5 RBAC Matrix, Staff Lifecycle, 8 Hubs, Global Search & Notifications', () => {
  const store = new ChevalierStore();
  const s = getStrings('en');

  assert.equal(MANAGEMENT_SECTIONS.length, 20);
  assert.equal(Object.keys(PERMISSION_ACTIONS).length, 5);
  assert.equal(ADMIN_DASHBOARD_CARDS.length, 8);

  // 1. Setup Master Admin
  store.setupMasterAdmin('Rev. Fr. Master Admin', 'master', '111222', '111222');
  store.confirmRecoveryKeySaved();

  // Master Admin has all supported 20x5 permissions and all 8 dashboard cards unlocked
  for (const sec of MANAGEMENT_SECTIONS) {
    for (const act of Object.values(PERMISSION_ACTIONS)) {
      const expected = act === PERMISSION_ACTIONS.PUBLISH ? Boolean(sec.supportsPublish) : true;
      assert.equal(store.hasPermission(sec.key, act), expected, `Master Admin mismatch on ${sec.key}.${act}`);
    }
  }
  for (const card of ADMIN_DASHBOARD_CARDS) {
    assert.equal(store.canAccessAdminCard(card.id), true);
  }

  // 2. Create up to 2 SUPER_OWNER accounts; 3rd SUPER_OWNER must be rejected
  const so1 = store.createStaffAccount({
    displayName: 'Vice Principal 1',
    username: 'super1',
    pin: '222333',
    confirmPin: '222333',
    role: MANAGEMENT_ROLES.SUPER_OWNER
  });
  const so2 = store.createStaffAccount({
    displayName: 'Vice Principal 2',
    username: 'super2',
    pin: '333444',
    confirmPin: '333444',
    role: MANAGEMENT_ROLES.SUPER_OWNER
  });
  assert.throws(
    () =>
      store.createStaffAccount({
        displayName: 'Vice Principal 3',
        username: 'super3',
        pin: '444555',
        confirmPin: '444555',
        role: MANAGEMENT_ROLES.SUPER_OWNER
      }),
    /Maximum of 2 Super Owner accounts allowed/
  );

  // 3. Create a STAFF account with granular permissions:
  //    - NEWS: VIEW + CREATE (no EDIT/DELETE/PUBLISH)
  //    - HIGHER_SECONDARY_COURSES: VIEW + EDIT (no CREATE/DELETE/PUBLISH)
  //    - All other 18 sections: false
  const staffPerms = {
    NEWS: { VIEW: true, CREATE: true, EDIT: false, DELETE: false, PUBLISH: false },
    HIGHER_SECONDARY_COURSES: { VIEW: true, CREATE: false, EDIT: true, DELETE: false, PUBLISH: false }
  };

  const staffAcc = store.createStaffAccount({
    displayName: 'Media & Course Coordinator',
    username: 'staff_coord',
    pin: '555666',
    confirmPin: '555666',
    role: MANAGEMENT_ROLES.STAFF,
    permissions: staffPerms
  });

  // Render Staff Management screen as Master Admin
  const staffCreateHtml = renderAdminStaffManagementView(store, store.getState(), s, {
    staffDialogMode: 'create',
    staffDraftRole: MANAGEMENT_ROLES.STAFF,
    staffDraftPermissions: staffPerms
  });
  assert.match(staffCreateHtml, /staff_pin_input/);
  assert.match(staffCreateHtml, /staff_confirm_pin_input/);

  const staffMgmtHtml = renderAdminStaffManagementView(store, store.getState(), s, {
    staffDialogMode: 'edit',
    editingStaffId: staffAcc.id,
    staffDraftRole: staffAcc.role,
    staffDraftPermissions: staffAcc.permissions
  });
  assert.match(staffMgmtHtml, /staff_management_screen/);
  assert.match(staffMgmtHtml, /create_staff_account_btn/);
  assert.match(staffMgmtHtml, /staff_account_dialog/);
  assert.match(staffMgmtHtml, /staff_permission_matrix/);
  for (const sec of MANAGEMENT_SECTIONS) {
    assert.match(staffMgmtHtml, new RegExp(`perm_row_${sec.key}`));
  }

  // 4. Switch session to SUPER_OWNER 1 -> can access content hubs, cannot access STAFF_MANAGEMENT
  store.logoutAdmin();
  assert.equal(store.loginWithPin('super1', '222333').ok, true);
  assert.equal(store.hasPermission('STAFF_MANAGEMENT', 'VIEW'), false);
  assert.equal(store.canAccessAdminCard('staff_management'), false);
  assert.equal(store.canAccessAdminCard('home_page'), true);
  assert.equal(store.canAccessAdminCard('academy'), true);
  assert.throws(
    () =>
      store.createStaffAccount({
        displayName: 'Unauthorized Staff',
        username: 'unauth',
        pin: '777888',
        confirmPin: '777888',
        role: MANAGEMENT_ROLES.STAFF
      }),
    /Only Master Admin/
  );

  // 5. Switch session to STAFF account -> verify 8-Hub locks, Access Denied screen, and store-level RBAC enforcement
  store.logoutAdmin();
  assert.equal(store.loginWithPin('staff_coord', '555666').ok, true);

  assert.equal(store.canAccessAdminCard('updates'), true); // has NEWS.VIEW
  assert.equal(store.canAccessAdminCard('academy'), true); // has HIGHER_SECONDARY_COURSES.VIEW (under academy hub)
  assert.equal(store.canAccessAdminCard('home_page'), false);
  assert.equal(store.canAccessAdminCard('parent'), false);
  assert.equal(store.canAccessAdminCard('contact'), false);
  assert.equal(store.canAccessAdminCard('staff_management'), false);
  assert.equal(store.canAccessAdminCard('common_app'), false);
  assert.equal(store.canAccessAdminCard('management_tools'), false);

  const staffDashHtml = renderAdminDashboardView(store.getState(), s, {}, store);
  assert.match(staffDashHtml, /admin_dashboard_header/);
  assert.match(staffDashHtml, /admin_hero_banner_card/);
  assert.doesNotMatch(staffDashHtml, /admin_hero_edit_btn/); // STAFF without LOGO_BANNER.EDIT cannot edit hero banner
  assert.match(staffDashHtml, /management_dashboard_grid/);
  assert.match(staffDashHtml, /admin_dash_card_lock_home_page/);
  assert.match(staffDashHtml, /admin_dash_card_lock_staff_management/);
  assert.doesNotMatch(staffDashHtml, /admin_dash_card_lock_updates/);

  // Navigating to restricted hub or section renders AdminAccessDeniedScreen
  const deniedHubHtml = renderAdminHubView(store, store.getState(), s, {}, 'home_page');
  assert.match(deniedHubHtml, /admin_access_denied_screen/);
  const deniedSecHtml = renderAdminSectionView(store, store.getState(), s, {}, 'BACKUP_RESTORE');
  assert.match(deniedSecHtml, /admin_access_denied_screen/);
  const allowedHubHtml = renderAdminHubView(store, store.getState(), s, {}, 'updates');
  assert.match(allowedHubHtml, /admin_hub_item_news/);

  // Store-level RBAC enforcement on mutations
  // Allowed: NEWS.CREATE and HIGHER_SECONDARY_COURSES.EDIT
  store.addNews({ title: 'Staff Drafted Science News', summary: 'Created by staff with NEWS.CREATE' });
  store.updateCourse(1, { title: 'Bio-Maths Stream', medium: 'English Medium', subjects: 'Physics, Chemistry, Biology, Maths', verified: false });

  // Denied: NEWS.DELETE, TRANSPORT.CREATE, ANNOUNCEMENTS.CREATE, SCHOOL_PROFILE.EDIT, BACKUP_RESTORE.VIEW
  const newsId = store.getState().news[0].id;
  assert.throws(() => store.deleteNews(newsId), /Permission denied: role STAFF lacks DELETE permission on NEWS/);
  assert.throws(() => store.addTransportRoute({ routeNumber: 'R-99', routeName: 'Bypass', stops: 'Stop 1' }), /Permission denied: role STAFF lacks CREATE permission on TRANSPORT/);
  assert.throws(() => store.addAnnouncement({ title: 'Unauth', message: 'No perm' }), /Permission denied: role STAFF lacks CREATE permission on ANNOUNCEMENTS/);
  assert.throws(() => store.updateProfile({ phone: '000' }), /Permission denied: role STAFF lacks EDIT permission on SCHOOL_PROFILE/);
  assert.throws(() => store.exportBackupJson(), /Permission denied: role STAFF lacks VIEW permission on BACKUP_RESTORE/);

  // 6. Permission-filtered Admin Global Search & Notifications
  // Add an announcement as Master Admin so there is data in an unauthorized section
  store.logoutAdmin();
  store.loginWithPin('master', '111222');
  store.addAnnouncement({ title: 'Secret Master Circular', message: 'Confidential notice' });

  // As STAFF, searching 'Science' finds the NEWS record, but searching 'Secret' or 'Backup' returns 0 results
  store.logoutAdmin();
  store.loginWithPin('staff_coord', '555666');
  const staffSearchScience = store.searchAdminDashboard('Science');
  assert.ok(staffSearchScience.some((r) => r.title === 'Staff Drafted Science News'));

  const staffSearchSecret = store.searchAdminDashboard('Secret');
  assert.equal(staffSearchSecret.length, 0);
  const staffSearchBackup = store.searchAdminDashboard('Backup');
  assert.equal(staffSearchBackup.length, 0);

  const searchScreenHtml = renderAdminSearchView(store, store.getState(), s, { adminSearchQuery: 'Science' });
  assert.match(searchScreenHtml, /admin_dashboard_search_screen/);
  assert.match(searchScreenHtml, /Staff Drafted Science News/);

  // Grant NOTIFICATIONS.VIEW + EDIT to staff_coord so they can view & mark read their section notifications
  store.logoutAdmin();
  store.loginWithPin('master', '111222');
  store.updateStaffAccount({
    accountId: staffAcc.id,
    permissions: {
      ...staffPerms,
      NOTIFICATIONS: { VIEW: true, CREATE: false, EDIT: true, DELETE: false, PUBLISH: false }
    }
  });
  store.logoutAdmin();
  store.loginWithPin('staff_coord', '555666');

  // Notifications filtered by section VIEW permission
  const staffNotifs = store.getVisibleNotifications();
  assert.ok(staffNotifs.length > 0);
  assert.ok(staffNotifs.every((n) => n.sectionKey === 'NEWS' || n.sectionKey === 'HIGHER_SECONDARY_COURSES' || n.sectionKey === 'NOTIFICATIONS'));
  const notifHtml = renderAdminNotificationsView(store, store.getState(), s, {});
  assert.match(notifHtml, /device_alerts_card/);
  assert.match(notifHtml, /notifications_mark_all_read_btn/);
  store.markAllNotificationsRead();
  assert.equal(store.getUnreadNotificationsCount(), 0);

  // 7. Deactivating a STAFF account immediately terminates its session and blocks login
  store.logoutAdmin();
  store.loginWithPin('master', '111222');
  store.updateStaffAccount({ accountId: staffAcc.id, isActive: false });

  const disabledLogin = store.loginWithPin('staff_coord', '555666');
  assert.equal(disabledLogin.ok, false);
  assert.match(disabledLogin.error, /deactivated/i);

  // 8. Deleting non-Master account succeeds; deleting MASTER_ADMIN is blocked
  const masterId = store.getState().managementAccounts.find((a) => a.role === MANAGEMENT_ROLES.MASTER_ADMIN).id;
  assert.throws(() => store.deleteStaffAccount(masterId), /Master Admin account cannot be deleted/);
  store.deleteStaffAccount(so2.id);
  assert.equal(store.getState().managementAccounts.some((a) => a.id === so2.id), false);

  // 9. Backup JSON strips PIN & Recovery verifiers/salts
  const backupJson = store.exportBackupJson();
  assert.ok(!backupJson.includes('pinVerifierBase64'));
  assert.ok(!backupJson.includes('recoveryVerifierBase64'));
});

test('Base 4 — Academics V2 (13 Categories), Card 07 Photo-Only Gallery, Card 09 Exam Results Owner Gate & Card 11 8-Field Faculty Privacy', () => {
  const store = new ChevalierStore();
  const s = getStrings('en');

  store.setupMasterAdmin('Rev. Fr. Correspondent', 'owner_master', '123456', '123456');
  store.confirmRecoveryKeySaved();

  // Create a STAFF account with broad PUBLISH permissions across content sections
  const staffPerms = buildDefaultPermissionsForRole(MANAGEMENT_ROLES.SUPER_OWNER);
  store.createStaffAccount({
    displayName: 'Academic Coordinator',
    username: 'acad_staff',
    pin: '654321',
    confirmPin: '654321',
    role: MANAGEMENT_ROLES.STAFF,
    permissions: staffPerms
  });

  // 1. All 13 V2 categories render in Admin V2 Hub and individual V2 category management screens
  const v2HubHtml = renderAdminV2AcademicsHubView(store, store.getState(), s, {});
  assert.match(v2HubHtml, /admin_v2_academics_hub/);
  for (const cat of ACADEMICS_V2_CATEGORIES) {
    assert.match(v2HubHtml, new RegExp(`admin_v2_academics_cat_${cat.routeId}`));
    const catHtml = renderAdminV2CategoryManagementView(store, store.getState(), s, {}, cat.routeId);
    assert.match(catHtml, new RegExp(`admin_v2_category_screen_${cat.routeId}`));
  }

  // 2. Draft-by-default V2 record lifecycle: create -> edit -> submit -> publish -> unpublish -> archive -> delete
  const draftRec = store.saveV2Record({
    categoryKey: 'academics',
    sectionKey: 'curriculum',
    title: '2026 Matriculation Syllabus',
    subtitle: 'Grades I to XII',
    bodyText: 'Updated Samacheer Kalvi aligned syllabus.',
    academicYear: '2025-2026'
  });
  assert.equal(draftRec.status, ACADEMICS_V2_RECORD_STATUS.DRAFT);
  assert.equal(store.getPublicV2Records('academics', 'curriculum').length, 0);

  const editedRec = store.saveV2Record({
    existingId: draftRec.id,
    categoryKey: 'academics',
    sectionKey: 'curriculum',
    title: '2026 Matriculation Syllabus (Revised)',
    bodyText: 'Revised with STEM lab hours.'
  });
  assert.equal(editedRec.status, ACADEMICS_V2_RECORD_STATUS.DRAFT);

  const pendingRec = store.submitV2RecordForApproval(editedRec.id);
  assert.equal(pendingRec.status, ACADEMICS_V2_RECORD_STATUS.PENDING_APPROVAL);

  const publishedRec = store.publishV2Record(editedRec.id);
  assert.equal(publishedRec.status, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  assert.equal(store.getPublicV2Records('academics', 'curriculum').length, 1);

  const publicAcadHtml = renderAcademyCategoryDetailView(
    store.getState(),
    s,
    { expandedAcademySections: { 'academics::curriculum': true } },
    'academics'
  );
  assert.match(publicAcadHtml, /2026 Matriculation Syllabus \(Revised\)/);

  const unpubRec = store.updateV2RecordStatus(editedRec.id, ACADEMICS_V2_RECORD_STATUS.UNPUBLISHED);
  assert.equal(unpubRec.status, ACADEMICS_V2_RECORD_STATUS.UNPUBLISHED);
  assert.equal(store.getPublicV2Records('academics', 'curriculum').length, 0);

  const archivedRec = store.updateV2RecordStatus(editedRec.id, ACADEMICS_V2_RECORD_STATUS.ARCHIVED);
  assert.equal(archivedRec.status, ACADEMICS_V2_RECORD_STATUS.ARCHIVED);
  assert.equal(store.deleteV2Record(editedRec.id), true);

  // 3. Card 09 — Exam Results Owner-Only Publishing Gate
  store.logoutAdmin();
  assert.equal(store.loginWithPin('acad_staff', '654321').ok, true);

  const examDraft = store.saveV2Record({
    categoryKey: 'examinations',
    sectionKey: 'exam_results',
    title: 'Class XII Public Exam Results 2026',
    bodyText: '100% pass record across all streams.'
  });
  assert.equal(examDraft.isExamResultAuthorized, false);
  assert.equal(store.canPublishV2Directly('examinations', 'exam_results'), false);

  // Staff with PUBLISH permission is still blocked from publishing Exam Results
  assert.throws(
    () => store.publishV2Record(examDraft.id),
    /only the School Owner can approve and publish Exam Results/i
  );
  assert.equal(store.getPublicV2Records('examinations', 'exam_results').length, 0);

  // Staff submits Exam Results for Owner Approval -> Owner authorizes & publishes
  store.submitV2RecordForApproval(examDraft.id);
  store.logoutAdmin();
  assert.equal(store.loginWithPin('owner_master', '123456').ok, true);

  const publishedExam = store.ownerApproveAndPublishV2Record(examDraft.id, { authorizeExamResult: true });
  assert.equal(publishedExam.status, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  assert.equal(publishedExam.isExamResultAuthorized, true);
  assert.equal(store.getPublicV2Records('examinations', 'exam_results').length, 1);

  const publicExamHtml = renderAcademyCategoryDetailView(
    store.getState(),
    s,
    { expandedAcademySections: { 'examinations::exam_results': true } },
    'examinations'
  );
  assert.match(publicExamHtml, /Class XII Public Exam Results 2026/);

  // 4. Card 07 — Photo-Only Gallery Manager & Multi-Album Photo Reuse
  const album1 = store.saveV2GalleryAlbum({
    sectionKey: 'event_wise_albums',
    title: 'Science Expo 2026',
    coverImageUri: 'data:image/jpeg;base64,ALBUM_COVER'
  });
  assert.equal(album1.coverPhotoUri, 'data:image/jpeg;base64,ALBUM_COVER');
  const album2 = store.saveV2GalleryAlbum({ sectionKey: 'campus_life_photos', title: 'Campus Highlights' });
  assert.equal(album1.status, ACADEMICS_V2_RECORD_STATUS.DRAFT);

  // Rejects YouTube / video URIs strictly
  assert.throws(
    () =>
      store.saveV2GalleryPhoto({
        title: 'Blocked Video',
        imageUri: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        albumIds: [album1.id]
      }),
    /strictly for Photos only/i
  );
  assert.throws(
    () =>
      store.saveV2GalleryPhoto({
        title: 'Blocked MP4',
        imageUri: 'https://example.com/clip.mp4',
        albumIds: [album1.id]
      }),
    /strictly for Photos only/i
  );

  // Valid photo assigned to album1, then reused across both album1 & album2
  const photo = store.saveV2GalleryPhoto({
    title: 'Robotics Showcase',
    caption: 'Student robotics exhibit',
    imageUri: 'data:image/jpeg;base64,ROBOTICS_PHOTO',
    albumIds: [album1.id]
  });
  assert.equal(photo.description, 'Student robotics exhibit');
  store.assignV2GalleryPhotoToAlbums(photo.id, [album1.id, album2.id]);

  // Neither album nor photo is published yet -> hidden publicly
  assert.equal(store.getPublicV2GalleryAlbums().length, 0);
  assert.equal(store.getPublicV2GalleryPhotosForAlbum(album1.id).length, 0);

  // Publish both albums, photo still draft -> still 0 public photos
  store.updateV2GalleryAlbumStatus(album1.id, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  store.updateV2GalleryAlbumStatus(album2.id, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  assert.equal(store.getPublicV2GalleryAlbums().length, 2);
  assert.equal(store.getPublicV2GalleryPhotosForAlbum(album1.id).length, 0);

  // Publish photo -> now visible in both albums & on Public Gallery Detail view
  store.updateV2GalleryPhotoStatus(photo.id, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  assert.equal(store.getPublicV2GalleryPhotosForAlbum(album1.id).length, 1);
  assert.equal(store.getPublicV2GalleryPhotosForAlbum(album2.id).length, 1);

  const publicGalleryCatHtml = renderAcademyCategoryDetailView(store.getState(), s, {}, 'gallery');
  assert.match(publicGalleryCatHtml, /Science Expo 2026/);
  assert.match(publicGalleryCatHtml, /Robotics Showcase/);

  // 5. Card 11 — Privacy-Guarded 8-Field Faculty & Staff Manager
  const facultyDraft = store.saveV2FacultyProfile({
    sectionKey: 'staff_profiles',
    teacherName: 'Mrs. Kavitha Sundaram',
    designation: 'Senior Physics Teacher',
    department: 'Science',
    teachingSubjects: 'Physics, Applied Mechanics',
    educationalQualifications: 'M.Sc., B.Ed.',
    teachingExperience: '14 Years',
    professionalIntroduction: 'Mentors Higher Secondary Physics Olympiad students.',
    approvedProfilePhotoUri: 'data:image/jpeg;base64,FACULTY_PHOTO',
    isSchoolApproved: true,
    hasStaffConsent: false
  });
  assert.equal(facultyDraft.status, ACADEMICS_V2_RECORD_STATUS.DRAFT);

  // Blocked from publishing without both School Approval AND Staff Consent
  assert.throws(
    () => store.updateV2FacultyProfileStatus(facultyDraft.id, ACADEMICS_V2_RECORD_STATUS.PUBLISHED),
    /School Approval and Staff Consent/
  );
  assert.equal(store.getPublicV2FacultyProfiles().length, 0);

  // Confirm both approvals and publish
  store.saveV2FacultyProfile({
    ...facultyDraft,
    existingId: facultyDraft.id,
    isSchoolApproved: true,
    hasStaffConsent: true
  });
  store.updateV2FacultyProfileStatus(facultyDraft.id, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);

  const publicFaculty = store.getPublicV2FacultyProfiles('staff_profiles');
  assert.equal(publicFaculty.length, 1);
  assert.deepEqual(
    Object.keys(publicFaculty[0]).sort(),
    [
      'approvedProfilePhotoUri',
      'department',
      'designation',
      'educationalQualifications',
      'id',
      'professionalIntroduction',
      'sectionKey',
      'teacherName',
      'teachingExperience',
      'teachingSubjects'
    ].sort()
  );

  const publicFacultyHtml = renderAcademyCategoryDetailView(
    store.getState(),
    s,
    { expandedAcademySections: { 'faculty_staff::staff_profiles': true } },
    'faculty_staff'
  );
  assert.match(publicFacultyHtml, /Mrs\. Kavitha Sundaram/);
  assert.match(publicFacultyHtml, /M\.Sc\., B\.Ed\./);
});

test('Base 4 — School Owner Control Center (6 Sections), Staff Access Request Lifecycle & Privacy Isolation', () => {
  const store = new ChevalierStore();
  const s = getStrings('en');

  assert.equal(SCHOOL_OWNER_SECTIONS.length, 6);

  store.setupMasterAdmin('Rev. Fr. School Owner', 'owner_chief', '111222', '111222');
  store.confirmRecoveryKeySaved();
  store.createStaffAccount({
    displayName: 'Office Staff',
    username: 'office_staff',
    pin: '333444',
    confirmPin: '333444',
    role: MANAGEMENT_ROLES.STAFF,
    permissions: {}
  });

  // Owner updates private profile (must never leak on public Home/About/Contact pages)
  const ownerProfile = store.updateOwnerPrivateProfile({
    ownerName: 'Rev. Fr. Joseph Correspondent',
    designation: 'School Owner / Correspondent',
    privateContactPhone: '+91 98421 00999 PRIVATE',
    privateContactEmail: 'private.owner@chevalier-internal.local',
    governanceNote: 'Confidential trustee phone line'
  });
  assert.equal(ownerProfile.privateContactPhone, '+91 98421 00999 PRIVATE');

  for (const pubHtml of [
    renderHomeView(store.getState(), s, {}),
    renderAboutView(store.getState(), s),
    renderContactView(store.getState(), s)
  ]) {
    assert.doesNotMatch(pubHtml, /98421 00999 PRIVATE/);
    assert.doesNotMatch(pubHtml, /private\.owner@chevalier-internal\.local/);
    assert.doesNotMatch(pubHtml, /Confidential trustee phone line/);
  }

  // All 6 Owner Control Center sections render for School Owner
  const expectedPanels = {
    OWNER_PROFILE_CONTACT: 'owner_section_panel_profile',
    STAFF_ACCESS_APPROVAL: 'owner_section_panel_staff_access',
    ROLE_PERMISSION_CONTROL: 'owner_section_panel_roles',
    APPROVAL_REQUESTS: 'owner_section_panel_approvals',
    FINAL_PREVIEW_APPROVAL: 'owner_section_panel_final_preview',
    ACCESS_HISTORY_SECURITY: 'owner_section_panel_audit'
  };
  for (const sec of SCHOOL_OWNER_SECTIONS) {
    const html = renderSchoolOwnerControlCenterView(store, store.getState(), s, { ownerControlSection: sec.key });
    assert.match(html, /school_owner_control_center_screen/);
    assert.match(html, new RegExp(`owner_tab_${sec.key}`));
    assert.match(html, new RegExp(expectedPanels[sec.key]));
  }

  // Switch to STAFF -> Owner Control Center is denied, private profile read/write blocked, Staff Access Request form allowed
  store.logoutAdmin();
  assert.equal(store.loginWithPin('office_staff', '333444').ok, true);
  assert.throws(() => store.getOwnerPrivateProfile(), /School Owner/);
  assert.throws(() => store.updateOwnerPrivateProfile({ ownerName: 'Hijack' }), /School Owner/);

  const staffDeniedHtml = renderSchoolOwnerControlCenterView(store, store.getState(), s, {});
  assert.match(staffDeniedHtml, /owner_control_center_denied/);
  assert.match(staffDeniedHtml, /staff_access_request_form/);

  const req = store.submitStaffAccessRequest({
    requestedRole: MANAGEMENT_ROLES.STAFF,
    requestedCategoryKeys: ['academics', 'gallery'],
    reason: 'Need access to upload Sports Day photos.'
  });
  assert.equal(req.status, 'PENDING');
  assert.throws(() => store.reviewStaffAccessRequest(req.id, true), /School Owner/);

  // Switch back to Owner -> Approve, then Revoke Staff Access Request
  store.logoutAdmin();
  assert.equal(store.loginWithPin('owner_chief', '111222').ok, true);

  const approved = store.reviewStaffAccessRequest(req.id, true, 'Approved for Sports Day');
  assert.equal(approved.status, 'APPROVED');
  const revoked = store.revokeStaffAccessRequest(req.id, 'Event concluded');
  assert.equal(revoked.status, 'REVOKED');
  assert.ok(store.getV2AccessAuditEvents().length >= 4);
});

test('Base 4 — CMS Reorder/Publish, Media Studio Operations, Approval Center, Preview Hub, Version History Restore, Backup Inspection & Audit Log Filtering', () => {
  const store = new ChevalierStore();
  const s = getStrings('en');

  store.setupMasterAdmin('Master Admin', 'master_ops', '246810', '246810');
  store.confirmRecoveryKeySaved();

  // 1. CMS Create, Update, Reorder (UP/DOWN), Publish/Unpublish & Public Filtering
  store.addNews({ title: 'First News Item', summary: 'Summary 1' });
  store.addNews({ title: 'Second News Item', summary: 'Summary 2' });
  const topBefore = store.getState().news[0]; // Second News Item (newest-first prepend)
  const bottomBefore = store.getState().news[1]; // First News Item
  assert.notEqual(topBefore.id, bottomBefore.id);

  store.updateNews(topBefore.id, { title: 'Second News Updated', summary: 'Updated Summary 2' });
  assert.equal(store.getState().news.find((n) => n.id === topBefore.id).title, 'Second News Updated');

  // Reorder down moves topBefore (index 0) to index 1
  store.reorderCmsItem('NEWS', topBefore.id, 'down');
  assert.equal(store.getState().news[0].id, bottomBefore.id);
  assert.equal(store.getState().news[1].id, topBefore.id);

  // Unpublish hides topBefore from public Updates screen; Publish makes it visible again
  store.setCmsItemPublished('NEWS', topBefore.id, false);
  const pubUpdatesHidden = renderUpdatesView(store.getState(), s, {});
  assert.doesNotMatch(pubUpdatesHidden, /Second News Updated/);
  assert.match(pubUpdatesHidden, /First News Item/);

  store.setCmsItemPublished('NEWS', topBefore.id, true);
  const pubUpdatesVisible = renderUpdatesView(store.getState(), s, {});
  assert.match(pubUpdatesVisible, /Second News Updated/);

  // 2. Media Studio: Import image, YouTube validation, Edit metadata, Replace image, Assign to Hero & Album, Usage Warnings, Delete
  assert.throws(
    () =>
      store.saveMediaStudioItem({
        title: 'Invalid YouTube Video',
        mediaType: 'YOUTUBE_VIDEO',
        mediaUri: 'https://not-youtube.example.com/watch?v=123'
      }),
    /valid YouTube URL/
  );
  const ytMedia = store.saveMediaStudioItem({
    title: 'Campus Tour Video',
    mediaType: 'YOUTUBE_VIDEO',
    mediaUri: 'https://youtu.be/dQw4w9WgXcQ',
    collectionName: 'Videos'
  });
  assert.equal(ytMedia.youtubeVideoId, 'dQw4w9WgXcQ');

  const mediaItem = store.saveMediaStudioItem({
    title: 'Main Auditorium',
    mediaUri: 'data:image/jpeg;base64,AUDITORIUM_V1',
    collectionName: 'Campus',
    caption: 'Front view'
  });
  store.updateMediaMetadata(mediaItem.id, { title: 'Main Auditorium HD', caption: 'Newly renovated hall' });
  assert.equal(store.getState().mediaStudioItems.find((m) => m.id === mediaItem.id).title, 'Main Auditorium HD');

  store.replaceMediaImage(mediaItem.id, 'data:image/jpeg;base64,AUDITORIUM_V2');
  assert.equal(store.getState().mediaStudioItems.find((m) => m.id === mediaItem.id).mediaUri, 'data:image/jpeg;base64,AUDITORIUM_V2');

  store.assignMediaToHero(mediaItem.id, { publishNow: true });
  store.assignMediaToAlbum(mediaItem.id, { album: 'Campus Views', category: 'Campus', publishNow: true });
  const warnings = store.getMediaUsageWarnings(mediaItem.id);
  assert.ok(warnings.some((w) => /Hero Banner/i.test(w)));
  assert.ok(warnings.some((w) => /Gallery/i.test(w)));

  const mediaStudioHtml = renderAdminSectionView(store, store.getState(), s, {}, 'MEDIA_STUDIO');
  assert.match(mediaStudioHtml, /Main Auditorium HD/);
  assert.match(mediaStudioHtml, /Hero Banner/);

  store.deleteMediaStudioItem(mediaItem.id);
  assert.equal(store.getState().mediaStudioItems.some((m) => m.id === mediaItem.id), false);

  // 3. Content Approval Center (Pending/Approved/Rejected tabs) + 3-Tab Preview & Publish Hub
  const approvalReq = store.submitForApproval({
    sectionKey: 'ANNOUNCEMENTS',
    itemId: 9001,
    itemTitle: 'Term 2 Holiday Notice',
    actionType: 'PUBLISH',
    payloadSummary: 'Submitted for Principal approval'
  });
  store.reviewApproval(approvalReq.id, true, 'Approved for immediate release');
  assert.equal(store.getState().contentApprovals.find((a) => a.id === approvalReq.id).status, 'APPROVED');

  const approvalHtml = renderAdminSectionView(store, store.getState(), s, { approvalFilter: 'APPROVED' }, 'APPROVAL_CENTER');
  assert.match(approvalHtml, /Term 2 Holiday Notice/);

  for (const tab of ['review', 'home', 'academics']) {
    const previewHtml = renderAdminV2PreviewAndPublishView(store, store.getState(), s, { previewPublishTab: tab });
    assert.match(previewHtml, /admin_v2_preview_publish_screen/);
  }

  // 4. Version History JSON Snapshot Capture & Restore (Publish & Draft modes)
  const v2Draft = store.saveV2Record({
    categoryKey: 'admissions',
    sectionKey: 'admission_process',
    title: 'Admissions 2026 Original',
    bodyText: 'Original version body'
  });
  store.publishV2Record(v2Draft.id);
  const versionsBeforeEdit = store.getState().versionHistory.filter((v) => String(v.itemTitle || '').includes('Admissions 2026 Original'));
  assert.ok(versionsBeforeEdit.length >= 1);
  assert.ok(versionsBeforeEdit[0].contentSnapshotJson, 'Version history must capture JSON snapshot');

  // Modify title, then restore previous snapshot
  store.saveV2Record({
    existingId: v2Draft.id,
    categoryKey: 'admissions',
    sectionKey: 'admission_process',
    title: 'Admissions 2026 Overwritten',
    bodyText: 'Overwritten body'
  });
  store.restoreVersionSnapshot(versionsBeforeEdit[0].id, { publishImmediately: true });
  const restoredV2 = store.getState().academicsV2.records.find((r) => r.id === v2Draft.id);
  assert.equal(restoredV2.title, 'Admissions 2026 Original');
  assert.equal(restoredV2.status, ACADEMICS_V2_RECORD_STATUS.PUBLISHED);

  // Restore in draft mode (e.g., for EDIT-only role)
  store.restoreVersionSnapshot(versionsBeforeEdit[0].id, { publishImmediately: false });
  assert.equal(store.getState().academicsV2.records.find((r) => r.id === v2Draft.id).status, ACADEMICS_V2_RECORD_STATUS.DRAFT);

  // 5. Complete JSON Backup Inspection, Optional Password Protection, Credential Stripping & Full V1+V2 Restore
  const protectedBackup = store.exportBackupJson({ password: 'VaultPassword2026!' });
  for (const secretField of [
    'pinHash',
    'pinSalt',
    'pinSaltBase64',
    'pinVerifierBase64',
    'recoverySaltBase64',
    'recoveryVerifierBase64'
  ]) {
    assert.ok(!protectedBackup.includes(secretField), `Backup leaked sensitive key: ${secretField}`);
  }

  // Wrong password rejected during inspection and restore
  assert.throws(() => store.inspectBackupJson(protectedBackup, 'WrongPassword'), /password/i);
  assert.throws(() => store.importBackupJson(protectedBackup, { password: 'WrongPassword' }), /password/i);

  // Correct password returns complete Android-parity BackupInspectionSummary
  const summary = store.inspectBackupJson(protectedBackup, 'VaultPassword2026!');
  assert.ok(summary.accountCount >= 1);
  assert.ok(summary.contentRecordCount >= 3);
  assert.ok(summary.galleryItemCount >= 2);
  assert.ok(summary.historyAndLogCount >= 5);

  const restoredStore = new ChevalierStore();
  restoredStore.importBackupJson(protectedBackup, { password: 'VaultPassword2026!' });
  assert.equal(restoredStore.getState().news.length, store.getState().news.length);
  assert.equal(restoredStore.getState().academicsV2.records.length, store.getState().academicsV2.records.length);

  // 6. Filterable Activity / Audit Log (Search, Section Filter, Chronological Sort Toggle)
  const allNewest = store.filterActivityLogs({ sortNewestFirst: true });
  const allOldest = store.filterActivityLogs({ sortNewestFirst: false });
  assert.ok(allNewest.length >= 5);
  assert.ok(allNewest[0].createdAtMillis >= allNewest[allNewest.length - 1].createdAtMillis);
  assert.ok(allOldest[0].createdAtMillis <= allOldest[allOldest.length - 1].createdAtMillis);

  const newsOnlyLogs = store.filterActivityLogs({ sectionKey: 'NEWS' });
  assert.ok(newsOnlyLogs.length >= 1);
  assert.ok(newsOnlyLogs.every((l) => l.sectionKey === 'NEWS' || l.section === 'NEWS'));

  const searchedLogs = store.filterActivityLogs({ query: 'Auditorium' });
  assert.ok(searchedLogs.length >= 1);
  assert.ok(searchedLogs.every((l) => `${l.actorName || ''} ${l.action || ''} ${l.section || ''} ${l.details || ''}`.toLowerCase().includes('auditorium')));

  const auditHtml = renderAdminSectionView(
    store,
    store.getState(),
    s,
    { activityLogQuery: 'Auditorium', activityLogSortNewest: true },
    'ACTIVITY_LOG'
  );
  assert.match(auditHtml, /activity_log_search_input/);
  assert.match(auditHtml, /activity_log_sort_btn/);
  assert.match(auditHtml, /activity_log_section_chips/);
});



