package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.Team;
import art.arcane.thaumcraft.golems.seals.SealButcher;

/** TC6 EntityTurretCrossbowAdvanced: faster, armoured, with selectable targets (animals, mobs, players, friendly/owned). */
public class TurretCrossbowAdvancedEntity extends TurretCrossbowEntity {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(TurretCrossbowAdvancedEntity.class, EntityDataSerializers.BYTE);
    public TurretCrossbowAdvancedEntity(EntityType<? extends TurretCrossbowAdvancedEntity> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() { return TurretCrossbowEntity.createAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 8.0); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(FLAGS, (byte) 0); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 40, 24.0f));
        goalSelector.addGoal(2, new WatchTargetGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new ValidTargetGoal(this, LivingEntity.class, (e, l) -> isGoodTarget(e)));
        setTargetMob(true);
    }
    private boolean bit(int b) { return (entityData.get(FLAGS) & (1 << b)) != 0; }
    private void bit(int b, boolean v) { byte f = entityData.get(FLAGS); entityData.set(FLAGS, (byte) (v ? f | (1 << b) : f & ~(1 << b))); setTarget(null); }
    public boolean getTargetAnimal() { return bit(0); } public void setTargetAnimal(boolean v) { bit(0, v); }
    public boolean getTargetMob() { return bit(1); } public void setTargetMob(boolean v) { bit(1, v); }
    public boolean getTargetPlayer() { return bit(2); } public void setTargetPlayer(boolean v) { bit(2, v); }
    public boolean getTargetFriendly() { return bit(3); } public void setTargetFriendly(boolean v) { bit(3, v); }

    /** canAttackClass. */
    private boolean canAttackKind(LivingEntity e) {
        if (SealButcher.isAnimalLike(e) && !(e instanceof Enemy) && getTargetAnimal()) return true;
        if (e instanceof Enemy && getTargetMob()) return true;
        if (!(e instanceof Player) || !getTargetPlayer()) return false;
        if (level() instanceof ServerLevel sl && !sl.getGameRules().get(GameRules.PVP) && !getTargetFriendly()) { setTargetPlayer(false); return false; }
        return true;
    }
    /** EntityAINearestValidTarget.isGoodTarget (advanced turret). */
    private boolean isGoodTarget(LivingEntity t) {
        if (t == this || !t.isAlive() || !canAttackKind(t)) return false;
        Team team = getTeam(), team2 = t.getTeam();
        boolean fr = getTargetFriendly();
        if (team != null && team2 == team && !fr) return false;
        if (team != null && team2 != team && fr) return false;
        if (getOwnerId() != null) {
            boolean ownable = t instanceof OwnableEntity;
            var oid = ownable && ((OwnableEntity) t).getOwnerReference() != null ? ((OwnableEntity) t).getOwnerReference().getUUID() : null;
            if (ownable && getOwnerId().equals(oid) && !fr) return false;
            if (!ownable && !(t instanceof Player) && fr) return false;
            if (ownable && !getOwnerId().equals(oid) && fr) return false;
            if (t == getOwnerEntity() && !fr) return false;
        } else if (t instanceof Player p && p.getAbilities().invulnerable && !fr) return false;
        return true;
    }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && !sl.getGameRules().get(GameRules.PVP) && getTarget() instanceof Player && getTarget() != getOwnerEntity()) setTarget(null);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putByte("targets", entityData.get(FLAGS)); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); entityData.set(FLAGS, in.getByteOr("targets", (byte) 2)); }
    @Override public void knockback(double power, double xd, double zd, DamageSource src, float dmg, boolean fx) { super.knockback(power, xd / 10.0, zd / 10.0, src, dmg, fx); }
    @Override protected ItemStack placer() { return new ItemStack(ModItems.TURRET_ADVANCED.get()); }
    @Override protected double moveScale() { return 15.0; }
    @Override protected void openGui(ServerPlayer sp) {
        sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new TurretMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        if (random.nextFloat() < 0.2f) spawnAtLocation(level, new ItemStack(ModItems.MIND_BIOTHAUMIC.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.PLANK_GREATWOOD.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.PLANK_GREATWOOD.get()), 0.5f);
        if (random.nextFloat() < 0.3f) spawnAtLocation(level, new ItemStack(ModItems.PLATE_BRASS.get()), 0.5f);
        if (random.nextFloat() < 0.4f) spawnAtLocation(level, new ItemStack(ModItems.PLATE_IRON.get()), 0.5f);
        if (random.nextFloat() < 0.4f) spawnAtLocation(level, new ItemStack(ModItems.PLATE_IRON.get()), 0.5f);
    }
}
