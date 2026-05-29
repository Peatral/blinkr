use crate::pebble_ext::internal::functions;

pub fn schedule(timestamp: i32, reason: i32, exclusive: bool) {
    unsafe {
        functions::wakeup_schedule(timestamp, reason, exclusive);
    }
}

pub fn cancel_all() {
    unsafe {
        functions::wakeup_cancel_all();
    }
}
