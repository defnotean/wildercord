package dev.wildercord.cast;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What each effect rune does to a hit. Numbers match the rune descriptions in {@link Runes}
 * and docs/DESIGN.md; Amplify multiplies power by 1.5 and Extend doubles durations.
 */
public final class Effects {
	private Effects() {}

	/** Damage to players is scaled down so PvP stays fair. */
	public static final float PVP_DAMAGE = 0.6F;
	private static final int MAX_STRIKES_PER_HIT = 8;

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit) {
		apply(cast, node, hit, 1.0);
	}

	/** Execute on the effect being applied: extra power against targets under half health (1 = none). */
	private static double executeBonus = 1.0;

	/** @param groupPower extra power from the shape (Focus on a shape) */
	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		executeBonus = SpellNumbers.executeBonus(node);
		try {
			applyEffect(cast, node, hit, groupPower);
		} finally {
			executeBonus = 1.0;
		}
	}

	private static void applyEffect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		RuneDef rune = node.effect;
		ServerPlayer caster = cast.caster;
		ServerLevel level = cast.level;
		double power = SpellNumbers.power(node) * groupPower * cast.power;
		double duration = SpellNumbers.duration(node) * cast.duration;
		int amplify = node.count(Runes.AMPLIFY);
		List<LivingEntity> helped = filter(hit.entities(), e -> Targets.canHelp(caster, e));
		List<LivingEntity> harmed = filter(hit.entities(), e -> Targets.canHarm(caster, e));
		// Self always means you: movement effects move you even though they are "harmful" to others.
		List<LivingEntity> moved = hit.self() ? List.of(caster) : harmed;

		switch (rune.path()) {
			case "feather_fall" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks(12, duration), 0, false, true));
				t.resetFallDistance();
				Vfx.featherFall(level, t);
			});
			case "swift" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks(10, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.swift(level, t);
			});
			case "night_eye" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks(60, duration), 0, false, true));
				Vfx.nightEye(level, t);
			});
			case "heal" -> helped.forEach(t -> {
				t.heal((float) (8 * power));
				Vfx.heal(level, t);
			});
			case "shield" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks(12, duration), Math.min(5, 2 + amplify), false, true));
				Vfx.shield(level, t);
			});
			case "harm" -> harmed.forEach(t -> {
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 7 * power);
				Vfx.harm(level, t);
			});
			case "push" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				push(t, away.scale(2.2 * power).add(0, 0.45, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				Vfx.push(level, t, away);
			});
			case "pull" -> harmed.forEach(t -> {
				Vec3 towards = hit.origin().subtract(t.position());
				double distance = towards.length();
				Vfx.pull(level, t, hit.origin());
				Reactions.mark(t, Reactions.Mark.PULLED);
				push(t, towards.normalize().scale(Math.min(2.6, 0.5 + distance * 0.22) * power).add(0, 0.3, 0));
			});
			case "launch" -> moved.forEach(t -> {
				Vec3 v = t.getDeltaMovement();
				Vec3 kick = new Vec3(0, 1.5 * power, 0);
				if (t == caster) {
					kick = kick.add(horizontal(caster.getLookAngle(), caster.getLookAngle()).scale(0.7));
					caster.resetFallDistance();
				}
				push(t, new Vec3(v.x, Math.max(0, v.y), v.z).add(kick).subtract(v));
				if (t != caster) {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				}
				Vfx.launch(level, t);
			});
			case "dash" -> moved.forEach(t -> {
				Vec3 look = caster.getLookAngle();
				Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.45, look.y)) + 0.12, look.z).normalize();
				push(t, dir.scale(2.6 * power));
				if (t == caster) {
					caster.resetFallDistance();
				} else {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				}
				Vfx.dash(level, t, dir);
			});
			case "fire" -> harmed.forEach(t -> {
				double react = Reactions.fire(cast, t);
				t.igniteForSeconds((float) (6 * duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 5 * power * react);
				Vfx.fire(level, t);
			});
			case "frost" -> harmed.forEach(t -> {
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 5 * power);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(4, duration), 2, false, true));
				t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + ticks(4, duration)));
				Reactions.mark(t, Reactions.Mark.FROZEN);
				Vfx.frost(level, t);
			});
			case "lightning" -> {
				List<Vec3> strikes = new ArrayList<>();
				harmed.forEach(t -> strikes.add(t.position()));
				if (strikes.isEmpty()) {
					strikes.add(hit.point());
				}
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, strikes.size()); i++) {
					lightning(cast, strikes.get(i), power);
				}
			}
			case "explode" -> {
				double radius = SpellNumbers.explodeRadius(node);
				List<Vec3> blasts = new ArrayList<>();
				harmed.forEach(t -> blasts.add(t.getBoundingBox().getCenter()));
				if (blasts.isEmpty()) {
					blasts.add(hit.point());
				}
				for (int i = 0; i < Math.min(4, blasts.size()); i++) {
					explode(cast, blasts.get(i), radius, power);
				}
			}
			case "sonic_boom" -> harmed.forEach(t -> {
				Vfx.sonicBoom(level, hit.origin(), t.getBoundingBox().getCenter());
				hurt(cast, t, level.damageSources().sonicBoom(caster), 16 * power);
			});
			case "wither" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks(8, duration), 2, false, true), caster);
				Vfx.wither(level, t);
			});
			case "dragon_breath" -> dragonBreath(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, duration);
			case "shock" -> harmed.forEach(t -> shock(cast, t, power));
			case "haste" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks(30, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.haste(level, t);
			});
			case "reveal" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks(15, duration), 0, false, false));
				Vfx.reveal(level, t);
			});
			case "regrowth" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks(8, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.regrowth(level, t);
			});
			case "cleanse" -> helped.forEach(t -> {
				List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
				for (MobEffectInstance effect : t.getActiveEffects()) {
					if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
						bad.add(effect.getEffect());
					}
				}
				bad.forEach(t::removeEffect);
				t.clearFire();
				t.setTicksFrozen(0);
				Reactions.clear(t, Reactions.Mark.FROZEN);
				Vfx.cleanse(level, t);
			});
			case "stoneskin" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks(10, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.stoneskin(level, t);
			});
			case "root" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(3, duration), 6, false, false));
				t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
				Vfx.root(level, t);
			});
			case "veil" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks(12, duration), 0, false, true));
				for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(16.0), e -> e instanceof net.minecraft.world.entity.Mob)) {
					net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) e;
					if (mob.getTarget() == t) {
						mob.setTarget(null);
					}
				}
				Vfx.veil(level, t);
			});
			case "empower" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks(10, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.empower(level, t);
			});
			case "levitate" -> moved.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks(3, duration), 1, false, true));
				if (t != caster) {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT, ticks(3, duration) + 20);
				}
				Vfx.levitate(level, t);
			});
			case "freeze" -> harmed.forEach(t -> {
				Spirits.freeze(t, ticks(2.5, duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 3 * power);
				Vfx.freeze(level, t);
			});
			case "meteor" -> {
				List<Vec3> targets = new ArrayList<>();
				harmed.forEach(t -> targets.add(t.position()));
				if (targets.isEmpty()) {
					targets.add(hit.point());
				}
				double radius = 3.0 * SpellNumbers.effectRadius(node);
				for (int i = 0; i < Math.min(4, targets.size()); i++) {
					meteor(cast, targets.get(i), radius, power);
				}
			}
			case "tremor" -> tremor(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "gravity_well" -> gravityWell(cast, hit.point(), 7.0 * SpellNumbers.effectRadius(node), power, duration);
			case "summon" -> Spirits.summonWolves(cast, caster.position(), 3, power, duration);
			case "venom" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.POISON, ticks(6, duration), Math.min(3, 1 + amplify), false, true), caster);
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 2 * power);
				Vfx.venom(level, t);
			});
			case "smite" -> harmed.forEach(t -> {
				double undead = t.isInvertedHealAndHarm() ? 2.0 : 1.0;
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 10 * power * undead);
				Vfx.smite(level, t);
			});
			case "inferno" -> inferno(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power, duration);
			case "thunderclap" -> thunderclap(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power);
			case "starfall" -> starfall(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "blind" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks(5, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks(5, duration), 0, false, true));
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				Vfx.blind(level, t);
			});
			case "chill" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(6, duration), 1, false, true));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 1 * power);
				Reactions.mark(t, Reactions.Mark.FROZEN, 40);
				Vfx.chill(level, t);
			});
			case "silence" -> harmed.forEach(t -> {
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks(6, duration), 1, false, true));
				Vfx.silence(level, t);
			});
			case "fireward" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks(30, duration), 0, false, true));
				t.clearFire();
				Vfx.fireward(level, t);
			});
			case "nourish" -> helped.forEach(t -> {
				if (t instanceof Player player) {
					player.getFoodData().eat((int) Math.round(6 * power), 0.6F);
				}
				Vfx.nourish(level, t);
			});
			case "tidebreath" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks(30, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks(30, duration), 0, false, true));
				Vfx.tidebreath(level, t);
			});
			case "leap" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks(15, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.leap(level, t);
			});
			case "grapple" -> grapple(cast, hit, power);
			case "harvest" -> harvest(cast, hit, SpellNumbers.effectRadius(node));
			case "icepath" -> icepath(cast, hit, 3.0 * SpellNumbers.effectRadius(node));
			case "collect" -> collect(cast, hit.point(), 8.0 * SpellNumbers.effectRadius(node));
			case "excavate" -> excavate(cast, hit, amplify > 0);
			case "blink" -> blink(cast, hit);
			case "light" -> light(cast, hit, duration);
			case "grow" -> grow(cast, hit, power);
			case "break" -> breakBlock(cast, hit, amplify > 0);
			case "cleave" -> harmed.forEach(t -> Techniques.cleave(cast, t, power));
			case "dismantle" -> harmed.forEach(t -> Techniques.dismantle(cast, t, power));
			case "blackspark" -> harmed.forEach(t -> Techniques.blackspark(cast, t, power));
			case "aftershock" -> harmed.forEach(t -> Techniques.aftershock(cast, t, power));
			case "resonance" -> harmed.forEach(t -> Techniques.resonance(cast, t, power, ticks(10, duration)));
			case "ripple" -> harmed.forEach(t -> Techniques.ripple(cast, t, power));
			case "primer" -> {
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, harmed.size()); i++) {
					Techniques.primer(cast, harmed.get(i), 3.0 * SpellNumbers.effectRadius(node), power);
				}
			}
			case "blackflame" -> harmed.forEach(t -> Techniques.blackflame(cast, t, power, (int) Math.round(6 * duration), true));
			case "hollow" -> Techniques.hollow(cast, hit, harmed, 4.0 * SpellNumbers.effectRadius(node), power);
			case "repel" -> Techniques.repel(cast, hit, 3.0 * SpellNumbers.effectRadius(node), power);
			case "decree" -> Techniques.decree(cast, harmed, ticks(2, duration));
			case "weigh" -> harmed.forEach(t -> Techniques.weigh(cast, t, ticks(5, duration)));
			case "shackle" -> harmed.forEach(t -> Techniques.shackle(cast, t, ticks(5, duration)));
			case "bubble" -> harmed.forEach(t -> Techniques.bubble(cast, t, ticks(3, duration), power));
			case "infinity" -> helped.forEach(t -> Wards.infinity(cast, t, ticks(6, duration)));
			case "reversal" -> helped.forEach(t -> Wards.reversal(cast, t, ticks(30, duration)));
			case "reflect" -> helped.forEach(t -> Wards.reflect(cast, t, ticks(10, duration), Math.min(1.5, 0.6 * power)));
			case "overdrive" -> helped.forEach(t -> Techniques.overdrive(cast, t, ticks(10, duration), amplify));
			case "foresight" -> helped.forEach(t -> Wards.foresight(cast, t, ticks(15, duration), (int) Math.max(1, Math.round(2 * power))));
			case "restore" -> helped.forEach(t -> Techniques.restore(cast, t, power));
			case "swap" -> Techniques.swap(cast, hit);
			case "zipper" -> Techniques.zipper(cast);
			case "shadowstep" -> Techniques.shadowstep(cast, hit);
			case "stasis" -> harmed.forEach(t -> Wards.stasis(cast, t, ticks(5, duration)));
			case "rewind" -> helped.forEach(t -> Wards.rewind(cast, t));
			case "accelerate" -> helped.forEach(t -> Techniques.accelerate(cast, t, ticks(10, duration), amplify));
			case "time_skip" -> Techniques.timeSkip(cast);
			case "rampart" -> Techniques.rampart(cast, hit, SpellNumbers.effectRadius(node), ticks(10, duration));
			case "shades" -> Spirits.summonShades(cast, caster.position(), 2, power, duration);
			case "thunderbird" -> Techniques.thunderbird(cast, power, ticks(15, duration));
			default -> { }
		}
		List<LivingEntity> touched = rune.kind() == EffectKind.HELPFUL ? helped : harmed;
		if (!hit.self()) {
			Vfx.Theme theme = Vfx.theme(rune);
			touched.forEach(t -> Vfx.touched(level, t, theme));
		}
	}

	// ------------------------------------------------------------------ helpers

	private static List<LivingEntity> filter(List<Entity> entities, Predicate<Entity> test) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : entities) {
			if (e instanceof LivingEntity living && test.test(e)) {
				out.add(living);
			}
		}
		return out;
	}

	static int ticks(double seconds, double duration) {
		return (int) Math.round(seconds * 20 * duration);
	}

	/** A horizontal unit vector, falling back to {@code fallback} when the input is (nearly) vertical. */
	static Vec3 horizontal(Vec3 v, Vec3 fallback) {
		Vec3 flat = new Vec3(v.x, 0, v.z);
		if (flat.lengthSqr() < 1.0E-4) {
			flat = new Vec3(fallback.x, 0, fallback.z);
		}
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	static void push(LivingEntity target, Vec3 impulse) {
		double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Vec3 scaled = target instanceof Player ? impulse : impulse.scale(Math.max(0.0, 1.0 - resist));
		target.setDeltaMovement(target.getDeltaMovement().add(scaled));
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/**
	 * Damages with invulnerability frames skipped, so stacked effects in one spell all land.
	 * Execute on the current effect doubles it against targets under half health.
	 */
	static void hurt(Cast cast, LivingEntity target, DamageSource source, double amount) {
		if (executeBonus > 1.0 && target.getHealth() < target.getMaxHealth() * 0.5F) {
			amount *= executeBonus;
			Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, target.getBoundingBox().getCenter(), 4, 0.3, 0.1);
		}
		float damage = (float) amount;
		if (target instanceof Player) {
			damage *= PVP_DAMAGE;
		}
		HeartCircles.hurtBySpell(cast.caster, target);
		target.setInvulnerableTime(0);
		target.hurtServer(cast.level, source, damage);
	}

	private static void lightning(Cast cast, Vec3 at, double power) {
		ServerLevel level = cast.level;
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(at.x, at.y, at.z);
			level.addFreshEntity(bolt);
		}
		Vfx.lightning(level, at);
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(2.0, 3.0, 2.0), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 12 * power * Reactions.storm(cast, target));
			target.igniteForSeconds(4);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
		}
	}

	static void explode(Cast cast, Vec3 center, double radius, double power) {
		ServerLevel level = cast.level;
		double implode = Reactions.blast(cast, center, radius);
		radius *= implode;
		power *= implode > 1 ? 1.3 : 1.0;
		Vfx.explosion(level, center, radius);
		DamageSource source = level.damageSources().explosion(cast.caster, cast.caster);
		for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			double distance = target.getBoundingBox().getCenter().distanceTo(center);
			if (distance > radius) {
				continue;
			}
			double falloff = 1.0 - 0.4 * (distance / radius);
			hurt(cast, target, source, 12 * power * falloff * Reactions.fire(cast, target));
			Vec3 away = target.position().subtract(center);
			push(target, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(1.3 * falloff).add(0, 0.45, 0));
		}
	}

	private static void dragonBreath(Cast cast, Vec3 center, double radius, double power, double duration) {
		int pulses = (int) Math.round(5 * duration);
		Fx.sound(cast.level, center, net.minecraft.sounds.SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.dragonBreath(cast.level, center, radius);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
					hurt(cast, (LivingEntity) e, cast.level.damageSources().source(DamageTypes.DRAGON_BREATH, cast.caster), 5 * power);
				}
			});
		}
	}

	private static void blink(Cast cast, Cast.Hit hit) {
		ServerPlayer caster = cast.caster;
		Vec3 target = hit.point();
		Vec3 back = hit.dir().lengthSqr() > 1.0E-4 ? hit.dir().normalize().scale(-0.6) : Vec3.ZERO;
		if (target.distanceTo(caster.position()) > 40) {
			caster.sendOverlayMessage(Component.translatable("message.wildercord.blink_far"));
			return;
		}
		for (int attempt = 0; attempt < 6; attempt++) {
			Vec3 spot = target.add(back.scale(1 + attempt * 0.5)).add(0, attempt % 2 == 0 ? 0 : 1, 0);
			spot = CastEngine.ground(cast.level, spot);
			AABB box = caster.getDimensions(caster.getPose()).makeBoundingBox(spot);
			if (cast.level.noCollision(caster, box)) {
				Vec3 from = caster.position();
				caster.teleportTo(cast.level, spot.x, spot.y, spot.z, Set.<Relative>of(), caster.getYRot(), caster.getXRot(), false);
				caster.resetFallDistance();
				Vfx.blink(cast.level, from, spot);
				return;
			}
		}
	}

	/** Inferno: everything around the point burns for a few seconds. */
	private static void inferno(Cast cast, Vec3 point, double radius, double power, double duration) {
		int pulses = (int) Math.round(4 * duration);
		Fx.sound(cast.level, point, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.inferno(cast.level, point, radius);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity t = (LivingEntity) e;
					t.igniteForSeconds(3);
					hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3 * power * Reactions.fire(cast, t));
				}
			});
		}
	}

	/** Thunderclap: a crack of thunder that hurts and hurls everything around the point. */
	private static void thunderclap(Cast cast, Vec3 point, double radius, double power) {
		Vfx.thunderclap(cast.level, point, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 5 * power * Reactions.storm(cast, t));
			Vec3 away = horizontal(t.position().subtract(point), cast.caster.getLookAngle());
			push(t, away.scale(2.0 * power).add(0, 0.5, 0));
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Starfall: stars rain down around the point over two seconds. */
	private static void starfall(Cast cast, Vec3 point, double radius, double power) {
		for (int i = 0; i < 8; i++) {
			double a = cast.level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(cast.level.getRandom().nextDouble()) * radius;
			Vec3 target = CastEngine.ground(cast.level, point.add(Math.cos(a) * r, 2, Math.sin(a) * r));
			Scheduler.later(1 + i * 5, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.star(cast.level, target);
				Scheduler.later(6, () -> {
					for (Entity e : cast.level.getEntities((Entity) null, new AABB(target, target).inflate(1.6, 2.0, 1.6), e -> Targets.canHarm(cast.caster, e))) {
						hurt(cast, (LivingEntity) e, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 6 * power);
					}
				});
			});
		}
	}

	/** Grapple: pulls the caster toward the point the spell hit. */
	private static void grapple(Cast cast, Cast.Hit hit, double power) {
		ServerPlayer caster = cast.caster;
		Vec3 to = hit.point().subtract(caster.position());
		double distance = to.length();
		if (distance < 1.0 || distance > 48) {
			return;
		}
		Vec3 pull = to.normalize().scale(Math.min(3.2, 0.8 + distance * 0.12) * Math.sqrt(power)).add(0, 0.35, 0);
		caster.setDeltaMovement(pull);
		caster.needsSync = true;
		caster.connection.send(new ClientboundSetEntityMotionPacket(caster));
		caster.resetFallDistance();
		Scheduler.later(30, caster::resetFallDistance);
		Vfx.grapple(cast.level, caster.getEyePosition().subtract(0, 0.4, 0), hit.point());
	}

	/** Harvest: breaks grown crops around the block hit and replants them from their drops. */
	private static void harvest(Cast cast, Cast.Hit hit, double radiusScale) {
		BlockPos center = targetBlock(hit);
		int r = (int) Math.round(1 * radiusScale) + 1;
		int harvested = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			BlockState state = cast.level.getBlockState(p);
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) || !crop.isMaxAge(state)) {
				continue;
			}
			if (!cast.caster.mayBuild() || !cast.level.mayInteract(cast.caster, p) || !cast.takeBlock()) {
				break;
			}
			cast.level.destroyBlock(p, true, cast.caster);
			cast.level.setBlockAndUpdate(p, crop.getStateForAge(0));
			harvested++;
		}
		if (harvested > 0) {
			Vfx.grow(cast.level, Vec3.atCenterOf(center).add(0, 0.6, 0));
			Fx.sound(cast.level, Vec3.atCenterOf(center), net.minecraft.sounds.SoundEvents.CROP_BREAK, 0.8F, 1.2F);
		}
	}

	/** Icepath: water near the point freezes into frosted ice that melts on its own. */
	private static void icepath(Cast cast, Cast.Hit hit, double radius) {
		BlockPos center = BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
		int r = (int) Math.ceil(radius);
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		int frozen = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			if (p.distSqr(center) > radius * radius) {
				continue;
			}
			BlockState state = cast.level.getBlockState(p);
			if (state.is(Blocks.WATER) && state.getFluidState().isSource() && cast.level.getBlockState(p.above()).isAir()
					&& cast.level.mayInteract(cast.caster, p) && cast.caster.mayBuild()) {
				cast.level.setBlockAndUpdate(p, ice);
				cast.level.scheduleTick(p, Blocks.FROSTED_ICE, 60 + cast.level.getRandom().nextInt(60));
				frozen++;
			}
		}
		if (frozen > 0) {
			Vfx.icepath(cast.level, Vec3.atCenterOf(center), radius);
		}
	}

	/** Collect: items and experience orbs around the point fly to the caster. */
	private static void collect(Cast cast, Vec3 point, double radius) {
		ServerPlayer caster = cast.caster;
		int moved = 0;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius),
				e -> e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb)) {
			Vfx.stream(cast.level, e.position(), caster.position().add(0, 1, 0), Vfx.theme("void"), 1);
			e.teleportTo(caster.getX(), caster.getY() + 0.5, caster.getZ());
			if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
				item.setNoPickUpDelay();
			}
			moved++;
		}
		if (moved > 0) {
			Fx.sound(cast.level, caster.position(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
		}
	}

	/** Excavate: mines a 3x3 face of blocks around the block hit. */
	private static void excavate(Cast cast, Cast.Hit hit, boolean amplified) {
		if (hit.block() == null) {
			return;
		}
		net.minecraft.core.Direction face = hit.face() == null ? net.minecraft.core.Direction.UP : hit.face();
		BlockPos center = hit.block();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				BlockPos p = switch (face.getAxis()) {
					case X -> center.offset(0, a, b);
					case Y -> center.offset(a, 0, b);
					case Z -> center.offset(a, b, 0);
				};
				BlockState state = cast.level.getBlockState(p);
				if (state.isAir() || state.getDestroySpeed(cast.level, p) < 0
						|| state.is(amplified ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL)) {
					continue;
				}
				if (mayEdit(cast, p)) {
					cast.level.destroyBlock(p, true, cast.caster);
				}
			}
		}
		Vfx.tremor(cast.level, Vec3.atCenterOf(center), 1.5);
	}

	/** Shock: a small zap that arcs on to the nearest other enemy. */
	private static void shock(Cast cast, LivingEntity target, double power) {
		ServerLevel level = cast.level;
		Vfx.shockArc(level, target.getBoundingBox().getCenter().add(0, 1.2, 0), target.getBoundingBox().getCenter());
		hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 4 * power * Reactions.storm(cast, target));
		Entity next = level.getEntities(target, target.getBoundingBox().inflate(4.0), e -> Targets.canHarm(cast.caster, e))
			.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target))).orElse(null);
		if (next != null) {
			Vfx.shockArc(level, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
			hurt(cast, (LivingEntity) next, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 3 * power);
		}
	}

	/** Meteor: a burning rock falls for half a second, then bursts. */
	private static void meteor(Cast cast, Vec3 target, double radius, double power) {
		Vec3 ground = CastEngine.ground(cast.level, target.add(0, 1, 0));
		Vfx.meteorFall(cast.level, ground);
		Scheduler.later(12, () -> {
			if (!cast.alive()) {
				return;
			}
			double implode = Reactions.blast(cast, ground, radius);
			double r = radius * implode;
			Vfx.explosion(cast.level, ground.add(0, 0.5, 0), r);
			DamageSource source = cast.level.damageSources().explosion(cast.caster, cast.caster);
			for (Entity e : cast.level.getEntities((Entity) null, new AABB(ground, ground).inflate(r), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity t = (LivingEntity) e;
				double distance = t.position().distanceTo(ground);
				if (distance > r) {
					continue;
				}
				double falloff = 1.0 - 0.4 * (distance / r);
				hurt(cast, t, source, 10 * power * falloff * (implode > 1 ? 1.3 : 1.0) * Reactions.fire(cast, t));
				t.igniteForSeconds(4);
				Vec3 away = t.position().subtract(ground);
				push(t, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.8 * falloff).add(0, 0.5, 0));
			}
		});
	}

	/** Tremor: the ground erupts around a point, hurting and throwing enemies standing on it. */
	private static void tremor(Cast cast, Vec3 point, double radius, double power) {
		Vec3 ground = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		Vfx.tremor(cast.level, ground, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(ground, ground).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			if (t.position().distanceTo(ground) > radius || (!t.onGround() && t.position().y - ground.y > 1.5)) {
				continue;
			}
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.FALLING_BLOCK, cast.caster), 8 * power);
			push(t, new Vec3(0, 0.75, 0));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Gravity Well: for a while, enemies around the point are dragged into it, then crushed a little. */
	private static void gravityWell(Cast cast, Vec3 point, double radius, double power, double duration) {
		int total = (int) Math.round(40 * duration);
		Fx.sound(cast.level, point, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 1.4F);
		for (int t = 0; t <= total; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.gravityWell(cast.level, point, radius, tick);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity victim = (LivingEntity) e;
					Vec3 towards = point.subtract(victim.position());
					double distance = towards.length();
					if (distance > radius || distance < 0.4) {
						continue;
					}
					victim.setDeltaMovement(victim.getDeltaMovement().scale(0.5).add(towards.normalize().scale(Math.min(0.6, 0.12 + distance * 0.05))));
					victim.needsSync = true;
					if (victim instanceof ServerPlayer player) {
						player.connection.send(new ClientboundSetEntityMotionPacket(player));
					}
					Reactions.mark(victim, Reactions.Mark.PULLED);
					if (tick >= total - 1) {
						hurt(cast, victim, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 4 * power);
					}
				}
			});
		}
	}

	/** The block a world effect should act on: the one hit, or the one under a creature. */
	private static BlockPos targetBlock(Cast.Hit hit) {
		if (hit.block() != null) {
			return hit.block();
		}
		return BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
	}

	private static boolean mayEdit(Cast cast, BlockPos pos) {
		return cast.caster.mayBuild() && cast.level.mayInteract(cast.caster, pos) && cast.takeBlock();
	}

	private static void light(Cast cast, Cast.Hit hit, double duration) {
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		ServerLevel level = cast.level;
		if (!level.getBlockState(pos).isAir() || !mayEdit(cast, pos)) {
			return;
		}
		level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
		Vfx.light(level, Vec3.atCenterOf(pos));
		Scheduler.later(ticks(60, duration), () -> {
			if (level.getBlockState(pos).is(Blocks.LIGHT)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		});
	}

	/** Bone-meals the block that was hit and the ones around it. */
	private static void grow(Cast cast, Cast.Hit hit, double power) {
		BlockPos center = targetBlock(hit);
		int times = (int) Math.round(2 * power);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			BlockPos p = pos.immutable();
			if (!cast.caster.mayBuild() || !cast.level.mayInteract(cast.caster, p)) {
				continue;
			}
			boolean grew = false;
			for (int i = 0; i < times; i++) {
				grew |= BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), cast.level, p);
			}
			if (grew) {
				cast.level.levelEvent(null, 1505, p, 15);
			}
		}
		Vfx.grow(cast.level, Vec3.atCenterOf(center).add(0, 0.6, 0));
	}

	private static void breakBlock(Cast cast, Cast.Hit hit, boolean amplified) {
		if (hit.block() == null) {
			return;
		}
		BlockPos pos = hit.block();
		BlockState state = cast.level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(cast.level, pos) < 0) {
			return;
		}
		if (state.is(amplified ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL)) {
			return;
		}
		if (mayEdit(cast, pos)) {
			cast.level.destroyBlock(pos, true, cast.caster);
		}
	}
}
