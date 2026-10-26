package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.node.AuraNodeGen;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC4 Primal Focus (ItemFocusPrimal) as a TC6 focus effect: every impact is a small chaotic blast that rolls one of
 * the six primal effects on everything around it, and rarely seeds fibrous taint (1 in 25) or condenses a small
 * unstable aura node (1 in 100). Decision and odds are documented in docs/FOCOS_ESTRUCTURAS_FUENTES.md.
 */
public final class FocusEffectPrimal extends FocusEffect {
    public static final String[] PRIMALS = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    public static final int TAINT_CHANCE = 25, NODE_CHANCE = 100;

    @Override public String getResearch() { return "FOCUSPRIMAL"; }
    @Override public String getKey() { return "thaumcraft.PRIMAL"; }
    @Override public String getAspect() { return "praecantatio"; }
    @Override public int getComplexity() { return 6 + getSettingValue("power") * 3; }
    @Override public float getDamageForDisplay(float finalPower) { return (2 + getSettingValue("power")) * finalPower; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        Level world = getPackage().world;
        var hv = target.getLocation();
        FocusFx.impact(world, hv.x, hv.y, hv.z, getKey());
        if (!(world instanceof ServerLevel sl)) return false;
        var r = sl.getRandom();
        BlockPos at = target instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK ? bh.getBlockPos().relative(bh.getDirection()) : BlockPos.containing(hv);
        blast(sl, hv, at, getPackage().getCaster(), getDamageForDisplay(finalPower), r.nextInt(PRIMALS.length), r.nextInt(TAINT_CHANCE) == 0, r.nextInt(NODE_CHANCE) == 0, r);
        return true;
    }

    /** One primal blast. {@code primal} indexes {@link #PRIMALS}; the taint/node rolls are passed in so tests can force them. */
    public static void blast(ServerLevel sl, Vec3 hv, BlockPos at, LivingEntity caster, float dmg, int primal, boolean taint, boolean node, RandomSource r) {
        sl.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f + r.nextFloat() * 0.3f);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(hv, hv).inflate(2.5), e -> e != caster && e.isAlive())) {
            e.hurtServer(sl, sl.damageSources().indirectMagic(e, caster), dmg);
            switch (PRIMALS[primal]) {
                case "aer" -> { Vec3 d = e.position().subtract(hv).normalize().scale(1.2); e.push(d.x, 0.5, d.z); e.hurtMarked = true; }
                case "terra" -> e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1));
                case "ignis" -> e.igniteForSeconds(4);
                case "aqua" -> e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                case "ordo" -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                default -> e.hurtServer(sl, sl.damageSources().magic(), dmg * 0.5f);
            }
        }
        if (PRIMALS[primal].equals("perditio") || PRIMALS[primal].equals("ignis"))
            sl.explode(caster, hv.x, hv.y, hv.z, 1.0f, PRIMALS[primal].equals("ignis"), Level.ExplosionInteraction.NONE);
        if (taint) {
            BlockPos p = at;
            if (sl.isEmptyBlock(p) && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP))
                sl.setBlockAndUpdate(p, ModBlocks.TAINT_FIBRE.get().defaultBlockState());
        }
        if (node) {
            BlockPos p = at.above();
            if (sl.isEmptyBlock(p)) AuraNodeGen.place(sl, p, NodeType.UNSTABLE, null, r, true);
        }
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.3f + (float) (caster.level().getRandom().nextGaussian() * 0.1));
    }

    private static final float[][] COLORS = {{1f, 1f, 0.5f}, {0.35f, 0.75f, 0f}, {1f, 0.35f, 0f}, {0.2f, 0.8f, 1f}, {0.85f, 0.85f, 0.95f}, {0.3f, 0.3f, 0.3f}};

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        var r = world.getRandom();
        float[] c = COLORS[r.nextInt(COLORS.length)];
        FociFx.gen(x, y, z, vx + r.nextGaussian() * 0.02, vy + r.nextGaussian() * 0.02, vz + r.nextGaussian() * 0.02, GenericParticleOptions.builder()
                .maxAge(12 + r.nextInt(8)).color(c[0], c[1], c[2]).alpha(0.0f, 1.0f, 1.0f, 0.0f).grid(64).particles(0, 1, 1)
                .scale(0.8f + r.nextFloat() * 0.5f, 0.1f).slowDown(0.9).randomMovement(0.02f, 0.02f, 0.02f), 0);
    }
}
