/**
 * Base 3 — Separated School Management & Admin Control Views for Web.
 * Complete parity with Android App V1:
 * - AdminDashboardHomeScreen.kt (Navy Header, Admin Hero Banner, 8 Management Hubs in 2x4 Grid)
 * - AdminDashboardHubs.kt (6 Section Hubs + AdminAccessDeniedScreen)
 * - ManagementPortalScreen.kt (First-time Master Admin Setup, Recovery Code, Multi-Account PIN Login, Lockout)
 * - ManagementGovernanceScreens.kt (StaffManagementScreen with 20x5 RBAC Matrix, ApprovalCenterScreen, MediaStudioScreen)
 * - ManagementAuditAndBackupScreens.kt (VersionHistoryScreen, ActivityLogScreen, NotificationsScreen, GlobalSearchScreen, BackupRestoreScreen)
 * - ParentPortalManagementScreens.kt (All 11 Parent Portal Management submenus)
 */

import { icon } from './icons.js';
import { ADMIN_PPM_MODULES, ACADEMICS_V2_CATEGORIES, getAdminPpmStrings } from './i18n.js';
import {
  MANAGEMENT_ROLES,
  PERMISSION_ACTIONS,
  MANAGEMENT_SECTIONS,
  MANAGEMENT_SECTION_MAP,
  ADMIN_DASHBOARD_CARDS,
  SCHOOL_POST_TYPES,
  SCHOOL_POST_STATUS,
  SCHOOL_POST_CATEGORIES,
  ACADEMICS_V2_RECORD_STATUS,
  SCHOOL_OWNER_SECTIONS,
  V2_CATEGORY_PRIMARY_SECTION_MAP,
  buildDefaultPermissionsForRole,
  normalizePermissionsMatrix
} from './store.js';
import { esc, renderFooter, renderHomeView, renderAcademicsHubView } from './publicViews.js';

function roleBadgeLabel(role) {
  if (role === MANAGEMENT_ROLES.MASTER_ADMIN) return 'Master Admin';
  if (role === MANAGEMENT_ROLES.SUPER_OWNER) return 'Super Owner';
  return 'Staff';
}

function canSessionAccessCard(store, state, cardId) {
  if (store && typeof store.canAccessAdminCard === 'function') {
    return store.canAccessAdminCard(cardId);
  }
  return true;
}

function canSessionPerm(store, sectionKey, action = PERMISSION_ACTIONS.VIEW) {
  if (store && typeof store.hasPermission === 'function') {
    return store.hasPermission(sectionKey, action);
  }
  return true;
}

/* ==========================================================================
   1. ADMIN AUTHENTICATION GATE (MASTER ADMIN SETUP, RECOVERY KEY, MULTI-ACCOUNT PIN LOGIN)
   ========================================================================== */
export function renderAdminGateView(store, s, ui = {}) {
  const hasMaster = store.hasMasterAdmin();
  const hasLegacyPinOnly = !hasMaster && Boolean(store.getState().admin?.pinHash);

  // Step 1B: Recovery Key Confirmation Card right after Master Admin Setup
  if (store.pendingRecoveryCode) {
    return `
      <div class="card gold-border" style="max-width:540px;margin:0 auto;width:100%;" data-testid="master_recovery_key_card">
        <div class="row-wrap" style="margin-bottom:12px;">
          <span class="icon-tile tone-forest" style="width:48px;height:48px;">${icon('verified', 24)}</span>
          <div>
            <span class="badge badge-gold">Save Master Recovery Code</span>
            <h2 class="section-title" style="margin:4px 0 0;">Master Admin Recovery Key</h2>
          </div>
        </div>
        <p class="muted text-sm" style="margin:0 0 14px;">
          Write down this one-time 12-character Master Recovery Key in a safe place. It is required if you ever need to reset the Master Admin PIN.
        </p>
        <div style="background:var(--bg-surface-variant);border:1.5px dashed var(--brand-gold);border-radius:14px;padding:16px;text-align:center;margin-bottom:16px;">
          <strong style="font-size:1.35rem;letter-spacing:0.12em;color:var(--brand-burgundy);" data-testid="generated_recovery_code_text">${esc(store.pendingRecoveryCode)}</strong>
        </div>
        <button type="button" class="btn btn-primary btn-full" data-action="confirm-recovery-saved" data-testid="confirm_recovery_saved_btn">
          ${icon('factCheck', 18)} I Have Saved This Recovery Key — Enter Admin Dashboard
        </button>
      </div>
      ${renderFooter(s)}
    `;
  }

  // Step 1A: First-Time Master Admin Setup Screen
  if (!hasMaster && !hasLegacyPinOnly) {
    return `
      <div class="card gold-border" style="max-width:540px;margin:0 auto;width:100%;" data-testid="master_admin_setup_screen">
        <div class="row-wrap" style="margin-bottom:14px;">
          <span class="icon-tile tone-burgundy" style="width:48px;height:48px;">${icon('security', 24)}</span>
          <div>
            <span class="badge badge-gold">Separated Management Area</span>
            <h2 class="section-title" style="margin:4px 0 0;">First-Time Master Admin Setup</h2>
          </div>
        </div>

        <p class="muted text-sm" style="margin:0 0 16px;">
          Configure the primary Master Admin account with a 6+ digit numeric PIN. A 12-character Master Recovery Key will be generated upon creation.
        </p>

        ${ui.adminAuthError ? `<div class="badge" style="background:var(--danger-soft);color:var(--danger);margin-bottom:12px;width:100%;padding:10px 12px;border-radius:10px;" data-testid="admin_auth_error_banner">${esc(ui.adminAuthError)}</div>` : ''}

        <form id="master-admin-setup-form">
          <div class="form-group">
            <label class="form-label" for="setup-display-name">Master Admin Display Name</label>
            <input id="setup-display-name" name="displayName" class="form-input" value="Principal Master Admin" required data-testid="setup_display_name_input" />
          </div>
          <div class="form-group">
            <label class="form-label" for="setup-username">Username</label>
            <input id="setup-username" name="username" class="form-input" value="masteradmin" required data-testid="setup_username_input" />
          </div>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label" for="setup-pin">Create PIN (6+ digits)</label>
              <input id="setup-pin" name="pin" type="password" inputmode="numeric" pattern="[0-9]{6,12}" minlength="6" class="form-input" placeholder="6+ digits" required data-testid="setup_pin_input" />
            </div>
            <div class="form-group">
              <label class="form-label" for="setup-confirm-pin">Confirm PIN</label>
              <input id="setup-confirm-pin" name="confirmPin" type="password" inputmode="numeric" pattern="[0-9]{6,12}" minlength="6" class="form-input" placeholder="Re-enter PIN" required data-testid="setup_confirm_pin_input" />
            </div>
          </div>
          <div class="stack-sm">
            <button type="submit" class="btn btn-primary btn-full" data-testid="create_master_admin_btn">
              ${icon('security', 18)} Create Master Admin &amp; Generate Recovery Key
            </button>
            <button type="button" class="btn btn-outline btn-full" data-route="home" data-testid="management_return_public_button">
              ${icon('back', 18)} ${esc(s.returnToPublicAppBtn)}
            </button>
          </div>
        </form>
      </div>
      ${renderFooter(s)}
    `;
  }

  // Step 2: Multi-User PIN Login Screen (+ Master PIN Recovery Modal)
  const activeAccounts = store.getActiveManagementAccounts();
  const defaultUsername = ui.selectedLoginUsername || activeAccounts[0]?.username || 'masteradmin';

  return `
    <div class="card gold-border" style="max-width:540px;margin:0 auto;width:100%;" data-testid="management_login_screen">
      <div class="row-wrap" style="margin-bottom:14px;">
        <span class="icon-tile tone-burgundy" style="width:48px;height:48px;">${icon('lock', 24)}</span>
        <div>
          <span class="badge badge-gold">Separated Management Area</span>
          <h2 class="section-title" style="margin:4px 0 0;">${esc(s.drawerManagementLogin)}</h2>
        </div>
      </div>

      <p class="muted text-sm" style="margin:0 0 14px;">
        Select your account or enter your username and 6+ digit PIN to unlock the Chevalier Academy Administration Portal.
      </p>

      ${activeAccounts.length > 0 ? `
        <div class="chip-row" style="margin-bottom:14px;" data-testid="login_active_accounts_chips">
          ${activeAccounts.map((acc) => `
            <button
              type="button"
              class="chip ${defaultUsername === acc.username ? 'selected' : ''}"
              data-action="select-login-user"
              data-username="${esc(acc.username)}"
              data-testid="login_account_chip_${esc(acc.username)}"
            >
              ${icon('person', 15)} ${esc(acc.displayName)} (@${esc(acc.username)})
            </button>
          `).join('')}
        </div>
      ` : ''}

      ${ui.adminAuthError ? `<div class="badge" style="background:var(--danger-soft);color:var(--danger);margin-bottom:12px;width:100%;padding:10px 12px;border-radius:10px;" data-testid="admin_auth_error_banner">${esc(ui.adminAuthError)}</div>` : ''}
      ${ui.rotatedRecoveryNotice ? `<div class="badge" style="background:var(--brand-forest-soft);color:var(--brand-forest-on-container);margin-bottom:12px;width:100%;padding:10px 12px;border-radius:10px;" data-testid="rotated_recovery_code_banner">New Master Recovery Key: <strong>${esc(ui.rotatedRecoveryNotice)}</strong></div>` : ''}

      <form id="admin-pin-login-form" data-legacy="${hasLegacyPinOnly ? 'true' : 'false'}">
        ${!hasLegacyPinOnly ? `
          <div class="form-group">
            <label class="form-label" for="login-username-input">Username</label>
            <input id="login-username-input" name="username" class="form-input" value="${esc(defaultUsername)}" required data-testid="login_username_input" />
          </div>
        ` : ''}
        <div class="form-group">
          <label class="form-label" for="login-pin-input">PIN</label>
          <input id="login-pin-input" name="pin" type="password" inputmode="numeric" class="form-input" placeholder="Enter PIN" required data-testid="login_pin_input" />
        </div>
        <div class="stack-sm">
          <button type="submit" class="btn btn-primary btn-full" data-testid="login_unlock_btn">
            ${icon('security', 18)} Unlock Management Portal
          </button>
          ${hasMaster ? `
            <button type="button" class="btn btn-outline-gold btn-full" data-action="toggle-recover-pin" data-testid="forgot_pin_btn">
              ${icon('verified', 18)} Forgot Master Admin PIN? Use Recovery Code
            </button>
          ` : ''}
          <button type="button" class="btn btn-outline btn-full" data-route="home" data-testid="management_return_public_button">
            ${icon('back', 18)} ${esc(s.returnToPublicAppBtn)}
          </button>
        </div>
      </form>

      ${ui.showRecoverPinDialog ? `
        <form id="master-pin-recovery-form" style="margin-top:18px;padding-top:16px;border-top:1px solid var(--border-subtle);" data-testid="master_pin_recovery_dialog">
          <h3 class="section-title" style="margin:0 0 10px;font-size:1rem;">Reset Master Admin PIN</h3>
          <div class="form-group">
            <label class="form-label">12-Character Recovery Code</label>
            <input name="recoveryCode" class="form-input" placeholder="XXXX-XXXX-XXXX" required data-testid="recover_code_input" />
          </div>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">New PIN (6+ digits)</label>
              <input name="newPin" type="password" inputmode="numeric" minlength="6" class="form-input" required data-testid="recover_new_pin_input" />
            </div>
            <div class="form-group">
              <label class="form-label">Confirm New PIN</label>
              <input name="confirmPin" type="password" inputmode="numeric" minlength="6" class="form-input" required data-testid="recover_confirm_pin_input" />
            </div>
          </div>
          <button type="submit" class="btn btn-forest btn-full" data-testid="submit_recover_pin_btn">
            ${icon('verified', 18)} Verify Recovery Key &amp; Reset PIN
          </button>
        </form>
      ` : ''}
    </div>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   2. ADMIN TOP BAR & ADMIN HERO BANNER CARD (AdminDashboardHomeScreen.kt)
   ========================================================================== */
export function renderAdminTopHeader(store, state, s, ui = {}, options = {}) {
  const activeAccount = store?.getActiveAccount?.() || {
    displayName: state.admin?.displayName || 'Administrator',
    username: 'masteradmin',
    role: MANAGEMENT_ROLES.MASTER_ADMIN
  };
  const unreadCount = store?.getUnreadNotificationsCount?.() || 0;
  const isSubScreen = Boolean(options.isSubScreen);
  const title = options.title || 'Admin Dashboard';
  const backRoute = options.backRoute || (isSubScreen ? 'admin' : 'home');
  const canEditHero = store ? store.canEditAdminHeroBanner() : true;
  const canViewSettings = canSessionPerm(store, 'APP_SETTINGS', PERMISSION_ACTIONS.VIEW);
  const canViewNotifications = canSessionPerm(store, 'NOTIFICATIONS', PERMISSION_ACTIONS.VIEW);

  return `
    <div class="admin-navy-header" data-testid="admin_dashboard_header">
      <button
        type="button"
        class="icon-btn admin-header-icon-btn"
        data-route="${esc(backRoute)}"
        aria-label="Back"
        data-testid="${isSubScreen ? 'mgmt_subscreen_back_btn' : 'mgmt_header_back_btn'}"
      >
        ${icon('back', 20)}
      </button>

      <div style="flex:1;min-width:0;">
        <span class="admin-header-kicker">CHEVALIER ACADEMY · ${esc(roleBadgeLabel(activeAccount.role).toUpperCase())}</span>
        <h2 class="admin-header-title" data-testid="mgmt_header_title">${esc(title)}</h2>
      </div>

      <div class="row-wrap" style="gap:4px;position:relative;">
        <button
          type="button"
          class="icon-btn admin-header-icon-btn"
          data-route="admin/search"
          aria-label="Search Admin Portal"
          data-testid="mgmt_top_search_btn"
        >
          ${icon('search', 20)}
        </button>

        ${canViewNotifications ? `
          <button
            type="button"
            class="icon-btn admin-header-icon-btn"
            data-route="admin/notifications"
            aria-label="Admin Notifications"
            data-testid="mgmt_top_notifications_btn"
          >
            ${icon('notifications', 20)}
            ${unreadCount > 0 ? `<span class="admin-notif-badge" data-testid="mgmt_top_notifications_badge">${unreadCount > 99 ? '99+' : unreadCount}</span>` : ''}
          </button>
        ` : ''}

        <button
          type="button"
          class="icon-btn admin-header-icon-btn"
          data-action="toggle-admin-menu"
          aria-label="Admin Menu"
          data-testid="mgmt_top_menu_btn"
        >
          ${icon('moreVert', 20)}
        </button>

        <div class="overflow-menu ${ui.adminMenuOpen ? 'open' : ''}" role="menu" data-testid="mgmt_header_menu_dropdown">
          <div style="padding:10px 12px;border-bottom:1px solid var(--border-subtle);">
            <strong style="display:block;font-size:0.9rem;color:var(--text-primary);">${esc(activeAccount.displayName)}</strong>
            <span class="badge badge-gold" style="margin-top:4px;">${esc(roleBadgeLabel(activeAccount.role))}</span>
          </div>
          ${canEditHero ? `
            <button type="button" class="overflow-item" data-action="open-admin-hero-editor" data-testid="mgmt_menu_edit_hero">
              ${icon('edit', 18)} <span>Customize Hero Banner</span>
            </button>
          ` : ''}
          ${canViewSettings ? `
            <button type="button" class="overflow-item" data-route="admin/section/APP_SETTINGS" data-testid="mgmt_menu_settings">
              ${icon('settings', 18)} <span>App Settings</span>
            </button>
          ` : ''}
          <button type="button" class="overflow-item" data-route="home" data-testid="mgmt_menu_public_app">
            ${icon('visibility', 18)} <span>Switch to Public App</span>
          </button>
          <button type="button" class="overflow-item" data-action="admin-logout" data-testid="mgmt_menu_logout">
            ${icon('lock', 18)} <span>Lock &amp; Sign Out</span>
          </button>
        </div>
      </div>
    </div>
  `;
}

export function renderAdminHeroBannerCard(store, state, ui = {}) {
  const activeAccount = store?.getActiveAccount?.() || {
    displayName: state.admin?.displayName || 'Administrator',
    role: MANAGEMENT_ROLES.MASTER_ADMIN
  };
  const banner = state.adminHeroBanner || {
    titleLine1: 'CHEVALIER ACADEMY',
    welcomeLine2: 'Welcome, {Admin Name}',
    subtitleLine3: 'School Management & Administration Portal',
    backgroundImageUri: null
  };
  const resolvedLine2 = String(banner.welcomeLine2 || 'Welcome, {Admin Name}')
    .replace(/\{Admin Name\}/gi, activeAccount.displayName)
    .replace(/\{name\}/gi, activeAccount.displayName);
  const canEdit = store ? store.canEditAdminHeroBanner() : true;

  return `
    <div class="admin-hero-banner" data-testid="admin_hero_banner_card" ${banner.backgroundImageUri ? `style="background-image:linear-gradient(135deg, rgba(16,37,66,0.88), rgba(27,59,111,0.84)), url('${esc(banner.backgroundImageUri)}');background-size:cover;background-position:center;"` : ''}>
      <div class="row-between" style="align-items:flex-start;gap:12px;">
        <div style="min-width:0;flex:1;">
          <div class="admin-hero-line-1" data-testid="admin_hero_line_1">${esc(banner.titleLine1 || 'CHEVALIER ACADEMY')}</div>
          <h3 class="admin-hero-line-2" data-testid="admin_hero_line_2">${esc(resolvedLine2)}</h3>
          <p class="admin-hero-line-3" data-testid="admin_hero_line_3">${esc(banner.subtitleLine3 || 'School Management & Administration Portal')}</p>
        </div>
        <div class="row-wrap" style="gap:8px;">
          <span class="badge badge-gold">${esc(roleBadgeLabel(activeAccount.role))}</span>
          ${canEdit ? `
            <button type="button" class="icon-btn admin-header-icon-btn" data-action="open-admin-hero-editor" aria-label="Edit Admin Hero Banner" data-testid="admin_hero_edit_btn">
              ${icon('edit', 18)}
            </button>
          ` : ''}
        </div>
      </div>

      ${ui.showAdminHeroEditor ? `
        <form id="admin-hero-banner-form" style="margin-top:14px;padding-top:14px;border-top:1px solid rgba(255,255,255,0.2);" data-testid="admin_hero_editor_dialog">
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label" style="color:#fff;">Line 1 (School Heading)</label>
              <input name="titleLine1" class="form-input" value="${esc(banner.titleLine1)}" required data-testid="admin_hero_input_line1" />
            </div>
            <div class="form-group">
              <label class="form-label" style="color:#fff;">Line 2 (Use {Admin Name})</label>
              <input name="welcomeLine2" class="form-input" value="${esc(banner.welcomeLine2)}" required data-testid="admin_hero_input_line2" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" style="color:#fff;">Line 3 (Subtitle)</label>
            <input name="subtitleLine3" class="form-input" value="${esc(banner.subtitleLine3)}" required data-testid="admin_hero_input_line3" />
          </div>
          <div class="row-wrap">
            <button type="submit" class="btn btn-primary" data-testid="admin_hero_save_btn">${icon('publish', 16)} Save Banner</button>
            <button type="button" class="btn btn-outline" style="color:#fff;border-color:rgba(255,255,255,0.5);" data-action="close-admin-hero-editor">Cancel</button>
          </div>
        </form>
      ` : ''}
    </div>
  `;
}

/* ==========================================================================
   3. ADMIN DASHBOARD HOME (8 MANAGEMENT HUBS IN 2x4 GRID + EXPANDABLE PPM)
   ========================================================================== */
export function renderAdminDashboardView(state, s, ui = {}, store = null) {
  const ppmStrings = getAdminPpmStrings(state.preferences.language);
  const expanded = Boolean(ui.ppmExpanded);

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: false, title: 'Admin Dashboard' })}
    ${renderAdminHeroBannerCard(store, state, ui)}

    <!-- 8 MANAGEMENT HUB CARDS (2x4 GRID MATCHING AdminDashboardHomeScreen.kt) -->
    <section>
      <div class="section-header">
        <h3 class="section-title">Management Hubs (8)</h3>
      </div>
      <div class="admin-dashboard-grid-8" data-testid="management_dashboard_grid">
        ${ADMIN_DASHBOARD_CARDS.map((card) => {
          const hasAccess = canSessionAccessCard(store, state, card.id);
          return `
            <button
              type="button"
              class="admin-hub-card ${hasAccess ? '' : 'locked'}"
              data-route="${esc(card.route)}"
              data-testid="admin_dash_card_${card.id}"
            >
              <div class="row-between" style="width:100%;">
                <span class="icon-tile ${hasAccess ? 'tone-navy' : 'tone-muted'}">${icon(card.icon, 22)}</span>
                ${!hasAccess ? `<span class="badge" style="background:var(--bg-surface-variant);color:var(--text-secondary);" data-testid="admin_dash_card_lock_${card.id}">${icon('lock', 14)} Restricted</span>` : `<span style="color:var(--text-secondary);">${icon('chevronRight', 20)}</span>`}
              </div>
              <div style="text-align:left;width:100%;">
                <strong class="admin-hub-card-title">${esc(card.title)}</strong>
                <p class="admin-hub-card-sub">${esc(card.subtitle)}</p>
              </div>
            </button>
          `;
        }).join('')}
      </div>
    </section>

    <!-- BASE 4: ACADEMICS V2 (13 CATEGORIES), PREVIEW & PUBLISH HUB & SCHOOL OWNER CONTROL CENTER -->
    <section data-testid="admin_v2_quick_actions_row">
      <div class="section-header">
        <h3 class="section-title">Academics V2 &amp; Owner Governance</h3>
      </div>
      <div class="two-col-responsive">
        <button type="button" class="card interactive gold-border" style="text-align:left;" data-route="admin/v2-academics" data-testid="admin_open_v2_academics_btn">
          <div class="row-between">
            <span class="icon-tile tone-burgundy">${icon('school', 22)}</span>
            <span class="badge badge-gold">13 Categories</span>
          </div>
          <strong style="display:block;margin-top:8px;">Academics V2 Management Hub</strong>
          <span class="muted text-xs">Draft, preview, approve &amp; publish across all 13 Academy categories, Photo-Only Gallery &amp; 8-Field Faculty Manager</span>
        </button>

        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/preview-publish" data-testid="admin_open_preview_publish_btn">
          <div class="row-between">
            <span class="icon-tile tone-forest">${icon('publish', 22)}</span>
            <span class="badge badge-forest">3-Tab Preview</span>
          </div>
          <strong style="display:block;margin-top:8px;">Preview &amp; Publish Hub</strong>
          <span class="muted text-xs">Inspect unpublished changes, preview Public Home &amp; Academics screens, and publish verified records</span>
        </button>

        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/owner-control-center" data-testid="admin_open_owner_control_center_btn">
          <div class="row-between">
            <span class="icon-tile tone-navy">${icon('security', 22)}</span>
            <span class="badge badge-burgundy">Owner Only · 6 Sections</span>
          </div>
          <strong style="display:block;margin-top:8px;">School Owner – Control Center</strong>
          <span class="muted text-xs">Private Owner Profile, Staff Access Approvals, Role Control, Approval Queue, Exam Results Gate &amp; Audit Security</span>
        </button>
      </div>
    </section>

    <!-- EXPANDABLE PARENT PORTAL MANAGEMENT (ALL 11 SUBMENUS) -->
    <section data-testid="admin_parent_portal_mgmt_section">
      <button
        type="button"
        class="ppm-expand-header ${expanded ? 'expanded' : ''}"
        data-action="toggle-ppm"
        aria-expanded="${expanded}"
        data-testid="admin_parent_portal_mgmt_header"
      >
        <span class="icon-tile tone-burgundy">${icon('parent', 22)}</span>
        <div style="flex:1;min-width:0;">
          <strong style="display:block;font-size:1rem;">${esc(ppmStrings.sectionTitle)}</strong>
          <span class="muted text-xs">${esc(ppmStrings.sectionSubtitle)}</span>
        </div>
        <span class="badge badge-gold">${ADMIN_PPM_MODULES.length}</span>
        <span style="color:var(--brand-burgundy);display:inline-flex;">${icon(expanded ? 'expandLess' : 'expandMore', 24)}</span>
      </button>

      ${expanded ? `
        <div class="ppm-submenus-grid" data-testid="admin_ppm_submenus_grid">
          ${ADMIN_PPM_MODULES.map((mod, idx) => {
            const copy = ppmStrings.modules[mod.key];
            const num = String(idx + 1).padStart(2, '0');
            return `
              <button
                type="button"
                class="parent-module-card"
                style="border-color:${mod.useForest ? 'rgba(8,122,69,0.35)' : 'rgba(141,32,41,0.35)'};"
                data-route="admin/ppm/${mod.key}"
                data-testid="ppm_submenu_card_${mod.key}"
              >
                <div class="row-between" style="width:100%;">
                  <span class="icon-tile ${mod.useForest ? 'tone-forest' : 'tone-burgundy'}">${icon(mod.icon, 20)}</span>
                  <span class="badge badge-gold">${num}</span>
                </div>
                <p class="parent-module-title">${esc(copy.title)}</p>
              </button>
            `;
          }).join('')}
        </div>
      ` : ''}
    </section>

    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   4. ADMIN ACCESS DENIED SCREEN & 6 HUB SUB-SCREENS (AdminDashboardHubs.kt)
   ========================================================================== */
export function renderAdminAccessDeniedView(store, state, s, ui = {}, sectionTitle = 'Restricted Section') {
  const role = store?.getCurrentRole?.() || MANAGEMENT_ROLES.STAFF;
  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: sectionTitle, backRoute: 'admin' })}
    <div class="card gold-border" style="text-align:center;padding:32px 20px;" data-testid="admin_access_denied_screen">
      <span class="icon-tile tone-burgundy" style="width:56px;height:56px;margin:0 auto 12px;">${icon('lock', 28)}</span>
      <h2 class="section-title" style="margin:0 0 8px;">Access Restricted</h2>
      <p class="muted text-sm" style="margin:0 0 8px;">${esc(sectionTitle)}</p>
      <p class="muted text-sm" style="max-width:440px;margin:0 auto 18px;">
        Your account role (${esc(roleBadgeLabel(role))}) does not currently have View permission for this section. Contact the Master Admin to request access.
      </p>
      <button type="button" class="btn btn-primary" data-route="admin" data-testid="admin_access_denied_back_btn">
        ${icon('back', 18)} Back to Admin Dashboard
      </button>
    </div>
    ${renderFooter(s)}
  `;
}

export function renderAdminHubView(store, state, s, ui = {}, hubId) {
  if (!canSessionAccessCard(store, state, hubId)) {
    const cardMeta = ADMIN_DASHBOARD_CARDS.find((c) => c.id === hubId);
    return renderAdminAccessDeniedView(store, state, s, ui, cardMeta?.title || hubId);
  }

  const ppmStrings = getAdminPpmStrings(state.preferences.language);

  const hubConfigs = {
    home_page: {
      testId: 'admin_hub_home_page',
      title: 'Home Page Management',
      subtitle: 'Manage the public Home Page hero banner, social feed posts, 24-hour highlights, and media assets.',
      items: [
        { id: 'hero_banner', title: 'Home Page Hero Banner & Logo', subtitle: `${state.heroPhotos.length} hero banner slides`, icon: 'camera', route: 'admin/section/LOGO_BANNER', secKey: 'LOGO_BANNER' },
        { id: 'school_posts', title: 'School Social Feed Posts', subtitle: `${(state.schoolPosts || []).length} posts (Photos & YouTube Videos)`, icon: 'forum', route: 'admin/section/SCHOOL_POSTS', secKey: 'SCHOOL_POSTS' },
        { id: 'school_highlights', title: '24-Hour YouTube Shorts Highlights', subtitle: `${(state.schoolHighlights || []).length} highlights`, icon: 'video', route: 'admin/section/SCHOOL_HIGHLIGHTS', secKey: 'SCHOOL_HIGHLIGHTS' },
        { id: 'media_studio', title: 'Media Studio', subtitle: `${(state.mediaStudioItems || []).length} managed media items`, icon: 'gallery', route: 'admin/section/MEDIA_STUDIO', secKey: 'MEDIA_STUDIO' }
      ]
    },
    academy: {
      testId: 'admin_hub_academy',
      title: 'Academy Management',
      subtitle: 'Manage all 13 Academics V2 categories & sub-sections, higher secondary streams, campus facilities, bus routes, gallery, and school history.',
      items: [
        { id: 'v2_academics_hub', title: 'Academics V2 — All 13 Categories & Sub-Sections', subtitle: `${((state.academicsV2 && state.academicsV2.records) || []).length} V2 records · Photo-Only Gallery · 8-Field Faculty Manager`, icon: 'school', route: 'admin/v2-academics', secKey: 'ACADEMICS' },
        { id: 'academy_hero', title: 'Academy Hero Banner', subtitle: 'Customize campus banner images', icon: 'camera', route: 'admin/section/LOGO_BANNER', secKey: 'LOGO_BANNER' },
        { id: 'academics', title: 'Academics & Curriculum', subtitle: `${state.academics.length} academic entries`, icon: 'school', route: 'admin/section/ACADEMICS', secKey: 'ACADEMICS' },
        { id: 'courses', title: 'Higher Secondary Courses', subtitle: `${state.courses.length} course streams (Groups I–IV)`, icon: 'book', route: 'admin/section/HIGHER_SECONDARY_COURSES', secKey: 'HIGHER_SECONDARY_COURSES' },
        { id: 'facilities', title: 'Campus Facilities', subtitle: `${state.facilities.length} facilities`, icon: 'apartment', route: 'admin/section/FACILITIES', secKey: 'FACILITIES' },
        { id: 'transport', title: 'Transport & Bus Routes', subtitle: `${state.transportRoutes.length} transport routes`, icon: 'bus', route: 'admin/section/TRANSPORT', secKey: 'TRANSPORT' },
        { id: 'photo_gallery', title: 'Photo & Video Gallery', subtitle: `${state.galleryMedia.length} media items`, icon: 'gallery', route: 'admin/section/PHOTO_GALLERY', secKey: 'PHOTO_GALLERY' },
        { id: 'about_school', title: 'About School, Vision & Mission', subtitle: 'History, motto, principal & affiliation', icon: 'info', route: 'admin/section/ABOUT_SCHOOL', secKey: 'ABOUT_SCHOOL' }
      ]
    },
    parent: {
      testId: 'admin_hub_parent',
      title: 'Parent Portal Management',
      subtitle: 'Manage parent notices, circulars, and all 11 Parent Portal V1 service modules.',
      items: [
        { id: 'parent_announcements', title: 'Parent Circulars & Announcements', subtitle: `${state.announcements.length} official circulars`, icon: 'campaign', route: 'admin/section/ANNOUNCEMENTS', secKey: 'ANNOUNCEMENTS' },
        ...ADMIN_PPM_MODULES.map((mod) => ({
          id: `ppm_${mod.key}`,
          title: ppmStrings.modules[mod.key]?.title || mod.key,
          subtitle: ppmStrings.modules[mod.key]?.intro || 'Parent Portal V1 Module',
          icon: mod.icon,
          route: `admin/ppm/${mod.key}`,
          secKey: 'SCHOOL_PROFILE',
          customTestId: `ppm_submenu_card_${mod.key}`
        }))
      ]
    },
    common_app: {
      testId: 'admin_hub_common_app',
      title: 'Common App Settings & Branding',
      subtitle: 'Configure app language, theme, school identity, official social media links, and branding.',
      items: [
        { id: 'app_settings', title: 'App Settings (Language & Theme)', subtitle: 'English, Tamil, Hindi & Light/Dark appearance', icon: 'settings', route: 'admin/section/APP_SETTINGS', secKey: 'APP_SETTINGS' },
        { id: 'school_profile', title: 'School Profile & Core Identity', subtitle: state.profile.schoolName, icon: 'school', route: 'admin/section/SCHOOL_PROFILE', secKey: 'SCHOOL_PROFILE' },
        { id: 'social_media', title: 'Official Social Media Links', subtitle: `${state.socialLinks.filter((l) => l.active).length} active channels`, icon: 'share', route: 'admin/section/SOCIAL_MEDIA', secKey: 'SOCIAL_MEDIA' },
        { id: 'logo_banner', title: 'Logo & Campus Hero Banner', subtitle: `${state.heroPhotos.length} hero slides`, icon: 'camera', route: 'admin/section/LOGO_BANNER', secKey: 'LOGO_BANNER' }
      ]
    },
    management_tools: {
      testId: 'admin_hub_management_tools',
      title: 'Management Tools & Governance',
      subtitle: 'Content approval workflow, version history snapshots, audit activity log, JSON backup/restore, media studio, and School Owner Control Center.',
      items: [
        { id: 'preview_publish_hub', title: 'Preview & Publish Hub', subtitle: 'Review changed drafts, preview Public Home & Academics, and publish', icon: 'publish', route: 'admin/preview-publish', customCanView: true },
        { id: 'owner_control_center', title: 'School Owner – Control Center (6 Sections)', subtitle: 'Owner-only private profile, staff access requests, role control & exam results gate', icon: 'security', route: 'admin/owner-control-center', customCanView: store ? store.isSchoolOwner() : true },
        { id: 'approval_center', title: 'Approval Center', subtitle: `${(state.contentApprovals || []).filter((a) => a.status === 'PENDING').length} pending approvals`, icon: 'factCheck', route: 'admin/section/APPROVAL_CENTER', customCanView: store ? store.canAccessApprovalCenter() : true },
        { id: 'version_history', title: 'Version History', subtitle: `${(store?.getVisibleVersionHistory?.() || state.versionHistory || []).length} version snapshots`, icon: 'schedule', route: 'admin/section/VERSION_HISTORY', customCanView: store ? store.canAccessVersionHistory() : true },
        { id: 'activity_log', title: 'Activity Log', subtitle: `${(store?.getVisibleActivityLogs?.() || state.admin?.activityLog || []).length} audit entries`, icon: 'assignment', route: 'admin/section/ACTIVITY_LOG', secKey: 'ACTIVITY_LOG' },
        { id: 'backup_restore', title: 'Backup & Restore', subtitle: 'Export or import verified school JSON backup', icon: 'download', route: 'admin/section/BACKUP_RESTORE', secKey: 'BACKUP_RESTORE' },
        { id: 'media_studio', title: 'Media Studio', subtitle: `${(state.mediaStudioItems || []).length} studio assets`, icon: 'gallery', route: 'admin/section/MEDIA_STUDIO', secKey: 'MEDIA_STUDIO' }
      ]
    },
    updates: {
      testId: 'admin_hub_updates',
      title: 'Updates & Notifications Hub',
      subtitle: 'Publish school news, official announcements, and manage admin portal notifications.',
      items: [
        { id: 'news', title: 'School News & Events', subtitle: `${state.news.length} published news articles`, icon: 'news', route: 'admin/section/NEWS', secKey: 'NEWS' },
        { id: 'announcements', title: 'Official Announcements & Circulars', subtitle: `${state.announcements.length} official announcements`, icon: 'campaign', route: 'admin/section/ANNOUNCEMENTS', secKey: 'ANNOUNCEMENTS' },
        { id: 'notifications', title: 'Admin Notifications & Device Alerts', subtitle: `${store?.getUnreadNotificationsCount?.() || 0} unread notifications`, icon: 'notifications', route: 'admin/notifications', secKey: 'NOTIFICATIONS' }
      ]
    }
  };

  const cfg = hubConfigs[hubId];
  if (!cfg) return renderAdminDashboardView(state, s, ui, store);

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: cfg.title, backRoute: 'admin' })}
    <div class="card gold-border" data-testid="${cfg.testId}">
      <h2 class="section-title" style="margin:0 0 6px;">${esc(cfg.title)}</h2>
      <p class="muted text-sm" style="margin:0;">${esc(cfg.subtitle)}</p>
    </div>

    <div class="stack-md">
      ${cfg.items.map((item) => {
        const canView = item.customCanView !== undefined
          ? Boolean(item.customCanView)
          : canSessionPerm(store, item.secKey, PERMISSION_ACTIONS.VIEW);
        return `
          <button
            type="button"
            class="card interactive"
            style="text-align:left;width:100%;opacity:${canView ? '1' : '0.68'};"
            data-route="${esc(item.route)}"
            data-testid="${item.customTestId || `admin_hub_item_${item.id}`}"
          >
            <div class="row-between" style="gap:12px;">
              <div class="row-wrap" style="gap:12px;flex:1;min-width:0;">
                <span class="icon-tile ${canView ? 'tone-navy' : 'tone-muted'}">${icon(item.icon, 22)}</span>
                <div style="min-width:0;flex:1;">
                  <strong style="display:block;font-size:0.98rem;">${esc(item.title)}</strong>
                  <span class="muted text-xs">${esc(item.subtitle)}</span>
                </div>
              </div>
              ${canView ? icon('chevronRight', 20) : `<span class="badge" style="background:var(--bg-surface-variant);">${icon('lock', 14)} Locked</span>`}
            </div>
          </button>
        `;
      }).join('')}
    </div>

    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   5. ADMIN GLOBAL SEARCH & NOTIFICATIONS SCREENS
   ========================================================================== */
export function renderAdminSearchView(store, state, s, ui = {}) {
  const query = String(ui.adminSearchQuery || '');
  const results = query.trim() ? (store?.searchAdminDashboard?.(query) || []) : [];

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Admin Global Search', backRoute: 'admin' })}
    <div class="card gold-border" data-testid="admin_dashboard_search_screen">
      <div class="row-wrap" style="gap:8px;">
        <span style="color:var(--brand-burgundy);">${icon('search', 22)}</span>
        <input
          id="admin-global-search-input"
          type="search"
          class="form-input"
          style="flex:1;"
          placeholder="Search authorized hubs, sections, posts, staff, logs..."
          value="${esc(query)}"
          data-testid="mgmt_global_search_input"
        />
        ${query ? `
          <button type="button" class="icon-btn" data-action="clear-admin-search" aria-label="Clear search" data-testid="admin_search_clear_btn">
            ${icon('close', 20)}
          </button>
        ` : ''}
      </div>
      <p class="muted text-xs" style="margin:8px 0 0;">
        Results are automatically filtered by your signed-in role and section permissions.
      </p>
    </div>

    ${!query.trim() ? `
      <div class="card" style="text-align:center;padding:28px 16px;">
        <p class="muted text-sm" style="margin:0;">Type a keyword to search across your authorized management sections and records.</p>
      </div>
    ` : results.length === 0 ? `
      <div class="card" style="text-align:center;padding:28px 16px;" data-testid="admin_search_empty_state">
        <strong style="display:block;margin-bottom:4px;">No matching authorized records</strong>
        <p class="muted text-sm" style="margin:0;">No permitted hubs, sections, or records matched "${esc(query)}".</p>
      </div>
    ` : `
      <div class="stack-sm" data-testid="admin_search_results_list">
        ${results.map((res) => `
          <button
            type="button"
            class="card interactive"
            style="text-align:left;width:100%;"
            data-route="${esc(res.route)}"
            data-testid="admin_search_result_${res.id}"
          >
            <div class="row-between" style="gap:10px;">
              <div style="min-width:0;flex:1;">
                <span class="badge badge-gold" style="margin-bottom:4px;">${esc(res.categoryBadge)}</span>
                <strong style="display:block;font-size:0.96rem;">${esc(res.title)}</strong>
                <span class="muted text-xs">${esc(res.subtitle)}</span>
              </div>
              ${icon('chevronRight', 20)}
            </div>
          </button>
        `).join('')}
      </div>
    `}
    ${renderFooter(s)}
  `;
}

export function renderAdminNotificationsView(store, state, s, ui = {}) {
  if (!canSessionPerm(store, 'NOTIFICATIONS', PERMISSION_ACTIONS.VIEW)) {
    return renderAdminAccessDeniedView(store, state, s, ui, 'Notifications');
  }
  const notifications = store?.getVisibleNotifications?.() || [];
  const canEdit = canSessionPerm(store, 'NOTIFICATIONS', PERMISSION_ACTIONS.EDIT);
  const canDelete = canSessionPerm(store, 'NOTIFICATIONS', PERMISSION_ACTIONS.DELETE);
  const deviceAlerts = Boolean(state.preferences?.deviceAlertsEnabled);

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Notifications', backRoute: 'admin' })}

    <div class="card gold-border" data-testid="device_alerts_card">
      <div class="row-between" style="gap:12px;">
        <div>
          <strong style="display:block;">Device System Alerts</strong>
          <span class="muted text-xs">Receive browser/device notifications for content approvals and security events</span>
        </div>
        <label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;">
          <input
            type="checkbox"
            ${deviceAlerts ? 'checked' : ''}
            data-action="toggle-device-alerts"
            data-testid="device_alerts_switch"
          />
          <span class="text-xs"><strong>${deviceAlerts ? 'Enabled' : 'Off'}</strong></span>
        </label>
      </div>
    </div>

    ${notifications.length > 0 && (canEdit || canDelete) ? `
      <div class="row-wrap">
        ${canEdit ? `
          <button type="button" class="btn btn-outline" data-action="notifications-mark-all-read" data-testid="notifications_mark_all_read_btn">
            ${icon('factCheck', 18)} Mark All Read
          </button>
        ` : ''}
        ${canDelete ? `
          <button type="button" class="btn btn-danger" data-action="notifications-clear-all" data-testid="notifications_clear_all_btn">
            ${icon('delete', 18)} Clear All
          </button>
        ` : ''}
      </div>
    ` : ''}

    ${notifications.length === 0 ? `
      <div class="card" style="text-align:center;padding:28px 16px;" data-testid="notifications_empty_state">
        <p class="muted text-sm" style="margin:0;">No notifications in your feed.</p>
      </div>
    ` : `
      <div class="stack-sm" data-testid="notifications_list">
        ${notifications.map((n) => `
          <div
            class="card ${n.isRead ? '' : 'gold-border'}"
            style="cursor:pointer;"
            data-action="mark-notification-read"
            data-id="${n.id}"
            data-testid="notification_card_${n.id}"
          >
            <div class="row-between" style="margin-bottom:4px;">
              <span class="badge ${n.isRead ? 'badge-gold' : 'badge-burgundy'}">${esc(n.eventType)}</span>
              <span class="muted text-xs">${esc(new Date(n.createdAtMillis).toLocaleString())}</span>
            </div>
            <strong style="display:block;font-size:0.95rem;">${esc(n.title)}</strong>
            <p class="muted text-sm" style="margin:4px 0 0;">${esc(n.message)}</p>
          </div>
        `).join('')}
      </div>
    `}
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   6. STAFF MANAGEMENT SCREEN (20-SECTION x 5-ACTION RBAC MATRIX)
   ========================================================================== */
export function renderAdminStaffManagementView(store, state, s, ui = {}) {
  if (!canSessionPerm(store, 'STAFF_MANAGEMENT', PERMISSION_ACTIONS.VIEW)) {
    return renderAdminAccessDeniedView(store, state, s, ui, 'Staff Management');
  }

  const isMaster = store ? store.isMasterAdmin() : true;
  const accounts = store ? store.getManagementAccounts() : (state.managementAccounts || []);
  const superOwnerCount = accounts.filter((a) => a.role === MANAGEMENT_ROLES.SUPER_OWNER).length;
  const canAddSuperOwner = superOwnerCount < 2;

  const dialogMode = ui.staffDialogMode || null; // 'create' | 'edit' | null
  const editingAccount = dialogMode === 'edit'
    ? accounts.find((a) => Number(a.id) === Number(ui.editingStaffId))
    : null;
  const draftRole = ui.staffDraftRole || editingAccount?.role || MANAGEMENT_ROLES.STAFF;
  const draftPermissions = normalizePermissionsMatrix(
    ui.staffDraftPermissions || editingAccount?.permissions || buildDefaultPermissionsForRole(draftRole),
    draftRole
  );

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Staff Management', backRoute: 'admin' })}

    <div class="card gold-border" data-testid="staff_management_screen">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div>
          <span class="badge badge-gold">${accounts.length} Accounts · ${superOwnerCount}/2 Super Owners</span>
          <h2 class="section-title" style="margin:4px 0 2px;">Staff &amp; Role Access Control</h2>
          <p class="muted text-sm" style="margin:0;">
            ${isMaster
              ? 'Create Staff or Super Owner accounts, assign granular 20-section × 5-action permissions (View, Create, Edit, Delete, Publish), reset PINs, or deactivate access.'
              : 'Only Master Admin can create, modify, or deactivate staff accounts and section permissions.'}
          </p>
        </div>
        ${isMaster ? `
          <button type="button" class="btn btn-primary" data-action="open-create-staff" data-testid="create_staff_account_btn">
            ${icon('add', 18)} Add Staff / Super Owner
          </button>
        ` : ''}
      </div>
      ${ui.staffFeedback ? `<div class="badge" style="margin-top:10px;background:var(--brand-forest-soft);color:var(--brand-forest-on-container);padding:8px 12px;">${esc(ui.staffFeedback)}</div>` : ''}
      ${ui.staffError ? `<div class="badge" style="margin-top:10px;background:var(--danger-soft);color:var(--danger);padding:8px 12px;" data-testid="staff_error_banner">${esc(ui.staffError)}</div>` : ''}
    </div>

    ${isMaster && dialogMode ? `
      <form id="staff-account-dialog-form" class="card gold-border" data-mode="${esc(dialogMode)}" data-account-id="${editingAccount ? editingAccount.id : ''}" data-testid="staff_account_dialog">
        <div class="row-between" style="margin-bottom:12px;">
          <h3 class="section-title" style="margin:0;">${dialogMode === 'create' ? 'Create Staff / Super Owner Account' : `Edit Account: @${esc(editingAccount?.username || '')}`}</h3>
          <button type="button" class="icon-btn" data-action="close-staff-dialog" aria-label="Close">${icon('close', 20)}</button>
        </div>

        ${dialogMode === 'create' ? `
          <div class="form-group">
            <label class="form-label">Account Role</label>
            <div class="chip-row">
              <button
                type="button"
                class="chip ${draftRole === MANAGEMENT_ROLES.STAFF ? 'selected' : ''}"
                data-action="staff-draft-role"
                data-role="STAFF"
                data-testid="staff_role_staff_btn"
              >
                Staff (Granular Permissions)
              </button>
              <button
                type="button"
                class="chip ${draftRole === MANAGEMENT_ROLES.SUPER_OWNER ? 'selected' : ''}"
                data-action="staff-draft-role"
                data-role="SUPER_OWNER"
                ${!canAddSuperOwner && draftRole !== MANAGEMENT_ROLES.SUPER_OWNER ? 'disabled' : ''}
                data-testid="staff_role_super_owner_btn"
              >
                Super Owner (${superOwnerCount}/2)
              </button>
            </div>
            <input type="hidden" name="role" value="${esc(draftRole)}" />
          </div>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Username (lowercase)</label>
              <input name="username" class="form-input" value="${esc(ui.staffDraftUsername || '')}" required placeholder="e.g. editor_science" data-testid="staff_username_input" />
            </div>
            <div class="form-group">
              <label class="form-label">Display Name</label>
              <input name="displayName" class="form-input" value="${esc(ui.staffDraftDisplayName || '')}" required placeholder="e.g. Science Dept Coordinator" data-testid="staff_displayname_input" />
            </div>
          </div>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Initial 6+ Digit PIN</label>
              <input name="pin" type="password" inputmode="numeric" minlength="6" class="form-input" required placeholder="Minimum 6 numeric digits" data-testid="staff_pin_input" />
            </div>
            <div class="form-group">
              <label class="form-label">Confirm 6+ Digit PIN</label>
              <input name="confirmPin" type="password" inputmode="numeric" minlength="6" class="form-input" required placeholder="Re-enter 6+ digit PIN" data-testid="staff_confirm_pin_input" />
            </div>
          </div>
        ` : `
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Display Name</label>
              <input name="displayName" class="form-input" value="${esc(ui.staffDraftDisplayName ?? editingAccount?.displayName ?? '')}" required data-testid="staff_displayname_input" />
            </div>
            <div class="form-group">
              <label class="form-label">Optional New 6+ Digit PIN (leave blank to keep current)</label>
              <input name="newPin" type="password" inputmode="numeric" minlength="6" class="form-input" placeholder="6+ digits to reset PIN" data-testid="staff_pin_input" />
            </div>
          </div>
          <div class="form-group">
            <label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;">
              <input type="checkbox" name="isActive" ${(ui.staffDraftIsActive ?? editingAccount?.isActive ?? true) ? 'checked' : ''} data-testid="staff_active_checkbox" />
              <strong>Account Active (Uncheck to immediately block login &amp; revoke active session)</strong>
            </label>
          </div>
        `}

        <div class="row-between" style="margin:12px 0 8px;flex-wrap:wrap;gap:8px;">
          <div>
            <strong style="display:block;">20-Section × 5-Action RBAC Permission Matrix</strong>
            <span class="muted text-xs">V = View · C = Create · E = Edit · D = Delete · P = Publish</span>
          </div>
          <div class="row-wrap" style="gap:6px;">
            <button type="button" class="btn btn-outline" data-action="staff-perms-grant-all" data-testid="staff_perms_grant_all_btn">Grant All</button>
            <button type="button" class="btn btn-outline" data-action="staff-perms-clear-all" data-testid="staff_perms_clear_all_btn">Clear All</button>
          </div>
        </div>

        <div class="rbac-matrix-table-wrap" data-testid="staff_permission_matrix">
          <table class="rbac-matrix-table">
            <thead>
              <tr>
                <th>Management Section (20)</th>
                <th>VIEW</th>
                <th>CREATE</th>
                <th>EDIT</th>
                <th>DELETE</th>
                <th>PUBLISH</th>
              </tr>
            </thead>
            <tbody>
              ${MANAGEMENT_SECTIONS.map((sec) => {
                const row = draftPermissions.find((p) => p.sectionKey === sec.key) || {};
                return `
                  <tr data-testid="perm_row_${sec.key}">
                    <td><strong>${esc(sec.label)}</strong></td>
                    <td><input type="checkbox" name="perm_${sec.key}_VIEW" ${row.canView ? 'checked' : ''} data-testid="perm_${sec.key}_VIEW" /></td>
                    <td><input type="checkbox" name="perm_${sec.key}_CREATE" ${row.canCreate ? 'checked' : ''} data-testid="perm_${sec.key}_CREATE" /></td>
                    <td><input type="checkbox" name="perm_${sec.key}_EDIT" ${row.canEdit ? 'checked' : ''} data-testid="perm_${sec.key}_EDIT" /></td>
                    <td><input type="checkbox" name="perm_${sec.key}_DELETE" ${row.canDelete ? 'checked' : ''} data-testid="perm_${sec.key}_DELETE" /></td>
                    <td>
                      ${sec.supportsPublish
                        ? `<input type="checkbox" name="perm_${sec.key}_PUBLISH" ${row.canPublish ? 'checked' : ''} data-testid="perm_${sec.key}_PUBLISH" />`
                        : `<span class="muted text-xs">—</span>`}
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>

        <div class="row-wrap" style="margin-top:14px;">
          <button type="submit" class="btn btn-primary" data-testid="staff_dialog_submit_btn">
            ${icon('publish', 18)} ${dialogMode === 'create' ? 'Create Account' : 'Save Account Changes'}
          </button>
          <button type="button" class="btn btn-outline" data-action="close-staff-dialog">Cancel</button>
        </div>
      </form>
    ` : ''}

    <div class="stack-md" data-testid="staff_accounts_list">
      ${accounts.map((acc) => {
        const perms = normalizePermissionsMatrix(acc.permissions, acc.role);
        const viewCount = acc.role === MANAGEMENT_ROLES.MASTER_ADMIN
          ? MANAGEMENT_SECTIONS.length
          : perms.filter((p) => p.canView).length;
        return `
          <div class="card" data-testid="staff_account_card_${acc.id}">
            <div class="row-between" style="flex-wrap:wrap;gap:10px;">
              <div>
                <div class="row-wrap" style="gap:8px;margin-bottom:4px;">
                  <strong>${esc(acc.displayName)}</strong>
                  <span class="badge ${acc.role === MANAGEMENT_ROLES.MASTER_ADMIN ? 'badge-burgundy' : 'badge-gold'}">${esc(roleBadgeLabel(acc.role))}</span>
                  <span class="badge" style="background:${acc.isActive ? 'var(--brand-forest-soft)' : 'var(--danger-soft)'};color:${acc.isActive ? 'var(--brand-forest-on-container)' : 'var(--danger)'};">
                    ${acc.isActive ? 'Active' : 'Deactivated'}
                  </span>
                </div>
                <div class="muted text-xs">
                  @${esc(acc.username)} · ${acc.role === MANAGEMENT_ROLES.MASTER_ADMIN ? 'All 20 sections (Full Unrestricted Access)' : `${viewCount}/${MANAGEMENT_SECTIONS.length} viewable sections`}
                </div>
              </div>
              ${isMaster && acc.role !== MANAGEMENT_ROLES.MASTER_ADMIN ? `
                <div class="row-wrap" style="gap:6px;">
                  <button type="button" class="btn btn-outline" data-action="open-edit-staff" data-id="${acc.id}" data-testid="edit_staff_btn_${acc.id}">
                    ${icon('edit', 16)} Edit / Permissions
                  </button>
                  <button type="button" class="btn btn-danger" data-action="delete-staff-account" data-id="${acc.id}" data-testid="delete_staff_btn_${acc.id}">
                    ${icon('delete', 16)} Delete
                  </button>
                </div>
              ` : ''}
            </div>
          </div>
        `;
      }).join('')}
    </div>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   7. SECTION & GOVERNANCE SCREENS (ALL 20 MANAGEMENT SECTIONS + APPROVALS + VERSIONS)
   ========================================================================== */
export function renderAdminSectionView(store, state, s, ui = {}, sectionKey) {
  // Dedicated routes first
  if (sectionKey === 'STAFF_MANAGEMENT') {
    return renderAdminStaffManagementView(store, state, s, ui);
  }
  if (sectionKey === 'NOTIFICATIONS') {
    return renderAdminNotificationsView(store, state, s, ui);
  }
  if (sectionKey === 'APPROVAL_CENTER') {
    if (store && !store.canAccessApprovalCenter()) {
      return renderAdminAccessDeniedView(store, state, s, ui, 'Approval Center');
    }
    const filterTab = ui.approvalFilterTab || 'ALL';
    const allApprovals = state.contentApprovals || [];
    const approvals = filterTab === 'ALL'
      ? allApprovals
      : allApprovals.filter((a) => a.status === filterTab);
    const v2PendingRecords = ((state.academicsV2 && state.academicsV2.records) || []).filter(
      (r) => r.status === ACADEMICS_V2_RECORD_STATUS.PENDING_APPROVAL
    );
    const isOwner = store ? store.isSchoolOwner() : true;

    return `
      ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Approval Center', backRoute: 'admin/hub/management_tools' })}
      <div class="card gold-border" data-testid="approval_center_screen">
        <div class="row-between" style="flex-wrap:wrap;gap:10px;">
          <div>
            <h2 class="section-title" style="margin:0 0 4px;">Content Approval Center</h2>
            <p class="muted text-sm" style="margin:0;">Review pending publishing requests submitted by staff accounts across CMS and Academics V2.</p>
          </div>
          <button type="button" class="btn btn-outline" data-route="admin/preview-publish" data-testid="approval_open_preview_publish_btn">
            ${icon('publish', 16)} Open Preview &amp; Publish Hub
          </button>
        </div>
        <div class="chip-row" style="margin-top:12px;">
          ${['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map((tab) => `
            <button
              type="button"
              class="chip ${filterTab === tab ? 'selected' : ''}"
              data-action="approval-filter-tab"
              data-tab="${tab}"
              data-testid="approval_tab_${tab}"
            >
              ${tab} (${tab === 'ALL' ? allApprovals.length : allApprovals.filter((a) => a.status === tab).length})
            </button>
          `).join('')}
        </div>
      </div>

      ${v2PendingRecords.length > 0 && (filterTab === 'ALL' || filterTab === 'PENDING') ? `
        <div class="card gold-border" data-testid="approval_v2_pending_list">
          <h3 class="section-title" style="margin:0 0 8px;">Academics V2 Pending Approval (${v2PendingRecords.length})</h3>
          <div class="stack-sm">
            ${v2PendingRecords.map((rec) => {
              const isExamResult = rec.categoryKey === 'examinations' && rec.sectionKey === 'exam_results';
              const canApproveV2 = isExamResult
                ? isOwner
                : (isOwner || (store && store.canPublishV2Directly(rec.categoryKey, rec.sectionKey)));
              return `
                <div class="card" data-testid="v2_approval_card_${rec.id}">
                  <div class="row-between" style="margin-bottom:4px;">
                    <span class="badge ${isExamResult ? 'badge-burgundy' : 'badge-gold'}">${esc(rec.categoryKey)} · ${esc(rec.sectionKey)}${isExamResult ? ' (Owner Exam Gate)' : ''}</span>
                    <span class="badge badge-gold">${esc(rec.status)}</span>
                  </div>
                  <strong style="display:block;">${esc(rec.title)}</strong>
                  <p class="muted text-xs" style="margin:4px 0 8px;">Updated by ${esc(rec.updatedByName)} (${esc(rec.updatedByRole)})</p>
                  ${canApproveV2 ? `
                    <div class="row-wrap">
                      <button type="button" class="btn btn-forest" data-action="v2-owner-approve-publish" data-id="${rec.id}" data-exam="${isExamResult ? 'true' : 'false'}" data-testid="v2_approve_publish_btn_${rec.id}">
                        ${icon('factCheck', 16)} ${isExamResult ? 'Owner Authorize & Publish Exam Result' : 'Approve & Publish'}
                      </button>
                      <button type="button" class="btn btn-danger" data-action="v2-set-record-status" data-id="${rec.id}" data-status="DRAFT" data-testid="v2_reject_to_draft_btn_${rec.id}">
                        ${icon('close', 16)} Return to Draft
                      </button>
                    </div>
                  ` : `<span class="badge badge-burgundy">Requires School Owner Authorization</span>`}
                </div>
              `;
            }).join('')}
          </div>
        </div>
      ` : ''}

      ${approvals.length === 0 ? `
        <div class="card" style="text-align:center;padding:24px;"><p class="muted text-sm" style="margin:0;">No approval requests in this filter.</p></div>
      ` : `
        <div class="stack-sm" data-testid="approval_items_list">
          ${approvals.map((a) => {
            const canPublishThis = canSessionPerm(store, a.sectionKey, PERMISSION_ACTIONS.PUBLISH);
            return `
              <div class="card" data-testid="approval_card_${a.id}">
                <div class="row-between" style="margin-bottom:6px;">
                  <span class="badge ${a.status === 'APPROVED' ? 'badge-forest' : a.status === 'REJECTED' ? 'badge-burgundy' : 'badge-gold'}">${esc(a.status)}</span>
                  <span class="muted text-xs">${esc(a.sectionKey)} · ${esc(a.actionType)}</span>
                </div>
                <strong style="display:block;">${esc(a.itemTitle)}</strong>
                <p class="muted text-xs" style="margin:4px 0 8px;">Requested by ${esc(a.requestedByName)} (${esc(a.requestedByRole)})${a.reviewNote ? ` · Note: ${esc(a.reviewNote)}` : ''}</p>
                ${a.status === 'PENDING' && canPublishThis ? `
                  <div class="row-wrap">
                    <button type="button" class="btn btn-forest" data-action="review-approval" data-id="${a.id}" data-approve="true" data-testid="approve_btn_${a.id}">${icon('factCheck', 16)} Approve &amp; Publish</button>
                    <button type="button" class="btn btn-danger" data-action="review-approval" data-id="${a.id}" data-approve="false" data-testid="reject_btn_${a.id}">${icon('close', 16)} Reject</button>
                  </div>
                ` : ''}
              </div>
            `;
          }).join('')}
        </div>
      `}
      ${renderFooter(s)}
    `;
  }

  if (sectionKey === 'VERSION_HISTORY') {
    if (store && !store.canAccessVersionHistory()) {
      return renderAdminAccessDeniedView(store, state, s, ui, 'Version History');
    }
    const versions = store?.getVisibleVersionHistory?.() || state.versionHistory || [];
    const inspectedId = ui.inspectedVersionId || null;
    return `
      ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Version History', backRoute: 'admin/hub/management_tools' })}
      <div class="card gold-border" data-testid="version_history_screen">
        <h2 class="section-title" style="margin:0 0 4px;">Version History &amp; Rollback</h2>
        <p class="muted text-sm" style="margin:0;">Inspect historical content snapshots (JSON payload) and restore prior versions as published or draft.</p>
      </div>
      ${versions.length === 0 ? `
        <div class="card" style="text-align:center;padding:24px;"><p class="muted text-sm" style="margin:0;">No version snapshots recorded yet.</p></div>
      ` : `
        <div class="stack-sm" data-testid="version_history_list">
          ${versions.map((v) => {
            const canRestoreEdit = canSessionPerm(store, v.sectionKey, PERMISSION_ACTIONS.EDIT);
            const canRestorePublish = canSessionPerm(store, v.sectionKey, PERMISSION_ACTIONS.PUBLISH);
            const isInspected = Number(inspectedId) === Number(v.id);
            return `
              <div class="card" data-testid="version_card_${v.id}">
                <div class="row-between" style="margin-bottom:4px;">
                  <span class="badge badge-gold">v${v.versionNumber} · ${esc(v.sectionKey)} · ${esc(v.approvalStatus || 'PUBLISHED')}</span>
                  <span class="muted text-xs">${esc(new Date(v.createdAtMillis).toLocaleString())}</span>
                </div>
                <strong style="display:block;">${esc(v.recordTitle || v.itemTitle)}</strong>
                <p class="muted text-xs" style="margin:4px 0 8px;">${esc(v.changeSummary || v.snapshotSummary)} · by ${esc(v.changedByName || v.editorName)}</p>
                <div class="row-wrap" style="gap:8px;">
                  <button type="button" class="btn btn-outline" data-action="inspect-version-json" data-id="${v.id}" data-testid="inspect_version_btn_${v.id}">
                    ${icon('visibility', 16)} ${isInspected ? 'Hide Snapshot JSON' : 'Inspect Snapshot'}
                  </button>
                  ${canRestoreEdit ? `
                    <button type="button" class="btn btn-primary" data-action="restore-version" data-id="${v.id}" data-publish="${canRestorePublish ? 'true' : 'false'}" data-testid="restore_version_btn_${v.id}">
                      ${icon('schedule', 16)} ${canRestorePublish ? 'Restore & Publish' : 'Restore as Draft'}
                    </button>
                  ` : ''}
                  ${canRestoreEdit && canRestorePublish ? `
                    <button type="button" class="btn btn-outline-gold" data-action="restore-version" data-id="${v.id}" data-publish="false" data-testid="restore_version_draft_btn_${v.id}">
                      Restore as Draft Only
                    </button>
                  ` : ''}
                </div>
                ${isInspected ? `
                  <pre style="margin-top:10px;padding:10px;background:var(--bg-surface-variant);border-radius:8px;font-size:0.75rem;overflow-x:auto;" data-testid="version_snapshot_json_${v.id}">${esc(v.contentSnapshotJson || '{}')}</pre>
                ` : ''}
              </div>
            `;
          }).join('')}
        </div>
      `}
      ${renderFooter(s)}
    `;
  }

  // Standard 20 ManagementSection permission gate
  const secMeta = MANAGEMENT_SECTION_MAP[sectionKey];
  if (secMeta && !canSessionPerm(store, sectionKey, PERMISSION_ACTIONS.VIEW)) {
    return renderAdminAccessDeniedView(store, state, s, ui, secMeta.label);
  }

  const canCreate = canSessionPerm(store, sectionKey, PERMISSION_ACTIONS.CREATE);
  const canEdit = canSessionPerm(store, sectionKey, PERMISSION_ACTIONS.EDIT);
  const canDelete = canSessionPerm(store, sectionKey, PERMISSION_ACTIONS.DELETE);
  const canPublish = canSessionPerm(store, sectionKey, PERMISSION_ACTIONS.PUBLISH);

  let body = '';

  if (sectionKey === 'SCHOOL_POSTS') {
    const posts = state.schoolPosts || [];
    body = `
      ${canCreate ? `
        <form class="card gold-border" id="cms-school-post-form" data-testid="school_posts_composer_form">
          <h2 class="section-title" style="margin:0 0 12px;">Create School Social Feed Post</h2>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Post Type</label>
              <select name="postType" class="form-input" data-testid="school_post_type_select">
                <option value="${SCHOOL_POST_TYPES.PHOTO}">Photo Post (Up to 10 Photos)</option>
                <option value="${SCHOOL_POST_TYPES.YOUTUBE_DIRECT}">YouTube Video Post</option>
                <option value="${SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO}">Custom Thumbnail + YouTube Video</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Category</label>
              <select name="category" class="form-input" data-testid="school_post_category_select">
                ${SCHOOL_POST_CATEGORIES.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('')}
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Post Title</label>
            <input name="title" class="form-input" required placeholder="Headline for Home & Academy feed" data-testid="school_post_title_input" />
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea name="description" class="form-textarea" placeholder="Details, caption, or event summary"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">Photo URLs (comma-separated, or for Photo Posts) / YouTube Video URL</label>
            <input name="mediaInput" class="form-input" required placeholder="https://www.youtube.com/watch?v=... or image URL(s)" data-testid="school_post_media_input" />
          </div>
          <div class="row-wrap">
            <label style="display:inline-flex;align-items:center;gap:6px;">
              <input type="checkbox" name="isFeatured" /> <span>Pin as Featured Post</span>
            </label>
            ${canPublish ? `
              <label style="display:inline-flex;align-items:center;gap:6px;">
                <input type="checkbox" name="publishNow" checked /> <span>Publish Immediately</span>
              </label>
            ` : `<span class="badge badge-gold">Draft Mode (No Publish Permission)</span>`}
          </div>
          <button type="submit" class="btn btn-primary" style="margin-top:12px;" data-testid="school_post_submit_btn">
            ${icon('publish', 18)} Save School Post
          </button>
        </form>
      ` : ''}
      <div class="card" data-testid="school_posts_admin_list">
        <h3 class="section-title" style="margin:0 0 10px;">Managed School Posts (${posts.length})</h3>
        ${posts.length === 0 ? '<p class="muted text-sm">No school posts created yet.</p>' : posts.map((p) => `
          <div class="row-between" style="padding:10px 0;border-bottom:1px solid var(--border-subtle);gap:10px;" data-testid="admin_post_row_${p.id}">
            <div style="min-width:0;flex:1;">
              <span class="badge badge-gold">${esc(p.postType)} · ${esc(p.status)}</span>
              <strong style="display:block;">${esc(p.title)}</strong>
              <span class="muted text-xs">${esc(p.category)}</span>
            </div>
            <div class="row-wrap" style="gap:6px;">
              ${canPublish && p.status !== SCHOOL_POST_STATUS.PUBLISHED ? `
                <button type="button" class="btn btn-forest" data-action="set-post-status" data-id="${p.id}" data-status="PUBLISHED">Publish</button>
              ` : ''}
              ${canEdit && p.status === SCHOOL_POST_STATUS.PUBLISHED ? `
                <button type="button" class="btn btn-outline" data-action="set-post-status" data-id="${p.id}" data-status="UNPUBLISHED">Unpublish</button>
              ` : ''}
              ${canDelete ? `
                <button type="button" class="icon-btn" data-action="delete-school-post" data-id="${p.id}" aria-label="Delete Post">${icon('delete', 18)}</button>
              ` : ''}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  } else if (sectionKey === 'SCHOOL_HIGHLIGHTS') {
    const highlights = state.schoolHighlights || [];
    body = `
      ${canCreate ? `
        <form class="card gold-border" id="cms-highlight-form" data-testid="school_highlights_composer_form">
          <h2 class="section-title" style="margin:0 0 12px;">Add 24-Hour YouTube Shorts Highlight</h2>
          <div class="form-group">
            <label class="form-label">Highlight Title</label>
            <input name="title" class="form-input" required placeholder="e.g. Morning Assembly Highlight" data-testid="highlight_title_input" />
          </div>
          <div class="form-group">
            <label class="form-label">YouTube Shorts URL (youtube.com/shorts/...)</label>
            <input name="youtubeShortsUrl" class="form-input" required placeholder="https://www.youtube.com/shorts/..." data-testid="highlight_url_input" />
          </div>
          <button type="submit" class="btn btn-primary" data-testid="highlight_submit_btn">
            ${icon('publish', 18)} ${canPublish ? 'Publish 24h Highlight' : 'Save Draft Highlight'}
          </button>
        </form>
      ` : ''}
      <div class="card" data-testid="school_highlights_admin_list">
        <h3 class="section-title" style="margin:0 0 10px;">24-Hour Highlights (${highlights.length})</h3>
        ${highlights.length === 0 ? '<p class="muted text-sm">No highlights added yet.</p>' : highlights.map((h) => `
          <div class="row-between" style="padding:10px 0;border-bottom:1px solid var(--border-subtle);">
            <div>
              <strong>${esc(h.title || 'YouTube Short')}</strong>
              <div class="muted text-xs">${esc(h.youtubeShortsUrl)} · ${esc(h.status)}</div>
            </div>
            ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-school-highlight" data-id="${h.id}" aria-label="Delete Highlight">${icon('delete', 18)}</button>` : ''}
          </div>
        `).join('')}
      </div>
    `;
  } else if (sectionKey === 'MEDIA_STUDIO') {
    const items = state.mediaStudioItems || [];
    const editingMediaId = ui.editingMediaStudioId || null;
    body = `
      ${canCreate ? `
        <form class="card gold-border" id="cms-media-studio-form" data-testid="media_studio_form">
          <h2 class="section-title" style="margin:0 0 12px;">Media Studio Asset Manager</h2>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Asset Title</label>
              <input name="title" class="form-input" required data-testid="media_studio_title_input" />
            </div>
            <div class="form-group">
              <label class="form-label">Category / Album</label>
              <input name="collectionName" class="form-input" value="Campus" required data-testid="media_studio_collection_input" />
            </div>
          </div>
          <div class="two-col-responsive">
            <div class="form-group">
              <label class="form-label">Media Type</label>
              <select name="mediaType" class="form-input" data-testid="media_studio_type_select">
                <option value="PHOTO">Photo</option>
                <option value="YOUTUBE_VIDEO">YouTube Video</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Image URL or YouTube URL</label>
              <input name="mediaUri" class="form-input" required placeholder="https://..." data-testid="media_studio_uri_input" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Optional Caption</label>
            <input name="caption" class="form-input" placeholder="Caption for Hero Banner or Gallery Album" data-testid="media_studio_caption_input" />
          </div>
          <button type="submit" class="btn btn-primary" data-testid="media_studio_add_btn">${icon('add', 18)} Add Media Asset</button>
        </form>
      ` : ''}
      <div class="card" data-testid="media_studio_list">
        <h3 class="section-title" style="margin:0 0 10px;">Media Studio Items (${items.length})</h3>
        ${items.length === 0 ? '<p class="muted text-sm">No studio items yet.</p>' : items.map((m) => {
          const warnings = store?.getMediaUsageWarnings?.(m.id) || [];
          const isEditingThis = Number(editingMediaId) === Number(m.id);
          return `
            <div style="padding:12px 0;border-bottom:1px solid var(--border-subtle);" data-testid="media_studio_item_${m.id}">
              <div class="row-between" style="flex-wrap:wrap;gap:10px;">
                <div>
                  <div class="row-wrap" style="gap:6px;margin-bottom:4px;">
                    <span class="badge badge-gold">${esc(m.mediaType)} · ${esc(m.approvalStatus)}</span>
                    ${warnings.length > 0 ? `<span class="badge badge-burgundy" data-testid="media_usage_warning_${m.id}">In Use (${warnings.join(', ')})</span>` : ''}
                  </div>
                  <strong style="display:block;">${esc(m.title)}</strong>
                  <span class="muted text-xs">${esc(m.categoryOrAlbum || m.collectionName)} · ${esc(m.caption || '')}</span>
                </div>
                <div class="row-wrap" style="gap:6px;">
                  ${canEdit ? `
                    <button type="button" class="btn btn-outline" data-action="toggle-edit-media-studio" data-id="${m.id}" data-testid="edit_media_studio_btn_${m.id}">
                      ${icon('edit', 16)} ${isEditingThis ? 'Close Edit' : 'Edit / Replace'}
                    </button>
                    <button type="button" class="btn btn-outline-gold" data-action="assign-media-hero" data-id="${m.id}" data-testid="assign_media_hero_btn_${m.id}">
                      Assign to Hero
                    </button>
                    <button type="button" class="btn btn-forest" data-action="assign-media-album" data-id="${m.id}" data-album="${esc(m.categoryOrAlbum || m.collectionName || 'Campus')}" data-testid="assign_media_album_btn_${m.id}">
                      Assign to Album
                    </button>
                  ` : ''}
                  ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-media-studio" data-id="${m.id}" aria-label="Delete" data-testid="delete_media_studio_btn_${m.id}">${icon('delete', 18)}</button>` : ''}
                </div>
              </div>
              ${isEditingThis && canEdit ? `
                <form class="media-metadata-form card" data-id="${m.id}" style="margin-top:10px;padding:12px;" data-testid="media_metadata_form_${m.id}">
                  <div class="two-col-responsive">
                    <div class="form-group"><label class="form-label">Title</label><input name="title" class="form-input" value="${esc(m.title)}" required /></div>
                    <div class="form-group"><label class="form-label">Album / Category</label><input name="categoryOrAlbum" class="form-input" value="${esc(m.categoryOrAlbum || m.collectionName || 'Campus')}" required /></div>
                  </div>
                  <div class="two-col-responsive">
                    <div class="form-group"><label class="form-label">Caption</label><input name="caption" class="form-input" value="${esc(m.caption || '')}" /></div>
                    <div class="form-group"><label class="form-label">Replace Image / Media URI (optional)</label><input name="newMediaUri" class="form-input" value="${esc(m.mediaUri || '')}" /></div>
                  </div>
                  <button type="submit" class="btn btn-primary" data-testid="save_media_metadata_btn_${m.id}">${icon('publish', 16)} Save Metadata &amp; URI</button>
                </form>
              ` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  } else if (sectionKey === 'ACTIVITY_LOG') {
    const allLogs = store?.getVisibleActivityLogs?.() || state.admin?.activityLog || [];
    const distinctSections = ['ALL', ...Array.from(new Set(allLogs.map((l) => l.section || 'GENERAL').filter(Boolean)))];
    const query = ui.activityLogQuery || '';
    const sectionFilter = ui.activityLogSection || 'ALL';
    const sortNewestFirst = ui.activityLogSortNewest !== false;
    const logs = store?.filterActivityLogs
      ? store.filterActivityLogs({ query, sectionKey: sectionFilter, sortNewestFirst })
      : allLogs;

    body = `
      <div class="card gold-border" data-testid="activity_log_filter_card">
        <div class="row-between" style="flex-wrap:wrap;gap:10px;margin-bottom:10px;">
          <h2 class="section-title" style="margin:0;">Audit Activity Log (${logs.length})</h2>
          <button
            type="button"
            class="btn btn-outline"
            data-action="toggle-activity-log-sort"
            data-testid="activity_log_sort_btn"
          >
            ${icon('schedule', 16)} Sort: ${sortNewestFirst ? 'Newest First' : 'Oldest First'}
          </button>
        </div>
        <div class="form-group" style="margin-bottom:10px;">
          <input
            type="search"
            id="activity-log-search-input"
            class="form-input"
            placeholder="Search by actor, action, section, or details..."
            value="${esc(query)}"
            data-testid="activity_log_search_input"
          />
        </div>
        <div class="chip-row" data-testid="activity_log_section_chips">
          ${distinctSections.map((sec) => `
            <button
              type="button"
              class="chip ${sectionFilter === sec ? 'selected' : ''}"
              data-action="activity-log-section"
              data-section="${esc(sec)}"
              data-testid="activity_log_chip_${esc(sec)}"
            >
              ${esc(sec === 'ALL' ? 'All Sections' : sec)}
            </button>
          `).join('')}
        </div>
      </div>

      <div class="card" data-testid="activity_log_list">
        ${logs.length === 0 ? '<p class="muted text-sm">No activity matched the current filters.</p>' : logs.slice(0, 60).map((l) => `
          <div style="padding:8px 0;border-bottom:1px solid var(--border-subtle);" data-testid="activity_log_row_${l.id || l.createdAtMillis}">
            <div class="row-between">
              <strong class="text-sm">${esc(l.section)} · ${esc(l.action)}</strong>
              <span class="muted text-xs">${esc(new Date(l.createdAtMillis || l.timestamp).toLocaleString())}</span>
            </div>
            <div class="muted text-xs">${esc(l.details)} — <em>${esc(l.actorName || l.actor || 'Admin')}</em></div>
          </div>
        `).join('')}
      </div>
    `;
  } else if (sectionKey === 'BACKUP_RESTORE') {
    const report = ui.backupInspectionReport || null;
    body = `
      <div class="card gold-border" data-testid="backup_restore_screen">
        <h2 class="section-title" style="margin:0 0 8px;">JSON Backup &amp; Restore</h2>
        <p class="muted text-sm" style="margin:0 0 14px;">
          Export verified school records as JSON (salted PINs &amp; recovery hashes are automatically excluded) or inspect and restore a backup file.
        </p>
        <div class="form-group" style="max-width:360px;">
          <label class="form-label" for="backup-password-input">Optional Backup Password (for verification)</label>
          <input
            type="password"
            id="backup-password-input"
            class="form-input"
            placeholder="Optional backup password"
            value="${esc(ui.backupPassword || '')}"
            data-testid="backup_password_input"
          />
        </div>
        <div class="row-wrap">
          <button type="button" class="btn btn-primary" data-action="export-backup" data-testid="export_backup_btn">${icon('download', 18)} Export Backup JSON</button>
          ${canEdit ? `
            <label class="btn btn-outline" style="cursor:pointer;" data-testid="import_backup_label">
              ${icon('upload', 18)} Inspect &amp; Restore Backup File
              <input type="file" id="import-backup-input" accept="application/json" style="display:none;" />
            </label>
          ` : ''}
        </div>
      </div>

      ${report ? `
        <div class="card gold-border" data-testid="backup_inspection_summary_card">
          <div class="row-between" style="margin-bottom:8px;">
            <strong>Backup Inspection Summary</strong>
            <span class="badge badge-forest">Schema v${esc(report.schemaVersion)} · App ${esc(report.appVersionName)}</span>
          </div>
          <div class="two-col-responsive" style="margin-bottom:12px;">
            <div class="text-xs"><strong>Accounts:</strong> ${report.accountCount}</div>
            <div class="text-xs"><strong>Content Records:</strong> ${report.contentRecordCount}</div>
            <div class="text-xs"><strong>Gallery Items:</strong> ${report.galleryItemCount}</div>
            <div class="text-xs"><strong>Media Studio Files:</strong> ${report.mediaFileCount}</div>
            <div class="text-xs"><strong>History &amp; Audit Logs:</strong> ${report.historyAndLogCount}</div>
            <div class="text-xs"><strong>Exported At:</strong> ${esc(new Date(report.exportedAtMillis).toLocaleString())}</div>
          </div>
          ${canEdit ? `
            <button type="button" class="btn btn-forest" data-action="confirm-restore-inspected-backup" data-testid="confirm_restore_backup_btn">
              ${icon('factCheck', 18)} Confirm &amp; Restore This Backup
            </button>
          ` : ''}
        </div>
      ` : ''}
    `;
  } else if (sectionKey === 'APP_SETTINGS') {
    const lang = state.preferences?.language || 'en';
    const theme = state.preferences?.theme || 'system';
    body = `
      <div class="card gold-border" data-testid="admin_app_settings_screen">
        <h2 class="section-title" style="margin:0 0 12px;">App Settings (Language &amp; Appearance)</h2>
        <div class="form-group">
          <label class="form-label">Language</label>
          <div class="chip-row">
            <button type="button" class="chip ${lang === 'en' ? 'selected' : ''}" data-action="set-language" data-lang="en">English</button>
            <button type="button" class="chip ${lang === 'ta' ? 'selected' : ''}" data-action="set-language" data-lang="ta">தமிழ் (Tamil)</button>
            <button type="button" class="chip ${lang === 'hi' ? 'selected' : ''}" data-action="set-language" data-lang="hi">हिन्दी (Hindi)</button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Theme</label>
          <div class="chip-row">
            <button type="button" class="chip ${theme === 'system' ? 'selected' : ''}" data-action="set-theme" data-theme="system">System Default</button>
            <button type="button" class="chip ${theme === 'light' ? 'selected' : ''}" data-action="set-theme" data-theme="light">Light</button>
            <button type="button" class="chip ${theme === 'dark' ? 'selected' : ''}" data-action="set-theme" data-theme="dark">Dark</button>
          </div>
        </div>
      </div>
    `;
  } else if (sectionKey === 'SCHOOL_PROFILE' || sectionKey === 'ABOUT_SCHOOL' || sectionKey === 'CONTACT_DETAILS') {
    body = renderCmsSectionBody(state, 'profile', { canCreate, canEdit, canDelete, canPublish }, ui);
  } else if (sectionKey === 'ACADEMICS' || sectionKey === 'HIGHER_SECONDARY_COURSES') {
    body = renderCmsSectionBody(state, 'academics', { canCreate, canEdit, canDelete, canPublish }, ui);
  } else if (sectionKey === 'NEWS' || sectionKey === 'ANNOUNCEMENTS') {
    body = renderCmsSectionBody(state, 'updates', { canCreate, canEdit, canDelete, canPublish }, ui);
  } else if (sectionKey === 'LOGO_BANNER' || sectionKey === 'PHOTO_GALLERY') {
    body = renderCmsSectionBody(state, 'media', { canCreate, canEdit, canDelete, canPublish }, ui);
  } else if (sectionKey === 'FACILITIES' || sectionKey === 'TRANSPORT') {
    body = renderCmsSectionBody(state, 'campus', { canCreate, canEdit, canDelete, canPublish }, ui);
  } else if (sectionKey === 'SOCIAL_MEDIA') {
    body = renderCmsSectionBody(state, 'social', { canCreate, canEdit, canDelete, canPublish }, ui);
  }

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: secMeta?.label || sectionKey, backRoute: 'admin' })}
    <div data-testid="admin_section_screen_${sectionKey}">
      ${body}
    </div>
    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   8. ADMIN PARENT PORTAL MANAGEMENT — 11 SUBMENU SCREENS
   ========================================================================== */
export function renderAdminPpmModuleView(state, s, ui, moduleKey, store = null) {
  const ppmStrings = getAdminPpmStrings(state.preferences.language);
  const mod = ADMIN_PPM_MODULES.find((m) => m.key === moduleKey) || ADMIN_PPM_MODULES[0];
  const copy = ppmStrings.modules[mod.key];
  const scopeState = ui.ppmScopes?.[mod.key] || {
    scope: mod.defaultBulkScope ? 'bulk' : 'individual',
    classIdx: 0,
    studentIdx: 0
  };
  const isBulk = scopeState.scope === 'bulk';
  const targetOptions = isBulk ? ppmStrings.sampleClassOptions : ppmStrings.sampleStudentOptions;
  const activeIdx = isBulk ? scopeState.classIdx : scopeState.studentIdx;
  const activeTarget = targetOptions[activeIdx] || targetOptions[0];

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: copy.title, backRoute: 'admin' })}
    <div class="card gold-border" data-testid="ppm_screen_${mod.key}">
      <div class="row-wrap" style="align-items:flex-start;margin-bottom:10px;">
        <span class="icon-tile ${mod.useForest ? 'tone-forest' : 'tone-burgundy'}" style="width:48px;height:48px;">${icon(mod.icon, 26)}</span>
        <div style="flex:1;min-width:200px;">
          <span class="badge badge-gold" style="margin-bottom:6px;">${esc(ppmStrings.trialBadge)}</span>
          <h2 class="section-title" style="margin:0;">${esc(copy.title)}</h2>
        </div>
      </div>
      <p class="muted text-sm" style="margin:0 0 12px;">${esc(copy.intro)}</p>
      <div style="background:var(--bg-surface-variant);border-radius:10px;padding:10px 12px;display:flex;gap:8px;align-items:flex-start;">
        <span style="flex-shrink:0;margin-top:2px;">${icon('info', 18)}</span>
        <span class="text-xs">${esc(ppmStrings.trialDisclaimer)}</span>
      </div>
    </div>

    <div class="card" data-testid="ppm_scope_selector_card">
      <strong style="display:block;margin-bottom:10px;">${esc(ppmStrings.targetScopeTitle)}</strong>
      <div class="chip-row" style="margin-bottom:12px;">
        <button type="button" class="chip ${!isBulk ? 'selected' : ''}" data-action="ppm-scope" data-module="${mod.key}" data-scope="individual" data-testid="ppm_scope_individual">
          ${icon('person', 16)} ${esc(ppmStrings.targetIndividualLabel)}
        </button>
        <button type="button" class="chip ${isBulk ? 'selected-forest' : ''}" data-action="ppm-scope" data-module="${mod.key}" data-scope="bulk" data-testid="ppm_scope_bulk_class">
          ${icon('groups', 16)} ${esc(ppmStrings.targetBulkClassLabel)}
        </button>
      </div>
      <div style="border-top:1px solid var(--border-subtle);padding-top:10px;" class="chip-row">
        ${targetOptions.map((label, idx) => `
          <button type="button" class="chip ${activeIdx === idx ? 'selected' : ''}" data-action="ppm-target" data-module="${mod.key}" data-index="${idx}">
            ${esc(label)}
          </button>
        `).join('')}
      </div>
    </div>

    <div class="card" data-testid="ppm_actions_card">
      <strong style="display:block;margin-bottom:10px;">${esc(ppmStrings.actionsTitle)}</strong>
      <div class="row-wrap">
        ${mod.supportsAdd ? `
          <button type="button" class="btn btn-primary" data-action="ppm-trial-action" data-label="${esc(ppmStrings.actionAdd)}" data-module="${esc(copy.title)}" data-target="${esc(activeTarget)}" data-testid="ppm_action_add">
            ${icon('add', 18)} ${esc(ppmStrings.actionAdd)}
          </button>
        ` : ''}
        <button type="button" class="btn btn-outline" data-action="ppm-trial-action" data-label="${esc(ppmStrings.actionEdit)}" data-module="${esc(copy.title)}" data-target="${esc(activeTarget)}" data-testid="ppm_action_edit">
          ${icon('edit', 18)} ${esc(ppmStrings.actionEdit)}
        </button>
        <button type="button" class="btn btn-outline-gold" data-action="ppm-preview" data-module="${esc(copy.title)}" data-target="${esc(activeTarget)}" data-testid="ppm_action_preview">
          ${icon('visibility', 18)} ${esc(ppmStrings.actionPreview)}
        </button>
        <button type="button" class="btn btn-forest" data-action="ppm-trial-action" data-label="${esc(ppmStrings.actionPublish)}" data-module="${esc(copy.title)}" data-target="${esc(activeTarget)}" data-testid="ppm_action_publish">
          ${icon('publish', 18)} ${esc(ppmStrings.actionPublish)}
        </button>
      </div>
    </div>

    <div class="card">
      <div class="row-between" style="margin-bottom:6px;">
        <strong>${esc(copy.primaryTitle)}</strong>
        <span class="badge badge-gold">${esc(ppmStrings.samplePreviewTag)}</span>
      </div>
      <div class="text-xs" style="color:var(--brand-burgundy);font-weight:700;margin-bottom:10px;">${esc(activeTarget)}</div>
      ${copy.items.map(([heading, desc]) => `
        <div style="padding:10px 0;border-top:1px solid var(--border-subtle);">
          <strong style="display:block;font-size:0.92rem;">${esc(heading)}</strong>
          <p class="muted text-sm" style="margin:3px 0 0;">${esc(desc)}</p>
        </div>
      `).join('')}
    </div>

    <div class="card">
      <strong style="display:block;margin-bottom:6px;">${esc(copy.secondaryTitle)}</strong>
      <p class="muted text-sm" style="margin:0;">${esc(copy.secondaryDesc)}</p>
    </div>

    <button type="button" class="btn btn-outline btn-full" data-route="admin" data-testid="ppm_back_to_dashboard_btn">
      ${icon('back', 18)} ${esc(ppmStrings.backToDashboardBtn)}
    </button>

    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   9. LIVE WEB CMS EDITORS (CREATE, EDIT, DELETE, REORDER & PUBLISH)
   ========================================================================== */
function renderCmsManagedItemControls(sectionKey, item, perms) {
  const isPublished = item.isPublished !== false;
  return `
    <div class="row-wrap" style="gap:4px;">
      <span class="badge ${isPublished ? 'badge-forest' : 'badge-gold'}" data-testid="cms_status_badge_${sectionKey}_${item.id}">
        ${isPublished ? 'Published' : 'Draft'}
      </span>
      ${perms.canEdit ? `
        <button type="button" class="btn btn-outline" style="padding:4px 8px;min-height:32px;" data-action="reorder-cms-item" data-section="${esc(sectionKey)}" data-id="${esc(item.id)}" data-dir="UP" title="Move Up" data-testid="cms_up_${sectionKey}_${item.id}">↑</button>
        <button type="button" class="btn btn-outline" style="padding:4px 8px;min-height:32px;" data-action="reorder-cms-item" data-section="${esc(sectionKey)}" data-id="${esc(item.id)}" data-dir="DOWN" title="Move Down" data-testid="cms_down_${sectionKey}_${item.id}">↓</button>
      ` : ''}
      ${(perms.canPublish || perms.canEdit) ? `
        <button
          type="button"
          class="btn ${isPublished ? 'btn-outline' : 'btn-forest'}"
          style="padding:4px 10px;min-height:32px;"
          data-action="toggle-cms-publish"
          data-section="${esc(sectionKey)}"
          data-id="${esc(item.id)}"
          data-published="${isPublished ? 'false' : 'true'}"
          data-testid="cms_publish_toggle_${sectionKey}_${item.id}"
        >
          ${isPublished ? 'Unpublish' : 'Publish'}
        </button>
      ` : ''}
    </div>
  `;
}

function renderCmsSectionBody(state, section, perms = { canCreate: true, canEdit: true, canDelete: true, canPublish: true }) {
  const { canCreate, canEdit, canDelete } = perms;
  if (section === 'profile') {
    const p = state.profile;
    return `
      <form class="card" id="cms-profile-form">
        <h2 class="section-title" style="margin-bottom:14px;">School Profile &amp; Contact Editor</h2>
        <div class="form-group"><label class="form-label">Welcome Introduction</label><textarea name="welcomeIntroduction" class="form-textarea" ${canEdit ? '' : 'disabled'}>${esc(p.welcomeIntroduction)}</textarea></div>
        <div class="two-col-responsive">
          <div class="form-group"><label class="form-label">Phone Number</label><input name="phone" class="form-input" value="${esc(p.phone)}" placeholder="+91 ..." ${canEdit ? '' : 'disabled'} /></div>
          <div class="form-group"><label class="form-label">Official Email</label><input name="email" type="email" class="form-input" value="${esc(p.email)}" ${canEdit ? '' : 'disabled'} /></div>
          <div class="form-group"><label class="form-label">Website</label><input name="website" class="form-input" value="${esc(p.website)}" ${canEdit ? '' : 'disabled'} /></div>
          <div class="form-group"><label class="form-label">Office Hours</label><input name="officeHours" class="form-input" value="${esc(p.officeHours)}" placeholder="Mon–Sat, 8:30 AM – 4:00 PM" ${canEdit ? '' : 'disabled'} /></div>
        </div>
        <div class="form-group"><label class="form-label">Principal Name</label><input name="principalName" class="form-input" value="${esc(p.principalName)}" ${canEdit ? '' : 'disabled'} /></div>
        <div class="form-group"><label class="form-label">School History</label><textarea name="history" class="form-textarea" ${canEdit ? '' : 'disabled'}>${esc(p.history)}</textarea></div>
        <div class="form-group"><label class="form-label">Vision</label><textarea name="vision" class="form-textarea" ${canEdit ? '' : 'disabled'}>${esc(p.vision)}</textarea></div>
        <div class="form-group"><label class="form-label">Mission</label><textarea name="mission" class="form-textarea" ${canEdit ? '' : 'disabled'}>${esc(p.mission)}</textarea></div>
        ${canEdit ? `<button type="submit" class="btn btn-primary">${icon('publish', 18)} Save &amp; Publish Profile</button>` : `<span class="badge badge-gold">Read-Only View</span>`}
      </form>
    `;
  }
  if (section === 'academics') {
    return `
      ${canCreate ? `
        <form class="card" id="cms-academic-form">
          <h2 class="section-title" style="margin-bottom:12px;">Add Academic Program</h2>
          <div class="two-col-responsive">
            <div class="form-group"><label class="form-label">Title</label><input name="title" class="form-input" required placeholder="e.g. Primary & Middle School" /></div>
            <div class="form-group"><label class="form-label">Level</label><input name="level" class="form-input" placeholder="Grades I – VIII" /></div>
          </div>
          <div class="form-group"><label class="form-label">Description</label><textarea name="description" class="form-textarea" required></textarea></div>
          <button type="submit" class="btn btn-primary">${icon('add', 18)} Add Program</button>
        </form>
      ` : ''}
      ${state.academics.length > 0 ? `
        <div class="card" data-testid="cms_academics_list">
          <h3 class="section-title" style="margin-bottom:10px;">Managed Academic Programs (${state.academics.length})</h3>
          ${state.academics.map((a) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="cms_academic_row_${a.id}">
              <div>
                <strong>${esc(a.title)}</strong>
                <div class="muted text-xs">${esc(a.level || a.category || 'Academic')} · ${esc(a.description)}</div>
              </div>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('ACADEMICS', a, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-academic" data-id="${a.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
      <div class="card">
        <h3 class="section-title" style="margin-bottom:12px;">4 Higher Secondary Course Streams</h3>
        <div class="stack-md">
          ${state.courses.map((c) => `
            <form class="cms-course-form" data-id="${c.id}" style="padding:12px;border:1px solid var(--border-subtle);border-radius:12px;">
              <strong style="display:block;margin-bottom:8px;">${esc(c.groupCode)}</strong>
              <div class="two-col-responsive">
                <div class="form-group"><label class="form-label">Stream Title</label><input name="title" class="form-input" value="${esc(c.title)}" required ${canEdit ? '' : 'disabled'} /></div>
                <div class="form-group"><label class="form-label">Medium</label><input name="medium" class="form-input" value="${esc(c.medium)}" placeholder="English Medium" ${canEdit ? '' : 'disabled'} /></div>
              </div>
              <div class="form-group"><label class="form-label">Subjects</label><input name="subjects" class="form-input" value="${esc(c.subjects)}" placeholder="Physics, Chemistry, Mathematics, Computer Science" ${canEdit ? '' : 'disabled'} /></div>
              ${canEdit ? `<button type="submit" class="btn btn-forest">${icon('publish', 18)} Save ${esc(c.groupCode)}</button>` : ''}
            </form>
          `).join('')}
        </div>
      </div>
    `;
  }
  if (section === 'updates') {
    return `
      ${canCreate ? `
        <div class="two-col-responsive">
          <form class="card" id="cms-announcement-form">
            <h2 class="section-title" style="margin-bottom:12px;">Publish Announcement</h2>
            <div class="form-group"><label class="form-label">Title</label><input name="title" class="form-input" required /></div>
            <div class="form-group"><label class="form-label">Priority Label</label><input name="priorityLabel" class="form-input" placeholder="Official Notice / Circular" /></div>
            <div class="form-group"><label class="form-label">Message</label><textarea name="message" class="form-textarea" required></textarea></div>
            <button type="submit" class="btn btn-primary">${icon('publish', 18)} Publish Notice</button>
          </form>
          <form class="card" id="cms-news-form">
            <h2 class="section-title" style="margin-bottom:12px;">Publish School News</h2>
            <div class="form-group"><label class="form-label">Headline</label><input name="title" class="form-input" required /></div>
            <div class="form-group"><label class="form-label">Summary</label><textarea name="summary" class="form-textarea" required></textarea></div>
            <div class="form-group"><label class="form-label">Optional Image URL</label><input name="imageUrl" class="form-input" placeholder="https://..." /></div>
            <button type="submit" class="btn btn-forest">${icon('publish', 18)} Publish News</button>
          </form>
        </div>
      ` : ''}
      ${state.announcements.length || state.news.length ? `
        <div class="card">
          <h3 style="margin:0 0 10px;">Managed News &amp; Announcements</h3>
          ${state.announcements.map((a) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[Notice]</strong> ${esc(a.title)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('ANNOUNCEMENTS', a, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-announcement" data-id="${a.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
          ${state.news.map((n) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[News]</strong> ${esc(n.title)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('NEWS', n, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-news" data-id="${n.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
    `;
  }
  if (section === 'media') {
    return `
      ${canCreate ? `
        <div class="two-col-responsive">
          <form class="card" id="cms-hero-form">
            <h2 class="section-title" style="margin-bottom:12px;">Add Hero Banner Slide</h2>
            <div class="form-group"><label class="form-label">Image URL</label><input name="imageUrl" class="form-input" placeholder="https://... or upload below" /></div>
            <div class="form-group"><label class="form-label">Or Upload Local Image</label><input name="imageFile" type="file" accept="image/*" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Caption</label><input name="caption" class="form-input" placeholder="Campus view" /></div>
            <button type="submit" class="btn btn-primary">${icon('add', 18)} Add Hero Banner</button>
          </form>
          <form class="card" id="cms-gallery-form">
            <h2 class="section-title" style="margin-bottom:12px;">Add Gallery Photo / Video</h2>
            <div class="form-group"><label class="form-label">Image / Thumbnail URL</label><input name="imageUrl" class="form-input" placeholder="https://... or upload below" /></div>
            <div class="form-group"><label class="form-label">Or Upload Local Image</label><input name="imageFile" type="file" accept="image/*" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Album Name</label><input name="album" class="form-input" placeholder="Annual Day / Campus" /></div>
            <div class="form-group"><label class="form-label">Category</label><input name="category" class="form-input" value="Campus" /></div>
            <div class="form-group"><label class="form-label">Caption</label><input name="caption" class="form-input" /></div>
            <div class="form-group"><label class="form-label">Optional YouTube Video URL</label><input name="youtubeUrl" class="form-input" placeholder="https://youtube.com/watch?v=..." /></div>
            <button type="submit" class="btn btn-forest">${icon('add', 18)} Add to Gallery</button>
          </form>
        </div>
      ` : '<div class="card"><span class="badge badge-gold">Read-Only Media View</span></div>'}
      ${(state.heroPhotos.length || state.galleryMedia.length) ? `
        <div class="card">
          <h3 style="margin:0 0 10px;">Managed Hero Slides &amp; Gallery Media</h3>
          ${state.heroPhotos.map((h) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[Hero]</strong> ${esc(h.caption || h.imageUrl)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('LOGO_BANNER', h, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-hero" data-id="${h.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
          ${state.galleryMedia.map((g) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[Gallery]</strong> ${esc(g.caption || g.album || g.imageUrl)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('PHOTO_GALLERY', g, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-gallery" data-id="${g.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
    `;
  }
  if (section === 'campus') {
    return `
      ${canCreate ? `
        <div class="two-col-responsive">
          <form class="card" id="cms-facility-form">
            <h2 class="section-title" style="margin-bottom:12px;">Add Campus Facility</h2>
            <div class="form-group"><label class="form-label">Facility Name</label><input name="name" class="form-input" required placeholder="Science Laboratory" /></div>
            <div class="form-group"><label class="form-label">Category</label><input name="category" class="form-input" value="Academics" /></div>
            <div class="form-group"><label class="form-label">Description</label><textarea name="description" class="form-textarea" required></textarea></div>
            <button type="submit" class="btn btn-primary">${icon('add', 18)} Add Facility</button>
          </form>
          <form class="card" id="cms-transport-form">
            <h2 class="section-title" style="margin-bottom:12px;">Add Bus Route</h2>
            <div class="form-group"><label class="form-label">Route Number</label><input name="routeNumber" class="form-input" required placeholder="01" /></div>
            <div class="form-group"><label class="form-label">Route Title</label><input name="routeName" class="form-input" required placeholder="Dindigul Bus Stand – Campus" /></div>
            <div class="form-group"><label class="form-label">Stops</label><textarea name="stops" class="form-textarea" required></textarea></div>
            <div class="form-group"><label class="form-label">Timings</label><input name="timings" class="form-input" placeholder="7:45 AM – 8:30 AM" /></div>
            <button type="submit" class="btn btn-forest">${icon('add', 18)} Add Route</button>
          </form>
        </div>
      ` : '<div class="card"><span class="badge badge-gold">Read-Only Campus View</span></div>'}
      ${(state.facilities.length || state.transportRoutes.length) ? `
        <div class="card">
          <h3 style="margin:0 0 10px;">Managed Facilities &amp; Bus Routes</h3>
          ${state.facilities.map((f) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[Facility]</strong> ${esc(f.name)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('FACILITIES', f, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-facility" data-id="${f.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
          ${state.transportRoutes.map((r) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;">
              <span><strong>[Route ${esc(r.routeNumber)}]</strong> ${esc(r.routeName)}</span>
              <div class="row-wrap" style="gap:6px;">
                ${renderCmsManagedItemControls('TRANSPORT', r, perms)}
                ${canDelete ? `<button type="button" class="icon-btn" data-action="delete-transport" data-id="${r.id}" aria-label="Delete">${icon('delete', 18)}</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      ` : ''}
    `;
  }
  if (section === 'social') {
    return `
      <div class="card">
        <h2 class="section-title" style="margin-bottom:12px;">Official Social Channels</h2>
        <div class="stack-md">
          ${state.socialLinks.map((link) => `
            <form class="cms-social-form" data-id="${link.id}" style="padding:12px;border:1px solid var(--border-subtle);border-radius:12px;">
              <strong style="display:block;margin-bottom:8px;">${esc(link.platformName)}</strong>
              <div class="two-col-responsive">
                <div class="form-group"><label class="form-label">Handle / Label</label><input name="handle" class="form-input" value="${esc(link.handle)}" placeholder="@chevalieracademy" ${canEdit ? '' : 'disabled'} /></div>
                <div class="form-group"><label class="form-label">Official URL</label><input name="url" class="form-input" value="${esc(link.url)}" placeholder="https://..." ${canEdit ? '' : 'disabled'} /></div>
              </div>
              ${canEdit ? `<button type="submit" class="btn btn-primary">${icon('publish', 18)} Save Channel</button>` : ''}
            </form>
          `).join('')}
        </div>
      </div>
    `;
  }
  const logs = state.admin.activityLog || [];
  return `
    <div class="card">
      <h2 class="section-title" style="margin-bottom:8px;">JSON Backup &amp; Restore</h2>
      <p class="muted text-sm" style="margin:0 0 14px;">Export verified school records as JSON or restore a previous backup file.</p>
      <div class="row-wrap">
        <button type="button" class="btn btn-primary" data-action="export-backup">${icon('download', 18)} Export Backup JSON</button>
        ${canEdit ? `
          <label class="btn btn-outline" style="cursor:pointer;">
            ${icon('upload', 18)} Restore Backup JSON
            <input type="file" id="import-backup-input" accept="application/json" style="display:none;" />
          </label>
        ` : ''}
      </div>
    </div>
    <div class="card">
      <h3 style="margin:0 0 10px;">Recent Activity Log (${logs.length})</h3>
      ${logs.length === 0 ? '<p class="muted text-sm">No activity recorded yet.</p>' : logs.slice(0, 15).map((l) => `
        <div style="padding:8px 0;border-bottom:1px solid var(--border-subtle);">
          <div class="row-between"><strong class="text-sm">${esc(l.section)} · ${esc(l.action)}</strong><span class="muted text-xs">${esc(new Date(l.timestamp).toLocaleString())}</span></div>
          <div class="muted text-xs">${esc(l.details)}</div>
        </div>
      `).join('')}
    </div>
  `;
}

export function renderAdminCmsView(state, s, section, store = null, ui = {}) {
  const sectionKeyMap = {
    profile: 'SCHOOL_PROFILE',
    academics: 'ACADEMICS',
    updates: 'NEWS',
    media: 'PHOTO_GALLERY',
    campus: 'FACILITIES',
    social: 'SOCIAL_MEDIA',
    backup: 'BACKUP_RESTORE'
  };
  const secKey = sectionKeyMap[section] || 'SCHOOL_PROFILE';
  if (store && !canSessionPerm(store, secKey, PERMISSION_ACTIONS.VIEW)) {
    return renderAdminAccessDeniedView(store, state, s, ui, MANAGEMENT_SECTION_MAP[secKey]?.label || section);
  }
  const perms = {
    canCreate: canSessionPerm(store, secKey, PERMISSION_ACTIONS.CREATE),
    canEdit: canSessionPerm(store, secKey, PERMISSION_ACTIONS.EDIT),
    canDelete: canSessionPerm(store, secKey, PERMISSION_ACTIONS.DELETE),
    canPublish: canSessionPerm(store, secKey, PERMISSION_ACTIONS.PUBLISH)
  };
  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: MANAGEMENT_SECTION_MAP[secKey]?.label || 'CMS Editor', backRoute: 'admin' })}
    ${renderCmsSectionBody(state, section, perms, ui)}
    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   10. BASE 4 — ACADEMICS V2 13-CATEGORY HUB & CATEGORY EDITOR SCREENS
   Mirrors AdminV2AcademicsHubScreen & AdminV2CategoryManagementScreen
   ========================================================================== */
export function renderAdminV2AcademicsHubView(store, state, s, ui = {}) {
  const v2Records = (state.academicsV2 && state.academicsV2.records) || [];
  const isOwner = store ? store.isSchoolOwner() : true;

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Academics V2 Management (13 Categories)', backRoute: 'admin' })}
    <div class="card gold-border" data-testid="admin_v2_academics_hub">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div>
          <span class="badge badge-gold">Draft-by-Default · 13 Categories</span>
          <h2 class="section-title" style="margin:4px 0 2px;">Academics V2 Content Management</h2>
          <p class="muted text-sm" style="margin:0;">
            Select any of the 13 Academy categories to draft, preview, submit for approval, or publish sub-section content.
          </p>
        </div>
        <div class="row-wrap" style="gap:8px;">
          <button type="button" class="btn btn-forest" data-route="admin/preview-publish" data-testid="v2_hub_open_preview_publish_btn">
            ${icon('publish', 16)} Preview &amp; Publish Hub
          </button>
          ${isOwner ? `
            <button type="button" class="btn btn-primary" data-route="admin/owner-control-center" data-testid="v2_hub_open_owner_center_btn">
              ${icon('security', 16)} Owner Control Center
            </button>
          ` : ''}
        </div>
      </div>
    </div>

    <div class="two-col-responsive" data-testid="admin_v2_categories_grid">
      ${ACADEMICS_V2_CATEGORIES.map((cat) => {
        const canView = store ? store.hasV2CategoryPermission(cat.routeId, PERMISSION_ACTIONS.VIEW) : true;
        const catRecords = v2Records.filter((r) => r.categoryKey === cat.routeId);
        const publishedCount = catRecords.filter((r) => r.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED).length;
        const draftCount = catRecords.filter((r) => r.status !== ACADEMICS_V2_RECORD_STATUS.PUBLISHED).length;
        return `
          <button
            type="button"
            class="card interactive"
            style="text-align:left;opacity:${canView ? '1' : '0.65'};"
            data-route="admin/v2-academics/${esc(cat.routeId)}"
            data-testid="admin_v2_academics_cat_${esc(cat.routeId)}"
          >
            <div class="row-between" style="margin-bottom:8px;">
              <span class="icon-tile tone-${esc(cat.tone)}">${icon(cat.icon, 22)}</span>
              <div class="row-wrap" style="gap:6px;">
                <span class="badge badge-forest">${publishedCount} Published</span>
                ${draftCount > 0 ? `<span class="badge badge-gold">${draftCount} Draft/Pending</span>` : ''}
              </div>
            </div>
            <strong style="display:block;font-size:1rem;">${esc(cat.title)}</strong>
            <p class="muted text-xs" style="margin:4px 0 0;">${esc(cat.subtitle)} · ${cat.subSections.length} sub-sections</p>
          </button>
        `;
      }).join('')}
    </div>

    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

export function renderAdminV2CategoryManagementView(store, state, s, ui = {}, categoryRouteId = 'academics') {
  const category = ACADEMICS_V2_CATEGORIES.find((c) => c.routeId === categoryRouteId) || ACADEMICS_V2_CATEGORIES[0];
  if (store && !store.hasV2CategoryPermission(category.routeId, PERMISSION_ACTIONS.VIEW)) {
    return renderAdminAccessDeniedView(store, state, s, ui, category.title);
  }

  const activeSectionKey = ui.v2SelectedSectionKey && category.subSections.some((sub) => sub.id === ui.v2SelectedSectionKey)
    ? ui.v2SelectedSectionKey
    : category.subSections[0].id;
  const activeSubSection = category.subSections.find((sub) => sub.id === activeSectionKey) || category.subSections[0];

  const canCreate = store ? store.hasV2CategoryPermission(category.routeId, PERMISSION_ACTIONS.CREATE) : true;
  const canEdit = store ? store.hasV2CategoryPermission(category.routeId, PERMISSION_ACTIONS.EDIT) : true;
  const canDelete = store ? store.hasV2CategoryPermission(category.routeId, PERMISSION_ACTIONS.DELETE) : true;
  const canPublishDirect = store ? store.canPublishV2Directly(category.routeId, activeSectionKey) : true;
  const isOwner = store ? store.isSchoolOwner() : true;
  const isExamResultsSection = category.routeId === 'examinations' && activeSectionKey === 'exam_results';

  const allCategoryRecords = ((state.academicsV2 && state.academicsV2.records) || []).filter(
    (r) => r.categoryKey === category.routeId && r.sectionKey === activeSectionKey
  );
  const editingRecord = ui.v2EditingRecordId
    ? allCategoryRecords.find((r) => r.id === ui.v2EditingRecordId)
    : null;
  const previewRecord = ui.v2PreviewRecordId
    ? ((state.academicsV2 && state.academicsV2.records) || []).find((r) => r.id === ui.v2PreviewRecordId)
    : null;

  // Card 07 Dedicated Photo-Only Gallery Manager
  const v2Albums = (state.academicsV2 && state.academicsV2.galleryAlbums) || [];
  const v2Photos = (state.academicsV2 && state.academicsV2.galleryPhotos) || [];

  // Card 11 Dedicated 8-Field Faculty & Staff Manager
  const v2Faculty = (state.academicsV2 && state.academicsV2.facultyProfiles) || [];

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: `${category.title} — V2 Manager`, backRoute: 'admin/v2-academics' })}

    <div class="card gold-border" data-testid="admin_v2_category_screen_${esc(category.routeId)}">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div>
          <span class="badge badge-gold">Category: ${esc(category.title)}</span>
          <h2 class="section-title" style="margin:4px 0 2px;">${esc(activeSubSection.title)}</h2>
          <p class="muted text-xs" style="margin:0;">
            All saves default to DRAFT. Use Preview to verify public appearance before publishing or submitting for Owner approval.
          </p>
        </div>
        <div class="row-wrap" style="gap:8px;">
          <button type="button" class="btn btn-outline" data-route="academics/category/${esc(category.routeId)}" data-testid="admin_v2_view_public_category_btn">
            ${icon('visibility', 16)} View Public Page
          </button>
          ${(canCreate || canEdit) ? `
            <button type="button" class="btn btn-primary" data-action="v2-new-draft" data-category="${esc(category.routeId)}" data-section="${esc(activeSectionKey)}" data-testid="admin_v2_create_record_btn">
              ${icon('add', 16)} + New Draft
            </button>
          ` : ''}
        </div>
      </div>

      <div class="chip-row" style="margin-top:12px;" data-testid="admin_v2_subsection_chips">
        ${category.subSections.map((sub) => `
          <button
            type="button"
            class="chip ${sub.id === activeSectionKey ? 'selected' : ''}"
            data-action="v2-select-section"
            data-section="${esc(sub.id)}"
            data-testid="admin_v2_section_chip_${esc(sub.id)}"
          >
            ${esc(sub.title)}
          </button>
        `).join('')}
      </div>
    </div>

    ${isExamResultsSection ? `
      <div class="card burgundy-border" data-testid="admin_v2_exam_results_owner_gate_banner">
        <span class="badge badge-burgundy" style="margin-bottom:4px;">School Owner Authorization Required</span>
        <p class="text-xs" style="margin:0;">
          Exam Results cannot be published directly by Staff. Non-owner edits are saved as Draft or Pending Approval and require School Owner authorization to go live.
        </p>
      </div>
    ` : ''}

    ${(canCreate || canEdit) ? `
      <form id="admin-v2-record-form" class="card gold-border" data-category="${esc(category.routeId)}" data-section="${esc(activeSectionKey)}" data-record-id="${editingRecord ? esc(editingRecord.id) : ''}" data-testid="admin_v2_editor_card">
        <h3 class="section-title" style="margin:0 0 10px;">
          ${editingRecord ? `Edit Record (Saves as Draft): ${esc(editingRecord.title)}` : `Create New Draft in "${esc(activeSubSection.title)}"`}
        </h3>
        <div class="two-col-responsive">
          <div class="form-group">
            <label class="form-label">Record Title</label>
            <input name="title" class="form-input" required value="${esc(editingRecord?.title || '')}" placeholder="Official title" data-testid="admin_v2_input_title" />
          </div>
          <div class="form-group">
            <label class="form-label">Badge / Academic Year / Subtitle</label>
            <input name="badgeText" class="form-input" value="${esc(editingRecord?.badgeText || editingRecord?.subtitle || '')}" placeholder="e.g. 2026–2027 / Verified" data-testid="admin_v2_input_badge" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Summary / Short Note</label>
          <input name="summary" class="form-input" value="${esc(editingRecord?.summary || '')}" placeholder="Brief summary" data-testid="admin_v2_input_summary" />
        </div>
        <div class="form-group">
          <label class="form-label">Detailed Content Body</label>
          <textarea name="body" class="form-textarea" required placeholder="Full verified information..." data-testid="admin_v2_input_body">${esc(editingRecord?.body || '')}</textarea>
        </div>
        <div class="form-group">
          <label class="form-label">Optional Image URL (Photo-only for Gallery)</label>
          <input name="imageUri" class="form-input" value="${esc(editingRecord?.imageUri || '')}" placeholder="https://..." data-testid="admin_v2_input_image" />
        </div>
        <div class="row-wrap" style="gap:8px;">
          <button type="submit" name="submitIntent" value="draft" class="btn btn-primary" data-testid="admin_v2_save_draft_btn">
            ${icon('edit', 16)} Save as Draft
          </button>
          <button type="button" class="btn btn-outline-gold" data-action="v2-submit-form-approval" data-testid="admin_v2_submit_approval_btn">
            ${icon('factCheck', 16)} Save &amp; Submit for Approval
          </button>
        </div>
      </form>
    ` : ''}

    ${previewRecord ? `
      <div class="card gold-border" data-testid="admin_v2_public_preview_modal">
        <div class="row-between" style="margin-bottom:8px;">
          <span class="badge badge-forest">Public Card Preview · ${esc(previewRecord.status)}</span>
          <button type="button" class="icon-btn" data-action="v2-close-preview" aria-label="Close Preview">${icon('close', 18)}</button>
        </div>
        <h3 style="margin:0 0 4px;">${esc(previewRecord.title)}</h3>
        ${previewRecord.badgeText ? `<span class="badge badge-burgundy-soft" style="margin-bottom:8px;">${esc(previewRecord.badgeText)}</span>` : ''}
        ${previewRecord.imageUri ? `<img src="${esc(previewRecord.imageUri)}" alt="${esc(previewRecord.title)}" style="width:100%;max-height:200px;object-fit:cover;border-radius:10px;margin:8px 0;" />` : ''}
        <p class="muted text-sm" style="margin:0;white-space:pre-line;">${esc(previewRecord.body || previewRecord.summary)}</p>
      </div>
    ` : ''}

    <div class="card" data-testid="admin_v2_records_list">
      <h3 class="section-title" style="margin:0 0 10px;">Records in "${esc(activeSubSection.title)}" (${allCategoryRecords.length})</h3>
      ${allCategoryRecords.length === 0 ? `<p class="muted text-sm" style="margin:0;">No records created in this sub-section yet.</p>` : `
        <div class="stack-sm">
          ${allCategoryRecords.map((rec) => `
            <div class="card" style="padding:12px;" data-testid="admin_v2_record_card_${esc(rec.id)}">
              <div class="row-between" style="margin-bottom:4px;flex-wrap:wrap;gap:8px;">
                <div class="row-wrap" style="gap:6px;">
                  <span class="badge ${rec.status === 'PUBLISHED' ? 'badge-forest' : rec.status === 'PENDING_APPROVAL' ? 'badge-burgundy' : 'badge-gold'}" data-testid="admin_v2_record_status_${esc(rec.id)}">
                    ${esc(rec.status)}
                  </span>
                  ${rec.badgeText ? `<span class="badge badge-gold">${esc(rec.badgeText)}</span>` : ''}
                  ${rec.isExamResultAuthorized ? `<span class="badge badge-forest">Owner-Authorized Exam Result</span>` : ''}
                </div>
                <span class="muted text-xs">Updated by ${esc(rec.updatedByName)}</span>
              </div>
              <strong style="display:block;">${esc(rec.title)}</strong>
              <p class="muted text-xs" style="margin:4px 0 10px;">${esc(rec.body || rec.summary)}</p>
              <div class="row-wrap" style="gap:6px;">
                <button type="button" class="btn btn-outline" data-action="v2-preview-record" data-id="${esc(rec.id)}" data-testid="admin_v2_preview_btn_${esc(rec.id)}">
                  ${icon('visibility', 15)} Preview
                </button>
                ${canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-edit-record" data-id="${esc(rec.id)}" data-testid="admin_v2_edit_btn_${esc(rec.id)}">
                    ${icon('edit', 15)} Edit
                  </button>
                ` : ''}
                ${rec.status !== 'PUBLISHED' && canPublishDirect && !isExamResultsSection ? `
                  <button type="button" class="btn btn-forest" data-action="v2-publish-record" data-id="${esc(rec.id)}" data-testid="admin_v2_publish_btn_${esc(rec.id)}">
                    ${icon('publish', 15)} Publish
                  </button>
                ` : ''}
                ${rec.status !== 'PUBLISHED' && isExamResultsSection && isOwner ? `
                  <button type="button" class="btn btn-forest" data-action="v2-owner-approve-publish" data-id="${esc(rec.id)}" data-exam="true" data-testid="admin_v2_owner_publish_${esc(rec.id)}">
                    ${icon('verified', 15)} Owner Authorize &amp; Publish Exam Result
                  </button>
                ` : ''}
                ${rec.status === 'DRAFT' && (!canPublishDirect || isExamResultsSection) ? `
                  <button type="button" class="btn btn-outline-gold" data-action="v2-submit-record-approval" data-id="${esc(rec.id)}" data-testid="admin_v2_request_approval_btn_${esc(rec.id)}">
                    Submit for Owner Approval
                  </button>
                ` : ''}
                ${rec.status === 'PUBLISHED' && canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-set-record-status" data-id="${esc(rec.id)}" data-status="UNPUBLISHED" data-testid="admin_v2_unpublish_btn_${esc(rec.id)}">
                    Unpublish
                  </button>
                ` : ''}
                ${rec.status !== 'ARCHIVED' && canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-set-record-status" data-id="${esc(rec.id)}" data-status="ARCHIVED" data-testid="admin_v2_archive_btn_${esc(rec.id)}">
                    Archive
                  </button>
                ` : ''}
                ${canDelete ? `
                  <button type="button" class="btn btn-danger" data-action="v2-delete-record" data-id="${esc(rec.id)}" data-testid="admin_v2_delete_btn_${esc(rec.id)}">
                    ${icon('delete', 15)} Delete
                  </button>
                ` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      `}
    </div>

    ${category.routeId === 'gallery' ? `
      <!-- CARD 07: DEDICATED PHOTO-ONLY GALLERY MANAGER WITH MULTI-ALBUM REUSE -->
      <section class="stack-md" data-testid="admin_v2_gallery_manager">
        <div class="card gold-border">
          <span class="badge badge-burgundy">Card 07 · Photo-Only Gallery Rule</span>
          <h3 class="section-title" style="margin:4px 0;">Photo Albums &amp; Multi-Album Photo Reuse</h3>
          <p class="muted text-xs" style="margin:0;">
            Strictly photo-only (YouTube &amp; video URLs are rejected). A single uploaded photo can be assigned to multiple albums simultaneously.
          </p>
        </div>

        ${(canCreate || canEdit) ? `
          <div class="two-col-responsive">
            <form id="admin-v2-album-form" class="card" data-testid="admin_v2_album_form">
              <h4 style="margin:0 0 10px;">Create Photo Album (Saves as Draft)</h4>
              <div class="form-group">
                <label class="form-label">Album Title</label>
                <input name="title" class="form-input" required placeholder="e.g. Science Exhibition 2026" data-testid="v2_album_title_input" />
              </div>
              <div class="form-group">
                <label class="form-label">Description</label>
                <input name="description" class="form-input" placeholder="Album description" data-testid="v2_album_desc_input" />
              </div>
              <div class="form-group">
                <label class="form-label">Optional Cover Photo URL</label>
                <input name="coverImageUri" class="form-input" placeholder="https://...jpg" data-testid="v2_album_cover_input" />
              </div>
              <button type="submit" class="btn btn-primary" data-testid="v2_album_submit_btn">${icon('add', 16)} Save Draft Album</button>
            </form>

            <form id="admin-v2-photo-form" class="card" data-testid="admin_v2_photo_form">
              <h4 style="margin:0 0 10px;">Upload Photo &amp; Assign to Multiple Albums</h4>
              <div class="form-group">
                <label class="form-label">Photo Title</label>
                <input name="title" class="form-input" required placeholder="e.g. Robotics Demo" data-testid="v2_photo_title_input" />
              </div>
              <div class="form-group">
                <label class="form-label">Photo Image URI (Photo Only — No YouTube/Video)</label>
                <input name="imageUri" class="form-input" required placeholder="https://.../photo.jpg" data-testid="v2_photo_uri_input" />
              </div>
              <div class="form-group">
                <label class="form-label">Caption</label>
                <input name="caption" class="form-input" placeholder="Optional caption" data-testid="v2_photo_caption_input" />
              </div>
              <div class="form-group">
                <label class="form-label">Assign to Album(s) (Multi-Album Reuse)</label>
                <div class="chip-row" data-testid="v2_photo_album_checkboxes">
                  ${v2Albums.length === 0 ? '<span class="muted text-xs">Create an album first</span>' : v2Albums.map((alb) => `
                    <label style="display:inline-flex;align-items:center;gap:6px;font-size:0.82rem;">
                      <input type="checkbox" name="albumIds" value="${esc(alb.id)}" data-testid="v2_photo_album_check_${esc(alb.id)}" />
                      <span>${esc(alb.title)}</span>
                    </label>
                  `).join('')}
                </div>
              </div>
              <button type="submit" class="btn btn-forest" data-testid="v2_photo_submit_btn">${icon('add', 16)} Save Draft Photo</button>
            </form>
          </div>
        ` : ''}

        <div class="card" data-testid="admin_v2_albums_list">
          <h4 style="margin:0 0 10px;">Managed V2 Photo Albums (${v2Albums.length})</h4>
          ${v2Albums.length === 0 ? '<p class="muted text-xs">No V2 photo albums yet.</p>' : v2Albums.map((alb) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="v2_album_row_${esc(alb.id)}">
              <div>
                <span class="badge ${alb.status === 'PUBLISHED' ? 'badge-forest' : 'badge-gold'}">${esc(alb.status)}</span>
                <strong style="margin-left:6px;">${esc(alb.title)}</strong>
              </div>
              <div class="row-wrap" style="gap:6px;">
                ${alb.status !== 'PUBLISHED' && canPublishDirect ? `
                  <button type="button" class="btn btn-forest" data-action="v2-album-status" data-id="${esc(alb.id)}" data-status="PUBLISHED" data-testid="v2_album_publish_${esc(alb.id)}">Publish Album</button>
                ` : ''}
                ${alb.status === 'PUBLISHED' && canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-album-status" data-id="${esc(alb.id)}" data-status="UNPUBLISHED" data-testid="v2_album_unpublish_${esc(alb.id)}">Unpublish</button>
                ` : ''}
                ${canDelete ? `
                  <button type="button" class="btn btn-danger" data-action="v2-album-delete" data-id="${esc(alb.id)}" data-testid="v2_album_delete_${esc(alb.id)}">Delete</button>
                ` : ''}
              </div>
            </div>
          `).join('')}
        </div>

        <div class="card" data-testid="admin_v2_photos_list">
          <h4 style="margin:0 0 10px;">Managed V2 Gallery Photos (${v2Photos.length})</h4>
          ${v2Photos.length === 0 ? '<p class="muted text-xs">No V2 photos uploaded yet.</p>' : v2Photos.map((p) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="v2_photo_row_${esc(p.id)}">
              <div>
                <span class="badge ${p.status === 'PUBLISHED' ? 'badge-forest' : 'badge-gold'}">${esc(p.status)}</span>
                <strong style="margin-left:6px;">${esc(p.title || p.caption)}</strong>
                <span class="muted text-xs"> · Assigned to ${(p.albumIds || []).length} album(s)</span>
              </div>
              <div class="row-wrap" style="gap:6px;">
                ${p.status !== 'PUBLISHED' && canPublishDirect ? `
                  <button type="button" class="btn btn-forest" data-action="v2-photo-status" data-id="${esc(p.id)}" data-status="PUBLISHED" data-testid="v2_photo_publish_${esc(p.id)}">Publish Photo</button>
                ` : ''}
                ${p.status === 'PUBLISHED' && canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-photo-status" data-id="${esc(p.id)}" data-status="UNPUBLISHED" data-testid="v2_photo_unpublish_${esc(p.id)}">Unpublish</button>
                ` : ''}
                ${canDelete ? `
                  <button type="button" class="btn btn-danger" data-action="v2-photo-delete" data-id="${esc(p.id)}" data-testid="v2_photo_delete_${esc(p.id)}">Delete</button>
                ` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      </section>
    ` : ''}

    ${category.routeId === 'faculty_staff' ? `
      <!-- CARD 11: PRIVACY-GUARDED 8-FIELD FACULTY & STAFF DIRECTORY MANAGER -->
      <section class="stack-md" data-testid="admin_v2_faculty_manager">
        <div class="card gold-border">
          <span class="badge badge-burgundy">Card 11 · 8 Public Fields + Dual Consent Gate</span>
          <h3 class="section-title" style="margin:4px 0;">Faculty &amp; Staff Public Profile Manager</h3>
          <p class="muted text-xs" style="margin:0;">
            Strictly limited to 8 professional public fields. Both School Approval and Staff Member Consent must be verified before a profile can be published.
          </p>
        </div>

        ${(canCreate || canEdit) ? `
          <form id="admin-v2-faculty-form" class="card" data-testid="admin_v2_faculty_form">
            <h4 style="margin:0 0 10px;">Add / Save Faculty Profile (8 Public Fields Only)</h4>
            <div class="two-col-responsive">
              <div class="form-group">
                <label class="form-label">1. Teacher / Staff Name *</label>
                <input name="teacherName" class="form-input" required placeholder="e.g. Dr. S. Arulmozhi" data-testid="v2_faculty_name_input" />
              </div>
              <div class="form-group">
                <label class="form-label">2. Approved Profile Photo URL</label>
                <input name="approvedProfilePhotoUri" class="form-input" placeholder="https://.../photo.jpg" data-testid="v2_faculty_photo_input" />
              </div>
              <div class="form-group">
                <label class="form-label">3. Designation *</label>
                <input name="designation" class="form-input" required placeholder="e.g. PGT Mathematics" data-testid="v2_faculty_designation_input" />
              </div>
              <div class="form-group">
                <label class="form-label">4. Department *</label>
                <input name="department" class="form-input" required placeholder="e.g. Department of Mathematics" data-testid="v2_faculty_dept_input" />
              </div>
              <div class="form-group">
                <label class="form-label">5. Teaching Subjects</label>
                <input name="teachingSubjects" class="form-input" placeholder="e.g. Higher Secondary Mathematics" data-testid="v2_faculty_subjects_input" />
              </div>
              <div class="form-group">
                <label class="form-label">6. Educational Qualifications</label>
                <input name="educationalQualifications" class="form-input" placeholder="e.g. M.Sc., B.Ed., M.Phil." data-testid="v2_faculty_qualifications_input" />
              </div>
              <div class="form-group">
                <label class="form-label">7. Teaching Experience</label>
                <input name="teachingExperience" class="form-input" placeholder="e.g. 14 Years" data-testid="v2_faculty_experience_input" />
              </div>
              <div class="form-group">
                <label class="form-label">8. Short Professional Introduction</label>
                <input name="professionalIntroduction" class="form-input" placeholder="Brief academic introduction" data-testid="v2_faculty_intro_input" />
              </div>
            </div>
            <div class="row-wrap" style="margin:8px 0 12px;gap:14px;">
              <label style="display:inline-flex;align-items:center;gap:6px;">
                <input type="checkbox" name="isSchoolApproved" data-testid="v2_faculty_school_approved_check" />
                <strong>School Administration Approved</strong>
              </label>
              <label style="display:inline-flex;align-items:center;gap:6px;">
                <input type="checkbox" name="hasStaffConsent" data-testid="v2_faculty_staff_consent_check" />
                <strong>Staff Member Consent Verified</strong>
              </label>
            </div>
            <button type="submit" class="btn btn-primary" data-testid="v2_faculty_submit_btn">${icon('add', 16)} Save Faculty Profile (Draft)</button>
          </form>
        ` : ''}

        <div class="card" data-testid="admin_v2_faculty_list">
          <h4 style="margin:0 0 10px;">Managed Faculty Profiles (${v2Faculty.length})</h4>
          ${v2Faculty.length === 0 ? '<p class="muted text-xs">No faculty profiles added yet.</p>' : v2Faculty.map((f) => `
            <div class="row-between" style="padding:10px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="v2_faculty_row_${esc(f.id)}">
              <div>
                <div class="row-wrap" style="gap:6px;margin-bottom:4px;">
                  <span class="badge ${f.status === 'PUBLISHED' ? 'badge-forest' : 'badge-gold'}">${esc(f.status)}</span>
                  <span class="badge ${f.isSchoolApproved && f.hasStaffConsent ? 'badge-forest' : 'badge-burgundy'}">
                    ${f.isSchoolApproved && f.hasStaffConsent ? 'Approved & Consented' : 'Pending Consent/Approval'}
                  </span>
                </div>
                <strong>${esc(f.teacherName)}</strong>
                <div class="muted text-xs">${esc(f.designation)} · ${esc(f.department)}</div>
              </div>
              <div class="row-wrap" style="gap:6px;">
                ${f.status !== 'PUBLISHED' && canPublishDirect && f.isSchoolApproved && f.hasStaffConsent ? `
                  <button type="button" class="btn btn-forest" data-action="v2-faculty-status" data-id="${esc(f.id)}" data-status="PUBLISHED" data-testid="v2_faculty_publish_${esc(f.id)}">Publish Profile</button>
                ` : ''}
                ${f.status === 'PUBLISHED' && canEdit ? `
                  <button type="button" class="btn btn-outline" data-action="v2-faculty-status" data-id="${esc(f.id)}" data-status="UNPUBLISHED" data-testid="v2_faculty_unpublish_${esc(f.id)}">Unpublish</button>
                ` : ''}
                ${canDelete ? `
                  <button type="button" class="btn btn-danger" data-action="v2-faculty-delete" data-id="${esc(f.id)}" data-testid="v2_faculty_delete_${esc(f.id)}">Delete</button>
                ` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      </section>
    ` : ''}

    <button type="button" class="btn btn-outline btn-full" data-route="admin/v2-academics">
      ${icon('back', 18)} Back to Academics V2 Hub
    </button>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   11. BASE 4 — PREVIEW & PUBLISH HUB (3 TABS: CHANGED CONTENT, HOME PREVIEW, ACADEMICS PREVIEW)
   Mirrors AdminV2PreviewAndPublishScreen
   ========================================================================== */
export function renderAdminV2PreviewAndPublishView(store, state, s, ui = {}) {
  const activeTab = ui.previewPublishTab || 'review';
  const v2Records = (state.academicsV2 && state.academicsV2.records) || [];
  const unpublishedV2 = v2Records.filter((r) => r.status !== ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  const unpublishedAlbums = ((state.academicsV2 && state.academicsV2.galleryAlbums) || []).filter((a) => a.status !== 'PUBLISHED');
  const unpublishedPhotos = ((state.academicsV2 && state.academicsV2.galleryPhotos) || []).filter((p) => p.status !== 'PUBLISHED');
  const unpublishedFaculty = ((state.academicsV2 && state.academicsV2.facultyProfiles) || []).filter((f) => f.status !== 'PUBLISHED');
  const isOwner = store ? store.isSchoolOwner() : true;

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'Preview & Publish Hub', backRoute: 'admin' })}

    <div class="card gold-border" data-testid="admin_v2_preview_publish_screen">
      <h2 class="section-title" style="margin:0 0 4px;">Preview &amp; Publish Verification Hub</h2>
      <p class="muted text-sm" style="margin:0 0 12px;">
        Inspect all unpublished drafts and pending approvals, preview the live Public Home and Public Academics screens, and publish verified items.
      </p>
      <div class="tabs-bar" role="tablist">
        <button type="button" class="tab-btn ${activeTab === 'review' ? 'active' : ''}" data-action="preview-publish-tab" data-tab="review" data-testid="preview_tab_review">
          Changed Content &amp; Publish (${unpublishedV2.length + unpublishedAlbums.length + unpublishedPhotos.length + unpublishedFaculty.length})
        </button>
        <button type="button" class="tab-btn ${activeTab === 'home' ? 'active' : ''}" data-action="preview-publish-tab" data-tab="home" data-testid="preview_tab_home">
          Public Home Preview
        </button>
        <button type="button" class="tab-btn ${activeTab === 'academics' ? 'active' : ''}" data-action="preview-publish-tab" data-tab="academics" data-testid="preview_tab_academics">
          Public Academics Preview
        </button>
      </div>
    </div>

    ${activeTab === 'review' ? `
      <div class="stack-md" data-testid="preview_changed_content_panel">
        <div class="card">
          <h3 class="section-title" style="margin:0 0 10px;">Unpublished Academics V2 Records (${unpublishedV2.length})</h3>
          ${unpublishedV2.length === 0 ? '<p class="muted text-sm">All Academics V2 records are published.</p>' : unpublishedV2.map((rec) => {
            const isExamResult = rec.categoryKey === 'examinations' && rec.sectionKey === 'exam_results';
            const canPub = isExamResult ? isOwner : (store ? store.canPublishV2Directly(rec.categoryKey, rec.sectionKey) : true);
            return `
              <div class="row-between" style="padding:10px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="preview_unpublished_v2_${esc(rec.id)}">
                <div>
                  <span class="badge ${isExamResult ? 'badge-burgundy' : 'badge-gold'}">${esc(rec.categoryKey)} · ${esc(rec.sectionKey)} · ${esc(rec.status)}</span>
                  <strong style="display:block;margin-top:2px;">${esc(rec.title)}</strong>
                </div>
                <div class="row-wrap" style="gap:6px;">
                  ${canPub ? `
                    <button
                      type="button"
                      class="btn btn-forest"
                      data-action="${isExamResult ? 'v2-owner-approve-publish' : 'v2-publish-record'}"
                      data-id="${esc(rec.id)}"
                      data-exam="${isExamResult ? 'true' : 'false'}"
                      data-testid="preview_publish_v2_btn_${esc(rec.id)}"
                    >
                      ${icon('publish', 15)} ${isExamResult ? 'Owner Authorize & Publish' : 'Publish Now'}
                    </button>
                  ` : `<span class="badge badge-burgundy">Awaiting School Owner Approval</span>`}
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    ` : activeTab === 'home' ? `
      <div class="card" data-testid="preview_public_home_container">
        <div class="badge badge-gold" style="margin-bottom:10px;">Live Public Home Page Preview</div>
        ${renderHomeView(state, s, ui)}
      </div>
    ` : `
      <div class="card" data-testid="preview_public_academics_container">
        <div class="badge badge-gold" style="margin-bottom:10px;">Live Public Academics Hub Preview</div>
        ${renderAcademicsHubView(state, s, ui)}
      </div>
    `}

    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   12. BASE 4 — CARD 08: SCHOOL OWNER – CONTROL CENTER (ALL 6 SECTIONS)
   Mirrors SchoolOwnerControlCenterScreen (Strictly Restricted to School Owner)
   ========================================================================== */
export function renderSchoolOwnerControlCenterView(store, state, s, ui = {}) {
  const isOwner = store ? store.isSchoolOwner() : false;

  if (!isOwner) {
    return `
      ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'School Owner – Control Center', backRoute: 'admin' })}
      <div class="card gold-border" style="text-align:center;padding:28px 20px;" data-testid="owner_control_center_denied">
        <span class="icon-tile tone-burgundy" style="width:56px;height:56px;margin:0 auto 12px;">${icon('lock', 28)}</span>
        <span class="badge badge-burgundy" style="margin-bottom:8px;">Restricted to School Owner</span>
        <h2 class="section-title" style="margin:0 0 8px;">School Owner Authorization Required</h2>
        <p class="muted text-sm" style="max-width:460px;margin:0 auto 18px;">
          The School Owner Control Center is strictly restricted to Master Admin and Super Owner accounts. Staff accounts may submit a permission request below.
        </p>

        <form id="staff-access-request-form" class="card" style="max-width:460px;margin:0 auto 16px;text-align:left;" data-testid="staff_access_request_form">
          <h4 style="margin:0 0 8px;">Request Additional Section Permission</h4>
          <div class="form-group">
            <label class="form-label">Requested Section / Category</label>
            <input name="requestedSection" class="form-input" required placeholder="e.g. examinations / exam_results" data-testid="staff_request_section_input" />
          </div>
          <div class="form-group">
            <label class="form-label">Reason / Note for School Owner</label>
            <input name="requestReason" class="form-input" required placeholder="Reason for access request" data-testid="staff_request_reason_input" />
          </div>
          <button type="submit" class="btn btn-primary btn-full" data-testid="staff_request_submit_btn">Submit Request to School Owner</button>
        </form>

        <button type="button" class="btn btn-outline" data-route="admin">
          ${icon('back', 18)} Back to Admin Dashboard
        </button>
      </div>
      ${renderFooter(s)}
    `;
  }

  const activeSection = ui.ownerControlSection || SCHOOL_OWNER_SECTIONS[0].key;
  const ownerProfile = store.getOwnerPrivateProfile();
  const accessRequests = (state.academicsV2 && state.academicsV2.staffAccessRequests) || [];
  const accounts = store.getManagementAccounts();
  const corePendingApprovals = (state.contentApprovals || []).filter((a) => a.status === 'PENDING');
  const v2Records = (state.academicsV2 && state.academicsV2.records) || [];
  const v2PendingApprovals = v2Records.filter((r) => r.status === ACADEMICS_V2_RECORD_STATUS.PENDING_APPROVAL);
  const v2UnpublishedAll = v2Records.filter((r) => r.status !== ACADEMICS_V2_RECORD_STATUS.PUBLISHED);
  const v2AuditEvents = store.getV2AccessAuditEvents();

  return `
    ${renderAdminTopHeader(store, state, s, ui, { isSubScreen: true, title: 'School Owner – Control Center', backRoute: 'admin' })}

    <div class="card gold-border" data-testid="school_owner_control_center_screen">
      <div class="row-between" style="flex-wrap:wrap;gap:10px;margin-bottom:10px;">
        <div>
          <span class="badge badge-burgundy">Card 08 · Strictly Owner-Only Governance</span>
          <h2 class="section-title" style="margin:4px 0 2px;">School Owner – Control Center (6 Sections)</h2>
          <p class="muted text-xs" style="margin:0;">
            Private Owner profile, staff access approvals, role control, content approvals, final Exam Results publication gate, and security audit trail.
          </p>
        </div>
      </div>

      <div class="chip-row" data-testid="owner_control_sections_tabs">
        ${SCHOOL_OWNER_SECTIONS.map((sec, idx) => `
          <button
            type="button"
            class="chip ${activeSection === sec.key ? 'selected' : ''}"
            data-action="select-owner-section"
            data-section="${esc(sec.key)}"
            data-testid="owner_tab_${esc(sec.key)}"
          >
            ${idx + 1}. ${esc(sec.title)}
          </button>
        `).join('')}
      </div>
    </div>

    ${activeSection === 'OWNER_PROFILE_CONTACT' ? `
      <form id="owner-private-profile-form" class="card gold-border" data-testid="owner_section_panel_profile">
        <span class="badge badge-burgundy" style="margin-bottom:6px;">Private Administrative Record — Never Exposed on Public Pages</span>
        <h3 class="section-title" style="margin:0 0 12px;">1. Owner Profile &amp; Private Contact</h3>
        <div class="two-col-responsive">
          <div class="form-group">
            <label class="form-label">Owner Full Name</label>
            <input name="ownerName" class="form-input" value="${esc(ownerProfile.ownerName)}" required data-testid="owner_profile_name_input" />
          </div>
          <div class="form-group">
            <label class="form-label">Official Designation</label>
            <input name="designation" class="form-input" value="${esc(ownerProfile.designation)}" required data-testid="owner_profile_designation_input" />
          </div>
          <div class="form-group">
            <label class="form-label">Private Direct Phone (Admin Only)</label>
            <input name="privateContactPhone" class="form-input" value="${esc(ownerProfile.privateContactPhone)}" data-testid="owner_profile_phone_input" />
          </div>
          <div class="form-group">
            <label class="form-label">Private Direct Email (Admin Only)</label>
            <input name="privateContactEmail" class="form-input" value="${esc(ownerProfile.privateContactEmail)}" data-testid="owner_profile_email_input" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Internal Governance Note</label>
          <textarea name="governanceNote" class="form-textarea" data-testid="owner_profile_note_input">${esc(ownerProfile.governanceNote)}</textarea>
        </div>
        <button type="submit" class="btn btn-primary" data-testid="owner_profile_save_btn">
          ${icon('publish', 16)} Save Private Owner Profile
        </button>
      </form>
    ` : ''}

    ${activeSection === 'STAFF_ACCESS_APPROVAL' ? `
      <div class="card gold-border" data-testid="owner_section_panel_staff_access">
        <h3 class="section-title" style="margin:0 0 10px;">2. Staff Access Approval (${accessRequests.length})</h3>
        ${accessRequests.length === 0 ? '<p class="muted text-sm">No staff access requests submitted.</p>' : accessRequests.map((req) => `
          <div class="card" style="margin-bottom:8px;" data-testid="owner_staff_request_card_${esc(req.id)}">
            <div class="row-between" style="margin-bottom:4px;">
              <strong>${esc(req.staffName)} (${esc(req.requestedRole)})</strong>
              <span class="badge ${req.status === 'APPROVED' ? 'badge-forest' : req.status === 'PENDING' ? 'badge-gold' : 'badge-burgundy'}" data-testid="owner_staff_request_status_${esc(req.id)}">${esc(req.status)}</span>
            </div>
            <div class="muted text-xs" style="margin-bottom:8px;">
              Section: <strong>${esc(req.requestedSection)}</strong> · Reason: ${esc(req.requestReason)}
            </div>
            <div class="row-wrap" style="gap:6px;">
              ${req.status === 'PENDING' ? `
                <button type="button" class="btn btn-forest" data-action="owner-review-staff-request" data-id="${esc(req.id)}" data-approve="true" data-testid="owner_approve_staff_req_${esc(req.id)}">Approve</button>
                <button type="button" class="btn btn-danger" data-action="owner-review-staff-request" data-id="${esc(req.id)}" data-approve="false" data-testid="owner_reject_staff_req_${esc(req.id)}">Reject</button>
              ` : ''}
              ${req.status === 'APPROVED' ? `
                <button type="button" class="btn btn-danger" data-action="owner-revoke-staff-request" data-id="${esc(req.id)}" data-testid="owner_revoke_staff_req_${esc(req.id)}">Revoke Access</button>
              ` : ''}
            </div>
          </div>
        `).join('')}
      </div>
    ` : ''}

    ${activeSection === 'ROLE_PERMISSION_CONTROL' ? `
      <div class="card gold-border" data-testid="owner_section_panel_roles">
        <div class="row-between" style="margin-bottom:10px;flex-wrap:wrap;gap:8px;">
          <h3 class="section-title" style="margin:0;">3. Role &amp; Permission Control (${accounts.length} Accounts)</h3>
          <button type="button" class="btn btn-primary" data-route="admin/section/STAFF_MANAGEMENT" data-testid="owner_open_full_staff_mgmt_btn">
            ${icon('settings', 16)} Open Full 20×5 RBAC Matrix Editor
          </button>
        </div>
        <div class="stack-sm">
          ${accounts.map((acc) => `
            <div class="row-between" style="padding:10px 0;border-bottom:1px solid var(--border-subtle);flex-wrap:wrap;gap:8px;" data-testid="owner_role_account_row_${acc.id}">
              <div>
                <strong>${esc(acc.displayName)}</strong> (@${esc(acc.username)})
                <span class="badge badge-gold" style="margin-left:6px;">${esc(roleBadgeLabel(acc.role))}</span>
                <span class="badge ${acc.isActive ? 'badge-forest' : 'badge-burgundy'}">${acc.isActive ? 'Active' : 'Suspended'}</span>
              </div>
              ${acc.role !== MANAGEMENT_ROLES.MASTER_ADMIN && store.isMasterAdmin() ? `
                <button
                  type="button"
                  class="btn ${acc.isActive ? 'btn-danger' : 'btn-forest'}"
                  data-action="owner-toggle-account-active"
                  data-id="${acc.id}"
                  data-active="${acc.isActive ? 'false' : 'true'}"
                  data-testid="owner_toggle_account_btn_${acc.id}"
                >
                  ${acc.isActive ? 'Suspend Account' : 'Activate Account'}
                </button>
              ` : ''}
            </div>
          `).join('')}
        </div>
      </div>
    ` : ''}

    ${activeSection === 'APPROVAL_REQUESTS' ? `
      <div class="card gold-border" data-testid="owner_section_panel_approvals">
        <div class="row-between" style="margin-bottom:10px;flex-wrap:wrap;gap:8px;">
          <h3 class="section-title" style="margin:0;">4. Approval Requests (Core: ${corePendingApprovals.length} · V2: ${v2PendingApprovals.length})</h3>
          <button type="button" class="btn btn-outline" data-route="admin/section/APPROVAL_CENTER">Open Core Approval Center</button>
        </div>
        ${v2PendingApprovals.length === 0 ? '<p class="muted text-sm">No pending Academics V2 approval requests.</p>' : v2PendingApprovals.map((rec) => {
          const isExam = rec.categoryKey === 'examinations' && rec.sectionKey === 'exam_results';
          return `
            <div class="card" style="margin-bottom:8px;" data-testid="owner_v2_pending_card_${esc(rec.id)}">
              <div class="row-between" style="margin-bottom:4px;">
                <span class="badge ${isExam ? 'badge-burgundy' : 'badge-gold'}">${esc(rec.categoryKey)} · ${esc(rec.sectionKey)}</span>
                <span class="muted text-xs">By ${esc(rec.updatedByName)}</span>
              </div>
              <strong style="display:block;margin-bottom:8px;">${esc(rec.title)}</strong>
              <div class="row-wrap" style="gap:6px;">
                <button type="button" class="btn btn-forest" data-action="v2-owner-approve-publish" data-id="${esc(rec.id)}" data-exam="${isExam ? 'true' : 'false'}" data-testid="owner_approve_v2_btn_${esc(rec.id)}">
                  Approve &amp; Publish
                </button>
                <button type="button" class="btn btn-danger" data-action="v2-set-record-status" data-id="${esc(rec.id)}" data-status="DRAFT" data-testid="owner_return_draft_v2_btn_${esc(rec.id)}">
                  Return to Draft
                </button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    ` : ''}

    ${activeSection === 'FINAL_PREVIEW_APPROVAL' ? `
      <div class="card gold-border" data-testid="owner_section_panel_final_preview">
        <div class="row-between" style="margin-bottom:10px;flex-wrap:wrap;gap:8px;">
          <h3 class="section-title" style="margin:0;">5. Final Preview &amp; Exam Results Publication Gate (${v2UnpublishedAll.length})</h3>
          <button type="button" class="btn btn-outline" data-route="admin/preview-publish">Open 3-Tab Preview Hub</button>
        </div>
        ${v2UnpublishedAll.length === 0 ? '<p class="muted text-sm">No unpublished Academics V2 records awaiting final Owner sign-off.</p>' : v2UnpublishedAll.map((rec) => {
          const isExam = rec.categoryKey === 'examinations' && rec.sectionKey === 'exam_results';
          return `
            <div class="card" style="margin-bottom:8px;" data-testid="owner_final_preview_card_${esc(rec.id)}">
              <div class="row-between" style="margin-bottom:4px;">
                <span class="badge ${isExam ? 'badge-burgundy' : 'badge-gold'}">${esc(rec.categoryKey)} · ${esc(rec.sectionKey)} (${esc(rec.status)})</span>
                ${isExam ? `<span class="badge badge-burgundy">Owner Exam Gate</span>` : ''}
              </div>
              <strong style="display:block;">${esc(rec.title)}</strong>
              <p class="muted text-xs" style="margin:4px 0 8px;">${esc(rec.body || rec.summary)}</p>
              <button
                type="button"
                class="btn btn-forest"
                data-action="v2-owner-approve-publish"
                data-id="${esc(rec.id)}"
                data-exam="${isExam ? 'true' : 'false'}"
                data-testid="owner_final_authorize_btn_${esc(rec.id)}"
              >
                ${icon('verified', 16)} ${isExam ? 'Authorize & Publish Exam Result' : 'Final Owner Approve & Publish'}
              </button>
            </div>
          `;
        }).join('')}
      </div>
    ` : ''}

    ${activeSection === 'ACCESS_HISTORY_SECURITY' ? `
      <div class="card gold-border" data-testid="owner_section_panel_audit">
        <div class="row-between" style="margin-bottom:10px;flex-wrap:wrap;gap:8px;">
          <h3 class="section-title" style="margin:0;">6. Access History &amp; Security Audit (${v2AuditEvents.length} V2 Events)</h3>
          <div class="row-wrap" style="gap:6px;">
            <button type="button" class="btn btn-outline" data-route="admin/section/BACKUP_RESTORE">Backup &amp; Restore</button>
            <button type="button" class="btn btn-outline" data-route="admin/section/ACTIVITY_LOG">Full Activity Log</button>
          </div>
        </div>
        ${v2AuditEvents.length === 0 ? '<p class="muted text-sm">No V2 governance audit events recorded yet.</p>' : v2AuditEvents.slice(0, 40).map((ev) => `
          <div style="padding:8px 0;border-bottom:1px solid var(--border-subtle);" data-testid="owner_audit_event_${esc(ev.id)}">
            <div class="row-between">
              <strong class="text-xs">${esc(ev.targetSection)} · ${esc(ev.action)}</strong>
              <span class="muted text-xs">${esc(new Date(ev.timestampMillis).toLocaleString())}</span>
            </div>
            <div class="muted text-xs">${esc(ev.details)} — <em>${esc(ev.actorName)} (${esc(ev.actorRole)})</em></div>
          </div>
        `).join('')}
      </div>
    ` : ''}

    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}

