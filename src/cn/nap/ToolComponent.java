package cn.nap;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.nap.ToolCommon.ThemeColor;
import static cn.nap.ToolCommon.TipType;
import static cn.nap.ToolCommon.I18n;

public class ToolComponent {

    public static Button button(String text) {
        return button(text, null);
    }

    public static Button button(String text, String svg) {
        Button button = new Button(text);
        final String color = ThemeColor.FONT_BG.color();
        final String hoverColor = ThemeColor.FONT_HOVER.color();
        final String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", color, color);
        final String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", hoverColor, hoverColor);
        button.setStyle(exitStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, color));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(enterStyle);
                button.setGraphic(createIconNode(svg, hoverColor));
            });
            button.setOnMouseExited((e) -> {
                button.setStyle(exitStyle);
                button.setGraphic(createIconNode(svg, color));
            });
        } else {
            button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
            button.setOnMouseExited((e) -> button.setStyle(exitStyle));
        }
        return button;
    }

    public static Button primaryButton(String text) {
        return primaryButton(text, null);
    }

    public static Button primaryButton(String text, String svg) {
        Button button = new Button(text);
        final String filledStyle = String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY.color(), ThemeColor.PRIMARY.color());
        final String hoverStyle = String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY_HOVER.color(), ThemeColor.PRIMARY_HOVER.color());
        button.setStyle(filledStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, "#fff"));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(hoverStyle);
                button.setGraphic(createIconNode(svg, "#fff"));
            });
            button.setOnMouseExited((e) -> {
                button.setStyle(filledStyle);
                button.setGraphic(createIconNode(svg, "#fff"));
            });
        } else {
            button.setOnMouseEntered((e) -> button.setStyle(hoverStyle));
            button.setOnMouseExited((e) -> button.setStyle(filledStyle));
        }
        return button;
    }

    public static Button startButton(String text) {
        return startButton(text, null);
    }

    public static Button startButton(String text, String svg) {
        Button button = new Button(text);
        final String outlineStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY.color(), ThemeColor.PRIMARY.color());
        final String filledStyle = String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY.color(), ThemeColor.PRIMARY.color());
        button.setStyle(outlineStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, ThemeColor.PRIMARY.color()));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(filledStyle);
                button.setGraphic(createIconNode(svg, "#fff"));
            });
            button.setOnMouseExited((e) -> {
                button.setStyle(outlineStyle);
                button.setGraphic(createIconNode(svg, ThemeColor.PRIMARY.color()));
            });
        } else {
            button.setOnMouseEntered((e) -> button.setStyle(filledStyle));
            button.setOnMouseExited((e) -> button.setStyle(outlineStyle));
        }
        return button;
    }

    public static Button stopButton(String text) {
        return stopButton(text, null);
    }

    public static Button stopButton(String text, String svg) {
        Button button = new Button(text);
        final String color = ThemeColor.FONT_BG.color();
        final String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", color, color);
        final String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.DANGER.color(), ThemeColor.DANGER.color());
        button.setStyle(exitStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, color));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(enterStyle);
                button.setGraphic(createIconNode(svg, ThemeColor.DANGER.color()));
            });
            button.setOnMouseExited((e) -> {
                button.setStyle(exitStyle);
                button.setGraphic(createIconNode(svg, color));
            });
        } else {
            button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
            button.setOnMouseExited((e) -> button.setStyle(exitStyle));
        }
        return button;
    }

    private static Region createIconNode(String svg, String color) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(svg);
        Region region = new Region();
        region.setShape(svgPath);
        region.setStyle(String.format("-fx-background-color: %s;", color));
        region.setPrefSize(12, 12);
        region.setMaxSize(12, 12);
        region.setMinSize(12, 12);
        return region;
    }

    public static Button themeIcon() {
        // 黑色展示太阳，白色展示月亮
        final String svg = ToolService.getInstance().isDark() ? ToolCommon.SVG_DARK : ToolCommon.SVG_LIGHT;
        return icon(svg);
    }

    public static Button icon(String svg) {
        Button button = new Button();
        button.setFont(new Font(14));
        button.setStyle("-fx-background-color: transparent;");
        button.setGraphic(iconGraphic(false, button.getFont().getSize(), svg));
        button.setOnMouseEntered((e) -> button.setGraphic(iconGraphic(true, button.getFont().getSize(), svg)));
        button.setOnMouseExited((e) -> button.setGraphic(iconGraphic(false, button.getFont().getSize(), svg)));
        return button;
    }

    private static Region iconGraphic(boolean hover, double size, String svg) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(svg);
        Region region = new Region();
        region.setShape(svgPath);
        region.setStyle(String.format("-fx-background-color: %s;", hover ? ThemeColor.FONT_HOVER.color() : ThemeColor.FONT_BG.color()));
        region.setPrefSize(size, size);
        region.setMaxSize(size, size);
        return region;
    }

    public static Region line() {
        Region region = new Region();
        region.setMaxWidth(Double.MAX_VALUE);
        region.setPrefHeight(0);
        region.setMaxHeight(0);
        region.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 0.5 0 0 0;", ThemeColor.BUTTON_BORDER.color()));
        return region;
    }

    public static ToggleButton menu(ToggleGroup group, String text) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        addMenuNormalStyle(button);
        button.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                addMenuToggleStyle(button);
            } else {
                addMenuNormalStyle(button);
            }
        });
        return button;
    }

    private static void addMenuToggleStyle(ToggleButton button) {
        button.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: transparent;-fx-background-radius: 8px;-fx-font-size: 14px;-fx-font-weight: bold;", ThemeColor.PRIMARY.color()));
        button.setOnMouseEntered(null);
        button.setOnMouseExited(null);
    }

    private static void addMenuNormalStyle(ToggleButton button) {
        String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 8px;-fx-font-size: 14px;", ThemeColor.FONT_BG.color());
        String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 8px;-fx-font-size: 14px;", ThemeColor.FONT_HOVER.color());
        button.setStyle(exitStyle);
        button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
        button.setOnMouseExited((e) -> button.setStyle(exitStyle));
    }

    public static ScrollPane scrollPane() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.skinProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }

            Node viewport = scrollPane.lookup(".viewport");
            if (viewport == null) {
                return;
            }
            viewport.setStyle("-fx-background-color: transparent;");

            ScrollBar horizontalScrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:horizontal");
            final String exitStyle = "-fx-background-color: transparent;-fx-pref-height: 10px;-fx-max-height: 10px;";
            final String enterStyle = "-fx-background-color: transparent;-fx-pref-height: 14px;-fx-max-height: 14px;";
            if (horizontalScrollBar != null) {
                horizontalScrollBar.setStyle(exitStyle);
                changeThumb(horizontalScrollBar);
                horizontalScrollBar.setOnMouseEntered((e) -> horizontalScrollBar.setStyle(enterStyle));
                horizontalScrollBar.setOnMouseExited((e) -> horizontalScrollBar.setStyle(exitStyle));
            }
            ScrollBar verticalScrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
            if (verticalScrollBar != null) {
                verticalScrollBar.setStyle(exitStyle);
                changeThumb(verticalScrollBar);
                verticalScrollBar.setOnMouseEntered((e) -> verticalScrollBar.setStyle(enterStyle));
                verticalScrollBar.setOnMouseExited((e) -> verticalScrollBar.setStyle(exitStyle));
            }
        });
        scrollPane.setStyle(String.format("-fx-background-color: %s;-fx-border-color: %s;-fx-background-radius: 8px;-fx-border-radius: 8px;", ThemeColor.SCROLL_BG.color(), ThemeColor.SCROLL_BG.color()));
        return scrollPane;
    }

    private static void changeThumb(ScrollBar scrollBar) {
        scrollBar.skinProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            Node thumb = scrollBar.lookup(".thumb");
            if (thumb == null) {
                return;
            }
            thumb.setStyle("-fx-background-color: #B0B0B0;-fx-background-insets: 2px;-fx-background-radius: 5px;");
        });
    }

    public static Label label(String text) {
        Label label = new Label(text);
        label.setStyle(String.format("-fx-text-fill: %s;-fx-wrap-text: true;", ThemeColor.FONT_BG.color()));
        return label;
    }

    public static HBox statusWithDot(String text, String dotColor) {
        // 实心小圆点
        Circle dot = new Circle(3);
        dot.setFill(Color.web(dotColor));

        // 发光阴影并脉动
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(dotColor));
        glow.setRadius(4);
        glow.setSpread(0.3);

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 1),
                        new KeyValue(glow.spreadProperty(), 0.1)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(glow.radiusProperty(), 5),
                        new KeyValue(glow.spreadProperty(), 0.4)),
                new KeyFrame(Duration.millis(2400),
                        new KeyValue(glow.radiusProperty(), 1),
                        new KeyValue(glow.spreadProperty(), 0.1))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
        dot.setEffect(glow);

        Label textLabel = new Label(text);
        textLabel.setStyle(String.format("-fx-text-fill: %s;", dotColor));

        HBox box = new HBox(6, dot, textLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox instanceInfo(String name, List<Node> statusList, List<Node> operateList) {
        // 上排：名称 + 状态
        Label nameLabel = label(name);
        nameLabel.setStyle(String.format("-fx-text-fill: %s;-fx-font-size: 14px;-fx-font-weight: bold;", ThemeColor.FONT_HOVER.color()));
        HBox topRow = new HBox(10, nameLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.getChildren().addAll(statusList);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(spacer, statusBox);

        // 下排：按钮
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        bottomRow.getChildren().addAll(operateList);

        VBox card = new VBox(10, topRow, bottomRow);
        final String exitStyle = String.format("-fx-background-color: %s;-fx-padding: 15px;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.CARD_BG.color(), ThemeColor.BUTTON_BORDER.color());
        final String enterStyle = String.format("-fx-background-color: %s;-fx-padding: 15px;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.CARD_BG.color(), ThemeColor.FONT_HOVER.color());
        card.setStyle(exitStyle);
        card.setOnMouseEntered((e) -> card.setStyle(enterStyle));
        card.setOnMouseExited((e) -> card.setStyle(exitStyle));
        return card;
    }

    public static void info(Stage primaryStage, StackPane root, String text) {
        tip(TipType.INFO, primaryStage, root, text);
    }

    public static void warning(Stage primaryStage, StackPane root, String text) {
        tip(TipType.WARNING, primaryStage, root, text);
    }

    public static void error(Stage primaryStage, StackPane root, String text) {
        tip(TipType.ERROR, primaryStage, root, text);
    }

    private static void tip(TipType type, Stage primaryStage, StackPane root, String text) {
        BorderPane modalRoot = new BorderPane();
        Stage stage = createModalStage(primaryStage, modalRoot, true);
        Region mask = mask(root);

        SVGPath svgPath = new SVGPath();
        svgPath.setContent(type.svg());
        Region icon = new Region();
        icon.setShape(svgPath);
        icon.setStyle(String.format("-fx-background-color: %s;", type.color()));
        icon.setPrefSize(16, 16);
        icon.setMaxSize(16, 16);
        icon.setMinSize(16, 16);

        Label label = label(text);
        label.setStyle(String.format("-fx-text-fill: %s;-fx-wrap-text: true;-fx-font-size: 13px;", ThemeColor.FONT_BG.color()));

        HBox center = new HBox(10, icon, label);
        center.setAlignment(Pos.CENTER_LEFT);
        center.setPadding(new Insets(20, 24, 12, 24));
        modalRoot.setCenter(center);

        Button close = button(I18n.CLOSE.translate(ToolService.getInstance().getLanguage()));
        VBox bottom = commonBottom(close);

        Runnable runnable = () -> {
            root.getChildren().remove(mask);
            stage.close();
        };
        mask.setOnMouseClicked(e -> runnable.run());
        close.setOnAction(e -> runnable.run());

        modalRoot.setBottom(bottom);
        stage.sizeToScene();
        stage.show();
    }

    public static boolean confirm(Stage primaryStage, StackPane root, String text) {
        return confirm(primaryStage, root, text, TipType.INFO);
    }

    public static boolean confirm(Stage primaryStage, StackPane root, String text, TipType tipType) {
        BorderPane modalRoot = new BorderPane();
        Stage stage = createModalStage(primaryStage, modalRoot, true);
        Region mask = mask(root);

        SVGPath svgPath = new SVGPath();
        svgPath.setContent(tipType.svg());
        Region icon = new Region();
        icon.setShape(svgPath);
        icon.setStyle(String.format("-fx-background-color: %s;", tipType.color()));
        icon.setPrefSize(16, 16);
        icon.setMaxSize(16, 16);
        icon.setMinSize(16, 16);

        Label label = label(text);
        label.setStyle(String.format("-fx-text-fill: %s;-fx-wrap-text: true;-fx-font-size: 13px;", ThemeColor.FONT_BG.color()));

        HBox center = new HBox(10, icon, label);
        center.setAlignment(Pos.CENTER_LEFT);
        center.setPadding(new Insets(20, 24, 12, 24));
        modalRoot.setCenter(center);

        Button cancel = button(I18n.CANCEL.translate(ToolService.getInstance().getLanguage()));
        Button ok = primaryButton(I18n.OK.translate(ToolService.getInstance().getLanguage()));
        VBox bottom = commonBottom(cancel, ok);

        AtomicBoolean result = new AtomicBoolean(false);
        Runnable runnable = () -> {
            root.getChildren().remove(mask);
            stage.close();
        };
        mask.setOnMouseClicked(e -> runnable.run());
        cancel.setOnAction(e -> runnable.run());
        ok.setOnAction(e -> {
            result.set(true);
            runnable.run();
        });

        modalRoot.setBottom(bottom);
        stage.sizeToScene();
        stage.showAndWait();
        return result.get();
    }

    public static Stage createModalStage(Stage primaryStage, Parent modalRoot, boolean maskClickable) {
        Stage stage = new Stage();
        modalRoot.setStyle(String.format("-fx-background-color: %s;-fx-background-radius: 8px;-fx-border-radius: 8px", ThemeColor.ROOT_BG.color()));
        if (modalRoot instanceof Region) {
            Region region = ((Region) modalRoot);
            region.setMinSize(ToolCommon.MIN_MODAL_WIDTH, ToolCommon.MIN_MODAL_HEIGHT);
            region.setMaxSize(ToolCommon.MAX_MODAL_WIDTH, ToolCommon.MAX_MODAL_HEIGHT);
        }
        Scene scene = new Scene(modalRoot);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initModality(maskClickable ? Modality.NONE : Modality.WINDOW_MODAL);
        stage.initOwner(primaryStage);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setOnShown(e -> {
            centerOnPrimary(primaryStage, stage);
            ChangeListener<Number> l = (obs, o, n) -> centerOnPrimary(primaryStage, stage);
            primaryStage.xProperty().addListener(l);
            primaryStage.yProperty().addListener(l);
            stage.setOnHidden(he -> {
                primaryStage.xProperty().removeListener(l);
                primaryStage.yProperty().removeListener(l);
            });
        });
        return stage;
    }

    public static Region mask(StackPane root) {
        Region mask = new Region();
        mask.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        root.getChildren().add(mask);
        return mask;
    }

    private static void centerOnPrimary(Stage primaryStage, Stage stage) {
        stage.setX(primaryStage.getX() + (primaryStage.getWidth() - stage.getWidth()) / 2);
        stage.setY(primaryStage.getY() + (primaryStage.getHeight() - stage.getHeight()) / 2);
    }

    public static VBox commonBottom(Button... buttons) {
        HBox hBox = new HBox(buttons);
        hBox.setStyle("-fx-alignment: center_right;-fx-spacing: 10px;-fx-padding: 0 15 0 15;");

        VBox vbox = new VBox(line(), hBox);
        vbox.setStyle("-fx-spacing: 8px;-fx-padding: 10 0 8 0;");
        return vbox;
    }

    public interface RepairAction {
        void run() throws Exception;
    }

    public static class RepairStep {
        public final String desc;
        public final RepairAction action;
        public Circle dot;
        public Label descLabel;

        public RepairStep(String desc, RepairAction action) {
            this.desc = desc;
            this.action = action;
        }
    }

    public static void repairProgress(Stage primaryStage, StackPane root, List<RepairStep> steps, Runnable onClose) {
        BorderPane modalRoot = new BorderPane();
        Stage stage = createModalStage(primaryStage, modalRoot, false);
        Region mask = mask(root);
        modalRoot.setPrefWidth(ToolCommon.MIN_PROGRESS_WIDTH);

        VBox stepBox = new VBox(8);
        stepBox.setPadding(new Insets(16, 20, 8, 20));

        for (RepairStep step : steps) {
            step.dot = new Circle(4);
            step.dot.setFill(Color.web(ThemeColor.FONT_BG.color()));

            step.descLabel = new Label(step.desc);
            step.descLabel.setWrapText(true);
            step.descLabel.maxWidthProperty().bind(stepBox.widthProperty().subtract(32));
            step.descLabel.setStyle(String.format("-fx-text-fill: %s;", ThemeColor.FONT_BG.color()));
            HBox.setHgrow(step.descLabel, Priority.ALWAYS);

            HBox row = new HBox(8, step.dot, step.descLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            stepBox.getChildren().add(row);
        }

        ScrollPane stepScroll = scrollPane();
        stepScroll.setContent(stepBox);
        modalRoot.setCenter(stepScroll);

        Button closeBtn = button(I18n.CLOSE.translate(ToolService.getInstance().getLanguage()));
        closeBtn.setDisable(true);
        modalRoot.setBottom(commonBottom(closeBtn));

        Runnable close = () -> {
            root.getChildren().remove(mask);
            stage.close();
        };
        closeBtn.setOnAction(e -> {
            close.run();
            if (onClose != null) onClose.run();
        });

        stage.setTitle(I18n.REPAIR.translate(ToolService.getInstance().getLanguage()));
        stage.show();
        Platform.runLater(() -> {
            stage.sizeToScene();
            centerOnPrimary(primaryStage, stage);
        });

        new Thread(() -> {
            for (RepairStep step : steps) {
                try {
                    step.action.run();
                    Platform.runLater(() -> {
                        step.dot.setFill(Color.web(ThemeColor.SUCCESS.color()));
                        DropShadow glow = new DropShadow();
                        glow.setColor(Color.web(ThemeColor.SUCCESS.color()));
                        glow.setRadius(4);
                        glow.setSpread(0.3);
                        step.dot.setEffect(glow);
                        step.descLabel.setText(step.desc + "  " + I18n.STEP_DONE.translate(ToolService.getInstance().getLanguage()));
                    });
                } catch (Exception e) {
                    String msg = e.getMessage();
                    Platform.runLater(() -> {
                        step.dot.setFill(Color.web(ThemeColor.DANGER.color()));
                        DropShadow glow = new DropShadow();
                        glow.setColor(Color.web(ThemeColor.DANGER.color()));
                        glow.setRadius(4);
                        glow.setSpread(0.3);
                        step.dot.setEffect(glow);
                        step.descLabel.setText(step.desc + "  " + (msg != null ? msg : I18n.STEP_FAIL.translate(ToolService.getInstance().getLanguage())));
                        closeBtn.setDisable(false);
                    });
                    return;
                }
            }
            Platform.runLater(() -> closeBtn.setDisable(false));
        }).start();
    }
}
