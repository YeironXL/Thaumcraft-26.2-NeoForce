package art.arcane.thaumcraft.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.Aspect;
import java.util.*;

/** TC6 basic, alchemy and auromancy theory cards. Random choices are fixed when drawn and persisted. */
public final class TheorySession {
    public static final List<String> ASPECTS=List.copyOf(AspectRegistry.all().keySet());
    public record Card(int kind,int category,int amount,int second,String aspect,String aspectSecond) {
        public Card(int kind,int category,int amount,int second){this(kind,category,amount,second,"","");}
        public static final Codec<Card> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,21).fieldOf("kind").forGetter(Card::kind),Codec.intRange(0,6).fieldOf("category").forGetter(Card::category),
            Codec.intRange(0,10000).fieldOf("amount").forGetter(Card::amount),Codec.intRange(0,17).fieldOf("second").forGetter(Card::second),
            Codec.STRING.optionalFieldOf("aspect","").forGetter(Card::aspect),Codec.STRING.optionalFieldOf("aspect_second","").forGetter(Card::aspectSecond)
        ).apply(i,Card::new));
        public Map<String,Integer> crystals(){
            var demands=new LinkedHashMap<String,Integer>();
            if(kind==9||kind==10){var a=AspectRegistry.get(aspect);if(a==null||a.isPrimal())return null;demands.merge(aspect,1,Integer::sum);
                if(kind==10){var b=AspectRegistry.get(aspectSecond);if(b==null||b.isPrimal())return null;demands.merge(aspectSecond,1,Integer::sum);}}
            if(kind==11){var a=AspectRegistry.get(aspect);if(a==null||a.isPrimal())return null;demands.merge(a.parentA().id(),1,Integer::sum);demands.merge(a.parentB().id(),1,Integer::sum);}
            return demands;
        }
        public int cost(){return switch(kind){case 0,1,2,3->2;case 4->-1;case 5->0;default->1;};}
    }
    public static final Codec<TheorySession> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("owner").forGetter(s->s.owner),Codec.intRange(0,15).fieldOf("start").forGetter(s->s.start),
        Codec.INT.fieldOf("inspiration").forGetter(s->s.inspiration),Codec.intRange(0,10000).fieldOf("bonus").forGetter(s->s.bonus),
        Codec.intRange(0,10000).fieldOf("penalty").forGetter(s->s.penalty),Codec.BOOL.fieldOf("bookshelf").forGetter(s->s.bookshelf),
        Codec.unboundedMap(Codec.STRING,Codec.intRange(0,10000)).fieldOf("totals").forGetter(s->s.totals),
        Codec.STRING.listOf().fieldOf("blocked").forGetter(s->List.copyOf(s.blocked)),Card.CODEC.listOf().fieldOf("cards").forGetter(s->s.cards),
        Codec.INT.fieldOf("revision").forGetter(s->s.revision),Codec.intRange(6,14).listOf().fieldOf("aid_pool").forGetter(s->s.aidPool),
        Codec.intRange(-1,7).optionalFieldOf("aid_mask",-1).forGetter(s->s.aidMask)
    ).apply(i,TheorySession::new));
    private final String owner;
    private final int start;
    private int inspiration,bonus,penalty,revision;
    private final boolean bookshelf;
    private final int aidMask;
    private final TreeMap<String,Integer> totals;
    private final Set<String> blocked;
    private List<Card> cards;
    private final List<Integer> aidPool;
    private TheorySession(String owner,int start,int inspiration,int bonus,int penalty,boolean bookshelf,Map<String,Integer> totals,List<String> blocked,List<Card> cards,int revision,List<Integer> aidPool,int aidMask){
        this.aidMask=aidMask<0?(bookshelf?1:0):aidMask;this.aidPool=new ArrayList<>(aidPool);this.owner=owner;this.start=start;this.inspiration=inspiration;this.bonus=bonus;this.penalty=penalty;this.bookshelf=bookshelf;
        this.totals=new TreeMap<>(totals);this.blocked=new HashSet<>(blocked);this.cards=List.copyOf(cards);this.revision=Math.floorMod(revision,32768);
    }
    public static int availableInspiration(PlayerResearch state){
        double available=5;
        for(var entry:ResearchCatalog.entries().values())if(ResearchRules.complete(state,entry.key())){
            if(entry.meta().contains("SPIKY"))available+=.5;if(entry.meta().contains("HIDDEN"))available+=.1;
        }
        return Math.min(15,(int)Math.round(available));
    }
    public static TheorySession start(String owner,PlayerResearch state,boolean bookshelf){return start(owner,state,bookshelf?1:0);}
    public static TheorySession start(String owner,PlayerResearch state,int aids){
        if(aids<0||aids>7)throw new IllegalArgumentException("Unknown initial theory aid");
        int start=availableInspiration(state);var pool=new ArrayList<Integer>();
        if((aids&1)!=0)pool.addAll(List.of(6,8,8,7,7,7));
        if((aids&2)!=0)pool.addAll(List.of(9,10,11));
        if((aids&4)!=0)pool.addAll(List.of(12,13,14));
        return new TheorySession(owner,start,start-Integer.bitCount(aids),0,1,(aids&1)!=0,Map.of(),List.of(),List.of(),0,pool,aids);
    }
    public int aidMask(){return aidMask;}
    public boolean ownedBy(String id){return owner.equals(id);}
    public int inspiration(){return inspiration;}public int start(){return start;}public int bonus(){return bonus;}public int revision(){return revision;}
    public int blockedMask(){int mask=0;for(var c:blocked){int n=index(c);if(n>=0)mask|=1<<n;}return mask;}
    public boolean complete(){return inspiration<=0;}public List<Card> cards(){return cards;}public Map<String,Integer> totals(){return Collections.unmodifiableMap(totals);}
    private void changed(){revision=(revision+1)%32768;}
    private void add(String category,int value){int next=totals.getOrDefault(category,0)+value;if(next<=0)totals.remove(category);else totals.put(category,Math.min(10000,next));}
    private List<String> active(){return totals.keySet().stream().filter(c->!blocked.contains(c)).toList();}
    private static int index(String category){return ResearchCatalog.CATEGORIES.indexOf(category);}
    private Card make(int kind,PlayerResearch state,RandomSource random,TheoryResources resources){
        var active=active();String category="BASICS";int amount=0,second=0;String aspect="",aspectSecond="";
        switch(kind){
            case 21->{if(totals.isEmpty()||!ResearchRules.complete(state,"CELESTIALSCANNING"))return null;
                category=totals.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
                amount=random.nextInt(13);do{second=random.nextInt(13);}while(second==amount);}
            case 0->{category=ResearchCatalog.CATEGORIES.get(random.nextInt(7));amount=15+random.nextInt(16);second=1+random.nextInt(10);}
            case 1->{var observed=ResearchCatalog.CATEGORIES.stream().filter(c->!c.equals("BASICS")&&state.knowledge("OBSERVATION",c)>=16).toList();
                if(observed.isEmpty())return null;category=observed.get(random.nextInt(observed.size()));amount=25+random.nextInt(26);}
            case 2->{if(active.isEmpty())return null;}
            case 3->{if(totals.isEmpty())return null;category=totals.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();amount=10+totals.get(category)/2;}
            case 4->{if(totals.values().stream().mapToInt(Integer::intValue).sum()<10)return null;amount=1+random.nextInt(10);}
            case 5->{if(active.isEmpty())return null;category=active.get(random.nextInt(active.size()));}
            case 6->{if(active.size()<2)return null;}
            case 7->{var available=ResearchCatalog.CATEGORIES.stream().filter(c->c.equals("BASICS")||ResearchRules.complete(state,"UNLOCK"+c)).toList();category=available.get(random.nextInt(available.size()));amount=15+random.nextInt(11);}
            case 8->{if(totals.size()<2)return null;
                category=totals.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
                String high=totals.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
                if(totals.get(category).equals(totals.get(high)))return null;second=index(high);int low=totals.get(category);amount=low/2+random.nextInt(low/2+1);}
            case 9,10,11->{category="ALCHEMY";var compounds=AspectRegistry.all().values().stream().filter(a->!a.isPrimal()).toList();int chosen=random.nextInt(compounds.size());aspect=compounds.get(chosen).id();
                if(kind==10){int other=random.nextInt(compounds.size()-1);if(other>=chosen)other++;aspectSecond=compounds.get(other).id();}}
            case 12,13->category="AUROMANCY";
            case 14->{if(resources.experienceLevels()<=0)return null;category="AUROMANCY";}
            case 19,20->category="ELDRITCH";
            case 18->{category="ELDRITCH";amount=30;}
            case 17->{var available=CurioTheory.available(resources);if(available.isEmpty())return null;second=available.get(random.nextInt(available.size()));}
            case 16->{category="GOLEMANCY";amount=25;}
            case 15->{category="ARTIFICE";second=random.nextInt(TinkerOptions.IDS.size());amount=TinkerOptions.minimum(second);}
            default->{return null;}
        }
        return new Card(kind,index(category),Math.min(10000,amount),second,aspect,aspectSecond);
    }
    public boolean draw(PlayerResearch state,RandomSource random,boolean extra){return draw(state,random,extra,TheoryResources.none());}
    public boolean draw(PlayerResearch state,RandomSource random,boolean extra,TheoryResources resources){
        if(complete()||!cards.isEmpty())return false;
        int count=extra&&bonus>0?3:2;var choices=new ArrayList<Card>();var used=new HashSet<Integer>();
        for(int tries=0;tries<100&&choices.size()<count;tries++){
            boolean aided=!aidPool.isEmpty()&&random.nextInt(4)==0;
            int poolIndex=aided?random.nextInt(aidPool.size()):-1;int kind=aided?aidPool.get(poolIndex):normalKind(random,state,resources);
            if(used.contains(kind))continue;var card=make(kind,state,random,resources);if(card==null||card.cost()>inspiration)continue;
            if(kind==1||kind==7||kind>=9){String category=ResearchCatalog.CATEGORIES.get(card.category());
                if(blocked.contains(category)||!aided&&!category.equals("BASICS")&&!ResearchRules.complete(state,"UNLOCK"+category))continue;}
            choices.add(card);used.add(kind);if(aided)aidPool.remove(poolIndex);
        }
        if(choices.isEmpty())return false;if(count==3)bonus--;cards=List.copyOf(choices);changed();return true;
    }
    private static int normalKind(RandomSource random,PlayerResearch state,TheoryResources resources){
        boolean golem=ResearchRules.complete(state,"UNLOCKGOLEMANCY"),curio=!CurioTheory.available(resources).isEmpty(),eldritch=ResearchRules.complete(state,"UNLOCKELDRITCH");
        int originalSize=14+(golem?1:0)+(curio?1:0)+(eldritch?3:0);
        int value=random.nextInt(originalSize+(ResearchRules.complete(state,"CELESTIALSCANNING")?1:0));
        if(value==originalSize)return 21;
        if(value<14)return value<7?value:value+2;
        int extra=value-14;if(golem){if(extra==0)return 16;extra--;}
        if(curio){if(extra==0)return 17;extra--;}
        return 18+extra;
    }

    public boolean choose(int choice,PlayerResearch state){return choose(choice,state,TheoryResources.none());}
    public boolean choose(int choice,PlayerResearch state,TheoryResources resources){
        if(complete()||choice<0||choice>=cards.size())return false;var card=cards.get(choice);if(card.cost()>inspiration)return false;
        String category=ResearchCatalog.CATEGORIES.get(card.category());var active=active();
        if(card.kind()>=9&&card.kind()<=11){var demands=card.crystals();if(demands==null||!resources.crystals(demands,card.kind()==11))return false;}
        if(card.kind()==14&&resources.experienceLevels()<=0)return false;
        if(card.kind()==15&&(card.second()<0||card.second()>=TinkerOptions.IDS.size()||!resources.hasItem(TinkerOptions.IDS.get(card.second()))))return false;
        switch(card.kind()){
            case 21->{
                int first=card.amount(),second=card.second();
                if(!ResearchRules.complete(state,"CELESTIALSCANNING")||first<0||first>12||second<0||second>12||first==second||!resources.consumeCelestialNotes(first,second))return false;
                add(category,25+resources.random().nextInt(26));
                if(first==0||second==0)penalty++;
                if(first>4||second>4)bonus++;
                if(first>0&&first<5||second>0&&second<5){int warp=resources.random().nextInt(6);add("ELDRITCH",warp*2);state.addWarp(PlayerResearch.WARP_TEMPORARY,warp);}
            }
            case 0->{add(category,card.amount());add("BASICS",card.second());}
            case 1->{if(state.knowledge("OBSERVATION",category)<16)return false;state.addKnowledge("OBSERVATION",category,-16);add(category,card.amount());add("BASICS",5);}
            case 2->{if(active.isEmpty())return false;for(int n=0;n<25;n++)add(active.get(n%active.size()),1);add("BASICS",5);bonus++;}
            case 3->add(category,card.amount());
            case 4->{if(totals.values().stream().mapToInt(Integer::intValue).sum()<10)return false;
                for(int n=0;n<10;n++){var keys=List.copyOf(totals.keySet());add(keys.get(n%keys.size()),-1);}bonus++;add("BASICS",card.amount());}
            case 5->{if(!totals.containsKey(category)||blocked.contains(category))return false;add("BASICS",5);blocked.add(category);}
            case 6->{if(active.size()<2)return false;int average=active.stream().mapToInt(c->totals.get(c)).sum()/active.size();for(var c:active)totals.put(c,average);add("BASICS",5);penalty++;}
            case 7->add(category,card.amount());
            case 8->{if(!totals.containsKey(category))return false;totals.remove(category);add(ResearchCatalog.CATEGORIES.get(card.second()),card.amount());}
            case 9,10,11->{add("ALCHEMY",switch(card.kind()){case 9->15;case 10->25;default->40;});if(card.kind()==9)bonus++;
                if(resources.random().nextFloat()<.33f)inspiration=Math.min(start,inspiration+1);if(card.kind()==11)resources.giveCrystal(card.aspect());}
            case 12->{add("AUROMANCY",15);bonus++;}
            case 13->{add("AUROMANCY",20);if(resources.random().nextFloat()<.33f){add("ELDRITCH",1+resources.random().nextInt(5));state.addWarp(PlayerResearch.WARP_NORMAL,1);}}
            case 14->{int levels=Math.min(5,resources.experienceLevels());resources.removeLevels(levels);add("AUROMANCY",levels*5);}
            case 19->{
                add("ELDRITCH",10+resources.random().nextInt(16));bonus++;
                state.addWarp(PlayerResearch.WARP_TEMPORARY,3);
            }
            case 20->{
                var random=resources.random();
                for(int n=0;n<2;n++)add(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5+random.nextInt(6));
                add("ELDRITCH",15);state.addWarp(PlayerResearch.WARP_TEMPORARY,5);
                if(random.nextBoolean())state.addWarp(PlayerResearch.WARP_NORMAL,1);
            }
            case 18->{
                var random=resources.random();add(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5+random.nextInt(6));add("ELDRITCH",30);
                state.addWarp(PlayerResearch.WARP_TEMPORARY,5);
                state.addWarp(PlayerResearch.WARP_NORMAL,1);penalty++;
            }
            case 17->{
                if(card.second()<0||card.second()>=CurioTheory.IDS.size()||!resources.consumeItem(CurioTheory.IDS.get(card.second())))return false;
                var random=resources.random();add("BASICS",5);add(ResearchCatalog.CATEGORIES.get(random.nextInt(7)),5);
                if(card.second()==7){add("ELDRITCH",15+random.nextInt(6));add("AUROMANCY",10+random.nextInt(6));}
                else add(CurioTheory.CATEGORIES.get(card.second()),25+random.nextInt(11));
                if(random.nextBoolean())bonus++;if(random.nextBoolean())bonus++;
            }
            case 16->{if(!resources.consumeScribingSupplies())return false;add("GOLEMANCY",25);}
            case 15->add("ARTIFICE",TinkerOptions.minimum(card.second())+resources.random().nextInt(11));
            default->{return false;}
        }
        inspiration=Math.min(start,inspiration-card.cost());cards=List.of();changed();return true;
    }
    public Map<String,Integer> rewards(){
        if(!complete())return Map.of();var ranked=new ArrayList<>(totals.entrySet());ranked.sort(Map.Entry.<String,Integer>comparingByValue().reversed());
        var rewards=new LinkedHashMap<String,Integer>();int rank=0;
        for(var entry:ranked){int raw=Math.round(entry.getValue()/100f*32);if(rank++>=penalty)raw=Math.max(1,raw*2/3);rewards.put(entry.getKey(),raw);}
        return rewards;
    }
}

