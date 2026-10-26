"""Package source and a validated production JAR without build caches or player worlds."""
import hashlib
import json
import re
import shutil
import zipfile
from pathlib import Path

root=Path(__file__).resolve().parents[1]
version=re.search(r'^mod_version=(.+)$',(root / 'gradle.properties').read_text(),re.M)[1].strip()
fix=version.rsplit('-',1)[1]
output=root.parents[1] / 'outputs'
output.mkdir(exist_ok=True)
jar=root / f'build/libs/thaumcraft-{version}.jar'
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist()
    assert not any('/verification/' in name or ('/test_instance/' in name or '/test_environment/' in name) for name in names), 'Verification content leaked into release'
    assert version in archive.read('META-INF/neoforge.mods.toml').decode(), 'Incorrect release metadata'
    assert 'art/arcane/thaumcraft/blockentity/CrucibleBlockEntity.class' in names
    assert 'art/arcane/thaumcraft/registry/ModSounds.class' in names
    assert 'art/arcane/thaumcraft/blockentity/ResearchTableBlockEntity.class' in names
    assert 'art/arcane/thaumcraft/research/TheorySession.class' in names
for file in (root / 'src/main/resources').rglob('*.json'):
    json.loads(file.read_text(encoding='utf-8-sig'))
shutil.copyfile(jar,output / jar.name)
package=output / f'Thaumcraft-NeoForge-26.2-{fix}-proyecto.zip'
prefix=f'Thaumcraft-NeoForge-26.2-{fix}'
with zipfile.ZipFile(package,'w',zipfile.ZIP_DEFLATED) as archive:
    for directory in ('src','gradle','tools','docs'):
        for file in sorted((root / directory).rglob('*')):
            if file.is_file() and '__pycache__' not in file.parts:
                archive.write(file,f'{prefix}/{file.relative_to(root).as_posix()}')
    for name in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','README.md',
                 'ESTADO_DEL_PORT.md','ESTADO_DEL_PORT_FIX23.md','ESTADO_DEL_PORT_FIX24.md','ESTADO_DEL_PORT_FIX25.md','ESTADO_DEL_PORT_FIX26.md','ESTADO_DEL_PORT_FIX27.md','ESTADO_DEL_PORT_FIX28.md','ESTADO_DEL_PORT_FIX29.md','ESTADO_DEL_PORT_FIX30.md','ESTADO_DEL_PORT_FIX31.md','ESTADO_DEL_PORT_FIX32.md','ESTADO_DEL_PORT_FIX33.md','ESTADO_DEL_PORT_FIX34.md','ESTADO_DEL_PORT_FIX35.md','ESTADO_DEL_PORT_FIX36.md','ESTADO_DEL_PORT_FIX37.md','ESTADO_DEL_PORT_FIX38.md','ESTADO_DEL_PORT_FIX39.md','ESTADO_DEL_PORT_FIX40.md','ESTADO_DEL_PORT_FIX41.md','ESTADO_DEL_PORT_FIX42.md','ESTADO_DEL_PORT_FIX43.md','ESTADO_DEL_PORT_FIX44.md','ESTADO_DEL_PORT_FIX45.md','ESTADO_DEL_PORT_FIX46.md','ESTADO_DEL_PORT_FIX47.md','ESTADO_DEL_PORT_FIX48.md','ESTADO_DEL_PORT_FIX49.md','ESTADO_DEL_PORT_FIX50.md','ESTADO_DEL_PORT_FIX51.md','ESTADO_DEL_PORT_FIX52.md','ESTADO_DEL_PORT_FIX53.md','ESTADO_DEL_PORT_FIX54.md','ESTADO_DEL_PORT_FIX55.md','ESTADO_DEL_PORT_FIX56.md','ESTADO_DEL_PORT_FIX57.md','ESTADO_DEL_PORT_FIX58.md','ESTADO_DEL_PORT_FIX59.md','ESTADO_DEL_PORT_FIX60.md','ESTADO_DEL_PORT_FIX61.md','ESTADO_DEL_PORT_FIX62.md','ESTADO_DEL_PORT_FIX63.md','ESTADO_DEL_PORT_FIX64.md','ESTADO_DEL_PORT_FIX65.md','ESTADO_DEL_PORT_FIX66.md','ESTADO_DEL_PORT_FIX67.md','ESTADO_DEL_PORT_FIX68.md','ESTADO_DEL_PORT_FIX69.md','ESTADO_DEL_PORT_FIX70.md','ESTADO_DEL_PORT_FIX71.md','ESTADO_DEL_PORT_FIX72.md','ESTADO_DEL_PORT_FIX73.md','ESTADO_DEL_PORT_FIX74.md','ESTADO_DEL_PORT_FIX75.md','ESTADO_DEL_PORT_FIX76.md','ESTADO_DEL_PORT_FIX77.md','ESTADO_DEL_PORT_FIX78.md','ESTADO_DEL_PORT_FIX79.md','ESTADO_DEL_PORT_FIX80.md','ESTADO_DEL_PORT_FIX93.md','ESTADO_DEL_PORT_FIX93C.md','ESTADO_DEL_PORT_FIX94.md','ESTADO_DEL_PORT_FIX94B.md','ESTADO_DEL_PORT_FIX94C.md','ESTADO_DEL_PORT_FIX95.md','ESTADO_DEL_PORT_FIX96.md','ESTADO_DEL_PORT_FIX97.md','ESTADO_DEL_PORT_FIX98.md','ESTADO_DEL_PORT_FIX99.md','ESTADO_DEL_PORT_FIX100.md','ESTADO_DEL_PORT_FIX101.md','ESTADO_DEL_PORT_FIX102.md','ESTADO_DEL_PORT_FIX103.md','ESTADO_DEL_PORT_FIX104.md','ESTADO_DEL_PORT_FIX105.md','ESTADO_DEL_PORT_FIX106.md','ESTADO_DEL_PORT_FIX107.md','ESTADO_DEL_PORT_FIX108.md','ESTADO_DEL_PORT_FIX109.md','ESTADO_DEL_PORT_FIX110.md','ESTADO_DEL_PORT_FIX111.md','ESTADO_DEL_PORT_FIX112.md','ESTADO_DEL_PORT_FIX113.md','ESTADO_DEL_PORT_FIX114.md','ESTADO_DEL_PORT_FIX115.md','ESTADO_DEL_PORT_FIX116.md','ESTADO_DEL_PORT_FIX117.md','ESTADO_DEL_PORT_FIX118.md','ESTADO_DEL_PORT_FIX119.md','ESTADO_DEL_PORT_FIX120.md','ESTADO_DEL_PORT_FIX121.md','ESTADO_DEL_PORT_FIX122.md',f'ESTADO_DEL_PORT_{fix.upper()}.md'):
        archive.write(root / name,f'{prefix}/{name}')
shutil.copyfile(root / f'ESTADO_DEL_PORT_{fix.upper()}.md',output / f'ESTADO_DEL_PORT_{fix.upper()}.md')
shutil.copyfile(root / 'docs/PLAN_DE_PORT_TC6.md',output / 'PLAN_DE_PORT_TC6.md')
shutil.copyfile(root / 'docs/AUDITORIA_TC6.json',output / 'AUDITORIA_TC6.json')
for source,name in [('thaumometer-model-verification.png',f'taumometro-modelo-{fix}.png'),('thaumonomicon-tag-recipe-verification.png',f'receta-etiquetas-thaumonomicon-{fix}.png'),('thaumonomicon-brick-recipe-verification.png',f'receta-ladrillos-thaumonomicon-{fix}.png'),('thaumonomicon-crucible-recipe-verification.png',f'receta-crisol-thaumonomicon-{fix}.png'),('thaumonomicon-normal-recipe-verification.png',f'receta-normal-thaumonomicon-{fix}.png'),('thaumonomicon-recipe-verification.png',f'receta-thaumonomicon-{fix}.png'),('crucible-overflow-verification.png',f'rebose-crisol-{fix}.png'),('crucible-aspect-bubbles-verification.png',f'burbujas-aspectos-{fix}.png'),('focal-spark-verification.png',f'destellos-manipulador-{fix}.png'),('thaumonomicon-illustrations-verification.png',f'ilustraciones-thaumonomicon-{fix}.png'),('thaumonomicon-siblings-verification.png',f'enlaces-alquimia-{fix}.png'),('thaumonomicon-categories-verification.png',f'categorias-thaumonomicon-{fix}.png'),('crucible-froth-verification.png',f'espuma-crisol-{fix}.png'),('fire-particle-verification.png',f'particula-fuego-{fix}.png'),('thaumonomicon-reading-verification.png',f'lectura-thaumonomicon-{fix}.png'),('thaumonomicon-verification.png',f'thaumonomicon-{fix}.png'),('workbench-verification.png',f'mesa-arcana-{fix}.png'),('smelter-verification.png',f'horno-alquimico-{fix}.png'),('research-table-verification.png',f'mesa-investigacion-{fix}.png'),('research-aids-verification.png',f'ayudas-investigacion-{fix}.png'),('focal-manipulator-verification.png',f'manipulador-focos-{fix}.png')]:
    if (root / f'ui-verification-{fix}.log').exists() and (root / f'run/client/screenshots/{source}').exists():
        shutil.copyfile(root / f'run/client/screenshots/{source}',output / name)
evidence=output / f'validacion-{fix}'
evidence.mkdir(exist_ok=True)
for name in (f'ui-verification-{fix}.log',f'server-verification-{fix}.log',f'build-verification-{fix}.log'):
    if (root / name).exists():
        shutil.copyfile(root / name,evidence / name)
if (root / f'server-verification-{fix}.log').exists():
    shutil.copyfile(root / 'build/reports/starter-gametest.xml',evidence / 'starter-gametest.xml')
print(json.dumps(dict(jar=str(output / jar.name),project=str(package),
    jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
    jar_bytes=jar.stat().st_size,project_bytes=package.stat().st_size),indent=2))

for name in ('AUDITORIA_VISUAL_TC6.md','AUDITORIA_VISUAL_TC6.json','REFERENCIA_VISUAL_VIDEO_TC6.md'):
    shutil.copyfile(root / 'docs' / name,output / name)
