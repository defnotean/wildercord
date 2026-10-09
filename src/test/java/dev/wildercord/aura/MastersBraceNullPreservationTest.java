package dev.wildercord.aura;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden preservation evidence for the player clips which predate Unmoved / Null Parry.
 * Expected values come exclusively from commit dcf242d9cd1fd8a65226f28545ab353506dc3696,
 * never from the working-tree implementation. See tools/freeze_brace_null_baseline.py
 * and the matching resource manifest for source hashes and the complete sample definition.
 *
 * Each row has separate SHA-256 digests for Classic body, Classic first person, articulated
 * world and articulated first person. All floats are serialized as exact IEEE-754 bits;
 * the articulated digests include every local transform and every column-major matrix,
 * including both weapon sockets. Unsupported old articulated clips freeze their fallback.
 * This is pure pose/geometry evidence, not native rendering or pixel evidence.
 */
class MastersBraceNullPreservationTest {
	private static final String BASELINE = "/dev/wildercord/aura/brace-null-legacy-poses.tsv.gz";
	private static final String COMMIT = "dcf242d9cd1fd8a65226f28545ab353506dc3696";
	private static final String UNCOMPRESSED_SHA256 = "89d7669ea689c5a381c4fd612d16026e340b95b5b4f445db154b1b775214a8b1";
	private static final String[] GROUPS = {"Classic body", "Classic first person", "articulated world", "articulated first person"};
	private static final float[][] VIEW_INPUTS = {{0, 0, 0}, {.35F, 27, -17}, {1, -71, 62}, {-.5F, 0, 0}, {1.5F, 0, 0}};

	@Test
	void frozenReferenceAndCoverageRemainIntact() throws Exception {
		byte[] bytes = referenceBytes();
		assertEquals(UNCOMPRESSED_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
			"The pre-feature reference must not be regenerated from edited production sources");
		List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
		assertEquals("# brace-null-preservation-v1 " + COMMIT, lines.getFirst());
		assertEquals("# move\twindup\trecovery\tleftHanded\tageHex\tclassicBody\tclassicView\tarticulatedWorld\tarticulatedView", lines.get(1));
		boolean[][] hands = new boolean[24][2];
		boolean[] invalid = new boolean[24], expired = new boolean[24], fractional = new boolean[24], alternate = new boolean[24];
		for (String line : lines.subList(2, lines.size())) {
			String[] c = line.split("\t");
			assertEquals(9, c.length);
			int move = Integer.parseInt(c[0]), windup = Integer.parseInt(c[1]), recovery = Integer.parseInt(c[2]);
			float age = Float.parseFloat(c[4]);
			hands[move][Boolean.parseBoolean(c[3]) ? 1 : 0] = true;
			invalid[move] |= !Float.isFinite(age) || age < 0;
			expired[move] |= age == windup + recovery;
			fractional[move] |= Float.isFinite(age) && age > 0 && age != (int) age;
			alternate[move] |= windup == 60 && recovery == 120;
		}
		for (int move = 0; move < 24; move++) {
			assertTrue(hands[move][0] && hands[move][1] && invalid[move] && expired[move] && fractional[move] && alternate[move],
				"Incomplete immutable reference for old player clip " + move);
		}
		assertTrue(lines.size() > 10_000, "Dense canonical and boundary samples must not be pruned");
	}

	@ParameterizedTest(name = "old player clip {0} preserves all four pose palettes")
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23})
	void everyOldPlayerClipMatchesTheImmutableReference(int selectedMove) throws Exception {
		int samples = 0;
		try (var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(referenceBytes()), StandardCharsets.UTF_8))) {
			for (String line; (line = reader.readLine()) != null;) {
				if (line.startsWith("#") || !line.startsWith(selectedMove + "\t")) continue;
				String[] c = line.split("\t");
				int windup = Integer.parseInt(c[1]), recovery = Integer.parseInt(c[2]);
				boolean left = Boolean.parseBoolean(c[3]);
				float age = Float.parseFloat(c[4]);
				String[] actual = snapshot(selectedMove, age, windup, recovery, left);
				// Styles 5-14 gained authored articulated forms after this fixture froze their fallback;
				// their Classic groups stay pinned and the new forms have dedicated opening-style tests.
				int groups = selectedMove >= ArticulatedCombatPose.CRACKLE && selectedMove <= ArticulatedCombatPose.BLOSSOM_FALL ? 2 : GROUPS.length;
				for (int group = 0; group < groups; group++)
					assertEquals(c[5 + group], actual[group], GROUPS[group] + " changed for clip " + selectedMove
						+ ", age=" + c[4] + ", windup=" + windup + ", recovery=" + recovery + ", left=" + left);
				samples++;
			}
		}
		assertTrue(samples > 400, "Missing samples for old player clip " + selectedMove);
	}

	private static byte[] referenceBytes() throws IOException {
		return FrozenPoseTextFixture.load(MastersBraceNullPreservationTest.class, BASELINE,
			1_593_774, "57fe71c19d79e02c1faf7a36aaad16bfccfd032994c55b8424fa526b969b2226", 5_239_773, UNCOMPRESSED_SHA256);
	}

	/** Export entry point; the freezer compiles this encoder against only immutable git-show sources. */
	public static void main(String[] args) throws Exception {
		if (args.length != 1 || !args[0].equals("--export-frozen")) throw new IllegalArgumentException("Use the freezer script");
		System.out.println("# brace-null-preservation-v1 " + COMMIT);
		System.out.println("# move\twindup\trecovery\tleftHanded\tageHex\tclassicBody\tclassicView\tarticulatedWorld\tarticulatedView");
		for (int move = 0; move < 24; move++) {
			int[] canonical = canonicalTiming(move);
			for (boolean left : new boolean[] {false, true}) {
				for (int[] timing : new int[][] {canonical, {1, 1}, {20, 30}, {60, 120}})
					for (float age : ages(move, timing[0], timing[1], timing == canonical))
						export(move, age, timing[0], timing[1], left);
				for (int[] invalid : new int[][] {{0, 12}, {-1, 12}, {61, 12}, {4, 0}, {4, -1}, {4, 121},
					{Integer.MAX_VALUE, 12}, {4, Integer.MAX_VALUE}, {Integer.MIN_VALUE, Integer.MIN_VALUE}})
					for (float age : new float[] {0, 1, Float.NaN}) export(move, age, invalid[0], invalid[1], left);
			}
		}
	}

	private static int[] canonicalTiming(int move) {
		if (move < 3) { var rule = MastersArtRules.move(move); return new int[] {rule.windup(), rule.recovery()}; }
		var rule = MastersStyleRules.animation(move);
		return new int[] {rule.windup(), rule.recovery()};
	}

	private static TreeSet<Float> ages(int move, int windup, int recovery, boolean dense) {
		TreeSet<Float> ages = new TreeSet<>();
		float end = windup + recovery;
		for (float age : new float[] {-1, -.001F, -Float.MIN_VALUE, -0.0F, 0, Float.MIN_VALUE,
			Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, end, end + 1, 200}) ages.add(age);
		if (dense) for (int i = 0; i <= end * 8; i++) ages.add(i / 8F);
		for (int i = 0; i <= 16; i++) ages.add(end * i / 16F);
		List<Float> boundaries = new ArrayList<>(List.of(0F, windup * .65F, (float) windup,
			windup + 1F, windup + Math.min(4, recovery * .25F), end));
		if (move == 5) {
			boundaries.add(windup + (float) ArtRules.CRACKLE_GAP);
			boundaries.add(windup + ArtRules.CRACKLE_GAP * 2F);
			boundaries.add(windup + Math.min(recovery * .8F, ArtRules.CRACKLE_GAP * 2F + 2));
		}
		if (move == 11) {
			boundaries.add(windup + 3F);
			boundaries.add(windup + ArtRules.ECHO_DELAY - 3F);
			boundaries.add(windup + (float) ArtRules.ECHO_DELAY);
			boundaries.add(windup + Math.min(recovery * .8F, ArtRules.ECHO_DELAY + 2F));
		}
		if (move == 16) {
			float answer = Math.min(ArtRules.SKYFALL_DELAY, recovery * .65F);
			boundaries.add(windup + answer * .5F);
			boundaries.add(windup + answer);
			boundaries.add(windup + Math.min(recovery * .8F, answer + 2));
		}
		for (float edge : boundaries) {
			ages.add(edge); ages.add(Math.nextDown(edge)); ages.add(Math.nextUp(edge));
			ages.add(edge - .001F); ages.add(edge + .001F);
		}
		return ages;
	}

	private static void export(int move, float age, int windup, int recovery, boolean left) throws IOException {
		System.out.println(move + "\t" + windup + "\t" + recovery + "\t" + left + "\t" + Float.toHexString(age)
			+ "\t" + String.join("\t", snapshot(move, age, windup, recovery, left)));
	}

	private static String[] snapshot(int move, float age, int windup, int recovery, boolean left) throws IOException {
		var classic = MastersArtAnimation.sample(move, age, windup, recovery);
		Hash body = new Hash();
		body.flag(MastersArtAnimation.supports(move)); body.flag(classic == MastersArtAnimation.NONE);
		int[] timing = canonicalTiming(move); body.integer(timing[0]); body.integer(timing[1]);
		body.floats(classic.weight(), classic.lower(), classic.forward());
		for (var joint : new MastersArtAnimation.Joint[] {classic.body(), classic.head(), classic.sword(), classic.guard(), classic.frontLeg(), classic.rearLeg()})
			body.floats(joint.x(), joint.y(), joint.z());
		for (float[] pivot : new float[][] {{0, 0, 0}, {-5, 2, 0}, {5, 2, 0}}) {
			var p = MastersArtAnimation.pivot(classic, pivot[0], pivot[1], pivot[2], left);
			body.floats(p.x(), p.y(), p.z());
		}
		body.floats(MastersArtAnimation.bladeTilt(move, age, windup, classic.weight()));
		var bounded = MastersArtAnimation.boundedHead(classic.body(), classic.head(), classic.weight());
		body.floats(bounded.x(), bounded.y(), bounded.z());
		Hash first = new Hash();
		first.hand(classic.hand());
		for (float[] input : VIEW_INPUTS) {
			var view = MastersArtAnimation.view(classic, left, input[0], input[1], input[2]);
			first.hand(view.transform()); first.floats(view.grip().x(), view.grip().y(), view.grip().z());
		}
		var articulated = ArticulatedCombatPose.samplePlayer(move, age, windup, recovery, left);
		Hash world = new Hash();
		world.flag(ArticulatedCombatPose.supportsPlayer(move)); world.flag(articulated == ArticulatedCombatPose.NONE);
		world.floats(articulated.weight()); world.text(articulated.phase().name());
		for (var joint : ArticulatedCombatPose.Joint.values()) {
			world.text(joint.name()); world.text(joint.parent() == null ? "" : joint.parent().name());
			world.transform(joint.bind()); world.transform(articulated.local(joint)); world.floats(articulated.world(joint).values());
		}
		world.floats(articulated.socket(false).values()); world.floats(articulated.socket(true).values());
		var view = ArticulatedCombatPose.view(articulated, left);
		Hash camera = new Hash();
		camera.floats(view.weight()); camera.text(view.phase().name()); camera.vector(view.origin());
		for (var joint : ArticulatedCombatPose.Joint.values()) {
			camera.text(joint.name()); camera.transform(view.local(joint)); camera.floats(view.world(joint).values());
			camera.vector(view.cameraPoint(joint, 0, 0, 0)); camera.vector(view.cameraPoint(joint, 1, 2, -3));
		}
		camera.floats(view.socket(false).values()); camera.floats(view.socket(true).values());
		return new String[] {body.finish(), first.finish(), world.finish(), camera.finish()};
	}

	private static final class Hash {
		private final MessageDigest digest;
		private final DataOutputStream data;
		Hash() {
			try { digest = MessageDigest.getInstance("SHA-256"); }
			catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
			data = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
		}
		void flag(boolean value) throws IOException { data.writeBoolean(value); }
		void integer(int value) throws IOException { data.writeInt(value); }
		void text(String value) throws IOException { data.writeUTF(value); }
		void floats(float... values) throws IOException { for (float value : values) data.writeInt(Float.floatToIntBits(value)); }
		void hand(MastersArtAnimation.Hand hand) throws IOException { floats(hand.x(), hand.y(), hand.z(), hand.pitch(), hand.yaw(), hand.roll()); }
		void vector(ArticulatedCombatPose.Vec3 vector) throws IOException { floats(vector.x(), vector.y(), vector.z()); }
		void transform(ArticulatedCombatPose.Transform transform) throws IOException {
			floats(transform.x(), transform.y(), transform.z(), transform.rotation().x(), transform.rotation().y(), transform.rotation().z());
		}
		String finish() { return HexFormat.of().formatHex(digest.digest()); }
	}
}
