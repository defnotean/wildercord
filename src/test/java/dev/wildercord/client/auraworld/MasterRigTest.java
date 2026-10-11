package dev.wildercord.client.auraworld;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Sword Master rig as shipped: it loads, every clip the motion asks for is there, and no clip sinks it into the ground. */
class MasterRigTest {
	private static final String[] CLIPS = {"idle", "walk", "sweep", "thrust", "crescent", "break_cast", "vanish", "victory", "flourish"};

	@Test
	void loadsWithEveryClip() {
		MasterRig rig = MasterRig.get();
		assertNotNull(rig, "rig loads");
		for (String name : CLIPS) assertNotNull(rig.clip(name), name);
		for (String name : new String[] {"sweep", "thrust", "crescent", "break_cast", "vanish"}) {
			MasterRig.Clip clip = rig.clip(name);
			assertTrue(clip.strike() < clip.impact() && clip.impact() < clip.hold() && clip.hold() <= clip.length(), name + " markers in order");
		}
		for (int i = 0; i < rig.bones.length; i++) assertTrue(rig.bones[i].parent() < i, "parents come before children");
	}

	@Test
	void neverSinksBelowTheFloor() {
		MasterRig rig = MasterRig.get();
		assertNotNull(rig);
		Vector3f v = new Vector3f();
		for (String name : CLIPS) {
			MasterRig.Clip clip = rig.clip(name);
			for (int frame = 0; frame <= Math.round(clip.length() * 20); frame++) {
				float t = frame / 20F;
				Matrix4f[] placed = rig.matrices(rig.sample(clip, t, rig.newPose()));
				for (int bone = 0; bone < placed.length; bone++) {
					String part = rig.bones[bone].name();
					// The victory plants its sword point in the ground.
					if (name.equals("victory") && part.equals("sword")) continue;
					// Soles sit a little into the ground, and a bent knee's shin corner may graze it.
					float floor = part.startsWith("shin") || part.startsWith("foot") ? -1.0F : -0.3F;
					float[] quads = rig.bones[bone].quads();
					for (int q = 0; q < quads.length; q += MasterRig.QUAD)
						for (int c = 0; c < 4; c++) {
							int k = q + 5 * c;
							float y = placed[bone].transformPosition(quads[k], quads[k + 1], quads[k + 2], v).y;
							assertTrue(y >= floor, name + " at " + t + "s sinks " + part + " to " + y);
						}
				}
			}
		}
	}

	@Test
	void attacksLandTheirStrikeOnTheServersHitTick() {
		MasterRig rig = MasterRig.get();
		assertNotNull(rig);
		MasterRig.Clip sweep = rig.clip("sweep");
		assertEquals(0, MasterMotion.warp(sweep, 0, 7, 1, 7, 0), 1e-5);
		assertEquals(sweep.strike(), MasterMotion.warp(sweep, 7, 7, 1, 7, 0), 1e-5);
		assertEquals(sweep.impact(), MasterMotion.warp(sweep, 8, 7, 1, 7, 0), 1e-5);
		assertEquals(sweep.hold(), MasterMotion.warp(sweep, 15, 7, 1, 7, 0), 1e-5);
		float last = -1;
		for (float e = 0; e <= 16; e += 0.25F) {
			float t = MasterMotion.warp(sweep, e, 7, 1, 7, 0);
			assertTrue(t >= last, "clip time never runs backwards");
			last = t;
		}
	}
}
