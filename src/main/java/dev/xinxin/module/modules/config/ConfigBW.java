package dev.xinxin.module.modules.config;

import dev.xinxin.SilenceFix;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;

public class ConfigBW extends Module {

    public ConfigBW() {
        super("-----Bedwars-----", Category.Config, "------起床模式------");
        setBindable(false); // 👈 不允许绑定
    }

    @Override
    public void onEnable() {
        SilenceFix.instance.configManager.loadUserConfig("bw.json");
        super.onEnable();
        NotificationManager.post(NotificationType.SUCCESS, "Config", "您的配置切换成功！");
    }
}
