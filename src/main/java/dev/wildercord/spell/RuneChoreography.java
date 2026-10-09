package dev.wildercord.spell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Three explicitly authored visual beats for each castable rune. The drawing vocabulary is shared,
 * but the sequence is chosen by name rather than by a hash, category, or element. */
public final class RuneChoreography {
	private RuneChoreography() {}

	public enum Gesture {
		SEAL, HALO, ORBIT, LANCE, FAN, CRESCENT, CROSS, FORK, BURST, CRACK,
		DROPLET, SHARD, SPIRAL, VINE, PETAL, ROOTS, WING, FLAME, RAIN, GEAR,
		CLOCK, GATE, MIRROR, CHAIN, CROWN, SHELL, STAR, WAVE, PILLAR, SWARM,
		MIST, FLARE, NEEDLE, DIAMOND, TIDE, CLAW, HEART, EYE, STEP, TETHER,
		CLOUD, FOAM, LEDGER
	}

	public record Sequence(Gesture opening, Gesture middle, Gesture finish) {
		public Gesture at(int beat) {
			return switch (beat) {
				case 0 -> opening;
				case 1 -> middle;
				case 2 -> finish;
				default -> throw new IllegalArgumentException("beat " + beat);
			};
		}
	}

	private static final Map<String, Sequence> ALL = load();
	private static final Sequence ADDON = new Sequence(Gesture.SEAL, Gesture.ORBIT, Gesture.FLARE);

	public static Sequence of(RuneDef rune) {
		// Add-on namespaces cannot have entries in Wildercord's bundled roster.
		if (!rune.id().startsWith("wildercord:")) return ADDON;
		Sequence sequence = ALL.get(rune.path());
		if (sequence == null) throw new IllegalArgumentException("No authored animation for " + rune.id());
		return sequence;
	}

	public static Map<String, Sequence> all() {
		return ALL;
	}

	private static Map<String, Sequence> load() {
		Map<String, Sequence> result = new LinkedHashMap<>();
		try (var stream = RuneChoreography.class.getResourceAsStream("/assets/wildercord/animations/rune_choreography.txt")) {
			if (stream == null) throw new IllegalStateException("Missing rune choreography resource");
			try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				int number = 0;
				while ((line = reader.readLine()) != null) {
					number++;
					line = line.strip();
					if (line.isEmpty() || line.startsWith("#")) continue;
					String[] fields = Arrays.stream(line.split("\\|", -1)).map(String::strip).toArray(String[]::new);
					if (fields.length != 4) throw new IllegalStateException("Choreography line " + number + " needs a rune and three gestures");
					Sequence sequence = new Sequence(Gesture.valueOf(fields[1]), Gesture.valueOf(fields[2]), Gesture.valueOf(fields[3]));
					if (result.putIfAbsent(fields[0], sequence) != null) throw new IllegalStateException("Duplicate choreography for " + fields[0]);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not load rune choreography", e);
		}
		return Collections.unmodifiableMap(result);
	}
}
