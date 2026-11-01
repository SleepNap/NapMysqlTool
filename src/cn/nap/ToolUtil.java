package cn.nap;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.io.*;
import java.nio.charset.Charset;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.Language;
import static cn.nap.ToolCommon.Theme;

public class ToolUtil {

    private static final int PROCESS_TERMINATE = 0x0001;
    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    private static final int ERROR_ACCESS_DENIED = 5;
    private static final int AF_INET = 2;
    private static final int TCP_TABLE_OWNER_PID_LISTENER = 3;
    private static final int ERROR_INSUFFICIENT_BUFFER = 122;
    private static final int SM_REMOTESESSION = 0x1000;
    /** config.ini 历史上是 GBK，读写都必须显式指定，不能跟随 JDK 默认的 UTF-8。 */
    private static final Charset CONFIG_CHARSET = Charset.forName("GBK");

    public static class SectionObj {
        public String section;
        public String comment;
        public List<IniObj> iniList = new ArrayList<>();

        public SectionObj() {
        }

        public SectionObj(String section, String comment) {
            this.section = section;
            this.comment = comment;
        }
    }

    public static class IniObj {
        public String key;
        public String value;
        public String comment;

        public IniObj() {
        }

        public IniObj(String key, String value, String comment) {
            this.key = key;
            this.value = value;
            this.comment = comment;
        }
    }

    public static interface CloseCallback {
        void onClose(int trigger);
    }

    public static List<ToolUtil.SectionObj> readConfig(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (!Files.exists(Paths.get(file))) {
            return null;
        }

        String line;
        ToolUtil.SectionObj sectionObj = null;
        String lastComment = null;
        List<ToolUtil.SectionObj> sectionList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), CONFIG_CHARSET))) {
            while (null != (line = reader.readLine())) {
                line = line.trim();
                // 空白行跳过
                if (line.isEmpty()) {
                    continue;
                }
                // 注释跳过
                if (line.startsWith(";") || line.startsWith("#")) {
                    lastComment = line;
                    continue;
                }
                // 获取当前节点
                if (line.startsWith("[") && line.endsWith("]")) {
                    sectionObj = new ToolUtil.SectionObj();
                    sectionObj.section = line.substring(0, line.length() - 1).substring(1);
                    sectionObj.comment = lastComment;
                    lastComment = null;
                    sectionList.add(sectionObj);
                    continue;
                }
                // 如果当前行还没到第一个节点，继续下一行
                if (null == sectionObj) {
                    continue;
                }
                // 将值赋值到sectionMap中
                String[] split = line.split("=", 2);
                if (split.length <= 1) {
                    continue;
                }
                IniObj iniObj = new IniObj();
                iniObj.key = split[0].trim();
                iniObj.value = split[1].trim();
                iniObj.comment = lastComment;
                lastComment = null;
                sectionObj.iniList.add(iniObj);
            }
            return sectionList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeConfig(String file, List<ToolUtil.SectionObj> sectionList) {
        if (sectionList == null) {
            return false;
        }
        Path destination = Paths.get(file).toAbsolutePath();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(destination.getParent(), ".nap-config-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, CONFIG_CHARSET)) {
                for (ToolUtil.SectionObj sectionObj : sectionList) {
                    if (sectionObj.comment != null) {
                        writer.write(sectionObj.comment);
                        writer.newLine();
                    }
                    writer.write("[" + sectionObj.section + "]");
                    writer.newLine();
                    for (IniObj iniObj : sectionObj.iniList) {
                        if (iniObj.comment != null) {
                            writer.write(iniObj.comment);
                            writer.newLine();
                        }
                        writer.write(iniObj.key + "=" + iniObj.value);
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static ToolConfig.Core parseCoreSection(ToolUtil.SectionObj sectionObj, int index) {
        ToolConfig.Core core = new ToolConfig.Core();
        core.section = new ToolConfig.Info<>(sectionObj.section, index, sectionObj.comment);
        for (int i = 0; i < sectionObj.iniList.size(); i++) {
            ToolUtil.IniObj iniObj = sectionObj.iniList.get(i);
            if (I18n.CORE_LANGUAGE.ZH().equals(iniObj.key) || I18n.CORE_LANGUAGE.EN().equals(iniObj.key)) {
                core.language = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.CORE_THEME.ZH().equals(iniObj.key) || I18n.CORE_THEME.EN().equals(iniObj.key)) {
                core.theme = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.CORE_RENDER.ZH().equals(iniObj.key) || I18n.CORE_RENDER.EN().equals(iniObj.key)) {
                core.render = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.CORE_INI_EDITOR.ZH().equals(iniObj.key) || I18n.CORE_INI_EDITOR.EN().equals(iniObj.key)) {
                core.iniEditor = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.CORE_ACCENT.ZH().equals(iniObj.key) || I18n.CORE_ACCENT.EN().equals(iniObj.key)) {
                core.accent = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            }
        }
        // 兼容读取旧版本的配置文件
        if (core.language == null) {
            core.language = new ToolConfig.Info<>(Language.ZH_CN.type(), 0, I18n.LANGUAGE_COMMAND.comment());
        }
        if (core.theme == null) {
            core.theme = new ToolConfig.Info<>(Theme.LIGHT.type(), 1, I18n.THEME_COMMAND.comment());
        }
        if (core.render == null) {
            core.render = new ToolConfig.Info<>("auto", 2, I18n.RENDER_COMMAND.comment());
        }
        if (core.iniEditor == null) {
            core.iniEditor = new ToolConfig.Info<>("default", 3, I18n.INI_EDITOR_COMMAND.comment());
        }
        if (core.accent == null) {
            core.accent = new ToolConfig.Info<>("blue", 4, I18n.ACCENT_COMMAND.comment());
        }
        return core;
    }

    public static ToolUtil.SectionObj fromCore(ToolConfig.Core core) {
        ToolUtil.SectionObj sectionObj = new ToolUtil.SectionObj(core.section.data, core.section.comment);
        TreeMap<Integer, ToolUtil.IniObj> sortedMap = new TreeMap<>();
        sortedMap.put(core.language.sort, new ToolUtil.IniObj(I18n.CORE_LANGUAGE.EN(), core.language.data, core.language.comment));
        sortedMap.put(core.theme.sort, new ToolUtil.IniObj(I18n.CORE_THEME.EN(), core.theme.data, core.theme.comment));
        if (core.render != null) {
            sortedMap.put(core.render.sort, new ToolUtil.IniObj(I18n.CORE_RENDER.EN(), core.render.data, core.render.comment));
        }
        if (core.iniEditor != null) {
            sortedMap.put(core.iniEditor.sort, new ToolUtil.IniObj(I18n.CORE_INI_EDITOR.EN(), core.iniEditor.data, core.iniEditor.comment));
        }
        if (core.accent != null) {
            sortedMap.put(core.accent.sort, new ToolUtil.IniObj(I18n.CORE_ACCENT.EN(), core.accent.data, core.accent.comment));
        }
        sectionObj.iniList = new ArrayList<>(sortedMap.values());
        return sectionObj;
    }

    public static ToolConfig.Instance parseInstanceSection(ToolUtil.SectionObj sectionObj, int index) {
        ToolConfig.Instance instance = new ToolConfig.Instance();
        instance.section = new ToolConfig.Info<>(sectionObj.section, index, sectionObj.comment);
        for (int i = 0; i < sectionObj.iniList.size(); i++) {
            ToolUtil.IniObj iniObj = sectionObj.iniList.get(i);
            if (I18n.INSTANCE_PATH.ZH().equals(iniObj.key) || I18n.INSTANCE_PATH.EN().equals(iniObj.key)) {
                instance.path = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.INSTANCE_USERNAME.ZH().equals(iniObj.key) || I18n.INSTANCE_USERNAME.EN().equals(iniObj.key)) {
                instance.username = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.INSTANCE_PASSWORD.ZH().equals(iniObj.key) || I18n.INSTANCE_PASSWORD.EN().equals(iniObj.key)) {
                instance.password = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.INSTANCE_PORT.ZH().equals(iniObj.key) || I18n.INSTANCE_PORT.EN().equals(iniObj.key)) {
                instance.port = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (I18n.INSTANCE_DATABASE.ZH().equals(iniObj.key) || I18n.INSTANCE_DATABASE.EN().equals(iniObj.key)) {
                instance.database = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            }
        }
        // 兼容读取旧版本的配置文件
        if (instance.path == null || instance.username == null || instance.password == null) {
            return null;
        }
        if (instance.port == null) {
            instance.port = new ToolConfig.Info<>("3306", 4, I18n.PORT_COMMAND.comment());
        }
        if (instance.database == null) {
            instance.database = new ToolConfig.Info<>("beidou", 5, I18n.DATABASE_COMMAND.comment());
        }
        return instance;
    }

    public static ToolUtil.SectionObj fromInstance(ToolConfig.Instance instance) {
        ToolUtil.SectionObj sectionObj = new ToolUtil.SectionObj(instance.section.data, instance.section.comment);
        TreeMap<Integer, ToolUtil.IniObj> sortedMap = new TreeMap<>();
        sortedMap.put(instance.path.sort, new ToolUtil.IniObj(I18n.INSTANCE_PATH.EN(), instance.path.data, instance.path.comment));
        sortedMap.put(instance.username.sort, new ToolUtil.IniObj(I18n.INSTANCE_USERNAME.EN(), instance.username.data, instance.username.comment));
        sortedMap.put(instance.password.sort, new ToolUtil.IniObj(I18n.INSTANCE_PASSWORD.EN(), instance.password.data, instance.password.comment));
        sortedMap.put(instance.port.sort, new ToolUtil.IniObj(I18n.INSTANCE_PORT.EN(), instance.port.data, instance.port.comment));
        sortedMap.put(instance.database.sort, new ToolUtil.IniObj(I18n.INSTANCE_DATABASE.EN(), instance.database.data, instance.database.comment));
        sectionObj.iniList = new ArrayList<>(sortedMap.values());
        return sectionObj;
    }

    public interface DwmApi extends com.sun.jna.Library {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);
        WinNT.HRESULT DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    /** Win11 才有效的窗口圆角；旧系统直接跳过。 */
    public static void applyWindowCorners(Window window) {
        if (!isWin11()) {
            return;
        }
        long ptr = getWindowHandle(window);
        if (ptr == 0L) {
            return;
        }
        try {
            DwmApi.INSTANCE.DwmSetWindowAttribute(
                    new WinDef.HWND(new Pointer(ptr)),
                    33, // DWMWA_WINDOW_CORNER_PREFERENCE
                    new WinDef.DWORDByReference(new WinDef.DWORD(2)).getPointer(), // DWMWCP_ROUND
                    4
            );
        } catch (Throwable ignored) {
        }
    }

    public static long getWindowHandle(Window window) {
        long ptr = 0L;
        try {
            final Object tkStage = getWindowPeer(window);
            if (tkStage != null) {
                final Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
                getRawHandle.setAccessible(true);
                ptr = (Long) getRawHandle.invoke(tkStage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ptr;
    }

    public static WinDef.HWND getWindowHWND(Window window) {
        return new WinDef.HWND(new Pointer(getWindowHandle(window)));
    }

    private static Object getWindowPeer(Window window) throws Exception {
        try {
            Field peerField = Window.class.getDeclaredField("peer");
            Object unsafe = getUnsafe();
            Method objectFieldOffset = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
            Method getObject = unsafe.getClass().getMethod("getObject", Object.class, long.class);
            long offset = (Long) objectFieldOffset.invoke(unsafe, peerField);
            return getObject.invoke(unsafe, window, offset);
        } catch (Exception ignored) {
            final Method getPeer = Window.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            return getPeer.invoke(window);
        }
    }

    private static Object getUnsafe() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return theUnsafe.get(null);
    }

    public static int toRGBInt(final Color color) {
        return (doubleTo8Bit(color.getBlue()) << 16)
                | (doubleTo8Bit(color.getGreen()) << 8)
                | doubleTo8Bit(color.getRed());
    }

    public static int doubleTo8Bit(final double number) {
        return (int) Math.clamp(number * 255.0, 0.0, 255.0);
    }

    // ==================== Windows 版本 / 会话 ====================

    /** 读注册表拿真实 build 号，不依赖已下线的 wmic。 */
    public static int getWindowsBuild() {
        try {
            String build = Advapi32Util.registryGetStringValue(
                    WinReg.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                    "CurrentBuildNumber");
            return Integer.parseInt(build.trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean isWin11() {
        String osName = System.getProperty("os.name");
        if (osName == null || !osName.startsWith("Windows")) {
            return false;
        }
        return getWindowsBuild() >= 22000;
    }

    /** 远程桌面会话：RDP 下优先走软件渲染。 */
    public static boolean isRemoteSession() {
        try {
            return User32.INSTANCE.GetSystemMetrics(SM_REMOTESESSION) != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ==================== 进程 ====================

    public static String readPidFile(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (!Files.exists(Paths.get(file))) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** OpenProcess 探测存活：比 tasklist 快两个数量级，且不起子进程。 */
    public static boolean isPidRunning(String pid) {
        if (pid == null || pid.isEmpty()) {
            return false;
        }
        int id;
        try {
            id = Integer.parseInt(pid.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, id);
        if (handle == null) {
            // 拒绝访问说明进程还在，只是没权限打开
            return Native.getLastError() == ERROR_ACCESS_DENIED;
        }
        Kernel32.INSTANCE.CloseHandle(handle);
        return true;
    }

    public static boolean killPid(String pid) {
        if (pid == null || pid.isEmpty()) {
            return false;
        }
        int id;
        try {
            id = Integer.parseInt(pid.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return terminate(id);
    }

    private static boolean terminate(int pid) {
        if (pid <= 0 || pid == Kernel32.INSTANCE.GetCurrentProcessId()) {
            return false;
        }
        WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess(PROCESS_TERMINATE, false, pid);
        if (handle == null) {
            return false;
        }
        try {
            return Kernel32.INSTANCE.TerminateProcess(handle, 1);
        } finally {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }

    public static boolean isPortOccupied(int port) {
        return findPidByPort(port) > 0;
    }

    public static boolean killPortProcess(int port) {
        int pid = findPidByPort(port);
        return pid > 0 && terminate(pid);
    }

    /** GetExtendedTcpTable 查监听端口的 PID，替代 netstat|findstr。 */
    private static int findPidByPort(int port) {
        try {
            IntByReference size = new IntByReference(0);
            int rc = IPHlpAPI.INSTANCE.GetExtendedTcpTable(null, size, false, AF_INET, TCP_TABLE_OWNER_PID_LISTENER, 0);
            if (rc != ERROR_INSUFFICIENT_BUFFER || size.getValue() <= 0) {
                return -1;
            }
            Memory buffer = new Memory(size.getValue());
            rc = IPHlpAPI.INSTANCE.GetExtendedTcpTable(buffer, size, false, AF_INET, TCP_TABLE_OWNER_PID_LISTENER, 0);
            if (rc != 0) {
                return -1;
            }
            int count = buffer.getInt(0);
            for (int i = 0; i < count; i++) {
                // MIB_TCPROW_OWNER_PID: 6 × DWORD = 24 字节，表头 4 字节
                int offset = 4 + i * 24;
                int raw = buffer.getInt(offset + 8) & 0xFFFF;
                int localPort = ((raw & 0xFF) << 8) | ((raw >> 8) & 0xFF);
                if (localPort == port) {
                    return buffer.getInt(offset + 20);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** 枚举快照杀掉所有 mysqld.exe，替代 taskkill /F /IM。 */
    public static void killAllMysql() {
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        if (WinBase.INVALID_HANDLE_VALUE.equals(snapshot)) {
            return;
        }
        try {
            Tlhelp32.PROCESSENTRY32 entry = new Tlhelp32.PROCESSENTRY32();
            entry.dwSize = new WinDef.DWORD(entry.size());
            if (Kernel32.INSTANCE.Process32First(snapshot, entry)) {
                do {
                    if ("mysqld.exe".equalsIgnoreCase(exeName(entry.szExeFile))) {
                        terminate(entry.th32ProcessID.intValue());
                    }
                    entry.dwSize = new WinDef.DWORD(entry.size());
                } while (Kernel32.INSTANCE.Process32Next(snapshot, entry));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
    }

    private static String exeName(char[] buffer) {
        int end = 0;
        while (end < buffer.length && buffer[end] != '\0') {
            end++;
        }
        return new String(buffer, 0, end);
    }

    /**
     * 判断路径是否为纯 ASCII（无中文/全角等非 ASCII 字符）。
     * mysqld 在含非 ASCII 字符的路径下可能启动失败，用此方法做预检查。
     */
    public static void checkAsciiPath(String path) throws Exception {
        if (path == null || path.isEmpty()) {
            throw new Exception();
        }
        String abs = Paths.get(path).toAbsolutePath().toString();
        for (int i = 0; i < abs.length(); i++) {
            if (abs.charAt(i) > 0x7F) {
                throw new Exception();
            }
        }
    }

    public static void clearFile(File file) {
        try (FileWriter ignored = new FileWriter(file, false)) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkVcRuntime() throws Exception {
        Path system32 = Paths.get(System.getenv("SystemRoot"), "System32");
        if (!Files.exists(system32.resolve("vcruntime140.dll"))) {
            throw new Exception();
        }
    }

    // ==================== 外部程序 ====================

    /**
     * 用外部编辑器打开文件。
     * kind: default（系统关联）/ notepad / 其他可执行文件完整路径。
     */
    public static void openWithEditor(File file, String kind) throws Exception {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        String path = file.getAbsolutePath();
        String type = kind == null ? "" : kind.trim();
        if (type.isEmpty() || "default".equalsIgnoreCase(type)) {
            // FileProtocolHandler 走系统关联，且不依赖 AWT
            new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", path).start();
            return;
        }
        if ("notepad".equalsIgnoreCase(type)) {
            new ProcessBuilder("notepad.exe", path).start();
            return;
        }
        new ProcessBuilder(type, path).start();
    }

    public static void openFolder(String path) throws Exception {
        File file = new File(path);
        if (file.isDirectory()) {
            new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start();
        } else if (file.exists()) {
            new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
        } else {
            File parent = file.getParentFile();
            if (parent != null && parent.exists()) {
                new ProcessBuilder("explorer.exe", parent.getAbsolutePath()).start();
            }
        }
    }
}
