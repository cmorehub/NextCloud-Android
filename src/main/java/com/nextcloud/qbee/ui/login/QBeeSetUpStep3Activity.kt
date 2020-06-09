package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class QBeeSetUpStep3Activity : AppCompatActivity() {

    lateinit var btnScanQRCode: Button

    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_qbee_3)

        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")

        btnScanQRCode = findViewById(R.id.btnScanQRCode)

        btnScanQRCode.setOnClickListener {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var intent = Intent(this@QBeeSetUpStep3Activity, QBeeSetupStep4Activity::class.java)
        intent.putExtra("mail", mail)
        intent.putExtra("pwd", pwd)
        intent.putExtra("mac", "12345678")
        startActivity(intent)
        this@QBeeSetUpStep3Activity.finish()
    }
}
