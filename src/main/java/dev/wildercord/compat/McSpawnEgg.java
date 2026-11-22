package dev.wildercord.compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TargetColorParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

/**
 * Version seams for the 1.21.3 API below the 1.21.4 item/render/particle reshuffle.
 *
 * <p>1.21.4 folded the spawn-egg colours into the item and renamed the trail particle option;
 * 1.21.3 still takes explicit colours and calls the option TargetColorParticleOption. Keeping
 * these in one place lets gameplay callers stay version-neutral.
 */
public final class McSpawnEgg {

	private McSpawnEgg() {}

	/** A spawn egg for {@code type}; 1.21.3 colours are a neutral pair read from the type id. */
	public static Item create(EntityType<? extends Mob> type, Item.Properties properties) {
		int seed = type.getDescriptionId().hashCode();
		int base = 0xFF000000 | ((seed >>> 16) & 0x7F7F7F);
		int high = 0xFF000000 | ((seed >>> 24) & 0x7F) << 16 | ((seed >>> 8) & 0x7F) << 8 | (seed & 0x7F);
		return dev.wildercord.compat.McSpawnEgg.create(type, base, high, properties);
	}

	/** A travelling tinted trail to {@code target}; the 1.21.3 option has no duration field. */
	public static ParticleOptions trail(Vec3 target, int color, int duration) {
		return new TargetColorParticleOption(target, color);
	}

	/** The trail's target on 1.21.3. */
	public static Vec3 trailTarget(ParticleOptions options) {
		return options instanceof TargetColorParticleOption trail ? trail.target() : Vec3.ZERO;
	}

	/** The trail's tint on 1.21.3. */
	public static int trailColor(ParticleOptions options) {
		return options instanceof TargetColorParticleOption trail ? trail.color() : -1;
	}

	/** Whether a particle option is a trail on 1.21.3. */
	public static boolean isTrail(ParticleOptions options) {
		return options instanceof TargetColorParticleOption;
	}

	/** Leaf-litter particles: 1.21.3 has cherry leaves but not pale oak leaves. */
	public static ParticleOptions leaves() {
		return ParticleTypes.CHERRY_LEAVES;
	}

	/** The armour model id: 1.21.3 armour materials carry a plain resource location. */
	public static ResourceLocation armorModel(String path) {
		return ResourceLocation.tryParse(path);
	}
}
