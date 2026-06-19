use crate::state::{CURRENT_START_TIME, HISTORY, TimePair};
use crate::utils::{format_duration, format_time_range};
use crate::window_manager::{AppWindow, release};
use alloc::ffi::CString;
use alloc::vec::Vec;
use core::cell::RefCell;
use core::cmp::{max, min};
use pebble::graphics::context::Context;
use pebble::graphics::types::{Color, Point, Rect, Size};
use pebble::layer::menu_layer::MenuCellLayer;
use pebble::layer::{ILayer, ILayerMut, MenuIndexRef, MenuLayer, MenuLayerDelegate, MenuLayerRef};
use pebble::std::time;
use pebble::system::fonts::{FONT_KEY_GOTHIC_18_BOLD, FONT_KEY_GOTHIC_24_BOLD, Font};
use pebble::window::{Window, WindowDelegate, WindowRef};
use pebble_sys::{GTextAlignment, GTextOverflowMode, time_t};

const SECONDS_PER_DAY: time_t = 86400;

#[derive(Clone)]
pub enum TimelineItem {
    Session {
        start: time_t,
        end: time_t,
        is_active: bool,
    },
    Break {
        start: time_t,
        end: time_t,
    },
    Empty,
}

pub struct DayBreakdownMenu {
    pub items: RefCell<Vec<TimelineItem>>,
    pub bounds_w: i16,
    pub day_index: i32,
}

impl DayBreakdownMenu {
    fn update_items(&self) {
        let now = time::get_time();
        let today_start = time::start_of_today();
        let day_start = today_start - (self.day_index as time_t * SECONDS_PER_DAY);
        let day_end = day_start + SECONDS_PER_DAY;

        let history = HISTORY.borrow();
        let current_start = CURRENT_START_TIME.get();

        let mut day_sessions = Vec::new();
        let mut process_session = |s: &TimePair| {
            if s.start < day_end && s.end > day_start {
                let is_active = Some(s.start) == current_start;
                day_sessions.push((
                    TimePair {
                        start: max(s.start, day_start),
                        end: min(s.end, day_end),
                    },
                    is_active,
                ));
            }
        };

        for s in history.iter() {
            process_session(s);
        }
        if let Some(start) = current_start {
            process_session(&TimePair { start, end: now });
        }
        day_sessions.sort_by_key(|s| s.0.start);

        let mut new_items = Vec::new();
        for (i, &(session, is_active)) in day_sessions.iter().enumerate() {
            if i > 0 {
                let prev_end = day_sessions[i - 1].0.end;
                if session.start > prev_end {
                    new_items.push(TimelineItem::Break {
                        start: prev_end,
                        end: session.start,
                    });
                }
            }
            new_items.push(TimelineItem::Session {
                start: session.start,
                end: session.end,
                is_active,
            });
        }

        if new_items.is_empty() {
            new_items.push(TimelineItem::Empty);
        }

        *self.items.borrow_mut() = new_items;
    }
}

impl MenuLayerDelegate for DayBreakdownMenu {
    fn get_num_rows(&self, _menu_layer: MenuLayerRef, _section_index: u16) -> u16 {
        self.update_items();
        self.items.borrow().len() as u16
    }

    fn get_cell_height(&self, _menu_layer: MenuLayerRef, _cell_index: MenuIndexRef) -> i16 {
        50
    }

    fn draw_row(&self, ctx: Context, cell_layer: MenuCellLayer, index: MenuIndexRef) {
        let items = self.items.borrow();
        let item = &items[index.row() as usize];
        let is_highlighted = cell_layer.is_highlighted();

        let font_title = Font::get_system(FONT_KEY_GOTHIC_24_BOLD);
        let font_sub = Font::get_system(FONT_KEY_GOTHIC_18_BOLD);
        let text_color = if is_highlighted {
            Color::BLACK
        } else {
            Color::WHITE
        };

        match item {
            TimelineItem::Empty => {
                ctx.set_text_color(text_color);
                ctx.draw_text(
                    c"No sessions yet",
                    &font_title,
                    Rect::new(Point::new(0, 8), Size::new(self.bounds_w, 30)),
                    GTextOverflowMode::GTextOverflowModeWordWrap,
                    GTextAlignment::GTextAlignmentCenter,
                    None,
                );
            }
            TimelineItem::Session {
                start,
                end,
                is_active,
            } => {
                let title = c"Session";
                let color_accent = Color::RED;
                let sub_color = if is_highlighted {
                    Color::BLACK
                } else {
                    color_accent
                };
                let dur_text = format_duration(*end - *start);
                let time_text = if *is_active {
                    let tm_s = time::get_local_time(*start);
                    let h_s = tm_s.tm_hour;
                    let m_s = tm_s.tm_min;
                    pebble::pbl_fmt!(let f = c"%02d:%02d - --:--", h_s, m_s);
                    CString::from(f)
                } else {
                    format_time_range(*start, *end)
                };

                ctx.set_text_color(text_color);
                ctx.draw_text(
                    title,
                    &font_title,
                    Rect::new(Point::new(10, 0), Size::new(100, 24)),
                    GTextOverflowMode::GTextOverflowModeWordWrap,
                    GTextAlignment::GTextAlignmentLeft,
                    None,
                );

                ctx.set_text_color(sub_color);
                ctx.draw_text(
                    &time_text,
                    &font_sub,
                    Rect::new(Point::new(10, 26), Size::new(100, 20)),
                    GTextOverflowMode::GTextOverflowModeWordWrap,
                    GTextAlignment::GTextAlignmentLeft,
                    None,
                );

                ctx.set_text_color(sub_color);
                ctx.draw_text(
                    &dur_text,
                    &font_title,
                    Rect::new(Point::new(self.bounds_w - 100, 8), Size::new(90, 30)),
                    GTextOverflowMode::GTextOverflowModeTrailingEllipsis,
                    GTextAlignment::GTextAlignmentRight,
                    None,
                );
            }
            TimelineItem::Break { start, end } => {
                let title = c"Break";
                let color_accent = Color::GREEN;
                let sub_color = if is_highlighted {
                    Color::BLACK
                } else {
                    color_accent
                };
                let dur_text = format_duration(*end - *start);
                let time_text = format_time_range(*start, *end);

                ctx.set_text_color(text_color);
                ctx.draw_text(
                    title,
                    &font_title,
                    Rect::new(Point::new(10, 0), Size::new(100, 24)),
                    GTextOverflowMode::GTextOverflowModeWordWrap,
                    GTextAlignment::GTextAlignmentLeft,
                    None,
                );

                ctx.set_text_color(sub_color);
                ctx.draw_text(
                    &time_text,
                    &font_sub,
                    Rect::new(Point::new(10, 26), Size::new(100, 20)),
                    GTextOverflowMode::GTextOverflowModeWordWrap,
                    GTextAlignment::GTextAlignmentLeft,
                    None,
                );

                ctx.set_text_color(sub_color);
                ctx.draw_text(
                    &dur_text,
                    &font_title,
                    Rect::new(Point::new(self.bounds_w - 100, 8), Size::new(90, 30)),
                    GTextOverflowMode::GTextOverflowModeTrailingEllipsis,
                    GTextAlignment::GTextAlignmentRight,
                    None,
                );
            }
        }
    }
}

pub struct DayBreakdownScreen {
    menu_layer: RefCell<Option<MenuLayer<DayBreakdownMenu>>>,
    day_index: i32,
}

impl DayBreakdownScreen {
    pub fn refresh(&self) {
        if let Some(menu) = self.menu_layer.borrow().as_ref() {
            menu.reload_data();
        }
    }
}

impl WindowDelegate for DayBreakdownScreen {
    fn load(&self, window: WindowRef) {
        window.set_background_color(Color::BLACK);
        let bounds = window.get_root_layer().get_bounds();

        let menu = MenuLayer::new(
            bounds,
            DayBreakdownMenu {
                items: RefCell::new(Vec::new()),
                bounds_w: bounds.size.w,
                day_index: self.day_index,
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

pub fn create(day_index: i32) -> AppWindow {
    AppWindow::DayBreakdown(Window::new(DayBreakdownScreen {
        menu_layer: RefCell::new(None),
        day_index,
    }))
}
