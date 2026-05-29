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

    public static ToolService getInstance() {
        return instance;
    }

    public void loadConfig() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        try {
            win11 = ToolUtil.isWin11();
            List<ToolUtil.SectionObj> sectionList = ToolUtil.readConfig(ToolCommon.DEFAULT_CONFIG_FILE);
            config = parseConfig(sectionList);
            refreshStatus();
        } catch (Exception e) {
            config = initDefaultConfig();
            refreshStatus();
        } finally {
            loading.set(false);
        }
    }

    public boolean isConfigLoading() {
        return loading.get();
    }

    public void saveConfig() {
        if (config == null) {
            return;
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
        ToolUtil.writeConfig(ToolCommon.DEFAULT_CONFIG_FILE, new ArrayList<>(sortedMap.values()));
    }

    public ToolConfig getConfig() {
        return config;
    }

    public boolean isWin11() {
        return win11;
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
        return new ToolConfig.Core(
                new ToolConfig.Info<>(I18n.CORE_SECTION.EN(), 0, I18n.TOOL_CONFIG_COMMAND.comment()),
                new ToolConfig.Info<>(Language.ZH_CN.type(), 0, I18n.LANGUAGE_COMMAND.comment()),
                new ToolConfig.Info<>(Theme.LIGHT.type(), 1, I18n.THEME_COMMAND.comment())
        );
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
        Path exeFile = Paths.get(instance.path.data, "bin", "mysqld.exe");
        Path iniFile = Paths.get(instance.path.data, "my.ini");
        List<String> cmd = new ArrayList<>();
        cmd.add(exeFile.toFile().getAbsolutePath());
        cmd.add("--port");
        cmd.add(instance.port.data);
        if (Files.exists(iniFile)) {
            cmd.add("--defaults-file=" + iniFile.toFile().getAbsolutePath());
        }
        cmd.add("--console");
        Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        // 惊了，输出为什么在ErrorStream里？
        InputStream inputStream = process.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 30000) {
            String output = reader.readLine();
            System.out.println(output);
            if (output != null && output.contains("ready for connections")) {
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
        String path = instance.path.data;
        String user = instance.username.data;
        String pass = instance.password.data;
        String dbs = instance.database.data;
        String file = "output_" + instance.port.data + ".sql";
        Runtime.getRuntime().exec(new String[]{"cmd.exe", "/C",
                path + File.separator + "bin" + File.separator + "mysqldump.exe"
                + " -u" + user + " -p" + pass + " --databases " + dbs + " --hex-blob > " + file}).waitFor();
    }

    public void importDb(ToolConfig.Instance instance, String file) throws Exception {
        String path = instance.path.data;
        String user = instance.username.data;
        String pass = instance.password.data;
        String db = instance.database.data;
        Runtime.getRuntime().exec(new String[]{"cmd.exe", "/C",
                path + File.separator + "bin" + File.separator + "mysql.exe"
                + " -u" + user + " -p" + pass + " " + db + " < " + file}).waitFor();
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

}