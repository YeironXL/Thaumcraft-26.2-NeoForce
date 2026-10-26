"""Rewrites basic/world block registrations in ModBlocks with TC6 properties."""
import re
P='src/main/java/art/arcane/thaumcraft/ModBlocks.java'
s=open(P).read()
B='BlockBehaviour.Properties.of()'
SND='net.minecraft.world.level.block.SoundType.'
MC='net.minecraft.world.level.material.MapColor.'
BW='art.arcane.thaumcraft.block.world.'
def stone(h=2.0,r=6.0): return f'{B}.mapColor({MC}STONE).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM).strength({h}F, {r}F).sound({SND}STONE).requiresCorrectToolForDrops()'
def wood(h,r,extra=''): return f'{B}.mapColor({MC}WOOD).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS).strength({h}F, {r}F).sound({SND}WOOD).ignitedByLava(){extra}'
leaves=lambda c: f'{B}.mapColor({MC}{c}).strength(0.2F).randomTicks().sound({SND}GRASS).noOcclusion().isValidSpawn((a,b,c2,d)->false).isSuffocating((a,b,c2)->false).isViewBlocking((a,b,c2)->false).ignitedByLava().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).isRedstoneConductor((a,b,c2)->false)'
plant=lambda light,off: f'{B}.mapColor({MC}PLANT).noCollision().instabreak().sound({SND}GRASS).lightLevel(x -> {light}).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)'+('.offsetType(net.minecraft.world.level.block.state.BlockBehaviour.OffsetType.XZ)' if off else '')
D={}
for n in ['stone_arcane','stone_arcane_brick','stone_ancient','stone_ancient_tile','stone_ancient_glyphed','matrix_speed','matrix_cost']: D[n]=('Block::new',stone())
D['stone_ancient_rock']=('Block::new',stone(-1.0,6.0)); D['stone_ancient_doorway']=('Block::new',stone(-1.0,6.0))
D['stone_eldritch_tile']=('Block::new',stone(15.0,600.0))
D['stone_porous']=(BW+'StonePorousBlock::new',stone(1.0,3.0))
for n in ['arcane_stone','arcane_brick','ancient','eldritch']:
    D['slab_'+n]=('net.minecraft.world.level.block.SlabBlock::new',stone(2.0,6.0)); D['slab_double_'+n]=('Block::new',stone(2.0,6.0))
D['slab_greatwood']=('net.minecraft.world.level.block.SlabBlock::new',wood(1.2,1.2)); D['slab_double_greatwood']=('Block::new',wood(1.2,1.2))
D['slab_silverwood']=('net.minecraft.world.level.block.SlabBlock::new',wood(1.0,1.2)); D['slab_double_silverwood']=('Block::new',wood(1.0,1.2))
D['plank_greatwood']=('Block::new',wood(2.0,2.0)); D['plank_silverwood']=('Block::new',wood(2.0,2.0))
D['log_greatwood']=('net.minecraft.world.level.block.RotatedPillarBlock::new',wood(2.0,3.0))
D['log_silverwood']=('net.minecraft.world.level.block.RotatedPillarBlock::new',wood(2.0,3.0,'.lightLevel(x -> 5)'))
D['leaves_greatwood']=('p -> new net.minecraft.world.level.block.TintedParticleLeavesBlock(0.01F, p)',leaves('PLANT'))
D['leaves_silverwood']=('p -> new net.minecraft.world.level.block.TintedParticleLeavesBlock(0.01F, p)',leaves('COLOR_LIGHT_BLUE'))
D['sapling_greatwood']=(f'p -> new {BW}TcSaplingBlock(true, p)',f'{B}.mapColor({MC}PLANT).noCollision().randomTicks().instabreak().sound({SND}GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)')
D['sapling_silverwood']=(f'p -> new {BW}TcSaplingBlock(false, p)',D['sapling_greatwood'][1])
D['cinderpearl']=(f'p -> new {BW}TcPlantBlock({BW}TcPlantBlock.Kind.CINDERPEARL, p)',plant(7,True))
D['shimmerleaf']=(f'p -> new {BW}TcPlantBlock({BW}TcPlantBlock.Kind.SHIMMERLEAF, p)',plant(6,True))
D['vishroom']=(f'p -> new {BW}TcPlantBlock({BW}TcPlantBlock.Kind.VISHROOM, p)',plant(6,False))
xp='p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(1, 4), p)'
D['ore_amber']=(xp,stone(1.5,3.0)); D['ore_quartz']=(xp,stone(3.0,3.0)); D['ore_cinnabar']=('Block::new',stone(2.0,3.0))
D['amber_block']=('net.minecraft.world.level.block.HalfTransparentBlock::new',f'{B}.mapColor({MC}COLOR_ORANGE).strength(0.5F).sound({SND}STONE).noOcclusion().isRedstoneConductor((a,b,c)->false).isSuffocating((a,b,c)->false).isViewBlocking((a,b,c)->false)')
D['amber_brick']=D['amber_block']
for n in ['brass','thaumium','void','alchemical','alchemical_advanced']: D['metal_'+n]=('Block::new',f'{B}.mapColor({MC}METAL).strength(4.0F, 6.0F).sound({SND}METAL).requiresCorrectToolForDrops()')
D['table_wood']=('Block::new',wood(2.0,2.0,'.noOcclusion()'))
D['table_stone']=('Block::new',stone(2.5,2.5)+'.noOcclusion()')
dyes={'white':'WHITE','orange':'ORANGE','magenta':'MAGENTA','lightblue':'LIGHT_BLUE','yellow':'YELLOW','lime':'LIME','pink':'PINK','gray':'GRAY','silver':'LIGHT_GRAY','cyan':'CYAN','purple':'PURPLE','blue':'BLUE','brown':'BROWN','green':'GREEN','red':'RED','black':'BLACK'}
for k,v in dyes.items(): D['candle_'+k]=(f'p -> new {BW}TcCandleBlock(net.minecraft.world.item.DyeColor.{v}, p)',B)
n=0
def rep(m):
    global n
    name=m.group(2)
    if name not in D: return m.group(0)
    n+=1
    f,pr=D[name]
    return f'{m.group(1)}BLOCKS.registerBlock("{name}", {f}, () -> {pr});'
s=re.sub(r'(public static final DeferredHolder<Block, Block> [A-Z_0-9]+ = )BLOCKS\.register(?:Simple)?Block\("([a-z_0-9]+)"[^;]*;',rep,s)
# stairs: move to end, needing base states
base={'stairs_arcane':('STONE_ARCANE',stone()),'stairs_arcane_brick':('STONE_ARCANE_BRICK',stone()),'stairs_ancient':('STONE_ANCIENT',stone()),'stairs_greatwood':('PLANK_GREATWOOD',wood(2.0,2.0)),'stairs_silverwood':('PLANK_SILVERWOOD',wood(2.0,2.0))}
moved=[]
for name,(b,pr) in base.items():
    m=re.search(r'\n    public static final DeferredHolder<Block, Block> ([A-Z_]+) = BLOCKS\.register(?:Simple)?Block\("'+name+r'"[^;]*;',s)
    s=s.replace(m.group(0),'')
    moved.append(f'    public static final DeferredHolder<Block, Block> {m.group(1)} = BLOCKS.registerBlock("{name}", p -> new net.minecraft.world.level.block.StairBlock({b}.get().defaultBlockState(), p), () -> {pr});'); n+=1
idx=[m.end() for m in re.finditer(r'public static final DeferredHolder<Block, Block> [^\n]*\n',s)][-1]
s=s[:idx]+'    // Stairs last: StairBlock needs its base block registered first.\n'+'\n'.join(moved)+'\n'+s[idx:]
open(P,'w').write(s); print('rewrote',n,'of',len(D)+len(base))
