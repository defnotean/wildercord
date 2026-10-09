package dev.wildercord.aura.world;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One owned, stationary signature of the Rime, Thunder, Verdant or Hollow Master. Like the Kiln it is admitted only on flat,
 * supported ground with the whole roster present, holds the Master still, warns every hit area before it resolves and ends
 * on any break in continuity. Damage is finite: each challenger is capped per signature.
 */
final class ElementalSignature {
	private final SwordMaster master;
	private final ServerLevel level;
	private final MastersRules.Move move;
	private final int school;
	private final Vec3 origin;
	private final Vec3 centre;
	private final float yaw;
	private final long began;
	private final Set<UUID> roster;
	/** Rime: the offsets (x, z) from the origin marked for each pulse. */
	private final List<List<double[]>> marks = new ArrayList<>();
	/** Thunder: the conductor positions. */
	private final double[][] rods;
	private final Map<UUID, Double> taken = new HashMap<>();
	private final Set<UUID> struck = new HashSet<>();
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private ElementalSignature(SwordMaster master, ServerLevel level, int school, long now, Vec3 centre, float yaw, double[][] rods) {
		this.master = master; this.level = level; this.school = school; began = now; origin = master.position();
		this.centre = centre; this.yaw = yaw; this.rods = rods;
		move = ElementalMasters.signatureMove(school);
		roster = master.challengers();
		for (int pulse = 0; pulse < RimeLatticeRules.PULSES; pulse++) marks.add(new ArrayList<>());
	}

	static ElementalSignature prepare(SwordMaster master, ServerPlayer target, int school, int sequence, double aura, long now, long readyAt) {
		if (!ElementalMasters.owns(school) || !(master.level() instanceof ServerLevel level) || !master.canBeginSignature(now)
			|| !master.canHarmParticipant(target) || !master.hasLineOfSight(target) || !master.onGround() || !still(master)) return null;
		double distance = master.distanceTo(target), height = target.getY() - master.getY();
		boolean eligible = switch (school) {
			case ElementalMasters.RIME -> RimeLatticeRules.eligible(school, sequence, distance, height, aura, now, readyAt);
			case ElementalMasters.THUNDER -> ThunderChainRules.eligible(school, sequence, distance, height, aura, now, readyAt);
			case ElementalMasters.VERDANT -> VerdantBloomRules.eligible(school, sequence, distance, height, aura, now, readyAt);
			default -> HollowPullRules.eligible(school, sequence, distance, height, aura, now, readyAt);
		};
		if (!eligible || !supported(level, master.position())) return null;
		for (UUID id : master.challengers())
			if (!(level.getPlayerByUUID(id) instanceof ServerPlayer player) || !master.canHarmParticipant(player)) return null;
		Vec3 toward = target.position().subtract(master.position());
		float yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
		double[][] rods = school == ElementalMasters.THUNDER
			? ThunderChainRules.rods(target.getX(), target.getZ(), toward.x, toward.z) : new double[0][];
		return new ElementalSignature(master, level, school, now, target.position(), yaw, rods);
	}

	MastersRules.Move move() { return move; }
	boolean released() { return released; }
	long endsAt() { return began + ElementalMasters.end(move); }
	void stop() { finished = true; }
	private boolean cancel() { stop(); return false; }

	/** The age of the signature's last resolving event. */
	static int lastEvent(MastersRules.Move move) { return ElementalMasters.lastEvent(move); }

	boolean tick(long now) {
		if (finished) return false;
		int last = lastEvent(move);
		if (!validOwner() || now != level.getGameTime() || now < began || now > began + last
			|| (lastTick == Long.MIN_VALUE ? now != began : now < lastTick || now > lastTick + 1)) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		if (!supported(level, origin)) return cancel();
		hold();
		int age = (int) (now - began);
		switch (school) {
			case ElementalMasters.RIME -> rime(age);
			case ElementalMasters.THUNDER -> thunder(age);
			case ElementalMasters.VERDANT -> verdant(age);
			default -> hollow(age);
		}
		if (age >= last) {
			finished = true; released = true;
			return false;
		}
		return true;
	}

	private void hold() {
		master.getNavigation().stop(); master.getMoveControl().setWait(); master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x - Math.sin(Math.toRadians(yaw)) * 8, master.getEyeY(), origin.z + Math.cos(Math.toRadians(yaw)) * 8);
	}

	private List<ServerPlayer> challengers() {
		var result = new ArrayList<ServerPlayer>();
		roster.stream().sorted().forEach(id -> {
			if (level.getPlayerByUUID(id) instanceof ServerPlayer player && master.canHarmParticipant(player)) result.add(player);
		});
		return result;
	}

	private void strike(ServerPlayer player, double damage) {
		if (damage <= 0) return;
		Effects.withSource(master, () -> master.projected(player, damage));
		taken.merge(player.getUUID(), damage, Double::sum);
	}

	private void release(Vec3 at) {
		master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Feels.sound(level, at, "aura_slash", 1, .7F);
	}

	// ---- Rime: mark a plus under each challenger, freeze it twelve ticks later.
	private void rime(int age) {
		int marking = RimeLatticeRules.markingPulse(age);
		if (marking >= 0) for (ServerPlayer player : challengers())
			marks.get(marking).add(new double[] {player.getX() - origin.x, player.getZ() - origin.z});
		for (int pulse = 0; pulse < RimeLatticeRules.PULSES; pulse++) {
			int from = RimeLatticeRules.markAt(pulse), at = RimeLatticeRules.freezeAt(pulse);
			if (age >= from && age < at && (age - from) % RimeLatticeRules.WARNING_REFRESH == 0)
				for (double[] mark : marks.get(pulse)) drawPlus(mark, false, Math.min(RimeLatticeRules.WARNING_REFRESH + 1, at - age));
		}
		int freezing = RimeLatticeRules.freezingPulse(age);
		if (freezing < 0) return;
		release(origin);
		List<double[]> pulseMarks = marks.get(freezing);
		for (double[] mark : pulseMarks) drawPlus(mark, true, 5);
		for (ServerPlayer player : challengers()) {
			double x = player.getX() - origin.x, z = player.getZ() - origin.z, height = player.getY() - origin.y;
			boolean hit = pulseMarks.stream().anyMatch(mark -> RimeLatticeRules.hits(mark[0], mark[1], x, z, height));
			if (hit) strike(player, RimeLatticeRules.damage(taken.getOrDefault(player.getUUID(), 0.0)));
		}
	}

	private void drawPlus(double[] mark, boolean impact, int life) {
		int cx = RimeLatticeRules.cell(mark[0]), cz = RimeLatticeRules.cell(mark[1]);
		int[][] cells = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] cell : cells) {
			double x0 = (cx + cell[0]) * RimeLatticeRules.CELL, z0 = (cz + cell[1]) * RimeLatticeRules.CELL, s = RimeLatticeRules.CELL;
			Vec3 a = origin.add(x0, .1, z0), b = origin.add(x0 + s, .1, z0), c = origin.add(x0 + s, .1, z0 + s), d = origin.add(x0, .1, z0 + s);
			int color = impact ? master.auraColor() : 0xBFEFFF;
			double width = impact ? .1 : .05;
			Light.ray(level, a, b, color, width, life); Light.ray(level, b, c, color, width, life);
			Light.ray(level, c, d, color, width, life); Light.ray(level, d, a, color, width, life);
		}
	}

	// ---- Thunder: three conductors, struck rod by rod; each bolt chains through crowded challengers.
	private void thunder(int age) {
		if (age < ThunderChainRules.strikeAt(ThunderChainRules.RODS - 1) && age % ThunderChainRules.WARNING_REFRESH == 0) {
			for (int rod = 0; rod < rods.length; rod++) {
				if (age >= ThunderChainRules.strikeAt(rod)) continue;
				int life = (int) Math.min(ThunderChainRules.WARNING_REFRESH + 1, ThunderChainRules.strikeAt(rod) - age);
				drawRing(rodAt(rod), ThunderChainRules.ROD, 0xFFF6B0, .05, life);
				Light.ray(level, rodAt(rod), rodAt(rod).add(0, 2.2, 0), master.auraColor(), .06, life);
			}
		}
		int rod = ThunderChainRules.strikingRod(age);
		if (rod < 0) return;
		Vec3 at = rodAt(rod);
		Light.ray(level, at.add(0, 7, 0), at, master.auraColor(), .16, 5);
		release(at);
		List<ServerPlayer> players = challengers();
		int n = players.size();
		double[] xs = new double[n], zs = new double[n], heights = new double[n];
		boolean[] spent = new boolean[n];
		for (int i = 0; i < n; i++) {
			ServerPlayer player = players.get(i);
			xs[i] = player.getX(); zs[i] = player.getZ(); heights[i] = player.getY() - centre.y;
			spent[i] = struck.contains(player.getUUID());
		}
		boolean[] hit = ThunderChainRules.chain(at.x, at.z, xs, zs, heights, spent);
		for (int i = 0; i < n; i++) if (hit[i] && struck.add(players.get(i).getUUID())) {
			Light.ray(level, at.add(0, 1, 0), players.get(i).position().add(0, 1, 0), master.auraColor(), .08, 4);
			strike(players.get(i), ThunderChainRules.DAMAGE);
		}
	}

	private Vec3 rodAt(int rod) { return new Vec3(rods[rod][0], centre.y + .1, rods[rod][1]); }

	// ---- Verdant: a ring of petals around the challenger's spot opens, cutting and rooting whoever is inside.
	private void verdant(int age) {
		if (age < VerdantBloomRules.TELL) {
			if (age % VerdantBloomRules.WARNING_REFRESH == 0)
				drawRing(centre.add(0, .1, 0), VerdantBloomRules.RADIUS, master.auraColor(), .06,
					Math.min(VerdantBloomRules.WARNING_REFRESH + 1, VerdantBloomRules.TELL - age));
			return;
		}
		drawRing(centre.add(0, .1, 0), VerdantBloomRules.RADIUS, master.auraColor(), .12, 5);
		release(centre);
		for (ServerPlayer player : challengers()) {
			if (!VerdantBloomRules.hits(player.getX() - centre.x, player.getZ() - centre.z, player.getY() - centre.y)) continue;
			strike(player, VerdantBloomRules.DAMAGE);
			if (player.isAlive()) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, VerdantBloomRules.ROOT_TICKS,
				VerdantBloomRules.ROOT_AMPLIFIER, false, true), master);
		}
	}

	// ---- Hollow: a well tugs everyone in reach toward the Master; sprinting breaks its hold. Then the core collapses.
	private void hollow(int age) {
		if (age < HollowPullRules.TELL) {
			if (age % HollowPullRules.WARNING_REFRESH == 0) {
				int life = Math.min(HollowPullRules.WARNING_REFRESH + 1, HollowPullRules.TELL - age);
				drawRing(origin.add(0, .1, 0), HollowPullRules.CORE, master.auraColor(), .07, life);
				drawRing(origin.add(0, .1, 0), HollowPullRules.REACH, 0xE0B0FF, .04, life);
			}
			if (HollowPullRules.pulling(age)) for (ServerPlayer player : challengers()) {
				double[] tug = HollowPullRules.tug(player.getX() - origin.x, player.getZ() - origin.z, player.isSprinting());
				if (tug[0] == 0 && tug[1] == 0) continue;
				dev.wildercord.aura.MasterFormMovement.begin(player, 2);
				player.setDeltaMovement(player.getKnownMovement().add(tug[0], 0, tug[1]));
				player.needsSync = true;
				player.connection.send(new ClientboundSetEntityMotionPacket(player));
			}
			return;
		}
		drawRing(origin.add(0, .1, 0), HollowPullRules.CORE, master.auraColor(), .14, 5);
		release(origin);
		for (ServerPlayer player : challengers())
			if (HollowPullRules.hits(player.getX() - origin.x, player.getZ() - origin.z, player.getY() - origin.y))
				strike(player, HollowPullRules.DAMAGE);
	}

	private void drawRing(Vec3 at, double radius, int color, double width, int life) {
		int segments = 16;
		for (int i = 0; i < segments; i++) {
			double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
			Light.ray(level, at.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), at.add(Math.cos(b) * radius, 0, Math.sin(b) * radius), color, width, life);
		}
	}

	private boolean validOwner() {
		return master.canMaintainSignature(this) && master.level() == level && roster.equals(master.challengers())
			&& roster.stream().allMatch(id -> level.getPlayerByUUID(id) instanceof ServerPlayer p
				&& (master.canHarmParticipant(p) || !p.isAlive() && p.level() == level))
			&& master.position().distanceToSqr(origin) <= .0025 && master.onGround() && still(master);
	}

	private static boolean still(SwordMaster master) {
		Vec3 velocity = master.getDeltaMovement();
		return Double.isFinite(velocity.x) && Double.isFinite(velocity.y) && Double.isFinite(velocity.z)
			&& velocity.horizontalDistanceSqr() <= .0025 && Math.abs(velocity.y) <= .1;
	}

	/** Flat, solid ground under the Master's stance; irregular terrain declines every signature. */
	private static boolean supported(ServerLevel level, Vec3 origin) {
		if (!Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)
			|| Math.abs(origin.y - Math.rint(origin.y)) > .01) return false;
		BlockPos center = BlockPos.containing(origin).below();
		for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
			BlockPos floor = center.offset(x, 0, z);
			if (!level.hasChunkAt(floor) || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				|| !level.getFluidState(floor.above()).isEmpty()) return false;
		}
		return true;
	}
}
