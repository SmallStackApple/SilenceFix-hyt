/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.netease;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.netease.PacketProcessor.HYT_REGISTER;
import static net.netease.PacketProcessor.MOD_LIST;


/**
 * Packet handshake sequence manager- client side (responding to remote server)
 * <p>
 * Flow:
 * 1. Wait for server hello. (START). Move to HELLO state.
 * 2. Receive Server Hello. Send customchannel registration. Send SilenceFix Hello. Send our modlist. Move to WAITINGFORSERVERDATA state.
 * 3. Receive server modlist. Send ack if acceptable, else send nack and exit error. Receive server IDs. Move to COMPLETE state. Send ack.
 *
 * @author cpw
 */
public enum FMLHandshakeClientState implements IHandshakeState<FMLHandshakeClientState> {
    START {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            cons.accept(HELLO);
        }
    },
    HELLO {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            cons.accept(WAITINGSERVERDATA);
            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("REGISTER", new PacketBuffer(Unpooled.buffer().writeBytes(HYT_REGISTER)));


            PacketBuffer helloBuffer = new PacketBuffer(Unpooled.buffer());
            helloBuffer.writeByte(1);
            helloBuffer.writeByte(2);

            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", helloBuffer);

            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", new PacketBuffer(Unpooled.buffer()
                    .writeBytes(MOD_LIST)));
        }
    },

    WAITINGSERVERDATA {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {

            cons.accept(WAITINGSERVERCOMPLETE);
            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", new PacketBuffer(Unpooled.buffer().writeByte(-1)
                    .writeByte(2)));

        }
    },
    WAITINGSERVERCOMPLETE {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            Map<ResourceLocation, Integer> ids = new HashMap<>();
            Set<ResourceLocation> dummied = new HashSet<>();
            boolean hasMore = payload.readBoolean();

            if (hasMore) {
                cons.accept(WAITINGSERVERCOMPLETE);
                return;
            }
            cons.accept(PENDINGCOMPLETE);

            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", new PacketBuffer(Unpooled.buffer().writeByte(-1).writeByte(3)));
        }
    },
    PENDINGCOMPLETE {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            cons.accept(COMPLETE);
            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", new PacketBuffer(Unpooled.buffer().writeByte(-1).writeByte(4)));
        }
    },
    COMPLETE {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            cons.accept(DONE);
            PacketProcessor.INSTANCE.getForgeChannel().sendToServer("FML|HS", new PacketBuffer(Unpooled.buffer().writeByte(-1).writeByte(5)));
        }
    },
    DONE {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
            if (id == -2) {
                cons.accept(HELLO);
            }
        }
    },
    ERROR {
        @Override
        public void accept(int id, ByteBuf payload, Consumer<? super FMLHandshakeClientState> cons) {
        }
    };
}