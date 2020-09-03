package com.askey.qbee.atv.data

import com.askey.qbee.atv.qbeecom.QBeeDotCom

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    class User(
        val username:String,
        val password:String,
        val device:QBeeDotCom.QBeeDevice?
    )

    // in-memory cache of the loggedInUser object
    var user: User? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    suspend fun login(username: String, password: String, forcePassing: Boolean = false): Result<User> {
        // handle login
        val result = dataSource.login(username, password, forcePassing)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoginUser(user: User){
        this.user = user
    }

    private fun setLoggedInUser(loggedInUser: User) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}
