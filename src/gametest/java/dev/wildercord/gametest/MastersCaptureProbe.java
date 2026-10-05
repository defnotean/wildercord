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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

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
	private static float[] handProjection;
	private static HandMotion handMotion;
	private static HudLayout hud;
	private static final List<HudSprite> hudSprites = new ArrayList<>();
	public record BladeBounds(double minX, double minY, double maxX, double maxY) {}
	public record HudLayout(int guiWidth, int guiHeight, int top) {}
	public record PixelStats(int cyan, double largestSpanGui, double fireFraction, int hudTopPixel) {}
	private record HudSprite(String sprite, int x, int y, int width, int height) {}
	private record HandMotion(float attackPhase, float inverseArmHeight, float previousHeight, float currentHeight, float modelSwapScale,
		boolean genuineEquip, float artOwnership, dev.wildercord.client.MastersArtPose.Frame renderedArt) {}
	private record CaptureEvidence(String name, int width, int height, boolean first, int swordSubmits, int bodySubmits,
		BladeBounds bladeBounds, HudLayout hud, List<HudSprite> hudSprites, float[] handProjection, HandMotion handMotion, PixelStats pixels) {}

	private static int owner, hands, bodies;
	private MastersCaptureProbe() {}

	public static void begin(Minecraft mc) {
		check(!mc.gui.hud.isHidden(), "HUD must remain visible: Minecraft skips first-person hands when it is hidden");
		owner = mc.player.getId();
		hands = bodies = 0;
		blade = null;
		handProjection = null;
		handMotion = null;
		hud = null;
		hudSprites.clear();
		armed = true;
	}

	/** Observe the native player-HUD sprites, including the actual heart/armor rows and hotbar. */
	public static void hudSprite(Identifier sprite, int x, int y, int width, int height, int guiWidth, int guiHeight) {
		if (!armed || !sprite.getNamespace().equals("minecraft")) return;
		String path = sprite.getPath();
		if (!(path.startsWith("hud/hotbar") || path.startsWith("hud/heart/") || path.startsWith("hud/armor_")
			|| path.startsWith("hud/food_") || path.startsWith("hud/air") || path.startsWith("hud/experience_bar"))) return;
		check(guiWidth > 0 && guiHeight > 0 && y >= 0, "Native HUD coordinates must be valid");
		check(hud == null || hud.guiWidth == guiWidth && hud.guiHeight == guiHeight, "HUD dimensions must stay stable during capture");
		hud = new HudLayout(guiWidth, guiHeight, hud == null ? y : Math.min(hud.top, y));
		hudSprites.add(new HudSprite(sprite.toString(), x, y, width, height));
	}

	public static void hand(int id, ItemStackRenderState item, PoseStack pose,
			net.minecraft.client.renderer.entity.state.AvatarRenderState avatar,
			net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands, float attack, float inverseHeight) {
		if (!armed || id != owner) return;
		MastersCaptureProbe.hands++;
		handMotion = new HandMotion(attack, inverseHeight, hands.oldMainHandHeight, hands.mainHandHeight, hands.mainHandSwapScale,
			((dev.wildercord.client.MastersHandMotionState) hands).wildercord$mainHandEquipping(),
			dev.wildercord.client.MastersArtPose.firstPersonOwnership(net.minecraft.world.InteractionHand.MAIN_HAND, avatar, hands),
			avatar.getData(dev.wildercord.client.MastersArtPose.FRAME));
		Minecraft mc = Minecraft.getInstance();
		var camera = new CameraRenderState();
		mc.gameRenderer.mainCamera().extractRenderState(camera, DELTA);
		var projection = new Projection();
		projection.setupPerspective(.05F, camera.depthFar, camera.hudFov, mc.getWindow().getWidth(), mc.getWindow().getHeight());
		var matrix = projection.getMatrix(new org.joml.Matrix4f()).mul(RenderSystem.getModelViewStack()).mul(pose.last().pose());
		handProjection = matrix.get(new float[16]);
		double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		item.visitExtents(point -> {
			var projected = matrix.transform(new org.joml.Vector4f(point.x(), point.y(), point.z(), 1));
			if (!projected.isFinite() || projected.w <= .05F) return;
			double x = (projected.x / projected.w + 1) / 2, y = (1 - projected.y / projected.w) / 2;
			bounds[0] = Math.min(bounds[0], x); bounds[1] = Math.min(bounds[1], y);
			bounds[2] = Math.max(bounds[2], x); bounds[3] = Math.max(bounds[3], y);
		});
		check(hud != null, "The capture must observe native HUD geometry before projecting the hand");
		check(bounds[0] < 1 && bounds[1] < (double) hud.top / hud.guiHeight && bounds[2] > 0 && bounds[3] > 0,
			"The submitted main-hand blade must intersect the visible viewport above the HUD");
		// Keep the complete bounds in the evidence; only the pixel scan is clipped to the viewport/HUD.
		blade = new BladeBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
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
		if (first && handMotion != null) Wildercord.LOGGER.info("MASTERS_NATIVE_HAND_MOTION name={} attack={} inverseHeight={} genuineEquip={} ownership={}",
			name, handMotion.attackPhase, handMotion.inverseArmHeight, handMotion.genuineEquip, handMotion.artOwnership);
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
			HudLayout capturedHud = hud;
			var capturedHudSprites = List.copyOf(hudSprites);
			float[] capturedProjection = handProjection;
			HandMotion capturedMotion = handMotion;
			int capturedHands = hands, capturedBodies = bodies;
			net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
				try (image) {
					var path = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name + ".png");
					Files.createDirectories(path.getParent());
					image.writeToFile(path); // Persist the exact native buffer, including rejected evidence.
					PixelStats pixels = measurePixels(image.getWidth(), image.getHeight(), image.getPixels(), capturedBlade, capturedHud);
					var evidence = new CaptureEvidence(name, image.getWidth(), image.getHeight(), first, capturedHands, capturedBodies,
						capturedBlade, capturedHud, capturedHudSprites, capturedProjection, capturedMotion, pixels);
					Files.writeString(path.resolveSibling(name + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence));
					Wildercord.LOGGER.info("MASTERS_NATIVE_PIXELS name={} hud={} bounds={} cyan={} spanGui={} fireFraction={}",
						name, capturedHud, capturedBlade, pixels.cyan, pixels.largestSpanGui, pixels.fireFraction);
					assertPixels(first, name, capturedBlade, pixels);
					result.complete(null);
				} catch (Throwable failure) { result.completeExceptionally(failure); }
			});
		} finally { end(); }
		return result;
	}

	/** Pure pixel policy, also exercised by the standalone regression harness. Never changes an image. */
	public static void assertPixels(int width, int height, int[] argb, boolean first, String name, BladeBounds bounds, HudLayout hud) {
		assertPixels(first, name, bounds, measurePixels(width, height, argb, bounds, hud));
	}

	private static void assertPixels(boolean first, String name, BladeBounds bounds, PixelStats pixels) {
		check(!first || bounds != null, "The captured native main-hand blade has no projected bounds");
		check(!first || pixels.cyan >= 8, "Native first-person frame must contain visible diamond-blade pixels above the observed HUD: " + name + " pixels=" + pixels.cyan);
		// An isolated crossguard fleck cannot prove a readable cutting edge. Require one contiguous
		// component at least as long as a native 16-GUI-pixel inventory icon, at the current GUI scale.
		check(!first || pixels.largestSpanGui >= 16, "Native first-person blade must have a readable span above the observed HUD: "
			+ name + " spanGui=" + pixels.largestSpanGui);
		check(pixels.fireFraction < .3, "Native camera is engulfed by target fire: " + name + " fraction=" + pixels.fireFraction);
	}

	public static PixelStats measurePixels(int width, int height, int[] argb, BladeBounds bounds, HudLayout hud) {
		check(width > 0 && height > 0 && argb.length == width * height, "Native frame dimensions must match");
		check(hud != null && hud.guiWidth > 0 && hud.guiHeight > 0 && hud.top > 0 && hud.top <= hud.guiHeight,
			"Native capture must include observed player-HUD dimensions");
		int hudTop = (int) Math.floor((double) hud.top * height / hud.guiHeight);
		boolean[] mask = new boolean[width * hudTop];
		int cyan = 0, fire = 0, firePixels = 0;
		for (int y = 0; y < Math.max(hudTop, height * 3 / 4); y++) for (int x = 0; x < width; x++) {
			int rgb = argb[y * width + x], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			if (y < hudTop && bounds != null && x >= bounds.minX * width && x <= bounds.maxX * width
				&& y >= bounds.minY * height && y <= bounds.maxY * height
				&& g > 75 && r < g * .7 && b > g * .65 && b < g * 1.3) {
				mask[y * width + x] = true;
				cyan++;
			}
			// Preserve the existing central-view fire policy; its sampling region is unrelated to HUD size.
			if (y < height * 3 / 4) {
				if (r > 170 && g > 65 && g < r * 1.02 && b < g * .98) fire++;
				firePixels++;
			}
		}
		int[] queue = new int[cyan];
		double largestSpan = 0;
		for (int start = 0; start < mask.length; start++) {
			if (!mask[start]) continue;
			mask[start] = false;
			int head = 0, tail = 1, minX = start % width, maxX = minX, minY = start / width, maxY = minY;
			queue[0] = start;
			while (head < tail) {
				int at = queue[head++], x = at % width, y = at / width;
				minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
				if (x > 0 && mask[at - 1]) { mask[at - 1] = false; queue[tail++] = at - 1; }
				if (x + 1 < width && mask[at + 1]) { mask[at + 1] = false; queue[tail++] = at + 1; }
				if (y > 0 && mask[at - width]) { mask[at - width] = false; queue[tail++] = at - width; }
				if (y + 1 < hudTop && mask[at + width]) { mask[at + width] = false; queue[tail++] = at + width; }
			}
			if (tail >= 8) largestSpan = Math.max(largestSpan, Math.max((double) (maxX - minX + 1) * hud.guiWidth / width,
				(double) (maxY - minY + 1) * hud.guiHeight / height));
		}
		return new PixelStats(cyan, largestSpan, firePixels == 0 ? 0 : (double) fire / firePixels, hudTop);
	}

	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
