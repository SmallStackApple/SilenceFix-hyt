package dev.xinxin.module.modules.misc;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.Island.Island;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.player.ChestStealer;
import dev.xinxin.module.modules.world.ChestAura;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class AutoReport extends Module {
    public static AutoReport Instance;
    private static final String[] REPORT_TYPE_NAMES = {
            "杀戮光环", "广告言论", "不雅言论", "其他言论"
    };
    private static final int[] REPORT_TYPES = {
            11, 20, 21, 24 // 对应MC的举报类型ID
    };

    public final ArrayDeque<String> awaitReportPlayers = new ArrayDeque<>();
    private final HashSet<String> reportedPlayers = new HashSet<>();
    private long lastActionTime = 0;
    private boolean isWaitingResponse = false;
    private boolean paused = false;
    private long pauseEndTime = 0;
    public static NetworkPlayerInfo currentTarget; // 当前正在举报的玩家
    public static boolean disable; // 是否临时禁用其他模块

    public AutoReport() {
        super("AutoReport", Category.Misc, "自动举报","Report All PeoPLes","举报所有何树友");

        Instance = this;
    }

    @Override
    public void onEnable() {
        reportedPlayers.clear();
        lastActionTime = System.currentTimeMillis();
        disable = false; // 启用时重置禁用标志
    }

    @EventTarget
    public void onWorldLoad(EventWorldLoad e) {
        reportedPlayers.clear();
        resetState();
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (SilenceFix.instance.moduleManager.getModule(Scaffold.class).state) return;
        if (ChestStealer.working || ChestAura.confirmedOpenedContainers.contains(ChestStealer.currentContainerPos)) {
            return;// 左脑攻击右脑
        }
        if (!KillAura.targets.isEmpty() && KillAura.target.hurtTime > 0 ||
                mc.currentScreen != null ||
                isWaitingResponse ||
                System.currentTimeMillis() - lastActionTime < 20000) {
            return;
        }


        if (ModuleManager.getModule(AutoGapple.class).getState() ||
                ModuleManager.getModule(Scaffold.class).getState()) {
            return;
        }

        List<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap()
                .stream()
                .filter(info -> !info.getGameProfile().getId().equals(mc.thePlayer.getUniqueID()))
                .toList();

        if (players.isEmpty()) {
            lastActionTime = System.currentTimeMillis();
            return;
        }

        final String awaitReportPlayer = awaitReportPlayers.pollFirst();
        if (awaitReportPlayer != null) {
            for (NetworkPlayerInfo info : players) {
                if (Objects.equals(info.getGameProfile().getName(), awaitReportPlayer)) {
                    startReport(info);
                    return;
                }
            }
        }

        for (NetworkPlayerInfo info : players) {
            if (info.isHacker) {
                String name = info.getGameProfile().getName();
                if (!reportedPlayers.contains(name)) {
                    startReport(info);
                    return;
                }

                return;
            }
        }

        for (NetworkPlayerInfo info : players) {
            String name = info.getGameProfile().getName();
            if (!reportedPlayers.contains(name)) {
                startReport(info);
                return;
            }
        }

        HelperUtil.sendMessage("§f所有何树友都被举报了 等待新的何树友进入游戏吧！");
        HelperUtil.sendMessage("§f所有何树友都被举报了 等待新的何树友进入游戏吧！");
        NotificationManager.post(NotificationType.WARNING, "AutoReport", "无法重复举报同一个何树友！！", 5);
        lastActionTime = System.currentTimeMillis();
    }

    private void startReport(NetworkPlayerInfo target) {
        String name = target.getGameProfile().getName();
        reportedPlayers.add(name);
        currentTarget = target; // 设置当前目标
        isWaitingResponse = true;
        disable = true; // 阻止其他模块干扰

        mc.thePlayer.sendChatMessage("/report " + EnumChatFormatting.getTextWithoutFormattingCodes(name));
        lastActionTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onPacket(EventPacketReceive e) {
        if (!isWaitingResponse || currentTarget == null) return;
        if (e.getPacket() instanceof S2DPacketOpenWindow) {
            S2DPacketOpenWindow packet = (S2DPacketOpenWindow) e.getPacket();
            String title = packet.getWindowTitle().getUnformattedText();
            if (!title.contains("请选择举报理由")) {
                return;
            }
            e.setCancelled(true);
            int reasonIndex;
            if (currentTarget.isHacker) {
                reasonIndex = 1;
            } else {
                do {
                    reasonIndex = ThreadLocalRandom.current().nextInt(REPORT_TYPES.length);
                } while (reasonIndex == 1);
            }
            mc.playerController.windowClick(
                    packet.getWindowId(),
                    REPORT_TYPES[reasonIndex], 0, 1, mc.thePlayer
            );
            HelperUtil.sendMessage(String.format(
                    "§a成功举报 §e%s §b(原因: %s)",
                    currentTarget.getGameProfile().getName(),
                    REPORT_TYPE_NAMES[reasonIndex]
            ));
            String playerName = currentTarget.getGameProfile().getName();
            String reportReason = REPORT_TYPE_NAMES[reasonIndex];
            NotificationManager.post(NotificationType.SUCCESS,
                    "自动举报成功",
                    "已举报何树友: " + playerName + "\n原因: " + reportReason,
                    5);
            try {
                if (SilenceFix.instance != null && SilenceFix.instance.island != null) {
                    SilenceFix.instance.island.addIsland(Island.IslandType.SUCCESS, "自动举报成功", "已举报何树友: " + playerName + " 原因: " + reportReason, 2000);
                }
            } catch (Throwable ignored) {}
            resetState();
        } else if (e.getPacket() instanceof S02PacketChat) {
            String msg = ((S02PacketChat) e.getPacket()).getChatComponent().getUnformattedText();
            if (msg.contains("操作过快") || msg.contains("不在线")) {
                NotificationManager.post(NotificationType.WARNING, "qvq", "打字时无法自动举报何树友", 6);
                resetState();
                e.setCancelled(true);
            }
        }
    }



    private void resetState() {
        currentTarget = null;
        isWaitingResponse = false;
        disable = false; // 重置禁用标志
        lastActionTime = System.currentTimeMillis();
    }
}