package art.arcane.thaumcraft.verification;
import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.*;
import art.arcane.thaumcraft.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
public final class TubeVariantsVerification {
    private static final BlockPos SOURCE=new BlockPos(0,2,2), TUBE=new BlockPos(1,2,2), JAR=new BlockPos(1,1,2);
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    private static EssentiaTubeBlockEntity setup(GameTestHelper h,net.minecraft.world.level.block.Block block){
        for(int x=0;x<5;x++)for(int y=1;y<4;y++)for(int z=1;z<4;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        h.setBlock(SOURCE,ModBlocks.ALEMBIC.get());h.setBlock(TUBE,block.defaultBlockState().setValue(EssentiaTubeBlock.FACING,Direction.UP));h.setBlock(JAR,ModBlocks.JAR_NORMAL.get());
        check(h.getBlockEntity(SOURCE,EssentiaJarBlockEntity.class).insert("aer",20),"source setup");
        return h.getBlockEntity(TUBE,EssentiaTubeBlockEntity.class);
    }
    private static int received(GameTestHelper h){return h.getBlockEntity(JAR,EssentiaJarBlockEntity.class).contents().amount();}
    private static void configure(GameTestHelper h,EssentiaTubeBlockEntity tube,String aspect){
        int[] messages={0};
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"tube-test-player"),net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component message){messages[0]++;}
        };
        var phial=art.arcane.thaumcraft.item.EssentiaPhialItem.filled(aspect);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,phial);
        var pos=h.absolutePos(TUBE);
        tube.getBlockState().useItemOn(phial,h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        check(tube.filter().equals(aspect) && phial.getCount()==1 && messages[0]==1,"Filter click consumed sample or failed");
    }
    public static void test(GameTestHelper h){
        var filter=setup(h,ModBlocks.TUBE_FILTER.get());configure(h,filter,"ignis");
        h.runAfterDelay(80,()->{
            check(received(h)==0 && filter.amount()==0,"Filter accepted wrong aspect");configure(h,filter,"aer");
            h.runAfterDelay(100,()->{
                check(received(h)>0,"Filter did not pass matching aspect");
                var restored=new EssentiaTubeBlockEntity(h.absolutePos(TUBE),filter.getBlockState());
                restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),filter.saveWithoutMetadata(h.getLevel().registryAccess())));
                check(restored.filter().equals("aer"),"Filter was not saved");
                valve(h);
            });
        });
    }
    private static void valve(GameTestHelper h){
        var valve=setup(h,ModBlocks.TUBE_VALVE.get());h.setBlock(new BlockPos(1,2,3),Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(80,()->{
            check(!valve.open() && received(h)==0,"Powered valve allowed flow");h.setBlock(new BlockPos(1,2,3),Blocks.AIR);
            h.runAfterDelay(80,()->{
                check(valve.open() && received(h)>0,"Valve did not reopen after redstone");valve.toggleValve();int count=received(h);
                h.runAfterDelay(40,()->{check(received(h)==count,"Manual closed valve leaked");restrict(h);});
            });
        });
    }
    private static void restrict(GameTestHelper h){
        var tube=setup(h,ModBlocks.TUBE_RESTRICT.get());
        h.runAfterDelay(80,()->{check(tube.suction(Direction.DOWN)==16 && received(h)>0,"Restrictor did not halve suction/pass essence");oneway(h);});
    }
    private static void oneway(GameTestHelper h){
        var tube=setup(h,ModBlocks.TUBE_ONEWAY.get());
        h.getLevel().setBlock(h.absolutePos(TUBE),tube.getBlockState().setValue(EssentiaTubeBlock.FACING,Direction.WEST),3);
        h.runAfterDelay(80,()->{
            check(received(h)==0,"One-way tube allowed reverse flow");h.getLevel().setBlock(h.absolutePos(TUBE),tube.getBlockState().setValue(EssentiaTubeBlock.FACING,Direction.DOWN),3);
            h.runAfterDelay(100,()->{check(received(h)>0,"One-way outlet failed");buffer(h);});
        });
    }
    private static void buffer(GameTestHelper h){
        var tube=setup(h,ModBlocks.TUBE_BUFFER.get());h.setBlock(JAR,Blocks.AIR);
        var source=h.getBlockEntity(SOURCE,EssentiaJarBlockEntity.class);source.extract(20);source.insert("aer",6);
        var second=new BlockPos(2,2,2);h.setBlock(second,ModBlocks.ALEMBIC.get());h.getBlockEntity(second,EssentiaJarBlockEntity.class).insert("ignis",6);
        h.runAfterDelay(100,()->{
            check(tube.amount()==10 && tube.contents().size()==2,"Buffer is not a ten-unit multi-aspect store");
            tube.toggleSide(Direction.WEST);check(!tube.connects(Direction.WEST),"Port closure failed");tube.toggleSide(Direction.WEST);
            var bellow=new BlockPos(1,3,2);h.setBlock(bellow,ModBlocks.BELLOWS.get().defaultBlockState().setValue(EssentiaBellowsBlock.FACING,Direction.DOWN));
            check(tube.suction(Direction.DOWN)==32,"Bellows did not increase buffer suction");tube.cycleChoke(Direction.DOWN);check(tube.suction(Direction.DOWN)==1,"Reduced buffer suction failed");tube.cycleChoke(Direction.DOWN);check(tube.suction(Direction.DOWN)==0,"Zero buffer suction failed");tube.cycleChoke(Direction.DOWN);tube.cycleChoke(Direction.DOWN);
            h.setBlock(JAR,ModBlocks.JAR_NORMAL.get());h.getBlockEntity(JAR,EssentiaJarBlockEntity.class).insert("aer",1);
            h.runAfterDelay(100,()->{
                check(received(h)==7 && tube.contents().getOrDefault("ignis",0)>0,"Buffer failed to select requested aspect");
                var restored=new EssentiaTubeBlockEntity(h.absolutePos(TUBE),tube.getBlockState());restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tube.saveWithoutMetadata(h.getLevel().registryAccess())));
                check(restored.contents().equals(tube.contents()) && restored.choke(Direction.DOWN)==1,"Buffer contents/chokes not persisted");
                System.out.println("All tube variants passed: filter, redstone/manual valve, half suction, one-way, multi-aspect capacity, bellows, side regulation, persistence.");h.succeed();
            });
        });
    }
}
