use pebble::types::Tuple;

/// Safely extracts varying-length integers from Clay payloads
/// without triggering ARM memory alignment faults.
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
