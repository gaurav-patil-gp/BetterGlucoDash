package com.eggyswarehouse.betterglucodash.data.network

/**
 * Canonical LibreLinkUp regional configuration.
 *
 * Replaces the stringly-typed `LLU_HOSTS` object and `region == "CA"` comparisons
 * throughout the codebase. Each [Region] encodes the API host, regional unit system,
 * and display label for a supported Abbott market.
 *
 * @property code     Two-letter market code stored in [AuthManager] and Room entities.
 * @property apiHost  Bare LibreLinkUp hostname for this region (no scheme prefix).
 * @property isMetric True when the regional standard is mmol/L (Canada, EU, etc.).
 */
enum class Region(val code: String, val apiHost: String, val isMetric: Boolean) {
    US("US", "api-us.libreview.io", false),
    CA("CA", "api-ca.libreview.io", true)
    ;

    /** Display label for the unit suffix field. */
    val unitLabel: String get() = if (isMetric) "mmol/L" else "mg/dL"

    companion object {
        /**
         * Converts a stored region code (e.g. "CA", "US") to its [Region] constant.
         * Defaults to [US] for null, empty, or unrecognised codes.
         */
        fun fromCode(code: String?): Region = entries.find { it.code.equals(code, ignoreCase = true) } ?: US
    }
}
