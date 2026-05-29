#![crate_type = "staticlib"]
#![no_std]
#![no_builtins]

#[macro_use]
extern crate pebble_rust as pebble;

mod pebble_ext;

use core::ffi::c_void;
use pebble::app_message::*;
use pebble::layer::{ILayer, Layer, TextLayer};
use pebble::types::{GPoint, GRect, GSize};
use pebble::window::WindowHandlers;
use pebble::{app, window, WindowPtr};

use pebble_ext::clay::extract_clay_int;
use pebble_ext::layer_ext::{GTextAlignment, TextLayerExt};
use pebble_ext::menu_layer::{self, MenuIndex, MenuLayer, MenuLayerCallbacks};
use pebble_ext::system::{self, AppLaunchReason};
use pebble_ext::wakeup;

// --- App Constants & State ---
const PERSIST_STATE_KEY: u32 = 1;
const PERSIST_INTERVAL_KEY: u32 = 2;
const CLAY_MESSAGE_KEY_INTERVAL: u32 = 10000;
const DEFAULT_INTERVAL_MINS: i32 = 20;

static mut CURRENT_LAUNCH_REASON: AppLaunchReason = AppLaunchReason::System;
static mut IS_ENABLED: bool = false;
static mut INTERVAL_MINS: i32 = 20;
static mut EXIT_TIMER: *mut c_void = core::ptr::null_mut();

// UI Pointers
static mut MENU_VIEW: Option<MenuLayer> = None;
static mut TEXT_MAIN: Option<TextLayer> = None;
static mut TEXT_SUB: Option<TextLayer> = None;
static mut SUBTITLE_BUF: [u8; 16] = [0; 16];

#[no_mangle]
pub fn main() -> isize {
    unsafe {
        CURRENT_LAUNCH_REASON = system::launch_reason();

        if CURRENT_LAUNCH_REASON == AppLaunchReason::Wakeup
            && system::persist::exists(PERSIST_STATE_KEY)
            && !system::persist::read_bool(PERSIST_STATE_KEY)
        {
            pbl_warn!("Ghost wakeup detected. Aborting.");
            return 0;
        }

        IS_ENABLED = if system::persist::exists(PERSIST_STATE_KEY) {
            system::persist::read_bool(PERSIST_STATE_KEY)
        } else {
            false
        };
        INTERVAL_MINS = if system::persist::exists(PERSIST_INTERVAL_KEY) {
            system::persist::read_int(PERSIST_INTERVAL_KEY)
        } else {
            DEFAULT_INTERVAL_MINS
        };

        if CURRENT_LAUNCH_REASON == AppLaunchReason::Wakeup {
            system::vibes::double_pulse();
            let now = system::time();
            wakeup::schedule(now + (INTERVAL_MINS * 60), 0, true);
        } else if CURRENT_LAUNCH_REASON == AppLaunchReason::QuickLaunch {
            IS_ENABLED = !IS_ENABLED;
            system::persist::write_bool(PERSIST_STATE_KEY, IS_ENABLED);

            if IS_ENABLED {
                system::vibes::long_pulse();
                let now = system::time();
                wakeup::schedule(now + (INTERVAL_MINS * 60), 0, true);
            } else {
                system::vibes::double_pulse();
                wakeup::cancel_all();
            }
        }
    }

    AppMessage::register_inbox(inbox_received_handler);
    AppMessage::open(128, 128);

    let app = app::App::new();
    let window = window::Window::new();
    window.set_handlers(WindowHandlers {
        load: load_handler,
        unload: unload_handler,
        appear: appear_handler,
        disappear: disappear_handler,
    });

    window.push(false);
    app.run_event_loop();
    window.clean_exit();
    0
}

extern "C" fn exit_timer_callback(_data: *mut c_void) {
    unsafe {
        if CURRENT_LAUNCH_REASON == AppLaunchReason::Wakeup {
            system::vibes::short_pulse();
        }
        system::window_stack_pop_all(false);
    }
}

extern "C" fn inbox_received_handler(
    dict_ptr: pebble::types::DictPtr,
    _ctx: pebble::types::VoidPtr,
) {
    let dict = Dictionary::from_raw(dict_ptr);

    if let Some(tuple) = dict.find(CLAY_MESSAGE_KEY_INTERVAL) {
        let new_interval = extract_clay_int(&tuple, DEFAULT_INTERVAL_MINS);
        unsafe {
            INTERVAL_MINS = new_interval;
            system::persist::write_int(PERSIST_INTERVAL_KEY, new_interval);

            if IS_ENABLED {
                wakeup::cancel_all();
                let now = system::time();
                wakeup::schedule(now + (new_interval * 60), 0, true);
            }

            if let Some(menu) = MENU_VIEW {
                menu.reload_data();
            }
        }
    }
}

extern "C" fn load_handler(window: WindowPtr) {
    let window_obj = window::Window::from_raw(window);
    let root = window_obj.get_root_layer();
    let bounds = root.get_bounds();
    let width = bounds.size.w;
    let height = bounds.size.h;

    unsafe {
        match CURRENT_LAUNCH_REASON {
            AppLaunchReason::Wakeup => {
                let text_main = TextLayer::new(GRect {
                    origin: GPoint { x: 0, y: 30 },
                    size: GSize { w: width, h: 50 },
                });
                text_main.set_text(nt!("Blink"));
                text_main.set_font(pebble::system::fonts::Font::get_system(nt!(
                    "RESOURCE_ID_BITHAM_42_BOLD"
                )));
                text_main.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text_main);
                TEXT_MAIN = Some(text_main);

                let text_sub = TextLayer::new(GRect {
                    origin: GPoint { x: 5, y: 100 },
                    size: GSize {
                        w: width - 10,
                        h: 60,
                    },
                });
                text_sub.set_text(nt!("20-20-20 Rule:\nLook 20ft away\nfor 20 seconds."));
                text_sub.set_font(pebble::system::fonts::Font::get_system(nt!(
                    "RESOURCE_ID_GOTHIC_18_BOLD"
                )));
                text_sub.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text_sub);
                TEXT_SUB = Some(text_sub);

                EXIT_TIMER = system::timer::register(20_000, exit_timer_callback);
            }

            AppLaunchReason::QuickLaunch => {
                let text = TextLayer::new(GRect {
                    origin: GPoint {
                        x: 0,
                        y: height / 2 - 20,
                    },
                    size: GSize { w: width, h: 40 },
                });
                text.set_text(if IS_ENABLED {
                    nt!("Active")
                } else {
                    nt!("Inactive")
                });
                text.set_font(pebble::system::fonts::Font::get_system(nt!(
                    "RESOURCE_ID_GOTHIC_28_BOLD"
                )));
                text.set_text_alignment(GTextAlignment::Center);
                root.add_child(&text);
                TEXT_MAIN = Some(text);

                EXIT_TIMER = system::timer::register(2_000, exit_timer_callback);
            }

            _ => {
                let menu = MenuLayer::new(bounds);

                let callbacks = MenuLayerCallbacks {
                    get_num_sections: None,
                    get_num_rows: Some(menu_get_num_rows),
                    get_cell_height: None,
                    get_header_height: None,
                    draw_row: Some(menu_draw_row),
                    draw_header: None,
                    select_click: Some(menu_select_click),
                    select_long_click: None,
                    selection_changed: None,
                    get_separator_height: None,
                    draw_separator: None,
                    selection_will_change: None,
                    draw_background: None,
                };

                menu.set_callbacks(callbacks);
                menu.set_click_config_onto_window(window);

                root.add_child(&menu.get_layer());

                MENU_VIEW = Some(menu);
            }
        }
    }
}

extern "C" fn menu_get_num_rows(_ctx: *mut c_void, _section: u16, _data: *mut c_void) -> u16 {
    2
}

extern "C" fn menu_draw_row(
    ctx: *mut c_void,
    layer: *const c_void,
    index: *mut MenuIndex,
    _data: *mut c_void,
) {
    unsafe {
        let index_val = core::ptr::read_unaligned(index);
        let row = index_val.row;

        if row == 0 {
            let subtitle = if IS_ENABLED {
                nt!("ON").as_ptr()
            } else {
                nt!("OFF").as_ptr()
            };
            menu_layer::cell_basic_draw(ctx, layer, nt!("Reminder").as_ptr(), subtitle);
        } else if row == 1 {
            system::format_int(
                core::ptr::addr_of_mut!(SUBTITLE_BUF) as *mut u8,
                16,
                nt!("%d mins").as_ptr(),
                INTERVAL_MINS,
            );

            menu_layer::cell_basic_draw(
                ctx,
                layer,
                nt!("Duration").as_ptr(),
                core::ptr::addr_of!(SUBTITLE_BUF) as *const u8,
            );
        }
    }
}

extern "C" fn menu_select_click(_ctx: *mut c_void, index: *mut MenuIndex, _data: *mut c_void) {
    unsafe {
        let index_val = core::ptr::read_unaligned(index);
        let row = index_val.row;

        if row == 0 {
            IS_ENABLED = !IS_ENABLED;
            system::persist::write_bool(PERSIST_STATE_KEY, IS_ENABLED);
            if IS_ENABLED {
                system::vibes::long_pulse();
                let now = system::time();
                wakeup::schedule(now + (INTERVAL_MINS * 60), 0, true);
            } else {
                system::vibes::double_pulse();
                wakeup::cancel_all();
            }
        } else if row == 1 {
            INTERVAL_MINS += 10;
            if INTERVAL_MINS > 60 {
                INTERVAL_MINS = 10;
            }
            system::persist::write_int(PERSIST_INTERVAL_KEY, INTERVAL_MINS);

            if IS_ENABLED {
                wakeup::cancel_all();
                let now = system::time();
                wakeup::schedule(now + (INTERVAL_MINS * 60), 0, true);
            }
        }

        if let Some(menu) = MENU_VIEW {
            menu.reload_data();
        }
    }
}

extern "C" fn unload_handler(_window: WindowPtr) {
    unsafe {
        if let Some(menu) = MENU_VIEW {
            menu.destroy();
            MENU_VIEW = None;
        }
    }
}

extern "C" fn appear_handler(_window: WindowPtr) {}
extern "C" fn disappear_handler(_window: WindowPtr) {}
