use crate::pebble::window::{WindowDelegate, WindowRef};
use crate::state::{CURRENT_START_TIME, HISTORY, TimePair};
use crate::ui::settings_window;
use crate::window_manager;
use crate::window_manager::{AppWindow, release};
use alloc::ffi::CString;
use core::cell::RefCell;
use core::cmp::{max, min};
use pebble::graphics::context::Context;
use pebble::graphics::types::{Color, Point, Rect, Size};
use pebble::layer::menu_layer::MenuCellLayer;
use pebble::layer::{ILayer, ILayerMut, MenuIndexRef, MenuLayer, MenuLayerDelegate, MenuLayerRef};
use pebble::std::time;
use pebble::system::fonts::{FONT_KEY_BITHAM_42_BOLD, FONT_KEY_GOTHIC_24_BOLD, Font};
use pebble::window::Window;
use pebble_sys::{GCornerMask, GTextAlignment, GTextOverflowMode, time_t};

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

pub struct HistoryMenu {
    pub bounds_w: i16,
}

impl MenuLayerDelegate for HistoryMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        DISPLAYED_DAYS as u16
    }

    fn get_cell_height(&self, _menu_layer: MenuLayerRef, _cell_index: MenuIndexRef) -> i16 {
        ROW_HEIGHT
    }

    fn get_header_height(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> i16 {
        HEADER_HEIGHT
    }

    fn draw_row(&self, ctx: Context, cell_layer: MenuCellLayer, index: MenuIndexRef) {
        let row = index.row() as i32;
        let now = time::get_time();
        let today_start = time::start_of_today();
        let day_start = today_start - (row as time_t * SECONDS_PER_DAY);
        let day_end = day_start + SECONDS_PER_DAY;
        let day_total_seconds = HistoryScreen::calculate_day_total(day_start, day_end, now);

        let bar_x: i16 = 10;
        let bar_y: i16 = 30;
        let bar_h: i16 = 12;
        let max_bar_w = self.bounds_w - (bar_x * 2);

        ctx.set_fill_color(Color::DARK_GRAY);
        ctx.fill_rect(
            Rect::new(Point::new(bar_x, bar_y), Size::new(max_bar_w, bar_h)),
            4,
            GCornerMask::GCornersAll,
        );

        if now > day_start {
            let end_of_passed_time = min(now, day_end);
            let passed_px =
                ((end_of_passed_time - day_start) * (max_bar_w as time_t)) / SECONDS_PER_DAY;

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
                let end_px = ((overlap_end - day_start) * (max_bar_w as time_t)) / SECONDS_PER_DAY;
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

        let day_text = format_day(row);
        let dur_text = format_duration(day_total_seconds);
        let font = Font::get_system(FONT_KEY_GOTHIC_24_BOLD);

        let is_highlighted = cell_layer.is_highlighted();
        let day_color = if is_highlighted {
            Color::BLACK
        } else {
            Color::WHITE
        };
        let dur_color = if is_highlighted {
            Color::BLACK
        } else {
            Color::GREEN
        };

        ctx.set_text_color(day_color);
        ctx.draw_text(
            &day_text,
            &font,
            Rect::new(Point::new(10, 0), Size::new(100, 24)),
            GTextOverflowMode::GTextOverflowModeWordWrap,
            GTextAlignment::GTextAlignmentLeft,
            None,
        );

        ctx.set_text_color(dur_color);
        ctx.draw_text(
            &dur_text,
            &font,
            Rect::new(Point::new(self.bounds_w - 80, 0), Size::new(70, 24)),
            GTextOverflowMode::GTextOverflowModeWordWrap,
            GTextAlignment::GTextAlignmentRight,
            None,
        );
    }

    fn draw_header(&self, ctx: Context, _cell_layer: MenuCellLayer, _section_index: u16) {
        let now = time::get_time();
        let today_start = time::start_of_today();
        let grand_total = HistoryScreen::calculate_displayed_total(now, today_start);
        let text = format_duration(grand_total);
        let font = Font::get_system(FONT_KEY_BITHAM_42_BOLD);

        ctx.set_text_color(Color::WHITE);
        ctx.draw_text(
            &text,
            &font,
            Rect::new(Point::new(0, 19), Size::new(self.bounds_w, 42)),
            GTextOverflowMode::GTextOverflowModeWordWrap,
            GTextAlignment::GTextAlignmentCenter,
            None,
        );
    }

    fn select_click(&self, _menu_layer: MenuLayerRef, _index: MenuIndexRef) {
        window_manager::push(settings_window::create(), true);
    }
}

pub struct HistoryScreen {
    menu_layer: RefCell<Option<MenuLayer<HistoryMenu>>>,
}

impl HistoryScreen {
    pub fn calculate_displayed_total(now: time_t, today_start: time_t) -> time_t {
        let mut total = 0;
        for i in 0..DISPLAYED_DAYS {
            let day_start = today_start - (i as time_t * SECONDS_PER_DAY);
            let day_end = day_start + SECONDS_PER_DAY;
            total += Self::calculate_day_total(day_start, day_end, now);
        }
        total
    }

    pub fn calculate_day_total(day_start: time_t, day_end: time_t, now: time_t) -> time_t {
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

    pub fn refresh(&self) {
        if let Some(menu) = self.menu_layer.borrow().as_ref() {
            menu.reload_data();
        }
    }
}

impl WindowDelegate for HistoryScreen {
    fn load(&self, window: WindowRef) {
        window.set_background_color(Color::BLACK);
        let bounds = window.get_root_layer().get_bounds();

        let menu = MenuLayer::new(
            bounds,
            HistoryMenu {
                bounds_w: bounds.size.w,
            },
        );
        menu.set_click_config_onto_window(&window);

        menu.set_normal_colors(Color::BLACK, Color::WHITE);
        menu.set_highlight_colors(Color::WHITE, Color::BLACK);

        window.get_root_layer().add_child(&menu);
        *self.menu_layer.borrow_mut() = Some(menu);
    }

    fn unload(&self, window: WindowRef) {
        *self.menu_layer.borrow_mut() = None;
        release(window);
    }
}

pub fn create() -> AppWindow {
    AppWindow::History(Window::new(HistoryScreen {
        menu_layer: RefCell::new(None),
    }))
}
