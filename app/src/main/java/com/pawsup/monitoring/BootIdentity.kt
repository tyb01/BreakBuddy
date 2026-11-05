package com.pawsup.monitoring

import android.content.Context
import android.provider.Settings

/** BOOT_COUNT is available from API 24; reading it requests no new permission. */
object BootIdentity {
    fun read(context: Context): Long = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1).toLong()
    }.getOrDefault(-1)
}
