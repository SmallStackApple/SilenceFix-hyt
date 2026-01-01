package dev.xinxin.module.modules.player;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventHigherPacketSend;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.event.world.EventUpdate;
import dev.xinxin.event.world.EventWorldLoad;
import dev.xinxin.gui.Island.AddIslandEvent;
import dev.xinxin.gui.notification.NotificationManager;
import dev.xinxin.gui.notification.NotificationType;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.modules.combat.LiveFriendly;
import dev.xinxin.module.modules.misc.AutoPlay;
import dev.xinxin.module.modules.world.Scaffold;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.render.AnimationUtil;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Blink extends Module {
    private LinkedList<List<Packet<?>>> packets = new LinkedList<>();
    public static EntityOtherPlayerMP fakePlayer;
    private int ticks;
    public BoolValue bw = new BoolValue("BedWars",false);
    public BoolValue slowpoll = new BoolValue("Slow Release",false);
    public BoolValue antiAIM = new BoolValue("AntiAim",true);
    public BoolValue blinkfly = new BoolValue("FakeBlockFly",true);
    public BoolValue grimBlinkValue = new BoolValue("Hyt Blink Fix",true);

    public BoolValue autoclose = new BoolValue("AutoClose", false);
    private boolean closing = false;

    public Blink() {
        super("Blink", Category.Player,"隐身","Suspends all movement packets.","暂停所有移动发包传输。");
    }

    private int maxPacketsBeforeRelease;
    @EventTarget
    public void onIsland(AddIslandEvent event) {
        if (event.getState() != AddIslandEvent.EventState.PRE) return;
        float target = Math.max(0f, Math.min(1f, packets.size() / (float) Math.max(1, maxPacketsBeforeRelease)));
        present = AnimationUtil.smooth(present, target, 0.1f);
        String title = "Blink";
        String description = packets.size() + " / " + maxPacketsBeforeRelease + " packets";
        SilenceFix.instance.island.addIsland(() -> FontManager.icontestFont35.drawString("", 12, 14, -1), title, description, present);
    }

    @Override
    public void onEnable() {
        closing = false;
        if (mc.thePlayer == null) return;
        maxPacketsBeforeRelease = 165;
        packets.clear();
        packets.add(new ArrayList<>());
        ticks = 0;
        fakePlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        fakePlayer.clonePlayer(mc.thePlayer, true);
        fakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
        fakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
        mc.theWorld.addEntityToWorld(-1337, fakePlayer);

    }
    @EventTarget
    public void onWorld(EventWorldLoad event) {
        this.setState(false);
    }

    @Override
    public void onDisable() {
        closing = true;
        packets.forEach(this::sendTick);
        packets.clear();

        try {
            if (fakePlayer != null) {
                mc.theWorld.removeEntity(fakePlayer);
                fakePlayer = null;
            }
        } catch (Exception ignored) {
        }


    }
    @EventTarget
    public void onPacket(EventHigherPacketSend event) {
        Packet<?> packet = event.getPacket();

        if (PacketUtil.isEssential(packet)) return;

        if (PacketUtil.isCPacket(packet)) {

            mc.addScheduledTask(() -> {
                if (packets.isEmpty()) {
                    packets.add(new LinkedList<Packet<?>>());
                }
                this.packets.getLast().add(packet);
            });
            event.setCancelled(true);
        }
    }

    float present;




    @EventTarget
    public void onTick(EventTick event) {

        ticks++;
        packets.add(new ArrayList<>());

        if (packets.size() > 170) {
            this.setState(false);
        }

        if(antiAIM.getValue()) {
            if (packets.size() >= maxPacketsBeforeRelease) {
                for (int i = 0; i < 7; i++) {
                    if (!packets.isEmpty()) {
                        sendTick(packets.getFirst());
                        packets.removeFirst();
                    } else {
                        return;
                    }
                }
            }
            checkNearbyEntities();
        }

        if(slowpoll.getValue()) {
            if (getModule(Scaffold.class).state && blinkfly.getValue() && !bw.getValue()
                    && SilenceFix.instance.moduleManager.getModule(LiveFriendly.class).getState())
            {
                if (this.packets.size() > 20) {
                    int packetsToSend = (int) (this.packets.size() - 6);
                    for (int i = 0; i < packetsToSend; i++) {
                        if (!this.packets.isEmpty() && AutoPlay.needblink) {
                            this.sendTick(this.packets.getFirst());
                            this.packets.removeFirst();
                        }
                    }
                }
            } else {
                if (packets.size() > 170 || (ticks % 10 == 0) && AutoPlay.needblink) {
                    this.poll();
                }
                if (closing && bw.getValue()) {
                    int remain = 5;
                    while (remain > 0 && !packets.isEmpty()) {
                        if (AutoPlay.needblink) return;
                        this.poll();
                        remain--;
                    }
                    if (packets.isEmpty()) {
                        this.setState(false);
                    }
                }
            }
        } else if (getModule(Scaffold.class).state && blinkfly.getValue() && !bw.getValue()
                && SilenceFix.instance.moduleManager.getModule(LiveFriendly.class).getState()) {
            if (this.packets.size() > 20) {
                int packetsToSend = this.packets.size() - 6;
                for (int i = 0; i < packetsToSend; i++) {
                    if (!this.packets.isEmpty() && AutoPlay.needblink) {
                        this.sendTick(this.packets.getFirst());
                        this.packets.removeFirst();
                    }
                }
            }
        }
    }

    private void releasePackets() {
        if (!packets.isEmpty()) {
            List<Packet<?>> toSend = packets.poll(); // 获取并删除队列的头部
            if (toSend != null) {
                sendTick(toSend); // 发送包含的数据包
            }
        }
    }

    private void checkNearbyEntities() {
        double checkRadiusPlayer = 5.0; // 用于检测玩家的半径
        double checkRadius = 6.0; // 减少检测投掷物的半径，以提前反应
        if (mc.theWorld != null && fakePlayer != null) {
            List<Entity> nearbyPlayers = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                    fakePlayer, fakePlayer.getEntityBoundingBox().expand(checkRadiusPlayer, checkRadiusPlayer, checkRadiusPlayer)
            );
            boolean isPlayerNearby = nearbyPlayers.stream()
                    .anyMatch(entity -> entity instanceof EntityPlayer && entity != fakePlayer && entity != mc.thePlayer);
            List<Entity> nearbyEntities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                    fakePlayer, fakePlayer.getEntityBoundingBox().expand(checkRadius, checkRadius, checkRadius)
            );
            boolean isDangerousEntityNearby = nearbyEntities.stream()
                    .anyMatch(entity -> (entity instanceof EntityArrow &&
                            entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ > 0.1) // 判断箭是否正在飞行
                            || entity instanceof EntityFireball || entity instanceof EntitySnowball || entity instanceof EntityTNTPrimed || entity instanceof EntityEgg);
            if (isDangerousEntityNearby || isPlayerNearby) {
                blinkShortDistance();
            }
        }
    }


    private void blinkShortDistance() {
        final int blinkPacketCount = 6; // 设置所需触发瞬移的数据包数量
        if (packets.size() < blinkPacketCount * 2) {
            return;
        }

        for (int i = 0; i < blinkPacketCount; i++) { // 只发送设定数量的数据包（在此例中为5个）
            if (!packets.isEmpty()) {
                sendTick(packets.getFirst());
                packets.removeFirst();
            } else {
                break;
            }
        }
    }

    private void poll() {
        if (packets.isEmpty()) return;
        this.sendTick(packets.getFirst());
        packets.removeFirst();
    }

    @Override
    public String getSuffix() {
        return String.valueOf(packets.size());
    }

    private void sendTick(List<Packet<?>> tick) {
        tick.forEach(packet -> {
            mc.getNetHandler().getNetworkManager().sendPacketWithoutHigherPacket(packet);
            this.handleFakePlayerPacket(packet);
        });
    }

    private void handleFakePlayerPacket(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition) {
            C03PacketPlayer.C04PacketPlayerPosition position = (C03PacketPlayer.C04PacketPlayerPosition) packet;

            fakePlayer.setPositionAndRotation2(
                    position.x,
                    position.y,
                    position.z,
                    fakePlayer.rotationYaw,
                    fakePlayer.rotationPitch,
                    3,
                    true
            );
            fakePlayer.onGround = position.isOnGround();
        } else if (packet instanceof C03PacketPlayer.C05PacketPlayerLook) {
            C03PacketPlayer.C05PacketPlayerLook rotation = (C03PacketPlayer.C05PacketPlayerLook) packet;
            fakePlayer.setPositionAndRotation2(
                    fakePlayer.posX,
                    fakePlayer.posY,
                    fakePlayer.posZ,
                    rotation.getYaw(),
                    rotation.getPitch(),
                    3,
                    true
            );
            fakePlayer.onGround = rotation.isOnGround();

            fakePlayer.rotationYawHead = rotation.getYaw();
            fakePlayer.rotationYaw = rotation.getYaw();
            fakePlayer.rotationPitch = rotation.getPitch();
        } else if (packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            C03PacketPlayer.C06PacketPlayerPosLook positionRotation = (C03PacketPlayer.C06PacketPlayerPosLook) packet;

            fakePlayer.setPositionAndRotation2(
                    positionRotation.x,
                    positionRotation.y,
                    positionRotation.z,
                    positionRotation.getYaw(),
                    positionRotation.getPitch(),
                    3,
                    true
            );
            fakePlayer.onGround = positionRotation.isOnGround();

            fakePlayer.rotationYawHead = positionRotation.getYaw();
            fakePlayer.rotationYaw = positionRotation.getYaw();
            fakePlayer.rotationPitch = positionRotation.getPitch();
        } else if (packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction action = (C0BPacketEntityAction) packet;
            if (action.getAction() == C0BPacketEntityAction.Action.START_SPRINTING) {
                fakePlayer.setSprinting(true);
            } else if (action.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                fakePlayer.setSprinting(false);
            } else if (action.getAction() == C0BPacketEntityAction.Action.START_SNEAKING) {
                fakePlayer.setSneaking(true);
            } else if (action.getAction() == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                fakePlayer.setSneaking(false);
            }
        } else if (packet instanceof C0APacketAnimation) {
            C0APacketAnimation animation = (C0APacketAnimation) packet;
            fakePlayer.swingItem();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer.hurtTime > 0 && autoclose.getValue()) {
            this.setState(false);
            NotificationManager.post(NotificationType.SUCCESS, "Blink", "AutoClose!", 5);
        }
        if (this.grimBlinkValue.getValue()) {
            mc.getNetHandler().addToSendQueue(new C0FPacketConfirmTransaction(1, (short) 1, true));
        }
    }

}