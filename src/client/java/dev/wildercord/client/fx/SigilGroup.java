package dev.wildercord.client.fx;

import net.minecraft.client.particle.ParticleRenderType;

/**
 * Where magic circles are kept and drawn. Ordinary particles are skipped when their centre is off
 * screen, but a circle is big: one lying under your feet has its centre below the view while most
 * of it is in plain sight. On 1.21.8 the particle engine draws every live particle without culling
 * any of them by view, so a circle is never cut off that way; what is left here is the group the
 * engine batches them under and the extent interface the circles still carry.
 */
public final class SigilGroup {
	private SigilGroup() {}

	/**
	 * The group the circles are drawn in. 1.21.8 has no particle groups to register (that arrives with
	 * 1.21.9), so this is the engine's own custom type: it hands each particle the whole buffer source,
	 * which is what lets a circle pick a different blend per quad.
	 */
	public static final ParticleRenderType TYPE = ParticleRenderType.CUSTOM;

	/** A particle that knows its whole extent: a circle's centre and how far it reaches from it. */
	public interface Extent {
		double centreX();

		double centreY();

		double centreZ();

		double reach();
	}
}
