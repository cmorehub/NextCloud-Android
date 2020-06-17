package com.nextcloud.qbee.ui.setup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.ui.event.SetupDataEvent
import com.nextcloud.qbee.ui.event.SetupFinishEvent
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QBeeSetupActivity : AppCompatActivity() {

    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_base)
        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        EventBus.getDefault().post(SetupDataEvent(mail!!, pwd!!))
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("0616","activity onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d("0616","activity onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SetupFinishEvent) {
        if (event.success) {
//            setResult(RESULT_OK, Intent().putExtra("AccountManager.KEY_ACCOUNT_NAME", event.account))
            val i = Intent(this, FileDisplayActivity::class.java)
            i.action = FileDisplayActivity.RESTART
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        } else {
            Toast.makeText(this@QBeeSetupActivity, "Login Failed", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
        }
        this@QBeeSetupActivity.finish()
    }
}
