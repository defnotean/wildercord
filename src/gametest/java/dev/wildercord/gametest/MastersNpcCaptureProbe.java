package dev.wildercord.gametest;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.MasterSchoolMotionChecks.ServerFrame;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterModel;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
	public record Ray(Vec3 from, Vec3 to, Vec3 renderedEnd, float width, int particleAge, int lifetime, float partial) {}
	public record WarningPixels(Ray ray, Point from, Point to, int coloredBins, int sampledBins) {}
	public record BodyPixels(int nonBlackPixels, int chromaticPixels, int distinctColors, int luminanceRange) {}
	public record Evidence(String name, String captureKind, String participantKind, String observerKind,
		boolean independentTrialPerFrame, String phase, int requestedTick, int width, int height, Timeline renderedTimeline, ServerFrame serverObservation,
		int bodySubmits, int modelPasses, float[] modelPose, Vec3 camera, Vec3 renderedPosition,
		List<Point> bodyPoints, BodyPixels bodyPixels, List<WarningPixels> warnings, String renderFailure) {}
	private static SwordMaster subject;
	private static Vec3 origin;
	private static AuraFighterRenderState renderedState;
	private static Timeline timeline;
	private static float[] modelPose;
	private static Vec3 renderedPosition;
	private static int bodies, models;
	private static final List<Ray> rays = new ArrayList<>();
	private MastersNpcCaptureProbe() {}

	public static void configure(Minecraft mc) {
		mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.options.fov().set(60);
		mc.options.bobView().set(false); mc.options.particles().set(ParticleStatus.ALL);
		mc.options.setCameraType(CameraType.FIRST_PERSON); mc.gui.setScreen(null); mc.resizeGui();
		if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
		MagicQuality.reducedFlash = false; MagicQuality.cameraShake = false;
	}
	public static void end() { subject = null; renderedState = null; rays.clear(); }

	/** Called only by the real registered MasterRenderer's extract method. */
	public static void extracted(SwordMaster master, AuraFighterRenderState state, float partial) {
		if (subject != master) return;
		renderedState = state;
		renderedPosition = new Vec3(state.x, state.y, state.z);
		float age = master.attackElapsed(partial);
		timeline = new Timeline(master.level().getGameTime(), master.level().getGameTime() - (long) master.attackElapsed(0),
			master.attackAnimation(), partial, age, master.attackTellTicks(), master.attackActiveTicks(), master.attackRecoveryTicks(),
			dev.wildercord.client.combat.ArticulatedCombat.frame(state) == null, state.getData(MasterModel.FRAME));
	}
	/** Called at the actual opaque living-model submit, not merely at renderer lookup. */
	public static void body(LivingEntityRenderState state) {
		if (subject != null && state == renderedState && !state.isInvisible && !state.isInvisibleToPlayer) bodies++;
	}
	/** Called after native animation setup during the rendered pass, never invoked by this probe. */
	public static void model(MasterModel model, AuraFighterRenderState state) {
		if (subject == null || state != renderedState) return;
		models++;
		ModelPart[] parts = {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
		modelPose = new float[parts.length * 6];
		for (int i = 0; i < parts.length; i++) {
			var p = parts[i]; int at = i * 6;
			modelPose[at] = p.x; modelPose[at + 1] = p.y; modelPose[at + 2] = p.z;
			modelPose[at + 3] = p.xRot; modelPose[at + 4] = p.yRot; modelPose[at + 5] = p.zRot;
		}
	}
	/** Native LightParticle ray extraction; its world position/color identifies the actual floor warning. */
	public static void ray(int color, Vec3 from, Vec3 delta, float width, int age, int lifetime, float partial) {
		if (subject == null || color != subject.auraColor() || Math.abs(from.y - origin.y - .12) > .01
			|| from.distanceToSqr(origin) > 64 || Math.abs(delta.y) > .001 || age + partial >= lifetime) return;
		rays.add(new Ray(from, from.add(delta), from.add(delta.scale(Math.min(1, (age + partial) / 2))), width, age, lifetime, partial));
	}

	public static CompletableFuture<Evidence> capture(Minecraft mc, SwordMaster master, String name, String phase,
		int requestedTick, int requiredWarningSegments, Vec3 acceptedOrigin, ServerFrame serverFrame) {
		check(subject == null, "NPC captures must not overlap");
		subject = master; origin = acceptedOrigin; timeline = null; renderedState = null; modelPose = null;
		bodies = models = 0; rays.clear();
		var result = new CompletableFuture<Evidence>();
		try {
			mc.gameRenderer.update(DELTA); mc.gameRenderer.extract(DELTA, true); mc.gameRenderer.render();
			RenderSystem.getDevice().createCommandEncoder().submit();
			Vec3 eye = mc.gameRenderer.mainCamera().position();
			List<Point> bodyPoints = new ArrayList<>();
			String failure = null;
			try {
				check(!mc.isPaused() && mc.player.isSpectator() && mc.options.getCameraType().isFirstPerson(), "The camera is an unpaused real spectator");
				check(bodies > 0 && models > 0 && modelPose != null, "Native NPC body submission and animated model passes must both occur");
				check(timeline != null && timeline.attackId == serverFrame.attackId() && timeline.acceptedTick == serverFrame.acceptedTick(), "The submitted model must belong to the accepted server form");
				check(Math.abs(timeline.clientGameTick - serverFrame.gameTick()) <= 1, "Server observation must be contemporaneous with native extraction");
				check(timeline.partial == PARTIAL && timeline.age >= requestedTick && timeline.age < requestedTick + 1, "Actual extraction must match the exact requested tick");
				check(timeline.frame != null && timeline.frame.move() == timeline.attackId && timeline.fallbackRig, "The actual original fallback rig must own these school frames");
				check(!master.isInvisible() && master.isAlive() && eye.distanceTo(master.position()) >= 4, "The complete master must be observable");
				for (double y : new double[] {.2, .95, 1.7}) for (double x : new double[] {-.15, 0, .15}) {
					Vec3 point = renderedPosition.add(x, y, 0);
					bodyPoints.add(project(mc, point));
					check(clear(mc, eye, point), "Stage and other bodies must not occlude the master");
				}
				double height = bodyPoints.stream().mapToDouble(Point::y).max().orElseThrow() - bodyPoints.stream().mapToDouble(Point::y).min().orElseThrow();
				check(height * mc.getWindow().getHeight() >= 70, "The visible native NPC must be large enough to review its motion");
				for (Vec3[] expected : expectedRays(phase, serverFrame.attackId(), acceptedOrigin))
					check(rays.stream().anyMatch(ray -> sameRay(ray, expected)), "Every actual warning segment must be extracted: " + phase);
				check(expectedRays(phase, serverFrame.attackId(), acceptedOrigin).size() == requiredWarningSegments, "The phase warning contract must match its geometry");
			} catch (Throwable problem) { failure = problem.toString(); }
			Timeline capturedTimeline = timeline;
			float[] capturedPose = modelPose;
			Vec3 capturedPosition = renderedPosition;
			int capturedBodies = bodies, capturedModels = models;
			List<Ray> capturedRays = List.copyOf(rays);
			var projectedRays = capturedRays.stream().map(ray -> new Point[] {project(mc, ray.from), project(mc, ray.renderedEnd)}).toList();
			String renderFailure = failure;
			net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
				try (image) {
					var path = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name + ".png");
					Files.createDirectories(path.getParent());
					image.writeToFile(path); // Exact native framebuffer, including failed evidence.
					var warnings = new ArrayList<WarningPixels>();
					int[] pixels = image.getPixels();
					BodyPixels bodyPixels = measureBody(bodyPoints, pixels, image.getWidth(), image.getHeight());
					for (int i = 0; i < capturedRays.size(); i++) warnings.add(measure(capturedRays.get(i), projectedRays.get(i), pixels, image.getWidth(), image.getHeight(), serverFrame.attackId()));
					var evidence = new Evidence(name, "native_unpaused_server_ai", "consenting_fabric_fake_player", "real_client_spectator",
						true, phase, requestedTick, image.getWidth(), image.getHeight(), capturedTimeline, serverFrame, capturedBodies, capturedModels,
						capturedPose, eye, capturedPosition, List.copyOf(bodyPoints), bodyPixels, warnings, renderFailure);
					Files.writeString(path.resolveSibling(name + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence));
					check(renderFailure == null, "Native NPC render rejected: " + renderFailure);
					check(bodyPixels.nonBlackPixels >= 48 && bodyPixels.distinctColors >= 8 && bodyPixels.luminanceRange >= 24,
						"The submitted NPC framebuffer region must contain nonblank body detail: " + name);
					for (Vec3[] expected : expectedRays(phase, serverFrame.attackId(), acceptedOrigin))
						check(warnings.stream().anyMatch(warning -> sameRay(warning.ray, expected) && warning.coloredBins >= 4),
							"Each warning segment must have visible school-colored framebuffer pixels across at least four of sixteen bins: " + name);
					Wildercord.LOGGER.info("MASTERS_NPC_CAPTURE name={} attack={} accepted={} age={} bodies={} models={} warnings={}",
						name, capturedTimeline.attackId, capturedTimeline.acceptedTick, capturedTimeline.age, capturedBodies, capturedModels, warnings.size());
					result.complete(evidence);
				} catch (Throwable problem) { result.completeExceptionally(problem); }
			});
		} finally { end(); }
		return result;
	}

	private static List<Vec3[]> expectedRays(String phase, int attack, Vec3 origin) {
		// The last real particle may fade through release; these beats require no warning segments.
		if (phase.equals("release") || phase.equals("recovery")) return List.of();
		var result = new ArrayList<Vec3[]>();
		Vec3 feet = origin.add(0, .12, 0);
		if (!phase.equals("reply_warning")) {
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

	private static WarningPixels measure(Ray ray, Point[] projected, int[] pixels, int width, int height, int attack) {
		int visible = 0;
		for (int bin = 0; bin < 16; bin++) {
			double t = (bin + .5) / 16;
			int cx = (int) ((projected[0].x + (projected[1].x - projected[0].x) * t) * width);
			int cy = (int) ((projected[0].y + (projected[1].y - projected[0].y) * t) * height);
			boolean found = false;
			for (int y = Math.max(0, cy - 3); y <= Math.min(height - 1, cy + 3); y++)
				for (int x = Math.max(0, cx - 3); x <= Math.min(width - 1, cx + 3); x++) {
					int rgb = pixels[y * width + x], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
					if (attack == 7 ? g > 130 && g > r + 3 && b > r : r > 130 && r > g + 3 && g > b + 3) found = true;
				}
			if (found) visible++;
		}
		return new WarningPixels(ray, projected[0], projected[1], visible, 16);
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
