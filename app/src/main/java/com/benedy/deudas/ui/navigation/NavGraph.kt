package com.benedy.deudas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.benedy.deudas.DeudasApp
import com.benedy.deudas.R
import com.benedy.deudas.ui.AddClientViewModelFactory
import com.benedy.deudas.ui.AddDebtViewModelFactory
import com.benedy.deudas.ui.AddProductViewModelFactory
import com.benedy.deudas.ui.ClientDetailViewModelFactory
import com.benedy.deudas.ui.ClientsListViewModelFactory
import com.benedy.deudas.ui.ProductsListViewModelFactory
import com.benedy.deudas.ui.ReceiptViewModelFactory
import com.benedy.deudas.ui.RegisterPaymentViewModelFactory
import com.benedy.deudas.ui.auth.AuthViewModel
import com.benedy.deudas.ui.backup.BackupViewModel
import com.benedy.deudas.ui.backup.BackupViewModelFactory
import com.benedy.deudas.ui.auth.LoginScreen
import com.benedy.deudas.ui.clientdetail.ClientDetailScreen
import com.benedy.deudas.ui.clients.AddClientScreen
import com.benedy.deudas.ui.clients.ClientsListScreen
import com.benedy.deudas.ui.debt.AddDebtScreen
import com.benedy.deudas.ui.home.HomeScreen
import com.benedy.deudas.ui.payment.RegisterPaymentScreen
import com.benedy.deudas.ui.products.AddProductScreen
import com.benedy.deudas.ui.products.ProductsListScreen
import com.benedy.deudas.ui.receipt.ReceiptScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ADD_CLIENT = "add_client"
    const val CLIENTS = "clients"
    const val SELECT_CLIENT_CHARGE = "select_client_charge"
    const val CLIENT_DETAIL = "client/{clientId}?charge={charge}"
    const val ADD_PRODUCT = "add_product"
    const val PRODUCTS = "products"
    const val ADD_DEBT = "client/{clientId}/add_debt"
    const val REGISTER_PAYMENT = "client/{clientId}/pay?debtId={debtId}"
    const val RECEIPT = "receipt/{paymentId}"

    fun clientDetail(clientId: Long, charge: Boolean = false) =
        "client/$clientId?charge=$charge"

    fun addDebt(clientId: Long) = "client/$clientId/add_debt"

    fun registerPayment(clientId: Long, debtId: Long? = null) =
        "client/$clientId/pay?debtId=${debtId ?: -1}"

    fun receipt(paymentId: Long) = "receipt/$paymentId"
}

@Composable
fun DeudasNavGraph(authViewModel: AuthViewModel) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as DeudasApp
    val crm = app.crmRepository

    val startDestination = if (uiState.isSignedIn) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = uiState,
                onContinueAsGuest = { authViewModel.continueAsGuest() },
                onGoogleSignIn = {
                    val activity = context as? android.app.Activity ?: context
                    authViewModel.signInWithGoogle(activity)
                },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Routes.HOME) {
            val backupVm = viewModel<BackupViewModel>(
                factory = BackupViewModelFactory(app.authRepository, crm)
            )
            HomeScreen(
                uiState = uiState,
                backupViewModel = backupVm,
                onLogout = { authViewModel.signOut() },
                onAddClient = { navController.navigate(Routes.ADD_CLIENT) },
                onCharge = { navController.navigate(Routes.SELECT_CLIENT_CHARGE) },
                onAddProduct = { navController.navigate(Routes.ADD_PRODUCT) },
                onViewClients = { navController.navigate(Routes.CLIENTS) },
                onViewProducts = { navController.navigate(Routes.PRODUCTS) }
            )
        }

        composable(Routes.ADD_CLIENT) {
            val vm = viewModel<com.benedy.deudas.ui.clients.AddClientViewModel>(
                factory = AddClientViewModelFactory(crm)
            )
            AddClientScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    navController.navigate(Routes.clientDetail(id)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.CLIENTS) {
            val vm = viewModel<com.benedy.deudas.ui.clients.ClientsListViewModel>(
                factory = ClientsListViewModelFactory(crm)
            )
            ClientsListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAddClient = { navController.navigate(Routes.ADD_CLIENT) },
                onClientClick = { id -> navController.navigate(Routes.clientDetail(id)) }
            )
        }

        composable(Routes.SELECT_CLIENT_CHARGE) {
            val vm = viewModel<com.benedy.deudas.ui.clients.ClientsListViewModel>(
                key = "select_charge",
                factory = ClientsListViewModelFactory(crm)
            )
            ClientsListScreen(
                viewModel = vm,
                title = stringResource(R.string.select_client_title),
                subtitle = stringResource(R.string.select_client_for_charge),
                onBack = { navController.popBackStack() },
                onAddClient = { navController.navigate(Routes.ADD_CLIENT) },
                onClientClick = { id ->
                    navController.navigate(Routes.clientDetail(id, charge = true))
                }
            )
        }

        composable(
            route = Routes.CLIENT_DETAIL,
            arguments = listOf(
                navArgument("clientId") { type = NavType.LongType },
                navArgument("charge") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val clientId = entry.arguments!!.getLong("clientId")
            val charge = entry.arguments!!.getBoolean("charge")
            val vm = viewModel<com.benedy.deudas.ui.clientdetail.ClientDetailViewModel>(
                key = "client_$clientId",
                factory = ClientDetailViewModelFactory(crm, clientId)
            )
            ClientDetailScreen(
                viewModel = vm,
                chargeMode = charge,
                onBack = { navController.popBackStack() },
                onAddDebt = { navController.navigate(Routes.addDebt(clientId)) },
                onRegisterPayment = { debtId ->
                    navController.navigate(Routes.registerPayment(clientId, debtId))
                }
            )
        }

        composable(Routes.ADD_PRODUCT) {
            val vm = viewModel<com.benedy.deudas.ui.products.AddProductViewModel>(
                factory = AddProductViewModelFactory(crm)
            )
            AddProductScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.navigate(Routes.PRODUCTS) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.PRODUCTS) {
            val vm = viewModel<com.benedy.deudas.ui.products.ProductsListViewModel>(
                factory = ProductsListViewModelFactory(crm)
            )
            ProductsListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Routes.ADD_PRODUCT) }
            )
        }

        composable(
            route = Routes.ADD_DEBT,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { entry ->
            val clientId = entry.arguments!!.getLong("clientId")
            val vm = viewModel<com.benedy.deudas.ui.debt.AddDebtViewModel>(
                key = "add_debt_$clientId",
                factory = AddDebtViewModelFactory(crm, clientId)
            )
            AddDebtScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.REGISTER_PAYMENT,
            arguments = listOf(
                navArgument("clientId") { type = NavType.LongType },
                navArgument("debtId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val clientId = entry.arguments!!.getLong("clientId")
            val debtIdArg = entry.arguments!!.getLong("debtId")
            val preselected = debtIdArg.takeIf { it > 0 }
            val vm = viewModel<com.benedy.deudas.ui.payment.RegisterPaymentViewModel>(
                key = "pay_${clientId}_$debtIdArg",
                factory = RegisterPaymentViewModelFactory(crm, clientId, preselected)
            )
            RegisterPaymentScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onPaid = { paymentId ->
                    navController.navigate(Routes.receipt(paymentId)) {
                        popUpTo(Routes.clientDetail(clientId, charge = true))
                    }
                }
            )
        }

        composable(
            route = Routes.RECEIPT,
            arguments = listOf(navArgument("paymentId") { type = NavType.LongType })
        ) { entry ->
            val paymentId = entry.arguments!!.getLong("paymentId")
            val vm = viewModel<com.benedy.deudas.ui.receipt.ReceiptViewModel>(
                key = "receipt_$paymentId",
                factory = ReceiptViewModelFactory(crm, paymentId)
            )
            ReceiptScreen(
                viewModel = vm,
                onDone = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }

    LaunchedEffect(uiState.isSignedIn) {
        val target = if (uiState.isSignedIn) Routes.HOME else Routes.LOGIN
        val current = navController.currentDestination?.route
        if (current != null && current != target &&
            ((uiState.isSignedIn && current == Routes.LOGIN) ||
                (!uiState.isSignedIn && current != Routes.LOGIN))
        ) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
