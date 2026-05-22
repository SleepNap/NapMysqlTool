package cn.nap;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ToolUtil {

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


    public static List<SectionObj> readConfig(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (!Files.exists(Paths.get(file))) {
            return null;
        }

        String line;
        SectionObj sectionObj = null;
        String lastComment = null;
        List<SectionObj> sectionList = new ArrayList<>();

        try (FileReader fr = new FileReader(file);
             BufferedReader reader = new BufferedReader(fr)) {
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
                    sectionObj = new SectionObj();
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


    public static void writeConfig(String file, ToolConfig config) {

    }


    public static ToolConfig.Core parseCoreSection(ToolUtil.SectionObj sectionObj, int index) {
        ToolConfig.Core core = new ToolConfig.Core();
        core.section = new ToolConfig.Info<>(sectionObj.section, index, sectionObj.comment);
        for (int i = 0; i < sectionObj.iniList.size(); i++) {
            ToolUtil.IniObj iniObj = sectionObj.iniList.get(i);
            if (ToolCommon.I18n.CORE_LANGUAGE.ZH().equals(iniObj.key) || ToolCommon.I18n.CORE_LANGUAGE.EN().equals(iniObj.key)) {
                core.language = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (ToolCommon.I18n.CORE_THEME.ZH().equals(iniObj.key) || ToolCommon.I18n.CORE_THEME.EN().equals(iniObj.key)) {
                core.theme = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            }
        }
        return core;
    }

    public static ToolUtil.SectionObj fromCore(ToolConfig.Core core) {
        ToolUtil.SectionObj sectionObj = new ToolUtil.SectionObj(core.section.data, core.section.comment);
        TreeMap<Integer, ToolUtil.IniObj> sortedMap = new TreeMap<>();
        sortedMap.put(core.language.sort, new ToolUtil.IniObj(ToolCommon.I18n.CORE_LANGUAGE.EN(), core.language.data, core.language.comment));
        sortedMap.put(core.theme.sort, new ToolUtil.IniObj(ToolCommon.I18n.CORE_THEME.EN(), core.theme.data, core.theme.comment));
        sectionObj.iniList = new ArrayList<>(sortedMap.values());
        return sectionObj;
    }

    public static ToolConfig.Instance parseInstanceSection(ToolUtil.SectionObj sectionObj, int index) {
        ToolConfig.Instance instance = new ToolConfig.Instance();
        instance.section = new ToolConfig.Info<>(sectionObj.section, index, sectionObj.comment);
        for (int i = 0; i < sectionObj.iniList.size(); i++) {
            ToolUtil.IniObj iniObj = sectionObj.iniList.get(i);
            if (ToolCommon.I18n.INSTANCE_PATH.ZH().equals(iniObj.key) || ToolCommon.I18n.INSTANCE_PATH.EN().equals(iniObj.key)) {
                instance.path = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (ToolCommon.I18n.INSTANCE_USERNAME.ZH().equals(iniObj.key) || ToolCommon.I18n.INSTANCE_USERNAME.EN().equals(iniObj.key)) {
                instance.username = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (ToolCommon.I18n.INSTANCE_PASSWORD.ZH().equals(iniObj.key) || ToolCommon.I18n.INSTANCE_PASSWORD.EN().equals(iniObj.key)) {
                instance.password = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (ToolCommon.I18n.INSTANCE_PORT.ZH().equals(iniObj.key) || ToolCommon.I18n.INSTANCE_PORT.EN().equals(iniObj.key)) {
                instance.port = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            } else if (ToolCommon.I18n.INSTANCE_DATABASE.ZH().equals(iniObj.key) || ToolCommon.I18n.INSTANCE_DATABASE.EN().equals(iniObj.key)) {
                instance.database = new ToolConfig.Info<>(iniObj.value, i, iniObj.comment);
            }
        }
        return instance;
    }

    public static ToolUtil.SectionObj fromInstance(ToolConfig.Instance instance) {
        ToolUtil.SectionObj sectionObj = new ToolUtil.SectionObj(instance.section.data, instance.section.comment);
        TreeMap<Integer, ToolUtil.IniObj> sortedMap = new TreeMap<>();
        sortedMap.put(instance.path.sort, new ToolUtil.IniObj(ToolCommon.I18n.INSTANCE_PATH.EN(), instance.path.data, instance.path.comment));
        sortedMap.put(instance.username.sort, new ToolUtil.IniObj(ToolCommon.I18n.INSTANCE_USERNAME.EN(), instance.username.data, instance.username.comment));
        sortedMap.put(instance.password.sort, new ToolUtil.IniObj(ToolCommon.I18n.INSTANCE_PASSWORD.EN(), instance.password.data, instance.password.comment));
        sortedMap.put(instance.port.sort, new ToolUtil.IniObj(ToolCommon.I18n.INSTANCE_PORT.EN(), instance.port.data, instance.port.comment));
        sortedMap.put(instance.database.sort, new ToolUtil.IniObj(ToolCommon.I18n.INSTANCE_DATABASE.EN(), instance.database.data, instance.database.comment));
        sectionObj.iniList = new ArrayList<>(sortedMap.values());
        return sectionObj;
    }

    public interface DwmApi extends Library {
        NapTheme.DwmApi INSTANCE = Native.load("dwmapi", NapTheme.DwmApi.class);
        WinNT.HRESULT DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }

    public static void setWindowFrameColor(Stage primaryStage, int rgb) {
        if (primaryStage.isShowing()) {
            NapTheme.DwmApi.INSTANCE.DwmSetWindowAttribute(
                    getWindowHWND(primaryStage),
                    35,
                    new WinDef.DWORDByReference(new WinDef.DWORD(rgb)),
                    4
            );
        } else {
            primaryStage.setOnShown(e -> NapTheme.DwmApi.INSTANCE.DwmSetWindowAttribute(
                    getWindowHWND(primaryStage),
                    35,
                    new WinDef.DWORDByReference(new WinDef.DWORD(rgb)),
                    4
            ));
        }
    }

    public static void setWindowDarkMode(boolean isWin11, Stage primaryStage, boolean dark) {
        if (isWin11) {
            if (primaryStage.isShowing()) {
                NapTheme.DwmApi.INSTANCE.DwmSetWindowAttribute(
                        getWindowHWND(primaryStage),
                        20,
                        new WinDef.BOOLByReference(new WinDef.BOOL(dark)),
                        4
                );
            } else {
                primaryStage.setOnShown(e -> NapTheme.DwmApi.INSTANCE.DwmSetWindowAttribute(
                        getWindowHWND(primaryStage),
                        20,
                        new WinDef.BOOLByReference(new WinDef.BOOL(dark)),
                        4
                ));
            }
            return;
        }
        // win10
        NativeLibrary user32 = NativeLibrary.getInstance("user32");
        Function user32Function = user32.getFunction("SetWindowCompositionAttribute");
        WinDef.HMODULE hmodule = Kernel32.INSTANCE.GetModuleHandle("uxtheme.dll");
        Kernel32 kernel32 = Native.load(Kernel32.class);
        Function kernel32Function = Function.getFunction(kernel32.GetProcAddress(hmodule, 133));
        kernel32Function.invoke(new Object[]{getWindowHWND(primaryStage), new WinDef.BOOL(dark)});
        NapTheme.WindowCompositionAttributeData data = new NapTheme.WindowCompositionAttributeData();
        data.Attribute = 26;
        data.Data = new WinDef.BOOLByReference(new WinDef.BOOL(dark)).getPointer();
        data.SizeOfData = 4;
        user32Function.invoke(WinNT.HRESULT.class, new Object[]{getWindowHWND(primaryStage), data});
        // 通过修改宽度，触发窗口重绘，强制刷新暗黑模式
        if (dark) {
            primaryStage.setWidth(primaryStage.getWidth() + 1);
        } else {
            primaryStage.setWidth(primaryStage.getWidth() - 1);
        }
    }

    public static WinDef.HWND getWindowHWND(Window window) {
        long ptr = 0L;
        try {
            final Method getPeer = Window.class.getDeclaredMethod("impl_getPeer");
            getPeer.setAccessible(true);
            final Object tkStage = getPeer.invoke(window);
            final Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
            getRawHandle.setAccessible(true);
            ptr = (Long) getRawHandle.invoke(tkStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new WinDef.HWND(new Pointer(ptr));
    }

    public static int toRGBInt(final Color color) {
        return (doubleTo8Bit(color.getBlue()) << 16)
                | (doubleTo8Bit(color.getGreen()) << 8)
                | doubleTo8Bit(color.getRed());
    }

    public static int doubleTo8Bit(final double number) {
        return (int) Math.min(255.0, Math.max(number * 255.0, 0.0));
    }

    public static boolean isWin11() {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return false;
        }
        if ("Windows 11".equals(osName)) {
            return true;
        }

        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("wmic os get version");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("10")) {
                    continue;
                }
                String[] splits = line.split("\\.");
                if (splits.length != 3) {
                    continue;
                }
                if (splits[2].length() < 2) {
                    continue;
                }
                int build = Integer.parseInt(splits[2].trim());
                return build >= 22000;
            }
            process.destroy();
            reader.close();
        } catch (IOException e) {
            if (process != null) {
                process.destroy();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        }
        return false;
    }
}

