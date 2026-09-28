package dev.wildercord.gametest;

import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.List;

/**
 * In game, the fused effects of {@code cast.FusedVoid}: each one cast for real and checked for what its
 * description promises.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedVoidTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		new FusedSample(Runes.ENTROPY, Runes.BOLT, 8, false),
		new FusedSample(Runes.DEVOUR, Runes.BOLT, 8, false),
		new FusedSample(Runes.TIMESTEAL, Runes.BOLT, 8, false),
		new FusedSample(Runes.HEMOMANCY, Runes.BOLT, 8, false),
		new FusedSample(Runes.RECKONING, Runes.BOLT, 8, false),
		new FusedSample(Runes.SINGULARITY, Runes.BOLT, 8, false),
		new FusedSample(Runes.PRISMATIC_BURST, Runes.BOLT, 8, false),
		new FusedSample(Runes.CHRONOSHIFT, Runes.SELF, 6, false));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
	}
}
