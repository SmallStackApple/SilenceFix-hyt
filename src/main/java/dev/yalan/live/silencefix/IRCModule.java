package dev.yalan.live.silencefix;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.misc.ClientUtils;

public class IRCModule extends Module {
    public static IRCModule Instance;
    public IRCModule(){
        super("IRC", Category.Misc,"在线聊天");
        Instance = this;
    }
    @Override
    public void onDisable() {
        ClientUtils.sendMessage(LiveClient.INSTANCE.liveUser.getLevel().getDefaultRank() + "§f用户无法关闭IRC服务器");
    }
}
