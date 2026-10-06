package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.render.AuraShellLayer;
import dev.wildercord.client.render.GearLook;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Opt-in vertical slice. Owns a complete body/hands/item presentation only when every supported
 * part can be drawn together. All hitboxes, movement, input and accepted deadlines stay untouched.
 */
public final class ArticulatedCombat {
	private ArticulatedCombat() {}
	public static final String ENABLE_PROPERTY = "wildercord.articulated";
	public static final String STABLE_CAMERA_PROPERTY = "wildercord.articulated.stableCamera";

	/** The activation identity makes a cosmetic hit-stop incapable of reviving a cancelled/new clip. */
	public record Frame(ArticulatedCombatPose.Pose pose, long activation, int move, boolean master,
			boolean leftHanded, float yawDelta, float pitchDelta, boolean scriptedFootwork, double horizontalVelocitySquared) {
		public Frame(ArticulatedCombatPose.Pose pose, long activation, int move, boolean master,
				boolean leftHanded, float yawDelta, float pitchDelta, boolean scriptedFootwork) {
			this(pose, activation, move, master, leftHanded, yawDelta, pitchDelta, scriptedFootwork, Double.NaN);
		}
		public Frame(ArticulatedCombatPose.Pose pose, long activation, int move, boolean master,
				boolean leftHanded, float yawDelta, float pitchDelta) {
			this(pose, activation, move, master, leftHanded, yawDelta, pitchDelta, false);
		}
		public boolean sameActivation(Frame other) {
			return other != null && activation == other.activation && move == other.move && leftHanded == other.leftHanded && master == other.master;
		}
	}
	public static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "wildercord:articulated_frame");
	public static final RenderStateDataKey<Boolean> KNOWN_LAYERS = RenderStateDataKey.create(() -> "wildercord:articulated_known_layers");

	/** Exact known layer classes; a subclass or newly injected layer must earn its own adapter. */
	public static boolean knownLayer(net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?> layer) {
		return switch (layer.getClass().getName()) {
			case "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer",
				"net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer",
				"net.minecraft.client.renderer.entity.layers.ItemInHandLayer",
				"net.minecraft.client.renderer.entity.layers.ArrowLayer",
				"net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer",
				"net.minecraft.client.renderer.entity.layers.CapeLayer",
				"net.minecraft.client.renderer.entity.layers.CustomHeadLayer",
				"net.minecraft.client.renderer.entity.layers.WingsLayer",
				"net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer",
				"net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer",
				"net.minecraft.client.renderer.entity.layers.BeeStingerLayer",
				"dev.wildercord.client.render.CordLayer", "dev.wildercord.client.render.GearLayer",
				"dev.wildercord.client.render.AuraShellLayer", "dev.wildercord.client.render.AuraBodyLayer",
				"dev.wildercord.client.render.RuneMarksLayer", "dev.wildercord.client.auraworld.AuraFighterRenderer$Glow",
				"dev.wildercord.client.combat.ArticulatedArmorLayer" -> true;
			default -> false;
		};
	}

	/** Explicit saved opt-in or developer override; compatibility below remains authoritative. */
	public static boolean enabled() { return dev.wildercord.client.CombatPresentation.effective().articulated(); }
	public static boolean stableCamera() {
		return dev.wildercord.client.CombatPresentation.effective().stableCamera();
	}

	public static void extract(Avatar avatar, AvatarRenderState state, float partial) {
		state.setData(FRAME, null);
		if (!enabled()) return;
		var movement = state.getData(dev.wildercord.client.MastersArtPose.FRAME);
		if (movement != null && movement.move() <= -2) return;
		var timeline = MastersArtsClient.timeline(avatar);
		boolean left = avatar.getMainArm() == HumanoidArm.LEFT;
		if (timeline == null) {
			// Stable first-person ownership avoids adding/removing two arms at every clip edge.
			state.setData(FRAME, new Frame(ArticulatedCombatPose.NONE, Long.MIN_VALUE, -1, false, left, 0, 0));
			return;
		}
		if (!ArticulatedCombatPose.supportsPlayer(timeline.move())) return;
		float age = avatar.level().getGameTime() - timeline.startTick() + partial;
		var pose = ArticulatedCombatPose.samplePlayer(timeline.move(), age, timeline.windup(), timeline.recovery(), left);
		float viewYaw = avatar.getViewYRot(partial), viewPitch = avatar.getViewXRot(partial);
		float yawDelta = net.minecraft.util.Mth.wrapDegrees(timeline.yaw() - viewYaw);
		state.setData(FRAME, new Frame(pose, timeline.startTick(), timeline.move(), false, left, yawDelta, timeline.pitch() - viewPitch));
	}

	public static void extractMaster(SwordMaster master, AuraFighterRenderState state, float partial) {
		state.setData(FRAME, null);
		if (!enabled() || !ArticulatedCombatPose.supportsMaster(master.attackAnimation())
			|| master.state(dev.wildercord.aura.world.AuraFighter.STAGGER)) return;
		float elapsed = master.attackElapsed(partial);
		boolean left = master.getMainArm() == HumanoidArm.LEFT;
		var pose = ArticulatedCombatPose.sampleMaster(master.attackAnimation(), elapsed, master.attackTellTicks(),
			master.attackActiveTicks(), master.attackRecoveryTicks(), left);
		if (pose.weight() <= 0) return;
		long activation = master.level().getGameTime() - (long) Math.floor(elapsed);
		state.setData(FRAME, new Frame(pose, activation, master.attackAnimation(), true, left, 0, 0,
			ArticulatedCombatPose.masterFootwork(master.attackAnimation(), elapsed, master.attackTellTicks()), master.getDeltaMovement().horizontalDistanceSqr()));
	}

	public static Frame frame(ArmedEntityRenderState state) { return compatible(state, false); }

	/** Independent camera-space idle/attack ownership; locomotion remains the world model's job. */
	public static Frame viewFrame(AvatarRenderState state) { return compatible(state, true); }

	private static Frame compatible(ArmedEntityRenderState state, boolean view) {
		if (!enabled() || !(state instanceof HumanoidRenderState humanoid) || !upright(humanoid) || !plainSword(state.getMainHandItemStack())
			|| !Boolean.TRUE.equals(state.getData(KNOWN_LAYERS))) return null;
		Frame frame = state.getData(FRAME);
		if (frame == null || !view && state.walkAnimationSpeed > .2F
				&& !schoolFootworkCompatible(frame)
			|| !ArticulatedArmorRenderer.compatible(humanoid) || !view && frame.pose().weight() <= 0
			|| view && frame.move() != -1 && !ArticulatedCombatPose.supportsPlayer(frame.move())
			|| view && frame.move() == -1 && state.swingAnimation > 0) return null;
		ItemStack off = state.mainArm == HumanoidArm.RIGHT ? state.leftHandItemStack : state.rightHandItemStack;
		if (off != null && !off.isEmpty()) return null;
		if (state instanceof AvatarRenderState avatar) {
			if (frame.master() || avatar.isSpectator || avatar.skin == null
				|| avatar.arrowCount > 0 || avatar.stingerCount > 0 || avatar.showExtraEars
				|| avatar.parrotOnLeftShoulder != null || avatar.parrotOnRightShoulder != null
				|| avatar.showCape && avatar.skin.cape() != null || !avatar.heldOnHead.isEmpty()
				|| state.getData(GearLook.PACK) != null || state.getData(GearLook.PIECES) != null
				|| !ArticulatedAuraShellRenderer.compatible(avatar) || state.getData(AuraShellLayer.IMAGES) != null) return null;
		} else if (!(state instanceof AuraFighterRenderState fighter) || !frame.master() || fighter.sit > 0 || fighter.yield > 0 || fighter.stagger > 0) return null;
		return frame;
	}

	private static boolean schoolFootworkCompatible(Frame frame) {
		if (!frame.master() || frame.move() != ArticulatedCombatPose.MASTER_CROSSWIND_REPRISE) return false;
		// Vanilla's eased walk speed remains high for several frames after the accepted step.
		// Synced velocity is distinct from the client's remaining position interpolation. Gale's
		// accepted script zeroes horizontal velocity; walking/knockback does not earn this path.
		// Unknown/synthetic velocity stays conservative, and the exact step window is unchanged.
		return frame.scriptedFootwork() || Double.isFinite(frame.horizontalVelocitySquared())
			&& frame.horizontalVelocitySquared() >= 0 && frame.horizontalVelocitySquared() <= 1.0e-8;
	}

	private static boolean upright(HumanoidRenderState state) {
		return state.deathTime <= 0 && !state.isInvisible && !state.isBaby && !state.isUpsideDown && !state.isAutoSpinAttack
			&& !state.isFallFlying && !state.isCrouching && !state.isPassenger && !state.isUsingItem
			&& !state.hasPose(Pose.SWIMMING) && !state.hasPose(Pose.SLEEPING);
	}
	private static boolean plainSword(ItemStack stack) {
		return stack != null && (stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.IRON_SWORD)
			|| stack.is(Items.GOLDEN_SWORD) || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD));
	}

	public static boolean applyPlayer(PlayerModel model, AvatarRenderState state) {
		if (model.getClass() != PlayerModel.class || !(model instanceof ArticulatedModelAccess access) || !access.wildercord$bodyOwned()) return false;
		return apply(access.wildercord$rig(), model, state);
	}

	public static boolean apply(ArticulatedRig rig, HumanoidModel<?> model, HumanoidRenderState state) {
		Frame frame = frame(state);
		if (frame == null) return false;
		rig.apply(frame.pose()::local);
		if (state instanceof AvatarRenderState avatar) rig.skinLayers(avatar);
		// The body follows accepted aim; the head retains bounded free look, without camera writes.
		var head = rig.part(ArticulatedCombatPose.Joint.HEAD);
		float weight = frame.pose().weight();
		head.xRot += net.minecraft.util.Mth.lerp(weight, model.head.xRot, Math.max(-.65F, Math.min(.65F, model.head.xRot)));
		head.yRot += net.minecraft.util.Mth.lerp(weight, model.head.yRot, Math.max(-1.1F, Math.min(1.1F, model.head.yRot)));
		head.zRot += model.head.zRot * (1 - weight);
		baselineArm(rig, model.rightArm, false, weight);
		baselineArm(rig, model.leftArm, true, weight);
		rig.hideRigid(model);
		return true;
	}

	/** The new body converges to the actual held-item/breathing pose at both backend edges. */
	static void baselineArm(ArticulatedRig rig, net.minecraft.client.model.geom.ModelPart baseline, boolean left, float weight) {
		float remaining = 1 - weight;
		if (remaining <= 0) return;
		var shoulder = rig.part(left ? ArticulatedCombatPose.Joint.LEFT_SHOULDER : ArticulatedCombatPose.Joint.RIGHT_SHOULDER);
		var initial = baseline.getInitialPose();
		shoulder.x += (baseline.x - initial.x()) * remaining;
		shoulder.y += (baseline.y - initial.y()) * remaining;
		shoulder.z += (baseline.z - initial.z()) * remaining;
		var upper = rig.part(left ? ArticulatedCombatPose.Joint.LEFT_UPPER_ARM : ArticulatedCombatPose.Joint.RIGHT_UPPER_ARM);
		var rotation = ArticulatedCombatPose.Rotation.ZERO.toward(new ArticulatedCombatPose.Rotation(baseline.xRot, baseline.yRot, baseline.zRot), remaining);
		upper.rotateBy(new org.joml.Quaternionf().rotationZYX(rotation.z(), rotation.y(), rotation.x()));
		// Vanilla's resolved hilt sits slightly above/behind the centre of the hand cuboid.
		var socket = rig.part(left ? ArticulatedCombatPose.Joint.LEFT_SOCKET : ArticulatedCombatPose.Joint.RIGHT_SOCKET);
		socket.y -= .439F * remaining;
		socket.z -= .673F * remaining;
		if (rig.slim()) {
			// PlayerModel.translateToHand shifts a slim arm's pivot in its parent's X axis
			// BEFORE rotating it. Narrowing the hand centre AFTER that rotation is not the
			// same transform. Convert the disappearing half-pixel difference into local
			// socket space; moving the shoulder itself would also displace the skin mesh.
			float pivot = left ? -.5F : .5F;
			var correction = new org.joml.Quaternionf().rotationZYX(rotation.z(), rotation.y(), rotation.x()).conjugate()
				.transform(new org.joml.Vector3f(pivot, 0, 0));
			socket.x += (correction.x - pivot) * remaining;
			socket.y += correction.y * remaining;
			socket.z += correction.z * remaining;
		}
	}

	/** Matches vanilla's later item transform exactly, so its hilt lands on our hand centre. */
	public static void translateHeld(ArticulatedRig rig, HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm, PoseStack stack) {
		apply(rig, model, state);
		model.root().translateAndRotate(stack);
		rig.socket(arm, stack);
		// Undo the vanilla pre-item hilt position, in wrist-local coordinates. ItemInHandLayer
		// subsequently applies Rx(-90), Ry(180), T(±1,2,-10)/16 and the resolved item display.
		stack.translate((arm == HumanoidArm.LEFT ? -1 : 1) / 16F, -8.561F / 16F, .673F / 16F);
	}

	/** Canonical third-person display hilt; first person deliberately uses the same resolved item. */
	public static void orientItemAtSocket(PoseStack stack) {
		stack.rotateDegrees(Axis.XP, -90);
		stack.rotateDegrees(Axis.YP, 180);
		stack.translate(0, 1.327F / 16, -1.439F / 16);
	}

	public static boolean legacyArm(PlayerModel model, AvatarRenderState state, HumanoidArm arm, PoseStack stack) {
		Frame frame = frame(state);
		if (frame == null || !(model instanceof ArticulatedModelAccess access) || !access.wildercord$bodyOwned()) return false;
		ArticulatedRig rig = access.wildercord$rig();
		applyPlayer(model, state);
		rig.transformTo(arm == HumanoidArm.LEFT ? ArticulatedCombatPose.Joint.LEFT_HAND : ArticulatedCombatPose.Joint.RIGHT_HAND, stack);
		stack.translate(0, -8F / 16, 0);
		return true;
	}

	public static boolean head(PlayerModel model, AvatarRenderState state, PoseStack stack) {
		if (frame(state) == null || !(model instanceof ArticulatedModelAccess access) || !access.wildercord$bodyOwned()) return false;
		applyPlayer(model, state);
		access.wildercord$rig().transformTo(ArticulatedCombatPose.Joint.HEAD, stack);
		return true;
	}
}
