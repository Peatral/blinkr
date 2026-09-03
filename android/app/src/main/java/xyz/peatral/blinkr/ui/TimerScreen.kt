package xyz.peatral.blinkr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import xyz.peatral.blinkr.data.room.SessionEntity
import xyz.peatral.blinkr.ui.components.SessionTimeline
import java.util.Calendar

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsByDaysAgo by viewModel.sessionsByDaysAgo.collectAsStateWithLifecycle()

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Allow scrolling for 7 days
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
                    stringResource(R.string.status_timer_active, uiState.remainingSeconds) to MaterialTheme.colorScheme.primary
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

            for (daysAgo in 0..sessionsByDaysAgo.size) {
                DayCard(
                    daysAgo = daysAgo,
                    sessions = sessionsByDaysAgo[daysAgo] ?: emptyList(),
                    currentTimeMillis = currentTimeMillis
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DayCard(
    daysAgo: Int,
    sessions: List<SessionEntity>,
    currentTimeMillis: Long
) {
    val title = when (daysAgo) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.yesterday)
        else -> stringResource(R.string.n_days_ago, daysAgo)
    }

    val (startMillis, endMillis) = remember(daysAgo, currentTimeMillis) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        val start = todayStart - (daysAgo * 24 * 60 * 60 * 1000L)
        val end = start + (24 * 60 * 60 * 1000L)

        start to end
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            SessionTimeline(
                sessions = sessions,
                startMillis = startMillis,
                endMillis = endMillis,
                currentTimeMillis = currentTimeMillis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}