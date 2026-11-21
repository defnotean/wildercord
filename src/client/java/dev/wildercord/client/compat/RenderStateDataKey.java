package dev.wildercord.client.compat;

import java.util.function.Supplier;

/**
 * A typed tag for per-render-state data, the shape fabric-rendering-v1 gives on 1.21.8 and newer.
 * 1.21.7's rendering module has no such class, so the keys are ours and
 * {@link RenderStateData} keeps the values.
 */
public final class RenderStateDataKey<T> {

	private final String name;

	private RenderStateDataKey(String name) {
		this.name = name;
	}

	/** A key distinct from every other one, named {@code name} for debugging. */
	public static <T> RenderStateDataKey<T> create(Supplier<String> name) {
		return new RenderStateDataKey<>(name.get());
	}

	@Override
	public String toString() {
		return name;
	}
}
