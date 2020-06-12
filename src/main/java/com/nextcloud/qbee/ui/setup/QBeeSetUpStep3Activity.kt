package com.nextcloud.qbee.ui.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.blikoon.qrcodescanner.QrCodeActivity
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QBeeSetUpStep3Activity : AppCompatActivity(R.layout.activity_qbee_setup_qbee_3) {

    private val REQUEST_CODE_QR_SCAN = 101

    lateinit var btnScanQRCode: Button

    var mail: String? = null
    var pwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mail = intent.getStringExtra("mail")
        pwd = intent.getStringExtra("pwd")

        btnScanQRCode = findViewById(R.id.btnScanQRCode)

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
                var intent = Intent(this@QBeeSetUpStep3Activity, QBeeSetupStep4Activity::class.java)
                intent.putExtra("mail", mail)
                intent.putExtra("pwd", pwd)
                intent.putExtra("mac", result)
                startActivity(intent)
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
        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startQRScanner()
        } else {
            PermissionUtil.requestCameraPermission(this)
        }
    }

    private fun startQRScanner() {
        val i = Intent(this, QrCodeActivity::class.java)
        startActivityForResult(i, REQUEST_CODE_QR_SCAN)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Boolean) {
        if (event) {
            this@QBeeSetUpStep3Activity.finish()
        }
    }
}
