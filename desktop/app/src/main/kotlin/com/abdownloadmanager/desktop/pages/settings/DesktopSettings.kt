package com.abdownloadmanager.desktop.pages.settings

import com.abdownloadmanager.desktop.storage.AppSettingsStorage
import com.abdownloadmanager.desktop.ui.configurable.platform.item.FontConfigurable
import com.abdownloadmanager.desktop.utils.renderapi.CustomRenderApi
import com.abdownloadmanager.desktop.utils.renderapi.RenderApi
import com.abdownloadmanager.resources.Res
import com.abdownloadmanager.shared.ui.configurable.item.BooleanConfigurable
import com.abdownloadmanager.shared.ui.configurable.item.EnumConfigurable
import com.abdownloadmanager.shared.util.asToggledStringSource
import ir.amirab.util.compose.asStringSource
import ir.amirab.util.flow.createMutableStateFlowFromStateFlow
import ir.amirab.util.platform.Platform
import ir.amirab.util.platform.isMac
import kotlinx.coroutines.CoroutineScope

object DesktopSettings {
    fun mergeTopBarWithTitleBarConfig(appSettings: AppSettingsStorage): BooleanConfigurable {
        return BooleanConfigurable(
            title = Res.string.settings_compact_top_bar.asStringSource(),
            description = Res.string.settings_compact_top_bar_description.asStringSource(),
            backedBy = appSettings.mergeTopBarWithTitleBar,
            describe = {
                it.asToggledStringSource()
            },
        )
    }

    fun useNativeMenuBarConfig(appSettings: AppSettingsStorage): BooleanConfigurable? {
        if (Platform.isMac().not()) return null
        return BooleanConfigurable(
            title = Res.string.settings_use_native_menu_bar.asStringSource(),
            description = Res.string.settings_use_native_menu_bar_description.asStringSource(),
            backedBy = appSettings.useNativeMenuBar,
            describe = {
                it.asToggledStringSource()
            },
        )
    }

    fun useSystemTray(appSettings: AppSettingsStorage): BooleanConfigurable {
        return BooleanConfigurable(
            title = Res.string.settings_use_system_tray.asStringSource(),
            description = Res.string.settings_use_system_tray_description.asStringSource(),
            backedBy = appSettings.useSystemTray,
            describe = {
                it.asToggledStringSource()
            },
        )
    }

    fun fontConfig(
        fontManager: FontManager,
        scope: CoroutineScope,
    ): FontConfigurable {
        return FontConfigurable(
            title = Res.string.settings_font.asStringSource(),
            description = Res.string.settings_font_description.asStringSource(),
            backedBy = createMutableStateFlowFromStateFlow(
                flow = fontManager.currentFontInfo,
                updater = { font ->
                    fontManager.setFont(font.id)
                },
                scope = scope,
            ),
            possibleValues = fontManager.selectableFonts.value,
            describe = {
                it.name
            }
        )
    }

    /** 建立立即反映於桌面文字的字級倍率選項。 */
    fun fontSizeConfig(appSettings: AppSettingsStorage): EnumConfigurable<Float> {
        return EnumConfigurable(
            title = Res.string.settings_font_size.asStringSource(),
            description = Res.string.settings_font_size_description.asStringSource(),
            backedBy = appSettings.fontScale,
            possibleValues = listOf(0.8f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f),
            itemTitle = { "${(it * 100).toInt()}%".asStringSource() },
        )
    }

    fun renderApi(
        customRenderApi: CustomRenderApi,
    ): EnumConfigurable<RenderApi?> {
        return EnumConfigurable(
            title = "Render API".asStringSource(),
            description = "Configures the Render API backend used by the application. A restart is required for the change to take effect.".asStringSource(),
            backedBy = customRenderApi.data,
            possibleValues = buildList {
                add(null)
                addAll(customRenderApi.getSupportedRenderApiForThisPlatform())
            },
            itemTitle = {
                it?.prettyName?.asStringSource()?: Res.string.default.asStringSource()
            }
        )
    }
}
