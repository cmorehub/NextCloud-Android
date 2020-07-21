package com.nextcloud.qbee.ui.login

import com.nextcloud.qbee.data.LoginRepository

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
    val success: LoginRepository.User? = null,
    val error: Exception? = null
)
