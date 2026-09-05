package xyz.peatral.blinkr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.ui.components.DayCard
import kotlin.time.Clock

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsByDaysAgo by viewModel.sessionsByDaysAgo.collectAsStateWithLifecycle()

    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle(Clock.System.now())

    val timer by viewModel.timerText.collectAsStateWithLifecycle("")

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Status Indicator
            val (statusText, statusColor) = when {
                !timer.isBlank() -> {
                    stringResource(R.string.status_timer_active, timer) to MaterialTheme.colorScheme.primary
                }
                else -> {
                    stringResource(R.string.status_timer_stopped) to MaterialTheme.colorScheme.onSurfaceVariant
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(64.dp))

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshSessions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (daysAgo in 0..sessionsByDaysAgo.size) {
                        DayCard(
                            daysAgo = daysAgo,
                            sessions = sessionsByDaysAgo[daysAgo] ?: emptyList(),
                            currentTime = currentTime
                        )
                    }
                }
            }
        }
    }
}
