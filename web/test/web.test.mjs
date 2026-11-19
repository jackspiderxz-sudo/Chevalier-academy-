import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { buildWeb } from '../build.mjs';
import { createWebServer } from '../server.mjs';
import { getStrings, getAdminPpmStrings, PARENT_MODULES, ADMIN_PPM_MODULES } from '../js/i18n.js';
import { ChevalierStore } from '../js/store.js';
import { renderHomeView, renderParentDashboardView, renderParentModuleDetailView } from '../js/publicViews.js';
import { renderAdminDashboardView, renderAdminPpmModuleView } from '../js/adminViews.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(__dirname, '..');

test('Web build reuses Android brand drawables and fonts into web/dist/', () => {
  const result = buildWeb();
  assert.equal(result.copiedAssets, 5);
  assert.equal(result.copiedScripts, 6);

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
  assert.match(homeHtml, /home_section_quick_access/);
  assert.match(homeHtml, /home_section_updates/);
  assert.match(homeHtml, /home_section_gallery/);
  assert.match(homeHtml, /home_section_contact_social/);

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
