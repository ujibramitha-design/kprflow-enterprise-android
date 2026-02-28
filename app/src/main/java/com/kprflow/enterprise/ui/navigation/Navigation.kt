package com.kprflow.enterprise.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    
    // Customer Routes
    object CustomerDashboard : Screen("customer_dashboard")
    object CustomerDossierDetail : Screen("customer_dossier_detail/{dossierId}") {
        fun createRoute(dossierId: String) = "customer_dossier_detail/$dossierId"
    }
    object CustomerDocumentUpload : Screen("customer_document_upload/{dossierId}") {
        fun createRoute(dossierId: String) = "customer_document_upload/$dossierId"
    }
    
    // Marketing Routes
    object MarketingDashboard : Screen("marketing_dashboard")
    object MarketingDossierList : Screen("marketing_dossier_list")
    object MarketingDossierDetail : Screen("marketing_dossier_detail/{dossierId}") {
        fun createRoute(dossierId: String) = "marketing_dossier_detail/$dossierId"
    }
    object UnitManagement : Screen("unit_management")
    
    // Legal Routes
    object LegalDashboard : Screen("legal_dashboard")
    object LegalKanban : Screen("legal_kanban")
    object LegalDocumentReview : Screen("legal_document_review/{dossierId}") {
        fun createRoute(dossierId: String) = "legal_document_review/$dossierId"
    }
    
    // Finance Routes
    object FinanceDashboard : Screen("finance_dashboard")
    object FinanceTransactions : Screen("finance_transactions")
    object FinanceCashFlow : Screen("finance_cash_flow")
    object FinanceTransactionDetail : Screen("finance_transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "finance_transaction_detail/$transactionId"
    }
    
    // Bank Routes
    object BankDashboard : Screen("bank_dashboard")
    object BankDecisionMatrix : Screen("bank_decision_matrix")
    object BankUploadDecision : Screen("bank_upload_decision/{dossierId}") {
        fun createRoute(dossierId: String) = "bank_upload_decision/$dossierId"
    }
    
    // Technical Routes
    object TechnicalDashboard : Screen("technical_dashboard")
    object TechnicalVerification : Screen("technical_verification")
    
    // Estate Routes
    object EstateDashboard : Screen("estate_dashboard")
    object EstateQC : Screen("estate_qc")
    object EstateBAST : Screen("estate_bast")
    
    // BOD Routes
    object BODDashboard : Screen("bod_dashboard")
    object BODAnalytics : Screen("bod_analytics")
    object BODReports : Screen("bod_reports")
    
    // Common Routes
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object Help : Screen("help")
}

// Navigation graphs for each role
object CustomerNavGraph {
    const val ROOT_ROUTE = "customer_nav_graph"
    val startDestination = Screen.CustomerDashboard.route
}

object MarketingNavGraph {
    const val ROOT_ROUTE = "marketing_nav_graph"
    val startDestination = Screen.MarketingDashboard.route
}

object LegalNavGraph {
    const val ROOT_ROUTE = "legal_nav_graph"
    val startDestination = Screen.LegalDashboard.route
}

object FinanceNavGraph {
    const val ROOT_ROUTE = "finance_nav_graph"
    val startDestination = Screen.FinanceDashboard.route
}

object BankNavGraph {
    const val ROOT_ROUTE = "bank_nav_graph"
    val startDestination = Screen.BankDashboard.route
}

object TechnicalNavGraph {
    const val ROOT_ROUTE = "technical_nav_graph"
    val startDestination = Screen.TechnicalDashboard.route
}

object EstateNavGraph {
    const val ROOT_ROUTE = "estate_nav_graph"
    val startDestination = Screen.EstateDashboard.route
}

object BODNavGraph {
    const val ROOT_ROUTE = "bod_nav_graph"
    val startDestination = Screen.BODDashboard.route
}
