package com.nextcloud.qbee.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.nextcloud.qbee.ui.event.SetupDataEvent
import com.owncloud.android.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
            var action = QBeeSetupConnectionTypeFragmentDirections.actionQBeeSetupConnectionTypeFragmentToQBeeSetup1Fragment(mail!!, pwd!!)
            navController.navigate(action)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SetupDataEvent) {
        mail = event!!.mail
        pwd = event!!.pwd
    }
}
