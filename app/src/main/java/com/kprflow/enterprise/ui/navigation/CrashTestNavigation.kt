package com.kprflow.enterprise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kprflow.enterprise.ui.screens.CrashTestScreen

/**
 * Navigation for Crash Test Screen
 * Adds the crash test screen to the navigation graph
 */

const val CRASH_TEST_ROUTE = "crash_test"

/**
 * Add crash test screen to navigation graph
 */
fun NavGraphBuilder.crashTestGraph() {
    composable(CRASH_TEST_ROUTE) {
        CrashTestScreen()
    }
}

/**
 * Navigate to crash test screen
 */
fun NavController.navigateToCrashTest() {
    navigate(CRASH_TEST_ROUTE)
}
