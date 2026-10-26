import re,json,sys,os
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from jparse import tokens,P,statements
import refmap
SRC=open(os.path.join(refmap.REF,'common/config/ConfigRecipes.java')).read()
ASP={}
for m in re.finditer(r'(?m)^\s*public static Aspect (\w+) = new Aspect\("(\w+)"',open(os.path.join(refmap.REF,'api/aspects/Aspect.java')).read()): ASP[m.group(1)]=m.group(2)
class Unsupported(Exception):pass
def method_body(name):
    i=SRC.index(f'public static void {name}(');i=SRC.index('{',i)+1;d=1;j=i
    while d: 
        if SRC[j]=='{':d+=1
        if SRC[j]=='}':d-=1
        j+=1
    return SRC[i:j-1]
def aspects(e):
    out={}
    while e and e[0]=='call':
        _,tgt,meth,args=e
        if meth in('add','merge'):
            a=args[0]
            if a[0]=='field' and a[1]==('name','Aspect'): out[ASP[a[2]]]=out.get(ASP[a[2]],0)+args[1][1] if meth=='add' else max(out.get(ASP[a[2]],0),args[1][1])
            else: raise Unsupported(a)
        e=tgt
    return out
def stack(e):
    """-> (id or tag, count, components dict)"""
    if e[0]=='str':
        o=e[1]
        if o in refmap.ORE: return refmap.ORE[o],1,{}
        raise Unsupported('ore '+o)
    if e[0]=='field':
        owner=e[1][1] if e[1][0]=='name' else None
        if owner in('ItemsTC','BlocksTC'): return refmap.tc(e[2]),1,{}
        if owner in('Items','Blocks'): return refmap.vanilla(owner,e[2]),1,{}
        if owner=='ConfigItems' and e[2].endswith('_CRYSTAL'): return 'thaumcraft:crystal_essence',1,{'thaumcraft:aspect':ASP[e[2][:-8]]}
        raise Unsupported(e)
    if e[0]=='new' and e[1]=='ItemStack':
        a=e[2];count=a[1][1] if len(a)>1 and a[1][0]=='num' else 1
        meta=a[2][1] if len(a)>2 and a[2][0]=='num' else None
        it=a[0]
        if it[0]=='call' and it[2]=='get' and it[1][0]=='field':
            col=it[3][0][2].lower().replace('_','');col={'lightblue':'lightblue','silver':'silver'}.get(col,col)
            return refmap.tc(it[1][2],color=col),count,{}
        if it[0]=='call' and it[2]=='getDefaultState': it=it[1]
        if it[0]=='field' and it[1][0]=='name':
            if it[1][1] in('ItemsTC','BlocksTC'): return refmap.tc(it[2],meta),count,{}
            return refmap.vanilla(it[1][1],it[2],meta),count,{}
        raise Unsupported(it)
    if e[0]=='call' and e[2]=='makeCrystal': return 'thaumcraft:crystal_essence',1,{'thaumcraft:aspect':ASP[e[3][0][2]]}
    if e[0]=='call' and e[2]=='makeFilledPhial': return 'thaumcraft:phial_filled',1,{'thaumcraft:essentia':{ASP[e[3][0][2]]:10}}
    if e[0]=='call' and e[1]==('name','BlocksTC') : raise Unsupported(e)
    if e[0]=='call' and e[2]=='get' and e[1][0]=='field':
        return refmap.tc(e[1][2],color=e[3][0][2].lower().replace('_','')),1,{}
    if e[0]=='new' and e[1]=='IngredientNBTTC': return stack(e[2][0])
    if e[0]=='call' and e[2]=='fromItem': return stack(e[3][0])
    if e[0]=='call' and e[2]=='getFilledBucket': return 'thaumcraft:bucket_death',1,{}
    if e[0]=='call' and e[2]=='getSealStack': raise Unsupported('seal')
    raise Unsupported(e)
def ingredient(e):
    i,c,comp=stack(e)
    if comp:
        return {'type':'neoforge:components','items':i,'components':comp,'strict':False} if not i.startswith('#') else i
    return i
def result(e):
    i,c,comp=stack(e)
    r={'id':i}
    if c!=1:r['count']=c
    if comp:r['components']=comp
    return r
def grid(args):
    pat=[];i=0
    if args and args[0][0]=='array': args=args[0][2]
    while i<len(args) and args[i][0]=='str': pat.append(args[i][1]);i+=1
    key={}
    while i<len(args):
        ch=args[i][1];key[ch]=ingredient(args[i+1]);i+=2
    return pat,key
def flat(args):
    if len(args)==1 and args[0][0]=='array': args=args[0][2]
    return [ingredient(a) for a in args]
def rl(e):
    if e[0]=='new' and e[1]=='ResourceLocation':
        s=[a[1] for a in e[2]];return (s[0]+':'+s[1]) if len(s)==2 else s[0]
    raise Unsupported(e)
OUT={};ERR=[]
def add(name,data):
    OUT[name]=data
def handle(st,env):
    try:
        p=P(tokens(st))
        if st.startswith(('ResourceLocation ','int ','IRecipe','ArrayList','CrucibleRecipe[]','ItemStack ','Part','Object','AspectList','Aspect ','String','for','++','a++','InfusionRecipe','NBTTagCompound','NBTBase','Iterator','ShapedArcaneRecipe')): 
            m=re.match(r'ResourceLocation (\w+) = (.*)',st,re.S)
            if m: env[m.group(1)]=rl(P(tokens(m.group(2))).expr())
            return
        e=p.expr()
        if e[0]!='call': return
        meth=e[2];args=e[3]
        if meth=='addCrucibleRecipe':
            name=rl(args[0]);r=args[1];a=r[2]
            d={'type':'thaumcraft:crucible','research':a[0][1],'catalyst':ingredient(a[2]),'aspects':aspects(a[3]),'result':result(a[1])}
            add(name,d)
        elif meth=='addArcaneCraftingRecipe':
            name=rl(args[0]);r=args[1];a=r[2]
            research=a[1][1];vis=a[2][1];crys=aspects(a[3]) if a[3][0]!='name' else {}
            if r[1]=='ShapedArcaneRecipe':
                pat,key=grid(a[5:]);add(name,{'type':'thaumcraft:arcane','research':research,'vis':vis,'crystals':crys,'pattern':pat,'key':key,'result':result(a[4])})
            elif r[1]=='ShapelessArcaneRecipe':
                add(name,{'type':'thaumcraft:shapeless_arcane','research':research,'vis':vis,'crystals':crys,'ingredients':flat(a[5:]),'result':result(a[4])})
            else: raise Unsupported(r[1])
        elif meth=='addInfusionCraftingRecipe':
            name=rl(args[0]);r=args[1];a=r[2]
            if r[1]!='InfusionRecipe': raise Unsupported(r[1])
            out=a[1]
            res=result(out) if out[0]!='array' else None
            add(name,{'type':'thaumcraft:infusion','research':a[0][1],'instability':a[2][1],'aspects':aspects(a[3]),'catalyst':ingredient(a[4]),'components':flat(a[5:]),'result':res})
        elif meth in('oreDictRecipe','shapelessOreDictRecipe'):
            name='thaumcraft:'+args[0][1]
            grp=env.get(args[1][1]) if args[1][0]=='name' else None
            if meth=='oreDictRecipe':
                pat,key=grid(args[3:]);d={'type':'minecraft:crafting_shaped','pattern':pat,'key':key,'result':result(args[2])}
            else: d={'type':'minecraft:crafting_shapeless','ingredients':flat(args[3:]),'result':result(args[2])}
            if grp:d['_group']=grp
            add(name,d)
        elif meth=='addShapedRecipe':
            name=rl(args[0]);pat,key=grid(args[3:]);add(name,{'type':'minecraft:crafting_shaped','pattern':pat,'key':key,'result':result(args[2])})
        elif meth=='addFakeCraftingRecipe':
            name=rl(args[0]);r=args[1];a=r[2]
            if r[1]=='ShapelessOreRecipe': add(name,{'_fake':True,'type':'minecraft:crafting_shapeless','ingredients':flat(a[2:]),'result':result(a[1])})
            elif r[1]=='ShapedOreRecipe': pat,key=grid(a[2:]);add(name,{'_fake':True,'type':'minecraft:crafting_shaped','pattern':pat,'key':key,'result':result(a[1])})
            else: raise Unsupported(r[1])
        elif meth=='addSmelting':
            src=stack(args[0]);add('smelt:'+src[0],{'type':'minecraft:smelting','ingredient':src[0],'result':result(args[1]),'experience':args[2][1]})
        elif meth=='addSmeltingBonus':
            pass
        elif meth=='setGroup' or meth=='addMultiblockRecipeToCatalog': pass
        elif meth=='put': pass
    except (Unsupported,KeyError,IndexError,SyntaxError,TypeError) as x:
        ERR.append((st[:160].replace('\n',' '),repr(x)[:200]))
for m in ['initializeAlchemyRecipes','initializeArcaneRecipes','initializeInfusionRecipes','initializeNormalRecipes','initializeSmelting']:
    env={'defaultGroup':None}
    body=method_body(m)
    # flatten: drop for-loops (handled manually)
    for st in statements(body):
        st=st.strip()
        if st.startswith('for ') or st.startswith('for('): 
            ERR.append(('LOOP '+st[:120].replace('\n',' '),'manual'));continue
        handle(st,env)
if __name__=='__main__':
    print(len(OUT),'converted;',len(ERR),'problems')
    for s,e in ERR: print('!!',s,'\n    ',e)
