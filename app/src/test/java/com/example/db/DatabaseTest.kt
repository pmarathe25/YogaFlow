package com.example.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseTest {

    private lateinit var db: YogaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, YogaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `database initializes and can access DAOs`() {
        assertNotNull(db.yogaSessionDao())
        assertNotNull(db.reminderDao())
    }

    @Test
    fun `can insert session`() = runTest {
        val session = YogaSession(
            id = 0,
            flowId = "test_flow",
            flowName = "Test Flow",
            durationMinutes = 10,
            timestamp = System.currentTimeMillis()
        )
        db.yogaSessionDao().insertSession(session)
        // No crash means success for basic integrity
    }
}
