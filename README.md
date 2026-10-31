<div align="center">
  <a href="https://abdownloadmanager.com" target="_blank">
    <img width="180" src="assets/logo/app_logo_with_background.svg" alt="AB Download Manager Logo">
  </a>
</div>
<h1 align="center">AB Download Manager</h1>
<p align="center">
    <a href="https://github.com/amir1376/ab-download-manager/releases/latest"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/amir1376/ab-download-manager?color=greenlight&label=latest%20release"></a>
    <a href="https://abdownloadmanager.com"><img alt="AB Download Manager Website" src="https://img.shields.io/badge/project-website-purple?&labelColor=gray"></a>
    <a href="https://t.me/abdownloadmanager"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue?logo=telegram&labelColor=gray"></a>
    <a href="https://t.me/abdownloadmanager_discussion"><img alt="Telegram Group" src="https://img.shields.io/badge/Telegram-Group-blue?logo=telegram&labelColor=gray"></a>
    <a href="https://crowdin.com/project/ab-download-manager"><img alt="Crowdin" src="https://badges.crowdin.net/ab-download-manager/localized.svg"></a>
</p>

<a href="https://abdownloadmanager.com" target="_blank">
    <img alt="AB Download Manager Banner" src="assets/banners/app_banner.png"/>
</a>

## Introduction

[AB Download Manager](https://abdownloadmanager.com) is a desktop app that helps you manage and organize your downloads more efficiently than ever before.

## Features

- ⚡️ Faster Download Speed
- ⏰ Queues and Schedulers
- 🌐 Browser Extensions
- 💻 Multiplatform (Android / Windows / Linux / Mac)
- 🌙 Multiple Themes (Dark/Light/Black and more) with modern UI
- ❤️ Free and Open Source

Please visit [Project Website](https://abdownloadmanager.com) for more info.

## Installation

<a href="https://github.com/amir1376/ab-download-manager/releases/latest"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/amir1376/ab-download-manager?color=greenlight&label=latest%20release"></a>

### Download and Install the App

<a href="https://abdownloadmanager.com"><img src="https://img.shields.io/badge/Official%20Website-897BFF?logo=abdownloadmanager&logoColor=fff&style=flat-square" alt="Official Website" height="32" /></a>
<a href="https://github.com/amir1376/ab-download-manager/releases/latest"><img src="https://img.shields.io/badge/GitHub%20Releases-2a2f36?logo=github&logoColor=fff&style=flat-square" alt="GitHub Releases" height="32" /></a>

#### Installation script (Linux)

The recommended way to install the app on Linux

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/amir1376/ab-download-manager/master/scripts/install.sh)
```

#### Winget or Scoop (for Windows)

**winget**:

```bash
winget install amir1376.ABDownloadManager
```

**scoop**:

```bash
scoop install extras/abdownloadmanager
```

#### Homebrew (for macOS & Linux)

```bash
brew tap amir1376/tap && brew install --cask ab-download-manager
```

> ⚠️ **Warning:** This software is NOT on Google Play or other app stores unless listed here. Any version **claiming to be or related to this project** should be considered SCAM and UNSAFE.

For alternative installation methods, uninstallation instructions, and more details, please refer to the [wiki](https://github.com/amir1376/ab-download-manager/wiki/) page.

### Browser Extensions

<a href="https://addons.mozilla.org/en-US/firefox/addon/ab-download-manager/"><img src="https://img.shields.io/amo/v/ab-download-manager?label=Firefox&logo=firefoxbrowser" alt="Firefox Add-ons Version"></a>
<a href="https://chromewebstore.google.com/detail/bbobopahenonfdgjgaleledndnnfhooj"> <img src="https://img.shields.io/chrome-web-store/v/bbobopahenonfdgjgaleledndnnfhooj?label=Chrome&logo=googlechrome" alt="Chrome Web Store Version"></a>

You can download the browser extension to integrate the app with your browser.

<p align="left">
<a href="https://addons.mozilla.org/firefox/addon/ab-download-manager/">
    <picture>
        <img alt="Chrome Extension" src="./assets/banners/firefox-extension.png" height="48">
    </picture>
</a>
<a href="https://chromewebstore.google.com/detail/bbobopahenonfdgjgaleledndnnfhooj">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="./assets/banners/chrome-extension_dark.png" height="48">
        <source media="(prefers-color-scheme: light)" srcset="./assets/banners/chrome-extension_light.png" height="48">
        <img alt="Chrome Extension" src="./assets/banners/chrome-extension_light.png" height="48">
    </picture>
</a>
</p>

## Screenshots

<div align="center">
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./assets/screenshots/app-home_dark.png">
  <source media="(prefers-color-scheme: light)" srcset="./assets/screenshots/app-home_light.png">
  <img alt="App Home Section" src="./assets/screenshots/app-home_dark.png">
</picture>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./assets/screenshots/app-download_dark.png">
  <source media="(prefers-color-scheme: light)" srcset="./assets/screenshots/app-download_light.png">
  <img alt="App Download Section" src="./assets/screenshots/app-download_dark.png">
</picture>
</div>

## Community

- 📢 [Telegram Channel](https://t.me/abdownloadmanager) – News, announcements, and release updates.
- 💬 [Telegram Group](https://t.me/abdownloadmanager_discussion) – Get help, report bugs, suggest features, and discuss
  the project with the community.

## Repositories And Source Code

There are multiple repositories related to the **AB Download Manager** project:

| Repository                                                                                 | Description                                                                   |
|--------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| [Main Application](https://github.com/amir1376/ab-download-manager) (You are here)         | Contains the  **Application** that runs on your  **device**                   |
| [Browser Integration](https://github.com/amir1376/ab-download-manager-browser-integration) | Contains the **Browser Extension** to be installed on your  **browser**       |
| [Website](https://github.com/amir1376/ab-download-manager-website)                         | Contains the **AB Download Manager** [website](https://abdownloadmanager.com) |

I've spent a lot of time to create this project.

If you like my work, please consider giving it a ⭐ — thanks! ❤️

## Bug Report

If you notice any bugs in the source code, please report them via the `GitHub Issues` section.

## Build From Source

To compile and test the desktop app on your local machine,
follow these steps:

1. Clone the project.
2. Download and extract the [JBR](https://github.com/JetBrains/JetBrainsRuntime/releases), and make it available by either:
    
    - Adding it to your `PATH`, or
    - Setting the `JAVA_HOME` environment variable to its installation path.
  
3. Navigate to the project directory, open your terminal and execute the following command:

    ```bash
    ./gradlew createReleaseFolderForCi
    ```

4. The output will be available at:

    ```
    <project_dir>/build/ci-release
    ```

> **Note**. This project is compiled and published by GitHub actions [here](./.github/workflows/publish.yml), so if you
> faced any problem you can check that too.

### Windows 本機建置與打包

在 Windows 上開啟終端機，於專案根目錄執行：

```powershell
.\build-windows.cmd
```

也可從其他工作目錄使用 `build-windows.cmd` 的完整路徑，或直接執行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1`。腳本不接受參數。

建置需要可啟動 Gradle wrapper 的 Java、已安裝且可由 Gradle 辨識的 Java 25 toolchain，以及安裝在 `C:\Program Files (x86)\NSIS\makensis.exe` 的 NSIS。可將 `JAVA_HOME` 指向 JDK 或 JBR 25，toolchain 仍由專案既有 Gradle 設定解析；首次執行需要網路下載 Gradle 與相依套件。入口使用 Windows 內建的 Windows PowerShell 5.1。

腳本會顯示建置進度，一次執行 `:desktop:app:createDistributableAppArchiveZip` 與 `:desktop:app:createInstaller`。成功檢查必要產物後，會將檔案複製到相對於腳本所在專案根目錄的 `release/`，並列出下列完整路徑：

- 執行檔套件目錄 `<專案根目錄>/release/ABDownloadManager/`，其中主程式是 `ABDownloadManager.exe`。
- ZIP 套件 `<專案根目錄>/release/ABDownloadManager.zip`。
- NSIS 安裝檔 `<專案根目錄>/release/ABDownloadManager-Setup.exe`。

使用執行檔套件時，請保留整個資料夾及其 `app`、`runtime` 內容。建置失敗會傳回非零退出碼，Gradle 失敗時保留原始退出碼。同一工作樹請循序建置；重跑會更新對應的 release 產物，不會清理其他檔案或 Gradle 中間建置目錄。`release/ABDownloadManager/` 中已不再由本次建置產生的舊檔也會保留。腳本不會啟動程式或執行安裝。

腳本的行為自我檢查可用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-build-windows.ps1` 執行。它使用假 Gradle 與小型 fixture，無須實際建置或安裝 NSIS；每次的 fixture 都保留在已忽略的 `build/windows-package-self-check/`。

## Translations

<a href="https://crowdin.com/project/ab-download-manager"><img alt="Crowdin" src="https://badges.crowdin.net/ab-download-manager/localized.svg"></a>

If you’d like to help translate AB Download Manager into another language, or improve existing translations, you can do
so on Crowdin. Here’s how:

- Visit the project in [Crowdin](https://crowdin.com/project/ab-download-manager)
- Please DO NOT submit translations via pull requests.
- If you want to add a new language, please see [this](https://github.com/amir1376/ab-download-manager/issues/144).

## Contribution

If you want to contribute to this project, please read [Contributing Guide](CONTRIBUTING.md) first.

## Support the Project

If you'd like to support the project, you can find details on how to donate in the [DONATE.md](DONATE.md) file.
