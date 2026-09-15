package com.benedy.deudas.ui.clientdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ClientDetailViewModel(
    repo: DebtCrmRepository,
    clientId: Long
) : ViewModel() {
    val client: StateFlow<ClientEntity?> = repo.observeClient(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val debts: StateFlow<List<DebtEntity>> = repo.observeDebts(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalRemaining: StateFlow<Double> = repo.observeTotalRemaining(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
}
