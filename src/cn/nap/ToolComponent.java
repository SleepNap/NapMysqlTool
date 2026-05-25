package cn.nap;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.util.List;

import static cn.nap.ToolCommon.ThemeColor;

public class ToolComponent {

    public static Button button(String text, boolean dark) {
        Button button = new Button(text);
        addButtonStyle(button, dark);
        return button;
    }

    public static Button highButton(String text, boolean dark) {
        Button button = button(text, dark);
        button.setPadding(new Insets(10, 0, 10, 0));
        return button;
    }

    private static void addButtonStyle(ButtonBase button, boolean dark) {
        final String exitStyle = String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 5px;-fx-border-color: %s;-fx-border-radius: 5px;", ThemeColor.BUTTON_BG.color(dark), ThemeColor.FONT_BG.color(dark), ThemeColor.BUTTON_BORDER.color(dark));
        final String enterStyle = String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 5px;-fx-border-color: %s;-fx-border-radius: 5px;", ThemeColor.BUTTON_HOVER.color(dark), ThemeColor.FONT_HOVER.color(dark), ThemeColor.BUTTON_BORDER_HOVER.color(dark));
        button.setStyle(exitStyle);
        button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
        button.setOnMouseExited((e) -> button.setStyle(exitStyle));
    }

    public static Button themeIcon(boolean dark) {
        // 黑色展示太阳，白色展示月亮
        final String svg = dark ? ToolCommon.SVG_LIGHT : ToolCommon.SVG_DARK;
        return icon(svg, dark);
    }

    public static Button icon(String svg, boolean dark) {
        Button button = new Button();
        button.setFont(new Font(14));
        button.setStyle("-fx-background-color: transparent;");
        button.setGraphic(iconGraphic(dark, false, button.getFont().getSize(), svg));
        button.setOnMouseEntered((e) -> button.setGraphic(iconGraphic(dark, true, button.getFont().getSize(), svg)));
        button.setOnMouseExited((e) -> button.setGraphic(iconGraphic(dark, false, button.getFont().getSize(), svg)));
        return button;
    }

    private static Region iconGraphic(boolean dark, boolean hover, double size, String svg) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(svg);
        Region region = new Region();
        region.setShape(svgPath);
        region.setStyle(String.format("-fx-background-color: %s;", hover ? ThemeColor.FONT_HOVER.color(dark) : ThemeColor.FONT_BG.color(dark)));
        region.setPrefSize(size, size);
        region.setMaxSize(size, size);
        return region;
    }

    public static Region line(boolean dark) {
        Region region = new Region();
        region.setMaxWidth(Double.MAX_VALUE);
        region.setPrefHeight(0.1);
        region.setMaxHeight(0.1);
        region.setStyle(String.format("-fx-background-color: %s;", ThemeColor.FONT_BG.color(dark)));
        return region;
    }

    public static ToggleButton menu(ToggleGroup group, String text, boolean dark) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        addMenuNormalStyle(button, dark);
        button.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                addMenuToggleStyle(button, dark);
            } else {
                addMenuNormalStyle(button, dark);
            }
        });
        return button;
    }

    private static void addMenuToggleStyle(ToggleButton button, boolean dark) {
        button.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;-fx-font-size: 14px;-fx-font-weight: bold;", ThemeColor.BUTTON_BG.color(dark), ThemeColor.FONT_HOVER.color(dark)));
        button.setOnMouseEntered(null);
        button.setOnMouseExited(null);
    }

    private static void addMenuNormalStyle(ToggleButton button, boolean dark) {
        String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;-fx-font-size: 14px;", ThemeColor.FONT_BG.color(dark));
        String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;-fx-font-size: 14px;", ThemeColor.FONT_HOVER.color(dark));
        button.setStyle(exitStyle);
        button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
        button.setOnMouseExited((e) -> button.setStyle(exitStyle));
    }

    public static ScrollPane scrollPane(boolean dark) {
        ScrollPane scrollPane = new ScrollPane();
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
        scrollPane.setStyle(String.format("-fx-background-color: %s;-fx-border-color: %s;", ThemeColor.SCROLL_BG.color(dark), ThemeColor.SCROLL_BG.color(dark)));
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

    public static Label label(String text, boolean dark) {
        Label label = new Label(text);
        label.setStyle(String.format("-fx-text-fill: %s;", ThemeColor.FONT_BG.color(dark)));
        return label;
    }

    public static GridPane instanceInfo(String name, List<Node> statusList, List<Node> operateList, boolean dark) {
        GridPane gridPane = getInstanceGrid(dark);

        int columnIndex = 0;
        HBox nameBox = new HBox();
        nameBox.setFillHeight(true);
        nameBox.setMaxWidth(Double.MAX_VALUE);
        nameBox.setStyle("-fx-alignment: center_left;-fx-spacing: 10px;");
        nameBox.getChildren().add(label(name, dark));
        gridPane.add(nameBox, columnIndex, 0);
        columnIndex++;

        HBox statusBox = new HBox();
        statusBox.setStyle("-fx-alignment: center_left;-fx-spacing: 10px;");
        statusBox.getChildren().addAll(statusList);
        gridPane.add(statusBox, columnIndex, 0);
        columnIndex++;

        HBox operateBox = new HBox();
        operateBox.setStyle("-fx-alignment: center_left;-fx-spacing: 10px;");
        operateBox.getChildren().addAll(operateList);
        gridPane.add(operateBox, columnIndex, 0);

        GridPane.setHgrow(nameBox, Priority.ALWAYS);
        return gridPane;
    }

    private static GridPane getInstanceGrid(boolean dark) {
        GridPane gridPane = new GridPane();
        final String exitStyle = String.format("-fx-alignment: center_left;-fx-hgap: 30px;-fx-padding: 10px;-fx-border-color: %s;-fx-border-radius: 5px;", ThemeColor.BUTTON_BORDER.color(dark));
        final String enterStyle = String.format("-fx-alignment: center_left;-fx-hgap: 30px;-fx-padding: 10px;-fx-border-color: %s;-fx-border-radius: 5px;", ThemeColor.FONT_HOVER.color(dark));
        gridPane.setStyle(exitStyle);
        gridPane.setOnMouseEntered((e) -> gridPane.setStyle(enterStyle));
        gridPane.setOnMouseExited((e) -> gridPane.setStyle(exitStyle));
        return gridPane;
    }
}
