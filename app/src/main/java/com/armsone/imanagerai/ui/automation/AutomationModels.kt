package com.armsone.imanagerai.ui.automation

import com.armsone.imanagerai.model.AIProviderIdentity
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.ui.externalai.ExternalAIAttachment

/**
 * Automation Studio 상태 모델.
 * 무작위 AI(온디바이스 + 켜진 클라우드 제공사) 선정, 1~8장 정규화 이미지 처리, 타임아웃, 전체화면 상태 제어를 담당한다.
 */
sealed class AutomationSessionState {
    data object Idle : AutomationSessionState()

    data class Processing(
        val provider: AIProviderIdentity,
        val stepTitle: String,
        val stepSubtitle: String,
        val elapsedSeconds: Long = 0L,
        val attachments: List<ExternalAIAttachment> = emptyList(),
        val rawImages: List<ByteArray> = emptyList(),
        val requestId: Int = 0
    ) : AutomationSessionState()

    data class Result(
        val provider: AIProviderIdentity,
        val generatedText: String,
        val attachments: List<ExternalAIAttachment>,
        val rawImages: List<ByteArray>,
        val isSharing: Boolean = false,
        val shareMessage: String? = null
    ) : AutomationSessionState()

    data class Failure(
        val lastProvider: AIProviderIdentity,
        val errorMessage: String,
        val attachments: List<ExternalAIAttachment>,
        val rawImages: List<ByteArray>
    ) : AutomationSessionState()
}

/**
 * 무작위 자동화 제공사 선택기.
 * 온디바이스(기기 AI)와 활성화된 클라우드 제공사(Gemini, ChatGPT, Claude)를 포함하며 Grok은 무작위 자동화에서 제외된다.
 */
object AutomationProviderSelector {
    val ELIGIBLE_IDENTITIES = listOf(
        AIProviderIdentity.DEVICE,
        AIProviderIdentity.GEMINI,
        AIProviderIdentity.OPEN_AI,
        AIProviderIdentity.CLAUDE
    )

    val ELIGIBLE_PROVIDERS = listOf(
        DirectAIProvider.GEMINI,
        DirectAIProvider.OPEN_AI,
        DirectAIProvider.CLAUDE
    )

    fun candidatePool(
        isGeminiEnabled: Boolean = true,
        isOpenAIEnabled: Boolean = true,
        isClaudeEnabled: Boolean = true
    ): List<AIProviderIdentity> = buildList {
        add(AIProviderIdentity.DEVICE)
        if (isGeminiEnabled) add(AIProviderIdentity.GEMINI)
        if (isOpenAIEnabled) add(AIProviderIdentity.OPEN_AI)
        if (isClaudeEnabled) add(AIProviderIdentity.CLAUDE)
    }

    fun selectRandom(
        candidates: List<AIProviderIdentity> = ELIGIBLE_IDENTITIES,
        exclude: AIProviderIdentity? = null,
        excludeIdentity: AIProviderIdentity? = null
    ): AIProviderIdentity {
        val targetExclude = excludeIdentity ?: exclude
        val pool = candidates.ifEmpty { listOf(AIProviderIdentity.DEVICE) }
        val filtered = if (targetExclude != null && pool.size > 1) {
            pool.filter { it != targetExclude }.ifEmpty { pool }
        } else {
            pool
        }
        return filtered.random()
    }

    fun selectRandom(exclude: DirectAIProvider? = null): DirectAIProvider {
        val candidates = if (exclude != null) {
            ELIGIBLE_PROVIDERS.filter { it != exclude }.ifEmpty { ELIGIBLE_PROVIDERS }
        } else {
            ELIGIBLE_PROVIDERS
        }
        return candidates.random()
    }
}
