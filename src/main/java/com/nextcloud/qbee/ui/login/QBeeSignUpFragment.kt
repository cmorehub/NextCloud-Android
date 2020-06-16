package com.nextcloud.qbee.ui.login

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.owncloud.android.R

class QBeeSignUpFragment : Fragment() {

    lateinit var editTextTextEmailAddress: EditText
    lateinit var btnCreateAccount: Button
    lateinit var btnSigninNow: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_signup, container, false)

        editTextTextEmailAddress = root.findViewById(R.id.editTextTextEmailAddress)
        btnCreateAccount = root.findViewById(R.id.btnCreateAccount)
        btnSigninNow = root.findViewById(R.id.btnSigninNow)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = NavHostFragment.findNavController(this)
        btnSigninNow.setOnClickListener {
            val action = QBeeSignUpFragmentDirections.actionQBeeSignUpFragmentToQBeeSignInFragment()
            navController.navigate(action)
        }
        btnCreateAccount.setOnClickListener {
            val pattern = Patterns.EMAIL_ADDRESS
            val email = editTextTextEmailAddress.text.toString()
            if(pattern.matcher(email).matches()){
                QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                    override fun onResult(result: QBeeSetupResult) {
                        var verified = result.code == 0
                        Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                        if (!verified) {
                            val action = QBeeSignUpFragmentDirections.actionQBeeSignUpFragmentToQBeeVerifyFragment(editTextTextEmailAddress.text.toString(), verified)
                            navController.navigate(action)
                        }
                    }
                }).execute(ApiQBeeBind.getApiVerifyCodeString(email))
            }else{
                Toast.makeText(context,R.string.qbee_prompt_email_not_valid,Toast.LENGTH_LONG).show()
            }
        }
    }
}
