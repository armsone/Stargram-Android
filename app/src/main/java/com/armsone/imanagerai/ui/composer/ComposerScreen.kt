package com.armsone.imanagerai.ui.composer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.imanagerai.R
import com.armsone.imanagerai.design.BrandSectionTitle
import com.armsone.imanagerai.design.BrandTheme
import com.armsone.imanagerai.design.GlossyPrimaryButton
import com.armsone.imanagerai.design.LocalAppAppearance
import com.armsone.imanagerai.design.StarSegmentedControl
import com.armsone.imanagerai.design.StarSlider
import com.armsone.imanagerai.design.oxbloodPreferenceCard
import com.armsone.imanagerai.design.starCard
import com.armsone.imanagerai.model.AppAppearance
import com.armsone.imanagerai.model.AudienceAgeGroup
import com.armsone.imanagerai.model.CreatorProfile
import com.armsone.imanagerai.model.EmojiIntensity
import com.armsone.imanagerai.model.LineBreakFrequency
import com.armsone.imanagerai.model.PostLength
import com.armsone.imanagerai.model.PostMood
import com.armsone.imanagerai.model.PostStyle
import com.armsone.imanagerai.model.PostTone
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.ui.automation.AutomationSessionState
import com.armsone.imanagerai.ui.automation.AutomationStudio
import com.armsone.imanagerai.ui.externalai.ExternalAIAttachment
import com.armsone.imanagerai.ui.externalai.ExternalAIAutomationPhase
import com.armsone.imanagerai.ui.externalai.ExternalAIErrorSanitizer
import com.armsone.imanagerai.ui.externalai.ExternalAINativeAttachmentBatch
import com.armsone.imanagerai.ui.externalai.ExternalAIFallbackClassifier
import com.armsone.imanagerai.ui.externalai.ExternalAIFallbackReason
import com.armsone.imanagerai.ui.externalai.ExternalAIPollResult
import com.armsone.imanagerai.ui.externalai.ExternalAIScripts
import com.armsone.imanagerai.ui.externalai.ExternalAISecurityPolicy
import com.armsone.imanagerai.ui.externalai.ExternalAIStabilityReducer
import com.armsone.imanagerai.ui.externalai.ExternalAIStabilityState
import com.armsone.imanagerai.ui.externalai.ExternalAISurface
import com.armsone.imanagerai.ui.externalai.ExternalAISurfaceMode
import com.armsone.imanagerai.ui.externalai.ExternalAITimerFormatter
import com.armsone.imanagerai.ui.externalai.ExternalAITimingProfile
import com.armsone.imanagerai.ui.externalai.attachOrderedPhotosToProvider
import com.armsone.imanagerai.ui.externalai.ExternalAIDiagnosticsStore
import com.armsone.imanagerai.ui.externalai.dispatchAIBISubmitOnce
import com.armsone.imanagerai.ui.externalai.drainAIBIDiagnostics
import com.armsone.imanagerai.ui.externalai.evaluateForAIBI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ComposerScreen(
    viewModel: ComposerViewModel,
    automationPickerTrigger: Int = 0,
    cameraShortcutTrigger: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profileStore.profile.collectAsStateWithLifecycle()
    val automationEnabled by viewModel.profileStore.automationEnabled.collectAsStateWithLifecycle()
    val automationSessionState by viewModel.automationSessionState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val appearance = LocalAppAppearance.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissKeyboardOnUserScroll = remember(focusManager, keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(state.lastImportSuccessToken) {
        if (state.lastImportSuccessToken > 0L) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val maxSelection = maxOf(1, MediaAttachmentPolicy.availableSlots(state.mediaItems.size))
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = if (maxSelection < 2) 2 else maxSelection)
    ) { uris ->
        viewModel.addPickedMedia(uris.take(maxSelection), context.contentResolver)
    }

    var isMediaDropTargeted by remember { mutableStateOf(false) }
    val mediaDropTarget = remember(context, viewModel) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isMediaDropTargeted = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isMediaDropTargeted = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isMediaDropTargeted = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isMediaDropTargeted = false
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData ?: return false
                val uris = buildList {
                    for (index in 0 until clipData.itemCount) {
                        clipData.getItemAt(index).uri?.let(::add)
                    }
                }
                if (uris.isEmpty()) return false

                val permissions = context.findActivity()?.requestDragAndDropPermissions(androidEvent)
                viewModel.addPickedMedia(
                    uris = uris,
                    resolver = context.contentResolver,
                    onFinished = { permissions?.release() }
                )
                return true
            }
        }
    }

    var showsContinuousCamera by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) showsContinuousCamera = true
    }

    val requestOpenCamera = remember(context) {
        {
            val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            if (!hasCamera) {
                viewModel.cameraUnavailable()
            } else {
                val permissions = buildList {
                    add(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (permissions.isEmpty()) showsContinuousCamera = true
                else cameraPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    LaunchedEffect(cameraShortcutTrigger) {
        if (cameraShortcutTrigger > 0) requestOpenCamera()
    }

    val automationPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MediaAttachmentPolicy.MAX_ITEMS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val images = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                    }
                }
                if (images.isNotEmpty()) viewModel.startOrdinaryPhotoAutomation(images)
            }
        }
    }

    LaunchedEffect(automationPickerTrigger) {
        if (automationPickerTrigger > 0) {
            automationPhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    LaunchedEffect(automationEnabled) {
        if (!automationEnabled) viewModel.cancelOrdinaryAutomationIfActive()
    }

    AutomationStudio(
        state = automationSessionState,
        onStartAutomation = { images, provider -> viewModel.startPhotoAutomation(images, provider) },
        onCancel = { viewModel.cancelAutomationSession() }
    )

    if (automationSessionState is AutomationSessionState.Processing) {
        val processing = automationSessionState as AutomationSessionState.Processing
        if (processing.attachments.isNotEmpty()) {
            HiddenExternalAIWebView(
                provider = processing.provider,
                prompt = profile.photoOnlyPrompt(profile.mood, profile.preferredLength, processing.attachments.size),
                attachments = processing.attachments,
                requestId = processing.requestId,
                onSubmitted = { viewModel.onAutomationStudioSubmitted() },
                onFallbackRequired = { reason ->
                    viewModel.onAutomationStudioError("FALLBACK_REQUIRED:${reason.name}", processing.provider)
                },
                onError = { err -> viewModel.onAutomationStudioError(err, processing.provider) },
                onSuccess = { answer -> viewModel.onAutomationStudioSuccess(answer, context) }
            )
        }
    }

    if (showsContinuousCamera) {
        ContinuousCameraCapture(
            maxCount = MediaAttachmentPolicy.MAX_ITEMS,
            currentCount = state.mediaItems.size,
            onDone = { photos ->
                showsContinuousCamera = false
                scope.launch {
                    photos.forEach { bytes ->
                        val saved = withContext(Dispatchers.IO) { saveImageToGallery(context, bytes) }
                        viewModel.addCameraPhoto(bytes, gallerySaved = saved)
                    }
                }
            },
            onCancel = { showsContinuousCamera = false }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.canvasBrush(appearance))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(dismissKeyboardOnUserScroll)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Hero
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = BrandTheme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "오늘 어떤 이야기를 전할까요?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTheme.labelPrimary(appearance),
                        lineHeight = 28.sp,
                        maxLines = 2
                    )
                }
                if (appearance == AppAppearance.BK) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 3.dp)
                            .background(BrandTheme.accent, RoundedCornerShape(1.5.dp))
                    )
                }
            }

            // Distinct Top Panel (Media & Writing Content)
            Column(
                Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .starCard(appearance)
                    .testTag("composer.creationCard"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 미디어 섹션 (미디어 우선 작문 흐름)
                val mediaSectionShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0 &&
                                    event.mimeTypes().any { mime ->
                                        mime.startsWith("image/") || mime.startsWith("video/")
                                    }
                            },
                            target = mediaDropTarget
                        )
                        .then(
                            if (isMediaDropTargeted) {
                                Modifier.border(2.dp, BrandTheme.accent, mediaSectionShape)
                            } else {
                                Modifier
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrandSectionTitle(
                        title = "미디어",
                        icon = Icons.Filled.PhotoLibrary,
                        appearance = appearance
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BorderedActionButton(
                            title = "미디어",
                            icon = Icons.Filled.AddPhotoAlternate,
                            enabled = !state.isLoadingMedia &&
                                MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0,
                            appearance = appearance,
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("composer.pickMedia")
                        )
                        BorderedActionButton(
                            title = "카메라",
                            icon = Icons.Filled.PhotoCamera,
                            enabled = MediaAttachmentPolicy.availableSlots(state.mediaItems.size) > 0,
                            appearance = appearance,
                            onClick = requestOpenCamera,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("composer.camera")
                        )
                    }

                    if (state.mediaItems.isNotEmpty()) {
                        MediaOrderEditor(viewModel, state)
                    }

                    if (state.mediaItems.isNotEmpty() || state.isLoadingMedia) {
                        MediaPreview(state.mediaItems, state.previewAspect.ratio, state.isLoadingMedia)
                    }

                    if (state.mediaItems.isNotEmpty()) {
                        StarSegmentedControl(
                            options = PreviewAspect.entries.map { it.title },
                            selectedIndex = PreviewAspect.entries.indexOf(state.previewAspect),
                            appearance = appearance,
                            onSelect = { viewModel.setPreviewAspect(PreviewAspect.entries[it]) },
                            modifier = Modifier.testTag("composer.aspect")
                        )
                    }
                }

                // 3. 이야기 / 초안 단일 편집 필드 (생성/외부 결과도 동일 필드에 반영)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val source = state.activeCaptionSource
                    if (source != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                sourceIcon(source),
                                contentDescription = null,
                                tint = BrandTheme.labelSecondary(appearance),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                source.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelSecondary(appearance)
                            )
                        }
                    }

                    IdeaField(
                        value = state.idea,
                        onValueChange = viewModel::setIdea,
                        isGenerating = state.isGenerating,
                        appearance = appearance,
                        modifier = Modifier.testTag("composer.idea")
                    )

                    // 실시간 검증 리포트
                    if (state.idea.trim().isNotEmpty()) {
                        val validation = viewModel.activeValidationReport()
                        if (validation != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        if (validation.passesAllRules) Icons.Filled.Verified else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (validation.passesAllRules) BrandTheme.green else BrandTheme.orange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        if (validation.passesAllRules) "기준 통과" else "확인 필요",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (validation.passesAllRules) BrandTheme.green else BrandTheme.orange,
                                        modifier = Modifier.testTag("preview.validation")
                                    )
                                }
                            }

                            if (!validation.passesAllRules) {
                                Text(
                                    validation.failedRuleDescriptions.joinToString(" · "),
                                    fontSize = 11.sp,
                                    color = BrandTheme.orange,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // 4. 스타일 요약 문구
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = BrandTheme.labelSecondary(appearance),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "${profile.mood.rawValue} · ${profile.style.title} · ${profile.tone.title} · ${profile.controls.characterCount}자",
                        fontSize = 13.sp,
                        color = BrandTheme.labelSecondary(appearance),
                        modifier = Modifier.testTag("composer.styleSummary")
                    )
                }

                // 5. AI 생성 버튼 (Gemini, ChatGPT, Claude, 기기 AI)
                AiChoiceButtons(viewModel, state)

                // 6. 공유 버튼 (게시 대상 일반화 라벨, 현재 편집된 텍스트 공유)
                if (state.idea.trim().isNotEmpty()) {
                    val shareEnabled = !state.isPreparingShare && !state.isGenerating
                    GlossyPrimaryButton(
                        onClick = {
                            scope.launch {
                                val intent = viewModel.prepareShare(context)
                                if (intent != null) {
                                    val chooser = Intent.createChooser(intent, null).apply {
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(chooser)
                                }
                            }
                        },
                        enabled = shareEnabled,
                        appearance = appearance,
                        modifier = Modifier.testTag("preview.share")
                    ) {
                        if (state.isPreparingShare) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (state.isPreparingShare) "준비 중" else "공유하기 →",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // 상태 또는 에러 메시지
                val message = state.errorMessage ?: state.statusMessage
                if (message != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (state.errorMessage == null) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (state.errorMessage == null) BrandTheme.accent else BrandTheme.red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            message,
                            fontSize = 13.sp,
                            color = if (state.errorMessage == null) BrandTheme.accent else BrandTheme.red,
                            modifier = Modifier.testTag("composer.message")
                        )
                    }
                }
            }

            // 하단 글쓰기 설정 카드 (별도 독립 패널)
            Column(
                Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                WritingSettingsCard(
                    viewModel = viewModel,
                    profile = profile,
                    appearance = appearance
                )
            }
        }
    }
}

// MARK: - 글쓰기 설정 카드

@Composable
private fun WritingSettingsCard(
    viewModel: ComposerViewModel,
    profile: CreatorProfile,
    appearance: AppAppearance
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .oxbloodPreferenceCard(appearance),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .testTag("settings.toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = null,
                tint = BrandTheme.labelPrimary(appearance),
                modifier = Modifier.size(16.dp)
            )
            Text(
                "글 스타일 설정",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTheme.labelPrimary(appearance),
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                tint = BrandTheme.labelSecondary(appearance),
                modifier = Modifier.size(18.dp)
            )
        }

        if (isExpanded) {
        // 글자 수
        SettingsRow(
            title = "글자 수 (${profile.controls.characterCount}자)",
            icon = Icons.Filled.FormatSize,
            appearance = appearance
        ) {
            StarSlider(
                value = profile.controls.characterCount.toFloat(),
                onValueChange = { viewModel.setCharacterCount(it.toInt()) },
                valueRange = 50f..500f,
                step = 10f,
                appearance = appearance,
                modifier = Modifier.testTag("settings.characterCount")
            )
        }

        // 이모지 사용 (한 줄: 안씀, 최소한, 적극적, 과하게)
        SettingsRow(title = "이모지 사용", icon = Icons.Filled.EmojiEmotions, appearance = appearance) {
            StarSegmentedControl(
                options = EmojiIntensity.entries.map { it.title },
                selectedIndex = EmojiIntensity.entries.indexOf(profile.emojiIntensity),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(emojiIntensity = EmojiIntensity.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.emojiIntensity")
            )
        }

        // 분위기
        SettingsRow(title = "분위기", icon = Icons.Filled.WbSunny, appearance = appearance) {
            StarSegmentedControl(
                options = PostMood.entries.map { it.rawValue },
                selectedIndex = PostMood.entries.indexOf(profile.mood),
                onSelect = { index ->
                    viewModel.setMood(PostMood.entries[index])
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.mood")
            )
        }

        // 스타일 (한 줄: 메모, 시, 일기, 수필, 소설)
        SettingsRow(title = "스타일", icon = Icons.Filled.MenuBook, appearance = appearance) {
            StarSegmentedControl(
                options = PostStyle.entries.map { it.title },
                selectedIndex = PostStyle.entries.indexOf(profile.style),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(style = PostStyle.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.style")
            )
        }

        // 말투
        SettingsRow(title = "말투", icon = Icons.Filled.ChatBubble, appearance = appearance) {
            StarSegmentedControl(
                options = PostTone.entries.map { it.title },
                selectedIndex = PostTone.entries.indexOf(profile.tone),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(tone = PostTone.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.tone")
            )
        }

        // 나잇대 (한 줄: XZ, X, 386, 꼰대)
        SettingsRow(title = "나잇대", icon = Icons.Filled.AccessTime, appearance = appearance) {
            StarSegmentedControl(
                options = AudienceAgeGroup.entries.map { it.title },
                selectedIndex = AudienceAgeGroup.entries.indexOf(profile.ageGroup),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(ageGroup = AudienceAgeGroup.entries[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.ageGroup")
            )
        }

        // 줄넘김 (한 줄: 최소, 적당히, 자주)
        val lineBreakOptions = listOf(LineBreakFrequency.MINIMAL, LineBreakFrequency.MODERATE, LineBreakFrequency.FREQUENT)
        SettingsRow(title = "줄넘김", icon = Icons.Filled.KeyboardReturn, appearance = appearance) {
            StarSegmentedControl(
                options = lineBreakOptions.map { it.title },
                selectedIndex = lineBreakOptions.indexOf(profile.lineBreakFrequency),
                onSelect = { index ->
                    viewModel.profileStore.updateProfile { it.copy(lineBreakFrequency = lineBreakOptions[index]) }
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.lineBreakFrequency")
            )
        }

        // 내 글 반영
        SettingsRow(title = "내 글 반영", icon = Icons.Filled.Edit, appearance = appearance) {
            StarSegmentedControl(
                options = PostLength.entries.map { it.storyWeightTitle },
                selectedIndex = PostLength.entries.indexOf(profile.preferredLength),
                onSelect = { index ->
                    viewModel.setLength(PostLength.entries[index])
                },
                appearance = appearance,
                modifier = Modifier.testTag("settings.length")
            )
        }

        // 추가로 하고 싶은 설정 (여러 줄)
        SettingsRow(title = "추가로 하고 싶은 설정", icon = Icons.Filled.RateReview, appearance = appearance) {
            MultilineInputRow(
                value = profile.detailedGuidelines,
                placeholder = "예: 이모티콘 대신 물결표를 즐겨 써줘 / 문장은 짧게 끊어줘",
                onValueChange = { viewModel.profileStore.updateProfile { p -> p.copy(detailedGuidelines = it) } },
                minLines = 2,
                maxLines = 6,
                appearance = appearance,
                testTag = "settings.detailedGuidelines"
            )
        }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    appearance: AppAppearance,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandTheme.labelPrimary(appearance),
                modifier = Modifier.size(14.dp)
            )
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTheme.labelPrimary(appearance)
            )
        }
        content()
    }
}

@Composable
private fun MultilineInputRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 2,
    maxLines: Int = 6,
    appearance: AppAppearance,
    testTag: String
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(8.dp)
    val bg = if (isBk) Color.White else BrandTheme.surface
    val border = BorderStroke(0.8.dp, if (isBk) Color(0xFFD6DAE0) else BrandTheme.border)

    Box(
        Modifier
            .fillMaxWidth()
            .border(border, shape)
            .background(bg, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 18.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 13.sp, color = BrandTheme.labelPrimary(appearance), lineHeight = 18.sp),
            minLines = minLines,
            maxLines = maxLines,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

// MARK: - 단일 이야기 / 초안 텍스트 필드

@Composable
private fun IdeaField(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(14.dp)
    val bg = if (isBk) Color(0xFFF6F8FA) else BrandTheme.canvas
    val border = if (isBk) BorderStroke(1.dp, Color(0xFFE2E6EC)) else null

    Box(
        modifier
            .fillMaxWidth()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(bg, shape)
            .padding(14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                "이야기를 적어 주세요 · AI 결과도 여기에 채워져요",
                fontSize = 17.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 22.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !isGenerating,
            textStyle = TextStyle(
                fontSize = 17.sp,
                color = BrandTheme.labelPrimary(appearance),
                lineHeight = 23.sp
            ),
            minLines = 3,
            maxLines = 20,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BorderedActionButton(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val shape = RoundedCornerShape(13.dp)
    val bg = if (enabled) Color(0xFF1F2024) else Color(0xFF18191C)
    val borderStroke = BorderStroke(
        0.8.dp,
        if (enabled) Color(0x38FFFFFF) else Color(0x18FFFFFF)
    )
    val iconTint = if (enabled) BrandTheme.accent else BrandTheme.accent.copy(alpha = 0.35f)
    val textColor = if (enabled) Color(0xFFF3F4F6) else Color(0xFF7A7E87)

    Row(
        modifier = modifier
            .height(48.dp)
            .border(borderStroke, shape)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// MARK: - AI 선택 버튼

@Composable
private fun AiChoiceButtons(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val appearance = LocalAppAppearance.current
    val showsExternalAIBrowser by viewModel.profileStore.showsExternalAIBrowser.collectAsStateWithLifecycle()
        val hasGenerationInput = state.idea.trim().isNotEmpty() || viewModel.hasRepresentativePhoto
        val isEnabled = !state.isGenerating && hasGenerationInput

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val columns = 4
        AIChoice.all.chunked(columns).forEach { rowChoices ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowChoices.forEach { choice ->
                    AIChoiceCard(
                        choice = choice,
                        enabled = isEnabled,
                        onClick = {
                            when (choice) {
                                AIChoice.OnDevice -> viewModel.generateDraft()
                                is AIChoice.External -> viewModel.startExternalGeneration(choice.provider)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("composer.ai.${choice.id}")
                    )
                }
                repeat(columns - rowChoices.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (state.isGenerating) {
            GenerationStatusCard(
                viewModel = viewModel,
                state = state,
                appearance = appearance
            )
        }

        val pendingProvider = state.pendingExternalProvider
        if (pendingProvider != null && !state.isGenerating) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(BrandTheme.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable {
                        val text = viewModel.readClipboard(context)
                        viewModel.importAIResult(text, pendingProvider, context)
                    }
                    .testTag("composer.paste"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    tint = BrandTheme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("붙여넣기", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BrandTheme.accent)
            }
        }
    }

    // 설정이 꺼져 있으면 백그라운드 WebView, 켜져 있으면 보이는 WebView에서 수행한다.
    val automationProvider = state.activeAutomationProvider
    if (
        state.isGenerating &&
        automationProvider != null &&
        !showsExternalAIBrowser &&
        !state.isFallbackBrowserVisible
    ) {
        HiddenExternalAIWebView(
            provider = automationProvider,
            prompt = viewModel.externalPrompt(),
            attachments = state.activeAttachments,
            requestId = state.automationRequestId,
            onSubmitted = {
                viewModel.onAutomationSubmitted()
            },
            onFallbackRequired = { reason ->
                viewModel.onAutomationFallbackRequired(reason)
            },
            onError = { err ->
                viewModel.onAutomationError(err, automationProvider)
            },
            onSuccess = { answer ->
                viewModel.importAIResult(answer, automationProvider, context)
            }
        )
    }

    // 사용자가 항상 보기를 선택했거나 상호작용 폴백이 필요할 때 보이는 표면.
    if (
        state.isGenerating &&
        automationProvider != null &&
        (showsExternalAIBrowser || state.isFallbackBrowserVisible)
    ) {
        ExternalAISurface(
            provider = automationProvider,
            mode = ExternalAISurfaceMode.GENERATION,
            fallbackReason = state.fallbackReason,
            prompt = viewModel.externalPrompt(),
            attachments = state.activeAttachments,
            onClose = {
                if (showsExternalAIBrowser) {
                    if (viewModel.state.value.isGenerating) {
                        viewModel.cancelGeneration()
                    }
                } else {
                    viewModel.dismissFallbackBrowser()
                }
            },
            onSubmitted = viewModel::onAutomationSubmitted,
            onError = { err ->
                viewModel.onAutomationError(err, automationProvider)
            },
            autoImportOnComplete = true,
            onImport = { text ->
                viewModel.importAIResult(text, automationProvider, context)
            },
            appearance = appearance
        )
    }
}

@Composable
private fun GenerationStatusCard(
    viewModel: ComposerViewModel,
    state: ComposerUiState,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(14.dp)
    val bg = if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper
    val border = BorderStroke(1.dp, if (isBk) Color(0xFFE2E6EC) else BrandTheme.border)
    val infiniteTransition = rememberInfiniteTransition(label = "generation_status_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "generation_status_angle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(border, shape)
            .background(bg, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("composer.statusCard"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "AI가 글을 만드는 중",
            tint = BrandTheme.accent,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation }
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                state.automationStepTitle ?: "글을 만드는 중…",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTheme.labelPrimary(appearance),
                maxLines = 1
            )
            Text(
                state.automationStepSubtitle ?: "잠시만 기다려 주세요",
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary(appearance),
                maxLines = 1
            )
            if (state.automationPhase == ExternalAIAutomationPhase.SUBMITTED ||
                state.automationPhase == ExternalAIAutomationPhase.WAITING_ELAPSED
            ) {
                LinearProgressIndicator(
                    progress = { ExternalAITimerFormatter.progress(state.automationElapsedSeconds) },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandTheme.accent
                )
            }
        }

        TextButton(
            onClick = { viewModel.cancelGeneration() },
            modifier = Modifier.testTag("composer.ai.cancel")
        ) {
            Text("취소", fontSize = 14.sp, color = BrandTheme.labelSecondary(appearance))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun HiddenExternalAIWebView(
    provider: DirectAIProvider,
    prompt: String,
    attachments: List<ExternalAIAttachment> = emptyList(),
    requestId: Int,
    onSubmitted: () -> Unit,
    onFallbackRequired: (ExternalAIFallbackReason) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val diagnostics = remember { ExternalAIDiagnosticsStore(context) }
    val diagnosticRun = remember(requestId) { diagnostics.start(provider.name) }
    var runFinished by remember(requestId) { mutableStateOf(false) }
    var hasDispatchedPrompt by remember(requestId) { mutableStateOf(false) }
    var webViewRef by remember(requestId) { mutableStateOf<WebView?>(null) }
    var isAutomationActive by remember(requestId) { mutableStateOf(true) }
    var nativeAttachmentBatch by remember(requestId) { mutableStateOf(ExternalAINativeAttachmentBatch.EMPTY) }
    var nativeAttachmentPreparationFailed by remember(requestId) { mutableStateOf(false) }
    val timings = ExternalAITimingProfile.DEFAULT

    LaunchedEffect(requestId, attachments) {
        nativeAttachmentPreparationFailed = false
        diagnostics.record(diagnosticRun, "media_preparation_started", mapOf("expected_count" to attachments.size))
        runCatching {
            withContext(Dispatchers.IO) {
                ExternalAINativeAttachmentBatch.prepare(context, attachments)
            }
        }.onSuccess {
            nativeAttachmentBatch = it
            diagnostics.record(diagnosticRun, "media_prepared", mapOf("prepared_count" to it.uris.size))
        }.onFailure {
            nativeAttachmentPreparationFailed = true
            diagnostics.record(diagnosticRun, "media_preparation_failed")
        }
    }
    val batchForDisposal = nativeAttachmentBatch
    DisposableEffect(batchForDisposal) {
        onDispose { batchForDisposal.dispose() }
    }

    DisposableEffect(requestId, provider) {
        val hostView = context.findActivity()?.window?.decorView as? ViewGroup
        val density = context.resources.displayMetrics.density
        val wv = WebView(context).apply {
            alpha = 0.001f
            // It is behind the opaque Compose surface and cannot intercept the user's touch,
            // but trusted programmatic taps must still reach hydrated provider controls.
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = userAgentString.replace("; wv", "")
            }
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val targetUrl = request?.url?.toString()
                    if (!ExternalAISecurityPolicy.isAllowedUrl(targetUrl, provider)) {
                        isAutomationActive = false
                        onFallbackRequired(ExternalAIFallbackReason.NAVIGATION_DISALLOWED)
                        return true
                    }
                    val fallback = ExternalAIFallbackClassifier.classifyUrl(targetUrl)
                    if (fallback != null) {
                        isAutomationActive = false
                        onFallbackRequired(fallback)
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    cookieManager.flush()

                    val fallback = ExternalAIFallbackClassifier.classifyUrl(url)
                    if (fallback != null) {
                        isAutomationActive = false
                        onFallbackRequired(fallback)
                        return
                    }

                    if (ExternalAISecurityPolicy.isAuthOrigin(url, provider)) {
                        isAutomationActive = false
                        onFallbackRequired(ExternalAIFallbackReason.LOGIN_REQUIRED)
                        return
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString() ?: "연결 실패"
                        val sanitized = ExternalAIErrorSanitizer.sanitize("Network error: $desc", provider)
                        runFinished = true
                        isAutomationActive = false
                        diagnostics.record(diagnosticRun, "browser_load_failed")
                        onError(sanitized)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    if (!isAutomationActive || hasDispatchedPrompt) {
                        filePathCallback.onReceiveValue(null)
                        return true
                    }
                    return nativeAttachmentBatch.handleFileChooser(filePathCallback, fileChooserParams)
                }
            }
        }

        val referenceWidth = (412 * density).toInt()
        val referenceHeight = (892 * density).toInt()
        val availableWidth = hostView?.width?.takeIf { it > 0 }
            ?: context.resources.displayMetrics.widthPixels
        val availableHeight = hostView?.height?.takeIf { it > 0 }
            ?: context.resources.displayMetrics.heightPixels
        val lp = FrameLayout.LayoutParams(
            minOf(referenceWidth, availableWidth),
            minOf(referenceHeight, availableHeight)
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(0, 0, 0, 0)
        }

        // Keep the browser in the real on-screen layout so provider portals fully hydrate and
        // native file gestures work, but place it behind the opaque app surface.
        hostView?.addView(wv, 0, lp)
        webViewRef = wv
        wv.loadUrl(provider.url)

        onDispose {
            if (!runFinished) diagnostics.record(diagnosticRun, "run_cancelled")
            wv.evaluateJavascript(ExternalAIScripts.cancelTaskScript(), null)
            isAutomationActive = false
            wv.stopLoading()
            hostView?.removeView(wv)
            wv.destroy()
            webViewRef = null
        }
    }

    LaunchedEffect(requestId, provider, nativeAttachmentBatch.uris.size, nativeAttachmentPreparationFailed) {
        if (nativeAttachmentPreparationFailed) {
            isAutomationActive = false
            onFallbackRequired(ExternalAIFallbackReason.ATTACHMENT_FAILED)
            return@LaunchedEffect
        }
        if (attachments.isNotEmpty() && nativeAttachmentBatch.uris.size != attachments.size) return@LaunchedEffect
        nativeAttachmentBatch.resetDelivery()
        val wv = webViewRef ?: return@LaunchedEffect
        val startTime = System.currentTimeMillis()
        var baselineCaptured = false
        var baselineCount = 0
        var attachmentHandled = attachments.isEmpty()
        var attachmentAttempted = attachments.isEmpty()
        var promptInjected = false
        var promptSubmitted = false
        var submissionStarted = 0L

        while (isAutomationActive && System.currentTimeMillis() - startTime < timings.visibleAutoFillTimeoutMs) {
            val url = wv.url
            if (ExternalAISecurityPolicy.canInjectScript(url, provider)) {
                if (!baselineCaptured) {
                    val baselineRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.recordBaselineScript(provider)) { if (cont.isActive) cont.resume(it) }
                    }
                    baselineCount = ExternalAIScripts.parseBaselineCount(baselineRes)
                    wv.evaluateForAIBI(ExternalAIScripts.beginTaskScript(attachments.size))
                    wv.evaluateForAIBI(ExternalAIScripts.installDiagnosticsScript())
                    diagnostics.record(diagnosticRun, "browser_loaded")
                    baselineCaptured = true
                }

                if (!attachmentHandled && !attachmentAttempted) {
                    attachmentAttempted = true
                    diagnostics.record(diagnosticRun, "attachment_started", mapOf("expected_count" to attachments.size))
                    attachmentHandled = attachOrderedPhotosToProvider(wv, provider, attachments, timings)
                    dismissHiddenAIBIKeyboard(context, wv)
                    if (!attachmentHandled) {
                        isAutomationActive = false
                        runFinished = true
                        diagnostics.record(diagnosticRun, "attachment_failed")
                        diagnostics.record(diagnosticRun, "manual_takeover")
                        onFallbackRequired(ExternalAIFallbackReason.ATTACHMENT_FAILED)
                        return@LaunchedEffect
                    }
                }

                if ((attachmentHandled || attachments.isEmpty()) && !promptInjected) {
                    val injectRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.injectPromptScript(provider, prompt, force = false)) { if (cont.isActive) cont.resume(it) }
                    }
                    dismissHiddenAIBIKeyboard(context, wv)
                    val injection = ExternalAIScripts.parseInjectionResult(injectRes)
                    if (injection.success && injection.inputFound) {
                        promptInjected = true
                        diagnostics.record(diagnosticRun, "prompt_inserted", mapOf("prompt_length" to prompt.length))
                    }
                }

                if (promptInjected && !promptSubmitted) {
                    if (submissionStarted == 0L) submissionStarted = android.os.SystemClock.elapsedRealtime()
                    if (android.os.SystemClock.elapsedRealtime() - submissionStarted >= 15_000L) break
                    delay(350L)
                    if (!hasDispatchedPrompt) {
                        hasDispatchedPrompt = wv.dispatchAIBISubmitOnce(provider, attachments.size) { hasDispatchedPrompt = true }
                        if (hasDispatchedPrompt) diagnostics.record(diagnosticRun, "send_attempted", mapOf("attempt" to 1))
                    }
                    dismissHiddenAIBIKeyboard(context, wv)
                    delay(700L)
                    val verifyRes = suspendCancellableCoroutine<String?> { cont ->
                        wv.evaluateJavascript(ExternalAIScripts.verifySubmissionScript(provider, baselineCount)) { if (cont.isActive) cont.resume(it) }
                    }
                    dismissHiddenAIBIKeyboard(context, wv)
                    wv.drainAIBIDiagnostics(provider, diagnostics, diagnosticRun)
                    if (ExternalAIScripts.parseSubmissionVerified(verifyRes)) {
                        promptSubmitted = true
                        diagnostics.record(diagnosticRun, "generation_started")
                        onSubmitted()
                        break
                    }
                }
            }
            delay(timings.observationCadenceMs)
        }

        if (!promptSubmitted && isAutomationActive) {
            isAutomationActive = false
            runFinished = true
            wv.evaluateJavascript(ExternalAIScripts.cancelTaskScript(), null)
            diagnostics.record(diagnosticRun, "send_timeout")
            diagnostics.record(diagnosticRun, "run_failed")
            onError("답변 시작을 확인하지 못했어요. 다시 시도하거나 설정에서 진단 로그를 공유해 주세요.")
            return@LaunchedEffect
        }

        // 응답 관찰 루프
        var stabilityState = ExternalAIStabilityState()
        val observationStarted = android.os.SystemClock.elapsedRealtime()
        while (isAutomationActive) {
            delay(timings.observationCadenceMs)
            if (android.os.SystemClock.elapsedRealtime() - observationStarted >= 119_000L) {
                runFinished = true
                isAutomationActive = false
                wv.evaluateJavascript(ExternalAIScripts.cancelTaskScript(), null)
                diagnostics.record(diagnosticRun, "generation_failed")
                onError("1분 59초 동안 답변이 없어서 중단했어요. 다시 시도해 주세요.")
                return@LaunchedEffect
            }
            wv.drainAIBIDiagnostics(provider, diagnostics, diagnosticRun)
            val errorRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(ExternalAIScripts.extractErrorScript()) { if (cont.isActive) cont.resume(it) }
            }
            val domError = ExternalAIScripts.parseErrorResult(errorRes)
            if (domError.hasError && !domError.error.isNullOrBlank()) {
                val sanitized = ExternalAIErrorSanitizer.sanitize(domError.error, provider)
                runFinished = true
                isAutomationActive = false
                wv.evaluateJavascript(ExternalAIScripts.cancelTaskScript(), null)
                diagnostics.record(diagnosticRun, "generation_failed")
                onError(sanitized)
                return@LaunchedEffect
            }

            val answerRes = suspendCancellableCoroutine<String?> { cont ->
                wv.evaluateJavascript(ExternalAIScripts.extractAnswerScript(provider)) { if (cont.isActive) cont.resume(it) }
            }
            val poll = ExternalAIScripts.parsePollResult(answerRes)
            val nextStability = ExternalAIStabilityReducer.step(stabilityState, poll)
            stabilityState = nextStability

            if (nextStability.isStable && nextStability.stableAnswer != null) {
                runFinished = true
                diagnostics.record(diagnosticRun, "result_applied", mapOf("response_length" to nextStability.stableAnswer.length))
                diagnostics.record(diagnosticRun, "run_completed")
                onSuccess(nextStability.stableAnswer)
                return@LaunchedEffect
            }
        }
    }
}

internal fun dismissHiddenAIBIKeyboard(context: Context, webView: WebView) {
    fun dismissNow() {
        if (!webView.isAttachedToWindow) return
        webView.evaluateJavascript(
            "if(document.activeElement&&document.activeElement.blur){document.activeElement.blur();}",
            null
        )
        webView.clearFocus()
        val activity = context.findActivity()
        activity?.currentFocus?.clearFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(webView.windowToken, 0)
        activity?.window?.decorView?.windowToken?.let { token ->
            inputMethodManager?.hideSoftInputFromWindow(token, 0)
        }
    }

    dismissNow()
    // WebView can enqueue the IME show request just after evaluateJavascript returns. Closing it
    // once more on the next input-method frame prevents a brief or persistent keyboard takeover.
    webView.postDelayed({ dismissNow() }, 120L)
}

// MARK: - 미디어 순서 편집기

@Composable
private fun MediaOrderEditor(viewModel: ComposerViewModel, state: ComposerUiState) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val itemSlotPx = with(density) { (104 + 10).dp.toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.mediaItems.forEachIndexed { index, media ->
                Box(
                    modifier = Modifier
                        .then(
                            if (draggingId == media.id) {
                                Modifier
                                    .graphicsLayer { translationX = dragOffset }
                                    .shadow(8.dp, RoundedCornerShape(14.dp))
                            } else Modifier
                        )
                        .pointerInput(media.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = media.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                    var currentIndex = viewModel.state.value.mediaItems
                                        .indexOfFirst { it.id == media.id }
                                    if (currentIndex >= 0) {
                                        while (dragOffset > itemSlotPx * 0.6f &&
                                            currentIndex + 1 < viewModel.state.value.mediaItems.size
                                        ) {
                                            viewModel.moveMedia(currentIndex, 1)
                                            currentIndex += 1
                                            dragOffset -= itemSlotPx
                                        }
                                        while (dragOffset < -itemSlotPx * 0.6f && currentIndex > 0) {
                                            viewModel.moveMedia(currentIndex, -1)
                                            currentIndex -= 1
                                            dragOffset += itemSlotPx
                                        }
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffset = 0f },
                                onDragCancel = { draggingId = null; dragOffset = 0f }
                            )
                        }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("앞으로 이동") {
                                    viewModel.moveMedia(index, -1)
                                    true
                                },
                                CustomAccessibilityAction("뒤로 이동") {
                                    viewModel.moveMedia(index, 1)
                                    true
                                }
                            )
                        }
                        .testTag("composer.media.$index")
                ) {
                    MediaThumbnail(media)

                    // 삭제 버튼 (우상단)
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
                            .clickable { viewModel.removeMedia(media.id) },
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "${index + 1}번째 미디어 삭제",
                            tint = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                                .background(Color.White, CircleShape)
                        )
                    }

                    if (index == 0) {
                        Text(
                            "대표",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(BrandTheme.accent, RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun MediaThumbnail(media: ComposerMedia) {
    val context = LocalContext.current
    val appearance = LocalAppAppearance.current
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(14.dp)
    val imageBitmap = rememberMediaBitmap(media, maximumLongEdgePixels = 320, lowMemory = true, context = context)
    Box(
        Modifier
            .size(width = 104.dp, height = 118.dp)
            .background(if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper, shape)
            .border(1.dp, if (isBk) Color(0xFFE2E6EC) else Color.Black.copy(alpha = 0.09f), shape)
            .clip(shape)
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = media.kind.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), color = BrandTheme.accent, strokeWidth = 2.dp)
            }
        }
        Icon(
            if (media.kind == MediaKind.IMAGE) Icons.Filled.Photo else Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp)
                .background(Color.Black.copy(alpha = 0.66f), CircleShape)
                .padding(6.dp)
                .size(12.dp)
        )
    }
}

private fun makeVideoThumbnail(media: ComposerMedia, cacheDir: File): android.graphics.Bitmap? {
    val ext = media.fileExtension?.takeIf { it.isNotEmpty() } ?: "mp4"
    val file = File(cacheDir, "imanagerai-thumb-${media.id}.$ext")
    return try {
        file.writeBytes(media.data)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.path)
        val frame = retriever.getFrameAtTime(0)
        retriever.release()
        if (frame == null || maxOf(frame.width, frame.height) <= 320) {
            frame
        } else {
            val ratio = 320f / maxOf(frame.width, frame.height).toFloat()
            val smaller = android.graphics.Bitmap.createScaledBitmap(
                frame,
                maxOf(1, (frame.width * ratio).toInt()),
                maxOf(1, (frame.height * ratio).toInt()),
                true
            )
            if (smaller !== frame) frame.recycle()
            smaller
        }
    } catch (_: Exception) {
        null
    } finally {
        file.delete()
    }
}

private fun saveImageToGallery(context: Context, bytes: ByteArray): Boolean {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    val resolver = context.contentResolver
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = runCatching {
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }.getOrNull() ?: return false

    return try {
        val written = resolver.openOutputStream(uri)?.use { stream ->
            stream.write(bytes)
            stream.flush()
            true
        } ?: false
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            val updated = resolver.update(uri, values, null, null)
            if (updated == 0) {
                runCatching { resolver.delete(uri, null, null) }
                return false
            }
        }
        true
    } catch (_: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        false
    }
}

@Composable
private fun MediaPreview(items: List<ComposerMedia>, aspect: Float, isLoading: Boolean) {
    val appearance = LocalAppAppearance.current
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .heightIn(max = 420.dp)
            .background(if (isBk) Color(0xFFF1F3F6) else BrandTheme.paper, shape)
            .border(1.dp, if (isBk) Color(0xFFE2E6EC) else Color(0x2E3C3C43), shape)
            .clip(shape)
            .testTag("preview.media"),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), color = BrandTheme.accent, strokeWidth = 2.dp)
                Text("미디어 불러오는 중", fontSize = 13.sp, color = BrandTheme.labelSecondary(appearance))
            }
            items.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { items.size })
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val media = items[page]
                    Box(Modifier.fillMaxSize()) {
                        if (media.kind == MediaKind.IMAGE) {
                            val bitmap = rememberMediaBitmap(
                                media = media,
                                maximumLongEdgePixels = 1_400,
                                lowMemory = false,
                                context = LocalContext.current
                            )
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "사진",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.SmartDisplay,
                                    contentDescription = null,
                                    tint = BrandTheme.accent,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("영상", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = BrandTheme.accent)
                            }
                        }
                        Text(
                            "${page + 1}/${items.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = BrandTheme.labelSecondary(appearance),
                    modifier = Modifier.size(34.dp)
                )
                Text("불러오는 중", fontSize = 12.sp, color = BrandTheme.labelSecondary(appearance))
            }
        }
    }
}

@Composable
private fun rememberMediaBitmap(
    media: ComposerMedia,
    maximumLongEdgePixels: Int,
    lowMemory: Boolean,
    context: Context
): android.graphics.Bitmap? {
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        media.id,
        maximumLongEdgePixels,
        lowMemory
    ) {
        value = withContext(Dispatchers.IO) {
            when (media.kind) {
                MediaKind.IMAGE -> runCatching {
                    ComposerImagePipeline.decodeForDisplay(
                        source = media.data,
                        maximumLongEdgePixels = maximumLongEdgePixels,
                        lowMemory = lowMemory
                    )
                }.getOrNull()
                MediaKind.VIDEO -> makeVideoThumbnail(media, context.cacheDir)
            }
        }
    }
    // Compose may retain the painter for an additional display-list frame after disposal.
    // The decoded bitmap is already bounded, so let GC own its lifetime instead of recycling it.
    return bitmap
}

private fun sourceIcon(source: CaptionSource): ImageVector = when (source) {
    CaptionSource.DEVICE -> Icons.Filled.Smartphone
    CaptionSource.DETERMINISTIC -> Icons.Filled.Smartphone
    CaptionSource.GEMINI -> Icons.Outlined.Diamond
    CaptionSource.CHAT_GPT -> Icons.Filled.Forum
    CaptionSource.CLAUDE -> Icons.Filled.AutoAwesome
    CaptionSource.GROK -> Icons.Filled.Close
}

@Composable
private fun AIChoiceCard(
    choice: AIChoice,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: AppAppearance = LocalAppAppearance.current
) {
    val isBk = appearance == AppAppearance.BK
    val shape = RoundedCornerShape(13.dp)
    val bg = if (isBk) {
        if (enabled) Color.White else Color(0xFFF6F7F9)
    } else {
        if (enabled) BrandTheme.paper else BrandTheme.canvas
    }
    val border = BorderStroke(
        1.dp,
        if (isBk) {
            if (enabled) Color(0xFFD8DCE3) else Color(0xFFE5E8EE)
        } else {
            BrandTheme.border
        }
    )

    Column(
        modifier = modifier
            .heightIn(min = 62.dp)
            .border(border, shape)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AIChoiceMark(choice)
        Text(
            choice.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) BrandTheme.labelPrimary(appearance) else BrandTheme.labelSecondary(appearance)
        )
    }
}

@Composable
private fun AIChoiceMark(choice: AIChoice) {
    when (choice) {
        AIChoice.OnDevice -> Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = choice.title,
            tint = Color(0xFF3DDC84),
            modifier = Modifier.size(24.dp)
        )
        is AIChoice.External -> {
            val brandResource = when (choice.provider) {
                DirectAIProvider.GEMINI -> R.drawable.brand_gemini
                DirectAIProvider.OPEN_AI -> R.drawable.brand_chatgpt
                DirectAIProvider.CLAUDE -> R.drawable.brand_claude
                DirectAIProvider.GROK -> R.drawable.brand_grok
            }
            Image(
                painter = painterResource(brandResource),
                contentDescription = choice.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
        }
    }
}
