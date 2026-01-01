package dev.yalan.live.silencefix.gui;

import cn.dev.annotations.JNICExclude;
import cn.dev.annotations.JNICInclude;
import dev.xinxin.SilenceFix;
import dev.xinxin.config.ConfigManager;
import dev.xinxin.event.EventManager;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.utils.client.menu.BetterMainMenu;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.StencilUtil;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.utils.shader.DualBlurUtils;
import dev.xinxin.utils.shader.ShaderUtils;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.events.*;
import dev.yalan.live.silencefix.netty.LiveProto;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@JNICInclude
public class GuiAuthentication extends GuiScreen {
    public static final GuiAuthentication INSTANCE = new GuiAuthentication();
    private static final Logger logger = LogManager.getLogger("GuiAuthentication");
    private static final File accountDataFile = new File(ConfigManager.dir, "LiveAccount.dat");
    private static String savedUsername = "";
    private static String savedPassword = "";

    static {
        try {
            loadAccountData();
        } catch (Exception e) {
            logger.error("Can't load live account data", e);
        }
    }

    private boolean triedConnection;

    private GuiTextField username;
    private GuiTextField password;
    private GuiButton loginButton;

    private String status = "";

    private Rectangle loginRect, registerRect, reconnectRect, exitRect;
    private Rectangle siteRect, groupRect;

    private GuiAuthentication() {
        EventManager.register(this);
    }

    @EventTarget
    public void onLiveChannelActive(EventLiveChannelActive e) {
        if (loginButton != null) loginButton.enabled = true;
        if (username != null) username.setEnabled(true);
        if (password != null) password.setEnabled(true);
    }

    @EventTarget
    public void onLiveChannelInactive(EventLiveChannelInactive e) {
        if (loginButton != null) loginButton.enabled = false;
        if (username != null) username.setEnabled(true);
        if (password != null) password.setEnabled(true);
    }

    @EventTarget
    public void onLiveConnectionStatus(EventLiveConnectionStatus e) {
        if (username != null) username.setEnabled(true);
        if (password != null) password.setEnabled(true);
        if (e.isSuccess()) {
            if (loginButton != null) loginButton.enabled = true;
        } else {
            if (loginButton != null) loginButton.enabled = false;
            if (e.getCause() != null) {
                logger.error("Can't connect to LiveServer", e.getCause());
                status = "无法连接到服务器: " + e.getCause().toString();
            } else {
                status = "无法连接到服务器: 未知错误";
            }
        }
    }

    @EventTarget
    public void onLiveGenericMessage(EventLiveGenericMessage e) {
        if (e.getChannel().equals("Disconnect")) {
            status = e.getMessage();
        }
    }

    @EventTarget
    public void onLiveAuthenticationResult(EventLiveAuthenticationResult e) {
        if (loginButton != null) loginButton.enabled = true;
        if (!e.isSuccess()) {
            status = e.getMessage();
            if (username != null) username.setEnabled(true);
            if (password != null) password.setEnabled(true);
            return;
        }

        // e.isSuccess() == true 但 liveUser == null
        // 说明了用户不是内部及以上级别
        // 我们取消连接并告知用户
//        if (LiveClient.INSTANCE.liveUser == null) {
//            LiveClient.INSTANCE.closeChannel();
//            status = "本客户端只有内部及以上的用户使用";
//            if (username != null) username.setEnabled(true);
//            if (password != null) password.setEnabled(true);
//            return;
//        }

        try {
            saveAccountData();
        } catch (Exception ex) {
            logger.error("Can't save account data", ex);
        }
        LiveClient.INSTANCE.autoUsername = username.getText();
        LiveClient.INSTANCE.autoPassword = password.getText();
        LiveClient.INSTANCE.startReconnectionThread();
        mc.displayGuiScreen(new BetterMainMenu());
        EventManager.unregister(this);
    }


    @JNICExclude
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        SilenceFix.instance.wallpaperEngine.render(width, height);

        float s = Math.max(1.0f, Math.min(width / 800f, height / 600f));
        float sText = Math.max(1f, (float) Math.round(s));

        final int hw = width / 2;
        final int hh = height / 2;

        final int panelW = Math.round(360 * s);
        final int panelH = Math.round(260 * s);
        final int panelX = hw - panelW / 2;
        final int panelY = hh - panelH / 2;
        final float radius = 14f * s;

        final Color accent = new Color(21, 217, 123, 255);
        final Color darkGlass = new Color(20, 20, 24, 120);
        final Color stroke = new Color(255, 255, 255, 24);
        final Color inputBg = new Color(40, 40, 40, 140);
        final Color inputBgIdle = new Color(40, 40, 40, 120);
        final int labelColor = 0xFFA9B1BB;

//        for (int i = 6; i >= 1; i--) {
//            float a = Math.max(6, 22 - i * 3);
//            RenderUtil.drawRound(panelX + i, panelY + i + 1, panelW - i * 2, panelH - i * 2, radius + i, new Color(0, 0, 0, (int) a));
//        }
        RenderUtil.drawRound(panelX + 1, panelY + 1, panelW - 2, panelH - 2, Math.max(1f, radius - 1f), darkGlass);

        // 1) 写模板：把“需要被模糊”的形状画进模板缓冲，不真正上色
        StencilUtil.write(false);

        RenderUtil.resetColor();
        RenderUtil.drawRound(panelX + 1, panelY + 1, panelW - 2, panelH - 2, Math.max(1f, radius - 1f), darkGlass);

        // 2) 开启模板测试，让后续全屏模糊只在模板区生效
        StencilUtil.erase(true);

        DualBlurUtils.renderBlur(
                3,
                4,
                true
        );

        // 3) 收尾，恢复 GL 状态
        StencilUtil.dispose();
//        RenderUtil.drawRound(panelX + 1, panelY + panelH - 2, panelW - 2, 2, Math.max(1f, radius - 1f), stroke);

        String title = "SilenceFix 登录界面";
        int titleY = panelY + Math.round(18 * s);
        {
            int cx = hw;
            int cy = titleY;
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(cx, cy, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            if (FontManager.harmonybold28 != null) {
                int w0 = FontManager.harmonybold28.getStringWidth(title);
                FontManager.harmonybold28.drawString(title, -w0 / 2f + 1, 1, 0x80000000);
                FontManager.harmonybold28.drawString(title, -w0 / 2f, 0, 0xFFFFFFFF);
            } else if (FontManager.harmonybold22 != null) {
                int w0 = FontManager.harmonybold22.getStringWidth(title);
                FontManager.harmonybold22.drawString(title, -w0 / 2f + 1, 1, 0x80000000);
                FontManager.harmonybold22.drawString(title, -w0 / 2f, 0, 0xFFFFFFFF);
            } else {
                int w0 = FontManager.arial20.getStringWidth(title);
                FontManager.arial20.drawString(title, -w0 / 2f + 1, 1, 0x80000000);
                FontManager.arial20.drawString(title, -w0 / 2f, 0, 0xFFFFFFFF);
            }
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }

        int statusY = panelY + Math.round(52 * s);
        {
            int cx = hw;
            int cy = statusY;
            String txt = status;
            int w = FontManager.arial18.getStringWidth(txt);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(cx, cy, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            FontManager.arial18.drawString(txt, -w / 2f + 1, 1, 0x66000000);
            FontManager.arial18.drawString(txt, -w / 2f, 0, 0xFFB8C0CC);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }

        int fieldW = Math.round(panelW - 64 * s);
        int fieldH = Math.round(20 * s);
        int fieldX = hw - fieldW / 2;
        int userY = panelY + Math.round(76 * s);
        int passY = userY + Math.round(40 * s);

        {
            int lx = fieldX;
            int lyUser = userY - Math.round(16 * s);
            int lyPass = passY - Math.round(12 * s);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(lx, lyUser, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            if (FontManager.harmonybold16 != null) {
                FontManager.harmonybold16.drawString("用户名", 1, 1, 0x66000000);
                FontManager.harmonybold16.drawString("用户名", 0, 0, labelColor);
            } else {
                FontManager.arial18.drawString("用户名", 1, 1, 0x66000000);
                FontManager.arial18.drawString("用户名", 0, 0, labelColor);
            }
            net.minecraft.client.renderer.GlStateManager.translate(0, (lyPass - lyUser) / sText, 0);
            if (FontManager.harmonybold16 != null) {
                FontManager.harmonybold16.drawString("密码", 1, 1, 0x66000000);
                FontManager.harmonybold16.drawString("密码", 0, 0, labelColor);
            } else {
                FontManager.arial18.drawString("密码", 1, 1, 0x66000000);
                FontManager.arial18.drawString("密码", 0, 0, labelColor);
            }
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }

        boolean uFocused = username != null && username.isFocused();
        int ux = fieldX - Math.round(2 * s), uy = userY - Math.round(4 * s), uw = fieldW + Math.round(4 * s), uh = fieldH + Math.round(8 * s);
        RenderUtil.drawRound(ux + Math.round(1*s), uy + Math.round(2*s), uw, uh, 7f * s, new Color(0,0,0,80));
        RenderUtil.drawRound(ux, uy, uw, uh, 7f * s, uFocused ? inputBg : inputBgIdle);
        boolean pFocused = password != null && password.isFocused();
        int px = fieldX - Math.round(2 * s), py = passY - Math.round(4 * s), pw = fieldW + Math.round(4 * s), ph = fieldH + Math.round(8 * s);
        RenderUtil.drawRound(px + Math.round(1*s), py + Math.round(2*s), pw, ph, 7f * s, new Color(0,0,0,80));
        RenderUtil.drawRound(px, py, pw, ph, 7f * s, pFocused ? inputBg : inputBgIdle);

        username.setEnableBackgroundDrawing(false);
        password.setEnableBackgroundDrawing(false);
        username.setTextColor(0xFFFFFFFF);
        password.setTextColor(0xFFFFFFFF);
        username.xPosition = fieldX + Math.round(6 * s);
        username.yPosition = userY + Math.round((fieldH - mc.fontRendererObj.FONT_HEIGHT) / 2f);
        username.width = fieldW - Math.round(12 * s);
        username.height = fieldH;
        password.xPosition = fieldX + Math.round(6 * s);
        password.yPosition = passY + Math.round((fieldH - mc.fontRendererObj.FONT_HEIGHT) / 2f);
        password.width = fieldW - Math.round(12 * s);
        password.height = fieldH;

        username.drawTextBox();
        password.drawTextBox();

        int btnY1 = passY + Math.round(40 * s);
        int btnY2 = btnY1 + Math.round(26 * s);

        if (loginRect == null) {
            loginRect = new Rectangle(hw - Math.round(83 * s), hh + Math.round(35 * s), Math.round(80 * s), Math.round(22 * s));
            registerRect = new Rectangle(hw + Math.round(3 * s), hh + Math.round(35 * s), Math.round(80 * s), Math.round(22 * s));
            reconnectRect = new Rectangle(hw - Math.round(83 * s), hh + Math.round(62 * s), Math.round(80 * s), Math.round(22 * s));
            exitRect = new Rectangle(hw + Math.round(3 * s), hh + Math.round(62 * s), Math.round(80 * s), Math.round(22 * s));
        }
        loginRect.setBounds(hw - Math.round(83 * s), btnY1, Math.round(80 * s), Math.round(22 * s));
        registerRect.setBounds(hw + Math.round(3 * s), btnY1, Math.round(80 * s), Math.round(22 * s));
        reconnectRect.setBounds(hw - Math.round(83 * s), btnY2, Math.round(80 * s), Math.round(22 * s));
        exitRect.setBounds(hw + Math.round(3 * s), btnY2, Math.round(80 * s), Math.round(22 * s));

        drawButtonRect(loginRect, "登录", mouseX, mouseY, new Color(21, 188, 123, 200), new Color(21, 188, 123, 230), s, sText);
        drawButtonRect(registerRect, "注册", mouseX, mouseY, new Color(255, 255, 255, 42), new Color(255, 255, 255, 72), s, sText);
        drawButtonRect(reconnectRect, "重连", mouseX, mouseY, new Color(255, 255, 255, 42), new Color(255, 255, 255, 72), s, sText);
        drawButtonRect(exitRect, "退出", mouseX, mouseY, new Color(205, 80, 80, 180), new Color(205, 100, 100, 220), s, sText);

        String siteText = "点我进入官网：heshuyou.xyz";
        String groupText = "点我进入官群：请勿再次添加";
        int linkBaseY = btnY2 + Math.round(36 * s);

        {
            int cx = hw;
            int cy = linkBaseY;
            int w = FontManager.arial18.getStringWidth(siteText);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(cx, cy, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            FontManager.arial18.drawString(siteText, -w / 2f + 1, 1, 0x66000000);
            FontManager.arial18.drawString(siteText, -w / 2f, 0, 0xFFE5EAF0);
            net.minecraft.client.renderer.GlStateManager.popMatrix();

            siteX = cx - w / 2;
            siteY = cy;
            siteW = w;
            siteH = FontManager.arial18.getHeight();
        }
        {
            int cx = hw;
            int cy = linkBaseY + Math.round((FontManager.arial18.getHeight() + 6) * s);
            int w = FontManager.arial18.getStringWidth(groupText);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(cx, cy, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            FontManager.arial18.drawString(groupText, -w / 2f + 1, 1, 0x66000000);
            FontManager.arial18.drawString(groupText, -w / 2f, 0, 0xFFE5EAF0);
            net.minecraft.client.renderer.GlStateManager.popMatrix();

            groupX = cx - w / 2;
            groupY = cy;
            groupW = w;
            groupH = FontManager.arial18.getHeight();
        }

        boolean hoverSite = mouseX >= siteX && mouseX <= siteX + siteW && mouseY >= siteY && mouseY <= siteY + siteH;
        boolean hoverGroup = mouseX >= groupX && mouseX <= groupX + groupW && mouseY >= groupY && mouseY <= groupY + groupH;
        if (hoverSite) RenderUtil.drawRect(siteX, siteY + siteH, siteX + siteW, siteY + siteH + 1, accent.getRGB());
        if (hoverGroup) RenderUtil.drawRect(groupX, groupY + groupH, groupX + groupW, groupY + groupH + 1, accent.getRGB());

        {
            int x = Math.round(6 * s);
            int y = height - mc.fontRendererObj.FONT_HEIGHT - Math.round(6 * s);
            String ls = "LiveServer: " + getLiveConnectionStatus();
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(x, y, 0);
            net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
            FontManager.arial18.drawString(ls, 1, 1, 0x66000000);
            FontManager.arial18.drawString(ls, 0, 0, 0xFFFFFFFF);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }





    private void drawButtonRect(Rectangle rect, String text, int mouseX, int mouseY, Color normal, Color hover, float s, float sText) {
        boolean hovered = rect.contains(mouseX, mouseY);
        RenderUtil.drawRound(rect.x, rect.y, rect.width, rect.height, 5f * s, hovered ? hover : normal);

        int cx = rect.x + rect.width / 2;
        int cy = rect.y + rect.height / 2;
        int w = FontManager.arial18.getStringWidth(text);
        int h = FontManager.arial18.getHeight();

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(cx, cy, 0);
        net.minecraft.client.renderer.GlStateManager.scale(sText, sText, 1f);
        FontManager.arial18.drawString(text, -w / 2f, -h / 2f + 1, 0xFFFFFFFF);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }







    @JNICExclude
    private String getLiveConnectionStatus() {
        if (LiveClient.INSTANCE.isActive()) {
            return EnumChatFormatting.GREEN + "已连接";
        }
        if (LiveClient.INSTANCE.isConnecting()) {
            return EnumChatFormatting.YELLOW + "连接中...";
        }
        return EnumChatFormatting.RED + "无连接";
    }

    @Override
    public void initGui() {
        final int hw = width / 2;
        final int hh = height / 2;
        final String lastUsername;
        final String lastPassword;

        if (username == null) {
            lastUsername = savedUsername;
        } else {
            lastUsername = username.getText();
        }

        if (password == null) {
            lastPassword = savedPassword;
        } else {
            lastPassword = password.getText();
        }

        username = new GuiTextField(0, mc.fontRendererObj, hw - 83, hh - 35, 166, 20);
        username.setMaxStringLength(32);
        username.setText(lastUsername);
        password = new GuiTextField(1, mc.fontRendererObj, hw - 83, hh, 166, 20);
        password.setMaxStringLength(64);
        password.setText(lastPassword);

        loginButton = new GuiButton(0, hw - 83, hh + 35, 80, 20, "登录");
        loginButton.enabled = LiveClient.INSTANCE.isActive();

        loginRect     = new Rectangle(hw - 83, hh + 35, 80, 22);
        registerRect  = new Rectangle(hw + 3,  hh + 35, 80, 22);
        reconnectRect = new Rectangle(hw - 83, hh + 62, 80, 22);
        exitRect      = new Rectangle(hw + 3,  hh + 62, 80, 22);
        siteRect      = new Rectangle(hw - 160, hh + 90, 320, 22);
        groupRect     = new Rectangle(hw - 160, hh + 116, 320, 22);

        if (!triedConnection) {
            triedConnection = true;
            LiveClient.INSTANCE.connect();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0 -> {
                if (LiveClient.INSTANCE.isActive()) {
                    if (loginButton != null) loginButton.enabled = false;
                    username.setEnabled(false);
                    password.setEnabled(false);
                    LiveClient.INSTANCE.sendPacket(LiveProto.createAuthentication(username.getText(), password.getText(), LiveClient.INSTANCE.getHardwareId()));
                }
            }
            case 1 -> {
                Desktop.getDesktop().browse(URI.create("https://live.heshuyou.xyz/SilenceFix/html?name=Register&hardwareId=" + URLEncoder.encode(LiveClient.INSTANCE.getHardwareId(), StandardCharsets.UTF_8)));
            }
            case 2 -> {
                LiveClient.INSTANCE.connect();
            }
            case 3 -> {
                mc.shutdown();
            }
        }
    }

    @Override
    public void updateScreen() {
        username.updateCursorCounter();
        password.updateCursorCounter();
        if (LiveProto.PROTOCOL_VERSION != LiveClient.INSTANCE.serversideProtocolVersion
                || (LiveClient.INSTANCE.clientSetting != null
                && !SilenceFix.VERSION.equals(LiveClient.INSTANCE.clientSetting.paidVersion))) {
            status = EnumChatFormatting.RED + "去群里下载最新版!";
            if (loginButton != null) loginButton.enabled = false;
            if (LiveClient.INSTANCE.isActive()) {
                LiveClient.INSTANCE.shutdown();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        username.textboxKeyTyped(typedChar, keyCode);
        password.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) return;

        if (loginRect.contains(mouseX, mouseY)) {
            if (LiveClient.INSTANCE.isActive()) {
                if (loginButton != null) loginButton.enabled = false;
                username.setEnabled(false);
                password.setEnabled(false);
                LiveClient.INSTANCE.sendPacket(LiveProto.createAuthentication(username.getText(), password.getText(), LiveClient.INSTANCE.getHardwareId()));
            }
            return;
        }
        if (registerRect.contains(mouseX, mouseY)) {
            Desktop.getDesktop().browse(URI.create("https://live.heshuyou.xyz/SilenceFix/html?name=Register&hardwareId=" + URLEncoder.encode(LiveClient.INSTANCE.getHardwareId(), StandardCharsets.UTF_8)));
            return;
        }
        if (reconnectRect.contains(mouseX, mouseY)) {
            LiveClient.INSTANCE.connect();
            return;
        }
        if (exitRect.contains(mouseX, mouseY)) {
            mc.shutdown();
            return;
        }

        boolean clickSite  = mouseX >= siteX  && mouseX <= siteX  + siteW  && mouseY >= siteY  && mouseY <= siteY  + siteH;
        boolean clickGroup = mouseX >= groupX && mouseX <= groupX + groupW && mouseY >= groupY && mouseY <= groupY + groupH;

        if (clickSite) {
            Desktop.getDesktop().browse(URI.create("https://heshuyou.xyz"));
            return;
        }
        if (clickGroup) {
            Desktop.getDesktop().browse(URI.create("https://qm.qq.com/q/B4zDK5b0w8"));
        }
    }


    private int siteX, siteY, siteW, siteH;
    private int groupX, groupY, groupW, groupH;

    private static void loadAccountData() throws Exception {
        if (!accountDataFile.exists()) {
            return;
        }
        final byte[] data = FileUtils.readFileToByteArray(accountDataFile);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        final SecretKey key = new SecretKeySpec(Base64.getDecoder().decode("z2SSbtrapztLIPpZxCDBzA=="), "AES");
        final byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD(accountDataFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
        final byte[] out = cipher.doFinal(data, iv.length, data.length - iv.length);
        final String[] split = new String(out, StandardCharsets.UTF_8).split(System.lineSeparator());
        savedUsername = split[0];
        savedPassword = split[1];
    }

    private void saveAccountData() throws Exception {
        final byte[] data = (username.getText() + System.lineSeparator() + password.getText()).getBytes(StandardCharsets.UTF_8);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        final SecretKey key = new SecretKeySpec(Base64.getDecoder().decode("z2SSbtrapztLIPpZxCDBzA=="), "AES");
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD(accountDataFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
        final byte[] out = new byte[12 + cipher.getOutputSize(data.length)];
        System.arraycopy(iv, 0, out, 0, iv.length);
        cipher.doFinal(data, 0, data.length, out, 12);
        FileUtils.writeByteArrayToFile(accountDataFile, out);
    }
}
