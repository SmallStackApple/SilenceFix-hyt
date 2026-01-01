package dev.xinxin.gui.ui.modules;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventShader;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.values.BoolValue;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.Collection;
import java.util.List;

public class Scoreboard extends UiModule {
    private final BoolValue leftLayout = new BoolValue("Left Layout", true);
    private final BoolValue redNumbers = new BoolValue("Red Numbers", false);
    private int cachedX, cachedY, cachedWidth, cachedHeight;

    public Scoreboard() {
        super("Scoreboard", 20.0, 40.0, 150.0, 60.0);
    }

    private record Layout(int x, int y, int xEnd, int yEnd, int lineHeight, List<Score> scores, int width, int height, ScoreObjective objective) {}

    private Layout computeLayout(FontRenderer font) {
        ScoreObjective scoreObjective = getScoreObjective();
        if (scoreObjective == null) return null;

        net.minecraft.scoreboard.Scoreboard scoreboard = scoreObjective.getScoreboard();
        Collection<Score> allScores = scoreboard.getSortedScores(scoreObjective);
        List<Score> scores = Lists.newArrayList(Iterables.filter(allScores, s -> s.getPlayerName() != null && !s.getPlayerName().startsWith("#")));

        if (scores.size() > 15)
            scores = Lists.newArrayList(Iterables.skip(scores, scores.size() - 15));

        int maxWidth = font.getStringWidth(scoreObjective.getDisplayName());
        boolean domain = false;

        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()) +
                    (redNumbers.getValue() ? ": " + EnumChatFormatting.AQUA + score.getScorePoints() : "");
            if (!domain) {
                name = "免费heshuyou.xyz";
                domain = true;
            }
            maxWidth = Math.max(maxWidth, font.getStringWidth(name));
        }

        if (!redNumbers.getValue()) {
            maxWidth += 4;
        }

        int lineHeight = font.getHeight();
        int totalHeight = scores.size() * lineHeight;

        int x = (int) getPosX();
        int y = (int) getPosY();

        if (!leftLayout.getValue()) {
            x -= maxWidth;
        }

        int xEnd = x + maxWidth;
        int yEnd = y + totalHeight;
        cachedX = x;
        cachedY = y;
        cachedWidth = maxWidth;
        cachedHeight = totalHeight;

        this.setWidth(leftLayout.getValue() ? maxWidth : -maxWidth);
        this.setHeight(totalHeight);

        return new Layout(x, y, xEnd, yEnd, lineHeight, scores, maxWidth, totalHeight, scoreObjective);
    }

    @EventTarget
    public void  drawHUD(EventRender2D event) {
        FontRenderer font = mc.fontRendererObj;
        Layout layout = computeLayout(font);
        if (layout == null) return;
        LiveUser.Level userLevel = LiveClient.INSTANCE.liveUser.getLevel();
        net.minecraft.scoreboard.Scoreboard scoreboard = layout.objective().getScoreboard();

        int index = -1;
        for (Score score : layout.scores()) {
            ++index;
            String rawName = score.getPlayerName();
            String name;
            boolean skipScore = false;

            ScorePlayerTeam team = scoreboard.getPlayersTeam(rawName);
            name = ScorePlayerTeam.formatPlayerName(team, rawName);
            if (name.contains("会员")) {
                name = "会员：" + userLevel.getDefaultRank(); // 英文权限名
                skipScore = true;
            } else if (name.contains("花雨币")) {
                name = "   权  限：  " + userLevel.getDefaultRank(); // 中文权限描述
                skipScore = true;
            } else if (name.contains("频道说话") || name.contains("举  报")) {
                name = "IRC发言 " + ".i 想说的话";
                skipScore = true;
            }

            int y = layout.yEnd() - index * layout.lineHeight();
            drawRect(layout.x(), y, layout.xEnd(), y + layout.lineHeight(), 0x50000000);

            if (index == 0) {
                font.drawString(EnumChatFormatting.AQUA + "免费heshuyou.xyz", layout.x() + 2, y, 0x20FFFFFF);
            } else {
                font.drawString(name, layout.x() + 2, y, 0x20FFFFFF);
            }

            if (!skipScore && redNumbers.getValue()) {
                String point = EnumChatFormatting.RED + String.valueOf(score.getScorePoints());
                font.drawString(point, layout.xEnd() - font.getStringWidth(point), y, 0x20FFFFFF);
            }

            if (index == layout.scores().size() - 1) {
                String title = layout.objective().getDisplayName()
                        .replace("花雨庭", "欣欣客户端")
                        .replace("§c✿", "§c⌨");
                drawRect(layout.x(), y - layout.lineHeight() - 1, layout.xEnd(), y - 1, 0x60000000);
                drawRect(layout.x(), y - 1, layout.xEnd(), y, 0x50000000);
                font.drawString(title, layout.x() + layout.width() / 2 - font.getStringWidth(title) / 2, y - layout.lineHeight(), 0x20FFFFFF);
            }
        }



    }





    @EventTarget
    public void onShader(EventShader e) {
        if (!e.getSource().equals("PostProcessing")) return;

        drawRect(cachedX, cachedY, cachedX + cachedWidth, cachedY + cachedHeight,
                new Color(20, 20, 20, 245).getRGB());
    }

    private static ScoreObjective getScoreObjective() {
        net.minecraft.scoreboard.Scoreboard board = mc.theWorld.getScoreboard();
        ScorePlayerTeam team = board.getPlayersTeam(mc.thePlayer.getName());
        if (team != null) {
            int colorIndex = team.getChatFormat().getColorIndex();
            if (colorIndex >= 0) {
                ScoreObjective obj = board.getObjectiveInDisplaySlot(3 + colorIndex);
                if (obj != null) return obj;
            }
        }
        return board.getObjectiveInDisplaySlot(1);
    }
}
