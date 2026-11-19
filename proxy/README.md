# IGDB proxy (Cloudflare Worker)

Oyun verisi IGDB'den gelir. IGDB'ye erişim için Twitch Client Secret gerekir ve bu secret APK'ya gömülmemelidir. Bu küçük Worker secret'ı sunucu tarafında tutar ve izin verilen iki endpoint'e (`games`, `game_time_to_beats`) gelen istekleri iletir. Cloudflare'in ücretsiz katmanı (günde 100.000 istek) yeterlidir.

Proxy URL'si ayarlanmazsa uygulama oyunlar için örnek (Mock) veriyle çalışır.

## Kurulum

Gereken: Node.js, ücretsiz bir Cloudflare hesabı ve **istemci türü "Gizli"** olan bir Twitch uygulaması
(https://dev.twitch.tv/console → OAuth Redirect URL: `http://localhost`, kategori: Application Integration).
Twitch istemci kimliği `wrangler.toml` içindedir; parola ise yalnızca Cloudflare'e kaydedilir.

```sh
cd proxy
npx wrangler login                              # tarayıcıda Cloudflare girişini onayla
npx wrangler secret put TWITCH_CLIENT_SECRET    # parolayı yapıştır (ekranda görünmez)
npx wrangler deploy                             # çıktıdaki https://ourshelf-igdb.<hesap>.workers.dev adresini al
```

Ardından uygulamanın `local.properties` dosyasına:

```properties
IGDB_PROXY_URL=https://ourshelf-igdb.<hesabın>.workers.dev/
```

İsteğe bağlı olarak `npx wrangler secret put APP_KEY` ile bir anahtar belirleyip aynı değeri `IGDB_PROXY_KEY` olarak
ekleyebilirsin; APK'dan çıkarılabildiği için yalnızca rastgele kötüye kullanımı azaltır.

## Test

```sh
curl -X POST https://ourshelf-igdb.<hesabın>.workers.dev/games \
  -H "X-Ourshelf-Key: <APP_KEY>" \
  -d 'search "hades"; fields name,cover.image_id; limit 3;'
```
