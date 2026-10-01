package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every breathing method: the ten built in, one per element, and any an add-on registers. Pure, so the HUD, the Aura page,
 * loot, trades and the unit tests all read the same list.
 *
 * <p>The colours are each element's own (see docs/ART.md), so a blade's aura reads as its element at a glance.</p>
 */
public final class BreathingMethods {
	private BreathingMethods() {}

	private static final Map<String, BreathingMethod> ALL = Collections.synchronizedMap(new LinkedHashMap<>());

	public static final BreathingMethod EMBER = register(new BreathingMethod("ember", "fire", 0xF06E32, 0xFFD060, BreathingMethod.Flavour.IGNITE));
	public static final BreathingMethod RIME = register(new BreathingMethod("rime", "frost", 0x8CDCFF, 0xE6FAFF, BreathingMethod.Flavour.CHILL));
	public static final BreathingMethod THUNDER = register(new BreathingMethod("thunder", "storm", 0xFFE650, 0xFFFBE0, BreathingMethod.Flavour.SPARK));
	public static final BreathingMethod GALE = register(new BreathingMethod("gale", "wind", 0xC8F0DC, 0xFFFFFF, BreathingMethod.Flavour.GALE));
	public static final BreathingMethod STONE = register(new BreathingMethod("stone", "earth", 0xC8A06A, 0xF0D8A8, BreathingMethod.Flavour.STONE));
	public static final BreathingMethod VERDANT = register(new BreathingMethod("verdant", "life", 0x6EDC64, 0xE8FFB0, BreathingMethod.Flavour.MEND));
	public static final BreathingMethod HOLLOW = register(new BreathingMethod("hollow", "void", 0xB45AF0, 0xE0B0FF, BreathingMethod.Flavour.PULL));
	public static final BreathingMethod STARLIT = register(new BreathingMethod("starlit", "arcane", 0xE678DC, 0xFFD8FA, BreathingMethod.Flavour.STARLIT));
	public static final BreathingMethod HOURGLASS = register(new BreathingMethod("hourglass", "time", 0xF2D98A, 0xFFF8E0, BreathingMethod.Flavour.HASTE));
	public static final BreathingMethod CRIMSON = register(new BreathingMethod("crimson", "blood", 0xD2283C, 0xFF6474, BreathingMethod.Flavour.LEECH));

	/** The built-in methods, in element order. */
	public static final List<BreathingMethod> BUILT_IN = List.of(EMBER, RIME, THUNDER, GALE, STONE, VERDANT, HOLLOW, STARLIT, HOURGLASS, CRIMSON);

	/**
	 * Adds a method (an add-on's: give it a namespaced id). Registering an id again replaces it, so a reload of an add-on's
	 * methods stays tidy.
	 */
	public static BreathingMethod register(BreathingMethod method) {
		ALL.put(method.id(), method);
		return method;
	}

	public static Optional<BreathingMethod> byId(String id) {
		return id == null || id.isEmpty() ? Optional.empty() : Optional.ofNullable(ALL.get(id));
	}

	/** Every method, built-in first, in the order registered. */
	public static List<BreathingMethod> all() {
		synchronized (ALL) {
			return List.copyOf(new ArrayList<>(ALL.values()));
		}
	}

	/** The built-in method of {@code element}, if any. */
	public static Optional<BreathingMethod> ofElement(String element) {
		for (BreathingMethod method : BUILT_IN) {
			if (method.element().equals(element)) {
				return Optional.of(method);
			}
		}
		return Optional.empty();
	}
}
