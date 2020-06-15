package com.nextcloud.qbee.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.nextcloud.qbee.network.QBeeSetupTask
import com.nextcloud.qbee.network.model.ApiQBeeBind
import com.nextcloud.qbee.network.model.QBeeSetupResult
import com.owncloud.android.R

class QBeeSetup4Fragment() : Fragment() {

    lateinit var btnConnect: Button
    lateinit var textView21: TextView
    lateinit var textView22: TextView

    lateinit var mail: String
    lateinit var pwd: String
    lateinit var mac: String


    private val args: QBeeSetup4FragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_4, container, false)
        btnConnect = root.findViewById(R.id.btnConnect)
        textView21 = root.findViewById(R.id.textView21)
        textView22 = root.findViewById(R.id.textView22)

        mail = args.mail
        pwd = args.pwd
        mac = args.mac

        textView21.text = getString(R.string.qbee_setup_device_id, mac)
        textView22.text = getString(R.string.qbee_setup_device_status, "未配對")

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnConnect.setOnClickListener {

            QBeeSetupTask(ApiQBeeBind.apiUrl, object : QBeeSetupTask.Callback {
                override fun onResult(result: QBeeSetupResult) {
                    if (result.success) {
                        val navController = NavHostFragment.findNavController(this@QBeeSetup4Fragment)
                        var action = QBeeSetup4FragmentDirections.actionQBeeSetup4FragmentToQBeeSetup5Fragment(mail!!,
                            pwd!!, mac!!)
                        navController.navigate(action)
                    }
                    Toast.makeText(context, result.result as String, Toast.LENGTH_LONG).show()
                }
            }).execute(ApiQBeeBind.getApiBindMacString(mail, pwd, mac))
        }
    }
}
