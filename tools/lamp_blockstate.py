#!/usr/bin/env python3
"""lamp_blockstate.py <block> <off_texture> <on_texture>: TC6 lamp blockstate (lamp core by enabled + metal stand by facing)."""
import json, os, sys
name, off, on = sys.argv[1:4]
B = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/thaumcraft/')
for state, tex in (('off', off), ('on', on)):
    json.dump({"parent": "thaumcraft:block/lamp", "textures": {"particle": "thaumcraft:block/" + off, "side": "thaumcraft:block/" + tex}},
              open(B + f'models/block/{name}_{state}.json', 'w'), indent=1)
json.dump({"parent": "thaumcraft:block/lamp_stand", "textures": {"particle": "thaumcraft:block/base_metal", "side": "thaumcraft:block/base_metal"}},
          open(B + 'models/block/lamp_stand_metal.json', 'w'), indent=1)
rot = {'up': {'x': 180}, 'down': {}, 'north': {'y': 180, 'x': 90}, 'south': {'x': 90}, 'east': {'y': 270, 'x': 90}, 'west': {'y': 90, 'x': 90}}
mp = [{"when": {"enabled": "true"}, "apply": {"model": f"thaumcraft:block/{name}_on"}},
      {"when": {"enabled": "false"}, "apply": {"model": f"thaumcraft:block/{name}_off"}}]
mp += [{"when": {"facing": f}, "apply": dict(model="thaumcraft:block/lamp_stand_metal", **r)} for f, r in rot.items()]
json.dump({"multipart": mp}, open(B + f'blockstates/{name}.json', 'w'), indent=1)
