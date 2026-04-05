package com.eggyswarehouse.betterglucodash.ui.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val region: String = "US",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
