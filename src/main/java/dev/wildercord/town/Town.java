package dev.wildercord.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

/**
 * The Wayfarer Inn's town life (0.12 "Tempering"): its bounty board, the keepers who trade there (a cook, a stablemaster and a
 * Master's emissary) and each traveller's standing with them. Art and text are written by tools/town_art.py.
 */
public final class Town {
	private Town() {}

	/**
	 * A bounty being hunted: {@code kills} of {@code needed} {@code target}s, counted only within range of the board at
	 * ({@code boardX}, {@code boardZ}) in {@code dimension}.
	 */
	public record Active(String target, int needed, int kills, int emeralds, int reputation, int boardX, int boardZ, String dimension) {
		public static final Codec<Active> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("target").forGetter(Active::target),
			Codec.INT.fieldOf("needed").forGetter(Active::needed),
			Codec.INT.fieldOf("kills").forGetter(Active::kills),
			Codec.INT.fieldOf("emeralds").forGetter(Active::emeralds),
			Codec.INT.fieldOf("reputation").forGetter(Active::reputation),
			Codec.INT.fieldOf("board_x").forGetter(Active::boardX),
			Codec.INT.fieldOf("board_z").forGetter(Active::boardZ),
			Codec.STRING.fieldOf("dimension").forGetter(Active::dimension)
		).apply(i, Active::new));

		public boolean done() {
			return kills >= needed;
		}

		Active killed() {
			return new Active(target, needed, kills + 1, emeralds, reputation, boardX, boardZ, dimension);
		}
	}

	/** A traveller's standing with the inns: reputation, the day they last turned a bounty in (-1 for never), and the bounty they hold. */
	public record Standing(int reputation, long lastDay, Optional<Active> bounty) {
		public static final Standing NONE = new Standing(0, -1, Optional.empty());
		public static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("reputation", 0).forGetter(Standing::reputation),
			Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(Standing::lastDay),
			Active.CODEC.optionalFieldOf("bounty").forGetter(Standing::bounty)
		).apply(i, Standing::new));

		public BountyRules.Tier tier() {
			return BountyRules.Tier.of(reputation);
		}

		Standing with(Optional<Active> bounty) {
			return new Standing(reputation, lastDay, bounty);
		}
	}

	public static final AttachmentType<Standing> STANDING = AttachmentRegistry.create(Wildercord.id("town_standing"),
		builder -> builder.initializer(() -> Standing.NONE).persistent(Standing.CODEC).copyOnDeath());

	public static Standing standing(ServerPlayer player) {
		return player.getAttachedOrCreate(STANDING);
	}

	public static void set(ServerPlayer player, Standing standing) {
		player.setAttached(STANDING, standing);
	}

	// ------------------------------------------------------------------ registry

	private static final ResourceKey<Block> BOARD_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("bounty_board"));
	public static final BountyBoardBlock BOUNTY_BOARD = Registry.register(BuiltInRegistries.BLOCK, BOARD_KEY,
		new BountyBoardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)
			.noOcclusion().noLootTable().setId(BOARD_KEY)));
	private static final ResourceKey<Item> BOARD_ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("bounty_board"));
	public static final Item BOUNTY_BOARD_ITEM = Registry.register(BuiltInRegistries.ITEM, BOARD_ITEM_KEY,
		new BlockItem(BOUNTY_BOARD, new Item.Properties().setId(BOARD_ITEM_KEY).useBlockDescriptionPrefix()));

	private static final ResourceKey<Item> DEED_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("ridgeback_deed"));
	public static final Item RIDGEBACK_DEED = Registry.register(BuiltInRegistries.ITEM, DEED_KEY,
		new RidgebackDeedItem(new Item.Properties().setId(DEED_KEY).stacksTo(1).rarity(Rarity.UNCOMMON)));

	private static final ResourceKey<EntityType<?>> KEEPER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("wayfarer_keeper"));
	public static final EntityType<WayfarerKeeper> KEEPER = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEEPER_KEY,
		EntityType.Builder.of(WayfarerKeeper::new, MobCategory.MISC).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build(KEEPER_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(KEEPER, Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player) {
				counted(player, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.getX(), entity.getZ(),
					entity.level().dimension().identifier().toString());
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(RIDGEBACK_DEED));
	}

	/** A kill by {@code player}: counts toward their bounty when it's the right creature, near enough its board. */
	public static void counted(ServerPlayer player, String type, double x, double z, String dimension) {
		Standing standing = standing(player);
		if (standing.bounty().isEmpty()) return;
		Active bounty = standing.bounty().get();
		if (bounty.done() || !bounty.target().equals(type) || !bounty.dimension().equals(dimension)
			|| !BountyRules.near(x - (bounty.boardX() + 0.5), z - (bounty.boardZ() + 0.5))) {
			return;
		}
		Active next = bounty.killed();
		set(player, standing.with(Optional.of(next)));
		player.sendOverlayMessage(next.done()
			? Component.translatable("message.wildercord.bounty.complete", targetName(next.target())).withStyle(ChatFormatting.GOLD)
			: Component.translatable("message.wildercord.bounty.progress", next.kills(), next.needed(), targetName(next.target())).withStyle(ChatFormatting.YELLOW));
	}

	public static Component targetName(String id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).map(EntityType::getDescription)
			.orElse(Component.literal(id));
	}
}
