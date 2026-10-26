package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * TC6 ItemSanitySoap: scrub for 5 seconds; it is released after 95 ticks, used up, and removes all temporary warp
 * and 1 normal warp (+1 under Warp Ward, +1 standing in purifying fluid).
 */
public final class SanitySoapItem extends Item {
    private static final int USE_DURATION_TICKS = 100;

    public SanitySoapItem(Item.Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_DURATION_TICKS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BLOCK; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
        int ticks = getUseDuration(stack, player) - count;
        if (ticks > 95) { player.releaseUsingItem(); return; }
        if (level.isClientSide()) {
            var r = level.getRandom();
            if (r.nextFloat() < 0.2f)
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.PLAYERS, 0.1f, 1.5f + r.nextFloat() * 0.2f, false);
            for (int a = 0; a < 10; ++a)
                art.arcane.thaumcraft.client.fx.ClientHooks.crucibleBubble((float) player.getX() - 0.5f + r.nextFloat(),
                        (float) player.getBoundingBox().minY + r.nextFloat() * player.getBbHeight(), (float) player.getZ() - 0.5f + r.nextFloat(), 1.0f, 0.8f, 0.9f);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity player, int timeLeft) {
        int qq = getUseDuration(stack, player) - timeLeft;
        if (qq <= 95 || !(player instanceof Player)) return false;
        stack.shrink(1);
        if (player instanceof ServerPlayer sp) {
            int amt = 1;
            if (sp.hasEffect(ModEffects.WARP_WARD)) ++amt;
            BlockPos pos = new BlockPos(Mth.floor(sp.getX()), Mth.floor(sp.getY()), Mth.floor(sp.getZ()));
            if (level.getBlockState(pos).is(ModBlocks.PURIFYING_FLUID.get())) ++amt;
            if (PlayerProgressManager.getWarp(sp, PlayerProgressManager.WarpType.NORMAL) > 0)
                PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.NORMAL, -amt);
            int temp = PlayerProgressManager.getWarp(sp, PlayerProgressManager.WarpType.TEMPORARY);
            if (temp > 0) PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.TEMPORARY, -temp);
        } else if (level.isClientSide()) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.get("craftstart"), SoundSource.PLAYERS, 0.25f, 1.0f, false);
            var r = level.getRandom();
            for (int a = 0; a < 40; ++a)
                art.arcane.thaumcraft.client.fx.ClientHooks.crucibleBubble((float) player.getX() - 0.5f + r.nextFloat() * 1.5f,
                        (float) player.getBoundingBox().minY + r.nextFloat() * player.getBbHeight(), (float) player.getZ() - 0.5f + r.nextFloat() * 1.5f, 1.0f, 0.7f, 0.9f);
        }
        return true;
    }
}
