package dev.wildercord.client.render;

import dev.wildercord.client.compat.RenderStateDataKey;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where the values behind {@link dev.wildercord.client.compat.RenderStateDataKey} live on 1.21.7. Fabric attaches them to the
 * render states themselves on newer versions; here they are kept beside them, keyed by the state
 * instance and dropped with it, so a state that goes out of frame takes its data with it.
 */
public final class RenderStateData {

	private static final Map<Object, Map<RenderStateDataKey<?>, Object>> STATES = new WeakHashMap<>();

	private RenderStateData() {}

	/** Records {@code value} for {@code key} on {@code state}; null clears it. */
	public static <T> void set(Object state, RenderStateDataKey<T> key, T value) {
		if (state == null || key == null) {
			return;
		}
		synchronized (STATES) {
			Map<RenderStateDataKey<?>, Object> values = STATES.computeIfAbsent(state, ignored -> new HashMap<>());
			if (value == null) {
				values.remove(key);
			} else {
				values.put(key, value);
			}
			if (values.isEmpty()) {
				STATES.remove(state);
			}
		}
	}

	/** The value recorded for {@code key} on {@code state}, or null. */
	@SuppressWarnings("unchecked")
	public static <T> T get(Object state, RenderStateDataKey<T> key) {
		if (state == null || key == null) {
			return null;
		}
		synchronized (STATES) {
			Map<RenderStateDataKey<?>, Object> values = STATES.get(state);
			return values == null ? null : (T) values.get(key);
		}
	}
}
