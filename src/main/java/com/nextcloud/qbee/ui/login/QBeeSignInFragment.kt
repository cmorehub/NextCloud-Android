package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.RemoteItRestTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.network.model.RemoteItRest
import com.nextcloud.qbee.network.model.RemoteItUser
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.event.SetupFinishEvent
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject

class QBeeSignInFragment : Fragment() {

    lateinit var editTextTextEmailAddress2: EditText
    lateinit var editTextTextPassword: EditText
    lateinit var btnSignIn: Button
    lateinit var btnSignUp: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_signin, container, false)

        editTextTextEmailAddress2 = root.findViewById(R.id.editTextTextEmailAddress2)
        editTextTextPassword = root.findViewById(R.id.editTextTextPassword)
        btnSignIn = root.findViewById(R.id.btnSignIn)
        btnSignUp = root.findViewById(R.id.btnSignUp)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireContext().getSharedPreferences(QBEE_LOGIN_DATA, 0).edit().clear().apply()
        val navController = NavHostFragment.findNavController(this)
        btnSignUp.setOnClickListener {
            val action = QBeeSignInFragmentDirections.actionQBeeSignInFragmentToQBeeSignUpFragment()
            navController.navigate(action)
        }
        btnSignIn.setOnClickListener {
            val acctmail = editTextTextEmailAddress2.text.toString()
            val pass = editTextTextPassword.text.toString()
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
                                        RemoteItRestTask(RemoteItRest.deviceListUrl, object : RemoteItRestTask.Callback {
                                            override fun onResult(result: RemoteItRest) {
                                                if (result.success) {
                                                    var deviceRemoteIdAddr = ""
                                                    val resultJson = result.result as JSONObject
                                                    val devicesListArr = resultJson.getJSONArray("devices")
                                                    for (i in 0 until devicesListArr.length()) {
                                                        val deviceJson = devicesListArr[i] as JSONObject
                                                        if (deviceJson.getString("servicetitle") == "NextCloud") {
                                                            if (deviceJson.getString("devicealias").contains(devicemac,true)
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
                                                                    Thread{
                                                                        val devicedata = result.result as JSONObject
                                                                        val connectInfo = devicedata.getJSONObject("connection")
                                                                        val deviceProxy = connectInfo.getString("proxy")
                                                                        val url = Uri.parse(deviceProxy.replace("http", "https"))
                                                                        val loginName: String = "admin"
                                                                        val password: String = "admin"

                                                                        val accountManager = AccountManager.get(context)
                                                                        val accountName = AccountUtils.buildAccountName(url, loginName)
                                                                        val newAccount = Account(accountName, "nextcloud")

                                                                        accountManager.addAccountExplicitly(newAccount, password, null)
                                                                        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                                                                        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

                                                                        val manager = OwnCloudClientManager()
                                                                        val account = OwnCloudAccount(newAccount, context)

                                                                        val client = manager.getClientFor(account, context)
                                                                        val loginSuccess = loginName == client.userId
                                                                        EventBus.getDefault().post(LoginFinishEvent(loginSuccess, newAccount.name, "", LoginFinishEvent.LoginForCloud))
                                                                        EventBus.getDefault().post(QBeeLoginEvent(acctmail, pass))
                                                                    }.start()
                                                                } else {
                                                                    val errorJson = result.result as JSONObject
                                                                    val reason = errorJson.getString("reason")
                                                                    Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                                }

                                                            }
                                                        }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                                                            RemoteItRest.getApiMethod("POST"),
                                                            RemoteItRest.getApiDeviceProxyString(deviceRemoteIdAddr, true, "0.0.0.0"))
                                                    } else {
                                                        Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                                            RemoteItRest.getApiMethod("GET"))
                                    } else {
                                        val errorJson = result.result as JSONObject
                                        val reason = errorJson.getString("reason")
                                        Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }).execute(RemoteItRest.getApiLoginHeader("QzlEQ0JDQzItNjYyMC00RjVCLUIwOTgtQkFBQkNCMzgxRUFG"),
                                RemoteItRest.getApiMethod("POST"),
                                RemoteItRest.getApiLoginString("ccmaped@gmail.com", "maped1234"))
                        } else {
                            Thread {
                                EventBus.getDefault().post(LoginFinishEvent(true, editTextTextEmailAddress2.text.toString(), editTextTextPassword
                                    .text.toString(), LoginFinishEvent
                                    .LoginForSetup))
                                EventBus.getDefault().post(QBeeLoginEvent(acctmail, pass))
                            }.start()
                        }
                    } else {
                        Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                    }
                }
            }).execute(ApiQBeeBind.getApiLoginString(acctmail, pass))
        }
    }
}
