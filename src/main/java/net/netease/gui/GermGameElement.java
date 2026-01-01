package net.netease.gui;

import dev.xinxin.utils.DebugUtil;
import dev.xinxin.utils.TimerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.netease.GsonUtil;
import net.netease.PacketProcessor;
import net.netease.packet.impl.Packet26;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ByteBreaker
 * create 29/12/2023
 */
@Getter
public class GermGameElement {
    private final List<GermGameSubElement> subElements = new ArrayList<>();
    private final String name;
    private final String defaultPath;
    private final String hoverPath;
    private final List<String> hoverDos;
    private ResourceLocation defaultImage;
    private ResourceLocation hoverImage;
    private GifDecoder.GifImage gifImage;
    private final List<Integer> delayList = new ArrayList<>();
    private final TimerUtil updateTimer = new TimerUtil();
    private int imageCount;
    @Setter
    private Runnable runnable;
    private final String clickName;

    public GermGameElement(String name, String defaultPath, String hoverPath, List<String> hoverDos, String clickName) {
        this.name = name;
        this.defaultPath = defaultPath;
        this.hoverPath = hoverPath;
        this.hoverDos = hoverDos;
        this.clickName = clickName;
    }

    public synchronized void loadTexture() throws IOException {
        this.defaultImage = TextureUtil.loadTextureFormURL(defaultPath);
    }

    public synchronized void loadHoverTexture() {
        try {
            URL url = new URL(hoverPath);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            gifImage = GifDecoder.read(IOUtils.toByteArray(inputStream));
            for (int i = 0; i < gifImage.getFrameCount(); i++) {
                BufferedImage bufferedImage = gifImage.getFrame(i);
                ResourceLocation resourceLocation = new ResourceLocation(String.valueOf(hoverPath.hashCode() + i));
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft.getMinecraft().getTextureManager().loadTexture(resourceLocation, new DynamicTexture(bufferedImage));
                });
                delayList.add(gifImage.getDelay(i));
            }
            inputStream.close();
        } catch (Exception e) {
            DebugUtil.log(e);
        }
    }
    public ResourceLocation getCurrentGifImage() {
        try {
            if (imageCount >= gifImage.getFrameCount()) {
                imageCount = 0;
            }
            if (updateTimer.hasTimeElapsed(delayList.get(imageCount) * 10)) {
                hoverImage = new ResourceLocation(String.valueOf(hoverPath.hashCode() + imageCount));
                updateTimer.reset();
                imageCount++;
            }
            return hoverImage;
        } catch (Exception e) {
            e.printStackTrace();

            return TextureMap.LOCATION_MISSING_TEXTURE;
        }
    }

    public void click(String guiName) {
        Map<String, Object> data = new HashMap<>();
        data.put("click", 1);
        String json = GsonUtil.toJson(data);
        String message = new StringBuilder().insert(0, "GUI$").append(guiName).append("@").append(clickName).toString();


        PacketProcessor.INSTANCE.sendPacket(new Packet26(message, json));
    }
}
