package com.nextcloud.qbee.ui.event

data class LoginFinishEvent(val success: Boolean, val account: String?, val pwd: String?, val nextTo: Int) {

    companion object {
        val LoginForCloud = 0
        val LoginForSetup = 1
    }
}
