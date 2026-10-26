package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.ArcaneWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    private static final Identifier TEXTURE=Identifier.parse("thaumcraft:textures/gui/arcaneworkbench.png");
    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title,190,234);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick) {
        super.extractBackground(g,mouseX,mouseY,tick);
        g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY) {
        ThaumcraftTheme.text(g,font,title,8,-13,0xFFE5D6B3,false);
        g.pose().pushMatrix();
        g.pose().translate(144,43);
        g.pose().scale(0.65f,0.65f);
        ThaumcraftTheme.text(g,font,menu.availableVis()+" vis",0,0,0xFFE5D6B3,false);
        ThaumcraftTheme.text(g,font,menu.requiredVis()+" vis",0,65,menu.availableVis()>=menu.requiredVis()?0xFFE5D6B3:0xFFFF8A86,false);
        g.pose().popMatrix();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick) {
        super.extractRenderState(g,mouseX,mouseY,tick);
        if(mouseX>=leftPos+142&&mouseX<leftPos+190&&mouseY>=topPos+28&&mouseY<topPos+105)
            g.setTooltipForNextFrame(ThaumcraftTheme.ink(Component.translatable("screen.thaumcraft.workbench.vis",menu.availableVis(),menu.requiredVis())),mouseX,mouseY);
    }
}
