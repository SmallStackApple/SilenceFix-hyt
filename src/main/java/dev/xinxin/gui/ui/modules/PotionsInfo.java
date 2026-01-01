package dev.xinxin.gui.ui.modules;

import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.gui.ui.UiModule;
import dev.xinxin.module.modules.render.HUD;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.RoundedUtils;
import dev.xinxin.utils.render.fontRender.FontManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static dev.xinxin.module.modules.render.HUD.mainColor;

public class PotionsInfo extends UiModule {
    private List<PotionEffect> effects = new ArrayList<>();

    private static final Map<String, String> CN = new HashMap<String, String>() {{
        put("potion.moveSpeed", "速度");
        put("potion.moveSlowdown", "缓慢");
        put("potion.digSpeed", "急迫");
        put("potion.digSlowdown", "挖掘疲劳");
        put("potion.damageBoost", "力量");
        put("potion.heal", "瞬间治疗");
        put("potion.harm", "瞬间伤害");
        put("potion.jump", "跳跃提升");
        put("potion.confusion", "反胃");
        put("potion.regeneration", "生命恢复");
        put("potion.resistance", "抗性提升");
        put("potion.fireResistance", "抗火");
        put("potion.waterBreathing", "水下呼吸");
        put("potion.invisibility", "隐身");
        put("potion.blindness", "失明");
        put("potion.nightVision", "夜视");
        put("potion.hunger", "饥饿");
        put("potion.weakness", "虚弱");
        put("potion.poison", "中毒");
        put("potion.wither", "凋零");
        put("potion.healthBoost", "生命提升");
        put("potion.absorption", "伤害吸收");
        put("potion.saturation", "饱和");
        put("potion.glowing", "发光");
        put("potion.levitation", "漂浮");
        put("potion.luck", "幸运");
        put("potion.unluck", "霉运");
    }};

    private static final Set<String> NEG = new HashSet<>(Arrays.asList(
            "potion.moveSlowdown","potion.digSlowdown","potion.confusion","potion.blindness",
            "potion.hunger","potion.weakness","potion.poison","potion.wither","potion.harm","potion.unluck"
    ));

    public PotionsInfo() {
        super("PotionsInfo", 20.0, 40.0, 200.0, 20.0);
    }

    private final Map<Integer, Integer> durationMax = new HashMap<>();


    @EventTarget
    public void onRender2D(EventRender2D e) {
        effects = mc.thePlayer.getActivePotionEffects().stream()
                .sorted(Comparator.comparingInt(p -> -p.getDuration()))
                .collect(Collectors.toList());
        if (effects.isEmpty()) return;

        for (PotionEffect eff : effects) {
            int id = eff.getPotionID();
            int cur = eff.getDuration();
            Integer known = durationMax.get(id);
            if (known == null || cur > known) durationMax.put(id, cur);
        }

        double x = this.getPosX();
        double y = this.getPosY();

        int h = 24;
        int gap = 8;
        int iconW = 28;
        int between = 6;
        int padText = 12;
        int minTextCapsule = 100;

        int wMax = 0;
        for (PotionEffect eff : effects) {
            String t = buildText(eff);
            int textW = FontManager.chineseFont20.getStringWidth(t);
            int tw = Math.max(minTextCapsule, padText * 2 + textW);
            int w = iconW + between + tw;
            wMax = Math.max(wMax, w);
        }

        for (int i = 0; i < effects.size(); i++) {
            int y0 = (int) y + i * (h + gap);
            drawRow(effects.get(i), (int) x, y0, h, iconW, between, padText, minTextCapsule, wMax);
        }

        this.setWidth(wMax);
        this.setHeight(effects.size() * h + Math.max(0, effects.size() - 1) * gap);
    }



    private void drawRow(PotionEffect eff, int x, int y, int h, int iconW, int between, int padText, int minTextCapsule, int rowW) {
        Potion p = Potion.potionTypes[eff.getPotionID()];
        String key = p.getName();
        String text = buildText(eff);

        Color fill = new Color(0, 0, 0, 85);
        float r = 6f;

        int iconX = x, iconY = y, iconWv = iconW, iconH = h;
        ShaderElement.addBlurTask(() -> RenderUtil.drawRoundedRect(iconX, iconY, iconWv, iconH, 6, new Color(0,0,0,200)));
        ShaderElement.addBloomTask(() -> RenderUtil.drawRoundedRect(iconX, iconY, iconWv, iconH, 6, new Color(0,0,0,200)));
        RoundedUtils.drawRound(iconX, iconY, iconWv, iconH, r, fill);

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1,1,1,1);
        if (p.hasStatusIcon()) {
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
            int idx = p.getStatusIconIndex();
            int ix = iconX + (iconWv - 18) / 2;
            int iy = iconY + (iconH - 18) / 2;
            mc.ingameGUI.drawTexturedModalRect(ix, iy, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
        }
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();

        int textW = FontManager.chineseFont20.getStringWidth(text);
        int gap = Math.max(2, between - 2);
        int usable = Math.max(30, rowW - iconW - gap - 12);
        int maxCapsule = Math.max(minTextCapsule, (int)(usable * 0.72f));
        int tw = Math.min(maxCapsule, Math.max(minTextCapsule, padText * 2 + textW));
        int tx = x + iconW + gap;
        int ty = y;

        ShaderElement.addBlurTask(() -> RenderUtil.drawRoundedRect(tx, ty, tw, h, 6, new Color(0,0,0,200)));
        ShaderElement.addBloomTask(() -> RenderUtil.drawRoundedRect(tx, ty, tw, h, 6, new Color(0,0,0,200)));
        RoundedUtils.drawRound(tx, ty, tw, h, r, fill);

        Integer maxDur = durationMax.get(eff.getPotionID());
        int curDur = Math.max(0, eff.getDuration());
        float prog = maxDur != null && maxDur > 0 ? Math.min(1f, curDur / (float) maxDur) : 1f;

        int inset = 2;
        int innerX = tx + inset;
        int innerY = ty + inset;
        int innerW = tw - inset * 2;
        int innerH = h - inset * 2;

        Color cL = HUD.color(1);
        Color cR = HUD.color(6);
        float[] hsbL = Color.RGBtoHSB(cL.getRed(), cL.getGreen(), cL.getBlue(), null);
        float[] hsbR = Color.RGBtoHSB(cR.getRed(), cR.getGreen(), cR.getBlue(), null);
        cL = new Color(Color.HSBtoRGB(hsbL[0], Math.max(0f, hsbL[1] * 0.6f), Math.min(1f, hsbL[2] * 1.15f)));
        cR = new Color(Color.HSBtoRGB(hsbR[0], Math.max(0f, hsbR[1] * 0.6f), Math.min(1f, hsbR[2] * 1.15f)));
        cL = new Color(cL.getRed(), cL.getGreen(), cL.getBlue(), 180);
        cR = new Color(cR.getRed(), cR.getGreen(), cR.getBlue(), 180);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

        int fillW = Math.max(0, Math.round(innerW * prog));
        int minCap = Math.round(r * 2f);
        if (fillW > 0) {
            int drawW = Math.min(innerW, Math.max(minCap, fillW));
            RoundedUtils.drawGradientRoundLR(innerX, innerY, drawW, innerH, r - 1f, cL, cR);
        }

        GlStateManager.disableBlend();

        int textX = tx + (tw - textW) / 2;
        int textY = y + (h - FontManager.chineseFont20.getHeight()) / 2 + 1;
        FontManager.chineseFont20.drawStringWithShadow(text, textX, textY, pickTextColor(key));
    }














    private String buildText(PotionEffect eff) {
        Potion p = Potion.potionTypes[eff.getPotionID()];
        String key = p.getName();
        String name = CN.getOrDefault(key, I18n.format(key));
        return name + " " + toRoman(eff.getAmplifier() + 1);
    }

    private int pickTextColor(String key) {
        if ("potion.resistance".equals(key))     return new Color(166, 116, 255).getRGB(); // 紫
        if ("potion.regeneration".equals(key))   return new Color(255, 130, 210).getRGB(); // 粉
        if ("potion.absorption".equals(key))     return new Color(74, 131, 243).getRGB();  // 蓝
        if ("potion.fireResistance".equals(key)) return new Color(255, 170, 60).getRGB();  // 橙
        if ("potion.moveSpeed".equals(key))      return new Color(170, 255, 170).getRGB();
        if ("potion.nightVision".equals(key))    return new Color(170, 255, 170).getRGB();
        if ("potion.waterBreathing".equals(key)) return new Color(170, 255, 170).getRGB();
        if ("potion.healthBoost".equals(key))    return new Color(170, 255, 170).getRGB();
        if ("potion.invisibility".equals(key))   return new Color(170, 255, 170).getRGB();
        if ("potion.saturation".equals(key))     return new Color(170, 255, 170).getRGB();
        if ("potion.luck".equals(key))           return new Color(170, 255, 170).getRGB();
        if ("potion.heal".equals(key))           return new Color(170, 255, 170).getRGB();
        if ("potion.harm".equals(key))           return new Color(255, 110, 110).getRGB();
        if ("potion.moveSlowdown".equals(key))   return new Color(255, 110, 110).getRGB();
        if ("potion.digSlowdown".equals(key))    return new Color(255, 110, 110).getRGB();
        if ("potion.confusion".equals(key))      return new Color(255, 110, 110).getRGB();
        if ("potion.blindness".equals(key))      return new Color(255, 110, 110).getRGB();
        if ("potion.hunger".equals(key))         return new Color(255, 110, 110).getRGB();
        if ("potion.weakness".equals(key))       return new Color(255, 110, 110).getRGB();
        if ("potion.poison".equals(key))         return new Color(255, 110, 110).getRGB();
        if ("potion.wither".equals(key))         return new Color(255, 110, 110).getRGB();
        if ("potion.unluck".equals(key))         return new Color(255, 110, 110).getRGB();
        return new Color(180, 230, 180).getRGB();
    }


    private String toRoman(int n) {
        if (n < 1 || n > 3999) return String.valueOf(n);
        String[] M = {"", "M", "MM", "MMM"};
        String[] C = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] X = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] I = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return M[n / 1000] + C[(n % 1000) / 100] + X[(n % 100) / 10] + I[n % 10];
    }
}
