package dev.xinxin.module.modules.misc;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.world.*;
import dev.xinxin.gui.Island.AddIslandEvent;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.render.AnimationUtil;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;

public class AutoGapple extends Module {
    public static boolean eating = false;
    public static int movingPackets = 0;
    private int slot = 0;
    private final LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();

    public AutoGapple() {
        super("AutoGapple", Category.Combat,"自动金苹果","Eat Apple Attack","吃苹果攻击");
    }

    @Override
    public void onEnable() {
        if(mc.thePlayer == null) return;
        eating = true;
        state = true;

        movingPackets = 0;
        packets.clear();

        if (getGApple() >= 0) {
            slot = getGApple();
        } else {
            setState(false);
        }
    }

    @Override
    public void onDisable() {
        eating = false;
        state = false;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(),    Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(),    Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(),   Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),    Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),  Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        poll();
    }

    @EventTarget
    public void onWorld(EventWorldLoad e) {
        setState(false);
    }

    @EventTarget
    public void onUpdate(EventUpdate update) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead || getGApple() < 0) {
            setState(false);
        }

        {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword);
        }
    }

    @EventTarget
    public void onPackerSend(EventPacketSend packetSend) {
        if (mc.isSingleplayer()) {
            movingPackets = 5;
            return;
        }
        if (packetSend.getPacket() instanceof C01PacketChatMessage || packetSend.getPacket() instanceof C08PacketPlayerBlockPlacement || !eating) return;

        if (packetSend.getPacket() instanceof C03PacketPlayer) {
            movingPackets++;
        }

        packets.add(packetSend.getPacket());
        packetSend.setCancelled();
    }

    @EventTarget
    public void onMotion(EventMotion motion) {
        if (eating) {
            if (motion.isPost()) {
                packets.add(new C01PacketChatMessage());
            }
        }
    }

    @EventTarget
    public void onTick(EventTick tick) {
        if (eating) {
            mc.gameSettings.keyBindSprint.pressed = false;
            mc.gameSettings.keyBindForward.pressed = false;
            mc.gameSettings.keyBindBack.pressed = false;
            mc.gameSettings.keyBindLeft.pressed = false;
            mc.gameSettings.keyBindRight.pressed = false;
            mc.gameSettings.keyBindJump.pressed = false;
        }
        if (movingPackets >= 32) {
            if (mc.thePlayer.inventory.currentItem != slot ||
                    mc.thePlayer.getHeldItem() == null ||
                    !(mc.thePlayer.getHeldItem().getItem() instanceof ItemAppleGold)) {

                send(new C09PacketHeldItemChange(slot));
            }
            send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            poll();
            if (mc.thePlayer.inventory.currentItem != slot) {
                send(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            }

//            HelperUtil.sendMessage("§a§l何树友和树成为朋友了呢§e§l/呲牙");
            movingPackets = 0;
        } else {
            if (mc.thePlayer.ticksExisted % 5 == 0) {
                while (!packets.isEmpty()) {
                    Packet<?> packet = packets.poll();

                    if (packet instanceof C01PacketChatMessage) break;

                    if (packet instanceof C03PacketPlayer) movingPackets--;

                    send(packet);
                }
            }
        }
    }


    @EventTarget
    public void onMoveMath(MoveMathEvent event) {
        event.setCancelled(mc.thePlayer.positionUpdateTicks < 19);
    }


    private int getGApple() {
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null)
                continue;

            if (stack.getItem() instanceof ItemAppleGold) {
                return i;
            }
        }

        this.toggle();
        return -1;
    }


    private void send(Packet<?> packet) {
        mc.thePlayer.sendQueue.addToSendQueueUnregisteredNoEvent(packet);
    }

    private void poll() {
        while (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();

            if (packet instanceof C01PacketChatMessage) continue;

            send(packet);
        }
    }

    float present;
    @EventTarget
        public void onIsland(AddIslandEvent event) {
        if (event.getState() == AddIslandEvent.EventState.PRE) {
            present = AnimationUtil.smooth(present, (float) movingPackets / 32, 0.2f);
            SilenceFix.instance.island.addIsland(() -> FontManager.icontestFont35.drawString("", 12, 14, -1), "Gapple", "C03Tick: " + movingPackets, present);
        }
    }

}