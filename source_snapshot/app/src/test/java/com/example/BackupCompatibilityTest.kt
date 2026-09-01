package com.example

import com.example.data.local.BackupData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCompatibilityTest {
    @Test
    fun oldBackupWithoutTemplatesStillOpens() {
        val oldJson = """{"folders":[],"plans":[]}"""

        val backup = Json { ignoreUnknownKeys = true }.decodeFromString<BackupData>(oldJson)

        assertTrue(backup.templates.isEmpty())
    }

    @Test
    fun unknownFieldsFromFutureBackupAreIgnoredByRestoreParser() {
        val futureJson = """{"folders":[],"plans":[],"templates":[],"futureField":42}"""

        val backup = Json { ignoreUnknownKeys = true }.decodeFromString<BackupData>(futureJson)

        assertEquals(0, backup.folders.size)
    }
}
