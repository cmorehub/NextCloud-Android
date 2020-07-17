package com.nextcloud.qbee.network

import android.os.AsyncTask
import android.util.Log
import com.nextcloud.qbee.network.model.RemoteItRest
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class RemoteItRestTask() : AsyncTask<JSONObject, Void, String?>() {
    interface Callback {
        fun onResult(result: RemoteItRest)
    }

    private var connection: HttpURLConnection? = null
    private var data_output_stream: DataOutputStream? = null
    private var SERVER_URL: String? = null
    private var callback: Callback? = null

    constructor(url: String, callback: Callback) : this() {
        this.SERVER_URL = url
        this.callback = callback
    }

    override fun doInBackground(vararg params: JSONObject?): String? {
        var result: String? = null
        try {
            var server_response_code = 0
            val url = URL(SERVER_URL)
            Log.d("0716", "SERVER_URL:$SERVER_URL")
            connection = url.openConnection() as HttpURLConnection
            connection!!.doInput = true //Allow Inputs
            connection!!.doOutput = params[1]!!.getString("method") == "POST" //Allow Outputs

            connection!!.useCaches = false //Don't use a cached Copy

            connection!!.setRequestProperty("Connection", "Keep-Alive")
            params[0]!!.keys().forEach {
                val prop = params[0]!!.getString(it)
                connection!!.setRequestProperty(it, prop)
                Log.d("0716", "remoteit:$it=$prop")
            }
            connection!!.requestMethod = params[1]!!.getString("method")
            Log.d("0716", "method=${params[1]!!.getString("method")}")

            if (params[1]!!.getString("method") == "POST") {
                connection!!.setRequestProperty("Content-Type", "application/json;")
                //creating new dataoutputstream
                data_output_stream = DataOutputStream(connection!!.outputStream)
                data_output_stream!!.write(params[2]!!.toString().toByteArray())
                data_output_stream!!.flush()
                data_output_stream!!.close()
            }
            server_response_code = connection!!.responseCode
            Log.d("0716", "server_response_code=$server_response_code")
            val serverResponseMessage: String = connection!!.responseMessage
            Log.d("0716", "serverResponseMessage=$serverResponseMessage")

            val inputStream: InputStream = if (server_response_code == 200) {
                connection!!.inputStream
            } else {
                connection!!.errorStream
            }
            val stream = ByteArrayOutputStream()
            val buffer2 = ByteArray(10)
            var readCount = 0
            while (inputStream.read(buffer2).also { readCount = it } > 0) {
                stream.write(buffer2, 0, readCount)
            }
            val resultString = stream.toString()
            Log.d("0716", "resultString:$resultString")
            result = resultString

            inputStream.close()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    override fun onPostExecute(result: String?) {
        if (result != null) {
            var resultJson = JSONObject(result)
            val status = resultJson.optBoolean("status", false)
            val result = RemoteItRest(status, resultJson)
            callback!!.onResult(result)
        }
    }
}
