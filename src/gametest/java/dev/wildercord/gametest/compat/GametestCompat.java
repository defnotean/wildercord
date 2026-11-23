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

	/** 1.21.1 sends an entity's absolute position in a teleport packet; the target is the packet's own position. */
	public static Vec3 entityPositionSyncTarget(Object sync) {
		if (sync instanceof net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket teleport) {
			return new Vec3(teleport.getX(), teleport.getY(), teleport.getZ());
		}
		return Vec3.ZERO;
	}

	/**
	 * Whether the player is sending no movement input. 1.21.1 has no {@code Input} record and no
	 * {@code ServerPlayer#getLastClientInput}: the client's own input state is read directly, and a
	 * server-side player is judged by what that input has made them do.
	 */
	public static boolean inputIdle(net.minecraft.world.entity.player.Player player) {
		if (player instanceof LocalPlayer client) {
			net.minecraft.client.player.Input keys = client.input;
			return keys.forwardImpulse == 0.0F && keys.leftImpulse == 0.0F && !keys.up && !keys.down
				&& !keys.left && !keys.right && !keys.jumping && !keys.shiftKeyDown;
		}
		return !player.isShiftKeyDown() && !player.isSprinting() && player.getDeltaMovement().lengthSqr() < 1.0E-7;
	}

	/** The last input the client sent this player. 1.21.1 keeps it in private fields with no accessor. */
	public static Object lastClientInput(ServerPlayer player) {
		return null;
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

	/**
	 * Waits for the server to satisfy {@code ready}, looking once a tick. The newer versions hand this to
	 * fabric's own waiters; 1.21.1's harness has none, so it polls from the calling thread and stops after
	 * {@code ticks} either way, leaving the caller's own checks to report what the server actually did.
	 * The server handle is the harness's own context object, which these waiters only wake.
	 */
	public static void waitFor(ClientGameTestContext client, Object server,
			java.util.function.Predicate<Object> ready, int ticks) {
		for (int i = 0; i < Math.max(1, ticks); i++) {
			if (ready.test(server)) {
				return;
			}
			client.waitTicks(1);
		}
		ready.test(server);
	}

	/** Empties the particle engine between screenshots. 1.21.1's engine has no public clear. */
	public static void clearParticles(Object particleEngine) {}

	/** The keys the client is holding, as one string for a probe row. 1.21.1 keeps them as plain fields. */
	public static String inputSummary(LocalPlayer player) {
		net.minecraft.client.player.Input keys = player.input;
		return "[up=" + keys.up + ", down=" + keys.down + ", left=" + keys.left + ", right=" + keys.right
			+ ", jump=" + keys.jumping + ", shift=" + keys.shiftKeyDown
			+ ", forward=" + keys.forwardImpulse + ", strafe=" + keys.leftImpulse + "]";
	}
}
