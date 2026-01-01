package dev.xinxin.config;

import com.google.gson.*;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectArrayList;
import dev.xinxin.SilenceFix;
import dev.xinxin.api.netease.NeteaseAccount;
import dev.xinxin.config.configs.HudConfig;
import dev.xinxin.config.configs.ModuleConfig;
import dev.xinxin.gui.altmanager.AltManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;

public class ConfigManager
{
    private static final Logger logger = LogManager.getLogger("ConfigManager");
    public static final List<Config> configs;
    public static final File dir;
    private static final Gson gson;

    public ConfigManager() {
        if (!ConfigManager.dir.exists()) {
            ConfigManager.dir.mkdir();
        }
        ConfigManager.configs.add((Config)new ModuleConfig());
        ConfigManager.configs.add((Config)new HudConfig());
    }

    public void loadIRCFriends() {
        SilenceFix.instance.ircFriends.clear();

        try {
            final File file = new File(ConfigManager.dir, "ircFriends.txt");

            if (!file.exists()) {
                return;
            }

            for (String name : FileUtils.readLines(file, StandardCharsets.UTF_8)) {
                if (name.isEmpty()) {
                    continue;
                }

                SilenceFix.instance.ircFriends.add(name);
            }
        } catch (IOException e) {
            logger.error("Can't load irc friends", e);
        }
    }

    public void saveIRCFriends() {
        try {
            final File file = new File(ConfigManager.dir, "ircFriends.txt");

            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (String ircFriend : SilenceFix.instance.ircFriends) {
                    writer.write(ircFriend);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.error("Can't save irc friends", e);
        }
    }

    public void loadAlts() {
        AltManager.Instance.getAltList().clear();

        try {
            final File file = new File(ConfigManager.dir, "alts.json");

            if (!file.exists()) {
                return;
            }

            final JsonArray array = new JsonParser().parse(FileUtils.readFileToString(file)).getAsJsonArray();

            for (JsonElement jsonElement : array) {
                final JsonObject altObject = jsonElement.getAsJsonObject();
                try {
                    AltManager.Instance.getAltList().add(new NeteaseAccount(altObject.get("cookies").getAsString(),altObject.get("userid").getAsString()));
                }catch (Exception e){
                    logger.error("Can't load alt", e);
                }
            }
        } catch (Exception e) {
            logger.error("Can't load irc friends", e);
        }
    }

    public void saveAlts() {
        try {
            final File file = new File(dir, "alts.json");

            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                final JsonArray jsonArray = new JsonArray();

                for (NeteaseAccount alt : AltManager.Instance.getAltList()) {
                    final JsonObject altObject = new JsonObject();

                    altObject.addProperty("cookies", alt.cookies.toString());
                    altObject.addProperty("userid", alt.getEntityId());

                    jsonArray.add(altObject);
                }

                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray));
            }
        } catch (IOException e) {
            logger.error("Can't save alts", e);
        }
    }

    public void loadConfig(final String name) {
        JsonParser jsonParser = new JsonParser();
        final File file = new File(ConfigManager.dir, name);
        if (file.exists()) {
            System.out.println("Loading config: " + name);
            for (final Config config : ConfigManager.configs) {
                if (config.getName().equals(name)) {
                    try {
                        config.loadConfig(jsonParser.parse((Reader)new FileReader(file)).getAsJsonObject());
                    }
                    catch (FileNotFoundException e) {
                        System.out.println("Failed to load config: " + name);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Config " + name + " doesn't exist, creating a new one...");
            this.saveConfig(name);
        }
    }

    public void loadUserConfig(final String name) {
        JsonParser jsonParser = new JsonParser();
        final File file = new File(ConfigManager.dir, name);
        if (file.exists()) {
            System.out.println("Loading config: " + name);
            for (final Config config : ConfigManager.configs) {
                if (config.getName().equals("modules.json")) {
                    try {
                        config.loadConfig(jsonParser.parse((Reader)new FileReader(file)).getAsJsonObject());
                    }
                    catch (FileNotFoundException e) {
                        System.out.println("Failed to load config: " + name);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Config " + name + " doesn't exist, creating a new one...");
            this.saveUserConfig(name);
        }
    }

    public void saveConfig(final String name) {
        final File file = new File(ConfigManager.dir, name);
        try {
            System.out.println("Saving config: " + name);
            file.createNewFile();
            for (final Config config : ConfigManager.configs) {
                if (config.getName().equals(name)) {
                    FileUtils.writeByteArrayToFile(file, ConfigManager.gson.toJson((JsonElement)config.saveConfig()).getBytes(StandardCharsets.UTF_8));
                    break;
                }
            }
        }
        catch (IOException e) {
            System.out.println("Failed to save config: " + name);
        }
    }

    public void saveUserConfig(final String name) {
        final File file = new File(ConfigManager.dir, name);
        try {
            System.out.println("Saving config: " + name);
            file.createNewFile();
            for (final Config config : ConfigManager.configs) {
                if (config.getName().equals("modules.json")) {
                    FileUtils.writeByteArrayToFile(file, ConfigManager.gson.toJson((JsonElement)config.saveConfig()).getBytes(StandardCharsets.UTF_8));
                    break;
                }
            }
        }
        catch (IOException e) {
            System.out.println("Failed to save config: " + name);
        }
    }

    public void loadAllConfig() {
        System.out.println("Loading all configs...");
        loadIRCFriends();
        loadAlts();
        ConfigManager.configs.forEach(it -> this.loadConfig(it.getName()));
    }

    public void saveAllConfig() {
        System.out.println("Saving all configs...");
        saveIRCFriends();
        saveAlts();
        ConfigManager.configs.forEach(it -> this.saveConfig(it.getName()));
    }
    public List<String> getConfigs() {
        Stream<Path> filesStream;

        try {
            filesStream = walk(dir.toPath());
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return filesStream // @off
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(s -> s.endsWith(".json"))
                .map(s -> s.substring(0, s.length() - ".json".length()))
                .collect(Collectors.toCollection(ObjectArrayList::new)); // @on
    }
    static {
        configs = new ArrayList<Config>();
        dir = new File(SilenceFix.mc.mcDataDir, "XinXin");
        gson = new GsonBuilder().setPrettyPrinting().create();
    }
}
