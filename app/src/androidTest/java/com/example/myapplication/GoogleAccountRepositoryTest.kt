package com.example.myapplication

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GoogleAccountRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrationKeepsExistingUserIdPasswordAndName() = runBlocking {
        val name = "google-migration-test-${UUID.randomUUID()}"
        val hash = BCrypt.hashpw("ExistingPassword123", BCrypt.gensalt(4))
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fullName TEXT NOT NULL, email TEXT NOT NULL, password TEXT NOT NULL)")
            old.execSQL("INSERT INTO users (id, fullName, email, password) VALUES (?, ?, ?, ?)", arrayOf(41, "Existing Athlete", "athlete@example.test", hash))
            old.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            val account = database.userDao().getUserById(41)!!
            assertEquals(41, account.id)
            assertEquals("Existing Athlete", account.fullName)
            assertEquals(hash, account.password)
            assertNull(account.googleUid)
            assertTrue(PasswordVerifier.matches("ExistingPassword123", account.password))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun firstGoogleLoginCreatesOneAccountAndReturningUidKeepsTheSameId() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = GoogleAccountRepository(database)
            val identity = VerifiedGoogleIdentity("google-test-uid", "NewAthlete@example.test", "Google Athlete")
            val first = repository.signIn(identity) as GoogleAccountResult.SignedIn
            val second = repository.signIn(identity) as GoogleAccountResult.SignedIn
            assertEquals(first.userId, second.userId)
            val account = database.userDao().getUserById(first.userId)!!
            assertEquals(identity.uid, account.googleUid)
            assertEquals("newathlete@example.test", account.email)
            assertEquals("Google Athlete", account.fullName)
            assertFalse(PasswordVerifier.matches("any password", account.password))
        } finally { database.close() }
    }

    @Test
    fun linkingRequiresCorrectLocalPasswordAndPreservesProgressOwnerId() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val hash = BCrypt.hashpw("ExistingPassword123", BCrypt.gensalt(4))
            val oldId = database.userDao().signup(User(fullName = "Existing Name", email = "Athlete@example.test", password = hash)).toInt()
            val identity = VerifiedGoogleIdentity("google-linked-uid", "athlete@example.test", "Google Name")
            val repository = GoogleAccountRepository(database)
            assertEquals(GoogleAccountResult.PasswordRequired, repository.signIn(identity))
            assertNull(database.userDao().getUserById(oldId)!!.googleUid)
            try {
                repository.signIn(identity, "wrong password")
                fail("An incorrect password must not link the account")
            } catch (_: LocalLinkPasswordException) { }
            assertNull(database.userDao().getUserById(oldId)!!.googleUid)
            val linked = repository.signIn(identity, "ExistingPassword123") as GoogleAccountResult.SignedIn
            assertEquals(oldId, linked.userId)
            val account = database.userDao().getUserById(oldId)!!
            assertEquals("Existing Name", account.fullName)
            assertEquals(hash, account.password)
            assertEquals(identity.uid, account.googleUid)
            assertEquals(oldId, (repository.signIn(identity) as GoogleAccountResult.SignedIn).userId)
        } finally { database.close() }
    }

    @Test
    fun anExistingGoogleAccountCannotBeReassignedToAnotherGoogleUid() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = GoogleAccountRepository(database)
            val first = VerifiedGoogleIdentity("first-uid", "athlete@example.test", "First")
            repository.signIn(first)
            try {
                repository.signIn(first.copy(uid = "different-uid"))
                fail("A different Google UID must not take over the mapped account")
            } catch (_: GoogleAccountConflictException) { }
            assertNotNull(database.userDao().getUserByGoogleUid(first.uid))
            assertNull(database.userDao().getUserByGoogleUid("different-uid"))
        } finally { database.close() }
    }
}
