package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Register every retained original sound ID, so native machines can play the bundled audio. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,Thaumcraft.MOD_ID);
    private static final Map<String,DeferredHolder<SoundEvent,SoundEvent>> ENTRIES=new LinkedHashMap<>();
    static {
        try (var reader=new InputStreamReader(ModSounds.class.getResourceAsStream("/assets/thaumcraft/sounds.json"),StandardCharsets.UTF_8)) {
            for (String key : JsonParser.parseReader(reader).getAsJsonObject().keySet())
                ENTRIES.put(key,SOUNDS.register(key,() -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,key))));
        } catch (java.io.IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    public static SoundEvent get(String name) { return java.util.Objects.requireNonNull(ENTRIES.get(name),name).get(); }
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    private ModSounds() {}
}
