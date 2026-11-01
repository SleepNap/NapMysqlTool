package cn.nap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static cn.nap.ToolCommon.Status;
import static cn.nap.ToolCommon.ThemeColor;

/** 入口：无边框自绘窗口 + 状态驱动渲染 + 后台操作。 */
public class ToolApp extends Application {

    public static class Running {
        public final ToolConfig.Instance instance;
        public final String label;
        public final long start;

        public Running(ToolConfig.Instance instance, String label, long start) {
            this.instance = instance;
            this.label = label;
            this.start = start;
        }
    }

    public static class IniSession {
        public final ToolConfig.Instance instance;
        public final File file;
        public final String editor;
        public long mtime;
        public boolean saved;
        public boolean deleted;

        public IniSession(ToolConfig.Instance instance, File file, String editor) {
            this.instance = instance;
            this.file = file;
            this.editor = editor;
            this.mtime = file.lastModified();
            this.deleted = !file.isFile();
        }

        public void refresh() {
            long modified = file.lastModified();
            boolean missing = !file.isFile();
            if (modified != mtime || missing != deleted) {
                mtime = modified;
                deleted = missing;
                saved = !missing;
            }
        }
    }

    public static class LogEntry {
        public final String time;
        public final String text;
        public final boolean error;

        public LogEntry(String time, String text, boolean error) {
            this.time = time;
            this.text = text;
            this.error = error;
        }
    }

    private final ToolService service = ToolService.getInstance();
    private final List<LogEntry> logs = new ArrayList<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss");

    private MainView view;
    private Timeline ticker;
    private Running running;
    private IniSession ini;

    public static void main(String[] args) {
        // 渲染管线必须在 launch 之前定：Server 2016/2019 与 RDP 默认走软件渲染
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.order", resolveRenderOrder());
        launch(args);
    }

    private static String resolveRenderOrder() {
        String configured = null;
        List<ToolUtil.SectionObj> sections = ToolUtil.readConfig(ToolCommon.DEFAULT_CONFIG_FILE);
        if (sections != null) {
            for (ToolUtil.SectionObj section : sections) {
                for (ToolUtil.IniObj item : section.iniList) {
                    if ("render".equalsIgnoreCase(item.key) || "渲染".equals(item.key)) {
                        configured = item.value;
                    }
                }
            }
        }
        if ("sw".equalsIgnoreCase(configured)) {
            return "sw";
        }
        if ("d3d".equalsIgnoreCase(configured)) {
            return "d3d,sw";
        }
        boolean software = ToolUtil.isRemoteSession() || ToolUtil.getWindowsBuild() < 22000;
        return software ? "sw" : "d3d,sw";
    }

    @Override
    public void init() {
        // 现在读取配置只有注册表 + OpenProcess，足够快，直接同步加载，不再 join
        service.loadConfig();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle(ToolCommon.I18n.TOOL_TITLE.translate(service.getLanguage()));
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(ToolCommon.WIN_MIN_WIDTH);
        primaryStage.setMinHeight(ToolCommon.WIN_MIN_HEIGHT);
        int instanceCount = service.getConfig().instances == null ? 0 : service.getConfig().instances.size();
        double initialWidth = ToolCommon.windowWidthFor(instanceCount);
        double initialHeight = ToolCommon.windowHeightFor(instanceCount);
        primaryStage.setWidth(initialWidth);
        primaryStage.setHeight(initialHeight);

        openSettingsOnStart = Boolean.getBoolean("nap.openSettings");
        view = new MainView(this, primaryStage);
        Scene scene = new Scene(view.getRoot(), initialWidth, initialHeight);
        scene.setFill(Color.web(ThemeColor.BACKGROUND.color()));
        primaryStage.setScene(scene);
        primaryStage.show();

        ToolUtil.applyWindowCorners(primaryStage);

        // 恢复时重建一次，避免最小化久了留下黑块
        primaryStage.iconifiedProperty().addListener((observable, previous, iconified) -> {
            if (!iconified) {
                Platform.runLater(() -> {
                    if (view != null) {
                        view.render();
                        view.getRoot().requestLayout();
                    }
                });
            }
        });

        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        ticker.setCycleCount(Timeline.INDEFINITE);

        addLog(ToolCommon.I18n.TOOL_TITLE.translate(service.getLanguage()) + " " + ToolCommon.VERSION, false);
        if (service.isLoadFailed()) {
            addLog("config.ini 读取失败，本次不会回写配置（请检查文件编码/占用）", true);
        }
    }

    @Override
    public void stop() {
        service.saveConfig();
        if (ticker != null) {
            ticker.stop();
        }
    }

    private void tick() {
        if (ini != null) {
            ini.refresh();
        }
        if (view != null) {
            view.refreshDurations();
        }
    }

    public void startTickerIfNeeded() {
        if (ticker == null) {
            return;
        }
        if (running != null || ini != null) {
            if (ticker.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                ticker.play();
            }
        } else {
            ticker.stop();
        }
    }

    /** 后台执行一次实例操作，期间锁住其它按钮。 */
    public void startOperation(ToolConfig.Instance instance, Status active, Status finish,
                               String label, Callable<Boolean> action) {
        if (running != null) {
            return;
        }
        int previousStatus = instance.status;
        running = new Running(instance, label, System.currentTimeMillis());
        instance.status = active.type();
        startTickerIfNeeded();
        if (view != null) {
            view.render();
        }
        new Thread(() -> {
            boolean ok;
            String error = null;
            try {
                ok = action.call();
            } catch (Throwable t) {
                ok = false;
                error = t.getMessage() == null ? t.toString() : t.getMessage();
            }
            final boolean success = ok;
            final String message = error;
            Platform.runLater(() -> {
                running = null;
                instance.status = success ? finish.type()
                        : (active == Status.STARTED ? previousStatus
                        : (finish == Status.STARTED ? Status.STOPPED.type() : finish.type()));
                String result = success
                        ? ToolCommon.I18n.STEP_DONE.translate(service.getLanguage())
                        : ToolCommon.I18n.STEP_FAIL.translate(service.getLanguage());
                addLog(instance.section.data + " · " + result + (message == null ? "" : ": " + message), !success);
                startTickerIfNeeded();
                if (view != null) {
                    view.render();
                }
            });
        }, "nap-operation").start();
    }

    public void addLog(String text, boolean error) {
        logs.add(new LogEntry(clock.format(new Date()), text, error));
        while (logs.size() > 200) {
            logs.removeFirst();
        }
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public Running getRunning() {
        return running;
    }

    public IniSession getIni() {
        return ini;
    }

    public void setIni(IniSession session) {
        this.ini = session;
        startTickerIfNeeded();
    }

    private boolean openSettingsOnStart;

    public boolean consumeOpenSettingsOnStart() {
        boolean value = openSettingsOnStart;
        openSettingsOnStart = false;
        return value;
    }
}
