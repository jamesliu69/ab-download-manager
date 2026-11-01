# 網頁連結匯入實作計畫

> **面向 AI 代理的工作者：** 依序以測試先行方式完成各步驟；每個檢查點都須執行對應驗證。

**目標：** 在桌面批次下載視窗加入網頁連結擷取，讓使用者檢視擷取結果後送入既有多筆下載確認流程。

**架構：** 新增桌面 JVM `WebPageLinkExtractor`，以 OkHttp 抓取單一使用者指定的 HTTP(S) HTML 頁面，並用 JDK HTML parser 擷取錨點連結。限制 HTML 大小與匯入連結數；桌面批次元件負責載入狀態、錯誤、結果及沿用現有 `importLinks` 流程，不做遞迴爬站。

**技術棧：** Kotlin/JVM、OkHttp、JDK `ParserDelegator`、Compose Desktop、既有 Gradle headless smoke 驗證慣例。

---

## 檔案與職責

- 建立 `desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\pages\batchdownload\WebPageLinkExtractor.kt`：有界 HTTP 抓取、HTML 錨點解析、相對網址解析、HTTP(S) 篩選與去重。
- 建立 `desktop\app\src\test\kotlin\com\abdownloadmanager\desktop\pages\batchdownload\WebPageLinkExtractorSmoke.kt`：以真實 JDK HTML parser 驗證連結解析邊界。
- 修改 `desktop\app\build.gradle.kts`：新增專用 headless smoke task。
- 修改 `desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\pages\batchdownload\DesktopBatchDownloadComponent.kt`：管理網頁網址、掃描狀態、擷取錯誤及結果匯入。
- 修改 `desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\AppComponent.kt`：將應用程式設定的 OkHttp client 傳給批次下載元件。
- 修改 `desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\pages\batchdownload\BatchDownnload.kt`：提供網頁網址輸入、掃描、結果預覽及匯入操作。
- 修改 `shared\resources\src\commonMain\resources\com\abdownloadmanager\resources\locales\en_US.properties`：新增英文介面字串作為 Crowdin 翻譯來源。

## 任務 1：建立連結解析行為測試

- [ ] 測試擷取錨點連結時解析相對網址、只保留 HTTP(S)、保留首次出現順序並去重。
- [ ] 測試拒絕頁面網址無效及連結數上限的邊界。
- [ ] 執行 `.\gradlew.bat :desktop:app:verifyWebPageLinkExtractor`，確認新 API 缺失造成預期編譯失敗。

## 任務 2：實作有界擷取器

- [ ] 新增 parser API 與測試所需實作，使用 JDK `ParserDelegator`，錨點 URL 以頁面 URL／首個有效 `<base>` 解析。
- [ ] 新增 OkHttp 抓取：只允許 HTTP(S)、只接受 HTML 回應、限制回應本文至 2 MiB，並傳回最多 1000 個唯一連結及截斷旗標。
- [ ] 重跑 smoke task，確認解析案例通過。

## 任務 3：接入桌面批次下載流程

- [ ] 將 AppComponent 使用的 OkHttp client 注入桌面批次元件。
- [ ] 加入網頁網址、掃描中、錯誤訊息、連結數及超量截斷狀態；取消或失敗不得假裝成功。
- [ ] 在批次下載視窗加入網頁連結掃描操作，使用者確認後透過既有批次匯入 callback 開啟多筆下載確認視窗。
- [ ] 將新增字串加入 `en_US.properties`，以既有生成任務產生資源。

## 任務 4：驗證完整變更

- [ ] 執行 `.\gradlew.bat :desktop:app:verifyWebPageLinkExtractor`。
- [ ] 執行桌面 app compile/build 任務，確認 UI、DI 與資源生成一致。
- [ ] 執行 `git diff --check` 並確認不包含無關變更。
