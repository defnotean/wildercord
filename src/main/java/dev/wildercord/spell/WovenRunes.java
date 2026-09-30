package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/** A pair of effects bound into one socket. The id carries both inputs, so it survives saves and trades. */
public final class WovenRunes {
	private WovenRunes() {}

	public static final String PREFIX = "wildercord:weave/";
	private static final int MAX_ID_LENGTH = Knots.MAX_ID_LENGTH;
	private static final Map<String, Optional<RuneDef>> CACHE = new ConcurrentHashMap<>();

	public static boolean isWoven(String id) {
		return id != null && id.startsWith(PREFIX);
	}

	public static boolean isWoven(RuneDef rune) {
		return isWoven(rune.id());
	}

	/** Input order does not matter: the same two effects always make the same rune. */
	public static RuneDef bind(RuneDef a, RuneDef b) {
		if (!Fusions.fusible(a) || !Fusions.fusible(b)) {
			throw new IllegalArgumentException("Only elemental effects can be woven");
		}
		String first = a.id().compareTo(b.id()) <= 0 ? a.id() : b.id();
		String second = a.id().compareTo(b.id()) <= 0 ? b.id() : a.id();
		String id = PREFIX + Knots.encode((first + "\n" + second).getBytes(StandardCharsets.UTF_8));
		return def(id).orElseThrow(() -> new IllegalArgumentException("Woven rune id is too long"));
	}

	public static Optional<RuneDef> def(String id) {
		if (!isWoven(id) || id.length() > MAX_ID_LENGTH) {
			return Optional.empty();
		}
		if (CACHE.size() > 4096) {
			CACHE.clear();
		}
		return CACHE.computeIfAbsent(id, WovenRunes::read);
	}

	public static List<RuneDef> contents(RuneDef rune) {
		return def(rune.id()).isEmpty() ? List.of() : readContents(rune.id());
	}

	private static Optional<RuneDef> read(String id) {
		List<RuneDef> pair = readContents(id);
		if (pair.size() != 2) {
			return Optional.empty();
		}
		RuneDef a = pair.get(0);
		RuneDef b = pair.get(1);
		Set<String> traits = new HashSet<>(a.traits());
		traits.addAll(b.traits());
		EffectKind kind = a.kind() == EffectKind.HARMFUL || b.kind() == EffectKind.HARMFUL ? EffectKind.HARMFUL : a.kind();
		return Optional.of(new RuneDef(id, a.name() + " & " + b.name(), RuneFamily.EFFECT,
			Math.max(a.tier(), b.tier()), a.cost() + b.cost(), 1.0, a.element(), kind, traits, "",
			"Casts " + a.name() + " and " + b.name() + " together from one socket at their full combined mana cost. Woven from those two runes and an amethyst block at a Fusion Altar.", "fusion"));
	}

	private static List<RuneDef> readContents(String id) {
		if (!isWoven(id) || id.length() > MAX_ID_LENGTH) {
			return List.of();
		}
		byte[] bytes = Knots.decode(id.substring(PREFIX.length()));
		if (bytes == null || !id.equals(PREFIX + Knots.encode(bytes))) {
			return List.of();
		}
		String[] parts = new String(bytes, StandardCharsets.UTF_8).split("\n", -1);
		if (parts.length != 2 || parts[0].compareTo(parts[1]) > 0 || isWoven(parts[0]) || isWoven(parts[1])) {
			return List.of();
		}
		Optional<RuneDef> a = Runes.get(parts[0]);
		Optional<RuneDef> b = Runes.get(parts[1]);
		if (a.isEmpty() || b.isEmpty() || !Fusions.fusible(a.get()) || !Fusions.fusible(b.get())) {
			return List.of();
		}
		return List.of(a.get(), b.get());
	}
}
