package com.nextcloud.qbee.ui.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.VideoView
import javax.net.ssl.HttpsURLConnection

class SSLVideoView : VideoView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun setVideoURI(uri: Uri?) {
        super.setVideoURI(uri)
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(SSLUtiles.createSSLSocketFactory())
            HttpsURLConnection.setDefaultHostnameVerifier(SSLUtiles.TrustAllHostnameVerifier())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
