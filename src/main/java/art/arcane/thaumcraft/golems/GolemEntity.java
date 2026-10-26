package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.construct.OwnedConstruct;
import art.arcane.thaumcraft.golems.ai.FollowOwnerGoal;
import art.arcane.thaumcraft.golems.ai.GotoBlockGoal;
import art.arcane.thaumcraft.golems.ai.GotoEntityGoal;
import art.arcane.thaumcraft.golems.ai.GotoHomeGoal;
import art.arcane.thaumcraft.golems.ai.OwnerHurtByTargetGoal;
import art.arcane.thaumcraft.golems.ai.OwnerHurtTargetGoal;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityThaumcraftGolem: a construct assembled from parts (see {@link GolemProperties}), driven by seal tasks or
 * following its owner. Carries one stack (two with the hauler addon) in its hands.
 */
public class GolemEntity extends OwnedConstruct implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> PROPS1 = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROPS2 = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROPS3 = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> CLIMBING = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BYTE);
    public static final int XPM = 1000;
    public int rankXp = 0;
    public boolean redrawParts = false;
    private boolean firstRun = true;
    protected Task task = null;
    protected int taskID = Integer.MAX_VALUE;
    /** Client: wheel rotation (GolemLegWheels.ani). */
    public float wheelRot;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        setDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(2, new GotoEntityGoal(this));
        goalSelector.addGoal(3, new GotoBlockGoal(this));
        goalSelector.addGoal(4, new GotoHomeGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PROPS1, 0); b.define(PROPS2, 0); b.define(PROPS3, 0); b.define(CLIMBING, (byte) 0);
    }

    // ---- properties packed into two synced ints (ByteBuffer big-endian, as TC6)
    public GolemProperties getProperties() {
        long hi = entityData.get(PROPS1) & 0xFFFFFFFFL, lo = entityData.get(PROPS2) & 0xFFFFFFFFL;
        return GolemProperties.fromLong((hi << 32) | lo);
    }
    public void setProperties(GolemProperties p) {
        long l = p.toLong();
        entityData.set(PROPS1, (int) (l >>> 32));
        entityData.set(PROPS2, (int) l);
    }
    private byte[] props3() { int v = entityData.get(PROPS3); return new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v}; }
    private void props3(byte[] b) { entityData.set(PROPS3, (b[0] & 255) << 24 | (b[1] & 255) << 16 | (b[2] & 255) << 8 | (b[3] & 255)); }
    public byte getGolemColor() { return props3()[0]; }
    public void setGolemColor(byte b) { byte[] a = props3(); a[0] = b; props3(a); }
    public byte getFlags() { return props3()[1]; }
    public void setFlags(byte b) { byte[] a = props3(); a[1] = b; props3(a); }
    private boolean flag(int bit) { return (getFlags() & (1 << bit)) != 0; }
    private void flag(int bit, boolean v) { setFlags((byte) (v ? getFlags() | (1 << bit) : getFlags() & ~(1 << bit))); }

    public float getGolemEyeHeight() { return 0.7f; }

    /** updateEntityAttributes: health, step, home radius, follow range, navigator, move helper, damage, then rebuild AI. */
    public void updateEntityAttributes() {
        GolemProperties p = getProperties();
        int mh = 10 + p.getMaterial().healthMod;
        if (p.hasTrait(GolemTrait.FRAGILE)) mh = (int) (mh * 0.75);
        mh += p.getRank();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(mh);
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(p.hasTrait(GolemTrait.WHEELED) ? 0.5 : 0.6);
        setHomeTo(!hasHome() || getHomePosition().equals(BlockPos.ZERO) ? blockPosition() : getHomePosition(), p.hasTrait(GolemTrait.SCOUT) ? 48 : 32);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(p.hasTrait(GolemTrait.SCOUT) ? 56.0 : 40.0);
        navigation = getGolemNavigator();
        if (p.hasTrait(GolemTrait.FLYER)) moveControl = new FlyingMoveControl(this);
        if (p.hasTrait(GolemTrait.FIGHTER)) {
            double da = p.getMaterial().damage;
            if (p.hasTrait(GolemTrait.BRUTAL)) da = Math.max(da * 1.5, da + 1.0);
            da += p.getRank() * 0.25;
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(da);
        } else getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
        createAI();
    }

    private void createAI() {
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        if (isFollowingOwner()) goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        else {
            goalSelector.addGoal(3, new GotoEntityGoal(this));
            goalSelector.addGoal(4, new GotoBlockGoal(this));
            goalSelector.addGoal(5, new GotoHomeGoal(this));
        }
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        GolemProperties p = getProperties();
        if (p.hasTrait(GolemTrait.FIGHTER)) {
            if (getNavigation() instanceof GroundPathNavigation) goalSelector.addGoal(0, new FloatGoal(this));
            if (p.hasTrait(GolemTrait.RANGED) && p.getArms().function != null) {
                Goal aa = p.getArms().function.getRangedAttackAI(this);
                if (aa != null) goalSelector.addGoal(1, aa);
            }
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
            if (isFollowingOwner()) {
                targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
                targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
            }
            targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }
    }

    @Override public boolean onClimbable() { return isBesideClimbableBlock(); }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setHomeTo(blockPosition(), 32);
        updateEntityAttributes();
        return data;
    }
    /** ItemGolemPlacer calls onInitialSpawn directly. */
    public void initialSpawn() { setHomeTo(blockPosition(), 32); updateEntityAttributes(); }

    @Override public int getArmorValue() {
        GolemProperties p = getProperties();
        int armor = p.getMaterial().armor;
        if (p.hasTrait(GolemTrait.ARMORED)) armor = (int) Math.max(armor * 1.5, armor + 1);
        if (p.hasTrait(GolemTrait.FRAGILE)) armor = (int) (armor * 0.75);
        return armor;
    }

    @Override
    public void tick() {
        super.tick();
        GolemProperties p = getProperties();
        if (p.hasTrait(GolemTrait.FLYER)) setNoGravity(true);
        if (!level().isClientSide()) {
            if (firstRun) {
                firstRun = false;
                if (hasHome() && !blockPosition().equals(getHomePosition())) goHome();
            }
            if (task != null && task.isSuspended()) task = null;
            if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
            if (getTarget() != null && p.hasTrait(GolemTrait.RANGED) && distanceToSqr(getTarget()) > 1024.0) setTarget(null);
            if (level() instanceof ServerLevel sl && !sl.getGameRules().get(GameRules.PVP) && getTarget() instanceof Player) setTarget(null);
            if (tickCount % (p.hasTrait(GolemTrait.REPAIR) ? 40 : 100) == 0) heal(1.0f);
            if (p.hasTrait(GolemTrait.CLIMBER)) setBesideClimbableBlock(horizontalCollision);
        } else if (tickCount < 20 || tickCount % 20 == 0) redrawParts = true;
        if (p.getHead().function != null) p.getHead().function.onUpdateTick(this);
        if (p.getArms().function != null) p.getArms().function.onUpdateTick(this);
        if (p.getLegs().function != null) p.getLegs().function.onUpdateTick(this);
        if (p.getAddon().function != null) p.getAddon().function.onUpdateTick(this);
    }

    /** Emote particles (TC6 handleStatusUpdate 5-9), only when showGolemEmotes is on (default true). */
    public void emote(int id) { if (!level().isClientSide()) level().broadcastEntityEvent(this, (byte) id); }
    @Override
    public void handleEntityEvent(byte id) {
        if (id >= 5 && id <= 9) { if (level().isClientSide()) art.arcane.thaumcraft.golems.client.GolemClientFx.emote(this, id); }
        else super.handleEntityEvent(id);
    }

    public float getGolemMoveSpeed() {
        GolemProperties p = getProperties();
        return 1.0f + p.getRank() * 0.025f + (p.hasTrait(GolemTrait.LIGHT) ? 0.2f : 0.0f) + (p.hasTrait(GolemTrait.HEAVY) ? -0.175f : 0.0f)
                + (p.hasTrait(GolemTrait.FLYER) ? -0.33f : 0.0f) + (p.hasTrait(GolemTrait.WHEELED) ? 0.25f : 0.0f);
    }
    public PathNavigation getGolemNavigator() {
        GolemProperties p = getProperties();
        if (p.hasTrait(GolemTrait.FLYER)) { var n = new FlyingPathNavigation(this, level()); n.setCanOpenDoors(false); n.setCanFloat(true); return n; }
        if (p.hasTrait(GolemTrait.CLIMBER)) return new WallClimberNavigation(this, level());
        var n = new GroundPathNavigation(this, level());
        n.setCanOpenDoors(true);
        n.setCanFloat(true);
        return n;
    }

    /** canTriggerWalking: only heavy walkers make step sounds. */
    @Override protected MovementEmission getMovementEmission() {
        GolemProperties p = getProperties();
        return p.hasTrait(GolemTrait.HEAVY) && !p.hasTrait(GolemTrait.FLYER) ? MovementEmission.ALL : MovementEmission.EVENTS;
    }
    @Override public boolean causeFallDamage(double distance, float mult, DamageSource src) {
        GolemProperties p = getProperties();
        if (p.hasTrait(GolemTrait.FLYER) || p.hasTrait(GolemTrait.CLIMBER)) return false;
        return super.causeFallDamage(distance, mult, src);
    }

    private void goHome() {
        double x0 = getX(), y0 = getY(), z0 = getZ();
        BlockPos home = getHomePosition();
        double px = home.getX() + 0.5, py = home.getY(), pz = home.getZ() + 0.5;
        BlockPos bp = BlockPos.containing(px, py, pz);
        boolean ok = false, top = false;
        while (!top && bp.getY() < level().getMaxY()) {
            BlockPos up = bp.above();
            if (level().getBlockState(up).blocksMotion()) top = true;
            else { ++py; bp = up; }
        }
        if (top) {
            teleportTo(px, py, pz);
            if (level().noCollision(this, getBoundingBox())) ok = true;
        }
        if (!ok) teleportTo(x0, y0, z0);
        else getNavigation().stop();
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putLong("props", getProperties().toLong());
        out.putLong("homepos", getHomePosition().asLong());
        out.putByte("gflags", getFlags());
        out.putInt("rankXP", rankXp);
        out.putByte("color", getGolemColor());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setProperties(GolemProperties.fromLong(in.getLongOr("props", 0L)));
        setHomeTo(BlockPos.of(in.getLongOr("homepos", 0L)), 32);
        setFlags(in.getByteOr("gflags", (byte) 0));
        rankXp = in.getIntOr("rankXP", 0);
        setGolemColor(in.getByteOr("color", (byte) 0));
        updateEntityAttributes();
    }

    @Override public boolean fireImmune() { return getProperties().hasTrait(GolemTrait.FIREPROOF) || super.fireImmune(); }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource ds, float damage) {
        GolemProperties p = getProperties();
        if (ds.is(DamageTypeTags.IS_FIRE) && p.hasTrait(GolemTrait.FIREPROOF)) return;
        if (ds.is(DamageTypeTags.IS_EXPLOSION) && p.hasTrait(GolemTrait.BLASTPROOF)) damage = Math.min(getMaxHealth() / 2.0f, damage * 0.3f);
        if (ds.is(DamageTypes.CACTUS)) return;
        if (hasHome() && (ds.is(DamageTypes.IN_WALL) || ds.is(DamageTypes.FELL_OUT_OF_WORLD))) goHome();
        super.actuallyHurt(level, ds, damage);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRemoved()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof NameTagItem) return InteractionResult.PASS;
        if (!level().isClientSide() && isOwner(player)) {
            if (player.isShiftKeyDown()) {
                playSound(ModSounds.get("zap"), 1.0f, 1.0f);
                if (task != null) task.setReserved(false);
                dropCarried();
                ItemStack placer = new ItemStack(ModItems.GOLEM.get());
                placer.set(ModComponents.GOLEM_PROPS.get(), getProperties().toLong());
                placer.set(ModComponents.GOLEM_XP.get(), rankXp);
                spawnAtLocation((ServerLevel) level(), placer, 0.5f);
                discard();
                player.swing(hand);
            } else if (held.getItem() instanceof GolemBellItem && player instanceof net.minecraft.server.level.ServerPlayer sp && art.arcane.thaumcraft.progression.Warp.knows(sp, "GOLEMDIRECT")) {
                if (task != null) task.setReserved(false);
                playSound(ModSounds.get("scan"), 1.0f, 1.0f);
                setFollowingOwner(!isFollowingOwner());
                if (isFollowingOwner()) {
                    player.sendOverlayMessage(Component.translatable("golem.follow", ""));
                    emote(5);
                    clearHome();
                } else {
                    player.sendOverlayMessage(Component.translatable("golem.stay", ""));
                    emote(8);
                    setHomeTo(blockPosition(), getProperties().hasTrait(GolemTrait.SCOUT) ? 48 : 32);
                }
                updateEntityAttributes();
                player.swing(hand);
            } else if (!held.isEmpty() && held.get(DataComponents.DYE) != null) {
                playSound(ModSounds.get("zap"), 1.0f, 1.0f);
                setGolemColor((byte) (held.get(DataComponents.DYE).getId() + 1));
                held.shrink(1);
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override public void die(DamageSource cause) {
        if (task != null) task.setReserved(false);
        super.die(cause);
        if (!level().isClientSide()) dropCarried();
    }
    protected void dropCarried() {
        for (ItemStack s : getCarrying()) if (!s.isEmpty()) spawnAtLocation((ServerLevel) level(), s.copy(), 0.25f);
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }
    /** dropFewItems: each component has a 30% (+15% per looting) chance to drop, partly. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = 0;
        if (source.getEntity() instanceof LivingEntity le) {
            var ench = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(ench.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), le.getMainHandItem());
        }
        float b = looting * 0.15f;
        for (ItemStack stack : getProperties().generateComponents()) {
            ItemStack s = stack.copy();
            if (random.nextFloat() < 0.3f + b) {
                if (s.getCount() > 0) s.shrink(random.nextInt(s.getCount()));
                spawnAtLocation(level, s, 0.25f);
            }
        }
    }

    public boolean isBesideClimbableBlock() { return (entityData.get(CLIMBING) & 1) != 0; }
    public void setBesideClimbableBlock(boolean c) { byte b = entityData.get(CLIMBING); entityData.set(CLIMBING, (byte) (c ? b | 1 : b & ~1)); }
    public boolean isFollowingOwner() { return flag(1); }
    public void setFollowingOwner(boolean v) { flag(1, v); }
    @Override public void setTarget(@Nullable LivingEntity t) { super.setTarget(t); setInCombat(getTarget() != null); }
    public boolean isInCombat() { return flag(3); }
    public void setInCombat(boolean v) { flag(3, v); }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity ent) {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        var src = damageSources().mobAttack(this);
        boolean flag = ent.hurtServer(level, src, dmg);
        if (flag) {
            if (ent instanceof LivingEntity le && getProperties().hasTrait(GolemTrait.DEFT)) le.setLastHurtByPlayer(java.util.UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(java.nio.charset.StandardCharsets.UTF_8)), 100);
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(level, ent, src);
            if (getProperties().getArms().function != null) getProperties().getArms().function.onMeleeAttack(this, ent);
            if (ent instanceof Mob && !ent.isAlive()) addRankXp(8);
            setLastHurtMob(ent);
        }
        return flag;
    }

    public Task getTask() {
        if (task == null && taskID != Integer.MAX_VALUE) { task = TaskHandler.getTask(level(), taskID); taskID = Integer.MAX_VALUE; }
        return task;
    }
    public void setTask(Task t) { task = t; }

    public void addRankXp(int xp) {
        if (!getProperties().hasTrait(GolemTrait.SMART) || level().isClientSide()) return;
        int rank = getProperties().getRank();
        if (rank < 10) {
            rankXp += xp;
            int xn = (rank + 1) * (rank + 1) * XPM;
            if (rankXp >= xn) {
                rankXp -= xn;
                GolemProperties props = getProperties();
                props.setRank(rank + 1);
                setProperties(props);
                emote(9);
                playSound(SoundEvents.PLAYER_LEVELUP, 0.25f, 1.0f);
            }
        }
    }

    // ---- carrying (IGolemAPI)
    private int slots() { return getProperties().hasTrait(GolemTrait.HAULER) ? 2 : 1; }
    private static EquipmentSlot hand(int a) { return a == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND; }
    public ItemStack holdItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;
        for (int a = 0; a < slots(); a++) {
            ItemStack in = getItemBySlot(hand(a));
            if (in.isEmpty()) { setItemSlot(hand(a), stack); return ItemStack.EMPTY; }
            if (in.getCount() < in.getMaxStackSize() && ItemStack.isSameItemSameComponents(in, stack)) {
                int d = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                stack.shrink(d);
                in.grow(d);
                if (stack.getCount() <= 0) stack = ItemStack.EMPTY;
            }
        }
        return stack;
    }
    public ItemStack dropItem(ItemStack stack) {
        ItemStack out = ItemStack.EMPTY;
        for (int a = 0; a < slots(); a++) {
            ItemStack in = getItemBySlot(hand(a));
            if (in.isEmpty()) continue;
            if (stack == null || stack.isEmpty()) { out = in.copy(); setItemSlot(hand(a), ItemStack.EMPTY); }
            else if (ItemStack.isSameItemSameComponents(in, stack)) {
                out = in.copy();
                out.setCount(Math.min(stack.getCount(), out.getCount()));
                in.shrink(stack.getCount());
                if (in.getCount() <= 0) setItemSlot(hand(a), ItemStack.EMPTY);
            }
            if (!out.isEmpty()) break;
        }
        if (getProperties().hasTrait(GolemTrait.HAULER) && getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && !getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, getItemBySlot(EquipmentSlot.OFFHAND).copy());
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        return out;
    }
    public int canCarryAmount(ItemStack stack) {
        int ss = 0;
        for (int a = 0; a < slots(); a++) {
            ItemStack in = getItemBySlot(hand(a));
            if (in.isEmpty()) ss += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(in, stack)) ss += in.getMaxStackSize() - in.getCount();
        }
        return ss;
    }
    public boolean canCarry(ItemStack stack, boolean partial) {
        int ca = canCarryAmount(stack);
        return ca > 0 && (partial || ca >= stack.getCount());
    }
    public boolean isCarrying(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (int a = 0; a < slots(); a++) { ItemStack in = getItemBySlot(hand(a)); if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack)) return true; }
        return false;
    }
    public NonNullList<ItemStack> getCarrying() {
        if (getProperties().hasTrait(GolemTrait.HAULER)) { NonNullList<ItemStack> l = NonNullList.withSize(2, ItemStack.EMPTY); l.set(0, getItemBySlot(EquipmentSlot.MAINHAND)); l.set(1, getItemBySlot(EquipmentSlot.OFFHAND)); return l; }
        return NonNullList.withSize(1, getItemBySlot(EquipmentSlot.MAINHAND));
    }
    public void swingArm() { swing(InteractionHand.MAIN_HAND); }
    @Override public boolean canPickUpLoot() { return false; }

    @Override public void performRangedAttack(LivingEntity target, float range) {
        if (getProperties().getArms().function != null) getProperties().getArms().function.onRangedAttack(this, target, range);
    }

    /** TC6 FlyingMoveControl (vex-like). */
    static final class FlyingMoveControl extends MoveControl<GolemEntity> {
        FlyingMoveControl(GolemEntity g) { super(g); }
        @Override public void tick() {
            if (operation != Operation.MOVE_TO) return;
            double d0 = wantedX - mob.getX(), d2 = wantedY - mob.getY(), d3 = wantedZ - mob.getZ();
            double d4 = Math.sqrt(d0 * d0 + d2 * d2 + d3 * d3);
            if (d4 < mob.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
            } else {
                mob.setDeltaMovement(mob.getDeltaMovement().add(d0 / d4 * 0.033 * speedModifier, d2 / d4 * 0.0125 * speedModifier, d3 / d4 * 0.033 * speedModifier));
                Vec3 m = mob.getDeltaMovement();
                if (mob.getTarget() == null) mob.setYRot(-(float) Mth.atan2(m.x, m.z) * 57.295776f);
                else mob.setYRot(-(float) Mth.atan2(mob.getTarget().getX() - mob.getX(), mob.getTarget().getZ() - mob.getZ()) * 57.295776f);
                mob.yBodyRot = mob.getYRot();
            }
        }
    }
}
