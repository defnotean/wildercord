package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

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
	public static final Item EMBER_TUFT = item("ember_tuft",
		dev.wildercord.compat.McBlocksAndItems.cookingFuelProperties(new Item.Properties(), 1600, 1.0F));
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
		return Registry.register(BuiltInRegistries.ITEM, key, new WildlifeItem(properties