use crate::message_queue::{push_message, Message};
use crate::state::CURRENT_WAKEUP_ID;
use alloc::ffi::CString;
use pebble::std::time;
use pebble_sys::{time_t, Tuple, WakeupId};

pub const DISTANT_PAST_SECONDS: time_t = i32::MIN;
pub const DISTANT_FUTURE_SECONDS: time_t = i32::MAX;

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

pub fn reschedule_timer_interval(interval: time_t) -> Option<WakeupId> {
    let now = time::get_time();
    reschedule_timer(now, now + interval)
}

pub fn reschedule_timer(start_timestamp: time_t, end_timestamp: time_t) -> Option<WakeupId> {
    cancel_wakeup();

    let wakeup = pebble::wakeup::schedule(end_timestamp, 0, true);

    if let Some(wakeup_id) = wakeup.ok() {
        CURRENT_WAKEUP_ID.set(Some(wakeup_id));
        push_message(
            Message::RescheduleWakeup { start_timestamp, end_timestamp }.into()
        );
        return Some(wakeup_id)
    }
    None
}

pub fn cancel_wakeup() {
    pebble::wakeup::cancel_all();
    CURRENT_WAKEUP_ID.set(None);
}

pub fn start_session(start_timestamp: time_t) {
    push_message(
        Message::StartSession { start_timestamp }
    );
}

pub fn stop_session(start_timestamp: time_t, end_timestamp: time_t) {
    push_message(
        Message::StopSession { start_timestamp, end_timestamp }
    );
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
