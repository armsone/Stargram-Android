package com.armsone.imanagerai

import com.armsone.imanagerai.model.AIProviderAvailabilityStore
import com.armsone.imanagerai.model.AIProviderIdentity
import com.armsone.imanagerai.model.AIRunMetric
import com.armsone.imanagerai.model.AIRunMetricsFormatter
import com.armsone.imanagerai.model.AIRunMetricsStore
import com.armsone.imanagerai.model.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AIProviderAvailabilityStoreTest {

    @Test
    fun `초기 상태에서는 세 클라우드 AI가 모두 켜져 있다`() {
        val storage = InMemoryKeyValueStore()
        val store = AIProviderAvailabilityStore(storage)

        assertTrue(store.isOpenAIEnabled.value)
        assertTrue(store.isGeminiEnabled.value)
        assertTrue(store.isClaudeEnabled.value)
        assertTrue(store.isEnabled(AIProviderIdentity.DEVICE))
        assertTrue(store.isEnabled(AIProviderIdentity.OPEN_AI))
        assertTrue(store.isEnabled(AIProviderIdentity.GEMINI))
        assertTrue(store.isEnabled(AIProviderIdentity.CLAUDE))

        val candidates = store.enabledCandidates
        assertEquals(4, candidates.size)
        assertTrue(candidates.contains(AIProviderIdentity.DEVICE))
        assertTrue(candidates.contains(AIProviderIdentity.OPEN_AI))
        assertTrue(candidates.contains(AIProviderIdentity.GEMINI))
        assertTrue(candidates.contains(AIProviderIdentity.CLAUDE))
    }

    @Test
    fun `제공자 토글 시 영속화되고 활성 후보 목록에서 반영된다`() {
        val storage = InMemoryKeyValueStore()
        val store = AIProviderAvailabilityStore(storage)

        store.setOpenAIEnabled(false)
        assertFalse(store.isOpenAIEnabled.value)
        assertFalse(store.isEnabled(AIProviderIdentity.OPEN_AI))
        assertFalse(store.enabledCandidates.contains(AIProviderIdentity.OPEN_AI))

        // 재로드 시에도 반영
        val reloaded = AIProviderAvailabilityStore(storage)
        assertFalse(reloaded.isOpenAIEnabled.value)
        assertTrue(reloaded.isGeminiEnabled.value)
        assertTrue(reloaded.isClaudeEnabled.value)
    }

    @Test
    fun `비활성화된 활성 AI는 GEMINI OPEN_AI CLAUDE DEVICE 순서로 자동 폴백된다`() {
        val storage = InMemoryKeyValueStore()
        val store = AIProviderAvailabilityStore(storage)

        // Gemini가 켜져 있으면 선호 AI가 꺼졌을 때 Gemini로 폴백
        store.setOpenAIEnabled(false)
        assertEquals(AIProviderIdentity.GEMINI, store.resolvedChoice(preferring = AIProviderIdentity.OPEN_AI))

        // Gemini도 꺼지면 Claude로 폴백 (OpenAI는 꺼짐)
        store.setGeminiEnabled(false)
        assertEquals(AIProviderIdentity.CLAUDE, store.resolvedChoice(preferring = AIProviderIdentity.OPEN_AI))

        // 모두 꺼지면 온디바이스 AI(DEVICE)로 폴백
        store.setClaudeEnabled(false)
        assertEquals(AIProviderIdentity.DEVICE, store.resolvedChoice(preferring = AIProviderIdentity.OPEN_AI))
        assertEquals(listOf(AIProviderIdentity.DEVICE), store.enabledCandidates)
    }

    @Test
    fun `마지막 사용 AI가 정상적으로 기록되고 꺼지면 fallback을 제공한다`() {
        val storage = InMemoryKeyValueStore()
        val store = AIProviderAvailabilityStore(storage)

        store.recordUsed(AIProviderIdentity.CLAUDE)
        assertEquals(AIProviderIdentity.CLAUDE, store.lastUsedChoice.value)

        // Claude 비활성화 시 resolved는 Gemini로 폴백
        store.setClaudeEnabled(false)
        assertEquals(AIProviderIdentity.GEMINI, store.resolvedChoice(preferring = store.lastUsedChoice.value))
    }

    @Test
    fun `실행 메트릭 저장 및 포맷터가 정상 동작한다`() {
        val storage = InMemoryKeyValueStore()
        val metricsStore = AIRunMetricsStore(storage)

        // 초기 상태에는 아무 기록 없음
        assertEquals("아직 실행 안 함", AIRunMetricsFormatter.formatSubtitle(null))

        // 실행 기록 추가
        val now = Date()
        val metric = AIRunMetric(
            lastRunDate = now,
            lastElapsedSeconds = 3.2
        )
        metricsStore.recordRun(AIProviderIdentity.OPEN_AI, 3.2)

        val retrieved = metricsStore.metricFor(AIProviderIdentity.OPEN_AI)
        assertTrue(retrieved != null)
        assertEquals(3.2, retrieved!!.lastElapsedSeconds, 0.01)

        val formatted = AIRunMetricsFormatter.formatSubtitle(retrieved)
        assertTrue(formatted.startsWith("마지막 실행"))
        assertTrue(formatted.contains("3.2초 걸림"))

        // 영속화 확인
        val reloaded = AIRunMetricsStore(storage)
        val reloadedMetric = reloaded.metricFor(AIProviderIdentity.OPEN_AI)
        assertTrue(reloadedMetric != null)
        assertEquals(3.2, reloadedMetric!!.lastElapsedSeconds, 0.01)
    }
}
