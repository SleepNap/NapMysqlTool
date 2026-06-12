# 用 native-image-agent 收集反射/JNI/资源/代理配置
# 运行前确保 IDEA 已编译项目（Build → Build Project）
# 运行后请操作 APP 的所有功能（启停、切换主题/语言、导入导出、修复等）
# 最后正常关闭 APP，agent 会自动写入配置文件

$JAVA_HOME = "D:\Program Files\Java\bellsoft-liberica-vm-full-openjdk21-23.1.11"
$PROJECT_DIR = Split-Path -Parent $PSScriptRoot
$CONFIG_OUT = $PSScriptRoot
$CP = "$PROJECT_DIR\out\production\NapMysqlTool;$PROJECT_DIR\lib\*"

New-Item -ItemType Directory -Force -Path $CONFIG_OUT | Out-Null

$javaArgs = @(
    "-agentlib:native-image-agent=config-output-dir=$CONFIG_OUT",
    "--add-exports", "javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
    "--add-opens", "javafx.graphics/javafx.stage=ALL-UNNAMED",
    "--add-opens", "javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
    "--add-opens", "javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
    "--add-opens", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens", "javafx.base/com.sun.javafx.event=ALL-UNNAMED",
    "-cp", $CP,
    "cn.nap.ToolApp"
)

& "$JAVA_HOME\bin\java.exe" @javaArgs
