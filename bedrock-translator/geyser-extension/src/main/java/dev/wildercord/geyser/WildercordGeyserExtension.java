package dev.wildercord.geyser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Registers Wildercord's translated custom items and solid block overrides in Geyser so Java +
 * Bedrock + Console players share the exact same items, runes, weapons, and sanctuaries.
 */
public final class WildercordGeyserExtension implements Extension {
	private static final String BUNDLED_MAPPINGS = "/wildercord-geyser-mappings.json";

	@Subscribe
	public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
		JsonObject mappings = loadMappings();
		if (mappings == null) {
			logger().error("Could not load Wildercord Geyser mappings!");
			return;
		}
		int itemCount = registerItemSection(event, mappings.getAsJsonObject("items"));
		int runeCount = registerItemSection(event, mappings.getAsJsonObject("runes"));
		logger().info("Registered " + itemCount + " Wildercord items and " + runeCount + " rune icons for Bedrock/Console clients.");
	}

	@Subscribe
	public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
		JsonObject mappings = loadMappings();
		if (mappings == null || !mappings.has("blocks")) {
			return;
		}
		int registered = 0;
		JsonObject blocks = mappings.getAsJsonObject("blocks");
		for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
			JsonObject spec = entry.getValue().getAsJsonObject();
			String carrierState = str(spec, "javaCarrierState", "");
			String defaultTexture = str(spec, "defaultTexture", "");
			if (!carrierState.startsWith("minecraft:note_block[") || defaultTexture.isEmpty()) {
				continue;
			}
			try {
				String shortName = entry.getKey().contains(":") ? entry.getKey().substring(entry.getKey().indexOf(':') + 1) : entry.getKey();
				String safeName = "wildercord_" + shortName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
				String displayName = str(spec, "displayName", shortName);
				int light = spec.has("lightEmission") ? Math.clamp(spec.get("lightEmission").getAsInt(), 0, 15) : 0;

				CustomBlockComponents.Builder compBuilder = CustomBlockComponents.builder()
					.displayName(displayName)
					.geometry(GeometryComponent.builder().identifier("minecraft:geometry.full_block").build())
					.materialInstance("*", MaterialInstance.builder()
						.texture(defaultTexture)
						.renderMethod("opaque")
						.faceDimming(true)
						.ambientOcclusionExponent(1.0F)
						.build());
				if (light > 0) {
					compBuilder.lightEmission(light);
				}

				CustomBlockData blockData = CustomBlockData.builder()
					.name(safeName)
					.includedInCreativeInventory(false)
					.components(compBuilder.build())
					.build();

				event.register(blockData);
				event.registerOverride(carrierState, blockData.defaultBlockState());
				registered++;
			} catch (Exception ex) {
				logger().warning("Skipping custom block " + entry.getKey() + ": " + ex.getMessage());
			}
		}
		logger().info("Registered " + registered + " Wildercord custom blocks for Bedrock/Console clients.");
	}

	@Subscribe
	public void onDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
		JsonObject mappings = loadMappings();
		String expectedUuid = mappings != null ? str(mappings, "rpUuid", "") : "";
		boolean found = false;
		for (var pack : event.resourcePacks()) {
			if (!expectedUuid.isEmpty() && pack.uuid().toString().equalsIgnoreCase(expectedUuid)) {
				found = true;
				break;
			}
		}
		if (found) {
			logger().info("Wildercord Bedrock Resource Pack verified in Geyser packs (" + expectedUuid + ").");
		} else {
			logger().warning("Wildercord Bedrock Resource Pack (" + expectedUuid + ") was not found in Geyser packs folder. "
				+ "Copy wildercord-bedrock-rp.mcpack into Geyser's packs/ directory for textures and sounds.");
		}
	}

	@Subscribe
	public void onPostInit(GeyserPostInitializeEvent event) {
		logger().info("Wildercord Geyser Translator initialized (Java 26.3 <-> Bedrock/Console crossplay ready).");
	}

	private int registerItemSection(GeyserDefineCustomItemsEvent event, JsonObject obj) {
		if (obj == null) {
			return 0;
		}
		int registered = 0;
		for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
			JsonObject spec = entry.getValue().getAsJsonObject();
			try {
				String javaCarrier = str(spec, "javaCarrier", "minecraft:paper");
				String bedrockId = str(spec, "bedrockId", entry.getKey());
				String itemModel = str(spec, "itemModel", entry.getKey());
				String icon = str(spec, "icon", "wildercord.rune");
				String displayName = str(spec, "displayName", entry.getKey());
				boolean handheld = spec.has("handheld") && spec.get("handheld").getAsBoolean();
				boolean allowOffhand = !spec.has("allowOffhand") || spec.get("allowOffhand").getAsBoolean();
				CreativeCategory category = parseCategory(str(spec, "creativeCategory", "ITEMS"));

				CustomItemDefinition def = CustomItemDefinition.builder(Identifier.of(bedrockId), Identifier.of(itemModel))
					.displayName(displayName)
					.bedrockOptions(CustomItemBedrockOptions.builder()
						.icon(icon)
						.displayHandheld(handheld)
						.allowOffhand(allowOffhand)
						.creativeCategory(category))
					.build();

				event.register(Identifier.of(javaCarrier), def);
				registered++;
			} catch (Exception ex) {
				logger().warning("Skipping custom item " + entry.getKey() + ": " + ex.getMessage());
			}
		}
		return registered;
	}

	private static CreativeCategory parseCategory(String raw) {
		return switch (raw.toUpperCase(Locale.ROOT)) {
			case "EQUIPMENT" -> CreativeCategory.EQUIPMENT;
			case "NATURE" -> CreativeCategory.NATURE;
			case "CONSTRUCTION" -> CreativeCategory.CONSTRUCTION;
			default -> CreativeCategory.ITEMS;
		};
	}

	private JsonObject loadMappings() {
		Path override = dataFolder().resolve("wildercord-geyser-mappings.json");
		if (Files.isRegularFile(override)) {
			try (var reader = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			} catch (Exception ex) {
				logger().warning("Failed reading override mappings at " + override + ": " + ex.getMessage());
			}
		}
		try (InputStream in = WildercordGeyserExtension.class.getResourceAsStream(BUNDLED_MAPPINGS)) {
			if (in != null) {
				try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					return JsonParser.parseReader(reader).getAsJsonObject();
				}
			}
		} catch (Exception ex) {
			logger().error("Failed reading bundled mappings: " + ex.getMessage());
		}
		return null;
	}

	private static String str(JsonObject obj, String key, String fallback) {
		JsonElement el = obj.get(key);
		return el != null && !el.isJsonNull() ? el.getAsString() : fallback;
	}
}
