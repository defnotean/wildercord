package dev.wildercord.player;

import dev.wildercord.spell.Feats;
import dev.wildercord.spell.MasterStudyRules;
import dev.wildercord.spell.Runes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Lessons survive death and restart in the existing private Grimoire; reading/casting sessions never do. */
public final class MasterStudies {
	private MasterStudies() {}

	public static boolean knowsRelay(Player player) {
		return Heart.discovered(player, MasterStudyRules.RELAY);
	}

	/** A verified copied lesson is retrievable before it is learned; older learned saves remain compatible. */
	public static boolean hasRelayLesson(Player player) {
		return knowsRelay(player) || Heart.discovered(player, MasterStudyRules.RELAY_COPIED);
	}

	public static boolean mayStudyRelay(Player player) {
		return MasterStudyRules.mayStudyRelay(Heart.active(player), Heart.discovered(player, "feat:" + Feats.ARCHIVIST), hasRelayLesson(player));
	}

	public static boolean eligibleRelay(Player player) {
		return MasterStudyRules.eligibleRelay(Heart.active(player), Heart.discovered(player, "feat:" + Feats.ARCHIVIST));
	}

	public static boolean practicedRelay(Player player) {
		return Heart.discovered(player, MasterStudyRules.RELAY_PRACTICE);
	}

	/** Called only by the server's completed, validated Archive reading session. A book item cannot call this. */
	public static boolean learnRelay(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || !mayStudyRelay(player)) return false;
		boolean fresh = remember(player, MasterStudyRules.RELAY);
		Spellbooks.learn(player, Runes.RELAY.id());
		return fresh;
	}

	/** The paid Relay runtime calls this after moving from placement and actually striking a Training Dummy. */
	public static boolean completeRelayPractice(ServerPlayer player) {
		if (!player.isAlive() || !knowsRelay(player) || !eligibleRelay(player)
			|| !remember(player, MasterStudyRules.RELAY_PRACTICE)) return false;
		player.sendSystemMessage(Component.translatable("message.wildercord.relay_lesson.practiced").withColor(0x7FDAD4));
		return true;
	}

	public static boolean knowsReweave(Player player) {
		return Heart.discovered(player, MasterStudyRules.REWEAVE);
	}

	/** Low Tide is the recoverable entitlement, including victories saved before this lesson existed. */
	public static boolean hasReweaveLesson(Player player) {
		return MasterStudyRules.hasReweaveLesson(Heart.discovered(player, "feat:" + Feats.TIDE_SCRIBE),
			Heart.discovered(player, MasterStudyRules.REWEAVE_COPIED), knowsReweave(player));
	}

	public static boolean eligibleReweave(Player player) {
		return MasterStudyRules.eligibleReweave(Heart.active(player), Heart.discovered(player, "feat:" + Feats.TIDE_SCRIBE));
	}

	public static boolean mayStudyReweave(Player player) {
		return MasterStudyRules.mayStudyReweave(Heart.active(player),
			Heart.discovered(player, "feat:" + Feats.TIDE_SCRIBE), hasReweaveLesson(player));
	}

	public static boolean practicedReweave(Player player) {
		return Heart.discovered(player, MasterStudyRules.REWEAVE_PRACTICE);
	}

	/** The paid runtime calls this only after the rewritten lane damages a Dummy outside the old disc. */
	public static boolean completeReweavePractice(ServerPlayer player) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !knowsReweave(player)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player
			|| !eligibleReweave(player) || !remember(player, MasterStudyRules.REWEAVE_PRACTICE)) return false;
		player.sendSystemMessage(Component.translatable("message.wildercord.reweave_lesson.practiced").withColor(0x7FDAD4));
		return true;
	}

	/** Called only after the server validates all three ordered pages of one live reading. */
	public static boolean learnReweave(ServerPlayer player) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !mayStudyReweave(player)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return false;
		boolean fresh = remember(player, MasterStudyRules.REWEAVE);
		Spellbooks.learn(player, Runes.REWEAVE.id());
		return fresh;
	}

	public static boolean knowsExcise(Player player) {
		return Heart.discovered(player, MasterStudyRules.EXCISE);
	}

	/** Heartwood is the recoverable entitlement, including victories saved before this lesson existed. */
	public static boolean hasExciseLesson(Player player) {
		return MasterStudyRules.hasExciseLesson(Heart.discovered(player, "feat:" + Feats.ROOT_GUARDIAN),
			Heart.discovered(player, MasterStudyRules.EXCISE_COPIED), knowsExcise(player));
	}

	public static boolean eligibleExcise(Player player) {
		return MasterStudyRules.eligibleExcise(Heart.active(player), Heart.discovered(player, "feat:" + Feats.ROOT_GUARDIAN));
	}

	public static boolean mayStudyExcise(Player player) {
		return MasterStudyRules.mayStudyExcise(Heart.active(player),
			Heart.discovered(player, "feat:" + Feats.ROOT_GUARDIAN), hasExciseLesson(player));
	}

	public static boolean practicedExcise(Player player) {
		return Heart.discovered(player, MasterStudyRules.EXCISE_PRACTICE);
	}

	/** The paid runtime calls this only after the locked native hostile emitter has been cut. */
	public static boolean completeExcisePractice(ServerPlayer player) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !knowsExcise(player)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player
			|| !eligibleExcise(player) || !remember(player, MasterStudyRules.EXCISE_PRACTICE)) return false;
		player.sendSystemMessage(Component.translatable("message.wildercord.excise_lesson.practiced").withColor(0x7FDAD4));
		return true;
	}

	/** Called only after the server validates all three ordered pages of one live reading. */
	public static boolean learnExcise(ServerPlayer player) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !mayStudyExcise(player)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return false;
		boolean fresh = remember(player, MasterStudyRules.EXCISE);
		Spellbooks.learn(player, Runes.EXCISE.id());
		return fresh;
	}

	public static boolean knows(Player player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		return Heart.discovered(player, lesson.study);
	}

	/** The boss feat is the recoverable entitlement, including victories saved before this pack existed. */
	public static boolean hasLesson(Player player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		return lesson.hasLesson(Heart.discovered(player, "feat:" + lesson.feat), Heart.discovered(player, lesson.copied), knows(player, lesson));
	}

	public static boolean eligible(Player player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		return lesson.eligible(Heart.active(player), Heart.discovered(player, "feat:" + lesson.feat));
	}

	public static boolean mayStudy(Player player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		return lesson.mayStudy(Heart.active(player), Heart.discovered(player, "feat:" + lesson.feat), hasLesson(player, lesson));
	}

	public static boolean practiced(Player player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		return Heart.discovered(player, lesson.practice);
	}

	/** The paid pack runtime calls this after the lesson's first real use: a toll, a reel or an arrival. */
	public static boolean completePractice(ServerPlayer player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !knows(player, lesson)
			|| !eligible(player, lesson) || !remember(player, lesson.practice)) return false;
		player.sendSystemMessage(Component.translatable("message.wildercord." + lesson.path + "_lesson.practiced").withColor(0x7FDAD4));
		return true;
	}

	/** Called only after the server validates all three ordered pages of one live reading. */
	public static boolean learn(ServerPlayer player, dev.wildercord.spell.LessonPackRules.Lesson lesson) {
		if (!player.isAlive() || player.isRemoved() || player.isSpectator() || !mayStudy(player, lesson)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return false;
		boolean fresh = remember(player, lesson.study);
		Spellbooks.learn(player, lesson.id);
		return fresh;
	}

	/** A record only: no condensed mana, affinity, XP or combat reward is generated by lesson bookkeeping. */
	public static boolean remember(ServerPlayer player, String key) {
		List<String> entries = Heart.grimoire(player);
		if (entries.contains(key)) return false;
		List<String> next = new ArrayList<>(entries);
		next.add(key);
		player.setAttached(WildercordAttachments.GRIMOIRE, List.copyOf(next));
		return true;
	}
}
