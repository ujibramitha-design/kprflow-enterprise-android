package com.kprflow.enterprise.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kprflow.enterprise.data.model.UserRole
import com.kprflow.enterprise.ui.navigation.Screen

@Composable
fun KprFlowBottomBar(
    navController: NavController,
    userRole: UserRole
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val bottomNavItems = getBottomNavItemsForRole(userRole)
    
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(painterResource(id = item.iconRes), contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val iconRes: Int
)

private fun getBottomNavItemsForRole(userRole: UserRole): List<BottomNavItem> {
    return when (userRole) {
        UserRole.CUSTOMER -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.CustomerDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Documents",
                route = Screen.CustomerDossierDetail.route, // This would need to be adjusted
                iconRes = android.R.drawable.ic_menu_agenda
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.MARKETING -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.MarketingDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Dossiers",
                route = Screen.MarketingDossierList.route,
                iconRes = android.R.drawable.ic_menu_agenda
            ),
            BottomNavItem(
                title = "Units",
                route = Screen.UnitManagement.route,
                iconRes = android.R.drawable.ic_menu_gallery
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.LEGAL -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.LegalDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Kanban",
                route = Screen.LegalKanban.route,
                iconRes = android.R.drawable.ic_menu_view
            ),
            BottomNavItem(
                title = "Review",
                route = Screen.LegalDocumentReview.route, // This would need to be adjusted
                iconRes = android.R.drawable.ic_menu_edit
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.FINANCE -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.FinanceDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Transactions",
                route = Screen.FinanceTransactions.route,
                iconRes = android.R.drawable.ic_menu_info_details
            ),
            BottomNavItem(
                title = "Cash Flow",
                route = Screen.FinanceCashFlow.route,
                iconRes = android.R.drawable.ic_menu_recent_history
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.BANK -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.BankDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Decisions",
                route = Screen.BankDecisionMatrix.route,
                iconRes = android.R.drawable.ic_menu_sort_by_size
            ),
            BottomNavItem(
                title = "Upload",
                route = Screen.BankUploadDecision.route, // This would need to be adjusted
                iconRes = android.R.drawable.ic_menu_upload
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.TEKNIK -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.TechnicalDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Verification",
                route = Screen.TechnicalVerification.route,
                iconRes = android.R.drawable.ic_menu_camera
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.ESTATE -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.EstateDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "QC",
                route = Screen.EstateQC.route,
                iconRes = android.R.drawable.ic_menu_preferences
            ),
            BottomNavItem(
                title = "BAST",
                route = Screen.EstateBAST.route,
                iconRes = android.R.drawable.ic_menu_save
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
        
        UserRole.BOD -> listOf(
            BottomNavItem(
                title = "Dashboard",
                route = Screen.BODDashboard.route,
                iconRes = android.R.drawable.ic_menu_dashboard
            ),
            BottomNavItem(
                title = "Analytics",
                route = Screen.BODAnalytics.route,
                iconRes = android.R.drawable.ic_menu_graph
            ),
            BottomNavItem(
                title = "Reports",
                route = Screen.BODReports.route,
                iconRes = android.R.drawable.ic_menu_call
            ),
            BottomNavItem(
                title = "Profile",
                route = Screen.Profile.route,
                iconRes = android.R.drawable.ic_menu_myplaces
            )
        )
    }
}
