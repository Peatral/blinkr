#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

extern crate alloc;
extern crate pebble_rs as pebble;

mod state;
mod ui;
mod utils;
pub mod window_manager;

use pebble::event::tick_timer;
use pebble::{app, app_message::AppMessage, include_message_keys, launch, wakeup};
use pebble_sys::{AppLaunchReason, TimeUnits};

include_message_keys!();

#[unsafe(no_mangle)]
pub fn main() -> isize {
    let launch = launch::get_reason();

    state::init_state();

    if launch == AppLaunchReason::APP_LAUNCH_WAKEUP && !state::IS_ENABLED.get() {
        pebble::pbl_warn!(c"Ghost wakeup detected. Aborting.");
        return 0;
    }

    AppMessage::register_inbox_received(ui::settings_window::inbox_received_handler);
    if AppMessage::open(128, 128).is_err() {
        pebble::pbl_err!(c"Failed to open AppMessage subsystem!");
    }

    let app = app::App::new();

    let active_window = match launch {
        AppLaunchReason::APP_LAUNCH_QUICK_LAUNCH => ui::splash_window::create(),
        AppLaunchReason::APP_LAUNCH_WAKEUP => ui::reminder_window::create(),
        _ => ui::history_window::create(),
    };

    window_manager::push(active_window, false);

    wakeup::subscribe(|_id, _cookie| {
        window_manager::push(ui::reminder_window::create(), false);
    });

    tick_timer::subscribe(TimeUnits::MINUTE_UNIT, |tm, time_units| {
        window_manager::notify_tick(tm, time_units);
    });

    app.run_event_loop();

    wakeup::unsubscribe();
    tick_timer::unsubscribe();

    window_manager::deinit();
    state::deinit_state();

    0
}
