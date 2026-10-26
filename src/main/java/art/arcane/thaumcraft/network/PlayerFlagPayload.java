package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.events.BaubleEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** TC6 PacketPlayerFlagToServer: flag 1 = ring of clouds jump (reset fall distance). */
public record PlayerFlagPayload(int flag) implements CustomPacketPayload {
    public static final Type<PlayerFlagPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "player_flag"));
    public static final StreamCodec<ByteBuf, PlayerFlagPayload> CODEC = ByteBufCodecs.VAR_INT.map(PlayerFlagPayload::new, PlayerFlagPayload::flag);
    @Override public Type<PlayerFlagPayload> type() { return TYPE; }
    public static void handle(PlayerFlagPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer p && msg.flag == 1) BaubleEvents.cloudJump(p); });
    }
}
