package dev.wildercord.world.dungeons;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Part 3 of 6 of the structure gallery: see {@link StructureGallery}. */
public final class StructureGallery3Test implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		StructureGallery.shoot(c, 2, 6);
	}
}
