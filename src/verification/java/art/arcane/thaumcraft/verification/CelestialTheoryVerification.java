package art.arcane.thaumcraft.verification;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.research.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
final class CelestialTheoryVerification {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static TheorySession session(int first,int second){return TheorySession.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
        {"owner":"astronomer","start":5,"inspiration":5,"bonus":0,"penalty":1,"bookshelf":false,"totals":{"BASICS":10},"blocked":[],"cards":[{"kind":21,"category":0,"amount":%d,"second":%d}],"revision":0,"aid_pool":[],"aid_mask":0}
        """.formatted(first,second))).getOrThrow();}
    private static ItemStack note(int variant,int count){var stack=new ItemStack(ModItems.CELESTIAL_NOTES.get(),count);stack.set(ModComponents.CELESTIAL_NOTE.get(),variant);return stack;}
    static void verify(GameTestHelper h){
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);var inv=player.getInventory();var resources=TheoryResources.player(player);var state=new PlayerResearch();
        inv.setItem(3,note(0,2));inv.setItem(4,note(5,2));var trial=session(0,5);
        check(!trial.choose(0,state,resources)&&inv.getItem(3).getCount()==2,"Celestial research bypass consumed notes");
        state.unlock("CELESTIALSCANNING");state.setStage("CELESTIALSCANNING",2);
        inv.setItem(4,ItemStack.EMPTY);check(!trial.choose(0,state,resources)&&inv.getItem(3).getCount()==2&&trial.inspiration()==5,"Missing second note charged first/inspiration");
        inv.setItem(4,note(5,2));check(trial.choose(0,state,resources),"Valid sun/moon card failed");
        check(inv.getItem(3).getCount()==1&&inv.getItem(4).getCount()==1&&trial.inspiration()==4,"Celestial card cost not exactly two notes/one inspiration");
        check(trial.totals().get("BASICS")>=35&&trial.totals().get("BASICS")<=60&&trial.bonus()==1,"Celestial score/moon bonus wrong");
        var saved=TheorySession.CODEC.encodeStart(JsonOps.INSTANCE,trial).getOrThrow().getAsJsonObject();check(saved.get("penalty").getAsInt()==2,"Sun penalty missing");
        check(!trial.choose(0,state,resources),"Consumed celestial card replayed");
        inv.setItem(3,note(1,1));inv.setItem(4,note(2,1));var stars=session(1,2);check(stars.choose(0,state,resources),"Star pair failed");
        check(state.temporaryWarp()<=5&&stars.totals().getOrDefault("ELDRITCH",0)==state.temporaryWarp()*2,"Star warp/Eldritch effect wrong");
        boolean drawn=false;for(int seed=0;seed<200;seed++){
            var deck=session(0,5);check(deck.choose(0,state,new TheoryResources(){public boolean consumeCelestialNotes(int a,int b){return true;}public int experienceLevels(){return 0;}public boolean crystals(java.util.Map<String,Integer> d,boolean c){return false;}public void removeLevels(int n){}public void giveCrystal(String a){}public RandomSource random(){return RandomSource.create(seedValue);}private final int seedValue=1;}),"Fixture activation failed");
            deck.draw(state,RandomSource.create(seed),false,resources);
            for(var card:deck.cards())if(card.kind()==21){drawn=true;check(card.amount()!=card.second()&&card.amount()<13&&card.second()<13,"Drawn celestial variants invalid");}
            var round=TheorySession.CODEC.parse(JsonOps.INSTANCE,TheorySession.CODEC.encodeStart(JsonOps.INSTANCE,deck).getOrThrow()).getOrThrow();check(round.cards().equals(deck.cards()),"Celestial save lost variant pair");
        }
        check(drawn,"Celestial never appears in eligible normal deck");
        System.out.println("Celestial theory passed: gate, atomic typed-note costs, original score/sun/moon/stars effects, replay rejection and persisted normal draws.");
    }
}
