package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * TC6 PacketNote: the client (goggles looking at a note block or arcane ear, every 5 ticks) asks with note -1; the
 * server answers with the note to everyone within 8 blocks, and the client stores it on the arcane ear. The dimension
 * of TC6's packet is the sender's own level here.
 */
public record NotePayload(BlockPos pos, byte note) implements CustomPacketPayload {
    public static final Type<NotePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "note"));
    public static final StreamCodec<ByteBuf, NotePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NotePayload::pos, ByteBufCodecs.BYTE, NotePayload::note, NotePayload::new);
    @Override public Type<NotePayload> type() { return TYPE; }

    /** Server side note of a note block (its NOTE state) or an arcane ear, -1 otherwise. */
    public static byte noteAt(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof NoteBlock) return state.getValue(NoteBlock.NOTE).byteValue();
        if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) return (byte) ear.note;
        return -1;
    }

    /** Server handling of a request: returns the answered note (or -1 when nothing was sent). */
    public static byte answer(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        if (!level.isLoaded(pos)) return -1;
        byte note = noteAt(level, pos);
        if (note >= 0) PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 8.0, new NotePayload(pos, note));
        return note;
    }

    public static void handle(NotePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound()) { if (msg.note == -1 && ctx.player() instanceof ServerPlayer p) answer(p, msg.pos); }
            else if (msg.note >= 0 && ctx.player().level().getBlockEntity(msg.pos) instanceof ArcaneEarBlockEntity ear) ear.note = msg.note;
        });
    }
}
