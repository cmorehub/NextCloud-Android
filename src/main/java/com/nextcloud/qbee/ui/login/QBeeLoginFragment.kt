package com.nextcloud.qbee.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.R

class QBeeLoginFragment : Fragment() {

    lateinit var btnLogin: Button
    lateinit var btnCreateNewAccount: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_login, container, false)

        btnLogin = root.findViewById(R.id.btnLogin)
        btnCreateNewAccount = root.findViewById(R.id.btnCreateNewAccount)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = NavHostFragment.findNavController(this)
        btnLogin.setOnClickListener {
            val action = QBeeLoginFragmentDirections.actionQBeeLoginFragmentToQBeeSignInFragment()
            navController.navigate(action)
        }
        btnCreateNewAccount.setOnClickListener {
            val action = QBeeLoginFragmentDirections.actionQBeeLoginFragmentToQBeeSignUpFragment()
            navController.navigate(action)
        }
    }
}
