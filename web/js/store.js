/**
 * Web-compatible persistence layer with Two-Way Live Firebase Firestore Synchronization.
 * Uses localStorage in browsers with an automatic in-memory fallback for tests/SSR,
 * and synchronizes all 13 Chevalier Academy collections with Firebase Firestore
 * (`gen-lang-client-0028096876` / `ai-studio-android-chevalie-d85b2a3e-45dc-4e49-9c97-6ab207063b96`).
 */

import { BRAND_CONSTANTS } from './i18n.js';
import { parseYouTubeUrl, MAX_MEDIA_URI_CHARS } from './mediaUtils.js';

const STORAGE_KEY = 'chevalier_academy_web_state_v1';
const FIREBASE_MEDIA_URI_PREFIX = 'firebase-media://';
const POST_PHOTO_DOC_PREFIX = 'post_photo_';

export const FIREBASE_SYNC_CONFIG = Object.freeze({
  projectId: 'gen-lang-client-0028096876',
  databaseId: 'ai-studio-android-chevalie-d85b2a3e-45dc-4e49-9c97-6ab207063b96',
  tenantId: 'chevalier_academy_dindigul',
  apiKey: 'AIzaSyA1Xc1LEraBfhoQvgmDrPG0TAD7-U8auOk',
  storageBucket: 'gen-lang-client-0028096876.firebasestorage.app',
  pinGateDocId: 'pin_session_v1_verified',
  pinGateEmail: 'verified-pin-gate@chevalieracademy.edu.in',
  pollIntervalMs: 2500
});

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

export const ACADEMICS_V2_RECORD_STATUS = Object.freeze({
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  PUBLISHED: 'PUBLISHED',
  UNPUBLISHED: 'UNPUBLISHED',
  ARCHIVED: 'ARCHIVED'
});

export const SCHOOL_OWNER_SECTIONS = Object.freeze([
  {
    key: 'OWNER_PROFILE_CONTACT',
    routeId: 'owner_section_profile',
    title: 'Owner Profile & Contact',
    subtitle: 'School Owner identity and private governance contact records'
  },
  {
    key: 'STAFF_ACCESS_APPROVAL',
    routeId: 'owner_section_staff_access',
    title: 'Staff Access Approval',
    subtitle: 'Approve, reject, or revoke staff access requests'
  },
  {
    key: 'ROLE_PERMISSION_CONTROL',
    routeId: 'owner_section_roles',
    title: 'Role & Permission Control',
    subtitle: 'Assign category permissions and role boundaries'
  },
  {
    key: 'APPROVAL_REQUESTS',
    routeId: 'owner_section_approvals',
    title: 'Approval Requests',
    subtitle: 'Review pending content and sensitive publication requests'
  },
  {
    key: 'FINAL_PREVIEW_APPROVAL',
    routeId: 'owner_section_final_preview',
    title: 'Final Preview & Publish Approval',
    subtitle: 'Inspect final public preview and authorize Exam Results or content release'
  },
  {
    key: 'ACCESS_HISTORY_SECURITY',
    routeId: 'owner_section_audit',
    title: 'Access History & Security',
    subtitle: 'Review login history, permission changes, and publication audit logs'
  }
]);

export const V2_CATEGORY_PRIMARY_SECTION_MAP = Object.freeze({
  academics: 'ACADEMICS',
  higher_secondary_courses: 'HIGHER_SECONDARY_COURSES',
  about_school: 'ABOUT_SCHOOL',
  facilities: 'FACILITIES',
  transport: 'TRANSPORT',
  news_announcements: 'NEWS',
  gallery: 'PHOTO_GALLERY',
  school_timetable: 'ACADEMICS',
  examinations: 'ACADEMICS',
  academic_calendar: 'ACADEMICS',
  faculty_staff: 'ABOUT_SCHOOL',
  student_activities: 'ACADEMICS',
  admissions: 'ACADEMICS'
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
export const ADMIN_INACTIVITY_TIMEOUT_MILLIS = 5 * 60 * 1000;

export const MANAGEMENT_ROLES = Object.freeze({
  MASTER_ADMIN: 'MASTER_ADMIN',
  SUPER_OWNER: 'SUPER_OWNER',
  STAFF: 'STAFF'
});

export const PERMISSION_ACTIONS = Object.freeze({
  VIEW: 'VIEW',
  CREATE: 'CREATE',
  EDIT: 'EDIT',
  DELETE: 'DELETE',
  PUBLISH: 'PUBLISH'
});

export const MANAGEMENT_SECTIONS = Object.freeze([
  { key: 'SCHOOL_PROFILE', label: 'School Profile', supportsPublish: false },
  { key: 'LOGO_BANNER', label: 'Logo & Hero Banner', supportsPublish: true },
  { key: 'ABOUT_SCHOOL', label: 'About School', supportsPublish: false },
  { key: 'ACADEMICS', label: 'Academics', supportsPublish: true },
  { key: 'HIGHER_SECONDARY_COURSES', label: 'Higher Secondary Courses', supportsPublish: true },
  { key: 'FACILITIES', label: 'Facilities', supportsPublish: true },
  { key: 'TRANSPORT', label: 'Transport', supportsPublish: true },
  { key: 'NEWS', label: 'News', supportsPublish: true },
  { key: 'ANNOUNCEMENTS', label: 'Announcements', supportsPublish: true },
  { key: 'PHOTO_GALLERY', label: 'Photo Gallery', supportsPublish: true },
  { key: 'CONTACT_DETAILS', label: 'Contact Details', supportsPublish: false },
  { key: 'SOCIAL_MEDIA', label: 'Social Media', supportsPublish: true },
  { key: 'SCHOOL_POSTS', label: 'School Posts', supportsPublish: true },
  { key: 'SCHOOL_HIGHLIGHTS', label: 'School Highlights (24h)', supportsPublish: true },
  { key: 'MEDIA_STUDIO', label: 'Media Studio', supportsPublish: true },
  { key: 'STAFF_MANAGEMENT', label: 'Staff Management', supportsPublish: false },
  { key: 'NOTIFICATIONS', label: 'Notifications', supportsPublish: false },
  { key: 'ACTIVITY_LOG', label: 'Activity Log', supportsPublish: false },
  { key: 'BACKUP_RESTORE', label: 'Backup & Restore', supportsPublish: false },
  { key: 'APP_SETTINGS', label: 'App Settings', supportsPublish: false }
]);

export const MANAGEMENT_SECTION_MAP = Object.freeze(
  Object.fromEntries(MANAGEMENT_SECTIONS.map((s) => [s.key, s]))
);

export const SEARCHABLE_SECTIONS = Object.freeze([
  'ACADEMICS',
  'HIGHER_SECONDARY_COURSES',
  'FACILITIES',
  'TRANSPORT',
  'NEWS',
  'ANNOUNCEMENTS',
  'PHOTO_GALLERY',
  'SOCIAL_MEDIA',
  'SCHOOL_POSTS',
  'SCHOOL_HIGHLIGHTS',
  'MEDIA_STUDIO',
  'STAFF_MANAGEMENT',
  'ACTIVITY_LOG'
]);

export const ADMIN_DASHBOARD_CARDS = Object.freeze([
  {
    id: 'home_page',
    title: 'Home Page',
    subtitle: 'Logo & Hero Banner · Social Feed Posts · 24h Highlights · Media Studio',
    icon: 'home',
    route: 'admin/hub/home_page',
    requiredViewSections: ['LOGO_BANNER', 'SCHOOL_POSTS', 'SCHOOL_HIGHLIGHTS', 'MEDIA_STUDIO'],
    keywords: ['home', 'hero', 'banner', 'logo', 'posts', 'feed', 'highlights', 'shorts', 'media']
  },
  {
    id: 'academy',
    title: 'Academy',
    subtitle: 'Academics · Courses · Facilities · Transport · Gallery · About School',
    icon: 'school',
    route: 'admin/hub/academy',
    requiredViewSections: ['ACADEMICS', 'HIGHER_SECONDARY_COURSES', 'FACILITIES', 'TRANSPORT', 'PHOTO_GALLERY', 'ABOUT_SCHOOL'],
    keywords: ['academy', 'academics', 'courses', 'groups', 'facilities', 'transport', 'bus', 'gallery', 'photos', 'about']
  },
  {
    id: 'parent',
    title: 'Parent',
    subtitle: 'Parent Notices · Student Circulars · Campus Alerts · Portal Info',
    icon: 'parent',
    route: 'admin/hub/parent',
    requiredViewSections: ['ANNOUNCEMENTS', 'NOTIFICATIONS', 'SCHOOL_PROFILE'],
    keywords: ['parent', 'guardian', 'circulars', 'notices', 'alerts', 'attendance', 'homework', 'timetable', 'guidance']
  },
  {
    id: 'contact',
    title: 'Contact',
    subtitle: 'Phone Numbers · Official Email · Office Hours · Map Query',
    icon: 'contact',
    route: 'admin/section/CONTACT_DETAILS',
    requiredViewSections: ['CONTACT_DETAILS'],
    keywords: ['contact', 'phone', 'email', 'address', 'map', 'office', 'hours', 'location']
  },
  {
    id: 'staff_management',
    title: 'Staff Management',
    subtitle: 'Staff Accounts · Roles · Section Permissions · PIN Controls',
    icon: 'groups',
    route: 'admin/section/STAFF_MANAGEMENT',
    requiredViewSections: ['STAFF_MANAGEMENT'],
    keywords: ['staff', 'users', 'roles', 'permissions', 'pin', 'accounts', 'super owner', 'admin']
  },
  {
    id: 'common_app',
    title: 'Common App',
    subtitle: 'App Settings · School Identity · Social Channels · Branding',
    icon: 'settings',
    route: 'admin/hub/common_app',
    requiredViewSections: ['APP_SETTINGS', 'SCHOOL_PROFILE', 'SOCIAL_MEDIA', 'LOGO_BANNER'],
    keywords: ['common', 'settings', 'language', 'theme', 'profile', 'identity', 'social', 'youtube', 'instagram', 'facebook']
  },
  {
    id: 'management_tools',
    title: 'Management Tools',
    subtitle: 'Approval Center · Version History · Activity Log · Backup & Restore',
    icon: 'security',
    route: 'admin/hub/management_tools',
    requiredViewSections: ['ACTIVITY_LOG', 'BACKUP_RESTORE', 'MEDIA_STUDIO'],
    keywords: ['tools', 'approval', 'version', 'history', 'restore', 'activity', 'log', 'audit', 'backup', 'export', 'media studio']
  },
  {
    id: 'updates',
    title: 'Updates',
    subtitle: 'News · Announcements · Official Circulars · Notifications',
    icon: 'campaign',
    route: 'admin/hub/updates',
    requiredViewSections: ['NEWS', 'ANNOUNCEMENTS', 'NOTIFICATIONS'],
    keywords: ['updates', 'news', 'announcements', 'circulars', 'events', 'notifications', 'push', 'alerts']
  }
]);

const RECOVERY_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function bytesToBase64(bytes) {
  if (typeof Buffer !== 'undefined') {
    return Buffer.from(bytes).toString('base64');
  }
  let bin = '';
  for (let i = 0; i < bytes.length; i++) {
    bin += String.fromCharCode(bytes[i]);
  }
  return btoa(bin);
}

function base64ToBytes(b64) {
  const clean = String(b64 || '').trim();
  if (!clean) return new Uint8Array(0);
  try {
    if (typeof Buffer !== 'undefined') {
      return new Uint8Array(Buffer.from(clean, 'base64'));
    }
    const bin = atob(clean);
    const out = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) {
      out[i] = bin.charCodeAt(i);
    }
    return out;
  } catch {
    return new Uint8Array(0);
  }
}

function randomBytes(length) {
  const out = new Uint8Array(length);
  if (typeof globalThis !== 'undefined' && globalThis.crypto?.getRandomValues) {
    globalThis.crypto.getRandomValues(out);
    return out;
  }
  for (let i = 0; i < length; i++) {
    out[i] = Math.floor(Math.random() * 256);
  }
  return out;
}

/**
 * Pure synchronous 256-bit key derivation for PinSecurityManager so both sync
 * and async callers in browser & Node tests work identically with zero external deps.
 */
function deriveKeyBytes256(secret, saltBytes, iterations = 2048) {
  const secretBytes = new TextEncoder().encode(String(secret ?? ''));
  const state = new Uint32Array(8);
  const initSeeds = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
  ];
  for (let i = 0; i < 8; i++) state[i] = initSeeds[i];

  for (let s = 0; s < saltBytes.length; s++) {
    const idx = s & 7;
    state[idx] = Math.imul(state[idx] ^ (saltBytes[s] + s * 31), 0x01000193) >>> 0;
  }

  for (let r = 0; r < iterations; r++) {
    for (let i = 0; i < secretBytes.length; i++) {
      const idx = (r + i) & 7;
      const saltByte = saltBytes.length ? saltBytes[(r + i) % saltBytes.length] : 0x5a;
      let v = state[idx] ^ ((secretBytes[i] << 8) | saltByte) ^ (r * 0x9e3779b9);
      v = ((v << 13) | (v >>> 19)) >>> 0;
      state[idx] = Math.imul(v, 0x85ebca6b) >>> 0;
      state[(idx + 3) & 7] = (state[(idx + 3) & 7] + state[idx]) >>> 0;
    }
  }

  const out = new Uint8Array(32);
  for (let i = 0; i < 8; i++) {
    const word = state[i];
    out[i * 4] = (word >>> 24) & 0xff;
    out[i * 4 + 1] = (word >>> 16) & 0xff;
    out[i * 4 + 2] = (word >>> 8) & 0xff;
    out[i * 4 + 3] = word & 0xff;
  }
  return out;
}

/**
 * Direct Web parity for Android PinSecurityManager.kt
 */
export const PinSecurityManager = Object.freeze({
  MIN_PIN_LENGTH: 6,

  isValidPinFormat(pin) {
    return typeof pin === 'string' && pin.length >= 6 && /^\d+$/.test(pin);
  },

  generateSaltBase64() {
    return bytesToBase64(randomBytes(16));
  },

  deriveVerifierBase64(secret, saltBase64) {
    const saltBytes = base64ToBytes(saltBase64);
    const derived = deriveKeyBytes256(secret, saltBytes);
    return bytesToBase64(derived);
  },

  verifySecretConstantTime(inputSecret, saltBase64, expectedVerifierBase64) {
    if (!inputSecret || !saltBase64 || !expectedVerifierBase64) return false;
    const actualBytes = base64ToBytes(this.deriveVerifierBase64(inputSecret, saltBase64));
    const expectedBytes = base64ToBytes(expectedVerifierBase64);
    if (actualBytes.length === 0 || actualBytes.length !== expectedBytes.length) return false;
    let diff = 0;
    for (let i = 0; i < actualBytes.length; i++) {
      diff |= actualBytes[i] ^ expectedBytes[i];
    }
    return diff === 0;
  },

  generateRecoveryCode() {
    const rnd = randomBytes(12);
    const groups = [];
    for (let g = 0; g < 3; g++) {
      let chunk = '';
      for (let i = 0; i < 4; i++) {
        const b = rnd[g * 4 + i];
        chunk += RECOVERY_ALPHABET[b % RECOVERY_ALPHABET.length];
      }
      groups.push(chunk);
    }
    return groups.join('-');
  },

  computeLockoutDurationMillis(failedAttempts) {
    const n = Number(failedAttempts || 0);
    if (n < 3) return 0;
    if (n === 3) return 15_000;
    if (n === 4) return 30_000;
    if (n === 5) return 60_000;
    return 300_000;
  }
});

export function buildDefaultPermissionsForRole(role) {
  return MANAGEMENT_SECTIONS.map((sec) => {
    const grantAll =
      role === MANAGEMENT_ROLES.MASTER_ADMIN ||
      (role === MANAGEMENT_ROLES.SUPER_OWNER && sec.key !== 'STAFF_MANAGEMENT');
    return {
      sectionKey: sec.key,
      canView: grantAll,
      canCreate: grantAll,
      canEdit: grantAll,
      canDelete: grantAll,
      canPublish: grantAll && sec.supportsPublish
    };
  });
}

export function normalizePermissionsMatrix(permissionsInput, fallbackRole = MANAGEMENT_ROLES.STAFF) {
  const defaults = buildDefaultPermissionsForRole(fallbackRole);
  if (!permissionsInput) return defaults;

  const byKey = new Map();
  if (Array.isArray(permissionsInput)) {
    for (const row of permissionsInput) {
      if (row && row.sectionKey) byKey.set(row.sectionKey, row);
    }
  } else if (typeof permissionsInput === 'object') {
    for (const [k, v] of Object.entries(permissionsInput)) {
      if (Array.isArray(v) || v instanceof Set) {
        const set = new Set(Array.from(v));
        byKey.set(k, {
          sectionKey: k,
          canView: set.has(PERMISSION_ACTIONS.VIEW),
          canCreate: set.has(PERMISSION_ACTIONS.CREATE),
          canEdit: set.has(PERMISSION_ACTIONS.EDIT),
          canDelete: set.has(PERMISSION_ACTIONS.DELETE),
          canPublish: set.has(PERMISSION_ACTIONS.PUBLISH)
        });
      } else if (v && typeof v === 'object') {
        byKey.set(k, { sectionKey: k, ...v });
      }
    }
  }

  return MANAGEMENT_SECTIONS.map((sec) => {
    if (fallbackRole !== MANAGEMENT_ROLES.MASTER_ADMIN && sec.key === 'STAFF_MANAGEMENT' && fallbackRole === MANAGEMENT_ROLES.SUPER_OWNER) {
      return {
        sectionKey: sec.key,
        canView: false,
        canCreate: false,
        canEdit: false,
        canDelete: false,
        canPublish: false
      };
    }
    const row = byKey.get(sec.key);
    if (!row) {
      return {
        sectionKey: sec.key,
        canView: false,
        canCreate: false,
        canEdit: false,
        canDelete: false,
        canPublish: false
      };
    }
    return {
      sectionKey: sec.key,
      canView: Boolean(row.canView ?? row.VIEW),
      canCreate: Boolean(row.canCreate ?? row.CREATE),
      canEdit: Boolean(row.canEdit ?? row.EDIT),
      canDelete: Boolean(row.canDelete ?? row.DELETE),
      canPublish: Boolean(row.canPublish ?? row.PUBLISH) && sec.supportsPublish
    };
  });
}

export function deterministicRemoteId(tableName, localId) {
  const safeTable = String(tableName || 'record').replace(/[^a-zA-Z0-9_-]/g, '_');
  const safeId = String(localId ?? '1').replace(/[^a-zA-Z0-9_-]/g, '_');
  return `${safeTable}_${safeId}`.slice(0, 64);
}

export function toFirestoreValue(val) {
  if (val === null || val === undefined) {
    return { nullValue: null };
  }
  if (typeof val === 'boolean') {
    return { booleanValue: val };
  }
  if (typeof val === 'number') {
    if (Number.isInteger(val)) {
      return { integerValue: String(val) };
    }
    return { doubleValue: val };
  }
  if (typeof val === 'string') {
    return { stringValue: val };
  }
  if (Array.isArray(val)) {
    return {
      arrayValue: {
        values: val.map((item) => toFirestoreValue(item))
      }
    };
  }
  if (typeof val === 'object') {
    return {
      mapValue: {
        fields: toFirestoreFields(val)
      }
    };
  }
  return { stringValue: String(val) };
}

export function toFirestoreFields(obj) {
  const fields = {};
  for (const [k, v] of Object.entries(obj || {})) {
    if (v === undefined) continue;
    fields[k] = toFirestoreValue(v);
  }
  return fields;
}

export function fromFirestoreValue(v) {
  if (!v || typeof v !== 'object') return null;
  if ('nullValue' in v) return null;
  if ('booleanValue' in v) return Boolean(v.booleanValue);
  if ('integerValue' in v) return Number(v.integerValue);
  if ('doubleValue' in v) return Number(v.doubleValue);
  if ('stringValue' in v) return String(v.stringValue);
  if ('timestampValue' in v) return v.timestampValue;
  if ('arrayValue' in v) {
    const arr = v.arrayValue?.values;
    return Array.isArray(arr) ? arr.map(fromFirestoreValue) : [];
  }
  if ('mapValue' in v) {
    return fromFirestoreFields(v.mapValue?.fields || {});
  }
  return null;
}

export function fromFirestoreFields(fields) {
  const out = {};
  for (const [k, v] of Object.entries(fields || {})) {
    out[k] = fromFirestoreValue(v);
  }
  return out;
}

export function fromFirestoreDocument(doc) {
  if (!doc || !doc.name) return null;
  const parts = String(doc.name).split('/');
  const remoteId = parts[parts.length - 1];
  return {
    remoteId,
    data: fromFirestoreFields(doc.fields || {})
  };
}

function stableNumericId(remoteId, fallbackIndex = 1) {
  const trailingDigits = String(remoteId || '').match(/(\d+)$/);
  if (trailingDigits) {
    const n = Number(trailingDigits[1]);
    if (Number.isSafeInteger(n) && n > 0) return n;
  }
  let h = 2166136261;
  const s = String(remoteId || fallbackIndex);
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0) || fallbackIndex;
}

export function createDefaultAcademicsV2State() {
  return {
    records: [],
    galleryAlbums: [],
    galleryPhotos: [],
    facultyProfiles: [],
    ownerPrivateProfile: {
      ownerName: '',
      designation: 'School Owner / Correspondent',
      privateContactPhone: '',
      privateContactEmail: '',
      governanceNote: '',
      updatedAtMillis: 0
    },
    staffAccessRequests: [],
    accessAuditEvents: []
  };
}

export function createVerifiedDefaultState() {
  return {
    version: 1,
    preferences: {
      language: 'en',
      theme: 'system',
      hasCompletedOnboarding: false,
      likedPostIds: [],
      bookmarkedPostIds: [],
      deviceAlertsEnabled: false
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
      affiliationNumber: '',
      profileImageUri: '',
      aboutImageUri: ''
    },
    heroPhotos: [],
    academics: [],
    academicsV2Items: [],
    facultyProfiles: [],
    academicsV2: createDefaultAcademicsV2State(),
    courses: [
      { id: 1, groupCode: 'Group I', title: 'Higher Secondary Group I', classLevelLabel: 'Classes XI & XII', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 2, groupCode: 'Group II', title: 'Higher Secondary Group II', classLevelLabel: 'Classes XI & XII', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 3, groupCode: 'Group III', title: 'Higher Secondary Group III', classLevelLabel: 'Classes XI & XII', medium: '', subjects: '', eligibility: '', verified: false },
      { id: 4, groupCode: 'Group IV', title: 'Higher Secondary Group IV', classLevelLabel: 'Classes XI & XII', medium: '', subjects: '', eligibility: '', verified: false }
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
    managementAccounts: [],
    contentApprovals: [],
    versionHistory: [],
    mediaStudioItems: [],
    adminNotifications: [],
    adminHeroBanner: {
      titleLine1: 'CHEVALIER ACADEMY',
      welcomeLine2: 'Welcome, {Admin Name}',
      subtitleLine3: 'School Management & Administration Portal',
      backgroundImageUri: null,
      updatedAtMillis: 0,
      updatedByName: ''
    },
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
  constructor(customStorage = null, cloudOptions = null) {
    this.storage = getStorageBackend(customStorage);
    this.listeners = new Set();
    this.state = this._load();
    this.isAdminAuthenticated = false;
    this.currentSession = null;
    this.pendingRecoveryCode = null;
    this.lastAdminInteractionAtMillis = 0;
    this.strictAuthRequired = false;

    this.cloudConfig = {
      ...FIREBASE_SYNC_CONFIG,
      ...(cloudOptions || {})
    };
    this.cloudSyncEnabled = false;
    this.cloudPollTimer = null;
    this.pinGateReady = false;
    this.pendingCloudQueue = [];
    this.lastCloudPullAt = 0;
  }

  _getBaseDocumentsUrl() {
    if (this.cloudConfig.baseUrl) {
      return this.cloudConfig.baseUrl.replace(/\/+$/, '');
    }
    const { projectId, databaseId } = this.cloudConfig;
    return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/${databaseId}/documents`;
  }

  _getFetch() {
    return this.cloudConfig.fetchImpl || (typeof globalThis !== 'undefined' ? globalThis.fetch?.bind(globalThis) : null);
  }

  enableCloudSync(options = {}) {
    this.cloudConfig = { ...this.cloudConfig, ...options };
    this.cloudSyncEnabled = true;

    const pollMs = Number(this.cloudConfig.pollIntervalMs ?? FIREBASE_SYNC_CONFIG.pollIntervalMs);
    if (pollMs > 0 && !this.cloudPollTimer) {
      this.cloudPollTimer = setInterval(() => {
        this.pullFromCloud().catch(() => {});
      }, pollMs);
      if (typeof this.cloudPollTimer?.unref === 'function') {
        this.cloudPollTimer.unref();
      }
    }

    if (typeof window !== 'undefined' && typeof document !== 'undefined') {
      window.addEventListener('focus', () => {
        this.pullFromCloud().catch(() => {});
      });
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
          this.pullFromCloud().catch(() => {});
        }
      });
    }

    return this.pullFromCloud();
  }

  disableCloudSync() {
    this.cloudSyncEnabled = false;
    if (this.cloudPollTimer) {
      clearInterval(this.cloudPollTimer);
      this.cloudPollTimer = null;
    }
  }

  async ensureCloudPinGate() {
    if (!this.cloudSyncEnabled) return false;
    const hasConfiguredAccounts = Array.isArray(this.state.managementAccounts) && this.state.managementAccounts.length > 0;
    if ((hasConfiguredAccounts || this.strictAuthRequired) && (!this.isAdminAuthenticated || !this.currentSession)) {
      return false;
    }
    if (this.pinGateReady) return true;
    const fetchFn = this._getFetch();
    if (!fetchFn) return false;

    const { tenantId, pinGateDocId, pinGateEmail, apiKey } = this.cloudConfig;
    const now = Date.now();
    const docUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/management_accounts/${pinGateDocId}${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
    const activeAcc = this.getActiveAccount();
    const firestoreRole = activeAcc?.role === MANAGEMENT_ROLES.STAFF ? 'STAFF' : 'SUPER_OWNER';
    const payload = {
      uid: pinGateDocId,
      username: activeAcc?.username || 'superowner',
      displayName: activeAcc?.displayName || this.state.admin.displayName || 'Chevalier Admin',
      email: pinGateEmail,
      role: firestoreRole,
      isActive: activeAcc ? Boolean(activeAcc.isActive) : true,
      createdAtMillis: now,
      updatedAtMillis: now,
      tenantId
    };

    try {
      const res = await fetchFn(docUrl, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fields: toFirestoreFields(payload) })
      });
      if (res.ok) {
        this.pinGateReady = true;
        return true;
      }
    } catch {
      // Non-fatal offline fallback
    }
    return false;
  }

  async _pushCloudDoc(collection, remoteId, payload) {
    if (!this.cloudSyncEnabled) return false;
    const fetchFn = this._getFetch();
    if (!fetchFn) return false;

    await this.ensureCloudPinGate();
    const { tenantId, apiKey } = this.cloudConfig;
    const docUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/${collection}/${remoteId}${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
    try {
      const res = await fetchFn(docUrl, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fields: toFirestoreFields(payload) })
      });
      return res.ok;
    } catch {
      return false;
    }
  }

  async _deleteCloudDoc(collection, remoteId) {
    if (!this.cloudSyncEnabled) return false;
    const fetchFn = this._getFetch();
    if (!fetchFn) return false;

    await this.ensureCloudPinGate();
    const { tenantId, apiKey } = this.cloudConfig;
    const docUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/${collection}/${remoteId}${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
    try {
      const res = await fetchFn(docUrl, { method: 'DELETE' });
      return res.ok;
    } catch {
      return false;
    }
  }

  _enqueueCloudOp(promiseFactory) {
    if (!this.cloudSyncEnabled) return Promise.resolve(false);
    const p = Promise.resolve().then(promiseFactory).catch(() => false);
    this.pendingCloudQueue.push(p);
    p.finally(() => {
      this.pendingCloudQueue = this.pendingCloudQueue.filter((item) => item !== p);
    });
    return p;
  }

  async flushCloudSync() {
    await Promise.allSettled([...this.pendingCloudQueue]);
  }

  async _uploadOrOffloadPostPhoto(postRemoteId, index, rawUri) {
    const clean = String(rawUri || '').trim();
    if (!clean) return null;
    if (clean.startsWith('https://') || clean.startsWith('http://') || clean.startsWith(FIREBASE_MEDIA_URI_PREFIX)) {
      return clean.length <= MAX_MEDIA_URI_CHARS ? clean : clean.slice(0, MAX_MEDIA_URI_CHARS);
    }

    const fetchFn = this._getFetch();
    const { tenantId, storageBucket, apiKey } = this.cloudConfig;
    const mediaDocId = `${POST_PHOTO_DOC_PREFIX}${postRemoteId}_${index}`.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 64);

    // 1. Try uploading binary to Firebase Storage bucket if available
    if (fetchFn && storageBucket && clean.startsWith('data:image/')) {
      try {
        const commaIdx = clean.indexOf(',');
        if (commaIdx > 0) {
          const base64Part = clean.slice(commaIdx + 1);
          const binaryBytes = typeof Buffer !== 'undefined'
            ? Buffer.from(base64Part, 'base64')
            : Uint8Array.from(atob(base64Part), (c) => c.charCodeAt(0));
          const objectPath = `schools/${tenantId}/post_photos/${mediaDocId}.jpg`;
          const storageUrl = `https://firebasestorage.googleapis.com/v0/b/${storageBucket}/o?uploadType=media&name=${encodeURIComponent(objectPath)}`;
          const uploadRes = await fetchFn(storageUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'image/jpeg' },
            body: binaryBytes
          });
          if (uploadRes && uploadRes.ok) {
            return `https://firebasestorage.googleapis.com/v0/b/${storageBucket}/o/${encodeURIComponent(objectPath)}?alt=media`;
          }
        }
      } catch {
        // Fall through to dedicated media document offload
      }
    }

    // 2. Dedicated Firestore media document offload (keeps school_posts document < 2 KB)
    if (clean.length > MAX_MEDIA_URI_CHARS) {
      return null;
    }
    const now = Date.now();
    const pushed = await this._pushCloudDoc('gallery_media', mediaDocId, {
      albumRemoteId: 'post_media_offload',
      title: `Post Photo ${index + 1}`,
      caption: null,
      mediaUri: clean,
      mediaType: 'IMAGE',
      displayOrder: index,
      addedAtMillis: now,
      isVerified: true,
      isPublished: true,
      isApproved: true,
      approvalStatus: 'APPROVED',
      remoteId: mediaDocId,
      tenantId,
      syncState: 'SYNCED',
      deletedAtMillis: null
    });
    return pushed ? `${FIREBASE_MEDIA_URI_PREFIX}${mediaDocId}` : clean;
  }

  async _resolvePostPhotoUri(uri, mediaLookup = null) {
    const clean = String(uri || '').trim();
    if (!clean.startsWith(FIREBASE_MEDIA_URI_PREFIX)) return clean;
    const mediaDocId = clean.slice(FIREBASE_MEDIA_URI_PREFIX.length).trim();
    if (!mediaDocId) return '';
    if (mediaLookup && mediaLookup.has(mediaDocId)) {
      return mediaLookup.get(mediaDocId) || clean;
    }
    const fetchFn = this._getFetch();
    if (!fetchFn) return clean;
    const { tenantId, apiKey } = this.cloudConfig;
    const docUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/gallery_media/${mediaDocId}${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
    try {
      const res = await fetchFn(docUrl, { method: 'GET' });
      if (!res.ok) return clean;
      const doc = fromFirestoreDocument(await res.json());
      return doc?.data?.mediaUri || clean;
    } catch {
      return clean;
    }
  }

  async _queryCloudCollection(collectionId, filters) {
    const fetchFn = this._getFetch();
    if (!fetchFn) return null;
    const { tenantId, apiKey } = this.cloudConfig;
    const listUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/${collectionId}?pageSize=200${apiKey ? `&key=${encodeURIComponent(apiKey)}` : ''}`;

    const optionalApprovalDefaults = {
      isVerified: true,
      isPublished: true,
      isApproved: true,
      approvalStatus: 'APPROVED',
      status: 'PUBLISHED'
    };

    const applyClientFilters = (docs) => {
      if (!Array.isArray(filters) || filters.length === 0) {
        return docs;
      }
      return docs.filter((doc) => {
        const data = doc.data || {};
        return filters.every(([fieldPath, op, expected]) => {
          let actual = data[fieldPath];
          if (actual === undefined && fieldPath in optionalApprovalDefaults) {
            actual = optionalApprovalDefaults[fieldPath];
          }
          if (op === 'IS_NULL' || expected === null) {
            return actual === null || actual === undefined;
          }
          if (op === 'EQUAL') {
            return actual === expected;
          }
          return true;
        });
      });
    };

    try {
      // 1. Prefer Firestore :runQuery POST first so live Firestore Security Rules return HTTP 200
      // with zero 403 console noise on every poll cycle.
      const runQueryUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}:runQuery${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
      const structuredQuery = {
        from: [{ collectionId }],
        limit: 200
      };
      if (Array.isArray(filters) && filters.length > 0) {
        const structuredFilters = filters.map(([fieldPath, op, value]) => {
          if (value === null || op === 'IS_NULL') {
            return {
              unaryFilter: {
                field: { fieldPath },
                op: 'IS_NULL'
              }
            };
          }
          return {
            fieldFilter: {
              field: { fieldPath },
              op,
              value: toFirestoreValue(value)
            }
          };
        });
        structuredQuery.where =
          structuredFilters.length === 1
            ? structuredFilters[0]
            : {
                compositeFilter: {
                  op: 'AND',
                  filters: structuredFilters
                }
              };
      }

      const qRes = await fetchFn(runQueryUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ structuredQuery })
      });
      if (qRes && qRes.ok) {
        const rows = await qRes.json();
        if (!Array.isArray(rows)) return [];
        const parsedDocs = rows
          .filter((r) => r && r.document)
          .map((r) => fromFirestoreDocument(r.document))
          .filter(Boolean);
        return applyClientFilters(parsedDocs);
      }

      // 2. Fallback to REST GET list only when :runQuery is unavailable (e.g., simplified local/mock HTTP endpoints)
      const res = await fetchFn(listUrl, {
        method: 'GET',
        headers: { Accept: 'application/json' }
      });
      if (res && res.ok) {
        const payload = await res.json();
        const rawDocs = Array.isArray(payload?.documents) ? payload.documents : [];
        const parsedDocs = rawDocs.map((d) => fromFirestoreDocument(d)).filter(Boolean);
        return applyClientFilters(parsedDocs);
      }

      return null;
    } catch {
      return null;
    }
  }

  async pullFromCloud() {
    if (!this.cloudSyncEnabled) return false;
    const { tenantId } = this.cloudConfig;

    const cmsFilters = [
      ['tenantId', 'EQUAL', tenantId],
      ['isVerified', 'EQUAL', true],
      ['isPublished', 'EQUAL', true],
      ['isApproved', 'EQUAL', true],
      ['approvalStatus', 'EQUAL', 'APPROVED'],
      ['deletedAtMillis', 'IS_NULL', null]
    ];

    const socialFilters = [
      ['tenantId', 'EQUAL', tenantId],
      ['status', 'EQUAL', 'PUBLISHED'],
      ['deletedAtMillis', 'IS_NULL', null]
    ];

    const [
      profileDocs,
      heroDocs,
      academicDocs,
      courseDocs,
      facilityDocs,
      transportDocs,
      newsDocs,
      announcementDocs,
      albumDocs,
      mediaDocs,
      socialLinkDocs,
      postDocs,
      highlightDocs
    ] = await Promise.all([
      this._queryCloudCollection('school_profile', [['tenantId', 'EQUAL', tenantId]]),
      this._queryCloudCollection('campus_hero_photos', cmsFilters),
      this._queryCloudCollection('academic_info', cmsFilters),
      this._queryCloudCollection('higher_secondary_courses', cmsFilters),
      this._queryCloudCollection('facilities', cmsFilters),
      this._queryCloudCollection('transport_routes', cmsFilters),
      this._queryCloudCollection('news_items', cmsFilters),
      this._queryCloudCollection('announcements', cmsFilters),
      this._queryCloudCollection('gallery_albums', cmsFilters),
      this._queryCloudCollection('gallery_media', cmsFilters),
      this._queryCloudCollection('social_media_links', cmsFilters),
      this._queryCloudCollection('school_posts', socialFilters),
      this._queryCloudCollection('school_highlights', socialFilters)
    ]);

    let changed = false;
    const prevSnapshot = JSON.stringify({
      profile: this.state.profile,
      heroPhotos: this.state.heroPhotos,
      academics: this.state.academics,
      courses: this.state.courses,
      facilities: this.state.facilities,
      transportRoutes: this.state.transportRoutes,
      news: this.state.news,
      announcements: this.state.announcements,
      galleryAlbums: this.state.galleryAlbums,
      galleryMedia: this.state.galleryMedia,
      socialLinks: this.state.socialLinks,
      schoolPosts: this.state.schoolPosts,
      schoolHighlights: this.state.schoolHighlights
    });

    // 1. School Profile (pick the most recently updated profile document across Android & Website)
    if (Array.isArray(profileDocs) && profileDocs.length > 0) {
      const sortedProfiles = [...profileDocs].sort(
        (a, b) => Number(b.data?.updatedAtMillis || 0) - Number(a.data?.updatedAtMillis || 0)
      );
      const p = sortedProfiles[0].data;
      this.state.profile = {
        ...this.state.profile,
        schoolName: p.schoolName || this.state.profile.schoolName,
        shortName: p.shortName || this.state.profile.shortName,
        motto: p.motto || this.state.profile.motto,
        addressLine: p.addressLine || this.state.profile.addressLine,
        emblemMark: p.emblemMark || this.state.profile.emblemMark,
        poweredBy: p.poweredBy || this.state.profile.poweredBy,
        welcomeIntroduction: p.welcomeIntroduction ?? this.state.profile.welcomeIntroduction,
        mediumAndGradesNote: p.mediumAndGradesNote ?? this.state.profile.mediumAndGradesNote,
        mapCoordinatesOrQuery: p.mapCoordinatesOrQuery ?? this.state.profile.mapCoordinatesOrQuery,
        phone: p.phone ?? this.state.profile.phone,
        email: p.email ?? this.state.profile.email,
        website: p.website ?? this.state.profile.website,
        officeHours: p.officeHours ?? this.state.profile.officeHours,
        principalName: p.principalName ?? this.state.profile.principalName,
        history: p.history ?? this.state.profile.history,
        vision: p.vision ?? this.state.profile.vision,
        mission: p.mission ?? this.state.profile.mission,
        affiliationNumber: p.affiliationNumber ?? this.state.profile.affiliationNumber,
        profileImageUri: p.profileImageUri ?? this.state.profile.profileImageUri,
        aboutImageUri: p.aboutImageUri ?? this.state.profile.aboutImageUri
      };
    }

    // 2. Hero Photos
    if (Array.isArray(heroDocs)) {
      this.state.heroPhotos = heroDocs
        .sort((a, b) => (a.data.displayOrder || 0) - (b.data.displayOrder || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          imageUrl: doc.data.imageUri || '',
          imageUri: doc.data.imageUri || '',
          caption: doc.data.caption || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 3. Academics
    if (Array.isArray(academicDocs)) {
      this.state.academics = academicDocs
        .sort((a, b) => (a.data.displayOrder || 0) - (b.data.displayOrder || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          title: doc.data.title || '',
          category: doc.data.category || 'General',
          level: doc.data.category || 'General',
          description: doc.data.description || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 4. Higher Secondary Courses
    if (Array.isArray(courseDocs) && courseDocs.length > 0) {
      const updatedCourses = [...this.state.courses];
      for (const doc of courseDocs) {
        const d = doc.data;
        const idx = updatedCourses.findIndex(
          (c) => c.remoteId === doc.remoteId || c.groupCode.toLowerCase() === String(d.groupCode || '').toLowerCase()
        );
        const mapped = {
          id: idx >= 0 ? updatedCourses[idx].id : stableNumericId(doc.remoteId, updatedCourses.length + 1),
          remoteId: doc.remoteId,
          groupCode: d.groupCode || 'Group I',
          title: d.streamTitle || '',
          medium: d.mediumOfInstruction || '',
          subjects: d.subjectsSummary || '',
          eligibility: d.eligibilityNote || '',
          verified: Boolean(d.isVerified && d.isPublished),
          displayOrder: Number(d.displayOrder ?? (idx >= 0 ? idx : updatedCourses.length))
        };
        if (idx >= 0) {
          updatedCourses[idx] = mapped;
        } else {
          updatedCourses.push(mapped);
        }
      }
      this.state.courses = updatedCourses;
    }

    // 5. Facilities
    if (Array.isArray(facilityDocs)) {
      this.state.facilities = facilityDocs
        .sort((a, b) => (a.data.displayOrder || 0) - (b.data.displayOrder || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          name: doc.data.name || '',
          category: doc.data.category || 'Campus',
          description: doc.data.description || '',
          capacityOrSpec: doc.data.capacityOrSpec || '',
          imageUrl: doc.data.imageUri || '',
          imageUri: doc.data.imageUri || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 6. Transport Routes
    if (Array.isArray(transportDocs)) {
      this.state.transportRoutes = transportDocs
        .sort((a, b) => (a.data.displayOrder || 0) - (b.data.displayOrder || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          routeNumber: doc.data.routeNumber || '',
          routeName: doc.data.routeName || '',
          stops: doc.data.stopsSummary || '',
          stopsSummary: doc.data.stopsSummary || '',
          timings: doc.data.morningTiming || doc.data.afternoonTiming || '',
          morningTiming: doc.data.morningTiming || '',
          afternoonTiming: doc.data.afternoonTiming || '',
          coordinatorContact: doc.data.coordinatorContact || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 7. News Items
    if (Array.isArray(newsDocs)) {
      this.state.news = newsDocs
        .sort((a, b) => {
          const orderDiff = Number(a.data.displayOrder ?? 0) - Number(b.data.displayOrder ?? 0);
          return orderDiff !== 0 ? orderDiff : (b.data.publishedAtMillis || 0) - (a.data.publishedAtMillis || 0);
        })
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          title: doc.data.title || '',
          summary: doc.data.summary || doc.data.fullBody || '',
          fullBody: doc.data.fullBody || doc.data.summary || '',
          publishedDateText: doc.data.publishedDateText || '',
          publishedAtMillis: doc.data.publishedAtMillis || Date.now(),
          isFeatured: Boolean(doc.data.isFeatured),
          imageUrl: doc.data.imageUri || '',
          imageUri: doc.data.imageUri || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 8. Announcements
    if (Array.isArray(announcementDocs)) {
      this.state.announcements = announcementDocs
        .sort((a, b) => {
          const orderDiff = Number(a.data.displayOrder ?? 0) - Number(b.data.displayOrder ?? 0);
          return orderDiff !== 0 ? orderDiff : (b.data.issuedAtMillis || 0) - (a.data.issuedAtMillis || 0);
        })
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          title: doc.data.title || '',
          message: doc.data.message || '',
          priorityLabel: doc.data.priorityLabel || 'Official Notice',
          issuedDateText: doc.data.issuedDateText || '',
          issuedAtMillis: doc.data.issuedAtMillis || Date.now(),
          imageUrl: doc.data.imageUri || '',
          imageUri: doc.data.imageUri || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 9. Gallery Albums
    if (Array.isArray(albumDocs)) {
      this.state.galleryAlbums = albumDocs
        .sort((a, b) => (a.data.displayOrder || 0) - (b.data.displayOrder || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          name: doc.data.title || 'Campus Highlights',
          category: doc.data.category || 'Campus',
          coverMediaUri: doc.data.coverMediaUri || '',
          displayOrder: Number(doc.data.displayOrder ?? idx)
        }));
    }

    // 10. Gallery Media (filter out post_photo_* offload documents from public Gallery)
    const offloadedMediaMap = new Map();
    if (Array.isArray(mediaDocs)) {
      for (const doc of mediaDocs) {
        if (doc?.remoteId && doc.data?.mediaUri) {
          offloadedMediaMap.set(doc.remoteId, doc.data.mediaUri);
        }
      }
      const albumNameByRemoteId = new Map(
        (this.state.galleryAlbums || []).map((a) => [a.remoteId, a.name])
      );
      this.state.galleryMedia = mediaDocs
        .filter((doc) => !String(doc.remoteId || '').startsWith(POST_PHOTO_DOC_PREFIX) && doc.data?.albumRemoteId !== 'post_media_offload')
        .sort((a, b) => {
          const orderDiff = Number(a.data.displayOrder ?? 0) - Number(b.data.displayOrder ?? 0);
          return orderDiff !== 0 ? orderDiff : (b.data.addedAtMillis || 0) - (a.data.addedAtMillis || 0);
        })
        .map((doc, idx) => {
          const albumName =
            albumNameByRemoteId.get(doc.data.albumRemoteId) ||
            doc.data.title ||
            'Campus Highlights';
          return {
            id: stableNumericId(doc.remoteId, idx + 1),
            remoteId: doc.remoteId,
            albumRemoteId: doc.data.albumRemoteId || 'gallery_albums_1',
            album: albumName,
            category: 'Campus',
            caption: doc.data.caption || doc.data.title || '',
            imageUrl: doc.data.mediaUri || '',
            mediaUri: doc.data.mediaUri || '',
            mediaType: doc.data.mediaType || 'IMAGE',
            displayOrder: Number(doc.data.displayOrder ?? idx)
          };
        });
    }

    // 11. Social Media Links
    if (Array.isArray(socialLinkDocs) && socialLinkDocs.length > 0) {
      const updatedLinks = [...this.state.socialLinks];
      for (const doc of socialLinkDocs) {
        const d = doc.data;
        const idx = updatedLinks.findIndex(
          (s) => s.remoteId === doc.remoteId || s.platformName.toLowerCase() === String(d.platformName || '').toLowerCase()
        );
        const mapped = {
          id: idx >= 0 ? updatedLinks[idx].id : stableNumericId(doc.remoteId, updatedLinks.length + 1),
          remoteId: doc.remoteId,
          platformName: d.platformName || '',
          handle: d.handleOrLabel || '',
          url: d.url || '',
          active: Boolean(d.isPublished && d.url)
        };
        if (idx >= 0) {
          updatedLinks[idx] = mapped;
        } else {
          updatedLinks.push(mapped);
        }
      }
      this.state.socialLinks = updatedLinks;
    }

    // 12. School Posts (resolve firebase-media:// post photo references transparently)
    if (Array.isArray(postDocs)) {
      const unpublishedLocal = (this.state.schoolPosts || []).filter(
        (p) => p.status !== SCHOOL_POST_STATUS.PUBLISHED
      );
      const publishedRemote = await Promise.all(
        postDocs.map(async (doc, idx) => {
          const d = doc.data;
          const rawPhotoUris = Array.isArray(d.photoUris) ? d.photoUris : [];
          const photoUrls = (
            await Promise.all(rawPhotoUris.map((u) => this._resolvePostPhotoUri(u, offloadedMediaMap)))
          ).filter(Boolean);
          return {
            id: stableNumericId(doc.remoteId, idx + 1),
            remoteId: doc.remoteId,
            postType: d.postType || SCHOOL_POST_TYPES.PHOTO,
            category: d.category || 'General',
            title: d.title || '',
            description: d.description || '',
            youtubeUrl: d.youtubeUrl || null,
            customThumbnailUrl: d.customThumbnailUri || null,
            photoUrls,
            status: d.status || SCHOOL_POST_STATUS.PUBLISHED,
            isFeatured: Boolean(d.isFeatured),
            createdAtMillis: Number(d.createdAtMillis || Date.now()),
            publishedAtMillis: Number(d.publishedAtMillis || d.createdAtMillis || Date.now()),
            updatedAtMillis: Number(d.updatedAtMillis || Date.now())
          };
        })
      );
      const remoteIds = new Set(publishedRemote.map((p) => p.remoteId));
      this.state.schoolPosts = [
        ...publishedRemote,
        ...unpublishedLocal.filter((p) => !p.remoteId || !remoteIds.has(p.remoteId))
      ];
    }

    // 13. School Highlights
    if (Array.isArray(highlightDocs)) {
      this.state.schoolHighlights = highlightDocs.map((doc, idx) => {
        const d = doc.data;
        const publishedAt = Number(d.publishedAtMillis || Date.now());
        return {
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          title: d.title || '',
          youtubeShortsUrl: d.youtubeShortsUrl || '',
          status: d.status || SCHOOL_POST_STATUS.PUBLISHED,
          createdAtMillis: Number(d.createdAtMillis || publishedAt),
          publishedAtMillis: publishedAt,
          expiresAtMillis: Number(d.expiresAtMillis || publishedAt + HIGHLIGHT_TTL_MILLIS)
        };
      });
    }

    const nextSnapshot = JSON.stringify({
      profile: this.state.profile,
      heroPhotos: this.state.heroPhotos,
      academics: this.state.academics,
      courses: this.state.courses,
      facilities: this.state.facilities,
      transportRoutes: this.state.transportRoutes,
      news: this.state.news,
      announcements: this.state.announcements,
      galleryAlbums: this.state.galleryAlbums,
      galleryMedia: this.state.galleryMedia,
      socialLinks: this.state.socialLinks,
      schoolPosts: this.state.schoolPosts,
      schoolHighlights: this.state.schoolHighlights
    });

    changed = prevSnapshot !== nextSnapshot;
    this.lastCloudPullAt = Date.now();
    if (changed) {
      this._save();
    }
    return changed;
  }

  _load() {
    const defaults = createVerifiedDefaultState();
    try {
      const raw = this.storage.getItem(STORAGE_KEY);
      if (!raw) return defaults;
      const parsed = JSON.parse(raw);
      const managementAccounts = Array.isArray(parsed.managementAccounts)
        ? parsed.managementAccounts.map((acc) => ({
            ...acc,
            permissions: normalizePermissionsMatrix(acc.permissions, acc.role)
          }))
        : [];
      const defaultV2 = createDefaultAcademicsV2State();
      const rawV2 = parsed.academicsV2 || {};
      return {
        ...defaults,
        ...parsed,
        academicsV2: {
          records: Array.isArray(rawV2.records) ? rawV2.records : [],
          galleryAlbums: Array.isArray(rawV2.galleryAlbums) ? rawV2.galleryAlbums : [],
          galleryPhotos: Array.isArray(rawV2.galleryPhotos) ? rawV2.galleryPhotos : [],
          facultyProfiles: Array.isArray(rawV2.facultyProfiles) ? rawV2.facultyProfiles : [],
          ownerPrivateProfile: {
            ...defaultV2.ownerPrivateProfile,
            ...(rawV2.ownerPrivateProfile || {})
          },
          staffAccessRequests: Array.isArray(rawV2.staffAccessRequests) ? rawV2.staffAccessRequests : [],
          accessAuditEvents: Array.isArray(rawV2.accessAuditEvents) ? rawV2.accessAuditEvents : []
        },
        schoolPosts: Array.isArray(parsed.schoolPosts) ? parsed.schoolPosts : [],
        schoolHighlights: Array.isArray(parsed.schoolHighlights) ? parsed.schoolHighlights : [],
        managementAccounts,
        contentApprovals: Array.isArray(parsed.contentApprovals) ? parsed.contentApprovals : [],
        versionHistory: Array.isArray(parsed.versionHistory) ? parsed.versionHistory : [],
        mediaStudioItems: Array.isArray(parsed.mediaStudioItems) ? parsed.mediaStudioItems : [],
        adminNotifications: Array.isArray(parsed.adminNotifications) ? parsed.adminNotifications : [],
        adminHeroBanner: { ...defaults.adminHeroBanner, ...(parsed.adminHeroBanner || {}) },
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

  /* ==========================================================================
     BASE 3 — STAFF & ADMIN AUTHENTICATION, PIN SECURITY, ROLES & 20x5 RBAC
     ========================================================================== */

  hasMasterAdmin() {
    return (this.state.managementAccounts || []).some(
      (a) => a.role === MANAGEMENT_ROLES.MASTER_ADMIN
    );
  }

  hasAdminPin() {
    return this.hasMasterAdmin() || Boolean(this.state.admin.pinHash && this.state.admin.pinSalt);
  }

  getManagementAccounts() {
    return [...(this.state.managementAccounts || [])].sort((a, b) => {
      const rank = { MASTER_ADMIN: 0, SUPER_OWNER: 1, STAFF: 2 };
      const diff = (rank[a.role] ?? 9) - (rank[b.role] ?? 9);
      if (diff !== 0) return diff;
      return String(a.displayName || '').localeCompare(String(b.displayName || ''));
    });
  }

  getActiveManagementAccounts() {
    return this.getManagementAccounts().filter((a) => Boolean(a.isActive));
  }

  getActiveAccount() {
    if (!this.isAdminAuthenticated) return null;
    if (this.currentSession?.accountId) {
      const found = (this.state.managementAccounts || []).find(
        (a) => a.id === this.currentSession.accountId
      );
      if (found && found.isActive) return found;
      return null;
    }
    // Legacy single-PIN session bridge
    if (this.state.admin.pinHash) {
      return {
        id: 1,
        username: 'admin',
        displayName: this.state.admin.displayName || 'School Administrator',
        role: MANAGEMENT_ROLES.MASTER_ADMIN,
        isActive: true,
        permissions: buildDefaultPermissionsForRole(MANAGEMENT_ROLES.MASTER_ADMIN)
      };
    }
    return null;
  }

  getCurrentSession() {
    const account = this.getActiveAccount();
    if (!account) return null;
    return {
      account,
      role: account.role,
      permissions: normalizePermissionsMatrix(account.permissions, account.role)
    };
  }

  getCurrentRole() {
    return this.getActiveAccount()?.role || null;
  }

  isMasterAdmin() {
    return this.getCurrentRole() === MANAGEMENT_ROLES.MASTER_ADMIN;
  }

  hasPermission(sectionKey, action = PERMISSION_ACTIONS.VIEW) {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return false;
    const secMeta = MANAGEMENT_SECTION_MAP[sectionKey];
    if (action === PERMISSION_ACTIONS.PUBLISH && secMeta && !secMeta.supportsPublish) {
      return false;
    }
    if (session.account.role === MANAGEMENT_ROLES.MASTER_ADMIN) {
      return true;
    }
    const row = session.permissions.find((p) => p.sectionKey === sectionKey);
    if (!row) return false;
    switch (action) {
      case PERMISSION_ACTIONS.VIEW:
        return Boolean(row.canView);
      case PERMISSION_ACTIONS.CREATE:
        return Boolean(row.canCreate);
      case PERMISSION_ACTIONS.EDIT:
        return Boolean(row.canEdit);
      case PERMISSION_ACTIONS.DELETE:
        return Boolean(row.canDelete);
      case PERMISSION_ACTIONS.PUBLISH:
        return Boolean(row.canPublish) && Boolean(secMeta?.supportsPublish);
      default:
        return false;
    }
  }

  _shouldEnforceRbac() {
    return (
      this.strictAuthRequired ||
      (Array.isArray(this.state.managementAccounts) && this.state.managementAccounts.length > 0)
    );
  }

  requirePermission(sectionKey, action) {
    if (!this._shouldEnforceRbac()) return;
    const session = this.getCurrentSession();
    if (!session || !this.isAdminAuthenticated) {
      throw new Error(`Authentication required: active admin session needed for ${action} on ${sectionKey}.`);
    }
    if (!this.hasPermission(sectionKey, action)) {
      throw new Error(`Permission denied: role ${session.role} lacks ${action} permission on ${sectionKey}.`);
    }
  }

  requireMasterAdmin(operationLabel = 'manage staff accounts') {
    const session = this.getCurrentSession();
    if (!session || !this.isAdminAuthenticated || session.role !== MANAGEMENT_ROLES.MASTER_ADMIN) {
      throw new Error(`Only Master Admin can ${operationLabel}.`);
    }
    return session.account;
  }

  canAccessAdminCard(cardId) {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return false;
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN) return true;
    const card = ADMIN_DASHBOARD_CARDS.find((c) => c.id === cardId);
    if (!card) return false;
    if (card.id === 'management_tools' && this.canAccessApprovalCenter()) {
      return true;
    }
    return card.requiredViewSections.some((secKey) =>
      this.hasPermission(secKey, PERMISSION_ACTIONS.VIEW)
    );
  }

  canAccessApprovalCenter() {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return false;
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN) return true;
    return MANAGEMENT_SECTIONS.some(
      (sec) => sec.supportsPublish && this.hasPermission(sec.key, PERMISSION_ACTIONS.PUBLISH)
    );
  }

  canAccessVersionHistory() {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return false;
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN) return true;
    return MANAGEMENT_SECTIONS.some((sec) => this.hasPermission(sec.key, PERMISSION_ACTIONS.EDIT));
  }

  canEditAdminHeroBanner() {
    return (
      this.isMasterAdmin() ||
      this.hasPermission('LOGO_BANNER', PERMISSION_ACTIONS.EDIT) ||
      this.hasPermission('SCHOOL_PROFILE', PERMISSION_ACTIONS.EDIT)
    );
  }

  setupMasterAdmin(displayName, username, pin, confirmPin = pin, nowMillis = Date.now()) {
    if (this.hasMasterAdmin()) {
      throw new Error('Master Admin account is already configured.');
    }
    const cleanName = String(displayName || '').trim();
    const cleanUsername = String(username || '').trim().toLowerCase();
    const cleanPin = String(pin || '').trim();
    const cleanConfirm = String(confirmPin || '').trim();

    if (!cleanName || !cleanUsername) {
      throw new Error('Display name and username are required.');
    }
    if (!PinSecurityManager.isValidPinFormat(cleanPin)) {
      throw new Error('PIN must be at least 6 numeric digits.');
    }
    if (cleanPin !== cleanConfirm) {
      throw new Error('PIN and confirmation PIN do not match.');
    }

    const pinSaltBase64 = PinSecurityManager.generateSaltBase64();
    const pinVerifierBase64 = PinSecurityManager.deriveVerifierBase64(cleanPin, pinSaltBase64);
    const recoveryCode = PinSecurityManager.generateRecoveryCode();
    const normalizedRecovery = recoveryCode.replace(/[-\s]/g, '').toUpperCase();
    const recoverySaltBase64 = PinSecurityManager.generateSaltBase64();
    const recoveryVerifierBase64 = PinSecurityManager.deriveVerifierBase64(
      normalizedRecovery,
      recoverySaltBase64
    );

    const account = {
      id: 1,
      username: cleanUsername,
      displayName: cleanName,
      role: MANAGEMENT_ROLES.MASTER_ADMIN,
      pinSaltBase64,
      pinVerifierBase64,
      recoverySaltBase64,
      recoveryVerifierBase64,
      isActive: true,
      failedAttempts: 0,
      lockoutUntilMillis: 0,
      createdAtMillis: nowMillis,
      updatedAtMillis: nowMillis,
      permissions: buildDefaultPermissionsForRole(MANAGEMENT_ROLES.MASTER_ADMIN)
    };

    this.state.managementAccounts = [account];
    this.state.admin.displayName = cleanName;
    this.currentSession = {
      accountId: account.id,
      username: account.username,
      displayName: account.displayName,
      role: account.role
    };
    this.isAdminAuthenticated = true;
    this.pendingRecoveryCode = recoveryCode;
    this.lastAdminInteractionAtMillis = nowMillis;
    this.pinGateReady = false;

    this.logActivity(
      'MASTER_ADMIN_SETUP',
      'STAFF_MANAGEMENT',
      'Initial Master Admin account configured with salted PIN & recovery key.',
      account,
      nowMillis
    );
    this.recordNotification(
      'MASTER_SETUP',
      'Master Admin Configured',
      `Master Admin account (${cleanName}) was initialized.`,
      'STAFF_MANAGEMENT',
      nowMillis
    );
    this._save();
    this._enqueueCloudOp(() => this.ensureCloudPinGate());

    return {
      ok: true,
      account: this._sanitizeAccountForPublicView(account),
      recoveryCode
    };
  }

  confirmRecoveryKeySaved() {
    this.pendingRecoveryCode = null;
    this.listeners.forEach((fn) => fn(this.state));
  }

  loginWithPin(username, pin, nowMillis = Date.now()) {
    const cleanUsername = String(username || '').trim().toLowerCase();
    const cleanPin = String(pin || '').trim();
    const accounts = this.state.managementAccounts || [];
    const idx = accounts.findIndex((a) => a.username.toLowerCase() === cleanUsername);

    if (idx === -1) {
      return {
        ok: false,
        status: 'INVALID_CREDENTIALS',
        remainingAttemptsBeforeLockout: 3,
        lockoutDurationMillis: 0,
        error: 'Invalid username or PIN.'
      };
    }

    const account = accounts[idx];
    if (!account.isActive) {
      this.logActivity(
        'LOGIN_DENIED_DEACTIVATED',
        'STAFF_MANAGEMENT',
        `Blocked login attempt for deactivated account @${account.username}`,
        account,
        nowMillis
      );
      this._save();
      return {
        ok: false,
        status: 'DEACTIVATED',
        error: 'This staff account has been deactivated by Master Admin.'
      };
    }

    if (account.lockoutUntilMillis && account.lockoutUntilMillis > nowMillis) {
      const remainingSeconds = Math.ceil((account.lockoutUntilMillis - nowMillis) / 1000);
      return {
        ok: false,
        status: 'LOCKED_OUT',
        remainingSeconds,
        error: `Account temporarily locked due to failed PIN attempts. Try again in ${remainingSeconds}s.`
      };
    }

    const verified = PinSecurityManager.verifySecretConstantTime(
      cleanPin,
      account.pinSaltBase64,
      account.pinVerifierBase64
    );

    if (!verified) {
      const newAttempts = Number(account.failedAttempts || 0) + 1;
      const lockoutDurationMillis = PinSecurityManager.computeLockoutDurationMillis(newAttempts);
      const lockoutUntilMillis = lockoutDurationMillis > 0 ? nowMillis + lockoutDurationMillis : 0;
      const updatedAccount = {
        ...account,
        failedAttempts: newAttempts,
        lockoutUntilMillis,
        updatedAtMillis: nowMillis
      };
      this.state.managementAccounts[idx] = updatedAccount;
      this.logActivity(
        'FAILED_LOGIN',
        'STAFF_MANAGEMENT',
        `Failed PIN attempt #${newAttempts} for @${account.username}`,
        account,
        nowMillis
      );
      this._save();
      const remainingBeforeLock = Math.max(0, 3 - newAttempts);
      const errorMsg =
        lockoutDurationMillis > 0
          ? `Incorrect PIN. Account locked for ${lockoutDurationMillis / 1000}s.`
          : `Incorrect PIN. ${remainingBeforeLock} attempt(s) left before progressive lockout.`;
      return {
        ok: false,
        status: 'INVALID_CREDENTIALS',
        remainingAttemptsBeforeLockout: remainingBeforeLock,
        lockoutDurationMillis,
        error: errorMsg
      };
    }

    const resetAccount = {
      ...account,
      failedAttempts: 0,
      lockoutUntilMillis: 0,
      updatedAtMillis: nowMillis
    };
    this.state.managementAccounts[idx] = resetAccount;
    this.state.admin.displayName = resetAccount.displayName;
    this.currentSession = {
      accountId: resetAccount.id,
      username: resetAccount.username,
      displayName: resetAccount.displayName,
      role: resetAccount.role
    };
    this.isAdminAuthenticated = true;
    this.lastAdminInteractionAtMillis = nowMillis;
    this.pinGateReady = false;

    this.logActivity(
      'LOGIN',
      'STAFF_MANAGEMENT',
      `${resetAccount.displayName} (${resetAccount.role}) signed in.`,
      resetAccount,
      nowMillis
    );
    this._save();
    this._enqueueCloudOp(() => this.ensureCloudPinGate());

    return {
      ok: true,
      status: 'SUCCESS',
      account: this._sanitizeAccountForPublicView(resetAccount),
      session: this.getCurrentSession()
    };
  }

  recoverMasterAdminPin(recoveryCode, newPin, confirmPin = newPin, nowMillis = Date.now()) {
    const cleanPin = String(newPin || '').trim();
    const cleanConfirm = String(confirmPin || '').trim();
    if (!PinSecurityManager.isValidPinFormat(cleanPin)) {
      throw new Error('New PIN must be at least 6 numeric digits.');
    }
    if (cleanPin !== cleanConfirm) {
      throw new Error('New PIN and confirmation PIN do not match.');
    }
    const accounts = this.state.managementAccounts || [];
    const idx = accounts.findIndex((a) => a.role === MANAGEMENT_ROLES.MASTER_ADMIN);
    if (idx === -1) {
      throw new Error('Master Admin account not found.');
    }
    const master = accounts[idx];
    const normalizedInput = String(recoveryCode || '').replace(/[-\s]/g, '').toUpperCase();
    const verified = PinSecurityManager.verifySecretConstantTime(
      normalizedInput,
      master.recoverySaltBase64,
      master.recoveryVerifierBase64
    );

    if (!verified) {
      this.logActivity(
        'PIN_RECOVERY_FAILED',
        'STAFF_MANAGEMENT',
        'Invalid recovery code attempt on Master Admin.',
        master,
        nowMillis
      );
      this._save();
      throw new Error('Invalid Master Recovery Code.');
    }

    const newPinSalt = PinSecurityManager.generateSaltBase64();
    const newPinVerifier = PinSecurityManager.deriveVerifierBase64(cleanPin, newPinSalt);
    const rotatedRecoveryCode = PinSecurityManager.generateRecoveryCode();
    const normalizedRotated = rotatedRecoveryCode.replace(/[-\s]/g, '').toUpperCase();
    const newRecoverySalt = PinSecurityManager.generateSaltBase64();
    const newRecoveryVerifier = PinSecurityManager.deriveVerifierBase64(
      normalizedRotated,
      newRecoverySalt
    );

    const updatedMaster = {
      ...master,
      pinSaltBase64: newPinSalt,
      pinVerifierBase64: newPinVerifier,
      recoverySaltBase64: newRecoverySalt,
      recoveryVerifierBase64: newRecoveryVerifier,
      failedAttempts: 0,
      lockoutUntilMillis: 0,
      updatedAtMillis: nowMillis
    };
    this.state.managementAccounts[idx] = updatedMaster;
    this.logActivity(
      'MASTER_PIN_RECOVERED',
      'STAFF_MANAGEMENT',
      'Master Admin PIN reset using recovery code; recovery key rotated.',
      updatedMaster,
      nowMillis
    );
    this.recordNotification(
      'SECURITY',
      'Master PIN Reset & Recovery Rotated',
      'Master Admin PIN was reset via recovery code and a new recovery key was issued.',
      'STAFF_MANAGEMENT',
      nowMillis
    );
    this._save();

    return {
      ok: true,
      account: this._sanitizeAccountForPublicView(updatedMaster),
      recoveryCode: rotatedRecoveryCode
    };
  }

  createStaffAccount(input, nowMillis = Date.now()) {
    const masterActor = this.requireMasterAdmin('create staff accounts');
    const cleanUsername = String(input?.username || '').trim().toLowerCase();
    const cleanDisplayName = String(input?.displayName || '').trim();
    const cleanPin = String(input?.initialPin ?? input?.pin ?? '').trim();
    const role = input?.role || MANAGEMENT_ROLES.STAFF;

    if (role !== MANAGEMENT_ROLES.STAFF && role !== MANAGEMENT_ROLES.SUPER_OWNER) {
      throw new Error('Invalid staff role.');
    }
    if (role === MANAGEMENT_ROLES.SUPER_OWNER) {
      const existingSuperOwners = (this.state.managementAccounts || []).filter(
        (a) => a.role === MANAGEMENT_ROLES.SUPER_OWNER
      ).length;
      if (existingSuperOwners >= 2) {
        throw new Error('Maximum of 2 Super Owner accounts allowed.');
      }
    }
    if (!cleanUsername || !cleanDisplayName) {
      throw new Error('Username and display name are required.');
    }
    if (!PinSecurityManager.isValidPinFormat(cleanPin)) {
      throw new Error('Staff PIN must be at least 6 numeric digits.');
    }
    const duplicate = (this.state.managementAccounts || []).some(
      (a) => a.username.toLowerCase() === cleanUsername
    );
    if (duplicate) {
      throw new Error('Username already exists.');
    }

    const pinSaltBase64 = PinSecurityManager.generateSaltBase64();
    const pinVerifierBase64 = PinSecurityManager.deriveVerifierBase64(cleanPin, pinSaltBase64);
    const nextId =
      (this.state.managementAccounts || []).reduce((max, a) => Math.max(max, Number(a.id || 0)), 0) + 1;

    const permissions = normalizePermissionsMatrix(input?.permissions, role);
    const created = {
      id: nextId,
      username: cleanUsername,
      displayName: cleanDisplayName,
      role,
      pinSaltBase64,
      pinVerifierBase64,
      recoverySaltBase64: null,
      recoveryVerifierBase64: null,
      isActive: true,
      failedAttempts: 0,
      lockoutUntilMillis: 0,
      createdAtMillis: nowMillis,
      updatedAtMillis: nowMillis,
      permissions
    };

    this.state.managementAccounts = [...(this.state.managementAccounts || []), created];
    const roleLabel = role === MANAGEMENT_ROLES.SUPER_OWNER ? 'Super Owner' : 'Staff';
    this.logActivity(
      'STAFF_CREATED',
      'STAFF_MANAGEMENT',
      `Created ${roleLabel} account: ${cleanDisplayName} (@${cleanUsername})`,
      masterActor,
      nowMillis
    );
    this.recordNotification(
      'STAFF_CREATED',
      `${roleLabel} Account Created`,
      `${cleanDisplayName} (@${cleanUsername}) was added by ${masterActor.displayName}.`,
      'STAFF_MANAGEMENT',
      nowMillis
    );
    this._save();
    return this._sanitizeAccountForPublicView(created);
  }

  updateStaffAccount(targetAccountIdOrObj, maybeInput, nowMillis = Date.now()) {
    const masterActor = this.requireMasterAdmin('update staff accounts');
    let targetAccountId = targetAccountIdOrObj;
    let input = maybeInput;
    if (targetAccountIdOrObj && typeof targetAccountIdOrObj === 'object') {
      targetAccountId = targetAccountIdOrObj.accountId ?? targetAccountIdOrObj.id;
      input = targetAccountIdOrObj;
      if (typeof maybeInput === 'number') nowMillis = maybeInput;
    }
    const accounts = this.state.managementAccounts || [];
    const idx = accounts.findIndex((a) => Number(a.id) === Number(targetAccountId));
    if (idx === -1) {
      throw new Error('Staff account not found.');
    }
    const target = accounts[idx];
    if (target.role === MANAGEMENT_ROLES.MASTER_ADMIN) {
      throw new Error('Use Master Admin settings to update Master account.');
    }

    const cleanDisplayName =
      input?.displayName !== undefined ? String(input.displayName || '').trim() : target.displayName;
    if (!cleanDisplayName) {
      throw new Error('Display name cannot be empty.');
    }

    let pinSaltBase64 = target.pinSaltBase64;
    let pinVerifierBase64 = target.pinVerifierBase64;
    if (input?.newPin !== undefined && input?.newPin !== null && String(input.newPin).trim() !== '') {
      const cleanPin = String(input.newPin).trim();
      if (!PinSecurityManager.isValidPinFormat(cleanPin)) {
        throw new Error('New PIN must be at least 6 numeric digits.');
      }
      pinSaltBase64 = PinSecurityManager.generateSaltBase64();
      pinVerifierBase64 = PinSecurityManager.deriveVerifierBase64(cleanPin, pinSaltBase64);
    }

    const isActive = input?.isActive !== undefined ? Boolean(input.isActive) : Boolean(target.isActive);
    const permissions =
      input?.permissions !== undefined
        ? normalizePermissionsMatrix(input.permissions, target.role)
        : normalizePermissionsMatrix(target.permissions, target.role);

    const updated = {
      ...target,
      displayName: cleanDisplayName,
      isActive,
      pinSaltBase64,
      pinVerifierBase64,
      failedAttempts: 0,
      lockoutUntilMillis: 0,
      updatedAtMillis: nowMillis,
      permissions
    };

    this.state.managementAccounts[idx] = updated;

    // Immediately terminate active session if the currently logged-in account was deactivated
    if (!isActive && this.currentSession?.accountId === updated.id) {
      this.isAdminAuthenticated = false;
      this.currentSession = null;
      this.pinGateReady = false;
    }

    this.logActivity(
      'STAFF_UPDATED',
      'STAFF_MANAGEMENT',
      `Updated staff @${target.username} (active=${isActive})`,
      masterActor,
      nowMillis
    );
    this._save();
    return this._sanitizeAccountForPublicView(updated);
  }

  deleteStaffAccount(targetAccountId, nowMillis = Date.now()) {
    const masterActor = this.requireMasterAdmin('delete staff accounts');
    const accounts = this.state.managementAccounts || [];
    const target = accounts.find((a) => Number(a.id) === Number(targetAccountId));
    if (!target) {
      throw new Error('Staff account not found.');
    }
    if (target.role === MANAGEMENT_ROLES.MASTER_ADMIN) {
      throw new Error('Master Admin account cannot be deleted.');
    }

    this.state.managementAccounts = accounts.filter((a) => Number(a.id) !== Number(targetAccountId));
    if (this.currentSession?.accountId === target.id) {
      this.isAdminAuthenticated = false;
      this.currentSession = null;
      this.pinGateReady = false;
    }

    this.logActivity(
      'STAFF_DELETED',
      'STAFF_MANAGEMENT',
      `Deleted staff account @${target.username}`,
      masterActor,
      nowMillis
    );
    this.recordNotification(
      'STAFF_DELETED',
      'Staff Account Removed',
      `Account @${target.username} was removed by ${masterActor.displayName}.`,
      'STAFF_MANAGEMENT',
      nowMillis
    );
    this._save();
    return true;
  }

  onAdminInteraction(nowMillis = Date.now()) {
    if (this.isAdminAuthenticated) {
      this.lastAdminInteractionAtMillis = nowMillis;
    }
  }

  checkInactivityLock(nowMillis = Date.now(), timeoutMillis = ADMIN_INACTIVITY_TIMEOUT_MILLIS) {
    if (!this.isAdminAuthenticated) return false;
    if (
      this.lastAdminInteractionAtMillis > 0 &&
      nowMillis - this.lastAdminInteractionAtMillis >= timeoutMillis
    ) {
      this.logoutAdmin('INACTIVITY_AUTO_LOCK', nowMillis);
      return true;
    }
    return false;
  }

  _sanitizeAccountForPublicView(acc) {
    if (!acc) return null;
    const {
      pinSaltBase64,
      pinVerifierBase64,
      recoverySaltBase64,
      recoveryVerifierBase64,
      ...rest
    } = acc;
    return {
      ...rest,
      permissions: normalizePermissionsMatrix(acc.permissions, acc.role)
    };
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
    this.lastAdminInteractionAtMillis = Date.now();
    this.logActivity('ADMIN_PIN_CREATED', 'Admin Portal', 'Local management PIN configured');
    this._save();
    this._enqueueCloudOp(() => this.ensureCloudPinGate());
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
      this.lastAdminInteractionAtMillis = now;
      this.logActivity('ADMIN_LOGIN', 'Admin Portal', 'Administrator signed in');
      this._save();
      this._enqueueCloudOp(() => this.ensureCloudPinGate());
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

  logoutAdmin(reason = 'LOGOUT', nowMillis = Date.now()) {
    const active = this.getActiveAccount();
    if (active && this.state.managementAccounts?.length > 0) {
      this.logActivity(
        reason,
        'STAFF_MANAGEMENT',
        `${active.displayName} signed out.`,
        active,
        nowMillis
      );
    }
    this.isAdminAuthenticated = false;
    this.currentSession = null;
    this.pendingRecoveryCode = null;
    this.pinGateReady = false;
    this._save();
  }

  /* ==========================================================================
     ADMIN HERO BANNER, NOTIFICATIONS, ACTIVITY LOG, APPROVALS, VERSIONS & SEARCH
     ========================================================================== */

  saveAdminHeroBanner(input, nowMillis = Date.now()) {
    if (this._shouldEnforceRbac() && !this.canEditAdminHeroBanner()) {
      throw new Error('Permission denied: cannot edit Admin Hero Banner.');
    }
    const active = this.getActiveAccount();
    const prev = this.state.adminHeroBanner || {};
    this.state.adminHeroBanner = {
      titleLine1: String(input?.titleLine1 ?? prev.titleLine1 ?? 'CHEVALIER ACADEMY').trim() || 'CHEVALIER ACADEMY',
      welcomeLine2: String(input?.welcomeLine2 ?? prev.welcomeLine2 ?? 'Welcome, {Admin Name}').trim() || 'Welcome, {Admin Name}',
      subtitleLine3: String(input?.subtitleLine3 ?? prev.subtitleLine3 ?? 'School Management & Administration Portal').trim() || 'School Management & Administration Portal',
      backgroundImageUri: input?.backgroundImageUri !== undefined ? input.backgroundImageUri : (prev.backgroundImageUri || null),
      updatedAtMillis: nowMillis,
      updatedByName: active?.displayName || this.state.admin.displayName || 'Administrator'
    };
    this.logActivity('UPDATE_ADMIN_HERO', 'LOGO_BANNER', 'Updated Admin Dashboard Hero Banner', active, nowMillis);
    this._save();
    return this.state.adminHeroBanner;
  }

  logActivity(action, section, details, actorAccount = null, nowMillis = Date.now()) {
    const active = actorAccount || this.getActiveAccount();
    const entry = {
      id: nowMillis + Math.floor(Math.random() * 10000),
      action,
      section,
      sectionKey: MANAGEMENT_SECTION_MAP[section] ? section : this._resolveSectionKeyFromLabel(section),
      details,
      actor: active?.displayName || this.state.admin.displayName || 'Administrator',
      actorName: active?.displayName || this.state.admin.displayName || 'Administrator',
      actorRole: active?.role || MANAGEMENT_ROLES.MASTER_ADMIN,
      createdAtMillis: nowMillis,
      timestamp: new Date(nowMillis).toISOString()
    };
    this.state.admin.activityLog = [entry, ...(this.state.admin.activityLog || [])].slice(0, 120);
  }

  _resolveSectionKeyFromLabel(labelOrKey) {
    const raw = String(labelOrKey || '').trim();
    if (MANAGEMENT_SECTION_MAP[raw]) return raw;
    const found = MANAGEMENT_SECTIONS.find(
      (s) => s.label.toLowerCase() === raw.toLowerCase()
    );
    if (found) return found.key;
    const lower = raw.toLowerCase();
    if (lower.includes('hero')) return 'LOGO_BANNER';
    if (lower.includes('gallery')) return 'PHOTO_GALLERY';
    if (lower.includes('highlight')) return 'SCHOOL_HIGHLIGHTS';
    if (lower.includes('post')) return 'SCHOOL_POSTS';
    if (lower.includes('backup')) return 'BACKUP_RESTORE';
    if (lower.includes('staff') || lower.includes('admin portal')) return 'STAFF_MANAGEMENT';
    return 'SCHOOL_PROFILE';
  }

  getVisibleActivityLogs() {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return [];
    if (
      session.role !== MANAGEMENT_ROLES.MASTER_ADMIN &&
      !this.hasPermission('ACTIVITY_LOG', PERMISSION_ACTIONS.VIEW)
    ) {
      return [];
    }
    const allLogs = this.state.admin.activityLog || [];
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN) return allLogs;
    const canViewStaff = this.hasPermission('STAFF_MANAGEMENT', PERMISSION_ACTIONS.VIEW);
    const canViewBackup = this.hasPermission('BACKUP_RESTORE', PERMISSION_ACTIONS.VIEW);
    return allLogs.filter((log) => {
      const secKey = log.sectionKey || this._resolveSectionKeyFromLabel(log.section);
      if (secKey === 'STAFF_MANAGEMENT') return canViewStaff;
      if (secKey === 'BACKUP_RESTORE') return canViewBackup;
      return this.hasPermission(secKey, PERMISSION_ACTIONS.VIEW);
    });
  }

  recordNotification(eventType, title, message, sectionKey = 'NOTIFICATIONS', nowMillis = Date.now()) {
    const item = {
      id: nowMillis + Math.floor(Math.random() * 10000),
      eventType,
      title,
      message,
      sectionKey,
      isRead: false,
      createdAtMillis: nowMillis
    };
    this.state.adminNotifications = [item, ...(this.state.adminNotifications || [])].slice(0, 100);
    return item;
  }

  getVisibleNotifications() {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return [];
    if (
      session.role !== MANAGEMENT_ROLES.MASTER_ADMIN &&
      !this.hasPermission('NOTIFICATIONS', PERMISSION_ACTIONS.VIEW)
    ) {
      return [];
    }
    const all = this.state.adminNotifications || [];
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN) return all;
    const canViewStaff = this.hasPermission('STAFF_MANAGEMENT', PERMISSION_ACTIONS.VIEW);
    const canViewBackup = this.hasPermission('BACKUP_RESTORE', PERMISSION_ACTIONS.VIEW);
    return all.filter((n) => {
      if (n.sectionKey === 'STAFF_MANAGEMENT') return canViewStaff;
      if (n.sectionKey === 'BACKUP_RESTORE') return canViewBackup;
      return this.hasPermission(n.sectionKey, PERMISSION_ACTIONS.VIEW);
    });
  }

  getUnreadNotificationsCount() {
    return this.getVisibleNotifications().filter((n) => !n.isRead).length;
  }

  markNotificationRead(notificationId) {
    this.requirePermission('NOTIFICATIONS', PERMISSION_ACTIONS.VIEW);
    this.state.adminNotifications = (this.state.adminNotifications || []).map((n) =>
      Number(n.id) === Number(notificationId) ? { ...n, isRead: true } : n
    );
    this._save();
  }

  markAllNotificationsRead() {
    this.requirePermission('NOTIFICATIONS', PERMISSION_ACTIONS.EDIT);
    this.state.adminNotifications = (this.state.adminNotifications || []).map((n) => ({
      ...n,
      isRead: true
    }));
    this._save();
  }

  clearNotifications() {
    this.requirePermission('NOTIFICATIONS', PERMISSION_ACTIONS.DELETE);
    this.state.adminNotifications = [];
    this._save();
  }

  setDeviceAlertsEnabled(enabled) {
    this.state.preferences.deviceAlertsEnabled = Boolean(enabled);
    this._save();
  }

  submitForApproval({ sectionKey, itemId, itemTitle, actionType = 'PUBLISH', payloadSummary = '' }, nowMillis = Date.now()) {
    const active = this.getActiveAccount();
    if (!active) throw new Error('Active admin session required to submit content for approval.');
    const item = {
      id: nowMillis + Math.floor(Math.random() * 10000),
      sectionKey,
      itemId: Number(itemId || 0),
      itemTitle: String(itemTitle || '').trim() || sectionKey,
      actionType,
      payloadSummary: String(payloadSummary || '').trim(),
      status: 'PENDING',
      requestedByAccountId: active.id,
      requestedByName: active.displayName,
      requestedByRole: active.role,
      reviewedByName: null,
      reviewNotes: null,
      createdAtMillis: nowMillis,
      reviewedAtMillis: null
    };
    this.state.contentApprovals = [item, ...(this.state.contentApprovals || [])];
    this.logActivity(
      'APPROVAL_REQUESTED',
      sectionKey,
      `Submitted "${item.itemTitle}" (${actionType}) for publishing approval.`,
      active,
      nowMillis
    );
    this.recordNotification(
      'APPROVAL_PENDING',
      `Approval Needed: ${item.itemTitle}`,
      `${active.displayName} submitted ${MANAGEMENT_SECTION_MAP[sectionKey]?.label || sectionKey} for review.`,
      sectionKey,
      nowMillis
    );
    this._save();
    return item;
  }

  reviewApproval(approvalId, approve, reviewNotes = '', nowMillis = Date.now()) {
    const list = this.state.contentApprovals || [];
    const idx = list.findIndex((a) => Number(a.id) === Number(approvalId));
    if (idx === -1) throw new Error('Approval request not found.');
    const existing = list[idx];
    this.requirePermission(existing.sectionKey, PERMISSION_ACTIONS.PUBLISH);
    const active = this.getActiveAccount();
    const status = approve ? 'APPROVED' : 'REJECTED';
    const updated = {
      ...existing,
      status,
      reviewedByName: active?.displayName || 'Master Admin',
      reviewNotes: String(reviewNotes || '').trim() || null,
      reviewedAtMillis: nowMillis
    };
    this.state.contentApprovals[idx] = updated;
    this.logActivity(
      approve ? 'APPROVAL_APPROVED' : 'APPROVAL_REJECTED',
      existing.sectionKey,
      `${status} "${existing.itemTitle}" by ${updated.reviewedByName}`,
      active,
      nowMillis
    );
    this.recordNotification(
      `APPROVAL_${status}`,
      `Content ${approve ? 'Approved' : 'Rejected'}: ${existing.itemTitle}`,
      `Reviewed by ${updated.reviewedByName}.`,
      existing.sectionKey,
      nowMillis
    );
    this._save();
    return updated;
  }

  filterActivityLogs({ query = '', sectionKey = null, sortNewestFirst = true } = {}) {
    const visible = this.getVisibleActivityLogs();
    const q = String(query || '').trim().toLowerCase();
    const secFilter = sectionKey && sectionKey !== 'ALL' ? String(sectionKey) : null;

    const filtered = visible.filter((entry) => {
      const resolvedSec = entry.sectionKey || this._resolveSectionKeyFromLabel(entry.section);
      if (secFilter && resolvedSec !== secFilter && entry.section !== secFilter) {
        return false;
      }
      if (!q) return true;
      return (
        String(entry.actorName || entry.actor || '').toLowerCase().includes(q) ||
        String(entry.action || '').toLowerCase().includes(q) ||
        String(entry.section || '').toLowerCase().includes(q) ||
        String(entry.details || '').toLowerCase().includes(q)
      );
    });

    return [...filtered].sort((a, b) => {
      const ta = Number(a.createdAtMillis || new Date(a.timestamp || 0).getTime() || 0);
      const tb = Number(b.createdAtMillis || new Date(b.timestamp || 0).getTime() || 0);
      return sortNewestFirst ? tb - ta : ta - tb;
    });
  }

  recordVersionSnapshot(
    sectionKey,
    itemId,
    itemTitle,
    snapshotSummary,
    nowMillis = Date.now(),
    extra = {}
  ) {
    const active = this.getActiveAccount();
    const existingForTarget = (this.state.versionHistory || []).filter(
      (v) => v.sectionKey === sectionKey && String(v.itemId) === String(itemId)
    );
    const nextVersionNumber =
      existingForTarget.reduce((max, v) => Math.max(max, Number(v.versionNumber || 0)), 0) + 1;
    const prevVersion = existingForTarget[0] || null;
    const contentSnapshotJson =
      extra?.contentSnapshotJson !== undefined
        ? String(extra.contentSnapshotJson)
        : JSON.stringify(this._captureSectionRecordPayload(sectionKey, itemId) || { title: itemTitle, summary: snapshotSummary });
    const previousContentJson =
      extra?.previousContentJson !== undefined
        ? extra.previousContentJson
        : (prevVersion?.contentSnapshotJson || null);

    const entry = {
      id: nowMillis + Math.floor(Math.random() * 10000),
      sectionKey,
      itemId: typeof itemId === 'string' && isNaN(Number(itemId)) ? itemId : Number(itemId || 0),
      recordId: String(itemId ?? '0'),
      itemTitle: String(itemTitle || '').trim() || sectionKey,
      recordTitle: String(itemTitle || '').trim() || sectionKey,
      versionNumber: nextVersionNumber,
      snapshotSummary: String(snapshotSummary || '').trim(),
      changeSummary: String(snapshotSummary || '').trim(),
      contentSnapshotJson,
      previousContentJson,
      approvalStatus: extra?.approvalStatus || 'APPROVED',
      editorName: active?.displayName || this.state.admin.displayName || 'Administrator',
      changedByName: active?.displayName || this.state.admin.displayName || 'Administrator',
      editorRole: active?.role || MANAGEMENT_ROLES.MASTER_ADMIN,
      createdAtMillis: nowMillis
    };
    this.state.versionHistory = [entry, ...(this.state.versionHistory || [])].slice(0, 150);
    return entry;
  }

  _captureSectionRecordPayload(sectionKey, itemId) {
    const numId = Number(itemId);
    switch (sectionKey) {
      case 'SCHOOL_PROFILE':
      case 'ABOUT_SCHOOL':
      case 'CONTACT_DETAILS':
        return { ...this.state.profile };
      case 'ACADEMICS':
        return (this.state.academics || []).find((x) => Number(x.id) === numId) || null;
      case 'HIGHER_SECONDARY_COURSES':
        return (this.state.courses || []).find((x) => Number(x.id) === numId) || null;
      case 'FACILITIES':
        return (this.state.facilities || []).find((x) => Number(x.id) === numId) || null;
      case 'TRANSPORT':
        return (this.state.transportRoutes || []).find((x) => Number(x.id) === numId) || null;
      case 'NEWS':
        return (this.state.news || []).find((x) => Number(x.id) === numId) || null;
      case 'ANNOUNCEMENTS':
        return (this.state.announcements || []).find((x) => Number(x.id) === numId) || null;
      case 'LOGO_BANNER':
        return (this.state.heroPhotos || []).find((x) => Number(x.id) === numId) || null;
      case 'PHOTO_GALLERY':
        return (this.state.galleryMedia || []).find((x) => Number(x.id) === numId) || null;
      case 'SOCIAL_MEDIA':
        return (this.state.socialLinks || []).find((x) => Number(x.id) === numId) || null;
      case 'SCHOOL_POSTS':
        return (this.state.schoolPosts || []).find((x) => String(x.id) === String(itemId)) || null;
      case 'SCHOOL_HIGHLIGHTS':
        return (this.state.schoolHighlights || []).find((x) => String(x.id) === String(itemId)) || null;
      case 'MEDIA_STUDIO':
        return (this.state.mediaStudioItems || []).find((x) => Number(x.id) === numId) || null;
      default: {
        if (String(sectionKey || '').startsWith('V2_')) {
          return (this.state.academicsV2?.records || []).find((r) => String(r.id) === String(itemId)) || null;
        }
        return null;
      }
    }
  }

  _applyRestoredSnapshotPayload(version, publishImmediately = true, nowMillis = Date.now()) {
    if (!version?.contentSnapshotJson) return;
    let payload = null;
    try {
      payload = JSON.parse(version.contentSnapshotJson);
    } catch {
      return;
    }
    if (!payload || typeof payload !== 'object') return;
    const secKey = version.sectionKey;
    const numId = Number(version.itemId);

    if (secKey === 'SCHOOL_PROFILE' || secKey === 'ABOUT_SCHOOL' || secKey === 'CONTACT_DETAILS') {
      this.state.profile = { ...this.state.profile, ...payload };
    } else if (secKey === 'ACADEMICS' && payload.title) {
      const exists = (this.state.academics || []).some((x) => Number(x.id) === numId);
      const restored = { ...payload, id: numId || nowMillis, isPublished: publishImmediately };
      this.state.academics = exists
        ? this.state.academics.map((x) => (Number(x.id) === numId ? restored : x))
        : [restored, ...(this.state.academics || [])];
    } else if (secKey === 'HIGHER_SECONDARY_COURSES' && payload.groupCode) {
      this.state.courses = (this.state.courses || []).map((c) =>
        Number(c.id) === numId ? { ...c, ...payload, verified: publishImmediately ? Boolean(payload.verified) : false } : c
      );
    } else if (secKey === 'FACILITIES' && payload.name) {
      const exists = (this.state.facilities || []).some((x) => Number(x.id) === numId);
      const restored = { ...payload, id: numId || nowMillis, isPublished: publishImmediately };
      this.state.facilities = exists
        ? this.state.facilities.map((x) => (Number(x.id) === numId ? restored : x))
        : [restored, ...(this.state.facilities || [])];
    } else if (secKey === 'TRANSPORT' && payload.routeNumber) {
      const exists = (this.state.transportRoutes || []).some((x) => Number(x.id) === numId);
      const restored = { ...payload, id: numId || nowMillis, isPublished: publishImmediately };
      this.state.transportRoutes = exists
        ? this.state.transportRoutes.map((x) => (Number(x.id) === numId ? restored : x))
        : [restored, ...(this.state.transportRoutes || [])];
    } else if (secKey === 'NEWS' && payload.title) {
      const exists = (this.state.news || []).some((x) => Number(x.id) === numId);
      const restored = { ...payload, id: numId || nowMillis, isPublished: publishImmediately };
      this.state.news = exists
        ? this.state.news.map((x) => (Number(x.id) === numId ? restored : x))
        : [restored, ...(this.state.news || [])];
    } else if (secKey === 'ANNOUNCEMENTS' && payload.title) {
      const exists = (this.state.announcements || []).some((x) => Number(x.id) === numId);
      const restored = { ...payload, id: numId || nowMillis, isPublished: publishImmediately };
      this.state.announcements = exists
        ? this.state.announcements.map((x) => (Number(x.id) === numId ? restored : x))
        : [restored, ...(this.state.announcements || [])];
    } else if (String(secKey || '').startsWith('V2_') && payload.id) {
      const records = this.state.academicsV2?.records || [];
      const exists = records.some((r) => String(r.id) === String(payload.id));
      const targetStatus =
        publishImmediately && !(payload.categoryKey === 'examinations' && payload.sectionKey === 'exam_results' && !this.isSchoolOwner())
          ? ACADEMICS_V2_RECORD_STATUS.PUBLISHED
          : ACADEMICS_V2_RECORD_STATUS.DRAFT;
      const restored = {
        ...payload,
        status: targetStatus,
        updatedAtMillis: nowMillis
      };
      this.state.academicsV2.records = exists
        ? records.map((r) => (String(r.id) === String(payload.id) ? restored : r))
        : [restored, ...records];
    }
  }

  getVisibleVersionHistory() {
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return [];
    const all = this.state.versionHistory || [];
    if (session.role === MANAGEMENT_ROLES.MASTER_ADMIN || session.role === MANAGEMENT_ROLES.SUPER_OWNER) {
      return all;
    }
    const canViewStaff = this.hasPermission('STAFF_MANAGEMENT', PERMISSION_ACTIONS.VIEW);
    const canViewBackup = this.hasPermission('BACKUP_RESTORE', PERMISSION_ACTIONS.VIEW);
    return all.filter((v) => {
      if (v.sectionKey === 'STAFF_MANAGEMENT') return canViewStaff;
      if (v.sectionKey === 'BACKUP_RESTORE') return canViewBackup;
      if (String(v.sectionKey || '').startsWith('V2_')) {
        const catRoute = String(v.sectionKey).slice(3).toLowerCase();
        return this.hasV2CategoryPermission(catRoute, PERMISSION_ACTIONS.VIEW);
      }
      return this.hasPermission(v.sectionKey, PERMISSION_ACTIONS.VIEW);
    });
  }

  restoreVersionSnapshot(versionId, optionsOrMillis = Date.now(), maybeMillis = Date.now()) {
    let publishImmediately = true;
    let nowMillis = Date.now();
    if (typeof optionsOrMillis === 'number') {
      nowMillis = optionsOrMillis;
    } else if (typeof optionsOrMillis === 'boolean') {
      publishImmediately = optionsOrMillis;
      nowMillis = typeof maybeMillis === 'number' ? maybeMillis : Date.now();
    } else if (optionsOrMillis && typeof optionsOrMillis === 'object') {
      if (optionsOrMillis.publishImmediately !== undefined) {
        publishImmediately = Boolean(optionsOrMillis.publishImmediately);
      }
      if (typeof optionsOrMillis.nowMillis === 'number') {
        nowMillis = optionsOrMillis.nowMillis;
      }
    }

    const version = (this.state.versionHistory || []).find((v) => Number(v.id) === Number(versionId));
    if (!version) throw new Error('Version snapshot not found.');
    const secKey = version.sectionKey;
    if (String(secKey || '').startsWith('V2_')) {
      const catRoute = String(secKey).slice(3).toLowerCase();
      this.requireV2CategoryPermission(catRoute, PERMISSION_ACTIONS.EDIT);
    } else {
      this.requirePermission(secKey, PERMISSION_ACTIONS.EDIT);
    }

    const canPub = String(secKey || '').startsWith('V2_')
      ? this.hasV2CategoryPermission(String(secKey).slice(3).toLowerCase(), PERMISSION_ACTIONS.PUBLISH)
      : (this.hasPermission(secKey, PERMISSION_ACTIONS.PUBLISH) || !this._shouldEnforceRbac());
    const effectivePublish = publishImmediately && canPub;

    this._applyRestoredSnapshotPayload(version, effectivePublish, nowMillis);

    const restoredEntry = this.recordVersionSnapshot(
      version.sectionKey,
      version.itemId,
      version.itemTitle,
      `Restored from v${version.versionNumber} (${effectivePublish ? 'Published' : 'Draft'}): ${version.snapshotSummary}`,
      nowMillis,
      {
        contentSnapshotJson: version.contentSnapshotJson,
        approvalStatus: effectivePublish ? 'APPROVED' : 'DRAFT'
      }
    );
    this.logActivity(
      'VERSION_RESTORED',
      version.sectionKey,
      `Restored "${version.itemTitle}" to v${version.versionNumber} (${effectivePublish ? 'Published' : 'Draft'})`,
      null,
      nowMillis
    );
    this._save();
    return restoredEntry;
  }

  saveMediaStudioItem(input, nowMillis = Date.now()) {
    const isEdit = Boolean(input?.id);
    this.requirePermission(
      'MEDIA_STUDIO',
      isEdit ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const title = String(input?.title || '').trim();
    if (!title) throw new Error('Media Studio title is required.');
    const collectionName = String(input?.collectionName || input?.categoryOrAlbum || 'General').trim() || 'General';
    const rawType = String(input?.mediaType || 'PHOTO').toUpperCase();
    const mediaType = rawType === 'YOUTUBE_VIDEO' || rawType === 'YOUTUBE' ? 'YOUTUBE_VIDEO' : 'PHOTO';
    const mediaUri = String(input?.mediaUri || input?.mediaUrl || '').trim();
    if (!mediaUri) throw new Error('Media URI or YouTube URL is required.');
    if (mediaType === 'YOUTUBE_VIDEO' && !parseYouTubeUrl(mediaUri)) {
      throw new Error('A valid YouTube URL is required for video media items.');
    }

    const canPublish = this.hasPermission('MEDIA_STUDIO', PERMISSION_ACTIONS.PUBLISH) || !this._shouldEnforceRbac();
    const active = this.getActiveAccount();
    const id = isEdit ? Number(input.id) : nowMillis + Math.floor(Math.random() * 1000);
    const parsedYt = mediaType === 'YOUTUBE_VIDEO' ? parseYouTubeUrl(mediaUri) : null;
    const item = {
      id,
      title,
      caption: String(input?.caption || '').trim(),
      collectionName,
      categoryOrAlbum: collectionName,
      mediaType,
      mediaUri: parsedYt ? parsedYt.canonicalWatchUrl : mediaUri,
      youtubeVideoId: parsedYt ? parsedYt.videoId : null,
      thumbnailUri: String(input?.thumbnailUri || '').trim() || null,
      displayOrder: Number(input?.displayOrder || 0),
      isPublished: canPublish ? Boolean(input?.isPublished ?? true) : false,
      isApproved: canPublish,
      approvalStatus: canPublish ? 'APPROVED' : 'PENDING',
      createdByName: active?.displayName || 'Administrator',
      createdAtMillis: nowMillis,
      updatedAtMillis: nowMillis
    };

    const list = this.state.mediaStudioItems || [];
    this.state.mediaStudioItems = isEdit
      ? list.map((m) => (Number(m.id) === id ? item : m))
      : [item, ...list];

    if (!canPublish) {
      this.submitForApproval(
        {
          sectionKey: 'MEDIA_STUDIO',
          itemId: item.id,
          itemTitle: item.title,
          actionType: isEdit ? 'EDIT' : 'CREATE',
          payloadSummary: `${mediaType} in ${collectionName}`
        },
        nowMillis
      );
    }
    this.recordVersionSnapshot('MEDIA_STUDIO', item.id, item.title, `${mediaType} (${collectionName})`, nowMillis, {
      contentSnapshotJson: JSON.stringify(item)
    });
    this.logActivity('SAVE_MEDIA_STUDIO', 'MEDIA_STUDIO', `Saved Media Studio item: ${title}`, active, nowMillis);
    this._save();
    return item;
  }

  updateMediaMetadata(mediaId, { title, caption, collectionName, categoryOrAlbum } = {}, nowMillis = Date.now()) {
    this.requirePermission('MEDIA_STUDIO', PERMISSION_ACTIONS.EDIT);
    const list = this.state.mediaStudioItems || [];
    const idx = list.findIndex((m) => Number(m.id) === Number(mediaId));
    if (idx === -1) throw new Error('Media item not found.');
    const existing = list[idx];
    const cleanTitle = title !== undefined ? String(title).trim() : existing.title;
    if (!cleanTitle) throw new Error('Media title cannot be empty.');
    const nextCol = String(collectionName ?? categoryOrAlbum ?? existing.collectionName ?? 'General').trim() || 'General';
    const updated = {
      ...existing,
      title: cleanTitle,
      caption: caption !== undefined ? String(caption || '').trim() : existing.caption,
      collectionName: nextCol,
      categoryOrAlbum: nextCol,
      updatedAtMillis: nowMillis
    };
    this.state.mediaStudioItems[idx] = updated;
    this.recordVersionSnapshot('MEDIA_STUDIO', updated.id, updated.title, `Updated metadata (${nextCol})`, nowMillis, {
      contentSnapshotJson: JSON.stringify(updated)
    });
    this.logActivity('UPDATE_MEDIA_METADATA', 'MEDIA_STUDIO', `Updated metadata for "${updated.title}"`, null, nowMillis);
    this._save();
    return updated;
  }

  replaceMediaImage(mediaId, newMediaUri, nowMillis = Date.now()) {
    this.requirePermission('MEDIA_STUDIO', PERMISSION_ACTIONS.EDIT);
    const cleanUri = String(newMediaUri || '').trim();
    if (!cleanUri) throw new Error('Replacement image URI is required.');
    const list = this.state.mediaStudioItems || [];
    const idx = list.findIndex((m) => Number(m.id) === Number(mediaId));
    if (idx === -1) throw new Error('Media item not found.');
    const existing = list[idx];
    const oldUri = existing.mediaUri;
    const updated = {
      ...existing,
      mediaUri: cleanUri,
      updatedAtMillis: nowMillis
    };
    this.state.mediaStudioItems[idx] = updated;

    // Also update any Hero Photos or Gallery Media referencing oldUri
    if (oldUri) {
      this.state.heroPhotos = (this.state.heroPhotos || []).map((h) =>
        h.imageUri === oldUri || h.imageUrl === oldUri ? { ...h, imageUri: cleanUri, imageUrl: cleanUri } : h
      );
      this.state.galleryMedia = (this.state.galleryMedia || []).map((g) =>
        g.mediaUri === oldUri || g.imageUrl === oldUri ? { ...g, mediaUri: cleanUri, imageUrl: cleanUri } : g
      );
    }

    this.recordVersionSnapshot('MEDIA_STUDIO', updated.id, updated.title, 'Replaced media image URI', nowMillis, {
      contentSnapshotJson: JSON.stringify(updated)
    });
    this.logActivity('REPLACE_MEDIA_IMAGE', 'MEDIA_STUDIO', `Replaced image for "${updated.title}"`, null, nowMillis);
    this._save();
    return updated;
  }

  assignMediaToHero(mediaId, { publishNow = true } = {}, nowMillis = Date.now()) {
    this.requirePermission('LOGO_BANNER', PERMISSION_ACTIONS.CREATE);
    const item = (this.state.mediaStudioItems || []).find((m) => Number(m.id) === Number(mediaId));
    if (!item) throw new Error('Media item not found.');
    if (item.mediaType === 'YOUTUBE_VIDEO') {
      throw new Error('Only image media items can be assigned to the Hero Banner.');
    }
    const canPub = this.hasPermission('LOGO_BANNER', PERMISSION_ACTIONS.PUBLISH) || !this._shouldEnforceRbac();
    const isPublished = Boolean(publishNow && canPub);
    this.addHeroPhoto({
      imageUrl: item.mediaUri,
      imageUri: item.mediaUri,
      caption: item.caption || item.title,
      isPublished
    });
    if (publishNow && !canPub) {
      this.submitForApproval(
        {
          sectionKey: 'LOGO_BANNER',
          itemId: item.id,
          itemTitle: item.title,
          actionType: 'PUBLISH',
          payloadSummary: `Assign Media Studio image "${item.title}" to Hero Banner`
        },
        nowMillis
      );
    }
    this.logActivity('ASSIGN_MEDIA_HERO', 'LOGO_BANNER', `Assigned "${item.title}" to Hero Banner`, null, nowMillis);
    this._save();
    return true;
  }

  assignMediaToAlbum(mediaId, { album = 'Campus Highlights', category = 'Campus', publishNow = true } = {}, nowMillis = Date.now()) {
    this.requirePermission('PHOTO_GALLERY', PERMISSION_ACTIONS.CREATE);
    const item = (this.state.mediaStudioItems || []).find((m) => Number(m.id) === Number(mediaId));
    if (!item) throw new Error('Media item not found.');
    const canPub = this.hasPermission('PHOTO_GALLERY', PERMISSION_ACTIONS.PUBLISH) || !this._shouldEnforceRbac();
    const isPublished = Boolean(publishNow && canPub);
    this.addGalleryMedia({
      imageUrl: item.mediaUri,
      mediaUri: item.mediaUri,
      album: String(album || item.collectionName || 'Campus Highlights').trim(),
      category: String(category || 'Campus').trim(),
      caption: item.caption || item.title,
      youtubeUrl: item.mediaType === 'YOUTUBE_VIDEO' ? item.mediaUri : '',
      isPublished
    });
    if (publishNow && !canPub) {
      this.submitForApproval(
        {
          sectionKey: 'PHOTO_GALLERY',
          itemId: item.id,
          itemTitle: item.title,
          actionType: 'PUBLISH',
          payloadSummary: `Assign "${item.title}" to gallery album "${album}"`
        },
        nowMillis
      );
    }
    this.logActivity('ASSIGN_MEDIA_ALBUM', 'PHOTO_GALLERY', `Assigned "${item.title}" to album "${album}"`, null, nowMillis);
    this._save();
    return true;
  }

  getMediaUsageWarnings(mediaId) {
    const item = (this.state.mediaStudioItems || []).find((m) => Number(m.id) === Number(mediaId));
    if (!item) return [];
    const uri = item.mediaUri;
    if (!uri) return [];
    const usages = [];
    if ((this.state.heroPhotos || []).some((h) => h.imageUri === uri || h.imageUrl === uri)) {
      usages.push('Home Hero Banner');
    }
    if ((this.state.galleryMedia || []).some((g) => g.mediaUri === uri || g.imageUrl === uri)) {
      usages.push('Photo & Video Gallery');
    }
    if ((this.state.schoolPosts || []).some((p) => (p.photoUrls || []).includes(uri) || p.youtubeUrl === uri)) {
      usages.push('School Social Feed Posts');
    }
    if ((this.state.academicsV2?.records || []).some((r) => (r.attachmentUris || []).includes(uri) || r.imageUri === uri)) {
      usages.push('Academics V2 Records');
    }
    if ((this.state.academicsV2?.galleryPhotos || []).some((p) => p.imageUri === uri)) {
      usages.push('Academics V2 Gallery Albums');
    }
    return usages;
  }

  deleteMediaStudioItem(id, nowMillis = Date.now()) {
    this.requirePermission('MEDIA_STUDIO', PERMISSION_ACTIONS.DELETE);
    const usages = this.getMediaUsageWarnings(id);
    this.state.mediaStudioItems = (this.state.mediaStudioItems || []).filter(
      (m) => Number(m.id) !== Number(id)
    );
    this.logActivity(
      'DELETE_MEDIA_STUDIO',
      'MEDIA_STUDIO',
      `Removed Media Studio item #${id}${usages.length ? ` (was referenced in: ${usages.join(', ')})` : ''}`,
      null,
      nowMillis
    );
    this._save();
  }

  /**
   * Permission-filtered Admin Global Search matching Android AdminDashboardSearchScreen
   * and ManagementRepository.searchManagement. Never returns unauthorized sections or private data.
   */
  searchAdminDashboard(rawQuery) {
    const q = String(rawQuery || '').trim().toLowerCase();
    if (!q) return [];
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return [];

    const results = [];

    // 1. Authorized 8 Admin Dashboard Cards
    for (const card of ADMIN_DASHBOARD_CARDS) {
      if (!this.canAccessAdminCard(card.id)) continue;
      const matchesCard =
        card.title.toLowerCase().includes(q) ||
        card.subtitle.toLowerCase().includes(q) ||
        card.keywords.some((kw) => kw.includes(q));
      if (matchesCard) {
        results.push({
          id: `card_${card.id}`,
          title: card.title,
          subtitle: card.subtitle,
          categoryBadge: 'Admin Hub',
          route: card.route,
          icon: card.icon
        });
      }
    }

    // 2. Authorized Management Sections & Governance Tools
    for (const sec of MANAGEMENT_SECTIONS) {
      if (!this.hasPermission(sec.key, PERMISSION_ACTIONS.VIEW)) continue;
      if (sec.label.toLowerCase().includes(q) || sec.key.toLowerCase().includes(q)) {
        results.push({
          id: `section_${sec.key}`,
          title: sec.label,
          subtitle: `Management Section · ${sec.key}`,
          categoryBadge: 'Section',
          route: `admin/section/${sec.key}`,
          icon: 'settings'
        });
      }
    }

    if (this.canAccessApprovalCenter() && ('approval center'.includes(q) || 'pending'.includes(q))) {
      results.push({
        id: 'tool_approval_center',
        title: 'Approval Center',
        subtitle: `${(this.state.contentApprovals || []).filter((a) => a.status === 'PENDING').length} pending items`,
        categoryBadge: 'Management Tool',
        route: 'admin/section/APPROVAL_CENTER',
        icon: 'factCheck'
      });
    }

    if (this.canAccessVersionHistory() && ('version history'.includes(q) || 'restore'.includes(q))) {
      results.push({
        id: 'tool_version_history',
        title: 'Version History',
        subtitle: `${this.getVisibleVersionHistory().length} snapshots`,
        categoryBadge: 'Management Tool',
        route: 'admin/section/VERSION_HISTORY',
        icon: 'schedule'
      });
    }

    // 3. Authorized Records across SEARCHABLE_SECTIONS
    const cmsRecords = this.searchManagement(rawQuery);
    for (const rec of cmsRecords) {
      results.push({
        id: `record_${rec.sectionKey}_${rec.itemId}`,
        title: rec.title,
        subtitle: rec.subtitle,
        categoryBadge: rec.statusBadge,
        route: `admin/section/${rec.sectionKey}`,
        icon: 'description'
      });
    }

    return results.slice(0, 30);
  }

  searchManagement(rawQuery) {
    const q = String(rawQuery || '').trim().toLowerCase();
    if (!q) return [];
    const session = this.getCurrentSession();
    if (!session || !session.account.isActive) return [];

    const results = [];
    const canView = (secKey) => this.hasPermission(secKey, PERMISSION_ACTIONS.VIEW);

    if (canView('ACADEMICS')) {
      for (const item of this.state.academics || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.description || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'ACADEMICS', itemId: item.id, title: item.title, subtitle: `Academics · ${item.category || ''}`, statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('HIGHER_SECONDARY_COURSES')) {
      for (const item of this.state.courses || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.groupCode || '').toLowerCase().includes(q) || String(item.subjects || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'HIGHER_SECONDARY_COURSES', itemId: item.id, title: `${item.groupCode}: ${item.title}`, subtitle: item.subjects || 'Course Stream', statusBadge: item.verified ? 'PUBLISHED' : 'DRAFT' });
        }
      }
    }
    if (canView('FACILITIES')) {
      for (const item of this.state.facilities || []) {
        if (String(item.name || '').toLowerCase().includes(q) || String(item.description || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'FACILITIES', itemId: item.id, title: item.name, subtitle: `Facilities · ${item.category || ''}`, statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('TRANSPORT')) {
      for (const item of this.state.transportRoutes || []) {
        if (String(item.routeNumber || '').toLowerCase().includes(q) || String(item.routeName || '').toLowerCase().includes(q) || String(item.stopsSummary || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'TRANSPORT', itemId: item.id, title: `Route ${item.routeNumber}: ${item.routeName}`, subtitle: item.stopsSummary || 'Transport', statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('NEWS')) {
      for (const item of this.state.news || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.summary || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'NEWS', itemId: item.id, title: item.title, subtitle: `News · ${item.publishedDateText || ''}`, statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('ANNOUNCEMENTS')) {
      for (const item of this.state.announcements || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.message || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'ANNOUNCEMENTS', itemId: item.id, title: item.title, subtitle: `Announcement · ${item.issuedDateText || ''}`, statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('PHOTO_GALLERY')) {
      for (const item of this.state.galleryMedia || []) {
        if (String(item.caption || '').toLowerCase().includes(q) || String(item.album || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'PHOTO_GALLERY', itemId: item.id, title: item.caption || item.album || 'Gallery Media', subtitle: `Gallery · ${item.album || ''}`, statusBadge: 'PUBLISHED' });
        }
      }
    }
    if (canView('SOCIAL_MEDIA')) {
      for (const item of this.state.socialLinks || []) {
        if (String(item.platformName || '').toLowerCase().includes(q) || String(item.handle || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'SOCIAL_MEDIA', itemId: item.id, title: item.platformName, subtitle: item.handle || item.url || 'Social Channel', statusBadge: item.active ? 'ACTIVE' : 'INACTIVE' });
        }
      }
    }
    if (canView('SCHOOL_POSTS')) {
      for (const item of this.state.schoolPosts || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.description || '').toLowerCase().includes(q) || String(item.category || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'SCHOOL_POSTS', itemId: item.id, title: item.title, subtitle: `School Posts · ${item.category}`, statusBadge: item.status });
        }
      }
    }
    if (canView('SCHOOL_HIGHLIGHTS')) {
      for (const item of this.state.schoolHighlights || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.youtubeShortsUrl || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'SCHOOL_HIGHLIGHTS', itemId: item.id, title: item.title || '24h YouTube Short', subtitle: item.youtubeShortsUrl, statusBadge: item.status });
        }
      }
    }
    if (canView('MEDIA_STUDIO')) {
      for (const item of this.state.mediaStudioItems || []) {
        if (String(item.title || '').toLowerCase().includes(q) || String(item.collectionName || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'MEDIA_STUDIO', itemId: item.id, title: item.title, subtitle: `Media Studio · ${item.collectionName}`, statusBadge: item.isPublished ? 'PUBLISHED' : 'DRAFT' });
        }
      }
    }
    if (canView('STAFF_MANAGEMENT')) {
      for (const acc of this.state.managementAccounts || []) {
        if (String(acc.displayName || '').toLowerCase().includes(q) || String(acc.username || '').toLowerCase().includes(q) || String(acc.role || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'STAFF_MANAGEMENT', itemId: acc.id, title: `${acc.displayName} (@${acc.username})`, subtitle: `Role: ${acc.role}`, statusBadge: acc.isActive ? 'ACTIVE' : 'INACTIVE' });
        }
      }
    }
    if (canView('ACTIVITY_LOG')) {
      for (const log of this.getVisibleActivityLogs()) {
        if (String(log.details || '').toLowerCase().includes(q) || String(log.action || '').toLowerCase().includes(q) || String(log.actorName || '').toLowerCase().includes(q)) {
          results.push({ sectionKey: 'ACTIVITY_LOG', itemId: log.id, title: `${log.action} · ${log.actorName}`, subtitle: log.details, statusBadge: 'AUDIT' });
        }
      }
    }

    return results;
  }

  /* ==========================================================================
     CMS CONTENT MUTATIONS (ENFORCING RBAC PERMISSIONS WHEN CONFIGURED)
     ========================================================================== */

  updateProfile(patch) {
    if (this._shouldEnforceRbac()) {
      const canProfile = this.hasPermission('SCHOOL_PROFILE', PERMISSION_ACTIONS.EDIT);
      const canAbout = this.hasPermission('ABOUT_SCHOOL', PERMISSION_ACTIONS.EDIT);
      const canContact = this.hasPermission('CONTACT_DETAILS', PERMISSION_ACTIONS.EDIT);
      if (!canProfile && !canAbout && !canContact) {
        this.requirePermission('SCHOOL_PROFILE', PERMISSION_ACTIONS.EDIT);
      }
    }
    this.state.profile = { ...this.state.profile, ...patch };
    this.recordVersionSnapshot('SCHOOL_PROFILE', 1, this.state.profile.schoolName, 'Updated school profile & contact fields');
    this.logActivity('UPDATE_PROFILE', 'SCHOOL_PROFILE', 'Updated verified school profile & contact details');
    this._save();

    const p = this.state.profile;
    const remoteId = 'school_profile_1';
    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('school_profile', remoteId, {
        schoolName: p.schoolName || BRAND_CONSTANTS.officialSchoolName,
        shortName: p.shortName || BRAND_CONSTANTS.shortSchoolName,
        motto: p.motto || BRAND_CONSTANTS.officialMotto,
        addressLine: p.addressLine || BRAND_CONSTANTS.officialAddress,
        emblemMark: p.emblemMark || BRAND_CONSTANTS.emblemMark,
        poweredBy: p.poweredBy || BRAND_CONSTANTS.poweredByAttribution,
        welcomeIntroduction: p.welcomeIntroduction || null,
        mediumAndGradesNote: p.mediumAndGradesNote || null,
        mapCoordinatesOrQuery: p.mapCoordinatesOrQuery || null,
        phone: p.phone || null,
        email: p.email || null,
        website: p.website || null,
        officeHours: p.officeHours || null,
        principalName: p.principalName || null,
        history: p.history || null,
        vision: p.vision || null,
        mission: p.mission || null,
        affiliationNumber: p.affiliationNumber || null,
        profileImageUri: p.profileImageUri || null,
        aboutImageUri: p.aboutImageUri || null,
        updatedAtMillis: Date.now(),
        remoteId,
        tenantId,
        syncState: 'SYNCED'
      })
    );
  }

  updateCourse(id, patch) {
    this.requirePermission('HIGHER_SECONDARY_COURSES', PERMISSION_ACTIONS.EDIT);
    if (patch?.verified && this._shouldEnforceRbac()) {
      this.requirePermission('HIGHER_SECONDARY_COURSES', PERMISSION_ACTIONS.PUBLISH);
    }
    let targetCourse = null;
    this.state.courses = this.state.courses.map((c) => {
      if (c.id !== id) return c;
      targetCourse = {
        ...c,
        ...patch,
        remoteId: c.remoteId || deterministicRemoteId('higher_secondary_courses', c.id)
      };
      return targetCourse;
    });
    if (targetCourse) {
      this.recordVersionSnapshot('HIGHER_SECONDARY_COURSES', id, targetCourse.title || targetCourse.groupCode, targetCourse.subjects || '');
    }
    this.logActivity('UPDATE_COURSE', 'HIGHER_SECONDARY_COURSES', `Updated course slot #${id}`);
    this._save();

    if (targetCourse) {
      const c = targetCourse;
      const { tenantId } = this.cloudConfig;
      const isPub = Boolean(c.verified);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('higher_secondary_courses', c.remoteId, {
          streamTitle: c.title || c.groupCode,
          groupCode: c.groupCode,
          subjectsSummary: c.subjects || 'Official Curriculum',
          mediumOfInstruction: c.medium || 'English Medium',
          eligibilityNote: c.eligibility || null,
          displayOrder: Number(c.id || 0),
          isVerified: isPub,
          isPublished: isPub,
          isApproved: isPub,
          approvalStatus: isPub ? 'APPROVED' : 'PENDING',
          remoteId: c.remoteId,
          tenantId,
          syncState: 'SYNCED',
          deletedAtMillis: null
        })
      );
    }
  }

  _nextId(nowMillis = Date.now()) {
    const base = Number(nowMillis) || Date.now();
    this._lastGeneratedId = Math.max(base, (this._lastGeneratedId || 0) + 1);
    return this._lastGeneratedId;
  }

  addAcademicProgram(item) {
    this.requirePermission('ACADEMICS', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('academic_info', id);
    const category = item.category || item.level || 'General';
    const next = { id, remoteId, category, level: category, ...item };
    this.state.academics = [next, ...this.state.academics];
    this.recordVersionSnapshot('ACADEMICS', id, next.title, next.description || '');
    this.logActivity('ADD_ACADEMIC', 'ACADEMICS', `Added academic program: ${item.title}`);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('academic_info', remoteId, {
        title: next.title,
        category: next.category || 'General',
        description: next.description || next.title,
        displayOrder: 0,
        isVerified: true,
        isPublished: true,
        isApproved: true,
        approvalStatus: 'APPROVED',
        updatedAtMillis: id,
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  deleteAcademicProgram(id) {
    this.requirePermission('ACADEMICS', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.academics.find((x) => x.id === id);
    this.state.academics = this.state.academics.filter((x) => x.id !== id);
    this.logActivity('DELETE_ACADEMIC', 'ACADEMICS', `Removed academic program #${id}`);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('academic_info', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('academic_info', remoteId));
    }
  }

  addFacility(item) {
    this.requirePermission('FACILITIES', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('facilities', id);
    const imageUri = item.imageUrl || item.imageUri || null;
    const next = { id, remoteId, imageUrl: imageUri || '', imageUri: imageUri || '', ...item };
    this.state.facilities = [next, ...this.state.facilities];
    this.recordVersionSnapshot('FACILITIES', id, next.name, next.description || '');
    this.logActivity('ADD_FACILITY', 'FACILITIES', `Added facility: ${item.name}`);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('facilities', remoteId, {
        name: next.name,
        category: next.category || 'Campus',
        description: next.description || next.name,
        capacityOrSpec: next.capacityOrSpec || null,
        displayOrder: 0,
        imageUri,
        isVerified: true,
        isPublished: true,
        isApproved: true,
        approvalStatus: 'APPROVED',
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  deleteFacility(id) {
    this.requirePermission('FACILITIES', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.facilities.find((x) => x.id === id);
    this.state.facilities = this.state.facilities.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('facilities', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('facilities', remoteId));
    }
  }

  addTransportRoute(item) {
    this.requirePermission('TRANSPORT', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('transport_routes', id);
    const stopsSummary = item.stopsSummary || item.stops || '';
    const morningTiming = item.morningTiming || item.timings || null;
    const next = {
      id,
      remoteId,
      stops: stopsSummary,
      stopsSummary,
      timings: morningTiming || '',
      morningTiming,
      ...item
    };
    this.state.transportRoutes = [next, ...this.state.transportRoutes];
    this.recordVersionSnapshot('TRANSPORT', id, `${next.routeNumber} ${next.routeName}`, stopsSummary);
    this.logActivity('ADD_TRANSPORT', 'TRANSPORT', `Added route: ${item.routeNumber}`);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('transport_routes', remoteId, {
        routeNumber: next.routeNumber,
        routeName: next.routeName,
        stopsSummary: stopsSummary || next.routeName,
        morningTiming,
        afternoonTiming: next.afternoonTiming || null,
        coordinatorContact: next.coordinatorContact || null,
        displayOrder: 0,
        isVerified: true,
        isPublished: true,
        isApproved: true,
        approvalStatus: 'APPROVED',
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  deleteTransportRoute(id) {
    this.requirePermission('TRANSPORT', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.transportRoutes.find((x) => x.id === id);
    this.state.transportRoutes = this.state.transportRoutes.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('transport_routes', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('transport_routes', remoteId));
    }
  }

  addNews(item) {
    this.requirePermission('NEWS', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('news_items', id);
    const publishedDateText = item.publishedDateText || new Date().toLocaleDateString();
    const imageUri = item.imageUrl || item.imageUri || null;
    const next = {
      id,
      remoteId,
      publishedDateText,
      publishedAtMillis: id,
      imageUrl: imageUri || '',
      imageUri: imageUri || '',
      ...item
    };
    this.state.news = [next, ...this.state.news];
    this.recordVersionSnapshot('NEWS', id, next.title, next.summary || '');
    this.logActivity('PUBLISH_NEWS', 'NEWS', `Published news: ${item.title}`);
    this.recordNotification('NEWS_PUBLISHED', `News Published: ${next.title}`, next.summary || next.title, 'NEWS', id);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('news_items', remoteId, {
        title: next.title,
        summary: next.summary || next.title,
        fullBody: next.fullBody || next.summary || next.title,
        publishedDateText,
        publishedAtMillis: id,
        isFeatured: Boolean(next.isFeatured),
        imageUri,
        displayOrder: 0,
        isVerified: true,
        isPublished: true,
        isApproved: true,
        approvalStatus: 'APPROVED',
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  deleteNews(id) {
    this.requirePermission('NEWS', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.news.find((x) => x.id === id);
    this.state.news = this.state.news.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('news_items', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('news_items', remoteId));
    }
  }

  addAnnouncement(item) {
    this.requirePermission('ANNOUNCEMENTS', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('announcements', id);
    const issuedDateText = item.issuedDateText || new Date().toLocaleDateString();
    const imageUri = item.imageUrl || item.imageUri || null;
    const next = {
      id,
      remoteId,
      issuedDateText,
      issuedAtMillis: id,
      imageUrl: imageUri || '',
      imageUri: imageUri || '',
      ...item
    };
    this.state.announcements = [next, ...this.state.announcements];
    this.recordVersionSnapshot('ANNOUNCEMENTS', id, next.title, next.message || '');
    this.logActivity('PUBLISH_ANNOUNCEMENT', 'ANNOUNCEMENTS', `Published announcement: ${item.title}`);
    this.recordNotification('ANNOUNCEMENT_PUBLISHED', `Announcement Published: ${next.title}`, next.message || next.title, 'ANNOUNCEMENTS', id);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('announcements', remoteId, {
        title: next.title,
        message: next.message || next.title,
        priorityLabel: next.priorityLabel || null,
        issuedDateText,
        issuedAtMillis: id,
        imageUri,
        displayOrder: 0,
        isVerified: true,
        isPublished: true,
        isApproved: true,
        approvalStatus: 'APPROVED',
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  deleteAnnouncement(id) {
    this.requirePermission('ANNOUNCEMENTS', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.announcements.find((x) => x.id === id);
    this.state.announcements = this.state.announcements.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('announcements', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('announcements', remoteId));
    }
  }

  addHeroPhoto(item) {
    this.requirePermission('LOGO_BANNER', PERMISSION_ACTIONS.CREATE);
    const id = this._nextId();
    const remoteId = item.remoteId || deterministicRemoteId('campus_hero_photos', id);
    const imageUri = item.imageUrl || item.imageUri || '';
    const next = {
      id,
      remoteId,
      imageUrl: imageUri,
      imageUri,
      ...item
    };
    this.state.heroPhotos = [next, ...this.state.heroPhotos];
    this.recordVersionSnapshot('LOGO_BANNER', id, next.caption || 'Hero Slide', imageUri.slice(0, 48));
    this.logActivity('ADD_HERO_PHOTO', 'LOGO_BANNER', `Added campus hero banner image`);
    this._save();

    if (imageUri) {
      const { tenantId } = this.cloudConfig;
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('campus_hero_photos', remoteId, {
          imageUri,
          caption: next.caption || null,
          displayOrder: 0,
          isVerified: true,
          isPublished: true,
          isApproved: true,
          approvalStatus: 'APPROVED',
          addedAtMillis: id,
          remoteId,
          tenantId,
          syncState: 'SYNCED',
          deletedAtMillis: null
        })
      );
    }
  }

  deleteHeroPhoto(id) {
    this.requirePermission('LOGO_BANNER', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.heroPhotos.find((x) => x.id === id);
    this.state.heroPhotos = this.state.heroPhotos.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('campus_hero_photos', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('campus_hero_photos', remoteId));
    }
  }

  addGalleryMedia(item) {
    this.requirePermission('PHOTO_GALLERY', PERMISSION_ACTIONS.CREATE);
    const now = Date.now();
    const albumName = (item.album || 'Campus Highlights').trim() || 'Campus Highlights';
    let albumObj = this.state.galleryAlbums.find(
      (a) => a.name.toLowerCase() === albumName.toLowerCase()
    );
    const { tenantId } = this.cloudConfig;

    if (!albumObj) {
      const albumId = now + 1;
      const albumRemoteId = deterministicRemoteId('gallery_albums', albumId);
      albumObj = {
        id: albumId,
        remoteId: albumRemoteId,
        name: albumName,
        category: item.category || 'Campus'
      };
      this.state.galleryAlbums.push(albumObj);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('gallery_albums', albumRemoteId, {
          title: albumName,
          category: albumObj.category,
          coverMediaUri: item.imageUrl || item.mediaUri || null,
          createdAtMillis: now,
          displayOrder: 0,
          isVerified: true,
          isPublished: true,
          isApproved: true,
          approvalStatus: 'APPROVED',
          remoteId: albumRemoteId,
          tenantId,
          syncState: 'SYNCED',
          deletedAtMillis: null
        })
      );
    }

    const mediaId = now;
    const mediaRemoteId = item.remoteId || deterministicRemoteId('gallery_media', mediaId);
    const mediaUri = item.imageUrl || item.mediaUri || '';
    const nextMedia = {
      id: mediaId,
      remoteId: mediaRemoteId,
      albumRemoteId: albumObj.remoteId || deterministicRemoteId('gallery_albums', albumObj.id),
      album: albumName,
      imageUrl: mediaUri,
      mediaUri,
      ...item
    };
    this.state.galleryMedia = [nextMedia, ...this.state.galleryMedia];
    this.recordVersionSnapshot('PHOTO_GALLERY', mediaId, item.caption || albumName, albumName);
    this.logActivity('ADD_GALLERY_MEDIA', 'PHOTO_GALLERY', `Added gallery item: ${item.caption || albumName}`);
    this._save();

    if (mediaUri) {
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('gallery_media', mediaRemoteId, {
          albumRemoteId: nextMedia.albumRemoteId,
          title: albumName,
          caption: nextMedia.caption || null,
          mediaUri,
          mediaType: nextMedia.youtubeUrl ? 'YOUTUBE' : 'IMAGE',
          displayOrder: 0,
          addedAtMillis: now,
          isVerified: true,
          isPublished: true,
          isApproved: true,
          approvalStatus: 'APPROVED',
          remoteId: mediaRemoteId,
          tenantId,
          syncState: 'SYNCED',
          deletedAtMillis: null
        })
      );
    }
  }

  deleteGalleryMedia(id) {
    this.requirePermission('PHOTO_GALLERY', PERMISSION_ACTIONS.DELETE);
    const existing = this.state.galleryMedia.find((x) => x.id === id);
    this.state.galleryMedia = this.state.galleryMedia.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('gallery_media', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('gallery_media', remoteId));
    }
  }

  updateSocialLink(id, patch) {
    this.requirePermission('SOCIAL_MEDIA', PERMISSION_ACTIONS.EDIT);
    if (patch?.active && this._shouldEnforceRbac()) {
      this.requirePermission('SOCIAL_MEDIA', PERMISSION_ACTIONS.PUBLISH);
    }
    let updatedLink = null;
    this.state.socialLinks = this.state.socialLinks.map((s) => {
      if (s.id !== id) return s;
      updatedLink = {
        ...s,
        ...patch,
        remoteId: s.remoteId || deterministicRemoteId('social_media_links', s.id)
      };
      return updatedLink;
    });
    if (updatedLink) {
      this.recordVersionSnapshot('SOCIAL_MEDIA', id, updatedLink.platformName, updatedLink.url || '');
    }
    this.logActivity('UPDATE_SOCIAL', 'SOCIAL_MEDIA', `Updated channel #${id}`);
    this._save();

    if (updatedLink && updatedLink.url) {
      const s = updatedLink;
      const { tenantId } = this.cloudConfig;
      const isPub = Boolean(s.active);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('social_media_links', s.remoteId, {
          platformName: s.platformName,
          handleOrLabel: s.handle || s.platformName,
          url: s.url,
          displayOrder: Number(s.id || 0),
          isVerified: isPub,
          isPublished: isPub,
          isApproved: isPub,
          approvalStatus: isPub ? 'APPROVED' : 'PENDING',
          remoteId: s.remoteId,
          tenantId,
          syncState: 'SYNCED',
          deletedAtMillis: null
        })
      );
    }
  }

  saveSchoolPost(input, nowMillis = Date.now()) {
    const existing = input?.id ? (this.state.schoolPosts || []).find((p) => p.id === input.id) : null;
    this.requirePermission(
      'SCHOOL_POSTS',
      existing ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const postType = input?.postType || SCHOOL_POST_TYPES.PHOTO;
    const status = input?.status || SCHOOL_POST_STATUS.UNPUBLISHED;
    if (status === SCHOOL_POST_STATUS.PUBLISHED) {
      this.requirePermission('SCHOOL_POSTS', PERMISSION_ACTIONS.PUBLISH);
    }
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

    const id = existing ? existing.id : nowMillis + Math.floor(Math.random() * 1000);
    const remoteId = existing?.remoteId || input?.remoteId || deterministicRemoteId('school_posts', id);
    const createdAtMillis = existing ? existing.createdAtMillis : nowMillis;
    const publishedAtMillis =
      status === SCHOOL_POST_STATUS.PUBLISHED
        ? (existing?.publishedAtMillis ?? nowMillis)
        : (existing?.publishedAtMillis ?? null);

    const record = {
      id,
      remoteId,
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
    this.recordVersionSnapshot('SCHOOL_POSTS', id, title, `${postType} · ${status}`, nowMillis);
    this.logActivity('SAVE_SCHOOL_POST', 'SCHOOL_POSTS', `${existing ? 'Updated' : 'Created'} post: ${title}`, null, nowMillis);
    this._save();
    this._syncSchoolPostToCloud(record);
    return record;
  }

  _syncSchoolPostToCloud(record) {
    if (!record) return;
    const { tenantId } = this.cloudConfig;
    const remoteId = record.remoteId || deterministicRemoteId('school_posts', record.id);
    this._enqueueCloudOp(async () => {
      const rawPhotos = Array.isArray(record.photoUrls) ? record.photoUrls.slice(0, MAX_POST_PHOTOS) : [];
      const offloadedPhotoUris = (
        await Promise.all(
          rawPhotos.map((uri, idx) => this._uploadOrOffloadPostPhoto(remoteId, idx, uri))
        )
      ).filter(Boolean);
      const thumbUri = record.customThumbnailUrl
        ? (record.customThumbnailUrl.length <= MAX_MEDIA_URI_CHARS ? record.customThumbnailUrl : null)
        : null;
      return this._pushCloudDoc('school_posts', remoteId, {
        postType: record.postType,
        category: record.category,
        title: record.title,
        description: record.description || '',
        youtubeUrl: record.youtubeUrl || null,
        customThumbnailUri: thumbUri,
        photoUris: offloadedPhotoUris,
        status: record.status,
        isFeatured: Boolean(record.isFeatured),
        createdAtMillis: record.createdAtMillis,
        publishedAtMillis: record.publishedAtMillis ?? record.createdAtMillis,
        updatedAtMillis: record.updatedAtMillis,
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      });
    });
  }

  setSchoolPostStatus(id, status, nowMillis = Date.now()) {
    if (!Object.values(SCHOOL_POST_STATUS).includes(status)) {
      throw new Error(`Unsupported post status: ${status}`);
    }
    this.requirePermission(
      'SCHOOL_POSTS',
      status === SCHOOL_POST_STATUS.PUBLISHED ? PERMISSION_ACTIONS.PUBLISH : PERMISSION_ACTIONS.EDIT
    );
    let updatedPost = null;
    this.state.schoolPosts = (this.state.schoolPosts || []).map((p) => {
      if (p.id !== id) return p;
      updatedPost = {
        ...p,
        status,
        publishedAtMillis: status === SCHOOL_POST_STATUS.PUBLISHED ? (p.publishedAtMillis ?? nowMillis) : p.publishedAtMillis,
        updatedAtMillis: nowMillis
      };
      return updatedPost;
    });
    this._save();
    if (updatedPost) {
      this._syncSchoolPostToCloud(updatedPost);
    }
  }

  setSchoolPostFeatured(id, isFeatured, nowMillis = Date.now()) {
    this.requirePermission('SCHOOL_POSTS', PERMISSION_ACTIONS.EDIT);
    let updatedPost = null;
    this.state.schoolPosts = (this.state.schoolPosts || []).map((p) => {
      if (p.id !== id) return p;
      updatedPost = { ...p, isFeatured: Boolean(isFeatured), updatedAtMillis: nowMillis };
      return updatedPost;
    });
    this._save();
    if (updatedPost) {
      this._syncSchoolPostToCloud(updatedPost);
    }
  }

  deleteSchoolPost(id) {
    this.requirePermission('SCHOOL_POSTS', PERMISSION_ACTIONS.DELETE);
    const existing = (this.state.schoolPosts || []).find((p) => p.id === id);
    this.state.schoolPosts = (this.state.schoolPosts || []).filter((p) => p.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('school_posts', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('school_posts', remoteId));
    }
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
    const existing = input?.id ? (this.state.schoolHighlights || []).find((h) => h.id === input.id) : null;
    this.requirePermission(
      'SCHOOL_HIGHLIGHTS',
      existing ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const status = input?.status || SCHOOL_POST_STATUS.PUBLISHED;
    if (status === SCHOOL_POST_STATUS.PUBLISHED) {
      this.requirePermission('SCHOOL_HIGHLIGHTS', PERMISSION_ACTIONS.PUBLISH);
    }
    if (!Object.values(SCHOOL_POST_STATUS).includes(status)) {
      throw new Error(`Unsupported highlight status: ${status}`);
    }
    const parsed = parseYouTubeUrl(input?.youtubeShortsUrl);
    if (!parsed || !parsed.isShort) {
      throw new Error('A valid YouTube Shorts URL (youtube.com/shorts/...) is required.');
    }
    const id = existing ? existing.id : nowMillis + Math.floor(Math.random() * 1000);
    const remoteId = existing?.remoteId || input?.remoteId || deterministicRemoteId('school_highlights', id);
    const createdAtMillis = existing ? existing.createdAtMillis : nowMillis;
    const publishedAtMillis = existing ? existing.publishedAtMillis : nowMillis;
    const expiresAtMillis = existing ? existing.expiresAtMillis : publishedAtMillis + HIGHLIGHT_TTL_MILLIS;

    const record = {
      id,
      remoteId,
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
    this.recordVersionSnapshot('SCHOOL_HIGHLIGHTS', id, record.title || parsed.videoId, record.youtubeShortsUrl, nowMillis);
    this.logActivity('SAVE_HIGHLIGHT', 'SCHOOL_HIGHLIGHTS', `Saved 24h highlight: ${record.title || parsed.videoId}`, null, nowMillis);
    this._save();

    const { tenantId } = this.cloudConfig;
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('school_highlights', remoteId, {
        title: record.title,
        youtubeShortsUrl: record.youtubeShortsUrl,
        status: record.status,
        createdAtMillis: record.createdAtMillis,
        publishedAtMillis: record.publishedAtMillis,
        expiresAtMillis: record.expiresAtMillis,
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
    return record;
  }

  getActiveHighlights(nowMillis = Date.now()) {
    return [...(this.state.schoolHighlights || [])]
      .filter((h) => h.status === SCHOOL_POST_STATUS.PUBLISHED && h.expiresAtMillis > nowMillis)
      .sort((a, b) => b.publishedAtMillis - a.publishedAtMillis);
  }

  deleteSchoolHighlight(id) {
    this.requirePermission('SCHOOL_HIGHLIGHTS', PERMISSION_ACTIONS.DELETE);
    const existing = (this.state.schoolHighlights || []).find((h) => h.id === id);
    this.state.schoolHighlights = (this.state.schoolHighlights || []).filter((h) => h.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('school_highlights', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('school_highlights', remoteId));
    }
  }

  _postEngagementKeys(postOrId) {
    if (typeof postOrId === 'object' && postOrId !== null) {
      return [String(postOrId.id ?? ''), String(postOrId.remoteId ?? '')].filter(Boolean);
    }
    const raw = String(postOrId ?? '').trim();
    if (!raw) return [];
    const found = (this.state.schoolPosts || []).find(
      (p) => String(p.id) === raw || String(p.remoteId) === raw
    );
    return found
      ? [String(found.id ?? ''), String(found.remoteId ?? '')].filter(Boolean)
      : [raw];
  }

  isPostLiked(postOrId) {
    const keys = this._postEngagementKeys(postOrId);
    const liked = new Set((this.state.preferences.likedPostIds || []).map(String));
    return keys.some((k) => liked.has(k));
  }

  toggleLikePost(postOrId) {
    const keys = this._postEngagementKeys(postOrId);
    if (keys.length === 0) return false;
    const current = new Set((this.state.preferences.likedPostIds || []).map(String));
    const already = keys.some((k) => current.has(k));
    if (already) {
      keys.forEach((k) => current.delete(k));
    } else {
      keys.forEach((k) => current.add(k));
    }
    this.state.preferences.likedPostIds = Array.from(current);
    this._save();
    return !already;
  }

  isPostBookmarked(postOrId) {
    const keys = this._postEngagementKeys(postOrId);
    const saved = new Set((this.state.preferences.bookmarkedPostIds || []).map(String));
    return keys.some((k) => saved.has(k));
  }

  toggleBookmarkPost(postOrId) {
    const keys = this._postEngagementKeys(postOrId);
    if (keys.length === 0) return false;
    const current = new Set((this.state.preferences.bookmarkedPostIds || []).map(String));
    const already = keys.some((k) => current.has(k));
    if (already) {
      keys.forEach((k) => current.delete(k));
    } else {
      keys.forEach((k) => current.add(k));
    }
    this.state.preferences.bookmarkedPostIds = Array.from(current);
    this._save();
    return !already;
  }

  getBookmarkedPosts() {
    return this.getPublishedSchoolPosts().filter((p) => this.isPostBookmarked(p));
  }

  getAcademyFeedPosts() {
    const academyCategories = new Set(['academics', 'events', 'achievements']);
    return this.getPublishedSchoolPosts().filter((p) =>
      academyCategories.has(String(p.category || '').trim().toLowerCase())
    );
  }

  /* ==========================================================================
     BASE 4 — FULL CMS EDIT, REORDER (DISPLAY ORDER) & PUBLISH/UNPUBLISH CONTROLS
     ========================================================================== */

  updateAcademicProgram(id, patch, nowMillis = Date.now()) {
    this.requirePermission('ACADEMICS', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.academics = (this.state.academics || []).map((a) => {
      if (Number(a.id) !== Number(id)) return a;
      const category = patch?.category ?? patch?.level ?? a.category ?? a.level ?? 'General';
      updated = {
        ...a,
        ...patch,
        category,
        level: category,
        remoteId: a.remoteId || deterministicRemoteId('academic_info', a.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Academic program not found.');
    this.recordVersionSnapshot('ACADEMICS', id, updated.title, updated.description || '', nowMillis);
    this.logActivity('UPDATE_ACADEMIC', 'ACADEMICS', `Updated academic program: ${updated.title}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('ACADEMICS', updated, nowMillis);
    return updated;
  }

  updateFacility(id, patch, nowMillis = Date.now()) {
    this.requirePermission('FACILITIES', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.facilities = (this.state.facilities || []).map((f) => {
      if (Number(f.id) !== Number(id)) return f;
      const imageUri = patch?.imageUrl ?? patch?.imageUri ?? f.imageUri ?? f.imageUrl ?? '';
      updated = {
        ...f,
        ...patch,
        imageUrl: imageUri,
        imageUri,
        remoteId: f.remoteId || deterministicRemoteId('facilities', f.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Facility not found.');
    this.recordVersionSnapshot('FACILITIES', id, updated.name, updated.description || '', nowMillis);
    this.logActivity('UPDATE_FACILITY', 'FACILITIES', `Updated facility: ${updated.name}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('FACILITIES', updated, nowMillis);
    return updated;
  }

  updateTransportRoute(id, patch, nowMillis = Date.now()) {
    this.requirePermission('TRANSPORT', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.transportRoutes = (this.state.transportRoutes || []).map((r) => {
      if (Number(r.id) !== Number(id)) return r;
      const stopsSummary = patch?.stopsSummary ?? patch?.stops ?? r.stopsSummary ?? r.stops ?? '';
      const morningTiming = patch?.morningTiming ?? patch?.timings ?? r.morningTiming ?? r.timings ?? '';
      updated = {
        ...r,
        ...patch,
        stops: stopsSummary,
        stopsSummary,
        timings: morningTiming,
        morningTiming,
        remoteId: r.remoteId || deterministicRemoteId('transport_routes', r.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Transport route not found.');
    this.recordVersionSnapshot('TRANSPORT', id, `${updated.routeNumber} ${updated.routeName}`, updated.stopsSummary || '', nowMillis);
    this.logActivity('UPDATE_TRANSPORT', 'TRANSPORT', `Updated route: ${updated.routeNumber}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('TRANSPORT', updated, nowMillis);
    return updated;
  }

  updateNews(id, patch, nowMillis = Date.now()) {
    this.requirePermission('NEWS', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.news = (this.state.news || []).map((n) => {
      if (Number(n.id) !== Number(id)) return n;
      const imageUri = patch?.imageUrl ?? patch?.imageUri ?? n.imageUri ?? n.imageUrl ?? '';
      updated = {
        ...n,
        ...patch,
        imageUrl: imageUri,
        imageUri,
        remoteId: n.remoteId || deterministicRemoteId('news_items', n.id)
      };
      return updated;
    });
    if (!updated) throw new Error('News item not found.');
    this.recordVersionSnapshot('NEWS', id, updated.title, updated.summary || '', nowMillis);
    this.logActivity('UPDATE_NEWS', 'NEWS', `Updated news: ${updated.title}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('NEWS', updated, nowMillis);
    return updated;
  }

  updateAnnouncement(id, patch, nowMillis = Date.now()) {
    this.requirePermission('ANNOUNCEMENTS', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.announcements = (this.state.announcements || []).map((a) => {
      if (Number(a.id) !== Number(id)) return a;
      const imageUri = patch?.imageUrl ?? patch?.imageUri ?? a.imageUri ?? a.imageUrl ?? '';
      updated = {
        ...a,
        ...patch,
        imageUrl: imageUri,
        imageUri,
        remoteId: a.remoteId || deterministicRemoteId('announcements', a.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Announcement not found.');
    this.recordVersionSnapshot('ANNOUNCEMENTS', id, updated.title, updated.message || '', nowMillis);
    this.logActivity('UPDATE_ANNOUNCEMENT', 'ANNOUNCEMENTS', `Updated announcement: ${updated.title}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('ANNOUNCEMENTS', updated, nowMillis);
    return updated;
  }

  updateHeroPhoto(id, patch, nowMillis = Date.now()) {
    this.requirePermission('LOGO_BANNER', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.heroPhotos = (this.state.heroPhotos || []).map((h) => {
      if (Number(h.id) !== Number(id)) return h;
      const imageUri = patch?.imageUrl ?? patch?.imageUri ?? h.imageUri ?? h.imageUrl ?? '';
      updated = {
        ...h,
        ...patch,
        imageUrl: imageUri,
        imageUri,
        remoteId: h.remoteId || deterministicRemoteId('campus_hero_photos', h.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Hero photo not found.');
    this.recordVersionSnapshot('LOGO_BANNER', id, updated.caption || 'Hero Slide', updated.imageUri || '', nowMillis);
    this.logActivity('UPDATE_HERO_PHOTO', 'LOGO_BANNER', `Updated hero slide #${id}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('LOGO_BANNER', updated, nowMillis);
    return updated;
  }

  updateGalleryMedia(id, patch, nowMillis = Date.now()) {
    this.requirePermission('PHOTO_GALLERY', PERMISSION_ACTIONS.EDIT);
    let updated = null;
    this.state.galleryMedia = (this.state.galleryMedia || []).map((m) => {
      if (Number(m.id) !== Number(id)) return m;
      const mediaUri = patch?.imageUrl ?? patch?.mediaUri ?? m.mediaUri ?? m.imageUrl ?? '';
      updated = {
        ...m,
        ...patch,
        imageUrl: mediaUri,
        mediaUri,
        remoteId: m.remoteId || deterministicRemoteId('gallery_media', m.id)
      };
      return updated;
    });
    if (!updated) throw new Error('Gallery media item not found.');
    this.recordVersionSnapshot('PHOTO_GALLERY', id, updated.caption || updated.album || 'Gallery Media', updated.album || '', nowMillis);
    this.logActivity('UPDATE_GALLERY_MEDIA', 'PHOTO_GALLERY', `Updated gallery item #${id}`, null, nowMillis);
    this._save();
    this._syncCmsSectionItemToCloud('PHOTO_GALLERY', updated, nowMillis);
    return updated;
  }

  _getCmsArrayKeyForSection(sectionKey) {
    switch (sectionKey) {
      case 'ACADEMICS':
        return 'academics';
      case 'HIGHER_SECONDARY_COURSES':
        return 'courses';
      case 'FACILITIES':
        return 'facilities';
      case 'TRANSPORT':
        return 'transportRoutes';
      case 'NEWS':
        return 'news';
      case 'ANNOUNCEMENTS':
        return 'announcements';
      case 'LOGO_BANNER':
        return 'heroPhotos';
      case 'PHOTO_GALLERY':
        return 'galleryMedia';
      case 'SOCIAL_MEDIA':
        return 'socialLinks';
      case 'SCHOOL_POSTS':
        return 'schoolPosts';
      case 'SCHOOL_HIGHLIGHTS':
        return 'schoolHighlights';
      default:
        return null;
    }
  }

  reorderCmsItem(sectionKey, itemId, direction = 'up', nowMillis = Date.now()) {
    this.requirePermission(sectionKey, PERMISSION_ACTIONS.EDIT);
    const arrayKey = this._getCmsArrayKeyForSection(sectionKey);
    if (!arrayKey || !Array.isArray(this.state[arrayKey])) {
      throw new Error(`Section ${sectionKey} does not support item reordering.`);
    }
    const list = [...this.state[arrayKey]];
    const idx = list.findIndex((item) => String(item.id) === String(itemId));
    if (idx === -1) throw new Error('Item to reorder not found.');
    const targetIdx = direction === 'down' ? idx + 1 : idx - 1;
    if (targetIdx < 0 || targetIdx >= list.length) {
      return list;
    }
    const temp = list[idx];
    list[idx] = list[targetIdx];
    list[targetIdx] = temp;

    const reindexed = list.map((item, index) => ({
      ...item,
      displayOrder: index
    }));
    this.state[arrayKey] = reindexed;
    this.logActivity(
      'REORDER_CMS_ITEM',
      sectionKey,
      `Reordered item #${itemId} (${direction}) in ${sectionKey}`,
      null,
      nowMillis
    );
    this._save();
    this._syncCmsSectionItemToCloud(sectionKey, reindexed[targetIdx], nowMillis);
    this._syncCmsSectionItemToCloud(sectionKey, reindexed[idx], nowMillis);
    return reindexed;
  }

  setCmsItemPublished(sectionKey, itemId, isPublished, nowMillis = Date.now()) {
    const arrayKey = this._getCmsArrayKeyForSection(sectionKey);
    if (!arrayKey || !Array.isArray(this.state[arrayKey])) {
      throw new Error(`Unsupported CMS section: ${sectionKey}`);
    }
    const pubBool = Boolean(isPublished);
    if (pubBool) {
      if (this._shouldEnforceRbac() && !this.hasPermission(sectionKey, PERMISSION_ACTIONS.PUBLISH)) {
        this.requirePermission(sectionKey, PERMISSION_ACTIONS.EDIT);
        const existingItem = this.state[arrayKey].find((x) => String(x.id) === String(itemId));
        return this.submitForApproval(
          {
            sectionKey,
            itemId,
            itemTitle: existingItem?.title || existingItem?.name || existingItem?.caption || existingItem?.routeName || `${sectionKey} #${itemId}`,
            actionType: 'PUBLISH',
            payloadSummary: `Request to publish ${sectionKey} item #${itemId}`
          },
          nowMillis
        );
      }
      this.requirePermission(sectionKey, PERMISSION_ACTIONS.PUBLISH);
    } else {
      this.requirePermission(sectionKey, PERMISSION_ACTIONS.EDIT);
    }

    let updatedItem = null;
    this.state[arrayKey] = this.state[arrayKey].map((item) => {
      if (String(item.id) !== String(itemId)) return item;
      updatedItem = {
        ...item,
        isPublished: pubBool,
        verified: sectionKey === 'HIGHER_SECONDARY_COURSES' ? pubBool : item.verified,
        active: sectionKey === 'SOCIAL_MEDIA' ? pubBool : item.active,
        status: pubBool ? 'PUBLISHED' : 'UNPUBLISHED'
      };
      return updatedItem;
    });
    if (!updatedItem) throw new Error('CMS item not found.');
    this.logActivity(
      pubBool ? 'PUBLISH_CMS_ITEM' : 'UNPUBLISH_CMS_ITEM',
      sectionKey,
      `${pubBool ? 'Published' : 'Unpublished'} item #${itemId} in ${sectionKey}`,
      null,
      nowMillis
    );
    this._save();
    this._syncCmsSectionItemToCloud(sectionKey, updatedItem, nowMillis);
    return updatedItem;
  }

  _syncCmsSectionItemToCloud(sectionKey, item, nowMillis = Date.now()) {
    if (!item || !this.cloudSyncEnabled) return;
    const { tenantId } = this.cloudConfig;
    const isPub = item.isPublished !== undefined ? Boolean(item.isPublished) : true;
    const commonFlags = {
      displayOrder: Number(item.displayOrder || 0),
      isVerified: isPub,
      isPublished: isPub,
      isApproved: isPub,
      approvalStatus: isPub ? 'APPROVED' : 'PENDING',
      tenantId,
      syncState: 'SYNCED',
      deletedAtMillis: null
    };

    if (sectionKey === 'ACADEMICS') {
      const remoteId = item.remoteId || deterministicRemoteId('academic_info', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('academic_info', remoteId, {
          title: item.title,
          category: item.category || 'General',
          description: item.description || item.title,
          updatedAtMillis: nowMillis,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'FACILITIES') {
      const remoteId = item.remoteId || deterministicRemoteId('facilities', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('facilities', remoteId, {
          name: item.name,
          category: item.category || 'Campus',
          description: item.description || item.name,
          capacityOrSpec: item.capacityOrSpec || null,
          imageUri: item.imageUri || item.imageUrl || null,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'TRANSPORT') {
      const remoteId = item.remoteId || deterministicRemoteId('transport_routes', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('transport_routes', remoteId, {
          routeNumber: item.routeNumber,
          routeName: item.routeName,
          stopsSummary: item.stopsSummary || item.stops || item.routeName,
          morningTiming: item.morningTiming || item.timings || null,
          afternoonTiming: item.afternoonTiming || null,
          coordinatorContact: item.coordinatorContact || null,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'NEWS') {
      const remoteId = item.remoteId || deterministicRemoteId('news_items', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('news_items', remoteId, {
          title: item.title,
          summary: item.summary || item.title,
          fullBody: item.fullBody || item.summary || item.title,
          publishedDateText: item.publishedDateText || '',
          publishedAtMillis: item.publishedAtMillis || nowMillis,
          isFeatured: Boolean(item.isFeatured),
          imageUri: item.imageUri || item.imageUrl || null,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'ANNOUNCEMENTS') {
      const remoteId = item.remoteId || deterministicRemoteId('announcements', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('announcements', remoteId, {
          title: item.title,
          message: item.message || item.title,
          priorityLabel: item.priorityLabel || null,
          issuedDateText: item.issuedDateText || '',
          issuedAtMillis: item.issuedAtMillis || nowMillis,
          imageUri: item.imageUri || item.imageUrl || null,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'LOGO_BANNER') {
      const remoteId = item.remoteId || deterministicRemoteId('campus_hero_photos', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('campus_hero_photos', remoteId, {
          imageUri: item.imageUri || item.imageUrl || '',
          caption: item.caption || null,
          addedAtMillis: nowMillis,
          remoteId,
          ...commonFlags
        })
      );
    } else if (sectionKey === 'PHOTO_GALLERY') {
      const remoteId = item.remoteId || deterministicRemoteId('gallery_media', item.id);
      this._enqueueCloudOp(() =>
        this._pushCloudDoc('gallery_media', remoteId, {
          albumRemoteId: item.albumRemoteId || 'gallery_albums_1',
          title: item.album || 'Campus Highlights',
          caption: item.caption || null,
          mediaUri: item.mediaUri || item.imageUrl || '',
          mediaType: item.youtubeUrl ? 'YOUTUBE' : 'IMAGE',
          addedAtMillis: nowMillis,
          remoteId,
          ...commonFlags
        })
      );
    }
  }

  /* ==========================================================================
     BASE 4 — ACADEMICS V2 ACCESS CONTROL, 13 CATEGORIES, GALLERY, FACULTY & OWNER CONTROL CENTER
     Mirrors Android AcademicsV2AccessControl.kt & AcademicsV2ContentRepository.kt
     ========================================================================== */

  isSchoolOwner() {
    const session = this.getCurrentSession();
    if (!session) {
      return !this._shouldEnforceRbac();
    }
    return (
      Boolean(session.account?.isActive) &&
      (session.role === MANAGEMENT_ROLES.MASTER_ADMIN || session.role === MANAGEMENT_ROLES.SUPER_OWNER)
    );
  }

  requireSchoolOwner(operationLabel = 'perform this School Owner action') {
    if (!this._shouldEnforceRbac() && !this.getCurrentSession()) {
      return {
        id: 1,
        username: 'masteradmin',
        displayName: this.state.admin.displayName || 'School Owner',
        role: MANAGEMENT_ROLES.MASTER_ADMIN,
        isActive: true
      };
    }
    const session = this.getCurrentSession();
    if (!session || !this.isAdminAuthenticated) {
      throw new Error(`Authentication required to ${operationLabel}.`);
    }
    if (!this.isSchoolOwner()) {
      throw new Error(`Access denied: only the School Owner can ${operationLabel}.`);
    }
    return session.account;
  }

  hasV2CategoryPermission(categoryRouteId, action = PERMISSION_ACTIONS.VIEW) {
    if (!this._shouldEnforceRbac() && !this.getCurrentSession()) return true;
    const session = this.getCurrentSession();
    if (!session || !session.account?.isActive) return false;
    if (this.isSchoolOwner()) return true;

    const primarySec = V2_CATEGORY_PRIMARY_SECTION_MAP[categoryRouteId] || 'ACADEMICS';
    if (this.hasPermission(primarySec, action)) {
      return true;
    }
    if (categoryRouteId === 'news_announcements' && this.hasPermission('ANNOUNCEMENTS', action)) {
      return true;
    }
    if (categoryRouteId === 'about_school' && this.hasPermission('SCHOOL_PROFILE', action)) {
      return true;
    }
    // For non-publishing base sections (ABOUT_SCHOOL / SCHOOL_PROFILE), map PUBLISH check to EDIT or ACADEMICS PUBLISH
    if (
      action === PERMISSION_ACTIONS.PUBLISH &&
      (categoryRouteId === 'about_school' || categoryRouteId === 'faculty_staff')
    ) {
      return (
        this.hasPermission('ABOUT_SCHOOL', PERMISSION_ACTIONS.EDIT) ||
        this.hasPermission('ACADEMICS', PERMISSION_ACTIONS.PUBLISH)
      );
    }
    return false;
  }

  requireV2CategoryPermission(categoryRouteId, action = PERMISSION_ACTIONS.VIEW) {
    if (!this._shouldEnforceRbac()) return;
    const session = this.getCurrentSession();
    if (!session || !this.isAdminAuthenticated) {
      throw new Error(`Authentication required for ${action} on ${categoryRouteId}.`);
    }
    if (!this.hasV2CategoryPermission(categoryRouteId, action)) {
      throw new Error(`Permission denied: you do not have ${action} access for ${categoryRouteId}.`);
    }
  }

  canPublishV2Directly(categoryRouteId, sectionKey = '') {
    if (categoryRouteId === 'examinations' && sectionKey === 'exam_results') {
      return this.isSchoolOwner();
    }
    return this.hasV2CategoryPermission(categoryRouteId, PERMISSION_ACTIONS.PUBLISH);
  }

  validatePhotoOnlyUri(uri) {
    const clean = String(uri || '').trim();
    if (!clean) {
      throw new Error('Photo image is required.');
    }
    const lower = clean.toLowerCase();
    const forbiddenTokens = [
      'youtube.com',
      'youtu.be',
      'vimeo.com',
      '.mp4',
      '.mov',
      '.mkv',
      '.webm',
      '.avi',
      '.3gp',
      'video/'
    ];
    if (forbiddenTokens.some((token) => lower.includes(token))) {
      throw new Error(
        'Card 07 Gallery is strictly for Photos only. Videos and YouTube links must be published via Home Social Posts or Highlights.'
      );
    }
  }

  _ensureV2State() {
    if (!this.state.academicsV2) {
      this.state.academicsV2 = createDefaultAcademicsV2State();
    }
    return this.state.academicsV2;
  }

  _appendV2Audit(actionType, categoryOrSection, targetTitle, details, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    const active = this.getActiveAccount();
    const event = {
      id: `v2_audit_${nowMillis}_${Math.floor(Math.random() * 10000)}`,
      actorName: active?.displayName || this.state.admin.displayName || 'Administrator',
      actorRole: active?.role || MANAGEMENT_ROLES.MASTER_ADMIN,
      actionType,
      categoryOrSection,
      targetTitle,
      details,
      timestampMillis: nowMillis
    };
    v2.accessAuditEvents = [event, ...(v2.accessAuditEvents || [])].slice(0, 500);
    return event;
  }

  getV2AccessAuditEvents() {
    return this._ensureV2State().accessAuditEvents || [];
  }

  /**
   * Creates or updates an Academics V2 record across any of the 13 categories.
   * Mirrors AcademicsV2ContentRepository.saveRecord:
   * - Creating or editing NEVER auto-publishes; records are saved as DRAFT
   *   (or PENDING_APPROVAL when submitForApproval is true).
   */
  saveV2Record(input, nowMillis = Date.now()) {
    const categoryKey = String(input?.categoryKey || input?.categoryId || 'academics').trim();
    const sectionKey = String(input?.sectionKey || input?.subSectionId || '').trim();
    const existingId = input?.existingId ?? input?.id ?? null;
    const isEdit = Boolean(existingId);

    this.requireV2CategoryPermission(
      categoryKey,
      isEdit ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );

    const cleanTitle = String(input?.title || '').trim();
    if (!cleanTitle) {
      throw new Error('Title is required.');
    }
    if (!sectionKey) {
      throw new Error('Sub-section key is required.');
    }

    const v2 = this._ensureV2State();
    const existing = isEdit ? (v2.records || []).find((r) => String(r.id) === String(existingId)) : null;
    const submitForApproval = Boolean(input?.submitForApproval);
    const nextStatus = submitForApproval
      ? ACADEMICS_V2_RECORD_STATUS.PENDING_APPROVAL
      : ACADEMICS_V2_RECORD_STATUS.DRAFT;

    const active = this.getActiveAccount();
    const actorName = active?.displayName || this.state.admin.displayName || 'Administrator';
    const recordId = existing?.id || `v2_rec_${nowMillis}_${Math.floor(Math.random() * 10000)}`;
    const attachmentUris = Array.isArray(input?.attachmentUris)
      ? input.attachmentUris.map((u) => String(u || '').trim()).filter(Boolean)
      : (input?.imageUri ? [String(input.imageUri).trim()].filter(Boolean) : (existing?.attachmentUris || []));

    const updatedRecord = {
      id: recordId,
      categoryKey,
      sectionKey,
      title: cleanTitle,
      summary: String(input?.summary || '').trim(),
      body: String(input?.body ?? input?.summary ?? '').trim(),
      dateOrScheduleText: String(input?.dateOrScheduleText || '').trim(),
      academicYear: String(input?.academicYear || '2025–2026').trim(),
      classOrGroupScope: String(input?.classOrGroupScope || '').trim(),
      badgeText: String(input?.badgeText || '').trim(),
      attachmentUris,
      imageUri: attachmentUris[0] || '',
      externalLink: String(input?.externalLink || '').trim(),
      displayOrder: Number(input?.displayOrder ?? existing?.displayOrder ?? 0),
      status: nextStatus,
      isExamResultAuthorized: false,
      publishAtMillis: input?.publishAtMillis ?? null,
      expireAtMillis: input?.expireAtMillis ?? null,
      createdByName: existing?.createdByName || actorName,
      updatedByName: actorName,
      approvedByName: null,
      createdAtMillis: existing?.createdAtMillis || nowMillis,
      updatedAtMillis: nowMillis,
      publishedAtMillis: existing?.publishedAtMillis || null
    };

    const list = (v2.records || []).filter((r) => String(r.id) !== String(recordId));
    v2.records = [updatedRecord, ...list];

    this.recordVersionSnapshot(
      `V2_${categoryKey.toUpperCase()}`,
      recordId,
      cleanTitle,
      submitForApproval ? 'Submitted for Owner approval' : (existing ? 'Saved draft changes' : 'Created draft'),
      nowMillis,
      {
        contentSnapshotJson: JSON.stringify(updatedRecord),
        previousContentJson: existing ? JSON.stringify(existing) : null,
        approvalStatus: submitForApproval ? 'PENDING' : 'DRAFT'
      }
    );

    const actionType = submitForApproval
      ? 'SUBMIT_FOR_APPROVAL'
      : (existing ? 'EDIT_DRAFT' : 'CREATE_DRAFT');
    this._appendV2Audit(
      actionType,
      `${categoryKey}:${sectionKey}`,
      cleanTitle,
      `Status=${nextStatus}`,
      nowMillis
    );
    this.logActivity(
      actionType,
      V2_CATEGORY_PRIMARY_SECTION_MAP[categoryKey] || 'ACADEMICS',
      `${actionType}: "${cleanTitle}" (${categoryKey}/${sectionKey})`,
      active,
      nowMillis
    );
    this._save();
    return updatedRecord;
  }

  submitV2RecordForApproval(recordId, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    const existing = (v2.records || []).find((r) => String(r.id) === String(recordId));
    if (!existing) throw new Error('Record not found.');
    this.requireV2CategoryPermission(existing.categoryKey, PERMISSION_ACTIONS.EDIT);

    const active = this.getActiveAccount();
    const updated = {
      ...existing,
      status: ACADEMICS_V2_RECORD_STATUS.PENDING_APPROVAL,
      updatedByName: active?.displayName || this.state.admin.displayName || 'Administrator',
      updatedAtMillis: nowMillis
    };
    v2.records = v2.records.map((r) => (String(r.id) === String(recordId) ? updated : r));
    this._appendV2Audit(
      'SUBMIT_FOR_OWNER_APPROVAL',
      `${existing.categoryKey}:${existing.sectionKey}`,
      existing.title,
      'Queued for School Owner approval',
      nowMillis
    );
    this.logActivity(
      'SUBMIT_V2_APPROVAL',
      V2_CATEGORY_PRIMARY_SECTION_MAP[existing.categoryKey] || 'ACADEMICS',
      `Submitted "${existing.title}" for School Owner approval`,
      active,
      nowMillis
    );
    this._save();
    return updated;
  }

  publishV2Record(recordId, { authorizeExamResult = false } = {}, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    const existing = (v2.records || []).find((r) => String(r.id) === String(recordId));
    if (!existing) throw new Error('Record not found.');

    const isExamResultsSection =
      existing.categoryKey === 'examinations' && existing.sectionKey === 'exam_results';

    if (isExamResultsSection) {
      this.requireSchoolOwner('approve and publish Exam Results');
      if (!authorizeExamResult) {
        throw new Error('Exam Results require explicit School Owner authorization before publishing.');
      }
    } else {
      this.requireV2CategoryPermission(existing.categoryKey, PERMISSION_ACTIONS.PUBLISH);
    }

    const active = this.getActiveAccount();
    const actorName = active?.displayName || this.state.admin.displayName || 'School Owner';
    const updated = {
      ...existing,
      status: ACADEMICS_V2_RECORD_STATUS.PUBLISHED,
      isExamResultAuthorized: isExamResultsSection ? true : existing.isExamResultAuthorized,
      approvedByName: actorName,
      updatedByName: actorName,
      updatedAtMillis: nowMillis,
      publishedAtMillis: nowMillis
    };
    v2.records = v2.records.map((r) => (String(r.id) === String(recordId) ? updated : r));

    this.recordVersionSnapshot(
      `V2_${existing.categoryKey.toUpperCase()}`,
      existing.id,
      existing.title,
      isExamResultsSection ? 'School Owner authorized & published Exam Results' : 'Published to Public App',
      nowMillis,
      {
        contentSnapshotJson: JSON.stringify(updated),
        previousContentJson: JSON.stringify(existing),
        approvalStatus: 'APPROVED'
      }
    );
    this._appendV2Audit(
      isExamResultsSection ? 'OWNER_PUBLISH_EXAM_RESULTS' : 'PUBLISH_RECORD',
      `${existing.categoryKey}:${existing.sectionKey}`,
      existing.title,
      `Published by ${actorName}`,
      nowMillis
    );
    this.logActivity(
      isExamResultsSection ? 'OWNER_PUBLISH_EXAM_RESULTS' : 'PUBLISH_V2_RECORD',
      V2_CATEGORY_PRIMARY_SECTION_MAP[existing.categoryKey] || 'ACADEMICS',
      `Published V2 record "${existing.title}" (${existing.categoryKey}/${existing.sectionKey})`,
      active,
      nowMillis
    );
    this._save();
    this._syncV2RecordToCloud(updated, nowMillis);
    return updated;
  }

  ownerApproveAndPublishV2Record(recordId, { authorizeExamResult = true } = {}, nowMillis = Date.now()) {
    this.requireSchoolOwner('approve and publish queued records');
    return this.publishV2Record(recordId, { authorizeExamResult }, nowMillis);
  }

  updateV2RecordStatus(recordId, targetStatus, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    const existing = (v2.records || []).find((r) => String(r.id) === String(recordId));
    if (!existing) throw new Error('Record not found.');

    if (targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED) {
      return this.publishV2Record(
        recordId,
        { authorizeExamResult: this.isSchoolOwner() },
        nowMillis
      );
    }

    this.requireV2CategoryPermission(existing.categoryKey, PERMISSION_ACTIONS.EDIT);
    const active = this.getActiveAccount();
    const actorName = active?.displayName || this.state.admin.displayName || 'Administrator';
    const updated = {
      ...existing,
      status: targetStatus,
      updatedByName: actorName,
      updatedAtMillis: nowMillis
    };
    v2.records = v2.records.map((r) => (String(r.id) === String(recordId) ? updated : r));
    this._appendV2Audit(
      `STATUS_${targetStatus}`,
      `${existing.categoryKey}:${existing.sectionKey}`,
      existing.title,
      `Changed status to ${targetStatus}`,
      nowMillis
    );
    this.logActivity(
      `V2_STATUS_${targetStatus}`,
      V2_CATEGORY_PRIMARY_SECTION_MAP[existing.categoryKey] || 'ACADEMICS',
      `Updated "${existing.title}" status to ${targetStatus}`,
      active,
      nowMillis
    );
    this._save();
    this._syncV2RecordToCloud(updated, nowMillis);
    return updated;
  }

  deleteV2Record(recordId, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    const existing = (v2.records || []).find((r) => String(r.id) === String(recordId));
    if (!existing) return false;
    this.requireV2CategoryPermission(existing.categoryKey, PERMISSION_ACTIONS.DELETE);
    v2.records = v2.records.filter((r) => String(r.id) !== String(recordId));
    this._appendV2Audit(
      'DELETE_RECORD',
      `${existing.categoryKey}:${existing.sectionKey}`,
      existing.title,
      'Deleted record',
      nowMillis
    );
    this.logActivity(
      'DELETE_V2_RECORD',
      V2_CATEGORY_PRIMARY_SECTION_MAP[existing.categoryKey] || 'ACADEMICS',
      `Deleted V2 record "${existing.title}"`,
      null,
      nowMillis
    );
    this._save();
    if (this.cloudSyncEnabled) {
      const remoteId = deterministicRemoteId('academic_info', recordId);
      this._enqueueCloudOp(() => this._deleteCloudDoc('academic_info', remoteId));
    }
    return true;
  }

  _syncV2RecordToCloud(record, nowMillis = Date.now()) {
    if (!record || !this.cloudSyncEnabled) return;
    const { tenantId } = this.cloudConfig;
    const isPub = record.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED;
    const remoteId = deterministicRemoteId('academic_info', record.id);
    this._enqueueCloudOp(() =>
      this._pushCloudDoc('academic_info', remoteId, {
        title: record.title,
        category: `${record.categoryKey}:${record.sectionKey}`,
        description: record.body || record.summary || record.title,
        displayOrder: Number(record.displayOrder || 0),
        isVerified: isPub,
        isPublished: isPub,
        isApproved: isPub,
        approvalStatus: isPub ? 'APPROVED' : 'PENDING',
        updatedAtMillis: nowMillis,
        remoteId,
        tenantId,
        syncState: 'SYNCED',
        deletedAtMillis: null
      })
    );
  }

  getPublicV2Records(categoryKey, sectionKey = null, nowMillis = Date.now()) {
    const v2 = this._ensureV2State();
    return (v2.records || [])
      .filter((record) => {
        if (record.categoryKey !== categoryKey) return false;
        if (sectionKey && record.sectionKey !== sectionKey) return false;
        if (record.status !== ACADEMICS_V2_RECORD_STATUS.PUBLISHED) return false;
        if (
          record.categoryKey === 'examinations' &&
          record.sectionKey === 'exam_results' &&
          !record.isExamResultAuthorized
        ) {
          return false;
        }
        if (record.publishAtMillis && nowMillis < record.publishAtMillis) return false;
        if (record.expireAtMillis && nowMillis >= record.expireAtMillis) return false;
        return true;
      })
      .sort((a, b) => {
        const orderDiff = Number(a.displayOrder || 0) - Number(b.displayOrder || 0);
        if (orderDiff !== 0) return orderDiff;
        return Number(b.publishedAtMillis || b.updatedAtMillis || 0) - Number(a.publishedAtMillis || a.updatedAtMillis || 0);
      });
  }

  /* ==========================================================================
     CARD 07 — PHOTO-ONLY GALLERY MANAGER (ALBUMS + MULTI-ALBUM PHOTO REUSE)
     ========================================================================== */

  saveV2GalleryAlbum(input, nowMillis = Date.now()) {
    const existingId = input?.existingId ?? input?.id ?? null;
    this.requireV2CategoryPermission(
      'gallery',
      existingId ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const cleanTitle = String(input?.title || '').trim();
    if (!cleanTitle) throw new Error('Album title is required.');
    const v2 = this._ensureV2State();
    const existing = existingId
      ? (v2.galleryAlbums || []).find((a) => String(a.id) === String(existingId))
      : null;

    const albumId = existing?.id || `v2_album_${nowMillis}_${Math.floor(Math.random() * 10000)}`;
    const updatedAlbum = {
      id: albumId,
      sectionKey: String(input?.sectionKey || existing?.sectionKey || 'event_wise_albums').trim(),
      title: cleanTitle,
      description: String(input?.description || '').trim(),
      dateText: String(input?.dateText || '').trim(),
      coverPhotoUri: String(input?.coverPhotoUri || input?.coverImageUri || existing?.coverPhotoUri || '').trim(),
      status: ACADEMICS_V2_RECORD_STATUS.DRAFT,
      displayOrder: Number(input?.displayOrder ?? existing?.displayOrder ?? 0),
      createdAtMillis: existing?.createdAtMillis || nowMillis,
      updatedAtMillis: nowMillis
    };
    v2.galleryAlbums = [
      updatedAlbum,
      ...(v2.galleryAlbums || []).filter((a) => String(a.id) !== String(albumId))
    ];
    this._appendV2Audit(
      existing ? 'EDIT_GALLERY_ALBUM_DRAFT' : 'CREATE_GALLERY_ALBUM_DRAFT',
      'gallery',
      cleanTitle,
      `Album saved as DRAFT (${updatedAlbum.sectionKey})`,
      nowMillis
    );
    this._save();
    return updatedAlbum;
  }

  updateV2GalleryAlbumStatus(albumId, targetStatus, nowMillis = Date.now()) {
    this.requireV2CategoryPermission(
      'gallery',
      targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED
        ? PERMISSION_ACTIONS.PUBLISH
        : PERMISSION_ACTIONS.EDIT
    );
    const v2 = this._ensureV2State();
    const existing = (v2.galleryAlbums || []).find((a) => String(a.id) === String(albumId));
    if (!existing) throw new Error('Gallery album not found.');
    const updated = {
      ...existing,
      status: targetStatus,
      updatedAtMillis: nowMillis
    };
    v2.galleryAlbums = v2.galleryAlbums.map((a) => (String(a.id) === String(albumId) ? updated : a));
    this._appendV2Audit(
      `GALLERY_ALBUM_${targetStatus}`,
      'gallery',
      existing.title,
      `Album status -> ${targetStatus}`,
      nowMillis
    );
    this._save();
    return updated;
  }

  deleteV2GalleryAlbum(albumId, nowMillis = Date.now()) {
    this.requireV2CategoryPermission('gallery', PERMISSION_ACTIONS.DELETE);
    const v2 = this._ensureV2State();
    const existing = (v2.galleryAlbums || []).find((a) => String(a.id) === String(albumId));
    if (!existing) return false;
    v2.galleryAlbums = v2.galleryAlbums.filter((a) => String(a.id) !== String(albumId));
    v2.galleryPhotos = (v2.galleryPhotos || []).map((photo) => ({
      ...photo,
      albumIds: (photo.albumIds || []).filter((id) => String(id) !== String(albumId))
    }));
    this._appendV2Audit('DELETE_GALLERY_ALBUM', 'gallery', existing.title, 'Deleted album', nowMillis);
    this._save();
    return true;
  }

  saveV2GalleryPhoto(input, nowMillis = Date.now()) {
    const existingId = input?.existingId ?? input?.id ?? null;
    this.requireV2CategoryPermission(
      'gallery',
      existingId ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const imageUri = String(input?.imageUri || input?.imageUrl || '').trim();
    this.validatePhotoOnlyUri(imageUri);

    const cleanTitle = String(input?.title || '').trim();
    if (!cleanTitle) throw new Error('Photo title is required.');

    const v2 = this._ensureV2State();
    const validAlbumIds = new Set((v2.galleryAlbums || []).map((a) => String(a.id)));
    const rawAlbumIds = Array.isArray(input?.albumIds) ? input.albumIds : [];
    const normalizedAlbums = Array.from(
      new Set(rawAlbumIds.map((id) => String(id)).filter((id) => validAlbumIds.has(id)))
    );
    if (normalizedAlbums.length === 0) {
      throw new Error('Select at least one photo album.');
    }

    const existing = existingId
      ? (v2.galleryPhotos || []).find((p) => String(p.id) === String(existingId))
      : null;
    const active = this.getActiveAccount();
    const photoId = existing?.id || `v2_photo_${nowMillis}_${Math.floor(Math.random() * 10000)}`;
    const updatedPhoto = {
      id: photoId,
      title: cleanTitle,
      description: String(input?.description || input?.caption || '').trim(),
      dateText: String(input?.dateText || '').trim(),
      imageUri,
      albumIds: normalizedAlbums,
      status: ACADEMICS_V2_RECORD_STATUS.DRAFT,
      createdByName: existing?.createdByName || active?.displayName || 'Administrator',
      createdAtMillis: existing?.createdAtMillis || nowMillis,
      updatedAtMillis: nowMillis,
      publishedAtMillis: existing?.publishedAtMillis || null
    };

    v2.galleryPhotos = [
      updatedPhoto,
      ...(v2.galleryPhotos || []).filter((p) => String(p.id) !== String(photoId))
    ];
    v2.galleryAlbums = (v2.galleryAlbums || []).map((album) => {
      if (normalizedAlbums.includes(String(album.id)) && !album.coverPhotoUri) {
        return { ...album, coverPhotoUri: imageUri };
      }
      return album;
    });
    this._appendV2Audit(
      existing ? 'EDIT_GALLERY_PHOTO_DRAFT' : 'UPLOAD_GALLERY_PHOTO_DRAFT',
      'gallery',
      cleanTitle,
      `Assigned to ${normalizedAlbums.length} album(s)`,
      nowMillis
    );
    this._save();
    return updatedPhoto;
  }

  assignV2GalleryPhotoToAlbums(photoId, albumIds, nowMillis = Date.now()) {
    this.requireV2CategoryPermission('gallery', PERMISSION_ACTIONS.EDIT);
    const v2 = this._ensureV2State();
    const existing = (v2.galleryPhotos || []).find((p) => String(p.id) === String(photoId));
    if (!existing) throw new Error('Photo not found.');
    const validAlbumIds = new Set((v2.galleryAlbums || []).map((a) => String(a.id)));
    const normalized = Array.from(
      new Set((albumIds || []).map((id) => String(id)).filter((id) => validAlbumIds.has(id)))
    );
    if (normalized.length === 0) {
      throw new Error('Photo must belong to at least one album.');
    }
    const updated = {
      ...existing,
      albumIds: normalized,
      updatedAtMillis: nowMillis
    };
    v2.galleryPhotos = v2.galleryPhotos.map((p) => (String(p.id) === String(photoId) ? updated : p));
    this._appendV2Audit(
      'REUSE_PHOTO_ACROSS_ALBUMS',
      'gallery',
      existing.title,
      `Photo linked to ${normalized.length} album(s)`,
      nowMillis
    );
    this._save();
    return updated;
  }

  updateV2GalleryPhotoStatus(photoId, targetStatus, nowMillis = Date.now()) {
    this.requireV2CategoryPermission(
      'gallery',
      targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED
        ? PERMISSION_ACTIONS.PUBLISH
        : PERMISSION_ACTIONS.EDIT
    );
    const v2 = this._ensureV2State();
    const existing = (v2.galleryPhotos || []).find((p) => String(p.id) === String(photoId));
    if (!existing) throw new Error('Photo not found.');
    const updated = {
      ...existing,
      status: targetStatus,
      updatedAtMillis: nowMillis,
      publishedAtMillis:
        targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED ? nowMillis : existing.publishedAtMillis
    };
    v2.galleryPhotos = v2.galleryPhotos.map((p) => (String(p.id) === String(photoId) ? updated : p));
    this._appendV2Audit(
      `GALLERY_PHOTO_${targetStatus}`,
      'gallery',
      existing.title,
      `Photo status -> ${targetStatus}`,
      nowMillis
    );
    this._save();
    return updated;
  }

  deleteV2GalleryPhoto(photoId, nowMillis = Date.now()) {
    this.requireV2CategoryPermission('gallery', PERMISSION_ACTIONS.DELETE);
    const v2 = this._ensureV2State();
    const existing = (v2.galleryPhotos || []).find((p) => String(p.id) === String(photoId));
    if (!existing) return false;
    v2.galleryPhotos = v2.galleryPhotos.filter((p) => String(p.id) !== String(photoId));
    this._appendV2Audit('DELETE_GALLERY_PHOTO', 'gallery', existing.title, 'Deleted photo', nowMillis);
    this._save();
    return true;
  }

  getPublicV2GalleryAlbums() {
    const v2 = this._ensureV2State();
    return (v2.galleryAlbums || [])
      .filter((a) => a.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED)
      .sort((a, b) => Number(a.displayOrder || 0) - Number(b.displayOrder || 0));
  }

  getPublicV2GalleryPhotosForAlbum(albumId) {
    const v2 = this._ensureV2State();
    const album = (v2.galleryAlbums || []).find(
      (a) => String(a.id) === String(albumId) && a.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED
    );
    if (!album) return [];
    return (v2.galleryPhotos || [])
      .filter(
        (p) =>
          p.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED &&
          Array.isArray(p.albumIds) &&
          p.albumIds.map(String).includes(String(albumId))
      )
      .sort((a, b) => Number(b.publishedAtMillis || b.updatedAtMillis || 0) - Number(a.publishedAtMillis || a.updatedAtMillis || 0));
  }

  /* ==========================================================================
     CARD 11 — FACULTY & STAFF PRIVACY-GUARDED 8-FIELD MANAGER
     ========================================================================== */

  saveV2FacultyProfile(input, nowMillis = Date.now()) {
    const existingId = input?.existingId ?? input?.id ?? null;
    this.requireV2CategoryPermission(
      'faculty_staff',
      existingId ? PERMISSION_ACTIONS.EDIT : PERMISSION_ACTIONS.CREATE
    );
    const cleanName = String(input?.teacherName || '').trim();
    const cleanDesignation = String(input?.designation || '').trim();
    if (!cleanName) throw new Error('Teacher Name is required.');
    if (!cleanDesignation) throw new Error('Designation / Role is required.');

    const v2 = this._ensureV2State();
    const existing = existingId
      ? (v2.facultyProfiles || []).find((f) => String(f.id) === String(existingId))
      : null;
    const profileId = existing?.id || `v2_faculty_${nowMillis}_${Math.floor(Math.random() * 10000)}`;

    // Store strictly the 8 approved professional fields + school approval & staff consent flags
    const updated = {
      id: profileId,
      sectionKey: String(input?.sectionKey || existing?.sectionKey || 'staff_profiles').trim(),
      teacherName: cleanName,
      approvedProfilePhotoUri: String(input?.approvedProfilePhotoUri || '').trim(),
      designation: cleanDesignation,
      department: String(input?.department || '').trim(),
      teachingSubjects: String(input?.teachingSubjects || '').trim(),
      educationalQualifications: String(input?.educationalQualifications || '').trim(),
      teachingExperience: String(input?.teachingExperience || '').trim(),
      professionalIntroduction: String(input?.professionalIntroduction || '').trim(),
      isSchoolApproved: Boolean(input?.isSchoolApproved),
      hasStaffConsent: Boolean(input?.hasStaffConsent),
      status: ACADEMICS_V2_RECORD_STATUS.DRAFT,
      displayOrder: Number(input?.displayOrder ?? existing?.displayOrder ?? 0),
      updatedAtMillis: nowMillis
    };

    v2.facultyProfiles = [
      updated,
      ...(v2.facultyProfiles || []).filter((f) => String(f.id) !== String(profileId))
    ];
    // Keep top-level state.facultyProfiles synchronized for any legacy consumers
    this.state.facultyProfiles = v2.facultyProfiles;

    this._appendV2Audit(
      existing ? 'EDIT_FACULTY_PROFILE_DRAFT' : 'CREATE_FACULTY_PROFILE_DRAFT',
      'faculty_staff',
      cleanName,
      `Approved=${updated.isSchoolApproved}, Consent=${updated.hasStaffConsent}`,
      nowMillis
    );
    this._save();
    return updated;
  }

  updateV2FacultyProfileStatus(profileId, targetStatus, nowMillis = Date.now()) {
    this.requireV2CategoryPermission(
      'faculty_staff',
      targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED
        ? PERMISSION_ACTIONS.PUBLISH
        : PERMISSION_ACTIONS.EDIT
    );
    const v2 = this._ensureV2State();
    const existing = (v2.facultyProfiles || []).find((f) => String(f.id) === String(profileId));
    if (!existing) throw new Error('Faculty profile not found.');

    if (targetStatus === ACADEMICS_V2_RECORD_STATUS.PUBLISHED) {
      if (!existing.isSchoolApproved || !existing.hasStaffConsent) {
        throw new Error(
          'Faculty profile can only be published after both School Approval and Staff Consent are confirmed.'
        );
      }
    }

    const updated = {
      ...existing,
      status: targetStatus,
      updatedAtMillis: nowMillis
    };
    v2.facultyProfiles = v2.facultyProfiles.map((f) =>
      String(f.id) === String(profileId) ? updated : f
    );
    this.state.facultyProfiles = v2.facultyProfiles;
    this._appendV2Audit(
      `FACULTY_PROFILE_${targetStatus}`,
      'faculty_staff',
      existing.teacherName,
      `Status -> ${targetStatus}`,
      nowMillis
    );
    this._save();
    return updated;
  }

  deleteV2FacultyProfile(profileId, nowMillis = Date.now()) {
    this.requireV2CategoryPermission('faculty_staff', PERMISSION_ACTIONS.DELETE);
    const v2 = this._ensureV2State();
    const existing = (v2.facultyProfiles || []).find((f) => String(f.id) === String(profileId));
    if (!existing) return false;
    v2.facultyProfiles = v2.facultyProfiles.filter((f) => String(f.id) !== String(profileId));
    this.state.facultyProfiles = v2.facultyProfiles;
    this._appendV2Audit(
      'DELETE_FACULTY_PROFILE',
      'faculty_staff',
      existing.teacherName,
      'Deleted faculty profile',
      nowMillis
    );
    this._save();
    return true;
  }

  getPublicV2FacultyProfiles(sectionKey = null) {
    const v2 = this._ensureV2State();
    return (v2.facultyProfiles || [])
      .filter((f) => {
        if (sectionKey && f.sectionKey !== sectionKey) return false;
        return (
          f.status === ACADEMICS_V2_RECORD_STATUS.PUBLISHED &&
          Boolean(f.isSchoolApproved) &&
          Boolean(f.hasStaffConsent)
        );
      })
      .sort((a, b) => Number(a.displayOrder || 0) - Number(b.displayOrder || 0))
      .map((f) => ({
        id: f.id,
        sectionKey: f.sectionKey,
        teacherName: f.teacherName,
        approvedProfilePhotoUri: f.approvedProfilePhotoUri,
        designation: f.designation,
        department: f.department,
        teachingSubjects: f.teachingSubjects,
        educationalQualifications: f.educationalQualifications,
        teachingExperience: f.teachingExperience,
        professionalIntroduction: f.professionalIntroduction
      }));
  }

  /* ==========================================================================
     CARD 08 — SCHOOL OWNER CONTROL CENTER (ALL 6 SECTIONS)
     ========================================================================== */

  getOwnerPrivateProfile() {
    this.requireSchoolOwner('view School Owner profile');
    return { ...this._ensureV2State().ownerPrivateProfile };
  }

  updateOwnerPrivateProfile(input, nowMillis = Date.now()) {
    this.requireSchoolOwner('update School Owner profile');
    const v2 = this._ensureV2State();
    const updated = {
      ownerName: String(input?.ownerName ?? v2.ownerPrivateProfile?.ownerName ?? '').trim(),
      designation:
        String(input?.designation || '').trim() || 'School Owner / Correspondent',
      privateContactPhone: String(input?.privateContactPhone ?? '').trim(),
      privateContactEmail: String(input?.privateContactEmail ?? '').trim(),
      governanceNote: String(input?.governanceNote ?? '').trim(),
      updatedAtMillis: nowMillis
    };
    v2.ownerPrivateProfile = updated;
    this._appendV2Audit(
      'UPDATE_OWNER_PROFILE',
      'SchoolOwnerControlCenter',
      updated.ownerName || 'School Owner',
      'Updated private School Owner profile',
      nowMillis
    );
    this._save();
    return { ...updated };
  }

  submitStaffAccessRequest({ requestedRole = MANAGEMENT_ROLES.STAFF, requestedCategoryKeys = [], reason = '' } = {}, nowMillis = Date.now()) {
    const active = this.getActiveAccount();
    if (!active) {
      throw new Error('Must be logged in to submit an access request.');
    }
    const v2 = this._ensureV2State();
    const request = {
      id: `v2_req_${nowMillis}_${Math.floor(Math.random() * 10000)}`,
      staffUsername: active.username,
      staffDisplayName: active.displayName,
      requestedRole,
      requestedCategoryKeys: Array.isArray(requestedCategoryKeys) ? requestedCategoryKeys : [],
      reason: String(reason || '').trim(),
      status: 'PENDING',
      requestedAtMillis: nowMillis,
      reviewedByName: null,
      reviewedAtMillis: null,
      reviewNote: ''
    };
    v2.staffAccessRequests = [request, ...(v2.staffAccessRequests || [])];
    this._appendV2Audit(
      'SUBMIT_STAFF_ACCESS_REQUEST',
      'SchoolOwnerControlCenter',
      active.displayName,
      `Requested access to ${request.requestedCategoryKeys.join(', ')}`,
      nowMillis
    );
    this._save();
    return request;
  }

  reviewStaffAccessRequest(requestId, approve, reviewNote = '', nowMillis = Date.now()) {
    const owner = this.requireSchoolOwner('review staff access requests');
    const v2 = this._ensureV2State();
    const existing = (v2.staffAccessRequests || []).find((r) => String(r.id) === String(requestId));
    if (!existing) throw new Error('Access request not found.');
    const nextStatus = approve ? 'APPROVED' : 'REJECTED';
    const updated = {
      ...existing,
      status: nextStatus,
      reviewedByName: owner.displayName,
      reviewedAtMillis: nowMillis,
      reviewNote: String(reviewNote || '').trim()
    };
    v2.staffAccessRequests = v2.staffAccessRequests.map((r) =>
      String(r.id) === String(requestId) ? updated : r
    );
    this._appendV2Audit(
      approve ? 'APPROVE_STAFF_ACCESS' : 'REJECT_STAFF_ACCESS',
      'SchoolOwnerControlCenter',
      existing.staffDisplayName,
      `Status -> ${nextStatus}`,
      nowMillis
    );
    this._save();
    return updated;
  }

  revokeStaffAccessRequest(requestId, reviewNote = '', nowMillis = Date.now()) {
    const owner = this.requireSchoolOwner('revoke staff access');
    const v2 = this._ensureV2State();
    const existing = (v2.staffAccessRequests || []).find((r) => String(r.id) === String(requestId));
    if (!existing) throw new Error('Access request not found.');
    const updated = {
      ...existing,
      status: 'REVOKED',
      reviewedByName: owner.displayName,
      reviewedAtMillis: nowMillis,
      reviewNote: String(reviewNote || '').trim()
    };
    v2.staffAccessRequests = v2.staffAccessRequests.map((r) =>
      String(r.id) === String(requestId) ? updated : r
    );
    this._appendV2Audit(
      'REVOKE_STAFF_ACCESS',
      'SchoolOwnerControlCenter',
      existing.staffDisplayName,
      'Access revoked by School Owner',
      nowMillis
    );
    this._save();
    return updated;
  }

  /* ==========================================================================
     BASE 4 — JSON BACKUP & RESTORE WITH PREVIEW INSPECTION & CREDENTIAL EXCLUSION
     ========================================================================== */

  exportBackupJson(options = {}) {
    this.requirePermission('BACKUP_RESTORE', PERMISSION_ACTIONS.VIEW);
    const clone = JSON.parse(JSON.stringify(this.state));
    // Never export salted PIN credentials or recovery verifiers in backup JSON
    if (clone.admin) {
      delete clone.admin.pinHash;
      delete clone.admin.pinSalt;
    }
    if (Array.isArray(clone.managementAccounts)) {
      clone.managementAccounts = clone.managementAccounts.map((acc) =>
        this._sanitizeAccountForPublicView(acc)
      );
    }
    const nowMillis = Date.now();
    clone.backupMetadata = {
      exportedAtMillis: nowMillis,
      appVersionName: '1.0.0-web',
      schemaVersion: 2,
      passwordProtected: Boolean(options?.password && String(options.password).trim().length >= 4),
      passwordVerifier:
        options?.password && String(options.password).trim().length >= 4
          ? PinSecurityManager.deriveVerifierBase64(String(options.password).trim(), 'chevalier_backup_v1')
          : null
    };
    this.logActivity('EXPORT_BACKUP', 'BACKUP_RESTORE', 'Exported verified school JSON backup');
    this._save();
    return JSON.stringify(clone, null, 2);
  }

  inspectBackupJson(rawJson, password = '') {
    this.requirePermission('BACKUP_RESTORE', PERMISSION_ACTIONS.VIEW);
    let parsed = null;
    try {
      parsed = typeof rawJson === 'string' ? JSON.parse(rawJson) : rawJson;
    } catch {
      throw new Error('Invalid JSON backup file.');
    }
    if (!parsed || typeof parsed !== 'object' || !parsed.profile) {
      throw new Error('Invalid Chevalier Academy backup format.');
    }
    if (parsed.backupMetadata?.passwordProtected && parsed.backupMetadata?.passwordVerifier) {
      const cleanPass = String(password || '').trim();
      const verified = PinSecurityManager.verifySecretConstantTime(
        cleanPass,
        'chevalier_backup_v1',
        parsed.backupMetadata.passwordVerifier
      );
      if (!verified) {
        throw new Error('Incorrect backup password.');
      }
    }

    const v2 = parsed.academicsV2 || {};
    const accountCount = Array.isArray(parsed.managementAccounts) ? parsed.managementAccounts.length : 0;
    const contentRecordCount =
      (Array.isArray(parsed.academics) ? parsed.academics.length : 0) +
      (Array.isArray(parsed.courses) ? parsed.courses.length : 0) +
      (Array.isArray(parsed.facilities) ? parsed.facilities.length : 0) +
      (Array.isArray(parsed.transportRoutes) ? parsed.transportRoutes.length : 0) +
      (Array.isArray(parsed.news) ? parsed.news.length : 0) +
      (Array.isArray(parsed.announcements) ? parsed.announcements.length : 0) +
      (Array.isArray(parsed.schoolPosts) ? parsed.schoolPosts.length : 0) +
      (Array.isArray(parsed.schoolHighlights) ? parsed.schoolHighlights.length : 0) +
      (Array.isArray(v2.records) ? v2.records.length : 0) +
      (Array.isArray(v2.facultyProfiles) ? v2.facultyProfiles.length : 0);

    const galleryItemCount =
      (Array.isArray(parsed.heroPhotos) ? parsed.heroPhotos.length : 0) +
      (Array.isArray(parsed.galleryAlbums) ? parsed.galleryAlbums.length : 0) +
      (Array.isArray(parsed.galleryMedia) ? parsed.galleryMedia.length : 0) +
      (Array.isArray(v2.galleryAlbums) ? v2.galleryAlbums.length : 0) +
      (Array.isArray(v2.galleryPhotos) ? v2.galleryPhotos.length : 0);

    const mediaFileCount = Array.isArray(parsed.mediaStudioItems) ? parsed.mediaStudioItems.length : 0;
    const historyAndLogCount =
      (Array.isArray(parsed.versionHistory) ? parsed.versionHistory.length : 0) +
      (Array.isArray(parsed.admin?.activityLog) ? parsed.admin.activityLog.length : 0) +
      (Array.isArray(v2.accessAuditEvents) ? v2.accessAuditEvents.length : 0);

    return {
      exportedAtMillis: Number(parsed.backupMetadata?.exportedAtMillis || Date.now()),
      appVersionName: parsed.backupMetadata?.appVersionName || '1.0.0',
      schemaVersion: Number(parsed.backupMetadata?.schemaVersion || parsed.version || 1),
      accountCount,
      contentRecordCount,
      galleryItemCount,
      mediaFileCount,
      historyAndLogCount
    };
  }

  importBackupJson(rawJson, options = {}) {
    this.requirePermission('BACKUP_RESTORE', PERMISSION_ACTIONS.EDIT);
    const password = typeof options === 'string' ? options : (options?.password || '');
    // Validate format & optional password via inspectBackupJson
    this.inspectBackupJson(rawJson, password);

    const parsed = typeof rawJson === 'string' ? JSON.parse(rawJson) : rawJson;
    const preservedAdmin = {
      pinHash: this.state.admin.pinHash,
      pinSalt: this.state.admin.pinSalt,
      displayName: this.state.admin.displayName,
      failedAttempts: 0,
      lockoutUntil: 0,
      activityLog: Array.isArray(parsed.admin?.activityLog)
        ? parsed.admin.activityLog
        : this.state.admin.activityLog
    };
    const preservedAccounts = this.state.managementAccounts;
    const defaults = createVerifiedDefaultState();
    const defaultV2 = createDefaultAcademicsV2State();
    const rawV2 = parsed.academicsV2 || {};

    this.state = {
      ...defaults,
      ...parsed,
      academicsV2: {
        records: Array.isArray(rawV2.records) ? rawV2.records : [],
        galleryAlbums: Array.isArray(rawV2.galleryAlbums) ? rawV2.galleryAlbums : [],
        galleryPhotos: Array.isArray(rawV2.galleryPhotos) ? rawV2.galleryPhotos : [],
        facultyProfiles: Array.isArray(rawV2.facultyProfiles) ? rawV2.facultyProfiles : [],
        ownerPrivateProfile: {
          ...defaultV2.ownerPrivateProfile,
          ...(rawV2.ownerPrivateProfile || {})
        },
        staffAccessRequests: Array.isArray(rawV2.staffAccessRequests) ? rawV2.staffAccessRequests : [],
        accessAuditEvents: Array.isArray(rawV2.accessAuditEvents) ? rawV2.accessAuditEvents : []
      },
      schoolPosts: Array.isArray(parsed.schoolPosts) ? parsed.schoolPosts : [],
      schoolHighlights: Array.isArray(parsed.schoolHighlights) ? parsed.schoolHighlights : [],
      managementAccounts: preservedAccounts?.length
        ? preservedAccounts
        : (Array.isArray(parsed.managementAccounts) ? parsed.managementAccounts : []),
      contentApprovals: Array.isArray(parsed.contentApprovals) ? parsed.contentApprovals : [],
      versionHistory: Array.isArray(parsed.versionHistory) ? parsed.versionHistory : [],
      mediaStudioItems: Array.isArray(parsed.mediaStudioItems) ? parsed.mediaStudioItems : [],
      adminNotifications: Array.isArray(parsed.adminNotifications) ? parsed.adminNotifications : [],
      adminHeroBanner: { ...defaults.adminHeroBanner, ...(parsed.adminHeroBanner || {}) },
      preferences: { ...defaults.preferences, ...(parsed.preferences || {}) },
      profile: { ...defaults.profile, ...(parsed.profile || {}) },
      admin: preservedAdmin
    };
    this.logActivity('RESTORE_BACKUP', 'BACKUP_RESTORE', 'Restored school records from JSON backup');
    this._save();
  }
}

