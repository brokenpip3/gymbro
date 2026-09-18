package com.brokenpip3.gymbro.backup

import android.content.ContentResolver
import android.net.Uri

interface GymbroBackupFileStore {
    fun write(
        uri: String,
        text: String,
    )

    fun read(uri: String): String
}

class GymbroBackupFileStorage(
    private val contentResolver: ContentResolver,
) : GymbroBackupFileStore {
    override fun write(
        uri: String,
        text: String,
    ) {
        val target = Uri.parse(uri)
        contentResolver.openOutputStream(target)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw InvalidBackupException("Could not open file for writing: $uri")
    }

    override fun read(uri: String): String {
        val target = Uri.parse(uri)
        return contentResolver.openInputStream(target)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: throw InvalidBackupException("Could not open file for reading: $uri")
    }
}
