package dev.wildercord.gametest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/** Test-only observations of the screenshot's native submit paths and unmodified framebuffer. */
public final class MastersCaptureProbe {
	public static final float PARTIAL = .5F;
	private static final DeltaTracker DELTA = new DeltaTracker() {
		public float getGameTimeDeltaTicks() { return PARTIAL; }
		public float getGameTimeDeltaPartialTick(boolean paused) { return PARTIAL; }
		public float getRealtimeDeltaTicks() { return PARTIAL; }
	};
	private static boolean armed;
	private static BladeBounds blade;
	public record BladeBounds(double minX, double minY, double maxX, double maxY) {}

	private static int owner, hands, bodies;
	private MastersCaptureProbe() {}

	public static void begin(Minecraft mc) {
		check(!mc.gui.hud.isHidden(), "HUD must remain visible: Minecraft skips first-person hands when it is hidden");
		owner = mc.player.getId();
		hands = bodies = 0;
		blade = null;
		armed = true;
	}

	public static void hand(int id, ItemStackRenderState item, PoseStack pose) {
		if (!armed || id != owner) return;
		hands++;
		Minecraft mc = Minecraft.getInstance();
		var camera = new CameraRenderState();
		mc.gameRenderer.mainCamera().extractRenderState(camera, DELTA);
		var projection = new Projection();
		projection.setupPerspective(.05F, camera.depthFar, camera.hudFov, mc.getWindow().getWidth(), mc.getWindow().getHeight());
		var matrix = projection.getMatrix(new org.joml.Matrix4f()).mul(RenderSystem.getModelViewStack()).mul(pose.last().pose());
		double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		item.visitExtents(point -> {
			var projected = matrix.transform(new org.joml.Vector4f(point.x(), point.y(), point.z(), 1));
			if (!projected.isFinite() || projected.w <= .05F) return;
			double x = (projected.x / projected.w + 1) / 2, y = (1 - projected.y / projected.w) / 2;
			bounds[0] = Math.min(bounds[0], x); bounds[1] = Math.min(bounds[1], y);
			bounds[2] = Math.max(bounds[2], x); bounds[3] = Math.max(bounds[3], y);
		});
		check(bounds[0] < 1 && bounds[1] < .75 && bounds[2] > 0 && bounds[3] > 0,
			"The submitted main-hand blade must intersect the visible viewport above the HUD");
		blade = new BladeBounds(Math.max(0, bounds[0]), Math.max(0, bounds[1]), Math.min(1, bounds[2]), Math.min(.75, bounds[3]));
	}
	public static void body(int id) { if (armed && id == owner) bodies++; }
	public static void end() { armed = false; }

	public static void verifyRender(Minecraft mc, String name) {
		boolean first = mc.options.getCameraType().isFirstPerson();
		check(first ? hands > 0 : bodies > 0, "Native screenshot must submit the owner's " + (first ? "main-hand sword" : "body") + ": " + name);
		if (!first) {
			var camera = mc.gameRenderer.mainCamera();
			Vec3 eye = camera.position();
			check(eye.distanceTo(mc.player.getEyePosition()) > 2, "Third-person camera must not collapse into the fighter");
			var torso = mc.player.position().add(0, 1, 0).subtract(eye);
			var projected = camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f())
				.transform(new org.joml.Vector4f((float) torso.x, (float) torso.y, (float) torso.z, 1));
			check(projected.isFinite() && projected.w > 0 && Math.abs(projected.x / projected.w) < .85
				&& Math.abs(projected.y / projected.w) < .85, "The native body must occupy the camera viewport");
			// Check feet, torso and head; a target between the front camera and hero used to pass model-only tests.
			for (double y : new double[] {.25, .9, 1.55}) for (double x : new double[] {-.15, 0, .15}) {
				Vec3 point = mc.player.position().add(x, y, 0);
				check(mc.level.clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS,
					"Stage geometry must not occlude the fighter: " + name);
				for (var entity : mc.level.entitiesForRendering()) {
					if (entity instanceof LivingEntity && entity != mc.player && !entity.isInvisible()) {
						check(!entity.getBoundingBox().inflate(.1).clip(eye, point).isPresent(),
							"A combat target must not occlude the fighter: " + name);
					}
				}
			}
		}
		Wildercord.LOGGER.info("MASTERS_NATIVE_RENDER name={} first={} swordSubmits={} bodySubmits={}", name, first, hands, bodies);
	}

	/** Same native update/extract/render/readback as Fabric, without waiting between phase renders. */
	public static CompletableFuture<Void> capture(Minecraft mc, String name) {
		var result = new CompletableFuture<Void>();
		begin(mc);
		try {
			mc.gameRenderer.update(DELTA);
			mc.gameRenderer.extract(DELTA, true);
			mc.gameRenderer.render();
			RenderSystem.getDevice().createCommandEncoder().submit();
			verifyRender(mc, name);
			boolean first = mc.options.getCameraType().isFirstPerson();
			BladeBounds capturedBlade = blade;
			net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
				try (image) {
					var path = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name + ".png");
					Files.createDirectories(path.getParent());
					image.writeToFile(path); // Persist the exact native buffer, including rejected evidence.
					assertPixels(image.getWidth(), image.getHeight(), image.getPixels(), first, name, capturedBlade);
					result.complete(null);
				} catch (Throwable failure) { result.completeExceptionally(failure); }
			});
		} finally { end(); }
		return result;
	}

	/** Pure pixel policy, also exercised by the standalone regression harness. Never changes an image. */
	public static void assertPixels(int width, int height, int[] argb, boolean first, String name) {
		assertPixels(width, height, argb, first, name, new BladeBounds(0, 0, 1, .75));
	}

	public static void assertPixels(int width, int height, int[] argb, boolean first, String name, BladeBounds bounds) {
		check(!first || bounds != null, "The captured native main-hand blade has no projected bounds");
		check(width > 0 && height > 0 && argb.length == width * height, "Native frame dimensions must match");
		int cyan = 0, fire = 0, pixels = 0;
		// Exclude the bottom HUD strip so the inventory sword icon cannot satisfy a missing viewmodel.
		for (int y = 0; y < height * 3 / 4; y++) for (int x = 0; x < width; x++) {
			int rgb = argb[y * width + x], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			if (bounds != null && x >= bounds.minX * width && x <= bounds.maxX * width && y >= bounds.minY * height && y <= bounds.maxY * height
				&& g > 75 && r < g * .7 && b > g * .65 && b < g * 1.3) cyan++;
			// Include the pale hot core, not only orange borders of the target's native fire sprite.
			if (r > 170 && g > 65 && g < r * 1.02 && b < g * .98) fire++;
			pixels++;
		}
		check(!first || cyan >= 8, "Native first-person frame must contain visible diamond-blade pixels above the HUD: " + name + " pixels=" + cyan);
		check(fire < pixels * .3, "Native camera is engulfed by target fire: " + name + " fraction=" + (double) fire / pixels);
	}

	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
