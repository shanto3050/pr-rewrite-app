package com.rewriteai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.rewriteai.service.RewriteOverlayService

/**
 * Receives PROCESS_TEXT from the system (user selected text and chose "Rewrite AI").
 * Starts the overlay service with the selected text and stays alive so that
 * when the user taps "Replace", we can setResult(EXTRA_PROCESS_TEXT) and finish().
 * Handles onNewIntent so that when the user selects text again (e.g. second time in LINE),
 * the activity receives the new intent and starts the service with the new text instead of freezing.
 */
class ProcessTextActivity : ComponentActivity() {

    private var replaceReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProcessTextPlaceholder()
        }
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
        startOverlayWithIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startOverlayWithIntent(intent)
    }

    /**
     * Start the overlay service with the selected text from the given intent.
     * Called from onCreate and onNewIntent so the second (and later) text selection from LINE works.
     */
    private fun startOverlayWithIntent(intent: Intent?) {
        val selectedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: ""
            else -> intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            finish()
            return
        }
        val serviceIntent = Intent(this, RewriteOverlayService::class.java).apply {
            putExtra(RewriteOverlayService.EXTRA_INITIAL_TEXT, selectedText)
            putExtra(RewriteOverlayService.EXTRA_FROM_PROCESS_TEXT, true)
        }
        try {
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            finish()
        }
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

@Composable
private fun ProcessTextPlaceholder() {
    Box(Modifier.fillMaxSize())
}
