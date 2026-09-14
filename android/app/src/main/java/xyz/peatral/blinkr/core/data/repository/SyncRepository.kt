package xyz.peatral.blinkr.core.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncState {
    object Idle : SyncState
    class Syncing(progress: Float) : SyncState
}

@Singleton
class SyncRepository @Inject constructor(
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _syncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncRequests = _syncRequests.asSharedFlow()

    fun requestSync() {
        _syncRequests.tryEmit(Unit)
    }

    fun updateSyncState(state: SyncState) {
        _syncState.value = state
    }
}