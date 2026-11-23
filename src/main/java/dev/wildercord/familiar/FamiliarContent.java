package dev.wildercord.familiar;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import dev.wildercord.compat.McAttachments;

/**
 * Everything familiars register: the wisp, the Wisp Lantern, their sounds (synthesised by
 * {@code tools/sound_art.py}) and the {@link Bonds} each player keeps.
 */
public final class FamiliarContent {
	private FamiliarContent() {}

	private static final ResourceKey<EntityType<?>> WISP_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("wisp"));

	/** A small creature of light: wild along ley lines at night, or someone's familiar. */
	public static final EntityType<Wisp> WISP = Registry.register(BuiltInRegistries.ENTITY_TYPE, WISP_KEY,
		EntityType.Builder.<Wisp>of(Wisp::new, MobCategory.AMBIENT)
			.sized(0.45F, 0.45F)
			.eyeHeight(0.25F)
			.clientTrackingRange(8)
			.fireImmune()
			.build(WISP_KEY));

	private static final ResourceKey<Item> LANTERN_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("wisp_lantern"));

	/** Where familiars rest: use it to call one out or send it home, sneak-use to call the next. */
	public static final Item WISP_LANTERN = Registry.register(BuiltInRegistries.ITEM, LANTERN_KEY,
		new WispLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)