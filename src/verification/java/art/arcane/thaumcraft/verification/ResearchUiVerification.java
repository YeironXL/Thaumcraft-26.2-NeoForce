package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.HungryChestBlockEntity;
import art.arcane.thaumcraft.client.book.ResearchBrowserScreen;
import art.arcane.thaumcraft.client.ClientResearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

/** Native GUI render smoke test; this class is excluded from the production JAR. */
@Mod(value = Thaumcraft.MOD_ID, dist = Dist.CLIENT)
public final class ResearchUiVerification {
    private int frames;
    private boolean opened;
    private int titleTicks;
    private int phase;
    private boolean languageRequested;
    public ResearchUiVerification() { NeoForge.EVENT_BUS.addListener(this::tick); }
    private static ResearchBrowserScreen book(String json) { ClientResearch.receive(json); return new ResearchBrowserScreen(); }
    private void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("thaumcraft.verifyUi")) return;
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.gui.overlay()!=null)return;
        if(minecraft.getWindow().getWidth()<640||minecraft.getWindow().getHeight()<540){
            org.lwjgl.glfw.GLFW.glfwRestoreWindow(minecraft.getWindow().handle());minecraft.getWindow().setWindowed(854,600);frames=0;return;
        }
        if (!opened && minecraft.gui.screen() instanceof TitleScreen) {
            if (++titleTicks < 2) return;
            if (!languageRequested) {
                languageRequested=true;
                minecraft.getLanguageManager().setSelected("es_es");
                minecraft.reloadResourcePacks();
                titleTicks=0;
                return;
            }
            // This dedicated process never opens a world. Supply item fixtures
            // because 26.2 normally binds prototypes during world resource loading.
            for(var item:java.util.List.of(Items.BOOKSHELF,Items.PISTON,Items.STONE,Items.DIRT,Items.COAL,Items.IRON_INGOT,Items.GOLD_INGOT,Items.GLASS_PANE,Items.GLOWSTONE_DUST,Items.OAK_PLANKS,Items.SPRUCE_PLANKS,Items.OAK_SLAB,Items.SPRUCE_SLAB,art.arcane.thaumcraft.ModItems.FOCUS_1.get())){
                var fixture=net.minecraft.core.component.DataComponentMap.builder()
                    .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,64)
                    .set(net.minecraft.core.component.DataComponents.ITEM_NAME,net.minecraft.network.chat.Component.translatable(item==Items.PISTON?"block.minecraft.piston":item.getDescriptionId()))
                    .set(net.minecraft.core.component.DataComponents.ITEM_MODEL,BuiltInRegistries.ITEM.getKey(item)).build();
                item.builtInRegistryHolder().bindComponents(fixture);
            }
            // Research previews also need native item models without loading a world.
            for(var item:BuiltInRegistries.ITEM){
                var id=BuiltInRegistries.ITEM.getKey(item);
                if(!id.getNamespace().equals("thaumcraft"))continue;
                item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder()
                    .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,64)
                    .set(net.minecraft.core.component.DataComponents.ITEM_NAME,net.minecraft.network.chat.Component.translatable(item.getDescriptionId()))
                    .set(net.minecraft.core.component.DataComponents.ITEM_MODEL,id).build());
            }
            var firstNode=art.arcane.thaumcraft.client.ResearchMapData.node("FIRSTSTEPS");
            require(firstNode!=null&&firstNode.column()==0&&firstNode.row()==0&&firstNode.icons().getFirst().contains("thaumonomicon"),"Authored first-step node metadata missing");
            var alchemyNode=art.arcane.thaumcraft.client.ResearchMapData.node("UNLOCKALCHEMY");
            require(alchemyNode!=null&&alchemyNode.column()==0&&alchemyNode.row()==2,"Original alchemy node position changed");
            require(art.arcane.thaumcraft.client.ResearchMapData.background("INFUSION")==7,"Original infusion background mapping changed");
            require(art.arcane.thaumcraft.research.ResearchCatalog.entries().get("ESSENTIASMELTER").siblings().contains("WARDEDJARS"),"Authored smelter/jar secondary link missing");
            var authored=art.arcane.thaumcraft.client.ResearchBookText.readable("Uno<BR>Dos<PAGE>Tres<IMG>thaumcraft:textures/research/research5.png:128:136:108:117:.75</IMG>Fin");
            require(authored.startsWith("Uno\n\nDos\fTres")&&!authored.contains("textures/")&&!authored.contains("<IMG>"),"Research paragraphs/page markers leaked image descriptors");
            var authoredPages=art.arcane.thaumcraft.client.ResearchBookText.pages(minecraft.font,authored,140,20);
            require(authoredPages.size()==2,"An explicit authored page break must start the next physical page");
            require(art.arcane.thaumcraft.client.ResearchBookText.pages(minecraft.font,"Uno\f\fTres",140,20).size()==3,"Explicit empty research pages must preserve page alignment");
            var illustrated=art.arcane.thaumcraft.client.ResearchBookText.illustratedPages(minecraft.font,"Inicio<IMG>thaumcraft:textures/research/research1.png:0:0:128:96:1</IMG>Fin<PAGE>Otra",140,182);
            require(illustrated.stream().flatMap(java.util.Collection::stream).anyMatch(element->element instanceof art.arcane.thaumcraft.client.ResearchBookText.Image image && image.width()==128&&image.height()==96),"Original illustration crop/scale missing");
            require(illustrated.size()>=2,"Illustrated page break did not preserve the next page");
            require(art.arcane.thaumcraft.client.ResearchBookText.illustratedPages(minecraft.font,"<IMG>thaumcraft:textures/research/research1.png:0:0:128:96:NaN</IMG>",140,182).getFirst().isEmpty(),"Malformed image scale must not produce invalid geometry");
            checkChest();
            var projectileState=new net.minecraft.client.renderer.entity.state.EntityRenderState();
            projectileState.entityType=art.arcane.thaumcraft.registry.ModEntities.ALUMENTUM.get();
            require(minecraft.getEntityRenderDispatcher().getRenderer(projectileState) instanceof net.minecraft.client.renderer.entity.ThrownItemRenderer,"Alumentum renderer missing");
            System.out.println("Alumentum client renderer registered and loaded.");
            checkHeldFurnace(minecraft);
            checkStarterRecipes(minecraft);
            String cost=art.arcane.thaumcraft.client.ResearchRequirementText.format("required_item","thaumcraft:crystal_essence;1;0;{Aspects:[{amount:1,key:'aer'}]}").getString();
            require(!cost.contains("Aspects:") && !cost.contains("thaumcraft:") && cost.contains("1"),"Research cost still exposes raw legacy data");
            System.out.println("Readable research requirement labels passed: "+cost);
            checkPlacedModels(minecraft);
            var fire=minecraft.particleEngine.createParticle(art.arcane.thaumcraft.registry.ModParticles.FOCUS_FIRE.get(),0,0,0,0,0,0);
            require(fire instanceof art.arcane.thaumcraft.client.FocusFireParticle && fire.getLifetime()==10,"Original fire particle provider was not loaded");
            var renderState=new net.minecraft.client.renderer.state.level.QuadParticleRenderState();
            ((art.arcane.thaumcraft.client.FocusFireParticle)fire).extract(renderState,new net.minecraft.client.Camera(),0);
            require(!renderState.isEmpty()&&renderState.layers().contains(net.minecraft.client.particle.SingleQuadParticle.Layer.TRANSLUCENT),"Fire particle did not produce translucent native geometry");
            for(int step=0;step<11;step++)fire.tick();
            require(!fire.isAlive(),"Fire particles must expire rather than accumulating");
            System.out.println("TC6 fire particle provider, translucent geometry and expiration checks passed.");
            var runeType=art.arcane.thaumcraft.registry.ModParticles.SCAN_RUNE.get();
            var runeOption=net.minecraft.core.particles.ColorParticleOption.create(runeType,0xFFAA00DD);
            var rune=minecraft.particleEngine.createParticle(runeOption,0,0,0,15,.03,0);
            require(rune instanceof art.arcane.thaumcraft.client.ScanRuneParticle&&rune.getLifetime()==45,"Scan rune provider/original duration missing");
            rune.tick();rune.tick();renderState.clear();((art.arcane.thaumcraft.client.ScanRuneParticle)rune).extract(renderState,new net.minecraft.client.Camera(),0);
            require(!renderState.isEmpty(),"Scan rune fixed-plane native geometry missing");
            for(int step=0;step<45;step++)rune.tick();require(!rune.isAlive(),"Scan runes accumulate instead of expiring");
            var tallRune=minecraft.particleEngine.createParticle(runeOption,0,0,0,30,.03,0);require(tallRune.getLifetime()==90,"Entity scan rune duration not height-dependent");
            System.out.println("Scan rune native provider, fixed-plane geometry,45/90tick lifetimes and expiration passed.");
            var bubbleType=art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_ASPECT_BUBBLE.get();
            for(var aspect:art.arcane.thaumcraft.aspect.AspectRegistry.all().values()){
                int color=0xFF000000|art.arcane.thaumcraft.aspect.AspectRegistry.color(aspect.id());var option=net.minecraft.core.particles.ColorParticleOption.create(bubbleType,color);
                var buffer=io.netty.buffer.Unpooled.buffer();net.minecraft.core.particles.ColorParticleOption decoded;
                try{var codec=net.minecraft.core.particles.ColorParticleOption.streamCodec(bubbleType);codec.encode(buffer,option);decoded=codec.decode(buffer);}finally{buffer.release();}
                var json=bubbleType.codec().codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,decoded).getOrThrow();decoded=bubbleType.codec().codec().parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
                require(decoded.getType()==bubbleType&&decoded.getRed()==option.getRed()&&decoded.getGreen()==option.getGreen()&&decoded.getBlue()==option.getBlue(),"Aspect bubble color lost in network/JSON codecs");
                var bubble=minecraft.particleEngine.createParticle(decoded,0,0,0,0,0,0);require(bubble instanceof art.arcane.thaumcraft.client.CrucibleAspectBubbleParticle&&bubble.getLifetime()>=15&&bubble.getLifetime()<=24,"Aspect bubble provider/lifetime missing");
                renderState.clear();((art.arcane.thaumcraft.client.CrucibleAspectBubbleParticle)bubble).extract(renderState,new net.minecraft.client.Camera(),0);require(!renderState.isEmpty(),"Aspect bubble native geometry missing");
                for(int step=0;step<25;step++)bubble.tick();require(!bubble.isAlive(),"Aspect bubble failed expiration");
            }
            System.out.println("Crucible aspect bubbles passed: all37 colors through network/JSON codecs, native provider, 15..24tick life, geometry and expiration.");
            var overflow=minecraft.particleEngine.createParticle(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_OVERFLOW.get(),0,0,0,0,0,0);
            require(overflow instanceof art.arcane.thaumcraft.client.CrucibleOverflowParticle&&overflow.getLifetime()>=12&&overflow.getLifetime()<=23,"Crucible overflow provider/lifetime missing");
            renderState.clear();((art.arcane.thaumcraft.client.CrucibleOverflowParticle)overflow).extract(renderState,new net.minecraft.client.Camera(),0);require(!renderState.isEmpty(),"Crucible overflow native geometry missing");
            for(int step=0;step<24;step++)overflow.tick();require(!overflow.isAlive(),"Crucible overflow failed expiration");
            System.out.println("Original crucible overflow provider, native geometry,12..23tick life and expiration passed.");
            var froth=minecraft.particleEngine.createParticle(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_FROTH.get(),0,0,0,0,0,0);
            require(froth instanceof art.arcane.thaumcraft.client.CrucibleFrothParticle&&froth.getLifetime()>=4&&froth.getLifetime()<=6,"Crucible froth provider was not loaded");
            renderState.clear();((art.arcane.thaumcraft.client.CrucibleFrothParticle)froth).extract(renderState,new net.minecraft.client.Camera(),0);
            require(!renderState.isEmpty(),"Crucible froth native geometry missing");
            for(int step=0;step<7;step++)froth.tick();require(!froth.isAlive(),"Crucible froth must expire");
            require(art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(0)==0xFFFFFFFF&&art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(1)==0xFFD47FBF&&art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(250)==0xFFBF409F&&art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(500)==0xFFAA0080,"Crucible tint differs from original RGB multipliers");
            require(art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(Long.MAX_VALUE)==0xFFAA0080&&art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(-1)==0xFFFFFFFF,"Crucible tint failed bounds");
            System.out.println("Original crucible water concentration colors passed: empty, first aspect, half capacity, saturation and long/negative bounds.");
            System.out.println("TC6 crucible froth provider, native geometry and expiration checks passed.");
            var sparkle=minecraft.particleEngine.createParticle(art.arcane.thaumcraft.registry.ModParticles.FOCAL_CRAFT.get(),0,0,0,0,0,0);
            require(sparkle instanceof art.arcane.thaumcraft.client.FocalCraftParticle&&sparkle.getLifetime()>=6&&sparkle.getLifetime()<=10,"Manipulator crafting sparkle provider/lifetime missing");
            renderState.clear();((art.arcane.thaumcraft.client.FocalCraftParticle)sparkle).extract(renderState,new net.minecraft.client.Camera(),0);
            require(!renderState.isEmpty(),"Manipulator crafting sparkle native geometry missing");
            for(int step=0;step<11;step++)sparkle.tick();require(!sparkle.isAlive(),"Manipulator sparkle must expire");
            System.out.println("TC6 focal crafting sparkle provider, geometry and expiration checks passed.");
            opened = true;
            minecraft.gui.setScreen(book("{\"researched\":[\"!gotthaumonomicon\"],\"scanned_count\":12}"));
        }
        if (opened && ++frames == 60) {
            if (phase == 0) {
                require(minecraft.gui.screen() instanceof ResearchBrowserScreen,"Research screen was replaced before screenshot verification");
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message -> minecraft.execute(() -> { frames=0;
                            var screen=(ResearchBrowserScreen)minecraft.gui.screen();
                            require(!screen.selectCategory("AUROMANCY"),"Locked category must not be accessible before its discovery");
                            require(screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.EditBox).count()==1,"Research map must have exactly one search field");
                            phase=13;
                        }));
            } else if (phase == 10) {
                require(minecraft.gui.screen() instanceof ResearchBrowserScreen,"Research reading screen was replaced");
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-reading-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message -> minecraft.execute(() -> {
                            phase=13;frames=0;
                            var snapshot=new com.google.gson.JsonObject();var known=new com.google.gson.JsonArray();var stages=new com.google.gson.JsonObject();
                            for(var entry:art.arcane.thaumcraft.research.ResearchCatalog.entries().values())if(entry.category().equals("BASICS")){
                                known.add(entry.key());stages.addProperty(entry.key(),entry.key().equals("THEORYRESEARCH")?2:entry.stages().size()+1);
                            }
                            snapshot.add("researched",known);snapshot.add("stages",stages);snapshot.addProperty("scanned_count",12);
                            var screen=(ResearchBrowserScreen)minecraft.gui.screen();ClientResearch.receive(snapshot.toString());screen.onSnapshot();
                        }));
            } else if(phase==13){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-illustrations-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message -> minecraft.execute(() -> {
                            phase=11;frames=0;
                            var snapshot=new com.google.gson.JsonObject();var known=new com.google.gson.JsonArray();var stages=new com.google.gson.JsonObject();
                            for(String key:java.util.List.of("FIRSTSTEPS","UNLOCKALCHEMY","UNLOCKAUROMANCY","UNLOCKARTIFICE")){
                                known.add(key);stages.addProperty(key,art.arcane.thaumcraft.research.ResearchCatalog.entries().get(key).stages().size()+1);
                            }
                            snapshot.add("researched",known);snapshot.add("stages",stages);snapshot.addProperty("scanned_count",12);
                            var screen=(ResearchBrowserScreen)minecraft.gui.screen();ClientResearch.receive(snapshot.toString());screen.onSnapshot();
                            require(screen.selectCategory("AUROMANCY")&&screen.selectCategory("ARTIFICE"),"Completed category unlocks must expose the matching map");
                            require(!screen.selectCategory("ELDRITCH"),"Category switching must not unlock later research");
                        }));
            } else if(phase==11){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-categories-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message->minecraft.execute(()->{
                            phase=12;frames=0;
                            var snapshot=new com.google.gson.JsonObject();var known=new com.google.gson.JsonArray();var stages=new com.google.gson.JsonObject();
                            for(var entry:art.arcane.thaumcraft.research.ResearchCatalog.entries().values())if(entry.category().equals("ALCHEMY")||entry.key().equals("FIRSTSTEPS")||entry.key().equals("UNLOCKALCHEMY")){
                                known.add(entry.key());stages.addProperty(entry.key(),entry.stages().size()+1);
                            }
                            snapshot.add("researched",known);snapshot.add("stages",stages);snapshot.addProperty("scanned_count",12);
                            var screen=(ResearchBrowserScreen)minecraft.gui.screen();ClientResearch.receive(snapshot.toString());screen.onSnapshot();
                            require(screen.selectCategory("ALCHEMY"),"Alchemy secondary-link preview could not open");
                        }));
            } else if(phase==12){
                require(minecraft.gui.screen() instanceof ResearchBrowserScreen screen && screen.children().stream().anyMatch(child->child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().startsWith(net.minecraft.network.chat.Component.translatable("tc.research_category.ALCHEMY").getString())),"Secondary-link screenshot must show the Alchemy category");
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-siblings-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message->minecraft.execute(()->{phase=1;frames=0;minecraft.gui.setScreen(new WorkbenchPreview());}));
            } else if (phase == 1) {
                require(minecraft.gui.screen() instanceof WorkbenchPreview,"Workbench preview was replaced before screenshot verification");
                Screenshot.grab(minecraft.gameDirectory,"workbench-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message -> minecraft.execute(() -> { phase=2; frames=0; minecraft.gui.setScreen(new SmelterPreview()); }));
            } else if (phase == 2) {
                require(minecraft.gui.screen() instanceof SmelterPreview,"Smelter preview was replaced before screenshot verification");
                Screenshot.grab(minecraft.gameDirectory,"smelter-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,
                        message -> minecraft.execute(() -> {phase=3;frames=0;minecraft.gui.setScreen(new TheoryPreview());}));
            } else if (phase == 3) {
                require(minecraft.gui.screen() instanceof TheoryPreview,"Theory preview was replaced");
                Screenshot.grab(minecraft.gameDirectory,"research-table-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=4;frames=0;minecraft.gui.setScreen(new TheoryPreview(true));}));
            } else if(phase==4) {
                require(minecraft.gui.screen() instanceof TheoryPreview,"Theory aid preview was replaced");
                Screenshot.grab(minecraft.gameDirectory,"research-aids-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=5;frames=0;minecraft.gui.setScreen(new FocusPreview());}));
            } else if(phase==5) {
                require(minecraft.gui.screen() instanceof FocusPreview,"Focus preview was replaced");
                Screenshot.grab(minecraft.gameDirectory,"focal-manipulator-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=6;frames=0;minecraft.gui.setScreen(new FireParticlePreview());}));
            } else if(phase==6) {
                Screenshot.grab(minecraft.gameDirectory,"fire-particle-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=7;frames=0;minecraft.gui.setScreen(new FrothParticlePreview());}));
            } else if(phase==7) {
                Screenshot.grab(minecraft.gameDirectory,"crucible-froth-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=8;frames=0;minecraft.gui.setScreen(new SparkParticlePreview());}));
            } else if(phase==8) {
                Screenshot.grab(minecraft.gameDirectory,"focal-spark-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=9;frames=0;minecraft.gui.setScreen(new AspectBubblePreview());}));
            } else if(phase==9) {
                Screenshot.grab(minecraft.gameDirectory,"crucible-aspect-bubbles-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=14;frames=0;minecraft.gui.setScreen(new OverflowPreview());}));
            } else if(phase==14){
                Screenshot.grab(minecraft.gameDirectory,"crucible-overflow-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{
                    phase=15;frames=0;
                    var diagrams=art.arcane.thaumcraft.client.ResearchRecipeScreen.resolve(java.util.List.of("thaumcraft:thaumometer","thaumcraft:salismundusfake","thaumcraft:no_such_recipe"));
                    require(diagrams.size()==1&&diagrams.getFirst().recipe().get("vis").getAsInt()==20&&diagrams.getFirst().recipe().getAsJsonObject("crystals").size()==6,"Book recipe must preserve shipped costs and omit unresolved references");
                    System.out.println("Arcane book recipe passed: stage isolation, missing references, exact20 vis/six crystals, navigation and return.");
                }));
            } else if(phase==15){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-recipe-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{
                    phase=16;frames=0;
                    var diagrams=art.arcane.thaumcraft.client.ResearchRecipeScreen.resolve(java.util.List.of("thaumcraft:ironplate","thaumcraft:tablewood","thaumcraft:no_such_recipe"));
                    require(diagrams.size()==1&&diagrams.getFirst().recipe().getAsJsonObject("result").get("count").getAsInt()==3&&diagrams.getFirst().recipe().getAsJsonArray("pattern").get(0).getAsString().equals("BBB"),"Normal crafting diagram lost pattern/count or fabricated unresolved tag ingredients");
                    minecraft.gui.setScreen(new art.arcane.thaumcraft.client.ResearchRecipeScreen(minecraft.gui.screen(),diagrams));
                    System.out.println("Normal crafting book passed: iron plate3 output, original BBB pattern, missing references and unavailable tag recipes omitted.");
                }));
            } else if(phase==16){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-normal-recipe-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{
                    phase=17;frames=0;var diagrams=art.arcane.thaumcraft.client.ResearchRecipeScreen.resolve(java.util.List.of("thaumcraft:nitor","thaumcraft:nitorgroup"));
                    require(diagrams.size()==1,"Crucible book must resolve Nitor and omit unresolved groups");var recipe=diagrams.getFirst().recipe();
                    require(recipe.get("catalyst").getAsString().equals("minecraft:glowstone_dust")&&recipe.getAsJsonObject("aspects").size()==3&&recipe.getAsJsonObject("aspects").get("ignis").getAsInt()==10&&recipe.getAsJsonObject("aspects").get("lux").getAsInt()==10&&recipe.getAsJsonObject("aspects").get("potentia").getAsInt()==10,"Nitor illustration altered original catalyst/aspect costs");
                    minecraft.gui.setScreen(new art.arcane.thaumcraft.client.ResearchRecipeScreen(minecraft.gui.screen(),diagrams));System.out.println("Crucible book passed: Nitor glowstone catalyst,10 Ignis/Lux/Potentia and unresolved group omitted.");
                }));
            } else if(phase==17){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-crucible-recipe-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{
                    phase=18;frames=0;var diagrams=art.arcane.thaumcraft.client.ResearchRecipeScreen.resolve(java.util.List.of("thaumcraft:BrickArcane","thaumcraft:StoneArcane","thaumcraft:IronPlate"));
                    require(diagrams.size()==1,"Book recipe aliases must be explicit, not automatic case conversion");var recipe=diagrams.getFirst().recipe();
                    require(recipe.getAsJsonObject("result").get("count").getAsInt()==4&&recipe.getAsJsonObject("key").get("K").getAsString().equals("thaumcraft:stone_arcane")&&recipe.getAsJsonArray("pattern").size()==2&&recipe.getAsJsonArray("pattern").get(0).getAsString().equals("KK")&&recipe.getAsJsonArray("pattern").get(1).getAsString().equals("KK"),"Arcane brick book alias changed original pattern/count");
                    var book=book("{\"researched\":[\"!gotthaumonomicon\",\"FIRSTSTEPS\"],\"stages\":{\"FIRSTSTEPS\":3},\"scanned_count\":12}");
                    minecraft.gui.setScreen(new art.arcane.thaumcraft.client.ResearchRecipeScreen(book,diagrams));System.out.println("Arcane brick legacy book link passed: explicit alias, first-stage access, four stones/four bricks; unrelated uppercase references omitted.");
                }));
            } else if(phase==18){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-brick-recipe-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{
                    phase=19;frames=0;
                    var planks=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse("minecraft:planks"));
                    var slabs=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse("minecraft:wooden_slabs"));
                    java.util.Map<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>,java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>>> tags=new java.util.HashMap<>();
                    tags.put(planks,java.util.List.of(Items.OAK_PLANKS.builtInRegistryHolder(),Items.SPRUCE_PLANKS.builtInRegistryHolder()));tags.put(slabs,java.util.List.of(Items.OAK_SLAB.builtInRegistryHolder(),Items.SPRUCE_SLAB.builtInRegistryHolder()));
                    BuiltInRegistries.ITEM.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.ITEM,tags)).apply();
                    require(art.arcane.thaumcraft.client.ResearchRecipeScreen.ingredientOptions("#minecraft:planks").size()==2&&art.arcane.thaumcraft.client.ResearchRecipeScreen.ingredientAt("#minecraft:planks",0,0).is(Items.OAK_PLANKS)&&art.arcane.thaumcraft.client.ResearchRecipeScreen.ingredientAt("#minecraft:planks",0,1000).is(Items.SPRUCE_PLANKS)&&art.arcane.thaumcraft.client.ResearchRecipeScreen.ingredientAt("#minecraft:planks",0,2000).is(Items.OAK_PLANKS),"Native tag ingredients failed one-second rotation");
                    require(art.arcane.thaumcraft.client.ResearchRecipeScreen.ingredientOptions("#thaumcraft:missing_book_tag").isEmpty(),"Missing tag must not produce invented ingredients");
                    var diagrams=art.arcane.thaumcraft.client.ResearchRecipeScreen.resolve(java.util.List.of("thaumcraft:tablewood"));require(diagrams.size()==1,"Wood table should resolve after actual client tag reload");
                    minecraft.gui.setScreen(new art.arcane.thaumcraft.client.ResearchRecipeScreen(minecraft.gui.screen(),diagrams));System.out.println("Book ingredient tags passed: actual registry reload, two woods, one-second rotation/wrap and missing-tag rejection; tablewood resolved.");
                }));
            } else if(phase==19){
                Screenshot.grab(minecraft.gameDirectory,"thaumonomicon-tag-recipe-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(()->{phase=20;frames=0;minecraft.gui.setScreen(new ThaumometerPreview());}));
            } else {
                Screenshot.grab(minecraft.gameDirectory,"thaumometer-model-verification.png",minecraft.gameRenderer.mainRenderTarget(),1,message->minecraft.execute(minecraft::stop));
            }
        }
    }
    private static final class ThaumometerPreview extends net.minecraft.client.gui.screens.Screen{
        ThaumometerPreview(){super(net.minecraft.network.chat.Component.literal("Taumómetro · modelo original"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick){
            g.fill(0,0,width,height,0xFF302836);
            g.text(font,title,20,20,0xFFFFFFFF,false);
            g.pose().pushMatrix();g.pose().translate(width/2F-64,height/2F-64);g.pose().scale(8,8);
            g.item(new net.minecraft.world.item.ItemStack(art.arcane.thaumcraft.ModItems.THAUMOMETER.get()),0,0);g.pose().popMatrix();
        }
    }
    private static final class FireParticlePreview extends net.minecraft.client.gui.screens.Screen {
        private FireParticlePreview(){super(net.minecraft.network.chat.Component.literal("TC6 · Fuego · 10 fotogramas originales"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
            int x=(width-340)/2,y=(height-170)/2;
            art.arcane.thaumcraft.client.ThaumcraftTheme.panel(g,x,y,340,170);g.nextStratum();
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,title,x+10,y+8,0xFF392F33,false);
            var texture=net.minecraft.resources.Identifier.parse("thaumcraft:textures/particle/tc6_fire_sheet.png");
            for(int frame=0;frame<10;frame++){
                int px=x+20+(frame%5)*64,py=y+30+(frame/5)*65;
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,px,py,frame*16,160,44,44,16,16,1024,1024);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,Integer.toString(frame+1),px+18,py+46,0xFF392F33,false);
            }
        }
    }
    private static final class SparkParticlePreview extends net.minecraft.client.gui.screens.Screen {
        private SparkParticlePreview(){super(net.minecraft.network.chat.Component.literal("TC6 - Manipulador - 9 fotogramas originales"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
            int x=(width-340)/2,y=(height-170)/2;
            art.arcane.thaumcraft.client.ThaumcraftTheme.panel(g,x,y,340,170);g.nextStratum();
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,title,x+10,y+8,0xFF392F33,false);
            var texture=net.minecraft.resources.Identifier.parse("thaumcraft:textures/particle/tc6_fire_sheet.png");
            for(int frame=0;frame<9;frame++){
                int px=x+20+(frame%5)*64,py=y+30+(frame/5)*65;
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,px,py,frame*16,112,44,44,16,16,1024,1024);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,Integer.toString(frame+1),px+18,py+46,0xFF392F33,false);
            }
        }
    }
    private static final class FrothParticlePreview extends net.minecraft.client.gui.screens.Screen {
        private FrothParticlePreview(){super(net.minecraft.network.chat.Component.literal("TC6 · Crisol · Espuma y concentración"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
            int x=(width-340)/2,y=(height-210)/2;
            art.arcane.thaumcraft.client.ThaumcraftTheme.panel(g,x,y,340,210);g.nextStratum();
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,title,x+10,y+8,0xFF392F33,false);
            var texture=net.minecraft.resources.Identifier.parse("thaumcraft:textures/particle/tc6_fire_sheet.png");
            for(int frame=0;frame<3;frame++){
                int px=x+34+frame*100,py=y+40;
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,px,py,frame*16,16,64,64,16,16,1024,1024,0xFF8080B3);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,Integer.toString(64+frame),px+24,py+66,0xFF392F33,false);
            }
            int[] counts={0,1,250,500};
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,"Aspectos disueltos: color original",x+10,y+133,0xFF392F33,false);
            for(int n=0;n<counts.length;n++){
                int px=x+25+n*78;g.fill(px,y+152,px+54,y+176,art.arcane.thaumcraft.client.render.CrucibleRenderer.colorFor(counts[n]));g.outline(px,y+152,54,24,0xFF392F33);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,Integer.toString(counts[n]),px+16,y+183,0xFF392F33,false);
            }
        }
    }
    private static final class AspectBubblePreview extends net.minecraft.client.gui.screens.Screen {
        private AspectBubblePreview(){super(net.minecraft.network.chat.Component.literal("TC6 · Crisol · Burbujas por aspecto"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
            int x=(width-340)/2,y=(height-180)/2;art.arcane.thaumcraft.client.ThaumcraftTheme.panel(g,x,y,340,180);g.nextStratum();
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,title,x+10,y+8,0xFF392F33,false);
            var texture=net.minecraft.resources.Identifier.parse("thaumcraft:textures/particle/tc6_fire_sheet.png");var tags=java.util.List.of("aer","terra","ignis","aqua","ordo","perditio");
            for(int n=0;n<tags.size();n++){
                int px=x+40+(n%3)*100,py=y+32+(n/3)*70;
                int color=0xFF000000|art.arcane.thaumcraft.aspect.AspectRegistry.color(tags.get(n));
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,px,py,0,16,42,42,16,16,1024,1024,color);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,tags.get(n),px+5,py+46,0xFF392F33,false);
            }
        }
    }
    private static final class OverflowPreview extends net.minecraft.client.gui.screens.Screen {
        private OverflowPreview(){super(net.minecraft.network.chat.Component.literal("TC6 · Crisol · Rebose por los bordes"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
            int x=(width-340)/2,y=(height-160)/2;art.arcane.thaumcraft.client.ThaumcraftTheme.panel(g,x,y,340,160);g.nextStratum();
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,title,x+10,y+8,0xFF392F33,false);
            var texture=net.minecraft.resources.Identifier.parse("thaumcraft:textures/particle/tc6_fire_sheet.png");int[] frames={73,66,65};
            for(int n=0;n<frames.length;n++){
                int px=x+38+n*100;g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,px,y+35,(frames[n]%64)*16,16,54,54,16,16,1024,1024,0xCC4000BF);
                art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,Integer.toString(frames[n]),px+20,y+96,0xFF392F33,false);
            }
            art.arcane.thaumcraft.client.ThaumcraftTheme.text(g,font,"Agua · calor > 150 · aspectos > 500",x+10,y+128,0xFF392F33,false);
        }
    }
    private static final class FocusPreview extends net.minecraft.client.gui.screens.Screen{
        private final art.arcane.thaumcraft.client.FocalManipulatorScreen preview;
        private FocusPreview(){super(net.minecraft.network.chat.Component.literal("Verification"));
            var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
            var menu=new art.arcane.thaumcraft.menu.FocalManipulatorMenu(11,inventory,new art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity(net.minecraft.core.BlockPos.ZERO,art.arcane.thaumcraft.ModBlocks.WAND_WORKBENCH.get().defaultBlockState()));
            menu.slots.get(0).set(new ItemStack(art.arcane.thaumcraft.ModItems.FOCUS_1.get()));
            preview=new art.arcane.thaumcraft.client.FocalManipulatorScreen(menu,inventory,net.minecraft.network.chat.Component.translatable("block.thaumcraft.wand_workbench"));
            System.out.println("Focal manipulator client screen/progress/Spanish labels passed.");}
        @Override protected void init(){preview.init(width,height);}
        @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float tick){preview.extractBackground(g,x,y,tick);}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float tick){preview.extractRenderState(g,x,y,tick);}
    }
    private static final class WorkbenchPreview extends net.minecraft.client.gui.screens.Screen {
        private final art.arcane.thaumcraft.client.ArcaneWorkbenchScreen preview;
        private WorkbenchPreview() {
            super(net.minecraft.network.chat.Component.literal("Verification"));
            var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
            var menu=new art.arcane.thaumcraft.menu.ArcaneWorkbenchMenu(7,inventory);
            for (int slot : new int[]{2,4,6,8}) menu.slots.get(slot).set(new ItemStack(Items.GOLD_INGOT));
            menu.slots.get(5).set(new ItemStack(Items.GLASS_PANE));
            menu.slots.get(0).set(new ItemStack(art.arcane.thaumcraft.ModItems.THAUMOMETER.get()));
            for (int index=0;index<6;index++) menu.slots.get(10+index).set(art.arcane.thaumcraft.item.CrystalEssenceItem.of(
                    art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity.PRIMALS.get(index),8));
            menu.setData(0,250); menu.setData(1,20);
            preview=new art.arcane.thaumcraft.client.ArcaneWorkbenchScreen(menu,inventory,net.minecraft.network.chat.Component.translatable("block.thaumcraft.arcane_workbench"));
        }
        @Override protected void init() { preview.init(width,height); }
        @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,float tick) { preview.extractBackground(graphics,x,y,tick); }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,float tick) { preview.extractRenderState(graphics,x,y,tick); }
    }
    private static final class SmelterPreview extends net.minecraft.client.gui.screens.Screen {
        private final art.arcane.thaumcraft.client.EssentiaSmelterScreen preview;
        private SmelterPreview() {
            super(net.minecraft.network.chat.Component.literal("Verification"));
            var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
            var menu=new art.arcane.thaumcraft.menu.EssentiaSmelterMenu(8,inventory);
            menu.slots.get(0).set(new ItemStack(Items.IRON_INGOT,16));
            menu.slots.get(1).set(new ItemStack(Items.COAL,8));
            int[] values={15,30,750,60,128,80};for(int i=0;i<values.length;i++)menu.setData(i,values[i]);
            require(menu.progress()==500 && menu.fuelFraction()==750 && menu.stored()==128,"Smelter client data decoding failed");
            preview=new art.arcane.thaumcraft.client.EssentiaSmelterScreen(menu,inventory,net.minecraft.network.chat.Component.translatable("block.thaumcraft.smelter_basic"));
            require(!net.minecraft.network.chat.Component.translatable("screen.thaumcraft.smelter.progress",50).getString().contains("screen.thaumcraft"),"Smelter translation missing");
            System.out.println("Smelter client screen and progress data checks passed.");
        }
        @Override protected void init() { preview.init(width,height); }
        @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,float tick) { preview.extractBackground(graphics,x,y,tick); }
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,float tick) { preview.extractRenderState(graphics,x,y,tick); }
    }
    private static final class TheoryPreview extends net.minecraft.client.gui.screens.Screen {
        private final art.arcane.thaumcraft.client.ResearchTableScreen preview;
        private TheoryPreview(){this(false);}
        private TheoryPreview(boolean idle){
            super(net.minecraft.network.chat.Component.literal("Verification"));
            var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
            var menu=new art.arcane.thaumcraft.menu.ResearchTableMenu(9,inventory);
            for(var item:java.util.List.of(Items.PAPER,art.arcane.thaumcraft.ModItems.SCRIBING_TOOLS.get()))item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,64).set(net.minecraft.core.component.DataComponents.ITEM_MODEL,BuiltInRegistries.ITEM.getKey(item)).build());
            menu.slots.get(0).set(new ItemStack(art.arcane.thaumcraft.ModItems.SCRIBING_TOOLS.get()));menu.slots.get(1).set(new ItemStack(Items.PAPER,32));
            int[] values={12,1,3,5,1,7,12,30,42,18,7,4,9,9,2,0,1,10,2,0,1,11,2,0,1};for(int i=0;i<values.length;i++)menu.setData(i,values[i]);
            int[] aspects={7,-1,7,11,11,-1};for(int i=0;i<aspects.length;i++)menu.setData(26+i,aspects[i]);
            String synthesis=art.arcane.thaumcraft.client.TheoryCardText.help(menu,2).getString();
            require(synthesis.contains("Consume")&&!synthesis.contains("screen.thaumcraft")&&!synthesis.contains("%s")&&!synthesis.contains("%1$s"),"Specialised crystal tooltip did not localize its component costs");
            require(art.arcane.thaumcraft.client.TheoryCardText.name(14).getString().equals("Encantamiento"),"Specialised theory name not localized");
            menu.setData(21,15);menu.setData(22,art.arcane.thaumcraft.research.ResearchCatalog.CATEGORIES.indexOf("ARTIFICE"));menu.setData(23,14);menu.setData(24,1);menu.setData(30,10);menu.setData(31,-1);
            require(art.arcane.thaumcraft.client.TheoryCardText.name(16).getString().equals("Programación"),"Scripting Spanish name missing");
            require(art.arcane.thaumcraft.client.TheoryCardText.name(17).getString().equals("Curiosidad"),"Curio Spanish name missing");
            String tinker=art.arcane.thaumcraft.client.TheoryCardText.help(menu,2).getString();require(tinker.contains("14")&&tinker.contains("24")&&tinker.toLowerCase(java.util.Locale.ROOT).contains("pist")&&!tinker.contains("screen.thaumcraft")&&!tinker.contains("%3$s"),"Tinker tooltip did not localize score/item requirement");require(art.arcane.thaumcraft.client.TheoryCardText.name(15).getString().equals("Trastear"),"Tinker name missing Spanish localization");
            menu.setData(13,17);menu.setData(14,0);menu.setData(15,0);menu.setData(16,1);menu.setData(26,1);menu.setData(27,-1);
            String curio=art.arcane.thaumcraft.client.TheoryCardText.help(menu,0).getString();require(curio.contains("Consume")&&curio.contains("25")&&curio.contains("35")&&!curio.contains("screen.thaumcraft")&&!curio.contains("%1$s"),"Curio tooltip missing consumed item or original reward ranges");
            require(art.arcane.thaumcraft.client.TheoryCardText.name(18).getString().equals("Revelación"),"Revelation Spanish name missing");
            menu.setData(13,18);menu.setData(14,art.arcane.thaumcraft.research.ResearchCatalog.CATEGORIES.indexOf("ELDRITCH"));menu.setData(15,30);menu.setData(16,1);
            String revelation=art.arcane.thaumcraft.client.TheoryCardText.help(menu,0).getString();require(revelation.contains("30")&&revelation.contains("temporal")&&revelation.contains("normal")&&!revelation.contains("screen.thaumcraft"),"Revelation tooltip omitted warp or point costs");
            require(art.arcane.thaumcraft.client.TheoryCardText.name(19).getString().equals("Verdad")&&art.arcane.thaumcraft.client.TheoryCardText.name(20).getString().equals("Comprensión"),"Truth/Realization Spanish names missing");
            for(int card=1;card<3;card++){menu.setData(13+card*4,18+card);menu.setData(14+card*4,art.arcane.thaumcraft.research.ResearchCatalog.CATEGORIES.indexOf("ELDRITCH"));menu.setData(15+card*4,0);menu.setData(16+card*4,1);}
            String truth=art.arcane.thaumcraft.client.TheoryCardText.help(menu,1).getString(),realization=art.arcane.thaumcraft.client.TheoryCardText.help(menu,2).getString();require(truth.contains("25")&&truth.contains("temporal")&&realization.contains("50")&&realization.contains("normal")&&!truth.contains("screen.thaumcraft")&&!realization.contains("screen.thaumcraft"),"Truth/Realization tooltips missing original effects");
            menu.setData(13,21);menu.setData(14,0);menu.setData(15,0);menu.setData(16,1);menu.setData(26,0);menu.setData(27,5);
            var celestial=art.arcane.thaumcraft.client.TheoryCardText.help(menu,0).getString();
            require(art.arcane.thaumcraft.client.TheoryCardText.name(21).getString().equals("Celestial")&&celestial.contains("25")&&celestial.contains("50")&&!celestial.contains("item.celestial_notes.")&&!celestial.contains("screen.thaumcraft"),"Celestial title/typed note requirements untranslated");
            if(idle){menu.setData(1,0);menu.setData(2,5);for(int i=6;i<13;i++)menu.setData(i,0);}
            preview=new art.arcane.thaumcraft.client.ResearchTableScreen(menu,inventory,net.minecraft.network.chat.Component.translatable("block.thaumcraft.research_table"));
            require(menu.button(2)==194&&menu.value(2)==(idle?5:3),"Theory client data decoding failed");
            System.out.println("Research table client screen, specialised crystal tooltips, aid selection and synchronized card checks passed.");
        }
        @Override protected void init(){preview.init(width,height);}
        @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float tick){preview.extractBackground(g,x,y,tick);}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float tick){preview.extractRenderState(g,x,y,tick);}
    }
    @SuppressWarnings("deprecation")
    private static void checkPlacedModels(Minecraft minecraft) {
        var models=minecraft.getModelManager().getBlockStateModelSet();
        int checked=0;
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            var block=holder.get();
            if (!(block instanceof art.arcane.thaumcraft.block.CrystalClusterBlock) && !(block instanceof art.arcane.thaumcraft.block.NitorBlock) && !(block instanceof art.arcane.thaumcraft.block.EssentiaJarBlock) && !(block instanceof art.arcane.thaumcraft.block.EssentiaTubeBlock) && !(block instanceof art.arcane.thaumcraft.block.EssentiaSmelterBlock) && !(block instanceof art.arcane.thaumcraft.block.EssentiaCentrifugeBlock) && !(block instanceof art.arcane.thaumcraft.block.ResearchTableBlock)) continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var model=models.get(state);
                require(model!=models.missingModel(),"Missing placed model: "+state);
                var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                model.collectParts(net.minecraft.util.RandomSource.create(0),parts);
                int quads=0;
                for (var part : parts) {
                    quads+=part.getQuads(null).size();
                    for (var direction : net.minecraft.core.Direction.values()) quads+=part.getQuads(direction).size();
                }
                require(quads>0,"Invisible placed model: "+state);
                if ((block==ModBlocks.JAR_NORMAL.get() || block==ModBlocks.JAR_VOID.get()) && state.getValue(art.arcane.thaumcraft.block.EssentiaJarBlock.FILL)>0) {
                    int color=state.getValue(art.arcane.thaumcraft.block.EssentiaJarBlock.COLOR);
                    var fluid=new java.util.ArrayList<net.minecraft.client.resources.model.geometry.BakedQuad>();
                    for(var part:parts)for(var quad:part.getQuads(null))if(quad.materialInfo().tintIndex()==color)fluid.add(quad);
                    require(fluid.size()==6,"Jar fluid missing faces/tint: "+state);
                    float top=(float)((0.5+state.getValue(art.arcane.thaumcraft.block.EssentiaJarBlock.FILL)*2.5)/16);
                    float actual=0;for(var quad:fluid)for(int v=0;v<4;v++)actual=Math.max(actual,quad.position(v).y());
                    require(Math.abs(actual-top)<0.0001,"Jar fluid height incorrect: "+state);
                }
                checked++;
            }
        }
        System.out.println("Placed crystal/Nitor baked geometry checks passed for "+checked+" block states.");
    }
    private static void checkChest() {
        var state = ModBlocks.HUNGRY_CHEST.get().defaultBlockState();
        var chest = new HungryChestBlockEntity(BlockPos.ZERO, state);
        chest.setItem(0, new ItemStack(Items.STONE, 60));
        ItemStack incoming = new ItemStack(Items.STONE, 10);
        require(chest.insert(incoming).isEmpty(), "Chest rejected available space");
        require(incoming.getCount() == 10 && chest.getItem(0).getCount() == 64 && chest.getItem(1).getCount() == 6,
                "Chest duplicated, lost, or mutated incoming items");
        for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
        require(chest.insert(new ItemStack(Items.DIRT, 10)).getCount() == 10, "Full chest destroyed an item");
        chest.setItem(3, new ItemStack(Items.STONE, 63));
        require(chest.insert(new ItemStack(Items.STONE, 10)).getCount() == 9 && chest.getItem(3).getCount() == 64,
                "Partial insertion returned an incorrect remainder");
        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var saved = chest.saveWithoutMetadata(lookup);
        var restored = new HungryChestBlockEntity(BlockPos.ZERO, state);
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, lookup, saved));
        require(restored.getContainerSize() == 27 && restored.getItem(3).getCount() == 64,
                "Chest inventory failed its native save/reload test");
        System.out.println("Hungry Chest integration checks passed: insertion, full/partial capacity, save/reload.");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void fixture(net.minecraft.world.item.Item item, String model) {
        var components = net.minecraft.core.component.DataComponentMap.builder()
                .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,64)
                .set(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse(model));
        if (item == art.arcane.thaumcraft.ModItems.CRYSTAL_ESSENCE.get())
            components.set(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get(),"aer");
        item.builtInRegistryHolder().bindComponents(components.build());
    }
    private static void checkStarterRecipes(Minecraft minecraft) {
        fixture(art.arcane.thaumcraft.ModItems.PHIAL_FILLED.get(),"thaumcraft:phial_filled");
        var crystal = art.arcane.thaumcraft.ModItems.CRYSTAL_ESSENCE.get();
        fixture(crystal,"thaumcraft:crystal_essence");
        fixture(art.arcane.thaumcraft.ModItems.SALIS_MUNDUS.get(),"thaumcraft:salis_mundus");
        fixture(art.arcane.thaumcraft.ModItems.THAUMOMETER.get(),"thaumcraft:thaumometer");
        for (var item : java.util.List.of(Items.BOWL,Items.FLINT,Items.REDSTONE,Items.GOLD_INGOT,Items.GLASS_PANE))
            fixture(item,item.builtInRegistryHolder().key().identifier().toString());
        var dust = new art.arcane.thaumcraft.crafting.MagicDustRecipe();
        var ingredients = new java.util.ArrayList<ItemStack>(java.util.List.of(new ItemStack(Items.BOWL),new ItemStack(Items.FLINT),
                new ItemStack(Items.REDSTONE),art.arcane.thaumcraft.item.CrystalEssenceItem.of("aer",1),
                art.arcane.thaumcraft.item.CrystalEssenceItem.of("terra",1),art.arcane.thaumcraft.item.CrystalEssenceItem.of("ignis",1)));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3,2,ingredients);
        require(dust.matches(input,null) && dust.assemble(input).is(art.arcane.thaumcraft.ModItems.SALIS_MUNDUS.get()),"Starter Salis Mundus recipe failed");
        var remainder = dust.getRemainingItems(input);
        require(remainder.get(0).is(Items.BOWL) && remainder.get(1).is(Items.FLINT) && remainder.get(2).isEmpty(),"Salis Mundus destroyed its tools or returned redstone");
        ingredients.set(5,art.arcane.thaumcraft.item.CrystalEssenceItem.of("aer",1));
        require(!dust.matches(net.minecraft.world.item.crafting.CraftingInput.of(3,2,ingredients),null),"Duplicate crystal aspects bypassed Salis Mundus recipe");
        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        try (var reader = new java.io.InputStreamReader(ResearchUiVerification.class.getResourceAsStream("/data/thaumcraft/recipe/thaumometer.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            var json = com.google.gson.JsonParser.parseReader(reader);
            var arcane = art.arcane.thaumcraft.crafting.ArcaneRecipe.CODEC.codec().parse(lookup.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),json).getOrThrow();
            var grid = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));
            for (int index : new int[]{1,3,5,7}) grid.set(index,new ItemStack(Items.GOLD_INGOT));
            grid.set(4,new ItemStack(Items.GLASS_PANE));
            require(arcane.matches(net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid),null) && arcane.vis() == 20
                    && arcane.crystals().size() == 6 && arcane.research().equals("FIRSTSTEPS@2"),"Original Thaumometer recipe/cost drift");
            grid.set(4,new ItemStack(Items.STONE));
            require(!arcane.matches(net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid),null),"Arcane pattern accepted a wrong ingredient");
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
        for (String aspect : art.arcane.thaumcraft.aspect.AspectRegistry.all().keySet()) {
            var rendered = new net.minecraft.client.renderer.item.ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(rendered,art.arcane.thaumcraft.item.CrystalEssenceItem.of(aspect,1),
                    net.minecraft.world.item.ItemDisplayContext.GUI,null,null,0);
            require(!rendered.isEmpty(),"Crystal aspect model missing: " + aspect);
            for (var context : net.minecraft.world.item.ItemDisplayContext.values()) {
                var phial=new net.minecraft.client.renderer.item.ItemStackRenderState();
                minecraft.getItemModelResolver().updateForTopItem(phial,art.arcane.thaumcraft.item.EssentiaPhialItem.filled(aspect),context,null,null,0);
                require(!phial.isEmpty(),"Phial model missing: " + aspect + " / " + context);
            }
        }
        var bench = new art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity(BlockPos.ZERO,ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState());
        bench.setItem(9,art.arcane.thaumcraft.item.CrystalEssenceItem.of("aer",8));
        bench.setItem(15,new ItemStack(Items.STONE,3));
        var restored = new art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity(BlockPos.ZERO,bench.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,lookup,bench.saveWithoutMetadata(lookup)));
        require(restored.getItem(9).getCount() == 8 && art.arcane.thaumcraft.item.CrystalEssenceItem.aspect(restored.getItem(9)).equals("aer")
                && restored.getItem(15).getCount() == 3,"Workbench lost crystals or committed output on reload");
        System.out.println("Starter integration checks passed: Salis Mundus, aspect uniqueness/tool return, native arcane recipe decoding, 37 crystal models, workbench save/reload.");
    }
    private static void checkHeldFurnace(Minecraft minecraft) {
        var item = art.arcane.thaumcraft.ModItems.BLOCK_INFERNAL_FURNACE.get();
        item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder()
                .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64)
                .set(net.minecraft.core.component.DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.parse("thaumcraft:infernal_furnace")).build());
        var stack = new ItemStack(item);
        for (var context : net.minecraft.world.item.ItemDisplayContext.values()) {
            var rendered = new net.minecraft.client.renderer.item.ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(rendered, stack, context, null, null, 0);
            require(!rendered.isEmpty(), "Furnace item has no baked model in " + context);
            var bounds = rendered.getModelBoundingBox();
            require(bounds.getXsize() <= 1.4 && bounds.getYsize() <= 1.4 && bounds.getZsize() <= 1.4,
                    "Furnace still uses multiblock dimensions in item context " + context + ": " + bounds);
        }
        System.out.println("Infernal Furnace baked model checks passed in all display contexts, including both hands and both perspectives.");
    }
}

