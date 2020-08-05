package com.nextcloud.qbee.ui.widget

import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class SSLUtiles {

    companion object {
        fun createSSLSocketFactory(): SSLSocketFactory? {
            var sSLSocketFactory: SSLSocketFactory? = null
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, arrayOf(TrustAllManager() as TrustManager), SecureRandom())
                sSLSocketFactory = sc.socketFactory
            } catch (e: Exception) {
            }
            return sSLSocketFactory
        }
    }

    class TrustAllManager : X509TrustManager {

        @Throws(CertificateException::class)
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate?> {
            return arrayOfNulls<X509Certificate>(0)
        }
    }

    class TrustAllHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }
}
