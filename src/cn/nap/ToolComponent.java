package cn.nap;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import static cn.nap.ToolCommon.ThemeColor;

/** v2 组件工厂：文字按钮、图标、浮层、菜单、分段控件。 */
public class ToolComponent {

    public static Label label(String text, double size, ThemeColor color) {
        Label label = new Label(text);
        label.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: %.1fpx;", color.color(), size));
        return label;
    }

    public static Label label(String text, double size, ThemeColor color, boolean bold) {
        Label label = label(text, size, color);
        if (bold) {
            label.setStyle(label.getStyle() + " -fx-font-weight: 500;");
        }
        return label;
    }

    /**
     * 悬停只追加/移除背景色，绝不重建整条样式 —— 否则调用方追加的字号会被冲掉。
     * 基准样式在第一次悬停时才抓取，保证拿到调用方最终设置过的值。
     */
    public static void installHover(Region node, boolean hoverable) {
        if (!hoverable) {
            return;
        }
        final String[] base = new String[1];
        node.setOnMouseEntered(e -> {
            if (node.isDisable()) {
                return;
            }
            if (base[0] == null) {
                base[0] = node.getStyle() == null ? "" : node.getStyle();
            }
            node.setStyle(base[0] + "-fx-background-color: " + ThemeColor.HOVER_2.color() + ";");
        });
        node.setOnMouseExited(e -> {
            if (base[0] != null) {
                node.setStyle(base[0]);
            }
        });
    }

    /** 无边框文字按钮。 */
    public static Button textButton(String text, ThemeColor color) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-size: 13px;"
                        + " -fx-padding: 0 12 0 12; -fx-background-radius: 8; -fx-cursor: hand;"
                        + " -fx-min-height: 28; -fx-pref-height: 28; -fx-graphic-text-gap: 6;",
                color.color()));
        installHover(button, true);
        return button;
    }

    public static Button iconButton(String svg, double size, ThemeColor color, double width, double height) {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.setGraphic(strokeIcon(svg, size, color));
        button.setStyle(String.format(
                "-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 7; -fx-cursor: hand;"
                        + " -fx-min-width: %.0f; -fx-pref-width: %.0f; -fx-max-width: %.0f;"
                        + " -fx-min-height: %.0f; -fx-pref-height: %.0f; -fx-max-height: %.0f;",
                width, width, width, height, height, height));
        installHover(button, true);
        return button;
    }

    /** 固定 24×24 画布缩放，保留图标留白；不随每条路径的包围盒重新居中或拉伸。 */
    public static StackPane strokeIcon(String svg, double size, ThemeColor color) {
        return svgCanvas(size, svgPath(svg, Color.web(color.color()), false));
    }

    /** Windows 标识保留四个实心窗格，使用品牌蓝，选中状态由外层滑块表达。 */
    public static StackPane windowsIcon(double size) {
        return svgCanvas(size, svgPath(ToolCommon.SVG_WINDOWS, Color.web("#0078d4"), true));
    }

    /** 使用系统原生的小尺寸图标，保留小尺寸像素对齐、纸页阴影与装订细节。 */
    public static StackPane notepadIcon(double size) {
        ImageView image = new ImageView(NotepadImage.VALUE);
        image.setFitWidth(size);
        image.setFitHeight(size);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.setMouseTransparent(true);
        StackPane box = new StackPane(image);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    private static final class NotepadImage {
        private static final Image VALUE = new Image(new ByteArrayInputStream(
                Base64.getDecoder().decode(ToolCommon.NOTEPAD_PNG)));
    }

    private static SVGPath svgPath(String svg, Color color, boolean solid) {
        SVGPath path = new SVGPath();
        path.setContent(svg);
        path.setFill(solid ? color : null);
        path.setStroke(solid ? null : color);
        path.setStrokeWidth(2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    private static StackPane svgCanvas(double size, SVGPath... paths) {
        StackPane box = new StackPane();
        for (SVGPath path : paths) {
            path.setManaged(false);
            path.getTransforms().add(new Scale(size / 24.0, size / 24.0, 0, 0));
            path.setMouseTransparent(true);
            box.getChildren().add(path);
        }
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    /** 三个小圆点，代替 ⋯ 图标（-fx-shape 会把扁长路径拉伸）。 */
    public static HBox dots(ThemeColor color, double size) {
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);
        for (int i = 0; i < 3; i++) {
            box.getChildren().add(dot(color, size));
        }
        return box;
    }

    public static Region dot(ThemeColor color, double size) {
        Region dot = new Region();
        dot.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.0f; -fx-min-width: %.0f;"
                        + " -fx-pref-width: %.0f; -fx-max-width: %.0f; -fx-min-height: %.0f;"
                        + " -fx-pref-height: %.0f; -fx-max-height: %.0f;",
                color.color(), size / 2, size, size, size, size, size, size));
        return dot;
    }

    public static Region hspacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    public static Region line(ThemeColor color) {
        Region region = new Region();
        region.setStyle(String.format(
                "-fx-background-color: %s; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1;",
                color.color()));
        return region;
    }

    /** 细滚动条样式；用 data URI 注入，不需要额外资源文件。 */
    public static String appCss() {
        String thumb = ToolService.getInstance().isDark() ? "#3c4653" : "#c8cfd8";
        String thumbHover = ToolService.getInstance().isDark() ? "#647286" : "#9ba7b7";
        // 自绘按钮统一清除 Modena 默认的多层内缩和阴影；主窗口与独立菜单共用。
        String css = ".button,.button:hover,.button:armed,.button:focused{"
                + "-fx-background-insets:0;-fx-border-insets:0;-fx-effect:null;}"
                + ".scroll-pane{-fx-background-color:transparent;-fx-background:transparent;}"
                + ".scroll-pane>.viewport{-fx-background-color:transparent;}"
                + ".scroll-pane>.scroll-bar:vertical{-fx-background-color:transparent;-fx-min-width:8;-fx-pref-width:8;-fx-max-width:8;-fx-padding:0;}"
                + ".scroll-pane>.scroll-bar:vertical>.track{-fx-background-color:transparent;-fx-border-color:transparent;}"
                + ".scroll-pane>.scroll-bar:vertical>.thumb{-fx-background-color:" + thumb
                + ";-fx-background-radius:4;-fx-background-insets:2;}"
                + ".scroll-pane>.scroll-bar:vertical>.thumb:hover{-fx-background-color:" + thumbHover + ";}"
                + ".scroll-pane>.scroll-bar:vertical>.increment-button,"
                + ".scroll-pane>.scroll-bar:vertical>.decrement-button{-fx-pref-height:0;-fx-pref-width:0;-fx-opacity:0;-fx-padding:0;}"
                + ".scroll-pane>.scroll-bar:vertical>.increment-arrow,"
                + ".scroll-pane>.scroll-bar:vertical>.decrement-arrow{-fx-shape:\" \";-fx-padding:0;}";
        try {
            return "data:text/css," + java.net.URLEncoder.encode(css, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "data:text/css,";
        }
    }

    /** 应用内弹层：遮罩 + 居中内容，盖住内容区但不盖标题栏。 */
    public static StackPane openSheet(StackPane host, Region content, boolean dismissOnScrim, Runnable onClose) {
        StackPane layer = new StackPane();
        // 不要让图层整块吃鼠标事件，否则标题栏的拖动/最小化/关闭全被挡住
        layer.setPickOnBounds(false);
        layer.setPadding(new Insets(ToolCommon.TITLE_BAR_HEIGHT, 0, 0, 0));

        Region scrim = new Region();
        scrim.setStyle("-fx-background-color: rgba(16,24,40,0.34);");
        if (dismissOnScrim) {
            final long openedAt = System.currentTimeMillis();
            scrim.setOnMouseClicked(e -> {
                if (System.currentTimeMillis() - openedAt < 150) {
                    return;
                }
                close(host, layer);
                if (onClose != null) {
                    onClose.run();
                }
            });
        }

        StackPane holder = new StackPane(content);
        holder.setAlignment(Pos.CENTER);
        layer.getChildren().addAll(scrim, holder);
        host.getChildren().add(layer);
        fitSheetWindow(host, content, layer);
        return layer;
    }

    /** 紧凑窗口打开确认框时临时扩高，避免风险提示和按钮被裁掉。 */
    private static void fitSheetWindow(StackPane host, Region content, StackPane layer) {
        if (host.getScene() == null || !(host.getScene().getWindow() instanceof Stage stage)
                || !stage.isShowing()) {
            return;
        }
        host.applyCss();
        double width = Math.min(content.maxWidth(-1), Math.max(1, host.getWidth() - 32));
        double needed = Math.ceil(Math.max(content.minHeight(width), content.prefHeight(width)))
                + ToolCommon.TITLE_BAR_HEIGHT + 24;
        Rectangle2D screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        double beforeHeight = stage.getHeight();
        double beforeY = stage.getY();
        double expanded = Math.min(needed, screen.getHeight() - 16);
        if (expanded <= beforeHeight) {
            return;
        }
        double expandedY = Math.max(screen.getMinY(), Math.min(beforeY, screen.getMaxY() - expanded));
        stage.setHeight(expanded);
        stage.setY(expandedY);
        layer.parentProperty().addListener((observable, previous, current) -> {
            if (current == null && Math.abs(stage.getHeight() - expanded) < 2) {
                stage.setHeight(beforeHeight);
                if (Math.abs(stage.getY() - expandedY) < 2) {
                    stage.setY(beforeY);
                }
            }
        });
    }

    public static void close(StackPane host, Node layer) {
        host.getChildren().remove(layer);
    }

    /** 弹层里的白色面板。 */
    public static VBox panel(double width) {
        VBox panel = new VBox();
        panel.setMaxWidth(width);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12; -fx-border-radius: 12;"
                        + " -fx-border-color: %s; -fx-border-width: 1; -fx-padding: 20 26 14 26;"
                        + " -fx-effect: dropshadow(gaussian, rgba(16,24,40,%s), 30, 0.28, 0, 12);",
                ThemeColor.PANEL.color(), ThemeColor.PANEL_BORDER.color(),
                ToolService.getInstance().isDark() ? "0.55" : "0.18"));
        return panel;
    }

    public static HBox panelHeader(String title, String subtitle, Runnable onClose) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(label(title, 16, ThemeColor.TEXT));
        if (subtitle != null && !subtitle.isEmpty()) {
            box.getChildren().add(label(subtitle, 12.5, ThemeColor.TEXT_3));
        }
        box.getChildren().add(hspacer());
        if (onClose != null) {
            Button close = new Button();
            close.setGraphic(strokeIcon(ToolCommon.LINE_CLOSE, 14, ThemeColor.TEXT_2));
            close.setFocusTraversable(false);
            close.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-background-radius: 7;"
                    + " -fx-min-width: 28; -fx-pref-width: 28; -fx-max-width: 28; -fx-min-height: 28;"
                    + " -fx-pref-height: 28; -fx-max-height: 28; -fx-cursor: hand;");
            installHover(close, true);
            close.setOnAction(e -> onClose.run());
            box.getChildren().add(close);
        }
        return box;
    }

    /** 表单输入行：标签在上，输入在下。 */
    public static VBox field(String labelText, boolean required, Region input, double width) {
        Label label = label(labelText, 12, ThemeColor.TEXT_2);
        Node labelNode = label;
        if (required) {
            Label star = new Label("*");
            star.setStyle("-fx-text-fill: " + ThemeColor.RED.color() + "; -fx-font-size: 12px;");
            HBox labelBox = new HBox(2, label, star);
            labelBox.setAlignment(Pos.CENTER_LEFT);
            labelNode = labelBox;
        }
        VBox box = new VBox(6, labelNode, input);
        box.setMinWidth(0);
        if (width > 0) {
            box.setPrefWidth(width);
        }
        return box;
    }

    public static HBox buttonsRight(Button... buttons) {
        HBox box = new HBox(8, buttons);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    public static Button primaryButton(String text) {
        return primaryButton(text, ThemeColor.BLUE);
    }

    public static Button primaryButton(String text, ThemeColor color) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        String normal = String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 13px;"
                        + " -fx-padding: 0 12; -fx-background-radius: 6; -fx-min-height: 28;"
                        + " -fx-pref-height: 28; -fx-max-height: 28; -fx-cursor: hand;",
                color == ThemeColor.RED ? ThemeColor.RED.light() : color.color());
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(normal + "-fx-opacity: 0.88;"));
        button.setOnMouseExited(e -> button.setStyle(normal));
        return button;
    }

    /** 控制面板中的停止操作采用轻量红色，避免实心色块压过实例信息。 */
    public static Button stopButton(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        boolean dark = ToolService.getInstance().isDark();
        String normal = "-fx-text-fill: " + ThemeColor.RED.color()
                + "; -fx-font-size: 13px; -fx-padding: 0 12; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-border-width: 1;"
                + " -fx-border-color: rgba(220,55,45," + (dark ? "0.38" : "0.22") + ");"
                + " -fx-min-height: 28; -fx-pref-height: 28; -fx-max-height: 28; -fx-cursor: hand;";
        String background = "-fx-background-color: rgba(220,55,45," + (dark ? "0.10" : "0.05") + ");";
        String hoverBackground = "-fx-background-color: rgba(220,55,45," + (dark ? "0.18" : "0.10") + ");";
        button.setStyle(normal + background);
        button.setOnMouseEntered(e -> button.setStyle(normal + (button.isDisabled() ? background : hoverBackground)));
        button.setOnMouseExited(e -> button.setStyle(normal + background));
        return button;
    }

    public static Button plainButton(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-size: 13px;"
                        + " -fx-padding: 0 12; -fx-background-radius: 6; -fx-min-height: 28;"
                        + " -fx-pref-height: 28; -fx-max-height: 28; -fx-cursor: hand;",
                ThemeColor.TEXT_2.color()));
        installHover(button, true);
        return button;
    }

    /** 文本输入框。 */
    public static javafx.scene.control.TextField input(String text) {
        javafx.scene.control.TextField field = new javafx.scene.control.TextField(text == null ? "" : text);
        field.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13.5px;"
                        + " -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: %s;"
                        + " -fx-border-width: 1; -fx-padding: 0 12 0 12; -fx-min-height: 34; -fx-pref-height: 34;",
                ThemeColor.CARD.color(), ThemeColor.TEXT.color(), ThemeColor.CARD_BORDER.color()));
        return field;
    }

    /** 菜单容器。 */
    public static VBox menuBox() {
        VBox box = new VBox();
        box.setFillWidth(true);
        box.setMinWidth(Region.USE_PREF_SIZE);
        box.setMaxWidth(Region.USE_PREF_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-radius: 10;"
                        + " -fx-border-color: %s; -fx-border-width: 1; -fx-padding: 6;"
                        + " -fx-effect: dropshadow(gaussian, rgba(16,24,40,%s), 22, 0.24, 0, 8);",
                ThemeColor.PANEL.color(), ThemeColor.PANEL_BORDER.color(),
                ToolService.getInstance().isDark() ? "0.55" : "0.16"));
        return box;
    }

    /** 菜单项：按钮行，hover 只换底色，文字不位移。 */
    public static Button menuItem(String text, ThemeColor color) {
        Button item = new Button(text);
        item.setFocusTraversable(false);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPrefHeight(32);
        item.setStyle(String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-size: 13px;"
                        + " -fx-padding: 0 12 0 12; -fx-background-radius: 7; -fx-cursor: hand;",
                color.color()));
        installHover(item, true);
        return item;
    }

    public static Button menuItem(String text, String svg, ThemeColor color) {
        Button item = menuItem(text, color);
        item.setGraphic(strokeIcon(svg, 16, color));
        item.setGraphicTextGap(10);
        return item;
    }

    public static Region menuSeparator() {
        Region region = new Region();
        region.setStyle(String.format(
                "-fx-background-color: %s; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1;",
                ThemeColor.LINE.color()));
        VBox.setMargin(region, new Insets(4, 0, 4, 0));
        return region;
    }

    /**
     * 圆角分段控件：选中块是一个会滑动的圆角拇指，切换时 170ms 平移。
     * 内容由调用方用 setCell 填充（图标或文字）。
     */
    public static class Segmented extends Region {
        private final Region background = new Region();
        private final HBox track = new HBox();
        private final Region thumb = new Region();
        private final List<StackPane> cells = new ArrayList<>();
        private final TranslateTransition slide;
        private final IntConsumer onSelect;
        private final double cellWidth;
        private final double height;
        private int selected;

        public Segmented(int count, double cellWidth, double height, int selected, IntConsumer onSelect) {
            this.cellWidth = cellWidth;
            this.height = height;
            this.selected = Math.max(0, Math.min(count - 1, selected));
            this.onSelect = onSelect;

            setMinSize(cellWidth * count, height);
            setPrefSize(cellWidth * count, height);
            setMaxSize(cellWidth * count, height);

            background.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 9;", ThemeColor.FILL.color()));
            track.setStyle("-fx-background-color: transparent;");
            track.setAlignment(Pos.CENTER_LEFT);
            track.setMinSize(cellWidth * count, height);
            track.setPrefSize(cellWidth * count, height);

            for (int i = 0; i < count; i++) {
                final int index = i;
                StackPane cell = new StackPane();
                cell.setMinSize(cellWidth, height);
                cell.setPrefSize(cellWidth, height);
                cell.setMaxSize(cellWidth, height);
                cell.setStyle("-fx-cursor: hand;");
                cell.setOnMouseClicked(e -> choose(index));
                cells.add(cell);
                track.getChildren().add(cell);
            }

            thumb.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 7; -fx-background-insets: 3;"
                            + " -fx-effect: dropshadow(gaussian, rgba(16,24,40,%s), 6, 0.12, 0, 1);",
                    ThemeColor.CARD.color(), ToolService.getInstance().isDark() ? "0.45" : "0.10"));
            thumb.setMouseTransparent(true);
            thumb.setTranslateX(this.selected * cellWidth);

            slide = new TranslateTransition(Duration.millis(170), thumb);
            slide.setInterpolator(Interpolator.EASE_BOTH);

            // 图层顺序：底色 -> 滑块 -> 内容，保证选中项的字/图标始终可见
            getChildren().addAll(background, thumb, track);
        }

        public void setCell(int index, Node node) {
            cells.get(index).getChildren().add(node);
        }

        private void choose(int index) {
            if (index == selected) {
                return;
            }
            selected = index;
            slide.stop();
            slide.setToX(index * cellWidth);
            slide.playFromStart();
            if (onSelect != null) {
                onSelect.accept(index);
            }
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            background.resizeRelocate(0, 0, w, h);
            track.resizeRelocate(0, 0, w, h);
            // 拇指宽度固定为一个格子，水平位置由 translateX 控制（动画期间不会被布局覆盖）
            thumb.resizeRelocate(0, 0, cellWidth, h);
        }
    }

    /** 兼容旧调用：返回带滑块的胶囊控件。 */
    public static Segmented segmented(String[] labels, int selected, IntConsumer onSelect) {
        Segmented seg = new Segmented(labels.length, 84, 34, selected, onSelect);
        for (int i = 0; i < labels.length; i++) {
            seg.setCell(i, label(labels[i], 12.5, i == selected ? ThemeColor.TEXT : ThemeColor.TEXT_2));
        }
        return seg;
    }
}
