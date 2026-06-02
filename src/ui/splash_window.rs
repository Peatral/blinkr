use crate::state;
use core::cell::RefCell;
use core::sync::atomic::Ordering;
use pebble::layer::{ILayer, TextLayer};
use pebble::system::fonts::FONT_KEY_GOTHIC_28_BOLD;
use pebble::timer::AppTimer;
use pebble::types::{GPoint, GRect, GSize, GTextAlignment};
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble::window_stack;

pub struct SplashDelegate {
    text_main: RefCell<Option<TextLayer>>,
    exit_timer: RefCell<Option<AppTimer>>,
}

impl WindowDelegate for SplashDelegate {
    fn load(&self, window: WindowRef) {
        let root = window.get_root_layer();
        let bounds = root.get_bounds();
        let width = bounds.size.w;
        let height = bounds.size.h;

        let is_enabled = state::IS_ENABLED.load(Ordering::Relaxed);

        let text = TextLayer::new(GRect {
            origin: GPoint {
                x: 0,
                y: height / 2 - 20,
            },
            size: GSize { w: width, h: 40 },
        });
        text.set_text(if is_enabled { c"Active" } else { c"Inactive" });
        text.set_font(pebble::system::fonts::Font::get_system(
            FONT_KEY_GOTHIC_28_BOLD,
        ));
        text.set_text_alignment(GTextAlignment::Center);
        root.add_child(&text);

        *self.text_main.borrow_mut() = Some(text);
        *self.exit_timer.borrow_mut() = Some(AppTimer::register(3_000, move || {
            window_stack::remove(window, false);
        }));
    }

    fn unload(&self, _window: WindowRef) {
        self.text_main.borrow_mut().take();
        self.exit_timer.borrow_mut().take();
    }
}

pub fn create() -> Window<SplashDelegate> {
    Window::new(SplashDelegate {
        text_main: RefCell::new(None),
        exit_timer: RefCell::new(None),
    })
}
