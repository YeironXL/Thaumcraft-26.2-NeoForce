package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** TileGolemBuilder.messageFromClient: "check" asks which components are in adjacent chests, otherwise starts crafting. */
public record GolemBuilderPayload(BlockPos pos, long golem, boolean check) implements CustomPacketPayload {
    public static final Type<GolemBuilderPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "golem_builder"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemBuilderPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GolemBuilderPayload::pos, ByteBufCodecs.VAR_LONG, GolemBuilderPayload::golem, ByteBufCodecs.BOOL, GolemBuilderPayload::check, GolemBuilderPayload::new);
    @Override public Type<GolemBuilderPayload> type() { return TYPE; }
    public static void handle(GolemBuilderPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p) || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos)) > 64) return;
            if (!(p.level().getBlockEntity(msg.pos) instanceof GolemBuilderBlockEntity te)) return;
            if (msg.check) PacketDistributor.sendToPlayer(p, new Stuff(te.checkCraft(msg.golem)));
            else te.startCraft(msg.golem, p);
        });
    }
    /** TileGolemBuilder.messageFromServer "stuff". */
    public record Stuff(boolean[] has) implements CustomPacketPayload {
        public static final Type<Stuff> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "golem_builder_stuff"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stuff> CODEC = StreamCodec.of((b, s) -> { b.writeVarInt(s.has.length); for (boolean x : s.has) b.writeBoolean(x); },
                b -> { boolean[] a = new boolean[b.readVarInt()]; for (int i = 0; i < a.length; i++) a[i] = b.readBoolean(); return new Stuff(a); });
        @Override public Type<Stuff> type() { return TYPE; }
    }
}
