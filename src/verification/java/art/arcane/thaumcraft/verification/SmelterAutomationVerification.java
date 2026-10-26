package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class SmelterAutomationVerification {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void test(GameTestHelper h){
        var world=h.getLevel();
        int x=0;
        for(var block:java.util.List.of(ModBlocks.SMELTER_BASIC.get(),ModBlocks.SMELTER_THAUMIUM.get(),ModBlocks.SMELTER_VOID.get())){
            var relative=new BlockPos(x++,1,1);h.setBlock(relative,block);
            var pos=h.absolutePos(relative);var smelter=h.getBlockEntity(relative,EssentiaSmelterBlockEntity.class);
            var side=world.getCapability(Capabilities.Item.BLOCK,pos,Direction.WEST);
            var bottom=world.getCapability(Capabilities.Item.BLOCK,pos,Direction.DOWN);
            var top=world.getCapability(Capabilities.Item.BLOCK,pos,Direction.UP);
            check(side!=null && bottom!=null && top!=null && side.size()==1 && bottom.size()==1 && top.size()==0,"Incorrect sided capability");
            try(var tx=Transaction.openRoot()){
                check(side.insert(0,ItemResource.of(Items.COBBLESTONE),70,tx)==64,"Input stack capacity changed");
            }
            check(smelter.input().isEmpty(),"Aborted input transfer mutated inventory");
            try(var tx=Transaction.openRoot()){
                check(side.insert(0,ItemResource.of(Items.IRON_INGOT),2,tx)==2,"Valid input rejected");
                check(side.insert(0,ItemResource.of(Items.COBBLESTONE),1,tx)==0,"Mixed input stack accepted");
                check(bottom.insert(0,ItemResource.of(Items.STONE),1,tx)==0,"Nonfuel accepted below");
                check(bottom.insert(0,ItemResource.of(Items.COAL),1,tx)==1,"Fuel rejected below");tx.commit();
            }
            EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);
            int progress=smelter.cook();check(progress==1 && smelter.burn()>0,"Automation inventory did not start distillation");
            try(var tx=Transaction.openRoot()){
                check(side.extract(0,ItemResource.of(Items.IRON_INGOT),2,tx)==2,"Input extraction failed");
            }
            check(smelter.input().getCount()==2 && smelter.cook()==progress,"Aborted extraction lost items or progress");
            EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);
            check(smelter.cook()==progress+1,"Rollback reset processing progress");
            var restored=new EssentiaSmelterBlockEntity(pos,smelter.getBlockState());
            restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),smelter.saveWithoutMetadata(world.registryAccess())));
            check(restored.input().getCount()==2 && restored.burn()==smelter.burn() && restored.cook()==smelter.cook(),"Automation inventory failed save/reload");
            smelter.setItem(0,new ItemStack(Items.GOLD_INGOT));
            var changed=new EssentiaSmelterBlockEntity(pos,smelter.getBlockState());
            changed.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),smelter.saveWithoutMetadata(world.registryAccess())));
            changed.setLevel(world);EssentiaSmelterBlockEntity.tick(world,pos,changed.getBlockState(),changed);
            check(changed.cook()==1,"Input replacement before save inherited old processing progress");
            EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);
            check(smelter.cook()==1,"Replacing input inherited previous cook progress");
        }
        var machine=new BlockPos(3,1,3);h.setBlock(machine,ModBlocks.SMELTER_BASIC.get());
        var smelter=h.getBlockEntity(machine,EssentiaSmelterBlockEntity.class);
        var source=new BlockPos(2,1,3);h.setBlock(source,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST));
        var feed=h.getBlockEntity(source,HopperBlockEntity.class);feed.setItem(0,new ItemStack(Items.COBBLESTONE,2));
        h.setBlock(machine.above(),Blocks.HOPPER);
        var blocked=h.getBlockEntity(machine.above(),HopperBlockEntity.class);blocked.setItem(0,new ItemStack(Items.IRON_INGOT));
        var lock=machine.below().east();h.setBlock(lock,Blocks.REDSTONE_BLOCK);
        h.setBlock(machine.below(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.ENABLED,false));
        var drain=h.getBlockEntity(machine.below(),HopperBlockEntity.class);
        smelter.setItem(1,new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(50,()->{
            check(feed.isEmpty() && smelter.input().isEmpty() && smelter.total()>0,"Side hopper failed: feed="+feed.getItem(0)+", input="+smelter.input()+", fuel="+smelter.fuel()+", burn="+smelter.burn()+", drained="+drain.getItem(0));
            check(blocked.getItem(0).is(Items.IRON_INGOT),"Top hopper bypassed alembic-only face");
            check(smelter.fuel().is(Items.BUCKET) && smelter.burn()>0,"Fuel container remainder lost");
            h.setBlock(lock,Blocks.AIR);
            h.runAfterDelay(30,()->{
                check(drain.countItem(Items.BUCKET)==1 && smelter.fuel().isEmpty(),"Bottom hopper did not retrieve fuel remainder");
                check(drain.countItem(Items.COBBLESTONE)==0,"Bottom hopper extracted material input");
                System.out.println("Smelter automation passed: three variants, sided transfer, rollback, persistence and live hoppers.");
                h.succeed();
            });
        });
    }
}
