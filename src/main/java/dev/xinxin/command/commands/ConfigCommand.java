package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class ConfigCommand
extends Command {
    public ConfigCommand() {
        super("config", "cfg");
    }

    @Override
    public List<String> autoComplete(int arg, String[] args) {
        return new ArrayList<String>();
    }

    @Override
    public void run(String[] args) {
        if (args.length == 2) {
            switch (args[0]) {
                case "save": {
                    String name = args[1];
                    if (!name.isEmpty()) {
                        SilenceFix.instance.configManager.saveUserConfig(name + ".json");
                        HelperUtil.sendMessage(EnumChatFormatting.GREEN + "Config " + name + " 保存成功!");
                        break;
                    }
                    HelperUtil.sendMessage(EnumChatFormatting.RED + "?");
                    break;
                }
                case "load": {
                    String name = args[1];
                    if (!name.isEmpty()) {
                        SilenceFix.instance.configManager.loadUserConfig(name + ".json");
                        HelperUtil.sendMessage(EnumChatFormatting.GREEN + "Config " + name + " 加载成功!");
                        break;
                    }
                    HelperUtil.sendMessage(EnumChatFormatting.RED + "?");
                    break;
                }
            }
        } else {
            HelperUtil.sendMessage(EnumChatFormatting.RED + "Usage: config save/load <name>");
        }
    }
}

