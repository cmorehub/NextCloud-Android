package com.nextcloud.qbee.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.nextcloud.qbee.ui.event.LoginFinishEvent
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus

class QBeeVerifyFragment : Fragment() {

    lateinit var edtVerifyCode: EditText
    lateinit var edtPassword: EditText
    lateinit var edtConfirmPassword: EditText
    lateinit var btnVerifyCode: Button
    lateinit var btnSignUp: Button
    lateinit var textView14: TextView

    var mail: String? = null

    val args: QBeeVerifyFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_verify_register, container, false)

        edtVerifyCode = root.findViewById(R.id.edtVerifyCode)
        edtPassword = root.findViewById(R.id.edtPassword)
        edtConfirmPassword = root.findViewById(R.id.edtConfirmPassword)
        btnVerifyCode = root.findViewById(R.id.btnVerifyCode)
        btnSignUp = root.findViewById(R.id.btnSignUp)
        textView14 = root.findViewById(R.id.textView14)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtPassword.isEnabled = args.verified
        edtConfirmPassword.isEnabled = args.verified
        btnSignUp.isEnabled = args.verified

        mail = args.mail

        textView14.text = getString(R.string.qbee_check_verify_email, mail)

        val navController = NavHostFragment.findNavController(this)
        btnVerifyCode.setOnClickListener {

            QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                override fun onResult(result: QBeeSetupResult) {
                    val success = result.success
                    edtVerifyCode.isEnabled = !success
                    btnVerifyCode.isEnabled = !success
                    edtPassword.isEnabled = success
                    edtConfirmPassword.isEnabled = success
                    btnSignUp.isEnabled = success
                    Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                }
            }).execute(ApiQBeeBind.getApiVerifyCodeCheckString(mail!!, edtVerifyCode.text.toString()))
        }
        btnSignUp.setOnClickListener {
            val oriPwd = edtPassword.text.toString()
            val confirmPwd = edtConfirmPassword.text.toString()
            if (oriPwd.length in 7..12) {
                if (oriPwd == confirmPwd) {
                    QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                        override fun onResult(result: QBeeSetupResult) {
                            if (result.success) {
                                EventBus.getDefault().post(LoginFinishEvent(true, mail!!, oriPwd, LoginFinishEvent
                                    .LoginForSetup))
                            }
                            Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                        }
                    }).execute(ApiQBeeBind.getApiRegisterString(mail!!, oriPwd))
                } else {
                    Toast.makeText(context, R.string.qbee_confirm_password_error, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, R.string.qbee_password_length_error, Toast.LENGTH_LONG).show()
            }
        }
    }
}
