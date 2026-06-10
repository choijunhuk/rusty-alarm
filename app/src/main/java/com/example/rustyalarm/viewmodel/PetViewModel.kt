package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rustyalarm.pet.Pet
import com.example.rustyalarm.pet.PetDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(private val dao: PetDao) : ViewModel() {

    val pet: StateFlow<Pet?> = dao.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun rename(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { dao.rename(newName.trim()) }
    }

    fun setSkin(skinName: String) {
        viewModelScope.launch { dao.setSkin(skinName) }
    }

    class Factory(private val dao: PetDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PetViewModel(dao) as T
    }
}
