package dev.wildercord.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;

/** Exposes the per-frame extracted PlayerRenderState used during ItemInHandRenderer submission. */
public interface FirstPersonAvatarState {
	PlayerRenderState wildercord$handsAvatar(AbstractClientPlayer player, float partial);
	void wildercord$prepareHandsAvatar(AbstractClientPlayer player, float partial);
	void wildercord$clearHandsAvatar();
}
