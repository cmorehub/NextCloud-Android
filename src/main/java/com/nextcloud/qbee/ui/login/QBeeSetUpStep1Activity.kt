package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class QBeeSetUpStep1Activity : AppCompatActivity() {

    lateinit var btnNext: Button


    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_qbee_1)

        btnNext = findViewById(R.id.btnNext)
        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")

        btnNext.setOnClickListener {
            var intent = Intent(this@QBeeSetUpStep1Activity, QBeeSelectConnectionActivity::class.java)
            intent.putExtra("mail", mail)
            intent.putExtra("pwd", pwd)
            startActivity(intent)
            this@QBeeSetUpStep1Activity.finish()
        }
    }
}
