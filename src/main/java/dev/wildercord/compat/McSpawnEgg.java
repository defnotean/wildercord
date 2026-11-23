package dev.wildercord.compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

/**
 * Version seams for the 1.21.1 API below the 1.21.3 particle/spawn-egg reshuffle.
 *
 * <p>1.21.1 has no {@code TargetColorParticleOption}, so the travelling trail is approximated
 * by a plain particle at the origin point. The 1.21.2 trail keeps its target only through
 * 1.21.3+; here {@link #isTrail} is always false so callers take the vanilla path.
 */
public final class McSpawnEgg {

	private McSpawnEgg() {}

	/** A spawn egg for {@code type}; the colours are a neutral pair read from the type id. */
	public static Item create(EntityType<? extends Mob> type, Item.Properties properties) {
		int seed = type.getDescriptionId().hashCode();
		int base = 0xFF000000 | ((seed >>> 16) & 0x7F7F7F);
		int high = 0xFF000000 | ((seed >>> 24) & 0x7F) << 16 | ((seed >>> 8) & 0x7F) << 8 | (seed & 0x7F);
		return new SpawnEggItem(type, base, high, properties);
	}

	/** A plain particle at the origin point; 1.21.1 has no trail target. */
	public static ParticleOptions trail(Vec3 target, int color, int duration) {
		return ParticleTypes.ENCHANT;
	}

	/** The trail target is gone in 1.21.1. */
	public static Vec3 trailTarget(ParticleOptions options) {
		return Vec3.ZERO;
	}

	/** The trail tint is gone in 1.21.1. */
	public static int trailColor(ParticleOptions options) {
		return -1;
	}

	/** 1.21.1 has no trail options. */
	public static boolean isTrail(ParticleOptions options) {
		return false;
	}

	/** Leaf-litter particles; 1.21.1 has cherry leaves. */
	public static ParticleOptions leaves() {
		return ParticleTypes.CHERRY_LEAVES;
	}

	/** The armour model id; 1.21.1 armour materials carry a plain resource location. */
	public static ResourceLocation armorModel(String path) {
		return ResourceLocation.tryParse(path);
	}
}
