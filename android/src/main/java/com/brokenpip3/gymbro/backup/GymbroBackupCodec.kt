package com.brokenpip3.gymbro.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class InvalidBackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

object GymbroBackupCodec {
    const val SUPPORTED_SCHEMA_VERSION = 1

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(backup: GymbroBackup): String = json.encodeToString(backup)

    fun decode(value: String): GymbroBackup {
        val backup =
            try {
                json.decodeFromString<GymbroBackup>(value)
            } catch (exception: SerializationException) {
                throw InvalidBackupException("Invalid Gymbro backup JSON", exception)
            }

        if (backup.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw InvalidBackupException(
                "Unsupported Gymbro backup schema version: ${backup.schemaVersion}",
            )
        }

        return backup
    }
}
