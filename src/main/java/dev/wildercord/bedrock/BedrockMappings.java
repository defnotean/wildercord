package dev.wildercord.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.content.Imbued;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.ritual.RitualTabletItem;
import dev.wildercord.ritual.Rituals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads {@code data/wildercord/bedrock/mappings.json} and translates non-vanilla Wildercord
 * items, runes, blocks, entities, particles, sounds, and effects into vanilla-compatible wire
 * carriers paired with the Wildercord Geyser Extension + Bedrock Resource Pack.
 */
public final class BedrockMappings {
	private static final String RESOURCE_PATH = "/wildercord-bedrock-mappings.json";

	public record ItemEntry(String javaCarrier, String bedrockId, String itemModel, int customModelData, String displayName) {}
	public record BlockEntry(String javaCarrierState, String javaFallbackBlock, String bedrockId) {}

	private static volatile BedrockMappings instance;

	private final Map<String, ItemEntry> items;
	private final Map<String, ItemEntry> runes;
	private final Map<String, BlockEntry> blocks;
	private final Map<String, String> entities;
	private final Map<String, String> particles;

	private final ConcurrentHashMap<Block, BlockState> blockStateCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<EntityType<?>, EntityType<?>> entityCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, SimpleParticleType> particleCache = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<java.util.UUID, Integer> PENDING_MENU_ROWS = new ConcurrentHashMap<>();

	private BedrockMappings(
		Map<String, ItemEntry> items,
		Map<String, ItemEntry> runes,
		Map<String, BlockEntry> blocks,
		Map<String, String> entities,
		Map<String, String> particles
	) {
		this.items = Collections.unmodifiableMap(items);
		this.runes = Collections.unmodifiableMap(runes);
		this.blocks = Collections.unmodifiableMap(blocks);
		this.entities = Collections.unmodifiableMap(entities);
		this.particles = Collections.unmodifiableMap(particles);
	}

	public static BedrockMappings ensureLoaded() {
		BedrockMappings cached = instance;
		if (cached != null) {
			return cached;
		}
		synchronized (BedrockMappings.class) {
			if (instance == null) {
				instance = loadFromClasspath();
			}
			return instance;
		}
	}

	private static BedrockMappings loadFromClasspath() {
		Map<String, ItemEntry> items = new LinkedHashMap<>();
		Map<String, ItemEntry> runes = new LinkedHashMap<>();
		Map<String, BlockEntry> blocks = new LinkedHashMap<>();
		Map<String, String> entities = new LinkedHashMap<>();
		Map<String, String> particles = new LinkedHashMap<>();

		try (InputStream stream = BedrockMappings.class.getResourceAsStream(RESOURCE_PATH)) {
			if (stream == null) {
				return new BedrockMappings(items, runes, blocks, entities, particles);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

			readItemSection(root.getAsJsonObject("items"), items);
			readItemSection(root.getAsJsonObject("runes"), runes);

			JsonObject blockObj = root.getAsJsonObject("blocks");
			if (blockObj != null) {
				for (Map.Entry<String, JsonElement> e : blockObj.entrySet()) {
					JsonObject spec = e.getValue().getAsJsonObject();
					blocks.put(e.getKey(), new BlockEntry(
						str(spec, "javaCarrierState", "minecraft:polished_deepslate"),
						str(spec, "javaFallbackBlock", "minecraft:polished_deepslate"),
						str(spec, "bedrockId", e.getKey())
					));
				}
			}

			JsonObject entObj = root.getAsJsonObject("entities");
			if (entObj != null) {
				for (Map.Entry<String, JsonElement> e : entObj.entrySet()) {
					JsonObject spec = e.getValue().getAsJsonObject();
					entities.put(e.getKey(), str(spec, "javaCarrierEntity", "minecraft:zombie"));
				}
			}

			JsonObject partObj = root.getAsJsonObject("particles");
			if (partObj != null) {
				for (Map.Entry<String, JsonElement> e : partObj.entrySet()) {
					particles.put(e.getKey(), e.getValue().getAsString());
				}
			}
		} catch (Exception ignored) {
		}
		return new BedrockMappings(items, runes, blocks, entities, particles);
	}

	private static void readItemSection(JsonObject obj, Map<String, ItemEntry> dest) {
		if (obj == null) {
			return;
		}
		for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
			JsonObject spec = e.getValue().getAsJsonObject();
			dest.put(e.getKey(), new ItemEntry(
				str(spec, "javaCarrier", "minecraft:paper"),
				str(spec, "bedrockId", e.getKey()),
				str(spec, "itemModel", e.getKey()),
				spec.has("customModelData") ? spec.get("customModelData").getAsInt() : 10000,
				str(spec, "displayName", e.getKey())
			));
		}
	}

	private static String str(JsonObject obj, String key, String fallback) {
		JsonElement el = obj.get(key);
		return el != null && !el.isJsonNull() ? el.getAsString() : fallback;
	}

	// ---- Pure query helpers (safe for JUnit without Minecraft registry initialization)

	public Optional<ItemEntry> lookupItem(String javaId) {
		return Optional.ofNullable(items.get(javaId));
	}

	public Optional<ItemEntry> lookupRune(String runeId) {
		return Optional.ofNullable(runes.get(runeId));
	}

	public Optional<BlockEntry> lookupBlock(String javaId) {
		return Optional.ofNullable(blocks.get(javaId));
	}

	public Optional<String> lookupEntity(String javaId) {
		return Optional.ofNullable(entities.get(javaId));
	}

	public Optional<String> lookupParticle(String javaId) {
		return Optional.ofNullable(particles.get(javaId));
	}

	public int itemCount() { return items.size(); }
	public int runeCount() { return runes.size(); }
	public int blockCount() { return blocks.size(); }
	public int entityCount() { return entities.size(); }
	public int particleCount() { return particles.size(); }

	// ---- Runtime translation helpers for outbound Bedrock packets

	public static boolean isModdedId(Identifier id) {
		return id != null && !Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace());
	}

	public static boolean isModdedBlock(BlockState state) {
		return state != null && isModdedId(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
	}

	public static boolean isModdedBlockEntity(BlockEntityType<?> type) {
		return type != null && isModdedId(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type));
	}

	public static boolean isModdedEntity(EntityType<?> type) {
		return type != null && isModdedId(BuiltInRegistries.ENTITY_TYPE.getKey(type));
	}

	/**
	 * Keeps base Entity (0..7) or LivingEntity (0..14) metadata indices when a modded entity is mapped to
	 * a vanilla carrier (e.g., zombie or snowball), stripping subclass-specific IDs whose wire types
	 * can collide with vanilla entity metadata definitions in Geyser (such as RuneBolt ID 8 colliding with Snowball ItemStack).
	 */
	public static boolean shouldKeepModdedEntityDataId(boolean isLiving, int id) {
		return id >= 0 && id <= (isLiving ? 14 : 7);
	}

	public static void setPendingMenuRows(java.util.UUID playerId, int rows) {
		if (playerId != null) {
			PENDING_MENU_ROWS.put(playerId, Math.clamp(rows, 1, 6));
		}
	}

	public static int consumePendingMenuRows(java.util.UUID playerId, int fallbackRows) {
		Integer rows = playerId == null ? null : PENDING_MENU_ROWS.remove(playerId);
		return rows != null ? Math.clamp(rows, 1, 6) : Math.clamp(fallbackRows, 1, 6);
	}

	public static ItemStack translateItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return stack;
		}
		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!isModdedId(itemId)) {
			return translateVanillaWildercordComponents(stack);
		}

		BedrockMappings map = ensureLoaded();
		String key = itemId.toString();
		ItemEntry entry = null;

		String runeId = stack.get(WildercordComponents.RUNE);
		if (runeId != null && !runeId.isEmpty()) {
			entry = map.runes.get(runeId);
		}
		if (entry == null) {
			entry = map.items.get(key);
		}

		Item carrier = Items.PAPER;
		String modelId = key;
		int cmd = Math.abs(key.hashCode()) % 500000 + 20000;
		if (entry != null) {
			Identifier carrierId = Identifier.tryParse(entry.javaCarrier());
			if (carrierId != null && BuiltInRegistries.ITEM.containsKey(carrierId)) {
				carrier = BuiltInRegistries.ITEM.getValue(carrierId);
			}
			modelId = entry.itemModel();
			cmd = entry.customModelData();
		}

		ItemStack out = new ItemStack(carrier, stack.getCount());
		Identifier modelLoc = Identifier.tryParse(modelId);
		if (modelLoc != null) {
			out.set(DataComponents.ITEM_MODEL, modelLoc);
		}
		out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) cmd), List.of(), List.of(), List.of()));

		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		out.set(DataComponents.CUSTOM_NAME, BedrockTextTranslator.translate(customName != null ? customName : stack.getHoverName()));

		List<Component> loreLines = new ArrayList<>();
		ItemLore existingLore = stack.get(DataComponents.LORE);
		if (existingLore != null) {
			for (Component line : existingLore.lines()) {
				if (loreLines.size() < 14) {
					loreLines.add(BedrockTextTranslator.translate(line));
				}
			}
		}
		try {
			stack.getItem().appendHoverText(
				stack,
				Item.TooltipContext.EMPTY,
				TooltipDisplay.DEFAULT,
				line -> {
					if (loreLines.size() < 14) {
						loreLines.add(BedrockTextTranslator.translate(line));
					}
				},
				TooltipFlag.NORMAL
			);
		} catch (Throwable ignored) {
		}
		appendWildercordOverlayLore(stack, loreLines);
		if (!loreLines.isEmpty()) {
			out.set(DataComponents.LORE, new ItemLore(loreLines));
		}

		copyComponent(stack, out, DataComponents.DAMAGE);
		copyComponent(stack, out, DataComponents.MAX_DAMAGE);
		copyComponent(stack, out, DataComponents.MAX_STACK_SIZE);
		copyComponent(stack, out, DataComponents.ENCHANTMENTS);
		copyComponent(stack, out, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		copyComponent(stack, out, DataComponents.DYED_COLOR);
		copyComponent(stack, out, DataComponents.RARITY);
		copyComponent(stack, out, DataComponents.UNBREAKABLE);
		copyComponent(stack, out, DataComponents.FOOD);
		copyComponent(stack, out, DataComponents.CONSUMABLE);
		copyComponent(stack, out, DataComponents.EQUIPPABLE);
		copyComponent(stack, out, DataComponents.POTION_CONTENTS);
		copyComponent(stack, out, DataComponents.ATTRIBUTE_MODIFIERS);

		WrittenBookContent book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (book != null) {
			out.set(DataComponents.WRITTEN_BOOK_CONTENT, translateBookPages(book));
		}

		if (stack.getItem() instanceof RitualTabletItem && out.get(DataComponents.CONSUMABLE) == null) {
			float secs = Math.max(1.0F, Rituals.ritual(stack).channel / 20.0F);
			out.set(DataComponents.CONSUMABLE, new Consumable(
				secs,
				ItemUseAnimation.BOW,
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME),
				false,
				List.of()
			));
		}

		return out;
	}

	private static ItemStack translateVanillaWildercordComponents(ItemStack stack) {
		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		Component itemName = stack.get(DataComponents.ITEM_NAME);
		ItemLore existingLore = stack.get(DataComponents.LORE);
		WrittenBookContent book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		Imbued imbued = stack.get(WildercordComponents.IMBUED);
		BladeBond bond = BondedBlades.bond(stack);
		Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
		ItemEntry modelEntry = (modelId != null && isModdedId(modelId)) ? ensureLoaded().items.get(modelId.toString()) : null;

		boolean nameHasKey = BedrockTextTranslator.containsWildercordKey(customName)
			|| BedrockTextTranslator.containsWildercordKey(itemName);
		boolean loreHasKey = false;
		if (existingLore != null) {
			for (Component line : existingLore.lines()) {
				if (BedrockTextTranslator.containsWildercordKey(line)) {
					loreHasKey = true;
					break;
				}
			}
		}
		boolean bookHasKey = false;
		if (book != null) {
			for (Component page : book.getPages(false)) {
				if (BedrockTextTranslator.containsWildercordKey(page)) {
					bookHasKey = true;
					break;
				}
			}
		}
		if (!nameHasKey && !loreHasKey && !bookHasKey && imbued == null && bond == null && modelEntry == null) {
			return stack;
		}

		ItemStack out = stack.copy();
		if (modelEntry != null && out.get(DataComponents.CUSTOM_MODEL_DATA) == null) {
			out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) modelEntry.customModelData()), List.of(), List.of(), List.of()));
		}
		if (customName != null && BedrockTextTranslator.containsWildercordKey(customName)) {
			out.set(DataComponents.CUSTOM_NAME, BedrockTextTranslator.translate(customName));
		}
		if (itemName != null && BedrockTextTranslator.containsWildercordKey(itemName)) {
			out.set(DataComponents.ITEM_NAME, BedrockTextTranslator.translate(itemName));
		}
		if (bookHasKey) {
			out.set(DataComponents.WRITTEN_BOOK_CONTENT, translateBookPages(book));
		}
		if (loreHasKey || imbued != null || bond != null) {
			List<Component> lines = new ArrayList<>();
			if (existingLore != null) {
				for (Component line : existingLore.lines()) {
					lines.add(BedrockTextTranslator.translate(line));
				}
			}
			appendWildercordOverlayLore(stack, lines);
			if (!lines.isEmpty()) {
				out.set(DataComponents.LORE, new ItemLore(lines));
			}
		}
		return out;
	}

	private static WrittenBookContent translateBookPages(WrittenBookContent book) {
		List<Filterable<Component>> translated = new ArrayList<>();
		boolean changed = false;
		for (Component page : book.getPages(false)) {
			Component tr = BedrockTextTranslator.translate(page);
			if (tr != page) {
				changed = true;
			}
			translated.add(Filterable.passThrough(tr));
		}
		return changed ? book.withReplacedPages(translated) : book;
	}

	private static void appendWildercordOverlayLore(ItemStack stack, List<Component> lines) {
		Imbued imbued = stack.get(WildercordComponents.IMBUED);
		if (imbued != null && lines.size() < 14) {
			String runes = String.join(" + ", imbued.runes().stream()
				.map(r -> r.contains(":") ? r.substring(r.indexOf(':') + 1) : r).toList());
			lines.add(Component.literal("Imbued: " + runes + " (" + imbued.charges() + " charges)")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		BladeBond bond = BondedBlades.bond(stack);
		if (bond != null && lines.size() < 14) {
			String tierName = BladeRules.tierId(bond.growth().tier());
			String owner = bond.who().ownerName().isEmpty() ? "" : " (" + bond.who().ownerName() + ")";
			lines.add(Component.literal("Bonded Blade [" + tierName + "]" + owner).withStyle(ChatFormatting.GOLD));
			if (!bond.growth().trait().isEmpty() && lines.size() < 14) {
				lines.add(Component.literal("Trait: " + bond.growth().trait()).withStyle(ChatFormatting.AQUA));
			}
		}
	}

	private static <T> void copyComponent(ItemStack src, ItemStack dest, DataComponentType<T> type) {
		T val = src.get(type);
		if (val != null) {
			dest.set(type, val);
		}
	}

	public static BlockState translateBlockState(BlockState state) {
		if (state == null) {
			return Blocks.AIR.defaultBlockState();
		}
		Block block = state.getBlock();
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		if (!isModdedId(id)) {
			return state;
		}
		BedrockMappings map = ensureLoaded();
		return map.blockStateCache.computeIfAbsent(block, b -> resolveBlockCarrier(map, id.toString()));
	}

	private static BlockState resolveBlockCarrier(BedrockMappings map, String blockId) {
		BlockEntry entry = map.blocks.get(blockId);
		if (entry == null) {
			return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}
		BlockState parsed = parseBlockState(entry.javaCarrierState());
		return parsed != null ? parsed : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState parseBlockState(String spec) {
		if (spec == null || spec.isEmpty()) {
			return null;
		}
		int bracket = spec.indexOf('[');
		String blockName = bracket >= 0 ? spec.substring(0, bracket) : spec;
		Identifier id = Identifier.tryParse(blockName);
		if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
			return null;
		}
		BlockState state = BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
		if (bracket >= 0 && spec.endsWith("]")) {
			String inner = spec.substring(bracket + 1, spec.length() - 1);
			for (String pair : inner.split(",")) {
				String[] kv = pair.split("=", 2);
				if (kv.length != 2) {
					continue;
				}
				Property prop = state.getBlock().getStateDefinition().getProperty(kv[0].trim());
				if (prop != null) {
					Optional<?> parsed = prop.getValue(kv[1].trim());
					if (parsed.isPresent()) {
						state = state.setValue(prop, (Comparable) parsed.get());
					}
				}
			}
		}
		return state;
	}

	public static EntityType<?> translateEntityType(EntityType<?> type) {
		if (type == null) {
			return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
		}
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (!isModdedId(id)) {
			return type;
		}
		BedrockMappings map = ensureLoaded();
		return map.entityCache.computeIfAbsent(type, t -> {
			String target = map.entities.getOrDefault(id.toString(), "minecraft:zombie");
			Identifier tid = Identifier.tryParse(target);
			if (tid != null && BuiltInRegistries.ENTITY_TYPE.containsKey(tid)) {
				return BuiltInRegistries.ENTITY_TYPE.getValue(tid);
			}
			return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
		});
	}

	public static ParticleOptions translateParticle(ParticleOptions options) {
		if (options == null) {
			return ParticleTypes.ENCHANT;
		}
		Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
		if (!isModdedId(id)) {
			return options;
		}
		BedrockMappings map = ensureLoaded();
		return map.particleCache.computeIfAbsent(id.toString(), key -> {
			String vanilla = map.particles.getOrDefault(key, "minecraft:enchant");
			Identifier vid = Identifier.tryParse(vanilla);
			if (vid != null && BuiltInRegistries.PARTICLE_TYPE.containsKey(vid)) {
				var type = BuiltInRegistries.PARTICLE_TYPE.getValue(vid);
				if (type instanceof SimpleParticleType simple) {
					return simple;
				}
			}
			return ParticleTypes.ENCHANT;
		});
	}

	public static String toBedrockSoundKey(String namespace, String path) {
		return namespace + "." + path;
	}

	public static Holder<SoundEvent> translateSound(Holder<SoundEvent> sound) {
		if (sound == null) {
			return null;
		}
		Identifier id = sound.unwrapKey().map(k -> k.identifier()).orElseGet(() -> sound.value().location());
		if (isModdedId(id)) {
			// Geyser strips only the default namespace, leaving dot-prefixed keys matching sounds/sound_definitions.json (e.g. "wildercord.altar_fuse").
			return Holder.direct(SoundEvent.createVariableRangeEvent(
				Identifier.withDefaultNamespace(toBedrockSoundKey(id.getNamespace(), id.getPath()))
			));
		}
		return sound;
	}

	public static Holder<MobEffect> translateMobEffect(Holder<MobEffect> effect) {
		if (effect == null) {
			return MobEffects.GLOWING;
		}
		var key = effect.unwrapKey();
		if (key.isEmpty() || !isModdedId(key.get().identifier())) {
			return effect;
		}
		String path = key.get().identifier().getPath();
		if (path.contains("root") || path.contains("tether")) {
			return MobEffects.SLOWNESS;
		}
		if (path.contains("ward") || path.contains("shield") || path.contains("bastion")) {
			return MobEffects.RESISTANCE;
		}
		if (path.contains("downwind") || path.contains("swift") || path.contains("speed") || path.contains("torrent") || path.contains("gale")) {
			return MobEffects.SPEED;
		}
		if (path.contains("nourish")) {
			return MobEffects.SATURATION;
		}
		if (path.contains("focus")) {
			return MobEffects.NIGHT_VISION;
		}
		if (path.contains("clarity") || path.contains("mana") || path.contains("condens") || path.contains("deep")) {
			return MobEffects.HASTE;
		}
		if (path.contains("heal") || path.contains("mend")) {
			return MobEffects.REGENERATION;
		}
		return MobEffects.GLOWING;
	}
}
