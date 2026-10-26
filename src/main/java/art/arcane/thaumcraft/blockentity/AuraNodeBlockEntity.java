package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.api.IGogglesDisplayExtended;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.aura.node.AuraNodeGen;
import art.arcane.thaumcraft.aura.node.NodeModifier;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TC4 TileNode adapted to TC6. A node holds a type, a modifier and an aspect pool (current amount and maximum per
 * aspect). Sourced TC4 behaviour: regeneration (+1 per aspect every 600/400/900 ticks, none when fading), a drained-out
 * aspect shrinks the node, larger nodes "bully" (drain) smaller ones closer than 4 blocks, hungry nodes pull and
 * devour items, creatures and blocks (not obsidian) and gain their aspects, unstable nodes shift their aspects, dark
 * nodes spawn furious (brainy) zombies, tainted nodes spread taint, pure nodes purify it.
 * TC6 adaptation (TC6 has no wands/aspect vis pools): the node "breathes" its aspects into the chunk aura as vis,
 * raising the chunk above its biome base; tainted/unstable nodes add flux, pure nodes remove it.
 */
public final class AuraNodeBlockEntity extends BlockEntity implements IGogglesDisplayExtended {
    public NodeType type = NodeType.NORMAL;
    public NodeModifier modifier = NodeModifier.NONE;
    /** Current amounts and maxima, keyed by aspect id (insertion order = render order). */
    public final LinkedHashMap<String, Integer> aspects = new LinkedHashMap<>(), base = new LinkedHashMap<>();
    public boolean initialized;
    private int age;
    public AuraNodeBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.AURA_NODE.get(), pos, state); }

    // ------------------------------------------------------------------ data
    public int total() { int t = 0; for (int v : aspects.values()) t += v; return t; }
    public int totalBase() { int t = 0; for (int v : base.values()) t += v; return t; }
    public int amount(Aspect a) { return aspects.getOrDefault(a.id(), 0); }
    public void set(Aspect a, int amount) { set(a, amount, amount); }
    public void set(Aspect a, int amount, int max) {
        if (max <= 0) { aspects.remove(a.id()); base.remove(a.id()); }
        else { aspects.put(a.id(), Math.min(amount, max)); base.put(a.id(), max); }
        initialized = true;
        sync();
    }
    public Map<Aspect, Integer> aspectMap() {
        var m = new LinkedHashMap<Aspect, Integer>();
        aspects.forEach((k, v) -> { var a = AspectRegistry.get(k); if (a != null) m.put(a, v); });
        return m;
    }
    /** Drain from one aspect (TC4 TileNode.takeFromContainer); a fully drained aspect shrinks the node by one. */
    public int drain(String id, int amount) {
        int have = aspects.getOrDefault(id, 0);
        int taken = Math.min(have, amount);
        if (taken <= 0) return 0;
        aspects.put(id, have - taken);
        if (have - taken <= 0) {
            int b = base.getOrDefault(id, 0) - 1;
            if (b <= 0) { base.remove(id); aspects.remove(id); } else base.put(id, b);
        }
        sync();
        if (base.isEmpty() && level != null && !level.isClientSide()) level.removeBlock(worldPosition, false);
        return taken;
    }
    /** Grow by an absorbed aspect (hungry nodes, bullying). */
    public void absorb(String id, int amount, boolean grow) {
        int cur = aspects.getOrDefault(id, 0) + amount, max = base.getOrDefault(id, 0);
        if (grow || cur > max) max = Math.max(max + (grow ? 1 : 0), cur);
        aspects.put(id, Math.min(cur, 255)); base.put(id, Math.min(max, 255));
        sync();
    }
    public String largest() {
        String best = null; int bv = 0;
        for (var e : aspects.entrySet()) if (e.getValue() > bv) { bv = e.getValue(); best = e.getKey(); }
        return best;
    }

    public CompoundTag toTag() {
        var t = new CompoundTag();
        t.putInt("nodetype", type.ordinal());
        t.putInt("nodemod", modifier.ordinal());
        t.putBoolean("init", initialized);
        var a = new CompoundTag(); aspects.forEach(a::putInt); t.put("aspects", a);
        var b = new CompoundTag(); base.forEach(b::putInt); t.put("base", b);
        return t;
    }
    public void fromTag(CompoundTag t) {
        type = NodeType.byId(t.getIntOr("nodetype", 0));
        modifier = NodeModifier.byId(t.getIntOr("nodemod", 0));
        initialized = t.getBooleanOr("init", false);
        aspects.clear(); base.clear();
        t.getCompound("base").ifPresent(b -> { for (String k : b.keySet()) if (AspectRegistry.get(k) != null) base.put(k, b.getIntOr(k, 0)); });
        t.getCompound("aspects").ifPresent(a -> { for (String k : base.keySet()) aspects.put(k, Math.min(a.getIntOr(k, 0), base.get(k))); });
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("node", CompoundTag.CODEC, toTag()); out.putInt("age", age); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); in.read("node", CompoundTag.CODEC).ifPresent(this::fromTag); age = in.getIntOr("age", 0); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public List<Component> getIGogglesText() {
        var lines = new ArrayList<Component>();
        Component name = Component.translatable("tc.node.type." + type.key());
        if (modifier != NodeModifier.NONE) name = Component.translatable("tc.node.mod." + modifier.key()).append(" ").append(name);
        lines.add(name.copy().withStyle(type == NodeType.NORMAL ? ChatFormatting.WHITE : ChatFormatting.LIGHT_PURPLE));
        return lines;
    }
    @Override public Vec3 getIGogglesTextOffset() { return new Vec3(0, -0.6, 0); }

    // ------------------------------------------------------------------ ticking
    public static void tick(Level world, BlockPos pos, BlockState state, AuraNodeBlockEntity te) {
        if (world.isClientSide()) {
            if (te.type == NodeType.HUNGRY && world.getRandom().nextInt(3) == 0) {
                var r = world.getRandom();
                world.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, pos.getX() + 0.5 + (r.nextFloat() - 0.5) * 4, pos.getY() + 0.5 + (r.nextFloat() - 0.5) * 4, pos.getZ() + 0.5 + (r.nextFloat() - 0.5) * 4, 0, 0, 0);
            }
            return;
        }
        var level = (ServerLevel) world;
        if (!te.initialized) { AuraNodeGen.randomize(te, level, level.getRandom(), null); te.sync(); }
        te.age++;
        // TC4 regeneration: every `regen` ticks each aspect below its maximum regains one point.
        int regen = te.modifier.regen;
        if (regen > 0 && te.age % regen == 0) te.regenStep();
        if (te.type == NodeType.HUNGRY) te.hungryPull(level);
        if (te.age % 20 == 0) {
            te.feedAura(level);
            te.typeEffects(level);
        }
        if (te.age % 40 == 0) te.bully(level);
    }

    /** One regeneration pulse: every aspect below its maximum regains a point. */
    public boolean regenStep() {
        boolean changed = false;
        for (var e : base.entrySet()) {
            int cur = aspects.getOrDefault(e.getKey(), 0);
            if (cur < e.getValue()) { aspects.put(e.getKey(), cur + 1); changed = true; }
        }
        if (changed) sync();
        return changed;
    }

    /** Chunk-aura cap a node supports: biome base plus half of the node's maximum (TC6 adaptation). */
    public int auraCap(ServerLevel level) { return NativeAura.base(level, worldPosition) + totalBase() / 2; }

    /** Each second: while the chunk is below the node-raised cap, one aspect point becomes vis (compounds 2). */
    public void feedAura(ServerLevel level) {
        if (aspects.isEmpty()) return;
        float vis = NativeAura.vis(level, worldPosition), flux = NativeAura.flux(level, worldPosition);
        if (vis + flux >= auraCap(level)) return;
        var candidates = new ArrayList<String>();
        aspects.forEach((k, v) -> { if (v > 1) candidates.add(k); });
        if (candidates.isEmpty()) return;
        String id = candidates.get(level.getRandom().nextInt(candidates.size()));
        Aspect a = AspectRegistry.get(id);
        aspects.put(id, aspects.get(id) - 1);
        NativeAura.addVis(level, worldPosition, a.isPrimal() ? 1.0f : 2.0f);
        setChanged();
    }

    public void typeEffects(ServerLevel level) {
        var r = level.getRandom();
        switch (type) {
            case UNSTABLE -> {
                NativeAura.addFlux(level, worldPosition, 0.05f);
                if (r.nextInt(20) == 0 && !aspects.isEmpty()) {
                    // TC4: unstable nodes randomly change their aspects.
                    var keys = new ArrayList<>(aspects.keySet());
                    String from = keys.get(r.nextInt(keys.size()));
                    var all = new ArrayList<>(AspectRegistry.all().values());
                    Aspect to = all.get(r.nextInt(all.size()));
                    if (!to.id().equals(from) && drain(from, 1) > 0) absorb(to.id(), 1, false);
                }
            }
            case TAINTED -> {
                NativeAura.addFlux(level, worldPosition, 0.1f);
                if (r.nextInt(10) == 0) {
                    BlockPos p = worldPosition.offset(r.nextInt(13) - 6, r.nextInt(7) - 3, r.nextInt(13) - 6);
                    if (level.isLoaded(p) && level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP))
                        level.setBlock(p, ModBlocks.TAINT_FIBRE.get().defaultBlockState(), 3);
                }
            }
            case PURE -> {
                NativeAura.drainFlux(level, worldPosition, 0.2f, false);
                if (modifier != NodeModifier.PALE && r.nextInt(5) == 0) purify(level, worldPosition.offset(r.nextInt(13) - 6, r.nextInt(7) - 3, r.nextInt(13) - 6));
            }
            case DARK -> {
                if (r.nextInt(30) == 0 && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL) spawnFurious(level);
            }
            case HUNGRY -> { if (age % 100 == 0) eatBlock(level); }
            default -> {}
        }
    }

    public static void purify(ServerLevel level, BlockPos p) {
        if (!level.isLoaded(p)) return;
        var s = level.getBlockState(p);
        if (s.is(ModBlocks.TAINT_FIBRE.get())) level.removeBlock(p, false);
        else if (s.is(ModBlocks.TAINT_SOIL.get()) || s.is(ModBlocks.TAINT_CRUST.get())) level.setBlock(p, Blocks.DIRT.defaultBlockState(), 3);
        else if (s.is(ModBlocks.TAINT_LOG.get())) level.setBlock(p, Blocks.OAK_LOG.defaultBlockState(), 3);
    }

    private void spawnFurious(ServerLevel level) {
        var box = new AABB(worldPosition).inflate(16);
        if (level.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.BrainyZombieEntity.class, box).size() >= 3) return;
        var r = level.getRandom();
        for (int i = 0; i < 8; i++) {
            BlockPos p = worldPosition.offset(r.nextInt(11) - 5, r.nextInt(5) - 2, r.nextInt(11) - 5);
            if (!level.isLoaded(p) || !level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir() || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) continue;
            if (level.getMaxLocalRawBrightness(p) > 7) continue;
            var z = art.arcane.thaumcraft.registry.ModEntities.BRAINY_ZOMBIE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWNER);
            if (z == null) return;
            z.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360, 0);
            level.addFreshEntity(z);
            return;
        }
    }

    /** Hungry node: pulls loose things within 15 blocks; swallows items (gaining their aspects) and bites creatures. */
    public void hungryPull(ServerLevel level) {
        Vec3 c = Vec3.atCenterOf(worldPosition);
        for (Entity e : level.getEntities((Entity) null, new AABB(worldPosition).inflate(15), x -> x.isAlive() && !x.isSpectator())) {
            if (e instanceof Player p && (p.isCreative())) continue;
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist > 15) continue;
            if (dist < 1.0 && e instanceof ItemEntity item) { eat(item.getItem()); item.discard(); continue; }
            if (dist < 1.5 && e instanceof LivingEntity living && age % 10 == 0) {
                living.hurtServer(level, level.damageSources().magic(), 1.0f);
                if (!living.isAlive()) absorb(AspectRegistry.VICTUS.id(), 1, level.getRandom().nextInt(3) == 0);
            }
            double f = 0.03 + 0.12 * (1.0 - dist / 16.0);
            e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(f)));
            e.hurtMarked = true;
        }
    }

    /** A swallowed stack: one point of each aspect per item (capped at the stack's own amounts), with a chance to grow. */
    public void eat(ItemStack stack) {
        if (stack.isEmpty()) return;
        var tags = ObjectAspects.get(stack);
        if (tags.isEmpty()) return;
        var r = level == null ? net.minecraft.util.RandomSource.create() : level.getRandom();
        for (var e : tags.entrySet()) absorb(e.getKey().id(), Math.max(1, e.getValue() * stack.getCount() / 8), r.nextInt(4) == 0);
    }

    public void eatBlock(ServerLevel level) {
        var r = level.getRandom();
        BlockPos p = worldPosition.offset(r.nextInt(11) - 5, r.nextInt(11) - 5, r.nextInt(11) - 5);
        if (p.equals(worldPosition) || !level.isLoaded(p)) return;
        var s = level.getBlockState(p);
        if (s.isAir() || s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN) || s.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null || s.is(ModBlocks.AURA_NODE.get())) return;
        var tags = ObjectAspects.get(new ItemStack(s.getBlock().asItem()));
        level.destroyBlock(p, false);
        if (tags.isEmpty()) return;
        var list = new ArrayList<>(tags.keySet());
        absorb(list.get(r.nextInt(list.size())).id(), 1, r.nextInt(3) == 0);
    }

    /** TC4 node bullying: a bigger node drains a point from a smaller one closer than 4 blocks and may grow from it. */
    public void bully(ServerLevel level) {
        var r = level.getRandom();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, -3, -3), worldPosition.offset(3, 3, 3))) {
            if (p.equals(worldPosition) || !(level.getBlockEntity(p) instanceof AuraNodeBlockEntity other)) continue;
            if (other.totalBase() >= totalBase() || other.aspects.isEmpty()) continue;
            var keys = new ArrayList<>(other.aspects.keySet());
            String id = keys.get(r.nextInt(keys.size()));
            if (other.drain(id, 1) > 0) absorb(id, 1, r.nextInt(5) == 0);
            return;
        }
    }
}
