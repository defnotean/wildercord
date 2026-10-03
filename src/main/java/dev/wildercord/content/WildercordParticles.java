package dev.wildercord.content;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Wildercord's own particles: magic circles, a spell's whole circle, shaped light, motes and vapours. Everything else is vanilla. */
public final class WildercordParticles {
	private WildercordParticles() {}

	/** Always shown, whatever the particle setting: a circle is one particle and it carries meaning (a telegraph, a charge). */
	public static final ParticleType<SigilOption> SIGIL = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("sigil"),
		FabricParticleTypes.complex(true, SigilOption.CODEC, SigilOption.STREAM_CODEC));

	/** A spell's whole magic circle, written out from its runes so it can be read. Always shown, like the circle. */
	public static final ParticleType<SpellCircleOption> SPELL_CIRCLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("spell_circle"),
		FabricParticleTypes.complex(true, SpellCircleOption.CODEC, SpellCircleOption.STREAM_CODEC));

	/** Shaped light: shockwave rings, beams, crescent slashes and orbs. Always shown: it's what a spell looks like. */
	public static final ParticleType<LightOption> LIGHT = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("light"),
		FabricParticleTypes.complex(true, LightOption.CODEC, LightOption.STREAM_CODEC));

	/** A Shield blocking a spell or shattering: one particle each client turns into the whole effect. Always shown. */
	public static final ParticleType<ShieldOption> SHIELD = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("shield"),
		FabricParticleTypes.complex(true, ShieldOption.CODEC, ShieldOption.STREAM_CODEC));

	/** Motes, butterflies of light, steam and smoke. Left out like vanilla's particles when particles are turned down. */
	public static final ParticleType<MoteOption> MOTE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("mote"),
		FabricParticleTypes.complex(false, MoteOption.CODEC, MoteOption.STREAM_CODEC));

	/** An attunement's ritual: one particle every few ticks keeps each client's going. Always shown. */
	public static final ParticleType<RitualOption> RITUAL = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("ritual"),
		FabricParticleTypes.complex(true, RitualOption.CODEC, RitualOption.STREAM_CODEC));

	/** Curved wind streams carry pressure and drift without luminous beam primitives. */
	public static final ParticleType<LifeOption> LIFE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("life"),
		FabricParticleTypes.complex(false, LifeOption.CODEC, LifeOption.STREAM_CODEC));

	public static final ParticleType<VoidOption> VOID_MATERIAL = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("void_material"),
		FabricParticleTypes.complex(false, VoidOption.CODEC, VoidOption.STREAM_CODEC));

	public static final ParticleType<EarthOption> EARTH = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("earth"),
		FabricParticleTypes.complex(false, EarthOption.CODEC, EarthOption.STREAM_CODEC));

	public static final ParticleType<AirflowOption> AIRFLOW = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("airflow"),
		FabricParticleTypes.complex(true, AirflowOption.CODEC, AirflowOption.STREAM_CODEC));

	public static void init() {}
	public static final ParticleType<MaterialOption> MATERIAL = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildercord.id("material"),
		FabricParticleTypes.complex(false, MaterialOption.CODEC, MaterialOption.STREAM_CODEC));
}
