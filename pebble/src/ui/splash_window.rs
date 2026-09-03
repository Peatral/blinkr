use crate::state::toggle_state;
use crate::ui::history_window;
use crate::window_manager::{AppWindow, release};
use crate::{state, window_manager};
use core::cell::RefCell;
use pebble::clicks::{ClickConfigurator, ClickDelegate, ClickRecognizer};
use pebble::graphics::types::{Point, Rect, Size};
use pebble::layer::{ILayer, ILayerMut, TextLayer};
use pebble::system::fonts::FONT_KEY_GOTHIC_28_BOLD;
use pebble::timer::AppTimer;
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble::window_stack;
use pebble_sys::{ButtonId, GTextAlignment};

pub struct SplashScreen {
    text_main: RefCell<Option<TextLayer>>,
    exit_timer: RefCell<Option<AppTimer>>,
}

impl ClickDelegate for SplashScreen {
    fn click_config(&self, config: &ClickConfigurator<Self>) {
        config.subscribe_single_click(ButtonId::BUTTON_ID_SELECT);
    }

    fn on_single_click(&self, _recognizer: ClickRecognizer) {
        // There probably won't be any case where toggle state has been called since the window has been opened
        // So reverting the state change like this should be safe
        toggle_state();
        window_manager::replace_top(history_window::create(), true);
    }
}

impl WindowDelegate for SplashScreen {
    fn load(&self, window: WindowRef) {
        let root = window.get_root_layer();
        let bounds = root.get_bounds();
        let width = bounds.size.w;
        let height = bounds.size.h;

        let is_enabled = state::IS_ENABLED.get();

        let text = TextLayer::new(Rect::new(
            Point::new(0, height / 2 - 20),
            Size::new(width, 40),
        ));
        text.set_text_static(if is_enabled { c"Active" } else { c"Inactive" });
        text.set_font(pebble::system::fonts::Font::get_system(
            FONT_KEY_GOTHIC_28_BOLD,
        ));
        text.set_text_alignment(GTextAlignment::GTextAlignmentCenter);
        root.add_child(&text);

        *self.text_main.borrow_mut() = Some(text);
        *self.exit_timer.borrow_mut() = Some(AppTimer::register(3_000, move || {
            window_stack::remove(window, false);
        }));
    }

    fn unload(&self, window: WindowRef) {
        self.text_main.borrow_mut().take();
        self.exit_timer.borrow_mut().take();
        release(window);
    }
}

pub fn create() -> AppWindow {
    toggle_state();

    let window = Window::new(SplashScreen {
        text_main: RefCell::new(None),
        exit_timer: RefCell::new(None),
    });
    window.enable_clicks();
    AppWindow::Splash(window)
}
