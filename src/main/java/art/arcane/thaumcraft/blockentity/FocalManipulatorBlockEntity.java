package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.research.ResearchRules;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileFocalManipulator: designs a focus as a node tree, then drains vis (20/s) to imprint it. */
public final class FocalManipulatorBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    public float vis = 0.0f;
    public Map<Integer, FocusElementNode> data = new HashMap<>();
    public String focusName = "";
    private int ticks = 0;
    public int xpCost = 0;
    public Map<String, Integer> crystalsSync = new LinkedHashMap<>();
    /** Client: the GUI must rebuild its node tree. */
    public boolean doGuiReset = false;
    public boolean doGather = false;

    public FocalManipulatorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FOCAL_MANIPULATOR.get(), pos, state); }

    @Override public int getContainerSize() { return 1; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> v) { items = v; }
    @Override protected Component getDefaultName() { return Component.translatable("block.thaumcraft.wand_workbench"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new art.arcane.thaumcraft.menu.FocalManipulatorMenu(id, inv, this); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof FocusItem; }
    @Override public int getMaxStackSize() { return 1; }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack prev = getItem(slot);
        super.setItem(slot, stack);
        if (stack.isEmpty() || !ItemStack.matches(stack, prev)) {
            if (level != null && level.isClientSide()) { data.clear(); doGuiReset = true; }
            else { vis = 0.0f; crystalsSync = new LinkedHashMap<>(); setChanged(); sync(); }
        }
    }

    public void sync() { if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    /** Spends from the own chunk, or the 3x3 chunks when a workbench charger sits on top. */
    public float spendAura(float vis) {
        var w = (ServerLevel) level;
        if (w.getBlockState(worldPosition.above()).is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get())) {
            float q = vis, z = vis / 9.0f;
            outer:
            for (int xx = -1; xx <= 1; ++xx) for (int zz = -1; zz <= 1; ++zz) {
                if (z > q) z = q;
                q -= NativeAura.drainVis(w, worldPosition.offset(xx * 16, 0, zz * 16), z, false);
                if (q <= 0.0f) break outer;
            }
            return vis - q;
        }
        return NativeAura.drainVis(w, worldPosition, vis, false);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FocalManipulatorBlockEntity te) {
        boolean complete = false;
        ++te.ticks;
        if (!world.isClientSide()) {
            if (te.ticks % 20 == 0) {
                ItemStack in = te.getItem(0);
                if (te.vis > 0.0f && (in.isEmpty() || !(in.getItem() instanceof FocusItem))) {
                    complete = true;
                    te.vis = 0.0f;
                    world.playSound(null, pos, ModSounds.get("wandfail"), SoundSource.BLOCKS, 0.33f, 1.0f);
                }
                if (!complete && te.vis > 0.0f) {
                    float amt = te.spendAura(Math.min(20.0f, te.vis));
                    if (amt > 0.0f) {
                        world.blockEvent(pos, state.getBlock(), 5, 1);
                        te.vis -= amt;
                        te.sync();
                        te.setChanged();
                    }
                    if (te.vis <= 0.0f && !te.getItem(0).isEmpty() && te.getItem(0).getItem() instanceof FocusItem) { complete = true; te.endCraft(); }
                }
            }
        } else if (te.vis > 0.0f) {
            var r = world.getRandom();
            art.arcane.thaumcraft.client.fx.FXDispatcher.generic(pos.getX() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3f, pos.getY() + 1.4 + (r.nextFloat() - r.nextFloat()) * 0.3f,
                    pos.getZ() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3f, 0, 0, 0, art.arcane.thaumcraft.fx.GenericParticleOptions.builder().maxAge(6 + r.nextInt(5))
                            .color(0.5f + r.nextFloat() * 0.4f, 1.0f - r.nextFloat() * 0.4f, 1.0f - r.nextFloat() * 0.4f).alpha(0.8f).particles(448, 9, 1)
                            .scale(0.3f + r.nextFloat() * 0.3f).build(), 0);
        }
        if (complete) { te.vis = 0.0f; te.sync(); te.setChanged(); }
    }

    private FocusPackage generateFocus() {
        if (data == null || data.isEmpty()) return null;
        FocusPackage core = new FocusPackage();
        int totalComplexity = 0;
        Map<String, Integer> compCount = new HashMap<>();
        for (FocusElementNode node : data.values()) {
            if (node.node == null) continue;
            int a = compCount.getOrDefault(node.node.getKey(), 0) + 1;
            node.complexityMultiplier = 0.5f * (a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity += (int) (node.node.getComplexity() * node.complexityMultiplier);
        }
        core.setComplexity(totalComplexity);
        traverseChildren(core, data.get(0));
        return core;
    }

    private void traverseChildren(FocusPackage currentPackage, FocusElementNode currentNode) {
        if (currentPackage == null || currentNode == null) return;
        currentPackage.addNode(currentNode.node);
        if (currentNode.children == null || currentNode.children.length == 0) return;
        if (currentNode.children.length == 1) traverseChildren(currentPackage, data.get(currentNode.children[0]));
        else {
            FocusModSplit splitNode = (FocusModSplit) currentNode.node;
            splitNode.getSplitPackages().clear();
            for (int c : currentNode.children) {
                FocusPackage sp = new FocusPackage();
                traverseChildren(sp, data.get(c));
                splitNode.getSplitPackages().add(sp);
            }
        }
    }

    public void endCraft() {
        vis = 0.0f;
        ItemStack focus = getItem(0);
        if (!focus.isEmpty() && focus.getItem() instanceof FocusItem) {
            FocusPackage core = generateFocus();
            if (core != null) {
                level.playSound(null, worldPosition, ModSounds.get("wand"), SoundSource.BLOCKS, 1.0f, 1.0f);
                focus = focus.copy();
                focus.set(DataComponents.CUSTOM_NAME, Component.literal(focusName));
                FocusItem.setPackage(focus, core);
                super.setItem(0, focus);
                crystalsSync = new LinkedHashMap<>();
                data.clear();
                sync();
                setChanged();
            }
        }
    }

    private static int countCrystals(Player p, String aspect) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (CrystalEssenceItem.aspect(s).equals(aspect)) n += s.getCount();
        return n;
    }

    private static void consumeCrystals(Player p, String aspect, int amount) {
        var inv = p.getInventory().getNonEquipmentItems();
        for (int i = 0; i < inv.size() && amount > 0; i++) {
            ItemStack s = inv.get(i);
            if (!CrystalEssenceItem.aspect(s).equals(aspect)) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
    }

    public boolean startCraft(ServerPlayer p) {
        ItemStack in = getItem(0);
        if (data == null || data.isEmpty() || vis > 0.0f || in.isEmpty() || !(in.getItem() instanceof FocusItem f)) return false;
        int maxComplexity = f.getMaxComplexity();
        int totalComplexity = 0;
        Map<String, Integer> crystals = new LinkedHashMap<>();
        Map<String, Integer> compCount = new HashMap<>();
        var state = PlayerProgressManager.get(p);
        for (FocusElementNode node : data.values()) {
            if (node.node == null) return false;
            if (!ResearchRules.knows(state, node.node.getResearch())) return false;
            int a = compCount.getOrDefault(node.node.getKey(), 0) + 1;
            node.complexityMultiplier = 0.5f * (a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity += (int) (node.node.getComplexity() * node.complexityMultiplier);
            if (node.node.getAspect() == null) continue;
            crystals.merge(node.node.getAspect(), 1, Integer::sum);
        }
        vis = totalComplexity * 10 + maxComplexity / 5;
        xpCost = (int) Math.max(1L, Math.round(Math.sqrt(totalComplexity)));
        if (!p.getAbilities().instabuild && p.experienceLevel < xpCost) { vis = 0.0f; return false; }
        if (!p.getAbilities().instabuild) p.giveExperienceLevels(-xpCost);
        if (!crystals.isEmpty()) {
            for (var e : crystals.entrySet()) if (countCrystals(p, e.getKey()) < e.getValue()) { vis = 0.0f; return false; }
            for (var e : crystals.entrySet()) consumeCrystals(p, e.getKey(), e.getValue());
            crystalsSync = new LinkedHashMap<>(crystals);
            setChanged();
            sync();
            level.playSound(null, worldPosition, ModSounds.get("craftstart"), SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }
        vis = 0.0f;
        return false;
    }

    @Override
    public boolean triggerEvent(int i, int j) {
        if (i == 1) { doGuiReset = true; return true; }
        if (i == 5) {
            if (level != null && level.isClientSide()) {
                var r = level.getRandom();
                art.arcane.thaumcraft.client.fx.FXDispatcher.visSparkle(worldPosition.getX() + r.nextInt(3) - r.nextInt(3), worldPosition.getY() + r.nextInt(3),
                        worldPosition.getZ() + r.nextInt(3) - r.nextInt(3), worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), j);
            }
            return true;
        }
        return super.triggerEvent(i, j);
    }

    // ---- persistence and client sync (readSyncNBT / writeSyncNBT) ----
    private void writeSync(ValueOutput out) {
        out.putFloat("vis", vis);
        out.putString("focusName", focusName);
        CompoundTag cr = new CompoundTag();
        crystalsSync.forEach(cr::putInt);
        out.store("crystals", CompoundTag.CODEC, cr);
        ListTag nodes = new ListTag();
        for (FocusElementNode n : data.values()) nodes.add(n.serialize());
        CompoundTag holder = new CompoundTag();
        holder.put("nodes", nodes);
        out.store("nodes", CompoundTag.CODEC, holder);
    }

    private void readSync(ValueInput in) {
        vis = in.getFloatOr("vis", 0);
        focusName = in.getStringOr("focusName", "");
        crystalsSync = new LinkedHashMap<>();
        in.read("crystals", CompoundTag.CODEC).ifPresent(t -> t.keySet().forEach(k -> crystalsSync.put(k, t.getIntOr(k, 0))));
        data.clear();
        in.read("nodes", CompoundTag.CODEC).ifPresent(t -> {
            var list = t.getListOrEmpty("nodes");
            for (int i = 0; i < list.size(); i++) { FocusElementNode n = new FocusElementNode(); n.deserialize(list.getCompoundOrEmpty(i)); data.put(n.id, n); }
        });
        if (level != null && level.isClientSide()) doGather = true;
    }

    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); ContainerHelper.saveAllItems(out, items); writeSync(out); }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        readSync(in);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
