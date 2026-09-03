use crate::window_manager::release;
use crate::{resources, window_manager};
use core::cell::RefCell;
use core::ffi::CStr;
use pebble::clicks::{ClickConfigurator, ClickDelegate, ClickRecognizer};
use pebble::graphics::bitmap::Bitmap;
use pebble::graphics::types::{Color, Point, Rect, Size};
use pebble::layer::{ActionBarLayer, ILayer, ILayerMut, TextLayer};
use pebble::system::fonts::{FONT_KEY_GOTHIC_24_BOLD, Font};
use pebble::window::{WindowDelegate, WindowRef};
use pebble_sys::{ACTION_BAR_WIDTH, ButtonId, GTextAlignment, GTextOverflowMode};

struct ConfirmationState {
    action_bar: ActionBarLayer<ConfirmClicks>,
    _text_layer: TextLayer,
    _icon_check: Bitmap,
    _icon_cross: Bitmap,
}

struct ConfirmClicks {
    on_confirm: fn(),
}

impl ClickDelegate for ConfirmClicks {
    fn click_config(&self, config: &ClickConfigurator<Self>) {
        config.subscribe_single_click(ButtonId::BUTTON_ID_UP);
        config.subscribe_single_click(ButtonId::BUTTON_ID_DOWN);
    }

    fn on_single_click(&self, recognizer: ClickRecognizer) {
        match recognizer.get_button_id() {
            ButtonId::BUTTON_ID_UP => {
                (self.on_confirm)();
                window_manager::pop(true);
            }
            ButtonId::BUTTON_ID_DOWN => {
                window_manager::pop(true);
            }
            _ => {}
        }
    }
}

pub struct ConfirmationScreen {
    text: &'static CStr,
    on_confirm: fn(),
    state: RefCell<Option<ConfirmationState>>,
}

impl ConfirmationScreen {
    /// Create a new Confirmation screen.
    /// `text`: The prompt to show the user.
    /// `on_confirm`: A simple function pointer to call when 'Up' is pressed.
    pub fn new(text: &'static CStr, on_confirm: fn()) -> Self {
        Self {
            text,
            on_confirm,
            state: RefCell::new(None),
        }
    }
}

impl WindowDelegate for ConfirmationScreen {
    fn load(&self, window: WindowRef) {
        let bounds = window.get_root_layer().get_bounds();
        let safe_width = bounds.size.w - ACTION_BAR_WIDTH;

        let icon_check = Bitmap::new(resources::RESOURCE_ID_ICON_CHECK);
        let icon_cross = Bitmap::new(resources::RESOURCE_ID_ICON_DISMISS);

        let action_bar = ActionBarLayer::new(ConfirmClicks {
            on_confirm: self.on_confirm,
        });
        action_bar.set_icon(ButtonId::BUTTON_ID_UP, icon_check.as_ref());
        action_bar.set_icon(ButtonId::BUTTON_ID_DOWN, icon_cross.as_ref());

        action_bar.add_to_window(&window);

        let text_layer = TextLayer::new(Rect::new(
            Point::new(0, 0),
            Size::new(safe_width, bounds.size.h),
        ));

        text_layer.set_text_static(self.text);
        text_layer.set_font(Font::get_system(FONT_KEY_GOTHIC_24_BOLD));
        text_layer.set_text_alignment(GTextAlignment::GTextAlignmentCenter);
        text_layer.set_overflow_mode(GTextOverflowMode::GTextOverflowModeWordWrap);
        text_layer.set_background_color(Color::CLEAR);
        text_layer.set_text_color(Color::BLACK);

        let content_size = text_layer.get_content_size();

        let y_offset = (bounds.size.h - content_size.h) / 2;

        let centered_frame = Rect::new(
            Point::new(0, y_offset),
            Size::new(safe_width, content_size.h),
        );
        text_layer.set_frame(centered_frame);

        window.get_root_layer().add_child(&text_layer);

        *self.state.borrow_mut() = Some(ConfirmationState {
            action_bar,
            _text_layer: text_layer,
            _icon_check: icon_check,
            _icon_cross: icon_cross,
        });
    }

    fn unload(&self, window: WindowRef) {
        if let Some(state) = self.state.borrow_mut().take() {
            state.action_bar.remove_from_window();
        }
        release(window);
    }
}
