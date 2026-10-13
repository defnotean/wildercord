package dev.wildercord.world.hollowbell;

import dev.wildercord.Wildercord;
import dev.wildercord.world.sites.belowkeeper.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.*;
import java.util.*;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;

/** Real client belfry packets, normal AI resonance attacks, a saved identity, and one attributed compass reward. */
public final class HollowBellEncounterTest implements FabricClientGameTest {
	private BellSanctumPiece piece;
	private UUID identity;

	@Override public void runTest(ClientGameTestContext c) {
		var oldParticles = c.computeOnClient(mc -> mc.options.particles().get());
		var oldCamera = c.computeOnClient(mc -> mc.options.getCameraType());
		net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
		try {
			try (var world = c.worldBuilder().create()) {
				c.waitTicks(25);
				world.getServer().runCommand("gamerule spawn_mobs false");
				world.getServer().runCommand("gamerule natural_health_regeneration false");
				world.getServer().runCommand("difficulty normal");
				world.getServer().runCommand("time set 6000");
				world.getServer().runOnServer(s -> {
					int index = 0;
					for (var direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
						var built = new BellSanctumPiece(96 + index++ * 64, 24, 96, direction);
						build(s.overworld(), built, false);
						verify(s.overworld(), built);
						build(s.overworld(), built, true);
						verify(s.overworld(), built);
						if (piece == null) piece = built;
					}
					var p = player(s);
					p.setGameMode(GameType.SURVIVAL);
					p.getInventory().clearContent();
					p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
					p.setHealth(200);
					p.getFoodData().setFoodLevel(20);
					p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false, false));
					visit(p, piece.belfry().offset(0, 0, -2));
				});
				c.waitTicks(6);
				use(c, piece.belfry());
				c.waitTicks(6);
				world.getServer().runOnServer(s -> {
					check(belfry(s).guardian() == null, "Unregistered decorative belfry cannot summon");
					register(s.overworld(), piece);
				});
				c.waitTicks(65);
				world.getServer().runOnServer(s -> check(belfry(s).guardian() == null, "Walking into the authentic Bell Sanctum never starts combat"));
				use(c, piece.belfry());
				world.getServer().waitFor(s -> belfry(s).guardian() != null, 60);
				identity = world.getServer().computeOnServer(s -> belfry(s).guardian());
				use(c, piece.belfry());
				c.waitTicks(3);
				world.getServer().runOnServer(s -> {
					check(belfry(s).guardian().equals(identity), "Repeated belfry packet preserves identity");
					unique(s);
					check(boss(s).active(), "Real survival use begins the resonance challenge");
					build(s.overworld(), piece, true);
					check(belfry(s).guardian().equals(identity), "Chunk replay preserves an active belfry's saved guardian");
					check(dev.wildercord.cast.Spirits.isBoss(boss(s)) && dev.wildercord.monster.Tempering.elite(boss(s)) == null,
						"The guardian has boss tempering, never a random elite");
					stand(s, 0, 4.5);
				});
				await(c, world, TOLL);
				capture(c, world, "hollow_bell_toll_full", false, false);
				float before = health(world);
				impact(world, TOLL);
				check(health(world) < before, "The annular pressure toll hits challengers standing in the 3.0..6.5 ring");
				world.getServer().runOnServer(s -> stand(s, 8, 4));
				await(c, world, CLAPPER);
				before = health(world);
				world.getServer().runOnServer(s -> stand(s, 0, -8));
				impact(world, CLAPPER);
				check(health(world) == before, "Stepping out of the locked clapper circle avoids damage");
				world.getServer().runOnServer(s -> stand(s, 5, 0));
				await(c, world, CHORD);
				capture(c, world, "hollow_bell_chord_minimal", true, false);
				before = health(world);
				impact(world, CHORD);
				check(health(world) < before, "The resonance chord damages an announced challenger outside the damped lane");
				world.getServer().runOnServer(s -> stand(s, 0, 4.5));
				await(c, world, TOLL);
				before = health(world);
				world.getServer().runOnServer(s -> stand(s, 0, 1.5));
				impact(world, TOLL);
				check(health(world) == before, "Standing in the quiet eye directly beneath the bell avoids the annular toll");
				world.getServer().runOnServer(s -> stand(s, 8, 4));
				await(c, world, CLAPPER);
				capture(c, world, "hollow_bell_clapper_third_person", true, true);
				before = health(world);
				impact(world, CLAPPER);
				check(health(world) < before, "Staying in the clapper mark receives its normal hit");
				world.getServer().runOnServer(s -> stand(s, 0, 6));
				await(c, world, CHORD);
				before = health(world);
				impact(world, CHORD);
				check(health(world) == before, "The whole damped shutter lane stays safe");
				world.getServer().runOnServer(s -> {
					var b = boss(s);
					check(b.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player(s)), 120), "A real player hit crosses half health");
					check(b.getHealth() > 0 && b.getHealth() <= b.getMaxHealth() / 2, "Phase change follows actual damage");
				});
				await(c, world, CHORD);
				world.getServer().runOnServer(s -> check(boss(s).secondPhase() && boss(s).acrossX(),
					"Phase two starts a new, rotated resonance chord after the old move recovers"));
				capture(c, world, "hollow_bell_encounter", false, true);
				// Leaving the arena retires the pending impact before anybody can inherit it.
				world.getServer().runOnServer(s -> stand(s, 0, 15.5));
				c.waitTicks(3);
				world.getServer().runOnServer(s -> check(boss(s).move() == IDLE, "Target leaving retires the announced damage"));
				c.waitTicks(QUIET_RESET + 5);
				world.getServer().runOnServer(s -> {
					check(!boss(s).active() && boss(s).getHealth() == boss(s).getMaxHealth(),
						"A quiet Bell Sanctum resets after its finite abandonment interval");
					visit(player(s), piece.belfry().offset(0, 0, -2));
				});
				c.waitTicks(5);
				use(c, piece.belfry());
				world.getServer().waitFor(s -> boss(s).active(), 60);
				world.getServer().runOnServer(s -> stand(s, 0, 4.5));
				await(c, world, TOLL);
				save = world.getWorldSave();
			}
			try (var world = save.open()) {
				world.getServer().runOnServer(s -> {
					check(belfry(s).guardian().equals(identity), "Saved belfry retains the exact guardian UUID");
					unique(s);
					check(!boss(s).active() && boss(s).move() == IDLE, "Reload cancels a mid-tell encounter without replaying an impact");
					visit(player(s), piece.belfry().offset(0, 0, -2));
				});
				c.waitTicks(5);
				use(c, piece.belfry());
				c.waitTicks(3);
				world.getServer().runOnServer(s -> {
					var p = player(s);
					var b = boss(s);
					check(b.active(), "The same saved guardian accepts another deliberate challenge");
					int beforeXp = p.totalExperience;
					check(b.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(p), 10000), "The participant deals the lethal hit");
					check(!b.isAlive() && belfry(s).slain(), "Actual death permanently completes this Bell Sanctum");
					check(p.getInventory().countItem(HollowBellContent.COMPASS) == 1, "The contributor receives one Quiet Bell compass");
					var compass = java.util.stream.IntStream.range(0, p.getInventory().getContainerSize()).mapToObj(p.getInventory()::getItem)
						.filter(i -> i.is(HollowBellContent.COMPASS)).findFirst().orElseThrow();
					var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
					check(tracker != null && !tracker.tracked() && tracker.target().orElseThrow().pos().equals(piece.belfry()),
						"The utility compass remembers this exact Bell Sanctum without requiring a lodestone");
					check(p.totalExperience == beforeXp + 80, "Victory pays its exact direct reward");
					check(dev.wildercord.lore.LoreJournal.data(p).has("learned:hollow_bell"), "The contributor keeps the encounter's field note");
				});
				use(c, piece.belfry());
				c.waitTicks(25);
				world.getServer().runOnServer(s -> {
					check(player(s).getInventory().countItem(HollowBellContent.COMPASS) == 1, "Using a completed belfry cannot repeat the reward");
					check(belfry(s).guardian().equals(identity), "Completed belfry never replaces its guardian");
					build(s.overworld(), piece, false);
					check(belfry(s).slain() && belfry(s).guardian().equals(identity), "Chunk replay preserves completion and cannot reopen the reward");
				});
				var listener = world.getServer().computeOnServer(s -> {
					var at = piece.belfry().offset(2, 0, -2);
					check(BelowkeeperResident.spawn(s.overworld(), at), "An ordinary Belowkeeper Listener enters the completed sanctum for the return conversation");
					return s.overworld().getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new AABB(at).inflate(3),
						v -> v.entityTags().contains(BelowkeeperResident.TAG)).getFirst().getUUID();
				});
				for (String line : List.of("greet", "glowcap", "pressure", "hollow_bell", "bell_return"))
					BelowkeeperStationChecks.hear(c, world, listener, line);
				int returnedXp = world.getServer().computeOnServer(s -> player(s).totalExperience);
				BelowkeeperStationChecks.hear(c, world, listener, "bell_return");
				world.getServer().runOnServer(s -> {
					check(player(s).totalExperience == returnedXp, "Return conversation cannot repay the encounter");
					check(player(s).getInventory().countItem(HollowBellContent.COMPASS) == 1, "Repeated thanks cannot create another compass");
				});
			}
			try (var world = save.open()) {
				world.getServer().runOnServer(s -> {
					check(belfry(s).slain() && belfry(s).guardian().equals(identity), "Completion and identity persist across reopening");
					check(player(s).getInventory().countItem(HollowBellContent.COMPASS) == 1, "The one reward survives reopening");
					check(dev.wildercord.lore.LoreJournal.data(player(s)).has("said:belowkeeper_listener.bell_return"),
						"The listener's response to the actual victory survives reopening");
				});
			}
		} finally {
			c.runOnClient(mc -> { mc.options.particles().set(oldParticles); mc.options.setCameraType(oldCamera); });
			piece = null;
			identity = null;
		}
	}

	private void capture(ClientGameTestContext c, TestSingleplayerContext w, String name, boolean minimal, boolean third) {
		w.getServer().waitFor(s -> boss(s).elapsed(0) >= tell(boss(s).move()) * .65, 60);
		w.getServer().runOnServer(s -> s.tickRateManager().setFrozen(true));
		try {
			c.runOnClient(mc -> {
				mc.options.particles().set(minimal ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
				mc.options.setCameraType(third ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : net.minecraft.client.CameraType.FIRST_PERSON);
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			});
			c.waitTicks(4);
			w.getConnection().waitForChunksRender();
			c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
		} finally {
			w.getServer().runOnServer(s -> s.tickRateManager().setFrozen(false));
		}
	}

	private void await(ClientGameTestContext c, TestSingleplayerContext w, int move) {
		w.getServer().waitFor(s -> boss(s).move() == move && boss(s).elapsed(0) >= 4 && boss(s).elapsed(0) < tell(move) - 6, 360);
		c.waitTicks(1);
	}
	private void impact(TestSingleplayerContext w, int move) { w.getServer().waitFor(s -> boss(s).move() == move && boss(s).elapsed(0) >= tell(move) + 1, 100); }
	private float health(TestSingleplayerContext w) { return w.getServer().computeOnServer(s -> player(s).getHealth()); }
	private void stand(net.minecraft.server.MinecraftServer s, double x, double z) {
		var p = player(s);
		var home = Vec3.atBottomCenterOf(piece.plinth().above());
		var at = home.add(x, 0, z);
		var delta = home.add(0, 1.6, 0).subtract(at.add(0, p.getEyeHeight(), 0));
		p.teleportTo(s.overworld(), at.x, at.y, at.z, Set.of(), (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
			(float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())), false);
		p.setDeltaMovement(Vec3.ZERO);
	}
	private static void visit(ServerPlayer p, BlockPos at) { p.teleportTo(p.level(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.of(), 0, 0, false); }
	private static ServerPlayer player(net.minecraft.server.MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private ListeningBelfryEntity belfry(net.minecraft.server.MinecraftServer s) { return (ListeningBelfryEntity) s.overworld().getBlockEntity(piece.belfry()); }
	private HollowBell boss(net.minecraft.server.MinecraftServer s) { return (HollowBell) s.overworld().getEntity(identity); }
	private void unique(net.minecraft.server.MinecraftServer s) {
		var box = piece.getBoundingBox();
		check(s.overworld().getEntitiesOfClass(HollowBell.class, new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1)).size() == 1,
			"One Bell Sanctum has exactly one guardian");
	}
	private static void use(ClientGameTestContext c, BlockPos pos) {
		c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
	}
	private static void verify(ServerLevel level, BellSanctumPiece piece) {
		check(level.getBlockEntity(piece.belfry()) instanceof ListeningBelfryEntity, "All four facings place the Listening Belfry inside their bounds");
		var book = (LecternBlockEntity) level.getBlockEntity(piece.localPosition(15, BellSanctumPiece.DECK + 1, 4));
		check(book != null && book.getBook().get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == 2, "Each clipped sanctum keeps its instruction book");
		check(level.getBlockState(piece.plinth()).is(Blocks.LODESTONE), "Central resonance plinth is present");
	}
	private static void build(ServerLevel level, BellSanctumPiece piece, boolean reverse) {
		var box = piece.getBoundingBox();
		var chunks = new ArrayList<ChunkPos>();
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
			level.getChunk(x, z);
			chunks.add(new ChunkPos(x, z));
		}
		if (reverse) Collections.reverse(chunks);
		for (var chunk : chunks) piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7),
			new BoundingBox(chunk.getMinBlockX(), level.getMinY() + 1, chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(), chunk.getMaxBlockZ()), chunk, BlockPos.ZERO);
	}
	private static void register(ServerLevel level, BellSanctumPiece piece) {
		var box = piece.getBoundingBox();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(BelowkeeperSites.BELL_SANCTUM)));
		var origin = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
		level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, new StructureStart(structure, origin, 0, new PiecesContainer(List.of(piece))));
		for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
			level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
