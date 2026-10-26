package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.HoleBlockEntity;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectRift extends FocusEffect {
    /** BlockUtils.portableHoleBlackList (beds, pistons, doors, infernal furnace). */
    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("thaumcraft", "portable_hole_blacklist"));

    @Override public String getResearch() { return "FOCUSRIFT"; }
    @Override public String getKey() { return "thaumcraft.RIFT"; }
    @Override public String getAspect() { return "alienis"; }
    @Override public int getComplexity() { return 3 + getSettingValue("duration") / 2 + getSettingValue("depth") / 4; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (!(target instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) return false;
        Level world = getPackage().world;
        BlockPos tp = bh.getBlockPos();
        if (world.dimension().identifier().getPath().equals("outer_lands")) {
            world.playSound(null, tp.getX() + 0.5, tp.getY() + 0.5, tp.getZ() + 0.5, ModSounds.get("wandfail"), SoundSource.PLAYERS, 1.0f, 1.0f);
            return false;
        }
        float maxdis = getSettingValue("depth") * finalPower;
        int dur = 20 * getSettingValue("duration");
        int distance;
        BlockPos pos = tp;
        for (distance = 0; distance < maxdis; ++distance) {
            BlockState bi = world.getBlockState(pos);
            if (bi.is(BLACKLIST) || bi.is(Blocks.BEDROCK) || bi.is(ModBlocks.HOLE.get()) || bi.isAir()) break;
            if (bi.getDestroySpeed(world, pos) == -1.0f) break;
            pos = pos.relative(bh.getDirection().getOpposite());
        }
        createHole(world, tp, bh.getDirection(), (byte) Math.round((float) (distance + 1)), dur);
        return true;
    }

    public static boolean createHole(Level world, BlockPos pos, Direction side, byte count, int max) {
        BlockState bs = world.getBlockState(pos);
        if (!world.isClientSide() && world.getBlockEntity(pos) == null && !bs.is(BLACKLIST) && !bs.is(Blocks.BEDROCK) && !bs.is(ModBlocks.HOLE.get())
                && (bs.isAir() || !bs.canBeReplaced()) && bs.getDestroySpeed(world, pos) != -1.0f) {
            if (world.setBlock(pos, ModBlocks.HOLE.get().defaultBlockState(), 3) && world.getBlockEntity(pos) instanceof HoleBlockEntity ts) {
                ts.oldblock = bs;
                ts.countdownmax = (short) max;
                ts.count = count;
                ts.direction = side;
                ts.setChanged();
            }
            return true;
        }
        return false;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("depth", "focus.rift.depth", new NodeSetting.NodeSettingIntList(new int[] {8, 16, 24, 32}, new String[] {"8", "16", "24", "32"})),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, mx, my, mz, GenericParticleOptions.builder().maxAge(16 + r.nextInt(16)).particles(384 + r.nextInt(16), 1, 1).slowDown(0.75).alpha(1.0f, 0.0f)
                .scale((float) (0.7 + r.nextGaussian() * 0.3)).color(0.25f, 0.25f, 1.0f).randomMovement(0.01f, 0.01f, 0.01f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2f, 0.7f);
    }
}
