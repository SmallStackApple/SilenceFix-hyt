package dev.xinxin.module.modules.combat;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;

public class LiveFriendly extends Module {
    public static LiveFriendly Instance;

    public boolean disablable = true;

    public LiveFriendly() {
        super("NoAuraIRC", Category.Combat,"好友模式");
        Instance = this;
    }


    @Override
    public void onDisable() {
        if (LiveFriendly.Instance != null && LiveClient.INSTANCE.liveUser != null) {
            LiveUser.Level level = LiveClient.INSTANCE.liveUser.getLevel();
            if (level == LiveUser.Level.ADMINISTRATOR) {
                return;
            }
        }
    }


  /*@Override
  public void onDisable() {
      HelperUtil.sendMessage("§b§l您已关闭友好模式，现在您的杀戮可以攻击公益了！");
      HelperUtil.sendMessage("§b§l您已关闭友好模式，现在您的杀戮可以攻击公益了！");
  }
    @Override
    public void onEnable() {
        HelperUtil.sendMessage("§a§l您已开启友好模式，现在您的杀戮不能攻击公益了！");
        HelperUtil.sendMessage("§a§l您已开启友好模式，现在您的杀戮不能攻击公益了！");
    }*/

}

