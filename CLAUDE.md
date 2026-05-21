# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

NapMysqlTool 是一个 Windows 平台的 MySQL 启停工具，基于 JavaFX 构建桌面 GUI。为 HeavenMS-Nap（冒险岛服务端）项目提供 MySQL 的便捷管理，支持 MySQL5.7 和 PHPStudy 下的精简版 MySQL。

## 构建与运行

- **IDE**: IntelliJ IDEA 项目，无 Maven/Gradle，依赖手动管理（JAR 放在 `lib/` 目录）
- **构建**: IDEA Artifacts → Build Artifacts → NapMysqlTool.jar，输出到 `out/artifacts/NapMysqlTool/`
- **运行**: `java -jar out/artifacts/NapMysqlTool/NapMysqlTool.jar`，需要 JRE 且项目目录下需有 MySQL 文件夹（如 `mysql-8.0.39-winx64`）
- **依赖**: mysql-connector-java-8.0.28、jna-5.16.0、jna-platform-5.16.0

## 架构

包 `cn.nap`，4个核心类，无分层架构：

- **MysqlToolApp** — JavaFX Application 入口，所有 UI 构建和事件处理在此。通过 `iniProp`（Map<String, Map<String, String>>）持有全局配置，运行时通过 `config.ini` 文件读写配置
- **MysqlOperator** — MySQL 操作层，通过 `Runtime.exec()` 调用 mysqld/mysqladmin/mysqldump 命令行工具，通过 JDBC 检测连接状态，通过 `.pid` 文件判断进程
- **MysqlUtils** — 工具类，INI 文件读写、初始化脚本追踪列表读写、文件操作
- **NapTheme** — 主题管理，支持亮/暗主题切换、中英文国际化（I18n enum）、Win10/Win11 系统窗口暗色模式（JNA 调用 dwmapi/user32）

## 关键约定

- 配置文件 `config.ini` 使用中文 section 名（`mysql配置`、`工具配置`）和中文 key（`mysql路径`、`mysql账号`）
- 初始化脚本放在 `init/` 目录：文件夹名为库名，其中 `.sql` 文件按库名执行；直接放的 `.sql` 文件不指定库名
- 已执行的脚本记录在 `已初始化列表(别乱搞).txt`，避免重复执行
- 版本号格式：`2.YY.MMDD`（如 `2.25.0715`），定义在 `MysqlToolApp.start()` 中
- MySQL 命令通过 `cmd.exe /C` 执行（import/export），直接通过路径执行 start/stop
- 所有 MySQL 操作有最多3次重试机制，stop 失败最终会 `taskkill /f /pid`