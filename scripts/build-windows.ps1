$ErrorActionPreference = 'Stop'
# 由下方處理 Gradle 退出碼，避免 PowerShell 7 提前將非零碼轉成例外。
$PSNativeCommandUseErrorActionPreference = $false
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$exitCode = 1

Push-Location -LiteralPath $repositoryRoot
try {
    $gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
        throw "找不到 Gradle wrapper：$gradleWrapper"
    }
    $nsisExecutable = 'C:\Program Files (x86)\NSIS\makensis.exe'
    if (-not (Test-Path -LiteralPath $nsisExecutable -PathType Leaf)) {
        throw "找不到 NSIS，請安裝至既有建置流程指定的位置：$nsisExecutable"
    }

    Write-Host '正在建置 Windows 執行檔套件與安裝檔…'
    & $gradleWrapper ':desktop:app:createDistributableAppArchiveZip' ':desktop:app:createInstaller' '--console=plain'
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        exit $exitCode
    }
    $exitCode = 1

    $appImage = Join-Path $repositoryRoot 'desktop/app/build/compose/binaries/main-release/app/ABDownloadManager'
    $archivePath = Join-Path $repositoryRoot 'desktop/app/build/dist/archives/app.zip'
    $installerPath = Join-Path $repositoryRoot 'desktop/app/build/custom-installer/ABDownloadManager.exe'
    $requiredImageFiles = @('ABDownloadManager.exe', 'app/ABDownloadManager.cfg', 'runtime/bin/server/jvm.dll')
    $requiredFiles = @($requiredImageFiles | ForEach-Object { Join-Path $appImage $_ }) + @($archivePath, $installerPath)
    foreach ($path in $requiredFiles) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -eq 0) {
            throw "必要產物不存在或為空：$path"
        }
    }
    if (-not (Get-ChildItem -LiteralPath (Join-Path $appImage 'app') -Filter '*.jar' -File | Where-Object Length -gt 0 | Select-Object -First 1)) {
        throw "執行檔套件缺少非空的 app/*.jar：$appImage"
    }

    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
    try {
        $mainEntries = @($archive.Entries | Where-Object { $_.FullName.Replace('\', '/') -match '(^|/)ABDownloadManager\.exe$' })
        if ($mainEntries.Count -ne 1 -or $mainEntries[0].Length -eq 0) {
            throw 'ZIP 必須包含唯一且非空的 ABDownloadManager.exe。'
        }
        $mainEntryName = $mainEntries[0].FullName.Replace('\', '/')
        $prefix = $mainEntryName.Substring(0, $mainEntryName.Length - 'ABDownloadManager.exe'.Length)
        foreach ($relativePath in $requiredImageFiles) {
            $entry = $archive.GetEntry($prefix + $relativePath)
            if ($null -eq $entry -or $entry.Length -eq 0) {
                throw "ZIP 缺少與主程式同層的必要內容，或內容為空：$relativePath"
            }
        }
        if (-not ($archive.Entries | Where-Object {
            $_.Length -gt 0 -and $_.FullName.StartsWith($prefix, [StringComparison]::Ordinal) -and
            $_.FullName.Substring($prefix.Length) -match '^app/[^/]+\.jar$'
        } | Select-Object -First 1)) {
            throw 'ZIP 缺少與主程式同層的非空 app/*.jar。'
        }
    }
    finally {
        $archive.Dispose()
    }

    $releaseDirectory = Join-Path $repositoryRoot 'release'
    $releaseAppImage = Join-Path $releaseDirectory 'ABDownloadManager'
    $releaseArchivePath = Join-Path $releaseDirectory 'ABDownloadManager.zip'
    $releaseInstallerPath = Join-Path $releaseDirectory 'ABDownloadManager-Setup.exe'
    if ([IO.File]::Exists($releaseDirectory)) {
        throw "release 輸出路徑已是一般檔案：$releaseDirectory"
    }
    if ([IO.File]::Exists($releaseAppImage)) {
        throw "程式目錄輸出路徑已是一般檔案：$releaseAppImage"
    }
    if ([IO.Directory]::Exists($releaseArchivePath) -or [IO.Directory]::Exists($releaseInstallerPath)) {
        throw 'ZIP 或安裝檔輸出路徑已是目錄。'
    }

    [void][IO.Directory]::CreateDirectory($releaseDirectory)
    [void][IO.Directory]::CreateDirectory($releaseAppImage)
    foreach ($imageItem in Get-ChildItem -LiteralPath $appImage -Force) {
        Copy-Item -LiteralPath $imageItem.FullName -Destination $releaseAppImage -Recurse -Force
    }
    Copy-Item -LiteralPath $archivePath -Destination $releaseArchivePath -Force
    Copy-Item -LiteralPath $installerPath -Destination $releaseInstallerPath -Force

    Write-Host 'Windows 建置完成，必要產物檢查與匯出通過。'
    Write-Host "執行檔套件目錄：$releaseAppImage"
    Write-Host "主執行檔：$(Join-Path $releaseAppImage 'ABDownloadManager.exe')"
    Write-Host "ZIP 套件：$releaseArchivePath"
    Write-Host "NSIS 安裝檔：$releaseInstallerPath"
    $exitCode = 0
}
catch {
    Write-Error "Windows 建置失敗：$($_.Exception.Message)" -ErrorAction Continue
}
finally {
    Pop-Location
}
exit $exitCode
