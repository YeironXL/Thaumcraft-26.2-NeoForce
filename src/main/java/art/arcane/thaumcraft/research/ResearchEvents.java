package art.arcane.thaumcraft.research;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.server.level.ServerPlayer;

/** Server-side TC6 ResearchManager.progressResearch for triggers (scans, events) plus client synchronisation. */
public final class ResearchEvents {
    /** TC6 progressResearch: unknown keys become known; research entries start (or complete when stage-less). */
    public static boolean progress(ServerPlayer player, String key) {
        var state = PlayerProgressManager.get(player);
        if (ResearchRules.complete(state, key)) return false;
        var entry = ResearchCatalog.entries().get(key);
        if (entry == null) {
            boolean added = state.unlock(key);
            if (added) markDirty(player);
            return added;
        }
        if (!ResearchRules.available(state, entry)) return false;
        int stage = state.stage(key);
        boolean ok = false;
        if (stage <= 0) ok = withAddendumMessages(player, () -> ResearchProgression.advance(state, key, -1, cost -> cost.isEmpty()));
        else if (stage <= entry.stages().size() && entry.stages().get(stage - 1).requirements().isEmpty())
            ok = withAddendumMessages(player, () -> ResearchProgression.advance(state, key, stage, cost -> cost.isEmpty()));
        if (ok) {
            markDirty(player);
            if (ResearchRules.complete(state, key)) ResearchNotifications.complete(player, key);
        }
        return ok;
    }
    /** Runs a progression with TC6's "tc.addaddendum" chat notice routed to this player. */
    public static boolean withAddendumMessages(ServerPlayer player, java.util.function.BooleanSupplier action) {
        var previous = ResearchProgression.addendumListener;
        ResearchProgression.addendumListener = (key, owner) -> {
            var entry = ResearchCatalog.entries().get(owner);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.addaddendum",
                    net.minecraft.network.chat.Component.translatable(entry == null ? owner : entry.name())));
        };
        try { return action.getAsBoolean(); } finally { ResearchProgression.addendumListener = previous; }
    }
    private static final java.util.Set<java.util.UUID> DIRTY = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void markDirty(ServerPlayer player) { DIRTY.add(player.getUUID()); }
    /** Sends the research snapshot if anything changed since the last sync. */
    public static void sync(ServerPlayer player) {
        if (!DIRTY.remove(player.getUUID()) || player.connection == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(player, false));
    }
    private ResearchEvents() {}
}
