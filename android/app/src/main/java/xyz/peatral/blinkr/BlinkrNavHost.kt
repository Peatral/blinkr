package xyz.peatral.blinkr

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.datetime.LocalDate
import xyz.peatral.blinkr.ui.DayBreakdownScreen
import xyz.peatral.blinkr.ui.DayBreakdownViewModel
import xyz.peatral.blinkr.ui.LocalAnimatedVisibilityScope
import xyz.peatral.blinkr.ui.SessionOverviewScreen

@Composable
fun BlinkrNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Overview,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        composable<Overview> {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                SessionOverviewScreen(
                    onDayClick = { date ->
                        navController.navigate(DayBreakdown(dateString = date.toString()))
                    }
                )
            }
        }

        composable<DayBreakdown> { backStackEntry ->
            val route = backStackEntry.toRoute<DayBreakdown>()
            val targetDate = LocalDate.parse(route.dateString)

            val viewModel = hiltViewModel(
                creationCallback = { factory: DayBreakdownViewModel.Factory ->
                    factory.create(targetDate)
                }
            )

            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                DayBreakdownScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
