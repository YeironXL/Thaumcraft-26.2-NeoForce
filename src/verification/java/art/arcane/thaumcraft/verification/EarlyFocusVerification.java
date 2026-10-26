package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.*;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.*;
import art.arcane.thaumcraft.crafting.CrucibleRecipe;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModRecipes;
import art.arcane.thaumcraft.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import java.util.*;

public final class EarlyFocusVerification {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static ItemStack craft(GameTestHelper h,int width,int height,List<ItemStack> stacks){
        var input=CraftingInput.of(width,height,stacks);return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value().assemble(input);
    }
    private static void fire(ServerPlayer player,net.minecraft.world.damagesource.DamageSource source){
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(player,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,1)));
    }
    private static ServerPlayer verifyBeginning(GameTestHelper h){
        var world=h.getLevel();var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var research=PlayerProgressManager.get(player);player.setShiftKeyDown(true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
        check(player.getInventory().countItem(ModItems.THAUMONOMICON.get())==0&&research.researched().stream().noneMatch(key->key.equals("FIRSTSTEPS")),"New player already progressed or received a login book");
        // TC6 route: crystal pickup -> sleep -> dream journal -> Salis Mundus on a bookshelf -> floating Thaumonomicon.
        var shelfRelative=new BlockPos(14,1,2);h.setBlock(shelfRelative,net.minecraft.world.level.block.Blocks.BOOKSHELF);var shelfPos=h.absolutePos(shelfRelative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(shelfPos.above()));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.SALIS_MUNDUS.get(),2));
        var shelfUse=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(shelfPos),net.minecraft.core.Direction.UP,shelfPos,false));
        check(ModItems.SALIS_MUNDUS.get().useOn(shelfUse)==net.minecraft.world.InteractionResult.PASS&&player.getMainHandItem().getCount()==2,"Bookshelf accepted Salis before the dream");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent(player,false,true));
        check(!research.has("!gotdream"),"Sleeping without crystals granted the dream");
        var crystalEntity=new net.minecraft.world.entity.item.ItemEntity(world,player.getX(),player.getY(),player.getZ(),ItemStack.EMPTY);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post(player,crystalEntity,CrystalEssenceItem.of("aer",1)));
        check(research.has("!gotcrystals"),"Crystal pickup did not mark gotcrystals");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent(player,false,true));
        boolean journal=false;for(var stack:player.getInventory().getNonEquipmentItems())if(stack.is(Items.WRITTEN_BOOK)){var content=stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);journal|=content!=null&&content.generation()==3&&content.pages().size()==3;}
        check(research.has("!gotdream")&&journal,"Waking after crystals did not give the worn three-page dream journal");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent(player,false,true));
        int journals=0;for(var stack:player.getInventory().getNonEquipmentItems())if(stack.is(Items.WRITTEN_BOOK))journals+=stack.getCount();
        check(journals==1,"Dream journal was given twice");
        check(ModItems.SALIS_MUNDUS.get().useOn(shelfUse)==net.minecraft.world.InteractionResult.SUCCESS&&player.getMainHandItem().getCount()==1,"Salis on bookshelf did not trigger after the dream");
        art.arcane.thaumcraft.progression.DustSwaps.advance(art.arcane.thaumcraft.progression.DustSwaps.DELAY_TICKS-1);
        check(world.getBlockState(shelfPos).is(net.minecraft.world.level.block.Blocks.BOOKSHELF),"Bookshelf changed before the 50-tick delay");
        art.arcane.thaumcraft.progression.DustSwaps.advance(1);
        check(world.getBlockState(shelfPos).isAir(),"Bookshelf was not consumed after the delay");
        var floating=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(shelfPos).inflate(0.5),entity->entity.getItem().is(ModItems.THAUMONOMICON.get()));
        check(floating.size()==1&&floating.getFirst() instanceof art.arcane.thaumcraft.entity.SpecialItemEntity,"Floating Thaumonomicon missing after bookshelf conversion");
        floating.getFirst().playerTouch(player);
        check(player.getInventory().countItem(ModItems.THAUMONOMICON.get())==1&&research.has("!gotthaumonomicon"),"Picking up the floating Thaumonomicon failed");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.THAUMONOMICON.get()));ModItems.THAUMONOMICON.get().use(world,player,net.minecraft.world.InteractionHand.MAIN_HAND);
        check(research.has("!gotthaumonomicon")&&ResearchProgression.advance(research,"FIRSTSTEPS",-1,cost->cost.isEmpty()),"Opening starter book did not unlock first research stage");
        var relative=new BlockPos(14,1,4);h.setBlock(relative,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.SALIS_MUNDUS.get(),2));
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        player.setShiftKeyDown(false);check(ModItems.SALIS_MUNDUS.get().useOn(context)==net.minecraft.world.InteractionResult.PASS&&player.getMainHandItem().getCount()==2,"Salis converted the crafting table without sneaking");player.setShiftKeyDown(true);
        check(ModItems.SALIS_MUNDUS.get().useOn(context)==net.minecraft.world.InteractionResult.SUCCESS&&player.getMainHandItem().getCount()==1&&world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE),"Survival Salis use failed or converted instantly");
        art.arcane.thaumcraft.progression.DustSwaps.advance(art.arcane.thaumcraft.progression.DustSwaps.DELAY_TICKS);check(world.getBlockState(pos).is(ModBlocks.ARCANE_WORKBENCH.get()),"Survival Salis conversion failed after delay");
        check(ResearchProgression.advance(research,"FIRSTSTEPS",1,cost->cost.isEmpty())&&research.stage("FIRSTSTEPS")==2,"Workbench craft history did not advance first stage");
        var bench=h.getBlockEntity(relative,ArcaneWorkbenchBlockEntity.class);for(int slot:new int[]{1,3,5,7})bench.setItem(slot,new ItemStack(Items.GOLD_INGOT));bench.setItem(4,new ItemStack(Items.GLASS_PANE));
        for(int primal=0;primal<6;primal++)bench.setItem(9+primal,CrystalEssenceItem.of(ArcaneWorkbenchBlockEntity.PRIMALS.get(primal),1));
        var aura=NativeAura.get(world,world.getChunkAt(pos));aura.spend(aura.vis());aura.addVis(20);
        var thermometer=bench.takeOutput(player,1);check(thermometer.is(ModItems.THAUMOMETER.get())&&aura.vis()==0,"Initial thaumometer recipe/cost blocked player");player.getInventory().add(thermometer);
        check(!ResearchProgression.advance(research,"FIRSTSTEPS",2,cost->cost.isEmpty()),"First steps bypassed observation requirement");
        for(String id:List.of("minecraft:stone","minecraft:dirt","minecraft:oak_log","minecraft:sand","minecraft:cobblestone","minecraft:gravel","minecraft:oak_leaves","minecraft:grass_block","minecraft:coal","minecraft:flint","minecraft:feather","minecraft:string","minecraft:bone","minecraft:rotten_flesh","minecraft:gunpowder","minecraft:spider_eye","minecraft:wheat_seeds","minecraft:sugar_cane","minecraft:egg","minecraft:apple","minecraft:slime_ball","minecraft:glowstone_dust","minecraft:redstone","minecraft:nether_wart","minecraft:blaze_powder","minecraft:ghast_tear","minecraft:fermented_spider_eye","minecraft:magma_cream","minecraft:sugar","minecraft:glass","minecraft:quartz","minecraft:diamond","minecraft:emerald","minecraft:lapis_lazuli","minecraft:iron_ingot","minecraft:gold_ingot"))art.arcane.thaumcraft.scan.ScanProgression.record(research,id,art.arcane.thaumcraft.scan.ScanRegistry.get(id));
        check(ResearchProgression.advance(research,"FIRSTSTEPS",2,cost->cost.isEmpty())&&ResearchRules.complete(research,"FIRSTSTEPS"),"Available initial scan targets did not supply required observations");
        check(ResearchProgression.advance(research,"UNLOCKALCHEMY",-1,cost->cost.isEmpty()),"Alchemy could not start after first steps");
        System.out.println("New-player beginning passed with supplied materials: crystal, dream journal, bookshelf Thaumonomicon, first research, Salis conversion, thaumometer craft, genuine scan rewards and alchemy entry.");
        return player;
    }
    private static void verifyInitialAlchemy(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);int basics=state.knowledge("OBSERVATION","BASICS"),alchemy=state.knowledge("OBSERVATION","ALCHEMY");
        check(ResearchProgression.advance(state,"UNLOCKALCHEMY",1,cost->cost.isEmpty())&&state.knowledge("OBSERVATION","BASICS")==basics-16&&state.knowledge("OBSERVATION","ALCHEMY")==alchemy-16,"Initial alchemy knowledge cost/entry failed");
        check(!ResearchProgression.advance(state,"UNLOCKALCHEMY",2,cost->cost.isEmpty()),"Alchemy skipped crucible conversion");
        var relative=new BlockPos(14,1,8);h.setBlock(relative,net.minecraft.world.level.block.Blocks.CAULDRON);var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.SALIS_MUNDUS.get()));
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        check(ModItems.SALIS_MUNDUS.get().useOn(context)==net.minecraft.world.InteractionResult.SUCCESS&&player.getMainHandItem().isEmpty()&&world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CAULDRON),"Initial crucible conversion did not pay Salis");
        art.arcane.thaumcraft.progression.DustSwaps.advance(art.arcane.thaumcraft.progression.DustSwaps.DELAY_TICKS);check(world.getBlockState(pos).is(ModBlocks.CRUCIBLE.get()),"Cauldron did not become a crucible after the delay");
        check(ResearchProgression.advance(state,"UNLOCKALCHEMY",2,cost->cost.isEmpty())&&state.stage("UNLOCKALCHEMY")==3,"Crucible history failed to advance alchemy");
        var pot=h.getBlockEntity(relative,CrucibleBlockEntity.class);pot.mixture().fill();h.setBlock(relative.below(),net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
        for(int tick=0;tick<151;tick++)CrucibleBlockEntity.tick(world,pos,pot.getBlockState(),pot);
        check(pot.mixture().boiling(),"Magma beneath new crucible did not boil supplied water: heat="+pot.mixture().heat()+" below="+world.getBlockState(pos.below()));
        check(pot.processOne(new ItemStack(Items.COAL),player)&&pot.processOne(new ItemStack(Items.GLOWSTONE_DUST),player),"Initial Nitor ingredient dissolution failed");
        check(!ResearchProgression.advance(state,"UNLOCKALCHEMY",3,cost->cost.isEmpty()),"Alchemy completed before Nitor craft");
        check(pot.processOne(new ItemStack(Items.GLOWSTONE_DUST),player)&&pot.mixture().water()==950&&pot.mixture().total()==5&&state.crafted("thaumcraft:nitor_yellow"),"Initial Nitor production/cost/residual aspects incorrect");
        check(ResearchProgression.advance(state,"UNLOCKALCHEMY",3,cost->cost.isEmpty())&&ResearchRules.complete(state,"UNLOCKALCHEMY"),"Same new player could not complete initial alchemy");
        check(ResearchProgression.advance(state,"UNLOCKAUROMANCY",-1,cost->cost.isEmpty()),"Same new player could not enter auromancy");
        System.out.println("Continuous initial alchemy passed: paid observations, Salis/cauldron, magma heating (TC6 heat source), real coal/glowstone dissolution, Nitor, residual Sensus5 and auromancy entry.");
    }
    private static void verifyInitialAuromancy(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);
        check(!ResearchProgression.advance(state,"UNLOCKAUROMANCY",1,cost->cost.isEmpty()),"Auromancy skipped height discoveries");
        player.tickCount=0;player.setPos(player.getX(),world.getMinY()+5,player.getZ());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        check(state.has("m_deepdown")&&!state.has("m_uphigh")&&!ResearchProgression.advance(state,"UNLOCKAUROMANCY",1,cost->cost.isEmpty()),"One height discovery bypassed the other");
        player.setPos(player.getX(),world.getMinY()+world.getHeight()*0.4+10,player.getZ());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        check(state.has("m_uphigh")&&ResearchProgression.advance(state,"UNLOCKAUROMANCY",1,cost->cost.isEmpty()),"Actual height events failed auromancy progression");
        var relative=new BlockPos(14,1,4);var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        var bench=h.getBlockEntity(relative,ArcaneWorkbenchBlockEntity.class);bench.clearContent();
        var aura=NativeAura.get(world,world.getChunkAt(pos));aura.spend(aura.vis());aura.addVis(50);
        bench.setItem(0,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(1,new ItemStack(Items.QUARTZ));
        bench.setItem(9,CrystalEssenceItem.of("aer",1));bench.setItem(12,CrystalEssenceItem.of("aqua",1));
        var resonator=bench.takeOutput(player,1);
        check(resonator.is(ModItems.VIS_RESONATOR.get())&&aura.vis()==0&&bench.getItem(9).isEmpty()&&bench.getItem(12).isEmpty(),"Initial resonator did not charge 50 vis and two crystals");
        check(!ResearchProgression.advance(state,"UNLOCKAUROMANCY",2,cost->cost.isEmpty()),"Resonator alone completed auromancy");
        bench.clearContent();for(int slot:new int[]{0,1,2})bench.setItem(slot,new ItemStack(Items.IRON_INGOT));for(int slot:new int[]{3,5,6,8})bench.setItem(slot,new ItemStack(Items.LEATHER));
        int thermometerSlot=player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.THAUMOMETER.get()));
        check(thermometerSlot>=0,"Actual crafted thaumometer lost before gauntlet recipe");
        bench.setItem(4,resonator);bench.setItem(7,player.getInventory().removeItem(thermometerSlot,1));
        for(int primal=0;primal<6;primal++)bench.setItem(9+primal,CrystalEssenceItem.of(ArcaneWorkbenchBlockEntity.PRIMALS.get(primal),1));aura.addVis(100);
        var gauntlet=bench.takeOutput(player,1);check(gauntlet.is(ModItems.CASTER_BASIC.get())&&aura.vis()==0,"Initial gauntlet did not charge 100 vis");
        for(int slot=0;slot<15;slot++)check(bench.getItem(slot).isEmpty(),"Gauntlet recipe retained ingredients/crystals: "+slot);
        player.getInventory().add(gauntlet);
        check(ResearchProgression.advance(state,"UNLOCKAUROMANCY",2,cost->cost.isEmpty())&&ResearchRules.complete(state,"UNLOCKAUROMANCY"),"Same new player could not complete gauntlet introduction");
        check(ResearchProgression.advance(state,"BASEAUROMANCY",-1,cost->cost.isEmpty()),"First focus research remained inaccessible");
        System.out.println("Continuous auromancy passed: actual height discovery events, resonator 50 vis/two crystals, gauntlet 100 vis/six crystals, consumption of the previously crafted thaumometer and first-focus entry.");
    }
    private static void verifyContinuousFocus(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);
        check(!ResearchProgression.advance(state,"BASEAUROMANCY",1,cost->cost.isEmpty()),"First focus research skipped blank focus");
        var potRelative=new BlockPos(14,1,8);var pot=h.getBlockEntity(potRelative,CrucibleBlockEntity.class);
        art.arcane.thaumcraft.aspect.ObjectAspects.rebuild(world.getServer());
        var sacrificialRelative=new BlockPos(12,1,4);h.setBlock(sacrificialRelative,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);var sacrificialPos=h.absolutePos(sacrificialRelative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(sacrificialPos));var heldBeforeConversion=player.getMainHandItem();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.SALIS_MUNDUS.get()));if(!heldBeforeConversion.isEmpty())check(player.getInventory().add(heldBeforeConversion),"Could not preserve carried gauntlet before conversion");
        var conversion=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(sacrificialPos),net.minecraft.core.Direction.UP,sacrificialPos,false));
        check(ModItems.SALIS_MUNDUS.get().useOn(conversion)==net.minecraft.world.InteractionResult.SUCCESS&&player.getMainHandItem().isEmpty(),"Sacrificial workbench conversion failed");art.arcane.thaumcraft.progression.DustSwaps.advance(art.arcane.thaumcraft.progression.DustSwaps.DELAY_TICKS);check(world.getBlockState(sacrificialPos).is(ModBlocks.ARCANE_WORKBENCH.get()),"Sacrificial workbench did not convert after the delay");
        check(world.destroyBlock(sacrificialPos,true,player),"Sacrificial workbench could not be harvested");
        var recovered=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(sacrificialPos).inflate(0.1),entity->entity.getItem().is(ModItems.BLOCK_ARCANE_WORKBENCH.get()));
        if(recovered.size()!=1){
            var nearby=new java.util.ArrayList<String>();
            for(var entity:world.getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity drop && drop.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(sacrificialPos))<64)
                nearby.add(drop.getItem()+" at "+drop.position()+" removed="+drop.isRemoved());
            System.err.println("Workbench harvest diagnostics: pos="+sacrificialPos+", blockDrops="+world.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS)+", nearby="+nearby);
        }
        check(recovered.size()==1,"Harvested arcane workbench did not drop one block item: "+recovered.stream().map(entity->entity.getItem().toString()+" at "+entity.position()).toList());var benchIngredient=recovered.getFirst().getItem().copy();recovered.getFirst().discard();
        check(pot.processOne(benchIngredient,player)&&pot.processOne(new ItemStack(ModItems.SALIS_MUNDUS.get()),player),"Real workbench/Salis could not supply Aura/Magic");
        for(int glass=0;glass<4;glass++)check(pot.processOne(new ItemStack(Items.GLASS),player),"Glass dissolution failed");
        check(pot.mixture().aspects().getOrDefault("vitreus",0)==20&&pot.mixture().aspects().getOrDefault("praecantatio",0)==10&&pot.mixture().aspects().getOrDefault("auram",0)==5,"Actual blank focus ingredients supplied incorrect costs");
        check(pot.processOne(CrystalEssenceItem.of("ordo",1),player)&&state.crafted("thaumcraft:focus_1")&&pot.mixture().water()==900&&pot.mixture().total()==39,"Continuous blank focus recipe/cost/residual aspects failed");
        
        var outputs=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(potRelative)).inflate(2),entity->entity.getItem().is(ModItems.FOCUS_1.get()));
        check(outputs.size()==1,"Blank focus did not produce exactly one physical output");var blank=outputs.getFirst().getItem().copy();outputs.getFirst().discard();
        check(ResearchProgression.advance(state,"BASEAUROMANCY",1,cost->cost.isEmpty()),"Actual blank focus did not advance research");
        fire(player,world.damageSources().onFire());check(state.has("f_onfire")&&!ResearchProgression.advance(state,"BASEAUROMANCY",2,cost->cost.isEmpty()),"Manipulator requirement bypassed");
        var benchRelative=new BlockPos(14,1,4);var bench=h.getBlockEntity(benchRelative,ArcaneWorkbenchBlockEntity.class);var benchPos=h.absolutePos(benchRelative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(benchPos));bench.clearContent();
        var aura=NativeAura.get(world,world.getChunkAt(benchPos));aura.spend(aura.vis());aura.addVis(50);
        bench.setItem(0,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(1,new ItemStack(Items.QUARTZ));bench.setItem(9,CrystalEssenceItem.of("aer",1));bench.setItem(12,CrystalEssenceItem.of("aqua",1));
        var resonator=bench.takeOutput(player,1);check(resonator.is(ModItems.VIS_RESONATOR.get())&&aura.vis()==0,"Second resonator for manipulator failed");bench.clearContent();
        var stone=new ItemStack(Items.STONE);var stones=craft(h,3,3,List.of(stone,stone,stone,stone,CrystalEssenceItem.of("ignis",1),stone,stone,stone,stone));
        var slabs=craft(h,3,1,List.of(stones,stones,stones));var slab=new ItemStack(Items.SMOOTH_STONE_SLAB);var pedestal=craft(h,3,2,List.of(slab,slab,slab,stone,ItemStack.EMPTY,stone));
        bench.setItem(0,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(1,slabs.copyWithCount(1));bench.setItem(2,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(3,stones.copyWithCount(1));bench.setItem(4,resonator);bench.setItem(5,stones.copyWithCount(1));bench.setItem(6,new ItemStack(Items.GOLD_INGOT));bench.setItem(7,pedestal);bench.setItem(8,new ItemStack(Items.GOLD_INGOT));bench.setItem(10,CrystalEssenceItem.of("terra",1));bench.setItem(12,CrystalEssenceItem.of("aqua",1));aura.addVis(100);
        var manipulator=bench.takeOutput(player,1);check(manipulator.is(ModItems.BLOCK_WAND_WORKBENCH.get())&&aura.vis()==0&&ResearchProgression.advance(state,"BASEAUROMANCY",2,cost->cost.isEmpty())&&ResearchRules.complete(state,"BASEAUROMANCY"),"Continuous manipulator craft/research failed");
        System.out.println("Continuous first focus passed: physical crucible output, fire discovery and crafted manipulator.");
    }
    private static void verifyContinuousTheory(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);var relative=new BlockPos(10,1,12);h.setBlock(relative,ModBlocks.TABLE_WOOD.get());var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        var tools=craft(h,2,2,List.of(new ItemStack(Items.GLASS_BOTTLE),new ItemStack(Items.FEATHER),new ItemStack(Items.INK_SAC),ItemStack.EMPTY));check(tools.is(ModItems.SCRIBING_TOOLS.get()),"Continuous writing tools recipe failed");
        player.getInventory().add(player.getMainHandItem().copy());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tools);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));ModItems.SCRIBING_TOOLS.get().useOn(context);
        var table=h.getBlockEntity(relative,ResearchTableBlockEntity.class);check(player.getMainHandItem().isEmpty()&&state.crafted("thaumcraft:research_table"),"Continuous table formation lost tools or craft history");table.setItem(1,new ItemStack(Items.PAPER,64));
        player.giveExperienceLevels(10);for(var aspect:art.arcane.thaumcraft.aspect.AspectRegistry.all().values())if(!aspect.isPrimal())check(player.getInventory().add(CrystalEssenceItem.of(aspect.id(),8)),"Compound crystal fixture exceeded player inventory");player.getRandom().setSeed(38);
        var menu=(art.arcane.thaumcraft.menu.ResearchTableMenu)table.createMenu(10,player.getInventory(),player);check(menu.clickMenuButton(player,menu.button(0)),"Same player could not start theory");int draws=0,choices=0;
        for(int step=0;step<200&&!table.session().complete();step++){
            if(table.session().cards().isEmpty()){check(menu.clickMenuButton(player,menu.button(2)),"Continuous theory draw deadlocked");draws++;}
            boolean selected=false;for(int card=0;card<table.session().cards().size()&&!selected;card++)selected=menu.clickMenuButton(player,menu.button(4+card));check(selected,"Continuous theory offered no payable card");choices++;
        }
        check(table.session().complete()&&draws>0&&choices>0&&table.getItem(1).getCount()==64-draws&&table.getItem(0).getDamageValue()==choices,"Continuous theory completion/paper/ink cost failed");
        var rewards=table.session().rewards();var before=new HashMap<String,Integer>();rewards.forEach((category,raw)->before.put(category,state.knowledge("THEORY",category)));int claim=menu.button(7);
        check(menu.clickMenuButton(player,claim)&&table.session()==null&&!menu.clickMenuButton(player,claim),"Continuous theory claim missing or duplicated");rewards.forEach((category,raw)->check(state.knowledge("THEORY",category)==before.get(category)+raw,"Continuous theory reward differs from original conversion"));
        System.out.println("Continuous research passed: crafted writing tools, table formation, owned menu session, supplied compound crystals/experience, paper and ink costs, completion and exactly-once original theory rewards.");
    }
    private static void verifyAccessibleFocusCrystal(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);
        check(ResearchProgression.advance(state,"BASEALCHEMY",-1,cost->cost.isEmpty())&&ResearchRules.complete(state,"BASEALCHEMY"),"Base alchemy inaccessible after introduction");
        art.arcane.thaumcraft.aspect.ObjectAspects.rebuild(world.getServer());java.util.function.Function<String,Map<String,Integer>> active=VerifyAspects::of;
        check(active.apply("minecraft:arrow").equals(Map.of("terra",1,"instrumentum",1,"aer",1,"volatus",1,"aversio",5)),"Arrow recipe-derived tags/TC6 Aversion addition incorrect: "+active.apply("minecraft:arrow"));
        var arrows=craft(h,1,3,List.of(new ItemStack(Items.FLINT),new ItemStack(Items.STICK),new ItemStack(Items.FEATHER)));check(arrows.is(Items.ARROW)&&arrows.getCount()==4,"Accessible arrow recipe missing");
        var nuggets=craft(h,1,1,List.of(new ItemStack(Items.QUARTZ)));check(nuggets.is(ModItems.NUGGET_QUARTZ.get())&&nuggets.getCount()==9,"Quartz crystal catalyst recipe missing");
        var relative=new BlockPos(10,1,4);h.setBlock(relative,ModBlocks.CRUCIBLE.get());var pot=h.getBlockEntity(relative,CrucibleBlockEntity.class);pot.mixture().fill();for(int tick=0;tick<151;tick++)pot.mixture().heatTick(true);
        check(pot.processOne(arrows.copyWithCount(1),player)&&pot.mixture().aspects().getOrDefault("aversio",0)==5,"Arrow failed real Aversion dissolution");
        check(pot.processOne(nuggets.copyWithCount(1),player)&&pot.mixture().aspects().getOrDefault("aversio",0)==3&&pot.mixture().water()==950,"Aversion crystallization original cost incorrect");
        var outputs=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(relative)).inflate(2),entity->entity.getItem().is(ModItems.CRYSTAL_ESSENCE.get())&&CrystalEssenceItem.aspect(entity.getItem()).equals("aversio"));
        check(outputs.size()==1&&outputs.getFirst().getItem().getCount()==1,"Aversion catalyst did not produce one physical crystal: "+outputs.stream().map(entity->entity.getItem().toString()+" at "+entity.position()).toList());check(player.getInventory().add(outputs.getFirst().getItem().copy()),"Produced Aversion crystal could not enter inventory");outputs.getFirst().discard();
        var fireRelative=new BlockPos(10,1,8);h.setBlock(fireRelative,ModBlocks.CRUCIBLE.get());var firePot=h.getBlockEntity(fireRelative,CrucibleBlockEntity.class);firePot.mixture().fill();for(int tick=0;tick<151;tick++)firePot.mixture().heatTick(true);
        check(firePot.processOne(new ItemStack(Items.COAL),player),"Coal could not supply first-focus Fire");
        for(int catalyst=0;catalyst<9;catalyst++)check(firePot.processOne(nuggets.copyWithCount(1),player),"Coal crystallization failed before ninth catalyst");
        check(firePot.mixture().water()==550&&firePot.mixture().total()==2,"Coal crystallization did not retain two aspects/pay nine times");
        var fireOutputs=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(fireRelative)).inflate(2),entity->entity.getItem().is(ModItems.CRYSTAL_ESSENCE.get()));
        check(fireOutputs.size()==9&&fireOutputs.stream().anyMatch(entity->CrystalEssenceItem.aspect(entity.getItem()).equals("ignis")),"Coal/quartz did not produce physical Fire crystals");
        for(var entity:fireOutputs){check(player.getInventory().add(entity.getItem().copy()),"Could not collect coal crystal outputs");entity.discard();}
        System.out.println("Accessible first-focus Fire passed: actual coal dissolution, nine loaded quartz-nugget catalysts, nine physical crystal outputs including Ignis, 450mB paid and two residual aspects; produced Ignis is retained for configuration.");
        
        System.out.println("Accessible first-focus Aversion passed: original arrow complex tags, loaded flint/stick/feather recipe, quartz nuggets, actual crucible dissolution and one crystal for two Aversion/50mB with three Aversion retained.");
    }
    private static void verifyContinuousMetallurgy(GameTestHelper h,ServerPlayer player){
        var world=h.getLevel();var state=PlayerProgressManager.get(player);check(ResearchProgression.advance(state,"METALLURGY",-1,cost->cost.isEmpty()),"Metallurgy did not open after base alchemy");
        var relative=new BlockPos(8,1,12);h.setBlock(relative,ModBlocks.CRUCIBLE.get());var pot=h.getBlockEntity(relative,CrucibleBlockEntity.class);pot.mixture().fill();for(int tick=0;tick<151;tick++)pot.mixture().heatTick(true);
        check(pot.processOne(new ItemStack(ModItems.SALIS_MUNDUS.get()),player)&&pot.processOne(new ItemStack(Items.STONE),player),"Thaumium source dissolution failed");
        check(pot.processOne(new ItemStack(Items.IRON_INGOT),player)&&!state.crafted("thaumcraft:ingot_thaumium")&&pot.mixture().water()==1000&&pot.mixture().aspects().getOrDefault("praecantatio",0)==5,"Thaumium bypassed metallurgy stage2");
        check(pot.processOne(new ItemStack(Items.FLINT),player)&&pot.processOne(new ItemStack(Items.IRON_INGOT),player)&&state.crafted("thaumcraft:ingot_brass")&&pot.mixture().water()==950,"Original brass recipe/cost/history failed");
        int observation=state.knowledge("OBSERVATION","ALCHEMY");check(ResearchProgression.advance(state,"METALLURGY",1,cost->cost.isEmpty())&&state.stage("METALLURGY")==2&&state.knowledge("OBSERVATION","ALCHEMY")==observation-16,"Brass did not advance metallurgy with observation cost");
        check(pot.processOne(new ItemStack(Items.IRON_INGOT),player)&&state.crafted("thaumcraft:ingot_thaumium")&&pot.mixture().water()==900&&!pot.mixture().aspects().containsKey("praecantatio")&&pot.mixture().aspects().getOrDefault("terra",0)==5,"Thaumium did not pay original Magic5/Earth5");
        var outputs=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(relative)).inflate(2),entity->entity.getItem().is(ModItems.INGOT_THAUMIUM.get()));check(outputs.size()==1&&outputs.getFirst().getItem().getCount()==1,"Thaumium did not yield one physical ingot");outputs.getFirst().discard();
        System.out.println("Continuous metallurgy passed: no direct research grants, early thaumium rejection, real Flint/iron brass, paid observation stage, Salis/stone thaumium ingredients, exact five Magic/five Earth/50mB and physical ingot; theory completion remains separate.");
    }
    private static void verifyThaumiumSmelterUpgrade(GameTestHelper h){
        var world=h.getLevel();var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var relative=new BlockPos(8,1,8);h.setBlock(relative,ModBlocks.ARCANE_WORKBENCH.get());var bench=h.getBlockEntity(relative,ArcaneWorkbenchBlockEntity.class);var pos=h.absolutePos(relative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        bench.setItem(0,new ItemStack(ModItems.PLATE_BRASS.get()));bench.setItem(2,new ItemStack(ModItems.PLATE_BRASS.get()));bench.setItem(1,new ItemStack(ModItems.BLOCK_SMELTER_BASIC.get()));bench.setItem(4,new ItemStack(ModItems.BLOCK_METAL_ALCHEMICAL.get()));for(int slot:new int[]{3,5,6,7,8})bench.setItem(slot,new ItemStack(ModItems.PLATE_THAUMIUM.get()));bench.setItem(11,CrystalEssenceItem.of("ignis",2));
        var aura=NativeAura.get(world,world.getChunkAt(pos));aura.spend(aura.vis());aura.addVis(250);
        check(bench.preview(player).isEmpty()&&bench.takeOutput(player,1).isEmpty()&&aura.vis()==250&&bench.getItem(11).getCount()==2,"Thaumium smelter upgrade bypassed research or charged failure");
        var state=PlayerProgressManager.get(player);state.unlock("ESSENTIASMELTERTHAUMIUM");state.setStage("ESSENTIASMELTERTHAUMIUM",3);
        aura.spend(1);check(bench.takeOutput(player,1).isEmpty()&&aura.vis()==249&&bench.getItem(11).getCount()==2,"Insufficient upgrade aura consumed ingredients");aura.addVis(1);
        var result=bench.takeOutput(player,1);check(result.is(ModItems.BLOCK_SMELTER_THAUMIUM.get())&&result.getCount()==1&&aura.vis()==0&&state.crafted("thaumcraft:smelter_thaumium"),"Thaumium upgrade did not charge 250 vis or record crafting");
        for(int slot=0;slot<15;slot++)check(bench.getItem(slot).isEmpty(),"Thaumium smelter retained upgrade ingredient/crystal: "+slot);
        bench.clearContent();bench.setItem(0,new ItemStack(ModItems.BLOCK_PLANK_GREATWOOD.get()));bench.setItem(2,new ItemStack(ModItems.BLOCK_PLANK_GREATWOOD.get()));bench.setItem(1,new ItemStack(ModItems.BLOCK_TUBE_FILTER.get()));bench.setItem(3,new ItemStack(ModItems.PLATE_BRASS.get()));bench.setItem(5,new ItemStack(ModItems.PLATE_BRASS.get()));bench.setItem(4,new ItemStack(ModItems.BLOCK_METAL_ALCHEMICAL.get()));bench.setItem(6,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(8,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(7,new ItemStack(ModItems.BLOCK_BELLOWS.get()));bench.setItem(9,CrystalEssenceItem.of("aer",1));bench.setItem(10,CrystalEssenceItem.of("terra",1));aura.addVis(100);
        check(bench.takeOutput(player,1).isEmpty()&&aura.vis()==100,"Auxiliary recipe bypassed research gate");state.unlock("IMPROVEDSMELTING");state.setStage("IMPROVEDSMELTING",3);
        check(bench.takeOutput(player,1).is(ModItems.BLOCK_SMELTER_AUX.get())&&aura.vis()==0&&state.crafted("thaumcraft:smelter_aux"),"Auxiliary original 100-vis recipe failed");for(int slot=0;slot<15;slot++)check(bench.getItem(slot).isEmpty(),"Auxiliary recipe retained paid ingredient/crystal");
        System.out.println("Auxiliary smelter crafting passed: separate completed-research fixture, research gate, original recipe materials, 100 vis, Aer/Terra crystals and exact consumption.");
        System.out.println("Thaumium smelter upgrade recipe passed: isolated researched-player fixture, research gate, insufficient aura atomicity, original 250 vis/two Fire crystals, two brass/five thaumium plates, basic smelter/alchemical construct and craft history.");
    }
    private static void verifySmelterBellows(GameTestHelper h){
        var world=h.getLevel();var relative=new BlockPos(6,1,12);h.setBlock(relative,ModBlocks.SMELTER_BASIC.get());var smelter=h.getBlockEntity(relative,EssentiaSmelterBlockEntity.class);var pos=h.absolutePos(relative);var facing=smelter.getBlockState().getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.FACING);
        smelter.setItem(0,new ItemStack(Items.COAL));smelter.setItem(1,new ItemStack(ModItems.ALUMENTUM.get()));
        for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL)world.setBlockAndUpdate(pos.relative(side),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        check(smelter.cookDuration()==40&&smelter.bellowsCount()==0,"Baseline coal smelting duration changed");
        world.setBlockAndUpdate(pos.relative(facing),ModBlocks.BELLOWS.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING,facing.getOpposite()));check(smelter.bellowsCount()==0,"Front bellows sped up smelter");
        int count=0;for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL){if(side==facing)continue;world.setBlockAndUpdate(pos.relative(side),ModBlocks.BELLOWS.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING,side.getOpposite()));count++;check(smelter.bellowsCount()==count&&smelter.cookDuration()==40-count*5,"Original bellows speed reduction incorrect");}
        var rear=pos.relative(facing.getOpposite());world.setBlockAndUpdate(rear,ModBlocks.BELLOWS.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING,facing.getOpposite()));check(smelter.bellowsCount()==2&&smelter.cookDuration()==30,"Misaligned bellows counted");
        world.setBlockAndUpdate(rear,ModBlocks.BELLOWS.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING,facing));world.setBlockAndUpdate(rear.above(),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());check(smelter.bellowsCount()==2,"Powered bellows counted");world.setBlockAndUpdate(rear.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var aura=NativeAura.get(world,world.getChunkAt(pos));float before=aura.flux();world.getRandom().setSeed(0);
        for(int tick=0;tick<24;tick++)EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);check(!smelter.input().isEmpty(),"Three bellows cooked coal too early");
        EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);check(smelter.input().isEmpty()&&Math.abs(smelter.total()+aura.flux()-before-20)<0.001,"Bellows changed aspect conservation or failed 25-tick coal cooking");
        for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL)world.setBlockAndUpdate(pos.relative(side),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        System.out.println("Smelter bellows passed: front excluded, zero/one/two/three timings 40/35/30/25, orientation/redstone gates, live 25-tick distillation and 20-aspect conservation including flux.");
    }
    private static void verifyAuxiliaryOutlets(GameTestHelper h){
        var world=h.getLevel();var relative=new BlockPos(6,1,8);h.setBlock(relative,ModBlocks.SMELTER_BASIC.get());var smelter=h.getBlockEntity(relative,EssentiaSmelterBlockEntity.class);var pos=h.absolutePos(relative);var front=smelter.getBlockState().getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.FACING);
        var jars=new ArrayList<EssentiaJarBlockEntity>();world.setBlockAndUpdate(pos.above(),ModBlocks.ALEMBIC.get().defaultBlockState());jars.add((EssentiaJarBlockEntity)world.getBlockEntity(pos.above()));
        for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL){var outlet=pos.relative(side);world.setBlockAndUpdate(outlet,ModBlocks.SMELTER_AUX.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.SmelterAuxBlock.FACING,side.getOpposite()));world.setBlockAndUpdate(outlet.above(),ModBlocks.ALEMBIC.get().defaultBlockState());if(side!=front)jars.add((EssentiaJarBlockEntity)world.getBlockEntity(outlet.above()));}
        smelter.setItem(0,new ItemStack(Items.COAL));smelter.setItem(1,new ItemStack(ModItems.ALUMENTUM.get()));for(int tick=0;tick<40;tick++)EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);
        int before=smelter.total();var timeData=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();long previousTime=world.getGameTime();try{timeData.setGameTime(previousTime+Math.floorMod(-previousTime,15));EssentiaSmelterBlockEntity.tick(world,pos,smelter.getBlockState(),smelter);}finally{timeData.setGameTime(previousTime);}
        int delivered=jars.stream().mapToInt(jar->jar.contents().amount()).sum();check(delivered>0&&delivered==before-smelter.total(),"Auxiliary outlets duplicated/lost aspects");for(var jar:jars)check(jar.contents().amount()>0,"An auxiliary/main column did not receive essentia");
        check(((EssentiaJarBlockEntity)world.getBlockEntity(pos.relative(front).above())).contents().amount()==0,"Front accessory exported essentia");
        for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL){world.setBlockAndUpdate(pos.relative(side).above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());world.setBlockAndUpdate(pos.relative(side),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}world.setBlockAndUpdate(pos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        System.out.println("Auxiliary smelter outlets passed: main plus three oriented non-front alembic columns, physical transfer and exact conservation; front outlet rejected.");
    }
    private static void verifyContinuousArtificeEntry(GameTestHelper h,ServerPlayer player){
        var state=PlayerProgressManager.get(player);check(ResearchProgression.advance(state,"UNLOCKARTIFICE",-1,cost->cost.isEmpty()),"Artifice entry blocked after actual metallurgy stage2");
        check(!state.has("!machina")&&!ResearchProgression.advance(state,"UNLOCKARTIFICE",1,cost->cost.isEmpty()),"Artifice bypassed Machine discovery");
        art.arcane.thaumcraft.aspect.ObjectAspects.rebuild(h.getLevel().getServer());java.util.function.Function<String,Map<String,Integer>> active=VerifyAspects::of;check(active.apply("minecraft:lever").equals(Map.of("terra",3,"machina",5)),"Lever original mechanism addition/base recipe tags wrong: "+active.apply("minecraft:lever"));
        check(active.apply("minecraft:piston").equals(Map.of("herba",6,"terra",15,"perditio",3,"metallum",11,"potentia",7,"machina",10,"motus",10)),"Piston original mechanism/motion additions or recipe tags wrong: "+active.apply("minecraft:piston"));
        var lever=craft(h,1,2,List.of(new ItemStack(Items.STICK),new ItemStack(Items.COBBLESTONE)));check(lever.is(Items.LEVER),"Lever loaded recipe failed");
        check(art.arcane.thaumcraft.scan.ScanProgression.record(state,"minecraft:lever",art.arcane.thaumcraft.scan.ScanRegistry.get("minecraft:lever"))&&state.has("!machina"),"Actual lever scan rewards missed Machine discovery");
        int observations=state.knowledge("OBSERVATION","ARTIFICE");check(ResearchProgression.advance(state,"UNLOCKARTIFICE",1,cost->cost.isEmpty())&&ResearchRules.complete(state,"UNLOCKARTIFICE")&&state.knowledge("OBSERVATION","ARTIFICE")==observations-16,"Artifice observation cost/entry did not complete");
        var benchRelative=new BlockPos(14,1,4);var bench=h.getBlockEntity(benchRelative,ArcaneWorkbenchBlockEntity.class);var pos=h.absolutePos(benchRelative);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));bench.clearContent();for(int slot:new int[]{0,2,3,5})bench.setItem(slot,new ItemStack(Items.LEATHER));for(int slot:new int[]{1,7})bench.setItem(slot,new ItemStack(ModItems.INGOT_BRASS.get()));for(int slot:new int[]{6,8})bench.setItem(slot,new ItemStack(ModItems.THAUMOMETER.get()));
        var aura=NativeAura.get(h.getLevel(),h.getLevel().getChunkAt(pos));aura.spend(aura.vis());aura.addVis(50);var goggles=bench.takeOutput(player,1);check(goggles.is(ModItems.GOGGLES.get())&&aura.vis()==0&&goggles.getMaxDamage()==350&&goggles.getMaxStackSize()==1,"Original goggles recipe/durability failed");for(int slot=0;slot<15;slot++)check(bench.getItem(slot).isEmpty(),"Goggles retained original ingredients");
        check(goggles.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot()==net.minecraft.world.entity.EquipmentSlot.HEAD&&goggles.get(net.minecraft.core.component.DataComponents.REPAIRABLE).isValidRepairItem(new ItemStack(ModItems.INGOT_BRASS.get()))&&!goggles.get(net.minecraft.core.component.DataComponents.REPAIRABLE).isValidRepairItem(new ItemStack(Items.IRON_INGOT)),"Goggles equipment/repair material incorrect");player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,goggles);check(Math.abs(art.arcane.thaumcraft.casters.CasterManager.getTotalVisDiscount(player)-0.05F)<0.0001,"Equipped goggles missing original five percent discount");
        System.out.println("Goggles passed: actual unlocked-artifice player, original recipe 50 vis/no crystals/two thaumometers, native head equipment, 350 durability, brass repair and five percent casting discount.");
        System.out.println("Continuous artifice entry passed: actual metallurgy parent, missing Machine gate, original lever/piston recipe aspects, crafted lever scan discovery and paid 16 Artifice observations.");
    }
    public static void test(GameTestHelper h){
        var template=net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE));
        var low=new CrucibleRecipe(Ingredient.of(Items.QUARTZ),template,"",Map.of("aer",2),"");
        var high=new CrucibleRecipe(Ingredient.of(Items.QUARTZ),template,"",Map.of("aer",2,"ignis",5),"");
        var tie=new CrucibleRecipe(Ingredient.of(Items.QUARTZ),template,"",Map.of("terra",7),"");
        check(CrucibleRecipe.select(List.of(low,high,tie),recipe->true)==high,"Crucible did not select greatest aspect cost or preserve first tie");
        check(CrucibleRecipe.select(List.of(low,high,tie),recipe->recipe!=high&&recipe!=tie)==low&&CrucibleRecipe.select(List.of(low),recipe->false)==null,"Crucible priority bypassed eligibility or empty selection");
        art.arcane.thaumcraft.aspect.ObjectAspects.rebuild(h.getLevel().getServer());java.util.function.Function<String,Map<String,Integer>> nativeGraph=VerifyAspects::of;
        check(nativeGraph.apply("minecraft:iron_nugget").equals(Map.of("metallum",1)),"Native nine-nugget output recipe aspect calculation failed");
        check(nativeGraph.apply("minecraft:iron_ingot").equals(Map.of("metallum",15)),"Native graph overwrote explicit tags with reversible recipe");
        check(nativeGraph.apply("minecraft:oak_planks").equals(Map.of("herba",3)),"Real wood recipe lost original Plant20 log assignment");
        check(nativeGraph.apply("minecraft:paper").equals(Map.of("herba",3,"aqua",2,"aer",1,"cognitio",2)),"Real sugarcane paper base derivation incorrect");
        check(nativeGraph.apply("minecraft:crafting_table").equals(Map.of("herba",9,"fabrico",20)),"Crafting table complex addition missing");
        check(nativeGraph.apply("thaumcraft:arcane_workbench").equals(Map.of("herba",9,"fabrico",20,"praecantatio",5,"auram",5)),"Arcane workbench copied tags incorrectly scaled");
        art.arcane.thaumcraft.aspect.ObjectAspects.rebuild(h.getLevel().getServer());java.util.function.Function<String,Map<String,Integer>> active=VerifyAspects::of;
        check(EssentiaSmelterBlockEntity.tags(new ItemStack(ModItems.BLOCK_ARCANE_WORKBENCH.get())).getOrDefault("auram",0)==5,"Derived workbench aura not visible to smelter");
        check(active.apply("minecraft:book").equals(Map.of("herba",6,"aqua",4,"aer",2,"cognitio",4,"bestia",3,"praemunio",3)),"Book early-game aspects incorrect: "+active.apply("minecraft:book"));
        check(active.apply("minecraft:bucket").equals(Map.of("metallum",33,"vacuos",5)),"Bucket aspects incorrect: "+active.apply("minecraft:bucket"));
        check(active.apply("minecraft:glass_bottle").equals(Map.of("vitreus",3,"vacuos",5)),"Bottle output quantity or original Void addition wrong: "+active.apply("minecraft:glass_bottle"));
        check(active.apply("minecraft:bowl").equals(Map.of("herba",1,"vacuos",5)),"Bowl output quantity or original Void addition wrong: "+active.apply("minecraft:bowl"));
        check(active.apply("minecraft:chest").equals(Map.of("herba",18,"vacuos",15)),"Chest aspects incorrect: "+active.apply("minecraft:chest"));
        check(active.apply("minecraft:furnace").equals(Map.of("terra",30,"perditio",6,"ignis",10)),"Furnace aspects incorrect: "+active.apply("minecraft:furnace"));
        check(art.arcane.thaumcraft.scan.ScanRegistry.get("minecraft:paper").getOrDefault(art.arcane.thaumcraft.aspect.AspectRegistry.get("cognitio"),0)==2,"Derived paper mind not visible to scanning");
        var smelterRelative=new BlockPos(12,1,12);h.setBlock(smelterRelative,ModBlocks.SMELTER_BASIC.get());var derivedSmelter=h.getBlockEntity(smelterRelative,EssentiaSmelterBlockEntity.class);var smelterPos=h.absolutePos(smelterRelative);
        derivedSmelter.setItem(0,new ItemStack(ModItems.BLOCK_ARCANE_WORKBENCH.get()));derivedSmelter.setItem(1,new ItemStack(ModItems.ALUMENTUM.get()));
        var distillationAura=NativeAura.get(h.getLevel(),h.getLevel().getChunkAt(smelterPos));float fluxBefore=distillationAura.flux();h.getLevel().getRandom().setSeed(0);
        for(int tick=0;tick<78;tick++)EssentiaSmelterBlockEntity.tick(h.getLevel(),smelterPos,derivedSmelter.getBlockState(),derivedSmelter);
        var storedTags=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),derivedSmelter.saveWithoutMetadata(h.getLevel().registryAccess())).read("aspects",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.INT)).orElseThrow();
        check(derivedSmelter.input().isEmpty()&&storedTags.getOrDefault("auram",0)>0,"Live workbench distillation failed to produce Aura");
        check(Math.abs(derivedSmelter.total()+distillationAura.flux()-fluxBefore-39)<0.001,"Derived distillation did not conserve 39 aspects including flux");
        System.out.println("Derived workbench distillation passed: 78 cooking ticks, Aura output, exact 39-aspect conservation including flux.");
        var snapshot=art.arcane.thaumcraft.network.AspectSnapshotPayload.of(art.arcane.thaumcraft.aspect.ObjectAspects.export());var syncBuffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{art.arcane.thaumcraft.network.AspectSnapshotPayload.CODEC.encode(syncBuffer,snapshot);var decoded=art.arcane.thaumcraft.network.AspectSnapshotPayload.CODEC.decode(syncBuffer);var before=art.arcane.thaumcraft.aspect.ObjectAspects.export();art.arcane.thaumcraft.aspect.ObjectAspects.clear();art.arcane.thaumcraft.aspect.ObjectAspects.install(decoded.values());check(art.arcane.thaumcraft.aspect.ObjectAspects.export().equals(before),"Aspect payload changed the table");}finally{syncBuffer.release();}
        check(art.arcane.thaumcraft.scan.ScanRegistry.get("minecraft:iron_ingot").get(art.arcane.thaumcraft.aspect.AspectRegistry.METALLUM)==15,"Derived cleanup removed explicit tags");
        var beginningPlayer=verifyBeginning(h);
        verifyInitialAlchemy(h,beginningPlayer);
        verifyInitialAuromancy(h,beginningPlayer);
        verifyAccessibleFocusCrystal(h,beginningPlayer);
        verifyContinuousFocus(h,beginningPlayer);
        verifyContinuousTheory(h,beginningPlayer);
        verifyContinuousMetallurgy(h,beginningPlayer);
        verifyContinuousArtificeEntry(h,beginningPlayer);
        verifyThaumiumSmelterUpgrade(h);
        verifySmelterBellows(h);
        verifyAuxiliaryOutlets(h);
        var world=h.getLevel();var stone=new ItemStack(Items.STONE);var crystal=CrystalEssenceItem.of("ignis",1);
        var arcane=craft(h,3,3,List.of(stone,stone,stone,stone,crystal,stone,stone,stone,stone));
        check(arcane.is(ModItems.BLOCK_STONE_ARCANE.get())&&arcane.getCount()==9,"Arcane stone base recipe lost original output or crystal variants");
        var slabs=craft(h,3,1,List.of(arcane,arcane,arcane));check(slabs.is(ModItems.BLOCK_SLAB_ARCANE_STONE.get())&&slabs.getCount()==6,"Arcane slab recipe missing");
        var smoothSlab=new ItemStack(Items.SMOOTH_STONE_SLAB);
        var table=craft(h,3,2,List.of(smoothSlab,smoothSlab,smoothSlab,stone,ItemStack.EMPTY,stone));check(table.is(ModItems.BLOCK_TABLE_STONE.get())&&table.getCount()==1,"Original stone table recipe missing");
        check(new ItemStack(ModItems.FOCUS_1.get()).getMaxStackSize()==1&&new ItemStack(ModItems.CASTER_BASIC.get()).getMaxStackSize()==1,"Focus or gauntlet could stack");
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var research=PlayerProgressManager.get(player);research.unlock("UNLOCKAUROMANCY");research.setStage("UNLOCKAUROMANCY",4);
        check(ResearchProgression.advance(research,"BASEAUROMANCY",-1,cost->cost.isEmpty())&&research.stage("BASEAUROMANCY")==1,"Auromancy intro could not start");
        fire(player,world.damageSources().onFire());check(!research.has("f_onfire"),"Fire discovery bypassed the focus prerequisite");
        var benchPos=new BlockPos(2,1,2);h.setBlock(benchPos,ModBlocks.ARCANE_WORKBENCH.get());var bench=h.getBlockEntity(benchPos,ArcaneWorkbenchBlockEntity.class);var absolute=h.absolutePos(benchPos);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(absolute));
        var aura=NativeAura.get(world,world.getChunkAt(absolute));aura.spend(aura.vis());aura.addVis(100);
        bench.setItem(0,new ItemStack(ModItems.PLATE_IRON.get()));bench.setItem(1,slabs.copyWithCount(1));bench.setItem(2,new ItemStack(ModItems.PLATE_IRON.get()));
        bench.setItem(3,arcane.copyWithCount(1));bench.setItem(4,new ItemStack(ModItems.VIS_RESONATOR.get()));bench.setItem(5,arcane.copyWithCount(1));
        bench.setItem(6,new ItemStack(Items.GOLD_INGOT));bench.setItem(7,table);bench.setItem(8,new ItemStack(Items.GOLD_INGOT));
        bench.setItem(10,CrystalEssenceItem.of("terra",1));bench.setItem(12,CrystalEssenceItem.of("aqua",1));
        check(bench.preview(player).isEmpty()&&bench.takeOutput(player,1).isEmpty()&&aura.vis()==100,"Manipulator recipe bypassed research or charged a failed craft");
        var focus=world.getServer().getRecipeManager().recipeMap().byType(ModRecipes.CRUCIBLE_TYPE.get()).stream().map(holder->holder.value()).filter(recipe->recipe.result().create().is(ModItems.FOCUS_1.get())).findFirst().orElseThrow();
        check(focus.matches(new SingleRecipeInput(CrystalEssenceItem.of("ordo",1)),world)&&!focus.matches(new SingleRecipeInput(CrystalEssenceItem.of("aer",1)),world),"Blank focus accepted the wrong catalyst aspect");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),world.registryAccess());
        CrucibleRecipe.STREAM_CODEC.encode(buffer,focus);var decoded=CrucibleRecipe.STREAM_CODEC.decode(buffer);check(decoded.catalystAspect().equals("ordo")&&!decoded.matches(new SingleRecipeInput(crystal),world),"Network recipe lost its component catalyst requirement");buffer.release();
        var potPos=new BlockPos(5,1,2);h.setBlock(potPos,ModBlocks.CRUCIBLE.get());var pot=h.getBlockEntity(potPos,CrucibleBlockEntity.class);pot.mixture().fill();for(int i=0;i<151;i++)pot.mixture().heatTick(true);
        pot.mixture().dissolve(Map.of("vitreus",20,"praecantatio",10,"auram",5));
        check(pot.processOne(CrystalEssenceItem.of("ordo",1),player)&&pot.mixture().total()==0&&pot.mixture().water()==950&&research.crafted("thaumcraft:focus_1"),"Blank focus did not charge original crucible costs or record crafting");
        check(ResearchProgression.advance(research,"BASEAUROMANCY",1,cost->cost.isEmpty())&&research.stage("BASEAUROMANCY")==2,"Blank focus did not advance the original book stage");
        fire(player,world.damageSources().generic());check(!research.has("f_onfire"),"Non-fire damage granted fire discovery");
        fire(player,world.damageSources().onFire());check(research.has("f_onfire"),"Fire damage did not unlock the real second-stage discovery");
        int discoveries=research.researched().size();fire(player,world.damageSources().onFire());check(research.researched().size()==discoveries,"Repeated fire duplicated a discovery");
        check(!ResearchProgression.advance(research,"BASEAUROMANCY",2,cost->cost.isEmpty()),"Fire alone bypassed manipulator crafting");
        var result=bench.takeOutput(player,1);check(result.is(ModItems.BLOCK_WAND_WORKBENCH.get())&&aura.vis()==0&&bench.getItem(10).isEmpty()&&bench.getItem(12).isEmpty()&&research.crafted("thaumcraft:wand_workbench"),"Manipulator craft did not charge exactly 100 vis and two original crystals");
        check(ResearchProgression.advance(research,"BASEAUROMANCY",2,cost->cost.isEmpty())&&ResearchRules.complete(research,"BASEAUROMANCY"),"Original basic auromancy intro remained blocked");
        System.out.println("Early focus prerequisites passed: arcane stone/slabs/table recipes, aspect-specific blank focus, recipe network codec, fire discovery gate, exact manipulator costs and completion of original BASEAUROMANCY stages.");h.succeed();
    }
}



