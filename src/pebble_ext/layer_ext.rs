use crate::pebble_ext::internal::functions;
use core::ffi::c_void;
use pebble_rust::layer::{ILayer, TextLayer};

pub use crate::pebble_ext::internal::types::GTextAlignment;

pub trait TextLayerExt {
    fn set_text_alignment(&self, alignment: GTextAlignment);
}

impl TextLayerExt for TextLayer {
    fn set_text_alignment(&self, alignment: GTextAlignment) {
        unsafe {
            functions::text_layer_set_text_alignment(
                self.get_internal() as *mut c_void,
                alignment as u32,
            );
        }
    }
}
