package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.ui.setup.QBeeSetupActivity
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QBeeVerifyActivity : AppCompatActivity() {

    lateinit var editTextTextPersonName: EditText
    lateinit var editTextTextPassword2: EditText
    lateinit var editTextTextPassword3: EditText
    lateinit var btnSignUp: Button
    lateinit var textView14: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_verify_register)

        val mail = intent.getStringExtra("mail")

        editTextTextPersonName = findViewById(R.id.editTextTextPersonName)
        editTextTextPassword2 = findViewById(R.id.editTextTextPassword2)
        editTextTextPassword3 = findViewById(R.id.editTextTextPassword3)
        btnSignUp = findViewById(R.id.btnSignUp)
        textView14 = findViewById(R.id.textView14)


        textView14.text = getString(R.string.qbee_check_verify_email, mail)

        btnSignUp.setOnClickListener {
            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                        override fun onResult(result: String?) {
                            var intent = Intent(this@QBeeVerifyActivity, QBeeSetupActivity::class.java)
                            intent.putExtra("mail", mail)
                            intent.putExtra("pwd", editTextTextPassword2.text.toString())
                            startActivity(intent)
                        }
                    }).execute(ApiQBeeBind.getApiRegisterString(mail, editTextTextPassword2.text.toString()))
                }
            }).execute(ApiQBeeBind.getApiVerifyCodeCheckString(mail, editTextTextPersonName.text.toString()))
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
            this@QBeeVerifyActivity.finish()
        }
    }
}
