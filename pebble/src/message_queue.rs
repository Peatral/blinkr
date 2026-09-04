extern crate alloc;
use crate::message_keys::{MESSAGE_KEY_END_TIMESTAMP, MESSAGE_KEY_MSG_TYPE, MESSAGE_KEY_NEXT_WAKEUP, MESSAGE_KEY_START_TIMESTAMP, MESSAGE_KEY_SYNC_DATA_CHUNK, MESSAGE_KEY_SYNC_TOTAL_CHUNKS};
use crate::message_types::{MSG_TYPE_REQUEST_SYNC, MSG_TYPE_RESCHEDULE_WAKEUP, MSG_TYPE_START_SESSION, MSG_TYPE_STOP_SESSION, MSG_TYPE_SYNC_CHUNK, MSG_TYPE_SYNC_START};
use crate::state::TimePair;
use crate::sync::start_sync;
use crate::{state, ui};
use alloc::collections::VecDeque;
use pebble::app_message::{Dictionary, Outbox};
use pebble::types::GlobalRefCell;
use pebble_sys::AppMessageResult;

static MSG_QUEUE: GlobalRefCell<MessageQueue> = GlobalRefCell::new(MessageQueue::new());

#[derive(Clone)]
pub enum Message {
    StartSession { start_timestamp: i32 },
    StopSession { start_timestamp: i32, end_timestamp: i32 },
    RescheduleWakeup { next_wakeup: i32 },
    SyncStart { total_chunks: i32 },
    SyncChunk { chunk_index: usize },
}

pub struct MessageQueue {
    queue: VecDeque<Message>,
    is_sending: bool,
}

impl MessageQueue {
    pub const fn new() -> Self {
        Self {
            queue: VecDeque::new(),
            is_sending: false,
        }
    }

    pub fn push(&mut self, msg: Message) {
        self.queue.push_back(msg);
        self.try_send_next();
    }

    fn try_send_next(&mut self) {
        if self.is_sending || self.queue.is_empty() {
            return;
        }

        if let Some(msg) = self.queue.front() {
            if let Ok(dict) = Outbox::begin() {
                match &msg {
                    Message::StartSession { start_timestamp } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_START_SESSION);
                        let _ = dict.write_int(MESSAGE_KEY_START_TIMESTAMP, *start_timestamp);
                    }
                    Message::StopSession { start_timestamp, end_timestamp } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_STOP_SESSION);
                        let _ = dict.write_int(MESSAGE_KEY_START_TIMESTAMP, *start_timestamp);
                        let _ = dict.write_int(MESSAGE_KEY_END_TIMESTAMP, *end_timestamp);
                    }
                    Message::RescheduleWakeup { next_wakeup } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_RESCHEDULE_WAKEUP);
                        let _ = dict.write_int(MESSAGE_KEY_NEXT_WAKEUP, *next_wakeup);
                    }
                    Message::SyncStart { total_chunks } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_SYNC_START);
                        let _ = dict.write_int(MESSAGE_KEY_SYNC_TOTAL_CHUNKS, *total_chunks);
                    }
                    Message::SyncChunk { chunk_index } => {
                        let history = state::HISTORY.borrow();
                        let chunks: alloc::vec::Vec<&[TimePair]> =
                            history.chunks(state::PAIRS_PER_CHUNK).collect();

                        if let Some(&chunk) = chunks.get(*chunk_index) {
                            let bytes = bytemuck::cast_slice(chunk);
                            let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_SYNC_CHUNK);
                            let _ = dict.write_data(MESSAGE_KEY_SYNC_DATA_CHUNK, bytes);
                        }
                    }
                }

                if Outbox::send().is_ok() {
                    self.is_sending = true;
                }
            }
        }
    }

    pub fn on_success(&mut self) {
        self.is_sending = false;
        self.queue.pop_front();
        self.try_send_next();
    }

    pub fn on_failure(&mut self) {
        self.is_sending = false;
        self.try_send_next();
    }

    pub fn clear(&mut self) {
        self.queue.clear();
        self.is_sending = false;
    }
}

pub fn push_message(message: Message) {
    MSG_QUEUE.borrow_mut().push(message);
}

pub fn inbox_received_handler(dict: Dictionary) {
    if let Some(tuple) = dict.find(MESSAGE_KEY_MSG_TYPE) {
        if tuple.type_() == pebble_sys::TupleType::TUPLE_INT && tuple.length == 4 {
            let msg_type_opt = unsafe {
                let value_ptr = core::ptr::addr_of!(tuple.value) as *const i32;
                Some(core::ptr::read_unaligned(value_ptr))
            };

            if let Some(msg_type) = msg_type_opt {
                match msg_type {
                    MSG_TYPE_REQUEST_SYNC => start_sync(),
                    _ => {},
                }
            }
        }
    } else {
        // TODO: Do the settings messages properly
        ui::settings_window::inbox_received_handler(dict)
    }
}

pub fn outbox_sent_handler(_dict: Dictionary) {
    MSG_QUEUE.borrow_mut().on_success();
}

pub fn outbox_failed_handler(_dict: Dictionary, _result: AppMessageResult) {
    MSG_QUEUE.borrow_mut().on_failure();
}

pub fn deinit() {
    MSG_QUEUE.borrow_mut().clear();
}