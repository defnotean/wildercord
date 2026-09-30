package dev.wildercord.menu;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Wildercord's menus: screens with slots, run by the server. */
public final class WildercordMenus {
	private WildercordMenus() {}

	public static final MenuType<FusionAltarMenu> FUSION_ALTAR = Registry.register(BuiltInRegistries.MENU, Wildercord.id("fusion_altar"),
		new MenuType<>(FusionAltarMenu::new, FeatureFlags.VANILLA_SET));

	/** An open backpack; the client is told its size and which slot holds it (see {@link BackpackMenu.Opening}). */
	public static final ExtendedMenuType<BackpackMenu, BackpackMenu.Opening> BACKPACK = Registry.register(BuiltInRegistries.MENU, Wildercord.id("backpack"),
		new ExtendedMenuType<>(BackpackMenu::new, BackpackMenu.Opening.STREAM_CODEC));

	public static void init() {}
}
