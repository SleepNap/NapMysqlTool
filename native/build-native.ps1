# NapMysqlTool Native Image 构建

param(
    [ValidateSet("gui", "console")]
    [string]$Mode = "gui"
)

$PROJECT_DIR = Split-Path -Parent $PSScriptRoot
$NIK = "D:\Program Files\Java\bellsoft-liberica-vm-full-openjdk21-23.1.11\bin\native-image.cmd"
$VS = "D:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvarsall.bat"

$CP = "$PROJECT_DIR\out\production\NapMysqlTool;$PROJECT_DIR\lib\*"
$OUT = "$PROJECT_DIR\out\native\NapMysqlTool3"
$IS_GUI = $Mode -eq "gui"
$MODULE_ARGS = @(
    "--add-exports javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
    "--add-opens javafx.graphics/javafx.stage=ALL-UNNAMED",
    "--add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
    "--add-opens javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
    "--add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
    "--add-opens java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED"
) -join " "
$LINKER_ARGS = if ($IS_GUI) {
    "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS -H:NativeLinkerOption=/ENTRY:mainCRTStartup"
} else {
    ""
}

New-Item -ItemType Directory -Force -Path "$PROJECT_DIR\out\native" | Out-Null

Write-Host "Building ($Mode)..." -ForegroundColor Cyan

$bat = @"
@echo off
call "$VS" x64
"$NIK" -H:ConfigurationFileDirectories="$PSScriptRoot" -H:+AddAllCharsets -R:MaxHeapSize=128m -R:MinHeapSize=16m -R:MaxNewSize=32m --no-fallback $MODULE_ARGS $LINKER_ARGS -cp "$CP" -o "$OUT" cn.nap.ToolApp
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
"@

if ($IS_GUI) {
    $bat += @"
echo Stripping console...
editbin /SUBSYSTEM:WINDOWS "$OUT.exe" 2>&1
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
echo Console removed
"@
}

$tmp = "$env:TEMP\nap-build.bat"
[System.IO.File]::WriteAllText($tmp, $bat, [System.Text.Encoding]::Default)
cmd /c $tmp
$ok = $LASTEXITCODE
Remove-Item $tmp -ErrorAction SilentlyContinue

if ($ok -ne 0) { Write-Host "FAIL!" -ForegroundColor Red; exit 1 }

$bytes = [System.IO.File]::ReadAllBytes("$OUT.exe")
$pe = [BitConverter]::ToInt32($bytes, 0x3c)
$subsystem = [BitConverter]::ToUInt16($bytes, $pe + 24 + 68)
$expectedSubsystem = if ($IS_GUI) { 2 } else { 3 }
if ($subsystem -ne $expectedSubsystem) {
    Write-Host "FAIL: $OUT.exe subsystem is $subsystem, expected $expectedSubsystem for $Mode mode" -ForegroundColor Red
    exit 1
}

Write-Host "OK: $OUT.exe" -ForegroundColor Green
