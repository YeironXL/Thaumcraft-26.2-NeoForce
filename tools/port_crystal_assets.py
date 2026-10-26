"""Adapt the retained original crystal mesh to native OBJ loading and block states."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/thaumcraft'
source = (assets / 'models/obj/crystal.obj').read_text()
registry = root / 'src/main/java/art/arcane/thaumcraft/ModBlocks.java'
text = registry.read_text()
colors = dict(aer=0xffff7e, terra=0x56c000, ignis=0xff5a01, aqua=0x3cd4fc,
              ordo=0xd5d4ec, perditio=0x404040, vitium=0x800080)

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

for aspect, color in colors.items():
    text = text.replace(f'BLOCKS.registerSimpleBlock("crystal_{aspect}", () -> BlockBehaviour.Properties.of())',
        f'BLOCKS.registerBlock("crystal_{aspect}", p -> new art.arcane.thaumcraft.block.CrystalClusterBlock("{aspect}", p), () -> BlockBehaviour.Properties.of())')
    rgb = ' '.join(f'{(color >> shift & 255)/255:.6f}' for shift in (16,8,0))
    (assets / f'models/obj/crystal_{aspect}.mtl').write_text(
        f'newmtl Material__25\nKd {rgb}\nKa {rgb}\nd 1.0\nmap_Kd #texture\n')
    for size in range(4):
        lines, group = [], -1
        for line in source.splitlines():
            if line.startswith('mtllib '):
                line = f'mtllib crystal_{aspect}.mtl'
            if line.startswith('g '):
                group += 1
            if line.startswith('f ') and group >= size + 1:
                continue
            lines.append(line)
        mesh = f'crystal_{aspect}_{size}'
        (assets / f'models/obj/{mesh}.obj').write_text('\n'.join(lines) + '\n')
        write(assets / f'models/block/{mesh}.json', {
            'parent':'minecraft:block/block', 'loader':'neoforge:obj',
            'model':f'thaumcraft:models/obj/{mesh}.obj', 'automatic_culling':False,
            'flip_v':True, 'textures':{'texture':'thaumcraft:block/crystal','particle':'thaumcraft:block/crystal'}})
    variants = {}
    for facing, x, y in [('up',0,0),('down',180,0),('north',90,0),('south',90,180),('east',90,90),('west',90,270)]:
        for size in range(4):
            variants[f'facing={facing},size={size}'] = dict(model=f'thaumcraft:block/crystal_{aspect}_{size}',x=x,y=y)
    write(assets / f'blockstates/crystal_{aspect}.json', {'variants':variants})
    write(assets / f'models/item/crystal_{aspect}.json', {'parent':f'thaumcraft:block/crystal_{aspect}_3'})
registry.write_text(text, encoding='utf-8')
data = root / 'src/main/resources/data'
write(data / 'thaumcraft/worldgen/configured_feature/vis_crystals.json', {'type':'thaumcraft:vis_crystals','config':{}})
write(data / 'thaumcraft/worldgen/placed_feature/vis_crystals.json', {
    'feature':'thaumcraft:vis_crystals','placement':[
        {'type':'minecraft:count','count':16}, {'type':'minecraft:in_square'},
        {'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-32},'max_inclusive':{'absolute':48}}},
        {'type':'minecraft:biome'}]})
write(data / 'thaumcraft/neoforge/biome_modifier/vis_crystals.json', {
    'type':'neoforge:add_features','biomes':'#minecraft:is_overworld','features':['thaumcraft:vis_crystals'],'step':'underground_decoration'})
