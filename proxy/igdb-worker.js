/**
 * Ourshelf IGDB proxy — Cloudflare Workers (ücretsiz katman: 100.000 istek/gün).
 *
 * Neden var? IGDB, Twitch "Client Secret" ile alınan bir erişim token'ı ister. Secret APK'ya gömülürse
 * herkes çıkarabilir. Bu Worker secret'ı sunucu tarafında tutar, token'ı önbellekler ve yalnızca
 * izin verilen IGDB endpoint'lerine POST isteklerini iletir. Kullanıcı verisi taşımaz ve saklamaz.
 *
 * Gerekli secret'lar (wrangler secret put ...):
 *   TWITCH_CLIENT_ID      Twitch geliştirici konsolundaki uygulamanın Client ID'si
 *   TWITCH_CLIENT_SECRET  Aynı uygulamanın Client Secret'ı
 * İsteğe bağlı:
 *   APP_KEY               Ayarlanırsa istemcinin `X-Ourshelf-Key` başlığında göndermesi gerekir
 *                         (APK'dan çıkarılabilir; sadece rastgele kötüye kullanımı azaltır).
 */

const ALLOWED_ENDPOINTS = new Set(["games", "game_time_to_beats"]);
const MAX_BODY_BYTES = 2048;
const CACHE_TTL_SECONDS = 60 * 60 * 24; // Oyun meta verisi nadiren değişir.

let cachedToken = null; // Worker izolatı yaşadığı sürece bellekte tutulur.

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const endpoint = url.pathname.replace(/^\/+/, "").replace(/^v4\//, "");

    if (request.method !== "POST" || !ALLOWED_ENDPOINTS.has(endpoint)) {
      return new Response("Not found", { status: 404 });
    }
    if (env.APP_KEY && request.headers.get("X-Ourshelf-Key") !== env.APP_KEY) {
      return new Response("Forbidden", { status: 403 });
    }

    const body = await request.text();
    if (body.length === 0 || body.length > MAX_BODY_BYTES) {
      return new Response("Bad request", { status: 400 });
    }

    // POST yanıtları Cache API'de doğrudan önbelleklenemez; gövdenin hash'inden sentetik bir GET anahtarı üret.
    const cacheKey = new Request(`https://igdb-cache.ourshelf/${endpoint}/${await sha256(body)}`);
    const cache = caches.default;
    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    let upstream = await callIgdb(env, endpoint, body, false);
    if (upstream.status === 401) {
      upstream = await callIgdb(env, endpoint, body, true); // Token süresi dolmuş olabilir.
    }

    const response = new Response(upstream.body, {
      status: upstream.status,
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": `public, max-age=${CACHE_TTL_SECONDS}`,
      },
    });
    if (upstream.ok) ctx.waitUntil(cache.put(cacheKey, response.clone()));
    return response;
  },
};

async function callIgdb(env, endpoint, body, forceRefresh) {
  const token = await getToken(env, forceRefresh);
  return fetch(`https://api.igdb.com/v4/${endpoint}`, {
    method: "POST",
    headers: {
      "Client-ID": env.TWITCH_CLIENT_ID,
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
      "Content-Type": "text/plain",
    },
    body,
  });
}

async function getToken(env, forceRefresh) {
  if (!forceRefresh && cachedToken && cachedToken.expiresAt > Date.now() + 60_000) {
    return cachedToken.value;
  }
  const params = new URLSearchParams({
    client_id: env.TWITCH_CLIENT_ID,
    client_secret: env.TWITCH_CLIENT_SECRET,
    grant_type: "client_credentials",
  });
  const res = await fetch(`https://id.twitch.tv/oauth2/token?${params}`, { method: "POST" });
  if (!res.ok) throw new Error(`Twitch token request failed: ${res.status}`);
  const json = await res.json();
  cachedToken = { value: json.access_token, expiresAt: Date.now() + json.expires_in * 1000 };
  return cachedToken.value;
}

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
