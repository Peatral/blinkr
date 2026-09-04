extern crate alloc;
use alloc::collections::VecDeque;
use pebble::app_message::{Dictionary, Outbox};
use pebble::types::GlobalRefCell;
use pebble_sys::AppMessageResult;
use crate::message_keys::{MESSAGE_KEY_MSG_TYPE, MESSAGE_KEY_SYNC_DATA_CHUNK, MESSAGE_KEY_SYNC_TOTAL_CHUNKS, MESSAGE_KEY_TIMESTAMP};
use crate::message_types::{MSG_TYPE_RESCHEDULE_WAKEUP, MSG_TYPE_START_SESSION, MSG_TYPE_STOP_SESSION, MSG_TYPE_SYNC_CHUNK, MSG_TYPE_SYNC_START};
use crate::state;
use crate::state::TimePair;

pub static MSG_QUEUE: GlobalRefCell<MessageQueue> = GlobalRefCell::new(MessageQueue::new());

#[derive(Clone)]
pub enum Message {
    StartSession { timestamp: i32 },
    StopSession { timestamp: i32 },
    RescheduleWakeup { next_wakeup: i32 },
    SyncStart { total_chunks: i32 },
    SyncChunk { chunk_index: usize },
}

pub struct MessageQueue {
    queue: VecDeque<Message>,
    is_sending: bool,
}

impl MessageQueue {
    pub fn new() -> Self {
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
                    Message::StartSession { timestamp } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_START_SESSION);
                        let _ = dict.write_int(MESSAGE_KEY_TIMESTAMP, *timestamp);
                    }
                    Message::StopSession { timestamp } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_STOP_SESSION);
                        let _ = dict.write_int(MESSAGE_KEY_TIMESTAMP, *timestamp);
                    }
                    Message::RescheduleWakeup { next_wakeup } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_RESCHEDULE_WAKEUP);
                        let _ = dict.write_int(MESSAGE_KEY_TIMESTAMP, *next_wakeup);
                    }
                    Message::SyncStart { total_chunks: totalChunks } => {
                        let _ = dict.write_int(MESSAGE_KEY_MSG_TYPE, MSG_TYPE_SYNC_START);
                        let _ = dict.write_int(MESSAGE_KEY_SYNC_TOTAL_CHUNKS, *totalChunks);
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

pub fn outbox_sent_handler(_dict: Dictionary) {
    MSG_QUEUE.borrow_mut().on_success();
}

pub fn outbox_failed_handler(_dict: Dictionary, _result: AppMessageResult) {
    MSG_QUEUE.borrow_mut().on_failure();
}

pub fn deinit() {
    MSG_QUEUE.borrow_mut().clear();
}