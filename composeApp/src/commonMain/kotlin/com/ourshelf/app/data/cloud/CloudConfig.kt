package com.ourshelf.app.data.cloud

import com.ourshelf.app.Secrets

/**
 * Firebase REST uç noktaları. Paylaşım özellikleri Firebase'in yerel SDK'ları yerine REST ile konuşur:
 * Android ve iOS'ta aynı kod çalışır, iOS'a yerel kütüphane eklenmez ve her şey bu bilgisayarda test edilebilir.
 * Testlerde aynı sınıf Firebase Emulator adreslerine yönlendirilir.
 */
data class CloudConfig(
    val projectId: String,
    val apiKey: String,
    val firestoreBase: String = "https://firestore.googleapis.com",
    val identityBase: String = "https://identitytoolkit.googleapis.com",
    val secureTokenBase: String = "https://securetoken.googleapis.com",
    /** Emulator'da kurallar "owner" yetkisiyle atlanabilir (yalnızca testlerde kullanılır). */
    val isEmulator: Boolean = false,
) {
    val isConfigured: Boolean get() = projectId.isNotBlank() && apiKey.isNotBlank()

    val documentsRoot: String get() = "projects/$projectId/databases/(default)/documents"

    companion object {
        fun fromSecrets() = CloudConfig(projectId = Secrets.FIREBASE_PROJECT_ID, apiKey = platformApiKey())

        /** Firebase Emulator Suite (firebase/firebase.json'daki portlar). */
        fun emulator(host: String = "localhost", projectId: String = "demo-ourshelf") = CloudConfig(
            projectId = projectId,
            apiKey = "fake-api-key",
            firestoreBase = "http://$host:8080",
            identityBase = "http://$host:9099/identitytoolkit.googleapis.com",
            secureTokenBase = "http://$host:9099/securetoken.googleapis.com",
            isEmulator = true,
        )
    }
}

/** Her platform kendi Firebase uygulamasının API anahtarını kullanır. */
internal expect fun platformApiKey(): String

sealed class CloudException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotConfigured : CloudException("Firebase is not configured")
    class PermissionDenied(message: String) : CloudException(message)
    class AlreadyExists(message: String) : CloudException(message)
    class NotFound(message: String) : CloudException(message)
    class Server(val status: Int, message: String) : CloudException("HTTP $status: $message")
}
