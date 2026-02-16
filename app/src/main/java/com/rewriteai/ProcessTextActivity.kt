package com.rewriteai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.rewriteai.service.RewriteOverlayService

/**
 * Receives PROCESS_TEXT from the system (user selected text and chose "Rewrite AI").
 * Starts the overlay service with the selected text and stays alive so that
 * when the user taps "Replace", we can setResult(EXTRA_PROCESS_TEXT) and finish().
 */
class ProcessTextActivity : ComponentActivity() {

    private var replaceReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedText = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val serviceIntent = Intent(this, RewriteOverlayService::class.java).apply {
            putExtra(RewriteOverlayService.EXTRA_INITIAL_TEXT, selectedText)
            putExtra(RewriteOverlayService.EXTRA_FROM_PROCESS_TEXT, true)
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        replaceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_REPLACE_RESULT) return
                val text = intent.getStringExtra(EXTRA_REPLACE_TEXT)
                if (text != null) {
                    setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, text))
                }
                finish()
            }
        }
        val filter = IntentFilter(ACTION_REPLACE_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(replaceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(replaceReceiver, filter)
        }
        // Do not finish: wait for Replace broadcast so we can setResult.
    }

    override fun onDestroy() {
        replaceReceiver?.let { unregisterReceiver(it) }
        replaceReceiver = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_REPLACE_RESULT = "com.rewriteai.ACTION_REPLACE_RESULT"
        const val EXTRA_REPLACE_TEXT = "replace_text"
    }
}
