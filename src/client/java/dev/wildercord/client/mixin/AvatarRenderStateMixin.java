package dev.wildercord.client.mixin;

import dev.wildercord.client.CastingPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries a player's casting pose from the renderer to the model. */
@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements CastingPose {
	@Unique
	private String wildercord$shape = "";
	@Unique
	private float wildercord$progress = -1;
	@Unique
	private boolean wildercord$charging;
	@Unique
	private dev.wildercord.player.WildercordAttachments.CordLook wildercord$cord = dev.wildercord.player.WildercordAttachments.CordLook.NONE;
	@Unique
	private float wildercord$glow = 1;

	@Override
	public dev.wildercord.player.WildercordAttachments.CordLook wildercord$cord() {
		return wildercord$cord;
	}

	@Override
	public void wildercord$setCord(dev.wildercord.player.WildercordAttachments.CordLook cord) {
		wildercord$cord = cord;
	}

	@Override
	public float wildercord$glow() {
		return wildercord$glow;
	}

	@Override
	public void wildercord$setGlow(float glow) {
		wildercord$glow = glow;
	}

	@Override
	public String wildercord$shape() {
		return wildercord$shape;
	}

	@Override
	public float wildercord$progress() {
		return wildercord$progress;
	}

	@Override
	public boolean wildercord$charging() {
		return wildercord$charging;
	}

	@Override
	public void wildercord$setPose(String shape, float progress, boolean charging) {
		wildercord$shape = shape;
		wildercord$progress = progress;
		wildercord$charging = charging;
	}
}
