/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2019 Chris Narkiewicz
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.account

import android.accounts.Account
import android.content.Context
import android.net.Uri
import com.owncloud.android.MainApp
import com.askey.qbee.atv.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudBasicCredentials
import java.net.URI

/**
 * This object represents anonymous user, ie. user that did not log in the Nextcloud server.
 * It serves as a semantically correct "empty value", allowing simplification of logic
 * in various components requiring user data, such as DB queries.
 */
internal class AnonymousUser(private val accountType: String) : User {

    companion object {
        @JvmStatic
        fun fromContext(context: Context): AnonymousUser {
            val type = context.getString(R.string.account_type)
            return AnonymousUser(type)
        }
    }

    override val accountName: String = "anonymous"
    override val server = Server(URI.create(""), MainApp.MINIMUM_SUPPORTED_SERVER_VERSION)
    override val isAnonymous = true

    override fun toPlatformAccount(): Account {
        return Account(accountName, accountType)
    }

    override fun toOwnCloudAccount(): OwnCloudAccount {
        return OwnCloudAccount(Uri.EMPTY, OwnCloudBasicCredentials("", ""))
    }
}
