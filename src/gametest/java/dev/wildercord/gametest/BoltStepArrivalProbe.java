package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ThunderArts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Test-only receipts from direct Bolt Step cuts; Thunder's passive spark never calls Hits.strike. */
public final class BoltStepArrivalProbe {
	private BoltStepArrivalProbe() {}

	public record Arrival(UUID target, long tick, Vec3 playerPosition, Vec3 targetPosition, float damage) {}
	private static UUID watchedPlayer;
	private static List<UUID> expectedTargets = List.of();
	private static final List<Arrival> ARRIVALS = new ArrayList<>();

	public static synchronized void begin(UUID player, List<UUID> targets) {
		watchedPlayer = player;
		expectedTargets = List.copyOf(targets);
		ARRIVALS.clear();
	}

	public static synchronized void clear() {
		watchedPlayer = null;
		expectedTargets = List.of();
		ARRIVALS.clear();
	}

	/** Called after the direct cut returns, in the same callback as the real blink and before later physics. */
	public static synchronized void struck(ServerPlayer player, AuraApi.StringArt art, LivingEntity target, float damage) {
		if (!player.getUUID().equals(watchedPlayer) || art == null || !art.id().equals(ThunderArts.BOLT_STEP)) return;
		Arrival arrival = new Arrival(target.getUUID(), player.level().getGameTime(), player.position(), target.position(), damage);
		ARRIVALS.add(arrival);
		dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_BOLT_STEP_ARRIVAL {}", arrival);
	}

	public static synchronized String problem(UUID player) {
		if (!player.equals(watchedPlayer)) return "the Bolt Step arrival recorder was not armed for this player";
		return problem(expectedTargets, ARRIVALS);
	}

	/** The late health checks remain separate; arrival must also contain every successful direct cut in order. */
	public static String problem(List<UUID> expected, List<Arrival> arrivals) {
		List<UUID> actual = arrivals.stream().map(Arrival::target).toList();
		if (expected.isEmpty() || !actual.equals(expected)) {
			return "the direct Bolt Step cuts should reach each expected husk once in order (expected=" + expected + ", arrivals=" + arrivals + ")";
		}
		if (arrivals.stream().anyMatch(arrival -> !(arrival.damage() > 0) || !Float.isFinite(arrival.damage()))) {
			return "each direct Bolt Step cut should deal damage (arrivals=" + arrivals + ")";
		}
		Arrival last = arrivals.getLast();
		double distance = last.playerPosition().distanceTo(last.targetPosition());
		return distance < 3 ? null : "and end beside the last at its actual cut (distance=" + distance + ", arrival=" + last + ")";
	}
}
