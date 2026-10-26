package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.*;
import art.arcane.thaumcraft.research.*;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;

public final class SpecialisedTheoryVerification {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static TheorySession fixture(int kind,String aspect,String second){
        var json=new JsonObject();json.addProperty("owner","tester");json.addProperty("start",5);json.addProperty("inspiration",3);json.addProperty("bonus",0);json.addProperty("penalty",1);json.addProperty("bookshelf",false);
        json.add("totals",new JsonObject());json.add("blocked",new JsonArray());json.addProperty("revision",0);json.add("aid_pool",new JsonArray());
        var card=new JsonObject();card.addProperty("kind",kind);card.addProperty("category",kind<12?2:1);card.addProperty("amount",0);card.addProperty("second",0);card.addProperty("aspect",aspect);card.addProperty("aspect_second",second);
        var cards=new JsonArray();cards.add(card);json.add("cards",cards);
        return TheorySession.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
    }
    private static void seedChance(ServerPlayer player,boolean chance){
        for(long seed=0;seed<100000;seed++)if((RandomSource.create(seed).nextFloat()<.33f)==chance){player.getRandom().setSeed(seed);return;}
        throw new AssertionError("No deterministic chance fixture");
    }
    public static void test(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var inventory=player.getInventory();var resources=TheoryResources.player(player);var state=new PlayerResearch();
        var concentrate=fixture(9,"lux","");inventory.setItem(0,CrystalEssenceItem.of("aer",2));
        check(!concentrate.choose(0,state,resources)&&concentrate.inspiration()==3&&inventory.getItem(0).getCount()==2,"Concentrate accepted wrong crystal or charged a failed action");
        inventory.setItem(0,CrystalEssenceItem.of("lux",2));seedChance(player,false);
        check(concentrate.choose(0,state,resources)&&concentrate.totals().get("ALCHEMY")==15&&concentrate.bonus()==1&&concentrate.inspiration()==2&&inventory.getItem(0).getCount()==2,"Concentrate consumed crystal or changed original effect");
        var refunded=fixture(9,"lux","");seedChance(player,true);check(refunded.choose(0,state,resources)&&refunded.inspiration()==3,"Alchemy inspiration chance did not refund a use");
        var reactions=fixture(10,"lux","metallum");check(!reactions.choose(0,state,resources)&&inventory.getItem(0).getCount()==2,"Reactions accepted a missing second crystal");
        inventory.setItem(1,CrystalEssenceItem.of("metallum",2));seedChance(player,false);
        check(reactions.choose(0,state,resources)&&reactions.totals().get("ALCHEMY")==25&&inventory.getItem(0).getCount()==2&&inventory.getItem(1).getCount()==2,"Reactions costs or progress changed");
        var duplicate=fixture(10,"lux","lux");inventory.setItem(0,CrystalEssenceItem.of("lux",1));
        check(!duplicate.choose(0,state,resources)&&inventory.getItem(0).getCount()==1,"Two crystal requirements reused one unit");
        inventory.clearContent();inventory.setItem(0,CrystalEssenceItem.of("terra",2));var synthesis=fixture(11,"metallum","");
        check(!synthesis.choose(0,state,resources)&&inventory.getItem(0).getCount()==2,"Incomplete synthesis consumed its first reagent");
        inventory.setItem(1,CrystalEssenceItem.of("ordo",2));seedChance(player,false);
        check(synthesis.choose(0,state,resources)&&synthesis.totals().get("ALCHEMY")==40&&inventory.getItem(0).getCount()==1&&inventory.getItem(1).getCount()==1,"Synthesis did not consume one of each parent");
        check(inventory.getItem(2).getCount()==1&&CrystalEssenceItem.aspect(inventory.getItem(2)).equals("metallum"),"Synthesis did not produce its compound crystal");
        inventory.clearContent();for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        inventory.setItem(0,CrystalEssenceItem.of("terra",64));inventory.setItem(1,CrystalEssenceItem.of("ordo",64));player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4))));
        var full=fixture(11,"metallum","");seedChance(player,false);check(full.choose(0,state,resources),"Full inventory rejected payable synthesis");
        check(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(3),e->CrystalEssenceItem.aspect(e.getItem()).equals("metallum")&&e.getItem().getCount()==1).isEmpty(),"Full inventory lost synthesis reward instead of dropping it");
        var focus=fixture(12,"","");check(focus.choose(0,state,resources)&&focus.totals().get("AUROMANCY")==15&&focus.bonus()==1&&focus.inspiration()==2,"Focus card changed");
        var safe=fixture(13,"","");seedChance(player,false);check(safe.choose(0,state,resources)&&safe.totals().get("AUROMANCY")==20&&!safe.totals().containsKey("ELDRITCH")&&state.normalWarp()==0,"Awareness guaranteed its random penalty");
        var risky=fixture(13,"","");seedChance(player,true);check(risky.choose(0,state,resources)&&risky.totals().get("ELDRITCH")>=1&&risky.totals().get("ELDRITCH")<=5&&state.normalWarp()==1,"Awareness did not award original knowledge/warp chance");
        var spell=fixture(14,"","");check(!spell.choose(0,state,resources)&&spell.inspiration()==3,"Spellbinding accepted zero experience");
        player.giveExperienceLevels(2);check(spell.choose(0,state,resources)&&player.experienceLevel==0&&spell.totals().get("AUROMANCY")==10,"Spellbinding did not consume two levels for ten percent");
        player.giveExperienceLevels(8);var capped=fixture(14,"","");check(capped.choose(0,state,resources)&&player.experienceLevel==3&&capped.totals().get("AUROMANCY")==25,"Spellbinding consumed more than five levels");
        for(int kind=9;kind<=14;kind++){
            var session=fixture(kind,kind<=11?"metallum":"",kind==10?"lux":"");var encoded=TheorySession.CODEC.encodeStart(JsonOps.INSTANCE,session).getOrThrow();var restored=TheorySession.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();
            check(restored.cards().equals(session.cards()),"Saved specialised card lost its aspect requirements");
        }
        // Compatibility with a fix34 save with no aid_mask or new aspect fields.
        var old=JsonParser.parseString("{\"owner\":\"tester\",\"start\":5,\"inspiration\":4,\"bonus\":0,\"penalty\":1,\"bookshelf\":true,\"totals\":{},\"blocked\":[],\"cards\":[{\"kind\":0,\"category\":2,\"amount\":20,\"second\":5}],\"revision\":4,\"aid_pool\":[6,8,7]}");
        var migrated=TheorySession.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();check(migrated.aidMask()==1&&migrated.cards().get(0).amount()==20&&migrated.inspiration()==4,"fix34 session migration failed");
        var pos=new BlockPos(7,1,7);h.setBlock(pos,ModBlocks.RESEARCH_TABLE.get());var table=h.getBlockEntity(pos,ResearchTableBlockEntity.class);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)));inventory.clearContent();
        table.setItem(0,new ItemStack(ModItems.SCRIBING_TOOLS.get()));table.setItem(1,new ItemStack(Items.PAPER,10));
        check(!table.act(player,2)&&table.session()==null,"Absent crucible could be selected as an aid");
        h.setBlock(pos.offset(1,0,0),net.minecraft.world.level.block.Blocks.BOOKSHELF);h.setBlock(pos.offset(0,0,1),ModBlocks.CRUCIBLE.get());h.setBlock(pos.offset(1,0,1),ModBlocks.WAND_WORKBENCH.get());
        check(table.availableAids()==7&&table.act(player,7)&&table.session().aidMask()==7&&table.session().inspiration()==2,"Combined aids did not cost one inspiration each");
        var aided=TheorySession.CODEC.encodeStart(JsonOps.INSTANCE,table.session()).getOrThrow().getAsJsonObject();check(aided.getAsJsonArray("aid_pool").size()==12,"Combined aid pool lost original weights");
        var gated=new PlayerResearch();for(int seed=0;seed<100;seed++){
            var session=TheorySession.start("tester",gated,0);check(session.draw(gated,RandomSource.create(seed),false,resources),"Initial basic deck failed");
            check(session.cards().stream().noneMatch(c->c.kind()>=9),"Normal deck exposed locked research categories");
        }
        CelestialTheoryVerification.verify(h);
        System.out.println("Specialised theory cards passed: six original effects, atomic/aspect costs, retained crystals, full-inventory reward, experience cap, warp/refund probabilities, fix34 migration and combined aid costs.");h.succeed();
    }
}

