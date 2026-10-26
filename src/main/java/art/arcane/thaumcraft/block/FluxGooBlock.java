package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.client.fx.FXDispatcher;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockFluxGoo (a finite fluid of 8 levels): slows and exhausts creatures inside it, and slowly evaporates back
 * into the aura as flux, the last layer either vanishing or turning into taint fibres.
 * Spreads like Forge's BlockFluidFinite (8 quanta, density 8 so it sinks, tick rate viscosity 6000 / 200 = 30) and
 * coalesces into thaumic slimes.
 */
public final class FluxGooBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
    private static final VoxelShape[] SHAPES = new VoxelShape[8];
    static { for (int i = 0; i < 8; i++) SHAPES[i] = Block.box(0, 0, 0, 16, 2 * (i + 1), 16); }

    public FluxGooBlock(Properties p) {
        super(p.mapColor(MapColor.COLOR_PURPLE).sound(SoundType.SLIME_BLOCK).strength(100.0f).noCollision().noOcclusion().randomTicks().pushReaction(PushReaction.DESTROY).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 7));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEVEL); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(LEVEL)]; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected boolean canBeReplaced(BlockState s, net.minecraft.world.item.context.BlockPlaceContext ctx) { return s.getValue(LEVEL) < 4; }

    /** BlockFluidFinite quanta percentage: (level + 1) / 8. */
    private static float quanta(BlockState s) { return (s.getValue(LEVEL) + 1) / 8.0f; }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier fx, boolean precise) {
        int md = state.getValue(LEVEL);
        if (entity instanceof art.arcane.thaumcraft.entity.ThaumicSlimeEntity slime) {
            if (!level.isClientSide() && slime.getSize() < md && level.getRandom().nextBoolean()) {
                slime.setSize(slime.getSize() + 1, true);
                if (md > 1) level.setBlock(pos, state.setValue(LEVEL, md - 1), 2);
                else level.removeBlock(pos, false);
            }
            return;
        }
        var m = entity.getDeltaMovement();
        float q = quanta(state);
        entity.setDeltaMovement(m.x * (1.0f - q), m.y, m.z * (1.0f - q));
        if (!level.isClientSide() && entity instanceof LivingEntity le)
            le.addEffect(new MobEffectInstance(ModEffects.VIS_EXHAUST, 600, md / 3, true, true));
    }

    private static final int TICK_RATE = 30;
    @Override protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) { l.scheduleTick(p, this, TICK_RATE); }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos p, Block b, net.minecraft.world.level.redstone.Orientation o, boolean moved) { l.scheduleTick(p, this, TICK_RATE); }
    @Override protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) { update(state, world, pos, rand); }
    @Override protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) { update(state, world, pos, rand); }

    private void slime(ServerLevel world, BlockPos pos, int size) {
        world.removeBlock(pos, false);
        var slime = new art.arcane.thaumcraft.entity.ThaumicSlimeEntity(art.arcane.thaumcraft.registry.ModEntities.THAUMIC_SLIME.get(), world);
        slime.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        slime.setSize(size, true);
        world.addFreshEntity(slime);
        slime.playSound(art.arcane.thaumcraft.registry.ModSounds.get("gore"), 1.0f, 1.0f);
    }

    private void update(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(LEVEL);
        if (meta >= 2 && meta < 6 && world.isEmptyBlock(pos.above()) && rand.nextInt(50) == 0) { slime(world, pos, 1); return; }
        if (meta >= 6 && world.isEmptyBlock(pos.above()) && rand.nextInt(50) == 0) { slime(world, pos, 2); return; }
        if (rand.nextInt(4) == 0) {
            if (meta == 0) {
                if (rand.nextBoolean()) { NativeAura.get(world, world.getChunkAt(pos)).addFlux(1); world.removeBlock(pos, false); }
                else world.setBlockAndUpdate(pos, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
            } else {
                world.setBlock(pos, state.setValue(LEVEL, meta - 1), 2);
                NativeAura.get(world, world.getChunkAt(pos)).addFlux(1);
            }
            return;
        }
        flow(state, world, pos, rand);
    }

    // ---- BlockFluidFinite ----
    private boolean canDisplace(BlockState s) {
        if (s.isAir()) return true;
        if (s.is(this)) return false;
        if (s.is(ModBlocks.TAINT_FIBRE.get())) return true;
        if (!s.getFluidState().isEmpty() || s.blocksMotion() || s.is(Blocks.NETHER_PORTAL)) return false;
        return true;
    }
    private boolean displaceIfPossible(ServerLevel w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        if (s.isAir() || !canDisplace(s)) return false;
        w.destroyBlock(p, true);
        return true;
    }
    /** getQuantaValueBelow: 0 for air, level+1 for goo, -1 otherwise or when not below {@code below}. */
    private int quantaBelow(ServerLevel w, BlockPos p, int below) {
        BlockState s = w.getBlockState(p);
        int q = s.isAir() ? 0 : s.is(this) ? s.getValue(LEVEL) + 1 : -1;
        return q >= below ? -1 : q;
    }
    private void set(ServerLevel w, BlockPos p, int quanta) {
        if (quanta <= 0) w.removeBlock(p, false);
        else w.setBlock(p, defaultBlockState().setValue(LEVEL, quanta - 1), 2);
        w.scheduleTick(p, this, TICK_RATE);
    }
    private int flowDown(ServerLevel w, BlockPos pos, int amt) {
        BlockPos other = pos.below();
        if (other.getY() < w.getMinY()) { w.removeBlock(pos, false); return 0; }
        displaceIfPossible(w, other);
        int q = quantaBelow(w, other, 8);
        if (q >= 0) {
            q += amt;
            if (q > 8) { set(w, other, 8); return q - 8; }
            if (q > 0) { set(w, other, q); w.removeBlock(pos, false); return 0; }
        }
        return amt;
    }
    private void flow(BlockState state, ServerLevel w, BlockPos pos, RandomSource rand) {
        boolean changed = false;
        int remaining = state.getValue(LEVEL) + 1, prev = remaining;
        remaining = flowDown(w, pos, remaining);
        if (remaining < 1) return;
        if (remaining != prev) {
            changed = true;
            if (remaining == 1) { w.setBlock(pos, state.setValue(LEVEL, 0), 2); return; }
        } else if (remaining == 1) return;
        int lowerThan = remaining - 1, total = remaining, count = 1;
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos off = pos.relative(side);
            displaceIfPossible(w, off);
            int q = quantaBelow(w, off, lowerThan);
            if (q >= 0) { count++; total += q; }
        }
        if (count == 1) { if (changed) w.setBlock(pos, state.setValue(LEVEL, remaining - 1), 2); return; }
        int each = total / count, rem = total % count;
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos off = pos.relative(side);
            int q = quantaBelow(w, off, lowerThan);
            if (q >= 0) {
                int nq = each;
                if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) { ++nq; --rem; }
                if (nq != q) set(w, off, nq);
                --count;
            }
        }
        if (rem > 0) ++each;
        if (each <= 0) w.removeBlock(pos, false); else w.setBlock(pos, state.setValue(LEVEL, each - 1), 2);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(LEVEL);
        if (rand.nextInt(44) <= meta)
            FXDispatcher.generic(pos.getX() + rand.nextFloat(), pos.getY() + 0.125f * meta, pos.getZ() + rand.nextFloat(), 0, 0, 0,
                    GenericParticleOptions.builder().maxAge(2 + rand.nextInt(3)).scale(rand.nextFloat() * 0.3f + 0.2f).color(1.0f, 0.0f, 0.5f)
                            .randomMovement(0.001f, 0.001f, 0.001f).gravity(-0.01f).alpha(0.25f).particle(64).finalFrames(65, 66).build(), 0);
    }
}
