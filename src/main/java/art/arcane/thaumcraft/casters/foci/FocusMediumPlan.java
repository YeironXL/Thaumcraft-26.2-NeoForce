package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.util.BlockUtils;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FocusMediumPlan extends FocusMedium implements IArchitect {
    private final Set<BlockPos> checked = new HashSet<>();

    @Override public String getResearch() { return "FOCUSPLAN"; }
    @Override public String getKey() { return "thaumcraft.PLAN"; }
    @Override public int getComplexity() { return 4; }
    @Override public String getAspect() { return "fabrico"; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("method", "focus.plan.method", new NodeSetting.NodeSettingIntList(new int[] {0, 1}, new String[] {"focus.plan.full", "focus.plan.surface"}))};
    }

    private static ItemStack casterOf(LivingEntity c) {
        if (c.getMainHandItem().getItem() instanceof CasterItem) return c.getMainHandItem();
        if (c.getOffhandItem().getItem() instanceof CasterItem) return c.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @Override
    public HitResult[] supplyTargets() {
        if (getParent() == null || !(getPackage().getCaster() instanceof Player player)) return new HitResult[0];
        ArrayList<HitResult> targets = new ArrayList<>();
        ItemStack casterStack = casterOf(player);
        for (Trajectory sT : getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize().scale(16.0).add(sT.source);
            HitResult target = EntityUtils.rayTraceBlocks(getPackage().world, sT.source, end, player);
            if (target instanceof BlockHitResult bh) {
                var usl = getArchitectBlocks(casterStack, getPackage().world, bh.getBlockPos(), bh.getDirection(), player);
                usl.sort(Comparator.comparingDouble(p -> p.distSqr(bh.getBlockPos())));
                for (BlockPos p : usl) targets.add(new BlockHitResult(new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5), bh.getDirection(), p, false));
            }
        }
        return targets.toArray(new HitResult[0]);
    }

    @Override
    public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        return EntityUtils.rayTraceBlocks(world, start, player.getLookAngle().scale(16.0).add(start), player);
    }
    @Override public boolean useBlockHighlight(ItemStack stack) { return false; }
    @Override public boolean isExclusive() { return true; }

    @Override
    public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
        if (stack == null || stack.isEmpty()) return false;
        int dim = CasterManager.getAreaDim(stack);
        if (getSettingValue("method") == 0) {
            return switch (axis) {
                case Y -> dim == 0 || dim == 3;
                case Z -> dim == 0 || dim == 2;
                case X -> dim == 0 || dim == 1;
            };
        }
        return switch (side.getAxis()) {
            case Y -> (axis == EnumAxis.X && (dim == 0 || dim == 1)) || (axis == EnumAxis.Z && (dim == 0 || dim == 2));
            case Z -> (axis == EnumAxis.Y && (dim == 0 || dim == 1)) || (axis == EnumAxis.X && (dim == 0 || dim == 2));
            case X -> (axis == EnumAxis.Y && (dim == 0 || dim == 1)) || (axis == EnumAxis.Z && (dim == 0 || dim == 2));
        };
    }

    @Override
    public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
        ArrayList<BlockPos> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return out;
        checked.clear();
        if (getSettingValue("method") == 0)
            checkNeighboursFull(world, pos, pos, side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack), CasterManager.getAreaZ(stack), out);
        else {
            BlockState bi = world.getBlockState(pos);
            if (side.getAxis() == Direction.Axis.Z) checkNeighboursSurface(world, pos, bi, pos, side, CasterManager.getAreaZ(stack), CasterManager.getAreaY(stack), CasterManager.getAreaX(stack), out);
            else checkNeighboursSurface(world, pos, bi, pos, side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack), CasterManager.getAreaZ(stack), out);
        }
        return out;
    }

    private void checkNeighboursFull(Level world, BlockPos pos1, BlockPos pos2, Direction side, int sx, int sy, int sz, ArrayList<BlockPos> list) {
        if (!checked.add(pos2)) return;
        if (!world.isEmptyBlock(pos2)) list.add(pos2);
        int xs = -sx + pos1.getX() - sx * side.getStepX(), xe = sx + pos1.getX() - sx * side.getStepX();
        int ys = -sy + pos1.getY() - sy * side.getStepY(), ye = sy + pos1.getY() - sy * side.getStepY();
        int zs = -sz + pos1.getZ() - sz * side.getStepZ(), ze = sz + pos1.getZ() - sz * side.getStepZ();
        for (Direction dir : Direction.values()) {
            BlockPos q = pos2.relative(dir);
            if (q.getX() >= xs && q.getX() <= xe && q.getY() >= ys && q.getY() <= ye && q.getZ() >= zs && q.getZ() <= ze)
                checkNeighboursFull(world, pos1, q, side, sx, sy, sz, list);
        }
    }

    private void checkNeighboursSurface(Level world, BlockPos pos1, BlockState bi, BlockPos pos2, Direction side, int sx, int sy, int sz, ArrayList<BlockPos> list) {
        if (!checked.add(pos2)) return;
        switch (side.getAxis()) {
            case Y -> { if (Math.abs(pos2.getX() - pos1.getX()) > sx || Math.abs(pos2.getZ() - pos1.getZ()) > sz) return; }
            case Z -> { if (Math.abs(pos2.getX() - pos1.getX()) > sx || Math.abs(pos2.getY() - pos1.getY()) > sz) return; }
            case X -> { if (Math.abs(pos2.getY() - pos1.getY()) > sx || Math.abs(pos2.getZ() - pos1.getZ()) > sz) return; }
        }
        if (world.getBlockState(pos2) == bi && BlockUtils.isBlockExposed(world, pos2) && !world.isEmptyBlock(pos2)) {
            list.add(pos2);
            for (Direction dir : Direction.values())
                if (dir != side && dir.getOpposite() != side) checkNeighboursSurface(world, pos1, bi, pos2.relative(dir), side, sx, sy, sz, list);
        }
    }
}
