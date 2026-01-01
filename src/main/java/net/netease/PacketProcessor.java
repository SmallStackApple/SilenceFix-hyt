package net.netease;

import com.google.gson.JsonObject;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.misc.EventKey;
import dev.xinxin.event.world.EventPacketReceive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.netease.chunk.WorldLoader;
import net.netease.gui.GermGameSubElement;
import net.netease.packet.Channel;
import net.netease.packet.GermPacket;
import net.netease.packet.impl.*;
import org.apache.commons.io.IOUtils;
import org.lwjgl.input.Keyboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;

/**
 * @author ByteBreaker
 * create 28/12/2023
 */
@Getter
@Setter
public class PacketProcessor {
    private final Channel forgeChannel = new Channel("FML|HS");
    private final Channel germChannel = new Channel("germplugin-netease");
    private final Channel hyt0Channel = new Channel("hyt0");
    private GermGameSubElement lastGameElement;
    public static byte[] MOD_LIST;
    public static byte[] HYT_REGISTER;
    private static byte[] VEXVIEW_VERSION;
    private final Map<Integer, GermPacket> registry = new HashMap<>();
    private FMLHandshakeClientState currentState = FMLHandshakeClientState.START; // 初始化 currentState
    private final Set<Integer> outstandingKeys = new HashSet<>();
    public static PacketProcessor INSTANCE;

    public PacketProcessor() {
        register(new PacketNegative1());
        register(new Packet04());
        register(new Packet16());
        register(new Packet26());
        register(new Packet67());
        register(new Packet72());
        register(new Packet73());
        register(new Packet79());
        register(new Packet731());
        register(new Packet714());
        register(new Packet723());
        register(new Packet2141());
        register(new Packet76());
        register(new Packet01());
        register(new Packet03());
        String key = "klooy7Bm6jmf4734";
        MOD_LIST = new byte[]{2, 28, 9, 109, 105, 110, 101, 99, 114, 97, 102, 116, 6, 49, 46, 49, 50, 46, 50, 9, 100, 101, 112, 97, 114, 116, 109, 111, 100, 3, 49, 46, 48, 13, 115, 99, 114, 101, 101, 110, 115, 104, 111, 116, 109, 111, 100, 3, 49, 46, 48, 3, 101, 115, 115, 5, 49, 46, 48, 46, 50, 7, 118, 101, 120, 118, 105, 101, 119, 6, 50, 46, 54, 46, 49, 48, 18, 98, 97, 115, 101, 109, 111, 100, 110, 101, 116, 101, 97, 115, 101, 99, 111, 114, 101, 5, 49, 46, 57, 46, 52, 10, 115, 105, 100, 101, 98, 97, 114, 109, 111, 100, 3, 49, 46, 48, 11, 115, 107, 105, 110, 99, 111, 114, 101, 109, 111, 100, 6, 49, 46, 49, 50, 46, 50, 15, 102, 117, 108, 108, 115, 99, 114, 101, 101, 110, 112, 111, 112, 117, 112, 12, 49, 46, 49, 50, 46, 50, 46, 51, 56, 48, 48, 48, 8, 115, 116, 111, 114, 101, 109, 111, 100, 3, 49, 46, 48, 3, 109, 99, 112, 4, 57, 46, 52, 50, 7, 115, 107, 105, 110, 109, 111, 100, 3, 49, 46, 48, 13, 112, 108, 97, 121, 101, 114, 109, 97, 110, 97, 103, 101, 114, 3, 49, 46, 48, 13, 100, 101, 112, 97, 114, 116, 99, 111, 114, 101, 109, 111, 100, 6, 49, 46, 49, 50, 46, 50, 9, 109, 99, 98, 97, 115, 101, 109, 111, 100, 3, 49, 46, 48, 17, 109, 101, 114, 99, 117, 114, 105, 117, 115, 95, 117, 112, 100, 97, 116, 101, 114, 3, 49, 46, 48, 3, 70, 77, 76, 9, 56, 46, 48, 46, 57, 57, 46, 57, 57, 11, 110, 101, 116, 101, 97, 115, 101, 99, 111, 114, 101, 6, 49, 46, 49, 50, 46, 50, 7, 97, 110, 116, 105, 109, 111, 100, 3, 50, 46, 48, 11, 102, 111, 97, 109, 102, 105, 120, 99, 111, 114, 101, 5, 55, 46, 55, 46, 52, 10, 110, 101, 116, 119, 111, 114, 107, 109, 111, 100, 6, 49, 46, 49, 49, 46, 50, 7, 102, 111, 97, 109, 102, 105, 120, 9, 64, 86, 69, 82, 83, 73, 79, 78, 64, 5, 102, 111, 114, 103, 101, 12, 49, 52, 46, 50, 51, 46, 53, 46, 50, 55, 54, 56, 13, 102, 114, 105, 101, 110, 100, 112, 108, 97, 121, 109, 111, 100, 3, 49, 46, 48, 4, 108, 105, 98, 115, 5, 49, 46, 48, 46, 50, 9, 102, 105, 108, 116, 101, 114, 109, 111, 100, 3, 49, 46, 48, 7, 103, 101, 114, 109, 109, 111, 100, 5, 51, 46, 52, 46, 50, 9, 112, 114, 111, 109, 111, 116, 105, 111, 110, 14, 49, 46, 48, 46, 48, 45, 83, 78, 65, 80, 83, 72, 79, 84};

        HYT_REGISTER = AESUtil.decrypt("60ygBu+SjDGX5p08vofaGSoSCYcXcAlAlJebyBAEMqIapu4mzVEDZ76MkfeWGABVqmSaZrQsXbpPHOKeWhQAAm6Hf9nubL4S3jy5rK7ot2zZVYUKivtmdYJP7tuyalmxTzzs/nmeYS+xdCQH1SzAexELf39J/GxHgm/YdvFUqCNgPljdTiVUBzc9zwGav0yh", key);
        VEXVIEW_VERSION = AESUtil.decrypt("GEssAnITtBCuIdmR3B7D1uBMMM0FYrg+4OxZqfGPldMV5/KEAS22q45DK3eeF4Sr1/KSvuplp0LcB4CVKqRHS0ioLY7yjVozG2H0c4JKZ5Q=", key);
    }

    private void register(GermPacket packet) {
        registry.put(packet.getPacketId(), packet);
    }
    private AtomicBoolean cancellationToken;
    @EventTarget
    private void onPacket(EventPacketReceive event) {
        if (event.getPacket() instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload wrapper = ((S3FPacketCustomPayload) event.getPacket());
            String channelName = wrapper.getChannelName();
            PacketBuffer packetBuffer = wrapper.getBufferData();
            if (channelName.equals(germChannel.getName())) {
                processPacket(wrapper.getBufferData());
            } else if (channelName.equals(hyt0Channel.getName())) {
                byte by = packetBuffer.readByte();
                if (by == 0) {
                    String string = packetBuffer.readStringFromBuffer(123456); //lobby1
                    WorldLoader.getInstance().setWorldDirectoryName(string);
                    return;
                }
                if (by == 1) {
                    WorldClient world = mc.theWorld;
                    if (world == null) {
                        return;
                    }
                    WorldLoader.getInstance().loadWorldData(world);
                    int n = packetBuffer.readShort();
                    for (int i = 0; i < n; ++i) {
                        int n2 = packetBuffer.readInt();
                        int n3 = packetBuffer.readInt();
                        WorldLoader.getInstance().loadChunk(n2, n3);
                    }
                    if (cancellationToken != null && !cancellationToken.get()) {
                        cancellationToken.set(true);
                    }
                    cancellationToken = new AtomicBoolean(false);
                    AtomicBoolean currentToken = cancellationToken;
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            if (!currentToken.get()) {
                                mc.getNetHandler().getNetworkManager().sendPacket(new C17PacketCustomPayload("germmod-netease",
                                        new PacketBuffer(Unpooled.buffer().writeInt(3).writeInt(36).writeBoolean(true))));
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            } else if ("FML|HS".equals(channelName) || "REGISTER".equals(channelName) || "UNREGISTER".equals(channelName)) {
                forgeHandshake(packetBuffer.readByte(), packetBuffer);
            } else if ("VexView".equalsIgnoreCase(channelName)) {
                try {
                    byte[] data = new byte[wrapper.getBufferData().readableBytes()];
                    wrapper.getBufferData().getBytes(0, data);
                    String string = new String(decode(data));
                    JsonObject object = GsonUtil.fromJson(string, JsonObject.class);
                    if (object.get("packet_type").getAsString().equals("ver")) {
                        mc.getNetHandler().getNetworkManager().sendPacket(new C17PacketCustomPayload("VexView",
                                new PacketBuffer(Unpooled.buffer().writeBytes(VEXVIEW_VERSION))));
                    }
                    if (object.get("packet_type").getAsString().equals("gui")) {
                        if (object.get("packet_sub_type").getAsString().equals("end")){
                            var xbc = object.get("packet_data").getAsString();
                            var xhl = GsonUtil.fromJson(xbc, JsonObject.class);
                            xbc = xhl.get("base").getAsString();
                            var xxh = "http://ok.166.net/16163/2020-11-25/mm18/1606268690302_lzgtte.jpg";
                            if (xbc.contains(xxh)) {
                                var xjl = StringExtractor.extractBetween(xbc, xxh + "<&>", "<&>" + xxh);
                                Map<String, Object> qiandao = new HashMap<>();
                                qiandao.put("packet_type", "button");
                                qiandao.put("packet_sub_type", xjl);
                                qiandao.put("packet_data", "null");

                                mc.getNetHandler().getNetworkManager().sendPacket(new C17PacketCustomPayload(
                                        "VexView",
                                        new PacketBuffer(Unpooled.buffer().writeBytes(
                                                GZipUtils.gzipEncode(GsonUtil.toJson(qiandao))
                                        ))
                                ));

                                mc.ingameGUI.displayTitle("§a自动签到完成！", "§e已成功升级为 §l2级！", 10, 60, 10);
                            }

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @EventTarget
    private void onKey(EventKey event) {
        if (outstandingKeys.contains(Keyboard.getEventKey())) {
            sendPacket(new Packet03(Keyboard.getEventKey(), Keyboard.getEventKeyState()));
        }
    }

    private static byte[] decode(byte[] b) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(b);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        return IOUtils.toByteArray(gzipInputStream);
    }

    public void sendPacket(GermPacket packet) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeInt(packet.getPacketId());
        packet.writePacketData(buffer);
        germChannel.sendToServer("germmod-netease", buffer);
    }

    private void forgeHandshake(int id, ByteBuf packetBuffer) {
        if (currentState != null) { // 检查 currentState 是否为 null
            currentState.accept(id, packetBuffer, s -> {
                currentState = s;
            });
        } else {
            currentState = FMLHandshakeClientState.START;
        }
    }

    public void processPacket(PacketBuffer packetBuffer) {
        int id = packetBuffer.readInt();
        GermPacket packet = registry.get(id);
        if (packet == null) return;
        packet.readPacketData(packetBuffer);
        packet.process();
    }

    static {
        INSTANCE = new PacketProcessor();
    }
}
