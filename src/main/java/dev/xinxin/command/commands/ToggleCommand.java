package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToggleCommand
extends Command {
    public ToggleCommand() {
        super("toggle", "t");
    }

    @Override
    public List<String> autoComplete(int arg, String[] args) {
        String prefix = "";
        boolean flag = false;
        if (arg == 0 || args.length == 0) {
            flag = true;
        } else if (arg == 1) {
            flag = true;
            prefix = args[0];
        }
        if (flag) {
            String finalPrefix = prefix;
            return SilenceFix.instance.moduleManager.getModules().stream().filter(mod -> mod.getName().toLowerCase().startsWith(finalPrefix)).map(Module::getName).collect(Collectors.toList());
        }
        if (arg == 2) {
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("none");
            arrayList.add("show");
            return arrayList;
        }
        return new ArrayList<String>();
    }

    @Override
    public void run(String[] args) {
        if (args.length == 1) {
            Module m = SilenceFix.instance.moduleManager.getModule(args[0]);
            if (m != null) {
                m.toggle();
            } else {
                HelperUtil.sendMessage((Object)((Object)EnumChatFormatting.RED) + "Module not found!");
            }
        } else {
            HelperUtil.sendMessage((Object)((Object)EnumChatFormatting.RED) + "Usage: t <Module>");
        }
    }
}

