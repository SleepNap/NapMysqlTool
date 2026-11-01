package cn.nap;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;

import static cn.nap.ToolCommon.ThemeColor;

/** 设置共用的胶囊选择器：同一圆形滑块，原位换色，连续切换不中断面板。 */
public final class SettingsPicker extends Region {
    private static final double CELL_WIDTH = 32;
    private static final double HEIGHT = 30;
    private static final double KNOB_SIZE = 24;

    private final Region track = new Region();
    private final Region knob = new Region();
    private final List<Button> cells = new ArrayList<>();
    private final TranslateTransition slide = new TranslateTransition(Duration.millis(180), knob);
    private final BiFunction<Integer, Boolean, Node> graphic;
    private final IntConsumer onSelect;
    private int selected;

    public SettingsPicker(int count, int selected, BiFunction<Integer, Boolean, Node> graphic,
                          IntConsumer onSelect) {
        this.selected = selected;
        this.graphic = graphic;
        this.onSelect = onSelect;
        setMinSize(count * CELL_WIDTH, HEIGHT);
        setPrefSize(count * CELL_WIDTH, HEIGHT);
        setMaxSize(count * CELL_WIDTH, HEIGHT);
        track.setManaged(false);
        track.setMouseTransparent(true);
        knob.setManaged(false);
        knob.setMouseTransparent(true);
        knob.setTranslateX(selected * CELL_WIDTH);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        getChildren().addAll(track, knob);

        for (int i = 0; i < count; i++) {
            int index = i;
            Button cell = new Button();
            cell.setManaged(false);
            cell.setAlignment(Pos.CENTER);
            cell.setTooltip(new Tooltip());
            cell.setOnAction(e -> choose(index));
            cell.hoverProperty().addListener((observable, before, after) -> refreshTrack());
            cell.focusedProperty().addListener((observable, before, after) -> refreshTrack());
            cell.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case LEFT -> { choose(Math.max(0, this.selected - 1)); e.consume(); }
                    case RIGHT -> { choose(Math.min(cells.size() - 1, this.selected + 1)); e.consume(); }
                    default -> { }
                }
            });
            cells.add(cell);
            getChildren().add(cell);
        }
    }

    private void choose(int index) {
        if (selected == index) {
            return;
        }
        selected = index;
        double from = knob.getTranslateX();
        slide.stop();
        slide.setFromX(from);
        slide.setToX(index * CELL_WIDTH);
        slide.playFromStart();
        onSelect.accept(index);
    }

    /** 只更新原有节点的外观与提示，保留滑块的位置和正在播放的动画。 */
    public void refresh(String... labels) {
        refreshTrack();
        knob.setStyle("-fx-background-color: " + ThemeColor.THUMB.color()
                + "; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 1;"
                + " -fx-border-color: " + ThemeColor.BLUE.color() + ";");
        for (int i = 0; i < cells.size(); i++) {
            Button cell = cells.get(i);
            cell.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;"
                    + " -fx-border-color: transparent; -fx-padding: 0; -fx-cursor: hand;"
                    + " -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            cell.setGraphic(graphic.apply(i, i == selected));
            cell.setAccessibleText(labels[i]);
            cell.getTooltip().setText(labels[i]);
        }
    }

    private void refreshTrack() {
        boolean active = cells.stream().anyMatch(cell -> cell.isHover() || cell.isFocused());
        track.setStyle("-fx-background-color: " + ThemeColor.TRACK.color()
                + "; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-width: 1;"
                + " -fx-border-color: " + (active ? ThemeColor.BLUE.color() : ThemeColor.CARD_BORDER.color())
                + ";");
    }

    @Override
    protected void layoutChildren() {
        track.resizeRelocate(0, 0, getWidth(), HEIGHT);
        knob.resizeRelocate((CELL_WIDTH - KNOB_SIZE) / 2, (HEIGHT - KNOB_SIZE) / 2,
                KNOB_SIZE, KNOB_SIZE);
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).resizeRelocate(i * CELL_WIDTH, 0, CELL_WIDTH, HEIGHT);
        }
    }
}
