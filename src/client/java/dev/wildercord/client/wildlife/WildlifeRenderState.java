package dev.wildercord.client.wildlife;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What every wildlife model needs to pose and light itself, filled by its renderer each frame. Each creature reads only
 * its own fields; every blend runs 0 to 1, already eased by the creature on the client.
 */
public class WildlifeRenderState extends LivingEntityRenderState {
	/** A glimmerwing's colouring, a tortoise's garden. */
	public int variant;
	/** How strongly its glowing parts burn, 0 to 1. */
	public float glow;
	/** A per-creature offset, so a herd or a swarm doesn't move in step. */
	public float seed;

	/** Wings: the beat's phase (radians) and how hard they're beating. */
	public float flapPhase;
	public float flapStrength;
	/** A skyray's lean into its turn (radians) and its pitch. */
	public float bank;
	public float pitch;

	/** A lumen stag grazing, watching someone, bowing to shed; and whether today's antler is already shed. */
	public float graze;
	public float watch;
	public float bow;
	public boolean shed;

	/** A tortoise drawn into its shell. */
	public float hide;
	/** A cinderfox sitting. */
	public float sit;
	/** A newt settled beneath a reed roof. */
	public float rest;
 public float claws,strike,settle;
	/** A rimehare in the air (0 on the ground, 1 at the top of a bound), whether it's rising, and sitting up on alert. */
	public float air;
	public float rise;
	public float alert;
	/** A reefback swimming (0 ashore, 1 in water), and a delver digging. */
	public float swim;
	public float dig;

	/** A tortoise's garden: up to three plants, each a block model drawn small on its shell. */
	public final BlockModelRenderState[] garden = {new BlockModelRenderState(), new BlockModelRenderState(), new BlockModelRenderState()};
}
