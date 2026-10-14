package com.armsone.imanagerai.ui.composer

import com.armsone.imanagerai.model.CreatorProfile
import com.armsone.imanagerai.model.GeneratedPost
import com.armsone.imanagerai.model.PostLength
import com.armsone.imanagerai.model.PostMood
import com.armsone.imanagerai.service.DirectAIProvider
import java.util.UUID

/** iOS PreviewAspect — 게시 비율 3종. */
enum class PreviewAspect(val title: String, val ratio: Float) {
    SQUARE("1:1", 1f),
    FEED("4:5", 4f / 5f),
    VERTICAL("9:16", 9f / 16f)
}

/** 생성 당시 조건 스냅샷 — 조건이 바뀌면 초안을 만료 처리한다. */
data class DraftSignature(
    val idea: String,
    val mood: PostMood,
    val length: PostLength,
    val profile: CreatorProfile
)

/** 문구 출처 — 비교 카드와 미리보기 라벨에 사용. */
enum class CaptionSource {
    DEVICE,
    DETERMINISTIC,
    GEMINI,
    CHAT_GPT,
    CLAUDE,
    GROK;

    val title: String
        get() = when (this) {
            DEVICE -> "기기 AI"
            DETERMINISTIC -> "기기 AI"
            GEMINI -> "Gemini"
            CHAT_GPT -> "ChatGPT"
            CLAUDE -> "Claude"
            GROK -> "Grok"
        }
}

val DirectAIProvider.captionSource: CaptionSource
    get() = when (this) {
        DirectAIProvider.GEMINI -> CaptionSource.GEMINI
        DirectAIProvider.OPEN_AI -> CaptionSource.CHAT_GPT
        DirectAIProvider.CLAUDE -> CaptionSource.CLAUDE
        DirectAIProvider.GROK -> CaptionSource.GROK
    }

data class CaptionCandidate(
    val source: CaptionSource,
    val post: GeneratedPost,
    val signature: DraftSignature,
    val requestId: String = UUID.randomUUID().toString()
)

enum class MediaKind {
    IMAGE,
    VIDEO;

    val title: String get() = if (this == IMAGE) "사진" else "영상"
}

object MediaAttachmentPolicy {
    const val MAX_ITEMS = 8

    fun availableSlots(currentCount: Int): Int = (MAX_ITEMS - currentCount).coerceAtLeast(0)

    fun canShare(itemCount: Int): Boolean = itemCount in 1..MAX_ITEMS

    fun mimeTypeFor(kinds: Collection<MediaKind>): String {
        val hasImage = kinds.any { it == MediaKind.IMAGE }
        val hasVideo = kinds.any { it == MediaKind.VIDEO }
        return when {
            hasImage && hasVideo -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
    }
}

class ComposerMedia(
    val data: ByteArray,
    val kind: MediaKind,
    val fileExtension: String?,
    val generatedFromPostId: String? = null,
    val id: String = UUID.randomUUID().toString()
)

/** 만들기 카드의 AI 선택 버튼 — Gemini, ChatGPT, Claude, 기기 AI, Random. */
sealed class AIChoice(val id: String, val title: String) {
    data object OnDevice : AIChoice("device-ai", "AI")
    data class External(val provider: DirectAIProvider) : AIChoice(provider.rawValue, provider.title)
    data object Random : AIChoice("random", "Random")

    fun isEnabled(trimmedIdea: String, hasRepresentativePhoto: Boolean): Boolean =
        trimmedIdea.isNotEmpty() || hasRepresentativePhoto

    companion object {
        val visibleChoices: List<AIChoice> = listOf(
            External(DirectAIProvider.GEMINI),
            External(DirectAIProvider.OPEN_AI),
            External(DirectAIProvider.CLAUDE),
            OnDevice,
            Random
        )
        val all: List<AIChoice> = visibleChoices
    }
}
