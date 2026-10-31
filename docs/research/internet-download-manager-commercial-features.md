# Internet Download Manager 與 AB Download Manager：產品功能差異分析

**分析日期：** 2026-11-01
**比較基準：** IDM 官方網站當日可查的產品說明，與本工作樹中的 AB Download Manager 原始碼及文件。本文所稱「商業級／商業化功能」是指成熟下載管理器提供給使用者的產品功能與體驗，**不代表要收費、販售授權或經營商業服務**。這份分析不是實機效能測試，也不代表完整稽核獨立維護的瀏覽器擴充套件。

## 摘要

AB Download Manager（以下簡稱 AB）已具備下載管理器的核心：HTTP 分段下載、續傳與重試、HLS 下載、下載佇列及排程、分類與篩選、代理設定、多平台桌面封裝及更新檢查。它不是只做了介面的原型；核心下載流程及桌面應用程式都已存在。

與 IDM 的主要差異不在「有沒有下載佇列或續傳」，而在使用者可直接感受到的功能廣度與完成度：IDM 官方明確列出 HTTP/HTTPS/FTP/MMS、瀏覽器下載攔截、影片下載面板、網站抓取、下載完成後防毒掃描、流量配額、佇列排程及自訂介面等。AB 工作樹中有原生訊息整合與 HLS 支援，但完整瀏覽器擴充功能是獨立專案；本倉庫證據不足以確認其擷取覆蓋率，也未見足以證明 FTP/MMS、通用影片抓取或網站抓取與 IDM 等價的實作。

你的目標是先了解 IDM 提供哪些使用者功能，再自行決定要開發哪些；因此本文聚焦功能盤點與技術差距。授權收費、付款、銷售、客服 SLA 等商業營運議題不屬於本次開發範圍，也不應被當作你要補上的功能。

**重要範圍說明：** 以下程式碼判斷以目前工作樹為準，不等同於機器上已安裝的 AB 1.11.1。README 提到瀏覽器擴充功能另有獨立儲存庫，因此「本倉庫未證明」不應直接解讀成「整個 AB 產品沒有」。

## IDM 官方能力快照

IDM 官方將產品描述為 Windows 下載管理器，宣稱透過動態分段與重用連線提升速度「最高 8 倍」；這是廠商宣稱，不是獨立基準測試。官方功能頁列出 HTTP、HTTPS、FTP、MMS、下載續傳與錯誤復原、排程及佇列、網站抓取、頁面連結檔案的「Download All」、分類、介面主題、多語系、流量配額、拖放、代理、下載後自動防毒掃描及影片下載面板。 [官方首頁](https://www.internetdownloadmanager.com/) · [功能清單](https://www.internetdownloadmanager.com/features2.html) · [分段下載說明](https://www.internetdownloadmanager.com/support/segmentation.html)

首頁列出 Chrome、Firefox、Edge、Opera 等整合；專門說明頁提供 Chrome、Chromium Edge、Opera 的擴充功能設定方式。官方頁面表示需安裝並啟用 IDM Integration Module 才能使用瀏覽器整合。 [Chrome](https://www.internetdownloadmanager.com/register/new_faq/chrome_extension.html) · [Edge](https://www.internetdownloadmanager.com/register/new_faq/edge-extension.html) · [Opera](https://www.internetdownloadmanager.com/register/new_faq/opera-extension.html)

官方首頁列出 Windows 7、8/8.1、10、11。已檢視的官方頁面未能證實 IDM 提供 macOS/Linux 版本。產品頁提及每週自動檢查更新，並提供使用說明及聯絡頁。試用、授權及價格屬產品供應方式，與本文要比較的下載管理功能無關，故不列為待開發差距。 [官方首頁](https://www.internetdownloadmanager.com/) · [更新說明](https://www.internetdownloadmanager.com/register/new_faq/idm_update.html) · [聯絡支援](https://www.internetdownloadmanager.com/contact_us.html)

IDM 官方更新頁於本次分析日期列出 **6.43 Build 16（2026-10-23）**。購買頁當日顯示的價格為新台幣（TWD），可能依地區、稅項及時間變動，不應當作全球固定價格。官方首頁的版本文案與更新紀錄有落差；更新紀錄是本報告判斷當前版本的依據。 [官方更新紀錄](https://www.internetdownloadmanager.com/news.html) · [購買頁](https://www.internetdownloadmanager.com/buy.html)

## 功能差距矩陣

| 面向 | AB：工作樹中已確認 | 對 IDM 的差距／待驗證項目 |
|---|---|---|
| 下載引擎與協定 | HTTP 下載、分段工作、續傳入口及重試流程：`downloader\core\src\commonMain\kotlin\ir\amirab\downloader\downloaditem\http\HttpDownloadJob.kt`、`...\DownloadManager.kt`。OkHttp 客戶端處理 HTTP 標頭、代理及 Range 請求：`...\connection\OkHttpHttpDownloaderClient.kt`。 | IDM 明列 HTTP、HTTPS、FTP、MMS。AB 此處證明 HTTP(S) 路徑，不足以證明 FTP/MMS。需做正式協定支援矩陣，勿以 HTTP 客戶端具備 TLS 推論所有協定皆受支援。 |
| 分段與效能 | 下載工作有多部分段實作，亦有速度限制器：`HttpDownloadJob.kt`、`downloader\core\src\commonMain\kotlin\ir\amirab\downloader\utils\speedlimiter\SpeedLimiter.kt`。 | IDM 的「最高 8 倍」是廠商宣稱；AB 沒有在此證據中提供可比較的基準數據。尚需評估連線數策略、伺服器相容性、低速／高速網路表現及對伺服器負載的影響。 |
| 續傳、錯誤處理 | 有續傳與重試程式路徑；HTTP 測試會嘗試 Range，遇到部分伺服器不相容時退回無續傳模式：`HttpDownloadJob.kt`、`HttpDownloaderClient.kt`。 | 核心能力已存在，但可靠性品質不能只由程式碼存在與否推斷。需以斷網、伺服器不支援 Range、重導向、認證、磁碟空間不足、應用程式重新啟動等情境驗證。 |
| 影片與串流 | 有 HLS 下載工作：`downloader\core\src\commonMain\kotlin\ir\amirab\downloader\downloaditem\hls\HLSDownloadJob.kt`。`HLSResponseInfo.kt` 對已知片段格式有 `.ts` 限制。 | IDM 提供瀏覽器影片下載面板。AB 本倉庫可確認 HLS 核心路徑，不能據此確認網站影片偵測、DRM／非 DRM 邊界、分段格式及瀏覽器面板等級的體驗。 |
| 網站抓取與批次擷取 | 此功能分支新增桌面單頁擷取：`desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\pages\batchdownload\WebPageLinkExtractor.kt` 可解析錨點及常見圖片／音訊／影片資源，限制 HTML 2 MiB、最多匯入 1000 個唯一連結；`BatchDownnload.kt` 提供掃描、預覽與既有批次確認流程。 | IDM 官方列有網站抓取、篩選器及頁面「Download All」。目前仍不遞迴抓取其他頁面，也沒有網域／深度／檔案類型篩選；擴充前應提供使用者可控範圍與取消機制。 |
| 瀏覽器整合 | 有 native messaging host 與 handler：`desktop\app\src\main\kotlin\com\abdownloadmanager\desktop\nativemessaging\host\NativeMessagingHost.kt`、`...\DefinedHandlers.kt`。README 列有 Chrome／Firefox 擴充功能連結並指出其為獨立專案。 | 不是從零開始，但本倉庫不足以確認擴充功能商店發布、瀏覽器版本涵蓋、下載攔截成功率、Cookie／Referer／POST 情況、更新相容性及影片面板行為。應將獨立擴充專案納入後續產品盤點。 |
| 佇列與排程 | 佇列有併發數、排程時間及佇列空時停止等持久化欄位：`DownloadQueuePersistedDataAccess.kt`；排程與佇列管理在 `queue\DownloadQueue.kt`、`queue\QueueManager.kt`；桌面佇列頁提供相應操作。 | 基礎能力已存在，不宜列為待從零開發。需比對更細的排程條件、重複任務、完成後關機／斷線及同步工作等 IDM 使用情境。 |
| 分類、搜尋與介面 | 有分類管理、首頁篩選／搜尋狀態、主題及多語系資源：`shared\app\src\commonMain\kotlin\com\abdownloadmanager\shared\pages\home\FilterState.kt`、`...\util\category\CategoryManager.kt`、`shared\resources\src\commonMain\resources\com\abdownloadmanager\resources\locales\`。 | 基礎組織及在地化已存在。IDM 的自訂介面、拖放、下載配額等需逐項核對；無障礙完整度與鍵盤／螢幕閱讀器涵蓋率尚未證實。 |
| 代理及下載後處理 | HTTP 客戶端支援直連、系統代理、腳本代理、手動 HTTP/SOCKS 代理；相關邏輯在 `OkHttpHttpDownloaderClient.kt`。 | IDM 也列出代理及下載後自動防毒掃描。AB 已證實代理路徑；目前證據未確認內建下載後防毒掃描流程。 |
| 平台與安裝 | README 宣稱 Android、Windows、Linux、macOS；桌面 Gradle 設定包含 Windows NSIS、macOS DMG 與平台封裝：`desktop\app\build.gradle.kts`。 | 多平台是 AB 的優勢，不是 IDM 已證實的差距。需分開驗證各平台功能一致性、簽章／公證、安裝升級與解除安裝，不應把 README 宣稱等同正式支援矩陣。 |
| 更新及維護 | 有 GitHub 更新檢查器：`shared\updater\src\commonMain\kotlin\com\abdownloadmanager\updatechecker\GithubUpdateChecker.kt`；有安裝包建置。 | 更新檢查的存在不等於安全、不中斷的自動更新服務。尚需驗證簽章／雜湊、回滾、分批發布、版本支援政策及發布事故處理。 |
| 易用性與輔助功能 | 有多語系資源及首頁篩選／搜尋狀態：`shared\resources\src\commonMain\resources\com\abdownloadmanager\resources\locales\`、`FilterState.kt`。 | IDM 列有自訂介面、深色主題、拖放、流量配額等。AB 的主題、分類與多語系已存在；逐項核實拖放、配額及鍵盤／螢幕閱讀器涵蓋率，再決定是否要補。 |

**判讀方式：** 「未確認／未證明」表示本次引用的工作樹證據不足，不能當成整個產品必定沒有；「已確認」表示有程式碼或專案文件佐證，也不等於已達 IDM 品質、效能或市場支援程度。

## 若要達到 IDM 類產品能力，建議補足的項目

### P0：先釐清真實差距並保住核心品質

1. **建立可驗收的功能矩陣。** 將 HTTP/HTTPS、FTP/MMS、續傳、分段、佇列、排程、代理、HLS、瀏覽器攔截及批次擷取分別標示「已支援／部分支援／不支援／尚未驗證」，按 Windows、Linux、macOS、Android、Chrome、Firefox、Edge 列出版本範圍與限制。
2. **將瀏覽器擴充功能納入產品範圍。** 核對獨立擴充專案與 native messaging 協定；測試一般下載、登入 Cookie、Referer、重導向、檔名及重複下載處理。若影片面板與 IDM 對標，另外驗證站點覆蓋與錯誤回報方式。
3. **建立下載可靠性測試矩陣。** 覆蓋支援／不支援 Range、斷線重連、程序終止後續傳、HTTP 錯誤、重導向、認證、磁碟滿、檔名衝突、代理故障及大量併發。用自建測試伺服器與可重現案例做回歸，避免只比較宣傳速度數字。
4. **公布能力限制與支援平台。** 明確標示 HLS 格式、協定支援、瀏覽器版本、各桌面平台安裝及 Android 限制，避免使用者把部分支援誤認為完整相容。

### P1：補齊產品功能差距

5. **確認並視需求新增 FTP/MMS 等協定。** 先評估用戶需求、維護負擔與測試方式，再決定實作順序；不要為了功能清單而引入無法穩定維護的協定。
6. **擴展網站抓取與「Download All」。** 單頁連結／常見媒體資源擷取已在功能分支實作；若需要跨頁網站抓取，再設計使用者可控的網域／檔案類型／深度限制、預覽清單與取消機制，降低誤抓與對網站造成負載的風險。
7. **擴展媒體擷取能力前先釐清範圍。** 區分標準 HLS、其他串流格式、瀏覽器可下載媒體與受 DRM 保護內容；明確定義合規邊界、錯誤呈現及維護成本。
8. **依使用情境補上下載後流程。** IDM 列有防毒掃描、流量配額、拖放及完成後動作等功能。先調查需求，再決定要整合外部防毒、提供可設定的完成動作，或維持由作業系統處理。

### P2：把功能打磨到成熟產品的使用體驗

- **擷取流程：** 若加入網站抓取或「Download All」，需提供連結預覽、檔案類型／網域／深度篩選、批次選取及取消，避免一次加入不需要的內容。
- **下載完成動作：** 評估 IDM 提到的下載後防毒掃描，以及排程完成後的關機／斷線等動作是否符合你的使用情境；列為可設定選項，並對失敗狀況給出明確提示。
- **管理與控制：** 依需求評估下載配額、拖放、自訂分類／檔名及介面設定；現有分類、佇列、搜尋與主題能力應先作差異驗證，不必重做已有功能。
- **品質與可存取性：** 為常見瀏覽器、網路中斷、代理、認證、檔名衝突及大量下載建立可重現測試；同步檢查鍵盤操作、焦點與螢幕閱讀器，而非只追求功能清單相同。

## 優先順序結論

AB 已有堅實的下載管理核心、排程與多平台封裝。最值得先投入的不是重做佇列或分類，而是：**(1) 對獨立瀏覽器擴充功能做端到端盤點與測試；(2) 建立可靠性與效能基準；(3) 釐清 FTP/MMS、影片面板、網站抓取等具體差距；(4) 再由你依使用者需求挑選值得開發的功能。** 收費、授權及銷售不在這個優先順序內。

在功能差距經過驗證前，不宜把 IDM 的每個宣傳項目直接當作 AB 的缺陷；同樣地，程式碼中存在分段、續傳或更新檢查，也不能直接推論已達到 IDM 的效能、相容性或商業服務水準。

## 來源與限制

- IDM 官方功能：[Features](https://www.internetdownloadmanager.com/features2.html)、[segmentation](https://www.internetdownloadmanager.com/support/segmentation.html)、[homepage](https://www.internetdownloadmanager.com/)。
- IDM 官方瀏覽器說明：[Chrome](https://www.internetdownloadmanager.com/register/new_faq/chrome_extension.html)、[Edge](https://www.internetdownloadmanager.com/register/new_faq/edge-extension.html)、[Opera](https://www.internetdownloadmanager.com/register/new_faq/opera-extension.html)。
- IDM 官方產品資訊：[updates](https://www.internetdownloadmanager.com/register/new_faq/idm_update.html)、[news](https://www.internetdownloadmanager.com/news.html)、[contact](https://www.internetdownloadmanager.com/contact_us.html)。
- IDM 官網上的版本、價格與功能可能隨時間和地區改變；「最高 8 倍」等數字為廠商宣稱。官方頁面未寫明的支援能力，不推斷為不存在。
- AB 的功能結論依據目前工作樹內可定位的文件與程式碼；本次沒有掃描獨立瀏覽器擴充專案、執行所有平台版本或進行效能／安全測試。
