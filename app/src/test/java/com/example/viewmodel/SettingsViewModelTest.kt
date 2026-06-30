package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.db.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsViewModelTest {

    @Test
    fun `setThemeMode updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setThemeMode("Dark")
        assertEquals("Dark", vm.themeMode.value)
        assertEquals("Dark", SettingsManager.getThemeMode(context))
    }

    @Test
    fun `setPreferredVoice updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setPreferredVoice("sa")
        assertEquals("sa", vm.preferredVoice.value)
        assertEquals("sa", SettingsManager.getPreferredVoice(context))
    }

    @Test
    fun `setIsMusicMuted updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        assertEquals(false, vm.isMusicMuted.value)
        vm.setIsMusicMuted(true)
        assertEquals(true, vm.isMusicMuted.value)
        assertEquals(true, SettingsManager.getIsMusicMuted(context))
    }

    @Test
    fun `setIsMusicMuted toggles back to false`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setIsMusicMuted(true)
        vm.setIsMusicMuted(false)
        assertEquals(false, vm.isMusicMuted.value)
        assertEquals(false, SettingsManager.getIsMusicMuted(context))
    }

    @Test
    fun `setCurrentTrackIndex updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setCurrentTrackIndex(2)
        assertEquals(2, vm.currentTrackIndex.value)
        assertEquals(2, SettingsManager.getCurrentTrackIndex(context))
    }

    @Test
    fun `setKeepScreenAwake updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setKeepScreenAwake(false)
        assertEquals(false, vm.keepScreenAwake.value)
        assertEquals(false, SettingsManager.getKeepScreenAwake(context))
    }

    @Test
    fun `setBackgroundAudioEnabled updates state and SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = SettingsViewModel(context)
        vm.setBackgroundAudioEnabled(false)
        assertEquals(false, vm.backgroundAudioEnabled.value)
        assertEquals(false, SettingsManager.getBackgroundAudioEnabled(context))
    }

    @Test
    fun `init loads persisted values from SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        SettingsManager.savePreferredVoice(context, "sa")
        SettingsManager.saveIsMusicMuted(context, true)
        SettingsManager.saveCurrentTrackIndex(context, 1)

        val vm = SettingsViewModel(context)

        assertEquals("sa", vm.preferredVoice.value)
        assertEquals(true, vm.isMusicMuted.value)
        assertEquals(1, vm.currentTrackIndex.value)
    }

}
