package dev.wildercord.party;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned, Minecraft-free party membership. An invitation never joins its recipient: only
 * that UUID accepting a live invitation can do so. A new party has a fresh identity, so an old
 * invitation cannot be replayed after its leader disbands or leaves.
 */
public final class PartyRules {
	public static final int MAX_MEMBERS = 8;
	public static final int INVITE_TICKS = 20 * 60;
	public static final int INVITE_COOLDOWN_TICKS = 20;
	public static final int MAX_PENDING_INVITES = 8;

	public enum Result {
		OK, SELF, NO_PARTY, NOT_LEADER, ALREADY_GROUPED, NO_INVITE, FULL, INVITE_LIMIT, WAIT, NOT_MEMBER
	}

	/** Immutable snapshot; member order is join order, used for deterministic leadership succession. */
	public record Party(UUID id, UUID leader, List<UUID> members) {
		public Party {
			members = List.copyOf(members);
		}
	}

	private record Invite(UUID party, UUID from, UUID to, long created) {
		boolean expired(long now) {
			return now < created || now - created >= INVITE_TICKS;
		}
	}

	private static final class Group {
		final UUID id = UUID.randomUUID();
		UUID leader;
		final Set<UUID> members = new LinkedHashSet<>();

		Group(UUID leader) {
			this.leader = leader;
			members.add(leader);
		}

		Party snapshot() {
			return new Party(id, leader, List.copyOf(members));
		}
	}

	private final Map<UUID, Group> byMember = new HashMap<>();
	private final Map<UUID, Map<UUID, Invite>> invitations = new HashMap<>();
	private final Map<UUID, Long> lastInvite = new HashMap<>();

	public Party party(UUID member) {
		Group group = byMember.get(member);
		return group == null ? null : group.snapshot();
	}

	public boolean sameParty(UUID first, UUID second) {
		Group group = byMember.get(first);
		return group != null && group == byMember.get(second);
	}

	/** Resolved and offline projectile owners use the same server-recorded UUID membership. */
	public boolean blocksHarm(UUID owner, UUID targetOwner, Boolean duel) {
		return owner != null && targetOwner != null
			&& blocksHarm(owner.equals(targetOwner), sameParty(owner, targetOwner), duel);
	}

	/** Whether party protection vetoes harm; an explicit duel decision takes precedence. */
	public static boolean blocksHarm(boolean sameOwner, boolean sameParty, Boolean duel) {
		return !sameOwner && sameParty && !Boolean.TRUE.equals(duel);
	}

	public Result invite(UUID from, UUID to, long now) {
		prune(now);
		if (from.equals(to)) return Result.SELF;
		Group group = byMember.get(from);
		if (group != null && !group.leader.equals(from)) return Result.NOT_LEADER;
		if (byMember.containsKey(to)) return Result.ALREADY_GROUPED;
		if (group != null && group.members.size() >= MAX_MEMBERS) return Result.FULL;
		Long last = lastInvite.get(from);
		if (last != null && now >= last && now - last < INVITE_COOLDOWN_TICKS) return Result.WAIT;
		Map<UUID, Invite> received = invitations.get(to);
		long sent = invitations.values().stream().filter(map -> map.containsKey(from)).count();
		if ((received != null && received.size() >= MAX_PENDING_INVITES && !received.containsKey(from))
				|| sent >= MAX_PENDING_INVITES && (received == null || !received.containsKey(from))) return Result.INVITE_LIMIT;
		if (group == null) {
			group = new Group(from);
			byMember.put(from, group);
		}
		invitations.computeIfAbsent(to, ignored -> new LinkedHashMap<>()).put(from, new Invite(group.id, from, to, now));
		lastInvite.put(from, now);
		return Result.OK;
	}

	public Result accept(UUID recipient, UUID inviter, long now) {
		prune(now);
		if (byMember.containsKey(recipient)) return Result.ALREADY_GROUPED;
		Invite invite = invitation(recipient, inviter);
		Group group = byMember.get(inviter);
		if (invite == null || group == null || !group.id.equals(invite.party) || !group.leader.equals(inviter)) return Result.NO_INVITE;
		if (group.members.size() >= MAX_MEMBERS) return Result.FULL;
		group.members.add(recipient);
		byMember.put(recipient, group);
		// Accepting one invitation cancels every other one, so none becomes a future silent join.
		invitations.remove(recipient);
		return Result.OK;
	}

	public Result decline(UUID recipient, UUID inviter, long now) {
		prune(now);
		if (invitation(recipient, inviter) == null) return Result.NO_INVITE;
		removeInvitation(recipient, inviter);
		return Result.OK;
	}

	/** A departing leader passes leadership to the earliest remaining member. */
	public Result leave(UUID member) {
		Group group = byMember.get(member);
		if (group == null) return Result.NO_PARTY;
		removeMember(group, member);
		return Result.OK;
	}

	public Result kick(UUID actor, UUID member) {
		Group group = byMember.get(actor);
		if (group == null) return Result.NO_PARTY;
		if (!group.leader.equals(actor)) return Result.NOT_LEADER;
		if (actor.equals(member)) return Result.SELF;
		if (byMember.get(member) != group) return Result.NOT_MEMBER;
		removeMember(group, member);
		return Result.OK;
	}

	public Result disband(UUID actor) {
		Group group = byMember.get(actor);
		if (group == null) return Result.NO_PARTY;
		if (!group.leader.equals(actor)) return Result.NOT_LEADER;
		for (UUID member : group.members) byMember.remove(member);
		cancelInvitations(group.id);
		lastInvite.remove(actor);
		return Result.OK;
	}

	/** Disconnect keeps membership for reconnect, but cancels incoming and outgoing invitations. */
	public void disconnect(UUID member) {
		invitations.remove(member);
		invitations.values().forEach(map -> map.remove(member));
		invitations.values().removeIf(Map::isEmpty);
		lastInvite.remove(member);
	}

	public void prune(long now) {
		invitations.values().forEach(map -> map.values().removeIf(invite -> {
			Group group = byMember.get(invite.from);
			return invite.expired(now) || group == null || !group.id.equals(invite.party) || !group.leader.equals(invite.from);
		}));
		invitations.values().removeIf(Map::isEmpty);
		lastInvite.entrySet().removeIf(entry -> now < entry.getValue() || now - entry.getValue() >= INVITE_COOLDOWN_TICKS);
	}

	public void clear() {
		byMember.clear();
		invitations.clear();
		lastInvite.clear();
	}

	private Invite invitation(UUID recipient, UUID inviter) {
		Map<UUID, Invite> received = invitations.get(recipient);
		return received == null ? null : received.get(inviter);
	}

	private void removeInvitation(UUID recipient, UUID inviter) {
		Map<UUID, Invite> received = invitations.get(recipient);
		if (received != null) {
			received.remove(inviter);
			if (received.isEmpty()) invitations.remove(recipient);
		}
	}

	private void removeMember(Group group, UUID member) {
		byMember.remove(member);
		group.members.remove(member);
		disconnect(member);
		if (group.leader.equals(member)) {
			cancelInvitations(group.id);
			if (!group.members.isEmpty()) group.leader = group.members.iterator().next();
		}
	}

	private void cancelInvitations(UUID party) {
		invitations.values().forEach(map -> map.values().removeIf(invite -> invite.party.equals(party)));
		invitations.values().removeIf(Map::isEmpty);
	}
}
