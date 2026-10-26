package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.PatternCrafterBlock;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * TC6 TilePatternCrafter: once a second, while enabled, pulls the items for the selected 3x3 pattern from the inventory
 * above, crafts them and pushes the result (plus container leftovers) into the inventory below; 1 vis per 5 crafts.
 */
public final class PatternCrafterBlockEntity extends BlockEntity {
    private static final int[] AMOUNTS = {9, 1, 2, 2, 4, 3, 3, 6, 6, 8};
    public byte type;
    public int count = new java.util.Random().nextInt(20);
    float power;
    public float rot, rp;
    public int rotTicks;

    public PatternCrafterBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PATTERN_CRAFTER.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, PatternCrafterBlockEntity te) {
        if (world.isClientSide()) {
            if (te.rotTicks > 0) {
                --te.rotTicks;
                if (te.rotTicks % Math.floor(Math.max(1.0f, te.rp)) == 0.0)
                    world.playLocalSound(pos, ModSounds.get("clack"), SoundSource.BLOCKS, 0.2f, 1.7f, false);
                ++te.rp;
            } else te.rp *= 0.8f;
            te.rot += te.rp;
            return;
        }
        if (te.count++ % 20 != 0 || !state.getValue(PatternCrafterBlock.ENABLED)) return;
        ServerLevel sl = (ServerLevel) world;
        if (te.power <= 0.0f) te.power += NativeAura.drainVis(sl, pos, 5.0f, false);
        int amt = AMOUNTS[Math.clamp(te.type, 0, 9)];
        var above = sl.getCapability(Capabilities.Item.BLOCK, pos.above(), Direction.DOWN);
        var below = sl.getCapability(Capabilities.Item.BLOCK, pos.below(), Direction.UP);
        if (above == null || below == null || te.power < 1.0f) return;
        for (int a = 0; a < above.size(); ++a) {
            ItemResource res = above.getResource(a);
            if (res.isEmpty()) continue;
            ItemStack in = res.toStack(1);
            var recipeIn = te.pattern(in);
            var recipe = sl.recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeIn, sl);
            if (recipe.isEmpty()) continue;
            ItemStack out = recipe.get().value().assemble(recipeIn);
            if (out.isEmpty()) continue;
            List<ItemStack> outputs = new ArrayList<>();
            outputs.add(out);
            for (ItemStack r : recipe.get().value().getRemainingItems(recipeIn)) if (!r.isEmpty()) outputs.add(r);
            try (var tx = Transaction.openRoot()) {
                if (above.extract(res, amt, tx) != amt) continue;
                boolean ok = true;
                for (ItemStack o : outputs) {
                    if (ResourceHandlerUtil.insertStacking(below, ItemResource.of(o), o.getCount(), tx) != o.getCount()) { ok = false; break; }
                }
                if (!ok) continue;
                tx.commit();
            }
            sl.blockEvent(pos, state.getBlock(), 1, 0);
            --te.power;
            te.setChanged();
            break;
        }
    }

    /** craft(): lays out the input according to the selected mode (TC6 slot indices a + b*3). */
    private CraftingInput pattern(ItemStack one) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; ++i) grid.add(ItemStack.EMPTY);
        int[] slots = switch (type) {
            case 1 -> new int[] {0};
            case 2 -> new int[] {0, 1};
            case 3 -> new int[] {0, 3};
            case 4 -> new int[] {0, 1, 3, 4};
            case 5 -> new int[] {0, 1, 2};
            case 6 -> new int[] {0, 3, 6};
            case 7 -> new int[] {0, 1, 2, 3, 4, 5};
            case 8 -> new int[] {0, 1, 3, 4, 6, 7};
            case 9 -> new int[] {0, 1, 2, 3, 5, 6, 7, 8};
            default -> new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
        };
        for (int s : slots) grid.set(s, one.copyWithCount(1));
        return CraftingInput.of(3, 3, grid);
    }

    public void cycle() {
        if (++type > 9) type = 0;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public boolean triggerEvent(int id, int param) {
        if (id == 1) { if (level != null && level.isClientSide()) rotTicks = 10; return true; }
        return super.triggerEvent(id, param);
    }
    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putFloat("power", power); o.putByte("type", type); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); power = i.getFloatOr("power", 0); type = i.getByteOr("type", (byte) 0); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
