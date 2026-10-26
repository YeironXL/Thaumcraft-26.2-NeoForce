package art.arcane.thaumcraft.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES=DeferredRegister.create(Registries.PARTICLE_TYPE,"thaumcraft");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> FOCUS_FIRE=TYPES.register("focus_fire",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> CRUCIBLE_OVERFLOW=TYPES.register("crucible_overflow",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> CRUCIBLE_FROTH=TYPES.register("crucible_froth",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> FOCAL_CRAFT=TYPES.register("focal_craft",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,ParticleType<net.minecraft.core.particles.ColorParticleOption>> CRUCIBLE_ASPECT_BUBBLE=TYPES.register("crucible_aspect_bubble",()->new ParticleType<net.minecraft.core.particles.ColorParticleOption>(false){
        public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec(){return net.minecraft.core.particles.ColorParticleOption.codec(this);}
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,net.minecraft.core.particles.ColorParticleOption> streamCodec(){return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);}
    });
    public static final DeferredHolder<ParticleType<?>,ParticleType<net.minecraft.core.particles.ColorParticleOption>> SCAN_RUNE=TYPES.register("scan_rune",()->new ParticleType<net.minecraft.core.particles.ColorParticleOption>(false){
        public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec(){return net.minecraft.core.particles.ColorParticleOption.codec(this);}
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,net.minecraft.core.particles.ColorParticleOption> streamCodec(){return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);}
    });
    public static final DeferredHolder<ParticleType<?>,ParticleType<art.arcane.thaumcraft.fx.GenericParticleOptions>> GENERIC=TYPES.register("generic",()->new ParticleType<art.arcane.thaumcraft.fx.GenericParticleOptions>(false){
        public com.mojang.serialization.MapCodec<art.arcane.thaumcraft.fx.GenericParticleOptions> codec(){return art.arcane.thaumcraft.fx.GenericParticleOptions.CODEC;}
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,art.arcane.thaumcraft.fx.GenericParticleOptions> streamCodec(){return art.arcane.thaumcraft.fx.GenericParticleOptions.STREAM_CODEC;}
    });
    public static void register(IEventBus bus){TYPES.register(bus);}
    private ModParticles(){}
}
