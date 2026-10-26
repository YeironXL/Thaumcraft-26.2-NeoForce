package art.arcane.thaumcraft.client.render.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** TC6 ModelFireBat animation (flying pose; hanging pose for resting fire bats). */
public final class FireBatModel extends EntityModel<FireBatModel.State> {
    public static class State extends LivingEntityRenderState { public boolean hanging; public int color = 0xFFFFFF; }
    private final ModelPart head, body, rightWing, leftWing, rightTip, leftTip;

    public FireBatModel(ModelPart root) {
        super(root);
        head = root.getChild("head"); body = root.getChild("body");
        rightWing = body.getChild("right_wing"); leftWing = body.getChild("left_wing");
        rightTip = rightWing.getChild("right_wing_tip"); leftTip = leftWing.getChild("left_wing_tip");
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float par4 = s.ageInTicks, par5 = s.yRot, par6 = s.xRot;
        if (s.hanging) {
            head.xRot = par6 / 57.295776f; head.yRot = (float) Math.PI - par5 / 57.295776f; head.zRot = (float) Math.PI;
            head.setPos(0, -2, 0); rightWing.setPos(-3, 0, 3); leftWing.setPos(3, 0, 3);
            body.xRot = (float) Math.PI;
            rightWing.xRot = -0.15707964f; rightWing.yRot = -1.2566371f; rightTip.yRot = -1.7278761f;
            leftWing.xRot = rightWing.xRot; leftWing.yRot = -rightWing.yRot; leftTip.yRot = -rightTip.yRot;
        } else {
            head.xRot = par6 / 57.295776f; head.yRot = par5 / 57.295776f; head.zRot = 0;
            head.setPos(0, 0, 0); rightWing.setPos(0, 0, 0); leftWing.setPos(0, 0, 0);
            body.xRot = 0.7853982f + Mth.cos(par4 * 0.1f) * 0.15f; body.yRot = 0;
            rightWing.yRot = Mth.cos(par4 * 1.3f) * (float) Math.PI * 0.25f; leftWing.yRot = -rightWing.yRot;
            rightTip.yRot = rightWing.yRot * 0.5f; leftTip.yRot = -rightWing.yRot * 0.5f;
        }
    }
}
