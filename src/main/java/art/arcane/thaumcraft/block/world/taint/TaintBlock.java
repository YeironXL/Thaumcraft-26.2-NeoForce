package art.arcane.thaumcraft.block.world.taint;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** TC6 BlockTaint (crust, soil, rock and geyser): withers without a nearby seed, rock spreads fibres, crust slumps down. */
public final class TaintBlock extends Block implements ITaintBlock {
    public TaintBlock(Properties p) {
        super(p.mapColor(MapColor.COLOR_PURPLE).strength(10.0f, 100.0f).sound(TaintFibreBlock.GORE).randomTicks());
    }

    @Override
    public void die(Level world, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.TAINT_ROCK.get())) world.setBlockAndUpdate(pos, ModBlocks.STONE_POROUS.get().defaultBlockState());
        else if (state.is(ModBlocks.TAINT_SOIL.get())) world.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        else if (state.is(ModBlocks.TAINT_CRUST.get()) || state.is(ModBlocks.TAINT_GEYSER.get())) world.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState());
        else world.removeBlock(pos, false);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!(TaintHelper.isNearTaintSeed(world, pos) || art.arcane.thaumcraft.world.outer.MazeGenerator.permanentTaint(world, pos)) && random.nextInt(10) == 0) { die(world, pos, state); return; }
        if (state.is(ModBlocks.TAINT_ROCK.get())) TaintHelper.spreadFibres(world, pos);
        if (state.is(ModBlocks.TAINT_CRUST.get())) {
            if (tryToFall(world, pos, pos)) return;
            if (world.isEmptyBlock(pos.above())) {
                boolean doIt = true;
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                for (int a = 1; a < 4; ++a) {
                    if (!world.isEmptyBlock(pos.relative(dir).below(a))) { doIt = false; break; }
                    if (world.getBlockState(pos.below(a)).getBlock() != this) { doIt = false; break; }
                }
                if (doIt) tryToFall(world, pos, pos.relative(dir));
            }
        } else if (state.is(ModBlocks.TAINT_GEYSER.get())) {
            // a taint swarm rises (20%, player within 32, no other swarm within 32), otherwise the geyser vents flux
            if (random.nextFloat() < 0.2 && world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0, false) != null
                    && TaintHelper.inRange(world, pos, art.arcane.thaumcraft.entity.monster.TaintSwarmEntity.class, 32.0, null).isEmpty()) {
                var e = art.arcane.thaumcraft.registry.ModEntities.TAINT_SWARM.get().create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (e != null) {
                    e.snapTo(pos.getX() + 0.5f, pos.getY() + 1.25f, pos.getZ() + 0.5f, random.nextInt(360), 0.0f);
                    world.addFreshEntity(e);
                }
            } else if (NativeAura.flux(world, pos) < 2.0f) NativeAura.addFlux(world, pos, 0.25f);
        }
    }

    public static boolean canFallBelow(Level world, BlockPos pos) {
        BlockState bs = world.getBlockState(pos);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) if (world.getBlockState(p).is(BlockTags.LOGS)) return false;
        if (bs.isAir()) return true;
        if (bs.is(ModBlocks.FLUX_GOO.get()) && bs.getValue(art.arcane.thaumcraft.block.FluxGooBlock.LEVEL) >= 4) return false;
        return bs.is(Blocks.FIRE) || bs.is(ModBlocks.TAINT_FIBRE.get()) || bs.canBeReplaced() || !bs.getFluidState().isEmpty();
    }

    /** EntityFallingTaint is a falling taint crust; vanilla's falling block entity does the same fall-and-settle. */
    private boolean tryToFall(ServerLevel world, BlockPos pos, BlockPos pos2) {
        if (!TaintFibreBlock.isOnlyAdjacentToTaint(world, pos)) return false;
        if (canFallBelow(world, pos2.below()) && pos2.getY() >= world.getMinY()) {
            BlockState state = world.getBlockState(pos);
            if (!pos2.equals(pos)) { world.removeBlock(pos, false); world.setBlock(pos2, state, 2); }
            FallingBlockEntity.fall(world, pos2, state);
            return true;
        }
        return false;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && entity instanceof LivingEntity le && !le.isInvertedHealAndHarm() && world.getRandom().nextInt(250) == 0)
            le.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 200, 0, false, true));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (level.isClientSide()) level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1f, 0.9f + level.getRandom().nextFloat() * 0.2f, false);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    /** getDrops: tainted rock sometimes yields a flux crystal (r.nextInt(15) > 13). */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean xp) {
        super.spawnAfterBreak(state, level, pos, tool, xp);
        if (state.is(ModBlocks.TAINT_ROCK.get()) && level.getRandom().nextInt(15) > 13) Block.popResource(level, pos, CrystalEssenceItem.of("vitium", 1));
    }
}
