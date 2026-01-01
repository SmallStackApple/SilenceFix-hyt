package dev.xinxin.gui.clickgui.irc;

import dev.xinxin.utils.render.AnimationUtil;
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
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminPanel extends GuiScreen {

    public final List<IRCData> ircDataList = new CopyOnWriteArrayList<>();
    private boolean list = false;
    private boolean mouse0 = false;
    private float scrollOffset = 0;
    private float smoothScroll = 0;
    public float userWidth = 0;
    private String searchText = "";
    private boolean focusedSearch = false;
    private final List<IRCData> filteredList = new ArrayList<>();
    private boolean blockClick = false;
    private final GuiScreen parent;

    public AdminPanel() { this.parent = null; }
    public AdminPanel(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        userWidth = 0;
        scrollOffset = 0;
        smoothScroll = 0;
        mouse0 = false;
        list = false;
        searchText = "";
        focusedSearch = false;
        filteredList.clear();
        ircDataList.clear();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int guiWidth = 480;
        int guiHeight = 300;
        int x = width / 2 - guiWidth / 2;
        int y = height / 2 - guiHeight / 2;

        drawBackground(guiWidth, guiHeight, x, y);
        drawTitle(guiWidth, x, y);

        // 控件区域
        int backW = 64, backH = 20, backR = 8;
        int backX = x + 10, backY = y + 6;
        boolean backHoverPre = RenderUtil.isHovering(backX, backY, backW, backH, mouseX, mouseY);

        int actionW = 120, actionH = 24, actionR = 8;
        int actionX = x + guiWidth - actionW - 10;
        int actionY = y + 6;
        boolean refreshHoverPre = RenderUtil.isHovering(actionX, actionY, actionW, actionH, mouseX, mouseY);

        blockClick = backHoverPre || refreshHoverPre;

        drawUserPanel(guiWidth, guiHeight, x, y, actionX, actionY, actionW, actionH, actionR);
        drawBackButton("关闭界面", backX, backY, backW, backH, backR, backHoverPre);

        if (!list) {
            drawEmptyList(guiWidth, guiHeight, x, y);
        } else if (!ircDataList.isEmpty()) {
            drawSearchAndUserList(guiWidth, guiHeight, x, y, mouseX, mouseY);
        } else {
            drawLoadingState(guiWidth, guiHeight, x, y);
        }

        drawUpdateButton("刷新列表", actionX, actionY, actionW, actionH, actionR, refreshHoverPre);
    }
    private void drawBackground(int guiWidth, int guiHeight, int x, int y) {
        RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(20, 20, 20, 80));
        RoundedUtils.drawRoundOutline(x, y, guiWidth, guiHeight, 10, 1.0f,
                new Color(0, 0, 0, 50), new Color(255, 255, 255, 18));
        RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(255, 255, 255, 8));
        ShaderElement.getTasks().add(() -> RoundedUtils.drawRound(x, y, guiWidth, guiHeight, 10, true, new Color(255, 255, 255, 255)));
        PostProcessing.INSTANCE.blurScreen();
    }


    private void drawTitle(int guiWidth, int x, int y) {
        String title = "SilenceIRC-管理面板";
        int tw = FontManager.chineseFont18.getStringWidth(title);
        FontManager.chineseFont18.drawString(title,
                x + guiWidth / 2f - tw / 2f,
                y + 8,
                new Color(255, 255, 255, 230).getRGB());
    }

    private void drawUserPanel(int guiWidth, int guiHeight, int x, int y,
                               int actionX, int actionY, int actionW, int actionH, int actionR) {
        float targetWidth = list ? guiWidth - 170 : guiWidth - 200;
        userWidth = AnimationUtil.animateSmooth(userWidth, targetWidth, 0.25f);

        RoundedUtils.drawRound(x + 10, y + 28, userWidth, guiHeight - 38, 8, true, new Color(16, 16, 16, 90));
        RoundedUtils.drawRoundOutline(x + 10, y + 28, userWidth, guiHeight - 38, 8, 1.0f,
                new Color(0, 0, 0, 50), new Color(255, 255, 255, 18));
        RoundedUtils.drawRound(x + 10, y + 28, userWidth, guiHeight - 38, 8, true, new Color(255, 255, 255, 6));

        ShaderElement.getTasks().add(() ->
                RoundedUtils.drawRound(x + 10, y + 28, userWidth, guiHeight - 38, 8, true, new Color(255, 255, 255, 255))
        );
        ShaderElement.getBloomTasks().add(() ->
                RoundedUtils.drawRound(actionX, actionY, actionW, actionH, actionR, true, new Color(255, 255, 255, 255))
        );
        PostProcessing.INSTANCE.blurScreen();
    }

    private void drawEmptyList(int guiWidth, int guiHeight, int x, int y) {
        FontManager.chineseFont18.drawString("列表为空",
                x + 10 + ((guiWidth - 200) / 2f - FontManager.chineseFont18.getStringWidth("列表为空") / 2f),
                y + guiHeight / 2f - FontManager.chineseFont18.getHeight() / 2f,
                Color.WHITE.getRGB());
    }

    private void drawSearchAndUserList(int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY) {
        int searchBoxX = x + 12;
        int searchBoxY = y + 30;
        int searchBoxW = (int) userWidth - 4;
        int searchBoxH = 20;

        drawSearchBox(searchBoxX, searchBoxY, searchBoxW, searchBoxH, mouseX, mouseY);
        filterUserList();

        int visibleH = guiHeight - 38 - 24;
        int maxScroll = Math.max(0, filteredList.size() * 52 - visibleH);
        float target = Math.max(0, Math.min(scrollOffset, maxScroll));
        smoothScroll = AnimationUtil.animateSmooth(smoothScroll, target, 0.3f);

        RenderUtil.enableRoundNoRender(x + 10, y + 28 + 24, userWidth, guiHeight - 38 - 24, 8);
        if (!filteredList.isEmpty()) {
            int dataY = (int) (y + 54 - smoothScroll);
            for (IRCData data : filteredList) {
                drawUserCard(data, x + 15, dataY, (int) userWidth - 10, 48, mouseX, mouseY);
                dataY += 52;
            }
        }
        RenderUtil.disableRoundNoRender();
    }

    private void drawSearchBox(int x, int y, int w, int h, int mouseX, int mouseY) {
        RoundedUtils.drawRound(x, y, w, h, 6, true, new Color(255, 255, 255, 22));
        RoundedUtils.drawRoundOutline(x, y, w, h, 6, 1.0f,
                new Color(255, 255, 255, 22), focusedSearch ? new Color(255, 255, 255, 70) : new Color(255, 255, 255, 36));

        String shown = searchText.isEmpty() && !focusedSearch ? "输入用户名或游戏名搜索..." : searchText;
        int textColor = searchText.isEmpty() && !focusedSearch ? new Color(180, 180, 180).getRGB() : Color.WHITE.getRGB();
        FontManager.chineseFont16.drawString(shown, x + 5, y + 5, textColor);

        if (!searchText.isEmpty()) {
            int clearSize = 10;
            int clearX = x + w - clearSize - 6;
            int clearY = y + (h - clearSize) / 2;
            RenderUtil.drawLine2D(clearX, clearY, clearX + clearSize, clearY + clearSize, 1f, Color.WHITE);
            RenderUtil.drawLine2D(clearX + clearSize, clearY, clearX, clearY + clearSize, 1f, Color.WHITE);
            if (RenderUtil.isHovering(clearX - 3, clearY - 3, clearSize + 6, clearSize + 6, mouseX, mouseY) && Mouse.isButtonDown(0)) {
                searchText = "";
                scrollOffset = 0;
                smoothScroll = 0;
            }
        }
    }

    private void filterUserList() {
        filteredList.clear();
        String self = mc.getSession().getUsername();
        for (IRCData data : ircDataList) {
            if (data == null || data.name().equals("未找到") || data.name().equalsIgnoreCase(self)) continue;
            String name = data.name().toLowerCase();
            String game = data.gameName().toLowerCase();
            if (searchText.isEmpty() || name.contains(searchText.toLowerCase()) || game.contains(searchText.toLowerCase())) {
                filteredList.add(data);
            }
        }
    }

    private void drawLoadingState(int guiWidth, int guiHeight, int x, int y) {
        int searchBoxX = x + 12;
        int searchBoxY = y + 30;
        int searchBoxW = (int) userWidth - 4;
        int searchBoxH = 20;

        RoundedUtils.drawRound(searchBoxX, searchBoxY, searchBoxW, searchBoxH, 6, true, new Color(255, 255, 255, 22));
        RoundedUtils.drawRoundOutline(searchBoxX, searchBoxY, searchBoxW, searchBoxH, 6, 1.0f,
                new Color(255, 255, 255, 22), focusedSearch ? new Color(255, 255, 255, 70) : new Color(255, 255, 255, 36));

        String loading = "正在加载…";
        RenderUtil.enableRoundNoRender(x + 10, y + 28 + 24, userWidth, guiHeight - 38 - 24, 8);
        FontManager.chineseFont18.drawString(
                loading,
                (int) (x + 10 + userWidth / 2f - FontManager.chineseFont18.getStringWidth(loading) / 2f),
                y + 28 + 24 + (guiHeight - 38 - 24) / 2 - FontManager.chineseFont18.getHeight() / 2,
                new Color(230, 230, 230, 200).getRGB()
        );
        RenderUtil.disableRoundNoRender();
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        mouse0 = button == 0;
        int guiWidth = 480;
        int guiHeight = 300;
        int bx = this.width / 2 - guiWidth / 2;
        int by = this.height / 2 - guiHeight / 2;
        int searchBoxX = bx + 12;
        int searchBoxY = by + 30;
        int searchBoxW = (int) userWidth - 4;
        int searchBoxH = 20;
        focusedSearch = list && RenderUtil.isHovering(searchBoxX, searchBoxY, searchBoxW, searchBoxH, mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (focusedSearch) {
            String prevText = searchText;
            boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (keyCode == Keyboard.KEY_BACK && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                focusedSearch = false;
            } else if (ctrlDown && keyCode == Keyboard.KEY_V) {
                try {
                    String clip = getClipboardString();
                    if (clip != null && !clip.isEmpty()) {
                        for (char c : clip.toCharArray()) {
                            if (ChatAllowedCharacters.isAllowedCharacter(c)) searchText += c;
                        }
                    }
                } catch (Exception ignored) {}
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                searchText += typedChar;
            }
            if (!searchText.equals(prevText)) {
                scrollOffset = 0;
                smoothScroll = 0;
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
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && list) {
            scrollOffset -= wheel / 6.0f;
            int visibleH = 300 - 38 - 24;
            int maxScroll = Math.max(0, (filteredList != null ? filteredList.size() : 0) * 52 - visibleH);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    private void drawBackButton(String label, int x, int y, int w, int h, int r, boolean preHover) {
        boolean hover = preHover;
        RoundedUtils.drawRound(x, y, w, h, r, true, new Color(0, 0, 0, hover ? 90 : 70));
        RoundedUtils.drawRoundOutline(x, y, w, h, r, 1.2f, new Color(255, 255, 255, 24), new Color(255, 255, 255, 32));
        FontManager.chineseFont16.drawString(label,
                x + w / 2f - FontManager.chineseFont16.getStringWidth(label) / 2f,
                y + h / 2f - FontManager.chineseFont16.getHeight() / 2f,
                Color.WHITE.getRGB());
        if (hover && mouse0) {
            mc.displayGuiScreen(parent != null ? parent : null);
            mouse0 = false;
        }
    }

    private void drawUpdateButton(String label, int x, int y, int w, int h, int r, boolean preHover) {
        boolean hover = preHover;
        RoundedUtils.drawRound(x, y, w, h, r, true, new Color(0, 0, 0, hover ? 110 : 90));
        RoundedUtils.drawRoundOutline(x, y, w, h, r, 1.4f, new Color(255, 255, 255, 28), new Color(255, 255, 255, 36));
        FontManager.chineseFont16.drawString(label,
                x + w / 2f - FontManager.chineseFont16.getStringWidth(label) / 2f,
                y + h / 2f - FontManager.chineseFont16.getHeight() / 2f,
                Color.WHITE.getRGB());
        if (hover && mouse0) {
            final UUID executionId = UUID.randomUUID();

            LiveClient.INSTANCE.getLiveComponent().getCommandOutMap().put(executionId, (out) -> {
                String qq = match(out, "QQ='(.*?)'");
                String username = match(out, "username='(.*?)'");
                String level = match(out, "level=(\\w+)");
                String rank = match(out, "rank='(.*?)'");
                String mcName = match(out, "lastMinecraftName=([^,\\s]+)");

                IRCData data = new IRCData(username, mcName, level, rank, qq);

                if (!ircDataList.contains(data)) {
                    ircDataList.add(data);
                }
            });
            LiveClient.INSTANCE.sendPacket(LiveProto.createExecuteCommand(executionId, "get sessions"));
            list = true;
            mouse0 = false;
        }
    }

    private void drawUserCard(IRCData data, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hover = RenderUtil.isHovering(x, y, w, h, mouseX, mouseY);
        int fH = FontManager.chineseFont18.getHeight();
        int padding = 6;
        Color bg = hover ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 12);
        RoundedUtils.drawRound(x, y, w, h, 8, true, bg);
        RoundedUtils.drawRoundOutline(x, y, w, h, 8, 1.0f, new Color(255, 255, 255, 14), new Color(255, 255, 255, 26));
        String nameText = "用户名：" + data.name();
        String qqText = "当前QQ：" + data.qq();
        FontManager.chineseFont18.drawString(nameText, x + padding, y + padding, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString(qqText, x + w / 2 + padding, y + padding, Color.WHITE.getRGB());
        String gameText = "游戏名：" + data.gameName();
        String levelText = "IRC等级：" + data.level();
        FontManager.chineseFont18.drawString(gameText, x + padding, y + padding + fH + 2, Color.WHITE.getRGB());
        FontManager.chineseFont18.drawString(levelText, x + w / 2 + padding, y + padding + fH + 2, Color.WHITE.getRGB());
        String rankText = "IRC头衔：" + data.rank();
        FontManager.chineseFont18.drawString(rankText, x + padding, y + h - fH, Color.WHITE.getRGB());
        if (!blockClick && hover && mouse0) {
            mouse0 = false;
            mc.displayGuiScreen(new User(this, data));
        }
    }

    public String match(String input, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : "§c未找到";
    }
}
