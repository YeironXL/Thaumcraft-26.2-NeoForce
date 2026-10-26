package art.arcane.thaumcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Research completed by a world trigger; the client shows the TC6 research toast. */
public record ResearchToastPayload(String key) implements CustomPacketPayload {
    public static final Type<ResearchToastPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "research_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchToastPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ResearchToastPayload::key, ResearchToastPayload::new);
    @Override public Type<ResearchToastPayload> type() { return TYPE; }
}
