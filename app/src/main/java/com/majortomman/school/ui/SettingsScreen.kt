package com.majortomman.school.ui

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
import com.majortomman.school.data.DisplayPreferences
import com.majortomman.school.data.DisplaySettings
import com.majortomman.school.data.ThemeMode
import com.majortomman.school.network.AppProxy
import com.majortomman.school.network.AppProxySettings
import com.majortomman.school.update.UpdateCoordinator
import com.majortomman.school.update.UpdateState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private enum class SettingsPage(val label: String) {
    APP("应用"),
    COURSE("课程"),
    DISPLAY("显示"),
    NETWORK("网络"),
    AI("AI"),
    DATA("数据"),
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit,
    onOpenSubjects: () -> Unit,
    onClearProgress: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.APP.name) }
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SchoolUiMetrics.pageHorizontal, vertical = SchoolUiMetrics.pageTop),
    ) {
        if (onBack != null) {
            Text(
                "‹ 我的",
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
        }
        SchoolPageTitle("设置", eyebrow = "SCHOOL / SETTINGS")
        Spacer(Modifier.height(20.dp))
        val selectedPage = SettingsPage.valueOf(pageName)
        SchoolScrollableTabs(
            labels = SettingsPage.entries.map { it.label },
            selectedIndex = selectedPage.ordinal,
            onSelect = { index -> pageName = SettingsPage.entries[index].name },
            selectedColor = MaterialTheme.colorScheme.onBackground,
            mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(30.dp))

        AnimatedContent(
            targetState = selectedPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settingsPages",
        ) { page ->
            when (page) {
                SettingsPage.NETWORK -> ProxySettingsPage(
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

                SettingsPage.APP -> UpdateSettingsPage(
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
                )

                SettingsPage.DATA -> LearningDataSettingsPage(
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SettingsToggleRow("版本与课程更新走代理", useForUpdates, onToggleUpdates)
        SettingsToggleRow("AI 请求走代理", useForAi, onToggleAi)
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SettingsAction("保存代理", MaterialTheme.colorScheme.primary, onSaveProxy)
        }
        AnimatedVisibility(visible = proxyStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SettingsInlineNotice(
                color = if (proxyStatus.orEmpty().startsWith("保存失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
        Text("${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("开发通道 · GitHub dev-latest", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(7.dp))
        Text(
            if (updateUsesProxy) "更新清单、签名与 APK 下载：通过代理" else "更新清单、签名与 APK 下载：直接连接",
            color = if (updateUsesProxy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SettingsToggleRow("自动检查更新", autoCheck, onToggleAutoCheck)
        SettingsToggleRow("仅在 Wi-Fi 下载", wifiOnly, onToggleWifiOnly)
        Spacer(Modifier.height(12.dp))
        Text("上次检查：${formatUpdateCheckTime(lastCheckedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SettingsAction("检查更新", if (updateState is UpdateState.Checking) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, onCheckUpdate, updateState !is UpdateState.Checking)
            if (updateState is UpdateState.Available || updateState is UpdateState.Downloading || updateState is UpdateState.Ready || updateState is UpdateState.Error || updateState is UpdateState.UpToDate) {
                SettingsAction("查看状态", MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), onShowUpdateStatus)
            }
        }
        SettingsInlineNotice(
            color = when (updateState) {
                is UpdateState.Error -> MaterialTheme.colorScheme.error
                is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Ready -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
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
) {
    Column {
        SettingsSectionTitle("AI")
        Text(
            if (aiUsesProxy) "当前 AI 请求通过代理连接。" else "当前 AI 请求直接连接，不使用代理。",
            color = if (aiUsesProxy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            SettingsAction("测试连接", if (isTesting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f), onTest, !isTesting && endpoint.isNotBlank())
            SettingsAction("保存", MaterialTheme.colorScheme.primary, onSaveAi, endpoint.isNotBlank() && model.isNotBlank())
        }
        AnimatedVisibility(visible = connectionStatus != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            SettingsInlineNotice(
                color = if (connectionStatus.orEmpty().startsWith("连接失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "AI 状态",
                body = connectionStatus.orEmpty(),
            )
        }

    }
}

@Composable
private fun LearningDataSettingsPage(
    confirmClearProgress: Boolean,
    onBeginClear: () -> Unit,
    onCancelClear: () -> Unit,
    onConfirmClear: () -> Unit,
) {
    Column {
        SettingsSectionTitle("学习数据")
        Text(
            "课程进度、答题记录、学习证据、掌握度和复习队列都保存在本机；删除课程资源不会自动删除这些记录。",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "课程结构、题库和教材文件请在“课程”页管理。这里仅处理学习产生的数据。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = confirmClearProgress,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "clearLearningData",
        ) { confirming ->
            if (!confirming) {
                SettingsAction("清空全部学习记录", MaterialTheme.colorScheme.error, onBeginClear)
            } else {
                Column {
                    Text(
                        "这会清空课程进度、练习会话、学习证据与掌握状态，但不会删除已下载的课程、题库或教材。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SettingsAction("取消", MaterialTheme.colorScheme.onSurfaceVariant, onCancelClear)
                        SettingsAction("确认清空", MaterialTheme.colorScheme.error, onConfirmClear)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySettingsPage(settings: DisplaySettings) {
    val context = LocalContext.current
    val textOptions = listOf("小" to 0.90f, "标准" to 1.00f, "大" to 1.15f, "特大" to 1.30f, "超大" to 1.50f)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSectionTitle("显示模式")
        Text(
            "School 的日间与夜间模式共享同一套排版、色彩角色和组件结构；只切换主题，不切换设计语言。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        ThemeMode.entries.forEach { mode ->
            SchoolSettingRow(
                label = mode.label,
                value = if (settings.themeMode == mode) "使用中" else "",
                selected = settings.themeMode == mode,
                onClick = { DisplayPreferences.setThemeMode(context, mode) },
                valueColor = if (settings.themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        SettingsSectionTitle("文字大小")
        Text(
            "课程正文、题目、解析和数学可视化标签会同时调整。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        textOptions.forEach { (label, scale) ->
            val selected = kotlin.math.abs(settings.textScale - scale) < 0.01f
            SchoolSettingRow(
                label = label,
                value = "${(scale * 100).toInt()}%",
                selected = selected,
                onClick = { DisplayPreferences.setTextScale(context, scale) },
                valueColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("预览：负半轴　−3　0　+3　正半轴　答案与解释", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsToggleRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    SchoolSettingRow(
        label = label,
        value = if (enabled) "开启" else "关闭",
        selected = enabled,
        onClick = onClick,
        valueColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = SchoolUiMetrics.textInputMinHeight).padding(vertical = 12.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            singleLine = true,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        SchoolDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    SchoolSectionLabel(text = text, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun SettingsAction(label: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text = label,
        modifier = Modifier.heightIn(min = SchoolUiMetrics.minTouchHeight).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
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
        Text(body, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
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
