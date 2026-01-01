package dev.xinxin.utils;

import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.misc.MinecraftInstance;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S00PacketKeepAlive;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;


    public class PacketStoringComponent
implements MinecraftInstance {
    private static final Map<Class<?>, Consumer<Packet<?>>> cancelAction = new HashMap();
    private static final Map<Class<?>, Consumer<Packet<?>>> releaseAction = new HashMap();
    public static boolean storing = false;
    private static final List<Class<?>> blackList = new ArrayList();
    private static Map<Class<?>, Predicate<Packet<?>>> addReturnMap = new HashMap();
    private static final Map<Class<?>, Predicate<Packet<?>>> cancelPacketMap = new HashMap();
    private static final Map<Class<?>, Predicate<Packet<?>>> releaseMap = new HashMap();
    private static final List<Class<?>> whitList = new ArrayList();
    public static LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue();
    public static boolean noEvt = false;

    public static void send(Packet<?> packet, boolean event) {
        if (event) {
            EventPacketSend packetSendEvent = new EventPacketSend(packet);
            EventManager.call(packetSendEvent);
            if (packetSendEvent.isCancelled()) {
                return;
            }
        }
        noEvt = true;
        PacketUtil.sendPacketNoEvent(packet);
        noEvt = false;
    }

    public static void startStoringPackets(Class<?> ... fliterPackets) {
        if (storing) {
            return;
        }
        Arrays.asList(fliterPackets).forEach(e -> {
            blackList.add((Class<?>)e);
            cancelPacketMap.put((Class<?>)e, f -> true);
        });
        storing = true;
    }

        public static void setCancelAction(Class<?> clazz, Consumer<Packet<?>> packetConsumer) {
        boolean isIN = false;
        for (Class<?> classes : cancelAction.keySet()) {
            if (classes != clazz) continue;
            isIN = true;
            break;
        }
        if (isIN) {
            cancelAction.replace(clazz, packetConsumer);
        } else {
            cancelAction.put(clazz, packetConsumer);
        }
    }

    public static void setCanelPackets(Class<?> clazz, Predicate<Packet<?>> predicate) {
        boolean isIN = false;
        for (Class<?> classes : cancelPacketMap.keySet()) {
            if (classes != clazz) continue;
            isIN = true;
            break;
        }
        if (isIN) {
            cancelPacketMap.replace(clazz, predicate);
        } else {
            cancelPacketMap.put(clazz, predicate);
        }
    }

    public static void resetBlackList() {
        blackList.clear();
    }

    public static void setReleaseAction(Class<?> clazz, Consumer<Packet<?>> packetConsumer) {
        boolean isIN = false;
        for (Class<?> classes : releaseAction.keySet()) {
            if (classes != clazz) continue;
            isIN = true;
            break;
        }
        if (isIN) {
            releaseAction.replace(clazz, packetConsumer);
        } else {
            releaseAction.put(clazz, packetConsumer);
        }
    }

    public static void setReleaseMap(Class<?> clazz, Predicate<Packet<?>> predicate) {
        boolean isIN = false;
        for (Class<?> classes : releaseMap.keySet()) {
            if (classes != clazz) continue;
            isIN = true;
            break;
        }
        if (isIN) {
            releaseMap.replace(clazz, predicate);
        } else {
            releaseMap.put(clazz, predicate);
        }
    }

    public static void releasePacket(int sendPackets, boolean noEvent) {
        PacketStoringComponent.releasePacket(sendPackets, noEvent, false);
    }

    public static void releasePacket(boolean sendOneTick) {
        PacketStoringComponent.releasePacket(packets.size(), true, sendOneTick);
    }

    public static void releasePacket() {
        PacketStoringComponent.releasePacket(packets.size(), true);
    }

    public static void releasePacket(int sendPackets, boolean noEvent, boolean sendOneTick) {
        int sends = 0;
        try {
            block2: while (!packets.isEmpty()) {
                Packet<?> packet = packets.take();
                if (packet instanceof S00PacketKeepAlive) {
                    if (!sendOneTick) continue;
                } else {
                    for (Map.Entry<Class<?>, Predicate<Packet<?>>> entries : releaseMap.entrySet()) {
                        if (!entries.getKey().isAssignableFrom(packet.getClass()) || !entries.getValue().test(packet)) continue;
                        continue block2;
                    }
                    releaseAction.forEach((key, value) -> {
                        if (key.isAssignableFrom(packet.getClass())) {
                            value.accept(packet);
                        }
                    });
                    ++sends;
                    if (noEvent) {
                        noEvt = true;
                        PacketUtil.sendPacketNoEvent(packet);
                        noEvt = false;
                    } else {
                        noEvt = true;
                        mc.getNetHandler().addToSendQueue(packet);
                        noEvt = false;
                    }
                    if (sends < sendPackets) continue;
                }
                break;
            }
        }
        catch (Exception e) {
        }
    }

    public static void stopStoringPackets() {
        storing = false;
        noEvt = false;
        PacketStoringComponent.releasePacket();
        blackList.clear();
        cancelPacketMap.clear();
        cancelAction.clear();
        releaseAction.clear();
        releaseMap.clear();
        whitList.clear();
        packets.clear();
    }

    public static boolean onStorePacket(Packet<?> packet) {
        if (storing && !noEvt) {
            cancelAction.forEach((aClass, packetConsumer) -> {
                if (aClass.isAssignableFrom(packet.getClass())) {
                    packetConsumer.accept(packet);
                }
            });
            for (Class<?> clazz : blackList) {
                if (!clazz.isAssignableFrom(packet.getClass())) continue;
                return true;
            }
            for (Map.Entry entry : cancelPacketMap.entrySet()) {
                if (!((Class)entry.getKey()).isAssignableFrom(packet.getClass()) || !((Predicate)entry.getValue()).test(packet)) continue;
                return true;
            }
            if (!whitList.isEmpty() && !whitList.contains(packet.getClass())) {
                return true;
            }
            boolean needAdd = true;
            for (Map.Entry<Class<?>, Predicate<Packet<?>>> entries : addReturnMap.entrySet()) {
                if (!entries.getKey().isAssignableFrom(packet.getClass()) || !entries.getValue().test(packet)) continue;
                needAdd = false;
            }
            if (needAdd) {
                packets.add(packet);
            }
            return false;
        }
        return true;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (storing) {
            packets.add(new S00PacketKeepAlive());
        }
        if (mc.getNetHandler() == null) {
            PacketStoringComponent.stopStoringPackets();
        }
    }

    public static void setAddReturnMap(Map<Class<?>, Predicate<Packet<?>>> addReturnMap) {
        PacketStoringComponent.addReturnMap = addReturnMap;
    }
}

