package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The colour-blind-safe setting recolours spell circles and shield rings, which carry boss telegraphs, not only sigils. */
class SafeTelegraphReachTest {
	@Test
	void circlesAndShieldsUseTheSafePalette() throws Exception {
		for (String file : List.of("SpellCircleParticle.java", "ShieldCircles.java")) {
			String source = Files.readString(Path.of("src/client/java/dev/wildercord/client/fx", file));
			assertTrue(source.contains("MagicQuality.safeTelegraphs ? dev.wildercord.presentation.SafeColourRules.safe(option.color())"),
				file + " ignores the colour-blind-safe setting");
		}
	}
}
