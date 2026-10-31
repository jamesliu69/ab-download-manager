@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)

package com.abdownloadmanager.desktop.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.abdownloadmanager.desktop.pages.settings.DesktopSettings
import com.abdownloadmanager.desktop.storage.AppSettingsStorage
import com.abdownloadmanager.shared.storage.appsettings.DesktopSettingsSchema
import com.abdownloadmanager.shared.ui.theme.ABDownloaderTheme
import com.abdownloadmanager.shared.ui.theme.DefaultThemes
import com.abdownloadmanager.shared.util.ui.icon.MyIcons
import com.abdownloadmanager.shared.util.ui.widget.MyIcon
import com.abdownloadmanager.shared.util.schemakt.initializeForABDM
import io.github.amir1376.schemakt.Schema
import ir.amirab.util.compose.IconSource
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.action.MenuItem
import ir.amirab.util.compose.localizationmanager.LocaleLanguageDirection
import ir.amirab.util.config.JsonObjectToMap
import ir.amirab.util.config.datastore.createSchemaBasedDatastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/** 預期的桌面字級倍率，用於核對外觀設定選項。 */
private val fontScales = listOf(0.8f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f)

/** 離屏驗證桌面設定寫入、舊資料回退及工具列字級更新，輸出截圖與測試 JSON。 */
fun main(args: Array<String>) {
    check(java.awt.GraphicsEnvironment.isHeadless()) { "Font scale verification must stay headless" }
    Schema.initializeForABDM()
    val reportDir = File(args.single()).apply { mkdirs() }
    val settingsFile = File(reportDir, "font-scale-settings-${UUID.randomUUID()}.json")
    val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val koin = startKoin {
        modules(module { single<CoroutineScope> { storageScope } })
    }
    try {
        val dataStore = createSchemaBasedDatastore(settingsFile, Json, DesktopSettingsSchema)
        val appSettings = AppSettingsStorage(dataStore)
        check(runBlocking { dataStore.data.first() }.fontScale == 1f) { "Missing fontScale must default to 100%" }
        val fontSizeConfig = DesktopSettings.fontSizeConfig(appSettings)
        check(fontSizeConfig.possibleValues == fontScales) { "Font size options do not match the supported scales" }
        appSettings.theme.value = "font-scale-smoke"
        appSettings.apiAuthKey.value = "font-scale-smoke-only"
        check(fontSizeConfig.set(1.5f)) { "The real font size configurable rejected 150%" }

        val (firstPersisted, persistedObject) = runBlocking {
            awaitPersistedSettings(dataStore, settingsFile) {
                it.fontScale == 1.5f && it.theme == "font-scale-smoke"
            }
        }
        println("PASS AppSettingsStorage and EnumConfigurable persisted 150% to ${settingsFile.name}")

        verifySchemaFallbacks(persistedObject, firstPersisted)
        // 後續倍率只驗即時重組，停止重複寫入 smoke 設定檔。
        storageScope.cancel()
        verifyFontScales(appSettings, fontSizeConfig, reportDir)
        verifyIconSize(reportDir)
        verifyNarrowToolbar(appSettings, fontSizeConfig)
        check(appSettings.fontScale.value == 2f) { "The real settings StateFlow did not retain the 200% selection" }
        println("PASS live 200% selection; reports=$reportDir")
    } finally {
        storageScope.cancel()
        koin.close()
    }
}

/** 等候 DataStore 值與獨立讀取的磁碟 JSON 相符，涵蓋 debounced 寫檔時序。 */
private suspend fun awaitPersistedSettings(
    dataStore: androidx.datastore.core.DataStore<com.abdownloadmanager.shared.storage.appsettings.AppSettingsModel>,
    file: File,
    expected: (com.abdownloadmanager.shared.storage.appsettings.AppSettingsModel) -> Boolean,
): Pair<com.abdownloadmanager.shared.storage.appsettings.AppSettingsModel, JsonObject> = withTimeout(10_000) {
    val stored = dataStore.data.first(expected)
    while (true) {
        val readback = runCatching {
            val jsonObject = Json.parseToJsonElement(file.readText()).jsonObject
            val model = DesktopSettingsSchema.parse(JsonObjectToMap().transformJsonObject(jsonObject)).getOrThrow()
            model to jsonObject
        }.getOrNull()
        if (readback?.first == stored) return@withTimeout stored to readback.second
        delay(50)
    }
    error("Timed out waiting for independent settings JSON readback")
}

/** 確認缺少、錯型、低於下界及超過上界的 fontScale 只回退該欄位。 */
private fun verifySchemaFallbacks(source: JsonObject, persisted: com.abdownloadmanager.shared.storage.appsettings.AppSettingsModel) {
    val invalidObjects = listOf(
        "missing" to JsonObject(source - "fontScale"),
        "wrong-type" to JsonObject(source + ("fontScale" to JsonPrimitive("large"))),
        "below-range" to JsonObject(source + ("fontScale" to JsonPrimitive(0.7))),
        "above-range" to JsonObject(source + ("fontScale" to JsonPrimitive(2.1))),
    )
    for ((name, jsonObject) in invalidObjects) {
        val parsed = DesktopSettingsSchema.parse(JsonObjectToMap().transformJsonObject(jsonObject)).getOrThrow()
        check(parsed == persisted.copy(fontScale = 1f)) { "$name changed settings outside fontScale" }
        println("PASS schema fallback $name preserves other settings")
    }
}

/** 以真實 Actions 與 StateFlow 驗證四種字級並輸出亮暗主題工具列畫面。 */
private fun verifyFontScales(
    appSettings: AppSettingsStorage,
    fontSizeConfig: com.abdownloadmanager.shared.ui.configurable.item.EnumConfigurable<Float>,
    reportDir: File,
) {
    for ((themeName, theme) in listOf("light" to DefaultThemes.light, "dark" to DefaultThemes.dark)) {
        val actions = toolbarActions {}
        ImageComposeScene(1100, 180) {
            CompositionLocalProvider(LocaleLanguageDirection provides LayoutDirection.Ltr) {
                ABDownloaderTheme(theme, fontScale = appSettings.fontScale.collectAsState().value) {
                    Box(Modifier.fillMaxSize().background(theme.background)) {
                        Actions(actions, showLabels = true)
                    }
                }
            }
        }.use { scene ->
            var frameTime = 0L
            var baselineSp = 0f
            for (scale in listOf(1f, 0.8f, 1.5f, 2f)) {
                check(fontSizeConfig.set(scale)) { "The real font size configurable rejected ${scale * 100}%" }
                frameTime = settle(scene, frameTime)
                val measuredSp = textFontSize(buttonWithText(scene, "新增下載"))
                if (scale == 1f) baselineSp = measuredSp
                check(baselineSp > 0f && abs(measuredSp - baselineSp * scale) < 0.02f) {
                    "${scale * 100}% produced $measuredSp sp; 100% baseline is $baselineSp sp"
                }
                saveScreenshot(scene, frameTime, File(reportDir, "$themeName-${(scale * 100).roundToInt()}.png"))
                println("PASS toolbar text ${themeName} ${(scale * 100).roundToInt()}%: $measuredSp sp")
            }
        }
    }
}

/** 在桌面主題中量測實際 32dp 圖示，確認字級倍率不改動 dp 與系統密度。 */
private fun verifyIconSize(reportDir: File) {
    for ((themeName, theme) in listOf("light" to DefaultThemes.light, "dark" to DefaultThemes.dark)) {
        for (scale in listOf(0.8f, 1f, 1.5f, 2f)) {
            var measuredSize = IntSize.Zero
            var densityScale = 0f
            var systemFontScale = 0f
            ImageComposeScene(80, 80) {
                densityScale = LocalDensity.current.density
                systemFontScale = LocalDensity.current.fontScale
                ABDownloaderTheme(theme, fontScale = scale) {
                    MyIcon(
                        icon = MyIcons.add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).onSizeChanged { measuredSize = it },
                    )
                }
            }.use { scene -> settle(scene, 0L) }
            val expectedPixels = (32 * densityScale).roundToInt()
            check(measuredSize == IntSize(expectedPixels, expectedPixels)) {
                "$themeName ${scale * 100}% icon size changed: $measuredSize at density $densityScale"
            }
            println("PASS icon remains 32dp: $themeName ${(scale * 100).roundToInt()}% fontScale=$systemFontScale")
        }
    }
}

/** 在 400dp 寬、200% 字級下以 Tab 導航並啟用工具列末端按鈕。 */
private fun verifyNarrowToolbar(
    appSettings: AppSettingsStorage,
    fontSizeConfig: com.abdownloadmanager.shared.ui.configurable.item.EnumConfigurable<Float>,
) {
    check(fontSizeConfig.set(2f))
    var clicks = 0
    val actions = toolbarActions { clicks++ }
    for ((themeName, theme) in listOf("light" to DefaultThemes.light, "dark" to DefaultThemes.dark)) {
        ImageComposeScene(400, 180) {
            CompositionLocalProvider(LocaleLanguageDirection provides LayoutDirection.Ltr) {
                ABDownloaderTheme(theme, fontScale = appSettings.fontScale.collectAsState().value) {
                    Box(Modifier.fillMaxSize().background(theme.background)) {
                        Actions(actions, showLabels = true)
                    }
                }
            }
        }.use { scene ->
            var frameTime = settle(scene, 0L)
            var settingsButton = buttonWithText(scene, "設定")
            var attempts = 0
            while (!isFocused(settingsButton) && attempts < 30) {
                frameTime = sendKey(scene, Key.Tab, frameTime)
                settingsButton = buttonWithText(scene, "設定")
                attempts++
            }
            check(isFocused(settingsButton)) { "$themeName settings button never received keyboard focus" }
            val bounds = settingsButton.boundsInRoot
            check(bounds.left >= 0 && bounds.right <= 400 && bounds.top >= 0 && bounds.bottom <= 180) {
                "$themeName settings button is not reachable at 400dp: $bounds"
            }
            val before = clicks
            click(scene, settingsButton, 400, 180)
            frameTime = settle(scene, frameTime)
            check(clicks == before + 1) { "$themeName settings action did not activate after navigation" }
            println("PASS narrow 400dp 200% $themeName settings button reachable at $bounds")
        }
    }
}

/** 建立與主畫面相同的 Actions 工具列資料，並回報指定動作的點擊。 */
private fun toolbarActions(onClick: () -> Unit): List<MenuItem> = listOf(
    singleAction("新增下載", MyIcons.add, onClick),
    singleAction("剪貼簿匯入", MyIcons.paste, onClick),
    singleAction("批次下載", MyIcons.download, onClick),
    MenuItem.Separator,
    singleAction("續傳", MyIcons.resume, onClick),
    singleAction("暫停", MyIcons.pause, onClick),
    singleAction("全部停止", MyIcons.stop, onClick),
    MenuItem.Separator,
    singleAction("刪除", MyIcons.remove, onClick),
    singleAction("佇列", MyIcons.queue, onClick),
    MenuItem.Separator,
    singleAction("設定", MyIcons.settings, onClick),
)

/** 建立供離屏工具列使用、點擊時執行指定回呼的單一動作。 */
private fun singleAction(title: String, icon: IconSource, action: () -> Unit) =
    object : MenuItem.SingleItem(StringSource.FromString(title), icon) {
        /** 將按鈕點擊交回 smoke 驗證器計數。 */
        override fun onClick() = action()
    }

/** 攤平場景語意樹，供文字量測、焦點與按鈕操作共用。 */
private fun semanticsNodes(scene: ImageComposeScene): List<SemanticsNode> {
    val pending = ArrayDeque<SemanticsNode>()
    scene.semanticsOwners.forEach { pending.add(it.rootSemanticsNode) }
    val result = mutableListOf<SemanticsNode>()
    while (pending.isNotEmpty()) {
        val node = pending.removeLast()
        result += node
        node.children.forEach(pending::add)
    }
    return result
}

/** 依工具列按鈕的可見標籤取得唯一語意節點。 */
private fun buttonWithText(scene: ImageComposeScene, label: String): SemanticsNode = semanticsNodes(scene).single {
    it.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
}

/** 讀取按鈕內真實 Text 元件的排版字級。 */
private fun textFontSize(node: SemanticsNode): Float {
    val results = mutableListOf<TextLayoutResult>()
    val action = checkNotNull(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)) {
        "Button does not expose its Text layout result"
    }
    check(action.action?.invoke(results) == true && results.isNotEmpty()) {
        "Text layout result could not be read from the button"
    }
    return results.first().layoutInput.style.fontSize.value
}

/** 推進 Compose 離屏場景，處理 StateFlow 更新、重組與捲動動畫。 */
private fun settle(scene: ImageComposeScene, previousFrameTime: Long): Long {
    Snapshot.sendApplyNotifications()
    var frameTime = previousFrameTime
    repeat(40) {
        frameTime += 16_000_000
        scene.render(frameTime).close()
    }
    return frameTime
}

/** 將目前離屏工具列影像寫入本次驗證報告目錄。 */
private fun saveScreenshot(scene: ImageComposeScene, frameTime: Long, file: File) {
    scene.render(frameTime).use { image ->
        file.writeBytes(checkNotNull(image.encodeToData()).bytes)
    }
}

/** 確認語意節點目前持有鍵盤焦點。 */
private fun isFocused(node: SemanticsNode): Boolean =
    node.config.getOrNull(SemanticsProperties.Focused) == true

/** 向離屏場景送出 Tab 等鍵盤事件並推進後續重組。 */
private fun sendKey(scene: ImageComposeScene, key: Key, previousFrameTime: Long): Long {
    scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
    scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
    return settle(scene, previousFrameTime)
}

/** 在按鈕中心送出滑鼠事件，並確認目標落在可視範圍。 */
private fun click(scene: ImageComposeScene, node: SemanticsNode, width: Int, height: Int) {
    val point = node.boundsInRoot.center
    check(point.x in 0f..width.toFloat() && point.y in 0f..height.toFloat()) {
        "Click target outside viewport: $point"
    }
    scene.sendPointerEvent(
        PointerEventType.Press,
        point,
        buttons = PointerButtons(isPrimaryPressed = true),
        button = PointerButton.Primary,
    )
    scene.sendPointerEvent(PointerEventType.Release, point, buttons = PointerButtons(), button = PointerButton.Primary)
}
