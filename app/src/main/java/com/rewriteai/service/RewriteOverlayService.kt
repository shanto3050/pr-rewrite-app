package com.rewriteai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.rewriteai.MainActivity
import com.rewriteai.ProcessTextActivity
import com.rewriteai.R
import com.rewriteai.data.RewriteRepository
import com.rewriteai.ui.RewriteOverlayContent
import com.rewriteai.ui.RewriteViewModel

class RewriteOverlayService : LifecycleService() {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var isExpanded = false
    private var fromProcessText = false
    private var initialText = ""
    private lateinit var savedStateOwner: ServiceSavedStateOwner

    override fun onCreate() {
        super.onCreate()
        // Create a SavedStateRegistryOwner so Compose is happy even outside an Activity
        savedStateOwner = ServiceSavedStateOwner(this)
        savedStateOwner.performRestore()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Call super so LifecycleService can dispatch lifecycle events correctly
        super.onStartCommand(intent, flags, startId)

        fromProcessText = intent?.getBooleanExtra(EXTRA_FROM_PROCESS_TEXT, false) ?: false
        initialText = intent?.getCharSequenceExtra(EXTRA_INITIAL_TEXT)?.toString() ?: ""

        if (overlayView == null) {
            showOverlay()
        } else {
            // Already showing; update initial text if from PROCESS_TEXT
            (overlayView?.getTag(R.id.rewrite_view_model_tag) as? RewriteViewModel)?.setOriginalText(initialText)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
        }
    }

    private fun showOverlay() {
        startForeground(NOTIFICATION_ID, createNotification())

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - BUBBLE_SIZE - 32
            y = 128
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        val baseUrl = "${getString(R.string.firebase_rewrite_base_url).trimEnd('/')}/"
        val repository = RewriteRepository(baseUrl)
        val viewModel = RewriteViewModel(repository)
        if (initialText.isNotEmpty()) viewModel.setOriginalText(initialText)
        if (fromProcessText) viewModel.setExpanded(true)

        overlayView = ComposeView(this).apply {
            // Attach LifecycleOwner and SavedStateRegistryOwner so Compose can run safely
            setViewTreeLifecycleOwner(this@RewriteOverlayService)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                RewriteOverlayContent(
                    viewModel = viewModel,
                    fromProcessText = fromProcessText,
                    onDismiss = { stopSelf() },
                    onReplace = { text -> sendReplaceResultAndFinish(text) },
                    onCopyAndClose = { stopSelf() },
                    onExpandChanged = { expanded -> updateOverlaySize(expanded) }
                )
            }
        }

        windowManager?.addView(overlayView, bubbleParams)
    }

    private fun createNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateOverlaySize(expanded: Boolean) {
        val view = overlayView ?: return
        val params = bubbleParams ?: return
        val wm = windowManager ?: return
        val metrics = resources.displayMetrics
        if (expanded) {
            // Make the panel wide and center it on screen
            params.width = (metrics.widthPixels * 0.9f).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.x = ((metrics.widthPixels - params.width) / 2f).toInt()
            params.y = (metrics.heightPixels * 0.15f).toInt()
            // Allow the panel to take focus and receive keyboard input
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            // When collapsed, don't steal focus from underlying app
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            // Keep last x/y so the bubble stays where the user left it
        }
        wm.updateViewLayout(view, params)
    }

    private fun sendReplaceResultAndFinish(text: String) {
        val intent = Intent(ProcessTextActivity.ACTION_REPLACE_RESULT).apply {
            setPackage(packageName)
            putExtra(ProcessTextActivity.EXTRA_REPLACE_TEXT, text)
        }
        sendBroadcast(intent)
        stopSelf()
    }

    companion object {
        const val EXTRA_INITIAL_TEXT = "initial_text"
        const val EXTRA_FROM_PROCESS_TEXT = "from_process_text"

        private const val CHANNEL_ID = "rewrite_ai_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val BUBBLE_SIZE = 56
    }
}

/**
 * Simple SavedStateRegistryOwner implementation backed by the service's lifecycle.
 */
private class ServiceSavedStateOwner(
    private val lifecycleService: LifecycleService
) : SavedStateRegistryOwner {

    private val controller: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val lifecycle
        get() = lifecycleService.lifecycle

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry

    fun performRestore() {
        controller.performRestore(null)
    }
}
