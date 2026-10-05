package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFxRules;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Aura's hit-stop, on the client only: as a heavy blow lands, the striker and the one struck hold still on this screen for a few
 * frames (milliseconds of real time, {@link AuraFxRules.Weight#hitStop}), the blade bitten in, then carry on. Nothing in the game
 * waits: the world ticks on and only how the two are drawn holds. Where each stood and how it was posed (its place, its turn, its
 * walk, its swing) is caught the first time it's drawn in the hold and laid over its look every frame after (see
 * {@code mixin.EntityRenderDispatcherHitStopMixin}); everything else (what it holds, its light) stays live. A held foe catches up
 * to where its knockback took it as the moment ends. Your own first-person swing holds the same way (it's drawn from your own
 * render state).
 *
 * <p>Only the striker's and a struck player's own clients hold (an onlooker sees the flash, not the stop); stops close together
 * are one ({@link AuraFxRules#HIT_STOP_GAP}), and the player's impact setting shortens or removes them.</p>
 */
public final class HitStop {
	private HitStop() {}

	/** How a held entity stood and was posed as its hold began. */
	private record Pose(double x, double y, double z, float age, float bodyRot, float yRot, float xRot, float walk, float walkSpeed, float swing,
			dev.wildercord.client.MastersArtPose.Frame art, dev.wildercord.client.auraworld.MasterModel.Frame master,
			dev.wildercord.client.combat.ArticulatedCombat.Frame articulated) {
		static Pose of(EntityRenderState state) {
			float bodyRot = 0;
			float yRot = 0;
			float xRot = 0;
			float walk = 0;
			float walkSpeed = 0;
			float swing = 0;
			if (state instanceof LivingEntityRenderState living) {
				bodyRot = living.bodyRot;
				yRot = living.yRot;
				xRot = living.xRot;
				walk = living.walkAnimationPos;
				walkSpeed = living.walkAnimationSpeed;
			}
			if (state instanceof ArmedEntityRenderState armed) {
				swing = armed.swingAnimation;
			}
			return new Pose(state.x, state.y, state.z, state.ageInTicks, bodyRot, yRot, xRot, walk, walkSpeed, swing,
				state.getData(dev.wildercord.client.MastersArtPose.FRAME), state.getData(dev.wildercord.client.auraworld.MasterModel.FRAME),
				state.getData(dev.wildercord.client.combat.ArticulatedCombat.FRAME));
		}

		boolean sameActivation(EntityRenderState state) {
			var liveArt = state.getData(dev.wildercord.client.MastersArtPose.FRAME);
			var liveMaster = state.getData(dev.wildercord.client.auraworld.MasterModel.FRAME);
			var liveRig = state.getData(dev.wildercord.client.combat.ArticulatedCombat.FRAME);
			return (art == null ? liveArt == null : art.sameActivation(liveArt))
				&& (master == null ? liveMaster == null : master.sameActivation(liveMaster))
				&& (articulated == null ? liveRig == null : articulated.sameActivation(liveRig));
		}

		void onto(EntityRenderState state) {
			// Cosmetic palettes freeze with the base pose, but cancellation/new activation wins.
			if (art != null && art.sameActivation(state.getData(dev.wildercord.client.MastersArtPose.FRAME)))
				state.setData(dev.wildercord.client.MastersArtPose.FRAME, art);
			if (master != null && master.sameActivation(state.getData(dev.wildercord.client.auraworld.MasterModel.FRAME)))
				state.setData(dev.wildercord.client.auraworld.MasterModel.FRAME, master);
			if (articulated != null && articulated.sameActivation(state.getData(dev.wildercord.client.combat.ArticulatedCombat.FRAME)))
				state.setData(dev.wildercord.client.combat.ArticulatedCombat.FRAME, articulated);
			state.x = x;
			state.y = y;
			state.z = z;
			state.ageInTicks = age;
			if (state instanceof LivingEntityRenderState living) {
				living.bodyRot = bodyRot;
				living.yRot = yRot;
				living.xRot = xRot;
				living.walkAnimationPos = walk;
				living.walkAnimationSpeed = walkSpeed;
			}
			if (state instanceof ArmedEntityRenderState armed) {
				armed.swingAnimation = swing;
			}
		}
	}

	/** A held entity: until when (nanoseconds) and how it stood as the hold began (null until it's next drawn). */
	private static final class Held {
		final long until;
		Pose pose;
		java.util.UUID identity;

		Held(long until) {
			this.until = until;
		}
	}

	private static final Map<Integer, Held> HELD = new HashMap<>();
	private static long lastStart = Long.MIN_VALUE / 4;
	private static int stops;

	/** Holds these entities (by network id; -1 is skipped) for {@code millis} of real time, unless a hold began just now. */
	public static void hold(int millis, int... entities) {
		if (millis <= 0) {
			return;
		}
		long now = System.nanoTime();
		HELD.values().removeIf(held -> now >= held.until);
		if ((now - lastStart) / 1_000_000L < AuraFxRules.HIT_STOP_GAP) {
			return;
		}
		lastStart = now;
		stops++;
		long until = now + millis * 1_000_000L;
		for (int id : entities) {
			if (id >= 0) {
				HELD.put(id, new Held(until));
			}
		}
	}

	/** An entity's look was just made for this frame: while it's held, it stands and is posed as it was when the hold began. */
	public static void extracted(Entity entity, EntityRenderState state) {
		if (HELD.isEmpty() || state == null) {
			return;
		}
		Held held = HELD.get(entity.getId());
		if (held == null) {
			return;
		}
		if (System.nanoTime() >= held.until || entity.isRemoved() || !entity.isAlive()
			|| held.identity != null && !held.identity.equals(entity.getUUID())) {
			HELD.remove(entity.getId());
			return;
		}
		if (held.pose == null) {
			held.identity = entity.getUUID();
			held.pose = Pose.of(state);
		} else if (!held.pose.sameActivation(state)) {
			HELD.remove(entity.getId());
		} else {
			held.pose.onto(state);
		}
	}

	/** Whether anything is held now. */
	public static boolean holding() {
		long now = System.nanoTime();
		HELD.values().removeIf(h -> now >= h.until);
		return !HELD.isEmpty();
	}

	/** Lets go of everything (leaving a world). */
	public static void clear() {
		HELD.clear();
		lastStart = Long.MIN_VALUE / 4;
	}

	/** How many hit-stops began since the game started (the game tests read it). */
	public static int stops() {
		return stops;
	}
}
