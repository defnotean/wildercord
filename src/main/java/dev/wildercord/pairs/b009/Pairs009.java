package dev.wildercord.pairs.b009;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.PairCast;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.pairs.Pair;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.EffectKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Batch 9: ten hand-made pairs. Each is its own mechanic, look and sound. */
public final class Pairs009 {
	private Pairs009() {}

	private static final Identifier GASH_ARMOUR = Wildercord.id("briar_gash_armour");

	/**
	 * Briar Gash: the wound is torn open and briars grow out of it. The look: red tears across each target, then green
	 * thorns rising round it, pricking the air around it once a second.
	 */
	@Pair(a = "bramble", b = "rend", name = "Briar Gash", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Up to 4 enemies are torn for 4 damage and wrapped in briars for 8 seconds: their armour drops by 4. "
			+ "Each second the briars prick: any enemy within 2 blocks of a torn one takes 2 damage and is shoved away.")
	public static void brambleRend(PairCast c) {
		List<LivingEntity> torn = PairCast.first(c.enemies(), 4);
		int lasts = c.ticks(8);
		for (LivingEntity t : torn) {
			c.hurt(t, 4 * c.power);
			AttributeInstance armour = t.getAttribute(Attributes.ARMOR);
			if (armour != null) {
				armour.addOrUpdateTransientModifier(new AttributeModifier(GASH_ARMOUR, -4.0, AttributeModifier.Operation.ADD_VALUE));
			}
			// Scheduled outside the cast so the armour is always given back, even if the caster dies first.
			Scheduler.later(lasts, () -> {
				AttributeInstance back = t.getAttribute(Attributes.ARMOR);
				if (back != null) {
					back.removeModifier(GASH_ARMOUR);
				}
			});
			c.zigzag(PairCast.dust(0xB71C1C, 1.0F), t.position().add(0, 0.2, 0), t.position().add(0, t.getBbHeight(), 0), 0.35, 3);
			c.spiral(PairCast.dust(0x2E7D32, 1.0F), t.position(), 0.7, 1.8, 2, 20);
		}
		c.sound(SoundEvents.SHEARS_SNIP, c.point(), 0.9F, 0.7F);
		c.every(20, 8, frame -> {
			for (LivingEntity t : c.still(torn)) {
				Vec3 at = PairCast.mid(t);
				c.ring(PairCast.shift(0x2E7D32, 0xB71C1C, 0.8F), at, 2.0, 24, frame * 0.35);
				for (LivingEntity near : c.enemiesNear(at, 2.0)) {
					if (near != t) {
						c.hurt(near, 2 * c.power);
						c.knockFrom(near, at, 1.5, 0.2);
					}
				}
			}
			c.sound(SoundEvents.GRASS_BREAK, c.point(), 0.6F, 0.6F + frame * 0.02F);
		});
	}

	/**
	 * Vinehaul: a vine shot to where the spell landed reels you there, and the lash comes back down on the spot.
	 * The look: a green vine drawn tight from your hand, a blink, then a snapping lash and falling spores.
	 */
	@Pair(a = "grapple", b = "vinelash", name = "Vinehaul", element = "life", kind = EffectKind.MOVEMENT,
		traits = {"power", "radius"},
		text = "A vine reels you to where the spell landed, and you stop there. Then it lashes each enemy within 2.5 blocks of "
			+ "that spot: 5 damage, yanked toward it and tripped (Slowness II for 2 seconds).")
	public static void grappleVinelash(PairCast c) {
		Vec3 spot = c.point();
		Vec3 hand = c.origin();
		c.zigzag(PairCast.dust(0x7CB342, 0.9F), hand, spot, 0.4, 3);
		c.sound(SoundEvents.GRASS_PLACE, hand, 0.8F, 0.9F);
		// The vine tightens a frame at a time while you're reeled in.
		c.every(3, 3, frame -> c.line(PairCast.shift(0x7CB342, 0x5D4037, 0.8F), hand, spot, 2));
		c.later(9, () -> {
			c.blink(c.caster, c.ground(spot));
			for (LivingEntity t : PairCast.first(c.enemiesNear(spot, 2.5 * c.radius), PairCast.MAX_TARGETS)) {
				c.hurt(t, 5 * c.power);
				c.effect(t, MobEffects.SLOWNESS, 2, 1);
				c.zigzag(PairCast.dust(0x9CCC65, 0.9F), spot, PairCast.mid(t), 0.3, 3);
				c.pullTo(t, spot, 3.0);
			}
			c.shake(spot, 0.2F, 6);
			c.sound(SoundEvents.CHAIN_HIT, spot, 1.0F, 0.8F);
		});
		c.later(14, () -> {
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, spot, 12, 0.8, 0.02);
			c.star(PairCast.dust(0xAED581, 0.8F), spot.add(0, 0.2, 0), 6, 2.0, 0.0);
		});
	}

	/**
	 * Hexbloom: a spore cloud that hexes. The look: a violet-to-green shell at the point, then a ring that shrinks
	 * each second while the monsters inside turn to face the caster.
	 */
	@Pair(a = "hex", b = "sporebloom", name = "Hexbloom", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power", "radius"},
		text = "Spores bloom 3 blocks round the point: each enemy in them takes 2 damage and Poison I for 5 seconds, and monsters "
			+ "fix on you. For 6 seconds the cloud re-hexes: each second, monsters still inside it fix on you again.")
	public static void hexSporebloom(PairCast c) {
		Vec3 at = c.point();
		double r = 3.0 * c.radius;
		for (LivingEntity t : c.enemiesNear(at, r)) {
			c.hurt(t, 2 * c.power);
			c.effect(t, MobEffects.POISON, 5, 0);
			fixOnCaster(c, t);
		}
		c.sphere(PairCast.dust(0x9CCC65, 1.2F), at, r, 40);
		c.sound(SoundEvents.ELDER_GUARDIAN_CURSE, at, 0.7F, 1.4F);
		c.every(20, 6, frame -> {
			if (frame > 0) {
				for (LivingEntity t : c.enemiesNear(at, r)) {
					fixOnCaster(c, t);
				}
			}
			c.ring(PairCast.shift(0x7E57C2, 0xAED581, 1.0F), at.add(0, 0.2, 0), r * (1 - frame * 0.1), 28, frame * 0.4);
			c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, at, 10, r * 0.6, 0.02);
		});
	}

	private static void fixOnCaster(PairCast c, LivingEntity t) {
		if (t instanceof Mob mob && c.movable(t)) {
			mob.setTarget(c.caster);
		}
	}

	/**
	 * Unmaking Mercy: every harmful effect and mark is undone, and the bad ones turn good on the way out. The look:
	 * a gold ring on each ally, dark motes flung off, then a green spiral of rising hearts.
	 */
	@Pair(a = "cleanse", b = "remedy", name = "Unmaking Mercy", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 8 allies lose every harmful effect and every elemental mark. Poison turns to Regeneration, slowness to Speed "
			+ "and weakness to Strength, each for half the time it had left (10 seconds at most); the rest is simply gone. Each heals 4 and gains Regeneration I for 6 seconds.")
	public static void cleanseRemedy(PairCast c) {
		List<LivingEntity> mended = PairCast.first(c.allies(), PairCast.MAX_TARGETS);
		for (LivingEntity t : mended) {
			for (MobEffectInstance inst : List.copyOf(t.getActiveEffects())) {
				MobEffect kind = inst.getEffect().value();
				if (kind.getCategory() != MobEffectCategory.HARMFUL) {
					continue;
				}
				// Half the time left, and never more than 10 seconds of the good side (an endless curse has no half).
				double half = inst.isInfiniteDuration() ? 10 : Math.min(10, inst.getDuration() / 40.0);
				int amp = inst.getAmplifier();
				t.removeEffect(inst.getEffect());
				if (kind == MobEffects.POISON.value()) {
					c.effect(t, MobEffects.REGENERATION, half, amp);
				} else if (kind == MobEffects.SLOWNESS.value()) {
					c.effect(t, MobEffects.SPEED, half, amp);
				} else if (kind == MobEffects.WEAKNESS.value()) {
					c.effect(t, MobEffects.STRENGTH, half, amp);
				}
			}
			for (Reactions.Mark mark : List.copyOf(Reactions.marks(t))) {
				Reactions.clear(t, mark);
			}
			c.heal(t, 4 * c.power);
			c.effect(t, MobEffects.REGENERATION, 6, 0);
			c.ring(PairCast.dust(0xFFF59D, 1.0F), PairCast.mid(t), 1.1, 18, 0);
		}
		c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c.point(), 0.9F, 1.3F);
		c.later(3, () -> {
			for (LivingEntity t : c.still(mended)) {
				c.wave(PairCast.shift(0x4A2C5A, 0x9E9E9E, 0.9F), PairCast.mid(t), 14, 0.15);
			}
			c.sound(SoundEvents.BOOK_PAGE_TURN, c.point(), 0.8F, 0.7F);
		});
		c.later(6, () -> {
			for (LivingEntity t : c.still(mended)) {
				c.spiral(PairCast.shift(0xFFF59D, 0x7CFC9A, 0.9F), t.position(), 0.8, 2.0, 2, 24);
				c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 6, 0.4, 0.0);
			}
			c.sound(SoundEvents.VILLAGER_YES, c.point(), 0.8F, 1.2F);
		});
	}

	/**
	 * Borrowed Spring: regrowth in three stages, and then the debt. The look: a gold spiral round each ally, a ring
	 * at each stage, and at the end a grey column sinking down onto them.
	 */
	@Pair(a = "borrowed_time", b = "regrowth", name = "Borrowed Spring", element = "time", kind = EffectKind.HELPFUL,
		traits = {},
		text = "Up to 4 allies get Regeneration I for 3 seconds, then II for 3 and III for 2, and then the debt comes due: "
			+ "Weakness I for 10 seconds.")
	public static void borrowedRegrowth(PairCast c) {
		for (LivingEntity t : PairCast.first(c.allies(), 4)) {
			int second = c.ticks(3);
			int third = c.ticks(6);
			int debt = c.ticks(8);
			c.effect(t, MobEffects.REGENERATION, 3, 0);
			c.spiral(PairCast.shift(0xFFE082, 0x8D6E63, 0.9F), t.position(), 0.6, 1.8, 1.5, 22);
			c.later(second, () -> {
				if (c.here(t)) {
					c.effect(t, MobEffects.REGENERATION, 3, 1);
					c.ring(PairCast.dust(0xFFE082, 1.0F), PairCast.mid(t), 0.9, 18, 0);
					c.sound(SoundEvents.BEACON_POWER_SELECT, PairCast.mid(t), 0.7F, 1.2F);
				}
			});
			c.later(third, () -> {
				if (c.here(t)) {
					c.effect(t, MobEffects.REGENERATION, 2, 2);
					c.ring(PairCast.dust(0xFFF8E1, 1.2F), PairCast.mid(t), 1.5, 26, 0.2);
					c.sound(SoundEvents.BEACON_POWER_SELECT, PairCast.mid(t), 0.8F, 1.5F);
				}
			});
			c.later(debt, () -> {
				if (c.here(t)) {
					c.effect(t, MobEffects.WEAKNESS, 10, 0);
					c.column(PairCast.dust(0x546E7A, 1.0F), t.position(), 0.8, 1.8, 26);
					c.sound(SoundEvents.ANVIL_LAND, PairCast.mid(t), 0.3F, 1.8F);
				}
			});
		}
	}

	/**
	 * Feasting Rot: venom that you feed on. The look: a green-brown cloud round each target, dripping while it rots,
	 * and a pale puff when one dies of it.
	 */
	@Pair(a = "slowburn", b = "venom", name = "Feasting Rot", element = "life", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Venom on the first 4 enemies: 2 damage now, then 0.75 a second for 4 seconds, with Poison I. While they rot you are fed "
			+ "1 hunger a second, and 2 more (with saturation) for each one that dies of it.")
	public static void slowburnVenom(PairCast c) {
		List<LivingEntity> rotting = PairCast.first(c.enemies(), 4);
		Set<LivingEntity> fed = new HashSet<>();
		for (LivingEntity t : rotting) {
			c.effect(t, MobEffects.POISON, 4, 0);
			c.sphere(PairCast.dust(0x8BC34A, 0.9F), PairCast.mid(t), 0.9, 20);
		}
		c.sound(SoundEvents.SLIME_SQUISH, c.point(), 0.8F, 0.6F);
		c.every(20, 5, frame -> {
			boolean anyRot = false;
			for (LivingEntity t : rotting) {
				if (!t.isAlive()) {
					if (fed.add(t) && c.caster instanceof Player eater) {
						eater.getFoodData().eat(2, 0.4F);
						c.sphere(PairCast.shift(0x8BC34A, 0xD7CCC8, 1.0F), PairCast.mid(t), 0.6, 16);
						c.particles(ParticleTypes.SPORE_BLOSSOM_AIR, PairCast.mid(t), 14, 0.5, 0.05);
						c.sound(SoundEvents.GENERIC_EAT, PairCast.mid(t), 0.9F, 1.1F);
					}
					continue;
				}
				if (!c.here(t)) {
					continue;
				}
				anyRot = true;
				c.hurt(t, (frame == 0 ? 2 : 0.75) * c.power);
				c.mote(PairCast.dust(0x8BC34A, 0.7F), PairCast.mid(t).add(0, 0.5, 0), new Vec3(0, -0.2, 0));
			}
			if (frame > 0 && anyRot && c.caster instanceof Player eater) {
				eater.getFoodData().eat(1, 0.0F);
			}
		});
	}

	/**
	 * Manna Table: a shared meal. The look: a golden arc from you to each ally, a blue-gold helix rising on them, and
	 * a burst of happy motes.
	 */
	@Pair(a = "managift", b = "nourish", name = "Manna Table", element = "life", kind = EffectKind.HELPFUL,
		traits = {"power"},
		text = "Up to 8 allies are fed: a player gets 6 hunger with saturation and loses Hunger, and each other player also gets three "
			+ "quarters of up to 12 of your mana (you lose that much each time). Other allies, pets too, are healed 4.")
	public static void managiftNourish(PairCast c) {
		Player giver = c.caster instanceof Player owner ? owner : null;
		for (LivingEntity t : PairCast.first(c.allies(), PairCast.MAX_TARGETS)) {
			c.arc(PairCast.dust(0xFFD54F, 1.0F), PairCast.mid(c.caster), PairCast.mid(t), 1.2, 14);
			if (t instanceof Player eater) {
				eater.getFoodData().eat(6, 0.6F);
				eater.removeEffect(MobEffects.HUNGER);
				if (giver != null && eater != giver) {
					float have = Spellbooks.mana(giver);
					float gift = Math.min(12.0F, have);
					if (gift > 0) {
						Spellbooks.setMana(giver, have - gift);
						Spellbooks.setMana(eater, Spellbooks.mana(eater) + gift * 0.75F);
					}
				}
			} else {
				c.heal(t, 4 * c.power);
			}
			c.later(4, () -> {
				if (c.here(t)) {
					c.helix(PairCast.dust(0x64B5F6, 0.9F), PairCast.dust(0xFFD54F, 0.9F), t.position(), 0.5, 1.8, 2, 24);
				}
			});
			c.later(8, () -> {
				if (c.here(t)) {
					c.particles(ParticleTypes.HAPPY_VILLAGER, PairCast.mid(t), 8, 0.4, 0.0);
				}
			});
		}
		c.sound(SoundEvents.GENERIC_EAT, c.point(), 0.8F, 1.3F);
		c.later(8, () -> c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, c.point(), 0.6F, 1.4F));
	}

	/**
	 * Scarlet Tithe: a bleeding wound whose blood is yours. The look: a dark red thread from the wound to you, drips
	 * falling from it, then at the end a pale thread of blood flowing to the caster.
	 */
	@Pair(a = "bleed", b = "clot", name = "Scarlet Tithe", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "Opens a wound on the first enemy: 2 damage, then 1 more every half second for 4 seconds (10 in all). The blood it draws "
			+ "becomes your absorption: 40% of what it drew, for 10 seconds.")
	public static void bleedClot(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		double[] drawn = {0};
		c.mark(t, Reactions.Mark.BLEEDING);
		c.arc(PairCast.dust(0x8E0000, 1.0F), PairCast.mid(t), PairCast.mid(c.caster), 1.0, 14);
		c.sound(SoundEvents.SLIME_BLOCK_HIT, PairCast.mid(t), 1.0F, 0.8F);
		c.every(10, 9, frame -> {
			if (!c.here(t)) {
				return;
			}
			double dealt = (frame == 0 ? 2 : 1) * c.power;
			c.hurt(t, dealt);
			drawn[0] += dealt;
			c.absorb(c.caster, drawn[0] * 0.4, 10);
			Vec3 at = PairCast.mid(t);
			if (frame == 0) {
				c.particles(ParticleTypes.DAMAGE_INDICATOR, at, 6, 0.2, 0.1);
			} else {
				c.mote(PairCast.dust(0x8E0000, 0.8F), at.add(0, 0.4, 0), new Vec3(0, -0.2, 0));
			}
			if (frame == 8) {
				c.line(PairCast.shift(0x8E0000, 0xFFCDD2, 0.8F), at, PairCast.mid(c.caster), 3);
				c.sound(SoundEvents.SPONGE_ABSORB, PairCast.mid(c.caster), 0.9F, 1.2F);
			}
		});
	}

	/**
	 * Red Siphon: a single hard hit that drinks back. The look: a crimson-to-gold line from you to the target, then a
	 * helix of red and gold round it for six seconds while its losses flow back to you.
	 */
	@Pair(a = "leech", b = "lifesteal", name = "Red Siphon", element = "blood", kind = EffectKind.HARMFUL,
		traits = {"power"},
		text = "5 damage to the first enemy, and you heal 5 for it; anything over your full health becomes absorption, up to 4. "
			+ "For 6 seconds, five times a second, a quarter of what it loses (from anyone) heals you.")
	public static void leechLifesteal(PairCast c) {
		LivingEntity t = c.firstEnemy();
		if (t == null) {
			return;
		}
		double dealt = 5 * c.power;
		c.line(PairCast.shift(0xD50000, 0xFFD180, 0.9F), PairCast.mid(c.caster), PairCast.mid(t), 4);
		c.sound(SoundEvents.PLAYER_ATTACK_CRIT, PairCast.mid(t), 1.0F, 0.8F);
		c.hurt(t, dealt);
		double missing = Math.max(0, c.caster.getMaxHealth() - c.caster.getHealth());
		double healed = Math.min(missing, dealt);
		c.heal(c.caster, healed);
		double overflow = dealt - healed;
		if (overflow > 0) {
			c.absorb(c.caster, Math.min(4.0, overflow), 10);
		}
		double[] last = {t.getHealth()};
		c.every(4, 31, frame -> {
			if (!c.here(t)) {
				return;
			}
			double hp = t.getHealth();
			if (hp < last[0]) {
				c.heal(c.caster, (last[0] - hp) * 0.25);
			}
			last[0] = hp;
			c.helix(PairCast.dust(0xD50000, 0.8F), PairCast.dust(0xFFD180, 0.8F), t.position(), 0.5, 1.6, 1.0, 14);
			if (frame % 5 == 0) {
				c.sound(SoundEvents.SPONGE_ABSORB, PairCast.mid(c.caster), 0.25F, 1.0F + frame * 0.01F);
			}
		});
	}

	/**
	 * Redline: an engine run up through its gears until the needle sits in the red, then it coughs out. The look: a
	 * tachometer ring on you whose needle sweeps further each second, sparks that turn amber then red as the gears
	 * climb, a heartbeat that quickens, and a gout of black smoke when it stalls.
	 */
	@Pair(a = "haste", b = "overdrive", name = "Redline", element = "blood", kind = EffectKind.HELPFUL,
		traits = {"duration"},
		text = "Three gears of 4 seconds. First: Speed I, Haste I, lose 1 health. Second: Speed II, Haste II, Strength I, lose 2. "
			+ "Third: Speed II, Haste III, Strength II, lose 4. Never below 2 health. Then it stalls: Slowness I for 3 seconds.")
	public static void hasteOverdrive(PairCast c) {
		LivingEntity me = c.caster;
		// Each gear lasts four seconds (times duration); the drain is spread over the gear one beat at a time.
		int gear = Math.max(1, (int) Math.round(4 * c.duration));
		float[] drain = {1.0F, 2.0F, 4.0F};
		int[] colour = {0xFFD54F, 0xFF8F00, 0xD50000};
		c.wave(PairCast.dust(0xFFD54F, 1.1F), me.position().add(0, 0.1, 0), 24, 0.25);
		c.sound(SoundEvents.PISTON_EXTEND, me.position(), 0.8F, 0.6F);
		c.every(20, gear * 3, beat -> {
			if (!me.isAlive()) {
				return;
			}
			int g = beat / gear;
			int into = beat % gear;
			if (into == 0) {
				// A gear change: the effects step up and the engine roars.
				c.effect(me, MobEffects.SPEED, gear + 1, g == 0 ? 0 : 1);
				c.effect(me, MobEffects.HASTE, gear + 1, g);
				if (g > 0) {
					c.effect(me, MobEffects.STRENGTH, gear + 1, g - 1);
				}
				c.sound(SoundEvents.RAVAGER_ROAR, me.position(), 0.4F, 1.2F + g * 0.3F);
				c.wave(PairCast.dust(colour[g], 1.2F), me.position().add(0, 0.1, 0), 28, 0.3 + g * 0.1);
				c.punch(0.1F + g * 0.08F);
			}
			float bleed = drain[g] / gear;
			if (me.getHealth() > 2.0F) {
				me.setHealth(Math.max(2.0F, me.getHealth() - bleed));
			}
			// The needle sweeps round from the left a little further each second, into the red at the end.
			Vec3 hub = me.position().add(0, 0.15, 0);
			c.ring(PairCast.dust(0x3A3A3A, 0.7F), hub, 1.2, 24, 0);
			double sweep = Math.PI * (beat + 1) / (gear * 3.0);
			Vec3 tip = hub.add(-Math.cos(sweep) * 1.2, 0, Math.sin(sweep) * 1.2);
			c.line(PairCast.dust(colour[g], 1.0F), hub, tip, 6);
			c.particles(ParticleTypes.ELECTRIC_SPARK, PairCast.mid(me), 3 + g * 4, 0.4, 0.08);
			c.sound(SoundEvents.WARDEN_HEARTBEAT, me.position(), 0.7F, 1.0F + g * 0.35F);
		});
		// The stall.
		c.later(gear * 60, () -> {
			if (!me.isAlive()) {
				return;
			}
			c.effect(me, MobEffects.SLOWNESS, 3, 0);
			c.particles(ParticleTypes.LARGE_SMOKE, PairCast.mid(me), 24, 0.5, 0.04);
			c.sound(SoundEvents.FIRE_EXTINGUISH, me.position(), 0.9F, 0.5F);
			c.sound(SoundEvents.PISTON_CONTRACT, me.position(), 0.8F, 0.5F);
		});
	}
}
