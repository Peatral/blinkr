use crate::message_queue::{push_message, Message};
use crate::state;

pub fn start_sync() {
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
