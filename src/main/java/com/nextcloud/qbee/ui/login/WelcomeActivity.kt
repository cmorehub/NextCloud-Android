package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.AccountsException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.data.model.QBeeAccountUtils
import com.nextcloud.qbee.network.QBeeSetupController
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.RemoteItRestTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.network.model.RemoteItRest
import com.nextcloud.qbee.network.model.RemoteItUser
import com.nextcloud.qbee.remoteit.RemoteItController
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.ui.activity.FileDisplayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class WelcomeActivity : AppCompatActivity() {

    private val remoteItController by lazy {
        RemoteItController(this)
    }

    private val qbeeSetupController by lazy {
        QBeeSetupController(this)
    }

    private var remoteItAuthToken: String? = null

    private lateinit var progressWelcome: ProgressBar

    val localaddr = "http://192.168.0.107"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_welcome)
        progressWelcome = findViewById(R.id.progressWelcome)
        CoroutineScope(Dispatchers.Main).launch {
            val logged = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "") != ""
            val debug = false
            if (logged && debug) {
                val acctmail = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "")
                val pass = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_PWD, "")
                val url = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_URL, localaddr)
                doLocalLogin(acctmail!!, pass!!, url!!)
            } else if (logged) {
//                doLogin()
                val acctmail = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "")
                val pass = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_PWD, "")
                QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                    override fun onResult(result: QBeeSetupResult) {
                        if (result.success) {
                            val devices = result.result as JSONArray
                            if (devices.length() > 0) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val device = devices[0] as JSONObject
                                    val devicemac = device.getString("mac").toLowerCase()
                                    val deviceremote = device.getString("remote")
                                    try {
                                        val remoteDevice = findQBeeDeviceOfName(deviceremote ?: return@launch)
                                        if (remoteDevice != null) {
                                            loginQBee(remoteDevice, acctmail!!, pass!!)
//                                            loginQBee(remoteDevice, acctmail!!, pass!!, true)
                                        } else {
                                            Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_device_not_found_error), Toast.LENGTH_LONG).show()
                                            var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                            startActivity(intent)
                                            this@WelcomeActivity.finish()
                                        }
                                    } catch (e: ConnectException) {
                                        Log.d("QBeeDotCom", "ConnectException=${e.message}")
                                        val remoteDevice = findQBeeDeviceOfName(deviceremote ?: return@launch)
                                        if (remoteDevice != null) {
                                            loginQBee(remoteDevice, acctmail!!, pass!!)
                                        } else {
                                            Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_device_not_found_error), Toast.LENGTH_LONG).show()
                                            var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                            startActivity(intent)
                                            this@WelcomeActivity.finish()
                                        }
                                    } catch (e: Exception) {
                                        Log.d("QBeeDotCom", "Exception=${e.message}")
                                        Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                        var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                                        startActivity(intent)
                                        this@WelcomeActivity.finish()
                                    }
                                }
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
            } else {
                Handler().postDelayed(Runnable {
                    var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                    startActivity(intent)
                    this@WelcomeActivity.finish()
                }, 3000)
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun checkAskeyLogin() = withContext(Dispatchers.IO) {
        val accountManager = AccountManager.get(this@WelcomeActivity)
        val accounts = accountManager.getAccountsByType("nextcloud")
        return@withContext if (accounts.isNotEmpty()) {
            val account = accounts[0]
            val askeyAcct = JSONObject()
            askeyAcct.put("acct", accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_ID))
            askeyAcct.put("token", accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN))
            askeyAcct
        } else {
            null
        }
    }

    @Throws(Exception::class)
    private suspend fun findQBeeDeviceOfName(deviceName: String, forceFirst: Boolean = false): RemoteItController
    .RemoteDevice? {
        remoteItAuthToken = remoteItController.restGetAuthToken()
        val deviceList = remoteItController.restGetDeviceList(remoteItAuthToken!!, ofType = if (forceFirst)
            RemoteItController.TYPE_NEXTCLOUD else null)
        return if (forceFirst) {
            deviceList[0]
        } else {
            val filteredDeviceList = deviceList.filter {
                it.name == deviceName
            }
            if (filteredDeviceList.isEmpty()) null else filteredDeviceList[0]
        }
    }

    @Throws(Exception::class)
    private suspend fun loginQBee(remoteItQBee: RemoteItController.RemoteDevice, bindAccount: String, bindPwd: String,
                                  usePeerToPeer: Boolean = false) =
        withContext(Dispatchers.IO) {
            Log.d("QBeeDotCom", "loginQBee")
            addQBeeCert()
            remoteItAuthToken = remoteItAuthToken ?: remoteItController.restGetAuthToken()
            val qbeeUrl = if (usePeerToPeer) {
                remoteItController.peerToPeerLogin()
                remoteItController.peerToPeerConnect(remoteItQBee.address)
            } else remoteItController.restGetRemoteProxy(remoteItAuthToken!!, remoteItQBee.address)
            //"https://iottalk.cmoremap.com.tw:6326"//"https://www.askey.it"//"https://iottalk.cmoremap
            // .com.tw:6326"
            Log.d("QBeeDotCom", "qbeeUrl = $qbeeUrl")
            val loginName = "admin"//"nextcloud"//"iottalk"//"admin"
            val password = "admin"//"Aa123456"//"97497929"//"admin"

            val accountManager = AccountManager.get(this@WelcomeActivity)
            val accountName = AccountUtils.buildAccountName(Uri.parse(qbeeUrl), loginName)
            val accounts = accountManager.getAccountsByType("nextcloud")
            if (accounts.isEmpty()) {
                Log.d("QBeeDotCom", "accounts.isEmpty()")
                val account = Account(accountName, "nextcloud")
//                Log.d("QBeeDotCom", "check new account =${account == null}")
                var addResult = accountManager.addAccountExplicitly(account, password, null)
//                Log.d("QBeeDotCom", "check add =$addResult")
                if (!addResult) {
                    accountManager.removeAccountExplicitly(account)
                    addResult = accountManager.addAccountExplicitly(account, password, null)
//                    Log.d("QBeeDotCom", "check add =$addResult")
                    if (!addResult) {
                        throw AccountsException("Account create failed")
                    }
                }
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
                val url = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)
                Log.d("QBeeDotCom", "check add url=$url")
                addOwnCloudAccount(account, loginName)
            } else {
                Log.d("QBeeDotCom", "accounts.isNotEmpty()")
                var logedAccount: Account? = null
                for (account in accounts) {
                    val qbeeAccount = accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_ID)
                    if (bindAccount == qbeeAccount) {
                        logedAccount = account
                        break
                    }
                }
                if (logedAccount != null) {
                    Log.d("QBeeDotCom", "logedAccount != null")
                    accountManager.removeAccountExplicitly(logedAccount)
                    val account = Account(accountName, "nextcloud")
                    var addResult = accountManager.addAccountExplicitly(account, password, null)
                    if (!addResult) {
                        throw AccountsException("Account create failed")
                    }
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
                    addOwnCloudAccount(account, loginName)
                } else {
                    Log.d("QBeeDotCom", "logedAccount == null")
                    val account = Account(accountName, "nextcloud")
                    accountManager.addAccountExplicitly(account, password, null)
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
                    addOwnCloudAccount(account, loginName)
                }
            }
        }

    private suspend fun addOwnCloudAccount(savedAccount: Account, loginName: String) =
        withContext(Dispatchers.IO) {
            val accountManager = AccountManager.get(this@WelcomeActivity)
            val url = accountManager.getUserData(savedAccount, AccountUtils.Constants.KEY_OC_BASE_URL)
            Log.d("QBeeDotCom", "addOwnCloudAccount=$url")
            val manager = OwnCloudClientManager()
            val account = OwnCloudAccount(savedAccount, this@WelcomeActivity)
            val client = manager.getNextcloudClientFor(account, this@WelcomeActivity)
            val loginSuccess = loginName == client.userId
            withContext(Dispatchers.Main) {
                if (loginSuccess) {
                    val i = Intent(this@WelcomeActivity, FileDisplayActivity::class.java)
                    i.action = FileDisplayActivity.RESTART
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                } else {
                    Toast.makeText(this@WelcomeActivity, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                    var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                    startActivity(intent)
                }
                this@WelcomeActivity.finish()
            }
        }

    @Throws(Exception::class)
    private suspend fun doLocalLogin(bindAccount: String, bindPwd: String, qbeeUrl: String) = withContext(Dispatchers.IO) {
        addQBeeCert()
        Log.d("QBeeDotCom", "qbeeUrl = $qbeeUrl")
        val loginName = bindAccount//"admin"//"nextcloud"//"iottalk"//"admin"
        val password = bindPwd//"admin"//"Aa123456"//"97497929"//"admin"

        val accountManager = AccountManager.get(this@WelcomeActivity)
        val accountName = AccountUtils.buildAccountName(Uri.parse(qbeeUrl), loginName)
        val accounts = accountManager.getAccountsByType("nextcloud")
        if (accounts.isEmpty()) {
            Log.d("QBeeDotCom", "accounts.isEmpty()")
            val account = Account(accountName, "nextcloud")
//                Log.d("QBeeDotCom", "check new account =${account == null}")
            var addResult = accountManager.addAccountExplicitly(account, password, null)
//                Log.d("QBeeDotCom", "check add =$addResult")
            if (!addResult) {
                accountManager.removeAccountExplicitly(account)
                addResult = accountManager.addAccountExplicitly(account, password, null)
//                    Log.d("QBeeDotCom", "check add =$addResult")
                if (!addResult) {
                    throw AccountsException("Account create failed")
                }
            }
            accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
            accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
            accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
            accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
            val url = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)
            Log.d("QBeeDotCom", "check add url=$url")
            addOwnCloudAccount(account, loginName)
        } else {
            Log.d("QBeeDotCom", "accounts.isNotEmpty()")
            var logedAccount: Account? = null
            for (account in accounts) {
                val qbeeAccount = accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_ID)
                if (bindAccount == qbeeAccount) {
                    logedAccount = account
                    break
                }
            }
            if (logedAccount != null) {
                Log.d("QBeeDotCom", "logedAccount != null")
                accountManager.removeAccountExplicitly(logedAccount)
                val account = Account(accountName, "nextcloud")
                var addResult = accountManager.addAccountExplicitly(account, password, null)
                if (!addResult) {
                    throw AccountsException("Account create failed")
                }
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
                addOwnCloudAccount(account, loginName)
                Log.d("QBeeDotCom", "check add =$addResult")
            } else {
                Log.d("QBeeDotCom", "logedAccount == null")
                val account = Account(accountName, "nextcloud")
                accountManager.addAccountExplicitly(account, password, null)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, bindAccount)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, bindPwd)
                addOwnCloudAccount(account, loginName)
            }
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

    private suspend fun addQBeeCert() = withContext(Dispatchers.IO) {
        resources.openRawResource(R.raw.qbee).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, this@WelcomeActivity)
        }
        Log.d("QBeeDotCom", "addQBeeCert")
        resources.openRawResource(R.raw.askeyit).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, this@WelcomeActivity)
        }
        resources.openRawResource(R.raw.qbee_9214).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, this@WelcomeActivity)
        }
    }

}
