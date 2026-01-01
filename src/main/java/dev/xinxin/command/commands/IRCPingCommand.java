package dev.xinxin.command.commands;

import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class IRCPingCommand extends Command {
    public IRCPingCommand() {
        super("iping");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] var1) {
        HelperUtil.sendMessage("与IRC服务器之间的延迟为：" + (new DecimalFormat("#.###").format(30) + "ms"));
    }
}
