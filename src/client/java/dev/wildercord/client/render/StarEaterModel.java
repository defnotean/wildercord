package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Star-Eater: a floating knot of void with one great eye, a crown of four spikes, four tendrils
 * trailing under it, and six star shards circling it. Its shield up, the shards whirl in close and
 * fast; broken, they drift far out and sag, slow; between phases they spin in tight around it; as it
 * dies they fall in on it and the knot shrinks away to nothing. Casting, it swells and throbs.
 *
 * <p>Laid out on a 64x64 skin (see {@code star_eater_texture} in {@code tools/dungeon_assets.py}):</p>
 * <pre>
 *   core (0,0) 12x12x12    shard (48,0) 3x6x3    spike (48,10) 2x6x2
 *   tendril (0,24) 2x8x2   tendril tip (8,24) 2x7x2
 * </pre>
 */
public class StarEaterModel extends EntityModel<DungeonBossRenderState> {
	private static final int SHARDS = 6;
	private static final int TENDRILS = 4;

	private final ModelPart core;
	private final ModelPart orbit;
	private final ModelPart[] shards = new ModelPart[SHARDS];
	private final ModelPart[] tendrils = new ModelPart[TENDRILS];
	private final ModelPart[] tips = new ModelPart[TENDRILS];

	public StarEaterModel(ModelPart root) {
		super(root);
		this.core = root.getChild("core");
		this.orbit = root.getChild("orbit");
		for (int i = 0; i < SHARDS; i++) {
			shards[i] = orbit.getChild("shard_" + i);
		}
		for (int i = 0; i < TENDRILS; i++) {
			tendrils[i] = core.getChild("tendril_" + i);
			tips[i] = tendrils[i].getChild("tip");
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F),
			PartPose.offset(0.0F, 11.0F, 0.0F));
		// A crown of spikes, leaning out.
		for (int i = 0; i < 4; i++) {
			float x = i % 2 == 0 ? -3.5F : 3.5F;
			float z = i < 2 ? -3.5F : 3.5F;
			core.addOrReplaceChild("spike_" + i, CubeListBuilder.create().texOffs(48, 10).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(x, -5.0F, z, z < 0 ? -0.35F : 0.35F, 0.0F, x < 0 ? 0.35F : -0.35F));
		}
		// Tendrils trailing beneath, in two joints.
		for (int i = 0; i < TENDRILS; i++) {
			float x = i % 2 == 0 ? -3.0F : 3.0F;
			float z = i < 2 ? -3.0F : 3.0F;
			PartDefinition tendril = core.addOrReplaceChild("tendril_" + i, CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
				PartPose.offset(x, 5.0F, z));
			tendril.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				PartPose.offset(0.0F, 7.5F, 0.0F));
		}
		// The shards of its shield; placed each frame.
		PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0.0F, 11.0F, 0.0F));
		for (int i = 0; i < SHARDS; i++) {
			orbit.addOrReplaceChild("shard_" + i, CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float g = state.guarded;
		float e = state.exposed;
		float sh = state.shifting;
		float dying = Math.min(1, state.deathTime / 50.0F);

		// The knot bobs and turns slowly; casting, it throbs; dying, it shrinks to nothing.
		float bob = Mth.sin(t * 0.07F) * 1.2F;
		core.y = 11.0F + bob + e * 1.5F;
		core.xRot = Mth.sin(t * 0.05F) * 0.08F + e * 0.35F;
		core.zRot = Mth.cos(t * 0.04F) * 0.06F + sh * Mth.sin(t * 1.4F) * 0.08F;
		core.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
		float scale = (1.0F + 0.08F * c * Mth.sin(t * 0.9F) + 0.1F * sh) * (1 - 0.85F * dying);
		core.xScale = scale;
		core.yScale = scale;
		core.zScale = scale;

		for (int i = 0; i < TENDRILS; i++) {
			float phase = t * 0.09F + i * 1.6F;
			tendrils[i].xRot = Mth.sin(phase) * 0.25F + (i < 2 ? 0.2F : -0.2F) * (1 + e);
			tendrils[i].zRot = Mth.cos(phase * 0.8F) * 0.2F;
			tips[i].xRot = Mth.sin(phase - 0.8F) * 0.45F;
		}

		// The shards: whirling close with its shield up, drifting wide and low with it broken, spinning tight between phases.
		orbit.y = core.y;
		float radius = Mth.lerp(g, 20.0F, 12.5F) + 6.0F * e;
		radius = Mth.lerp(sh, radius, 8.0F) * (1 - dying);
		float speed = 0.02F + 0.07F * g + 0.2F * sh;
		orbit.yRot = t * speed;
		for (int i = 0; i < SHARDS; i++) {
			ModelPart shard = shards[i];
			float at = Mth.TWO_PI * i / SHARDS;
			float tilt = Mth.sin(t * 0.05F + i) * (0.35F + 0.4F * e);
			shard.x = Mth.sin(at) * radius;
			shard.z = Mth.cos(at) * radius;
			shard.y = Mth.sin(t * 0.08F + i * 1.3F) * 2.0F + tilt * 4.0F + 7.0F * e + dying * 6.0F;
			shard.yRot = at + t * 0.1F;
			shard.xRot = tilt + c * Mth.sin(t * 0.5F + i) * 0.3F;
			shard.zRot = Mth.cos(t * 0.07F + i) * 0.3F;
		}
	}
}
