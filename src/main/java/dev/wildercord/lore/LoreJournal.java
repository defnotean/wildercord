package dev.wildercord.lore;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraExperience;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.FormDashRules;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.Battlefields;
import dev.wildercord.aura.world.Duelist;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.SleepingBlades;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.aura.world.SwordTombs;
import dev.wildercord.aura.world.TrainingGrounds;
import dev.wildercord.aura.world.TrainingRules;
import dev.wildercord.aura.world.VillageTournaments;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The lore journal on the server: the per-player record, the discovery quests, and the short conversation
 * lines of duelists and Sword Masters. It only watches: every hook returns PASS and changes no fight.
 */
public final class LoreJournal {
	private LoreJournal() {}

	public static final AttachmentType<LoreJournalData> JOURNAL = AttachmentRegistry.create(Wildercord.id("lore_journal"),
		builder -> builder.initializer(() -> LoreJournalData.EMPTY).persistent(LoreJournalData.CODEC)
			.syncWith(LoreJournalData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()).copyOnDeath());

	/** How often each player's journal is brought up to date, in ticks. */
	public static final int POLL_TICKS = 40;
	/** The gap between a teacher's repeated lines to one player, in ticks. */
	public static final int TALK_COOLDOWN = 200;
	private static final int TITLE = 0xFFD7B56D, CLUE = 0xFFC8BFA8, HINT = 0xFF9FB8B0, SPEECH = 0xFFE8DCC0;

	private static final Map<UUID, Long> LAST_TALK = new HashMap<>();
	private static final Map<UUID, Integer> TALKS = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LoreJournal::tick);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer server && !player.isSpectator()) {
				if (entity instanceof Duelist duelist && !duelist.inDuel() && !duelist.leaving()) talk(server, duelist.method().id());
			}
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer server && !player.isSpectator()) {
				Block block = level.getBlockState(hit.getBlockPos()).getBlock();
				String place = block == Battlefields.MEMORIAL ? "place:memorial" : block == SwordTombs.RELIQUARY ? "place:tomb"
					: block == VillageTournaments.BOARD ? "place:tournament" : block == SleepingBlades.STONE ? "place:sleeping_blade" : null;
				if (place != null) record(server, place, true);
			}
			return InteractionResult.PASS;
		});
	}

	public static LoreJournalData data(net.minecraft.world.entity.player.Player player) {
		return player.getAttachedOrElse(JOURNAL, LoreJournalData.EMPTY);
	}

	private static void set(ServerPlayer player, LoreJournalData data) {
		player.setAttached(JOURNAL, data);
	}

	/** Writes an entry. A new one announced brings the journal chime and the key reminder. Returns whether it was new. */
	public static boolean record(ServerPlayer player, String entry, boolean announce) {
		LoreJournalData before = data(player);
		LoreJournalData after = before.with(entry);
		if (after == before) return false;
		set(player, after);
		if (announce) notifyUpdated(player);
		return true;
	}

	private static void notifyUpdated(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("journal.wildercord.updated", Component.keybind("key.wildercord.lore_journal")).withColor(TITLE));
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------ conversation

	private static void say(ServerPlayer player, String role, String method, String kind) {
		String line = LoreEntries.line(role, method, kind);
		Component speaker = Component.translatable("dialogue.wildercord.speaker." + role, methodName(method));
		player.sendSystemMessage(Component.translatable("dialogue.wildercord.says", speaker, Component.translatable(LoreEntries.lineKey(line))).withColor(SPEECH));
		set(player, data(player).with("said:" + line));
	}

	public static Component methodName(String method) {
		String clean = LoreEntries.clean(method);
		String plain = clean.substring(clean.lastIndexOf('.') + 1);
		return Component.translatableWithFallback("aura.wildercord.method." + clean,
			plain.isEmpty() ? plain : Character.toUpperCase(plain.charAt(0)) + plain.substring(1));
	}

	/** A duelist spoken to: a greeting the first time, then (not too often) its hint and the journal's open lead. */
	static void talk(ServerPlayer player, String method) {
		String clean = LoreEntries.clean(method);
		long now = player.level().getGameTime();
		if (!data(player).has("duelist:" + clean)) {
			say(player, "duelist", method, "greet");
			record(player, "duelist:" + clean, true);
			LAST_TALK.put(player.getUUID(), now);
			poll(player);
			return;
		}
		Long last = LAST_TALK.get(player.getUUID());
		if (last != null && now - last < TALK_COOLDOWN && now >= last) return;
		if (LAST_TALK.size() > 512) LAST_TALK.clear();
		LAST_TALK.put(player.getUUID(), now);
		int count = TALKS.merge(player.getUUID(), 1, Integer::sum);
		if (TALKS.size() > 512) TALKS.clear();
		LoreQuestRules.Quest open = LoreQuestRules.firstActive(data(player).quests());
		if (open != null && count % 2 == 0) {
			player.sendSystemMessage(Component.translatable("journal.wildercord.lead", Component.translatable(open.key("title")),
				Component.translatable(open.key("hint"))).withColor(HINT));
		} else {
			say(player, "duelist", method, "hint");
		}
	}

	/** Called once a duel is won and taught: the duelist's parting line. */
	public static void duelWon(ServerPlayer player, String method) {
		say(player, "duelist", method, "victory");
		record(player, "won:" + LoreEntries.clean(method), true);
	}

	/** Called on a Sword Master's first clear: the Master's parting line and the ledger entry. */
	public static void masterWon(ServerPlayer player, int school) {
		String method = schoolMethod(school);
		say(player, "master", method, "victory");
		record(player, "victory:" + method, true);
	}

	public static String schoolMethod(int school) {
		return switch (school) {
			case MastersRules.EMBER -> "ember";
			case MastersRules.GALE -> "gale";
			case MastersRules.STONE -> "stone";
			default -> "school_" + school;
		};
	}

	// ------------------------------------------------------------------ the poll

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			if ((player.tickCount + (player.getId() & 31)) % POLL_TICKS == 0 && player.isAlive()) poll(player);
		}
	}

	/** Brings the journal up to date from what the player already has, then steps the quests. Safe to call any time. */
	public static void poll(ServerPlayer player) {
		LoreJournalData before = data(player);
		LoreJournalData data = before;
		List<String> grimoire = Heart.grimoire(player);
		for (String key : grimoire) {
			String entry = LoreEntries.fromGrimoire(key);
			if (entry != null) data = data.with(entry);
		}
		AuraAttachments.Data aura = Aura.data(player);
		if (aura.learned()) {
			data = data.with("method:" + LoreEntries.clean(aura.method()));
			for (int stage = AuraRules.GLOW; stage <= Math.min(aura.stage(), AuraRules.SOVEREIGN); stage++) data = data.with("stage:" + AuraStages.id(stage));
		}
		if (MasterForms.data(player).knows(MasterForms.WALL_TURN)) data = data.with("form:wall_turn");
		if (MasterForms.data(player).knows(MasterForms.STONE_HINGE)) data = data.with("form:stone_hinge");
		if (FormDash.data(player).knows(FormDashRules.CINDER_LUNGE)) data = data.with("form:cinder_lunge");
		if (FormDash.data(player).knows(FormDashRules.REED_SLIP)) data = data.with("form:reed_slip");
		int schools = MasterVictories.progress(player).schools();
		for (int school = 0; school < 31; school++) if ((schools & (1 << school)) != 0) data = data.with("victory:" + schoolMethod(school));
		boolean training = Aura.state(player).breathing() && TrainingGrounds.ground(player) != TrainingRules.Ground.NONE;
		if (training) data = data.with("place:training");
		if (data != before) set(player, data);
		meetMasters(player);
		data = data(player);

		LoreQuestRules.Advance advance = LoreQuestRules.advance(data.quests(), LoreQuestRules.facts(grimoire, data.entries(), training));
		if (!advance.changed()) return;
		set(player, data.withQuests(advance.quests()));
		for (LoreQuestRules.Quest quest : advance.clued()) {
			player.sendSystemMessage(Component.translatable("journal.wildercord.clue", Component.translatable(quest.key("title"))).withColor(TITLE)
				.withStyle(ChatFormatting.BOLD));
			player.sendSystemMessage(Component.translatable(quest.key("clue")).withColor(CLUE));
			player.sendSystemMessage(Component.translatable("journal.wildercord.objective", Component.translatable(quest.key("objective"))).withColor(HINT));
		}
		for (LoreQuestRules.Quest quest : advance.completed()) reward(player, quest);
		notifyUpdated(player);
	}

	/** A Sword Master close by and facing this player: its greeting, once per school. */
	private static void meetMasters(ServerPlayer player) {
		for (SwordMaster master : player.level().getEntitiesOfClass(SwordMaster.class, player.getBoundingBox().inflate(20), SwordMaster::isAlive)) {
			String method = LoreEntries.clean(master.method().id());
			if (data(player).has("master:" + method)) continue;
			say(player, "master", method, "greet");
			record(player, "master:" + method, true);
		}
	}

	private static void reward(ServerPlayer player, LoreQuestRules.Quest quest) {
		player.sendSystemMessage(Component.translatable("journal.wildercord.complete", Component.translatable(quest.key("title"))).withColor(TITLE)
			.withStyle(ChatFormatting.BOLD));
		player.sendSystemMessage(Component.translatable(quest.key("done")).withColor(CLUE));
		if (quest.xp() > 0) player.giveExperiencePoints(quest.xp());
		if (quest.shards() > 0) give(player, new ItemStack(AuraWorld.AURA_SHARD, quest.shards()));
		if (quest.runes() > 0) give(player, new ItemStack(WildercordItems.BLANK_RUNE, quest.runes()));
		double aura = quest.auraXp() > 0 ? AuraExperience.grant(player, quest.auraXp()) : 0;
		net.minecraft.network.chat.MutableComponent paid = Component.empty();
		int parts = 0;
		for (Component part : new Component[] {
			quest.xp() > 0 ? Component.translatable("journal.wildercord.reward.xp", quest.xp()) : null,
			quest.shards() > 0 ? Component.translatable("journal.wildercord.reward.shards", quest.shards()) : null,
			quest.runes() > 0 ? Component.translatable("journal.wildercord.reward.runes", quest.runes()) : null,
			aura > 0 ? Component.translatable("journal.wildercord.reward.aura", Math.round(aura)) : null}) {
			if (part == null) continue;
			if (parts++ > 0) paid.append(", ");
			paid.append(part);
		}
		player.sendSystemMessage(Component.translatable("journal.wildercord.reward", paid).withColor(HINT));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.3F);
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !stack.isEmpty()) {
			player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
		}
	}
}
