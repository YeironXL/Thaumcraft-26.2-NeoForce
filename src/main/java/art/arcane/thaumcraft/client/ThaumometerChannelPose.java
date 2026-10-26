package art.arcane.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Gentle bow-like tension with smooth release, mirrored for either hand. */
final class ThaumometerChannelPose implements IClientItemExtensions {
    private LocalPlayer lastPlayer;
    private final float[] tension = new float[2], lastFrame = {-1F, -1F};
    @Override
    public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.InteractionHand hand, ItemStack stack) {
        return ThaumometerArmPose.get();
    }
    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
            ItemStack stack, float partialTick, float equip, float swing) {
        if (lastPlayer != player) {
            lastPlayer = player;
            java.util.Arrays.fill(tension, 0F);
            java.util.Arrays.fill(lastFrame, -1F);
        }
        int index = arm == HumanoidArm.RIGHT ? 0 : 1;
        float now = player.tickCount + partialTick;
        if (lastFrame[index] >= 0 && (now < lastFrame[index] || now - lastFrame[index] > 3F)) tension[index] = 0F;
        float dt = lastFrame[index] < 0 ? 0F : Math.clamp(now - lastFrame[index], 0F, 2F);
        lastFrame[index] = now;
        boolean channeling = player.isUsingItem() && player.getUseItem() == stack;
        // Twelve ticks to gather tension, six to relax; independent of frame rate.
        tension[index] = Math.clamp(tension[index] + (channeling ? dt / 12F : -dt / 6F), 0F, 1F);
        if (!channeling && swing > 0F) tension[index] = 0F;
        if (!channeling && tension[index] == 0F) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float amount = tension[index] * tension[index] * (3F - 2F * tension[index]);
        float breath = (float)Math.sin(now * .14F) * .0015F * amount;
        pose.translate(side * (.56F - .02F * amount), -.52F - .6F * equip + .025F * amount + breath, -.72F + .035F * amount);
        pose.mulPose(Axis.XP.rotationDegrees(-2F * amount));
        pose.mulPose(Axis.YP.rotationDegrees(side * 3F * amount));
        return true;
    }
}
