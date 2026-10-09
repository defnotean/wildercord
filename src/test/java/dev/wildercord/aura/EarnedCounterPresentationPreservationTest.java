package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exact Java-25 base fingerprint: IDs 0–19, all local/world body/view transforms, both hand
 * sockets, Classic poses/blade tilt, phases and both hands every 0.125 tick.
 * Baseline 35e56e1d7a995a2674c90773065175b8a8cfad58 was compiled independently before these additions.
 */
class EarnedCounterPresentationPreservationTest {
	@Test
	void allPriorPlayerClipsRemainBitForBitUnchanged() throws Exception {
		assertEquals("3ddf391dd42b0976b79b823b954ef6faeb2d5d7be2e7dca0b471d999bc8bdd07", fingerprint());
	}

	private static String fingerprint() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		for (int move = 0; move <= 19; move++) for (boolean left : new boolean[] {false, true}) {
			int windup = move < 3 ? MastersArtRules.move(move).windup() : MastersStyleRules.animation(move).windup();
			int recovery = move < 3 ? MastersArtRules.move(move).recovery() : MastersStyleRules.animation(move).recovery();
			for (int step = 0; step <= (windup + recovery) * 8; step++) {
				float age = step / 8F;
				// Styles 5-14 were frozen on the fallback; their authored forms have dedicated tests.
				var pose = move >= CRACKLE && move <= BLOSSOM_FALL ? NONE : samplePlayer(move, age, windup, recovery, left);
				var view = view(pose, left);
				add(digest, pose.weight(), pose.phase().ordinal(), view.origin().x(), view.origin().y(), view.origin().z());
				for (var joint : Joint.values()) {
					var a = pose.local(joint); var b = view.local(joint);
					add(digest, a.x(), a.y(), a.z(), a.rotation().x(), a.rotation().y(), a.rotation().z(),
						b.x(), b.y(), b.z(), b.rotation().x(), b.rotation().y(), b.rotation().z());
					add(digest, pose.world(joint).values()); add(digest, view.world(joint).values());
				}
				var classic = MastersArtAnimation.sample(move, age, windup, recovery);
				digest.update(classic.toString().getBytes(StandardCharsets.UTF_8));
				add(digest, MastersArtAnimation.bladeTilt(move, age, windup, classic.weight()));
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void add(MessageDigest digest, float... values) {
		for (float value : values) digest.update(ByteBuffer.allocate(4).putFloat(value).array());
	}
}
