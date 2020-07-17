package com.nextcloud.qbee.ui.setup

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.RemoteItRestTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.network.model.RemoteItRest
import com.nextcloud.qbee.network.model.RemoteItUser
import com.nextcloud.qbee.ui.login.QBeeLoginActivity
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.json.JSONObject

class QBeeSetup4Fragment() : Fragment() {

    lateinit var btnConnect: Button
    lateinit var textView21: TextView
    lateinit var textView22: TextView
    lateinit var pgsConnecting: ProgressBar

    lateinit var mail: String
    lateinit var pwd: String
    lateinit var mac: String


    private val args: QBeeSetup4FragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_4, container, false)
        btnConnect = root.findViewById(R.id.btnConnect)
        textView21 = root.findViewById(R.id.textView21)
        textView22 = root.findViewById(R.id.textView22)
        pgsConnecting = root.findViewById(R.id.pgsConnecting)

        mail = args.mail
        pwd = args.pwd
        mac = args.mac

        textView21.text = getString(R.string.qbee_setup_device_id, mac)
        textView22.text = getString(R.string.qbee_setup_device_status, "未配對")

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnConnect.setOnClickListener {
            setConnectState(true)
            RemoteItRestTask(RemoteItRest.loginUrl, object : RemoteItRestTask.Callback {
                override fun onResult(result: RemoteItRest) {
                    if (result.success) {
                        val logeduser = result.result as JSONObject
                        val remotruser = RemoteItUser(logeduser.getString("developer_key"), logeduser.getString("token"))
                        val devicemac = mac.toLowerCase()
                        RemoteItRestTask(RemoteItRest.deviceListUrl, object : RemoteItRestTask.Callback {
                            override fun onResult(result: RemoteItRest) {
                                if (result.success) {
                                    var deviceRemoteIdAddr = ""
                                    val resultJson = result.result as JSONObject
                                    val devicesListArr = resultJson.getJSONArray("devices")
                                    for (i in 0 until devicesListArr.length()) {
                                        val deviceJson = devicesListArr[i] as JSONObject
                                        if (deviceJson.getString("servicetitle") == "NextCloud") {
                                            if (deviceJson.getString("devicealias").contains(devicemac, true)) {
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
                                                    val devicedata = result.result as JSONObject
                                                    val connectInfo = devicedata.getJSONObject("connection")
                                                    val deviceProxy = connectInfo.getString("proxy")
                                                    QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                                                        override fun onResult(result: QBeeSetupResult) {
                                                            if (result.success) {
                                                                val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                                                                var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetup5Fragment(mail!!,
                                                                    pwd!!, mac!!, deviceProxy.replace("http", "https"))
                                                                navController.navigate(action)
                                                            }
                                                            setConnectState(false)
                                                            Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                                                        }
                                                    }).execute(ApiQBeeBind.getApiBindMacString(mail, pwd, mac))
                                                } else {
                                                    setConnectState(false)
                                                    val errorJson = result.result as JSONObject
                                                    val reason = errorJson.getString("reason")
                                                    Log.d("0716", "reason=$reason")
                                                    Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                    val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                                                    var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetupConnectionTypeFragment()
                                                    navController.navigate(action)
                                                }

                                            }
                                        }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                                            RemoteItRest.getApiMethod("POST"),
                                            RemoteItRest.getApiDeviceProxyString(deviceRemoteIdAddr, true, "0.0.0.0"))
                                    } else {
                                        setConnectState(false)
                                        Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                        val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                                        var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetupConnectionTypeFragment()
                                        navController.navigate(action)
                                    }
                                } else {
                                    setConnectState(false)
                                    Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                    val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                                    var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetupConnectionTypeFragment()
                                    navController.navigate(action)
                                }
                            }
                        }).execute(RemoteItRest.getApiCommonHeader(remotruser.dev, remotruser.token),
                            RemoteItRest.getApiMethod("GET"))
                    } else {
                        setConnectState(false)
                        val errorJson = result.result as JSONObject
                        val reason = errorJson.getString("reason")
                        Log.d("0716", "reason=$reason")
                        Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                        val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                        var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetupConnectionTypeFragment()
                        navController.navigate(action)
                    }
                }
            }).execute(RemoteItRest.getApiLoginHeader("QzlEQ0JDQzItNjYyMC00RjVCLUIwOTgtQkFBQkNCMzgxRUFG"),
                RemoteItRest.getApiMethod("POST"),
                RemoteItRest.getApiLoginString("ccmaped@gmail.com", "maped1234"))
        }
    }

    private fun setConnectState(isConnecting: Boolean) {
        pgsConnecting.visibility = if (isConnecting) {
            VISIBLE
        } else {
            GONE
        }
        btnConnect.visibility = if (isConnecting) {
            GONE
        } else {
            VISIBLE
        }
    }
}
