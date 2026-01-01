package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import dev.xinxin.gui.altmanager.GuiAltManager;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.NeteaseApiResult;
import dev.yalan.live.silencefix.netty.LiveProto;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.login.server.S03PacketEnableCompression;
import net.minecraft.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class NetHandlerLoginClient
        implements INetHandlerLoginClient {
    public static final Semaphore semaphore = new Semaphore(0);

    private static final Logger logger = LogManager.getLogger();
    private final Minecraft mc;
    private final GuiScreen previousGuiScreen;
    private final NetworkManager networkManager;
    private GameProfile gameProfile;

    public NetHandlerLoginClient(NetworkManager networkManagerIn, Minecraft mcIn, GuiScreen p_i45059_3_) {
        this.networkManager = networkManagerIn;
        this.mc = mcIn;
        this.previousGuiScreen = p_i45059_3_;
    }

    @Override
    public void handleEncryptionRequest(S01PacketEncryptionRequest packetIn) {
        final SecretKey secretkey = CryptManager.createNewSharedKey();
        String s2 = packetIn.getServerId();
        PublicKey publickey = packetIn.getPublicKey();
        String s1 = new BigInteger(CryptManager.getServerIdHash(s2, publickey, secretkey)).toString(16);
        if (this.mc.getCurrentServerData() != null && this.mc.getCurrentServerData().isOnLAN()) {
            if (GuiAltManager.currentProxyServer != null){
                logger.info("Attempting to encrypt server: " + s1);
                LiveClient.INSTANCE.sendPacket(LiveProto.createJoinNeteaseServer(
                        GuiAltManager.currentProxyServer.neteaseAccount.authInfo.entity.entity_id,
                        GuiAltManager.currentProxyServer.neteaseAccount.authInfo.entity.token,
                        s1,
                        GuiAltManager.currentProxyServer.serverId,
                        GuiAltManager.currentProxyServer.serverVersion,
                        GuiAltManager.currentProxyServer.modHashList
                ));

                try {
                    semaphore.tryAcquire(10, TimeUnit.SECONDS);
                }catch (InterruptedException e) {}
            }else{
                try {
                    this.getSessionService().joinServer(this.mc.getSession().getProfile(), this.mc.getSession().getToken(), s1);
                }
                catch (AuthenticationException var10) {
                    logger.warn("Couldn't connect to auth servers but will continue to join LAN");
                }
            }
        } else {
            try {
                this.getSessionService().joinServer(this.mc.getSession().getProfile(), this.mc.getSession().getToken(), s1);
            }
            catch (AuthenticationUnavailableException var7) {
                this.networkManager.closeChannel(new ChatComponentTranslation("disconnect.loginFailedInfo", new ChatComponentTranslation("disconnect.loginFailedInfo.serversUnavailable", new Object[0])));
                return;
            }
            catch (InvalidCredentialsException var8) {
                this.networkManager.closeChannel(new ChatComponentTranslation("disconnect.loginFailedInfo", new ChatComponentTranslation("disconnect.loginFailedInfo.invalidSession", new Object[0])));
                return;
            }
            catch (AuthenticationException authenticationexception) {
                this.networkManager.closeChannel(new ChatComponentTranslation("disconnect.loginFailedInfo", authenticationexception.getMessage()));
                return;
            }
        }
        this.networkManager.sendPacket(new C01PacketEncryptionResponse(secretkey, publickey, packetIn.getVerifyToken()), (GenericFutureListener<? extends Future<? super Void>>)new GenericFutureListener<Future<? super Void>>(){

            public void operationComplete(Future<? super Void> p_operationComplete_1_) throws Exception {
                NetHandlerLoginClient.this.networkManager.enableEncryption(secretkey);
            }
        }, new GenericFutureListener[0]);
    }

    private MinecraftSessionService getSessionService() {
        return this.mc.getSessionService();
    }

    @Override
    public void handleLoginSuccess(S02PacketLoginSuccess packetIn) {
        this.gameProfile = packetIn.getProfile();
        this.networkManager.setConnectionState(EnumConnectionState.PLAY);
        this.networkManager.setNetHandler(new NetHandlerPlayClient(this.mc, this.previousGuiScreen, this.networkManager, this.gameProfile));
    }

    @Override
    public void onDisconnect(IChatComponent reason) {
        this.mc.displayGuiScreen(new GuiDisconnected(this.previousGuiScreen, "connect.failed", reason));
    }

    @Override
    public void handleDisconnect(S00PacketDisconnect packetIn) {
        IChatComponent chatComponent = packetIn.func_149603_c();

        if (chatComponent instanceof ChatComponentText text) {
            final String message = text.getUnformattedText();

            if ("验证失败,请尝试重启启动器!".equals(message)) {
                final NeteaseApiResult result = LiveClient.INSTANCE.lastNeteaseApiResult;

                if (result != null) {
                    if (result.getCode() == -114515) {
                        chatComponent = new ChatComponentText("只有 §d大粉丝 §e内部 §c管理员 §f才可以全天内置进服-花雨庭 因为你是§a公益§f无法全天内置进服！当然了！我们也有布吉岛最强的客户端请联系欣欣咨询哦！");
                    } else {
                        chatComponent = new ChatComponentText("重新客户端才能给网易发送进服请求！ Code: " + result.getCode() + ", Message: " + result.getMessage()+" 重新启动客户端。");
                    }

                    LiveClient.INSTANCE.lastNeteaseApiResult = null;
                }
            } else if (message.contains("由于您涉嫌作弊被禁止登录游戏")) {
                if (message.contains("364天") || message.contains("13天") || message.contains("6天")) {
                    chatComponent = new ChatComponentText(EnumChatFormatting.RED + "被花雨庭封禁的时间：14/365天  "+EnumChatFormatting.YELLOW+"学生党，你玩的很好，你是被花雨庭客服封禁的，与你的操作无关！");
                } else if (message.contains("2天")) {
                    chatComponent = new ChatComponentText(EnumChatFormatting.RED + "被花雨庭封禁的时间：3天  "+EnumChatFormatting.YELLOW+"学生党，你被反作弊封禁了，请去群公告观看视频教学，学习如何正确的使用客户端。");
                    chatComponent = new ChatComponentText(EnumChatFormatting.GREEN + "\n修复IPban的教学：  "+EnumChatFormatting.RED+"新号也封就属于IPban  "+EnumChatFormatting.YELLOW+"拔出光猫电源，等待30秒，然后接入电源即可。光猫不是路由器，是小机顶盒。不懂可百度。");

                }
            }
        }

        this.networkManager.closeChannel(chatComponent);
    }

    @Override
    public void handleEnableCompression(S03PacketEnableCompression packetIn) {
        if (!this.networkManager.isLocalChannel()) {
            this.networkManager.setCompressionTreshold(packetIn.getCompressionTreshold());
        }
    }
}

