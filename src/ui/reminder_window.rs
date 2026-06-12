use crate::state::INTERVAL_MINS;
use crate::utils::reschedule_wakeup;
use crate::window_manager::{AppWindow, release};
use core::cell::RefCell;
use pebble::graphics::types::{Point, Rect, Size};
use pebble::layer::{ILayer, ILayerMut, TextLayer};
use pebble::system::fonts::{FONT_KEY_BITHAM_42_BOLD, FONT_KEY_GOTHIC_18_BOLD};
use pebble::timer::AppTimer;
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble::{vibes, window_stack};
use pebble_sys::GTextAlignment;

pub struct ReminderScreen {
    text_main: RefCell<Option<TextLayer>>,
    text_sub: RefCell<Option<TextLayer>>,
    exit_timer: RefCell<Option<AppTimer>>,
}

impl WindowDelegate for ReminderScreen {
    fn load(&self, window: WindowRef) {
        let root = window.get_root_layer();
        let bounds = root.get_bounds();
        let width = bounds.size.w;

        let text_main = TextLayer::new(Rect::new(Point::new(0, 30), Size::new(width, 50)));
        text_main.set_text_static(c"Blink");
        text_main.set_font(pebble::system::fonts::Font::get_system(
            FONT_KEY_BITHAM_42_BOLD,
        ));
        text_main.set_text_alignment(GTextAlignment::GTextAlignmentCenter);
        root.add_child(&text_main);

        let text_sub = TextLayer::new(Rect::new(Point::new(5, 100), Size::new(width - 10, 60)));
        text_sub.set_text_static(c"20-20-20 Rule:\nLook 20ft away\nfor 20 seconds.");
        text_sub.set_font(pebble::system::fonts::Font::get_system(
            FONT_KEY_GOTHIC_18_BOLD,
        ));
        text_sub.set_text_alignment(GTextAlignment::GTextAlignmentCenter);
        root.add_child(&text_sub);

        *self.text_main.borrow_mut() = Some(text_main);
        *self.text_sub.borrow_mut() = Some(text_sub);
        *self.exit_timer.borrow_mut() = Some(AppTimer::register(20_000, move || {
            vibes::short_pulse();
            window_stack::remove(window, false);
        }));
    }

    fn unload(&self, window: WindowRef) {
        self.text_main.borrow_mut().take();
        self.text_sub.borrow_mut().take();
        self.exit_timer.borrow_mut().take();
        release(window);
    }
}

pub fn create() -> AppWindow {
    vibes::double_pulse();
    let interval = INTERVAL_MINS.get();
    let _ = reschedule_wakeup(interval);

    AppWindow::Reminder(Window::new(ReminderScreen {
        text_main: RefCell::new(None),
        text_sub: RefCell::new(None),
        exit_timer: RefCell::new(None),
    }))
}
