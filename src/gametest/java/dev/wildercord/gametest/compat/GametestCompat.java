package dev.wildercord.gametest.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** 1.21.1 compat seam for the dropped render-state gametest helpers: inert. */
public final class GametestCompat {
	private GametestCompat() {}

	public static InputConstants.Key keyboardKey(int keyCode) {
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
	}

	public static void setFullscreen(Window window, boolean fullscreen) {
		if (window.isFullscreen() != fullscreen) {
			window.toggleFullScreen();
		}
	}

	public static void renderFrame(Minecraft mc, DeltaTracker delta) {}

	public static void extractMainCamera(GameRenderer gameRenderer, Object camera, DeltaTracker delta) {}

	public static BlockPos findNearestStructure(ServerLevel level, Holder<net.minecraft.world.level.levelgen.structure.Structure> structure, BlockPos origin, Object arg1, Object arg2) {
		return null;
	}

	public static Object generateStructureStart(ServerLevel level, Holder<net.minecraft.world.level.levelgen.structure.Structure> structure, Object chunk) {
		return null;
	}

	public static Object findBiomeHorizontal(Object source, int x, int y, int z, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		return null;
	}

	public static boolean biomeHasMob(Object biome, Object category, Object type) {
		return false;
	}

	public static Object configuredFeatureValue(Object placed) {
		return null;
	}

	public static Object dummyStructureContext(Object chunk, Object height) {
		return null;
	}

	public static Vec3 entityPositionSyncTarget(Object sync) {
		return Vec3.ZERO;
	}

	public static Vec3 moveEntityDeltaTarget(Object move, Vec3 base) {
		return base;
	}

	public static boolean syncVelocityFlag(ServerPlayer player) {
		return false;
	}

	public static NarrationElementOutput narrationOutput(StringBuilder said) {
		return null;
	}

	public static int particlePacketCount(Object packet) {
		return 0;
	}

	public static void swingMainHandWithItem(LocalPlayer player) {}
}
