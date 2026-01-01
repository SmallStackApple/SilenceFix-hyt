package dev.xinxin.module.modules.misc;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventMotion;
import dev.xinxin.event.world.EventPacketReceive;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.gui.Island.Island;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.KillAura;
import dev.xinxin.module.modules.player.Blink;
import dev.xinxin.module.modules.player.ChestStealer;
import dev.xinxin.module.modules.player.InvCleaner;
import dev.xinxin.module.modules.world.PlayerWarn;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ModeValue;
import dev.xinxin.module.values.NumberValue;
import dev.xinxin.utils.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.netease.GsonUtil;
import net.netease.PacketProcessor;
import net.netease.gui.GermGameGui;
import net.netease.packet.impl.Packet04;
import net.netease.packet.impl.Packet26;

import java.util.HashMap;
import java.util.regex.Pattern;

public class AutoPlay extends Module {
    private final ModeValue<again> autoMode = new ModeValue<>("Auto Mode", again.values() , again.SkyWars);

    private enum again {
        SkyWars,
        BedWars,
        None;
    }
    
    private final BoolValue autoAgain = new BoolValue("Auto Again", false);
    private final NumberValue delayValue = new NumberValue("Again Delay", 3.0, 1.0, 10.0, 0.1, autoAgain::getValue);
    private final BoolValue autoKit = new BoolValue("Auto kit", true, ()->autoMode.is("SkyWars"));
    private final NumberValue select = new NumberValue("Select Slot",6,0,7,1,()->autoMode.is("SkyWars") && autoKit.getValue());
    private final BoolValue autoClip = new BoolValue("Auto Clip",true, ()->autoMode.is("SkyWars"));
    private final BoolValue toggleModule = new BoolValue("Auto Module", true);
    private final TimerUtil timer = new TimerUtil();
    private boolean waiting = false;
    private boolean waiting2 = false;

    private static final Pattern PATTERN_BEHAVIOR_EXCEPTION = Pattern.compile("玩家(.*?)在本局游戏中行为异常");
    private static final Pattern PATTERN_WIN_MESSAGE = Pattern.compile("你在地图(.*?)中赢得了(.*?)");
    private static final String TEXT_COUNTDOWN = "开始倒计时: 1 秒";

    public static boolean needblink = false;
    public AutoPlay() {
        super("AutoPlay", Category.Misc ,"自动游玩");
    }

    private final TimerUtil timers = new TimerUtil();
    private boolean game = false;
    private int SWtimer = 5;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (SWtimer >= delayValue.getValue()) {
            game = false;
            SWtimer = 5;
        }
        if (game) {
            SWtimer--;
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        ItemStack itemStack;
        if (event.isPost()) {
            return;
        }
        if (mc.currentScreen != null) {
            if (mc.currentScreen instanceof GuiChest chest) {
                //Click Kit Slot ok.
                if (chest.lowerChestInventory.getDisplayName().toString().contains("职业") && autoKit.getValue() && autoMode.is("SkyWars"))
                    mc.playerController.windowClick(chest.inventorySlots.windowId, select.value.intValue(), 0, 0, mc.thePlayer);
            }
        }
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (this.waiting && this.waiting2 && GermGameGui.INSTANCE.getCurrentElement() != null) {
            mc.thePlayer.swingItem();
            HashMap<String, Integer> data = new HashMap<>();
            data.put("click", 1);
            String json = GsonUtil.toJson(data);
            String message = new StringBuilder().insert(0, "GUI$").append("mainmenu").append("@").append("subject/skywar").toString();
            PacketProcessor.INSTANCE.sendPacket(new Packet04("mainmenu"));
            PacketProcessor.INSTANCE.sendPacket(new Packet26(message, json));
            HashMap<String, Object> data2 = new HashMap<>();
            data2.put("entry", GermGameGui.INSTANCE.getCurrentElement().getSubElements().get(0).getIndex());
            data2.put("sid", GermGameGui.INSTANCE.getCurrentElement().getSubElements().get(0).getSid());
            String json2 = GsonUtil.toJson(data2);
            String message2 = new StringBuilder().insert(0, "GUI$").append("mainmenu").append("@").append("entry/").append(0).toString();
            PacketProcessor.INSTANCE.sendPacket(new Packet04("mainmenu"));
            PacketProcessor.INSTANCE.sendPacket(new Packet26(message2, json2));
            this.waiting = false;
            this.waiting2 = false;
        }
        if ((itemStack = mc.thePlayer.inventoryContainer.getSlot(44).getStack()) == null || itemStack.getDisplayName() == null) {
            return;
        }
        if (itemStack.getDisplayName().contains("游戏指南")) {
            this.waiting2 = true;
        }
        if (!itemStack.getDisplayName().contains("退出观战")) {
            return;
        }
        if (itemStack.getItem().equals(Items.iron_door) && autoMode.is("SkyWars") && autoAgain.getValue()|| itemStack.getItem().equals(Items.chest_minecart) && autoMode.is("BedWars")  && autoAgain.getValue()) {
            this.timer.reset();
            this.waiting = true;
        }
    }

    @EventTarget
    public void onPacketReceiveEvent(EventPacketReceive event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        var packet = event.getPacket();
        if (packet instanceof S02PacketChat) {
            String text = ((S02PacketChat)packet).getChatComponent().getUnformattedText();
            String loseMessage = "You died! Want to play again? Click here!";
            String winMessage = "You won! Want to play again? Click here!";
            if (PATTERN_BEHAVIOR_EXCEPTION.matcher(text).find()) {
                SilenceFix.instance.island.addIsland(Island.IslandType.SUCCESS,"Checker","A player was banned.",3000);
            } else if (PATTERN_WIN_MESSAGE.matcher(text).find() || mc.thePlayer.isSpectator() && this.toggleModule.getValue()) {
                this.toggleOffensiveModules(false);
                getModule(KillAura.class).setState(false);
                getModule(Scaffold.class).setState(false);
                getModule(AutoGapple.class).setState(false);
                SilenceFix.instance.island.addIsland(Island.IslandType.SUCCESS,"Game Ending","Sending you to next game in " + this.delayValue.getValue() + "s",3000);
            }

            if ((text.contains(winMessage) && text.length() < winMessage.length() + 3) || (text.contains(loseMessage) && text.length() < loseMessage.length() + 3)) {
                timers.reset();
            }
            if (text.contains(TEXT_COUNTDOWN)) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 等待1秒
                        getModule(Blink.class).setState(false);
                        needblink = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            if (text.contains("开始倒计时: 3 秒") && autoClip.getValue() && autoMode.is("SkyWars")) {
                game = true;
                SWtimer = 5;
                needblink = true;
                getModule(Blink.class).setState(true);
                //Break Glass Block ok.
                BlockPos blockPos = new BlockPos(mc.thePlayer.posX , mc.thePlayer.posY -1, mc.thePlayer.posZ);
                Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                if (block == Blocks.glass) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
                    mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
                    mc.theWorld.setBlockState(blockPos, Blocks.air.getDefaultState(), 2);
                }
            }
            if (text.contains("开始倒计时: 5 秒") && autoKit.getValue() && autoMode.is("SkyWars")) {
                int slot = 0;
                int nslot = mc.thePlayer.inventory.currentItem;
                //Open Kit menu ok.
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
                mc.rightClickMouse();
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(nslot));
            }
        }
    }

    private void toggleOffensiveModules(boolean state) {
        getModule(InvCleaner.class).setState(state);
        getModule(ChestStealer.class).setState(state);
    }

    private void checkAndTogglePlayerTracker() {
        if (!getModule(PlayerWarn.class).getState()) {
            NotificationManager.post(NotificationType.WARNING, "SkyWars Warning (Wait 15s)", "Please Enable PlayerWarn.", 15.0f);
        } else if (this.toggleModule.getValue()) {
            this.toggleOffensiveModules(true);
        }
    }
}
