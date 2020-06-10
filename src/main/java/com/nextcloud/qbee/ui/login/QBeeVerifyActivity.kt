package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.owncloud.android.R

class QBeeVerifyActivity : AppCompatActivity() {

    lateinit var editTextTextPersonName: EditText
    lateinit var editTextTextPassword2: EditText
    lateinit var editTextTextPassword3: EditText
    lateinit var btnSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_verify_register)

        val mail = intent.getStringExtra("mail")

        editTextTextPersonName = findViewById(R.id.editTextTextPersonName)
        editTextTextPassword2 = findViewById(R.id.editTextTextPassword2)
        editTextTextPassword3 = findViewById(R.id.editTextTextPassword3)
        btnSignUp = findViewById(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                        override fun onResult(result: String?) {
                            var intent = Intent(this@QBeeVerifyActivity, QBeeSelectConnectionActivity::class.java)
                            intent.putExtra("mail", mail)
                            intent.putExtra("pwd", editTextTextPassword2.text.toString())
                            startActivity(intent)
                            this@QBeeVerifyActivity.finish()
                        }
                    }).execute(ApiQBeeBind.getApiRegisterString(mail, editTextTextPassword2.text.toString()))
                }
            }).execute(ApiQBeeBind.getApiVerifyCodeCheckString(mail, editTextTextPersonName.text.toString()))
        }
    }
}
