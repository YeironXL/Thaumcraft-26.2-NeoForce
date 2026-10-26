package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.*;
import art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity;
import art.arcane.thaumcraft.menu.ResearchTableMenu;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import java.util.*;

public final class ResearchTableVerification {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void verifyCardRules(){
        for(int kind=0;kind<9;kind++){
            int category=kind==8?0:2,amount=kind==4?5:kind==8?7:30,second=kind==8?2:1;
            String text="{\"owner\":\"tester\",\"start\":5,\"inspiration\":3,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"totals\":{\"ALCHEMY\":40,\"AUROMANCY\":20,\"BASICS\":10},\"blocked\":[],\"cards\":[{\"kind\":"+kind+",\"category\":"+category+",\"amount\":"+amount+",\"second\":"+second+"}],\"revision\":0,\"aid_pool\":[]}";
            var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();
            var state=new PlayerResearch();state.addKnowledge("OBSERVATION","ALCHEMY",16);
            check(session.choose(0,state),"Original basic card failed: "+kind);
            var totals=session.totals();
            switch(kind){
                case 0->check(totals.get("ALCHEMY")==70&&totals.get("BASICS")==11,"Experimentation effects changed");
                case 1->check(totals.get("ALCHEMY")==70&&totals.get("BASICS")==15&&state.knowledge("OBSERVATION","ALCHEMY")==0,"Analyze observation cost changed");
                case 2->check(totals.get("ALCHEMY")==49&&totals.get("AUROMANCY")==28&&totals.get("BASICS")==23&&session.bonus()==1,"Ponder distribution changed");
                case 3->check(totals.get("ALCHEMY")==70,"Inspired effect changed");
                case 4->check(totals.values().stream().mapToInt(Integer::intValue).sum()==65&&session.inspiration()==4&&session.bonus()==1,"Rethink recovery changed");
                case 5->check((session.blockedMask()&4)!=0&&totals.get("BASICS")==15,"Reject blocking changed");
                case 6->check(totals.get("ALCHEMY")==23&&totals.get("AUROMANCY")==23&&totals.get("BASICS")==28,"Balance distribution changed");
                case 7->check(totals.get("ALCHEMY")==70,"Study effect changed");
                case 8->check(!totals.containsKey("BASICS")&&totals.get("ALCHEMY")==47,"Notation transfer changed");
            }
        }
    }
    private static void verifyTinker(GameTestHelper h){
        art.arcane.thaumcraft.scan.DerivedAspectEvents.rebuild(h.getLevel().getServer());check(TinkerOptions.minimum(10)==14,"Original Tinker piston reward minimum changed");
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var resources=TheoryResources.player(player);int category=ResearchCatalog.CATEGORIES.indexOf("ARTIFICE");
        String text="{\"owner\":\"tester\",\"start\":5,\"inspiration\":2,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"totals\":{},\"blocked\":[],\"cards\":[{\"kind\":15,\"category\":"+category+",\"amount\":14,\"second\":10}],\"revision\":0,\"aid_pool\":[]}";
        var missing=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();check(!missing.choose(0,new PlayerResearch(),resources)&&missing.inspiration()==2&&missing.totals().isEmpty(),"Missing Tinker item paid inspiration/reward");player.getInventory().add(new ItemStack(Items.PISTON));
        for(int seed=0;seed<50;seed++){var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();var persisted=TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow();session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,persisted).getOrThrow();check(session.cards().getFirst().second()==10,"Tinker persistence changed selected item");player.getRandom().setSeed(seed);check(session.choose(0,new PlayerResearch(),resources)&&session.inspiration()==1&&player.getInventory().countItem(Items.PISTON)==1,"Tinker consumed required item or wrong inspiration");int gained=session.totals().getOrDefault("ARTIFICE",0);check(gained>=14&&gained<=24&&!session.choose(0,new PlayerResearch(),resources),"Tinker reward out of original range/repeated activation");}
        var locked=new PlayerResearch();var unlocked=new PlayerResearch();unlocked.unlock("UNLOCKARTIFICE");unlocked.setStage("UNLOCKARTIFICE",3);boolean found=false;
        for(int seed=0;seed<200;seed++){var a=TheorySession.start("tester",locked,0);check(a.draw(locked,net.minecraft.util.RandomSource.create(seed),false,resources)&&a.cards().stream().noneMatch(card->card.kind()==15),"Tinker bypassed category unlock");var b=TheorySession.start("tester",unlocked,0);check(b.draw(unlocked,net.minecraft.util.RandomSource.create(seed),false,resources),"Artifice deck failed drawing");found|=b.cards().stream().anyMatch(card->card.kind()==15);}check(found,"Tinker never appeared in unlocked deck");art.arcane.thaumcraft.scan.ScanRegistry.clearDerived();
        System.out.println("Original Tinker card passed: category gate, item requirement retained, 1 inspiration, piston score14..24 over50 seeds, persistence and one activation.");
    }
    private static void verifyScripting(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var relative=new BlockPos(4,1,4);h.setBlock(relative,ModBlocks.RESEARCH_TABLE.get());
        var table=h.getBlockEntity(relative,ResearchTableBlockEntity.class);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(relative)));
        int category=ResearchCatalog.CATEGORIES.indexOf("GOLEMANCY");
        String text="{\"owner\":\""+player.getUUID()+"\",\"start\":5,\"inspiration\":2,\"totals\":{},\"blocked\":[],\"cards\":[{\"kind\":16,\"category\":"+category+",\"amount\":25,\"second\":0}],\"revision\":0,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"aid_pool\":[]}";
        var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();
        var tag=table.saveWithoutMetadata(h.getLevel().registryAccess());tag.put("theory",TheorySession.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,session).getOrThrow());
        table.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
        var ink=new ItemStack(ModItems.SCRIBING_TOOLS.get());ink.setDamageValue(ink.getMaxDamage()-1);table.setItem(0,ink);table.setItem(1,new ItemStack(Items.PAPER));
        check(!table.act(player,4)&&table.getItem(1).getCount()==1&&table.session().inspiration()==2,"Scripting accepted insufficient ink or charged on failure");
        ink.setDamageValue(0);table.setItem(1,ItemStack.EMPTY);check(!table.act(player,4)&&ink.getDamageValue()==0&&table.session().totals().isEmpty(),"Scripting accepted missing paper or changed totals");
        table.setItem(1,new ItemStack(Items.PAPER));check(table.act(player,4)&&ink.getDamageValue()==2&&table.getItem(1).isEmpty()&&table.session().inspiration()==1&&table.session().totals().get("GOLEMANCY")==25,"Scripting costs or reward differ from original");
        check(!table.act(player,4)&&ink.getDamageValue()==2,"Scripting replay charged twice");
        var locked=new PlayerResearch();var unlocked=new PlayerResearch();unlocked.unlock("UNLOCKGOLEMANCY");unlocked.setStage("UNLOCKGOLEMANCY",3);boolean found=false;
        for(int seed=0;seed<200;seed++){var a=TheorySession.start("tester",locked,0);a.draw(locked,net.minecraft.util.RandomSource.create(seed),false);check(a.cards().stream().noneMatch(c->c.kind()==16),"Scripting bypassed Golemancy unlock");var b=TheorySession.start("tester",unlocked,0);b.draw(unlocked,net.minecraft.util.RandomSource.create(seed),false);found|=b.cards().stream().anyMatch(c->c.kind()==16);}check(found,"Scripting never drawn after Golemancy unlock");
        System.out.println("Scripting passed: saved card, category gate, 25 Golemancy, 1 inspiration, 1 extra paper, 2 ink, atomic missing supplies and replay rejection.");
    }
    private static void verifyCurio(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var resources=TheoryResources.player(player);
        for(int variant=0;variant<CurioTheory.IDS.size();variant++)for(int seed=0;seed<20;seed++){
            player.getInventory().clearContent();String text="{\"owner\":\"tester\",\"start\":5,\"inspiration\":2,\"totals\":{},\"blocked\":[],\"cards\":[{\"kind\":17,\"category\":0,\"amount\":0,\"second\":"+variant+"}],\"revision\":0,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"aid_pool\":[]}";
            var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();
            session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow()).getOrThrow();
            check(!session.choose(0,new PlayerResearch(),resources)&&session.inspiration()==2&&session.totals().isEmpty(),"Missing curio charged costs or awarded points");
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(CurioTheory.IDS.get(variant)));player.getInventory().add(new ItemStack(item,2));
            var random=net.minecraft.util.RandomSource.create(seed);var expected=new HashMap<String,Integer>();expected.put("BASICS",5);expected.merge(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5,Integer::sum);
            if(variant==7){expected.merge("ELDRITCH",15+random.nextInt(6),Integer::sum);expected.merge("AUROMANCY",10+random.nextInt(6),Integer::sum);}else expected.merge(CurioTheory.CATEGORIES.get(variant),25+random.nextInt(11),Integer::sum);
            int bonus=(random.nextBoolean()?1:0)+(random.nextBoolean()?1:0);player.getRandom().setSeed(seed);
            check(session.choose(0,new PlayerResearch(),resources)&&session.inspiration()==1&&player.getInventory().countItem(item)==1&&session.totals().equals(expected)&&session.bonus()==bonus,"Curio variant reward, item consumption or random sequence differs from original");
            check(!session.choose(0,new PlayerResearch(),resources)&&player.getInventory().countItem(item)==1,"Curio replay consumed twice");
        }
        player.getInventory().clearContent();boolean found=false;
        for(int seed=0;seed<200;seed++){var a=TheorySession.start("tester",new PlayerResearch(),0);a.draw(new PlayerResearch(),net.minecraft.util.RandomSource.create(seed),false,resources);check(a.cards().stream().noneMatch(c->c.kind()==17),"Curio drawn with no inventory requirement");}
        player.getInventory().add(new ItemStack(ModItems.CURIO_ARCANE.get()));
        for(int seed=0;seed<200;seed++){var a=TheorySession.start("tester",new PlayerResearch(),0);a.draw(new PlayerResearch(),net.minecraft.util.RandomSource.create(seed),false,resources);for(var card:a.cards())if(card.kind()==17){found=true;check(card.second()==1,"Curio draw selected unavailable variant");}}check(found,"Curio never drawn with available variant");
        System.out.println("Curio passed: eight variants over160 seeds, exact original rewards/random sequence, item and inspiration costs, bonus draws, persisted variant, missing/replay rejection and inventory-aware draw.");
    }
    private static void verifyRevelation(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var resources=TheoryResources.player(player);int category=ResearchCatalog.CATEGORIES.indexOf("ELDRITCH");
        for(int seed=0;seed<50;seed++){
            String text="{\"owner\":\"tester\",\"start\":5,\"inspiration\":1,\"totals\":{\"BASICS\":40,\"ALCHEMY\":50,\"GOLEMANCY\":60},\"blocked\":[],\"cards\":[{\"kind\":18,\"category\":"+category+",\"amount\":30,\"second\":0}],\"revision\":0,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"aid_pool\":[]}";
            var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow()).getOrThrow();
            var state=new PlayerResearch();state.setNormalWarp(2);state.setTemporaryWarp(4);var random=net.minecraft.util.RandomSource.create(seed);var expected=new HashMap<String,Integer>(session.totals());expected.merge(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5+random.nextInt(6),Integer::sum);expected.merge("ELDRITCH",30,Integer::sum);player.getRandom().setSeed(seed);
            check(session.choose(0,state,resources)&&session.complete()&&session.inspiration()==0&&session.totals().equals(expected)&&state.normalWarp()==3&&state.temporaryWarp()==9&&state.permanentWarp()==0,"Revelation points, inspiration or warp differ from original");
            var persisted=TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow();check(persisted.getAsJsonObject().get("penalty").getAsInt()==2,"Revelation penalty threshold not persisted");
            var ranked=new ArrayList<>(expected.entrySet());ranked.sort(Map.Entry.<String,Integer>comparingByValue().reversed());var rewards=session.rewards();for(int rank=0;rank<ranked.size();rank++){var entry=ranked.get(rank);int raw=Math.round(entry.getValue()/100f*32);check(rewards.get(entry.getKey())==(rank<2?raw:Math.max(1,raw*2/3)),"Revelation final conversion failed extra unpenalized category");}
            check(!session.choose(0,state,resources)&&state.normalWarp()==3&&state.temporaryWarp()==9,"Revelation repeat applied warp twice");
        }
        var locked=new PlayerResearch();var unlocked=new PlayerResearch();unlocked.unlock("UNLOCKELDRITCH");unlocked.setStage("UNLOCKELDRITCH",3);boolean found=false;
        for(int seed=0;seed<200;seed++){var a=TheorySession.start("tester",locked,0);a.draw(locked,net.minecraft.util.RandomSource.create(seed),false);check(a.cards().stream().noneMatch(c->c.kind()==18),"Revelation bypassed Eldritch unlock");var b=TheorySession.start("tester",unlocked,0);b.draw(unlocked,net.minecraft.util.RandomSource.create(seed),false);found|=b.cards().stream().anyMatch(c->c.kind()==18);}check(found,"Revelation never drawn after Eldritch unlock");
        System.out.println("Revelation passed: 50 seeded original rewards, warp types, inspiration, persisted penalty threshold, final conversion, repeat rejection and Eldritch category gate.");
    }
    private static void verifyEldritchCards(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var resources=TheoryResources.player(player);int category=ResearchCatalog.CATEGORIES.indexOf("ELDRITCH");
        boolean normalAdded=false,normalUnchanged=false;
        for(int kind=19;kind<=20;kind++)for(int seed=0;seed<50;seed++){
            String text="{\"owner\":\"tester\",\"start\":5,\"inspiration\":2,\"totals\":{},\"blocked\":[],\"cards\":[{\"kind\":"+kind+",\"category\":"+category+",\"amount\":0,\"second\":0}],\"revision\":0,\"bonus\":0,\"penalty\":1,\"bookshelf\":false,\"aid_pool\":[]}";
            var session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(text)).getOrThrow();session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow()).getOrThrow();
            var state=new PlayerResearch();state.setNormalWarp(2);state.setTemporaryWarp(4);state.setPermanentWarp(7);var random=net.minecraft.util.RandomSource.create(seed);var expected=new TreeMap<String,Integer>();boolean normal=false;
            if(kind==19)expected.put("ELDRITCH",10+random.nextInt(16));else{for(int n=0;n<2;n++)expected.merge(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5+random.nextInt(6),Integer::sum);expected.merge("ELDRITCH",15,Integer::sum);normal=random.nextBoolean();normalAdded|=normal;normalUnchanged|=!normal;}
            player.getRandom().setSeed(seed);check(session.choose(0,state,resources)&&session.inspiration()==1&&session.totals().equals(expected)&&session.bonus()==(kind==19?1:0)&&state.temporaryWarp()==(kind==19?7:9)&&state.normalWarp()==(normal?3:2)&&state.permanentWarp()==7,"Truth/Realization rewards, bonus or warp differ from original");
            int warp=state.temporaryWarp();check(!session.choose(0,state,resources)&&state.temporaryWarp()==warp,"Truth/Realization repeat applied warp twice");
            var persisted=TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow();var restored=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,persisted).getOrThrow();check(restored.bonus()==session.bonus()&&restored.totals().equals(expected)&&persisted.getAsJsonObject().get("penalty").getAsInt()==1,"Truth/Realization persistence changed bonus, totals or penalty threshold");
        }
        check(normalAdded&&normalUnchanged,"Realization normal warp chance branches not exercised");
        var locked=new PlayerResearch();var unlocked=new PlayerResearch();unlocked.unlock("UNLOCKELDRITCH");unlocked.setStage("UNLOCKELDRITCH",3);int found=0;
        for(int seed=0;seed<300;seed++){var a=TheorySession.start("tester",locked,0);a.draw(locked,net.minecraft.util.RandomSource.create(seed),false);check(a.cards().stream().noneMatch(c->c.kind()==19||c.kind()==20),"Truth/Realization bypassed Eldritch unlock");var b=TheorySession.start("tester",unlocked,0);b.draw(unlocked,net.minecraft.util.RandomSource.create(seed),false);for(var card:b.cards())if(card.kind()==19||card.kind()==20)found|=1<<(card.kind()-19);}check(found==3,"Truth/Realization never drawn after Eldritch unlock");
        System.out.println("Truth and Realization passed: 100 seeded original reward comparisons, bonus, temporary/normal warp and both chance branches, unchanged permanent warp/penalty, persistence, replay rejection and category gate.");
    }
    public static void test(GameTestHelper h){
        verifyEldritchCards(h);
        verifyRevelation(h);
        verifyCurio(h);
        verifyScripting(h);
        verifyTinker(h);
        verifyCardRules();
        var world=h.getLevel();var manager=world.getServer().getRecipeManager();
        for(var bottle:List.of(Items.GLASS_BOTTLE,ModItems.PHIAL_EMPTY.get()))for(var ink:List.of(Items.INK_SAC,Items.DYE.black())){
            var input=CraftingInput.of(2,2,List.of(new ItemStack(bottle),new ItemStack(Items.FEATHER),new ItemStack(ink),ItemStack.EMPTY));
            var recipe=manager.getRecipeFor(RecipeType.CRAFTING,input,world).orElseThrow();
            check(recipe.value().assemble(input).is(ModItems.SCRIBING_TOOLS.get()),"Scribing recipe rejected original materials");
        }
        var tools=new ItemStack(ModItems.SCRIBING_TOOLS.get());tools.setDamageValue(100);
        tools.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My ink"));
        var refill=CraftingInput.of(2,1,List.of(tools,new ItemStack(Items.INK_SAC)));
        var result=manager.getRecipeFor(RecipeType.CRAFTING,refill,world).orElseThrow().value().assemble(refill);
        check(result.getDamageValue()==0&&result.getHoverName().getString().equals("My ink")&&tools.getDamageValue()==100,"Refill lost components or mutated input");
        check(!new art.arcane.thaumcraft.crafting.ScribingToolsRecipe(true).matches(CraftingInput.of(2,1,List.of(result,new ItemStack(Items.INK_SAC))),world),"Full ink allowed refill");
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var relative=new BlockPos(2,1,2);h.setBlock(relative,ModBlocks.TABLE_WOOD.get());
        var pos=h.absolutePos(relative);player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,result);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        ModItems.SCRIBING_TOOLS.get().useOn(context);var table=h.getBlockEntity(relative,ResearchTableBlockEntity.class);
        check(player.getMainHandItem().isEmpty()&&table.getItem(0).getHoverName().getString().equals("My ink")&&PlayerProgressManager.get(player).crafted("thaumcraft:research_table"),"Table formation lost ink or craft discovery");
        table.setItem(1,new ItemStack(Items.PAPER,64));
        var menu=(ResearchTableMenu)table.createMenu(12,player.getInventory(),player);check(menu.slots.size()==38,"Table slot count changed");
        check(!menu.slots.get(0).mayPlace(new ItemStack(Items.PAPER))&&!menu.slots.get(1).mayPlace(new ItemStack(Items.COAL)),"Research slots accept invalid items");
        check(menu.clickMenuButton(player,menu.button(0)),"Theory did not start");
        int draw=menu.button(2);check(menu.clickMenuButton(player,draw)&&table.getItem(1).getCount()==63,"Draw did not charge one paper");
        check(!menu.clickMenuButton(player,draw)&&table.getItem(1).getCount()==63,"Stale button charged twice");
        var saved=table.saveWithoutMetadata(world.registryAccess());var restored=new ResearchTableBlockEntity(pos,table.getBlockState());
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),saved));
        check(restored.session()!=null&&restored.session().cards().equals(table.session().cards())&&restored.getItem(1).getCount()==63,"Persistence rerolled or lost cards");
        var other=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);other.setPos(player.position());
        check(!table.act(other,menu.button(4))&&!table.act(other,menu.button(8)),"Other player altered owned theory");
        int inkDamage=table.getItem(0).getDamageValue();check(menu.clickMenuButton(player,menu.button(4))&&table.getItem(0).getDamageValue()==inkDamage+1,"Choosing a card failed ink cost");
        for(int n=0;n<200&&!table.session().complete();n++){
            if(table.session().cards().isEmpty())check(menu.clickMenuButton(player,menu.button(2)),"Theory draw deadlocked");
            int best=0;for(int c=1;c<table.session().cards().size();c++)if(table.session().cards().get(c).cost()>table.session().cards().get(best).cost())best=c;
            check(menu.clickMenuButton(player,menu.button(4+best)),"Theory activation rejected a valid card");
        }
        check(table.session().complete(),"Theory never completed");var rewards=table.session().rewards();int claim=menu.button(7);
        check(menu.clickMenuButton(player,claim)&&table.session()==null,"Theory claim failed");
        for(var reward:rewards.entrySet())check(PlayerProgressManager.get(player).knowledge("THEORY",reward.getKey())==reward.getValue(),"Original theory conversion changed");
        check(!menu.clickMenuButton(player,claim),"Theory could be claimed twice");
        table.getItem(0).setDamageValue(100);check(!menu.clickMenuButton(player,menu.button(0))&&!table.getItem(0).isEmpty(),"Exhausted ink was removed or accepted");
        player.setPos(pos.getX()+20,pos.getY(),pos.getZ());check(!menu.stillValid(player),"Remote research menu remained valid");
        // Exercise many seeds, including last-inspiration recovery cards and serialization after each draw.
        for(int seed=0;seed<100;seed++){
            var state=new PlayerResearch();state.addKnowledge("OBSERVATION","ALCHEMY",1000);var session=TheorySession.start("tester",state,seed%2==0);var random=net.minecraft.util.RandomSource.create(seed);
            for(int step=0;step<200&&!session.complete();step++){
                check(session.draw(state,random,true),"Basic deck deadlocked for seed "+seed);
                var json=TheorySession.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,session).getOrThrow();session=TheorySession.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
                int best=0;for(int c=1;c<session.cards().size();c++)if(session.cards().get(c).cost()>session.cards().get(best).cost())best=c;
                check(session.choose(best,state),"Seed card activation failed");
            }
            check(session.complete(),"Seed never completed: "+seed);
        }
        System.out.println("Research table passed: original ink materials, refill components, formation, paper/ink costs, saved cards, ownership, stale actions, single claim and 100 theory seeds.");h.succeed();
    }
}
