package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils

class QBeeSetupStep5Activity : AppCompatActivity() {

    lateinit var btnSetupFinish: Button

    var mail: String? = null
    var pwd: String? = null
    var mac: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_qbee_5)

        btnSetupFinish = findViewById(R.id.btnSetupFinish)


        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")
        mac = intent.getStringExtra("mac")
        btnSetupFinish.setOnClickListener {
            Thread {
                val url = Uri.parse("http://iottalk.cmoremap.com.tw:6325")
                val loginName: String = "askey"
                val password: String = "askeyqbee"

                val accountManager = AccountManager.get(this@QBeeSetupStep5Activity)
                val accountName = AccountUtils.buildAccountName(url, loginName)
                val newAccount = Account(accountName, "nextcloud")

                accountManager.addAccountExplicitly(newAccount, password, null)
                accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

                val manager = OwnCloudClientManager()
                val account = OwnCloudAccount(newAccount, this@QBeeSetupStep5Activity)

                val client = manager.getClientFor(account, this@QBeeSetupStep5Activity)
                val loginSuccess = loginName == client.userId
                runOnUiThread {
                    if (loginSuccess) {
                        setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", newAccount.name))
                    } else {
                        Toast.makeText(this@QBeeSetupStep5Activity, "Login Failed", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_CANCELED)
                    }
                    this@QBeeSetupStep5Activity.finish()
                }
            }.start()
        }
    }
}
