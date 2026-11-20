package com.ourshelf.app.data.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * Firestore REST "Value" biçimi ile Kotlin değerleri arasında çeviri.
 * Desteklenen tipler: null, Boolean, Int/Long, Float/Double, String, Instant, List, Map<String, *>.
 */
object FirestoreValues {

    fun encode(value: Any?): JsonObject = buildJsonObject {
        when (value) {
            null -> put("nullValue", JsonNull)
            is Boolean -> put("booleanValue", value)
            is Int -> put("integerValue", value.toString())
            is Long -> put("integerValue", value.toString())
            is Float -> put("doubleValue", value.toDouble())
            is Double -> put("doubleValue", value)
            is String -> put("stringValue", value)
            is Instant -> put("timestampValue", value.toString())
            is List<*> -> put("arrayValue", buildJsonObject {
                put("values", buildJsonArray { value.forEach { add(encode(it)) } })
            })
            is Map<*, *> -> put("mapValue", buildJsonObject { put("fields", encodeFields(value)) })
            else -> error("Unsupported Firestore value type: ${value::class}")
        }
    }

    fun encodeFields(map: Map<*, *>): JsonObject = buildJsonObject {
        map.forEach { (k, v) -> put(k as String, encode(v)) }
    }

    fun decode(element: JsonElement): Any? {
        val obj = element.jsonObject
        val (type, raw) = obj.entries.firstOrNull() ?: return null
        return when (type) {
            "nullValue" -> null
            "booleanValue" -> raw.jsonPrimitive.booleanOrNull
            "integerValue" -> raw.jsonPrimitive.contentOrNull?.toLongOrNull()
            "doubleValue" -> raw.jsonPrimitive.doubleOrNull
            "stringValue" -> raw.jsonPrimitive.contentOrNull
            "timestampValue" -> raw.jsonPrimitive.contentOrNull?.let { runCatching { Instant.parse(it) }.getOrNull() }
            "arrayValue" -> (raw.jsonObject["values"] as? JsonArray).orEmpty().map(::decode)
            "mapValue" -> decodeFields(raw.jsonObject["fields"] as? JsonObject)
            else -> (raw as? JsonPrimitive)?.contentOrNull // referenceValue, geoPointValue vb. kullanılmıyor
        }
    }

    fun decodeFields(fields: JsonObject?): Map<String, Any?> =
        fields?.mapValues { (_, v) -> decode(v) }.orEmpty()
}

/** Okunan bir Firestore belgesi. [path]: "shelves/abc/items/xyz" gibi göreli yol. */
data class FsDocument(
    val path: String,
    val fields: Map<String, Any?>,
    val updateTime: Instant?,
) {
    val id: String get() = path.substringAfterLast('/')

    fun string(key: String): String? = fields[key] as? String
    fun long(key: String): Long? = (fields[key] as? Number)?.toLong()
    fun instant(key: String): Instant? = fields[key] as? Instant

    @Suppress("UNCHECKED_CAST")
    fun map(key: String): Map<String, Any?> = fields[key] as? Map<String, Any?> ?: emptyMap()

    fun stringList(key: String): List<String> = (fields[key] as? List<*>).orEmpty().filterIsInstance<String>()
}
