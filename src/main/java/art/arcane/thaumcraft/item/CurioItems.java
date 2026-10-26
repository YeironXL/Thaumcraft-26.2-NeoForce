package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.progression.Warp;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.research.ResearchEvents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** TC6 ItemCurio, ItemLootBag, ItemPechWand, ItemChunksEdible, ItemResonator, ItemCreativeFluxSponge and ItemPrimordialPearl. */
public final class CurioItems {
    private CurioItems() {}

    /** IPlayerKnowledge.EnumKnowledgeType progression: OBSERVATION 16, THEORY 32. */
    private static final int O_PROG = 16, T_PROG = 32;

    static void knowledge(ServerPlayer player, String type, String category, int amount) {
        PlayerProgressManager.get(player).addKnowledge(type, category, amount);
    }

    static String randomCategory(ServerPlayer player) {
        List<String> c = ResearchCatalog.CATEGORIES;
        return c.get(player.getRandom().nextInt(c.size()));
    }

    static void learnSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.get("learn"), SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    /** ItemCurio: knowledge in the curio's category plus a random one; the eldritch and rites curios add warp. */
    public static class Curio extends Item {
        /** Variant order of ItemCurio: arcane, preserved, ancient, eldritch, knowledge, twisted, rites. */
        private final int variant;
        public Curio(Properties p, int variant) { super(p.rarity(Rarity.UNCOMMON)); this.variant = variant; }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.translatable("item.curio.text"));
        }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            learnSound(level, player);
            if (player instanceof ServerPlayer sp) {
                var rng = sp.getRandom();
                String category = switch (variant) { case 1 -> "ALCHEMY"; case 2 -> "GOLEMANCY"; case 3 -> "ELDRITCH"; case 4 -> "INFUSION"; case 5 -> "ARTIFICE"; case 6 -> "ELDRITCH"; default -> "AUROMANCY"; };
                if (variant == 6) {
                    if (Warp.actual(sp) <= 20) {
                        sp.sendSystemMessage(Component.translatable("fail.crimsonrites").withStyle(ChatFormatting.DARK_PURPLE));
                        return InteractionResult.PASS;
                    }
                    if (!Warp.knows(sp, "CrimsonRites")) Warp.completeResearch(sp, "CrimsonRites");
                }
                knowledge(sp, "OBSERVATION", category, Mth.nextInt(rng, O_PROG / 2, O_PROG));
                knowledge(sp, "THEORY", category, Mth.nextInt(rng, T_PROG / 3, T_PROG / 2));
                if (variant == 3 || variant == 6) {
                    PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.NORMAL, 1);
                    PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.TEMPORARY, 5);
                    if (variant == 6 && rng.nextBoolean()) PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.PERMANENT, 1);
                }
                knowledge(sp, "OBSERVATION", randomCategory(sp), Mth.nextInt(rng, O_PROG / 2, O_PROG));
                knowledge(sp, "THEORY", randomCategory(sp), Mth.nextInt(rng, T_PROG / 3, T_PROG / 2));
                ResearchEvents.markDirty(sp);
                ResearchEvents.sync(sp);
                player.getItemInHand(hand).consume(1, player);
                sp.sendSystemMessage(Component.translatable("tc.knowledge.gained").withStyle(ChatFormatting.DARK_PURPLE));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
    }

    /** ItemPechWand: needs BASEAUROMANCY; consumed for a little random knowledge. */
    public static class PechWand extends Item {
        public PechWand(Properties p) { super(p.rarity(Rarity.RARE)); }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.translatable("item.curio.text"));
        }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            if (!Warp.knows(sp, "BASEAUROMANCY")) {
                sp.sendSystemMessage(Component.translatable("not.pechwand").withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }
            player.getItemInHand(hand).consume(1, player);
            learnSound(level, player);
            // TC6 progresses "FOCUSPECH", a key BETA26 never defines, so the note shows every time.
            sp.sendSystemMessage(Component.translatable("got.pechwand").withStyle(ChatFormatting.DARK_PURPLE));
            var rng = sp.getRandom();
            knowledge(sp, "OBSERVATION", randomCategory(sp), Mth.nextInt(rng, O_PROG / 3, O_PROG / 2));
            knowledge(sp, "THEORY", randomCategory(sp), Mth.nextInt(rng, T_PROG / 5, T_PROG / 4));
            ResearchEvents.markDirty(sp);
            ResearchEvents.sync(sp);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
    }

    /** ItemLootBag: 8-12 rolls of the bag's loot table, with the coin sound. */
    public static class LootBag extends Item {
        private final int rarity;
        public LootBag(Properties p, int rarity) { super(p.rarity(rarity == 2 ? Rarity.RARE : rarity == 1 ? Rarity.UNCOMMON : Rarity.COMMON)); this.rarity = rarity; }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.translatable("tc.lootbag"));
        }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel sl) {
                for (int q = 8 + sl.getRandom().nextInt(5), a = 0; a < q; ++a) {
                    ItemStack is = TcLoot.generate(rarity, sl.getRandom(), sl.registryAccess());
                    if (!is.isEmpty()) sl.addFreshEntity(new ItemEntity(sl, player.getX(), player.getY(), player.getZ(), is));
                }
                sl.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.get("coins"), SoundSource.PLAYERS, 0.75f, 1.0f);
            }
            player.getItemInHand(hand).shrink(1);
            return InteractionResult.SUCCESS;
        }
    }

    /** ItemChunksEdible: 1 hunger, 0.3 saturation, eaten in half a second. */
    public static Item.Properties chunk(Item.Properties p) {
        return p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build(), Consumable.builder().consumeSeconds(0.5F).build());
    }

    /** ItemResonator: reports the essentia and suction of a transport block's face. */
    public static class Resonator extends Item {
        public Resonator(Properties p) { super(p.stacksTo(1).rarity(Rarity.UNCOMMON)); }

        @Override public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            if (!(level.getBlockEntity(pos) instanceof EssentiaPort port)) return InteractionResult.FAIL;
            if (!(level instanceof ServerLevel sl) || ctx.getPlayer() == null) return InteractionResult.SUCCESS;
            Player player = ctx.getPlayer();
            Direction side = ctx.getClickedFace();
            if (port instanceof EssentiaTubeBlockEntity tube && tube.kind() == EssentiaTubeBlockEntity.Kind.BUFFER) {
                tube.contents().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(e -> player.sendSystemMessage(Component.translatable("tc.resonator1", "" + e.getValue(), cap(e.getKey()))));
            } else {
                String aspect = port.offered("", side);
                if (aspect != null && !aspect.isEmpty()) {
                    int amount = port instanceof EssentiaTubeBlockEntity t ? t.contents().getOrDefault(aspect, 0)
                            : port instanceof EssentiaJarBlockEntity j ? j.contents().amount() : 1;
                    player.sendSystemMessage(Component.translatable("tc.resonator1", "" + amount, cap(aspect)));
                }
            }
            String wanted = port.wanted();
            Component type = wanted == null || wanted.isEmpty() ? Component.translatable("tc.resonator3") : Component.literal(cap(wanted));
            player.sendSystemMessage(Component.translatable("tc.resonator2", "" + port.suction(side), type));
            sl.playSound(null, pos, SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS, 0.5f, 1.9f + sl.getRandom().nextFloat() * 0.1f);
            return InteractionResult.SUCCESS;
        }

        private static String cap(String a) { return a.isEmpty() ? a : Character.toUpperCase(a.charAt(0)) + a.substring(1); }
    }

    /** ItemCreativeFluxSponge: drains the flux of the 9x9 chunks around; sneaking also removes nearby flux rifts. */
    public static class CreativeFluxSponge extends Item {
        public CreativeFluxSponge(Properties p) { super(p.stacksTo(1).rarity(Rarity.EPIC)); }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.literal("Right-click to drain all").withStyle(ChatFormatting.GREEN));
            builder.accept(Component.literal("flux from 9x9 chunk area").withStyle(ChatFormatting.GREEN));
            builder.accept(Component.literal("Also removes flux rifts").withStyle(ChatFormatting.DARK_AQUA));
            builder.accept(Component.literal("if used while sneaking.").withStyle(ChatFormatting.DARK_AQUA));
            builder.accept(Component.literal("Creative only").withStyle(ChatFormatting.DARK_PURPLE));
        }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel sl)) {
                player.swing(hand);
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.get("craftstart"), SoundSource.PLAYERS, 0.15f, 1.0f, false);
                return InteractionResult.SUCCESS;
            }
            int q = 0;
            BlockPos p = player.blockPosition();
            for (int x = -4; x <= 4; ++x) for (int z = -4; z <= 4; ++z) q += (int) NativeAura.drainFlux(sl, p.offset(16 * x, 0, 16 * z), 500.0f, false);
            player.sendSystemMessage(Component.literal(q + " flux drained from 81 chunks.").withStyle(ChatFormatting.GREEN));
            if (player.isShiftKeyDown()) {
                List<FluxRiftEntity> list = sl.getEntitiesOfClass(FluxRiftEntity.class, new AABB(player.blockPosition()).inflate(32.0));
                list.removeIf(r -> r.distanceToSqr(player) > 32.0 * 32.0);
                list.forEach(FluxRiftEntity::discard);
                player.sendSystemMessage(Component.literal(list.size() + " flux rifts removed.").withStyle(ChatFormatting.DARK_AQUA));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * ItemPrimordialPearl: eight uses. A recipe that consumes it gives back the pearl one step more worn (pearl 0-2,
     * nodule 3-5, mote 6-7) until the last use. Not enchantable or repairable.
     */
    public static class PrimordialPearl extends Item {
        public PrimordialPearl(Properties p) { super(p.durability(8).rarity(Rarity.UNCOMMON)); }

        @Override public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
            int damage = instance.getOrDefault(DataComponents.DAMAGE, 0);
            if (damage >= 7) return null;
            ItemStack next = new ItemStack(this);
            next.setDamageValue(damage + 1);
            return ItemStackTemplate.fromNonEmptyStack(next);
        }

        @Override public Component getName(ItemStack stack) {
            int d = stack.getDamageValue();
            return Component.translatable(d < 3 ? "item.thaumcraft.primordial_pearl" : d < 6 ? "item.thaumcraft.primordial_pearl_nodule" : "item.thaumcraft.primordial_pearl_mote");
        }

        @Override public boolean isBarVisible(ItemStack stack) { return false; }

        /** setNoRepair: no combining two worn pearls in an anvil or grindstone. */
        public static void register() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AnvilUpdateEvent e) -> {
                if (e.getLeft().getItem() instanceof PrimordialPearl || e.getRight().getItem() instanceof PrimordialPearl) e.setCanceled(true);
            });
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.GrindstoneEvent.OnPlaceItem e) -> {
                if (e.getTopItem().getItem() instanceof PrimordialPearl || e.getBottomItem().getItem() instanceof PrimordialPearl) e.setCanceled(true);
            });
        }
    }
}
