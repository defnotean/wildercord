package dev.wildercord.pairs.b042;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs042 {
	private Pairs042() {}

	/** The ally the spell struck, or the caster when it struck none. */
	private static LivingEntity anchorOf(PairCast c) {
		LivingEntity ally = c.firstAlly();
		return ally != null ? ally : c.caster;
	}

	/** Whether the block at {@code p} is a crop grown to its last stage. */
	private static boolean ripe(PairCast c, BlockPos p) {
		BlockState state = c.level.getBlockState(p);
		return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
	}

	/**
	 * Sounded Lead: a lead weight plumbed down from above each target, the way a sailor sounds a depth. The look: a
	 * grey line dropping from high above, a dark weight sinking down it, then a splash where it lands.
	 */
	@Pair(a = "fathom", b = "sounding", name = "Sounded Lead", element = "frost", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "A lead weight drops on each target from 6 blocks up, in four frames over 0.8 seconds: 6 damage (times power) when it "
			+ "lands. A target standing in water is also slowed (Slowness II) for 3 seconds and left soaked.")
	public static void soundedLead(PairCast c) {
		List<LivingEntity> sounded = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(sounded)) {
				Vec3 at = PairCast.mid(t);
				double drop = 6.0 - frame * 1.5;
				Vec3 weight = at.add(0, drop, 0);
				c.line(PairCast.dust(0x9FB4C7, 0.7F), at.add(0, 7.0, 0), weight, 2);
				c.sphere(PairCast.dust(0x2B4C7E, 1.0F), weight, 0.3, 8);
				if (frame == 0) {
					c.sound(SoundEvents.CHAIN_STEP, at, 0.7F, 0.6F);
				}
			}
		});
		c.later(16, () -> {
			for (LivingEntity t : c.still(sounded)) {
				Vec3 at = PairCast.mid(t);
				c.strike(t, 6 * c.power);
				if (t.isInWater()) {
					c.effect(t, MobEffects.SLOWNESS, 3, 1);
					c.mark(t, Reactions.Mark.SOAKED);
					c.particles(ParticleTypes.SPLASH, at, 10, 0.5, 0.2);
				}
				c.particles(ParticleTypes.ITEM_SNOWBALL, at, 6, 0.3, 0.1);
				c.sound(SoundEvents.ANVIL_LAND, at, 0.5F, 1.6F);
			}
		});
	}

	/**
	 * Harvest Heartbeat: the ally's pulse runs out through the field: four heartbeats glint every ripe crop near them,
	 * and each one that's ready feeds them a little. The look: green sparks on each ripe stalk, a red pulse expanding
	 * from the ally, then threads of light drawn in from the crops.
	 */
	@Pair(a = "fieldsense", b = "heartsense", name = "Harvest Heartbeat", element = "blood", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "The ally the spell struck (you, if none) is healed 1 (times power) for each ripe crop within 6 blocks of them "
			+ "(2 blocks up or down), up to 8, and gets Regeneration I for 4 seconds if there were any. Four heartbeats glint each crop first.")
	public static void harvestHeartbeat(PairCast c) {
		LivingEntity anchor = anchorOf(c);
		BlockPos centre = anchor.blockPosition();
		List<Vec3> ripeCrops = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-6, -2, -6), centre.offset(6, 2, 6))) {
			if (ripeCrops.size() < 8 && ripe(c, p)) {
				ripeCrops.add(Vec3.atCenterOf(p));
			}
		}
		c.every(6, 4, frame -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 now = PairCast.mid(anchor);
			c.sphere(PairCast.shift(0x7BD65A, 0xC8283C, 0.8F), now, 0.5 + frame * 0.6, 14);
			c.sound(SoundEvents.WARDEN_HEARTBEAT, now, 0.8F, 1.0F + 0.1F * frame);
			for (Vec3 crop : ripeCrops) {
				c.particles(ParticleTypes.HAPPY_VILLAGER, crop.add(0, 0.6, 0), 2, 0.2, 0.02);
			}
		});
		c.later(24, () -> {
			if (!anchor.isAlive()) {
				return;
			}
			Vec3 now = PairCast.mid(anchor);
			for (Vec3 crop : ripeCrops) {
				c.line(PairCast.shift(0x7BD65A, 0xFFE08A, 0.6F), crop.add(0, 0.5, 0), now, 3);
				c.heal(anchor, 1 * c.power);
			}
			if (!ripeCrops.isEmpty()) {
				c.effect(anchor, MobEffects.REGENERATION, 4, 0);
				c.ring(PairCast.dust(0x7BD65A, 0.9F), now, 1.6, 20, 0.0);
				c.sound(SoundEvents.BONE_MEAL_USE, now, 0.9F, 1.2F);
			}
		});
	}

	/**
	 * Gloomhound: the struck enemies are shadowed, and a hound of dark circles each one and bites three times. The
	 * look: a violet ring spinning round the target, a dark sphere that swells on each bite, then ink where the last
	 * bite lands.
	 */
	@Pair(a = "gloomsight", b = "shades", name = "Gloomhound", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 8 enemies it strikes are shadowed, and a hound of dark bites each of them three times, half a second apart, "
			+ "the first at once: 2 damage (times power) to it, and 1 (times power) to every other enemy within 2 blocks "
			+ "(times radius) of it, each bite.")
	public static void gloomhound(PairCast c) {
		List<LivingEntity> hunted = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : hunted) {
			c.mark(t, Reactions.Mark.SHADOWED);
		}
		c.every(10, 3, frame -> {
			for (LivingEntity t : c.still(hunted)) {
				Vec3 at = PairCast.mid(t);
				c.ring(PairCast.shift(0x2A0F3D, 0x9B5CFF, 1.1F), at.add(0, -0.3, 0), 1.1, 14, frame * 1.1);
				c.sphere(PairCast.dust(0x3B1E5C, 1.0F), at, 0.5 + frame * 0.3, 10);
				c.sound(SoundEvents.PHANTOM_BITE, at, 0.7F, 0.8F + 0.2F * frame);
				c.wither(t, 2 * c.power);
				for (LivingEntity e : c.enemiesNear(at, 2 * c.radius)) {
					if (e != t) {
						c.wither(e, 1 * c.power);
					}
				}
				if (frame == 2) {
					c.particles(ParticleTypes.SQUID_INK, at, 14, 0.4, 0.05);
				}
			}
		});
	}

	/**
	 * Pale Tithe: a toll taken from each enemy, three times, and paid into the caster. The look: pale motes streaming
	 * from every taxed creature along a thread back to you, a little each second.
	 */
	@Pair(a = "gravefinder", b = "quietus", name = "Pale Tithe", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Three tolls, a second apart, the first at once. Up to 8 enemies it strikes take 2 wither damage (times power) per toll, "
			+ "and the first toll also weakens them (Weakness I) for 3 seconds. You heal 1 (times power) per toll that took anything.")
	public static void paleTithe(PairCast c) {
		List<LivingEntity> taxed = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		c.every(20, 3, frame -> {
			if (!c.caster.isAlive()) {
				return;
			}
			Vec3 me = PairCast.mid(c.caster);
			boolean paid = false;
			for (LivingEntity t : c.still(taxed)) {
				Vec3 at = PairCast.mid(t);
				paid = true;
				c.wither(t, 2 * c.power);
				if (frame == 0) {
					c.effect(t, MobEffects.WEAKNESS, 3, 0);
				}
				c.line(PairCast.shift(0xE8E2FF, 0x6B6480, 0.7F), at, me, 4);
				c.mote(ParticleTypes.SOUL, at, me.subtract(at).scale(0.12));
				c.sound(SoundEvents.SOUL_ESCAPE, at, 0.6F, 0.9F + 0.15F * frame);
			}
			if (paid) {
				c.heal(c.caster, 1 * c.power);
				c.sphere(PairCast.dust(0xE8E2FF, 0.8F), me, 1.0, 12);
			}
		});
	}

	/**
	 * Dear Terms: a bargain struck with the enemies it hits, charged out in three instalments. The look: gold ledger
	 * threads from the point to each target, crimson rings on each charge, and a hammer-fall flash on the last.
	 */
	@Pair(a = "haggle", b = "red_ledger", name = "Dear Terms", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 8 enemies it strikes are charged three times, a second apart, the first at once: 2 damage (times power) per charge. "
			+ "The last charge also leaves each one bleeding and under Weakness II for 4 seconds, and you get Strength I for 4 seconds "
			+ "if any were still there.")
	public static void dearTerms(PairCast c) {
		List<LivingEntity> sold = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 point = c.point();
		c.sound(SoundEvents.VILLAGER_TRADE, point, 0.9F, 0.9F);
		for (LivingEntity t : sold) {
			c.line(PairCast.dust(0xFFD36B, 0.6F), point, PairCast.mid(t), 3);
		}
		c.every(20, 3, frame -> {
			for (LivingEntity t : c.still(sold)) {
				Vec3 at = PairCast.mid(t);
				c.wither(t, 2 * c.power);
				c.ring(PairCast.dust(0x8B0F1F, 0.9F), at.add(0, -0.2, 0), 0.9, 12, frame * 0.4);
				c.particles(ParticleTypes.DAMAGE_INDICATOR, at.add(0, 0.8, 0), 2, 0.3, 0.0);
				c.sound(SoundEvents.VILLAGER_YES, at, 0.7F, 0.8F + 0.1F * frame);
				if (frame == 2) {
					c.mark(t, Reactions.Mark.BLEEDING);
					c.effect(t, MobEffects.WEAKNESS, 4, 1);
					c.sphere(PairCast.shift(0xFFD36B, 0x8B0F1F, 1.0F), at, 1.0, 20);
					c.sound(SoundEvents.ANVIL_LAND, at, 0.5F, 1.6F);
				}
			}
			if (frame == 2 && !c.still(sold).isEmpty()) {
				c.effect(c.caster, MobEffects.STRENGTH, 4, 0);
			}
		});
	}

	/**
	 * Lantern Glare: each enemy struck is lit up like a stage: a glowing beam hangs over it for five seconds and each
	 * second the light flashes on it. The last flash bursts out over the enemies stood close by. The look: a white
	 * column over each target, a pale flash on each beat, and a wide white burst on the last.
	 */
	@Pair(a = "headlamp", b = "prismatic_burst", name = "Lantern Glare", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Up to 8 enemies it strikes glow for 5 seconds, with a beam over each. A flash each second deals 1 magic damage (times power), "
			+ "and the last flash bursts: 3 damage (times power) to every other enemy within 3 blocks (times radius).")
	public static void lanternGlare(PairCast c) {
		List<LivingEntity> lit = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : lit) {
			c.effect(t, MobEffects.GLOWING, 5, 0);
		}
		c.sound(SoundEvents.BEACON_ACTIVATE, c.point(), 0.6F, 1.6F);
		// The beam follows each target for the five seconds it glows.
		c.every(4, 25, frame -> {
			for (LivingEntity t : c.still(lit)) {
				Vec3 at = PairCast.mid(t);
				c.column(PairCast.dust(0xFFF2A8, 0.7F), at.add(0, 2.5, 0), 0.3, 3.0, 10);
			}
		});
		c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(lit)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 1 * c.power);
				c.sphere(PairCast.shift(0xFFFFFF, 0xBFE6FF, 0.9F), at, 0.6 + frame * 0.05, 12);
				c.sound(SoundEvents.FIRECHARGE_USE, at, 0.5F, 1.5F);
				if (frame == 4) {
					c.sphere(PairCast.shift(0xFFFFFF, 0xBFE6FF, 1.3F), at, 3.0 * c.radius, 30);
					c.shake(at, 0.15F, 5);
					for (LivingEntity near : c.enemiesNear(at, 3 * c.radius)) {
						if (near != t) {
							c.hurt(near, 3 * c.power);
						}
					}
				}
			}
		});
	}

	/**
	 * Hearthrun: you dash the way you face, leaving a warm trail, and where you started the hearth's warmth stays
	 * behind for the allies there. The look: a copper ring at the start, a flame trail under your feet, and a warm
	 * ring where you left.
	 */
	@Pair(a = "hearthpath", b = "homeward", name = "Hearthrun", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power"},
		text = "You dash up to 10 blocks the way you face, stopping at walls and never through one, leaving a warm trail. Where you "
			+ "started, allies within 3 blocks are healed 2 (times power) and get Regeneration I for 4 seconds.")
	public static void hearthrun(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 look = self.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return;
		}
		Vec3 forward = flat.normalize();
		Vec3 chest = start.add(0, 0.9, 0);
		HitResult wall = c.level.clip(new ClipContext(chest, chest.add(forward.scale(10)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, self));
		double reach = wall.getType() == HitResult.Type.MISS ? 10 : Math.max(0, wall.getLocation().distanceTo(chest) - 0.8);
		double run = reach;
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, start, 0.8F, 1.3F);
		c.disc(PairCast.dust(0xFFB347, 0.9F), start.add(0, 0.1, 0), 1.0, 16);
		boolean[] halted = {false};
		c.every(2, 5, frame -> {
			if (halted[0] || !self.isAlive()) {
				return;
			}
			Vec3 before = PairCast.mid(self);
			Vec3 next = c.ground(start.add(forward.scale(run * (frame + 1) / 5.0)));
			if (!c.blink(self, next)) {
				halted[0] = true;
				return;
			}
			Vec3 now = PairCast.mid(self);
			c.line(PairCast.shift(0xFFB347, 0xFF8A3D, 0.8F), before.add(0, -0.6, 0), now.add(0, -0.6, 0), 2);
			c.particles(ParticleTypes.FLAME, now.add(0, -0.5, 0), 3, 0.3, 0.02);
		});
		c.later(10, () -> {
			for (LivingEntity a : c.alliesNear(start, 3)) {
				c.heal(a, 2 * c.power);
				c.effect(a, MobEffects.REGENERATION, 4, 0);
			}
			c.ring(PairCast.shift(0xFFB347, 0xFFF1C2, 0.9F), start.add(0, 0.2, 0), 2.5, 24, 0.0);
			c.sound(SoundEvents.FIRECHARGE_USE, start, 0.8F, 1.0F);
		});
	}

	/**
	 * Drover's Pen: the enemies round the point are herded in, the way a drover works a flock: a ring of pen closes
	 * round them in four shoves, and each one is slowed and lit up for the pen. The look: a tan ring shrinking over
	 * half a second each, hoof sounds with every shove, and a green glow on each creature penned.
	 */
	@Pair(a = "corral", b = "herdsense", name = "Drover's Pen", element = "wind", kind = EffectKind.HARMFUL,
		traits = {"radius"},
		text = "Up to 8 enemies within 5 blocks (times radius) of the point are herded toward it by four shoves, half a second apart, "
			+ "while the pen shrinks. Each is also Slowness II and Glowing for 4 seconds. Bosses are not moved.")
	public static void droversPen(PairCast c) {
		Vec3 centre = c.point();
		double r = 5 * c.radius;
		List<LivingEntity> herd = PairCast.first(c.enemiesNear(centre, r), PairCast.MAX_TARGETS);
		c.every(10, 4, frame -> {
			c.ring(PairCast.shift(0xC9A36A, 0xB8F0D0, 0.8F), centre.add(0, 0.2, 0), r - frame * 0.9, 32, frame * 0.25);
			for (LivingEntity t : c.still(herd)) {
				Vec3 at = PairCast.mid(t);
				Vec3 inward = new Vec3(centre.x - at.x, 0, centre.z - at.z);
				if (inward.lengthSqr() > 0.25) {
					c.push(t, inward.normalize().scale(0.35));
				}
				c.effect(t, MobEffects.SLOWNESS, 4, 1);
				c.effect(t, MobEffects.GLOWING, 4, 0);
				c.sound(SoundEvents.HORSE_GALLOP, at, 0.5F, 0.8F + 0.1F * frame);
			}
		});
	}

	/**
	 * Lantern Vigil: a lantern's light follows the ally for twenty seconds, and the first time they fall low it flares
	 * for them as the last lantern does. The look: a small amber ring turning round the ally's head, then a column of
	 * bright light and a gold burst when it flares.
	 */
	@Pair(a = "lantern_soul", b = "last_lantern", name = "Lantern Vigil", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "The ally the spell struck (you, if none) is followed by a lantern's light for 20 seconds. The first time they fall to "
			+ "6 health (3 hearts) or less in that time, the last lantern flares: they are healed 6 (times power) and get "
			+ "Regeneration II for 4 seconds.")
	public static void lanternVigil(PairCast c) {
		LivingEntity ward = anchorOf(c);
		c.sound(SoundEvents.LANTERN_PLACE, PairCast.mid(ward), 0.9F, 1.1F);
		c.every(5, 80, frame -> {
			if (!ward.isAlive()) {
				return;
			}
			Vec3 at = PairCast.mid(ward);
			c.ring(PairCast.shift(0xFFE08A, 0xFFF8DC, 0.6F), at.add(0, 1.2 + 0.3 * Math.sin(frame * 0.1), 0), 0.9, 6, frame * 0.3);
			if (ward.getHealth() <= 6 && c.once("last-lantern")) {
				c.heal(ward, 6 * c.power);
				c.effect(ward, MobEffects.REGENERATION, 4, 1);
				c.sphere(PairCast.shift(0xFFE08A, 0xFFF8DC, 1.1F), at, 1.6, 30);
				c.column(PairCast.dust(0xFFF8DC, 1.0F), at, 0.6, 3.0, 16);
				c.sound(SoundEvents.BEACON_ACTIVATE, at, 0.8F, 1.5F);
			}
		});
	}

	/**
	 * Thrift Spring: a spring of blue light welling up at the point, tending whoever stands in it, and giving most
	 * where it's needed. The look: a blue disc on the ground, a gold column rising on each beat, and sparkles on
	 * every ally it heals.
	 */
	@Pair(a = "lapis_thrift", b = "manawell", name = "Thrift Spring", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A spring of blue light wells up at the point for 8 seconds, reaching 3 blocks out (times radius). Five times, two seconds "
			+ "apart, the first at once, each ally inside it is healed 1 (times power), or 2 (times power) if they're under half health.")
	public static void thriftSpring(PairCast c) {
		Vec3 spring = c.point();
		double r = 3 * c.radius;
		c.sound(SoundEvents.BUCKET_FILL, spring, 0.8F, 1.2F);
		c.disc(PairCast.dust(0x7FB8FF, 1.0F), spring.add(0, 0.1, 0), r, 24);
		c.every(40, 5, frame -> {
			c.column(PairCast.shift(0x7FB8FF, 0xF3D36B, 1.0F), spring.add(0, 0.1, 0), r * 0.5, 2.5, 20);
			for (LivingEntity a : c.alliesNear(spring, r)) {
				double amount = a.getHealth() < a.getMaxHealth() / 2 ? 2 : 1;
				c.heal(a, amount * c.power);
				c.particles(ParticleTypes.ENCHANT, PairCast.mid(a), 4, 0.4, 0.05);
			}
			c.sound(SoundEvents.BEACON_POWER_SELECT, spring, 0.6F, 1.0F + 0.1F * frame);
		});
	}
}
