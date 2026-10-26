package art.arcane.thaumcraft.casters.ward;

import art.arcane.thaumcraft.registry.ModAttachments;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * TC4 Focus of Warding (ItemFocusWarding / BlockWarded / TileWarded): a warded block cannot be broken, changed,
 * exploded or pushed by anyone but the player who warded it. The ward is per chunk (attachment, synced to clients
 * for the goggles overlay); the owner removes it by casting Warding on the block again, or by breaking it.
 */
public final class WardManager {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(WardManager::onBreak);
        NeoForge.EVENT_BUS.addListener(WardManager::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(WardManager::onInteract);
        NeoForge.EVENT_BUS.addListener(WardManager::onExplode);
        NeoForge.EVENT_BUS.addListener(WardManager::onPiston);
        NeoForge.EVENT_BUS.addListener(WardManager::onMobGrief);
    }

    private static WardData data(Level level, BlockPos pos, boolean create) {
        LevelChunk c = level.getChunkAt(pos);
        if (!create && !c.hasData(ModAttachments.WARDS)) return null;
        return c.getData(ModAttachments.WARDS);
    }

    /** Owner of the ward on {@code pos}, or null. A ward on a block that has since become air is dropped. */
    public static UUID owner(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        var d = data(level, pos, false);
        if (d == null) return null;
        UUID o = d.owner(pos.asLong());
        if (o != null && level.getBlockState(pos).isAir()) { if (!level.isClientSide()) unward(level, pos); return null; }
        return o;
    }
    public static boolean isWarded(Level level, BlockPos pos) { return owner(level, pos) != null; }

    /** TC4 BlockWarded accepts solid, non-air blocks without a block entity of their own being required. */
    public static boolean canWard(Level level, BlockPos pos) {
        var s = level.getBlockState(pos);
        return !s.isAir() && s.getFluidState().isEmpty() && s.getDestroySpeed(level, pos) >= 0;
    }

    public static boolean ward(Level level, BlockPos pos, UUID owner) {
        if (!canWard(level, pos) || isWarded(level, pos)) return false;
        LevelChunk c = level.getChunkAt(pos);
        var d = c.getData(ModAttachments.WARDS);
        d.put(pos.asLong(), owner);
        c.setData(ModAttachments.WARDS, d);
        c.markUnsaved();
        return true;
    }

    public static boolean unward(Level level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        if (!c.hasData(ModAttachments.WARDS)) return false;
        var d = c.getData(ModAttachments.WARDS);
        if (d.remove(pos.asLong()) == null) return false;
        c.setData(ModAttachments.WARDS, d);
        c.markUnsaved();
        return true;
    }

    /** True when {@code p} may change the block: not warded, or warded by {@code p}. */
    public static boolean mayEdit(Level level, BlockPos pos, Player p) {
        UUID o = owner(level, pos);
        return o == null || (p != null && o.equals(p.getUUID()));
    }

    private static void onBreak(BreakBlockEvent e) {
        if (!(e.getLevel() instanceof Level level)) return;
        if (!mayEdit(level, e.getPos(), e.getPlayer())) e.setCanceled(true);
        else unward(level, e.getPos());
    }
    private static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        var pos = e.getPosition().orElse(null);
        if (pos != null && !mayEdit(e.getEntity().level(), pos, e.getEntity())) e.setNewSpeed(0);
    }
    private static void onInteract(PlayerInteractEvent.RightClickBlock e) {
        if (!mayEdit(e.getLevel(), e.getPos(), e.getEntity())) e.setCanceled(true);
    }
    private static void onExplode(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(p -> isWarded(e.getLevel(), p));
    }
    private static void onPiston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof Level level)) return;
        var h = e.getStructureHelper();
        if (h == null) return;
        h.resolve();
        for (var p : h.getToPush()) if (isWarded(level, p)) { e.setCanceled(true); return; }
        for (var p : h.getToDestroy()) if (isWarded(level, p)) { e.setCanceled(true); return; }
    }
    private static void onMobGrief(LivingDestroyBlockEvent e) {
        if (isWarded(e.getEntity().level(), e.getPos())) e.setCanceled(true);
    }

    private WardManager() {}
}
