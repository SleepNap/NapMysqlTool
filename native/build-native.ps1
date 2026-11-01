# Clean the entire out directory, compile current sources, and build a native EXE.
[CmdletBinding()]
param(
    [ValidateSet("gui", "console")]
    [string]$Mode = "gui"
)
$ErrorActionPreference = "Stop"
$PROJECT_DIR = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$NIK_HOME = "D:\Program Files\Java\bellsoft-liberica-vm-full-openjdk21-23.1.11"
$VS = "D:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat"
$OUT_ROOT = Join-Path $PROJECT_DIR "out"
$WORK = Join-Path $OUT_ROOT "native-work"
$DEST = Join-Path $OUT_ROOT "native"
$UTF8 = New-Object Text.UTF8Encoding($false)
foreach ($tool in @((Join-Path $NIK_HOME "bin\javac.exe"), (Join-Path $NIK_HOME "bin\jmod.exe"), (Join-Path $NIK_HOME "bin\native-image.cmd"), $VS)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "Build tool missing: $tool" }
}
try {
    $buildLock = [IO.File]::Open((Join-Path ([IO.Path]::GetTempPath()) "NapMysqlTool-native-build.lock"),
        [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
} catch { throw "Another native build is running. Close it before retrying." }
try {
    # User-requested clean build: only the exact project/out directory may be removed.
    $resolvedOut = [IO.Path]::GetFullPath($OUT_ROOT)
    $expectedOut = [IO.Path]::GetFullPath((Join-Path $PROJECT_DIR "out"))
    if (-not $resolvedOut.Equals($expectedOut, [StringComparison]::OrdinalIgnoreCase) -or
        -not $resolvedOut.StartsWith($PROJECT_DIR + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Unexpected cleanup path: $resolvedOut"
    }
    $usingOut = @(Get-CimInstance Win32_Process | Where-Object {
        ($_.ExecutablePath -and $_.ExecutablePath.StartsWith($resolvedOut + '\', [StringComparison]::OrdinalIgnoreCase)) -or
        ($_.Name -in @('java.exe', 'javaw.exe') -and $_.CommandLine -and
            ($_.CommandLine.IndexOf($resolvedOut, [StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                $_.CommandLine -match 'cn\.nap\.ToolApp'))
    })
    if ($usingOut.Count -gt 0) { throw "Close the app/debug session using out before a clean native build." }
    foreach ($directory in @($OUT_ROOT)) {
        if (Test-Path -LiteralPath $directory) {
            if (((Get-Item -LiteralPath $directory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw "Refusing to clean through a link: $directory"
            }
        }
    }
    if (Test-Path -LiteralPath $OUT_ROOT) {
        $links = @(Get-ChildItem -LiteralPath $OUT_ROOT -Force -Recurse |
            Where-Object { ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0 })
        if ($links.Count -gt 0) { throw "Output directory contains a link; cleanup stopped." }
        Write-Host "Deleting entire output directory: $resolvedOut" -ForegroundColor Yellow
        Remove-Item -LiteralPath $resolvedOut -Recurse -Force
    }
    $classes = Join-Path $WORK "classes"
    $staging = Join-Path $WORK "output"
    New-Item -ItemType Directory -Path $classes, $staging -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $PROJECT_DIR "src") -Filter "*.java" -Recurse |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    if ($sources.Count -eq 0) { throw "No Java sources found." }
    $argFile = Join-Path $WORK "sources.args"
    [IO.File]::WriteAllLines($argFile, [string[]]$sources, $UTF8)
    Write-Host "Compiling current Java sources..." -ForegroundColor Cyan
    & (Join-Path $NIK_HOME "bin\javac.exe") -encoding UTF-8 -cp "$PROJECT_DIR\lib\*" --add-modules javafx.controls -d $classes ("@" + $argFile)
    if ($LASTEXITCODE -ne 0) { throw "Java compilation failed: $LASTEXITCODE" }
    $meta = Join-Path $PROJECT_DIR "src\META-INF"
    if (Test-Path -LiteralPath $meta) { Copy-Item -LiteralPath $meta -Destination $classes -Recurse }
    $cp = "$classes;$PROJECT_DIR\lib\*"
    $binary = Join-Path $staging "NapMysqlTool"
    $moduleArgs = @(
        "--add-exports javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
        "--add-opens javafx.graphics/javafx.stage=ALL-UNNAMED",
        "--add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
        "--add-opens javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
        "--add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
        "--add-opens java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED"
    ) -join " "
    $linker = if ($Mode -eq "gui") {
        "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS -H:NativeLinkerOption=/ENTRY:mainCRTStartup"
    } else { "" }
    $nativeCommand = 'call "{0}\bin\native-image.cmd" -H:ConfigurationFileDirectories="{1}" -H:+AddAllCharsets -R:MaxHeapSize=128m -R:MinHeapSize=16m -R:MaxNewSize=32m --no-fallback {2} {3} -cp "{4}" -o "{5}" cn.nap.ToolApp' -f $NIK_HOME, $PSScriptRoot, $moduleArgs, $linker, $cp, $binary
    if ($nativeCommand.Length -gt 7800) { throw "Native command is too long for cmd.exe." }
    $log = Join-Path $WORK "native-build.log"
    $batch = Join-Path $WORK "build.cmd"
    # CALL returns from native-image.cmd so the actual exit code is checked.
    $batchLines = @(
        "@echo off",
        ('call "{0}" x64 > "{1}" 2>&1' -f $VS, (Join-Path $WORK "vcvars.log")),
        "if errorlevel 1 exit /b %errorlevel%",
        ($nativeCommand + (' > "{0}" 2>&1' -f $log)),
        "exit /b %errorlevel%"
    )
    [IO.File]::WriteAllLines($batch, [string[]]$batchLines, [Text.Encoding]::Default)
    Write-Host "Building native image ($Mode); log: $log" -ForegroundColor Cyan
    & $env:ComSpec /d /c ('"' + $batch + '"')
    $nativeExit = $LASTEXITCODE
    if (Test-Path -LiteralPath $log) {
        Get-Content -LiteralPath $log | ForEach-Object { Write-Host $_ }
    } elseif (Test-Path -LiteralPath (Join-Path $WORK "vcvars.log")) {
        Get-Content -LiteralPath (Join-Path $WORK "vcvars.log") | ForEach-Object { Write-Host $_ }
    }
    if ($nativeExit -ne 0) { throw "Native Image failed: $nativeExit. See build logs in $WORK." }
    $exe = "$binary.exe"
    if (-not (Test-Path -LiteralPath $exe -PathType Leaf)) {
        throw "Native Image returned without creating $exe. See $log."
    }
    $bytes = [IO.File]::ReadAllBytes($exe)
    $pe = [BitConverter]::ToInt32($bytes, 0x3c)
    $machine = [BitConverter]::ToUInt16($bytes, $pe + 4)
    $subsystem = [BitConverter]::ToUInt16($bytes, $pe + 24 + 68)
    $expectedSubsystem = if ($Mode -eq "gui") { 2 } else { 3 }
    if ($machine -ne 0x8664 -or $subsystem -ne $expectedSubsystem) {
        throw "Unexpected PE architecture/subsystem."
    }
    # NIK statically links Glass/D3D, but software-renderer DLLs must be shipped separately.
    # Use matching module DLLs; do not add a second dynamic Glass implementation.
    $fxModule = Join-Path $WORK "javafx-module"
    & (Join-Path $NIK_HOME "bin\jmod.exe") extract --dir $fxModule (Join-Path $NIK_HOME "jmods\javafx.graphics.jmod")
    if ($LASTEXITCODE -ne 0) { throw "Cannot extract JavaFX software-renderer libraries." }
    foreach ($dllName in @('prism_sw.dll', 'decora_sse.dll')) {
        Copy-Item -LiteralPath (Join-Path $fxModule ('lib\' + $dllName)) -Destination $staging
    }
    $requiredNames = @('NapMysqlTool.exe', 'awt.dll', 'fontmanager.dll', 'freetype.dll', 'java.dll',
        'javaaccessbridge.dll', 'jawt.dll', 'jvm.dll', 'lcms.dll', 'prism_sw.dll', 'decora_sse.dll')
    foreach ($requiredName in $requiredNames) {
        if (-not (Test-Path -LiteralPath (Join-Path $staging $requiredName) -PathType Leaf)) {
            throw "Native output is missing $requiredName."
        }
    }
    $artifacts = @(Get-ChildItem -LiteralPath $staging -File | Where-Object { $_.Extension -in @(".exe", ".dll") })
    $running = @(Get-CimInstance Win32_Process | Where-Object {
        $_.ExecutablePath -and ([IO.Path]::GetDirectoryName($_.ExecutablePath) -eq $DEST)
    })
    if ($running.Count -gt 0) { throw "An EXE in $DEST is running. Build completed in $staging; close that EXE before publishing." }
    New-Item -ItemType Directory -Path $DEST -Force | Out-Null
    if (((Get-Item -LiteralPath $DEST -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Refusing to publish through a directory link: $DEST"
    }
    foreach ($artifact in $artifacts) { Copy-Item -LiteralPath $artifact.FullName -Destination $DEST }
    Write-Host "OK: $DEST\NapMysqlTool.exe ($Mode, Windows x64)" -ForegroundColor Green
} finally {
    $buildLock.Dispose()
}
