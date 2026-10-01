package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Mana Ooze, in two models drawn one inside the other: the inside (a smaller cube of denser jelly, the turning core of
 * raw mana at its heart, and a mote circling it) and the clear outside cube. Both are drawn translucent; the core and mote
 * glow besides. The fuller it is, the faster the core turns and the more the jelly swells and quivers.
 *
 * <p>Laid out on a 64x32 skin (see {@code mana_ooze} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   outer (0,0) 8x8x8     inner (0,16) 6x6x6    core (32,0) 4x4x4     mote (48,0) 2x2x2
 * </pre>
 */
public class ManaOozeModel extends EntityModel<ManaOozeModel.State> {
	/** A Mana Ooze's look for one frame: a slime's size and squish, how full it is, and how lately it drank. */
	public static class State extends SlimeRenderState {
		/** 0 empty to 1 about to grow or burst. */
		public float fill;
		/** 1 the moment it drinks a spell, fading over half a second. */
		public float flash;
	}

	private final ModelPart inner;
	private final ModelPart core;
	private final ModelPart mote;
	private final ModelPart outer;

	public ManaOozeModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.inner = root.hasChild("inner") ? root.getChild("inner") : null;
		this.core = root.hasChild("core") ? root.getChild("core") : null;
		this.mote = core != null ? core.getChild("mote") : null;
		this.outer = root.hasChild("outer") ? root.getChild("outer") : null;
	}

	/** The inside: denser jelly, the core, and its mote. */
	public static LayerDefinition createInner() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("inner", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(0.0F, 20.0F, 0.0F));
		PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
			PartPose.offset(0.0F, 20.0F, 0.0F));
		core.addOrReplaceChild("mote", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(3.2F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	/** The clear outside. */
	public static LayerDefinition createOuter() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("outer", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 20.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float fill = state.fill;
		// Nearly full, it quivers: the tell before it grows, or bursts.
		float quiver = Math.max(0, fill - 0.6F) / 0.4F;
		float jitterX = Mth.sin(t * 2.3F) * 0.25F * quiver;
		float jitterZ = Mth.cos(t * 2.9F) * 0.25F * quiver;
		if (outer != null) {
			float swell = 1 + 0.14F * fill + 0.06F * state.flash;
			outer.xScale = swell;
			outer.yScale = swell;
			outer.zScale = swell;
			outer.y = 20.0F - 4 * (swell - 1);
			outer.x = jitterX;
			outer.z = jitterZ;
		}
		if (inner != null) {
			inner.yRot = Mth.sin(t * 0.05F) * 0.25F;
			inner.x = jitterX * 0.6F;
			inner.z = jitterZ * 0.6F;
		}
		if (core != null) {
			float spin = t * (0.05F + 0.15F * fill);
			core.yRot = spin;
			core.xRot = spin * 0.6F;
			core.zRot = Mth.sin(t * 0.07F) * 0.4F;
			float s = 0.85F + 0.35F * fill + 0.25F * state.flash;
			core.xScale = s;
			core.yScale = s;
			core.zScale = s;
			core.y = 20.0F + Mth.sin(t * 0.1F) * 0.4F;
		}
	}
}
