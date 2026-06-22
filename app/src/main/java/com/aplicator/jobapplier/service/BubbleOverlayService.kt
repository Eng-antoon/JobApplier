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
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.aplicator.jobapplier.MainActivity
import com.aplicator.jobapplier.analytics.AnalyticsEvent
import com.aplicator.jobapplier.analytics.AnalyticsEvents
import com.aplicator.jobapplier.analytics.AnalyticsTracker
import com.aplicator.jobapplier.data.export.ExportFormat
import com.aplicator.jobapplier.data.export.GeneratedContentExport
import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.requireSuccessfulPersistence
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.domain.model.GeneratedContent
import com.aplicator.jobapplier.domain.model.UserProfileSnapshot
import com.aplicator.jobapplier.ui.snippets.buildSnippets
import com.aplicator.jobapplier.ui.theme.JobApplierTheme
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

class BubbleOverlayService : LifecycleService(), SavedStateRegistryOwner {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BubbleServiceEntryPoint {
        fun bubbleDataProvider(): BubbleDataProvider
        fun authRepository(): AuthRepository
        fun profileRepository(): ProfileRepository
        fun jobRepository(): JobRepository
        fun aiRepository(): AiRepository
        fun analyticsTracker(): AnalyticsTracker
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleDataProvider: BubbleDataProvider
    private lateinit var authRepository: AuthRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var jobRepository: JobRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var bubblePhysics: BubblePhysics
    private var bubbleView: ComposeView? = null
    private var expandedView: ComposeView? = null
    private var dismissZoneView: ComposeView? = null
    private var isExpanded = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val dismissVisible = MutableStateFlow(false)
    private val dismissHighlighted = MutableStateFlow(false)
    private val generatingActionKey = MutableStateFlow<String?>(null)

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
        authRepository = entryPoint.authRepository()
        profileRepository = entryPoint.profileRepository()
        jobRepository = entryPoint.jobRepository()
        aiRepository = entryPoint.aiRepository()
        analyticsTracker = entryPoint.analyticsTracker()
        analyticsTracker.track(
            AnalyticsEvent(
                AnalyticsEvents.BUBBLE_LAUNCHED,
                mapOf("entry_point" to "service_start"),
            ),
        )
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showBubble()
        loadBubbleData()
    }

    private fun loadBubbleData() {
        lifecycleScope.launch {
            val uid = authRepository.awaitCurrentUserId() ?: return@launch
            profileRepository.getProfile(uid).getOrNull()?.let { profile ->
                bubbleDataProvider.updateUserInfo(profile.fullName, null)
                val skills = profileRepository.getSkills(uid).getOrDefault(emptyList())
                val experiences = profileRepository.getWorkExperiences(uid).getOrDefault(emptyList())
                val education = profileRepository.getEducation(uid).getOrDefault(emptyList())
                val certifications = profileRepository.getCertifications(uid).getOrDefault(emptyList())
                val languages = profileRepository.getLanguages(uid).getOrDefault(emptyList())
                val snippets = buildSnippets(profile, skills, experiences, education, certifications, languages)
                bubbleDataProvider.updateSnippets(snippets)
            }
            jobRepository.getJobs(uid).getOrNull()?.let { jobs ->
                bubbleDataProvider.updateRecentJobs(jobs.take(5).map { it.toBubbleJobItem() })
                jobs.take(5).forEach { job ->
                    val content = jobRepository.getGeneratedContent(job.id).getOrDefault(emptyList())
                    bubbleDataProvider.updateGeneratedContent(
                        job.id,
                        content.map { item ->
                            BubbleContentItem(
                                contentType = item.contentType,
                                content = item.content,
                                tone = item.tone,
                                createdAt = null,
                            )
                        },
                    )
                }
            }
        }
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
        analyticsTracker.flush()
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
        var dragTracked = false

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
                        if (!dragTracked) {
                            analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.BUBBLE_DRAG_STARTED))
                            dragTracked = true
                        }
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
                        analyticsTracker.track(
                            AnalyticsEvent(
                                AnalyticsEvents.BUBBLE_DISMISSED,
                                mapOf("method" to "drag_to_dismiss"),
                            ),
                        )
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
        analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.BUBBLE_OPENED))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        // Hide bubble while panel open
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }

        val density = resources.displayMetrics.density
        val statusBarDp = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android"),
        ) / density
        val panelLayout = expandedBubbleLayout(
            screenWidthDp = (resources.displayMetrics.widthPixels / density).toInt(),
            screenHeightDp = (resources.displayMetrics.heightPixels / density).toInt(),
            statusBarDp = statusBarDp.toInt(),
        )

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleOverlayService)
            setViewTreeSavedStateRegistryOwner(this@BubbleOverlayService)
            setContent {
                JobApplierTheme {
                    val snippets by bubbleDataProvider.snippets.collectAsState()
                    val userName by bubbleDataProvider.userName.collectAsState()
                    val recentJobs by bubbleDataProvider.recentJobs.collectAsState()
                    val generatedContent by bubbleDataProvider.generatedContent.collectAsState()
                    val activeGeneration by generatingActionKey.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Scrim — tap anywhere outside panel to close
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { hideExpanded() },
                        )

                        Box(
                            modifier = Modifier
                                .width(panelLayout.widthDp.dp)
                                .height(panelLayout.heightDp.dp)
                                .padding(top = (statusBarDp + 8).dp)
                                .align(Alignment.TopCenter)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { /* consume touch — prevent leaking to scrim */ },
                        ) {
                            BubbleExpandedPanel(
                                userName = userName,
                                snippets = snippets,
                                recentJobs = recentJobs,
                                generatedContent = generatedContent,
                                generatingActionKey = activeGeneration,
                                onClose = { hideExpanded() },
                                onCopy = { label, value ->
                                    analyticsTracker.track(
                                        AnalyticsEvent(
                                            AnalyticsEvents.BUBBLE_QUICK_COPY_USED,
                                            mapOf("item_type" to label),
                                        ),
                                    )
                                    copyToClipboard(label, value)
                                    if (BubbleCopyBehavior.dismissPanelAfterCopy) {
                                        hideExpanded()
                                    }
                                },
                                onOpenApp = {
                                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.BUBBLE_OPEN_APP_CLICKED))
                                    startMainActivity()
                                },
                                onAddJob = {
                                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.BUBBLE_ADD_JOB_CLICKED))
                                    startMainActivityForAddJob()
                                },
                                onDismissBubble = {
                                    analyticsTracker.track(
                                        AnalyticsEvent(
                                            AnalyticsEvents.BUBBLE_DISMISSED,
                                            mapOf("method" to "actions_tab"),
                                        ),
                                    )
                                    hideExpanded()
                                    removeBubble()
                                    stopSelf()
                                },
                                onRefresh = {
                                    analyticsTracker.track(AnalyticsEvent(AnalyticsEvents.BUBBLE_REFRESH_CLICKED))
                                    hideExpanded()
                                },
                                onGenerate = { job, action, question, tone ->
                                    analyticsTracker.track(
                                        AnalyticsEvent(
                                            AnalyticsEvents.BUBBLE_ACTION_SELECTED,
                                            mapOf("action" to action.contentType),
                                        ),
                                    )
                                    generateBubbleContent(job, action, question, tone)
                                },
                                onExport = { _, item, fileName, format ->
                                    exportBubbleContent(item, fileName, format)
                                },
                                onTabSelected = { tabName ->
                                    analyticsTracker.track(
                                        AnalyticsEvent(
                                            AnalyticsEvents.BUBBLE_TAB_SELECTED,
                                            mapOf("tab_name" to tabName),
                                        ),
                                    )
                                },
                                onJobSelected = { jobId, roleTitle ->
                                    analyticsTracker.track(
                                        AnalyticsEvent(
                                            AnalyticsEvents.BUBBLE_JOB_SELECTED,
                                            mapOf("job_id" to jobId, "role_title" to roleTitle),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        expandedView = view
        windowManager.addView(view, params)
        isExpanded = true
    }

    private fun hideExpanded() {
        if (isExpanded) {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.BUBBLE_CLOSED,
                    mapOf("method" to "close"),
                ),
            )
        }
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

    private fun generateBubbleContent(
        bubbleJob: BubbleJobItem,
        action: BubbleAiAction,
        question: String?,
        tone: String = "professional",
    ) {
        if (generatingActionKey.value != null) {
            Toast.makeText(this, "Generation already in progress", Toast.LENGTH_SHORT).show()
            return
        }

        val actionKey = bubbleActionKey(bubbleJob.jobId, action.contentType)
        lifecycleScope.launch {
            analyticsTracker.track(
                AnalyticsEvent(
                    AnalyticsEvents.AI_CONTENT_GENERATION_STARTED,
                    mapOf(
                        "surface" to "bubble",
                        "content_type" to action.contentType,
                        "tone" to tone,
                    ),
                ),
            )
            generatingActionKey.value = actionKey
            try {
                withTimeout(120_000L) {
                    val uid = authRepository.awaitCurrentUserId()
                    if (uid == null) {
                        Toast.makeText(this@BubbleOverlayService, "Sign in to generate content", Toast.LENGTH_SHORT).show()
                        return@withTimeout
                    }

                    val job = jobRepository.getJob(bubbleJob.jobId).getOrNull()
                    val jobDescription = job?.rawText?.takeIf { it.isNotBlank() }
                        ?: bubbleJob.rawText?.takeIf { it.isNotBlank() }

                    if (jobDescription == null) {
                        Toast.makeText(this@BubbleOverlayService, "Job description is missing", Toast.LENGTH_SHORT).show()
                        return@withTimeout
                    }

                    val profile = getProfileSnapshot(uid)
                    if (profile == null) {
                        Toast.makeText(this@BubbleOverlayService, "Profile data is missing", Toast.LENGTH_SHORT).show()
                        return@withTimeout
                    }

                    val result = when (action.contentType) {
                        "cover_letter" -> aiRepository.generateCoverLetter(
                            jobDescription = jobDescription,
                            userProfile = profile.toPromptText(),
                            tone = tone,
                            additionalInstructions = null,
                        )
                        "cover_email" -> aiRepository.generateCoverEmail(
                            jobDescription = jobDescription,
                            userProfile = profile.toPromptText(),
                            tone = tone,
                        )
                        else -> aiRepository.answerQuestion(
                            question = question?.takeIf { it.isNotBlank() } ?: action.label,
                            questionType = action.contentType,
                            jobDescription = jobDescription,
                            userProfile = profile.toPromptText(),
                            tone = tone,
                        )
                    }

                    result.requireSuccessfulPersistence { response ->
                        jobRepository.insertGeneratedContent(
                            uid,
                            GeneratedContent(
                                jobId = bubbleJob.jobId,
                                contentType = action.contentType,
                                content = response.content,
                                tone = tone,
                            ),
                        )
                    }
                        .onSuccess { response ->
                            refreshBubbleContent(bubbleJob.jobId)
                            analyticsTracker.track(
                                AnalyticsEvent(
                                    AnalyticsEvents.AI_CONTENT_GENERATION_SUCCEEDED,
                                    mapOf(
                                        "surface" to "bubble",
                                        "content_type" to action.contentType,
                                        "tone" to tone,
                                    ),
                                ),
                            )
                            Toast.makeText(this@BubbleOverlayService, "Generated: ${action.label}", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { error ->
                            analyticsTracker.track(
                                AnalyticsEvent(
                                    AnalyticsEvents.AI_CONTENT_GENERATION_FAILED,
                                    mapOf(
                                        "surface" to "bubble",
                                        "content_type" to action.contentType,
                                        "reason" to if (error is com.aplicator.jobapplier.data.remote.ai.QuotaExceededException) "quota_exceeded" else error.javaClass.simpleName,
                                        "quota_type" to (error as? com.aplicator.jobapplier.data.remote.ai.QuotaExceededException)?.quotaInfo?.quotaType,
                                    ),
                                ),
                            )
                            Toast.makeText(
                                this@BubbleOverlayService,
                                quotaAwareMessage(error as Exception),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (exception: Exception) {
                analyticsTracker.track(
                    AnalyticsEvent(
                        AnalyticsEvents.AI_CONTENT_GENERATION_FAILED,
                        mapOf(
                            "surface" to "bubble",
                            "content_type" to action.contentType,
                            "reason" to exception.javaClass.simpleName,
                        ),
                    ),
                )
                Toast.makeText(
                    this@BubbleOverlayService,
                    quotaAwareMessage(exception),
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                generatingActionKey.value = null
            }
        }
    }

    private suspend fun getProfileSnapshot(uid: String): UserProfileSnapshot? {
        val profile = profileRepository.getProfile(uid).getOrNull() ?: return null
        return UserProfileSnapshot(
            fullName = profile.fullName,
            email = profile.email,
            desiredRole = profile.desiredRole,
            summary = profile.summary,
            skills = profileRepository.getSkills(uid).getOrDefault(emptyList()),
            experiences = profileRepository.getWorkExperiences(uid).getOrDefault(emptyList()),
            educationList = profileRepository.getEducation(uid).getOrDefault(emptyList()),
            certifications = profileRepository.getCertifications(uid).getOrDefault(emptyList()),
            languages = profileRepository.getLanguages(uid).getOrDefault(emptyList()),
        )
    }

    private fun quotaAwareMessage(exception: Exception): String {
        return if (exception is com.aplicator.jobapplier.data.remote.ai.QuotaExceededException) {
            if (exception.quotaInfo.quotaType == "resume") "Resume parse limit reached. Open the app to request extra quota."
            else "Weekly AI limit reached. Open the app to request extra quota."
        } else {
            exception.message ?: "Could not generate content"
        }
    }

    private suspend fun refreshBubbleContent(jobId: String) {
        val content = jobRepository.getGeneratedContent(jobId).getOrDefault(emptyList())
        bubbleDataProvider.updateGeneratedContent(
            jobId,
            content.map { item ->
                BubbleContentItem(
                    contentType = item.contentType,
                    content = item.content,
                    tone = item.tone,
                    createdAt = null,
                )
            },
        )
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        expandedView?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        Toast.makeText(this, "Copied: $label", Toast.LENGTH_SHORT).show()
    }

    private fun exportBubbleContent(
        item: BubbleContentItem,
        fileName: String,
        format: ExportFormat,
    ) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                GeneratedContentExport.saveToDownloads(
                    context = this@BubbleOverlayService,
                    userFileName = fileName,
                    content = item.content,
                    format = format,
                )
            }
            result
                .onSuccess { savedName ->
                    analyticsTracker.track(
                        AnalyticsEvent(
                            AnalyticsEvents.GENERATED_CONTENT_EXPORTED,
                            mapOf(
                                "surface" to "bubble",
                                "content_type" to item.contentType,
                                "format" to format.name.lowercase(),
                            ),
                        ),
                    )
                    Toast.makeText(this@BubbleOverlayService, "Saved to Downloads: $savedName", Toast.LENGTH_LONG).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@BubbleOverlayService,
                        error.message ?: "Could not export answer",
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
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
