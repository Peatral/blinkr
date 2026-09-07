package xyz.peatral.blinkr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.Pager
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import xyz.peatral.blinkr.ui.DayRecord
import kotlin.time.Instant

@Composable
fun DayList(
    pager: Pager<Int, DayRecord>,
    currentTime: Instant,
) {
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        items(
            lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.daysAgo }
        ) { daysAgo ->
            DayItem(
                daysAgo = daysAgo,
                daysCount = lazyPagingItems.itemCount,
                sessions = lazyPagingItems[daysAgo]?.sessions ?: emptyList(),
                currentTime = currentTime
            )
        }
    }
}