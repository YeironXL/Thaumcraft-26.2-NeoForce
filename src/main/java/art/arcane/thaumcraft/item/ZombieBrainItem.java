package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

/** TC6 ItemZombieBrain: food (4, 0.2) with 80% hunger; eating it adds 1 normal warp (10%) or 1-3 temporary warp. */
public final class ZombieBrainItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.2f).build();
    public static final Consumable CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0), 0.8f)).build();

    public ZombieBrainItem(Item.Properties properties) { super(properties.food(FOOD, CONSUMABLE)); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (level.getRandom().nextFloat() < 0.1f) PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.NORMAL, 1);
            else PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.TEMPORARY, 1 + level.getRandom().nextInt(3));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
