package dev.xinxin.module.modules.world;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.attack.EventAttack;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.Island.Island;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.TimeHelper;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class AutoLFix extends Module {

    public final BoolValue fake = new BoolValue("fake", true); // 新增fake选项
    public static final String[] messages = new String[] {
            "@花雨庭最强38.50无需脱盒丨你怎么被欣欣最强的客户端拷打了呢 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 这就是花雨庭最强对刀客户端 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 你的付费客户端怎么打不过欣欣公益呢 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 现在知道啥是花雨庭最强金苹果了吧 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 花雨庭第一大端 用户最多 我们也有布吉岛客户端呢 客户端最稳定且最暴力 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 你也是公益客户端 为什么不来用最强的欣欣啊 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 你还不知道欣欣公益无需脱盒吗 无需脱盒工具箱 我们也有布吉岛客户端呢 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 全网首发空岛破甲 我们也有布吉岛客户端呢 客户端最稳定且最暴力 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 全网首发起床跑吃 我们也有布吉岛客户端呢 客户端最稳定且最暴力 免费获取点击右边代码heshuyou.xyz ",
            "@[欣欣公益38.50无需脱盒] 全网首发无需盒 我们也有布吉岛客户端呢 客户端最稳定且最暴力 免费获取点击右边代码heshuyou.xyz ",
    };

    public AutoLFix() {
        super("AutoLFix", Category.Combat,"击杀嘲讽");
    }


    private Entity target;

    private final TimeHelper timeHelper = new TimeHelper();

    private int kill;

    public static EntityPlayer getSB() {
        return mc.thePlayer;
    }

    @Override
    public void onDisable() {
        if (LiveClient.INSTANCE == null || LiveClient.INSTANCE.liveUser == null) return;

        final LiveUser.Level userLevel = LiveClient.INSTANCE.liveUser.getLevel();

        if (userLevel != LiveUser.Level.LITTLE_FANS && userLevel != LiveUser.Level.ADMINISTRATOR) {
            String message;

            switch (userLevel) {
                case FREE:
                    message = "§a公益§f用户无法关闭击杀嘲讽！";
                    break;
                case SUPER_FANS:
                    message = "§d大粉丝§f无法关闭击杀嘲讽！";
                    break;
                case FREE_KILLER:
                    message = "§4§l公益粉丝杀手§f无法关闭击杀嘲讽！";
                    break;
                case PAID:
                    message = "§e内部§f用户无法关闭击杀嘲讽！";
                    break;
                default:
                    message = "§f您的权限不足，无法关闭击杀嘲讽！";
                    break;
            }

            HelperUtil.sendMessage(message);
        }
    }


    @EventTarget
    private void onAttack(EventAttack eventAttack) {
        target = eventAttack.getTarget();
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (target != null && target.isDead) {
            kill++;
            String victimName = target.getName();
            if (timeHelper.delay(1000)) {
                String message = messages[ThreadLocalRandom.current().nextInt(messages.length)] + kill;
                if (!fake.getValue() || !isOwnMessage(message)) {
                    mc.thePlayer.sendChatMessage(message);
                }
                try {
                    if (dev.xinxin.SilenceFix.instance != null && dev.xinxin.SilenceFix.instance.island != null) {
                        dev.xinxin.SilenceFix.instance.island.addIsland(Island.IslandType.SUCCESS, "击杀成功", "已击杀: " + victimName + " 连杀: " + kill, 2000);
                    }
                } catch (Throwable ignored) {}
                timeHelper.reset();
            }
            target = null;
        }
    }


    private boolean isOwnMessage(String message) {
        for (String msg : messages) {
            if (message.contains(msg)) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onWold(EventWorldLoad eventWorldLoad) {
        target = null;
        kill = 0;
        timeHelper.reset();
    }

    public static String setSuffix() {
        return "你真的很想和欣欣哥哥doi吗？";
    }

    public static String LLL() { return "setSuffix的返回值是你妈的名字嘻嘻嘻~"; }
}