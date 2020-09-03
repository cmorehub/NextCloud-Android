package com.askey.qbee.atv.qbeecom

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class QBeeDotCom {

    class QBeeDevice {
        val mac: String = ""
        val remote: String = ""
    }

    open class WusiungResponse {
        val result: Int? = null
    }

    class WusiungResult : WusiungResponse(){
        @com.google.gson.annotations.SerializedName("error")
        val deviceList: Array<QBeeDevice>? = null
    }

    class WusiungError : WusiungResponse() {
        val error: String? = null
    }

    companion object {

        private val formBoundary = "********"
        private val lineEnd = "\r\n"

        @Throws(IOException::class)
        private inline fun <reified T> HttpsURLConnection.getResponseJson(): T {
            val responseCode = this.responseCode
            if (responseCode == 200) {
                return Gson().fromJson(this.inputStream.reader().readText(), T::class.java)
            } else {
                Log.d("RemoteIt", "errorStream ${this.errorStream.reader().readText()}")
                throw IOException("url ${this.url} responses $responseCode!!")
            }
        }

        private fun getApiPostConnection(multipart:Boolean=true): HttpsURLConnection {
            return (URL("https://askeyqb.com/askey_macbind.php").openConnection() as HttpsURLConnection).apply {
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 10000
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type",
                    if(multipart) "multipart/form-data;boundary=$formBoundary"
                    else "application/x-www-form-urlencoded;"
                )
                requestMethod = "POST"
            }
        }

        @Throws(IOException::class)
        private fun HttpsURLConnection.addFormPart(name: String, value: String) {
            this.outputStream.writer(Charsets.UTF_8)
                .append("--$formBoundary")
                .append(lineEnd)
                .append("Content-Disposition: form-data;name=\"$name\"")
                .append(lineEnd)
                .append(lineEnd)
                .append(value)
                .append(lineEnd)
                .flush()
        }

        public suspend fun login(username: String, password: String) :WusiungResponse = withContext(Dispatchers.IO){
            val connection = getApiPostConnection().apply {
                addFormPart("type", "login")
                addFormPart("mail", username)
                addFormPart("pwd", password)
            }
            val response: JsonObject = connection.getResponseJson()
            Log.d("QBeeDotCom", response.toString())
            try {
                Gson().fromJson(response, WusiungResult::class.java)
            } catch (e: JsonSyntaxException) {
                Gson().fromJson(response, WusiungError::class.java)
            }
        }
    }
}
