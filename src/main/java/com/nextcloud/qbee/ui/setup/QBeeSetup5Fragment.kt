package com.nextcloud.qbee.ui.setup

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import org.greenrobot.eventbus.EventBus

class QBeeSetup5Fragment() : Fragment() {

    lateinit var btnSetupFinish: Button

    var mail: String? = null
    var pwd: String? = null
    var mac: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_5, container, false)
        btnSetupFinish = root.findViewById(R.id.btnSetupFinish)


//        mail = intent.getStringExtra("mail")
//        pwd = intent.getStringExtra("pwd")
//        mac = intent.getStringExtra("mac")
        btnSetupFinish.setOnClickListener {
            Thread {
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
//                runOnUiThread {
//                    if (loginSuccess) {
//                        setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", newAccount.name))
//                    } else {
//                        Toast.makeText(this@QBeeSetupStep5Activity, "Login Failed", Toast.LENGTH_SHORT).show()
//                        setResult(Activity.RESULT_CANCELED)
//                    }
//                    EventBus.getDefault().post(true)
//                    this@QBeeSetupStep5Activity.finish()
//                }
            }.start()
        }
        return root
    }

}
