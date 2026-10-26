package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import art.arcane.thaumcraft.network.SealSyncPayload;
import art.arcane.thaumcraft.registry.ModAttachments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * TC6 SealHandler: seal types, the placed seals of every dimension and their ticking. Seals are saved with the chunk
 * they sit in (a chunk attachment, like TC6's chunk NBT) and mirrored to clients in a separate client-side map.
 */
public final class SealHandler {
    /** Registered seal types in TC6 order: key -> factory. */
    public static final LinkedHashMap<String, Supplier<ISeal>> types = new LinkedHashMap<>();
    /** key -> placer item. */
    public static final Map<String, Supplier<? extends Item>> items = new LinkedHashMap<>();
    public static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<SealPos, SealEntity>> sealEntities = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<SealPos, SealEntity>> clientSeals = new ConcurrentHashMap<>();
    static int count = 0;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public static void registerSeal(Supplier<ISeal> factory, Supplier<? extends Item> item) {
        String key = factory.get().getKey();
        if (types.containsKey(key)) return;
        types.put(key, factory);
        items.put(key, item);
    }
    public static String[] getRegisteredSeals() { return types.keySet().toArray(new String[0]); }
    public static @Nullable ISeal newSeal(String key) { var f = types.get(key); return f == null ? null : f.get(); }
    public static ItemStack getSealStack(String key) { var i = items.get(key); return i == null ? ItemStack.EMPTY : new ItemStack(i.get()); }

    public static ConcurrentHashMap<SealPos, SealEntity> seals(Level level) {
        return (level.isClientSide() ? clientSeals : sealEntities).computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
    }

    public static List<SealEntity> getSealsInRange(Level world, BlockPos source, int range) {
        List<SealEntity> out = new ArrayList<>();
        for (SealEntity se : seals(world).values()) if (se.getSeal() != null && se.getSealPos() != null && se.sealPos.pos.distSqr(source) <= range * range) out.add(se);
        return out;
    }
    public static List<SealEntity> getSealsInChunk(Level world, ChunkPos chunk) {
        List<SealEntity> out = new ArrayList<>();
        for (SealEntity se : seals(world).values()) if (se.getSeal() != null && se.getSealPos() != null && ChunkPos.containing(se.sealPos.pos).equals(chunk)) out.add(se);
        return out;
    }

    public static @Nullable ISealEntity getSealEntity(Level level, @Nullable SealPos pos) {
        if (pos == null) return null;
        return seals(level).get(pos);
    }

    public static void removeSealEntity(Level world, SealPos pos, boolean quiet) {
        SealEntity seal = seals(world).remove(pos);
        try {
            if (!world.isClientSide() && seal != null && seal.seal != null) seal.seal.onRemoval(world, pos.pos, pos.face);
            if (!quiet && seal != null && !world.isClientSide()) {
                ItemStack drop = getSealStack(seal.getSeal().getKey());
                if (!drop.isEmpty())
                    world.addFreshEntity(new ItemEntity(world, pos.pos.getX() + 0.5 + pos.face.getStepX() / 1.7f, pos.pos.getY() + 0.5 + pos.face.getStepY() / 1.7f, pos.pos.getZ() + 0.5 + pos.face.getStepZ() / 1.7f, drop));
            }
        } catch (Exception e) {
            LOG.warn("Removing invalid seal at {}", pos.pos);
        }
        if (!world.isClientSide()) {
            for (Task task : TaskHandler.getTasks(world).values()) if (task.getSealPos() != null && task.getSealPos().equals(pos)) task.setSuspended(true);
            if (world instanceof ServerLevel sl) PacketDistributor.sendToPlayersInDimension(sl, SealSyncPayload.remove(pos));
            if (!quiet) markChunkAsDirty(world, pos.pos);
        }
    }

    public static boolean addSealEntity(Level world, BlockPos pos, Direction face, ISeal seal, Player player) {
        var se = seals(world);
        SealPos sp = new SealPos(pos, face);
        if (se.containsKey(sp)) return false;
        SealEntity ent = new SealEntity(sp, seal);
        ent.setOwner(player.getUUID().toString());
        se.put(sp, ent);
        if (!world.isClientSide()) { ent.syncToClient(world); markChunkAsDirty(world, pos); }
        return true;
    }
    public static boolean addSealEntity(Level world, SealEntity seal) {
        if (world == null || seal.getSeal() == null) return false;
        var se = seals(world);
        if (se.containsKey(seal.getSealPos())) return false;
        se.put(seal.getSealPos(), seal);
        if (!world.isClientSide()) seal.syncToClient(world);
        return true;
    }

    public static void tickSealEntities(ServerLevel world) {
        ++count;
        for (SealEntity se : seals(world).values()) {
            if (!world.isLoaded(se.sealPos.pos)) continue;
            try {
                if (count % 20 == 0 && !se.seal.canPlaceAt(world, se.sealPos.pos, se.sealPos.face)) { removeSealEntity(world, se.sealPos, false); continue; }
                se.tickSealEntity(world);
            } catch (Exception e) {
                LOG.warn("Seal at {} failed to tick, removing it", se.sealPos.pos, e);
                removeSealEntity(world, se.sealPos, false);
            }
        }
    }

    // ---- persistence (TC6 ChunkEvents: seals live in the chunk's "Thaumcraft.seals" list)
    /** markChunkAsDirty: rewrite the chunk's seal list. */
    public static void markChunkAsDirty(Level world, BlockPos bp) {
        if (!(world instanceof ServerLevel sl) || !sl.hasChunkAt(bp)) return;
        LevelChunk chunk = sl.getChunkAt(bp);
        List<CompoundTag> list = new ArrayList<>();
        for (SealEntity se : getSealsInChunk(sl, chunk.getPos())) list.add(se.writeNBT(sl.registryAccess()));
        if (list.isEmpty()) chunk.removeData(ModAttachments.SEALS.get());
        else chunk.setData(ModAttachments.SEALS.get(), list);
        chunk.markUnsaved();
    }
    /** Changed settings of a seal: persist and resend. */
    public static void changed(Level world, SealEntity seal) {
        if (world.isClientSide()) return;
        seal.syncToClient(world);
        markChunkAsDirty(world, seal.getSealPos().pos);
    }
    public static void chunkLoaded(ServerLevel level, LevelChunk chunk) {
        if (!chunk.hasData(ModAttachments.SEALS.get())) return;
        for (CompoundTag tag : chunk.getData(ModAttachments.SEALS.get())) {
            SealEntity seal = new SealEntity();
            seal.readNBT(tag, level.registryAccess());
            if (seal.getSeal() != null) addSealEntity(level, seal);
        }
    }
    public static void chunkUnloaded(ServerLevel level, LevelChunk chunk) {
        for (SealEntity se : getSealsInChunk(level, chunk.getPos())) seals(level).remove(se.getSealPos());
    }

    // ---- client sync (TC6 PacketSealToClient)
    public static void syncToClient(Level world, SealEntity seal) {
        if (world instanceof ServerLevel sl) PacketDistributor.sendToPlayersInDimension(sl, new SealSyncPayload(false, seal.getSealPos().pos, seal.getSealPos().face, seal.writeNBT(sl.registryAccess())));
    }
    public static void syncAllTo(ServerPlayer player) {
        if (player.connection == null) return;
        var level = player.level();
        PacketDistributor.sendToPlayer(player, SealSyncPayload.clearAll());
        for (SealEntity se : seals(level).values()) PacketDistributor.sendToPlayer(player, new SealSyncPayload(false, se.getSealPos().pos, se.getSealPos().face, se.writeNBT(level.registryAccess())));
    }
    /** Client side of SealSyncPayload. */
    public static void receive(Level clientLevel, SealSyncPayload p) {
        var map = seals(clientLevel);
        if (p.clear()) { map.clear(); return; }
        SealPos sp = new SealPos(p.pos(), p.face());
        if (p.remove()) { map.remove(sp); return; }
        SealEntity existing = map.get(sp);
        SealEntity fresh = new SealEntity();
        fresh.readNBT(p.data(), clientLevel.registryAccess());
        if (fresh.getSeal() == null) return;
        if (existing != null && existing.getSeal() != null && existing.getSeal().getKey().equals(fresh.getSeal().getKey())) {
            // keep the instance an open GUI is looking at
            existing.readNBT(p.data(), clientLevel.registryAccess());
        } else map.put(sp, fresh);
    }
    private SealHandler() {}
}
