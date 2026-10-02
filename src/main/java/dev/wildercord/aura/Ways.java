package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.config.Config;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * Ways at runtime (the rules and numbers are {@link WayRules}): which Way a swordsman walks, kept on them, saved, kept through death
 * and synced to everyone near ({@link #WAY}), whether a node of it is in force ({@link #has}), and choosing, unbinding and settling.
 * Choosing happens at the crossroads ({@link Crossroads}); unbinding with a Crossroads Incense ({@link CrossroadsIncense}). What
 * each node does lives where the technique it changes does, asking {@link #has}: the Blade's, the Bulwark's and the Shadowstep's in
 * {@link WayEffects}, the Banner's in {@link WayBanner}.
 */
public final class Ways {
	private Ways() {}

	// ------------------------------------------------------------------ the state

	/**
	 * The Way a swordsman walks.
	 *
	 * @param way     the Way's id, or "" while they walk none
	 * @param since   when they chose it (game time; -1 never)
	 * @param owed    experience still owed before the Way's later nodes wake, after a change of Way (0 once settled)
	 * @param settle  what was owed when they chose it (0 for a first choice), so how far it has come reads the same whatever the
	 *                server's setting is now
	 * @param former  the Way they walked before their last change ("" for none)
	 * @param changes how many times they've changed Way
	 */
	public record State(String way, long since, float owed, float settle, String former, int changes) {
		public static final State NONE = new State("", -1, 0, 0, "", 0);

		public State {
			way = way == null ? "" : way;
			former = former == null ? "" : former;
			owed = Math.max(0, owed);
			settle = Math.max(owed, settle);
			changes = Math.max(0, changes);
		}

		public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("way", "").forGetter(State::way),
			Codec.LONG.optionalFieldOf("since", -1L).forGetter(State::since),
			Codec.FLOAT.optionalFieldOf("owed", 0.0F).forGetter(State::owed),
			Codec.FLOAT.optionalFieldOf("settle", 0.0F).forGetter(State::settle),
			Codec.STRING.optionalFieldOf("former", "").forGetter(State::former),
			Codec.INT.optionalFieldOf("changes", 0).forGetter(State::changes)
		).apply(i, State::new));

		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), State::way, ByteBufCodecs.VAR_LONG, State::since, ByteBufCodecs.FLOAT, State::owed,
			ByteBufCodecs.FLOAT, State::settle, ByteBufCodecs.stringUtf8(128), State::former, ByteBufCodecs.VAR_INT, State::changes, State::new);

		/** Whether a Way is walked at all. */
		public boolean walking() {
			return !way.isEmpty();
		}

		/** Where a node at {@code nodeStage} of {@code nodeWay} stands for a swordsman at {@code stage}. */
		public WayRules.NodeState node(String nodeWay, int stage, int nodeStage) {
			return WayRules.state(way, nodeWay, stage, nodeStage, owed, settle);
		}

		State settled(double xp) {
			return new State(way, since, (float) WayRules.settle(owed, xp), settle, former, changes);
		}
	}

	/** Each swordsman's Way: saved, kept through death (the path itself), synced to everyone near (an ally's client can see a Banner). */
	public static final AttachmentType<State> WAY = AttachmentRegistry.create(
		Wildercord.id("aura_way"),
		builder -> builder
			.initializer(() -> State.NONE)
			.persistent(State.CODEC)
			.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.all())
			.copyOnDeath()
	);

	// ------------------------------------------------------------------ reading (both sides)

	public static State state(Player player) {
		return player.getAttachedOrElse(WAY, State.NONE);
	}

	/** Whether Ways work for {@code player}: on on this server (or the one this client is on) and aura working. */
	public static boolean on(Player player) {
		return player != null && Config.ways(player) && Aura.enabled(player);
	}

	/** The Way {@code player} walks, if any (and registered: an add-on's Way gone walks none). */
	public static Optional<AuraApi.Way> way(Player player) {
		State s = state(player);
		return s.walking() ? AuraApi.way(s.way()) : Optional.empty();
	}

	/** Whether {@code player} has reached the crossroads (Edge or above) and walks no Way yet. */
	public static boolean wayless(Player player) {
		return on(player) && Aura.stage(player) >= WayRules.FROM && way(player).isEmpty();
	}

	/**
	 * Whether node {@code nodeId} is in force for {@code player}: Ways on, its Way walked, its stage reached, and awake (settled after a
	 * change of Way). Both sides read it from what's synced; the effects ask it on the server.
	 */
	public static boolean has(Player player, String nodeId) {
		if (player == null || !on(player)) {
			return false;
		}
		State s = state(player);
		if (!s.walking()) {
			return false;
		}
		AuraApi.Way way = AuraApi.way(s.way()).orElse(null);
		if (way == null) {
			return false;
		}
		int stage = Aura.stage(player);
		for (AuraApi.WayNode node : way.nodes()) {
			if (node.id().equals(nodeId)) {
				return stage >= node.stage() && WayRules.awake(node.stage(), s.owed(), s.settle());
			}
		}
		return false;
	}

	/** Where {@code node} of {@code way} stands for {@code player} (the Aura page's tree). */
	public static WayRules.NodeState nodeState(Player player, AuraApi.Way way, AuraApi.WayNode node) {
		return state(player).node(way.id(), Aura.stage(player), node.stage());
	}

	// ------------------------------------------------------------------ choosing and unbinding (server)

	private static void write(ServerPlayer player, State state) {
		if (!state.equals(state(player))) {
			player.setAttached(WAY, state);
		}
	}

	/**
	 * Sets {@code player} on the Way {@code wayId} (the crossroads' choice, or an add-on's own rite through {@link AuraApi#chooseWay}):
	 * a first choice owes nothing; a choice after an unbinding owes the server's {@code way_settle_xp} before the later nodes wake.
	 * Returns whether it was made (a registered Way, Edge reached, none walked now).
	 */
	public static boolean choose(ServerPlayer player, String wayId) {
		AuraApi.Way way = AuraApi.way(wayId).orElse(null);
		State s = state(player);
		if (way == null || s.walking() || Aura.stage(player) < WayRules.FROM || !on(player)) {
			return false;
		}
		boolean changed = s.changes() > 0 || !s.former().isEmpty();
		float owed = WayRules.settles(changed) ? (float) Math.max(0, Config.get().aura().ways().settleXp()) : 0;
		write(player, new State(way.id(), player.level().getGameTime(), owed, owed, s.former(), s.changes()));
		Grimoire.unlock(player, "aura:way");
		Grimoire.unlock(player, "aura:way_" + way.id().replace(':', '.'));
		for (AuraApi.WayHook hook : AuraApi.wayHooks()) {
			try {
				hook.chosen(player, way, !changed);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A Way hook threw; skipping it", e);
			}
		}
		return true;
	}

	/** Takes {@code player}'s Way from them (a Crossroads Incense, or {@link AuraApi#unbindWay}): its nodes go dark. Returns the Way left. */
	public static Optional<AuraApi.Way> unbind(ServerPlayer player) {
		State s = state(player);
		if (!s.walking()) {
			return Optional.empty();
		}
		Optional<AuraApi.Way> left = AuraApi.way(s.way());
		write(player, new State("", -1, 0, 0, s.way(), s.changes() + 1));
		left.ifPresent(way -> {
			for (AuraApi.WayHook hook : AuraApi.wayHooks()) {
				try {
					hook.unbound(player, way);
				} catch (RuntimeException e) {
					Wildercord.LOGGER.warn("A Way hook threw; skipping it", e);
				}
			}
		});
		return left;
	}

	/** Sets a Way outright, owing nothing (an operator's command and the game tests); "" clears it as if never chosen. */
	public static void set(ServerPlayer player, String wayId) {
		if (wayId == null || wayId.isEmpty()) {
			player.removeAttached(WAY);
			return;
		}
		State s = state(player);
		write(player, new State(wayId, player.level().getGameTime(), 0, 0, s.former(), s.changes()));
	}

	/**
	 * Experience {@code player} earned (before the stage's cap: a swordsman waiting at a threshold still settles), from
	 * {@code AuraExperience}: a new Way settles by it.
	 */
	static void earned(ServerPlayer player, double xp) {
		State s = state(player);
		if (!s.walking() || s.owed() <= 0 || xp <= 0) {
			return;
		}
		AuraApi.Way way = AuraApi.way(s.way()).orElse(null);
		int stage = Aura.stage(player);
		State next = s.settled(xp);
		write(player, next);
		if (way == null) {
			return;
		}
		// A node waking: said once, in the Way's colour.
		for (AuraApi.WayNode node : way.nodes()) {
			boolean was = WayRules.awake(node.stage(), s.owed(), s.settle());
			boolean now = WayRules.awake(node.stage(), next.owed(), next.settle());
			if (!was && now) {
				Component name = Component.translatable(node.nameKey()).withColor(0xFF000000 | way.color());
				player.sendSystemMessage(Component.translatable(stage >= node.stage() ? "message.wildercord.aura.way.woke" : "message.wildercord.aura.way.woke_later",
					name, Component.translatable("aura.wildercord.stage." + AuraStages.id(node.stage()))).withColor(0xE8D8B0));
				Aura.sound(player, "aura_way_lean", 0.7F, 1.2F);
			}
		}
	}

	// ------------------------------------------------------------------ the built-in Ways

	/** The four built-in Ways, as registered (both sides, at start). */
	public static List<AuraApi.Way> builtIn() {
		return List.of(
			builtIn(WayRules.BLADE, WayRules.BLADE_COLOR),
			builtIn(WayRules.BULWARK, WayRules.BULWARK_COLOR),
			builtIn(WayRules.SHADOWSTEP, WayRules.SHADOWSTEP_COLOR),
			builtIn(WayRules.BANNER, WayRules.BANNER_COLOR));
	}

	private static AuraApi.Way builtIn(String id, int color) {
		return new AuraApi.Way(id, color, List.of(
			new AuraApi.WayNode(WayRules.node(id, AuraRules.EDGE), AuraRules.EDGE),
			new AuraApi.WayNode(WayRules.node(id, AuraRules.FORM), AuraRules.FORM),
			new AuraApi.WayNode(WayRules.node(id, AuraRules.SOVEREIGN), AuraRules.SOVEREIGN)));
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		for (AuraApi.Way way : builtIn()) {
			AuraApi.registerWay(way);
		}
		Crossroads.init();
		CrossroadsIncense.init();
		WayEffects.init();
		WayBanner.init();
		// A swordsman already past the crossroads with no Way (Ways came in after they got there, or they unbound theirs) is told how
		// to call it, a few seconds after joining.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			dev.wildercord.cast.Scheduler.later(100, () -> {
				if (player.isAlive() && !player.hasDisconnected() && wayless(player)) {
					player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.waiting").withColor(0xE8D8B0));
					player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.waiting_how").withColor(0xB8A8D8));
				}
			});
		});
	}
}
