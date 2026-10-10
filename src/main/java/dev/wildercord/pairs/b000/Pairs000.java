package dev.wildercord.pairs.b000;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The first hand-made pairs: the reference every later batch is written to match. */
public final class Pairs000 {
	private Pairs000() {}

	/**
	 * Thermal Shock: a shell of frost closes round each target, and fire inside it cracks it apart. The look: a
	 * pale sphere tightening for half a second, then the shell bursts outward in orange shards.
	 */
	@Pair(a = "chill", b = "fire", name = "Thermal Shock", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power", "duration", "radius"},
		text = "Frost closes round each target for half a second (Slowness III), then fire inside cracks the shell: 6 damage, "
			+ "set alight for 3 seconds, and shards of 2 damage into every enemy within 2.5 blocks.")
	public static void chillFire(PairCast c) {
		List<LivingEntity> shelled = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : shelled) {
			c.effect(t, MobEffects.SLOWNESS, 1.5, 2);
			c.chill(t, 2);
			c.sound(SoundEvents.GLASS_PLACE, PairCast.mid(t), 0.8F, 0.6F);
		}
		// The shell tightens: four frames of a frost sphere shrinking round each target.
		c.every(2, 5, frame -> {
			for (LivingEntity t : c.still(shelled)) {
				c.sphere(PairCast.dust(0xCFEFFF, 1.1F), PairCast.mid(t), 1.4 - frame * 0.22, 26);
				if (frame == 4) {
					crack(c, t);
				}
			}
		});
	}

	private static void crack(PairCast c, LivingEntity t) {
		Vec3 at = PairCast.mid(t);
		c.freeze(t, 2 * c.power);
		c.burn(t, 4 * c.power);
		c.ignite(t, 3);
		c.mark(t, Reactions.Mark.CRACKED);
		c.particles(ParticleTypes.FLAME, at, 18, 0.3, 0.12);
		c.particles(ParticleTypes.ITEM_SNOWBALL, at, 14, 0.3, 0.25);
		c.sphere(PairCast.shift(0xCFEFFF, 0xFF7A1F, 1.3F), at, 1.6, 34);
		c.sound(SoundEvents.GLASS_BREAK, at, 1.0F, 0.8F);
		c.sound(SoundEvents.FIRECHARGE_USE, at, 0.7F, 1.2F);
		c.shake(at, 0.25F, 8);
		// The shards fly into the enemies round it.
		for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
			if (near != t) {
				c.line(PairCast.dust(0xBFE8FF, 0.8F), at, PairCast.mid(near), 4);
				c.freeze(near, 2 * c.power);
			}
		}
	}
}
