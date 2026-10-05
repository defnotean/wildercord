package dev.wildercord.party;

import dev.wildercord.cast.Effects;
import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative parties and attributed friendly-harm protection. Membership is deliberately
 * session-only: it survives death, travel and reconnect, and is cleared when that server stops.
 * No client packet, scoreboard write or offline-name-to-UUID conversion grants membership.
 *
 * <p>Party protection covers attributed player harm (including pets and projectiles), while
 * deliberate duels keep their existing rules. Unattributed environmental damage is unaffected.
 * Beneficial spells use the same party relation through Targets.canHelp and are never vetoed.</p>
 */
public final class Parties {
	private Parties() {}

	private static final Map<MinecraftServer, PartySession> SESSIONS = new IdentityHashMap<>();

	/** Register before other damage listeners, so a rejected friendly hit has no combat side effects. */
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> PartyCommands.register(dispatcher));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> !blocksDamage(target, source));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			session(server).remember(handler.player.getUUID(), handler.player.getGameProfile().name()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PartySession state = SESSIONS.get(server);
			if (state != null) state.disconnect(handler.player.getUUID());
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			PartySession state = SESSIONS.get(server);
			if (state != null && server.getTickCount() % 20 == 0) {
				state.prune(now(server), player -> server.getPlayerList().getPlayer(player) != null);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.remove(server));
	}

	static PartySession session(MinecraftServer server) {
		return SESSIONS.computeIfAbsent(server, ignored -> new PartySession());
	}

	static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	/** Immutable UUID membership for encounter opt-in; membership alone must not enroll a boss participant. */
	public static Set<UUID> members(ServerPlayer player) {
		PartySession state = SESSIONS.get(player.level().getServer());
		PartyRules.Party party = state == null ? null : state.rules.party(player.getUUID());
		return party == null ? Set.of() : Set.copyOf(party.members());
	}

	public static UUID leader(ServerPlayer player) {
		PartySession state = SESSIONS.get(player.level().getServer());
		PartyRules.Party party = state == null ? null : state.rules.party(player.getUUID());
		return party == null ? null : party.leader();
	}

	/** Player owners count as party members even when their pets outlive a logout. */
	public static boolean sameParty(Entity first, Entity second) {
		if (first == null || second == null || !(first.level() instanceof ServerLevel a)
				|| !(second.level() instanceof ServerLevel b) || a.getServer() != b.getServer()) return false;
		PartySession state = SESSIONS.get(a.getServer());
		UUID owner = ownerId(first), other = ownerId(second);
		return state != null && owner != null && other != null && state.rules.sameParty(owner, other);
	}

	/** True only for party-attributed harm, never self costs, healing or an unrelated monster/world hazard. */
	public static boolean blocksHarm(Entity attacker, Entity target) {
		// Trial boundaries also apply at status/physics mutation sinks, not only when an effect chooses its target.
		if (attacker != null && attacker != target) {
			if (attacker instanceof dev.wildercord.aura.world.SwordMaster master && !master.canHarmParticipant(target)) return true;
			if (target instanceof dev.wildercord.aura.world.SwordMaster master && !master.acceptsHarmFrom(attacker)) return true;
		}
		if (!sameParty(attacker, target)) return false;
		UUID owner = ownerId(attacker), other = ownerId(target);
		ServerPlayer player = ((ServerLevel) attacker.level()).getServer().getPlayerList().getPlayer(owner);
		Boolean duel = player == null ? null : Duels.canHarm(player, target);
		return SESSIONS.get(((ServerLevel) attacker.level()).getServer()).rules.blocksHarm(owner, other, duel);
	}

	/** Mutation helpers call this at impact, after any invitation acceptance since the spell was cast. */
	public static boolean blocksCurrentHarm(Entity target) {
		return blocksHarm(Effects.applying(), target);
	}

	/** Explicit source ownership wins; an unowned damage source falls back to the current spell context. */
	public static boolean blocksDamage(LivingEntity target, DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker == null) attacker = source.getDirectEntity();
		if (attacker == null) attacker = Effects.applying();
		return blocksHarm(attacker, target);
	}

	/**
	 * Resolve only server entity ownership, never a supplied name. Bounded traversal tolerates
	 * nested summons/projectiles and malformed ownership cycles. Offline owner references are
	 * safe to compare with membership because only connected players can enter the party ledger.
	 */
	private static UUID ownerId(Entity entity) {
		for (int depth = 0; entity != null && depth < 8; depth++) {
			if (entity instanceof Player player) return player.getUUID();
			if (entity instanceof Projectile projectile) {
				Entity owner = projectile.getOwner();
				if (owner == null) {
					var reference = ((dev.wildercord.mixin.ProjectileOwnerAccessor) projectile).wildercord$ownerReference();
					return reference == null ? null : reference.getUUID();
				}
				entity = owner;
			} else if (entity instanceof OwnableEntity ownable) {
				Entity owner = ownable.getOwner();
				if (owner == null) return ownable.getOwnerReference() == null ? null : ownable.getOwnerReference().getUUID();
				entity = owner;
			} else {
				return null;
			}
		}
		return null;
	}
}
