package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.research.ResearchEvents;
import art.arcane.thaumcraft.research.ResearchProgression;
import art.arcane.thaumcraft.research.ResearchRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Research browser requests from TC6's GuiResearchBrowser:
 * START = PacketSyncProgressToServer(key, true) (first=true, noFlags),
 * CLEAR = PacketSyncResearchFlagsToServer after clearing RESEARCH and PAGE,
 * FINISH = PacketSyncProgressToServer(key, false, true, false) (checks, flags).
 */
public record ResearchFlagsPayload(String key, int action) implements CustomPacketPayload {
    public static final int START = 0, CLEAR = 1, FINISH = 2;
    public static final Type<ResearchFlagsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "research_flags"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchFlagsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), ResearchFlagsPayload::key, ByteBufCodecs.VAR_INT, ResearchFlagsPayload::action, ResearchFlagsPayload::new);
    @Override public Type<ResearchFlagsPayload> type() { return TYPE; }

    public static void handle(ResearchFlagsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || payload.key.length() > 128) return;
            PlayerResearch state = PlayerProgressManager.get(player);
            String key = payload.key;
            switch (payload.action) {
                case START -> {
                    // message.first != knowsResearch: only research the player does not know yet.
                    if (state.has(key) || ResearchCatalog.entries().get(key) == null) return;
                    ResearchProgression.noFlags = true;
                    boolean ok;
                    try { ok = ResearchEvents.withAddendumMessages(player, () -> ResearchProgression.advance(state, key, state.stage(key), cost -> cost.isEmpty())); }
                    finally { ResearchProgression.noFlags = false; }
                    if (ok && ResearchRules.complete(state, key)) art.arcane.thaumcraft.research.ResearchNotifications.complete(player, key);
                }
                case CLEAR -> {
                    state.clearFlag(key, PlayerResearch.FLAG_RESEARCH);
                    state.clearFlag(key, PlayerResearch.FLAG_PAGE);
                }
                case FINISH -> {
                    if (!state.has(key)) return;
                    boolean ok = ResearchEvents.withAddendumMessages(player, () -> ResearchProgression.advance(state, key, state.stage(key), items -> AdvanceResearchPayload.consume(player, items)));
                    if (ok && ResearchRules.complete(state, key)) art.arcane.thaumcraft.research.ResearchNotifications.complete(player, key);
                }
                default -> { return; }
            }
            PacketDistributor.sendToPlayer(player, ResearchSnapshotPayload.of(player, false));
        });
    }
}
