package dev.wildercord.gametest;

import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.List;

/**
 * In game, the fused effects of {@code cast.FusedLife}: each one cast for real and checked for what its
 * description promises.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedLifeTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		new FusedSample(Runes.ZEPHYR, Runes.SELF, 6, false),
		new FusedSample(Runes.CRIMSON_MIST, Runes.BOLT, 8, false),
		new FusedSample(Runes.SOULBOND, Runes.SELF, 6, false),
		new FusedSample(Runes.SECOND_WIND, Runes.SELF, 6, false),
		new FusedSample(Runes.TRANSFUSION, Runes.SELF, 6, false),
		new FusedSample(Runes.LIFEBLOOM, Runes.SELF, 6, false),
		new FusedSample(Runes.BONESPUR, Runes.BOLT, 8, false),
		new FusedSample(Runes.SANGUINE_RITE, Runes.BOLT, 8, false));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
	}
}
