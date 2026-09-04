package xyz.peatral.blinkr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.ui.components.DayCard
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsByDaysAgo by viewModel.sessionsByDaysAgo.collectAsStateWithLifecycle()

    var currentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.minutes)
            currentTime = Clock.System.now()
        }
    }

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
                uiState.isWaiting -> {
                    stringResource(R.string.status_waiting) to MaterialTheme.colorScheme.onSurfaceVariant
                }
                uiState.isTimerRunning -> {
                    stringResource(R.string.status_timer_active, uiState.remainingTime.inWholeSeconds) to MaterialTheme.colorScheme.primary
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
