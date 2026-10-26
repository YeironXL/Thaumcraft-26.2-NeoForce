package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.RechargePedestalBlockEntity;
import art.arcane.thaumcraft.blockentity.VisGeneratorBlockEntity;
import art.arcane.thaumcraft.item.RechargeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Recharge pedestal (5 vis / 10 ticks into a grapple gun, rejects other items), workbench charger support, vis generator 1 vis -> 1000 FE. */
final class AuromancyDevicesVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos p = new BlockPos(3, 1, 3);
        h.setBlock(p, ModBlocks.RECHARGE_PEDESTAL.get());
        var te = h.getBlockEntity(p, RechargePedestalBlockEntity.class);
        check(!te.canPlaceItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND)), "Recharge pedestal accepted a non-rechargeable item");
        var gun = new ItemStack(ModItems.GRAPPLE_GUN.get());
        check(RechargeHelper.getCharge(gun) == 0, "Fresh grapple gun charge");
        te.setItem(0, gun);
        var aura = NativeAura.get(level, level.getChunkAt(h.absolutePos(p)));
        aura.spend(aura.vis()); aura.addVis(50);
        RechargePedestalBlockEntity.tick(level, h.absolutePos(p), te.getBlockState(), te);
        check(RechargeHelper.getCharge(te.item()) == 5 && Math.abs(aura.vis() - 45) < 0.01, "Pedestal did not move 5 vis: charge=" + RechargeHelper.getCharge(te.item()) + " vis=" + aura.vis());
        for (int i = 0; i < 9; i++) RechargePedestalBlockEntity.tick(level, h.absolutePos(p), te.getBlockState(), te);
        check(RechargeHelper.getCharge(te.item()) == 5, "Pedestal charged before 10 ticks");
        // charger needs a workbench below
        BlockPos w = new BlockPos(6, 1, 3);
        h.setBlock(w, ModBlocks.ARCANE_WORKBENCH.get());
        check(ModBlocks.ARCANE_WORKBENCH_CHARGER.get().defaultBlockState().canSurvive(level, h.absolutePos(w.above())), "Charger rejected workbench");
        check(!ModBlocks.ARCANE_WORKBENCH_CHARGER.get().defaultBlockState().canSurvive(level, h.absolutePos(w.above(2))), "Charger floats");
        // generator
        BlockPos g = new BlockPos(9, 1, 3);
        h.setBlock(g, ModBlocks.VIS_GENERATOR.get());
        var gen = h.getBlockEntity(g, VisGeneratorBlockEntity.class);
        var ga = NativeAura.get(level, level.getChunkAt(h.absolutePos(g)));
        ga.spend(ga.vis()); ga.addVis(10);
        VisGeneratorBlockEntity.tick(level, h.absolutePos(g), gen.getBlockState(), gen);
        check(gen.energy() == 1000 && Math.abs(ga.vis() - 9) < 0.01, "Generator did not convert 1 vis to 1000 FE: " + gen.energy());
        System.out.println("Auromancy devices passed: recharge pedestal 5 vis/10 ticks, charger placement, vis generator 1 vis -> 1000 FE.");
        h.succeed();
    }
}
