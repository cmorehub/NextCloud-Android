package com.askey.qbee.atv.ui.login

import com.askey.qbee.atv.data.LoginRepository

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
    val success: LoginRepository.User? = null,
    val error: Exception? = null
)
