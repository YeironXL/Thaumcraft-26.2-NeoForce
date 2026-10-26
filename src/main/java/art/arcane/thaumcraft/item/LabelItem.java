package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** TC6 ItemLabel: used on a jar it marks the jar with the label's aspect (or the jar's current essentia). */
public final class LabelItem extends Item {
    public LabelItem(Properties p) { super(p); }
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof EssentiaJarBlockEntity jar) || jar.alembic()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var stack = ctx.getItemInHand();
        if (jar.applyLabel(stack.get(ModComponents.ASPECT.get()))) {
            if (ctx.getPlayer() != null) jar.setFacing(ctx.getPlayer().getDirection().getOpposite());
            level.playSound(null, ctx.getClickedPos(), ModSounds.get("jar"), SoundSource.BLOCKS, 0.4f, 1.0f);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
