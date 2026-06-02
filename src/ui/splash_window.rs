use core::cell::RefCell;
use pebble::layer::{ILayer, TextLayer};
use pebble::types::{AppLaunchReason, GPoint, GRect, GSize, GTextAlignment};
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble::timer::AppTimer;
use pebble::system::fonts::{FONT_KEY_BITHAM_42_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_28_BOLD};
use pebble::{launch, vibes, window_stack};
use crate::state;
use core::sync::atomic::Ordering;

pub struct SplashDelegate {
    text_main: RefCell<Option<TextLayer>>,
    text_sub: RefCell<Option<TextLayer>>,
    exit_timer: RefCell<Option<AppTimer>>,
}

impl WindowDelegate for SplashDelegate {
    fn load(&self, window: WindowRef) {
        let root = window.get_root_layer();
        let bounds = root.get_bounds();
        let width = bounds.size.w;
        let height = bounds.size.h;

        let reason = launch::get_reason();
        let is_enabled = state::IS_ENABLED.load(Ordering::Relaxed);

        if reason == AppLaunchReason::Wakeup {
            let text_main = TextLayer::new(GRect {
                origin: GPoint { x: 0, y: 30 },
                size: GSize { w: width, h: 50 },
            });
            text_main.set_text(c"Blink");
            text_main.set_font(pebble::system::fonts::Font::get_system(FONT_KEY_BITHAM_42_BOLD));
            text_main.set_text_alignment(GTextAlignment::Center);
            root.add_child(&text_main);

            let text_sub = TextLayer::new(GRect {
                origin: GPoint { x: 5, y: 100 },
                size: GSize { w: width - 10, h: 60 },
            });
            text_sub.set_text(c"20-20-20 Rule:\nLook 20ft away\nfor 20 seconds.");
            text_sub.set_font(pebble::system::fonts::Font::get_system(FONT_KEY_GOTHIC_18_BOLD));
            text_sub.set_text_alignment(GTextAlignment::Center);
            root.add_child(&text_sub);

            *self.text_main.borrow_mut() = Some(text_main);
            *self.text_sub.borrow_mut() = Some(text_sub);
            *self.exit_timer.borrow_mut() = Some(AppTimer::register(20_000, handle_exit_timer));

        } else if reason == AppLaunchReason::QuickLaunch {
            let text = TextLayer::new(GRect {
                origin: GPoint { x: 0, y: height / 2 - 20 },
                size: GSize { w: width, h: 40 },
            });
            text.set_text(if is_enabled { c"Active" } else { c"Inactive" });
            text.set_font(pebble::system::fonts::Font::get_system(FONT_KEY_GOTHIC_28_BOLD));
            text.set_text_alignment(GTextAlignment::Center);
            root.add_child(&text);

            *self.text_main.borrow_mut() = Some(text);
            *self.exit_timer.borrow_mut() = Some(AppTimer::register(20_000, handle_exit_timer));
        }
    }

    fn unload(&self, _window: WindowRef) {
        self.text_main.borrow_mut().take();
        self.text_sub.borrow_mut().take();
        self.exit_timer.borrow_mut().take();
    }
}

fn handle_exit_timer() {
    if launch::get_reason() == AppLaunchReason::Wakeup {
        vibes::short_pulse();
    }
    window_stack::pop_all(false);
}

pub fn create() -> Window<SplashDelegate> {
    Window::new(SplashDelegate {
        text_main: RefCell::new(None),
        text_sub: RefCell::new(None),
        exit_timer: RefCell::new(None),
    })
}