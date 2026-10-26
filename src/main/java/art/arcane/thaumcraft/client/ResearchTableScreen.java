package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.ResearchTableMenu;
import art.arcane.thaumcraft.research.ResearchCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private final Button[] actions=new Button[9];
    private final Button[] aids=new Button[3];
    private int selectedAids;
    public ResearchTableScreen(ResearchTableMenu menu,Inventory inventory,Component title){super(menu,inventory,title,255,255);inventoryLabelX=77;inventoryLabelY=178;}
    private void add(int action,String label,int x,int y,int width){actions[action]=addRenderableWidget(Button.builder(Component.translatable("screen.thaumcraft.theory."+label),b->{
        if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.button(action==0&&menu.value(1)==0?selectedAids:action));
    }).bounds(leftPos+x,topPos+y,width,11).build(builder->new OriginalImageButton(builder,37,66,action==8?16720418:8978346)));}
    @Override protected void init(){super.init();
        add(0,"start",104,17,49);
        for(int i=0;i<3;i++){final int aid=i;aids[i]=addRenderableWidget(Button.builder(Component.empty(),b->selectedAids^=1<<aid).bounds(leftPos+98+i*20,topPos+85,16,16).build(builder->new Button(builder){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
                if((selectedAids&(1<<aid))!=0||isHoveredOrFocused())g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("thaumcraft:textures/gui/gui_base.png"),getX(),getY(),0,96,16,16,256,256);
                var item=aid==0?net.minecraft.world.item.Items.BOOKSHELF:aid==1?art.arcane.thaumcraft.ModItems.BLOCK_CRUCIBLE.get():art.arcane.thaumcraft.ModItems.BLOCK_WAND_WORKBENCH.get();
                g.item(new net.minecraft.world.item.ItemStack(item),getX(),getY());
            }
        }));}actions[2]=addRenderableWidget(Button.builder(Component.translatable("screen.thaumcraft.theory.draw"),b->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.button(menu.value(4)>0?3:2));
        }).bounds(leftPos+25,topPos+55,75,90).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.thaumcraft.theory.draw"))).build(builder->new Button(builder){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
                for(int i=0;i<3;i++)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("thaumcraft:textures/gui/paper.png"),getX()-8+i,getY()-3+i,0,0,96,96,256,256,256,256,isHoveredOrFocused()?0xFFFFFFFF:0xFFE6E6E6);
            }
        }));
        for(int i=0;i<3;i++){final int choice=i;actions[4+i]=addRenderableWidget(Button.builder(Component.empty(),b->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,menu.button(4+choice));
        }).bounds(leftPos+78+i*110,topPos+40,100,120).build(builder->new TheoryCardButton(builder,menu,choice)));}
        add(7,"claim",167,91,49);add(8,"abandon",104,163,49);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
        int state=menu.value(1);selectedAids&=menu.value(5);
        int choices=0;for(int i=0;i<3;i++)if(menu.value(13+i*4)>=0)choices++;
        for(int i=0;i<3;i++)if(actions[4+i]!=null)actions[4+i].setPosition(leftPos+5+128-55*choices+110*i,topPos+40);
        int aidRank=0,aidCount=Integer.bitCount(menu.value(5)&7);
        for(int i=0;i<3;i++){var aid=aids[i];if(aid==null)continue;aid.visible=state==0&&(menu.value(5)&(1<<i))!=0;
            if(aid.visible)aid.setPosition(leftPos+128-aidCount*10+20*aidRank++,topPos+85);
            aid.setMessage(Component.literal((selectedAids&(1<<i))!=0?"[x] ":"[ ] ").append(Component.translatable("screen.thaumcraft.theory.aid."+i)));
            aid.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.thaumcraft.theory.aid_help."+i)));
        }
        for(int i=0;i<actions.length;i++){var button=actions[i];if(button==null)continue;
            button.visible=switch(i){case 0,1->state==0;case 2,3->state==1&&menu.value(13)<0;case 7->state==2;case 8->state==1||state==2;default->state==1&&menu.value(13+(i-4)*4)>=0;};
            button.active=button.visible&&(i!=1||menu.value(5)>0)&&(i!=3||menu.value(4)>0)&&((i!=2&&i!=3)||menu.value(13)<0);
            if(i>=4&&i<=6&&button.visible){int kind=menu.value(13+(i-4)*4);button.setMessage(TheoryCardText.name(kind));
                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(TheoryCardText.help(menu,i-4)));}
        }
        super.extractRenderState(g,mouseX,mouseY,tick);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){super.extractBackground(g,mouseX,mouseY,tick);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("thaumcraft:textures/gui/gui_research_table.png"),leftPos,topPos,0,0,255,255,256,256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int state=menu.value(1),count=menu.value(3),remaining=state==0?Math.max(0,count-Integer.bitCount(selectedAids)):menu.value(2);
        for(int i=0;i<count;i++)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.parse("thaumcraft:textures/gui/gui_base.png"),128-count*5+i*10,state==0?40:16,i<remaining?32:48,96,8,8,16,16,256,256);
        int column=0;for(int i=0;i<7;i++)if(menu.value(6+i)>0){int x=253,y=16+column*18;
            var category=Component.translatable("tc.research_category."+ResearchCatalog.CATEGORIES.get(i));
            if((menu.value(25)&(1<<i))!=0)category.withStyle(net.minecraft.ChatFormatting.STRIKETHROUGH);
            int size=i==0?32:256;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,TheoryCardButton.categoryIcon(i),x,y,0,0,16,16,size,size,size,size);
            var amount=Component.literal(Integer.toString(menu.value(6+i)));
            if((menu.value(25)&(1<<i))!=0)amount.withStyle(net.minecraft.ChatFormatting.STRIKETHROUGH);
            ThaumcraftTheme.text(g,font,amount,x+23,y+4,0xFFE5D6B3,false);
            if(mouseX-leftPos>=x&&mouseX-leftPos<x+50&&mouseY-topPos>=y&&mouseY-topPos<y+11)g.setTooltipForNextFrame(font,ThaumcraftTheme.ink(Component.translatable("screen.thaumcraft.theory.total",category,menu.value(6+i))),mouseX,mouseY);
            column++;}

    }
}
