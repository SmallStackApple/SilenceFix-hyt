package dev.yalan.live.silencefix.netty.handler;

import cn.dev.annotations.JNICInclude;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xinxin.event.EventManager;
import dev.xinxin.gui.altmanager.GuiAltLogin;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveClientSetting;
import dev.yalan.live.silencefix.LiveUser;
import dev.yalan.live.silencefix.NeteaseApiResult;
import dev.yalan.live.silencefix.events.*;
import dev.yalan.live.silencefix.netty.LiveByteBuf;
import dev.yalan.live.silencefix.netty.LiveProto;
import dev.yalan.live.silencefix.netty.codec.crypto.AESDecoder;
import dev.yalan.live.silencefix.netty.codec.crypto.AESEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiConsumer;

@JNICInclude
public class LiveHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Logger logger = LogManager.getLogger("LiveHandler");
    private final JsonParser jsonParser = new JsonParser();
    private final HashMap<Integer, BiConsumer<ChannelHandlerContext, LiveByteBuf>> functionMap = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final LiveClient live;

    private boolean notCheckedProtocolVersion = true;

    public LiveHandler(LiveClient live) {
        this.live = live;
        this.functionMap.put(0, this::handleHandshake);
        this.functionMap.put(1, this::handleKeepAlive);
        this.functionMap.put(2, this::handleGenericMessage);
        this.functionMap.put(3, this::handleAuthenticationResult);
        this.functionMap.put(4, this::handleChat);
        this.functionMap.put(5, this::handleGenericQueryResult);
        this.functionMap.put(6, this::handleCommandOut);
        this.functionMap.put(7, this::handleKicked);
        this.functionMap.put(8, this::handleNeteaseAPIResult);
        this.functionMap.put(9, this::handleNeteaseCookie);
        this.functionMap.put(10, this::handleClientSetting);
    }

    private void handleHandshake(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final SecretKeySpec aesKey = new SecretKeySpec(buf.readByteArray(16), "AES");
        final byte[] aesAAD = "cubk".getBytes(StandardCharsets.UTF_8);

        ctx.pipeline().replace("rsa_decoder", "aes_decoder", new AESDecoder(aesKey, aesAAD));
        ctx.pipeline().replace("rsa_encoder", "aes_encoder", new AESEncoder(aesKey, aesAAD));
        LiveProto.sendPacket(ctx.channel(), LiveProto.createVerify("~~SFIX~~", ZoneId.systemDefault().getId(), Instant.now().toEpochMilli(), 547893455)).syncUninterruptibly();

        mc.addScheduledTask(() -> {
            if(live.autoUsername != null && live.autoPassword != null) {
                LiveProto.sendPacket(ctx.channel(), LiveProto.createAuthentication(live.autoUsername, live.autoPassword, live.getHardwareId()));
            }
        });
    }

    private void handleKeepAlive(ChannelHandlerContext ctx, LiveByteBuf buf) {
        LiveProto.sendPacket(ctx.channel(), LiveProto.createKeepAlive());
    }

    private void handleGenericMessage(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String channel = buf.readUTF();
        final String message = buf.readUTF();

        logger.info("[GenericMessage] Channel({}): {}", channel, message);

        mc.addScheduledTask(() -> {
            EventManager.call(new EventLiveGenericMessage(channel, message));
        });
    }

    private void handleAuthenticationResult(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final boolean isSuccess = buf.readBoolean();
        final String message = buf.readUTF();
        final UUID userId;
        final String username;
        final String userRank;
        final String userLevel;
        final String userQQ;

        if (isSuccess) {
            userId = buf.readUUID();
            username = buf.readUTF();
            userRank = buf.readUTF();
            userLevel = buf.readUTF();
            userQQ = buf.readUTF();
        } else {
            userId = new UUID(0L, 0L);
            username = "";
            userRank = "";
            userLevel = "";
            userQQ = "";
        }

        logger.info("[Authentication] isSuccess({}) Message({})", isSuccess, message);

        mc.addScheduledTask(() -> {
            if (isSuccess && LiveUser.Level.of(userLevel).isHigherOrSame(LiveUser.Level.PAID)) {
                final JsonObject payload = new JsonObject();
                payload.addProperty("username", username);
                payload.addProperty("rank", userRank);
                payload.addProperty("level", userLevel);
                payload.addProperty("qq", userQQ);

                live.liveUser = new LiveUser("SilenceFix", userId, payload);
            }

            EventManager.call(new EventLiveAuthenticationResult(isSuccess, message));
        });
    }

    private void handleChat(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String channel = buf.readUTF();
        final String payloadString = buf.readUTF();

        mc.addScheduledTask(() -> live.getLiveComponent().handleChat(channel, payloadString));
    }

    private void handleGenericQueryResult(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String channel = buf.readUTF();
        final String jsonString = buf.readUTF();
        final JsonObject json = jsonParser.parse(jsonString).getAsJsonObject();

        switch (channel) {
            case "mcProfile" -> mc.addScheduledTask(() ->
                    live.getLiveComponent().handleQueryResultMinecraftProfile(
                        UUID.fromString(json.get("mcUUID").getAsString()),
                        json.get("clientId").getAsString(),
                        UUID.fromString(json.get("userId").getAsString()),
                        json.get("userPayload").getAsString()
                    ));
            case "mcBedPos" -> mc.addScheduledTask(() -> {
                final UUID mcUUID = UUID.fromString(json.get("mcUUID").getAsString());
                final BlockPos pos = BlockPos.fromLong(json.get("pos").getAsLong());

                live.getLiveComponent().handleQueryResultBedPos(mcUUID, pos);
            });
            case "onlineUsers" -> mc.addScheduledTask(() -> {
                live.onlinePlayerCount = json.get("onlineUsers").getAsInt();
            });
            default -> logger.warn("Unknown QueryChannel({})", channel);
        }
    }

    private void handleCommandOut(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final UUID executionId = buf.readUUID();
        final String outType = buf.readUTF();
        final String out = buf.readUTF();

        mc.addScheduledTask(() -> live.getLiveComponent().handleCommandOut(executionId, outType, out));
    }

    private void handleKicked(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String who = buf.readUTF();
        final String reason = buf.readUTF();

        mc.addScheduledTask(() -> {
            if (mc.theWorld != null && !mc.isSingleplayer()) {
                mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("你被" + who + "踢出: " + reason));
            }
        });
    }

    private void handleNeteaseAPIResult(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String json = buf.readUTF();

        mc.addScheduledTask(() -> {
            NetHandlerLoginClient.semaphore.release();

            logger.info("[NeteaseAPIResult] {}", json);

            try {
                live.lastNeteaseApiResult = LiveClient.GSON.fromJson(json, NeteaseApiResult.class);
            } catch (Exception e) {
                logger.error("Can't parse NeteaseAPI response", e);
            }
        });
    }

    private void handleNeteaseCookie(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String cookie = buf.readUTF();

        mc.addScheduledTask(() -> {
            if (mc.currentScreen instanceof GuiAltLogin al) {
                if (al.getCookieButton != null && al.username != null) {
                    al.getCookieButton.enabled = true;
                    al.username.setText(cookie);
                    al.status = EnumChatFormatting.GREEN + "获取成功! 请点击 '添加' 添加到账号列表里";
                }
            }
        });
    }

    private void handleClientSetting(ChannelHandlerContext ctx, LiveByteBuf buf) {
        final String json = buf.readUTF();

        mc.addScheduledTask(() -> {
            final LiveClientSetting clientSetting = LiveClient.GSON.fromJson(json, LiveClientSetting.class);

            live.clientSetting = clientSetting;
            live.getLiveComponent().onClientSettingUpdated(clientSetting);
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            logger.warn("Channel{} was sent an malformed(length is less than 4) packet", ctx.channel());
            return;
        }

        if (notCheckedProtocolVersion) {
            notCheckedProtocolVersion = false;

            final int serversideProtocolVersion = buf.readInt();

            live.serversideProtocolVersion = serversideProtocolVersion;

            if (serversideProtocolVersion != LiveProto.PROTOCOL_VERSION) {
                logger.warn("ProtocolVersion doesn't match ({}, {})", LiveProto.PROTOCOL_VERSION, serversideProtocolVersion);
                ctx.channel().close();
            }

            return;
        }

        final int packetId = buf.readInt();
        final BiConsumer<ChannelHandlerContext, LiveByteBuf> handleFunction = functionMap.get(packetId);

        if (handleFunction == null) {
            logger.warn("Channel{} was sent an unrecognized PacketId({})", ctx.channel(), packetId);
            return;
        }

        handleFunction.accept(ctx, new LiveByteBuf(buf));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Channel active");

        mc.addScheduledTask(() -> EventManager.call(new EventLiveChannelActive()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Channel inactive");

        mc.addScheduledTask(() -> {
            EventManager.call(new EventLiveChannelInactive());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("LiveService Error occurred", cause);

        ctx.channel().close();

        mc.addScheduledTask(() -> EventManager.call(new EventLiveChannelException(cause)));
    }
}
