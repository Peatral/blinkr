use alloc::vec::Vec;
use core::slice;
use pebble::std::time::get_time;
use pebble::types::{GlobalCell, GlobalRefCell};
use pebble::{storage, vibes, wakeup};
use pebble_sys::time_t;

pub const PERSIST_STATE_KEY: u32 = 1;
pub const PERSIST_INTERVAL_KEY: u32 = 2;
pub const PERSIST_HISTORY_KEY: u32 = 3;
pub const PERSIST_CURRENT_START_KEY: u32 = 4;

pub const DEFAULT_INTERVAL_MINS: time_t = 20;

pub const MAX_HISTORY_PAIRS: usize = 4096 / size_of::<TimePair>();

#[derive(Debug, Clone, Copy)]
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

pub fn init_state() {
    IS_ENABLED.set(storage::read_bool(PERSIST_STATE_KEY));
    INTERVAL_MINS.set(storage::read_int(PERSIST_INTERVAL_KEY) as time_t);

    if storage::exists(PERSIST_HISTORY_KEY) {
        if let Ok(size) = storage::get_size(PERSIST_HISTORY_KEY) {
            let count = size / size_of::<TimePair>();

            let mut history = alloc::vec![TimePair { start: 0, end: 0 }; count];

            let byte_slice =
                unsafe { slice::from_raw_parts_mut(history.as_mut_ptr() as *mut u8, size) };

            if storage::read_data(PERSIST_HISTORY_KEY, byte_slice).is_ok() {
                let current_time = get_time();
                history.retain(|pair| {
                    pair.start > 0 &&
                    pair.end >= pair.start &&
                    pair.end <= current_time + DAY_SECONDS
                });

                *HISTORY.borrow_mut() = history;
            }
        }
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

            let byte_slice = unsafe {
                slice::from_raw_parts(
                    history.as_ptr() as *const u8,
                    history.len() * size_of::<TimePair>(),
                )
            };
            let _ = storage::write_data(PERSIST_HISTORY_KEY, byte_slice);
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

                let byte_slice = unsafe {
                    slice::from_raw_parts(
                        history.as_ptr() as *const u8,
                        history.len() * size_of::<TimePair>(),
                    )
                };
                let _ = storage::write_data(PERSIST_HISTORY_KEY, byte_slice);
            }
        }

        CURRENT_START_TIME.set(None);
        let _ = storage::delete(PERSIST_CURRENT_START_KEY);

        vibes::double_pulse();
        wakeup::cancel_all();
    }
}
