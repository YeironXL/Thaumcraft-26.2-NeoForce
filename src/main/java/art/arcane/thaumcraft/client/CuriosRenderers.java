package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.render.TcRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * TC6 IRenderBauble (RenderType.HEAD) for the goggles of revealing and the curiosity band: a 16x16 sprite extruded
 * 0.1 deep (UtilsFX.renderTextureIn3D) placed over the face at 0.55 scale, pushed 0.12 outwards when a helmet is worn.
 * Only loaded when Curios is present.
 */
public final class CuriosRenderers {
    private static final Identifier GOGGLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/item/goggles_bauble.png");
    private static final Identifier BAND = Identifier.fromNamespaceAndPath("thaumcraft", "textures/item/curiosity_band_worn.png");

    private record Face(Identifier tex, boolean alwaysArmor) implements ICurioRenderer {
        @Override
        public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack, SlotContext ctx, PoseStack ps,
                SubmitNodeCollector c, int light, S state, RenderLayerParent<S, M> parent, EntityRendererProvider.Context context, float yRot, float xRot) {
            if (!(parent.getModel() instanceof HumanoidModel<?> model) || !(state instanceof HumanoidRenderState hs)) return;
            // ItemGoggles tests getItemStackFromSlot(HEAD) != null, which is always true; ItemCuriosityBand tests !isEmpty().
            boolean armor = alwaysArmor || !hs.headEquipment.isEmpty();
            ps.pushPose();
            model.head.translateAndRotate(ps);
            ps.translate(0.0f, -0.25f, -0.25f - 0.005f);
            ps.mulPose(Axis.ZP.rotationDegrees(180.0f));
            ps.scale(0.55f, 0.55f, 0.55f);
            ps.translate(-0.5f, -0.5f, armor ? -0.12f : 0.0f);
            c.submitCustomGeometry(ps, RenderTypes.entityCutout(tex), (pose, vc) -> TcRender.textureIn3D(pose, vc, 0.0f, 0.0f, 1.0f, 1.0f, 16, 26, 0.1f, light));
            ps.popPose();
        }
    }

    public static void register(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> {
            ICurioRenderer.register(ModItems.GOGGLES.get(), () -> new Face(GOGGLES, true));
            ICurioRenderer.register(ModItems.CURIOSITY_BAND.get(), () -> new Face(BAND, false));
        });
    }
    private CuriosRenderers() {}
}
