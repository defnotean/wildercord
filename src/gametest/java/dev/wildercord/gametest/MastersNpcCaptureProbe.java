package dev.wildercord.gametest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.world.GaleRepriseRules;
import dev.wildercord.aura.world.EmberKilnRules;
import dev.wildercord.aura.world.MasterSchoolMotionChecks.ServerFrame;
import dev.wildercord.aura.world.StoneFractureRules;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterModel;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Read-only native NPC submit/model/particle and unmodified framebuffer evidence. No pose injection. */
public final class MastersNpcCaptureProbe {
	public static final float PARTIAL = .5F;
	private static final DeltaTracker DELTA = new DeltaTracker() {
		public float getGameTimeDeltaTicks() { return PARTIAL; }
		public float getGameTimeDeltaPartialTick(boolean paused) { return PARTIAL; }
		public float getRealtimeDeltaTicks() { return PARTIAL; }
	};
	public record Options(CameraType camera, int scale, int width, int height, int fov, boolean hidden, boolean fullscreen,
		boolean bob, ParticleStatus particles, boolean reducedFlash, boolean cameraShake) {
		public static Options save(Minecraft mc) {
			return new Options(mc.options.getCameraType(), mc.options.guiScale().get(), mc.getWindow().getWidth(), mc.getWindow().getHeight(),
				mc.options.fov().get(), mc.gui.hud.isHidden(), mc.options.fullscreen().get(), mc.options.bobView().get(), mc.options.particles().get(), MagicQuality.reducedFlash, MagicQuality.cameraShake);
		}
		public void restore(Minecraft mc) {
			mc.gui.setScreen(null);
			mc.options.setCameraType(camera); mc.options.guiScale().set(scale); mc.options.fov().set(fov);
			mc.options.bobView().set(bob); mc.options.particles().set(particles);
			mc.getWindow().setWindowed(width, height); mc.getWindow().setFullscreen(fullscreen); mc.resizeGui();
			if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
			MagicQuality.reducedFlash = reducedFlash; MagicQuality.cameraShake = cameraShake;
		}
	}
	public record Timeline(long clientGameTick, long acceptedTick, int attackId, float partial, float age,
		int tell, int active, int recovery, boolean fallbackRig, MasterModel.Frame frame) {}
	public record Point(double x, double y) {}
	public record Ray(Vec3 from, Vec3 to, Vec3 renderedEnd, float width, int particleAge, int lifetime, float partial, int color) {}
	public record WarningPixels(Ray ray, Point from, Point to, int coloredBins, int sampledBins) {}
	public record BodyPixels(int nonBlackPixels, int chromaticPixels, int distinctColors, int luminanceRange) {}
	/** Unclamped normalized bounds of the observed geometry, including every visible cube/held-item extent. */
	public record ProjectedBounds(double minX, double minY, double maxX, double maxY, int vertexCount,
		boolean allInFront, boolean wholeVisible) {}
	public record ArticulatedFrame(long activation, int move, String phase, float weight, boolean leftHanded,
		boolean scriptedFootwork, double horizontalVelocitySquared, double interpolatedTravelSquared, float walkAnimationSpeed) {}
	public record ModelReceipt(String backend, boolean segmentedRootVisible, boolean[] rigidPartsVisible,
		List<String> transformNames, float[] transforms, float[] modelRootTransform) {}
	/** Entry/hand matrices are observed; resolved item matrices apply vanilla's fixed adult item offsets to that receipt. */
	public record HandReceipt(String hand, float[] entryMatrix, float[] nativeHandMatrix, float[] resolvedItemMatrix,
		float[] expectedSocketItemMatrix, float hiltDistance, float maximumMatrixError) {}
	public record Evidence(String name, String captureKind, String participantKind, String observerKind,
		boolean independentTrialPerFrame, String phase, int requestedTick, int width, int height, Timeline renderedTimeline, ServerFrame serverObservation,
		int bodySubmits, int modelPasses, float[] modelPose, Vec3 camera, Vec3 renderedPosition,
		List<Point> bodyPoints, BodyPixels bodyPixels, List<WarningPixels> warnings, String renderFailure,
		String expectedBackend, ArticulatedFrame articulatedFrame, List<ModelReceipt> modelReceipts, List<HandReceipt> handReceipts,
		String framing, int fov, String warningCoverage, ProjectedBounds bodyBounds, ProjectedBounds bladeBounds,
		String bodyBoundsSource, String bladeBoundsSource, float[] bodySubmitMatrix, float[] viewRotationProjectionMatrix, String warningValidation, String captureStatus) {}
	private static SwordMaster subject;
	private static Vec3 origin;
	private static AuraFighterRenderState renderedState;
	private static Timeline timeline;
	private static float[] modelPose;
	private static Vec3 renderedPosition;
	private static int bodies, models;
	private static final List<Ray> rays = new ArrayList<>();
	private static final List<ModelReceipt> modelReceipts = new ArrayList<>();
	private static final List<HandReceipt> handReceipts = new ArrayList<>();
	private static ArticulatedFrame articulatedFrame;
	private static Matrix4f handEntry;
	private static HumanoidArm enteringHand;
	private static Matrix4f bodyEntry, viewProjection;
	private static BoundsAccumulator bodyBounds, bladeBounds;
	private static boolean zeroToOneDepth;
	private MastersNpcCaptureProbe() {}

	public static void configure(Minecraft mc) {
		mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.options.fov().set(60);
		mc.options.bobView().set(false); mc.options.particles().set(ParticleStatus.ALL);
		mc.options.setCameraType(CameraType.FIRST_PERSON); mc.gui.setScreen(null); mc.resizeGui();
		if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
		MagicQuality.reducedFlash = false; MagicQuality.cameraShake = false;
	}
	public static void end() {
		subject = null; renderedState = null; handEntry = null; enteringHand = null;
		bodyEntry = viewProjection = null; bodyBounds = bladeBounds = null;
		rays.clear(); modelReceipts.clear(); handReceipts.clear();
	}

	/** Called only by the real registered MasterRenderer's extract method. */
	public static void extracted(SwordMaster master, AuraFighterRenderState state, float partial) {
		if (subject != master) return;
		renderedState = state;
		renderedPosition = new Vec3(state.x, state.y, state.z);
		float age = master.attackElapsed(partial);
		timeline = new Timeline(master.level().getGameTime(), master.level().getGameTime() - (long) master.attackElapsed(0),
			master.attackAnimation(), partial, age, master.attackTellTicks(), master.attackActiveTicks(), master.attackRecoveryTicks(),
			ArticulatedCombat.frame(state) == null, state.getData(MasterModel.FRAME));
		// Preserve the extracted candidate even when eligibility rejects it; failed captures then
		// retain the synced velocity/interpolated travel needed to diagnose an ownership gap.
		var frame = state.getData(ArticulatedCombat.FRAME);
		double dx = master.getX() - master.xo, dz = master.getZ() - master.zo;
		articulatedFrame = frame == null ? null : new ArticulatedFrame(frame.activation(), frame.move(), frame.pose().phase().name(),
			frame.pose().weight(), frame.leftHanded(), frame.scriptedFootwork(), frame.horizontalVelocitySquared(), dx * dx + dz * dz, state.walkAnimationSpeed);
	}
	/** Called at the actual opaque living-model submit, not merely at renderer lookup. */
	public static void body(LivingEntityRenderState state, PoseStack stack) {
		if (subject != null && state == renderedState && !state.isInvisible && !state.isInvisibleToPlayer) {
			bodies++;
			if (ArticulatedCombat.frame(renderedState) != null) {
				// This is the native body submit matrix, before root/part transforms, in camera-relative world space.
				bodyEntry = new Matrix4f(stack.last().pose());
				viewProjection = Minecraft.getInstance().gameRenderer.mainCamera().getViewRotationProjectionMatrix(new Matrix4f());
			}
		}
	}
	/** Called after native animation setup during the rendered pass, never invoked by this probe. */
	public static void model(MasterModel model, AuraFighterRenderState state) {
		if (subject == null || state != renderedState) return;
		models++;
		var rig = model.articulatedRig();
		ModelPart[] rigid = {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
		boolean[] visible = new boolean[rigid.length];
		for (int i = 0; i < rigid.length; i++) visible[i] = rigid[i].visible;
		// Record the parts actually submitted by this pass, not the hidden rigid placeholders.
		ModelPart[] parts = rig.root.visible ? java.util.Arrays.stream(Joint.values()).map(rig::part).toArray(ModelPart[]::new) : rigid;
		List<String> names = rig.root.visible ? java.util.Arrays.stream(Joint.values()).map(Enum::name).toList()
			: List.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
		modelPose = new float[parts.length * 6];
		for (int i = 0; i < parts.length; i++) {
			var p = parts[i]; int at = i * 6;
			modelPose[at] = p.x; modelPose[at + 1] = p.y; modelPose[at + 2] = p.z;
			modelPose[at + 3] = p.xRot; modelPose[at + 4] = p.yRot; modelPose[at + 5] = p.zRot;
		}
		modelReceipts.add(new ModelReceipt(rig.root.visible ? "segmented" : "rigid", rig.root.visible, visible, names, modelPose,
			new float[] {model.root().x, model.root().y, model.root().z, model.root().xRot, model.root().yRot, model.root().zRot}));
		if (rig.root.visible && bodyEntry != null) {
			PoseStack submitted = new PoseStack(); submitted.last().pose().set(bodyEntry);
			model.root().visit(submitted, (pose, path, index, cube) -> {
				// ModelPart.visit itself ignores visibility. Match render's hidden-ancestor and skipDraw rules.
				if (!drawsCube(model.root(), path)) return;
				Matrix4f projected = new Matrix4f(viewProjection).mul(pose.pose());
				for (var polygon : cube.polygons) for (var vertex : polygon.vertices())
					bodyBounds.add(projected, vertex.x() / 16F, vertex.y() / 16F, vertex.z() / 16F);
			});
		}
	}

	/** Entry and return hooks run only when the native item layer calls the registered model. */
	public static void beforeHand(AuraFighterRenderState state, HumanoidArm arm, PoseStack stack) {
		if (subject == null || state != renderedState || ArticulatedCombat.frame(state) == null) return;
		check(handEntry == null, "Native hand attachments must not overlap");
		handEntry = new Matrix4f(stack.last().pose()); enteringHand = arm;
	}
	public static void hand(MasterModel model, AuraFighterRenderState state, HumanoidArm arm, PoseStack stack) {
		if (subject == null || state != renderedState || ArticulatedCombat.frame(state) == null) return;
		check(handEntry != null && enteringHand == arm, "Every native hand receipt needs its actual incoming matrix");
		PoseStack actual = new PoseStack(); actual.last().pose().set(stack.last().pose());
		actual.rotateDegrees(Axis.XP, -90); actual.rotateDegrees(Axis.YP, 180);
		actual.translate((arm == HumanoidArm.LEFT ? -1 : 1) / 16F, 2F / 16, -10F / 16);
		PoseStack expected = new PoseStack(); expected.last().pose().set(handEntry);
		model.root().translateAndRotate(expected); model.articulatedRig().socket(arm, expected);
		Vector3f hilt = actual.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
		Vector3f socket = expected.last().pose().transformPosition(new Vector3f());
		ArticulatedCombat.orientItemAtSocket(expected);
		float[] resolved = actual.last().pose().get(new float[16]), target = expected.last().pose().get(new float[16]);
		float error = 0;
		for (int i = 0; i < 16; i++) error = Math.max(error, Math.abs(resolved[i] - target[i]));
		handReceipts.add(new HandReceipt(arm.name(), handEntry.get(new float[16]), stack.last().pose().get(new float[16]),
			resolved, target, hilt.distance(socket), error));
		// visitExtents includes the actual resolved item's display transforms; the observed hand receipt
		// plus vanilla's fixed adult offsets is its input matrix. No authored blade dimensions are assumed.
		if (arm == state.mainArm && viewProjection != null) {
			Matrix4f projected = new Matrix4f(viewProjection).mul(actual.last().pose());
			state.getMainHandItemState().visitExtents(point -> bladeBounds.add(projected, point.x(), point.y(), point.z()));
		}
		handEntry = null; enteringHand = null;
	}
	/** Native LightParticle ray extraction; its world position/color identifies the actual floor warning. */
	public static void ray(int color, Vec3 from, Vec3 delta, float width, int age, int lifetime, float partial) {
		if (subject == null || color != subject.auraColor() && !(subject.attackAnimation() == 9 && color == 0x73E2CB)
			|| Math.abs(from.y - origin.y - .12) > .01
			|| from.distanceToSqr(origin) > 64 || Math.abs(delta.y) > .001 || age + partial >= lifetime) return;
		rays.add(new Ray(from, from.add(delta), from.add(delta.scale(Math.min(1, (age + partial) / 2))), width, age, lifetime, partial, color));
	}

	public static CompletableFuture<Evidence> capture(Minecraft mc, SwordMaster master, String name, String phase,
		int requestedTick, int requiredWarningSegments, Vec3 acceptedOrigin, ServerFrame serverFrame) {
		return capture(mc, master, name, phase, requestedTick, requiredWarningSegments, acceptedOrigin, serverFrame, false);
	}

	public static CompletableFuture<Evidence> capture(Minecraft mc, SwordMaster master, String name, String phase,
		int requestedTick, int requiredWarningSegments, Vec3 acceptedOrigin, ServerFrame serverFrame, boolean articulated) {
		return capture(mc, master, name, phase, requestedTick, requiredWarningSegments, acceptedOrigin, serverFrame,
			articulated, articulated && !phase.equals("reply_warning"));
	}

	/** Explicit framing allows the same close-body and complete-ring gates for both NPC backends. */
	public static CompletableFuture<Evidence> capture(Minecraft mc, SwordMaster master, String name, String phase,
		int requestedTick, int requiredWarningSegments, Vec3 acceptedOrigin, ServerFrame serverFrame, boolean articulated, boolean closeBody) {
		check(subject == null, "NPC captures must not overlap");
		subject = master; origin = acceptedOrigin; timeline = null; renderedState = null; modelPose = null;
		bodies = models = 0; rays.clear(); modelReceipts.clear(); handReceipts.clear(); articulatedFrame = null;
		handEntry = null; enteringHand = null;
		bodyEntry = viewProjection = null; bodyBounds = new BoundsAccumulator(); bladeBounds = new BoundsAccumulator();
		zeroToOneDepth = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
		boolean fullWarningCoverage = !closeBody;
		var result = new CompletableFuture<Evidence>();
		try {
			mc.gameRenderer.update(DELTA); mc.gameRenderer.extract(DELTA, true); mc.gameRenderer.render();
			RenderSystem.getDevice().createCommandEncoder().submit();
			Vec3 eye = mc.gameRenderer.mainCamera().position();
			ProjectedBounds capturedBodyBounds = articulated ? bodyBounds.snapshot() : null;
			ProjectedBounds capturedBladeBounds = articulated ? bladeBounds.snapshot() : null;
			float[] capturedBodyMatrix = bodyEntry == null ? null : bodyEntry.get(new float[16]);
			float[] capturedProjection = viewProjection == null ? null : viewProjection.get(new float[16]);
			int capturedFov = mc.options.fov().get();
			List<Point> bodyPoints = new ArrayList<>();
			String failure = null;
			try {
				check(!mc.isPaused() && mc.player.isSpectator() && mc.options.getCameraType().isFirstPerson(), "The camera is an unpaused real spectator");
				check(bodies > 0 && models > 0 && modelPose != null, "Native NPC body submission and animated model passes must both occur");
				check(timeline != null && timeline.attackId == serverFrame.attackId() && timeline.acceptedTick == serverFrame.acceptedTick(), "The submitted model must belong to the accepted server form");
				check(Math.abs(timeline.clientGameTick - serverFrame.gameTick()) <= 1, "Server observation must be contemporaneous with native extraction");
				check(timeline.partial == PARTIAL && timeline.age >= requestedTick && timeline.age < requestedTick + 1, "Actual extraction must match the exact requested tick");
				check(timeline.frame != null && timeline.frame.move() == timeline.attackId, "The rigid fallback timeline remains available for the accepted form");
				check(timeline.fallbackRig != articulated, "The requested backend must own the native school frame");
				check(modelReceipts.stream().allMatch(receipt -> receipt.segmentedRootVisible == articulated
					&& receipt.transforms.length == (articulated ? Joint.values().length : 6) * 6
					&& allVisibility(receipt.rigidPartsVisible, !articulated)), "Every native model pass must use one complete, identified backend");
				if (articulated) {
					check(articulatedFrame != null && articulatedFrame.activation == timeline.acceptedTick
						&& articulatedFrame.move == timeline.attackId && articulatedFrame.weight > 0, "The articulated frame must retain the accepted activation and form");
					check(Double.isFinite(articulatedFrame.horizontalVelocitySquared) && articulatedFrame.horizontalVelocitySquared >= 0
						&& Double.isFinite(articulatedFrame.interpolatedTravelSquared) && articulatedFrame.interpolatedTravelSquared >= 0,
						"Native receipts must retain finite synced velocity and separate interpolated travel");
					check(articulatedFrame.phase.equals(phase.equals("release") ? "ACTIVE" : phase.equals("recovery") ? "RECOVERY" : "WINDUP"),
						"The actual articulated palette must match the requested server phase");
					check(articulatedFrame.scriptedFootwork == (timeline.attackId == 7
						&& timeline.age >= GaleRepriseRules.GATHER && timeline.age < GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS),
						"Only Gale's accepted step owns scripted footwork");
					check(!handReceipts.isEmpty() && handReceipts.stream().allMatch(receipt -> receipt.hand.equals(master.getMainArm().name())
						&& Float.isFinite(receipt.hiltDistance) && receipt.hiltDistance < .00001F
						&& Float.isFinite(receipt.maximumMatrixError) && receipt.maximumMatrixError < .00001F),
						"Actual native held-item calls must agree with the wrist socket in position and orientation");
					check(capturedBodyBounds.wholeVisible && capturedBladeBounds.wholeVisible,
						"Every visible native body vertex and resolved held-blade extent must remain inside the viewport");
					if (closeBody) check((capturedBodyBounds.maxY - capturedBodyBounds.minY) * mc.getWindow().getHeight() >= 180,
						"The close native body geometry must occupy at least 180 vertical framebuffer pixels");
				}
				check(!master.isInvisible() && master.isAlive() && eye.distanceTo(master.position()) >= 4, "The complete master must be observable");
				for (double y : new double[] {.2, .95, 1.7}) for (double x : new double[] {-.15, 0, .15}) {
					Vec3 point = renderedPosition.add(x, y, 0);
					bodyPoints.add(project(mc, point));
					check(clear(mc, eye, point), "Stage and other bodies must not occlude the master");
				}
				double height = bodyPoints.stream().mapToDouble(Point::y).max().orElseThrow() - bodyPoints.stream().mapToDouble(Point::y).min().orElseThrow();
				check(height * mc.getWindow().getHeight() >= 70, "The visible native NPC must be large enough to review its motion");
				if (fullWarningCoverage) for (Vec3[] expected : expectedRays(requestedTick, serverFrame.attackId(), acceptedOrigin))
					check(rays.stream().anyMatch(ray -> sameRay(ray, expected)), "Every actual warning segment must be extracted: " + phase);
				check(expectedRays(requestedTick, serverFrame.attackId(), acceptedOrigin).size() == requiredWarningSegments, "The phase warning contract must match its geometry");
			} catch (Throwable problem) { failure = problem.toString(); }
			Timeline capturedTimeline = timeline;
			float[] capturedPose = modelPose;
			Vec3 capturedPosition = renderedPosition;
			int capturedBodies = bodies, capturedModels = models;
			List<Ray> capturedRays = List.copyOf(rays);
			ArticulatedFrame capturedFrame = articulatedFrame;
			List<ModelReceipt> capturedModelsReceipts = List.copyOf(modelReceipts);
			List<HandReceipt> capturedHands = List.copyOf(handReceipts);
			var projectedRays = capturedRays.stream().map(ray -> projectRay(mc, ray, fullWarningCoverage)).toList();
			String renderFailure = failure;
			net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
				try (image) {
					var path = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name + ".png");
					Files.createDirectories(path.getParent());
					image.writeToFile(path); // Exact native framebuffer, including failed evidence.
					var warnings = new ArrayList<WarningPixels>();
					int[] pixels = image.getPixels();
					BodyPixels bodyPixels = measureBody(bodyPoints, pixels, image.getWidth(), image.getHeight());
					for (int i = 0; i < capturedRays.size(); i++) warnings.add(measure(capturedRays.get(i), projectedRays.get(i), pixels, image.getWidth(), image.getHeight(), serverFrame.attackId(), fullWarningCoverage));
					String validationFailure = renderFailure;
					try {
						check(bodyPixels.nonBlackPixels >= 48 && bodyPixels.distinctColors >= 8 && bodyPixels.luminanceRange >= 24,
							"The submitted NPC framebuffer region must contain nonblank body detail: " + name);
						// A close body's visible geometry can occlude projected floor samples. Those counts
						// remain diagnostics; only the separate wide view establishes warning acceptance.
						if (fullWarningCoverage) for (Vec3[] expected : expectedRays(requestedTick, serverFrame.attackId(), acceptedOrigin))
							check(warnings.stream().anyMatch(warning -> sameRay(warning.ray, expected) && warning.coloredBins >= 4),
								"Each warning segment must have visible school-colored framebuffer pixels across at least four of sixteen bins: " + name);
					} catch (Throwable problem) {
						if (validationFailure == null) validationFailure = problem.toString();
					}
					var evidence = new Evidence(name, "native_unpaused_server_ai", "consenting_fabric_fake_player", "real_client_spectator",
						true, phase, requestedTick, image.getWidth(), image.getHeight(), capturedTimeline, serverFrame, capturedBodies, capturedModels,
						capturedPose, eye, capturedPosition, List.copyOf(bodyPoints), bodyPixels, warnings, validationFailure,
						articulated ? "segmented" : "rigid", capturedFrame, capturedModelsReceipts, capturedHands,
						closeBody ? "body_close" : capturedTimeline.attackId == 9 ? "warning_ring" : articulated ? "warning_lane" : "legacy_wide", capturedFov,
						fullWarningCoverage ? "full_lane" : "visible_portion", capturedBodyBounds, capturedBladeBounds,
						articulated ? "visible_native_model_cube_vertices_with_body_submit_matrix" : null,
						articulated ? "resolved_native_item_extents_with_hand_receipt_and_vanilla_adult_offsets" : null,
						capturedBodyMatrix, capturedProjection, fullWarningCoverage ? "full_lane_required" : "diagnostic_only",
						validationFailure == null ? "passed" : "failed");
					Files.writeString(path.resolveSibling(name + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence));
					check(validationFailure == null, "Native NPC capture rejected: " + validationFailure);
					Wildercord.LOGGER.info("MASTERS_NPC_CAPTURE name={} backend={} attack={} accepted={} age={} bodies={} models={} hands={} warnings={}",
						name, articulated ? "segmented" : "rigid", capturedTimeline.attackId, capturedTimeline.acceptedTick,
						capturedTimeline.age, capturedBodies, capturedModels, capturedHands.size(), warnings.size());
					result.complete(evidence);
				} catch (Throwable problem) { result.completeExceptionally(problem); }
			});
		} finally { end(); }
		return result;
	}

	private static boolean allVisibility(boolean[] visible, boolean expected) {
		for (boolean value : visible) if (value != expected) return false;
		return true;
	}
	private static boolean drawsCube(ModelPart root, String path) {
		ModelPart part = root;
		if (!part.visible) return false;
		for (String name : path.split("/")) if (!name.isEmpty()) {
			part = part.getChild(name);
			if (!part.visible) return false;
		}
		return !part.skipDraw;
	}
	private static final class BoundsAccumulator {
		private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		private int vertices, inFront, withinDepth;
		void add(Matrix4f projection, float x, float y, float z) {
			vertices++;
			var clip = projection.transform(new org.joml.Vector4f(x, y, z, 1));
			if (!clip.isFinite() || clip.w <= 0) return;
			double px = (clip.x / clip.w + 1) / 2, py = (1 - clip.y / clip.w) / 2;
			if (!Double.isFinite(px) || !Double.isFinite(py)) return;
			inFront++;
			if (insideDepth(clip)) withinDepth++;
			minX = Math.min(minX, px); minY = Math.min(minY, py);
			maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
		}
		ProjectedBounds snapshot() {
			boolean front = vertices > 0 && inFront == vertices;
			return new ProjectedBounds(inFront == 0 ? 0 : minX, inFront == 0 ? 0 : minY,
				inFront == 0 ? 0 : maxX, inFront == 0 ? 0 : maxY, vertices, front,
				front && withinDepth == vertices && minX > .03 && minY > .03 && maxX < .97 && maxY < .97);
		}
	}
	private static boolean insideDepth(org.joml.Vector4f clip) {
		// Native projections use the device's clip convention, including reversed Z.
		return clip.z >= (zeroToOneDepth ? 0 : -clip.w) && clip.z <= clip.w;
	}

	private static List<Vec3[]> expectedRays(int age, int attack, Vec3 origin) {
		// The last real particle may fade through release; these beats require no warning segments.
		if (attack == 9) {
			if (age >= EmberKilnRules.TELL) return List.of();
			var ring = new ArrayList<Vec3[]>();
			for (double radius : new double[] {EmberKilnRules.INNER, EmberKilnRules.OUTER / Math.cos(Math.PI / EmberKilnRules.SECTORS)})
				for (int i = 0; i < EmberKilnRules.SECTORS; i++) {
					double a = EmberKilnRules.angle(i), b = EmberKilnRules.angle(i + 1);
					ring.add(new Vec3[] {origin.add(radius * Math.cos(a), .12, radius * Math.sin(a)),
						origin.add(radius * Math.cos(b), .12, radius * Math.sin(b))});
				}
			return ring;
		}
		if (age >= (attack == 7 ? GaleRepriseRules.TELL : StoneFractureRules.TELL)) return List.of();
		var result = new ArrayList<Vec3[]>();
		Vec3 feet = origin.add(0, .12, 0);
		if (age < (attack == 7 ? GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS : StoneFractureRules.PLANT + StoneFractureRules.BRACE)) {
			if (attack == 7) for (int sign : new int[] {-1, 1}) {
				Vec3 from = feet.add(0, 0, .45 * sign); result.add(new Vec3[] {from, from.add(-1.8, 0, 0)});
			} else result.add(new Vec3[] {feet.add(.7, 0, .8), feet.add(-.7, 0, .8)});
		} else {
			Vec3 start = feet.add(attack == 7 ? -1.8 : 0, 0, 0);
			Vec3 aim = attack == 7 ? new Vec3(1.8, 0, 4).normalize() : new Vec3(0, 0, 1);
			Vec3 edge = new Vec3(-aim.z, 0, aim.x).scale(attack == 7 ? .75 : .7), end = start.add(aim.scale(attack == 7 ? 5.25 : 6));
			for (int sign : new int[] {-1, 1}) result.add(new Vec3[] {start.add(edge.scale(sign)), end.add(edge.scale(sign))});
			result.add(new Vec3[] {end.subtract(edge), end.add(edge)});
		}
		return result;
	}
	private static boolean sameRay(Ray actual, Vec3[] expected) {
		return actual.from.distanceToSqr(expected[0]) < .0001 && actual.to.distanceToSqr(expected[1]) < .0001;
	}
	private static Point project(Minecraft mc, Vec3 point) {
		var camera = mc.gameRenderer.mainCamera();
		Vec3 relative = point.subtract(camera.position());
		var clip = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f())
			.transform(new org.joml.Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1));
		check(clip.isFinite() && clip.w > 0 && Math.abs(clip.x / clip.w) < .94 && Math.abs(clip.y / clip.w) < .94,
			"NPC and warnings must remain inside the native camera viewport");
		return new Point((clip.x / clip.w + 1) / 2, (1 - clip.y / clip.w) / 2);
	}
	/** Close views retain offscreen endpoints; behind-camera/depth-clipped endpoints have no screen sample. */
	private static Point projectUnclamped(Minecraft mc, Vec3 point) {
		var camera = mc.gameRenderer.mainCamera();
		Vec3 relative = point.subtract(camera.position());
		var clip = camera.getViewRotationProjectionMatrix(new Matrix4f())
			.transform(new org.joml.Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1));
		if (!clip.isFinite() || clip.w <= 0 || !insideDepth(clip)) return null;
		double x = (clip.x / clip.w + 1) / 2, y = (1 - clip.y / clip.w) / 2;
		return Double.isFinite(x) && Double.isFinite(y) ? new Point(x, y) : null;
	}
	private static Point[] projectRay(Minecraft mc, Ray ray, boolean fullCoverage) {
		Point[] points = new Point[18];
		points[0] = fullCoverage ? project(mc, ray.from) : projectUnclamped(mc, ray.from);
		points[1] = fullCoverage ? project(mc, ray.renderedEnd) : projectUnclamped(mc, ray.renderedEnd);
		if (!fullCoverage) for (int bin = 0; bin < 16; bin++)
			points[bin + 2] = projectUnclamped(mc, ray.from.lerp(ray.renderedEnd, (bin + .5) / 16));
		return points;
	}
	private static boolean clear(Minecraft mc, Vec3 eye, Vec3 point) {
		if (mc.level.clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player)).getType() != HitResult.Type.MISS) return false;
		for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity && entity != subject && entity != mc.player && !entity.isInvisible())
			if (entity.getBoundingBox().inflate(.05).clip(eye, point).isPresent()) return false;
		return true;
	}
	private static BodyPixels measureBody(List<Point> points, int[] pixels, int width, int height) {
		if (points.isEmpty()) return new BodyPixels(0, 0, 0, 0);
		int left = (int) (points.stream().mapToDouble(Point::x).min().orElseThrow() * width);
		int right = (int) (points.stream().mapToDouble(Point::x).max().orElseThrow() * width);
		int top = (int) (points.stream().mapToDouble(Point::y).min().orElseThrow() * height);
		int bottom = (int) (points.stream().mapToDouble(Point::y).max().orElseThrow() * height);
		int nonBlack = 0, chromatic = 0, minLuma = 255, maxLuma = 0;
		var colors = new java.util.HashSet<Integer>();
		for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) {
			int rgb = pixels[y * width + x], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			int luma = (54 * r + 183 * g + 19 * b) >> 8;
			minLuma = Math.min(minLuma, luma); maxLuma = Math.max(maxLuma, luma);
			if (luma > 24) nonBlack++;
			colors.add(rgb & 0xFFFFFF);
			if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 12) chromatic++;
		}
		return new BodyPixels(nonBlack, chromatic, colors.size(), maxLuma - minLuma);
	}

	private static WarningPixels measure(Ray ray, Point[] projected, int[] pixels, int width, int height, int attack, boolean fullCoverage) {
		int visible = 0, sampled = 0;
		for (int bin = 0; bin < 16; bin++) {
			double t = (bin + .5) / 16;
			Point point = fullCoverage ? new Point(projected[0].x + (projected[1].x - projected[0].x) * t,
				projected[0].y + (projected[1].y - projected[0].y) * t) : projected[bin + 2];
			if (point == null || !fullCoverage && (point.x < 0 || point.x >= 1 || point.y < 0 || point.y >= 1)) continue;
			sampled++;
			int cx = (int) (point.x * width), cy = (int) (point.y * height);
			boolean found = false;
			for (int y = Math.max(0, cy - 3); y <= Math.min(height - 1, cy + 3); y++)
				for (int x = Math.max(0, cx - 3); x <= Math.min(width - 1, cx + 3); x++) {
					int rgb = pixels[y * width + x], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
					if (attack == 7 || attack == 9 && ray.color == 0x73E2CB ? g > 130 && g > r + 3 && b > r : r > 130 && r > g + 3 && g > b + 3) found = true;
				}
			if (found) visible++;
		}
		return new WarningPixels(ray, projected[0], projected[1], visible, sampled);
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
