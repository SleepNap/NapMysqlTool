package cn.nap;

public class ToolCommon {
    public static final String DEFAULT_CONFIG_FILE = "config.ini";

    // 同一套 24×24 SVG：2 单位圆头描边，四周留白固定，不按各自包围盒拉伸。
    public static final String LINE_PLUS = "M12 5v14M5 12h14";
    public static final String LINE_CLOSE = "M5 5l14 14M19 5L5 19";
    public static final String LINE_MIN = "M4 12h16";
    public static final String LINE_MAX = "M5 5h14v14H5z";
    public static final String LINE_SUN = "M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5";
    public static final String LINE_MOON = "M20 14.2A8.4 8.4 0 0 1 9.8 4a8.5 8.5 0 1 0 10.2 10.2z";
    public static final String LINE_ZH = "M3 6h8L3 18h8M14 6v12M14 13a3 3 0 0 1 6 0v5";
    public static final String LINE_EN = "M11 6H3v12h8M3 12h6M14 18v-8M14 13a3 3 0 0 1 6 0v5";
    public static final String SVG_WINDOWS = "M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z";
    public static final String LINE_FILE = "M13 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9l-6-6zM13 3v6h6M9 13h6M9 17h4";
    public static final String LINE_CLOCK = "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0zM12 7v5l3.5 2";
    public static final String LINE_RESTART = "M20 10a8 8 0 1 0-1.7 7M20 4v6h-6";
    public static final String LINE_REPAIR = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94L14.7 6.3z";
    public static final String LINE_IMPORT = "M12 3v12M8 11l4 4 4-4M4 16v4h16v-4";
    public static final String LINE_EXPORT = "M12 15V3M8 7l4-4 4 4M4 16v4h16v-4";
    public static final String LINE_DELETE = "M4 6h16M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7M14 10v7";
    public static final String LINE_EDIT = "M15.5 6l3 3M4 20l4.5-1 12-12a2.12 2.12 0 0 0-3-3l-12 12L4 20z";
    public static final String LINE_SETTINGS = "M4 7h9M17 7h3M4 17h3M11 17h9M15 4v6M9 14v6";

    public static final String LINE_MARK = "M6 18V6l6 8 6-8v12";

    // Windows 记事本 11.2607.14.0 的原生 24px 透明图标，内嵌以兼容普通编译和 Native Image。
    // SHA-256: 5f32b9eedc1f8586ba122ca3ff001ebf5cac7446c5a28c3c82c8c58eac3c770a
    public static final String NOTEPAD_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAACMUlEQVRIx6WUv2sTcRjGP7lr7tILpBUHpeCYTUGkg4WKtFAQXUoL"
            + "JYOSDg6CFbSLi4P/QRfFtdXBwcEfQ4fuLoJDwEFwdhEqhTYpJOT7ONxd8r3L5XKt73L3fvN+n+fe58n7TkkiHd6zV0cA3Z2t2fPk"
            + "dkzZyctfnV2gGecvfh4n+J//OJKxDrZbh4l86/sfSdp7PX9pM5NAUlMSvucBYIwwCN8rh7mEkfCivG8MEnjl6HdjEGoC2QQxwJPN"
            + "BpLoS0ji0f2NMI8AHzbWB7WSaG6sIisfK1EMoKgwviCBQZnnWWdjCYyJihBmzOVknl07nsAq2v3wmeOTNkWiGgSs3VsJO80lMCYi"
            + "gYuzM1R8LzI/CSgE1lnFr1ie5EjUt1q/s7SYaF0WgNCIVH0zfM/tYETnAViaLMMjipgcAb7/tM9Ju1PIgyCY5u7K0tlMvjBTGwxc"
            + "qLv1lrQA3/cj/5gsUazz8q2FEZ3HyoQSg5fbgSmoc2IWUrUTJjkE+Lh/QLtzWsiD6ekKy7cXC5hsfW0QBHiWB9EyTM1DGK7jJu5O"
            + "XBVGYv76NXq9Xth6PF4xgIQYSuK47tBkJpkctVmtBimdR/dQXGtv3nwPlLMxSW3X85h81evROnVDMGvo0oAj29Va5TeqOSY3LpdZ"
            + "63YHX1H0CSWghOM4I3+MBMFh64ArC6tjQChE9vvbF+r1ejbB1zfb/N15ijh7lAC3VMIvw831x9kEczXn3VzNecB/hFvirZ3/Azah"
            + "/GB9hAxqAAAAAElFTkSuQmCC";

    public static final String VERSION = "4.26.1101";

    // v2 尺寸（含 36px 标题栏）
    public static final double WIN_WIDTH = 680;
    public static final double WIN_HEIGHT = 400;
    public static final double WIN_MIN_WIDTH = 520;
    public static final double WIN_MIN_HEIGHT = 184;
    public static final double COMPACT_WIN_WIDTH = 540;
    public static final double TITLE_BAR_HEIGHT = 36;
    public static final double CONTENT_PAD = 32;
    public static final double ROW_HEIGHT = 80;
    public static final double LIST_TOP = 26;
    public static final double BAR_HEIGHT = 36;
    public static final double ACTION_COLUMN = 90;
    public static final double ACTION_BUTTON = 56;
    public static final double MORE_BUTTON = 28;
    public static final double DIALOG_WIDTH = 500;
    public static final double RESIZE_EDGE = 4;

    public static final String FONT = "Microsoft YaHei UI";

    /** 少量实例按内容收紧窗口，三个以上使用滚动列表。 */
    public static double windowHeightFor(int instanceCount) {
        if (instanceCount == 1) {
            return 220;
        }
        if (instanceCount == 2) {
            return 338;
        }
        if (isCompactCount(instanceCount)) {
            return Math.max(WIN_MIN_HEIGHT, TITLE_BAR_HEIGHT + 24 + instanceCount * ROW_HEIGHT + BAR_HEIGHT + 2);
        }
        int rows = Math.max(1, Math.min(instanceCount, 3));
        return Math.max(WIN_MIN_HEIGHT, TITLE_BAR_HEIGHT + 48 + BAR_HEIGHT + 2 + rows * (ROW_HEIGHT + 8));
    }

    public static boolean isCompactCount(int instanceCount) {
        return instanceCount > 0 && instanceCount <= 2;
    }

    public static double windowWidthFor(int instanceCount) {
        if (instanceCount == 1) {
            return 520;
        }
        return isCompactCount(instanceCount) ? COMPACT_WIN_WIDTH : WIN_WIDTH;
    }

    public enum Status {
        STARTED(1, I18n.STARTED, ThemeColor.GREEN_TEXT),
        STOPPED(0, I18n.STOPPED, ThemeColor.TEXT_3),
        STARTING(2, I18n.STARTING, ThemeColor.AMBER_TEXT),
        STOPPING(3, I18n.STOPPING, ThemeColor.AMBER_TEXT);

        private final int type;
        private final I18n i18n;
        private final ThemeColor color;

        Status(int type, I18n i18n, ThemeColor color) {
            this.type = type;
            this.i18n = i18n;
            this.color = color;
        }

        public int type() {
            return type;
        }

        public I18n i18n() {
            return i18n;
        }

        public ThemeColor color() {
            return color;
        }

        public static Status fromType(int type) {
            for (Status status : values()) {
                if (status.type == type) {
                    return status;
                }
            }
            return STOPPED;
        }
    }

    public enum ThemeColor {
        BACKGROUND("#f2f3f6", "#121418"),
        TITLE_BAR("#ffffff", "#242931"),
        CARD("#ffffff", "#1d2024"),
        CARD_BORDER("#e5e7ec", "#2b2f35"),
        HOVER_2("#e4e7ec", "#343b44"),
        TRACK("#e8ebef", "#2b3139"),
        THUMB("#ffffff", "#3b424c"),
        PANEL("#ffffff", "#23262b"),
        PANEL_BORDER("#dfe2e8", "#3a3f46"),
        TEXT("#1a1c1f", "#e8e9eb"),
        TEXT_2("#6b7075", "#9aa0a6"),
        TEXT_3("#a3a8ae", "#6b7178"),
        LINE("#e5e9ef", "#3b424d"),
        LINE_2("#e1e6ed", "#414955"),
        BORDER("#ccd2d9", "#2a2c2e"),
        BLUE("#1677ff", "#4c9aff"),
        GREEN("#16a34a", "#3fb950"),
        GREEN_TEXT("#15803d", "#56d364"),
        AMBER("#d97706", "#d29922"),
        AMBER_TEXT("#b45309", "#e3b341"),
        RED("#d92d20", "#f85149"),
        HOVER("#fafbfc", "#1d1f21"),
        FILL("#f7f8fa", "#222426"),
        WHITE("#ffffff", "#ffffff");

        private final String light;
        private final String dark;

        ThemeColor(String light, String dark) {
            this.light = light;
            this.dark = dark;
        }

        public String light() {
            return light;
        }

        public String dark() {
            return dark;
        }

        public String color() {
            if (this == BLUE) {
                return ToolService.getInstance().getAccentColor();
            }
            return ToolService.getInstance().isDark() ? dark : light;
        }
    }

    public enum Language {
        ZH_CN("zh-CN"),
        EN_US("en-US");

        private final String type;

        Language(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }

    public enum Theme {
        DARK("dark"),
        LIGHT("light");

        private final String type;

        Theme(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }

    public enum I18n {
        // command
        TOOL_CONFIG_COMMAND("工具配置", "Tool Configuration"),
        LANGUAGE_COMMAND("语言，zh-CN中文，en-US英文", "Language, zh-CN or en-US"),
        THEME_COMMAND("主题颜色，dark深色，light浅色", "Theme Color, dark or light"),
        RENDER_COMMAND("渲染方式，auto自动，sw软件，d3d硬件加速", "Render pipeline, auto/sw/d3d"),
        ACCENT_COMMAND("界面配色，blue/green/purple/slate/orange", "Accent color, blue/green/purple/slate/orange"),
        INI_EDITOR_COMMAND("打开my.ini的程序，default系统默认，notepad记事本，或可执行文件路径",
                "Program to open my.ini, default/notepad/executable path"),
        NAME_COMMAND("实例名称", "Instance Name"),
        PATH_COMMAND("实例路径", "Instance Path"),
        USERNAME_COMMAND("用户名", "Username"),
        PASSWORD_COMMAND("密码", "Password"),
        PORT_COMMAND("端口", "Port"),
        DATABASE_COMMAND("数据库", "Database"),

        // section
        CORE_SECTION("工具配置", "ToolConfiguration"),
        CORE_LANGUAGE("语言", "language"),
        CORE_THEME("主题", "theme"),
        CORE_RENDER("渲染", "render"),
        CORE_INI_EDITOR("INI编辑器", "ini_editor"),
        CORE_ACCENT("配色", "accent"),
        MYSQL8_SECTION("MySQL8.0", "MySQL8.0"),
        MYSQL5_SECTION("MySQL5.7", "MySQL5.7"),
        INSTANCE_PATH("路径", "path"),
        INSTANCE_USERNAME("用户名", "username"),
        INSTANCE_PASSWORD("密码", "password"),
        INSTANCE_PORT("端口", "port"),
        INSTANCE_DATABASE("数据库", "database"),

        // 窗口与列表
        TOOL_TITLE("MySQL启停工具 by Nap", "MySQL Tool by Nap"),
        LOG("日志", "Log"),
        SETTINGS("设置", "Settings"),
        MORE_ACTIONS("更多操作", "More actions"),
        DRAG_TO_REORDER("拖动调整顺序", "Drag to reorder"),
        REORDER_SAVE_FAILED("无法保存实例顺序，已恢复原顺序。请检查配置文件是否可写。",
                "Could not save the instance order. The original order was restored. Check that the configuration file is writable."),
        THEME_LABEL("主题", "Theme"),
        LANGUAGE_LABEL("语言", "Language"),
        OPEN_INI_WITH("打开 my.ini", "Open my.ini"),
        SYSTEM_DEFAULT("系统默认", "System default"),
        INI_FALLBACK_HINT("无关联时用记事本", "Falls back to Notepad"),
        LIGHT("浅色", "Light"),
        DARK("深色", "Dark"),
        DONE("完成", "Done"),
        BROWSE("浏览", "Browse"),
        FOLDER("目录", "Folder"),
        CHOOSE_FOLDER("选择目录", "Choose folder"),
        MESSAGE("提示", "Notice"),
        ACCENT("界面配色", "Accent"),
        ACCENT_BLUE("蓝色", "Blue"),
        ACCENT_GREEN("绿色", "Green"),
        ACCENT_PURPLE("紫色", "Purple"),
        ACCENT_SLATE("石板灰", "Slate"),
        ACCENT_ORANGE("橙色", "Orange"),
        ADD_INSTANCE_BTN("添加实例", "Add instance"),
        SAVE("保存", "Save"),
        CANCEL("取消", "Cancel"),
        CLOSE("关闭", "Close"),

        ADD_INSTANCE_ROW("添加实例", "Add instance"),
        INSTANCE_SUMMARY("%s 个实例 · %s 个运行中", "%s instances · %s running"),
        INSTANCE_SUMMARY_SINGLE("%s 个实例 · %s 个运行中", "%s instance · %s running"),
        START("启动", "Start"),
        STOP("停止", "Stop"),
        RESTART("重启", "Restart"),
        REPAIR("修复", "Repair"),
        IMPORT_DB("导入数据库", "Import database"),
        EXPORT_DB("导出数据库", "Export database"),
        EXPORT_CONFIRM_WARNING("导出实例「%s」的数据库期间会锁表，可能影响正在运行的业务。是否继续导出？",
                "Exporting the database of instance \"%s\" locks tables and may affect running services. Continue exporting?"),
        EXPORT_NOT_RUNNING("无法确认实例「%s」正在运行，已取消导出。不会自动启动或重启数据库。",
                "Cannot confirm that instance \"%s\" is running. Export cancelled; the database will not be started or restarted automatically."),
        EDIT_MY_INI("编辑 my.ini", "Edit my.ini"),
        MODIFY_INSTANCE("修改实例", "Edit instance"),
        DELETE_INSTANCE("移除实例", "Remove instance"),
        CONFIRM_DELETE("确定移除实例「%s」吗？仅移除配置，不会删除数据库文件。",
                "Remove instance \"%s\"? Only its configuration will be removed. Database files will be kept."),
        STOPPED("未启动", "Stopped"),
        STARTED("已启动", "Started"),
        STARTING("启动中", "Starting"),
        STOPPING("停止中", "Stopping"),
        STARTING_HINT("%s · 正在启动，已用时 %s 秒", "%s · starting, %s s"),
        STOPPING_HINT("%s · 正在停止，已用时 %s 秒", "%s · stopping, %s s"),
        RUNNING_LOG("已运行 %s", "up %s"),
        LAST_STOP("上次停止 %s", "stopped %s"),

        // 底栏
        VIEW_LOG("查看日志", "View log"),
        OP_STARTING("正在启动 %s", "Starting %s"),
        OP_STOPPING("正在停止 %s", "Stopping %s"),
        OP_REPAIRING("正在修复 %s", "Repairing %s"),
        OP_IMPORTING("正在导入 %s", "Importing %s"),
        OP_EXPORTING("正在导出 %s", "Exporting %s"),
        ELAPSED("已用时 %s 秒", "%s s elapsed"),
        INI_OPENED("my.ini 已在%s打开", "my.ini opened in %s"),
        INI_EDITING("编辑 my.ini", "Editing my.ini"),
        INI_SAVED("已保存 %s", "Saved %s"),
        INI_RESTART_HINT("重启 MySQL 实例后生效", "Restart MySQL instance to apply"),
        INI_NEXT_START_HINT("下次启动 MySQL 实例时生效", "Applies on next MySQL instance start"),
        RELOAD("重新读取", "Reload"),
        RESTART_INSTANCE("重启实例", "Restart"),
        END_SESSION("关闭提示", "Dismiss"),
        NOTEPAD("记事本", "Notepad"),

        // 操作与提示
        CONFIRM_BIND_START("实例「%s」的端口 %s 已被占用，是否强行启动？",
                "Port %2$s for instance \"%1$s\" is in use. Force startup?"),
        MYSQL_PATH_ERROR("实例「%s」的 MySQL 路径不存在。", "MySQL path missing for instance \"%s\"."),
        DATABASE_EMPTY("实例「%s」尚未配置数据库名称。", "Database name not configured for instance \"%s\"."),
        FIELD_REQUIRED("必填项不能为空", "Required fields cannot be empty"),
        PORT_INVALID("端口需在 1-65535 之间", "Port must be 1-65535"),
        PORT_DUPLICATE("端口 %s 已被实例「%s」使用。", "Port %s is already used by instance \"%s\"."),
        INI_WARN("修改 my.ini 后需重启 MySQL 实例才生效", "Restart the MySQL instance after editing my.ini to apply changes"),
        INI_MISSING("my.ini 不存在：%s", "my.ini not found: %s"),
        INI_OPEN_FAILED("无法打开实例「%s」的 my.ini", "Failed to open my.ini for instance \"%s\""),
        INI_OPEN_RUNNING("实例「%s」正在运行，修改配置后需要重启。先停止再编辑吗？",
                "Instance \"%s\" is running. Changes require a restart. Stop it before editing?"),
        STOP_AND_EDIT("停止并编辑", "Stop and edit"),
        EDIT_ONLY("仅编辑", "Edit anyway"),
        LOG_EMPTY("暂无日志", "No log entries"),

        // 兼容旧调用
        ADD_INSTANCE("添加", "Add"),
        EDIT_INI("编辑INI", "Edit INI"),
        INI_SAVE_SUCCESS("端口%s的my.ini已保存", "my.ini for port %s saved."),
        INI_IMPORT_TEMPLATE("模板", "Template"),
        INI_DELETE_FILE("删除 my.ini", "Delete my.ini"),
        INI_DELETE_CONFIRM("确定删除实例「%s」的 my.ini 吗？\n%s", "Delete my.ini for instance \"%s\"?\n%s"),
        INI_DELETE_SUCCESS("实例「%s」的 my.ini 已删除。", "my.ini deleted for instance \"%s\"."),
        INI_DELETE_FAILED("无法删除实例「%s」的 my.ini", "Failed to delete my.ini for instance \"%s\""),
        INI_TPL_CHARSET("# 字符集", "# Character set"),
        INI_TPL_COLLATION("# 排序规则", "# Collation"),
        INI_TPL_ENGINE("# 默认存储引擎", "# Default storage engine"),
        INI_TPL_MAX_CONN("# 允许最大连接数", "# Max connections"),
        INI_TPL_CONN_ERR("# 允许连接失败的次数", "# Max connect errors"),
        INI_TPL_TMP_TABLE("# 临时表大小", "# Temporary table size"),
        INI_TPL_MAX_PACKET("# 最大包大小", "# Max packet size"),
        INI_TPL_LOWER_CASE("# 表名不区分大小写", "# Case-insensitive table names"),
        INI_TPL_BUFFER("# InnoDB缓冲池", "# InnoDB buffer pool"),
        INSTANCE_NAME_LABEL("名称", "Name"),
        INSTANCE_PATH_LABEL("MySQL 路径", "MySQL path"),
        INSTANCE_USERNAME_LABEL("用户名", "Username"),
        INSTANCE_PASSWORD_LABEL("密码", "Password"),
        INSTANCE_PORT_LABEL("端口", "Port"),
        INSTANCE_DATABASE_LABEL("数据库", "Database"),

        // 任务
        REPAIR_STEP_VC("检查VC++环境依赖", "Check VC++ Runtime"),
        REPAIR_STEP_ASCII("检查英文路径", "Check ASCII Path"),
        REPAIR_STEP_STOP("停止服务", "Stop Service"),
        REPAIR_STEP_BINLOG("清空binlog", "Clear Binlog"),
        REPAIR_STEP_PID("删除pid文件", "Delete PID"),
        REPAIR_STEP_PORT("解除端口占用", "Release Port"),
        REPAIR_STEP_START("启动服务", "Start Service"),
        STEP_DONE("完成", "Done"),
        STEP_FAIL("失败", "Failed"),
        REPAIR_SUCCESS("修复完成", "Repair success."),
        REPAIR_ERR1("修复失败，MySQL未停止", "Repair failed, MySQL did not stop."),
        EXPORT_SUCCESS("导出完成", "Export success."),
        EXPORT_ERR1("导出失败，MySQL未启动", "Export failed, MySQL did not start."),
        IMPORT_SUCCESS("导入完成", "Import success."),
        IMPORT_ERR1("实例「%s」未能启动，已取消导入。", "Instance \"%s\" did not start. Import cancelled."),
        IMPORT_ERR2("无法导入实例「%s」：找不到文件 %s。", "Cannot import into instance \"%s\": file %s not found."),
        TAB_OPERATE("启停功能", "Control"),
        TAB_EXT("扩展功能", "More"),
        TAB_INSTANCE("实例管理", "Instances"),
        REPAIR_ALL("修复全部", "Repair all"),
        IMPORT_ALL("导入全部", "Import all"),
        EXPORT_ALL("导出全部", "Export all"),
        REPAIRING("修复中", "Repairing"),
        OK("确定", "OK");

        private final String zh;
        private final String en;

        I18n(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }

        public String ZH() {
            return zh;
        }

        public String EN() {
            return en;
        }

        public String comment() {
            return String.format("; %s(%s)", zh, en);
        }

        public String translate(String language) {
            return Language.EN_US.type().equals(language) ? en : zh;
        }
    }
}
