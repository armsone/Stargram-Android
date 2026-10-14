package com.armsone.imanagerai.model

import com.armsone.imanagerai.service.DirectAIProvider

/**
 * 기기 AI와 클라우드 제공자를 하나의 모델 체계로 다루기 위한 통합 식별자.
 * Grok은 사용자 노출 목록에서 제외돼 있어 이 열거형에 포함하지 않는다.
 */
enum class AIProviderIdentity(val rawValue: String, val title: String) {
    DEVICE("device", "기기 AI"),
    OPEN_AI("openAI", "ChatGPT"),
    GEMINI("gemini", "Gemini"),
    CLAUDE("claude", "Claude");

    val externalProvider: DirectAIProvider?
        get() = when (this) {
            DEVICE -> null
            OPEN_AI -> DirectAIProvider.OPEN_AI
            GEMINI -> DirectAIProvider.GEMINI
            CLAUDE -> DirectAIProvider.CLAUDE
        }

    companion object {
        fun fromRawValue(rawValue: String?): AIProviderIdentity? =
            entries.firstOrNull { it.rawValue.equals(rawValue, ignoreCase = true) }

        fun fromExternalProvider(provider: DirectAIProvider): AIProviderIdentity? = when (provider) {
            DirectAIProvider.OPEN_AI -> OPEN_AI
            DirectAIProvider.GEMINI -> GEMINI
            DirectAIProvider.CLAUDE -> CLAUDE
            DirectAIProvider.GROK -> null
        }
    }
}
