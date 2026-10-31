const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');

let testEnv;
const PROJECT_ID = 'demo-katalogyap-test';
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085'
).split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync('./firestore.rules', 'utf8'),
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

function validUserPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    phone: '+90 5321234567',
    email: 'ornek@katalogyap.com',
    displayName: 'Ornek Magaza',
    provider: 'phone_password',
    passwordHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    passwordResetRequestedAt: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test('unauthenticated user cannot read or write /users/{userId}', async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  const ref = unauthedDb.collection('users').doc('user_1');
  await assertFails(ref.get());
  await assertFails(ref.set(validUserPayload('user_1')));
});

test('authenticated user can create, read, update, and delete their own UserAccount', async () => {
  const aliceDb = testEnv.authenticatedContext('alice_1').firestore();
  const ref = aliceDb.collection('users').doc('alice_1');

  await assertSucceeds(ref.set(validUserPayload('alice_1')));
  const snap = await assertSucceeds(ref.get());
  assert.equal(snap.exists, true);

  await assertSucceeds(
    ref.update({
      passwordHash: 'new_hash_value_123456',
      passwordResetRequestedAt: 1710000000000,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(ref.delete());
});

test('authenticated user cannot read or modify another user document', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('users').doc('alice_1').set(validUserPayload('alice_1'));
  });

  const bobDb = testEnv.authenticatedContext('bob_2').firestore();
  const aliceRef = bobDb.collection('users').doc('alice_1');

  await assertFails(aliceRef.get());
  await assertFails(
    aliceRef.update({
      displayName: 'Hacked Name',
      updatedAt: new Date(),
    })
  );
  await assertFails(aliceRef.delete());
});

test('authenticated user cannot create document with invalid provider or mismatched userId', async () => {
  const aliceDb = testEnv.authenticatedContext('alice_1').firestore();
  const ref = aliceDb.collection('users').doc('alice_1');

  await assertFails(
    ref.set(validUserPayload('alice_1', { provider: 'anonymous' }))
  );

  await assertFails(
    ref.set(validUserPayload('different_user'))
  );
});

function validCatalogPayload(catalogId, ownerId, overrides = {}) {
  const now = new Date();
  return {
    catalogId,
    ownerId,
    title: 'Yaz Koleksiyonu',
    description: 'Yeni sezon ürünler',
    slug: 'yaz-koleksiyonu',
    shortCode: 'yaz123',
    coverImageUrl: 'https://constantinopolishotel.store/cover.jpg',
    tags: ['Yeni', 'Yaz'],
    isActive: true,
    productCount: 1,
    viewCount: 0,
    orderCount: 0,
    businessName: 'Anadolu Toptan',
    businessPhone: '+90 5321234567',
    currencySymbol: '₺',
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validProductPayload(productId, catalogId, ownerId, overrides = {}) {
  const now = new Date();
  return {
    productId,
    catalogId,
    ownerId,
    name: 'Keten Gömlek',
    description: 'Yüzde yüz keten',
    price: 499.0,
    originalPrice: 599.0,
    currency: '₺',
    imageUrl: 'data:image/jpeg;base64,/9j/4AAQSkZJRg==',
    category: 'Giyim',
    subCategory: 'Gömlek',
    sku: 'KTN-01',
    stockStatus: 'IN_STOCK',
    stockCount: 25,
    inStock: true,
    sortOrder: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test('anyone can read public catalogs and products, only owner can create/update/delete', async () => {
  const ownerDb = testEnv.authenticatedContext('owner_1').firestore();
  const catRef = ownerDb.collection('catalogs').doc('cat_1');
  const prodRef = ownerDb.collection('products').doc('prod_1');

  await assertSucceeds(catRef.set(validCatalogPayload('cat_1', 'owner_1')));
  await assertSucceeds(prodRef.set(validProductPayload('prod_1', 'cat_1', 'owner_1')));

  // Unauthenticated customer can read shared catalog and products
  const publicDb = testEnv.unauthenticatedContext().firestore();
  const pubCatSnap = await assertSucceeds(publicDb.collection('catalogs').doc('cat_1').get());
  assert.equal(pubCatSnap.exists, true);
  const pubProdSnap = await assertSucceeds(publicDb.collection('products').doc('prod_1').get());
  assert.equal(pubProdSnap.exists, true);

  // Unauthenticated or different user cannot modify or delete
  await assertFails(publicDb.collection('catalogs').doc('cat_1').delete());
  const attackerDb = testEnv.authenticatedContext('attacker_2').firestore();
  await assertFails(
    attackerDb.collection('catalogs').doc('cat_1').update({
      title: 'Hacked Catalog',
      updatedAt: new Date(),
    })
  );
  await assertFails(attackerDb.collection('products').doc('prod_1').delete());
});


