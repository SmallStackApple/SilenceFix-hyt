package dev.xinxin.gui.clickgui.irc;

import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import dev.xinxin.gui.ui.modules.ShaderElement;
import dev.xinxin.module.modules.render.PostProcessing;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.netty.LiveProto;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;

public class User extends GuiScreen {

    private final IRCData data;
    private final GuiScreen parent;

    private boolean mouse0 = false;

    private enum Mode { INFO, BAN_REASON, KICK_REASON }
    private Mode mode = Mode.INFO;

    private boolean focusedReason = false;
    private boolean focusedTime = false;

    private String reasonText = "";
    private String timeText = "";

    public User(IRCData ircData) {
        this.data = ircData;
        this.parent = null;
    }

    public User(GuiScreen parent, IRCData ircData) {
        this.data = ircData;
        this.parent = parent;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        mouse0 = button == 0;
        if (mode != Mode.INFO) {
            int guiWidth = 360;
            int guiHeight = 220;
            int x = width / 2 - guiWidth / 2;
            int y = height / 2 - guiHeight / 2;
            focusedReason = RenderUtil.isHovering(x + 20, y + 56, 320, 20, mouseX, mouseY);
            if (mode == Mode.BAN_REASON) focusedTime = RenderUtil.isHovering(x + 20, y + 86, 320, 20, mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (mode != Mode.INFO) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mode = Mode.INFO;
                focusedReason = false;
                focusedTime = false;
                return;
            }
            boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (focusedReason) {
                if (keyCode == Keyboard.KEY_BACK && !reasonText.isEmpty()) {
                    reasonText = reasonText.substring(0, reasonText.length() - 1);
                } else if (ctrl && keyCode == Keyboard.KEY_V) {
                    try {
                        String clip = getClipboardString();
                        if (clip != null && !clip.isEmpty()) {
                            for (char c : clip.toCharArray()) if (ChatAllowedCharacters.isAllowedCharacter(c)) reasonText += c;
                        }
                    } catch (Exception ignored) {}
                } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                    reasonText += typedChar;
                }
            }
            if (mode == Mode.BAN_REASON && focusedTime) {
                if (keyCode == Keyboard.KEY_BACK && !timeText.isEmpty()) {
                    timeText = timeText.substring(0, timeText.length() - 1);
                } else if (Character.isDigit(typedChar)) {
                    timeText += typedChar;
                }
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        mouse0 = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int guiWidth = 360, guiHeight = 220;
        int x = width / 2 - guiWidth / 2;
        int y = height / 2 - guiHeight / 2;

        drawBackground(guiWidth, guiHeight, x, y);

        String title = mode == Mode.INFO ? "IRC 用户信息" :
                (mode == Mode.BAN_REASON ? "封禁操作" : "踢出操作");
        drawTitle(title, x, y, guiWidth);

        drawAction(x + 10, y + 8, 64, 20, mouseX, mouseY, "返回", () -> {
            mc.displayGuiScreen(parent != null ? parent : new AdminPanel());
        });

        if (mode == Mode.INFO) {
            drawUserInfo(x, y, guiHeight);
            drawInfoActions(x, y, guiWidth, guiHeight, mouseX, mouseY);
        } else {
            drawReasonInputs(x, y, mouseX, mouseY);
            drawReasonActions(x, y, guiWidth, guiHeight, mouseX, mouseY);
        }

        PostProcessing.INSTANCE.blurScreen();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }


    private void drawBackground(int guiWidth, int guiHeight, int x, int y) {
        RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(20, 20, 20, 80));
        RoundedUtils.drawRoundOutline(x, y, guiWidth, guiHeight, 10, 1.0f,
                new Color(0, 0, 0, 50), new Color(255, 255, 255, 18));
        RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(255, 255, 255, 8));
        ShaderElement.getTasks().add(() -> RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(255, 255, 255, 255)));
    }

    private void drawTitle(String title, int x, int y, int guiWidth) {
        int tw = FontManager.chineseFont18.getStringWidth(title);
        FontManager.chineseFont18.drawString(title, x + guiWidth / 2f - tw / 2f, y + 10, new Color(255, 255, 255, 230).getRGB());
    }

    private void drawUserInfo(int x, int y, int guiHeight) {
        int lineH = FontManager.chineseFont18.getHeight() + 6;
        int infoY = y + 40;
        FontManager.chineseFont18.drawString("用户名：" + data.name(), x + 18, infoY, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString("游戏名：" + data.gameName(), x + 18, infoY += lineH, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString("当前QQ：" + data.qq(), x + 18, infoY += lineH, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString("IRC等级：" + data.level(), x + 18, infoY += lineH, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString("IRC头衔：" + data.rank(), x + 18, infoY += lineH, Color.WHITE.getRGB());
    }

    private void drawInfoActions(int x, int y, int guiWidth, int guiHeight, int mouseX, int mouseY) {
        int btnW = 88, btnH = 22, spacing = 12;
        int btnY = y + guiHeight - btnH - 12;
        int btnStartX = x + guiWidth / 2 - (btnW * 3 + spacing * 2) / 2;

        ShaderElement.getBloomTasks().add(() -> RoundedUtils.drawRound(btnStartX, btnY, btnW, btnH, 8, true, new Color(255, 255, 255, 255)));
        ShaderElement.getBloomTasks().add(() -> RoundedUtils.drawRound(btnStartX + btnW + spacing, btnY, btnW, btnH, 8, true, new Color(255, 255, 255, 255)));
        ShaderElement.getBloomTasks().add(() -> RoundedUtils.drawRound(btnStartX + (btnW + spacing) * 2, btnY, btnW, btnH, 8, true, new Color(255, 255, 255, 255)));

        drawAction(btnStartX, btnY, btnW, btnH, mouseX, mouseY, "封禁", () -> {
            mode = Mode.BAN_REASON;
            reasonText = "";
            timeText = "";
            focusedReason = false;
            focusedTime = false;
        });

        drawAction(btnStartX + btnW + spacing, btnY, btnW, btnH, mouseX, mouseY, "踢出", () -> {
            mode = Mode.KICK_REASON;
            reasonText = "";
            focusedReason = false;
            focusedTime = false;
        });

        drawAction(btnStartX + (btnW + spacing) * 2, btnY, btnW, btnH, mouseX, mouseY, "复制QQ", () -> {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(data.qq()), null);
                HelperUtil.sendMessage("§a已复制QQ：" + data.qq());
            } catch (Exception e) {
                HelperUtil.sendMessage("§c复制失败");
            }
        });
    }

    private void drawReasonInputs(int x, int y, int mouseX, int mouseY) {
        drawTextBox(x + 20, y + 56, 320, 20, reasonText, focusedReason, "封禁/踢出理由...", mouseX, mouseY);
        if (mode == Mode.BAN_REASON) drawTextBox(x + 20, y + 86, 320, 20, timeText, focusedTime, "封禁天数 (数字)", mouseX, mouseY);
    }

    private void drawReasonActions(int x, int y, int guiWidth, int guiHeight, int mouseX, int mouseY) {
        int btnW = 100, btnH = 22, spacing = 20;
        int btnY = y + guiHeight - btnH - 14;
        int confirmX = x + guiWidth / 2 - btnW - spacing / 2;
        int cancelX = x + guiWidth / 2 + spacing / 2;

        ShaderElement.getBloomTasks().add(() -> RoundedUtils.drawRound(confirmX, btnY, btnW, btnH, 8, true, new Color(255, 255, 255, 255)));
        ShaderElement.getBloomTasks().add(() -> RoundedUtils.drawRound(cancelX, btnY, btnW, btnH, 8, true, new Color(255, 255, 255, 255)));

        boolean valid = !reasonText.trim().isEmpty() && (mode != Mode.BAN_REASON || timeText.matches("\\d+"));

        drawAction(confirmX, btnY, btnW, btnH, mouseX, mouseY, "确认", () -> {
            if (!valid) return;
            if (mode == Mode.BAN_REASON) {
                HelperUtil.sendMessage("§c封禁 " + data.name() + "，理由：" + reasonText + "，天数：" + timeText);
            } else {
                HelperUtil.sendMessage("§6踢出 " + data.name() + "，理由：" + reasonText);
                LiveClient.INSTANCE.sendPacket(LiveProto.createKickPlayer(data.name(), reasonText));
            }
            mode = Mode.INFO;
            focusedReason = false;
            focusedTime = false;
        });

        drawAction(cancelX, btnY, btnW, btnH, mouseX, mouseY, "取消", () -> {
            mode = Mode.INFO;
            focusedReason = false;
            focusedTime = false;
        });
    }


    private void drawTextBox(int x, int y, int w, int h, String value, boolean focused, String hint, int mouseX, int mouseY) {
        RoundedUtils.drawRound(x, y, w, h, 6, true, new Color(255, 255, 255, 22));
        RoundedUtils.drawRoundOutline(x, y, w, h, 6, 1.0f, new Color(255, 255, 255, 22), focused ? new Color(255, 255, 255, 70) : new Color(255, 255, 255, 36));
        String shown = value.isEmpty() && !focused ? hint : value;
        int color = value.isEmpty() && !focused ? new Color(180, 180, 180).getRGB() : Color.WHITE.getRGB();
        FontManager.chineseFont16.drawString(shown, x + 6, y + (h - FontManager.chineseFont16.getHeight()) / 2, color);
        if (mouse0 && RenderUtil.isHovering(x, y, w, h, mouseX, mouseY)) {
            if (hint.contains("天数")) {
                focusedTime = true;
                focusedReason = false;
            } else {
                focusedReason = true;
                focusedTime = false;
            }
            mouse0 = false;
        }
    }

    private void drawAction(int x, int y, int w, int h, int mouseX, int mouseY, String label, Runnable onClick) {
        boolean hover = RenderUtil.isHovering(x, y, w, h, mouseX, mouseY);
        RoundedUtils.drawRound(x, y, w, h, 8, true, new Color(0, 0, 0, hover ? 110 : 90));
        RoundedUtils.drawRoundOutline(x, y, w, h, 8, 1.4f, new Color(255, 255, 255, 28), new Color(255, 255, 255, 36));
        FontManager.chineseFont16.drawString(label, x + (w - FontManager.chineseFont16.getStringWidth(label)) / 2f, y + (h - FontManager.chineseFont16.getHeight()) / 2f, Color.WHITE.getRGB());
        if (hover && mouse0) {
            onClick.run();
            mouse0 = false;
        }
    }

    private void drawButton(int x, int y, int w, int h, int r, boolean hover, String label) {
        RoundedUtils.drawRound(x, y, w, h, r, true, new Color(0, 0, 0, hover ? 90 : 70));
        RoundedUtils.drawRoundOutline(x, y, w, h, r, 1.2f, new Color(255, 255, 255, 24), new Color(255, 255, 255, 32));
        FontManager.chineseFont16.drawString(label, x + (w - FontManager.chineseFont16.getStringWidth(label)) / 2f, y + (h - FontManager.chineseFont16.getHeight()) / 2f, Color.WHITE.getRGB());
    }
}
