package dev.xinxin.utils.misc;

import dev.xinxin.utils.ChatUtil;
import net.minecraft.util.EnumChatFormatting;

public class ClientUtils {
    public static void sendMessage(String message) {
        new ChatUtil.ChatMessageBuilder(true, true).appendText(message).setColor(EnumChatFormatting.GRAY).build().displayClientSided();
    }
}
