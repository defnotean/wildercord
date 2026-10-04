package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
/** Independent physical sprite registration keeps the authored floss on the ordinary particle atlas. */
public final class MossveilParticles{
 public static final SimpleParticleType FIBER=Registry.register(BuiltInRegistries.PARTICLE_TYPE,Wildercord.id("mossveil_fiber"),FabricParticleTypes.simple());
 public static void init(){}
}
