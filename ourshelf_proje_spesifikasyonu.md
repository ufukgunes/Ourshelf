# PROJE SPESİFİKASYONU: OURSHELF

> **Revizyon 3 (2026-11-18):** Uygulamanın adı **Unwind → Ourshelf** oldu. Odak, "kişisel boş zaman listesi"nden **"paylaşılabilir raflar"** konseptine kaydı. Mimari **Kotlin Multiplatform + Compose Multiplatform** (Android + iOS) olarak değişti. Önceki revizyonların notları için bkz. Bölüm 12.

---

## 1. Kimlik

| | |
|---|---|
| **Ad** | Ourshelf ("our shelf / ourselves" kelime oyunu) |
| **Slogan** | EN: *Your shelf. Our shelf.* · TR: *Senin rafın. Bizim rafımız.* |
| **Paket adı** | `com.ourshelf.app` (ilk yayından sonra değiştirilemez) |
| **Mağaza başlığı** | EN: `Ourshelf: Watch, Read & Play Together` · TR: `Ourshelf: Birlikte İzle, Oku, Oyna` (≤30 karakter kısaltması mağaza kurallarına göre seçilecek) |
| **Kısa açıklama (≤80)** | TR: "İzleyeceğin, okuyacağın, oynayacağın her şey tek rafta. Sevdiklerinle paylaş." · EN: "Everything you'll watch, read & play, on one shelf. Share it with your people." |
| **Platformlar** | Android + iOS (aynı anda ilk yayın) |
| **Diller** | Türkçe + İngilizce |

**Yayın öncesi kontroller:** TÜRKPATENT / EUIPO marka araması, alan adı, Play Store + App Store isim araması. (İlk web taramasında "Ourshelf" adlı uygulama bulunmadı; benzer isimli "YourShelf" kitap uygulaması var.)

---

## 2. Konsept: Üç Katmanlı Raf

Uygulamanın temel nesnesi **raf**tır. Film, dizi, kitap, oyun, albüm ve podcast'ler raflarda durur.

1. **Benim rafım (kişisel):** Cihazda, gizli, çevrimdışı çalışır (Local-First). Hesap gerektirmez.
2. **Bizim rafımız (paylaşılan):** Davet bağlantısıyla eş, arkadaş, aile, kitap kulübü katılır; canlı senkronize olur. **Uygulamanın asıl farkı.**
3. **Raf vitrini (herkese açık bağlantı):** "2026 favorilerim" gibi bir raf web bağlantısıyla paylaşılır; uygulaması olmayan da görür ve kendi rafına kopyalayabilir. **Büyüme motoru.**

**Rekabet notu (Kasım 2026):** Çiftlere yönelik film uygulamaları (WeWatch Together, Shared Watchlist, CineDuo, NxtUp, Swatched…) kalabalık; neredeyse hepsi **sadece film/dizi** ve **kaydırarak eşleşme** sunuyor. Ourshelf'in farkı: **tüm medya türleri**, **estetik/dokunsal deneyim**, **gizlilik** (kişisel veri cihazda) ve çiftlerle sınırlı olmayan **raf** konsepti.

---

## 3. Özellik Planı

| Sürüm | Özellik | Durum |
|---|---|---|
| **İlk yayın** | Kişisel raf, Lounge, arama (Omni-Search), detay | ✅ (Android) |
| | "WOW" UI: ambiyans ışığı, 3B tactile poster, shared element geçişleri, Finisher Stamp | ✅ (Android) |
| | Raflar: oluşturma, düzenleme, sürükle-bırak sıralama | ✅ (Android) |
| | Karar çarkı (Roulette), ISBN barkod okuyucu, Logbook + paylaşım kartı | Faz B |
| | **Paylaşılan raflar:** davet bağlantısı, üyeler, birlikte ekle/çıkar | Faz C |
| | **"Ben de varım 👍" oyları** + aktivite akışı | Faz C |
| | **Ortak karar çarkı** (yalnızca herkesin istediği eserler) | Faz C |
| | **Planla:** "Cuma 21:00 film gecesi" → herkese bildirim | Faz C |
| | **Birlikte bitti:** FIN. mührü herkesin kütüphanesine, herkes kendi puanını verir | Faz C |
| **İlk güncelleme** | Eşleşme modu (herkes kaydırır, ortak sağa kaydırılanlar "Eşleşme!") | Faz E |
| | Raf vitrini + "rafı kopyala" | Faz E |
| | Ortak özet ("Bu ay birlikte 4 film, 1 kitap") | Faz E |
| **Sonra** | Widget'lar, Drive yedeği, akıllı listeler, temalar, alternatif ikonlar | Faz E |

---

## 4. Marka Öğeleri

* **Mühür:** **"FIN."** (film jeneriğindeki "son"; kitap ve oyun için de anlamlı). Paylaşılan rafta bitirilince altında üyelerin baş harfleri.
* **İkon:** Birbirine yaslanmış iki poster.
* **Dil:** "Raf", "Bizim raf", "Birlikte". Ana ekran adı: **Lounge** (gerekirse "Rafım" olarak değiştirilebilir).

---

## 5. Mimari ve Teknoloji Yığını

**Kotlin Multiplatform + Compose Multiplatform.** Kodun ~%85-90'ı ortak (`commonMain`); platforma özgü parçalar `expect/actual` ile.

| Katman | Ortak (commonMain) | Android | iOS |
|---|---|---|---|
| UI | Compose Multiplatform, Material 3 | — | — |
| Navigasyon / ViewModel | JetBrains Navigation Compose + Lifecycle ViewModel (KMP) | | |
| DI | Koin | | |
| Ağ | Ktor Client + kotlinx.serialization | OkHttp engine | Darwin engine |
| Veritabanı | Room KMP (+ FTS4) | | BundledSQLiteDriver |
| Ayarlar | DataStore KMP | | |
| Zaman | kotlinx-datetime | | |
| Görsel | Coil 3 (KMP) | | |
| Renk çıkarma | `expect` | AndroidX Palette | Kendi median-cut / k-means uygulamamız |
| Shader | `expect` | AGSL `RuntimeShader` (API 33+) / Canvas fallback | Skia `RuntimeEffect` |
| Eğim sensörü | `expect` | `TYPE_GAME_ROTATION_VECTOR` | CoreMotion |
| Haptik | `expect` | `Vibrator` / `HapticFeedbackConstants` | `UIImpactFeedbackGenerator` |
| Barkod | `expect` | ML Kit + CameraX | VisionKit / AVFoundation |
| Bulut (Faz C) | GitLive firebase-kotlin-sdk (resmi KMP SDK yok) | Firebase Android SDK | Firebase iOS SDK |
| Ödeme | RevenueCat `purchases-kmp` | | |
| Widget | — | Jetpack Glance | WidgetKit (Swift) |

* **Android:** minSdk 26. Compose Multiplatform Android'de birebir Jetpack Compose'dur, **performans farkı yoktur.**
* **iOS:** Skia ile çizim, 120Hz destekli. Uygulama boyutu ~10-15 MB daha büyük, açılış biraz daha uzun olabilir.
* **Mac yok:** Geliştirme Windows'ta. iOS derlemesi **bulutta (GitHub Actions macOS / Codemagic)**: her değişiklikte derleme + simülatörde çalıştırma + ekran görüntüsü. Xcode projesi `iosApp/project.yml` ile **XcodeGen** tarafından CI'da üretilir. Gerçek iPhone testi TestFlight ile (Apple Developer: 99 $/yıl).

---

## 6. Veri Sağlayıcıları ve Lisans

`MediaSearchProvider` arayüzü (Adapter Pattern):

```kotlin
interface MediaSearchProvider {
    val name: String
    val supportedTypes: Set<MediaType>
    suspend fun search(query: String, type: MediaType, page: Int = 1): Result<List<MediaSearchResult>>
    suspend fun details(result: MediaSearchResult): Result<MediaSearchResult>
}
```

Eser kaydedilince kayıt **hemen** yazılır; zenginleştirme ve poster/renk işleme arka planda yapılır.

| Tür | Sağlayıcı | Anahtar | Not |
|---|---|---|---|
| Film & Dizi | **TMDB** | v4 Read Token (`local.properties`) | Ücretsiz katman ticari olmayan kullanım içindir → **yayından önce ticari şartlar netleştirilmeli.** TMDB + JustWatch atfı zorunlu. |
| Kitap | **OpenLibrary** (+ Google Books yedek) | Yok | User-Agent zorunlu. Aynı kitabın baskıları başlık+yazara göre birleştirilecek (Faz B). |
| Oyun | **IGDB** | Cloudflare Worker proxy (`proxy/`) | Twitch secret istemcide tutulmaz. Bitirme süresi: IGDB `game_time_to_beats`. HowLongToBeat kullanılmaz. |
| Albüm & Podcast | **iTunes Search API** | Yok | ~20 istek/dk → debounce + önbellek. |
| Geliştirme | **MockProvider** | Yok | Anahtar yoksa ilgili tür Mock ile çalışır ("Örnek veri" ipucu). |

---

## 7. "WOW" Deneyimi (İmza Kurallar)

Tüm efektler sistemin "animasyonları kaldır" ayarına ve uygulama içi **Hareket efektleri / Dokunsal geri bildirim** ayarlarına uyar.

* **A. Ambiyans ışığı:** Posterin 2 rengi + türetilmiş 3. ton, yavaşça nefes alan mesh gradient; altta okunabilirlik scrim'i. ✅ Android'de cihazda doğrulandı (Xiaomi 15 Ultra, AGSL).
* **B. Tactile poster:** ±12° eğim + speküler parlama; sensör yoksa parmakla sürükleme. Sensör yalnızca ekran RESUMED iken dinlenir.
* **C. Karar çarkı:** 3B karusel, her geçişte `CLOCK_TICK`; sonuç önceden seçilir, animasyon ona göre hesaplanır. Filtreler: süre, tür, platform. Paylaşılan rafta "ortak çark".
* **D. Shared element geçişleri:** `spring(StiffnessMediumLow, DampingRatioLowBouncy)`; anahtar, posterin hangi raftan açıldığını içerir.
* **E. FIN. mührü:** 800 ms basılı tut → güçlenen titreşim → mühür → puan + anı notu kartı → "Geri al". TalkBack/VoiceOver için tek dokunuşla tamamlama eylemi.

---

## 8. Veri Modeli

### 8.1 Yerel (Room — kişisel raf ve paylaşılan rafların önbelleği)
* **`media_items`**: id, externalId (unique), mediaType, title, subtitle, posterUrl, localPosterFile (dosya adı), backdropUrl, dominantColorHex, accentColorHex, releaseYear, lengthValue (dk/sayfa/saat/bölüm/parça), genres (JSON), overview, metadataJson, status, progressValue, userRating (0.5 adım), userNote, isPinned, addedAt, startedAt, completedAt, **updatedAt**, **deletedAt** (tombstone).
* **`collections`**: id, name, iconEmoji, themeColorHex, sortOrder, smartRuleJson, updatedAt, deletedAt. *(Faz C: `remoteShelfId` — paylaşılan rafsa buluttaki karşılığı.)*
* **`collection_items`**: collectionId, mediaItemId, position, addedAt, updatedAt, deletedAt.
* **`media_items_fts`**: FTS4 (`unicode61` tokenizer) — title, subtitle, overview, genres.

### 8.2 Bulut (Firestore — yalnızca paylaşılan raflar, Faz C)
```
shelves/{shelfId}
  name, emoji, ownerUid, memberUids[], createdAt, updatedAt
  invites/{token}            # tahmin edilemez token; süreli/tek kullanımlık olabilir
  items/{externalId}         # eser bilgisinin anlık görüntüsü (title, posterUrl, mediaType, lengthValue...)
    addedBy, addedAt, votes{uid: true}, completedBy{uid: completedAt}, position, deletedAt
  plans/{planId}             # "Cuma 21:00" — itemId, scheduledAt, createdBy, rsvps{uid: yes/no}
  activity/{eventId}         # "Ayşe Dune'u ekledi" (TTL ile temizlenir)
users/{uid}
  displayName, avatarInitials, fcmTokens[]
```
* **Kimlik:** Firebase Auth — Google ile giriş + Sign in with Apple (iOS'ta zorunlu). Kişisel raf için giriş **gerekmez**; yalnızca paylaşım başlatınca/katılınca istenir.
* **Güvenlik kuralları:** Raf ve alt koleksiyonlarını yalnızca `memberUids` okur/yazar; katılım geçerli bir davet token'ı ile.
* **Kötüye kullanım:** Firebase App Check (Play Integrity / App Attest) + bütçe uyarıları (Firestore'da sert harcama tavanı yoktur).
* **Bildirimler:** FCM (iOS'ta APNs anahtarı ile). Planlar ve "yeni eser eklendi" için.
* **Web:** Firebase Hosting'te hafif bir sayfa — davet bağlantısı açılır, uygulama yoksa rafı gösterir ve mağazaya yönlendirir; Faz E'de raf vitrini.

### 8.3 Tahmini Bulut Maliyeti
Varsayım: aktif çift/grup başına günde ~240 okuma, ~10 yazma. Ücretsiz kota: 50k okuma + 20k yazma/gün, 1 GiB. Ücretli: ~0,03 $/100k okuma, ~0,09 $/100k yazma.

| Günlük aktif grup | Aylık |
|---|---|
| ≤ ~200 | 0 $ |
| 1.000 | ~2 $ |
| 10.000 | ~25 $ |
| 100.000 | ~250 $ |

Auth (≤50k MAU) ve Hosting ücretsiz katmanda. Ek sabit giderler: Apple Developer 99 $/yıl, Google Play 25 $ (tek sefer).

---

## 9. Ekranlar

1. **Lounge:** "Şu An Tükettiklerim" (cover-flow), "Sıradakiler", **"Bizim Raflar"** (Faz C), kişisel raflar (raf tahtası görünümü), "Son Bitirdiklerim".
2. **Omni-Search:** Tek arama, tür filtreleri, önce kütüphane (FTS) sonra çevrimiçi; önizleme kartından durum seçerek ekleme; ISBN barkod (Faz B).
3. **Detay:** Ambiyans + tactile poster, durum, ilerleme, puan, anı notu, raflar, FIN. mührü.
4. **Raf ekranı:** Sıralama, çıkarma, düzenleme. Paylaşılan rafta: üyeler, oylar, aktivite, ortak çark, planlar, davet.
5. **Logbook & paylaşım kartı** (Faz B).
6. **Onboarding:** (Faz C sonrası) 1) "Bizim raf"a davet, 2) her şey tek rafta, 3) eğim, 4) FIN. mührü.
7. **Ayarlar:** Haptik, hareket efektleri, atıflar; hesap ve "verimi sil" (Faz C).
8. **Share Sheet hedefi:** IMDb/TMDB linki, ISBN (Faz B).

---

## 10. Gelir Modeli

* **Ücretsiz:** Sınırsız kişisel raf ve eser, **1 paylaşılan raf**, temel karar çarkı, Auto Backup + JSON dışa aktarma. **Davet edilen kişi her zaman ücretsiz katılır.**
* **Ourshelf+:** Sınırsız paylaşılan raf, eşleşme modu, çark filtreleri, akıllı listeler, filigransız paylaşım kartları, widget'lar, Drive yedeği, özel ikon/temalar.
* **Fiyat:** Aylık 1,99 $ · Yıllık 14,99 $ · Ömür boyu 29,99 $ (bölgesel fiyatlandırma). Reklam yok.

---

## 11. Yol Haritası

* **Faz 1 — Temel (Android):** ✅ Tamamlandı.
* **Faz 2 — "WOW" UI (Android):** ✅ Tamamlandı, Xiaomi 15 Ultra'da doğrulandı.
* **Faz A — Multiplatform + Yeni Marka:**
  * Proje yapısı: `composeApp` (commonMain / androidMain / iosMain) + `iosApp` (XcodeGen).
  * Hilt → Koin, Retrofit → Ktor, Room → Room KMP, `java.time` → kotlinx-datetime, Palette/sensör/haptik/shader → `expect/actual`.
  * Testler `commonTest`'e taşınır; Android'de özellik kaybı olmadığı cihazda doğrulanır.
  * Yeniden markalama: ad, paket adı (`com.ourshelf.app`), FIN. mührü, metinler, ikon.
  * git + GitHub deposu; GitHub Actions: Android build/test + iOS build + simülatör ekran görüntüsü.
* **Faz B — Araçlar:** Karar çarkı, ISBN barkod okuyucu, Logbook + paylaşım kartı, Share Sheet hedefi, OpenLibrary baskı birleştirme, TMDB anahtarı yokken açık uyarı.
* **Faz C — "Bizim Raf":** Firebase (Auth, Firestore, FCM, App Check, Hosting), paylaşılan raflar, davet, oylar, aktivite, ortak çark, planlar + bildirimler, birlikte bitti, onboarding'in yenilenmesi, "verimi sil".
* **Faz D — Yayın:** RevenueCat + paywall, gizlilik politikası (KVKK/GDPR), Data Safety / App Privacy formları, TMDB ticari şartları, R8, Baseline Profiles, mağaza görselleri, kapalı test (Play: 12 test kullanıcısı/14 gün), TestFlight → **Android + iOS ilk yayın**.
* **Faz E — Büyüme:** Eşleşme modu, raf vitrini + kopyalama, ortak özet, widget'lar (Glance + WidgetKit), Drive/iCloud yedeği, akıllı listeler, temalar.

---

## 12. Revizyon Notları

**Revizyon 3 (2026-11-18)**
1. Ad: Unwind → **Ourshelf**; slogan ve mağaza metinleri eklendi. "Unwind" Play Store'da kalabalıktı (meditasyon uygulamaları).
2. Konsept: üç katmanlı raf (kişisel / paylaşılan / vitrin). Çiftlerle sınırlı değil; çiftler ana hedef kitle.
3. Mimari: Native Android → **Kotlin Multiplatform + Compose Multiplatform**; iOS bulutta derlenir (Mac yok).
4. "Sıfır sunucu" ilkesi esnetildi: kişisel veri cihazda kalır, **yalnızca paylaşılan raflar** Firestore'da. Maliyet tablosu eklendi.
5. Mühür: UNWOUND → **FIN.**
6. Gelir modeli: 1 paylaşılan raf ücretsiz, davetliler ücretsiz.
7. Fazlar A–E olarak yeniden düzenlendi; eski Faz 3 → Faz B, eski Faz 4 → Faz D/E.

**Revizyon 2 (2026-11-18)** — Teknoloji kesinleşti (Kotlin/Compose); TMDB/IGDB lisans notları; `collection_items` ara tablosu, `updatedAt`/`deletedAt`; yedekleme ile senkronizasyon ayrıldı; Share Sheet kapsamı daraltıldı; erişilebilirlik kuralları.

**Revizyon 1** — İlk taslak.
