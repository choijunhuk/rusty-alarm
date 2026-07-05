package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.rustyalarm.pet.Pet
import com.example.rustyalarm.pet.PetDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PetViewModel @Inject constructor(private val dao: PetDao) : ViewModel() {

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

    /**
     * Tries to feed the pet. Returns null on success or a Korean reason
     * string when feeding is rate-limited (last meal too recent).
     */
    suspend fun feed(): String? {
        val current = dao.get() ?: return "펫을 찾을 수 없어요"
        val gap = System.currentTimeMillis() - current.lastFedAt
        val cooldown = 60L * 60 * 1000L   // 1 hour
        if (gap < cooldown) {
            val mins = ((cooldown - gap) / 60_000L).toInt().coerceAtLeast(1)
            return "${mins}분 뒤에 다시 줄 수 있어요"
        }
        dao.addExp(8)
        return null
    }

}
