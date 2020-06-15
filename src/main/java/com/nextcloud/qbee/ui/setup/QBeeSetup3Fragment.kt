package com.nextcloud.qbee.ui.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.blikoon.qrcodescanner.QrCodeActivity
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil

class QBeeSetup3Fragment() : Fragment() {

    private val REQUEST_CODE_QR_SCAN = 101

    lateinit var btnScanQRCode: Button

    var mail: String? = null
    var pwd: String? = null

    private val args: QBeeSetup3FragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_qbee_setup_qbee_3, container, false)

        btnScanQRCode = root.findViewById(R.id.btnScanQRCode)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mail = args.mail
        pwd = args.pwd

        btnScanQRCode.setOnClickListener {
            onScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_QR_SCAN -> {
                if (data == null) {
                    return
                }
                val result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult") ?: return
                Log.d("0611", "Scan Result = $result")
                val navController = NavHostFragment.findNavController(this)
                var action = QBeeSetup3FragmentDirections.actionQBeeSetup3FragmentToQBeeSetup4Fragment(mail!!, pwd!!,
                    result!!)
                navController.navigate(action)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PermissionUtil.PERMISSIONS_CAMERA -> {

                // If request is cancelled, result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startQRScanner()
                } else {
                    // permission denied
                    return
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun onScan() {
        if (PermissionUtil.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            startQRScanner()
        } else {
            PermissionUtil.requestCameraPermission(activity)
        }
    }

    private fun startQRScanner() {
        val i = Intent(context, QrCodeActivity::class.java)
        startActivityForResult(i, REQUEST_CODE_QR_SCAN)
    }

}
