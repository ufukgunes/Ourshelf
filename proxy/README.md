# IGDB proxy (Cloudflare Worker)

Oyun verisi IGDB'den gelir. IGDB'ye erişim için Twitch Client Secret gerekir ve bu secret APK'ya gömülmemelidir. Bu küçük Worker secret'ı sunucu tarafında tutar ve izin verilen iki endpoint'e (`games`, `game_time_to_beats`) gelen istekleri iletir. Cloudflare'in ücretsiz katmanı (günde 100.000 istek) yeterlidir.

Proxy URL'si ayarlanmazsa uygulama oyunlar için örnek (Mock) veriyle çalışır.

## Kurulum

1. https://dev.twitch.tv/console adresinde bir uygulama oluştur (OAuth Redirect URL: `http://localhost`, kategori: Application Integration). **Client ID** ve **Client Secret**'ı al.
2. Worker'ı yayınla:
   ```sh
   npm create cloudflare@latest ourshelf-igdb -- --type hello-world
   # Oluşan src/index.js içeriğini proxy/igdb-worker.js ile değiştir
   cd ourshelf-igdb
   npx wrangler secret put TWITCH_CLIENT_ID
   npx wrangler secret put TWITCH_CLIENT_SECRET
   npx wrangler secret put APP_KEY        # isteğe bağlı
   npx wrangler deploy
   ```
3. Uygulamanın `local.properties` dosyasına ekle:
   ```properties
   IGDB_PROXY_URL=https://ourshelf-igdb.<hesabın>.workers.dev/
   IGDB_PROXY_KEY=<APP_KEY ile aynı değer, ayarladıysan>
   ```

## Test

```sh
curl -X POST https://ourshelf-igdb.<hesabın>.workers.dev/games \
  -H "X-Ourshelf-Key: <APP_KEY>" \
  -d 'search "hades"; fields name,cover.image_id; limit 3;'
```
