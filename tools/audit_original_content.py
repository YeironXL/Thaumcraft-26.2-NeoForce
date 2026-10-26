"""Inventory original source units and native resources without claiming feature parity."""
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

root=Path(__file__).resolve().parents[1]
reference=root.parent / 'reference-tc6'
source=reference / 'src/main/java'
units=[]
for file in sorted(source.rglob('*.java')):
    relative=file.relative_to(source).as_posix()
    units.append(dict(path=relative,sha256=hashlib.sha256(file.read_bytes()).hexdigest()))
groups=Counter('/'.join(unit['path'].split('/')[:4]) for unit in units)
recipes=[]
for file in sorted((root / 'src/main/resources/data/thaumcraft/recipe').glob('*.json')):
    recipe=json.loads(file.read_text())
    recipes.append(dict(id='thaumcraft:'+file.stem,type=recipe['type'],research=recipe.get('research'),result=recipe.get('result')))
config=(source / 'thaumcraft/common/config/ConfigRecipes.java').read_text()
original_recipe_calls={name:len(re.findall(r'\b'+name+r'\(',config)) for name in (
    'addArcaneCraftingRecipe','addCrucibleRecipe','addInfusionCraftingRecipe','addMultiblockRecipeToCatalog')}
report=dict(reference=dict(url='https://github.com/TheDarkTower314/Thaumcraft-6-Source-Code',
    commit='954022bb777b7546281fb36df8522f0ba6b43f81',kind='Reconstrucción comunitaria descompilada; no publicación oficial de Azanor'),
    original_java_units=units,original_groups=dict(sorted(groups.items())),
    original_direct_recipe_calls=original_recipe_calls,native_recipes=recipes,
    caveat='Los recuentos de archivos o recetas no miden el porcentaje de comportamiento portado. Los bucles originales generan más recetas que sus llamadas directas.')
docs=root / 'docs'
docs.mkdir(exist_ok=True)
(docs / 'AUDITORIA_TC6.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
counts=Counter(recipe['type'] for recipe in recipes)
text=f'''# Inventario de referencia y pendientes de Thaumcraft 6

Base de comportamiento: Thaumcraft 1.12.2-6.1.BETA26. La rama 1.8.9 sirve como referencia histórica; sus mecánicas no se mezclan automáticamente con TC6.

Fuentes verificadas:
- [Publicación de Azanor](https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2629023).
- [Reconstrucción comunitaria de código](https://github.com/TheDarkTower314/Thaumcraft-6-Source-Code), commit `954022bb777b7546281fb36df8522f0ba6b43f81`.

Se inventariaron **{len(units)} archivos Java** de la referencia. `AUDITORIA_TC6.json` conserva las rutas y hashes para revisar cada sistema. Esto es un inventario, no una afirmación de haber leído y portado cada clase.

El port contiene actualmente **{len(recipes)} recetas nativas**: {dict(counts)}. Los costes y requisitos de las recetas añadidas se contrastaron con `ConfigRecipes.java`; no todas las máquinas u objetos producidos tienen aún su comportamiento.

| Sistema | Estado actual | Trabajo restante |
| --- | --- | --- |
| Aspectos y cristales | 37 aspectos; variantes persistentes; cristales primales en cuevas; gotas por tamaño | Distribución exacta de biomas y caras múltiples de la geometría original |
| Inicio | Cristales, diario, Salis Mundus, Thaumonomicon, etapas y mesa arcana | Prueba manual del recorrido completo y requisitos antiguos aún sin migrar |
| Investigación | Catálogo, etapas, observaciones, descubrimientos y cobro del servidor | Mesa de investigación, teorías, cartas, escaneo del cielo y todos los disparadores especiales |
| Aura | Vis/flux por chunk, gasto y regeneración lunar | Difusión entre chunks, generación y simulación completa de contaminación y grietas |
| Alquimia | Crisol con agua/calor/aspectos; Nitor, Alumentum, metales y cristales | Todas las recetas, etiquetas derivadas, fluidos y transporte por tuberías/tolvas |
| Esencia | Destilación con combustible, alambiques de 128, tubos normales y cinco variantes con succión, filtros, redstone, direcciones y buffer, tarros de 250 con líquido visible y vacío automático; frascos de 10; centrífuga con separación, redstone y persistencia | Aspectos derivados completos, tolvas/interfaz del horno, auxiliares, etiquetas, usos restantes de fuelles y automatización avanzada |
| Auromancia | Recetas iniciales | Guantelete funcional, componentes de focos, manipulador, hechizos y efectos |
| Infusión | Registros/modelos conservados | Altar, pedestales, estabilidad, consumo de esencia, fallos y encantamientos |
| Golemancia | Registros/recetas de referencia inventariados | Constructor, materiales, rasgos, IA, sellos y tareas |
| Dispositivos | Hungry Chest y radio del cargador de mesa | Horno infernal funcional, espejos, lámparas, baterías, generadores y demás dispositivos |
| Herramientas/armaduras | Objetos y recursos conservados; algunas recetas | Estadísticas, durabilidad, equipamiento, habilidades y encantamientos propios |
| Entidades | Datos de escaneo para entidades vanilla | Todas las entidades propias del mod, modelos, animaciones, IA y aparición |
| Mundo | Cristales primales en chunks nuevos | Árboles, plantas, minerales, estructuras, biomas, corrupción y contenido eldritch |
| Visual/sonido | Menús iniciales adaptados; Nitor visible; sonidos registrados | Renderizadores y partículas originales restantes, animaciones y revisión de modelos |

El control general de bugs queda después de la implementación. Las pruebas de compilación, datos y transacciones que acompañan cada cambio son validación de desarrollo.

## Grupos de código original

| Ruta | Archivos Java |
| --- | ---: |
'''
text+=''.join(f'| `{group}` | {count} |\n' for group,count in sorted(groups.items()))
(docs / 'PLAN_DE_PORT_TC6.md').write_text(text,encoding='utf-8')
print(f'Inventario: {len(units)} archivos Java originales; {len(recipes)} recetas nativas; {dict(counts)}')
