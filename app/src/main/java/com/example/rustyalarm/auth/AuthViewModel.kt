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

    fun biometricEnabled(): Boolean = repository.biometricEnabled

    fun setBiometricEnabled(value: Boolean) {
        repository.biometricEnabled = value
    }

    fun setPin(pin: String) {
        repository.setPin(pin)
        _hasPin.value = true
        _unlocked.value = true
    }

    fun tryUnlock(pin: String): Boolean {
        val ok = repository.verifyPin(pin)
        if (ok) _unlocked.value = true
        return ok
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
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(repository) as T
    }
}
