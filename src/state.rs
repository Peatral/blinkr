use pebble::std::time::get_time;
use pebble::types::{time_t, GlobalCell};
use pebble::{storage, vibes, wakeup};

pub const PERSIST_STATE_KEY: u32 = 1;
pub const PERSIST_INTERVAL_KEY: u32 = 2;
pub const CLAY_MESSAGE_KEY_INTERVAL: u32 = 10000;
pub const DEFAULT_INTERVAL_MINS: time_t = 20;

pub static IS_ENABLED: GlobalCell<bool> = GlobalCell::new(false);
pub static INTERVAL_MINS: GlobalCell<u32> = GlobalCell::new(DEFAULT_INTERVAL_MINS as u32);

pub fn init_state() {
    *IS_ENABLED.borrow_mut() = storage::read_bool(PERSIST_STATE_KEY);
    *INTERVAL_MINS.borrow_mut() = storage::read_int(PERSIST_INTERVAL_KEY) as u32;
}

pub fn toggle_state() {
    let mut is_enabled = IS_ENABLED.borrow_mut();
    *is_enabled = !*is_enabled;

    let _ = storage::write_bool(PERSIST_STATE_KEY, *is_enabled);

    let now = get_time();

    if *is_enabled {
        vibes::long_pulse();
        let interval = *INTERVAL_MINS.borrow();
        let _ = wakeup::schedule(now + (interval * 60), 0, true);
    } else {
        vibes::double_pulse();
        wakeup::cancel_all();
    }
}
