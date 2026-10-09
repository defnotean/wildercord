package dev.wildercord.gametest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import javax.imageio.ImageIO;

/** Minecraft-independent receipt rules. Kept in the test mod, never shipped in the game mod. */
public final class ArticulatedRenderReceipt {
	public static final String PREFIX = "articulated_shared_";
	public static final String OPENING_PREFIX = "articulated_opening_style_";
	public static final String PIXEL_FORMAT = "argb32-be-row-major-v1";
	private ArticulatedRenderReceipt() {}

	public record Identity(String runId, long captureSequence, String trial, String requestedPhase) {
		public Identity {
			Objects.requireNonNull(runId); Objects.requireNonNull(trial); Objects.requireNonNull(requestedPhase);
		}
	}
	public record Palette(long activation, int move, String phase, float weight, boolean leftHanded,
			boolean master, String localsSha256) {}
	/** All attributes are copied scalars/strings; no native state, item, model, matrix or array escapes. */
	public record Pass(String kind, long stateIdentity, long modelIdentity, int owner, Palette palette,
			Map<String, String> attributes) {
		public Pass { attributes = Map.copyOf(attributes); }
	}
	public record Target(long targetGeneration, long textureGeneration, int width, int height, int mipLevel) {}
	public record Copy(Identity identity, long extractionSequence, long renderSequence, long copySequence,
			Map<String, String> observation, List<Pass> passes, Target target) {
		public Copy { observation = Map.copyOf(observation); passes = List.copyOf(passes); }
	}
	public record Pixels(int width, int height, String format, String sha256) {}
	public record Binding(String returnedPath, String relativeImagePath, long pngBytes, String pngSha256,
			Pixels decodedPixels) {}
	public record Report(int schemaVersion, Identity identity, String origin, String nativeLaunchNonce, List<String> stages, Copy copy,
			Pixels callbackPixels, Binding image, String renderedPhase, boolean requestedPhaseObserved,
			boolean unpausedPhaseCoverage, String exactImpactPixelCoverage, boolean nativePixelReviewRequired,
			boolean verified, List<String> failures, String expectedBackend, String rawAcceptedPhase, Boolean rawRequestedPhaseObserved) {
		public Report { stages = List.copyOf(stages); failures = List.copyOf(failures); }
	}

	public static boolean opening(String name) { return name != null && name.startsWith(OPENING_PREFIX); }
	public static boolean fallback(String name) { return opening(name) && name.contains("_adapter_disabled_requested_"); }
	public static boolean inScope(String name) { return name != null && (name.startsWith(PREFIX) || opening(name)); }
	public static String requestedPhase(String name) {
		int at = name.lastIndexOf("_requested_");
		if (at < 0) return "unknown";
		String value = name.substring(at + "_requested_".length()).toUpperCase(Locale.ROOT);
		return List.of("WINDUP", "ACTIVE", "RECOVERY", "NONE").contains(value) ? value : "unknown";
	}

	/** One operation, one immutable copy snapshot. Methods are safe across render/readback/test threads. */
	public static final class Session {
		private final Identity identity;
		private final String nativeLaunchNonce;
		private final Map<String, String> observation = new LinkedHashMap<>();
		private final List<Pass> passes = new ArrayList<>();
		private final List<String> stages = new ArrayList<>(List.of("requested"));
		private final List<String> failures = new ArrayList<>();
		private long extractionSequence, renderSequence;
		private Copy copy;
		private Pixels pixels;
		private Binding binding;
		private boolean ended;
		private int callbacks;

		public Session(Identity identity) { this(identity, "unbound"); }
		public Session(Identity identity, String nativeLaunchNonce) {
			this.identity = identity;
			this.nativeLaunchNonce = nativeLaunchNonce == null ? "unbound" : nativeLaunchNonce;
		}
		public Identity identity() { return identity; }
		public synchronized boolean ended() { return ended; }
		public synchronized void observe(String key, Object value) {
			if (copy != null || ended) { reject("observation_after_copy:" + key); return; }
			observation.put(key, String.valueOf(value));
		}
		public synchronized void extracted(long sequence) {
			if (extractionSequence != 0 || copy != null || ended) { reject("duplicate_or_late_extraction"); return; }
			extractionSequence = sequence; stages.add("extracted");
		}
		public synchronized void rendered(long sequence) {
			if (renderSequence != 0 || copy != null || ended) { reject("duplicate_or_late_render"); return; }
			renderSequence = sequence; stages.add("rendered");
		}
		public synchronized void pass(Pass pass) {
			if (copy != null || ended) { reject("pass_after_copy:" + pass.kind()); return; }
			passes.add(pass);
		}
		public synchronized Copy enqueue(long sequence, Target target) {
			if (copy != null || ended) { reject("duplicate_or_late_copy"); return copy; }
			copy = new Copy(identity, extractionSequence, renderSequence, sequence, observation, passes, target);
			stages.add("copy-enqueued");
			validate(copy);
			return copy;
		}
		public synchronized void received(Pixels received) {
			callbacks++;
			if (ended) { reject("late_callback"); return; }
			if (callbacks != 1) { reject("duplicate_callback"); return; }
			pixels = received; stages.add("image-received");
			if (copy == null) reject("callback_without_copy");
			else if (copy.target().width() != received.width() || copy.target().height() != received.height()) reject("callback_dimensions_changed");
		}
		public synchronized void written(Binding image) {
			if (ended || binding != null) { reject("duplicate_or_late_image_association"); return; }
			binding = image; stages.add("image-written");
			if (pixels == null) reject("missing_callback");
			else if (!pixels.equals(image.decodedPixels())) reject("decoded_pixel_hash_mismatch");
		}
		public synchronized void finish() {
			if (ended) { reject("duplicate_completion"); return; }
			ended = true;
			if (copy == null) reject("missing_copy");
			if (pixels == null) reject("missing_callback");
			if (binding == null) reject("missing_image");
			if (failures.isEmpty()) stages.add("receipt-verified");
		}
		public synchronized void failed(Throwable failure) {
			reject("original_operation_failed:" + failure.getClass().getName() + ":" + failure.getMessage());
			if (!ended) finish();
		}
		public synchronized void reject(String reason) { if (!failures.contains(reason)) failures.add(reason); }
		public synchronized Report report() {
			String rendered = renderedPhase(copy);
			boolean verified = ended && binding != null && pixels != null && copy != null && failures.isEmpty();
			boolean matched = verified && !rendered.equals("unknown") && identity.requestedPhase().equals(rendered);
			boolean unpaused = copy != null && "false".equals(copy.observation().get("paused"))
				&& "false".equals(copy.observation().get("frozen"));
			boolean opening = opening(identity.trial());
			String raw = opening ? rawPhase(copy) : null;
			return new Report(opening ? 2 : 1, identity, "fabric_test_screenshot", nativeLaunchNonce, stages, copy, pixels, binding, rendered, matched,
				matched && unpaused, "unverified", true, verified, failures,
				opening ? (fallback(identity.trial()) ? "full_fallback" : "segmented") : null, raw,
				opening ? verified && identity.requestedPhase().equals(raw) : null);
		}
		private void validate(Copy snapshot) {
			if (snapshot.extractionSequence() <= 0) reject("missing_extraction");
			if (snapshot.renderSequence() <= 0) reject("missing_render");
			if (!(snapshot.extractionSequence() < snapshot.renderSequence() && snapshot.renderSequence() < snapshot.copySequence())) reject("unordered_capture_stages");
			if (snapshot.target().width() <= 0 || snapshot.target().height() <= 0) reject("invalid_target_dimensions");
			if (snapshot.target().mipLevel() != 0) reject("unexpected_copy_mip");
			if (!String.valueOf(snapshot.target().targetGeneration()).equals(snapshot.observation().get("renderTargetGeneration"))
				|| !String.valueOf(snapshot.target().textureGeneration()).equals(snapshot.observation().get("renderTextureGeneration"))) reject("render_copy_target_mismatch");
			if (opening(identity.trial())) { validateOpening(snapshot); return; }
			boolean first = "true".equals(snapshot.observation().get("firstPerson"));
			String submission = first ? "view_submit" : "body_submit", deferred = first ? "view_deferred" : "body_deferred";
			List<Pass> submissions = snapshot.passes().stream().filter(p -> p.kind().equals(submission)).toList();
			List<Pass> palettes = snapshot.passes().stream().filter(p -> p.kind().equals(deferred)).toList();
			if (submissions.size() != 1) reject("expected_one_" + submission + ":" + submissions.size());
			if (palettes.isEmpty()) reject("missing_" + deferred);
			if (first && snapshot.passes().stream().filter(p -> p.kind().equals("view_item")).count() != 1) reject("expected_one_view_item");
			for (Pass p : snapshot.passes()) {
				if (!String.valueOf(p.owner()).equals(snapshot.observation().get("ownerId"))) reject("wrong_owner:" + p.kind());
				if (p.palette() == null) { reject("missing_palette:" + p.kind()); continue; }
				if (p.palette().master() || !String.valueOf(p.palette().activation()).equals(snapshot.observation().get("acceptedStartTick"))
					|| !String.valueOf(p.palette().move()).equals(snapshot.observation().get("acceptedMove"))) reject("wrong_activation:" + p.kind());
				if (p.kind().endsWith("deferred") && !"true".equals(p.attributes().get("segmentedVisible"))) reject("incompatible_backend:" + p.kind());
			}
			if (submissions.size() == 1) for (Pass p : palettes) {
				Pass submitted = submissions.getFirst();
				if (submitted.stateIdentity() != p.stateIdentity() || submitted.modelIdentity() != p.modelIdentity()
					|| !Objects.equals(submitted.palette(), p.palette())) reject("conflicting_deferred_palette");
				for (String attribute : List.of("skinModel", "skinTexture", "rigWidth")) {
					String value = p.attributes().get(attribute);
					if (value == null || value.equals("unknown") || !value.equals(submitted.attributes().get(attribute))) reject("conflicting_or_unknown_appearance:" + attribute);
				}
			}
		}
		private void validateOpening(Copy snapshot) {
			Map<String, String> o = snapshot.observation();
			boolean first = "true".equals(o.get("firstPerson")), fallback = fallback(identity.trial());
			String backend = fallback ? "full_fallback" : "segmented";
			if (!"true".equals(o.get("openingArmed")) || !backend.equals(o.get("expectedBackend"))) reject("missing_opening_expectation");
			for (String field : List.of("OwnerId", "OwnerUuid", "Move", "StartTick")) {
				String observed = field.startsWith("Owner") ? Character.toLowerCase(field.charAt(0)) + field.substring(1) : "accepted" + field;
				if (o.get("expected" + field) == null || !o.get("expected" + field).equals(o.get(observed))) reject("wrong_expected_" + field);
			}
			if (!Objects.equals(o.get("ownerId"), o.get("acceptedEntity"))) reject("wrong_accepted_entity");
			if (!String.valueOf(!fallback).equals(o.get("shellAdapterEnabled"))) reject("wrong_shell_adapter");
			String submitKind = first ? (fallback ? "view_fallback_submit" : "view_submit") : "body_submit";
			String drawKind = first ? (fallback ? "view_fallback_item" : "view_deferred") : "body_deferred";
			List<Pass> submits = snapshot.passes().stream().filter(p -> p.kind().equals(submitKind)).toList();
			List<Pass> draws = snapshot.passes().stream().filter(p -> p.kind().equals(drawKind)).toList();
			if (submits.size() != 1 || draws.isEmpty() || first && fallback && draws.size() != 1) reject("missing_opening_native_pass");
			List<String> allowed = first ? (fallback ? List.of("view_fallback_submit", "view_fallback_item")
				: List.of("view_submit", "view_deferred", "view_attachment", "view_item")) : List.of("body_submit", "body_deferred", "body_attachment");
			for (Pass p : snapshot.passes()) {
				if (!allowed.contains(p.kind())) reject("unexpected_opening_pass:" + p.kind());
				if (p.stateIdentity() <= 0 || p.modelIdentity() <= 0 || !String.valueOf(p.owner()).equals(o.get("ownerId"))) reject("wrong_opening_identity");
				Palette palette = p.palette(); Map<String, String> a = p.attributes();
				if (palette == null || palette.master() || !String.valueOf(palette.activation()).equals(o.get("acceptedStartTick"))
					|| !String.valueOf(palette.move()).equals(o.get("acceptedMove"))) { reject("wrong_opening_activation"); continue; }
				if (!Objects.equals(a.get("ownerUuid"), o.get("ownerUuid")) || !"true".equals(a.get("postHitStopExtractionMatched"))
					|| !Objects.equals(a.get("avatarStateIdentity"), a.get("extractedStateIdentity"))) reject("wrong_opening_extraction");
				if (!palette.phase().equals(a.get("rawAcceptedPhase")) || !String.valueOf(palette.activation()).equals(a.get("rawActivation"))
					|| !String.valueOf(palette.move()).equals(a.get("rawMove"))) reject("changed_post_hitstop_palette");
				if (!String.valueOf(palette.leftHanded()).equals(a.get("rawLeftHanded")) || !String.valueOf(palette.leftHanded()).equals(String.valueOf("LEFT".equals(a.get("mainArm"))))) reject("wrong_opening_hand");
				if (!String.valueOf(!fallback).equals(a.get("shellAdapterEnabled"))) reject("changed_shell_adapter");
				for (String field : List.of("skinModel", "skinTexture", "rigWidth", "head", "chest", "legs", "feet", "mainHand", "rawPaletteSha256"))
					if (a.get(field) == null || a.get(field).isEmpty() || a.get(field).equals("unknown")) reject("unknown_opening_appearance:" + field);
			}
			if (submits.size() == 1) {
				Pass submitted = submits.getFirst();
				for (Pass p : draws) {
					if (p.stateIdentity() != submitted.stateIdentity() || p.modelIdentity() != submitted.modelIdentity() || !Objects.equals(p.palette(), submitted.palette())) reject("conflicting_deferred_palette");
					for (String field : List.of("skinModel", "skinTexture", "rigWidth", "avatarStateIdentity", "ownerUuid", "head", "chest", "legs", "feet", "mainHand", "shellGlowPresent", "shellAdapterEnabled", "rawPaletteSha256"))
						if (!Objects.equals(p.attributes().get(field), submitted.attributes().get(field))) reject("conflicting_opening_appearance:" + field);
					if (!String.valueOf(!fallback).equals(p.attributes().get("segmentedVisible"))) reject("wrong_opening_backend");
					if (fallback && (!"false".equals(p.attributes().get("fallbackFrameCompatible"))
						|| !first && (!"true".equals(p.attributes().get("rigidVisible")) || !"false".equals(p.attributes().get("segmentedRootVisible"))))) reject("incomplete_native_fallback");
				}
				if (first) {
					List<Pass> items = snapshot.passes().stream().filter(p -> p.kind().equals(fallback ? "view_fallback_item" : "view_item")).toList();
					if (items.size() != 1) reject("expected_one_opening_item");
					else if (items.getFirst().stateIdentity() != submitted.stateIdentity() || items.getFirst().modelIdentity() != submitted.modelIdentity()
						|| !Objects.equals(items.getFirst().palette(), submitted.palette())) reject("conflicting_opening_item");
				}
			}
		}

	}

	private static String renderedPhase(Copy copy) {
		if (copy == null) return "unknown";
		if (fallback(copy.identity().trial())) {
			String kind = "true".equals(copy.observation().get("firstPerson")) ? "view_fallback_item" : "body_deferred";
			return copy.passes().stream().anyMatch(p -> p.kind().equals(kind) && "false".equals(p.attributes().get("segmentedVisible"))) ? "FALLBACK" : "unknown";
		}
		String kind = "true".equals(copy.observation().get("firstPerson")) ? "view_deferred" : "body_deferred";
		List<String> phases = copy.passes().stream().filter(p -> p.kind().equals(kind) && p.palette() != null
			&& "true".equals(p.attributes().get("segmentedVisible"))).map(p -> p.palette().phase()).distinct().toList();
		return phases.size() == 1 ? phases.getFirst() : "unknown";
	}

	private static String rawPhase(Copy copy) {
		if (copy == null) return "unknown";
		List<String> phases = copy.passes().stream().map(p -> p.attributes().get("rawAcceptedPhase")).filter(Objects::nonNull).distinct().toList();
		return phases.size() == 1 ? phases.getFirst() : "unknown";
	}

	/** Observer exceptions never prevent the original consumer from completing Fabric's future. */
	public static <T> Consumer<T> observeConsumer(Session session, Consumer<T> observer, Consumer<T> original) {
		return value -> {
			try { observer.accept(value); }
			catch (Throwable failure) { session.reject("callback_observer_failed:" + failure); }
			original.accept(value);
		};
	}

	/** Digest includes domain, dimensions and top-to-bottom, left-to-right ARGB32 big-endian pixels. */
	public static Pixels pixels(int width, int height, IntBinaryOperator argb) {
		if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid pixel dimensions");
		MessageDigest digest = digest();
		digest.update((PIXEL_FORMAT + "\0").getBytes(StandardCharsets.US_ASCII));
		integer(digest, width); integer(digest, height);
		for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) integer(digest, argb.applyAsInt(x, y));
		return new Pixels(width, height, PIXEL_FORMAT, HexFormat.of().formatHex(digest.digest()));
	}
	public static Binding bind(Path returnedPath, Path imageBase) throws IOException {
		byte[] png = Files.readAllBytes(returnedPath);
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(png));
		if (decoded == null) throw new IOException("Returned screenshot is not a decoded image");
		Path absolute = returnedPath.toAbsolutePath().normalize(), base = imageBase.toAbsolutePath().normalize();
		return new Binding(returnedPath.toString(), base.relativize(absolute).toString(), png.length, sha256(png),
			pixels(decoded.getWidth(), decoded.getHeight(), decoded::getRGB));
	}
	/** A reused filename never rebinds an old receipt; the consumer must recheck these saved hashes. */
	public static boolean stillMatches(Binding binding) throws IOException {
		Path path = Path.of(binding.returnedPath());
		Binding current = bind(path, path.toAbsolutePath().getParent());
		return current.pngBytes() == binding.pngBytes() && current.pngSha256().equals(binding.pngSha256())
			&& current.decodedPixels().equals(binding.decodedPixels());
	}
	public static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }
	private static MessageDigest digest() {
		try { return MessageDigest.getInstance("SHA-256"); }
		catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
	}
	private static void integer(MessageDigest digest, int value) {
		digest.update((byte) (value >>> 24)); digest.update((byte) (value >>> 16));
		digest.update((byte) (value >>> 8)); digest.update((byte) value);
	}
}
