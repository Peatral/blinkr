use core::ffi::CStr;
use pebble::types::{StatusCode, WakeupId};
use pebble::{snprintf, types::Tuple};

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

pub unsafe fn format_int(buf: *mut u8, len: usize, format: &CStr, val: i32) {
    unsafe {
        snprintf(buf as *mut _, len, format.as_ptr(), val);
    }
}

pub fn reschedule_wakeup(interval_mins: u32) -> Result<WakeupId, StatusCode> {
    pebble::wakeup::cancel_all();
    let now = pebble::std::time::get_time();
    pebble::wakeup::schedule(now + (interval_mins * 60), 0, true)
}
