package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemElementalHoe (Hoe of Growth): THAUMIUM_ELEMENTAL hoe with enchantability 5, rare. Tills a 3x3 area (sneak for a
 * single block); where nothing can be tilled it acts as bone meal on the block at three durability per use.
 */
public class ElementalHoeItem extends HoeItem {
    public ElementalHoeItem(Properties p) { super(ElementalGear.TOOL, -3.0F, 0.0F, p.enchantable(5)); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) return super.useOn(ctx);
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        boolean did = false;
        for (int xx = -1; xx <= 1; ++xx) for (int zz = -1; zz <= 1; ++zz) {
            BlockPos pp = pos.offset(xx, 0, zz);
            Vec3 hit = ctx.getClickLocation().add(xx, 0, zz);
            UseOnContext sub = new UseOnContext(world, player, ctx.getHand(), ctx.getItemInHand(), new BlockHitResult(hit, ctx.getClickedFace(), pp, ctx.isInside()));
            if (super.useOn(sub).consumesAction()) {
                if (world instanceof ServerLevel sl)
                    Fx.bamf(sl, pp.getX() + 0.5, pp.getY() + 1.01, pp.getZ() + 0.5, 0x4D1F1A, xx == 0 && zz == 0, false, Direction.UP);
                did = true;
            }
            if (ctx.getItemInHand().isEmpty()) break;
        }
        if (!did) {
            did = BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), world, pos, player);
            if (did) {
                if (player != null) ctx.getItemInHand().hurtAndBreak(3, player, ctx.getHand().asEquipmentSlot());
                if (!world.isClientSide()) world.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
