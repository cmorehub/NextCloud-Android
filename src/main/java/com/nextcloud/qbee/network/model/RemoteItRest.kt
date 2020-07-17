package com.nextcloud.qbee.network.model

import org.json.JSONObject

class RemoteItRest(val success: Boolean, val result: Any?) {
    companion object {
        val dev = "QzlEQ0JDQzItNjYyMC00RjVCLUIwOTgtQkFBQkNCMzgxRUFG"
        val loginUrl = "https://api.remot3.it/apv/v27/user/login"
        val deviceListUrl = "https://api.remot3.it/apv/v27/device/list/all"
        val deviceProxyUrl = "https://api.remot3.it/apv/v27/device/connect"

        fun getApiMethod(method: String): JSONObject {
            val data = JSONObject()
            data.put("method", method)
            return data
        }

        fun getApiLoginHeader(developerkey: String): JSONObject {
            val data = JSONObject()
            data.put("developerkey", developerkey)
            return data
        }

        fun getApiCommonHeader(developerkey: String, token: String): JSONObject {
            val data = JSONObject()
            data.put("developerkey", developerkey)
            data.put("token", token)
            return data
        }

        fun getApiLoginString(username: String, password: String): JSONObject {
            val data = JSONObject()
            data.put("username", username)
            data.put("password", password)
            return data
        }

        fun getApiDeviceProxyString(deviceaddress: String, wait: Boolean, hostip: String): JSONObject {
            val data = JSONObject()
            data.put("deviceaddress", deviceaddress)
            data.put("wait", wait)
            data.put("hostip", hostip)
            return data
        }
    }
}
