package cn.nap;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.ThemeColor;
import static cn.nap.ToolCommon.Status;

public class ToolApp extends Application {
    public static void main(String[] args) {
        System.setProperty("prism.allowhidpi", "false");
        System.setProperty("prism.lcdtext", "false");
        launch(args);
    }

    private final ToolService toolService = new ToolService();
    private Thread initThread;
    private Stage primaryStage;
    private StackPane root;
    private BorderPane body;

    @Override
    public void init() throws Exception {
        initThread = new Thread(toolService::loadConfig);
        initThread.start();
        super.init();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initThread.join(10000);
        this.primaryStage = primaryStage;
        root = new StackPane();
        loadRoot();
        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle(i18n(I18n.TOOL_TITLE));
        primaryStage.show();
    }

    private void loadRoot() {
        if (toolService.isWin11()) {
            ToolUtil.setWindowFrameColor(primaryStage, ToolUtil.toRGBInt(Color.web(ThemeColor.WINDOW_BG.color(toolService.isDark()))));
        } else {
            ToolUtil.setWindowDarkMode(toolService.isWin11(), primaryStage, toolService.isDark());
        }
        root.setStyle(String.format("-fx-background-color: %s", ThemeColor.ROOT_BG.color(toolService.isDark())));
        loadBody();
    }

    private void loadBody() {
        body = new BorderPane();
        body.setStyle(String.format("-fx-background-color: %s", ThemeColor.ROOT_BG.color(toolService.isDark())));

        loadTop();
        loadBottom();
        root.getChildren().add(body);
    }

    private void loadTop() {
        ToggleGroup topGroup = new ToggleGroup();
        ToggleButton operate = ToolComponent.menu(topGroup, i18n(I18n.TAB_OPERATE), toolService.isDark());
        ToggleButton other = ToolComponent.menu(topGroup, i18n(I18n.TAB_EXT), toolService.isDark());
        ToggleButton ini = ToolComponent.menu(topGroup, i18n(I18n.TAB_INI), toolService.isDark());

        Region spacer = new Region();

        Button create = ToolComponent.icon(ToolCommon.SVG_ADD, toolService.isDark());
        Button theme = ToolComponent.themeIcon(toolService.isDark());

        topGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                topGroup.selectToggle(oldVal);
            }
            if (Objects.equals(newVal, other)) {

            } else if (Objects.equals(newVal, ini)) {

            } else {
                loadOperateMenu();
            }
        });

        theme.setOnAction(event -> {
            toolService.changeTheme();
            loadRoot();
        });

        topGroup.selectToggle(operate);
        HBox top = new HBox(operate, other, ini, spacer, create, theme);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.setStyle("-fx-spacing: 10px;-fx-alignment: center;");

        VBox topBox = new VBox(top, ToolComponent.line(toolService.isDark()));
        topBox.setStyle("-fx-spacing: 10px;-fx-padding: 12 15 8 15;");

        body.setTop(topBox);
    }

    private void loadOperateMenu() {
        Button startAll = ToolComponent.startButton(i18n(I18n.START_ALL), ToolCommon.SVG_START, toolService.isDark());
        startAll.setPadding(new Insets(12, 0, 12, 0));
        Button stopAll = ToolComponent.stopButton(i18n(I18n.STOP_ALL), ToolCommon.SVG_STOP, toolService.isDark());
        stopAll.setPadding(new Insets(12, 0, 12, 0));
        Button restartAll = ToolComponent.button(i18n(I18n.RESTART_ALL), ToolCommon.SVG_RESTART, toolService.isDark());
        restartAll.setPadding(new Insets(12, 0, 12, 0));
        Button[] operateAll = {startAll, stopAll, restartAll};

        HBox operateMenus = new HBox(operateAll);
        HBox.setHgrow(startAll, Priority.ALWAYS);
        HBox.setHgrow(stopAll, Priority.ALWAYS);
        HBox.setHgrow(restartAll, Priority.ALWAYS);
        startAll.setMaxWidth(Double.MAX_VALUE);
        stopAll.setMaxWidth(Double.MAX_VALUE);
        restartAll.setMaxWidth(Double.MAX_VALUE);
        operateMenus.setStyle("-fx-spacing: 10px;-fx-alignment: center;");


        VBox instanceBox = new VBox();
        instanceBox.setStyle("-fx-spacing: 10px");
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        List<Button[]> operateSingleList = new ArrayList<>();
        if (instances != null && !instances.isEmpty()) {
            for (ToolConfig.Instance instance : instances) {
                Button start = ToolComponent.startButton(i18n(I18n.START), ToolCommon.SVG_START, toolService.isDark());
                Button stop = ToolComponent.stopButton(i18n(I18n.STOP), ToolCommon.SVG_STOP, toolService.isDark());
                Button restart = ToolComponent.button(i18n(I18n.RESTART), ToolCommon.SVG_RESTART, toolService.isDark());
                Button[] operateSingle = {start, stop, restart};
                operateSingleList.add(operateSingle);
                List<Node> buttons = Arrays.asList(operateSingle);
                instanceBox.getChildren().add(createInstance(instance, buttons));
            }
        }

        ScrollPane instanceScroll = ToolComponent.scrollPane(toolService.isDark());
        instanceScroll.setFitToWidth(true);
        instanceScroll.setContent(instanceBox);

        refreshOperateButton(operateAll, operateSingleList);
        VBox center = new VBox(operateMenus, instanceScroll);
        VBox.setVgrow(instanceScroll, Priority.ALWAYS);
        center.setStyle("-fx-spacing: 10px;-fx-alignment: top_center;-fx-padding: 0 15 0 15;");
        body.setCenter(center);
    }

    private void loadBottom() {
        Label version = ToolComponent.label(ToolCommon.VERSION, toolService.isDark());
        Region spacer = new Region();
        HBox bottom = new HBox(version, spacer);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottom.setStyle("-fx-spacing: 10px;-fx-alignment: center;");

        VBox bottomBox = new VBox(ToolComponent.line(toolService.isDark()), bottom);
        bottomBox.setStyle("-fx-spacing: 10px;-fx-padding: 8 15 12 15;");

        body.setBottom(bottomBox);
    }

    private VBox createInstance(ToolConfig.Instance instance, List<Node> buttons) {
        Label portLabel = ToolComponent.label(instance.port.data, toolService.isDark());
        Status status = Status.fromType(instance.status);
        Label statusLabel = ToolComponent.label(i18n(status.i18n()), toolService.isDark());
        statusLabel.setStyle(String.format("-fx-text-fill: %s;", status.color().color(toolService.isDark())));
        return ToolComponent.instanceInfo(instance.section.data, Arrays.asList(portLabel, statusLabel), buttons, toolService.isDark());
    }

    private void refreshOperateButton(Button[] operateAll, List<Button[]> operateSingleList) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        boolean allStarted = true;
        boolean allStopped = true;
        boolean allStarting = true;
        for (int i = 0; i < instances.size(); i++) {
            ToolConfig.Instance instance = instances.get(i);
            Button[] operateSingle = operateSingleList.get(i);
            if (Status.STARTED.type() == instance.status) {
                allStopped = false;
                allStarting = false;
                operateSingle[0].setDisable(true);
                operateSingle[1].setDisable(false);
                operateSingle[2].setDisable(false);
            } else if (Status.STARTING.type() == instance.status) {
                allStarted = false;
                allStopped = false;
                operateSingle[0].setDisable(true);
                operateSingle[1].setDisable(true);
                operateSingle[2].setDisable(true);
            } else {
                allStarted = false;
                allStarting = false;
                operateSingle[0].setDisable(false);
                operateSingle[1].setDisable(true);
                operateSingle[2].setDisable(false);
            }
        }
        if (allStarting) {
            operateAll[0].setDisable(true);
            operateAll[1].setDisable(true);
            operateAll[2].setDisable(true);
        } else if (allStarted) {
            operateAll[0].setDisable(true);
            operateAll[1].setDisable(false);
            operateAll[2].setDisable(false);
        } else if (allStopped) {
            operateAll[0].setDisable(false);
            operateAll[1].setDisable(true);
            operateAll[2].setDisable(false);
        } else {
            operateAll[0].setDisable(false);
            operateAll[1].setDisable(false);
            operateAll[2].setDisable(false);
        }
    }

    private String i18n(I18n i18n) {
        return ToolCommon.Language.EN_US.type().equals(toolService.getConfig().core.language.data) ? i18n.EN() : i18n.ZH();
    }
}
