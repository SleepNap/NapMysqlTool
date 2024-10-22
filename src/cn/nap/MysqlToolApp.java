package cn.nap;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MysqlToolApp extends Application {
    private final AtomicBoolean changed = new AtomicBoolean();
    private Button change;
    private Button start;
    private Button stop;
    private Button restart;
    private Label status;
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
        File initDir = new File(toolConf.get("初始化脚本路径"));
        // 创建init文件夹
        if (!initDir.exists()) {
            boolean ignore = initDir.mkdirs();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Label version = new Label("1.24.1021");
        change = new Button("切换");

        Label tips = new Label("Tips: 关闭本程序不会影响MySQL的启停状态");
        start = new Button("启动");
        stop = new Button("停止");
        restart = new Button("重启");
        Label text = new Label("当前MySQL状态: ");
        status = new Label("初始化中...");

        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(20);
        gridPane.setPadding(new Insets(25, 25, 25, 25));

        gridPane.add(version, 0, 0);
        gridPane.add(change, 2, 0);
        gridPane.add(tips, 0, 1, 3, 1);
        gridPane.add(start, 0, 2);
        gridPane.add(stop, 1, 2);
        gridPane.add(restart, 2, 2);
        gridPane.add(text, 0, 3, 2, 1);
        gridPane.add(status, 2, 3);

        Scene scene = new Scene(gridPane, 400, 200);

        gridPane.setStyle("-fx-background-color: rgb(43, 43, 43)");

        change.setStyle("-fx-background-color: green;-fx-text-fill: white;-fx-min-width: 80;-fx-min-height: 20");
        change.setOnAction(event -> {
            disableAll();
            changed.set(!changed.get());
            if (changed.get()) {
                start.setText("修复");
                stop.setText("导出");
                restart.setText("导入");
            } else {
                start.setText("启动");
                stop.setText("停止");
                restart.setText("重启");
            }
            updateStat();
        });

        double width = Math.ceil((400 - 20 - 50) / 3D);
        double height = 40;
        version.setStyle("-fx-text-fill: rgb(199, 199, 209)");
        start.setMinSize(width, height);
        start.setStyle("-fx-background-color: rgb(199, 199, 209);-fx-font-size: 14");
        stop.setMinSize(width, height);
        stop.setStyle("-fx-background-color: rgb(199, 199, 209);-fx-font-size: 14");
        restart.setMinSize(width, height);
        restart.setStyle("-fx-background-color: rgb(199, 199, 209);-fx-font-size: 14");
        text.setStyle("-fx-text-fill: rgb(199, 199, 209)");
        status.setTextFill(Color.GREEN);
        status.setStyle("-fx-font-size: 16");
        tips.setTextFill(Color.FIREBRICK);

        start.setOnAction(event -> {
            disableAll();
            if (changed.get()) {
                new Thread(() -> {
                    PlatformImpl.runAndWait(() -> status.setText("停止中..."));
                    stopMysql(true);
                    PlatformImpl.runAndWait(() -> {
                        disableAll();
                        status.setText("修复中...");
                    });
                    fixMysql();
                }).start();
                return;
            }
            status.setText("启动中...");
            new Thread(() -> startMysql(true)).start();
        });
        stop.setOnAction(event -> {
            disableAll();
            if (changed.get()) {
                new Thread(() -> {
                    PlatformImpl.runAndWait(() -> status.setText("启动中..."));
                    startMysql(true);
                    PlatformImpl.runAndWait(() -> {
                        disableAll();
                        status.setText("导出中...");
                    });
                    exportMysql();
                }).start();
                return;
            }
            status.setText("停止中...");
            new Thread(() -> stopMysql(true)).start();
        });
        restart.setOnAction(event -> {
            disableAll();
            if (changed.get()) {
                new Thread(() -> {
                    PlatformImpl.runAndWait(() -> status.setText("启动中..."));
                    startMysql(true);
                    PlatformImpl.runAndWait(() -> {
                        disableAll();
                        status.setText("导入中...");
                    });
                    importMysql();
                }).start();
                return;
            }
            status.setText("重启中...");
            new Thread(this::restartMysql).start();
        });
        disableAll();
        updateStat();

        primaryStage.setScene(scene);
        primaryStage.setTitle("MySQL启停工具 by Nap");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void startMysql(boolean update) {
        for (int i = 0; i < 3; i++) {
            try {
                if (MysqlOperator.hasPid(iniProp)) {
                    initSqlScript();
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
        if (update) {
            Platform.runLater(this::updateStat);
        }
    }

    private void stopMysql(boolean update) {
        for (int i = 0; i < 3; i++) {
            try {
                if (!MysqlOperator.hasPid(iniProp)) {
                    break;
                }
                MysqlOperator.stop(iniProp).waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (update) {
            Platform.runLater(this::updateStat);
        }
    }

    private void restartMysql() {
        stopMysql(false);
        startMysql(true);
    }

    private void fixMysql() {
        if (MysqlOperator.hasPid(iniProp) && MysqlOperator.isStarted(iniProp)) {
            Platform.runLater(() -> {
                updateStat();
                status.setText("请先停止!");
            });
            return;
        }
        MysqlOperator.fix(iniProp);
        Platform.runLater(() -> {
            updateStat();
            status.setText("修复完成");
        });
    }

    private void exportMysql() {
        boolean started = MysqlOperator.hasPid(iniProp);
        if (!started) {
            Platform.runLater(() -> {
                updateStat();
                status.setText("请先启动!");
            });
            return;
        }
        try {
            MysqlOperator.outDb(iniProp).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            updateStat();
            status.setText("导出完成");
        });
    }

    private void importMysql() {
        boolean started = MysqlOperator.hasPid(iniProp);
        if (!started) {
            Platform.runLater(() -> {
                updateStat();
                status.setText("请先启动!");
            });
            return;
        }
        File file = new File("output.sql");
        if (!file.exists()) {
            Platform.runLater(() -> {
                updateStat();
                status.setText("请先导出!");
            });
            return;
        }
        try {
            MysqlOperator.initSql(iniProp, "", file.getAbsolutePath()).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            updateStat();
            status.setText("导入完成");
        });
    }

    private void disableAll() {
        change.setDisable(true);
        start.setDisable(true);
        stop.setDisable(true);
        restart.setDisable(true);
    }

    private void updateStat() {
        if (changed.get()) {
            start.setDisable(false);
            stop.setDisable(false);
            restart.setDisable(false);
            change.setDisable(false);
            return;
        }
        new Thread(() -> {
            boolean started = MysqlOperator.hasPid(iniProp);
            PlatformImpl.runAndWait(() -> {
                if (started) {
                    status.setText("已启动");
                    start.setDisable(true);
                    stop.setDisable(false);
                    restart.setDisable(false);
                } else {
                    status.setText("未启动");
                    start.setDisable(false);
                    stop.setDisable(true);
                    restart.setDisable(false);
                }
                Runtime.getRuntime().gc();
                change.setDisable(false);
            });
        }).start();
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

        iniProp.put("工具配置", toolConf);
        MysqlUtils.writeIniFile(iniProp, "config.ini");
    }

    private void initSqlScript() throws Exception {
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
}
