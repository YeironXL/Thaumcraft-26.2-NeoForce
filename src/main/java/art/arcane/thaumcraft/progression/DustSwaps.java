package art.arcane.thaumcraft.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Port of TC6 ServerEvents.addSwapper as used by Salis Mundus: 50 ticks after use the clicked block is replaced,
 * but only if it is still the same block state, is not unbreakable and the player may still edit that position.
 * A block result is placed; an item result removes the block and leaves the item floating in its place
 * (TC6 EntitySpecialItem). Visual effects are vanilla stand-ins for TC6 PacketFXBlockBamf.
 */
public final class DustSwaps {
    public static final int DELAY_TICKS = 50;
    private static final List<Entry> PENDING = new ArrayList<>();

    private static final class Entry {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState expected;
        final ServerPlayer player;
        final Block block;
        final ItemStack item;
        int remaining = DELAY_TICKS;
        Entry(ServerLevel level, BlockPos pos, BlockState expected, ServerPlayer player, Block block, ItemStack item) {
            this.level = level; this.pos = pos.immutable(); this.expected = expected; this.player = player; this.block = block; this.item = item;
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DustSwaps::tick);
        // Pending swaps hold ServerLevel/ServerPlayer references: drop them when the server stops so a
        // single-player world reload never applies (or leaks) swaps from the previous world.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> PENDING.clear());
    }

    public static void scheduleBlock(ServerLevel level, BlockPos pos, BlockState expected, ServerPlayer player, Block result) {
        PENDING.add(new Entry(level, pos, expected, player, result, ItemStack.EMPTY));
    }

    public static void scheduleItem(ServerLevel level, BlockPos pos, BlockState expected, ServerPlayer player, ItemStack result) {
        PENDING.add(new Entry(level, pos, expected, player, null, result.copy()));
    }

    private static void tick(ServerTickEvent.Post event) {
        advance(1);
    }

    /** Advances every pending swap by the given number of server ticks (the tick handler uses 1; tests use it directly). */
    public static void advance(int ticks) {
        for (int i = 0; i < ticks && !PENDING.isEmpty(); i++) step();
    }

    public static int pending() {
        return PENDING.size();
    }

    private static void step() {
        for (Iterator<Entry> iterator = PENDING.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            if (--entry.remaining > 0) continue;
            iterator.remove();
            apply(entry);
        }
    }

    private static void apply(Entry entry) {
        ServerLevel level = entry.level;
        BlockState current = level.getBlockState(entry.pos);
        if (entry.player.isRemoved() || entry.player.level() != level || current != entry.expected || current.getDestroySpeed(level, entry.pos) < 0.0F || !entry.player.mayInteract(level, entry.pos)) return;
        // ForgeEventFactory.onPlayerBlockPlace in the original swapper.
        if (net.neoforged.neoforge.event.EventHooks.onBlockPlace(entry.player, net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, entry.pos), net.minecraft.core.Direction.UP)) return;
        if (entry.block != null) {
            level.setBlock(entry.pos, entry.block.defaultBlockState(), 3);
        } else {
            level.removeBlock(entry.pos, false);
            var floating = new art.arcane.thaumcraft.entity.SpecialItemEntity(level, entry.pos.getX() + 0.5, entry.pos.getY() + 0.1, entry.pos.getZ() + 0.5, entry.item.copy());
            floating.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(floating);
        }
        // PacketFXBlockBamf(pos, default colour, sound, fancy).
        art.arcane.thaumcraft.fx.Fx.bamf(level, entry.pos.getX() + 0.5, entry.pos.getY() + 0.5, entry.pos.getZ() + 0.5, true, true, null);
    }

    private DustSwaps() {}
}
