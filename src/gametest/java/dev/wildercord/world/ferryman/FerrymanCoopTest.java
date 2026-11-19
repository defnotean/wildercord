package dev.wildercord.world.ferryman;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.water.LastCrossingPiece;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

/**
 * The crossing kept by two: a registered last-crossing start, a bell that rings its guardian,
 * a phase-two anchor that marks both oarsmen, a tell that survives losing one challenger,
 * and a second count shared by every contributor.
 */
public final class FerrymanCoopTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");
			BlockPos[] crossing = world.getServer().computeOnServer(s -> {
				var l = s.overworld();
				var piece = new LastCrossingPiece(0, 60, 0, Direction.NORTH);
				BoundingBox b = piece.getBoundingBox();
				for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++)
					for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) l.getChunk(cx, cz);
				piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(),
					net.minecraft.util.RandomSource.create(77), b, new ChunkPos(b.minX() >> 4, b.minZ() >> 4), BlockPos.ZERO);
				var structure = l.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("wildercord:water_last_crossing"))).value();
				var startChunk = new ChunkPos(b.minX() >> 4, b.minZ() >> 4);
				var start = new StructureStart(structure, startChunk, 0, new PiecesContainer(List.<StructurePiece>of(piece)));
				// The lookup path is references-then-starts: every chunk in the footprint points at the
				// start's chunk, and that chunk carries the start itself.
				for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++)
					for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
						var chunk = l.getChunk(cx, cz);
						chunk.addReferenceForStructure(structure, startChunk.pack());
						chunk.setStartForStructure(structure, start);
					}
				var at = piece.bellPosition();
				check(l.getBlockState(at).is(FerrymanContent.BELL), "The placed crossing carries its bell");
				var found = l.structureManager().getStructureWithPieceAt(at,
					h -> h.value() instanceof SiteStructure site && site.site().equals("water_last_crossing"));
				check(found.isValid() && found.getPieces().stream().anyMatch(p -> p instanceof LastCrossingPiece),
					"The bell finds its own registered crossing start");
				// Dry deck columns beside the bell and six blocks past the mooring, both inside the arena.
				return new BlockPos[] {at, piece.mooringPosition(), piece.localPosition(17, LastCrossingPiece.DECK + 1, 6),
					piece.localPosition(17, LastCrossingPiece.DECK + 1, 23)};
			});
			BlockPos bell = crossing[0], mooring = crossing[1];
			stand(world, crossing[2]);
			c.waitTicks(15);
			use(c, bell);
			c.waitTicks(10);
			UUID guardian = world.getServer().computeOnServer(s -> {
				var all = s.overworld().getEntitiesOfClass(ReedCrownedFerryman.class, new AABB(bell).inflate(40));
				check(all.size() == 1, "Ringing the crossing bell spawns exactly one Ferryman");
				var f = all.getFirst();
				check(f.active() && f.belongsTo(bell), "The Ferryman keeps the crossing it was rung for");
				check(Vec3.atBottomCenterOf(mooring).distanceToSqr(f.position()) < 1, "The Ferryman is held at its mooring");
				return f.getUUID();
			});
			// Solo: the anchor marks one place only.
			awaitMove(c, world, guardian, ANCHOR, false, false);
			world.getServer().runOnServer(s -> check(!ferryman(s, guardian).pairedMark(), "A lone challenger is anchored alone"));

			// A companion takes the far side of the crossing.
			UUID oarsman = world.getServer().computeOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var fake = new Oarsman(s.overworld());
				fake.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
				fake.setHealth(500);
				fake.snapTo(crossing[3].getX() + .5, crossing[3].getY(), crossing[3].getZ() + .5, 0, 0);
				fake.setOnGround(true);
				fake.setGameMode(GameType.SURVIVAL);
				p.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fake)));
				s.overworld().addNewPlayer(fake);
				return fake.getUUID();
			});
			// Both stand on the crossing while the ferryman is brought to its second count.
			world.getServer().runOnServer(s -> {
				var f = ferryman(s, guardian);
				var p = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < 8 && !second(f.getHealth(), f.getMaxHealth()); i++)
					f.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(p), 40);
				check(second(f.getHealth(), f.getMaxHealth()), "Damage from a challenger brings the second count");
			});
			awaitMove(c, world, guardian, ANCHOR, true, true);
			var marks = world.getServer().computeOnServer(s -> {
				var f = ferryman(s, guardian);
				double[] absolute = {Math.abs(f.markX()), Math.abs(f.markZ()), Math.abs(f.markX2()), Math.abs(f.markZ2())};
				Arrays.sort(absolute);
				return absolute;
			});
			check(Arrays.equals(marks, new double[] {0, 0, 6, 11}),
				"The paired anchor marks both oarsmen's places (got " + Arrays.toString(marks) + ")");
			aim(c, mooring, 2);
			c.waitTicks(5);
			shot(c, "ferryman_coop_paired_anchor");

			// The lone survivor keeps the announced impact: the first challenger walks off mid-tell.
			world.getServer().runOnServer(s -> {
				oarsman(s, oarsman).setHealth(500);
				var p = s.getPlayerList().getPlayers().getFirst();
				p.teleportTo(s.overworld(), 400, 200, 400, Set.<Relative>of(), 0, 0, false);
			});
			boolean struck = false;
			for (int i = 0; i < 55 && !struck; i++) {
				struck = world.getServer().computeOnServer(s -> oarsman(s, oarsman).getHealth() < 500);
				if (!struck) c.waitTicks(1);
			}
			check(struck, "The tell stays announced against the companion who remained warned");

			// Both keepers are paid by the shared second count.
			world.getServer().runOnServer(s -> {
				var f = ferryman(s, guardian);
				var p = s.getPlayerList().getPlayers().getFirst();
				p.teleportTo(s.overworld(), mooring.getX() + .5, mooring.getY(), mooring.getZ() - 3.5, Set.<Relative>of(), 0, 0, false);
				f.setHealth(1);
				f.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(p), 20);
				check(s.overworld().getBlockEntity(bell) instanceof CrossingBellEntity keep && keep.slain(),
					"The saved guardian marks its bell slain");
			});
			c.waitTicks(10);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var fake = oarsman(s, oarsman);
				check(p.getInventory().countItem(AuraWorld.AURA_SHARD) == 3
						&& fake.getInventory().countItem(AuraWorld.AURA_SHARD) == 3,
					"A shared second count pays both keepers three shards each (traveller "
						+ p.getInventory().countItem(AuraWorld.AURA_SHARD) + ", companion " + fake.getInventory().countItem(AuraWorld.AURA_SHARD) + ")");
				check(p.getInventory().countItem(Items.COMPASS) == 1, "The traveller keeps a crossing compass");
				check(LoreJournal.data(p).has("learned:last_crossing"), "The shared crossing is written to the journal");
			});
			aim(c, bell, 1);
			c.waitTicks(5);
			shot(c, "ferryman_coop_shared_victory");
		}
	}

	private static ReedCrownedFerryman ferryman(net.minecraft.server.MinecraftServer s, UUID id) {
		var f = (ReedCrownedFerryman) s.overworld().getEntity(id);
		check(f != null, "The Ferryman is still keeping the crossing");
		return f;
	}
	private static ServerPlayer oarsman(net.minecraft.server.MinecraftServer s, UUID id) {
		var p = s.overworld().getPlayerByUUID(id);
		check(p instanceof ServerPlayer, "The companion is still on the crossing");
		return (ServerPlayer) p;
	}
	private static void awaitMove(ClientGameTestContext c, TestSingleplayerContext w, UUID id, int move, boolean second, boolean paired) {
		for (int i = 0; i < 1200; i++) {
			if (w.getServer().computeOnServer(s -> {
				var f = s.overworld().getEntity(id);
				return f instanceof ReedCrownedFerryman k && k.move() == move && k.secondPhase() == second && (!paired || k.pairedMark());
			})) return;
			w.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				if (p.getHealth() < 12) p.setHealth(20);
			});
			c.waitTicks(1);
		}
		throw new AssertionError("Ferryman did not reach move " + move + " (second=" + second + ", paired=" + paired + ")");
	}
	private static void stand(TestSingleplayerContext w, BlockPos at) {
		w.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.SURVIVAL);
			p.teleportTo(s.overworld(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.<Relative>of(), 0, 0, false);
			p.setHealth(20);
			p.fallDistance = 0;
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		});
	}
	private static void use(ClientGameTestContext c, BlockPos at) {
		c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
	}
	private static void aim(ClientGameTestContext c, BlockPos at, double lift) {
		c.runOnClient(mc -> {
			var v = Vec3.atCenterOf(at).add(0, lift, 0).subtract(mc.player.getEyePosition());
			mc.player.setYRot((float) (Math.toDegrees(Math.atan2(-v.x, v.z))));
			mc.player.setXRot((float) (-Math.toDegrees(Math.atan2(v.y, v.horizontalDistance()))));
		});
	}
	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> mc.gui.toastManager().clear());
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}
	private static final class Oarsman extends net.fabricmc.fabric.api.entity.FakePlayer {
		Oarsman(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(
				UUID.nameUUIDFromBytes("ferryman-coop-oarsman".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "Oarsman"));
		}
	}
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}
