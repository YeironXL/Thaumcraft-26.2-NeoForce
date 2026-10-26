package art.arcane.thaumcraft.verification;
import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
public final class CentrifugeVerification {
    private static final BlockPos SOURCE=new BlockPos(1,1,1), MACHINE=new BlockPos(1,2,1), POWER=new BlockPos(2,2,1), BUFFER=new BlockPos(1,3,1);
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static EssentiaCentrifugeBlockEntity reload(GameTestHelper h,EssentiaCentrifugeBlockEntity machine){
        var copy=new EssentiaCentrifugeBlockEntity(h.absolutePos(MACHINE),machine.getBlockState());
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),machine.saveWithoutMetadata(h.getLevel().registryAccess())));return copy;
    }
    public static void test(GameTestHelper h){
        h.setBlock(SOURCE,ModBlocks.JAR_NORMAL.get());h.setBlock(MACHINE,ModBlocks.CENTRIFUGE.get());
        var source=h.getBlockEntity(SOURCE,EssentiaJarBlockEntity.class);var machine=h.getBlockEntity(MACHINE,EssentiaCentrifugeBlockEntity.class);
        source.insert("aer",4);
        h.runAfterDelay(60,()->{
            check(source.contents().amount()==4 && machine.input().isEmpty() && machine.aspect().isEmpty(),"Centrifuge consumed primal essence");
            source.extract(4);source.insert("potentia",4);h.setBlock(POWER,Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(50,()->{
                check(source.contents().amount()==4 && machine.suction(Direction.DOWN)==0,"Powered centrifuge consumed input");h.setBlock(POWER,Blocks.AIR);
                h.runAfterDelay(10,()->{
                    check(machine.input().equals("potentia") && machine.remaining()>0 && machine.suction(Direction.DOWN)==64,"Centrifuge did not start processing");
                    h.setBlock(POWER,Blocks.REDSTONE_BLOCK);int remaining=machine.remaining();var copy=reload(h,machine);
                    check(copy.input().equals(machine.input()) && copy.remaining()==remaining,"In-flight centrifuge state was not saved");
                    h.runAfterDelay(80,()->{
                        check(machine.remaining()==remaining,"Redstone did not pause countdown");h.setBlock(POWER,Blocks.AIR);
                        h.runAfterDelay(60,()->{
                            String output=machine.aspect();check(output.equals("ordo")||output.equals("ignis"),"Centrifuge produced an unrelated component");
                            check(reload(h,machine).aspect().equals(output),"Saving rerolled or lost completed output");
                            h.runAfterDelay(80,()->{
                                check(source.contents().amount()==3 && machine.aspect().equals(output),"Blocked output consumed another input or rerolled");
                                check(!machine.extractOne(output,Direction.DOWN) && !machine.extractOne("aqua",Direction.UP),"Centrifuge allowed invalid extraction");
                                check(machine.extractOne(output,Direction.UP) && !machine.extractOne(output,Direction.UP),"Centrifuge extraction duplicated output");
                                h.setBlock(BUFFER,ModBlocks.TUBE_BUFFER.get());
                                h.runAfterDelay(220,()->{
                                    var buffer=h.getBlockEntity(BUFFER,EssentiaTubeBlockEntity.class);
                                    check(source.contents().amount()==0 && machine.input().isEmpty() && machine.aspect().isEmpty() && buffer.amount()==3,"Centrifuge/buffer chain lost or duplicated essence");
                                    check(buffer.contents().keySet().stream().allMatch(tag->tag.equals("ordo")||tag.equals("ignis")),"Buffer received invalid components");
                                    System.out.println("Centrifuge checks passed: primal rejection, redstone pause, saved countdown/output, original components, backpressure, side restrictions and buffer integration.");h.succeed();
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}
