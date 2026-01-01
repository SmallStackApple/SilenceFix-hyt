package dev.xinxin.command.commands;

import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;
import dev.yalan.live.silencefix.LiveClient;
import dev.yalan.live.silencefix.LiveUser;
import dev.yalan.live.silencefix.netty.LiveProto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IRCExecuteCommand extends Command {
    public IRCExecuteCommand() {
        super("ie");
    }

    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] args) {
        final int len = args.length;

        if (len < 1) {
            HelperUtil.sendMessage(".ie <command>");
            return;
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(args[i]);

            if (i != len - 1) {
                sb.append(' ');
            }
        }

        if (LiveClient.INSTANCE.liveUser.getLevel() != LiveUser.Level.ADMINISTRATOR) {
            HelperUtil.sendMessage("需要IRC管理员权限");
            return;
        }

        final UUID executionId = UUID.randomUUID();

        LiveClient.INSTANCE.getLiveComponent().getCommandOutMap().put(executionId, HelperUtil::sendMessage);
        LiveClient.INSTANCE.sendPacket(LiveProto.createExecuteCommand(executionId, sb.toString()));
    }
}
