package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.item.CasterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class FocusEffectExchange extends FocusEffect implements IFocusBlockPicker {
    @Override public String getResearch() { return "FOCUSEXCHANGE"; }
    @Override public String getKey() { return "thaumcraft.EXCHANGE"; }
    @Override public String getAspect() { return "permutatio"; }
    // Same operator precedence as TC6: (5 + silk*4 + fortune == 0) ? 0 : (fortune+1)*3
    @Override public int getComplexity() { return (5 + getSettingValue("silk") * 4 + getSettingValue("fortune") == 0) ? 0 : (getSettingValue("fortune") + 1) * 3; }

    @Override
    public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
        if (!(target instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) return false;
        var caster = getPackage().getCaster();
        ItemStack casterStack = ItemStack.EMPTY;
        if (caster.getMainHandItem().getItem() instanceof CasterItem) casterStack = caster.getMainHandItem();
        else if (caster.getOffhandItem().getItem() instanceof CasterItem) casterStack = caster.getOffhandItem();
        if (casterStack.isEmpty()) return false;
        boolean silk = getSettingValue("silk") > 0;
        int fortune = getSettingValue("fortune");
        ItemStack picked = ((CasterItem) casterStack.getItem()).getPickedBlock(casterStack);
        if (caster instanceof Player p && !picked.isEmpty() && getPackage().world instanceof ServerLevel sl)
            ServerEvents.addSwapper(sl, bh.getBlockPos(), sl.getBlockState(bh.getBlockPos()), picked, true, 0, p, true, false, 8038177, true, silk, fortune,
                    ServerEvents.DEFAULT_PREDICATE, 0.25f + (silk ? 0.25f : 0.0f) + fortune * 0.1f);
        return true;
    }

    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(new int[] {0, 1, 2, 3, 4}, new String[] {"focus.common.no", "I", "II", "III", "IV"})),
                new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(new int[] {0, 1}, new String[] {"focus.common.no", "focus.common.yes"}))};
    }

    @Override
    public void renderParticleFX(Level world, double x, double y, double z, double vx, double vy, double vz) {
        var r = world.getRandom();
        FociFx.gen(x, y, z, vx + r.nextGaussian() * 0.01, vy + r.nextGaussian() * 0.01, vz + r.nextGaussian() * 0.01, GenericParticleOptions.builder().maxAge(9)
                .color(0.25f + r.nextFloat() * 0.25f, 0.25f + r.nextFloat() * 0.25f, 0.25f + r.nextFloat() * 0.25f).alpha(0.0f, 0.6f, 0.6f, 0.0f).grid(64)
                .particles(448, 9, 1).scale(0.5f, 0.25f).gravity((float) (r.nextGaussian() * 0.01)).randomMovement(0.0025f, 0.0025f, 0.0025f), 0);
    }

    @Override
    public void onCast(Entity caster) {
        caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2f, 2.0f + (float) (caster.level().getRandom().nextGaussian() * 0.05));
    }
}
