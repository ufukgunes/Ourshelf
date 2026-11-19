# Ourshelf

*Your shelf. Our shelf.* — İzleyeceğin, okuyacağın, oynayacağın her şey tek rafta; sevdiklerinle paylaş.

Ürün spesifikasyonu ve yol haritası: [ourshelf_proje_spesifikasyonu.md](ourshelf_proje_spesifikasyonu.md)

## Yapı

Kotlin Multiplatform + Compose Multiplatform (Android + iOS).

```
composeApp/
  src/commonMain/      Ortak kod (UI, veri, iş mantığı) — kodun büyük kısmı burada
  src/androidMain/     Android'e özgü: Activity, sensör, haptik, AGSL shader, Room/DataStore kurulumu
  src/iosMain/         iOS'a özgü: CoreMotion, UIKit haptik, Skia shader, giriş noktası
  src/commonTest/      Platformdan bağımsız testler (Android + iOS simülatöründe çalışır)
  src/androidUnitTest/ Robolectric testleri (Room, Compose ekranları)
iosApp/                SwiftUI kabuğu + XcodeGen tanımı (project.yml)
proxy/                 IGDB için Cloudflare Worker
```

## Geliştirme (Windows / Linux / macOS)

Gereken: JDK 17+ ve Android SDK (compileSdk 36).

```sh
./gradlew :composeApp:testDebugUnitTest   # testler
./gradlew :composeApp:assembleDebug       # APK → composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug        # bağlı Android cihaza kur
```

iOS hedefleri yalnızca macOS'ta derlenir; Windows'ta otomatik atlanır.

## iOS (Mac olmadan)

`.github/workflows/ci.yml` her push'ta macOS makinesinde:
1. Ortak testleri iOS simülatöründe çalıştırır,
2. `xcodegen` ile Xcode projesini üretip uygulamayı derler,
3. Simülatörde açıp ekran görüntüsü alır (Actions → çalıştırma → *ios-screenshots* artifact'ı).

Gerçek iPhone'a kurulum TestFlight ile yapılacak (Apple Developer hesabı gerekir, Faz D).

## API anahtarları

`local.properties` (yerel) veya GitHub Secrets (CI) üzerinden; derleme sırasında `Secrets` nesnesi üretilir, repoya girmez.

| Anahtar | Ne için | Yoksa |
|---|---|---|
| `TMDB_API_KEY` | Film & dizi | Örnek veri |
| `IGDB_PROXY_URL`, `IGDB_PROXY_KEY` | Oyun (bkz. `proxy/README.md`) | Örnek veri |

Kitap (OpenLibrary), albüm ve podcast (iTunes) anahtarsız çalışır.
