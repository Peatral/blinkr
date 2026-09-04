use crate::message_queue::{push_message, Message};
use crate::state;
use pebble::types::GlobalCell;

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
    let total_chunks = chunks.len() as i32;

    SYNC_IN_PROGRESS.set(true);
    CURRENT_CHUNK_INDEX.set(0);

    push_message(Message::SyncStart { total_chunks });

    for i in 0..total_chunks as usize {
        push_message(Message::SyncChunk { chunk_index: i });
    }

    pebble::vibes::double_pulse();
}
