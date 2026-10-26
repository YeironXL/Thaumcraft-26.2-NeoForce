package art.arcane.thaumcraft.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/** Third-person pose for the thaumometer: both arms forward, hands meeting in front of the chest around the device. */
public final class ThaumometerArmPose {
    public static final float X_ROT = -0.95f, Y_ROT = 0.42f;
    private static final IArmPoseTransformer TRANSFORM = (model, state, arm) -> {
        float look = model.head.xRot * 0.35f;
        model.rightArm.xRot = X_ROT + look;
        model.leftArm.xRot = X_ROT + look;
        model.rightArm.yRot = -Y_ROT;
        model.leftArm.yRot = Y_ROT;
        // HumanoidModel bobs both arms in opposite directions after the pose; cancel it so the device stays in both hands.
        float a = state.ageInTicks;
        float bobZ = net.minecraft.util.Mth.cos(a * 0.09f) * 0.05f + 0.05f, bobX = net.minecraft.util.Mth.sin(a * 0.067f) * 0.05f;
        model.rightArm.zRot = -bobZ;
        model.leftArm.zRot = bobZ;
        model.rightArm.xRot -= bobX;
        model.leftArm.xRot += bobX;
    };
    /** Registered through META-INF/enumextensions.json: two-handed and overrides the off-hand pose. */
    public static final EnumProxy<HumanoidModel.ArmPose> PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, true, true, TRANSFORM);

    public static HumanoidModel.ArmPose get() { return PROXY.getValue(); }
    private ThaumometerArmPose() {}
}
