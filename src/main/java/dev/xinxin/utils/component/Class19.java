package dev.xinxin.utils.component;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.world.EventPacketSend;
import dev.xinxin.event.world.EventTick;
import dev.xinxin.utils.DebugUtil;
import lombok.Setter;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S00PacketKeepAlive;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Class19 extends SilenceFix {
    private static final Map<Class<?>, Consumer<Packet<?>>> cancelAction = new HashMap<>();
    private static final Map<Class<?>, Consumer<Packet<?>>> releaseAction = new HashMap<>();
    private static final List<Class<?>> blackList = new ArrayList<>();
    private static final Map<Class<?>, Predicate<Packet<?>>> cancelPacketMap = new HashMap<>();
    private static final Map<Class<?>, Predicate<Packet<?>>> releaseMap = new HashMap<>();
    private static final List<Class<?>> whitList = new ArrayList<>();
    public static boolean storing = false;
    public static LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();
    public static boolean noEvt = false;
    @Setter
    private static Map<Class<?>, Predicate<Packet<?>>> addReturnMap = new HashMap<>();

    public static void Method1(Packet<?> packet, boolean event) {
        if (event) {
            EventPacketSend packetSendEvent = new EventPacketSend(packet);
            EventManager.call(packetSendEvent);
            if (packetSendEvent.isCancelled()) {
                return;
            }
        }
        noEvt = true;
        mc.getNetHandler().addToSendQueueUnregisteredNoEvent(packet);
        noEvt = false;
    }

    public static void Method2(Class<?>... fliterPackets) {
        if (storing) {
            return;
        }
        Arrays.asList(fliterPackets).forEach(e -> {
            blackList.add(e);
            cancelPacketMap.put(e, f -> true);
        });
        storing = true;
    }

    public static void Method3(Class<?> clazz, Consumer<Packet<?>> packetConsumer) {
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

    public static void Method4(Class<?> clazz, Predicate<Packet<?>> predicate) {
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

    public static void Method5() {
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

    public static void Method6(Class<?> clazz, Predicate<Packet<?>> predicate) {
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

    public static void Method7(int sendPackets, boolean noEvent) {
        Method7(sendPackets, noEvent, false);
    }

    public static void Method7(boolean sendOneTick) {
        Method7(packets.size(), true, sendOneTick);
    }

    public static void Method7() {
        Method7(packets.size(), true);
    }

    public static void Method7(int sendPackets, boolean noEvent, boolean sendOneTick) {
        int sends = 0;
        try {
            block2:
            while (!packets.isEmpty()) {
                Packet<?> packet = packets.take();
                if (packet instanceof S00PacketKeepAlive) {
                    if (!sendOneTick) continue;
                } else {
                    for (Entry<Class<?>, Predicate<Packet<?>>> entries : releaseMap.entrySet()) {
                        if (!entries.getKey().isAssignableFrom(packet.getClass()) || !entries.getValue().test(packet))
                            continue;
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
                        mc.getNetHandler().addToSendQueueUnregisteredNoEvent(packet);
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
        } catch (Exception e) {
            DebugUtil.print(e.getMessage());
        }
    }

    public static void Method8() {
        storing = false;
        noEvt = false;
        Method7();
        blackList.clear();
        cancelPacketMap.clear();
        cancelAction.clear();
        releaseAction.clear();
        releaseMap.clear();
        whitList.clear();
        packets.clear();
    }

    public static boolean Method9(Packet<?> packet) {
        if (storing && !noEvt) {
            cancelAction.forEach((aClass, packetConsumer) -> {
                if (aClass.isAssignableFrom(packet.getClass())) {
                    packetConsumer.accept(packet);
                }
            });
            for (Class<?> clazz : blackList) {
                if (clazz.isAssignableFrom(packet.getClass())) {
                    return true;
                }
            }
            for (Map.Entry<Class<?>, Predicate<Packet<?>>> entry : cancelPacketMap.entrySet()) {
                if (entry.getKey().isAssignableFrom(packet.getClass()) && entry.getValue().test(packet)) {
                    return true;
                }
            }
            if (!whitList.isEmpty() && !whitList.contains(packet.getClass())) {
                return true;
            }
            boolean needAdd = true;
            for (Map.Entry<Class<?>, Predicate<Packet<?>>> entries : addReturnMap.entrySet()) {
                if (entries.getKey().isAssignableFrom(packet.getClass()) && entries.getValue().test(packet)) {
                    needAdd = false;
                    break;
                }
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
            Method8();
        }
    }

}

