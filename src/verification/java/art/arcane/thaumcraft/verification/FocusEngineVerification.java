package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.casters.foci.*;
import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.item.FocusItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** TC6 focus engine: package build/serialize, focus mounting in caster, real touch-fire cast with vis cost. */
final class FocusEngineVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var pkg = new FocusPackage();
        pkg.addNode(new FocusMediumRoot());
        pkg.addNode(new FocusMediumTouch());
        pkg.addNode(new FocusEffectFire());
        pkg.setComplexity(4);
        var focus = new ItemStack(ModItems.FOCUS_1.get());
        FocusItem.setPackage(focus, pkg);
        var back = FocusItem.getPackage(focus);
        check(back != null && back.nodes.size() == 3 && FocusEngine.doesPackageContainElement(back, "thaumcraft.FIRE"), "Focus package did not survive serialization");
        var caster = new ItemStack(ModItems.CASTER_BASIC.get());
        var ci = (CasterItem) caster.getItem();
        ci.setFocus(caster, focus);
        check(ci.getFocus(caster) != null, "Focus not mounted in caster");
        var rel = new BlockPos(4, 1, 4);
        for (int y = 1; y <= 3; y++) for (int z = 4; z <= 8; z++) h.setBlock(new BlockPos(4, y, z), net.minecraft.world.level.block.Blocks.AIR);
        var pos = h.absolutePos(rel);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5); player.setYRot(0); player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, caster);
        var victim = (net.minecraft.world.entity.Mob) h.spawn(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:husk")), rel.offset(0, 0, 2));
        victim.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5); victim.setNoAi(true); victim.setHealth(20);
        var aura = NativeAura.get(level, level.getChunkAt(pos)); aura.spend(aura.vis()); aura.addVis(10);
        var r = ci.use(level, player, InteractionHand.MAIN_HAND);
        check(r.consumesAction(), "Cast failed: " + r);
        check(victim.getHealth() < 20 && victim.getRemainingFireTicks() > 0, "Touch fire did not hit: hp=" + victim.getHealth());
        check(Math.abs(aura.vis() - 9.2f) < 0.001, "Touch fire did not cost 0.8 vis: " + aura.vis());
        check(CasterManager.isOnCooldown(player), "No cast cooldown");
        System.out.println("Focus engine passed: package serialization, mounting, touch-fire cast hp=" + victim.getHealth() + " vis left=" + aura.vis());
        h.succeed();
    }
}
