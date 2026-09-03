use alloc::ffi::CString;
use pebble::app_message::Outbox;
use pebble::std::time;
use pebble_sys::{StatusCode, Tuple, WakeupId, time_t};
use crate::message_keys::{MESSAGE_KEY_DURATION, MESSAGE_KEY_MSG_TYPE, MESSAGE_KEY_TIMESTAMP};
use crate::state::{MSG_TYPE_START, MSG_TYPE_STOP};

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

pub fn reschedule_wakeup(interval_secs: time_t) -> Result<WakeupId, StatusCode> {
    pebble::wakeup::cancel_all();
    let now = time::get_time();
    let wakeup = pebble::wakeup::schedule(now + interval_secs, 0, true);

    if let Ok(dict) = Outbox::begin() {
        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_START);
        let _ = dict.write_int(MESSAGE_KEY_TIMESTAMP, now);
        let _ = dict.write_int(MESSAGE_KEY_DURATION, interval_secs);
        let _ = Outbox::send();
    }

    wakeup
}

pub fn cancel_wakeup() {
    pebble::wakeup::cancel_all();

    let now = time::get_time();
    if let Ok(dict) = Outbox::begin() {
        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_STOP);
        let _ = dict.write_int(MESSAGE_KEY_TIMESTAMP, now);
        let _ = Outbox::send();
    }
}

pub fn format_duration(seconds: time_t) -> CString {
    let hours = seconds / 3600;
    let mins = (seconds % 3600) / 60;
    if hours > 0 {
        pebble::pbl_fmt!(let formatted = c"%dh %dm", hours, mins);
        CString::from(formatted)
    } else {
        pebble::pbl_fmt!(let formatted = c"%dm", mins);
        CString::from(formatted)
    }
}

pub fn format_day(day_index: i32) -> CString {
    match day_index {
        0 => CString::from(c"Today"),
        1 => CString::from(c"Yesterday"),
        _ => {
            pebble::pbl_fmt!(let formatted = c"%d days ago", day_index);
            CString::from(formatted)
        }
    }
}

pub fn format_time_range(start: time_t, end: time_t) -> CString {
    let tm_s = time::get_local_time(start);
    let h_s = tm_s.tm_hour;
    let m_s = tm_s.tm_min;

    let tm_e = time::get_local_time(end);
    let h_e = tm_e.tm_hour;
    let m_e = tm_e.tm_min;

    pebble::pbl_fmt!(let f = c"%02d:%02d - %02d:%02d", h_s, m_s, h_e, m_e);
    CString::from(f)
}
