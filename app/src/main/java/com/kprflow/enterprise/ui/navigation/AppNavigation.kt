package com.kprflow.enterprise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.ui.screens.SplashScreen
import com.kprflow.enterprise.ui.screens.LoginScreen
import com.kprflow.enterprise.ui.screens.CrashTestScreen
import com.kprflow.enterprise.ui.viewmodel.AuthViewModel
import com.kprflow.enterprise.ui.viewmodel.SplashViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    splashViewModel: SplashViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val splashState by splashViewModel.splashState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigationComplete = { destination ->
                    when (destination) {
                        is SplashDestination.Login -> navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        is SplashDestination.Dashboard -> {
                            navigateToRoleDashboard(navController, destination.userRole)
                        }
                    }
                }
            )
        }
        
        // Authentication Flow
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigation will be handled by AuthViewModel state change
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            // TODO: Implement RegisterScreen
        }
        
        // Role-based Navigation Graphs
        customerNavigationGraph(navController)
        marketingNavigationGraph(navController)
        legalNavigationGraph(navController)
        financeNavigationGraph(navController)
        bankNavigationGraph(navController)
        technicalNavigationGraph(navController)
        estateNavigationGraph(navController)
        bodNavigationGraph(navController)
        
        // Common Screens (accessible from all roles)
        composable(Screen.Profile.route) {
            // TODO: Implement ProfileScreen
        }
        
        composable(Screen.Settings.route) {
            // TODO: Implement SettingsScreen
        }
        
        composable(Screen.Notifications.route) {
            // TODO: Implement NotificationsScreen
        }
        
        composable(Screen.Help.route) {
            // TODO: Implement HelpScreen
        }
    }
}

private fun navigateToRoleDashboard(navController: NavController, userRole: UserRole) {
    val route = when (userRole) {
        UserRole.CUSTOMER -> CustomerNavGraph.ROOT_ROUTE
        UserRole.MARKETING -> MarketingNavGraph.ROOT_ROUTE
        UserRole.LEGAL -> LegalNavGraph.ROOT_ROUTE
        UserRole.FINANCE -> FinanceNavGraph.ROOT_ROUTE
        UserRole.BANK -> BankNavGraph.ROOT_ROUTE
        UserRole.TEKNIK -> TechnicalNavGraph.ROOT_ROUTE
        UserRole.ESTATE -> EstateNavGraph.ROOT_ROUTE
        UserRole.BOD -> BODNavGraph.ROOT_ROUTE
    }
    
    navController.navigate(route) {
        popUpTo(Screen.Splash.route) { inclusive = true }
        launchSingleTop = true
    }
}

// Customer Navigation Graph
private fun NavGraphBuilder.customerNavigationGraph(navController: NavController) {
    navigation(
        startDestination = CustomerNavGraph.startDestination,
        route = CustomerNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.CustomerDashboard.route) {
            CustomerDashboard(
                onDossierClick = { dossierId ->
                    navController.navigate(Screen.CustomerDossierDetail.createRoute(dossierId))
                },
                onDocumentUploadClick = { dossierId ->
                    navController.navigate(Screen.CustomerDocumentUpload.createRoute(dossierId))
                }
            )
        }
        
        composable(
            route = Screen.CustomerDossierDetail.route,
            arguments = listOf(NavArguments.dossierIdArg)
        ) { backStackEntry ->
            val dossierId = backStackEntry.getDossierId()
            // TODO: Implement CustomerDossierDetail
        }
        
        composable(
            route = Screen.CustomerDocumentUpload.route,
            arguments = listOf(NavArguments.dossierIdArg)
        ) { backStackEntry ->
            val dossierId = backStackEntry.getDossierId()
            // TODO: Implement CustomerDocumentUpload
        }
    }
}

// Marketing Navigation Graph
private fun NavGraphBuilder.marketingNavigationGraph(navController: NavController) {
    navigation(
        startDestination = MarketingNavGraph.startDestination,
        route = MarketingNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.MarketingDashboard.route) {
            // TODO: Implement MarketingDashboard
        }
        
        composable(Screen.MarketingDossierList.route) {
            // TODO: Implement MarketingDossierList
        }
        
        composable(
            route = Screen.MarketingDossierDetail.route,
            arguments = listOf(NavArguments.dossierIdArg)
        ) { backStackEntry ->
            val dossierId = backStackEntry.getDossierId()
            // TODO: Implement MarketingDossierDetail
        }
        
        composable(Screen.UnitManagement.route) {
            // TODO: Implement UnitManagement
        }
    }
}

// Legal Navigation Graph
private fun NavGraphBuilder.legalNavigationGraph(navController: NavController) {
    navigation(
        startDestination = LegalNavGraph.startDestination,
        route = LegalNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.LegalDashboard.route) {
            // TODO: Implement LegalDashboard
        }
        
        composable(Screen.LegalKanban.route) {
            LegalKanban(
                onDossierClick = { dossierId ->
                    navController.navigate(Screen.LegalDocumentReview.createRoute(dossierId))
                }
            )
        }
        
        composable(
            route = Screen.LegalDocumentReview.route,
            arguments = listOf(NavArguments.dossierIdArg)
        ) { backStackEntry ->
            val dossierId = backStackEntry.getDossierId()
            // TODO: Implement LegalDocumentReview
        }
    }
}

// Finance Navigation Graph
private fun NavGraphBuilder.financeNavigationGraph(navController: NavController) {
    navigation(
        startDestination = FinanceNavGraph.startDestination,
        route = FinanceNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.FinanceDashboard.route) {
            // TODO: Implement FinanceDashboard
        }
        
        composable(Screen.FinanceTransactions.route) {
            // TODO: Implement FinanceTransactions
        }
        
        composable(Screen.FinanceCashFlow.route) {
            // TODO: Implement FinanceCashFlow
        }
        
        composable(
            route = Screen.FinanceTransactionDetail.route,
            arguments = listOf(NavArguments.transactionIdArg)
        ) { backStackEntry ->
            val transactionId = backStackEntry.getTransactionId()
            // TODO: Implement FinanceTransactionDetail
        }
    }
}

// Bank Navigation Graph
private fun NavGraphBuilder.bankNavigationGraph(navController: NavController) {
    navigation(
        startDestination = BankNavGraph.startDestination,
        route = BankNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.BankDashboard.route) {
            // TODO: Implement BankDashboard
        }
        
        composable(Screen.BankDecisionMatrix.route) {
            // TODO: Implement BankDecisionMatrix
        }
        
        composable(
            route = Screen.BankUploadDecision.route,
            arguments = listOf(NavArguments.dossierIdArg)
        ) { backStackEntry ->
            val dossierId = backStackEntry.getDossierId()
            // TODO: Implement BankUploadDecision
        }
    }
}

// Technical Navigation Graph
private fun NavGraphBuilder.technicalNavigationGraph(navController: NavController) {
    navigation(
        startDestination = TechnicalNavGraph.startDestination,
        route = TechnicalNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.TechnicalDashboard.route) {
            // TODO: Implement TechnicalDashboard
        }
        
        composable(Screen.TechnicalVerification.route) {
            // TODO: Implement TechnicalVerification
        }
    }
}

// Estate Navigation Graph
private fun NavGraphBuilder.estateNavigationGraph(navController: NavController) {
    navigation(
        startDestination = EstateNavGraph.startDestination,
        route = EstateNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.EstateDashboard.route) {
            // TODO: Implement EstateDashboard
        }
        
        composable(Screen.EstateQC.route) {
            // TODO: Implement EstateQC
        }
        
        composable(Screen.EstateBAST.route) {
            // TODO: Implement EstateBAST
        }
    }
}

// BOD Navigation Graph
private fun NavGraphBuilder.bodNavigationGraph(navController: NavController) {
    navigation(
        startDestination = BODNavGraph.startDestination,
        route = BODNavGraph.ROOT_ROUTE
    ) {
        composable(Screen.BODDashboard.route) {
            // TODO: Implement BODDashboard
        }
        
        composable(Screen.BODAnalytics.route) {
            // TODO: Implement BODAnalytics
        }
        
        composable(Screen.BODReports.route) {
            // TODO: Implement BODReports
        }
        
        // Crash Test Screen (for testing Firebase Crashlytics)
        composable(CRASH_TEST_ROUTE) {
            CrashTestScreen()
        }
    }
}

// Standalone Crash Test Screen (accessible from any role)
private fun NavGraphBuilder.crashTestScreen() {
    composable(CRASH_TEST_ROUTE) {
        CrashTestScreen()
    }
}
