package dev.wildercord.pairs.b044;

import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs044 {
	private Pairs044() {}

	/**
	 * Tidelamp: a lantern of water over each ally, shining through the rain's healing. Allies are doused, then three
	 * pulses heal them; a light of bubbles rises from their feet to a glowing lamp above their heads.
	 */
	@Pair(a = "manatide", b = "tide_lantern", name = "Tidelamp", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies are doused and a water lantern rises over each. Three pulses, two seconds apart (the first at "
			+ "once): each heals 2 times power, or 3 times power while it rains.")
	public static void tidelamp(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		for (LivingEntity t : allies) {
			c.douse(t);
			c.spiral(ParticleTypes.BUBBLE_COLUMN_UP, t.position(), 0.5, t.getBbHeight() + 0.6, 2, 16);
		}
		c.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, c.point(), 0.9F, 1.1F);
		c.every(40, 3, frame -> {
			boolean rain = c.level.isRaining();
			for (LivingEntity t : c.still(allies)) {
				Vec3 lamp = t.position().add(0, t.getBbHeight() + 0.6, 0);
				c.sphere(PairCast.shift(0x9BE7FF, 0x2F7FD6, 0.9F), lamp, 0.3, 12);
				c.particles(ParticleTypes.BUBBLE_POP, PairCast.mid(t), 6, 0.3, 0.05);
				c.heal(t, (rain ? 3 : 2) * c.power);
			}
			c.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, c.point(), 0.9F, 0.9F + 0.15F * frame);
		});
	}

	/**
	 * Buoy Tempest: a glowing buoy sets where the spell lands, and a second later thunder rings the enemies round it.
	 * The ring of water expands from the buoy, then three lightning pulses crackle out to whatever still stands near.
	 */
	@Pair(a = "thunder_tide", b = "tide_marker", name = "Buoy Tempest", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Each enemy hit takes 2 lightning and glows for 6 seconds. A buoy sets at the point: a second later a ring of "
			+ "thunder soaks every enemy within 4 blocks of it and hits them for 3. Then three pulses, two seconds apart, give "
			+ "2 lightning to those within 3 blocks of the buoy.")
	public static void buoyTempest(PairCast c) {
		Vec3 buoy = c.point();
		for (LivingEntity t : PairCast.first(c.enemies(), 8)) {
			c.shock(t, 2 * c.power);
			c.effect(t, MobEffects.GLOWING, 6, 0);
		}
		c.line(PairCast.dust(0x3FA7FF, 1.0F), buoy.add(0, 4, 0), buoy, 3);
		c.sound(SoundEvents.BELL_RESONATE, buoy, 1.0F, 1.3F);
		c.later(20, () -> {
			c.every(4, 5, k -> c.ring(PairCast.shift(0x3FA7FF, 0xE8F4FF, 1.0F), buoy, 0.8 + 0.8 * k, 24, 0.3 * k));
			c.bolt(buoy);
			c.sound(SoundEvents.TRIDENT_THUNDER, buoy, 0.9F, 1.0F);
			for (LivingEntity t : c.still(c.enemiesNear(buoy, 4))) {
				c.shock(t, 3 * c.power);
				c.mark(t, Reactions.Mark.SOAKED);
			}
		});
		c.later(60, () -> c.every(40, 3, k -> {
			for (LivingEntity t : c.still(c.enemiesNear(buoy, 3))) {
				c.zigzag(ParticleTypes.ELECTRIC_SPARK, buoy, PairCast.mid(t), 0.4, 2);
				c.shock(t, 2 * c.power);
			}
			c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, buoy, 0.4F, 1.6F);
		}));
	}

	/**
	 * Mending Hum: a copper hum tightens round each ally, and the damaged gear in their hands and on them mends twice.
	 * The first mend comes after a shrinking ring of sparks, the second four seconds later with a beacon's chime.
	 */
	@Pair(a = "tinker_hum", b = "toolmend", name = "Mending Hum", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies: every damaged tool or armour piece they hold or wear mends 8 durability, and mends 8 more "
			+ "four seconds later. Both times 8 is multiplied by power.")
	public static void mendingHum(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		int amount = (int) Math.round(8 * c.power);
		c.every(4, 4, k -> {
			for (LivingEntity t : c.still(allies)) {
				c.ring(PairCast.shift(0xE0A060, 0xFFE08A, 0.9F), t.position().add(0, 0.2, 0), 1.2 - 0.25 * k, 14, 0.3 * k);
			}
			c.sound(SoundEvents.ANVIL_LAND, c.point(), 0.4F, 1.6F);
		});
		c.later(16, () -> mend(c, allies, amount));
		c.later(96, () -> {
			mend(c, allies, amount);
			c.sound(SoundEvents.BEACON_POWER_SELECT, c.point(), 0.6F, 1.6F);
		});
	}

	private static void mend(PairCast c, List<LivingEntity> allies, int amount) {
		for (LivingEntity t : c.still(allies)) {
			boolean mended = false;
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
					EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack item = t.getItemBySlot(slot);
				if (item.isDamageableItem() && item.getDamageValue() > 0) {
					item.setDamageValue(Math.max(0, item.getDamageValue() - amount));
					mended = true;
				}
			}
			if (mended) {
				c.ring(PairCast.shift(0xE0A060, 0xFFE08A, 0.9F), t.position().add(0, 0.2, 0), 0.8, 14, 0);
				c.particles(ParticleTypes.CRIT, PairCast.mid(t), 8, 0.3, 0.1);
			}
		}
	}

	/**
	 * Shard Prospect: crystal shells (Resistance and absorption) on the allies, then glints that fly from each one to the
	 * nearest enemy and mark it. The shell is an amethyst sphere that pulses with each glint.
	 */
	@Pair(a = "geode", b = "treasure_sense", name = "Shard Prospect", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies get Resistance I for 6 seconds and 4 absorption (2 hearts) for 8 seconds. Then three glints, two "
			+ "seconds apart, the first at once: each flies from an ally to the nearest enemy within 6 blocks, dealing 2 times "
			+ "power and making it glow for 3 seconds.")
	public static void shardProspect(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		for (LivingEntity t : allies) {
			c.effect(t, MobEffects.RESISTANCE, 6, 0);
			c.absorb(t, 4, 8);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 1.0F, 0.8F);
		c.every(40, 3, k -> {
			for (LivingEntity t : c.still(allies)) {
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.shift(0x9B6BFF, 0xFFE08A, 0.9F), at, 1.0, 14);
				LivingEntity glint = c.nearestEnemy(at, 6, null);
				if (glint != null) {
					c.line(PairCast.dust(0xD9A8FF, 0.8F), at, PairCast.mid(glint), 3);
					c.strike(glint, 2 * c.power);
					c.effect(glint, MobEffects.GLOWING, 3, 0);
					c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, PairCast.mid(glint), 0.8F, 1.4F);
				}
			}
		});
	}

	/**
	 * Sentry Web: lightning joins up to four marked enemies, and the strands stay for six seconds as a web. Each
	 * pulse hits everything inside it, and anything walking in for the first time takes a little more.
	 */
	@Pair(a = "stormweave", b = "watchweft", name = "Sentry Web", element = "storm", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Marks up to 4 enemies: each takes 3 lightning, and lightning strands join them. Then a web stays where they "
			+ "stood, reaching 1.5 blocks past the farthest: from one second on, four pulses 1.5 seconds apart give 2 lightning "
			+ "to each enemy in it, and anything that walks in for the first time takes 2 more.")
	public static void sentryWeb(PairCast c) {
		List<LivingEntity> marked = PairCast.first(c.enemies(), 4);
		if (marked.isEmpty()) {
			return;
		}
		Set<LivingEntity> caught = new HashSet<>(marked);
		Vec3 sum = Vec3.ZERO;
		for (LivingEntity t : marked) {
			sum = sum.add(PairCast.mid(t));
		}
		final Vec3 centre = sum.scale(1.0 / marked.size());
		double widest = 1.5;
		for (LivingEntity t : marked) {
			widest = Math.max(widest, PairCast.mid(t).distanceTo(centre) + 1.5);
		}
		final double reach = widest;
		for (LivingEntity t : marked) {
			c.shock(t, 3 * c.power);
		}
		for (int i = 0; i < marked.size() && marked.size() > 1; i++) {
			c.zigzag(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(marked.get(i)),
				PairCast.mid(marked.get((i + 1) % marked.size())), 0.5, 3);
		}
		c.sound(SoundEvents.TRIDENT_THUNDER, centre, 0.7F, 1.3F);
		c.later(20, () -> c.every(30, 4, k -> {
			c.ring(PairCast.shift(0xBFE6FF, 0xFFFFFF, 0.8F), centre, reach, 24, 0.2 * k);
			for (LivingEntity t : c.still(c.enemiesNear(centre, reach))) {
				c.shock(t, 2 * c.power);
				if (caught.add(t)) {
					c.shock(t, 2 * c.power);
				}
			}
			c.sound(SoundEvents.CHAIN_HIT, centre, 0.6F, 1.2F + 0.1F * k);
		}));
	}

	/**
	 * Stillwater: a still ring of cold water round each ally, drawn every two seconds. It douses them each time and
	 * makes any burning enemy near them glow, so the ones on fire can be seen.
	 */
	@Pair(a = "quench", b = "water_reading", name = "Stillwater", element = "frost", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Up to 6 allies are doused and get Fire Resistance for 8 seconds. A still ring of cold water is drawn round each one "
			+ "four times, two seconds apart: each time it douses them again, and any burning enemy within 3 blocks glows for 4 seconds.")
	public static void stillwater(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		for (LivingEntity t : allies) {
			c.douse(t);
			c.effect(t, MobEffects.FIRE_RESISTANCE, 8, 0);
		}
		c.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, c.point(), 0.8F, 1.2F);
		int frames = Math.max(1, (int) Math.round(4 * c.duration));
		c.every(40, frames, k -> {
			for (LivingEntity t : c.still(allies)) {
				c.douse(t);
				c.ring(PairCast.shift(0x9EE6FF, 0x3A6FD8, 0.8F), t.position().add(0, 0.1, 0), 1.2, 20, 0.3 * k);
				for (LivingEntity e : c.enemiesNear(t.position(), 3)) {
					if (e.isOnFire()) {
						c.effect(e, MobEffects.GLOWING, 4, 0);
					}
				}
			}
		});
	}

	/**
	 * Pilgrim Gale: the caster glides along the spell's line on a gust of the hymn, a few blocks at a time, and a pillar
	 * of light stands where they land. Anyone close to the start gets Speed.
	 */
	@Pair(a = "wayfarer_hymn", b = "waymark", name = "Pilgrim Gale", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"duration"},
		text = "You glide up to 12 blocks the way the spell flew, stopping short of walls. You and allies within 3 blocks of "
			+ "where you began get Speed I for 8 seconds, and a pillar of light marks where you land for 5 seconds.")
	public static void pilgrimGale(PairCast c) {
		LivingEntity me = c.caster;
		Vec3 from = me.position();
		for (LivingEntity t : c.alliesNear(from, 3)) {
			c.effect(t, MobEffects.SPEED, 8, 0);
		}
		Vec3 look = c.dir();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (forward.lengthSqr() < 1.0E-4) {
			forward = new Vec3(1, 0, 0);
		}
		forward = forward.normalize();
		Vec3 eye = from.add(0, 1, 0);
		Vec3 end = from.add(forward.scale(12));
		HitResult wall = c.level.clip(new ClipContext(eye, end.add(0, 1, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, me));
		if (wall.getType() != HitResult.Type.MISS) {
			Vec3 stop = wall.getLocation().subtract(forward.scale(0.8));
			end = new Vec3(stop.x, from.y, stop.z);
		}
		final Vec3 target = end;
		c.sound(SoundEvents.BEACON_ACTIVATE, from, 0.6F, 1.5F);
		c.every(2, 6, k -> {
			c.blink(me, from.lerp(target, (k + 1) / 6.0));
			c.particles(ParticleTypes.GUST, PairCast.mid(me), 2, 0.2, 0);
		});
		Vec3 landing = c.ground(target);
		c.later(14, () -> {
			c.sound(SoundEvents.BEACON_POWER_SELECT, landing, 0.8F, 1.3F);
			c.every(20, Math.max(1, (int) Math.round(6 * c.duration)), k ->
				c.column(PairCast.shift(0xFFF3B0, 0xFFFFFF, 1.0F), landing, 0.5, 6, 14));
		});
	}

	/**
	 * Bark Shed: bad effects peel off each ally like old bark, leaving Resistance I. Bark flakes fly off them three
	 * times, at once and then a second apart, while a shell of bark rings them.
	 */
	@Pair(a = "barkhide", b = "barkstrip", name = "Bark Shed", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Up to 6 allies shed every bad effect they carry (Poison, Wither, Slowness, Weakness, Mining Fatigue, Nausea, "
			+ "Blindness, Darkness) and get Resistance I for 12 seconds. Bark flakes fly off them at once and a second and two "
			+ "seconds later.")
	public static void barkShed(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		for (LivingEntity t : allies) {
			t.removeEffect(MobEffects.POISON);
			t.removeEffect(MobEffects.WITHER);
			t.removeEffect(MobEffects.SLOWNESS);
			t.removeEffect(MobEffects.WEAKNESS);
			t.removeEffect(MobEffects.MINING_FATIGUE);
			t.removeEffect(MobEffects.NAUSEA);
			t.removeEffect(MobEffects.BLINDNESS);
			t.removeEffect(MobEffects.DARKNESS);
			c.effect(t, MobEffects.RESISTANCE, 12, 0);
		}
		c.sound(SoundEvents.AXE_STRIP, c.point(), 1.0F, 0.8F);
		c.every(20, 3, k -> {
			for (LivingEntity t : c.still(allies)) {
				Vec3 at = PairCast.mid(t);
				c.sphere(PairCast.shift(0x6B4A2B, 0xC8A46A, 0.9F), at, 0.9, 16);
				for (int i = 0; i < 4; i++) {
					double a = Math.PI * 2 * (i + 0.5 * k) / 4;
					c.mote(PairCast.dust(0xA47B4A, 1.0F), at, new Vec3(Math.cos(a), 0.1, Math.sin(a)).scale(0.25));
				}
			}
			c.sound(SoundEvents.WOOD_BREAK, c.point(), 0.5F, 1.2F + 0.2F * k);
		});
	}

	/**
	 * Sweetthorn: each ally heals in a steady bloom, and when it ends berries burst out and sting every enemy near them.
	 * The bloom is pink and green; the berries burst as a sphere round each ally.
	 */
	@Pair(a = "berrybless", b = "lifebloom", name = "Sweetthorn", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 6 allies heal 3 times power at once, then 1 times power a second for 5 seconds. As the bloom ends, every "
			+ "enemy within 3 blocks of an ally takes 3 times power magic damage and Slowness I for 3 seconds.")
	public static void sweetthorn(PairCast c) {
		List<LivingEntity> allies = PairCast.first(c.allies(), 6);
		c.sound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, c.point(), 1.0F, 1.0F);
		c.every(20, 7, k -> {
			if (k == 0) {
				for (LivingEntity t : c.still(allies)) {
					c.heal(t, 3 * c.power);
					c.ring(PairCast.shift(0xFF6F91, 0xA7F070, 0.9F), t.position().add(0, 0.2, 0), 0.9, 14, 0);
				}
			} else if (k <= 5) {
				for (LivingEntity t : c.still(allies)) {
					c.heal(t, 1 * c.power);
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 4, 0.4, 0.05);
				}
			} else {
				Set<LivingEntity> stung = new LinkedHashSet<>();
				for (LivingEntity t : c.still(allies)) {
					Vec3 at = PairCast.mid(t);
					c.sphere(PairCast.dust(0xFF4D6D, 0.9F), at, 2.5, 24);
					stung.addAll(c.enemiesNear(at, 3));
				}
				for (LivingEntity e : c.still(List.copyOf(stung))) {
					c.hurt(e, 3 * c.power);
					c.effect(e, MobEffects.SLOWNESS, 3, 0);
				}
				c.sound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, c.point(), 0.9F, 0.7F);
			}
		});
	}

	/**
	 * Ridgeblast: a shock of basalt tosses the enemies hit into the air. A ridge of columns then rises on the line
	 * from the caster to the point, and the point blasts. Only enemies are hurt; no block changes.
	 */
	@Pair(a = "basalt_surge", b = "blastward", name = "Ridgeblast", element = "earth", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Each enemy hit takes 5 times power and is tossed up. A second later basalt columns rise on the line from you to "
			+ "the point, and enemies within 2 blocks of that line take 4 times power. At 1.5 seconds the point blasts: "
			+ "enemies within 3 blocks take 3 times power and are knocked away.")
	public static void ridgeblast(PairCast c) {
		Vec3 from = c.origin();
		Vec3 to = c.point();
		for (LivingEntity t : PairCast.first(c.enemies(), 8)) {
			c.strike(t, 5 * c.power);
			c.lift(t, 0.6);
		}
		c.sound(SoundEvents.ANVIL_LAND, c.point(), 0.5F, 0.5F);
		c.later(20, () -> {
			c.line(PairCast.dust(0x4A4A55, 1.2F), from, to, 3);
			for (int i = 1; i <= 5; i++) {
				Vec3 along = from.lerp(to, i / 5.0);
				c.column(PairCast.shift(0x3A3A44, 0x8A8A96, 1.0F), along, 0.7, 2.5, 8);
			}
			Vec3 mid = from.lerp(to, 0.5);
			double half = from.distanceTo(to) / 2 + 2;
			for (LivingEntity t : c.still(c.enemiesNear(mid, half))) {
				if (segmentDistance(PairCast.mid(t), from, to) <= 2) {
					c.strike(t, 4 * c.power);
				}
			}
			c.sound(SoundEvents.ANVIL_LAND, from, 0.7F, 0.6F);
		});
		c.later(30, () -> {
			c.particles(ParticleTypes.EXPLOSION, to, 1, 0, 0);
			c.wave(PairCast.dust(0xC9A36B, 1.0F), to.add(0, 0.2, 0), 24, 0.4);
			c.sound(SoundEvents.GENERIC_EXPLODE, to, 0.8F, 1.2F);
			for (LivingEntity t : c.still(c.enemiesNear(to, 3))) {
				c.strike(t, 3 * c.power);
				c.knockFrom(t, to, 0.6, 0.3);
			}
		});
	}

	private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double f = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
		return p.distanceTo(a.add(ab.scale(f)));
	}
}
