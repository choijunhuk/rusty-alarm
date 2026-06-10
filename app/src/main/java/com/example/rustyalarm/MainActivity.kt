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
import com.example.rustyalarm.ui.navigation.AppNavigation
import com.example.rustyalarm.ui.screens.LockScreen
import com.example.rustyalarm.ui.screens.PinSetupScreen
import com.example.rustyalarm.ui.theme.RustyAlarmTheme

class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val app        = application as RustyAlarmApplication
        val repository = app.repository
        val eventDao   = app.database.alarmEventDao()
        val authRepo   = AuthRepository(this)
        val authVm     = ViewModelProvider(this, AuthViewModel.Factory(authRepo))[AuthViewModel::class.java]
        authVmRef = authVm
        val themePrefs = ThemePreferences(applicationContext)

        setContent {
            val themeMode by themePrefs.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            RustyAlarmTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val unlocked by authVm.unlocked.collectAsStateWithLifecycle()
                    val hasPin by authVm.hasPin.collectAsStateWithLifecycle()
                    val biometricEnabled = authVm.biometricEnabled()
                    val canBio = remember { canUseBiometric() }

                    // Auto-try biometric on launch (only after PIN exists, lock state, opt-in flag)
                    LaunchedEffect(unlocked, hasPin) {
                        if (!unlocked && hasPin && biometricEnabled && canBio) {
                            promptBiometric(authVm)
                        }
                    }

                    when {
                        !hasPin -> PinSetupScreen(
                            vm = authVm,
                            onComplete = { /* unlocked flag flips inside setPin */ },
                        )
                        !unlocked -> LockScreen(
                            vm = authVm,
                            biometricEnabled = biometricEnabled && canBio,
                            onBiometric = { promptBiometric(authVm) },
                        )
                        else -> AppNavigation(
                            repository = repository,
                            eventDao = eventDao,
                            authVm = authVm,
                            themePrefs = themePrefs,
                            canUseBiometric = canBio,
                            onChangePin = {
                                // resetPin already flipped hasPin=false, navigation will surface PinSetupScreen automatically
                            },
                        )
                    }
                }
            }
        }
    }

    private var authVmRef: AuthViewModel? = null

    private fun canUseBiometric(): Boolean {
        val mgr = BiometricManager.from(this)
        // Prefer STRONG. Fall back to STRONG+CREDENTIAL only on Android 11+ where
        // setAllowedAuthenticators(STRONG|CREDENTIAL) is supported.
        val strongOnly = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        return strongOnly
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
        // Re-lock when leaving the foreground so an attacker with physical access
        // can't simply re-open the task. Matches the security-review guidance.
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
