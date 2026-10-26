package art.arcane.thaumcraft.essentia;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.fx.Fx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * TC6 lib.events.EssentiaHandler: devices pulling essentia straight out of nearby IAspectSources (warded jars) without
 * tubes. Sources are cached per device, nearest first; a failed scan or an exhausted list is retried after 10 seconds.
 * Each transfer sends PacketFXEssentiaSource (an essentia stream from the source to the device).
 */
public final class EssentiaHandler {
    private record Key(ResourceKey<Level> dim, long pos) {}
    private static final Map<Key, List<BlockPos>> SOURCES = new HashMap<>();
    private static final Map<Key, Long> SOURCES_DELAY = new HashMap<>();

    /** IAspectSource: TC6 TileJarFillable (normal and void jars). The alembic shares the jar block entity here but was not a source in TC6. */
    private static EssentiaJarBlockEntity source(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar && !jar.alembic() ? jar : null;
    }
    /** IAspectSource: jars and (TC6 TileMirrorEssentia) essentia mirrors. */
    private static boolean isSource(ServerLevel level, BlockPos pos) {
        return source(level, pos) != null || level.getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity;
    }

    public static boolean drainEssentia(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range, int ext) {
        return drainEssentia(level, tile, aspect, direction, range, false, ext);
    }

    public static boolean drainEssentia(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        Key key = new Key(level.dimension(), tile.asLong());
        if (!SOURCES.containsKey(key)) {
            getSources(level, key, tile, direction, range);
            return SOURCES.containsKey(key) && drainEssentia(level, tile, aspect, direction, range, ignoreMirror, ext);
        }
        for (BlockPos src : SOURCES.get(key)) {
            if (level.getBlockEntity(src) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity mirror) {
                if (ignoreMirror) continue;
                if (mirror.takeFromContainer(aspect, 1)) {
                    Fx.send(level, "essentia_source", tile.getX(), tile.getY(), tile.getZ(), tile.getX() - src.getX(), tile.getY() - src.getY(), tile.getZ() - src.getZ(), AspectRegistry.color(aspect), ext);
                    return true;
                }
                continue;
            }
            var as = source(level, src);
            if (as == null) break;
            if (as.blocked()) continue;
            if (as.aspect().equals(aspect) && as.extract(1)) {
                Fx.send(level, "essentia_source", tile.getX(), tile.getY(), tile.getZ(), tile.getX() - src.getX(), tile.getY() - src.getY(), tile.getZ() - src.getZ(), AspectRegistry.color(aspect), ext);
                return true;
            }
        }
        SOURCES.remove(key);
        SOURCES_DELAY.put(key, System.currentTimeMillis() + 10000L);
        return false;
    }

    /** drainEssentiaWithConfirmation: the first source that holds the aspect, without taking it yet (null if none). */
    public static BlockPos drainSource(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range, boolean ignoreMirror) {
        Key key = new Key(level.dimension(), tile.asLong());
        if (!SOURCES.containsKey(key)) {
            getSources(level, key, tile, direction, range);
            return SOURCES.containsKey(key) ? drainSource(level, tile, aspect, direction, range, ignoreMirror) : null;
        }
        for (BlockPos src : SOURCES.get(key)) {
            if (level.getBlockEntity(src) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity mirror) {
                if (!ignoreMirror && mirror.doesContainerContainAmount(aspect, 1)) return src;
                continue;
            }
            var as = source(level, src);
            if (as == null) break;
            if (!as.blocked() && as.aspect().equals(aspect) && as.contents().amount() >= 1) return src;
        }
        SOURCES.remove(key);
        SOURCES_DELAY.put(key, System.currentTimeMillis() + 10000L);
        return null;
    }

    /** confirmDrain: take one point from the source found by {@link #drainSource} and show the essentia stream. */
    public static void confirmDrain(ServerLevel level, BlockPos tile, BlockPos src, String aspect, int ext) {
        boolean took = level.getBlockEntity(src) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity mirror ? mirror.takeFromContainer(aspect, 1)
                : source(level, src) instanceof EssentiaJarBlockEntity jar && jar.aspect().equals(aspect) && jar.extract(1);
        if (took) Fx.send(level, "essentia_source", tile.getX(), tile.getY(), tile.getZ(), tile.getX() - src.getX(), tile.getY() - src.getY(), tile.getZ() - src.getZ(), AspectRegistry.color(aspect), ext);
    }

    /**
     * addEssentia: put one point into the nearest jar already holding that aspect (a full void jar swallows it), else
     * into the nearest empty jar that accepts it. Essentia mirrors never accept.
     */
    public static boolean addEssentia(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range, int ext) {
        Key key = new Key(level.dimension(), tile.asLong());
        if (!SOURCES.containsKey(key)) {
            getSources(level, key, tile, direction, range);
            return SOURCES.containsKey(key) && addEssentia(level, tile, aspect, direction, range, ext);
        }
        List<BlockPos> empties = new ArrayList<>();
        for (BlockPos src : SOURCES.get(key)) {
            if (level.getBlockEntity(src) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity) continue;
            var as = source(level, src);
            if (as == null) break;
            if (as.blocked() || (!as.filter().isEmpty() && !as.filter().equals(aspect))) continue;
            if (as.contents().amount() == 0) { empties.add(src); continue; }
            if (!as.aspect().equals(aspect)) continue;
            boolean voidJar = level.getBlockState(src).is(art.arcane.thaumcraft.ModBlocks.JAR_VOID.get());
            if (as.insert(aspect, 1) || voidJar) { stream(level, tile, src, aspect, ext); return true; }
        }
        for (BlockPos src : empties) {
            var as = source(level, src);
            if (as == null) break;
            if (as.insert(aspect, 1)) { stream(level, tile, src, aspect, ext); return true; }
        }
        SOURCES.remove(key);
        SOURCES_DELAY.put(key, System.currentTimeMillis() + 10000L);
        return false;
    }

    private static void stream(ServerLevel level, BlockPos tile, BlockPos src, String aspect, int ext) {
        Fx.send(level, "essentia_source", src.getX(), src.getY(), src.getZ(), src.getX() - tile.getX(), src.getY() - tile.getY(), src.getZ() - tile.getZ(), AspectRegistry.color(aspect), ext);
    }

    /** findEssentia. */
    public static boolean findEssentia(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range) {
        return findEssentia(level, tile, aspect, direction, range, false);
    }

    public static boolean findEssentia(ServerLevel level, BlockPos tile, String aspect, Direction direction, int range, boolean ignoreMirror) {
        Key key = new Key(level.dimension(), tile.asLong());
        if (!SOURCES.containsKey(key)) {
            getSources(level, key, tile, direction, range);
            return SOURCES.containsKey(key) && findEssentia(level, tile, aspect, direction, range, ignoreMirror);
        }
        for (BlockPos src : SOURCES.get(key)) {
            if (level.getBlockEntity(src) instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity mirror) {
                if (!ignoreMirror && mirror.doesContainerContainAmount(aspect, 1)) return true;
                continue;
            }
            var as = source(level, src);
            if (as == null) break;
            if (!as.blocked() && as.aspect().equals(aspect) && as.contents().amount() >= 1) return true;
        }
        return false;
    }

    public static void refreshSources(Level level, BlockPos tile) { SOURCES.remove(new Key(level.dimension(), tile.asLong())); }

    private static void getSources(ServerLevel level, Key key, BlockPos tile, Direction direction, int range) {
        Long d = SOURCES_DELAY.get(key);
        if (d != null) {
            if (d > System.currentTimeMillis()) return;
            SOURCES_DELAY.remove(key);
        }
        int start = 0;
        if (direction == null) { start = -range; direction = Direction.UP; }
        List<BlockPos> found = new ArrayList<>();
        for (int aa = -range; aa <= range; ++aa) for (int bb = -range; bb <= range; ++bb) for (int cc = start; cc < range; ++cc) {
            if (aa == 0 && bb == 0 && cc == 0) continue;
            int xx = tile.getX(), yy = tile.getY(), zz = tile.getZ();
            if (direction.getStepY() != 0) { xx += aa; yy += cc * direction.getStepY(); zz += bb; }
            else if (direction.getStepX() == 0) { xx += aa; yy += bb; zz += cc * direction.getStepZ(); }
            else { xx += cc * direction.getStepX(); yy += aa; zz += bb; }
            BlockPos p = new BlockPos(xx, yy, zz);
            if (level.isLoaded(p) && isSource(level, p)) found.add(p);
        }
        if (found.isEmpty()) { SOURCES_DELAY.put(key, System.currentTimeMillis() + 10000L); return; }
        // Insertion sort by distance, keeping scan order among equals (TC6 sourceList2).
        List<BlockPos> sorted = new ArrayList<>();
        outer:
        for (BlockPos wc : found) {
            double dist = wc.distSqr(tile);
            for (int a = 0; a < sorted.size(); ++a) if (dist < sorted.get(a).distSqr(tile)) { sorted.add(a, wc); continue outer; }
            sorted.add(wc);
        }
        SOURCES.put(key, sorted);
    }

    /** Tests: forget cached sources and retry delays. */
    public static void clear() { SOURCES.clear(); SOURCES_DELAY.clear(); }

    private EssentiaHandler() {}
}
