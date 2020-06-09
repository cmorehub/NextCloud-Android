package com.nextcloud.qbee.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R

class QBeeLoginActivity : AppCompatActivity() {

    lateinit var btnLogin: Button
    lateinit var btnCreateNewAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qbee_login)

        btnLogin = findViewById(R.id.btnLogin)
        btnCreateNewAccount = findViewById(R.id.btnCreateNewAccount)

        btnLogin.setOnClickListener {
            var intent = Intent(this, QBeeSignInActivity::class.java)
            startActivity(intent)
            this@QBeeLoginActivity.finish()
        }

        btnCreateNewAccount.setOnClickListener {
            var intent = Intent(this, QBeeSignUpActivity::class.java)
            startActivity(intent)
            this@QBeeLoginActivity.finish()
        }
    }
}
