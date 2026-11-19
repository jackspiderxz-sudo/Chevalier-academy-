/**
 * Web-compatible persistence layer replacing Android Room & DataStore.
 * Uses localStorage in browsers with an automatic in-memory fallback for tests/SSR.
 * Mirrors ChevalierDatabase.verifiedDefaultProfile() and structured CMS collections.
 */

import { BRAND_CONSTANTS } from './i18n.js';

const STORAGE_KEY = 'chevalier_academy_web_state_v1';

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
      preferences: { ...defaults.preferences, ...(parsed.preferences || {}) },
      profile: { ...defaults.profile, ...(parsed.profile || {}) },
      admin: preservedAdmin
    };
    this.logActivity('RESTORE_BACKUP', 'Backup', 'Restored school records from JSON backup');
    this._save();
  }
}
