package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils

class QBeeSignInActivity : AppCompatActivity() {


    lateinit var editTextTextEmailAddress2: EditText
    lateinit var editTextTextPassword: EditText
    lateinit var btnSignIn: Button
    lateinit var btnSignUp: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_signin)

        editTextTextEmailAddress2 = findViewById(R.id.editTextTextEmailAddress2)
        editTextTextPassword = findViewById(R.id.editTextTextPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)

        btnSignIn.setOnClickListener {
            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    Thread {
                        val url = Uri.parse("http://iottalk.cmoremap.com.tw:6325")
                        val loginName: String = "askey"
                        val password: String = "askeyqbee"

                        val accountManager = AccountManager.get(this@QBeeSignInActivity)
                        val accountName = AccountUtils.buildAccountName(url, loginName)
                        val newAccount = Account(accountName, "nextcloud")

                        accountManager.addAccountExplicitly(newAccount, password, null)
                        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

                        val manager = OwnCloudClientManager()
                        val account = OwnCloudAccount(newAccount, this@QBeeSignInActivity)

                        val client = manager.getClientFor(account, this@QBeeSignInActivity)
                        val loginSuccess = loginName == client.userId
                        runOnUiThread {
                            if (loginSuccess) {
                                setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", newAccount.name))
                            } else {
                                Toast.makeText(this@QBeeSignInActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                                setResult(Activity.RESULT_CANCELED)
                            }
                            this@QBeeSignInActivity.finish()
                        }
                    }.start()
                }
            }).execute(ApiQBeeBind.getApiLoginString(editTextTextEmailAddress2.text.toString(), editTextTextPassword
                .text.toString()))
        }


        btnSignUp.setOnClickListener {
            var intent = Intent(this@QBeeSignInActivity, QBeeSignUpActivity::class.java)
            startActivity(intent)
            this@QBeeSignInActivity.finish()
        }
    }
}
