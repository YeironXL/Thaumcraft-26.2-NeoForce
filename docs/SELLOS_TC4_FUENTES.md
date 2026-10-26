# Sellos TC4 (pescar, trasvasar, alquimia): fuentes e invenciones

TC6 sustituyó los núcleos de gólem de Thaumcraft 4 por sellos, pero tres núcleos de TC4 no tienen sello equivalente en TC6.
Este port los recrea como sellos de control con las mismas convenciones que los sellos TC6 (`ISeal` + `ISealGui`,
tareas de `TaskHandler`, prioridad/área/opciones/etiquetas en el GUI del sello, objeto `SealPlacerItem`, receta,
entrada de investigación en Golemancia, lang en_us/es_es).

## Fuentes

- Wiki de Thaumcraft 4, página *Golems* (revisión antigua): https://thaumcraft4.fandom.com/wiki/Golems?oldid=6174
  - Núcleo de pesca (avanzado, se obtiene por infusión desde el núcleo de cosecha): «Placed near water, will
    automatically fish in it (catches anything a player can!)». Sólo usa su posición de hogar; sin marcas ni GUI.
  - Núcleo de trasvase / *Decanting* (básico, crisol con Aqua 5 + Vacuos 5): «Fills containers with fluid». Su hogar es
    la entrada de líquido; «For Decanting golems, marks indicate the liquids to fill its home container».
  - Núcleo de alquimia (avanzado, por infusión desde el de trasvase): «Takes essentia from a furnace or other device and
    stores it in a bank of warded jars». Las marcas indican los tarros/contenedores de esencia a llenar.
- Código TC6 de referencia (`/home/claude/w/TC6-ref`) para la estructura de sellos (SealFill, SealHarvest, SealEmpty).

## Correspondencia y decisiones

| TC4 | Sello del port | Clave | Clase |
|---|---|---|---|
| Fishing core | Sello de control: Pescar | `thaumcraft:fishing` | `golems/seals/SealFishing` |
| Decanting (liquid) core | Sello de control: Trasvasar | `thaumcraft:fluid` | `golems/seals/SealFluid` |
| Alchemy (essentia) core | Sello de control: Alquimia | `thaumcraft:essentia` | `golems/seals/SealEssentia` |

Los tres se registran después de los 16 sellos TC6, así que no cambian el orden/ids existentes.

### Pescar
- Fiel a TC4: el gólem pesca en agua cercana y obtiene lo mismo que un jugador → se usa la tabla de botín vanilla
  `gameplay/fishing` con una caña como herramienta.
- Invención: «agua cercana» = bloque fuente de agua con aire encima dentro del área del sello (o un bloque por debajo
  del área, para poder poner el sello en la orilla). El gólem «lanza» (sonido/partículas), espera al menos ~30 ticks y
  luego tiene 1/4 de probabilidad por intento (cada ~10 ticks).
- Invención: la captura se queda en la mano del gólem como con los sellos Vaciar/Recoger; se entrega con Almacenar/Llenar.
  Un gólem sin mano libre no acepta la tarea. Los gólems listos (SMART) pescan con suerte +1.
- Receta (invención, en la línea del sello Cosecha): infusión sobre sello en blanco con caña, bacalao, salmón e hilo;
  Aqua 10, Bestia 10, Humanus 10. Investigación SEALFISHING (padre SEALHARVEST, igual que TC4 derivaba de cosecha).

### Trasvasar
- Fiel a TC4: el «hogar» es el contenedor que se llena (el sello va sobre él) y la receta es de crisol con Aqua + Vacuos
  (aquí 10 + 10 sobre sello en blanco, como los demás sellos de crisol TC6). Investigación SEALFLUID (padre SEALEMPTY).
- Invención: las marcas de TC4 se sustituyen por el área del sello usada como radio alrededor del contenedor
  (área + 1 en cada eje). Fuentes: otros contenedores con capacidad de fluidos de NeoForge y/o bloques fuente del mundo
  (se recogen como con un cubo); dos opciones del GUI activan cada tipo.
- Invención: el gólem lleva 1000 mB (2000 si es HAULER) guardados en sus datos persistentes. Si el destino ya contiene
  un fluido, sólo se trae ese fluido (equivale a la marca que indicaba el líquido en TC4). Sólo fluidos sin componentes.

### Alquimia
- Fiel a TC4: llena tarros protegidos con esencia; es un núcleo avanzado → requiere gólem SMART.
- Invención: el sello va sobre el tarro destino; las fuentes son otros tarros dentro del área-radio. El gólem lleva una
  reserva tipo vial de 10 puntos (20 si es HAULER), igual que el vial de TC6 (10 puntos).
- Invención: el aspecto lo decide el tarro destino (etiqueta; si no, su contenido; si está vacío y sin etiqueta, el
  primero que se encuentre). Por defecto no se vacían tarros etiquetados ni tarros del vacío (opciones del GUI), para
  no deshacer la esencia ya ordenada. TC4 tomaba la esencia de hornos/dispositivos; en este port los alambiques/tubos
  ya alimentan tarros, así que el gólem mueve esencia entre tarros.
- Receta (invención): infusión sobre sello en blanco con 2 viales, tarro y botella; Alkimia 10, Vacuos 10,
  Permutatio 10. Investigación SEALESSENTIA (padres SEALFLUID, porque TC4 derivaba de trasvase, y MINDBIOTHAUMIC).

## Texturas
Iconos en estilo TC6 (`textures/item/seals/seal_{fishing,fluid,essentia}.png`, 16x48 animadas con el mismo `.mcmeta`
que el resto): borde y fondo del sello TC6 con un glifo dorado nuevo (anzuelo, gota, vial).

## Verificación
Gametests `golem_fishing`, `golem_fluid`, `golem_essentia` (GolemancyVerification): el gólem pesca algo en un estanque,
llena un caldero desde un bloque fuente de agua y mueve 20 de ignis de un tarro a otro conservando la esencia.
Escena de depuración: `-PdebugHold=first -PdebugGolems=tc4seals`.
