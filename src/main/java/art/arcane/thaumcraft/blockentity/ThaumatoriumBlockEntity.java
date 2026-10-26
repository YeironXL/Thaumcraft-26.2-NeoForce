package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.BrainBoxBlock;
import art.arcane.thaumcraft.block.NitorBlock;
import art.arcane.thaumcraft.block.ThaumatoriumBlock;
import art.arcane.thaumcraft.crafting.CrucibleRecipe;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.menu.ThaumatoriumMenu;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModRecipes;
import art.arcane.thaumcraft.research.ResearchRules;
import art.arcane.thaumcraft.util.InventoryUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TC6 TileThaumatorium: an automated crucible. Selected crucible recipes (1 + 2 per mnemonic matrix) are crafted from
 * essentia sucked in from the sides when the catalyst sits in its slot and the crucible below is heated; output is
 * ejected from the front. Recipes are identified by recipe id (TC6 used a recipe hash).
 */
public final class ThaumatoriumBlockEntity extends BlockEntity implements Container, MenuProvider, EssentiaPort {
    /** Display data synced to clients: recipe output and essentia cost. */
    public record Entry(String id, ItemStack output, Map<String, Integer> aspects) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.fieldOf("id").forGetter(Entry::id),
                ItemStack.OPTIONAL_CODEC.fieldOf("out").forGetter(Entry::output), Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("aspects").forGetter(Entry::aspects)).apply(i, Entry::new));
    }
    private static final Codec<Map<String, Integer>> MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private ItemStack catalyst = ItemStack.EMPTY;
    public Map<String, Integer> essentia = new TreeMap<>();
    public List<String> recipeHash = new ArrayList<>();
    public List<String> recipePlayer = new ArrayList<>();
    /** Recipes available for selection (updateRecipes) - shown in the GUI. */
    public List<Entry> recipes = new ArrayList<>();
    /** Display data of the selected recipes. */
    public List<Entry> selected = new ArrayList<>();
    public int currentCraft = -1, maxRecipes = 1;
    public String currentSuction;
    private int counter;
    private boolean heated;
    private RecipeHolder<CrucibleRecipe> currentRecipe;

    public ThaumatoriumBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.THAUMATORIUM.get(), pos, state); }

    private Direction facing() { return getBlockState().getValue(ThaumatoriumBlock.FACING); }

    private List<RecipeHolder<CrucibleRecipe>> all() {
        return ((ServerLevel) level).getServer().getRecipeManager().recipeMap().byType(ModRecipes.CRUCIBLE_TYPE.get()).stream().toList();
    }
    private RecipeHolder<CrucibleRecipe> byId(String id) {
        for (var h : all()) if (h.id().identifier().toString().equals(id)) return h;
        return null;
    }
    private static Entry entry(RecipeHolder<CrucibleRecipe> h) { return new Entry(h.id().identifier().toString(), h.value().result().create(), new TreeMap<>(h.value().aspects())); }

    boolean checkHeat() {
        BlockState bs = level.getBlockState(worldPosition.below(2));
        return bs.getFluidState().is(FluidTags.LAVA) || bs.is(BlockTags.FIRE) || bs.getBlock() instanceof NitorBlock || bs.is(Blocks.MAGMA_BLOCK);
    }

    private boolean catalystMatches(RecipeHolder<CrucibleRecipe> r, ItemStack s) { return !s.isEmpty() && r.value().matches(new SingleRecipeInput(s), level); }

    public static void tick(Level world, BlockPos pos, BlockState state, ThaumatoriumBlockEntity te) {
        if (world.isClientSide()) return;
        if (te.counter == 0 || te.counter % 40 == 0) { te.heated = te.checkHeat(); te.getUpgrades(); }
        ++te.counter;
        if (!te.heated || te.gettingPower() || te.counter % 5 != 0 || te.recipeHash.isEmpty()) return;
        if (te.catalyst.isEmpty()) { te.currentSuction = null; return; }
        if (te.currentCraft < 0 || te.currentCraft >= te.recipeHash.size() || te.currentRecipe == null || !te.catalystMatches(te.currentRecipe, te.catalyst)) {
            for (int a = 0; a < te.recipeHash.size(); ++a) {
                var r = te.byId(te.recipeHash.get(a));
                if (r != null && te.catalystMatches(r, te.catalyst)) { te.currentCraft = a; te.currentRecipe = r; break; }
            }
        }
        if (te.currentCraft < 0 || te.currentCraft >= te.recipeHash.size() || te.currentRecipe == null) return;
        boolean done = true;
        te.currentSuction = null;
        for (var e : new TreeMap<>(te.currentRecipe.value().aspects()).entrySet()) {
            if (te.essentia.getOrDefault(e.getKey(), 0) < e.getValue()) { te.currentSuction = e.getKey(); done = false; break; }
        }
        if (done) te.completeRecipe();
        else if (te.currentSuction != null) te.fill();
    }

    private void completeRecipe() {
        var r = currentRecipe;
        if (r == null || currentCraft >= recipeHash.size() || !catalystMatches(r, catalyst)) return;
        for (var e : r.value().aspects().entrySet()) if (essentia.getOrDefault(e.getKey(), 0) < e.getValue()) return;
        catalyst.shrink(1);
        essentia = new TreeMap<>();
        ItemStack dropped = r.value().result().create();
        var p = ((ServerLevel) level).getServer().getPlayerList().getPlayerByName(recipePlayer.get(currentCraft));
        if (p != null) PlayerProgressManager.get(p).recordCraft(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(dropped.getItem()).toString());
        InventoryUtils.ejectStackAt((ServerLevel) level, worldPosition, facing(), dropped);
        level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 2.6f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8f);
        currentCraft = -1;
        sync();
    }

    private void fill() {
        Direction facing = facing();
        for (int y = 0; y <= 1; ++y) for (Direction dir : Direction.values()) {
            if (dir == facing || dir == Direction.DOWN || (y == 0 && dir == Direction.UP)) continue;
            var ic = EssentiaTubeBlockEntity.neighbor(level, worldPosition.above(y), dir);
            if (ic == null || ic == this) continue;
            Direction back = dir.getOpposite();
            if (ic.suction(back) < suction() && suction() >= ic.minimumSuction()) {
                String offered = ic.offered(currentSuction, back);
                if (currentSuction.equals(offered) && ic.extractOne(currentSuction, back)) { addToContainer(currentSuction, 1); return; }
            }
        }
    }

    public int addToContainer(String tag, int am) {
        if (currentRecipe == null) return am;
        int ce = currentRecipe.value().aspects().getOrDefault(tag, 0) - essentia.getOrDefault(tag, 0);
        if (ce <= 0) return am;
        int add = Math.min(ce, am);
        essentia.merge(tag, add, Integer::sum);
        sync();
        return am - add;
    }

    boolean gettingPower() { return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.below()) || level.hasNeighborSignal(worldPosition.above()); }

    /** Each mnemonic matrix facing into the machine (either half, any side but the front and bottom) adds two slots. */
    void getUpgrades() {
        Direction facing = facing();
        int mr = 1;
        for (int yy = 0; yy <= 1; ++yy) for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == facing) continue;
            BlockPos bp = new BlockPos(worldPosition.getX() + dir.getStepX(), worldPosition.getY() + yy + dir.getStepY(), worldPosition.getZ() + dir.getStepZ());
            BlockState bs = level.getBlockState(bp);
            if (bs.is(ModBlocks.BRAIN_BOX.get()) && bs.getValue(BrainBoxBlock.FACING) == dir.getOpposite()) mr += 2;
        }
        if (mr != maxRecipes) {
            maxRecipes = mr;
            while (recipeHash.size() > maxRecipes) { recipeHash.removeLast(); recipePlayer.removeLast(); }
            sync();
        }
    }

    /** TC6 updateRecipes: recipes the player knows whose catalyst is in the slot, plus the selected ones; sorted by output name. */
    public void updateRecipes(ServerPlayer player) {
        List<Entry> tmp = new ArrayList<>();
        if (!catalyst.isEmpty()) {
            var state = PlayerProgressManager.get(player);
            for (var h : all()) {
                if (ResearchRules.knows(state, h.value().research()) && catalystMatches(h, catalyst)) tmp.add(entry(h));
                else if (recipeHash.contains(h.id().identifier().toString())) tmp.add(entry(h));
            }
        }
        tmp.sort(Comparator.comparing(e -> e.output().getHoverName().getString()));
        if (!tmp.equals(recipes)) { recipes = tmp; sync(); }
    }

    /** PacketSelectThaumotoriumRecipeToServer: toggles a recipe in the selection. */
    public void select(ServerPlayer player, String id) {
        boolean flag = false;
        int i = recipeHash.indexOf(id);
        if (i >= 0) { recipeHash.remove(i); recipePlayer.remove(i); currentCraft = -1; flag = true; }
        else if (recipeHash.size() < maxRecipes) {
            for (Entry e : recipes) if (e.id().equals(id)) { recipeHash.add(id); recipePlayer.add(player.getGameProfile().name()); flag = true; break; }
        }
        if (flag) sync();
    }

    void sync() {
        if (level != null && !level.isClientSide()) {
            selected = new ArrayList<>();
            for (String id : recipeHash) { var h = byId(id); if (h != null) selected.add(entry(h)); }
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Hash list as the GUI shows it: selected recipes that are not available now, then the available ones. */
    public List<Entry> generateRecipeHashlist() {
        List<Entry> out = new ArrayList<>();
        for (Entry s : selected) if (recipes.stream().noneMatch(r -> r.id().equals(s.id()))) out.add(s);
        out.addAll(recipes);
        return out;
    }

    // ---- EssentiaPort: input from every side but the front ----
    @Override public boolean connects(Direction side) { return side != facing(); }
    @Override public int suction() { return currentSuction != null ? 128 : 0; }
    @Override public String wanted() { return currentSuction == null ? "" : currentSuction; }
    @Override public String aspect() { return ""; }
    @Override public String offered(String requested, Direction side) { return ""; }
    @Override public boolean extractOne(String aspect, Direction side) { return false; }

    // ---- inventory ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return catalyst.isEmpty(); }
    @Override public ItemStack getItem(int i) { return catalyst; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = catalyst.split(n); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = catalyst; catalyst = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { catalyst = s; setChanged(); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { catalyst = ItemStack.EMPTY; }
    @Override public Component getDisplayName() { return Component.translatable("container.alchemyfurnace"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new ThaumatoriumMenu(id, inv, this); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!catalyst.isEmpty()) out.store("catalyst", ItemStack.CODEC, catalyst);
        out.store("essentia", MAP, essentia);
        out.store("recipes", Codec.STRING.listOf(), recipeHash);
        out.store("players", Codec.STRING.listOf(), recipePlayer);
        out.putInt("maxrec", maxRecipes);
        out.store("available", Entry.CODEC.listOf(), recipes);
        out.store("selected", Entry.CODEC.listOf(), selected);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        catalyst = in.read("catalyst", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        essentia = new TreeMap<>(in.read("essentia", MAP).orElse(Map.of()));
        recipeHash = new ArrayList<>(in.read("recipes", Codec.STRING.listOf()).orElse(List.of()));
        recipePlayer = new ArrayList<>(in.read("players", Codec.STRING.listOf()).orElse(List.of()));
        while (recipePlayer.size() < recipeHash.size()) recipePlayer.add("");
        maxRecipes = in.getIntOr("maxrec", 1);
        recipes = new ArrayList<>(in.read("available", Entry.CODEC.listOf()).orElse(List.of()));
        selected = new ArrayList<>(in.read("selected", Entry.CODEC.listOf()).orElse(List.of()));
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
