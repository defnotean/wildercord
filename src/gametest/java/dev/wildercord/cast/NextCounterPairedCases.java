package dev.wildercord.cast;

import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static dev.wildercord.cast.NextSignatureNative.cast;
import static dev.wildercord.cast.NextSignatureNative.check;
import static dev.wildercord.cast.NextSignatureNative.teach;

/** The three counter scenarios needing two real connections, mandatory in the existing paired job. */
public final class NextCounterPairedCases {
	public static final List<String> CASES = List.of("COUNTER_QUIETUS_PAID_BOLT",
		"COUNTER_QUIETUS_THIRTEEN_EXISTING", "COUNTER_REFLECTED_RESPAWN_NULLCATCH");
	private UUID hostId, peerId;
	private Vec3 origin;
	private final List<RuneBolt> originals = new ArrayList<>();
	private RuneBolt projectile;
	private UUID projectileId;
	private ServerPlayer originalShooter;
	private float paidMana, emittedMana, taxedMana, receiverHealthBefore;
	private boolean captured, refused, watching;
	private int emittedAt, reflectedAt;
	private final Map<String, String> proof = new HashMap<>();

	public void runConnectedPair(ClientGameTestContext context, TestServerContext server, UUID host, UUID peer,
		Runnable respawnPeer, BiConsumer<String, Map<String, String>> observed, Consumer<String> completed) {
		check(!host.equals(peer), "Counter roles are distinct genuine players");
		hostId = host; peerId = peer;
		server.waitFor(s -> !dev.wildercord.aura.MastersArts.committed(host(s)) && !dev.wildercord.aura.MastersArts.committed(peer(s))
			&& !CastLock.locked(host(s)) && !CastLock.locked(peer(s)), NextSignatureRules.REST + 5);
		server.runOnServer(s -> {
			origin = host(s).position();
			for (int x = -4; x <= 12; x++) for (int z = -6; z <= 58; z++)
				host(s).level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
		});
		try {
			quietus(context, server, false, observed); completed.accept(CASES.get(0));
			// Keep the real UUID-scoped rest; do not clear or bypass a previous screen's claim.
			server.waitFor(s -> !Statuses.claimed(peer(s), "quietus", NextSignatureRules.REST), NextSignatureRules.REST + 5);
			quietus(context, server, true, observed); completed.accept(CASES.get(1));
			reflected(context, server, respawnPeer, observed); completed.accept(CASES.get(2));
		} finally {
			watching = false;
			server.runOnServer(s -> cleanup());
		}
	}

	private void quietus(ClientGameTestContext context, TestServerContext server, boolean crowded,
		BiConsumer<String, Map<String, String>> observed) {
		String id = CASES.get(crowded ? 1 : 0);
		proof.clear(); captured = false; projectile = null; projectileId = null;
		server.runOnServer(s -> {
			prepare(host(s)); prepare(peer(s)); place(host(s), 0, 0, 0, 0); place(peer(s), 0, 3, 180, 0);
		});
		context.waitTicks(3);
		server.runOnServer(s -> {
			var actor = host(s); var enemy = peer(s);
			check(Targets.canHarm(actor, enemy) && Targets.canHarm(enemy, actor), "Both genuine Survival casts have hostile admission");
			check(CounterSignatures.active() == 0, "No previous counter lease remains");
			if (crowded) {
				var group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
				for (int i = 0; i < 13; i++) {
					var bolt = RuneBolt.launch(new Cast(enemy), group, null, enemy.getEyePosition().add((i - 6) * .04, 0, 1.6), Vec3.ZERO, false);
					check(bolt != null && bolt.isAlive(), "Native pre-existing magic bolt created"); originals.add(bolt);
				}
				var batch = BoundedCounterCandidates.nearby(new Cast(actor), enemy);
				check(batch.enumerated() == 13 && batch.completeSnapshot().size() == 13 && batch.candidates().size() == 12
					&& ids(batch.completeSnapshot()).equals(ids(originals)), "Complete 13 actual UUIDs stay separate from nearest 12 handling");
				proof.put("existingUuids", String.join(",", ids(originals).stream().map(UUID::toString).sorted().toList()));
				proof.put("completeCount", "13"); proof.put("handledCount", "12");
			}
			cast(actor, Runes.BEAM, Runes.QUIETUS);
		});
		context.waitTicks(12);
		server.runOnServer(s -> {
			var enemy = peer(s);
			check(CounterSignatures.active() == 1 && screenOpened(enemy), "Actual paid Quietus has reached its capture window");
			if (crowded) {
				check(screenExisting(enemy).equals(ids(originals)), "Actual screen snapshots every one of the 13 pre-existing native UUIDs");
				check(originals.stream().allMatch(Entity::isAlive), "All 13 originals survive ordinary ticks until deliberate cleanup");
				originals.forEach(Entity::discard);
				check(originals.stream().noneMatch(Entity::isAlive), "Only deliberate fixture cleanup removes the original pool");
			}
		});
		// A new paid release is due on tick 3; tick 4 observes its dispatcher result before passive tick-5 mana regeneration.
		server.waitFor(s -> s.getTickCount() % 5 == 0, 6);
		server.runOnServer(s -> {
			var enemy = peer(s); var before = ids(bolts(enemy));
			cast(enemy, Runes.BOLT, Runes.HARM); paidMana = Spellbooks.mana(enemy);
			Scheduler.later(3, () -> {
				var emitted = bolts(enemy).stream().filter(bolt -> bolt.getOwner() == enemy && !before.contains(bolt.getUUID())).toList();
				check(emitted.size() == 1, "Exactly one actual paid hostile Bolt is newly emitted");
				projectile = emitted.getFirst(); projectileId = projectile.getUUID(); emittedAt = s.getTickCount();
				check(!projectile.isReflected() && projectile.tickCount == 0 && !screenExisting(enemy).contains(projectileId),
					"New paid Bolt is distinct from every screen-opening UUID and has native unreflected provenance");
				check(emittedAt % 5 != 0, "Capture admission is observed away from passive mana regeneration");
				emittedMana = Spellbooks.mana(enemy);
			});
			Scheduler.later(4, () -> {
				check(projectile != null && !projectile.isAlive() && CounterSignatures.active() == 0,
					"Actual dispatcher captures the newly paid hostile Bolt and consumes the screen");
				check(s.getTickCount() == emittedAt + 1 && s.getTickCount() % 5 != 0,
					"Exact capture-tax observation precedes the next passive mana tick");
				taxedMana = Spellbooks.mana(enemy);
				check(Math.abs(taxedMana - (emittedMana - 8)) < .001F, "Actual newly paid enemy magic has exactly eight mana tax once");
				captured = true;
			});
		});
		server.waitFor(s -> captured, 6);
		server.runOnServer(s -> {
			check(!projectile.isAlive() && CounterSignatures.active() == 0, "Consumed screen cannot capture or tax again");
			proof.put("projectileUuid", projectileId.toString()); proof.put("projectileEntity", Integer.toString(projectile.getId()));
			proof.put("projectilePresent", "false"); proof.put("paidMana", Float.toString(paidMana));
			proof.put("emittedMana", Float.toString(emittedMana)); proof.put("taxedMana", Float.toString(taxedMana));
			proof.put("tax", "8"); proof.put("screens", "0");
		});
		// Both actual-role witnesses precede cleanup and final passed acknowledgment.
		observed.accept(id, Map.copyOf(proof));
		server.runOnServer(s -> cleanup());
	}

	private void reflected(ClientGameTestContext context, TestServerContext server, Runnable respawnPeer,
		BiConsumer<String, Map<String, String>> observed) {
		proof.clear(); projectile = null; projectileId = null; refused = false;
		server.runOnServer(s -> {
			prepare(host(s)); prepare(peer(s)); place(host(s), 0, 0, 0, 0);
			// Aim at the defender's body so the genuinely reflected flight stays above the unchanged solid floor.
			place(peer(s), 0, 12, 180, (float) Math.toDegrees(Math.atan2(peer(s).getEyeHeight() - host(s).getBbHeight() / 2, 12)));
		});
		server.runCommand("spawnpoint " + server.computeOnServer(s -> peer(s).getScoreboardName()) + " "
			+ (int) (origin.x + 8) + " " + (int) origin.y + " " + (int) (origin.z - 3));
		context.waitTicks(3);
		server.runOnServer(s -> {
			originalShooter = peer(s); cast(originalShooter, Runes.BOLT, Runes.HARM);
			Scheduler.later(3, () -> {
				var emitted = bolts(originalShooter).stream().filter(bolt -> bolt.getOwner() == originalShooter).toList();
				check(emitted.size() == 1 && !emitted.getFirst().isReflected(), "Exactly one genuine paid, initially unreflected hostile Bolt leaves its shooter");
				projectile = emitted.getFirst(); projectileId = projectile.getUUID();
			});
		});
		context.waitTicks(3);
		server.runOnServer(s -> cast(host(s), Runes.SELF, Runes.SHIELD));
		server.waitFor(s -> projectile != null && projectile.isReflected(), 12);
		server.runOnServer(s -> {
			check(projectile.isAlive() && projectile.getUUID().equals(projectileId) && projectile.getOwner() == host(s),
				"Actual paid Shield reflects the original paid Bolt entity and transfers its owner");
			reflectedAt = projectile.tickCount;
			check(originalShooter.hurtServer(originalShooter.level(), originalShooter.damageSources().genericKill(), Float.MAX_VALUE)
				&& !originalShooter.isAlive(), "Actual vanilla damage kills the original connected shooter");
			proof.put("deadShooterEntity", Integer.toString(originalShooter.getId()));
		});
		// The peer sends the ordinary client respawn packet. This never fabricates a player or copies a body.
		respawnPeer.run();
		server.runOnServer(s -> {
			var receiver = peer(s);
			check(receiver != originalShooter && receiver.getUUID().equals(originalShooter.getUUID()) && !originalShooter.isAlive(),
				"A different current connected body has the original shooter's same UUID, while the old body stays dead");
			check(projectile.isAlive() && projectile.getUUID().equals(projectileId) && projectile.isReflected()
				&& projectile.tickCount > reflectedAt && projectile.tickCount - reflectedAt <= 6,
				"Same reflected projectile survives quarry death and ordinary flight during bounded native respawn");
			prepare(receiver);
			place(receiver, 0, 35, 180, 0);
			check(projectile.getOwner() == host(s) && receiver != host(s) && Targets.canHarm(host(s), receiver),
				"Replacement receiver is hostile to the live parrier, never its own projectile owner");
			cast(receiver, Runes.SELF, Runes.NULLCATCH);
			receiverHealthBefore = receiver.getHealth();
			watching = true;
			// The release already queued for +3 installs the screen and queues its +4 tick first.
			Scheduler.later(3, () -> Scheduler.later(1, () -> watchReflected(s)));
		});
		server.waitFor(s -> refused, 20);
		observed.accept(CASES.get(2), Map.copyOf(proof));
		server.runOnServer(s -> {
			check(!originalShooter.isAlive() && peer(s) != originalShooter && projectile.isAlive()
				&& projectile.getUUID().equals(projectileId) && projectile.isReflected() && CounterSignatures.active() == 1,
				"Actual peer observes retained reflected entity and live screen before deliberate cleanup");
			cleanup();
		});
		context.waitTicks(2);
	}

	/** Runs after the ordinary screen callback, observing admission without changing projectile motion or lifetime. */
	private void watchReflected(MinecraftServer server) {
		if (!watching || refused) return;
		var receiver = peer(server);
		check(projectile.isAlive() && projectile.isReflected() && !originalShooter.isAlive(), "Unmodified reflected flight and dead quarry persist until the actual front-cone test");
		var offset = projectile.position().subtract(receiver.getEyePosition());
		var motion = projectile.getDeltaMovement();
		var cast = new Cast(receiver);
		if (screenOpened(receiver) && BoundedCounterCandidates.nearby(cast, receiver).candidates().contains(projectile)
			&& offset.lengthSqr() >= .01 && offset.normalize().dot(receiver.getLookAngle()) >= .35
			&& motion.dot(receiver.getEyePosition().subtract(projectile.position())) > 0
			&& NextSignatureSafety.open(cast, projectile.position(), receiver.getEyePosition(), 4)) {
			check(projectile.getOwner() == host(server) && Targets.canHarm(host(server), receiver), "All hostile front-capture geometry admits the real reflected projectile");
			var owner = host(server);
			check(owner.isAlive() && owner.level() == receiver.level() && !Spirits.isBoss(owner) && !owner.isPermanentlyInvulnerable()
				&& NextSignatureSafety.loaded(cast, owner.blockPosition())
				&& !dev.wildercord.world.dungeons.DungeonWards.warded(cast.level, owner.blockPosition())
				&& NextSignatureSafety.target(cast, receiver, false), "Every ordinary owner and target safety gate also admits this candidate");
			check(owner.connection.hasClientLoaded() && receiver.connection.hasClientLoaded(), "Both real clients have completed loading before the armed reflection refusal");
			check(screenValue(receiver, "cast") instanceof Cast paidScreen
				&& NextSignaturePayments.of(paidScreen).left("nullcatch_capture", 1) == 1,
				"Real paid Nullcatch still owns its unspent one-projectile capture allowance");
			check(CounterSignatures.active() == 1 && projectile.getUUID().equals(projectileId),
				"Actual armed Nullcatch dispatch refuses the same reflected UUID after its quarry's real death and respawn");
			check(receiver.getHealth() == receiverHealthBefore, "Refusal is witnessed before ordinary projectile contact changes health");
			proof.put("projectileUuid", projectileId.toString()); proof.put("projectileEntity", Integer.toString(projectile.getId()));
			proof.put("projectilePresent", "true"); proof.put("projectileOwner", hostId.toString()); proof.put("reflected", "true");
			proof.put("replacementEntity", Integer.toString(receiver.getId())); proof.put("replacementUuid", receiver.getUUID().toString());
			proof.put("screens", "1"); proof.put("dispatchTick", Long.toString(receiver.level().getGameTime()));
			proof.put("healthBeforeContact", Float.toString(receiver.getHealth()));
			refused = true; return;
		}
		Scheduler.later(1, () -> watchReflected(server));
	}

	private void prepare(ServerPlayer player) {
		Charging.forget(player); player.removeAllEffects(); player.setAbsorptionAmount(0); player.setHealth(player.getMaxHealth());
		player.setPermanentlyInvulnerable(false);
		for (var slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
			EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		teach(player);
	}
	private void place(ServerPlayer player, double x, double z, float yaw, float pitch) {
		check(player.teleportTo(player.level(), origin.x + x, origin.y, origin.z + z, Set.of(), yaw, pitch, false), "Genuine connected body reaches the counter platform");
		player.setDeltaMovement(Vec3.ZERO);
		double y = player.getY(); player.move(MoverType.SELF, new Vec3(0, -.125, 0));
		check(player.onGround() && Math.abs(player.getY() - y) < .001, "Ordinary native movement meets the solid counter floor");
	}
	private ServerPlayer host(MinecraftServer server) { return connected(server, hostId); }
	private ServerPlayer peer(MinecraftServer server) { return connected(server, peerId); }
	private static ServerPlayer connected(MinecraftServer server, UUID id) {
		var player = server.getPlayerList().getPlayer(id);
		check(player != null && player.isAlive() && player.connection != null && player.connection.player == player
			&& server.getPlayerList().getPlayers().size() == 2, "Counter uses the exact current body of one of two genuine connections");
		return player;
	}
	private static List<RuneBolt> bolts(ServerPlayer player) { return player.level().getEntitiesOfClass(RuneBolt.class, player.getBoundingBox().inflate(64)); }
	private static Set<UUID> ids(List<? extends Entity> entities) { return entities.stream().map(Entity::getUUID).collect(Collectors.toSet()); }
	private static boolean screenOpened(ServerPlayer player) { return screenValue(player, "opened") instanceof Long opened && player.level().getGameTime() >= opened; }
	@SuppressWarnings("unchecked") private static Set<UUID> screenExisting(ServerPlayer player) { return (Set<UUID>) screenValue(player, "existing"); }
	private static Object screenValue(ServerPlayer player, String name) {
		try {
			var field = CounterSignatures.class.getDeclaredField("SCREENS"); field.setAccessible(true);
			var screen = ((Map<?, ?>) field.get(null)).get(player.getUUID());
			if (screen == null) return null;
			var accessor = screen.getClass().getDeclaredMethod(name); accessor.setAccessible(true); return accessor.invoke(screen);
		} catch (ReflectiveOperationException failure) { throw new AssertionError("Read-only native screen provenance", failure); }
	}
	private void cleanup() {
		watching = false;
		originals.forEach(Entity::discard); originals.clear();
		if (projectile != null) projectile.discard();
		projectile = null;
	}
}
