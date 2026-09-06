package xyz.peatral.blinkr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.peatral.blinkr.R
import xyz.peatral.blinkr.ui.components.DayItem
import kotlin.time.Clock

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsByDaysAgo by viewModel.sessionsByDaysAgo.collectAsStateWithLifecycle()

    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle(Clock.System.now())

    val timer by viewModel.timerText.collectAsStateWithLifecycle("")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.overview)) }
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            HorizontalFloatingToolbar(
                true,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset)
                    .zIndex(1f),
                content = {
                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        if (timer.isBlank()) {
                            Text(stringResource(R.string.status_timer_stopped))
                        } else {
                            Text(
                                stringResource(R.string.status_timer_active, timer),
                                style = LocalTextStyle.current.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refreshSessions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        for (daysAgo in 0..<sessionsByDaysAgo.size) {
                            DayItem(
                                daysAgo = daysAgo,
                                daysCount = sessionsByDaysAgo.size,
                                sessions = sessionsByDaysAgo[daysAgo] ?: emptyList(),
                                currentTime = currentTime
                            )
                        }
                    }
                }
            }
        }
    }
}
