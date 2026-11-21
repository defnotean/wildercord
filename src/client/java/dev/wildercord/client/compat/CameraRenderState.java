package dev.wildercord.client.compat;

import org.joml.Quaternionf;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Stands in for 1.21.9's {@code CameraRenderState}, which 1.21.8 has no equivalent of: where the frame's
 * camera is and which way it looks, so a world-space hook can place what it draws relative to the viewer.
 */
public final class CameraRenderState {
	public Vec3 pos = Vec3.ZERO;
	public Vec3 entityPos = Vec3.ZERO;
	public BlockPos blockPos = BlockPos.ZERO;
	public boolean initialized;
	public Quaternionf orientation = new Quaternionf();
}
