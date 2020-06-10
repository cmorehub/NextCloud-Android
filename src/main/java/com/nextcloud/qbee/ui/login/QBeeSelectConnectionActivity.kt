package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class QBeeSelectConnectionActivity : AppCompatActivity() {

    lateinit var blockOptionEthernet: LinearLayout



    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_type)

        blockOptionEthernet = findViewById(R.id.blockOptionEthernet)
        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")

        blockOptionEthernet.setOnClickListener {
            var intent = Intent(this@QBeeSelectConnectionActivity, QBeeSetUpStep1Activity::class.java)
            intent.putExtra("mail", mail)
            intent.putExtra("pwd", pwd)
            startActivity(intent)
            this@QBeeSelectConnectionActivity.finish()
        }
    }
}
