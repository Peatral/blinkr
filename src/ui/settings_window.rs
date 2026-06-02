use crate::utils::reschedule_wakeup;
use crate::{state, utils};
use core::cell::RefCell;
use core::ffi::CStr;
use pebble::app_message::Dictionary;
use pebble::layer::{ILayer, MenuLayer, MenuLayerDelegate, MenuLayerRef};
use pebble::types::{GContext, GlobalCell, MenuIndex};
use pebble::window::{Window, WindowDelegate, WindowRef};

static MENU_REF: GlobalCell<Option<MenuLayer<ReminderMenu>>> = GlobalCell::new(None);

struct ReminderMenu {
    subtitle_buf: RefCell<[u8; 16]>,
}

impl MenuLayerDelegate for ReminderMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        2
    }

    fn draw_row(
        &self,
        ctx: *mut GContext,
        cell_layer: *const pebble::types::Layer,
        index: *mut MenuIndex,
    ) {
        unsafe {
            let row = (*index).row;

            if row == 0 {
                let is_enabled = *state::IS_ENABLED.borrow();
                let subtitle = if is_enabled { c"ON" } else { c"OFF" };
                pebble::layer::menu_layer::cell_basic_draw(
                    ctx,
                    cell_layer,
                    c"Reminder",
                    subtitle,
                    core::ptr::null_mut(),
                );
            } else if row == 1 {
                let interval = *state::INTERVAL_MINS.borrow();
                let mut buf = self.subtitle_buf.borrow_mut();

                utils::format_int(buf.as_mut_ptr(), 16, c"%d mins", interval as i32);

                let subtitle_cstr = CStr::from_bytes_until_nul(&buf[..]).unwrap_or(c"Error");

                pebble::layer::menu_layer::cell_basic_draw(
                    ctx,
                    cell_layer,
                    c"Duration",
                    subtitle_cstr,
                    core::ptr::null_mut(),
                );
            }
        }
    }

    fn select_click(&self, menu_layer: MenuLayerRef, index: *mut MenuIndex) {
        unsafe {
            let row = (*index).row;
            if row == 0 {
                state::toggle_state();
            } else if row == 1 {
                let mut interval = *state::INTERVAL_MINS.borrow();
                interval += 10;
                if interval > 60 {
                    interval = 10;
                }

                *state::INTERVAL_MINS.borrow_mut() = interval;
                let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, interval as i32);

                if *state::IS_ENABLED.borrow() {
                    let _ = reschedule_wakeup(interval);
                }
            }
            menu_layer.reload_data();
        }
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
    if let Some(tuple) = dict.find(state::CLAY_MESSAGE_KEY_INTERVAL) {
        let new_interval =
            utils::extract_clay_int(&tuple, state::DEFAULT_INTERVAL_MINS as i32) as u32;

        *state::INTERVAL_MINS.borrow_mut() = new_interval;
        let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, new_interval as i32);

        if *state::IS_ENABLED.borrow() {
            let _ = reschedule_wakeup(new_interval);
        }

        if let Some(menu) = MENU_REF.borrow().as_ref() {
            menu.reload_data();
        }
    }
}
