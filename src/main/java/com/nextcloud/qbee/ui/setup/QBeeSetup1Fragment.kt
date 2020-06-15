package com.nextcloud.qbee.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.owncloud.android.R

class QBeeSetup1Fragment() : Fragment() {

    lateinit var btnNext: ImageButton

    var mail: String? = null
    var pwd: String? = null

    private val args: QBeeSetup1FragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_1, container, false)

        btnNext = root.findViewById(R.id.btnNext)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mail = args.mail
        pwd = args.pwd

        val navController = NavHostFragment.findNavController(this)
        btnNext.setOnClickListener {
            var action = QBeeSetup1FragmentDirections.actionQBeeSetup1FragmentToQBeeSetup2Fragment(mail!!, pwd!!)
            navController.navigate(action)
        }
    }
}
