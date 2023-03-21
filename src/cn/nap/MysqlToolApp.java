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
import javafx.stage.StageStyle;

public class MysqlToolApp extends Application {
    private final String DEFAULT_PATH = "mysql";
    private Button start;
    private Button stop;
    private Button restart;
    private Label status;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Label version = new Label("MySQL5.7.40");
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
            start.setDisable(true);
            status.setText("启动中...");
            startMysql();
        });
        stop.setOnAction(event -> {
            stop.setDisable(true);
            status.setText("停止中...");
            stopMysql();
        });
        restart.setOnAction(event -> {
            restart.setDisable(true);
            status.setText("重启中...");
            restartMysql();
        });
        updateStat();

        primaryStage.setScene(scene);
        primaryStage.setTitle("MySQL启停工具 by Nap");
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();
    }

    private void startMysql() {
        try {
            MysqlOperator.start(DEFAULT_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateStat();
    }

    private void stopMysql() {
        try {
            Process process = MysqlOperator.stop(DEFAULT_PATH);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateStat();
    }

    private void restartMysql() {
        stopMysql();
        startMysql();
    }

    private void updateStat() {
        Platform.runLater(() -> {
            String stat = MysqlOperator.isStarted() ? "已启动" : "未启动";
            status.setText(stat);
            if ("已启动".equals(stat)) {
                start.setDisable(true);
                stop.setDisable(false);
                restart.setDisable(false);
            } else {
                start.setDisable(false);
                stop.setDisable(true);
                restart.setDisable(false);
            }
        });
    }
}