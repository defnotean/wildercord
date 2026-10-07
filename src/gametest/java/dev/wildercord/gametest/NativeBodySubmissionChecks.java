package dev.wildercord.gametest;

/** CPU-only adversarial provenance checks. No Minecraft, mixin, renderer or pixel acceptance. */
public final class NativeBodySubmissionChecks {
	private record Node(int value) {}
	private static int passed, rejected;
	private static final class Case {
		final NativeBodySubmission proof = new NativeBodySubmission();
		final Object model = new Object(), state = new Object(), material = new Object();
		final NativeBodySubmission.Frames frames = new NativeBodySubmission.Frames(new Object(), new Object());
		final float[] root = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
		final Node node = new Node(7);
		void submit() {
			var call = proof.begin(model, state, frames, material, root);
			proof.submitted(node, model, state, frames, material, root, false);
			proof.submittedEnd(call, true);
		}
		NativeBodySubmission.Visit enter() { return proof.enter(node, model, state, frames, material, root); }
		void palette(NativeBodySubmission.Visit visit) { proof.baseline(visit, model, state, frames); proof.palette(visit, model, state, frames); }
		void draw(NativeBodySubmission.Visit visit) { proof.drawn(visit, node, model, state, frames, root); }
		void finish() { var visit = enter(); palette(visit); draw(visit); proof.leave(visit, true); proof.requireComplete(); }
		void auxiliary(Object otherModel, boolean outline) {
			var auxiliary = new Node(7); // Deliberately value-equal to the primary node.
			proof.submitted(auxiliary, otherModel, state, frames, material, root, outline);
			var visit = proof.enter(auxiliary, otherModel, state, frames, material, root);
			check(!visit.primary(), "Auxiliary node was promoted to primary");
			proof.drawn(visit, auxiliary, otherModel, state, frames, root); proof.leave(visit, true);
		}
	}
	public static void main(String[] args) {
		var basic = new Case(); basic.submit(); basic.finish(); passed++;
		for (boolean before : new boolean[] {false, true}) for (boolean sameModel : new boolean[] {false, true}) {
			var c = new Case(); c.submit();
			if (before) c.auxiliary(sameModel ? c.model : new Object(), false);
			c.finish();
			if (!before) c.auxiliary(sameModel ? c.model : new Object(), false);
			c.proof.requireComplete(); c.proof.clear(); check(c.proof.empty(), "Scope cleanup retained identities"); passed++;
		}
		var outline = new Case(); outline.submit(); outline.auxiliary(outline.model, true); outline.finish(); passed++;
		var nested = new Case(); nested.submit(); var visit = nested.enter(); nested.palette(visit);
		nested.auxiliary(new Object(), false); nested.draw(visit); nested.proof.leave(visit, true); nested.proof.requireComplete(); passed++;
		var neutral = new NativeBodySubmission(); Object node = new Object(), model = new Object(), state = new Object();
		var frames = new NativeBodySubmission.Frames(null, null); var root = new float[16];
		var origin = neutral.begin(model, state, frames, null, root); neutral.submitted(node, model, state, frames, null, root, false); neutral.submittedEnd(origin, true);
		var none = neutral.enter(node, model, state, frames, null, root); neutral.baseline(none, model, state, frames); neutral.palette(none, model, state, frames);
		neutral.drawn(none, node, model, state, frames, root); neutral.leave(none, true); neutral.requireComplete(); passed++;
		reject("missing primary", c -> c.proof.requireComplete());
		reject("missing native node", c -> { var call = c.proof.begin(c.model, c.state, c.frames, c.material, c.root); c.proof.submittedEnd(call, true); });
		reject("wrong submitted model", c -> { c.proof.begin(c.model, c.state, c.frames, c.material, c.root); c.proof.submitted(c.node, new Object(), c.state, c.frames, c.material, c.root, false); });
		reject("wrong submitted state", c -> { c.proof.begin(c.model, c.state, c.frames, c.material, c.root); c.proof.submitted(c.node, c.model, new Object(), c.frames, c.material, c.root, false); });
		reject("copied equal-valued node", c -> { c.submit(); c.proof.enter(new Node(7), c.model, c.state, c.frames, c.material, c.root); });
		reject("wrong deferred model", c -> { c.submit(); c.proof.enter(c.node, new Object(), c.state, c.frames, c.material, c.root); });
		reject("wrong deferred state", c -> { c.submit(); c.proof.enter(c.node, c.model, new Object(), c.frames, c.material, c.root); });
		reject("changed original frame", c -> { c.submit(); c.proof.enter(c.node, c.model, c.state, new NativeBodySubmission.Frames(new Object(), c.frames.articulated()), c.material, c.root); });
		reject("changed articulated frame", c -> { c.submit(); c.proof.enter(c.node, c.model, c.state, new NativeBodySubmission.Frames(c.frames.classic(), new Object()), c.material, c.root); });
		reject("changed material", c -> { c.submit(); c.proof.enter(c.node, c.model, c.state, c.frames, new Object(), c.root); });
		reject("changed root", c -> { c.submit(); var changed = c.root.clone(); changed[12] = 1; c.proof.enter(c.node, c.model, c.state, c.frames, c.material, changed); });
		reject("duplicate submission", c -> { c.submit(); c.proof.begin(c.model, c.state, c.frames, c.material, c.root); });
		reject("auxiliary only", c -> { c.submit(); c.auxiliary(c.model, false); c.proof.requireComplete(); });
		reject("duplicate preparation", c -> { c.submit(); c.finish(); c.enter(); });
		reject("missing baseline", c -> { c.submit(); var v = c.enter(); c.proof.palette(v, c.model, c.state, c.frames); });
		reject("wrong primary callback", c -> { c.submit(); var v = c.enter(); c.proof.baseline(v, new Object(), c.state, c.frames); });
		reject("duplicate palette", c -> { c.submit(); var v = c.enter(); c.palette(v); c.proof.palette(v, c.model, c.state, c.frames); });
		reject("missing draw", c -> { c.submit(); var v = c.enter(); c.palette(v); c.proof.leave(v, true); });
		reject("wrong draw node", c -> { c.submit(); var v = c.enter(); c.palette(v); c.proof.drawn(v, new Node(7), c.model, c.state, c.frames, c.root); });
		reject("duplicate draw", c -> { c.submit(); var v = c.enter(); c.palette(v); c.draw(v); c.draw(v); });
		reject("changed draw frame", c -> { c.submit(); var v = c.enter(); c.palette(v); c.proof.drawn(v, c.node, c.model, c.state, new NativeBodySubmission.Frames(new Object(), c.frames.articulated()), c.root); });
		reject("closed scope", c -> { c.submit(); var v = c.enter(); c.palette(v); c.draw(v); c.proof.leave(v, true); c.draw(v); });
		reject("swallowed rejection", c -> { c.submit(); try { c.proof.enter(new Node(7), c.model, c.state, c.frames, c.material, c.root); } catch (AssertionError expected) {} c.finish(); });
		reject("cross-capture node", c -> { var other = new Case(); other.submit(); c.proof.enter(other.node, other.model, other.state, other.frames, other.material, other.root); });
		for (boolean outlineFailure : new boolean[] {false, true}) submissionException(outlineFailure);
        prepareException(); observerTransparency();
        System.out.println("Native Submit CPU provenance controls=" + passed + ", adversarial rejections=" + rejected + "; native renderer not run");
	}
    private static void submissionException(boolean outlineFailure) {
        var c = new Case(); var nativeFailure = new IllegalStateException("native " + (outlineFailure ? "outline" : "normal"));
        int[] calls = {0}; Throwable observed = null;
        var origin = c.proof.observe(() -> c.proof.begin(c.model, c.state, c.frames, c.material, c.root), null);
        boolean completed = false;
        try {
            if (outlineFailure) c.proof.observe(() -> c.proof.submitted(c.node, c.model, c.state, c.frames, c.material, c.root, false));
            calls[0]++; throw nativeFailure;
        } catch (Throwable actual) { observed = actual; }
        finally { boolean done = completed; c.proof.observe(() -> c.proof.submittedEnd(origin, done)); }
        check(observed == nativeFailure && calls[0] == 1, "Observer changed native submission throwable identity or call count");
        requireFailure(c); c.proof.clear(); check(c.proof.empty(), "Failed submission scope leaked"); passed++;
    }
    private static void prepareException() {
        var c = new Case(); c.submit(); var nativeFailure = new AssertionError("native prepare");
        var visit = c.proof.observe(c::enter, null); int[] calls = {0}; Throwable observed = null; boolean completed = false;
        try { c.proof.observe(() -> c.palette(visit)); calls[0]++; throw nativeFailure; }
        catch (Throwable actual) { observed = actual; }
        finally { boolean done = completed; c.proof.observe(() -> c.proof.leave(visit, done)); }
        check(observed == nativeFailure && calls[0] == 1, "Observer changed native preparation throwable identity or call count");
        requireFailure(c); c.proof.clear(); check(c.proof.empty(), "Failed preparation scope leaked"); passed++;
    }
    private static void observerTransparency() {
        for (String position : new String[] {"normal", "outline", "begin", "baseline", "palette", "draw", "leave"}) {
            var c = new Case(); var expected = new Object(); int[] calls = {0}; var fault = new AssertionError("observer " + position);
            Object result;
            var origin = c.proof.observe(() -> c.proof.begin(c.model, c.state, c.frames, c.material, c.root), null);
            if (position.equals("begin")) c.proof.observe(() -> { throw fault; });
            calls[0]++; result = expected; // The unchanged native result remains authoritative.
            c.proof.observe(() -> { if (position.equals("normal")) throw fault; c.proof.submitted(c.node, c.model, c.state, c.frames, c.material, c.root, false); });
            c.proof.observe(() -> { if (position.equals("outline")) throw fault; c.proof.submitted(new Object(), c.model, c.state, c.frames, c.material, c.root, true); });
            c.proof.observe(() -> c.proof.submittedEnd(origin, true));
            var visit = c.proof.observe(c::enter, null);
            c.proof.observe(() -> { if (position.equals("baseline")) throw fault; c.proof.baseline(visit, c.model, c.state, c.frames); });
            c.proof.observe(() -> { if (position.equals("palette")) throw fault; c.proof.palette(visit, c.model, c.state, c.frames); });
            c.proof.observe(() -> { if (position.equals("draw")) throw fault; c.draw(visit); });
            c.proof.observe(() -> { if (position.equals("leave")) throw fault; c.proof.leave(visit, true); });
            check(result == expected && calls[0] == 1, "Observer changed native return identity or call count at " + position);
            requireFailure(c); c.proof.clear(); check(c.proof.empty(), "Observer-failed scope leaked at " + position); passed++;
        }
    }
    private static void requireFailure(Case c) {
        try { c.proof.requireComplete(); } catch (AssertionError expected) { return; }
        throw new AssertionError("Observer failure did not reject capture outside the native call");
    }
	private static void reject(String name, java.util.function.Consumer<Case> action) {
		try { action.accept(new Case()); } catch (AssertionError expected) { if (!expected.getMessage().startsWith("Native body Submit:")) throw expected; rejected++; return; }
		throw new AssertionError("Accepted " + name);
	}
	private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
