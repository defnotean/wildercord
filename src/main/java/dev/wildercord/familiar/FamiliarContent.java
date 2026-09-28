package dev.wildercord.familiar;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

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
		new WispLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).setId(LANTERN_KEY)));

	/** A wisp's soft, airy chime as it drifts. */
	public static final SoundEvent WISP_AMBIENT = sound("wisp_ambient");
	/** A wild wisp drinking a spell of its element: a note that climbs with each offering. */
	public static final SoundEvent WISP_CHIME = sound("wisp_chime");
	/** A wisp bonding: a bright chord blooming. */
	public static final SoundEvent WISP_BOND = sound("wisp_bond");
	/** A familiar's little spell leaving it. */
	public static final SoundEvent WISP_CAST = sound("wisp_cast");
	/** A familiar growing stronger. */
	public static final SoundEvent WISP_LEVEL = sound("wisp_level");

	/** Each player's familiars. Saved, kept through death, and synced to them for the lantern's tooltip. */
	public static final AttachmentType<Bonds> BONDS = AttachmentRegistry.create(
		Wildercord.id("familiars"),
		builder -> builder
			.initializer(() -> Bonds.NONE)
			.persistent(Bonds.CODEC)
			.syncWith(Bonds.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	private static SoundEvent sound(String path) {
		Identifier id = Wildercord.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(WISP, Wisp.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(WISP_LANTERN));
	}
}
