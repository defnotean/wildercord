package dev.wildercord.client.fx;

import dev.wildercord.Wildercord;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Where magic circles are kept and drawn. Ordinary particles are skipped when their centre is off
 * screen, but a circle is big: one lying under your feet has its centre below the view while most
 * of it is in plain sight. Here each circle is culled by its whole extent instead.
 */
public class SigilGroup extends ParticleGroup<SingleQuadParticle> {
	public static final ParticleRenderType TYPE = new ParticleRenderType(Wildercord.id("sigils").toString(), "WS");

	/** A particle that knows its whole extent: a circle's centre and how far it reaches from it. */
	public interface Extent {
		double centreX();

		double centreY();

		double centreZ();

		double reach();
	}

	private final QuadParticleRenderState state = new QuadParticleRenderState();

	public SigilGroup(ParticleEngine engine) {
		super(engine);
	}

	@Override
	public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partial) {
		for (SingleQuadParticle particle : particles) {
			if (!(particle instanceof Extent e)) {
				particle.extract(state, camera, partial);
				continue;
			}
			double r = e.reach();
			if (frustum.isVisible(new AABB(e.centreX() - r, e.centreY() - r, e.centreZ() - r, e.centreX() + r, e.centreY() + r, e.centreZ() + r))) {
				particle.extract(state, camera, partial);
			}
		}
		return state;
	}
}
