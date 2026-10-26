package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.block.PotionSprayerBlock;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.menu.PotionSprayerMenu;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * TC6 TilePotionSprayer: a potion sets an essentia recipe (its aspects, culled to 10); essentia is sucked in (128) from
 * every side but the front until the recipe is met, giving one charge (max 8). A redstone pulse spends a charge and
 * applies the potion to living things in a 3x3x3 area two blocks in front.
 */
public final class PotionSprayerBlockEntity extends BlockEntity implements Container, MenuProvider, EssentiaPort {
    private static final Codec<Map<String, Integer>> MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private ItemStack potion = ItemStack.EMPTY;
    public Map<String, Integer> recipe = new TreeMap<>(), progress = new TreeMap<>();
    public int charges, color;
    private int counter, venting;
    private boolean activated;
    private String currentSuction;

    public PotionSprayerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.POTION_SPRAYER.get(), pos, state); }

    private Direction facing() { return getBlockState().getValue(PotionSprayerBlock.FACING); }

    public static boolean isValidPotion(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.LINGERING_POTION) && !stack.is(Items.SPLASH_POTION)) return false;
        var pc = stack.get(DataComponents.POTION_CONTENTS);
        if (pc == null || pc.potion().isEmpty()) return false;
        var p = pc.potion().get();
        return !p.is(Potions.WATER) && !p.is(Potions.AWKWARD) && !p.is(Potions.MUNDANE) && !p.is(Potions.THICK);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, PotionSprayerBlockEntity te) {
        ++te.counter;
        Direction facing = state.getValue(PotionSprayerBlock.FACING);
        if (world.isClientSide()) {
            if (te.venting > 0) {
                --te.venting;
                var r = world.getRandom();
                for (int a = 0; a < te.venting / 2; ++a) {
                    float fx = 0.1f - r.nextFloat() * 0.2f, fz = 0.1f - r.nextFloat() * 0.2f, fy = 0.1f - r.nextFloat() * 0.2f;
                    art.arcane.thaumcraft.client.fx.FXDispatcher.drawVentParticles2(pos.getX() + 0.5f + fx + facing.getStepX() / 2.0f, pos.getY() + 0.5f + fy + facing.getStepY() / 2.0f,
                            pos.getZ() + 0.5f + fz + facing.getStepZ() / 2.0f, r.nextGaussian() * 0.06 + facing.getStepX() * 0.25, r.nextGaussian() * 0.06 + facing.getStepY() * 0.25,
                            r.nextGaussian() * 0.06 + facing.getStepZ() * 0.25, te.color, 4.0f);
                }
            }
            return;
        }
        if (te.counter % 5 == 0) {
            te.currentSuction = null;
            if (te.potion.isEmpty() || te.charges >= 8) return;
            boolean done = true;
            for (var e : te.recipe.entrySet()) {
                if (te.progress.getOrDefault(e.getKey(), 0) < e.getValue()) { te.currentSuction = e.getKey(); done = false; break; }
            }
            if (done) { te.progress = new TreeMap<>(); ++te.charges; te.sync(); }
            else if (te.currentSuction != null) te.fill();
        }
        if (!state.getValue(PotionSprayerBlock.ENABLED)) {
            if (!te.activated && te.charges > 0) {
                --te.charges;
                var pc = te.potion.get(DataComponents.POTION_CONTENTS);
                if (pc != null) {
                    BlockPos p = pos.relative(facing, 2);
                    for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(1))) {
                        if (!e.isAlive() || !e.isAffectedByPotions()) continue;
                        for (MobEffectInstance mi : pc.getAllEffects()) {
                            var eff = mi.getEffect().value();
                            if (eff.isInstantaneous()) eff.applyInstantaneousEffect((ServerLevel) world, null, null, e, mi.getAmplifier(), 1.0);
                            else e.addEffect(new MobEffectInstance(mi.getEffect(), mi.getDuration(), mi.getAmplifier()));
                        }
                    }
                }
                world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 2.6f + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8f);
                world.blockEvent(pos, state.getBlock(), 0, 0);
                te.sync();
            }
            te.activated = true;
        } else te.activated = false;
    }

    /** Pull one point of the wanted aspect from a connected neighbour of this block (or of the block above). */
    private void fill() {
        Direction facing = facing();
        for (int y = 0; y <= 1; ++y) for (Direction dir : Direction.values()) {
            if (dir == facing) continue;
            BlockPos from = worldPosition.above(y);
            var ic = EssentiaTubeBlockEntity.neighbor(level, from, dir);
            if (ic == null || ic == this) continue;
            Direction back = dir.getOpposite();
            if (ic.suction(back) < suction(null) && suction(null) >= ic.minimumSuction()) {
                String offered = ic.offered(currentSuction, back);
                if (currentSuction.equals(offered) && ic.extractOne(currentSuction, back)) { addToContainer(currentSuction, 1); return; }
            }
        }
    }

    public int addToContainer(String tag, int am) {
        int ce = recipe.getOrDefault(tag, 0) - progress.getOrDefault(tag, 0);
        if (ce <= 0) return am;
        int add = Math.min(ce, am);
        progress.merge(tag, add, Integer::sum);
        sync();
        return am - add;
    }

    private void recalcAspects() {
        if (level == null || level.isClientSide()) return;
        color = 0x333333;
        recipe = new TreeMap<>();
        if (!potion.isEmpty()) {
            var aspects = potionAspects(potion);
            var culled = ObjectAspects.cull(aspects, 10);
            culled.forEach((a, n) -> recipe.put(a.id(), n));
            var pc = potion.get(DataComponents.POTION_CONTENTS);
            if (pc != null) color = pc.getColor() & 0xFFFFFF;
        }
        charges = 0;
        progress = new TreeMap<>();
        sync();
    }

    /** ConfigAspects.getPotionAspects: the registered potion tags without the container's own +5. */
    private static LinkedHashMap<Aspect, Integer> potionAspects(ItemStack stack) {
        var out = new LinkedHashMap<>(ObjectAspects.get(stack));
        Aspect extra = stack.is(Items.SPLASH_POTION) ? AspectRegistry.POTENTIA : stack.is(Items.LINGERING_POTION) ? AspectRegistry.VINCULUM : AspectRegistry.AQUA;
        out.computeIfPresent(extra, (k, v) -> v - 5 > 0 ? v - 5 : null);
        return out;
    }

    // ---- EssentiaPort: receives from every side but the front, never gives ----
    @Override public boolean connects(Direction side) { return side != facing(); }
    @Override public int suction() { return currentSuction != null ? 128 : 0; }
    @Override public String wanted() { return currentSuction == null ? "" : currentSuction; }
    @Override public String aspect() { return ""; }
    @Override public String offered(String requested, Direction side) { return ""; }
    @Override public boolean extractOne(String aspect, Direction side) { return false; }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id >= 0) { if (level != null && level.isClientSide()) venting = 15; return true; }
        return super.triggerEvent(id, param);
    }

    void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    // ---- inventory ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return potion.isEmpty(); }
    @Override public ItemStack getItem(int i) { return potion; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = potion.split(n); recalcAspects(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = potion; potion = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { potion = s; recalcAspects(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return isValidPotion(s); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { potion = ItemStack.EMPTY; }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.potion_sprayer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new PotionSprayerMenu(id, inv, this); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!potion.isEmpty()) out.store("potion", ItemStack.CODEC, potion);
        out.store("recipe", MAP, recipe); out.store("progress", MAP, progress);
        out.putInt("charges", charges); out.putInt("color", color);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        potion = in.read("potion", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        recipe = new TreeMap<>(in.read("recipe", MAP).orElse(Map.of()));
        progress = new TreeMap<>(in.read("progress", MAP).orElse(Map.of()));
        charges = in.getIntOr("charges", 0); color = in.getIntOr("color", 0);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
