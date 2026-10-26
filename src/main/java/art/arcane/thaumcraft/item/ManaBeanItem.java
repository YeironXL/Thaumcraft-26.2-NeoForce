package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.ManaPodBlockEntity;
import art.arcane.thaumcraft.registry.ModComponents;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * TC4 ItemManaBean recreation: a bean holding one aspect (crucible value 1 of that aspect), edible (always) with a short
 * random potion effect, and plantable on the underside of a greatwood/silverwood log (or any log in a magical biome).
 */
public final class ManaBeanItem extends Item {
    public ManaBeanItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).alwaysEdible().build()));
    }

    public static String aspect(ItemStack stack) {
        if (!stack.is(ModItems.MANA_BEAN.get())) return "";
        String value = stack.get(ModComponents.ASPECT.get());
        return value != null && AspectRegistry.get(value) != null ? value : "";
    }

    public static ItemStack of(String aspect, int count) {
        ItemStack stack = new ItemStack(ModItems.MANA_BEAN.get(), count);
        if (AspectRegistry.get(aspect) != null) stack.set(ModComponents.ASPECT.get(), aspect);
        return stack;
    }

    @Override public Component getName(ItemStack stack) {
        String a = aspect(stack);
        return a.isEmpty() ? super.getName(stack) : Component.translatable("item.thaumcraft.mana_bean.aspect", Component.translatable(a));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getClickedFace() != Direction.DOWN) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos target = ctx.getClickedPos().below();
        var pod = ModBlocks.MANA_POD.get().defaultBlockState();
        if (!level.getBlockState(target).canBeReplaced() || !pod.canSurvive(level, target)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(target, pod, 3);
        if (level.getBlockEntity(target) instanceof ManaPodBlockEntity be) be.setAspect(aspect(ctx.getItemInHand()));
        level.playSound(null, target, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            var effects = new ArrayList<>(BuiltInRegistries.MOB_EFFECT.listElements().filter(h -> !h.value().isInstantaneous()).toList());
            if (!effects.isEmpty() && level.getRandom().nextFloat() < 0.6f)
                entity.addEffect(new MobEffectInstance(effects.get(level.getRandom().nextInt(effects.size())), 100 + level.getRandom().nextInt(200), 0));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
