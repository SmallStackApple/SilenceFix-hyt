package net.minecraft.network.play;

import net.minecraft.network.INetHandler;
import net.minecraft.network.play.client.*;

public interface INetHandlerPlayServer
extends INetHandler {
    public void handleAnimation(C0APacketAnimation var1);

    public void processChatMessage(C01PacketChatMessage var1);

    public void processTabComplete(C14PacketTabComplete var1);

    public void processClientStatus(C16PacketClientStatus var1);

    public void processClientSettings(C15PacketClientSettings var1);

    public void processConfirmTransaction(C0FPacketConfirmTransaction var1);

    public void processEnchantItem(C11PacketEnchantItem var1);

    public void processClickWindow(C0EPacketClickWindow var1);

    public void processCloseWindow(C0DPacketCloseWindow var1);

    public void processVanilla250Packet(C17PacketCustomPayload var1);

    public void processUseEntity(C02PacketUseEntity var1);

    public void processKeepAlive(C00PacketKeepAlive var1);

    public void processPlayer(C03PacketPlayer var1);

    public void processPlayerAbilities(C13PacketPlayerAbilities var1);

    public void processPlayerDigging(C07PacketPlayerDigging var1);

    public void processEntityAction(C0BPacketEntityAction var1);

    public void processInput(C0CPacketInput var1);

    public void processHeldItemChange(C09PacketHeldItemChange var1);

    public void processCreativeInventoryAction(C10PacketCreativeInventoryAction var1);

    public void processUpdateSign(C12PacketUpdateSign var1);

    public void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement var1);

    public void handleSpectate(C18PacketSpectate var1);

    public void handleResourcePackStatus(C19PacketResourcePackStatus var1);
}

