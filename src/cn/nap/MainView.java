package cn.nap;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.Cursor;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.Status;
import static cn.nap.ToolCommon.ThemeColor;

/**
 * v2 主界面：自绘标题栏 + 实例列表 + 底栏三态 + 应用内浮层。
 */
public class MainView {

    private static final String[] ACCENTS = {"blue", "green", "purple", "slate", "orange"};

    private final ToolApp app;
    private final Stage stage;
    private final ToolService service = ToolService.getInstance();

    private final StackPane root = new StackPane();
    private final HBox titleBar;
    private final VBox list = new VBox();
    private final VBox content = new VBox();
    private final HBox bar = new HBox();
    private final HBox header = new HBox(10);
    private final Label summaryLabel = ToolComponent.label("", 12, ThemeColor.TEXT_2);

    private final Map<ToolConfig.Instance, Label> metaLabels = new HashMap<>();
    private final WindowBehavior behavior;
    private final ScrollPane listScroll = new ScrollPane();
    private int layoutInstanceCount = -1;
    private Popup activeMenu;
    private ToolConfig.Instance draggedInstance;
    private Node draggedHandle;
    private Region draggedCard;
    private double dragSceneY;
    private final Region dropLine = new Region();
    private final Timeline reorderScroll = new Timeline(new KeyFrame(Duration.millis(40), event -> autoScrollOrder()));

    public MainView(ToolApp app, Stage stage) {
        this.app = app;
        this.stage = stage;
        root.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (isOrderDrag(event)) {
                dragSceneY = event.getSceneY();
            }
        });
        root.setOnDragExited(event -> {
            if (event.getTarget() == root) {
                dragSceneY = Double.NaN;
            }
        });

        root.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1;",
                ThemeColor.BACKGROUND.color(), ThemeColor.BORDER.color()));

        titleBar = buildTitleBar();

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 8, 16));
        renderHeader();

        content.setPadding(new Insets(0, 16, 0, 16));
        content.setMinHeight(0);
        listScroll.setContent(list);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        root.getStylesheets().add(ToolComponent.appCss());
        listScroll.skinProperty().addListener((observable, oldSkin, newSkin) -> {
            Node viewport = listScroll.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: transparent;");
            }
        });
        content.getChildren().add(listScroll);
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        bar.setMinHeight(ToolCommon.BAR_HEIGHT);
        bar.setPrefHeight(ToolCommon.BAR_HEIGHT);
        bar.setMaxHeight(ToolCommon.BAR_HEIGHT);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(9);
        bar.setPadding(new Insets(0, ToolCommon.CONTENT_PAD, 0, ToolCommon.CONTENT_PAD));

        VBox shell = new VBox();
        shell.getChildren().addAll(titleBar, header, content, bar);
        if (app.consumeOpenSettingsOnStart()) {
            Platform.runLater(this::showSettingsSheet);
        }
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().add(shell);

        behavior = new WindowBehavior(stage, root, titleBar);
        render();
    }

    public StackPane getRoot() {
        return root;
    }

    public void render() {
        resizeForInstanceCount();
        applyListLayout();
        renderList();
        renderBar();
    }

    private boolean compactLayout() {
        return ToolCommon.isCompactCount(service.getConfig().instances.size());
    }

    private boolean singleLayout() {
        return service.getConfig().instances.size() == 1;
    }

    private void applyListLayout() {
        boolean compact = compactLayout();
        header.setVisible(!compact);
        header.setManaged(!compact);
        if (compact) {
            listScroll.setContent(null);
            if (list.getParent() != content) {
                content.getChildren().setAll(list);
            }
            list.setPadding(Insets.EMPTY);
        } else {
            if (list.getParent() == content) {
                content.getChildren().clear();
            }
            listScroll.setContent(list);
            if (listScroll.getParent() != content) {
                content.getChildren().setAll(listScroll);
            }
            list.setPadding(new Insets(0, 6, 0, 0));
        }
        content.setPadding(singleLayout() ? new Insets(12, 16, 4, 16)
                : compact ? new Insets(12, 20, 12, 20) : new Insets(0, 8, 0, 16));
        root.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1;",
                ThemeColor.BACKGROUND.color(), ThemeColor.BORDER.color()));
    }

    /** 仅在实例数量改变且窗口仍为自动尺寸时调整；保留用户手动设置的大小。 */
    private void resizeForInstanceCount() {
        int count = service.getConfig().instances == null ? 0 : service.getConfig().instances.size();
        if (count == layoutInstanceCount || root.getChildren().size() > 1) {
            return;
        }
        stage.setMinHeight(ToolCommon.isCompactCount(count)
                ? ToolCommon.windowHeightFor(count) : ToolCommon.WIN_MIN_HEIGHT);
        if (layoutInstanceCount >= 0 && stage.isShowing()
                && Math.abs(stage.getHeight() - ToolCommon.windowHeightFor(layoutInstanceCount)) < 2) {
            stage.setHeight(ToolCommon.windowHeightFor(count));
        }
        if (layoutInstanceCount >= 0 && stage.isShowing()
                && Math.abs(stage.getWidth() - ToolCommon.windowWidthFor(layoutInstanceCount)) < 2) {
            stage.setWidth(ToolCommon.windowWidthFor(count));
        }
        layoutInstanceCount = count;
    }

    /**
     * 每秒只刷新时长文本，不重建节点。
     */
    public void refreshDurations() {
        for (Map.Entry<ToolConfig.Instance, Label> entry : metaLabels.entrySet()) {
            entry.getValue().setText(metaText(entry.getKey()));
        }
        renderBar();
    }

    private String lang() {
        return service.getLanguage();
    }

    private void renderHeader() {
        summaryLabel.setStyle("-fx-text-fill: " + ThemeColor.TEXT_2.color() + "; -fx-font-size: 12px;");
        Button addButton = ToolComponent.textButton(I18n.ADD_INSTANCE_BTN.translate(lang()), ThemeColor.BLUE);
        addButton.setGraphic(ToolComponent.strokeIcon(ToolCommon.LINE_PLUS, 16, ThemeColor.BLUE));
        addButton.setStyle(addButton.getStyle()
                + "-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-min-height: 30; -fx-pref-height: 30;");
        addButton.setOnAction(e -> showEditDialog(null));
        header.getChildren().setAll(summaryLabel, ToolComponent.hspacer(), addButton);
    }

    /**
     * 更新浮层下面的界面，保留 Scene、窗口行为和已经打开的设置节点。
     */
    private void refreshAppearance() {
        double scrollPosition = listScroll.getVvalue();
        root.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1;",
                ThemeColor.BACKGROUND.color(), ThemeColor.BORDER.color()));
        HBox freshTitle = buildTitleBar();
        List<Node> titleChildren = new ArrayList<>(freshTitle.getChildren());
        freshTitle.getChildren().clear();
        titleBar.setStyle(freshTitle.getStyle());
        titleBar.getChildren().setAll(titleChildren);
        renderHeader();
        root.getStylesheets().setAll(ToolComponent.appCss());
        stage.setTitle(I18n.TOOL_TITLE.translate(lang()));
        if (root.getScene() != null) {
            root.getScene().setFill(Color.web(ThemeColor.BACKGROUND.color()));
        }
        render();
        listScroll.setVvalue(scrollPosition);
    }

    // ==================== 标题栏 ====================

    private HBox buildTitleBar() {
        HBox box = new HBox(9);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinHeight(ToolCommon.TITLE_BAR_HEIGHT);
        box.setPrefHeight(ToolCommon.TITLE_BAR_HEIGHT);
        box.setMaxHeight(ToolCommon.TITLE_BAR_HEIGHT);
        box.setPadding(new Insets(0, 0, 0, 12));
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: transparent transparent %s transparent;"
                        + " -fx-border-width: 0 0 1 0;",
                ThemeColor.TITLE_BAR.color(), ThemeColor.CARD_BORDER.color()));

        StackPane icon = new StackPane();
        icon.setMinSize(16, 16);
        icon.setPrefSize(16, 16);
        icon.setMaxSize(16, 16);
        icon.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 4;", ThemeColor.BLUE.color()));
        icon.getChildren().add(ToolComponent.strokeIcon(ToolCommon.LINE_MARK, 16, ThemeColor.WHITE));

        Label title = ToolComponent.label(I18n.TOOL_TITLE.translate(lang()), 12.5, ThemeColor.TEXT);
        Label version = ToolComponent.label(ToolCommon.VERSION, 11, ThemeColor.TEXT_3);
        HBox.setMargin(version, new Insets(1, 0, 0, 2));

        Region spacer = ToolComponent.hspacer();

        javafx.scene.control.Button minimize = windowButton(0, false);
        minimize.setOnAction(e -> stage.setIconified(true));
        javafx.scene.control.Button maximize = windowButton(1, false);
        maximize.setOnAction(e -> behavior.toggleMaximize());
        javafx.scene.control.Button close = windowButton(2, true);
        close.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(icon, title, version, spacer, minimize, maximize, close);
        return box;
    }

    private javafx.scene.control.Button windowButton(int kind, boolean danger) {
        javafx.scene.control.Button button = new javafx.scene.control.Button();
        button.setFocusTraversable(false);
        button.getStyleClass().add("window-btn");
        button.setGraphic(windowGlyph(kind, ThemeColor.TEXT));
        String normal = String.format(
                "-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 0;"
                        + " -fx-min-width: 46; -fx-pref-width: 46; -fx-max-width: 46;"
                        + " -fx-min-height: %.0f; -fx-pref-height: %.0f; -fx-max-height: %.0f;",
                ToolCommon.TITLE_BAR_HEIGHT, ToolCommon.TITLE_BAR_HEIGHT, ToolCommon.TITLE_BAR_HEIGHT);
        button.setStyle(normal);
        button.setOnMouseEntered(e -> {
            if (danger) {
                button.setStyle(normal.replace("-fx-background-color: transparent;",
                        "-fx-background-color: #e81123;"));
                button.setGraphic(windowGlyph(kind, ThemeColor.WHITE));
            } else {
                button.setStyle(normal.replace("-fx-background-color: transparent;",
                        "-fx-background-color: " + ThemeColor.HOVER_2.color() + ";"));
            }
        });
        button.setOnMouseExited(e -> {
            button.setStyle(normal);
            button.setGraphic(windowGlyph(kind, ThemeColor.TEXT));
        });
        return button;
    }

    /**
     * 标题栏同样使用固定画布，最小化、最大化和关闭的线宽保持一致。
     */
    private Region windowGlyph(int kind, ThemeColor color) {
        String svg = kind == 0 ? ToolCommon.LINE_MIN : kind == 1 ? ToolCommon.LINE_MAX : ToolCommon.LINE_CLOSE;
        return ToolComponent.strokeIcon(svg, 16, color);
    }

    // ==================== 列表 ====================

    private void renderList() {
        list.getChildren().clear();
        metaLabels.clear();
        List<ToolConfig.Instance> instances = service.getConfig() == null ? null : service.getConfig().instances;
        if (instances != null) {
            if (compactLayout()) {
                for (int index = 0; index < instances.size(); index++) {
                    HBox panel = buildControlPanel(instances.get(index), instances.size() == 2);
                    if (index < instances.size() - 1) {
                        VBox.setMargin(panel, new Insets(0, 0, 10, 0));
                    }
                    list.getChildren().add(panel);
                }
                return;
            }
            for (int index = 0; index < instances.size(); index++) {
                HBox row = buildRow(instances.get(index));
                if (compactLayout() && index < instances.size() - 1) {
                    row.setStyle(row.getStyle() + "-fx-border-color: transparent transparent "
                            + ThemeColor.LINE_2.color() + " transparent; -fx-border-width: 0 0 1 0;");
                }
                list.getChildren().add(row);
            }
        }

    }

    /** 少量实例使用完整操作面板；双实例收紧面板内边距和信息间距。 */
    private HBox buildControlPanel(ToolConfig.Instance instance, boolean dense) {
        VBox panel = new VBox(dense ? 12 : 18);
        HBox card = new HBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(dense ? 12 : 16));
        card.setStyle("-fx-background-color: " + ThemeColor.CARD.color()
                + "; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;"
                + " -fx-border-color: " + ThemeColor.CARD_BORDER.color() + ";");
        Label name = ToolComponent.label(instance.section.data, 16, ThemeColor.TEXT, true);
        name.setMinWidth(0);
        Label details = ToolComponent.label(metaText(instance), 12, ThemeColor.TEXT_2);
        details.setMinWidth(0);
        details.setMaxWidth(Double.MAX_VALUE);
        metaLabels.put(instance, details);
        VBox identity = new VBox(6, name, details);
        identity.setMinWidth(0);
        HBox heading = new HBox(16, identity, ToolComponent.hspacer(), statusBox(instance, true));
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identity, Priority.ALWAYS);

        boolean started = instance.status == Status.STARTED.type();
        boolean busy = app.getRunning() != null || instance.status == Status.STARTING.type()
                || instance.status == Status.STOPPING.type();
        Button toggle = started ? ToolComponent.stopButton(I18n.STOP.translate(lang()))
                : ToolComponent.primaryButton(I18n.START.translate(lang()));
        toggle.setDisable(busy);
        toggle.setOnAction(e -> toggleInstance(instance));
        Button restart = ToolComponent.plainButton(I18n.RESTART.translate(lang()));
        restart.setDisable(busy || !started);
        restart.setOnAction(e -> restartInstance(instance));
        Button more = ToolComponent.plainButton(I18n.MORE_ACTIONS.translate(lang()));
        more.setGraphic(ToolComponent.dots(ThemeColor.TEXT_2, 2));
        more.setGraphicTextGap(6);
        more.setDisable(busy);
        more.setOnAction(e -> showRowMenu(instance, more));
        String secondaryStyle = "-fx-background-color: " + ThemeColor.FILL.color()
                + "; -fx-border-color: " + ThemeColor.CARD_BORDER.color()
                + "; -fx-border-width: 1; -fx-border-radius: 6;";
        restart.setStyle(restart.getStyle() + secondaryStyle);
        more.setStyle(more.getStyle() + secondaryStyle);
        HBox actions = new HBox(8, toggle, restart, more);
        for (Button button : List.of(toggle, restart, more)) {
            button.setMinWidth(0);
            button.setPrefWidth(140);
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        panel.getChildren().addAll(heading, actions);
        panel.setMinWidth(0);
        HBox.setHgrow(panel, Priority.ALWAYS);
        if (!singleLayout()) {
            card.getChildren().add(orderHandle(instance, card));
        }
        card.getChildren().add(panel);
        javafx.scene.control.Tooltip.install(identity, new javafx.scene.control.Tooltip(instance.path.data));
        return card;
    }

    private HBox buildRow(ToolConfig.Instance instance) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        boolean compact = compactLayout();
        row.setPadding(compact ? Insets.EMPTY : new Insets(0, 18, 0, 18));
        row.setMinHeight(ToolCommon.ROW_HEIGHT);
        row.setPrefHeight(ToolCommon.ROW_HEIGHT);
        row.setMaxHeight(ToolCommon.ROW_HEIGHT);
        row.setStyle(compact ? "-fx-background-color: transparent;" : cardStyle(false));
        VBox.setMargin(row, compact ? Insets.EMPTY : new Insets(0, 0, 8, 0));
        if (!compact) {
            row.setOnMouseEntered(e -> row.setStyle(cardStyle(true)));
            row.setOnMouseExited(e -> row.setStyle(cardStyle(false)));
        }

        VBox main = new VBox(7);
        main.setMinWidth(0);
        main.setAlignment(Pos.CENTER_LEFT);
        Label name = ToolComponent.label(instance.section.data, 16, ThemeColor.TEXT, true);
        name.setMinWidth(0);
        Label meta = ToolComponent.label(metaText(instance), 12.5, ThemeColor.TEXT_3);
        meta.setMinWidth(0);
        meta.setMaxWidth(Double.MAX_VALUE);
        metaLabels.put(instance, meta);
        if (compact) {
            HBox heading = new HBox(12, name, statusBox(instance, true));
            heading.setAlignment(Pos.CENTER_LEFT);
            name.setMaxWidth(220);
            main.getChildren().addAll(heading, meta);
        } else {
            main.getChildren().addAll(name, meta);
        }
        HBox.setHgrow(main, Priority.ALWAYS);

        HBox right = new HBox(14);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (!compact) {
            right.getChildren().add(statusBox(instance));
        }
        Button restart = ToolComponent.textButton(I18n.RESTART.translate(lang()), ThemeColor.TEXT_2);
        restart.setMinWidth(56);
        restart.setPrefWidth(56);
        restart.setMaxWidth(56);
        restart.setDisable(app.getRunning() != null || instance.status != Status.STARTED.type());
        restart.setOnAction(e -> restartInstance(instance));
        right.getChildren().addAll(actionButton(instance), restart, moreButton(instance));

        row.getChildren().addAll(orderHandle(instance, row), main, right);
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showEditDialog(instance);
            }
        });
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(tooltipText(instance));
        tip.setShowDelay(javafx.util.Duration.millis(600));
        javafx.scene.control.Tooltip.install(row, tip);
        return row;
    }

    /** 只有六点手柄能发起内部排序，卡片按钮和文字仍保持原有行为。 */
    private Region orderHandle(ToolConfig.Instance instance, Region card) {
        VBox grip = new VBox(3);
        grip.getStyleClass().add("order-handle");
        grip.setAlignment(Pos.CENTER);
        grip.setMinSize(16, 32);
        grip.setPrefSize(16, 32);
        grip.setMaxSize(16, 32);
        grip.setCursor(Cursor.OPEN_HAND);
        for (int index = 0; index < 3; index++) {
            HBox pair = new HBox(3, ToolComponent.dot(ThemeColor.TEXT_3, 2.5),
                    ToolComponent.dot(ThemeColor.TEXT_3, 2.5));
            pair.setAlignment(Pos.CENTER);
            pair.setMouseTransparent(true);
            grip.getChildren().add(pair);
        }
        grip.setDisable(service.isLoadFailed() || app.getRunning() != null);
        javafx.scene.control.Tooltip.install(grip,
                new javafx.scene.control.Tooltip(I18n.DRAG_TO_REORDER.translate(lang())));
        grip.setOnMouseClicked(event -> event.consume());
        grip.setOnDragDetected(event -> {
            if (grip.isDisabled() || service.getConfig().instances.size() < 2) {
                return;
            }
            if (activeMenu != null) {
                activeMenu.hide();
            }
            ClipboardContent payload = new ClipboardContent();
            payload.putString("NapMysqlTool.instance-order");
            grip.startDragAndDrop(TransferMode.MOVE).setContent(payload);
            draggedInstance = instance;
            draggedHandle = grip;
            draggedCard = card;
            card.setOpacity(0.55);
            dragSceneY = event.getSceneY();
            reorderScroll.setCycleCount(Timeline.INDEFINITE);
            reorderScroll.playFromStart();
            event.consume();
        });
        grip.setOnDragDone(event -> finishOrderDrag());
        card.setOnDragOver(event -> {
            if (!isOrderDrag(event)) {
                return;
            }
            event.acceptTransferModes(TransferMode.MOVE);
            dragSceneY = event.getSceneY();
            boolean after = card.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() >= card.getHeight() / 2;
            int destination = orderDropIndex(instance, after);
            dropLine.setVisible(destination != service.getConfig().instances.indexOf(draggedInstance));
            if (dropLine.isVisible()) {
                if (dropLine.getParent() != list) {
                    dropLine.setManaged(false);
                    dropLine.setMouseTransparent(true);
                    list.getChildren().add(dropLine);
                }
                dropLine.setStyle("-fx-background-color: " + ThemeColor.BLUE.color() + "; -fx-background-radius: 1;");
                double y = after ? card.getBoundsInParent().getMaxY() - 2 : card.getBoundsInParent().getMinY();
                dropLine.resizeRelocate(card.getLayoutX(), y, card.getWidth(), 2);
            }
            event.consume();
        });
        card.setOnDragExited(event -> dropLine.setVisible(false));
        card.setOnDragDropped(event -> {
            if (!isOrderDrag(event)) {
                return;
            }
            boolean after = card.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() >= card.getHeight() / 2;
            boolean saved = service.moveInstance(draggedInstance, orderDropIndex(instance, after));
            event.setDropCompleted(saved);
            finishOrderDrag();
            if (saved) {
                double scrollPosition = listScroll.getVvalue();
                Platform.runLater(() -> {
                    render();
                    root.applyCss();
                    root.layout();
                    listScroll.setVvalue(scrollPosition);
                });
            } else {
                alert(I18n.REORDER_SAVE_FAILED.translate(lang()));
            }
            event.consume();
        });
        return grip;
    }

    private boolean isOrderDrag(DragEvent event) {
        return draggedInstance != null && event.getGestureSource() == draggedHandle
                && event.getDragboard().hasString()
                && "NapMysqlTool.instance-order".equals(event.getDragboard().getString());
    }

    private int orderDropIndex(ToolConfig.Instance target, boolean after) {
        List<ToolConfig.Instance> instances = service.getConfig().instances;
        int destination = instances.indexOf(target) + (after ? 1 : 0);
        return destination - (instances.indexOf(draggedInstance) < destination ? 1 : 0);
    }

    private void autoScrollOrder() {
        if (draggedInstance == null || listScroll.getParent() != content) {
            return;
        }
        Bounds viewport = listScroll.localToScene(listScroll.getBoundsInLocal());
        if (!Double.isFinite(dragSceneY) || dragSceneY < viewport.getMinY() || dragSceneY > viewport.getMaxY()) {
            return;
        }
        double delta = dragSceneY < viewport.getMinY() + 28 ? -6
                : dragSceneY > viewport.getMaxY() - 28 ? 6 : 0;
        double overflow = list.getHeight() - listScroll.getViewportBounds().getHeight();
        if (overflow > 0 && delta != 0) {
            listScroll.setVvalue(Math.max(0, Math.min(1, listScroll.getVvalue() + delta / overflow)));
        }
    }

    private void finishOrderDrag() {
        reorderScroll.stop();
        if (draggedCard != null) {
            draggedCard.setOpacity(1);
        }
        draggedInstance = null;
        draggedHandle = null;
        draggedCard = null;
        dropLine.setVisible(false);
        list.getChildren().remove(dropLine);
    }

    /**
     * 扁平卡片：细边框表达层次，悬停只改变描边。
     */
    private String cardStyle(boolean hover) {
        return String.format(
                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-radius: 10;"
                        + " -fx-border-color: %s; -fx-border-width: 1;"
                        + " -fx-effect: none;",
                ThemeColor.CARD.color(),
                hover ? ThemeColor.BLUE.color() : ThemeColor.CARD_BORDER.color());
    }

    /**
     * 添加实例：虚线描边的轻量卡片。
     */
    private String ghostStyle(boolean hover) {
        return String.format(
                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-radius: 10;"
                        + " -fx-border-color: %s; -fx-border-width: 1; -fx-border-style: segments(4, 4);",
                hover ? ThemeColor.CARD.color() : "transparent",
                hover ? ThemeColor.BLUE.color() : ThemeColor.CARD_BORDER.color());
    }

    private String metaText(ToolConfig.Instance instance) {
        Status status = Status.fromType(instance.status);
        long elapsed = elapsedSeconds(instance);
        if (status == Status.STARTING) {
            return String.format(I18n.STARTING_HINT.translate(lang()), instance.port.data, elapsed);
        }
        if (status == Status.STOPPING) {
            return String.format(I18n.STOPPING_HINT.translate(lang()), instance.port.data, elapsed);
        }
        return instance.port.data + " · " + instance.path.data;
    }

    private long elapsedSeconds(ToolConfig.Instance instance) {
        ToolApp.Running running = app.getRunning();
        if (running == null || running.instance != instance) {
            return 0;
        }
        return Math.max(0, (System.currentTimeMillis() - running.start) / 1000);
    }

    private String tooltipText(ToolConfig.Instance instance) {
        Status status = Status.fromType(instance.status);
        String pid = service.getPid(instance);
        if (status == Status.STARTED && pid != null) {
            long start = service.getStartTime(instance);
            if (start > 0) {
                long up = Math.max(0, System.currentTimeMillis() - start);
                return "PID " + pid + " · " + I18n.RUNNING_LOG.translate(lang()).replace("%s", duration(up));
            }
            return "PID " + pid;
        }
        return instance.path.data;
    }

    private HBox statusBox(ToolConfig.Instance instance) {
        return statusBox(instance, false);
    }

    private HBox statusBox(ToolConfig.Instance instance, boolean compact) {
        Status status = Status.fromType(instance.status);
        ThemeColor color = status == Status.STARTED ? ThemeColor.GREEN_TEXT
                : status == Status.STOPPED ? ThemeColor.TEXT_3 : ThemeColor.AMBER_TEXT;
        ThemeColor dotColor = status == Status.STARTED ? ThemeColor.GREEN
                : status == Status.STOPPED ? ThemeColor.TEXT_3 : ThemeColor.AMBER;
        Label text = ToolComponent.label(status.i18n().translate(lang()), 12.5, color);
        text.setMinWidth(Region.USE_PREF_SIZE);
        HBox box = new HBox(7, ToolComponent.dot(dotColor, 6), text);
        box.setAlignment(compact ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        if (compact) {
            box.setMinWidth(Region.USE_PREF_SIZE);
            box.setMaxWidth(Region.USE_PREF_SIZE);
            return box;
        }
        box.setMinWidth(ToolCommon.ACTION_COLUMN);
        box.setPrefWidth(ToolCommon.ACTION_COLUMN);
        box.setMaxWidth(ToolCommon.ACTION_COLUMN);
        return box;
    }

    private javafx.scene.control.Button actionButton(ToolConfig.Instance instance) {
        Status status = Status.fromType(instance.status);
        boolean running = status == Status.STARTED;
        boolean busy = status == Status.STARTING || status == Status.STOPPING || app.getRunning() != null;
        String text = running ? I18n.STOP.translate(lang()) : I18n.START.translate(lang());
        ThemeColor color = running ? ThemeColor.RED : ThemeColor.BLUE;
        javafx.scene.control.Button button = ToolComponent.textButton(text, color);
        button.setMinWidth(ToolCommon.ACTION_BUTTON);
        button.setPrefWidth(ToolCommon.ACTION_BUTTON);
        button.setMaxWidth(ToolCommon.ACTION_BUTTON);
        button.setMinHeight(32);
        button.setPrefHeight(32);
        button.setDisable(busy);
        button.setOnAction(e -> toggleInstance(instance));
        return button;
    }

    private javafx.scene.control.Button moreButton(ToolConfig.Instance instance) {
        javafx.scene.control.Button button = new javafx.scene.control.Button();
        button.setFocusTraversable(false);
        button.setGraphic(ToolComponent.dots(ThemeColor.TEXT_3, 3));
        button.setStyle(String.format(
                "-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 7; -fx-cursor: hand;"
                        + " -fx-min-width: %.0f; -fx-pref-width: %.0f; -fx-max-width: %.0f;"
                        + " -fx-min-height: %.0f; -fx-pref-height: %.0f; -fx-max-height: %.0f;",
                ToolCommon.MORE_BUTTON, ToolCommon.MORE_BUTTON, ToolCommon.MORE_BUTTON,
                ToolCommon.MORE_BUTTON, ToolCommon.MORE_BUTTON, ToolCommon.MORE_BUTTON));
        ToolComponent.installHover(button, true);
        button.setDisable(app.getRunning() != null);
        button.setOnAction(e -> showRowMenu(instance, button));
        return button;
    }

    private HBox buildAddRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 18, 0, 18));
        row.setMinHeight(52);
        row.setPrefHeight(52);
        row.setMaxHeight(52);
        row.setStyle(ghostStyle(false));
        VBox.setMargin(row, new Insets(2, 0, 12, 0));
        row.setOnMouseEntered(e -> row.setStyle(ghostStyle(true)));
        row.setOnMouseExited(e -> row.setStyle(ghostStyle(false)));
        Label label = ToolComponent.label(I18n.ADD_INSTANCE_ROW.translate(lang()), 13, ThemeColor.TEXT_3);
        row.getChildren().addAll(ToolComponent.strokeIcon(ToolCommon.LINE_PLUS, 16, ThemeColor.TEXT_2), label);
        row.setOnMouseEntered(e -> label.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 13px;",
                ThemeColor.TEXT_2.color())));
        row.setOnMouseExited(e -> label.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 13px;",
                ThemeColor.TEXT_3.color())));
        row.setOnMouseClicked(e -> showEditDialog(null));
        return row;
    }

    // ==================== 底栏 ====================

    private void renderBar() {
        bar.getChildren().clear();
        boolean compact = compactLayout();
        bar.setPadding(new Insets(0, compact ? 20 : ToolCommon.CONTENT_PAD, 0, compact ? 20 : ToolCommon.CONTENT_PAD));
        bar.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s transparent transparent transparent;"
                        + " -fx-border-width: " + (compact ? "0;" : "1 0 0 0;"),
                app.getRunning() != null || app.getIni() != null ? ThemeColor.FILL.color()
                        : ThemeColor.BACKGROUND.color(),
                ThemeColor.LINE.color()));

        ToolApp.Running running = app.getRunning();
        if (running != null) {
            long elapsed = Math.max(0, (System.currentTimeMillis() - running.start) / 1000);
            Label message = ToolComponent.label(running.label, 12.5, ThemeColor.TEXT_2);
            bar.getChildren().addAll(
                    ToolComponent.strokeIcon(ToolCommon.LINE_CLOCK, 16, ThemeColor.AMBER),
                    message,
                    ToolComponent.label("· " + String.format(I18n.ELAPSED.translate(lang()), elapsed),
                            12.5, ThemeColor.TEXT_3),
                    ToolComponent.hspacer());
            return;
        }

        ToolApp.IniSession ini = app.getIni();
        if (ini != null) {
            Label text = ToolComponent.label(ini.deleted
                    ? String.format(I18n.INI_DELETE_SUCCESS.translate(lang()), ini.instance.section.data)
                    : I18n.INI_EDITING.translate(lang()), 12.5, ThemeColor.TEXT_2);
            bar.getChildren().addAll(
                    ToolComponent.strokeIcon(ini.deleted ? ToolCommon.LINE_DELETE : ToolCommon.LINE_FILE,
                            16, ini.deleted ? ThemeColor.AMBER : ThemeColor.BLUE),
                    text,
                    ToolComponent.label(iniText(ini), 12, ThemeColor.TEXT_3),
                    ToolComponent.hspacer(),
                    barButton((ini.instance.status == Status.STOPPED.type() ? I18n.START : I18n.RESTART_INSTANCE)
                            .translate(lang()), () -> restartInstance(ini.instance)),
                    barButton(I18n.END_SESSION.translate(lang()), this::endIni));
            return;
        }

        List<ToolConfig.Instance> instances = service.getConfig() == null ? null : service.getConfig().instances;
        int total = instances == null ? 0 : instances.size();
        int runningCount = 0;
        if (instances != null) {
            for (ToolConfig.Instance instance : instances) {
                if (instance.status == Status.STARTED.type()) {
                    runningCount++;
                }
            }
        }
        summaryLabel.setText(String.format((total == 1 ? I18n.INSTANCE_SUMMARY_SINGLE : I18n.INSTANCE_SUMMARY)
                .translate(lang()), total, runningCount));
        Button settings = barButton(I18n.SETTINGS.translate(lang()), this::showSettingsSheet);
        settings.setGraphic(ToolComponent.strokeIcon(ToolCommon.LINE_SETTINGS, 16, ThemeColor.TEXT_2));
        if (compact) {
            Button add = barButton(I18n.ADD_INSTANCE_BTN.translate(lang()), () -> showEditDialog(null));
            add.setGraphic(ToolComponent.strokeIcon(ToolCommon.LINE_PLUS, 14, ThemeColor.TEXT_2));
            bar.getChildren().add(add);
        }
        bar.getChildren().addAll(ToolComponent.hspacer(), settings);
    }

    private javafx.scene.control.Button barButton(String text, Runnable action) {
        javafx.scene.control.Button button = ToolComponent.textButton(text, ThemeColor.TEXT_2);
        button.setStyle(button.getStyle() + "-fx-font-size: 12px; -fx-min-height: 26; -fx-pref-height: 26;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Label versionLabel() {
        Label label = ToolComponent.label(ToolCommon.VERSION, 11.5, ThemeColor.TEXT_3);
        label.setPadding(new Insets(0, 0, 0, 10));
        label.setStyle(label.getStyle() + "-fx-cursor: hand;");
        label.setOnMouseClicked(e -> showSettingsSheet());
        return label;
    }

    private String iniText(ToolApp.IniSession ini) {
        String hint = iniApplyHint(ini.instance);
        if (ini.deleted) {
            return hint;
        }
        long saved = ini.file.lastModified();
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(saved));
        String savedText = ini.saved ? String.format(I18n.INI_SAVED.translate(lang()), time) + " · " : "";
        return savedText + hint;
    }

    private String iniApplyHint(ToolConfig.Instance instance) {
        return (instance.status == Status.STOPPED.type() ? I18n.INI_NEXT_START_HINT : I18n.INI_RESTART_HINT)
                .translate(lang());
    }

    private String editorDisplayName(String editor) {
        if (editor == null || editor.isEmpty() || "default".equalsIgnoreCase(editor)) {
            return I18n.SYSTEM_DEFAULT.translate(lang());
        }
        if ("notepad".equalsIgnoreCase(editor)) {
            return I18n.NOTEPAD.translate(lang());
        }
        return new File(editor).getName();
    }

    // ==================== 菜单 ====================

    private void showRowMenu(ToolConfig.Instance instance, Node anchor) {
        List<Node> items = new ArrayList<>();
        final Runnable[] closeMenu = {() -> {}};

        javafx.scene.control.Button repair = ToolComponent.menuItem(I18n.REPAIR.translate(lang()), ToolCommon.LINE_REPAIR, ThemeColor.TEXT);
        repair.setOnAction(e -> {
            closeMenu[0].run();
            repairInstance(instance);
        });
        javafx.scene.control.Button importDb = ToolComponent.menuItem(I18n.IMPORT_DB.translate(lang()), ToolCommon.LINE_IMPORT, ThemeColor.TEXT);
        importDb.setOnAction(e -> {
            closeMenu[0].run();
            importInstance(instance);
        });
        javafx.scene.control.Button exportDb = ToolComponent.menuItem(I18n.EXPORT_DB.translate(lang()), ToolCommon.LINE_EXPORT, ThemeColor.TEXT);
        exportDb.setOnAction(e -> {
            closeMenu[0].run();
            exportInstance(instance);
        });
        javafx.scene.control.Button editIni = ToolComponent.menuItem(I18n.EDIT_MY_INI.translate(lang()), ToolCommon.LINE_FILE, ThemeColor.TEXT);
        editIni.setOnAction(e -> {
            closeMenu[0].run();
            editIni(instance);
        });
        Button deleteIni = ToolComponent.menuItem(I18n.INI_DELETE_FILE.translate(lang()), ToolCommon.LINE_DELETE, ThemeColor.RED);
        deleteIni.setDisable(!new File(instance.path.data, "my.ini").isFile());
        deleteIni.setOnAction(e -> {
            closeMenu[0].run();
            deleteIni(instance);
        });
        javafx.scene.control.Button modify = ToolComponent.menuItem(I18n.MODIFY_INSTANCE.translate(lang()), ToolCommon.LINE_EDIT, ThemeColor.TEXT);
        modify.setOnAction(e -> {
            closeMenu[0].run();
            showEditDialog(instance);
        });
        Button remove = ToolComponent.menuItem(I18n.DELETE_INSTANCE.translate(lang()), ToolCommon.LINE_DELETE, ThemeColor.RED);
        remove.setOnAction(e -> {
            closeMenu[0].run();
            removeInstance(instance);
        });

        boolean hasDb = instance.database != null && instance.database.data != null
                && !instance.database.data.trim().isEmpty();
        boolean stopped = instance.status == Status.STOPPED.type();
        repair.setDisable(!stopped);
        importDb.setDisable(!stopped || !hasDb);
        exportDb.setDisable(stopped || !hasDb);

        items.add(repair);
        items.add(importDb);
        items.add(exportDb);
        items.add(ToolComponent.menuSeparator());
        items.add(editIni);
        items.add(deleteIni);
        items.add(ToolComponent.menuSeparator());
        items.add(modify);
        items.add(remove);

        VBox menu = ToolComponent.menuBox();
        menu.getChildren().addAll(items);
        closeMenu[0] = openMenu(anchor, menu);
    }

    /** 独立 Popup 以屏幕为边界，紧凑主窗口不会裁掉菜单。 */
    private Runnable openMenu(Node anchor, Region menu) {
        if (activeMenu != null) {
            activeMenu.hide();
        }
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) {
            return () -> {};
        }
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setAutoFix(true);
        popup.setConsumeAutoHidingEvents(true);
        StackPane frame = new StackPane(menu);
        frame.setPadding(new Insets(10));
        frame.getStylesheets().add(ToolComponent.appCss());
        popup.getContent().add(frame);
        frame.applyCss();
        double width = frame.prefWidth(-1);
        double height = frame.prefHeight(width);
        Rectangle2D screen = Screen.getScreensForRectangle(bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight()).stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        if (height > screen.getHeight() - 16) {
            ScrollPane scroll = new ScrollPane(menu);
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setPrefViewportHeight(screen.getHeight() - 36);
            scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
            frame.getChildren().setAll(scroll);
            height = screen.getHeight() - 16;
        }
        double x = Math.max(screen.getMinX() + 8, Math.min(bounds.getMaxX() - width + 10, screen.getMaxX() - width - 8));
        double below = bounds.getMaxY() + 2;
        double y = below + height <= screen.getMaxY() - 8 ? below : bounds.getMinY() - height - 2;
        y = Math.max(screen.getMinY() + 8, Math.min(y, screen.getMaxY() - height - 8));
        activeMenu = popup;
        popup.setOnHidden(e -> {
            if (activeMenu == popup) {
                activeMenu = null;
            }
        });
        popup.show(anchor, x, y);
        return popup::hide;
    }

    // ==================== 实例操作 ====================

    private void toggleInstance(ToolConfig.Instance instance) {
        if (app.getRunning() != null) {
            return;
        }
        if (instance.status == Status.STARTED.type()) {
            stopInstance(instance);
        } else {
            startInstance(instance);
        }
    }

    private boolean checkPath(ToolConfig.Instance instance) {
        File dir = new File(instance.path.data);
        if (!dir.exists()) {
            alert(String.format(I18n.MYSQL_PATH_ERROR.translate(lang()), instance.section.data));
            return false;
        }
        return true;
    }

    private void startInstance(ToolConfig.Instance instance) {
        if (!checkPath(instance)) {
            return;
        }
        if (service.isRunning(instance)) {
            instance.status = Status.STARTED.type();
            render();
            return;
        }
        if (service.isPortOccupied(instance)) {
            confirm(String.format(I18n.CONFIRM_BIND_START.translate(lang()), instance.section.data, instance.port.data),
                    () -> doStart(instance));
            return;
        }
        doStart(instance);
    }

    private void doStart(ToolConfig.Instance instance) {
        String label = String.format(I18n.OP_STARTING.translate(lang()), instance.section.data);
        app.startOperation(instance, Status.STARTING, Status.STARTED, label, startWithIniNotice(instance));
    }

    private void stopInstance(ToolConfig.Instance instance) {
        if (!checkPath(instance)) {
            return;
        }
        if (!service.isRunning(instance)) {
            instance.status = Status.STOPPED.type();
            render();
            return;
        }
        String label = String.format(I18n.OP_STOPPING.translate(lang()), instance.section.data);
        app.startOperation(instance, Status.STOPPING, Status.STOPPED, label, () -> {
            service.stop(instance);
            return true;
        });
    }

    private void restartInstance(ToolConfig.Instance instance) {
        if (app.getRunning() != null) {
            return;
        }
        if (!checkPath(instance)) {
            return;
        }
        String label = String.format(I18n.OP_STARTING.translate(lang()), instance.section.data);
        Callable<Boolean> start = startWithIniNotice(instance);
        app.startOperation(instance, Status.STOPPING, Status.STARTED, label, () -> {
            if (service.isRunning(instance)) {
                service.stop(instance);
            }
            return start.call();
        });
    }

    /**
     * 成功启动后清除已经应用的提示；启动期间产生的新文件变更仍保留。
     */
    private Callable<Boolean> startWithIniNotice(ToolConfig.Instance instance) {
        ToolApp.IniSession pending = app.getIni();
        if (pending != null) {
            pending.refresh();
        }
        long pendingMtime = pending == null ? 0 : pending.mtime;
        boolean pendingDeleted = pending != null && pending.deleted;
        return () -> {
            boolean started = service.start(instance);
            if (started && pending != null && pending.instance == instance) {
                Platform.runLater(() -> {
                    pending.refresh();
                    if (app.getIni() == pending && pending.mtime == pendingMtime && pending.deleted == pendingDeleted) {
                        app.setIni(null);
                    }
                });
            }
            return started;
        };
    }


    private void repairInstance(ToolConfig.Instance instance) {
        if (app.getRunning() != null || !checkPath(instance)) {
            return;
        }
        String label = String.format(I18n.OP_REPAIRING.translate(lang()), instance.section.data);
        app.startOperation(instance, Status.STOPPING, Status.STOPPED, label, () -> {
            app.addLog(String.format(I18n.REPAIR_STEP_VC.translate(lang())), false);
            ToolUtil.checkVcRuntime();
            if (service.isRunning(instance)) {
                service.stop(instance);
            }
            ToolUtil.checkAsciiPath(instance.path.data);
            service.clearBinlog(instance);
            service.clearPid(instance);
            ToolUtil.killPortProcess(Integer.parseInt(instance.port.data));
            return true;
        });
    }

    private void importInstance(ToolConfig.Instance instance) {
        if (app.getRunning() != null || !checkPath(instance)) {
            return;
        }
        File sql = new File("output_" + instance.port.data + ".sql");
        if (!sql.exists()) {
            alert(String.format(I18n.IMPORT_ERR2.translate(lang()), instance.section.data, sql.getName()));
            return;
        }
        String label = String.format(I18n.OP_IMPORTING.translate(lang()), instance.section.data);
        app.startOperation(instance, Status.STARTING, Status.STARTED, label, () -> {
            if (!service.isRunning(instance) && !service.start(instance)) {
                throw new Exception(String.format(I18n.IMPORT_ERR1.translate(lang()), instance.section.data));
            }
            service.importDb(instance, sql.getAbsolutePath());
            return true;
        });
    }

    private void exportInstance(ToolConfig.Instance instance) {
        if (app.getRunning() != null || !checkPath(instance)) {
            return;
        }
        if (instance.database == null || instance.database.data == null || instance.database.data.trim().isEmpty()) {
            alert(String.format(I18n.DATABASE_EMPTY.translate(lang()), instance.section.data));
            return;
        }
        if (!service.isRunning(instance)) {
            alert(String.format(I18n.EXPORT_NOT_RUNNING.translate(lang()), instance.section.data));
            return;
        }

        confirm(String.format(I18n.EXPORT_CONFIRM_WARNING.translate(lang()), instance.section.data), () -> {
            if (app.getRunning() != null) {
                return;
            }
            String label = String.format(I18n.OP_EXPORTING.translate(lang()), instance.section.data);
            app.startOperation(instance, Status.STARTED, Status.STARTED, label, () -> {
                if (!service.isRunning(instance)) {
                    throw new Exception(String.format(I18n.EXPORT_NOT_RUNNING.translate(lang()), instance.section.data));
                }
                service.exportDb(instance);
                return true;
            });
        });
    }

    private boolean checkPathSilent(ToolConfig.Instance instance) {
        return new File(instance.path.data).exists();
    }

    // ==================== my.ini ====================

    private void editIni(ToolConfig.Instance instance) {
        File file;
        try {
            file = service.createMyIniIfMissing(instance);
        } catch (Exception ex) {
            alert(String.format(I18n.INI_OPEN_FAILED.translate(lang()), instance.section.data) + ": " + ex.getMessage());
            return;
        }
        if (service.isRunning(instance)) {
            showIniRunningChoice(instance, file);
            return;
        }
        openIni(instance, file);
    }

    private void deleteIni(ToolConfig.Instance instance) {
        File file = new File(instance.path.data, "my.ini");
        confirm(String.format(I18n.INI_DELETE_CONFIRM.translate(lang()), instance.section.data, file.getAbsolutePath())
                + "\n\n" + iniApplyHint(instance), () -> {
            try {
                service.deleteMyIni(instance);
                app.setIni(new ToolApp.IniSession(instance, file, service.getIniEditor()));
                alert(String.format(I18n.INI_DELETE_SUCCESS.translate(lang()), instance.section.data) + "\n" + iniApplyHint(instance));
                render();
            } catch (Exception ex) {
                alert(String.format(I18n.INI_DELETE_FAILED.translate(lang()), instance.section.data) + ": " + ex.getMessage());
            }
        });
    }

    private void showIniRunningChoice(ToolConfig.Instance instance, File file) {
        VBox panel = messagePanel(String.format(I18n.INI_OPEN_RUNNING.translate(lang()), instance.section.data), 400);

        javafx.scene.control.Button cancel = ToolComponent.plainButton(I18n.CANCEL.translate(lang()));
        javafx.scene.control.Button editOnly = ToolComponent.plainButton(I18n.EDIT_ONLY.translate(lang()));
        javafx.scene.control.Button stopEdit = ToolComponent.primaryButton(I18n.STOP_AND_EDIT.translate(lang()));
        panel.getChildren().add(ToolComponent.buttonsRight(cancel, editOnly, stopEdit));
        cancel.setCancelButton(true);

        StackPane layer = ToolComponent.openSheet(root, panel, true, null);
        cancel.setOnAction(e -> ToolComponent.close(root, layer));
        editOnly.setOnAction(e -> {
            ToolComponent.close(root, layer);
            openIni(instance, file);
        });
        stopEdit.setOnAction(e -> {
            ToolComponent.close(root, layer);
            String label = String.format(I18n.OP_STOPPING.translate(lang()), instance.section.data);
            app.startOperation(instance, Status.STOPPING, Status.STOPPED, label, () -> {
                service.stop(instance);
                Platform.runLater(() -> openIni(instance, file));
                return true;
            });
        });
    }

    private void openIni(ToolConfig.Instance instance, File file) {
        String editor = service.getIniEditor();
        try {
            ToolUtil.openWithEditor(file, editor);
        } catch (Exception e) {
            app.addLog(String.format(I18n.INI_OPEN_FAILED.translate(lang()), instance.section.data) + ": " + e.getMessage(), true);
            alert(String.format(I18n.INI_OPEN_FAILED.translate(lang()), instance.section.data) + "\n" + e.getMessage());
            return;
        }
        app.setIni(new ToolApp.IniSession(instance, file, editor));
        app.addLog(String.format(I18n.INI_OPENED.translate(lang()), editorDisplayName(editor)), false);
        app.startTickerIfNeeded();
        render();
    }

    private void endIni() {
        app.setIni(null);
        render();
    }

    // ==================== 浮层：表单 / 确认 / 日志 / 设置 ====================

    private VBox messagePanel(String message, double width) {
        VBox panel = ToolComponent.panel(width);
        panel.setPrefWidth(width);
        panel.setSpacing(18);
        panel.setStyle(panel.getStyle() + "-fx-padding: 20;"
                + " -fx-effect: dropshadow(gaussian, rgba(16,24,40,0.16), 20, 0.12, 0, 6);");
        Label text = ToolComponent.label(message, 13, ThemeColor.TEXT);
        text.setStyle(text.getStyle() + "-fx-font-weight: normal;");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().add(text);
        return panel;
    }

    private void alert(String message) {
        VBox panel = messagePanel(message, 360);
        Button ok = ToolComponent.primaryButton(I18n.OK.translate(lang()));
        panel.getChildren().add(ToolComponent.buttonsRight(ok));
        StackPane layer = ToolComponent.openSheet(root, panel, true, null);
        ok.setOnAction(e -> ToolComponent.close(root, layer));
    }

    private void confirm(String message, Runnable onOk) {
        VBox panel = messagePanel(message, 360);
        Button cancel = ToolComponent.plainButton(I18n.CANCEL.translate(lang()));
        Button ok = ToolComponent.primaryButton(I18n.OK.translate(lang()));
        for (Button button : List.of(cancel, ok)) {
            button.setMinSize(64, 28);
            button.setPrefSize(64, 28);
            button.setMaxSize(64, 28);
            button.setStyle(button.getStyle()
                    + "-fx-min-height: 28; -fx-pref-height: 28; -fx-max-height: 28; -fx-padding: 0 12;");
        }
        String okStyle = ok.getStyle();
        ok.setOnMouseEntered(e -> ok.setStyle(okStyle + "-fx-opacity: 0.88;"));
        ok.setOnMouseExited(e -> ok.setStyle(okStyle));
        cancel.setCancelButton(true);
        ok.setDefaultButton(false);
        panel.getChildren().add(ToolComponent.buttonsRight(cancel, ok));
        StackPane layer = ToolComponent.openSheet(root, panel, true, null);
        cancel.setOnAction(e -> ToolComponent.close(root, layer));
        ok.setOnAction(e -> {
            ToolComponent.close(root, layer);
            onOk.run();
        });
        Platform.runLater(cancel::requestFocus);
    }

    private void removeInstance(ToolConfig.Instance instance) {
        confirm(String.format(I18n.CONFIRM_DELETE.translate(lang()), instance.section.data), () -> {
            if (!service.getConfig().instances.remove(instance)) {
                return;
            }
            service.saveConfig();
            app.addLog(I18n.DELETE_INSTANCE.translate(lang()) + ": " + instance.section.data, false);
            render();
        });
    }

    private void showEditDialog(ToolConfig.Instance existing) {
        boolean isEdit = existing != null;
        VBox panel = ToolComponent.panel(ToolCommon.DIALOG_WIDTH);
        final StackPane[] layerRef = new StackPane[1];
        HBox header = ToolComponent.panelHeader(
                isEdit ? I18n.MODIFY_INSTANCE.translate(lang()) : I18n.ADD_INSTANCE_ROW.translate(lang()),
                isEdit ? existing.section.data : null,
                () -> {
                    if (layerRef[0] != null) {
                        ToolComponent.close(root, layerRef[0]);
                    }
                });

        TextField nameField = ToolComponent.input(isEdit ? existing.section.data : "");
        TextField portField = ToolComponent.input(isEdit ? existing.port.data : service.getDefaultPort());
        TextField pathField = ToolComponent.input(isEdit ? existing.path.data : "");
        TextField userField = ToolComponent.input(isEdit ? existing.username.data : "root");
        TextField passField = ToolComponent.input(isEdit ? existing.password.data : "");
        TextField dbField = ToolComponent.input(isEdit ? existing.database.data : "");

        HBox firstRow = new HBox(14,
                ToolComponent.field(I18n.INSTANCE_NAME_LABEL.translate(lang()), true, nameField, 220),
                ToolComponent.field(I18n.INSTANCE_PORT_LABEL.translate(lang()), true, portField, 120));
        HBox.setHgrow(firstRow.getChildren().getFirst(), Priority.ALWAYS);

        javafx.scene.control.Button pick = ToolComponent.plainButton(
                I18n.CHOOSE_FOLDER.translate(lang()));
        pick.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(I18n.CHOOSE_FOLDER.translate(lang()));
            File initial = existingDirectory(pathField.getText());
            if (initial != null) {
                chooser.setInitialDirectory(initial);
            }
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                pathField.setText(dir.getAbsolutePath());
            }
        });
        HBox pathRow = new HBox(8, pathField, pick);
        pathRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        HBox thirdRow = new HBox(14,
                ToolComponent.field(I18n.INSTANCE_USERNAME_LABEL.translate(lang()), true, userField, 150),
                ToolComponent.field(I18n.INSTANCE_PASSWORD_LABEL.translate(lang()), false, passField, 150),
                ToolComponent.field(I18n.INSTANCE_DATABASE_LABEL.translate(lang()), false, dbField, 140));
        for (int i = 0; i < 3; i++) {
            HBox.setHgrow(thirdRow.getChildren().get(i), Priority.ALWAYS);
        }

        VBox body = new VBox(15, firstRow,
                ToolComponent.field(I18n.INSTANCE_PATH_LABEL.translate(lang()), true, pathRow, 0),
                thirdRow);
        VBox.setMargin(body, new Insets(20, 0, 0, 0));

        javafx.scene.control.Button cancel = ToolComponent.plainButton(I18n.CANCEL.translate(lang()));
        javafx.scene.control.Button save = ToolComponent.primaryButton(I18n.SAVE.translate(lang()));
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(18, 0, 0, 0));
        footer.getChildren().addAll(ToolComponent.hspacer(), cancel, save);

        panel.getChildren().addAll(header, body, footer);
        StackPane layer = ToolComponent.openSheet(root, panel, false, null);
        layerRef[0] = layer;
        cancel.setOnAction(e -> ToolComponent.close(root, layer));

        save.setOnAction(e -> {
            String name = nameField.getText().trim();
            String port = portField.getText().trim();
            String path = pathField.getText().trim();
            String user = userField.getText().trim();
            if (name.isEmpty() || port.isEmpty() || path.isEmpty() || user.isEmpty()) {
                alert(I18n.FIELD_REQUIRED.translate(lang()));
                return;
            }
            int portValue;
            try {
                portValue = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                alert(I18n.PORT_INVALID.translate(lang()));
                return;
            }
            if (portValue < 1 || portValue > 65535) {
                alert(I18n.PORT_INVALID.translate(lang()));
                return;
            }
            List<ToolConfig.Instance> instances = service.getConfig().instances;
            if (instances != null) {
                for (ToolConfig.Instance other : instances) {
                    if (other != existing && port.equals(other.port.data)) {
                        alert(String.format(I18n.PORT_DUPLICATE.translate(lang()), port, other.section.data));
                        return;
                    }
                }
            }
            if (isEdit) {
                existing.section.data = name;
                existing.path.data = path;
                existing.username.data = user;
                existing.password.data = passField.getText().trim();
                existing.port.data = port;
                existing.database.data = dbField.getText().trim();
            } else {
                ToolConfig.Instance instance = new ToolConfig.Instance();
                int sort = service.getNextInstanceSort();
                instance.section = new ToolConfig.Info<>(name, sort, I18n.NAME_COMMAND.comment());
                instance.path = new ToolConfig.Info<>(path, 0, I18n.PATH_COMMAND.comment());
                instance.username = new ToolConfig.Info<>(user, 1, I18n.USERNAME_COMMAND.comment());
                instance.password = new ToolConfig.Info<>(passField.getText().trim(), 2, I18n.PASSWORD_COMMAND.comment());
                instance.port = new ToolConfig.Info<>(port, 3, I18n.PORT_COMMAND.comment());
                instance.database = new ToolConfig.Info<>(dbField.getText().trim(), 4, I18n.DATABASE_COMMAND.comment());
                service.getConfig().instances.add(instance);
            }
            service.saveConfig();
            ToolComponent.close(root, layer);
            render();
        });
    }


    /**
     * 目录选择器的初始目录：当前路径存在就用它，否则退到父目录，再不行返回 null。
     */
    private File existingDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        File dir = new File(path.trim());
        if (dir.isDirectory()) {
            return dir;
        }
        File parent = dir.getParentFile();
        while (parent != null && !parent.isDirectory()) {
            parent = parent.getParentFile();
        }
        return parent != null && parent.isDirectory() ? parent : null;
    }

    private void showSettingsSheet() {
        new SettingsSheet().show();
    }

    /**
     * 设置拥有独立生命周期；切换只更新原有控件，不销毁浮层或替换 Scene。
     */
    private final class SettingsSheet {
        private final VBox panel = new VBox();
        private final Label title = new Label();
        private final Label hint = new Label();
        private final Button done = ToolComponent.primaryButton("");
        private final List<HBox> rows = new ArrayList<>();
        private final List<Label> labels = new ArrayList<>();
        private final I18n[] captions = {
                I18n.THEME_LABEL, I18n.ACCENT, I18n.LANGUAGE_LABEL, I18n.OPEN_INI_WITH
        };
        private final I18n[] colorNames = {
                I18n.ACCENT_BLUE, I18n.ACCENT_GREEN, I18n.ACCENT_PURPLE, I18n.ACCENT_SLATE, I18n.ACCENT_ORANGE
        };
        private final String[] palette = {"#1677ff", "#16a34a", "#7c3aed", "#475569", "#ea580c"};
        private final SettingsPicker theme;
        private final SettingsPicker colors;
        private final SettingsPicker language;
        private final SettingsPicker editor;
        private SequentialTransition hintAnimation;
        private StackPane layer;

        private SettingsSheet() {
            theme = new SettingsPicker(2, service.isDark() ? 1 : 0,
                    (index, selected) -> ToolComponent.strokeIcon(
                            index == 0 ? ToolCommon.LINE_SUN : ToolCommon.LINE_MOON,
                            16, selected ? ThemeColor.BLUE : ThemeColor.TEXT_2), index -> {
                if ((index == 1) != service.isDark()) {
                    service.changeTheme();
                }
                apply((index == 1 ? I18n.DARK : I18n.LIGHT).translate(lang()), true);
            });
            int accent = 0;
            for (int i = 0; i < ACCENTS.length; i++) {
                if (ACCENTS[i].equals(service.getAccent())) {
                    accent = i;
                }
            }
            colors = new SettingsPicker(ACCENTS.length, accent, (index, selected) -> {
                Region dot = new Region();
                dot.setMinSize(10, 10);
                dot.setPrefSize(10, 10);
                dot.setMaxSize(10, 10);
                dot.setStyle("-fx-background-color: " + palette[index] + "; -fx-background-radius: 5;");
                return dot;
            }, index -> {
                service.setAccent(ACCENTS[index]);
                apply(colorNames[index].translate(lang()), true);
            });
            language = new SettingsPicker(2, "en-US".equals(lang()) ? 1 : 0,
                    (index, selected) -> ToolComponent.strokeIcon(
                            index == 0 ? ToolCommon.LINE_ZH : ToolCommon.LINE_EN,
                            16, selected ? ThemeColor.BLUE : ThemeColor.TEXT_2), index -> {
                String wanted = index == 1 ? "en-US" : "zh-CN";
                if (!wanted.equals(lang())) {
                    service.changeLanguage();
                }
                apply(index == 1 ? "English" : "中文", true);
            });
            editor = new SettingsPicker(2, "notepad".equalsIgnoreCase(service.getIniEditor()) ? 1 : 0,
                    (index, selected) -> {
                        Region glyph = index == 0 ? ToolComponent.windowsIcon(16) : ToolComponent.notepadIcon(16);
                        glyph.setOpacity(selected ? 1 : 0.72);
                        return glyph;
                    }, index -> {
                service.setIniEditor(index == 1 ? "notepad" : "default");
                apply((index == 1 ? I18n.NOTEPAD : I18n.SYSTEM_DEFAULT).translate(lang()), false);
            });

            panel.setMinWidth(372);
            panel.setPrefWidth(372);
            panel.setMaxWidth(372);
            panel.setMaxHeight(Region.USE_PREF_SIZE);
            HBox head = new HBox(12, title, ToolComponent.hspacer(), hint);
            head.setAlignment(Pos.CENTER_LEFT);
            head.setPadding(new Insets(18, 24, 12, 24));
            hint.setOpacity(0);
            hint.setMinWidth(0);
            VBox body = new VBox();
            body.setPadding(new Insets(0, 24, 0, 24));
            SettingsPicker[] controls = {theme, colors, language, editor};
            for (SettingsPicker control : controls) {
                Label label = new Label();
                HBox row = new HBox(16, label, ToolComponent.hspacer(), control);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMinHeight(42);
                row.setPrefHeight(42);
                rows.add(row);
                labels.add(label);
                body.getChildren().add(row);
            }
            HBox footer = new HBox(done);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(12, 24, 16, 24));
            panel.getChildren().addAll(head, body, footer);
            done.setOnAction(e -> {
                stopHint();
                ToolComponent.close(root, layer);
            });
            refresh();
        }

        private void show() {
            layer = ToolComponent.openSheet(root, panel, true, this::stopHint);
            refreshScrim();
        }

        private void apply(String message, boolean updateShell) {
            service.saveConfig();
            if (updateShell) {
                refreshAppearance();
            }
            refresh();
            flashHint(message);
        }

        private void refresh() {
            panel.setStyle("-fx-background-color: " + ThemeColor.PANEL.color()
                    + "; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 1;"
                    + " -fx-border-color: " + ThemeColor.PANEL_BORDER.color()
                    + "; -fx-effect: dropshadow(gaussian, rgba(16,24,40,0.16), 24, 0.12, 0, 8);");
            title.setText(I18n.SETTINGS.translate(lang()));
            title.setStyle("-fx-text-fill: " + ThemeColor.TEXT.color() + "; -fx-font-size: 16px;");
            hint.setStyle("-fx-text-fill: " + ThemeColor.TEXT_2.color() + "; -fx-font-size: 11px;");
            for (int i = 0; i < rows.size(); i++) {
                labels.get(i).setText(captions[i].translate(lang()));
                labels.get(i).setStyle("-fx-text-fill: " + ThemeColor.TEXT.color() + "; -fx-font-size: 13px;");
                rows.get(i).setStyle(i == rows.size() - 1 ? ""
                        : "-fx-border-color: transparent transparent " + ThemeColor.LINE_2.color()
                        + " transparent; -fx-border-width: 0 0 1 0;");
            }
            theme.refresh(I18n.LIGHT.translate(lang()), I18n.DARK.translate(lang()));
            String[] names = new String[colorNames.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = colorNames[i].translate(lang());
            }
            colors.refresh(names);
            language.refresh("中文 (Zh)", "English (En)");
            editor.refresh(I18n.SYSTEM_DEFAULT.translate(lang()), I18n.NOTEPAD.translate(lang()));
            done.setText(I18n.DONE.translate(lang()));
            // 同一个按钮原位更新颜色，悬停时也不会恢复旧主色。
            String doneStyle = "-fx-background-color: " + ThemeColor.BLUE.color()
                    + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 0 14;"
                    + " -fx-background-radius: 7; -fx-min-height: 28; -fx-pref-height: 28;"
                    + " -fx-max-height: 28; -fx-cursor: hand;";
            done.setStyle(doneStyle);
            done.setOnMouseEntered(e -> done.setStyle(doneStyle + "-fx-opacity: 0.88;"));
            done.setOnMouseExited(e -> done.setStyle(doneStyle));
            refreshScrim();
        }

        private void refreshScrim() {
            if (layer != null) {
                layer.getChildren().getFirst().setStyle("-fx-background-color: rgba(16,24,40,"
                        + (service.isDark() ? "0.48" : "0.22") + ");");
            }
        }

        private void stopHint() {
            if (hintAnimation != null) {
                hintAnimation.stop();
            }
        }

        private void flashHint(String message) {
            stopHint();
            hint.setText(message);
            hint.setOpacity(0);
            FadeTransition in = new FadeTransition(Duration.millis(120), hint);
            in.setToValue(1);
            PauseTransition hold = new PauseTransition(Duration.millis(950));
            FadeTransition out = new FadeTransition(Duration.millis(220), hint);
            out.setToValue(0);
            hintAnimation = new SequentialTransition(in, hold, out);
            hintAnimation.play();
        }
    }

    // ==================== 工具 ====================

    private String duration(long millis) {
        long minutes = millis / 60000;
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " 小时 " + (minutes % 60) + " 分";
        }
        return (hours / 24) + " 天 " + (hours % 24) + " 小时";
    }

    public void showToast(String message) {
        app.addLog(message, false);
        render();
    }
}
