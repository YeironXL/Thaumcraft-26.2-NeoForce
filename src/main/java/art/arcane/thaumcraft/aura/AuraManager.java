package art.arcane.thaumcraft.aura;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import art.arcane.thaumcraft.aspect.Aspect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Chunk-keyed aura service. The persistence hook will be connected to SavedData later. */
public final class AuraManager {
    private final Map<Long, AuraChunk> chunks = new ConcurrentHashMap<>();

    public AuraChunk get(ChunkPos pos) {
        return chunks.computeIfAbsent(ChunkPos.pack(pos.x(), pos.z()), key -> new AuraChunk());
    }

    public AuraChunk get(BlockPos pos) {
        return get(ChunkPos.containing(pos));
    }

    public void addVis(ChunkPos pos, Aspect aspect, int amount) {
        get(pos).addVis(aspect, amount);
    }

    public void addFlux(ChunkPos pos, int amount) {
        get(pos).addFlux(amount);
    }
}
