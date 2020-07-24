package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.RemoteItRestTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.network.model.RemoteItRest
import com.nextcloud.qbee.network.model.RemoteItUser
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_welcome)
        val logged = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "") != ""
        if (logged) {
            doLogin()
        } else {
            Handler().postDelayed(Runnable {
                var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                startActivity(intent)
                this@WelcomeActivity.finish()
            }, 3000)
        }
    }

    private fun doLogin() {
        val acctmail = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "")
        val pass = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_PWD, "")
        QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
            override fun onResult(result: QBeeSetupResult) {
                if (result.success) {
                    val devices = result.result as JSONArray
                    if (devices.length() > 0) {
                        RemoteItRestTask(RemoteItRest.loginUrl, object : RemoteItRestTask.Callback {
                            override fun onResult(result: RemoteItRest) {
                                if (result.success) {
                                    val logeduser = result.result as JSONObject
                                    val remotruser = RemoteItUser(logeduser.getString("developer_key"), logeduser.getString("token"))
                                    val device = devices[0] as JSONObject
                                    val devicemac = device.getString("mac").toLowerCase()
                                    val deviceremote = device.getString("remote")
                                    Log.d("0716", "deviceremote=$deviceremote")
                                    RemoteItRestTask(RemoteItRest.deviceListUrl, object : RemoteItRestTask.Callback {
                                        override fun onResult(result: RemoteItRest) {
                                            if (result.success) {
                                                var deviceRemoteIdAddr = ""
                                                val resultJson = result.result as JSONObject
                                                val devicesListArr = resultJson.getJSONArray("devices")
                                                for (i in 0 until devicesListArr.length()) {
                                                    val deviceJson = devicesListArr[i] as JSONObject
                                                    if (deviceJson.getString("servicetitle") == "NextCloud") {
                                                        if (deviceJson.getString("devicealias").contains(devicemac, true)
                                                            || deviceJson.getString("devicealias") == deviceremote) {
                                                            deviceRemoteIdAddr = deviceJson.getString("deviceaddress")
                                                            break
                                                        }
                                                    }
                                                }
                                                Log.d("0716", "deviceRemoteIdAddr:$deviceRemoteIdAddr")
                                                if (deviceRemoteIdAddr != "") {
                                                    RemoteItRestTask(RemoteItRest.deviceProxyUrl, object : RemoteItRestTask.Callback {
                                                        override fun onResult(result: RemoteItRest) {
                                                            if (result.success) {
                                                                Thread {
                                                                    val devicedata = result.result as JSONObject
                                                                    val connectInfo = devicedata.getJSONObject("connection")
                                                                    val deviceProxy = connectInfo.getString("proxy")
//                                                                    val url = Uri.parse("http://iottalk.cmoremap.com.tw:6325")
//                                                                    val loginName: String = "askey"
//                                                                    val password: String = "askeyqbee"
                                                                    val url = Uri.parse(deviceProxy.replace("http", "https"))
                                                                    val loginName: String = "admin"
                                                                    val password: String = "admin"

                                                                    val accountManager = AccountManager.get(this@WelcomeActivity)
                                                                    val accountName = AccountUtils.buildAccountName(url, loginName)
                                                                    val accounts = accountManager.getAccountsByType("nextcloud")
                                                                    val newAccount = if (accounts.isEmpty()) {
                                                                        val account = Account(accountName, "nextcloud")
                                                                        accountManager.addAccountExplicitly(account, password, null)
                                                                        accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                                                                        accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                                                                        account
                                                                    } else {
                                                                        val account = accounts[0]
                                                                        accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                                                                        accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                                                                        account
                                                                    }

                                                                    val manager = OwnCloudClientManager()
                                                                    val account = OwnCloudAccount(newAccount, this@WelcomeActivity)

                                                                    val client = manager.getClientFor(account, this@WelcomeActivity)
                                                                    val loginSuccess = loginName == client.userId
                                                                    val i = Intent(this@WelcomeActivity, FileDisplayActivity::class.java)
                                                                    i.action = FileDisplayActivity.RESTART
                                                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                                    startActivity(i)
                                                                    this@WelcomeActivity.finish()
                                                                }.start()
                                                            } else {
                                                                val errorJson = result.result as JSONObject
                                                                val reason = errorJson.getString("reason")
                                                                Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                                var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                                                startActivity(intent)
                                                                this@WelcomeActivity.finish()
                                                            }
                                                        }
                                                    }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                                                        RemoteItRest.getApiMethod("POST"),
                                                        RemoteItRest.getApiDeviceProxyString(deviceRemoteIdAddr, true, "0.0.0.0"))
                                                } else {
                                                    Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                    var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                                    startActivity(intent)
                                                    this@WelcomeActivity.finish()
                                                }
                                            } else {
                                                Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                                startActivity(intent)
                                                this@WelcomeActivity.finish()
                                            }
                                        }
                                    }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                                        RemoteItRest.getApiMethod("GET"))
                                } else {
                                    val errorJson = result.result as JSONObject
                                    val reason = errorJson.getString("reason")
                                    Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                    var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                    startActivity(intent)
                                    this@WelcomeActivity.finish()
                                }
                            }
                        }).execute(RemoteItRest.getApiLoginHeader("QzlEQ0JDQzItNjYyMC00RjVCLUIwOTgtQkFBQkNCMzgxRUFG"),
                            RemoteItRest.getApiMethod("POST"),
                            RemoteItRest.getApiLoginString("ccmaped@gmail.com", "maped1234"))
                    } else {
                        var intent = Intent(this@WelcomeActivity, QBeeSetupActivity::class.java)
                        intent.putExtra("mail", acctmail)
                        intent.putExtra("pwd", pass)
                        startActivity(intent)
                        this@WelcomeActivity.finish()
                    }
                } else {
                    val errorMsg = result.result as String
                    Toast.makeText(this@WelcomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                    AlertDialog.Builder(this@WelcomeActivity)
                        .setTitle("Connect Error")
                        .setMessage(errorMsg)
                        .setPositiveButton("OK") { dialog, which ->
//                            var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
//                            startActivity(intent)
                            this@WelcomeActivity.finish()
                        }
                        .create().show()
                }
            }
        }).execute(ApiQBeeBind.getApiLoginString(acctmail!!, pass!!))
    }
}
