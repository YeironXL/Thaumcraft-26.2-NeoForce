package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** TC6 PacketSelectThaumotoriumRecipeToServer: toggles a recipe in a thaumatorium the player has open. */
public record ThaumatoriumSelectPayload(BlockPos pos, String recipe) implements CustomPacketPayload {
    public static final Type<ThaumatoriumSelectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "thaumatorium_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThaumatoriumSelectPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ThaumatoriumSelectPayload::pos, ByteBufCodecs.stringUtf8(256), ThaumatoriumSelectPayload::recipe, ThaumatoriumSelectPayload::new);
    @Override public Type<ThaumatoriumSelectPayload> type() { return TYPE; }

    public static void handle(ThaumatoriumSelectPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p) || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos)) > 64) return;
            if (p.level().getBlockEntity(msg.pos) instanceof ThaumatoriumBlockEntity te) te.select(p, msg.recipe);
        });
    }
}
