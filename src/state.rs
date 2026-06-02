use core::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use pebble::std::time::get_time;
use pebble::{storage, vibes, wakeup};
use pebble::types::time_t;
pub const PERSIST_STATE_KEY: u32 = 1;
pub const PERSIST_INTERVAL_KEY: u32 = 2;
pub const CLAY_MESSAGE_KEY_INTERVAL: u32 = 10000;
pub const DEFAULT_INTERVAL_MINS: time_t = 20;

pub static IS_ENABLED: AtomicBool = AtomicBool::new(false);
pub static INTERVAL_MINS: AtomicU32 = AtomicU32::new(DEFAULT_INTERVAL_MINS);

pub fn init_state() {
    let is_enabled = storage::read_bool(PERSIST_STATE_KEY);
    IS_ENABLED.store(is_enabled, Ordering::Relaxed);

    let interval = storage::read_int(PERSIST_INTERVAL_KEY) as u32;
    INTERVAL_MINS.store(interval, Ordering::Relaxed);
}

pub fn toggle_state() {
    let mut is_enabled = IS_ENABLED.load(Ordering::Relaxed);
    is_enabled = !is_enabled;
    IS_ENABLED.store(is_enabled, Ordering::Relaxed);
    let _ = storage::write_bool(PERSIST_STATE_KEY, is_enabled);

    let now = get_time();

    if is_enabled {
        vibes::long_pulse();
        let interval = INTERVAL_MINS.load(Ordering::Relaxed);
        let _ = wakeup::schedule(now + (interval * 60), 0, true);
    } else {
        vibes::double_pulse();
        wakeup::cancel_all();
    }
}

pub fn handle_launch(launch: pebble::types::AppLaunchReason) {
    let interval = INTERVAL_MINS.load(Ordering::Relaxed);
    if launch == pebble::types::AppLaunchReason::Wakeup {
        vibes::double_pulse();
        let now = get_time();
        let _ = wakeup::schedule(now + (interval * 60), 0, true);
    } else if launch == pebble::types::AppLaunchReason::QuickLaunch {
        toggle_state();
    }
}