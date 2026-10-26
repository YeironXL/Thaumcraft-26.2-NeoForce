package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.eldritch.EldritchAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * TC4 Eldritch Obelisk: an obsidian / eldritch tile platform sunk a few blocks into the ground, the Eldritch Altar
 * at its centre and the obelisk floating above it (the portal opens in the gap). Guarded by Crimson Cultists.
 */
public final class EldritchObeliskGen {
    public static final int CHANCE = 200;

    public static boolean generate(LevelAccessor w, Random r, BlockPos ground) {
        BlockPos c = ground;
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState(), tile = ModBlocks.STONE_ELDRITCH_TILE.get().defaultBlockState();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int d2 = x * x + z * z;
            if (d2 > 18) continue;
            for (int y = -3; y <= 0; y++) w.setBlock(c.offset(x, y, z), d2 <= 2 || (d2 > 9 && r.nextInt(3) == 0) ? tile : obsidian, 2);
            for (int y = 1; y <= 8; y++) w.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockPos altar = c.above();
        w.setBlock(altar, ModBlocks.ELDRITCH_ALTAR.get().defaultBlockState(), 2);
        if (w.getBlockEntity(altar) instanceof EldritchAltarBlockEntity be) be.markNatural();
        w.setBlock(altar.above(3), ModBlocks.ELDRITCH_OBELISK_BASE.get().defaultBlockState(), 2);
        w.setBlock(altar.above(4), ModBlocks.ELDRITCH_OBELISK_BODY.get().defaultBlockState(), 2);
        w.setBlock(altar.above(5), ModBlocks.ELDRITCH_OBELISK_BODY.get().defaultBlockState(), 2);
        w.setBlock(altar.above(6), ModBlocks.ELDRITCH_CAPSTONE.get().defaultBlockState(), 2);
        return true;
    }

    private EldritchObeliskGen() {}
}
