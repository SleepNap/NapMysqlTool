# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

NapMysqlTool 是一个 Windows 平台的 MySQL 多实例启停管理工具，基于 JavaFX 21（Liberica Full JDK 内置）构建桌面 GUI。为 HeavenMS-Nap（冒险岛服务端）项目提供 MySQL 的便捷管理，支持多 MySQL 实例的启动/停止/重启/修复/导入/导出，含亮/暗主题切换和中英文国际化。

## 构建与运行

- **IDE**: IntelliJ IDEA 项目，无 Maven/Gradle，依赖手动管理（JAR 放在 `lib/` 目录）
- **JDK**: [Liberica Native Image Kit Full 23.1.11+1](https://bell-sw.com/pages/downloads/native-image-kit/?version=23&version-annual=21&vtabs=true)（内置 JavaFX 和 Native Image）
- **编译**: IDEA → Build → Build Project，class 输出到 `out/production/NapMysqlTool/`
- **运行** (开发调试): `java -cp "out/production/NapMysqlTool;lib/*" cn.nap.ToolApp`，项目目录下需有 MySQL 文件夹
- **EXE 打包**: 通过 `native/build-native.ps1` 进行 Native Image 编译（详见下方"Native Image 打包"）
- **依赖** (手动放在 `lib/`):
  - `jna-5.16.0.jar` / `jna-platform-5.16.0.jar` — Windows 原生 API 调用（DWM 窗口暗色模式、标题栏着色）

### Native Image 打包

- 打包脚本 `native/build-native.ps1`，需先安装 **Visual Studio**（MSVC v143 生成工具）和 **Liberica NIK Full**，脚本顶部硬编码了 `$NIK` 和 `$VS` 路径，需按实际安装位置修改
- 按用户要求，每次打包先删除整个 `out/`，再从源码编译到 `out/native-work/classes`。执行前必须关闭使用 `out` 的工具/调试进程；需保留的旧包、日志自行移到 `tmp/` 或桌面。最终 EXE/DLL 输出到 `out/native/`。
- 可双击 `native/build-native.cmd` 启动；PS1 双击行为由 Windows 文件关联决定。
- 默认构建 GUI 版：`.\native\build-native.ps1` 或 `.\native\build-native.ps1 -Mode gui`
- 调试构建 console 版：`.\native\build-native.ps1 -Mode console`；console 版通过 `cmd .\NapMysqlTool.exe` 启动时可看到 `System.out`/`System.err`
- 软件渲染须保留 SWPipeline/PSWRenderer 反射、Pisces JNI 与 prism_sw.dll/decora_sse.dll 资源，不能只采集 D3D；Native 启动验收须覆盖 render=sw。软件管线需随包提供 NIK javafx.graphics.jmod 中的 prism_sw.dll、decora_sse.dll；不要额外打入已静态链接的 glass.dll 等另一套动态库。
- GUI 版去控制台依赖 native linker 参数 `/SUBSYSTEM:WINDOWS` 与 `/ENTRY:mainCRTStartup`；缺少入口参数会因找不到 `WinMain` 链接失败
- 脚本末尾会读取 PE 头校验 Subsystem：GUI 期望 `2`，console 期望 `3`
- `out/native` 下的 JDK/JavaFX 伴随 DLL（如 `java.dll`、`jvm.dll`、`fontmanager.dll`、`freetype.dll`、`lcms.dll` 等）发布时建议与 exe 同目录保留；本机单 exe 能跑不代表干净机器也稳定
- 干净 Windows 环境上若出现 `MissingReflectionRegistrationError`、`NoSuchMethodException`、`NoSuchMethodError`，优先检查 `native/*-config.json`。JNA 的 `Kernel32` 需要 proxy metadata；Win10 暗色模式链路需要 `HMODULE`/`HINSTANCE`/`HANDLE` 等反射 metadata；Windows UIA/Accessibility 触发时需要 `WinAccessible`/`WinTextRangeProvider`/`WinVariant` 的 JNI metadata

## 架构

包 `cn.nap`，8 个类，单层扁平结构（无分层）：

| 类 | 行数 | 职责 |
|----|------|------|
| **ToolApp** | ~230 | JavaFX Application 入口。决定渲染管线、创建无边框窗口、持有运行态（Running/IniSession/日志）、后台执行实例操作 |
| **MainView** | ~1060 | 4.x 主界面。自绘标题栏、实例列表（滚动）、底栏三态、行内菜单、表单/确认/日志/设置浮层、my.ini 外部编辑会话 |
| **WindowBehavior** | ~250 | 无边框窗口行为：拖动、八向缩放、双击最大化、多屏可视区约束 |
| **ToolComponent** | ~420 | v2 组件工厂：文字按钮、SVG 描边图标（固定 24×24 画布）、圆点、浮层 sheet、菜单、分段控件、输入框 |
| **ToolService** | ~450 | 业务逻辑层（单例）。配置加载/保存/解析、进程生命周期、导入导出、PID/启动时间、`loadFailed` 只读保护 |
| **ToolUtil** | ~600 | INI 读写（GBK）、JNA 原生调用（注册表版本号、Toolhelp32、TerminateProcess、GetExtendedTcpTable、DWM）、外部编辑器 |
| **ToolCommon** | ~330 | 常量与枚举：Status、ThemeColor（明/暗两套 token）、I18n、尺寸常量、SVG 图标、版本号 |
| **ToolConfig** | ~65 | 纯数据模型。Core（语言/主题/渲染/编辑器）、Instance、Info\<T\> |

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
- `0` STOPPED（灰） / `1` STARTED（绿） / `2` STARTING（琥珀） / `3` STOPPING（琥珀）——红色只留给失败与危险操作
- 状态仅存内存，不持久化到 config.ini
- 启动时通过 `refreshStatus()` 读 PID 文件判断；PID 存活用 `OpenProcess`，不起子进程
- 行内主按钮文字与可用性、状态点颜色都依赖此状态

### 线程模型

- 配置在 `ToolApp.init()` 里同步加载（只有注册表 + OpenProcess，毫秒级），不再 `join(10000)`
- 所有 MySQL 操作走 `ToolApp.startOperation(...)`：置 STARTING/STOPPING → 后台线程执行 → `Platform.runLater` 落状态并 `view.render()`
- 视图是状态驱动的：`MainView.render()` 按模型重建列表/底栏，不做节点级增量刷新
- 1 秒一次的 `Timeline` 只在有操作或 my.ini 会话时运行，用于刷时长与检测文件保存

### 主题、渲染与窗口

- `ThemeColor` 是明/暗两套 token；设置切换原位更新浮层控件，`MainView.refreshAppearance()` 刷新背景界面；保留 Scene root、浮层和滑块动画，不关闭再重开设置
- 所有 UI 仍用内联 `setStyle()`，没有 CSS 文件
- 窗口是 `StageStyle.UNDECORATED` + 自绘 36px 标题栏（`MainView` + `WindowBehavior`）；**禁止再用 `StageStyle.TRANSPARENT`**（Win10/RDP 黑屏根因）
- 弹窗一律是窗口内浮层（`ToolComponent.openSheet`），不再 `new Stage` / `showAndWait`
- 渲染管线在 `main()` 里 `launch()` 之前定：`render=auto` 时 RDP 或 build<22000 → `sw`，否则 `d3d,sw`；打包后的 exe 不解析 `-D`，只能靠 `System.setProperty`
- Win11 圆角走 `DwmSetWindowAttribute(33)`；Win10/Server 直角 + 1px 边框，不做透明
- Native Image 下取 HWND 仍走 `Unsafe` 读 `Window.peer` + `getRawHandle()`（需要 `--add-opens javafx.graphics/com.sun.javafx.tk.quantum`）
- 语言切换：`ToolService.changeLanguage()` 切换 zh-CN ↔ en-US，`I18n.translate(language)` 取文案

## 关键约定

- **配置文件** `config.ini` 使用 GBK 编码：`ToolUtil.readConfig/writeConfig` 里显式指定 charset， **不能依赖 JDK 默认的 UTF-8**（否则中文路径读成 `?`，回写还会把整个文件改成 UTF-8）
- 配置读取失败时 `loadFailed=true`，`saveConfig()` 直接跳过 —— 绝不能用默认配置覆盖用户文件
- Section 名即为实例名。首个 section `[ToolConfiguration]` 为全局配置（语言/主题/渲染/编辑器）
- INI 注释以 `;` 开头，支持双语注释格式：`; 中文(English)`
- 新建实例时 port 自动递增（max+1），默认 3306；端口重复会拒绝保存
- 导入导出文件命名规则：`output_<端口号>.sql`
- **当前分支**：`4.x`；版本号格式 `4.YY.MMDD`，定义在 `ToolCommon.VERSION`
- MySQL 启动通过 `mysqld.exe --defaults-file=<my.ini> --port <port> --console`，监控 stderr 等待 "ready for connections"（30s 超时）
- MySQL 停止流程：读 PID 文件 → `OpenProcess`+`TerminateProcess` → 删 PID 文件 → 查监听端口 PID 并杀
- 修复流程：检查 VC++ 运行时 → 停止 → 清理 binlog → 删除 PID → 杀端口进程 → 全部 mysqld 杀干净
- Import/Export 通过 `cmd.exe /C` 包装执行 mysqldump/mysql 命令
- 失败/提示不再弹模态：写日志 + 需要决策时才开窗口内浮层
- `config.ini` 的 section/key 排序通过 `TreeMap` 保证稳定性

- 设置控件统一使用 `SettingsPicker`（30px 胶囊轨道 + 24px 圆形滑块）；连续切换维护当前选中值，不使用初始值判断。缺少 my.ini 时创建空文件，不填模板；“更多”提供确认后删除 my.ini。主界面无“全部启动”入口。

- SVG 图标统一用 `ToolCommon.LINE_*` + `ToolComponent.strokeIcon`：24×24 画布、2 单位圆头描边，按画布原点等比缩放；禁止按单条路径边界拉伸，或把开口路径交给填充图标工厂。Windows 四格使用品牌 SVG；记事本使用系统原生 24px 透明 PNG（内嵌并缓存，普通编译与 Native Image 不依赖外部资源）；语言用 Zh/En。

- 单实例使用独立控制面板（实例信息/状态 + 启停、重启、更多操作），不得复用紧凑列表行；双实例使用两个紧凑操作面板，三实例起使用列表；所有布局的重启按钮均固定在菜单外，停止使用红色。所有布局均保留 TITLE_BAR/BACKGROUND 背景分层，禁止把紧凑内容背景统一换成 CARD。

- 一至两个实例直接排版，不放入滚动容器；三实例起列表滚动条宽 8px、可见滑块 4px，与卡片留 6px 间距，避免右侧占位叠加。编辑表单不重复放置移除实例入口，保留更多菜单入口。
- 单/双实例控制面板的停止按钮使用轻红底、红字、细边框（ToolComponent.stopButton），不要恢复成实心红色大色块；启动保持主题色实心按钮。
- appCss 对所有自绘按钮统一清除默认背景/边框内缩及阴影，覆盖普通、悬停、按下、焦点状态；主窗口与独立菜单均加载此样式，避免暗色按钮出现背景与边框不贴合的双框效果。
- 简短提示、二次确认及运行中编辑 my.ini 的选择框统一用 MainView.messagePanel：无标题，13px 正常字重正文、20px 内边距、18px 正文与按钮间距，不设置撑高空白；表单和设置页保留各自标题。
- 针对具体实例的提示、确认及错误文案使用实例名称标明对象（中文为「名称」），不要仅称“此实例”或只用端口代替名称；端口占用等提示同时保留实际端口号。修改 I18n 占位符时同步检查所有调用位置及中英文格式化参数。
- 实例卡片左侧六点手柄支持拖动排序；单实例隐藏手柄，双实例与列表共用排序逻辑。只有内部手柄可发起排序，拖动显示插入线，列表边缘自动滚动，松手后通过 ToolService.moveInstance 保存，取消不改顺序。同步更新 section.sort，保留核心配置在首位；保存失败恢复顺序。配置写入使用同目录临时文件后替换，保持 GBK；不得在排序时触发实例启停或数据库操作。
