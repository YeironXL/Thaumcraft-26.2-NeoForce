#!/usr/bin/env python3
"""Writes 'drops itself' block loot tables for the given block ids (skips existing ones)."""
import json, os, sys
root = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/data/thaumcraft/loot_table/blocks')
for n in sys.argv[1:]:
    p = os.path.join(root, n + '.json')
    if os.path.exists(p): continue
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "thaumcraft:" + n}],
               "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "thaumcraft:blocks/" + n}, open(p, 'w'), indent=2)
    print('wrote', n)
