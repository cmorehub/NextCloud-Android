package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.owncloud.android.R
import org.w3c.dom.Text

class QBeeSetupStep4Activity : AppCompatActivity() {

    lateinit var btnConnect: Button
    lateinit var textView21: TextView

    lateinit var mail: String
    lateinit var pwd: String
    lateinit var mac: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_setup_qbee_4)

        btnConnect = findViewById(R.id.btnConnect)
        textView21 = findViewById(R.id.textView21)

        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")
        mac = intent.getStringExtra("mac")

        textView21.setText(getString(R.string.qbee_setup_device_id, mac))

        btnConnect.setOnClickListener {

            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    var intent = Intent(this@QBeeSetupStep4Activity, QBeeSetupStep5Activity::class.java)
                    intent.putExtra("mail", mail)
                    intent.putExtra("pwd", pwd)
                    intent.putExtra("mac", mac)
                    startActivity(intent)
                    this@QBeeSetupStep4Activity.finish()
                }
            }).execute(ApiQBeeBind.getApiBindMacString(mail, pwd, mac))
        }
    }
}
