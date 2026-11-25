package dev.wildercord.trials;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.aura.world.Guardians;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.trials.LeylineSanctumPiece;
import dev.wildercord.world.sites.trials.TrialSites;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * End-to-end Co-Op Trial verification: places a registered Leyline Sanctum, checks DungeonWards,
 * spawns a 2-player party (480 HP Vaelkhor), exercises Halberd Cleave, Bell Toll Conduit dampening
 * stagger, Phase 2 Leyline Cataclysm soak rings, real in-game visual screenshots, and instanced
 * once-per-player Trial Treasury claims.
 */
public final class CoopTrialVaelkhorTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");

			BlockPos[] points = world.getServer().computeOnServer(s -> {
				Guardians.setEnabled(true);
				ServerLevel l = s.overworld();
				var piece = new LeylineSanctumPiece(0, 70, 0, Direction.NORTH);
				BoundingBox b = piece.getBoundingBox();
				for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
					for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
						l.getChunk(cx, cz);
					}
				}
				piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(),
					net.minecraft.util.RandomSource.create(2026), b, new ChunkPos(b.minX() >> 4, b.minZ() >> 4), BlockPos.ZERO);

				var structure = l.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("wildercord:" + TrialSites.SANCTUM_ID))).value();
				var startChunk = new ChunkPos(b.minX() >> 4, b.minZ() >> 4);
				var start = new StructureStart(structure, startChunk, 0, new PiecesContainer(List.<StructurePiece>of(piece)));
				for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
					for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
						var chunk = l.getChunk(cx, cz);
						chunk.addReferenceForStructure(structure, startChunk.pack());
						chunk.setStartForStructure(structure, start);
					}
				}
				return new BlockPos[]{
					piece.gatePosition(),
					piece.arenaCenter(),
					piece.treasuryPosition(),
					piece.localPosition(20, LeylineSanctumPiece.FLOOR + 1, 7),
					piece.localPosition(20, LeylineSanctumPiece.FLOOR + 1, 14)
				};
			});

			c.waitTicks(3); // flush DungeonWards.remember queue
			BlockPos gate = points[0], center = points[1], treasury = points[2], standSpot = points[3], allySpot = points[4];

			// Verify ward protection + spawn 2nd party player
			UUID partnerId = world.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, standSpot.getX() + 0.5, standSpot.getY(), standSpot.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setHealth(20.0F);
				p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
				p.setHealth(40.0F);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));

				check(DungeonWards.warded(l, center), "Leyline Sanctum center is warded");
				check(!DungeonWards.admits(l, p, center.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), false),
					"Survival player cannot mine warded Sanctum floor");

				var partner = new TrialCompanion(l);
				partner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
				partner.setHealth(200.0F);
				partner.snapTo(allySpot.getX() + 0.5, allySpot.getY(), allySpot.getZ() + 0.5, 0.0F, 0.0F);
				partner.setGameMode(GameType.SURVIVAL);
				p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(partner)));
				l.addNewPlayer(partner);
				return partner.getUUID();
			});

			c.waitTicks(5);
			use(c, gate);
			c.waitTicks(8);

			UUID bossId = world.getServer().computeOnServer(s -> {
				var all = s.overworld().getEntitiesOfClass(VaelkhorBoss.class, new AABB(center).inflate(30));
				check(all.size() == 1, "Trial Gate spawned exactly one Vaelkhor");
				VaelkhorBoss boss = all.getFirst();
				check(Math.abs(boss.getMaxHealth() - 480.0F) < 0.5F, "2-player Vaelkhor scaled to 480 HP (got " + boss.getMaxHealth() + ")");
				check(BossDamage.boss(boss), "Vaelkhor recognized by BossDamage");
				return boss.getUUID();
			});

			// Capture arena idle view looking at Vaelkhor
			aimAt(c, center, 2.2);
			c.waitTicks(3);
			shot(c, "vaelkhor_arena_idle");

			// Wait for HALBERD_CLEAVE & capture screenshot
			awaitMove(c, world, bossId, VaelkhorRules.HALBERD_CLEAVE, 8);
			aimAt(c, center, 2.0);
			c.waitTicks(2);
			shot(c, "vaelkhor_halberd_cleave");

			// Wait for BELL_TOLL_CONDUIT, capture screenshot, then dampen all active conduits for stagger
			awaitMove(c, world, bossId, VaelkhorRules.BELL_TOLL_CONDUIT, 4);
			aimAt(c, center, 2.2);
			c.waitTicks(2);
			shot(c, "vaelkhor_bell_toll_conduit");

			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				VaelkhorBoss boss = (VaelkhorBoss) l.getEntity(bossId);
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				int mask = boss.conduitMask();
				check(Integer.bitCount(mask) == 2, "2-player Bell Toll Conduit activated 2 conduits (mask=" + mask + ")");
				for (int i = 0; i < 4; i++) {
					if ((mask & (1 << i)) != 0) {
						BlockPos conduitPos = center.offset(VaelkhorRules.CONDUIT_OFFSETS[i][0], 1, VaelkhorRules.CONDUIT_OFFSETS[i][1]);
						check(boss.tryDampenConduit(conduitPos, partner), "Partner dampened conduit " + i);
					}
				}
				check(boss.staggered(), "Dampening all conduits staggered Vaelkhor");
				// Drop Vaelkhor into Phase 2 and verify bonus stagger multiplier works cleanly
				boss.setHealth(boss.getMaxHealth() * 0.45F);
			});

			// Wait for Phase 2 LEYLINE_CATACLYSM soak rings and defeat Vaelkhor
			awaitMove(c, world, bossId, VaelkhorRules.LEYLINE_CATACLYSM, 4);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				VaelkhorBoss boss = (VaelkhorBoss) l.getEntity(bossId);
				check(boss.phaseTwo(), "Vaelkhor entered Phase 2");
				check(boss.soakCount() == 2, "2-player Leyline Cataclysm spawned 2 soak rings");
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, treasury.getX() + 0.5, treasury.getY(), treasury.getZ() - 3.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				boss.hurtServer(l, l.damageSources().playerAttack(p), 5000.0F);
			});

			c.waitTicks(12);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				check(l.getEntity(bossId) == null || !l.getEntity(bossId).isAlive(), "Vaelkhor died cleanly");
				var gateBe = (TrialGateBlockEntity) l.getBlockEntity(gate);
				var treBe = (TrialTreasuryBlockEntity) l.getBlockEntity(treasury);
				check(gateBe.slain(), "Trial Gate recorded slain");
				check(treBe.isUnlocked(), "Trial Treasury unlocked on Vaelkhor death");

				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				ServerPlayer partner = (ServerPlayer) l.getEntity(partnerId);
				int beforeMain = countRewards(p);
				int beforePartner = countRewards(partner);
				treBe.claim(p);
				treBe.claim(partner);
				check(countRewards(p) > beforeMain, "Primary challenger received instanced Treasury reward");
				check(countRewards(partner) > beforePartner, "Party partner received instanced Treasury reward");

				int afterFirstClaim = countRewards(p);
				treBe.claim(p);
				check(countRewards(p) == afterFirstClaim, "Double-claim rejected");
			});

			aimAt(c, treasury, 0.5);
			c.waitTicks(3);
			shot(c, "vaelkhor_treasury_victory");
		}
	}

	private static int countRewards(ServerPlayer p) {
		int total = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack st = p.getInventory().getItem(i);
			if (st.is(CoopTrialContent.SOVEREIGN_HALBERD) || st.is(CoopTrialContent.LEYLINE_BELL_CHARM)) {
				total += st.getCount();
			}
		}
		return total;
	}

	private static void awaitMove(ClientGameTestContext c, TestSingleplayerContext w, UUID bossId, int move, int waitAfter) {
		for (int i = 0; i < 600; i++) {
			if (w.getServer().computeOnServer(s -> {
				var e = s.overworld().getEntity(bossId);
				return e instanceof VaelkhorBoss b && b.move() == move;
			})) {
				if (waitAfter > 0) c.waitTicks(waitAfter);
				return;
			}
			w.getServer().runOnServer(s -> {
				for (var p : s.overworld().players()) {
					if (p.getHealth() < 16.0F) p.setHealth(p.getMaxHealth());
				}
			});
			c.waitTicks(1);
		}
		throw new AssertionError("Vaelkhor never entered move " + move);
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

	private static final class TrialCompanion extends FakePlayer {
		TrialCompanion(ServerLevel level) {
			super(level, new GameProfile(
				UUID.nameUUIDFromBytes("coop-trial-partner".getBytes(StandardCharsets.UTF_8)), "TrialAlly"));
		}
	}
}
