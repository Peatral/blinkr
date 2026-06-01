use core::cell::RefCell;
use core::ffi::CStr;
use core::sync::atomic::{AtomicPtr, Ordering};
use pebble::app_message::Dictionary;
use pebble::layer::{ILayer, MenuLayer, MenuLayerDelegate, MenuLayerRef};
use pebble::types::{GContext, MenuIndex};
use pebble::window::{Window, WindowDelegate, WindowRef};
use crate::{state, utils};

static MENU_PTR: AtomicPtr<MenuLayer<ReminderMenu>> = AtomicPtr::new(core::ptr::null_mut());

struct ReminderMenu {
    subtitle_buf: RefCell<[u8; 16]>,
}

impl MenuLayerDelegate for ReminderMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        2
    }

    fn draw_row(&self, ctx: *mut GContext, cell_layer: *const pebble::types::Layer, index: *mut MenuIndex) {
        unsafe {
            let row = (*index).row;

            if row == 0 {
                let is_enabled = state::IS_ENABLED.load(Ordering::Relaxed);
                let subtitle = if is_enabled {
                    c"ON"
                } else {
                    c"OFF"
                };
                pebble::layer::menu_layer::cell_basic_draw(
                    ctx,
                    cell_layer,
                    c"Reminder",
                    subtitle,
                    core::ptr::null_mut(),
                );
            } else if row == 1 {
                let interval = state::INTERVAL_MINS.load(Ordering::Relaxed);
                let mut buf = self.subtitle_buf.borrow_mut();

                utils::format_int(
                    buf.as_mut_ptr(),
                    16,
                    c"%d mins",
                    interval as i32,
                );

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
                let mut interval = state::INTERVAL_MINS.load(Ordering::Relaxed);
                interval += 10;
                if interval > 60 { interval = 10; }
                state::INTERVAL_MINS.store(interval, Ordering::Relaxed);
                let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, interval as i32);

                if state::IS_ENABLED.load(Ordering::Relaxed) {
                    pebble::wakeup::cancel_all();
                    let now = pebble::std::time::get_time();
                    let _ = pebble::wakeup::schedule(now + (interval * 60), 0, true);
                }
            }
            menu_layer.reload_data();
        }
    }
}

pub struct SettingsDelegate {
    menu_view: RefCell<Option<MenuLayer<ReminderMenu>>>,
}

impl WindowDelegate for SettingsDelegate {
    fn load(&self, window: WindowRef) {
        let bounds = window.get_root_layer().get_bounds();
        let menu = MenuLayer::new(bounds, ReminderMenu { subtitle_buf: RefCell::new([0; 16]) });
        menu.set_click_config_onto_window(&window);
        window.get_root_layer().add_child(&menu);

        let stable_ptr = &menu as *const _ as *mut MenuLayer<ReminderMenu>;
        MENU_PTR.store(stable_ptr, Ordering::Relaxed);
        *self.menu_view.borrow_mut() = Some(menu);
    }

    fn unload(&self, _window: WindowRef) {
        self.menu_view.borrow_mut().take();
        MENU_PTR.store(core::ptr::null_mut(), Ordering::Relaxed);
    }
}

pub fn create() -> Window<SettingsDelegate> {
    Window::new(SettingsDelegate {
        menu_view: RefCell::new(None),
    })
}

pub fn inbox_received_handler(dict: Dictionary) {
    if let Some(tuple) = dict.find(state::CLAY_MESSAGE_KEY_INTERVAL) {
        let new_interval = utils::extract_clay_int(&tuple, state::DEFAULT_INTERVAL_MINS as i32) as u32;
        state::INTERVAL_MINS.store(new_interval, Ordering::Relaxed);
        let _ = pebble::storage::write_int(state::PERSIST_INTERVAL_KEY, new_interval as i32);

        if state::IS_ENABLED.load(Ordering::Relaxed) {
            pebble::wakeup::cancel_all();
            let _ = pebble::wakeup::schedule(pebble::std::time::get_time() + (new_interval * 60), 0, true);
        }

        let ptr = MENU_PTR.load(Ordering::Relaxed);
        if !ptr.is_null() {
            unsafe { (*ptr).reload_data(); }
        }
    }
}