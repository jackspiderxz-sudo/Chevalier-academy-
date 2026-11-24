/**
 * Chevalier Academy — Responsive Web SPA Controller
 * Preserves finalized navigation:
 * - Primary 4 items: Home -> Parent -> Academics -> Contact
 * - Top-right overflow menu -> Gallery (plus Navigation Menu & Settings)
 * - Separated School Management / Admin Login inside Navigation Drawer/Sidebar
 */

import {
  ChevalierStore,
  MANAGEMENT_ROLES,
  PERMISSION_ACTIONS,
  MANAGEMENT_SECTIONS,
  SCHOOL_POST_TYPES,
  SCHOOL_POST_STATUS,
  buildDefaultPermissionsForRole
} from './store.js';
import { getStrings, getAdminPpmStrings, PARENT_MODULES, ADMIN_PPM_MODULES, ACADEMICS_V2_CATEGORIES } from './i18n.js';
import { icon } from './icons.js';
import { compressImageFileForStorage, parseYouTubeUrl } from './mediaUtils.js';
import {
  esc,
  safeExternalUrl,
  renderOnboarding,
  renderHomeView,
  renderBookmarksView,
  renderParentDashboardView,
  renderParentModuleDetailView,
  renderAcademicsHubView,
  renderAcademyCategoryDetailView,
  renderAcademicsView,
  renderCoursesView,
  renderAboutView,
  renderFacilitiesView,
  renderTransportView,
  renderUpdatesView,
  renderGalleryView,
  renderContactView,
  renderSocialMediaView,
  renderSettingsView,
  getActiveHighlightsFromState,
  renderHighlightCard
} from './publicViews.js';
import {
  renderAdminGateView,
  renderAdminDashboardView,
  renderAdminHubView,
  renderAdminSectionView,
  renderAdminSearchView,
  renderAdminNotificationsView,
  renderAdminPpmModuleView,
  renderAdminCmsView,
  renderAdminV2AcademicsHubView,
  renderAdminV2CategoryManagementView,
  renderAdminV2PreviewAndPublishView,
  renderSchoolOwnerControlCenterView
} from './adminViews.js';

const store = new ChevalierStore();

const ui = {
  route: 'home',
  drawerOpen: false,
  overflowOpen: false,
  heroSlideIndex: 0,
  selectedFeedCategory: null,
  activeVideoPostId: null,
  parentSearchOpen: false,
  parentSearchQuery: '',
  academySearchQuery: '',
  expandedAcademySections: {},
  updatesTab: 'all',
  galleryTab: 'photos',
  galleryCategory: 'all',
  adminAuthError: '',
  selectedLoginUsername: '',
  showRecoverPinDialog: false,
  rotatedRecoveryNotice: '',
  adminMenuOpen: false,
  showAdminHeroEditor: false,
  adminSearchQuery: '',
  staffDialogMode: null,
  editingStaffId: null,
  staffDraftRole: MANAGEMENT_ROLES.STAFF,
  staffDraftUsername: '',
  staffDraftDisplayName: '',
  staffDraftIsActive: true,
  staffDraftPermissions: null,
  staffFeedback: '',
  staffError: '',
  ppmExpanded: false,
  ppmScopes: {},
  approvalFilterTab: 'ALL',
  inspectedVersionId: null,
  editingMediaStudioId: null,
  activityLogQuery: '',
  activityLogSection: 'ALL',
  activityLogSortNewest: true,
  backupPassword: '',
  backupInspectionReport: null,
  pendingBackupRawJson: '',
  v2SelectedSectionKey: null,
  v2EditingRecordId: null,
  v2PreviewRecordId: null,
  previewPublishTab: 'review',
  ownerControlSection: 'OWNER_PROFILE_CONTACT',
  modal: null // { type: 'news'|'announcement'|'lightbox'|'post_lightbox'|'youtube'|'all_highlights'|'dialog', payload }
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
  if (route === 'home' || route === 'bookmarks') return 'home';
  if (route === 'parent' || route.startsWith('parent/')) return 'parent';
  if (route === 'academics' || route === 'academics/overview' || route.startsWith('academics/category/') || route === 'courses') return 'academics';
  if (route === 'contact' || route === 'social') return 'contact';
  return '';
}

function getScreenTitle(route, state, s) {
  if (route === 'home') return s.titleHome;
  if (route === 'bookmarks') return s.titleBookmarks || 'Bookmarks';
  if (route === 'parent') return s.parentPortalTitle;
  if (route.startsWith('parent/module/')) {
    const key = route.slice('parent/module/'.length);
    const mod = PARENT_MODULES.find((m) => m.key === key);
    return mod ? s[mod.titleKey] : s.titleParent;
  }
  if (route === 'academics' || route === 'academics/overview') return s.titleAcademics;
  if (route.startsWith('academics/category/')) {
    const catId = route.slice('academics/category/'.length);
    const cat = ACADEMICS_V2_CATEGORIES.find((c) => c.routeId === catId);
    return cat ? cat.title : s.titleAcademics;
  }
  if (route === 'courses') return s.titleHigherSecondaryCourses;
  if (route === 'about') return s.titleAboutSchool;
  if (route === 'facilities') return s.titleFacilities;
  if (route === 'transport') return s.titleTransport;
  if (route === 'updates' || route === 'news' || route === 'announcements') return s.titleNewsAnnouncements;
  if (route === 'gallery') return s.titleGallery;
  if (route === 'contact') return s.titleContact;
  if (route === 'social') return s.titleSocialMedia;
  if (route === 'settings') return s.titleSettings;
  if (route === 'admin') return s.titleManagementEntry;
  if (route === 'admin/v2-academics') return 'Academics V2 Management';
  if (route.startsWith('admin/v2-academics/')) {
    const catId = route.slice('admin/v2-academics/'.length);
    const cat = ACADEMICS_V2_CATEGORIES.find((c) => c.routeId === catId);
    return cat ? `${cat.title} (V2)` : 'Academics V2 Management';
  }
  if (route === 'admin/preview-publish') return 'Preview & Publish Hub';
  if (route === 'admin/owner-control-center') return 'School Owner – Control Center';
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
        ${item('bookmarks', s.titleBookmarks || 'Bookmarks', 'bookmark')}
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
          <button type="button" class="overflow-item" role="menuitem" data-route="bookmarks" data-testid="top_bar_menu_bookmarks">
            ${icon('bookmark', 20)} <span>${esc(s.titleBookmarks || 'Bookmarks')}</span>
          </button>
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
  if (route === 'bookmarks') return renderBookmarksView(state, s, ui);
  if (route === 'parent') return renderParentDashboardView(state, s, ui);
  if (route.startsWith('parent/module/')) {
    return renderParentModuleDetailView(state, s, route.slice('parent/module/'.length));
  }
  if (route === 'academics') return renderAcademicsHubView(state, s, ui);
  if (route === 'academics/overview') return renderAcademicsView(state, s);
  if (route.startsWith('academics/category/')) {
    return renderAcademyCategoryDetailView(state, s, ui, route.slice('academics/category/'.length));
  }
  if (route === 'courses') return renderCoursesView(state, s);
  if (route === 'about') return renderAboutView(state, s);
  if (route === 'facilities') return renderFacilitiesView(state, s);
  if (route === 'transport') return renderTransportView(state, s);
  if (route === 'updates') return renderUpdatesView(state, s, ui);
  if (route === 'news') return renderUpdatesView(state, s, { ...ui, updatesTab: 'news' });
  if (route === 'announcements') return renderUpdatesView(state, s, { ...ui, updatesTab: 'announcements' });
  if (route === 'gallery') return renderGalleryView(state, s, ui);
  if (route === 'contact') return renderContactView(state, s);
  if (route === 'social') return renderSocialMediaView(state, s);
  if (route === 'settings') return renderSettingsView(state, s);

  // Separated Admin Portal Routes (Base 3 + Base 4 Complete Staff, V2 Academics & Owner Control)
  if (route === 'admin' || route.startsWith('admin/')) {
    if (!store.isAdminAuthenticated || store.pendingRecoveryCode) {
      return renderAdminGateView(store, s, ui);
    }
    if (route === 'admin') return renderAdminDashboardView(state, s, ui, store);
    if (route === 'admin/search') return renderAdminSearchView(store, state, s, ui);
    if (route === 'admin/notifications') return renderAdminNotificationsView(store, state, s, ui);
    if (route === 'admin/v2-academics') return renderAdminV2AcademicsHubView(store, state, s, ui);
    if (route.startsWith('admin/v2-academics/')) {
      return renderAdminV2CategoryManagementView(store, state, s, ui, route.slice('admin/v2-academics/'.length));
    }
    if (route === 'admin/preview-publish') return renderAdminV2PreviewAndPublishView(store, state, s, ui);
    if (route === 'admin/owner-control-center') return renderSchoolOwnerControlCenterView(store, state, s, ui);
    if (route.startsWith('admin/hub/')) {
      return renderAdminHubView(store, state, s, ui, route.slice('admin/hub/'.length));
    }
    if (route.startsWith('admin/section/')) {
      return renderAdminSectionView(store, state, s, ui, route.slice('admin/section/'.length));
    }
    if (route.startsWith('admin/ppm/')) {
      return renderAdminPpmModuleView(state, s, ui, route.slice('admin/ppm/'.length), store);
    }
    if (route.startsWith('admin/cms/')) {
      return renderAdminCmsView(state, s, route.slice('admin/cms/'.length), store, ui);
    }
    return renderAdminDashboardView(state, s, ui, store);
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
          <p style="margin:0 0 18px;white-space:pre-line;">${esc(item.fullBody || item.summary)}</p>
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
          ${item.imageUrl ? `<img src="${esc(item.imageUrl)}" alt="${esc(item.title)}" style="width:100%;max-height:240px;object-fit:cover;border-radius:12px;margin-bottom:12px;" />` : ''}
          <h2 class="section-title" style="margin-bottom:6px;">${esc(item.title)}</h2>
          <p class="muted text-xs" style="margin:0 0 12px;">${esc(item.issuedDateText)}</p>
          <p style="margin:0 0 18px;white-space:pre-line;">${esc(item.message)}</p>
          <button type="button" class="btn btn-primary btn-full" data-action="close-modal">${esc(s.closeLabel)}</button>
        </div>
      `;
    }
  } else if (ui.modal.type === 'youtube') {
    const parsed = parseYouTubeUrl(ui.modal.url);
    const titleText = ui.modal.title || s.youtubeVideoLabel;
    const externalUrl = parsed?.externalFallbackUrl || safeExternalUrl(ui.modal.url);
    html = `
      <div class="modal-card" style="max-width:${parsed?.isShort ? '420px' : '740px'};" role="dialog" aria-modal="true" aria-label="${esc(titleText)}" data-testid="youtube_player_modal">
        <div class="row-between" style="margin-bottom:10px;">
          <span class="badge badge-burgundy">${icon('play', 14)} ${esc(parsed?.isShort ? 'YouTube Shorts' : s.youtubeVideoLabel)}</span>
          <button type="button" class="icon-btn" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 20)}</button>
        </div>
        <h2 class="section-title" style="margin-bottom:10px;font-size:1.05rem;">${esc(titleText)}</h2>
        ${parsed ? `
          <div class="${parsed.isShort ? 'youtube-short-stage' : 'post-video-stage'}">
            <iframe
              src="${esc(parsed.clickToPlayEmbedUrl)}"
              title="${esc(titleText)}"
              class="youtube-embed-iframe"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowfullscreen
            ></iframe>
          </div>
        ` : ''}
        <div class="row-between" style="margin-top:14px;">
          ${externalUrl ? `<a href="${esc(externalUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-outline">${esc(s.openYouTubeVideoLabel)} ${icon('openInNew', 16)}</a>` : '<span></span>'}
          <button type="button" class="btn btn-primary" data-action="close-modal">${esc(s.closeLabel)}</button>
        </div>
      </div>
    `;
  } else if (ui.modal.type === 'all_highlights') {
    const activeHighlights = getActiveHighlightsFromState(state);
    html = `
      <div class="modal-card" style="max-width:680px;" role="dialog" aria-modal="true" aria-label="${esc(s.socialAllHighlightsTitle || 'All Active Highlights')}" data-testid="all_highlights_modal">
        <div class="row-between" style="margin-bottom:12px;">
          <div class="row-wrap" style="gap:8px;">
            <h2 class="section-title" style="margin:0;">${esc(s.socialAllHighlightsTitle || 'All Active Highlights')}</h2>
            <span class="badge badge-burgundy">24H</span>
          </div>
          <button type="button" class="icon-btn" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 20)}</button>
        </div>
        <div class="highlights-modal-grid">
          ${activeHighlights.map((hl) => renderHighlightCard(hl, s)).join('')}
        </div>
        <button type="button" class="btn btn-outline btn-full" style="margin-top:16px;" data-action="close-modal">${esc(s.closeLabel)}</button>
      </div>
    `;
  } else if (ui.modal.type === 'post_lightbox') {
    const post = (state.schoolPosts || []).find((p) => String(p.id) === String(ui.modal.postId));
    const photos = Array.isArray(post?.photoUrls) ? post.photoUrls : [];
    const idx = Math.max(0, Math.min(ui.modal.index || 0, Math.max(0, photos.length - 1)));
    const imgUrl = photos[idx];
    if (post && imgUrl) {
      html = `
        <div class="lightbox-card" role="dialog" aria-modal="true" aria-label="${esc(post.title)}">
          <div class="row-between" style="padding:10px 16px;border-bottom:1px solid rgba(255,255,255,0.12);">
            <strong>${idx + 1} / ${photos.length}</strong>
            <button type="button" class="icon-btn" style="color:#fff;" data-action="close-modal" aria-label="${esc(s.closeLabel)}">${icon('close', 22)}</button>
          </div>
          <div class="lightbox-stage">
            <img src="${esc(imgUrl)}" alt="${esc(post.title)}" />
          </div>
          <div class="lightbox-footer">
            <div style="min-width:0;flex:1;">
              <div style="font-weight:700;">${esc(post.title)}</div>
            </div>
            ${photos.length > 1 ? `
              <div class="row-wrap">
                <button type="button" class="icon-btn" style="color:#fff;" data-action="post-lightbox-prev" aria-label="${esc(s.previousPhotoDesc)}">${icon('chevronLeft', 24)}</button>
                <button type="button" class="icon-btn" style="color:#fff;" data-action="post-lightbox-next" aria-label="${esc(s.nextPhotoDesc)}">${icon('chevronRight', 24)}</button>
              </div>
            ` : ''}
          </div>
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

  // Keep focus inside Parent or Admin search input when active
  if (ui.route === 'parent' && ui.parentSearchOpen) {
    const searchEl = document.getElementById('parent-search-input');
    if (searchEl) {
      searchEl.focus();
      searchEl.setSelectionRange(searchEl.value.length, searchEl.value.length);
    }
  } else if (ui.route === 'admin/search') {
    const adminSearchEl = document.getElementById('admin-global-search-input');
    if (adminSearchEl) {
      adminSearchEl.focus();
      adminSearchEl.setSelectionRange(adminSearchEl.value.length, adminSearchEl.value.length);
    }
  }
}

/* ==========================================================================
   Global Event Delegation (Clicks, Forms, Search, Keyboard)
   ========================================================================== */
document.addEventListener('click', (event) => {
  if (store.isAdminAuthenticated) {
    store.onAdminInteraction();
  }
  const target = event.target.closest('[data-route], [data-action]');
  const state = store.getState();
  const s = getStrings(state.preferences.language);

  if (!target) {
    let shouldRender = false;
    if (ui.overflowOpen && !event.target.closest('.top-bar-actions')) {
      ui.overflowOpen = false;
      shouldRender = true;
    }
    if (ui.adminMenuOpen && !event.target.closest('[data-testid="mgmt_top_menu_btn"]')) {
      ui.adminMenuOpen = false;
      shouldRender = true;
    }
    if (shouldRender) render();
    return;
  }

  if (target.dataset.route) {
    ui.adminMenuOpen = false;
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
    else if (ui.route.startsWith('academics/category/')) navigate('academics');
    else if (ui.route.startsWith('admin/v2-academics/')) {
      navigate('admin/v2-academics');
    } else if (
      ui.route.startsWith('admin/ppm/') ||
      ui.route.startsWith('admin/cms/') ||
      ui.route.startsWith('admin/hub/') ||
      ui.route.startsWith('admin/section/') ||
      ui.route === 'admin/v2-academics' ||
      ui.route === 'admin/preview-publish' ||
      ui.route === 'admin/owner-control-center' ||
      ui.route === 'admin/search' ||
      ui.route === 'admin/notifications'
    ) {
      navigate('admin');
    } else if (window.history.length > 1) window.history.back();
    else navigate('home');
  } else if (action === 'hero-slide') {
    ui.heroSlideIndex = Number(target.dataset.index || 0);
    render();
  } else if (action === 'feed-category') {
    const cat = target.dataset.category || '';
    ui.selectedFeedCategory = cat ? cat : null;
    render();
  } else if (action === 'toggle-like-post') {
    store.toggleLikePost(target.dataset.postId);
  } else if (action === 'toggle-bookmark-post') {
    store.toggleBookmarkPost(target.dataset.postId);
  } else if (action === 'play-post-video') {
    ui.activeVideoPostId = target.dataset.postId;
    render();
  } else if (action === 'play-youtube') {
    ui.modal = {
      type: 'youtube',
      url: target.dataset.url || '',
      title: target.dataset.title || ''
    };
    renderModal(state, s);
  } else if (action === 'open-all-highlights') {
    ui.modal = { type: 'all_highlights' };
    renderModal(state, s);
  } else if (action === 'open-post-lightbox') {
    ui.modal = {
      type: 'post_lightbox',
      postId: target.dataset.postId,
      index: Number(target.dataset.index || 0)
    };
    renderModal(state, s);
  } else if (action === 'post-lightbox-prev' && ui.modal?.type === 'post_lightbox') {
    const post = (state.schoolPosts || []).find((p) => String(p.id) === String(ui.modal.postId));
    const n = post?.photoUrls?.length || 1;
    ui.modal.index = (ui.modal.index - 1 + n) % n;
    renderModal(state, s);
  } else if (action === 'post-lightbox-next' && ui.modal?.type === 'post_lightbox') {
    const post = (state.schoolPosts || []).find((p) => String(p.id) === String(ui.modal.postId));
    const n = post?.photoUrls?.length || 1;
    ui.modal.index = (ui.modal.index + 1) % n;
    renderModal(state, s);
  } else if (action === 'toggle-academy-section') {
    const catId = target.dataset.categoryId || 'academics';
    const secId = target.dataset.sectionId || '';
    const key = `${catId}::${secId}`;
    const current = key in ui.expandedAcademySections ? Boolean(ui.expandedAcademySections[key]) : false;
    ui.expandedAcademySections[key] = !current;
    render();
  } else if (action === 'clear-academy-search') {
    ui.academySearchQuery = '';
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
    ui.adminMenuOpen = false;
    navigate('home');
  } else if (action === 'confirm-recovery-saved') {
    store.confirmRecoveryKeySaved();
    render();
  } else if (action === 'select-login-user') {
    ui.selectedLoginUsername = target.dataset.username || '';
    ui.adminAuthError = '';
    render();
  } else if (action === 'toggle-recover-pin') {
    ui.showRecoverPinDialog = !ui.showRecoverPinDialog;
    ui.adminAuthError = '';
    render();
  } else if (action === 'toggle-admin-menu') {
    ui.adminMenuOpen = !ui.adminMenuOpen;
    render();
  } else if (action === 'open-admin-hero-editor') {
    ui.showAdminHeroEditor = true;
    ui.adminMenuOpen = false;
    render();
  } else if (action === 'close-admin-hero-editor') {
    ui.showAdminHeroEditor = false;
    render();
  } else if (action === 'clear-admin-search') {
    ui.adminSearchQuery = '';
    render();
  } else if (action === 'toggle-device-alerts') {
    store.setDeviceAlertsEnabled(Boolean(target.checked));
  } else if (action === 'mark-notification-read') {
    store.markNotificationRead(Number(target.dataset.id));
  } else if (action === 'notifications-mark-all-read') {
    store.markAllNotificationsRead();
  } else if (action === 'notifications-clear-all') {
    store.clearNotifications();
  } else if (action === 'open-create-staff') {
    ui.staffDialogMode = 'create';
    ui.editingStaffId = null;
    ui.staffDraftRole = 'STAFF';
    ui.staffDraftPermissions = buildDefaultPermissionsForRole('STAFF');
    ui.staffDialogError = '';
    render();
  } else if (action === 'open-edit-staff') {
    const accountId = Number(target.dataset.id);
    const acc = (state.managementAccounts || []).find((a) => Number(a.id) === accountId);
    if (acc) {
      ui.staffDialogMode = 'edit';
      ui.editingStaffId = acc.id;
      ui.staffDraftRole = acc.role;
      ui.staffDraftPermissions = JSON.parse(JSON.stringify(acc.permissions || buildDefaultPermissionsForRole(acc.role)));
      ui.staffDialogError = '';
      render();
    }
  } else if (action === 'close-staff-dialog') {
    ui.staffDialogMode = null;
    ui.editingStaffId = null;
    ui.staffDialogError = '';
    render();
  } else if (action === 'staff-draft-role') {
    const role = target.dataset.role === 'SUPER_OWNER' ? 'SUPER_OWNER' : 'STAFF';
    ui.staffDraftRole = role;
    ui.staffDraftPermissions = buildDefaultPermissionsForRole(role);
    render();
  } else if (action === 'staff-perms-grant-all') {
    ui.staffDraftPermissions = buildDefaultPermissionsForRole('SUPER_OWNER');
    render();
  } else if (action === 'staff-perms-clear-all') {
    ui.staffDraftPermissions = buildDefaultPermissionsForRole('STAFF');
    render();
  } else if (action === 'delete-staff-account') {
    try {
      store.deleteStaffAccount(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'approval-filter-tab') {
    ui.approvalFilterTab = target.dataset.tab || 'ALL';
    render();
  } else if (action === 'review-approval') {
    try {
      store.reviewApproval(Number(target.dataset.id), target.dataset.approve === 'true');
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'inspect-version-json') {
    const id = Number(target.dataset.id);
    ui.inspectedVersionId = Number(ui.inspectedVersionId) === id ? null : id;
    render();
  } else if (action === 'restore-version') {
    try {
      const publishImmediately = target.dataset.publish !== 'false';
      store.restoreVersionSnapshot(Number(target.dataset.id), { publishImmediately });
      ui.modal = {
        type: 'dialog',
        title: 'Version Restored',
        body: publishImmediately
          ? 'Historical snapshot restored and published successfully.'
          : 'Historical snapshot restored as a draft revision.'
      };
      renderModal(store.getState(), s);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Restore Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'set-post-status') {
    try {
      store.setSchoolPostStatus(target.dataset.id, target.dataset.status);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-school-post') {
    try {
      store.deleteSchoolPost(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-school-highlight') {
    try {
      store.deleteSchoolHighlight(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'toggle-edit-media-studio') {
    const id = Number(target.dataset.id);
    ui.editingMediaStudioId = Number(ui.editingMediaStudioId) === id ? null : id;
    render();
  } else if (action === 'assign-media-hero') {
    try {
      store.assignMediaToHero(Number(target.dataset.id));
      ui.modal = { type: 'dialog', title: 'Assigned to Hero Banner', body: 'Media asset added to the public Campus Hero Banner.' };
      renderModal(store.getState(), s);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'assign-media-album') {
    try {
      store.assignMediaToAlbum(Number(target.dataset.id), target.dataset.album || 'Campus');
      ui.modal = { type: 'dialog', title: 'Assigned to Gallery Album', body: 'Media asset published to the school Gallery album.' };
      renderModal(store.getState(), s);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-media-studio') {
    try {
      store.deleteMediaStudioItem(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'toggle-activity-log-sort') {
    ui.activityLogSortNewest = ui.activityLogSortNewest === false;
    render();
  } else if (action === 'activity-log-section') {
    ui.activityLogSection = target.dataset.section || 'ALL';
    render();
  } else if (action === 'reorder-cms-item') {
    try {
      store.reorderCmsItem(target.dataset.section, target.dataset.id, target.dataset.dir);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Reorder Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'toggle-cms-publish') {
    try {
      store.setCmsItemPublished(target.dataset.section, target.dataset.id, target.dataset.published === 'true');
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Publish Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-academic') {
    try {
      store.deleteAcademicProgram(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-hero') {
    try {
      store.deleteHeroPhoto(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-gallery') {
    try {
      store.deleteGalleryMedia(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-facility') {
    try {
      store.deleteFacility(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-transport') {
    try {
      store.deleteTransportRoute(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-announcement') {
    try {
      store.deleteAnnouncement(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'delete-news') {
    try {
      store.deleteNews(Number(target.dataset.id));
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-select-section') {
    ui.v2SelectedSectionKey = target.dataset.section || null;
    ui.v2EditingRecordId = null;
    ui.v2PreviewRecordId = null;
    render();
  } else if (action === 'v2-new-draft') {
    ui.v2SelectedSectionKey = target.dataset.section || ui.v2SelectedSectionKey;
    ui.v2EditingRecordId = null;
    render();
  } else if (action === 'v2-edit-record') {
    ui.v2EditingRecordId = target.dataset.id || null;
    render();
  } else if (action === 'v2-preview-record') {
    ui.v2PreviewRecordId = target.dataset.id || null;
    render();
  } else if (action === 'v2-close-preview') {
    ui.v2PreviewRecordId = null;
    render();
  } else if (action === 'v2-submit-form-approval') {
    try {
      const v2Form = document.getElementById('admin-v2-record-form');
      if (v2Form) {
        const v2Fd = new FormData(v2Form);
        store.saveV2Record({
          id: v2Form.dataset.recordId || undefined,
          categoryKey: v2Form.dataset.category,
          sectionKey: v2Form.dataset.section,
          title: String(v2Fd.get('title') || '').trim(),
          badgeText: String(v2Fd.get('badgeText') || '').trim(),
          summary: String(v2Fd.get('summary') || '').trim(),
          body: String(v2Fd.get('body') || '').trim(),
          imageUri: String(v2Fd.get('imageUri') || '').trim(),
          submitForApproval: true
        });
        ui.v2EditingRecordId = null;
        ui.modal = { type: 'dialog', title: 'Submitted for Approval', body: 'Record saved and submitted to the School Owner approval queue.' };
        renderModal(store.getState(), s);
      }
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Submission Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-submit-record-approval') {
    try {
      store.submitV2RecordForApproval(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-publish-record') {
    try {
      store.publishV2Record(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Publish Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-owner-approve-publish') {
    try {
      store.ownerApproveAndPublishV2Record(target.dataset.id, {
        authorizeExamResult: target.dataset.exam === 'true'
      });
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Owner Authorization Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-set-record-status') {
    try {
      store.updateV2RecordStatus(target.dataset.id, target.dataset.status);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-delete-record') {
    try {
      store.deleteV2Record(target.dataset.id);
      if (ui.v2EditingRecordId === target.dataset.id) ui.v2EditingRecordId = null;
      if (ui.v2PreviewRecordId === target.dataset.id) ui.v2PreviewRecordId = null;
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Delete Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-album-status') {
    try {
      store.updateV2GalleryAlbumStatus(target.dataset.id, target.dataset.status);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-album-delete') {
    try {
      store.deleteV2GalleryAlbum(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Delete Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-photo-status') {
    try {
      store.updateV2GalleryPhotoStatus(target.dataset.id, target.dataset.status);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-photo-delete') {
    try {
      store.deleteV2GalleryPhoto(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Delete Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-faculty-status') {
    try {
      store.updateV2FacultyProfileStatus(target.dataset.id, target.dataset.status);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Consent Gate Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'v2-faculty-delete') {
    try {
      store.deleteV2FacultyProfile(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Delete Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'preview-publish-tab') {
    ui.previewPublishTab = target.dataset.tab || 'review';
    render();
  } else if (action === 'select-owner-section') {
    ui.ownerControlSection = target.dataset.section || 'OWNER_PROFILE_CONTACT';
    render();
  } else if (action === 'owner-review-staff-request') {
    try {
      store.reviewStaffAccessRequest(target.dataset.id, target.dataset.approve === 'true');
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Owner Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'owner-revoke-staff-request') {
    try {
      store.revokeStaffAccessRequest(target.dataset.id);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Owner Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'owner-toggle-account-active') {
    try {
      const accountId = Number(target.dataset.id);
      const targetAcc = store.getManagementAccounts().find((a) => Number(a.id) === accountId);
      if (targetAcc) {
        store.updateStaffAccount({
          accountId,
          displayName: targetAcc.displayName,
          isActive: target.dataset.active === 'true',
          permissions: targetAcc.permissions
        });
      }
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'export-backup') {
    try {
      const blob = new Blob([store.exportBackupJson({ password: ui.backupPassword || '' })], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `chevalier-academy-backup-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Permission Required', body: err.message };
      renderModal(state, s);
    }
  } else if (action === 'confirm-restore-inspected-backup') {
    try {
      if (!ui.pendingBackupRawJson) {
        throw new Error('No inspected backup file loaded.');
      }
      store.importBackupJson(ui.pendingBackupRawJson, { password: ui.backupPassword || '' });
      ui.backupInspectionReport = null;
      ui.pendingBackupRawJson = '';
      ui.modal = { type: 'dialog', title: 'Backup Restored', body: 'Verified school records (including V1 and V2 state) have been restored successfully.' };
      render();
    } catch (err) {
      ui.modal = { type: 'dialog', title: 'Restore Failed', body: err.message || 'Invalid backup file.' };
      render();
    }
  }
});

document.addEventListener('input', (event) => {
  if (store.isAdminAuthenticated) {
    store.onAdminInteraction();
  }
  if (event.target.id === 'parent-search-input') {
    ui.parentSearchQuery = event.target.value;
    render();
  } else if (event.target.id === 'academy-search-input') {
    ui.academySearchQuery = event.target.value;
    render();
  } else if (event.target.id === 'admin-global-search-input') {
    ui.adminSearchQuery = event.target.value;
    render();
  } else if (event.target.id === 'activity-log-search-input') {
    ui.activityLogQuery = event.target.value;
    render();
  } else if (event.target.id === 'backup-password-input') {
    ui.backupPassword = event.target.value;
  }
});

document.addEventListener('change', async (event) => {
  if (event.target.id === 'import-backup-input' && event.target.files?.[0]) {
    try {
      const text = await event.target.files[0].text();
      const report = store.inspectBackupJson(text, ui.backupPassword || '');
      ui.pendingBackupRawJson = text;
      ui.backupInspectionReport = report;
      render();
    } catch (err) {
      ui.backupInspectionReport = null;
      ui.pendingBackupRawJson = '';
      ui.modal = { type: 'dialog', title: 'Backup Inspection Failed', body: err.message || 'Invalid backup file.' };
      render();
    }
  }
});

document.addEventListener('submit', async (event) => {
  const form = event.target;
  event.preventDefault();
  if (store.isAdminAuthenticated) {
    store.onAdminInteraction();
  }
  const fd = new FormData(form);

  try {
    if (form.id === 'master-admin-setup-form') {
      ui.adminAuthError = '';
      store.setupMasterAdmin(
        String(fd.get('displayName') || '').trim(),
        String(fd.get('username') || '').trim(),
        String(fd.get('pin') || ''),
        String(fd.get('confirmPin') || '')
      );
      render();
    } else if (form.id === 'admin-pin-login-form') {
      ui.adminAuthError = '';
      const pin = String(fd.get('pin') || '');
      if (form.dataset.legacy === 'true') {
        const res = await store.verifyAdminPin(pin);
        if (!res.ok) ui.adminAuthError = res.error;
      } else {
        const username = String(fd.get('username') || ui.selectedLoginUsername || '').trim();
        const res = store.loginWithPin(username, pin);
        if (!res.ok) ui.adminAuthError = res.error;
      }
      render();
    } else if (form.id === 'master-pin-recovery-form') {
      ui.adminAuthError = '';
      const res = store.recoverMasterAdminPin(
        String(fd.get('recoveryCode') || '').trim(),
        String(fd.get('newPin') || ''),
        String(fd.get('confirmPin') || '')
      );
      ui.rotatedRecoveryNotice = res.recoveryCode;
      ui.showRecoverPinDialog = false;
      render();
    } else if (form.id === 'admin-hero-banner-form') {
      store.saveAdminHeroBanner({
        titleLine1: String(fd.get('titleLine1') || '').trim(),
        welcomeLine2: String(fd.get('welcomeLine2') || '').trim(),
        subtitleLine3: String(fd.get('subtitleLine3') || '').trim()
      });
      ui.showAdminHeroEditor = false;
      render();
    } else if (form.id === 'staff-account-dialog-form') {
      ui.staffDialogError = '';
      const permissions = {};
      for (const sec of MANAGEMENT_SECTIONS) {
        const actions = sec.supportsPublish
          ? ['VIEW', 'CREATE', 'EDIT', 'DELETE', 'PUBLISH']
          : ['VIEW', 'CREATE', 'EDIT', 'DELETE'];
        permissions[sec.key] = {};
        for (const act of Object.values(PERMISSION_ACTIONS)) {
          permissions[sec.key][act] = actions.includes(act) && fd.get(`perm_${sec.key}_${act}`) === 'on';
        }
        if (!sec.supportsPublish) permissions[sec.key].PUBLISH = false;
      }

      if (ui.staffDialogMode === 'create') {
        store.createStaffAccount({
          displayName: String(fd.get('displayName') || '').trim(),
          username: String(fd.get('username') || '').trim(),
          pin: String(fd.get('pin') || ''),
          confirmPin: String(fd.get('confirmPin') || ''),
          role: String(fd.get('role') || ui.staffDraftRole || 'STAFF'),
          permissions
        });
      } else if (ui.staffDialogMode === 'edit' && ui.editingStaffId) {
        store.updateStaffAccount({
          accountId: ui.editingStaffId,
          displayName: String(fd.get('displayName') || '').trim(),
          newPin: String(fd.get('newPin') || ''),
          isActive: fd.get('isActive') === 'on',
          permissions
        });
      }
      ui.staffDialogMode = null;
      ui.editingStaffId = null;
      render();
    } else if (form.id === 'admin-auth-form') {
      const mode = form.dataset.mode;
      const pin = String(fd.get('pin') || '');
      ui.adminAuthError = '';
      if (mode === 'setup') {
        await store.setupAdminPin(pin, String(fd.get('displayName') || 'School Administrator'));
      } else {
        const res = await store.verifyAdminPin(pin);
        if (!res.ok) ui.adminAuthError = res.error;
      }
      render();
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
      if (!imageUrl && file && file.size > 0) {
        const processed = await compressImageFileForStorage(file);
        imageUrl = processed.dataUrl;
      }
      if (imageUrl) {
        store.addHeroPhoto({ imageUrl, caption: String(fd.get('caption') || '').trim() });
        form.reset();
      }
    } else if (form.id === 'cms-gallery-form') {
      const file = fd.get('imageFile');
      let imageUrl = String(fd.get('imageUrl') || '').trim();
      if (!imageUrl && file && file.size > 0) {
        const processed = await compressImageFileForStorage(file);
        imageUrl = processed.dataUrl;
      }
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
    } else if (form.id === 'cms-school-post-form') {
      const postType = String(fd.get('postType') || 'PHOTO');
      const mediaInput = String(fd.get('mediaInput') || '').trim();
      const photoUrl = String(fd.get('photoUrl') || '').trim();
      const youtubeUrl = String(fd.get('youtubeUrl') || '').trim();
      const isVideo = postType === SCHOOL_POST_TYPES.YOUTUBE_DIRECT || postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO;
      const resolvedPhotos = !isVideo
        ? (mediaInput ? mediaInput.split(',').map((u) => u.trim()).filter(Boolean) : (photoUrl ? [photoUrl] : []))
        : [];
      const resolvedYt = isVideo ? (mediaInput || youtubeUrl) : youtubeUrl;
      const publishNow = fd.get('publishNow') === 'on';
      store.saveSchoolPost({
        postType,
        category: String(fd.get('category') || 'Campus').trim(),
        title: String(fd.get('title') || '').trim(),
        description: String(fd.get('description') || '').trim(),
        photoUrls: resolvedPhotos,
        youtubeUrl: resolvedYt,
        isFeatured: fd.get('isFeatured') === 'on',
        status: publishNow ? SCHOOL_POST_STATUS.PUBLISHED : SCHOOL_POST_STATUS.UNPUBLISHED
      });
      form.reset();
    } else if (form.id === 'cms-highlight-form') {
      store.saveSchoolHighlight({
        title: String(fd.get('title') || '').trim(),
        youtubeShortsUrl: String(fd.get('youtubeShortsUrl') || '').trim(),
        status: 'PUBLISHED'
      });
      form.reset();
    } else if (form.id === 'cms-media-studio-form') {
      store.saveMediaStudioItem({
        title: String(fd.get('title') || '').trim(),
        collectionName: String(fd.get('collectionName') || 'Campus').trim(),
        caption: String(fd.get('caption') || '').trim(),
        mediaType: String(fd.get('mediaType') || 'PHOTO'),
        mediaUri: String(fd.get('mediaUri') || fd.get('mediaUrl') || '').trim()
      });
      form.reset();
    } else if (form.classList.contains('media-metadata-form')) {
      const mediaId = Number(form.dataset.id);
      store.updateMediaMetadata(mediaId, {
        title: String(fd.get('title') || '').trim(),
        caption: String(fd.get('caption') || '').trim(),
        categoryOrAlbum: String(fd.get('categoryOrAlbum') || 'Campus').trim()
      });
      const newMediaUri = String(fd.get('newMediaUri') || '').trim();
      const existing = (store.getState().mediaStudioItems || []).find((m) => Number(m.id) === mediaId);
      if (newMediaUri && existing && existing.mediaUri !== newMediaUri) {
        store.replaceMediaImage(mediaId, newMediaUri);
      }
      ui.editingMediaStudioId = null;
      render();
    } else if (form.id === 'admin-v2-record-form') {
      store.saveV2Record({
        id: form.dataset.recordId || undefined,
        categoryKey: form.dataset.category,
        sectionKey: form.dataset.section,
        title: String(fd.get('title') || '').trim(),
        badgeText: String(fd.get('badgeText') || '').trim(),
        summary: String(fd.get('summary') || '').trim(),
        body: String(fd.get('body') || '').trim(),
        imageUri: String(fd.get('imageUri') || '').trim(),
        submitForApproval: false
      });
      ui.v2EditingRecordId = null;
      form.reset();
    } else if (form.id === 'admin-v2-album-form') {
      store.saveV2GalleryAlbum({
        title: String(fd.get('title') || '').trim(),
        description: String(fd.get('description') || '').trim(),
        coverImageUri: String(fd.get('coverImageUri') || '').trim()
      });
      form.reset();
    } else if (form.id === 'admin-v2-photo-form') {
      store.saveV2GalleryPhoto({
        title: String(fd.get('title') || '').trim(),
        imageUri: String(fd.get('imageUri') || '').trim(),
        caption: String(fd.get('caption') || '').trim(),
        albumIds: fd.getAll('albumIds').map((v) => String(v))
      });
      form.reset();
    } else if (form.id === 'admin-v2-faculty-form') {
      store.saveV2FacultyProfile({
        teacherName: String(fd.get('teacherName') || '').trim(),
        approvedProfilePhotoUri: String(fd.get('approvedProfilePhotoUri') || '').trim(),
        designation: String(fd.get('designation') || '').trim(),
        department: String(fd.get('department') || '').trim(),
        teachingSubjects: String(fd.get('teachingSubjects') || '').trim(),
        educationalQualifications: String(fd.get('educationalQualifications') || '').trim(),
        teachingExperience: String(fd.get('teachingExperience') || '').trim(),
        professionalIntroduction: String(fd.get('professionalIntroduction') || '').trim(),
        isSchoolApproved: fd.get('isSchoolApproved') === 'on',
        hasStaffConsent: fd.get('hasStaffConsent') === 'on'
      });
      form.reset();
    } else if (form.id === 'owner-private-profile-form') {
      store.updateOwnerPrivateProfile({
        ownerName: String(fd.get('ownerName') || '').trim(),
        designation: String(fd.get('designation') || '').trim(),
        privateContactPhone: String(fd.get('privateContactPhone') || '').trim(),
        privateContactEmail: String(fd.get('privateContactEmail') || '').trim(),
        governanceNote: String(fd.get('governanceNote') || '').trim()
      });
      ui.modal = { type: 'dialog', title: 'Owner Profile Saved', body: 'Private School Owner profile updated securely (never exposed on public screens).' };
      render();
    } else if (form.id === 'staff-access-request-form') {
      const requestedSection = String(fd.get('requestedSection') || '').trim();
      const requestReason = String(fd.get('requestReason') || '').trim();
      store.submitStaffAccessRequest({
        requestedCategoryKeys: requestedSection ? [requestedSection] : [],
        reason: requestReason
      });
      ui.modal = { type: 'dialog', title: 'Access Request Submitted', body: 'Your permission request has been queued for School Owner review.' };
      form.reset();
      render();
    }
  } catch (err) {
    if (form.id === 'master-admin-setup-form' || form.id === 'admin-pin-login-form' || form.id === 'master-pin-recovery-form') {
      ui.adminAuthError = err.message;
      render();
    } else if (form.id === 'staff-account-dialog-form') {
      ui.staffDialogError = err.message;
      render();
    } else {
      ui.modal = { type: 'dialog', title: 'Action Blocked', body: err.message };
      renderModal(store.getState(), getStrings(store.getState().preferences.language));
    }
  }
});

document.addEventListener('keydown', (event) => {
  if (store.isAdminAuthenticated) {
    store.onAdminInteraction();
  }
  if (event.key === 'Escape') {
    if (ui.modal) {
      ui.modal = null;
      renderModal(store.getState(), getStrings(store.getState().preferences.language));
    } else if (ui.drawerOpen || ui.overflowOpen || ui.adminMenuOpen) {
      ui.drawerOpen = false;
      ui.overflowOpen = false;
      ui.adminMenuOpen = false;
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
  ui.adminMenuOpen = false;
  render();
});

// 5-Minute Admin Inactivity Auto-Lock Monitor
setInterval(() => {
  if (store.isAdminAuthenticated) {
    store.checkInactivityLock();
  }
}, 15_000);

store.subscribe(() => render());

// Boot
ui.route = parseHashRoute();
render();
store.enableCloudSync().catch(() => {});
