package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountsException
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
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
import com.nextcloud.qbee.ui.event.SetupFinishEvent
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.utils.Log_OC
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

class QBeeSignInFragment : Fragment() {

    private val remoteItController by lazy {
        RemoteItController(this.requireContext())
    }

    private var remoteItAuthToken: String? = null

    lateinit var editTextTextEmailAddress2: EditText
    lateinit var editTextTextPassword: EditText
    lateinit var btnSignIn: Button
    lateinit var btnSignUp: TextView
    lateinit var progressSignIn: ProgressBar

    val debug = false
    val localaddr = "http://192.168.0.107"

    val customUrl = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_signin, container, false)

        editTextTextEmailAddress2 = root.findViewById(R.id.editTextTextEmailAddress2)
        editTextTextPassword = root.findViewById(R.id.editTextTextPassword)
        btnSignIn = root.findViewById(R.id.btnSignIn)
        btnSignUp = root.findViewById(R.id.btnSignUp)
        progressSignIn = root.findViewById(R.id.progressSignIn)
        progressSignIn.visibility = GONE

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
            if (customUrl) {
                val action = QBeeSignInFragmentDirections.actionQBeeSignInFragmentToQBeeSignForUrlFragment(acctmail, pass)
                navController.navigate(action)
            } else {
                btnSignIn.isEnabled = false
                progressSignIn.visibility = VISIBLE
                if (debug) {
                    CoroutineScope(Dispatchers.Main).launch {
                        doLocalLogin(acctmail!!, pass!!)
                    }
                } else {
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
                                            loginQBee(remoteDevice, acctmail, pass)
//                                                loginQBee(remoteDevice, acctmail, pass, true)
                                            } else {
                                                Toast.makeText(context, getString(R.string.qbee_setup_device_not_found_error), Toast.LENGTH_LONG).show()
                                                btnSignIn.isEnabled = true
                                                progressSignIn.visibility = GONE
                                            }
                                        } catch (e: ConnectException) {
                                            Log.d("QBeeDotComSignIn", "ConnectException=${e.message}")
                                            val remoteDevice = findQBeeDeviceOfName(deviceremote ?: return@launch)
                                            if (remoteDevice != null) {
                                                loginQBee(remoteDevice, acctmail, pass)
                                            } else {
                                                Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                                btnSignIn.isEnabled = true
                                                progressSignIn.visibility = GONE
                                            }
                                        } catch (e: Exception) {
                                            Log.d("QBeeDotComSignIn", "Exception=${e.message}")
                                            Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                                            btnSignIn.isEnabled = true
                                            progressSignIn.visibility = GONE
                                        }
                                    }
                                } else {
                                    Thread {
                                        EventBus.getDefault().post(LoginFinishEvent(true, editTextTextEmailAddress2.text.toString(), editTextTextPassword
                                            .text.toString(), LoginFinishEvent
                                            .LoginForSetup))
                                        EventBus.getDefault().post(QBeeLoginEvent(acctmail, pass))
                                    }.start()
                                }
                            } else {
                                btnSignIn.isEnabled = true
                                progressSignIn.visibility = GONE
                                Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                            }
                        }
                    }).execute(ApiQBeeBind.getApiLoginString(acctmail, pass))
                }
            }
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
    private suspend fun loginQBee(remoteItQBee: RemoteItController.RemoteDevice, qbeeAcct: String, qbeePwd: String, usePeerToPeer: Boolean = false) =
        withContext(Dispatchers.IO) {
            Log.d("QBeeDotComSignIn", "loginQBee")
            addQBeeCert()
            remoteItAuthToken = remoteItAuthToken ?: remoteItController.restGetAuthToken()

            val qbeeUrl = if (usePeerToPeer) {
                remoteItController.peerToPeerLogin()
                remoteItController.peerToPeerConnect(remoteItQBee.address)
            } else remoteItController.restGetRemoteProxy(remoteItAuthToken!!, remoteItQBee.address)
            //"https://www.askey.it"//"https://iottalk.cmoremap.com.tw:6326"
            Log.d("QBeeDotCom", "qbeeUrl = $qbeeUrl")
            val loginName = "admin"//"nextcloud"//"iottalk"//"admin"
            val password = "admin"//"Aa123456"//"97497929"//"admin"

            val accountManager = AccountManager.get(context)
            val accountName = AccountUtils.buildAccountName(Uri.parse(qbeeUrl), loginName)
            val accounts = accountManager.getAccountsByType("nextcloud")
            if (accounts.isEmpty()) {
                Log.d("QBeeDotComSignIn", "accounts.isEmpty()")
                val account = Account(accountName, "nextcloud")
                var addResult = accountManager.addAccountExplicitly(account, password, null)
                if (!addResult) {
                    accountManager.removeAccountExplicitly(account)
                    addResult = accountManager.addAccountExplicitly(account, password, null)
                    if (!addResult) {
                        throw AccountsException("Account create failed")
                    }
                }
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
                addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
            } else {
                Log.d("QBeeDotComSignIn", "accounts.isNotEmpty()")
                var logedAccount: Account? = null
                for (account in accounts) {
                    val qbeeAccount = accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_ID)
                    if (qbeeAcct == qbeeAccount) {
                        logedAccount = account
                        break
                    }
                }
                if (logedAccount != null) {
                    Log.d("QBeeDotComSignIn", "logedAccount.isNotEmpty()")
                    accountManager.removeAccountExplicitly(logedAccount)
                    val account = Account(accountName, "nextcloud")
                    var addResult = accountManager.addAccountExplicitly(account, password, null)
                    if (!addResult) {
                        throw AccountsException("Account create failed")
                    }
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
                    addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
//                    accountManager.renameAccount(logedAccount, accountName, {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            val reacct = it.result
//                            Log.d("QBeeDotCom", "accountname B= ${reacct.name}")
//                            accountManager.addAccountExplicitly(reacct, password, null)
//                            accountManager.setUserData(reacct, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
//                            accountManager.setUserData(reacct, AccountUtils.Constants.KEY_USER_ID, loginName)
//                            accountManager.setUserData(reacct, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
//                            accountManager.setUserData(reacct, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
//                            addOwnCloudAccount(reacct, loginName, qbeeAcct, qbeePwd)
//                        }
//                    }, null)
                } else {
                    Log.d("QBeeDotComSignIn", "logedAccount.isEmpty()")
                    val account = Account(accountName, "nextcloud")
                    var addResult = accountManager.addAccountExplicitly(account, password, null)
                    if (!addResult) {
                        accountManager.removeAccountExplicitly(account)
                        addResult = accountManager.addAccountExplicitly(account, password, null)
                        if (!addResult) {
                            throw AccountsException("Account create failed")
                        }
                    }
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                    accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
                    accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
                    addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
                }
            }
        }

    private suspend fun addOwnCloudAccount(savedAccount: Account, loginName: String, qbeeAcct: String, qbeePwd: String) =
        withContext(Dispatchers.IO) {
            Log_OC.d("QBeeDotComSignIn", "addOwnCloudAccount")
            val manager = OwnCloudClientManager()
            Log.d("QBeeDotComSignIn", "1")
            val account = OwnCloudAccount(savedAccount, context)
            Log_OC.d("QBeeDotComSignIn", "2")

            val client = manager.getNextcloudClientFor(account, context)
            Log_OC.d("QBeeDotComSignIn", "client.userId=${client.userId}")
            val loginSuccess = loginName == client.userId
            if (loginSuccess) {
                Log_OC.d("QBeeDotComSignIn", "3")
                EventBus.getDefault().post(LoginFinishEvent(loginSuccess, savedAccount.name, "", LoginFinishEvent.LoginForCloud))
                EventBus.getDefault().post(QBeeLoginEvent(qbeeAcct, qbeePwd))
            } else {
                Log_OC.d("QBeeDotComSignIn", "4")
                Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                btnSignIn.isEnabled = true
                progressSignIn.visibility = GONE
            }
        }

    private suspend fun addQBeeCert() = withContext(Dispatchers.IO) {
        resources.openRawResource(R.raw.qbee).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, context)
        }
        resources.openRawResource(R.raw.askeyit).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, context)
        }
        resources.openRawResource(R.raw.qbee_9214).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert, context)
        }
    }

    @Throws(Exception::class)
    private suspend fun doLocalLogin(qbeeAcct: String, qbeePwd: String) = withContext(Dispatchers.IO) {
        addQBeeCert()
        remoteItAuthToken = remoteItAuthToken ?: remoteItController.restGetAuthToken()
        val qbeeUrl = localaddr
        Log.d("QBeeDotCom", "qbeeUrl = $qbeeUrl")
        val loginName = "admin"//"nextcloud"//"iottalk"//"admin"
        val password = "admin"//"Aa123456"//"97497929"//"admin"

        val accountManager = AccountManager.get(context)
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
            accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
            accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
            val url = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)
            Log.d("QBeeDotCom", "check add url=$url")
            addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
        } else {
            Log.d("QBeeDotComSignIn", "accounts.isNotEmpty()")
            var logedAccount: Account? = null
            for (account in accounts) {
                val qbeeAccount = accountManager.getUserData(account, QBeeAccountUtils.ASKEY_USER_ID)
                if (qbeeAcct == qbeeAccount) {
                    logedAccount = account
                    break
                }
            }
            if (logedAccount != null) {
                Log.d("QBeeDotComSignIn", "logedAccount.isNotEmpty()")
                accountManager.removeAccountExplicitly(logedAccount)
                val account = Account(accountName, "nextcloud")
                var addResult = accountManager.addAccountExplicitly(account, password, null)
                if (!addResult) {
                    throw AccountsException("Account create failed")
                }
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
                addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
//                accountManager.renameAccount(logedAccount, accountName, {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        val reacct = it.result
//                        Log.d("QBeeDotCom", "accountname B= ${reacct.name}")
//                        accountManager.addAccountExplicitly(reacct, password, null)
//                        accountManager.setUserData(reacct, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
//                        accountManager.setUserData(reacct, AccountUtils.Constants.KEY_USER_ID, loginName)
//                        accountManager.setUserData(reacct, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
//                        accountManager.setUserData(reacct, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
//                        addOwnCloudAccount(reacct, loginName, qbeeAcct, qbeePwd)
//                    }
//                }, null)
            } else {
                Log.d("QBeeDotComSignIn", "logedAccount.isEmpty()")
                val account = Account(accountName, "nextcloud")
                var addResult = accountManager.addAccountExplicitly(account, password, null)
                if (!addResult) {
                    accountManager.removeAccountExplicitly(account)
                    addResult = accountManager.addAccountExplicitly(account, password, null)
                    if (!addResult) {
                        throw AccountsException("Account create failed")
                    }
                }
                accountManager.setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, qbeeUrl)
                accountManager.setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_ID, qbeeAcct)
                accountManager.setUserData(account, QBeeAccountUtils.ASKEY_USER_TOKEN, qbeePwd)
                addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd)
            }
        }
    }
}
