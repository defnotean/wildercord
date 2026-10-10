package dev.wildercord.pairs.b020;

import dev.wildercord.cast.PairCast;
import dev.wildercord.pairs.Pair;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ten hand-made pairs: each has its own mechanic, look, sounds and rule text. */
public final class Pairs020 {
	private Pairs020() {}

	/**
	 * Haymow Vault: a toss into the air. You are flung up from the spot where you stand (nothing happens under a low
	 * ceiling), and at the top the allies round you are lifted and eased down too.
	 */
	@Pair(a = "hayloft", b = "skylatch", name = "Haymow Vault", element = "wind", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "Tosses you about 4 blocks up, unless a ceiling closer than 4 blocks stops it. At the top, each ally within "
			+ "3 blocks rises about a block and drifts slowly for 2 seconds. You float down for 3 seconds with no fall damage.")
	public static void haymowVault(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 feet = self.position();
		boolean roofed = c.level.clip(new ClipContext(feet, feet.add(0, 4.0, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, self)).getType() != HitResult.Type.MISS;
		if (roofed) {
			c.sound(SoundEvents.WOOL_STEP, feet, 0.6F, 0.7F);
			c.line(PairCast.dust(0xC9B27A, 0.8F), feet, feet.add(0, 3, 0), 2);
			return;
		}
		c.effect(self, MobEffects.SLOW_FALLING, 3, 0);
		// Wind-up: straw swirls up round your feet for a few ticks.
		c.every(2, 3, frame -> c.spiral(PairCast.dust(0xF2D16B, 0.9F), feet.add(0, 0.2, 0), 0.9 - frame * 0.2, 1.6, 1.0, 16));
		c.later(6, () -> {
			c.push(self, new Vec3(0, 0.85, 0));
			self.resetFallDistance();
			c.wave(PairCast.dust(0xF2D16B, 1.0F), feet.add(0, 0.1, 0), 14, 0.25);
			c.sound(SoundEvents.BREEZE_JUMP, feet, 0.9F, 1.1F);
			c.punch(0.1F);
		});
		// The apex, about ten ticks after the toss.
		c.later(16, () -> {
			Vec3 top = self.position();
			for (LivingEntity a : c.alliesNear(top, 3 * c.radius)) {
				if (a == self) {
					continue;
				}
				c.lift(a, 0.45);
				c.effect(a, MobEffects.SLOW_FALLING, 2, 0);
			}
			c.ring(PairCast.shift(0xFFF3C4, 0xF2D16B, 1.0F), top.add(0, 0.2, 0), 2.5 * c.radius, 24, 0);
			c.sound(SoundEvents.WIND_CHARGE_BURST, top, 0.7F, 1.4F);
		});
	}

	/**
	 * Gallop Lane: a lane of wind runs ahead of you, and everything it passes is quickened. The lane travels over a
	 * second and a half, so you see it sweep past each ally in turn; a horse or a tamed animal gets the long, strong boost.
	 */
	@Pair(a = "fieldstride", b = "steedsong", name = "Gallop Lane", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "duration", "radius"},
		text = "A lane of wind runs 10 blocks ahead of you in 1.5 seconds. You and each ally it passes get Speed I and Jump Boost I "
			+ "for 20 seconds; a horse or tamed animal it passes gets Speed II and Jump Boost II for 3 minutes.")
	public static void gallopLane(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 start = self.position();
		Vec3 ahead = flatDir(c);
		Set<LivingEntity> passed = new HashSet<>();
		c.sound(SoundEvents.HORSE_GALLOP, start, 0.8F, 1.2F);
		c.ring(PairCast.dust(0x9FE7A8, 1.0F), start.add(0, 0.1, 0), 1.0, 12, 0);
		c.every(4, 8, frame -> {
			Vec3 head = start.add(ahead.scale(1.25 * (frame + 1)));
			c.particles(PairCast.dust(0xE6FFF0, 0.9F), head.add(0, 0.8, 0), 3, 0.3, 0.02);
			c.mote(ParticleTypes.CLOUD, head, ahead.scale(0.2));
			for (LivingEntity a : c.alliesNear(head, 1.5 * c.radius)) {
				if (!passed.add(a)) {
					continue;
				}
				boolean steed = a instanceof AbstractHorse || a instanceof TamableAnimal;
				c.effect(a, MobEffects.SPEED, steed ? 180 : 20, steed ? 1 : 0);
				c.effect(a, MobEffects.JUMP_BOOST, steed ? 180 : 20, steed ? 1 : 0);
				c.sound(SoundEvents.NOTE_BLOCK_FLUTE, PairCast.mid(a), 0.5F, 1.6F);
			}
		});
	}

	/**
	 * Hoofmend Dash: on foot, a short dash that stops at the first wall and heals whoever stands at the landing; riding,
	 * the mount gets the speed and the mending instead. The healing green is the colour of the grass you land on.
	 */
	@Pair(a = "steedmend", b = "trot", name = "Hoofmend Dash", element = "life", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "On foot: you dash up to 7 blocks forward, stopping short of any wall, and on landing you and allies within "
			+ "2.5 blocks heal 4 health each. Riding: your mount gets Speed II and Regeneration II for 3 seconds instead; you stay put.")
	public static void hoofmendDash(PairCast c) {
		LivingEntity self = c.caster;
		if (self.getVehicle() instanceof LivingEntity steed) {
			c.effect(steed, MobEffects.SPEED, 3, 1);
			c.effect(steed, MobEffects.REGENERATION, 3, 1);
			c.sound(SoundEvents.HORSE_GALLOP, PairCast.mid(steed), 0.9F, 1.2F);
			c.every(3, 4, frame -> c.ring(PairCast.dust(0x8DE39A, 1.0F), PairCast.mid(steed).add(0, -0.4, 0),
				1.3 + frame * 0.1, 18, frame * 0.3));
			return;
		}
		Vec3 from = self.position();
		Vec3 flat = flatDir(c);
		Vec3 start = from.add(0, 0.5, 0);
		Vec3 want = start.add(flat.scale(7));
		BlockHitResult wall = c.level.clip(new ClipContext(start, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? want : wall.getLocation().subtract(flat.scale(0.6));
		if (end.distanceTo(start) < 1.5) {
			c.sound(SoundEvents.WOOL_STEP, from, 0.6F, 0.8F);
			return;
		}
		Vec3 landing = c.ground(end);
		c.sound(SoundEvents.BREEZE_CHARGE, from, 0.7F, 1.2F);
		c.every(2, 3, frame -> c.ring(PairCast.dust(0x8DE39A, 1.0F), from.add(0, 0.1, 0), 1.0 - frame * 0.25, 14, frame * 0.4));
		c.later(6, () -> {
			if (!c.blink(self, landing)) {
				return;
			}
			c.line(PairCast.shift(0xB8FFD0, 0x8DE39A, 0.9F), from.add(0, 0.2, 0), landing.add(0, 0.2, 0), 2);
			c.wave(PairCast.dust(0x8DE39A, 1.0F), landing.add(0, 0.1, 0), 16, 0.3);
			c.sound(SoundEvents.BREEZE_SLIDE, landing, 0.8F, 1.3F);
			for (LivingEntity a : PairCast.first(c.alliesNear(landing, 2.5 * c.radius), PairCast.MAX_TARGETS)) {
				c.heal(a, 4 * c.power);
			}
			c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, landing, 0.8F, 1.2F);
		});
	}

	/**
	 * Salt Gale: a sea gust rolls outward from you in four rings. As it reaches the allies in it, the sea air takes away
	 * the sickness and hunger, and lifts the harmful effect that has the longest left.
	 */
	@Pair(a = "sea_breeze", b = "shrug_off", name = "Salt Gale", element = "wind", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "A sea gust rolls out 4 blocks from you. Each ally in it loses Mining Fatigue, Nausea and Hunger, and the "
			+ "harmful effect with the most time left is lifted from it.")
	public static void saltGale(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = self.position().add(0, 1, 0);
		double reach = 4 * c.radius;
		List<LivingEntity> swept = c.alliesNear(at, reach);
		c.sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, at, 0.8F, 1.5F);
		c.every(3, 4, frame -> {
			c.ring(PairCast.shift(0xD8F6FF, 0x6FD0E0, 0.8F), at, reach * (frame + 1) / 4.0, 28, frame * 0.5);
			if (frame == 2) {
				for (LivingEntity a : c.still(swept)) {
					cleanse(c, a);
				}
			}
		});
	}

	private static void cleanse(PairCast c, LivingEntity a) {
		a.removeEffect(MobEffects.MINING_FATIGUE);
		a.removeEffect(MobEffects.NAUSEA);
		a.removeEffect(MobEffects.HUNGER);
		MobEffectInstance worst = null;
		for (MobEffectInstance inst : a.getActiveEffects()) {
			if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
				&& (worst == null || inst.getDuration() > worst.getDuration())) {
				worst = inst;
			}
		}
		if (worst != null) {
			a.removeEffect(worst.getEffect());
		}
		c.particles(ParticleTypes.CLOUD, PairCast.mid(a), 8, 0.4, 0.02);
		c.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, PairCast.mid(a), 0.5F, 1.3F);
	}

	/**
	 * Whistle Pack: a whistle that pulls your pets back to you over a second and a half, glowing gold as they run in.
	 * Each one that arrives is armoured. With no pet near, the whistle guards your allies instead.
	 */
	@Pair(a = "faithful", b = "heel", name = "Whistle Pack", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"power", "radius"},
		text = "Pulls each of your pets within 32 blocks that isn't sitting towards you for 1.5 seconds. Each one that ends up "
			+ "within 4 blocks gets Resistance I for 20 seconds. With no pet in range, you and allies within 4 blocks get Resistance I for 10 seconds.")
	public static void whistlePack(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = self.position();
		c.sound(SoundEvents.NOTE_BLOCK_FLUTE, at.add(0, 1.5, 0), 0.9F, 1.6F);
		c.wave(PairCast.dust(0xFFE08A, 0.9F), at.add(0, 1, 0), 20, 0.4);
		List<LivingEntity> pack = new ArrayList<LivingEntity>(c.level.getEntitiesOfClass(TamableAnimal.class,
			self.getBoundingBox().inflate(32),
			p -> p.isAlive() && p.isOwnedBy(self) && !p.isOrderedToSit() && p.distanceToSqr(self) <= 32 * 32));
		if (pack.isEmpty()) {
			for (LivingEntity a : c.alliesNear(at, 4 * c.radius)) {
				c.effect(a, MobEffects.RESISTANCE, 10, 0);
			}
			c.every(4, 3, frame -> c.ring(PairCast.dust(0xFFE08A, 0.8F), at.add(0, 1, 0), 1.5 * c.radius + frame, 20, frame * 0.3));
			return;
		}
		List<LivingEntity> callers = PairCast.first(pack, PairCast.MAX_TARGETS);
		c.every(6, 5, frame -> {
			for (LivingEntity p : c.still(callers)) {
				Vec3 body = PairCast.mid(p);
				if (body.distanceTo(at) > 1.5) {
					c.pullTo(p, at, 1.2);
				}
				c.particles(PairCast.dust(0xFFE08A, 0.7F), body.add(0, 0.4, 0), 3, 0.2, 0);
			}
		});
		c.later(30, () -> {
			for (LivingEntity p : c.still(callers)) {
				if (p.distanceTo(self) <= 4) {
					c.effect(p, MobEffects.RESISTANCE, 20, 0);
					c.sound(SoundEvents.ARMOR_EQUIP_LEATHER, PairCast.mid(p), 0.6F, 1.1F);
				}
			}
		});
	}

	/**
	 * Pardoning Ring: a ring of white light falls from six blocks up to the ground. Where it lands, the animals in it
	 * are shielded and the allies in it get a brief stoneskin of their own.
	 */
	@Pair(a = "beastguard", b = "grace", name = "Pardoning Ring", element = "arcane", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "A ring of white light falls 6 blocks to the ground in about a second. Up to 8 animals within 6 blocks "
			+ "(pets, mounts, farm animals) get Resistance II and Fire Resistance for 60 seconds and 3 absorption hearts for 10 "
			+ "seconds. Allies within 6 blocks who are not animals (you included) get Resistance III for 3 seconds.")
	public static void pardoningRing(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 at = self.position();
		double reach = 6 * c.radius;
		c.sound(SoundEvents.BEACON_POWER_SELECT, at, 0.5F, 1.8F);
		c.every(3, 7, frame -> {
			c.ring(PairCast.dust(0xFFF7D6, 1.0F), at.add(0, 6 - frame, 0), reach, 26, frame * 0.25);
			if (frame == 6) {
				pardon(c, at, reach);
			}
		});
	}

	private static void pardon(PairCast c, Vec3 at, double reach) {
		AABB box = new AABB(at, at).inflate(reach);
		List<LivingEntity> beasts = new ArrayList<LivingEntity>(c.level.getEntitiesOfClass(Animal.class, box,
			a -> a.isAlive() && !(a instanceof Enemy) && a.distanceToSqr(at) <= reach * reach));
		for (LivingEntity beast : PairCast.first(beasts, PairCast.MAX_TARGETS)) {
			c.effect(beast, MobEffects.RESISTANCE, 60, 1);
			c.effect(beast, MobEffects.FIRE_RESISTANCE, 60, 0);
			c.absorb(beast, 6 * c.power, 10);
			c.column(PairCast.dust(0xFFF7D6, 0.9F), beast.position(), 0.5, 2.0, 10);
		}
		for (LivingEntity a : c.alliesNear(at, reach)) {
			if (!(a instanceof Animal)) {
				c.effect(a, MobEffects.RESISTANCE, 3, 2);
			}
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, at, 0.8F, 1.4F);
		c.shake(at, 0.15F, 6);
	}

	/**
	 * Stoneshell Vow: three plates of stone orbit you for two seconds and settle on the ground. You are armoured for
	 * 15 seconds (30 in rain or water), and the allies beside you for ten.
	 */
	@Pair(a = "shellback", b = "stoutheart", name = "Stoneshell Vow", element = "earth", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "Three stone plates orbit you for 2 seconds, then settle. You get Resistance I for 15 seconds (30 in rain or water), "
			+ "and each ally within 3 blocks gets Resistance I for 10 seconds.")
	public static void stoneshellVow(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 feet = self.position();
		boolean wet = self.isInWaterOrRain();
		c.sound(SoundEvents.STONE_PLACE, feet, 0.9F, 0.7F);
		for (LivingEntity a : c.alliesNear(feet, 3 * c.radius)) {
			if (a != self) {
				c.effect(a, MobEffects.RESISTANCE, 10, 0);
			}
		}
		c.effect(self, MobEffects.RESISTANCE, wet ? 30 : 15, 0);
		c.every(4, 10, frame -> {
			for (int k = 0; k < 3; k++) {
				double angle = frame * 0.45 + k * Math.PI * 2 / 3;
				Vec3 plate = feet.add(Math.cos(angle) * 1.1, 1.0, Math.sin(angle) * 1.1);
				c.particles(PairCast.dust(0xB9A27A, 1.2F), plate, 4, 0.12, 0);
			}
			if (frame == 9) {
				c.disc(PairCast.dust(0x8C7A55, 1.0F), feet.add(0, 0.1, 0), 1.2, 20);
				c.sound(SoundEvents.ANVIL_LAND, feet, 0.4F, 1.6F);
			}
		});
	}

	/**
	 * Hollow Snare: a void pocket opens where the spell lands. It strikes the first enemy it touches, then draws the
	 * enemies near it in for a second, and shuts with a blast of its own.
	 */
	@Pair(a = "nullcatch", b = "quietus", name = "Hollow Snare", element = "void", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "A void pocket opens where it lands. The first enemy it touches takes 4 magic damage after 0.3 seconds, and Slowness I for "
			+ "3 seconds. For a second, enemies within 3 blocks are drawn into it; when it shuts, each enemy within 1.5 blocks takes 3 more.")
	public static void hollowSnare(PairCast c) {
		Vec3 at = c.point();
		LivingEntity first = c.firstEnemy();
		c.sound(SoundEvents.SCULK_CLICKING, at, 0.9F, 0.6F);
		// Two frames: the void pocket shrinks into the point it landed on.
		c.every(3, 2, frame -> c.sphere(PairCast.shift(0x6B3FA0, 0x0B0412, 1.0F), at, 1.6 - frame * 0.6, 26));
		c.later(6, () -> {
			if (first != null && c.here(first)) {
				c.hurt(first, 4 * c.power);
				c.effect(first, MobEffects.SLOWNESS, 3, 0);
				c.zigzag(PairCast.dust(0x9B6BFF, 0.9F), at, PairCast.mid(first), 0.3, 3);
				c.sound(SoundEvents.PORTAL_TRIGGER, PairCast.mid(first), 0.5F, 1.4F);
			}
			c.every(4, 6, frame -> {
				for (LivingEntity e : c.still(c.enemiesNear(at, 3 * c.radius))) {
					c.pullTo(e, at, 0.9 * c.power);
					c.particles(PairCast.dust(0x6B3FA0, 0.8F), PairCast.mid(e), 2, 0.2, 0);
				}
				c.ring(PairCast.dust(0x6B3FA0, 1.0F), at.add(0, 0.2, 0), 3 * c.radius * (1 - frame / 6.0), 20, frame * 0.3);
			});
		});
		c.later(30, () -> {
			for (LivingEntity e : c.still(c.enemiesNear(at, 1.5 * c.radius))) {
				c.hurt(e, 3 * c.power);
			}
			c.sphere(PairCast.shift(0xB9A0FF, 0x0B0412, 1.2F), at, 1.6, 30);
			c.wave(PairCast.dust(0x9B6BFF, 1.0F), at.add(0, 0.2, 0), 18, 0.4);
			c.sound(SoundEvents.GENERIC_EXPLODE, at, 0.4F, 1.6F);
		});
	}

	/**
	 * Lantern Recall: you light a lantern on the ground you stand on, and four seconds later a line of lantern light
	 * draws you back to it, if it's near and no wall is between.
	 */
	@Pair(a = "last_lantern", b = "wayline", name = "Lantern Recall", element = "arcane", kind = EffectKind.MOVEMENT,
		traits = {"duration", "radius"},
		text = "Marks the ground under you. Four seconds later, if you are within 8 blocks of it with no wall between, you are "
			+ "drawn back onto it along a lantern line. If that spot is no longer safe, nothing happens.")
	public static void lanternRecall(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 mark = c.ground(self.position());
		int wait = c.ticks(4);
		c.sound(SoundEvents.LANTERN_PLACE, mark, 0.9F, 1.0F);
		c.column(PairCast.shift(0xFFE7A8, 0xFFB347, 0.8F), mark, 0.7, 2.0, 14);
		// Four beats while it waits: the line runs from the lantern to you, drawing taut as you wander.
		c.every(Math.max(1, wait / 4), 4, frame -> {
			if (self.isAlive()) {
				c.line(PairCast.dust(0xFFD27A, 0.6F), mark.add(0, 1, 0), PairCast.mid(self), 2);
			}
		});
		c.later(wait, () -> {
			Vec3 now = self.position();
			if (now.distanceTo(mark) > 8 * c.radius) {
				return;
			}
			Vec3 from = now.add(0, 0.5, 0);
			Vec3 to = mark.add(0, 0.5, 0);
			if (c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self)).getType() != HitResult.Type.MISS) {
				return;
			}
			if (!c.blink(self, mark)) {
				return;
			}
			c.line(PairCast.shift(0xFFE7A8, 0xFFB347, 0.8F), from, to, 2);
			c.wave(PairCast.dust(0xFFE7A8, 0.9F), mark.add(0, 0.1, 0), 14, 0.3);
			c.sound(SoundEvents.PORTAL_TRIGGER, mark, 0.5F, 1.6F);
		});
	}

	/**
	 * Long Supper: a hearth-glow spreads from where you cast it. Every two seconds it warms each ally within four
	 * blocks of that spot, healing them and leaving them with a brief regeneration, for as long as the supper lasts.
	 */
	@Pair(a = "feastday", b = "savor", name = "Long Supper", element = "fire", kind = EffectKind.HELPFUL,
		traits = {"duration", "radius"},
		text = "A glow of hearth-light reaches 4 blocks out from where you cast it, in 6 beats two seconds apart. Each beat "
			+ "heals each ally inside it 2 health and gives them Regeneration I for 4 seconds.")
	public static void longSupper(PairCast c) {
		LivingEntity self = c.caster;
		Vec3 hearth = self.position();
		double reach = 4 * c.radius;
		int beats = Math.max(1, (int) Math.round(6 * c.duration));
		c.sound(SoundEvents.CAMPFIRE_CRACKLE, hearth, 0.9F, 0.9F);
		// Wind-up: a column of smoke curls up from the spot.
		c.every(4, 3, frame -> c.spiral(PairCast.dust(0xFFB347, 0.9F), hearth.add(0, 0.1, 0), 1.2 - frame * 0.3, 1.2, 1.5, 14));
		c.later(12, () -> c.every(40, beats, frame -> {
			for (LivingEntity a : c.alliesNear(hearth, reach)) {
				c.heal(a, 2 * c.power);
				c.effect(a, MobEffects.REGENERATION, 4, 0);
			}
			c.ring(PairCast.shift(0xFFB347, 0xFF5A1F, 1.0F), hearth.add(0, 0.2, 0), reach, 30, frame * 0.4);
			c.particles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hearth, 6, 0.5, 0.01);
			c.sound(SoundEvents.FIRECHARGE_USE, hearth, 0.5F, 1.4F);
		}));
	}

	/** The way you are facing, flattened onto the ground (east if you look straight up or down). */
	private static Vec3 flatDir(PairCast c) {
		Vec3 d = c.dir();
		Vec3 flat = new Vec3(d.x, 0, d.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}
}
