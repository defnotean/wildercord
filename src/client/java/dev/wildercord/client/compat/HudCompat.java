package dev.wildercord.client.compat;

import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21.5 has no {@code HudElementRegistry}: the layered HUD, with its vanilla anchors and its before/after
 * ordering, arrived with 1.21.6. This keeps the registration shape the HUD classes were written against and
 * routes every element through {@link HudRenderCallback}, which fires once vanilla has drawn its own HUD.
 * The anchor is carried for the call sites' sake; on this version everything lands on top of the vanilla
 * layers, so a panel that used to sit under the hotbar now sits over it. Nothing overlaps badly enough to
 * need the layered path back.
 */
public final class HudCompat {
	/** The vanilla elements the mod used to order itself against. */
	public enum Anchor {
		HOTBAR, CROSSHAIR, BOSS_BAR, MISC_OVERLAYS
	}

	private HudCompat() {
	}

	public static void after(Anchor anchor, ResourceLocation id, BiConsumer<GuiGraphics, DeltaTracker> renderer) {
		HudRenderCallback.EVENT.register(renderer::accept);
	}

	public static void before(Anchor anchor, ResourceLocation id, BiConsumer<GuiGraphics, DeltaTracker> renderer) {
		HudRenderCallback.EVENT.register(renderer::accept);
	}

	/** The old {@code addLast}: everything already lands last here. */
	public static void last(ResourceLocation id, BiConsumer<GuiGraphics, DeltaTracker> renderer) {
		HudRenderCallback.EVENT.register(renderer::accept);
	}
}
