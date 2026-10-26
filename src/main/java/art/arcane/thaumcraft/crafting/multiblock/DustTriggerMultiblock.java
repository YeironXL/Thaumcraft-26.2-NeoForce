package art.arcane.thaumcraft.crafting.multiblock;

import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.util.BlockUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** TC6 lib.crafting.DustTriggerMultiblock: salis mundus turns a matching structure into its formed blocks. */
public final class DustTriggerMultiblock {
    public record Placement(int xOffset, int yOffset, int zOffset, Direction facing) {}

    public static final List<DustTriggerMultiblock> TRIGGERS = new ArrayList<>();

    final Part[][][] blueprint;
    final String research;
    final int ySize, xSize, zSize;

    public DustTriggerMultiblock(String research, Part[][][] blueprint) {
        this.blueprint = blueprint; this.research = research;
        ySize = blueprint.length; xSize = blueprint[0].length; zSize = blueprint[0][0].length;
    }

    public Part[][][] blueprint() { return blueprint; }
    public String research() { return research; }

    /** Direction.getHorizontal order of 1.12: S, W, N, E (2D data value). */
    private static final Direction[] HORIZONTALS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

    public Placement getValidFace(Level world, ServerPlayer player, BlockPos pos, Direction face) {
        if (research != null && !PlayerProgressManager.has(player, research)) return null;
        for (int yy = -ySize; yy <= 0; ++yy)
            for (int xx = -xSize; xx <= 0; ++xx)
                for (int zz = -zSize; zz <= 0; ++zz) {
                    Direction f = fitMultiblock(world, pos.offset(xx, yy, zz));
                    if (f != null) return new Placement(xx, yy, zz, f);
                }
        return null;
    }

    private Direction fitMultiblock(Level world, BlockPos pos) {
        faces:
        for (Direction face : HORIZONTALS) {
            for (int y = 0; y < ySize; ++y) {
                Matrix m = new Matrix(blueprint[y]);
                m.rotate90DegRight(3 - face.get2DDataValue());
                for (int x = 0; x < m.rows; ++x) for (int z = 0; z < m.cols; ++z) {
                    Part p = m.matrix[x][z];
                    if (p != null && !p.matches(world.getBlockState(pos.offset(x, -y + (ySize - 1), z)))) continue faces;
                }
            }
            return face;
        }
        return null;
    }

    public List<BlockPos> sparkle(Level world, BlockPos pos, Placement placement) {
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        List<BlockPos> list = new ArrayList<>();
        for (int y = 0; y < ySize; ++y) {
            Matrix m = new Matrix(blueprint[y]);
            m.rotate90DegRight(3 - placement.facing.get2DDataValue());
            for (int x = 0; x < m.rows; ++x) for (int z = 0; z < m.cols; ++z) {
                if (m.matrix[x][z] == null) continue;
                BlockPos p3 = p2.offset(x, -y + (ySize - 1), z);
                if (m.matrix[x][z].getSource() != null && BlockUtils.isBlockExposed(world, p3)) list.add(p3);
            }
        }
        return list;
    }

    public void execute(ServerLevel world, ServerPlayer player, BlockPos pos, Placement placement, Direction side) {
        // TC6 fires the crafting event with an infernal furnace stack for every multiblock.
        PlayerProgressManager.get(player).recordCraft("thaumcraft:infernal_furnace");
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        for (int y = 0; y < ySize; ++y) {
            Matrix m = new Matrix(blueprint[y]);
            m.rotate90DegRight(3 - placement.facing.get2DDataValue());
            for (int x = 0; x < m.rows; ++x) for (int z = 0; z < m.cols; ++z) {
                Part part = m.matrix[x][z];
                if (part == null || part.getTarget() == null) continue;
                BlockState target;
                if (part.getTarget() instanceof Block b) {
                    target = b.defaultBlockState();
                    var facingProp = target.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? BlockStateProperties.HORIZONTAL_FACING : null;
                    if (facingProp != null) {
                        Direction side2 = side;
                        if (side2.getAxis().isVertical()) side2 = player.getDirection().getOpposite();
                        target = target.setValue(facingProp, part.getApplyPlayerFacing() ? side2 : (part.isOpp() ? placement.facing.getOpposite() : placement.facing));
                    }
                } else if (part.getTarget() instanceof BlockState s) target = s;
                else target = null; // "AIR"
                BlockPos p3 = p2.offset(x, -y + (ySize - 1), z);
                Object source = part.getSource() instanceof Block ? world.getBlockState(p3) : part.getSource();
                final BlockState t = target;
                ServerEvents.addRunnableServer(world, () -> {
                    if (t == null) ServerEvents.addSwapper(world, p3, source, null, false, 0, player, true, false, -9999, false, false, 0, ServerEvents.DEFAULT_PREDICATE, 0.0f);
                    else ServerEvents.addSwapperState(world, p3, source, t, player, true, false, -9999);
                }, part.getPriority());
            }
        }
    }
}
