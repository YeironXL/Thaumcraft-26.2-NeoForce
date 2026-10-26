package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.ai.CultistHurtByTargetGoal;
import java.util.Optional;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** TC6 EntityCultistKnight: crimson plate armour and a sword (rarely thaumium or void). */
public class CultistKnightEntity extends CultistEntity {
    public CultistKnightEntity(EntityType<? extends CultistKnightEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() { return CultistEntity.createAttributes().add(Attributes.MAX_HEALTH, 30.0); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(4, new art.arcane.thaumcraft.entity.ai.RestrictOpenDoorGoal(this));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EldritchGuardianEntity.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    @Override
    protected void setLoot(DifficultyInstance diff) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PLATE_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PLATE_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PLATE_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
        if (random.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.05f : 0.01f)) {
            int i = random.nextInt(5);
            if (i == 0) {
                setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOID_SWORD.get()));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_ROBE_HELM.get()));
            } else {
                setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THAUMIUM_SWORD.get()));
                if (random.nextBoolean()) setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
        } else setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance diff) {
        float f = diff.getSpecialMultiplier();
        if (!getMainHandItem().isEmpty() && random.nextFloat() < 0.25f * f)
            setItemInHand(InteractionHand.MAIN_HAND, EnchantmentHelper.enchantItem(random, getMainHandItem(), (int) (5.0f + f * random.nextInt(18)), level().registryAccess(), Optional.empty()));
    }
}
