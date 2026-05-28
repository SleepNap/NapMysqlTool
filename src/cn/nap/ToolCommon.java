package cn.nap;

public class ToolCommon {
    public static final String DEFAULT_CONFIG_FILE = "config.ini";
    public static final String SVG_DARK = "M593.054 120.217C483.656 148.739 402.91 248.212 402.91 366.546c0 140.582 113.962 254.544 254.544 254.544 118.334 0 217.808-80.746 246.328-190.144C909.17 457.12 912 484.23 912 512c0 220.914-179.086 400-400 400S112 732.914 112 512s179.086-400 400-400c27.77 0 54.88 2.83 81.054 8.217z";
    public static final String SVG_LIGHT = "M512 831.508c16.264 0 29.792 11.72 32.597 27.176l0.534 5.955v62.23C545.13 945.167 530.298 960 512 960c-16.265 0-29.792-11.72-32.597-27.175l-0.534-5.956v-62.23c0-18.298 14.833-33.13 33.131-33.13z m267.584-97.826l5.197 4.245 44.002 44.002c12.938 12.938 12.938 33.916 0 46.854-11.321 11.321-28.797 12.736-41.656 4.245l-5.198-4.245-44.002-44.002c-12.938-12.938-12.938-33.915 0-46.854 11.321-11.32 28.797-12.736 41.657-4.245z m-493.511 4.246c11.32 11.32 12.736 28.796 4.245 41.656l-4.246 5.197-44.002 44.002c-12.939 12.938-33.916 12.938-46.854 0-11.32-11.321-12.736-28.797-4.245-41.657l4.245-5.197 44.003-44.002c12.938-12.938 33.916-12.938 46.854 0zM512 274.932c130.929 0 237.068 106.14 237.068 237.068 0 130.929-106.14 237.068-237.068 237.068-130.929 0-237.068-106.14-237.068-237.068 0-130.929 106.14-237.068 237.068-237.068z m414.87 203.937c18.297 0 33.13 14.833 33.13 33.131 0 16.264-11.72 29.792-27.175 32.597l-5.956 0.534h-62.23c-18.298 0-33.13-14.833-33.13-33.131 0-16.265 11.72-29.792 27.175-32.597l5.955-0.534h62.23z m-767.509 0c18.298 0 33.13 14.833 33.13 33.131 0 16.264-11.72 29.792-27.175 32.597l-5.955 0.534h-62.23C78.833 545.13 64 530.298 64 512c0-16.265 11.72-29.792 27.175-32.597l5.956-0.534h62.23z m669.422-283.653c11.321 11.32 12.736 28.797 4.246 41.656l-4.246 5.198-44.002 44.002c-12.938 12.939-33.915 12.94-46.853 0-11.322-11.32-12.737-28.796-4.246-41.656l4.245-5.197 44.002-44.003c12.938-12.938 33.915-12.938 46.854 0z m-591.91-4.245l5.197 4.245 44.003 44.003c12.938 12.938 12.938 33.915 0 46.854-11.321 11.32-28.797 12.736-41.657 4.245l-5.197-4.245-44.003-44.003c-12.938-12.938-12.938-33.915 0-46.854 11.321-11.32 28.797-12.736 41.657-4.245zM512 64c16.264 0 29.792 11.72 32.597 27.175l0.534 5.956v62.23c0 18.298-14.833 33.13-33.131 33.13-16.265 0-29.792-11.72-32.597-27.175l-0.534-5.955v-62.23C478.87 78.833 493.702 64 512 64z";
    public static final String SVG_ADD = "M105 480a8 8 0 0 1 8-8h799a8 8 0 0 1 8 8v64a8 8 0 0 1-8 8H113a8 8 0 0 1-8-8v-64z M480 920a8 8 0 0 1-8-8V112a8 8 0 0 1 8-8h64a8 8 0 0 1 8 8v800a8 8 0 0 1-8 8h-64z";
    public static final String SVG_START = "M 280 160 L 800 512 L 280 864 Z";
    public static final String SVG_STOP = "M 288 288 L 736 288 L 736 736 L 288 736 Z";
    public static final String SVG_RESTART = "M 512 80 C 273.6 80 80 273.6 80 512 s 193.6 432 432 432 s 432 -193.6 432 -432 h -96 c 0 185.6 -150.4 336 -336 336 S 176 697.6 176 512 S 326.4 176 512 176 c 92.8 0 178.4 37.6 240 98.8 l -144 144 h 320 V 98.8 l -86.4 86.4 C 772 108.8 648 80 512 80 z";
    public static final String VERSION = "3.26.0525";


    public enum Status {
        STARTED(1, I18n.STARTED, ThemeColor.PRIMARY),
        STOPPED(0, I18n.STOPPED, ThemeColor.DANGER),
        STARTING(2, I18n.STARTING, ThemeColor.FONT_BG);

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
        WINDOW_BG("#1f2d3d", "#5c5c5c"),

        ROOT_BG("#2b3a4f", "#fff"),

        BUTTON_BG("#3a4d66", "#f5f5f5"),
        BUTTON_HOVER("#4a6080", "#eee"),
        BUTTON_BORDER("#4a6080", "#999"),
        BUTTON_BORDER_HOVER("#5a7090", "#999"),

        CARD_BG("#334560", "#f8f9fa"),

        FONT_BG("#c8d6e5", "#555"),
        FONT_HOVER("#fff", "#000"),

        SCROLL_BG("#2b3a4f", "#fff"),

        PRIMARY("#4a9eff", "#1677ff"),
        PRIMARY_HOVER("#6ab4ff", "#4096ff"),
        DANGER("#ff4d4f", "#ff4d4f"),
        DANGER_HOVER("#ff7875", "#ff7875"),
        ;

        private final String dark;
        private final String light;

        ThemeColor(String dark, String light) {
            this.dark = dark;
            this.light = light;
        }

        public String dark() {
            return dark;
        }

        public String light() {
            return light;
        }

        public String color(boolean isDark) {
            return isDark ? dark : light;
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
        MYSQL8_SECTION("MySQL8.0", "MySQL8.0"),
        MYSQL5_SECTION("MySQL5.7", "MySQL5.7"),
        INSTANCE_PATH("路径", "path"),
        INSTANCE_USERNAME("用户名", "username"),
        INSTANCE_PASSWORD("密码", "password"),
        INSTANCE_PORT("端口", "port"),
        INSTANCE_DATABASE("数据库", "database"),

        // ui
        TOOL_TITLE("MySQL启停工具 by Nap", "MySQL Tool by Nap"),
        TAB_OPERATE("启停功能", "Start/Stop"),
        TAB_EXT("扩展功能", "Extension"),
        TAB_INI("实例管理", "Instance"),
        CREATE("添加实例", "CreateInstance"),

        START_ALL("启动全部", "StartAll"),
        STOP_ALL("停止全部", "StopAll"),
        RESTART_ALL("重启全部", "RestartAll"),
        START("启动", "Start"),
        STOP("停止", "Stop"),
        RESTART("重启", "Restart"),

        STARTED("已启动", "Started"),
        STOPPED("未启动", "Stopped"),
        STARTING("启动中", "Starting"),

        ;

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
    }
}
