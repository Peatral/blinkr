use crate::pebble_ext::internal::functions;
// Removed c_void import to satisfy Clippy

pub use crate::pebble_ext::internal::types::AppLaunchReason;

pub fn launch_reason() -> AppLaunchReason {
    unsafe { functions::launch_reason() }
}

pub fn time() -> i32 {
    unsafe { functions::time(core::ptr::null_mut()) }
}

pub fn window_stack_pop_all(animated: bool) {
    unsafe { functions::window_stack_pop_all(animated) }
}

// Accepts raw pointers now to avoid static mut reference UB
pub fn format_int(buf: *mut u8, len: usize, format: *const u8, val: i32) {
    unsafe {
        functions::snprintf(buf, len, format, val);
    }
}

pub mod persist {
    use crate::pebble_ext::internal::functions;
    pub fn exists(key: u32) -> bool {
        unsafe { functions::persist_exists(key) }
    }
    pub fn read_bool(key: u32) -> bool {
        unsafe { functions::persist_read_bool(key) }
    }
    pub fn write_bool(key: u32, value: bool) {
        unsafe {
            functions::persist_write_bool(key, value);
        }
    }
    pub fn read_int(key: u32) -> i32 {
        unsafe { functions::persist_read_int(key) }
    }
    pub fn write_int(key: u32, value: i32) {
        unsafe {
            functions::persist_write_int(key, value);
        }
    }
}

pub mod vibes {
    use crate::pebble_ext::internal::functions;
    pub fn short_pulse() {
        unsafe { functions::vibes_short_pulse() }
    }
    pub fn long_pulse() {
        unsafe { functions::vibes_long_pulse() }
    }
    pub fn double_pulse() {
        unsafe { functions::vibes_double_pulse() }
    }
}

pub mod timer {
    use crate::pebble_ext::internal::functions;
    use core::ffi::c_void;

    pub fn register(timeout_ms: u32, callback: extern "C" fn(*mut c_void)) -> *mut c_void {
        unsafe { functions::app_timer_register(timeout_ms, callback, core::ptr::null_mut()) }
    }
    pub fn cancel(timer_handle: *mut c_void) {
        unsafe {
            functions::app_timer_cancel(timer_handle);
        }
    }
}
