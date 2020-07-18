package com.nextcloud.qbee.qbeecom

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class QBeeDotCom {
    companion object {
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

        public suspend fun login(username: String, password: String) =
            withContext(Dispatchers.IO) {
                val connection = URL("https://askeyqb.com/askey_macbind.php").openConnection() as HttpsURLConnection
            }
    }
}
