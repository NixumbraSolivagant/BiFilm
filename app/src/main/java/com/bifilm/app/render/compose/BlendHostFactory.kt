package com.bifilm.app.render.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import com.bifilm.app.util.Logger

object BlendHostFactory {
    fun create(context: Context): BlendHost {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslBlendHostImpl(context)
        } else {
            GlEsBlendHost(context)
        }
    }
}