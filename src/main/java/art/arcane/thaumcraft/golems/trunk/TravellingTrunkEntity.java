package art.arcane.thaumcraft.golems.trunk;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.construct.OwnedConstruct;
import art.arcane.thaumcraft.golems.GolemBellItem;
import art.arcane.thaumcraft.golems.ai.FollowOwnerGoal;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Thaumcraft 4 Travelling Trunk, recreated (TC6 dropped it): an owned, living chest that follows its owner and
 * teleports to them like tamed animals, carries 27 slots (54 with an Earth upgrade), heals slowly or by feeding,
 * and takes one elemental upgrade (sources and inventions in docs/BAUL_FUENTES.md).
 */
public class TravellingTrunkEntity extends OwnedConstruct {
    /** Upgrade ids: -1 none; order matches TC4's golem upgrade list (air, earth, fire, water, order, entropy). */
    public static final String[] UPGRADES = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    public static final int AIR = 0, EARTH = 1, FIRE = 2, WATER = 3, ORDER = 4, ENTROPY = 5;
    private static final EntityDataAccessor<Byte> UPGRADE = SynchedEntityData.defineId(TravellingTrunkEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> STAY = SynchedEntityData.defineId(TravellingTrunkEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> OPEN = SynchedEntityData.defineId(TravellingTrunkEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ANGRY = SynchedEntityData.defineId(TravellingTrunkEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double BASE_SPEED = 0.3, AIR_SPEED = 0.42;

    private final TrunkContainer inventory = new TrunkContainer(54);
    /** Client lid animation (0 closed .. 1 fully open). */
    public float lidAngle, prevLidAngle;
    private int eatTicks;

    public TravellingTrunkEntity(EntityType<? extends TravellingTrunkEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(UPGRADE, (byte) -1); b.define(STAY, false); b.define(OPEN, (byte) 0); b.define(ANGRY, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new StayGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) { @Override public boolean canUse() { return getUpgrade() == FIRE && super.canUse(); } });
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 3.0f, 2.0f) { @Override public boolean canUse() { return !isStaying() && super.canUse(); } });
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (t, l) -> t instanceof Enemy) {
            @Override public boolean canUse() { return getUpgrade() == FIRE && super.canUse(); }
        });
    }

    // --- state ---
    public int getUpgrade() { return entityData.get(UPGRADE); }
    public void setUpgrade(int u) {
        entityData.set(UPGRADE, (byte) u);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(u == AIR ? AIR_SPEED : BASE_SPEED);
    }
    public boolean isStaying() { return entityData.get(STAY); }
    public void setStaying(boolean s) { entityData.set(STAY, s); if (s) getNavigation().stop(); }
    public boolean isOpen() { return entityData.get(OPEN) > 0; }
    public boolean isAngry() { return entityData.get(ANGRY); }
    public int slotCount() { return getUpgrade() == EARTH ? 54 : 27; }
    public SimpleContainer inventory() { return inventory; }

    // --- ticking ---
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) {
            prevLidAngle = lidAngle;
            float target = isOpen() ? 1.0f : 0.0f;
            if (eatTicks > 0) { eatTicks--; target = 0.6f; }
            else if (!isOpen() && walkAnimation.speed() > 0.05f) target = Math.abs((float) Math.sin(walkAnimation.position() * 0.6f)) * 0.35f;
            lidAngle += (target - lidAngle) * 0.35f;
        }
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % (getUpgrade() == WATER ? 20 : 50) == 0 && getHealth() < getMaxHealth()) heal(1.0f);
        entityData.set(ANGRY, getTarget() != null && getTarget().isAlive());
        if (getUpgrade() == ENTROPY && tickCount % 5 == 0) suckItems(sl);
    }

    private void suckItems(ServerLevel sl) {
        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(3.0), e -> e.isAlive() && !e.hasPickUpDelay())) {
            ItemStack before = ie.getItem();
            ItemStack rest = addToInventory(before.copy());
            if (rest.getCount() != before.getCount()) {
                if (rest.isEmpty()) ie.discard(); else ie.setItem(rest);
                playSound(SoundEvents.ITEM_PICKUP, 0.2f, 0.8f + random.nextFloat() * 0.4f);
                level().broadcastEntityEvent(this, (byte) 60);
            }
        }
    }

    /** Inserts into the usable slots only (27 or 54); returns the remainder. */
    public ItemStack addToInventory(ItemStack stack) {
        for (int i = 0; i < slotCount() && !stack.isEmpty(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                int n = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                if (n > 0) { s.grow(n); stack.shrink(n); }
            }
        }
        for (int i = 0; i < slotCount() && !stack.isEmpty(); i++)
            if (inventory.getItem(i).isEmpty()) { inventory.setItem(i, stack.copy()); stack.setCount(0); }
        inventory.setChanged();
        return stack;
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 60) eatTicks = 6; else super.handleEntityEvent(id);
    }

    // --- interaction ---
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRemoved()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!isOwner(player)) {
            msg(player, Component.translatable("tc.notowned").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            setStaying(!isStaying());
            msg(player, Component.translatable(isStaying() ? "tc.trunk.stay" : "tc.trunk.follow"));
            playSound(ModSounds.get("clack"), 1.0f, isStaying() ? 0.8f : 1.2f);
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof GolemBellItem) { pickUp((ServerPlayer) player); return InteractionResult.SUCCESS; }
        if (held.is(ModItems.CRYSTAL_ESSENCE.get())) {
            int u = upgradeFor(held);
            if (u >= 0) {
                if (getUpgrade() >= 0) { msg(player, Component.translatable("tc.trunk.upgraded")); return InteractionResult.SUCCESS; }
                applyUpgrade(u);
                if (!player.getAbilities().instabuild) held.shrink(1);
                msg(player, Component.translatable("tc.trunk.upgrade." + UPGRADES[u]));
                return InteractionResult.SUCCESS;
            }
        }
        var food = held.get(DataComponents.FOOD);
        if (food != null && getHealth() < getMaxHealth()) {
            heal(food.nutrition());
            if (!player.getAbilities().instabuild) held.shrink(1);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.6f, 1.0f);
            level().broadcastEntityEvent(this, (byte) 60);
            return InteractionResult.SUCCESS;
        }
        openInventory(player);
        return InteractionResult.SUCCESS;
    }

    private static void msg(Player p, Component c) {
        if (p instanceof ServerPlayer sp && sp.connection == null) return; // mock players in game tests
        p.sendOverlayMessage(c);
    }

    public static int upgradeFor(ItemStack crystal) {
        String a = CrystalEssenceItem.aspect(crystal);
        for (int i = 0; i < UPGRADES.length; i++) if (UPGRADES[i].equals(a)) return i;
        return -1;
    }

    public void applyUpgrade(int u) {
        setUpgrade(u);
        playSound(ModSounds.get("upgrade"), 1.0f, 1.0f);
    }

    public void openInventory(Player player) {
        int rows = slotCount() / 9;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> rows == 6 ? ChestMenu.sixRows(id, inv, inventory) : ChestMenu.threeRows(id, inv, inventory.view27()),
                hasCustomName() ? getCustomName() : Component.translatable("entity.thaumcraft.travelling_trunk")));
    }

    // --- damage, protection, pickup ---
    @Override public boolean hurtServer(ServerLevel level, DamageSource src, float amount) {
        Entity e = src.getEntity();
        if (e instanceof Player p && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (isOwner(p) && p instanceof ServerPlayer sp) {
                if (p.isShiftKeyDown()) pickUp(sp); // sneak-hit: straight into the owner's inventory
                else dropAsItem();                  // plain hit: pops off as an item on the ground, contents kept
            }
            return false; // players never hurt a trunk; only its owner can pick it up
        }
        if (getUpgrade() == WATER && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) amount *= 0.1f;
        return super.hurtServer(level, src, amount);
    }

    /** The trunk as an item, keeping contents, upgrade and name. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModItems.TRAVELLING_TRUNK.get());
        NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
        boolean any = false;
        for (int i = 0; i < 54; i++) { items.set(i, inventory.getItem(i).copy()); any |= !items.get(i).isEmpty(); }
        if (any) stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        if (getUpgrade() >= 0) stack.set(ModComponents.VARIANT.get(), getUpgrade() + 1);
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        return stack;
    }

    public void pickUp(ServerPlayer player) {
        ItemStack stack = toItem();
        inventory.clearContent();
        playSound(ModSounds.get("zap"), 1.0f, 1.0f);
        if (!player.getInventory().add(stack)) spawnAtLocation((ServerLevel) level(), stack, 0.5f);
        discard();
    }

    /** Port addition (user request): the owner can knock the trunk loose as an item on the ground and place it again. */
    public void dropAsItem() {
        ItemStack stack = toItem();
        inventory.clearContent();
        playSound(ModSounds.get("zap"), 1.0f, 1.0f);
        var drop = spawnAtLocation((ServerLevel) level(), stack, 0.5f);
        if (drop != null) { drop.setUnlimitedLifetime(); drop.setDefaultPickUpDelay(); }
        discard();
    }

    /** Loads contents/upgrade from a trunk item. */
    public void fromItem(ItemStack stack) {
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        if (c != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
            c.copyInto(items);
            for (int i = 0; i < 54; i++) inventory.setItem(i, items.get(i));
        }
        Integer v = stack.get(ModComponents.VARIANT.get());
        if (v != null && v > 0 && v <= UPGRADES.length) setUpgrade(v - 1);
        if (stack.has(DataComponents.CUSTOM_NAME)) setCustomName(stack.getHoverName());
    }

    @Override public void die(DamageSource cause) {
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            if (getUpgrade() == ORDER) spawnAtLocation(sl, toItem(), 0.5f); // Order: dies back into an item, contents kept
            else {
                for (int i = 0; i < 54; i++) { ItemStack s = inventory.getItem(i); if (!s.isEmpty()) spawnAtLocation(sl, s.copy(), 0.5f); }
                ItemStack empty = new ItemStack(ModItems.TRAVELLING_TRUNK.get());
                if (getUpgrade() >= 0) empty.set(ModComponents.VARIANT.get(), getUpgrade() + 1);
                spawnAtLocation(sl, empty, 0.5f);
            }
            inventory.clearContent();
        }
        super.die(cause);
    }

    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) { target.igniteForSeconds(4.0f); level.broadcastEntityEvent(this, (byte) 60); playSound(ModSounds.get("crabclaw"), 0.6f, 1.0f); }
        return hit;
    }

    // --- sounds ---
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(SoundEvents.WOOD_STEP, 0.15f, 1.3f); }

    // --- save ---
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
        for (int i = 0; i < 54; i++) items.set(i, inventory.getItem(i));
        out.store("Items", ItemContainerContents.CODEC, ItemContainerContents.fromItems(items));
        out.putByte("Upgrade", (byte) getUpgrade());
        out.putBoolean("Stay", isStaying());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.read("Items", ItemContainerContents.CODEC).ifPresent(c -> {
            NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
            c.copyInto(items);
            for (int i = 0; i < 54; i++) inventory.setItem(i, items.get(i));
        });
        setUpgrade(in.getByteOr("Upgrade", (byte) -1));
        setStaying(in.getBooleanOr("Stay", false));
    }

    public static TravellingTrunkEntity spawn(ServerLevel level, double x, double y, double z, float yaw, Player owner, ItemStack from) {
        TravellingTrunkEntity t = ModEntities.TRAVELLING_TRUNK.get().create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (t == null) return null;
        t.snapTo(x, y, z, yaw, 0);
        t.setOwned(true); t.setValidSpawn();
        t.entityData.set(OWNER, java.util.Optional.of(net.minecraft.world.entity.EntityReference.<net.minecraft.world.entity.LivingEntity>of(owner)));
        if (from != null) t.fromItem(from);
        level.addFreshEntity(t);
        return t;
    }

    /** TC4: "it will even follow you between dimensions". Range in blocks around the owner's departure point. */
    public static final double DIMENSION_FOLLOW_RANGE = 16.0;
    private static final java.util.Map<UUID, java.util.List<TravellingTrunkEntity>> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    /** Following (not staying) trunks owned by {@code owner} within range in its current level. */
    public static java.util.List<TravellingTrunkEntity> followers(net.minecraft.server.level.ServerPlayer owner) {
        return owner.level().getEntitiesOfClass(TravellingTrunkEntity.class, owner.getBoundingBox().inflate(DIMENSION_FOLLOW_RANGE),
                t -> t.isAlive() && !t.isStaying() && owner.getUUID().equals(t.getOwnerId()) && !t.isPassenger());
    }

    /** Moves the trunk to {@code dest} next to the owner; contents, upgrade and owner travel with the entity's data. */
    public static TravellingTrunkEntity bringTo(TravellingTrunkEntity trunk, ServerLevel dest, Vec3 pos, float yaw) {
        trunk.stopRiding();
        trunk.ejectPassengers();
        trunk.getNavigation().stop();
        var moved = trunk.teleport(new net.minecraft.world.level.portal.TeleportTransition(dest, pos, Vec3.ZERO, yaw, 0.0f,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        return moved instanceof TravellingTrunkEntity t ? t : null;
    }

    /** Owner is about to leave: remember the trunks following it. */
    public static void onOwnerTravel(net.minecraft.server.level.ServerPlayer owner) {
        var list = followers(owner);
        if (!list.isEmpty()) PENDING.put(owner.getUUID(), list); else PENDING.remove(owner.getUUID());
    }

    /** Owner arrived in its new level: bring the remembered trunks along. */
    public static void onOwnerArrived(net.minecraft.server.level.ServerPlayer owner) {
        var list = PENDING.remove(owner.getUUID());
        if (list == null || !(owner.level() instanceof ServerLevel dest)) return;
        for (var t : list) if (t.isAlive() && t.level() != dest) bringTo(t, dest, owner.position(), owner.getYRot());
    }

    /** Keeps the trunk still while ordered to stay. */
    private class StayGoal extends Goal {
        StayGoal() { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return isStaying() && getTarget() == null; }
        @Override public void start() { getNavigation().stop(); }
    }

    /** Inventory that tracks viewers to open the lid, and refuses use once the trunk is gone or far away. */
    private class TrunkContainer extends SimpleContainer {
        TrunkContainer(int n) { super(n); }
        @Override public boolean stillValid(Player p) { return isAlive() && p.distanceToSqr(TravellingTrunkEntity.this) < 64.0; }
        @Override public void startOpen(net.minecraft.world.entity.ContainerUser user) {
            entityData.set(OPEN, (byte) Math.min(127, entityData.get(OPEN) + 1));
            if (entityData.get(OPEN) == 1) playSound(SoundEvents.CHEST_OPEN, 0.5f, 1.0f);
        }
        @Override public void stopOpen(net.minecraft.world.entity.ContainerUser user) {
            entityData.set(OPEN, (byte) Math.max(0, entityData.get(OPEN) - 1));
            if (entityData.get(OPEN) == 0) playSound(SoundEvents.CHEST_CLOSE, 0.5f, 1.0f);
        }
        /** First 27 slots as their own container (no Earth upgrade). */
        SimpleContainer view27() {
            TrunkContainer self = this;
            return new SimpleContainer(27) {
                @Override public int getContainerSize() { return 27; }
                @Override public ItemStack getItem(int i) { return self.getItem(i); }
                @Override public ItemStack removeItem(int i, int n) { return self.removeItem(i, n); }
                @Override public ItemStack removeItemNoUpdate(int i) { return self.removeItemNoUpdate(i); }
                @Override public void setItem(int i, ItemStack s) { self.setItem(i, s); }
                @Override public boolean stillValid(Player p) { return self.stillValid(p); }
                @Override public void startOpen(net.minecraft.world.entity.ContainerUser u) { self.startOpen(u); }
                @Override public void stopOpen(net.minecraft.world.entity.ContainerUser u) { self.stopOpen(u); }
                @Override public boolean isEmpty() { for (int i = 0; i < 27; i++) if (!self.getItem(i).isEmpty()) return false; return true; }
                @Override public void setChanged() { self.setChanged(); }
            };
        }
    }
}
