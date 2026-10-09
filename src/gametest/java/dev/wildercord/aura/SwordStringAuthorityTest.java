package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Real 26.3 attack/Punch ordering, pre-reset marks, tick-boundary pairing, spoof rejection and one-use requests. */
public final class SwordStringAuthorityTest implements FabricClientGameTest {
	private static final String PREFIX = "ledger_test:";
	private final Map<String, Integer> performed = new HashMap<>();
	private List<Integer> lastMarks = List.of();
	private LivingEntity target;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			try {
				context.waitTicks(40);
				world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
				world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
				world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:stone");
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					player.setGameMode(GameType.SURVIVAL);
					player.teleportTo(.5, 100, .5);
					player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
					player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
					player.setYRot(0);
					player.setXRot(0);
					target = foe(player, 2.2);
					for (String token : List.of("swing", "full", "low", "leap", "run")) {
						AuraApi.registerString(AuraApi.StringArt.of(PREFIX + token, token, AuraRules.GLOW, 0, 0, (actor, move) -> {
							performed.merge(token, 1, Integer::sum);
							lastMarks = move.marks();
							return true;
						}));
					}
					SwordStrings.forget(player.getUUID());
				});
				context.waitTicks(25);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					player.setSprinting(true);
					check(SwordString.Token.FULL.fits(SwordStrings.observedMarks(player)), "The actual server charge is full before attacking");
					float before = target.getHealth();
					player.connection.handleAttack(new ServerboundAttackPacket(target.getId()));
					check(target.getHealth() < before, "The real attack handler reached and hurt its target");
					check(!player.isSprinting() && player.getAttackStrengthScale(.5F) < AuraRules.FULL_SWING,
						"Vanilla consumed sprint and charge before the trailing Punch");
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					request(player, "run", SwordString.Token.marks(SwordString.Token.RUN, SwordString.Token.FULL, SwordString.Token.LEAP));
					check(count("run") == 1, "A legitimate pre-reset running hit is admitted");
					check(SwordString.Token.FULL.fits(lastMarks.getFirst()) && SwordString.Token.RUN.fits(lastMarks.getFirst())
						&& !SwordString.Token.LEAP.fits(lastMarks.getFirst()), "The performer gets observed FULL/RUN, never the forged extra LEAP bit");
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					request(player, "swing", SwordString.Token.SWING.bit());
					check(count("swing") == 0, "Same-tick duplicate Punch cannot fund another art after consumption");
				});

				context.waitTicks(1);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					player.resetAttackStrengthTicker();
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					for (var token : List.of(SwordString.Token.FULL, SwordString.Token.LOW, SwordString.Token.LEAP, SwordString.Token.RUN)) {
						int before = count(token.id);
						request(player, token.id, SwordString.Token.marks(token));
						check(count(token.id) == before, "A standing partial air swing cannot claim " + token.id);
					}
					request(player, "swing", Integer.MAX_VALUE);
					check(count("swing") == 1 && lastMarks.equals(List.of(SwordString.Token.SWING.bit())),
						"A valid plain stroke carries only the authoritative marks");
					request(player, "swing", SwordString.Token.SWING.bit());
					check(count("swing") == 1, "The same observed suffix cannot be replayed");
				});

				context.waitTicks(25);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					check(SwordString.Token.FULL.fits(SwordStrings.observedMarks(player)), "The untouched air-swing ticker is ready");
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					request(player, "full", SwordString.Token.marks(SwordString.Token.FULL));
					check(count("full") == 1, "An air Punch samples FULL before its own reset");
				});

				context.waitTicks(25);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					resetTarget(player);
					player.setSprinting(true);
					player.connection.handleAttack(new ServerboundAttackPacket(target.getId()));
				});
				context.waitTicks(1);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					request(player, "run", SwordString.Token.marks(SwordString.Token.RUN));
					check(count("run") == 2, "A legitimate attack/Punch pair split across server ticks retains pre-hit movement marks");
				});

				context.waitTicks(25);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					resetTarget(player);
					player.setSprinting(true);
					player.connection.handleAttack(new ServerboundAttackPacket(target.getId()));
				});
				context.waitTicks(SwordStringLedger.ATTACK_PUNCH_TICKS + 1);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					grounded(player);
					player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
					request(player, "run", SwordString.Token.marks(SwordString.Token.RUN));
					check(count("run") == 2, "A stale pre-hit RUN snapshot is not reusable by a later Punch");
					SwordStrings.forget(player.getUUID());
					resetTarget(player);
					LivingEntity other = foe(player, 2.2);
					float one = target.getHealth(), two = other.getHealth();
					new PiercingWeapon(false, false, Optional.empty(), Optional.empty()).attack(player, EquipmentSlot.MAINHAND);
					check(target.getHealth() < one && other.getHealth() < two, "One native piercing component actually hits two bodies");
					var twoSwings = AuraApi.StringArt.of(PREFIX + "two", "swing swing", AuraRules.GLOW, 0, 0, (actor, move) -> true);
					check(!SwordStrings.saw(player, twoSwings), "A multi-target spear component contributes only one stroke");
					request(player, "swing", SwordString.Token.SWING.bit());
					check(count("swing") == 2, "The single spear stroke remains usable");
					other.discard();
				});
			} finally {
				world.getServer().runOnServer(server -> {
					for (String token : List.of("swing", "full", "low", "leap", "run")) AuraApi.unregisterString(PREFIX + token);
				});
			}
		}
	}

	private int count(String token) { return performed.getOrDefault(token, 0); }
	private static void request(ServerPlayer player, String token, int marks) {
		SwordStrings.request(player, new SwordStrings.Perform(PREFIX + token, List.of(marks)));
	}
	private static void grounded(ServerPlayer player) {
		player.setShiftKeyDown(false);
		player.setSprinting(false);
		player.setOnGround(true);
		player.setDeltaMovement(0, 0, 0);
	}
	private void resetTarget(ServerPlayer player) {
		target.setHealth(target.getMaxHealth());
		Effects.readyToHurt(target);
		target.snapTo(player.getX(), player.getY(), player.getZ() + 1.7, 180, 0);
		target.setDeltaMovement(0, 0, 0);
	}
	private static LivingEntity foe(ServerPlayer player, double z) {
		var foe = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
		check(foe != null, "A native attack target exists");
		foe.setNoAi(true);
		foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		foe.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		foe.setHealth(100);
		foe.snapTo(player.getX(), player.getY(), z, 180, 0);
		player.level().addFreshEntity(foe);
		return foe;
	}
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
