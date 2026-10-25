package dev.wildercord.world.dungeons;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Part 1 of 3 of the structure gallery: see {@link StructureGallery}. */
public final class StructureGallery1Test implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		StructureGallery.shoot(c, 0, 3);
	}
}
