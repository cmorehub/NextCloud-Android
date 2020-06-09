package com.nextcloud.qbee.network

import android.os.AsyncTask
import android.util.Log
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class QBeeMacBindTask() : AsyncTask<String, Void, String?>() {
    interface Callback {
        fun onResult(result: String?)
    }

    private var connection: HttpURLConnection? = null
    private var data_output_stream: DataOutputStream? = null
    private var SERVER_URL: String? = null
    private var callback: Callback? = null

    constructor(url: String, callback: Callback) : this() {
        this.SERVER_URL = url
        this.callback = callback
    }

    override fun doInBackground(vararg params: String?): String? {
        var result: String? = null
        try {
            var server_response_code = 0
            val url = URL(SERVER_URL)
            connection = url.openConnection() as HttpURLConnection
            connection!!.doInput = true //Allow Inputs

            connection!!.doOutput = true //Allow Outputs

            connection!!.useCaches = false //Don't use a cached Copy

            connection!!.setRequestProperty("Connection", "Keep-Alive")
            connection!!.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;")
            connection!!.requestMethod = "POST"

            //creating new dataoutputstream
            data_output_stream = DataOutputStream(connection!!.outputStream)
            data_output_stream!!.writeBytes(params?.get(0))
            server_response_code = connection!!.getResponseCode()
            val serverResponseMessage: String = connection!!.responseMessage

            val inputStream: InputStream = connection!!.inputStream
            val stream = ByteArrayOutputStream()
            val buffer2 = ByteArray(10)
            var readCount = 0
            while (inputStream.read(buffer2).also { readCount = it } > 0) {
                stream.write(buffer2, 0, readCount)
            }
            val resultString = stream.toString()
            Log.d("0206", resultString)
            result = if (server_response_code == 200) {
                resultString
            } else {
                null
            }

            data_output_stream!!.flush()
            data_output_stream!!.close()
            inputStream.close()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return result
    }

    override fun onPostExecute(result: String?) {
        callback!!.onResult(result)
    }
}
