# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

NapMysqlTool 是一个 Windows 平台的 MySQL 多实例启停管理工具，基于 JavaFX 8 构建桌面 GUI。为 HeavenMS-Nap（冒险岛服务端）项目提供 MySQL 的便捷管理，支持多 MySQL 实例的启动/停止/重启/修复/导入/导出，含亮/暗主题切换和中英文国际化。

## 构建与运行

- **IDE**: IntelliJ IDEA 项目，无 Maven/Gradle，依赖手动管理（JAR 放在 `lib/` 目录）
- **JDK**: 1.8
- **构建**: IDEA → Build → Build Artifacts → NapMysqlTool.jar，输出到 `out/artifacts/NapMysqlTool/`
- **EXE 打包**: exe4j 配置文件 `NapMysqlTool.exe4j`，入口类 `cn.nap.ToolApp`，需 JRE 1.8
- **运行**: `java -jar out/artifacts/NapMysqlTool/NapMysqlTool.jar`，项目目录下需有 MySQL 文件夹
- **依赖** (手动放在 `lib/`):
  - `jna-5.16.0.jar` / `jna-platform-5.16.0.jar` — Windows 原生 API 调用（DWM 窗口暗色模式、标题栏着色）
  - `mysql-connector-java-8.0.28.jar` — JDBC 连接检测（声明在 IDEA artifact 配置中，不在 module `.iml` 中）

## 架构

包 `cn.nap`，6 个类，单层扁平结构（无分层）：

| 类 | 行数 | 职责 |
|----|------|------|
| **ToolApp** | ~1117 | JavaFX Application 入口。包含全部 UI 构建（菜单、实例卡片、模态弹窗、表单、INI 编辑器）和全部事件处理（单实例/批量 启停/修复/导入导出） |
| **ToolComponent** | ~574 | UI 组件静态工厂。所有按钮、卡片、弹窗、进度对话框的创建和样式（纯内联 style，无 CSS 文件） |
| **ToolService** | ~355 | 业务逻辑层（单例）。配置加载/保存/解析、MySQL 进程生命周期管理、导入导出、my.ini 读写、状态刷新 |
| **ToolUtil** | ~447 | 底层工具类。INI 文件解析/写入、JNA 原生 Windows API 调用、进程管理（PID/端口/杀进程）、VC++ 运行时检测 |
| **ToolCommon** | ~291 | 常量与枚举定义。Status、ThemeColor、I18n、TipType、Theme、Language、SVG 图标路径、窗口尺寸常量、版本号 |
| **ToolConfig** | ~60 | 纯数据模型。Core（全局配置）、Instance（实例配置）、Info\<T\>（带排序和注释的泛型属性包装） |

### 数据流

```
config.ini ──(ToolUtil.readConfig)──> List<SectionObj>
    ──(ToolService.parseConfig)──> ToolConfig
        ├── Core (language, theme)
        └── List<Instance> (section名称, path, username, password, port, database, status)

ToolApp ──(读)──> ToolService.getInstance().getConfig()
ToolApp ──(写)──> ToolService.getInstance().saveConfig() ──(ToolUtil.writeConfig)──> config.ini
```

### 状态管理

实例运行时状态（`Instance.status`）为 int 枚举（`Status` 类）：
- `0` STOPPED（红点） / `1` STARTED（绿点） / `2` STARTING（黄点） / `3` STOPPING（黄点）
- 状态仅存内存，不持久化到 config.ini
- 启动时通过 `refreshStatus()` 读取 PID 文件判断实际运行状态
- 按钮启用/禁用和状态圆点颜色均依赖此状态

### 线程模型

- 所有 MySQL 操作（启动/停止/重启/修复/导入导出）均在后台线程执行（`new Thread(() -> {...}).start()`）
- UI 更新通过 `Platform.runLater()` 切回 JavaFX 线程
- 配置加载在 `ToolApp.init()` 中通过后台线程执行，`start()` 用 10 秒超时 join

### 主题与国际化

- 主题色通过 `ThemeColor.color()` 动态获取，内部根据 `ToolService.getInstance().isDark()` 返回深色或浅色 hex 值
- 所有 UI 使用内联 `setStyle()` 而非 CSS 文件
- Win11 暗色模式通过 JNA 调用 `DwmSetWindowAttribute`（属性 20）设置；Win10 通过 `SetWindowCompositionAttribute`（属性 26）+ 窗口宽度 ±1 强制重绘
- Win11 标题栏着色通过 DWM 属性 35
- 语言切换：`ToolService.changeLanguage()` 切换 zh-CN ↔ en-US，通过 `I18n.translate(language)` 获取对应文本

## 关键约定

- **配置文件** `config.ini` 使用 GBK 编码（`.idea/encodings.xml` 中指定）
- Section 名即为实例名。首个 section `[ToolConfiguration]` 为全局配置（语言/主题）
- INI 注释以 `;` 开头，支持双语注释格式：`; 中文(English)`
- 新建实例时 port 自动递增（max+1），默认 3306
- 导入导出文件命名规则：`output_<端口号>.sql`
- **版本号**：`3.YY.MMDD`，定义在 `ToolCommon.VERSION`
- MySQL 启动通过 `mysqld.exe --defaults-file=<my.ini> --port <port> --console`，监控 stderr 等待 "ready for connections"（30s 超时）
- MySQL 停止流程：读 PID 文件 → PowerShell `Stop-Process` → 删 PID 文件 → `netstat` 杀端口占用
- 修复流程：检查 VC++ 运行时 → 停止 → 清理 binlog → 删除 PID → 杀端口进程 → 全部 mysqld 杀干净
- Import/Export 通过 `cmd.exe /C` 包装执行 mysqldump/mysql 命令
- 所有操作失败时通过 `ToolComponent.error()` / `.warning()` / `.info()` 弹出模态提示
- `config.ini` 的 section/key 排序通过 `TreeMap` 保证稳定性
