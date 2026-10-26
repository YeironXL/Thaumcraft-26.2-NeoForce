package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.crafting.ArcaneCraftingRecipe;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.menu.ArcaneWorkbenchMenu;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModRecipes;
import art.arcane.thaumcraft.research.ResearchRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;
import java.util.Map;

public final class ArcaneWorkbenchBlockEntity extends RandomizableContainerBlockEntity {
    public static final List<String> PRIMALS = List.of("aer","terra","ignis","aqua","ordo","perditio");
    private NonNullList<ItemStack> items = NonNullList.withSize(16,ItemStack.EMPTY);
    public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ARCANE_WORKBENCH.get(),pos,state); }
    @Override public int getContainerSize() { return 16; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> values) { items = values; }
    @Override protected Component getDefaultName() { return Component.translatable("block.thaumcraft.arcane_workbench"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ArcaneWorkbenchMenu(id,inventory,this); }
    @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); ContainerHelper.saveAllItems(output,items); }
    @Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input); items = NonNullList.withSize(16,ItemStack.EMPTY); ContainerHelper.loadAllItems(input,items); }
    private CraftingInput.Positioned input() { return CraftingInput.ofPositioned(3,3,List.copyOf(items.subList(0,9))); }
    private record Plan(CraftingInput.Positioned input, ItemStack result, NonNullList<ItemStack> remainders, int vis, Map<String,Integer> crystals) {}
    private Plan plan(ServerPlayer player) {
        if (!(level instanceof ServerLevel world)) return null;
        var positioned = input(); var input = positioned.input();
        for (var holder : world.getServer().getRecipeManager().recipeMap().byType(ModRecipes.ARCANE_TYPE.get())) {
            ArcaneCraftingRecipe recipe = holder.value();
            if (recipe.matches(input,world) && ResearchRules.knows(PlayerProgressManager.get(player),recipe.research()))
                return new Plan(positioned,recipe.assemble(input),CraftingRecipe.defaultCraftingReminder(input),recipe.vis(),recipe.crystals());
        }
        return world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,world)
                .map(holder -> new Plan(positioned,holder.value().assemble(input),holder.value().getRemainingItems(input),0,Map.of())).orElse(null);
    }
    private boolean affordable(Plan plan) {
        if (!(level instanceof ServerLevel world) || NativeAura.available(world,worldPosition) < plan.vis) return false;
        for (var demand : plan.crystals.entrySet()) {
            int slot = 9 + PRIMALS.indexOf(demand.getKey());
            if (!CrystalEssenceItem.aspect(getItem(slot)).equals(demand.getKey()) || getItem(slot).getCount() < demand.getValue()) return false;
        }
        return true;
    }
    public int availableVis() { return level instanceof ServerLevel world ? NativeAura.available(world,worldPosition) : 0; }
    public int requiredVis(ServerPlayer player) { var plan = plan(player); return plan == null ? 0 : plan.vis; }
    public ItemStack preview(ServerPlayer player) {
        if (!getItem(15).isEmpty()) return getItem(15);
        var plan = plan(player);
        return plan != null && affordable(plan) ? plan.result.copy() : ItemStack.EMPTY;
    }
    public ItemStack takeOutput(ServerPlayer player, int amount) {
        if (amount <= 0 || !stillValid(player) || player.level() != level) return ItemStack.EMPTY;
        if (getItem(15).isEmpty()) {
            var plan = plan(player);
            if (plan == null || !affordable(plan) || !(level instanceof ServerLevel world) || !NativeAura.spend(world,worldPosition,plan.vis)) return ItemStack.EMPTY;
            plan.crystals.forEach((aspect,count) -> getItem(9 + PRIMALS.indexOf(aspect)).shrink(count));
            var input = plan.input.input();
            for (int y = 0; y < input.height(); y++) for (int x = 0; x < input.width(); x++) {
                int slot = x + plan.input.left() + (y + plan.input.top()) * 3;
                var stack = getItem(slot);
                if (!stack.isEmpty()) stack.shrink(1);
                var remainder = plan.remainders.get(x + y * input.width());
                if (!remainder.isEmpty()) {
                    if (stack.isEmpty()) setItem(slot,remainder.copy());
                    else if (ItemStack.isSameItemSameComponents(stack,remainder) && stack.getCount() + remainder.getCount() <= stack.getMaxStackSize()) stack.grow(remainder.getCount());
                    else Block.popResource(level,worldPosition,remainder.copy());
                }
            }
            setItem(15,plan.result.copy());
            // Store committed output before callbacks/partial extraction: closing cannot duplicate it.
            var committed = getItem(15);
            committed.onCraftedBy(player,committed.getCount());
            net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player,committed,this);
        }
        ItemStack result = ContainerHelper.removeItem(items,15,amount);
        setChanged();
        return result;
    }
}
