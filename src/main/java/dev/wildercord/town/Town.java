package dev.wildercord.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.DungeonBoss;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import dev.wildercord.compat.McAttachments;

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

	public static final AttachmentType<Standing> STANDING = McAttachments.create(Wildercord.id("town_standing"),
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
			.noOcclusion().noLootTable()