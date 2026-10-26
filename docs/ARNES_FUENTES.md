# Arnés taumostático (Thaumostatic Harness): fuentes y decisiones

TC6 no tiene el arnés. Esta versión recrea el de TC4 (`ItemHoverHarness`) adaptada al port. No pude conseguir el código ni las texturas de TC4, así que las cifras sin fuente se marcan **[INVENCIÓN]**.

## Fuentes consultadas
- FTB Wiki, *Thaumostatic Harness (Thaumcraft 4)*: https://ftb.gamepedia.com/Thaumostatic_Harness_(Thaumcraft_4)
- FTB Wiki, *Thaumostatic Girdle (Thaumcraft 4)*: https://ftb.fandom.com/wiki/Thaumostatic_Girdle_(Thaumcraft_4)
- Wiki de Thaumcraft 5, comentario en *Talk:Thaumostatic Harness* (receta, investigación y consumo **de TC5**): https://thaumcraft-5.fandom.com/wiki/Talk:Thaumostatic_Harness/@comment-73.10.124.5-20161007042721/@comment-24896639-20161007204131
- No encontré una página del arnés en la wiki de TC4 (thaumcraft-4.fandom.com).

## Datos con fuente (FTB Wiki, TC4)
- Ranura de pecho, 3 puntos de armadura, 400 de durabilidad.
- Con combustible, el jugador puede volar. A diferencia del vuelo creativo, el vuelo **no se desactiva al tocar el suelo**.
- La tecla **H** (por defecto) lo activa y lo desactiva.
- Funciona con **Potentia pura** (no con vis) que se toma de **tarros sellados**. Mientras está activo consume Potentia de forma continua.
- Se fabrica por **infusión** con inestabilidad **alta**.
- La wiki no da la capacidad ni el ritmo de consumo.
- El cinturón taumostático aumenta mucho la velocidad de vuelo y gasta algo más de combustible (implementado, ver `CINTURON_FUENTES.md`).
- Descuento de vis del 2 % y otro del 5 % en Aer (no implementado, porque TC6 no usa descuentos por pieza).

## Datos de TC5 que se reutilizan
- Investigación en Artificio con prerrequisitos *Levitador arcano*, *Infusión* y *Metalurgia mágica*: `HOVERHARNESS` tiene como padres `LEVITATOR`, `INFUSION` y `METALLURGY`.
- Receta: infundir una túnica de cuero con 2 lingotes de hierro, placas de latón, fragmentos de Aer, tablas de gran madera y un resonador mórfico, con 32 de Machina, 32 de Volatus, 32 de Potentia y 16 de Motus. Se usan 2 placas de latón, 2 cristales de Aer y 1 tabla **[INVENCIÓN en las cantidades]**. La inestabilidad es 5 en la escala de TC6, que equivale a «alta» **[INVENCIÓN en la cifra]**.
- Consumo de 1 por segundo. TC5 gastaba 1 de Aer y 1 de Ignis por segundo. Aquí se gasta 1 de Potentia por segundo **solo mientras se vuela** (cuando el vuelo está permitido pero no se usa, no gasta).

## Invenciones del port
- Capacidad de 500 de Potentia, equivalente a 2 tarros de TC6 (unos 8 min de vuelo) **[INVENCIÓN]**.
- Carga: se usa el arnés (clic derecho) sobre un tarro de Potentia y absorbe todo lo que cabe **[INVENCIÓN en la mecánica exacta]**.
- Aterrizaje seguro: si el vuelo se retira en el aire (combustible agotado, apagado con H o arnés quitado), se reinicia la caída y se aplica Lentitud de caída durante 5 s **[INVENCIÓN]**. Mientras se vuela con el arnés no se acumula daño por caída.
- El vuelo lo concede y lo retira solo el servidor (`mayfly`), y nunca toca el vuelo de creativo ni de espectador.
- Partículas `END_ROD` bajo el jugador mientras vuela **[INVENCIÓN]**.
- HUD: medidor de Potentia abajo a la derecha, con los sprites del medidor del HUD del caster. Con mayúsculas se ve el número. El tooltip muestra la carga y el estado.
- Textura del objeto y de la armadura (chaleco de cuero con correas y carcasa de latón con núcleo de Potentia en la espalda): hecha a mano en el estilo de TC6 **[INVENCIÓN]**, porque la de TC4 no estaba disponible.
- No es un bauble de Curios, sino armadura normal de pecho.
