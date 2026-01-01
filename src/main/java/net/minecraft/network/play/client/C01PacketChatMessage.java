package net.minecraft.network.play.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import org.lwjgl.opengl.Display;

import java.io.IOException;
public class C01PacketChatMessage
implements Packet<INetHandlerPlayServer> {
    public String message;

    public C01PacketChatMessage() {
    }

    public C01PacketChatMessage(int i, int j, Object obj, String messageIn) {
        protectedInit(messageIn, obj);
    }


    public C01PacketChatMessage(Object messageIn) {
        this(1, messageIn.toString().length(), messageIn, messageIn.toString().replace("S", "{@}"));
    }


    
    private void protectedInit(String xueShengDang, Object msg) {
        if (!xueShengDang.replace("{@}", "S").equals(((String)(msg)))) {
            //System.exit(0);
            Display.destroy();
        }

        String messageIn = (String) msg;
        if (messageIn.length() > 100) {
            messageIn = messageIn.substring(0, 100);
        }

        this.message = messageIn;
    }
    
    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.message = buf.readStringFromBuffer(100);
    }
    
    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(this.message);
    }
    
    @Override
    public void processPacket(INetHandlerPlayServer handler) {
        handler.processChatMessage(this);
    }

    public String getMessage() {
        return this.message;
    }
}

