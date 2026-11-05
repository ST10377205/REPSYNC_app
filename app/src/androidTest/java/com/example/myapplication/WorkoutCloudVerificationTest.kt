package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/** Explicit opt-in live checks. They use existing workouts and never create a fake fitness record. */
@RunWith(AndroidJUnit4::class)
class WorkoutCloudVerificationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun session(): Pair<Int, String> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("live_cloud") == "true")
        val prefs = context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val id = prefs.getInt("current_user_id", -1)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        assertTrue("Use the existing Google-signed-in profile for cloud verification", id > 0 && !uid.isNullOrBlank())
        assertEquals(uid, prefs.getString("firebase_uid", null))
        return id to uid!!
    }

    @Test
    fun readsOwnCollectionAndRejectsAnotherAccountsPath() = runBlocking {
        val (id, uid) = session()
        assertEquals(uid, AppDatabase.getDatabase(context).userDao().getUserById(id)!!.googleUid)
        val firestore = FirebaseFirestore.getInstance()
        val own = Tasks.await(firestore.collection("users").document(uid).collection("workouts").get(Source.SERVER), 35, TimeUnit.SECONDS)
        assertTrue(own.documents.all { it.getString("ownerUid") == uid })
        try {
            Tasks.await(firestore.collection("users").document("another-owner-verification").collection("workouts").document("not-readable").get(Source.SERVER), 35, TimeUnit.SECONDS)
            fail("Another account's workout path must not be readable")
        } catch (error: ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (error.cause as? FirebaseFirestoreException)?.code)
        }
    }

    @Test
    fun capturesDurablePendingWorkoutsWhilePhoneIsOffline() = runBlocking {
        val (id, uid) = session()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        assertFalse("Turn off internet on the phone before capturing offline proof",
            connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
        val rows = AppDatabase.getDatabase(context).workoutDao().pending(id, uid)
        assertTrue("Complete and save at least one workout while offline", rows.isNotEmpty())
        val proof = JSONObject().put("userId", id).put("ownerUid", uid).put("ids", JSONArray(rows.map { it.id }))
        File(context.filesDir, "workout-sync-verification.json").writeText(proof.toString())
        assertTrue(rows.all { it.valid() && it.syncState == WorkoutSyncState.PENDING })
    }

    @Test
    fun savedOfflineWorkoutsReachTheServerAfterReconnection() = runBlocking {
        val (id, uid) = session()
        val file = File(context.filesDir, "workout-sync-verification.json")
        assertTrue("Capture the offline-save proof first", file.exists())
        val proof = JSONObject(file.readText())
        assertEquals(id, proof.getInt("userId")); assertEquals(uid, proof.getString("ownerUid"))
        val ids = proof.getJSONArray("ids").let { array -> (0 until array.length()).map { array.getString(it) } }
        val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("workouts")
        WorkoutSyncScheduler.enqueue(context, id, uid)
        val deadline = System.currentTimeMillis() + 90000
        val database = AppDatabase.getDatabase(context)
        var verified = false
        while (System.currentTimeMillis() < deadline) {
            val snapshot = Tasks.await(collection.get(Source.SERVER), 35, TimeUnit.SECONDS)
            if (ids.all { wanted -> snapshot.documents.any { it.id == wanted } }) {
                for (wanted in ids) {
                    val local = database.workoutDao().get(id, wanted)!!
                    val remote = snapshot.documents.first { it.id == wanted }
                    assertEquals(uid, remote.getString("ownerUid"))
                    assertEquals(local.title, remote.getString("title"))
                    assertEquals(local.notes, remote.getString("notes"))
                    assertEquals(local.completedAt, remote.getLong("completedAt"))
                }
                if (ids.all { database.workoutDao().get(id, it)?.syncState == WorkoutSyncState.SYNCED }) {
                    verified = true
                    break
                }
            }
            Thread.sleep(1000)
        }
        assertTrue("Saved offline workouts must be acknowledged by Firestore and marked synced locally", verified)
    }
}
