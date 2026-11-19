/**
 * Public & Parent Portal Views for Chevalier Academy Responsive Website.
 * Mirrors Android PrimaryTabScreens.kt, SecondaryDetailScreens.kt,
 * UpdatesAndSettingsScreens.kt, and ParentDashboardScreens.kt.
 */

import { icon } from './icons.js';
import { PARENT_MODULES } from './i18n.js';

export function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function safeExternalUrl(url) {
  const trimmed = String(url || '').trim();
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  if (trimmed.length > 0 && !trimmed.includes(':')) return `https://${trimmed}`;
  return '';
}

export function googleMapsUrl(query) {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query || 'Chevalier Academy Matric Higher Secondary School, Siluvathur Road, Dindigul - 624 306')}`;
}

export function renderFooter(s) {
  return `
    <footer class="jk-footer" data-testid="powered_by_jk_labs_footer">
      <p class="jk-footer-brand">${esc(s.poweredByAttribution)}</p>
      <p class="muted text-xs" style="margin:0;">${esc(s.shortSchoolName)} · ${esc(s.officialAddress)}</p>
    </footer>
  `;
}

function renderAwaitingCard(title, description, iconName, s) {
  return `
    <div class="card">
      <div class="row-wrap" style="align-items:flex-start;">
        <span class="icon-tile tone-gold">${icon(iconName, 22)}</span>
        <div class="stack-sm" style="flex:1;min-width:200px;">
          <span class="badge badge-gold" style="align-self:flex-start;">${esc(s.awaitingVerificationLabel)}</span>
          <strong style="font-size:0.98rem;">${esc(title)}</strong>
          <p class="muted text-sm" style="margin:0;">${esc(description)}</p>
        </div>
      </div>
    </div>
  `;
}

function renderVerifiedField(label, value, iconName, s) {
  const hasVal = Boolean(value && String(value).trim());
  return `
    <div class="field-row">
      <span class="icon-tile tone-burgundy" style="width:36px;height:36px;">${icon(iconName, 18)}</span>
      <div style="flex:1;min-width:0;">
        <div class="field-row-label">${esc(label)}</div>
        ${hasVal
          ? `<div class="field-row-value">${esc(value)}</div>`
          : `<div class="field-row-pending">${esc(s.awaitingVerificationLabel)}</div>`}
      </div>
    </div>
  `;
}

function renderSchoolBrandBanner(profile, s) {
  return `
    <section class="brand-header-card" aria-label="${esc(profile.schoolName)}">
      <img src="./assets/chevalier_emblem.png" alt="${esc(profile.shortName)} Emblem" />
      <div style="min-width:0;flex:1;">
        <h2 class="brand-header-name">${esc(profile.schoolName)}</h2>
        <p class="brand-header-motto">"${esc(profile.motto)}"</p>
        <p class="brand-header-address">${esc(profile.addressLine)}</p>
      </div>
    </section>
  `;
}

/* ==========================================================================
   Onboarding Language Selector
   ========================================================================== */
export function renderOnboarding(state, s) {
  const lang = state.preferences.language;
  const options = [
    { code: 'en', native: 'English', sub: 'Default' },
    { code: 'ta', native: 'தமிழ்', sub: 'Tamil' },
    { code: 'hi', native: 'हिन्दी', sub: 'Hindi' }
  ];
  return `
    <div class="onboarding-shell" data-testid="onboarding_screen">
      <div class="onboarding-card">
        <img src="./assets/chevalier_emblem.png" alt="Chevalier Academy Emblem" class="onboarding-emblem" />
        <p class="onboarding-school">${esc(s.officialSchoolName)}</p>
        <p class="onboarding-motto">"${esc(s.officialMotto)}"</p>
        <h1 class="section-title" style="margin-bottom:6px;">${esc(s.chooseLanguageTitle)}</h1>
        <p class="muted text-sm" style="margin:0;">${esc(s.chooseLanguageSubtitle)}</p>

        <div class="lang-grid" role="radiogroup" aria-label="${esc(s.chooseLanguageTitle)}">
          ${options.map((opt) => `
            <button
              type="button"
              class="lang-option ${lang === opt.code ? 'selected' : ''}"
              data-action="pick-onboarding-lang"
              data-lang="${opt.code}"
              aria-pressed="${lang === opt.code}"
            >
              <span class="lang-option-native">${esc(opt.native)}</span>
              <span class="lang-option-sub">${esc(opt.sub)}</span>
            </button>
          `).join('')}
        </div>

        <button type="button" class="btn btn-primary btn-full" data-action="complete-onboarding" data-testid="onboarding_continue_btn">
          ${esc(s.continueButton)} ${icon('arrowForward', 18)}
        </button>
        <p class="jk-footer-brand" style="margin-top:18px;">${esc(s.poweredByAttribution)}</p>
      </div>
    </div>
  `;
}

/* ==========================================================================
   HOME SCREEN — Exact 6 Sections Matching Android HomeScreen
   ========================================================================== */
export function renderHomeView(state, s, ui) {
  const { profile, heroPhotos, news, announcements, galleryMedia, socialLinks } = state;
  const activeSlide = heroPhotos.length > 0 ? Math.min(ui.heroSlideIndex || 0, heroPhotos.length - 1) : 0;
  const activeSocials = socialLinks.filter((l) => l.active && l.url);

  // Section 1: Hero Banner
  const heroHtml = heroPhotos.length > 0
    ? `
      <div class="hero-banner" data-testid="home_campus_hero_banner">
        <img src="${esc(heroPhotos[activeSlide].imageUrl)}" alt="${esc(heroPhotos[activeSlide].caption || profile.schoolName)}" class="hero-slide-img" />
        ${heroPhotos[activeSlide].caption ? `<div class="hero-slide-caption">${esc(heroPhotos[activeSlide].caption)}</div>` : ''}
        ${heroPhotos.length > 1 ? `
          <div class="hero-dots" role="tablist" aria-label="Campus slides">
            ${heroPhotos.map((_, idx) => `
              <button type="button" class="hero-dot ${idx === activeSlide ? 'active' : ''}" data-action="hero-slide" data-index="${idx}" aria-label="Slide ${idx + 1}"></button>
            `).join('')}
          </div>
        ` : ''}
      </div>
    `
    : `
      <div class="hero-banner" data-testid="home_campus_hero_banner">
        <div class="hero-banner-inner">
          <div class="hero-icon-circle">${icon('camera', 26)}</div>
          <h2 style="font-family:var(--font-serif);margin:0;font-size:1.35rem;">${esc(profile.schoolName)}</h2>
          <p style="margin:0;color:#EABF16;font-style:italic;font-weight:600;">"${esc(profile.motto)}"</p>
          <span class="badge badge-gold" style="color:#FFF9E8;border-color:rgba(234,191,22,0.75);">${esc(s.heroBannerPendingLabel)}</span>
          <p style="margin:0;font-size:0.86rem;opacity:0.9;">${esc(s.heroBannerPendingDesc)}</p>
        </div>
      </div>
    `;

  // Section 4: Latest Updates
  const featuredNews = news.find((n) => n.isFeatured) || news[0];
  const otherNews = news.filter((n) => n.id !== featuredNews?.id).slice(0, 2);
  const topAnnouncements = announcements.slice(0, 2);

  let updatesHtml = '';
  if (news.length === 0 && announcements.length === 0) {
    updatesHtml = renderAwaitingCard(s.latestUpdatesTitle, s.announcementsPendingDesc, 'campaign', s);
  } else {
    updatesHtml = `<div class="stack-sm">
      ${featuredNews ? `
        <article class="card gold-border interactive" data-action="open-news" data-id="${featuredNews.id}">
          <div class="row-between" style="margin-bottom:8px;">
            <span class="badge badge-burgundy">${esc(s.featuredNewsBadge)}</span>
            <span class="muted text-xs">${esc(featuredNews.publishedDateText)}</span>
          </div>
          ${featuredNews.imageUrl ? `<img src="${esc(featuredNews.imageUrl)}" alt="${esc(featuredNews.title)}" style="width:100%;height:170px;object-fit:cover;border-radius:12px;margin-bottom:10px;" />` : ''}
          <h3 style="margin:0 0 6px;font-size:1.05rem;">${esc(featuredNews.title)}</h3>
          <p class="muted text-sm" style="margin:0;">${esc(featuredNews.summary)}</p>
        </article>
      ` : ''}
      ${otherNews.map((item) => `
        <article class="card interactive" data-action="open-news" data-id="${item.id}">
          <div class="row-between" style="margin-bottom:6px;">
            <span class="badge badge-gold">${esc(s.tabNews)}</span>
            <span class="muted text-xs">${esc(item.publishedDateText)}</span>
          </div>
          <strong style="display:block;margin-bottom:4px;">${esc(item.title)}</strong>
          <p class="muted text-sm" style="margin:0;">${esc(item.summary)}</p>
        </article>
      `).join('')}
      ${topAnnouncements.map((ann) => `
        <article class="card burgundy-border interactive" data-action="open-announcement" data-id="${ann.id}">
          <div class="row-between" style="margin-bottom:6px;">
            <span class="badge badge-burgundy-soft">${esc(ann.priorityLabel || s.tabAnnouncements)}</span>
            <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
          </div>
          <strong style="display:block;margin-bottom:4px;">${esc(ann.title)}</strong>
          <p class="muted text-sm" style="margin:0;">${esc(ann.message)}</p>
        </article>
      `).join('')}
    </div>`;
  }

  // Section 5: Gallery Preview
  const galleryPreviewHtml = galleryMedia.length === 0
    ? renderAwaitingCard(s.galleryPreviewTitle, s.galleryPendingDesc, 'gallery', s)
    : `
      <div class="gallery-grid">
        ${galleryMedia.slice(0, 4).map((m, idx) => `
          <button type="button" class="gallery-tile" data-action="open-lightbox" data-index="${idx}">
            <img src="${esc(m.imageUrl)}" alt="${esc(m.caption || s.titleGallery)}" />
            ${m.caption ? `<div class="gallery-tile-caption">${esc(m.caption)}</div>` : ''}
          </button>
        `).join('')}
      </div>
    `;

  return `
    <!-- SECTION 1 — HERO BANNER -->
    <section data-testid="home_section_hero">${heroHtml}</section>

    <!-- SECTION 2 — WELCOME / ABOUT -->
    <section data-testid="home_section_welcome">
      <div class="section-header"><h2 class="section-title">${esc(s.welcomeSectionTitle)}</h2></div>
      <div class="card">
        ${profile.welcomeIntroduction
          ? `<p style="margin:0 0 14px;">${esc(profile.welcomeIntroduction)}</p>`
          : `
            <span class="badge badge-gold" style="margin-bottom:10px;">${esc(s.awaitingVerificationLabel)}</span>
            <p class="muted text-sm" style="margin:0 0 14px;">${esc(s.welcomePendingDesc)}</p>
          `}
        <button type="button" class="btn btn-outline" data-route="about" data-testid="home_welcome_read_more">
          ${esc(s.readMore)} ${icon('arrowForward', 18)}
        </button>
      </div>
    </section>

    <!-- SECTION 3 — QUICK ACCESS (4 Cards: Academics, Courses, Facilities, Transport) -->
    <section data-testid="home_section_quick_access">
      <div class="section-header"><h2 class="section-title">${esc(s.quickAccessTitle)}</h2></div>
      <div class="quick-access-grid">
        <button type="button" class="card interactive" style="text-align:left;" data-route="academics" data-testid="home_quick_academics">
          <div class="stack-sm">
            <span class="icon-tile tone-burgundy">${icon('school', 22)}</span>
            <strong>${esc(s.quickCardAcademicsTitle)}</strong>
            <span class="muted text-xs">${esc(s.quickCardAcademicsDesc)}</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="courses" data-testid="home_quick_courses">
          <div class="stack-sm">
            <span class="icon-tile tone-forest">${icon('book', 22)}</span>
            <strong>${esc(s.quickCardCoursesTitle)}</strong>
            <span class="muted text-xs">${esc(s.quickCardCoursesDesc)}</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="facilities" data-testid="home_quick_facilities">
          <div class="stack-sm">
            <span class="icon-tile tone-gold">${icon('apartment', 22)}</span>
            <strong>${esc(s.quickCardFacilitiesTitle)}</strong>
            <span class="muted text-xs">${esc(s.quickCardFacilitiesDesc)}</span>
          </div>
        </button>
        <button type="button" class="card interactive" style="text-align:left;" data-route="transport" data-testid="home_quick_transport">
          <div class="stack-sm">
            <span class="icon-tile tone-burgundy">${icon('bus', 22)}</span>
            <strong>${esc(s.quickCardTransportTitle)}</strong>
            <span class="muted text-xs">${esc(s.quickCardTransportDesc)}</span>
          </div>
        </button>
      </div>
    </section>

    <!-- SECTION 4 & 5 — UPDATES & GALLERY PREVIEW -->
    <section data-testid="home_section_updates">
      <div class="section-header">
        <h2 class="section-title">${esc(s.latestUpdatesTitle)}</h2>
        <button type="button" class="btn btn-text" data-route="updates">${esc(s.viewAll)}</button>
      </div>
      ${updatesHtml}
    </section>

    <section data-testid="home_section_gallery">
      <div class="section-header">
        <h2 class="section-title">${esc(s.galleryPreviewTitle)}</h2>
        <button type="button" class="btn btn-text" data-route="gallery">${esc(s.viewAll)}</button>
      </div>
      ${galleryPreviewHtml}
    </section>

    <!-- SECTION 6 — CONTACT & SOCIAL MEDIA -->
    <section data-testid="home_section_contact_social">
      <div class="section-header">
        <h2 class="section-title">${esc(s.contactSocialTitle)}</h2>
        <button type="button" class="btn btn-text" data-route="contact">${esc(s.viewAll)}</button>
      </div>
      <div class="card">
        ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
        ${renderVerifiedField(s.phoneLabel, profile.phone, 'contact', s)}
        ${renderVerifiedField(s.emailLabel, profile.email, 'email', s)}
        <div class="row-wrap" style="margin-top:14px;">
          <a href="${esc(googleMapsUrl(profile.mapCoordinatesOrQuery || profile.addressLine))}" target="_blank" rel="noopener noreferrer" class="btn btn-outline-forest" style="flex:1;">
            ${icon('map', 18)} ${esc(s.openMapsActionLabel)}
          </a>
          ${profile.phone ? `<a href="tel:${esc(profile.phone)}" class="btn btn-primary">${icon('contact', 18)} ${esc(s.callActionLabel)}</a>` : ''}
          ${profile.email ? `<a href="mailto:${esc(profile.email)}" class="btn btn-outline">${icon('email', 18)} ${esc(s.emailActionLabel)}</a>` : ''}
        </div>
        <div style="border-top:1px solid var(--border-subtle);margin-top:16px;padding-top:12px;">
          <div class="row-between" style="margin-bottom:8px;">
            <strong class="text-sm">${esc(s.officialChannelsTitle)}</strong>
            <button type="button" class="btn btn-text" data-route="social">${esc(s.viewAll)}</button>
          </div>
          ${activeSocials.length === 0
            ? `<span class="badge badge-gold">${esc(s.awaitingVerificationLabel)}</span>`
            : `<div class="chip-row">${activeSocials.map((link) => `
                <a href="${esc(safeExternalUrl(link.url))}" target="_blank" rel="noopener noreferrer" class="chip">
                  ${esc(link.platformName)} ${icon('openInNew', 15)}
                </a>
              `).join('')}</div>`}
        </div>
      </div>
    </section>

    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   PARENT DASHBOARD — Finalized 6-Section Design + All 11 Parent Modules
   ========================================================================== */
export function renderParentDashboardView(state, s, ui) {
  const query = (ui.parentSearchQuery || '').trim().toLowerCase();
  const filteredModules = query
    ? PARENT_MODULES.filter((m) =>
        s[m.titleKey].toLowerCase().includes(query) ||
        s[m.descKey].toLowerCase().includes(query)
      )
    : PARENT_MODULES;

  const recentAnnouncements = state.announcements.slice(0, 2);
  const recentNews = state.news.slice(0, 1);

  return `
    ${ui.parentSearchOpen ? `
      <div class="card" style="padding:12px 14px;" data-testid="parent_dashboard_search_field">
        <div class="row-between">
          <span style="color:var(--brand-burgundy);display:inline-flex;">${icon('search', 20)}</span>
          <input
            type="search"
            id="parent-search-input"
            class="form-input"
            style="border:none;min-height:40px;padding:6px 8px;"
            placeholder="${esc(s.parentSearchPlaceholder)}"
            value="${esc(ui.parentSearchQuery || '')}"
            aria-label="${esc(s.parentSearchPlaceholder)}"
          />
          ${ui.parentSearchQuery ? `<button type="button" class="icon-btn" data-action="clear-parent-search" aria-label="${esc(s.parentClearSearchDesc)}">${icon('close', 18)}</button>` : ''}
        </div>
      </div>
    ` : ''}

    <!-- SECTION B — MAROON STUDENT INFORMATION CARD (SAMPLE PREVIEW) -->
    <section
      class="student-card"
      role="button"
      tabindex="0"
      data-route="parent/module/student_profile"
      data-testid="parent_student_info_card"
    >
      <div class="student-card-top">
        <span class="badge badge-gold" style="color:#EABF16;background:rgba(234,191,22,0.2);border-color:rgba(234,191,22,0.8);">
          ${esc(s.parentSamplePreviewBadge)}
        </span>
        <span style="color:#EABF16;">${icon('chevronRight', 22)}</span>
      </div>
      <div class="student-card-body">
        <div class="student-avatar" data-testid="parent_student_avatar">
          ${icon('person', 36)}
        </div>
        <div style="min-width:0;flex:1;">
          <h2 class="student-name">${esc(s.parentSampleStudentName)}</h2>
          <div class="student-meta">
            <span style="color:#EABF16;display:inline-flex;">${icon('school', 16)}</span>
            <span>${esc(s.parentSampleClassSectionValue)}</span>
          </div>
          <span class="student-year-pill">${esc(s.parentAcademicYearLabel)}: ${esc(s.parentSampleAcademicYearValue)}</span>
        </div>
      </div>
    </section>

    <!-- SECTION C — YOUR CHILD AT A GLANCE -->
    <section data-testid="parent_glance_section">
      <div class="section-header"><h2 class="section-title">${esc(s.parentGlanceSectionTitle)}</h2></div>
      <div class="two-col-responsive">
        <button type="button" class="card interactive" style="text-align:left;" data-route="parent/module/attendance" data-testid="parent_glance_attendance_card">
          <div class="stack-sm">
            <div class="row-between">
              <span class="icon-tile tone-forest">${icon('factCheck', 22)}</span>
              <span class="muted">${icon('chevronRight', 18)}</span>
            </div>
            <strong>${esc(s.parentAttendanceOverviewTitle)}</strong>
            <p class="muted text-sm" style="margin:0;">${esc(s.parentAttendanceOverviewEmpty)}</p>
            <span class="badge badge-gold" style="align-self:flex-start;">${esc(s.parentAwaitingStudentLinkBadge)}</span>
          </div>
        </button>

        <button type="button" class="card interactive" style="text-align:left;" data-route="parent/module/exams_results" data-testid="parent_glance_exams_card">
          <div class="stack-sm">
            <div class="row-between">
              <span class="icon-tile tone-burgundy">${icon('award', 22)}</span>
              <span class="muted">${icon('chevronRight', 18)}</span>
            </div>
            <strong>${esc(s.parentExamResultsOverviewTitle)}</strong>
            <p class="muted text-sm" style="margin:0;">${esc(s.parentExamResultsOverviewEmpty)}</p>
            <span class="badge badge-gold" style="align-self:flex-start;">${esc(s.parentAwaitingStudentLinkBadge)}</span>
          </div>
        </button>
      </div>
    </section>

    <!-- SECTION D — LATEST SCHOOL UPDATES -->
    <section data-testid="parent_latest_updates_section">
      <div class="section-header">
        <h2 class="section-title">${esc(s.parentLatestUpdatesTitle)}</h2>
        <button type="button" class="btn btn-text" data-route="updates" data-testid="parent_updates_view_all">
          ${esc(s.viewAll)} ${icon('arrowForward', 16)}
        </button>
      </div>
      <div class="stack-sm">
        ${recentAnnouncements.map((ann) => `
          <article class="card burgundy-border interactive" data-action="open-announcement" data-id="${ann.id}">
            <div class="row-between" style="margin-bottom:4px;">
              <span class="badge badge-burgundy-soft">${esc(s.parentUpdateTypeAnnouncement)}</span>
              <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
            </div>
            <strong>${esc(ann.title)}</strong>
            <p class="muted text-sm" style="margin:4px 0 0;">${esc(ann.message)}</p>
          </article>
        `).join('')}
        ${recentNews.map((n) => `
          <article class="card interactive" data-action="open-news" data-id="${n.id}">
            <div class="row-between" style="margin-bottom:4px;">
              <span class="badge badge-gold">${esc(s.parentUpdateTypeGeneral)}</span>
              <span class="muted text-xs">${esc(n.publishedDateText)}</span>
            </div>
            <strong>${esc(n.title)}</strong>
            <p class="muted text-sm" style="margin:4px 0 0;">${esc(n.summary)}</p>
          </article>
        `).join('')}
        ${recentAnnouncements.length === 0 && recentNews.length === 0 ? `
          <div class="card">
            <span class="badge badge-burgundy-soft" style="margin-bottom:6px;">${esc(s.parentUpdateTypeAnnouncement)}</span>
            <p class="muted text-sm" style="margin:0;">${esc(s.parentSchoolUpdatesEmptyNotice)}</p>
          </div>
        ` : ''}
        <button type="button" class="card interactive" style="text-align:left;" data-route="parent/module/homework_assignments">
          <div class="row-between" style="margin-bottom:4px;">
            <span class="badge badge-forest">${esc(s.parentUpdateTypeHomework)}</span>
            <span class="muted">${icon('chevronRight', 18)}</span>
          </div>
          <p class="muted text-sm" style="margin:0;">${esc(s.parentHomeworkEmptyNotice)}</p>
        </button>
      </div>
    </section>

    <!-- SECTION E — PARENT SERVICES (ALL 11 MODULES) -->
    <section data-testid="parent_services_section">
      <div class="section-header"><h2 class="section-title">${esc(s.parentServicesSectionTitle)}</h2></div>
      ${filteredModules.length === 0
        ? `<div class="card" style="text-align:center;" data-testid="parent_search_empty_state"><p class="muted" style="margin:8px 0;">${esc(s.parentSearchNoResults)}</p></div>`
        : `
          <div class="parent-modules-grid">
            ${filteredModules.map((mod) => `
              <button
                type="button"
                class="parent-module-card"
                data-route="parent/module/${mod.key}"
                data-testid="parent_module_card_${mod.key}"
              >
                <div class="row-between" style="width:100%;">
                  <span class="icon-tile tone-${mod.tone}">${icon(mod.icon, 22)}</span>
                  <span class="muted">${icon('chevronRight', 18)}</span>
                </div>
                <p class="parent-module-title">${esc(s[mod.titleKey])}</p>
              </button>
            `).join('')}
          </div>
        `}
    </section>

    <!-- SECTION F — FOOTER -->
    ${renderFooter(s)}
  `;
}

export function renderParentModuleDetailView(state, s, moduleKey) {
  const mod = PARENT_MODULES.find((m) => m.key === moduleKey) || PARENT_MODULES[0];
  const { profile, announcements } = state;

  let liveSection = '';
  if (mod.key === 'emergency_info') {
    liveSection = `
      <div class="card">
        <h3 style="margin:0 0 8px;font-family:var(--font-serif);">${esc(s.parentModuleEmergencySchoolOfficeTitle)}</h3>
        ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
        ${renderVerifiedField(s.phoneLabel, profile.phone, 'contact', s)}
        ${renderVerifiedField(s.emailLabel, profile.email, 'email', s)}
        <div class="row-wrap" style="margin-top:12px;">
          ${profile.phone ? `<a href="tel:${esc(profile.phone)}" class="btn btn-primary">${icon('contact', 18)} ${esc(s.callActionLabel)}</a>` : ''}
          <button type="button" class="btn btn-outline" data-route="contact">${esc(s.parentModuleOpenContactBtn)}</button>
        </div>
        <p class="muted text-sm" style="margin:14px 0 0;">${esc(s.parentModuleEmergencyStudentNotice)}</p>
      </div>
    `;
  } else if (mod.key === 'school_circulars' || mod.key === 'school_communication') {
    liveSection = `
      <div class="card">
        <div class="row-between" style="margin-bottom:10px;">
          <h3 style="margin:0;font-family:var(--font-serif);font-size:1.05rem;">${esc(s.parentModuleCircularsLiveHeader)}</h3>
          <button type="button" class="btn btn-text" data-route="updates">${esc(s.parentModuleOpenAnnouncementsBtn)}</button>
        </div>
        ${announcements.length === 0
          ? `<p class="muted text-sm" style="margin:0;">${esc(s.announcementsPendingDesc)}</p>`
          : `<div class="stack-sm">${announcements.slice(0, 5).map((ann) => `
              <div style="padding:10px 0;border-bottom:1px solid var(--border-subtle);">
                <div class="row-between">
                  <strong>${esc(ann.title)}</strong>
                  <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
                </div>
                <p class="muted text-sm" style="margin:4px 0 0;">${esc(ann.message)}</p>
              </div>
            `).join('')}</div>`}
      </div>
    `;
  }

  return `
    <div class="card gold-border" data-testid="parent_module_detail_${mod.key}">
      <div class="row-wrap" style="align-items:flex-start;margin-bottom:12px;">
        <span class="icon-tile tone-${mod.tone}" style="width:52px;height:52px;">${icon(mod.icon, 28)}</span>
        <div style="flex:1;min-width:200px;">
          <span class="badge badge-gold" style="margin-bottom:6px;">${esc(s.parentModulePlaceholderBadge)}</span>
          <h2 class="section-title" style="margin:0;">${esc(s[mod.titleKey])}</h2>
        </div>
      </div>
      <p class="muted" style="margin:0;">${esc(s[mod.descKey])}</p>
    </div>

    ${liveSection}

    <button type="button" class="btn btn-outline btn-full" data-route="parent" data-testid="parent_module_back_dashboard_btn">
      ${icon('back', 18)} ${esc(s.parentModuleReturnDashboardBtn)}
    </button>

    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   ACADEMICS, COURSES, ABOUT, FACILITIES, TRANSPORT, UPDATES, GALLERY,
   CONTACT, SOCIAL MEDIA, SETTINGS
   ========================================================================== */
export function renderAcademicsView(state, s) {
  return `
    ${renderSchoolBrandBanner(state.profile, s)}
    <div class="card gold-border">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div>
          <h2 class="section-title">${esc(s.titleHigherSecondaryCourses)}</h2>
          <p class="muted text-sm" style="margin:4px 0 0;">${esc(s.coursesOverviewBanner)}</p>
        </div>
        <button type="button" class="btn btn-primary" data-route="courses">
          ${icon('book', 18)} ${esc(s.exploreHigherSecondaryBtn)}
        </button>
      </div>
    </div>

    <div class="section-header"><h2 class="section-title">${esc(s.academicOverviewTitle)}</h2></div>
    ${state.academics.length === 0
      ? renderAwaitingCard(s.academicOverviewTitle, s.academicPendingDesc, 'school', s)
      : `<div class="two-col-responsive">${state.academics.map((a) => `
          <article class="card">
            <span class="badge badge-forest" style="margin-bottom:8px;">${esc(a.level || 'Academic')}</span>
            <h3 style="margin:0 0 6px;">${esc(a.title)}</h3>
            <p class="muted text-sm" style="margin:0;">${esc(a.description)}</p>
          </article>
        `).join('')}</div>`}

    ${renderFooter(s)}
  `;
}

export function renderCoursesView(state, s) {
  return `
    <div class="card gold-border">
      <h2 class="section-title">${esc(s.titleHigherSecondaryCourses)}</h2>
      <p class="muted text-sm" style="margin:6px 0 0;">${esc(s.coursesOverviewBanner)}</p>
    </div>
    <div class="two-col-responsive">
      ${state.courses.map((c) => `
        <article class="card ${c.verified ? 'burgundy-border' : ''}">
          <div class="row-between" style="margin-bottom:8px;">
            <span class="badge badge-burgundy">${esc(s.streamCodeLabel)}: ${esc(c.groupCode)}</span>
            <span class="badge ${c.verified ? 'badge-forest' : 'badge-gold'}">${esc(c.verified ? 'Verified' : s.awaitingVerificationLabel)}</span>
          </div>
          <h3 style="margin:0 0 8px;font-family:var(--font-serif);">${esc(c.title)}</h3>
          ${c.subjects
            ? `
              <p class="text-sm" style="margin:0 0 6px;"><strong>${esc(s.subjectsLabel)}:</strong> ${esc(c.subjects)}</p>
              ${c.medium ? `<p class="text-sm muted" style="margin:0 0 4px;"><strong>${esc(s.mediumLabel)}:</strong> ${esc(c.medium)}</p>` : ''}
              ${c.eligibility ? `<p class="text-sm muted" style="margin:0;"><strong>${esc(s.eligibilityLabel)}:</strong> ${esc(c.eligibility)}</p>` : ''}
            `
            : `<p class="muted text-sm" style="margin:0;">${esc(s.coursesPendingDesc)}</p>`}
        </article>
      `).join('')}
    </div>
    ${renderFooter(s)}
  `;
}

export function renderAboutView(state, s) {
  const { profile } = state;
  return `
    ${renderSchoolBrandBanner(profile, s)}
    <div class="card">
      ${renderVerifiedField(s.principalLabel, profile.principalName, 'person', s)}
      ${renderVerifiedField(s.historyLabel, profile.history, 'info', s)}
      ${renderVerifiedField(s.visionLabel, profile.vision, 'visibility', s)}
      ${renderVerifiedField(s.missionLabel, profile.mission, 'award', s)}
      ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
    </div>
    ${renderFooter(s)}
  `;
}

export function renderFacilitiesView(state, s) {
  return `
    <div class="section-header"><h2 class="section-title">${esc(s.titleFacilities)}</h2></div>
    ${state.facilities.length === 0
      ? renderAwaitingCard(s.titleFacilities, s.facilitiesPendingDesc, 'apartment', s)
      : `<div class="two-col-responsive">${state.facilities.map((f) => `
          <article class="card">
            ${f.imageUrl ? `<img src="${esc(f.imageUrl)}" alt="${esc(f.name)}" style="width:100%;height:160px;object-fit:cover;border-radius:12px;margin-bottom:10px;" />` : ''}
            <span class="badge badge-gold" style="margin-bottom:6px;">${esc(f.category || 'Campus')}</span>
            <h3 style="margin:0 0 6px;">${esc(f.name)}</h3>
            <p class="muted text-sm" style="margin:0;">${esc(f.description)}</p>
          </article>
        `).join('')}</div>`}
    ${renderFooter(s)}
  `;
}

export function renderTransportView(state, s) {
  return `
    <div class="section-header"><h2 class="section-title">${esc(s.titleTransport)}</h2></div>
    ${state.transportRoutes.length === 0
      ? renderAwaitingCard(s.titleTransport, s.transportPendingDesc, 'bus', s)
      : `<div class="two-col-responsive">${state.transportRoutes.map((r) => `
          <article class="card">
            <span class="badge badge-burgundy" style="margin-bottom:8px;">${esc(s.routeNumberPrefix)} ${esc(r.routeNumber)}</span>
            <h3 style="margin:0 0 6px;">${esc(r.routeName)}</h3>
            <p class="text-sm" style="margin:0 0 6px;">${esc(r.stops)}</p>
            ${r.timings ? `<p class="muted text-xs" style="margin:0 0 4px;"><strong>${esc(s.pickupTimingLabel)}:</strong> ${esc(r.timings)}</p>` : ''}
            ${r.coordinator ? `<p class="muted text-xs" style="margin:0;"><strong>${esc(s.coordinatorLabel)}:</strong> ${esc(r.coordinator)}</p>` : ''}
          </article>
        `).join('')}</div>`}
    ${renderFooter(s)}
  `;
}

export function renderUpdatesView(state, s, ui) {
  const tab = ui.updatesTab || 'all';
  const showNews = tab === 'all' || tab === 'news';
  const showAnn = tab === 'all' || tab === 'announcements';
  const empty = (!showNews || state.news.length === 0) && (!showAnn || state.announcements.length === 0);

  return `
    <div class="tabs-bar" role="tablist">
      <button type="button" class="tab-btn ${tab === 'all' ? 'active' : ''}" data-action="updates-tab" data-tab="all">${esc(s.tabAllUpdates)}</button>
      <button type="button" class="tab-btn ${tab === 'news' ? 'active' : ''}" data-action="updates-tab" data-tab="news">${esc(s.tabNews)}</button>
      <button type="button" class="tab-btn ${tab === 'announcements' ? 'active' : ''}" data-action="updates-tab" data-tab="announcements">${esc(s.tabAnnouncements)}</button>
    </div>

    ${empty
      ? renderAwaitingCard(s.titleNewsAnnouncements, tab === 'news' ? s.newsPendingDesc : s.announcementsPendingDesc, 'campaign', s)
      : `<div class="stack-sm">
          ${showAnn ? state.announcements.map((ann) => `
            <article class="card burgundy-border interactive" data-action="open-announcement" data-id="${ann.id}">
              <div class="row-between" style="margin-bottom:6px;">
                <span class="badge badge-burgundy-soft">${esc(ann.priorityLabel || s.tabAnnouncements)}</span>
                <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
              </div>
              <h3 style="margin:0 0 4px;font-size:1rem;">${esc(ann.title)}</h3>
              <p class="muted text-sm" style="margin:0;">${esc(ann.message)}</p>
            </article>
          `).join('') : ''}
          ${showNews ? state.news.map((item) => `
            <article class="card interactive" data-action="open-news" data-id="${item.id}">
              <div class="row-between" style="margin-bottom:6px;">
                <span class="badge badge-gold">${esc(item.isFeatured ? s.featuredNewsBadge : s.tabNews)}</span>
                <span class="muted text-xs">${esc(item.publishedDateText)}</span>
              </div>
              <h3 style="margin:0 0 4px;font-size:1rem;">${esc(item.title)}</h3>
              <p class="muted text-sm" style="margin:0;">${esc(item.summary)}</p>
            </article>
          `).join('') : ''}
        </div>`}
    ${renderFooter(s)}
  `;
}

export function renderGalleryView(state, s, ui) {
  const tab = ui.galleryTab || 'photos';
  const cat = ui.galleryCategory || 'all';
  const categories = ['all', ...Array.from(new Set(state.galleryMedia.map((m) => m.category || 'Campus')))];
  const filtered = cat === 'all'
    ? state.galleryMedia
    : state.galleryMedia.filter((m) => (m.category || 'Campus') === cat);

  return `
    <div class="tabs-bar" role="tablist">
      <button type="button" class="tab-btn ${tab === 'photos' ? 'active' : ''}" data-action="gallery-tab" data-tab="photos">${esc(s.allPhotosTab)}</button>
      <button type="button" class="tab-btn ${tab === 'albums' ? 'active' : ''}" data-action="gallery-tab" data-tab="albums">${esc(s.albumsTab)}</button>
    </div>

    ${categories.length > 1 ? `
      <div class="chip-row">
        ${categories.map((c) => `
          <button type="button" class="chip ${cat === c ? 'selected' : ''}" data-action="gallery-cat" data-cat="${esc(c)}">
            ${esc(c === 'all' ? s.allCategoriesLabel : c)}
          </button>
        `).join('')}
      </div>
    ` : ''}

    ${filtered.length === 0
      ? renderAwaitingCard(s.titleGallery, s.galleryPendingDesc, 'gallery', s)
      : tab === 'albums'
        ? `<div class="two-col-responsive">${Array.from(new Set(filtered.map((m) => m.album || 'Campus Highlights'))).map((albumName) => {
            const items = filtered.filter((m) => (m.album || 'Campus Highlights') === albumName);
            const cover = items[0];
            return `
              <button type="button" class="card interactive" style="text-align:left;padding:12px;" data-action="open-lightbox" data-index="${state.galleryMedia.indexOf(cover)}">
                <img src="${esc(cover.imageUrl)}" alt="${esc(albumName)}" style="width:100%;aspect-ratio:16/10;object-fit:cover;border-radius:12px;margin-bottom:10px;" />
                <strong>${esc(albumName)}</strong>
                <div class="muted text-xs">${items.length} ${esc(s.photosCountSuffix)}</div>
              </button>
            `;
          }).join('')}</div>`
        : `<div class="gallery-grid">${filtered.map((m) => {
            const globalIdx = state.galleryMedia.indexOf(m);
            const isYt = Boolean(m.youtubeUrl);
            return `
              <button type="button" class="gallery-tile" data-action="open-lightbox" data-index="${globalIdx}">
                <img src="${esc(m.imageUrl)}" alt="${esc(m.caption || s.titleGallery)}" />
                ${isYt ? `<span class="badge badge-burgundy" style="position:absolute;top:8px;left:8px;">${icon('play', 14)} YouTube</span>` : ''}
                ${m.caption ? `<div class="gallery-tile-caption">${esc(m.caption)}</div>` : ''}
              </button>
            `;
          }).join('')}</div>`}

    ${renderFooter(s)}
  `;
}

export function renderContactView(state, s) {
  const { profile } = state;
  return `
    ${renderSchoolBrandBanner(profile, s)}
    <div class="card">
      ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
      ${renderVerifiedField(s.phoneLabel, profile.phone, 'contact', s)}
      ${renderVerifiedField(s.emailLabel, profile.email, 'email', s)}
      ${renderVerifiedField(s.websiteLabel, profile.website, 'language', s)}
      ${renderVerifiedField(s.officeHoursLabel, profile.officeHours, 'calendar', s)}
      <div class="row-wrap" style="margin-top:16px;">
        <a href="${esc(googleMapsUrl(profile.mapCoordinatesOrQuery || profile.addressLine))}" target="_blank" rel="noopener noreferrer" class="btn btn-forest" style="flex:1;">
          ${icon('map', 18)} ${esc(s.openMapsActionLabel)}
        </a>
        ${profile.phone ? `<a href="tel:${esc(profile.phone)}" class="btn btn-primary">${icon('contact', 18)} ${esc(s.callActionLabel)}</a>` : ''}
        ${profile.email ? `<a href="mailto:${esc(profile.email)}" class="btn btn-outline">${icon('email', 18)} ${esc(s.emailActionLabel)}</a>` : ''}
      </div>
    </div>
    <button type="button" class="btn btn-outline btn-full" data-route="social">
      ${icon('share', 18)} ${esc(s.officialChannelsTitle)}
    </button>
    ${renderFooter(s)}
  `;
}

export function renderSocialMediaView(state, s) {
  return `
    <div class="section-header"><h2 class="section-title">${esc(s.officialChannelsTitle)}</h2></div>
    <div class="two-col-responsive">
      ${state.socialLinks.map((link) => `
        <div class="card">
          <div class="row-between" style="margin-bottom:8px;">
            <strong>${esc(link.platformName)}</strong>
            <span class="badge ${link.active && link.url ? 'badge-forest' : 'badge-gold'}">
              ${esc(link.active && link.url ? 'Verified' : s.awaitingVerificationLabel)}
            </span>
          </div>
          ${link.handle ? `<p class="muted text-sm" style="margin:0 0 10px;">${esc(link.handle)}</p>` : `<p class="muted text-sm" style="margin:0 0 10px;">${esc(s.socialPendingDesc)}</p>`}
          ${link.active && link.url
            ? `<a href="${esc(safeExternalUrl(link.url))}" target="_blank" rel="noopener noreferrer" class="btn btn-outline btn-full">${esc(s.openLink)} ${icon('openInNew', 16)}</a>`
            : ''}
        </div>
      `).join('')}
    </div>
    ${renderFooter(s)}
  `;
}

export function renderSettingsView(state, s) {
  const { language, theme } = state.preferences;
  return `
    <div class="card">
      <h2 class="section-title" style="margin-bottom:4px;">${esc(s.languageSettingTitle)}</h2>
      <p class="muted text-sm" style="margin:0 0 14px;">${esc(s.languageSettingDesc)}</p>
      <div class="chip-row">
        <button type="button" class="chip ${language === 'en' ? 'selected' : ''}" data-action="set-language" data-lang="en">English</button>
        <button type="button" class="chip ${language === 'ta' ? 'selected' : ''}" data-action="set-language" data-lang="ta">தமிழ் (Tamil)</button>
        <button type="button" class="chip ${language === 'hi' ? 'selected' : ''}" data-action="set-language" data-lang="hi">हिन्दी (Hindi)</button>
      </div>
    </div>

    <div class="card">
      <h2 class="section-title" style="margin-bottom:4px;">${esc(s.themeSettingTitle)}</h2>
      <p class="muted text-sm" style="margin:0 0 14px;">${esc(s.themeSettingDesc)}</p>
      <div class="chip-row">
        <button type="button" class="chip ${theme === 'system' ? 'selected' : ''}" data-action="set-theme" data-theme="system">${esc(s.themeSystemDefault)}</button>
        <button type="button" class="chip ${theme === 'light' ? 'selected' : ''}" data-action="set-theme" data-theme="light">${esc(s.themeLightMode)}</button>
        <button type="button" class="chip ${theme === 'dark' ? 'selected' : ''}" data-action="set-theme" data-theme="dark">${esc(s.themeDarkMode)}</button>
      </div>
    </div>

    <div class="card">
      <h2 class="section-title" style="margin-bottom:8px;">${esc(s.appInfoSectionTitle)}</h2>
      ${renderVerifiedField(s.appVersionLabel, '1.0 (Unified Android + Responsive Web)', 'info', s)}
      ${renderVerifiedField(s.storageStatusLabel, s.storageStatusValue, 'security', s)}
      ${renderVerifiedField(s.demoNoticeLabel, s.demoNoticeValue, 'award', s)}
    </div>

    ${renderFooter(s)}
  `;
}
