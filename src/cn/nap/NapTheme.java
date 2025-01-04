package cn.nap;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.event.EventTarget;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class NapTheme {
    public interface DwmApi extends Library {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);

        WinNT.HRESULT DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }

    public static class ToggleLabel extends Label implements Toggle {
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();

        public ToggleLabel() {
            super();
        }

        public ToggleLabel(String text) {
            super(text);
        }

        @Override
        public ToggleGroup getToggleGroup() {
            return toggleGroup.get();
        }

        @Override
        public void setToggleGroup(ToggleGroup toggleGroup) {
            this.toggleGroup.set(toggleGroup);
            if (toggleGroup != null) {
                toggleGroup.getToggles().add(this);
                setOnMouseClicked(e -> setSelected(true));
            }
        }

        @Override
        public ObjectProperty<ToggleGroup> toggleGroupProperty() {
            return toggleGroup;
        }

        @Override
        public boolean isSelected() {
            return selected.get();
        }

        @Override
        public synchronized void setSelected(boolean selected) {
            if (this.selected.get() == selected) {
                return;
            }
            this.selected.set(selected);
            if (toggleGroup.get() == null) {
                return;
            }

            if (selected) {
                if (toggleGroup.get().getSelectedToggle() != this) {
                    toggleGroup.get().selectToggle(this);
                }
                setUnderline(true);
            } else {
                if (toggleGroup.get().getSelectedToggle() == this) {
                    toggleGroup.get().selectToggle(null);
                }
                setUnderline(false);
            }
        }

        @Override
        public BooleanProperty selectedProperty() {
            return selected;
        }
    }

    public static class WindowCompositionAttributeData extends Structure implements Structure.ByReference {
        public int Attribute;
        public Pointer Data;
        public int SizeOfData;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Attribute", "Data", "SizeOfData");
        }
    }

    public static final String SVG_DARK = "M593.054 120.217C483.656 148.739 402.91 248.212 402.91 366.546c0 140.582 113.962 254.544 254.544 254.544 118.334 0 217.808-80.746 246.328-190.144C909.17 457.12 912 484.23 912 512c0 220.914-179.086 400-400 400S112 732.914 112 512s179.086-400 400-400c27.77 0 54.88 2.83 81.054 8.217z";
    public static final String SVG_LIGHT = "M512 831.508c16.264 0 29.792 11.72 32.597 27.176l0.534 5.955v62.23C545.13 945.167 530.298 960 512 960c-16.265 0-29.792-11.72-32.597-27.175l-0.534-5.956v-62.23c0-18.298 14.833-33.13 33.131-33.13z m267.584-97.826l5.197 4.245 44.002 44.002c12.938 12.938 12.938 33.916 0 46.854-11.321 11.321-28.797 12.736-41.656 4.245l-5.198-4.245-44.002-44.002c-12.938-12.938-12.938-33.915 0-46.854 11.321-11.32 28.797-12.736 41.657-4.245z m-493.511 4.246c11.32 11.32 12.736 28.796 4.245 41.656l-4.246 5.197-44.002 44.002c-12.939 12.938-33.916 12.938-46.854 0-11.32-11.321-12.736-28.797-4.245-41.657l4.245-5.197 44.003-44.002c12.938-12.938 33.916-12.938 46.854 0zM512 274.932c130.929 0 237.068 106.14 237.068 237.068 0 130.929-106.14 237.068-237.068 237.068-130.929 0-237.068-106.14-237.068-237.068 0-130.929 106.14-237.068 237.068-237.068z m414.87 203.937c18.297 0 33.13 14.833 33.13 33.131 0 16.264-11.72 29.792-27.175 32.597l-5.956 0.534h-62.23c-18.298 0-33.13-14.833-33.13-33.131 0-16.265 11.72-29.792 27.175-32.597l5.955-0.534h62.23z m-767.509 0c18.298 0 33.13 14.833 33.13 33.131 0 16.264-11.72 29.792-27.175 32.597l-5.955 0.534h-62.23C78.833 545.13 64 530.298 64 512c0-16.265 11.72-29.792 27.175-32.597l5.956-0.534h62.23z m669.422-283.653c11.321 11.32 12.736 28.797 4.246 41.656l-4.246 5.198-44.002 44.002c-12.938 12.939-33.915 12.94-46.853 0-11.322-11.32-12.737-28.796-4.246-41.656l4.245-5.197 44.002-44.003c12.938-12.938 33.915-12.938 46.854 0z m-591.91-4.245l5.197 4.245 44.003 44.003c12.938 12.938 12.938 33.915 0 46.854-11.321 11.32-28.797 12.736-41.657 4.245l-5.197-4.245-44.003-44.003c-12.938-12.938-12.938-33.915 0-46.854 11.321-11.32 28.797-12.736 41.657-4.245zM512 64c16.264 0 29.792 11.72 32.597 27.175l0.534 5.956v62.23c0 18.298-14.833 33.13-33.131 33.13-16.265 0-29.792-11.72-32.597-27.175l-0.534-5.955v-62.23C478.87 78.833 493.702 64 512 64z";

    public Region createGraphic(String style, String color, int size) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(style);
        Region region = new Region();
        region.setShape(svgPath);
        region.setStyle("-fx-background-color:" + color);
        region.setPrefSize(size, size);
        region.setMaxSize(size, size);
        return region;
    }

    public static final NapTheme INSTANCE = new NapTheme();
    private Stage primaryStage;
    private final BooleanProperty dark = new SimpleBooleanProperty(false) {
        @Override
        public void set(boolean newValue) {
            super.set(newValue);
            applyThemeChange();
        }
    };
    private final MapProperty<EventTarget, Pair<Runnable, Runnable>> targets = new SimpleMapProperty<EventTarget, Pair<Runnable, Runnable>>(FXCollections.observableHashMap()) {
        @Override
        public Pair<Runnable, Runnable> put(EventTarget key, Pair<Runnable, Runnable> value) {
            if (dark.get()) {
                value.getValue().run();
            } else {
                value.getKey().run();
            }
            return super.put(key, value);
        }
    };

    public void applyThemeChange() {
        targets.forEach((eventTarget, pair) -> {
            if (dark.get()) {
                pair.getValue().run();
            } else {
                pair.getKey().run();
            }
        });
        if (isWin11()) {
            setWindowFrameColor();
        } else {
            setWindowDarkMode();
        }
    }

    private boolean isWin11() {
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

    public void setWindowFrameColor() {
        if (primaryStage.isShowing()) {
            DwmApi.INSTANCE.DwmSetWindowAttribute(
                    getWindowHWND(primaryStage),
                    35,
                    new WinDef.DWORDByReference(new WinDef.DWORD(toRGBInt(Color.web(dark.get() ? "#374357" : "#5c5c5c")))),
                    4
            );
        } else {
            primaryStage.setOnShown(e -> DwmApi.INSTANCE.DwmSetWindowAttribute(
                    getWindowHWND(primaryStage),
                    35,
                    new WinDef.DWORDByReference(new WinDef.DWORD(toRGBInt(Color.web(dark.get() ? "#374357" : "#5c5c5c")))),
                    4
            ));
        }
    }

    public void setWindowDarkMode() {
        if (isWin11()) {
            if (primaryStage.isShowing()) {
                DwmApi.INSTANCE.DwmSetWindowAttribute(
                        getWindowHWND(primaryStage),
                        20,
                        new WinDef.BOOLByReference(new WinDef.BOOL(dark.get())),
                        4
                );
            } else {
                primaryStage.setOnShown(e -> DwmApi.INSTANCE.DwmSetWindowAttribute(
                        getWindowHWND(primaryStage),
                        20,
                        new WinDef.BOOLByReference(new WinDef.BOOL(dark.get())),
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
        kernel32Function.invoke(new Object[]{getWindowHWND(primaryStage), new WinDef.BOOL(dark.get())});
        WindowCompositionAttributeData data = new WindowCompositionAttributeData();
        data.Attribute = 26;
        data.Data = new WinDef.BOOLByReference(new WinDef.BOOL(dark.get())).getPointer();
        data.SizeOfData = 4;
        user32Function.invoke(WinNT.HRESULT.class, new Object[]{getWindowHWND(primaryStage), data});
        // 通过修改宽度，触发窗口重绘，强制刷新暗黑模式
        if (dark.get()) {
            primaryStage.setWidth(primaryStage.getWidth() + 1);
        } else {
            primaryStage.setWidth(primaryStage.getWidth() - 1);
        }
    }

    private WinDef.HWND getWindowHWND(Window window) {
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

    private int toRGBInt(final Color color) {
        return (doubleTo8Bit(color.getBlue()) << 16)
                | (doubleTo8Bit(color.getGreen()) << 8)
                | doubleTo8Bit(color.getRed());
    }

    private int doubleTo8Bit(final double number) {
        return (int) Math.min(255.0, Math.max(number * 255.0, 0.0));
    }

    public boolean isDark() {
        return dark.get();
    }

    public void setDark(boolean dark) {
        this.dark.set(dark);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public MapProperty<EventTarget, Pair<Runnable, Runnable>> targetsProperty() {
        return targets;
    }
}
