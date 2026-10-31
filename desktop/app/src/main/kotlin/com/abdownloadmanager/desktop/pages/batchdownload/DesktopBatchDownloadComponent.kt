package com.abdownloadmanager.desktop.pages.batchdownload

import com.abdownloadmanager.shared.pages.batchdownload.BaseBatchDownloadComponent
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DesktopBatchDownloadComponent(
    ctx: ComponentContext,
    onClose: () -> Unit,
    importLinks: (List<String>) -> Unit,
    /** 使用桌面設定的 HTTP client 抓取使用者指定的網頁。 */
    private val pageLinkExtractor: WebPageLinkExtractor,
) : BaseBatchDownloadComponent(
    ctx = ctx,
    onClose = onClose,
    importLinks = importLinks
) {
    /** 使用者目前輸入、準備掃描的網頁網址。 */
    private val _pageUrl = MutableStateFlow("")
    val pageUrl = _pageUrl.asStateFlow()

    /** 表示網頁連結擷取請求是否仍在執行。 */
    private val _isScanningPage = MutableStateFlow(false)
    val isScanningPage = _isScanningPage.asStateFlow()

    /** 最近一次成功掃描得到的連結與截斷資訊。 */
    private val _pageLinks = MutableStateFlow<ExtractedPageLinks?>(null)
    val pageLinks = _pageLinks.asStateFlow()

    /** 最近一次掃描失敗時供畫面顯示的錯誤訊息。 */
    private val _pageScanError = MutableStateFlow<String?>(null)
    val pageScanError = _pageScanError.asStateFlow()

    /** 更新網址時清除舊的掃描結果與錯誤，避免匯入不相干的連結。 */
    fun setPageUrl(url: String) {
        _pageUrl.value = url
        _pageLinks.value = null
        _pageScanError.value = null
    }

    /** 掃描單一網頁並將錯誤顯示在批次下載視窗中。 */
    fun scanPageLinks() {
        if (_isScanningPage.value) return
        _isScanningPage.value = true
        _pageLinks.value = null
        _pageScanError.value = null
        scope.launch {
            try {
                _pageLinks.value = pageLinkExtractor.extractPageLinks(_pageUrl.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _pageScanError.value = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: e::class.simpleName
                    ?: "Unknown error"
            } finally {
                _isScanningPage.value = false
            }
        }
    }

    /** 將目前掃描結果交給既有多筆下載確認流程。 */
    fun importExtractedPageLinks() {
        val links = _pageLinks.value?.links.orEmpty()
        if (links.isEmpty()) return
        importLinks(links)
        onClose()
    }

    fun bringToFront() {
        sendEffect(Effects.BringToFront)
    }

    sealed interface Effects : BaseBatchDownloadComponent.Effects.PlatformEffects {
        data object BringToFront : Effects
    }
}
