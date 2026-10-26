package art.arcane.thaumcraft.aura;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModAttachments;
import art.arcane.thaumcraft.world.BiomeHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * TC6 AuraHandler + AuraThread on the server thread. Every loaded chunk carries an aura (generated from the
 * biome like AuraHandler.generateAura); once per second vis/flux spread to the neighbours, regenerate with the
 * moon phase and turn into flux exactly as AuraThread.processAuraChunk.
 */
public final class NativeAura {
    private static final Map<ServerLevel, Map<Long, LevelChunk>> LOADED = new WeakHashMap<>();
    private static final Map<ServerLevel, Long> LAST_TIME = new WeakHashMap<>();
    private static final Random RAND = new Random();
    /** AuraHandler.riftTrigger: last chunk origin per level where a flux rift should form. */
    public static final Map<ServerLevel, BlockPos> RIFT_TRIGGER = new WeakHashMap<>();
    private static final float[] PHASE_TABLE = {0.25f, 0.15f, 0.1f, 0.05f, 0.0f, 0.05f, 0.1f, 0.15f};
    private static final float[] MAX_TABLE = {0.15f, 0.05f, 0.0f, -0.05f, -0.15f, -0.05f, 0.0f, 0.05f};

    public static ChunkAura get(ServerLevel level, LevelChunk chunk) {
        var aura = chunk.getData(ModAttachments.AURA.get());
        if (aura.base() < 0) {
            generate(level, chunk, aura);
            chunk.markUnsaved();
        }
        LOADED.computeIfAbsent(level, k -> new HashMap<>()).put(chunk.getPos().pack(), chunk);
        return aura;
    }

    private static void generate(ServerLevel level, LevelChunk chunk, ChunkAura aura) {
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        float life = BiomeHandler.auraModifier(level.getBiome(new BlockPos(cx * 16 + 8, 50, cz * 16 + 8)));
        for (Direction dir : Direction.Plane.HORIZONTAL)
            life += BiomeHandler.auraModifier(level.getBiome(new BlockPos((cx + dir.getStepX()) * 16 + 8, 50, (cz + dir.getStepZ()) * 16 + 8)));
        life /= 5.0f;
        var random = new Random(level.getSeed() ^ ChunkPos.pack(cx, cz));
        float noise = (float) (1.0 + random.nextGaussian() * 0.10000000149011612);
        short base = (short) (life * 500.0f * noise);
        aura.initialize(Math.clamp(base, 0, 500));
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) get(level, chunk);
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var m = LOADED.get(level);
            if (m != null) m.remove(event.getChunk().getPos().pack());
        }
    }

    private static ChunkAura at(ServerLevel level, int cx, int cz) {
        var chunk = level.getChunkSource().getChunkNow(cx, cz);
        return chunk == null ? null : get(level, chunk);
    }

    private static void dirty(ServerLevel level, int cx, int cz) {
        var chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk != null) chunk.markUnsaved();
    }

    // ---- AuraHandler API ----
    public static float vis(ServerLevel l, BlockPos p) { var a = at(l, p.getX() >> 4, p.getZ() >> 4); return a == null ? 0 : a.vis(); }
    public static float flux(ServerLevel l, BlockPos p) { var a = at(l, p.getX() >> 4, p.getZ() >> 4); return a == null ? 0 : a.flux(); }
    public static int base(ServerLevel l, BlockPos p) { var a = at(l, p.getX() >> 4, p.getZ() >> 4); return a == null ? 0 : a.base(); }
    public static float totalAura(ServerLevel l, BlockPos p) { return vis(l, p) + flux(l, p); }
    public static float fluxSaturation(ServerLevel l, BlockPos p) { var a = at(l, p.getX() >> 4, p.getZ() >> 4); return a == null ? 0 : a.flux() / a.base(); }

    public static void addVis(ServerLevel l, BlockPos p, float amount) {
        if (amount < 0) return;
        var a = at(l, p.getX() >> 4, p.getZ() >> 4);
        if (a != null) { a.setVis(a.vis() + amount); dirty(l, p.getX() >> 4, p.getZ() >> 4); }
    }

    public static void addFlux(ServerLevel l, BlockPos p, float amount) {
        if (amount < 0) return;
        var a = at(l, p.getX() >> 4, p.getZ() >> 4);
        if (a != null) { a.setFlux(a.flux() + amount); dirty(l, p.getX() >> 4, p.getZ() >> 4); }
    }

    public static float drainVis(ServerLevel l, BlockPos p, float amount, boolean simulate) {
        var a = at(l, p.getX() >> 4, p.getZ() >> 4);
        if (a == null) return 0;
        if (amount > a.vis()) amount = a.vis();
        if (!simulate) { a.setVis(a.vis() - amount); dirty(l, p.getX() >> 4, p.getZ() >> 4); }
        return amount;
    }

    public static float drainFlux(ServerLevel l, BlockPos p, float amount, boolean simulate) {
        var a = at(l, p.getX() >> 4, p.getZ() >> 4);
        if (a == null) return 0;
        if (amount > a.flux()) amount = a.flux();
        if (!simulate) { a.setFlux(a.flux() - amount); dirty(l, p.getX() >> 4, p.getZ() >> 4); }
        return amount;
    }

    // ---- arcane workbench (TileArcaneWorkbench.getAura / spendAura) ----
    private static boolean charger(ServerLevel level, BlockPos pos) { return level.getBlockState(pos.above()).is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get()); }

    public static int available(ServerLevel level, BlockPos pos) {
        if (!charger(level, pos)) return (int) vis(level, pos);
        int t = 0;
        for (int xx = -1; xx <= 1; ++xx) for (int zz = -1; zz <= 1; ++zz) {
            var a = at(level, (pos.getX() >> 4) + xx, (pos.getZ() >> 4) + zz);
            if (a != null) t += (int) a.vis();
        }
        return t;
    }

    public static boolean spend(ServerLevel level, BlockPos pos, int vis) {
        if (vis < 0 || available(level, pos) < vis) return false;
        if (!charger(level, pos)) { drainVis(level, pos, vis, false); return true; }
        int q = vis, z = Math.max(1, vis / 9), attempts = 0;
        outer:
        while (q > 0) {
            ++attempts;
            for (int xx = -1; xx <= 1; ++xx) for (int zz = -1; zz <= 1; ++zz) {
                if (z > q) z = q;
                q -= (int) drainVis(level, pos.offset(xx * 16, 0, zz * 16), z, false);
                if (q <= 0 || attempts > 1000) break outer;
            }
        }
        return true;
    }

    /** Casters drain vis from their own chunk only. */
    public static boolean spendLocal(ServerLevel level, BlockPos pos, float amount) {
        if (!Float.isFinite(amount) || amount < 0 || vis(level, pos) < amount) return false;
        drainVis(level, pos, amount, false);
        return true;
    }

    // ---- AuraThread ----
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        // ServerEvents.worldTick (every 20 ticks): AuraHandler.riftTrigger -> EntityFluxRift.createRift
        BlockPos rift = RIFT_TRIGGER.remove(level);
        if (rift != null) art.arcane.thaumcraft.entity.FluxRiftEntity.createRift(level, rift);
        long time = level.getGameTime();
        Long last = LAST_TIME.get(level);
        if (last != null && last == time) return; // world time frozen: AuraThread skips
        LAST_TIME.put(level, time);
        var chunks = LOADED.get(level);
        if (chunks == null || chunks.isEmpty()) return;
        int phase = Math.floorMod(art.arcane.thaumcraft.util.TcWorld.moonPhase(level, net.minecraft.world.phys.Vec3.ZERO), 8);
        float phaseVis = PHASE_TABLE[phase], phaseMax = 1.0f + MAX_TABLE[phase], phaseFlux = 0.25f - phaseVis;
        for (var chunk : new ArrayList<>(chunks.values())) process(level, chunk, phaseVis, phaseMax, phaseFlux);
    }

    private static void process(ServerLevel level, LevelChunk chunk, float phaseVis, float phaseMax, float phaseFlux) {
        ChunkAura aura = chunk.getData(ModAttachments.AURA.get());
        if (aura.base() < 0) return;
        List<Direction> dirs = new ArrayList<>(List.of(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST));
        Collections.shuffle(dirs, RAND);
        int x = chunk.getPos().x(), z = chunk.getPos().z();
        float base = aura.base() * phaseMax;
        boolean dirty = false;
        float currentVis = aura.vis(), currentFlux = aura.flux();
        ChunkAura nVis = null, nFlux = null; int nvx = 0, nvz = 0, nfx = 0, nfz = 0;
        float lowestVis = Float.MAX_VALUE, lowestFlux = Float.MAX_VALUE;
        var map = LOADED.get(level);
        for (Direction dir : dirs) {
            var nc = map.get(ChunkPos.pack(x + dir.getStepX(), z + dir.getStepZ()));
            if (nc == null) continue;
            ChunkAura n = nc.getData(ModAttachments.AURA.get());
            if (n.base() < 0) continue;
            if ((nVis == null || lowestVis > n.vis()) && n.vis() + n.flux() < n.base() * phaseMax) {
                nVis = n; lowestVis = n.vis(); nvx = x + dir.getStepX(); nvz = z + dir.getStepZ();
            }
            if (nFlux != null && lowestFlux <= n.flux()) continue;
            nFlux = n; lowestFlux = n.flux(); nfx = x + dir.getStepX(); nfz = z + dir.getStepZ();
        }
        if (nVis != null && lowestVis < currentVis && lowestVis / currentVis < 0.75) {
            float inc = Math.min(currentVis - lowestVis, 1.0f);
            currentVis -= inc; nVis.setVis(lowestVis + inc); dirty = true; dirty(level, nvx, nvz);
        }
        if (nFlux != null && currentFlux > Math.max(5.0f, aura.base() / 10.0f) && lowestFlux < currentFlux / 1.75) {
            float inc = Math.min(currentFlux - lowestFlux, 1.0f);
            currentFlux -= inc; nFlux.setFlux(lowestFlux + inc); dirty = true; dirty(level, nfx, nfz);
        }
        if (currentVis + currentFlux < base) {
            currentVis += Math.min(base - (currentVis + currentFlux), phaseVis); dirty = true;
        } else if (currentVis > base * 1.25 && RAND.nextFloat() < 0.1) {
            currentFlux += phaseFlux; currentVis -= phaseFlux; dirty = true;
        } else if (currentVis <= base * 0.1 && currentVis >= currentFlux && RAND.nextFloat() < 0.1) {
            currentFlux += phaseFlux; dirty = true;
        }
        if (dirty) { aura.setVis(currentVis); aura.setFlux(currentFlux); chunk.markUnsaved(); }
        if (currentFlux > base * 0.75 && RAND.nextFloat() < currentFlux / 500.0f / 10.0f)
            RIFT_TRIGGER.put(level, new BlockPos(x * 16, 0, z * 16));
    }

    private NativeAura() {}
}
