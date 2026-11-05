package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class WorkoutModelTest {
    private fun row() = Workout("workout-1", 1, "owner-a", "Home Workout", 1700000000000, 90, 120.0, 3, "My notes", WorkoutSyncState.PENDING)

    @Test
    fun cloudRoundTripKeepsWorkoutFieldsButUsesTheReceivingDevicesLocalUserId() {
        val original = row()
        val restored = Workout.fromCloud(original.id, original.cloudData(), 42, "owner-a")!!
        assertEquals(42, restored.localUserId)
        assertEquals(original.id, restored.id)
        assertEquals(original.notes, restored.notes)
        assertEquals(original.durationSeconds, restored.durationSeconds)
        assertEquals(WorkoutSyncState.SYNCED, restored.syncState)
    }

    @Test
    fun wrongOwnerWrongDocumentIdAndInvalidMetricsAreNotImported() {
        val original = row()
        assertNull(Workout.fromCloud(original.id, original.cloudData(), 2, "owner-b"))
        assertNull(Workout.fromCloud("different-id", original.cloudData(), 2, "owner-a"))
        assertFalse(original.copy(totalVolume = Double.NaN).valid())
        assertFalse(original.copy(totalVolume = Double.POSITIVE_INFINITY).valid())
        assertFalse(original.copy(durationSeconds = -1).valid())
        assertFalse(original.copy(completedSets = -1).valid())
    }

    @Test
    fun legacyParserKeepsTitlesWithSeparatorsAndHasStableRetryIds() {
        val history = "Home • Full Body • 250 kg • 1700000000000\n"
        val first = LegacyWorkoutParser.parse(7, "owner-a", history).single()
        assertEquals("Home • Full Body", first.title)
        assertEquals(250.0, first.totalVolume, 0.0)
        assertEquals(first.id, LegacyWorkoutParser.parse(7, "owner-a", history).single().id)
        assertEquals(WorkoutSyncState.PENDING, first.syncState)
        assertEquals(WorkoutSyncState.LOCAL_ONLY, LegacyWorkoutParser.parse(7, null, history).single().syncState)
    }

    @Test
    fun malformedLegacyRowsAreSkippedWithoutDiscardingValidHistory() {
        val history = "broken\nWorkout • nope kg • 1700000000000\nWorkout • 4 kg • bad-date\nWorkout • 5 kg • 1700000000001\n"
        val rows = LegacyWorkoutParser.parse(1, null, history)
        assertEquals(1, rows.size)
        assertEquals(5.0, rows.single().totalVolume, 0.0)
    }
}
