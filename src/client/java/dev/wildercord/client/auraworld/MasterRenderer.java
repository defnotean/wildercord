package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.MasterTechniques;
import dev.wildercord.aura.world.SwordMaster;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Captures a complete immutable master frame from the synced server clock, including recovery. Where the Sword Master rig
 * is loaded (see {@link MasterMotion#enabled()}), the master is drawn from it instead: its skin, the glow of its eyes and
 * emblem, the aura along its blade and the afterimage of a vanish, all from {@link MasterMotion}'s pose. Sitting, kneeling
 * and bowing have no rig clips and keep the native model.
 */
public final class MasterRenderer extends AuraFighterRenderer<SwordMaster, MasterModel> {
	/** The schools with their own master dress; anything else keeps the duelist's skin. */
	private static final java.util.Set<String> MASTER_SKINS = java.util.Set.of("ember", "gale", "stone",
		"rime", "thunder", "verdant", "hollow", // ---- masters-a pack
		"starlit", "hourglass", "crimson", // ---- masters-b pack
		"tide", "iron", "dune", // ---- methods-a pack
		"echo", "dawn", "venom"); // ---- methods-b pack

	private static final Identifier RIG_GLOW = Wildercord.id("textures/entity/master_rig/glow.png");
	private static final Identifier RIG_BLADE = Wildercord.id("textures/entity/master_rig/blade_glow.png");

	public MasterRenderer(EntityRendererProvider.Context context) {
		super(context, new MasterModel(context.bakeLayer(AuraWorldClient.DUELIST)),
			state -> Wildercord.id((MASTER_SKINS.contains(state.method) ? "textures/entity/master/" : "textures/entity/duelist/") + state.method + ".png"),
			Wildercord.id("textures/entity/duelist/glow.png"),
			state -> AuraFighterRenderer.tint((.7F + .3F * state.windup) * AuraFighterRenderer.alive(state), state.color), 1);
	}

	@Override
	public void extractRenderState(SwordMaster master, AuraFighterRenderState state, float partial) {
		super.extractRenderState(master, state, partial);
		state.setData(MasterModel.FRAME, null);
		dev.wildercord.client.combat.ArticulatedCombat.extractMaster(master, state, partial);
		// This rig owns the entire swipe, including cancellation. A vanilla swing may not survive
		// the server clearing an attack during its active tick.
		state.swingAnimation = 0;
		state.currentSwing = null;
		if (!state.isUpsideDown) MasterMotion.extract(master, state, partial);
		else state.setData(MasterMotion.RIG, null);
		if (!master.isAlive() || master.isRemoved() || state.deathTime > 0 || state.isUpsideDown) return;
		var pose = master.state(AuraFighter.STAGGER) ? MasterAnimationRules.NONE
			: master.attackAnimation() == MasterAnimationRules.TECHNIQUE ? MasterTechniques.sample(master.technique(), master.attackElapsed(partial))
			: MasterAnimationRules.sample(master.attackAnimation(), master.attackElapsed(partial), master.attackTellTicks(),
				master.attackActiveTicks(), master.attackRecoveryTicks());
		if (pose.weight() > 0 && master.attackAnimation() == MasterAnimationRules.CRESCENT) {
			pose = MasterAnimationRules.aimed(pose, master.attackAnimation(), master.attackAimPitch());
			state.xRot = net.minecraft.util.Mth.lerp(pose.weight(), state.xRot, master.attackAimPitch());
		}
		if (pose.weight() <= 0) pose = MasterAnimationRules.defence(state.guard, state.dash, state.stagger);
		if (pose.weight() > 0) {
			state.setData(MasterModel.FRAME, new MasterModel.Frame(pose, master.getMainArm() == HumanoidArm.LEFT,
				master.level().getGameTime() - (long) Math.floor(master.attackElapsed(partial)), master.attackAnimation()));
		}
	}

	@Override
	protected @Nullable RenderType getRenderType(AuraFighterRenderState state, boolean visible, boolean translucent, boolean glowing) {
		return state.getData(MasterMotion.RIG) != null ? null : super.getRenderType(state, visible, translucent, glowing);
	}

	@Override
	protected boolean shouldRenderLayers(AuraFighterRenderState state) {
		return state.getData(MasterMotion.RIG) == null && super.shouldRenderLayers(state);
	}

	@Override
	public void submit(AuraFighterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		MasterMotion.Frame frame = state.getData(MasterMotion.RIG);
		MasterRig rig = MasterRig.get();
		if (frame == null || rig == null || state.isInvisible) return;
		float mirror = frame.left() ? 1 : -1;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, getWhiteOverlayProgress(state));
		int light = state.lightCoords;
		boolean[] blade = blade(rig);

		poseStack.pushPose();
		poseStack.scale(state.scale, state.scale, state.scale);
		setupRotations(state, poseStack, state.bodyRot, state.scale);
		poseStack.scale(mirror / 16, 1 / 16F, 1 / 16F);
		Matrix4f[] placed = rig.matrices(frame.pose());
		Identifier skin = Wildercord.id("textures/entity/master_rig/" + (MASTER_SKINS.contains(state.method) ? state.method : "base") + ".png");
		boolean whole = frame.alpha() >= 0.999F;
		int body = (Math.round(frame.alpha() * 255) << 24) | 0xFFFFFF;
		collector.order(0).submitCustomGeometry(poseStack, whole ? RenderTypes.entityCutout(skin) : RenderTypes.entityTranslucent(skin),
			(pose, buffer) -> rig.emit(placed, bone -> true, mirror < 0, pose, buffer, body, overlay, light));
		float glow = (.7F + .3F * state.windup) * AuraFighterRenderer.alive(state) * frame.alpha();
		int eyes = AuraFighterRenderer.tint(glow, state.color);
		if ((eyes >>> 24) >= 4) collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(RIG_GLOW),
			(pose, buffer) -> rig.emit(placed, bone -> !blade[bone], mirror < 0, pose, buffer, eyes, overlay, light));
		AuraBlade.Glow aura = state.getData(AuraBlade.GLOW);
		if (aura != null && state.drawn > 0.5F) {
			int edge = AuraFighterRenderer.tint(Math.min(1, aura.strength()) * frame.alpha(), aura.color());
			if ((edge >>> 24) >= 4) collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(RIG_BLADE),
				(pose, buffer) -> rig.emit(placed, bone -> blade[bone], mirror < 0, pose, buffer, edge, overlay, light));
		}
		poseStack.popPose();

		MasterMotion.Ghost ghost = frame.ghost();
		if (ghost != null && ghost.alpha() > 0.02F) {
			// The afterimage stays where the master stood, facing the way it faced, in its aura's colour.
			poseStack.pushPose();
			poseStack.translate(ghost.x() - state.x, ghost.y() - state.y, ghost.z() - state.z);
			poseStack.scale(state.scale, state.scale, state.scale);
			setupRotations(state, poseStack, ghost.bodyRot(), state.scale);
			poseStack.scale(mirror / 16, 1 / 16F, 1 / 16F);
			Matrix4f[] image = rig.matrices(ghost.pose());
			int tint = AuraFighterRenderer.tint(ghost.alpha(), state.color);
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(skin),
				(pose, buffer) -> rig.emit(image, bone -> true, mirror < 0, pose, buffer, tint, overlay, light));
			poseStack.popPose();
		}
	}

	private static boolean @Nullable [] bladeBones;

	/** The sword bone and everything under it: what the blade's aura runs along. */
	private static boolean[] blade(MasterRig rig) {
		if (bladeBones == null || bladeBones.length != rig.bones.length) {
			boolean[] out = new boolean[rig.bones.length];
			for (int i = 0; i < out.length; i++) out[i] = i == rig.sword || rig.bones[i].parent() >= 0 && out[rig.bones[i].parent()];
			bladeBones = out;
		}
		return bladeBones;
	}
}
