package net.minecraft.client.model.geom;
public class ModelPart {
	public boolean visible = true;
	public float x, y, z, xRot, yRot, zRot;
	public float xScale = 1, yScale = 1, zScale = 1;
	public record Initial(float x, float y, float z, float xRot, float yRot, float zRot) {}
	public Initial initial = new Initial(0, 0, 0, 0, 0, 0);
	public Initial getInitialPose() { return initial; }
}
