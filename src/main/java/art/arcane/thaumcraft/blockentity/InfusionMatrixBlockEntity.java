package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.api.IGogglesDisplayExtended;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.PedestalBlock;
import art.arcane.thaumcraft.block.PillarBlock;
import art.arcane.thaumcraft.casters.IInteractWithCaster;
import art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliser;
import art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliserExt;
import art.arcane.thaumcraft.essentia.EssentiaHandler;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.infusion.InfusionRecipe;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModRecipes;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.research.ResearchEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * TC6 TileInfusionMatrix: structure check, activation with a caster, stability (symmetry of stabilisers, pillar and matrix
 * modifiers), the infusion crafting cycle (essentia drain, item drain, instability events), the goggles readout and the
 * client effects (source particles, runes, sparks, sounds).
 */
public final class InfusionMatrixBlockEntity extends BlockEntity implements IInteractWithCaster, IGogglesDisplayExtended {
    public enum EnumStability { VERY_STABLE, STABLE, UNSTABLE, VERY_UNSTABLE }

    private static final DecimalFormat FORMATTER = new DecimalFormat("#######.##");
    private record AspectAmount(String aspect, int amount) {
        static final Codec<AspectAmount> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(AspectAmount::aspect), Codec.INT.fieldOf("amount").forGetter(AspectAmount::amount)).apply(i, AspectAmount::new));
    }

    private final List<BlockPos> pedestals = new ArrayList<>();
    private final List<BlockPos> problemBlocks = new ArrayList<>();
    private final Map<Block, Integer> tempBlockCount = new HashMap<>();
    public boolean active, crafting, checkSurroundings = true;
    public float costMult, stability, stabilityReplenish;
    public int stabilityCap = 25, count, craftCount, cycleTime = 20, countDelay = 10;
    public float startUp;
    /** Remaining essentia cost, in TC6 AspectList order. */
    private LinkedHashMap<String, Integer> recipeEssentia = new LinkedHashMap<>();
    private List<ItemStack> recipeIngredients = new ArrayList<>();
    private ItemStack recipeOutput = ItemStack.EMPTY;
    private boolean recipeOutputModifiesInput;
    private String recipePlayer;
    private ItemStack recipeInput = ItemStack.EMPTY;
    private int recipeInstability;
    private int itemCount;
    /** Client: TileInfusionMatrix.sourceFX (pedestals and players feeding the matrix), keyed like PacketFXInfusionSource. */
    public final Map<String, SourceFX> sourceFX = new HashMap<>();

    public static final class SourceFX {
        public final BlockPos loc; public int ticks; public final int color;
        public SourceFX(BlockPos loc, int ticks, int color) { this.loc = loc; this.ticks = ticks; this.color = color; }
    }

    public InfusionMatrixBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.INFUSION_MATRIX.get(), pos, state); }

    public List<BlockPos> pedestals() { return pedestals; }
    public List<BlockPos> problemBlocks() { return problemBlocks; }
    public Map<String, Integer> recipeEssentia() { return java.util.Collections.unmodifiableMap(recipeEssentia); }
    public List<ItemStack> recipeIngredients() { return java.util.Collections.unmodifiableList(recipeIngredients); }
    public int recipeInstability() { return recipeInstability; }

    public EnumStability getStability() {
        return stability > stabilityCap / 2 ? EnumStability.VERY_STABLE : stability >= 0 ? EnumStability.STABLE : stability > -25 ? EnumStability.UNSTABLE : EnumStability.VERY_UNSTABLE;
    }

    private float getModFromCurrentStability() {
        return switch (getStability()) { case VERY_STABLE -> 5.0f; case STABLE -> 6.0f; case UNSTABLE -> 7.0f; case VERY_UNSTABLE -> 8.0f; };
    }

    public static void tick(Level world, BlockPos pos, BlockState state, InfusionMatrixBlockEntity te) { te.update(); }

    private void update() {
        ++count;
        if (checkSurroundings) { checkSurroundings = false; getSurroundings(); }
        if (level.isClientSide()) { doEffects(); return; }
        if (count % (crafting ? 20 : 100) == 0 && !validLocation()) { active = false; sync(); return; }
        if (active && !crafting && stability < stabilityCap && count % Math.max(5, countDelay) == 0) {
            stability += Math.max(0.1f, stabilityReplenish);
            if (stability > stabilityCap) stability = stabilityCap;
            sync();
        }
        if (active && crafting && count % countDelay == 0) { craftCycle(); setChanged(); }
    }

    public boolean validLocation() {
        return level.getBlockState(worldPosition.offset(0, -2, 0)).getBlock() instanceof PedestalBlock
                && level.getBlockState(worldPosition.offset(1, -2, 1)).getBlock() instanceof PillarBlock
                && level.getBlockState(worldPosition.offset(-1, -2, 1)).getBlock() instanceof PillarBlock
                && level.getBlockState(worldPosition.offset(1, -2, -1)).getBlock() instanceof PillarBlock
                && level.getBlockState(worldPosition.offset(-1, -2, -1)).getBlock() instanceof PillarBlock;
    }

    @Override
    public boolean onCasterRightClick(Level world, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand) {
        if (world.isClientSide() && active && !crafting) checkSurroundings = true;
        if (!world.isClientSide() && active && !crafting) { craftingStart(player); return false; }
        if (!world.isClientSide() && !active && validLocation()) {
            world.playSound(null, pos, ModSounds.get("craftstart"), SoundSource.BLOCKS, 0.5f, 1.0f);
            active = true;
            sync();
            return false;
        }
        return false;
    }

    private PedestalBlockEntity centralPedestal() {
        return level.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity p ? p : null;
    }

    /** ThaumcraftCraftingManager.findMatchingInfusionRecipe: first registered recipe that matches. */
    private InfusionRecipe findRecipe(List<ItemStack> components, ItemStack input, Player player) {
        if (!(level instanceof ServerLevel sl)) return null;
        for (var holder : sl.getServer().getRecipeManager().recipeMap().byType(ModRecipes.INFUSION_TYPE.get()))
            if (holder.value().matches(components, input, level, player)) return holder.value();
        return null;
    }

    public void craftingStart(Player player) {
        if (!validLocation()) { active = false; sync(); return; }
        getSurroundings();
        recipeInput = ItemStack.EMPTY;
        var ped = centralPedestal();
        if (ped != null && !ped.getItem(0).isEmpty()) recipeInput = ped.getItem(0).copy();
        if (recipeInput.isEmpty()) return;
        List<ItemStack> components = new ArrayList<>();
        for (BlockPos cc : pedestals) {
            if (level.getBlockEntity(cc) instanceof PedestalBlockEntity ped2 && !ped2.getItem(0).isEmpty()) components.add(ped2.getItem(0).copy());
        }
        if (components.isEmpty()) return;
        InfusionRecipe recipe = findRecipe(components, recipeInput, player);
        if (costMult < 0.5) costMult = 0.5f;
        if (recipe == null) return;
        var out = recipe.output(player, recipeInput, components);
        if (out == null) return;
        recipeIngredients = components;
        recipeOutput = out.stack();
        recipeOutputModifiesInput = out.modifiesInput();
        recipeInstability = recipe.instability(player, recipeInput, components);
        LinkedHashMap<String, Integer> al2 = new LinkedHashMap<>();
        recipe.aspects(player, recipeInput, components).forEach((as, n) -> { int v = (int) (n * costMult); if (v > 0) al2.put(as, v); });
        recipeEssentia = al2;
        recipePlayer = player.getName().getString();
        crafting = true;
        level.playSound(null, worldPosition, ModSounds.get("craftstart"), SoundSource.BLOCKS, 0.5f, 1.0f);
        sync();
    }

    private float getLossPerCycle() { return recipeInstability / getModFromCurrentStability(); }

    private boolean sameForCrafting(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }

    public void craftCycle() {
        var rand = level.getRandom();
        boolean valid = false;
        float ff = rand.nextFloat() * getLossPerCycle();
        stability -= ff;
        stability += stabilityReplenish;
        if (stability < -100.0f) stability = -100.0f;
        if (stability > stabilityCap) stability = stabilityCap;
        var ped = centralPedestal();
        if (ped != null && !ped.getItem(0).isEmpty() && sameForCrafting(ped.getItem(0), recipeInput)) valid = true;
        if (!valid || (stability < 0.0f && rand.nextInt(1500) <= Math.abs(stability))) {
            switch (rand.nextInt(24)) {
                case 0, 1, 2, 3 -> inEvEjectItem(0);
                case 4, 5, 6 -> inEvWarp();
                case 7, 8, 9 -> inEvZap(false);
                case 10, 11 -> inEvZap(true);
                case 12, 13 -> inEvEjectItem(1);
                case 14, 15 -> inEvEjectItem(2);
                case 16 -> inEvEjectItem(3);
                case 17 -> inEvEjectItem(4);
                case 18, 19 -> inEvHarm(false);
                case 20, 21 -> inEvEjectItem(5);
                case 22 -> inEvHarm(true);
                default -> level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1.5f + rand.nextFloat(), Level.ExplosionInteraction.NONE);
            }
            stability += 5.0f + rand.nextFloat() * 5.0f;
            inResAdd();
            if (valid) return;
        }
        if (!valid) {
            crafting = false;
            recipeEssentia = new LinkedHashMap<>();
            recipeInstability = 0;
            sync();
            level.playSound(null, worldPosition, ModSounds.get("craftfail"), SoundSource.BLOCKS, 1.0f, 0.6f);
            return;
        }
        if (countDelay < 1) countDelay = 1;
        if (!recipeEssentia.isEmpty()) {
            for (String aspect : new ArrayList<>(recipeEssentia.keySet())) {
                int na = recipeEssentia.getOrDefault(aspect, 0);
                if (na > 0) {
                    if (EssentiaHandler.drainEssentia((ServerLevel) level, worldPosition, aspect, null, 12, na > 1 ? countDelay : 0)) {
                        if (na - 1 <= 0) recipeEssentia.remove(aspect); else recipeEssentia.put(aspect, na - 1);
                        sync();
                        return;
                    }
                    stability -= 0.25f;
                    sync();
                }
            }
            checkSurroundings = true;
            return;
        }
        if (!recipeIngredients.isEmpty()) {
            for (int a = 0; a < recipeIngredients.size(); ++a) {
                for (BlockPos cc : pedestals) {
                    if (level.getBlockEntity(cc) instanceof PedestalBlockEntity te && !te.getItem(0).isEmpty() && sameForCrafting(te.getItem(0), recipeIngredients.get(a))) {
                        if (itemCount == 0) {
                            itemCount = 5;
                            Fx.send((ServerLevel) level, "infusion_source", worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), cc.getX(), cc.getY(), cc.getZ(), 0);
                        } else if (itemCount-- <= 1) {
                            ItemStack held = te.getItem(0);
                            var rem = held.getCraftingRemainder();
                            te.setItem(0, rem == null ? ItemStack.EMPTY : rem.create());
                            recipeIngredients.remove(a);
                            setChanged();
                        }
                        return;
                    }
                }
                if (!recipeEssentia.isEmpty() && rand.nextInt(1 + a) == 0) {
                    var keys = new ArrayList<>(recipeEssentia.keySet());
                    recipeEssentia.merge(keys.get(rand.nextInt(keys.size())), 1, Integer::sum);
                    stability -= 0.25f;
                    sync();
                }
            }
            return;
        }
        crafting = false;
        craftingFinish();
        recipeOutput = ItemStack.EMPTY;
        sync();
    }

    private AABB around(double r) { return new AABB(worldPosition).inflate(r); }

    private void blockArc(BlockPos from, double tx, double ty, double tz) {
        var rand = level.getRandom();
        Fx.send((ServerLevel) level, "block_arc", from.getX(), from.getY(), from.getZ(), (float) tx, (float) ty, (float) tz, 0.3f - rand.nextFloat() * 0.1f, 0.0f, 0.3f - rand.nextFloat() * 0.1f);
    }

    /** Development aid (debugInfusion=..._zap): forces the TC6 inEvZap instability event. */
    public void debugZap(boolean all) { inEvZap(all); }

    private void inEvZap(boolean all) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, around(10.0))) {
            blockArc(worldPosition, target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0f, target.getZ());
            target.hurtServer((ServerLevel) level, level.damageSources().magic(), 4 + level.getRandom().nextInt(4));
            if (!all) break;
        }
    }

    private void inEvHarm(boolean all) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, around(10.0))) {
            if (level.getRandom().nextBoolean()) target.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 120, 0, false, true));
            else target.addEffect(new MobEffectInstance(ModEffects.VIS_EXHAUST, 2400, 0, true, true));
            if (!all) break;
        }
    }

    /** First instability event seen: the !INSTABILITY research flag and its status message. */
    private void inResAdd() {
        for (Player player : level.getEntitiesOfClass(Player.class, around(10.0))) {
            if (player instanceof ServerPlayer sp && !PlayerProgressManager.has(sp, "!INSTABILITY")) {
                PlayerProgressManager.unlock(sp, "!INSTABILITY");
                ResearchEvents.markDirty(sp);
                ResearchEvents.sync(sp);
                if (sp.connection != null) sp.sendOverlayMessage(Component.translatable("got.instability").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    private void inEvWarp() {
        var targets = level.getEntitiesOfClass(ServerPlayer.class, around(10.0));
        if (targets.isEmpty()) return;
        var target = targets.get(level.getRandom().nextInt(targets.size()));
        if (level.getRandom().nextFloat() < 0.25f) PlayerProgressManager.addWarp(target, PlayerProgressManager.WarpType.NORMAL, 1);
        else PlayerProgressManager.addWarp(target, PlayerProgressManager.WarpType.TEMPORARY, 2 + level.getRandom().nextInt(4));
    }

    private void inEvEjectItem(int type) {
        var rand = level.getRandom();
        for (int retries = 0; retries < 25 && !pedestals.isEmpty(); ++retries) {
            BlockPos cc = pedestals.get(rand.nextInt(pedestals.size()));
            if (!(level.getBlockEntity(cc) instanceof PedestalBlockEntity te) || te.getItem(0).isEmpty()) continue;
            BlockPos stabPos = te.findInstabilityMitigator();
            if (stabPos != null && level.getBlockEntity(stabPos) instanceof StabilizerBlockEntity tste && tste.mitigate(Mth.nextInt(rand, 5, 10))) {
                level.blockEvent(cc, level.getBlockState(cc).getBlock(), 5, 0);
                blockArc(worldPosition, cc.getX() + 0.5, cc.getY() + 1.5, cc.getZ() + 0.5);
                blockArc(cc.above(), stabPos.getX() + 0.5, stabPos.getY() + 0.5, stabPos.getZ() + 0.5);
                return;
            }
            if (type <= 3 || type == 5) {
                Containers.dropContents(level, cc, te);
                te.setItem(0, ItemStack.EMPTY);
            } else te.setItem(0, ItemStack.EMPTY);
            if (type == 1 || type == 3) {
                level.setBlock(cc.above(), ModBlocks.FLUX_GOO.get().defaultBlockState(), 3);
                level.playSound(null, cc, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.3f, 1.0f);
            } else if (type == 2 || type == 4) {
                NativeAura.addFlux((ServerLevel) level, cc, 5 + rand.nextInt(5));
            } else if (type == 5) {
                level.explode(null, cc.getX() + 0.5f, cc.getY() + 0.5f, cc.getZ() + 0.5f, 1.0f, Level.ExplosionInteraction.NONE);
            }
            level.blockEvent(cc, level.getBlockState(cc).getBlock(), 11, 0);
            blockArc(worldPosition, cc.getX() + 0.5, cc.getY() + 1.5, cc.getZ() + 0.5);
            return;
        }
    }

    public void craftingFinish() {
        var te = centralPedestal();
        if (te == null) return;
        if (!recipeOutputModifiesInput) {
            ItemStack qs = recipeOutput.copy();
            ItemStack cur = te.getItem(0);
            if (cur.isDamageableItem() && cur.isDamaged()) {
                float dmg = cur.getDamageValue() / (float) cur.getMaxDamage();
                if (qs.isDamageableItem() && !qs.isDamaged()) qs.setDamageValue((int) (qs.getMaxDamage() * dmg));
            }
            te.setInventorySlotContentsFromInfusion(qs);
        } else {
            // NBTBase output: the tag lands on the item already on the pedestal.
            ItemStack temp = te.getItem(0).copy();
            temp.applyComponents(recipeOutput.getComponentsPatch());
            te.setInventorySlotContentsFromInfusion(temp);
        }
        if (recipePlayer != null && level instanceof ServerLevel sl) {
            var p = sl.getServer().getPlayerList().getPlayerByName(recipePlayer);
            if (p != null) net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(p, te.getItem(0), new SimpleContainer(recipeIngredients.toArray(new ItemStack[0])));
        }
        recipeEssentia = new LinkedHashMap<>();
        recipeInstability = 0;
        sync();
        level.blockEvent(worldPosition.below(2), level.getBlockState(worldPosition.below(2)).getBlock(), 12, 0);
        level.playSound(null, worldPosition, ModSounds.get("wand"), SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    private void getSurroundings() {
        Set<Long> stuff = new HashSet<>();
        pedestals.clear(); tempBlockCount.clear(); problemBlocks.clear();
        cycleTime = 10; stabilityReplenish = 0.0f; costMult = 1.0f;
        for (int xx = -8; xx <= 8; ++xx) for (int zz = -8; zz <= 8; ++zz) for (int yy = -3; yy <= 7; ++yy) {
            if (xx == 0 && zz == 0) continue;
            BlockPos bp = new BlockPos(worldPosition.getX() + xx, worldPosition.getY() - yy, worldPosition.getZ() + zz);
            Block bi = level.getBlockState(bp).getBlock();
            if (bi instanceof PedestalBlock) pedestals.add(bp);
            if (bi instanceof AbstractSkullBlock || (bi instanceof IInfusionStabiliser s && s.canStabaliseInfusion(level, bp))) stuff.add(bp.asLong());
        }
        while (!stuff.isEmpty()) {
            long lp = stuff.iterator().next();
            BlockPos c1 = BlockPos.of(lp);
            int x2 = worldPosition.getX() - c1.getX(), z2 = worldPosition.getZ() - c1.getZ();
            BlockPos c2 = new BlockPos(worldPosition.getX() + x2, c1.getY(), worldPosition.getZ() + z2);
            Block sb1 = level.getBlockState(c1).getBlock(), sb2 = level.getBlockState(c2).getBlock();
            float amt1 = sb1 instanceof IInfusionStabiliserExt e ? e.getStabilizationAmount(level, c1) : 0.1f;
            float amt2 = sb2 instanceof IInfusionStabiliserExt e ? e.getStabilizationAmount(level, c2) : 0.1f;
            if (sb1 == sb2 && amt1 == amt2) {
                if (sb1 instanceof IInfusionStabiliserExt e && e.hasSymmetryPenalty(level, c1, c2)) { stabilityReplenish -= e.getSymmetryPenalty(level, c1); problemBlocks.add(c1); }
                else stabilityReplenish += calcDeminishingReturns(sb1, amt1);
            } else { stabilityReplenish -= Math.max(amt1, amt2); problemBlocks.add(c1); }
            stuff.remove(c2.asLong());
            stuff.remove(lp);
        }
        if (pillars(PillarBlock.class)) {
            if (allPillars(ModBlocks.PILLAR_ANCIENT.get())) { --cycleTime; costMult -= 0.1f; stabilityReplenish -= 0.1f; }
            if (allPillars(ModBlocks.PILLAR_ELDRITCH.get())) { cycleTime -= 3; costMult += 0.05f; stabilityReplenish += 0.2f; }
        }
        int[] xm = {-1, 1, 1, -1}, zm = {-1, -1, 1, 1};
        for (int a = 0; a < 4; ++a) {
            BlockState b = level.getBlockState(worldPosition.offset(xm[a], -3, zm[a]));
            if (b.is(ModBlocks.MATRIX_SPEED.get())) { --cycleTime; costMult += 0.01f; }
            if (b.is(ModBlocks.MATRIX_COST.get())) { ++cycleTime; costMult -= 0.02f; }
        }
        countDelay = cycleTime / 2;
        for (BlockPos cc : pedestals) {
            BlockState bb = level.getBlockState(cc);
            if (bb.is(ModBlocks.PEDESTAL_ELDRITCH.get())) costMult += 0.0025f;
            if (bb.is(ModBlocks.PEDESTAL_ANCIENT.get())) costMult -= 0.01f;
        }
    }

    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
    private boolean pillars(Class<?> c) { for (int[] p : CORNERS) if (!c.isInstance(level.getBlockState(worldPosition.offset(p[0], -2, p[1])).getBlock())) return false; return true; }
    private boolean allPillars(Block b) { for (int[] p : CORNERS) if (!level.getBlockState(worldPosition.offset(p[0], -2, p[1])).is(b)) return false; return true; }

    private float calcDeminishingReturns(Block b, float base) {
        int c = tempBlockCount.getOrDefault(b, 0);
        float bb = c > 0 ? base * (float) Math.pow(0.75, c) : base;
        tempBlockCount.put(b, c + 1);
        return bb;
    }

    private void doEffects() {
        if (crafting) {
            if (craftCount == 0) level.playLocalSound(worldPosition, ModSounds.get("infuserstart"), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            else if (craftCount % 65 == 0) level.playLocalSound(worldPosition, ModSounds.get("infuser"), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            ++craftCount;
            art.arcane.thaumcraft.client.fx.InfusionFx.blockRunes(level, worldPosition.getX(), worldPosition.getY() - 2, worldPosition.getZ(),
                    0.5f + level.getRandom().nextFloat() * 0.2f, 0.1f, 0.7f + level.getRandom().nextFloat() * 0.3f, 25, -0.03f);
        } else if (craftCount > 0) {
            craftCount -= 2;
            if (craftCount < 0) craftCount = 0;
            if (craftCount > 50) craftCount = 50;
        }
        if (active && startUp != 1.0f) {
            if (startUp < 1.0f) startUp += Math.max(startUp / 10.0f, 0.001f);
            if (startUp > 0.999) startUp = 1.0f;
        }
        if (!active && startUp > 0.0f) {
            startUp -= startUp / 10.0f;
            if (startUp < 0.001) startUp = 0.0f;
        }
        art.arcane.thaumcraft.client.fx.InfusionFx.matrixSources(this);
        var rand = level.getRandom();
        if (crafting && stability < 0.0f && rand.nextInt(250) <= Math.abs(stability))
            art.arcane.thaumcraft.client.fx.InfusionFx.spark(worldPosition.getX() + rand.nextFloat(), worldPosition.getY() + rand.nextFloat(), worldPosition.getZ() + rand.nextFloat(),
                    3.0f + rand.nextFloat() * 2.0f, 0.7f + rand.nextFloat() * 0.1f, 0.1f, 0.65f + rand.nextFloat() * 0.1f, 0.8f);
        if (active && !problemBlocks.isEmpty() && rand.nextInt(25) == 0) {
            BlockPos p = problemBlocks.get(rand.nextInt(problemBlocks.size()));
            art.arcane.thaumcraft.client.fx.InfusionFx.spark(p.getX() + rand.nextFloat(), p.getY() + rand.nextFloat(), p.getZ() + rand.nextFloat(),
                    2.0f + rand.nextFloat(), 0.7f + rand.nextFloat() * 0.1f, 0.1f, 0.65f + rand.nextFloat() * 0.1f, 0.8f);
        }
    }

    /** PacketFXInfusionSource on the client: a pedestal (60 ticks) or a player (15 ticks) feeding the matrix. */
    public void addSourceFX(BlockPos p2, int color) {
        String key = p2.getX() + ":" + p2.getY() + ":" + p2.getZ() + ":" + color;
        int ticks = level.getBlockEntity(p2) instanceof PedestalBlockEntity ? 60 : 15;
        var sf = sourceFX.get(key);
        if (sf != null) sf.ticks = ticks; else sourceFX.put(key, new SourceFX(p2, ticks, color));
    }

    @Override
    public List<Component> getIGogglesText() {
        float lpc = getLossPerCycle();
        var out = new ArrayList<Component>();
        out.add(Component.translatable("stability." + getStability().name()).withStyle(ChatFormatting.BOLD));
        out.add(Component.literal(FORMATTER.format(stabilityReplenish) + " ").append(Component.translatable("stability.gain")).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        if (lpc != 0.0f) out.add(Component.translatable("stability.range").withStyle(ChatFormatting.RED)
                .append(Component.literal(FORMATTER.format(lpc) + " ").append(Component.translatable("stability.loss")).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)));
        return out;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static final Codec<List<ItemStack>> STACKS = ItemStack.OPTIONAL_CODEC.listOf();

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("active", active); out.putBoolean("crafting", crafting); out.putFloat("stability", stability);
        out.putInt("recipeinst", recipeInstability);
        var list = new ArrayList<AspectAmount>();
        recipeEssentia.forEach((a, n) -> list.add(new AspectAmount(a, n)));
        out.store("Aspects", AspectAmount.CODEC.listOf(), list);
        if (!recipeIngredients.isEmpty()) out.store("recipein", STACKS, recipeIngredients);
        if (!recipeOutput.isEmpty()) { out.store("recipeout", ItemStack.OPTIONAL_CODEC, recipeOutput); out.putBoolean("rotag", recipeOutputModifiesInput); }
        if (!recipeInput.isEmpty()) out.store("recipeinput", ItemStack.OPTIONAL_CODEC, recipeInput);
        out.putString("recipeplayer", recipePlayer == null ? "" : recipePlayer);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        active = in.getBooleanOr("active", false); crafting = in.getBooleanOr("crafting", false); stability = in.getFloatOr("stability", 0.0f);
        recipeInstability = in.getIntOr("recipeinst", 0);
        recipeEssentia = new LinkedHashMap<>();
        in.read("Aspects", AspectAmount.CODEC.listOf()).ifPresent(l -> l.forEach(a -> { if (a.amount() > 0) recipeEssentia.put(a.aspect(), a.amount()); }));
        recipeIngredients = new ArrayList<>(in.read("recipein", STACKS).orElse(List.of()));
        recipeIngredients.removeIf(ItemStack::isEmpty);
        recipeOutput = in.read("recipeout", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        recipeOutputModifiesInput = in.getBooleanOr("rotag", false);
        recipeInput = in.read("recipeinput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        String rp = in.getStringOr("recipeplayer", "");
        recipePlayer = rp.isEmpty() ? null : rp;
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
