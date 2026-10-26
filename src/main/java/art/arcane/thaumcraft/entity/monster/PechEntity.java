package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusMediumRoot;
import art.arcane.thaumcraft.casters.FocusPackage;
import art.arcane.thaumcraft.casters.foci.FocusEffectAir;
import art.arcane.thaumcraft.casters.foci.FocusEffectCurse;
import art.arcane.thaumcraft.casters.foci.FocusEffectEarth;
import art.arcane.thaumcraft.casters.foci.FocusEffectFire;
import art.arcane.thaumcraft.casters.foci.FocusEffectFlux;
import art.arcane.thaumcraft.casters.foci.FocusMediumProjectile;
import art.arcane.thaumcraft.menu.PechMenu;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TC6 EntityPech: a goblin-like forager (type 0), mage (1, wields a pech wand that casts random foci) or stalker (2, archer).
 * Pech hoard items they pick up; tossing them something they value (ender pearls, desire-aspected items) may tame them,
 * after which right-clicking opens the trade GUI. Hurting one angers every pech within 32 blocks.
 */
public class PechEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.BOOLEAN);

    public NonNullList<ItemStack> loot = NonNullList.withSize(9, ItemStack.EMPTY);
    public boolean trading;
    private final RangedAttackGoal aiArrowAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0f);
    private final RangedAttackGoal aiBlastAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0f);
    private final MeleeAttackGoal aiMeleeAttack = new MeleeAttackGoal(this, 0.6, false);
    private final AvoidEntityGoal<Player> aiAvoidPlayer = new AvoidEntityGoal<>(this, Player.class, 8.0f, 0.5, 0.6);
    public float mumble;
    int chargecount;

    public PechEntity(EntityType<? extends PechEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.ARMOR, 2.0);
    }

    @Override
    public Component getTypeName() {
        return switch (getPechType()) {
            case 1 -> Component.translatable("entity.thaumcraft.pech.1");
            case 2 -> Component.translatable("entity.thaumcraft.pech.2");
            default -> super.getTypeName();
        };
    }

    @Override
    protected void registerGoals() {
        if (getNavigation() instanceof GroundPathNavigation g) g.setCanFloat(false);
        setDropChance(EquipmentSlot.MAINHAND, 0.2f);
        setDropChance(EquipmentSlot.OFFHAND, 0.2f);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradePlayerGoal());
        goalSelector.addGoal(3, new ItemEntityGotoGoal());
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0f, 1.0f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canUse() { return getAnger() != 0 && super.canUse(); }
        });
    }

    /** setCombatTask: bow -> arrows, pech wand -> focus blasts, anything else -> melee; untamed pech flee players. */
    public void setCombatTask() {
        if (level() == null || level().isClientSide()) return;
        goalSelector.removeGoal(aiMeleeAttack);
        goalSelector.removeGoal(aiArrowAttack);
        goalSelector.removeGoal(aiBlastAttack);
        ItemStack held = getMainHandItem();
        if (held.is(Items.BOW)) goalSelector.addGoal(2, aiArrowAttack);
        else if (held.is(ModItems.PECH_WAND.get())) goalSelector.addGoal(2, aiBlastAttack);
        else goalSelector.addGoal(2, aiMeleeAttack);
        if (isTamed()) goalSelector.removeGoal(aiAvoidPlayer);
        else goalSelector.addGoal(4, aiAvoidPlayer);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (getPechType() == 2) {
            ItemStack bow = getMainHandItem();
            ItemStack ammo = new ItemStack(Items.ARROW);
            if (random.nextFloat() < 0.2f) {
                ammo = new ItemStack(Items.TIPPED_ARROW);
                ammo.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(), java.util.Optional.empty(),
                        List.of(new MobEffectInstance(MobEffects.POISON, 40)), java.util.Optional.empty()));
            }
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, f, bow);
            double d0 = target.getX() - getX(), d2 = target.getY(0.3333333333333333) - arrow.getY(), d3 = target.getZ() - getZ();
            double d4 = Math.sqrt(d0 * d0 + d3 * d3);
            arrow.shoot(d0, d2 + d4 * 0.20000000298023224, d3, 1.6f, 14 - sl.getDifficulty().getId() * 4);
            playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (random.nextFloat() * 0.4f + 0.8f));
            sl.addFreshEntity(arrow);
        } else if (getPechType() == 1) {
            FocusPackage p = new FocusPackage(this);
            FocusMediumRoot root = new FocusMediumRoot();
            root.setupFromCasterToTarget(this, target, distanceTo(target) / 6.0f);
            p.addNode(root);
            FocusMediumProjectile fp = new FocusMediumProjectile();
            fp.initialize();
            fp.getSetting("speed").setValue(2);
            p.addNode(fp);
            p.addNode(random.nextBoolean() ? new FocusEffectCurse() : random.nextBoolean() ? new FocusEffectFlux()
                    : random.nextBoolean() ? new FocusEffectEarth() : random.nextBoolean() ? new FocusEffectAir() : new FocusEffectFire());
            FocusEngine.castFocusPackage(this, p, true);
            swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!level().isClientSide() && slot == EquipmentSlot.MAINHAND) setCombatTask();
    }

    /** setEquipmentBasedOnDifficulty: one roll of twenty picks the pech's tool. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        ItemStack s = switch (random.nextInt(20)) {
            case 0, 12 -> new ItemStack(ModItems.PECH_WAND.get());
            case 1 -> new ItemStack(Items.STONE_SWORD);
            case 3 -> new ItemStack(Items.STONE_AXE);
            case 5 -> new ItemStack(Items.IRON_SWORD);
            case 6 -> new ItemStack(Items.IRON_AXE);
            case 7 -> new ItemStack(Items.FISHING_ROD);
            case 8 -> new ItemStack(Items.STONE_PICKAXE);
            case 9 -> new ItemStack(Items.IRON_PICKAXE);
            case 2, 4, 10, 11, 13 -> new ItemStack(Items.BOW);
            default -> ItemStack.EMPTY;
        };
        if (!s.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, s);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, SpawnGroupData data) {
        populateDefaultEquipmentSlots(random, diff);
        ItemStack held = getMainHandItem();
        if (held.is(ModItems.PECH_WAND.get())) {
            setPechType(1);
            setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        } else if (!held.isEmpty()) {
            if (held.is(Items.BOW)) setPechType(2);
            populateDefaultEquipmentEnchantments(level, random, diff);
        }
        setCanPickUpLoot(random.nextFloat() < 0.75f * diff.getSpecialMultiplier());
        setCombatTask();
        return super.finalizeSpawn(level, diff, reason, data);
    }

    /** The vanilla pick-up-and-equip behaviour is replaced by the pech's own hoarding goal. */
    @Override public boolean wantsToPickUp(ServerLevel level, ItemStack stack) { return false; }

    /** getCanSpawnHere: fewer than 4 pech within 16 blocks (the magical-biome check is done by the biome spawn list). */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
        int count = level.getEntitiesOfClass(PechEntity.class, getBoundingBox().inflate(16.0)).size();
        return count < 4 && super.checkSpawnRules(level, reason);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TYPE, (byte) 0);
        b.define(ANGER, 0);
        b.define(TAMED, false);
    }
    public int getPechType() { return entityData.get(TYPE); }
    public int getAnger() { return entityData.get(ANGER); }
    public boolean isTamed() { return entityData.get(TAMED); }
    public void setPechType(int t) { entityData.set(TYPE, (byte) t); }
    public void setAnger(int a) { entityData.set(ANGER, a); }
    public void setTamed(boolean t) { entityData.set(TAMED, t); }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putByte("PechType", (byte) getPechType());
        out.putShort("Anger", (short) getAnger());
        out.putBoolean("Tamed", isTamed());
        ContainerHelper.saveAllItems(out, loot);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setPechType(in.getByteOr("PechType", (byte) 0));
        setAnger(in.getShortOr("Anger", (short) 0));
        setTamed(in.getBooleanOr("Tamed", false));
        loot = NonNullList.withSize(9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, loot);
        setCombatTask();
    }

    /** canDespawn: only while carrying fewer than five stacks. */
    @Override
    public boolean removeWhenFarAway(double d) {
        int q = 0;
        for (ItemStack is : loot) if (!is.isEmpty()) ++q;
        return q < 5;
    }

    @Override public boolean canBeLeashed() { return false; }

    /** dropLoot: each hoarded stack has a 33% chance to fall out, then the pech loot table. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        for (ItemStack s : loot) if (!s.isEmpty() && level.getRandom().nextFloat() < 0.33f) spawnAtLocation(level, s.copy(), 1.5f);
        super.dropCustomDeathLoot(level, source, killedByPlayer);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) mumble = (float) Math.PI;
        else if (id == 17) mumble = (float) (Math.PI * 2);
        else if (id == 18) particles(ParticleTypes.HAPPY_VILLAGER, 5);
        if (id == 19) { particles(ParticleTypes.ANGRY_VILLAGER, 5); mumble = (float) (Math.PI * 2); }
        else if (id != 16 && id != 17 && id != 18) super.handleEntityEvent(id);
    }

    private void particles(net.minecraft.core.particles.ParticleOptions type, int n) {
        for (int i = 0; i < n; ++i)
            level().addParticle(type, getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(), getY() + 0.5 + random.nextFloat() * getBbHeight(),
                    getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(), random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
    }

    /** playLivingSound: a third of the time, near another pech, they trade mumbles. */
    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel sl) {
            if (random.nextInt(3) == 0) {
                for (Entity e : sl.getEntities(this, getBoundingBox().inflate(4.0, 2.0, 4.0))) {
                    if (e instanceof PechEntity) {
                        sl.broadcastEntityEvent(this, (byte) 17);
                        playSound(ModSounds.get("pech_trade"), getSoundVolume(), getVoicePitch());
                        return;
                    }
                }
            }
            sl.broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }

    @Override public int getAmbientSoundInterval() { return 120; }
    @Override protected float getSoundVolume() { return 0.4f; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("pech_idle"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.get("pech_hit"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("pech_death"); }

    private void becomeAngryAt(Entity entity) {
        if (entity instanceof Player p && p.isCreative()) return;
        if (getAnger() <= 0) {
            level().broadcastEntityEvent(this, (byte) 19);
            playSound(ModSounds.get("pech_charge"), getSoundVolume(), getVoicePitch());
        }
        if (entity instanceof LivingEntity le) setTarget(le);
        setAnger(400 + random.nextInt(400));
        setTamed(false);
        setCombatTask();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableTo(level, source)) return false;
        if (source.getEntity() instanceof Player player) {
            for (Entity e : level.getEntities(this, getBoundingBox().inflate(32.0, 16.0, 32.0))) if (e instanceof PechEntity pech) pech.becomeAngryAt(player);
            becomeAngryAt(player);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        if (mumble > 0.0f) mumble *= 0.75f;
        if (!level().isClientSide()) {
            if (getAnger() > 0) setAnger(getAnger() - 1);
            if (getAnger() > 0 && getTarget() != null) {
                if (chargecount > 0) --chargecount;
                if (chargecount == 0) {
                    chargecount = 100;
                    playSound(ModSounds.get("pech_charge"), getSoundVolume(), getVoicePitch());
                }
                level().broadcastEntityEvent(this, (byte) 17);
            }
        } else {
            if (random.nextInt(15) == 0 && getAnger() > 0) particles(ParticleTypes.ANGRY_VILLAGER, 1);
            if (random.nextInt(25) == 0 && isTamed()) particles(ParticleTypes.HAPPY_VILLAGER, 1);
        }
        super.tick();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 40 == 0) heal(1.0f);
    }

    public boolean canPickup(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!isTamed() && isValuedItem(stack)) return true;
        for (int a = 0; a < loot.size(); ++a) {
            ItemStack l = loot.get(a);
            if (l.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(stack, l) && stack.getCount() + l.getCount() <= l.getMaxStackSize()) return true;
        }
        return false;
    }

    /** pickupItem: hoards non-valued items (or everything once tamed); a valued gift may tame it and is eaten (one item). */
    public ItemStack pickupItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (isTamed() || !isValued(stack)) {
            for (int a = 0; a < loot.size(); ++a) {
                ItemStack l = loot.get(a);
                if (!stack.isEmpty() && !l.isEmpty() && l.getCount() < l.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack, l)) {
                    if (stack.getCount() + l.getCount() <= l.getMaxStackSize()) { l.grow(stack.getCount()); return ItemStack.EMPTY; }
                    int sz = Math.min(stack.getCount(), l.getMaxStackSize() - l.getCount());
                    l.grow(sz);
                    stack.shrink(sz);
                }
            }
            for (int a = 0; a < loot.size(); ++a) {
                if (!stack.isEmpty() && loot.get(a).isEmpty()) { loot.set(a, stack.copy()); return ItemStack.EMPTY; }
            }
            return stack;
        }
        if (random.nextInt(10) < getValue(stack)) {
            setTamed(true);
            setCombatTask();
            level().broadcastEntityEvent(this, (byte) 18);
        }
        stack.shrink(1);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).getItem() instanceof NameTagItem) return InteractionResult.PASS;
        if (isTamed()) {
            if (player instanceof ServerPlayer sp)
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new PechMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** valuedItems (ender pearl: 15), or anything with more than one desire. */
    public static boolean isValuedItem(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        return item.is(Items.ENDER_PEARL) || ObjectAspects.get(item).getOrDefault(AspectRegistry.DESIDERIUM, 0) > 1;
    }
    public boolean isValued(ItemStack item) { return isValuedItem(item); }

    public static int getValueOf(ItemStack item) {
        if (item == null || item.isEmpty()) return 0;
        if (item.is(Items.ENDER_PEARL)) return 15;
        return Math.min(32, ObjectAspects.get(item).getOrDefault(AspectRegistry.DESIDERIUM, 0) / 2);
    }
    public int getValue(ItemStack item) { return getValueOf(item); }

    /** AIPechTradePlayer: stand still while a player has the trade GUI open. */
    final class TradePlayerGoal extends Goal {
        TradePlayerGoal() { setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP)); }
        @Override public boolean canUse() { return isAlive() && !isInWater() && isTamed() && onGround() && !hurtMarked && trading; }
        @Override public void start() { getNavigation().stop(); }
        @Override public void stop() { trading = false; }
    }

    /** AIPechItemEntityGoto: walk to the nearest item within 16 blocks it wants (ignoring items it dropped from trades) and take it. */
    final class ItemEntityGotoGoal extends Goal {
        private Entity targetEntity;
        private int count, failedPathFindingPenalty;
        ItemEntityGotoGoal() { setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (--count > 0) return false;
            double range = Double.MAX_VALUE;
            targetEntity = null;
            for (Entity e : level().getEntities(PechEntity.this, getBoundingBox().inflate(16.0))) {
                if (e instanceof ItemEntity ie && canPickup(ie.getItem()) && !PechMenu.PECH_DROP.equals(ie.getPersistentData().getStringOr("thaumcraft:thrower", ""))) {
                    double d = e.distanceToSqr(PechEntity.this);
                    if (d < range && d <= 256.0) { range = d; targetEntity = e; }
                }
            }
            return targetEntity != null;
        }
        @Override public boolean canContinueToUse() { return targetEntity != null && targetEntity.isAlive() && !getNavigation().isDone() && targetEntity.distanceToSqr(PechEntity.this) < 256.0; }
        @Override public void stop() { targetEntity = null; }
        @Override public void start() { getNavigation().moveTo(targetEntity, getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5); count = 0; }

        @Override
        public void tick() {
            getLookControl().setLookAt(targetEntity, 30.0f, 30.0f);
            if (getSensing().hasLineOfSight(targetEntity) && --count <= 0) {
                count = failedPathFindingPenalty + 4 + random.nextInt(4);
                getNavigation().moveTo(targetEntity, getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5);
                var path = getNavigation().getPath();
                if (path != null && path.getEndNode() != null && targetEntity.distanceToSqr(path.getEndNode().x, path.getEndNode().y, path.getEndNode().z) < 1.0) failedPathFindingPenalty = 0;
                else failedPathFindingPenalty += 10;
            }
            double distance = distanceToSqr(targetEntity.getX(), targetEntity.getBoundingBox().minY, targetEntity.getZ());
            if (distance <= 1.5 && targetEntity instanceof ItemEntity ie) {
                count = 0;
                int am = ie.getItem().getCount();
                ItemStack is = pickupItem(ie.getItem().copy());
                if (!is.isEmpty()) ie.setItem(is);
                else ie.discard();
                if (is.isEmpty() || is.getCount() != am)
                    level().playSound(null, ie.getX(), ie.getY(), ie.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2f, ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            }
        }
    }
}
