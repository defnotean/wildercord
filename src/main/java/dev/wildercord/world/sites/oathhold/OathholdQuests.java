package dev.wildercord.world.sites.oathhold;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.ServerState;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.FallenKnight;
import dev.wildercord.aura.world.OathboundBannerMarshal;
import dev.wildercord.aura.world.OathboundBastion;
import dev.wildercord.aura.world.OathboundBladeChampion;
import dev.wildercord.aura.world.OathboundHalberdier;
import dev.wildercord.cast.DungeonBoss;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.cooking.Meals;
import dev.wildercord.wildlife.WetlandGarden;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/**
 * Manga-style Oathhold Adventurer's Guild quest manager: persistent Copper (F) through Orichalcum (S)
 * license progression, Cleaning chores, Herb Foraging, Artisan Work Shifts, Courier Deliveries,
 * Monster Subjugations, and Colosseum Arena Bouts, plus {@code /adventurer}.
 */
public final class OathholdQuests {
	private OathholdQuests() {}

	public static final String QUEST_PEST_PREFIX = "wildercord.quest_pest.";

	public record ActiveJob(
		String jobId,
		String category,
		String targetKey,
		int needed,
		int progress,
		int emeralds,
		int rankPoints,
		String bonusItem,
		int originX,
		int originY,
		int originZ,
		int destX,
		int destZ
	) {
		public static final Codec<ActiveJob> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(ActiveJob::jobId),
			Codec.STRING.fieldOf("category").forGetter(ActiveJob::category),
			Codec.STRING.fieldOf("target").forGetter(ActiveJob::targetKey),
			Codec.INT.fieldOf("needed").forGetter(ActiveJob::needed),
			Codec.INT.fieldOf("progress").forGetter(ActiveJob::progress),
			Codec.INT.fieldOf("emeralds").forGetter(ActiveJob::emeralds),
			Codec.INT.fieldOf("rank_points").forGetter(ActiveJob::rankPoints),
			Codec.STRING.optionalFieldOf("bonus", "").forGetter(ActiveJob::bonusItem),
			Codec.INT.optionalFieldOf("ox", 0).forGetter(ActiveJob::originX),
			Codec.INT.optionalFieldOf("oy", 64).forGetter(ActiveJob::originY),
			Codec.INT.optionalFieldOf("oz", 0).forGetter(ActiveJob::originZ),
			Codec.INT.optionalFieldOf("dx", 0).forGetter(ActiveJob::destX),
			Codec.INT.optionalFieldOf("dz", 0).forGetter(ActiveJob::destZ)
		).apply(i, ActiveJob::new));

		ActiveJob withProgress(int nextProgress) {
			return new ActiveJob(jobId, category, targetKey, needed, nextProgress, emeralds, rankPoints, bonusItem,
				originX, originY, originZ, destX, destZ);
		}
	}

	public record AdventurerProfile(int rankPoints, int completedCount, Optional<ActiveJob> activeJob) {
		public static final AdventurerProfile EMPTY = new AdventurerProfile(0, 0, Optional.empty());
		public static final Codec<AdventurerProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("rank_points", 0).forGetter(AdventurerProfile::rankPoints),
			Codec.INT.optionalFieldOf("completed", 0).forGetter(AdventurerProfile::completedCount),
			ActiveJob.CODEC.optionalFieldOf("active").forGetter(AdventurerProfile::activeJob)
		).apply(i, AdventurerProfile::new));

		public OathholdQuestRules.Rank rank() {
			return OathholdQuestRules.Rank.ofPoints(rankPoints);
		}

		AdventurerProfile withJob(Optional<ActiveJob> next) {
			return new AdventurerProfile(rankPoints, completedCount, next);
		}
	}

	public static final AttachmentType<AdventurerProfile> PROFILE = AttachmentRegistry.create(
		Wildercord.id("adventurer_profile"),
		builder -> builder.initializer(() -> AdventurerProfile.EMPTY).persistent(AdventurerProfile.CODEC).copyOnDeath()
	);

	private static final Map<UUID, Set<BlockPos>> CHORE_SPOTS = new HashMap<>();
	private static final Map<UUID, Long> LAST_WORK_TICK = new HashMap<>();

	public static void init() {
		ServerState.forgetOnDisconnect(CHORE_SPOTS);
		ServerState.resetOnStop(CHORE_SPOTS);
		ServerState.forgetOnDisconnect(LAST_WORK_TICK);
		ServerState.resetOnStop(LAST_WORK_TICK);

		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> registerCommands(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(OathholdQuests::tick);

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			return onBlockUse(level, server, pos, state);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel && source.getEntity() instanceof ServerPlayer killer) {
				onKill(killer, entity);
			}
		});
	}

	public static String challengerTag(UUID player) {
		return "wildercord.arena_challenger." + player;
	}

	public static AdventurerProfile profile(ServerPlayer player) {
		return player.getAttachedOrElse(PROFILE, AdventurerProfile.EMPTY);
	}

	public static OathholdQuestRules.Rank rank(ServerPlayer player) {
		return profile(player).rank();
	}

	public static void openBoard(ServerPlayer player, BlockPos boardPos) {
		GuildBoardMenu.open(player, boardPos);
	}

	public static boolean acceptJob(ServerPlayer player, BlockPos boardPos, OathholdQuestRules.JobSpec spec) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isPresent()) {
			player.sendSystemMessage(Component.literal("[Guild] Complete or abandon your current request before taking another!")
				.withStyle(ChatFormatting.YELLOW));
			return false;
		}
		OathholdQuestRules.Rank currentRank = prof.rank();
		int maxAllowed = Math.min(OathholdQuestRules.Rank.S.ordinal(), currentRank.ordinal() + 1);
		if (spec.minRank().ordinal() > maxAllowed) {
			player.sendSystemMessage(Component.literal("[Guild] Requires Rank " + spec.minRank().name() + " or higher.")
				.withStyle(ChatFormatting.RED));
			return false;
		}

		ServerLevel level = (ServerLevel) player.level();
		Optional<OathholdOrigin.Origin> city = OathholdLaw.findContainingCity(level, boardPos);
		int ox = city.map(OathholdOrigin.Origin::originX).orElse(boardPos.getX());
		int oy = city.map(OathholdOrigin.Origin::baseY).orElse(boardPos.getY());
		int oz = city.map(OathholdOrigin.Origin::originZ).orElse(boardPos.getZ());

		int destX = boardPos.getX();
		int destZ = boardPos.getZ();
		if (spec.category() == OathholdQuestRules.JobCategory.CARGO_DELIVERY) {
			switch (spec.targetKey()) {
				case "harbor" -> { destX = ox + 538; destZ = oz; }
				case "colosseum" -> { destX = ox - 546; destZ = oz; }
				case "exchange" -> { destX = ox; destZ = oz + 548; }
				case "abbey" -> { destX = ox; destZ = oz - 546; }
				default -> {}
			}
		} else if (spec.category() == OathholdQuestRules.JobCategory.ARENA_BOUT) {
			destX = city.isPresent() ? ox - 612 : boardPos.getX();
			destZ = city.isPresent() ? oz : boardPos.getZ();
		}

		ActiveJob active = new ActiveJob(
			spec.id(), spec.category().id, spec.targetKey(), spec.needed(), 0,
			spec.emeralds(), spec.rankPoints(), spec.bonusItem(),
			ox, oy, oz, destX, destZ
		);
		player.setAttached(PROFILE, prof.withJob(Optional.of(active)));

		if (spec.category() == OathholdQuestRules.JobCategory.CLEANING) {
			int placed = spawnChoreSpots(level, player.getUUID(), boardPos, spec);
			player.sendSystemMessage(Component.literal("[Guild] Accepted Cleaning Request: " + spec.title()
				+ " (" + placed + " chore spots spawned nearby — break or right-click them to clean!).").withStyle(ChatFormatting.GREEN));
		} else if (spec.category() == OathholdQuestRules.JobCategory.MONSTER_HUNT
				&& !spec.targetKey().equals("dungeon_boss")
				&& (spec.targetKey().equals("minecraft:silverfish") || spec.targetKey().equals("minecraft:drowned"))) {
			spawnHuntPests(level, player.getUUID(), boardPos, spec);
			player.sendSystemMessage(Component.literal("[Guild] Accepted Hunt: " + spec.title()
				+ " — defeat " + spec.needed() + " targets!").withStyle(ChatFormatting.GOLD));
		} else if (spec.category() == OathholdQuestRules.JobCategory.CARGO_DELIVERY
				|| spec.category() == OathholdQuestRules.JobCategory.ARENA_BOUT) {
			player.sendSystemMessage(Component.literal("[Guild] Accepted: " + spec.title()
				+ " — head to (" + destX + ", " + destZ + ")!").withStyle(ChatFormatting.AQUA));
		} else {
			player.sendSystemMessage(Component.literal("[Guild] Accepted Request: " + spec.title() + "!")
				.withStyle(ChatFormatting.GREEN));
		}
		level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.1F);
		return true;
	}

	public static void abandonJob(ServerPlayer player) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) {
			player.sendSystemMessage(Component.literal("[Guild] You have no active request to abandon.").withStyle(ChatFormatting.GRAY));
			return;
		}
		clearChoreSpots(player);
		player.setAttached(PROFILE, prof.withJob(Optional.empty()));
		player.sendSystemMessage(Component.literal("[Guild] Request abandoned.").withStyle(ChatFormatting.GRAY));
	}

	public static void tryTurnInOrReport(ServerPlayer player, BlockPos boardPos) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) {
			player.sendSystemMessage(Component.literal("[Guild] Pick a request from the board to begin!").withStyle(ChatFormatting.GRAY));
			return;
		}
		ActiveJob job = prof.activeJob().get();
		OathholdQuestRules.JobSpec spec = OathholdQuestRules.byId(job.jobId());
		if (spec != null && spec.category() == OathholdQuestRules.JobCategory.HERB_GATHERING) {
			int have = countRequiredGoods(player, job.targetKey());
			if (have >= job.needed()) {
				consumeRequiredGoods(player, job.targetKey(), job.needed());
				completeJob(player, prof, job);
			} else {
				player.sendSystemMessage(Component.literal("[Guild] Carrying " + have + " / " + job.needed()
					+ " required items for " + spec.title() + ".").withStyle(ChatFormatting.YELLOW));
			}
			return;
		}
		if (job.progress() >= job.needed()) {
			completeJob(player, prof, job);
		} else {
			player.sendSystemMessage(Component.literal("[Guild] Progress: " + job.progress() + " / " + job.needed()
				+ (spec != null ? " (" + spec.title() + ")" : "")).withStyle(ChatFormatting.YELLOW));
		}
	}

	public static int countRequiredGoods(ServerPlayer player, String itemId) {
		Item item = resolveItem(itemId);
		return item == null ? 0 : player.getInventory().countItem(item);
	}

	private static void consumeRequiredGoods(ServerPlayer player, String itemId, int needed) {
		Item item = resolveItem(itemId);
		if (item == null) return;
		var inv = player.getInventory();
		int left = needed;
		for (int s = 0; s < inv.getContainerSize() && left > 0; s++) {
			ItemStack st = inv.getItem(s);
			if (st.is(item)) {
				int take = Math.min(left, st.getCount());
				st.shrink(take);
				left -= take;
			}
		}
		inv.setChanged();
	}

	public static boolean onBlockBroken(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) return false;
		ActiveJob job = prof.activeJob().get();
		if (!OathholdQuestRules.JobCategory.CLEANING.id.equals(job.category())) return false;

		Set<BlockPos> spots = CHORE_SPOTS.get(player.getUUID());
		boolean tracked = spots != null && spots.remove(pos);
		boolean naturalMatch = OathholdLaw.findContainingCity(level, pos).isPresent()
			&& (state.is(Blocks.COBWEB) || state.is(Blocks.MOSS_CARPET));
		if (!tracked && !naturalMatch) {
			return false;
		}
		advanceCleaning(level, player, prof, job, pos);
		return true;
	}

	private static InteractionResult onBlockUse(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) return InteractionResult.PASS;
		ActiveJob job = prof.activeJob().get();

		if (OathholdQuestRules.JobCategory.CLEANING.id.equals(job.category())) {
			Set<BlockPos> spots = CHORE_SPOTS.get(player.getUUID());
			boolean tracked = spots != null && spots.contains(pos);
			if (tracked || (OathholdLaw.findContainingCity(level, pos).isPresent()
					&& (state.is(Blocks.COBWEB) || state.is(Blocks.MOSS_CARPET)))) {
				if (spots != null) spots.remove(pos);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				advanceCleaning(level, player, prof, job, pos);
				return InteractionResult.SUCCESS;
			}
		}

		if (OathholdQuestRules.JobCategory.WORK_SHIFT.id.equals(job.category()) && !player.isShiftKeyDown()) {
			if (matchesWorkStation(state, job.targetKey())) {
				if (tryPerformWorkShift(player, pos)) {
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.PASS;
	}

	/** Executes one work-shift action if the player holds an active {@code WORK_SHIFT} job. */
	public static boolean tryPerformWorkShift(ServerPlayer player, BlockPos stationPos) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) return false;
		ActiveJob job = prof.activeJob().get();
		if (!OathholdQuestRules.JobCategory.WORK_SHIFT.id.equals(job.category())) return false;

		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		long last = LAST_WORK_TICK.getOrDefault(player.getUUID(), -100L);
		if (now - last < 8L) return true;
		LAST_WORK_TICK.put(player.getUUID(), now);

		switch (job.targetKey()) {
			case "anvil", "smithing" -> level.playSound(null, stationPos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.1F);
			case "hearth" -> level.playSound(null, stationPos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
			default -> level.playSound(null, stationPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9F, 1.0F);
		}
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, stationPos.getX() + 0.5, stationPos.getY() + 1.1, stationPos.getZ() + 0.5,
			6, 0.25, 0.25, 0.25, 0.02);

		int next = job.progress() + 1;
		ActiveJob updated = job.withProgress(next);
		if (next >= job.needed()) {
			completeJob(player, prof, updated);
		} else {
			player.setAttached(PROFILE, prof.withJob(Optional.of(updated)));
			player.sendOverlayMessage(Component.literal("[Guild Shift] " + next + " / " + job.needed()).withStyle(ChatFormatting.GOLD));
		}
		return true;
	}

	private static boolean matchesWorkStation(BlockState state, String targetKey) {
		return switch (targetKey) {
			case "anvil" -> state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL);
			case "hearth" -> state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.SMOKER) || state.is(Blocks.BLAST_FURNACE);
			case "lectern" -> state.is(Blocks.LECTERN) || state.is(Blocks.CARTOGRAPHY_TABLE);
			case "smithing" -> state.is(Blocks.SMITHING_TABLE);
			default -> false;
		};
	}

	private static void advanceCleaning(ServerLevel level, ServerPlayer player, AdventurerProfile prof, ActiveJob job, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.9F, 1.25F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
			8, 0.3, 0.25, 0.3, 0.03);
		int next = job.progress() + 1;
		ActiveJob updated = job.withProgress(next);
		if (next >= job.needed()) {
			clearChoreSpots(player);
			completeJob(player, prof, updated);
		} else {
			player.setAttached(PROFILE, prof.withJob(Optional.of(updated)));
			player.sendOverlayMessage(Component.literal("[Guild Chore] Cleaned " + next + " / " + job.needed()).withStyle(ChatFormatting.GREEN));
		}
	}

	public static boolean tryDeliverToCitizen(ServerPlayer player, OathholdCitizen recipient) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) return false;
		ActiveJob job = prof.activeJob().get();
		if (!OathholdQuestRules.JobCategory.CARGO_DELIVERY.id.equals(job.category())) return false;
		double dx = recipient.getX() - job.destX();
		double dz = recipient.getZ() - job.destZ();
		if (dx * dx + dz * dz > 64.0 * 64.0) return false;
		completeJob(player, prof, job.withProgress(job.needed()));
		return true;
	}

	/** Starts a sanctioned Colosseum Arena Bout near {@code anchorPos} when the player holds an {@code ARENA_BOUT} job. */
	public static boolean tryStartArenaBout(ServerPlayer player, ServerLevel level, BlockPos anchorPos) {
		AdventurerProfile prof = profile(player);
		if (prof.activeJob().isEmpty()) return false;
		ActiveJob job = prof.activeJob().get();
		if (!OathholdQuestRules.JobCategory.ARENA_BOUT.id.equals(job.category()) || job.progress() >= job.needed()) {
			return false;
		}
		String cTag = challengerTag(player.getUUID());
		var existing = level.getEntitiesOfClass(Mob.class, new AABB(anchorPos).inflate(96.0),
			m -> m.isAlive() && m.entityTags().contains(OathholdLaw.ARENA_DUELIST_TAG) && m.entityTags().contains(cTag));
		if (!existing.isEmpty()) {
			player.sendSystemMessage(Component.literal("[Colosseum] Defeat your active Arena opponent first!").withStyle(ChatFormatting.YELLOW));
			return true;
		}

		Mob duelist = createArenaDuelist(level, job.targetKey());
		if (duelist == null) return false;
		BlockPos spawnAt = anchorPos;
		duelist.snapTo(spawnAt.getX() + 0.5, spawnAt.getY(), spawnAt.getZ() + 0.5, 0.0F, 0.0F);
		duelist.addTag(OathholdLaw.ARENA_DUELIST_TAG);
		duelist.addTag(cTag);
		duelist.setCustomName(Component.literal("Colosseum Challenger — " + duelist.getType().getDescription().getString())
			.withStyle(ChatFormatting.GOLD));
		duelist.setCustomNameVisible(true);
		duelist.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnAt), EntitySpawnReason.EVENT, null);
		duelist.setTarget(player);
		level.addFreshEntity(duelist);
		level.playSound(null, spawnAt, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.75F, 1.15F);
		player.sendSystemMessage(Component.literal("[Colosseum] Let the sanctioned bout begin! Defeat "
			+ duelist.getType().getDescription().getString() + "!").withStyle(ChatFormatting.GOLD));
		return true;
	}

	private static Mob createArenaDuelist(ServerLevel level, String targetKey) {
		return switch (targetKey) {
			case "fallen_knight" -> {
				FallenKnight fk = AuraWorld.FALLEN_KNIGHT.create(level, EntitySpawnReason.EVENT);
				yield fk;
			}
			case "oathbound_halberdier" -> AuraWorld.OATHBOUND_HALBERDIER.create(level, EntitySpawnReason.EVENT);
			case "oathbound_blade_champion" -> AuraWorld.OATHBOUND_BLADE_CHAMPION.create(level, EntitySpawnReason.EVENT);
			case "oathbound_bastion" -> AuraWorld.OATHBOUND_BASTION.create(level, EntitySpawnReason.EVENT);
			case "oathbound_banner_marshal" -> AuraWorld.OATHBOUND_BANNER_MARSHAL.create(level, EntitySpawnReason.EVENT);
			default -> null;
		};
	}

	private static void onKill(ServerPlayer killer, net.minecraft.world.entity.Entity victim) {
		AdventurerProfile prof = profile(killer);
		if (prof.activeJob().isEmpty()) return;
		ActiveJob job = prof.activeJob().get();

		if (OathholdQuestRules.JobCategory.ARENA_BOUT.id.equals(job.category())) {
			if (victim.entityTags().contains(OathholdLaw.ARENA_DUELIST_TAG)
					&& victim.entityTags().contains(challengerTag(killer.getUUID()))) {
				completeJob(killer, prof, job.withProgress(job.needed()));
			}
			return;
		}

		if (OathholdQuestRules.JobCategory.MONSTER_HUNT.id.equals(job.category())) {
			boolean match = job.targetKey().equals("dungeon_boss")
				? victim instanceof DungeonBoss
				: BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString().equals(job.targetKey())
					|| victim.entityTags().contains(QUEST_PEST_PREFIX + killer.getUUID());
			if (match) {
				int next = job.progress() + 1;
				ActiveJob updated = job.withProgress(next);
				if (next >= job.needed()) {
					completeJob(killer, prof, updated);
				} else {
					killer.setAttached(PROFILE, prof.withJob(Optional.of(updated)));
					killer.sendOverlayMessage(Component.literal("[Guild Hunt] " + next + " / " + job.needed()).withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	public static void completeJob(ServerPlayer player, AdventurerProfile profBefore, ActiveJob job) {
		OathholdQuestRules.Rank prevRank = profBefore.rank();
		int nextPoints = profBefore.rankPoints() + job.rankPoints();
		OathholdQuestRules.Rank nextRank = OathholdQuestRules.Rank.ofPoints(nextPoints);
		player.setAttached(PROFILE, new AdventurerProfile(nextPoints, profBefore.completedCount() + 1, Optional.empty()));

		giveOrDrop(player, new ItemStack(Items.EMERALD, job.emeralds()));
		Item bonus = resolveItem(job.bonusItem());
		if (bonus != null) {
			giveOrDrop(player, new ItemStack(bonus));
		}

		OathholdQuestRules.JobSpec spec = OathholdQuestRules.byId(job.jobId());
		String title = spec == null ? job.jobId() : spec.title();
		player.sendSystemMessage(Component.literal(
			"[Guild] Request Complete: " + title + " — +" + job.emeralds() + " Emeralds, +" + job.rankPoints() + " Guild Pts!")
			.withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1.1F);

		if (nextRank != prevRank) {
			player.sendSystemMessage(Component.literal(
				"[Guild Promotion!] You advanced from Rank " + prevRank.name() + " to Rank " + nextRank.name()
					+ " (" + nextRank.sealMaterial + " Seal)!")
				.withStyle(GuildBoardMenu.rankColor(nextRank), ChatFormatting.BOLD));
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator()) continue;
			AdventurerProfile prof = profile(player);
			if (prof.activeJob().isEmpty()) continue;
			ActiveJob job = prof.activeJob().get();
			double dx = player.getX() - job.destX();
			double dz = player.getZ() - job.destZ();
			if (OathholdQuestRules.JobCategory.ARENA_BOUT.id.equals(job.category()) && dx * dx + dz * dz <= 22.0 * 22.0) {
				tryStartArenaBout(player, (ServerLevel) player.level(), new BlockPos(job.destX(), (int) player.getY(), job.destZ()));
			}
		}
	}

	private static int spawnChoreSpots(ServerLevel level, UUID playerId, BlockPos center, OathholdQuestRules.JobSpec spec) {
		clearChoreSpotsForUuid(level, playerId);
		Set<BlockPos> spots = CHORE_SPOTS.computeIfAbsent(playerId, id -> new HashSet<>());
		BlockState choreState = spec.targetKey().equals("minecraft:moss_carpet")
			? Blocks.MOSS_CARPET.defaultBlockState()
			: Blocks.COBWEB.defaultBlockState();
		RandomSource rng = level.getRandom();
		int target = spec.needed();
		for (int attempt = 0; attempt < 96 && spots.size() < target; attempt++) {
			int rx = center.getX() + rng.nextInt(25) - 12;
			int rz = center.getZ() + rng.nextInt(25) - 12;
			if (Math.abs(rx - center.getX()) <= 2 && Math.abs(rz - center.getZ()) <= 2) continue;
			for (int dy = 2; dy >= -2; dy--) {
				BlockPos p = new BlockPos(rx, center.getY() + dy, rz);
				if (spots.contains(p)) continue;
				if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
					level.setBlock(p, choreState, 3);
					spots.add(p);
					break;
				}
			}
		}
		return spots.size();
	}

	private static void spawnHuntPests(ServerLevel level, UUID playerId, BlockPos center, OathholdQuestRules.JobSpec spec) {
		Identifier id = Identifier.tryParse(spec.targetKey());
		if (id == null) return;
		var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
		if (type.isEmpty()) return;
		RandomSource rng = level.getRandom();
		for (int i = 0; i < spec.needed(); i++) {
			for (int attempt = 0; attempt < 16; attempt++) {
				int rx = center.getX() + rng.nextInt(33) - 16;
				int rz = center.getZ() + rng.nextInt(33) - 16;
				if (Math.abs(rx - center.getX()) < 6 && Math.abs(rz - center.getZ()) < 6) continue;
				BlockPos p = new BlockPos(rx, center.getY(), rz);
				if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
					if (type.get().create(level, EntitySpawnReason.EVENT) instanceof Mob mob) {
						mob.snapTo(rx + 0.5, p.getY(), rz + 0.5, rng.nextFloat() * 360.0F, 0.0F);
						mob.addTag(QUEST_PEST_PREFIX + playerId);
						mob.setCustomName(Component.literal("Guild Subjugation Target"));
						level.addFreshEntity(mob);
						break;
					}
				}
			}
		}
	}

	private static void clearChoreSpots(ServerPlayer player) {
		if (player.level() instanceof ServerLevel level) {
			clearChoreSpotsForUuid(level, player.getUUID());
		}
	}

	private static void clearChoreSpotsForUuid(ServerLevel level, UUID playerId) {
		Set<BlockPos> spots = CHORE_SPOTS.remove(playerId);
		if (spots == null) return;
		for (BlockPos p : spots) {
			BlockState cur = level.getBlockState(p);
			if (cur.is(Blocks.COBWEB) || cur.is(Blocks.MOSS_CARPET)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	private static Item resolveItem(String id) {
		if (id == null || id.isEmpty()) return null;
		Item meal = Meals.MEALS.get(id);
		if (meal != null) return meal;
		return switch (id) {
			case "wildercord:blank_rune" -> WildercordItems.BLANK_RUNE;
			case "wildercord:mana_crystal" -> WildercordItems.MANA_CRYSTAL;
			case "wildercord:aura_shard" -> AuraWorld.AURA_SHARD;
			case "wildercord:moonreed_floss" -> WetlandGarden.FLOSS;
			default -> {
				Identifier key = Identifier.tryParse(id);
				yield key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
			}
		};
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.spawnAtLocation((ServerLevel) player.level(), stack);
		}
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("adventurer")
			.executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				AdventurerProfile prof = profile(p);
				OathholdQuestRules.Rank rank = prof.rank();
				p.sendSystemMessage(Component.literal(
					"=== Adventurer License ===\n"
						+ "Rank: " + rank.name() + " (" + rank.sealMaterial + ") | Guild Points: " + prof.rankPoints()
						+ " | Completed: " + prof.completedCount() + "\n"
						+ "Law Status: " + (OathholdJustice.isImprisoned(p)
							? "IMPRISONED (" + Math.max(1L, (OathholdJustice.remainingSentenceTicks(p) + 19L) / 20L) + "s remaining)"
							: OathholdLaw.isWanted(p) ? "WANTED (" + OathholdLaw.bountyOf(p) + " Emeralds)" : "Good Standing") + "\n"
						+ "Active Request: " + prof.activeJob().map(j -> j.jobId() + " (" + j.progress() + "/" + j.needed() + ")").orElse("None"))
					.withStyle(ChatFormatting.GOLD));
				return 1;
			})
			.then(literal("abandon").executes(ctx -> {
				abandonJob(ctx.getSource().getPlayerOrException());
				return 1;
			}))
			.then(literal("payfine").executes(ctx -> OathholdLaw.tryPayFine(ctx.getSource().getPlayerOrException()) ? 1 : 0))
			.then(literal("release")
				.requires(src -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(src)
					|| (src.getEntity() instanceof ServerPlayer sp && sp.isCreative()))
				.executes(ctx -> {
					OathholdJustice.forceRelease(ctx.getSource().getPlayerOrException());
					return 1;
				}))
		);
	}
}
