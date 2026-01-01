package dev.xinxin.utils.render;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class BackgroundTextureManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String Default = "express/bg.png";
    private static volatile ResourceLocation backgroundTexture = null;
    private static volatile ResourceLocation prevTexture = null;
    private static volatile long fadeStartMs = 0L;
    private static final long fadeDurationMs = 350L;

    private static final ExecutorService IO_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BG-Loader");
        t.setDaemon(true);
        return t;
    });

    private static final ExecutorService RACE_EXEC = Executors.newFixedThreadPool(
            Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)),
            r -> { Thread t = new Thread(r, "BG-Fetch"); t.setDaemon(true); return t; }
    );

    private static final List<String> APIS = Arrays.asList(
            "https://t.alcy.cc/ysz",
            "https://api.suyanw.cn/api/ys/",
            "https://ybapi.cn/API/pixiv.php",
            "https://www.loliapi.com/acg/",
            "https://app.zichen.zone/api/acg/api.php",
            "https://www.dmoe.cc/random.php",
            "https://img.paulzzh.com/touhou/random",
            "https://haowallpaper.com/link/common/file/previewFileImg/15680526683050304"
    );

    private static final File CACHE_DIR = new File(mc.mcDataDir, "bg_cache_tmp");
    private static final File CACHE_FILE = new File(CACHE_DIR, "bg.png");
    static {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuiet(CACHE_DIR), "BG-CACHE-CLEAN"));
        } catch (Throwable ignored) {}
    }

    private static final Object PIPE_LOCK = new Object();
    private static volatile BufferedImage stagedNext = null;
    private static volatile boolean fetchInFlight = false;
    private static volatile BufferedImage cacheBuffer = null;

    public static void loadTextureFromApi() {
        if (CACHE_FILE.isFile()) {
            try {
                BufferedImage cached = ImageIO.read(CACHE_FILE);
                if (cached != null) {
                    BufferedImage scaled = adjustImageToScreenSize(cached);
                    mc.addScheduledTask(() -> {
                        try {
                            DynamicTexture dyn = new DynamicTexture(scaled);
                            ResourceLocation rl = new ResourceLocation("background/bg_" + System.nanoTime());
                            mc.getTextureManager().loadTexture(rl, dyn);
                            prevTexture = backgroundTexture;
                            backgroundTexture = rl;
                            fadeStartMs = System.currentTimeMillis();
                        } catch (Throwable ignored) {}
                    });
                }
            } catch (Throwable ignored) {}
        }
        CompletableFuture
                .supplyAsync(BackgroundTextureManager::fetchFirstValidImage, IO_EXEC)
                .thenAccept(img -> {
                    if (img != null) {
                        BufferedImage scaled = adjustImageToScreenSize(img);
                        mc.addScheduledTask(() -> {
                            try {
                                DynamicTexture dyn = new DynamicTexture(scaled);
                                ResourceLocation rl = new ResourceLocation("background/bg_" + System.nanoTime());
                                mc.getTextureManager().loadTexture(rl, dyn);
                                prevTexture = backgroundTexture;
                                backgroundTexture = rl;
                                fadeStartMs = System.currentTimeMillis();
                            } catch (Throwable ignored) {}
                        });
                    } else {
                        mc.addScheduledTask(BackgroundTextureManager::loadDefaultTexture);
                    }
                });
    }

    // 用这段替换原来的 fetchFirstValidImage()
    private static BufferedImage fetchFirstValidImage() {
        int w = Math.max(1, mc.displayWidth);
        int h = Math.max(1, mc.displayHeight);
        CompletionService<BufferedImage> cs = new ExecutorCompletionService<>(RACE_EXEC);
        java.util.ArrayList<Future<BufferedImage>> futures = new java.util.ArrayList<>();
        for (String api : APIS) {
            Future<BufferedImage> f = cs.submit(() -> {
                BufferedImage img = fetchImage(api);
                if (img != null && img.getWidth() >= w && img.getHeight() >= h) return img;
                throw new Exception("invalid");
            });
            futures.add(f);
        }
        BufferedImage picked = null;
        int remaining = futures.size();
        while (remaining-- > 0) {
            try {
                Future<BufferedImage> done = cs.take();
                BufferedImage img = done.get();
                if (img != null) { picked = img; break; }
            } catch (Throwable ignored) { }
        }
        for (Future<BufferedImage> f : futures) {
            if (!f.isDone()) try { f.cancel(true); } catch (Throwable ignored) { }
        }
        return picked;
    }


    private static BufferedImage fetchImage(String apiUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream in = connection.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) writeCache(img);
                return img;
            }
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static void loadDefaultTexture() {
        try {
            File file = new File(Default);
            if (!file.exists()) return;
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) return;
            bufferedImage = adjustImageToScreenSize(bufferedImage);
            DynamicTexture dyn = new DynamicTexture(bufferedImage);
            ResourceLocation rl = new ResourceLocation("background/bg_" + System.nanoTime());
            mc.getTextureManager().loadTexture(rl, dyn);
            prevTexture = backgroundTexture;
            backgroundTexture = rl;
            fadeStartMs = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    private static BufferedImage adjustImageToScreenSize(BufferedImage image) {
        int screenWidth = Math.max(1, mc.displayWidth);
        int screenHeight = Math.max(1, mc.displayHeight);
        if (image.getWidth() == screenWidth && image.getHeight() == screenHeight) return image;
        BufferedImage resizedImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(image, 0, 0, screenWidth, screenHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public static void renderBackground() {
        if (backgroundTexture == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        float a = fadeAlpha();
        if (prevTexture != null && a < 1f) {
            setColorAlpha(1f - a);
            RenderUtil.drawImage(prevTexture, 0, 0, sw, sh);
        }
        setColorAlpha(a < 1f ? a : 1f);
        RenderUtil.drawImage(backgroundTexture, 0, 0, sw, sh);
        setColorAlpha(1f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        if (prevTexture != null && a >= 1f) {
            TextureManager tm = mc.getTextureManager();
            try { tm.deleteTexture(prevTexture); } catch (Throwable ignored) {}
            prevTexture = null;
        }
    }

    private static float fadeAlpha() {
        long dt = System.currentTimeMillis() - fadeStartMs;
        if (dt <= 0) return 0f;
        if (dt >= fadeDurationMs) return 1f;
        return dt / (float) fadeDurationMs;
    }

    private static void setColorAlpha(float a) {
        GL11.glColor4f(1f, 1f, 1f, Math.max(0f, Math.min(1f, a)));
    }

    private static void writeCache(BufferedImage img) {
        try {
            if (!CACHE_DIR.exists()) CACHE_DIR.mkdirs();
            BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            ImageIO.write(out, "png", CACHE_FILE);
        } catch (Throwable ignored) {}
    }

    private static void deleteQuiet(File f) {
        try {
            if (f == null || !f.exists()) return;
            if (f.isDirectory()) {
                File[] list = f.listFiles();
                if (list != null) for (File c : list) deleteQuiet(c);
            }
            f.delete();
        } catch (Throwable ignored) {}
    }
    @Getter
    private static volatile boolean refreshing = false;

    private static boolean isOnMainMenu() {
        Object s = Minecraft.getMinecraft().currentScreen;
        if (s == null) return false;
        try { if (Class.forName("net.minecraft.client.gui.GuiMainMenu").isInstance(s)) return true; } catch (Throwable ignored) {}
        try { if (Class.forName("net.minecraft.client.gui.screens.TitleScreen").isInstance(s)) return true; } catch (Throwable ignored) {}
        return false;
    }

    private static void prefetchAsync() {
        if (fetchInFlight) return;
        fetchInFlight = true;
        CompletableFuture
                .supplyAsync(BackgroundTextureManager::fetchFirstValidImage, IO_EXEC)
                .whenComplete((img, err) -> {
                    try {
                        if (img != null) {
                            synchronized (PIPE_LOCK) {
                                stagedNext = img;
                                PIPE_LOCK.notifyAll();
                            }
                        }
                    } finally {
                        fetchInFlight = false;
                    }
                });
    }

    private static BufferedImage takeNextBlocking() {
        if (cacheBuffer != null) {
            BufferedImage out = cacheBuffer;
            cacheBuffer = null;
            return out;
        }
        synchronized (PIPE_LOCK) {
            while (stagedNext == null) {
                prefetchAsync();
                try { PIPE_LOCK.wait(); } catch (InterruptedException ignored) {}
            }
            BufferedImage out = stagedNext;
            stagedNext = null;
            return out;
        }
    }

    public static boolean toggleRefreshing(long intervalMillis) {
        if (refreshing) {
            refreshing = false;
            return false;
        } else {
            refreshing = true;
            if (CACHE_FILE.isFile()) {
                try { cacheBuffer = ImageIO.read(CACHE_FILE); } catch (Throwable ignored) {}
            } else {
                cacheBuffer = null;
            }
            prefetchAsync();
            new Thread(() -> {
                long sleepNs = Math.max(1L, intervalMillis) * 1_000_000L;
                while (refreshing) {
                    try {
                        if (!isOnMainMenu()) {
                            LockSupport.parkNanos(200_000_000L);
                            continue;
                        }
                        BufferedImage img = takeNextBlocking();
                        if (img != null) {
                            BufferedImage scaled = adjustImageToScreenSize(img);
                            mc.addScheduledTask(() -> {
                                try {
                                    DynamicTexture dyn = new DynamicTexture(scaled);
                                    ResourceLocation rl = new ResourceLocation("background/bg_" + System.nanoTime());
                                    mc.getTextureManager().loadTexture(rl, dyn);
                                    prevTexture = backgroundTexture;
                                    backgroundTexture = rl;
                                    fadeStartMs = System.currentTimeMillis();
                                } catch (Throwable ignored) {}
                            });
                        }
                        prefetchAsync();
                        long deadline = System.nanoTime() + sleepNs;
                        while (refreshing && isOnMainMenu()) {
                            long now = System.nanoTime();
                            long left = deadline - now;
                            if (left <= 0) break;
                            LockSupport.parkNanos(Math.min(left, 50_000_000L));
                        }
                    } catch (Throwable ignored) {}
                }
            }, "BG-Refresher").start();
            return true;
        }
    }
}
