package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Native collision geometry, accepted damage, fixed payment, replay and original-body retirement. */
public final class WallTurnSafetyTest implements FabricClientGameTest {
	private long sequence = 100;
	private static ServerPlayer rejectedDamage;
	private static boolean listener;
	private static final class Reloaded extends net.fabricmc.fabric.api.entity.FakePlayer {
		Reloaded(net.minecraft.server.level.ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "WallTurnReload"));
		}
	}
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		if (!listener) { listener = true; ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> entity != rejectedDamage); }
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.WallTurnSafetyTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				var p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -8; x <= 10; x++) for (int z = -4; z <= 4; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 108; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				for (int y = 100; y <= 106; y++) for (int z = -2; z <= 2; z++) p.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE));
				prepare(p); p.teleportTo(8.5, 102, .5); p.setOnGround(false);
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!press(p), "No wall rejects before payment"); check(Aura.aura(p) == 160, "Invalid contact costs nothing");
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p);
				p.fallDistance = 7;
				check(press(p), "A visible wall and whole-body room admit a paid brace");
				check(Aura.aura(p) == 140 && p.fallDistance >= 7, "Brace retains fall risk and pays exactly once");
				var view = MasterForms.view(p);
				check(!MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, view.epoch(), sequence)), "The accepted sequence cannot be replayed");
				check(!press(p), "Repeated presses without release do not activate a kick");
				int slot = p.getInventory().getSelectedSlot(); var original = p.getMainHandItem();
				p.getInventory().setSelectedSlot((slot + 1) % 9); p.getInventory().setSelectedSlot(slot);
				check(p.getMainHandItem() == original && !MasterForms.ownsMotion(p), "Same-call weapon switch-back cannot restore the original brace");
				check(MasterForms.data(p).airborneUsed() && MasterForms.data(p).readyAt() > MasterForms.now(p), "Cancellation retains both airborne use and paid rest");
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p); check(press(p), "Damage test braces");
				rejectedDamage = p;
				try { p.hurtServer(p.level(), p.damageSources().generic(), 3); } finally { rejectedDamage = null; }
				check(MasterForms.ownsMotion(p), "A rejected damage attempt does not interrupt a real brace");
				p.setAbsorptionAmount(20); p.setInvulnerableTime(0);
				p.hurtServer(p.level(), p.damageSources().generic(), 3);
				check(!MasterForms.ownsMotion(p), "Genuine fully absorbed damage interrupts without checking net health");
				p.setAbsorptionAmount(0);
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p); check(press(p), "Moving-obstruction test braces");
				p.level().setBlockAndUpdate(new BlockPos(1, 102, 0), Blocks.STONE.defaultBlockState());
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!press(p), "A newly placed obstruction refuses the actual swept first kick step");
				check(Aura.aura(p) == 140, "An obstructed paid brace has no refund or second charge");
				MasterForms.cancel(p); p.level().setBlockAndUpdate(new BlockPos(1, 102, 0), Blocks.AIR.defaultBlockState());
				prepare(p);
				p.level().setBlockAndUpdate(new BlockPos(1, 102, 0), Blocks.LAVA.defaultBlockState());
				check(!WallTurn.swept(p, p.position(), p.position().add(.75, .2, 0), false), "Real swept body volume refuses fluid before an endpoint reaches its centre");
				p.level().setBlockAndUpdate(new BlockPos(1, 102, 0), Blocks.AIR.defaultBlockState());
				p.level().setBlockAndUpdate(new BlockPos(1, 103, 0), Blocks.STONE.defaultBlockState());
				check(!WallTurn.swept(p, p.position(), p.position().add(.75, 0, 0), false), "Low ceiling and side obstruction are whole-body collisions");
				p.level().setBlockAndUpdate(new BlockPos(1, 103, 0), Blocks.AIR.defaultBlockState());
				p.level().getWorldBorder().setCenter(0, 0); p.level().getWorldBorder().setSize(3);
				check(!WallTurn.swept(p, p.position(), p.position().add(1, 0, 0), false), "The whole body must remain inside the world border");
				p.level().getWorldBorder().setSize(59_999_968);
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p); check(press(p), "Lifecycle test braces");
				long epoch = MasterForms.view(p).epoch(); var original = p.level();
				check(p.teleportTo(server.getLevel(Level.NETHER), .5, 102, .5, Set.of(), 0, 0, false), "A native dimension departure succeeds");
				check(p.teleportTo(original, .5, 102, .5, Set.of(), 0, 0, false), "The same body returns in the same call");
				check(!MasterForms.ownsMotion(p) && MasterForms.committed(p) && !MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, epoch, ++sequence)),
					"Same-call dimension round trip retires the move and its input epoch");
			});
			context.waitTicks(3); sequence = 100; fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p); check(press(p), "Death test braces");
				long oldEpoch = MasterForms.view(p).epoch(); long rest = MasterForms.data(p).readyAt();
				var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess());
				p.saveWithoutId(saved);
				var loaded = new Reloaded(p.level());
				loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess(), saved.buildResult()));
				check(MasterForms.data(loaded).equals(MasterForms.data(p)), "Native NBT reload retains learned form, equipped slot, rest and airborne use");
				check(MasterForms.view(loaded).epoch() == 0 && !MasterForms.ownsMotion(loaded), "Native reload cannot restore a transient movement owner or input epoch");
				loaded.discard();
				p.hurtServer(p.level(), p.damageSources().genericKill(), Float.MAX_VALUE);
				p.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				var replacement = server.getPlayerList().getPlayer(p.getUUID());
				check(replacement != p && replacement != null && replacement.isAlive(), "Native respawn creates a replacement original body");
				check(MasterForms.data(replacement).learned() && MasterForms.data(replacement).readyAt() == rest, "Death preserves the learned lesson and shared rest");
				check(!MasterForms.request(replacement, new MasterForms.Action(WallTurnRules.PRESS, oldEpoch, ++sequence)), "The old body's packet cannot activate its replacement");
				prepare(replacement);
				DungeonWards.remember(replacement.level(), () -> List.of(new BoundingBox(1, 100, -2, 4, 107, 2)));
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst(); prepare(p);
				check(!WallTurn.swept(p, p.position(), p.position().add(.75, 0, 0), false), "A loaded dungeon boundary rejects the swept body at its real cells");
			});
		} finally { rejectedDamage = null; }
	}
	private boolean press(ServerPlayer p) { return MasterForms.request(p, new MasterForms.Action(WallTurnRules.PRESS, MasterForms.view(p).epoch(), ++sequence)); }
	private void fresh(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst();
			MasterForms.request(p, new MasterForms.Action(WallTurnRules.RELEASE, MasterForms.view(p).epoch(), ++sequence)); });
		context.waitTicks(1);
	}
	private static void prepare(ServerPlayer p) {
		MasterForms.cancel(p); p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.setInvulnerableTime(0);
		p.teleportTo(.5, 102, .5); p.setNoGravity(false); p.setDeltaMovement(Vec3.ZERO); p.setOnGround(false); p.setShiftKeyDown(false);
		p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
		p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, MasterForms.WALL_TURN, 0, false, false));
	}
}
