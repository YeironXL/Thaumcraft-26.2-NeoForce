package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.research.PlayerResearch;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Thaumcraft.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerResearch>> RESEARCH =
            TYPES.register("player_research", () -> AttachmentType.builder(PlayerResearch::new)
                    .serialize(PlayerResearch.CODEC).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<art.arcane.thaumcraft.aura.ChunkAura>> AURA =
            TYPES.register("chunk_aura", () -> AttachmentType.builder(art.arcane.thaumcraft.aura.ChunkAura::new)
                    .serialize(art.arcane.thaumcraft.aura.ChunkAura.CODEC).build());
    /** TC6 chunk NBT "Thaumcraft.seals": the golem seals placed in this chunk. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.List<net.minecraft.nbt.CompoundTag>>> SEALS =
            TYPES.register("golem_seals", () -> AttachmentType.<java.util.List<net.minecraft.nbt.CompoundTag>>builder(() -> new java.util.ArrayList<>())
                    .serialize(net.minecraft.nbt.CompoundTag.CODEC.listOf().fieldOf("seals"), l -> !l.isEmpty()).build());
    /** TC4 Focus of Warding: warded block positions and their owners, synced for the goggles overlay. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<art.arcane.thaumcraft.casters.ward.WardData>> WARDS =
            TYPES.register("wards", () -> AttachmentType.builder(art.arcane.thaumcraft.casters.ward.WardData::new)
                    .serialize(art.arcane.thaumcraft.casters.ward.WardData.CODEC, d -> !d.isEmpty())
                    .sync(art.arcane.thaumcraft.casters.ward.WardData.STREAM_CODEC).build());
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private ModAttachments() {}
}
