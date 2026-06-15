use crate::pebble::window::{WindowDelegate, WindowRef};
use crate::state::{CURRENT_START_TIME, HISTORY, TimePair};
use crate::ui::settings_window;
use crate::window_manager;
use crate::window_manager::{AppWindow, release};
use alloc::ffi::CString;
use alloc::vec::Vec;
use core::cell::RefCell;
use core::cmp::{max, min};
use pebble::clicks::{ClickConfigurator, ClickDelegate, ClickRecognizer};
use pebble::graphics::types::{Color, Point, Rect, Size};
use pebble::layer::{CanvasLayer, ILayer, ILayerMut, ScrollDelegate, ScrollLayer, TextLayer};
use pebble::std::time;
use pebble::system::fonts::{FONT_KEY_BITHAM_42_BOLD, Font};
use pebble::window::Window;
use pebble_sys::{ButtonId, GCornerMask, GTextAlignment, time_t};

const DISPLAYED_DAYS: i32 = 7;
const ROW_HEIGHT: i16 = 50;
const SECONDS_PER_DAY: time_t = 86400;
const HEADER_HEIGHT: i16 = 80;

fn format_duration(seconds: time_t) -> CString {
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

fn format_day(day_index: i32) -> CString {
    match day_index {
        0 => CString::from(c"Today"),
        1 => CString::from(c"Yesterday"),
        _ => {
            pebble::pbl_fmt!(let formatted = c"%d days ago", day_index);
            CString::from(formatted)
        }
    }
}

struct RowUI {
    container: CanvasLayer,
    _day_label: TextLayer,
    duration_label: TextLayer,
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
    header_label: RefCell<Option<TextLayer>>,
}

impl HistoryScreen {
    fn calculate_displayed_total(now: time_t, today_start: time_t) -> time_t {
        let mut total = 0;
        for i in 0..DISPLAYED_DAYS {
            let day_start = today_start - (i as time_t * SECONDS_PER_DAY);
            let day_end = day_start + SECONDS_PER_DAY;
            total += Self::calculate_day_total(day_start, day_end, now);
        }
        total
    }

    fn calculate_day_total(day_start: time_t, day_end: time_t, now: time_t) -> time_t {
        let mut total = 0;
        let history = HISTORY.borrow();

        for session in history.iter() {
            if session.start < day_end && session.end > day_start {
                total += min(session.end, day_end) - max(session.start, day_start);
            }
        }

        if let Some(start) = CURRENT_START_TIME.get() {
            if start < day_end && now > day_start {
                total += min(now, day_end) - max(start, day_start);
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

                let now = time::get_time();

                ctx.set_fill_color(Color::DARK_GRAY);
                ctx.fill_rect(
                    Rect::new(Point::new(bar_x, bar_y), Size::new(max_bar_w, bar_h)),
                    4,
                    GCornerMask::GCornersAll,
                );

                if now > day_start {
                    let end_of_passed_time = min(now, day_end);
                    let passed_px = ((end_of_passed_time - day_start) * (max_bar_w as time_t))
                        / SECONDS_PER_DAY;

                    ctx.set_fill_color(Color::GREEN);
                    ctx.fill_rect(
                        Rect::new(Point::new(bar_x, bar_y), Size::new(passed_px as i16, bar_h)),
                        4,
                        GCornerMask::GCornersAll,
                    );
                }

                ctx.set_fill_color(Color::RED);

                let history = HISTORY.borrow();
                let mut active_sessions = history.clone();

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
            container,
            _day_label: day_label,
            duration_label: dur_label,
        }
    }

    pub fn refresh(&self) {
        let now = time::get_time();
        let today_start = time::start_of_today();

        let grand_total = Self::calculate_displayed_total(now, today_start);
        if let Some(header) = self.header_label.borrow_mut().as_mut() {
            header.set_text(format_duration(grand_total));
            header.mark_dirty();
        }

        let mut rows = self.rows.borrow_mut();
        for (i, row) in rows.iter_mut().enumerate() {
            let day_start = today_start - (i as time_t * SECONDS_PER_DAY);
            let day_end = day_start + SECONDS_PER_DAY;
            let day_total_seconds = Self::calculate_day_total(day_start, day_end, now);

            row.duration_label
                .set_text(format_duration(day_total_seconds));

            row.container.mark_dirty();
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
        scroll.set_content_size(Size::new(
            bounds.size.w,
            HEADER_HEIGHT + DISPLAYED_DAYS as i16 * ROW_HEIGHT,
        ));
        window.get_root_layer().add_child(&scroll);

        let now = time::get_time();
        let today_start = time::start_of_today();

        let grand_total = Self::calculate_displayed_total(now, today_start);
        let mut header = TextLayer::new(Rect::new(Point::new(0, 19), Size::new(bounds.size.w, 42)));
        header.set_text_alignment(GTextAlignment::GTextAlignmentCenter);
        header.set_text_color(Color::WHITE);
        header.set_background_color(Color::CLEAR);
        header.set_font(Font::get_system(FONT_KEY_BITHAM_42_BOLD));
        header.set_text(format_duration(grand_total));
        scroll.add_scroll_child(&header);

        let mut ui_rows = Vec::with_capacity(DISPLAYED_DAYS as usize);

        for i in 0..DISPLAYED_DAYS {
            let day_start = today_start - (i as time_t * SECONDS_PER_DAY);
            let day_end = day_start + SECONDS_PER_DAY;
            let row_y = HEADER_HEIGHT + i as i16 * ROW_HEIGHT;

            let day_total_seconds = Self::calculate_day_total(day_start, day_end, now);
            let row_ui = Self::build_row_ui(
                i,
                row_y,
                bounds.size.w,
                day_start,
                day_end,
                day_total_seconds,
            );

            scroll.add_scroll_child(&row_ui.container);
            ui_rows.push(row_ui);
        }

        *self.header_label.borrow_mut() = Some(header);
        *self.rows.borrow_mut() = ui_rows;
        *self.scroll_layer.borrow_mut() = Some(scroll);
    }

    fn unload(&self, window: WindowRef) {
        *self.rows.borrow_mut() = Vec::new();
        *self.scroll_layer.borrow_mut() = None;
        *self.header_label.borrow_mut() = None;
        release(window);
    }
}

pub fn create() -> AppWindow {
    AppWindow::History(Window::new(HistoryScreen {
        scroll_layer: RefCell::new(None),
        rows: RefCell::new(Vec::new()),
        header_label: RefCell::new(None),
    }))
}
