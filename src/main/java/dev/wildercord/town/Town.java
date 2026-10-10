package dev.wildercord.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.DungeonBoss;
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
import net.minecraft.world.entity.Entity;
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
import java.util.UUID;

/**
 * The Wayfarer Inn's town life (0.12 "Tempering"): its bounty board, the keepers who trade there (a cook, a stablemaster and a
 * Master's emissary) and each traveller's standing with them. Art and text are written by tools/town_art.py.
 */
public final class Town {
	private Town() {}

	/**
	 * A bounty being worked: {@code kills} of {@code needed} {@code target}s, of a {@link BountyRules.Kind} by its id. Hunts and
	 * great hunts count only within range of the board at ({@code boardX}, {@code boardZ}) in {@code dimension}; a gathering is
	 * checked at the board; a named elite is the one creature set loose for it, called {@code name}; a dungeon bounty counts any
	 * dungeon's guardian; an escort leads {@code name}'s pack llama to ({@code destX}, {@code destZ}).
	 */
	public record Active(String kind, String target, int needed, int kills, int emeralds, int reputation, int boardX, int boardZ,
			String dimension, String name, int destX, int destZ) {
		public static final Codec<Active> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("kind", "hunt").forGetter(Active::kind),
			Codec.STRING.fieldOf("target").forGetter(Active::target),
			Codec.INT.fieldOf("needed").forGetter(Active::needed),
			Codec.INT.fieldOf("kills").forGetter(Active::kills),
			Codec.INT.fieldOf("emeralds").forGetter(Active::emeralds),
			Codec.INT.fieldOf("reputation").forGetter(Active::reputation),
			Codec.INT.fieldOf("board_x").forGetter(Active::boardX),
			Codec.INT.fieldOf("board_z").forGetter(Active::boardZ),
			Codec.STRING.fieldOf("dimension").forGetter(Active::dimension),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Active::name),
			Codec.INT.optionalFieldOf("dest_x", 0).forGetter(Active::destX),
			Codec.INT.optionalFieldOf("dest_z", 0).forGetter(Active::destZ)
		).apply(i, Active::new));

		public Active(String kind, String target, int needed, int kills, int emeralds, int reputation, int boardX, int boardZ,
				String dimension, String name) {
			this(kind, target, needed, kills, emeralds, reputation, boardX, boardZ, dimension, name, 0, 0);
		}

		public BountyRules.Kind type() {
			return BountyRules.Kind.of(kind);
		}

		/** Whether it's done. A gathering is only checked at the board. */
		public boolean done() {
			return type() != BountyRules.Kind.GATHER && kills >= needed;
		}

		Active killed() {
			return new Active(kind, target, needed, kills + 1, emeralds, reputation, boardX, boardZ, dimension, name, destX, destZ);
		}

		/** This escort, bound for ({@code x}, {@code z}). */
		Active bound(int x, int z) {
			return new Active(kind, target, needed, kills, emeralds, reputation, boardX, boardZ, dimension, name, x, z);
		}
	}

	/**
	 * A traveller's standing with the inns: reputation, the day they last turned a bounty in (-1 for never), the bounty they hold,
	 * the week they last had a great hunt and the day bandits last raided an inn they were in (both -1 for never).
	 */
	public record Standing(int reputation, long lastDay, Optional<Active> bounty, long lastGreatWeek, long lastRaidDay) {
		public static final Standing NONE = new Standing(0, -1, Optional.empty(), -1, -1);
		public static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("reputation", 0).forGetter(Standing::reputation),
			Codec.LONG.optionalFieldOf("last_day", -1L).forGetter(Standing::lastDay),
			Active.CODEC.optionalFieldOf("bounty").forGetter(Standing::bounty),
			Codec.LONG.optionalFieldOf("last_great_week", -1L).forGetter(Standing::lastGreatWeek),
			Codec.LONG.optionalFieldOf("last_raid_day", -1L).forGetter(Standing::lastRaidDay)
		).apply(i, Standing::new));

		public Standing(int reputation, long lastDay, Optional<Active> bounty, long lastGreatWeek) {
			this(reputation, lastDay, bounty, lastGreatWeek, -1);
		}

		public BountyRules.Tier tier() {
			return BountyRules.Tier.of(reputation);
		}

		public Standing with(Optional<Active> bounty) {
			return new Standing(reputation, lastDay, bounty, lastGreatWeek, lastRaidDay);
		}

		/** This standing with {@code more} reputation (which can be negative, but never takes it below 0). */
		public Standing plus(int more) {
			return new Standing(Math.max(0, reputation + more), lastDay, bounty, lastGreatWeek, lastRaidDay);
		}

		/** This standing, raided on {@code day}. */
		public Standing raided(long day) {
			return new Standing(reputation, lastDay, bounty, lastGreatWeek, day);
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
				counted(player, entity);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(RIDGEBACK_DEED));
	}

	/** The tag an escort's pack llama carries, before the escorting traveller's id. */
	public static final String ESCORT_TAG = "wildercord.escort.";

	/** The tag a named elite carries: whose bounty it is. */
	public static String markTag(UUID player) {
		return "wildercord.bounty_mark." + player;
	}

	/** A kill by {@code player}: counts toward their bounty when it's what the bounty asks for. */
	public static void counted(ServerPlayer player, Entity victim) {
		Standing standing = standing(player);
		if (standing.bounty().isEmpty()) return;
		Active bounty = standing.bounty().get();
		switch (bounty.type()) {
			case ELITE -> {
				if (!bounty.done() && victim.entityTags().contains(markTag(player.getUUID()))) progress(player, standing, bounty);
			}
			case DUNGEON -> {
				if (!bounty.done() && victim instanceof DungeonBoss) progress(player, standing, bounty);
			}
			case GATHER, ESCORT -> { }
			default -> counted(player, BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), victim.getX(), victim.getZ(),
				victim.level().dimension().identifier().toString());
		}
	}

	/** A kill by {@code player} of a {@code type} at ({@code x}, {@code z}): counts toward a hunt when it's the right creature, near enough its board. */
	public static void counted(ServerPlayer player, String type, double x, double z, String dimension) {
		Standing standing = standing(player);
		if (standing.bounty().isEmpty()) return;
		Active bounty = standing.bounty().get();
		BountyRules.Kind kind = bounty.type();
		if ((kind != BountyRules.Kind.HUNT && kind != BountyRules.Kind.GREAT) || bounty.done() || !bounty.target().equals(type)
			|| !bounty.dimension().equals(dimension) || !BountyRules.near(x - (bounty.boardX() + 0.5), z - (bounty.boardZ() + 0.5))) {
			return;
		}
		progress(player, standing, bounty);
	}

	private static void progress(ServerPlayer player, Standing standing, Active bounty) {
		Active next = bounty.killed();
		set(player, standing.with(Optional.of(next)));
		player.sendOverlayMessage(next.done()
			? Component.translatable("message.wildercord.bounty.complete", targetName(next)).withStyle(ChatFormatting.GOLD)
			: Component.translatable("message.wildercord.bounty.progress", next.kills(), next.needed(), targetName(next)).withStyle(ChatFormatting.YELLOW));
	}

	/** A creature's or an item's name by its id. */
	public static Component targetName(String id) {
		if (id.equals(BountyRules.DUNGEON_GUARDIAN)) return Component.translatable("town.wildercord.dungeon_guardian");
		Identifier key = Identifier.tryParse(id);
		if (key == null) return Component.literal(id);
		Optional<Component> creature = BuiltInRegistries.ENTITY_TYPE.getOptional(key).map(EntityType::getDescription);
		if (creature.isPresent()) return creature.get();
		return BuiltInRegistries.ITEM.getOptional(key).map(item -> item.getName(item.getDefaultInstance())).orElse(Component.literal(id));
	}

	/** What a bounty is after, as it's shown: a named elite by its name. */
	public static Component targetName(Active bounty) {
		if (bounty.type() == BountyRules.Kind.ESCORT) return Component.translatable("town.wildercord.pack_llama", bounty.name());
		return bounty.name().isEmpty() ? targetName(bounty.target())
			: Component.translatable("town.wildercord.named_elite", bounty.name(), targetName(bounty.target()));
	}
}
