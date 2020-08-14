package com.nextcloud.qbee.remoteit

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nextcloud.qbee.network.QBeeSetupController.Companion.DEFAULT_REST
import com.remoteit.P2PManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RemoteItController(context: Context) {
    private val p2PManager = P2PManager(context)
    private var currentUrl: String? = null

    companion object {
        private const val DEVELOPER_KEY = "QzlEQ0JDQzItNjYyMC00RjVCLUIwOTgtQkFBQkNCMzgxRUFG"
        const val TYPE_BULK = "00:23:81:00:00:04:00:06:04:60:FF:FF:00:01:00:00"
        const val TYPE_HTTP = "00:1E:00:00:00:04:00:08:00:00:18:B5:00:01:00:00"
        const val TYPE_NEXTCLOUD = "00:26:81:00:00:04:00:06:04:60:01:BB:00:01:00:00"
        const val DEFAULT_USERNAME = "ccmaped@gmail.com"
        const val DEFAULT_PASSWORD = "maped1234"
    }

    @Throws(IOException::class)
    private fun HttpsURLConnection.getResponseJson(): JsonObject {
        val responseCode = this.responseCode
        if (responseCode == 200) {
            return Gson().fromJson(this.inputStream.reader().readText(), JsonObject::class.java)
        } else {
            Log.e("RemoteIt", "errorStream ${this.errorStream.reader().readText()}")
            throw IOException("url ${this.url} responses $responseCode!!")
        }
    }

    @Throws(IOException::class)
    private suspend fun restRequest(url: String): HttpsURLConnection =
        withContext(Dispatchers.IO) {
            return@withContext (URL(url).openConnection() as HttpsURLConnection).apply {
                setRequestProperty("developerkey", DEVELOPER_KEY)
            }
        }

    @Throws(IOException::class)
    public suspend fun restGetAuthToken(userName: String = DEFAULT_USERNAME, password: String = DEFAULT_PASSWORD) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest("https://api.remot3.it/apv/v27/user/login")
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/json")
            val postBody = Gson().toJson(
                JsonObject().apply {
                    this.addProperty("username", userName)
                    this.addProperty("password", password)
                }
            )
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with if (this.get("status").asBoolean) {
                    this.get("token").asString
                } else null
            }
        }

    class RemoteDevice(
        @com.google.gson.annotations.SerializedName("devicetype") val type: String,
        @com.google.gson.annotations.SerializedName("devicealias") val name: String,
        @com.google.gson.annotations.SerializedName("deviceaddress") val address: String
    )

    @Throws(IOException::class)
    public suspend fun restGetDeviceList(authToken: String, ofType: String? = null): List<RemoteDevice> =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest("https://api.remot3.it/apv/v27/device/list/all")
            urlConnection.setRequestProperty("token", authToken)
            return@withContext with(urlConnection.getResponseJson()) {
                val result = mutableListOf<RemoteDevice>()
                if (this.get("status").asBoolean) {
                    this.getAsJsonArray("devices").forEach {
                        val device = Gson().fromJson(it, RemoteDevice::class.java)
                        if (ofType == null || device.type == ofType)
                            result.add(device)
                    }
                }
                return@with result
            }
        }

    @Throws(IOException::class)
    public suspend fun restGetRemoteProxy(authToken: String, deviceId: String): String? =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest("https://api.remot3.it/apv/v27/device/connect")
            urlConnection.setRequestProperty("token", authToken)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/json")
            val postBody = Gson().toJson(
                JsonObject().apply {
                    this.addProperty("deviceaddress", deviceId)
                    this.addProperty("wait", true)
                    this.addProperty("hostip", "0.0.0.0")
                }
            )
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with if (this.get("status").asBoolean) {
                    var url = this.get("connection").asJsonObject.get("proxy").asString
                    if (url.startsWith("https")) {
                        url
                    } else {
                        url.replace("http", "https")
                    }
                } else null
            }
        }

    @Throws(IOException::class)
    public suspend fun peerToPeerLogin(userName: String = DEFAULT_USERNAME, password: String = DEFAULT_PASSWORD) =
        withContext(Dispatchers.IO) {
            p2PManager.signInWithPassword(userName, password, hashMapOf()) // seems not blocking
        }

    @Throws(IOException::class, IndexOutOfBoundsException::class)
    public suspend fun peerToPeerConnect(): String =
        withContext(Dispatchers.IO) {
            return@withContext "" // todo : First device
        }

    @Throws(IOException::class, ConnectException::class)
    public suspend fun peerToPeerConnect(deviceId: String): String =
        withContext(Dispatchers.IO) {
            return@withContext suspendCoroutine<String> { continuation ->
                p2PManager.setP2PEventListener(object : P2PManager.P2PEventListener {
                    override fun p2pSignedOut(userName: String?) {
                        // Empty : Not required?
                    }

                    override fun p2pConnectionSucceeded(deviceAddress: String, url: String) {
                        p2PManager.setP2PEventListener(null)
                        continuation.resume(url.replace("http", "https"))
                    }

                    override fun p2pConnectionFailed(deviceAddress: String, reason: String) {
                        p2PManager.setP2PEventListener(null)
                        continuation.resumeWithException(ConnectException(reason))
                    }

                    override fun p2pConnectionDestroyed(deviceAddress: String, reason: String) {
                        // Empty : Not required?
                    }
                })
                p2PManager.connectDevice(deviceId, hashMapOf())
            }
        }
}
