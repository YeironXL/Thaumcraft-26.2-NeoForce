#!/usr/bin/env python3
"""facing_blockstate.py <block> <model> [enabled]: TC6 IBlockFacing blockstate (model points up by default)."""
import json, sys, os
name, model = sys.argv[1], sys.argv[2]
enabled = len(sys.argv) > 3
rot = {'down': {'x': 180}, 'up': {}, 'east': {'y': 90, 'x': 90}, 'west': {'y': 270, 'x': 90}, 'north': {'x': 90}, 'south': {'y': 180, 'x': 90}}
v = {}
for e in (('false', 'true') if enabled else (None,)):
    for f, r in rot.items():
        key = (f'enabled={e},' if e else '') + f'facing={f}'
        v[key] = dict(model=model, **r)
p = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/thaumcraft/blockstates', name + '.json')
json.dump({'variants': v}, open(p, 'w'), indent=2)
