package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.block.world.taint.ITaintBlock;
import art.arcane.thaumcraft.block.world.taint.TaintFibreBlock;
import art.arcane.thaumcraft.block.world.taint.TaintHelper;
import art.arcane.thaumcraft.entity.ITaintedMob;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 EntityTaintCrawler: a small tainted silverfish that weaves taint fibres where it walks and inflicts flux taint. */
public class TaintCrawlerEntity extends Monster implements ITaintedMob {
    BlockPos lastPos = BlockPos.ZERO;

    public TaintCrawlerEntity(EntityType<? extends TaintCrawlerEntity> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.275).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof ITaintedMob) && super.canAttack(target); }
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof ITaintedMob || super.considersEntityAsAlly(other); }

    @Override public float getVoicePitch() { return 0.7f; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
    /** canTriggerWalking() == false: no step sounds or walk distance. */
    @Override protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.NONE; }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel sl && isAlive() && tickCount % 40 == 0 && !lastPos.equals(blockPosition())) {
            lastPos = blockPosition();
            BlockState bs = sl.getBlockState(lastPos);
            if (!bs.is(BlockTags.LEAVES) && bs.getFluidState().isEmpty() && !ITaintBlock.isTaint(bs)
                    && (bs.isAir() || bs.canBeReplaced() || bs.getBlock() instanceof FlowerBlock || bs.getBlock() instanceof BushBlock)
                    && TaintHelper.isAdjacentToSolidBlock(sl, lastPos) && !TaintFibreBlock.isOnlyAdjacentToTaint(sl, lastPos))
                sl.setBlockAndUpdate(lastPos, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
        }
        super.tick();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        if (level.getRandom().nextInt(8) == 0) spawnAtLocation(level, CrystalEssenceItem.of(AspectRegistry.VITIUM.id(), 1), getBbHeight() / 2.0f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity victim) {
        if (!super.doHurtTarget(level, victim)) return false;
        if (victim instanceof LivingEntity le) {
            int b0 = level.getDifficulty() == Difficulty.NORMAL ? 3 : level.getDifficulty() == Difficulty.HARD ? 6 : 0;
            if (b0 > 0 && random.nextInt(b0 + 1) > 2) le.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, b0 * 20, 0));
        }
        return true;
    }
}
