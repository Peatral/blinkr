use crate::pebble::window::{WindowDelegate, WindowRef};
use crate::state::{CURRENT_START_TIME, HISTORY, TimePair};
use crate::ui::settings_window;
use crate::window_manager;
use crate::window_manager::{AppWindow, release};
use alloc::ffi::CString;
use alloc::vec::Vec;
use core::cell::RefCell;
use core::cmp::{max, min};
use core::ffi::CStr;
use pebble::clicks::{ClickConfigurator, ClickDelegate, ClickRecognizer};
use pebble::graphics::types::{Color, Point, Rect, Size};
use pebble::layer::{CanvasLayer, ILayer, ILayerMut, ScrollDelegate, ScrollLayer, TextLayer};
use pebble::window::Window;
use pebble_sys::{ButtonId, GCornerMask, GTextAlignment, time_t};

const DISPLAYED_DAYS: i32 = 7;
const ROW_HEIGHT: i16 = 50;
const SECONDS_PER_DAY: time_t = 86400;

fn format_duration(seconds: time_t) -> CString {
    let hours = seconds / 3600;
    let mins = (seconds % 3600) / 60;

    let mut buf = [0u8; 16];

    unsafe {
        if hours > 0 {
            pebble_sys::snprintf(
                buf.as_mut_ptr() as *mut _,
                16,
                c"%dh %dm".as_ptr(),
                hours,
                mins,
            );
        } else {
            pebble_sys::snprintf(buf.as_mut_ptr() as *mut _, 16, c"%dm".as_ptr(), mins);
        }
    }

    let c_str = CStr::from_bytes_until_nul(&buf).unwrap();
    CString::from(c_str)
}

fn format_day(day_index: i32) -> CString {
    let mut buf = [0u8; 16];

    unsafe {
        match day_index {
            0 => {
                pebble_sys::snprintf(buf.as_mut_ptr() as *mut _, 16, c"Today".as_ptr());
            }
            1 => {
                pebble_sys::snprintf(buf.as_mut_ptr() as *mut _, 16, c"Yesterday".as_ptr());
            }
            _ => {
                pebble_sys::snprintf(
                    buf.as_mut_ptr() as *mut _,
                    16,
                    c"%d days ago".as_ptr(),
                    day_index,
                );
            }
        };
    }

    let c_str = CStr::from_bytes_until_nul(&buf).unwrap();
    CString::from(c_str)
}

struct RowUI {
    _container: CanvasLayer,
    _day_label: TextLayer,
    _duration_label: TextLayer,
}

pub struct HistoryScrollHandler;

impl ScrollDelegate for HistoryScrollHandler {}

impl ClickDelegate for HistoryScrollHandler {
    fn click_config(&self, config: &ClickConfigurator<Self>) {
        config.subscribe_single_click(ButtonId::BUTTON_ID_SELECT);
    }

    fn on_single_click(&self, _recognizer: ClickRecognizer) {
        window_manager::push(settings_window::create(), true);
    }
}

pub struct HistoryScreen {
    scroll_layer: RefCell<Option<ScrollLayer<HistoryScrollHandler>>>,
    rows: RefCell<Vec<RowUI>>,
}

impl HistoryScreen {
    fn calculate_day_total(
        day_start: time_t,
        day_end: time_t,
        is_today: bool,
        now: time_t,
    ) -> time_t {
        let mut total = 0;
        let history = HISTORY.borrow();

        for session in history.iter() {
            if session.start < day_end && session.end > day_start {
                total += min(session.end, day_end) - max(session.start, day_start);
            }
        }

        if is_today {
            if let Some(start) = CURRENT_START_TIME.get() {
                total += now - start;
            }
        }

        total
    }

    fn build_row_ui(
        day_index: i32,
        row_y: i16,
        bounds_w: i16,
        day_start: time_t,
        day_end: time_t,
        day_total_seconds: time_t,
    ) -> RowUI {
        let container = CanvasLayer::new(
            Rect::new(Point::new(0, row_y), Size::new(bounds_w, ROW_HEIGHT)),
            move |layer, ctx| {
                let bounds = layer.get_bounds();
                let bar_x: i16 = 10;
                let bar_y: i16 = 30;
                let bar_h: i16 = 12;
                let max_bar_w = bounds.size.w - (bar_x * 2);

                ctx.set_fill_color(Color::DARK_GRAY);
                ctx.fill_rect(
                    Rect::new(Point::new(bar_x, bar_y), Size::new(max_bar_w, bar_h)),
                    4,
                    GCornerMask::GCornersAll,
                );

                ctx.set_fill_color(Color::GREEN);

                let history = HISTORY.borrow();
                let mut active_sessions = history.clone();
                let now = pebble::std::time::get_time();

                if let Some(start) = CURRENT_START_TIME.get() {
                    active_sessions.push(TimePair { start, end: now });
                }

                for session in active_sessions.iter() {
                    if session.start < day_end && session.end > day_start {
                        let overlap_start = max(session.start, day_start);
                        let overlap_end = min(session.end, day_end);

                        let start_px =
                            ((overlap_start - day_start) * (max_bar_w as time_t)) / SECONDS_PER_DAY;
                        let end_px =
                            ((overlap_end - day_start) * (max_bar_w as time_t)) / SECONDS_PER_DAY;
                        let width_px = max(2, end_px - start_px);

                        ctx.fill_rect(
                            Rect::new(
                                Point::new(bar_x + start_px as i16, bar_y),
                                Size::new(width_px as i16, bar_h),
                            ),
                            2,
                            GCornerMask::GCornersAll,
                        );
                    }
                }
            },
        );

        let mut day_label = TextLayer::new(Rect::new(Point::new(10, 0), Size::new(100, 24)));
        day_label.set_text_color(Color::WHITE);
        day_label.set_background_color(Color::CLEAR);
        day_label.set_text(format_day(day_index));

        let mut dur_label =
            TextLayer::new(Rect::new(Point::new(bounds_w - 80, 0), Size::new(70, 24)));
        dur_label.set_text_alignment(GTextAlignment::GTextAlignmentRight);
        dur_label.set_text_color(Color::GREEN);
        dur_label.set_background_color(Color::CLEAR);
        dur_label.set_text(format_duration(day_total_seconds));

        container.add_child(&day_label);
        container.add_child(&dur_label);

        RowUI {
            _container: container,
            _day_label: day_label,
            _duration_label: dur_label,
        }
    }
}

impl WindowDelegate for HistoryScreen {
    fn load(&self, window: WindowRef) {
        window.set_background_color(Color::BLACK);
        let bounds = window.get_root_layer().get_bounds();

        let scroll = ScrollLayer::new(bounds, HistoryScrollHandler);
        scroll.set_click_config_onto_window(&window);
        scroll.enable_clicks_override();
        scroll.set_content_size(Size::new(bounds.size.w, DISPLAYED_DAYS as i16 * ROW_HEIGHT));
        window.get_root_layer().add_child(&scroll);

        let now = pebble::std::time::get_time();
        let today_start = now - (now % SECONDS_PER_DAY);
        let mut ui_rows = Vec::with_capacity(DISPLAYED_DAYS as usize);

        for i in 0..DISPLAYED_DAYS {
            let day_start = today_start - (i as time_t * SECONDS_PER_DAY);
            let day_end = day_start + SECONDS_PER_DAY;
            let row_y = i as i16 * ROW_HEIGHT;

            let day_total_seconds = Self::calculate_day_total(day_start, day_end, i == 0, now);
            let row_ui = Self::build_row_ui(
                i,
                row_y,
                bounds.size.w,
                day_start,
                day_end,
                day_total_seconds,
            );

            scroll.add_scroll_child(&row_ui._container);
            ui_rows.push(row_ui);
        }

        *self.rows.borrow_mut() = ui_rows;
        *self.scroll_layer.borrow_mut() = Some(scroll);
    }

    fn unload(&self, window: WindowRef) {
        *self.rows.borrow_mut() = Vec::new();
        *self.scroll_layer.borrow_mut() = None;
        release(window);
    }
}

pub fn create() -> AppWindow {
    AppWindow::History(Window::new(HistoryScreen {
        scroll_layer: RefCell::new(None),
        rows: RefCell::new(Vec::new()),
    }))
}
