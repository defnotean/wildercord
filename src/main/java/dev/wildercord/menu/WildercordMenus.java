package dev.wildercord.menu;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Wildercord's menus: screens with slots, run by the server. */
public final class WildercordMenus {
	private WildercordMenus() {}

	public static final MenuType<FusionAltarMenu> FUSION_ALTAR = Registry.register(BuiltInRegistries.MENU, Wildercord.id("fusion_altar"),
		new MenuType<>(FusionAltarMenu::new, FeatureFlags.VANILLA_SET));

	public static void init() {}
}
