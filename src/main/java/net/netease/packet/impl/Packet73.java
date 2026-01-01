package net.netease.packet.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xinxin.gui.CustomMenuButton;
import net.minecraft.network.PacketBuffer;
import net.netease.GsonUtil;
import net.netease.PacketProcessor;
import net.netease.gui.GermGameElement;
import net.netease.gui.GermGameGui;
import net.netease.gui.party.GermPartyGui;
import net.netease.image.ImageLoader;
import net.netease.packet.GermPacket;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.xinxin.utils.misc.MinecraftInstance.mc;


/**
 * @author ByteBreaker
 * create 28/12/2023
 */
public class Packet73 implements GermPacket {
    private String type;
    private String name;
    private String data;

    @Override
    public void process() {
        Yaml yaml = new Yaml(new Constructor(Map.class, new LoaderOptions()));
        Map<String, Object> data = yaml.load(this.data);
        JsonElement element = GsonUtil.fromJson(GsonUtil.toJson(data), JsonElement.class);
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject jsonObject = element.getAsJsonObject().getAsJsonObject(name);
        if ("mainmenu".equals(name)) {

            JsonObject relativeParts = jsonObject.getAsJsonObject("自适应背景").getAsJsonObject("relativeParts");

            JsonObject mainRelativeParts = relativeParts.getAsJsonObject("主分类").getAsJsonObject("relativeParts");

            List<GermGameElement> elements = GermGameGui.INSTANCE.getElements();

            if (elements.isEmpty()) {
                for (Map.Entry<String, JsonElement> entry : mainRelativeParts.entrySet()) {
                    if (entry.getKey().startsWith("subject")) {
                        JsonObject subjectObj = entry.getValue().getAsJsonObject();
                        String name = entry.getKey();
                        String defaultPath = subjectObj.get("defaultPath").getAsString();
                        defaultPath = defaultPath.substring(defaultPath.indexOf("https"));
                        String hoverPath = subjectObj.get("hoverPath").getAsString();
                        hoverPath = hoverPath.substring(hoverPath.indexOf("https"));
                        String hoverDos = subjectObj.get("hoverDos").getAsString();
                        String clickName = subjectObj.get("clickScript").getAsString();
                        clickName = clickName.split("'")[1];

                        List<String> hoverDoes = Arrays.stream(hoverDos.split("\n"))
                                .filter(s -> s.startsWith("§9"))
                                .collect(Collectors.toList());

                        GermGameElement gameElement = new GermGameElement(name, defaultPath, hoverPath, hoverDoes, clickName);
                        elements.add(gameElement);
                    }
                }
            }

            mc.threadPool.execute(() -> {
                synchronized (this) {
                    try {
                        ImageLoader.loadImage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        } else if (name.endsWith("_effect")) {
            JsonObject guiObject = jsonObject.getAsJsonObject("gui");
            JsonObject amountObject = null;
            JsonObject lastObject = null;
            int index = 0;

            for (Map.Entry<String, JsonElement> entry : guiObject.entrySet()) {
                if (entry.getValue() instanceof JsonObject) {
                    if (index == 2) {
                        amountObject = (JsonObject) entry.getValue();
                    }
                    lastObject = (JsonObject) entry.getValue();
                }
                index++;
            }

            if (amountObject != null) {
                putText(amountObject.getAsJsonArray("texts"));
            }

            if (lastObject == null) return;
            JsonObject relativeParts = lastObject.getAsJsonObject("relativeParts");
            for (Map.Entry<String, JsonElement> entry : relativeParts.entrySet()) {
                JsonObject inRelativeParts = entry.getValue().getAsJsonObject().getAsJsonObject("relativeParts");
                for (Map.Entry<String, JsonElement> inEntry : inRelativeParts.entrySet()) {
                    putText(inEntry.getValue().getAsJsonObject().getAsJsonArray("texts"));
                }
            }
        }
    }

    private boolean check(GermPartyGui.Type type) {

        if (!GermPartyGui.INSTANCE.getButtons().containsKey(type)) {
            GermPartyGui.INSTANCE.getButtons().put(type, new ArrayList<>());
        }

        return !GermPartyGui.INSTANCE.getButtons().get(type).isEmpty();
    }

    private void addPartyButton(GermPartyGui.Type type, String text, String script) {
        GermPartyGui.INSTANCE.getButtons().get(type).add(new CustomMenuButton(text, getClickAction(script)));
    }

    private Runnable getClickAction(String script) {
        Matcher matcher = getMatcher(script);
        if (matcher.find()) {
            String string = new StringBuilder().insert(0, "GUI$").append(name).append("@").append(matcher.group(1)).toString();
            return () -> {
                PacketProcessor.INSTANCE.sendPacket(new Packet26(string, matcher.group(2)));
            };
        }
        return null;
    }

    private Matcher getMatcher(String script) {
        String regex = "GuiScreen\\.post\\('([^']+)',\\s*(\\{[^}]+\\})\\);";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(script);
    }

    private void putText(JsonArray texts) {
        /*Map<String, String> punishMap = PacketProcessor.INSTANCE.getPunishMap();
        for (JsonElement text : texts) {
            punishMap.put(text.getAsString(), punishMap.getOrDefault(text.getAsString(), null));
        }*/
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        this.type = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
        this.name = packetBuffer.readStringFromBuffer(Short.MAX_VALUE);
        this.data = packetBuffer.readStringFromBuffer(9999999);
    }

    @Override
    public int getPacketId() {
        return 73;
    }
}
