package com.rewriteai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
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
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand begin")
        val notification = try {
            createNotification()
        } catch (e: Exception) {
            Log.e(TAG, "createNotification failed, using fallback", e)
            createMinimalNotification()
        }
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "startForeground done")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }
        fromProcessText = intent?.getBooleanExtra(EXTRA_FROM_PROCESS_TEXT, false) ?: false
        initialText = intent?.getCharSequenceExtra(EXTRA_INITIAL_TEXT)?.toString() ?: ""

        if (overlayView == null) {
            Handler(Looper.getMainLooper()).post {
                Log.d(TAG, "showOverlay posted run")
                try {
                    showOverlay()
                    Log.d(TAG, "showOverlay done")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show overlay", e)
                    e.printStackTrace()
                    stopSelf()
                }
            }
        } else {
            val vm = overlayView?.getTag(R.id.rewrite_view_model_tag) as? RewriteViewModel
            vm?.setOriginalText(initialText)
            if (fromProcessText) {
                isExpanded = true
                vm?.setExpanded(true)
                updateOverlaySize(true)
            }
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
            try {
                val name = try {
                    getString(R.string.notification_channel_name)
                } catch (e: Exception) {
                    "Rewrite AI"
                }
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
                (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "createNotificationChannel failed", e)
            }
        }
    }

    private fun showOverlay() {
        Log.d(TAG, "showOverlay begin")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted, cannot show bubble")
            stopSelf()
            return
        }
        val wm = windowManager ?: run {
            Log.e(TAG, "WindowManager is null")
            stopSelf()
            return
        }
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val panelWidth = (screenWidth * 0.9f).toInt()
        val panelX = (screenWidth - panelWidth) / 2
        val panelY = (metrics.heightPixels * 0.15f).toInt()

        bubbleParams = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = panelX
            y = panelY
        }

        val baseUrl = "${getString(R.string.firebase_rewrite_base_url).trimEnd('/')}/"
        val repository = RewriteRepository(baseUrl)
        val viewModel = RewriteViewModel(repository)
        if (initialText.isNotEmpty()) viewModel.setOriginalText(initialText)
        viewModel.setExpanded(true)

        val composeView = ComposeView(applicationContext).apply {
            setContent {
                MaterialTheme {
                    RewriteOverlayContent(
                        viewModel = viewModel,
                        fromProcessText = fromProcessText,
                        onDismiss = { stopSelf() },
                        onReplace = { text -> sendReplaceResultAndFinish(text) },
                        onCopyAndClose = { stopSelf() },
                        onExpandChanged = { expanded ->
                            isExpanded = expanded
                            updateOverlaySize(expanded)
                        }
                    )
                }
            }
        }

        val wrapper = FrameLayout(this).apply {
            setTag(R.id.rewrite_view_model_tag, viewModel)
            addView(composeView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            setViewTreeLifecycleOwner(this@RewriteOverlayService)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
        }

        overlayView = wrapper
        try {
            Log.d(TAG, "addView about to run")
            wm.addView(overlayView, bubbleParams)
            updateOverlaySize(true)
            Log.d(TAG, "addView done")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
            stopSelf()
        }
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

    /** Fallback if createNotification() throws; uses only system resources so it cannot fail. */
    private fun createMinimalNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rewrite AI")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateOverlaySize(expanded: Boolean) {
        isExpanded = expanded
        val view = overlayView ?: return
        val params = bubbleParams ?: return
        val wm = windowManager ?: return
        val metrics = resources.displayMetrics
        if (expanded) {
            params.width = (metrics.widthPixels * 0.9f).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.x = ((metrics.widthPixels - params.width) / 2f).toInt()
            params.y = (metrics.heightPixels * 0.15f).toInt()
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    (view.context as? RewriteOverlayService)?.stopSelf()
                    true
                } else false
            }
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            view.setOnKeyListener(null)
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
        private const val TAG = "RewriteOverlayService"
        const val EXTRA_INITIAL_TEXT = "initial_text"
        const val EXTRA_FROM_PROCESS_TEXT = "from_process_text"

        private const val CHANNEL_ID = "rewrite_ai_overlay"
        private const val NOTIFICATION_ID = 1001
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
