package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity;
import art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity;
import art.arcane.thaumcraft.entity.monster.MindSpiderEntity;
import art.arcane.thaumcraft.item.WarpingGear;
import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.progression.Warp;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.util.Baubles;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * TC6 common.lib.events.WarpEvents plus the warp parts of PlayerEvents (handleWarp, unnatural hunger food handling,
 * the "Warping" tooltip).
 */
public final class WarpEvents {
    private WarpEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WarpEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(WarpEvents::finishedUsingItem);
        NeoForge.EVENT_BUS.addListener(WarpEvents::usingItemTick);
        NeoForge.EVENT_BUS.addListener(WarpEvents::tooltip);
    }

    /** PlayerEvents.handleWarp. */
    private static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount > 0 && player.tickCount % 2000 == 0 && !player.hasEffect(ModEffects.WARP_WARD))
            checkWarpEvent(player, player.level().getRandom());
        if (player.tickCount % 20 == 0 && player.hasEffect(ModEffects.DEATH_GAZE)) checkDeathGaze(player);
    }

    /** What {@link #checkWarpEvent} did, for tests: the rolled effect value (or -1 when nothing fired) and the event. */
    public record Result(int eff, String event) {}

    /** The warp event picked by an effect roll (TC6 if/else chain, so 73..75 and 77..80 both give the long hunger); "none" for eff <= 0. */
    public static String eventFor(int eff) {
        if (eff <= 0) return "none";
        if (eff <= 4) return "creeper_hiss";
        if (eff <= 8) return "explosion_sound";
        if (eff <= 12) return "text_11";
        if (eff <= 16) return "vis_exhaust";
        if (eff <= 20) return "thaumarhia";
        if (eff <= 24) return "unnatural_hunger";
        if (eff <= 28) return "text_12";
        if (eff <= 32) return "mist";
        if (eff <= 36) return "blurred_vision";
        if (eff <= 40) return "sun_scorned";
        if (eff <= 44) return "mining_fatigue";
        if (eff <= 48) return "infectious_vis_exhaust";
        if (eff <= 52) return "night_vision";
        if (eff <= 56) return "death_gaze";
        if (eff <= 60) return "fake_spiders";
        if (eff <= 64) return "text_13";
        if (eff <= 68) return "mist_guardians";
        if (eff <= 72) return "blindness";
        if (eff == 76) return "clarity";
        if (eff <= 80) return "unnatural_hunger_long";
        if (eff <= 88) return "portal";
        if (eff <= 92) return "real_spiders";
        return "mist_many_guardians";
    }

    private static void status(ServerPlayer player, String key) {
        if (player.connection != null) player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private static void miscEvent(ServerPlayer player, int type) {
        if (player.connection != null)
            PacketDistributor.sendToPlayer(player, new FxPayload("misc_event", player.getX(), player.getY(), player.getZ(), List.of((float) type)));
    }

    /** Potion effect that neither milk nor anything but its own handler removes (TC6 getCurativeItems().clear()). */
    private static void addUncurable(ServerPlayer player, Holder<MobEffect> effect, int duration, int amp, boolean ambient) {
        player.addEffect(new MobEffectInstance(effect, duration, amp, ambient, true));
    }

    /** WarpEvents.checkWarpEvent. */
    public static Result checkWarpEvent(ServerPlayer player, RandomSource rand) {
        PlayerResearch wc = PlayerProgressManager.get(player);
        wc.addWarp(PlayerResearch.WARP_TEMPORARY, -1);
        int tw = wc.temporaryWarp(), nw = wc.normalWarp(), pw = wc.permanentWarp();
        int warp = tw + nw + pw;
        int actualwarp = pw + nw;
        int gearWarp = getWarpFromGear(player);
        warp += gearWarp;
        int warpCounter = wc.warpCounter();
        int r = rand.nextInt(100);
        if (!(warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter))) return new Result(-1, "none");
        warp = Math.min(100, (warp + warp + warpCounter) / 3);
        warpCounter -= (int) Math.max(5.0, Math.sqrt(warpCounter) * 2.0 - gearWarp * 2);
        wc.setWarpCounter(warpCounter);
        int eff = rand.nextInt(warp) + gearWarp;
        ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helm.is(ModItems.FORTRESS_HELM.get())) {
            var tag = helm.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains("mask") && tag.getIntOr("mask", -1) == 0) eff -= 2 + rand.nextInt(4);
        }
        miscEvent(player, 0);
        String event = eventFor(eff);
        ServerLevel level = player.level();
        int amp = Math.min(3, warp / 15);
        switch (event) {
            // World.playSound(player, ...) on the server plays for everyone nearby *except* that player (1:1).
            case "creeper_hiss" -> level.playSound(player, player.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.AMBIENT, 1.0f, 0.5f);
            case "explosion_sound" -> level.playSound(player, player.getX() + (rand.nextFloat() - rand.nextFloat()) * 10.0f,
                    player.getY() + (rand.nextFloat() - rand.nextFloat()) * 10.0f, player.getZ() + (rand.nextFloat() - rand.nextFloat()) * 10.0f,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 4.0f, (1.0f + (rand.nextFloat() - rand.nextFloat()) * 0.2f) * 0.7f);
            case "text_11" -> status(player, "warp.text.11");
            case "vis_exhaust" -> { addUncurable(player, ModEffects.VIS_EXHAUST, 5000, amp, true); status(player, "warp.text.1"); }
            case "thaumarhia" -> { addUncurable(player, ModEffects.THAUMARHIA, Math.min(32000, 10 * warp), 0, true); status(player, "warp.text.15"); }
            case "unnatural_hunger" -> { addUncurable(player, ModEffects.UNNATURAL_HUNGER, 5000, amp, true); status(player, "warp.text.2"); }
            case "text_12" -> status(player, "warp.text.12");
            case "mist" -> spawnMist(player, warp, 1, rand);
            case "blurred_vision" -> player.addEffect(new MobEffectInstance(ModEffects.BLURRED_VISION, Math.min(32000, 10 * warp), 0, true, true));
            case "sun_scorned" -> { addUncurable(player, ModEffects.SUN_SCORNED, 5000, amp, true); status(player, "warp.text.5"); }
            case "mining_fatigue" -> { player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 1200, amp, true, true)); status(player, "warp.text.9"); }
            case "infectious_vis_exhaust" -> { addUncurable(player, ModEffects.INFECTIOUS_VIS_EXHAUST, 6000, amp, false); status(player, "warp.text.1"); }
            case "night_vision" -> { player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true)); status(player, "warp.text.10"); }
            case "death_gaze" -> { addUncurable(player, ModEffects.DEATH_GAZE, 6000, amp, true); status(player, "warp.text.4"); }
            case "fake_spiders" -> suddenlySpiders(player, warp, false, rand);
            case "text_13" -> status(player, "warp.text.13");
            case "mist_guardians" -> spawnMist(player, warp, warp / 30, rand);
            case "blindness" -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
            case "clarity" -> { if (nw > 0) wc.addWarp(PlayerResearch.WARP_NORMAL, -1); status(player, "warp.text.14"); }
            case "unnatural_hunger_long" -> { addUncurable(player, ModEffects.UNNATURAL_HUNGER, 6000, amp, true); status(player, "warp.text.2"); }
            case "portal" -> spawnPortal(player, rand);
            case "real_spiders" -> suddenlySpiders(player, warp, true, rand);
            case "mist_many_guardians" -> spawnMist(player, warp, warp / 15, rand);
            default -> {}
        }
        if (actualwarp > 10 && !Warp.knows(player, "BATHSALTS") && !Warp.knows(player, "!BATHSALTS")) {
            status(player, "warp.text.8");
            Warp.completeResearch(player, "!BATHSALTS");
        }
        if (actualwarp > 25 && !Warp.knows(player, "ELDRITCHMINOR")) Warp.completeResearch(player, "ELDRITCHMINOR");
        if (actualwarp > 50 && !Warp.knows(player, "ELDRITCHMAJOR")) Warp.completeResearch(player, "ELDRITCHMAJOR");
        art.arcane.thaumcraft.research.ResearchEvents.markDirty(player);
        art.arcane.thaumcraft.research.ResearchEvents.sync(player);
        return new Result(eff, event);
    }

    private static void spawnMist(ServerPlayer player, int warp, int guardian, RandomSource rand) {
        miscEvent(player, 1);
        if (guardian > 0) {
            guardian = Math.min(8, guardian);
            for (int a = 0; a < guardian; ++a) spawnGuardian(player, rand);
        }
        status(player, "warp.text.6");
    }

    private static int offset(RandomSource rand) { return Mth.nextInt(rand, 7, 24) * Mth.nextInt(rand, -1, 1); }

    private static boolean freeSpace(ServerLevel level, Entity e) {
        return level.isUnobstructed(e) && level.noCollision(e, e.getBoundingBox()) && !level.containsAnyLiquid(e.getBoundingBox());
    }

    public static CultistPortalLesserEntity spawnPortal(ServerPlayer player, RandomSource rand) {
        ServerLevel level = player.level();
        CultistPortalLesserEntity eg = ModEntities.CULTIST_PORTAL_LESSER.get().create(level, EntitySpawnReason.EVENT);
        if (eg == null) return null;
        int i = Mth.floor(player.getX()), j = Mth.floor(player.getY()), k = Mth.floor(player.getZ());
        for (int l = 0; l < 50; ++l) {
            int i2 = i + offset(rand), j2 = j + offset(rand), k2 = k + offset(rand);
            eg.setPos(i2 + 0.5, j2 + 1.0, k2 + 0.5);
            BlockPos below = new BlockPos(i2, j2 - 1, k2);
            if (level.getBlockState(below).isSolidRender() && freeSpace(level, eg)) {
                eg.finalizeSpawn(level, level.getCurrentDifficultyAt(eg.blockPosition()), EntitySpawnReason.EVENT, null);
                level.addFreshEntity(eg);
                status(player, "warp.text.16");
                return eg;
            }
        }
        return null;
    }

    public static EldritchGuardianEntity spawnGuardian(ServerPlayer player, RandomSource rand) {
        ServerLevel level = player.level();
        EldritchGuardianEntity eg = ModEntities.ELDRITCH_GUARDIAN.get().create(level, EntitySpawnReason.EVENT);
        if (eg == null) return null;
        int i = Mth.floor(player.getX()), j = Mth.floor(player.getY()), k = Mth.floor(player.getZ());
        for (int l = 0; l < 50; ++l) {
            int i2 = i + offset(rand), j2 = j + offset(rand), k2 = k + offset(rand);
            BlockPos below = new BlockPos(i2, j2 - 1, k2);
            if (level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                eg.setPos(i2, j2, k2);
                if (freeSpace(level, eg)) {
                    eg.setTarget(player);
                    level.addFreshEntity(eg);
                    return eg;
                }
            }
        }
        return null;
    }

    public static List<MindSpiderEntity> suddenlySpiders(ServerPlayer player, int warp, boolean real, RandomSource rand) {
        ServerLevel level = player.level();
        List<MindSpiderEntity> out = new ArrayList<>();
        for (int spawns = Math.min(50, warp), a = 0; a < spawns; ++a) {
            MindSpiderEntity spider = ModEntities.MIND_SPIDER.get().create(level, EntitySpawnReason.EVENT);
            if (spider == null) continue;
            int i = Mth.floor(player.getX()), j = Mth.floor(player.getY()), k = Mth.floor(player.getZ());
            boolean success = false;
            for (int l = 0; l < 50; ++l) {
                int i2 = i + offset(rand), j2 = j + offset(rand), k2 = k + offset(rand);
                BlockPos below = new BlockPos(i2, j2 - 1, k2);
                if (level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                    spider.setPos(i2, j2, k2);
                    if (freeSpace(level, spider)) { success = true; break; }
                }
            }
            if (success) {
                spider.setTarget(player);
                if (!real) {
                    spider.setViewer(player.getName().getString());
                    spider.setHarmless(true);
                }
                level.addFreshEntity(spider);
                out.add(spider);
            }
        }
        status(player, "warp.text.7");
        return out;
    }

    /** WarpEvents.checkDeathGaze: withers living things the player looks at (range 8 + 3 per level, max 24). */
    public static void checkDeathGaze(ServerPlayer player) {
        MobEffectInstance pe = player.getEffect(ModEffects.DEATH_GAZE);
        if (pe == null) return;
        int lvl = pe.getAmplifier();
        int range = Math.min(8 + lvl * 3, 24);
        ServerLevel level = player.level();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(range, range, range))) {
            if (!entity.isPickable() || !(entity instanceof LivingEntity le) || !le.isAlive()) continue;
            if (!EntityUtils.isVisibleTo(0.75f, player, entity, (float) range)) continue;
            if (player.hasLineOfSight(entity) && (!(entity instanceof Player) || level.isPvpAllowed()) && !le.hasEffect(MobEffects.WITHER)) {
                le.setLastHurtByMob(player);
                le.setLastHurtMob(player);
                if (entity instanceof PathfinderMob creature) creature.setTarget(player);
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            }
        }
    }

    /** PlayerEvents.getFinalWarp: IWarpingGear plus the TC.WARP tag (warping infusion enchantment). */
    public static int getFinalWarp(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return 0;
        int warp = 0;
        if (stack.getItem() instanceof WarpingGear gear) warp += gear.getWarp(stack, player);
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("TC.WARP")) warp += tag.getByteOr("TC.WARP", (byte) 0);
        return warp;
    }

    /** WarpEvents.getWarpFromGear: main hand, armour and baubles. */
    public static int getWarpFromGear(Player player) {
        int w = getFinalWarp(player.getMainHandItem(), player);
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD })
            w += getFinalWarp(player.getItemBySlot(slot), player);
        for (Baubles.Worn worn : Baubles.worn(player)) w += getFinalWarp(worn.stack(), player);
        return w;
    }

    private static void tooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) return;
        int warp = getFinalWarp(event.getItemStack(), event.getEntity());
        if (warp > 0) event.getToolTip().add(Component.translatable("item.warping").append(" " + warp).withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Effects TC6 gives no curative items: milk must not remove them. */
    public static boolean milkProof(Holder<MobEffect> effect) {
        return effect.is(ModEffects.THAUMARHIA) || effect.is(ModEffects.UNNATURAL_HUNGER) || effect.is(ModEffects.SUN_SCORNED) || effect.is(ModEffects.DEATH_GAZE);
    }

    private static final Map<LivingEntity, List<MobEffectInstance>> MILK_SNAPSHOT = new WeakHashMap<>();

    private static boolean clearsEffects(ItemStack stack) {
        var c = stack.get(DataComponents.CONSUMABLE);
        return c != null && c.onConsumeEffects().stream().anyMatch(e -> e instanceof net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect);
    }

    private static void usingItemTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide() || event.getDuration() > 1 || !clearsEffects(event.getItem())) return;
        List<MobEffectInstance> keep = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (milkProof(i.getEffect())) keep.add(new MobEffectInstance(i));
        if (!keep.isEmpty()) MILK_SNAPSHOT.put(e, keep);
    }

    /** PlayerEvents.finishedUsingItem (unnatural hunger) and the milk restore above. */
    private static void finishedUsingItem(LivingEntityUseItemEvent.Finish event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) return;
        List<MobEffectInstance> keep = MILK_SNAPSHOT.remove(e);
        if (keep != null) for (MobEffectInstance i : keep) if (!e.hasEffect(i.getEffect())) e.addEffect(i);
        if (!e.hasEffect(ModEffects.UNNATURAL_HUNGER)) return;
        ItemStack item = event.getItem();
        if (item.is(Items.ROTTEN_FLESH) || item.is(ModItems.BRAIN.get())) {
            MobEffectInstance pe = e.getEffect(ModEffects.UNNATURAL_HUNGER);
            int amp = pe.getAmplifier() - 1;
            int duration = pe.getDuration() - 600;
            e.removeEffect(ModEffects.UNNATURAL_HUNGER);
            if (duration > 0 && amp >= 0) e.addEffect(new MobEffectInstance(ModEffects.UNNATURAL_HUNGER, duration, amp, true, false));
            if (e instanceof ServerPlayer sp && sp.connection != null) sp.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        } else if (item.has(DataComponents.FOOD)) {
            if (e instanceof ServerPlayer sp && sp.connection != null) sp.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
