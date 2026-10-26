package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.golems.seals.SealPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** TC6 PacketSealToClient: a placed/changed seal (full NBT), a removal, or "forget all" when the player changes level. */
public record SealSyncPayload(boolean remove, BlockPos pos, Direction face, CompoundTag data, boolean clear) implements CustomPacketPayload {
    public SealSyncPayload(boolean remove, BlockPos pos, Direction face, CompoundTag data) { this(remove, pos, face, data, false); }
    public static final Type<SealSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "seal_sync"));
    public static final StreamCodec<FriendlyByteBuf, SealSyncPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeBoolean(p.remove); buf.writeBlockPos(p.pos); buf.writeEnum(p.face); buf.writeNbt(p.data); buf.writeBoolean(p.clear);
    }, buf -> new SealSyncPayload(buf.readBoolean(), buf.readBlockPos(), buf.readEnum(Direction.class), buf.readNbt(), buf.readBoolean()));
    public static SealSyncPayload remove(SealPos sp) { return new SealSyncPayload(true, sp.pos, sp.face, new CompoundTag(), false); }
    public static SealSyncPayload clearAll() { return new SealSyncPayload(false, BlockPos.ZERO, Direction.DOWN, new CompoundTag(), true); }
    @Override public Type<SealSyncPayload> type() { return TYPE; }
}
