package com.armsone.imanagerai.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 제공자별 마지막 실제 실행 시각과 걸린 시간(초).
 * 설정 화면에서 어떤 AI가 실행됐고 응답을 받는 데 얼마나 걸렸는지 보여준다.
 */
data class AIRunMetric(
    val lastRunAtMillis: Long,
    val elapsedSeconds: Double
) {
    constructor(lastRunDate: Date, lastElapsedSeconds: Double) : this(
        lastRunAtMillis = lastRunDate.time,
        elapsedSeconds = lastElapsedSeconds
    )

    val lastRunDate: Date get() = Date(lastRunAtMillis)
    val lastElapsedSeconds: Double get() = elapsedSeconds
}

class AIRunMetricsStore(private val storage: KeyValueStore) {

    private val gson = Gson()
    private val _metrics = MutableStateFlow<Map<AIProviderIdentity, AIRunMetric>>(emptyMap())
    val metrics: StateFlow<Map<AIProviderIdentity, AIRunMetric>> get() = _metrics.asStateFlow()

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        val json = storage.getString(STORAGE_KEY)
        if (!json.isNullOrBlank()) {
            val decoded = runCatching {
                val mapType = object : TypeToken<Map<String, AIRunMetric>>() {}.type
                val rawMap: Map<String, AIRunMetric>? = gson.fromJson(json, mapType)
                rawMap?.mapNotNull { (key, value) ->
                    AIProviderIdentity.fromRawValue(key)?.let { it to value }
                }?.toMap()
            }.getOrNull()
            if (decoded != null) {
                _metrics.value = decoded
            }
        }
    }

    fun metric(identity: AIProviderIdentity): AIRunMetric? = _metrics.value[identity]
    fun metricFor(identity: AIProviderIdentity): AIRunMetric? = metric(identity)

    /**
     * 실제 온디바이스 생성 완료 또는 외부 AI 요청~응답 수신 시점에만 기록한다.
     */
    fun recordRun(
        identity: AIProviderIdentity,
        elapsedSeconds: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val safeElapsed = maxOf(0.0, elapsedSeconds)
        val updated = _metrics.value.toMutableMap()
        updated[identity] = AIRunMetric(lastRunAtMillis = timestamp, elapsedSeconds = safeElapsed)
        _metrics.value = updated
        persist()
    }

    private fun persist() {
        val rawMap = _metrics.value.mapKeys { it.key.rawValue }
        storage.putString(STORAGE_KEY, gson.toJson(rawMap))
    }

    companion object {
        const val STORAGE_KEY = "aiProviderRunMetrics"
    }
}

object AIRunMetricsFormatter {
    private val dateFormat = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA)

    fun format(metric: AIRunMetric?): String {
        if (metric == null || metric.lastRunAtMillis <= 0L) {
            return "아직 실행 안 함"
        }
        val dateText = dateFormat.format(Date(metric.lastRunAtMillis))
        val seconds = metric.elapsedSeconds
        val formattedSeconds = if (seconds >= 10.0 || seconds % 1.0 == 0.0) {
            String.format(Locale.KOREA, "%.0f", seconds)
        } else {
            String.format(Locale.KOREA, "%.1f", seconds)
        }
        return "마지막 실행 $dateText · ${formattedSeconds}초 걸림"
    }

    fun formatSubtitle(metric: AIRunMetric?): String = format(metric)
}
