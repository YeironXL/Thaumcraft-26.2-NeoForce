package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.item.gear.HoverHarnessItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Thaumostatic Harness on/off key (TC4 'H'). */
public record HarnessTogglePayload() implements CustomPacketPayload {
    public static final HarnessTogglePayload INSTANCE = new HarnessTogglePayload();
    public static final Type<HarnessTogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "harness_toggle"));
    public static final StreamCodec<ByteBuf, HarnessTogglePayload> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<HarnessTogglePayload> type() { return TYPE; }
    public static void handle(HarnessTogglePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer p) HoverHarnessItem.toggle(p); });
    }
}
