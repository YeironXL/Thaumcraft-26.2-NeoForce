package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityTurretCrossbow: a stationary crossbow that shoots its arrows at monsters, reloading from an upward dispenser below. */
public class TurretCrossbowEntity extends OwnedConstruct implements RangedAttackMob {
    int loadProgressInt;
    boolean isLoadInProgress;
    float loadProgress, prevLoadProgress;
    public float loadProgressForRender;

    public TurretCrossbowEntity(EntityType<? extends TurretCrossbowEntity> type, Level level) { super(type, level); setDropChance(EquipmentSlot.MAINHAND, 0.0f); }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 0.0).add(Attributes.ARMOR, 2.0);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 60, 24.0f));
        goalSelector.addGoal(2, new WatchTargetGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new ValidTargetGoal(this, Mob.class, (e, l) -> e instanceof Enemy));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float range) {
        ItemStack ammo = getMainHandItem();
        if (ammo.isEmpty()) return;
        Arrow arrow = new Arrow(level(), this, ammo.copyWithCount(1), null);
        arrow.setBaseDamage(2.25 + range * 2.0f + random.nextGaussian() * 0.25);
        Vec3 look = getViewVector(1.0f);
        if (!isPassenger()) arrow.setPos(arrow.getX() - look.x * 0.9, arrow.getY() - look.y * 0.9, arrow.getZ() - look.z * 0.9);
        else arrow.setPos(arrow.getX() + look.x * 1.75, arrow.getY() + look.y * 1.75, arrow.getZ() + look.z * 1.75);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double d0 = target.getX() - getX(), d2 = target.getBoundingBox().minY + target.getEyeHeight() + range * range * 3.0f - arrow.getY(), d3 = target.getZ() - getZ();
        arrow.shoot(d0, d2, d3, 2.0f, 2.0f);
        level().addFreshEntity(arrow);
        level().broadcastEntityEvent(this, (byte) 16);
        playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        ammo.shrink(1);
        if (ammo.isEmpty()) setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 16) { if (!swinging) { swingTime = -1; swinging = true; } }
        else if (id == 17) { if (!isLoadInProgress) { loadProgressInt = -1; isLoadInProgress = true; } }
        else super.handleEntityEvent(id);
    }
    public float getLoadProgress(float pt) { float f1 = loadProgress - prevLoadProgress; if (f1 < 0.0f) ++f1; return prevLoadProgress + f1 * pt; }
    @Override protected void updateSwingTime() {
        if (swinging) { if (++swingTime >= 6) { swingTime = 0; swinging = false; } } else swingTime = 0;
        attackAnim = swingTime / 6.0f;
        if (isLoadInProgress) { if (++loadProgressInt >= 10) { loadProgressInt = 0; isLoadInProgress = false; } } else loadProgressInt = 0;
        loadProgress = loadProgressInt / 10.0f;
    }

    @Override
    public void baseTick() {
        prevLoadProgress = loadProgress;
        if (!level().isClientSide() && getMainHandItem().isEmpty()) {
            BlockPos p = blockPosition().below();
            if (level().getBlockEntity(p) instanceof DispenserBlockEntity d && level().getBlockState(p).getValue(DispenserBlock.FACING) == Direction.UP) {
                for (int a = 0; a < d.getContainerSize(); ++a) {
                    if (d.getItem(a).getItem() instanceof ArrowItem) {
                        setItemInHand(InteractionHand.MAIN_HAND, d.removeItem(a, d.getItem(a).getCount()));
                        playSound(ModSounds.get("ticks"), 1.0f, 1.0f);
                        level().broadcastEntityEvent(this, (byte) 17);
                        break;
                    }
                }
            }
        }
        super.baseTick();
    }


    @Override
    public void tick() {
        super.tick();
        if (getTarget() != null && (!getTarget().isAlive() || isAlliedTo(getTarget()))) setTarget(null);
        if (!level().isClientSide()) {
            setYRot(yHeadRot);
            if (tickCount % 80 == 0) heal(1.0f);
            BlockPos bp = blockPosition();
            if (BaseRailBlock.isRail(level(), bp.below())) bp = bp.below();
            var st = level().getBlockState(bp);
            if (st.is(Blocks.ACTIVATOR_RAIL)) setNoAi(st.getValue(PoweredRailBlock.POWERED));
        } else updateSwingTime();
    }
    @Override public boolean isPushable() { return true; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        setYRot(getYRot() + (float) (getRandom().nextGaussian() * 45.0));
        setXRot(getXRot() + (float) (getRandom().nextGaussian() * 20.0));
        return super.hurtServer(level, source, amount);
    }
    @Override public void knockback(double power, double xd, double zd, DamageSource src, float dmg, boolean fx) {
        super.knockback(power, xd / 10.0, zd / 10.0, src, dmg, fx);
        if (getDeltaMovement().y > 0.1) setDeltaMovement(getDeltaMovement().x, 0.1, getDeltaMovement().z);
    }
    protected ItemStack placer() { return new ItemStack(ModItems.TURRET_BASIC.get()); }
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && isOwner(player) && !isRemoved()) {
            if (player.isShiftKeyDown()) {
                playSound(ModSounds.get("zap"), 1.0f, 1.0f);
                dropAmmo();
                spawnAtLocation((ServerLevel) level(), placer(), 0.5f);
                discard();
                player.swing(hand);
            } else if (player instanceof ServerPlayer sp) openGui(sp);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    protected void openGui(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new TurretMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
    }
    protected double moveScale() { return 20.0; }
    @Override public void move(MoverType mt, Vec3 d) { super.move(mt, new Vec3(d.x / moveScale(), d.y, d.z / moveScale())); }
    @Override public void die(DamageSource cause) { super.die(cause); if (!level().isClientSide()) dropAmmo(); }
    protected void dropAmmo() { if (!getMainHandItem().isEmpty()) { spawnAtLocation((ServerLevel) level(), getMainHandItem().copy(), 0.5f); setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); } }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        float b = 0;
        if (random.nextFloat() < 0.2f + b) spawnAtLocation(level, new ItemStack(ModItems.MIND_CLOCKWORK.get()), 0.5f);
        if (random.nextFloat() < 0.5f + b) spawnAtLocation(level, new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5f);
        if (random.nextFloat() < 0.5f + b) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.PLANK_GREATWOOD.get()), 0.5f);
        if (random.nextFloat() < 0.5f + b) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.PLANK_GREATWOOD.get()), 0.5f);
    }
    @Override public int getMaxHeadXRot() { return 20; }

    /** EntityAIWatchTarget: track the current target for 2-4 seconds. */
    protected static class WatchTargetGoal extends Goal {
        final Mob watcher; Entity closest; int lookTime;
        WatchTargetGoal(Mob m) { watcher = m; setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() { if (watcher.getTarget() != null) closest = watcher.getTarget(); return closest != null; }
        @Override public boolean canContinueToUse() { double d = watcher.getAttributeValue(Attributes.FOLLOW_RANGE); return closest.isAlive() && watcher.distanceToSqr(closest) <= d * d && lookTime > 0; }
        @Override public void start() { lookTime = 40 + watcher.getRandom().nextInt(40); }
        @Override public void stop() { closest = null; }
        @Override public void tick() { watcher.getLookControl().setLookAt(closest.getX(), closest.getY() + closest.getEyeHeight(), closest.getZ(), 10.0f, watcher.getMaxHeadXRot()); --lookTime; }
    }
    /** EntityAINearestValidTarget: nearest target within follow range (vertically 4), sneaking/invisible players harder to spot, inside the home area. */
    protected static class ValidTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ValidTargetGoal(Mob owner, Class<? extends LivingEntity> cls, TargetingConditions.Selector sel) {
            super(owner, (Class) cls, 5, true, false, (e, l) -> (sel == null || sel.test(e, l)) && owner.isWithinHome(e.blockPosition()) && visibleEnough(owner, e));
        }
        static boolean visibleEnough(Mob owner, LivingEntity e) {
            if (!(e instanceof Player pl)) return true;
            double d0 = owner.getAttributeValue(Attributes.FOLLOW_RANGE);
            if (pl.isShiftKeyDown()) d0 *= 0.8;
            if (pl.isInvisible()) { float f = Math.max(0.1f, pl.getArmorCoverPercentage()); d0 *= 0.7f * f; }
            return pl.distanceTo(owner) <= d0;
        }
        @Override protected AABB getTargetSearchArea(double d) { return mob.getBoundingBox().inflate(d, 4.0, d); }
    }
}
