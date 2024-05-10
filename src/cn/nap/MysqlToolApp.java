package cn.nap;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MysqlToolApp extends Application {
    private Button start;
    private Button stop;
    private Button restart;
    private Label status;
    private final Map<String, Map<String, String>> iniProp = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        File currPath = new File(".");
        File[] files = currPath.listFiles();
        if (files == null) {
            initDefault();
            return;
        }
        for (File file : files) {
            if (!file.getName().equals("config.ini")) {
                continue;
            }
            Map<String, Map<String, String>> readedMap = MysqlUtils.readIniFile(file.getAbsolutePath());
            if (readedMap != null) {
                iniProp.putAll(readedMap);
                return;
            }
            break;
        }
        initDefault();
    }

    @Override
    public void start(Stage primaryStage) {
        Label version = new Label("1.24.0510");
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

        int columnIndex = 0;
        int rowIndex = 0;
        gridPane.add(version, columnIndex, rowIndex, 2, 1);
        rowIndex++;
        gridPane.add(tips, columnIndex, rowIndex, 3, 1);
        rowIndex++;
        gridPane.add(start, columnIndex, rowIndex);
        gridPane.add(stop, columnIndex + 1, rowIndex);
        gridPane.add(restart, columnIndex + 2, rowIndex);
        rowIndex++;
        gridPane.add(text, columnIndex, rowIndex, 2, 1);
        gridPane.add(status, columnIndex + 2, rowIndex);

        Scene scene = new Scene(gridPane, 400, 200);

        gridPane.setStyle("-fx-background-color: rgb(43, 43, 43)");
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
            status.setText("启动中...");
            new Thread(this::startMysql).start();
        });
        stop.setOnAction(event -> {
            disableAll();
            status.setText("停止中...");
            new Thread(this::stopMysql).start();
        });
        restart.setOnAction(event -> {
            disableAll();
            status.setText("重启中...");
            new Thread(this::restartMysql).start();
        });
        updateStat();

        primaryStage.setScene(scene);
        primaryStage.setTitle("MySQL启停工具 by Nap");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void startMysql() {
        for (int i = 0; i < 3; i++) {
            try {
                if (MysqlOperator.isStarted(iniProp)) {
                    break;
                }
                MysqlOperator.start(iniProp).waitFor(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Platform.runLater(this::updateStat);
    }

    private void stopMysql() {
        for (int i = 0; i < 3; i++) {
            try {
                if (!MysqlOperator.isStarted(iniProp)) {
                    break;
                }
                MysqlOperator.stop(iniProp).waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(this::updateStat);
        }
    }

    private void restartMysql() {
        try {
            for (int i = 0; i < 3; i++) {
                if (!MysqlOperator.isStarted(iniProp)) {
                    break;
                }
                MysqlOperator.stop(iniProp).waitFor();
            }
            for (int i = 0; i < 3; i++) {
                if (MysqlOperator.isStarted(iniProp)) {
                    break;
                }
                MysqlOperator.start(iniProp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.runLater(this::updateStat);
    }

    private void disableAll() {
        start.setDisable(true);
        stop.setDisable(true);
        restart.setDisable(true);
    }

    private void updateStat() {
        if (MysqlOperator.isStarted(iniProp)) {
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
    }

    private void initDefault() {
        Map<String, String> mysqlConf = new HashMap<>();
        mysqlConf.put("mysql路径", "mysql-5.7.44-winx64");
        mysqlConf.put("mysql账号", "root");
        mysqlConf.put("mysql密码", "root");
        mysqlConf.put("mysql.ini路径", "");
        iniProp.put("mysql配置", mysqlConf);

//        Map<String, String> toolConf = new HashMap<>();
//        iniProp.put("工具配置", toolConf);
        MysqlUtils.writeIniFile(iniProp, "config.ini");
    }
}
