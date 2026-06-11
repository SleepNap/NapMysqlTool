package cn.nap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.ThemeColor;
import static cn.nap.ToolCommon.Status;
import static cn.nap.ToolCommon.TipType;

public class ToolApp extends Application {
    public static void main(String[] args) {
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
    private ToggleGroup menuGroup;

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
        primaryStage.setResizable(false);
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
        menuGroup = new ToggleGroup();
        ToggleButton operate = ToolComponent.menu(menuGroup, I18n.TAB_OPERATE.translate(toolService.getLanguage()));
        ToggleButton other = ToolComponent.menu(menuGroup, I18n.TAB_EXT.translate(toolService.getLanguage()));
        ToggleButton instance = ToolComponent.menu(menuGroup, I18n.TAB_INSTANCE.translate(toolService.getLanguage()));

        Region spacer = new Region();

        Button create = ToolComponent.icon(ToolCommon.SVG_ADD);
        create.setOnAction(event -> showInstanceForm(null));
        Button theme = ToolComponent.themeIcon();

        menuGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                menuGroup.selectToggle(oldVal);
            }
            int index = menuGroup.getToggles().indexOf(newVal);
            loadMenuWithIndex(index);
        });

        theme.setOnAction(event -> {
            toolService.changeTheme();
            loadRoot();
        });

        menuGroup.selectToggle(operate);
        HBox top = new HBox(operate, other, instance, spacer, create, theme);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.setStyle("-fx-spacing: 10px;-fx-alignment: center;-fx-padding: 0 15 0 15;");

        VBox topBox = new VBox(top, ToolComponent.line());
        topBox.setStyle("-fx-spacing: 10px;-fx-padding: 10 0 10 0;");

        body.setTop(topBox);
    }

    private void loadOperateMenu() {
        Button startAll = ToolComponent.startButton(I18n.START_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_START);
        Button stopAll = ToolComponent.stopButton(I18n.STOP_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_STOP);
        Button restartAll = ToolComponent.button(I18n.RESTART_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_RESTART);
        Button[] operateAll = {startAll, stopAll, restartAll};

        instanceBox = buildInstanceBox((instance, allBtns) -> {
            Button start = ToolComponent.startButton(I18n.START.translate(toolService.getLanguage()), ToolCommon.SVG_START);
            Button stop = ToolComponent.stopButton(I18n.STOP.translate(toolService.getLanguage()), ToolCommon.SVG_STOP);
            Button restart = ToolComponent.button(I18n.RESTART.translate(toolService.getLanguage()), ToolCommon.SVG_RESTART);
            Button[] single = {start, stop, restart};
            start.setOnAction(e -> startInstance(instance, operateAll, single));
            stop.setOnAction(e -> stopInstance(instance, operateAll, single));
            restart.setOnAction(e -> restartInstance(instance, operateAll, single));
            return single;
        });

        startAll.setOnAction(event -> startAllInstance(operateAll));
        stopAll.setOnAction(event -> stopAllInstance(operateAll));
        restartAll.setOnAction(event -> restartAllInstance(operateAll));

        refreshOperateButton(operateAll);
        setMenuCenter(operateAll, instanceBox);
    }

    private void loadExtMenu() {
        Button repairAll = ToolComponent.button(I18n.REPAIR_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_REPAIR);
        Button importAll = ToolComponent.button(I18n.IMPORT_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_IMPORT);
        Button exportAll = ToolComponent.button(I18n.EXPORT_ALL.translate(toolService.getLanguage()), ToolCommon.SVG_EXPORT);
        Button[] extAll = {repairAll, importAll, exportAll};

        instanceBox = buildInstanceBox((instance, allBtns) -> {
            Button repair = ToolComponent.button(I18n.REPAIR.translate(toolService.getLanguage()), ToolCommon.SVG_REPAIR);
            Button imp = ToolComponent.button(I18n.IMPORT.translate(toolService.getLanguage()), ToolCommon.SVG_IMPORT);
            Button exp = ToolComponent.button(I18n.EXPORT.translate(toolService.getLanguage()), ToolCommon.SVG_EXPORT);
            Button[] single = {repair, imp, exp};
            repair.setOnAction(e -> repairSingle(instance, extAll, single));
            imp.setOnAction(e -> importSingle(instance, extAll, single));
            exp.setOnAction(e -> exportSingle(instance, extAll, single));
            return single;
        });

        repairAll.setOnAction(event -> repairAllInstance(extAll));
        importAll.setOnAction(event -> importAllInstance(extAll));
        exportAll.setOnAction(event -> exportAllInstance(extAll));

        refreshExtButtons(extAll);
        setMenuCenter(extAll, instanceBox);
    }

    private interface SingleBuilder {
        Button[] build(ToolConfig.Instance instance, Button[] allButtons);
    }

    private VBox buildInstanceBox(SingleBuilder builder) {
        VBox box = new VBox();
        box.setStyle("-fx-spacing: 10px");
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances != null && !instances.isEmpty()) {
            for (ToolConfig.Instance instance : instances) {
                Button[] single = builder.build(instance, null);
                VBox card = createInstance(instance, Arrays.asList(single));
                cards.put(instance, card);
                box.getChildren().add(card);
            }
        }
        return box;
    }

    private void setMenuCenter(Button[] allButtons, VBox instanceBox, Node... topNodes) {
        HBox allRow = new HBox(allButtons);
        for (Button btn : allButtons) {
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPadding(new Insets(12, 0, 12, 0));
        }
        allRow.setStyle("-fx-spacing: 10px;-fx-alignment: center;");

        ScrollPane scroll = ToolComponent.scrollPane();
        scroll.setContent(instanceBox);

        VBox center = new VBox();
        center.getChildren().addAll(topNodes);
        center.getChildren().addAll(allRow, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
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

    // ==================== 扩展操作 ====================

    private void repairSingle(ToolConfig.Instance instance, Button[] extAll, Button[] extSingle) {
        if (!checkPath(instance)) return;
        disableOperateButton(extAll, extSingle);

        String pre = instance.port.data + ":";
        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        steps.add(new ToolComponent.RepairStep(
                I18n.REPAIR_STEP_VC.translate(toolService.getLanguage()),
                ToolUtil::checkVcRuntime));
        steps.add(new ToolComponent.RepairStep(
                pre + I18n.REPAIR_STEP_STOP.translate(toolService.getLanguage()),
                () -> { if (toolService.isRunning(instance)) toolService.stop(instance); }));
        steps.add(new ToolComponent.RepairStep(
                I18n.REPAIR_STEP_ASCII.translate(toolService.getLanguage()),
                () -> ToolUtil.checkAsciiPath(instance.path.data)));
        steps.add(new ToolComponent.RepairStep(
                pre + I18n.REPAIR_STEP_BINLOG.translate(toolService.getLanguage()),
                () -> toolService.clearBinlog(instance)));
        steps.add(new ToolComponent.RepairStep(
                pre + I18n.REPAIR_STEP_PID.translate(toolService.getLanguage()),
                () -> toolService.clearPid(instance)));
        steps.add(new ToolComponent.RepairStep(
                pre + I18n.REPAIR_STEP_PORT.translate(toolService.getLanguage()),
                () -> ToolUtil.killPortProcess(Integer.parseInt(instance.port.data))));

        ToolComponent.repairProgress(primaryStage, root, steps, () -> {
            instance.status = Status.STOPPED.type();
            refreshExtButton(instance, extAll);
        });
    }

    private void importSingle(ToolConfig.Instance instance, Button[] extAll, Button[] extSingle) {
        if (!checkPath(instance)) return;
        if (instance.database.data == null || instance.database.data.trim().isEmpty()) {
            ToolComponent.error(primaryStage, root, String.format(I18n.DATABASE_EMPTY.translate(toolService.getLanguage()), instance.port.data));
            return;
        }
        File sqlFile = findImportFile(instance);
        if (sqlFile == null) {
            ToolComponent.error(primaryStage, root, I18n.IMPORT_ERR2.translate(toolService.getLanguage()));
            return;
        }
        disableOperateButton(extAll, extSingle);
        boolean needStart = !toolService.isRunning(instance);
        String desc = String.format("%s:%s/%s", I18n.IMPORT.translate(toolService.getLanguage()), instance.port.data, instance.database.data);

        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        steps.add(new ToolComponent.RepairStep(desc,
                () -> {
                    if (needStart && !toolService.start(instance)) {
                        throw new Exception(I18n.IMPORT_ERR1.translate(toolService.getLanguage()));
                    }
                    toolService.importDb(instance, sqlFile.getAbsolutePath());
                }));

        ToolComponent.repairProgress(primaryStage, root, steps, () -> {
            instance.status = Status.STARTED.type();
            refreshExtButton(instance, extAll);
        });
    }

    private void exportSingle(ToolConfig.Instance instance, Button[] extAll, Button[] extSingle) {
        if (!checkPath(instance)) return;
        if (instance.database.data == null || instance.database.data.trim().isEmpty()) {
            ToolComponent.error(primaryStage, root, String.format(I18n.DATABASE_EMPTY.translate(toolService.getLanguage()), instance.port.data));
            return;
        }
        disableOperateButton(extAll, extSingle);
        boolean needStart = !toolService.isRunning(instance);
        String desc = String.format("%s:%s/%s", I18n.EXPORT.translate(toolService.getLanguage()), instance.port.data, instance.database.data);

        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        steps.add(new ToolComponent.RepairStep(desc,
                () -> {
                    if (needStart && !toolService.start(instance)) {
                        throw new Exception(I18n.EXPORT_ERR1.translate(toolService.getLanguage()));
                    }
                    toolService.exportDb(instance);
                }));

        ToolComponent.repairProgress(primaryStage, root, steps, () -> {
            instance.status = Status.STARTED.type();
            refreshExtButton(instance, extAll);
        });
    }

    private void repairAllInstance(Button[] extAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        disableOperateButton(extAll);
        disableExtCards();

        List<ToolConfig.Instance> validInstances = new ArrayList<>();
        for (ToolConfig.Instance instance : instances) {
            if (checkPathSilent(instance)) validInstances.add(instance);
        }
        if (validInstances.isEmpty()) return;

        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        steps.add(new ToolComponent.RepairStep(
                I18n.REPAIR_STEP_VC.translate(toolService.getLanguage()),
                ToolUtil::checkVcRuntime));
        steps.add(new ToolComponent.RepairStep(
                I18n.REPAIR_STEP_STOP.translate(toolService.getLanguage()),
                ToolUtil::killAllMysql));
        for (ToolConfig.Instance inst : validInstances) {
            String pre = inst.port.data + ":";
            steps.add(new ToolComponent.RepairStep(
                    pre + I18n.REPAIR_STEP_ASCII.translate(toolService.getLanguage()),
                    () -> ToolUtil.checkAsciiPath(inst.path.data)));
            steps.add(new ToolComponent.RepairStep(
                    pre + I18n.REPAIR_STEP_BINLOG.translate(toolService.getLanguage()),
                    () -> toolService.clearBinlog(inst)));
            steps.add(new ToolComponent.RepairStep(
                    pre + I18n.REPAIR_STEP_PID.translate(toolService.getLanguage()),
                    () -> toolService.clearPid(inst)));
            steps.add(new ToolComponent.RepairStep(
                    pre + I18n.REPAIR_STEP_PORT.translate(toolService.getLanguage()),
                    () -> ToolUtil.killPortProcess(Integer.parseInt(inst.port.data))));
        }

        ToolComponent.repairProgress(primaryStage, root, steps, () -> {
            for (ToolConfig.Instance inst : validInstances) inst.status = Status.STOPPED.type();
            Platform.runLater(() -> refreshExtButtons(extAll));
        });
    }

    private void importAllInstance(Button[] extAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        for (ToolConfig.Instance instance : instances) {
            if (!checkPathSilent(instance)) continue;
            if (Status.STOPPED.type() == instance.status) {
                instance.status = Status.STARTING.type();
                refreshCard(instance);
            }
        }
        disableOperateButton(extAll);
        disableExtCards();

        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        for (ToolConfig.Instance instance : instances) {
            if (!checkPathSilent(instance)) continue;
            if (instance.database.data == null || instance.database.data.trim().isEmpty()) {
                String desc = String.format("%s:%s/ -", I18n.IMPORT.translate(toolService.getLanguage()), instance.port.data);
                steps.add(new ToolComponent.RepairStep(desc, () -> { throw new Exception(); }));
                continue;
            }
            if (Status.STARTING.type() != instance.status && Status.STARTED.type() != instance.status) continue;
            String desc = String.format("%s:%s/%s", I18n.IMPORT.translate(toolService.getLanguage()), instance.port.data, instance.database.data);
            steps.add(new ToolComponent.RepairStep(desc, () -> {
                File sqlFile = findImportFile(instance);
                if (sqlFile == null) throw new Exception("file not found");
                if (Status.STARTING.type() == instance.status) {
                    if (!toolService.start(instance)) {
                        throw new Exception(I18n.IMPORT_ERR1.translate(toolService.getLanguage()));
                    }
                    instance.status = Status.STARTED.type();
                }
                toolService.importDb(instance, sqlFile.getAbsolutePath());
            }));
        }

        ToolComponent.repairProgress(primaryStage, root, steps, () -> refreshExtButtons(extAll));
    }

    private void exportAllInstance(Button[] extAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        for (ToolConfig.Instance instance : instances) {
            if (!checkPathSilent(instance)) continue;
            if (Status.STOPPED.type() == instance.status) {
                instance.status = Status.STARTING.type();
                refreshCard(instance);
            }
        }
        disableOperateButton(extAll);
        disableExtCards();

        List<ToolComponent.RepairStep> steps = new ArrayList<>();
        for (ToolConfig.Instance instance : instances) {
            if (instance.database.data == null || instance.database.data.trim().isEmpty()) {
                String desc = String.format("%s:%s/ -", I18n.EXPORT.translate(toolService.getLanguage()), instance.port.data);
                steps.add(new ToolComponent.RepairStep(desc, () -> { throw new Exception(); }));
                continue;
            }
            if (Status.STARTING.type() != instance.status && Status.STARTED.type() != instance.status) continue;
            String desc = String.format("%s:%s/%s", I18n.EXPORT.translate(toolService.getLanguage()), instance.port.data, instance.database.data);
            steps.add(new ToolComponent.RepairStep(desc, () -> {
                if (Status.STARTING.type() == instance.status) {
                    if (!toolService.start(instance)) {
                        throw new Exception(I18n.EXPORT_ERR1.translate(toolService.getLanguage()));
                    }
                    instance.status = Status.STARTED.type();
                }
                toolService.exportDb(instance);
            }));
        }

        ToolComponent.repairProgress(primaryStage, root, steps, () -> refreshExtButtons(extAll));
    }

    private void refreshExtButton(ToolConfig.Instance instance, Button[] extAll) {
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
        boolean inProgress = Status.STARTING.type() == instance.status || Status.STOPPING.type() == instance.status;
        buttons.get(0).setDisable(isStarted || inProgress);   // 修复：已启动或操作中禁用
        buttons.get(1).setDisable(!isStarted || inProgress);  // 导入：未启动或操作中禁用
        buttons.get(2).setDisable(!isStarted || inProgress);  // 导出：未启动或操作中禁用
        refreshExtAllButtons(extAll);
    }

    private void refreshExtButtons(Button[] extAll) {
        for (ToolConfig.Instance instance : toolService.getConfig().instances) {
            refreshCardExt(instance);
        }
        refreshExtAllButtons(extAll);
    }

    private void refreshCardExt(ToolConfig.Instance instance) {
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
        boolean inProgress = Status.STARTING.type() == instance.status || Status.STOPPING.type() == instance.status;
        buttons.get(0).setDisable(isStarted || inProgress);
        buttons.get(1).setDisable(!isStarted || inProgress);
        buttons.get(2).setDisable(!isStarted || inProgress);
    }

    private void disableExtCards() {
        for (VBox card : cards.values()) {
            HBox bottomRow = (HBox) card.getChildren().get(1);
            for (Node btn : bottomRow.getChildren()) {
                btn.setDisable(true);
            }
        }
    }

    private void refreshExtAllButtons(Button[] extAll) {
        List<ToolConfig.Instance> instances = toolService.getConfig().instances;
        if (instances == null || instances.isEmpty()) return;
        boolean allStarted = true;
        boolean allStopped = true;
        boolean anyInProgress = false;
        for (ToolConfig.Instance inst : instances) {
            if (Status.STARTED.type() == inst.status) {
                allStopped = false;
            } else if (Status.STARTING.type() == inst.status || Status.STOPPING.type() == inst.status) {
                allStarted = false;
                allStopped = false;
                anyInProgress = true;
            } else {
                allStarted = false;
            }
        }
        if (anyInProgress) {
            extAll[0].setDisable(true);
            extAll[1].setDisable(true);
            extAll[2].setDisable(true);
        } else if (allStarted) {
            extAll[0].setDisable(true);
            extAll[1].setDisable(false);
            extAll[2].setDisable(false);
        } else if (allStopped) {
            extAll[0].setDisable(false);
            extAll[1].setDisable(true);
            extAll[2].setDisable(true);
        } else {
            extAll[0].setDisable(false);
            extAll[1].setDisable(false);
            extAll[2].setDisable(false);
        }
    }

    // ==================== 通用工具方法 ====================

    private File findImportFile(ToolConfig.Instance instance) {
        File f = new File("output_" + instance.port.data + ".sql");
        return f.exists() ? f : null;
    }

    private boolean checkPathSilent(ToolConfig.Instance instance) {
        return Files.exists(Paths.get(instance.path.data));
    }

    private void enableButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(false);
        }
    }

    // ==================== 实例管理 ====================

    private void loadInstanceMenu() {
        instanceBox = buildInstanceBox((instance, allBtns) -> {
            Button editIni = ToolComponent.button(I18n.EDIT_INI.translate(toolService.getLanguage()));
            Button modify = ToolComponent.button(I18n.MODIFY_INSTANCE.translate(toolService.getLanguage()));
            Button delete = ToolComponent.stopButton(I18n.DELETE_INSTANCE.translate(toolService.getLanguage()));
            Button[] btns = {editIni, modify, delete};

            boolean isStarted = Status.STARTED.type() == instance.status;
            boolean inProgress = Status.STARTING.type() == instance.status || Status.STOPPING.type() == instance.status;
            for (Button btn : btns) btn.setDisable(isStarted || inProgress);

            editIni.setOnAction(e -> {
                if (checkPath(instance)) showIniEditor(instance);
            });
            modify.setOnAction(e -> showInstanceForm(instance));
            delete.setOnAction(e -> deleteInstance(instance));
            return btns;
        });

        ScrollPane scroll = ToolComponent.scrollPane();
        scroll.setContent(instanceBox);

        VBox center = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        center.setStyle("-fx-alignment: top_center;-fx-padding: 0 15 0 15;");
        body.setCenter(center);
    }

    private void showInstanceForm(ToolConfig.Instance existing) {
        BorderPane modalRoot = new BorderPane();
        Stage stage = ToolComponent.createModalStage(primaryStage, modalRoot, false);
        Region mask = ToolComponent.mask(root);

        boolean isEdit = existing != null;
        String title = isEdit ? I18n.MODIFY_INSTANCE.translate(toolService.getLanguage()) : I18n.ADD_INSTANCE.translate(toolService.getLanguage());

        TextField nameField = textField(isEdit ? existing.section.data : "");
        TextField pathField = textField(isEdit ? existing.path.data : "");
        TextField userField = textField(isEdit ? existing.username.data : "root");
        TextField passField = textField(isEdit ? existing.password.data : "");
        TextField portField = textField(isEdit ? existing.port.data : toolService.getDefaultPort());
        TextField dbField = textField(isEdit ? existing.database.data : "");

        VBox form = new VBox(8);
        form.setPadding(new Insets(16, 20, 8, 20));
        form.getChildren().addAll(
                formRow(I18n.INSTANCE_NAME_LABEL, nameField, true),
                formRow(I18n.INSTANCE_PATH_LABEL, pathField, true),
                formRow(I18n.INSTANCE_USERNAME_LABEL, userField, true),
                formRow(I18n.INSTANCE_PASSWORD_LABEL, passField, false),
                formRow(I18n.INSTANCE_PORT_LABEL, portField, true),
                formRow(I18n.INSTANCE_DATABASE_LABEL, dbField, false)
        );

        Label titleLabel = ToolComponent.label(title);
        titleLabel.setStyle(String.format("-fx-text-fill: %s;-fx-font-size: 15px;-fx-font-weight: bold;", ThemeColor.FONT_HOVER.color()));
        modalRoot.setTop(titleLabel);
        BorderPane.setMargin(modalRoot.getTop(), new Insets(12, 20, 0, 20));
        modalRoot.setCenter(form);

        Button cancel = ToolComponent.button(I18n.CANCEL.translate(toolService.getLanguage()));
        Button save = ToolComponent.primaryButton(I18n.OK.translate(toolService.getLanguage()));
        VBox bottom = ToolComponent.commonBottom(cancel, save);

        Runnable close = () -> {
            root.getChildren().remove(mask);
            stage.close();
        };
        cancel.setOnAction(e -> close.run());
        save.setOnAction(e -> {
            String name = nameField.getText().trim();
            String path = pathField.getText().trim();
            String port = portField.getText().trim();
            String db = dbField.getText().trim();
            boolean valid = true;
            if (name.isEmpty()) { setFieldError(nameField); valid = false; } else clearFieldError(nameField);
            if (path.isEmpty()) { setFieldError(pathField); valid = false; } else clearFieldError(pathField);
            if (userField.getText().trim().isEmpty()) { setFieldError(userField); valid = false; } else clearFieldError(userField);
            if (port.isEmpty()) { setFieldError(portField); valid = false; } else clearFieldError(portField);
            if (!valid) return;

            if (isEdit) {
                existing.section.data = name;
                existing.path.data = path;
                existing.username.data = userField.getText().trim();
                existing.password.data = passField.getText().trim();
                existing.port.data = port;
                existing.database.data = db;
            } else {
                ToolConfig.Instance inst = new ToolConfig.Instance();
                int sort = toolService.getNextInstanceSort();
                inst.section = new ToolConfig.Info<>(name, sort, I18n.NAME_COMMAND.comment());
                inst.path = new ToolConfig.Info<>(path, 0, I18n.PATH_COMMAND.comment());
                inst.username = new ToolConfig.Info<>(userField.getText().trim(), 1, I18n.USERNAME_COMMAND.comment());
                inst.password = new ToolConfig.Info<>(passField.getText().trim(), 2, I18n.PASSWORD_COMMAND.comment());
                inst.port = new ToolConfig.Info<>(port, 3, I18n.PORT_COMMAND.comment());
                inst.database = new ToolConfig.Info<>(db, 4, I18n.DATABASE_COMMAND.comment());
                toolService.getConfig().instances.add(inst);
            }
            toolService.saveConfig();
            close.run();
            loadMenuWithIndex(menuGroup.getToggles().indexOf(menuGroup.getSelectedToggle()));
        });

        modalRoot.setBottom(bottom);
        stage.sizeToScene();
        stage.show();
    }

    private void deleteInstance(ToolConfig.Instance instance) {
        String text = String.format(I18n.CONFIRM_DELETE.translate(toolService.getLanguage()), instance.section.data);
        if (ToolComponent.confirm(primaryStage, root, text, TipType.WARNING)) {
            toolService.getConfig().instances.remove(instance);
            toolService.saveConfig();
            loadMenuWithIndex(menuGroup.getToggles().indexOf(menuGroup.getSelectedToggle()));
        }
    }

    private void showIniEditor(ToolConfig.Instance instance) {
        BorderPane modalRoot = new BorderPane();
        Stage stage = ToolComponent.createModalStage(primaryStage, modalRoot, false);
        modalRoot.setPrefSize(ToolCommon.MAX_MODAL_WIDTH, ToolCommon.MAX_MODAL_HEIGHT);
        Region mask = ToolComponent.mask(root);

        Label warn = ToolComponent.label(I18n.INI_WARN.translate(toolService.getLanguage()));
        warn.setStyle(String.format("-fx-text-fill: %s;-fx-font-size: 12px;", ThemeColor.DANGER.color()));
        warn.setWrapText(true);
        warn.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setMargin(warn, new Insets(12, 20, 0, 20));
        modalRoot.setTop(warn);

        TextArea textArea = new TextArea();
        String bg = ThemeColor.CARD_BG.color();
        String fg = ThemeColor.FONT_HOVER.color();
        String borderColor = ThemeColor.BUTTON_BORDER.color();
        textArea.setStyle(String.format(
                "-fx-background-insets: 0; " +
                "-fx-background-color: transparent; " +
                "-fx-control-inner-background: %s; " +
                "-fx-text-fill: %s; " +
                "-fx-font-family: monospace; " +
                "-fx-font-size: 13px; " +
                "-fx-focus-color: %s; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 0; " +
                "-fx-highlight-fill: %s; " +
                "-fx-highlight-text-fill: #fff;",
                bg, fg, borderColor, borderColor, ThemeColor.PRIMARY.color()));

        // 给.content加padding，并美化滚动条，修复内部节点白色漏出
        textArea.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin == null) return;
            Node scrollPane = textArea.lookup(".scroll-pane");
            if (scrollPane != null) {
                scrollPane.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px;");
            }
            Node content = textArea.lookup(".content");
            if (content != null) {
                content.setStyle("-fx-background-color: transparent; -fx-padding: 8px;");
            }
            // 延迟确保scroll-bar节点已创建
            Platform.runLater(() -> styleTextAreaScrollBars(textArea));
        });

        try {
            textArea.setText(toolService.readMyIni(instance));
        } catch (Exception ex) {
            textArea.setText("");
        }
        BorderPane.setMargin(textArea, new Insets(8, 20, 0, 20));
        modalRoot.setCenter(textArea);

        Runnable close = () -> {
            root.getChildren().remove(mask);
            stage.close();
        };

        Button deleteIni = ToolComponent.stopButton(I18n.DELETE_INSTANCE.translate(toolService.getLanguage()));
        deleteIni.setOnAction(e -> {
            try {
                toolService.deleteMyIni(instance);
            } catch (Exception ex) {
                ToolComponent.error(primaryStage, root, ex.getMessage());
            }
            close.run();
        });

        Button importTpl = ToolComponent.button(I18n.INI_IMPORT_TEMPLATE.translate(toolService.getLanguage()));
        importTpl.setOnAction(e -> {
            String l = toolService.getLanguage();
            textArea.setText(
                "[mysqld]\n" +
                I18n.INI_TPL_CHARSET.translate(l) + "\n" +
                "character-set-server=utf8mb4\n" +
                I18n.INI_TPL_COLLATION.translate(l) + "\n" +
                "collation-server=utf8mb4_general_ci\n" +
                I18n.INI_TPL_ENGINE.translate(l) + "\n" +
                "default-storage-engine=INNODB\n" +
                I18n.INI_TPL_MAX_CONN.translate(l) + "\n" +
                "max_connections=200\n" +
                I18n.INI_TPL_CONN_ERR.translate(l) + "\n" +
                "max_connect_errors=10\n" +
                I18n.INI_TPL_TMP_TABLE.translate(l) + "\n" +
                "tmp_table_size=64M\n" +
                I18n.INI_TPL_MAX_PACKET.translate(l) + "\n" +
                "max_allowed_packet=1024M\n" +
                I18n.INI_TPL_LOWER_CASE.translate(l) + "\n" +
                "lower_case_table_names=1\n" +
                I18n.INI_TPL_BUFFER.translate(l) + "\n" +
                "innodb_buffer_pool_size=1G\n");
        });
        Button cancel = ToolComponent.button(I18n.CANCEL.translate(toolService.getLanguage()));
        Button save = ToolComponent.primaryButton(I18n.OK.translate(toolService.getLanguage()));
        VBox bottom = ToolComponent.commonBottom(cancel, save);
        HBox btnRow = (HBox) bottom.getChildren().get(1);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        btnRow.getChildren().addAll(0, Arrays.asList(deleteIni, importTpl, spacer));

        cancel.setOnAction(e -> close.run());
        save.setOnAction(e -> {
            try {
                toolService.writeMyIni(instance, textArea.getText());
                close.run();
                ToolComponent.info(primaryStage, root, String.format(I18n.INI_SAVE_SUCCESS.translate(toolService.getLanguage()), instance.port.data));
            } catch (Exception ex) {
                ToolComponent.error(primaryStage, root, ex.getMessage());
            }
        });

        modalRoot.setBottom(bottom);
        stage.sizeToScene();
        stage.show();
    }

    private TextField textField(String text) {
        TextField tf = new TextField(text);
        tf.setMaxWidth(Double.MAX_VALUE);
        clearFieldError(tf);
        return tf;
    }

    private void setFieldError(TextField tf) {
        tf.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 6px;-fx-background-radius: 6px;",
                ThemeColor.CARD_BG.color(), ThemeColor.FONT_BG.color(), ThemeColor.DANGER.color()));
    }

    private void clearFieldError(TextField tf) {
        tf.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 6px;-fx-background-radius: 6px;",
                ThemeColor.CARD_BG.color(), ThemeColor.FONT_BG.color(), ThemeColor.BUTTON_BORDER.color()));
    }

    private HBox formRow(I18n labelKey, TextField field, boolean required) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label text = ToolComponent.label(labelKey.translate(toolService.getLanguage()));
        HBox labelBox = new HBox(0);
        labelBox.setPrefWidth(90);
        labelBox.setAlignment(Pos.CENTER_LEFT);
        if (required) {
            Label star = new Label("*");
            star.setStyle(String.format("-fx-text-fill: %s;", ThemeColor.DANGER.color()));
            labelBox.getChildren().addAll(text, star);
        } else {
            labelBox.getChildren().add(text);
        }
        row.getChildren().addAll(labelBox, field);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void styleTextAreaScrollBars(TextArea textArea) {
        for (Node child : textArea.lookupAll(".scroll-bar")) {
            if (child instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) child;
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    bar.setStyle("-fx-background-color: transparent;-fx-pref-width: 10px;-fx-max-width: 10px;");
                } else {
                    bar.setStyle("-fx-background-color: transparent;-fx-pref-height: 10px;-fx-max-height: 10px;");
                }
                Node thumb = bar.lookup(".thumb");
                if (thumb != null) thumb.setStyle("-fx-background-color: #B0B0B0;-fx-background-insets: 2px;-fx-background-radius: 5px;");
            }
        }
    }

    private void loadMenuWithIndex(int index) {
        if (index == 1) {
            loadExtMenu();
        } else if (index == 2) {
            loadInstanceMenu();
        } else {
            loadOperateMenu();
        }
    }
}
