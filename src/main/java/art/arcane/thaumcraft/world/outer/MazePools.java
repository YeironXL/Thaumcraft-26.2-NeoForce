package art.arcane.thaumcraft.world.outer;

import art.arcane.thaumcraft.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Maze version 5 corridor pools (TC5 wiki: "pools of all types of liquids, from lava to Liquid Death, can be found
 * throughout the halls"; TC4/TC5 tainted areas). Shapes are invented: an elongated, ragged pool along the corridor,
 * 1 block deep at the edges and 2 at the middle, always leaving a one-block walkway on each side. Kinds:
 * lava (rimmed with magma), water, Liquid Death (rimmed with crusted stone) and flux goo (rimmed with taint crust).
 */
public final class MazePools {
    public enum Kind { LAVA, WATER, DEATH, GOO }
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public static Kind kind(Random r) {
        int k = r.nextInt(100);
        return k < 30 ? Kind.LAVA : k < 55 ? Kind.WATER : k < 80 ? Kind.DEATH : Kind.GOO;
    }

    /** Builds a pool in the corridor cell whose origin is {@code o}; returns the kind. The cell centre is always liquid. */
    public static Kind pool(ServerLevel w, BlockPos o, int conn, Random r) {
        Kind kind = kind(r);
        boolean alongZ = (conn & 5) != 0 && (conn & 10) == 0 || ((conn & 5) != 0 && r.nextBoolean());
        double len = 2.0 + r.nextDouble() * 2.5, wid = 1.0 + r.nextDouble() * 0.5;
        BlockState liquid = switch (kind) {
            case LAVA -> Blocks.LAVA.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case DEATH -> ModBlocks.LIQUID_DEATH.get().defaultBlockState();
            case GOO -> ModBlocks.FLUX_GOO.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.FluxGooBlock.LEVEL, 7);
        };
        BlockState rim = switch (kind) {
            case LAVA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case DEATH -> ModBlocks.CRUSTED_STONE.get().defaultBlockState();
            case GOO -> ModBlocks.TAINT_CRUST.get().defaultBlockState();
            default -> null;
        };
        for (int a = -5; a <= 5; a++) for (int b = -2; b <= 2; b++) {
            int x = 8 + (alongZ ? b : a), z = 8 + (alongZ ? a : b);
            if (x < 6 || x > 10 || z < 1 || z > 15) continue;
            double e = (a * a) / (len * len) + (b * b) / (wid * wid);
            boolean in = e <= 1.0 + (r.nextDouble() - 0.5) * 0.35 || (a == 0 && b == 0);
            if (Math.abs(alongZ ? x - 8 : z - 8) >= 2) in = false; // keep the walkway
            BlockPos floor = o.offset(x, 0, z);
            if (!w.getBlockState(floor.above()).isAir()) in = in && a == 0 && b == 0;
            if (in) {
                if (kind == Kind.GOO) {
                    w.setBlock(floor, ModBlocks.TAINT_SOIL.get().defaultBlockState(), FLAGS);
                    w.setBlock(floor.above(), liquid, FLAGS);
                } else {
                    w.setBlock(floor, liquid, FLAGS);
                    if (e < 0.35) w.setBlock(floor.below(), liquid, FLAGS);
                }
            } else if (rim != null && e <= 2.2 && w.getBlockState(floor.above()).isAir() && r.nextInt(3) != 0) {
                w.setBlock(floor, rim, FLAGS);
            }
        }
        return kind;
    }

    private MazePools() {}
}
