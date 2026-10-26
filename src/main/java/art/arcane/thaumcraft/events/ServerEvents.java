package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.entity.SpecialItemEntity;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.util.BlockUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** TC6 common.lib.events.ServerEvents: delayed server runnables, virtual block breakers and swappers. */
public final class ServerEvents {
    public record SwapperPredicate(ServerLevel world, Player player, BlockPos pos) {}
    public static final Predicate<SwapperPredicate> DEFAULT_PREDICATE = p -> true;

    private static final class BreakData {
        final float strength, durabilityMax; float durabilityCurrent; final BlockState source; final BlockPos pos; final Player player;
        final boolean fx, silk; final int fortune; final float visCost;
        BreakData(float strength, float cur, float max, BlockPos pos, BlockState source, Player player, boolean fx, boolean silk, int fortune, float vis) {
            this.strength = strength; this.durabilityCurrent = cur; this.durabilityMax = max; this.pos = pos; this.source = source; this.player = player;
            this.fx = fx; this.silk = silk; this.fortune = fortune; this.visCost = vis;
        }
    }

    /** source: a BlockState (exact match), or null for anything. */
    private record VirtualSwapper(BlockPos pos, Object source, ItemStack target, boolean consumeTarget, int lifespan, Player player, boolean fx, boolean fancy,
                                  int color, boolean pickup, boolean silk, int fortune, Predicate<SwapperPredicate> allowSwap, float visCost, BlockState targetState) {}

    private record RunnableEntry(Runnable runnable, int[] delay) {}

    private static final Map<ServerLevel, Deque<BreakData>> breakList = new WeakHashMap<>();
    private static final Map<ServerLevel, Deque<VirtualSwapper>> swapList = new WeakHashMap<>();
    private static final Map<ServerLevel, List<RunnableEntry>> runList = new WeakHashMap<>();

    public static void register() { NeoForge.EVENT_BUS.addListener(ServerEvents::tick); }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        var rl = runList.get(world);
        if (rl != null && !rl.isEmpty()) {
            for (var e : new ArrayList<>(rl)) {
                if (e.delay[0] > 0) e.delay[0]--;
                else { rl.remove(e); try { e.runnable.run(); } catch (Exception ignored) {} }
            }
        }
        tickBlockSwap(world);
        tickBlockBreak(world);
    }

    @SuppressWarnings("unchecked")
    private static void tickBlockSwap(ServerLevel world) {
        var queue = swapList.get(world);
        if (queue == null || queue.isEmpty()) return;
        Deque<VirtualSwapper> queue2 = new ArrayDeque<>();
        while (!queue.isEmpty()) {
            VirtualSwapper vs = queue.poll();
            BlockState bs = world.getBlockState(vs.pos);
            boolean allow = bs.getDestroySpeed(world, vs.pos) >= 0.0f;
            if (vs.source instanceof BlockState s && s != bs) allow = false;
            if (vs.source instanceof Predicate<?> m && !((Predicate<BlockState>) m).test(bs)) allow = false;
            if (vs.visCost > 0 && NativeAura.vis(world, vs.pos) < vs.visCost) allow = false;
            if (!world.mayInteract(vs.player, vs.pos) || !allow) continue;
            if (vs.target != null && !vs.target.isEmpty() && vs.target.getItem() instanceof BlockItem bi && bi.getBlock() == bs.getBlock()) continue;
            if (!vs.allowSwap.test(new SwapperPredicate(world, vs.player, vs.pos))) continue;
            int slot;
            if (!vs.consumeTarget || vs.target == null || vs.target.isEmpty()) slot = 1;
            else slot = slotFor(vs.player, vs.target);
            if (vs.player.isCreative()) slot = 1;
            if ((vs.source instanceof BlockState && vs.source != bs) || slot < 0) continue;
            if (!vs.player.isCreative()) {
                if (vs.consumeTarget) vs.player.getInventory().removeItem(slot, 1);
                if (vs.pickup) {
                    List<ItemStack> ret;
                    var tool = vs.player.getMainHandItem().copy();
                    var enchants = world.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                    if (vs.silk) { tool = new ItemStack(net.minecraft.world.item.Items.STICK); tool.enchant(enchants.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1); }
                    else if (vs.fortune > 0) { tool = new ItemStack(net.minecraft.world.item.Items.STICK); tool.enchant(enchants.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), vs.fortune); }
                    ret = net.minecraft.world.level.block.Block.getDrops(bs, world, vs.pos, world.getBlockEntity(vs.pos), vs.player, tool);
                    for (ItemStack is : ret) if (!vs.player.getInventory().add(is))
                        world.addFreshEntity(new ItemEntity(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, is));
                }
                if (vs.visCost > 0) NativeAura.drainVis(world, vs.pos, vs.visCost, false);
            }
            if (vs.targetState != null) world.setBlock(vs.pos, vs.targetState, 3);
            else if (vs.target == null || vs.target.isEmpty()) world.removeBlock(vs.pos, false);
            else if (vs.target.getItem() instanceof BlockItem bi && bi.getBlock() != Blocks.AIR) world.setBlock(vs.pos, bi.getBlock().defaultBlockState(), 3);
            else {
                world.removeBlock(vs.pos, false);
                var e = new SpecialItemEntity(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.1, vs.pos.getZ() + 0.5, vs.target.copy());
                e.setDeltaMovement(0, 0, 0);
                world.addFreshEntity(e);
            }
            if (vs.fx) {
                if (vs.color == -9999) Fx.bamf(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, true, vs.fancy, null);
                else Fx.bamf(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, vs.color, true, vs.fancy, null);
            }
            if (vs.lifespan <= 0) continue;
            for (int xx = -1; xx <= 1; ++xx) for (int yy = -1; yy <= 1; ++yy) for (int zz = -1; zz <= 1; ++zz) {
                BlockPos np = vs.pos.offset(xx, yy, zz);
                boolean matches = vs.source instanceof BlockState s && world.getBlockState(np) == s;
                if ((xx != 0 || yy != 0 || zz != 0) && matches && BlockUtils.isBlockExposed(world, np))
                    queue2.offer(new VirtualSwapper(np, vs.source, vs.target, vs.consumeTarget, vs.lifespan - 1, vs.player, vs.fx, vs.fancy, vs.color, vs.pickup, vs.silk, vs.fortune, vs.allowSwap, vs.visCost, vs.targetState));
            }
        }
        swapList.put(world, queue2);
    }

    private static int slotFor(Player p, ItemStack target) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (ItemStack.isSameItemSameComponents(inv.getItem(i), target)) return i;
        return -1;
    }

    private static void tickBlockBreak(ServerLevel world) {
        var queue = breakList.get(world);
        if (queue == null || queue.isEmpty()) return;
        Deque<BreakData> queue2 = new ArrayDeque<>();
        while (!queue.isEmpty()) {
            BreakData vs = queue.poll();
            BlockState bs = world.getBlockState(vs.pos);
            if (bs == vs.source) {
                if (vs.visCost > 0 && NativeAura.vis(world, vs.pos) < vs.visCost) continue;
                if (!world.mayInteract(vs.player, vs.pos) || bs.getDestroySpeed(world, vs.pos) < 0) continue;
                if (vs.fx) world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, (int) ((1.0f - vs.durabilityCurrent / vs.durabilityMax) * 10.0f));
                vs.durabilityCurrent -= vs.strength;
                if (vs.durabilityCurrent <= 0) {
                    BlockUtils.harvestBlock(world, vs.player, vs.pos, true, vs.silk, vs.fortune, false);
                    if (vs.fx) world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
                    if (vs.visCost > 0) NativeAura.drainVis(world, vs.pos, vs.visCost, false);
                } else queue2.offer(vs);
            } else if (vs.fx) world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
        }
        breakList.put(world, queue2);
    }

    public static void addSwapper(ServerLevel world, BlockPos pos, Object source, ItemStack target, boolean consumeTarget, int life, Player player, boolean fx, boolean fancy,
                                  int color, boolean pickup, boolean silk, int fortune, Predicate<SwapperPredicate> allowSwap, float visCost) {
        swapList.computeIfAbsent(world, k -> new ArrayDeque<>()).offer(new VirtualSwapper(pos, source, target, consumeTarget, life, player, fx, fancy, color, pickup, silk, fortune, allowSwap, visCost, null));
    }

    /** addSwapper whose target is an exact block state (TC6 ItemStack target with metadata -> getStateFromMeta). source may be a BlockState or a Predicate&lt;BlockState&gt; (Material). */
    public static void addSwapperState(ServerLevel world, BlockPos pos, Object source, BlockState target, Player player, boolean fx, boolean fancy, int color) {
        swapList.computeIfAbsent(world, k -> new ArrayDeque<>()).offer(new VirtualSwapper(pos, source, new ItemStack(target.getBlock().asItem()), false, 0, player, fx, fancy, color, false, false, 0, DEFAULT_PREDICATE, 0.0f, target));
    }

    public static void addBreaker(ServerLevel world, BlockPos pos, BlockState source, Player player, boolean fx, boolean silk, int fortune, float str, float durabilityCurrent,
                                  float durabilityMax, int delay, float vis, Runnable run) {
        if (delay > 0) {
            addRunnableServer(world, () -> addBreaker(world, pos, source, player, fx, silk, fortune, str, durabilityCurrent, durabilityMax, 0, vis, run), delay);
        } else {
            breakList.computeIfAbsent(world, k -> new ArrayDeque<>()).offer(new BreakData(str, durabilityCurrent, durabilityMax, pos, source, player, fx, silk, fortune, vis));
            if (run != null) run.run();
        }
    }

    public static void addRunnableServer(ServerLevel world, Runnable runnable, int delay) {
        runList.computeIfAbsent(world, k -> new ArrayList<>()).add(new RunnableEntry(runnable, new int[] {delay}));
    }

    private ServerEvents() {}
}
