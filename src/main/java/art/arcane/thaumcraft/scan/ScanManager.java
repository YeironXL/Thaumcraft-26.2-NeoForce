package art.arcane.thaumcraft.scan;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ObservationFormula;
import art.arcane.thaumcraft.research.ResearchEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.*;
import java.util.function.Predicate;

/**
 * Port of TC6 api.research.ScanningManager with every scannable registered by ConfigResearch.initScannables:
 * generic object scans (aspect knowledge), per-aspect discoveries, enchantments, potion effects, entity, block,
 * item, ore-dictionary (tags) and material triggers, and celestial scanning. Container contents are scanned too.
 */
public final class ScanManager {
    public interface Thing {
        boolean check(ServerPlayer player, Object object);
        /** Research key to progress; empty = no key (the scan is then silent, as ScanSky). */
        String key(ServerPlayer player, Object object);
        default void onSuccess(ServerPlayer player, Object object) {}
        /** Generic per-object keys are kept in the scanned-objects set instead of the research list. */
        default boolean generic() { return false; }
    }
    private static List<Thing> things;

    private static synchronized List<Thing> things(ServerPlayer player) {
        if (things == null) things = build(player);
        return things;
    }
    /** Enchantments are data-driven; rebuild the list when a server starts. */
    public static synchronized void reset() { things = null; }

    // ------------------------------------------------------------------ scanning
    public static void scan(ServerPlayer player, Object object) {
        boolean found = false, suppress = false;
        for (Thing thing : things(player)) {
            if (!thing.check(player, object)) continue;
            String key = thing.key(player, object);
            boolean empty = key == null || key.isEmpty();
            if (empty || progress(player, thing, key)) {
                if (empty) suppress = true;
                found = true;
                thing.onSuccess(player, object);
            }
        }
        if (!suppress && player.connection != null) {
            if (!found) player.sendOverlayMessage(Component.translatable("tc.unknownobject").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            else player.sendOverlayMessage(Component.translatable("tc.knownobject").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        }
        if (object instanceof BlockPos pos) {
            var handler = player.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, Direction.UP);
            if (handler != null) {
                int scanned = 0;
                for (int slot = 0; slot < handler.size(); slot++) {
                    var resource = handler.getResource(slot);
                    if (resource.isEmpty()) continue;
                    scan(player, resource.toStack(handler.getAmountAsInt(slot)));
                    scanned++;
                    if (scanned >= 100) {
                        if (player.connection != null) player.sendOverlayMessage(Component.translatable("tc.invtoolarge").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                        break;
                    }
                }
            }
        }
        ResearchEvents.sync(player);
    }
    private static boolean progress(ServerPlayer player, Thing thing, String key) {
        if (thing.generic()) return PlayerProgressManager.get(player).scan(key.substring(1));
        return ResearchEvents.progress(player, key);
    }
    /** TC6 isThingStillScannable: something about the object would still progress research. */
    public static boolean stillScannable(ServerPlayer player, Object object) {
        var research = PlayerProgressManager.get(player);
        for (Thing thing : things(player)) {
            if (!thing.check(player, object)) continue;
            String key = thing.key(player, object);
            if (key == null || key.isEmpty()) continue;
            if (thing.generic() ? !research.scanned(key.substring(1)) : !research.has(key)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ object helpers
    /** ScanningManager.getItemFromParms. */
    public static ItemStack itemFrom(ServerPlayer player, Object object) {
        if (object instanceof ItemStack stack) return stack;
        if (object instanceof ItemEntity item) return item.getItem();
        if (object instanceof BlockPos pos) {
            var state = player.level().getBlockState(pos);
            var stack = state.getCloneItemStack(pos, player.level(), true, player);
            if (stack.isEmpty()) stack = new ItemStack(state.getBlock().asItem());
            if (stack.isEmpty() && state.getFluidState().is(Fluids.WATER)) stack = new ItemStack(Items.WATER_BUCKET);
            if (stack.isEmpty() && state.getFluidState().is(Fluids.LAVA)) stack = new ItemStack(Items.LAVA_BUCKET);
            return stack;
        }
        return ItemStack.EMPTY;
    }
    /** AspectHelper.getEntityAspects. */
    public static Map<Aspect, Integer> entityAspects(Entity entity) { return EntityScanRegistry.aspects(entity); }
    private static Map<Aspect, Integer> aspectsOf(ServerPlayer player, Object object) {
        if (object instanceof Entity entity && !(entity instanceof ItemEntity)) return entityAspects(entity);
        if (object instanceof BlockPos pos && player.level().getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity node) return node.aspectMap();
        var stack = itemFrom(player, object);
        return stack.isEmpty() ? Map.of() : ObjectAspects.get(stack);
    }
    private static String genericKey(ServerPlayer player, Object object) {
        if (object instanceof Entity entity && !(entity instanceof ItemEntity)) return "!entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        // every aura node is its own discovery (TC4 granted research points per scanned node)
        if (object instanceof BlockPos pos && player.level().getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity) return "!node:" + player.level().dimension().identifier() + "@" + pos.toShortString().replace(" ", "");
        var stack = itemFrom(player, object);
        if (stack.isEmpty()) return null;
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String aspect = art.arcane.thaumcraft.item.CrystalEssenceItem.aspect(stack);
        if (!aspect.isEmpty()) key += "/" + aspect;
        var potion = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (potion != null && potion.potion().isPresent()) key += "|" + potion.potion().get().getRegisteredName();
        return "!" + key;
    }

    // ------------------------------------------------------------------ registration (ConfigResearch.initScannables)
    private static List<Thing> build(ServerPlayer player) {
        var list = new ArrayList<Thing>();
        // Aspect.java registers "!<tag>" scans as each aspect is created.
        for (Aspect aspect : AspectRegistry.all().values()) list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o != null && aspectsOf(p, o).getOrDefault(aspect, 0) > 0; }
            public String key(ServerPlayer p, Object o) { return "!" + aspect.id(); }
            public void onSuccess(ServerPlayer p, Object o) {
                var r = PlayerProgressManager.get(p);
                r.addKnowledge("OBSERVATION", "AUROMANCY", 1); r.addKnowledge("OBSERVATION", "BASICS", 1); r.addKnowledge("OBSERVATION", "ALCHEMY", 1);
            }
        });
        // ScanGeneric
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o != null && !aspectsOf(p, o).isEmpty(); }
            public String key(ServerPlayer p, Object o) { return genericKey(p, o); }
            public void onSuccess(ServerPlayer p, Object o) { ObservationFormula.award(PlayerProgressManager.get(p), aspectsOf(p, o)); }
            public boolean generic() { return true; }
        });
        // ScanEnchantment / ScanPotion
        var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        enchantments.listElements().forEach(holder -> list.add(item("!" + holder.key().identifier(), stack ->
                stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(holder.key()))
                || stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).keySet().stream().anyMatch(h -> h.is(holder.key())))));
        BuiltInRegistries.MOB_EFFECT.listElements().forEach(effect -> list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) {
                if (o == null) return false;
                if (o instanceof LivingEntity living) return living.hasEffect(effect);
                if (o instanceof Entity && !(o instanceof ItemEntity)) return false;
                var stack = itemFrom(p, o);
                if (stack.isEmpty()) return false;
                for (var e : stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects())
                    if (e.getEffect().is(effect.key())) return true;
                return false;
            }
            public String key(ServerPlayer p, Object o) { return "!" + effect.key().identifier(); }
        }));
        // ScanEntity
        entity(list, "!Firebat", e -> type(e, "thaumcraft:firebat"));
        entity(list, "!Wisp", e -> type(e, "thaumcraft:wisp"));
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o instanceof BlockPos pos && p.level().getBlockEntity(pos) instanceof art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity; }
            public String key(ServerPlayer p, Object o) { return "!AuraNode"; }
        });
        entity(list, "!ThaumSlime", e -> type(e, "thaumcraft:thaumslime"));
        entity(list, "!Pech", e -> type(e, "thaumcraft:pech"));
        entity(list, "!BrainyZombie", e -> type(e, "thaumcraft:brainy_zombie") || type(e, "thaumcraft:giant_brainy_zombie"));
        entity(list, "!EldritchCrab", e -> type(e, "thaumcraft:eldritch_crab") || type(e, "thaumcraft:inhabited_zombie"));
        entity(list, "!CrimsonCultist", e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().startsWith("cultist"));
        entity(list, "!EldritchGuardian", e -> type(e, "thaumcraft:eldritch_guardian") || type(e, "thaumcraft:eldritch_warden"));
        entity(list, "!ORBOSS", e -> e instanceof art.arcane.thaumcraft.entity.monster.boss.ThaumcraftBossEntity);
        entity(list, "!TaintCrawler", e -> type(e, "thaumcraft:taint_crawler"));
        entity(list, "!Taintacle", e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().startsWith("taintacle"));
        entity(list, "!TaintSeed", e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().startsWith("taint_seed"));
        entity(list, "!TaintSwarm", e -> type(e, "thaumcraft:taint_swarm"));
        entity(list, "f_toomuchflux", e -> type(e, "thaumcraft:flux_rift"));
        entity(list, "!FluxRift", e -> type(e, "thaumcraft:flux_rift"));
        entity(list, "f_golem", e -> e instanceof net.minecraft.world.entity.animal.golem.AbstractGolem || type(e, "thaumcraft:golem"));
        entity(list, "f_SPIDER", e -> e instanceof net.minecraft.world.entity.monster.spider.Spider);
        entity(list, "f_BAT", e -> e instanceof net.minecraft.world.entity.ambient.Bat || type(e, "thaumcraft:firebat"));
        entity(list, "f_FLY", e -> e instanceof net.minecraft.world.entity.ambient.Bat || e instanceof net.minecraft.world.entity.animal.parrot.Parrot
                || type(e, "thaumcraft:firebat") || type(e, "thaumcraft:taint_swarm") || type(e, "thaumcraft:wisp")
                || e instanceof net.minecraft.world.entity.monster.Ghast || e instanceof net.minecraft.world.entity.monster.Blaze);
        // ScanBlock (each also registers ScanItem for its block's item)
        block(list, "ORE", ModBlocks.ORE_AMBER.get(), ModBlocks.ORE_CINNABAR.get(), ModBlocks.CRYSTAL_AER.get(), ModBlocks.CRYSTAL_IGNIS.get(), ModBlocks.CRYSTAL_AQUA.get(),
                ModBlocks.CRYSTAL_TERRA.get(), ModBlocks.CRYSTAL_ORDO.get(), ModBlocks.CRYSTAL_PERDITIO.get(), ModBlocks.CRYSTAL_VITIUM.get());
        block(list, "!OREAMBER", ModBlocks.ORE_AMBER.get());
        block(list, "!ORECINNABAR", ModBlocks.ORE_CINNABAR.get());
        block(list, "!ORECRYSTAL", ModBlocks.CRYSTAL_AER.get(), ModBlocks.CRYSTAL_IGNIS.get(), ModBlocks.CRYSTAL_AQUA.get(), ModBlocks.CRYSTAL_TERRA.get(),
                ModBlocks.CRYSTAL_ORDO.get(), ModBlocks.CRYSTAL_PERDITIO.get(), ModBlocks.CRYSTAL_VITIUM.get());
        block(list, "PLANTS", ModBlocks.LOG_GREATWOOD.get(), ModBlocks.LOG_SILVERWOOD.get(), ModBlocks.SAPLING_GREATWOOD.get(), ModBlocks.SAPLING_SILVERWOOD.get(),
                ModBlocks.CINDERPEARL.get(), ModBlocks.SHIMMERLEAF.get(), ModBlocks.VISHROOM.get());
        block(list, "!PLANTWOOD", ModBlocks.LOG_GREATWOOD.get());
        block(list, "!PLANTWOOD", ModBlocks.LOG_SILVERWOOD.get());
        block(list, "!PLANTWOOD", ModBlocks.SAPLING_GREATWOOD.get());
        block(list, "!PLANTWOOD", ModBlocks.SAPLING_SILVERWOOD.get());
        block(list, "!PLANTCINDERPEARL", ModBlocks.CINDERPEARL.get());
        block(list, "!PLANTSHIMMERLEAF", ModBlocks.SHIMMERLEAF.get());
        block(list, "!PLANTVISHROOM", ModBlocks.VISHROOM.get());
        block(list, "!ORBLOCK1", ModBlocks.STONE_ANCIENT.get(), ModBlocks.STONE_ANCIENT_TILE.get());
        block(list, "!ORBLOCK2", ModBlocks.STONE_ELDRITCH_TILE.get());
        block(list, "!ORBLOCK3", ModBlocks.STONE_ANCIENT_GLYPHED.get());
        // ScanItem
        list.add(item("PRIMPEARL", s -> s.is(ModItems.PRIMORDIAL_PEARL.get())));
        list.add(item("!DRAGONBREATH", s -> s.is(Items.DRAGON_BREATH)));
        list.add(item("!TOTEMUNDYING", s -> s.is(Items.TOTEM_OF_UNDYING)));
        block(list, "f_TELEPORT", Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME);
        list.add(item("f_TELEPORT", s -> s.is(Items.ENDER_PEARL)));
        entity(list, "f_TELEPORT", e -> e instanceof net.minecraft.world.entity.monster.EnderMan);
        entity(list, "f_BRAIN", e -> type(e, "thaumcraft:brainy_zombie"));
        list.add(item("f_BRAIN", s -> s.is(ModItems.BRAIN.get())));
        block(list, "f_DISPENSER", Blocks.DISPENSER);
        list.add(item("f_DISPENSER", s -> s.is(Items.DISPENSER)));
        list.add(item("f_MATCLAY", s -> s.is(Items.CLAY_BALL)));
        var terracotta = new ArrayList<Block>(List.of(Blocks.TERRACOTTA));
        for (var color : net.minecraft.world.item.DyeColor.values()) terracotta.add(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getName() + "_terracotta")));
        block(list, "f_MATCLAY", terracotta.toArray(Block[]::new));
        blockIf(list, "f_MATCLAY", state -> state.is(Blocks.CLAY));   // ScanMaterial(Material.CLAY)
        ore(list, "f_MATIRON", "c:ores/iron", "c:ingots/iron", "c:storage_blocks/iron", "c:plates/iron");
        ore(list, "f_MATBRASS", "c:ingots/brass", "c:storage_blocks/brass", "c:plates/brass");
        ore(list, "f_MATTHAUMIUM", "c:ingots/thaumium", "c:storage_blocks/thaumium", "c:plates/thaumium");
        ore(list, "f_MATVOID", "c:ingots/void", "c:storage_blocks/void", "c:plates/void");
        entity(list, "f_arrow", e -> e instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow);
        list.add(item("f_arrow", s -> s.is(Items.ARROW)));
        entity(list, "f_fireball", e -> e instanceof net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile && !(e instanceof net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge));
        entity(list, "f_spit", e -> e instanceof net.minecraft.world.entity.projectile.LlamaSpit);
        list.add(item("!Pechwand", s -> s.is(ModItems.PECH_WAND.get())));
        list.add(item("f_VOIDSEED", s -> s.is(ModItems.VOID_SEED.get())));
        // ScanSky
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o == null && CelestialScanning.scannable(p); }
            public String key(ServerPlayer p, Object o) { return ""; }
            public void onSuccess(ServerPlayer p, Object o) { CelestialScanning.scan(p); }
        });
        return List.copyOf(list);
    }
    private static boolean type(Entity e, String id) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(id); }
    private static void entity(List<Thing> list, String key, Predicate<Entity> test) {
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o instanceof Entity e && !(e instanceof ItemEntity) && test.test(e); }
            public String key(ServerPlayer p, Object o) { return key; }
        });
    }
    private static Thing item(String key, Predicate<ItemStack> test) {
        return new Thing() {
            public boolean check(ServerPlayer p, Object o) {
                ItemStack s = o instanceof ItemStack is ? is : o instanceof ItemEntity ie ? ie.getItem() : null;
                return s != null && !s.isEmpty() && test.test(s);
            }
            public String key(ServerPlayer p, Object o) { return key; }
        };
    }
    private static void block(List<Thing> list, String key, Block... blocks) {
        var set = Set.of(blocks);
        blockIf(list, key, state -> set.contains(state.getBlock()));
        for (Block b : blocks) { Item item = b.asItem(); if (item != Items.AIR) list.add(item(key, s -> s.is(item))); }
    }
    private static void blockIf(List<Thing> list, String key, Predicate<net.minecraft.world.level.block.state.BlockState> test) {
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) { return o instanceof BlockPos pos && test.test(p.level().getBlockState(pos)); }
            public String key(ServerPlayer p, Object o) { return key; }
        });
    }
    private static void ore(List<Thing> list, String key, String... tags) {
        var keys = Arrays.stream(tags).map(t -> TagKey.create(Registries.ITEM, Identifier.parse(t))).toList();
        list.add(new Thing() {
            public boolean check(ServerPlayer p, Object o) {
                ItemStack s = o instanceof BlockPos pos ? new ItemStack(p.level().getBlockState(pos).getBlock().asItem())
                        : o instanceof ItemStack is ? is : o instanceof ItemEntity ie ? ie.getItem() : ItemStack.EMPTY;
                return !s.isEmpty() && keys.stream().anyMatch(s::is);
            }
            public String key(ServerPlayer p, Object o) { return key; }
        });
    }
    private ScanManager() {}
}
