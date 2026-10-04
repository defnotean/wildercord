package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.spell.Secrets;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
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
import net.minecraft.world.item.Items;
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
			// As Reversal: never against what nothing survives (/kill, the void).
			if (until == null || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || !(entity.level() instanceof ServerLevel level)
					|| level.getGameTime() > until || DeathsDoor.resting(entity) > 0) {
				return true;
			}
			REBIRTH.remove(entity.getUUID());
			DeathsDoor.saved(entity, true);
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
		if (!cast.alive()) return;
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
				Vec3 head = from.add(dir.scale(b));
				// The lance: a white-hot core in a halo of frost, its trail hanging behind it, shards glancing off its tip.
				if (b > LANCE_START + 0.1) {
					Light.ray(level, from.add(dir.scale(Math.max(LANCE_START, a - 0.5))), head, 0xE8FAFF, 0.14, 6);
					Light.ray(level, from.add(dir.scale(Math.max(LANCE_START, a - 2))), head, ElementFx.FROST.primary(), 0.3, 12);
				}
				ElementFx.ring(level, head, dir, ElementFx.FROST.secondary(), 0.1, 0.8, 0.04, 6);
				ElementFx.shards(level, head, 0.7, 2);
				Vfx.emit(level, ParticleTypes.SNOWFLAKE, head, 4, 0.2, 0.02);
				Fx.sound(level, from.add(dir.scale(b)), SoundEvents.GLASS_HIT, 0.6F, 1.6F);
			});
		}
		// It hangs in the air, frozen, and bursts into frost where it ends.
		Scheduler.later(flight, () -> {
			if (length > LANCE_START + 0.5) {
				Light.ray(level, from.add(dir.scale(LANCE_START)), end, 0xCFF4FF, 0.07, 12);
			}
			ElementFx.frostImpact(level, end, 1.5);
		});
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
			if (!cast.alive()) {
				return;
			}
			// Lightning runs the whole length of the frozen lance, and it shatters.
			if (length > LANCE_START + 0.5) {
				ElementFx.bolt(level, from.add(dir.scale(LANCE_START)), end, 0.06, 2, 3);
			}
			for (int k = 1; k <= 4; k++) {
				Vec3 p = from.add(dir.scale(length * k / 5));
				ElementFx.shatterRing(level, p, 1.2);
				Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), p, 5, 0.15);
			}
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

	/** How far out along the aim Glacial Lance's light begins: clear of the caster's own view. */
	private static final double LANCE_START = 1.2;

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
				// A small sun: a white-gold core in an orange bloom, rings of fire turning round it and a shaft of
				// heat reaching down to where it will land.
				Light.orb(level, sun, 0xFFE070, r * 0.55, 3);
				Vfx.emit(level, SigilOption.glow(0xFF8A30, (float) (r * 3.2)), sun, 1, 0.0, 0.0);
				Light.ring(level, sun, ElementFx.tilted(1.1, tick * 0.3), 0xFF9A40, r * 1.15, r * 1.15, 0.06, 3);
				Light.ring(level, sun, ElementFx.tilted(0.5, -tick * 0.4), 0xFFD060, r * 1.35, r * 1.35, 0.04, 3);
				if (tick % 4 == 0) {
					Light.ray(level, sun, point.add(0, 0.2, 0), 0xFF8A30, 0.08 + 0.12 * k, 5);
				}
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
			ScreenFx.shake(level, c, 1.0F, 48);
			Effects.explode(cast, c, 7.0, power * 2.2);
			Sigils.flash(level, c, 0xFFFFD080, 3.0F);
			Vfx.shockwave(level, point, 8.0, Vfx.theme("fire"), 8);
			// A pillar of fire out of the blast (not if it lands on the caster), rings of it climbing, and a
			// great burst of flame slashes.
			if (cast.caster.position().distanceTo(point) > 3) {
				Light.ray(level, point, point.add(0, 12, 0), ElementFx.FIRE.primary(), 1.1, 16);
				Light.ray(level, point, point.add(0, 12, 0), ElementFx.FIRE.secondary(), 0.4, 14);
			}
			for (int i = 0; i < 4; i++) {
				int k = i;
				Scheduler.later(1 + i * 2, () -> Light.ring(level, point.add(0, 1 + k * 2.5, 0), new Vec3(0, 1, 0), k % 2 == 0 ? ElementFx.FIRE.primary()
					: ElementFx.FIRE.secondary(), 1.0, 3.6 - k * 0.5, 0.1, 10));
			}
			ElementFx.flameBurst(level, c, 3.5, 8);
			Vfx.radial(level, ParticleTypes.FLAME, c, 36, 0.5);
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
					for (int i = 0; i < 14; i++) {
						double a = level.getRandom().nextDouble() * Math.PI * 2;
						double rr = Math.sqrt(level.getRandom().nextDouble()) * 5.5;
						Vfx.emit(level, ParticleTypes.FLAME, point.add(Math.cos(a) * rr, 0.15, Math.sin(a) * rr), 1, 0.05, 0.02);
					}
					// The burning ground: a ring of fire round it and flame tongues leaping up inside.
					ElementFx.groundRing(level, point, ElementFx.FIRE.primary(), 5.0, 5.5, 0.1, 20);
					for (int i = 0; i < 5; i++) {
						double a = level.getRandom().nextDouble() * Math.PI * 2;
						double rr = Math.sqrt(level.getRandom().nextDouble()) * 5.0;
						ElementFx.flames(level, point.add(Math.cos(a) * rr, 0, Math.sin(a) * rr), 0.3, 1.0, 1);
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
				// One crescent of light cut out to the horizon: the circle through its tip, reach ahead, and its
				// two ends, half either side and 2.2 blocks further back. White at the edge, crimson behind.
				double radius = (half * half + 2.2 * 2.2) / (2 * 2.2);
				double span = 2 * Math.asin(Math.min(1, half / radius));
				Vec3 centre = eye.add(fwd.scale(reach - radius));
				Light.slash(level, centre, new Vec3(0, 1, 0), fwd, 0xFFFFFF, radius, span, 0.3 + tick * 0.06, 1, 5);
				Light.slash(level, centre.subtract(fwd.scale(0.35)), new Vec3(0, 1, 0), fwd, 0xF0C0D0, radius, span * 0.97, 0.18, 1, 6);
				Light.slash(level, centre.subtract(fwd.scale(0.7)), new Vec3(0, 1, 0), fwd, 0xB01830, radius, span * 0.92, 0.12, 1, 7);
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
				// Three arms of petal light whirling round the caster, an inner and an outer crescent each.
				for (int arm = 0; arm < 3; arm++) {
					double a = tick * 0.2 + Math.PI * 2 * arm / 3;
					for (int ring = 0; ring < 2; ring++) {
						double r = ring == 0 ? 2.6 : 5.2;
						double y = Math.sin(tick * 0.15 + arm * 2 + ring) * 0.6 - 0.2;
						Light.slash(level, c.add(0, y, 0), ElementFx.tilted(0.15, a + ring), ElementFx.flatDir(a + ring * 0.8), ring == 0 ? 0xFFB0DC : 0xFFE0F0,
							r, ring == 0 ? 1.1 : 0.8, 0.1, 1, 4);
					}
				}
				// And petals along the arms.
				for (int i = 0; i < 24; i += 2) {
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
							Vec3 hit = target.getBoundingBox().getCenter();
							Vec3 bulge = ElementFx.flatDir(level.getRandom().nextDouble() * Math.PI * 2).add(0, 0.6, 0).normalize();
							ElementFx.slash(level, hit.subtract(bulge.scale(0.7)), bulge.cross(new Vec3(0, 1, 0)), bulge, 0xF080C0, 0.7, 1.8, 0.1, 1, 5);
							Vfx.emit(level, dust(0xE060A0, 1.0F), hit, 3, 0.3, 0.0);
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
				// And a bolt out of the sky onto each one as you arrive.
				ElementFx.bolt(level, c.add(0.4, 7, -0.3), c, 0.07, 2, 2);
				ElementFx.stormImpact(level, c, 1.2);
				ElementFx.groundRing(level, target.position(), ElementFx.STORM.primary(), 0.3, 2.4, 0.08, 8);
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
					ElementFx.orb(level, p, ElementFx.dark(ElementFx.VOID.accent()), 0.35, 2);
					ElementFx.ring(level, p, ElementFx.tilted(1.2, tick * 0.5), ElementFx.VOID.primary(), 0.5, 0.5, 0.025, 2);
					Vfx.emit(level, ParticleTypes.PORTAL, p, 3, 0.1, 0.5);
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
				// The black star: a hole in the world, rings of violet light turning round it, light spiralling in,
				// and every so often darkness falling in on it from the edge of its reach.
				ElementFx.orb(level, stop, ElementFx.dark(ElementFx.VOID.accent()), 0.9, 3);
				for (int ring = 0; ring < 3; ring++) {
					double r = 2.0 + ring * 1.6;
					Light.ring(level, stop, ElementFx.tilted(0.25 + ring * 0.08, spin * (0.6 - ring * 0.15)), ring == 0 ? 0xE0B0FF : 0x9A5AF0, r, r, 0.05 - ring * 0.01, 3);
				}
				for (int k = 0; k < 2; k++) {
					Light.slash(level, stop, ElementFx.tilted(0.25, spin * 0.6), ElementFx.flatDir(spin * 1.6 + k * Math.PI), k == 0 ? 0x9A5AF0 : 0xE0B0FF,
						3.2 - (tick % 10) * 0.2, 1.6, 0.08, 2, 4);
				}
				if (tick % 6 == 0) {
					Light.ring(level, stop, new Vec3(0, 1, 0), ElementFx.dark(ElementFx.VOID.accent()), 9, 1, 0.14, 12);
					Light.ring(level, stop, new Vec3(0, 1, 0), 0x9A5AF0, 9.5, 1.2, 0.04, 11);
				}
				Vfx.emit(level, ParticleTypes.PORTAL, stop, 8, 0.1, 4.0);
				for (LivingEntity t2 : enemiesNear(pulse, stop, 9)) {
					// A boss is struck but never held at the star (bosses are only ever slowed, as the fused Singularity and Vortex keep to).
					if (!Spirits.isBoss(t2)) {
						Vec3 pull = stop.subtract(t2.getBoundingBox().getCenter());
						double d = Math.max(0.5, pull.length());
						Effects.push(t2, pull.normalize().scale(Math.min(0.55, 0.12 + 0.9 / d)).subtract(t2.getDeltaMovement().scale(0.4)));
						Reactions.mark(t2, Reactions.Mark.PULLED);
					}
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
			// It collapses: a last black core, and shells of violet light bursting out through the blast.
			ElementFx.blackCore(level, stop, 0.9, 8);
			for (int i = 0; i < 3; i++) {
				Light.ring(level, stop, ElementFx.tilted(i == 0 ? 0 : 1.2, i * Math.PI * 2 / 3), i == 1 ? 0xE0B0FF : 0x9A5AF0, 0.5, 7.0, 0.1, 12);
			}
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, stop, 30, 0.7);
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
		// Time stops: a white shockwave races out to the edge, and the whole circle becomes a clock face, the
		// hours marked round its rim, its hands sweeping round it once over the stop.
		Sigils.flash(level, c.add(0, 1, 0), 0xFFFFFF, 3.0F);
		Light.groundRing(level, c, 0xFFFFFF, 0.5, 20, 0.14, 16);
		Light.groundRing(level, c, 0xF2D98A, 0.4, 19, 0.08, 20);
		Vec3 face = c.add(0, 0.12, 0);
		for (int h = 0; h < 12; h++) {
			double a = Math.PI * 2 * h / 12;
			Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
			Light.ray(level, face.add(out.scale(h % 3 == 0 ? 16.5 : 17.5)), face.add(out.scale(19.5)), h % 3 == 0 ? 0xFFFFFF : 0xF2D98A, h % 3 == 0 ? 0.16 : 0.1,
				ticks);
		}
		Light.slash(level, face.add(0, 0.03, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1), 0xFFF8E0, 18.5, Math.PI * 1.96, 0.35, ticks, ticks + 10);
		Light.slash(level, face.add(0, 0.06, 0), new Vec3(0, 1, 0), new Vec3(1, 0, 0), 0xC8962E, 11, Math.PI / 6, 0.5, ticks, ticks + 10);
		for (Entity e : level.getEntities(caster, new AABB(c, c).inflate(20), e -> e instanceof LivingEntity && e.isAlive() && e != caster)) {
			LivingEntity t = (LivingEntity) e;
			if (t.distanceTo(caster) <= 20 && Targets.canHarm(caster, t)) {
				Wards.stasis(cast, t, ticks);
			}
		}
		// Time's shell: a slow golden dome that fades as time returns.
		for (int t = 0; t < ticks; t += 10) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Light.groundRing(level, c.add(0, 0.2, 0), 0xF2D98A, 20, 20, 0.12, 12);
				for (int k = 0; k < 16; k++) {
					double a = Math.PI * 2 * k / 16 + tick * 0.01;
					Fx.sendFar(level, dust(0xF2D98A, 2.2F), c.add(Math.cos(a) * 20, 0.3, Math.sin(a) * 20));
				}
			});
		}
	}

	/** For a minute, death burns you back to life. */
	private static void rebirth(Cast cast, double power) {
		LivingEntity caster = cast.caster;
		int resting = DeathsDoor.restingFromRebirth(caster);
		if (resting > 0) {
			// Recast every few seconds it would never let its caster die: it rests after it burns (see DeathsDoor).
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.rebirth_resting",
				"Too soon to be reborn again (%s s)", resting).withColor(0xFF7040));
			return;
		}
		long until = cast.level.getGameTime() + Math.round(1200 * cast.duration);
		REBIRTH.put(caster.getUUID(), until);
		REBIRTH_POWER.put(caster.getUUID(), power);
		Vec3 c = caster.position();
		Sigils.ground(cast.level, c, 0xFF7040, 0xFFD070, 1.8F, 40);
		for (int t = 0; t < 20; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 fwd = flat(caster.getLookAngle());
				Vec3 back = caster.position().add(0, 1.3, 0).subtract(fwd.scale(0.3));
				Vec3 side = fwd.cross(new Vec3(0, 1, 0));
				double lift = tick * 0.02;
				// Wings of flame: three feathers of fire a side, spreading and lifting.
				for (int s = -1; s <= 1; s += 2) {
					for (int f = 0; f < 3; f++) {
						Vec3 toward = side.scale(s).add(0, 0.5 - f * 0.35 + lift, 0);
						ElementFx.slash(cast.level, back.add(side.scale(s * 0.2)), fwd, toward, f == 0 ? ElementFx.FIRE.secondary() : ElementFx.FIRE.primary(),
							0.7 + f * 0.35, 0.8, 0.12 - f * 0.02, 2, 5);
					}
					dot(cast.level, ParticleTypes.FLAME, back.add(side.scale(s * 1.2)).add(0, 0.5 + lift, 0));
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
		// Burning back to life: a great heat flare, flame slashes whirling out, fire licking up round you, rings of
		// it climbing and columns of it rising all round.
		ElementFx.heatFlare(level, c, 3.0);
		ElementFx.flameBurst(level, c, 2.2, 8);
		ElementFx.flames(level, entity.position(), 0.8, 2.6, 8);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 3, () -> ElementFx.ring(level, entity.position().add(0, 0.3 + k * 0.9, 0), new Vec3(0, 1, 0), k == 1 ? ElementFx.FIRE.secondary()
				: ElementFx.FIRE.primary(), 0.4, 3.5 - k * 0.6, 0.08, 10));
		}
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 * i / 5;
			Vec3 foot = entity.position().add(Math.cos(a) * 1.4, 0.05, Math.sin(a) * 1.4);
			ElementFx.ray(level, foot, foot.add(0, 2.8, 0), i % 2 == 0 ? ElementFx.FIRE.primary() : ElementFx.FIRE.secondary(), 0.12, 14);
		}
		Vfx.radial(level, ParticleTypes.FLAME, c, 36, 0.45);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 24, 0.5);
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
				Vfx.emit(level, stone, p.add(0, 0.5, 0), 8, 0.4, 0.15);
				Vfx.emit(level, deep, p.add(0, 0.2, 0), 4, 0.6, 0.1);
				// The ground splits open along the line, cracking round every spire.
				Vec3 last = CastEngine.ground(level, base.add(fwd.scale(step - 1)).add(0, 2, 0));
				ElementFx.ray(level, last.add(0, 0.1, 0), p.add(0, 0.1, 0), ElementFx.EARTH.secondary(), 0.12, 24);
				ElementFx.groundRing(level, p, ElementFx.EARTH.primary(), 0.3, 1.6, 0.08, 10);
				if (step % 2 == 0) {
					ElementFx.flatSigil(level, p, SigilOption.CRACKED, ElementFx.EARTH.secondary(), 1.3, 30, 0.0);
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
		Light.ray(level, from.add(dir.scale(0.06)), end, 0xE8F0FF, 0.05, 16);
		ElementFx.starSeal(level, end.add(0, 0.2, 0), new Vec3(0, 1, 0), 1.2, 60);
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
