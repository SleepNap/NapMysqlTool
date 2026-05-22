package cn.nap;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.ThemeColor;

public class ToolApp extends Application {
    private final ToolService toolService = new ToolService();
    private Thread initThread;
    private Stage primaryStage;
    private StackPane root;
    private BorderPane body;

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
        loadRoot();
        Scene scene = new Scene(root, 560, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle(i18n(I18n.TOOL_TITLE));
        primaryStage.show();
    }

    private void loadRoot() {
        if (toolService.isWin11()) {
            ToolUtil.setWindowFrameColor(primaryStage, ToolUtil.toRGBInt(Color.web(ThemeColor.WINDOW_BG.color(toolService.isDark()))));
        } else {
            ToolUtil.setWindowDarkMode(toolService.isWin11(), primaryStage, toolService.isDark());
        }
        root = new StackPane();
        loadBody();
    }

    private void loadBody() {
        body = new BorderPane();
        body.setStyle(String.format("-fx-background-color: %s", ThemeColor.ROOT_BG.color(toolService.isDark())));

        loadTop();
        root.getChildren().add(body);
    }

    private void loadTop() {
        ToggleGroup topGroup = new ToggleGroup();
        ToggleButton operate = ToolComponent.largeToggleButton(topGroup, i18n(I18n.TAB_OPERATE), toolService.isDark());
        ToggleButton other = ToolComponent.largeToggleButton(topGroup, i18n(I18n.TAB_OTHER), toolService.isDark());
        ToggleButton ini = ToolComponent.largeToggleButton(topGroup, i18n(I18n.TAB_INI), toolService.isDark());
        topGroup.selectToggle(operate);

        Region spacer = new Region();

        Button create = ToolComponent.largeButton(i18n(I18n.CREATE), toolService.isDark());
        Button icon = ToolComponent.themeIcon(toolService.isDark());

        HBox top = new HBox(operate, other, ini, spacer, create, icon);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.setStyle("-fx-spacing: 10px;-fx-padding: 10px;-fx-alignment: center;");

        body.setTop(top);
    }

    private String i18n(I18n i18n) {
        return ToolCommon.Language.EN_US.type().equals(toolService.getConfig().core.language.data) ? i18n.EN() : i18n.ZH();
    }
}
