package cn.nap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.nap.ToolCommon.I18n;
import static cn.nap.ToolCommon.Theme;
import static cn.nap.ToolCommon.Language;

public class ToolService {
    private ToolConfig config;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private boolean win11;

    public void loadConfig() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        try {
            win11 = ToolUtil.isWin11();
            List<ToolUtil.SectionObj> sectionList = ToolUtil.readConfig(ToolCommon.DEFAULT_CONFIG_FILE);
            config = parseConfig(sectionList);
        } catch (Exception e) {
            config = initDefaultConfig();
        } finally {
            loading.set(false);
        }
    }

    public boolean isConfigLoading() {
        return loading.get();
    }

    public ToolConfig getConfig() {
        return config;
    }

    public boolean isWin11() {
        return win11;
    }

    public boolean isDark() {
        return Theme.DARK.type().equals(config.core.theme.data);
    }

    public void changeTheme() {
        config.core.theme.data = isDark() ? Theme.LIGHT.type() : Theme.DARK.type();
    }

    private ToolConfig parseConfig(List<ToolUtil.SectionObj> sectionList) {
        if (sectionList == null || sectionList.isEmpty()) {
            return initDefaultConfig();
        }
        ToolConfig config = new ToolConfig();
        config.instances = new ArrayList<>();
        for (int i = 0; i < sectionList.size(); i++) {
            ToolUtil.SectionObj sectionObj = sectionList.get(i);
            if (I18n.CORE_SECTION.ZH().equals(sectionObj.section) || I18n.CORE_SECTION.EN().equals(sectionObj.section)) {
                config.core = ToolUtil.parseCoreSection(sectionObj, i);
            } else {
                config.instances.add(ToolUtil.parseInstanceSection(sectionObj, i));
            }
        }
        return config;
    }

    private ToolConfig initDefaultConfig() {
        ToolConfig config = new ToolConfig();
        config.core = initDefaultCore();
        config.instances = initDefaultInstances();
        return config;
    }

    private ToolConfig.Core initDefaultCore() {
        return new ToolConfig.Core(
                new ToolConfig.Info<>(I18n.CORE_SECTION.EN(), 0, I18n.TOOL_CONFIG_COMMAND.comment()),
                new ToolConfig.Info<>(Language.ZH_CN.type(), 0, I18n.LANGUAGE_COMMAND.comment()),
                new ToolConfig.Info<>(Theme.LIGHT.type(), 1, I18n.THEME_COMMAND.comment())
        );
    }


    private List<ToolConfig.Instance> initDefaultInstances() {
        AtomicInteger instanceSort = new AtomicInteger(0);
        AtomicInteger mysql8Sort = new AtomicInteger(0);
        ToolConfig.Instance mysql8 = new ToolConfig.Instance();
        mysql8.section = new ToolConfig.Info<>(I18n.MYSQL8_SECTION.EN(), instanceSort.incrementAndGet(), I18n.NAME_COMMAND.comment());
        mysql8.path = new ToolConfig.Info<>("mysql-8.0.39-winx64", mysql8Sort.incrementAndGet(), I18n.PATH_COMMAND.comment());
        mysql8.username = new ToolConfig.Info<>("root", mysql8Sort.incrementAndGet(), I18n.USERNAME_COMMAND.comment());
        mysql8.password = new ToolConfig.Info<>("root", mysql8Sort.incrementAndGet(), I18n.PASSWORD_COMMAND.comment());
        mysql8.port = new ToolConfig.Info<>("3306", mysql8Sort.incrementAndGet(), I18n.PORT_COMMAND.comment());
        mysql8.database = new ToolConfig.Info<>("beidou", mysql8Sort.incrementAndGet(), I18n.DATABASE_COMMAND.comment());

        AtomicInteger mysql5Sort = new AtomicInteger(0);
        ToolConfig.Instance mysql5 = new ToolConfig.Instance();
        mysql5.section = new ToolConfig.Info<>(I18n.MYSQL5_SECTION.EN(), instanceSort.incrementAndGet(), I18n.NAME_COMMAND.comment());
        mysql5.path = new ToolConfig.Info<>("mysql-5.7.44-winx64", mysql5Sort.incrementAndGet(), I18n.PATH_COMMAND.comment());
        mysql5.username = new ToolConfig.Info<>("root", mysql5Sort.incrementAndGet(), I18n.USERNAME_COMMAND.comment());
        mysql5.password = new ToolConfig.Info<>("root", mysql5Sort.incrementAndGet(), I18n.PASSWORD_COMMAND.comment());
        mysql5.port = new ToolConfig.Info<>("3307", mysql5Sort.incrementAndGet(), I18n.PORT_COMMAND.comment());
        mysql5.database = new ToolConfig.Info<>("napms", mysql5Sort.incrementAndGet(), I18n.DATABASE_COMMAND.comment());

        List<ToolConfig.Instance> instanceList = new ArrayList<>();
        instanceList.add(mysql8);
        instanceList.add(mysql5);
        return instanceList;
    }

}