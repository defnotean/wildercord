package dev.wildercord.client.cosmetic;

import dev.wildercord.Wildercord;
import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.cosmetic.CordStyles;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

/**
 * How a player's Cord style reaches the Cord layer: read from the synced attachment into the
 * render state as it's extracted (see {@code AvatarRendererStyleMixin}), then turned into the
 * beads' textures and colours. The Cosmetics page sets it by hand to preview a style.
 */
public final class CordStyleLook {
	private CordStyleLook() {}

	/** The style to draw this frame; absent means the default glass beads that follow the spell. */
	public static final RenderStateDataKey<CordStyles.Style> STYLE = RenderStateDataKey.create(() -> "wildercord:cord_style");

	private static final Identifier CORE = Wildercord.id("textures/entity/cord/bead_core.png");

	public static void extract(Avatar avatar, AvatarRenderState state) {
		CordStyles.Style style = avatar.getAttached(CordCosmetics.STYLE);
		state.setData(STYLE, style);
	}

	public static CordStyles.Style of(AvatarRenderState state) {
		CordStyles.Style style = state.getData(STYLE);
		return style == null ? CordStyles.Style.DEFAULT : style;
	}

	/** Glass beads are tinted glass, lit from within; every other material keeps its own colour around a glowing core. */
	public static boolean tinted(CordStyles.Style style) {
		return style.material().equals("glass");
	}

	/** The bead's own texture: glass is the plain white bead the rune's colour tints. */
	public static Identifier beadTexture(CordStyles.Style style, Identifier glass) {
		return tinted(style) ? glass : Wildercord.id("textures/entity/cord/bead_" + style.material() + ".png");
	}

	/** What glows: the whole glass bead, or just the core showing through the other materials. */
	public static Identifier glowTexture(CordStyles.Style style, Identifier glass) {
		return tinted(style) ? glass : CORE;
	}

	/** How strongly a material lets the glow through (glass fully; obsidian's core burns brightest against the dark). */
	public static float glowStrength(CordStyles.Style style) {
		return switch (style.material()) {
			case "gold" -> 0.75F;
			case "obsidian" -> 1.0F;
			case "amethyst" -> 0.9F;
			case "bone" -> 0.7F;
			case "prismarine" -> 0.85F;
			default -> 1.0F;
		};
	}

	/** A bead's glow colour: the rune's own, unless the style fixes one. */
	public static int glowColor(CordStyles.Style style, int rune) {
		int fixed = CordStyles.glowColor(style.glow());
		return fixed >= 0 ? fixed : rune;
	}
}
