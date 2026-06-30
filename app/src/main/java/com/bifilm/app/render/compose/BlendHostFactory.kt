package com.bifilm.app.render.compose

import android.content.Context
import android.os.Build
import com.bifilm.app.util.Logger

object BlendHostFactory {
    fun create(context: Context): BlendHost {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val agsl = AgslBlendHostImpl(context)
            if (agsl.isUsable) {
                return agsl
            }
            Logger.w(TAG, "AGSL not usable on this device, using software fallback host")
            return SoftwareBlendHost()
        }
        Logger.w(TAG, "pre-Android-13 device, using software fallback host")
        return SoftwareBlendHost()
    }

    private const val TAG = "BlendHostFactory"
}