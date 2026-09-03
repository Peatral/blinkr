package xyz.peatral.blinkr.data.room

import androidx.room.TypeConverter
import kotlin.time.Instant

class DateTimeConverters {

    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(millis: Long): Instant {
        return Instant.fromEpochMilliseconds(millis)
    }
}