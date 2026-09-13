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
import xyz.peatral.blinkr.core.ui.LocalAnimatedVisibilityScope
import xyz.peatral.blinkr.feature.glyph.ui.GlyphSettingsScreen
import xyz.peatral.blinkr.feature.pebble.ui.PebbleSettingsScreen
import xyz.peatral.blinkr.feature.settings.ui.SettingsScreen
import xyz.peatral.blinkr.feature.timeline.ui.breakdown.DayBreakdownScreen
import xyz.peatral.blinkr.feature.timeline.ui.breakdown.DayBreakdownViewModel
import xyz.peatral.blinkr.feature.timeline.ui.overview.SessionOverviewScreen

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
                    },
                    onNavigateToSettings = { navController.navigate(Settings) },
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
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        composable<Settings> {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPebbleSettings = { navController.navigate(PebbleSettings) },
                    onNavigateToGlyphSettings = { navController.navigate(GlyphSettings) },
                )
            }
        }

        composable<PebbleSettings> {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                PebbleSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        composable<GlyphSettings> {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                GlyphSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
