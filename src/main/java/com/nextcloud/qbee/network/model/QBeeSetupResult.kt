package com.nextcloud.qbee.network.model

data class QBeeSetupResult(val success: Boolean, val result: Any?) {
    var code: Int? = null
}
