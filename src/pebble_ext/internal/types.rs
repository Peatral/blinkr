use core::ffi::c_void;

#[repr(C)]
pub struct MenuLayer(c_void);

#[repr(C, packed)]
#[derive(Copy, Clone)]
pub struct MenuIndex {
    pub section: u16,
    pub row: u16,
}

pub type MenuLayerGetNumSectionsCb = extern "C" fn(*mut c_void, *mut c_void) -> u16;
pub type MenuLayerGetNumRowsCb = extern "C" fn(*mut c_void, u16, *mut c_void) -> u16;
pub type MenuLayerGetCellHeightCb = extern "C" fn(*mut c_void, *mut MenuIndex, *mut c_void) -> i16;
pub type MenuLayerGetHeaderHeightCb = extern "C" fn(*mut c_void, u16, *mut c_void) -> i16;
pub type MenuLayerDrawRowCb =
    extern "C" fn(*mut c_void, *const c_void, *mut MenuIndex, *mut c_void);
pub type MenuLayerDrawHeaderCb = extern "C" fn(*mut c_void, *const c_void, u16, *mut c_void);
pub type MenuLayerSelectCb = extern "C" fn(*mut c_void, *mut MenuIndex, *mut c_void);
pub type MenuLayerSelectLongCb = extern "C" fn(*mut c_void, *mut MenuIndex, *mut c_void);
pub type MenuLayerSelectionChangedCb =
    extern "C" fn(*mut c_void, MenuIndex, MenuIndex, *mut c_void);
pub type MenuLayerGetSeparatorHeightCb =
    extern "C" fn(*mut c_void, *mut MenuIndex, *mut c_void) -> i16;
pub type MenuLayerDrawSeparatorCb =
    extern "C" fn(*mut c_void, *const c_void, *mut MenuIndex, *mut c_void);
pub type MenuLayerSelectionWillChangeCb =
    extern "C" fn(*mut c_void, *mut MenuIndex, MenuIndex, *mut c_void);
pub type MenuLayerDrawBackgroundCb = extern "C" fn(*mut c_void, *const c_void, bool, *mut c_void);

#[repr(C)]
pub struct MenuLayerCallbacks {
    pub get_num_sections: Option<MenuLayerGetNumSectionsCb>,
    pub get_num_rows: Option<MenuLayerGetNumRowsCb>,
    pub get_cell_height: Option<MenuLayerGetCellHeightCb>,
    pub get_header_height: Option<MenuLayerGetHeaderHeightCb>,
    pub draw_row: Option<MenuLayerDrawRowCb>,
    pub draw_header: Option<MenuLayerDrawHeaderCb>,
    pub select_click: Option<MenuLayerSelectCb>,
    pub select_long_click: Option<MenuLayerSelectLongCb>,
    pub selection_changed: Option<MenuLayerSelectionChangedCb>,
    pub get_separator_height: Option<MenuLayerGetSeparatorHeightCb>,
    pub draw_separator: Option<MenuLayerDrawSeparatorCb>,
    pub selection_will_change: Option<MenuLayerSelectionWillChangeCb>, // The 13th element
    pub draw_background: Option<MenuLayerDrawBackgroundCb>,
}

#[repr(u32)]
#[derive(PartialEq, Copy, Clone)]
pub enum AppLaunchReason {
    System = 0,
    User = 1,
    Phone = 2,
    Wakeup = 3,
    Worker = 4,
    QuickLaunch = 5,
    TimelineAction = 6,
    Smartstrap = 7,
}

#[repr(u32)]
pub enum GTextAlignment {
    Left = 0,
    Center = 1,
    Right = 2,
}
