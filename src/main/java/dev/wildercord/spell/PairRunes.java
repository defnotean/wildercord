package dev.wildercord.spell;

import dev.wildercord.pairs.PairSpecs;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The pair fusions: every two elemental effects fuse at the Fusion Altar (with an amethyst shard) into a rune of
 * their own, each made by hand with its own effect in {@code dev.wildercord.pairs}. A pair rune's id names its two
 * runes, alphabetically ({@code wildercord:pair/chill/shock}), so the roster never holds them all: each is read
 * when first asked for. A pair not yet written fuses as its elements do (see {@link Fusions#recipe}).
 */
public final class PairRunes {
	private PairRunes() {}

	public static final String PREFIX = "wildercord:pair/";
	private static final Map<String, Optional<RuneDef>> CACHE = new ConcurrentHashMap<>();

	public static boolean isPair(String id) {
		return id != null && id.startsWith(PREFIX);
	}

	public static boolean isPair(RuneDef rune) {
		return isPair(rune.id());
	}

	/** Whether {@code rune} can be one half of a pair: one of Wildercord's own elemental effects, not a weave. */
	public static boolean pairable(RuneDef rune) {
		return rune.id().startsWith("wildercord:") && Fusions.weavable(rune) && !WovenRunes.isWoven(rune) && !isPair(rune);
	}

	/** The id of the pair of {@code a} and {@code b}, in either order. */
	public static String id(RuneDef a, RuneDef b) {
		String x = a.path();
		String y = b.path();
		return x.compareTo(y) <= 0 ? PREFIX + x + "/" + y : PREFIX + y + "/" + x;
	}

	/** The pair rune {@code a} and {@code b} fuse into, if they're two different pairable effects and it has been written. */
	public static Optional<RuneDef> of(RuneDef a, RuneDef b) {
		if (!pairable(a) || !pairable(b) || a.is(b.id())) {
			return Optional.empty();
		}
		return def(id(a, b));
	}

	public static Optional<RuneDef> def(String id) {
		if (!isPair(id) || id.length() > 160) {
			return Optional.empty();
		}
		if (CACHE.size() > 4096) {
			CACHE.clear();
		}
		return CACHE.computeIfAbsent(id, PairRunes::read);
	}

	/** The two runes a pair rune is made of, alphabetically, or none if it isn't one. */
	public static List<RuneDef> parts(RuneDef rune) {
		if (def(rune.id()).isEmpty()) {
			return List.of();
		}
		String[] paths = rune.id().substring(PREFIX.length()).split("/", -1);
		return List.of(Runes.get("wildercord:" + paths[0]).orElseThrow(), Runes.get("wildercord:" + paths[1]).orElseThrow());
	}

	/** The pair's own spec, or null when it hasn't been written (or isn't a pair). */
	public static PairSpec spec(RuneDef rune) {
		if (!isPair(rune)) {
			return null;
		}
		String[] paths = rune.id().substring(PREFIX.length()).split("/", -1);
		return paths.length == 2 ? PairSpecs.spec(paths[0], paths[1]) : null;
	}

	private static Optional<RuneDef> read(String id) {
		String[] paths = id.substring(PREFIX.length()).split("/", -1);
		if (paths.length != 2 || paths[0].compareTo(paths[1]) >= 0) {
			return Optional.empty();
		}
		Optional<RuneDef> a = Runes.get("wildercord:" + paths[0]);
		Optional<RuneDef> b = Runes.get("wildercord:" + paths[1]);
		if (a.isEmpty() || b.isEmpty() || !pairable(a.get()) || !pairable(b.get())) {
			return Optional.empty();
		}
		PairSpec spec = PairSpecs.spec(paths[0], paths[1]);
		if (spec == null || !spec.element().equals(a.get().element()) && !spec.element().equals(b.get().element())) {
			return Optional.empty();
		}
		RuneDef x = a.get();
		RuneDef y = b.get();
		boolean soul = Runes.innate(x) || Runes.innate(y);
		Set<String> traits = new HashSet<>(spec.traits());
		traits.add(Trait.FRUGAL);
		if (spec.kind() == EffectKind.HELPFUL) {
			traits.add(Trait.SHARE);
		}
		// Innates grow with the heart, never through rune ranks.
		if (soul) {
			traits.remove(Trait.POWER);
		}
		// A pair is a fused rune: tier III at least (IV with an innate), costing its dearer half and half the other.
		int tier = soul ? 4 : Math.max(3, Math.max(x.tier(), y.tier()));
		double cost = Math.round(Math.max(x.cost(), y.cost()) + Math.min(x.cost(), y.cost()) / 2);
		String text = spec.text() + (soul ? " A soul pair: only works for a caster whose heart owns its innate rune." : "");
		return Optional.of(new RuneDef(id, spec.name(), RuneFamily.EFFECT, tier, cost, 1.0, spec.element(), spec.kind(), traits, "", text, "fusion"));
	}
}
