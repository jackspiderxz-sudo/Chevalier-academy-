/**
 * Public & Parent Portal Views for Chevalier Academy Responsive Website.
 * Mirrors Android HomeSocialFeedScreen.kt, HomeSocialFeedComponents.kt,
 * AcademyPageScreen.kt, AcademyCategoryDetailScreen.kt,
 * PrimaryTabScreens.kt, SecondaryDetailScreens.kt,
 * UpdatesAndSettingsScreens.kt, and ParentDashboardScreens.kt.
 */

import { icon } from './icons.js';
import { PARENT_MODULES, ACADEMICS_V2_CATEGORIES, ACADEMY_12_CARDS } from './i18n.js';
import { SCHOOL_POST_TYPES, SCHOOL_POST_STATUS, SCHOOL_POST_CATEGORIES, HIGHLIGHT_TTL_MILLIS } from './store.js';
import { parseYouTubeUrl } from './mediaUtils.js';

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
   Social Feed & 24-Hour Highlights Helpers (Mirrors HomeSocialFeedComponents.kt)
   ========================================================================== */
export function formatHighlightRemainingLabel(expiresAtMillis, nowMillis = Date.now()) {
  const remainingMs = Math.max(0, Number(expiresAtMillis || 0) - nowMillis);
  const hours = Math.max(1, Math.floor(remainingMs / (1000 * 60 * 60)));
  return `${hours}h`;
}

export function formatPostTimestamp(millis) {
  const ts = Number(millis || 0);
  if (!ts) return '';
  try {
    return new Date(ts).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  } catch {
    return '';
  }
}

export function getPublishedPostsFromState(state) {
  return [...(state.schoolPosts || [])]
    .filter((p) => p && p.status === SCHOOL_POST_STATUS.PUBLISHED)
    .sort((a, b) => {
      if (Boolean(b.isFeatured) !== Boolean(a.isFeatured)) return b.isFeatured ? 1 : -1;
      return (b.publishedAtMillis ?? b.createdAtMillis ?? 0) - (a.publishedAtMillis ?? a.createdAtMillis ?? 0);
    });
}

export function getActiveHighlightsFromState(state, nowMillis = Date.now()) {
  return [...(state.schoolHighlights || [])]
    .filter((h) => h && h.status === SCHOOL_POST_STATUS.PUBLISHED && Number(h.expiresAtMillis || (Number(h.publishedAtMillis || 0) + HIGHLIGHT_TTL_MILLIS)) > nowMillis)
    .sort((a, b) => Number(b.publishedAtMillis || 0) - Number(a.publishedAtMillis || 0));
}

export function isPostLikedInState(state, post) {
  const liked = new Set((state.preferences?.likedPostIds || []).map(String));
  return liked.has(String(post.id)) || (post.remoteId && liked.has(String(post.remoteId)));
}

export function isPostBookmarkedInState(state, post) {
  const saved = new Set((state.preferences?.bookmarkedPostIds || []).map(String));
  return saved.has(String(post.id)) || (post.remoteId && saved.has(String(post.remoteId)));
}

export function renderHighlightCard(highlight, s, nowMillis = Date.now()) {
  const parsed = parseYouTubeUrl(highlight.youtubeShortsUrl);
  const thumbUrl = parsed?.thumbnailUrl || '';
  const remaining = formatHighlightRemainingLabel(highlight.expiresAtMillis, nowMillis);
  const titleText = highlight.title || s.socialHighlightsTitle || 'School Highlight';

  return `
    <button
      type="button"
      class="highlight-short-card"
      data-action="play-youtube"
      data-url="${esc(highlight.youtubeShortsUrl)}"
      data-title="${esc(titleText)}"
      data-testid="highlight_card_${esc(highlight.id)}"
    >
      ${thumbUrl ? `<img src="${esc(thumbUrl)}" alt="${esc(titleText)}" class="highlight-short-thumb" />` : '<div class="highlight-short-thumb"></div>'}
      <div class="highlight-short-overlay">
        <div class="row-between" style="width:100%;">
          <span class="badge badge-burgundy" style="font-size:0.68rem;padding:2px 7px;">Shorts</span>
          <span class="badge badge-gold" style="font-size:0.68rem;padding:2px 7px;color:#FFF9E8;background:rgba(0,0,0,0.55);">${esc(remaining)}</span>
        </div>
        <div class="highlight-play-pill">${icon('play', 22)}</div>
        <div class="highlight-short-title">${esc(titleText)}</div>
      </div>
    </button>
  `;
}

export function renderHighlightMoreCard(totalCount, s) {
  return `
    <button
      type="button"
      class="highlight-short-card highlight-more-card"
      data-action="open-all-highlights"
      data-testid="highlight_more_card"
    >
      <div class="highlight-more-inner">
        <div class="highlight-play-pill">${icon('video', 22)}</div>
        <strong style="font-size:0.88rem;color:#FFF9E8;">${esc(s.socialMoreLabel || 'More')}</strong>
        <span style="font-size:0.74rem;color:#EABF16;font-weight:700;">${totalCount} active</span>
      </div>
    </button>
  `;
}

export function renderSchoolPostCard(post, state, s, options = {}) {
  const { showEngagementActions = true, activeVideoPostId = null } = options;
  const isLiked = isPostLikedInState(state, post);
  const isBookmarked = isPostBookmarkedInState(state, post);
  const dateText = formatPostTimestamp(post.publishedAtMillis || post.createdAtMillis);
  const isPlayingInline = activeVideoPostId !== null && String(activeVideoPostId) === String(post.id);

  let mediaHtml = '';
  if (post.postType === SCHOOL_POST_TYPES.YOUTUBE_DIRECT || post.postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO) {
    const parsed = parseYouTubeUrl(post.youtubeUrl);
    const thumbUrl =
      post.postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO && post.customThumbnailUrl
        ? post.customThumbnailUrl
        : (parsed?.thumbnailUrl || '');
    const externalUrl = parsed?.externalFallbackUrl || safeExternalUrl(post.youtubeUrl);

    if (isPlayingInline && parsed) {
      mediaHtml = `
        <div class="post-video-stage" data-testid="post_inline_player_${esc(post.id)}">
          <iframe
            src="${esc(parsed.clickToPlayEmbedUrl)}"
            title="${esc(post.title)}"
            class="youtube-embed-iframe"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowfullscreen
          ></iframe>
        </div>
        <div class="row-between" style="margin-top:8px;">
          <span class="badge badge-burgundy-soft">${icon('video', 14)} ${esc(s.youtubeVideoLabel)}</span>
          ${externalUrl ? `<a href="${esc(externalUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-text" style="min-height:36px;font-size:0.82rem;" data-testid="post_youtube_fallback_${esc(post.id)}">${esc(s.openYouTubeVideoLabel)} ${icon('openInNew', 14)}</a>` : ''}
        </div>
      `;
    } else {
      mediaHtml = `
        <div class="post-video-preview-wrap">
          <button
            type="button"
            class="post-video-preview"
            data-action="play-post-video"
            data-post-id="${esc(post.id)}"
            data-url="${esc(post.youtubeUrl || '')}"
            data-title="${esc(post.title)}"
            data-testid="post_play_btn_${esc(post.id)}"
          >
            ${thumbUrl ? `<img src="${esc(thumbUrl)}" alt="${esc(post.title)}" class="post-video-thumb" />` : '<div class="post-video-thumb"></div>'}
            <div class="post-video-play-overlay">
              <span class="post-video-play-circle">${icon('play', 28)}</span>
            </div>
            <span class="badge badge-burgundy" style="position:absolute;top:10px;left:10px;">
              ${icon('play', 14)} ${esc(post.postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO ? 'School Video' : 'YouTube')}
            </span>
          </button>
          ${externalUrl ? `
            <div class="row-between" style="margin-top:6px;">
              <span class="muted text-xs">${esc(s.youtubeVideoLabel)}</span>
              <a href="${esc(externalUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-text" style="min-height:34px;font-size:0.8rem;" data-testid="post_youtube_fallback_${esc(post.id)}">
                ${esc(s.openYouTubeVideoLabel)} ${icon('openInNew', 14)}
              </a>
            </div>
          ` : ''}
        </div>
      `;
    }
  } else if (post.postType === SCHOOL_POST_TYPES.PHOTO && Array.isArray(post.photoUrls) && post.photoUrls.length > 0) {
    const photos = post.photoUrls;
    if (photos.length === 1) {
      mediaHtml = `
        <button
          type="button"
          class="post-single-photo-btn"
          data-action="open-post-lightbox"
          data-post-id="${esc(post.id)}"
          data-index="0"
          data-testid="post_photo_${esc(post.id)}_0"
        >
          <img src="${esc(photos[0])}" alt="${esc(post.title)}" class="post-single-photo-img" />
        </button>
      `;
    } else {
      mediaHtml = `
        <div class="post-multi-photo-strip" data-testid="post_photo_strip_${esc(post.id)}">
          ${photos.map((url, idx) => `
            <button
              type="button"
              class="post-multi-photo-item"
              data-action="open-post-lightbox"
              data-post-id="${esc(post.id)}"
              data-index="${idx}"
              data-testid="post_photo_${esc(post.id)}_${idx}"
            >
              <img src="${esc(url)}" alt="${esc(post.title)} (${idx + 1}/${photos.length})" />
              <span class="post-photo-count-badge">${idx + 1}/${photos.length}</span>
            </button>
          `).join('')}
        </div>
      `;
    }
  }

  return `
    <article class="card social-post-card ${post.isFeatured ? 'gold-border' : ''}" data-testid="social_post_card_${esc(post.id)}">
      <div class="social-post-header">
        <img src="./assets/chevalier_emblem.png" alt="${esc(state.profile.shortName || s.shortSchoolName)}" class="social-post-avatar" />
        <div style="flex:1;min-width:0;">
          <div class="row-wrap" style="gap:6px;">
            <strong style="font-size:0.94rem;">${esc(state.profile.shortName || s.shortSchoolName)}</strong>
            <span style="color:var(--brand-forest);display:inline-flex;">${icon('verified', 16)}</span>
          </div>
          <div class="row-wrap" style="gap:6px;margin-top:2px;">
            <span class="badge badge-burgundy-soft" style="font-size:0.7rem;padding:2px 8px;">${esc(post.category || 'General')}</span>
            ${dateText ? `<span class="muted text-xs">· ${esc(dateText)}</span>` : ''}
          </div>
        </div>
        ${post.isFeatured ? `
          <span class="badge badge-gold" data-testid="post_pinned_badge_${esc(post.id)}">
            ${icon('pin', 14)} ${esc(s.socialPinnedBadge || 'Pinned')}
          </span>
        ` : ''}
      </div>

      <div class="social-post-body">
        <h3 class="social-post-title">${esc(post.title)}</h3>
        ${post.description ? `<p class="social-post-desc">${esc(post.description)}</p>` : ''}
      </div>

      ${mediaHtml ? `<div class="social-post-media">${mediaHtml}</div>` : ''}

      ${showEngagementActions ? `
        <div class="social-post-actions">
          <button
            type="button"
            class="social-action-btn ${isLiked ? 'liked' : ''}"
            data-action="toggle-like-post"
            data-post-id="${esc(post.id)}"
            aria-pressed="${isLiked}"
            data-testid="post_like_btn_${esc(post.id)}"
          >
            ${icon(isLiked ? 'heartFilled' : 'heart', 20)}
            <span>${esc(isLiked ? (s.socialLikedLabel || 'Liked') : (s.socialLikeLabel || 'Like'))}</span>
          </button>

          <button
            type="button"
            class="social-action-btn ${isBookmarked ? 'bookmarked' : ''}"
            data-action="toggle-bookmark-post"
            data-post-id="${esc(post.id)}"
            aria-pressed="${isBookmarked}"
            data-testid="post_bookmark_btn_${esc(post.id)}"
          >
            ${icon(isBookmarked ? 'bookmarkFilled' : 'bookmark', 20)}
            <span>${esc(isBookmarked ? (s.socialBookmarkedLabel || 'Saved') : (s.socialBookmarkLabel || 'Bookmark'))}</span>
          </button>
        </div>
      ` : ''}
    </article>
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
   HOME SCREEN — Complete Android HomeSocialFeedScreen Parity + Quick Sections
   ========================================================================== */
export function renderHomeView(state, s, ui = {}) {
  const { profile, heroPhotos, news, announcements, galleryMedia } = state;
  const activeSlide = heroPhotos.length > 0 ? Math.min(ui.heroSlideIndex || 0, heroPhotos.length - 1) : 0;

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

  // Section 2: 24-Hour School Highlights (YouTube Shorts)
  const activeHighlights = getActiveHighlightsFromState(state);
  const visibleHighlights = activeHighlights.slice(0, 4);
  const highlightsHtml = activeHighlights.length === 0
    ? `
      <div class="card" data-testid="home_highlights_empty_card">
        <div class="row-wrap" style="align-items:center;">
          <span class="icon-tile tone-burgundy">${icon('video', 22)}</span>
          <div style="flex:1;min-width:200px;">
            <strong style="display:block;font-size:0.95rem;">${esc(s.socialHighlightsEmptyTitle || '24-Hour School Highlights')}</strong>
            <p class="muted text-xs" style="margin:2px 0 0;">${esc(s.socialHighlightsEmptyDesc || 'Official short video highlights published by the school appear here for 24 hours.')}</p>
          </div>
        </div>
      </div>
    `
    : `
      <div class="highlights-strip" data-testid="home_highlights_row">
        ${visibleHighlights.map((hl) => renderHighlightCard(hl, s)).join('')}
        ${renderHighlightMoreCard(activeHighlights.length, s)}
      </div>
    `;

  // Section 3: School Social Feed / Timeline with Category Filter Chips + Likes + Bookmarks
  const publishedPosts = getPublishedPostsFromState(state);
  const selectedCategory = ui.selectedFeedCategory || null;
  const availableCategories = Array.from(
    new Set([...SCHOOL_POST_CATEGORIES, ...publishedPosts.map((p) => p.category).filter(Boolean)])
  );
  const filteredPosts = !selectedCategory
    ? publishedPosts
    : publishedPosts.filter((p) => String(p.category || '').toLowerCase() === String(selectedCategory).toLowerCase());

  const socialFeedHtml = `
    <div class="chip-row" style="margin-bottom:14px;" data-testid="home_feed_category_row">
      <button
        type="button"
        class="chip ${!selectedCategory ? 'selected' : ''}"
        data-action="feed-category"
        data-category=""
        data-testid="feed_filter_all"
      >
        ${esc(s.socialAllPostsFilter || 'All Posts')}
      </button>
      ${availableCategories.map((cat) => {
        const isSel = selectedCategory && String(selectedCategory).toLowerCase() === String(cat).toLowerCase();
        const slug = String(cat).toLowerCase().replace(/\s+/g, '_');
        return `
          <button
            type="button"
            class="chip ${isSel ? 'selected' : ''}"
            data-action="feed-category"
            data-category="${esc(cat)}"
            data-testid="feed_filter_${esc(slug)}"
          >
            ${esc(cat)}
          </button>
        `;
      }).join('')}
    </div>

    ${filteredPosts.length === 0
      ? `
        <div class="card" data-testid="home_social_feed_empty_card">
          <div class="row-wrap" style="align-items:flex-start;">
            <span class="icon-tile tone-gold">${icon('campaign', 22)}</span>
            <div class="stack-sm" style="flex:1;min-width:200px;">
              <span class="badge badge-gold" style="align-self:flex-start;">${esc(s.awaitingVerificationLabel)}</span>
              <strong>${esc(selectedCategory ? (s.socialFeedCategoryEmptyTitle || 'No Posts in Category') : (s.socialFeedEmptyTitle || 'Official School Feed Ready'))}</strong>
              <p class="muted text-sm" style="margin:0;">
                ${esc(selectedCategory ? `${s.socialFeedCategoryEmptyDesc || 'No published posts in'} "${selectedCategory}".` : (s.socialFeedEmptyDesc || 'Verified school videos, campus photo updates, and event highlights published by management will appear in this timeline.'))}
              </p>
              ${selectedCategory ? `
                <div>
                  <button type="button" class="btn btn-outline" data-action="feed-category" data-category="">
                    ${esc(s.socialShowAllPostsBtn || 'Show All Posts')}
                  </button>
                </div>
              ` : ''}
            </div>
          </div>
        </div>
      `
      : `
        <div class="stack-md" data-testid="home_social_feed_list">
          ${filteredPosts.map((post) =>
            renderSchoolPostCard(post, state, s, {
              showEngagementActions: true,
              activeVideoPostId: ui.activeVideoPostId || null
            })
          ).join('')}
        </div>
      `}
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

    <!-- SECTION 2 — 24-HOUR SCHOOL HIGHLIGHTS (YOUTUBE SHORTS) -->
    <section data-testid="home_section_highlights">
      <div class="section-header">
        <div class="row-wrap" style="gap:8px;">
          <h2 class="section-title">${esc(s.socialHighlightsTitle || 'School Highlights')}</h2>
          <span class="badge badge-burgundy">24H</span>
        </div>
        ${activeHighlights.length > 0 ? `
          <button type="button" class="btn btn-text" data-action="open-all-highlights" data-testid="home_highlights_more_btn">
            ${esc(s.socialMoreLabel || 'More')} (${activeHighlights.length})
          </button>
        ` : ''}
      </div>
      ${highlightsHtml}
    </section>

    <!-- SECTION 3 — SCHOOL SOCIAL FEED / TIMELINE -->
    <section data-testid="home_section_social_feed">
      <div class="section-header">
        <h2 class="section-title">${esc(s.socialFeedTitle || 'School Social Feed')}</h2>
        <button type="button" class="btn btn-text" data-route="bookmarks" data-testid="home_open_bookmarks_btn">
          ${icon('bookmark', 18)} ${esc(s.titleBookmarks || 'Bookmarks')}
        </button>
      </div>
      ${socialFeedHtml}
    </section>

    <!-- SECTION 4 — WELCOME / ABOUT -->
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

    <!-- SECTION 5 & 6 — UPDATES & GALLERY PREVIEW -->
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
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   BOOKMARKS SCREEN — Saved School Social Feed Posts (Mirrors BookmarksScreen)
   ========================================================================== */
export function renderBookmarksView(state, s, ui = {}) {
  const bookmarkedPosts = getPublishedPostsFromState(state).filter((p) => isPostBookmarkedInState(state, p));

  if (bookmarkedPosts.length === 0) {
    return `
      <div class="card" style="text-align:center;padding:32px 20px;" data-testid="bookmarks_empty_state">
        <div style="margin:0 auto 12px;" class="icon-tile tone-burgundy">${icon('bookmark', 26)}</div>
        <h2 class="section-title" style="margin-bottom:8px;">${esc(s.socialBookmarksEmptyTitle || 'No Saved Bookmarks Yet')}</h2>
        <p class="muted text-sm" style="max-width:420px;margin:0 auto 18px;">
          ${esc(s.socialBookmarksEmptyDesc || 'Tap the Bookmark icon on any published post in the School Social Feed to save it here for quick reading.')}
        </p>
        <button type="button" class="btn btn-primary" data-route="home" data-testid="bookmarks_explore_home_btn">
          ${esc(s.socialBrowseFeedBtn || 'Browse School Feed')}
        </button>
      </div>
      ${renderFooter(s)}
    `;
  }

  return `
    <section class="stack-md" data-testid="bookmarks_list">
      ${bookmarkedPosts.map((post) =>
        renderSchoolPostCard(post, state, s, {
          showEngagementActions: true,
          activeVideoPostId: ui.activeVideoPostId || null
        })
      ).join('')}
    </section>
    ${renderFooter(s)}
  `;
}

/* ==========================================================================
   PARENT DASHBOARD — Finalized Design + All 12 Parent Modules + Student Feed
   ========================================================================== */
export function renderParentDashboardView(state, s, ui = {}) {
  const query = (ui.parentSearchQuery || '').trim().toLowerCase();
  const filteredModules = query
    ? PARENT_MODULES.filter((m) =>
        (s[m.titleKey] || '').toLowerCase().includes(query) ||
        (s[m.descKey] || '').toLowerCase().includes(query)
      )
    : PARENT_MODULES;

  const recentAnnouncements = state.announcements.slice(0, 2);
  const recentNews = state.news.slice(0, 1);
  const studentFeedPosts = getPublishedPostsFromState(state);

  return `
    <div class="stack-md" data-testid="parent_screen_list">
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
            ${ui.parentSearchQuery ? `<button type="button" class="icon-btn" data-action="clear-parent-search" aria-label="${esc(s.parentClearSearchDesc)}" data-testid="parent_search_clear_button">${icon('close', 18)}</button>` : ''}
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
          <div class="student-avatar" data-testid="parent_student_avatar" aria-label="${esc(s.parentStudentPhotoDesc || 'Student profile photograph placeholder')}">
            ${icon('person', 36)}
          </div>
          <div style="min-width:0;flex:1;">
            <h2 class="student-name">${esc(s.parentSampleStudentName)}</h2>
            <div class="student-meta">
              <span style="color:#EABF16;display:inline-flex;">${icon('school', 16)}</span>
              <span>${esc(s.parentClassSectionLabel ? `${s.parentClassSectionLabel}: ${s.parentSampleClassSectionValue}` : s.parentSampleClassSectionValue)}</span>
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
            <article class="card burgundy-border interactive" data-action="open-announcement" data-id="${esc(ann.id)}">
              <div class="row-between" style="margin-bottom:4px;">
                <span class="badge badge-burgundy-soft">${esc(ann.priorityLabel || s.parentUpdateTypeAnnouncement)}</span>
                <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
              </div>
              <strong>${esc(ann.title)}</strong>
              <p class="muted text-sm" style="margin:4px 0 0;">${esc(ann.message)}</p>
            </article>
          `).join('')}
          ${recentNews.map((n) => `
            <article class="card interactive" data-action="open-news" data-id="${esc(n.id)}">
              <div class="row-between" style="margin-bottom:4px;">
                <span class="badge badge-gold">${esc(s.parentUpdateTypeGeneral)}</span>
                <span class="muted text-xs">${esc(n.publishedDateText)}</span>
              </div>
              <strong>${esc(n.title)}</strong>
              <p class="muted text-sm" style="margin:4px 0 0;">${esc(n.summary)}</p>
            </article>
          `).join('')}
          ${recentAnnouncements.length === 0 && recentNews.length === 0 ? `
            <button type="button" class="card interactive" style="text-align:left;" data-route="updates">
              <div class="row-between" style="margin-bottom:4px;">
                <span class="badge badge-burgundy-soft">${esc(s.parentUpdateTypeAnnouncement)}</span>
                <span class="muted">${icon('chevronRight', 18)}</span>
              </div>
              <strong style="display:block;margin-bottom:3px;">${esc(s.awaitingVerificationLabel)}</strong>
              <p class="muted text-sm" style="margin:0;">${esc(s.parentSchoolUpdatesEmptyNotice)}</p>
            </button>
          ` : ''}
          <button type="button" class="card interactive" style="text-align:left;" data-route="parent/module/homework_assignments">
            <div class="row-between" style="margin-bottom:4px;">
              <span class="badge badge-forest">${esc(s.parentUpdateTypeHomework)}</span>
              <span class="muted">${icon('chevronRight', 18)}</span>
            </div>
            <strong style="display:block;margin-bottom:3px;">${esc(s.parentAwaitingStudentLinkBadge)}</strong>
            <p class="muted text-sm" style="margin:0;">${esc(s.parentHomeworkEmptyNotice)}</p>
          </button>
        </div>
      </section>

      <!-- SECTION E — PARENT SERVICES (ALL 12 MODULES) -->
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

      <!-- SECTION E2 — STUDENT SOCIAL FEED (Read-only mode matching Android ParentDashboardScreens.kt) -->
      <section data-testid="parent_student_social_feed_section">
        <div class="section-header">
          <h2 class="section-title">${esc(s.parentStudentSocialFeedTitle || 'Student Social Feed')}</h2>
        </div>
        ${studentFeedPosts.length === 0
          ? `
            <div class="card" id="parent_student_social_feed_empty_card" data-testid="parent_social_feed_empty_card">
              <div class="row-wrap" style="align-items:center;">
                <span class="icon-tile tone-burgundy">${icon('video', 22)}</span>
                <div style="flex:1;min-width:180px;">
                  <strong style="display:block;margin-bottom:2px;">${esc(s.socialFeedEmptyTitle || 'No Published School Posts Yet')}</strong>
                  <p class="muted text-sm" style="margin:0;">${esc(s.parentStudentSocialFeedEmptyDesc || 'Published school photos, videos, and student activity updates will appear here once shared by Chevalier Academy.')}</p>
                </div>
              </div>
            </div>
          `
          : `
            <div class="stack-md" data-testid="parent_student_social_feed_list">
              ${studentFeedPosts.map((post) =>
                renderSchoolPostCard(post, state, s, {
                  showEngagementActions: false,
                  activeVideoPostId: ui.activeVideoPostId || null
                })
              ).join('')}
            </div>
          `}
      </section>

      <!-- SECTION F — FOOTER -->
      ${renderFooter(s)}
    </div>
  `;
}

export function renderParentModuleDetailView(state, s, moduleKey) {
  const mod = PARENT_MODULES.find((m) => m.key === moduleKey) || PARENT_MODULES[0];
  const { profile, announcements } = state;

  let liveSection = '';
  if (mod.key === 'emergency_info') {
    const mapQuery = profile.mapCoordinatesOrQuery || `${profile.schoolName}, ${profile.addressLine}`;
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(mapQuery)}`;
    liveSection = `
      <div class="stack-sm">
        <div class="section-header"><h3 class="section-title" style="font-size:1.05rem;">${esc(s.parentModuleEmergencySchoolOfficeTitle)}</h3></div>
        <div class="card">
          ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
          ${renderVerifiedField(s.phoneLabel, profile.phone, 'contact', s)}
          ${renderVerifiedField(s.emailLabel, profile.email, 'email', s)}
          <div class="row-wrap" style="margin-top:12px;">
            ${profile.phone ? `<a href="tel:${esc(profile.phone)}" class="btn btn-primary">${icon('contact', 18)} ${esc(s.callActionLabel)}</a>` : ''}
            <a href="${esc(mapsUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-outline-forest">${icon('location', 18)} ${esc(s.openMapsActionLabel)}</a>
          </div>
          <p class="muted text-sm" style="margin:14px 0 0;">${esc(s.parentModuleEmergencyStudentNotice)}</p>
        </div>
        <button type="button" class="btn btn-outline btn-full" data-route="contact" data-testid="parent_module_open_contact_btn">
          ${esc(s.parentModuleOpenContactBtn)}
        </button>
      </div>
    `;
  } else if (mod.key === 'school_circulars' || mod.key === 'school_communication') {
    liveSection = `
      <div class="stack-sm">
        <div class="section-header"><h3 class="section-title" style="font-size:1.05rem;">${esc(s.parentModuleCircularsLiveHeader)}</h3></div>
        <div class="card">
          ${announcements.length === 0
            ? `<p class="muted text-sm" style="margin:0;">${esc(s.announcementsPendingDesc)}</p>`
            : `<div class="stack-sm">${announcements.slice(0, 4).map((ann) => `
                <article class="card interactive" style="padding:12px;" data-action="open-announcement" data-id="${esc(ann.id)}">
                  <div class="row-between" style="margin-bottom:4px;">
                    <span class="badge badge-burgundy-soft">${esc(ann.priorityLabel || s.parentUpdateTypeAnnouncement)}</span>
                    <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
                  </div>
                  <strong>${esc(ann.title)}</strong>
                  <p class="muted text-sm" style="margin:4px 0 0;">${esc(ann.message)}</p>
                </article>
              `).join('')}</div>`}
        </div>
        <button type="button" class="btn btn-outline btn-full" data-route="updates" data-testid="parent_module_open_announcements_btn">
          ${esc(s.parentModuleOpenAnnouncementsBtn)}
        </button>
      </div>
    `;
  }

  return `
    <div class="stack-md" data-testid="parent_module_screen_${mod.key}">
      <div class="card gold-border" style="text-align:center;padding:22px 20px;" data-testid="parent_module_detail_${mod.key}">
        <div style="margin:0 auto 12px;width:60px;height:60px;border-radius:50%;" class="icon-tile tone-${mod.tone}">${icon(mod.icon, 30)}</div>
        <span class="badge badge-gold" style="margin-bottom:10px;">${esc(s.parentModulePlaceholderBadge)}</span>
        <h2 class="section-title" style="margin:0 0 8px;">${esc(s[mod.titleKey])}</h2>
        <p class="muted" style="margin:0;">${esc(s[mod.descKey])}</p>
      </div>

      ${liveSection}

      <button type="button" class="btn btn-primary btn-full" data-route="parent" data-testid="parent_module_back_dashboard_btn">
        ${icon('back', 18)} ${esc(s.parentModuleReturnDashboardBtn)}
      </button>

      ${renderFooter(s)}
    </div>
  `;
}

/* ==========================================================================
   ACADEMY / ACADEMICS HUB & 13-CATEGORY SUB-SECTION DETAIL VIEWS
   Mirrors Android AcademyPageScreen.kt & AcademyCategoryDetailScreen.kt
   ========================================================================== */
export function renderAcademicsHubView(state, s, ui = {}) {
  const query = (ui.academySearchQuery || '').trim().toLowerCase();

  // Preserve original 7 direct quick-cards with their exact data-testid attributes
  const quickSections = [
    {
      route: 'academics/overview',
      title: s.titleAcademics,
      desc: s.quickCardAcademicsDesc,
      iconName: 'school',
      tone: 'tone-burgundy',
      testId: 'academics_card_academics'
    },
    {
      route: 'courses',
      title: s.titleHigherSecondaryCourses,
      desc: s.quickCardCoursesDesc,
      iconName: 'book',
      tone: 'tone-forest',
      testId: 'academics_card_courses'
    },
    {
      route: 'about',
      title: s.titleAboutSchool,
      desc: `${s.historyLabel}, ${s.visionLabel} & ${s.missionLabel}`,
      iconName: 'info',
      tone: 'tone-gold',
      testId: 'academics_card_about'
    },
    {
      route: 'facilities',
      title: s.titleFacilities,
      desc: s.quickCardFacilitiesDesc,
      iconName: 'apartment',
      tone: 'tone-burgundy',
      testId: 'academics_card_facilities'
    },
    {
      route: 'transport',
      title: s.titleTransport,
      desc: s.quickCardTransportDesc,
      iconName: 'bus',
      tone: 'tone-forest',
      testId: 'academics_card_transport'
    },
    {
      route: 'updates',
      title: s.titleNewsAnnouncements,
      desc: s.latestUpdatesTitle,
      iconName: 'campaign',
      tone: 'tone-gold',
      testId: 'academics_card_news_announcements'
    },
    {
      route: 'gallery',
      title: s.titleGallery,
      desc: s.galleryPreviewTitle,
      iconName: 'gallery',
      tone: 'tone-burgundy',
      testId: 'academics_card_gallery'
    }
  ];

  // Ordered 12 Academy cards (3x4 grid) + 13th Admissions category matching AcademyPageScreen.kt
  const orderedCategories = [
    ...ACADEMY_12_CARDS.map((id) => ACADEMICS_V2_CATEGORIES.find((c) => c.routeId === id)).filter(Boolean),
    ...ACADEMICS_V2_CATEGORIES.filter((c) => !ACADEMY_12_CARDS.includes(c.routeId))
  ];
  const filteredCategories = query
    ? orderedCategories.filter(
        (c) =>
          c.title.toLowerCase().includes(query) ||
          c.subtitle.toLowerCase().includes(query) ||
          c.subSections.some((sub) => sub.title.toLowerCase().includes(query))
      )
    : orderedCategories;

  // Academy Social Feed (Academics, Events, Achievements)
  const academyCategories = new Set(['academics', 'events', 'achievements']);
  const academyFeedPosts = getPublishedPostsFromState(state).filter((p) => {
    const inCat = academyCategories.has(String(p.category || '').trim().toLowerCase());
    if (!inCat) return false;
    if (!query) return true;
    return (
      String(p.title || '').toLowerCase().includes(query) ||
      String(p.description || '').toLowerCase().includes(query) ||
      String(p.category || '').toLowerCase().includes(query)
    );
  });

  return `
    <!-- ACADEMY SEARCH BAR -->
    <div class="card" style="padding:10px 14px;" data-testid="academy_search_bar">
      <div class="row-between">
        <span style="color:var(--brand-burgundy);display:inline-flex;">${icon('search', 20)}</span>
        <input
          type="search"
          id="academy-search-input"
          class="form-input"
          style="border:none;min-height:40px;padding:6px 8px;"
          placeholder="${esc(s.academySearchPlaceholder || 'Search Academy sections & posts...')}"
          value="${esc(ui.academySearchQuery || '')}"
          aria-label="${esc(s.academySearchPlaceholder || 'Search Academy sections & posts...')}"
          data-testid="academy_search_input"
        />
        ${ui.academySearchQuery ? `<button type="button" class="icon-btn" data-action="clear-academy-search" aria-label="Clear search">${icon('close', 18)}</button>` : ''}
      </div>
    </div>

    <!-- ACADEMY 12-CARD GRID (3x4) + ADMISSIONS -->
    <section data-testid="academy_categories_grid_section">
      <div class="section-header">
        <h2 class="section-title">${esc(s.academyCategoriesSectionTitle || 'Academy Categories')}</h2>
      </div>
      <div class="academy-grid-12" data-testid="academy_12_cards_grid">
        ${filteredCategories.map((cat) => `
          <button
            type="button"
            class="academy-grid-card"
            data-route="academics/category/${esc(cat.routeId)}"
            data-testid="academy_card_${esc(cat.routeId)}"
          >
            <span class="icon-tile tone-${esc(cat.tone)}">${icon(cat.icon, 22)}</span>
            <strong class="academy-grid-card-title">${esc(cat.title)}</strong>
            <span class="muted text-xs">${cat.subSections.length} sections</span>
          </button>
        `).join('')}
      </div>
    </section>

    <!-- DIRECT PAGES QUICK ACCESS -->
    <section data-testid="academics_hub_screen_list">
      <div class="section-header">
        <h2 class="section-title">${esc(s.quickAccessTitle)}</h2>
      </div>
      <div class="academics-sections-grid">
        ${quickSections.map((item) => `
          <button type="button" class="card interactive" style="text-align:left;" data-route="${item.route}" data-testid="${item.testId}">
            <div class="stack-sm">
              <span class="icon-tile ${item.tone}">${icon(item.iconName, 22)}</span>
              <strong>${esc(item.title)}</strong>
              <span class="muted text-xs">${esc(item.desc)}</span>
            </div>
          </button>
        `).join('')}
      </div>
    </section>

    <!-- ACADEMY SOCIAL FEED -->
    <section data-testid="academy_social_feed_section">
      <div class="section-header">
        <h2 class="section-title">${esc(s.academyFeedSectionTitle || 'Academy Updates & Highlights')}</h2>
      </div>
      ${academyFeedPosts.length === 0
        ? renderAwaitingCard(s.academyFeedSectionTitle || 'Academy Updates & Highlights', s.socialFeedEmptyDesc || s.academicPendingDesc, 'school', s)
        : `
          <div class="stack-md" data-testid="academy_social_feed_list">
            ${academyFeedPosts.map((post) =>
              renderSchoolPostCard(post, state, s, {
                showEngagementActions: true,
                activeVideoPostId: ui.activeVideoPostId || null
              })
            ).join('')}
          </div>
        `}
    </section>

    ${renderFooter(s)}
  `;
}

export function collectAcademyPublishedLegacyCards(categoryId, subSectionId, state, s) {
  const { profile } = state;
  const academics = (state.academics || []).filter((x) => x.isPublished !== false);
  const courses = (state.courses || []).filter((x) => x.isPublished !== false);
  const facilities = (state.facilities || []).filter((x) => x.isPublished !== false);
  const transportRoutes = (state.transportRoutes || []).filter((x) => x.isPublished !== false);
  const news = (state.news || []).filter((x) => x.isPublished !== false);
  const announcements = (state.announcements || []).filter((x) => x.isPublished !== false);
  const galleryMedia = (state.galleryMedia || []).filter((x) => x.isPublished !== false);
  const items = [];

  if (categoryId === 'academics') {
    if (subSectionId === 'classes' && profile.mediumAndGradesNote) {
      items.push({ title: s.mediumAndGradesLabel || 'Medium & Classes', badge: 'Official', summary: profile.mediumAndGradesNote });
    }
    if (subSectionId === 'curriculum' || subSectionId === 'academic_structure') {
      academics.forEach((a) => {
        items.push({ title: a.title, badge: a.category || a.level || 'Academic', summary: a.description });
      });
    }
  } else if (categoryId === 'higher_secondary_courses') {
    const verifiedCourses = courses.filter((c) => c.verified || c.subjects);
    if (subSectionId === 'available_groups_streams' || subSectionId === 'group_wise_subjects' || subSectionId === 'course_details') {
      verifiedCourses.forEach((c) => {
        items.push({
          title: `${c.groupCode}: ${c.title}`,
          badge: c.medium || 'Higher Secondary',
          summary: `${s.subjectsLabel}: ${c.subjects}${c.eligibility ? ` · ${s.eligibilityLabel}: ${c.eligibility}` : ''}`
        });
      });
    } else if (subSectionId === 'eligibility_criteria') {
      verifiedCourses.filter((c) => c.eligibility).forEach((c) => {
        items.push({ title: `${c.groupCode} Eligibility`, badge: c.groupCode, summary: c.eligibility });
      });
    }
  } else if (categoryId === 'about_school') {
    if (subSectionId === 'school_profile' && profile.welcomeIntroduction) {
      items.push({ title: profile.schoolName, badge: 'School Profile', summary: profile.welcomeIntroduction, imageUri: profile.profileImageUri || profile.aboutImageUri });
    }
    if (subSectionId === 'history_background' && profile.history) {
      items.push({ title: s.historyLabel, badge: 'History', summary: profile.history, imageUri: profile.aboutImageUri });
    }
    if (subSectionId === 'vision_mission' && (profile.vision || profile.mission)) {
      if (profile.vision) items.push({ title: s.visionLabel, badge: 'Vision', summary: profile.vision });
      if (profile.mission) items.push({ title: s.missionLabel, badge: 'Mission', summary: profile.mission });
    }
    if (subSectionId === 'motto_core_values' && profile.motto) {
      items.push({ title: 'Official Motto', badge: profile.emblemMark || 'M S C', summary: `"${profile.motto}"` });
    }
    if (subSectionId === 'leadership_management' && profile.principalName) {
      items.push({ title: s.principalLabel, badge: 'Leadership', summary: profile.principalName });
    }
    if (subSectionId === 'recognitions_affiliations' && profile.affiliationNumber) {
      items.push({ title: s.affiliationLabel || 'Affiliation', badge: 'Recognition', summary: profile.affiliationNumber });
    }
  } else if (categoryId === 'facilities') {
    facilities.forEach((f) => {
      const cat = String(f.category || '').toLowerCase();
      const match =
        (subSectionId === 'classrooms' && cat.includes('class')) ||
        (subSectionId === 'laboratories' && cat.includes('lab')) ||
        (subSectionId === 'library' && cat.includes('lib')) ||
        (subSectionId === 'sports_playground' && (cat.includes('sport') || cat.includes('play'))) ||
        (subSectionId === 'safety_hygiene' && (cat.includes('safe') || cat.includes('hygien'))) ||
        subSectionId === 'campus_infrastructure';
      if (match) {
        items.push({
          title: f.name,
          badge: f.category || 'Campus',
          summary: `${f.description}${f.capacityOrSpec ? ` (${f.capacityOrSpec})` : ''}`,
          imageUri: f.imageUrl || f.imageUri
        });
      }
    });
  } else if (categoryId === 'transport') {
    if (subSectionId === 'bus_routes' || subSectionId === 'bus_stops' || subSectionId === 'timings') {
      transportRoutes.forEach((r) => {
        items.push({
          title: `${s.routeNumberPrefix} ${r.routeNumber} — ${r.routeName}`,
          badge: r.morningTiming || r.timings || 'Route',
          summary: `Stops: ${r.stopsSummary || r.stops}${r.afternoonTiming ? ` · Afternoon: ${r.afternoonTiming}` : ''}`
        });
      });
    } else if (subSectionId === 'contact_support') {
      transportRoutes.filter((r) => r.coordinatorContact || r.coordinator).forEach((r) => {
        items.push({
          title: `${s.routeNumberPrefix} ${r.routeNumber} Coordinator`,
          badge: 'Support',
          summary: r.coordinatorContact || r.coordinator
        });
      });
    }
  } else if (categoryId === 'news_announcements') {
    if (subSectionId === 'latest_news' || subSectionId === 'events_programs' || subSectionId === 'achievements') {
      news.forEach((n) => {
        items.push({
          title: n.title,
          badge: n.publishedDateText || s.tabNews,
          summary: n.fullBody || n.summary,
          imageUri: n.imageUrl || n.imageUri
        });
      });
    }
    if (subSectionId === 'announcements' || subSectionId === 'circulars_notices' || subSectionId === 'important_updates') {
      announcements.forEach((a) => {
        items.push({
          title: a.title,
          badge: a.priorityLabel || a.issuedDateText || s.tabAnnouncements,
          summary: a.message,
          imageUri: a.imageUrl || a.imageUri
        });
      });
    }
  } else if (categoryId === 'gallery') {
    if (subSectionId === 'photo_gallery') {
      galleryMedia
        .filter((m) => !m.youtubeUrl && m.mediaType !== 'YOUTUBE')
        .forEach((m) => {
          items.push({
            title: m.caption || m.album || s.titleGallery,
            badge: m.album || 'Photo',
            summary: m.category || 'Campus',
            imageUri: m.imageUrl || m.mediaUri
          });
        });
      // Also include published V2 Gallery Photos that belong to at least one published V2 Gallery Album
      const publishedV2Albums = new Map(
        ((state.academicsV2 && state.academicsV2.galleryAlbums) || [])
          .filter((alb) => alb && alb.status === 'PUBLISHED')
          .map((alb) => [alb.id, alb])
      );
      ((state.academicsV2 && state.academicsV2.galleryPhotos) || [])
        .filter((p) => p && p.status === 'PUBLISHED' && Array.isArray(p.albumIds) && p.albumIds.some((aid) => publishedV2Albums.has(aid)))
        .forEach((p) => {
          const albumTitles = p.albumIds.map((aid) => publishedV2Albums.get(aid)?.title).filter(Boolean).join(', ');
          items.push({
            title: p.title || p.caption || s.titleGallery,
            badge: albumTitles || 'Photo Album',
            summary: p.caption || '',
            imageUri: p.imageUri
          });
        });
    }
  }

  // Include published Academics V2 records from state.academicsV2.records
  const v2Records = ((state.academicsV2 && state.academicsV2.records) || []).filter((record) => {
    if (!record || record.categoryKey !== categoryId || record.sectionKey !== subSectionId) return false;
    if (record.status !== 'PUBLISHED') return false;
    if (categoryId === 'examinations' && subSectionId === 'exam_results' && !record.isExamResultAuthorized) {
      return false;
    }
    return true;
  });
  v2Records.forEach((rec) => {
    items.push({
      title: rec.title,
      badge: rec.badgeText || rec.academicYear || rec.subtitle || 'Verified',
      summary: rec.body || rec.summary || '',
      imageUri: rec.imageUri || ''
    });
  });

  // Also include any legacy academicsV2Items stored for this categoryId & subSectionId
  const v2Items = (state.academicsV2Items || []).filter(
    (item) =>
      item.categoryId === categoryId &&
      item.subSectionId === subSectionId &&
      item.status === 'PUBLISHED' &&
      item.isOwnerApproved !== false
  );
  v2Items.forEach((v2) => {
    items.push({
      title: v2.title,
      badge: v2.badgeText || v2.academicYear || 'Verified',
      summary: v2.body || v2.summary || '',
      imageUri: v2.imageUri || ''
    });
  });

  return items;
}

export function renderAcademyCategoryDetailView(state, s, ui = {}, categoryRouteId = 'academics') {
  const category =
    ACADEMICS_V2_CATEGORIES.find((c) => c.routeId === categoryRouteId) ||
    ACADEMICS_V2_CATEGORIES[0];

  const primarySections = category.subSections.slice(0, 5);
  const additionalSections = category.subSections.slice(5);
  const expandedMap = ui.expandedAcademySections || {};

  // Approved public faculty profiles (strictly 8 public fields) from both state.academicsV2.facultyProfiles and state.facultyProfiles
  const rawFacultyList = [
    ...((state.academicsV2 && state.academicsV2.facultyProfiles) || []),
    ...(state.facultyProfiles || [])
  ];
  const seenFacultyIds = new Set();
  const approvedFaculty = rawFacultyList.filter((f) => {
    if (!f || f.status !== 'PUBLISHED' || !f.isSchoolApproved || !f.hasStaffConsent) return false;
    if (seenFacultyIds.has(f.id)) return false;
    seenFacultyIds.add(f.id);
    return true;
  });

  // Published V2 Gallery Albums & Photos for Card 07 (gallery)
  const publishedV2Albums = ((state.academicsV2 && state.academicsV2.galleryAlbums) || [])
    .filter((alb) => alb && alb.status === 'PUBLISHED');
  const publishedV2Photos = ((state.academicsV2 && state.academicsV2.galleryPhotos) || [])
    .filter((p) => p && p.status === 'PUBLISHED');

  const renderSectionAccordion = (sub, idx) => {
    const key = `${category.routeId}::${sub.id}`;
    const isExpanded = key in expandedMap ? Boolean(expandedMap[key]) : idx === 0;
    const mappedItems = collectAcademyPublishedLegacyCards(category.routeId, sub.id, state, s);
    const isExamResults = category.routeId === 'examinations' && sub.id === 'exam_results';
    const isFacultySection =
      category.routeId === 'faculty_staff' &&
      (sub.id === 'teaching_staff' || sub.id === 'staff_profiles');
    const totalVisibleCount = mappedItems.length + (isFacultySection ? approvedFaculty.length : 0);

    return `
      <div class="card academy-accordion-card" data-testid="academy_section_card_${esc(sub.id)}">
        <button
          type="button"
          class="academy-accordion-header"
          data-action="toggle-academy-section"
          data-category-id="${esc(category.routeId)}"
          data-section-id="${esc(sub.id)}"
          aria-expanded="${isExpanded}"
          data-testid="academy_section_toggle_${esc(sub.id)}"
        >
          <div class="row-wrap" style="gap:10px;flex:1;min-width:0;">
            <span class="icon-tile tone-${esc(category.tone)}" style="width:36px;height:36px;">${icon(category.icon, 18)}</span>
            <div style="text-align:left;min-width:0;flex:1;">
              <strong style="display:block;font-size:0.96rem;">${esc(sub.title)}</strong>
              <span class="muted text-xs">${totalVisibleCount > 0 ? `${totalVisibleCount} published record(s)` : esc(s.awaitingVerificationLabel)}</span>
            </div>
          </div>
          <span class="muted">${icon(isExpanded ? 'expandLess' : 'expandMore', 20)}</span>
        </button>

        ${isExpanded ? `
          <div class="academy-accordion-body" data-testid="academy_section_body_${esc(sub.id)}">
            ${isExamResults ? `
              <div class="card gold-border" style="margin-bottom:10px;" data-testid="academy_exam_results_gate_notice">
                <span class="badge badge-burgundy" style="margin-bottom:6px;">Owner-Authorized Gate</span>
                <p class="text-sm" style="margin:0;">
                  Official examination results require Super Owner publication approval before appearing publicly. Individual student report cards remain protected inside the Parent Portal.
                </p>
              </div>
            ` : ''}

            ${isFacultySection && approvedFaculty.length > 0 ? `
              <div class="two-col-responsive" style="margin-bottom:10px;">
                ${approvedFaculty.map((f) => `
                  <article class="card" data-testid="public_faculty_card_${esc(f.id)}">
                    <div class="row-wrap" style="gap:12px;">
                      ${f.approvedProfilePhotoUri
                        ? `<img src="${esc(f.approvedProfilePhotoUri)}" alt="${esc(f.teacherName)}" style="width:52px;height:52px;border-radius:50%;object-fit:cover;" />`
                        : `<span class="icon-tile tone-burgundy">${icon('person', 22)}</span>`}
                      <div>
                        <strong>${esc(f.teacherName)}</strong>
                        <div class="muted text-xs">${esc(f.designation)} · ${esc(f.department)}</div>
                      </div>
                    </div>
                    ${f.teachingSubjects ? `<p class="text-xs" style="margin:8px 0 2px;"><strong>Subjects:</strong> ${esc(f.teachingSubjects)}</p>` : ''}
                    ${f.educationalQualifications ? `<p class="text-xs" style="margin:2px 0;"><strong>Qualifications:</strong> ${esc(f.educationalQualifications)}</p>` : ''}
                    ${f.teachingExperience ? `<p class="text-xs" style="margin:2px 0;"><strong>Experience:</strong> ${esc(f.teachingExperience)}</p>` : ''}
                    ${f.professionalIntroduction ? `<p class="muted text-xs" style="margin:6px 0 0;">${esc(f.professionalIntroduction)}</p>` : ''}
                  </article>
                `).join('')}
              </div>
            ` : ''}

            ${mappedItems.length === 0 && !(isFacultySection && approvedFaculty.length > 0)
              ? `
                <div class="academy-empty-slot">
                  <span class="badge badge-gold">${esc(s.awaitingVerificationLabel)}</span>
                  <p class="muted text-sm" style="margin:6px 0 0;">${esc(s.awaitingVerificationDesc)}</p>
                </div>
              `
              : mappedItems.length > 0 ? `
                <div class="stack-sm">
                  ${mappedItems.map((item) => `
                    <article class="card" style="padding:14px;">
                      <div class="row-between" style="margin-bottom:4px;">
                        <strong>${esc(item.title)}</strong>
                        ${item.badge ? `<span class="badge badge-burgundy-soft">${esc(item.badge)}</span>` : ''}
                      </div>
                      ${item.imageUri ? `<img src="${esc(item.imageUri)}" alt="${esc(item.title)}" style="width:100%;max-height:190px;object-fit:cover;border-radius:10px;margin:8px 0;" />` : ''}
                      <p class="muted text-sm" style="margin:0;white-space:pre-line;">${esc(item.summary)}</p>
                    </article>
                  `).join('')}
                </div>
              ` : ''}
          </div>
        ` : ''}
      </div>
    `;
  };

  return `
    <div class="card gold-border" data-testid="academy_category_detail_${esc(category.routeId)}">
      <div class="row-between" style="flex-wrap:wrap;gap:12px;">
        <div class="row-wrap" style="gap:12px;">
          <span class="icon-tile tone-${esc(category.tone)}" style="width:48px;height:48px;">${icon(category.icon, 26)}</span>
          <div>
            <h2 class="section-title" style="margin:0;">${esc(category.title)}</h2>
            <p class="muted text-sm" style="margin:2px 0 0;">${esc(category.subtitle)}</p>
          </div>
        </div>
        ${category.legacyRoute ? `
          <button type="button" class="btn btn-outline" data-route="${esc(category.legacyRoute)}">
            ${esc(s.viewAll)} ${icon('arrowForward', 16)}
          </button>
        ` : ''}
      </div>
    </div>

    ${category.routeId === 'gallery' && publishedV2Albums.length > 0 ? `
      <section class="stack-sm" data-testid="public_v2_gallery_albums_section">
        <div class="section-header"><h3 class="section-title" style="font-size:1.05rem;">Published Photo Albums</h3></div>
        <div class="two-col-responsive">
          ${publishedV2Albums.map((alb) => {
            const albumPhotos = publishedV2Photos.filter((p) => Array.isArray(p.albumIds) && p.albumIds.includes(alb.id));
            return `
              <article class="card" data-testid="public_v2_album_card_${esc(alb.id)}">
                <div class="row-between" style="margin-bottom:6px;">
                  <strong>${esc(alb.title)}</strong>
                  <span class="badge badge-forest">${albumPhotos.length} photo(s)</span>
                </div>
                ${alb.description ? `<p class="muted text-xs" style="margin:0 0 8px;">${esc(alb.description)}</p>` : ''}
                ${albumPhotos.length > 0 ? `
                  <div class="gallery-grid">
                    ${albumPhotos.map((p) => `
                      <div class="gallery-tile" data-testid="public_v2_photo_tile_${esc(p.id)}">
                        <img src="${esc(p.imageUri)}" alt="${esc(p.title || p.caption || alb.title)}" />
                        ${(p.title || p.caption) ? `<div class="gallery-tile-caption">${esc(p.title || p.caption)}</div>` : ''}
                      </div>
                    `).join('')}
                  </div>
                ` : ''}
              </article>
            `;
          }).join('')}
        </div>
      </section>
    ` : ''}

    <section class="stack-sm" data-testid="academy_primary_sections_list">
      ${primarySections.map((sub, idx) => renderSectionAccordion(sub, idx)).join('')}
    </section>

    ${additionalSections.length > 0 ? `
      <section class="stack-sm" data-testid="academy_additional_sections_list">
        <div class="section-header" style="margin-top:6px;">
          <h3 class="section-title" style="font-size:1.05rem;">${esc(s.academyMoreInCategoryPrefix || 'More in')} ${esc(category.title)}</h3>
        </div>
        ${additionalSections.map((sub, idx) => renderSectionAccordion(sub, idx + 5)).join('')}
      </section>
    ` : ''}

    <button type="button" class="btn btn-outline btn-full" data-route="academics" data-testid="academy_back_to_hub_btn">
      ${icon('back', 18)} ${esc(s.titleAcademics)}
    </button>

    ${renderFooter(s)}
  `;
}

export function renderAcademicsView(state, s) {
  const publishedAcademics = (state.academics || []).filter((a) => a.isPublished !== false);
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
    ${publishedAcademics.length === 0
      ? renderAwaitingCard(s.academicOverviewTitle, s.academicPendingDesc, 'school', s)
      : `<div class="two-col-responsive">${publishedAcademics.map((a) => `
          <article class="card">
            <span class="badge badge-forest" style="margin-bottom:8px;">${esc(a.level || a.category || 'Academic')}</span>
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
          <h3 style="margin:0 0 6px;font-family:var(--font-serif);">${esc(c.title)}</h3>
          ${c.classLevelLabel ? `<p class="text-xs muted" style="margin:0 0 6px;"><strong>${esc(s.classLevelLabel || 'Classes')}:</strong> ${esc(c.classLevelLabel)}</p>` : ''}
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
    ${profile.aboutImageUri ? `
      <div class="card" style="padding:10px;">
        <img src="${esc(profile.aboutImageUri)}" alt="${esc(profile.schoolName)}" style="width:100%;max-height:280px;object-fit:cover;border-radius:14px;" />
      </div>
    ` : ''}
    <div class="card">
      ${renderVerifiedField(s.welcomeSectionTitle, profile.welcomeIntroduction, 'school', s)}
      ${renderVerifiedField(s.principalLabel, profile.principalName, 'person', s)}
      ${renderVerifiedField(s.historyLabel, profile.history, 'info', s)}
      ${renderVerifiedField(s.visionLabel, profile.vision, 'visibility', s)}
      ${renderVerifiedField(s.missionLabel, profile.mission, 'award', s)}
      ${renderVerifiedField(s.affiliationLabel || 'Affiliation / Recognition', profile.affiliationNumber, 'verified', s)}
      ${renderVerifiedField(s.mediumAndGradesLabel || 'Medium & Classes', profile.mediumAndGradesNote, 'book', s)}
      ${renderVerifiedField(s.addressLabel, profile.addressLine, 'location', s)}
    </div>
    ${renderFooter(s)}
  `;
}

export function renderFacilitiesView(state, s) {
  const publishedFacilities = (state.facilities || []).filter((f) => f.isPublished !== false);
  return `
    <div class="section-header"><h2 class="section-title">${esc(s.titleFacilities)}</h2></div>
    ${publishedFacilities.length === 0
      ? renderAwaitingCard(s.titleFacilities, s.facilitiesPendingDesc, 'apartment', s)
      : `<div class="two-col-responsive">${publishedFacilities.map((f) => `
          <article class="card">
            ${f.imageUrl ? `<img src="${esc(f.imageUrl)}" alt="${esc(f.name)}" style="width:100%;height:160px;object-fit:cover;border-radius:12px;margin-bottom:10px;" />` : ''}
            <div class="row-between" style="margin-bottom:6px;">
              <span class="badge badge-gold">${esc(f.category || 'Campus')}</span>
              ${f.capacityOrSpec ? `<span class="badge badge-forest">${esc(f.capacityOrSpec)}</span>` : ''}
            </div>
            <h3 style="margin:0 0 6px;">${esc(f.name)}</h3>
            <p class="muted text-sm" style="margin:0;">${esc(f.description)}</p>
          </article>
        `).join('')}</div>`}
    ${renderFooter(s)}
  `;
}

export function renderTransportView(state, s) {
  const publishedRoutes = (state.transportRoutes || []).filter((r) => r.isPublished !== false);
  return `
    <div class="section-header"><h2 class="section-title">${esc(s.titleTransport)}</h2></div>
    ${publishedRoutes.length === 0
      ? renderAwaitingCard(s.titleTransport, s.transportPendingDesc, 'bus', s)
      : `<div class="two-col-responsive">${publishedRoutes.map((r) => `
          <article class="card">
            <span class="badge badge-burgundy" style="margin-bottom:8px;">${esc(s.routeNumberPrefix)} ${esc(r.routeNumber)}</span>
            <h3 style="margin:0 0 6px;">${esc(r.routeName)}</h3>
            <p class="text-sm" style="margin:0 0 6px;">${esc(r.stopsSummary || r.stops)}</p>
            ${(r.morningTiming || r.timings) ? `<p class="muted text-xs" style="margin:0 0 4px;"><strong>${esc(s.morningTimingLabel || s.pickupTimingLabel)}:</strong> ${esc(r.morningTiming || r.timings)}</p>` : ''}
            ${r.afternoonTiming ? `<p class="muted text-xs" style="margin:0 0 4px;"><strong>${esc(s.afternoonTimingLabel || 'Afternoon')}:</strong> ${esc(r.afternoonTiming)}</p>` : ''}
            ${(r.coordinatorContact || r.coordinator) ? `<p class="muted text-xs" style="margin:0;"><strong>${esc(s.coordinatorLabel)}:</strong> ${esc(r.coordinatorContact || r.coordinator)}</p>` : ''}
          </article>
        `).join('')}</div>`}
    ${renderFooter(s)}
  `;
}

export function renderUpdatesView(state, s, ui = {}) {
  const tab = ui.updatesTab || 'all';
  const showNews = tab === 'all' || tab === 'news';
  const showAnn = tab === 'all' || tab === 'announcements';
  const publishedNews = (state.news || []).filter((n) => n.isPublished !== false);
  const publishedAnnouncements = (state.announcements || []).filter((a) => a.isPublished !== false);
  const empty = (!showNews || publishedNews.length === 0) && (!showAnn || publishedAnnouncements.length === 0);

  return `
    <div class="tabs-bar" role="tablist">
      <button type="button" class="tab-btn ${tab === 'all' ? 'active' : ''}" data-action="updates-tab" data-tab="all">${esc(s.tabAllUpdates)}</button>
      <button type="button" class="tab-btn ${tab === 'news' ? 'active' : ''}" data-action="updates-tab" data-tab="news">${esc(s.tabNews)}</button>
      <button type="button" class="tab-btn ${tab === 'announcements' ? 'active' : ''}" data-action="updates-tab" data-tab="announcements">${esc(s.tabAnnouncements)}</button>
    </div>

    ${empty
      ? renderAwaitingCard(s.titleNewsAnnouncements, tab === 'news' ? s.newsPendingDesc : s.announcementsPendingDesc, 'campaign', s)
      : `<div class="stack-sm">
          ${showAnn ? publishedAnnouncements.map((ann) => `
            <article class="card burgundy-border interactive" data-action="open-announcement" data-id="${ann.id}">
              <div class="row-between" style="margin-bottom:6px;">
                <span class="badge badge-burgundy-soft">${esc(ann.priorityLabel || s.tabAnnouncements)}</span>
                <span class="muted text-xs">${esc(ann.issuedDateText)}</span>
              </div>
              ${ann.imageUrl ? `<img src="${esc(ann.imageUrl)}" alt="${esc(ann.title)}" style="width:100%;max-height:170px;object-fit:cover;border-radius:10px;margin-bottom:8px;" />` : ''}
              <h3 style="margin:0 0 4px;font-size:1rem;">${esc(ann.title)}</h3>
              <p class="muted text-sm" style="margin:0;">${esc(ann.message)}</p>
            </article>
          `).join('') : ''}
          ${showNews ? publishedNews.map((item) => `
            <article class="card interactive" data-action="open-news" data-id="${item.id}">
              <div class="row-between" style="margin-bottom:6px;">
                <span class="badge badge-gold">${esc(item.isFeatured ? s.featuredNewsBadge : s.tabNews)}</span>
                <span class="muted text-xs">${esc(item.publishedDateText)}</span>
              </div>
              ${item.imageUrl ? `<img src="${esc(item.imageUrl)}" alt="${esc(item.title)}" style="width:100%;max-height:170px;object-fit:cover;border-radius:10px;margin-bottom:8px;" />` : ''}
              <h3 style="margin:0 0 4px;font-size:1rem;">${esc(item.title)}</h3>
              <p class="muted text-sm" style="margin:0;">${esc(item.summary)}</p>
            </article>
          `).join('') : ''}
        </div>`}
    ${renderFooter(s)}
  `;
}

export function renderGalleryView(state, s, ui = {}) {
  const tab = ui.galleryTab || 'photos';
  const cat = ui.galleryCategory || 'all';
  const publishedMedia = (state.galleryMedia || []).filter((m) => m.isPublished !== false);
  const categories = ['all', ...Array.from(new Set(publishedMedia.map((m) => m.category || 'Campus')))];
  const filtered = cat === 'all'
    ? publishedMedia
    : publishedMedia.filter((m) => (m.category || 'Campus') === cat);

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
            const ytUrl = m.youtubeUrl || (m.mediaType === 'YOUTUBE' ? m.mediaUri : '');
            const parsedYt = parseYouTubeUrl(ytUrl);
            const thumbSrc = parsedYt ? parsedYt.thumbnailUrl : m.imageUrl;
            const isYt = Boolean(parsedYt || m.youtubeUrl || m.mediaType === 'YOUTUBE');
            return `
              <button
                type="button"
                class="gallery-tile"
                ${isYt && ytUrl
                  ? `data-action="play-youtube" data-url="${esc(ytUrl)}" data-title="${esc(m.caption || s.titleGallery)}"`
                  : `data-action="open-lightbox" data-index="${globalIdx}"`}
              >
                <img src="${esc(thumbSrc)}" alt="${esc(m.caption || s.titleGallery)}" />
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
