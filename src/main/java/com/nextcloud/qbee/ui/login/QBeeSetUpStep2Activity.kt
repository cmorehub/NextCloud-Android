package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class QBeeSetUpStep2Activity : AppCompatActivity() {

    lateinit var btnNext: Button

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
            this@QBeeSetUpStep2Activity.finish()
        }
    }
}
