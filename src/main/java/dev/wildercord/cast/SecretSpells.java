package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.Secrets;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What each secret spell does (see {@link Secrets}). Each is its own small set piece: a
 * telegraph, a build-up and a payoff, all scaled by the caster's power like any other spell.
 */
public final class SecretSpells {
	private SecretSpells() {}

	/** Rebirth: who is protected, and until when. */
	private static final Map<UUID, Long> REBIRTH = new HashMap<>();
	private static final Map<UUID, Double> REBIRTH_POWER = new HashMap<>();

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			Long until = REBIRTH.get(entity.getUUID());
			if (until == null || !(entity.level() instanceof ServerLevel level) || level.getGameTime() > until) {
				return true;
			}
			REBIRTH.remove(entity.getUUID());
			reborn(level, entity, REBIRTH_POWER.getOrDefault(entity.getUUID(), 1.0));
			return false;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			REBIRTH.clear();
			REBIRTH_POWER.clear();
		});
	}

	/** The first cast of a secret: it goes into the Grimoire, with a title. */
	public static void discover(ServerPlayer player, Secrets.Secret secret) {
		if (!Grimoire.unlock(player, secret.key())) {
			return;
		}
		player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(secret.name()).withColor(secret.color())));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.wildercord.secret").withColor(0xE8E0FF)));
		Fx.sound(player.level(), player.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7F, 1.3F);
	}

	public static void cast(Cast cast, Secrets.Secret secret) {
		double power = cast.power;
		switch (secret.id()) {
			case "glacial_lance" -> glacialLance(cast, power);
			case "sunfall" -> sunfall(cast, power);
			case "horizon_cut" -> horizonCut(cast, power);
			case "petal_storm" -> petalStorm(cast, power);
			case "tempest_step" -> tempestStep(cast, power);
			case "singularity" -> singularity(cast, power);
			case "zero_hour" -> zeroHour(cast);
			case "rebirth" -> rebirth(cast, power);
			case "tectonic_rise" -> tectonicRise(cast, power);
			case "starlight_cascade" -> starlightCascade(cast, power);
			default -> { }
		}
	}

	// ------------------------------------------------------------------ helpers

	private static DustParticleOptions dust(int color, float scale) {
		return new DustParticleOptions(color, scale);
	}

	private static void dot(ServerLevel level, ParticleOptions p, Vec3 at) {
		Vfx.emit(level, p, at, 1, 0.0, 0.0);
	}

	private static void line(ServerLevel level, ParticleOptions p, Vec3 a, Vec3 b, double step) {
		Vec3 d = b.subtract(a);
		int n = (int) Math.min(90, Math.max(1, d.length() / step));
		for (int i = 0; i <= n; i++) {
			dot(level, p, a.add(d.scale(i / (double) n)));
		}
	}

	private static void sphere(ServerLevel level, ParticleOptions p, Vec3 c, double r, int points, double spin) {
		for (int i = 0; i < points; i++) {
			double y = 1 - (i + 0.5) * 2.0 / points;
			double rr = Math.sqrt(Math.max(0, 1 - y * y));
			double a = i * 2.39996323 + spin;
			dot(level, p, c.add(Math.cos(a) * rr * r, y * r, Math.sin(a) * rr * r));
		}
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static List<LivingEntity> enemiesNear(Cast cast, Vec3 c, double r) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(r), e -> Targets.canHarm(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(c) <= r + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	private static Vec3 aim(Cast cast, double range) {
		Vec3 from = cast.caster.getEyePosition();
		Vec3 to = from.add(cast.caster.getLookAngle().scale(range));
		BlockHitResult hit = cast.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cast.caster));
		return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
	}

	// ------------------------------------------------------------------ the spells

	/** A lance of ice through everything in a line, then lightning along the frozen targets. */
	private static void glacialLance(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 dir = caster.getLookAngle();
		Vec3 from = caster.getEyePosition().add(dir.scale(0.8)).add(0, -0.15, 0);
		Vec3 end = aim(cast, 32);
		Sigils.telegraph(level, from.add(dir.scale(0.3)), dir, 0x9FE4FF, 0.9F, 14);
		List<LivingEntity> pierced = new ArrayList<>();
		for (Entity e : level.getEntities(caster, new AABB(from, end).inflate(1.2), e -> Targets.canHarm(caster, e))) {
			if (e.getBoundingBox().inflate(0.6).clip(from, end).isPresent()) {
				pierced.add((LivingEntity) e);
			}
		}
		pierced.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
		double length = end.distanceTo(from);
		int flight = 6;
		for (int t = 0; t < flight; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double a = length * tick / flight;
				double b = length * (tick + 1) / flight;
				line(level, dust(0xE8FAFF, 1.4F), from.add(dir.scale(a)), from.add(dir.scale(b)), 0.3);
				line(level, dust(0x7FD0FF, 0.9F), from.add(dir.scale(Math.max(0, a - 2))), from.add(dir.scale(b)), 0.5);
				Vfx.emit(level, ParticleTypes.SNOWFLAKE, from.add(dir.scale(b)), 6, 0.2, 0.02);
				Fx.sound(level, from.add(dir.scale(b)), SoundEvents.GLASS_HIT, 0.6F, 1.6F);
			});
		}
		for (LivingEntity t : pierced) {
			int delay = 1 + (int) Math.floor(flight * t.distanceTo(caster) / Math.max(1.0, length));
			Scheduler.later(delay, () -> {
				if (!cast.alive() || !t.isAlive()) {
					return;
				}
				Effects.hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 7 * power);
				Spirits.freeze(t, 50);
				BlockFx.encase(level, t, 50);
				t.setTicksFrozen(t.getTicksRequiredToFreeze() + 60);
				Reactions.mark(t, Reactions.Mark.FROZEN);
				Vfx.frost(level, t);
			});
		}
		// The spark wakes the ice: lightning jumps from target to target along the lance.
		Scheduler.later(flight + 6, () -> {
			Vec3 prev = from;
			for (LivingEntity t : pierced) {
				if (!t.isAlive()) {
					continue;
				}
				Vec3 c = t.getBoundingBox().getCenter();
				Vfx.shockArc(level, prev, c);
				Effects.hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 5 * power);
				prev = c;
			}
			Fx.sound(level, from, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.5F);
		});
		Fx.sound(level, from, SoundEvents.TRIDENT_THROW, 1.0F, 0.6F);
		Fx.sound(level, from, SoundEvents.GLASS_BREAK, 0.8F, 1.8F);
	}

	/** A small sun sinks onto the point, then bursts; the ground burns. */
	private static void sunfall(Cast cast, double power) {
		ServerLevel level = cast.level;
		Vec3 point = CastEngine.ground(level, aim(cast, CastEngine.AIM_RANGE));
		Sigils.target(level, point, 0xFF8A30, 7.0F, 42);
		Sigils.ground(level, point, 0xFFB050, 0xFFE8A0, 3.5F, 42);
		int fall = 36;
		for (int t = 0; t <= fall; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				double k = tick / (double) fall;
				Vec3 sun = point.add(0, 16 * (1 - k * k) + 1.4, 0);
				double r = 1.2 + 0.6 * k;
				sphere(level, dust(0xFFE070, 2.0F), sun, r, 26, tick * 0.3);
				sphere(level, dust(0xFF7A20, 1.6F), sun, r * 0.7, 14, -tick * 0.4);
				Vfx.emit(level, ParticleTypes.FLAME, sun, 6, r * 0.6, 0.02);
				Vfx.emit(level, ParticleTypes.LAVA, sun, 1, r * 0.4, 0.0);
				if (tick % 8 == 0) {
					Fx.sound(level, sun, SoundEvents.BLAZE_BURN, 1.0F, 0.6F + (float) k * 0.4F);
				}
			});
		}
		Scheduler.later(fall + 2, () -> {
			if (!cast.alive()) {
				return;
			}
			Vec3 c = point.add(0, 1.0, 0);
			Effects.explode(cast, c, 7.0, power * 2.2);
			Sigils.flash(level, c, 0xFFFFD080, 3.0F);
			Vfx.shockwave(level, point, 8.0, Vfx.theme("fire"), 8);
			Vfx.radial(level, ParticleTypes.FLAME, c, 60, 0.5);
			Vfx.radial(level, ParticleTypes.LAVA, c, 12, 0.3);
			Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.4F, 0.6F);
			Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 1.0F, 0.5F);
			// The ground burns for a few seconds.
			for (int s = 0; s < 4; s++) {
				Cast pulse = cast.pulse();
				Scheduler.later(10 + s * 20, () -> {
					if (!pulse.alive()) {
						return;
					}
					for (int i = 0; i < 26; i++) {
						double a = level.getRandom().nextDouble() * Math.PI * 2;
						double rr = Math.sqrt(level.getRandom().nextDouble()) * 5.5;
						Vfx.emit(level, ParticleTypes.FLAME, point.add(Math.cos(a) * rr, 0.15, Math.sin(a) * rr), 1, 0.05, 0.02);
					}
					for (LivingEntity t : enemiesNear(pulse, point.add(0, 1, 0), 5.5)) {
						t.igniteForSeconds(3);
						Effects.hurt(pulse, t, level.damageSources().source(DamageTypes.IN_FIRE, pulse.caster), 2 * power);
					}
				});
			}
		});
	}

	/** One slash that grows to 30 blocks wide, cutting everything in front at once. */
	private static void horizonCut(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 eye = caster.getEyePosition().add(0, -0.3, 0);
		Vec3 fwd = flat(caster.getLookAngle());
		Vec3 side = fwd.cross(new Vec3(0, 1, 0)).normalize();
		Set<UUID> cut = new HashSet<>();
		Fx.sound(level, eye, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
		Fx.sound(level, eye, SoundEvents.TRIDENT_RIPTIDE_1, 1.0F, 1.4F);
		for (int t = 0; t < 6; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				double reach = 3 + tick * 2.6;
				double half = 2 + tick * 2.6;
				int points = (int) (half * 5);
				for (int i = -points; i <= points; i++) {
					double s = i / (double) points;
					double bow = (1 - s * s) * 2.2;
					Vec3 p = eye.add(fwd.scale(reach - 2.2 + bow)).add(side.scale(s * half));
					dot(level, dust(i % 3 == 0 ? 0xFFFFFF : 0xF0C0D0, 1.3F), p);
					if (i % 4 == 0) {
						dot(level, dust(0xB01830, 0.9F), p.add(fwd.scale(-0.4)));
					}
				}
				Vfx.emit(level, ParticleTypes.SWEEP_ATTACK, eye.add(fwd.scale(reach)), 3, half * 0.3, 0.0);
				for (Entity e : level.getEntities(caster, new AABB(eye, eye).inflate(half + 2, 3.0, half + 2), e -> Targets.canHarm(caster, e))) {
					Vec3 rel = e.getBoundingBox().getCenter().subtract(eye);
					double along = rel.dot(fwd);
					if (along < 0 || along > reach + 1.5 || Math.abs(rel.dot(side)) > half + 1 || Math.abs(rel.y) > 3.0 || !cut.add(e.getUUID())) {
						continue;
					}
					LivingEntity target = (LivingEntity) e;
					double extra = Math.min(40, target.getMaxHealth() * 0.1);
					Effects.hurt(cast, target, magic(cast), 12 * power + extra);
					TechniqueVfx.cleave(level, target, fwd);
				}
			});
		}
	}

	/** A storm of petals around the caster: it mends allies and cuts enemies. */
	private static void petalStorm(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		int ticks = (int) Math.round(160 * cast.duration);
		Fx.sound(level, caster.position(), SoundEvents.CHERRY_LEAVES_BREAK, 1.2F, 0.8F);
		Sigils.ground(level, caster.position(), 0xFFA8D8, 0xFFE8F4, 2.4F, 30);
		for (int t = 0; t < ticks; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 c = caster.position().add(0, 1.0, 0);
				// Three spiral arms of petals, turning around the caster.
				for (int i = 0; i < 24; i++) {
					double arm = Math.PI * 2 * (i % 3) / 3;
					double along = (i / 3) / 8.0;
					double r = 1.5 + 5.5 * along;
					double a = tick * 0.2 + arm + along * 2.4;
					double y = Math.sin(tick * 0.15 + i) * 0.9 - 0.3 + along * 0.6;
					dot(level, dust(i % 3 == 0 ? 0xFFE0F0 : i % 3 == 1 ? 0xFFB0DC : 0xF080C0, 1.1F), c.add(Math.cos(a) * r, y, Math.sin(a) * r));
				}
				Vfx.emit(level, ParticleTypes.CHERRY_LEAVES, c.add(0, 1.5, 0), 6, 3.5, 0.0);
				if (tick % 40 == 0) {
					Sigils.send(level, SigilOption.flat(SigilOption.RING, 0xFFA8D8, 7.2F, 44, 0.03F), caster.position().add(0, 0.07, 0));
				}
				if (tick % 20 == 0) {
					Cast pulse = cast.pulse();
					for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(7), e -> e instanceof LivingEntity && e.isAlive())) {
						LivingEntity target = (LivingEntity) e;
						if (target.distanceTo(caster) > 7.5) {
							continue;
						}
						if (Targets.canHelp(caster, target)) {
							target.heal((float) (3 * power));
							Vfx.emit(level, ParticleTypes.HEART, target.getEyePosition().add(0, 0.4, 0), 1, 0.2, 0.0);
						} else if (Targets.canHarm(caster, target)) {
							target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1, false, true));
							Effects.hurt(pulse, target, magic(pulse), 2 * power);
							Vfx.emit(level, dust(0xE060A0, 1.0F), target.getBoundingBox().getCenter(), 6, 0.3, 0.0);
						}
					}
					Fx.sound(level, c, SoundEvents.CHERRY_LEAVES_STEP, 0.8F, 1.2F);
				}
			});
		}
	}

	/** Flash from enemy to enemy, striking each with lightning. */
	private static void tempestStep(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		List<LivingEntity> path = new ArrayList<>();
		Vec3 from = caster.position();
		for (int i = 0; i < 5; i++) {
			Vec3 at = from;
			LivingEntity next = level.getEntities(caster, new AABB(at, at).inflate(12),
					e -> Targets.canHarm(caster, e) && !path.contains(e) && e.position().distanceTo(at) <= 12)
				.stream().map(e -> (LivingEntity) e).min(Comparator.comparingDouble(e -> e.position().distanceToSqr(at))).orElse(null);
			if (next == null) {
				break;
			}
			path.add(next);
			from = next.position();
		}
		if (path.isEmpty()) {
			Casters.tell(caster, Component.translatable("message.wildercord.no_targets"));
			return;
		}
		Fx.sound(level, caster.position(), SoundEvents.TRIDENT_THUNDER, 0.8F, 1.6F);
		for (int i = 0; i < path.size(); i++) {
			LivingEntity target = path.get(i);
			Scheduler.later(1 + i * 3, () -> {
				if (!cast.alive() || !target.isAlive()) {
					return;
				}
				Vec3 before = caster.position().add(0, 1, 0);
				Vec3 away = flat(target.position().subtract(caster.position()));
				Vec3 spot = target.position().subtract(away.scale(target.getBbWidth() / 2 + 0.7));
				if (!level.noCollision(caster, caster.getDimensions(caster.getPose()).makeBoundingBox(spot))) {
					spot = target.position().add(0, target.getBbHeight() + 0.1, 0);
				}
				caster.teleportTo(level, spot.x, spot.y, spot.z, Set.of(), caster.getYRot(), caster.getXRot(), false);
				caster.resetFallDistance();
				Vec3 c = target.getBoundingBox().getCenter();
				Vfx.shockArc(level, before, c);
				Vfx.shockArc(level, before.add(0, 0.4, 0), c.add(0, 0.3, 0));
				Sigils.flash(level, c, 0xFFFFF4A0, 3.0F);
				Effects.hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 8 * power * Reactions.storm(cast, target));
				Effects.push(target, away.scale(0.6).add(0, 0.3, 0));
				Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.8F);
			});
		}
	}

	/** A black star that drifts out, swallows everything around it, then collapses. */
	private static void singularity(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 dir = caster.getLookAngle();
		Vec3 start = caster.getEyePosition().add(dir.scale(1.2));
		Vec3 stop = aim(cast, 12).subtract(dir.scale(0.8));
		double travel = stop.distanceTo(start);
		int drift = Math.max(4, (int) (travel / 0.6));
		Fx.sound(level, start, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 0.6F);
		for (int t = 0; t <= drift; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (cast.alive()) {
					Vec3 p = start.add(stop.subtract(start).scale(tick / (double) drift));
					sphere(level, dust(0x100818, 1.8F), p, 0.5, 14, tick * 0.5);
					Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, p, 4, 0.3, 0.02);
				}
			});
		}
		int hold = 60;
		Scheduler.later(drift + 2, () -> Sigils.layer(level, stop, new Vec3(0, 1, 0), SigilOption.RING, 0x9A5AF0, 4.5F, hold + 4, 0.15F));
		for (int t = 0; t < hold; t += 2) {
			int tick = t;
			Cast pulse = cast.pulse();
			Scheduler.later(drift + 2 + t, () -> {
				if (!pulse.alive()) {
					return;
				}
				double spin = tick * 0.25;
				sphere(level, dust(0x05020A, 2.2F), stop, 0.9, 20, spin);
				for (int ring = 0; ring < 3; ring++) {
					double r = 2.0 + ring * 1.6;
					for (int k = 0; k < 14; k++) {
						double a = spin * (1.4 - ring * 0.3) + Math.PI * 2 * k / 14;
						dot(level, dust(ring == 0 ? 0xE0B0FF : 0x9A5AF0, 1.0F), stop.add(Math.cos(a) * r, Math.sin(a * 2) * 0.15, Math.sin(a) * r));
					}
				}
				Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, stop, 10, 4.0, 0.0);
				for (LivingEntity t2 : enemiesNear(pulse, stop, 9)) {
					Vec3 pull = stop.subtract(t2.getBoundingBox().getCenter());
					double d = Math.max(0.5, pull.length());
					Effects.push(t2, pull.normalize().scale(Math.min(0.55, 0.12 + 0.9 / d)).subtract(t2.getDeltaMovement().scale(0.4)));
					Reactions.mark(t2, Reactions.Mark.PULLED);
					if (tick % 20 == 0) {
						Effects.hurt(pulse, t2, magic(pulse), 3 * power);
					}
				}
				if (tick % 20 == 0) {
					Fx.sound(level, stop, SoundEvents.BEACON_AMBIENT, 1.2F, 0.5F);
				}
			});
		}
		Scheduler.later(drift + 2 + hold, () -> {
			if (!cast.alive()) {
				return;
			}
			Effects.explode(cast, stop, 5.0, power * 1.5);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, stop, 50, 0.7);
			Sigils.flash(level, stop, 0xFFB080FF, 3.0F);
			Fx.sound(level, stop, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.7F);
		});
	}

	/** Time stops for everything around the caster but the caster. */
	private static void zeroHour(Cast cast) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		int ticks = (int) Math.round(80 * cast.duration);
		Vec3 c = caster.position();
		Sigils.ground(level, c, 0xF2D98A, 0xFFFFFF, 6.0F, ticks);
		Sigils.layer(level, c.add(0, 0.1, 0), new Vec3(0, 1, 0), SigilOption.RING, 0xF2D98A, 20.0F, ticks, 0.01F);
		Fx.sound(level, c, SoundEvents.BELL_BLOCK, 1.5F, 0.4F);
		Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.5F);
		for (Entity e : level.getEntities(caster, new AABB(c, c).inflate(20), e -> e instanceof LivingEntity && e.isAlive() && e != caster)) {
			LivingEntity t = (LivingEntity) e;
			if (t.distanceTo(caster) <= 20 && !Targets.isAlly(caster, t)) {
				Wards.stasis(cast, t, ticks);
			}
		}
		// Time's shell: a slow golden dome that fades as time returns.
		for (int t = 0; t < ticks; t += 10) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double r = 20;
				for (int k = 0; k < 64; k++) {
					double a = Math.PI * 2 * k / 64 + tick * 0.01;
					Fx.sendFar(level, dust(0xF2D98A, 2.2F), c.add(Math.cos(a) * r, 0.3, Math.sin(a) * r));
				}
			});
		}
	}

	/** For a minute, death burns you back to life. */
	private static void rebirth(Cast cast, double power) {
		LivingEntity caster = cast.caster;
		long until = cast.level.getGameTime() + Math.round(1200 * cast.duration);
		REBIRTH.put(caster.getUUID(), until);
		REBIRTH_POWER.put(caster.getUUID(), power);
		Vec3 c = caster.position();
		Sigils.ground(cast.level, c, 0xFF7040, 0xFFD070, 1.8F, 40);
		for (int t = 0; t < 20; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 back = caster.position().add(0, 1.3, 0);
				Vec3 side = flat(caster.getLookAngle()).cross(new Vec3(0, 1, 0));
				for (int k = 1; k <= 8; k++) {
					double s = k * 0.22;
					double lift = Math.sin(k * 0.35) * 0.6 + tick * 0.02;
					dot(cast.level, ParticleTypes.FLAME, back.add(side.scale(s)).add(0, lift, 0).subtract(flat(caster.getLookAngle()).scale(0.3)));
					dot(cast.level, ParticleTypes.FLAME, back.add(side.scale(-s)).add(0, lift, 0).subtract(flat(caster.getLookAngle()).scale(0.3)));
				}
			});
		}
		Fx.sound(cast.level, c, SoundEvents.BLAZE_AMBIENT, 1.0F, 0.6F);
		Fx.sound(cast.level, c, SoundEvents.TOTEM_USE, 0.4F, 1.6F);
	}

	private static void reborn(ServerLevel level, LivingEntity entity, double power) {
		entity.setHealth(entity.getMaxHealth());
		List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
		for (MobEffectInstance effect : entity.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				bad.add(effect.getEffect());
			}
		}
		bad.forEach(entity::removeEffect);
		entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, true));
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
		Vec3 c = entity.getBoundingBox().getCenter();
		Cast blast = new Cast(entity);
		for (LivingEntity t : enemiesNear(blast, c, 6)) {
			t.igniteForSeconds(5);
			Effects.hurt(blast, t, level.damageSources().source(DamageTypes.IN_FIRE, entity), 14 * power);
		}
		Sigils.ground(level, entity.position(), 0xFF7040, 0xFFE0A0, 3.0F, 30);
		Sigils.flash(level, c, 0xFFFFA040, 3.0F);
		Vfx.radial(level, ParticleTypes.FLAME, c, 70, 0.45);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 30, 0.5);
		Vfx.shockwave(level, entity.position(), 6.0, Vfx.theme("fire"), 6);
		Fx.sound(level, c, SoundEvents.TOTEM_USE, 1.0F, 0.9F);
		Fx.sound(level, c, SoundEvents.BLAZE_SHOOT, 1.0F, 0.5F);
		if (entity instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.reborn").withColor(0xFF9050));
		}
	}

	/** Stone spires erupt in a line, throwing everything they hit into the air. */
	private static void tectonicRise(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 fwd = flat(caster.getLookAngle());
		Vec3 base = caster.position();
		Set<UUID> hit = new HashSet<>();
		BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
		BlockParticleOption deep = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState());
		for (int i = 1; i <= 20; i++) {
			int step = i;
			Scheduler.later(i, () -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 p = CastEngine.ground(level, base.add(fwd.scale(step)).add(0, 2, 0));
				float height = 2.0F + (step % 3) * 0.6F + (step % 2) * 0.3F;
				Vec3 side = fwd.cross(new Vec3(0, 1, 0)).normalize().scale(((step * 7) % 5 - 2) * 0.18);
				BlockFx.spire(level, p.add(side), height, 0.75F + (step % 2) * 0.15F, 12);
				Vfx.emit(level, stone, p.add(0, 0.5, 0), 14, 0.4, 0.15);
				Vfx.emit(level, deep, p.add(0, 0.2, 0), 8, 0.6, 0.1);
				if (step % 2 == 0) {
					Fx.sound(level, p, SoundEvents.POINTED_DRIPSTONE_LAND, 1.0F, 0.6F);
					Fx.sound(level, p, SoundEvents.MACE_SMASH_GROUND, 0.6F, 0.8F);
				}
				for (LivingEntity t : enemiesNear(cast, p.add(0, 1, 0), 1.8)) {
					if (hit.add(t.getUUID())) {
						Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.FALLING_STALACTITE, caster), 10 * power);
						Effects.push(t, new Vec3(0, 1.1, 0).add(fwd.scale(0.3)));
					}
				}
			});
		}
	}

	/** A beam that calls stars down along its whole length. */
	private static void starlightCascade(Cast cast, double power) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		Vec3 from = caster.getEyePosition();
		Vec3 end = aim(cast, 24);
		Vec3 dir = end.subtract(from);
		Vfx.beam(level, from.add(caster.getLookAngle().scale(0.8)), end, Vfx.theme("arcane"));
		line(level, dust(0xE8F0FF, 1.2F), from.add(dir.scale(0.05)), end, 0.4);
		Fx.sound(level, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.8F);
		Fx.sound(level, from, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
		Vec3 side = flat(dir).cross(new Vec3(0, 1, 0)).normalize();
		for (int i = 0; i < 24; i++) {
			double along = 0.15 + 0.85 * level.getRandom().nextDouble();
			double lateral = (level.getRandom().nextDouble() - 0.5) * 3.0;
			Vec3 target = CastEngine.ground(level, from.add(dir.scale(along)).add(side.scale(lateral)).add(0, 1, 0));
			int delay = 4 + i * 60 / 24;
			Cast pulse = cast.pulse();
			Scheduler.later(delay, () -> {
				if (pulse.alive()) {
					Vfx.star(level, target);
				}
			});
			Scheduler.later(delay + 6, () -> {
				if (!pulse.alive()) {
					return;
				}
				for (LivingEntity t : enemiesNear(pulse, target.add(0, 0.8, 0), 1.9)) {
					Effects.hurt(pulse, t, magic(pulse), 7 * power);
				}
			});
		}
	}

	public static void forget(UUID id) {
		REBIRTH.remove(id);
		REBIRTH_POWER.remove(id);
	}

}
