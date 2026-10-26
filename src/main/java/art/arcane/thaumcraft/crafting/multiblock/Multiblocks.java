package art.arcane.thaumcraft.crafting.multiblock;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.PillarBlock;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/** ConfigRecipes.initializeCompoundRecipes: the salis mundus multiblock blueprints, in TC6 registration order. */
public final class Multiblocks {
    private static boolean done;

    public static void init() {
        if (done) return;
        done = true;
        Part NB = new Part(Blocks.NETHER_BRICKS, ModBlocks.PLACEHOLDER_BRICK.get());
        Part OB = new Part(Blocks.OBSIDIAN, ModBlocks.PLACEHOLDER_OBSIDIAN.get());
        Part IB = new Part(Blocks.IRON_BARS, "AIR");
        Predicate<BlockState> lava = s -> s.getFluidState().is(FluidTags.LAVA);
        Part LA = new Part(lava, ModBlocks.INFERNAL_FURNACE.get(), true);
        reg("INFERNALFURNACE", new Part[][][] {{{NB, OB, NB}, {OB, null, OB}, {NB, OB, NB}}, {{NB, OB, NB}, {OB, LA, OB}, {NB, IB, NB}}, {{NB, OB, NB}, {OB, OB, OB}, {NB, OB, NB}}});

        Part IM = new Part(ModBlocks.INFUSION_MATRIX.get(), null);
        altar("INFUSION", ModBlocks.STONE_ARCANE.get(), ModBlocks.PILLAR_ARCANE.get(), ModBlocks.PEDESTAL_ARCANE.get(), IM);
        altar("INFUSIONANCIENT", ModBlocks.STONE_ANCIENT.get(), ModBlocks.PILLAR_ANCIENT.get(), ModBlocks.PEDESTAL_ANCIENT.get(), IM);
        altar("INFUSIONELDRITCH", ModBlocks.STONE_ELDRITCH_TILE.get(), ModBlocks.PILLAR_ELDRITCH.get(), ModBlocks.PEDESTAL_ELDRITCH.get(), IM);

        Part TH1 = new Part(ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState(), ModBlocks.THAUMATORIUM_TOP.get()).setApplyPlayerFacing(true);
        Part TH2 = new Part(ModBlocks.METAL_ALCHEMICAL.get().defaultBlockState(), ModBlocks.THAUMATORIUM.get()).setApplyPlayerFacing(true);
        Part TH3 = new Part(ModBlocks.CRUCIBLE.get(), null);
        reg("THAUMATORIUM", new Part[][][] {{{TH1}}, {{TH2}}, {{TH3}}});

        Part GP1 = new Part(Blocks.IRON_BARS, ModBlocks.PLACEHOLDER_BARS.get());
        Part GP2 = new Part(Blocks.CAULDRON, ModBlocks.PLACEHOLDER_CAULDRON.get());
        Part GP3 = new Part(Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), ModBlocks.GOLEM_BUILDER.get());
        Part GP4 = new Part(Blocks.ANVIL, ModBlocks.PLACEHOLDER_ANVIL.get());
        Part GP5 = new Part(ModBlocks.TABLE_STONE.get(), ModBlocks.PLACEHOLDER_TABLE.get());
        reg("MINDCLOCKWORK", new Part[][][] {{{null, null}, {GP1, null}}, {{GP2, GP4}, {GP3, GP5}}});
    }

    private static void altar(String research, Block stone, Block pillar, Block pedestal, Part im) {
        Part top = new Part(stone, "AIR");
        Part b1 = new Part(stone, pillar.defaultBlockState().setValue(PillarBlock.FACING, Direction.EAST));
        Part b2 = new Part(stone, pillar.defaultBlockState().setValue(PillarBlock.FACING, Direction.NORTH));
        Part b3 = new Part(stone, pillar.defaultBlockState().setValue(PillarBlock.FACING, Direction.SOUTH));
        Part b4 = new Part(stone, pillar.defaultBlockState().setValue(PillarBlock.FACING, Direction.WEST));
        Part ped = new Part(pedestal, null);
        reg(research, new Part[][][] {{{null, null, null}, {null, im, null}, {null, null, null}}, {{top, null, top}, {null, null, null}, {top, null, top}}, {{b1, null, b2}, {null, ped, null}, {b3, null, b4}}});
    }

    private static void reg(String research, Part[][][] bp) { DustTriggerMultiblock.TRIGGERS.add(new DustTriggerMultiblock(research, bp)); }

    private Multiblocks() {}
}
