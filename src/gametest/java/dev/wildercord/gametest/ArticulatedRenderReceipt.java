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
			boolean verified, List<String> failures) {
		public Report { stages = List.copyOf(stages); failures = List.copyOf(failures); }
	}

	public static boolean inScope(String name) { return name != null && name.startsWith(PREFIX); }
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
			return new Report(1, identity, "fabric_test_screenshot", nativeLaunchNonce, stages, copy, pixels, binding, rendered, matched,
				matched && unpaused, "unverified", true, verified, failures);
		}
		private void validate(Copy snapshot) {
			if (snapshot.extractionSequence() <= 0) reject("missing_extraction");
			if (snapshot.renderSequence() <= 0) reject("missing_render");
			if (!(snapshot.extractionSequence() < snapshot.renderSequence() && snapshot.renderSequence() < snapshot.copySequence())) reject("unordered_capture_stages");
			if (snapshot.target().width() <= 0 || snapshot.target().height() <= 0) reject("invalid_target_dimensions");
			if (snapshot.target().mipLevel() != 0) reject("unexpected_copy_mip");
			if (!String.valueOf(snapshot.target().targetGeneration()).equals(snapshot.observation().get("renderTargetGeneration"))
				|| !String.valueOf(snapshot.target().textureGeneration()).equals(snapshot.observation().get("renderTextureGeneration"))) reject("render_copy_target_mismatch");
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
	}

	private static String renderedPhase(Copy copy) {
		if (copy == null) return "unknown";
		String kind = "true".equals(copy.observation().get("firstPerson")) ? "view_deferred" : "body_deferred";
		List<String> phases = copy.passes().stream().filter(p -> p.kind().equals(kind) && p.palette() != null
			&& "true".equals(p.attributes().get("segmentedVisible"))).map(p -> p.palette().phase()).distinct().toList();
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
