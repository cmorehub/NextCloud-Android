package com.nextcloud.qbee.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.nextcloud.qbee.network.QBeeMacBindTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.owncloud.android.R

class QBeeSetup4Fragment() : Fragment() {

    lateinit var btnConnect: Button
    lateinit var textView21: TextView

    lateinit var mail: String
    lateinit var pwd: String
    lateinit var mac: String


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_4, container, false)
        btnConnect = root.findViewById(R.id.btnConnect)
        textView21 = root.findViewById(R.id.textView21)

//        mail = intent.getStringExtra("mail")
//        pwd = intent.getStringExtra("pwd")
//        mac = intent.getStringExtra("mac")

        textView21.text = getString(R.string.qbee_setup_device_id, mac)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnConnect.setOnClickListener {

            QBeeMacBindTask(ApiQBeeBind.apiUrl, object : QBeeMacBindTask.Callback {
                override fun onResult(result: String?) {
                    val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                    navController.navigate(R.id.QBeeSetup5Fragment)
                }
            }).execute(ApiQBeeBind.getApiBindMacString(mail, pwd, mac))
        }
    }
}
