package com.majortomman.school.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.majortomman.school.BuildConfig
import com.majortomman.school.ai.OpenAiCompatibleClient
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.BackgroundImportResult
import com.majortomman.school.data.BackgroundMode
import com.majortomman.school.data.BackgroundPreset
import com.majortomman.school.data.DisplayPreferences
import com.majortomman.school.data.DisplaySettings
import com.majortomman.school.network.AppProxy
import com.majortomman.school.network.AppProxySettings
import com.majortomman.school.update.UpdateCoordinator
import com.majortomman.school.update.UpdateState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private val SettingsBlack = Color.Transparent
private val SettingsWhite = Color(0xFFF5F7FA)
private val SettingsBlue = Color(0xFF2D7BFF)
private val SettingsRed = Color(0xFFFF453A)
private val SettingsYellow = Color(0xFFFFCC00)
private val SettingsMuted = SettingsWhite.copy(alpha = 0.46f)
private val SettingsLine = SettingsWhite.copy(alpha = 0.13f)

private enum class SettingsPage(val label: String) {
    PROXY("代理"),
    UPDATE("应用"),
    COURSE("课程"),
    DISPLAY("显示"),
    AI("AI"),
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MaterialSettingsScreen(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit,
    onOpenSubjects: () -> Unit,
    onClearProgress: () -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.PROXY.name) }
    var endpoint by rememberSaveable { mutableStateOf(settings.endpoint) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var connectionStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var isTesting by rememberSaveable { mutableStateOf(false) }
    var confirmClearProgress by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val updateCoordinator = remember(appContext) { UpdateCoordinator.get(appContext) }
    val updateState by updateCoordinator.state.collectAsState()
    val updateSettings by updateCoordinator.settings.collectAsState()
    val proxySettings by AppProxy.settings.collectAsState()
    val displaySettings by DisplayPreferences.state.collectAsState(initial = DisplaySettings())
    var proxyUrl by rememberSaveable { mutableStateOf(proxySettings.proxyUrl) }
    var useForUpdates by rememberSaveable { mutableStateOf(proxySettings.useForUpdates) }
    var useForAi by rememberSaveable { mutableStateOf(proxySettings.useForAi) }
    var proxyStatus by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(settings) {
        endpoint = settings.endpoint
        model = settings.model
        apiKey = settings.apiKey
    }
    LaunchedEffect(proxySettings) {
        proxyUrl = proxySettings.proxyUrl
        useForUpdates = proxySettings.useForUpdates
        useForAi = proxySettings.useForAi
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        SchoolPageTitle("设置")
        Spacer(Modifier.height(20.dp))
        val selectedPage = SettingsPage.valueOf(pageName)
        SchoolScrollableTabs(
            labels = SettingsPage.entries.map { it.label },
            selectedIndex = selectedPage.ordinal,
            onSelect = { index -> pageName = SettingsPage.entries[index].name },
            selectedColor = SettingsWhite,
            mutedColor = SettingsMuted,
            indicatorColor = SettingsBlue,
        )
        Spacer(Modifier.height(30.dp))

        AnimatedContent(
            targetState = selectedPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settingsPages",
        ) { page ->
            when (page) {
                SettingsPage.PROXY -> ProxySettingsPage(
                    proxyUrl = proxyUrl,
                    onProxyUrlChange = {
                        proxyUrl = it
                        proxyStatus = null
                    },
                    useForUpdates = useForUpdates,
                    onToggleUpdates = {
                        useForUpdates = !useForUpdates
                        proxyStatus = null
                    },
                    useForAi = useForAi,
                    onToggleAi = {
                        useForAi = !useForAi
                        proxyStatus = null
                    },
                    proxyStatus = proxyStatus,
                    onSaveProxy = {
                        runCatching {
                            AppProxy.save(
                                appContext,
                                AppProxySettings(proxyUrl = proxyUrl, useForUpdates = useForUpdates, useForAi = useForAi),
                            )
                        }.fold(
                            onSuccess = { proxyStatus = "代理设置已保存。" },
                            onFailure = { proxyStatus = "保存失败：${it.message ?: "代理地址无效"}" },
                        )
                    },
                )

                SettingsPage.UPDATE -> UpdateSettingsPage(
                    updateState = updateState,
                    autoCheck = updateSettings.autoCheck,
                    wifiOnly = updateSettings.wifiOnly,
                    lastCheckedAt = updateSettings.lastCheckedAt,
                    updateUsesProxy = proxySettings.useForUpdates,
                    onToggleAutoCheck = { updateCoordinator.setAutoCheck(!updateSettings.autoCheck) },
                    onToggleWifiOnly = { updateCoordinator.setWifiOnly(!updateSettings.wifiOnly) },
                    onCheckUpdate = { updateCoordinator.checkNow(force = true) },
                    onShowUpdateStatus = updateCoordinator::showDialog,
                )

                SettingsPage.COURSE -> CourseStorageSettingsPage()
                SettingsPage.DISPLAY -> DisplaySettingsPage(settings = displaySettings)
                SettingsPage.AI -> AiSettingsPage(
                    endpoint = endpoint,
                    onEndpointChange = {
                        endpoint = it
                        connectionStatus = null
                    },
                    model = model,
                    onModelChange = {
                        model = it
                        connectionStatus = null
                    },
                    apiKey = apiKey,
                    onApiKeyChange = { apiKey = it },
                    connectionStatus = connectionStatus,
                    isTesting = isTesting,
                    aiUsesProxy = proxySettings.useForAi,
                    onTest = {
                        isTesting = true
                        connectionStatus = "正在连接…"
                        val updated = AiSettings(endpoint.trim(), model.trim(), apiKey.trim())
                        scope.launch {
                            connectionStatus = OpenAiCompatibleClient(updated).testConnection().fold(
                                onSuccess = { it },
                                onFailure = { "连接失败：${it.message ?: it::class.java.simpleName}" },
                            )
                            isTesting = false
                        }
                    },
                    onSaveAi = {
                        onSave(AiSettings(endpoint.trim(), model.trim(), apiKey.trim()))
                        connectionStatus = "已保存"
                    },
                    confirmClearProgress = confirmClearProgress,
                    onBeginClear = { confirmClearProgress = true },
                    onCancelClear = { confirmClearProgress = false },
                    onConfirmClear = {
                        onClearProgress()
                        confirmClearProgress = false
                    },
                )
            }
        }
        Spacer(Modifier.height(SchoolUiMetrics.pageBottom))
    }
}

@Composable
private fun ProxySettingsPage(
    proxyUrl: String,
    onProxyUrlChange: (String) -> Unit,
    useForUpdates: Boolean,
    onToggleUpdates: () -> Unit,
    useForAi: Boolean,
    onToggleAi: () -> Unit,
    proxyStatus: String?,
    onSaveProxy: () -> Unit,
) {
    Column {
        SettingsSectionTitle("代理")
        SettingsInput(
            label = "代理地址",
            value = proxyUrl,
            onValueChange = onProxyUrlChange,
            keyboardType = KeyboardType.Uri,
            placeholder = "http://192.168.1.2:7890",
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "支持 HTTP、HTTPS、SOCKS 和 SOCKS5。未写协议时按 HTTP 处理；未写端口时 HTTP 使用 8080，SOCKS 使用 1080。",
            color = SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SettingsToggleRow("版本与课程更新走代理", useForUpdates, onToggleUpdates)
        SettingsToggleRow("AI 请求走代理", useForAi, onToggleAi)
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SettingsAction("保存代理", SettingsBlue, onSaveProxy)
        }
        AnimatedVisibility(visible = proxyStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SettingsInlineNotice(
                color = if (proxyStatus.orEmpty().startsWith("保存失败")) SettingsRed else SettingsBlue,
                label = "代理状态",
                body = proxyStatus.orEmpty(),
            )
        }
    }
}

@Composable
private fun UpdateSettingsPage(
    updateState: UpdateState,
    autoCheck: Boolean,
    wifiOnly: Boolean,
    lastCheckedAt: Long,
    updateUsesProxy: Boolean,
    onToggleAutoCheck: () -> Unit,
    onToggleWifiOnly: () -> Unit,
    onCheckUpdate: () -> Unit,
    onShowUpdateStatus: () -> Unit,
) {
    Column {
        SettingsSectionTitle("应用更新")
        Text("${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）", color = SettingsWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("开发通道 · GitHub dev-latest", color = SettingsMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(7.dp))
        Text(
            if (updateUsesProxy) "更新清单、签名与 APK 下载：通过代理" else "更新清单、签名与 APK 下载：直接连接",
            color = if (updateUsesProxy) SettingsBlue else SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SettingsToggleRow("自动检查更新", autoCheck, onToggleAutoCheck)
        SettingsToggleRow("仅在 Wi-Fi 下载", wifiOnly, onToggleWifiOnly)
        Spacer(Modifier.height(12.dp))
        Text("上次检查：${formatUpdateCheckTime(lastCheckedAt)}", color = SettingsMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SettingsAction("检查更新", if (updateState is UpdateState.Checking) SettingsMuted else SettingsBlue, onCheckUpdate, updateState !is UpdateState.Checking)
            if (updateState is UpdateState.Available || updateState is UpdateState.Downloading || updateState is UpdateState.Ready || updateState is UpdateState.Error || updateState is UpdateState.UpToDate) {
                SettingsAction("查看状态", SettingsWhite.copy(alpha = 0.72f), onShowUpdateStatus)
            }
        }
        SettingsInlineNotice(
            color = when (updateState) {
                is UpdateState.Error -> SettingsRed
                is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Ready -> SettingsBlue
                else -> SettingsYellow
            },
            label = "更新状态",
            body = updateState.settingsDescription(),
        )
    }
}

@Composable
private fun AiSettingsPage(
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    connectionStatus: String?,
    isTesting: Boolean,
    aiUsesProxy: Boolean,
    onTest: () -> Unit,
    onSaveAi: () -> Unit,
    confirmClearProgress: Boolean,
    onBeginClear: () -> Unit,
    onCancelClear: () -> Unit,
    onConfirmClear: () -> Unit,
) {
    Column {
        SettingsSectionTitle("AI")
        Text(
            if (aiUsesProxy) "当前 AI 请求通过代理连接。" else "当前 AI 请求直接连接，不使用代理。",
            color = if (aiUsesProxy) SettingsBlue else SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SettingsInput("接口地址", endpoint, onEndpointChange, KeyboardType.Uri, placeholder = "http://192.168.1.2:7777/v1")
        Spacer(Modifier.height(20.dp))
        SettingsInput("模型", model, onModelChange, placeholder = "gemma-4")
        Spacer(Modifier.height(20.dp))
        SettingsInput("API Key", apiKey, onApiKeyChange, visualTransformation = PasswordVisualTransformation(), placeholder = "可留空")
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SettingsAction("测试连接", if (isTesting) SettingsMuted else SettingsWhite.copy(alpha = 0.68f), onTest, !isTesting && endpoint.isNotBlank())
            SettingsAction("保存", SettingsBlue, onSaveAi, endpoint.isNotBlank() && model.isNotBlank())
        }
        AnimatedVisibility(visible = connectionStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SettingsInlineNotice(
                color = if (connectionStatus.orEmpty().startsWith("连接失败")) SettingsRed else SettingsBlue,
                label = "AI 状态",
                body = connectionStatus.orEmpty(),
            )
        }

        Spacer(Modifier.height(42.dp))
        SettingsSectionTitle("学习数据")
        Text("答案、反馈、复习计划和掌握状态保存在本机。", color = SettingsWhite.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(targetState = confirmClearProgress, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "clearLearningData") { confirming ->
            if (!confirming) {
                SettingsAction("清空学习记录", SettingsRed, onBeginClear)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SettingsAction("取消", SettingsMuted, onCancelClear)
                    SettingsAction("确认清空", SettingsRed, onConfirmClear)
                }
            }
        }
    }
}

@Composable
private fun DisplaySettingsPage(settings: DisplaySettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        importStatus = "正在校验并导入背景图片…"
        scope.launch {
            when (val result = DisplayPreferences.importCustomBackground(context, uri)) {
                is BackgroundImportResult.Success -> importStatus = "背景图片已导入并应用。"
                is BackgroundImportResult.Failure -> importStatus = "导入失败：${result.message}。已保留原来的背景。"
            }
            importing = false
        }
    }
    val textOptions = listOf("小" to 0.90f, "标准" to 1.00f, "大" to 1.15f, "特大" to 1.30f, "超大" to 1.50f)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSectionTitle("文字大小")
        Text(
            "课程正文、题目、解析和数学可视化标签会同时调整。界面使用自适应高度，放大后不会压缩成竖排。",
            color = SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        textOptions.forEach { (label, scale) ->
            val selected = kotlin.math.abs(settings.textScale - scale) < 0.01f
            SchoolSettingRow(
                label = label,
                value = "${(scale * 100).toInt()}%",
                selected = selected,
                onClick = { DisplayPreferences.setTextScale(context, scale) },
                valueColor = if (selected) SettingsBlue else SettingsMuted,
            )
        }
        Text("预览：负半轴　−3　0　+3　正半轴　答案与解释", color = SettingsWhite, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(20.dp))
        SettingsSectionTitle("背景")
        Text(
            "默认保持纯黑风格。预定义颜色和自定义图片只改变底层背景，界面会自动保留暗色遮罩保证文字可读。",
            color = SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        BackgroundPreset.entries.forEach { preset ->
            val selected = settings.backgroundMode == BackgroundMode.PRESET && settings.backgroundPreset == preset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SchoolUiMetrics.settingsRowMinHeight)
                    .clickable {
                        DisplayPreferences.setBackgroundPreset(context, preset)
                        importStatus = "已切换为${preset.label}。"
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(Color(preset.argb), CircleShape))
                    Text(
                        preset.label,
                        modifier = Modifier.weight(1f),
                        color = SettingsWhite.copy(alpha = if (selected) 1f else 0.75f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                    )
                }
                if (selected) {
                    Text("使用中", color = SettingsBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            SchoolDivider(color = SettingsLine)
        }

        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (settings.customImagePath != null) {
                SettingsAction(
                    label = if (settings.backgroundMode == BackgroundMode.CUSTOM) "自定义背景使用中" else "使用已导入背景",
                    color = if (settings.backgroundMode == BackgroundMode.CUSTOM) SettingsYellow else SettingsWhite.copy(alpha = 0.72f),
                    onClick = {
                        if (DisplayPreferences.useExistingCustomBackground(context)) importStatus = "已切换到已导入的自定义背景。"
                    },
                    enabled = !importing,
                )
            }
            SettingsAction(
                label = if (importing) "正在导入…" else "导入自定义图片",
                color = if (importing) SettingsMuted else SettingsBlue,
                onClick = { imagePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                enabled = !importing,
            )
        }
        Text(
            "支持 JPEG、PNG、WebP；至少短边 720px、长边 1280px，不超过 20 MB 和 3200 万像素。格式、尺寸或解码失败时不会替换当前背景。",
            color = SettingsMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        AnimatedVisibility(visible = importStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            val failed = importStatus.orEmpty().startsWith("导入失败")
            SettingsInlineNotice(color = if (failed) SettingsRed else SettingsBlue, label = "背景状态", body = importStatus.orEmpty())
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    SchoolSettingRow(
        label = label,
        value = if (enabled) "开启" else "关闭",
        selected = enabled,
        onClick = onClick,
        valueColor = if (enabled) SettingsBlue else SettingsMuted,
    )
}

@Composable
private fun SettingsInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    placeholder: String = "输入…",
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = SettingsMuted, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = SchoolUiMetrics.textInputMinHeight).padding(vertical = 12.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = SettingsWhite),
            cursorBrush = SolidColor(SettingsBlue),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            singleLine = true,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = SettingsWhite.copy(alpha = 0.2f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        SchoolDivider(color = SettingsLine)
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    SchoolSectionLabel(text = text, modifier = Modifier.padding(bottom = 16.dp), color = SettingsYellow)
}

@Composable
private fun SettingsAction(label: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text = label,
        modifier = Modifier.heightIn(min = SchoolUiMetrics.minTouchHeight).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        color = if (enabled) color else SettingsMuted,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun SettingsInlineNotice(color: Color, label: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(body, color = SettingsWhite.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun UpdateState.settingsDescription(): String = when (this) {
    UpdateState.Idle -> "尚未发现可用更新。"
    UpdateState.Checking -> "正在获取并验证更新清单。"
    is UpdateState.UpToDate -> "当前版本已是最新版本。"
    is UpdateState.Available -> "发现 ${manifest.versionName}，可查看变更并下载。"
    is UpdateState.Downloading -> "正在下载 ${manifest.versionName}：$progress%。"
    is UpdateState.Ready -> "${manifest.versionName} 已下载并通过校验，可立即安装。"
    is UpdateState.Error -> message
}

private fun formatUpdateCheckTime(timestamp: Long): String {
    if (timestamp <= 0L) return "尚未检查"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
}
