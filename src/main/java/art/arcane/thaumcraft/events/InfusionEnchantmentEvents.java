package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.infusion.InfusionEnchantment;
import art.arcane.thaumcraft.infusion.InfusionRunicAugmentRecipe;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.scan.ScanManager;
import art.arcane.thaumcraft.util.Baubles;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * TC6 ToolEvents (infusion enchantment effects), the infusion enchantment / runic charge tooltip lines of
 * PlayerEvents.tooltipEvent and PlayerEvents.handleRunicArmor (runic shielding recharge).
 */
public final class InfusionEnchantmentEvents {
    private static final Map<UUID, Direction> LAST_FACE_CLICKED = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean blockDestructiveRecursion;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::playerAttack);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::leftClickBlock);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::breakBlock);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::harvestDrops);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::livingDrops);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::tooltip);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(InfusionEnchantmentEvents::incomingDamage);
    }

    // ------------------------------------------------------------------ arcing

    /** ToolEvents.playerAttack: ARCING hits up to rank extra mobs around the target for half damage. */
    private static void playerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack held = player.getItemInHand(player.getUsedItemHand());
        if (held.isEmpty()) return;
        var list = InfusionEnchantment.getInfusionEnchantments(held);
        Entity target = event.getTarget();
        if (!list.contains(InfusionEnchantment.ARCING) || !target.isAlive()) return;
        int rank = InfusionEnchantment.getInfusionEnchantmentLevel(held, InfusionEnchantment.ARCING);
        var targets = level.getEntities(player, target.getBoundingBox().inflate(1.5 + rank, 1.0f + rank / 2.0f, 1.5 + rank));
        int count = 0;
        if (targets.size() > 1) {
            for (Entity e : targets) {
                if (e.isRemoved() || EntityUtils.isFriendly(player, e)) continue;
                if (e instanceof Mob && e.getId() != target.getId() && !(e instanceof Player p && p.getName().getString().equals(player.getName().getString())) && e.isAlive()) {
                    float f = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    if (e.hurtServer(level, player.damageSources().playerAttack(player), f * 0.5f)) {
                        e.push(-Mth.sin(player.getYRot() * Mth.PI / 180.0f) * 0.5f, 0.1, Mth.cos(player.getYRot() * Mth.PI / 180.0f) * 0.5f);
                        ++count;
                        // PacketFXSlash between the target and the chained mob.
                        slash(level, target, e);
                    }
                }
                if (count >= rank) break;
            }
            if (count > 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.get("wind"), SoundSource.PLAYERS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
                slash(level, player, target);
            }
        }
    }

    /** PacketFXSlash(source, target): the client draws FXDispatcher.drawSlash between the two entities' centres. */
    private static void slash(ServerLevel level, Entity a, Entity b) {
        art.arcane.thaumcraft.fx.Fx.send(level, "slash", a.getX(), a.getY(), a.getZ(), a.getId(), b.getId());
    }

    // ------------------------------------------------------------------ sounding

    /** ToolEvents.playerRightClickBlock: sneak-using a SOUNDING pick pings ores around the block (PacketFXScanSource). */
    private static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) return;
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.isEmpty()) return;
        if (InfusionEnchantment.getInfusionEnchantments(held).contains(InfusionEnchantment.SOUNDING) && player.isShiftKeyDown()) {
            held.hurtAndBreak(5, player, event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            BlockPos pos = event.getPos();
            player.level().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.get("wandfail"), SoundSource.BLOCKS, 0.2f, 0.2f + player.level().getRandom().nextFloat() * 0.2f);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new art.arcane.thaumcraft.network.FxPayload("scan_source", pos.getX(), pos.getY(), pos.getZ(),
                    List.of((float) InfusionEnchantment.getInfusionEnchantmentLevel(held, InfusionEnchantment.SOUNDING))));
        }
    }

    private static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Only the server reads it (block harvesting); the event also fires client-side on the same JVM in singleplayer.
        if (event.getFace() != null && !event.getLevel().isClientSide()) LAST_FACE_CLICKED.put(event.getEntity().getUUID(), event.getFace());
    }

    // ------------------------------------------------------------------ burrowing

    /** Utils.isWoodLog. */
    public static boolean isWoodLog(Level level, BlockPos pos) { return level.getBlockState(pos).is(BlockTags.LOGS); }
    /** Utils.isOreBlock (ore dictionary "ore*" names). */
    public static boolean isOreBlock(Level level, BlockPos pos) { return level.getBlockState(pos).is(Tags.Blocks.ORES); }

    private static boolean toolEffective(ItemStack tool, BlockState state) { return tool.isCorrectToolForDrops(state) || tool.getDestroySpeed(state) > 1.0f; }

    /** ToolEvents.breakBlockEvent: a BURROWING tool breaks the furthest connected log/ore instead of the one hit. */
    private static void breakBlock(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;
        var list = InfusionEnchantment.getInfusionEnchantments(held);
        if (list.contains(InfusionEnchantment.BURROWING) && toolEffective(held, event.getState()) && !player.isShiftKeyDown() && (isWoodLog(level, event.getPos()) || isOreBlock(level, event.getPos()))) {
            event.setCanceled(true);
            held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            breakFurthestBlock(level, event.getPos(), event.getState(), sp);
        }
    }

    private static BlockPos lastPos;
    private static double lastDistance;

    private static void findBlocks(Level level, BlockPos pos, BlockState block, int reach) {
        for (int xx = -reach; xx <= reach; ++xx) for (int yy = reach; yy >= -reach; --yy) for (int zz = -reach; zz <= reach; ++zz) {
            if (Math.abs(lastPos.getX() + xx - pos.getX()) > 24) return;
            if (Math.abs(lastPos.getY() + yy - pos.getY()) > 48) return;
            if (Math.abs(lastPos.getZ() + zz - pos.getZ()) > 24) return;
            BlockPos p = lastPos.offset(xx, yy, zz);
            BlockState bs = level.getBlockState(p);
            if (bs.getBlock() == block.getBlock() && bs.getDestroySpeed(level, p) >= 0.0f) {
                double xd = p.getX() - pos.getX(), yd = p.getY() - pos.getY(), zd = p.getZ() - pos.getZ();
                double d = xd * xd + yd * yd + zd * zd;
                if (d > lastDistance) {
                    lastDistance = d;
                    lastPos = p;
                    findBlocks(level, pos, block, reach);
                    return;
                }
            }
        }
    }

    /** BlockUtils.breakFurthestBlock. */
    public static boolean breakFurthestBlock(ServerLevel level, BlockPos pos, BlockState block, ServerPlayer player) {
        lastPos = pos;
        lastDistance = 0.0;
        boolean log = isWoodLog(level, pos);
        findBlocks(level, pos, block, log ? 2 : 1);
        boolean worked = harvestBlockSkipCheck(level, player, lastPos);
        level.sendBlockUpdated(pos, block, level.getBlockState(pos), 3);
        if (worked && log) {
            for (int xx = -3; xx <= 3; ++xx) for (int yy = -3; yy <= 3; ++yy) for (int zz = -3; zz <= 3; ++zz) {
                BlockPos p = lastPos.offset(xx, yy, zz);
                level.scheduleTick(p, level.getBlockState(p).getBlock(), 50 + level.getRandom().nextInt(75));
            }
        }
        return worked;
    }

    /** BlockUtils.harvestBlockSkipCheck: break and drop a block as the player would, without firing the break event. */
    public static boolean harvestBlockSkipCheck(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) return false;
        var be = level.getBlockEntity(pos);
        level.levelEvent(2001, pos, Block.getId(st));
        if (player.isCreative()) return level.removeBlock(pos, false);
        ItemStack tool = player.getMainHandItem();
        boolean canHarvest = st.canHarvestBlock(level, pos, player);
        boolean removed = st.onDestroyedByPlayer(level, pos, player, tool, canHarvest, level.getFluidState(pos));
        if (removed) {
            st.getBlock().destroy(level, pos, st);
            if (canHarvest) st.getBlock().playerDestroy(level, player, pos, st, be, tool.copy());
        }
        return removed;
    }

    // ------------------------------------------------------------------ refining / destructive / collector / lamplight

    private record Special(Predicate<ItemStack> test, java.util.function.Supplier<Item> result, float chance) {}
    private static TagKey<Item> c(String path) { return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path)); }
    /** Utils.specialMiningResult (ModConfig: ore dictionary ores and the vanilla/TC ores to their native clusters, chance 1.0). */
    private static final List<Special> SPECIAL = List.of(
            new Special(s -> s.is(c("ores/iron")) || s.is(Items.RAW_IRON), ModItems.CLUSTER_IRON::get, 1.0f),
            new Special(s -> s.is(c("ores/gold")) || s.is(Items.RAW_GOLD), ModItems.CLUSTER_GOLD::get, 1.0f),
            new Special(s -> s.is(c("ores/copper")) || s.is(Items.RAW_COPPER), ModItems.CLUSTER_COPPER::get, 1.0f),
            new Special(s -> s.is(c("ores/tin")) || s.is(c("raw_materials/tin")), ModItems.CLUSTER_TIN::get, 1.0f),
            new Special(s -> s.is(c("ores/silver")) || s.is(c("raw_materials/silver")), ModItems.CLUSTER_SILVER::get, 1.0f),
            new Special(s -> s.is(c("ores/lead")) || s.is(c("raw_materials/lead")), ModItems.CLUSTER_LEAD::get, 1.0f),
            new Special(s -> s.is(ModBlocks.ORE_CINNABAR.get().asItem()), ModItems.CLUSTER_CINNABAR::get, 1.0f),
            new Special(s -> s.is(Items.QUARTZ), ModItems.CLUSTER_QUARTZ::get, 1.0f));

    /** Utils.findSpecialMiningResult. */
    public static ItemStack findSpecialMiningResult(ItemStack is, float chance, RandomSource rand) {
        float r = rand.nextFloat();
        for (Special sp : SPECIAL) if (sp.test().test(is)) {
            if (r <= chance * sp.chance()) return new ItemStack(sp.result().get(), is.getCount());
            break;
        }
        return is.copy();
    }

    /** ToolEvents.harvestBlockEvent. */
    private static void harvestDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player harvester)) return;
        ServerLevel level = event.getLevel();
        ItemStack held = event.getTool();
        if (held.isEmpty()) return;
        var list = InfusionEnchantment.getInfusionEnchantments(held);
        if (list.isEmpty()) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        boolean silk = held.getEnchantments().keySet().stream().anyMatch(h -> h.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH));
        if (!(silk || toolEffective(held, state))) return;
        if (list.contains(InfusionEnchantment.REFINING)) {
            int fortune = 1 + InfusionEnchantment.getInfusionEnchantmentLevel(held, InfusionEnchantment.REFINING);
            float chance = fortune * 0.125f;
            boolean b = false;
            for (ItemEntity ie : event.getDrops()) {
                ItemStack is = ie.getItem();
                ItemStack smr = findSpecialMiningResult(is, chance, level.getRandom());
                if (!ItemStack.isSameItem(is, smr)) { ie.setItem(smr); b = true; }
            }
            if (b) level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2f, 0.7f + level.getRandom().nextFloat() * 0.2f);
        }
        if (!blockDestructiveRecursion && list.contains(InfusionEnchantment.DESTRUCTIVE) && !harvester.isShiftKeyDown() && harvester instanceof ServerPlayer sp) {
            blockDestructiveRecursion = true;
            try {
                Direction face = LAST_FACE_CLICKED.get(harvester.getUUID());
                if (face == null) face = Direction.orderedByNearest(harvester)[0].getOpposite();
                for (int aa = -1; aa <= 1; ++aa) for (int bb = -1; bb <= 1; ++bb) {
                    if (aa == 0 && bb == 0) continue;
                    int xx = 0, yy = 0, zz = 0;
                    if (face.getAxis() == Direction.Axis.Y) { xx = aa; zz = bb; }
                    else if (face.getAxis() == Direction.Axis.Z) { xx = aa; yy = bb; }
                    else { zz = aa; yy = bb; }
                    BlockPos p = pos.offset(xx, yy, zz);
                    BlockState bl = level.getBlockState(p);
                    if (!bl.isAir() && bl.getDestroySpeed(level, p) >= 0.0f && toolEffective(sp.getMainHandItem(), bl)) sp.gameMode.destroyBlock(p);
                }
            } finally { blockDestructiveRecursion = false; }
        }
        if (list.contains(InfusionEnchantment.COLLECTOR) && !harvester.isShiftKeyDown()) {
            // InventoryUtils.dropHarvestsAtPos(world, pos, drops, true, 10, harvester): each drop respawns as a following item.
            var drops = event.getDrops();
            for (int i = 0; i < drops.size(); ++i) {
                float f = 0.5f;
                var r = level.getRandom();
                double d0 = r.nextFloat() * f + (1.0f - f) * 0.5, d2 = r.nextFloat() * f + (1.0f - f) * 0.5, d3 = r.nextFloat() * f + (1.0f - f) * 0.5;
                var fi = new art.arcane.thaumcraft.entity.FollowingItemEntity(level, pos.getX() + d0, pos.getY() + d2, pos.getZ() + d3, drops.get(i).getItem().copy(), harvester, 10);
                fi.setDefaultPickUpDelay();
                drops.set(i, fi);
            }
        }
        if (list.contains(InfusionEnchantment.LAMPLIGHT) && !harvester.isShiftKeyDown() && harvester instanceof ServerPlayer sp) {
            sp.level().getServer().execute(() -> {
                if (level.isEmptyBlock(pos) && !level.getBlockState(pos).is(ModBlocks.EFFECT_GLIMMER.get()) && level.getMaxLocalRawBrightness(pos) < 10)
                    level.setBlock(pos, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), 3);
            });
        }
    }

    /** ToolEvents.livingDrops: the drop is replaced by an EntityFollowingItem (type 10) flying to the killer. */
    private static ItemEntity follow(ItemEntity ei, Entity target) {
        var nei = new art.arcane.thaumcraft.entity.FollowingItemEntity(ei.level(), ei.getX(), ei.getY(), ei.getZ(), ei.getItem().copy(), target, 10);
        nei.setDeltaMovement(ei.getDeltaMovement());
        nei.setDefaultPickUpDelay();
        ei.discard();
        return nei;
    }

    /** ToolEvents.livingDrops: COLLECTOR mob drops and ESSENCE crystals. */
    private static void livingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        ItemStack held = killer.getItemInHand(killer.getUsedItemHand());
        if (held.isEmpty()) return;
        var list = InfusionEnchantment.getInfusionEnchantments(held);
        LivingEntity dead = event.getEntity();
        var rand = dead.getRandom();
        if (list.contains(InfusionEnchantment.COLLECTOR)) {
            var replaced = new ArrayList<ItemEntity>();
            for (ItemEntity ie : event.getDrops()) replaced.add(follow(ie, killer));
            event.getDrops().clear();
            event.getDrops().addAll(replaced);
        }
        if (list.contains(InfusionEnchantment.ESSENCE)) {
            Map<Aspect, Integer> as = ScanManager.entityAspects(dead);
            if (as != null && !as.isEmpty()) {
                var aspects = new LinkedHashMap<>(as);
                int q = InfusionEnchantment.getInfusionEnchantmentLevel(held, InfusionEnchantment.ESSENCE);
                List<Aspect> al = new ArrayList<>(aspects.keySet());
                for (int b = rand.nextInt(5) < q ? 0 : 99; b < q && !al.isEmpty(); b += 1 + rand.nextInt(2)) {
                    Aspect aspect = al.get(rand.nextInt(al.size()));
                    if (aspects.getOrDefault(aspect, 0) > 0) {
                        int left = aspects.get(aspect) - 1;
                        if (left <= 0) aspects.remove(aspect); else aspects.put(aspect, left);
                        ItemStack stack = CrystalEssenceItem.of(aspect.id(), 1);
                        if (list.contains(InfusionEnchantment.COLLECTOR))
                            event.getDrops().add(new art.arcane.thaumcraft.entity.FollowingItemEntity(dead.level(), dead.getX(), dead.getY() + dead.getEyeHeight(), dead.getZ(), stack, killer, 10));
                        else event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + dead.getEyeHeight(), dead.getZ(), stack));
                        ++b;
                    }
                    al = new ArrayList<>(aspects.keySet());
                }
            }
        }
    }

    // ------------------------------------------------------------------ tooltip

    /** PlayerEvents.tooltipEvent: runic charge and the infusion enchantments (inserted after the name, last added first). */
    private static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int charge = InfusionRunicAugmentRecipe.getRunicCharge(stack);
        if (charge > 0) event.getToolTip().add(Component.translatable("item.runic.charge").append(" +" + charge).withStyle(ChatFormatting.GOLD));
        var tip = event.getToolTip();
        for (var e : InfusionEnchantment.entries(stack)) {
            var ie = InfusionEnchantment.byId(e.id());
            if (ie == null) continue;
            var line = Component.translatable(ie.translationKey());
            if (ie.maxLevel > 1) line = line.append(" ").append(Component.translatable("enchantment.level." + e.lvl()));
            tip.add(Math.min(1, tip.size()), line.withStyle(ChatFormatting.GOLD));
        }
    }

    // ------------------------------------------------------------------ runic shielding

    private static final Map<UUID, Integer> RUNIC_INFO = new HashMap<>(), LAST_MAX_CHARGE = new HashMap<>(), LAST_CHARGE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CYCLE = new HashMap<>();
    /** ModConfig.CONFIG_MISC defaults. */
    public static final int SHIELD_RECHARGE = 2000, SHIELD_WAIT = 4000, SHIELD_COST = 1;

    /** Total runic charge of worn armour and baubles. */
    public static int totalRunicCharge(Player player) {
        int max = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD })
            max += InfusionRunicAugmentRecipe.getRunicCharge(player.getItemBySlot(slot));
        for (Baubles.Worn worn : Baubles.worn(player)) max += InfusionRunicAugmentRecipe.getRunicCharge(worn.stack());
        return max;
    }

    private static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) handleRunicArmor(player);
    }

    /** PlayerEvents.handleRunicArmor: absorption hearts recharged from the aura's vis. */
    public static void handleRunicArmor(ServerPlayer player) {
        UUID id = player.getUUID();
        if (player.tickCount % 20 == 0) {
            int max = totalRunicCharge(player);
            Integer charge = LAST_MAX_CHARGE.get(id);
            if (charge != null) {
                if (charge > max) player.setAbsorptionAmount(Math.max(0.0f, player.getAbsorptionAmount() - (charge - max)));
                if (max <= 0) LAST_MAX_CHARGE.remove(id);
            }
            if (max > 0) { RUNIC_INFO.put(id, max); LAST_MAX_CHARGE.put(id, max); }
            else RUNIC_INFO.remove(id);
        }
        Integer info = RUNIC_INFO.get(id);
        if (info == null) return;
        NEXT_CYCLE.putIfAbsent(id, 0L);
        long time = System.currentTimeMillis();
        int charge = (int) player.getAbsorptionAmount();
        if (charge == 0 && LAST_CHARGE.getOrDefault(id, 0) > 0) {
            NEXT_CYCLE.put(id, time + SHIELD_WAIT);
            LAST_CHARGE.put(id, 0);
        }
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        if (charge < info && NEXT_CYCLE.get(id) < time && !art.arcane.thaumcraft.item.RechargeHelper.shouldPreserveAura(level, player, pos) && NativeAura.vis(level, pos) >= SHIELD_COST) {
            NativeAura.drainVis(level, pos, SHIELD_COST, false);
            NEXT_CYCLE.put(id, time + SHIELD_RECHARGE);
            player.setAbsorptionAmount(charge + 1);
            LAST_CHARGE.put(id, charge + 1);
        }
    }

    /**
     * EntityEvents.entityHurt: a hit absorbed by the runic shield sends PacketFXShield(player, target) — target is the
     * attacker's id, -2 for falls, -3 for falling blocks, -1 otherwise — and plays runicShieldEffect.
     */
    private static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (int) player.getAbsorptionAmount() > 0 && RUNIC_INFO.containsKey(player.getUUID()) && LAST_MAX_CHARGE.containsKey(player.getUUID())) {
            var src = event.getSource();
            int target = src.getEntity() != null ? src.getEntity().getId() : -1;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.FALL)) target = -2;
            if (src.is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK) || src.is(net.minecraft.world.damagesource.DamageTypes.FALLING_ANVIL)) target = -3;
            shieldFx(player.level(), player, target);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.get("runicshieldeffect"), SoundSource.PLAYERS, 0.66f, 1.1f + player.getRandom().nextFloat() * 0.1f);
        }
    }

    /** PacketFXShield, sent to players within 32 blocks. */
    public static void shieldFx(ServerLevel level, Entity source, int target) {
        var payload = new art.arcane.thaumcraft.network.FxPayload("shield", source.getX(), source.getY(), source.getZ(), List.of((float) source.getId(), (float) target));
        for (ServerPlayer p : level.players())
            if (p.distanceToSqr(source) <= 32.0 * 32.0) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
    }

    private InfusionEnchantmentEvents() {}
}
