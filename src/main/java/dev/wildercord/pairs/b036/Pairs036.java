package dev.wildercord.pairs.b036;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs036 {
	private Pairs036() {}

	/**
	 * Moonbitten: the moon's shadow bites twice. The look: a pale moon ring tightening over each target, then a
	 * dark column with a sphere of shadow, and a second bite that leaps along the enemies beside them.
	 */
	@Pair(a = "moon_reading", b = "umbra", name = "Moonbitten", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Shadows up to 8 enemies: 4 withering damage and shadowed. A second later the moon bites again: each still there "
			+ "takes 3 more, and each other enemy within 2.5 blocks of one takes 2, once.")
	public static void moonbitten(PairCast c) {
		List<LivingEntity> bitten = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : bitten) {
			c.wither(t, 4 * c.power);
			c.mark(t, Reactions.Mark.SHADOWED);
		}
		c.sound(SoundEvents.PHANTOM_BITE, c.point(), 0.7F, 0.8F);
		// Wind-up: a pale moon ring tightens over each target, four frames a fifth of a second apart.
		c.every(4, 4, frame -> {
			for (LivingEntity t : c.still(bitten)) {
				c.ring(PairCast.dust(0xD6E4FF, 1.0F), PairCast.mid(t).add(0, 1.6, 0), 1.2 - frame * 0.25, 14, frame * 0.5);
			}
		});
		// Payoff: the second bite, a second on, and it leaps once to each enemy beside a bitten one.
		c.later(20, () -> {
			Set<LivingEntity> leapt = new HashSet<>();
			for (LivingEntity t : c.still(bitten)) {
				Vec3 at = PairCast.mid(t);
				c.wither(t, 3 * c.power);
				c.column(PairCast.shift(0xE6EEFF, 0x2A1A4A, 0.9F), t.position(), 0.8, 2.5, 18);
				c.sphere(PairCast.dust(0x2A1A4A, 1.1F), at, 1.0, 22);
				c.particles(ParticleTypes.SOUL, at, 6, 0.3, 0.02);
				for (LivingEntity near : c.enemiesNear(at, 2.5 * c.radius)) {
					if (near != t && !bitten.contains(near) && leapt.add(near)) {
						c.line(PairCast.dust(0xD6E4FF, 0.8F), at, PairCast.mid(near), 4);
						c.wither(near, 2 * c.power);
					}
				}
			}
			c.sound(SoundEvents.PHANTOM_AMBIENT, c.point(), 0.8F, 0.6F);
		});
	}

	/**
	 * Honeyed Swarm: a swarm of bees gathers round each target and stings it three times, and the last sting leaves
	 * honey. The look: golden orbiting dust closing in, then three stings with a flash of damage, and a drip of honey.
	 */
	@Pair(a = "hivehum", b = "venom", name = "Honeyed Swarm", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Up to 6 enemies are swarmed: three stings half a second apart, 2 damage each. The first sting brings Poison I "
			+ "for 3 seconds; the last leaves honey: Slowness II for 3 seconds.")
	public static void honeyedSwarm(PairCast c) {
		List<LivingEntity> swarmed = PairCast.first(c.enemies(), 6);
		c.sound(SoundEvents.BEE_LOOP, c.point(), 0.8F, 1.1F);
		// Wind-up: the bees gather round each target, closing in over the first beat.
		c.every(3, 5, frame -> {
			for (LivingEntity t : c.still(swarmed)) {
				c.sphere(PairCast.shift(0xFFD35A, 0x4A3000, 0.8F), PairCast.mid(t), 1.4 - frame * 0.22, 12);
			}
		});
		// Payoff: three stings, ten ticks apart, the first poisons and the last one leaves honey.
		c.later(16, () -> c.every(10, 3, frame -> {
			for (LivingEntity t : c.still(swarmed)) {
				Vec3 at = PairCast.mid(t);
				c.hurt(t, 2 * c.power);
				c.sound(SoundEvents.BEE_LOOP, at, 0.6F, 1.0F + frame * 0.25F);
				c.particles(ParticleTypes.DAMAGE_INDICATOR, at, 2, 0.2, 0.05);
				if (frame == 0) {
					c.effect(t, MobEffects.POISON, 3, 0);
				}
				if (frame == 2) {
					c.effect(t, MobEffects.SLOWNESS, 3, 1);
					c.particles(ParticleTypes.FALLING_HONEY, at, 10, 0.3, 0.05);
					c.sphere(PairCast.dust(0xFFC83A, 1.0F), at, 1.0, 20);
				}
			}
		}));
	}

	/**
	 * Hothouse Shell: allies are given a barrier of light, and a glass shell of quickened time grows round them. For
	 * five seconds the time inside runs fast and each ally heals. The look: a pale dome growing, then green rings
	 * blooming to gold at the fifth beat.
	 */
	@Pair(a = "barrier", b = "cloche", name = "Hothouse Shell", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "duration"},
		text = "Allies get 4 absorption (2 hearts) for 20 seconds, under a glass shell of quick time. Then, once a second for "
			+ "5 seconds, each heals 1 health.")
	public static void hothouseShell(PairCast c) {
		List<LivingEntity> sheltered = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : sheltered) {
			c.absorb(t, 4 * c.power, 20);
			c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, PairCast.mid(t), 0.6F, 1.4F);
		}
		// Wind-up: a pale glass dome grows round each ally.
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(sheltered)) {
				c.sphere(PairCast.dust(0xBDF2FF, 0.9F), PairCast.mid(t), 0.6 + frame * 0.2, 18);
			}
		});
		// Quick time: once a second for five seconds, each ally heals and a ring blooms at its feet.
		c.later(20, () -> c.every(20, 5, frame -> {
			for (LivingEntity t : c.still(sheltered)) {
				Vec3 at = PairCast.mid(t);
				c.heal(t, 1 * c.power);
				c.ring(PairCast.shift(0x9CFF8A, 0xFFF6B0, 0.8F), t.position().add(0, 0.1, 0), 0.9, 12, frame * 0.5);
				if (frame == 4) {
					c.wave(ParticleTypes.END_ROD, at, 12, 0.2);
				}
			}
			if (frame == 4) {
				c.sound(SoundEvents.BELL_RESONATE, c.point(), 0.6F, 1.2F);
			}
		}));
	}

	/**
	 * Rewound Hour: the allies' places and health are kept, and three seconds later the clock turns back. The look: a
	 * ring of light turning backwards over each ally, then a streak of light from where each one was to where it is.
	 */
	@Pair(a = "rewind", b = "trade_renew", name = "Rewound Hour", element = "time", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Records each ally (up to 8) where it stands and its health. Three seconds later the clock turns back: each still "
			+ "here returns to that spot if it fits, healed toward the health it had if it is lower (8 at most), with Regeneration II for 4 seconds.")
	public static void secondDawn(PairCast c) {
		List<LivingEntity> kept = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		Map<LivingEntity, Vec3> spots = new HashMap<>();
		Map<LivingEntity, Float> health = new HashMap<>();
		for (LivingEntity t : kept) {
			spots.put(t, t.position());
			health.put(t, t.getHealth());
		}
		c.sound(SoundEvents.BEACON_AMBIENT, c.point(), 0.6F, 1.8F);
		// Wind-up: a ring of light turns backwards over each ally for three seconds: the clock's hand.
		c.every(10, 6, frame -> {
			for (LivingEntity t : c.still(kept)) {
				c.ring(PairCast.shift(0xFFE9A8, 0xB8D8FF, 0.8F), PairCast.mid(t).add(0, 1.0, 0), 0.9, 12, -frame * 0.5);
			}
		});
		// Payoff: the clock turns back, and each ally is restored to what it was when the record was made.
		c.later(60, () -> {
			for (LivingEntity t : c.still(kept)) {
				Vec3 before = PairCast.mid(t);
				c.blink(t, spots.get(t));
				float was = health.get(t);
				if (t.getHealth() < was) {
					c.heal(t, Math.min(8 * c.power, was - t.getHealth()));
				}
				c.effect(t, MobEffects.REGENERATION, 4, 1);
				c.line(PairCast.shift(0xB8D8FF, 0xFFE9A8, 0.8F), before, PairCast.mid(t), 3);
				c.sphere(PairCast.dust(0xFFF3CC, 0.9F), PairCast.mid(t), 0.9, 16);
			}
			c.sound(SoundEvents.ENDERMAN_TELEPORT, c.point(), 0.4F, 1.5F);
		});
	}

	/**
	 * Voidwake: the caster blinks to where the spell landed, and a void disc opens there and implodes. The look: a
	 * portal burst at the start, a dark disc spreading with its rim drawn in, then a sphere of void light and a wave.
	 */
	@Pair(a = "blink", b = "void_step", name = "Voidwake", element = "void", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "You blink to where the spell landed, if the spot is safe, with Speed I for 4 seconds. Two seconds later a void disc "
			+ "implodes there: each enemy within 3 blocks is pulled in and takes 5 withering damage.")
	public static void voidwake(PairCast c) {
		Vec3 spot = c.ground(c.point());
		Vec3 leaving = PairCast.mid(c.caster);
		c.particles(ParticleTypes.REVERSE_PORTAL, leaving, 24, 0.4, 0.4);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, leaving, 0.8F, 0.7F);
		c.blink(c.caster, c.point());
		c.effect(c.caster, MobEffects.SPEED, 4, 0);
		c.sound(SoundEvents.ENDERMAN_TELEPORT, PairCast.mid(c.caster), 0.6F, 1.4F);
		// Wind-up: a disc of void spreads over the landing spot while its rim draws in, for two seconds.
		c.every(10, 5, frame -> {
			c.disc(PairCast.dust(0x2A0B4A, 1.2F), spot, 3.0 * c.radius, 30);
			c.ring(PairCast.shift(0x8A5AD8, 0x2A0B4A, 0.9F), spot.add(0, 0.1, 0), 3.0 * c.radius * (1 - frame * 0.15), 18, frame * 0.3);
			if (frame == 4) {
				implode(c, spot);
			}
		});
	}

	private static void implode(PairCast c, Vec3 spot) {
		Vec3 core = spot.add(0, 1, 0);
		c.sound(SoundEvents.ENDER_EYE_DEATH, spot, 0.9F, 0.6F);
		c.shake(spot, 0.3F, 10);
		c.sphere(PairCast.shift(0xE0C8FF, 0x2A0B4A, 1.2F), core, 1.5, 30);
		c.wave(ParticleTypes.END_ROD, core, 14, 0.3);
		for (LivingEntity near : c.enemiesNear(spot, 3 * c.radius)) {
			c.pullTo(near, spot, 1.0);
			c.wither(near, 5 * c.power);
		}
	}

	/**
	 * Hollow Feast: a dark maw closes on each target and bites, harder the more health it has lost. For two seconds
	 * after, the hollow feeds: each target that dies gives the caster absorption. The look: a maw of dust tightening,
	 * a column of violet light at the bite, and a thread of light from each death to the caster.
	 */
	@Pair(a = "devour", b = "hollow_pocket", name = "Hollow Feast", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 4 enemies take 4 withering damage, plus 1 for each tenth of their health they've lost (up to 10 more). For 2 "
			+ "seconds after, each one that dies gives you 4 absorption for 10 seconds.")
	public static void hollowFeast(PairCast c) {
		List<LivingEntity> hollowed = PairCast.first(c.enemies(), 4);
		c.sound(SoundEvents.SOUL_ESCAPE, c.point(), 0.8F, 0.6F);
		// Wind-up: a dark maw of dust closes on each target, tightening over the first half second.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(hollowed)) {
				c.sphere(PairCast.dust(0x2B1640, 1.0F), PairCast.mid(t), 1.6 - frame * 0.4, 16);
			}
		});
		// Payoff: the bite, and then the feed watches for deaths for two seconds.
		c.later(12, () -> {
			for (LivingEntity t : c.still(hollowed)) {
				double missing = 1 - t.getHealth() / t.getMaxHealth();
				int extra = (int) Math.min(10, Math.floor(missing * 10));
				c.wither(t, (4 + extra) * c.power);
				c.column(PairCast.shift(0xC79CFF, 0x2B1640, 0.9F), t.position(), 0.7, 2.2, 16);
			}
			c.sound(SoundEvents.WITHER_SHOOT, c.point(), 0.4F, 1.4F);
		});
		Set<LivingEntity> fed = new HashSet<>();
		c.later(12, () -> c.every(4, 11, frame -> {
			for (LivingEntity t : hollowed) {
				if (!t.isAlive() && fed.add(t)) {
					c.line(PairCast.dust(0xC79CFF, 0.8F), PairCast.mid(t), PairCast.mid(c.caster), 3);
					c.absorb(c.caster, 4 * c.power, 10);
					c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, PairCast.mid(c.caster), 0.6F, 0.6F);
				}
			}
		}));
	}

	/**
	 * Wraithcloak: allies vanish into shadow, and the monsters near them lose their way while they do. When it fades
	 * an afterimage bursts into the enemies beside them. The look: dark wisps winding round each ally, a sink of
	 * violet into it, then a flare of shadow where it ends.
	 */
	@Pair(a = "frame_veil", b = "phantom", name = "Wraithcloak", element = "void", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "The allies it reaches turn invisible for 4 seconds. Every half second meanwhile, monsters within 8 blocks of them "
			+ "lose their target and stop. When it fades, the afterimage bursts: 3 withering damage to each enemy within 2 blocks of an ally.")
	public static void wraithcloak(PairCast c) {
		List<LivingEntity> cloaked = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		int span = c.ticks(4);
		int beats = span / 10 + 1;
		for (LivingEntity t : cloaked) {
			c.effect(t, MobEffects.INVISIBILITY, 4, 0);
		}
		c.sound(SoundEvents.PHANTOM_AMBIENT, c.point(), 0.6F, 1.4F);
		// Wind-up: dark wisps wind round each ally and sink into it.
		c.every(4, 3, frame -> {
			for (LivingEntity t : c.still(cloaked)) {
				c.spiral(PairCast.shift(0x9C7BFF, 0x2B1640, 0.8F), PairCast.mid(t), 1.2, 1.6, 2, 10);
			}
		});
		// The cloak: every half second, the monsters near the cloaked lose their target and stop where they are.
		c.every(10, beats, frame -> {
			for (LivingEntity t : c.still(cloaked)) {
				for (LivingEntity near : c.enemiesNear(PairCast.mid(t), 8 * c.radius)) {
					if (near instanceof Mob mob && c.movable(mob)) {
						mob.setTarget(null);
						mob.getNavigation().stop();
					}
				}
			}
		});
		// Afterimage: when it fades, a flare of shadow, and 3 withering damage to each enemy beside an ally, once each.
		c.later(span, () -> {
			Set<LivingEntity> burst = new HashSet<>();
			for (LivingEntity t : c.still(cloaked)) {
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.shift(0x9C7BFF, 0x2B1640, 1.0F), at, 1.4, 24);
				c.particles(ParticleTypes.WITCH, at, 8, 0.3, 0.05);
				for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
					if (burst.add(near)) {
						c.wither(near, 3 * c.power);
					}
				}
			}
			c.sound(SoundEvents.PHANTOM_DEATH, c.point(), 0.7F, 1.2F);
		});
	}

	/**
	 * Gravemark: a mark of harm, then a gravestone of light falls on each marked target. The look: a ring of pale light
	 * scribed round each target and drawn in to its feet, then a shaft of light from the sky and a stone's shadow.
	 */
	@Pair(a = "grave_bearing", b = "harm", name = "Gravemark", element = "arcane", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Harms up to 8 enemies: 7 magic damage, and each is marked exposed. Two seconds later a gravestone of light falls on "
			+ "each one still there: 6 more damage, and 3 to each other enemy within 2 blocks.")
	public static void gravemark(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), 8);
		for (LivingEntity t : marked) {
			c.hurt(t, 7 * c.power);
			c.mark(t, Reactions.Mark.EXPOSED);
		}
		c.sound(SoundEvents.BELL_BLOCK, c.point(), 0.8F, 0.6F);
		// Wind-up: a ring of pale light is scribed round each target and draws in to its feet.
		c.every(10, 3, frame -> {
			for (LivingEntity t : c.still(marked)) {
				c.ring(PairCast.shift(0xE8E0FF, 0x5A4A7A, 0.9F), t.position().add(0, 0.1, 0), 2.0 - frame * 0.6, 16, frame * 0.4);
			}
		});
		// Payoff: the stone falls on each one still there, and its fall spreads to the enemies beside it.
		c.later(40, () -> {
			Set<LivingEntity> spread = new HashSet<>();
			for (LivingEntity t : c.still(marked)) {
				Vec3 at = PairCast.mid(t);
				c.line(ParticleTypes.END_ROD, at.add(0, 7, 0), at, 3);
				c.strike(t, 6 * c.power);
				c.sphere(PairCast.shift(0xE8E0FF, 0x5A4A7A, 1.0F), at, 1.2, 24);
				c.sound(SoundEvents.ANVIL_LAND, at, 0.6F, 0.6F);
				c.shake(at, 0.25F, 8);
				for (LivingEntity near : c.enemiesNear(at, 2 * c.radius)) {
					if (!marked.contains(near) && spread.add(near)) {
						c.hurt(near, 3 * c.power);
					}
				}
			}
		});
	}

	/**
	 * Exile Beacon: enemies are thrown out of reach and dazed, and three seconds later a beacon calls them back. The
	 * look: a violet thread from you to each one as it goes, a column of void light over each while the beacon
	 * pulses, then the recall with a thread back.
	 */
	@Pair(a = "banish", b = "spawner_sense", name = "Exile Beacon", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "duration"},
		text = "Banishes up to 8 enemies up to 8 blocks further from you, where the spot is safe, and dazes them (Slowness II for "
			+ "3 seconds). Three seconds later the beacon calls each back to 3 blocks from you, where it fits: 4 withering damage.")
	public static void exileBeacon(PairCast c) {
		List<LivingEntity> banished = PairCast.first(c.enemies(), PairCast.MAX_TARGETS);
		Vec3 me = c.caster.position();
		int recall = c.ticks(3);
		c.sound(SoundEvents.PORTAL_TRAVEL, c.point(), 0.4F, 1.6F);
		for (LivingEntity t : banished) {
			Vec3 out = outward(c, PairCast.mid(t).subtract(me));
			c.line(PairCast.dust(0x6A4AB0, 0.8F), PairCast.mid(c.caster), PairCast.mid(t), 2);
			c.blink(t, t.position().add(out.scale(8 * c.radius)));
			c.effect(t, MobEffects.SLOWNESS, 3, 1);
		}
		// Wind-up: a column of void light stands over each exiled enemy and pulses while the recall comes.
		c.every(10, Math.max(1, recall / 10), frame -> {
			for (LivingEntity t : c.still(banished)) {
				c.column(PairCast.shift(0x8A5AD8, 0x2A0B4A, 0.8F), t.position(), 0.5, 3.0, 10);
			}
		});
		// Recall: the beacon pulls each one back to 3 blocks from you, where it takes 4 damage.
		c.later(recall, () -> {
			for (LivingEntity t : c.still(banished)) {
				Vec3 back = outward(c, PairCast.mid(t).subtract(c.caster.position()));
				Vec3 spot = c.caster.position().add(back.scale(3));
				c.line(PairCast.dust(0x8A5AD8, 0.8F), PairCast.mid(t), PairCast.mid(c.caster), 3);
				c.blink(t, spot);
				c.wither(t, 4 * c.power);
				c.sphere(PairCast.shift(0x8A5AD8, 0x2A0B4A, 0.9F), PairCast.mid(t), 0.9, 14);
			}
			c.sound(SoundEvents.PORTAL_TRAVEL, c.point(), 0.4F, 0.9F);
		});
	}

	/** The flat direction from the caster out through {@code from}, as a unit vector (never zero). */
	private static Vec3 outward(PairCast c, Vec3 from) {
		Vec3 d = new Vec3(from.x, 0, from.z);
		if (d.lengthSqr() < 1.0E-4) {
			d = new Vec3(c.dir().x, 0, c.dir().z);
		}
		if (d.lengthSqr() < 1.0E-4) {
			d = new Vec3(1, 0, 0);
		}
		return d.normalize();
	}

	/**
	 * Reaping Tithe: enemies near a point are drawn in, and two seconds later whoever has gathered takes a tithe that
	 * grows with the crowd. The look: golden motes streaming in along lines to the point, then a ring of gold and a
	 * violet burst on each one taxed.
	 */
	@Pair(a = "collect", b = "pull", name = "Reaping Tithe", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Pulls up to 6 enemies within 6 blocks toward the point, marked pulled. Two seconds later each one gathered within "
			+ "2 blocks takes 4 withering damage, and 1 more for each other gathered enemy (up to 5).")
	public static void reapingTithe(PairCast c) {
		Vec3 at = c.point();
		List<LivingEntity> pulled = PairCast.first(c.enemiesNear(at, 6 * c.radius), 6);
		c.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, at, 0.8F, 0.6F);
		// Wind-up: the enemies are drawn in for a second, and golden motes stream in along a line from each.
		c.every(4, 5, frame -> {
			for (LivingEntity t : c.still(pulled)) {
				c.pullTo(t, at, 0.9);
				c.mark(t, Reactions.Mark.PULLED);
				c.line(PairCast.shift(0xE8C872, 0x4A1A6A, 0.7F), PairCast.mid(t), at, 1.5);
			}
		});
		// Payoff: the tithe is taken from whoever is gathered, more of it the bigger the crowd.
		c.later(40, () -> {
			List<LivingEntity> gathered = new ArrayList<>();
			for (LivingEntity t : c.still(pulled)) {
				if (PairCast.mid(t).distanceTo(at) <= 2 * c.radius) {
					gathered.add(t);
				}
			}
			int extra = Math.min(5, Math.max(0, gathered.size() - 1));
			for (LivingEntity t : gathered) {
				Vec3 mid = PairCast.mid(t);
				c.wither(t, (4 + extra) * c.power);
				c.particles(ParticleTypes.ENCHANT, mid, 8, 0.3, 0.1);
			}
			c.ring(PairCast.shift(0xFFE27A, 0x4A1A6A, 1.0F), at.add(0, 0.2, 0), 2.0 * c.radius, 20, 0);
			c.sound(SoundEvents.BELL_BLOCK, at, 0.6F, 1.5F);
		});
	}
}
