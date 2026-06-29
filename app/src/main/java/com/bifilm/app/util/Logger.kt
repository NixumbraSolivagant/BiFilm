package com.bifilm.app.util

import android.util.Log
import com.bifilm.app.BuildConfig

object Logger {
    private const val GLOBAL_TAG = "BiFilm"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(prefix(tag), message)
    }

    fun w(tag: String, message: String, t: Throwable? = null) {
        if (t != null) Log.w(prefix(tag), message, t) else Log.w(prefix(tag), message)
    }

    fun e(tag: String, message: String, t: Throwable? = null) {
        if (t != null) Log.e(prefix(tag), message, t) else Log.e(prefix(tag), message)
    }

    private fun prefix(tag: String) = "$GLOBAL_TAG/$tag"
}
