package com.nextcloud.qbee.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


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
                    setResult(Activity.RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", event.account))
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
}
