package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity;
import art.arcane.thaumcraft.casters.FocusElementNode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** TC6 PacketFocusNodesToServer / PacketFocusNameToServer (nodes == null: name only). */
public record FocusNodesPayload(BlockPos pos, boolean nameOnly, List<CompoundTag> nodes, String name) implements CustomPacketPayload {
    public static final Type<FocusNodesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "focus_nodes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusNodesPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FocusNodesPayload::pos, ByteBufCodecs.BOOL, FocusNodesPayload::nameOnly,
            ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list(255)), FocusNodesPayload::nodes, ByteBufCodecs.stringUtf8(64), FocusNodesPayload::name, FocusNodesPayload::new);
    @Override public Type<FocusNodesPayload> type() { return TYPE; }

    public static void handle(FocusNodesPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p) || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos)) > 64) return;
            if (!(p.level().getBlockEntity(msg.pos) instanceof FocalManipulatorBlockEntity te) || te.vis > 0) return;
            if (!msg.nameOnly) {
                te.data.clear();
                for (CompoundTag t : msg.nodes) { FocusElementNode n = new FocusElementNode(); n.deserialize(t); te.data.put(n.id, n); }
            }
            te.focusName = msg.name;
            te.setChanged();
        });
    }
}
