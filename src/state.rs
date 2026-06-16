use alloc::vec::Vec;
use bytemuck::{Pod, Zeroable};
use core::mem::size_of;
use pebble::std::time::get_time;
use pebble::types::{GlobalCell, GlobalRefCell};
use pebble::{storage, vibes, wakeup};
use pebble_sys::time_t;

pub const PERSIST_STATE_KEY: u32 = 1;
pub const PERSIST_INTERVAL_KEY: u32 = 2;
pub const PERSIST_CURRENT_START_KEY: u32 = 4;
pub const PERSIST_HISTORY_COUNT_KEY: u32 = 5;

pub const DEFAULT_INTERVAL_MINS: time_t = 20;

pub const PERSIST_HISTORY_BASE_KEY: u32 = 100; // Chunks saved to 100, 101, 102...
pub const PERSIST_DATA_MAX_LENGTH: usize = 256;
pub const PAIRS_PER_CHUNK: usize = PERSIST_DATA_MAX_LENGTH / size_of::<TimePair>();
pub const MAX_HISTORY_PAIRS: usize = 4096 / size_of::<TimePair>();

#[derive(Debug, Clone, Copy, Pod, Zeroable)]
#[repr(C)]
pub struct TimePair {
    pub start: time_t,
    pub end: time_t,
}

pub static IS_ENABLED: GlobalCell<bool> = GlobalCell::new(false);
pub static INTERVAL_MINS: GlobalCell<time_t> = GlobalCell::new(DEFAULT_INTERVAL_MINS as time_t);

pub static HISTORY: GlobalRefCell<Vec<TimePair>> = GlobalRefCell::new(Vec::new());
pub static CURRENT_START_TIME: GlobalCell<Option<time_t>> = GlobalCell::new(None);

const DAY_SECONDS: i32 = 60 * 60 * 24;

/// Helper function to chunk the history array and save it to multiple keys
fn save_history(history: &[TimePair]) {
    let chunks = history.chunks(PAIRS_PER_CHUNK);
    let chunk_count = chunks.len() as u32;

    for (i, chunk) in chunks.enumerate() {
        let chunk_key = PERSIST_HISTORY_BASE_KEY + i as u32;
        let byte_slice = bytemuck::cast_slice(chunk);
        let _ = storage::write_data(chunk_key, byte_slice);
    }

    let _ = storage::write_int(PERSIST_HISTORY_COUNT_KEY, chunk_count as i32);
}

/// Helper function to load the history array from multiple keys
fn load_history(current_time: time_t) -> Vec<TimePair> {
    if !storage::exists(PERSIST_HISTORY_COUNT_KEY) {
        return Vec::new();
    }

    let chunk_count = storage::read_int(PERSIST_HISTORY_COUNT_KEY) as u32;
    let mut full_history = Vec::new();

    for i in 0..chunk_count {
        let chunk_key = PERSIST_HISTORY_BASE_KEY + i;

        if let Ok(size) = storage::get_size(chunk_key) {
            let count = size / size_of::<TimePair>();
            let mut chunk = alloc::vec![TimePair { start: 0, end: 0 }; count];
            let byte_slice = bytemuck::cast_slice_mut(&mut chunk);
            if storage::read_data(chunk_key, byte_slice).is_ok() {
                full_history.extend(chunk);
            }
        }
    }

    full_history.retain(|pair| {
        pair.start > 0 && pair.end >= pair.start && pair.end <= current_time + DAY_SECONDS
    });

    full_history.sort_unstable_by_key(|pair| pair.start);

    if !full_history.is_empty() {
        let mut write_idx = 0;
        for read_idx in 1..full_history.len() {
            let current = full_history[read_idx];

            if current.start < full_history[write_idx].end + 60 {
                if current.end > full_history[write_idx].end {
                    full_history[write_idx].end = current.end;
                }
            } else {
                write_idx += 1;
                full_history[write_idx] = current;
            }
        }
        full_history.truncate(write_idx + 1);
    }
    full_history.retain(|session| session.end - session.start >= 60);

    full_history
}

pub fn init_state() {
    IS_ENABLED.set(storage::read_bool(PERSIST_STATE_KEY));
    INTERVAL_MINS.set(storage::read_int(PERSIST_INTERVAL_KEY) as time_t);

    let current_time = get_time();

    let loaded_history = load_history(current_time);
    if !loaded_history.is_empty() {
        *HISTORY.borrow_mut() = loaded_history;
    }

    if IS_ENABLED.get() {
        if storage::exists(PERSIST_CURRENT_START_KEY) {
            let st = storage::read_int(PERSIST_CURRENT_START_KEY) as time_t;
            CURRENT_START_TIME.set(Some(st));
        }
    } else {
        if storage::exists(PERSIST_CURRENT_START_KEY) {
            let _ = storage::delete(PERSIST_CURRENT_START_KEY);
            CURRENT_START_TIME.set(None);
        }
    }
}

pub fn deinit_state() {
    let mut history = HISTORY.borrow_mut();
    *history = Vec::new();
    history.shrink_to_fit();

    CURRENT_START_TIME.set(None);
}

pub fn toggle_state() {
    let mut is_enabled = IS_ENABLED.get();
    is_enabled = !is_enabled;
    IS_ENABLED.set(is_enabled);

    let _ = storage::write_bool(PERSIST_STATE_KEY, is_enabled);
    let now = get_time();

    if is_enabled {
        let mut start_time = now;

        let mut history = HISTORY.borrow_mut();

        // Resume previous session if it was less than 60 seconds ago
        let resume_start = history.last().and_then(|last| {
            let diff = now - last.end;
            if diff >= 0 && diff < 60 {
                Some(last.start)
            } else {
                None
            }
        });

        if let Some(st) = resume_start {
            start_time = st;
            history.pop();
            save_history(&history);
        }

        CURRENT_START_TIME.set(Some(start_time));
        let _ = storage::write_int(PERSIST_CURRENT_START_KEY, start_time as i32);

        vibes::long_pulse();
        let interval = INTERVAL_MINS.get();
        let _ = wakeup::schedule(now + (interval as time_t * 60), 0, true);
    } else {
        if let Some(start) = CURRENT_START_TIME.get() {
            // Only save the session if it lasted 60 seconds or more
            if now >= start && (now - start) >= 60 {
                let mut history = HISTORY.borrow_mut();
                history.push(TimePair { start, end: now });

                while history.len() > MAX_HISTORY_PAIRS {
                    history.remove(0);
                }

                save_history(&history);
            }
        }

        CURRENT_START_TIME.set(None);
        let _ = storage::delete(PERSIST_CURRENT_START_KEY);

        vibes::double_pulse();
        wakeup::cancel_all();
    }
}
