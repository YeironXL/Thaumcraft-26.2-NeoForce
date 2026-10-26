package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Thaumostatic Harness, recreated from TC4 (ItemHoverHarness; TC6 dropped it). Chest armour, 3 armour points and 400
 * durability (FTB wiki). It stores raw Potentia essentia drawn from warded jars and, while switched on (H) and fuelled,
 * grants flight that is kept on the ground. Flying burns one Potentia per second (TC5 rate, the TC4 one is not documented).
 * Flight is granted and revoked on the server only; when it is revoked mid-air the wearer gets a short Slow Falling so the
 * landing is safe (port addition). See docs/ARNES_FUENTES.md.
 */
public class HoverHarnessItem extends Item {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(25,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3, ArmorType.HELMET, 1, ArmorType.BODY, 3),
            25, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:hover_harness")));
    public static final int MAX_FUEL = 500;
    public static final int TICKS_PER_FUEL = 20;
    public static final String GRANTED = "thaumcraft.harness_flight";

    public HoverHarnessItem(Properties p) { super(p.humanoidArmor(MATERIAL, ArmorType.CHESTPLATE).durability(400).rarity(Rarity.RARE)); }

    private static net.minecraft.nbt.CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int getFuel(ItemStack s) { return tag(s).getIntOr("potentia", 0); }
    public static void setFuel(ItemStack s, int v) { int f = Math.max(0, Math.min(MAX_FUEL, v)); CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt("potentia", f)); }
    public static boolean isActive(ItemStack s) { return tag(s).getBooleanOr("active", true); }
    public static void setActive(ItemStack s, boolean on) { CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean("active", on)); }

    public static ItemStack worn(Player p) {
        ItemStack s = p.getItemBySlot(EquipmentSlot.CHEST);
        return s.getItem() instanceof HoverHarnessItem ? s : ItemStack.EMPTY;
    }

    /** Draws as much Potentia as fits from a jar; returns the amount moved. */
    public static int fillFromJar(ItemStack harness, EssentiaJarBlockEntity jar) {
        if (!"potentia".equals(jar.aspect())) return 0;
        int n = Math.min(MAX_FUEL - getFuel(harness), jar.contents().amount());
        if (n <= 0 || !jar.extract(n)) return 0;
        setFuel(harness, getFuel(harness) + n);
        return n;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof EssentiaJarBlockEntity jar)) return InteractionResult.PASS;
        if (!"potentia".equals(jar.aspect()) || getFuel(ctx.getItemInHand()) >= MAX_FUEL) return InteractionResult.FAIL;
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (fillFromJar(ctx.getItemInHand(), jar) > 0)
            ctx.getLevel().playSound(null, ctx.getClickedPos(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.5f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    public static void toggle(Player p) {
        ItemStack s = worn(p);
        if (s.isEmpty()) return;
        boolean on = !isActive(s);
        setActive(s, on);
        p.level().playSound(null, p.blockPosition(), on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4f, 1.6f);
        if (p instanceof net.minecraft.server.level.ServerPlayer sp && sp.connection != null) sp.sendOverlayMessage(Component.translatable(on ? "tc.harness.on" : "tc.harness.off"));
    }

    /** Run every player tick on both sides. */
    public static void tick(Player p) {
        ItemStack s = worn(p);
        boolean canFly = !s.isEmpty() && isActive(s) && getFuel(s) > 0;
        if (p.level().isClientSide()) {
            if (canFly && p.getAbilities().flying && p.tickCount % 2 == 0)
                p.level().addParticle(ParticleTypes.END_ROD, p.getX() + (p.getRandom().nextDouble() - 0.5) * 0.5, p.getY() + 0.7, p.getZ() + (p.getRandom().nextDouble() - 0.5) * 0.5, 0, -0.05, 0);
            return;
        }
        var data = p.getPersistentData();
        boolean granted = data.getBooleanOr(GRANTED, false);
        boolean natural = p.isCreative() || p.isSpectator();
        if (canFly) {
            if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
            if (!granted && !natural) data.putBoolean(GRANTED, true);
            boolean girdle = HoverGirdleItem.isWorn(p);
            if (p.getAbilities().flying && !natural) {
                p.resetFallDistance();
                if (p.tickCount % (girdle ? HoverGirdleItem.TICKS_PER_FUEL : TICKS_PER_FUEL) == 0) setFuel(s, getFuel(s) - 1);
            }
            setBoost(p, girdle && p.getAbilities().flying && !natural);
        } else {
            setBoost(p, false);
        }
        if (!canFly && granted) {
            data.remove(GRANTED);
            if (!natural) {
                boolean wasFlying = p.getAbilities().flying;
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                if (wasFlying || !p.onGround()) { p.resetFallDistance(); p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false)); }
            }
        }
    }

    /** Thaumostatic Girdle speed: raised only while harness-flying, restored to vanilla otherwise (only if we raised it). */
    private static void setBoost(Player p, boolean on) {
        var data = p.getPersistentData();
        boolean was = data.getBooleanOr(HoverGirdleItem.BOOSTED, false);
        if (on && !was) { data.putBoolean(HoverGirdleItem.BOOSTED, true); p.getAbilities().setFlyingSpeed(HoverGirdleItem.FLY_SPEED); p.onUpdateAbilities(); }
        else if (!on && was) { data.remove(HoverGirdleItem.BOOSTED); p.getAbilities().setFlyingSpeed(HoverGirdleItem.BASE_FLY_SPEED); p.onUpdateAbilities(); }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tc.harness.fuel", getFuel(stack), MAX_FUEL).withStyle(ChatFormatting.AQUA));
        lines.accept(Component.translatable(isActive(stack) ? "tc.harness.on" : "tc.harness.off").withStyle(ChatFormatting.GRAY));
    }
}
