package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountsException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.nextcloud.qbee.data.model.QBeeAccountUtils
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.setup.QBeeSetup4FragmentArgs
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.android.synthetic.main.activity_qbee_signforurl.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class QBeeSignForUrlFragment : Fragment() {

    lateinit var btnConnectToUrl: Button
    lateinit var editUrl: EditText
    lateinit var progressConnectToUrl: ProgressBar

    lateinit var mail: String
    lateinit var pwd: String

    private val args: QBeeSignForUrlFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_signforurl, container, false)
        btnConnectToUrl = root.findViewById(R.id.btnConnectToUrl)
        editUrl = root.findViewById(R.id.editUrl)
        progressConnectToUrl = root.findViewById(R.id.progressConnectToUrl)

        mail = args.mail
        pwd = args.pwd

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireContext().getSharedPreferences(QBEE_LOGIN_DATA, 0).edit().clear().apply()
        btnConnectToUrl.setOnClickListener {
            val url = "https://${editUrl.text}"
            if (url.isNotBlank() && url.isNotEmpty()) {
                btnConnectToUrl.isEnabled = false
                progressConnectToUrl.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    doLocalLogin(mail!!, pwd!!, url)
                }
            }
        }
    }

    private suspend fun addOwnCloudAccount(savedAccount: Account, loginName: String, qbeeAcct: String, qbeePwd: String,
                                           qbeeUrl: String = "") =
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
                EventBus.getDefault().post(QBeeLoginEvent(qbeeAcct, qbeePwd, qbeeUrl))
            } else {
                Log_OC.d("QBeeDotComSignIn", "4")
                Toast.makeText(context, getString(R.string.qbee_setup_connected_error), Toast.LENGTH_LONG).show()
                btnConnectToUrl.isEnabled = true
                progressConnectToUrl.visibility = View.GONE
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
    private suspend fun doLocalLogin(qbeeAcct: String, qbeePwd: String, qbeeUrl: String) = withContext(Dispatchers.IO) {
        addQBeeCert()
        Log.d("QBeeDotCom", "qbeeUrl = $qbeeUrl")
        val loginName = qbeeAcct// "admin"//"nextcloud"//"iottalk"//"admin"
        val password = qbeePwd//"admin"//"Aa123456"//"97497929"//"admin"

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
            addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd, qbeeUrl)
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
                addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd, qbeeUrl)
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
                addOwnCloudAccount(account, loginName, qbeeAcct, qbeePwd, qbeeUrl)
            }
        }
    }
}
