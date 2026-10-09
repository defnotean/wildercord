package dev.wildercord.gametest;

import java.util.Arrays;
import java.util.IdentityHashMap;

/** Passive, capture-local provenance for the exact native primary body Submit and its draw. */
public final class NativeBodySubmission {
	public record Frames(Object classic, Object articulated) {
		boolean same(Frames other) { return other != null && classic == other.classic && articulated == other.articulated; }
	}
	public static final class Origin {
		final Object model, state, material; final Frames frames; final float[] root;
		Object node;
		Origin(Object model, Object state, Frames frames, Object material, float[] root) {
			this.model = model; this.state = state; this.frames = frames; this.material = material; this.root = root.clone();
		}
	}
	public static final class Visit {
		final Entry entry; final Visit previous;
		Visit(Entry entry, Visit previous) { this.entry = entry; this.previous = previous; }
		public boolean primary() { return entry.primary; }
	}
	private static final class Entry {
		final Object node, model, state, material; final Frames frames; final float[] root; final boolean primary;
		boolean entered, baseline, palette, drawn, completed;
		Entry(Object node, Object model, Object state, Frames frames, Object material, float[] root, boolean primary) {
			this.node = node; this.model = model; this.state = state; this.frames = frames; this.material = material;
			this.root = root.clone(); this.primary = primary;
		}
	}
	private final IdentityHashMap<Object, Entry> entries = new IdentityHashMap<>();
	private Origin origin;
	private Entry primary;
	private Visit active;
	private String failure;
	private String lastEvent;

	/** Observer errors are reported at capture finalization, never through the native operation. */
	public <T> T observe(java.util.function.Supplier<T> observer, T fallback) {
		try { return observer.get(); }
		catch (Throwable problem) { if (failure == null) failure = "observer " + problem; return fallback; }
	}
	public void observe(Runnable observer) { observe(() -> { observer.run(); return null; }, null); }

	public Origin begin(Object model, Object state, Frames frames, Object material, float[] root) {
		check(origin == null && primary == null, "duplicate or nested primary submission");
		check(model != null && state != null && frames != null && root != null, "missing primary identity");
		return origin = new Origin(model, state, frames, material, root);
	}
	/** The constructor observer returns the original Submit unchanged. Outline nodes are auxiliary. */
	public void submitted(Object node, Object model, Object state, Frames frames, Object material, float[] root, boolean outline) {
		lastEvent = describe("submit", node, model, state, frames, material, root);
		check(node != null && !entries.containsKey(node), "duplicate native Submit identity");
		boolean body = origin != null && !outline;
		if (body) {
			check(origin.node == null && primary == null, "duplicate primary native Submit");
			check(origin.model == model && origin.state == state && origin.frames.same(frames)
				&& origin.material == material && Arrays.equals(origin.root, root), "substituted primary native Submit");
			origin.node = node;
		}
		var entry = new Entry(node, model, state, frames, material, root, body);
		entries.put(node, entry); if (body) primary = entry;
	}
	public void submittedEnd(Origin call, boolean completed) {
		try { check(origin == call && completed && call.node != null, "primary submission did not complete"); }
		finally { origin = null; }
	}
	public boolean tracked(Object node) { return entries.containsKey(node); }
	public Visit enter(Object node, Object model, Object state, Frames frames, Object material, float[] root) {
		lastEvent = describe("prepare", node, model, state, frames, material, root);
		var entry = entries.get(node);
		check(entry != null, "unobserved native Submit");
		check(entry.model == model && entry.state == state && entry.frames.same(frames)
			&& entry.material == material && Arrays.equals(entry.root, root), "changed deferred native Submit");
		check(!entry.entered, "duplicate native Submit preparation"); entry.entered = true;
		return active = new Visit(entry, active);
	}
	public void baseline(Visit call, Object model, Object state, Frames frames) {
		var entry = requirePrimary(call, model, state, frames);
		check(!entry.baseline && !entry.palette && !entry.drawn, "duplicate or late primary baseline"); entry.baseline = true;
	}
	public void palette(Visit call, Object model, Object state, Frames frames) {
		var entry = requirePrimary(call, model, state, frames);
		check(entry.baseline && !entry.palette && !entry.drawn, "missing or duplicate primary palette"); entry.palette = true;
	}
	/** Called only after the original Model.renderToBuffer returns for this exact prepareModel call. */
	public void drawn(Visit call, Object node, Object model, Object state, Frames frames, float[] root) {
		lastEvent = describe("draw", node, model, state, frames, call == null ? null : call.entry.material, root);
		check(active == call && call != null && call.entry.node == node && call.entry.model == model
			&& call.entry.state == state && call.entry.frames.same(frames) && Arrays.equals(call.entry.root, root), "substituted native body draw");
		var entry = call.entry;
		check(!entry.drawn && (!entry.primary || entry.baseline && entry.palette), "missing palette or duplicate native body draw");
		entry.drawn = true;
	}
	public void leave(Visit call, boolean completed) {
		try { check(active == call && call != null && completed && call.entry.drawn, "native body preparation did not complete"); call.entry.completed = true; }
		finally { active = call == null ? null : call.previous; }
	}
	private Entry requirePrimary(Visit call, Object model, Object state, Frames frames) {
		check(active == call && call != null && call.entry == primary && call.entry.primary
			&& call.entry.model == model && call.entry.state == state && call.entry.frames.same(frames), "unbound primary body callback");
		return call.entry;
	}
	public void requireHealthy() { check(failure == null, "native body observer failed"); }
	public void requireComplete() {
		check(failure == null && origin == null && active == null && primary != null
			&& primary.entered && primary.baseline && primary.palette && primary.drawn && primary.completed, "missing complete exact primary body draw");
	}
	public void clear() { entries.clear(); origin = null; primary = null; active = null; failure = null; lastEvent = null; }
	public boolean empty() { return entries.isEmpty() && origin == null && primary == null && active == null && failure == null && lastEvent == null; }
	private static String identity(Object value) { return value == null ? "null" : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value)); }
	private static String describe(String event, Object node, Object model, Object state, Frames frames, Object material, float[] root) {
		return event + "[node=" + identity(node) + ",model=" + identity(model) + ",state=" + identity(state)
			+ ",classic=" + identity(frames == null ? null : frames.classic()) + ",articulated=" + identity(frames == null ? null : frames.articulated())
			+ ",material=" + identity(material) + ",root=" + Arrays.toString(root) + "]";
	}
	private void check(boolean pass, String message) {
		if (!pass || failure != null) {
			if (failure == null) failure = message + "; last=" + lastEvent + "; primary=" + (primary == null ? "absent"
				: describe("primary", primary.node, primary.model, primary.state, primary.frames, primary.material, primary.root)
				+ ", stages=" + primary.entered + "/" + primary.baseline + "/" + primary.palette + "/" + primary.drawn + "/" + primary.completed);
			throw new AssertionError("Native body Submit: " + failure);
		}
	}
}
