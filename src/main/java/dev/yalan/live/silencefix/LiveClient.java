package dev.yalan.live.silencefix;

import cn.dev.annotations.JNICExclude;
import cn.dev.annotations.JNICInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.xinxin.event.EventManager;
import dev.yalan.live.silencefix.events.EventLiveConnectionStatus;
import dev.yalan.live.silencefix.netty.LiveProto;
import dev.yalan.live.silencefix.netty.codec.FrameDecoder;
import dev.yalan.live.silencefix.netty.codec.FrameEncoder;
import dev.yalan.live.silencefix.netty.codec.crypto.RSADecoder;
import dev.yalan.live.silencefix.netty.codec.crypto.RSAEncoder;
import dev.yalan.live.silencefix.netty.handler.LiveHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.minecraft.client.Minecraft;
import oshi.SystemInfo;
import oshi.hardware.Processor;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@JNICInclude
public class LiveClient {
    public static LiveClient INSTANCE;
    public static final Gson GSON = new GsonBuilder().create();

    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("SilenceFix-Live-Worker"));
    private final LiveReconnectionThread reconnectionThread = new LiveReconnectionThread();
    private final Map<String, Long> offlineTrustedMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isConnecting = new AtomicBoolean();
    private final AtomicReference<String> crashMessage = new AtomicReference<>();
    private final LiveComponent liveComponent = new LiveComponent(this);
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;
    private final String hardwareId;

    public int serversideProtocolVersion = LiveProto.PROTOCOL_VERSION;
    public String autoUsername;
    public String autoPassword;
    public LiveClientSetting clientSetting;
    public LiveUser liveUser;
    public NeteaseApiResult lastNeteaseApiResult;
    public boolean skippedLogin;
    public int onlinePlayerCount;

    private Channel channel;

    public LiveClient() {
        try {
            rsaPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCPUsYvYPsPwIejMEek75wK2nboQOpPa1FCZoiyVbOOVHxz4n8c/xEUIF/f0Zdvj8HELnhZ6txpG3ulv30s0JL7a+4zE5yUBQEC8sI/0TXBjUBItlg5Ig18CUHDpApb9hHq3UivOO9nCg0RBdSxMf0ZpXcpKj1EUHOsnF9CmfPzaKYKiKWsTPGbXTox3IdblL8tzieWxgowcPK8QW5di7iC+q7M6D1SpxGbCMJL4jUWjf/nW3/V7vkXVULoXAoMKTrLJHU0kLPaLrpd028g6OkjSHMMBgzQGT4isveJt5/J2QRkEXQxk0TeCJ0P4EoT8uEYAbKYi0Tatw7qBA0YdSWzAgMBAAECggEADKWCUlTtjQQb6So4mtT/wQoU5ZqGHQWRsS5opR/LImXRviIAEw1uCDIbyPKZY7aHeP7xZLHEyst/hhkW+64t11yR3f+ULeV2eOL11qQTE7tEhFtT/o/tflfiGILFh+bMdEZxWZtzAyBlFcRHcm6Vmuk5p1q69uezGp8zsqXwsODAaXD5+N/L6JzTEz5XJGZqaltnwQm4f8jyoSV9gIeW0cxjLz6pLD8kzyZKMaMMs5gXoYjz+0aVT0JTkCwVxLNwdiirQsa64rLxo3qyPpjW3X24NT5rR2c1ixCejWcMHE/6zcgkHlNUrv5Mg6wayRnGC5gBsksUKs2o1ZJ8egcdBQKBgQC/Ct40ZkJnCW3UAqd8JG8tL+qGcnybRCylQphqmPQoXZiSdJQxDPrfVsDrcGrkqPclZJoT5lfQ+kmqGv4F93gMUIPeFOYjZNUklk/NT+sucRuojJxGIAeHJ6fgXntNiipKcQbV1l8mQL3F1aSELmTkJaWl6q/ascC4s2ccqrcLBQKBgQDADjw3c0ABc3P583/e68qCg9rfwVySEnzvDYF/g3fnFn4JrghQlVAwMBbvt7F/hDoqocbpBcBnegiaEkIIgKAZ7xIuPt5wumM2oGrRGtIW0eAKva/FjzUYbTaQlx1axPUlT+mt9IYkZjj4cfpDpBXqtNaSuh1icpqc3v96zjF7VwKBgARDwtganErejAQalxCPY6f3lN2xepSgvfpmdS6UAYdRJ5HFZnV926/WqPHYjZpTJ0k/aK0fDEDPBYv1lEwfzR9BQOBZSRXrL1LkxB1KNm7P+ZUWpnpuRpy+xuGcWlZNyknlIgjuAyvAcDwVW9nTi08IhB0jEw/nveyhnKCoMvPdAoGANdVHp7tUW5PDFHLeitvI/eB7v3BxxDgOcOt2Ownc2BeD6K6xgfT4bylrpHH2/OlRbJXALZ0BG4AnXRh1DfEP73UFwZS5wRtdp/g7OLWt4dueUyRsWpITre8e9lSFU+YVWQoXVD1QRG+q1GkOX1tlEU7zPlmQ1wGMuSAAuKmHUS0CgYA4Lc6IU7we8RkQTxhn3q470fWDjdLUNkPMOfQa2TwKlnb9sD72zTOnuZULO5kGEZz0LSgSLs7ZfNm0YYCzKzAS/4BYCdcC4mC7jp68GjxsCWc5CGyKQDt9HaG32Bqn/ELZc8KE2n+VLQra3MZPMG+dmGZyQL3J70wrrIgfRPLlRA==")));
            rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApbHpZ5Ri+/S9p+D6JRiNK+AifmOyPPSz3+NSJNFWhmkDFJQA8Qoy7zavZE+iIonAcSfMqRLhuqBTcEy9h1U4aEEzz4feiRATVuxQnufvonxswqhNmQCNNXOnijTG5OGy7aEJyy/kZHOPhQPbRHHP15C+cCoY4vAuZA/9sN1I0MDDuiN4ERAohNO+CV4y6jIzslrDGwuiYUNr2yIu6hXDqv4M8wI5FzNQS+L4pd8R+VXLtv2CN7+ylCN2BlYzrT1htbIcDNFuu42PGvmBBTFIWGaODn5Z25O8cKfgwF2cR6Tt7cldubvXkAcrE4W5NztIxm2/nUdA6+kofNEXQhC0GwIDAQAB")));
            hardwareId = generateHardwareId();
        } catch (Exception e) {
            throw new RuntimeException("Can't init LiveClient", e);
        }
    }

    public void initializeOfflineUser(String username) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        liveUser = new LiveUser("Offline", UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)), payload);
        clientSetting = new LiveClientSetting();
        skippedLogin = true;
    }

    public void connect() {
        if (isOpen() || isConnecting.get()) {
            return;
        }

        isConnecting.set(true);

        final Bootstrap bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast("frame_decoder", new FrameDecoder())
                                .addLast("rsa_decoder", new RSADecoder(rsaPrivateKey))
                                .addLast("frame_encoder", new FrameEncoder())
                                .addLast("rsa_encoder", new RSAEncoder(rsaPublicKey))
                                .addLast("live_handler", new LiveHandler(LiveClient.this));
                    }
                });

        try {
            bootstrap.connect("u75ketvy.svipcdn.cn", 1235).addListener((ChannelFutureListener) future -> {
                isConnecting.set(false);

                if (future.isSuccess()) {
                    LiveProto.sendPacket(future.channel(), LiveProto.createHandshake()).syncUninterruptibly();
                }

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (future.isSuccess()) {
                        channel = future.channel();
                    }

                    EventManager.call(new EventLiveConnectionStatus(future.isSuccess(), future.cause()));
                });
            });
        } catch (RuntimeException e) {
            isConnecting.set(false);
            Minecraft.getMinecraft().addScheduledTask(() ->
                    EventManager.call(new EventLiveConnectionStatus(false, e)));
        }
    }

    @JNICExclude
    public void sendPacket(LiveProto.LivePacket packet) {
        if (isActive()) {
            LiveProto.sendPacket(channel, packet);
        }
    }

    public void closeChannel() {
        if (isOpen()) {
            channel.close();
        }
    }

    public void shutdown() {
        stopReconnectionThread();
        closeChannel();
        workerGroup.shutdownGracefully();
    }

    public void startReconnectionThread() {
        reconnectionThread.start();
    }

    public void stopReconnectionThread() {
        if (reconnectionThread.isAlive()) {
            reconnectionThread.interrupt();
        }
    }

    @JNICExclude
    public String getHardwareId() {
        return hardwareId;
    }

    @JNICExclude
    public boolean isConnecting() {
        return isConnecting.get();
    }

    @JNICExclude
    public AtomicReference<String> getCrashMessage() {
        return crashMessage;
    }

    @JNICExclude
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @JNICExclude
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @JNICExclude
    public LiveComponent getLiveComponent() {
        return liveComponent;
    }

    @JNICExclude
    public Map<String, Long> getOfflineTrustedMap() {
        return offlineTrustedMap;
    }

    private static String generateHardwareId() throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final SystemInfo systemInfo = new SystemInfo();
        final Processor[] processors = systemInfo.getHardware().getProcessors();
        final byte[] digest = messageDigest.digest(("x" + processors.length + "(" + processors[0].getName() + ":" + processors[0].getIdentifier() + ")").getBytes(StandardCharsets.UTF_8));
        final StringBuilder digestSB = new StringBuilder();

        for (byte b : digest) {
            final String hexString = Integer.toHexString(b & 0xFF);

            if (hexString.length() == 1) {
                digestSB.append('0').append(hexString);
            } else {
                digestSB.append(hexString);
            }
        }

        return digestSB.toString();
    }
}
