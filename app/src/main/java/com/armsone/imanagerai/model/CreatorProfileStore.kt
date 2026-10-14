package com.armsone.imanagerai.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UserDefaults 대응. JVM 테스트에서는 인메모리 구현으로 대체한다. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
}

class SharedPreferencesKeyValueStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("starmanager", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}

class InMemoryKeyValueStore : KeyValueStore {
    private val strings = mutableMapOf<String, String>()
    private val ints = mutableMapOf<String, Int>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getInt(key: String, default: Int): Int = ints[key] ?: default
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun remove(key: String) { strings.remove(key); ints.remove(key) }
}

/** 앱 외형 테마: BK(기본), 클래식, 인터스텔라 3가지 선택지 제공. */
enum class AppAppearance(val title: String) {
    BK("BK"),
    CLASSIC("클래식"),
    INTERSTELLAR("인터스텔라");

    companion object {
        fun fromString(value: String?): AppAppearance = when (value?.uppercase()) {
            "CLASSIC", "클래식" -> CLASSIC
            "INTERSTELLAR", "인터스텔라" -> INTERSTELLAR
            else -> BK
        }
    }
}

/**
 * iOS CreatorProfileStore 포팅.
 * 프로필과 프리셋은 변경 즉시 저장되고, 레거시 JSON(usesEmoji 불리언, 레거시 기본 문구)을
 * 관대하게 새 Typed Enum 구조로 마이그레이션한다.
 */
class CreatorProfileStore(private val storage: KeyValueStore) {

    private val gson = Gson()

    private val _appearance: MutableStateFlow<AppAppearance>
    val appearance: StateFlow<AppAppearance> get() = _appearance.asStateFlow()

    private val _showsExternalAIBrowser: MutableStateFlow<Boolean>
    val showsExternalAIBrowser: StateFlow<Boolean> get() = _showsExternalAIBrowser.asStateFlow()

    private val _automationEnabled: MutableStateFlow<Boolean>
    val automationEnabled: StateFlow<Boolean> get() = _automationEnabled.asStateFlow()

    private val _profile: MutableStateFlow<CreatorProfile>
    val profile: StateFlow<CreatorProfile> get() = _profile.asStateFlow()

    private val _presets: MutableStateFlow<List<WritingPreset>>
    val presets: StateFlow<List<WritingPreset>> get() = _presets.asStateFlow()

    val availabilityStore: AIProviderAvailabilityStore = AIProviderAvailabilityStore(storage)
    val runMetricsStore: AIRunMetricsStore = AIRunMetricsStore(storage)

    init {
        val storedAppearance = storage.getString(APPEARANCE_STORAGE_KEY)
        _appearance = MutableStateFlow(AppAppearance.fromString(storedAppearance))
        _showsExternalAIBrowser = MutableStateFlow(
            storage.getInt(SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY, 0) == 1
        )
        _automationEnabled = MutableStateFlow(
            storage.getInt(AUTOMATION_ENABLED_STORAGE_KEY, 0) == 1
        )
        val storedPresets = storage.getString(PRESETS_STORAGE_KEY)?.let { json ->
            runCatching {
                gson.fromJson<List<WritingPreset>>(
                    json, object : TypeToken<List<WritingPreset>>() {}.type
                )
            }.getOrNull()
        }
        _presets = MutableStateFlow(storedPresets ?: WritingPreset.defaults)

        val rawProfileJson = storage.getString(STORAGE_KEY)
        val decodedProfile = rawProfileJson?.let { parseAndMigrateProfile(it) } ?: CreatorProfile()
        val clampedProfile = decodedProfile.clampCharacterCountToDestinationLimit()

        _profile = MutableStateFlow(clampedProfile)
    }

    fun updateProfile(transform: (CreatorProfile) -> CreatorProfile) {
        val updated = transform(_profile.value).clampCharacterCountToDestinationLimit()
        _profile.value = updated
        save()
    }

    fun setProfile(profile: CreatorProfile) = updateProfile { profile }

    fun restoreDefaultWritingGuidelines() = updateProfile {
        it.copy(writingGuidelines = "")
    }

    fun applyGenerationStyle(preset: GenerationStylePreset) {
        setProfile(preset.applyingTo(_profile.value))
    }

    fun savePreset(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        val current = _profile.value
        val preset = WritingPreset(
            name = name,
            controls = current.controls,
            additionalInstructions = current.additionalInstructions ?: "",
            writingGuidelines = current.writingGuidelines,
            accountTopic = current.accountTopic,
            voice = current.voice,
            audience = current.audience,
            preferredLength = current.preferredLength,
            usesEmoji = current.usesEmoji,
            prohibitedPhrases = current.prohibitedPhrases,
            hashtagStyle = current.hashtagStyle,
            mood = current.mood,
            selectedGenerationStyle = current.selectedGenerationStyle
        )
        _presets.value = _presets.value.filter { it.name != name } + preset
        savePresets()
    }

    fun apply(preset: WritingPreset) = updateProfile { profile ->
        var updated = profile.copy(
            generationControls = preset.controls,
            additionalInstructions = preset.additionalInstructions,
            writingGuidelines = preset.writingGuidelines
        )
        preset.accountTopic?.let { updated = updated.copy(accountTopic = it) }
        preset.voice?.let { updated = updated.copy(voice = it) }
        preset.audience?.let { updated = updated.copy(audience = it) }
        preset.preferredLength?.let { updated = updated.copy(preferredLength = it) }
        preset.usesEmoji?.let { updated = updated.copy(emojiIntensity = if (it) EmojiIntensity.LOW else EmojiIntensity.NONE) }
        preset.prohibitedPhrases?.let { updated = updated.copy(prohibitedPhrases = it) }
        preset.hashtagStyle?.let { updated = updated.copy(hashtagStyle = it) }
        preset.mood?.let { updated = updated.copy(mood = it) }
        preset.selectedGenerationStyle?.let { updated = updated.copy(selectedGenerationStyle = it) }
        updated
    }

    fun deletePreset(index: Int) {
        val current = _presets.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _presets.value = current
            savePresets()
        }
    }

    fun deletePreset(preset: WritingPreset) {
        val index = _presets.value.indexOfFirst { it.id == preset.id }
        if (index >= 0) deletePreset(index)
    }

    fun setAppearance(appearance: AppAppearance) {
        _appearance.value = appearance
        storage.putString(APPEARANCE_STORAGE_KEY, appearance.name)
    }

    fun setShowsExternalAIBrowser(showsBrowser: Boolean) {
        _showsExternalAIBrowser.value = showsBrowser
        storage.putInt(SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY, if (showsBrowser) 1 else 0)
    }

    fun setAutomationEnabled(enabled: Boolean) {
        _automationEnabled.value = enabled
        storage.putInt(AUTOMATION_ENABLED_STORAGE_KEY, if (enabled) 1 else 0)
    }

    private fun save() {
        storage.putString(STORAGE_KEY, gson.toJson(_profile.value))
    }

    private fun savePresets() {
        storage.putString(PRESETS_STORAGE_KEY, gson.toJson(_presets.value))
    }

    private fun parseAndMigrateProfile(json: String): CreatorProfile {
        return try {
            val element = JsonParser.parseString(json)
            if (!element.isJsonObject) return CreatorProfile()
            val obj = element.asJsonObject

            val accountTopicRaw = obj.get("accountTopic")?.asStringOrNull() ?: ""
            val audienceRaw = obj.get("audience")?.asStringOrNull() ?: ""
            val hashtagStyleRaw = obj.get("hashtagStyle")?.asStringOrNull() ?: ""
            val additionalInstructionsRaw = obj.get("additionalInstructions")?.asStringOrNull()
            val prohibitedPhrasesRaw = obj.get("prohibitedPhrases")?.asStringOrNull() ?: ""
            val detailedGuidelinesRaw = obj.get("detailedGuidelines")?.asStringOrNull() ?: ""
            val voiceRaw = obj.get("voice")?.asStringOrNull() ?: ""

            val accountTopic = if (accountTopicRaw == LEGACY_ACCOUNT_TOPIC) "" else accountTopicRaw
            val audience = if (audienceRaw == LEGACY_AUDIENCE) "" else audienceRaw
            val hashtagStyle = if (hashtagStyleRaw == LEGACY_HASHTAG_STYLE) "" else hashtagStyleRaw
            val additionalInstructions = if (additionalInstructionsRaw != null && LEGACY_ADDITIONAL_INSTRUCTIONS.contains(additionalInstructionsRaw)) {
                null
            } else {
                additionalInstructionsRaw
            }
            val voice = if (voiceRaw == LEGACY_VOICE) "" else voiceRaw

            // 이모지 강도 마이그레이션: emojiIntensity가 없으면 usesEmoji(Boolean) 확인
            val emojiIntensity = if (obj.has("emojiIntensity")) {
                runCatching { EmojiIntensity.valueOf(obj.get("emojiIntensity").asString) }.getOrDefault(EmojiIntensity.LOW)
            } else if (obj.has("usesEmoji")) {
                val uses = obj.get("usesEmoji").asBoolean
                if (uses) EmojiIntensity.LOW else EmojiIntensity.NONE
            } else {
                EmojiIntensity.LOW
            }

            val destination = obj.get("destination")?.asStringOrNull()?.let {
                runCatching { PostDestination.valueOf(it) }.getOrNull()
            } ?: PostDestination.INSTAGRAM

            val ageGroup = obj.get("ageGroup")?.asStringOrNull()?.let {
                runCatching { AudienceAgeGroup.valueOf(it) }.getOrNull()
            } ?: AudienceAgeGroup.XZ

            val style = obj.get("style")?.asStringOrNull()?.let {
                runCatching { PostStyle.valueOf(it) }.getOrNull()
            } ?: PostStyle.MEMO

            val tone = obj.get("tone")?.asStringOrNull()?.let {
                runCatching { PostTone.valueOf(it) }.getOrNull()
            } ?: PostTone.KIND

            val lineBreakFrequency = obj.get("lineBreakFrequency")?.asStringOrNull()?.let {
                runCatching { LineBreakFrequency.valueOf(it) }.getOrNull()
            } ?: LineBreakFrequency.MODERATE

            val preferredLength = obj.get("preferredLength")?.asStringOrNull()?.let {
                runCatching { PostLength.valueOf(it) }.getOrNull()
            } ?: PostLength.MEDIUM

            val mood = obj.get("mood")?.asStringOrNull()?.let {
                runCatching { PostMood.valueOf(it) }.getOrNull()
            } ?: PostMood.WITTY

            val generationControls = if (obj.has("generationControls") && obj.get("generationControls").isJsonObject) {
                runCatching { gson.fromJson(obj.get("generationControls"), GenerationControls::class.java) }.getOrNull()
            } else {
                null
            }

            val selectedGenerationStyle = obj.get("selectedGenerationStyle")?.asStringOrNull()?.let {
                runCatching { GenerationStylePreset.valueOf(it) }.getOrNull()
            }

            CreatorProfile(
                accountTopic = accountTopic,
                voice = voice,
                audience = audience,
                preferredLength = preferredLength,
                emojiIntensity = emojiIntensity,
                prohibitedPhrases = prohibitedPhrasesRaw,
                hashtagStyle = hashtagStyle,
                detailedGuidelines = detailedGuidelinesRaw,
                destination = destination,
                ageGroup = ageGroup,
                style = style,
                tone = tone,
                lineBreakFrequency = lineBreakFrequency,
                generationControls = generationControls,
                additionalInstructions = additionalInstructions,
                mood = mood,
                selectedGenerationStyle = selectedGenerationStyle
            )
        } catch (_: Exception) {
            CreatorProfile()
        }
    }

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else null

    companion object {
        const val STORAGE_KEY = "creatorProfile"
        const val PRESETS_STORAGE_KEY = "writingPresets"
        const val DEFAULT_STYLE_VERSION_KEY = "defaultGenerationStyleVersion"
        const val CURRENT_DEFAULT_STYLE_VERSION = 2
        const val APPEARANCE_STORAGE_KEY = "appAppearance"
        const val SHOW_EXTERNAL_AI_BROWSER_STORAGE_KEY = "showsExternalAIBrowser"
        const val AUTOMATION_ENABLED_STORAGE_KEY = "automationEnabled"

        private const val LEGACY_ACCOUNT_TOPIC = "나의 일상과 경험"
        private const val LEGACY_AUDIENCE = "내 이야기에 공감하는 사람들"
        private const val LEGACY_HASHTAG_STYLE = "핵심 키워드 중심"
        private const val LEGACY_VOICE = "다정하고 솔직하게"
        private val LEGACY_ADDITIONAL_INSTRUCTIONS: Set<String> = setOf(
            "억지 유행어는 피하고 설명보다 장면, 장면보다 한 방 있는 말맛을 먼저 보여주기",
            "과한 신파 없이 장면은 선명하게, 결론은 무심한 듯 멋있게 남기기",
            "성공담보다 시행착오를 앞세우고, 잔소리가 될 순간에는 자조적인 유머로 방향 틀기",
            "한 번쯤 훈계할 듯 운을 떼되 결론에서는 자기 흑역사를 꺼내 웃음과 쓸 만한 지혜를 함께 남기기",
            "군더더기 없이 낯선 비유와 짧은 호흡을 사용",
            "잔잔한 여운과 따뜻한 장면 묘사를 강조"
        )
    }
}
