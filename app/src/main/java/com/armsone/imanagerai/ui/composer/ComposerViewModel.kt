package com.armsone.imanagerai.ui.composer

import android.content.ContentResolver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armsone.imanagerai.FixtureHooks
import com.armsone.imanagerai.model.AIProviderIdentity
import com.armsone.imanagerai.model.CaptionValidationContext
import com.armsone.imanagerai.model.CaptionValidationReport
import com.armsone.imanagerai.model.CreatorProfileStore
import com.armsone.imanagerai.model.GeneratedPost
import com.armsone.imanagerai.model.GenerationStylePreset
import com.armsone.imanagerai.model.PostLength
import com.armsone.imanagerai.model.PostMood
import com.armsone.imanagerai.service.DeterministicCaptionGenerator
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.service.ExternalPromptBuilder
import com.armsone.imanagerai.ui.automation.AutomationProviderSelector
import com.armsone.imanagerai.ui.automation.AutomationSessionState
import com.armsone.imanagerai.ui.externalai.ExternalAIAnswerCleaner
import com.armsone.imanagerai.ui.externalai.ExternalAIAttachment
import com.armsone.imanagerai.ui.externalai.ExternalAIAutomationPhase
import com.armsone.imanagerai.ui.externalai.ExternalAIErrorSanitizer
import com.armsone.imanagerai.ui.externalai.ExternalAIFallbackReason
import com.armsone.imanagerai.ui.externalai.ExternalAIImageNormalizer
import com.armsone.imanagerai.ui.externalai.ExternalAITimerFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

data class ComposerUiState(
    val idea: String = "",
    val generatedPost: GeneratedPost? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val previewAspect: PreviewAspect = PreviewAspect.FEED,
    val mediaItems: List<ComposerMedia> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val isPreparingShare: Boolean = false,
    val shareMessage: String? = null,
    val shareMessageIsError: Boolean = false,
    val generatedSignature: DraftSignature? = null,
    val activeCaptionSource: CaptionSource? = null,
    val captionCandidates: Map<CaptionSource, CaptionCandidate> = emptyMap(),
    val pendingExternalProvider: DirectAIProvider? = null,
    val activeAutomationProvider: DirectAIProvider? = null,
    val activeAttachments: List<ExternalAIAttachment> = emptyList(),
    val automationPhase: ExternalAIAutomationPhase = ExternalAIAutomationPhase.IDLE,
    val automationElapsedSeconds: Long = 0L,
    val automationStepTitle: String? = null,
    val automationStepSubtitle: String? = null,
    val fallbackReason: ExternalAIFallbackReason? = null,
    val isFallbackBrowserVisible: Boolean = false,
    val lastImportSuccessToken: Long = 0L,
    val automationRequestId: Int = 0
)

class ComposerViewModel : ViewModel() {

    lateinit var profileStore: CreatorProfileStore

    private val _state = MutableStateFlow(ComposerUiState())
    val state: StateFlow<ComposerUiState> = _state.asStateFlow()

    private val _automationSessionState = MutableStateFlow<AutomationSessionState>(AutomationSessionState.Idle)
    val automationSessionState: StateFlow<AutomationSessionState> = _automationSessionState.asStateFlow()

    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var mediaLoadJob: Job? = null
    private var automationTimerJob: Job? = null
    private var automationRequestIdCounter = 0
    private var resetVersion = 0
    private var isOrdinaryAutomationSession = false
    private var externalGenerationStartTime = 0L

    private fun update(transform: (ComposerUiState) -> ComposerUiState) {
        _state.value = transform(_state.value)
    }

    // MARK: - 입력

    fun setIdea(value: String) = update { it.copy(idea = value) }
    fun setMood(value: PostMood) = profileStore.updateProfile { it.copy(mood = value) }
    fun setLength(value: PostLength) = profileStore.updateProfile { it.copy(preferredLength = value) }
    fun setPreviewAspect(value: PreviewAspect) = update { it.copy(previewAspect = value) }

    val trimmedIdea: String get() = _state.value.idea.trim()

    val hasRepresentativePhoto: Boolean
        get() = _state.value.mediaItems.any { it.kind == MediaKind.IMAGE }

    fun makeSelectedPhotoAttachments(): List<ExternalAIAttachment> =
        ExternalAIImageNormalizer.normalizeOrdered(
            _state.value.mediaItems.filter { it.kind == MediaKind.IMAGE }.map { it.data }
        )

    fun hasMediaOrText(): Boolean = trimmedIdea.isNotEmpty() || _state.value.mediaItems.isNotEmpty()

    fun hasContent(): Boolean = _state.value.run {
        idea.isNotEmpty() || generatedPost != null || mediaItems.isNotEmpty() || captionCandidates.isNotEmpty() || pendingExternalProvider != null || isGenerating || _automationSessionState.value !is AutomationSessionState.Idle
    }

    fun resetComposer() {
        resetVersion += 1
        generationJob?.cancel()
        timerJob?.cancel()
        mediaLoadJob?.cancel()
        automationTimerJob?.cancel()
        _automationSessionState.value = AutomationSessionState.Idle
        isOrdinaryAutomationSession = false
        update { state ->
            state.copy(
                idea = "",
                generatedPost = null,
                isGenerating = false,
                errorMessage = null,
                statusMessage = null,
                mediaItems = emptyList(),
                isLoadingMedia = false,
                isPreparingShare = false,
                shareMessage = null,
                shareMessageIsError = false,
                generatedSignature = null,
                activeCaptionSource = null,
                captionCandidates = emptyMap(),
                pendingExternalProvider = null,
                activeAutomationProvider = null,
                activeAttachments = emptyList(),
                automationPhase = ExternalAIAutomationPhase.IDLE,
                automationElapsedSeconds = 0L,
                automationStepTitle = null,
                automationStepSubtitle = null,
                fallbackReason = null,
                isFallbackBrowserVisible = false,
                lastImportSuccessToken = 0L,
                automationRequestId = state.automationRequestId + 1
            )
        }
    }

    // MARK: - Automation Studio 세션 관리

    /** 앱을 열거나 15초 뒤 돌아왔을 때 자동으로 시작되는 일반 자동화. 설정에서 끄면 즉시 취소된다. */
    fun startOrdinaryPhotoAutomation(rawImages: List<ByteArray>) {
        isOrdinaryAutomationSession = true
        startPhotoAutomationInternal(rawImages, provider = null)
    }

    fun cancelOrdinaryAutomationIfActive() {
        if (isOrdinaryAutomationSession && _automationSessionState.value !is AutomationSessionState.Idle) {
            cancelAutomationSession()
        }
    }

    /** 외부 공유 인텐트나 결과 화면의 재시도에서 호출된다. 최초 호출이면 일반 자동화 표시를 지운다. */
    fun startPhotoAutomation(rawImages: List<ByteArray>, provider: AIProviderIdentity? = null) {
        if (provider == null) isOrdinaryAutomationSession = false
        startPhotoAutomationInternal(rawImages, provider)
    }

    private fun startPhotoAutomationInternal(rawImages: List<ByteArray>, provider: AIProviderIdentity?) {
        if (rawImages.isEmpty()) return
        val candidates = if (::profileStore.isInitialized) {
            profileStore.availabilityStore.enabledCandidates
        } else {
            AutomationProviderSelector.ELIGIBLE_IDENTITIES
        }
        val targetProvider = provider ?: AutomationProviderSelector.selectRandom(candidates = candidates)
        val count = rawImages.size.coerceIn(1, 8)
        val images = rawImages.take(count)
        automationTimerJob?.cancel()
        automationRequestIdCounter += 1
        val reqId = automationRequestIdCounter

        _automationSessionState.value = AutomationSessionState.Processing(
            provider = targetProvider,
            stepTitle = "사진을 준비하는 중…",
            stepSubtitle = "선택한 사진 ${images.size}장을 전송용으로 줄이고 있어요",
            elapsedSeconds = 0L,
            attachments = emptyList(),
            rawImages = images,
            requestId = reqId
        )

        viewModelScope.launch {
            try {
                val attachments = withContext(Dispatchers.Default) {
                    ExternalAIImageNormalizer.normalizeOrdered(images)
                }
                val current = _automationSessionState.value
                if (current is AutomationSessionState.Processing && current.requestId == reqId) {
                    _automationSessionState.value = current.copy(
                        attachments = attachments,
                        stepTitle = "${targetProvider.title}에 연결하는 중…",
                        stepSubtitle = "사진 ${attachments.size}장과 입력창을 준비하고 있어요"
                    )

                    // 온디바이스 AI인 경우 즉시 로컬 생성 파이프라인 실행
                    if (targetProvider == AIProviderIdentity.DEVICE) {
                        val genStart = System.currentTimeMillis()
                        val profile = profileStore.profile.value
                        val generator = DeterministicCaptionGenerator(
                            simulatedDelayMillis = FixtureHooks.captionDelayMillis ?: 550L
                        )
                        val post = generator.generate(
                            idea = "",
                            mood = profile.mood,
                            length = profile.preferredLength,
                            profile = profile
                        )
                        val elapsed = (System.currentTimeMillis() - genStart) / 1000.0
                        if (::profileStore.isInitialized) {
                            profileStore.runMetricsStore.recordRun(AIProviderIdentity.DEVICE, elapsed)
                            profileStore.availabilityStore.recordUsed(AIProviderIdentity.DEVICE)
                        }
                        onAutomationStudioSuccess(post.composedText, context = null)
                    }
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (_: Exception) {
                val current = _automationSessionState.value
                if (current is AutomationSessionState.Processing && current.requestId == reqId) {
                    _automationSessionState.value = AutomationSessionState.Failure(
                        lastProvider = targetProvider,
                        errorMessage = "선택한 사진을 전송용으로 준비하지 못했어요. 사진을 확인하고 다시 시도해 주세요.",
                        attachments = emptyList(),
                        rawImages = images
                    )
                }
            }
        }
    }

    fun onAutomationStudioSubmitted() {
        val current = _automationSessionState.value
        if (current !is AutomationSessionState.Processing) return
        val reqId = current.requestId
        val targetProvider = current.provider

        _automationSessionState.value = current.copy(
            stepTitle = "정보를 보냈어요",
            stepSubtitle = ExternalAITimerFormatter.formatWaitingStatus(0L),
            elapsedSeconds = 0L
        )

        automationTimerJob?.cancel()
        automationTimerJob = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(1000L)
                elapsed += 1L
                val stateNow = _automationSessionState.value
                if (stateNow !is AutomationSessionState.Processing || stateNow.requestId != reqId) {
                    break
                }
                if (elapsed >= ExternalAITimerFormatter.GENERATION_TIMEOUT_SECONDS) {
                    _automationSessionState.value = AutomationSessionState.Failure(
                        lastProvider = targetProvider,
                        errorMessage = "1분 59초 동안 답변이 없어서 중단했어요. 다시 시도해 주세요.",
                        attachments = stateNow.attachments,
                        rawImages = stateNow.rawImages
                    )
                    break
                }
                _automationSessionState.value = stateNow.copy(
                    stepTitle = "답변을 기다리는 중",
                    stepSubtitle = ExternalAITimerFormatter.formatWaitingStatus(elapsed),
                    elapsedSeconds = elapsed
                )
            }
        }
    }

    fun onAutomationStudioSuccess(answer: String, context: Context? = null) {
        automationTimerJob?.cancel()
        val current = _automationSessionState.value
        if (current !is AutomationSessionState.Processing) return
        val extProvider = current.provider.externalProvider
        val cleaned = if (extProvider != null) {
            ExternalAIAnswerCleaner.clean(answer, extProvider)
        } else {
            answer.trim()
        }
        if (cleaned.isBlank()) {
            _automationSessionState.value = AutomationSessionState.Failure(
                lastProvider = current.provider,
                errorMessage = "생성된 문구가 비어 있어요. 다시 시도해 주세요.",
                attachments = current.attachments,
                rawImages = current.rawImages
            )
            return
        }
        if (context != null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Stargram", cleaned))
        }
        if (::profileStore.isInitialized) {
            profileStore.runMetricsStore.recordRun(current.provider, current.elapsedSeconds.toDouble())
            profileStore.availabilityStore.recordUsed(current.provider)
        }
        _automationSessionState.value = AutomationSessionState.Result(
            provider = current.provider,
            generatedText = cleaned,
            attachments = current.attachments,
            rawImages = current.rawImages
        )
    }

    fun onAutomationStudioError(rawError: String?, provider: AIProviderIdentity? = null) {
        automationTimerJob?.cancel()
        val current = _automationSessionState.value
        if (current !is AutomationSessionState.Processing) return
        val targetProvider = provider ?: current.provider
        val sanitized = targetProvider.externalProvider?.let {
            ExternalAIErrorSanitizer.sanitize(rawError, it)
        } ?: (rawError ?: "알 수 없는 오류가 발생했어요.")
        _automationSessionState.value = AutomationSessionState.Failure(
            lastProvider = targetProvider,
            errorMessage = sanitized,
            attachments = current.attachments,
            rawImages = current.rawImages
        )
    }

    fun cancelAutomationSession() {
        automationTimerJob?.cancel()
        _automationSessionState.value = AutomationSessionState.Idle
        isOrdinaryAutomationSession = false
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        timerJob?.cancel()
        update { state ->
            state.copy(
                isGenerating = false,
                activeAutomationProvider = null,
                activeAttachments = emptyList(),
                automationPhase = ExternalAIAutomationPhase.IDLE,
                automationElapsedSeconds = 0L,
                automationStepTitle = null,
                automationStepSubtitle = null,
                fallbackReason = null,
                isFallbackBrowserVisible = false,
                pendingExternalProvider = null,
                statusMessage = "AI 요청을 취소했어요",
                automationRequestId = state.automationRequestId + 1
            )
        }
    }

    fun applyGenerationStyle(preset: GenerationStylePreset) {
        profileStore.applyGenerationStyle(preset)
        update {
            it.copy(
                statusMessage = "${preset.title} 적용",
                errorMessage = null
            )
        }
    }

    fun setCharacterCount(value: Int) {
        profileStore.updateProfile { profile ->
            profile.withControls(profile.controls.copy(characterCount = value))
        }
    }

    // MARK: - 초안 상태

    fun currentDraftSignature(): DraftSignature {
        val profile = profileStore.profile.value
        return DraftSignature(
            idea = trimmedIdea,
            mood = profile.mood,
            length = profile.preferredLength,
            profile = profile
        )
    }

    fun draftIsCurrent(): Boolean =
        _state.value.generatedSignature == currentDraftSignature()

    fun comparisonCandidates(): List<CaptionCandidate> {
        val signature = _state.value.generatedSignature ?: return emptyList()
        return CaptionSource.entries.mapNotNull { source ->
            _state.value.captionCandidates[source]?.takeIf { it.signature == signature }
        }
    }

    fun validationContext(signature: DraftSignature): CaptionValidationContext =
        CaptionValidationContext(
            destinationLimit = ExternalPromptBuilder.NEUTRAL_SAFETY_LIMIT,
            prohibitedPhrases = "",
            emojiIntensity = signature.profile.emojiIntensity
        )

    fun validationReport(candidate: CaptionCandidate): CaptionValidationReport =
        CaptionValidationReport.evaluate(
            candidate.post.composedText,
            validationContext(candidate.signature)
        )

    fun activeValidationReport(): CaptionValidationReport? {
        val textToValidate = _state.value.idea.ifBlank { _state.value.generatedPost?.composedText } ?: return null
        val profile = profileStore.profile.value
        return CaptionValidationReport.evaluate(
            textToValidate,
            CaptionValidationContext(
                destinationLimit = ExternalPromptBuilder.NEUTRAL_SAFETY_LIMIT,
                prohibitedPhrases = "",
                emojiIntensity = profile.emojiIntensity
            )
        )
    }

    // MARK: - 생성

    fun generateDraft() {
        if (_state.value.isGenerating) return
        val hasPhoto = hasRepresentativePhoto
        if (trimmedIdea.isEmpty() && !hasPhoto) {
            update { it.copy(errorMessage = "이야기나 사진을 입력해 주세요.") }
            return
        }
        timerJob?.cancel()
        generationJob?.cancel()
        val startTime = System.currentTimeMillis()
        update {
            it.copy(
                isGenerating = true,
                activeAutomationProvider = null,
                activeAttachments = emptyList(),
                automationPhase = ExternalAIAutomationPhase.CONNECTING,
                automationElapsedSeconds = 0L,
                automationStepTitle = "기기 AI로 만드는 중…",
                automationStepSubtitle = "잠시만 기다려 주세요",
                fallbackReason = null,
                isFallbackBrowserVisible = false,
                errorMessage = null,
                statusMessage = null,
                shareMessage = null,
                shareMessageIsError = false,
                activeCaptionSource = null
            )
        }
        generationJob = viewModelScope.launch {
            try {
                val profile = profileStore.profile.value
                val signature = currentDraftSignature()
                val generator = DeterministicCaptionGenerator(
                    simulatedDelayMillis = FixtureHooks.captionDelayMillis ?: 550L
                )
                val post = generator.generate(signature.idea, signature.mood, signature.length, profile)
                val candidate = CaptionCandidate(
                    source = CaptionSource.DETERMINISTIC,
                    post = post,
                    signature = signature
                )
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                if (::profileStore.isInitialized) {
                    profileStore.runMetricsStore.recordRun(AIProviderIdentity.DEVICE, elapsedSeconds)
                    profileStore.availabilityStore.recordUsed(AIProviderIdentity.DEVICE)
                }
                update {
                    it.copy(
                        idea = post.composedText,
                        captionCandidates = it.captionCandidates + (candidate.source to candidate),
                        generatedPost = post,
                        generatedSignature = signature,
                        activeCaptionSource = candidate.source,
                        statusMessage = if (validationReport(candidate).passesAllRules) "완료" else "확인 필요",
                        isGenerating = false,
                        automationPhase = ExternalAIAutomationPhase.COMPLETED,
                        lastImportSuccessToken = System.currentTimeMillis()
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                update {
                    it.copy(
                        errorMessage = error.localizedMessage ?: "게시물을 만들지 못했어요.",
                        isGenerating = false,
                        automationPhase = ExternalAIAutomationPhase.ERROR
                    )
                }
            }
        }
    }

    // MARK: - 외부 AI 백그라운드 자동화 / 공유 / 가져오기

    fun startExternalGeneration(provider: DirectAIProvider) = startExternalAIGeneration(provider)

    fun startExternalAIGeneration(provider: DirectAIProvider) {
        if (_state.value.isGenerating) return
        val hasPhoto = hasRepresentativePhoto
        if (trimmedIdea.isEmpty() && !hasPhoto) {
            update { it.copy(errorMessage = "이야기나 사진을 입력해 주세요.") }
            return
        }
        timerJob?.cancel()
        generationJob?.cancel()
        externalGenerationStartTime = System.currentTimeMillis()
        val imageCount = _state.value.mediaItems.count { it.kind == MediaKind.IMAGE }
        update { state ->
            state.copy(
                isGenerating = true,
                activeAutomationProvider = null,
                pendingExternalProvider = provider,
                activeAttachments = emptyList(),
                automationPhase = if (imageCount > 0) ExternalAIAutomationPhase.ATTACHING else ExternalAIAutomationPhase.CONNECTING,
                automationElapsedSeconds = 0L,
                automationStepTitle = if (imageCount > 0) "사진을 준비하는 중…" else "${provider.title}에 연결하는 중…",
                automationStepSubtitle = if (imageCount > 0) "선택한 사진 ${imageCount}장을 전송용으로 줄이고 있어요" else "입력창을 준비하고 있어요",
                fallbackReason = null,
                isFallbackBrowserVisible = false,
                errorMessage = null,
                statusMessage = "${provider.title}에 연결하는 중…",
                shareMessage = null,
                shareMessageIsError = false,
                activeCaptionSource = null,
                automationRequestId = state.automationRequestId + 1
            )
        }
        generationJob = viewModelScope.launch {
            try {
                val attachments = withContext(Dispatchers.Default) { makeSelectedPhotoAttachments() }
                update { state ->
                    if (!state.isGenerating || state.pendingExternalProvider != provider) state else state.copy(
                        activeAutomationProvider = provider,
                        activeAttachments = attachments,
                        automationPhase = ExternalAIAutomationPhase.CONNECTING,
                        automationStepTitle = "${provider.title}에 연결하는 중…",
                        automationStepSubtitle = if (attachments.isNotEmpty()) {
                            "사진 ${attachments.size}장과 입력창을 준비하고 있어요"
                        } else {
                            "입력창을 준비하고 있어요"
                        }
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (_: Exception) {
                update { state -> state.copy(
                    isGenerating = false,
                    activeAutomationProvider = null,
                    activeAttachments = emptyList(),
                    automationPhase = ExternalAIAutomationPhase.ERROR,
                    automationStepTitle = null,
                    automationStepSubtitle = null,
                    errorMessage = "선택한 사진을 전송용으로 준비하지 못했어요. 사진을 확인하고 다시 시도해 주세요."
                ) }
            }
        }
    }

    fun onAutomationSubmitted() {
        timerJob?.cancel()
        update { state ->
            state.copy(
                automationPhase = ExternalAIAutomationPhase.SUBMITTED,
                automationElapsedSeconds = 0L,
                automationStepTitle = "정보를 보냈어요",
                automationStepSubtitle = ExternalAITimerFormatter.formatWaitingStatus(0L)
            )
        }
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(1000L)
                elapsed += 1L
                if (elapsed >= ExternalAITimerFormatter.GENERATION_TIMEOUT_SECONDS) {
                    generationJob?.cancel()
                    update { s ->
                        s.copy(
                            isGenerating = false,
                            activeAutomationProvider = null,
                            activeAttachments = emptyList(),
                            automationPhase = ExternalAIAutomationPhase.ERROR,
                            automationElapsedSeconds = elapsed,
                            automationStepTitle = null,
                            automationStepSubtitle = null,
                            fallbackReason = null,
                            isFallbackBrowserVisible = false,
                            errorMessage = "1분 59초 동안 답변이 없어서 중단했어요. 다시 시도해 주세요.",
                            automationRequestId = s.automationRequestId + 1
                        )
                    }
                    break
                }
                update { s ->
                    if (!s.isGenerating || s.automationPhase == ExternalAIAutomationPhase.COMPLETED) {
                        s
                    } else {
                        s.copy(
                            automationPhase = ExternalAIAutomationPhase.WAITING_ELAPSED,
                            automationElapsedSeconds = elapsed,
                            automationStepTitle = "답변을 기다리는 중",
                            automationStepSubtitle = ExternalAITimerFormatter.formatWaitingStatus(elapsed)
                        )
                    }
                }
            }
        }
    }

    fun onAutomationFallbackRequired(reason: ExternalAIFallbackReason) {
        update {
            it.copy(
                isFallbackBrowserVisible = true,
                fallbackReason = reason,
                automationPhase = ExternalAIAutomationPhase.FALLBACK_REQUIRED
            )
        }
    }

    fun dismissFallbackBrowser() {
        update {
            it.copy(
                isFallbackBrowserVisible = false,
                fallbackReason = null
            )
        }
    }

    fun onAutomationError(rawError: String?, provider: DirectAIProvider? = null) {
        timerJob?.cancel()
        val targetProvider = provider ?: _state.value.activeAutomationProvider
        val sanitized = ExternalAIErrorSanitizer.sanitize(rawError, targetProvider)
        update {
            it.copy(
                isGenerating = false,
                activeAutomationProvider = null,
                activeAttachments = emptyList(),
                automationPhase = ExternalAIAutomationPhase.ERROR,
                errorMessage = sanitized,
                fallbackReason = null,
                isFallbackBrowserVisible = false
            )
        }
    }

    fun externalPrompt(): String {
        val profile = profileStore.profile.value
        val imageCount = _state.value.mediaItems.count { it.kind == MediaKind.IMAGE }
        val hasPhoto = imageCount > 0
        return when {
            hasPhoto && trimmedIdea.isNotEmpty() ->
                profile.photoAndTextPrompt(trimmedIdea, profile.mood, profile.preferredLength, imageCount)
            hasPhoto && trimmedIdea.isEmpty() ->
                profile.photoOnlyPrompt(profile.mood, profile.preferredLength, imageCount)
            else ->
                profile.generationPrompt(trimmedIdea, profile.mood, profile.preferredLength)
        }
    }

    /** 요청문을 클립보드에 복사하고 공유 인텐트를 돌려준다. */
    fun sharePrompt(provider: DirectAIProvider, context: Context): Intent? {
        val hasPhoto = hasRepresentativePhoto
        if (trimmedIdea.isEmpty() && !hasPhoto) {
            update { it.copy(errorMessage = "이야기나 사진을 입력해 주세요.") }
            return null
        }
        val prompt = externalPrompt()
        copyToClipboard(context, prompt)
        update {
            it.copy(
                errorMessage = null,
                pendingExternalProvider = provider,
                statusMessage = "공유 화면에서 ${provider.title} 선택",
                shareMessage = null
            )
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
        }
    }

    fun importAIResult(text: String, provider: DirectAIProvider, context: Context? = null) {
        val cleanedText = ExternalAIAnswerCleaner.clean(text, provider)
        if (cleanedText.isEmpty()) {
            update {
                it.copy(
                    isGenerating = false,
                    activeAutomationProvider = null,
                    activeAttachments = emptyList(),
                    automationPhase = ExternalAIAutomationPhase.ERROR,
                    errorMessage = "복사한 결과가 비어 있어요."
                )
            }
            return
        }
        if (context != null) {
            copyToClipboard(context, cleanedText)
        }
        timerJob?.cancel()
        val elapsedSeconds = if (externalGenerationStartTime > 0L) {
            (System.currentTimeMillis() - externalGenerationStartTime) / 1000.0
        } else {
            0.0
        }
        AIProviderIdentity.fromExternalProvider(provider)?.let { identity ->
            if (::profileStore.isInitialized) {
                profileStore.runMetricsStore.recordRun(identity, elapsedSeconds)
                profileStore.availabilityStore.recordUsed(identity)
            }
        }
        val signature = currentDraftSignature()
        val lines = cleanedText.split("\n")
        val hashtags = (lines.firstOrNull() ?: "")
            .split(" ")
            .filter { it.isNotEmpty() }
            .mapNotNull { token -> if (token.startsWith("#")) token.drop(1) else null }
        val post = GeneratedPost.create(
            sourceIdea = signature.idea,
            hook = lines.drop(1).firstOrNull() ?: "",
            caption = lines.drop(1).dropLast(1).joinToString("\n"),
            callToAction = lines.lastOrNull() ?: "",
            hashtags = hashtags,
            composedText = cleanedText,
            targetCharacterCount = null,
            destinationCharacterLimit = null
        )
        val candidate = CaptionCandidate(
            source = provider.captionSource,
            post = post,
            signature = signature
        )
        update {
            it.copy(
                idea = cleanedText,
                captionCandidates = it.captionCandidates + (candidate.source to candidate),
                generatedPost = post,
                generatedSignature = signature,
                activeCaptionSource = candidate.source,
                isGenerating = false,
                activeAutomationProvider = null,
                activeAttachments = emptyList(),
                automationPhase = ExternalAIAutomationPhase.COMPLETED,
                fallbackReason = null,
                isFallbackBrowserVisible = false,
                pendingExternalProvider = null,
                errorMessage = null,
                statusMessage = if (validationReport(candidate).passesAllRules) {
                    "${provider.title} 결과 가져옴"
                } else {
                    "가져옴 · 기준 확인 필요"
                },
                lastImportSuccessToken = System.currentTimeMillis()
            )
        }
    }

    fun useCandidate(candidate: CaptionCandidate) {
        update { state ->
            state.copy(
                idea = candidate.post.composedText,
                mediaItems = state.mediaItems.filter { media ->
                    media.generatedFromPostId == null || media.generatedFromPostId == candidate.post.id
                },
                generatedPost = candidate.post,
                generatedSignature = candidate.signature,
                activeCaptionSource = candidate.source
            )
        }
    }

    // MARK: - 미디어

    fun addPickedMedia(
        uris: List<Uri>,
        resolver: ContentResolver,
        onFinished: () -> Unit = {}
    ) {
        if (uris.isEmpty()) {
            onFinished()
            return
        }
        if (MediaAttachmentPolicy.availableSlots(_state.value.mediaItems.size) == 0) {
            update { it.copy(errorMessage = "미디어는 최대 ${MediaAttachmentPolicy.MAX_ITEMS}개까지 추가할 수 있어요.") }
            onFinished()
            return
        }
        update { it.copy(isLoadingMedia = true, errorMessage = null) }
        mediaLoadJob?.cancel()
        mediaLoadJob = viewModelScope.launch {
            try {
                val requestedSlots = MediaAttachmentPolicy.availableSlots(_state.value.mediaItems.size)
                val loaded = withContext(Dispatchers.IO) {
                    buildList {
                        for (uri in uris.take(requestedSlots)) {
                            val original = runCatching {
                                resolver.openInputStream(uri)?.use { it.readBytes() }
                            }.getOrNull() ?: continue
                            val mime = resolver.getType(uri)
                            val kind = if (mime?.startsWith("video") == true) MediaKind.VIDEO else MediaKind.IMAGE
                            val prepared = if (kind == MediaKind.IMAGE) {
                                runCatching { ComposerImagePipeline.prepareForComposer(original) }.getOrNull() ?: continue
                            } else {
                                original
                            }
                            val ext = if (kind == MediaKind.IMAGE) {
                                "jpg"
                            } else {
                                mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                            }
                            add(ComposerMedia(data = prepared, kind = kind, fileExtension = ext))
                        }
                    }
                }
                update { state ->
                    if (loaded.isEmpty()) {
                        state.copy(isLoadingMedia = false, errorMessage = "선택한 미디어를 불러오지 못했어요.")
                    } else {
                        val accepted = loaded.take(MediaAttachmentPolicy.availableSlots(state.mediaItems.size))
                        if (accepted.isEmpty()) {
                            state.copy(
                                isLoadingMedia = false,
                                errorMessage = "미디어는 최대 ${MediaAttachmentPolicy.MAX_ITEMS}개까지 추가할 수 있어요."
                            )
                        } else {
                            state.copy(
                                isLoadingMedia = false,
                                mediaItems = state.mediaItems + accepted,
                                errorMessage = null,
                                statusMessage = if (loaded.size > accepted.size) {
                                    "${accepted.size}개 추가 · 최대 ${MediaAttachmentPolicy.MAX_ITEMS}개"
                                } else {
                                    "${accepted.size}개 추가"
                                }
                            )
                        }
                    }
                }
            } finally {
                onFinished()
            }
        }
    }

    fun addCameraPhoto(bytes: ByteArray, gallerySaved: Boolean = true) {
        update { state ->
            if (MediaAttachmentPolicy.availableSlots(state.mediaItems.size) == 0) state
            else state.copy(
                mediaItems = state.mediaItems +
                    ComposerMedia(data = bytes, kind = MediaKind.IMAGE, fileExtension = "jpg"),
                statusMessage = "촬영한 사진 추가",
                errorMessage = if (gallerySaved) state.errorMessage else "갤러리에 사진을 저장하지 못했어요."
            )
        }
    }

    fun cameraUnavailable() {
        update { it.copy(errorMessage = "이 기기에서는 카메라를 사용할 수 없어요.") }
    }

    fun removeMedia(id: String) {
        update { state -> state.copy(mediaItems = state.mediaItems.filter { it.id != id }) }
    }

    fun moveMedia(index: Int, offset: Int) {
        val destination = index + offset
        update { state ->
            val items = state.mediaItems.toMutableList()
            if (index !in items.indices || destination !in items.indices) return@update state
            val tmp = items[index]
            items[index] = items[destination]
            items[destination] = tmp
            state.copy(mediaItems = items)
        }
    }

    fun reorderMedia(fromIndex: Int, toIndex: Int) {
        update { state ->
            val items = state.mediaItems.toMutableList()
            if (fromIndex !in items.indices || toIndex !in items.indices) return@update state
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)
            state.copy(mediaItems = items)
        }
    }

    // MARK: - 공유

    /** 문구를 복사하고 미디어 공유 인텐트를 준비한다. 반환값 null이면 안내 메시지만 갱신됨. */
    suspend fun prepareShare(context: Context): Intent? {
        val version = resetVersion
        val snapshot = _state.value.mediaItems
        val textToShare = _state.value.idea.ifBlank { _state.value.generatedPost?.composedText ?: "" }
        if (_state.value.isPreparingShare || _state.value.isGenerating) {
            return null
        }
        if (snapshot.isEmpty()) {
            update {
                it.copy(shareMessage = "사진이나 영상을 먼저 추가해 주세요.", shareMessageIsError = true)
            }
            return null
        }
        if (!MediaAttachmentPolicy.canShare(snapshot.size)) {
            update {
                it.copy(
                    shareMessage = "미디어는 최대 ${MediaAttachmentPolicy.MAX_ITEMS}개까지 공유할 수 있어요.",
                    shareMessageIsError = true
                )
            }
            return null
        }
        copyToClipboard(context, textToShare)
        update {
            it.copy(
                isPreparingShare = true,
                errorMessage = null,
                shareMessage = "공유 준비 중",
                shareMessageIsError = false
            )
        }
        return try {
            val uris = withContext(Dispatchers.IO) { prepareShareFiles(snapshot, context) }
            if (version != resetVersion) return null
            if (uris.isEmpty()) {
                throw IllegalStateException("공유할 미디어 파일을 준비하지 못했어요.")
            }
            val passes = activeValidationReport()?.passesAllRules == true
            update {
                it.copy(
                    isPreparingShare = false,
                    shareMessage = if (passes) "문구 복사됨" else "확인 필요 · 문구 복사됨",
                    shareMessageIsError = false
                )
            }
            buildShareIntent(uris, snapshot)
        } catch (error: Exception) {
            if (version != resetVersion) return null
            update {
                it.copy(
                    isPreparingShare = false,
                    shareMessage = "미디어 공유를 준비하지 못했어요: ${error.localizedMessage ?: error.javaClass.simpleName}",
                    shareMessageIsError = true
                )
            }
            null
        }
    }

    private fun prepareShareFiles(mediaItems: List<ComposerMedia>, context: Context): List<Uri> {
        val shareRoot = File(context.cacheDir, "share")
        shareRoot.listFiles()?.forEach { it.deleteRecursively() }
        val directory = File(shareRoot, "imanagerai-share-${UUID.randomUUID()}")
        directory.mkdirs()
        return mediaItems.mapIndexed { index, media ->
            val safeExtension = media.fileExtension
                ?.lowercase()
                ?.filter { it.isLetter() || it.isDigit() }
            val fallback = if (media.kind == MediaKind.IMAGE) "png" else "mp4"
            val ext = if (safeExtension.isNullOrEmpty()) fallback else safeExtension
            val file = File(directory, String.format(Locale.ROOT, "%02d.%s", index + 1, ext))
            file.writeBytes(media.data)
            FileProvider.getUriForFile(context, "com.armsone.starmanager.fileprovider", file)
        }
    }

    internal fun buildShareIntent(uris: List<Uri>, mediaItems: List<ComposerMedia>): Intent {
        require(uris.isNotEmpty()) { "Cannot build share intent with empty URI list" }
        val mimeType = MediaAttachmentPolicy.mimeTypeFor(mediaItems.map { it.kind })
        return if (uris.size == 1) {
            val uri = uris.first()
            val clipData = ClipData(ClipDescription(null, arrayOf(mimeType)), ClipData.Item(uri))
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val clipData = ClipData(ClipDescription(null, arrayOf(mimeType)), ClipData.Item(uris.first()))
            for (i in 1 until uris.size) {
                clipData.addItem(ClipData.Item(uris[i]))
            }
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Stargram", text))
    }

    fun readClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
    }

    fun shouldShowPasteGuidance(context: Context): Boolean {
        val prefs = context.getSharedPreferences("starmanager", Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_PASTE_GUIDANCE_SHOWN, false)
    }

    fun markPasteGuidanceShown(context: Context) {
        val prefs = context.getSharedPreferences("starmanager", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PASTE_GUIDANCE_SHOWN, true).apply()
    }

    companion object {
        const val KEY_PASTE_GUIDANCE_SHOWN = "hasShownPasteGuidance"
    }
}
