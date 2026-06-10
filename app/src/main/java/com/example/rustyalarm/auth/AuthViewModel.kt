package com.example.rustyalarm.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _hasPin = MutableStateFlow(repository.hasPin())
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _throttleSeconds = MutableStateFlow(repository.throttleSecondsRemaining())
    val throttleSeconds: StateFlow<Long> = _throttleSeconds.asStateFlow()

    fun biometricEnabled(): Boolean = repository.biometricEnabled

    fun setBiometricEnabled(value: Boolean) {
        repository.biometricEnabled = value
    }

    fun setPin(pin: String) {
        repository.setPin(pin)
        _hasPin.value = true
        _unlocked.value = true
        _throttleSeconds.value = 0
    }

    /** Returns true on success (subject to current throttle). */
    fun tryUnlock(pin: String): UnlockResult {
        val wait = repository.throttleSecondsRemaining()
        if (wait > 0) {
            _throttleSeconds.value = wait
            return UnlockResult.Throttled(wait)
        }
        val ok = repository.verifyPin(pin)
        _throttleSeconds.value = repository.throttleSecondsRemaining()
        return if (ok) {
            _unlocked.value = true
            UnlockResult.Success
        } else {
            UnlockResult.Failed(repository.failureCount())
        }
    }

    /** Returns true if [pin] matches the stored hash; does NOT flip unlocked. */
    fun verifyCurrentPin(pin: String): Boolean = repository.verifyPin(pin)

    fun refreshThrottle() {
        _throttleSeconds.value = repository.throttleSecondsRemaining()
    }

    fun unlockViaBiometric() {
        _unlocked.value = true
    }

    fun lock() {
        _unlocked.value = false
    }

    fun resetPin() {
        repository.clearPin()
        _hasPin.value = false
        _unlocked.value = false
        _throttleSeconds.value = 0
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(repository) as T
    }
}

sealed interface UnlockResult {
    data object Success                  : UnlockResult
    data class Failed(val totalFails: Int): UnlockResult
    data class Throttled(val waitSec: Long): UnlockResult
}
