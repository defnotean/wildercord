package dev.wildercord.gametest;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

/** Standalone pure tests: tools/test_articulated_render_receipts.py. No game, GPU or synthetic skin claims. */
public final class ArticulatedRenderReceiptChecks {
	private static int checks;
	private static final ArticulatedRenderReceipt.Palette ACTIVE = new ArticulatedRenderReceipt.Palette(42, 1, "ACTIVE", 1, false, false, "actual-palette");
	private ArticulatedRenderReceiptChecks() {}

	public static void main(String[] args) throws Exception {
		check(ArticulatedRenderReceipt.inScope("articulated_shared_trial"), "exact prefix selected");
		check(!ArticulatedRenderReceipt.inScope("other_articulated_shared_trial") && !ArticulatedRenderReceipt.inScope("world_thumbnail")
			&& !ArticulatedRenderReceipt.inScope(null), "thumbnail and neighboring names excluded");
		check(ArticulatedRenderReceipt.requestedPhase("articulated_shared_x_requested_active").equals("ACTIVE")
			&& ArticulatedRenderReceipt.requestedPhase("articulated_shared_x").equals("unknown"), "request parsing does not invent phase");
		Path directory = Files.createTempDirectory("articulated-receipt-test-");
		try {
			Path image = directory.resolve("actual-returned-name.png");
			BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
			source.setRGB(0, 0, 0xff123456); source.setRGB(1, 0, 0xffabcdef);
			source.setRGB(0, 1, 0xff010203); source.setRGB(1, 1, 0xff987654);
			ImageIO.write(source, "PNG", image.toFile());
			byte[] originalBytes = Files.readAllBytes(image);
			var pixels = ArticulatedRenderReceipt.pixels(2, 2, source::getRGB);
			check(pixels.sha256().equals("5a55e7a3668f6030a15cb196031462464b431438ac6b34a855ab712d3182528a"), "fixed ARGB digest vector agrees with external artifact tooling");
			var binding = ArticulatedRenderReceipt.bind(image, directory);
			check(binding.returnedPath().equals(image.toString()) && binding.relativeImagePath().equals("actual-returned-name.png"), "actual path preserved");
			check(binding.decodedPixels().equals(pixels), "PNG decoded colors match callback digest");
			check(!pixels.equals(ArticulatedRenderReceipt.pixels(2, 2, (x, y) -> source.getRGB(x, 1 - y))), "pixel order affects hash");
			check(!pixels.equals(ArticulatedRenderReceipt.pixels(4, 1, (x, y) -> source.getRGB(x % 2, x / 2))), "dimensions domain-separate pixel hash");
			var success = ready("ACTIVE", false, false, false, ACTIVE);
			finish(success, pixels, binding);
			check(success.report().verified() && success.report().requestedPhaseObserved() && success.report().unpausedPhaseCoverage(), "genuine matching association passes");
			check(success.report().nativeLaunchNonce().equals("unbound"), "ordinary unwrapped native route preserves valid image association without claiming launch provenance");
			check(success.report().renderedPhase().equals("ACTIVE") && success.report().exactImpactPixelCoverage().equals("unverified")
				&& success.report().nativePixelReviewRequired(), "active palette does not claim exact contact pixels");
			var miss = ready("WINDUP", false, false, false, ACTIVE); finish(miss, pixels, binding);
			check(miss.report().verified() && miss.report().renderedPhase().equals("ACTIVE") && !miss.report().requestedPhaseObserved(), "phase miss preserves actual ACTIVE without relabel or retry");
			for (boolean frozen : new boolean[] {false, true}) {
				var paused = ready("ACTIVE", false, !frozen, frozen, ACTIVE); finish(paused, pixels, binding);
				check(paused.report().verified() && !paused.report().unpausedPhaseCoverage(), "paused or frozen image cannot satisfy unpaused coverage");
			}
			var repeat = ready("ACTIVE", false, false, false, ACTIVE); finish(repeat, pixels, binding);
			check(repeat.report().verified() && repeat.report().callbackPixels().equals(success.report().callbackPixels())
				&& !repeat.report().identity().equals(success.report().identity()), "identical frame pixels preserve distinct captures");
			var changed = ready("ACTIVE", false, false, false, ACTIVE);
			changed.received(ArticulatedRenderReceipt.pixels(2, 2, (x, y) -> 0xff000000)); changed.written(binding); changed.finish();
			check(!changed.report().verified() && changed.report().failures().contains("decoded_pixel_hash_mismatch"), "digest mismatch rejects association");
			check(java.util.Arrays.equals(originalBytes, Files.readAllBytes(image)), "failure preserves original PNG bytes");
			var missing = ready("ACTIVE", false, false, false, ACTIVE); missing.written(binding); missing.finish();
			check(missing.report().failures().contains("missing_callback"), "old same-name PNG cannot replace missing callback");
			var late = ready("ACTIVE", false, false, false, ACTIVE); var copied = late.report().copy(); late.failed(new IllegalStateException("deadline"));
			late.received(pixels);
			check(!late.report().verified() && late.report().failures().contains("late_callback") && late.report().copy() == copied, "late callback cannot revive failed operation or replace copy snapshot");
			var duplicate = ready("ACTIVE", false, false, false, ACTIVE); duplicate.received(pixels); duplicate.received(pixels); duplicate.written(binding); duplicate.finish();
			check(duplicate.report().failures().contains("duplicate_callback") && !duplicate.report().verified(), "duplicate callbacks reject one-to-one binding");
			success.received(pixels);
			check(!success.report().verified() && success.report().failures().contains("late_callback"), "callback after completion invalidates prior success");
			var twice = ready("ACTIVE", false, false, false, ACTIVE); twice.enqueue(999, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
			check(twice.report().copy().copySequence() != 999 && twice.report().failures().contains("duplicate_or_late_copy"), "duplicate copy retains first immutable evidence");
			var rebind = ready("ACTIVE", false, false, false, ACTIVE); rebind.received(pixels); rebind.written(binding); rebind.written(binding); rebind.finish();
			check(!rebind.report().verified() && rebind.report().failures().contains("duplicate_or_late_image_association"), "one copy cannot be associated twice");
			var dimensions = ready("ACTIVE", false, false, false, ACTIVE); dimensions.received(ArticulatedRenderReceipt.pixels(1, 1, (x, y) -> 0));
			check(dimensions.report().failures().contains("callback_dimensions_changed"), "readback dimensions must match actual copied target");
			var replacement = ready("ACTIVE", false, false, false, new ArticulatedRenderReceipt.Palette(43, 1, "ACTIVE", 1, false, false, "replacement"));
			check(replacement.report().failures().stream().anyMatch(s -> s.startsWith("wrong_activation")), "replacement cannot inherit previous activation");
			var cancelled = ready("ACTIVE", false, false, false, null);
			check(cancelled.report().renderedPhase().equals("unknown") && cancelled.report().failures().stream().anyMatch(s -> s.startsWith("missing_palette")), "cancelled frame cannot revive old palette");
			var first = ready("ACTIVE", true, false, false, ACTIVE); finish(first, pixels, binding);
			check(first.report().verified(), "view model, deferred palette and real item establish first-person association");
			check(first.report().copy().passes().stream().filter(p -> p.kind().equals("view_attachment")).count() == 1
				&& first.report().copy().passes().stream().filter(p -> p.kind().equals("view_deferred")).count() == 1, "attachment setup is separate from deferred setup");
			immutableCopies(); invalidSubmissions(); callbackSafety(pixels); delayedCallbackIsolation(pixels, binding);
			openingChecks(pixels, binding);
			check(ArticulatedRenderReceipt.stillMatches(binding), "unchanged returned image remains associated");
			source.setRGB(0, 0, 0xff654321); ImageIO.write(source, "PNG", image.toFile());
			check(!ArticulatedRenderReceipt.stillMatches(binding), "later filename reuse invalidates old receipt");
		} finally {
			try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.delete(file); }
			Files.delete(directory);
		}
		System.out.println("Articulated render receipt checks passed: " + checks);
	}

	private static long ids;
	private static ArticulatedRenderReceipt.Session session(String requested, boolean first, boolean paused, boolean frozen) {
		var s = new ArticulatedRenderReceipt.Session(new ArticulatedRenderReceipt.Identity("test-run", ++ids, "articulated_shared_x_requested_" + requested.toLowerCase(), requested));
		s.observe("firstPerson", first); s.observe("paused", paused); s.observe("frozen", frozen);
		s.observe("ownerId", 7); s.observe("acceptedStartTick", 42); s.observe("acceptedMove", 1);
		s.observe("renderTargetGeneration", 1); s.observe("renderTextureGeneration", 2);
		s.extracted(10 + ids); s.rendered(20 + ids); return s;
	}
	private static ArticulatedRenderReceipt.Pass pass(String kind, ArticulatedRenderReceipt.Palette palette) {
		return new ArticulatedRenderReceipt.Pass(kind, 100, 200, 7, palette, Map.of("segmentedVisible", "true", "skinModel", "wide", "skinTexture", "observed-skin", "rigWidth", "wide"));
	}
	private static ArticulatedRenderReceipt.Session ready(String requested, boolean first, boolean paused, boolean frozen, ArticulatedRenderReceipt.Palette palette) {
		var s = session(requested, first, paused, frozen);
		s.pass(pass(first ? "view_submit" : "body_submit", palette)); s.pass(pass(first ? "view_deferred" : "body_deferred", palette));
		if (first) { s.pass(pass("view_item", palette)); s.pass(pass("view_attachment", palette)); }
		s.enqueue(30 + ids, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0)); return s;
	}
	private static void finish(ArticulatedRenderReceipt.Session s, ArticulatedRenderReceipt.Pixels pixels, ArticulatedRenderReceipt.Binding binding) {
		s.received(pixels); s.written(binding); s.finish();
	}
	private static void immutableCopies() {
		Map<String, String> mutable = new HashMap<>(Map.of("skinModel", "slim"));
		var pass = new ArticulatedRenderReceipt.Pass("body_submit", 1, 2, 7, ACTIVE, mutable); mutable.put("skinModel", "wide");
		check(pass.attributes().get("skinModel").equals("slim"), "pass defensively copies appearance");
		var s = ready("ACTIVE", false, false, false, ACTIVE); var copy = s.report().copy();
		s.observe("acceptedStartTick", 99); s.pass(pass);
		check(copy.observation().get("acceptedStartTick").equals("42") && copy.passes().size() == 2, "late game state cannot change copied activation or palette");
		try { copy.passes().clear(); throw new AssertionError("copy mutable"); } catch (UnsupportedOperationException expected) { checks++; }
	}
	private static void invalidSubmissions() {
		var attachment = session("ACTIVE", false, false, false); attachment.pass(pass("body_submit", ACTIVE)); attachment.pass(pass("body_attachment", ACTIVE));
		attachment.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(attachment.report().renderedPhase().equals("unknown") && attachment.report().failures().contains("missing_body_deferred"), "body layer attachment cannot stand in for actual deferred geometry preparation");
		var s = session("ACTIVE", false, false, false); s.pass(pass("body_submit", ACTIVE));
		s.pass(new ArticulatedRenderReceipt.Pass("body_deferred", 100, 201, 7, ACTIVE, Map.of("segmentedVisible", "true")));
		s.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(s.report().failures().contains("conflicting_deferred_palette"), "different model cannot prove submitted body's palette");
		var invisible = session("ACTIVE", false, false, false); invisible.pass(pass("body_submit", ACTIVE));
		invisible.pass(new ArticulatedRenderReceipt.Pass("body_deferred", 100, 200, 7, ACTIVE, Map.of("segmentedVisible", "false")));
		invisible.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(invisible.report().renderedPhase().equals("unknown") && invisible.report().failures().contains("incompatible_backend:body_deferred"), "raw compatible frame cannot prove hidden segmented backend");
		var missingItem = session("ACTIVE", true, false, false); missingItem.pass(pass("view_submit", ACTIVE)); missingItem.pass(pass("view_deferred", ACTIVE));
		missingItem.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(missingItem.report().failures().contains("expected_one_view_item"), "successful offhand ownership cannot stand in for drawn item");
		var noDraw = session("ACTIVE", false, false, false); noDraw.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(noDraw.report().renderedPhase().equals("unknown") && !noDraw.report().failures().isEmpty(), "missing native submission rejects receipt");
		var owner = session("ACTIVE", false, false, false); owner.pass(new ArticulatedRenderReceipt.Pass("body_submit", 100, 200, 8, ACTIVE, Map.of()));
		owner.pass(pass("body_deferred", ACTIVE)); owner.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(owner.report().failures().contains("wrong_owner:body_submit"), "another player's pass cannot satisfy owner gate");
	}
	private static void callbackSafety(ArticulatedRenderReceipt.Pixels pixels) {
		var s = ready("ACTIVE", false, false, false, ACTIVE); AtomicInteger originalCalls = new AtomicInteger();
		var consumer = ArticulatedRenderReceipt.<Object>observeConsumer(s, value -> { throw new AssertionError("observer broke"); }, value -> originalCalls.incrementAndGet());
		consumer.accept(new Object());
		check(originalCalls.get() == 1 && s.report().failures().stream().anyMatch(f -> f.startsWith("callback_observer_failed")), "observer error still completes original consumer once");
		RuntimeException originalFailure = new RuntimeException("original fixture failure");
		var throwing = ArticulatedRenderReceipt.<Object>observeConsumer(s, value -> { throw new Error("probe"); }, value -> { originalCalls.incrementAndGet(); throw originalFailure; });
		try { throwing.accept(null); throw new AssertionError("missing original exception"); }
		catch (RuntimeException failure) { check(failure == originalFailure && originalCalls.get() == 2, "original exception instance and exactly-once call preserved"); }
		var future = new CompletableFuture<Object>();
		ArticulatedRenderReceipt.<Object>observeConsumer(s, value -> { throw new Error("probe"); }, future::complete).accept("saved");
		check(future.isDone() && future.join().equals("saved"), "observer failure cannot strand Fabric-style completion future");
	}
	private static void delayedCallbackIsolation(ArticulatedRenderReceipt.Pixels pixels, ArticulatedRenderReceipt.Binding binding) throws Exception {
		var a = ready("WINDUP", false, false, false, ACTIVE); var copy = a.report().copy();
		var callback = ArticulatedRenderReceipt.observeConsumer(a, a::received, value -> {});
		var b = ready("RECOVERY", false, false, false, new ArticulatedRenderReceipt.Palette(42, 1, "RECOVERY", .5F, false, false, "later"));
		Thread callbackThread = new Thread(() -> callback.accept(pixels)); callbackThread.start(); callbackThread.join();
		a.written(binding); a.finish();
		check(a.report().verified() && a.report().copy() == copy && a.report().renderedPhase().equals("ACTIVE")
			&& b.report().callbackPixels() == null, "delayed callback remains bound to immutable capture across threads and later renders");
	}
	private static ArticulatedRenderReceipt.Session openingSession(boolean first, boolean fallback, String requested) {
		String name = "articulated_opening_style_kindling_draw_" + (first ? "first" : "third")
			+ "_right_netherite_funded_shell" + (fallback ? "_adapter_disabled" : "") + "_requested_" + requested.toLowerCase();
		var s = new ArticulatedRenderReceipt.Session(new ArticulatedRenderReceipt.Identity("opening-run", ++ids, name, requested), "launch-nonce");
		for (var entry : Map.of("firstPerson", String.valueOf(first), "paused", "false", "frozen", "false", "ownerId", "7", "ownerUuid", "owner-uuid",
			"acceptedStartTick", "42", "acceptedMove", "3", "renderTargetGeneration", "1", "renderTextureGeneration", "2").entrySet()) s.observe(entry.getKey(), entry.getValue());
		for (var entry : Map.of("openingArmed", "true", "expectedBackend", fallback ? "full_fallback" : "segmented", "shellAdapterEnabled", String.valueOf(!fallback),
			"expectedOwnerId", "7", "expectedOwnerUuid", "owner-uuid", "expectedMove", "3", "expectedStartTick", "42", "acceptedEntity", "7").entrySet()) s.observe(entry.getKey(), entry.getValue());
		s.extracted(10); s.rendered(20); return s;
	}
	private static ArticulatedRenderReceipt.Pass openingPass(String kind, boolean fallback) {
		Map<String, String> a = new HashMap<>(Map.of("skinModel", "wide", "skinTexture", "actual-skin", "rigWidth", "wide", "mainArm", "RIGHT",
			"ownerUuid", "owner-uuid", "avatarStateIdentity", "100", "extractedStateIdentity", "100", "postHitStopExtractionMatched", "true"));
		a.putAll(Map.of("rawAcceptedPhase", "ACTIVE", "rawActivation", "42", "rawMove", "3", "rawLeftHanded", "false", "rawPaletteSha256", "raw-palette",
			"shellGlowPresent", "true", "shellAdapterEnabled", String.valueOf(!fallback), "segmentedVisible", String.valueOf(!fallback), "rigidVisible", String.valueOf(fallback), "segmentedRootVisible", String.valueOf(!fallback)));
		a.putAll(Map.of("head", "minecraft:netherite_helmet:foil=true", "chest", "minecraft:netherite_chestplate:foil=true", "legs", "minecraft:netherite_leggings:foil=true",
			"feet", "minecraft:netherite_boots:foil=true", "mainHand", "minecraft:diamond_sword:foil=false", "fallbackFrameCompatible", String.valueOf(!fallback), "itemStateIdentity", "300"));
		return new ArticulatedRenderReceipt.Pass(kind, 100, 200, 7, new ArticulatedRenderReceipt.Palette(42, 3, "ACTIVE", 1, false, false, "raw-palette"), a);
	}
	private static void openingPasses(ArticulatedRenderReceipt.Session s, boolean first, boolean fallback) {
		s.pass(openingPass(first ? (fallback ? "view_fallback_submit" : "view_submit") : "body_submit", fallback));
		s.pass(openingPass(first ? (fallback ? "view_fallback_item" : "view_deferred") : "body_deferred", fallback));
		if (first && !fallback) s.pass(openingPass("view_item", false));
	}
	private static void openingChecks(ArticulatedRenderReceipt.Pixels pixels, ArticulatedRenderReceipt.Binding binding) {
		for (boolean first : new boolean[] {false, true}) for (boolean fallback : new boolean[] {false, true}) {
			var s = openingSession(first, fallback, "ACTIVE"); openingPasses(s, first, fallback);
			s.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0)); finish(s, pixels, binding);
			check(s.report().verified(), "exact accepted opening native backend binds: " + first + "/" + fallback + " " + s.report().failures());
			check(s.report().schemaVersion() == 2 && s.report().rawAcceptedPhase().equals("ACTIVE") && s.report().rawRequestedPhaseObserved(), "opening raw phase is separate actual evidence");
			check(s.report().renderedPhase().equals(fallback ? "FALLBACK" : "ACTIVE") && s.report().requestedPhaseObserved() == !fallback
				&& s.report().unpausedPhaseCoverage() == !fallback, "fallback never claims segmented phase coverage");
		}
		var missed = openingSession(false, false, "WINDUP"); openingPasses(missed, false, false);
		missed.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0)); finish(missed, pixels, binding);
		check(missed.report().verified() && !missed.report().rawRequestedPhaseObserved() && !missed.report().requestedPhaseObserved(), "opening phase miss retains exact actual palette");
		var wrong = openingSession(false, false, "ACTIVE"); wrong.observe("expectedStartTick", 41); openingPasses(wrong, false, false);
		wrong.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0)); finish(wrong, pixels, binding);
		check(!wrong.report().verified() && wrong.report().failures().contains("wrong_expected_StartTick"), "new activation cannot replace explicitly armed accepted opening");
		var copied = openingSession(false, false, "ACTIVE"); copied.pass(openingPass("body_submit", false));
		var bad = openingPass("body_deferred", false); var attrs = new HashMap<>(bad.attributes()); attrs.put("extractedStateIdentity", "101");
		copied.pass(new ArticulatedRenderReceipt.Pass(bad.kind(), bad.stateIdentity(), bad.modelIdentity(), bad.owner(), bad.palette(), attrs));
		copied.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(copied.report().failures().contains("wrong_opening_extraction"), "another extracted avatar state cannot prove submitted owner");
		var pretend = openingSession(false, true, "ACTIVE"); pretend.pass(openingPass("body_submit", true)); pretend.pass(openingPass("body_deferred", false));
		pretend.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(pretend.report().failures().contains("wrong_opening_backend"), "segmented geometry cannot stand in for declared full fallback");
		var noItem = openingSession(true, true, "ACTIVE"); noItem.pass(openingPass("view_fallback_submit", true));
		noItem.enqueue(30, new ArticulatedRenderReceipt.Target(1, 2, 2, 2, 0));
		check(noItem.report().failures().contains("expected_one_opening_item"), "vanilla callback alone cannot stand in for actual fallback sword submission");
	}

	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
