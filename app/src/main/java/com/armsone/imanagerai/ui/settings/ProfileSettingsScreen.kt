package com.armsone.imanagerai.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armsone.imanagerai.design.BrandTheme
import com.armsone.imanagerai.design.IconWell
import com.armsone.imanagerai.design.IconWellVariant
import com.armsone.imanagerai.design.LocalAppAppearance
import com.armsone.imanagerai.design.StarSegmentedControl
import com.armsone.imanagerai.design.StarSwitch
import com.armsone.imanagerai.model.AppAppearance
import com.armsone.imanagerai.model.CreatorProfileStore
import com.armsone.imanagerai.service.DirectAIProvider
import com.armsone.imanagerai.ui.externalai.ExternalAIAuthStatus
import com.armsone.imanagerai.ui.externalai.ExternalAIAuthState
import com.armsone.imanagerai.ui.externalai.ExternalAISurface
import com.armsone.imanagerai.ui.externalai.ExternalAISurfaceMode
import com.armsone.imanagerai.update.DirectUpdateManager
import com.armsone.imanagerai.update.DirectUpdateSettings
import kotlinx.coroutines.launch

/**
 * "설정" 화면.
 * 1. 외부 로그인 관리 (브라우저 보기 토글, Gemini, ChatGPT, Claude)
 * 2. 앱 업데이트 관리
 * 3. 테마 관리 (BK / 클래식 외형 전환)
 */
@Composable
fun ProfileSettingsScreen(store: CreatorProfileStore) {
    val context = LocalContext.current
    val updateManager = remember(context) { DirectUpdateManager.get(context) }
    val appearance by store.appearance.collectAsStateWithLifecycle()
    val showsExternalAIBrowser by store.showsExternalAIBrowser.collectAsStateWithLifecycle()
    val automationEnabled by store.automationEnabled.collectAsStateWithLifecycle()

    var activeLoginProvider by remember { mutableStateOf<DirectAIProvider?>(null) }
    var showsLogoutConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loginProviders = remember {
        listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
    }
    var authStates by remember {
        mutableStateOf(loginProviders.associateWith { ExternalAIAuthState.CHECKING })
    }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        loginProviders.forEach { provider ->
            launch {
                val state = ExternalAIAuthStatus.probe(context, provider)
                authStates = authStates + (provider to state)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandTheme.settingsBackground(appearance))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. 자동화 설정
                SettingsSection(
                    header = "자동화",
                    icon = Icons.Filled.AutoAwesome,
                    footer = "끄면 앱을 열거나 15초 뒤 돌아와도 자동 미디어 선택이 시작되지 않아요. 다른 앱에서 직접 공유한 사진과 카메라 퀵 액션은 계속 사용할 수 있어요.",
                    appearance = appearance,
                    variant = IconWellVariant.ACCENT
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "켜면 앱을 열 때 미디어를 고른 뒤 무작위 AI로 보내는 자동화가 시작됩니다"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "자동화 사용",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelPrimary(appearance)
                            )
                            Text(
                                "켜면 앱을 열 때 미디어를 고른 뒤 무작위 AI로 보내는 자동화가 시작됩니다",
                                fontSize = 12.sp,
                                color = BrandTheme.labelSecondary(appearance)
                            )
                        }
                        StarSwitch(
                            checked = automationEnabled,
                            appearance = appearance,
                            onCheckedChange = { checked ->
                                store.setAutomationEnabled(checked)
                            },
                            modifier = Modifier
                                .testTag("settings.automationEnabled")
                                .semantics {
                                    contentDescription = "켜면 앱을 열 때 미디어를 고른 뒤 무작위 AI로 보내는 자동화가 시작됩니다"
                                }
                        )
                    }
                }

                // 2. 외부 로그인 관리
                SettingsSection(
                    header = "외부 로그인 관리",
                    icon = Icons.Filled.AccountCircle,
                    footer = "서비스 행을 누르면 공식 로그인 화면을 열 수 있어요. 모든 외부 AI 로그아웃은 이 앱의 Gemini, ChatGPT, Claude 웹 세션을 함께 지워요. Stargram는 비밀번호를 보거나 저장하지 않아요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    // 브라우저 보기 토글
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "브라우저 보기",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelPrimary(appearance)
                            )
                            Text(
                                "AI가 답하는 화면을 처음부터 보여줘요.",
                                fontSize = 12.sp,
                                color = BrandTheme.labelSecondary(appearance)
                            )
                        }
                        StarSwitch(
                            checked = showsExternalAIBrowser,
                            appearance = appearance,
                            onCheckedChange = { checked ->
                                store.setShowsExternalAIBrowser(checked)
                            },
                            modifier = Modifier.testTag("settings.showsExternalAIBrowser")
                        )
                    }

                    HorizontalDivider(color = BrandTheme.divider(appearance))

                    // 로그인 제공사 행들
                    val providers = listOf(DirectAIProvider.GEMINI, DirectAIProvider.OPEN_AI, DirectAIProvider.CLAUDE)
                    providers.forEachIndexed { index, provider ->
                        if (index > 0) {
                            HorizontalDivider(color = BrandTheme.divider(appearance))
                        }
                        val state = authStates[provider] ?: ExternalAIAuthState.CHECKING
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ExternalAIAuthStatus.markLoginFlowStarted(context, provider)
                                    activeLoginProvider = provider
                                }
                                .padding(vertical = 12.dp)
                                .testTag("settings.loginRow.${provider.rawValue}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                provider.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTheme.labelPrimary(appearance),
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("settings.login.${provider.rawValue}")
                            ) {
                                if (state == ExternalAIAuthState.LOGGED_IN) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    state.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (state == ExternalAIAuthState.REQUIRES_LOGIN) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when (state) {
                                        ExternalAIAuthState.REQUIRES_LOGIN -> BrandTheme.accent
                                        ExternalAIAuthState.LOGGED_IN -> Color(0xFF34C759)
                                        ExternalAIAuthState.CHECKING,
                                        ExternalAIAuthState.UNKNOWN -> BrandTheme.labelSecondary(appearance)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BrandTheme.divider(appearance))
                    TextButton(
                        onClick = {
                            scope.launch {
                                val export = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    runCatching {
                                        val latest = com.armsone.imanagerai.ui.externalai.ExternalAIDiagnosticsStore(context).latestFile()
                                            ?: return@runCatching null
                                        val folder = java.io.File(context.cacheDir, "share")
                                        check(folder.isDirectory || folder.mkdirs())
                                        latest.copyTo(java.io.File(folder, "aibi-diagnostics.json"), overwrite = true)
                                    }.getOrNull()
                                }
                                if (export == null) {
                                    android.widget.Toast.makeText(context, "공유할 진단 로그가 없어요. AI를 한 번 실행해 주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    runCatching {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", export)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            clipData = android.content.ClipData.newRawUri("AIBI diagnostics", uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "진단 로그 공유"))
                                    }.onFailure {
                                        android.widget.Toast.makeText(context, "진단 로그 공유를 열지 못했어요. 다시 시도해 주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("settings.aibi.shareDiagnostics")
                    ) { Text("최근 AI 진단 로그 공유") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                authStates = loginProviders.associateWith { ExternalAIAuthState.CHECKING }
                                refreshTrigger += 1
                            },
                            modifier = Modifier.testTag("settings.login.refresh")
                        ) {
                            Text("상태 다시 확인")
                        }
                        TextButton(
                            onClick = { showsLogoutConfirmation = true },
                            modifier = Modifier.testTag("settings.login.logoutAll")
                        ) {
                            Text("모두 로그아웃", color = BrandTheme.accent)
                        }
                    }
                }

                // 2. 앱 업데이트 관리
                SettingsSection(
                    header = "앱 업데이트 관리",
                    icon = Icons.Outlined.ArrowCircleDown,
                    footer = "최신 버전을 확인하고 설치할 수 있어요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    DirectUpdateSettings(updateManager)
                }

                // 3. 테마 관리 (BK / 클래식 / 인터스텔라 외형 전환)
                SettingsSection(
                    header = "테마 관리",
                    icon = Icons.Outlined.Diamond,
                    footer = "BK는 기본 모습, 클래식은 예전 모습, 인터스텔라는 은빛과 금빛이 도는 어두운 모습이에요.",
                    appearance = appearance,
                    variant = IconWellVariant.CARBON
                ) {
                    StarSegmentedControl(
                        options = listOf(AppAppearance.BK.title, AppAppearance.CLASSIC.title, AppAppearance.INTERSTELLAR.title),
                        selectedIndex = when (appearance) {
                            AppAppearance.BK -> 0
                            AppAppearance.CLASSIC -> 1
                            AppAppearance.INTERSTELLAR -> 2
                        },
                        appearance = appearance,
                        onSelect = { index ->
                            store.setAppearance(
                                when (index) {
                                    0 -> AppAppearance.BK
                                    1 -> AppAppearance.CLASSIC
                                    else -> AppAppearance.INTERSTELLAR
                                }
                            )
                        },
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .testTag("settings.appearance")
                    )
                }
            }
        }
    }

    val loginProvider = activeLoginProvider
    if (loginProvider != null) {
        ExternalAISurface(
            provider = loginProvider,
            mode = ExternalAISurfaceMode.LOGIN,
            prompt = "",
            onClose = {
                activeLoginProvider = null
                refreshTrigger += 1
            },
            onLoginSuccess = {
                authStates = authStates + (loginProvider to ExternalAIAuthState.LOGGED_IN)
                refreshTrigger += 1
            },
            appearance = appearance
        )
    }


    if (showsLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showsLogoutConfirmation = false },
            title = { Text("모든 외부 AI에서 로그아웃할까요?") },
            text = { Text("이 앱에 저장된 Gemini, ChatGPT, Claude 웹 세션을 지웁니다. 다시 사용하려면 각 서비스 행을 눌러 로그인하세요.") },
            dismissButton = {
                TextButton(onClick = { showsLogoutConfirmation = false }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showsLogoutConfirmation = false
                        scope.launch {
                            ExternalAIAuthStatus.clearAllSessions(context)
                            authStates = loginProviders.associateWith { ExternalAIAuthState.REQUIRES_LOGIN }
                        }
                    },
                    modifier = Modifier.testTag("settings.login.logoutAll.confirm")
                ) {
                    Text("로그아웃", color = BrandTheme.accent)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    header: String? = null,
    icon: ImageVector? = null,
    footer: String? = null,
    appearance: AppAppearance = LocalAppAppearance.current,
    variant: IconWellVariant = IconWellVariant.CARBON,
    content: @Composable () -> Unit
) {
    val isBk = appearance == AppAppearance.BK
    Column {
        if (header != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    IconWell(
                        icon = icon,
                        appearance = appearance,
                        variant = variant,
                        size = 22.dp,
                        iconSize = if (isBk) 13.dp else 16.dp
                    )
                }
                Text(
                    header,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTheme.labelSecondary(appearance)
                )
            }
        }
        val shape = RoundedCornerShape(if (isBk) 14.dp else 10.dp)
        val bg = BrandTheme.settingsSectionBackground(appearance)
        val border = if (isBk) BorderStroke(1.dp, Color(0xFFE2E6EC)) else null
        val shadowElevation = if (isBk) 3.dp else 0.dp

        Column(
            Modifier
                .fillMaxWidth()
                .shadow(shadowElevation, shape, ambientColor = Color(0x08141518), spotColor = Color(0x0C141518))
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .background(bg, shape)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            content()
        }

        if (footer != null) {
            Text(
                footer,
                fontSize = 13.sp,
                color = BrandTheme.labelSecondary(appearance),
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp)
            )
        }
    }
}
