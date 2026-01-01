package dev.xinxin.module.modules.config;

import dev.xinxin.SilenceFix;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;

public class ConfigBWBEST extends Module {

    public ConfigBWBEST() {
        super("BetterRenderForBedwars", Category.Config, "------起床高性能模式------");
        setBindable(false); // 👈 不允许绑定
    }


    @Override
    public void onEnable() {
        SilenceFix.instance.configManager.loadUserConfig("bwbest.json");
        super.onEnable();
        NotificationManager.post(NotificationType.SUCCESS, "Config", "您的配置切换成功！");
    }
}
