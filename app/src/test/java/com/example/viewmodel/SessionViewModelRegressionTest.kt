package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.db.SettingsManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionViewModelRegressionTest {

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("app_settings_backup", 0).edit().clear().apply()
        context.getSharedPreferences("ambient_music_prefs", 0).edit().clear().apply()
    }

    @Test
    fun `toggleMusicMute flips from default false to true`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(false, SettingsManager.getIsMusicMuted(context))

        val vm = SessionViewModel(context)
        vm.toggleMusicMute()

        assertEquals(true, SettingsManager.getIsMusicMuted(context))
    }

    @Test
    fun `toggleMusicMute flips back to false when called twice`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(false, SettingsManager.getIsMusicMuted(context))

        val vm = SessionViewModel(context)
        vm.toggleMusicMute()
        assertEquals(true, SettingsManager.getIsMusicMuted(context))

        vm.toggleMusicMute()
        assertEquals(false, SettingsManager.getIsMusicMuted(context))
    }

    @Test
    fun `setPreferredVoice updates internal state`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SessionViewModel(context)
        assertEquals("en", vm.preferredVoice.value)

        vm.setPreferredVoice("sa")
        assertEquals("sa", vm.preferredVoice.value)
    }

    @Test
    fun `setPreferredVoice persists to SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SessionViewModel(context)
        assertEquals("en", SettingsManager.getPreferredVoice(context))

        vm.setPreferredVoice("sa")
        assertEquals("sa", SettingsManager.getPreferredVoice(context))
    }
}
