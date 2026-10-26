package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** TC6 ServerEvents/ChunkEvents golem parts: seal ticking, task expiry, seal load/unload with chunks and client sync. */
public final class GolemEvents {
    private static final Map<ResourceKey<Level>, Integer> ticks = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity.onOwnerTravel(sp);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity.onOwnerArrived(sp);
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (!(e.getLevel() instanceof ServerLevel sl)) return;
            int t = ticks.merge(sl.dimension(), 1, Integer::sum);
            if (t % 20 == 0) TaskHandler.clearSuspendedOrExpiredTasks(sl);
            SealHandler.tickSealEntities(sl);
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getChunk() instanceof LevelChunk c) SealHandler.chunkLoaded(sl, c);
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getChunk() instanceof LevelChunk c) SealHandler.chunkUnloaded(sl, c);
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof Level l) {
                (l.isClientSide() ? SealHandler.clientSeals : SealHandler.sealEntities).remove(l.dimension());
                if (!l.isClientSide()) { TaskHandler.tasks.remove(l.dimension()); GolemHelper.provisionRequests.remove(l.dimension()); }
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) SealHandler.syncAllTo(sp); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) SealHandler.syncAllTo(sp); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer sp) SealHandler.syncAllTo(sp); });
    }
    private GolemEvents() {}
}
