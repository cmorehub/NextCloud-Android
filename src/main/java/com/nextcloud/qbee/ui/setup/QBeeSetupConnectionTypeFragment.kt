package com.nextcloud.qbee.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.owncloud.android.R

class QBeeSetupConnectionTypeFragment() : Fragment() {

    lateinit var blockOptionEthernet: LinearLayout

    var mail: String? = null
    var pwd: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_type, container, false)
        blockOptionEthernet = root.findViewById(R.id.blockOptionEthernet)

        return root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController(this)
        blockOptionEthernet.setOnClickListener {
            navController.navigate(R.id.QBeeSetup1Fragment)
        }
    }
}
