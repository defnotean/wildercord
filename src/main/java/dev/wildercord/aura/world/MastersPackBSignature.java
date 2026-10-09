package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One owned, stationary Starlit, Hourglass or Crimson signature. The master stays planted for the whole form; every
 * beat is drawn before it lands and no challenger takes more than {@link MastersPackB#CHAIN_CAP} from one form.
 */
final class MastersPackBSignature {
	private static final int SPENT = 0xF2D98A;
	private final SwordMaster master;
	private final ServerLevel level;
	private final MastersRules.Move kind;
	private final long began;
	private final Vec3 origin;
	private final Set<UUID> roster;
	private final List<Vec3> stars;
	private final Map<UUID, Double> dealt = new HashMap<>();
	private float yaw;
	private Vec3 aim;
	private int beat, landed;
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private MastersPackBSignature(SwordMaster master, ServerLevel level, MastersRules.Move kind, long now, float yaw, List<Vec3> stars) {
		this.master = master; this.level = level; this.kind = kind; began = now; origin = master.position(); this.yaw = yaw;
		roster = master.challengers(); this.stars = List.copyOf(stars); aim = look(yaw);
	}

	static MastersPackBSignature prepare(SwordMaster master, ServerPlayer target, int school, int sequence, double aura, long now, long readyAt) {
		if (!MastersPackB.owns(school) || !(master.level() instanceof ServerLevel level) || !master.canBeginKiln(now)
			|| target == null || !master.canHarmParticipant(target) || !master.hasLineOfSight(target) || !master.onGround()) return null;
		double distance = master.distanceTo(target), height = target.getY() - master.getY();
		boolean eligible = switch (school) {
			case MastersPackB.STARLIT -> StarlitConstellationRules.eligible(school, sequence, distance, height, aura, now, readyAt);
			case MastersPackB.HOURGLASS -> HourglassRewindRules.eligible(school, sequence, distance, height, aura, now, readyAt);
			default -> CrimsonFrenzyRules.eligible(school, sequence, distance, height, aura, master.getHealth(), master.getMaxHealth(), now, readyAt);
		};
		if (!eligible) return null;
		Vec3 toward = target.position().subtract(master.position());
		float yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
		List<Vec3> stars = new ArrayList<>();
		if (school == MastersPackB.STARLIT) {
			Vec3 forward = look(yaw), right = new Vec3(-forward.z, 0, forward.x);
			for (int i = 0; i < StarlitConstellationRules.STARS; i++) {
				double[] offset = StarlitConstellationRules.offset(i);
				Vec3 star = target.position().add(forward.scale(offset[0])).add(right.scale(offset[1]));
				double r = StarlitConstellationRules.RADIUS;
				if (!master.pursuitInsideArena(new AABB(star.x - r, star.y, star.z - r, star.x + r, star.y + 1, star.z + r))) return null;
				stars.add(star);
			}
		}
		return new MastersPackBSignature(master, level, MastersPackB.signature(school), now, yaw, stars);
	}

	MastersRules.Move kind() { return kind; }

	boolean tick(long now) {
		if (finished) return false;
		if (!validOwner() || now != level.getGameTime() || now < began || now > began + kind.tell
			|| (lastTick == Long.MIN_VALUE ? now != began : now < lastTick || now > lastTick + 1)) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		int age = (int) (now - began);
		if (kind == MastersRules.Move.CRIMSON_FRENZY && age == 0) {
			// The price is paid up front and is never the blow that ends the trial.
			double price = CrimsonFrenzyRules.price(master.getMaxHealth());
			master.setHealth((float) Math.max(1, master.getHealth() - price));
		}
		track(age);
		hold();
		return switch (kind) {
			case STARLIT_CONSTELLATION -> starlit(age);
			case HOURGLASS_REWIND -> hourglass(age);
			default -> crimson(age);
		};
	}

	/** Hourglass and Crimson follow the target until their lock; Starlit's stars are placed at admission. */
	private void track(int age) {
		int impact = kind == MastersRules.Move.HOURGLASS_REWIND ? HourglassRewindRules.STRIKE
			: kind == MastersRules.Move.CRIMSON_FRENZY && beat < CrimsonFrenzyRules.BEATS.length ? CrimsonFrenzyRules.BEATS[beat] : -1;
		int lock = kind == MastersRules.Move.HOURGLASS_REWIND ? HourglassRewindRules.LOCK : CrimsonFrenzyRules.LOCK;
		if (impact < 0 || age >= impact - lock || !(master.getTarget() instanceof ServerPlayer target) || !roster.contains(target.getUUID())) return;
		Vec3 toward = target.position().subtract(origin);
		if (toward.horizontalDistanceSqr() < 1e-4) return;
		yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
		aim = look(yaw);
	}

	private void hold() {
		master.getNavigation().stop(); master.getMoveControl().setWait(); master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x + aim.x * 8, master.getEyeY(), origin.z + aim.z * 8);
	}

	private boolean starlit(int age) {
		while (beat < StarlitConstellationRules.STARS && age >= StarlitConstellationRules.burst(beat)) {
			Vec3 star = stars.get(beat);
			beat++;
			Light.ray(level, star, star.add(0, StarlitConstellationRules.HIGH, 0), master.auraColor(), .35, 6);
			ring(star, master.auraColor(), .12, 6);
			Feels.sound(level, star, "aura_slash", 1, 1.4F + beat * .1F);
			swing();
			strike(player -> {
				Vec3 d = player.position().subtract(star);
				return StarlitConstellationRules.hits(d.x, d.z, d.y);
			}, StarlitConstellationRules.DAMAGE, star.add(0, 1, 0));
		}
		if (beat >= StarlitConstellationRules.STARS) return done();
		if (age % StarlitConstellationRules.WARNING_REFRESH == 0) {
			int life = StarlitConstellationRules.WARNING_REFRESH + 1;
			for (int i = beat; i < stars.size(); i++) {
				ring(stars.get(i), i == beat ? 0xFFFFFF : 0x9DB7FF, i == beat ? .09 : .05, life);
				if (i + 1 < stars.size()) Light.ray(level, stars.get(i).add(0, .15, 0), stars.get(i + 1).add(0, .15, 0), 0x9DB7FF, .03, life);
			}
		}
		return true;
	}

	private boolean hourglass(int age) {
		if (beat == 0 && age >= HourglassRewindRules.STRIKE || beat == 1 && age >= HourglassRewindRules.REPLAY) {
			boolean replay = beat++ == 1;
			lane(HourglassRewindRules.REACH, HourglassRewindRules.HALF_WIDTH, master.auraColor(), .12, 5);
			Feels.sound(level, origin, "aura_slash", 1, replay ? .6F : 1.1F);
			swing();
			strike(player -> {
				double[] f = frame(player);
				return HourglassRewindRules.hits(f[0], f[1], f[2]);
			}, HourglassRewindRules.DAMAGE, origin.add(0, 1, 0));
			if (replay) return done();
			return true;
		}
		if (age % HourglassRewindRules.WARNING_REFRESH == 0) {
			int life = HourglassRewindRules.WARNING_REFRESH + 1;
			if (beat == 0 && age >= HourglassRewindRules.STRIKE - HourglassRewindRules.LOCK)
				lane(HourglassRewindRules.REACH, HourglassRewindRules.HALF_WIDTH, 0xE0C060, .06, life);
			// The spent lane stays gold until it replays: the first cut's ground is not safe to step back into.
			if (beat == 1) lane(HourglassRewindRules.REACH, HourglassRewindRules.HALF_WIDTH, SPENT, .07, life);
		}
		return true;
	}

	private boolean crimson(int age) {
		if (beat < CrimsonFrenzyRules.BEATS.length && age == CrimsonFrenzyRules.BEATS[beat] - CrimsonFrenzyRules.LOCK) {
			AuraFx.banner(master, Component.translatable("boss.wildercord.master.crimson_frenzy"),
				Component.translatable(MastersPackB.answer(CrimsonFrenzyRules.SHAPES[beat])), master.auraColor(), AuraFxRules.BannerKind.ART);
		}
		if (beat < CrimsonFrenzyRules.BEATS.length && age >= CrimsonFrenzyRules.BEATS[beat]) {
			int current = beat++;
			shape(current, master.auraColor(), .12, 5);
			Feels.sound(level, origin, "aura_slash", 1, .8F + current * .1F);
			swing();
			float lost = strike(player -> {
				double[] f = frame(player);
				return CrimsonFrenzyRules.hits(current, f[0], f[1], f[2], player.isCrouching());
			}, CrimsonFrenzyRules.DAMAGE, origin.add(0, 1, 0));
			if (lost > 0) {
				landed++;
				double heal = CrimsonFrenzyRules.heal(master.getMaxHealth());
				Effects.withSource(master, () -> master.heal((float) heal));
			}
		}
		if (beat >= CrimsonFrenzyRules.BEATS.length) return done();
		if (age % CrimsonFrenzyRules.WARNING_REFRESH == 0 && age >= CrimsonFrenzyRules.BEATS[beat] - CrimsonFrenzyRules.LOCK)
			shape(beat, 0xB0303A, .06, CrimsonFrenzyRules.WARNING_REFRESH + 1);
		return true;
	}

	private float strike(java.util.function.Predicate<ServerPlayer> inside, double damage, Vec3 from) {
		// Snapshot before any reaction so a callback cannot drag a safe challenger into the resolving beat.
		List<ServerPlayer> victims = roster.stream().sorted(Comparator.naturalOrder())
			.map(id -> level.getPlayerByUUID(id) instanceof ServerPlayer p ? p : null)
			.filter(p -> p != null && master.canHarmParticipant(p) && inside.test(p)
				&& EmberAfterburn.clear(level, master, from, p.position().add(0, .35, 0))).toList();
		float total = 0;
		for (ServerPlayer player : victims) {
			if (!master.canMaintainAfterburn() || !master.canHarmParticipant(player)) continue;
			double share = MastersPackB.capped(dealt.getOrDefault(player.getUUID(), 0.0), damage);
			if (share <= 0) continue;
			dealt.merge(player.getUUID(), share, Double::sum);
			total += Effects.withSource(master, () -> master.projected(player, share));
		}
		return total;
	}

	/** {forward along the locked aim, side across it, height} from the planted origin. */
	private double[] frame(ServerPlayer player) {
		Vec3 d = player.position().subtract(origin);
		return new double[] {d.x * aim.x + d.z * aim.z, d.x * -aim.z + d.z * aim.x, d.y};
	}

	private boolean done() { finished = true; released = true; return false; }
	private void swing() { master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true); }

	private boolean validOwner() {
		return master.canMaintainPackB(this) && master.level() == level && roster.equals(master.challengers())
			&& master.position().distanceToSqr(origin) <= .0025;
	}

	boolean released() { return released; }
	/** Crimson stays open longer when none of its four beats landed. */
	int recovery() {
		return kind == MastersRules.Move.CRIMSON_FRENZY ? CrimsonFrenzyRules.recovery(landed) : kind.recovery;
	}
	long endsAt() { return began + kind.tell + kind.recovery; }
	void stop() { finished = true; }
	private boolean cancel() { stop(); return false; }

	private static Vec3 look(float yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private void ring(Vec3 center, int color, double width, int life) {
		double r = StarlitConstellationRules.RADIUS;
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8, b = Math.PI * 2 * (i + 1) / 8;
			Light.ray(level, center.add(Math.cos(a) * r, .12, Math.sin(a) * r), center.add(Math.cos(b) * r, .12, Math.sin(b) * r), color, width, life);
		}
	}

	private void lane(double reach, double half, int color, double width, int life) {
		Vec3 side = new Vec3(-aim.z, 0, aim.x).scale(half), base = origin.add(0, .12, 0), tip = base.add(aim.scale(reach));
		Light.ray(level, base.add(side), tip.add(side), color, width, life);
		Light.ray(level, base.subtract(side), tip.subtract(side), color, width, life);
		Light.ray(level, tip.add(side), tip.subtract(side), color, width, life);
	}

	private void shape(int index, int color, double width, int life) {
		double reach = CrimsonFrenzyRules.REACH[index];
		switch (CrimsonFrenzyRules.SHAPES[index]) {
			case LANE -> lane(reach, CrimsonFrenzyRules.LANE_WIDTH, color, width, life);
			case CIRCLE -> {
				for (int i = 0; i < 12; i++) {
					double a = Math.PI * 2 * i / 12, b = Math.PI * 2 * (i + 1) / 12;
					Light.ray(level, origin.add(Math.cos(a) * reach, .12, Math.sin(a) * reach),
						origin.add(Math.cos(b) * reach, .12, Math.sin(b) * reach), color, width, life);
				}
			}
			default -> {
				// Low arcs are drawn at the shins, high arcs at the head: the line's height is the answer.
				double y = CrimsonFrenzyRules.SHAPES[index] == MasterTechniques.Shape.ARC_HIGH ? 1.6 : .3;
				Vec3 side = new Vec3(-aim.z, 0, aim.x);
				Vec3 previous = null;
				for (int i = -3; i <= 3; i++) {
					double angle = i * Math.toRadians(18);
					Vec3 dir = aim.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
					Vec3 point = origin.add(dir.scale(reach)).add(0, y, 0);
					if (previous != null) Light.ray(level, previous, point, color, width, life);
					previous = point;
				}
			}
		}
	}
}
