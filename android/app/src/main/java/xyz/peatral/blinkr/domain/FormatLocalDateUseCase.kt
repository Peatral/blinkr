package xyz.peatral.blinkr.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.minus
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import xyz.peatral.blinkr.R
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Clock

@OptIn(FormatStringsInDatetimeFormats::class)
class FormatLocalDateUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(date: LocalDate): String {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date
        return if (date == today) {
            context.getString(R.string.today)
        } else if (date == today.minus(DatePeriod(days = 1))) {
            context.getString(R.string.yesterday)
        } else if (date > today.minus(DatePeriod(days = 7))) {
            context.getString(R.string.n_days_ago, date.periodUntil(today).days)
        } else {
            // TODO: The java LocalDate handles the Locale. is that a good idea? who knows
            if (date.year == today.year) {
                date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
            } else {
                date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            }
        }
    }
}