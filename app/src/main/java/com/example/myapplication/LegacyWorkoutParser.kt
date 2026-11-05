package com.example.myapplication

import java.util.UUID

object LegacyWorkoutParser {
    fun parse(userId: Int, ownerUid: String?, history: String): List<Workout> =
        history.lineSequence().mapIndexedNotNull { index, line ->
            val timeSeparator = line.lastIndexOf(" • ")
            if (timeSeparator < 0) return@mapIndexedNotNull null
            val beforeTime = line.substring(0, timeSeparator)
            val volumeSeparator = beforeTime.lastIndexOf(" • ")
            if (volumeSeparator < 0) return@mapIndexedNotNull null
            val timestamp = line.substring(timeSeparator + 3).trim().toLongOrNull() ?: return@mapIndexedNotNull null
            val volume = beforeTime.substring(volumeSeparator + 3).trim().removeSuffix(" kg").toDoubleOrNull()
                ?: return@mapIndexedNotNull null
            val title = beforeTime.substring(0, volumeSeparator).trim().take(200)
            val id = UUID.nameUUIDFromBytes("legacy:$userId:$index:$line".toByteArray(Charsets.UTF_8)).toString()
            Workout(id, userId, ownerUid, title, timestamp, 0, volume, 0, "",
                if (ownerUid == null) WorkoutSyncState.LOCAL_ONLY else WorkoutSyncState.PENDING).takeIf { it.valid() }
        }.toList()
}
