package dev.wildercord.bedrock;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Entry point and runtime availability detection for optional Bedrock crossplay support
 * (Geyser-Fabric, Floodgate-Fabric, and Cumulus forms). All hooks degrade safely to no-ops when
 * those mods are not present so vanilla Java clients and standalone tests need zero extra jars.
 */
public final class BedrockBridge {
	private static final String GEYSER_MOD_ID = "geyser-fabric";
	private static final String FLOODGATE_MOD_ID = "floodgate";

	private BedrockBridge() {}

	public static boolean isGeyserLoaded() {
		return isModOrClassPresent(GEYSER_MOD_ID, "org.geysermc.geyser.api.GeyserApi");
	}

	public static boolean isFloodgateLoaded() {
		return isModOrClassPresent(FLOODGATE_MOD_ID, "org.geysermc.floodgate.api.FloodgateApi");
	}

	public static boolean isAnyLoaded() {
		return isGeyserLoaded() || isFloodgateLoaded();
	}

	public static void init() {
		BedrockMappings.ensureLoaded();
		BedrockForms.init();
		BedrockHud.init();
		BedrockAuraForms.init();
		BedrockInteractBridge.init();
		BedrockVfxBridge.init();
	}

	private static boolean isModOrClassPresent(String modId, String className) {
		try {
			if (FabricLoader.getInstance().isModLoaded(modId)) {
				return true;
			}
		} catch (Throwable ignored) {
			// Standalone unit tests do not initialize FabricLoader.
		}
		try {
			Class.forName(className, false, BedrockBridge.class.getClassLoader());
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
