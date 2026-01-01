package dev.xinxin.module.modules.world;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;

public class AdminPanel extends Module {
    public AdminPanel() {
        super("AdminPanel", Category.Render, "IRC管理面板");
    }

    @Override
    public void onEnable() {
        final LiveUser.Level userLevel = LiveClient.INSTANCE.liveUser.getLevel();
        if (userLevel == LiveUser.Level.ADMINISTRATOR || userLevel == LiveUser.Level.PAID) {
            if (mc.thePlayer != null && mc.theWorld != null) {
                mc.displayGuiScreen(new dev.xinxin.gui.clickgui.irc.AdminPanel());
            }
        } else {
            HelperUtil.sendMessage("§f只有内部或管理员可以使用");
            if (mc.thePlayer != null && mc.theWorld != null) {
                mc.displayGuiScreen(new dev.xinxin.gui.clickgui.irc.AdminPanel());
            }
        }
        setState(false);
    }
}
