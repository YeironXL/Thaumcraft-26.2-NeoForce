package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** TC6 FXBreakingFade: an item-break chip, tinted, translucent and fading out over its life. */
public final class BreakingFadeParticle extends BreakingItemParticle {
    private static final ItemStackRenderState SCRATCH = new ItemStackRenderState();
    private float baseAlpha = 1.0f;

    public BreakingFadeParticle(ClientLevel level, double x, double y, double z, Item item) { super(level, x, y, z, sprite(level, item)); }
    private static TextureAtlasSprite sprite(ClientLevel level, Item item) {
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(SCRATCH, new ItemStack(item), ItemDisplayContext.GROUND, level, null, 0);
        var m = SCRATCH.pickParticleMaterial(level.getRandom());
        return m != null ? m.sprite() : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
    }
    public BreakingFadeParticle color(float r, float g, float b) { rCol = r; gCol = g; bCol = b; return this; }
    public BreakingFadeParticle alpha(float a) { baseAlpha = a; alpha = a; return this; }
    public BreakingFadeParticle maxAge(int age) { lifetime = age; return this; }
    @Override public SingleQuadParticle.Layer getLayer() { return SingleQuadParticle.Layer.TRANSLUCENT_ITEMS; }
    @Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
        alpha = baseAlpha * (1.0f - age / (float) lifetime);
        super.extract(state, camera, partial);
    }
}
