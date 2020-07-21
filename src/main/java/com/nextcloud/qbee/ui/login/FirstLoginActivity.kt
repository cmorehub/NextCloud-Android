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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.nextcloud.qbee.qbeecom.QBeeDotCom
import com.nextcloud.qbee.remoteit.RemoteItController
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class FirstLoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private val remoteItController by lazy{
        RemoteItController(this)
    }
    private var remoteItAuthToken : String? = null

    @Throws(Exception::class)
    private suspend fun findQBeeDeviceOfName(deviceName:String):RemoteItController
    .RemoteDevice?{
        remoteItAuthToken = remoteItController.restGetAuthToken()
        val filteredDeviceList = remoteItController.restGetDeviceList(remoteItAuthToken!!).filter {
            it.name == deviceName
        }
        return if(filteredDeviceList.isEmpty()) null else filteredDeviceList[0]
    }

    private suspend fun loginQBee(remoteItQBee : RemoteItController.RemoteDevice) = withContext(Dispatchers.Main){
        loading.visibility = View.VISIBLE
        withContext(Dispatchers.IO){
            addQBeeCert()
            remoteItAuthToken = remoteItAuthToken?:remoteItController.restGetAuthToken()
            remoteItController.peerToPeerLogin()
            val qbeeUrl = remoteItController.restGetRemoteProxy(remoteItAuthToken!!,remoteItQBee.address)
            Log.d("QBeeDotCom","qbeeUrl = $qbeeUrl")
            val url = Uri.parse(qbeeUrl!!.replace("http:","https:"))
            val loginName = "admin"
            val password = "admin"

            val accountManager = AccountManager.get(this@FirstLoginActivity)
            val accountName = AccountUtils.buildAccountName(url, loginName)
            val newAccount = Account(accountName, "nextcloud")

            accountManager.addAccountExplicitly(newAccount, password, null)
            accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
            accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

            val manager = OwnCloudClientManager()
            val account = OwnCloudAccount(newAccount, this@FirstLoginActivity)

            val client = manager.getNextcloudClientFor(account, this@FirstLoginActivity)
            val loginSuccess = loginName==client.userId
            withContext(Dispatchers.Main){
                this@FirstLoginActivity.loading.visibility = View.GONE
                if (loginSuccess){
                    setResult(Activity.RESULT_OK,Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME",newAccount.name))
                }else{
                    Toast.makeText(this@FirstLoginActivity,"Login Failed",Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                }
                this@FirstLoginActivity.finish()
            }
        }
    }

    val username by lazy{
        findViewById<EditText>(R.id.username)
    }
    val password by lazy{
        findViewById<EditText>(R.id.password)
    }
    val login by lazy{
        findViewById<Button>(R.id.login)
    }
    val loading by lazy{
        findViewById<ProgressBar>(R.id.loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_simple_login)

        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@FirstLoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@FirstLoginActivity, Observer {
            CoroutineScope(Dispatchers.Main).launch {
                if(it?.success != null) {
                    val device = findQBeeDeviceOfName(it.success.device?.remote?:return@launch)
                    if(device!=null){
                        loginQBee(device)
                    } else Toast.makeText(this@FirstLoginActivity,it.error?.message?:getString(R.string.login_failed)
                        ,Toast
                        .LENGTH_SHORT)
                        .show()
                } else{
                    Toast.makeText(this@FirstLoginActivity,it.error?.message?:getString(R.string.login_failed),Toast
                        .LENGTH_SHORT)
                        .show()
                }
            }
        })

        username.setText("ccmaped@gmail.com")

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        CoroutineScope(Dispatchers.Main).launch {
                            loading.visibility = View.VISIBLE
                            loginViewModel.login(
                                username.text.toString(),
                                password.text.toString()
                            )
                            loading.visibility = View.INVISIBLE
                        }
                }
                false
            }

            login.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    loading.visibility = View.VISIBLE
                    loginViewModel.login(username.text.toString(), password.text.toString())
                    loading.visibility = View.INVISIBLE
                }
            }
        }
    }

    private suspend fun addQBeeCert() = withContext(Dispatchers.IO){
        resources.openRawResource(R.raw.qbee).use {
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            NetworkUtils.addCertToKnownServersStore(cert,this@FirstLoginActivity)
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
