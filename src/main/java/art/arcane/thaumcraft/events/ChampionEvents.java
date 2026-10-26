package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.ITaintedMob;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.BrainyZombieEntity;
import art.arcane.thaumcraft.entity.monster.EldritchCrabEntity;
import art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity;
import art.arcane.thaumcraft.entity.monster.TaintacleEntity;
import art.arcane.thaumcraft.registry.ModAttributes;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * TC6 EntityEvents (champion half): spawning mobs may become champions, champion modifiers tick / strike / retaliate,
 * shielded champions and eldritch mobs flash runic shields, champions drop loot bags, zombies drop brains and creatures
 * dying of flux taint turn tainted.
 */
public final class ChampionEvents {
    /** ModConfig.CONFIG_WORLD.allowChampionMobs (default true). */
    public static boolean allowChampionMobs = true;
    /** ConfigEntities.championModWhitelist ("championWhiteList" IMC): class -> level. */
    public static final Map<Predicate<Entity>, Integer> CHAMPION_WHITELIST = new LinkedHashMap<>();
    static {
        CHAMPION_WHITELIST.put(e -> e instanceof Zombie, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof Spider, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof Blaze, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof EnderMan, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof Skeleton, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof Witch, 1);
        CHAMPION_WHITELIST.put(e -> e instanceof EldritchCrabEntity, 0);
        CHAMPION_WHITELIST.put(e -> e instanceof TaintacleEntity, 2);
        CHAMPION_WHITELIST.put(e -> e instanceof InhabitedZombieEntity, 3);
    }
    private static final TagKey<Biome> IS_SPOOKY = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_spooky"));

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChampionEvents::entitySpawns);
        NeoForge.EVENT_BUS.addListener(ChampionEvents::livingTick);
        NeoForge.EVENT_BUS.addListener(ChampionEvents::entityHurt);
        NeoForge.EVENT_BUS.addListener(ChampionEvents::livingDrops);
    }

    /** EntityEvents.entitySpawns. */
    private static void entitySpawns(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob creature)) return;
        if (ChampionModifier.championValue(creature) == ChampionModifier.TAINTED) {
            // a reloaded tainted creature gets its hostile AI back on its next tick
            AttributeInstance modai = creature.getAttribute(ModAttributes.TAINTED_MOD);
            if (modai != null) modai.removeModifier(ChampionModifier.IS_TAINTED);
        }
        if (!(creature instanceof Monster mob) || ChampionModifier.championValue(mob) >= -1) return;
        rollChampion((ServerLevel) event.getLevel(), mob);
    }

    /** The champion roll for a freshly spawned monster: champion when c <= 0, otherwise marked ordinary (-1). */
    public static void rollChampion(ServerLevel level, Monster mob) {
        int c = level.getRandom().nextInt(100);
        if (level.getDifficulty() == Difficulty.EASY || !allowChampionMobs) c += 2;
        if (level.getDifficulty() == Difficulty.HARD) c -= allowChampionMobs ? 2 : 0;
        var bg = level.getBiome(mob.blockPosition());
        if (bg.is(IS_SPOOKY) || bg.is(BiomeTags.IS_NETHER) || bg.is(BiomeTags.IS_END)) c -= allowChampionMobs ? 2 : 1;
        int cc = 0;
        boolean whitelisted = false;
        for (var en : CHAMPION_WHITELIST.entrySet()) {
            if (en.getKey().test(mob)) {
                whitelisted = true;
                if (!allowChampionMobs) continue;
                cc = Math.max(cc, en.getValue() - 1);
            }
        }
        c -= cc;
        if (whitelisted && c <= 0 && mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() >= 10.0) ChampionModifier.makeChampion(mob, false);
        else {
            AttributeInstance modai = mob.getAttribute(ModAttributes.CHAMPION_MOD);
            if (modai != null) { modai.removeModifier(ChampionModifier.ATTRIBUTE_MOD_NONE.id()); modai.addPermanentModifier(ChampionModifier.ATTRIBUTE_MOD_NONE); }
        }
    }

    /** EntityEvents.livingTick (type 0 effects) and RenderEventHandler.livingTick (the champion particles, client side). */
    private static void livingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof PathfinderMob mob) || !mob.isAlive()) return;
        ChampionModifier mod = ChampionModifier.of(mob);
        if (mod == null) return;
        if (mob.level() instanceof ServerLevel sl) { if (mod.type() == 0) mod.performEffect(sl, mob, null, null, 0.0f); }
        else art.arcane.thaumcraft.client.fx.ChampionFx.showFX(mod, mob);
    }

    /** EntityEvents.entityHurt (non-player victims). */
    private static void entityHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || victim instanceof Player) return;
        DamageSource src = event.getSource();
        if (victim.getHealth() < 2.0f && !victim.isInvertedHealAndHarm() && victim.isAlive() && !(victim instanceof ITaintedMob)
                && victim.hasEffect(ModEffects.FLUX_TAINT) && victim.getRandom().nextBoolean()) {
            ChampionModifier.makeTainted(victim);
            return;
        }
        if (!(victim instanceof Monster mob)) return;
        int t = ChampionModifier.championValue(mob);
        if (t >= 0 || mob instanceof EldritchMob) {
            if ((t == 5 || mob instanceof EldritchMob) && mob.getAbsorptionAmount() > 0.0f) {
                int target = src.getEntity() != null ? src.getEntity().getId() : -1;
                if (src.is(DamageTypes.FALL)) target = -2;
                if (src.is(DamageTypes.FALLING_BLOCK) || src.is(DamageTypes.FALLING_ANVIL)) target = -3;
                InfusionEnchantmentEvents.shieldFx(level, mob, target);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.get("runicshieldecharge"), SoundSource.HOSTILE, 0.66f, 1.1f + mob.getRandom().nextFloat() * 0.1f);
            } else if (t >= 0 && t < ChampionModifier.MODS.length && ChampionModifier.MODS[t].type() == 2 && src.getEntity() instanceof LivingEntity attacker) {
                event.setAmount(ChampionModifier.MODS[t].performEffect(level, mob, attacker, src, event.getAmount()));
            }
        }
        // (as in TC6 the attacking champion's own effects only fire inside this branch, i.e. against monster victims)
        if (event.getAmount() > 0.0f && src.getEntity() instanceof Monster attacker) {
            ChampionModifier am = ChampionModifier.of(attacker);
            if (am != null && am.type() == 1) event.setAmount(am.performEffect(level, attacker, victim, src, event.getAmount()));
        }
    }

    /** EntityEvents.livingDrops: champion xp and loot bag; zombie brains. */
    private static void livingDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) return;
        boolean fakeplayer = event.getSource().getEntity() instanceof FakePlayer;
        int looting = art.arcane.thaumcraft.util.EntityUtils.lootingLevel(level, event.getSource());
        int t = ChampionModifier.championValue(e);
        if (event.isRecentlyHit() && !fakeplayer && e instanceof Monster && t >= 0 && t != ChampionModifier.TAINTED) {
            ExperienceOrb.award(level, e.position(), 5 + level.getRandom().nextInt(3));
            int lb = Math.min(2, Mth.floor((level.getRandom().nextInt(9) + looting) / 5.0f));
            var bag = lb == 0 ? ModItems.LOOT_BAG_COMMON.get() : lb == 1 ? ModItems.LOOT_BAG_UNCOMMON.get() : ModItems.LOOT_BAG_RARE.get();
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), new ItemStack(bag)));
        }
        if (e instanceof Zombie && !(e instanceof BrainyZombieEntity) && event.isRecentlyHit() && level.getRandom().nextInt(10) - looting < 1)
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), new ItemStack(ModItems.BRAIN.get())));
    }

    private ChampionEvents() {}
}
