package dev.wildercord.wildlife;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Runs from the nearest player it fears, at a walk, then flat out once they're within seven blocks. Like vanilla's
 * avoid goal, but shy creatures stay shy on Peaceful too (vanilla's looks for players as it would for a fight, and
 * Peaceful turns that off), and what it fears is the creature's own judgement: a stag's stance, a hare's nerves.
 */
final class FleeGoal extends Goal {
	private final PathfinderMob mob;
	private final Predicate<Player> fears;
	private final double range;
	private final double walk;
	private final double sprint;
	private @Nullable Player threat;
	private @Nullable Path path;

	FleeGoal(PathfinderMob mob, Predicate<Player> fears, double range, double walk, double sprint) {
		this.mob = mob;
		this.fears = fears;
		this.range = range;
		this.walk = walk;
		this.sprint = sprint;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		threat = null;
		double nearest = range * range;
		for (Player player : mob.level().players()) {
			double d = mob.distanceToSqr(player);
			if (d < nearest && Math.abs(player.getY() - mob.getY()) < 4 && fears.test(player) && (d < 36 || mob.hasLineOfSight(player))) {
				nearest = d;
				threat = player;
			}
		}
		if (threat == null) {
			return false;
		}
		Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, threat.position());
		if (away == null || threat.distanceToSqr(away) < threat.distanceToSqr(mob)) {
			return false;
		}
		path = mob.getNavigation().createPath(away.x, away.y, away.z, 0);
		return path != null;
	}

	@Override
	public boolean canContinueToUse() {
		return !mob.getNavigation().isDone();
	}

	@Override
	public void start() {
		mob.getNavigation().moveTo(path, walk);
	}

	@Override
	public void stop() {
		threat = null;
		path = null;
	}

	@Override
	public void tick() {
		if (threat != null) {
			mob.getNavigation().setSpeedModifier(mob.distanceToSqr(threat) < 49 ? sprint : walk);
		}
	}
}
