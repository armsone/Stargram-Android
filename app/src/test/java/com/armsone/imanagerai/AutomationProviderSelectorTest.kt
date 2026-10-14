package com.armsone.imanagerai

import com.armsone.imanagerai.model.AIProviderIdentity
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.ui.automation.AutomationProviderSelector
import com.armsone.imanagerai.ui.automation.AutomationSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationProviderSelectorTest {

    @Test
    fun `무작위 선택은 Grok을 제외한 온디바이스 및 활성 클라우드 중에서만 고른다`() {
        repeat(50) {
            val identity = AutomationProviderSelector.selectRandom(
                candidates = AutomationProviderSelector.ELIGIBLE_IDENTITIES
            )
            assertTrue(identity in AutomationProviderSelector.ELIGIBLE_IDENTITIES)
            assertFalse(identity.rawValue == "grok")
        }
    }

    @Test
    fun `제외 대상을 지정하면 남은 후보 중에서만 고른다`() {
        repeat(50) {
            val providerGeminiExcluded = AutomationProviderSelector.selectRandom(exclude = DirectAIProvider.GEMINI)
            assertFalse(providerGeminiExcluded == DirectAIProvider.GEMINI)
            assertFalse(providerGeminiExcluded == DirectAIProvider.GROK)
            assertTrue(providerGeminiExcluded == DirectAIProvider.OPEN_AI || providerGeminiExcluded == DirectAIProvider.CLAUDE)

            val identityDeviceExcluded = AutomationProviderSelector.selectRandom(
                candidates = listOf(AIProviderIdentity.DEVICE, AIProviderIdentity.OPEN_AI),
                excludeIdentity = AIProviderIdentity.DEVICE
            )
            assertEquals(AIProviderIdentity.OPEN_AI, identityDeviceExcluded)
        }
    }

    @Test
    fun `후보 풀이 비어있으면 DEVICE로 안전하게 폴백한다`() {
        val fallback = AutomationProviderSelector.selectRandom(candidates = emptyList())
        assertEquals(AIProviderIdentity.DEVICE, fallback)
    }

    @Test
    fun `모든 클라우드 AI가 꺼진 경우 온디바이스만 후보로 선택된다`() {
        val candidates = listOf(AIProviderIdentity.DEVICE)
        repeat(20) {
            val chosen = AutomationProviderSelector.selectRandom(candidates = candidates)
            assertEquals(AIProviderIdentity.DEVICE, chosen)
        }
    }

    @Test
    fun `AutomationSessionState 모델 상태 계약이 유효하다`() {
        val idle = AutomationSessionState.Idle
        assertTrue(idle is AutomationSessionState)

        val processing = AutomationSessionState.Processing(
            provider = AIProviderIdentity.GEMINI,
            stepTitle = "사진을 준비하는 중…",
            stepSubtitle = "선택한 사진 5장을 전송용으로 줄이고 있어요",
            elapsedSeconds = 10L,
            attachments = emptyList(),
            rawImages = emptyList(),
            requestId = 1
        )
        assertEquals(AIProviderIdentity.GEMINI, processing.provider)
        assertEquals(10L, processing.elapsedSeconds)
        assertEquals(1, processing.requestId)

        val result = AutomationSessionState.Result(
            provider = AIProviderIdentity.OPEN_AI,
            generatedText = "완성된 글",
            attachments = emptyList(),
            rawImages = emptyList()
        )
        assertEquals(AIProviderIdentity.OPEN_AI, result.provider)
        assertEquals("완성된 글", result.generatedText)
        assertFalse(result.isSharing)

        val failure = AutomationSessionState.Failure(
            lastProvider = AIProviderIdentity.CLAUDE,
            errorMessage = "에러 메시지",
            attachments = emptyList(),
            rawImages = emptyList()
        )
        assertEquals(AIProviderIdentity.CLAUDE, failure.lastProvider)
        assertEquals("에러 메시지", failure.errorMessage)
    }
}
