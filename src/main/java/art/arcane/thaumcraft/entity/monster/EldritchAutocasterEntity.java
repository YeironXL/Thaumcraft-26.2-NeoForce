package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * TC5 EntityTurretEldritch ("Ancient Autocaster"): a stationary hostile caster found in the Outer Lands halls, set on an
 * Ancient Doorway block and armed with one offensive focus. The port has no TC4/TC5 foci, so the "focus" is one of two
 * attacks: an eldritch orb (as the Eldritch Guardian throws) or an arcing shock (as wisps zap).
 */
public class EldritchAutocasterEntity extends Monster implements Enemy {
    public static final int FOCUS_ORB = 0, FOCUS_SHOCK = 1;
    private int focus = -1, cooldown = 20;
    public int shots;

    public EldritchAutocasterEntity(EntityType<? extends EldritchAutocasterEntity> type, Level level) { super(type, level); setPersistenceRequired(); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 0.0);
    }

    public int focus() { if (focus < 0) focus = random.nextInt(2); return focus; }
    public void setFocus(int f) { focus = f; }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new CastGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) {}
    @Override protected double getDefaultGravity() { return 0.0; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public void travel(Vec3 v) { setDeltaMovement(Vec3.ZERO); }

    /** Casts at the target: faces it, then every 30-50 ticks fires its focus while it has line of sight. */
    private final class CastGoal extends Goal {
        CastGoal() { setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30f, 30f);
            if (--cooldown > 0 || !hasLineOfSight(t) || distanceToSqr(t) > 20 * 20) return;
            cooldown = 30 + random.nextInt(21);
            cast(t);
        }
    }

    public void cast(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        shots++;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0)); yHeadRot = getYRot();
        setXRot((float) -Math.toDegrees(Math.atan2(t.getEyeY() - getEyeY(), Math.sqrt(dx * dx + dz * dz))));
        if (focus() == FOCUS_ORB) {
            EldritchOrbEntity orb = new EldritchOrbEntity(sl, this);
            Vec3 d = new Vec3(t.getX() - getX(), t.getY(0.5) - getEyeY(), t.getZ() - getZ()).normalize();
            orb.setDeltaMovement(d.scale(0.75));
            sl.addFreshEntity(orb);
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.6f);
        } else {
            Fx.send(sl, "zap", getX(), getEyeY(), getZ(), (float) t.getX(), (float) t.getY(0.5), (float) t.getZ(), 0x8844FF, 1.0f);
            playSound(ModSounds.get("zap"), 1.0f, 0.9f);
            t.hurtServer(sl, damageSources().indirectMagic(this, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f);
        }
    }

    /** TC5: the focus may drop on destruction, with void metal and other loot. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recent) {
        super.dropCustomDeathLoot(level, src, recent);
        if (random.nextInt(4) == 0) spawnAtLocation(level, new ItemStack(ModItems.FOCUS_1.get()));
        if (random.nextInt(3) == 0) spawnAtLocation(level, new ItemStack(ModItems.INGOT_VOID.get(), 1 + random.nextInt(2)));
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("Focus", focus()); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); focus = in.getIntOr("Focus", -1); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
}
