package cn.nap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.Theme;
import static cn.nap.ToolCommon.Language;

public class ToolService {
    private static final ToolService instance = new ToolService();

    private ToolConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private boolean win11;
    private boolean loadFailed;

    public static ToolService getInstance() {
        return instance;
    }

    public void loadConfig() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        boolean exists = Files.exists(Paths.get(ToolCommon.DEFAULT_CONFIG_FILE));
        try {
            win11 = ToolUtil.isWin11();
            List<ToolUtil.SectionObj> sectionList = ToolUtil.readConfig(ToolCommon.DEFAULT_CONFIG_FILE);
            if (sectionList == null && exists) {
                // 文件存在却读不出来：宁可本次只读，也不能用默认配置覆盖用户数据
                throw new IllegalStateException("config.ini unreadable");
            }
            config = parseConfig(sectionList);
            refreshStatus();
        } catch (Exception e) {
            loadFailed = true;
            config = initDefaultConfig();
            refreshStatus();
        } finally {
            loading.set(false);
        }
    }

    public boolean isConfigLoading() {
        return loading.get();
    }

    public boolean saveConfig() {
        // 读取失败时禁止回写，避免把用户的 config.ini 覆盖成默认值
        if (config == null || loadFailed) {
            return false;
        }
        TreeMap<Integer, ToolUtil.SectionObj> sortedMap = new TreeMap<>();
        if (config.core != null) {
            sortedMap.put(config.core.section.sort, ToolUtil.fromCore(config.core));
        }
        if (config.instances != null) {
            for (ToolConfig.Instance instance : config.instances) {
                sortedMap.put(instance.section.sort, ToolUtil.fromInstance(instance));
            }
        }
        return ToolUtil.writeConfig(ToolCommon.DEFAULT_CONFIG_FILE, new ArrayList<>(sortedMap.values()));
    }

    /** position 是移动后的索引；同步 section 排序值，确保重启后顺序不变。 */
    public boolean moveInstance(ToolConfig.Instance moving, int position) {
        if (config == null || loadFailed || config.instances == null
                || position < 0 || position >= config.instances.size()) {
            return false;
        }
        int from = config.instances.indexOf(moving);
        if (from < 0) {
            return false;
        }
        if (from == position) {
            return true;
        }
        List<ToolConfig.Instance> previous = new ArrayList<>(config.instances);
        int[] previousSort = previous.stream().mapToInt(item -> item.section.sort).toArray();
        int coreSort = config.core == null ? 0 : config.core.section.sort;
        config.instances.remove(from);
        config.instances.add(position, moving);
        if (config.core != null) {
            config.core.section.sort = 0;
        }
        for (int index = 0; index < config.instances.size(); index++) {
            config.instances.get(index).section.sort = index + 1;
        }
        if (saveConfig()) {
            return true;
        }
        config.instances.clear();
        config.instances.addAll(previous);
        for (int index = 0; index < previous.size(); index++) {
            previous.get(index).section.sort = previousSort[index];
        }
        if (config.core != null) {
            config.core.section.sort = coreSort;
        }
        return false;
    }

    public ToolConfig getConfig() {
        return config;
    }

    public boolean isWin11() {
        return win11;
    }

    public boolean isLoadFailed() {
        return loadFailed;
    }

    public boolean isDark() {
        return Theme.DARK.type().equals(config.core.theme.data);
    }

    public String getLanguage() {
        return config.core.language.data;
    }

    public void changeTheme() {
        config.core.theme.data = isDark() ? Theme.LIGHT.type() : Theme.DARK.type();
    }

    public void changeLanguage() {
        config.core.language.data = Language.ZH_CN.type().equals(getLanguage()) ? Language.EN_US.type() : Language.ZH_CN.type();
    }

    private ToolConfig parseConfig(List<ToolUtil.SectionObj> sectionList) {
        if (sectionList == null || sectionList.isEmpty()) {
            return initDefaultConfig();
        }
        ToolConfig config = new ToolConfig();
        config.instances = new ArrayList<>();
        for (int i = 0; i < sectionList.size(); i++) {
            ToolUtil.SectionObj sectionObj = sectionList.get(i);
            if (I18n.CORE_SECTION.ZH().equals(sectionObj.section) || I18n.CORE_SECTION.EN().equals(sectionObj.section)) {
                config.core = ToolUtil.parseCoreSection(sectionObj, i);
            } else {
                ToolConfig.Instance instance = ToolUtil.parseInstanceSection(sectionObj, i);
                if (instance != null) {
                    config.instances.add(instance);
                }
            }
        }
        return config;
    }

    private ToolConfig initDefaultConfig() {
        ToolConfig config = new ToolConfig();
        config.core = initDefaultCore();
        config.instances = initDefaultInstances();
        return config;
    }

    private ToolConfig.Core initDefaultCore() {
        ToolConfig.Core core = new ToolConfig.Core(
                new ToolConfig.Info<>(I18n.CORE_SECTION.EN(), 0, I18n.TOOL_CONFIG_COMMAND.comment()),
                new ToolConfig.Info<>(Language.ZH_CN.type(), 0, I18n.LANGUAGE_COMMAND.comment()),
                new ToolConfig.Info<>(Theme.LIGHT.type(), 1, I18n.THEME_COMMAND.comment())
        );
        core.render = new ToolConfig.Info<>("auto", 2, I18n.RENDER_COMMAND.comment());
        core.iniEditor = new ToolConfig.Info<>("default", 3, I18n.INI_EDITOR_COMMAND.comment());
        core.accent = new ToolConfig.Info<>("blue", 4, I18n.ACCENT_COMMAND.comment());
        return core;
    }

    /** auto / sw / d3d */
    public String getRenderMode() {
        return config == null || config.core == null || config.core.render == null
                ? "auto" : config.core.render.data;
    }

    /** default / notepad / 可执行文件路径 */
    public String getIniEditor() {
        return config == null || config.core == null || config.core.iniEditor == null
                ? "default" : config.core.iniEditor.data;
    }

    /** blue / green / purple / slate / orange */
    public String getAccent() {
        if (config == null || config.core == null || config.core.accent == null) {
            return "blue";
        }
        return config.core.accent.data;
    }

    public void setAccent(String accent) {
        if (config != null && config.core != null && config.core.accent != null) {
            config.core.accent.data = accent;
        }
    }

    /** 主色 hex；ThemeColor.BLUE 直接取这里。 */
    public String getAccentColor() {
        return switch (getAccent()) {
            case "green" -> "#16a34a";
            case "purple" -> "#7c3aed";
            case "slate" -> "#475569";
            case "orange" -> "#ea580c";
            default -> "#1677ff";
        };
    }

    public void setIniEditor(String editor) {
        if (config != null && config.core != null && config.core.iniEditor != null) {
            config.core.iniEditor.data = editor;
        }
    }

    /** 实例当前 PID，取 data 目录下的 *.pid。 */
    public String getPid(ToolConfig.Instance instance) {
        Path pidFile = getPidFile(instance);
        return pidFile == null ? null : ToolUtil.readPidFile(pidFile.toString());
    }

    /** pid 文件的修改时间当作启动时刻，取不到返回 -1。 */
    public long getStartTime(ToolConfig.Instance instance) {
        Path pidFile = getPidFile(instance);
        if (pidFile == null) {
            return -1;
        }
        try {
            return Files.getLastModifiedTime(pidFile).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    public boolean isRunning(ToolConfig.Instance instance) {
        Path pidFile = getPidFile(instance);
        if (pidFile == null) {
            return false;
        }
        String pid = ToolUtil.readPidFile(pidFile.toString());
        if (pid != null && !pid.isEmpty()) {
            return ToolUtil.isPidRunning(pid);
        }
        return false;
    }

    private Path getPidFile(ToolConfig.Instance instance) {
        Path dataDir = Paths.get(instance.path.data, "data");
        try (Stream<Path> pathStream = Files.list(dataDir)) {
            return pathStream.filter(f -> f.getFileName().toString().endsWith(".pid")).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean isPortOccupied(ToolConfig.Instance instance) {
        return ToolUtil.isPortOccupied(Integer.parseInt(instance.port.data));
    }

    public boolean start(ToolConfig.Instance instance) throws Exception {
        Path pidFile = getPidFile(instance);
        if (pidFile != null) {
            String pid = ToolUtil.readPidFile(pidFile.toString());
            if (pid != null && !pid.isEmpty()) {
                ToolUtil.killPid(pid);
            }
        }

        ToolUtil.killPortProcess(Integer.parseInt(instance.port.data));

        Path baseDir = Paths.get(instance.path.data).toAbsolutePath().normalize();
        Path iniFile = baseDir.resolve("my.ini");
        // mysqld 不支持中文路径
        StringBuilder shell = new StringBuilder("bin\\mysqld.exe");
        if (Files.exists(iniFile)) {
            shell.append(" --defaults-file=my.ini");
        }
        shell.append(" --port ").append(instance.port.data);
        shell.append(" --console");
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(baseDir.toFile());
        pb.command("cmd", "/c", shell.toString());
        Process process = pb.start();
        // 惊了，输出为什么在ErrorStream里？
        InputStream errStream = process.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(errStream));

        // 30s 拆 3 段，每段 10s 优先等 "ready for connections"
        for (int i = 0; i < 3; i++) {
            long segEnd = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < segEnd) {
                if (errStream.available() > 0) {
                    String output = reader.readLine();
                    System.out.println(output);
                    if (output == null) {
                        return false;
                    }
                    if (output.contains("ready for connections")) {
                        return true;
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            // 10s 没等到 ready 字样，用进程存活+pid 兜底；任一为真即视作启动成功
            if (isRunning(instance)) {
                return true;
            }
        }
        return false;
    }

    public boolean stop(ToolConfig.Instance instance) {
        Path pidFile = getPidFile(instance);
        if (pidFile != null) {
            String pid = ToolUtil.readPidFile(pidFile.toString());
            if (pid != null && !pid.isEmpty()) {
                ToolUtil.killPid(pid);
            }
            try {
                Files.deleteIfExists(pidFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ToolUtil.killPortProcess(Integer.parseInt(instance.port.data));
        instance.status = ToolCommon.Status.STOPPED.type();
        return true;
    }

    public void clearBinlog(ToolConfig.Instance instance) throws Exception {
        Path dataDir = Paths.get(instance.path.data, "data");
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) return;
        try (Stream<Path> stream = Files.list(dataDir)) {
            stream.forEach(f -> {
                try {
                    String name = f.getFileName().toString();
                    if ("binlog.index".equals(name)) {
                        ToolUtil.clearFile(f.toFile());
                    } else if (name.startsWith("binlog")) {
                        Files.deleteIfExists(f);
                    }
                } catch (IOException ignored) {
                }
            });
        }
    }

    public void clearPid(ToolConfig.Instance instance) throws Exception {
        Path dataDir = Paths.get(instance.path.data, "data");
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) return;
        try (Stream<Path> stream = Files.list(dataDir)) {
            stream.filter(f -> f.getFileName().toString().endsWith(".pid"))
                    .forEach(f -> {
                        try {
                            Files.deleteIfExists(f);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    public void exportDb(ToolConfig.Instance instance) throws Exception {
        String user = instance.username.data;
        String pass = instance.password.data;
        String dbs = instance.database.data;
        // 输出到项目根目录（findImportFile 在此查找）
        String file = new File("output_" + instance.port.data + ".sql").getAbsolutePath();
        StringBuilder cmd = new StringBuilder();
        cmd.append("bin\\mysqldump.exe");
        cmd.append(" -u").append(user);
        cmd.append(" -P").append(instance.port.data);
        if (pass != null && !pass.trim().isEmpty()) {
            cmd.append(" -p").append(pass);
        }
        if (dbs != null && !dbs.trim().isEmpty()) {
            cmd.append(" --databases ").append(dbs);
        }
        cmd.append(" --hex-blob > \"").append(file).append("\"");
        execMysqlTool(instance, cmd.toString(), "mysqldump");
    }

    public void importDb(ToolConfig.Instance instance, String file) throws Exception {
        String user = instance.username.data;
        String pass = instance.password.data;
        StringBuilder cmd = new StringBuilder();
        cmd.append("bin\\mysql.exe");
        cmd.append(" -u").append(user);
        cmd.append(" -P").append(instance.port.data);
        if (pass != null && !pass.trim().isEmpty()) {
            cmd.append(" -p").append(pass);
        }
        // 不指定数据库名：SQL 文件里已有 CREATE DATABASE / USE 语句
        cmd.append(" < \"").append(file).append("\"");
        execMysqlTool(instance, cmd.toString(), "mysql");
    }

    private void execMysqlTool(ToolConfig.Instance instance, String cmd, String toolName) throws Exception {
        System.out.println(toolName + " cmd: " + cmd);
        Path baseDir = Paths.get(instance.path.data).toAbsolutePath().normalize();
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", cmd);
        pb.directory(baseDir.toFile());
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[" + toolName + "] " + line);
            }
        }
        p.waitFor();
        int exitCode = p.exitValue();
        System.out.println(toolName + " exit code: " + exitCode);
        if (exitCode != 0) {
            throw new Exception(toolName + " failed with exit code " + exitCode);
        }
    }

    public void refreshStatus() {
        if (config.instances == null) return;
        for (ToolConfig.Instance instance : config.instances) {
            if (instance.status == ToolCommon.Status.STARTING.type()) {
                continue;
            }
            Path dir = Paths.get(instance.path.data);
            if (!Files.exists(dir)) {
                continue;
            }
            instance.status = isRunning(instance) ? ToolCommon.Status.STARTED.type() : ToolCommon.Status.STOPPED.type();
        }
    }

    private Map<String, Map<String, String>> toIniProp(ToolConfig.Instance instance) {
        Map<String, String> mysqlConf = new HashMap<>();
        mysqlConf.put("mysql路径", instance.path.data);
        mysqlConf.put("mysql账号", instance.username.data);
        mysqlConf.put("mysql密码", instance.password.data);
        mysqlConf.put("mysql.ini路径", "");

        Map<String, Map<String, String>> iniProp = new HashMap<>();
        iniProp.put("mysql配置", mysqlConf);
        return iniProp;
    }

    private List<ToolConfig.Instance> initDefaultInstances() {
        AtomicInteger instanceSort = new AtomicInteger(0);
        AtomicInteger mysql8Sort = new AtomicInteger(0);
        ToolConfig.Instance mysql8 = new ToolConfig.Instance();
        mysql8.section = new ToolConfig.Info<>(I18n.MYSQL8_SECTION.EN(), instanceSort.incrementAndGet(), I18n.NAME_COMMAND.comment());
        mysql8.path = new ToolConfig.Info<>("mysql-8.0.39-winx64", mysql8Sort.incrementAndGet(), I18n.PATH_COMMAND.comment());
        mysql8.username = new ToolConfig.Info<>("root", mysql8Sort.incrementAndGet(), I18n.USERNAME_COMMAND.comment());
        mysql8.password = new ToolConfig.Info<>("root", mysql8Sort.incrementAndGet(), I18n.PASSWORD_COMMAND.comment());
        mysql8.port = new ToolConfig.Info<>("3306", mysql8Sort.incrementAndGet(), I18n.PORT_COMMAND.comment());
        mysql8.database = new ToolConfig.Info<>("beidou", mysql8Sort.incrementAndGet(), I18n.DATABASE_COMMAND.comment());

        AtomicInteger mysql5Sort = new AtomicInteger(0);
        ToolConfig.Instance mysql5 = new ToolConfig.Instance();
        mysql5.section = new ToolConfig.Info<>(I18n.MYSQL5_SECTION.EN(), instanceSort.incrementAndGet(), I18n.NAME_COMMAND.comment());
        mysql5.path = new ToolConfig.Info<>("mysql-5.7.44-winx64", mysql5Sort.incrementAndGet(), I18n.PATH_COMMAND.comment());
        mysql5.username = new ToolConfig.Info<>("root", mysql5Sort.incrementAndGet(), I18n.USERNAME_COMMAND.comment());
        mysql5.password = new ToolConfig.Info<>("root", mysql5Sort.incrementAndGet(), I18n.PASSWORD_COMMAND.comment());
        mysql5.port = new ToolConfig.Info<>("3307", mysql5Sort.incrementAndGet(), I18n.PORT_COMMAND.comment());
        mysql5.database = new ToolConfig.Info<>("napms", mysql5Sort.incrementAndGet(), I18n.DATABASE_COMMAND.comment());

        List<ToolConfig.Instance> instanceList = new ArrayList<>();
        instanceList.add(mysql8);
        instanceList.add(mysql5);
        return instanceList;
    }

    public int getNextInstanceSort() {
        if (config.instances == null || config.instances.isEmpty()) return 1;
        int maxSort = 0;
        for (ToolConfig.Instance inst : config.instances) {
            if (inst.section.sort > maxSort) maxSort = inst.section.sort;
        }
        return maxSort + 1;
    }

    public String getDefaultPort() {
        if (config.instances == null || config.instances.isEmpty()) return "3306";
        int maxPort = 0;
        for (ToolConfig.Instance inst : config.instances) {
            try {
                int p = Integer.parseInt(inst.port.data);
                if (p > maxPort) maxPort = p;
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(maxPort + 1);
    }

    private void checkInstancePath(ToolConfig.Instance instance) throws Exception {
        Path dir = Paths.get(instance.path.data);
        if (!Files.isDirectory(dir)) {
            throw new Exception(String.format(I18n.MYSQL_PATH_ERROR.translate(getLanguage()), instance.section.data));
        }
    }

    /** 缺少时创建零字节文件；已存在时不改动原内容，禁止覆盖现有配置。 */
    public File createMyIniIfMissing(ToolConfig.Instance instance) throws IOException {
        File file = new File(instance.path.data, "my.ini");
        file.createNewFile();
        if (!file.isFile()) {
            throw new IOException("Not a regular file: " + file.getAbsolutePath());
        }
        return file;
    }

    public String readMyIni(ToolConfig.Instance instance) throws Exception {
        Path iniFile = Paths.get(instance.path.data, "my.ini");
        if (!Files.exists(iniFile)) return "";
        return new String(Files.readAllBytes(iniFile));
    }

    public void writeMyIni(ToolConfig.Instance instance, String content) throws Exception {
        Path iniFile = Paths.get(instance.path.data, "my.ini");
        Files.write(iniFile, content.getBytes());
    }

    public void deleteMyIni(ToolConfig.Instance instance) throws Exception {
        Path iniFile = Paths.get(instance.path.data, "my.ini");
        if (Files.isDirectory(iniFile)) {
            throw new IOException("Not a regular file: " + iniFile.toAbsolutePath());
        }
        Files.deleteIfExists(iniFile);
    }
}
