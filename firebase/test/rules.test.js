// Firestore güvenlik kuralı testleri — Firebase Emulator üzerinde çalışır:
//   npm run test:rules
import { readFileSync } from 'node:fs';
import { afterAll, beforeAll, beforeEach, describe, test } from 'vitest';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  arrayUnion,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  collection,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
} from 'firebase/firestore';

let env;
const day = 24 * 60 * 60 * 1000;
const future = () => Timestamp.fromMillis(Date.now() + 7 * day);
const past = () => Timestamp.fromMillis(Date.now() - day);

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-ourshelf',
    firestore: { rules: readFileSync('firestore.rules', 'utf8') },
  });
});
afterAll(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => (uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore());

/** Kuralları atlayarak başlangıç verisi yazar. */
async function seed(fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

/** alice (plus) bir raf kurmuş; bob üye değil. */
async function seedShelf({ members = ['alice'] } = {}) {
  await seed(async (f) => {
    await setDoc(doc(f, 'users/alice'), { displayName: 'Alice', plus: true });
    await setDoc(doc(f, 'shelves/s1'), {
      name: 'Film gecesi', emoji: '🍿', ownerUid: 'alice',
      memberUids: members, memberNames: Object.fromEntries(members.map((m) => [m, m])),
    });
  });
}

describe('users', () => {
  test('kullanıcı kendi profilini oluşturur ama plus yazamaz', async () => {
    await assertSucceeds(setDoc(doc(db('bob'), 'users/bob'), { displayName: 'Bob', createdAt: serverTimestamp() }));
    await assertFails(setDoc(doc(db('eve'), 'users/eve'), { displayName: 'Eve', plus: true }));
  });

  test('plus sonradan da yazılamaz; başkasının profili okunamaz', async () => {
    await seed((f) => setDoc(doc(f, 'users/bob'), { displayName: 'Bob' }));
    await assertFails(updateDoc(doc(db('bob'), 'users/bob'), { plus: true }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'users/bob'), { displayName: 'Bobby' }));
    await assertFails(getDoc(doc(db('eve'), 'users/bob')));
  });
});

describe('raf kurmak (Ourshelf+)', () => {
  const newShelf = (uid) => ({
    name: 'Kitap kulübü', emoji: '📚', ownerUid: uid, memberUids: [uid], memberNames: { [uid]: uid },
  });

  test('abone raf kurabilir', async () => {
    await seed((f) => setDoc(doc(f, 'users/alice'), { displayName: 'Alice', plus: true }));
    await assertSucceeds(setDoc(doc(db('alice'), 'shelves/new'), newShelf('alice')));
  });

  test('abone olmayan raf kuramaz (profili olsa da olmasa da)', async () => {
    await seed((f) => setDoc(doc(f, 'users/bob'), { displayName: 'Bob' }));
    await assertFails(setDoc(doc(db('bob'), 'shelves/new'), newShelf('bob')));
    await assertFails(setDoc(doc(db('carol'), 'shelves/new'), newShelf('carol')));
  });

  test('kurucu başkasını üye olarak ekleyerek başlayamaz', async () => {
    await seed((f) => setDoc(doc(f, 'users/alice'), { displayName: 'Alice', plus: true }));
    await assertFails(setDoc(doc(db('alice'), 'shelves/new'), {
      ...newShelf('alice'), memberUids: ['alice', 'bob'],
    }));
  });
});

describe('katılmak (davetle, ücretsiz)', () => {
  test('geçerli davetle katılınır', async () => {
    await seedShelf();
    await seed((f) => setDoc(doc(f, 'invites/tok1'), { shelfId: 's1', createdBy: 'alice', expiresAt: future() }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'shelves/s1'), {
      memberUids: arrayUnion('bob'), 'memberNames.bob': 'Bob', joinToken: 'tok1',
    }));
    // Katıldıktan sonra raf okunabilir.
    await assertSucceeds(getDoc(doc(db('bob'), 'shelves/s1')));
  });

  test('davetsiz, süresi dolmuş ya da başka rafın davetiyle katılınamaz', async () => {
    await seedShelf();
    await seed(async (f) => {
      await setDoc(doc(f, 'invites/old'), { shelfId: 's1', createdBy: 'alice', expiresAt: past() });
      await setDoc(doc(f, 'invites/other'), { shelfId: 's2', createdBy: 'alice', expiresAt: future() });
    });
    const join = (token) => updateDoc(doc(db('bob'), 'shelves/s1'), {
      memberUids: arrayUnion('bob'), 'memberNames.bob': 'Bob', ...(token ? { joinToken: token } : {}),
    });
    await assertFails(join(null));
    await assertFails(join('nope'));
    await assertFails(join('old'));
    await assertFails(join('other'));
  });

  test('katılan kişi başkalarını ekleyemez', async () => {
    await seedShelf();
    await seed((f) => setDoc(doc(f, 'invites/tok1'), { shelfId: 's1', createdBy: 'alice', expiresAt: future() }));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1'), {
      memberUids: ['alice', 'bob', 'eve'], 'memberNames.bob': 'Bob', joinToken: 'tok1',
    }));
  });

  test('üye sayısı 10 ile sınırlı', async () => {
    const ten = ['alice', ...Array.from({ length: 9 }, (_, i) => `m${i}`)];
    await seedShelf({ members: ten });
    await seed((f) => setDoc(doc(f, 'invites/tok1'), { shelfId: 's1', createdBy: 'alice', expiresAt: future() }));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1'), {
      memberUids: arrayUnion('bob'), 'memberNames.bob': 'Bob', joinToken: 'tok1',
    }));
  });
});

describe('davetler', () => {
  test('yalnızca üye davet oluşturur; davet listelenemez', async () => {
    await seedShelf();
    await assertSucceeds(setDoc(doc(db('alice'), 'invites/t'), { shelfId: 's1', createdBy: 'alice', expiresAt: future() }));
    await assertFails(setDoc(doc(db('bob'), 'invites/t2'), { shelfId: 's1', createdBy: 'bob', expiresAt: future() }));
    await assertFails(getDocs(collection(db('bob'), 'invites')));
    await assertSucceeds(getDoc(doc(db('bob'), 'invites/t')));
  });

  test('davet en fazla 31 gün geçerli olabilir', async () => {
    await seedShelf();
    await assertFails(setDoc(doc(db('alice'), 'invites/t'), {
      shelfId: 's1', createdBy: 'alice', expiresAt: Timestamp.fromMillis(Date.now() + 60 * day),
    }));
  });
});

describe('raf içeriği', () => {
  const item = (uid) => ({ externalId: 'tmdb:movie:550', title: 'Fight Club', addedBy: uid, votes: {}, completedBy: {} });

  test('üye olmayan okuyamaz ve yazamaz', async () => {
    await seedShelf();
    await assertFails(getDoc(doc(db('eve'), 'shelves/s1')));
    await assertFails(setDoc(doc(db('eve'), 'shelves/s1/items/i1'), item('eve')));
    await assertFails(getDocs(collection(db('eve'), 'shelves/s1/items')));
  });

  test('üye eser ekler; ekleyen başkası gibi görünemez', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await assertSucceeds(setDoc(doc(db('bob'), 'shelves/s1/items/i1'), item('bob')));
    await assertFails(setDoc(doc(db('bob'), 'shelves/s1/items/i2'), item('alice')));
  });

  test('oy ve "bitirdim" yalnızca kendi adına', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await seed((f) => setDoc(doc(f, 'shelves/s1/items/i1'), item('alice')));
    await assertSucceeds(updateDoc(doc(db('bob'), 'shelves/s1/items/i1'), { 'votes.bob': true }));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1/items/i1'), { 'votes.alice': true }));
    await assertSucceeds(updateDoc(doc(db('bob'), 'shelves/s1/items/i1'), { 'completedBy.bob': serverTimestamp() }));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1/items/i1'), { addedBy: 'bob' }));
  });

  test('aktivite yalnızca kendi adına yazılır ve değiştirilemez', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await assertSucceeds(setDoc(doc(db('bob'), 'shelves/s1/activity/a1'), { actorUid: 'bob', type: 'added' }));
    await assertFails(setDoc(doc(db('bob'), 'shelves/s1/activity/a2'), { actorUid: 'alice', type: 'added' }));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1/activity/a1'), { type: 'x' }));
  });
});

describe('üyelik değişiklikleri', () => {
  test('üye ayrılabilir; sahip ayrılamaz ama rafı silebilir', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await assertSucceeds(updateDoc(doc(db('bob'), 'shelves/s1'), { memberUids: ['alice'], memberNames: { alice: 'alice' } }));
    await assertFails(updateDoc(doc(db('alice'), 'shelves/s1'), { memberUids: [], memberNames: {} }));
    await assertSucceeds(deleteDoc(doc(db('alice'), 'shelves/s1')));
  });

  test('üye olmayan rafı silemez; üye sahiplik değiştiremez', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await assertFails(deleteDoc(doc(db('bob'), 'shelves/s1')));
    await assertFails(updateDoc(doc(db('bob'), 'shelves/s1'), { ownerUid: 'bob' }));
  });

  test('sahip üye çıkarabilir ama yeni üye ekleyemez', async () => {
    await seedShelf({ members: ['alice', 'bob'] });
    await assertSucceeds(updateDoc(doc(db('alice'), 'shelves/s1'), { memberUids: ['alice'], memberNames: { alice: 'alice' } }));
    await assertFails(updateDoc(doc(db('alice'), 'shelves/s1'), { memberUids: ['alice', 'zed'], memberNames: { alice: 'alice', zed: 'z' } }));
  });

  test('oturum açmamış kullanıcı hiçbir şeye erişemez', async () => {
    await seedShelf();
    await assertFails(getDoc(doc(db(null), 'shelves/s1')));
    await assertFails(getDoc(doc(db(null), 'invites/x')));
  });
});
