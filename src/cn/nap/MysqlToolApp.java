package cn.nap;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlToolApp extends Application {
    private final Map<String, Map<String, String>> iniProp = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        // 初始化ini文件
        File iniFile = new File("config.ini");
        if (iniFile.exists()) {
            Map<String, Map<String, String>> readedMap = MysqlUtils.readIniFile(iniFile.getAbsolutePath());
            if (readedMap != null) {
                iniProp.putAll(readedMap);
            } else {
                initDefaultIni();
            }
        } else {
            initDefaultIni();
        }
        // 检查配置，避免乱配导致的异常
        Map<String, String> mysqlConf = iniProp.get("mysql配置");
        if (mysqlConf == null) {
            initDefaultIni();
            mysqlConf = iniProp.get("mysql配置");
        }
        String mysqlPath = mysqlConf.get("mysql路径");
        String mysqlUser = mysqlConf.get("mysql账号");
        String mysqlPass = mysqlConf.get("mysql密码");
        // 密码可为空字符，但不能为null
        if (mysqlPath == null || mysqlUser == null || mysqlPass == null || mysqlPath.isEmpty() || mysqlUser.isEmpty()) {
            initDefaultIni();
        }
        Map<String, String> toolConf = iniProp.get("工具配置");
        if (toolConf == null) {
            initDefaultIni();
            toolConf = iniProp.get("工具配置");
        }
        String initPath = toolConf.get("初始化脚本路径");
        if (initPath == null || initPath.isEmpty()) {
            initDefaultIni();
            toolConf = iniProp.get("工具配置");
        }
        String dbName = toolConf.get("导出的库名，多个用空格分割");
        if (dbName == null || dbName.isEmpty()) {
            initDefaultIni();
            toolConf = iniProp.get("工具配置");
        }
        String theme = toolConf.get("主题");
        if (theme == null || theme.isEmpty()) {
            initDefaultIni();
            toolConf = iniProp.get("工具配置");
        }
        String i18n = toolConf.get("多语言");
        if (i18n == null || i18n.isEmpty()) {
            initDefaultIni();
            toolConf = iniProp.get("工具配置");
        }
        NapTheme.setI18n("zh-CN".equals(toolConf.get("多语言")));
        File initDir = new File(toolConf.get("初始化脚本路径"));
        // 创建init文件夹
        if (!initDir.exists()) {
            boolean ignore = initDir.mkdirs();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        NapTheme.INSTANCE.setPrimaryStage(primaryStage);

        BorderPane root = new BorderPane();
        NapTheme.INSTANCE.targetsProperty().put(root, new Pair<>(
                () -> root.setStyle("-fx-background-color: #F2F2F2;"),
                () -> root.setStyle("-fx-background-color: #2b2b2b;")
        ));

        BorderPane top = new BorderPane();
        ToggleGroup typeGroup = new ToggleGroup();
        top.setRight(createThemeButton());
        NapTheme.ToggleLabel startStopType = createSelectedLabel(NapTheme.I18n.START_STOP.get());
        NapTheme.ToggleLabel importExportType = createSelectedLabel(NapTheme.I18n.OTHERS.get());
        startStopType.setToggleGroup(typeGroup);
        importExportType.setToggleGroup(typeGroup);

        HBox topCenter = new HBox(startStopType, importExportType);
        topCenter.setStyle("-fx-alignment: center;-fx-spacing: 20");
        top.setCenter(topCenter);

        GridPane bottom = new GridPane();
        NapTheme.INSTANCE.targetsProperty().put(bottom, new Pair<>(
                () -> bottom.setStyle("-fx-background-color: transparent;-fx-alignment: center;-fx-hgap: 100;-fx-vgap: 10;-fx-padding: 10"),
                () -> bottom.setStyle("-fx-background-color: transparent;-fx-alignment: center;-fx-hgap: 100;-fx-vgap: 10;-fx-padding: 10")
        ));
        Label version = createNormalLabel("2.25.0102");
        Label status = createNormalLabel(NapTheme.I18n.OUTPUT_INITIALIZING.get());
        bottom.add(version, 0, 0);
        bottom.add(status, 1, 0);

        root.setTop(top);
        root.setBottom(bottom);
        Scene scene = new Scene(root, 400, 150);
        NapTheme.INSTANCE.targetsProperty().put(scene, new Pair<>(
                () -> scene.setFill(Color.web("#F2F2F2")),
                () -> scene.setFill(Color.web("#2b2b2b"))
        ));
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle(NapTheme.I18n.TITLE.get());
        primaryStage.show();

        typeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == startStopType) {
                startStopUI(root, status);
            } else if (newValue == importExportType) {
                importExportUI(root, status);
            }
        });
        typeGroup.selectToggle(startStopType);
        // 设置默认主题
        Map<String, String> toolConf = iniProp.get("工具配置");
        NapTheme.INSTANCE.setDark("dark".equals(toolConf.get("主题")));
    }

    @Override
    public void stop() throws Exception {
        Map<String, String> toolConf = iniProp.get("工具配置");
        toolConf.put("主题", NapTheme.INSTANCE.isDark() ? "dark" : "light");
        MysqlUtils.writeIniFile(iniProp, "config.ini");
    }

    private void startMysql(Label status) {
        for (int i = 0; i < 3; i++) {
            try {
                if (MysqlOperator.hasPid(iniProp)) {
                    initSqlScript(status);
                    break;
                }
                Process process = MysqlOperator.start(iniProp);
                // 惊了，输出为什么在ErrorStream里？
                InputStream inputStream = process.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                long startTime = System.currentTimeMillis();
                // 最多等待3s
                while (System.currentTimeMillis() - startTime < 5000) {
                    String output = reader.readLine();
                    System.out.println(output);
                    if (output != null && output.contains("ready for connections")) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopMysql() {
        for (int i = 0; i < 3; i++) {
            try {
                if (!MysqlOperator.hasPid(iniProp)) {
                    break;
                }
                MysqlOperator.stop(iniProp).waitFor();
                if (i == 2) {
                    MysqlOperator.killPid(iniProp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fixMysql(Label status) {
        if (MysqlOperator.hasPid(iniProp) && MysqlOperator.isStarted(iniProp)) {
            Platform.runLater(() -> status.setText(NapTheme.I18n.REPAIR_ERR1.get()));
            return;
        }
        MysqlOperator.fix(iniProp);
        Platform.runLater(() -> status.setText(NapTheme.I18n.REPAIR_SUCCESS.get()));
    }

    private void exportMysql(Label status) {
        boolean started = MysqlOperator.hasPid(iniProp);
        if (!started) {
            Platform.runLater(() -> status.setText(NapTheme.I18n.EXPORT_ERR1.get()));
            return;
        }
        try {
            MysqlOperator.outDb(iniProp).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> status.setText(NapTheme.I18n.EXPORT_SUCCESS.get()));
    }

    private void importMysql(Label status) {
        boolean started = MysqlOperator.hasPid(iniProp);
        if (!started) {
            Platform.runLater(() -> status.setText(NapTheme.I18n.IMPORT_ERR1.get()));
            return;
        }
        File file = new File("output.sql");
        if (!file.exists()) {
            Platform.runLater(() -> status.setText(NapTheme.I18n.IMPORT_ERR2.get()));
            return;
        }
        try {
            MysqlOperator.initSql(iniProp, "", file.getAbsolutePath()).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> status.setText(NapTheme.I18n.IMPORT_SUCCESS.get()));
    }

    private void initDefaultIni() {
        Map<String, String> mysqlConf = new HashMap<>();
        mysqlConf.put("mysql路径", "mysql-8.0.39-winx64");
        mysqlConf.put("mysql账号", "root");
        mysqlConf.put("mysql密码", "root");
        mysqlConf.put("mysql.ini路径", "");
        iniProp.put("mysql配置", mysqlConf);

        Map<String, String> toolConf = new HashMap<>();
        toolConf.put("初始化脚本路径", "init");
        toolConf.put("导出的库名，多个用空格分割", "beidou");
        toolConf.put("主题", "dark");
        toolConf.put("多语言", "zh-CN");

        iniProp.put("工具配置", toolConf);
        MysqlUtils.writeIniFile(iniProp, "config.ini");
    }

    private void initSqlScript(Label status) throws Exception {
        String initPath = iniProp.get("工具配置").get("初始化脚本路径");
        File initDir = new File(initPath);
        if (!initDir.exists()) {
            boolean ignore = initDir.mkdirs();
        }
        File[] listFiles = initDir.listFiles();
        if (listFiles == null) {
            return;
        }
        List<String> finishedList = MysqlUtils.readFinishedList("已初始化列表(别乱动).txt");
        Platform.runLater(() -> status.setText("初始化脚本中..."));
        for (File file : listFiles) {
            // 如果是文件夹，文件的名字就是要执行数据库的库名
            if (file.isDirectory()) {
                File[] listChildFiles = file.listFiles();
                if (listChildFiles == null) {
                    continue;
                }
                for (File childFile : listChildFiles) {
                    if (finishedList.contains(childFile.getAbsolutePath())) {
                        continue;
                    }
                    MysqlOperator.initSql(iniProp, file.getName(), childFile.getAbsolutePath()).waitFor();
                    MysqlUtils.appendFinishedList("已初始化列表(别乱动).txt", childFile.getAbsolutePath());
                }
                continue;
            }
            // 如果不在文件夹里，就不指定库名更新
            if (file.getName().endsWith(".sql")) {
                if (finishedList.contains(file.getAbsolutePath())) {
                    continue;
                }
                MysqlOperator.initSql(iniProp, "", file.getAbsolutePath()).waitFor();
                MysqlUtils.appendFinishedList("已初始化列表(别乱动).txt", file.getAbsolutePath());
            }
        }
    }

    private Button createThemeButton() {
        Button button = new Button();
        NapTheme.INSTANCE.targetsProperty().put(button, new Pair<>(
                () -> {
                    button.setStyle("-fx-background-color: transparent;");
                    button.setGraphic(NapTheme.INSTANCE.createGraphic(NapTheme.SVG_DARK, "#5c5c5c", 12));
                },
                () -> {
                    button.setStyle("-fx-background-color: transparent;");
                    button.setGraphic(NapTheme.INSTANCE.createGraphic(NapTheme.SVG_LIGHT, "#FFFFFF", 12));
                }
        ));
        button.setOnAction(event -> NapTheme.INSTANCE.setDark(!NapTheme.INSTANCE.isDark()));
        return button;
    }

    private NapTheme.ToggleLabel createSelectedLabel(String text) {
        NapTheme.ToggleLabel label = new NapTheme.ToggleLabel(text);
        NapTheme.INSTANCE.targetsProperty().put(label, new Pair<>(
                () -> label.setStyle("-fx-background-color: transparent;-fx-text-fill: #5c5c5c;-fx-font-weight: bold;-fx-font-size: 14px"),
                () -> label.setStyle("-fx-background-color: transparent;-fx-text-fill: #FFFFFF;-fx-font-weight: bold;-fx-font-size: 14px")
        ));
        return label;
    }

    private Label createNormalLabel(String text) {
        Label label = new Label(text);
        NapTheme.INSTANCE.targetsProperty().put(label, new Pair<>(
                () -> label.setStyle("-fx-text-fill: #5c5c5c;-fx-font-size: 14px"),
                () -> label.setStyle("-fx-text-fill: #FFFFFF;-fx-font-size: 14px")
        ));
        return label;
    }

    private void startStopUI(BorderPane root, Label status) {
        TilePane center = createCenter(root);
        Button start = createOperateButton(NapTheme.I18n.START.get());
        Button stop = createOperateButton(NapTheme.I18n.STOP.get());
        stop.setDisable(true);
        Button restart = createOperateButton(NapTheme.I18n.RESTART.get());
        center.getChildren().addAll(start, stop, restart);
        root.setCenter(center);

        disableAll(start, stop, restart);
        updateOperateStat(status, start, stop, restart);
        start.setOnAction(event -> {
            disableAll(start, stop, restart);
            status.setText(NapTheme.I18n.OUTPUT_STARTING.get());
            new Thread(() -> {
                startMysql(status);
                Platform.runLater(() -> updateOperateStat(status, start, stop, restart));
            }).start();
        });
        stop.setOnAction(event -> {
            disableAll(start, stop, restart);
            status.setText(NapTheme.I18n.OUTPUT_STOPPING.get());
            new Thread(() -> {
                stopMysql();
                Platform.runLater(() -> updateOperateStat(status, start, stop, restart));
            }).start();
        });
        restart.setOnAction(event -> {
            disableAll(start, stop, restart);
            status.setText(NapTheme.I18n.OUTPUT_RESTARTING.get());
            new Thread(() -> {
                stopMysql();
                startMysql(status);
                Platform.runLater(() -> updateOperateStat(status, start, stop, restart));
            }).start();
        });
    }

    private void importExportUI(BorderPane root, Label status) {
        TilePane center = createCenter(root);
        Button repair = createOperateButton(NapTheme.I18n.REPAIR.get());
        Button imp = createOperateButton(NapTheme.I18n.IMPORT.get());
        Button exp = createOperateButton(NapTheme.I18n.EXPORT.get());
        center.getChildren().addAll(repair, imp, exp);
        root.setCenter(center);
        disableAll(repair, imp, exp);
        updateToolStat(repair, imp, exp);

        repair.setOnAction(event -> {
            disableAll(repair, imp, exp);
            status.setText(NapTheme.I18n.OUTPUT_STOPPING.get());
            new Thread(() -> {
                stopMysql();
                PlatformImpl.runAndWait(() -> status.setText(NapTheme.I18n.OUTPUT_REPAIRING.get()));
                fixMysql(status);
                PlatformImpl.runAndWait(() -> updateToolStat(repair, imp, exp));
            }).start();
        });
        imp.setOnAction(event -> {
            disableAll(repair, imp, exp);
            status.setText(NapTheme.I18n.OUTPUT_STARTING.get());
            new Thread(() -> {
                startMysql(status);
                PlatformImpl.runAndWait(() -> status.setText(NapTheme.I18n.OUTPUT_IMPORTING.get()));
                importMysql(status);
                PlatformImpl.runAndWait(() -> updateToolStat(repair, imp, exp));
            }).start();
        });
        exp.setOnAction(event -> {
            disableAll(repair, imp, exp);
            status.setText(NapTheme.I18n.OUTPUT_STARTING.get());
            new Thread(() -> {
                startMysql(status);
                PlatformImpl.runAndWait(() -> status.setText(NapTheme.I18n.OUTPUT_EXPORTING.get()));
                exportMysql(status);
                PlatformImpl.runAndWait(() -> updateToolStat(repair, imp, exp));
            }).start();
        });
    }

    private TilePane createCenter(BorderPane root) {
        Pane lastNode = (Pane) root.getCenter();
        if (lastNode != null) {
            lastNode.getChildren().forEach(node -> NapTheme.INSTANCE.targetsProperty().remove(node));
            NapTheme.INSTANCE.targetsProperty().remove(lastNode);
        }

        TilePane center = new TilePane();
        NapTheme.INSTANCE.targetsProperty().put(center, new Pair<>(
                () -> center.setStyle("-fx-background-color: transparent;-fx-alignment: center;-fx-hgap: 10;-fx-vgap: 10"),
                () -> center.setStyle("-fx-background-color: transparent;-fx-alignment: center;-fx-hgap: 10;-fx-vgap: 10")
        ));
        return center;
    }

    private Button createOperateButton(String text) {
        Button button = new Button(text);
        NapTheme.INSTANCE.targetsProperty().put(button, new Pair<>(
                () -> button.setStyle("-fx-background-color: #5c5c5c;-fx-text-fill: #F5F5F5;-fx-font-size: 14px;-fx-pref-width: 100px;-fx-pref-height: 40px"),
                () -> button.setStyle("-fx-background-color: #414654;-fx-text-fill: #FFFFFF;-fx-font-size: 14px;-fx-pref-width: 100px;-fx-pref-height: 40px")
        ));
        return button;
    }

    private void disableAll(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(true);
        }
    }

    private void enableAll(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(false);
        }
    }

    private void updateOperateStat(Label status, Button start, Button stop, Button restart) {
        if (MysqlOperator.hasPid(iniProp)) {
            status.setText(NapTheme.I18n.START_SUCCESS.get());
            enableAll(stop, restart);
            return;
        }
        status.setText(NapTheme.I18n.START_FAILED.get());
        enableAll(start, restart);
    }

    private void updateToolStat(Button repair, Button imp, Button exp) {
        File file = new File("output.sql");
        if (file.exists()) {
            enableAll(repair, imp, exp);
            return;
        }
        enableAll(repair, exp);
    }
}
