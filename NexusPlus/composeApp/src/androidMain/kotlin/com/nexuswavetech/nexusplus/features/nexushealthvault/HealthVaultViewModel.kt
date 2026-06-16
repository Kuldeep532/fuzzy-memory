package com.nexuswavetech.nexusplus.features.nexushealthvault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HealthVaultViewModel(private val repository: HealthVaultRepository) : ViewModel() {

    val records: StateFlow<List<HealthRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addRecord(
        category:   String,
        label:      String,
        value:      String,
        unit:       String,
        recordedOn: String,
    ) = viewModelScope.launch {
        repository.addRecord(
            HealthRecord(
                id         = UUID.randomUUID().toString(),
                category   = category,
                label      = label,
                value      = value,
                unit       = unit,
                recordedOn = recordedOn,
            )
        )
    }

    fun deleteRecord(id: String) = viewModelScope.launch { repository.deleteRecord(id) }
}
