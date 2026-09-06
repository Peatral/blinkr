use crate::message_queue::{push_message, Message};
use crate::message_queue::Message::{RescheduleWakeup, StartSession};
use crate::state;

pub fn start_sync() {
    start_state_sync();
    start_history_sync();
}

pub fn start_state_sync() {
    if !state::IS_ENABLED.get() {
        return;
    }

    if let Some(start_time) = state::CURRENT_START_TIME.get() {
        push_message(StartSession {
            start_timestamp: start_time,
        });
    }
    if let Some(wakeup_id) = state::CURRENT_WAKEUP_ID.get() {
        if let Some(wakeup_time) = pebble::wakeup::query(wakeup_id) {
            let timer_start = wakeup_time - state::INTERVAL_MINS.get() * 60;
            push_message(RescheduleWakeup {
                start_timestamp: timer_start,
                end_timestamp: wakeup_time,
            });
        }
    }
}

pub fn start_history_sync() {
    let history = state::HISTORY.borrow();
    if history.is_empty() {
        return;
    }

    let chunks = history.chunks(state::PAIRS_PER_CHUNK);
    let total_chunks = chunks.len() as i32;

    push_message(Message::SyncStart { total_chunks });

    for i in 0..total_chunks as usize {
        push_message(Message::SyncChunk {
            chunk_index: i,
            is_last: i == (total_chunks - 1) as usize
        });
    }
}
