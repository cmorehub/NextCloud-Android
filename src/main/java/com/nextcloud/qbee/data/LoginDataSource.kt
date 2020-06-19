package com.nextcloud.qbee.data

import android.accounts.AuthenticatorException
import com.nextcloud.qbee.data.model.LoggedInUser
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            if(username!="qbee@askey.com"||password!="askeyqbee"){
                return Result.Error(AuthenticatorException("Invalid user or password!!"))
            } else{
                val fakeUser = LoggedInUser(java.util.UUID.randomUUID().toString(), "Jane Doe")
                return Result.Success(fakeUser)
            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

