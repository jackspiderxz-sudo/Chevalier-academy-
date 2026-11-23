/**
 * Web-compatible persistence layer replacing Android Room & DataStore.
 * Uses localStorage in browsers with an automatic in-memory fallback for tests/SSR.
 * Mirrors ChevalierDatabase.verifiedDefaultProfile() and structured CMS collections.
 */

import { BRAND_CONSTANTS } from './i18n.js';
import { parseYouTubeUrl } from './mediaUtils.js';

const STORAGE_KEY = 'chevalier_academy_web_state_v1';

export const SCHOOL_POST_TYPES = Object.freeze({
  YOUTUBE_DIRECT: 'YOUTUBE_DIRECT',
  ADMIN_CUSTOM_VIDEO: 'ADMIN_CUSTOM_VIDEO',
  PHOTO: 'PHOTO'
});

export const SCHOOL_POST_STATUS = Object.freeze({
  PUBLISHED: 'PUBLISHED',
  UNPUBLISHED: 'UNPUBLISHED',
  ARCHIVED: 'ARCHIVED'
});

export const SCHOOL_POST_CATEGORIES = Object.freeze([
  'General',
  'Academics',
  'Events',
  'Sports',
  'Achievements',
  'Campus Life',
  'Announcement'
]);

export const MAX_POST_PHOTOS = 10;
export const HIGHLIGHT_TTL_MILLIS = 24 * 60 * 60 * 1000;

export function createVerifiedDefaultState() {
  return {
    version: 1,
    preferences: {
      language: 'en',
      theme: 'system',
      hasCompletedOnboarding: false
    },
    profile: {
      schoolName: BRAND_CONSTANTS.officialSchoolName,
      shortName: BRAND_CONSTANTS.shortSchoolName,
      motto: BRAND_CONSTANTS.officialMotto,
      addressLine: BRAND_CONSTANTS.officialAddress,
      emblemMark: BRAND_CONSTANTS.emblemMark,
      poweredBy: BRAND_CONSTANTS.poweredByAttribution,
      welcomeIntroduction: '',
      mediumAndGradesNote: '',
      mapCoordinatesOrQuery: 'Chevalier Academy Matric Higher Secondary School, Siluvathur Road, Dindigul - 624 306',
      phone: '',
      email: '',
      website: '',
      officeHours: '',
      principalName: '',
      history: '',
      vision: '',
      mission: '',
      affiliationNumber: ''
    },
    heroPhotos: [],
    academics: [],
    courses: [
      { id: 1, groupCode: 'Group I', title: 'Higher Secondary Group I', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 2, groupCode: 'Group II', title: 'Higher Secondary Group II', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 3, groupCode: 'Group III', title: 'Higher Secondary Group III', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 4, groupCode: 'Group IV', title: 'Higher Secondary Group IV', medium: '', subjects: '', eligibility: '', verified: false }
    ],
    facilities: [],
    transportRoutes: [],
    news: [],
    announcements: [],
    galleryAlbums: [],
    galleryMedia: [],
    schoolPosts: [],
    schoolHighlights: [],
    socialLinks: [
      { id: 1, platformName: 'Facebook', handle: '', url: '', active: false },
      { id: 2, platformName: 'Instagram', handle: '', url: '', active: false },
      { id: 3, platformName: 'YouTube', handle: '', url: '', active: false },
      { id: 4, platformName: 'Official Website', handle: '', url: '', active: false }
    ],
    admin: {
      pinHash: null,
      pinSalt: null,
      displayName: 'School Administrator',
      failedAttempts: 0,
      lockoutUntil: 0,
      activityLog: []
    }
  };
}

function getStorageBackend(customStorage) {
  if (customStorage) return customStorage;
  try {
    if (typeof globalThis !== 'undefined' && globalThis.localStorage) {
      return globalThis.localStorage;
    }
  } catch {
    // Fallback below
  }
  const map = new Map();
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k)
  };
}

export async function hashPin(pin, salt) {
  const input = `${salt}::${pin}::chevalier_academy`;
  if (typeof globalThis !== 'undefined' && globalThis.crypto?.subtle) {
    const encoded = new TextEncoder().encode(input);
    const buf = await globalThis.crypto.subtle.digest('SHA-256', encoded);
    return Array.from(new Uint8Array(buf))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }
  // Deterministic fallback hash for environments without subtle crypto
  let h = 2166136261;
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0).toString(16).padStart(8, '0');
}

export class ChevalierStore {
  constructor(customStorage = null) {
    this.storage = getStorageBackend(customStorage);
    this.listeners = new Set();
    this.state = this._load();
    this.isAdminAuthenticated = false;
  }

  _load() {
    const defaults = createVerifiedDefaultState();
    try {
      const raw = this.storage.getItem(STORAGE_KEY);
      if (!raw) return defaults;
      const parsed = JSON.parse(raw);
      return {
        ...defaults,
        ...parsed,
        schoolPosts: Array.isArray(parsed.schoolPosts) ? parsed.schoolPosts : [],
        schoolHighlights: Array.isArray(parsed.schoolHighlights) ? parsed.schoolHighlights : [],
        preferences: { ...defaults.preferences, ...(parsed.preferences || {}) },
        profile: { ...defaults.profile, ...(parsed.profile || {}) },
        admin: { ...defaults.admin, ...(parsed.admin || {}) }
      };
    } catch {
      return defaults;
    }
  }

  _save() {
    try {
      this.storage.setItem(STORAGE_KEY, JSON.stringify(this.state));
    } catch {
      // Ignore storage quota exceptions gracefully
    }
    this.listeners.forEach((fn) => fn(this.state));
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  getState() {
    return this.state;
  }

  setLanguage(lang) {
    if (!['en', 'ta', 'hi'].includes(lang)) return;
    this.state.preferences.language = lang;
    this._save();
  }

  completeOnboarding(lang) {
    if (lang) this.state.preferences.language = lang;
    this.state.preferences.hasCompletedOnboarding = true;
    this._save();
  }

  setTheme(theme) {
    if (!['system', 'light', 'dark'].includes(theme)) return;
    this.state.preferences.theme = theme;
    this._save();
  }

  hasAdminPin() {
    return Boolean(this.state.admin.pinHash && this.state.admin.pinSalt);
  }

  async setupAdminPin(pin, displayName = 'School Administrator') {
    const cleanPin = String(pin || '').trim();
    if (!/^\d{4,8}$/.test(cleanPin)) {
      throw new Error('PIN must be 4 to 8 digits.');
    }
    const salt = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
    const digest = await hashPin(cleanPin, salt);
    this.state.admin.pinSalt = salt;
    this.state.admin.pinHash = digest;
    this.state.admin.displayName = displayName.trim() || 'School Administrator';
    this.state.admin.failedAttempts = 0;
    this.state.admin.lockoutUntil = 0;
    this.isAdminAuthenticated = true;
    this.logActivity('ADMIN_PIN_CREATED', 'Admin Portal', 'Local management PIN configured');
    this._save();
  }

  async verifyAdminPin(pin) {
    const now = Date.now();
    if (this.state.admin.lockoutUntil && now < this.state.admin.lockoutUntil) {
      const secs = Math.ceil((this.state.admin.lockoutUntil - now) / 1000);
      return { ok: false, error: `Too many failed attempts. Try again in ${secs}s.` };
    }
    if (!this.hasAdminPin()) {
      return { ok: false, error: 'Admin PIN not configured yet.' };
    }
    const digest = await hashPin(String(pin || '').trim(), this.state.admin.pinSalt);
    if (digest === this.state.admin.pinHash) {
      this.state.admin.failedAttempts = 0;
      this.state.admin.lockoutUntil = 0;
      this.isAdminAuthenticated = true;
      this.logActivity('ADMIN_LOGIN', 'Admin Portal', 'Administrator signed in');
      this._save();
      return { ok: true };
    }
    this.state.admin.failedAttempts = (this.state.admin.failedAttempts || 0) + 1;
    if (this.state.admin.failedAttempts >= 5) {
      this.state.admin.lockoutUntil = now + 30_000;
      this.state.admin.failedAttempts = 0;
    }
    this._save();
    return { ok: false, error: 'Incorrect PIN. Please try again.' };
  }

  logoutAdmin() {
    this.isAdminAuthenticated = false;
    this.listeners.forEach((fn) => fn(this.state));
  }

  logActivity(action, section, details) {
    const entry = {
      id: Date.now() + Math.floor(Math.random() * 1000),
      action,
      section,
      details,
      actor: this.state.admin.displayName || 'Administrator',
      timestamp: new Date().toISOString()
    };
    this.state.admin.activityLog = [entry, ...(this.state.admin.activityLog || [])].slice(0, 60);
  }

  updateProfile(patch) {
    this.state.profile = { ...this.state.profile, ...patch };
    this.logActivity('UPDATE_PROFILE', 'School Profile', 'Updated verified school profile & contact details');
    this._save();
  }

  updateCourse(id, patch) {
    this.state.courses = this.state.courses.map((c) => (c.id === id ? { ...c, ...patch } : c));
    this.logActivity('UPDATE_COURSE', 'Higher Secondary Courses', `Updated course slot #${id}`);
    this._save();
  }

  addAcademicProgram(item) {
    const next = { id: Date.now(), ...item };
    this.state.academics = [next, ...this.state.academics];
    this.logActivity('ADD_ACADEMIC', 'Academics', `Added academic program: ${item.title}`);
    this._save();
  }

  deleteAcademicProgram(id) {
    this.state.academics = this.state.academics.filter((x) => x.id !== id);
    this.logActivity('DELETE_ACADEMIC', 'Academics', `Removed academic program #${id}`);
    this._save();
  }

  addFacility(item) {
    this.state.facilities = [{ id: Date.now(), ...item }, ...this.state.facilities];
    this.logActivity('ADD_FACILITY', 'Facilities', `Added facility: ${item.name}`);
    this._save();
  }

  deleteFacility(id) {
    this.state.facilities = this.state.facilities.filter((x) => x.id !== id);
    this._save();
  }

  addTransportRoute(item) {
    this.state.transportRoutes = [{ id: Date.now(), ...item }, ...this.state.transportRoutes];
    this.logActivity('ADD_TRANSPORT', 'Transport', `Added route: ${item.routeNumber}`);
    this._save();
  }

  deleteTransportRoute(id) {
    this.state.transportRoutes = this.state.transportRoutes.filter((x) => x.id !== id);
    this._save();
  }

  addNews(item) {
    this.state.news = [{ id: Date.now(), publishedDateText: new Date().toLocaleDateString(), ...item }, ...this.state.news];
    this.logActivity('PUBLISH_NEWS', 'News', `Published news: ${item.title}`);
    this._save();
  }

  deleteNews(id) {
    this.state.news = this.state.news.filter((x) => x.id !== id);
    this._save();
  }

  addAnnouncement(item) {
    this.state.announcements = [{ id: Date.now(), issuedDateText: new Date().toLocaleDateString(), ...item }, ...this.state.announcements];
    this.logActivity('PUBLISH_ANNOUNCEMENT', 'Announcements', `Published announcement: ${item.title}`);
    this._save();
  }

  deleteAnnouncement(id) {
    this.state.announcements = this.state.announcements.filter((x) => x.id !== id);
    this._save();
  }

  addHeroPhoto(item) {
    this.state.heroPhotos = [{ id: Date.now(), ...item }, ...this.state.heroPhotos];
    this.logActivity('ADD_HERO_PHOTO', 'Hero Banner', `Added campus hero banner image`);
    this._save();
  }

  deleteHeroPhoto(id) {
    this.state.heroPhotos = this.state.heroPhotos.filter((x) => x.id !== id);
    this._save();
  }

  addGalleryMedia(item) {
    const albumName = (item.album || 'Campus Highlights').trim() || 'Campus Highlights';
    if (!this.state.galleryAlbums.some((a) => a.name.toLowerCase() === albumName.toLowerCase())) {
      this.state.galleryAlbums.push({
        id: Date.now() + 1,
        name: albumName,
        category: item.category || 'Campus'
      });
    }
    this.state.galleryMedia = [{ id: Date.now(), album: albumName, ...item }, ...this.state.galleryMedia];
    this.logActivity('ADD_GALLERY_MEDIA', 'Gallery', `Added gallery item: ${item.caption || albumName}`);
    this._save();
  }

  deleteGalleryMedia(id) {
    this.state.galleryMedia = this.state.galleryMedia.filter((x) => x.id !== id);
    this._save();
  }

  updateSocialLink(id, patch) {
    this.state.socialLinks = this.state.socialLinks.map((s) => (s.id === id ? { ...s, ...patch } : s));
    this.logActivity('UPDATE_SOCIAL', 'Social Media', `Updated channel #${id}`);
    this._save();
  }

  saveSchoolPost(input, nowMillis = Date.now()) {
    const postType = input?.postType || SCHOOL_POST_TYPES.PHOTO;
    const status = input?.status || SCHOOL_POST_STATUS.UNPUBLISHED;
    if (!Object.values(SCHOOL_POST_TYPES).includes(postType)) {
      throw new Error(`Unsupported post type: ${postType}`);
    }
    if (!Object.values(SCHOOL_POST_STATUS).includes(status)) {
      throw new Error(`Unsupported post status: ${status}`);
    }
    const title = String(input?.title || '').trim();
    if (!title) throw new Error('Post title is required.');

    const category = String(input?.category || 'General').trim() || 'General';
    const description = String(input?.description || '').trim();
    const youtubeUrl = String(input?.youtubeUrl || '').trim() || null;
    const customThumbnailUrl = String(input?.customThumbnailUrl || '').trim() || null;
    const photoUrls = Array.isArray(input?.photoUrls)
      ? input.photoUrls.map((u) => String(u || '').trim()).filter(Boolean)
      : [];

    if (postType === SCHOOL_POST_TYPES.YOUTUBE_DIRECT || postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO) {
      if (!parseYouTubeUrl(youtubeUrl)) {
        throw new Error('A valid YouTube URL is required.');
      }
    } else if (postType === SCHOOL_POST_TYPES.PHOTO) {
      if (photoUrls.length === 0) throw new Error('Photo Posts require at least 1 photo.');
      if (photoUrls.length > MAX_POST_PHOTOS) {
        throw new Error(`Photo Posts allow a maximum of ${MAX_POST_PHOTOS} photos.`);
      }
    }

    const existing = input?.id ? (this.state.schoolPosts || []).find((p) => p.id === input.id) : null;
    const id = existing ? existing.id : nowMillis + Math.floor(Math.random() * 1000);
    const createdAtMillis = existing ? existing.createdAtMillis : nowMillis;
    const publishedAtMillis =
      status === SCHOOL_POST_STATUS.PUBLISHED
        ? (existing?.publishedAtMillis ?? nowMillis)
        : (existing?.publishedAtMillis ?? null);

    const record = {
      id,
      postType,
      category,
      title,
      description,
      youtubeUrl: postType === SCHOOL_POST_TYPES.PHOTO ? null : youtubeUrl,
      customThumbnailUrl: postType === SCHOOL_POST_TYPES.ADMIN_CUSTOM_VIDEO ? customThumbnailUrl : null,
      photoUrls: postType === SCHOOL_POST_TYPES.PHOTO ? photoUrls : [],
      status,
      isFeatured: Boolean(input?.isFeatured),
      createdAtMillis,
      publishedAtMillis,
      updatedAtMillis: nowMillis
    };

    const current = this.state.schoolPosts || [];
    this.state.schoolPosts = existing
      ? current.map((p) => (p.id === id ? record : p))
      : [record, ...current];
    this.logActivity('SAVE_SCHOOL_POST', 'School Posts', `${existing ? 'Updated' : 'Created'} post: ${title}`);
    this._save();
    return record;
  }

  setSchoolPostStatus(id, status, nowMillis = Date.now()) {
    if (!Object.values(SCHOOL_POST_STATUS).includes(status)) {
      throw new Error(`Unsupported post status: ${status}`);
    }
    this.state.schoolPosts = (this.state.schoolPosts || []).map((p) => {
      if (p.id !== id) return p;
      return {
        ...p,
        status,
        publishedAtMillis: status === SCHOOL_POST_STATUS.PUBLISHED ? (p.publishedAtMillis ?? nowMillis) : p.publishedAtMillis,
        updatedAtMillis: nowMillis
      };
    });
    this._save();
  }

  setSchoolPostFeatured(id, isFeatured, nowMillis = Date.now()) {
    this.state.schoolPosts = (this.state.schoolPosts || []).map((p) =>
      p.id === id ? { ...p, isFeatured: Boolean(isFeatured), updatedAtMillis: nowMillis } : p
    );
    this._save();
  }

  deleteSchoolPost(id) {
    this.state.schoolPosts = (this.state.schoolPosts || []).filter((p) => p.id !== id);
    this._save();
  }

  getPublishedSchoolPosts() {
    return [...(this.state.schoolPosts || [])]
      .filter((p) => p.status === SCHOOL_POST_STATUS.PUBLISHED)
      .sort((a, b) => {
        if (Boolean(b.isFeatured) !== Boolean(a.isFeatured)) return b.isFeatured ? 1 : -1;
        return (b.publishedAtMillis ?? b.createdAtMillis) - (a.publishedAtMillis ?? a.createdAtMillis);
      });
  }

  saveSchoolHighlight(input, nowMillis = Date.now()) {
    const status = input?.status || SCHOOL_POST_STATUS.PUBLISHED;
    if (!Object.values(SCHOOL_POST_STATUS).includes(status)) {
      throw new Error(`Unsupported highlight status: ${status}`);
    }
    const parsed = parseYouTubeUrl(input?.youtubeShortsUrl);
    if (!parsed || !parsed.isShort) {
      throw new Error('A valid YouTube Shorts URL (youtube.com/shorts/...) is required.');
    }
    const existing = input?.id ? (this.state.schoolHighlights || []).find((h) => h.id === input.id) : null;
    const id = existing ? existing.id : nowMillis + Math.floor(Math.random() * 1000);
    const createdAtMillis = existing ? existing.createdAtMillis : nowMillis;
    const publishedAtMillis = existing ? existing.publishedAtMillis : nowMillis;
    const expiresAtMillis = existing ? existing.expiresAtMillis : publishedAtMillis + HIGHLIGHT_TTL_MILLIS;

    const record = {
      id,
      title: String(input?.title || '').trim(),
      youtubeShortsUrl: parsed.canonicalWatchUrl,
      status,
      createdAtMillis,
      publishedAtMillis,
      expiresAtMillis
    };
    const current = this.state.schoolHighlights || [];
    this.state.schoolHighlights = existing
      ? current.map((h) => (h.id === id ? record : h))
      : [record, ...current];
    this.logActivity('SAVE_HIGHLIGHT', 'Highlights', `Saved 24h highlight: ${record.title || parsed.videoId}`);
    this._save();
    return record;
  }

  getActiveHighlights(nowMillis = Date.now()) {
    return [...(this.state.schoolHighlights || [])]
      .filter((h) => h.status === SCHOOL_POST_STATUS.PUBLISHED && h.expiresAtMillis > nowMillis)
      .sort((a, b) => b.publishedAtMillis - a.publishedAtMillis);
  }

  deleteSchoolHighlight(id) {
    this.state.schoolHighlights = (this.state.schoolHighlights || []).filter((h) => h.id !== id);
    this._save();
  }

  exportBackupJson() {
    const clone = JSON.parse(JSON.stringify(this.state));
    // Never export salted PIN credentials in plain backup JSON
    delete clone.admin.pinHash;
    delete clone.admin.pinSalt;
    return JSON.stringify(clone, null, 2);
  }

  importBackupJson(rawJson) {
    const parsed = JSON.parse(rawJson);
    if (!parsed || typeof parsed !== 'object' || !parsed.profile) {
      throw new Error('Invalid Chevalier Academy backup format.');
    }
    const preservedAdmin = {
      pinHash: this.state.admin.pinHash,
      pinSalt: this.state.admin.pinSalt,
      displayName: this.state.admin.displayName,
      failedAttempts: 0,
      lockoutUntil: 0,
      activityLog: this.state.admin.activityLog
    };
    const defaults = createVerifiedDefaultState();
    this.state = {
      ...defaults,
      ...parsed,
      schoolPosts: Array.isArray(parsed.schoolPosts) ? parsed.schoolPosts : [],
      schoolHighlights: Array.isArray(parsed.schoolHighlights) ? parsed.schoolHighlights : [],
      preferences: { ...defaults.preferences, ...(parsed.preferences || {}) },
      profile: { ...defaults.profile, ...(parsed.profile || {}) },
      admin: preservedAdmin
    };
    this.logActivity('RESTORE_BACKUP', 'Backup', 'Restored school records from JSON backup');
    this._save();
  }
}
