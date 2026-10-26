"""Convert TC6 ConfigAspects.registerItemAspects/registerEntityAspects into ordered data the port replays.

Output data/thaumcraft/aspects/objects.json: list of operations executed in the original order at server start.
 {"op":"set"|"complex","targets":[item ids or #tags],"base":"item id"(optional),"ops":[["add"|"merge"|"remove",aspect,amount],...]}
 {"op":"potions"} replays the potion loop.
Usage: TC6_REF=<reference repo> python3 tools/port_tc6_aspects.py
"""
import json,os,re,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent/'tc6ref'))
import refmap,flatten
from jparse import tokens,P,statements
root=Path(__file__).resolve().parents[1]
SRC=open(os.path.join(refmap.REF,'common/config/ConfigAspects.java')).read()
ASP={m.group(1):m.group(2) for m in re.finditer(r'(?m)^\s*public static Aspect (\w+) = new Aspect\("(\w+)"',open(os.path.join(refmap.REF,'api/aspects/Aspect.java')).read())}
ORE=dict(refmap.ORE)
ORE.update({'blockCactus':'minecraft:cactus','bone':'#c:bones','cobblestone':'#c:cobblestones','cropCarrot':'#c:crops/carrot','cropNetherWart':'#c:crops/nether_wart',
 'cropPotato':'#c:crops/potato','cropWheat':'#c:crops/wheat','dirt':'#c:dirt'.replace('#c:dirt','minecraft:dirt'),'dustBrass':'#c:dusts/brass','dustBronze':'#c:dusts/bronze','dustCopper':'#c:dusts/copper',
 'dustGold':'#c:dusts/gold','dustIron':'#c:dusts/iron','dustLead':'#c:dusts/lead','dustSilver':'#c:dusts/silver','dustTin':'#c:dusts/tin','egg':'#c:eggs','enderpearl':'#c:ender_pearls',
 'endstone':'#c:end_stones','gemGreenSapphire':'#c:gems/peridot','gemRuby':'#c:gems/ruby','gemSapphire':'#c:gems/sapphire','glowstone':'minecraft:glowstone','grass':'minecraft:grass_block',
 'gravel':'#c:gravels','ingotBrickNether':'minecraft:nether_brick','ingotBronze':'#c:ingots/bronze','ingotLead':'#c:ingots/lead','ingotSilver':'#c:ingots/silver','ingotSteel':'#c:ingots/steel',
 'ingotTin':'#c:ingots/tin','ingotUranium':'#c:ingots/uranium','itemDropUranium':'#c:drops/uranium','itemRubber':'#c:rubbers','netherStar':'#c:nether_stars','netherrack':'#c:netherracks',
 'oreDiamond':'#c:ores/diamond','oreEmerald':'#c:ores/emerald','oreLapis':'#c:ores/lapis','oreQuartz':'#c:ores/quartz','oreRedstone':'#c:ores/redstone','oreUranium':'#c:ores/uranium',
 'oreAmber':'thaumcraft:ore_amber','sand':'#c:sands','sugarcane':'minecraft:sugar_cane','stoneAndesite':'minecraft:andesite','stoneDiorite':'minecraft:diorite','stoneGranite':'minecraft:granite',
 'clusterIron':'thaumcraft:cluster_iron','clusterGold':'thaumcraft:cluster_gold','clusterCopper':'thaumcraft:cluster_copper','clusterTin':'thaumcraft:cluster_tin','clusterSilver':'thaumcraft:cluster_silver',
 'clusterLead':'thaumcraft:cluster_lead','clusterCinnabar':'thaumcraft:cluster_cinnabar','clusterQuartz':'thaumcraft:cluster_quartz','gear*':'#c:gears','stone':'#c:stones','obsidian':'#c:obsidians',
 'logWood':'#minecraft:logs','treeSapling':'#minecraft:saplings','treeLeaves':'#minecraft:leaves','vine':'minecraft:vine','feather':'#c:feathers','slimeball':'#c:slime_balls',
 'string':'#c:strings','leather':'#c:leathers','gunpowder':'#c:gunpowders','blockGlass':'#c:glass_blocks','dustRedstone':'#c:dusts/redstone','dustGlowstone':'#c:dusts/glowstone'})
UNKNOWN=[]
def stack_targets(e):
    """ItemStack expression -> list of item ids."""
    if e[0]=='new' and e[1]=='ItemStack':
        a=e[2];meta=a[2][1] if len(a)>2 and a[2][0]=='num' else (a[1][1] if len(a)==2 and a[1][0]=='num' and False else None)
        it=a[0]
        if it[0]=='field' and it[1][0]=='name':
            owner,const=it[1][1],it[2]
            if owner in('Blocks','Items'): return flatten.ids(const,meta)
            if owner in('BlocksTC','ItemsTC'):
                if meta==32767 and const in refmap.VARIANTS: return ['thaumcraft:'+v for v in refmap.VARIANTS[const]]
                try: return [refmap.tc(const,meta)]
                except KeyError: UNKNOWN.append(const);return []
            if owner=='ForgeModContainer' : return []
        if it[0]=='call' and it[2]=='getInstance': return []
        if it[0]=='field' and it[2]=='universalBucket': return []
        if it[0]=='name' and it[1]=='ca': return ['$LOOPVAR']
        UNKNOWN.append(repr(it)[:80]);return []
    if e[0]=='call' and e[2]=='getFilledBucket':
        f=e[3][0][2][0][2][1]  # ConfigBlocks.FluidPure.instance
        return ['thaumcraft:bucket_pure' if 'Pure' in repr(e) else 'thaumcraft:bucket_death']
    if e[0]=='name' and e[1] in('sis','stack','stack2','stack3','stack4'): return ['$SKIP']
    UNKNOWN.append(repr(e)[:80]);return []
def aspect_ops(e):
    ops=[];base=None
    while e[0]=='call':
        _,tgt,meth,args=e
        a=args[0];asp=ASP[a[2]]
        amt=args[1][1] if len(args)>1 else 0
        ops.append([meth,asp,amt]);e=tgt
    if e[0]=='new' and e[1]=='AspectList' and e[2]:
        t=stack_targets(e[2][0]);base=t[0] if t else None
    ops.reverse();return base,ops
def body(name):
    i=SRC.index(f'private static void {name}()');i=SRC.index('{',i)+1;d=1;j=i
    while d:
        d+={'{':1,'}':-1}.get(SRC[j],0);j+=1
    return SRC[i:j-1]
out=[]
for st in statements(body('registerItemAspects')):
    st=st.strip()
    if st.startswith('for (Block ca : BlocksTC.candles'):
        inner=st[st.index('{')+1:st.rindex('}')].strip().rstrip(';')
        e=P(tokens(inner)).expr();b,o=aspect_ops(e[3][1])
        out.append({'op':'complex','targets':[f'thaumcraft:candle_{c}' for c in refmap.COLORS],'ops':o});continue
    if st.startswith('for (Block ca : BlocksTC.banners'):
        inner=st[st.index('{')+1:st.rindex('}')].strip().rstrip(';')
        e=P(tokens(inner)).expr();b,o=aspect_ops(e[3][1])
        out.append({'op':'complex','targets':[f'thaumcraft:banner_{c}' for c in refmap.COLORS],'ops':o});continue
    if st.startswith('for (PotionType'): out.append({'op':'potions'});continue
    if st.startswith('for (int a = 0; a < 16'): continue  # shield base-colour NBT variants: modern shields keep one id
    e=P(tokens(st)).expr()
    if e[0]!='call' or e[2] not in('registerObjectTag','registerComplexObjectTag'): print('skip',st[:80]);continue
    tgt=e[3][0]
    targets=[ORE[tgt[1]]] if tgt[0]=='str' else stack_targets(tgt)
    if tgt[0]=='str' and tgt[1] not in ORE: UNKNOWN.append('ore:'+tgt[1])
    if not targets or targets==['$SKIP']: continue
    base,ops=aspect_ops(e[3][1])
    d={'op':'set' if e[2]=='registerObjectTag' else 'complex','targets':targets,'ops':ops}
    if base:d['base']=base
    out.append(d)
ents=[]
for st in statements(body('registerEntityAspects')):
    st=st.strip()
    if st.startswith('for (Aspect tag'):
        ents.append({'entity':'Thaumcraft.Wisp','per_aspect':True,'ops':[['add','auram',5],['add','volatus',5]],'nbt':'Type'});continue
    e=P(tokens(st)).expr();a=e[3]
    base,ops=aspect_ops(a[1])
    d={'entity':a[0][1],'ops':ops}
    if len(a)>2: d['nbt']={x[2][0][1]:x[2][1][1] for x in a[2:]}
    ents.append(d)
dst=root/'src/main/resources/data/thaumcraft/aspects'
dst.mkdir(parents=True,exist_ok=True)
(dst/'objects.json').write_text(json.dumps(out,indent=1)+'\n')

print(len(out),'object operations;',len(ents),'entity entries; unknown:',sorted(set(UNKNOWN)))
