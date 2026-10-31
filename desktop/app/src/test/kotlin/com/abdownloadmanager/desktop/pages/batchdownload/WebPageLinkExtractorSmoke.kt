package com.abdownloadmanager.desktop.pages.batchdownload

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/** 驗證網頁連結解析的相對網址、協定篩選、去重及數量上限。 */
fun main() {
    val pageUrl = "https://example.com/catalog/index.html"
    val result = WebPageLinkExtractor.extractLinks(
        html = """
            <html><head><base href="/downloads/"></head><body>
              <a href="one.zip">one</a>
              <a href="/downloads/one.zip#mirror">duplicate</a>
              <a href="../manual.pdf">manual</a>
              <a href="javascript:alert(1)">script</a>
              <a href="mailto:help@example.com">mail</a>
              <a href="ftp://example.com/archive.zip">ftp</a>
            </body></html>
        """.trimIndent(),
        pageUrl = pageUrl,
    )
    check(
        result.links == listOf(
            "https://example.com/downloads/one.zip",
            "https://example.com/manual.pdf",
        )
    ) { "Unexpected extracted links: ${result.links}" }
    check(!result.truncated) { "A short page must not be marked as truncated" }

    val mediaResult = WebPageLinkExtractor.extractLinks(
        html = """
            <a href="/archive.zip">archive</a>
            <img src="/poster.png">
            <video src="/trailer.mp4"></video>
            <audio src="/podcast.mp3"></audio>
            <video><source src="/clip.webm"></video>
        """.trimIndent(),
        pageUrl = pageUrl,
    )
    check(
        mediaResult.links == listOf(
            "https://example.com/archive.zip",
            "https://example.com/poster.png",
            "https://example.com/trailer.mp4",
            "https://example.com/podcast.mp3",
            "https://example.com/clip.webm",
        )
    ) { "Anchors and direct media resources must be extracted: ${mediaResult.links}" }

    val limited = WebPageLinkExtractor.extractLinks(
        html = """
            <a href="/one.zip">one</a>
            <a href="/two.zip">two</a>
            <a href="/three.zip">three</a>
        """.trimIndent(),
        pageUrl = pageUrl,
        maxLinks = 2,
    )
    check(limited.links == listOf("https://example.com/one.zip", "https://example.com/two.zip")) {
        "The result must keep the first links up to the configured limit: ${limited.links}"
    }
    check(limited.truncated) { "Extra links must be reported as truncated" }

    val invalidUrl = runCatching {
        WebPageLinkExtractor.extractLinks("<a href='/file.zip'>file</a>", "file:///page.html")
    }.exceptionOrNull()
    check(invalidUrl is IllegalArgumentException) {
        "Non-HTTP page URLs must be rejected, got: $invalidUrl"
    }

    verifyPageFetchingLimits()
    println("PASS page and media link extraction, URL filtering, de-duplication, and limit reporting")
}

/** 使用本機 HTTP server 驗證抓取成功、HTTP 錯誤、非 HTML 回應與本文大小上限。 */
private fun verifyPageFetchingLimits() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
        val (status, contentType, body) = when (exchange.requestURI.path) {
            "/page" -> Triple(
                200,
                "text/html; charset=utf-8",
                """<a href="/download.zip">download</a>""".toByteArray(StandardCharsets.UTF_8),
            )

            "/not-html" -> Triple(200, "application/json", "{}".toByteArray())
            "/error" -> Triple(503, "text/plain", "unavailable".toByteArray())
            else -> Triple(200, "text/html", ByteArray(WebPageLinkExtractor.MAX_PAGE_BYTES + 1))
        }
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }
    server.start()
    try {
        val extractor = WebPageLinkExtractor(OkHttpClient())
        val pageUrl = "http://127.0.0.1:${server.address.port}"
        val fetched = runBlocking { extractor.extractPageLinks("$pageUrl/page") }
        check(fetched.links == listOf("$pageUrl/download.zip")) {
            "Fetched links must resolve against the final page URL: ${fetched.links}"
        }
        checkFetchFails(extractor, "$pageUrl/not-html", "not HTML")
        checkFetchFails(extractor, "$pageUrl/error", "HTTP 503")
        checkFetchFails(extractor, "$pageUrl/large", "2 MiB")
    } finally {
        server.stop(0)
    }
}

/** 確認抓取錯誤會以明確訊息回報，而不是回傳空的成功結果。 */
private fun checkFetchFails(
    extractor: WebPageLinkExtractor,
    url: String,
    expectedMessage: String,
) {
    val failure = runBlocking {
        runCatching { extractor.extractPageLinks(url) }.exceptionOrNull()
    }
    check(failure is IOException && failure.message.orEmpty().contains(expectedMessage)) {
        "Expected an IOException containing '$expectedMessage', got: $failure"
    }
}
