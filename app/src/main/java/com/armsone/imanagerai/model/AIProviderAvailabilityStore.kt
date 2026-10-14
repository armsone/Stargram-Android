package com.armsone.imanagerai.model

import com.armsone.imanagerai.service.DirectAIProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 클라우드 AI 제공자(ChatGPT·Gemini·Claude)를 독립적으로 켜고 끄고,
 * 스튜디오·자동화가 마지막으로 사용한 AI를 안전하게 선택할 수 있도록 돕는 저장소.
 * 세 클라우드 제공자를 모두 꺼도 온디바이스(기기 AI)는 항상 사용할 수 있다.
 */
class AIProviderAvailabilityStore(private val storage: KeyValueStore) {

    private val _isOpenAIEnabled: MutableStateFlow<Boolean>
    val isOpenAIEnabled: StateFlow<Boolean> get() = _isOpenAIEnabled.asStateFlow()

    private val _isGeminiEnabled: MutableStateFlow<Boolean>
    val isGeminiEnabled: StateFlow<Boolean> get() = _isGeminiEnabled.asStateFlow()

    private val _isClaudeEnabled: MutableStateFlow<Boolean>
    val isClaudeEnabled: StateFlow<Boolean> get() = _isClaudeEnabled.asStateFlow()

    private val _lastUsedChoice: MutableStateFlow<AIProviderIdentity>
    val lastUsedChoice: StateFlow<AIProviderIdentity> get() = _lastUsedChoice.asStateFlow()

    init {
        val openAI = storage.getInt(OPEN_AI_KEY, 1) == 1
        val gemini = storage.getInt(GEMINI_KEY, 1) == 1
        val claude = storage.getInt(CLAUDE_KEY, 1) == 1
        _isOpenAIEnabled = MutableStateFlow(openAI)
        _isGeminiEnabled = MutableStateFlow(gemini)
        _isClaudeEnabled = MutableStateFlow(claude)

        val rawLastUsed = storage.getString(LAST_USED_KEY)
        val initialChoice = AIProviderIdentity.fromRawValue(rawLastUsed) ?: AIProviderIdentity.DEVICE
        _lastUsedChoice = MutableStateFlow(resolveInitialChoice(initialChoice, openAI, gemini, claude))
    }

    fun isEnabled(identity: AIProviderIdentity): Boolean = when (identity) {
        AIProviderIdentity.DEVICE -> true
        AIProviderIdentity.OPEN_AI -> _isOpenAIEnabled.value
        AIProviderIdentity.GEMINI -> _isGeminiEnabled.value
        AIProviderIdentity.CLAUDE -> _isClaudeEnabled.value
    }

    fun isEnabled(provider: DirectAIProvider): Boolean = when (provider) {
        DirectAIProvider.OPEN_AI -> _isOpenAIEnabled.value
        DirectAIProvider.GEMINI -> _isGeminiEnabled.value
        DirectAIProvider.CLAUDE -> _isClaudeEnabled.value
        DirectAIProvider.GROK -> false
    }

    fun setEnabled(enabled: Boolean, identity: AIProviderIdentity) {
        when (identity) {
            AIProviderIdentity.DEVICE -> return
            AIProviderIdentity.OPEN_AI -> {
                _isOpenAIEnabled.value = enabled
                storage.putInt(OPEN_AI_KEY, if (enabled) 1 else 0)
            }
            AIProviderIdentity.GEMINI -> {
                _isGeminiEnabled.value = enabled
                storage.putInt(GEMINI_KEY, if (enabled) 1 else 0)
            }
            AIProviderIdentity.CLAUDE -> {
                _isClaudeEnabled.value = enabled
                storage.putInt(CLAUDE_KEY, if (enabled) 1 else 0)
            }
        }
        if (!enabled && _lastUsedChoice.value == identity) {
            _lastUsedChoice.value = resolvedChoice(preferring = identity)
            storage.putString(LAST_USED_KEY, _lastUsedChoice.value.rawValue)
        }
    }

    fun setOpenAIEnabled(enabled: Boolean) = setEnabled(enabled, AIProviderIdentity.OPEN_AI)
    fun setGeminiEnabled(enabled: Boolean) = setEnabled(enabled, AIProviderIdentity.GEMINI)
    fun setClaudeEnabled(enabled: Boolean) = setEnabled(enabled, AIProviderIdentity.CLAUDE)

    fun recordUsed(identity: AIProviderIdentity) {
        if (!isEnabled(identity)) return
        _lastUsedChoice.value = identity
        storage.putString(LAST_USED_KEY, identity.rawValue)
    }

    /**
     * 선호 선택지가 꺼져 있으면 켜진 클라우드 제공자로, 그것도 없으면 기기 AI로 안전하게 대체한다.
     */
    fun resolvedChoice(preferring: AIProviderIdentity): AIProviderIdentity {
        if (isEnabled(preferring)) return preferring
        val fallback = CLOUD_FALLBACK_ORDER.firstOrNull { isEnabled(it) }
        return fallback ?: AIProviderIdentity.DEVICE
    }

    val enabledCandidates: List<AIProviderIdentity>
        get() = buildList {
            add(AIProviderIdentity.DEVICE)
            if (_isGeminiEnabled.value) add(AIProviderIdentity.GEMINI)
            if (_isOpenAIEnabled.value) add(AIProviderIdentity.OPEN_AI)
            if (_isClaudeEnabled.value) add(AIProviderIdentity.CLAUDE)
        }

    val enabledExternalProviders: List<DirectAIProvider>
        get() = listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
            .filter { isEnabled(it) }

    private fun resolveInitialChoice(
        choice: AIProviderIdentity,
        openAI: Boolean,
        gemini: Boolean,
        claude: Boolean
    ): AIProviderIdentity {
        val enabled = when (choice) {
            AIProviderIdentity.DEVICE -> true
            AIProviderIdentity.OPEN_AI -> openAI
            AIProviderIdentity.GEMINI -> gemini
            AIProviderIdentity.CLAUDE -> claude
        }
        if (enabled) return choice
        if (gemini) return AIProviderIdentity.GEMINI
        if (openAI) return AIProviderIdentity.OPEN_AI
        if (claude) return AIProviderIdentity.CLAUDE
        return AIProviderIdentity.DEVICE
    }

    companion object {
        const val OPEN_AI_KEY = "aiProviderEnabled.openAI"
        const val GEMINI_KEY = "aiProviderEnabled.gemini"
        const val CLAUDE_KEY = "aiProviderEnabled.claude"
        const val LAST_USED_KEY = "aiProviderLastUsedChoice"

        val CLOUD_FALLBACK_ORDER = listOf(
            AIProviderIdentity.GEMINI,
            AIProviderIdentity.OPEN_AI,
            AIProviderIdentity.CLAUDE
        )
    }
}
