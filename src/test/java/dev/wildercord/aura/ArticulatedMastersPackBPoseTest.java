package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.MastersPackBAnimation;
import dev.wildercord.aura.world.MastersRules;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Starlit, Hourglass and Crimson signatures: Classic and articulated forms strike on the server's own beats. */
class ArticulatedMastersPackBPoseTest {
	private static final MastersRules.Move[] MOVES = {MastersRules.Move.STARLIT_CONSTELLATION, MastersRules.Move.HOURGLASS_REWIND,
		MastersRules.Move.CRIMSON_FRENZY};

	@Test
	void theThreeSignatureIdsMatchTheWireAndBothRigs() {
		assertEquals(MASTER_STARLIT_CONSTELLATION, MastersPackBAnimation.STARLIT_CONSTELLATION);
		assertEquals(MASTER_HOURGLASS_REWIND, MastersPackBAnimation.HOURGLASS_REWIND);
		assertEquals(MASTER_CRIMSON_FRENZY, MastersPackBAnimation.CRIMSON_FRENZY);
		for (MastersRules.Move move : MOVES) {
			int id = move.ordinal() + 1;
			assertTrue(supportsMasterPackB(id), move.name());
			assertFalse(supportsMaster(id), "The shared NPC set stays unchanged");
			assertTrue(MastersPackBAnimation.owns(id));
		}
		assertFalse(supportsMaster(15));
		assertFalse(MastersPackBAnimation.owns(11));
		assertFalse(MastersPackBAnimation.owns(15));
		assertFalse(supportsMasterPackB(15));
		assertFalse(supportsMasterPackB(19));
		assertFalse(MastersPackBAnimation.owns(19));
		assertNull(MastersPackBAnimation.sample(9, 10, 40, 1, 47), "Other forms keep their own Classic clip");
	}

	@Test
	void everyBeatIsActiveOnTheStrikeTickAndTheFormEndsCleanly() {
		for (MastersRules.Move move : MOVES) {
			int id = move.ordinal() + 1;
			float[] beats = MastersPackBAnimation.beats(id);
			assertTrue(beats.length >= 2, move.name());
			assertEquals(move.tell, beats[beats.length - 1], 1e-6, "The last beat closes the tell");
			for (float beat : beats) {
				assertEquals(Phase.ACTIVE, sampleMaster(id, beat, move.tell, 1, move.recovery, false).phase(), move.name() + " " + beat);
				assertEquals(Phase.ACTIVE, sampleMaster(id, beat + .5F, move.tell, 1, move.recovery, true).phase());
			}
			assertEquals(Phase.WINDUP, sampleMaster(id, beats[0] - 1, move.tell, 1, move.recovery, false).phase());
			assertEquals(Phase.RECOVERY, sampleMaster(id, move.tell + 2, move.tell, 1, move.recovery, false).phase());
			assertSame(NONE, sampleMaster(id, move.tell + 1 + move.recovery, move.tell, 1, move.recovery, false));
			assertSame(NONE, sampleMaster(id, -1, move.tell, 1, move.recovery, false));
			assertSame(NONE, sampleMaster(id, Float.NaN, move.tell, 1, move.recovery, false));
			assertSame(MasterAnimationRules.NONE, MastersPackBAnimation.sample(id, move.tell + 1 + move.recovery, move.tell, 1, move.recovery));
			assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(id, Float.NaN, move.tell, 1, move.recovery));
		}
	}

	@Test
	void posesAreFiniteFullWeightAtTheBeatsAndFadeInFromRest() {
		for (MastersRules.Move move : MOVES) {
			int id = move.ordinal() + 1;
			for (float age = 0; age < move.tell + 1 + move.recovery; age += .25F) {
				var pose = sampleMaster(id, age, move.tell, 1, move.recovery, false);
				assertTrue(pose.weight() >= 0 && pose.weight() <= 1, move.name() + " " + age);
				for (Joint joint : Joint.values()) {
					var t = pose.local(joint);
					assertTrue(Float.isFinite(t.x()) && Float.isFinite(t.y()) && Float.isFinite(t.z()), joint + " at " + age);
				}
				var classic = MasterAnimationRules.sample(id, age, move.tell, 1, move.recovery);
				assertNotNull(classic);
				assertTrue(classic.weight() >= 0 && classic.weight() <= 1);
			}
			assertEquals(0, sampleMaster(id, 0, move.tell, 1, move.recovery, false).weight(), 1e-6);
			for (float beat : MastersPackBAnimation.beats(id)) {
				assertEquals(1, sampleMaster(id, beat, move.tell, 1, move.recovery, false).weight(), 1e-6);
				assertEquals(1, MasterAnimationRules.sample(id, beat, move.tell, 1, move.recovery).weight(), 1e-6);
			}
		}
	}

	@Test
	void theFrenzyEndsFacingForwardAfterItsFullTurn() {
		int id = MASTER_CRIMSON_FRENZY;
		var move = MastersRules.Move.CRIMSON_FRENZY;
		var before = sampleMaster(id, move.tell - 7, move.tell, 1, move.recovery, false).local(Joint.PELVIS);
		var mid = sampleMaster(id, move.tell - 3, move.tell, 1, move.recovery, false).local(Joint.PELVIS);
		assertNotEquals(before, mid, "The ring is announced by a visible turn");
		assertEquals(-(float) (Math.PI * 2), MastersPackBAnimation.sample(id, move.tell + 1, move.tell, 1, move.recovery).rootYaw(), 1e-4);
	}
}
