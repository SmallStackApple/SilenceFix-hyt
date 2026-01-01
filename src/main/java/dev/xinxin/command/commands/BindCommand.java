package dev.xinxin.command.commands;

import dev.xinxin.SilenceFix;
import dev.xinxin.command.Command;
import dev.xinxin.module.Module;
import dev.xinxin.utils.client.HelperUtil;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "b");
    }

    @Override
    public List<String> autoComplete(int arg, String[] args) {
        if (arg == 0 || arg == 1) {
            String input = args.length > 0 ? args[0].toLowerCase() : "";

            return SilenceFix.instance.moduleManager.getModules().stream()
                    .filter(mod -> mod.isBindable() && (
                            mod.getName().toLowerCase().contains(input)
                                    || mod.getCnName().toLowerCase().contains(input)))
                    .map(Module::getName)
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (arg == 2) {
            return new ArrayList<>(Arrays.asList(
                    "none", "mouse1", "mouse2", "mouse3", "cejian1", "cejian2"
            ));
        }

        return new ArrayList<>();
    }

    @Override
    public void run(String[] args) {
        if (args.length == 2) {
            Module m = SilenceFix.instance.moduleManager.getModules().stream()
                    .filter(mod -> mod.isBindable() && (
                            mod.getName().equalsIgnoreCase(args[0])
                                    || mod.getCnName().equalsIgnoreCase(args[0])))
                    .findFirst()
                    .orElse(null);

            if (m != null) {
                int key = parseKey(args[1]);
                m.setKey(key);
                HelperUtil.sendMessage(EnumChatFormatting.GREEN + "成功绑定 " + m.getName() + " 到 " + getKeyName(key));
            } else {
                HelperUtil.sendMessage(EnumChatFormatting.RED + "模块不存在或不支持绑定！");
            }
        } else {
            HelperUtil.sendMessage(EnumChatFormatting.RED + "用法: .bind <模块名> <键位>");
        }
    }

    private int parseKey(String input) {
        input = input.toLowerCase();
        return switch (input) {
            case "none" -> -1;
            case "mouse1" -> 0;
            case "mouse2" -> 1;
            case "mouse3" -> 2;
            case "cejian1" -> 3;
            case "cejian2" -> 4;
            default -> Keyboard.getKeyIndex(input.toUpperCase());
        };
    }

    private String getKeyName(int keyCode) {
        return switch (keyCode) {
            case -1 -> "None";
            case 0 -> "Mouse1";
            case 1 -> "Mouse2";
            case 2 -> "Mouse3";
            case 3 -> "cejian1";
            case 4 -> "cejian2";
            default -> Keyboard.getKeyName(keyCode);
        };
    }
}
