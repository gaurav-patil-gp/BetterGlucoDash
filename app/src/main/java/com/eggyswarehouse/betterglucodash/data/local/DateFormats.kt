package com.eggyswarehouse.betterglucodash.data.local

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared, thread-safe formatter for Abbott's FactoryTimestamp field.
 *
 * Example timestamp: `"10/24/2023 2:45:00 PM"`
 *
 * [DateTimeFormatter] is immutable and safe for concurrent use — unlike
 * [java.text.SimpleDateFormat] which is stateful and was causing data races in
 * [LibreRepository][com.eggyswarehouse.betterglucodash.data.repository.LibreRepository]
 * and [DashboardViewModel][com.eggyswarehouse.betterglucodash.ui.dashboard.DashboardViewModel].
 */
val LIBRE_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US)
