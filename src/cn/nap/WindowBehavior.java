package cn.nap;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** 无边框窗口的拖动、八向缩放、双击最大化。 */
public class WindowBehavior {

    private final Stage stage;
    private final Region surface;
    private final Region dragHandle;

    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    private int resizeEdge;
    private double pressScreenX;
    private double pressScreenY;
    private double pressWidth;
    private double pressHeight;
    private double pressX;
    private double pressY;

    private Rectangle2D restoreBounds;
    private boolean maximized;

    public WindowBehavior(Stage stage, Region surface, Region dragHandle) {
        this.stage = stage;
        this.surface = surface;
        this.dragHandle = dragHandle;
        install();
    }

    private void install() {
        dragHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, this::pressOnBar);
        dragHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::dragBar);
        dragHandle.addEventHandler(MouseEvent.MOUSE_CLICKED, this::doubleClick);

        surface.addEventFilter(MouseEvent.MOUSE_MOVED, this::hover);
        surface.addEventFilter(MouseEvent.MOUSE_PRESSED, this::pressResize);
        surface.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::resize);
        surface.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> resizeEdge = 0);
    }

    private boolean isWindowButton(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node.getStyleClass().contains("window-btn")) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void pressOnBar(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || maximized || isWindowButton(event.getTarget())) {
            return;
        }
        if (resizeEdge != 0) {
            return;
        }
        dragging = true;
        dragOffsetX = event.getScreenX() - stage.getX();
        dragOffsetY = event.getScreenY() - stage.getY();
        event.consume();
    }

    private void dragBar(MouseEvent event) {
        if (!dragging || !event.isPrimaryButtonDown()) {
            return;
        }
        double x = event.getScreenX() - dragOffsetX;
        double y = event.getScreenY() - dragOffsetY;
        Rectangle2D screen = currentScreen();
        double visible = 160;
        x = Math.clamp(x, screen.getMinX() - stage.getWidth() + visible, screen.getMaxX() - visible);
        y = Math.clamp(y, screen.getMinY(), screen.getMaxY() - 32);
        stage.setX(x);
        stage.setY(y);
        event.consume();
    }

    private void doubleClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                && !isWindowButton(event.getTarget())) {
            toggleMaximize();
        }
    }

    private void hover(MouseEvent event) {
        if (dragging) {
            surface.setCursor(Cursor.DEFAULT);
            return;
        }
        if (maximized || !stage.isResizable()) {
            surface.setCursor(Cursor.DEFAULT);
            return;
        }
        surface.setCursor(cursor(edge(event.getSceneX(), event.getSceneY())));
    }

    private int edge(double x, double y) {
        double size = ToolCommon.RESIZE_EDGE * Math.max(1, stage.getOutputScaleX());
        int result = 0;
        if (x <= size) {
            result |= 1;
        }
        if (x >= surface.getWidth() - size) {
            result |= 2;
        }
        if (y <= size) {
            result |= 4;
        }
        if (y >= surface.getHeight() - size) {
            result |= 8;
        }
        return result;
    }

    private Cursor cursor(int edge) {
        return switch (edge) {
            case 1 -> Cursor.W_RESIZE;
            case 2, 3 -> Cursor.E_RESIZE;
            case 4 -> Cursor.N_RESIZE;
            case 8, 12 -> Cursor.S_RESIZE;
            case 5, 10 -> Cursor.NW_RESIZE;
            case 6, 9 -> Cursor.NE_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private void pressResize(MouseEvent event) {
        if (maximized || !stage.isResizable() || event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        resizeEdge = edge(event.getSceneX(), event.getSceneY());
        if (resizeEdge == 0) {
            return;
        }
        pressScreenX = event.getScreenX();
        pressScreenY = event.getScreenY();
        pressWidth = stage.getWidth();
        pressHeight = stage.getHeight();
        pressX = stage.getX();
        pressY = stage.getY();
        event.consume();
    }

    private void resize(MouseEvent event) {
        if (resizeEdge == 0 || !event.isPrimaryButtonDown()) {
            return;
        }
        double dx = event.getScreenX() - pressScreenX;
        double dy = event.getScreenY() - pressScreenY;
        double width = pressWidth;
        double height = pressHeight;
        if ((resizeEdge & 1) != 0) {
            width = Math.max(stage.getMinWidth(), pressWidth - dx);
        }
        if ((resizeEdge & 2) != 0) {
            width = Math.max(stage.getMinWidth(), pressWidth + dx);
        }
        if ((resizeEdge & 4) != 0) {
            height = Math.max(stage.getMinHeight(), pressHeight - dy);
        }
        if ((resizeEdge & 8) != 0) {
            height = Math.max(stage.getMinHeight(), pressHeight + dy);
        }
        double x = (resizeEdge & 1) != 0 ? pressX + pressWidth - width : pressX;
        double y = (resizeEdge & 4) != 0 ? pressY + pressHeight - height : pressY;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(x);
        stage.setY(y);
        event.consume();
    }

    /** 无边框窗口不能依赖 setMaximized：自己铺满可视区，避开任务栏。 */
    public void toggleMaximize() {
        if (!stage.isResizable()) {
            return;
        }
        if (maximized) {
            maximized = false;
            if (restoreBounds != null) {
                stage.setX(restoreBounds.getMinX());
                stage.setY(restoreBounds.getMinY());
                stage.setWidth(restoreBounds.getWidth());
                stage.setHeight(restoreBounds.getHeight());
            }
            return;
        }
        restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Rectangle2D bounds = currentScreen();
        maximized = true;
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    public boolean isMaximized() {
        return maximized;
    }

    private Rectangle2D currentScreen() {
        double centerX = stage.getX() + stage.getWidth() / 2;
        double centerY = stage.getY() + stage.getHeight() / 2;
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (bounds.contains(centerX, centerY)) {
                return bounds;
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }
}
