package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.SwordMaster;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.HumanoidArm;

/** Captures a complete immutable master frame from the synced server clock, including recovery. */
public final class MasterRenderer extends AuraFighterRenderer<SwordMaster, MasterModel> {
	public MasterRenderer(EntityRendererProvider.Context context) {
		super(context, new MasterModel(context.bakeLayer(AuraWorldClient.DUELIST)),
			state -> Wildercord.id("textures/entity/duelist/" + state.method + ".png"),
			Wildercord.id("textures/entity/duelist/glow.png"),
			state -> AuraFighterRenderer.tint((.7F + .3F * state.windup) * AuraFighterRenderer.alive(state), state.color), 1);
	}

	@Override
	public void extractRenderState(SwordMaster master, AuraFighterRenderState state, float partial) {
		super.extractRenderState(master, state, partial);
		state.setData(MasterModel.FRAME, null);
		// This rig owns the entire swipe, including cancellation. A vanilla swing may not survive
		// the server clearing an attack during its active tick.
		state.swingAnimation = 0;
		state.currentSwing = null;
		if (!master.isAlive() || master.isRemoved() || state.deathTime > 0 || state.isUpsideDown) return;
		var pose = master.state(AuraFighter.STAGGER) ? MasterAnimationRules.NONE
			: MasterAnimationRules.sample(master.attackAnimation(), master.attackElapsed(partial), master.attackTellTicks(),
				master.attackActiveTicks(), master.attackRecoveryTicks());
		if (pose.weight() > 0 && master.attackAnimation() == MasterAnimationRules.CRESCENT) {
			pose = MasterAnimationRules.aimed(pose, master.attackAnimation(), master.attackAimPitch());
			state.xRot = net.minecraft.util.Mth.lerp(pose.weight(), state.xRot, master.attackAimPitch());
		}
		if (pose.weight() <= 0) pose = MasterAnimationRules.defence(state.guard, state.dash, state.stagger);
		if (pose.weight() > 0) {
			state.setData(MasterModel.FRAME, new MasterModel.Frame(pose, master.getMainArm() == HumanoidArm.LEFT));
		}
	}
}
