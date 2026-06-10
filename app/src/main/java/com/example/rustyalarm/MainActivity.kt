package com.example.rustyalarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rustyalarm.auth.AuthRepository
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.prefs.ThemeMode
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.prefs.UserPreferences
import com.example.rustyalarm.prefs.UserProfile
import com.example.rustyalarm.ui.navigation.AppNavigation
import com.example.rustyalarm.ui.screens.LockScreen
import com.example.rustyalarm.ui.screens.OnboardingScreen
import com.example.rustyalarm.ui.screens.PinSetupScreen
import com.example.rustyalarm.ui.theme.RustyAlarmTheme

class MainActivity : FragmentActivity() {

    private var authVmRef: AuthViewModel? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val app        = application as RustyAlarmApplication
        val repository = app.repository
        val eventDao   = app.database.alarmEventDao()
        val petDao     = app.database.petDao()
        val authRepo   = AuthRepository(this)
        val authVm     = ViewModelProvider(this, AuthViewModel.Factory(authRepo))[AuthViewModel::class.java]
        authVmRef = authVm
        val themePrefs = ThemePreferences(applicationContext)
        val userPrefs  = UserPreferences(applicationContext)

        setContent {
            val themeMode by themePrefs.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val userProfile by userPrefs.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
            RustyAlarmTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val unlocked by authVm.unlocked.collectAsStateWithLifecycle()
                    val hasPin   by authVm.hasPin.collectAsStateWithLifecycle()
                    val biometricEnabled = authVm.biometricEnabled()
                    val canBio = remember { canUseBiometric() }

                    // Only auto-prompt biometric when app lock is actively engaged
                    LaunchedEffect(unlocked, hasPin, userProfile.appLockEnabled) {
                        if (userProfile.appLockEnabled && hasPin && !unlocked
                            && biometricEnabled && canBio) {
                            promptBiometric(authVm)
                        }
                    }

                    when {
                        // First-launch onboarding
                        !userProfile.hasOnboarded -> OnboardingScreen(
                            userPrefs = userPrefs,
                            onComplete = {},   // hasOnboarded flips inside completeOnboarding
                        )

                        // App lock turned on but no PIN yet → one-time setup
                        userProfile.appLockEnabled && !hasPin -> PinSetupScreen(
                            vm = authVm,
                            onComplete = {},
                        )

                        // App lock + PIN exists + currently locked
                        userProfile.appLockEnabled && hasPin && !unlocked -> LockScreen(
                            vm = authVm,
                            biometricEnabled = biometricEnabled && canBio,
                            onBiometric = { promptBiometric(authVm) },
                        )

                        // Normal app
                        else -> AppNavigation(
                            repository = repository,
                            eventDao   = eventDao,
                            petDao     = petDao,
                            authVm     = authVm,
                            themePrefs = themePrefs,
                            userPrefs  = userPrefs,
                            userProfile = userProfile,
                            canUseBiometric = canBio,
                        )
                    }
                }
            }
        }
    }

    private fun canUseBiometric(): Boolean {
        val mgr = BiometricManager.from(this)
        return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun promptBiometric(authVm: AuthViewModel) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authVm.unlockViaBiometric()
            }
        }
        val prompt = BiometricPrompt(this, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Rusty Alarm 잠금 해제")
            .setSubtitle("지문으로 인증하세요")
            .setNegativeButtonText("PIN 사용")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    override fun onStop() {
        super.onStop()
        // Only re-lock when the user has explicitly opted into app lock
        // (avoid surprising users who never enabled it)
        authVmRef?.lock()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
