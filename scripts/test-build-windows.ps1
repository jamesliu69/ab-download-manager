$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
# 模擬呼叫端啟用原生命令錯誤處理，確認正式腳本仍保留 Gradle 退出碼。
$PSNativeCommandUseErrorActionPreference = $true
# 每次執行保留獨立 fixture，避免覆寫先前的檢查證據。
$fixtureRoot = Join-Path (Split-Path -Parent $PSScriptRoot) ('build/windows-package-self-check/' + [Guid]::NewGuid().ToString('N'))
# 所有 fixture 都複製同一份正式腳本，僅替換外部 Gradle 與 NSIS 存在檢查。
$sourceScript = Join-Path $PSScriptRoot 'build-windows.ps1'

function Test-Path {
    <# .SYNOPSIS
    僅模擬固定 NSIS 路徑存在，其餘檢查交回原生 cmdlet。
    #>
    param([string]$LiteralPath, [string]$PathType)
    if ($LiteralPath -eq 'C:\Program Files (x86)\NSIS\makensis.exe') {
        return $true
    }
    Microsoft.PowerShell.Management\Test-Path -LiteralPath $LiteralPath -PathType $PathType
}

function New-BuildFixture {
    <# .SYNOPSIS
    建立含空白路徑的獨立專案、假 Gradle 及指定缺陷的產物。
    #>
    param([string]$Case)
    $project = Join-Path $fixtureRoot ($Case + ' project with spaces')
    $scripts = New-Item -ItemType Directory -Path (Join-Path $project 'scripts') -Force
    Copy-Item -LiteralPath $sourceScript -Destination (Join-Path $scripts.FullName 'build-windows.ps1')
    $gradleCode = if ($Case -eq 'gradle-failure') { 37 } else { 0 }
    $gradle = "@echo off`r`n>> `"%~dp0gradle-call.txt`" echo %CD%`r`n>> `"%~dp0gradle-call.txt`" echo %*`r`nexit /b $gradleCode`r`n"
    [IO.File]::WriteAllText((Join-Path $project 'gradlew.bat'), $gradle, [Text.Encoding]::ASCII)

    $appRelative = 'desktop/app/build/compose/binaries/main-release/app/ABDownloadManager/'
    $files = @(
        $appRelative + 'ABDownloadManager.exe'
        $appRelative + 'runtime/bin/server/jvm.dll'
        $appRelative + 'app/ABDownloadManager.cfg'
        'desktop/app/build/custom-installer/ABDownloadManager.exe'
        $appRelative + 'app/application.jar'
        $appRelative + 'app/resources/test-resource.bin'
    )
    foreach ($relative in $files) {
        if (($Case -eq 'missing-main' -and $relative -eq $files[0]) -or
            ($Case -eq 'missing-installer' -and $relative -eq $files[3]) -or
            ($Case -eq 'missing-jar' -and $relative -eq $files[4])) {
            continue
        }
        $path = Join-Path $project $relative
        [void][IO.Directory]::CreateDirectory((Split-Path -Parent $path))
        $empty = ($Case -eq 'empty-runtime' -and $relative -eq $files[1]) -or
            ($Case -eq 'empty-app' -and $relative -eq $files[2]) -or
            ($Case -eq 'empty-installer' -and $relative -eq $files[3])
        $bytes = [byte[]]@(1)
        if ($empty) { $bytes = [byte[]]@() }
        [IO.File]::WriteAllBytes($path, $bytes)
    }

    $archivePath = Join-Path $project 'desktop/app/build/dist/archives/app.zip'
    [void][IO.Directory]::CreateDirectory((Split-Path -Parent $archivePath))
    if ($Case -eq 'missing-zip') {
        return $project
    }
    if ($Case -eq 'broken-zip') {
        [IO.File]::WriteAllBytes($archivePath, [byte[]]@(1))
        return $project
    }
    $prefix = if ($Case -eq 'success-root') { '' } else { 'portable/ABDownloadManager/' }
    $entries = @('ABDownloadManager.exe', 'runtime/bin/server/jvm.dll', 'app/ABDownloadManager.cfg', 'app/application.jar')
    $archive = [IO.Compression.ZipFile]::Open($archivePath, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in $entries) {
            if (($Case -eq 'zip-missing-runtime' -and $entry.StartsWith('runtime/')) -or
                ($Case -eq 'zip-missing-jar' -and $entry.EndsWith('.jar'))) {
                continue
            }
            $entryPrefix = if ($Case -eq 'zip-wrong-prefix' -and $entry.StartsWith('runtime/')) { 'elsewhere/' } else { $prefix }
            $stream = $archive.CreateEntry($entryPrefix + $entry).Open()
            try { $stream.WriteByte(1) } finally { $stream.Dispose() }
        }
        if ($Case -eq 'zip-duplicate-main') {
            $stream = $archive.CreateEntry('other/ABDownloadManager.exe').Open()
            try { $stream.WriteByte(1) } finally { $stream.Dispose() }
        }
    }
    finally {
        $archive.Dispose()
    }
    if ($Case -eq 'release-is-file') {
        [IO.File]::WriteAllText((Join-Path $project 'release'), 'keep this file')
    }
    return $project
}

function Get-DirectoryHash {
    <# .SYNOPSIS
    以相對路徑與 SHA256 彙整目錄內所有檔案，供匯出內容讀回比對。
    #>
    param([string]$Path)
    $root = (Resolve-Path -LiteralPath $Path).Path.TrimEnd('\')
    $entries = @(
        Get-ChildItem -LiteralPath $Path -File -Recurse -Force | Sort-Object FullName | ForEach-Object {
            $relativePath = $_.FullName.Substring($root.Length).TrimStart('\')
            '{0}:{1}' -f $relativePath, (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    )
    return $entries -join [Environment]::NewLine
}

function Assert-BuildResult {
    <# .SYNOPSIS
    執行正式腳本，確認退出碼、Gradle 呼叫、release 內容、成功訊息與工作目錄還原。
    #>
    param([string]$Case, [int]$ExpectedExitCode)
    $project = New-BuildFixture $Case
    $originalLocation = (Get-Location).Path
    $output = @(& (Join-Path $project 'scripts/build-windows.ps1') *>&1)
    $actualExitCode = $LASTEXITCODE
    if ($actualExitCode -ne $ExpectedExitCode) {
        throw "$Case 退出碼應為 $ExpectedExitCode，實際為 $actualExitCode。輸出：$($output -join [Environment]::NewLine)"
    }
    if ((Get-Location).Path -ne $originalLocation) {
        throw "$Case 未還原呼叫端工作目錄。"
    }
    if (-not $PSNativeCommandUseErrorActionPreference) {
        throw "$Case 改變了呼叫端的原生命令錯誤處理設定。"
    }
    $call = @(Get-Content -LiteralPath (Join-Path $project 'gradle-call.txt'))
    if ($call.Count -ne 2 -or $call[0] -ne $project -or
        $call[1] -ne ':desktop:app:createDistributableAppArchiveZip :desktop:app:createInstaller --console=plain') {
        throw "$Case 必須從專案根目錄以指定參數呼叫 Gradle 一次。"
    }
    $reportedSuccess = ($output -join [Environment]::NewLine).Contains('Windows 建置完成')
    if ($reportedSuccess -ne ($ExpectedExitCode -eq 0)) {
        throw "$Case 的完成訊息與退出碼不一致。"
    }
    if ($ExpectedExitCode -eq 0) {
        $releaseDirectory = Join-Path $project 'release'
        $releaseAppImage = Join-Path $releaseDirectory 'ABDownloadManager'
        $releaseArchivePath = Join-Path $releaseDirectory 'ABDownloadManager.zip'
        $releaseInstallerPath = Join-Path $releaseDirectory 'ABDownloadManager-Setup.exe'
        $sourceAppImage = Join-Path $project 'desktop/app/build/compose/binaries/main-release/app/ABDownloadManager'
        $sourceArchivePath = Join-Path $project 'desktop/app/build/dist/archives/app.zip'
        $sourceInstallerPath = Join-Path $project 'desktop/app/build/custom-installer/ABDownloadManager.exe'
        $reportedOutput = $output -join [Environment]::NewLine
        if (-not $reportedOutput.Contains($releaseDirectory)) {
            throw "$Case 未顯示 release 產物完整路徑。"
        }
        if (-not (Test-Path -LiteralPath $releaseAppImage -PathType Container) -or
            -not (Test-Path -LiteralPath $releaseArchivePath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $releaseInstallerPath -PathType Leaf)) {
            throw "$Case 未產生完整的 release 輸出。"
        }
        if ((Get-DirectoryHash $sourceAppImage) -ne (Get-DirectoryHash $releaseAppImage)) {
            throw "$Case release 程式目錄與來源內容雜湊不符。"
        }
        if ((Get-FileHash -LiteralPath $sourceArchivePath -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $releaseArchivePath -Algorithm SHA256).Hash) {
            throw "$Case release ZIP 與來源內容雜湊不符。"
        }
        if ((Get-FileHash -LiteralPath $sourceInstallerPath -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $releaseInstallerPath -Algorithm SHA256).Hash) {
            throw "$Case release 安裝檔與來源內容雜湊不符。"
        }
    }
    elseif ($Case -eq 'release-is-file') {
        $releasePath = Join-Path $project 'release'
        if (-not (Test-Path -LiteralPath $releasePath -PathType Leaf) -or
            (Get-Content -Raw -LiteralPath $releasePath) -ne 'keep this file') {
            throw 'release 路徑衝突時應保留原一般檔案。'
        }
    }
    elseif (Test-Path -LiteralPath (Join-Path $project 'release') -PathType Any) {
        throw "$Case 失敗時不應先建立 release。"
    }
    if ($Case -eq 'success') {
        $releaseAppImage = Join-Path $project 'release/ABDownloadManager'
        $unrelatedPath = Join-Path $project 'release/keep.txt'
        [IO.File]::WriteAllText($unrelatedPath, 'keep this file')
        [IO.File]::WriteAllBytes((Join-Path $releaseAppImage 'ABDownloadManager.exe'), [byte[]]@(2, 3))
        [IO.File]::WriteAllBytes((Join-Path $project 'release/ABDownloadManager.zip'), [byte[]]@(4, 5))
        [IO.File]::WriteAllBytes((Join-Path $project 'release/ABDownloadManager-Setup.exe'), [byte[]]@(6, 7))
        $rerunOutput = @(& (Join-Path $project 'scripts/build-windows.ps1') *>&1)
        $rerunExitCode = $LASTEXITCODE
        if ($rerunExitCode -ne 0 -or -not ($rerunOutput -join [Environment]::NewLine).Contains('Windows 建置完成')) {
            throw "重跑未成功匯出：$($rerunOutput -join [Environment]::NewLine)"
        }
        if ((Get-Location).Path -ne $originalLocation -or -not $PSNativeCommandUseErrorActionPreference) {
            throw '重跑改變呼叫端工作目錄或原生命令錯誤處理設定。'
        }
        if (Test-Path -LiteralPath (Join-Path $releaseAppImage 'ABDownloadManager') -PathType Any) {
            throw '重跑產生巢狀 ABDownloadManager 目錄。'
        }
        if (-not (Test-Path -LiteralPath $unrelatedPath -PathType Leaf) -or
            (Get-Content -Raw -LiteralPath $unrelatedPath) -ne 'keep this file') {
            throw '重跑移除或覆寫了其他 release 檔案。'
        }
        if ((Get-DirectoryHash (Join-Path $project 'desktop/app/build/compose/binaries/main-release/app/ABDownloadManager')) -ne
            (Get-DirectoryHash $releaseAppImage)) {
            throw '重跑後 release 程式目錄與來源內容雜湊不符。'
        }
        if ((Get-FileHash -LiteralPath (Join-Path $project 'desktop/app/build/dist/archives/app.zip') -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath (Join-Path $project 'release/ABDownloadManager.zip') -Algorithm SHA256).Hash) {
            throw '重跑後 release ZIP 與來源內容雜湊不符。'
        }
        if ((Get-FileHash -LiteralPath (Join-Path $project 'desktop/app/build/custom-installer/ABDownloadManager.exe') -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath (Join-Path $project 'release/ABDownloadManager-Setup.exe') -Algorithm SHA256).Hash) {
            throw '重跑後 release 安裝檔與來源內容雜湊不符。'
        }
        $call = @(Get-Content -LiteralPath (Join-Path $project 'gradle-call.txt'))
        if ($call.Count -ne 4 -or $call[2] -ne $project -or
            $call[3] -ne ':desktop:app:createDistributableAppArchiveZip :desktop:app:createInstaller --console=plain') {
            throw '重跑必須再次從專案根目錄以指定參數呼叫 Gradle。'
        }
    }
    Write-Host "通過：$Case"
}

Assert-BuildResult 'success' 0
Assert-BuildResult 'success-root' 0
Assert-BuildResult 'release-is-file' 1
Assert-BuildResult 'gradle-failure' 37
foreach ($case in @('missing-main', 'missing-zip', 'missing-installer', 'missing-jar', 'zip-missing-jar', 'empty-installer', 'empty-runtime', 'empty-app', 'zip-missing-runtime', 'zip-wrong-prefix', 'zip-duplicate-main', 'broken-zip')) {
    Assert-BuildResult $case 1
}
Write-Host "Windows 建置腳本自我檢查通過。Fixture 保留於：$fixtureRoot"
