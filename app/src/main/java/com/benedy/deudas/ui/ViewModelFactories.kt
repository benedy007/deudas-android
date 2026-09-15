package com.benedy.deudas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.benedy.deudas.data.repository.DebtCrmRepository
import com.benedy.deudas.ui.clientdetail.ClientDetailViewModel
import com.benedy.deudas.ui.clients.AddClientViewModel
import com.benedy.deudas.ui.clients.ClientsListViewModel
import com.benedy.deudas.ui.debt.AddDebtViewModel
import com.benedy.deudas.ui.payment.RegisterPaymentViewModel
import com.benedy.deudas.ui.products.AddProductViewModel
import com.benedy.deudas.ui.products.ProductsListViewModel
import com.benedy.deudas.ui.receipt.ReceiptViewModel

class ClientsListViewModelFactory(
    private val repo: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ClientsListViewModel(repo) as T
}

class AddClientViewModelFactory(
    private val repo: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AddClientViewModel(repo) as T
}

class ClientDetailViewModelFactory(
    private val repo: DebtCrmRepository,
    private val clientId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ClientDetailViewModel(repo, clientId) as T
}

class ProductsListViewModelFactory(
    private val repo: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProductsListViewModel(repo) as T
}

class AddProductViewModelFactory(
    private val repo: DebtCrmRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AddProductViewModel(repo) as T
}

class AddDebtViewModelFactory(
    private val repo: DebtCrmRepository,
    private val clientId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AddDebtViewModel(repo, clientId) as T
}

class RegisterPaymentViewModelFactory(
    private val repo: DebtCrmRepository,
    private val clientId: Long,
    private val preselectedDebtId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RegisterPaymentViewModel(repo, clientId, preselectedDebtId) as T
}

class ReceiptViewModelFactory(
    private val repo: DebtCrmRepository,
    private val paymentId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReceiptViewModel(repo, paymentId) as T
}
