package com.nextcloud.qbee.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nextcloud.qbee.network.model.ApiQBeeBind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class QBeeSetupController(context: Context) {

    companion object {
        const val DEFAULT_REST = "http://askeyqb.com/askey_macbind.php"
    }

    class QBeeSetupResponse(
        @com.google.gson.annotations.SerializedName("result") val success: Boolean,
        @com.google.gson.annotations.SerializedName("error") val data: String
    )

    class QBeeBindDevice(
        @com.google.gson.annotations.SerializedName("mac") val mac: String,
        @com.google.gson.annotations.SerializedName("remote") val remoteit: String
    )

    @Throws(IOException::class)
    private fun HttpURLConnection.getResponseJson(): JsonObject {
        val responseCode = this.responseCode
        if (responseCode == 200) {
            return Gson().fromJson(this.inputStream.reader().readText(), JsonObject::class.java)
        } else {
            Log.d("QBeeSetup", "errorStream ${this.errorStream.reader().readText()}")
            throw IOException("url ${this.url} responses $responseCode!!")
        }
    }

    @Throws(IOException::class)
    private suspend fun restRequest(url: String): HttpURLConnection =
        withContext(Dispatchers.IO) {
            return@withContext (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Connection", "Keep-Alive")
            }
        }

    @Throws(IOException::class)
    public suspend fun restGetVerifyCode(userMail: String) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest(DEFAULT_REST)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            val postBody = ApiQBeeBind.getApiVerifyCodeString(userMail)
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with Gson().fromJson(this, QBeeSetupResponse::class.java)
            }
        }

    @Throws(IOException::class)
    public suspend fun restGetVerifyCodeCheck(userMail: String, verifyCode: String) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest(DEFAULT_REST)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            val postBody = ApiQBeeBind.getApiVerifyCodeCheckString(userMail, verifyCode)
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with Gson().fromJson(this, QBeeSetupResponse::class.java)
            }
        }

    @Throws(IOException::class)
    public suspend fun restPostRegisterData(userMail: String, pwd: String) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest(DEFAULT_REST)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            val postBody = ApiQBeeBind.getApiRegisterString(userMail, pwd)
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with Gson().fromJson(this, QBeeSetupResponse::class.java)
            }
        }

    @Throws(IOException::class)
    public suspend fun restPostBindMacData(userMail: String, pwd: String, mac: String) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest(DEFAULT_REST)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            val postBody = ApiQBeeBind.getApiBindMacString(userMail, pwd, mac)
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with Gson().fromJson(this, QBeeSetupResponse::class.java)
            }
        }

    @Throws(IOException::class)
    public suspend fun restPostLoginData(userMail: String, pwd: String) =
        withContext(Dispatchers.IO) {
            val urlConnection = restRequest(DEFAULT_REST)
            urlConnection.requestMethod = "POST"
            urlConnection.doOutput = true
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            val postBody = ApiQBeeBind.getApiLoginString(userMail, pwd)
            urlConnection.outputStream.bufferedWriter().apply {
                write(postBody)
                flush()
                close()
            }
            return@withContext with(urlConnection.getResponseJson()) {
                return@with if (this.get("result").asInt == 0) {
                    val result = mutableListOf<QBeeBindDevice>()
                    this.getAsJsonArray("devices").forEach {
                        val device = Gson().fromJson(it, QBeeBindDevice::class.java)
                        result.add(device)
                    }
                    result
                } else {
                    Gson().fromJson(this, QBeeSetupResponse::class.java)
                }
            }
        }
}
