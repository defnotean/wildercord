package dev.wildercord.aura.world;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One owned, stationary Tide, Iron or Dune signature (see {@link MethodsASignatureRules}). The master stays planted for
 * the whole form, every beat is drawn before it lands, and no challenger takes more than {@link MethodsAMasters#CHAIN_CAP}
 * from one form. Each beat adds to a {@link MethodsASignatureRules.Receipt} the master keeps after the form ends.
 */
final class MethodsASignature {
	private static final int WARN = 0xE8E4D0, SAFE = 0xF4EEDC;
	private final SwordMaster master;
	private final ServerLevel level;
	private final MastersRules.Move kind;
	private final long began;
	private final Vec3 origin;
	private final Set<UUID> roster;
	private final Map<UUID, Double> dealt = new HashMap<>();
	private float yaw;
	private Vec3 aim, spot;
	private int beat;
	private MethodsASignatureRules.Receipt receipt;
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private MethodsASignature(SwordMaster master, ServerLevel level, MastersRules.Move kind, long now, float yaw, Vec3 spot) {
		this.master = master; this.level = level; this.kind = kind; began = now; origin = master.position(); this.yaw = yaw;
		roster = master.challengers(); aim = look(yaw); this.spot = spot;
		receipt = new MethodsASignatureRules.Receipt(kind, 0, 0, 0);
	}

	static MethodsASignature prepare(SwordMaster master, ServerPlayer target, int school, int sequence, double aura, long now, long readyAt) {
		if (!MethodsAMasters.owns(school) || !(master.level() instanceof ServerLevel level) || !master.canBeginKiln(now)
			|| target == null || !master.canHarmParticipant(target) || !master.hasLineOfSight(target) || !master.onGround()) return null;
		double distance = master.distanceTo(target), height = target.getY() - master.getY();
		if (!MethodsASignatureRules.eligible(school, sequence, distance, height, aura, now, readyAt)) return null;
		Vec3 toward = target.position().subtract(master.position());
		float yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
		return new MethodsASignature(master, level, MethodsAMasters.signature(school), now, yaw, target.position());
	}

	MastersRules.Move kind() { return kind; }
	MethodsASignatureRules.Receipt receipt() { return receipt; }

	boolean tick(long now) {
		if (finished) return false;
		if (!validOwner() || now != level.getGameTime() || now < began || now > began + kind.tell
			|| (lastTick == Long.MIN_VALUE ? now != began : now < lastTick || now > lastTick + 1)) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		int age = (int) (now - began);
		track(age);
		hold();
		int[] beats = MethodsASignatureRules.beats(kind);
		if (beat < beats.length && age >= beats[beat]) {
			int current = beat++;
			land(current);
			if (beat >= beats.length) return done();
			return true;
		}
		if (age % MethodsASignatureRules.WARNING_REFRESH == 0) warn(age, MethodsASignatureRules.WARNING_REFRESH + 1);
		return true;
	}

	/** Iron's hammer spot and Dune's lane follow the target until their lock; Tide's rings centre on the master. */
	private void track(int age) {
		int impact = kind == MastersRules.Move.IRON_ANVIL_VERDICT ? MethodsASignatureRules.HAMMER
			: kind == MastersRules.Move.DUNE_SHIFTING_SANDS ? MethodsASignatureRules.LANE : -1;
		int lock = kind == MastersRules.Move.IRON_ANVIL_VERDICT ? MethodsASignatureRules.HAMMER_LOCK : MethodsASignatureRules.LANE_LOCK;
		if (impact < 0 || beat > 0 || age >= impact - lock || !(master.getTarget() instanceof ServerPlayer target)
			|| !roster.contains(target.getUUID())) return;
		Vec3 toward = target.position().subtract(origin);
		if (toward.horizontalDistanceSqr() < 1e-4) return;
		yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
		aim = look(yaw);
		spot = target.position();
	}

	private void hold() {
		master.getNavigation().stop(); master.getMoveControl().setWait(); master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x + aim.x * 8, master.getEyeY(), origin.z + aim.z * 8);
	}

	private void land(int index) {
		int color = master.auraColor();
		swing();
		switch (kind) {
			case TIDE_UNDERTOW_RING -> {
				if (index == 0) {
					ring(origin, MethodsASignatureRules.WAVE_INNER, color, .10, 5);
					ring(origin, MethodsASignatureRules.WAVE_OUTER, color, .14, 5);
				} else ring(origin, MethodsASignatureRules.UNDERTOW_RADIUS, color, .14, 5);
				Feels.sound(level, origin, "aura_tide_impact", 1, index == 0 ? 1.0F : .7F);
			}
			case IRON_ANVIL_VERDICT -> {
				if (index == 0) {
					ring(spot, MethodsASignatureRules.HAMMER_RADIUS, color, .16, 5);
					Light.ray(level, spot.add(0, 4, 0), spot, color, .3, 5);
				} else ring(origin, MethodsASignatureRules.SHOCK_RADIUS, color, .12, 5);
				Feels.sound(level, index == 0 ? spot : origin, "aura_iron_impact", 1, index == 0 ? .9F : .6F);
			}
			default -> {
				if (index == 0) lane(color, .14, 5);
				else ring(origin, MethodsASignatureRules.STORM_RADIUS, color, .12, 5);
				Feels.sound(level, origin, "aura_dune_impact", 1, index == 0 ? 1.1F : .8F);
			}
		}
		Vec3 from = kind == MastersRules.Move.IRON_ANVIL_VERDICT && index == 0 ? spot.add(0, 1, 0) : origin.add(0, 1, 0);
		// Snapshot before any reaction so a callback cannot drag a safe challenger into the resolving beat.
		List<ServerPlayer> present = new ArrayList<>(), victims = new ArrayList<>();
		roster.stream().sorted(Comparator.naturalOrder())
			.map(id -> level.getPlayerByUUID(id) instanceof ServerPlayer p ? p : null)
			.filter(p -> p != null && master.canHarmParticipant(p))
			.forEach(p -> {
				present.add(p);
				double[] f = frame(p, index);
				if (MethodsASignatureRules.hits(kind, index, f[0], f[1], f[2], p.onGround())
					&& EmberAfterburn.clear(level, master, from, p.position().add(0, .35, 0))) victims.add(p);
			});
		int hit = 0;
		for (ServerPlayer player : victims) {
			if (!master.canMaintainAfterburn() || !master.canHarmParticipant(player)) continue;
			double share = MethodsAMasters.capped(dealt.getOrDefault(player.getUUID(), 0.0), MethodsASignatureRules.DAMAGE);
			if (share <= 0) continue;
			dealt.merge(player.getUUID(), share, Double::sum);
			float lost = Effects.withSource(master, () -> master.projected(player, share));
			if (lost > 0) hit++;
			if (lost > 0 && kind == MastersRules.Move.DUNE_SHIFTING_SANDS && index == 0) {
				int blind = MethodsASignatureRules.blind(true, false);
				if (blind > 0) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind, 0, false, true), master);
			}
		}
		receipt = receipt.beat(hit, Math.max(0, present.size() - hit));
	}

	/** Draws the next beat's ground, and once a beat is spent, Dune's pale lane that is now safe. */
	private void warn(int age, int life) {
		switch (kind) {
			case TIDE_UNDERTOW_RING -> {
				if (beat == 0) {
					ring(origin, MethodsASignatureRules.WAVE_INNER, SAFE, .05, life);
					ring(origin, MethodsASignatureRules.WAVE_OUTER, WARN, .06, life);
				} else ring(origin, MethodsASignatureRules.UNDERTOW_RADIUS, WARN, .07, life);
			}
			case IRON_ANVIL_VERDICT -> {
				if (beat == 0) ring(spot, MethodsASignatureRules.HAMMER_RADIUS, age >= MethodsASignatureRules.HAMMER - MethodsASignatureRules.HAMMER_LOCK
					? 0xFFFFFF : WARN, .07, life);
				else ring(origin, MethodsASignatureRules.SHOCK_RADIUS, WARN, .06, life);
			}
			default -> {
				if (beat == 0) lane(WARN, .06, life);
				else {
					ring(origin, MethodsASignatureRules.STORM_RADIUS, WARN, .06, life);
					lane(SAFE, .09, life);
				}
			}
		}
	}

	/** {forward, side, height}. Iron's hammer is measured from its locked spot; everything else from the planted origin. */
	private double[] frame(ServerPlayer player, int index) {
		if (kind == MastersRules.Move.IRON_ANVIL_VERDICT && index == 0) {
			Vec3 d = player.position().subtract(spot);
			return new double[] {d.x, d.z, d.y};
		}
		Vec3 d = player.position().subtract(origin);
		if (kind == MastersRules.Move.IRON_ANVIL_VERDICT) return new double[] {d.x, d.z, d.y};
		return new double[] {d.x * aim.x + d.z * aim.z, d.x * -aim.z + d.z * aim.x, d.y};
	}

	private boolean done() { finished = true; released = true; return false; }
	private void swing() { master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true); }

	private boolean validOwner() {
		return master.canMaintainMethodsA(this) && master.level() == level && roster.equals(master.challengers())
			&& master.position().distanceToSqr(origin) <= .0025;
	}

	boolean released() { return released; }
	int recovery() { return kind.recovery; }
	long endsAt() { return began + kind.tell + kind.recovery; }
	void stop() { finished = true; }
	private boolean cancel() { stop(); return false; }

	private static Vec3 look(float yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private void ring(Vec3 center, double r, int color, double width, int life) {
		int n = Math.max(8, (int) Math.ceil(r * 4));
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n, b = Math.PI * 2 * (i + 1) / n;
			Light.ray(level, center.add(Math.cos(a) * r, .12, Math.sin(a) * r), center.add(Math.cos(b) * r, .12, Math.sin(b) * r), color, width, life);
		}
	}

	private void lane(int color, double width, int life) {
		Vec3 side = new Vec3(-aim.z, 0, aim.x).scale(MethodsASignatureRules.LANE_HALF_WIDTH), base = origin.add(0, .12, 0),
			tip = base.add(aim.scale(MethodsASignatureRules.LANE_REACH));
		Light.ray(level, base.add(side), tip.add(side), color, width, life);
		Light.ray(level, base.subtract(side), tip.subtract(side), color, width, life);
		Light.ray(level, tip.add(side), tip.subtract(side), color, width, life);
	}
}
