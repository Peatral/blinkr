use crate::pebble_ext::internal::{functions, types};
use core::ffi::c_void;
use pebble_rust::layer::Layer;
use pebble_rust::pebble::WindowPtr;
use pebble_rust::types::GRect;

pub use crate::pebble_ext::internal::types::{MenuIndex, MenuLayerCallbacks};

#[derive(Copy, Clone)]
pub struct MenuLayer {
    internal: *mut types::MenuLayer,
}

impl MenuLayer {
    pub fn new(bounds: GRect) -> MenuLayer {
        unsafe {
            MenuLayer {
                internal: functions::menu_layer_create(bounds),
            }
        }
    }

    pub fn set_callbacks(&self, callbacks: types::MenuLayerCallbacks) {
        unsafe {
            functions::menu_layer_set_callbacks(
                self.internal as *mut c_void,
                core::ptr::null_mut(),
                callbacks,
            );
        }
    }

    pub fn set_click_config_onto_window(&self, window: WindowPtr) {
        unsafe {
            functions::menu_layer_set_click_config_onto_window(
                self.internal,
                window as *mut c_void,
            );
        }
    }

    pub fn get_layer(&self) -> Layer {
        unsafe {
            let layer_ptr = functions::menu_layer_get_layer(self.internal);
            Layer::from_raw(layer_ptr as *mut _)
        }
    }

    pub fn reload_data(&self) {
        unsafe {
            functions::menu_layer_reload_data(self.internal);
        }
    }

    pub fn destroy(&self) {
        unsafe {
            functions::menu_layer_destroy(self.internal);
        }
    }
}

pub fn cell_basic_draw(
    ctx: *mut c_void,
    layer: *const c_void,
    title: *const u8,
    subtitle: *const u8,
) {
    unsafe {
        functions::menu_cell_basic_draw(ctx, layer, title, subtitle, core::ptr::null_mut());
    }
}
