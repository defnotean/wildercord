package dev.wildercord.client.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * 1.21.5 has no {@code GameRenderer#projectPointToScreen}. The same maths by hand: the camera's rotation and
 * position make the view, the renderer's projection makes the clip, and the point is divided by w. A point at
 * or behind the eye comes back NaN, which is what the callers already test for.
 */
public final class McCamera {
	private McCamera() {
	}

	/** The point in screen space: x and y in [-1, 1], z the depth, or NaN when it is not in front of the eye. */
	public static Vec3 projectToScreen(Minecraft mc, Vec3 world) {
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 eye = camera.getPosition();
		Matrix4f view = new Matrix4f(camera.rotation());
		view.translate((float) -eye.x, (float) -eye.y, (float) -eye.z);
		Matrix4f clip = new Matrix4f(mc.gameRenderer.getProjectionMatrix(mc.getFrameTime()));
		clip.mul(view);
		Vector4f p = new Vector4f((float) world.x, (float) world.y, (float) world.z, 1.0F);
		p.mul(clip);
		if (p.w <= 1.0E-6F) {
			return new Vec3(Double.NaN, Double.NaN, Double.NaN);
		}
		return new Vec3(p.x / p.w, p.y / p.w, p.z / p.w);
	}
}
