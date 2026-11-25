package dev.wildercord.trials;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.sites.trials.TrialArenaPiece;
import dev.wildercord.world.sites.trials.TrialSites;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * End-to-end verification of Trial II (Ashen Crucible & Kharvok) and Trial III (Astral Eclipse Vault & Nyxara)
 * including Sovereign's Mantle Apex trigger, quench vents, prism alignment, Phase 2 split soaks,
 * Sovereign Crown & Trial Seals rewards, and Eclipse Twinblade / Astrolabe Dial relic usage.
 */
public final class CoopTrialsExpansionTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");

			// ---------------- Trial II: Ashen Crucible & Kharvok (2-player normal) ----------------
			BlockPos[] p2 = placeAndGetSpots(world, new BlockPos(200, 96, 200), "crucible");
			c.waitTicks(3);
			BlockPos gate2 = p2[0], center2 = p2[1], treasury2 = p2[2], stand2 = p2[3], view2 = p2[4], treasView2 = p2[5], ally2 = p2[6];

			UUID partnerId = world.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, stand2.getX() + 0.5, stand2.getY(), stand2.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
				p.setHealth(40.0F);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
				check(DungeonWards.warded(l, center2), "Ashen Crucible center is warded");
				check(!DungeonWards.admits(l, p, center2.below(), Blocks.BASALT.defaultBlockState(), false),
					"Survival player cannot break warded Crucible floor");

				var partner = new ExpansionCompanion(l, "coop-expansion-ally");
				partner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
				partner.setHealth(200.0F);
				partner.snapTo(ally2.getX() + 0.5, ally2.getY(), ally2.getZ() + 0.5, 0.0F, 0.0F);
				partner.setGameMode(GameType.SURVIVAL);
				p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(partner)));
				l.addNewPlayer(partner);
				return partner.getUUID();
			});

			awaitClientGateReady(c, stand2, gate2);
			use(c, gate2);
			c.waitTicks(8);

			UUID kharvokId = world.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var all = l.getEntitiesOfClass(KharvokBoss.class, new AABB(center2).inflate(30));
				check(all.size() == 1, "Crucible gate spawned exactly one Kharvok");
				KharvokBoss boss = all.getFirst();
				double expectedHp = KharvokRules.maxHealthFor(2, false);
				check(Math.abs(boss.getMaxHealth() - expectedHp) < 0.5F,
					"2-player Kharvok scaled to " + expectedHp + " HP (got " + boss.getMaxHealth() + ")");
				check(BossDamage.boss(boss), "Kharvok recognized by BossDamage");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, view2.getX() + 0.5, view2.getY(), view2.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				return boss.getUUID();
			});

			c.waitTicks(3);
			aimAt(c, center2, 2.2);
			c.waitTicks(1);
			shot(c, "kharvok_arena_idle");

			awaitKharvokMove(c, world, kharvokId, KharvokRules.ANVIL_SHATTER, 8);
			aimAt(c, center2, 2.0);
			c.waitTicks(2);
			shot(c, "kharvok_anvil_shatter");

			awaitKharvokMove(c, world, kharvokId, KharvokRules.CHAIN_SHACKLE, 4);
			aimAt(c, center2, 2.2);
			c.waitTicks(2);
			shot(c, "kharvok_chain_shackle");

			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				KharvokBoss boss = (KharvokBoss) l.getEntity(kharvokId);
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				int mask = boss.ventMask();
				check(Integer.bitCount(mask) == 2, "2-player Chain Shackle lit 2 vents (mask=" + mask + ")");
				for (int i = 0; i < 4; i++) {
					if ((mask & (1 << i)) != 0) {
						BlockPos vp = center2.offset(KharvokRules.VENT_OFFSETS[i][0], 1, KharvokRules.VENT_OFFSETS[i][1]);
						check(boss.tryQuenchVent(vp, partner), "Partner quenched Crucible Vent " + i);
					}
				}
				check(boss.staggered(), "Quenching all vents staggered Kharvok");
				boss.setHealth(boss.getMaxHealth() * 0.45F);
			});

			awaitKharvokMove(c, world, kharvokId, KharvokRules.CRUCIBLE_MELTDOWN, 4);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				KharvokBoss boss = (KharvokBoss) l.getEntity(kharvokId);
				check(boss.phaseTwo(), "Kharvok entered Phase 2");
				check(boss.soakCount() == 2, "2-player Crucible Meltdown spawned 2 soak rings");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				check(boss.hurtServer(l, l.damageSources().playerAttack(p), 6000.0F), "Killing blow accepted from Crucible challenger");
			});

			c.waitTicks(12);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				check(l.getEntity(kharvokId) == null || !l.getEntity(kharvokId).isAlive(), "Kharvok defeated cleanly");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				p.teleportTo(l, treasView2.getX() + 0.5, treasView2.getY(), treasView2.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				partner.teleportTo(l, treasView2.getX() + 1.5, treasView2.getY(), treasView2.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				var treBe = (TrialTreasuryBlockEntity) l.getBlockEntity(treasury2);
				check(treBe.isUnlocked(), "Crucible Treasury unlocked on Kharvok defeat");
				treBe.claim(p);
				treBe.claim(partner);
				check(hasItem(p, CoopTrialContent.CRUCIBLE_SEAL), "Primary player received Crucible Seal");
				check(hasItem(partner, CoopTrialContent.CRUCIBLE_SEAL), "Partner received Crucible Seal");
			});

			c.waitTicks(3);
			aimAt(c, treasury2, 0.5);
			c.waitTicks(1);
			shot(c, "kharvok_treasury_victory");

			// ---------------- Trial III: Astral Eclipse Vault & Nyxara (2-player APEX via Sovereign's Mantle) ----------------
			BlockPos[] p3 = placeAndGetSpots(world, new BlockPos(-200, 96, 200), "eclipse");
			c.waitTicks(3);
			BlockPos gate3 = p3[0], center3 = p3[1], treasury3 = p3[2], stand3 = p3[3], view3 = p3[4], treasView3 = p3[5], ally3 = p3[6];

			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				p.setHealth(40.0F);
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CoopTrialContent.SOVEREIGNS_MANTLE));
				p.teleportTo(l, stand3.getX() + 0.5, stand3.getY(), stand3.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				partner.snapTo(ally3.getX() + 0.5, ally3.getY(), ally3.getZ() + 0.5, 0.0F, 0.0F);
				check(DungeonWards.warded(l, center3), "Astral Eclipse Vault center is warded");
			});

			awaitClientGateReady(c, stand3, gate3);
			use(c, gate3);
			for (int i = 0; i < 30; i++) {
				c.waitTicks(1);
				if (world.getServer().computeOnServer(s ->
					s.overworld().getEntitiesOfClass(NyxaraBoss.class, new AABB(center3).inflate(30)).size() == 1)) {
					break;
				}
			}

			UUID nyxaraId = world.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var all = l.getEntitiesOfClass(NyxaraBoss.class, new AABB(center3).inflate(30));
				check(all.size() == 1, "Eclipse gate spawned exactly one Nyxara");
				NyxaraBoss boss = all.getFirst();
				check(boss.apex(), "Sovereign's Mantle triggered Apex Mode on Eclipse Trial");
				double expectedHp = NyxaraRules.maxHealthFor(2, true);
				check(Math.abs(boss.getMaxHealth() - expectedHp) < 0.5F,
					"2-player Apex Nyxara scaled to " + expectedHp + " HP (got " + boss.getMaxHealth() + ")");
				check(BossDamage.boss(boss), "Nyxara recognized by BossDamage");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, view3.getX() + 0.5, view3.getY(), view3.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				return boss.getUUID();
			});

			c.waitTicks(3);
			aimAt(c, center3, 2.2);
			c.waitTicks(1);
			shot(c, "nyxara_arena_idle");

			awaitNyxaraMove(c, world, nyxaraId, NyxaraRules.CRESCENT_SEVER, 7);
			aimAt(c, center3, 2.0);
			c.waitTicks(2);
			shot(c, "nyxara_crescent_sever");

			awaitNyxaraMove(c, world, nyxaraId, NyxaraRules.PRISM_CONVERGENCE, 4);
			aimAt(c, center3, 2.2);
			c.waitTicks(2);
			shot(c, "nyxara_prism_convergence");

			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				NyxaraBoss boss = (NyxaraBoss) l.getEntity(nyxaraId);
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				int mask = boss.prismMask();
				check(Integer.bitCount(mask) >= 2, "Apex Prism Convergence lit >=2 prisms (mask=" + mask + ")");
				for (int i = 0; i < 4; i++) {
					if ((mask & (1 << i)) != 0) {
						BlockPos pp = center3.offset(NyxaraRules.PRISM_OFFSETS[i][0], 1, NyxaraRules.PRISM_OFFSETS[i][1]);
						check(boss.tryAlignPrism(pp, partner), "Partner aligned Astral Prism " + i);
					}
				}
				check(boss.staggered(), "Aligning all Astral Prisms staggered Nyxara");
				boss.setHealth(boss.getMaxHealth() * 0.45F);
			});

			awaitNyxaraMove(c, world, nyxaraId, NyxaraRules.TOTAL_ECLIPSE, 4);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				NyxaraBoss boss = (NyxaraBoss) l.getEntity(nyxaraId);
				check(boss.phaseTwo(), "Nyxara entered Phase 2");
				check(boss.soakCount() >= 2, "Apex Total Eclipse spawned >=2 soak rings");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				check(boss.hurtServer(l, l.damageSources().playerAttack(p), 8000.0F), "Killing blow accepted from Eclipse challenger");
			});

			c.waitTicks(12);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				check(l.getEntity(nyxaraId) == null || !l.getEntity(nyxaraId).isAlive(), "Nyxara defeated cleanly");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				p.teleportTo(l, treasView3.getX() + 0.5, treasView3.getY(), treasView3.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				partner.teleportTo(l, treasView3.getX() + 1.5, treasView3.getY(), treasView3.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				var treBe = (TrialTreasuryBlockEntity) l.getBlockEntity(treasury3);
				check(treBe.isUnlocked(), "Eclipse Treasury unlocked on Nyxara defeat");
				check(treBe.isApex(), "Eclipse Treasury recorded Apex completion");
				treBe.claim(p);
				treBe.claim(partner);
				check(hasItem(p, CoopTrialContent.ECLIPSE_SEAL), "Primary player received Eclipse Seal");
				check(hasItem(p, CoopTrialContent.SOVEREIGN_CROWN), "Apex clear awarded Sovereign Crown");

				// Verify relic item uses without error
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CoopTrialContent.ECLIPSE_TWINBLADE));
				CoopTrialContent.ECLIPSE_TWINBLADE.use(l, p, InteractionHand.MAIN_HAND);
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(CoopTrialContent.ASTROLABE_DIAL));
				CoopTrialContent.ASTROLABE_DIAL.use(l, p, InteractionHand.OFF_HAND);
			});

			c.waitTicks(3);
			aimAt(c, treasury3, 0.5);
			c.waitTicks(1);
			shot(c, "nyxara_apex_victory");
		}
	}

	private static BlockPos[] placeAndGetSpots(TestSingleplayerContext world, BlockPos origin, String kind) {
		return world.getServer().computeOnServer(s -> {
			ServerLevel l = s.overworld();
			TrialArenaPiece arena = TrialSites.placeSanctum(l, origin, Direction.NORTH, kind);
			check(arena != null, "Placed Trial Sanctum: " + kind);
			BlockPos gate = arena.gatePosition();
			BlockPos center = arena.arenaCenter();
			BlockPos treasury = arena.treasuryPosition();
			Vec3 toGate = Vec3.atCenterOf(gate).subtract(Vec3.atCenterOf(center));
			Vec3 dir = new Vec3(toGate.x, 0.0, toGate.z).normalize();
			Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);

			BlockPos standSpot = BlockPos.containing(
				gate.getX() + 0.5 - dir.x * 2.5, gate.getY(), gate.getZ() + 0.5 - dir.z * 2.5);
			BlockPos viewSpot = BlockPos.containing(
				center.getX() + 0.5 + dir.x * 8.5, center.getY(), center.getZ() + 0.5 + dir.z * 8.5);
			BlockPos treasuryView = BlockPos.containing(
				treasury.getX() + 0.5 + dir.x * 4.5, treasury.getY(), treasury.getZ() + 0.5 + dir.z * 4.5);
			BlockPos allySpot = BlockPos.containing(
				center.getX() + 0.5 + perp.x * 6.0 + dir.x * 3.0, center.getY(), center.getZ() + 0.5 + perp.z * 6.0 + dir.z * 3.0);
			return new BlockPos[]{gate, center, treasury, standSpot, viewSpot, treasuryView, allySpot};
		});
	}

	private static void awaitClientGateReady(ClientGameTestContext c, BlockPos stand, BlockPos gate) {
		for (int i = 0; i < 80; i++) {
			if (c.computeOnClient(mc -> mc.level != null && mc.player != null
				&& mc.player.position().closerThan(Vec3.atCenterOf(stand), 3.5)
				&& mc.level.getBlockState(gate).is(CoopTrialContent.TRIAL_GATE))) {
				c.waitTicks(2);
				return;
			}
			c.waitTicks(1);
		}
		throw new AssertionError("Client never synced gate chunk at " + gate + " from stand " + stand);
	}

	private static boolean hasItem(ServerPlayer p, net.minecraft.world.item.Item item) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if (p.getInventory().getItem(i).is(item)) return true;
		}
		return false;
	}

	private static void awaitKharvokMove(ClientGameTestContext c, TestSingleplayerContext w, UUID bossId, int move, int waitAfter) {
		for (int i = 0; i < 650; i++) {
			if (w.getServer().computeOnServer(s -> s.overworld().getEntity(bossId) instanceof KharvokBoss b && b.move() == move)) {
				if (waitAfter > 0) c.waitTicks(waitAfter);
				return;
			}
			healPlayers(w);
			c.waitTicks(1);
		}
		throw new AssertionError("Kharvok never entered move " + move);
	}

	private static void awaitNyxaraMove(ClientGameTestContext c, TestSingleplayerContext w, UUID bossId, int move, int waitAfter) {
		for (int i = 0; i < 650; i++) {
			if (w.getServer().computeOnServer(s -> s.overworld().getEntity(bossId) instanceof NyxaraBoss b && b.move() == move)) {
				if (waitAfter > 0) c.waitTicks(waitAfter);
				return;
			}
			healPlayers(w);
			c.waitTicks(1);
		}
		throw new AssertionError("Nyxara never entered move " + move);
	}

	private static void healPlayers(TestSingleplayerContext w) {
		w.getServer().runOnServer(s -> {
			for (var p : s.overworld().players()) {
				if (p.getHealth() < 20.0F) p.setHealth(p.getMaxHealth());
			}
		});
	}

	private static void use(ClientGameTestContext c, BlockPos at) {
		c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
	}

	private static void aimAt(ClientGameTestContext c, BlockPos at, double yOffset) {
		c.runOnClient(mc -> {
			Vec3 v = Vec3.atCenterOf(at).add(0.0, yOffset, 0.0).subtract(mc.player.getEyePosition());
			mc.player.setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
			mc.player.setXRot((float) -Math.toDegrees(Math.atan2(v.y, v.horizontalDistance())));
		});
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> mc.gui.toastManager().clear());
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean ok, String why) {
		if (!ok) throw new AssertionError(why);
	}

	private static final class ExpansionCompanion extends FakePlayer {
		ExpansionCompanion(ServerLevel level, String name) {
			super(level, new GameProfile(
				UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), "TrialAllyEx"));
		}
	}
}
