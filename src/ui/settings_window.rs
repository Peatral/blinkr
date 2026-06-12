use crate::utils::reschedule_wakeup;
use crate::{message_keys, state, utils};
use core::cell::RefCell;
use core::ffi::CStr;
use pebble::app_message::Dictionary;
use pebble::graphics::context::Context;
use pebble::layer::menu_layer::MenuCellLayer;
use pebble::layer::{ILayer, ILayerMut, MenuIndexRef, MenuLayer, MenuLayerDelegate, MenuLayerRef};
use pebble::types::GlobalRefCell;
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble_sys::time_t;

static MENU_REF: GlobalRefCell<Option<MenuLayer<ReminderMenu>>> = GlobalRefCell::new(None);

struct ReminderMenu {
    subtitle_buf: RefCell<[u8; 16]>,
}

impl MenuLayerDelegate for ReminderMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        2
    }

    fn draw_row(&self, ctx: Context, cell_layer: MenuCellLayer, index: MenuIndexRef) {
        let row = index.row();

        if row == 0 {
            let is_enabled = state::IS_ENABLED.get();
            let subtitle = if is_enabled { c"ON" } else { c"OFF" };
            cell_layer.draw_basic(ctx, c"Reminder", subtitle, None);
        } else if row == 1 {
            let interval = state::INTERVAL_MINS.get();
            let mut buf = self.subtitle_buf.borrow_mut();

            unsafe {
                utils::format_int(buf.as_mut_ptr(), 16, c"%d mins", interval as i32);
            }

            let subtitle_cstr = CStr::from_bytes_until_nul(&buf[..]).unwrap_or(c"Error");

            cell_layer.draw_basic(ctx, c"Duration", subtitle_cstr, None);
        }
    }

    fn draw_header(&self, ctx: Context, cell_layer: MenuCellLayer, _section_index: u16) {
        cell_layer.draw_basic_header(ctx, c"Settings");
    }

    fn select_click(&self, menu_layer: MenuLayerRef, index: MenuIndexRef) {
        let row = index.row();
        if row == 0 {
            state::toggle_state();
        } else if row == 1 {
            let mut interval = state::INTERVAL_MINS.get();
            interval += 10;
            if interval > 60 {
                interval = 10;
            }

            state::INTERVAL_MINS.set(interval);
            let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, interval as i32);

            if state::IS_ENABLED.get() {
                let _ = reschedule_wakeup(interval);
            }
        }
        menu_layer.reload_data();
    }
}

pub struct SettingsDelegate;

impl WindowDelegate for SettingsDelegate {
    fn load(&self, window: WindowRef) {
        let bounds = window.get_root_layer().get_bounds();
        let menu = MenuLayer::new(
            bounds,
            ReminderMenu {
                subtitle_buf: RefCell::new([0; 16]),
            },
        );
        menu.set_click_config_onto_window(&window);
        window.get_root_layer().add_child(&menu);

        *MENU_REF.borrow_mut() = Some(menu);
    }

    fn unload(&self, _window: WindowRef) {
        MENU_REF.borrow_mut().take();
    }
}

pub fn create() -> Window<SettingsDelegate> {
    Window::new(SettingsDelegate {})
}

pub fn inbox_received_handler(dict: Dictionary) {
    if let Some(tuple) = dict.find(message_keys::MESSAGE_KEY_INTERVAL) {
        let new_interval =
            utils::extract_clay_int(&tuple, state::DEFAULT_INTERVAL_MINS as i32) as time_t;

        state::INTERVAL_MINS.set(new_interval);
        let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, new_interval as time_t);

        if state::IS_ENABLED.get() {
            let _ = reschedule_wakeup(new_interval);
        }

        if let Some(menu) = MENU_REF.borrow().as_ref() {
            menu.reload_data();
        }
    }
}
