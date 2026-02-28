package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KprFlowTopAppBar(
    navController: NavController,
    userRole: UserRole,
    onNavigationClick: () -> Unit = {},
    showNotifications: Boolean = true,
    onNotificationsClick: () -> Unit = {},
    title: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val appBarTitle = title ?: getTitleForRoute(currentRoute, userRole)
    val showBackButton = shouldShowBackButton(currentRoute)
    
    TopAppBar(
        title = {
            Text(
                text = appBarTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
        },
        actions = {
            if (showNotifications && shouldShowNotifications(currentRoute)) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }
            
            if (shouldShowSettings(currentRoute)) {
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

private fun getTitleForRoute(route: String?, userRole: UserRole): String {
    return when (route) {
        // Customer routes
        Screen.CustomerDashboard.route -> "Customer Dashboard"
        Screen.CustomerDossierDetail.route -> "Dossier Details"
        Screen.CustomerDocumentUpload.route -> "Upload Documents"
        
        // Marketing routes
        Screen.MarketingDashboard.route -> "Marketing Dashboard"
        Screen.MarketingDossierList.route -> "Dossier List"
        Screen.MarketingDossierDetail.route -> "Dossier Details"
        Screen.UnitManagement.route -> "Unit Management"
        
        // Legal routes
        Screen.LegalDashboard.route -> "Legal Dashboard"
        Screen.LegalKanban.route -> "Document Kanban"
        Screen.LegalDocumentReview.route -> "Document Review"
        
        // Finance routes
        Screen.FinanceDashboard.route -> "Finance Dashboard"
        Screen.FinanceTransactions.route -> "Transactions"
        Screen.FinanceCashFlow.route -> "Cash Flow"
        Screen.FinanceTransactionDetail.route -> "Transaction Details"
        
        // Bank routes
        Screen.BankDashboard.route -> "Bank Dashboard"
        Screen.BankDecisionMatrix.route -> "Decision Matrix"
        Screen.BankUploadDecision.route -> "Upload Decision"
        
        // Technical routes
        Screen.TechnicalDashboard.route -> "Technical Dashboard"
        Screen.TechnicalVerification.route -> "Verification"
        
        // Estate routes
        Screen.EstateDashboard.route -> "Estate Dashboard"
        Screen.EstateQC.route -> "Quality Control"
        Screen.EstateBAST.route -> "BAST Management"
        
        // BOD routes
        Screen.BODDashboard.route -> "Executive Dashboard"
        Screen.BODAnalytics.route -> "Analytics"
        Screen.BODReports.route -> "Reports"
        
        // Common routes
        Screen.Profile.route -> "Profile"
        Screen.Settings.route -> "Settings"
        Screen.Notifications.route -> "Notifications"
        Screen.Help.route -> "Help & Support"
        
        else -> "KPRFlow Enterprise"
    }
}

private fun shouldShowBackButton(route: String?): Boolean {
    return when (route) {
        Screen.Splash.route,
        Screen.Login.route,
        Screen.Register.route,
        Screen.CustomerDashboard.route,
        Screen.MarketingDashboard.route,
        Screen.LegalDashboard.route,
        Screen.FinanceDashboard.route,
        Screen.BankDashboard.route,
        Screen.TechnicalDashboard.route,
        Screen.EstateDashboard.route,
        Screen.BODDashboard.route -> false
        
        else -> true
    }
}

private fun shouldShowNotifications(route: String?): Boolean {
    return route != Screen.Splash.route && 
           route != Screen.Login.route && 
           route != Screen.Register.route
}

private fun shouldShowSettings(route: String?): Boolean {
    return route != Screen.Splash.route && 
           route != Screen.Login.route && 
           route != Screen.Register.route &&
           route != Screen.Settings.route
}
