package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.ResearchTableBlock;
import art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** TC6 research table conversion moves the held tools into the new table, including in creative. */
public final class ScribingToolsItem extends Item {
    public ScribingToolsItem(Item.Properties properties){super(properties.stacksTo(1).durability(100));}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var stack=context.getItemInHand();
        if(!level.getBlockState(pos).is(ModBlocks.TABLE_WOOD.get())||context.getPlayer()==null||!context.getPlayer().mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.PASS;
        if(!level.setBlockAndUpdate(pos,ModBlocks.RESEARCH_TABLE.get().defaultBlockState().setValue(ResearchTableBlock.FACING,player.getDirection())))return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table){table.setItem(0,stack.copyWithCount(1));stack.shrink(1);PlayerProgressManager.get(player).recordCraft("thaumcraft:research_table");}
        return InteractionResult.SUCCESS;
    }
}
