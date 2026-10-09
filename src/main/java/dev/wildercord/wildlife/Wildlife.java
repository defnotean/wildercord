package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Magical wildlife: the six creatures that make the world feel enchanted (see DESIGN.md, "Magical wildlife"), what
 * they leave behind, their spawn eggs and sounds. Where they spawn is {@link WildlifeSpawns}; their rules are
 * {@link WildlifeRules}. A creature seen up close goes into its finder's Grimoire (the field guide, {@link FieldGuide}).
 */
public final class Wildlife {
	private Wildlife() {}

	// ------------------------------------------------------------------ the creatures

	/** Soft-glowing moths that flutter in small swarms at night, drawn to lamps and to fresh spells. */
	public static final EntityType<Glimmerwing> GLIMMERWING = register("glimmerwing",
		EntityType.Builder.of(Glimmerwing::new, MobCategory.AMBIENT).sized(0.5F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8));
	/** A rare, shy deer of old forests with crystal antlers that brighten with the moon. */
	public static final EntityType<LumenStag> LUMEN_STAG = register("lumen_stag",
		EntityType.Builder.of(LumenStag::new, MobCategory.CREATURE).sized(0.9F, 1.6F).eyeHeight(1.45F).clientTrackingRange(10));
	/** A huge, slow tortoise with a garden growing on its shell. */
	public static final EntityType<MossbackTortoise> MOSSBACK_TORTOISE = register("mossback_tortoise",
		EntityType.Builder.of(MossbackTortoise::new, MobCategory.CREATURE).sized(1.4F, 1.05F).eyeHeight(0.75F).clientTrackingRange(10));
	/** A desert fox with an ember-tipped tail, tameable with rabbit. */
	public static final EntityType<Cinderfox> CINDERFOX = register("cinderfox",
		EntityType.Builder.of(Cinderfox::new, MobCategory.CREATURE).sized(0.6F, 0.7F).eyeHeight(0.55F).clientTrackingRange(8).fireImmune());
	/** A manta of the open sky, gliding in slow loops high over the mountains. */
	public static final EntityType<Skyray> SKYRAY = register("skyray",
		EntityType.Builder.of(Skyray::new, MobCategory.AMBIENT).sized(2.4F, 0.55F).eyeHeight(0.3F).clientTrackingRange(14));
	/** A quick snow hare that leaves fleeting frost prints. */
	public static final EntityType<Rimehare> RIMEHARE = register("rimehare",
		EntityType.Builder.of(Rimehare::new, MobCategory.CREATURE).sized(0.45F, 0.55F).eyeHeight(0.42F).clientTrackingRange(8));

	// ------------------------------------------------------------------ what they leave behind

	/** A glimmerwing's dust: brews Night Vision, and lights an ink sac. */
	public static final Item GLIMMER_DUST = item("glimmer_dust", new Item.Properties());
	/** A shed crystal antler: the heart of a Mana Crystal, in a diamond's place. Never taken from a stag that dies. */
	public static final Item LUMEN_ANTLER = item("lumen_antler", new Item.Properties().rarity(Rarity.UNCOMMON));
	/** A plate of a mossback's shell: brews the Turtle Master, and mends a turtle shell. */
	public static final Item MOSSBACK_SCUTE = item("mossback_scute", new Item.Properties());
	/** A tuft of a cinderfox's tail: brews Fire Resistance, and burns a long while in a furnace (eight items). */
	public static final Item EMBER_TUFT = item("ember_tuft", new Item.Properties()
		.component(DataComponents.COOKING_FUEL, new CookingFuel(new ResolvableInt.Constant(1600), new ResolvableFloat.Constant(1.0F))));
	/** A skyray's membrane: brews Slow Falling, and mends an elytra as a phantom's does. */
	public static final Item SKYRAY_MEMBRANE = item("skyray_membrane", new Item.Properties());
	/** A rimehare's frosted fur: weaves Rimebound armour in place of packed ice, and four make leather. */
	public static final Item RIME_FUR = item("rime_fur", new Item.Properties());

	/** Every drop, in the order of the creatures. */
	public static final List<Item> DROPS = List.of(GLIMMER_DUST, LUMEN_ANTLER, MOSSBACK_SCUTE, EMBER_TUFT, SKYRAY_MEMBRANE, RIME_FUR);

	// ------------------------------------------------------------------ spawn eggs

	public static final Item GLIMMERWING_SPAWN_EGG = egg("glimmerwing", GLIMMERWING);
	public static final Item LUMEN_STAG_SPAWN_EGG = egg("lumen_stag", LUMEN_STAG);
	public static final Item MOSSBACK_TORTOISE_SPAWN_EGG = egg("mossback_tortoise", MOSSBACK_TORTOISE);
	public static final Item CINDERFOX_SPAWN_EGG = egg("cinderfox", CINDERFOX);
	public static final Item SKYRAY_SPAWN_EGG = egg("skyray", SKYRAY);
	public static final Item RIMEHARE_SPAWN_EGG = egg("rimehare", RIMEHARE);

	public static final List<Item> EGGS = List.of(GLIMMERWING_SPAWN_EGG, LUMEN_STAG_SPAWN_EGG, MOSSBACK_TORTOISE_SPAWN_EGG, CINDERFOX_SPAWN_EGG,
		SKYRAY_SPAWN_EGG, RIMEHARE_SPAWN_EGG);

	/** Every wildlife type, in the guide's order. */
	public static final List<EntityType<?>> TYPES = List.of(GLIMMERWING, LUMEN_STAG, MOSSBACK_TORTOISE, CINDERFOX, SKYRAY, RIMEHARE);

	// ------------------------------------------------------------------ registering

	private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(path));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String path, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, new WildlifeItem(properties.setId(key)));
	}

	private static Item egg(String creature, EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(creature + "_spawn_egg"));
		return Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
	}

	/**
	 * A wildlife sound from the feel kit ({@code tools/feel/wildlife.py}), by the name after {@code wildlife_}; null when
	 * the kit has none, which plays nothing.
	 */
	public static @Nullable SoundEvent sound(String name) {
		return WildercordSounds.kit("wildlife_" + name);
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(GLIMMERWING, Glimmerwing.createAttributes());
		FabricDefaultAttributeRegistry.register(LUMEN_STAG, LumenStag.createAttributes());
		FabricDefaultAttributeRegistry.register(MOSSBACK_TORTOISE, MossbackTortoise.createAttributes());
		FabricDefaultAttributeRegistry.register(CINDERFOX, Cinderfox.createAttributes());
		FabricDefaultAttributeRegistry.register(SKYRAY, Skyray.createAttributes());
		FabricDefaultAttributeRegistry.register(RIMEHARE, Rimehare.createAttributes());
		WildlifeSpawns.init();

		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> {
			DROPS.forEach(output::accept);
			EGGS.forEach(output::accept);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> EGGS.forEach(output::accept));

		// A skyray's membrane mends an elytra as well as a phantom's does.
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(Items.ELYTRA, builder ->
			builder.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(Items.PHANTOM_MEMBRANE.builtInRegistryHolder(),
				SKYRAY_MEMBRANE.builtInRegistryHolder())))));

		// Glimmerwings are drawn to fresh magic: remember when each player last cast.
		WildercordEvents.AFTER_CAST.register((player, spell, runes, spent) -> LAST_CAST.put(player.getUUID(), player.level().getGameTime()));
		ServerTickEvents.END_SERVER_TICK.register(Wildlife::noticeCreatures);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST_CAST.clear());
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_CAST.remove(handler.player.getUUID()));
	}

	// ------------------------------------------------------------------ fresh spells

	private static final Map<UUID, Long> LAST_CAST = new HashMap<>();

	/** Whether {@code player} cast a spell within the last {@code ticks}. */
	public static boolean castRecently(ServerPlayer player, int ticks) {
		Long at = LAST_CAST.get(player.getUUID());
		return at != null && player.level().getGameTime() - at <= ticks;
	}

	// ------------------------------------------------------------------ the field guide

	/** How near a creature must be, and in sight, to count as met (blocks). */
	private static final double NOTICE = 14;

	/**
	 * Once a second (each player on their own tick of it), a creature from the field guide in plain sight near a player
	 * goes into their Grimoire, the first time. Survival or creative alike; spectators watch from outside the world.
	 */
	private static void noticeCreatures(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || (server.getTickCount() + player.getId()) % 20 != 0) {
				continue;
			}
			List<String> grimoire = Heart.grimoire(player);
			List<Entity> near = new ArrayList<>(player.level().getEntities(player, player.getBoundingBox().inflate(NOTICE),
				e -> e.isAlive() && FieldGuide.byType(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()).isPresent()));
			for (Entity creature : near) {
				String key = FieldGuide.key(BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()).toString());
				if (!grimoire.contains(key) && !creature.isInvisible() && player.hasLineOfSight(creature)) {
					Grimoire.unlock(player, key);
					grimoire = Heart.grimoire(player);
				}
			}
		}
	}
}
