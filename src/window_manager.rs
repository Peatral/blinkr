use crate::ui::history_window::HistoryScreen;
use crate::ui::reminder_window::ReminderScreen;
use crate::ui::settings_window::SettingsScreen;
use crate::ui::splash_window::SplashScreen;
use alloc::vec::Vec;
use core::mem::discriminant;
use pebble::types::GlobalRefCell;
use pebble::window::{Window, WindowRef};
use pebble::window_stack;
use pebble_sys::{TimeUnits, tm};

pub enum AppWindow {
    Splash(Window<SplashScreen>),
    Reminder(Window<ReminderScreen>),
    Settings(Window<SettingsScreen>),
    History(Window<HistoryScreen>),
}

impl AppWindow {
    fn as_window_ref(&self) -> WindowRef {
        match self {
            AppWindow::Splash(w) => **w,
            AppWindow::Reminder(w) => **w,
            AppWindow::Settings(w) => **w,
            AppWindow::History(w) => **w,
        }
    }

    pub fn on_tick(&self, _tick_time: &tm, _units_changed: TimeUnits) {
    }
}

static APP_STACK: GlobalRefCell<Vec<AppWindow>> = GlobalRefCell::new(Vec::new());

pub fn push(window: AppWindow, animated: bool) {
    let duplicate_window_ref = {
        let stack = APP_STACK.borrow();
        let target_discriminant = discriminant(&window);

        stack
            .iter()
            .find(|w| discriminant(*w) == target_discriminant)
            .map(|w| w.as_window_ref())
    };

    if let Some(old_win_ref) = duplicate_window_ref {
        window_stack::remove(old_win_ref, false);
    }

    window_stack::push(window.as_window_ref(), animated);
    APP_STACK.borrow_mut().push(window);
}

pub fn replace_top(window: AppWindow, animated: bool) {
    if let Some(old_win) = APP_STACK.borrow().last() {
        window_stack::remove(old_win.as_window_ref(), false);
    }

    push(window, animated);
}

pub fn release(target_window: WindowRef) {
    let mut stack = APP_STACK.borrow_mut();

    if let Some(index) = stack
        .iter()
        .position(|w| w.as_window_ref().as_ptr() == target_window.as_ptr())
    {
        stack.remove(index);
    }
}

pub fn deinit() {
    let mut stack = APP_STACK.borrow_mut();
    stack.clear();
    stack.shrink_to_fit();
}

pub fn notify_tick(tick_time: &tm, units_changed: TimeUnits) {
    if let Some(active_window) = APP_STACK.borrow().last() {
        active_window.on_tick(tick_time, units_changed);
    }
}
