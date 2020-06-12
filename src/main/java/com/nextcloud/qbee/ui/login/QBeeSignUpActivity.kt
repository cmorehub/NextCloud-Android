package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QBeeSignUpActivity : AppCompatActivity() {

    lateinit var editTextTextEmailAddress: EditText
    lateinit var btnCreateAccount: Button
    lateinit var btnSigninNow: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_signup)

        editTextTextEmailAddress = findViewById(R.id.editTextTextEmailAddress)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        btnSigninNow = findViewById(R.id.btnSigninNow)

        btnCreateAccount.setOnClickListener {
            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    var intent = Intent(this@QBeeSignUpActivity, QBeeVerifyActivity::class.java)
                    intent.putExtra("mail", editTextTextEmailAddress.text.toString())
                    startActivity(intent)
                }
            }).execute(ApiQBeeBind.getApiVerifyCodeString(editTextTextEmailAddress.text.toString()))
        }

        btnSigninNow.setOnClickListener {
            var intent = Intent(this@QBeeSignUpActivity, QBeeSignInActivity::class.java)
            startActivity(intent)
            this@QBeeSignUpActivity.finish()
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
            this@QBeeSignUpActivity.finish()
        }
    }
}
