# Cinturón taumostático (Thaumostatic Girdle): fuentes y decisiones

TC6 no tiene el cinturón. Esta versión recrea el de TC4 como complemento del arnés taumostático (ver `ARNES_FUENTES.md`). Las cifras sin fuente se marcan **[INVENCIÓN]**.

## Fuentes consultadas
- FTB Wiki, *Thaumostatic Girdle (Thaumcraft 4)*: https://ftb.fandom.com/wiki/Thaumostatic_Girdle_(Thaumcraft_4)
- Wiki de Thaumcraft 5, *Thaumostatic Girdle*: https://thaumcraft-5.fandom.com/wiki/Thaumostatic_Girdle

## Datos con fuente (FTB Wiki, TC4)
- Bauble de la ranura de **cinturón**.
- Aumenta la velocidad del arnés taumostático; el arnés gasta algo más de combustible con él puesto (la wiki marca esta frase como dudosa).
- **Evita el daño por caída** (el Thaumonomicon citado dice que «basta para evitar la mayor parte»). El port lo anula del todo, como pidió el usuario.
- La wiki no da receta ni investigación.

## Datos de TC5 que se reutilizan
- Investigación en Artificio con prerrequisito el arnés: `HOVERGIRDLE` tiene como padre `HOVERHARNESS`.
- Receta de infusión: cinturón corriente + 2 lingotes de oro, 2 plumas, 1 fragmento de Aer y 1 de Terra; 32 Aer, 32 Potentia, 16 Motus, 16 Volatus. Inestabilidad «muy alta» → 7 en la escala de TC6 **[INVENCIÓN en la cifra]**.
- En TC5 la velocidad sube de unos 2,25 a unos 10 bloques/s. El arnés del port usa el vuelo vanilla, así que no se copia la cifra.

## Invenciones del port
- Velocidad: con el arnés volando y el cinturón puesto, `flyingSpeed` pasa de 0,05 (vanilla) a 0,1, es decir, el doble **[INVENCIÓN]**. Solo se aplica mientras se vuela con el arnés; al aterrizar, apagarlo, quedarse sin combustible o quitarse el cinturón se restaura 0,05 (y solo si lo cambió el cinturón).
- Consumo: 1 de Potentia cada 15 ticks en vez de cada 20 (unos 1,33/s frente a 1/s) **[INVENCIÓN]**.
- El cinturón anula el daño por caída aunque no se lleve el arnés **[INVENCIÓN en el alcance]**.
- Sin Curios: el cinturón cuenta si está en cualquier parte del inventario o en la mano secundaria **[INVENCIÓN]** (los demás cinturones no tienen alternativa sin Curios).
- Textura: cinturón de cuero con hebilla de latón y núcleo de Potentia, hecha a mano en el estilo de TC6 **[INVENCIÓN]**. No hay render puesto (los otros cinturones tampoco lo tienen).
- Posición en el libro: `[9,3]`, a la derecha del arnés; conocimiento requerido 1 teoría de Artificio y 1 de Infusión **[INVENCIÓN]**.
