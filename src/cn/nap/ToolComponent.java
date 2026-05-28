package cn.nap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.util.List;

import static cn.nap.ToolCommon.ThemeColor;

public class ToolComponent {

    public static Button button(String text, boolean dark) {
        return button(text, null, dark);
    }

    public static Button button(String text, String svg, boolean dark) {
        Button button = new Button(text);
        final String color = ThemeColor.FONT_BG.color(dark);
        final String hoverColor = ThemeColor.FONT_HOVER.color(dark);
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

    public static Button highButton(String text, boolean dark) {
        Button button = button(text, dark);
        button.setPadding(new Insets(12, 0, 12, 0));
        return button;
    }

    public static Button startButton(String text, boolean dark) {
        return startButton(text, null, dark);
    }

    public static Button startButton(String text, String svg, boolean dark) {
        Button button = new Button(text);
        final String outlineStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY.color(dark), ThemeColor.PRIMARY.color(dark));
        final String filledStyle = String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.PRIMARY.color(dark), ThemeColor.PRIMARY.color(dark));
        button.setStyle(outlineStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, ThemeColor.PRIMARY.color(dark)));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(filledStyle);
                button.setGraphic(createIconNode(svg, "#fff"));
            });
            button.setOnMouseExited((e) -> {
                button.setStyle(outlineStyle);
                button.setGraphic(createIconNode(svg, ThemeColor.PRIMARY.color(dark)));
            });
        } else {
            button.setOnMouseEntered((e) -> button.setStyle(filledStyle));
            button.setOnMouseExited((e) -> button.setStyle(outlineStyle));
        }
        return button;
    }

    public static Button stopButton(String text, boolean dark) {
        return stopButton(text, null, dark);
    }

    public static Button stopButton(String text, String svg, boolean dark) {
        Button button = new Button(text);
        final String color = ThemeColor.FONT_BG.color(dark);
        final String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", color, color);
        final String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.DANGER.color(dark), ThemeColor.DANGER.color(dark));
        button.setStyle(exitStyle);
        if (svg != null) {
            button.setGraphic(createIconNode(svg, color));
            button.setGraphicTextGap(6);
            button.setOnMouseEntered((e) -> {
                button.setStyle(enterStyle);
                button.setGraphic(createIconNode(svg, ThemeColor.DANGER.color(dark)));
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
        region.setPrefHeight(0);
        region.setMaxHeight(0);
        region.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 0.5 0 0 0;", ThemeColor.BUTTON_BORDER.color(dark)));
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
        button.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: #fff;-fx-border-color: transparent;-fx-background-radius: 10px;-fx-font-size: 14px;-fx-font-weight: bold;", ThemeColor.PRIMARY.color(dark)));
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

    public static VBox instanceInfo(String name, List<Node> statusList, List<Node> operateList, boolean dark) {
        // 上排：名称 + 状态
        Label nameLabel = label(name, dark);
        nameLabel.setStyle(String.format("-fx-text-fill: %s;-fx-font-size: 14px;-fx-font-weight: bold;", ThemeColor.FONT_HOVER.color(dark)));
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
        final String exitStyle = String.format("-fx-background-color: %s;-fx-padding: 15px;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.CARD_BG.color(dark), ThemeColor.BUTTON_BORDER.color(dark));
        final String enterStyle = String.format("-fx-background-color: %s;-fx-padding: 15px;-fx-border-color: %s;-fx-border-radius: 8px;-fx-background-radius: 8px;", ThemeColor.CARD_BG.color(dark), ThemeColor.FONT_HOVER.color(dark));
        card.setStyle(exitStyle);
        card.setOnMouseEntered((e) -> card.setStyle(enterStyle));
        card.setOnMouseExited((e) -> card.setStyle(exitStyle));
        return card;
    }
}
