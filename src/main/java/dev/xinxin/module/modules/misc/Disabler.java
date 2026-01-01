package dev.xinxin.module.modules.misc;

import dev.xinxin.event.EventPriority;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventHigherPacketSend;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.movement.Fly;
import dev.xinxin.module.modules.movement.NoSlow;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.DebugUtil;
import dev.xinxin.utils.PacketProcess.PacketProcessListenableFutureTask;
import dev.xinxin.utils.TimerUtils;
import dev.xinxin.utils.client.PacketUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Util;

import java.util.*;
import java.util.concurrent.FutureTask;

public class Disabler
        extends Module {
    public static final BoolValue postValue = new BoolValue("Post", true);
    public final BoolValue oldPostValue = new BoolValue("OldPost", false);
    public final BoolValue digValue = new BoolValue("Digging", true);
    public final BoolValue blockValue = new BoolValue("Cancel Blocking Packet", false);
    private final BoolValue badPacketsA = new BoolValue("BadPacketsA", true);
    public final BoolValue badPacketH = new BoolValue("BadPacketH", false); // LLL
    public static final BoolValue badPacketsF = new BoolValue("BadPacketsF", true);
    private final BoolValue fakePingValue = new BoolValue("FakePing", false);
    public final BoolValue fastBreak = new BoolValue("FastBreak", true);
    public final BoolValue debug = new BoolValue("Debug", true);
    private final HashMap<Packet<?>, Long> packetsMap = new HashMap();
    int lastSlot = -1;
    boolean lastSprinting;
    static Disabler INSTANCE;
    private boolean S08 = false;
    private NoSlow noSlow;
    private boolean c0a;

    @Override
    public void onEnable() {
        this.noSlow = this.getModule(NoSlow.class);
    }

    public Disabler() {
        super("NewDisabler", Category.Misc,"绕过器");
        INSTANCE = this;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventTarget
    @EventPriority(value=9)
    public void onUpdate(EventUpdate event) {
        if (this.fakePingValue.getValue().booleanValue()) {
            try {
                HashMap<Packet<?>, Long> hashMap = this.packetsMap;
                synchronized (hashMap) {
                    Iterator<Map.Entry<Packet<?>, Long>> iterator = this.packetsMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Packet<?>, Long> entry = iterator.next();
                        if (entry.getValue() >= System.currentTimeMillis()) continue;
                        mc.getNetHandler().addToSendQueue(entry.getKey());
                        iterator.remove();
                    }
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (this.S08) {
            this.S08 = false;
        }
    }

    public static void onS08() {
        Disabler.INSTANCE.S08 = true;
    }

    @EventTarget
    public void onWorld(EventWorldLoad event) {
        this.lastSlot = -1;
        this.lastSprinting = false;
    }

    @EventTarget
    public void onHigherPacket(EventHigherPacketSend event) {
        Packet packet = event.getPacket();
        if (Disabler.mc.thePlayer == null) {
            return;
        }
        if (Disabler.mc.thePlayer.isDead) {
            return;
        }
        if ((this.blockValue.getValue().booleanValue() || this.digValue.getValue().booleanValue()) && Disabler.mc.thePlayer.getHeldItem() != null && this.blockValue.getValue().booleanValue() && Disabler.mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08 = (C08PacketPlayerBlockPlacement)packet;
            if (this.debug.getValue().booleanValue()) {
                DebugUtil.log(c08.getPosition());
            }
            if ((c08.getPosition().getX() == -1 || c08.facingX == -1.0f) && c08.facingZ == -1.0f) {
                if (this.debug.getValue().booleanValue() && mc.thePlayer.ticksExisted % 10 == 0) {
                    NotificationManager.post(NotificationType.INFO, "Disabler", "何树友当树的朋友");
                }
                event.setCancelled(true);
            }
        }
        if (this.digValue.getValue().booleanValue() && event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging)packet;
            if (Disabler.mc.thePlayer.getHeldItem() != null && Disabler.mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && c07.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                if (this.debug.getValue().booleanValue() && mc.thePlayer.ticksExisted % 10 == 0) {
                    NotificationManager.post(NotificationType.INFO, "Disabler", "何树友当树的朋友。。。");
                }
                event.setCancelled(true);
                Disabler.mc.thePlayer.sendQueue.addToSendQueue(new C0EPacketClickWindow(0, 36, 0, 2, new ItemStack(Block.getBlockById(166)), (short) 0));
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(Disabler.mc.thePlayer.inventory.currentItem % 8 + 1));
                mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload("test", new PacketBuffer(Unpooled.buffer())));
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(Disabler.mc.thePlayer.inventory.currentItem));
            }
        }
    }




    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventTarget
    public void onPacket(EventPacketSend event) {
        Packet packet = event.getPacket();
        if (Disabler.mc.thePlayer == null) {
            return;
        }
        if (Disabler.mc.thePlayer.isDead) {
            return;
        }

        if (packet instanceof C0APacketAnimation) {
            c0a = true;
        } else if (packet instanceof C02PacketUseEntity entity && badPacketH.getValue()) {
            if (entity.getAction() != C02PacketUseEntity.Action.ATTACK) return;

            if (!c0a) {
                PacketUtil.sendPacketNoEvent(new C0APacketAnimation()); // LLL
            }

            c0a = false;
        }

        if (badPacketsF.getValue().booleanValue() && packet instanceof C0BPacketEntityAction) {
            if (((C0BPacketEntityAction)packet).getAction() == C0BPacketEntityAction.Action.START_SPRINTING) {
                if (this.lastSprinting) {
                    event.setCancelled(true);
                }
                this.lastSprinting = true;
            } else if (((C0BPacketEntityAction)packet).getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                if (!this.lastSprinting) {
                    event.setCancelled(true);
                }
                this.lastSprinting = false;
            }
        }
        if (this.oldPostValue.getValue().booleanValue() && mc.getCurrentServerData() != null && (packet instanceof C0APacketAnimation || packet instanceof C02PacketUseEntity || packet instanceof C0EPacketClickWindow || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging)) {
            PacketUtil.send(new C0FPacketConfirmTransaction(114, (short) 514, true));
        }
        if (this.fastBreak.getValue().booleanValue() && packet instanceof C07PacketPlayerDigging && ((C07PacketPlayerDigging)packet).getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
            PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, ((C07PacketPlayerDigging)packet).getPosition(), ((C07PacketPlayerDigging)packet).getFacing()));
        }
        if (this.badPacketsA.getValue().booleanValue() && packet instanceof C09PacketHeldItemChange) {
            int slot = ((C09PacketHeldItemChange)packet).getSlotId();
            if (slot == this.lastSlot && slot != -1) {
                event.setCancelled(true);
            }
            this.lastSlot = ((C09PacketHeldItemChange)packet).getSlotId();
        }
        if (this.fakePingValue.getValue().booleanValue() && (packet instanceof C00PacketKeepAlive || packet instanceof C16PacketClientStatus) && !(Disabler.mc.thePlayer.getHealth() <= 0.0f) && !this.packetsMap.containsKey(packet)) {
            event.setCancelled(true);
            HashMap<Packet<?>, Long> hashMap = this.packetsMap;
            synchronized (hashMap) {
                this.packetsMap.put(packet, System.currentTimeMillis() + TimerUtils.randomDelay(199999, 9999999));
            }
        }
    }

    public static boolean getGrimPost() {
        return Disabler.mc.thePlayer != null && !mc.thePlayer.isDead && Disabler.mc.theWorld != null && INSTANCE != null && INSTANCE.getState() && postValue.getValue() && Disabler.mc.thePlayer.ticksExisted > 30 && !getModule(Fly.class).shouldDisableDisabler();
    }

    public static boolean shouldProcess() {
        return true;
    }

    public static boolean processingFakePlayers = false;
    public static void preProcessPackets() {
        LinkedList<FutureTask<?>> queue = new LinkedList<>();
        while (!mc.preProcessedTasks.isEmpty()) {
            FutureTask<?> task = mc.preProcessedTasks.get(0);
            if (mc.getNetHandler() != null && mc.theWorld != null && mc.thePlayer != null && task instanceof PacketProcessListenableFutureTask<?> packetTask) {

                processingFakePlayers = true;
                if (packetTask.packetToProcess instanceof S19PacketEntityHeadLook packet && packet.getEntity(mc.theWorld) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S14PacketEntity packet && packet.getEntity(mc.theWorld) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S19PacketEntityStatus packet && packet.getEntity(mc.theWorld) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S1DPacketEntityEffect packet && mc.theWorld.getEntityByID(packet.getEntityId()) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S0BPacketAnimation packet && mc.theWorld.getEntityByID(packet.getEntityID()) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S18PacketEntityTeleport packet && mc.theWorld.getEntityByID(packet.getEntityId()) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S12PacketEntityVelocity packet && mc.theWorld.getEntityByID(packet.getEntityID()) instanceof EntityPlayer player && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    packet.processPacket(mc.getNetHandler());
                }
                if (packetTask.packetToProcess instanceof S13PacketDestroyEntities packet) {
                    packet.processPacket(mc.getNetHandler());
                }

                processingFakePlayers = false;
            }
            queue.add(task);
            mc.preProcessedTasks.pollFirst();
        }
        mc.scheduledTasks.add(queue);
    }

    public static void grimProcessStoredPackets() {
        if (mc.scheduledTasks.isEmpty()) return;
        while (!mc.scheduledTasks.get(0).isEmpty())
        {
            Util.runTask(Objects.requireNonNull(mc.scheduledTasks.get(0).poll()), Minecraft.logger);
        }
        mc.scheduledTasks.remove(0);
    }

    public enum mode {
        Grim,
        Vulcan

    }
}

