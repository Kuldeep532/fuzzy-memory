package com.nexuswavetech.nexusplus.features.encryptednotes

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val isUnlocked: Boolean             = false,
    val notes:      List<EncryptedNote> = emptyList(),
    val authError:  String?             = null,
    val query:      String              = "",
)

class EncryptedNotesViewModel(
    private val repository: EncryptedNotesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    private val appBackgroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) { lock() }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appBackgroundObserver)
        viewModelScope.launch {
            repository.notesFlow.collect { notes ->
                _state.update { it.copy(notes = notes) }
            }
        }
    }

    fun onUnlockSuccess() { _state.update { it.copy(isUnlocked = true, authError = null) } }
    fun onAuthError(msg: String) { _state.update { it.copy(authError = msg) } }
    fun lock() { _state.update { it.copy(isUnlocked = false, authError = null) } }
    fun setQuery(q: String) { _state.update { it.copy(query = q) } }

    fun addOrUpdateNote(note: EncryptedNote) = viewModelScope.launch {
        val current = _state.value.notes
        val updated = if (current.any { it.id == note.id }) {
            current.map { if (it.id == note.id) note.copy(updatedAt = System.currentTimeMillis()) else it }
        } else {
            current + note
        }
        repository.saveNotes(updated)
    }

    fun deleteNote(id: String) = viewModelScope.launch {
        repository.saveNotes(_state.value.notes.filter { it.id != id })
    }

    val filteredNotes: List<EncryptedNote>
        get() {
            val q = _state.value.query.trim().lowercase()
            return if (q.isEmpty()) _state.value.notes.sortedByDescending { it.updatedAt }
            else _state.value.notes.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) }
                .sortedByDescending { it.updatedAt }
        }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appBackgroundObserver)
        _state.update { it.copy(isUnlocked = false) }
        super.onCleared()
    }
}
