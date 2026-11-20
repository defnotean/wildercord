package dev.wildercord.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Exposes the per-frame extracted AvatarRenderState used during ItemInHandRenderer submission. */
public interface FirstPersonAvatarState {
	AvatarRenderState wildercord$handsAvatar(AbstractClientPlayer player, float partial);
	void wildercord$prepareHandsAvatar(AbstractClientPlayer player, float partial);
	void wildercord$clearHandsAvatar();
}
