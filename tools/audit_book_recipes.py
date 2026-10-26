"""Audit book recipe references against migrated datapack identifiers; not functionality certification."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
recipes={"thaumcraft:"+p.stem:json.loads(p.read_text(encoding="utf-8-sig")) for p in (root/"src/main/resources/data/thaumcraft/recipe").glob("*.json")}
rows=[]
for file in sorted((root/"src/main/resources/assets/thaumcraft/research").glob("*.json")):
 data=json.loads(file.read_text(encoding="utf-8-sig"))
 entries=data.get("entries",[]) if isinstance(data,dict) else data
 for entry in entries:
  for section in ("stages","addenda"):
   for index,stage in enumerate(entry.get(section,[]),1):
    for key in stage.get("recipes",[]):
     normalized=key.lower()
     match=key if key in recipes else normalized if normalized in recipes else None
     rows.append({"research":entry.get("key"),"section":section,"stage":index,"reference":key,"datapack_match":match,"reviewed_book_alias":key=="thaumcraft:BrickArcane","status":"exact identifier" if key in recipes else "case-only candidate" if match else "unresolved: inspect original catalog/group/fake/blueprint","type":recipes[match].get("type") if match else None})
out=root/"docs/AUDITORIA_RECETAS_LIBRO_TC6.json"
out.write_text(json.dumps({"baseline":"fix76","limitation":"Identifier coincidence only; aliases, ingredient equivalence, server research gates, groups, fake recipes and blueprints require individual verification.","references":rows},ensure_ascii=False,indent=2),encoding="utf-8")
unique={row["reference"] for row in rows}
exact={row["reference"] for row in rows if row["status"]=="exact identifier"}
case={row["reference"] for row in rows if row["status"]=="case-only candidate"}
unresolved=unique-exact-case
lines=["# Auditoría de recetas del Thaumonomicon", "", "Estado de referencia: fix76. El libro dibuja algunas recetas normales, arcanas y de crisol; no todas las referencias tienen página. Este inventario compara identificadores, no acredita recetas equivalentes ni implementación funcional.","",f"Referencias únicas: {len(unique)}; coincidencias exactas: {len(exact)}; candidatas solo por minúsculas: {len(case)}; sin resolver: {len(unresolved)}.","", "El original GuiResearchPage.generateRecipesLists conserva el orden de cada etapa y añade addenda solo si su investigación está desbloqueada. addRecipesToList busca catálogo, recetas ilustrativas, CraftingManager y grupos; expande grupos conservando una pestaña. Los planos además comprueban su propia investigación.","", "drawRecipe distingue fabricación arcana, normal, crisol, infusión y planos. La página arcana utiliza gui_researchbook_overlay.png: cuadrícula UV112,15 de52x52; resultado UV20,3 de16x16, escalados por2. Distribuye ingredientes en3x3 a32 píxeles, resultado encima, cristales debajo y coste vis más abajo. No se han inspeccionado nuevas escenas de video en esta auditoría.","", "Alias implementado y contrastado: thaumcraft:BrickArcane → thaumcraft:brickarcane, patrón KK/KK, cuatro piedras arcanas → cuatro ladrillos. Fuente ConfigRecipes líneas452–453. No convertir automáticamente las demás coincidencias por minúsculas en alias: comprobar resultado, ingredientes y costes. No representar las referencias sin resolver como recetas completas.","", "| Investigación | Sección/etapa | Referencia | Candidata en datapack |", "|---|---|---|---|"]
for row in rows:lines.append(f"| {row['research']} | {row['section']} {row['stage']} | `{row['reference']}` | `{row['datapack_match']}` |" if row['datapack_match'] else f"| {row['research']} | {row['section']} {row['stage']} | `{row['reference']}` | Pendiente |")
(root/"docs/AUDITORIA_RECETAS_LIBRO_TC6.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
print(json.dumps({"references":len(rows),"unique":len(unique),"exact":len(exact),"case_only_candidates":len(case),"unresolved":len(unresolved)}))
