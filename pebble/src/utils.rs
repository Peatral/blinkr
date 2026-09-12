use crate::message_queue::{push_message, Message};
use crate::state::CURRENT_WAKEUP_ID;
use alloc::ffi::CString;
use pebble::std::time;
use pebble_sys::{time_t, WakeupId};

pub const DISTANT_PAST_SECONDS: time_t = i32::MIN;
pub const DISTANT_FUTURE_SECONDS: time_t = i32::MAX;

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
