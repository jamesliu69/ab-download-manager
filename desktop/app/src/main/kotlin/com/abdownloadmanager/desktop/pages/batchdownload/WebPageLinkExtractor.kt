package com.abdownloadmanager.desktop.pages.batchdownload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.StringReader
import java.nio.charset.StandardCharsets
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

/** 網頁連結解析結果；超過上限時保留前段連結並標示遭截斷。 */
data class ExtractedPageLinks(
    val links: List<String>,
    val truncated: Boolean,
)

/** 以有限大小的 HTTP(S) HTML 回應擷取錨點與常見媒體來源連結。 */
class WebPageLinkExtractor(
    private val httpClient: OkHttpClient,
) {
    /** 抓取單一 HTTP(S) 網頁並擷取最多指定數量的唯一連結與媒體來源。 */
    suspend fun extractPageLinks(pageUrl: String): ExtractedPageLinks {
        val url = pageUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid HTTP or HTTPS page URL.")
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("The page returned HTTP ${response.code}.")
                }
                val body = response.body
                val mediaType = body.contentType()?.let {
                    "${it.type}/${it.subtype}"
                }
                if (mediaType != null && !mediaType.equals("text/html", true) &&
                    !mediaType.equals("application/xhtml+xml", true)
                ) {
                    throw IOException("The page response is not HTML.")
                }
                val bytes = body.byteStream().use {
                    it.readNBytes(MAX_PAGE_BYTES + 1)
                }
                if (bytes.size > MAX_PAGE_BYTES) {
                    throw IOException("The page is larger than the 2 MiB scan limit.")
                }
                val charset = body.contentType()?.charset(StandardCharsets.UTF_8)
                    ?: StandardCharsets.UTF_8
                extractLinks(
                    html = String(bytes, charset),
                    pageUrl = response.request.url.toString(),
                )
            }
        }
    }

    companion object {
        /** 網頁本文最多讀取的位元組數。 */
        const val MAX_PAGE_BYTES = 2 * 1024 * 1024

        /** 批次匯入一次允許的唯一網頁連結上限。 */
        const val MAX_LINKS = 1000

        /** 解析錨點與常見媒體來源，以頁面及首個有效 base URL 展開相對網址並保留順序。 */
        fun extractLinks(
            html: String,
            pageUrl: String,
            maxLinks: Int = MAX_LINKS,
        ): ExtractedPageLinks {
            require(maxLinks > 0) { "The maximum link count must be positive." }
            val initialBase = pageUrl.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Enter a valid HTTP or HTTPS page URL.")
            require(initialBase.scheme == "http" || initialBase.scheme == "https") {
                "Only HTTP and HTTPS pages can be scanned."
            }

            var baseUrl = initialBase
            var baseElementHandled = false
            val links = linkedSetOf<String>()
            val mediaResourceTags = setOf("img", "video", "audio", "source")

            fun addLink(candidate: String?) {
                val value = candidate?.trim()?.takeIf {
                    it.isNotEmpty() && !it.startsWith("#")
                } ?: return
                val resolved = baseUrl.resolve(value)
                    ?.takeIf { it.scheme == "http" || it.scheme == "https" }
                    ?.newBuilder()
                    ?.fragment(null)
                    ?.build()
                    ?: return
                links.add(resolved.toString())
            }

            /** 擷取錨點目的地與常見媒體元素的直接來源網址。 */
            fun addElementLink(tag: HTML.Tag, attributes: MutableAttributeSet) {
                when (tag.toString().lowercase()) {
                    "a" -> addLink(attributes.getAttribute(HTML.Attribute.HREF)?.toString())
                    in mediaResourceTags -> addLink(attributes.getAttribute(HTML.Attribute.SRC)?.toString())
                }
            }

            fun updateBase(attributes: MutableAttributeSet) {
                if (baseElementHandled) return
                val href = attributes.getAttribute(HTML.Attribute.HREF)?.toString()
                    ?.takeIf { it.isNotBlank() } ?: return
                baseUrl.resolve(href)
                    ?.takeIf { it.scheme == "http" || it.scheme == "https" }
                    ?.let {
                        baseUrl = it
                        baseElementHandled = true
                    }
            }

            val parser = ParserDelegator()
            parser.parse(
                StringReader(html),
                object : HTMLEditorKit.ParserCallback() {
                    override fun handleStartTag(
                        tag: HTML.Tag,
                        attributes: MutableAttributeSet,
                        position: Int,
                    ) {
                        if (tag == HTML.Tag.BASE) {
                            updateBase(attributes)
                        } else {
                            addElementLink(tag, attributes)
                        }
                    }

                    override fun handleSimpleTag(
                        tag: HTML.Tag,
                        attributes: MutableAttributeSet,
                        position: Int,
                    ) {
                        if (tag == HTML.Tag.BASE) {
                            updateBase(attributes)
                        } else {
                            addElementLink(tag, attributes)
                        }
                    }
                },
                true,
            )
            return ExtractedPageLinks(
                links = links.take(maxLinks),
                truncated = links.size > maxLinks,
            )
        }
    }
}
