@file:JvmName("QBeeLoginSharedPref")

package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.event.QBeeLoginEvent
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

const val QBEE_LOGIN_DATA = "com.nextcloud.qbee.ui.login.QBEE_LOGIN_DATA"
const val QBEE_LOGIN_DATA_ACCT = "com.nextcloud.qbee.ui.login.QBEE_LOGIN_DATA_ACCT"
const val QBEE_LOGIN_DATA_PWD = "com.nextcloud.qbee.ui.login.QBEE_LOGIN_DATA_PWD"
const val QBEE_LOGIN_DATA_URL = "com.nextcloud.qbee.ui.login.QBEE_LOGIN_DATA_URL"


class QBeeLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_login_base)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: LoginFinishEvent) {
        if (event.success) {
            when (event.nextTo) {
                LoginFinishEvent.LoginForCloud -> {
//                    setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", event.account))
                    val i = Intent(this, FileDisplayActivity::class.java)
                    i.action = FileDisplayActivity.RESTART
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    this@QBeeLoginActivity.finish()
                }
                LoginFinishEvent.LoginForSetup -> {
                    var intent = Intent(this, QBeeSetupActivity::class.java)
                    intent.putExtra("mail", event.account)
                    intent.putExtra("pwd", event.pwd)
                    startActivity(intent)
                    this@QBeeLoginActivity.finish()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: QBeeLoginEvent) {
        getSharedPreferences(QBEE_LOGIN_DATA, 0).edit().putString(QBEE_LOGIN_DATA_ACCT, event.mail)
            .putString(QBEE_LOGIN_DATA_PWD, event.pwd).putString(QBEE_LOGIN_DATA_URL, event.url).apply()
    }
}
