package com.abdownloadmanager.desktop.pages.home

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abdownloadmanager.shared.ui.widget.Text
import com.abdownloadmanager.shared.ui.widget.Tooltip
import com.abdownloadmanager.shared.ui.widget.menu.custom.MyDropDown
import com.abdownloadmanager.shared.ui.widget.menu.custom.SubMenu
import com.abdownloadmanager.shared.util.darker
import com.abdownloadmanager.shared.util.div
import com.abdownloadmanager.shared.util.ui.MultiplatformHorizontalScrollbar
import com.abdownloadmanager.shared.util.ui.WithContentAlpha
import com.abdownloadmanager.shared.util.ui.WithContentColor
import com.abdownloadmanager.shared.util.ui.icon.MyIcons
import com.abdownloadmanager.shared.util.ui.myColors
import com.abdownloadmanager.shared.util.ui.needScroll
import com.abdownloadmanager.shared.util.ui.theme.myShapes
import com.abdownloadmanager.shared.util.ui.theme.myTextSizes
import com.abdownloadmanager.shared.util.ui.widget.MyIcon
import ir.amirab.util.compose.IconSource
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.action.MenuItem
import ir.amirab.util.ifThen

/** 依既有選單狀態顯示單列快捷操作，窄視窗可水平捲動。 */
@Composable
fun Actions(
    list: List<MenuItem>,
    showLabels: Boolean,
) {
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxWidth().background(myColors.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (a in list) {
                when (a) {
                    MenuItem.Separator -> Spacer(
                        Modifier
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(myColors.onBackground / 15)
                    )
                    is MenuItem.SingleItem -> ActionButton(a, showLabels)
                    is MenuItem.SubMenu -> GroupActionButton(a, showLabels)
                }
            }
        }
        val adapter = rememberScrollbarAdapter(scrollState)
        if (adapter.needScroll()) {
            MultiplatformHorizontalScrollbar(
                adapter = adapter,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            )
        }
    }
}

/** 直接呼叫既有動作，並隨其 StateFlow 更新標題、圖示與啟用狀態。 */
@Composable
private fun ActionButton(action: MenuItem.SingleItem, showLabels: Boolean) {
    ActionIconWithLabel(
        title = action.title.collectAsState().value,
        icon = action.icon.collectAsState().value,
        showLabels = showLabels,
        onClick = { action() },
        enabled = action.isEnabled.collectAsState().value,
    )
}

/** 以原有下拉元件顯示佇列操作，子項目與啟用狀態由選單提供。 */
@Composable
private fun GroupActionButton(action: MenuItem.SubMenu, showLabels: Boolean) {
    val enabled by action.isEnabled.collectAsState()
    var showSubMenu by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) showSubMenu = false
    }
    Column {
        ActionIconWithLabel(
            title = action.title.collectAsState().value,
            icon = action.icon.collectAsState().value,
            showLabels = showLabels,
            onClick = { showSubMenu = !showSubMenu },
            enabled = enabled,
            isGroup = true,
            expanded = showSubMenu,
        )
        val close = { showSubMenu = false }
        if (enabled && showSubMenu) {
            MyDropDown(onDismissRequest = close) {
                val items by action.items.collectAsState()
                SubMenu(subMenu = items, onRequestClose = close)
            }
        }
    }
}

/** 使用主題語意色與按鈕語意呈現操作，鍵盤移入焦點時將整個按鈕捲入畫面。 */
@Composable
private fun ActionIconWithLabel(
    title: StringSource,
    icon: IconSource?,
    showLabels: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    isGroup: Boolean = false,
    expanded: Boolean = false,
) {
    val label = title.rememberString()
    val colors = myColors
    val accent = when (icon) {
        MyIcons.add, MyIcons.resume, MyIcons.queueStart -> colors.success
        MyIcons.pause, MyIcons.queueStop -> colors.warning
        MyIcons.stop, MyIcons.remove -> colors.error
        MyIcons.paste, MyIcons.download -> colors.info
        MyIcons.queue -> colors.secondary
        else -> colors.primary
    }.let { if (colors.isLight) it.darker(if (it == colors.warning) 0.3f else 0.2f) else it }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }
    val shape = myShapes.defaultRounded
    Tooltip(title) {
        Column(
            modifier = Modifier
                .bringIntoViewRequester(bringIntoViewRequester)
                .ifThen(!enabled) { alpha(0.4f) }
                .border(2.dp, if (focused) colors.focusedBorderColor else colors.background.copy(alpha = 0f), shape)
                .clip(shape)
                .background(accent / if (hovered && enabled) 18 else 8)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics {
                    if (!showLabels) contentDescription = label
                    if (isGroup) {
                        val toggle: (() -> Boolean)? = if (enabled) ({ onClick(); true }) else null
                        if (expanded) collapse(action = toggle) else expand(action = toggle)
                    }
                }
                .widthIn(min = if (showLabels) 72.dp else 56.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let { MyIcon(it, null, Modifier.size(32.dp), tint = accent) }
                if (isGroup) {
                    Spacer(Modifier.width(2.dp))
                    MyIcon(MyIcons.down, null, Modifier.size(12.dp), tint = colors.onBackground)
                }
            }
            if (showLabels) {
                Spacer(Modifier.height(4.dp))
                WithContentColor(colors.onBackground) {
                    WithContentAlpha(1f) {
                        Text(label, maxLines = 1, fontSize = myTextSizes.base)
                    }
                }
            }
        }
    }
}
