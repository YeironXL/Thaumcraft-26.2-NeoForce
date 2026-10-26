package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ResearchProgression;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Map;
import net.minecraft.world.item.Item;
import java.util.LinkedHashMap;

public record AdvanceResearchPayload(String key, int stage) implements CustomPacketPayload {
    public static final Type<AdvanceResearchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "advance_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvanceResearchPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), AdvanceResearchPayload::key, ByteBufCodecs.INT, AdvanceResearchPayload::stage, AdvanceResearchPayload::new);
    @Override public Type<AdvanceResearchPayload> type() { return TYPE; }
    public static void handle(AdvanceResearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean hasBook = false;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                if (player.getInventory().getItem(slot).is(ModItems.THAUMONOMICON.get())) { hasBook = true; break; }
            if (!hasBook || payload.key.length() > 128) return;
            boolean advanced = art.arcane.thaumcraft.research.ResearchEvents.withAddendumMessages(player, () -> ResearchProgression.advance(PlayerProgressManager.get(player), payload.key, payload.stage, items -> consume(player, items)));
            // TC6 PacketSyncProgressToServer: no experience reward; completed research shows the popup toast.
            if (advanced) {
                if (art.arcane.thaumcraft.research.ResearchRules.complete(PlayerProgressManager.get(player), payload.key))
                    art.arcane.thaumcraft.research.ResearchNotifications.complete(player, payload.key);
            } else player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("screen.thaumcraft.requirements_unmet"));
            PacketDistributor.sendToPlayer(player, ResearchSnapshotPayload.of(player, false));
        });
    }
    static boolean consume(ServerPlayer player, Map<String, Integer> requirements) {
        return art.arcane.thaumcraft.research.ResearchItemCosts.consume(player.getInventory(),requirements);
    }
}