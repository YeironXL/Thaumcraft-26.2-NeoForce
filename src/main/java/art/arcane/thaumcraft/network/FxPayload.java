package art.arcane.thaumcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Server-triggered client effects (TC6 PacketFX* messages and block events that call FXDispatcher). */
public record FxPayload(String kind, double x, double y, double z, List<Float> args) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "fx"));
    private static final StreamCodec<ByteBuf, List<Float>> FLOATS = ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(256));
    public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1024), FxPayload::kind, ByteBufCodecs.DOUBLE, FxPayload::x, ByteBufCodecs.DOUBLE, FxPayload::y,
            ByteBufCodecs.DOUBLE, FxPayload::z, FLOATS, FxPayload::args, FxPayload::new);
    @Override public Type<FxPayload> type() { return TYPE; }
}
