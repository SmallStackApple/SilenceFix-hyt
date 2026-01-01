package dev.yalan.live.silencefix;

import dev.xinxin.command.Command;
import dev.xinxin.utils.client.HelperUtil;

import java.util.ArrayList;
import java.util.List;

public class QueryClientsCommand extends Command {
    public QueryClientsCommand() {
        super("is");
    }


    @Override
    public List<String> autoComplete(int var1, String[] var2) {
        return new ArrayList<>();
    }

    @Override
    public void run(String[] var1) {
        HelperUtil.sendMessage("§f当前IRC在线人数: " + LiveClient.INSTANCE.onlinePlayerCount);
    }
}



