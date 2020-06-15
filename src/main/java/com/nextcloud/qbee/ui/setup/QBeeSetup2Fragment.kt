package com.nextcloud.qbee.ui.setup

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

class QBeeSetup2Fragment() : Fragment() {

    lateinit var btnNext: ImageButton

    var mail: String? = null
    var pwd: String? = null

    private val args: QBeeSetup2FragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_2, container, false)

        btnNext = root.findViewById(R.id.btnNext)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mail = args.mail
        pwd = args.pwd

        val navController = NavHostFragment.findNavController(this)
        btnNext.setOnClickListener {
            var action = QBeeSetup2FragmentDirections.actionQBeeSetup2FragmentToQBeeSetup3Fragment(mail!!, pwd!!)
            navController.navigate(action)
        }
    }
}
