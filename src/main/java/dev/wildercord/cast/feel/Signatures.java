package dev.wildercord.cast.feel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The registry of {@link Signature}s, by rune id. Filled by the element classes listed in {@link Feels#init()}. */
public final class Signatures {
	private Signatures() {}

	private static final Map<String, Signature> ALL = new ConcurrentHashMap<>();

	static void put(Signature signature) {
		ALL.put(signature.id, signature);
	}

	/** The signature of a rune id, or null. */
	public static Signature get(String id) {
		return id == null || id.isEmpty() ? null : ALL.get(id);
	}

	/** The signature that leads a group: its first effect's, else its shape's, else null. */
	public static Signature of(Feel feel) {
		Signature s = get(feel.effectId());
		return s != null ? s : get(feel.shapeId());
	}

	/** The feel with its motion and scale adjusted by the leading signature, if it has any. */
	public static Feel adjust(Feel feel) {
		Signature s = of(feel);
		if (s == null || s.motion == null && s.scale == 1.0) {
			return feel;
		}
		double scale = Math.max(0.6, Math.min(2.2, feel.scale() * s.scale));
		return new Feel(s.motion != null ? s.motion : feel.motion(), feel.element(), feel.accent(), feel.role(), Band.of(scale), scale, feel.mods(),
			feel.shapeId(), feel.effectId());
	}

	static void clear() {
		ALL.clear();
	}
}
