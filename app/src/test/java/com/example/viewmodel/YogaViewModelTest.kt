package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YogaViewModelTest {

    @Test
    fun `setThemeMode updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        vm.setThemeMode("Dark")
        assertEquals("Dark", vm.themeMode.value)
        assertEquals("Dark", vm.settingsManager.themeMode.value)
    }

    @Test
    fun `setPreferredVoice updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        vm.setPreferredVoice("sa")
        assertEquals("sa", vm.preferredVoice.value)
        assertEquals("sa", vm.settingsManager.preferredVoice.value)
    }

    @Test
    fun `setIsMusicMuted updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        assertEquals(false, vm.isMusicMuted.value)
        vm.setIsMusicMuted(true)
        assertEquals(true, vm.isMusicMuted.value)
        assertEquals(true, vm.settingsManager.isMusicMuted.value)
    }

    @Test
    fun `setCurrentTrackIndex updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        vm.setCurrentTrackIndex(2)
        assertEquals(2, vm.currentTrackIndex.value)
        assertEquals(2, vm.settingsManager.currentTrackIndex.value)
    }

    @Test
    fun `setKeepScreenAwake updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        vm.setKeepScreenAwake(false)
        assertEquals(false, vm.keepScreenAwake.value)
        assertEquals(false, vm.settingsManager.keepScreenAwake.value)
    }

    @Test
    fun `setBackgroundAudioEnabled updates state and SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = YogaViewModel(context)
        vm.setBackgroundAudioEnabled(false)
        assertEquals(false, vm.backgroundAudioEnabled.value)
        assertEquals(false, vm.settingsManager.backgroundAudioEnabled.value)
    }

    @Test
    fun `init loads persisted values from SettingsManager`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val initialVm = YogaViewModel(context)
        initialVm.setPreferredVoice("sa")
        initialVm.setIsMusicMuted(true)
        initialVm.setCurrentTrackIndex(1)

        val vm = YogaViewModel(context)

        assertEquals("sa", vm.preferredVoice.value)
        assertEquals(true, vm.isMusicMuted.value)
        assertEquals(1, vm.currentTrackIndex.value)
    }
}
