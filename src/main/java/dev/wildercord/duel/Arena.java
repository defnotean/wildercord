package dev.wildercord.duel;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The spell-duel arena (see {@link ArenaRules}). Use an Arena Stone to step up; when a second caster steps up to the same
 * stone, a ranked duel begins round it on the duel's own rules. Sneak-use the stone to read the season's ladder.
 */
public final class Arena {
	private Arena() {}

	/** Who stands waiting at each stone, and since when (game time). */
	private record Waiting(UUID player, long since) {}

	private static final Map<ResourceKey<Level>, Map<BlockPos, Waiting>> WAITING = new HashMap<>();

	/** The terms a ranked bout is fought on: a duel's, round the stone, three minutes, not into the duel record. */
	public static final DuelRules.Terms TERMS = new DuelRules.Terms(ArenaRules.RADIUS, DuelRules.COUNTDOWN_TICKS, ArenaRules.BOUT_TICKS, 0F, 1.0F, false);

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(WildercordBlocks.ARENA_STONE));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WAITING.clear());
	}

	/** The season now. */
	public static int season(ServerLevel level) {
		return ArenaRules.season(level.getServer().overworld().getGameTime());
	}

	/** A caster's standing this season. */
	public static ArenaLadder.Standing standing(ServerPlayer player) {
		ServerLevel level = player.level();
		return ArenaLadder.of(level.getServer()).standing(player.getUUID(), player.getGameProfile().name(), season(level));
	}

	/** {@code player} used the stone at {@code pos}: step up, or (sneaking) read the ladder. */
	public static void use(ServerLevel level, BlockPos pos, ServerPlayer player) {
		if (player.isShiftKeyDown()) {
			showLadder(level, player);
			return;
		}
		if (Duels.inDuel(player)) return;
		DuelRules.Refusal refusal = Duels.readiness(player);
		if (refusal != DuelRules.Refusal.NONE) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.duel_" + refusal.name().toLowerCase(java.util.Locale.ROOT), player.getDisplayName())
				.withStyle(ChatFormatting.RED));
			return;
		}
		Map<BlockPos, Waiting> here = WAITING.computeIfAbsent(level.dimension(), k -> new HashMap<>());
		long now = level.getGameTime();
		Waiting waiting = here.get(pos);
		ServerPlayer other = waiting == null ? null : level.getServer().getPlayerList().getPlayer(waiting.player());
		if (other == null && waiting != null && level.getPlayerByUUID(waiting.player()) instanceof ServerPlayer there) other = there;
		boolean stillThere = other != null && other != player && other.level() == level && !Duels.inDuel(other)
			&& now - waiting.since() <= ArenaRules.WAIT_TICKS && other.position().distanceTo(Vec3.atCenterOf(pos)) <= ArenaRules.STEP_UP + 2;
		if (!stillThere) {
			here.put(pos.immutable(), new Waiting(player.getUUID(), now));
			player.sendOverlayMessage(Component.translatable("message.wildercord.arena.waiting").withColor(0xB8A8FF));
			level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.7F, 1.4F);
			return;
		}
		here.remove(pos);
		begin(level, pos, other, player);
	}

	/** Starts a ranked bout between {@code a} and {@code b} round the stone at {@code pos}. */
	public static DuelRules.Duel begin(ServerLevel level, BlockPos pos, ServerPlayer a, ServerPlayer b) {
		Vec3 centre = Vec3.atBottomCenterOf(pos.above());
		Bout bout = new Bout(level, centre, a, b);
		bout.duel = Duels.startBout(a, b, TERMS, centre, bout);
		ArenaLadder.Standing sa = standing(a), sb = standing(b);
		for (ServerPlayer p : List.of(a, b)) {
			ServerPlayer o = p == a ? b : a;
			ArenaLadder.Standing so = p == a ? sb : sa;
			p.sendSystemMessage(Component.translatable("message.wildercord.arena.begins", o.getDisplayName(),
				Component.translatable("arena.wildercord.rank." + ArenaRules.rank(so.rating()).id()).withColor(ArenaRules.rank(so.rating()).color), so.rating())
				.withColor(0xFFE8C46A));
		}
		level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.8F);
		return bout.duel;
	}

	/** The season's best, and where the reader stands. */
	public static void showLadder(ServerLevel level, ServerPlayer player) {
		int season = season(level);
		List<ArenaLadder.Standing> top = ArenaLadder.of(level.getServer()).top(season, ArenaRules.LADDER_SHOWN);
		player.sendSystemMessage(Component.translatable("message.wildercord.arena.ladder", season + 1).withColor(0xFFE8C46A));
		if (top.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.arena.ladder.empty").withStyle(ChatFormatting.GRAY));
		}
		for (int i = 0; i < top.size(); i++) {
			ArenaLadder.Standing s = top.get(i);
			player.sendSystemMessage(line(i + 1 + ". ", s));
		}
		player.sendSystemMessage(line("", standing(player)).withStyle(ChatFormatting.ITALIC));
	}

	private static net.minecraft.network.chat.MutableComponent line(String prefix, ArenaLadder.Standing s) {
		ArenaRules.Rank rank = ArenaRules.rank(s.rating());
		return Component.literal(prefix).withStyle(ChatFormatting.GRAY).append(Component.translatable("message.wildercord.arena.ladder.line", s.name(),
			Component.translatable("arena.wildercord.rank." + rank.id()).withColor(rank.color), s.rating(), s.wins(), s.losses()).withStyle(ChatFormatting.WHITE));
	}

	/**
	 * Writes a bout's outcome to the ladder: the winner up and the loser down by Elo, or both by an even score. Returns
	 * each one's change (a's first). Public for the game tests.
	 */
	public static int[] record(ServerLevel level, UUID a, String nameA, UUID b, String nameB, double scoreA) {
		ArenaLadder ladder = ArenaLadder.of(level.getServer());
		int season = season(level);
		ArenaLadder.Standing sa = ladder.standing(a, nameA, season), sb = ladder.standing(b, nameB, season);
		int da = ArenaRules.change(sa.rating(), sb.rating(), scoreA);
		int db = ArenaRules.change(sb.rating(), sa.rating(), 1 - scoreA);
		ladder.put(a, new ArenaLadder.Standing(nameA, Math.max(0, sa.rating() + da), sa.wins() + (scoreA > 0.5 ? 1 : 0), sa.losses() + (scoreA < 0.5 ? 1 : 0), season));
		ladder.put(b, new ArenaLadder.Standing(nameB, Math.max(0, sb.rating() + db), sb.wins() + (scoreA < 0.5 ? 1 : 0), sb.losses() + (scoreA > 0.5 ? 1 : 0), season));
		return new int[] {da, db};
	}

	/** One ranked bout, shown round its stone. */
	static final class Bout implements Duels.Watcher {
		final ServerLevel level;
		final Vec3 centre;
		final UUID a, b;
		final String nameA, nameB;
		DuelRules.Duel duel;

		Bout(ServerLevel level, Vec3 centre, ServerPlayer a, ServerPlayer b) {
			this.level = level;
			this.centre = centre;
			this.a = a.getUUID();
			this.b = b.getUUID();
			this.nameA = a.getGameProfile().name();
			this.nameB = b.getGameProfile().name();
		}

		private void both(java.util.function.Consumer<ServerPlayer> each) {
			for (UUID id : List.of(a, b)) {
				if (level.getPlayerByUUID(id) instanceof ServerPlayer p) each.accept(p);
			}
		}

		@Override
		public void counting(int second) {
			both(p -> p.sendOverlayMessage(Component.literal(String.valueOf(second)).withColor(0xFFE8C46A)));
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 0.6F + 0.2F * (3 - second));
			ring(32);
		}

		@Override
		public void began() {
			both(p -> p.sendOverlayMessage(Component.translatable("message.wildercord.arena.fight").withColor(0xFFFF6A4A)));
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
		}

		@Override
		public void tick(long now) {
			if (now % 20 == 0) ring(48);
		}

		/** The bounds: a ring of enchanting glyphs at the arena's edge. */
		private void ring(int points) {
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				level.sendParticles(ParticleTypes.ENCHANT, centre.x + Math.cos(angle) * ArenaRules.RADIUS, centre.y + 0.2,
					centre.z + Math.sin(angle) * ArenaRules.RADIUS, 1, 0, 0.1, 0, 0);
			}
		}

		@Override
		public void ended(DuelRules.Duel duel, ServerPlayer winner, ServerPlayer loser) {
			DuelRules.Ending ending = duel.ending();
			if (ending == null || ending == DuelRules.Ending.INTERRUPTED) {
				both(p -> p.sendSystemMessage(Component.translatable("message.wildercord.arena.called_off").withStyle(ChatFormatting.GRAY)));
				return;
			}
			double scoreA = duel.winner() == null ? 0.5 : duel.winner().equals(a) ? 1.0 : 0.0;
			int[] change = record(level, a, nameA, b, nameB, scoreA);
			both(p -> {
				boolean isA = p.getUUID().equals(a);
				int delta = isA ? change[0] : change[1];
				ArenaLadder.Standing now = standing(p);
				ArenaRules.Rank rank = ArenaRules.rank(now.rating());
				String key = duel.winner() == null ? "message.wildercord.arena.draw" : duel.winner().equals(p.getUUID()) ? "message.wildercord.arena.won" : "message.wildercord.arena.lost";
				p.sendSystemMessage(Component.translatable(key, (delta >= 0 ? "+" : "") + delta, now.rating(),
					Component.translatable("arena.wildercord.rank." + rank.id()).withColor(rank.color)).withColor(0xFFE8C46A));
			});
			if (winner != null) {
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, winner.getX(), winner.getY() + 1, winner.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
				level.playSound(null, winner.getX(), winner.getY(), winner.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
			}
		}
	}
}
