package art.arcane.thaumcraft.entity.champion;

import art.arcane.thaumcraft.entity.ITaintedMob;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModAttributes;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;

/**
 * TC6 entities.monster.mods: the fourteen champion modifiers. A mob's CHAMPION_MOD attribute is -2 until it has been
 * considered, -1 for an ordinary mob and the modifier id for a champion. Types: -1 passive (attribute buffs only),
 * 0 ticking, 1 on hitting, 2 on being hit.
 */
public record ChampionModifier(int id, String name, int type) {
    public static final ChampionModifier[] MODS = {
            new ChampionModifier(0, "bold", -1), new ChampionModifier(1, "spine", 2), new ChampionModifier(2, "armor", 2),
            new ChampionModifier(3, "mighty", -1), new ChampionModifier(4, "grim", 1), new ChampionModifier(5, "warded", 0),
            new ChampionModifier(6, "warp", 1), new ChampionModifier(7, "undying", 0), new ChampionModifier(8, "fiery", 1),
            new ChampionModifier(9, "sickly", 1), new ChampionModifier(10, "venomous", 1), new ChampionModifier(11, "vampiric", 1),
            new ChampionModifier(12, "infested", 2), new ChampionModifier(13, "tainted", 0)};
    public static final int TAINTED = 13;

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("thaumcraft", path); }
    /** ChampionModifier.attributeMod: CHAMPION_MOD + (id + 2). */
    public AttributeModifier attributeMod() { return new AttributeModifier(id("champion_" + name), id + 2, AttributeModifier.Operation.ADD_VALUE); }
    public static final AttributeModifier ATTRIBUTE_MOD_NONE = new AttributeModifier(id("champion_normal"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    public static final AttributeModifier ATTRIBUTE_MINUS_ONE = new AttributeModifier(id("champion_minus1"), -1.0, AttributeModifier.Operation.ADD_VALUE);
    /** EntityUtils.CHAMPION_HEALTH / CHAMPION_DAMAGE / BOLDBUFF / MIGHTYBUFF / HPBUFF[5]. */
    public static final AttributeModifier CHAMPION_HEALTH = new AttributeModifier(id("champion_health"), 100.0, AttributeModifier.Operation.ADD_VALUE);
    public static final AttributeModifier CHAMPION_DAMAGE = new AttributeModifier(id("champion_damage"), 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final AttributeModifier BOLDBUFF = new AttributeModifier(id("champion_bold_speed"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    public static final AttributeModifier MIGHTYBUFF = new AttributeModifier(id("champion_mighty_damage"), 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final AttributeModifier DMGBUFF_1 = new AttributeModifier(id("damage_buff_1"), 0.5, AttributeModifier.Operation.ADD_VALUE);
    public static final AttributeModifier TAINT_HEALTH = new AttributeModifier(id("health_buff_6"), 25.0, AttributeModifier.Operation.ADD_VALUE);
    /** ChampionModTainted "istainted": 1 once a creature's AI has been replaced by the hostile tainted AI. */
    public static final Identifier IS_TAINTED = id("istainted");

    /** 1.12 absorption had no cap; modern max_absorption must allow the warded shield. */
    private static void raiseAbsorptionCap(LivingEntity e, double v) {
        var a = e.getAttribute(Attributes.MAX_ABSORPTION);
        if (a != null && a.getBaseValue() < v) a.setBaseValue(v);
    }

    public Component displayName() { return Component.translatable("champion.mod." + name); }

    /** The champion modifier id of a living entity, -2/-1 when it is none. */
    public static int championValue(LivingEntity e) {
        AttributeInstance a = e.getAttribute(ModAttributes.CHAMPION_MOD);
        return a == null ? -2 : (int) a.getValue();
    }
    public static ChampionModifier of(LivingEntity e) {
        int t = championValue(e);
        return t >= 0 && t < MODS.length ? MODS[t] : null;
    }

    private static void apply(AttributeInstance ai, AttributeModifier mod) {
        if (ai == null) return;
        ai.removeModifier(mod.id());
        ai.addPermanentModifier(mod);
    }

    /** EntityUtils.makeChampion: a random modifier (always bold for creepers), +100 health, x3 damage and the champion name. */
    public static void makeChampion(Monster entity, boolean persist) { makeChampion(entity, persist, -1); }

    public static void makeChampion(Monster entity, boolean persist, int forced) {
        AttributeInstance modai = entity.getAttribute(ModAttributes.CHAMPION_MOD);
        if (modai == null || modai.getValue() > -2.0) return;
        int type = forced >= 0 ? forced : entity.getRandom().nextInt(MODS.length);
        if (entity instanceof Creeper) type = 0;
        apply(modai, MODS[type].attributeMod());
        apply(entity.getAttribute(Attributes.MAX_HEALTH), CHAMPION_HEALTH);
        apply(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHAMPION_DAMAGE);
        entity.heal(25.0f);
        entity.setCustomName(MODS[type].displayName().copy().append(" ").append(entity.getName()));
        if (persist) entity.setPersistenceRequired();
        switch (type) {
            case 0 -> apply(entity.getAttribute(Attributes.MOVEMENT_SPEED), BOLDBUFF);
            case 3 -> apply(entity.getAttribute(Attributes.ATTACK_DAMAGE), MIGHTYBUFF);
            case 5 -> {
                int bh = (int) entity.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / 2;
                raiseAbsorptionCap(entity, bh);
                entity.setAbsorptionAmount(entity.getAbsorptionAmount() + bh);
            }
            default -> {}
        }
    }

    /** EntityUtils.makeTainted: the tainted modifier (13) on an ordinary creature dying of flux taint, +25 health. */
    public static void makeTainted(LivingEntity target) {
        AttributeInstance modai = target.getAttribute(ModAttributes.CHAMPION_MOD);
        if (modai == null || modai.getValue() > -1.0) return;
        if (modai.getValue() == -1.0) apply(modai, ATTRIBUTE_MINUS_ONE);
        apply(modai, MODS[TAINTED].attributeMod());
        apply(target.getAttribute(Attributes.MAX_HEALTH), TAINT_HEALTH);
        AttributeInstance dmg = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            if (ModAttributes.ADDED_ATTACK_DAMAGE.contains(target.getType())) dmg.setBaseValue(Math.max(2.0f, (target.getBbHeight() + target.getBbWidth()) * 2.0f));
            else apply(dmg, DMGBUFF_1);
        }
        target.heal(25.0f);
    }

    /** ChampionModTainted.resetAI: a non-monster creature gets a plain hostile melee AI once. */
    public static void resetAI(PathfinderMob critter) {
        AttributeInstance modai = critter.getAttribute(ModAttributes.TAINTED_MOD);
        if (modai == null) return;
        if (!(critter instanceof Monster) && modai.getValue() == 0.0) {
            critter.goalSelector.removeAllGoals(g -> true);
            critter.targetSelector.removeAllGoals(g -> true);
            critter.goalSelector.addGoal(0, new FloatGoal(critter));
            critter.goalSelector.addGoal(2, new MeleeAttackGoal(critter, 1.2, false));
            critter.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(critter, 1.0));
            critter.goalSelector.addGoal(7, new RandomStrollGoal(critter, 1.0));
            critter.goalSelector.addGoal(8, new LookAtPlayerGoal(critter, Player.class, 8.0f));
            critter.goalSelector.addGoal(8, new RandomLookAroundGoal(critter));
            critter.targetSelector.addGoal(1, new HurtByTargetGoal(critter).setAlertOthers(ZombifiedPiglin.class));
            critter.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(critter, Player.class, true));
            modai.removeModifier(IS_TAINTED);
            modai.addPermanentModifier(new AttributeModifier(IS_TAINTED, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
        // (the attack damage attribute TC6 registers here when missing is added to every type up front, see ModAttributes)
    }

    /**
     * IChampionModifierEffect.performEffect (server side). Type 0 runs every tick with no target, type 1 when the champion
     * hurts something, type 2 when it is hurt by a living attacker. Returns the (possibly changed) damage.
     */
    public float performEffect(ServerLevel level, LivingEntity mob, LivingEntity target, DamageSource source, float amount) {
        var rand = mob.getRandom();
        switch (id) {
            case 0, 3 -> { return 0.0f; } // bold, mighty: attribute buffs only (TC6 returns 0, but they are never called)
            case 1 -> { // spined: thorns back at the attacker
                if (target == null || source.is(DamageTypes.THORNS)) return amount;
                target.hurtServer(level, level.damageSources().thorns(mob), 1 + rand.nextInt(3));
                target.playSound(SoundEvents.THORNS_HIT, 0.5f, 1.0f);
                return amount;
            }
            case 2 -> { // armored: blockable damage x19/25
                if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) amount = amount * 19.0f / 25.0f;
                return amount;
            }
            case 4 -> { if (rand.nextFloat() < 0.4f) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200)); return amount; }
            case 5 -> { // warded: regrow absorption up to half the base health
                if (mob.invulnerableTime <= 0 && mob.tickCount % 25 == 0) {
                    int bh = (int) mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / 2;
                    raiseAbsorptionCap(mob, bh);
                    if (mob.getAbsorptionAmount() < bh) mob.setAbsorptionAmount(mob.getAbsorptionAmount() + 1.0f);
                }
                return amount;
            }
            case 6 -> {
                if (rand.nextFloat() < 0.33f && target instanceof ServerPlayer p)
                    PlayerProgressManager.addWarp(p, PlayerProgressManager.WarpType.TEMPORARY, 1 + rand.nextInt(3));
                return amount;
            }
            case 7 -> { if (mob.tickCount % 20 == 0) mob.heal(1.0f); return amount; }
            case 8 -> { if (rand.nextFloat() < 0.4f) target.igniteForSeconds(4); return amount; }
            case 9 -> { if (rand.nextFloat() < 0.4f) target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 500)); return amount; }
            case 10 -> { if (rand.nextFloat() < 0.4f) target.addEffect(new MobEffectInstance(MobEffects.POISON, 100)); return amount; }
            case 11 -> { mob.heal(Math.max(2.0f, amount / 2.0f)); return amount; }
            case 12 -> { // infested: a taint crawler bursts out
                if (rand.nextFloat() < 0.4f) {
                    var spiderling = ModEntities.TAINT_CRAWLER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (spiderling != null) {
                        spiderling.snapTo(mob.getX(), mob.getY() + mob.getBbHeight() / 2.0f, mob.getZ(), rand.nextFloat() * 360.0f, 0.0f);
                        level.addFreshEntity(spiderling);
                        mob.playSound(ModSounds.get("gore"), 0.5f, 1.0f);
                    }
                }
                return amount;
            }
            case 13 -> { if (mob instanceof PathfinderMob c && !(mob instanceof ITaintedMob)) resetAI(c); return amount; }
            default -> { return amount; }
        }
    }
}
