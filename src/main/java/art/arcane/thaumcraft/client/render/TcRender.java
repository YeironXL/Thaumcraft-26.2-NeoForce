package art.arcane.thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/** Rendering helpers shared by Thaumcraft block entity renderers (UtilsFX equivalents). */
public final class TcRender {
    public static TextureAtlasSprite blockSprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(id);
    }
    /** Horizontal quad covering x/z 0..1 at height y, facing up (UtilsFX.renderQuadFromIcon after a -90° X rotation). */
    public static void flatQuad(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, float y, int argb, int light) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        buffer.addVertex(pose, 0, y, 1).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, 1, y, 1).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, 1, y, 0).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        buffer.addVertex(pose, 0, y, 0).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }
    /** UtilsFX.renderTextureIn3D: a sprite extruded into a 1x1 slab of the given thickness (one strip per texel edge). */
    public static void textureIn3D(PoseStack.Pose p, VertexConsumer b, float maxu, float maxv, float minu, float minv, int width, int height, float t, int light) {
        java.util.function.BiConsumer<float[], float[]> q = (pos, n) -> {
            for (int i = 0; i < 4; i++) b.addVertex(p, pos[i * 5], pos[i * 5 + 1], pos[i * 5 + 2]).setColor(-1).setUv(pos[i * 5 + 3], pos[i * 5 + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, n[0], n[1], n[2]).setNormal(p, 0f, 1f, 0f);
        };
        q.accept(new float[] {0, 0, 0, maxu, minv, 1, 0, 0, minu, minv, 1, 1, 0, minu, maxv, 0, 1, 0, maxu, maxv}, new float[] {0, 0, 1});
        q.accept(new float[] {0, 1, -t, maxu, maxv, 1, 1, -t, minu, maxv, 1, 0, -t, minu, minv, 0, 0, -t, maxu, minv}, new float[] {0, 0, -1});
        float f5 = 0.5f * (maxu - minu) / width, f6 = 0.5f * (minv - maxv) / height;
        for (int k = 0; k < width; ++k) {
            float f7 = k / (float) width, f8 = maxu + (minu - maxu) * f7 - f5, f9 = f7 + 1.0f / width;
            q.accept(new float[] {f7, 0, -t, f8, minv, f7, 0, 0, f8, minv, f7, 1, 0, f8, maxv, f7, 1, -t, f8, maxv}, new float[] {-1, 0, 0});
            q.accept(new float[] {f9, 1, -t, f8, maxv, f9, 1, 0, f8, maxv, f9, 0, 0, f8, minv, f9, 0, -t, f8, minv}, new float[] {1, 0, 0});
        }
        for (int k = 0; k < height; ++k) {
            float f7 = k / (float) height, f8 = minv + (maxv - minv) * f7 - f6, f9 = f7 + 1.0f / height;
            q.accept(new float[] {0, f9, 0, maxu, f8, 1, f9, 0, minu, f8, 1, f9, -t, minu, f8, 0, f9, -t, maxu, f8}, new float[] {0, 1, 0});
            q.accept(new float[] {1, f7, 0, minu, f8, 0, f7, 0, maxu, f8, 0, f7, -t, maxu, f8, 1, f7, -t, minu, f8}, new float[] {0, -1, 0});
        }
    }
    private TcRender() {}
}
