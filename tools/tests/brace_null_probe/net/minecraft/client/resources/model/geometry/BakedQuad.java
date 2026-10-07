package net.minecraft.client.resources.model.geometry;
/** Caller geometry shape only, never a native baked material assertion. */
public class BakedQuad {
 public org.joml.Vector3fc position(int index) { return new org.joml.Vector3f(index % 2, index / 2, .5F); }
}
