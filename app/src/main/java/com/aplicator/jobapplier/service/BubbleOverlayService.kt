package com.aplicator.jobapplier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.aplicator.jobapplier.MainActivity
import com.aplicator.jobapplier.ui.theme.JobApplierTheme
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class BubbleOverlayService : LifecycleService(), SavedStateRegistryOwner {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BubbleServiceEntryPoint {
        fun bubbleDataProvider(): BubbleDataProvider
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleDataProvider: BubbleDataProvider
    private lateinit var bubblePhysics: BubblePhysics
    private var bubbleView: ComposeView? = null
    private var expandedView: ComposeView? = null
    private var dismissZoneView: ComposeView? = null
    private var isExpanded = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val dismissVisible = MutableStateFlow(false)
    private val dismissHighlighted = MutableStateFlow(false)

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val CHANNEL_ID = "bubble_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val STOP_ACTION = "com.aplicator.jobapplier.STOP_BUBBLE"
        private const val BUBBLE_SIZE_DP = 62
        private const val NAVIGATE_EXTRA = "navigate_to"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()
        bubblePhysics = BubblePhysics(windowManager, screenWidth, bubbleSizePx)
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BubbleServiceEntryPoint::class.java,
        )
        bubbleDataProvider = entryPoint.bubbleDataProvider()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == STOP_ACTION) {
            removeBubble()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        bubblePhysics.release()
        removeBubble()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bubble Overlay",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows floating bubble for quick copy"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, BubbleOverlayService::class.java).apply {
            action = STOP_ACTION
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("JobApplier Bubble Active")
            .setContentText("Tap to open app")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(openPending)
            .addAction(Notification.Action.Builder(null, "Stop", stopPending).build())
            .setOngoing(true)
            .build()
    }

    private fun showBubble() {
        val density = resources.displayMetrics.density
        val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - bubbleSizePx - 8
            y = 300
        }

        bubbleParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@BubbleOverlayService)
            setContent {
                JobApplierTheme {
                    BubbleContent()
                }
            }
        }

        setupTouchListener(view, params)
        bubbleView = view
        windowManager.addView(view, params)

        bubblePhysics.snapToEdge(view, params, params.x, params.y)
    }

    private fun setupTouchListener(view: ComposeView, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var totalMovement = 0f
        var velocityTracker: VelocityTracker? = null
        var isDragging = false

        view.setOnTouchListener { v, event ->
            if (event.pointerCount > 1) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    totalMovement = 0f
                    isDragging = false
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    totalMovement = abs(dx) + abs(dy)
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    try {
                        windowManager.updateViewLayout(v, params)
                    } catch (_: Exception) {}

                    if (!isDragging && totalMovement > 10f) {
                        isDragging = true
                        showDismissZone()
                    }

                    if (isDragging) {
                        val screenHeight = resources.displayMetrics.heightPixels
                        val density = resources.displayMetrics.density
                        val inZone = bubblePhysics.isInDismissZone(
                            params.x, params.y, screenHeight, density,
                        )
                        dismissHighlighted.value = inZone
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    if (totalMovement < 30f) {
                        hideDismissZone()
                        toggleExpanded()
                    } else if (dismissHighlighted.value) {
                        val screenHeight = resources.displayMetrics.heightPixels
                        val density = resources.displayMetrics.density
                        bubblePhysics.animateToDismiss(v, params, screenHeight, density) {
                            hideDismissZone()
                            removeBubble()
                            stopSelf()
                        }
                    } else {
                        hideDismissZone()
                        bubblePhysics.snapToEdge(v, params, params.x, params.y, vx)
                    }
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    hideDismissZone()
                    isDragging = false
                    bubblePhysics.snapToEdge(v, params, params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    private fun showDismissZone() {
        if (dismissZoneView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        dismissVisible.value = true
        dismissHighlighted.value = false

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@BubbleOverlayService)
            setContent {
                DismissZoneOverlay(
                    isVisible = dismissVisible,
                    isHighlighted = dismissHighlighted,
                )
            }
        }

        dismissZoneView = view
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun hideDismissZone() {
        dismissVisible.value = false
        dismissHighlighted.value = false
        dismissZoneView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            dismissZoneView = null
        }
    }

    private fun toggleExpanded() {
        if (isExpanded) {
            hideExpanded()
        } else {
            showExpanded()
        }
    }

    private fun showExpanded() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // Hide bubble while panel open
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@BubbleOverlayService)
            setContent {
                JobApplierTheme {
                    val snippets by bubbleDataProvider.snippets.collectAsState()
                    val userName by bubbleDataProvider.userName.collectAsState()
                    val recentJobs by bubbleDataProvider.recentJobs.collectAsState()
                    val generatedContent by bubbleDataProvider.generatedContent.collectAsState()

                    BubbleExpandedPanel(
                        userName = userName,
                        snippets = snippets,
                        recentJobs = recentJobs,
                        generatedContent = generatedContent,
                        onClose = { hideExpanded() },
                        onCopy = { label, value -> copyToClipboard(label, value) },
                        onOpenApp = { startMainActivity() },
                        onAddJob = { startMainActivityForAddJob() },
                        onDismissBubble = {
                            hideExpanded()
                            removeBubble()
                            stopSelf()
                        },
                        onRefresh = { hideExpanded() },
                    )
                }
            }
        }

        expandedView = view
        windowManager.addView(view, params)
        isExpanded = true
    }

    private fun hideExpanded() {
        expandedView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            expandedView = null
        }
        isExpanded = false

        // Re-show bubble
        bubbleView?.let { view ->
            bubbleParams?.let { params ->
                try {
                    windowManager.addView(view, params)
                    bubblePhysics.snapToEdge(view, params, params.x, params.y)
                } catch (_: Exception) {}
            }
        }
    }

    private fun removeBubble() {
        hideDismissZone()
        hideExpanded()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            bubbleView = null
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun startMainActivityForAddJob() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NAVIGATE_EXTRA, "add_job")
        }
        startActivity(intent)
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "Copied: $label", Toast.LENGTH_SHORT).show()
    }

    @Composable
    private fun BubbleContent() {
        val userName by bubbleDataProvider.userName.collectAsState()
        val avatarUrl by bubbleDataProvider.avatarUrl.collectAsState()
        val initial = userName.firstOrNull()?.uppercase() ?: "J"

        val infiniteTransition = rememberInfiniteTransition(label = "bubblePulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowPulse",
        )

        Box(
            modifier = Modifier.size(BUBBLE_SIZE_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Messenger-style glow ring
            Box(
                modifier = Modifier
                    .size((BUBBLE_SIZE_DP + 10).dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = 0.30f
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF0F766E).copy(alpha = 0.28f)),
            )

            // Main bubble
            Box(
                modifier = Modifier
                    .size(BUBBLE_SIZE_DP.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF12324A), Color(0xFF0F766E), Color(0xFF06B6D4)),
                            start = Offset.Zero,
                            end = Offset(90f, 90f),
                        ),
                    )
                    .border(width = 2.5.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(BUBBLE_SIZE_DP.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF079455)),
                )
            }
        }
    }
}
