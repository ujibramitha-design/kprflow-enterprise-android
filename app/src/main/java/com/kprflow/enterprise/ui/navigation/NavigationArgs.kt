package com.kprflow.enterprise.ui.navigation

import android.net.Uri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

// Navigation argument keys
object NavArgs {
    const val DOSSIER_ID = "dossierId"
    const val TRANSACTION_ID = "transactionId"
    const val USER_ID = "userId"
    const val UNIT_ID = "unitId"
    const val DOCUMENT_ID = "documentId"
    const val DOCUMENT_TYPE = "documentType"
}

// Navigation argument definitions
object NavArguments {
    val dossierIdArg = navArgument(NavArgs.DOSSIER_ID) {
        type = NavType.StringType
        nullable = false
    }
    
    val transactionIdArg = navArgument(NavArgs.TRANSACTION_ID) {
        type = NavType.StringType
        nullable = false
    }
    
    val userIdArg = navArgument(NavArgs.USER_ID) {
        type = NavType.StringType
        nullable = false
    }
    
    val unitIdArg = navArgument(NavArgs.UNIT_ID) {
        type = NavType.StringType
        nullable = false
    }
    
    val documentIdArg = navArgument(NavArgs.DOCUMENT_ID) {
        type = NavType.StringType
        nullable = false
    }
    
    val documentTypeArg = navArgument(NavArgs.DOCUMENT_TYPE) {
        type = NavType.StringType
        nullable = false
    }
}

// Extension functions for safe argument extraction
fun NavBackStackEntry.getDossierId(): String? {
    return arguments?.getString(NavArgs.DOSSIER_ID)
}

fun NavBackStackEntry.getTransactionId(): String? {
    return arguments?.getString(NavArgs.TRANSACTION_ID)
}

fun NavBackStackEntry.getUserId(): String? {
    return arguments?.getString(NavArgs.USER_ID)
}

fun NavBackStackEntry.getUnitId(): String? {
    return arguments?.getString(NavArgs.UNIT_ID)
}

fun NavBackStackEntry.getDocumentId(): String? {
    return arguments?.getString(NavArgs.DOCUMENT_ID)
}

fun NavBackStackEntry.getDocumentType(): String? {
    return arguments?.getString(NavArgs.DOCUMENT_TYPE)
}

// Deep link configurations
object DeepLinks {
    const val DOSSIER_DETAIL_URI = "kprflow://dossier/{dossierId}"
    const val UNIT_DETAIL_URI = "kprflow://unit/{unitId}"
    const val DOCUMENT_REVIEW_URI = "kprflow://document/{documentId}/review"
    
    fun createDossierDeepLink(dossierId: String): Uri {
        return Uri.parse("kprflow://dossier/$dossierId")
    }
    
    fun createUnitDeepLink(unitId: String): Uri {
        return Uri.parse("kprflow://unit/$unitId")
    }
    
    fun createDocumentReviewDeepLink(documentId: String): Uri {
        return Uri.parse("kprflow://document/$documentId/review")
    }
}
