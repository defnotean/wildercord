package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/** Up to eight effects bound into one socket, stored as a flat, canonical list of registered rune ids. */
public final class WovenRunes {
	private WovenRunes() {}

	public static final String PREFIX = "wildercord:weave/";
	public static final int MAX_EFFECTS = 8;
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
		if (!Fusions.weavable(a) || !Fusions.weavable(b)) {
			throw new IllegalArgumentException("Only elemental effects can be woven");
		}
		java.util.ArrayList<String> ids = new java.util.ArrayList<>();
		for (RuneDef input : List.of(a, b)) {
			if (isWoven(input)) contents(input).forEach(r -> ids.add(r.id()));
			else ids.add(input.id());
		}
		if (ids.size() > MAX_EFFECTS) throw new IllegalArgumentException("A weave holds at most eight effects");
		ids.sort(String::compareTo);
		String id = PREFIX + Knots.encode(String.join("\n", ids).getBytes(StandardCharsets.UTF_8));
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
		if (pair.size() < 2) {
			return Optional.empty();
		}
		RuneDef a = pair.get(0);
		Set<String> traits = new HashSet<>();
		pair.forEach(r -> traits.addAll(r.traits()));
		if(pair.stream().filter(Runes::innate).map(RuneDef::id).distinct().count()>1)return Optional.empty();
		boolean soul = pair.stream().anyMatch(Runes::innate);
		if(soul)traits.remove(Trait.POWER); // Innates grow with the heart, never through rune ranks.
		EffectKind kind = pair.stream().anyMatch(r -> r.kind() == EffectKind.HARMFUL) ? EffectKind.HARMFUL : a.kind();
		int tier = Math.max(pair.stream().mapToInt(RuneDef::tier).max().orElse(1), pair.size() > 4 ? 4 : pair.size() > 2 ? 3 : 1);
		if(soul)tier=4;
		String name = a.name() + " & " + pair.get(1).name() + (pair.size() > 2 ? " + " + (pair.size() - 2) : "");
		return Optional.of(new RuneDef(id, name, RuneFamily.EFFECT, tier,
			pair.stream().mapToDouble(RuneDef::cost).sum(), 1.0, a.element(), kind, traits, "",
			"Casts " + String.join(", ", pair.stream().map(RuneDef::name).toList())
				+ " together at their full combined mana cost. Add an elemental rune with an amethyst block to weave up to eight effects. Three or four effects need tier III; five to eight need tier IV."
				+ (soul ? " This soul weave needs tier IV and only works for a caster whose heart owns its innate rune." : ""), "fusion"));
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
		if (parts.length < 2 || parts.length > MAX_EFFECTS) {
			return List.of();
		}
		java.util.ArrayList<RuneDef> runes = new java.util.ArrayList<>();
		for (int i = 0; i < parts.length; i++) {
			if (isWoven(parts[i]) || i > 0 && parts[i - 1].compareTo(parts[i]) > 0) return List.of();
			Optional<RuneDef> rune = Runes.get(parts[i]);
			if (rune.isEmpty() || !Fusions.weavable(rune.get())) return List.of();
			runes.add(rune.get());
		}
		return List.copyOf(runes);
	}
}
