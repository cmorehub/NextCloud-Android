package com.nextcloud.qbee.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_welcome)
        val logged = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "") != ""
        if (logged) {
            doLogin()
        } else {
            Handler().postDelayed(Runnable {
                var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                startActivity(intent)
                this@WelcomeActivity.finish()
            }, 3000)
        }
    }

    private fun doLogin() {
        val acctmail = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_ACCT, "")
        val pass = getSharedPreferences(QBEE_LOGIN_DATA, 0).getString(QBEE_LOGIN_DATA_PWD, "")
        QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
            override fun onResult(result: QBeeSetupResult) {
                if (result.success) {
                    Thread {
                        val devices = result.result as JSONArray
                        if (devices.length() > 0) {
                            val url = Uri.parse("http://iottalk.cmoremap.com.tw:6325")
                            val loginName: String = "askey"
                            val password: String = "askeyqbee"

                            val accountManager = AccountManager.get(this@WelcomeActivity)
                            val accountName = AccountUtils.buildAccountName(url, loginName)
                            val newAccount = Account(accountName, "nextcloud")

                            accountManager.addAccountExplicitly(newAccount, password, null)
                            accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
                            accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)

                            val manager = OwnCloudClientManager()
                            val account = OwnCloudAccount(newAccount, this@WelcomeActivity)

                            val client = manager.getClientFor(account, this@WelcomeActivity)
                            val loginSuccess = loginName == client.userId
                            val i = Intent(this@WelcomeActivity, FileDisplayActivity::class.java)
                            i.action = FileDisplayActivity.RESTART
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(i)
                            this@WelcomeActivity.finish()
                        } else {
                            var intent = Intent(this@WelcomeActivity, QBeeSetupActivity::class.java)
                            intent.putExtra("mail", acctmail)
                            intent.putExtra("pwd", pass)
                            startActivity(intent)
                            this@WelcomeActivity.finish()
                        }
                    }.start()
                } else {
                    var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
                    startActivity(intent)
                    this@WelcomeActivity.finish()
                }
            }
        }).execute(ApiQBeeBind.getApiLoginString(acctmail!!, pass!!))
    }
}
