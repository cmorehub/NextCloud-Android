package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.nextcloud.qbee.remoteit.RemoteItController
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirstLoginActivity : AppCompatActivity() {

    private val viewModelFactory = LoginViewModelFactory()
    private val loginViewModel: LoginViewModel by viewModels { viewModelFactory }
    private val remoteItController by lazy {
        RemoteItController(this)
    }
    private var remoteItAuthToken: String? = null
    private val accountType by lazy{
        getString(R.string.account_type)
    }

    @Throws(Exception::class)
    private suspend fun findQBeeDeviceOfName(deviceName: String, forceFirst: Boolean = false): RemoteItController
    .RemoteDevice? = withContext(Dispatchers.IO) {
        remoteItAuthToken = remoteItController.restGetAuthToken()
        val deviceList = remoteItController.restGetDeviceList(remoteItAuthToken!!, ofType = if (forceFirst)
            RemoteItController.TYPE_NEXTCLOUD else null)
        return@withContext if (forceFirst) {
            deviceList[0]
        } else {
            val filteredDeviceList = deviceList.filter {
                it.name == deviceName
            }
            if (filteredDeviceList.isEmpty()) null else filteredDeviceList[0]
        }
    }

    private suspend fun loginQBee(remoteItQBee: RemoteItController.RemoteDevice, usePeerToPeer: Boolean = false) =
        withContext(Dispatchers.IO) {
            Log.d("QBeeDotCom","connecting QBee $remoteItQBee")
            setLoadingEnabled()
            remoteItAuthToken = remoteItAuthToken ?: remoteItController.restGetAuthToken()
            val qBeeUrl =
                if (usePeerToPeer) {
                    remoteItController.peerToPeerLogin()
                    remoteItController.peerToPeerConnect(remoteItQBee.address)
                } else remoteItController.restGetRemoteProxy(remoteItAuthToken!!, remoteItQBee.address)
            Log.d("QBeeDotCom", "qbeeUrl = $qBeeUrl")
            loginQBee(android.net.Uri.parse(qBeeUrl!!.replace("http:", "https:")), "admin", "admin")
        }

    private suspend fun loginQBee(url: Uri, loginName: String, password: String) = withContext(Dispatchers.Main) {
        val accountManager = AccountManager.get(this@FirstLoginActivity)
        val accounts = accountManager.getAccountsByType(accountType) // TODO

        val accountName = AccountUtils.buildAccountName(url, loginName)
        val newAccount = Account(accountName, accountType)

        accountManager.addAccountExplicitly(newAccount, password, null)
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

        val manager = OwnCloudClientManager()
        val account = OwnCloudAccount(newAccount, this@FirstLoginActivity)

        val loginSuccess = withContext(Dispatchers.IO){
            val client = manager.getNextcloudClientFor(account, this@FirstLoginActivity)
            loginName == client.userId
        }

        withContext(Dispatchers.Main) {
            if (loginSuccess) {
                setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", newAccount.name))
            } else {
                Toast.makeText(this@FirstLoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
            }
            this@FirstLoginActivity.finish()
        }
    }

    private suspend fun setLoadingEnabled() = withContext(Dispatchers.Main) {
        this@FirstLoginActivity.loadingBar.visibility = View.VISIBLE
        this@FirstLoginActivity.loginButton.isEnabled = false
        this@FirstLoginActivity.usernameEditText.isEnabled = false
        this@FirstLoginActivity.passwordEditText.isEnabled = false
    }

    private suspend fun setLoadingDisabled() = withContext(Dispatchers.Main) {
        this@FirstLoginActivity.loadingBar.visibility = View.GONE
        this@FirstLoginActivity.loginButton.isEnabled = true
        this@FirstLoginActivity.usernameEditText.isEnabled = true
        this@FirstLoginActivity.passwordEditText.isEnabled = true
    }

    private val usernameEditText by lazy {
        findViewById<EditText>(R.id.username)
    }
    private val passwordEditText by lazy {
        findViewById<EditText>(R.id.password)
    }
    private val loginButton by lazy {
        findViewById<Button>(R.id.login)
    }
    private val loadingBar by lazy {
        findViewById<ProgressBar>(R.id.loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_simple_login)

//        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
//            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@FirstLoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            loginButton.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                usernameEditText.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                passwordEditText.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@FirstLoginActivity, Observer {
            CoroutineScope(Dispatchers.Main).launch {
                if (it?.success != null) {
//                    loginQBee("https://www.askey.it".toUri(),"nextcloud","Aa123456")
//                    loginQBee("http://iottalk.cmoremap.com.tw:6325".toUri(),"iottalk","97497929")
                    val device = findQBeeDeviceOfName(it.success.device?.remote ?: return@launch)
                    if (device != null) {
                        loginQBee(device,usePeerToPeer = false)
                    } else {
                        Toast.makeText(this@FirstLoginActivity, it.error?.message ?: getString(R.string
                            .login_failed)
                            , Toast.LENGTH_SHORT).show()
                        setLoadingDisabled()
                    }
                } else {
                    Toast.makeText(this@FirstLoginActivity, it.error?.message ?: getString(R.string.login_failed), Toast
                        .LENGTH_SHORT)
                        .show()
                    setLoadingDisabled()
                }
            }
        })

        usernameEditText.setText(getString(R.string.default_email))

        usernameEditText.afterTextChanged {
            loginViewModel.loginDataChanged(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        passwordEditText.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginAskeyQBeeDotCom()
                }
                false
            }

            loginButton.setOnClickListener {
                loginAskeyQBeeDotCom()
            }
        }
    }

    private fun loginAskeyQBeeDotCom() {
        CoroutineScope(Dispatchers.Main).launch {
            setLoadingEnabled()
            loginViewModel.login(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }
    }


    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
