package cn.nap;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import static cn.nap.ToolCommon.ThemeColor;

public class ToolComponent {

    public static Button normalButton(String text, boolean dark) {
        Button button = new Button(text);
        addButtonStyle(button, dark);
        return button;
    }

    public static Button largeButton(String text, boolean dark) {
        Button button = normalButton(text, dark);
        button.setFont(new Font(14));
        return button;
    }

    private static void addButtonStyle(ButtonBase button, boolean dark) {
        final String exitStyle = String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;", ThemeColor.BUTTON_BG.color(dark), ThemeColor.FONT_BG.color(dark));
        final String enterStyle = String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;", ThemeColor.BUTTON_HOVER.color(dark), ThemeColor.FONT_HOVER.color(dark));
        button.setStyle(exitStyle);
        button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
        button.setOnMouseExited((e) -> button.setStyle(exitStyle));
    }

    public static ToggleButton toggleButton(ToggleGroup group, String text, boolean dark) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setToggleGroup(group);
        addButtonToggleStyle(toggleButton, dark);
        toggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            addButtonToggleStyle(toggleButton, dark);
        });
        return toggleButton;
    }

    public static ToggleButton largeToggleButton(ToggleGroup group, String text, boolean dark) {
        ToggleButton toggleButton = toggleButton(group, text, dark);
        toggleButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        return toggleButton;
    }

    private static void addButtonToggleStyle(ToggleButton button, boolean dark) {
        if (button.isSelected()) {
            button.setOnMouseEntered(null);
            button.setOnMouseExited(null);
            button.setStyle(String.format("-fx-background-color: %s;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;", ThemeColor.BUTTON_BG.color(dark), ThemeColor.FONT_HOVER.color(dark)));
            return;
        }
        final String exitStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;", ThemeColor.FONT_BG.color(dark));
        final String enterStyle = String.format("-fx-background-color: transparent;-fx-text-fill: %s;-fx-border-color: transparent;-fx-background-radius: 10px;", ThemeColor.FONT_HOVER.color(dark));
        button.setStyle(exitStyle);
        button.setOnMouseEntered((e) -> button.setStyle(enterStyle));
        button.setOnMouseExited((e) -> button.setStyle(exitStyle));
    }

    public static Button themeIcon(boolean dark) {
        Button button = new Button();
        button.setFont(new Font(14));
        button.setStyle("-fx-background-color: transparent;");
        button.setGraphic(iconGraphic(dark, false, button.getFont().getSize()));
        button.setOnMouseEntered((e) -> button.setGraphic(iconGraphic(dark, true, button.getFont().getSize())));
        button.setOnMouseExited((e) -> button.setGraphic(iconGraphic(dark, false, button.getFont().getSize())));
        return button;
    }

    private static Region iconGraphic(boolean dark, boolean hover, double size) {
        // 黑色展示太阳，白色展示月亮
        final String svg = dark ? ToolCommon.SVG_LIGHT : ToolCommon.SVG_DARK;

        SVGPath svgPath = new SVGPath();
        svgPath.setContent(svg);
        Region region = new Region();
        region.setShape(svgPath);
        region.setStyle(String.format("-fx-background-color: %s;", hover ? ThemeColor.FONT_HOVER.color(dark) : ThemeColor.FONT_BG.color(dark)));
        region.setPrefSize(size, size);
        region.setMaxSize(size, size);
        return region;
    }
}
