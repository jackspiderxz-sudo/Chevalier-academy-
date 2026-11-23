const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const SCHOOL_ID = 'chevalier_academy_dindigul';
const OTHER_SCHOOL_ID = 'other_school_tenant';
const RULES_PATH = path.resolve(__dirname, 'firestore.rules');

const EMULATOR_HOST_ENV = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085';
const [EMULATOR_HOST, EMULATOR_PORT_STR] = EMULATOR_HOST_ENV.split(':');
const EMULATOR_PORT = Number(EMULATOR_PORT_STR || '8085');

let testEnv;

function publishedCmsBase(remoteId, overrides = {}) {
  return {
    isVerified: true,
    isPublished: true,
    isApproved: true,
    approvalStatus: 'APPROVED',
    displayOrder: 1,
    remoteId,
    tenantId: SCHOOL_ID,
    syncState: 'SYNCED',
    serverUpdatedAtMillis: 1700000000000,
    deletedAtMillis: null,
    ...overrides,
  };
}

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    // Offline-only demo-* prefix recognized by the local Firebase Emulator Suite.
    projectId: 'demo-chevalier-rules-test',
    firestore: {
      rules: fs.readFileSync(RULES_PATH, 'utf8'),
      host: EMULATOR_HOST,
      port: EMULATOR_PORT,
    },
  });
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

// =============================================================================
// 1. PUBLIC PUBLISHED-CONTENT READS
// =============================================================================

test('1.1 Public can get active school tenant by exact schoolId, but cannot list all schools or read inactive tenant', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`schools/${SCHOOL_ID}`).set({
      schoolId: SCHOOL_ID,
      schoolCode: 'CAMHSS-DGL',
      officialName: 'Chevalier Academy Matriculation Higher Secondary School',
      shortName: 'Chevalier Academy',
      tenancySchemaLevel: 1,
      isActive: true,
      updatedAtMillis: 1700000000000,
    });
    await db.doc(`schools/${OTHER_SCHOOL_ID}`).set({
      schoolId: OTHER_SCHOOL_ID,
      schoolCode: 'OTHER-01',
      officialName: 'Inactive Tenant',
      shortName: 'Inactive',
      tenancySchemaLevel: 1,
      isActive: false,
      updatedAtMillis: 1700000000000,
    });
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(anonDb.doc(`schools/${SCHOOL_ID}`).get());
  await assertFails(anonDb.doc(`schools/${OTHER_SCHOOL_ID}`).get());
  await assertFails(anonDb.collection('schools').get());
});

test('1.2 Public can get and tenant-list school_profile', async () => {
  const remoteId = 'profile-singleton-001';
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`schools/${SCHOOL_ID}/school_profile/${remoteId}`).set({
      schoolName: 'Chevalier Academy',
      shortName: 'Chevalier',
      motto: 'Knowledge, Discipline, Service',
      addressLine: 'Dindigul, Tamil Nadu',
      emblemMark: 'CA',
      poweredBy: 'Chevalier Academy',
      updatedAtMillis: 1700000000000,
      remoteId,
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      serverUpdatedAtMillis: 1700000000000,
    });
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(anonDb.doc(`schools/${SCHOOL_ID}/school_profile/${remoteId}`).get());
  await assertSucceeds(
    anonDb
      .collection(`schools/${SCHOOL_ID}/school_profile`)
      .where('tenantId', '==', SCHOOL_ID)
      .get()
  );
  // Unfiltered list without tenantId constraint must fail.
  await assertFails(anonDb.collection(`schools/${SCHOOL_ID}/school_profile`).get());
});

test('1.3 Public can get and properly filtered-list all 10 published CMS collections', async () => {
  const cmsSeed = [
    [
      'campus_hero_photos',
      'hero-001',
      {
        imageUri: 'https://example.org/hero.jpg',
        caption: 'Main Campus',
        addedAtMillis: 1700000000000,
      },
    ],
    [
      'academic_info',
      'acad-001',
      {
        title: 'Higher Secondary Stream',
        category: 'Academics',
        description: 'Tamil Nadu State Board curriculum',
        updatedAtMillis: 1700000000000,
      },
    ],
    [
      'higher_secondary_courses',
      'course-001',
      {
        streamTitle: 'Bio-Maths',
        groupCode: 'GROUP-I',
        subjectsSummary: 'Physics, Chemistry, Biology, Mathematics',
        mediumOfInstruction: 'English',
      },
    ],
    [
      'facilities',
      'fac-001',
      {
        name: 'Computer Lab',
        category: 'Labs',
        description: 'Modern lab',
      },
    ],
    [
      'transport_routes',
      'route-001',
      {
        routeNumber: 'R1',
        routeName: 'Dindigul Town Loop',
        stopsSummary: 'Bus Stand, Market, Campus',
      },
    ],
    [
      'news_items',
      'news-001',
      {
        title: 'Annual Science Fair',
        summary: 'Highlights from science expo',
        fullBody: 'Students showcased robotics and eco models.',
        publishedDateText: '2026-02-15',
        publishedAtMillis: 1700000000000,
        isFeatured: true,
      },
    ],
    [
      'announcements',
      'ann-001',
      {
        title: 'Reopening Schedule',
        message: 'School reopens on Monday.',
        issuedDateText: '2026-06-01',
        issuedAtMillis: 1700000000000,
      },
    ],
    [
      'gallery_albums',
      'album-001',
      {
        title: 'Sports Day 2026',
        category: 'Sports',
        createdAtMillis: 1700000000000,
      },
    ],
    [
      'gallery_media',
      'media-001',
      {
        albumRemoteId: 'album-001',
        mediaUri: 'https://example.org/sports1.jpg',
        mediaType: 'IMAGE',
        addedAtMillis: 1700000000000,
      },
    ],
    [
      'social_media_links',
      'social-001',
      {
        platformName: 'YouTube',
        handleOrLabel: '@chevalieracademy',
        url: 'https://youtube.com/@chevalieracademy',
      },
    ],
  ];

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const [col, id, fields] of cmsSeed) {
      await db.doc(`schools/${SCHOOL_ID}/${col}/${id}`).set(publishedCmsBase(id, fields));
    }
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  for (const [col, id] of cmsSeed) {
    await assertSucceeds(anonDb.doc(`schools/${SCHOOL_ID}/${col}/${id}`).get());
    const snap = await assertSucceeds(
      anonDb
        .collection(`schools/${SCHOOL_ID}/${col}`)
        .where('tenantId', '==', SCHOOL_ID)
        .where('isVerified', '==', true)
        .where('isPublished', '==', true)
        .where('isApproved', '==', true)
        .where('approvalStatus', '==', 'APPROVED')
        .where('deletedAtMillis', '==', null)
        .get()
    );
    assert.equal(snap.size, 1, `Expected 1 published doc in ${col}`);
  }
});

test('1.4 Public can get and filtered-list PUBLISHED school_posts and school_highlights', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`schools/${SCHOOL_ID}/school_posts/post-pub-001`).set({
      postType: 'PHOTO',
      category: 'Events',
      title: 'Independence Day Celebration',
      description: 'Flag hoisting ceremony at campus.',
      photoUris: ['https://example.org/p1.jpg', 'https://example.org/p2.jpg'],
      status: 'PUBLISHED',
      isFeatured: true,
      createdAtMillis: 1700000000000,
      publishedAtMillis: 1700000001000,
      updatedAtMillis: 1700000001000,
      remoteId: 'post-pub-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    });
    await db.doc(`schools/${SCHOOL_ID}/school_highlights/hl-pub-001`).set({
      title: 'Morning Assembly Short',
      youtubeShortsUrl: 'https://www.youtube.com/shorts/dQw4w9WgXcQ',
      status: 'PUBLISHED',
      createdAtMillis: 1700000000000,
      publishedAtMillis: 1700000000000,
      expiresAtMillis: 1700086400000,
      remoteId: 'hl-pub-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    });
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(anonDb.doc(`schools/${SCHOOL_ID}/school_posts/post-pub-001`).get());
  await assertSucceeds(
    anonDb
      .collection(`schools/${SCHOOL_ID}/school_posts`)
      .where('tenantId', '==', SCHOOL_ID)
      .where('status', '==', 'PUBLISHED')
      .where('deletedAtMillis', '==', null)
      .get()
  );

  await assertSucceeds(anonDb.doc(`schools/${SCHOOL_ID}/school_highlights/hl-pub-001`).get());
  await assertSucceeds(
    anonDb
      .collection(`schools/${SCHOOL_ID}/school_highlights`)
      .where('tenantId', '==', SCHOOL_ID)
      .where('status', '==', 'PUBLISHED')
      .where('deletedAtMillis', '==', null)
      .get()
  );
});

// =============================================================================
// 2. PUBLIC DRAFT / UNPUBLISHED / ARCHIVED / SOFT-DELETED / UNFILTERED DENIAL
// =============================================================================

test('2.1 Public is denied draft, pending, rejected, unverified, unpublished, and soft-deleted CMS items', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const base = {
      title: 'Internal News',
      summary: 'Summary',
      fullBody: 'Body',
      publishedDateText: '2026-03-01',
      publishedAtMillis: 1700000000000,
      isFeatured: false,
    };
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-draft`)
      .set(publishedCmsBase('news-draft', { ...base, approvalStatus: 'DRAFT', isApproved: false, isPublished: false }));
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-pending`)
      .set(publishedCmsBase('news-pending', { ...base, approvalStatus: 'PENDING', isApproved: false }));
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-rejected`)
      .set(publishedCmsBase('news-rejected', { ...base, approvalStatus: 'REJECTED', isApproved: false }));
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-unverified`)
      .set(publishedCmsBase('news-unverified', { ...base, isVerified: false }));
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-unpublished`)
      .set(publishedCmsBase('news-unpublished', { ...base, isPublished: false }));
    await db
      .doc(`schools/${SCHOOL_ID}/news_items/news-deleted`)
      .set(publishedCmsBase('news-deleted', { ...base, deletedAtMillis: 1700000099999 }));
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  for (const id of [
    'news-draft',
    'news-pending',
    'news-rejected',
    'news-unverified',
    'news-unpublished',
    'news-deleted',
  ]) {
    await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/news_items/${id}`).get());
  }

  // Unfiltered list queries must also fail.
  await assertFails(anonDb.collection(`schools/${SCHOOL_ID}/news_items`).get());
  await assertFails(
    anonDb
      .collection(`schools/${SCHOOL_ID}/news_items`)
      .where('tenantId', '==', SCHOOL_ID)
      .where('isPublished', '==', true)
      .get()
  );
});

test('2.2 Public is denied UNPUBLISHED, ARCHIVED, and soft-deleted school_posts and school_highlights', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const postBase = {
      postType: 'YOUTUBE_DIRECT',
      category: 'General',
      title: 'Draft Video',
      description: 'Not ready',
      youtubeUrl: 'https://youtu.be/dQw4w9WgXcQ',
      photoUris: [],
      isFeatured: false,
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      tenantId: SCHOOL_ID,
      syncState: 'LOCAL_ONLY',
      deletedAtMillis: null,
    };
    await db.doc(`schools/${SCHOOL_ID}/school_posts/post-unpub`).set({
      ...postBase,
      remoteId: 'post-unpub',
      status: 'UNPUBLISHED',
    });
    await db.doc(`schools/${SCHOOL_ID}/school_posts/post-arch`).set({
      ...postBase,
      remoteId: 'post-arch',
      status: 'ARCHIVED',
    });
    await db.doc(`schools/${SCHOOL_ID}/school_posts/post-del`).set({
      ...postBase,
      remoteId: 'post-del',
      status: 'PUBLISHED',
      deletedAtMillis: 1700000050000,
    });

    const hlBase = {
      title: 'Draft Short',
      youtubeShortsUrl: 'https://www.youtube.com/shorts/dQw4w9WgXcQ',
      createdAtMillis: 1700000000000,
      publishedAtMillis: 1700000000000,
      expiresAtMillis: 1700086400000,
      tenantId: SCHOOL_ID,
      syncState: 'LOCAL_ONLY',
      deletedAtMillis: null,
    };
    await db.doc(`schools/${SCHOOL_ID}/school_highlights/hl-unpub`).set({
      ...hlBase,
      remoteId: 'hl-unpub',
      status: 'UNPUBLISHED',
    });
    await db.doc(`schools/${SCHOOL_ID}/school_highlights/hl-arch`).set({
      ...hlBase,
      remoteId: 'hl-arch',
      status: 'ARCHIVED',
    });
    await db.doc(`schools/${SCHOOL_ID}/school_highlights/hl-del`).set({
      ...hlBase,
      remoteId: 'hl-del',
      status: 'PUBLISHED',
      deletedAtMillis: 1700000050000,
    });
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_posts/post-unpub`).get());
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_posts/post-arch`).get());
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_posts/post-del`).get());
  await assertFails(anonDb.collection(`schools/${SCHOOL_ID}/school_posts`).get());

  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_highlights/hl-unpub`).get());
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_highlights/hl-arch`).get());
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/school_highlights/hl-del`).get());
  await assertFails(anonDb.collection(`schools/${SCHOOL_ID}/school_highlights`).get());
});

// =============================================================================
// 3. UNAUTHENTICATED WRITE DENIAL
// =============================================================================

test('3.1 Unauthenticated users cannot create, update, or delete any record', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`schools/${SCHOOL_ID}/announcements/ann-existing`).set(
      publishedCmsBase('ann-existing', {
        title: 'Existing',
        message: 'Official notice',
        issuedDateText: '2026-06-01',
        issuedAtMillis: 1700000000000,
      })
    );
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();

  // Create attempts
  await assertFails(
    anonDb.doc(`schools/${SCHOOL_ID}/announcements/ann-new`).set(
      publishedCmsBase('ann-new', {
        title: 'Fake Notice',
        message: 'Unauthorized',
        issuedDateText: '2026-06-02',
        issuedAtMillis: 1700000000000,
      })
    )
  );
  await assertFails(
    anonDb.doc(`schools/${SCHOOL_ID}/school_posts/post-new`).set({
      postType: 'PHOTO',
      category: 'General',
      title: 'Unauthorized Post',
      description: 'Nope',
      photoUris: [],
      status: 'PUBLISHED',
      isFeatured: false,
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      remoteId: 'post-new',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    })
  );
  await assertFails(
    anonDb.doc(`schools/${SCHOOL_ID}/school_profile/profile-001`).set({
      schoolName: 'Hijacked',
      shortName: 'Hijacked',
      motto: 'Motto',
      addressLine: 'Addr',
      emblemMark: 'X',
      poweredBy: 'X',
      updatedAtMillis: 1700000000000,
      remoteId: 'profile-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
    })
  );

  // Update and delete attempts
  await assertFails(
    anonDb.doc(`schools/${SCHOOL_ID}/announcements/ann-existing`).update({ title: 'Tampered' })
  );
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/announcements/ann-existing`).delete());
});

// =============================================================================
// 4. CROSS-TENANT ACCESS DENIAL & PATH ISOLATION
// =============================================================================

test('4.1 Cross-tenant tenantId mismatch, remoteId mismatch, and invalid schoolId paths are denied', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    // Poisoned document placed under SCHOOL_ID path with OTHER_SCHOOL_ID tenantId
    await db.doc(`schools/${SCHOOL_ID}/facilities/fac-cross-tenant`).set(
      publishedCmsBase('fac-cross-tenant', {
        name: 'Other School Lab',
        category: 'Labs',
        description: 'Wrong tenant',
        tenantId: OTHER_SCHOOL_ID,
      })
    );
    // Document whose remoteId does not match document ID
    await db.doc(`schools/${SCHOOL_ID}/facilities/fac-id-mismatch`).set(
      publishedCmsBase('different-remote-id', {
        name: 'Mismatched ID',
        category: 'Labs',
        description: 'Mismatched',
      })
    );
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/facilities/fac-cross-tenant`).get());
  await assertFails(anonDb.doc(`schools/${SCHOOL_ID}/facilities/fac-id-mismatch`).get());

  // Cross-tenant query attempting to read OTHER_SCHOOL_ID records from SCHOOL_ID path
  await assertFails(
    anonDb
      .collection(`schools/${SCHOOL_ID}/facilities`)
      .where('tenantId', '==', OTHER_SCHOOL_ID)
      .where('isVerified', '==', true)
      .where('isPublished', '==', true)
      .where('isApproved', '==', true)
      .where('approvalStatus', '==', 'APPROVED')
      .where('deletedAtMillis', '==', null)
      .get()
  );

  // Invalid schoolId path characters (e.g., dot/dollar) rejected by isValidId
  await assertFails(anonDb.doc(`schools/bad.school$id/school_profile/p1`).get());
});

// =============================================================================
// 5. GOVERNANCE, IMMUTABLE LEDGERS & PRIVATE STUDENT/PARENT ISOLATION
// =============================================================================

test('5.1 Internal governance collections, immutable audit ledgers, and private student zones deny all reads and writes', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`schools/${SCHOOL_ID}/management_accounts/uid-master`).set({
      uid: 'uid-master',
      username: 'masteradmin',
      displayName: 'Principal',
      role: 'MASTER_ADMIN',
      isActive: true,
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      tenantId: SCHOOL_ID,
    });
    await db
      .doc(`schools/${SCHOOL_ID}/management_accounts/uid-master/staff_permissions/NEWS`)
      .set({
        accountUid: 'uid-master',
        sectionKey: 'NEWS',
        canView: true,
        canCreate: true,
        canEdit: true,
        canDelete: true,
        canPublish: true,
        tenantId: SCHOOL_ID,
      });
    await db.doc(`schools/${SCHOOL_ID}/content_approvals/appr-001`).set({
      sectionKey: 'NEWS',
      targetRecordRemoteId: 'news-001',
      recordTitle: 'Pending News',
      proposedPayloadJson: '{}',
      changeSummary: 'Draft edit',
      submittedByUid: 'uid-staff',
      submittedByName: 'Staff A',
      submittedAtMillis: 1700000000000,
      status: 'PENDING_APPROVAL',
      remoteId: 'appr-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    });
    await db.doc(`schools/${SCHOOL_ID}/version_history/ver-001`).set({
      sectionKey: 'NEWS',
      recordRemoteId: 'news-001',
      versionNumber: 1,
      recordTitle: 'Annual Day',
      contentSnapshotJson: '{}',
      changedByUid: 'uid-master',
      changedByName: 'Principal',
      changedAtMillis: 1700000000000,
      changeSummary: 'Initial publish',
      approvalStatus: 'PUBLISHED',
      remoteId: 'ver-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
    });
    await db.doc(`schools/${SCHOOL_ID}/media_studio_items/ms-001`).set({
      mediaKind: 'IMAGE',
      title: 'Campus Aerial',
      categoryOrAlbum: 'Campus',
      originalUri: 'https://example.org/orig.jpg',
      mimeType: 'image/jpeg',
      widthPx: 1920,
      heightPx: 1080,
      fileSizeBytes: 500000,
      optimizedSizeBytes: 180000,
      assignedSections: ['LOGO_BANNER'],
      isFeatured: false,
      displayOrder: 1,
      approvalStatus: 'PUBLISHED',
      createdByName: 'Principal',
      createdAtMillis: 1700000000000,
      remoteId: 'ms-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    });
    await db.doc(`schools/${SCHOOL_ID}/activity_logs/log-001`).set({
      actionType: 'PUBLISH_NEWS',
      actorUid: 'uid-master',
      actorName: 'Principal',
      actorRole: 'MASTER_ADMIN',
      sectionKey: 'NEWS',
      recordRef: 'news-001',
      details: 'Published news item',
      timestampMillis: 1700000000000,
      remoteId: 'log-001',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
    });
    await db.doc(`schools/${SCHOOL_ID}/private_students/stu-001`).set({
      studentId: 'stu-001',
      tenantId: SCHOOL_ID,
    });
    await db.doc(`schools/${SCHOOL_ID}/private_parent_links/link-001`).set({
      linkId: 'link-001',
      parentUid: 'uid-parent-1',
      studentId: 'stu-001',
      tenantId: SCHOOL_ID,
    });
    await db.doc(`schools/${SCHOOL_ID}/private/records/attendance/att-001`).set({
      tenantId: SCHOOL_ID,
    });
  });

  const sensitiveDocs = [
    `schools/${SCHOOL_ID}/management_accounts/uid-master`,
    `schools/${SCHOOL_ID}/management_accounts/uid-master/staff_permissions/NEWS`,
    `schools/${SCHOOL_ID}/content_approvals/appr-001`,
    `schools/${SCHOOL_ID}/version_history/ver-001`,
    `schools/${SCHOOL_ID}/media_studio_items/ms-001`,
    `schools/${SCHOOL_ID}/activity_logs/log-001`,
    `schools/${SCHOOL_ID}/private_students/stu-001`,
    `schools/${SCHOOL_ID}/private_parent_links/link-001`,
    `schools/${SCHOOL_ID}/private/records/attendance/att-001`,
  ];

  const sensitiveCollections = [
    `schools/${SCHOOL_ID}/management_accounts`,
    `schools/${SCHOOL_ID}/management_accounts/uid-master/staff_permissions`,
    `schools/${SCHOOL_ID}/content_approvals`,
    `schools/${SCHOOL_ID}/version_history`,
    `schools/${SCHOOL_ID}/media_studio_items`,
    `schools/${SCHOOL_ID}/activity_logs`,
    `schools/${SCHOOL_ID}/private_students`,
    `schools/${SCHOOL_ID}/private_parent_links`,
    `schools/${SCHOOL_ID}/private/records/attendance`,
  ];

  const anonDb = testEnv.unauthenticatedContext().firestore();
  const signedInDb = testEnv.authenticatedContext('uid-master').firestore();

  for (const docPath of sensitiveDocs) {
    // Public read/update/delete denied
    await assertFails(anonDb.doc(docPath).get());
    await assertFails(anonDb.doc(docPath).update({ tampered: true }));
    await assertFails(anonDb.doc(docPath).delete());

    // Signed-in read/update/delete also denied in Phase 4.2B (no unverified cloud auth)
    await assertFails(signedInDb.doc(docPath).get());
    await assertFails(signedInDb.doc(docPath).update({ tampered: true }));
    await assertFails(signedInDb.doc(docPath).delete());
  }

  for (const colPath of sensitiveCollections) {
    await assertFails(anonDb.collection(colPath).where('tenantId', '==', SCHOOL_ID).get());
    await assertFails(signedInDb.collection(colPath).where('tenantId', '==', SCHOOL_ID).get());
  }

  // Immutable append-only create/update/delete checks
  await assertFails(
    signedInDb.doc(`schools/${SCHOOL_ID}/activity_logs/log-002`).set({
      actionType: 'FORGED',
      actorUid: 'uid-master',
      actorName: 'Principal',
      actorRole: 'MASTER_ADMIN',
      sectionKey: 'NEWS',
      recordRef: 'news-001',
      details: 'Forged log',
      timestampMillis: 1700000000000,
      remoteId: 'log-002',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
    })
  );
  await assertFails(
    signedInDb.doc(`schools/${SCHOOL_ID}/version_history/ver-002`).set({
      sectionKey: 'NEWS',
      recordRemoteId: 'news-001',
      versionNumber: 2,
      recordTitle: 'Forged Version',
      contentSnapshotJson: '{}',
      changedByUid: 'uid-master',
      changedByName: 'Principal',
      changedAtMillis: 1700000000000,
      changeSummary: 'Forged',
      approvalStatus: 'PUBLISHED',
      remoteId: 'ver-002',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
    })
  );
});

// =============================================================================
// 6. PHASE 4.2B ADMIN WRITE & SPOOFED CUSTOM-CLAIM DENIAL
// =============================================================================

test('6.1 Authenticated users and spoofed admin token claims are strictly denied all writes until Phase 4.3 auth/RBAC', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`schools/${SCHOOL_ID}/school_posts/post-existing`).set({
      postType: 'PHOTO',
      category: 'General',
      title: 'Existing Post',
      description: 'Body',
      photoUris: [],
      status: 'PUBLISHED',
      isFeatured: false,
      createdAtMillis: 1700000000000,
      publishedAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      remoteId: 'post-existing',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    });
  });

  // Spoofed custom claims MUST NOT unlock any admin read or write in Phase 4.2B.
  const spoofedAdminDb = testEnv
    .authenticatedContext('uid-spoofed-admin', {
      role: 'MASTER_ADMIN',
      admin: true,
      superOwner: true,
      schoolId: SCHOOL_ID,
      email: 'admin@chevalieracademy.edu',
      email_verified: true,
    })
    .firestore();

  // Valid payload matching every schema constraint must STILL fail because isVerifiedCloudAdmin() is closed.
  await assertFails(
    spoofedAdminDb.doc(`schools/${SCHOOL_ID}/school_posts/post-valid-payload`).set({
      postType: 'PHOTO',
      category: 'Events',
      title: 'Valid Post Payload',
      description: 'Valid body',
      photoUris: ['https://example.org/1.jpg'],
      status: 'PUBLISHED',
      isFeatured: false,
      createdAtMillis: 1700000000000,
      publishedAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      remoteId: 'post-valid-payload',
      tenantId: SCHOOL_ID,
      syncState: 'SYNCED',
      deletedAtMillis: null,
    })
  );

  await assertFails(
    spoofedAdminDb.doc(`schools/${SCHOOL_ID}/school_posts/post-existing`).update({
      title: 'Attempted Admin Edit',
    })
  );
  await assertFails(
    spoofedAdminDb.doc(`schools/${SCHOOL_ID}/school_posts/post-existing`).delete()
  );

  await assertFails(
    spoofedAdminDb.doc(`schools/${SCHOOL_ID}/news_items/news-spoofed`).set(
      publishedCmsBase('news-spoofed', {
        title: 'Spoofed News',
        summary: 'Summary',
        fullBody: 'Body',
        publishedDateText: '2026-06-01',
        publishedAtMillis: 1700000000000,
        isFeatured: false,
      })
    )
  );

  await assertFails(
    spoofedAdminDb.doc(`schools/${SCHOOL_ID}/management_accounts/uid-spoofed-admin`).set({
      uid: 'uid-spoofed-admin',
      username: 'spoof',
      displayName: 'Spoofed Admin',
      role: 'MASTER_ADMIN',
      isActive: true,
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      tenantId: SCHOOL_ID,
    })
  );

  // Unknown top-level collections must also be denied by global catch-all.
  await assertFails(spoofedAdminDb.doc('arbitrary_collection/doc1').get());
  await assertFails(spoofedAdminDb.doc('arbitrary_collection/doc1').set({ foo: 'bar' }));
});
