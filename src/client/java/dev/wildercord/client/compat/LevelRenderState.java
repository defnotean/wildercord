package dev.wildercord.client.compat;

/**
 * Stands in for 1.21.9's {@code LevelRenderState}: the per-frame state a world-space hook reads. On
 * 1.21.8 only the camera part of it is used, so that is all it carries.
 */
public final class LevelRenderState {
	public final CameraRenderState cameraRenderState = new CameraRenderState();
}
