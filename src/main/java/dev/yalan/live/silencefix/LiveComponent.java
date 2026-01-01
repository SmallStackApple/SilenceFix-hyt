package dev.yalan.live.silencefix;

import cn.dev.annotations.JNICExclude;
import cn.dev.annotations.JNICInclude;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.api.types.Priority;
import dev.xinxin.event.misc.EventChatReceive;
import dev.xinxin.event.misc.EventTeleport;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventPlayerDead;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.altmanager.GuiAltLogin;
import dev.xinxin.module.ModuleManager;
import dev.xinxin.module.modules.combat.LiveFriendly;
import dev.xinxin.module.modules.misc.AutoReport;
import dev.xinxin.module.modules.movement.Fly;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.player.BlockUtil;
import dev.yalan.live.silencefix.events.EventLiveAuthenticationResult;
import dev.yalan.live.silencefix.events.EventLiveChannelInactive;
import dev.yalan.live.silencefix.events.EventLiveGenericMessage;
import dev.yalan.live.silencefix.netty.LiveProto;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@JNICInclude
public class LiveComponent {
    private static final Logger logger = LogManager.getLogger("LiveComponent");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final JsonParser jsonParser = new JsonParser();
    private final LiveClient live;

    private final ArrayList<TickScheduledTask> scheduledTasks = new ArrayList<>();
    private final HashMap<UUID, Consumer<String>> commandOutMap = new HashMap<>();
    private final HashMap<String, HackerPlayer.ClientId> clientChatMatchMap = new HashMap<>();
    private final HashMap<String, HackerPlayer> hackerMap = new HashMap<>();
    private final HashMap<String, Integer> listenChatPlayers = new HashMap<>();
    private boolean lastLiveFriendlyCheckState;
    private boolean isBedWarStarted;
    private Instant bedWarStartTime;
    private int ticks;

    LiveComponent(LiveClient live) {
        this.live = live;
        this.clientChatMatchMap.put("southside", HackerPlayer.ClientId.SOUTHSIDE);
        this.clientChatMatchMap.put("卧龙出山", HackerPlayer.ClientId.SOUTHSIDE);
        this.clientChatMatchMap.put("少羽", HackerPlayer.ClientId.SOUTHSIDE);
        this.clientChatMatchMap.put("guradfix", HackerPlayer.ClientId.GUARD_FIX);

        EventManager.register(this);
    }

    @EventTarget
    private void onLiveAuthenticationResult(EventLiveAuthenticationResult e) {
        if (e.isSuccess()) {
            if (mc.theWorld != null) {
                HelperUtil.sendMessage("§f重连成功!");

                if (mc.thePlayer != null) {
                    LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateMinecraftProfile(
                            mc.thePlayer.getUniqueID(),
                            mc.thePlayer.getName()
                    ));
                }
            }
        } else {
            if (LiveClient.INSTANCE.autoUsername != null && LiveClient.INSTANCE.autoPassword != null) {
                HelperUtil.sendMessage("§f停止重连: 自动凭据失效");

                live.stopReconnectionThread();
            }
        }
    }

    @EventTarget
    private void onLiveGenericMessage(EventLiveGenericMessage e) {
        if ("GetNeteaseCookie".equals(e.getChannel())) {
            if (mc.currentScreen instanceof GuiAltLogin al) {
                al.status = e.getMessage();
                al.getCookieButton.enabled = true;
            }

            return;
        }

        if (mc.theWorld != null) {
            printSimpleChat("服务器", e.getChannel() + "-" + e.getMessage());
        }
    }

    @EventTarget
    private void onLiveChannelInactive(EventLiveChannelInactive e) {
        commandOutMap.clear();

        if (mc.theWorld != null) {
            HelperUtil.sendMessage("§f与Live失去连接...");
        }
    }

    @EventTarget
    public void tickLive(EventTick e) {
        if (LiveClient.INSTANCE == null || LiveClient.INSTANCE.liveUser == null) {
            //mc.shutdown();
            return;
        }

        if (ticks % 20 == 0) {
            LiveClient.INSTANCE.sendPacket(LiveProto.createQueryOnlineUsersCount());
        }

        listenChatPlayers.values().removeIf((it) -> it < ticks);

        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (!scheduledTasks.isEmpty()) {
            for (TickScheduledTask scheduledTask : scheduledTasks) {
                if (scheduledTask.tick <= ticks) {
                    try {
                        scheduledTask.runnable.run();
                    } catch (Throwable t) {
                        logger.error("Can't execute scheduled task", t);
                    }
                }
            }

            scheduledTasks.removeIf(task -> task.tick <= ticks);
        }

        if (mc.theWorld != null) {
            if (bedWarStartTime != null) {
                final Instant now = Instant.now();
                if (bedWarStartTime.plus(Duration.ofMinutes(6)).isBefore(now)
                        && bedWarStartTime.plus(Duration.ofMinutes(6)).plusMillis(30).isAfter(now)) {
                    HelperUtil.sendMessage("§a 6分钟保护已过，现在可以挖床了！");
                    HelperUtil.sendMessage("§a 6分钟保护已过，现在可以挖床了！");
                    HelperUtil.sendMessage("§a 6分钟保护已过，现在可以挖床了！");
                }
            }

            if (mc.theWorld.worldTicks % 100 == 0) {
                for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    LiveClient.INSTANCE.sendPacket(LiveProto.createQueryMinecraftProfile(info.getGameProfile()));
                }
            }

            if ((mc.theWorld.worldTicks > 60 && !mc.theWorld.hasQueriedPlayerBed)
                    || mc.theWorld.worldTicks % 100 == 0) {
                mc.theWorld.hasQueriedPlayerBed = true;

                for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    if (info.getGameType() == WorldSettings.GameType.SPECTATOR && info.liveUser == null) continue;
                    LiveClient.INSTANCE.sendPacket(LiveProto.createQueryPlayerBed(EntityPlayer.getUUID(info.getGameProfile())));
                }
            }
        }

        ticks++;
    }

    @EventTarget
    public void onPlayerDead(EventPlayerDead e) {
        listenChatPlayers.put(e.getPlayer().getName(), ticks + 40);
    }

    @EventTarget
    public void onChat_ListenChat(EventChatReceive e) {
        final String message = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        final String killerID = extractPlayerId(message);

        if (killerID == null || killerID.length() > 12) {
            return;
        }

        if (Objects.equals(killerID, message)) {
            return;
        }

        if (Objects.equals(killerID, mc.thePlayer.getGameProfile().getName())) {
            return;
        }

        final String messageLowerCase = message.toLowerCase();
        for (Map.Entry<String, HackerPlayer.ClientId> entry : clientChatMatchMap.entrySet()) {
            if (messageLowerCase.contains(entry.getKey())) {
                hackerMap.put(killerID, new HackerPlayer(entry.getValue(), killerID));
                reportPlayer(killerID);

                e.setCancelled();
                return;
            }
        }

        for (String playerName : listenChatPlayers.keySet()) {
            if (message.contains(playerName)) {
                hackerMap.put(killerID, new HackerPlayer(HackerPlayer.ClientId.OTHERS, killerID));
                reportPlayer(killerID);

                e.setCancelled();
                break;
            }
        }
    }

    private void reportPlayer(String name) {
        AutoReport.Instance.awaitReportPlayers.add(name);
    }

    @EventTarget
    public void onTeleport(EventTeleport e) {
        if (isBedWarStarted) {
            scheduledTasks.add(new TickScheduledTask(20, () -> {
                final BlockPos bedPos = BlockUtil.findBlock(Blocks.bed, 32);
                if (bedPos != null) {
                    HelperUtil.sendMessage("§f哇咔咔游戏开始了耶！");
                    LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateBedPos(false, mc.thePlayer.getUniqueID(), bedPos));
                }
            }));

            isBedWarStarted = false;
            bedWarStartTime = Instant.now();

            executeAtTickDelayed(6 * 60 * 20, () -> {
                final LiveFriendly liveFriendly = ModuleManager.getModule(LiveFriendly.class);
//                liveFriendly.disablable = true;
//                liveFriendly.setState(false);

                lastLiveFriendlyCheckState = false;
                HelperUtil.sendMessage("§f6分钟保护已过，友好模式已强制关闭！");
                HelperUtil.sendMessage("§f6分钟保护已过，友好模式已强制关闭！");
                HelperUtil.sendMessage("§f6分钟保护已过，友好模式已强制关闭！");
            });
        }
    }

    @EventTarget
    public void onWorldLoad(EventWorldLoad e) {
        bedWarStartTime = null;
        isBedWarStarted = false;
        scheduledTasks.clear();
        HelperUtil.sendMessage("§c地图重载，6分钟保护已被重置（计时归零）。");

        if (e.getWorld() == null) {
            LiveClient.INSTANCE.sendPacket(LiveProto.createRemoveMinecraftProfile());
            LiveClient.INSTANCE.sendPacket(LiveProto.createUpdateBedPos(true, LiveProto.NULL_UUID, BlockPos.ORIGIN));
        }
    }

    @EventTarget
    public void onChat(EventChatReceive e) {
        if (e.type == 2) {
            return;
        }

        final String message = e.message.getFormattedText();

        if (message.contains("§b起床战争§r§7>>§r§f §r§a游戏开始 ...§r")) {
            isBedWarStarted = true;
            HelperUtil.sendMessage("§f检测到游戏开始，6分钟保护计时启动！");
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        final LiveFriendly liveFriendly = LiveFriendly.Instance;

//        if (LiveClient.INSTANCE.liveUser.getLevel().isGreaterThanFF()) {
//            return;
//        }

        final List<NetworkPlayerInfo> list = mc.getNetHandler().getPlayerInfoMap()
                .stream()
                .filter(playerInfo -> {
                    if (Objects.equals(EntityPlayer.getUUID(playerInfo.getGameProfile()), mc.thePlayer.getUniqueID())) {
                        return false;
                    }

                    return playerInfo.getGameType() != WorldSettings.GameType.SPECTATOR && playerInfo.getGameType() != WorldSettings.GameType.CREATIVE;
                })
                .toList();

        final boolean allLive;
        final boolean noneLive;

        int countOfLiveUsers = 0;
        for (NetworkPlayerInfo info : list) {
            if (info.liveUser != null) {
                countOfLiveUsers++;
            }
        }

        if (countOfLiveUsers == list.size()) {
            allLive = true;
            noneLive = false;
        } else if (countOfLiveUsers == 0) {
            allLive = false;
            noneLive = true;
        } else {
            allLive = false;
            noneLive = false;
        }

        if (allLive || noneLive) {
            if (lastLiveFriendlyCheckState) {
                lastLiveFriendlyCheckState = false;
//                liveFriendly.disablable = true;
//                liveFriendly.setState(false);

                final Blink blink = ModuleManager.getModule(Blink.class);
                if (blink != null && blink.getState()) {
                    blink.setState(false);
                }

                final Fly fly = ModuleManager.getModule(Fly.class);
                if (fly != null && fly.getState()) {
                    fly.setState(false);
                }

                for (int i = 0; i < 6; i++) {
                    HelperUtil.sendMessage("§f§l已为您关闭友好模式 现在您可以攻击自己人了！");
                }
            }
        } else {
            if (!lastLiveFriendlyCheckState) {
                lastLiveFriendlyCheckState = true;
//                liveFriendly.disablable = false;
//                liveFriendly.setState(true);

                for (int i = 0; i < 3; i++) {
                    HelperUtil.sendMessage("§4已为您开启友好模式 现在您无法攻击自己人了！");
                }
            }
        }
    }

    @EventTarget(value = Priority.LOWEST)
    public void onAttack(EventPacketSend e) {
        if (mc.theWorld == null) {
            return;
        }

        if (e.getPacket() instanceof C02PacketUseEntity packet) {
            final Entity entity = packet.getEntityFromWorld(mc.theWorld);

            if (entity instanceof EntityPlayer player) {
                if (player.liveUser != null && isNotAttackable(player.liveUser)) {
                    e.setCancelled();

                    if (live.liveUser.getLevel().isLowerOrSame(player.liveUser.getLevel())) {
                        HelperUtil.sendMessage("§f你无法攻击" + player.liveUser.getLevel().getDefaultRank() + "§r等级的玩家!");
                    } else {
                        HelperUtil.sendMessage("§f友好模式状态下 无法攻击IRC的玩家！");
                    }
                }
            }
        }
    }

    public void onClientSettingUpdated(LiveClientSetting clientSetting) {
        if (!clientSetting.allowFreeAttackFree
                && live.clientSetting.allowFreeAttackFree
                && mc.theWorld != null
                && live.liveUser != null
                && live.liveUser.getLevel().isFreeOrFans()) {//友好模式
            mc.addScheduledTask(() -> {
                for (int i = 0; i < 3;  i++) {
                    HelperUtil.sendMessage("§f由于欣欣关闭公益打公益 您无法关闭友好模式了！");
                }
//                LiveFriendly.Instance.setState(true);
            });
        }
    }
    
    public void handleCommandOut(UUID executionId, String outType, String out) {
        switch (outType) {
            case "commandOut" -> {
                final Consumer<String> consumer = commandOutMap.get(executionId);

                if (consumer != null) {
                    consumer.accept(out);
                }
            }
            case "completeException" -> {
                final Consumer<String> consumer = commandOutMap.remove(executionId);

                if (consumer != null) {
                    consumer.accept(out);
                }
            }
            case "complete" -> commandOutMap.remove(executionId);
        }
    }

    public void handleQueryResultBedPos(UUID mcUUID, BlockPos pos) {
        if (mc.getNetHandler() == null) {
            return;
        }

        final NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mcUUID);

        if (playerInfo == null) {
            return;
        }

        playerInfo.bedPos = pos;
    }

    public void handleQueryResultMinecraftProfile(UUID mcUUID, String clientId, UUID userId, String userPayloadString) {
        final JsonObject payload = jsonParser.parse(userPayloadString).getAsJsonObject();
        final LiveUser liveUser = new LiveUser(clientId, userId, payload);

        if (mc.getNetHandler() != null) {
            final NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mcUUID);

            if (playerInfo != null) {
                playerInfo.liveUser = liveUser;
            }
        }

        if (mc.theWorld != null) {
            final EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(mcUUID);

            if (player != null) {
                player.liveUser = liveUser;
            }
        }
    }

    @JNICExclude
    public void handleChat(String channel, String payloadString) {
        final JsonObject payload = jsonParser.parse(payloadString).getAsJsonObject();

        switch (channel) {
            case "LivePublic" -> {
                final String clientId = payload.get("clientId").getAsString();
                final String message = payload.get("message").getAsString();

                if ("SilenceFix".equals(clientId)) {
                    printPublicChat(
                            clientId,
                            message,
                            payload.get("username").getAsString(),
                            payload.get("rank").getAsString(),
                            Optional.ofNullable(payload.get("mcName"))
                                    .map(JsonElement::getAsString)
                                    .orElse(null)
                    );
                } else {
                    printPublicChat(
                            clientId,
                            message,
                            payload.get("username").getAsString(),
                            Optional.ofNullable(payload.get("rank"))
                                    .map(JsonElement::getAsString)
                                    .orElse(null),
                            null
                    );
                }
            }
            case "ServerLog" -> {
                final String message = payload.get("message").getAsString();
                printSimpleChat(EnumChatFormatting.AQUA + "服务器", message);
            }
            case "Broadcast" -> {
            }
            case "UserStatus" -> {
                final String status = payload.get("status").getAsString();
                final String username = payload.get("username").getAsString();
                final LiveUser.Level level = LiveUser.Level.of(payload.get("level").getAsString());
                final String rank = payload.get("rank").getAsString();
                final StringBuilder sb = new StringBuilder();

                sb.append(level.getDefaultRank())
                        .append(EnumChatFormatting.RESET)
                        .append("用户");
                sb.append(username);

                if (!LiveUser.Level.isDefaultRank(rank)) {
                    sb.append(EnumChatFormatting.GRAY)
                            .append('[')
                            .append(EnumChatFormatting.RESET)
                            .append(rank)
                            .append(EnumChatFormatting.GRAY)
                            .append(']')
                            .append(EnumChatFormatting.RESET);
                } else {
                    sb.append(' ');
                }

                if ("online".equals(status)) {
                    sb.append(EnumChatFormatting.GREEN).append("加入了频道");
                } else if ("offline".equals(status)) {
                    sb.append(EnumChatFormatting.RED).append("离开了频道");
                } else {
                    sb.append(status);
                }


                printSimpleChat(EnumChatFormatting.AQUA + "服务器", sb.toString());
            }
        }
    }

    @JNICExclude
    private void printPublicChat(String clientId, String message, String username, String rank, String mcName) {
        final StringBuilder builder = new StringBuilder();

        builder.append(EnumChatFormatting.GRAY).append("[");
        builder.append(EnumChatFormatting.YELLOW).append("Live");
        builder.append(EnumChatFormatting.GRAY).append("] ");
        builder.append(EnumChatFormatting.RESET).append(username);

        if (rank != null) {
            builder.append(EnumChatFormatting.GRAY).append("(");
            builder.append(EnumChatFormatting.RESET).append(rank).append(EnumChatFormatting.RESET);
            builder.append(EnumChatFormatting.GRAY).append(")");
        }

        builder.append(EnumChatFormatting.GRAY).append(": ");
        builder.append(EnumChatFormatting.RESET).append(message);

        final ChatComponentText chatComponentText = new ChatComponentText(builder.toString());

        if (mcName != null) {
            final ChatStyle chatStyle = new ChatStyle();
            chatStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§f点击irc名字可以和§a" + username + "§f完成组队")));
            chatStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".teamup " + username + " " + mcName));
            chatComponentText.setChatStyle(chatStyle);
        }

        mc.ingameGUI.getChatGUI().printChatMessage(chatComponentText);
    }

    @JNICExclude
    private void printSimpleChat(String sender, String message) {
        final StringBuilder builder = new StringBuilder();

        builder.append(EnumChatFormatting.GRAY).append("[");
        builder.append(EnumChatFormatting.YELLOW).append("Live");
        builder.append(EnumChatFormatting.GRAY).append("-");
        builder.append(EnumChatFormatting.RESET).append(sender);
        builder.append(EnumChatFormatting.GRAY).append("]: ");
        builder.append(EnumChatFormatting.RESET).append(message);

        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(builder.toString()));
    }

    @JNICExclude
    public static String getLiveUserDisplayName(LiveUser liveUser) {
        final StringBuilder sb = new StringBuilder(64);
        final String rawName = liveUser.getName() == null ? "" : liveUser.getName();
        final String name = rawName.replaceAll("§.", "");
        final String rank = liveUser.getRank();

        sb.append(EnumChatFormatting.GRAY).append("[")
                .append(EnumChatFormatting.YELLOW).append("Live")
                .append(EnumChatFormatting.GRAY).append("] ")
                .append(EnumChatFormatting.WHITE).append(name);

        if (rank != null && !rank.isEmpty()) {
            sb.append(" ")
                    .append(EnumChatFormatting.GRAY).append("[")
                    .append(EnumChatFormatting.AQUA).append(rank)
                    .append(EnumChatFormatting.GRAY).append("]");
        }

        sb.append(EnumChatFormatting.RESET);
        return sb.toString();
    }



    public static boolean isSelectable(LiveUser target) {
        return !isNotSelectable(target);
    }

    public static boolean isNotSelectable(LiveUser target) {
        if (target == null) {
            return false;
        }

        if (SilenceFix.instance.ircFriends.contains(target.getName())) {
            return true;
        }

        return LiveComponent.isNotAttackable(target);
    }

    public static boolean isAttackable(LiveUser target) {
        return !isNotAttackable(target);
    }

    // 若返回 true
    // 则该LiveUser无法被选中及攻击
    public static boolean isNotAttackable(LiveUser target) {
        // 若输入为 null
        // 则直接指示可以攻击
        if (target == null) {
            return false;
        }

        // 若对方的等级为其他客户端或未知
        // 则直接指示可以攻击
        if (target.getLevel() == LiveUser.Level.FOREIGNER
                || target.getLevel() == LiveUser.Level.UNKNOWN) {
            return false;
        }

        // 若开启友好模式 则取消攻击
        if (LiveFriendly.Instance.getState()) {
            return true;
        }

        final LiveUser self = LiveClient.INSTANCE.liveUser;

        // 若自己的等级低于对方 则取消攻击
        if (self.getLevel().isLower(target.getLevel())) {
            return true;
        }

        // 若自己和对方的等级都为公益/粉丝 且友好模式开启
        // 那么我们在平级判断之前检查是否允许 allowFreeAttackFree
        // 若为true则可以攻击
        // 若为false则不可以攻击
        if (self.getLevel().isFreeOrFans()) {
            return !LiveClient.INSTANCE.clientSetting.allowFreeAttackFree;
        }

        // 若自己与对方平级 则取消攻击
        if (self.getLevel().getPriority() == target.getLevel().getPriority()) {
            return true;
        }

        return false;
    }

    public static NetworkPlayerInfo isBlockBreakable(BlockPos blockPos, IBlockState blockState, Block block) {
        if (block != Blocks.bed) {
            return null;
        }

        final Instant bedWarStartTime = LiveClient.INSTANCE.getLiveComponent().bedWarStartTime;
        if (bedWarStartTime != null && bedWarStartTime.plus(Duration.ofMinutes(6)).isBefore(Instant.now())) {
            return null;
        }

        final LiveUser self = LiveClient.INSTANCE.liveUser;

        if (self.getLevel() == LiveUser.Level.ADMINISTRATOR) {
            return null;
        }

        final EnumFacing facing = blockState.getValue(BlockBed.FACING);

        for (NetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            if (networkPlayerInfo.liveUser == null
                    || networkPlayerInfo.bedPos == null) {
                continue;
            }

            if (networkPlayerInfo.liveUser == self) {
                continue;
            }

            final boolean isOwner =
                    Objects.equals(blockPos, networkPlayerInfo.bedPos)
                            || Objects.equals(blockPos, networkPlayerInfo.bedPos.add(facing.getOpposite().getDirectionVec()));

            if (isOwner) {
                return networkPlayerInfo;
            }
        }

        return null;
    }

    @JNICExclude
    public static String extractPlayerId(String msg) {
        if (msg.matches("^\\(\\d+\\)\\s*<[^>]+>.*")) {
            int start = msg.indexOf('<');
            int end = msg.indexOf('>', start);
            if (start != -1 && end != -1) {
                return msg.substring(start + 1, end).trim();
            }
        }

        int lt = msg.indexOf('<');
        int gt = msg.indexOf('>', lt);
        if (lt != -1 && gt != -1 && gt > lt) {
            String candidate = msg.substring(lt + 1, gt).trim();

            if (!candidate.contains("队")) {
                return candidate;
            }
        }

        if (msg.startsWith("<") && msg.contains(">")) {
            int gtIndex = msg.indexOf('>');
            int colonIndex = msg.indexOf(':', gtIndex);
            if (gtIndex != -1 && colonIndex != -1 && colonIndex > gtIndex) {
                return msg.substring(gtIndex + 1, colonIndex).trim();
            }
        }

        int colonIndex = msg.indexOf(':');
        if (colonIndex != -1) {
            return msg.substring(0, colonIndex).trim();
        }

        return null;
    }

    @JNICExclude
    public void executeAtTickDelayed(int delay, Runnable runnable) {
        scheduledTasks.add(new TickScheduledTask(delay, runnable));
    }

    @JNICExclude
    public HashMap<UUID, Consumer<String>> getCommandOutMap() {
        return commandOutMap;
    }

    @JNICExclude
    public HashMap<String, HackerPlayer> getHackerMap() {
        return hackerMap;
    }

    private class TickScheduledTask {
        private final int tick;
        private final Runnable runnable;

        public TickScheduledTask(int delay, Runnable runnable) {
            if (delay <= 0) {
                throw new IllegalArgumentException();
            }

            this.tick = LiveComponent.this.ticks + delay;
            this.runnable = runnable;
        }
    }
}
