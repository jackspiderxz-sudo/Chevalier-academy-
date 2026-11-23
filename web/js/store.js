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
  constructor(customStorage = null, cloudOptions = null) {
    this.storage = getStorageBackend(customStorage);
    this.listeners = new Set();
    this.state = this._load();
    this.isAdminAuthenticated = false;

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
    if (this.pinGateReady) return true;
    const fetchFn = this._getFetch();
    if (!fetchFn) return false;

    const { tenantId, pinGateDocId, pinGateEmail, apiKey } = this.cloudConfig;
    const now = Date.now();
    const docUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}/management_accounts/${pinGateDocId}${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;
    const payload = {
      uid: pinGateDocId,
      username: 'superowner',
      displayName: this.state.admin.displayName || 'Chevalier Admin',
      email: pinGateEmail,
      role: 'SUPER_OWNER',
      isActive: true,
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
    const runQueryUrl = `${this._getBaseDocumentsUrl()}/schools/${tenantId}:runQuery${apiKey ? `?key=${encodeURIComponent(apiKey)}` : ''}`;

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

    const whereClause =
      structuredFilters.length === 1
        ? structuredFilters[0]
        : {
            compositeFilter: {
              op: 'AND',
              filters: structuredFilters
            }
          };

    try {
      const res = await fetchFn(runQueryUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          structuredQuery: {
            from: [{ collectionId }],
            where: whereClause
          }
        })
      });
      if (!res.ok) return null;
      const rows = await res.json();
      if (!Array.isArray(rows)) return [];
      return rows
        .filter((r) => r && r.document)
        .map((r) => fromFirestoreDocument(r.document))
        .filter(Boolean);
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
          caption: doc.data.caption || ''
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
          description: doc.data.description || ''
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
          verified: Boolean(d.isVerified && d.isPublished)
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
          imageUri: doc.data.imageUri || ''
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
          coordinatorContact: doc.data.coordinatorContact || ''
        }));
    }

    // 7. News Items
    if (Array.isArray(newsDocs)) {
      this.state.news = newsDocs
        .sort((a, b) => (b.data.publishedAtMillis || 0) - (a.data.publishedAtMillis || 0))
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
          imageUri: doc.data.imageUri || ''
        }));
    }

    // 8. Announcements
    if (Array.isArray(announcementDocs)) {
      this.state.announcements = announcementDocs
        .sort((a, b) => (b.data.issuedAtMillis || 0) - (a.data.issuedAtMillis || 0))
        .map((doc, idx) => ({
          id: stableNumericId(doc.remoteId, idx + 1),
          remoteId: doc.remoteId,
          title: doc.data.title || '',
          message: doc.data.message || '',
          priorityLabel: doc.data.priorityLabel || 'Official Notice',
          issuedDateText: doc.data.issuedDateText || '',
          issuedAtMillis: doc.data.issuedAtMillis || Date.now(),
          imageUrl: doc.data.imageUri || '',
          imageUri: doc.data.imageUri || ''
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
          coverMediaUri: doc.data.coverMediaUri || ''
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
        .sort((a, b) => (b.data.addedAtMillis || 0) - (a.data.addedAtMillis || 0))
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
            mediaType: doc.data.mediaType || 'IMAGE'
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
    this.logActivity('UPDATE_COURSE', 'Higher Secondary Courses', `Updated course slot #${id}`);
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

  addAcademicProgram(item) {
    const id = Date.now();
    const remoteId = item.remoteId || deterministicRemoteId('academic_info', id);
    const category = item.category || item.level || 'General';
    const next = { id, remoteId, category, level: category, ...item };
    this.state.academics = [next, ...this.state.academics];
    this.logActivity('ADD_ACADEMIC', 'Academics', `Added academic program: ${item.title}`);
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
    const existing = this.state.academics.find((x) => x.id === id);
    this.state.academics = this.state.academics.filter((x) => x.id !== id);
    this.logActivity('DELETE_ACADEMIC', 'Academics', `Removed academic program #${id}`);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('academic_info', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('academic_info', remoteId));
    }
  }

  addFacility(item) {
    const id = Date.now();
    const remoteId = item.remoteId || deterministicRemoteId('facilities', id);
    const imageUri = item.imageUrl || item.imageUri || null;
    const next = { id, remoteId, imageUrl: imageUri || '', imageUri: imageUri || '', ...item };
    this.state.facilities = [next, ...this.state.facilities];
    this.logActivity('ADD_FACILITY', 'Facilities', `Added facility: ${item.name}`);
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
    const existing = this.state.facilities.find((x) => x.id === id);
    this.state.facilities = this.state.facilities.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('facilities', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('facilities', remoteId));
    }
  }

  addTransportRoute(item) {
    const id = Date.now();
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
    this.logActivity('ADD_TRANSPORT', 'Transport', `Added route: ${item.routeNumber}`);
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
    const existing = this.state.transportRoutes.find((x) => x.id === id);
    this.state.transportRoutes = this.state.transportRoutes.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('transport_routes', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('transport_routes', remoteId));
    }
  }

  addNews(item) {
    const id = Date.now();
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
    this.logActivity('PUBLISH_NEWS', 'News', `Published news: ${item.title}`);
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
    const existing = this.state.news.find((x) => x.id === id);
    this.state.news = this.state.news.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('news_items', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('news_items', remoteId));
    }
  }

  addAnnouncement(item) {
    const id = Date.now();
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
    this.logActivity('PUBLISH_ANNOUNCEMENT', 'Announcements', `Published announcement: ${item.title}`);
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
    const existing = this.state.announcements.find((x) => x.id === id);
    this.state.announcements = this.state.announcements.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('announcements', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('announcements', remoteId));
    }
  }

  addHeroPhoto(item) {
    const id = Date.now();
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
    this.logActivity('ADD_HERO_PHOTO', 'Hero Banner', `Added campus hero banner image`);
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
    const existing = this.state.heroPhotos.find((x) => x.id === id);
    this.state.heroPhotos = this.state.heroPhotos.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('campus_hero_photos', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('campus_hero_photos', remoteId));
    }
  }

  addGalleryMedia(item) {
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
    this.logActivity('ADD_GALLERY_MEDIA', 'Gallery', `Added gallery item: ${item.caption || albumName}`);
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
    const existing = this.state.galleryMedia.find((x) => x.id === id);
    this.state.galleryMedia = this.state.galleryMedia.filter((x) => x.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('gallery_media', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('gallery_media', remoteId));
    }
  }

  updateSocialLink(id, patch) {
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
    this.logActivity('UPDATE_SOCIAL', 'Social Media', `Updated channel #${id}`);
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
    this.logActivity('SAVE_SCHOOL_POST', 'School Posts', `${existing ? 'Updated' : 'Created'} post: ${title}`);
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
    this.logActivity('SAVE_HIGHLIGHT', 'Highlights', `Saved 24h highlight: ${record.title || parsed.videoId}`);
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
    const existing = (this.state.schoolHighlights || []).find((h) => h.id === id);
    this.state.schoolHighlights = (this.state.schoolHighlights || []).filter((h) => h.id !== id);
    this._save();
    if (existing) {
      const remoteId = existing.remoteId || deterministicRemoteId('school_highlights', id);
      this._enqueueCloudOp(() => this._deleteCloudDoc('school_highlights', remoteId));
    }
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
