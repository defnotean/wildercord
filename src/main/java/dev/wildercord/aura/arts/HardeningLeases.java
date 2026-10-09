package dev.wildercord.aura.arts;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** The two stone wards share one effective modifier, but never share a deadline or a body identity. */
final class HardeningLeases<B> {
	enum Source { MOUNTAIN, UNMOVED }

	static final class Lease<B> {
		final B body;
		final Object level;
		final Source source;
		private final long acceptedAt, until;
		private final BooleanSupplier owner;
		private final LongSupplier clock;
		private boolean attempted, retired;

		private Lease(B body, Object level, Source source, long acceptedAt, int ticks,
				BooleanSupplier owner, LongSupplier clock) {
			if (ticks < 0) throw new IllegalArgumentException("A hardening lease cannot have negative duration");
			this.body = Objects.requireNonNull(body); this.level = Objects.requireNonNull(level);
			this.source = Objects.requireNonNull(source); this.acceptedAt = acceptedAt;
			this.until = Math.addExact(acceptedAt, ticks); this.owner = owner; this.clock = clock;
		}

		private boolean live() {
			long now = clock.getAsLong();
			if (!retired && (!owner.getAsBoolean() || now < acceptedAt
				|| now > until)) retired = true;
			return !retired;
		}
	}

	private final Map<B, EnumMap<Source, Lease<B>>> bodies = new IdentityHashMap<>();

	Lease<B> receipt(B body, Object level, Source source, long acceptedAt, int ticks,
			BooleanSupplier owner, LongSupplier clock) {
		return new Lease<>(body, level, source, acceptedAt, ticks, owner, clock);
	}

	/** The attempted bit is set even for a stale grant; a receipt cannot be refreshed or revived by replay. */
	boolean grant(Lease<B> lease) {
		if (lease.attempted) return false;
		lease.attempted = true;
		if (!lease.live()) return false;
		var sources = bodies.computeIfAbsent(lease.body, ignored -> new EnumMap<>(Source.class));
		Lease<B> previous = sources.put(lease.source, lease);
		if (previous != null) previous.retired = true;
		return true;
	}

	boolean active(Lease<B> lease) {
		var sources = bodies.get(lease.body);
		if (!lease.attempted || sources == null || sources.get(lease.source) != lease || !lease.live()) {
			// Asking about a prepared receipt must not consume its one permitted grant.
			if (lease.attempted) retire(lease);
			return false;
		}
		return true;
	}

	boolean active(B body) {
		var sources = bodies.get(body);
		boolean active = false;
		if (sources != null) for (Lease<B> lease : List.copyOf(sources.values())) active |= active(lease);
		return active;
	}

	void retire(Lease<B> lease) {
		lease.retired = true;
		var sources = bodies.get(lease.body);
		if (sources != null) {
			sources.remove(lease.source, lease);
			if (sources.isEmpty()) bodies.remove(lease.body, sources);
		}
	}

	/** A late old-world callback cannot revoke a lease already created in the destination world. */
	void retire(B body, Object level) {
		var sources = bodies.get(body);
		if (sources != null) for (Lease<B> lease : List.copyOf(sources.values()))
			if (level == null || lease.level == level) retire(lease);
	}

	List<B> bodies() { return List.copyOf(bodies.keySet()); }

	void clear() {
		for (B body : bodies()) retire(body, null);
	}
}
