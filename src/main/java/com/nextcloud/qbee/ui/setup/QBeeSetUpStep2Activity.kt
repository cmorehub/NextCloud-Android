package com.nextcloud.qbee.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QBeeSetUpStep2Activity : AppCompatActivity() {

    lateinit var btnNext: ImageButton

    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_qbee_2)

        btnNext = findViewById(R.id.btnNext)

        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")
        btnNext.setOnClickListener {
            var intent = Intent(this@QBeeSetUpStep2Activity, QBeeSetUpStep3Activity::class.java)
            intent.putExtra("mail", mail)
            intent.putExtra("pwd", pwd)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Boolean) {
        if (event) {
            this@QBeeSetUpStep2Activity.finish()
        }
    }
}
