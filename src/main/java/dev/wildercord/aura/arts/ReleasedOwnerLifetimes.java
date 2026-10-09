package dev.wildercord.aura.arts;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Generations at actual world assignments. Minecraft entity IDs can be reused by distinct respawn bodies. */
final class ReleasedOwnerLifetimes<B, W> {
	static final class Life<W> {
		final W world;
		boolean retired;
		Life(W world) { this.world = world; }
	}
	static final class Change<B, W> {
		final B body;
		final W origin, destination;
		final Change<B, W> previous;
		final boolean changesWorld;
		boolean assigned, closed;
		Change(B body, W origin, W destination, Change<B, W> previous) {
			this.body = body; this.origin = origin; this.destination = destination; this.previous = previous;
			changesWorld = origin != destination;
		}
	}
	private static final class BodyKey<B> extends WeakReference<B> {
		private final int identityHash;
		BodyKey(B body, ReferenceQueue<B> queue) { super(body, queue); identityHash = System.identityHashCode(body); }
		@Override public int hashCode() { return identityHash; }
		@Override public boolean equals(Object other) {
			if (this == other) return true;
			B body = get();
			return body != null && other instanceof BodyKey<?> key && body == key.get();
		}
	}
	private final ReferenceQueue<B> abandoned = new ReferenceQueue<>();
	private final Map<BodyKey<B>, Life<W>> lives = new HashMap<>();
	private final Map<B, Change<B, W>> changing = new IdentityHashMap<>();

	Life<W> capture(B body, W world) {
		for (Change<B, W> change = changing.get(body); change != null; change = change.previous) {
			if (!change.assigned && change.origin == world && change.destination != world) {
				// A setter callback cannot acquire new outgoing-world authority after the departure barrier.
				Life<W> blocked = new Life<>(world); blocked.retired = true; return blocked;
			}
		}
		discardAbandoned();
		BodyKey<B> key = new BodyKey<>(body, abandoned);
		Life<W> previous = lives.get(key);
		if (previous != null && previous.world == world) return previous;
		if (previous != null) previous.retired = true;
		Life<W> fresh = new Life<>(world); lives.put(key, fresh); return fresh;
	}

	Change<B, W> begin(B body, W origin, W destination) {
		Change<B, W> change = new Change<>(body, origin, destination, changing.get(body));
		changing.put(body, change);
		if (change.changesWorld) retire(body);
		return change;
	}

	/** The native level assignment completed; later setter callbacks may legitimately return to the old world. */
	void assigned(B body) {
		Change<B, W> change = changing.get(body);
		if (change != null) change.assigned = true;
	}

	/** Called in finally with the body's actual world, including when a nested setter or the original invocation throws. */
	void finish(Change<B, W> change, W actualWorld) {
		if (change.closed) return;
		change.closed = true;
		if (changing.get(change.body) == change) {
			if (change.previous == null) changing.remove(change.body);
			else changing.put(change.body, change.previous);
		}
		Life<W> life = lives.get(new BodyKey<>(change.body, null));
		if (change.changesWorld && life != null && life.world != actualWorld) retire(change.body);
	}

	void retire(B body) {
		discardAbandoned();
		Life<W> life = lives.remove(new BodyKey<>(body, null));
		if (life != null) life.retired = true;
	}

	private void discardAbandoned() {
		for (var key = abandoned.poll(); key != null; key = abandoned.poll()) {
			Life<W> life = lives.remove(key);
			if (life != null) life.retired = true;
		}
	}

	void clear() {
		lives.values().forEach(life -> life.retired = true);
		lives.clear(); changing.clear();
	}
}
