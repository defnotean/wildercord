package dev.wildercord.client;
import dev.wildercord.aura.MastersArtAnimation;
public class MastersArtPose {
	public static final Object FRAME = new Object();
	public record Frame(MastersArtAnimation.Pose pose, boolean leftHanded, float yawDelta, float pitchDelta,
		float bladeTilt, long activation, int move) {}
}
