package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Real walls, ledges, hazards and wards stop field-form travel short; damage interrupts without refund; saves and respawns keep only the lesson. */
public final class FormDashSafetyTest implements FabricClientGameTest {
	private long sequence = 100;
	private float afterHit;
	private static net.minecraft.world.entity.LivingEntity target;
	private static final class Reloaded extends net.fabricmc.fabric.api.entity.FakePlayer {
		Reloaded(net.minecraft.server.level.ServerLevel level) { super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "FormDashReload")); }
	}
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.FormDashSafetyTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				var p = server.getPlayerList().getPlayers().getFirst();
				var empty = FormDash.Progress.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
				check(empty.equals(FormDash.Progress.NONE), "An old empty record decodes to no field form");
				var forged = new CompoundTag(); forged.put("learned", IntTag.valueOf(-1)); forged.put("equipped", IntTag.valueOf(1)); forged.put("practiced", IntTag.valueOf(-1));
				var normal = FormDash.Progress.CODEC.parse(NbtOps.INSTANCE, forged).getOrThrow();
				check(normal.learned() == (FormDashRules.bit(2) | FormDashRules.bit(3)) && normal.equipped() == 0 && normal.practiced() == normal.learned(),
					"Unknown bits and a Wall Turn slot number cannot enter the field-form record");
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.EMBER).withClear(MastersRules.GALE));
				// A record from a world with a later clock, holding both slots, as only an edited or merged save can.
				long now = MasterForms.now(p);
				p.setAttached(FormDash.PROGRESS, new FormDash.Progress(normal.learned(), FormDashRules.CINDER_LUNGE, now + 1_000_000, now + 1_000_000, 0));
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, MasterForms.WALL_TURN, now + 1_000_000, false, false, now + 1_000_000, 0, false));
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				long now = MasterForms.now(p);
				var field = FormDash.data(p); var wall = MasterForms.data(p);
				check(field.readyAt() <= now + FormDashRules.MAX_REST && field.recoveryUntil() <= now + FormDashRules.MAX_RECOVERY,
					"A loaded field-form rest and recovery never exceed one use from now: " + field + " at " + now);
				check(wall.readyAt() <= now + WallTurnRules.REST_TICKS && wall.recoveryUntil() <= now + WallTurnRules.RECOVERY_TICKS,
					"A loaded Wall Turn rest and recovery never exceed one use from now: " + wall + " at " + now);
				check(wall.equipped() == MasterForms.WALL_TURN && field.equipped() == 0, "A save holding both slots keeps Wall Turn and empties the field-form slot");
			});
			// A two-high wall three blocks ahead: the dash stops short with no cut and the longer recovery.
			arena(context, world, p -> { for (int y = 100; y <= 101; y++) for (int z = -2; z <= 2; z++) p.level().setBlockAndUpdate(new BlockPos(3, y, z), Blocks.STONE.defaultBlockState()); });
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(press(p) && Aura.aura(p) == 135, "A clear first stretch admits a paid lunge");
				check(!press(p), "The accepted sequence cannot be replayed into a second lunge");
			});
			long stalledAt = -1;
			for (int i = 0; i < FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 4 && stalledAt < 0; i++) {
				context.waitTicks(1);
				stalledAt = world.getServer().computeOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); return FormDash.ownsMotion(p) ? -1L : MasterForms.now(p); });
			}
			long stall = stalledAt;
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(stall >= 0, "The lunge into the wall ended");
				check(p.getBoundingBox().maxX <= 3.001 && p.getX() > 1.5, "The swept body stops at the wall face: " + p.position());
				check(!FormDash.data(p).drilled(FormDashRules.CINDER_LUNGE) && FormDash.data(p).recoveryUntil() == stall + FormDashRules.STALL_RECOVERY,
					"A stalled lunge is not practice and owes exactly the longer recovery: " + FormDash.data(p).recoveryUntil() + " stalled at " + stall);
				check(Aura.aura(p) == 135, "A stall neither refunds nor charges again");
			});
			// A ledge: a three-deep pit from x = 3 on. The body keeps footing and does not fall.
			arena(context, world, p -> { for (int y = 96; y <= 99; y++) for (int x = 3; x <= 8; x++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState()); });
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Ledge test lunges"); });
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(p.getY() - 100) < .01 && p.getBoundingBox().minX < 3 && p.getX() > 1.5, "A ledge ends travel with support underfoot: " + p.position());
			});
			// Lava floor ahead: no accepted body position stands over it.
			arena(context, world, p -> { for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(3, 99, z), Blocks.LAVA.defaultBlockState()); });
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Hazard test lunges"); });
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getBoundingBox().maxX < 3 && !p.isOnFire() && !p.isInLava(), "A hazard ends travel before the body stands over it: " + p.position());
			});
			// A one-block pit across the lane: no stride may step over the gap between its stride points.
			arena(context, world, p -> { for (int y = 96; y <= 99; y++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(3, y, z), Blocks.AIR.defaultBlockState()); });
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Gap test lunges"); });
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(p.getY() - 100) < .01 && p.getBoundingBox().minX < 3 && p.getX() < 3.5 && p.getX() > 1.5,
					"A one-block gap stops travel on the near side: " + p.position());
				check(!FormDash.data(p).drilled(FormDashRules.CINDER_LUNGE), "A lunge stopped by a gap is a stall, not practice");
			});
			// A foe behind a wall at the end of a clean lunge is in reach but out of sight: the cut does not pass through stone.
			arena(context, world, p -> {
				for (int y = 100; y <= 101; y++) for (int z = -2; z <= 2; z++) p.level().setBlockAndUpdate(new BlockPos(6, y, z), Blocks.STONE.defaultBlockState());
				var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(husk != null, "The hidden-foe fixture exists");
				husk.setNoAi(true); husk.snapTo(7.5, 100, .5, 90, 0); p.level().addFreshEntity(husk); target = husk;
			});
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Hidden-foe test lunges"); });
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getX() > 4.5 && FormDash.data(p).drilled(FormDashRules.CINDER_LUNGE), "The lunge ran its full clean length into the cut: " + p.position());
				check(target.getHealth() == target.getMaxHealth(), "The cut never reaches a foe behind a wall");
				target.discard(); target = null;
			});
			// Looking straight down still lunges along the body's facing.
			arena(context, world, p -> {});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.setXRot(90);
				check(press(p) && Aura.aura(p) == 135, "A lunge looking straight down is accepted");
			});
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getX() > 4.5 && Math.abs(p.getZ() - .5) < .1, "A vertical look lunges along the facing: " + p.position());
			});
			// An obstruction in the first stretch refuses before payment or rest.
			arena(context, world, p -> { for (int y = 100; y <= 101; y++) p.level().setBlockAndUpdate(new BlockPos(1, y, 0), Blocks.STONE.defaultBlockState()); });
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!press(p) && Aura.aura(p) == 160 && FormDash.data(p).readyAt() == 0 && !MasterForms.committed(p), "A blocked start costs nothing");
			});
			// Damage during the plant interrupts with no refund and the full stopped recovery.
			arena(context, world, p -> {});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(press(p), "Damage test lunges"); long rest = FormDash.data(p).readyAt();
				// Starvation is outside Aura armour, so the exact Aura left proves nothing was given back.
				p.setInvulnerableTime(0); p.hurtServer(p.level(), p.damageSources().starve(), 2);
				check(!FormDash.ownsMotion(p) && MasterForms.committed(p), "Damage stops the lunge and the recovery still holds");
				check(FormDash.data(p).readyAt() == rest && Aura.aura(p) == 135, "An interrupt keeps the paid rest and has no refund: " + FormDash.data(p).readyAt() + "/" + rest + " aura " + Aura.aura(p));
				check(FormDash.data(p).recoveryUntil() >= MasterForms.now(p) + FormDashRules.STALL_RECOVERY, "An interrupt pays the full stopped recovery");
				afterHit = Aura.aura(p);
			});
			fresh(context, world);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!press(p) && Aura.aura(p) == afterHit, "Recovery and rest refuse a second press without charging");
				check(MasterForms.committed(p), "Guard, Aura Step and arts read the same commitment");
			});
			// Lifecycle: the record survives NBT and death; the movement owner and input epoch do not.
			arena(context, world, p -> {});
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(press(p), "Lifecycle test lunges"); long epoch = FormDash.view(p).epoch();
				var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess());
				p.saveWithoutId(saved);
				var loaded = new Reloaded(p.level());
				loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess(), saved.buildResult()));
				check(FormDash.data(loaded).equals(FormDash.data(p)), "Native NBT reload keeps learned forms, slot, rest and recovery");
				check(FormDash.view(loaded).epoch() == 0 && !FormDash.ownsMotion(loaded), "A reload cannot restore the movement owner or epoch");
				loaded.discard();
				long rest = FormDash.data(p).readyAt();
				p.hurtServer(p.level(), p.damageSources().genericKill(), Float.MAX_VALUE);
				p.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				var replacement = server.getPlayerList().getPlayer(p.getUUID());
				check(replacement != p && replacement != null && replacement.isAlive(), "Native respawn creates a replacement body");
				check(FormDash.data(replacement).knows(FormDashRules.CINDER_LUNGE) && FormDash.data(replacement).readyAt() == rest, "Death keeps the lesson and the rest");
				check(!FormDash.ownsMotion(replacement) && !FormDash.request(replacement, new FormDash.Action(WallTurnRules.PRESS, 0, epoch, ++sequence)),
					"The old body's packet cannot drive its replacement");
			});
			// Last, as a ward stays for the world: a loaded dungeon boundary refuses the first stretch at no cost.
			arena(context, world, p -> DungeonWards.remember(p.level(), () -> List.of(new BoundingBox(1, 100, -2, 4, 107, 2))));
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!WallTurn.footing(p, p.position().add(.75, 0, 0), DungeonWards.movementWard(p.level(), p.blockPosition())), "Footing inside a ward is refused");
				check(!press(p) && Aura.aura(p) == 160 && FormDash.data(p).readyAt() == 0, "A ward in the first stretch refuses the lunge at no cost");
			});
		} finally { target = null; }
	}
	private void arena(ClientGameTestContext context, TestSingleplayerContext world, Consumer<ServerPlayer> build) {
		fresh(context, world);
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			MasterForms.cancel(p);
			for (int x = -4; x <= 9; x++) for (int z = -4; z <= 4; z++) {
				p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
				for (int y = 100; y <= 105; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
			}
			build.accept(p);
			p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.setInvulnerableTime(0); p.clearFire(); p.fallDistance = 0;
			p.teleportTo(p.level(), .5, 100, .5, Set.of(), -90, 0, false); p.setDeltaMovement(Vec3.ZERO); p.setShiftKeyDown(false);
			p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
			p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(false, 0, 0, false, false));
			int both = FormDashRules.bit(FormDashRules.CINDER_LUNGE) | FormDashRules.bit(FormDashRules.REED_SLIP);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(both, FormDashRules.CINDER_LUNGE, 0, 0, 0));
		});
		context.waitTicks(4);
	}
	private boolean press(ServerPlayer p) { return FormDash.request(p, new FormDash.Action(WallTurnRules.PRESS, 0, FormDash.view(p).epoch(), ++sequence)); }
	private void fresh(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst();
			FormDash.request(p, new FormDash.Action(WallTurnRules.RELEASE, 0, FormDash.view(p).epoch(), ++sequence)); });
		context.waitTicks(1);
	}
}
