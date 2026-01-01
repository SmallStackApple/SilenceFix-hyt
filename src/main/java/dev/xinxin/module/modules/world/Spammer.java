package dev.xinxin.module.modules.world;

import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Spammer extends Module implements Runnable {

    public final BoolValue fake = new BoolValue("fake", true);
    public final BoolValue backgroundEnabled = new BoolValue("back", true);
    private static final List<String> ads1 = new ArrayList<>(); // 广告列表1
    private static final List<String> ads2 = new ArrayList<>(); // 广告列表2
    private static final List<String> allAds = new ArrayList<>();
    private volatile int currentIndex = 0; // 当前广告索引
    private final AtomicBoolean isRunning = new AtomicBoolean(true); // 线程运行标志
    private static final Minecraft mc = Minecraft.getMinecraft();

   static {
        ads1.add("[SilenceFix48.0无需脱盒] 免费且最强的客户端 我们也有布吉岛客户端呢 点击右边的代码heshuyou.xyz 来获取！");
        ads1.add("SilenceFix Best Config Free 1055680656 ><");
       ads1.add("[SilenceFix48.0无需脱盒] 我们免费提供高强度客户端混淆 zkm+jnic且正版 我们也有布吉岛客户端呢 点击右边的代码heshuyou.xyz 来获取！");
       ads1.add("[欣欣公益48.0无需脱盒] 全网首发最强的起床跑吃跑喝 我们也有布吉岛客户端呢 免费获取点击右边的代码heshuyou.xyz ><");
        ads1.add("SilenceFix Best The Config Free 1055680656 ><");
        ads1.add("[欣欣公益48.0无需脱盒] 全网首发内置进服 无需脱盒 我们也有布吉岛客户端呢 免费获取点击右边的代码heshuyou.xyz ><");
        ads1.add("[SilenceFix48.0无需脱盒] 你的付费客户端被公益追着打 好丢人哇T-T 我们也有布吉岛客户端呢 点击右边的代码heshuyou.xyz 来获取！");
        ads1.add("[欣欣公益48.0无需脱盒] 全网首发空岛破甲 我们也有布吉岛客户端呢 免费获取点击右边的代码heshuyou.xyz ><");
        ads1.add("[SilenceFix48.0无需脱盒] 你的付费客户端被公益追着打哇 好丢人T-T 我们也有布吉岛客户端呢 点击右边的代码heshuyou.xyz 来获取！");
        ads1.add("SilenceFix Best The Config Free 1055680656 ><");
       ads1.add("[SilenceFix48.0无需脱盒] 我们免费提高强度供客户端混淆 zkm+jnic且正版 我们也有布吉岛客户端呢 点击右边的代码heshuyou.xyz 来获取！");

        ads2.add("欣欣公益48.0 全天免费的内置进服花雨庭 学生党可以放学游玩花雨庭！快来免费获取吧 我们也有最强的布吉岛 免费点击代码heshuyou.xyz ><");
        ads2.add("欣欣公益48.0 全天免费的内置进服花雨庭 看到了就赶快加入我们一起免费使用并获取吧 我们也有最强的布吉岛 免费点击代码heshuyou.xyz ><");
        ads2.add("SilenceFix Best The Config Free 1055680656 ><");
        ads2.add("欣欣公益48.0 全天免费的内置进服花雨庭 全网独家起床20CPS最强客户端 不服同装备对刀一下吗 同距离无敌 我们也有最强的布吉岛 免费点击heshuyou.xyz ><");
        allAds.addAll(ads1);
        allAds.addAll(ads2);
    }



    public Spammer() {
        super("Spammer", Category.Misc, "宣传");
        new Thread(this, "SpammerThread").start();
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                // ✅ 如果不允许后台宣传，并且游戏没有焦点，则跳过
                if (!backgroundEnabled.getValue() && !mc.inGameHasFocus) {
                    Thread.sleep(5000);
                    continue;
                }

                long delay = getDynamicDelay();
                Thread.sleep(delay);

                if (canSend()) {
                    sendChatMessage();
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {}
            }
        }
    }


    /**
     * 根据当前IRC在线人数动态计算发送延迟
     */
    private long getDynamicDelay() {
        try {
            if (mc.theWorld == null || mc.getNetHandler() == null) {
                return 3000L; // 默认3秒
            }
            int worldIRCPlayers = 0;
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer)entity;
                    NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                    if (info != null && info.liveUser != null) {
                        worldIRCPlayers++;
                        if (worldIRCPlayers >= 3) break;
                    }
                }
            }

            return worldIRCPlayers >= 3 ? 8000L : 3000L; // 人多时延迟8秒，人少时3秒
        } catch (Exception e) {
            return 3000L; // 异常时默认3秒
        }
    }
    private boolean canSend() {
        if (mc.currentScreen instanceof GuiChat) return false;
        return state && mc.thePlayer != null && mc.theWorld != null;
    }
    private void sendChatMessage() {
        List<String> currentAds = getCurrentAds();
        String message = "@" + currentAds.get(currentIndex);
        currentIndex = (currentIndex + 1) % currentAds.size();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message); // 这才是让玩家发送消息
        }
    }
    public static List<String> getCurrentAds() {
        return LiveClient.INSTANCE != null && LiveClient.INSTANCE.clientSetting.neteaseFree ? ads2 : ads1;
    }

    public static List<String> getAllAds() {
        return allAds;
    }

    @Override
    public void onDisable() {
        if (LiveClient.INSTANCE == null || LiveClient.INSTANCE.liveUser == null || LiveClient.INSTANCE.liveUser.getLevel() == null) {
//            setState(true); // 强制保持开启
            return;
        }

        LiveUser.Level userLevel = LiveClient.INSTANCE.liveUser.getLevel();
        if (userLevel != LiveUser.Level.LITTLE_FANS && userLevel != LiveUser.Level.ADMINISTRATOR ) {
//            setState(true); // 强制保持开启
            String message;

            switch (userLevel) {
                case FREE:
                    message = "§a公益§f用户无法关闭宣传！";
                    break;
                case SUPER_FANS:
                    message = "§d大粉丝§f无法关闭宣传！";
                    break;
                case FREE_KILLER:
                    message = "§4§l公益粉丝杀手§f无法关闭宣传！";
                    break;
                case PAID:
                    message = "§e内部§f用户无法关闭宣传！";
                    break;
                default:
                    message = "§f您的权限不足，无法关闭宣传！";
                    break;
            }

            HelperUtil.sendMessage(message);
        }
    }


    public static String LLL() {
        return "setSuffix的返回值是你妈的名字嘻嘻嘻~";
    }
}