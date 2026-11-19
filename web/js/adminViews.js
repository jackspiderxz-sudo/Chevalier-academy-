/**
 * Separated School Management & Admin Portal Views for Web.
 * Preserves Admin CMS editors + Expandable Parent Portal Management
 * with all 11 Admin Submenu screens mirroring Android ManagementPortalScreen.kt
 * and ParentPortalManagementScreens.kt.
 */

import { icon } from './icons.js';
import { ADMIN_PPM_MODULES, getAdminPpmStrings } from './i18n.js';
import { esc, renderFooter } from './publicViews.js';

export function renderAdminGateView(store, s, ui) {
  const hasPin = store.hasAdminPin();
  return `
    <div class="card gold-border" style="max-width:520px;margin:0 auto;width:100%;" data-testid="admin_auth_gate">
      <div class="row-wrap" style="margin-bottom:14px;">
        <span class="icon-tile tone-burgundy" style="width:48px;height:48px;">${icon('lock', 24)}</span>
        <div>
          <span class="badge badge-gold">Separated Management Area</span>
          <h2 class="section-title" style="margin:4px 0 0;">${esc(s.drawerManagementLogin)}</h2>
        </div>
      </div>

      <p class="muted text-sm" style="margin:0 0 16px;">
        ${hasPin
          ? 'Enter your local Administrator PIN to open Chevalier Academy School Management & Parent Portal Management.'
          : 'First-time setup: Create a 4 to 8 digit local Administrator PIN for this browser session.'}
      </p>

      ${ui.adminAuthError ? `<div class="badge" style="background:var(--danger-soft);color:var(--danger);margin-bottom:12px;width:100%;padding:10px 12px;border-radius:10px;">${esc(ui.adminAuthError)}</div>` : ''}

      <form id="admin-auth-form" data-mode="${hasPin ? 'login' : 'setup'}">
        ${!hasPin ? `
          <div class="form-group">
            <label class="form-label" for="admin-display-name">Administrator Name</label>
            <input id="admin-display-name" name="displayName" class="form-input" value="School Administrator" required />
          </div>
        ` : ''}
        <div class="form-group">
          <label class="form-label" for="admin-pin-input">${hasPin ? 'Administrator PIN' : 'Create PIN (4–8 digits)'}</label>
          <input id="admin-pin-input" name="pin" type="password" inputmode="numeric" pattern="[0-9]{4,8}" minlength="4" maxlength="8" class="form-input" placeholder="e.g. 2468" required />
        </div>
        <div class="stack-sm">
          <button type="submit" class="btn btn-primary btn-full" data-testid="admin_auth_submit_btn">
            ${icon('security', 18)} ${hasPin ? 'Unlock Management Portal' : 'Set Local PIN & Enter Portal'}
          </button>
          <button type="button" class="btn btn-outline btn-full" data-route="home">
            ${icon('back', 18)} ${esc(s.returnToPublicAppBtn)}
          </button>
        </div>
      </form>
    </div>
    ${renderFooter(s)}
  `;
}

export function renderAdminDashboardView(state, s, ui) {
  const ppmStrings = getAdminPpmStrings(state.preferences.language);
  const expanded = Boolean(ui.ppmExpanded);

  return `
    <div class="card gold-border">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div>
          <span class="badge badge-burgundy">${esc(state.admin.displayName || 'Administrator')}</span>
          <h2 class="section-title" style="margin:6px 0 2px;">School Management Dashboard</h2>
          <p class="muted text-sm" style="margin:0;">Manage live public website content, verified records, JSON backups, and Parent Portal Management.</p>
        </div>
        <div class="row-wrap">
          <button type="button" class="btn btn-outline" data-route="home">${icon('visibility', 18)} Public Site</button>
          <button type="button" class="btn btn-danger" data-action="admin-logout">${icon('lock', 18)} Lock</button>
        </div>
      </div>
    </div>

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

    <!-- EXISTING PUBLIC CMS MANAGEMENT SECTIONS -->
    <section>
      <div class="section-header"><h3 class="section-title">School Website CMS Modules</h3></div>
      <div class="quick-access-grid">
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/profile">
          <div class="stack-sm">
            <span class="icon-tile tone-burgundy">${icon('info', 22)}</span>
            <strong>Profile &amp; Contact</strong>
            <span class="muted text-xs">Welcome, About, Phone, Email</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/academics">
          <div class="stack-sm">
            <span class="icon-tile tone-forest">${icon('school', 22)}</span>
            <strong>Academics &amp; Courses</strong>
            <span class="muted text-xs">${state.academics.length} programs · 4 streams</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/updates">
          <div class="stack-sm">
            <span class="icon-tile tone-gold">${icon('campaign', 22)}</span>
            <strong>News &amp; Announcements</strong>
            <span class="muted text-xs">${state.news.length} news · ${state.announcements.length} notices</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/media">
          <div class="stack-sm">
            <span class="icon-tile tone-burgundy">${icon('gallery', 22)}</span>
            <strong>Hero &amp; Gallery</strong>
            <span class="muted text-xs">${state.heroPhotos.length} hero · ${state.galleryMedia.length} media</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/campus">
          <div class="stack-sm">
            <span class="icon-tile tone-forest">${icon('bus', 22)}</span>
            <strong>Facilities &amp; Transport</strong>
            <span class="muted text-xs">${state.facilities.length} facilities · ${state.transportRoutes.length} routes</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/social">
          <div class="stack-sm">
            <span class="icon-tile tone-gold">${icon('share', 22)}</span>
            <strong>Social Channels</strong>
            <span class="muted text-xs">4 official social slots</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="admin/cms/backup">
          <div class="stack-sm">
            <span class="icon-tile tone-burgundy">${icon('download', 22)}</span>
            <strong>Backup &amp; Logs</strong>
            <span class="muted text-xs">Export/Restore JSON · ${(state.admin.activityLog || []).length} logs</span>
          </div>
        </button>
      </div>
    </section>

    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   ADMIN PARENT PORTAL MANAGEMENT — 11 SUBMENU SCREENS
   ========================================================================== */
export function renderAdminPpmModuleView(state, s, ui, moduleKey) {
  const ppmStrings = getAdminPpmStrings(state.preferences.language);
  const mod = ADMIN_PPM_MODULES.find((m) => m.key === moduleKey) || ADMIN_PPM_MODULES[0];
  const copy = ppmStrings.modules[mod.key];
  const scopeState = ui.ppmScopes[mod.key] || {
    scope: mod.defaultBulkScope ? 'bulk' : 'individual',
    classIdx: 0,
    studentIdx: 0
  };
  const isBulk = scopeState.scope === 'bulk';
  const targetOptions = isBulk ? ppmStrings.sampleClassOptions : ppmStrings.sampleStudentOptions;
  const activeIdx = isBulk ? scopeState.classIdx : scopeState.studentIdx;
  const activeTarget = targetOptions[activeIdx] || targetOptions[0];

  return `
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

    <!-- Selection Scope: Individual Student vs Bulk / Class-wise -->
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

    <!-- Management Actions: Add, Edit, Preview, Publish -->
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

    <!-- Structured Sample Preview -->
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
   LIVE WEB CMS EDITORS (Profile, Academics/Courses, Updates, Media, Campus, Social, Backup)
   ========================================================================== */
export function renderAdminCmsView(state, s, section) {
  let body = '';

  if (section === 'profile') {
    const p = state.profile;
    body = `
      <form class="card" id="cms-profile-form">
        <h2 class="section-title" style="margin-bottom:14px;">School Profile &amp; Contact Editor</h2>
        <div class="form-group"><label class="form-label">Welcome Introduction</label><textarea name="welcomeIntroduction" class="form-textarea">${esc(p.welcomeIntroduction)}</textarea></div>
        <div class="two-col-responsive">
          <div class="form-group"><label class="form-label">Phone Number</label><input name="phone" class="form-input" value="${esc(p.phone)}" placeholder="+91 ..." /></div>
          <div class="form-group"><label class="form-label">Official Email</label><input name="email" type="email" class="form-input" value="${esc(p.email)}" /></div>
          <div class="form-group"><label class="form-label">Website</label><input name="website" class="form-input" value="${esc(p.website)}" /></div>
          <div class="form-group"><label class="form-label">Office Hours</label><input name="officeHours" class="form-input" value="${esc(p.officeHours)}" placeholder="Mon–Sat, 8:30 AM – 4:00 PM" /></div>
        </div>
        <div class="form-group"><label class="form-label">Principal Name</label><input name="principalName" class="form-input" value="${esc(p.principalName)}" /></div>
        <div class="form-group"><label class="form-label">School History</label><textarea name="history" class="form-textarea">${esc(p.history)}</textarea></div>
        <div class="form-group"><label class="form-label">Vision</label><textarea name="vision" class="form-textarea">${esc(p.vision)}</textarea></div>
        <div class="form-group"><label class="form-label">Mission</label><textarea name="mission" class="form-textarea">${esc(p.mission)}</textarea></div>
        <button type="submit" class="btn btn-primary">${icon('publish', 18)} Save &amp; Publish Profile</button>
      </form>
    `;
  } else if (section === 'academics') {
    body = `
      <form class="card" id="cms-academic-form">
        <h2 class="section-title" style="margin-bottom:12px;">Add Academic Program</h2>
        <div class="two-col-responsive">
          <div class="form-group"><label class="form-label">Title</label><input name="title" class="form-input" required placeholder="e.g. Primary & Middle School" /></div>
          <div class="form-group"><label class="form-label">Level</label><input name="level" class="form-input" placeholder="Grades I – VIII" /></div>
        </div>
        <div class="form-group"><label class="form-label">Description</label><textarea name="description" class="form-textarea" required></textarea></div>
        <button type="submit" class="btn btn-primary">${icon('add', 18)} Add Program</button>
      </form>
      <div class="card">
        <h3 class="section-title" style="margin-bottom:12px;">4 Higher Secondary Course Streams</h3>
        <div class="stack-md">
          ${state.courses.map((c) => `
            <form class="cms-course-form" data-id="${c.id}" style="padding:12px;border:1px solid var(--border-subtle);border-radius:12px;">
              <strong style="display:block;margin-bottom:8px;">${esc(c.groupCode)}</strong>
              <div class="two-col-responsive">
                <div class="form-group"><label class="form-label">Stream Title</label><input name="title" class="form-input" value="${esc(c.title)}" required /></div>
                <div class="form-group"><label class="form-label">Medium</label><input name="medium" class="form-input" value="${esc(c.medium)}" placeholder="English Medium" /></div>
              </div>
              <div class="form-group"><label class="form-label">Subjects</label><input name="subjects" class="form-input" value="${esc(c.subjects)}" placeholder="Physics, Chemistry, Mathematics, Computer Science" /></div>
              <button type="submit" class="btn btn-forest">${icon('publish', 18)} Save ${esc(c.groupCode)}</button>
            </form>
          `).join('')}
        </div>
      </div>
    `;
  } else if (section === 'updates') {
    body = `
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
      ${state.announcements.length || state.news.length ? `
        <div class="card">
          <h3 style="margin:0 0 10px;">Published Items</h3>
          ${state.announcements.map((a) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);">
              <span><strong>[Notice]</strong> ${esc(a.title)}</span>
              <button type="button" class="icon-btn" data-action="delete-announcement" data-id="${a.id}" aria-label="Delete">${icon('delete', 18)}</button>
            </div>
          `).join('')}
          ${state.news.map((n) => `
            <div class="row-between" style="padding:8px 0;border-bottom:1px solid var(--border-subtle);">
              <span><strong>[News]</strong> ${esc(n.title)}</span>
              <button type="button" class="icon-btn" data-action="delete-news" data-id="${n.id}" aria-label="Delete">${icon('delete', 18)}</button>
            </div>
          `).join('')}
        </div>
      ` : ''}
    `;
  } else if (section === 'media') {
    body = `
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
    `;
  } else if (section === 'campus') {
    body = `
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
    `;
  } else if (section === 'social') {
    body = `
      <div class="card">
        <h2 class="section-title" style="margin-bottom:12px;">Official Social Channels</h2>
        <div class="stack-md">
          ${state.socialLinks.map((link) => `
            <form class="cms-social-form" data-id="${link.id}" style="padding:12px;border:1px solid var(--border-subtle);border-radius:12px;">
              <strong style="display:block;margin-bottom:8px;">${esc(link.platformName)}</strong>
              <div class="two-col-responsive">
                <div class="form-group"><label class="form-label">Handle / Label</label><input name="handle" class="form-input" value="${esc(link.handle)}" placeholder="@chevalieracademy" /></div>
                <div class="form-group"><label class="form-label">Official URL</label><input name="url" class="form-input" value="${esc(link.url)}" placeholder="https://..." /></div>
              </div>
              <button type="submit" class="btn btn-primary">${icon('publish', 18)} Save Channel</button>
            </form>
          `).join('')}
        </div>
      </div>
    `;
  } else {
    const logs = state.admin.activityLog || [];
    body = `
      <div class="card">
        <h2 class="section-title" style="margin-bottom:8px;">JSON Backup &amp; Restore</h2>
        <p class="muted text-sm" style="margin:0 0 14px;">Export verified school records as JSON or restore a previous backup file.</p>
        <div class="row-wrap">
          <button type="button" class="btn btn-primary" data-action="export-backup">${icon('download', 18)} Export Backup JSON</button>
          <label class="btn btn-outline" style="cursor:pointer;">
            ${icon('upload', 18)} Restore Backup JSON
            <input type="file" id="import-backup-input" accept="application/json" style="display:none;" />
          </label>
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

  return `
    ${body}
    <button type="button" class="btn btn-outline btn-full" data-route="admin">
      ${icon('back', 18)} Back to Admin Dashboard
    </button>
    ${renderFooter(s)}
  `;
}
