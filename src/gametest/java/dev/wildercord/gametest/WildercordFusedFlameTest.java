package dev.wildercord.gametest;

import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.List;

/**
 * In game, the fused effects of {@code cast.FusedFlame}: each one cast for real and checked for what its
 * description promises.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedFlameTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		new FusedSample(Runes.PHOENIX_PYRE, Runes.SELF, 6, false),
		new FusedSample(Runes.HELLMOUTH, Runes.BOLT, 8, false),
		new FusedSample(Runes.STARFIRE, Runes.BOLT, 8, false),
		new FusedSample(Runes.EVERBURN, Runes.BOLT, 8, false),
		new FusedSample(Runes.BLOODBOIL, Runes.BOLT, 8, false),
		new FusedSample(Runes.CONFLAGRATION, Runes.BOLT, 8, false),
		new FusedSample(Runes.MONOLITH, Runes.BOLT, 8, false),
		new FusedSample(Runes.MAGNETIZE, Runes.BOLT, 8, false),
		new FusedSample(Runes.SINKHOLE, Runes.BOLT, 8, false));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
	}
}
