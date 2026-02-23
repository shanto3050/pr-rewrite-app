package com.rewriteai

import android.app.Application
import android.util.Log

class RewriteAIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("RewriteAI", "Uncaught exception", throwable)
            throwable.printStackTrace()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
