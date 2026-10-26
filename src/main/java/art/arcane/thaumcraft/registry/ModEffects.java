package art.arcane.thaumcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** TC6 potions used before the mid game: vis exhaust (caster discount -10% per level) and its infectious variant. */
public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "thaumcraft");

    static final class Plain extends MobEffect { Plain(int c) { super(MobEffectCategory.HARMFUL, c); } }
    static final class Good extends MobEffect { Good(int c) { super(MobEffectCategory.BENEFICIAL, c); } }

    /** PotionInfectiousVisExhaust: every 40 ticks spreads to living entities within 4 blocks, one level weaker. */
    static final class Infectious extends MobEffect {
        Infectious(int c) { super(MobEffectCategory.HARMFUL, c); }
        @Override public boolean shouldApplyEffectTickThisTick(int tick, int amp) { return tick % 40 == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amp) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0))) {
                if (e.hasEffect(INFECTIOUS_VIS_EXHAUST)) continue;
                if (amp > 0) e.addEffect(new MobEffectInstance(INFECTIOUS_VIS_EXHAUST, 6000, amp - 1, false, true));
                else e.addEffect(new MobEffectInstance(VIS_EXHAUST, 6000, 0, false, true));
            }
            return true;
        }
    }

    public static final DeferredHolder<MobEffect, MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust", () -> new Plain(0x664477));
    public static final DeferredHolder<MobEffect, MobEffect> INFECTIOUS_VIS_EXHAUST = EFFECTS.register("infectious_vis_exhaust", () -> new Infectious(0x665577));

    /** PotionFluxTaint: hurts (taint damage) every 40>>level ticks; heals tainted creatures. */
    static final class FluxTaint extends MobEffect {
        FluxTaint(int c) { super(MobEffectCategory.HARMFUL, c); }
        @Override public boolean shouldApplyEffectTickThisTick(int tick, int amp) { int k = 40 >> amp; return k <= 0 || tick % k == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amp) {
            if (target instanceof art.arcane.thaumcraft.entity.ITaintedMob) target.heal(1);
            else if (!target.isInvertedHealAndHarm() && (target.getMaxHealth() > 1 || target instanceof net.minecraft.world.entity.player.Player))
                target.hurtServer(level, level.damageSources().source(TAINT), 1);
            return true;
        }
    }
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> TAINT = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "taint"));
    public static final DeferredHolder<MobEffect, MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint", () -> new FluxTaint(0x663377));

    /** PotionWarpWard: blocks warp events while active (granted by purifying fluid). */
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = EFFECTS.register("warp_ward", () -> new Good(0xE0F3F7));

    /** PotionUnnaturalHunger: every tick adds 0.025 * (level + 1) exhaustion to players. */
    static final class UnnaturalHunger extends MobEffect {
        UnnaturalHunger(int c) { super(MobEffectCategory.HARMFUL, c); }
        @Override public boolean shouldApplyEffectTickThisTick(int tick, int amp) { return true; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amp) {
            if (target instanceof net.minecraft.world.entity.player.Player p) p.causeFoodExhaustion(0.025f * (amp + 1));
            return true;
        }
    }
    /** PotionSunScorned: every 40 ticks bright sky light sets the target on fire, darkness slowly heals it. */
    static final class SunScorned extends MobEffect {
        SunScorned(int c) { super(MobEffectCategory.HARMFUL, c); }
        @Override public boolean shouldApplyEffectTickThisTick(int tick, int amp) { return tick % 40 == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amp) {
            float f = target.getLightLevelDependentMagicValue();
            var r = level.getRandom();
            if (f > 0.5f && r.nextFloat() * 30.0f < (f - 0.4f) * 2.0f && level.canSeeSky(net.minecraft.core.BlockPos.containing(target.getX(), target.getY(), target.getZ())))
                target.igniteForSeconds(4);
            else if (f < 0.25f && r.nextFloat() > f * 2.0f) target.heal(1.0f);
            return true;
        }
    }
    /** PotionThaumarhia: every 20 ticks, 1 in 15 chance to leave flux goo where the target stands. */
    static final class Thaumarhia extends MobEffect {
        Thaumarhia(int c) { super(MobEffectCategory.HARMFUL, c); }
        @Override public boolean shouldApplyEffectTickThisTick(int tick, int amp) { return tick % 20 == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity target, int amp) {
            var pos = target.blockPosition();
            if (level.getRandom().nextInt(15) == 0 && level.isEmptyBlock(pos)) level.setBlockAndUpdate(pos, art.arcane.thaumcraft.ModBlocks.FLUX_GOO.get().defaultBlockState());
            return true;
        }
    }
    /** PotionUnnaturalHunger (registry "unnaturalHunger", colour 4482611, icon 7,1). */
    public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger", () -> new UnnaturalHunger(4482611));
    /** PotionDeathGaze (colour 6702131, icon 4,2): withers living things the player looks at, see WarpEvents.checkDeathGaze. */
    public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze", () -> new Plain(6702131));
    /** PotionBlurredVision (colour 8421504, icon 5,2): client blur post effect only. */
    public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision", () -> new Plain(8421504));
    /** PotionSunScorned (colour 16308330, icon 6,2). */
    public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned", () -> new SunScorned(16308330));
    /** PotionThaumarhia (colour 6702199, icon 7,2). */
    public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia", () -> new Thaumarhia(6702199));

    public static void register(IEventBus bus) { EFFECTS.register(bus); }
    private ModEffects() {}
}
