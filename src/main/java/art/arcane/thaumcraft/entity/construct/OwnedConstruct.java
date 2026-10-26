package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityOwnedConstruct: an owned, never-despawning, water-breathing construct (golems, turrets, arcane bore).
 * Constructs that were not spawned through a placer ("validSpawn") remove themselves.
 */
public abstract class OwnedConstruct extends PathfinderMob implements OwnableEntity {
    protected static final EntityDataAccessor<Byte> TAMED = SynchedEntityData.defineId(OwnedConstruct.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER = SynchedEntityData.defineId(OwnedConstruct.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    boolean validSpawn;

    protected OwnedConstruct(EntityType<? extends OwnedConstruct> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TAMED, (byte) 0);
        b.define(OWNER, Optional.empty());
    }

    public boolean isOwned() { return (entityData.get(TAMED) & 4) != 0; }
    public void setOwned(boolean t) { byte b = entityData.get(TAMED); entityData.set(TAMED, (byte) (t ? b | 4 : b & ~4)); }
    @Override public @Nullable EntityReference<LivingEntity> getOwnerReference() { return entityData.get(OWNER).orElse(null); }
    public @Nullable UUID getOwnerId() { var r = getOwnerReference(); return r == null ? null : r.getUUID(); }
    public void setOwnerId(@Nullable UUID id) { entityData.set(OWNER, Optional.ofNullable(id).map(u -> EntityReference.<LivingEntity>of(u))); }
    public @Nullable LivingEntity getOwnerEntity() { try { return getOwner(); } catch (Exception e) { return null; } }
    public boolean isOwner(LivingEntity e) { return e != null && e == getOwnerEntity(); }

    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("clack"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.get("clack"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("tool"); }
    @Override public int getAmbientSoundInterval() { return 240; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void tick() {
        super.tick();
        if (getTarget() != null && isAlliedTo(getTarget())) setTarget(null);
        if (!level().isClientSide() && !validSpawn) discard();
    }
    public void setValidSpawn() { validSpawn = true; }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("v", validSpawn);
        out.putString("OwnerUUID", getOwnerId() == null ? "" : getOwnerId().toString());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        validSpawn = in.getBooleanOr("v", false);
        String s = in.getStringOr("OwnerUUID", "");
        if (!s.isEmpty()) {
            try { setOwnerId(UUID.fromString(s)); setOwned(true); } catch (Throwable t) { setOwned(false); }
        }
    }

    @Override public @Nullable PlayerTeam getTeam() {
        if (isOwned()) { LivingEntity o = getOwnerEntity(); if (o != null && o.getTeam() != null) return o.getTeam(); }
        return super.getTeam();
    }
    @Override protected boolean considersEntityAsAlly(Entity other) {
        if (isOwned()) {
            LivingEntity o = getOwnerEntity();
            if (other == o) return true;
            if (o != null && o.isAlliedTo(other)) return true;
        }
        return super.considersEntityAsAlly(other);
    }

    @Override public void die(DamageSource cause) {
        if (level() instanceof ServerLevel sl && sl.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES) && hasCustomName() && getOwnerEntity() instanceof ServerPlayer sp)
            sp.sendSystemMessage(getCombatTracker().getDeathMessage());
        super.die(cause);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isRemoved()) return InteractionResult.PASS;
        if (player.isShiftKeyDown() || player.getMainHandItem().getItem() instanceof NameTagItem) return InteractionResult.PASS;
        if (!level().isClientSide() && !isOwner(player)) {
            player.sendOverlayMessage(Component.translatable("tc.notowned").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.ITALIC));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}
