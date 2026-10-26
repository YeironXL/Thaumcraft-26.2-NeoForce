package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.golems.seals.ISeal;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

/** TC6 ItemSealPlacer: one item per seal type (the blank seal has no key); sticks the seal on the clicked face. */
public class SealPlacerItem extends Item implements ISealDisplayer {
    private final @Nullable String sealKey;
    public SealPlacerItem(Properties p, @Nullable String sealKey) { super(p.stacksTo(64)); this.sealKey = sealKey; }
    public @Nullable String sealKey() { return sealKey; }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (ctx.getLevel().isClientSide() || sealKey == null || (player != null && player.isShiftKeyDown())) return InteractionResult.PASS;
        if (player != null && !player.mayUseItemAt(ctx.getClickedPos(), ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
        ISeal seal = SealHandler.newSeal(sealKey);
        if (seal == null || !seal.canPlaceAt(ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace())) return InteractionResult.FAIL;
        if (player != null && SealHandler.addSealEntity(ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace(), seal, player) && !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
    @Override public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) { return true; }
}
