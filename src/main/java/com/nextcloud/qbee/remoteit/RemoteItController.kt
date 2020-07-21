package com.nextcloud.qbee.remoteit

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.remoteit.P2PManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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
    }

    @Throws(IOException::class)
    private fun HttpsURLConnection.getResponseJson(): JsonObject {
        val responseCode = this.responseCode
        if (responseCode == 200) {
            return Gson().fromJson(this.inputStream.reader().readText(), JsonObject::class.java)
        } else {
            Log.d("RemoteIt", "errorStream ${this.errorStream.reader().readText()}")
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
    public suspend fun restGetAuthToken(userName: String, password: String) =
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

    @Throws(IOException::class)
    public suspend fun restGetDeviceList(authToken: String, ofType: String? = null): List<String> =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest("https://api.remot3.it/apv/v27/device/list/all")
            urlConnection.setRequestProperty("token", authToken)
            return@withContext with(urlConnection.getResponseJson()) {
                val result = mutableListOf<String>()
                if (this.get("status").asBoolean) {
                    this.getAsJsonArray("devices").forEach {
//                        Log.d("RemoteIt","devicealias=${it.asJsonObject.get("devicealias").asString} " +
//                            "devicetype=${it.asJsonObject.get("devicetype").asString}")
                        if (ofType==null || it.asJsonObject.get("devicetype").asString == ofType)
                            result.add(it.asJsonObject.get("deviceaddress").asString)
                    }
                }
                return@with result
            }
        }

    @Throws(IOException::class)
    public suspend fun restGetRemoteProxy(authToken: String, deviceId: String): String? =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest("https://api.remot3.it/apv/v27/device/connect")
            urlConnection.setRequestProperty("token",authToken)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/json")
            val postBody = Gson().toJson(
                JsonObject().apply {
                    this.addProperty("deviceaddress", deviceId)
                    this.addProperty("wait", true)
                    this.addProperty("hostip","0.0.0.0")
                }
            )
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with if (this.get("status").asBoolean) {
                    this.get("connection").asJsonObject.get("proxy").asString
                } else null
            }
        }

    @Throws(IOException::class)
    public suspend fun peerToPeerLogin(userName: String, password: String) =
        withContext(Dispatchers.IO) {
            p2PManager.signInWithPassword(userName, password, hashMapOf()) // seems not blocking
        }

    @Throws(IOException::class, IndexOutOfBoundsException::class)
    public suspend fun peerToPeerConnect(): String =
        withContext(Dispatchers.IO) {
            return@withContext "" // todo : First device
        }

    @Throws(IOException::class)
    public suspend fun peerToPeerConnect(deviceId: String): String =
        withContext(Dispatchers.IO) {
            return@withContext suspendCoroutine<String> { continuation ->
                p2PManager.setP2PEventListener(object : P2PManager.P2PEventListener {
                    override fun p2pSignedOut(userName: String?) {
                        // Empty : Not required?
                    }

                    override fun p2pConnectionSucceeded(deviceAddress: String, url: String) {
                        p2PManager.setP2PEventListener(null)
                        continuation.resume(url)
                    }

                    override fun p2pConnectionFailed(deviceAddress: String, reason: String) {
                        p2PManager.setP2PEventListener(null)
                        continuation.resumeWithException(Exception(reason))
                    }

                    override fun p2pConnectionDestroyed(deviceAddress: String, reason: String) {
                        // Empty : Not required?
                    }
                })
                p2PManager.connectDevice(deviceId, hashMapOf())
            }
        }
}
