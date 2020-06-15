package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.net.Uri
import android.os.Bundle
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
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.SetupFinishEvent
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray

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

        val navController = NavHostFragment.findNavController(this)
        btnSignUp.setOnClickListener {
            val action = QBeeSignInFragmentDirections.actionQBeeSignInFragmentToQBeeSignUpFragment()
            navController.navigate(action)
        }
        btnSignIn.setOnClickListener {
            QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                override fun onResult(result: QBeeSetupResult) {
                    if (result.success) {
                        Thread {
                            val devices = result.result as JSONArray
                            if (devices.length() > 0) {
                                val url = Uri.parse("http://iottalk.cmoremap.com.tw:6325")
                                val loginName: String = "askey"
                                val password: String = "askeyqbee"

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
                            } else {
                                EventBus.getDefault().post(LoginFinishEvent(true, editTextTextEmailAddress2.text.toString(), editTextTextPassword
                                    .text.toString(), LoginFinishEvent
                                    .LoginForSetup))
                            }
                        }.start()
                    } else {
                        Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                    }
                }
            }).execute(ApiQBeeBind.getApiLoginString(editTextTextEmailAddress2.text.toString(), editTextTextPassword
                .text.toString()))
        }
    }
}
