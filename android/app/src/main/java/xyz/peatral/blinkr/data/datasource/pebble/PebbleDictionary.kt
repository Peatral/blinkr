package xyz.peatral.blinkr.data.datasource.pebble

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem

fun PebbleDictionary.getInt(key: UInt): Int? {
    return (this[key] as? PebbleDictionaryItem.Int32)?.value
        ?: (this[key] as? PebbleDictionaryItem.UInt32)?.value?.toInt()
}

fun PebbleDictionary.getLong(key: UInt): Long? {
    return (this[key] as? PebbleDictionaryItem.Int32)?.value?.toLong()
        ?: (this[key] as? PebbleDictionaryItem.UInt32)?.value?.toLong()
}

fun PebbleDictionary.getBytes(key: UInt): ByteArray? {
    return (this[key] as? PebbleDictionaryItem.Bytes)?.value
}