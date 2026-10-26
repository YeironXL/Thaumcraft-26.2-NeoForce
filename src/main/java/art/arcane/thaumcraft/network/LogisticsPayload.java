package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.logistics.LogisticsMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** PacketLogisticsRequestToServer (stack + amount, delivered to the block face or the player) and PacketMiscStringToServer (search text). */
public record LogisticsPayload(Optional<BlockPos> pos, Optional<Direction> side, ItemStack stack, int size, String search) implements CustomPacketPayload {
    public static final Type<LogisticsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "logistics"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LogisticsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LogisticsPayload::pos, ByteBufCodecs.optional(Direction.STREAM_CODEC), LogisticsPayload::side,
            ItemStack.OPTIONAL_STREAM_CODEC, LogisticsPayload::stack, ByteBufCodecs.VAR_INT, LogisticsPayload::size, ByteBufCodecs.stringUtf8(32), LogisticsPayload::search, LogisticsPayload::new);
    @Override public Type<LogisticsPayload> type() { return TYPE; }
    public static void handle(LogisticsPayload m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            if (m.stack.isEmpty()) {
                if (p.containerMenu instanceof LogisticsMenu lm) { lm.searchText = m.search; lm.refreshItemList(true); }
                return;
            }
            int left = Math.min(m.size, 10000), ui = 0;
            while (left > 0) {
                ItemStack s = m.stack.copyWithCount(Math.min(left, m.stack.getMaxStackSize()));
                left -= s.getCount();
                if (m.pos.isPresent() && m.side.isPresent()) GolemHelper.requestProvisioning(p.level(), m.pos.get(), m.side.get(), s, ui);
                else GolemHelper.requestProvisioning(p.level(), p, s, ui);
                ++ui;
            }
        });
    }
}
