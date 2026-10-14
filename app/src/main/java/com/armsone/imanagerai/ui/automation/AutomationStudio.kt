package com.armsone.imanagerai.ui.automation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.armsone.imanagerai.R
import com.armsone.imanagerai.design.BrandTheme
import com.armsone.imanagerai.design.GlossyPrimaryButton
import com.armsone.imanagerai.model.AIProviderIdentity
import com.armsone.imanagerai.model.AppAppearance
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.ui.externalai.ExternalAIAnswerCleaner
import com.armsone.imanagerai.ui.externalai.ExternalAIAttachment
import com.armsone.imanagerai.ui.externalai.ExternalAITimerFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Stargram Automation Studio 전체 화면 서피스.
 * Deep space obsidian 배경, 회전하는 블랙홀/포털 링, 엔진 플레어 모션,
 * 40x40 썸네일 스트립, 빛나는 마크다운 결과 카드, 인스타그램 공유 CTA를 포함한다.
 */
@Composable
fun AutomationStudio(
    state: AutomationSessionState,
    onStartAutomation: (images: List<ByteArray>, provider: AIProviderIdentity?) -> Unit,
    onCancel: () -> Unit
) {
    if (state is AutomationSessionState.Idle) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        onCancel()
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandTheme.interstellarCanvas)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 배경 우주 모션 & 포털 링
            CosmicSpaceBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 헤더: 시그니처 타이틀 + 즉시 닫기 X 버튼
                AutomationStudioTopBar(
                    onClose = onCancel
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is AutomationSessionState.Processing -> {
                            AutomationProcessingView(
                                state = state
                            )
                        }
                        is AutomationSessionState.Result -> {
                            AutomationResultView(
                                state = state,
                                onShareInstagram = { text, attachments ->
                                    scope.launch {
                                        shareToInstagram(context, text, attachments)
                                    }
                                },
                                onRetryNew = {
                                    val nextProvider = AutomationProviderSelector.selectRandom(exclude = state.provider)
                                    onStartAutomation(state.rawImages, nextProvider)
                                },
                                onDismiss = onCancel
                            )
                        }
                        is AutomationSessionState.Failure -> {
                            AutomationFailureView(
                                state = state,
                                onRetry = {
                                    val nextProvider = AutomationProviderSelector.selectRandom(exclude = state.lastProvider)
                                    onStartAutomation(state.rawImages, nextProvider)
                                },
                                onClose = onCancel
                            )
                        }
                        AutomationSessionState.Idle -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationStudioTopBar(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = BrandTheme.interstellarAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Stargram 자동화",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTheme.interstellarLabelPrimary,
                letterSpacing = 0.5.sp
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .background(Color(0x2EFFFFFF), CircleShape)
                .testTag("automation.cancel")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "닫기",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 처리 중 화면: 회전하는 블랙홀/포털 링 + 현재 AI 아이콘, 단계별 메시지,
 * 1:59 역카운트다운 타이머, 축소 프로그레스 바, 하단 AI 캐릭터 페르소나 배지.
 */
@Composable
private fun AutomationProcessingView(
    state: AutomationSessionState.Processing
) {
    val progress = ExternalAITimerFormatter.progress(state.elapsedSeconds)
    val timeFormatted = ExternalAITimerFormatter.formatWaitingStatus(state.elapsedSeconds)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 회전하는 포털 링과 중심 AI 아이콘
        CosmicPortal(
            provider = state.provider,
            modifier = Modifier.size(190.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 단계 제목
        Text(
            text = state.stepTitle,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 단계 부제목 / 카운트다운
        Text(
            text = state.stepSubtitle,
            fontSize = 14.sp,
            color = BrandTheme.interstellarPlatinum,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 프로그레스 바 (119초 동안 1.0 -> 0.0으로 축소)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF24262E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandTheme.interstellarAccent, Color(0xFFE2C992))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 하단 선택된 AI의 캐릭터풍 로고 배지 (기존 사람 실루엣 대체)
        AutomationProviderPersonaBadge(
            provider = state.provider,
            modifier = Modifier.testTag("automation.personaBadge")
        )
    }
}

/**
 * 결과 화면: 40x40 썸네일 스트립, 빛나는 마크다운 결과 카드, AI 페르소나, 인스타그램 CTA.
 */
@Composable
private fun AutomationResultView(
    state: AutomationSessionState.Result,
    onShareInstagram: (text: String, attachments: List<ExternalAIAttachment>) -> Unit,
    onRetryNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 40x40 입력 썸네일 스트립
            if (state.rawImages.isNotEmpty()) {
                ThumbnailStrip(
                    images = state.rawImages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("automation.thumbnailStrip")
                )
            }

            // 2. 빛나는 결과 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x33000000), spotColor = Color(0x55C7AD73))
                    .background(BrandTheme.interstellarSurface, RoundedCornerShape(20.dp))
                    .border(
                        BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    BrandTheme.interstellarAccent.copy(alpha = 0.7f),
                                    BrandTheme.interstellarPlatinum.copy(alpha = 0.4f),
                                    BrandTheme.interstellarAccent.copy(alpha = 0.5f)
                                )
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
                    .testTag("automation.resultCard")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = BrandTheme.interstellarAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${state.provider.title} 작성 완료",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTheme.interstellarAccent
                            )
                        }

                        Text(
                            text = "문구 자동 복사됨",
                            fontSize = 12.sp,
                            color = BrandTheme.interstellarPlatinum
                        )
                    }

                    Text(
                        text = state.generatedText,
                        fontSize = 16.sp,
                        color = BrandTheme.interstellarLabelPrimary,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI 캐릭터 페르소나 배지
            AutomationProviderPersonaBadge(
                provider = state.provider,
                modifier = Modifier.testTag("automation.personaBadge")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 인스타그램 버튼 & 보조 액션 행
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InstagramShareCTA(
                onClick = { onShareInstagram(state.generatedText, state.attachments) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("automation.shareInstagram")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRetryNew,
                    modifier = Modifier.testTag("automation.retryNew")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = BrandTheme.interstellarAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "다시 만들기",
                            color = BrandTheme.interstellarAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("automation.dismiss")
                ) {
                    Text(
                        text = "취소",
                        color = BrandTheme.interstellarPlatinum,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 실패 화면: 에러 안내, 다시 시도, 닫기.
 */
@Composable
private fun AutomationFailureView(
    state: AutomationSessionState.Failure,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .background(BrandTheme.interstellarSurface, RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color(0xFF3E4352)), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFFF453A),
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = "게시물을 만들지 못했어요",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.errorMessage,
            fontSize = 14.sp,
            color = BrandTheme.interstellarPlatinum,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("automation.close")
            ) {
                Text(text = "닫기", color = BrandTheme.interstellarPlatinum, fontSize = 16.sp)
            }

            GlossyPrimaryButton(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("automation.retry"),
                appearance = AppAppearance.INTERSTELLAR
            ) {
                Text(
                    text = "다시 시도",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTheme.interstellarAccent
                )
            }
        }
    }
}

/**
 * 40x40 썸네일 스트립.
 */
@Composable
private fun ThumbnailStrip(
    images: List<ByteArray>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        images.forEachIndexed { index, bytes ->
            val bitmap by produceState<Bitmap?>(initialValue = null, bytes) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 120) {
                            sample *= 2
                        }
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    }.getOrNull()
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF24262E))
                    .border(BorderStroke(1.dp, Color(0xFF3E4352)), RoundedCornerShape(8.dp))
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "사진 ${index + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * 인스타그램 전용 보라-분홍-주황 그라데이션 CTA 버튼.
 */
@Composable
private fun InstagramShareCTA(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    val instagramGradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF833AB4),
            Color(0xFFFD1D1D),
            Color(0xFFFCB045)
        )
    )

    Box(
        modifier = modifier
            .shadow(if (pressed) 4.dp else 10.dp, shape, ambientColor = Color(0x33FD1D1D), spotColor = Color(0x55FCB045))
            .background(instagramGradient, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Instagram으로 보내기",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * 선택된 AI의 캐릭터풍 로고 배지 + 글로우 (사람 실루엣 대체).
 */
@Composable
fun AutomationProviderPersonaBadge(
    provider: AIProviderIdentity,
    modifier: Modifier = Modifier
) {
    val brandRes = when (provider) {
        AIProviderIdentity.GEMINI -> R.drawable.brand_gemini
        AIProviderIdentity.OPEN_AI -> R.drawable.brand_chatgpt
        AIProviderIdentity.CLAUDE -> R.drawable.brand_claude
        AIProviderIdentity.DEVICE -> null
    }

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(14.dp, CircleShape, ambientColor = Color(0x44000000), spotColor = Color(0x66C7AD73))
                .background(
                    Brush.radialGradient(
                        listOf(
                            BrandTheme.interstellarAccent.copy(alpha = 0.35f),
                            BrandTheme.interstellarSurface,
                            Color(0xFF14161D)
                        )
                    ),
                    CircleShape
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                BrandTheme.interstellarAccent,
                                BrandTheme.interstellarPlatinum.copy(alpha = 0.6f),
                                BrandTheme.interstellarAccent.copy(alpha = 0.8f)
                            )
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (brandRes != null) {
                Image(
                    painter = painterResource(brandRes),
                    contentDescription = provider.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = provider.title,
                    tint = Color(0xFF3DDC84),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Text(
            text = "${provider.title} 캐릭터",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandTheme.interstellarPlatinum,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * 우주 포털 / 블랙홀 링 + 중심 AI 아이콘.
 */
@Composable
private fun CosmicPortal(
    provider: AIProviderIdentity,
    modifier: Modifier = Modifier
) {
    val brandRes = when (provider) {
        AIProviderIdentity.GEMINI -> R.drawable.brand_gemini
        AIProviderIdentity.OPEN_AI -> R.drawable.brand_chatgpt
        AIProviderIdentity.CLAUDE -> R.drawable.brand_claude
        AIProviderIdentity.DEVICE -> null
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CosmicPortalCanvas(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xCC1A1B20)),
            contentAlignment = Alignment.Center
        ) {
            if (brandRes != null) {
                Image(
                    painter = painterResource(brandRes),
                    contentDescription = provider.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = provider.title,
                    tint = Color(0xFF3DDC84),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 우주 포털 / 블랙홀 링 캔버스 렌더링.
 */
@Composable
private fun CosmicPortalCanvas(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "portalRotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // 1. 외곽 골드/시안 빛나는 링
        rotate(angle, center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFC7AD73).copy(alpha = 0.8f),
                        Color(0xFF5AC8FA).copy(alpha = 0.6f),
                        Color(0xFFC7AD73).copy(alpha = 0.2f),
                        Color(0xFFC7AD73).copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = maxRadius * 0.90f * pulse,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // 2. 중간 역회전 링
        rotate(-angle * 1.5f, center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF8A909D).copy(alpha = 0.7f),
                        Color(0xFFC7AD73).copy(alpha = 0.5f),
                        Color(0xFF8A909D).copy(alpha = 0.1f),
                        Color(0xFF8A909D).copy(alpha = 0.7f)
                    ),
                    center = center
                ),
                radius = maxRadius * 0.72f,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // 3. 내부 링 및 중심 코어 글로우
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFC7AD73).copy(alpha = 0.35f),
                    Color(0xFF1A1B20).copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.55f
            ),
            radius = maxRadius * 0.55f
        )
    }
}

/**
 * 딥스페이스 배경 & 별자리/파티클 렌더링.
 */
@Composable
private fun CosmicSpaceBackground(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "starsDrift")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 은은한 그라데이션 글로우
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF12141A),
                    Color(0xFF0D0E12),
                    Color(0xFF07080A)
                )
            )
        )

        // 미세 별 파티클들
        val starPositions = listOf(
            Offset(w * 0.15f, h * 0.12f),
            Offset(w * 0.82f, h * 0.18f),
            Offset(w * 0.28f, h * 0.45f),
            Offset(w * 0.75f, h * 0.62f),
            Offset(w * 0.42f, h * 0.85f),
            Offset(w * 0.88f, h * 0.78f)
        )

        starPositions.forEachIndexed { i, pos ->
            val alpha = 0.3f + 0.5f * ((drift + i * 0.2f) % 1f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = (1.2f + (i % 2) * 0.8f).dp.toPx(),
                center = pos
            )
        }
    }
}

/**
 * 인스타그램으로 문구 복사 및 미디어 공유 핸드오프.
 */
private suspend fun shareToInstagram(
    context: Context,
    text: String,
    attachments: List<ExternalAIAttachment>
) {
    // 1. 문구 클립보드 복사
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Stargram", text))

    // 2. 미디어 파일 생성 및 FileProvider URI 준비
    val uris = withContext(Dispatchers.IO) {
        val shareRoot = File(context.cacheDir, "share")
        shareRoot.listFiles()?.forEach { it.deleteRecursively() }
        val directory = File(shareRoot, "imanagerai-auto-${UUID.randomUUID()}")
        directory.mkdirs()

        attachments.mapIndexed { index, att ->
            val file = File(directory, String.format(Locale.ROOT, "%02d.jpg", index + 1))
            file.writeBytes(att.data)
            FileProvider.getUriForFile(context, "com.armsone.starmanager.fileprovider", file)
        }
    }

    if (uris.isEmpty()) return

    // 3. 인텐트 구성 및 시스템 선택창 실행
    val intent = if (uris.size == 1) {
        val uri = uris.first()
        val clipData = ClipData(null, arrayOf("image/jpeg"), ClipData.Item(uri))
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        val clipData = ClipData(null, arrayOf("image/jpeg"), ClipData.Item(uris.first()))
        for (i in 1 until uris.size) {
            clipData.addItem(ClipData.Item(uris[i]))
        }
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
