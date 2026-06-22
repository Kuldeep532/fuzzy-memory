package com.nexuswavetech.nexusplus.features.emergencyguardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuardianUiState(
    val contacts:     List<EmergencyContact> = emptyList(),
    val isGuardActive: Boolean               = false,
    val nameInput:    String                 = "",
    val phoneInput:   String                 = "",
    val inputError:   String?                = null,
)

class EmergencyGuardianViewModel(
    private val repository: EmergencyGuardianRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GuardianUiState())
    val state: StateFlow<GuardianUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.contactsFlow.collect { contacts ->
                _state.update { it.copy(contacts = contacts) }
            }
        }
        _state.update { it.copy(isGuardActive = EmergencyGuardianService.isActive.get()) }
    }

    fun setNameInput(v: String)  { _state.update { it.copy(nameInput = v, inputError = null) } }
    fun setPhoneInput(v: String) { _state.update { it.copy(phoneInput = v, inputError = null) } }

    fun addContact() {
        val name  = _state.value.nameInput.trim()
        val phone = _state.value.phoneInput.trim()
        when {
            name.isEmpty()  -> { _state.update { it.copy(inputError = "Name cannot be empty") };  return }
            phone.isEmpty() -> { _state.update { it.copy(inputError = "Phone cannot be empty") }; return }
            phone.any { !it.isDigit() && it != '+' && it != '-' && it != ' ' } -> {
                _state.update { it.copy(inputError = "Invalid phone number") }; return
            }
        }
        viewModelScope.launch {
            repository.addContact(EmergencyContact(name = name, phone = phone))
            _state.update { it.copy(nameInput = "", phoneInput = "", inputError = null) }
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch { repository.deleteContact(id) }
    }

    fun setGuardActive(active: Boolean) {
        _state.update { it.copy(isGuardActive = active) }
    }

    fun syncGuardState() {
        _state.update { it.copy(isGuardActive = EmergencyGuardianService.isActive.get()) }
    }
}
