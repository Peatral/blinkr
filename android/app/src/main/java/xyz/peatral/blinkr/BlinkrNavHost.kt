package xyz.peatral.blinkr

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.peatral.blinkr.ui.SessionOverviewScreen

@Composable
fun BlinkrNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Overview) {
        composable<Overview> {
            SessionOverviewScreen()
        }
    }
}