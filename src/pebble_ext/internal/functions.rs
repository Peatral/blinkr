use super::types::*;
use core::ffi::c_void;
use pebble_rust::types::GRect;

extern "C" {
    pub fn menu_layer_create(bounds: GRect) -> *mut MenuLayer;
    pub fn menu_layer_destroy(menu_layer: *mut MenuLayer);
    pub fn menu_layer_get_layer(menu_layer: *mut MenuLayer) -> *mut c_void;

    pub fn menu_layer_set_callbacks(
        menu_layer: *mut c_void,
        context: *mut c_void,
        callbacks: MenuLayerCallbacks,
    );

    pub fn menu_layer_set_click_config_onto_window(menu_layer: *mut MenuLayer, window: *mut c_void);
    pub fn menu_layer_reload_data(menu_layer: *mut MenuLayer);
    pub fn menu_cell_basic_draw(
        ctx: *mut c_void,
        layer: *const c_void,
        title: *const u8,
        subtitle: *const u8,
        icon: *mut c_void,
    );

    pub fn text_layer_set_text_alignment(text_layer: *mut c_void, text_alignment: u32);
    pub fn wakeup_cancel_all();
    pub fn wakeup_schedule(timestamp: i32, reason: i32, exclusive: bool) -> i32;
    pub fn launch_reason() -> AppLaunchReason;
    pub fn persist_exists(key: u32) -> bool;
    pub fn persist_read_bool(key: u32) -> bool;
    pub fn persist_write_bool(key: u32, value: bool) -> i32;
    pub fn persist_read_int(key: u32) -> i32;
    pub fn persist_write_int(key: u32, value: i32) -> i32;
    pub fn time(tloc: *mut c_void) -> i32;
    pub fn vibes_short_pulse();
    pub fn vibes_long_pulse();
    pub fn vibes_double_pulse();
    pub fn app_timer_register(
        timeout_ms: u32,
        callback: extern "C" fn(*mut c_void),
        data: *mut c_void,
    ) -> *mut c_void;
    pub fn app_timer_cancel(timer_handle: *mut c_void) -> bool;
    pub fn window_stack_pop_all(animated: bool);
    pub fn snprintf(str: *mut u8, size: usize, format: *const u8, ...) -> i32;
}
