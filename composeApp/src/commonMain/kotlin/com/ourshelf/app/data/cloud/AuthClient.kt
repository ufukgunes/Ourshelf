package com.ourshelf.app.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/** Cihazda saklanan anonim oturum. */
@Serializable
data class CloudSession(
    val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

interface SessionStore {
    suspend fun load(): CloudSession?
    suspend fun save(session: CloudSession?)
}

class InMemorySessionStore : SessionStore {
    private var session: CloudSession? = null
    override suspend fun load() = session
    override suspend fun save(session: CloudSession?) {
        this.session = session
    }
}

/**
 * Firebase Auth REST: anonim hesap ve kimlik token'ı yenileme.
 * Hesap, kullanıcı paylaşımı ilk kez kullandığında oluşturulur; kişisel kütüphane için hiç gerekmez.
 */
class AuthClient(
    private val http: HttpClient,
    private val config: CloudConfig,
    private val store: SessionStore,
    private val clock: Clock = Clock.System,
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun currentUid(): String? = store.load()?.uid

    /** Gerekirse anonim hesap açar ve geçerli bir kimlik token'ı döndürür. */
    suspend fun idToken(): String = session().idToken

    suspend fun session(): CloudSession = mutex.withLock {
        if (!config.isConfigured) throw CloudException.NotConfigured()
        val existing = store.load()
        val fresh = when {
            existing == null -> signUpAnonymously()
            existing.expiresAtMillis - clock.now().toEpochMilliseconds() < REFRESH_MARGIN_MS -> refresh(existing)
            else -> return@withLock existing
        }
        store.save(fresh)
        fresh
    }

    private suspend fun signUpAnonymously(): CloudSession {
        val response = http.post("${config.identityBase}/v1/accounts:signUp") {
            parameter("key", config.apiKey)
            contentType(ContentType.Application.Json)
            setBody("""{"returnSecureToken":true}""")
        }
        val body = response.requireJson()
        return CloudSession(
            uid = body.str("localId"),
            idToken = body.str("idToken"),
            refreshToken = body.str("refreshToken"),
            expiresAtMillis = expiresAt(body.str("expiresIn")),
        )
    }

    private suspend fun refresh(session: CloudSession): CloudSession {
        val response = http.submitForm(
            url = "${config.secureTokenBase}/v1/token?key=${config.apiKey}",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", session.refreshToken)
            },
        )
        val body = response.requireJson()
        return CloudSession(
            uid = body.str("user_id"),
            idToken = body.str("id_token"),
            refreshToken = body.str("refresh_token"),
            expiresAtMillis = expiresAt(body.str("expires_in")),
        )
    }

    private fun expiresAt(seconds: String): Long =
        clock.now().toEpochMilliseconds() + (seconds.toLongOrNull() ?: 3600L) * 1000

    private suspend fun HttpResponse.requireJson(): JsonObject {
        val text = bodyAsText()
        if (!status.isSuccess()) throw CloudException.Server(status.value, text.take(300))
        return json.parseToJsonElement(text).jsonObject
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: throw CloudException.Server(200, "missing $key")

    private companion object {
        const val REFRESH_MARGIN_MS = 5 * 60 * 1000L
    }
}
