package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_welcome)

        Handler().postDelayed(Runnable {
            var intent = Intent(this@WelcomeActivity, QBeeLoginActivity::class.java)
            startActivity(intent)
            this@WelcomeActivity.finish()
        }, 3000)
    }
}
