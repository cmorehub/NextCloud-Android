package com.nextcloud.qbee.data

import android.accounts.AuthenticatorException
import android.util.Log
import com.nextcloud.qbee.data.model.LoggedInUser
import com.nextcloud.qbee.qbeecom.QBeeDotCom
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    suspend fun login(username: String, password: String): Result<LoginRepository.User> {
        try {
            val loginQBeeDotCom = QBeeDotCom.login(username, password)
            return if(loginQBeeDotCom.result!=0){
                if(loginQBeeDotCom is QBeeDotCom.WusiungError)
                    Result.Error(AuthenticatorException(loginQBeeDotCom.error))
                else
                    Result.Error(AuthenticatorException("Invalid user or password!!"))
            } else if(loginQBeeDotCom is QBeeDotCom.WusiungResult && loginQBeeDotCom
                    .deviceList.isNullOrEmpty()){
                Result.Error(AuthenticatorException("Not bound with any device!!"))
            } else if(loginQBeeDotCom is QBeeDotCom.WusiungResult && !loginQBeeDotCom.deviceList.isNullOrEmpty()){
                Result.Success(LoginRepository.User(username,password,loginQBeeDotCom.deviceList[0]))
            } else{
                Result.Error(IOException("Unknown Response:$loginQBeeDotCom"))
            }
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

