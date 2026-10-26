package art.arcane.thaumcraft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** Legacy food behavior for Triple Meat Treat. */
public final class TripleMeatTreatItem extends Item {
    public TripleMeatTreatItem(Item.Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(6)
                .saturationModifier(0.8F)
                .alwaysEdible()
                .build(), Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(
                        new MobEffectInstance(MobEffects.REGENERATION, 5, 0), 0.66F))
                .build()));
    }
}
