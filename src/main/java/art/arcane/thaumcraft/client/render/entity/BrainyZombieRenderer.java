package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** TC6 RenderBrainyZombie: the zombie with textures/entity/bzombie.png; furious zombies grow by (1 + anger) and redden. */
public final class BrainyZombieRenderer extends ZombieRenderer {
    public static final class State extends ZombieRenderState { public float anger; public boolean giant; }
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/bzombie.png");

    public BrainyZombieRenderer(EntityRendererProvider.Context ctx) { super(ctx); }
    @Override public ZombieRenderState createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(ZombieRenderState s) { return TEX; }
    @Override
    public void extractRenderState(Zombie e, ZombieRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        if (s instanceof State st) { st.giant = e instanceof GiantBrainyZombieEntity; st.anger = e instanceof GiantBrainyZombieEntity g ? g.getAnger() : 0.0f; }
    }
    @Override
    protected void scale(ZombieRenderState s, PoseStack ps) {
        super.scale(s, ps);
        if (s instanceof State st && st.giant) ps.scale(1.0f + st.anger, 1.0f + st.anger, 1.0f + st.anger);
    }
    @Override
    protected int getModelTint(ZombieRenderState s) {
        if (!(s instanceof State st) || !st.giant) return super.getModelTint(s);
        float q = Math.min(1.0f, st.anger) / 2.0f;
        return ARGB.colorFromFloat(1.0f, 1.0f, 1.0f - q, 1.0f - q);
    }
}
