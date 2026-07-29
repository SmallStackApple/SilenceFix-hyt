package dev.xinxin.utils.render.fontRender;

import dev.xinxin.SilenceFix;

import java.awt.*;
import java.io.InputStream;

public class FontManager {
    private static final String locate = "express/font/";
    public static RapeMasterFontManager arial10;
    public static RapeMasterFontManager arial12;
    public static RapeMasterFontManager arial14;
    public static RapeMasterFontManager titleFontBig;

    public static RapeMasterFontManager arial16;
    public static RapeMasterFontManager arial18;
    public static RapeMasterFontManager arial20;
    public static RapeMasterFontManager arial22;
    public static RapeMasterFontManager arial24;
    public static RapeMasterFontManager arial26;
    public static RapeMasterFontManager thin40;
    public static RapeMasterFontManager thin16;
    public static RapeMasterFontManager arial64;
    public static RapeMasterFontManager arial28;
    public static RapeMasterFontManager arial32;
    public static RapeMasterFontManager arial40;
    public static RapeMasterFontManager arial42;
    public static RapeMasterFontManager splash40;
    public static RapeMasterFontManager splash18;
    public static RapeMasterFontManager icon22;
    public static RapeMasterFontManager icon26;
    public static RapeMasterFontManager Tahoma12;
    public static RapeMasterFontManager Tahoma14;
    public static RapeMasterFontManager Tahoma16;
    public static RapeMasterFontManager Tahoma18;
    public static RapeMasterFontManager Tahoma20;
    public static RapeMasterFontManager Tahoma22;
    public static RapeMasterFontManager Tahoma24;
    public static RapeMasterFontManager Tahoma26;
    public static RapeMasterFontManager Tahoma28;
    public static RapeMasterFontManager Tahoma32;
    public static RapeMasterFontManager Tahoma40;
    public static RapeMasterFontManager Tahoma42;
    public static RapeMasterFontManager icontestFont35;
    public static RapeMasterFontManager icontestFont28;
    public static RapeMasterFontManager icontestFont90;
    public static RapeMasterFontManager icontestFont80;
    public static RapeMasterFontManager icontestFont20;
    public static RapeMasterFontManager icontestFont40;
    public static RapeMasterFontManager lettuceFont20;
    public static RapeMasterFontManager lettuceFont24;
    public static RapeMasterFontManager lettuceBoldFont26;
    public static RapeMasterFontManager infoFontBold;
    public static RapeMasterFontManager titleFontBold;

    public static RapeMasterFontManager bold22;
    public static RapeMasterFontManager bold24;
    public static RapeMasterFontManager bold38;

    public static RapeMasterFontManager infoFont;
    public static RapeMasterFontManager titleFont;

    public static RapeMasterFontManager chineseFont38;
    public static RapeMasterFontManager chineseFont20;
    public static RapeMasterFontManager chineseFont14;
    public static RapeMasterFontManager chineseFont18;
    public static RapeMasterFontManager chineseFont16;
    public static RapeMasterFontManager chineseFont24;
    public static RapeMasterFontManager chineseFont26;
    public static RapeMasterFontManager chineseFont22;

    public static RapeMasterFontManager harmony;


    public static RapeMasterFontManager harmonybold14;
    public static RapeMasterFontManager harmonybold16;
    public static RapeMasterFontManager harmonybold18;
    public static RapeMasterFontManager harmonybold20;
    public static RapeMasterFontManager harmonybold22;
    public static RapeMasterFontManager harmonybold24;
    public static RapeMasterFontManager harmonybold26;
    public static RapeMasterFontManager harmonybold28;
    public static RapeMasterFontManager harmonybold30;
    public static RapeMasterFontManager harmonybold38;

    public static RapeMasterFontManager navenRegular14;
    public static RapeMasterFontManager navenRegular16;
    public static RapeMasterFontManager navenRegular18;
    public static RapeMasterFontManager navenRegular20;
    public static RapeMasterFontManager navenRegular22;
    public static RapeMasterFontManager navenRegular24;
    public static RapeMasterFontManager navenRegular26;
    public static RapeMasterFontManager navenRegular30;
    public static RapeMasterFontManager navenRegular38;

    public static RapeMasterFontManager navenbold14;
    public static RapeMasterFontManager navenbold16;
    public static RapeMasterFontManager navenbold18;
    public static RapeMasterFontManager navenbold20;
    public static RapeMasterFontManager navenbold22;
    public static RapeMasterFontManager navenbold24;
    public static RapeMasterFontManager navenbold26;
    public static RapeMasterFontManager navenbold30;
    public static RapeMasterFontManager navenbold38;


    private RapeMasterFontManager getAdaptiveFont(float guiScale) {
        if (guiScale <= 1.2f) return FontManager.navenbold14;
        else if (guiScale <= 1.5f) return FontManager.navenbold16;
        else if (guiScale <= 2.0f) return FontManager.navenbold18;
        else if (guiScale <= 2.5f) return FontManager.navenbold20;
        else if (guiScale <= 3.0f) return FontManager.navenbold22;
        else return FontManager.navenbold24;
    }



    public static void init() {
        chineseFont20 = new RapeMasterFontManager(getFont("msyh.ttf", 20.0f)); // 微软雅黑
        chineseFont24 = new RapeMasterFontManager(getFont("msyh.ttf", 24.0f)); // 微软雅黑
        chineseFont26 = new RapeMasterFontManager(getFont("msyh.ttf", 26.0f)); // 微软雅黑
        chineseFont22 = new RapeMasterFontManager(getFont("msyh.ttf", 22.0f)); // 微软雅黑
        chineseFont38 = new RapeMasterFontManager(getFont("msyh.ttf", 38.0f)); // 微软雅黑
        chineseFont18 = new RapeMasterFontManager(getFont("msyh.ttf", 18.0f));
        chineseFont16 = new RapeMasterFontManager(getFont("msyh.ttf", 16.0f));
        chineseFont14 = new RapeMasterFontManager(getFont("msyh.ttf", 14.0f));

        navenRegular14 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 14));
        navenRegular16 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 16));
        navenRegular18 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 18));
        navenRegular20 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 20));
        navenRegular22 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 22));
        navenRegular24 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 24));
        navenRegular26 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 26));
        navenRegular30 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 30));
        navenRegular38 = new RapeMasterFontManager(getFont("SourceHanSansSC-Regular.ttf", 38));

        navenbold14 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 14));
        navenbold16 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 16));
        navenbold18 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 18));
        navenbold20 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 20));
        navenbold22 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 22));
        navenbold24 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 24));
        navenbold26 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 26));
        navenbold30 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 30));
        navenbold38 = new RapeMasterFontManager(getFont("SourceHanSansSC-Bold.ttf", 38));

//        harmony = new RapeMasterFontManager(getFont("harmony.ttf", 14));

        harmonybold14 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 14));
        harmonybold16 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 16));
        harmonybold18 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 18));
        harmonybold20 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 20));
        harmonybold22 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 22));
        harmonybold24 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 24));
        harmonybold26 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 26));
        harmonybold28 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 28));
        harmonybold30 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 30));
        harmonybold38 = new RapeMasterFontManager(getFont("HarmonyOS_Sans_SC_Bold.ttf", 38));

        thin40 = new RapeMasterFontManager(FontManager.getFont("sfthin.ttf", 40.0f));
        thin16 = new RapeMasterFontManager(FontManager.getFont("sfthin.ttf", 16.0f));

        arial10 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 10.0f));
        arial12 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 12.0f));
        arial14 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 14.0f));
        arial16 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 16.0f));
        arial18 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 18.0f));
        arial20 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 20.0f));
        arial22 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 22.0f));
        arial24 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 24.0f));
        arial26 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 26.0f));
        arial28 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 28.0f));
        arial32 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 32.0f));
        arial40 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 40.0f));
        arial42 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 42.0f));
        splash40 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 40.0f));
        titleFontBig = new RapeMasterFontManager(getFont("tahoma.ttf", 20.0F));
        splash18 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 18.0f));
        arial64 = new RapeMasterFontManager(FontManager.getFont("font.ttf", 64.0f));
        Tahoma12 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 12.0f));  // 修复：原代码使用font.ttf
        Tahoma14 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 14.0f));  // 修复：原代码使用font.ttf
        Tahoma16 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 16.0f));  // 修复：原代码使用font.ttf
        Tahoma18 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 18.0f));  // 修复：原代码使用font.ttf
        Tahoma20 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 20.0f));  // 修复：原代码使用font.ttf
        Tahoma22 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 22.0f));  // 修复：原代码使用font.ttf
        Tahoma24 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 24.0f));  // 修复：原代码使用font.ttf
        Tahoma26 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 26.0f));  // 修复：原代码使用font.ttf
        Tahoma28 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 28.0f));  // 修复：原代码使用font.ttf
        Tahoma32 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 32.0f));  // 修复：原代码使用font.ttf
        Tahoma40 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 40.0f));  // 修复：原代码使用font.ttf
        Tahoma42 = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 42.0f));  // 修复：原代码使用font.ttf
        bold22 = new RapeMasterFontManager(FontManager.getFont("bold.ttf", 22.0f));  // 修复：原大小为18.0f
        bold24 = new RapeMasterFontManager(FontManager.getFont("bold.ttf", 24.0f));
        bold38 = new RapeMasterFontManager(FontManager.getFont("bold.ttf", 38.0f));  // 修复：添加f后缀
        titleFontBold = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 18.0f));
        infoFontBold = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 15.0f));
        titleFont = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 19.0f));
        infoFont = new RapeMasterFontManager(FontManager.getFont("tahoma.ttf", 12.0f));
        icontestFont90 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 90.0f));
        icontestFont80 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 80.0f));
        icontestFont35 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 35.0f));
        icontestFont28 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 28.0f));
        icon22 = new RapeMasterFontManager(FontManager.getFont("iconfont.ttf", 22.0f));
        icon26 = new RapeMasterFontManager(FontManager.getFont("iconfont.ttf", 26.0f));
        icontestFont20 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 20.0f));
        icontestFont40 = new RapeMasterFontManager(FontManager.getFont("icont.ttf", 40.0f));
        lettuceFont20 = new RapeMasterFontManager(FontManager.getFont("geologica.ttf", 20.0f));
        lettuceFont24 = new RapeMasterFontManager(FontManager.getFont("geologica.ttf", 24.0f));
        lettuceBoldFont26 = new RapeMasterFontManager(FontManager.getFont("geologica-bold.ttf", 26.0f));
    }

    static Font getFont(String fontName, float fontSize) {
        Font font = null;
        try (InputStream inputStream = SilenceFix.class.getResourceAsStream("/assets/minecraft/express/font/" + fontName)) {
            if (inputStream != null) {
                font = Font.createFont(Font.TRUETYPE_FONT, inputStream);  // 修复：使用Font.TRUETYPE_FONT替代0
                font = font.deriveFont(fontSize);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (font == null) {
            System.out.println("[FontManager] Failed to load font: " + fontName + ", fallback to Arial");
            font = new Font("Arial", Font.PLAIN, Math.min((int) fontSize, 48));
        }
        return font;
    }
}