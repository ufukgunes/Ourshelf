package com.ourshelf.app.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Instant

/** Toplu yazma (commit) içindeki tek bir işlem. Yollar "shelves/abc" gibi görelidir. */
sealed interface FsWrite {
    val path: String

    /**
     * Belge yazar. [mask] null ise belgenin tamamı [fields] ile değiştirilir; değilse yalnızca maskedeki alanlar
     * güncellenir (maskede olup [fields]'ta olmayan alan silinir). Noktalı yollar ("votes.uid") iç içe alanı hedefler.
     */
    data class Set(
        override val path: String,
        val fields: Map<String, Any?> = emptyMap(),
        val mask: List<String>? = null,
        val mustNotExist: Boolean = false,
        val mustExist: Boolean = false,
        val serverTimestamps: List<String> = emptyList(),
        val arrayUnion: Map<String, List<Any?>> = emptyMap(),
        val arrayRemove: Map<String, List<Any?>> = emptyMap(),
    ) : FsWrite

    data class Delete(override val path: String) : FsWrite
}

/**
 * Firestore REST istemcisi. Kimlik doğrulama her istekte [AuthClient]'tan alınır; erişim denetimini
 * sunucudaki güvenlik kuralları yapar (bkz. firebase/firestore.rules).
 */
class FirestoreClient(
    private val http: HttpClient,
    private val config: CloudConfig,
    private val auth: AuthClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val apiBase get() = "${config.firestoreBase}/v1"

    private fun fullName(path: String) = "${config.documentsRoot}/$path"

    suspend fun get(path: String): FsDocument? {
        val response = http.get("$apiBase/${fullName(path)}") { authorize() }
        if (response.status == HttpStatusCode.NotFound) return null
        return parseDocument(response.requireJson())
    }

    /** Bir koleksiyonun tüm belgeleri (en fazla [pageSize]; raf içerikleri için yeterli). */
    suspend fun list(collectionPath: String, pageSize: Int = 300): List<FsDocument> {
        val response = http.get("$apiBase/${fullName(collectionPath)}") {
            authorize()
            parameter("pageSize", pageSize)
        }
        val docs = response.requireJson()["documents"] as? JsonArray ?: return emptyList()
        return docs.map { parseDocument(it.jsonObject) }
    }

    /** `collection` içinde `field` dizisi `value` içeren belgeler (örn. üyesi olduğum raflar). */
    suspend fun queryArrayContains(collection: String, field: String, value: String): List<FsDocument> {
        val body = buildJsonObject {
            putJsonObject("structuredQuery") {
                putJsonArray("from") { add(buildJsonObject { put("collectionId", collection) }) }
                putJsonObject("where") {
                    putJsonObject("fieldFilter") {
                        putJsonObject("field") { put("fieldPath", quoteFieldPath(field)) }
                        put("op", "ARRAY_CONTAINS")
                        put("value", FirestoreValues.encode(value))
                    }
                }
            }
        }
        val response = http.post("$apiBase/${config.documentsRoot}:runQuery") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.requireText()
        return json.parseToJsonElement(text).let { it as? JsonArray ?: JsonArray(emptyList()) }
            .mapNotNull { (it.jsonObject["document"] as? JsonObject)?.let(::parseDocument) }
    }

    /** Yazmaları tek bir atomik işlemde uygular. */
    suspend fun commit(writes: List<FsWrite>) {
        val body = buildJsonObject {
            put("writes", buildJsonArray { writes.forEach { add(encodeWrite(it)) } })
        }
        http.post("$apiBase/${config.documentsRoot}:commit") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.requireText()
    }

    private fun encodeWrite(write: FsWrite): JsonObject = when (write) {
        is FsWrite.Delete -> buildJsonObject { put("delete", fullName(write.path)) }
        is FsWrite.Set -> buildJsonObject {
            putJsonObject("update") {
                put("name", fullName(write.path))
                put("fields", FirestoreValues.encodeFields(nest(write.fields)))
            }
            write.mask?.let { mask ->
                putJsonObject("updateMask") { putJsonArray("fieldPaths") { mask.forEach { add(kotlinx.serialization.json.JsonPrimitive(quoteFieldPath(it))) } } }
            }
            when {
                write.mustNotExist -> putJsonObject("currentDocument") { put("exists", false) }
                write.mustExist -> putJsonObject("currentDocument") { put("exists", true) }
            }
            val transforms = buildJsonArray {
                write.serverTimestamps.forEach { field ->
                    add(buildJsonObject { put("fieldPath", quoteFieldPath(field)); put("setToServerValue", "REQUEST_TIME") })
                }
                write.arrayUnion.forEach { (field, values) ->
                    add(buildJsonObject { put("fieldPath", quoteFieldPath(field)); put("appendMissingElements", arrayValue(values)) })
                }
                write.arrayRemove.forEach { (field, values) ->
                    add(buildJsonObject { put("fieldPath", quoteFieldPath(field)); put("removeAllFromArray", arrayValue(values)) })
                }
            }
            if (transforms.isNotEmpty()) put("updateTransforms", transforms)
        }
    }

    private fun arrayValue(values: List<Any?>) = buildJsonObject {
        put("values", buildJsonArray { values.forEach { add(FirestoreValues.encode(it)) } })
    }

    /** "votes.uid" gibi noktalı anahtarları iç içe haritaya çevirir. */
    private fun nest(fields: Map<String, Any?>): Map<String, Any?> {
        val root = mutableMapOf<String, Any?>()
        fields.forEach { (key, value) ->
            val parts = key.split('.')
            var node = root
            parts.dropLast(1).forEach { part ->
                @Suppress("UNCHECKED_CAST")
                node = node.getOrPut(part) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
            }
            node[parts.last()] = value
        }
        return root
    }

    private fun parseDocument(obj: JsonObject): FsDocument {
        val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return FsDocument(
            path = name.substringAfter("/documents/"),
            fields = FirestoreValues.decodeFields(obj["fields"] as? JsonObject),
            updateTime = obj["updateTime"]?.jsonPrimitive?.contentOrNull?.let { runCatching { Instant.parse(it) }.getOrNull() },
        )
    }

    private suspend fun HttpRequestBuilder.authorize() {
        header(HttpHeaders.Authorization, "Bearer ${auth.idToken()}")
    }

    private suspend fun HttpResponse.requireText(): String {
        val text = bodyAsText()
        if (status.isSuccess()) return text
        throw when (status) {
            HttpStatusCode.Forbidden -> CloudException.PermissionDenied(text.take(300))
            HttpStatusCode.Conflict -> CloudException.AlreadyExists(text.take(300))
            HttpStatusCode.NotFound -> CloudException.NotFound(text.take(300))
            else -> CloudException.Server(status.value, text.take(300))
        }
    }

    private suspend fun HttpResponse.requireJson(): JsonObject = json.parseToJsonElement(requireText()).jsonObject
}

/**
 * Firestore alan yolu: harf/alt çizgiyle başlamayan ya da özel karakter içeren parçalar backtick ile tırnaklanır.
 * (Firebase kullanıcı kimlikleri rakamla başlayabilir: "votes.4Qx…" → "votes.`4Qx…`".)
 */
internal fun quoteFieldPath(path: String): String = path.split('.').joinToString(".") { segment ->
    if (SIMPLE_SEGMENT.matches(segment)) segment
    else "`" + segment.replace("\\", "\\\\").replace("`", "\\`") + "`"
}

private val SIMPLE_SEGMENT = Regex("[a-zA-Z_][a-zA-Z_0-9]*")
