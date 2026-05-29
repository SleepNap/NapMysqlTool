package cn.nap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final ToolService toolService = ToolService.getInstance();
    private Thread initThread;
    private Stage primaryStage;
    private StackPane root;
    private BorderPane body;
    private VBox instanceBox;
    private final Map<ToolConfig.Instance, VBox> cards = new HashMap<>();

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
        Scene scene = new Scene(root, ToolCommon.MAX_STAGE_WIDTH, ToolCommon.MAX_STAGE_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle(I18n.TOOL_TITLE.translate(toolService.getLanguage()));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        toolService.saveConfig();
        super.stop();
    }

    private void loadRoot() {
        if (toolService.isWin11()) {
            ToolUtil.setWindowFrameColor(primaryStage, ToolUtil.toRGBInt(Color.web(ThemeColor.WINDOW_BG.color())));
        } else {
            ToolUtil.setWindowDarkMode(toolService.isWin11(), primaryStage, toolService.isDark());
        }
        root.setStyle(String.format("-fx-background-color: %s", ThemeColor.ROOT_BG.color()));
        loadBody();
    }

    private void loadBody() {
        body = new BorderPane();
        body.setStyle(String.format("-fx-background-color: %s", ThemeColor.ROOT_BG.color()));

        loadTop();
        loadBottom();
        root.getChildren().add(body);
    }

    private void loadTop() {
        ToggleGroup topGroup = new ToggleGroup();
        ToggleButton operate = ToolComponent.menu(topGroup, I18n.TAB_OPERATE.translate(toolService.getLanguage()));
        ToggleButton other = ToolComponent.menu(topGroup, I18n.TAB_EXT.translate(toolService.getLanguage()));
        ToggleButton ini = ToolComponent.menu(topGroup, I18n.TAB_INSTANCE.translate(toolService.getLanguage()));

        Region spacer = new Region();

        Button create = ToolComponent.icon(ToolCommon.SVG_ADD);
        Button theme = ToolComponent.themeIcon();

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
        top.setStyle("-fx-spacing: 10px;-fx-alignment: center;-fx-padding: 0 15 0 15;");

        VBox topBox = new VBox(top, ToolComponent.line());
        topBox.setStyle("-fx-spacing: 10px;-fx-padding: 10 0 10 0;");

        body.setTop(topBox);
    }

    private void loadOperateMenu() {
        Button startAll = ToolComponent.startButton(I18n.START_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_START);
        startAll.setPadding(new Insets(12, 0, 12, 0));
        Button stopAll = ToolComponent.stopButton(I18n.STOP_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_STOP);
        stopAll.setPadding(new Insets(12, 0, 12, 0));
        Button restartAll = ToolComponent.button(I18n.RESTART_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_RESTART);
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


        instanceBox = new VBox();
        instanceBox.setStyle("-fx-spacing: 10px");
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances != null && !instances.isEmpty()) {
            for (ToolConfig.Instance instance : instances) {
                Button start = ToolComponent.startButton(I18n.START.translate(toolService.getLanguage()), ToolCommon.SVG_START);
                Button stop = ToolComponent.stopButton(I18n.STOP.translate(toolService.getLanguage()), ToolCommon.SVG_STOP);
                Button restart = ToolComponent.button(I18n.RESTART.translate(toolService.getLanguage()), ToolCommon.SVG_RESTART);
                Button[] operateSingle = {start, stop, restart};
                List<Node> buttons = Arrays.asList(operateSingle);
                VBox card = createInstance(instance, buttons);
                cards.put(instance, card);
                instanceBox.getChildren().add(card);

                start.setOnAction(event -> startInstance(instance, operateAll, operateSingle));
                stop.setOnAction(event -> stopInstance(instance, operateAll, operateSingle));
                restart.setOnAction(event -> restartInstance(instance, operateAll, operateSingle));
            }
        }

        ScrollPane instanceScroll = ToolComponent.scrollPane();
        instanceScroll.setFitToWidth(true);
        instanceScroll.setContent(instanceBox);

        startAll.setOnAction(event -> startAllInstance(operateAll));
        stopAll.setOnAction(event -> stopAllInstance(operateAll));
        restartAll.setOnAction(event -> restartAllInstance(operateAll));

        refreshOperateButton(operateAll);
        VBox center = new VBox(operateMenus, instanceScroll);
        VBox.setVgrow(instanceScroll, Priority.ALWAYS);
        center.setStyle("-fx-spacing: 10px;-fx-alignment: top_center;-fx-padding: 0 15 0 15;");
        body.setCenter(center);
    }

    private void loadBottom() {
        Label version = ToolComponent.label(ToolCommon.VERSION);
        Label language = ToolComponent.label(toolService.getLanguage());
        Region spacer = new Region();
        HBox bottom = new HBox(version, spacer, language);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottom.setStyle("-fx-spacing: 10px;-fx-alignment: center;-fx-padding: 0 15 0 15;");

        VBox bottomBox = new VBox(ToolComponent.line(), bottom);
        bottomBox.setStyle("-fx-spacing: 5px;-fx-padding: 10 0 5 0;");

        language.setOnMouseClicked(event -> {
            toolService.changeLanguage();
            loadBody();
        });

        body.setBottom(bottomBox);
    }

    private VBox createInstance(ToolConfig.Instance instance, List<Node> buttons) {
        Label portLabel = ToolComponent.label(instance.port.data);
        Status status = Status.fromType(instance.status);
        HBox statusWithDot = ToolComponent.statusWithDot(status.i18n().translate(toolService.getLanguage()), status.color().color());
        return ToolComponent.instanceInfo(instance.section.data, Arrays.asList(portLabel, statusWithDot), buttons);
    }

    private void refreshOperateButton(Button[] operateAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        for (ToolConfig.Instance instance : instances) {
            refreshCard(instance);
        }
        refreshAllButtons(operateAll);
    }

    private void refreshCard(ToolConfig.Instance instance) {
        VBox oldCard = cards.get(instance);
        if (oldCard == null) return;
        HBox bottomRow = (HBox) oldCard.getChildren().get(1);
        List<Node> buttons = new ArrayList<>(bottomRow.getChildren());
        VBox newCard = createInstance(instance, buttons);
        int index = instanceBox.getChildren().indexOf(oldCard);
        if (index >= 0) {
            instanceBox.getChildren().set(index, newCard);
        }
        cards.put(instance, newCard);

        boolean isStarted = Status.STARTED.type() == instance.status;
        boolean isStarting = Status.STARTING.type() == instance.status;
        boolean isStopping = Status.STOPPING.type() == instance.status;
        boolean inProgress = isStarting || isStopping;
        buttons.get(0).setDisable(isStarted || inProgress);
        buttons.get(1).setDisable(!isStarted || inProgress);
        buttons.get(2).setDisable(inProgress);
    }

    private void refreshAllButtons(Button[] operateAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        boolean allStarted = true;
        boolean allStopped = true;
        boolean allInProgress = true;
        for (ToolConfig.Instance inst : instances) {
            if (Status.STARTED.type() == inst.status) {
                allStopped = false;
                allInProgress = false;
            } else if (Status.STARTING.type() == inst.status || Status.STOPPING.type() == inst.status) {
                allStarted = false;
                allStopped = false;
            } else {
                allStarted = false;
                allInProgress = false;
            }
        }
        if (allInProgress) {
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

    private void refreshOperateButton(ToolConfig.Instance instance, Button[] operateAll) {
        refreshCard(instance);
        refreshAllButtons(operateAll);
    }

    private void disableOperateButton(Button[] operateAll, Button[] ...operateSingle) {
        for (Button button : operateAll) {
            button.setDisable(true);
        }
        for (Button[] btnArr : operateSingle) {
            for (Button btn : btnArr) {
                btn.setDisable(true);
            }
        }
    }

    private void startInstance(ToolConfig.Instance instance, Button[] operateAll, Button[] operateSingle) {
        if (!checkPath(instance)) {
            return;
        }
        if (toolService.isRunning(instance)) {
            instance.status = Status.STARTED.type();
        } else {
            instance.status = Status.STOPPED.type();
        }
        String translate = String.format(I18n.CONFIRM_BIND_START.translate(toolService.getLanguage()), instance.port.data);
        if (toolService.isPortOccupied(instance) && !ToolComponent.confirm(primaryStage, root, translate)) {
            return;
        }
        instance.status = Status.STARTING.type();
        refreshCard(instance);
        disableOperateButton(operateAll, operateSingle);
        new Thread(() -> {
            try {
                if (toolService.start(instance)) {
                    instance.status = Status.STARTED.type();
                } else {
                    instance.status = Status.STOPPED.type();
                }
            } catch (Exception e) {
                Platform.runLater(() -> ToolComponent.error(primaryStage, root, e.getMessage()));
                instance.status = Status.STOPPED.type();
            } finally {
                Platform.runLater(() -> refreshOperateButton(instance, operateAll));
            }
        }).start();

    }

    private void stopInstance(ToolConfig.Instance instance, Button[] operateAll, Button[] operateSingle) {
        if (!checkPath(instance)) {
            return;
        }
        if (!toolService.isRunning(instance)) {
            instance.status = Status.STOPPED.type();
            refreshOperateButton(instance, operateAll);
            return;
        }
        instance.status = Status.STOPPING.type();
        refreshCard(instance);
        disableOperateButton(operateAll, operateSingle);
        new Thread(() -> {
            try {
                toolService.stop(instance);
            } catch (Exception e) {
                Platform.runLater(() -> ToolComponent.error(primaryStage, root, e.getMessage()));
                instance.status = Status.STARTED.type();
            } finally {
                Platform.runLater(() -> refreshOperateButton(instance, operateAll));
            }
        }).start();
    }

    private void restartInstance(ToolConfig.Instance instance, Button[] operateAll, Button[] operateSingle) {
        if (!checkPath(instance)) {
            return;
        }
        boolean needStop = Status.STARTED.type() == instance.status;
        instance.status = needStop ? Status.STOPPING.type() : Status.STARTING.type();
        refreshCard(instance);
        disableOperateButton(operateAll, operateSingle);
        new Thread(() -> {
            int oldStatus = instance.status;
            try {
                if (needStop) {
                    toolService.stop(instance);
                    instance.status = Status.STARTING.type();
                    Platform.runLater(() -> refreshCard(instance));
                }
                if (toolService.start(instance)) {
                    instance.status = Status.STARTED.type();
                } else {
                    instance.status = Status.STOPPED.type();
                }
            } catch (Exception e) {
                Platform.runLater(() -> ToolComponent.error(primaryStage, root, e.getMessage()));
                instance.status = oldStatus;
            } finally {
                Platform.runLater(() -> refreshOperateButton(instance, operateAll));
            }
        }).start();
    }

    private void startAllInstance(Button[] operateAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        for (ToolConfig.Instance instance : instances) {
            if (!checkPath(instance)) continue;
            if (Status.STOPPED.type() == instance.status) {
                instance.status = Status.STARTING.type();
                refreshCard(instance);
            }
        }
        disableOperateButton(operateAll);
        new Thread(() -> {
            try {
                for (ToolConfig.Instance instance : instances) {
                    if (Status.STARTING.type() != instance.status) continue;
                    try {
                        if (toolService.start(instance)) {
                            instance.status = Status.STARTED.type();
                        } else {
                            instance.status = Status.STOPPED.type();
                        }
                    } catch (Exception e) {
                        instance.status = Status.STOPPED.type();
                    }
                }
            } finally {
                Platform.runLater(() -> {
                    for (ToolConfig.Instance inst : toolService.getConfig().instances) {
                        refreshCard(inst);
                    }
                    refreshAllButtons(operateAll);
                });
            }
        }).start();
    }

    private void stopAllInstance(Button[] operateAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        for (ToolConfig.Instance instance : instances) {
            if (!checkPath(instance)) continue;
            if (Status.STARTED.type() == instance.status) {
                instance.status = Status.STOPPING.type();
                refreshCard(instance);
            }
        }
        disableOperateButton(operateAll);
        new Thread(() -> {
            try {
                for (ToolConfig.Instance instance : instances) {
                    if (Status.STOPPING.type() != instance.status) continue;
                    toolService.stop(instance);
                }
            } finally {
                Platform.runLater(() -> {
                    for (ToolConfig.Instance inst : toolService.getConfig().instances) {
                        refreshCard(inst);
                    }
                    refreshAllButtons(operateAll);
                });
            }
        }).start();
    }

    private void restartAllInstance(Button[] operateAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        for (ToolConfig.Instance instance : instances) {
            if (!checkPath(instance)) continue;
            boolean needStop = Status.STARTED.type() == instance.status;
            instance.status = needStop ? Status.STOPPING.type() : Status.STARTING.type();
            refreshCard(instance);
        }
        disableOperateButton(operateAll);
        new Thread(() -> {
            try {
                for (ToolConfig.Instance instance : instances) {
                    int s = instance.status;
                    if (s != Status.STOPPING.type() && s != Status.STARTING.type()) continue;
                    int oldStatus = instance.status;
                    try {
                        if (Status.STOPPING.type() == s) {
                            toolService.stop(instance);
                            instance.status = Status.STARTING.type();
                            Platform.runLater(() -> refreshCard(instance));
                        }
                        if (toolService.start(instance)) {
                            instance.status = Status.STARTED.type();
                        } else {
                            instance.status = Status.STOPPED.type();
                        }
                    } catch (Exception e) {
                        instance.status = oldStatus;
                    }
                }
            } finally {
                Platform.runLater(() -> {
                    for (ToolConfig.Instance inst : toolService.getConfig().instances) {
                        refreshCard(inst);
                    }
                    refreshAllButtons(operateAll);
                });
            }
        }).start();
    }

    private boolean checkPath(ToolConfig.Instance instance) {
        Path dir = Paths.get(instance.path.data);
        if (!Files.exists(dir)) {
            String translate = String.format(I18n.MYSQL_PATH_ERROR.translate(toolService.getLanguage()), instance.port.data);
            ToolComponent.error(primaryStage, root, translate);
            return false;
        }
        return true;
    }
}
