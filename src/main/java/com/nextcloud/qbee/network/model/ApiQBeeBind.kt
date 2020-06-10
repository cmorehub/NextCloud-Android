package com.nextcloud.qbee.network.model

class ApiQBeeBind {
    companion object {

        val apiUrl = "http://askeyqb.com/askey_macbind.php"

        fun getApiVerifyCodeString(mail: String): String {
            return "type=mailcheck&mail=$mail"
        }

        fun getApiVerifyCodeCheckString(mail: String, code: String): String {
            return "type=codecheck&mail=$mail&code=$code"
        }

        fun getApiRegisterString(mail: String, pwd: String): String {
            return "type=register&mail=$mail&pwd=$pwd"
        }

        fun getApiBindMacString(mail: String, pwd: String, mac: String): String {
            return "type=app_add&mail=$mail&pwd=$pwd&mac=$mac"
        }

        fun getApiLoginString(mail: String, pwd: String): String {
            return "type=login&mail=$mail&pwd=$pwd"
        }
    }
}
