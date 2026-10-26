# Aspectos de mobs modernos (26.2)

TC6 (1.12) no registra estos mobs. Estos valores son **diseño propio del port en estilo TC6** (no originales): solo aspectos de TC6 y magnitudes similares a mobs análogos de `ConfigAspects`. Están al final de `src/main/resources/assets/thaumcraft/migration/entity_aspects.json`.

Además, el elder guardian recibe los aspectos que TC6 pretendía (`Guardian` + NBT `Elder`: bestia 10, alienis 15, aqua 15), aunque en TC6 1.12 la condición nunca se cumplía.

Omitidos: entidades no vivas (barcos, vagonetas, proyectiles…) y el armor stand, que TC6 tampoco etiquetaba.

| Mob | Aspectos | Motivo |
|---|---|---|
| allay | spiritus 5, volatus 5, desiderium 5, auram 5 | Espíritu volador que busca objetos; como vex pero benigno. |
| armadillo | bestia 10, terra 5, praemunio 10 | Animal terrestre acorazado (escamas para armadura). |
| axolotl | bestia 5, aqua 10, victus 5 | Anfibio acuático con regeneración, cercano al calamar. |
| bee | bestia 5, volatus 5, herba 5, aversio 5 | Insecto polinizador con aguijón. |
| bogged | exanimis 20, humanus 5, herba 5 | Esqueleto del pantano: esqueleto/stray con musgo (herba) en lugar de terra/vinculum. |
| breeze | aer 15, motus 10, aversio 5 | Elemental de viento, análogo al blaze con aer en lugar de ignis. |
| camel | bestia 15, terra 5, aer 5 | Montura grande como caballo/burro. |
| camel_husk | exanimis 10, bestia 5, terra 5, ignis 5 | Camello no-muerto del desierto: entre zombie horse y husk. |
| cat | bestia 10, perditio 5, tenebrae 5 | Felino como el ocelote, nocturno y doméstico. |
| cod | bestia 5, aqua 5 | Pez pequeño; mitad de un calamar. |
| copper_golem | metallum 10, machina 5, ordo 5 | Gólem de cobre que ordena cofres; versión menor del iron golem. |
| creaking | herba 15, tenebrae 10, exanimis 5 | Criatura de madera pálida animada de noche. |
| dolphin | bestia 10, aqua 15, motus 5 | Mamífero marino rápido. |
| drowned | exanimis 20, humanus 10, aqua 5 | Zombie ahogado: zombie con aqua en lugar de terra (como husk usa ignis). |
| fox | bestia 10, terra 5, desiderium 5 | Animal astuto que roba objetos. |
| frog | bestia 5, aqua 5, motus 5 | Anfibio saltador; análogo al conejo con aqua. |
| glow_squid | bestia 5, aqua 10, lux 5 | Calamar luminoso: calamar + lux. |
| goat | bestia 10, terra 10, aversio 5 | Cabra montés que embiste. |
| happy_ghast | bestia 10, volatus 10, aer 10 | Ghast pacífico domesticable (también ghastling): sin exanimis/ignis. |
| hoglin | bestia 15, ignis 5, aversio 10 | Bestia agresiva del Nether. |
| mannequin | humanus 5, fabrico 5 | Maniquí artificial con forma humana. |
| nautilus | bestia 5, aqua 10, praemunio 5 | Molusco marino con concha. |
| panda | bestia 15, herba 5 | Gran herbívoro que come bambú. |
| parched | exanimis 20, humanus 5, ignis 5 | Esqueleto del desierto: como stray pero ignis. |
| phantom | exanimis 10, volatus 10, tenebrae 5 | No-muerto volador nocturno. |
| piglin | humanus 10, ignis 15, desiderium 5 | Zombified piglin sin exanimis; codicioso de oro. |
| piglin_brute | humanus 10, ignis 15, aversio 10 | Piglin guerrero. |
| pufferfish | bestia 5, aqua 5, mortuus 5 | Pez venenoso. |
| salmon | bestia 5, aqua 5 | Pez pequeño como el bacalao. |
| sniffer | bestia 15, terra 10, herba 5 | Bestia antigua que desentierra semillas. |
| strider | bestia 10, ignis 10, motus 5 | Camina sobre lava. |
| sulfur_cube | victus 5, ignis 5, alkimia 10 | Cubo de azufre: slime/magma cube alquímico. |
| tadpole | bestia 5, aqua 5 | Cría de rana. |
| trader_llama | bestia 15, aqua 5, permutatio 5 | Llama + permutatio del comercio. |
| tropical_fish | bestia 5, aqua 5, sensus 5 | Pez colorido. |
| turtle | bestia 10, aqua 5, praemunio 10 | Reptil con caparazón. |
| wandering_trader | humanus 15, permutatio 10 | Aldeano comerciante errante. |
| warden | exanimis 25, tenebrae 25, sensus 20, potentia 20 | Guardián de las profundidades, nivel de jefe menor; ciego que percibe vibraciones. |
| zoglin | exanimis 15, bestia 15, aversio 10 | Hoglin zombificado. |
| zombie_nautilus | exanimis 10, aqua 10, praemunio 5 | Nautilus no-muerto. |
| zombie_villager | exanimis 20, humanus 10, terra 5 | Mismo que el zombie de TC6 (en 1.12 era una variante NBT de Zombie). |
| glow_item_frame | sensus 5, fabrico 5, lux 5 | Item frame de TC6 + lux. |
| pillager | aversio 10, praecantatio 5, humanus 10 | Illager ballestero; vindicator con aversio reforzada. |
| ravager | bestia 20, aversio 15, terra 10 | Bestia de asalto de los illagers, mayor que el lobo/hoglin. |
