#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

extern crate pebble_rust as pebble;

mod state;
mod utils;
mod ui;

use pebble::{app, app_message::AppMessage, launch, window_stack};
use pebble::types::AppLaunchReason;
use core::sync::atomic::Ordering;

#[no_mangle]
pub fn main() -> isize {
    let launch = launch::get_reason();
    state::LAUNCH_REASON.store(launch as u32 as usize, Ordering::Relaxed);

    state::init_state();

    if launch == AppLaunchReason::Wakeup && !state::IS_ENABLED.load(Ordering::Relaxed) {
        pebble::pbl_warn!(c"Ghost wakeup detected. Aborting.");
        return 0;
    }

    state::handle_launch(launch);

    AppMessage::register_inbox_received(ui::settings_window::inbox_received_handler);
    if AppMessage::open(128, 128).is_err() {
        pebble::pbl_err!(c"Failed to open AppMessage subsystem!");
    }

    let app = app::App::new();

    let mut _active_splash = None;
    let mut _active_settings = None;
    if launch == AppLaunchReason::Wakeup || launch == AppLaunchReason::QuickLaunch {
        _active_splash = Some(ui::splash_window::create());
        window_stack::push(&_active_splash.as_ref().unwrap(), false);
    } else {
        _active_settings = Some(ui::settings_window::create());
        window_stack::push(_active_settings.as_ref().unwrap(), false);
    }

    app.run_event_loop();

    0
}