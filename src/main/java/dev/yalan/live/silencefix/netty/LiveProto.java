package dev.yalan.live.silencefix.netty;

import cn.dev.annotations.JNICExclude;
import cn.dev.annotations.JNICInclude;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

import java.util.UUID;
import java.util.function.Consumer;

@JNICInclude
@SuppressWarnings("CodeBlock2Expr")
public class LiveProto {
    public static final int PROTOCOL_VERSION = 0;
    public static final UUID NULL_UUID = new UUID(0L, 0L);

    @JNICExclude
    public static ChannelFuture sendPacket(Channel channel, LivePacket packet) {
        final ByteBuf buf = channel.alloc().buffer();
        packet.write(new LiveByteBuf(buf));

        return channel.writeAndFlush(buf).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public static LivePacket createHandshake() {
        return new LivePacket(0);
    }

    public static LivePacket createVerify(String verifyString, String clientZoneRegion, long clientTime, int clientType) {
        return new LivePacket(1, (buf) -> {
            buf.writeUTF(verifyString);
            buf.writeUTF(clientZoneRegion);
            buf.writeLong(clientTime);
            buf.writeInt(clientType);
        });
    }

    public static LivePacket createKeepAlive() {
        return new LivePacket(2);
    }

    public static LivePacket createAuthentication(String username, String password, String hardwareId) {
        return new LivePacket(3, (buf) -> {
            buf.writeUTF(username);
            buf.writeUTF(password);
            buf.writeUTF(hardwareId);
        });
    }

    public static LivePacket createChat(String message) {
        return new LivePacket(4, (buf) -> {
            buf.writeUTF(message);
        });
    }

    public static LivePacket createUpdateMinecraftProfile(UUID mcUUID, String mcName) {
        return new LivePacket(5, (buf) -> {
            buf.writeUUID(mcUUID);
            buf.writeUTF(mcName);
        });
    }

    public static LivePacket createRemoveMinecraftProfile() {
        return new LivePacket(6);
    }

    public static LivePacket createQueryMinecraftProfile(GameProfile profile) {
        return createQueryMinecraftProfile(EntityPlayer.getUUID(profile));
    }

    public static LivePacket createQueryMinecraftProfile(UUID mcUUID) {
        return new LivePacket(7, (buf) -> {
            buf.writeUUID(mcUUID);
        });
    }

    public static LivePacket createExecuteCommand(UUID executionId, String command) {
        return new LivePacket(8, (buf) -> {
            buf.writeUUID(executionId);
            buf.writeUTF(command);
        });
    }

    public static LivePacket createUpdateBedPos(boolean isRemove, UUID mcUUID, BlockPos pos) {
        return new LivePacket(9, (buf) -> {
            buf.writeBoolean(isRemove);
            buf.writeUUID(mcUUID);
            buf.writeLong(pos.toLong());
        });
    }

    public static LivePacket createQueryPlayerBed(UUID mcUUID) {
        return new LivePacket(10, (buf) -> {
            buf.writeUUID(mcUUID);
        });
    }

    public static LivePacket createKickPlayer(String targetName, String reason) {
        return new LivePacket(11, (buf) -> {
            buf.writeUTF(targetName);
            buf.writeUTF(reason);
        });
    }

    public static LivePacket createQueryOnlineUsersCount() {
        return new LivePacket(12);
    }

    public static LivePacket createJoinNeteaseServer(String userId, String userToken, String serverIdHash, String serverId, String serverVersion, String modHashList) {
        return new LivePacket(13, (buf) -> {
            buf.writeUTF(userId);
            buf.writeUTF(userToken);
            buf.writeUTF(serverIdHash);
            buf.writeUTF(serverId);
            buf.writeUTF(serverVersion);
            buf.writeUTF(modHashList);
        });
    }

    public static LivePacket createGetNeteaseCookie() {
        return new LivePacket(14);
    }

    public static class LivePacket {
        private final int id;
        private final Consumer<LiveByteBuf> writeFunc;

        public LivePacket(int id) {
            this(id, null);
        }

        public LivePacket(int id, Consumer<LiveByteBuf> writeFunc) {
            this.id = id;
            this.writeFunc = writeFunc;
        }

        public void write(LiveByteBuf buf) {
            buf.writeInt(id);

            if (writeFunc != null) {
                writeFunc.accept(buf);
            }
        }
    }
}
