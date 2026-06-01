#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

extern crate pebble_rust as pebble;

use core::cell::RefCell;
use core::ffi::{c_char, CStr};
use core::sync::atomic::{AtomicBool, AtomicPtr, AtomicU32, AtomicUsize, Ordering};

use pebble::app_message::*;
use pebble::launch;
use pebble::layer::{ILayer, MenuLayer, MenuLayerDelegate, TextLayer, MenuLayerRef};
use pebble::std::time::get_time;
use pebble::storage;
use pebble::types::{
    AppLaunchReason, GContext, GPoint, GRect, GSize, GTextAlignment, Layer as PLayer, MenuIndex
};
use pebble::vibes;
use pebble::wakeup;
use pebble::window::{WindowDelegate, WindowRef};
use pebble::window_stack;
use pebble::{app, snprintf, timer::AppTimer, window};
use pebble::system::fonts::{FONT_KEY_BITHAM_42_BOLD, FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_28_BOLD};

const PERSIST_STATE_KEY: u32 = 1;
const PERSIST_INTERVAL_KEY: u32 = 2;
const CLAY_MESSAGE_KEY_INTERVAL: u32 = 10000;
const DEFAULT_INTERVAL_MINS: u32 = 20;


static IS_ENABLED: AtomicBool = AtomicBool::new(false);
static INTERVAL_MINS: AtomicU32 = AtomicU32::new(DEFAULT_INTERVAL_MINS);
static LAUNCH_REASON: AtomicUsize = AtomicUsize::new(0);
static MENU_PTR: AtomicPtr<MenuLayer<ReminderMenu>> = AtomicPtr::new(core::ptr::null_mut());


/// Safely extracts varying-length integers from Clay payloads
pub fn extract_clay_int(tuple: &Tuple, default: i32) -> i32 {
    unsafe {
        let ptr = tuple as *const _ as *const u8;
        let length_ptr = ptr.add(5) as *const u16;
        let length = core::ptr::read_unaligned(length_ptr);
        let val_ptr = ptr.add(7);

        match length {
            1 => (*val_ptr) as i8 as i32,
            2 => {
                let mut bytes = [0u8; 2];
                core::ptr::copy_nonoverlapping(val_ptr, bytes.as_mut_ptr(), 2);
                i16::from_le_bytes(bytes) as i32
            }
            4 => {
                let mut bytes = [0u8; 4];
                core::ptr::copy_nonoverlapping(val_ptr, bytes.as_mut_ptr(), 4);
                i32::from_le_bytes(bytes)
            }
            _ => default,
        }
    }
}

/// # Safety
/// `buf` must be a valid pointer to a mutable buffer of at least `len` bytes.
pub unsafe fn format_int(buf: *mut u8, len: usize, format: &CStr, val: i32) {
    snprintf(
        buf as *mut c_char,
        len,
        format.as_ptr(),
        val
    );
}


struct ReminderMenu {
    subtitle_buf: RefCell<[u8; 16]>,
}

impl ReminderMenu {
    fn new() -> Self {
        Self { subtitle_buf: RefCell::new([0; 16]) }
    }
}

impl MenuLayerDelegate for ReminderMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        2
    }

    fn draw_row(&self, ctx: *mut GContext, cell_layer: *const PLayer, index: *mut MenuIndex) {
        unsafe {
            let row = (*index).row;

            if row == 0 {
                let is_enabled = IS_ENABLED.load(Ordering::Relaxed);
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
                let interval = INTERVAL_MINS.load(Ordering::Relaxed);
                let mut buf = self.subtitle_buf.borrow_mut();

                format_int(
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
            let mut interval = INTERVAL_MINS.load(Ordering::Relaxed);
            let mut is_enabled = IS_ENABLED.load(Ordering::Relaxed);

            if row == 0 {
                is_enabled = !is_enabled;
                IS_ENABLED.store(is_enabled, Ordering::Relaxed);

                if let Err(_) = storage::write_bool(PERSIST_STATE_KEY, is_enabled) {
                    pebble::pbl_err!(c"Failed to write state to storage!");
                }

                if is_enabled {
                    vibes::long_pulse();
                    let now = get_time();
                    if let Err(_) = wakeup::schedule(now + (interval * 60), 0, true) {
                        pebble::pbl_err!(c"Failed to schedule wakeup!");
                    }
                } else {
                    vibes::double_pulse();
                    wakeup::cancel_all();
                }
            } else if row == 1 {
                interval += 10;
                if interval > 60 {
                    interval = 10;
                }
                INTERVAL_MINS.store(interval, Ordering::Relaxed);

                if let Err(_) = storage::write_int(PERSIST_INTERVAL_KEY, interval as i32) {
                    pebble::pbl_err!(c"Failed to write interval to storage!");
                }

                if is_enabled {
                    wakeup::cancel_all();
                    let now = get_time();
                    if let Err(_) = wakeup::schedule(now + (interval * 60), 0, true) {
                        pebble::pbl_err!(c"Failed to schedule wakeup!");
                    }
                }
            }

            menu_layer.reload_data();
        }
    }
}


struct MainWindowDelegate {
    text_main: RefCell<Option<TextLayer>>,
    text_sub: RefCell<Option<TextLayer>>,
    menu_view: RefCell<Option<MenuLayer<ReminderMenu>>>,
    exit_timer: RefCell<Option<AppTimer>>,
}

impl MainWindowDelegate {
    fn new() -> Self {
        Self {
            text_main: RefCell::new(None),
            text_sub: RefCell::new(None),
            menu_view: RefCell::new(None),
            exit_timer: RefCell::new(None),
        }
    }
}

impl WindowDelegate for MainWindowDelegate {
    fn load(&self, window: WindowRef) {
        let root = window.get_root_layer();
        let bounds = root.get_bounds();
        let width = bounds.size.w;
        let height = bounds.size.h;

        let launch_val = LAUNCH_REASON.load(Ordering::Relaxed) as u32;
        let reason = AppLaunchReason::from(launch_val);
        let is_enabled = IS_ENABLED.load(Ordering::Relaxed);

        match reason {
            AppLaunchReason::Wakeup => {
                let text_main = TextLayer::new(GRect {
                    origin: GPoint { x: 0, y: 30 },
                    size: GSize { w: width, h: 50 },
                });
                text_main.set_text(c"Blink");
                text_main.set_font(pebble::system::fonts::Font::get_system(
                    FONT_KEY_BITHAM_42_BOLD
                ));
                text_main.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text_main);

                let text_sub = TextLayer::new(GRect {
                    origin: GPoint { x: 5, y: 100 },
                    size: GSize { w: width - 10, h: 60 },
                });
                text_sub.set_text(c"20-20-20 Rule:\nLook 20ft away\nfor 20 seconds.");
                text_sub.set_font(pebble::system::fonts::Font::get_system(
                    FONT_KEY_GOTHIC_18_BOLD
                ));
                text_sub.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text_sub);

                *self.text_main.borrow_mut() = Some(text_main);
                *self.text_sub.borrow_mut() = Some(text_sub);
                *self.exit_timer.borrow_mut() = Some(AppTimer::register(20_000, handle_exit_timer));
            }

            AppLaunchReason::QuickLaunch => {
                let text = TextLayer::new(GRect {
                    origin: GPoint { x: 0, y: height / 2 - 20 },
                    size: GSize { w: width, h: 40 },
                });
                text.set_text(if is_enabled {
                    c"Active"
                } else {
                    c"Inactive"
                });
                text.set_font(pebble::system::fonts::Font::get_system(
                    FONT_KEY_GOTHIC_28_BOLD
                ));
                text.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text);

                *self.text_main.borrow_mut() = Some(text);
                *self.exit_timer.borrow_mut() = Some(AppTimer::register(20_000, handle_exit_timer));
            }

            _ => {
                let menu = MenuLayer::new(bounds, ReminderMenu::new());
                menu.set_click_config_onto_window(&window);
                root.add_child(&menu);

                let mut view = self.menu_view.borrow_mut();
                *view = Some(menu);

                let stable_ptr = view.as_mut().unwrap() as *mut MenuLayer<ReminderMenu>;
                MENU_PTR.store(stable_ptr, Ordering::Relaxed);
            }
        }
    }

    fn unload(&self, _window: WindowRef) {
        self.menu_view.borrow_mut().take();
        self.text_main.borrow_mut().take();
        self.text_sub.borrow_mut().take();

        self.exit_timer.borrow_mut().take();

        MENU_PTR.store(core::ptr::null_mut(), Ordering::Relaxed);
    }
}

#[no_mangle]
pub fn main() -> isize {
    let launch = launch::get_reason();
    LAUNCH_REASON.store(launch as u32 as usize, Ordering::Relaxed);

    if launch == AppLaunchReason::Wakeup
        && storage::exists(PERSIST_STATE_KEY)
        && !storage::read_bool(PERSIST_STATE_KEY)
    {
        pebble::pbl_warn!(c"Ghost wakeup detected. Aborting.");
        return 0;
    }

    let mut is_enabled = false;
    if storage::exists(PERSIST_STATE_KEY) {
        is_enabled = storage::read_bool(PERSIST_STATE_KEY);
    }
    IS_ENABLED.store(is_enabled, Ordering::Relaxed);

    let mut interval = DEFAULT_INTERVAL_MINS;
    if storage::exists(PERSIST_INTERVAL_KEY) {
        interval = storage::read_int(PERSIST_INTERVAL_KEY) as u32;
    }
    INTERVAL_MINS.store(interval, Ordering::Relaxed);

    if launch == AppLaunchReason::Wakeup {
        vibes::double_pulse();
        let now = get_time();
        if let Err(_) = wakeup::schedule(now + (interval * 60), 0, true) {
            pebble::pbl_err!(c"Failed to schedule wakeup during boot!");
        }
    } else if launch == AppLaunchReason::QuickLaunch {
        is_enabled = !is_enabled;
        IS_ENABLED.store(is_enabled, Ordering::Relaxed);

        if let Err(_) = storage::write_bool(PERSIST_STATE_KEY, is_enabled) {
            pebble::pbl_err!(c"Failed to write state on QuickLaunch");
        }

        if is_enabled {
            vibes::long_pulse();
            let now = get_time();
            if let Err(_) = wakeup::schedule(now + (interval * 60), 0, true) {
                pebble::pbl_err!(c"Failed to schedule wakeup on QuickLaunch!");
            }
        } else {
            vibes::double_pulse();
            wakeup::cancel_all();
        }
    }

    AppMessage::register_inbox_received(inbox_received_handler);

    if let Err(_) = AppMessage::open(128, 128) {
        pebble::pbl_err!(c"Failed to open AppMessage subsystem!");
    }

    let app = app::App::new();
    let window = window::Window::new(MainWindowDelegate::new());

    window_stack::push(&window, false);
    app.run_event_loop();

    0
}

fn handle_exit_timer() {
    let launch_val = LAUNCH_REASON.load(Ordering::Relaxed) as u32;
    if AppLaunchReason::from(launch_val) == AppLaunchReason::Wakeup {
        vibes::short_pulse();
    }

    window_stack::pop_all(false);
}

fn inbox_received_handler(dict: Dictionary) {
    if let Some(tuple) = dict.find(CLAY_MESSAGE_KEY_INTERVAL) {
        let new_interval = extract_clay_int(&tuple, DEFAULT_INTERVAL_MINS as i32) as u32;

        INTERVAL_MINS.store(new_interval, Ordering::Relaxed);

        if let Err(_) = storage::write_int(PERSIST_INTERVAL_KEY, new_interval as i32) {
            pebble::pbl_err!(c"AppMsg: Failed to save interval to storage");
        }

        if IS_ENABLED.load(Ordering::Relaxed) {
            wakeup::cancel_all();
            let now = get_time();
            if let Err(_) = wakeup::schedule(now + (new_interval * 60), 0, true) {
                pebble::pbl_err!(c"AppMsg: Failed to reschedule wakeup");
            }
        }

        let ptr = MENU_PTR.load(Ordering::Relaxed);
        if !ptr.is_null() {
            unsafe {
                (*ptr).reload_data();
            }
        }
    }
}
