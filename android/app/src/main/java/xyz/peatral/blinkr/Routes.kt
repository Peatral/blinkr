package xyz.peatral.blinkr

import kotlinx.serialization.Serializable

@Serializable
object Overview

@Serializable
data class DayBreakdown(val dateString: String)