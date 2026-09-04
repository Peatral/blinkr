use crate::message_keys::{MESSAGE_KEY_MSG_TYPE, MESSAGE_KEY_SYNC_DATA_CHUNK, MESSAGE_KEY_SYNC_TOTAL_CHUNKS};
use crate::state;
use pebble::app_message::{Dictionary, Outbox};
use pebble::types::GlobalCell;
use crate::message_types::{MSG_TYPE_SYNC_CHUNK, MSG_TYPE_SYNC_START};

static SYNC_IN_PROGRESS: GlobalCell<bool> = GlobalCell::new(false);
static CURRENT_CHUNK_INDEX: GlobalCell<usize> = GlobalCell::new(0);

pub fn start_sync() {
    if SYNC_IN_PROGRESS.get() {
        return;
    }

    let history = state::HISTORY.borrow();
    if history.is_empty() {
        return;
    }

    let chunks = history.chunks(state::PAIRS_PER_CHUNK);
    let total_chunks = chunks.len() as u32;

    SYNC_IN_PROGRESS.set(true);
    CURRENT_CHUNK_INDEX.set(0);

    if let Ok(dict) = Outbox::begin() {
        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_SYNC_START);
        let _ = dict.write_int(MESSAGE_KEY_SYNC_TOTAL_CHUNKS, total_chunks as i32);
        let _ = Outbox::send();
    }
}

pub fn send_next_chunk() {
    if !SYNC_IN_PROGRESS.get() {
        return;
    }

    let history = state::HISTORY.borrow();
    let chunks: alloc::vec::Vec<&[state::TimePair]> =
        history.chunks(state::PAIRS_PER_CHUNK).collect();
    let current_index = CURRENT_CHUNK_INDEX.get();

    if current_index < chunks.len() {
        let chunk = chunks[current_index];
        let bytes = bytemuck::cast_slice(chunk);

        if let Ok(dict) = Outbox::begin() {
            let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_SYNC_CHUNK);
            let _ = dict.write_data(MESSAGE_KEY_SYNC_DATA_CHUNK, bytes);

            if Outbox::send().is_ok() {
                CURRENT_CHUNK_INDEX.set(current_index + 1);
            }
        }
    } else {
        SYNC_IN_PROGRESS.set(false);
        pebble::vibes::double_pulse();
    }
}

pub fn abort_sync() {
    if SYNC_IN_PROGRESS.get() {
        SYNC_IN_PROGRESS.set(false);
    }
}

pub fn outbox_sent_handler(_dictionary: Dictionary) {
    send_next_chunk();
}

pub fn outbox_failed_handler(_dictionary: Dictionary, _error: pebble_sys::AppMessageResult) {
    abort_sync();
}
