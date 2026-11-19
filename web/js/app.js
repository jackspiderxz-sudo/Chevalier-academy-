/**
 * Chevalier Academy — Responsive Web SPA Controller
 * Preserves finalized navigation:
 * - Primary 4 items: Home -> Parent -> Academics -> Contact
 * - Top-right overflow menu -> Gallery (plus Navigation Menu & Settings)
 * - Separated School Management / Admin Login inside Navigation Drawer/Sidebar
 */

import { ChevalierStore } from './store.js';
import { getStrings, getAdminPpmStrings, PARENT_MODULES, ADMIN_PPM_MODULES } from './i18n.js';
import { icon } from './icons.js';
import {
  esc,
  safeExternalUrl,
  renderOnboarding,
  renderHomeView,
  renderParentDashboardView,
  renderParentModuleDetailView,
  renderAcademicsView,
  renderCoursesView,
  renderAboutView,
  renderFacilitiesView,
  renderTransportView,
  renderUpdatesView,
  renderGalleryView,
  renderContactView,
  renderSocialMediaView,
  renderSettingsView
} from './publicViews.js';
import {
  renderAdminGateView,
  renderAdminDashboardView,
  renderAdminPpmModuleView,
  renderAdminCmsView
} from './adminViews.js';

const store = new ChevalierStore();

const ui = {
  route: 'home',
  drawerOpen: false,
  overflowOpen: false,
  heroSlideIndex: 0,
  parentSearchOpen: false,
  parentSearchQuery: '',
  updatesTab: 'all',
  galleryTab: 'photos',
  galleryCategory: 'all',
  adminAuthError: '',
  ppmExpanded: false,
  ppmScopes: {},
  modal: null // { type: 'news'|'announcement'|'lightbox'|'dialog', payload }
};

function parseHashRoute() {
  const raw = (window.location.hash || '#/home').replace(/^#\/?/, '').trim();
  return raw || 'home';
}

function navigate(route) {
  const clean = String(route || 'home').replace(/^\/+/, '');
  ui.drawerOpen = false;
  ui.overflowOpen = false;
  if (window.location.hash !== `#/${clean}`) {
    window.location.hash = `#/${clean}`;
  } else {
    ui.route = clean;
    render();
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function applyTheme(themePref) {
  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  const resolved = themePref === 'dark' || (themePref === 'system' && prefersDark) ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', resolved);
}

function getPrimaryTabForRoute(route) {
  if (route === 'home') return 'home';
  if (route === 'parent' || route.startsWith('parent/')) return 'parent';
  if (route === 'academics' || route === 'courses') return 'academics';
  if (route === 'contact' || route === 'social') return 'contact';
  return '';
}

function getScreenTitle(route, state, s) {
  if (route === 'home') return s.titleHome;
  if (route === 'parent') return s.parentPortalTitle;
  if (route.startsWith('parent/module/')) {
    const key = route.slice('parent/module/'.length);
    const mod = PARENT_MODULES.find((m) => m.key === key);
    return mod ? s[mod.titleKey] : s.titleParent;
  }
  if (route === 'academics') return s.titleAcademics;
  if (route === 'courses') return s.titleHigherSecondaryCourses;
  if (route === 'about') return s.titleAboutSchool;
  if (route === 'facilities') return s.titleFacilities;
  if (route === 'transport') return s.titleTransport;
  if (route === 'updates') return s.titleNewsAnnouncements;
  if (route === 'gallery') return s.titleGallery;
  if (route === 'contact') return s.titleContact;
  if (route === 'social') return s.titleSocialMedia;
  if (route === 'settings') return s.titleSettings;
  if (route === 'admin') return s.titleManagementEntry;
  if (route.startsWith('admin/ppm/')) {
    const key = route.slice('admin/ppm/'.length);
    const ppm = getAdminPpmStrings(state.preferences.language);
    return ppm.modules[key]?.title || ppm.sectionTitle;
  }
  if (route.startsWith('admin/cms/')) return s.titleManagementEntry;
  return s.titleHome;
}

function isPrimaryRoute(route) {
  return ['home', 'parent', 'academics', 'contact'].includes(route);
}

function renderNavDrawerBody(s, route) {
  const item = (target, label, iconName, extraClass = '') => `
    <button type="button" class="nav-link-btn ${route === target ? 'active' : ''} ${extraClass}" data-route="${target}">
      ${icon(iconName, 20)} <span>${esc(label)}</span>
    </button>
  `;

  return `
    <div class="nav-drawer-header">
      <img src="./assets/chevalier_emblem.png" alt="Chevalier Academy Emblem" />
      <div style="min-width:0;">
        <p class="nav-drawer-school">${esc(s.shortSchoolName)}</p>
        <p class="nav-drawer-motto">"${esc(s.officialMotto)}"</p>
      </div>
    </div>

    <nav aria-label="School Navigation">
      <div class="nav-group">
        <div class="nav-group-label">${esc(s.drawerGroupMain)}</div>
        ${item('home', s.navHome, 'home')}
        ${item('parent', s.navParent, 'parent')}
        ${item('academics', s.navAcademics, 'school')}
        ${item('contact', s.navContact, 'contact')}
      </div>
      <div class="nav-divider"></div>
      <div class="nav-group">
        <div class="nav-group-label">${esc(s.drawerGroupSchoolInfo)}</div>
        ${item('about', s.titleAboutSchool, 'info')}
        ${item('courses', s.titleHigherSecondaryCourses, 'book')}
      </div>
      <div class="nav-group">
        <div class="nav-group-label">${esc(s.drawerGroupCampusActivities)}</div>
        ${item('facilities', s.titleFacilities, 'apartment')}
        ${item('transport', s.titleTransport, 'bus')}
        ${item('gallery', s.titleGallery, 'gallery')}
      </div>
      <div class="nav-group">
        <div class="nav-group-label">${esc(s.drawerGroupUpdates)}</div>
        ${item('updates', s.titleNewsAnnouncements, 'campaign')}
      </div>
      <div class="nav-group">
        <div class="nav-group-label">${esc(s.drawerGroupConnect)}</div>
        ${item('social', s.titleSocialMedia, 'share')}
        ${item('settings', s.titleSettings, 'settings')}
      </div>
      <div class="nav-divider"></div>
      <div class="nav-group" style="padding-bottom:20px;">
        <div class="nav-group-label">${esc(s.drawerGroupManagement)}</div>
        ${item('admin', s.drawerManagementLogin, 'security', 'admin-entry')}
      </div>
    </nav>
  `;
}

function renderTopBar(route, state, s) {
  const isParentHome = route === 'parent';
  const showBack = !isPrimaryRoute(route);
  const title = getScreenTitle(route, state, s);
  const hasNotifications = state.announcements.length > 0 || state.news.length > 0;

  return `
    <header class="top-bar" data-testid="${isParentHome ? 'parent_portal_top_header' : 'app_top_bar'}">
      ${showBack
        ? `<button type="button" class="icon-btn" data-action="go-back" aria-label="${esc(s.backContentDescription)}">${icon('back', 22)}</button>`
        : `<button type="button" class="icon-btn mobile-only-menu-btn" data-action="open-drawer" aria-label="${esc(s.openDrawerDescription)}">${icon('menu', 22)}</button>`}

      <div class="top-bar-brand">
        <img src="./assets/chevalier_emblem.png" alt="${esc(s.shortSchoolName)}" class="top-bar-emblem" />
        <div class="top-bar-titles">
          <span class="top-bar-kicker">${esc(isParentHome ? s.parentPortalBranding : s.officialMotto)}</span>
          <h1 class="top-bar-title">${esc(title)}</h1>
        </div>
      </div>

      <div class="top-bar-actions">
        ${isParentHome ? `
          <button
            type="button"
            class="icon-btn ${ui.parentSearchOpen ? 'active' : ''}"
            data-action="toggle-parent-search"
            aria-label="${esc(s.parentSearchContentDesc)}"
            data-testid="parent_header_search_button"
          >
            ${icon(ui.parentSearchOpen ? 'close' : 'search', 22)}
          </button>
          <button
            type="button"
            class="icon-btn"
            data-route="updates"
            aria-label="${esc(s.parentNotificationsContentDesc)}"
            data-testid="parent_header_notification_button"
          >
            ${icon('notifications', 22)}
            ${hasNotifications ? '<span class="notif-dot" aria-hidden="true"></span>' : ''}
          </button>
        ` : ''}

        <button
          type="button"
          class="icon-btn"
          data-action="toggle-overflow"
          aria-label="${esc(s.openTopMenuDescription)}"
          aria-expanded="${ui.overflowOpen}"
          data-testid="top_bar_more_menu_button"
        >
          ${icon('moreVert', 22)}
        </button>

        <div class="overflow-menu ${ui.overflowOpen ? 'open' : ''}" role="menu" data-testid="top_bar_overflow_menu">
          <button type="button" class="overflow-item" role="menuitem" data-route="gallery" data-testid="top_bar_menu_gallery">
            ${icon('gallery', 20)} <span>${esc(s.titleGallery)}</span>
          </button>
          <button type="button" class="overflow-item" role="menuitem" data-action="open-drawer" data-testid="top_bar_menu_drawer">
            ${icon('menu', 20)} <span>${esc(s.parentMenuDrawerOption)}</span>
          </button>
          <button type="button" class="overflow-item" role="menuitem" data-route="settings" data-testid="top_bar_menu_settings">
            ${icon('settings', 20)} <span>${esc(s.titleSettings)}</span>
          </button>
        </div>
      </div>
    </header>
  `;
}

function renderBottomNav(route, s) {
  const activeTab = getPrimaryTabForRoute(route);
  const tabs = [
    { key: 'home', label: s.navHome, icon: 'home', testId: 'bottom_nav_home' },
    { key: 'parent', label: s.navParent, icon: 'parent', testId: 'bottom_nav_parent' },
    { key: 'academics', label: s.navAcademics, icon: 'school', testId: 'bottom_nav_academics' },
    { key: 'contact', label: s.navContact, icon: 'contact', testId: 'bottom_nav_contact' }
  ];
  return `
    <nav class="bottom-nav" aria-label="Primary Navigation">
      ${tabs.map((t) => `
        <button
          type="button"
          class="bottom-nav-item ${activeTab === t.key ? 'active' : ''}"
          data-route="${t.key}"
          data-testid="${t.testId}"
          aria-current="${activeTab === t.key ? 'page' : 'false'}"
        >
          <span class="bottom-nav-pill">${icon(t.icon, 22)}</span>
          <span>${esc(t.label)}</span>
        </button>
      `).join('')}
    </nav>
  `;
}

function renderTabletRail(route, s) {
  const activeTab = getPrimaryTabForRoute(route);
  const items = [
    { key: 'home', label: s.navHome, icon: 'home' },
    { key: 'parent', label: s.navParent, icon: 'parent' },
    { key: 'academics', label: s.navAcademics, icon: 'school' },
    { key: 'contact', label: s.navContact, icon: 'contact' }
  ];
  return `
    <aside class="tablet-rail" aria-label="Tablet Navigation Rail">
      <button type="button" class="icon-btn" data-action="open-drawer" aria-label="${esc(s.openDrawerDescription)}">${icon('menu', 22)}</button>
      ${items.map((t) => `
        <button type="button" class="rail-item ${activeTab === t.key ? 'active' : ''}" data-route="${t.key}">
          ${icon(t.icon, 22)}
          <span>${esc(t.label)}</span>
        </button>
      `).join('')}
      <div class="rail-spacer"></div>
      <button type="button" class="rail-item ${route === 'gallery' ? 'active' : ''}" data-route="gallery">
        ${icon('gallery', 20)} <span>${esc(s.navGallery)}</span>
      </button>
      <button type="button" class="rail-item ${route === 'settings' ? 'active' : ''}" data-route="settings">
        ${icon('settings', 20)} <span>${esc(s.titleSettings)}</span>
      </button>
    </aside>
  `;
}

function renderRouteBody(route, state, s) {
  if (route === 'home') return renderHomeView(state, s, ui);
  if (route === 'parent') return renderParentDashboardView(state, s, ui);
  if (route.startsWith('parent/module/')) {
    return renderParentModuleDetailView(state, s, route.slice('parent/module/'.length));
  }
  if (route === 'academics') return renderAcademicsView(state, s);
  if (route === 'courses') return renderCoursesView(state, s);
  if (route === 'about') return renderAboutView(state, s);
  if (route === 'facilities') return renderFacilitiesView(state, s);
  if (route === 'transport') return renderTransportView(state, s);
  if (route === 'updates') return renderUpdatesView(state, s, ui);
  if (route === 'gallery') return renderGalleryView(state, s, ui);
  if (route === 'contact') return renderContactView(state, s);
  if (route === 'social') return renderSocialMediaView(state, s);
  if (route === 'settings') return renderSettingsView(state, s);

  // Separated Admin Portal Routes
  if (route === 'admin' || route.startsWith('admin/')) {
    if (!store.isAdminAuthenticated) {
      return renderAdminGateView(store, s, ui);
    }
    if (route === 'admin') return renderAdminDashboardView(state, s, ui);
    if (route.startsWith('admin/ppm/')) {
      return renderAdminPpmModuleView(state, s, ui, route.slice('admin/ppm/'.length));
    }
    if (route.startsWith('admin/cms/')) {
      return renderAdminCmsView(state, s, route.slice('admin/cms/'.length));
    }
  }
  return renderHomeView(state, s, ui);
}

function renderModal(state, s) {
  const root = document.getElementById('modal-root');
  if (!ui.modal) {
    root.classList.remove('open');
    root.setAttribute('aria-hidden', 'true');
    root.innerHTML = '';
    return;
  }

  let html = '';
  if (ui.modal.type === 'news') {
    const item = state.news.find((n) => n.id === ui.modal.id);
    if (item) {
      html = `
        <div class="modal-card" role="dialog" aria-modal="true" aria-label="${esc(item.title)}">
          <div class="row-between" style="margin-bottom:10px;">
            <span class="badge badge-gold">${esc(s.articleDetailTitle)}</span>
            <button type="button" class="icon-btn" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 20)}</button>
          </div>
          ${item.imageUrl ? `<img src="${esc(item.imageUrl)}" alt="${esc(item.title)}" style="width:100%;max-height:240px;object-fit:cover;border-radius:12px;margin-bottom:12px;" />` : ''}
          <h2 class="section-title" style="margin-bottom:6px;">${esc(item.title)}</h2>
          <p class="muted text-xs" style="margin:0 0 12px;">${esc(item.publishedDateText)}</p>
          <p style="margin:0 0 18px;white-space:pre-line;">${esc(item.summary)}</p>
          <button type="button" class="btn btn-primary btn-full" data-action="close-modal">${esc(s.closeLabel)}</button>
        </div>
      `;
    }
  } else if (ui.modal.type === 'announcement') {
    const item = state.announcements.find((a) => a.id === ui.modal.id);
    if (item) {
      html = `
        <div class="modal-card" role="dialog" aria-modal="true" aria-label="${esc(item.title)}">
          <div class="row-between" style="margin-bottom:10px;">
            <span class="badge badge-burgundy">${esc(item.priorityLabel || s.announcementDetailTitle)}</span>
            <button type="button" class="icon-btn" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 20)}</button>
          </div>
          <h2 class="section-title" style="margin-bottom:6px;">${esc(item.title)}</h2>
          <p class="muted text-xs" style="margin:0 0 12px;">${esc(item.issuedDateText)}</p>
          <p style="margin:0 0 18px;white-space:pre-line;">${esc(item.message)}</p>
          <button type="button" class="btn btn-primary btn-full" data-action="close-modal">${esc(s.closeLabel)}</button>
        </div>
      `;
    }
  } else if (ui.modal.type === 'lightbox') {
    const photos = state.galleryMedia;
    const idx = Math.max(0, Math.min(ui.modal.index, photos.length - 1));
    const item = photos[idx];
    if (item) {
      html = `
        <div class="lightbox-card" role="dialog" aria-modal="true" aria-label="${esc(item.caption || s.titleGallery)}">
          <div class="row-between" style="padding:10px 16px;border-bottom:1px solid rgba(255,255,255,0.12);">
            <strong>${idx + 1} / ${photos.length}</strong>
            <button type="button" class="icon-btn" style="color:#fff;" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 22)}</button>
          </div>
          <div class="lightbox-stage">
            <img src="${esc(item.imageUrl)}" alt="${esc(item.caption || s.titleGallery)}" />
          </div>
          <div class="lightbox-footer">
            <div style="min-width:0;flex:1;">
              <div style="font-weight:700;">${esc(item.caption || item.album || s.titleGallery)}</div>
              ${item.youtubeUrl ? `<a href="${esc(safeExternalUrl(item.youtubeUrl))}" target="_blank" rel="noopener noreferrer" style="color:#EABF16;font-size:0.84rem;text-decoration:underline;">${esc(s.openYouTubeVideoLabel)}</a>` : ''}
            </div>
            <div class="row-wrap">
              <button type="button" class="icon-btn" style="color:#fff;" data-action="lightbox-prev" aria-label="${esc(s.previousPhotoDesc)}">${icon('chevronLeft', 24)}</button>
              <button type="button" class="icon-btn" style="color:#fff;" data-action="lightbox-next" aria-label="${esc(s.nextPhotoDesc)}">${icon('chevronRight', 24)}</button>
            </div>
          </div>
        </div>
      `;
    }
  } else if (ui.modal.type === 'dialog') {
    html = `
      <div class="modal-card" role="dialog" aria-modal="true" aria-label="${esc(ui.modal.title)}">
        <h2 class="section-title" style="margin-bottom:8px;">${esc(ui.modal.title)}</h2>
        ${ui.modal.subtitle ? `<p class="text-sm" style="color:var(--brand-burgundy);font-weight:700;margin:0 0 8px;">${esc(ui.modal.subtitle)}</p>` : ''}
        <p class="muted" style="margin:0 0 18px;">${esc(ui.modal.body)}</p>
        <button type="button" class="btn btn-primary btn-full" data-action="close-modal">${esc(s.closeLabel)}</button>
      </div>
    `;
  }

  root.innerHTML = html;
  root.classList.add('open');
  root.setAttribute('aria-hidden', 'false');
}

function render() {
  const state = store.getState();
  const s = getStrings(state.preferences.language);
  applyTheme(state.preferences.theme);
  document.documentElement.lang = state.preferences.language;

  const app = document.getElementById('app');

  if (!state.preferences.hasCompletedOnboarding) {
    app.innerHTML = renderOnboarding(state, s);
    renderModal(state, s);
    return;
  }

  const drawerHtml = renderNavDrawerBody(s, ui.route);

  app.innerHTML = `
    <div class="app-shell">
      <aside class="desktop-sidebar" aria-label="Desktop Navigation">${drawerHtml}</aside>
      ${renderTabletRail(ui.route, s)}

      <div class="drawer-backdrop ${ui.drawerOpen ? 'open' : ''}" data-action="close-drawer"></div>
      <aside class="mobile-drawer ${ui.drawerOpen ? 'open' : ''}" aria-label="Mobile Navigation">${drawerHtml}</aside>

      <div class="shell-main">
        ${renderTopBar(ui.route, state, s)}
        <main id="main-content" class="main-content">
          ${renderRouteBody(ui.route, state, s)}
        </main>
      </div>

      ${renderBottomNav(ui.route, s)}
    </div>
  `;

  renderModal(state, s);

  // Keep focus inside Parent search input when active
  if (ui.route === 'parent' && ui.parentSearchOpen) {
    const searchEl = document.getElementById('parent-search-input');
    if (searchEl) {
      searchEl.focus();
      searchEl.setSelectionRange(searchEl.value.length, searchEl.value.length);
    }
  }
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

/* ==========================================================================
   Global Event Delegation (Clicks, Forms, Search, Keyboard)
   ========================================================================== */
document.addEventListener('click', (event) => {
  const target = event.target.closest('[data-route], [data-action]');
  const state = store.getState();
  const s = getStrings(state.preferences.language);

  if (!target) {
    if (ui.overflowOpen && !event.target.closest('.top-bar-actions')) {
      ui.overflowOpen = false;
      render();
    }
    return;
  }

  if (target.dataset.route) {
    navigate(target.dataset.route);
    return;
  }

  const action = target.dataset.action;
  if (action === 'pick-onboarding-lang') {
    store.setLanguage(target.dataset.lang);
  } else if (action === 'complete-onboarding') {
    store.completeOnboarding(state.preferences.language);
    navigate('home');
  } else if (action === 'open-drawer') {
    ui.drawerOpen = true;
    ui.overflowOpen = false;
    render();
  } else if (action === 'close-drawer') {
    ui.drawerOpen = false;
    render();
  } else if (action === 'toggle-overflow') {
    ui.overflowOpen = !ui.overflowOpen;
    render();
  } else if (action === 'go-back') {
    if (ui.route.startsWith('parent/module/')) navigate('parent');
    else if (ui.route.startsWith('admin/ppm/') || ui.route.startsWith('admin/cms/')) navigate('admin');
    else if (window.history.length > 1) window.history.back();
    else navigate('home');
  } else if (action === 'hero-slide') {
    ui.heroSlideIndex = Number(target.dataset.index || 0);
    render();
  } else if (action === 'toggle-parent-search') {
    ui.parentSearchOpen = !ui.parentSearchOpen;
    if (!ui.parentSearchOpen) ui.parentSearchQuery = '';
    render();
  } else if (action === 'clear-parent-search') {
    ui.parentSearchQuery = '';
    render();
  } else if (action === 'updates-tab') {
    ui.updatesTab = target.dataset.tab;
    render();
  } else if (action === 'gallery-tab') {
    ui.galleryTab = target.dataset.tab;
    render();
  } else if (action === 'gallery-cat') {
    ui.galleryCategory = target.dataset.cat;
    render();
  } else if (action === 'set-language') {
    store.setLanguage(target.dataset.lang);
  } else if (action === 'set-theme') {
    store.setTheme(target.dataset.theme);
  } else if (action === 'open-news') {
    ui.modal = { type: 'news', id: Number(target.dataset.id) };
    renderModal(state, s);
  } else if (action === 'open-announcement') {
    ui.modal = { type: 'announcement', id: Number(target.dataset.id) };
    renderModal(state, s);
  } else if (action === 'open-lightbox') {
    ui.modal = { type: 'lightbox', index: Number(target.dataset.index || 0) };
    renderModal(state, s);
  } else if (action === 'lightbox-prev' && ui.modal?.type === 'lightbox') {
    const n = state.galleryMedia.length;
    ui.modal.index = (ui.modal.index - 1 + n) % n;
    renderModal(state, s);
  } else if (action === 'lightbox-next' && ui.modal?.type === 'lightbox') {
    const n = state.galleryMedia.length;
    ui.modal.index = (ui.modal.index + 1) % n;
    renderModal(state, s);
  } else if (action === 'close-modal') {
    ui.modal = null;
    renderModal(state, s);
  } else if (action === 'toggle-ppm') {
    ui.ppmExpanded = !ui.ppmExpanded;
    render();
  } else if (action === 'ppm-scope') {
    const modKey = target.dataset.module;
    const mod = ADMIN_PPM_MODULES.find((m) => m.key === modKey);
    const prev = ui.ppmScopes[modKey] || { scope: mod?.defaultBulkScope ? 'bulk' : 'individual', classIdx: 0, studentIdx: 0 };
    ui.ppmScopes[modKey] = { ...prev, scope: target.dataset.scope };
    render();
  } else if (action === 'ppm-target') {
    const modKey = target.dataset.module;
    const prev = ui.ppmScopes[modKey] || { scope: 'individual', classIdx: 0, studentIdx: 0 };
    const idx = Number(target.dataset.index || 0);
    ui.ppmScopes[modKey] = prev.scope === 'bulk' ? { ...prev, classIdx: idx } : { ...prev, studentIdx: idx };
    render();
  } else if (action === 'ppm-preview') {
    const ppm = getAdminPpmStrings(state.preferences.language);
    ui.modal = {
      type: 'dialog',
      title: `${ppm.previewDialogTitle}: ${target.dataset.module}`,
      subtitle: target.dataset.target,
      body: ppm.previewDialogBody
    };
    renderModal(state, s);
  } else if (action === 'ppm-trial-action') {
    const ppm = getAdminPpmStrings(state.preferences.language);
    const label = target.dataset.label;
    ui.modal = {
      type: 'dialog',
      title: `${ppm.trialActionDialogTitle} — ${label}`,
      body: ppm.trialActionDialogTemplate
        .replace('{action}', label)
        .replace('{module}', target.dataset.module)
        .replace('{target}', target.dataset.target)
    };
    renderModal(state, s);
  } else if (action === 'admin-logout') {
    store.logoutAdmin();
    ui.adminAuthError = '';
    navigate('home');
  } else if (action === 'delete-announcement') {
    store.deleteAnnouncement(Number(target.dataset.id));
  } else if (action === 'delete-news') {
    store.deleteNews(Number(target.dataset.id));
  } else if (action === 'export-backup') {
    const blob = new Blob([store.exportBackupJson()], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chevalier-academy-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }
});

document.addEventListener('input', (event) => {
  if (event.target.id === 'parent-search-input') {
    ui.parentSearchQuery = event.target.value;
    render();
  }
});

document.addEventListener('change', async (event) => {
  if (event.target.id === 'import-backup-input' && event.target.files?.[0]) {
    try {
      const text = await event.target.files[0].text();
      store.importBackupJson(text);
      ui.modal = { type: 'dialog', title: 'Backup Restored', body: 'Verified school records have been restored successfully.' };
      render();
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Restore Failed', body: err.message || 'Invalid backup file.' };
      render();
    }
  }
});

document.addEventListener('submit', async (event) => {
  const form = event.target;
  event.preventDefault();
  const fd = new FormData(form);

  if (form.id === 'admin-auth-form') {
    const mode = form.dataset.mode;
    const pin = String(fd.get('pin') || '');
    ui.adminAuthError = '';
    if (mode === 'setup') {
      try {
        await store.setupAdminPin(pin, String(fd.get('displayName') || 'School Administrator'));
        render();
      } catch (err) {
        ui.adminAuthError = err.message;
        render();
      }
    } else {
      const res = await store.verifyAdminPin(pin);
      if (!res.ok) ui.adminAuthError = res.error;
      render();
    }
  } else if (form.id === 'cms-profile-form') {
    store.updateProfile({
      welcomeIntroduction: String(fd.get('welcomeIntroduction') || '').trim(),
      phone: String(fd.get('phone') || '').trim(),
      email: String(fd.get('email') || '').trim(),
      website: String(fd.get('website') || '').trim(),
      officeHours: String(fd.get('officeHours') || '').trim(),
      principalName: String(fd.get('principalName') || '').trim(),
      history: String(fd.get('history') || '').trim(),
      vision: String(fd.get('vision') || '').trim(),
      mission: String(fd.get('mission') || '').trim()
    });
    ui.modal = { type: 'dialog', title: 'Profile Published', body: 'School profile and contact information updated across the live public website.' };
    render();
  } else if (form.id === 'cms-academic-form') {
    store.addAcademicProgram({
      title: String(fd.get('title') || '').trim(),
      level: String(fd.get('level') || '').trim(),
      description: String(fd.get('description') || '').trim()
    });
    form.reset();
  } else if (form.classList.contains('cms-course-form')) {
    store.updateCourse(Number(form.dataset.id), {
      title: String(fd.get('title') || '').trim(),
      medium: String(fd.get('medium') || '').trim(),
      subjects: String(fd.get('subjects') || '').trim(),
      verified: Boolean(String(fd.get('subjects') || '').trim())
    });
    ui.modal = { type: 'dialog', title: 'Course Updated', body: 'Higher Secondary stream saved.' };
    render();
  } else if (form.id === 'cms-announcement-form') {
    store.addAnnouncement({
      title: String(fd.get('title') || '').trim(),
      priorityLabel: String(fd.get('priorityLabel') || '').trim(),
      message: String(fd.get('message') || '').trim()
    });
    form.reset();
  } else if (form.id === 'cms-news-form') {
    store.addNews({
      title: String(fd.get('title') || '').trim(),
      summary: String(fd.get('summary') || '').trim(),
      imageUrl: String(fd.get('imageUrl') || '').trim(),
      isFeatured: store.getState().news.length === 0
    });
    form.reset();
  } else if (form.id === 'cms-hero-form') {
    const file = fd.get('imageFile');
    let imageUrl = String(fd.get('imageUrl') || '').trim();
    if (!imageUrl && file && file.size > 0) imageUrl = await readFileAsDataUrl(file);
    if (imageUrl) {
      store.addHeroPhoto({ imageUrl, caption: String(fd.get('caption') || '').trim() });
      form.reset();
    }
  } else if (form.id === 'cms-gallery-form') {
    const file = fd.get('imageFile');
    let imageUrl = String(fd.get('imageUrl') || '').trim();
    if (!imageUrl && file && file.size > 0) imageUrl = await readFileAsDataUrl(file);
    if (imageUrl) {
      store.addGalleryMedia({
        imageUrl,
        album: String(fd.get('album') || 'Campus Highlights').trim(),
        category: String(fd.get('category') || 'Campus').trim(),
        caption: String(fd.get('caption') || '').trim(),
        youtubeUrl: String(fd.get('youtubeUrl') || '').trim()
      });
      form.reset();
    }
  } else if (form.id === 'cms-facility-form') {
    store.addFacility({
      name: String(fd.get('name') || '').trim(),
      category: String(fd.get('category') || 'Campus').trim(),
      description: String(fd.get('description') || '').trim()
    });
    form.reset();
  } else if (form.id === 'cms-transport-form') {
    store.addTransportRoute({
      routeNumber: String(fd.get('routeNumber') || '').trim(),
      routeName: String(fd.get('routeName') || '').trim(),
      stops: String(fd.get('stops') || '').trim(),
      timings: String(fd.get('timings') || '').trim()
    });
    form.reset();
  } else if (form.classList.contains('cms-social-form')) {
    const url = String(fd.get('url') || '').trim();
    store.updateSocialLink(Number(form.dataset.id), {
      handle: String(fd.get('handle') || '').trim(),
      url,
      active: Boolean(url)
    });
    ui.modal = { type: 'dialog', title: 'Channel Updated', body: 'Official social media channel saved.' };
    render();
  }
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    if (ui.modal) {
      ui.modal = null;
      renderModal(store.getState(), getStrings(store.getState().preferences.language));
    } else if (ui.drawerOpen || ui.overflowOpen) {
      ui.drawerOpen = false;
      ui.overflowOpen = false;
      render();
    }
  } else if (ui.modal?.type === 'lightbox') {
    const n = store.getState().galleryMedia.length;
    if (n > 0 && event.key === 'ArrowRight') {
      ui.modal.index = (ui.modal.index + 1) % n;
      renderModal(store.getState(), getStrings(store.getState().preferences.language));
    } else if (n > 0 && event.key === 'ArrowLeft') {
      ui.modal.index = (ui.modal.index - 1 + n) % n;
      renderModal(store.getState(), getStrings(store.getState().preferences.language));
    }
  }
});

window.addEventListener('hashchange', () => {
  ui.route = parseHashRoute();
  ui.drawerOpen = false;
  ui.overflowOpen = false;
  render();
});

store.subscribe(() => render());

// Boot
ui.route = parseHashRoute();
render();
