#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

extern crate alloc;
extern crate pebble_rs as pebble;

mod state;
mod ui;
mod utils;

use crate::ui::history_window::HistoryScreen;
use crate::ui::reminder_window::ReminderDelegate;
use crate::ui::splash_window::SplashDelegate;
use pebble::types::GlobalRefCell;
use pebble::window::{Window, WindowRef};
use pebble::{app, app_message::AppMessage, include_message_keys, launch, wakeup, window_stack};
use pebble_sys::AppLaunchReason;

include_message_keys!();

enum AppWindow {
    Splash(Window<SplashDelegate>),
    Reminder(Window<ReminderDelegate>),
    History(Window<HistoryScreen>),
}

impl AppWindow {
    fn as_window_ref(&self) -> WindowRef {
        match self {
            AppWindow::Splash(w) => **w,
            AppWindow::Reminder(w) => **w,
            AppWindow::History(w) => **w,
        }
    }
}

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
        AppLaunchReason::APP_LAUNCH_QUICK_LAUNCH => AppWindow::Splash(ui::splash_window::create()),
        AppLaunchReason::APP_LAUNCH_WAKEUP => AppWindow::Reminder(ui::reminder_window::create()),
        _ => AppWindow::History(ui::history_window::create()),
    };

    window_stack::push(active_window.as_window_ref(), false);

    static REMINDER_WINDOW: GlobalRefCell<Option<AppWindow>> = GlobalRefCell::new(None);

    wakeup::subscribe(|_id, _cookie| {
        let mut window_state = REMINDER_WINDOW.borrow_mut();

        if let Some(AppWindow::Reminder(old_win)) = window_state.take() {
            window_stack::remove(*old_win, false);
        }

        *window_state = Some(AppWindow::Reminder(ui::reminder_window::create()));

        if let Some(AppWindow::Reminder(ref new_win)) = *window_state {
            window_stack::push(**new_win, false);
        }
    });

    app.run_event_loop();

    REMINDER_WINDOW.borrow_mut().take();

    state::deinit_state();

    0
}
