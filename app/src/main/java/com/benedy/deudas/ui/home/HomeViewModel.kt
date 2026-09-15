package com.benedy.deudas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benedy.deudas.data.repository.DebtCrmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    repo: DebtCrmRepository
) : ViewModel() {
    val dashboard: StateFlow<DebtCrmRepository.DashboardStats> =
        repo.observeDashboardStats()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DebtCrmRepository.DashboardStats()
            )
}
