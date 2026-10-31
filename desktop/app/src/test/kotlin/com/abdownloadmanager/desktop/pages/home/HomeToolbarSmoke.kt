@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)

package com.abdownloadmanager.desktop.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.use
import com.abdownloadmanager.shared.ui.theme.ABDownloaderTheme
import com.abdownloadmanager.shared.ui.theme.DefaultThemes
import com.abdownloadmanager.shared.util.darker
import com.abdownloadmanager.shared.util.ui.icon.MyIcons
import ir.amirab.util.compose.IconSource
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.action.MenuItem
import ir.amirab.util.compose.localizationmanager.LocaleLanguageDirection
import java.io.File

/** 用真實 Actions 的離屏輸入與語意樹驗證操作，將各主題及標籤模式存成檢閱圖片。 */
fun main(args: Array<String>) {
    check(java.awt.GraphicsEnvironment.isHeadless()) { "Toolbar verification must stay headless" }
    val reportDir = File(args.single()).apply { mkdirs() }
    var clicks = 0
    /** 建立只累計點擊次數的選單動作，避免啟動應用程式服務。 */
    fun action(title: String, icon: IconSource) = object : MenuItem.SingleItem(StringSource.FromString(title), icon) {
        /** 記錄經工具列或子選單送達的點擊。 */
        override fun onClick() { clicks++ }
    }
    for (theme in listOf(DefaultThemes.light, DefaultThemes.dark)) {
        for (labels in listOf(true, false)) {
            for (width in listOf(1100, 400)) {
                val add = action("新增下載", MyIcons.add)
                val resume = action("續傳", MyIcons.resume).apply { setEnabled(false) }
                val queueItem = action("測試佇列", MyIcons.queue)
                val startQueue = MenuItem.SubMenu(MyIcons.queueStart, StringSource.FromString("開始佇列"), listOf(queueItem))
                val stopQueue = MenuItem.SubMenu(MyIcons.queueStop, StringSource.FromString("停止佇列"), emptyList())
                val actions = listOf(
                    add, action("剪貼簿匯入", MyIcons.paste), action("批次下載", MyIcons.download),
                    MenuItem.Separator, resume, action("暫停", MyIcons.pause), action("全部停止", MyIcons.stop),
                    MenuItem.Separator, action("刪除", MyIcons.remove), MenuItem.Separator,
                    startQueue, stopQueue, action("佇列", MyIcons.queue), MenuItem.Separator,
                    action("設定", MyIcons.settings),
                )
                ImageComposeScene(width, if (labels) 100 else 80) {
                    CompositionLocalProvider(LocaleLanguageDirection provides LayoutDirection.Ltr) {
                        ABDownloaderTheme(theme) {
                            Box(Modifier.fillMaxSize().background(theme.background)) {
                                Actions(actions, labels)
                            }
                        }
                    }
                }.use { scene ->
                    var time = 0L
                    /** 推進離屏畫面與動畫，讓狀態及輸入反映到語意樹。 */
                    fun settle() {
                        Snapshot.sendApplyNotifications()
                        repeat(40) {
                            time += 16_000_000
                            scene.render(time).close()
                        }
                    }
                    /** 收集包含彈出選單在內的所有已合併語意節點。 */
                    fun nodes(): List<SemanticsNode> {
                        /** 遞迴攤平目前節點與其子節點。 */
                        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap { walk(it) }
                        return scene.semanticsOwners.flatMap { walk(it.rootSemanticsNode) }
                    }
                    /** 依可存取名稱或可見標籤尋找唯一按鈕。 */
                    fun button(name: String): SemanticsNode = nodes().single {
                        it.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                            (it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(name) == true ||
                                it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == name } == true)
                    }
                    /** 在實際節點中心送出滑鼠按下及放開事件。 */
                    fun click(node: SemanticsNode) {
                        val point = node.boundsInRoot.center
                        check(point.x in 0f..width.toFloat() && point.y in 0f..100f) { "Click target outside viewport: $point" }
                        scene.sendPointerEvent(PointerEventType.Press, point, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                        scene.sendPointerEvent(PointerEventType.Release, point, buttons = PointerButtons(), button = PointerButton.Primary)
                        settle()
                    }
                    /** 向離屏場景送出鍵盤按下及放開事件。 */
                    fun key(key: Key) {
                        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
                        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
                        settle()
                    }
                    /** 檢查實際圖示像素存在，並以實際背景像素計算非文字對比。 */
                    fun checkIconContrast(name: String, color: Color, darken: Float) {
                        val bounds = button(name).boundsInRoot
                        val expected = if (theme.isLight) color.darker(darken) else color
                        scene.render(time).use { image ->
                            val pixels = image.toComposeImageBitmap().toPixelMap()
                            val background = pixels[bounds.left.toInt() + 5, bounds.top.toInt() + 5]
                            val iconPixel = (bounds.left.toInt() until bounds.right.toInt()).flatMap { x ->
                                (bounds.top.toInt() + 10 until bounds.top.toInt() + 42).map { y -> pixels[x, y] }
                            }.minBy {
                                kotlin.math.abs(it.red - expected.red) + kotlin.math.abs(it.green - expected.green) +
                                    kotlin.math.abs(it.blue - expected.blue)
                            }
                            check(kotlin.math.abs(iconPixel.red - expected.red) < 0.15f &&
                                kotlin.math.abs(iconPixel.green - expected.green) < 0.15f &&
                                kotlin.math.abs(iconPixel.blue - expected.blue) < 0.15f
                            ) { "Expected icon color is absent: $name" }
                            val high = maxOf(iconPixel.luminance(), background.luminance())
                            val low = minOf(iconPixel.luminance(), background.luminance())
                            val contrast = (high + 0.05f) / (low + 0.05f)
                            check(contrast >= 3f) { "Icon contrast below 3:1: ${theme.id} $name $contrast" }
                            println("CONTRAST ${theme.id} $name ${iconPixel.toArgb().toUInt().toString(16)} $contrast")
                        }
                    }
                    settle()
                    if (width == 1100 && labels) {
                        checkIconContrast("暫停", theme.warning, 0.3f)
                        scene.sendPointerEvent(PointerEventType.Move, button("暫停").boundsInRoot.center)
                        settle()
                        checkIconContrast("暫停", theme.warning, 0.3f)
                        scene.sendPointerEvent(PointerEventType.Move, Offset(-1f, -1f))
                        settle()
                    }
                    check(nodes().count { it.config.getOrNull(SemanticsProperties.Role) == Role.Button } == 11)
                    val initial = clicks
                    click(button("新增下載"))
                    check(clicks == initial + 1) { "Enabled action did not dispatch" }
                    if (width == 1100) {
                        check(button("續傳").config.contains(SemanticsProperties.Disabled))
                        click(button("續傳"))
                        check(clicks == initial + 1) { "Disabled action dispatched" }
                    }
                    add.setTitle(StringSource.FromString("新增網址"))
                    add.setIcon(MyIcons.link)
                    resume.setEnabled(true)
                    settle()
                    check(!button("續傳").config.contains(SemanticsProperties.Disabled))
                    check(button("新增網址").boundsInRoot.width >= 56f)
                    if (width == 1100) {
                        click(button("續傳"))
                        check(clicks == initial + 2)
                        check(button("開始佇列").config.contains(SemanticsActions.Expand))
                        click(button("開始佇列"))
                        check(button("開始佇列").config.contains(SemanticsActions.Collapse))
                        check(nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "測試佇列" } == true }) { "Queue submenu did not open" }
                        val child = nodes().first { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "測試佇列" } == true }
                        click(child)
                        check(clicks == initial + 3) { "Queue item did not dispatch" }
                        check(nodes().none { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "測試佇列" } == true }) { "Queue submenu did not dismiss" }
                        stopQueue.setItems(listOf(queueItem))
                        settle()
                        check(!button("停止佇列").config.contains(SemanticsProperties.Disabled))
                    } else {
                        check(nodes().any { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }) { "Narrow toolbar has no scroll semantics" }
                        click(button("新增網址"))
                        repeat(9) { key(Key.Tab) }
                        val focused = nodes().single { it.config.getOrNull(SemanticsProperties.Focused) == true }
                        check(focused.boundsInRoot.left >= 0 && focused.boundsInRoot.right <= width) { "Keyboard focus is clipped: ${focused.boundsInRoot}" }
                        check(button("設定").boundsInRoot.left >= 0 && button("設定").boundsInRoot.right <= width) { "Tab did not bring the final action into view" }
                        val before = clicks
                        key(Key.Enter)
                        check(clicks == before + 1) { "Keyboard activation did not dispatch" }
                        scene.sendPointerEvent(PointerEventType.Scroll, Offset(200f, 30f), scrollDelta = Offset(-1000f, 0f))
                        settle()
                        check(button("新增網址").boundsInRoot.left >= 0) { "Horizontal scrolling did not return to the start" }
                    }
                    add.setTitle(StringSource.FromString("新增下載"))
                    add.setIcon(MyIcons.add)
                    resume.setEnabled(false)
                    stopQueue.setItems(emptyList())
                    scene.sendPointerEvent(PointerEventType.Move, Offset(-1f, -1f))
                    settle()
                    scene.render(time).use { image ->
                        File(reportDir, "${theme.id}-${if (labels) "labels" else "icons"}-$width.png")
                            .writeBytes(checkNotNull(image.encodeToData()).bytes)
                    }
                    println("PASS ${theme.id} labels=$labels width=$width")
                }
            }
        }
    }
    println("PASS toolbar pointer dispatch, disabled, StateFlow, submenu, scroll and keyboard; reports=$reportDir")
}
