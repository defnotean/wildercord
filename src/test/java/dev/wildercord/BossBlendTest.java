package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The boss models blend through the shared {@code PoseBlend}: a move begun again blends out of its own last frame instead of
 * snapping back to its start, and no blend builds a new pose for every part every frame.
 */
class BossBlendTest {
	private static final Path DIR = Path.of("src/client/java/dev/wildercord/client/auraworld");

	@Test
	void everyBossBlendsThroughTheSharedHelper() throws Exception {
		for (String boss : List.of("AshenStag", "BellWarden", "FrostWyrmling", "HollowMatron", "LanternBearer", "Mojito", "Gravekeeper")) {
			String source = Files.readString(DIR.resolve(boss + "Model.java"));
			assertTrue(source.contains("blender=new PoseBlend(posed);"), boss + " has no PoseBlend");
			assertTrue(source.contains("if(s.blend<1){"), boss + " skips the blend when a move restarts");
			assertFalse(source.contains("new PartPose["), boss + " allocates poses every frame");
			assertTrue(source.contains("blender.store();") && source.contains("blender.mix(k);"), boss + " does not blend");
		}
	}

	@Test
	void theRendererMarksARestartAsASwitch() throws Exception {
		for (String boss : List.of("AshenStag", "BellWarden", "FrostWyrmling", "HollowMatron", "LanternBearer", "Mojito", "Gravekeeper")) {
			String source = Files.readString(DIR.resolve(boss + "Renderer.java"));
			assertTrue(source.contains("s.elapsed<") && source.contains("shownElapsed-1"), boss + "Renderer does not notice a move begun again");
		}
	}
}
