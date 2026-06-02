#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

extern crate pebble_rust as pebble;

mod state;
mod ui;
mod utils;

use crate::ui::reminder_window::ReminderDelegate;
use crate::ui::settings_window::SettingsDelegate;
use crate::ui::splash_window::SplashDelegate;
use core::sync::atomic::Ordering;
use pebble::types::AppLaunchReason;
use pebble::window::{Window, WindowRef};
use pebble::{app, app_message::AppMessage, launch, window_stack};

enum AppWindow {
    Splash(Window<SplashDelegate>),
    Settings(Window<SettingsDelegate>),
    Reminder(Window<ReminderDelegate>),
}

impl AppWindow {
    fn as_window_ref(&self) -> WindowRef {
        match self {
            AppWindow::Splash(w) => w.as_ref(),
            AppWindow::Settings(w) => w.as_ref(),
            AppWindow::Reminder(w) => w.as_ref(),
        }
    }
}

#[no_mangle]
pub fn main() -> isize {
    let launch = launch::get_reason();

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

    let active_window = if launch == AppLaunchReason::QuickLaunch {
        AppWindow::Splash(ui::splash_window::create())
    } else if launch == AppLaunchReason::Wakeup {
        AppWindow::Reminder(ui::reminder_window::create())
    } else {
        AppWindow::Settings(ui::settings_window::create())
    };

    window_stack::push(active_window.as_window_ref(), false);

    app.run_event_loop();

    0
}
