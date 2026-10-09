package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.client.MasterFormsClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** The lunge's cut lands, every use owns a punishable recovery, the shared rest blocks swapping, and Reed Slip follows the real strafe key. */
public final class FormDashCommitmentTest implements FabricClientGameTest {
	private long sequence = 100;
	private static LivingEntity target;
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.FormDashCommitmentTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				var p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -8; x <= 14; x++) for (int z = -8; z <= 8; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 105; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.EMBER).withClear(MastersRules.GALE));
			});
			// The real strafe key chooses the side; forward is never taken. It runs first: the client numbers its own input
			// from 1, so it must precede this test's server-side packets (numbered from 100) in the same input epoch.
			reset(context, world, FormDashRules.REED_SLIP);
			context.getInput().holdKey(o -> o.keyLeft);
			context.waitTicks(2);
			double[] before = world.getServer().computeOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); return new double[] {p.getX(), p.getZ()}; });
			context.getInput().pressKey(MasterFormsClient.mapping());
			context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.SLIP, 10);
			// The lane was fixed at acceptance; letting go now keeps walking from padding the distance during recovery.
			context.getInput().releaseKey(o -> o.keyLeft);
			context.waitTicks(FormDashRules.REED_TICKS + 1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Facing +X, the left strafe key is the -Z side.
				check(p.getZ() < before[1] - 2.2 && p.getX() <= before[0] + .05, "The held left strafe key slips to the left and never forward: " + p.position());
				check(Math.abs(Aura.aura(p) - 148) < .001, "The real key pays once");
			});
			reset(context, world, FormDashRules.CINDER_LUNGE);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var zombie = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
				check(zombie != null, "The target fixture exists");
				zombie.setNoAi(true); zombie.snapTo(7, 100, .5, 90, 0); p.level().addFreshEntity(zombie); target = zombie;
				check(press(p), "A planted lunge is accepted toward the target");
				check(MasterForms.committed(p) && !AuraGuard.guarding(p), "Commitment starts at the plant");
			});
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(target.getHealth() < target.getMaxHealth(), "The cut at the end of the lunge lands on the foe ahead");
				check(p.getX() > 4.5 && p.getX() < 6.6, "The lunge carried the body to the target: " + p.position());
				check(MasterForms.committed(p) && FormDash.data(p).recoveryUntil() > MasterForms.now(p), "The cut ends in an exposed recovery");
				check(!FormDash.request(p, new FormDash.Action(WallTurnRules.EQUIP, FormDashRules.REED_SLIP, FormDash.view(p).epoch(), ++sequence))
					&& FormDash.data(p).equipped() == FormDashRules.CINDER_LUNGE, "The other field form cannot be swapped in to skip the rest");
				check(!MasterForms.request(p, new MasterForms.Action(WallTurnRules.EQUIP, MasterForms.view(p).epoch(), ++sequence))
					&& MasterForms.data(p).equipped() == 0, "Wall Turn cannot be swapped in to skip the rest either");
				target.discard(); release(p);
			});
			context.waitTicks(FormDashRules.CINDER_RECOVERY + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!MasterForms.committed(p), "Recovery ends on time; only the shared rest remains");
				check(!press(p) && Aura.aura(p) == 135, "The rest refuses a second lunge without charging");
			});
			// Reed Slip with no move key steps straight back, still facing, and has no invulnerability.
			reset(context, world, FormDashRules.REED_SLIP);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(press(p) && Aura.aura(p) == 148, "A slip pays 12 Aura");
				p.setInvulnerableTime(0); float health = p.getHealth();
				var source = p.damageSources().generic();
				// Control: the Aura armour share this exact hit is owed, measured before it lands.
				double owed = AuraArmour.share(p, source, 2) * AuraRules.ARMOUR_COST_PER_POINT * WayEffects.armourCost(p);
				p.hurtServer(p.level(), source, 2);
				check(p.getHealth() < health, "A hit during the slip lands: there are no invulnerability frames");
				check(Math.abs(Aura.aura(p) - (148 - owed)) < .001, "The hit costs only the armour's share and refunds nothing: " + Aura.aura(p) + " owed " + owed);
				check(!FormDash.ownsMotion(p) && FormDash.data(p).recoveryUntil() >= MasterForms.now(p) + FormDashRules.REED_RECOVERY, "The hit ends the slip into its recovery");
			});
			reset(context, world, FormDashRules.REED_SLIP);
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Back slip accepted"); });
			context.waitTicks(FormDashRules.REED_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getX() < -1.7 && p.getX() > -2.1 && Math.abs(p.getZ() - .5) < .05, "No intent steps 2.5 blocks straight back: " + p.position());
				check(Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot() + 90)) < 1, "The slip keeps the facing");
				check(MasterForms.committed(p) && FormDash.data(p).drilled(FormDashRules.REED_SLIP), "A clean slip still owns its recovery");
			});
			// A foe three blocks ahead stops the lunge short of passing through it, and the cut lands.
			reset(context, world, FormDashRules.CINDER_LUNGE);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var husk = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
				check(husk != null, "The close target fixture exists");
				husk.setNoAi(true); husk.snapTo(3.5, 100, .5, 90, 0); p.level().addFreshEntity(husk); target = husk;
				check(press(p), "A lunge toward a close foe is accepted");
			});
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 2);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(target.getHealth() < target.getMaxHealth(), "The cut lands on a foe three blocks ahead");
				check(p.getBoundingBox().maxX <= target.getBoundingBox().minX + .001 && p.getX() > 1.5, "The lunge stops at the foe instead of passing through: " + p.position());
				target.discard();
			});
			// Out of any fight, after recovery but inside the shared rest, neither direction of swap is accepted.
			reset(context, world, FormDashRules.REED_SLIP);
			context.waitTicks(AuraRules.COMBAT_TICKS + 2);
			world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); check(press(p), "Rest-window slip accepted"); });
			context.waitTicks(FormDashRules.REED_TICKS + FormDashRules.REED_RECOVERY + 2);
			long restLeft = world.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(!MasterForms.committed(p) && !Aura.inFight(p) && FormDash.data(p).readyAt() > MasterForms.now(p) + 2, "Only the shared rest remains");
				check(!FormDash.request(p, new FormDash.Action(WallTurnRules.EQUIP, FormDashRules.CINDER_LUNGE, FormDash.view(p).epoch(), ++sequence))
					&& FormDash.data(p).equipped() == FormDashRules.REED_SLIP, "The rest refuses swapping Reed Slip for Cinder Lunge");
				check(!MasterForms.request(p, new MasterForms.Action(WallTurnRules.EQUIP, MasterForms.view(p).epoch(), ++sequence))
					&& MasterForms.data(p).equipped() == 0 && FormDash.data(p).equipped() == FormDashRules.REED_SLIP, "The rest refuses swapping in Wall Turn");
				return FormDash.data(p).readyAt() - MasterForms.now(p);
			});
			context.waitTicks((int) restLeft + 1);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				// Control: the same swaps are accepted once the rest has run, so the rest is what refused them.
				check(FormDash.request(p, new FormDash.Action(WallTurnRules.EQUIP, FormDashRules.CINDER_LUNGE, FormDash.view(p).epoch(), ++sequence))
					&& FormDash.data(p).equipped() == FormDashRules.CINDER_LUNGE, "After the rest the field forms swap");
				check(MasterForms.request(p, new MasterForms.Action(WallTurnRules.EQUIP, MasterForms.view(p).epoch(), ++sequence))
					&& MasterForms.data(p).equipped() == MasterForms.WALL_TURN && FormDash.data(p).equipped() == 0, "After the rest Wall Turn takes the slot");
				// Wall Turn's own spent rest refuses the field forms the same way.
				var wall = MasterForms.data(p);
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, MasterForms.WALL_TURN, MasterForms.now(p) + WallTurnRules.REST_TICKS, false, wall.practiced()));
				check(!FormDash.request(p, new FormDash.Action(WallTurnRules.EQUIP, FormDashRules.REED_SLIP, FormDash.view(p).epoch(), ++sequence))
					&& FormDash.data(p).equipped() == 0 && MasterForms.data(p).equipped() == MasterForms.WALL_TURN, "Wall Turn's rest refuses swapping in a field form");
			});
		} finally { target = null; }
	}
	private void reset(ClientGameTestContext context, TestSingleplayerContext world, int form) {
		world.getServer().runOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			if (sequence > 100) release(p); // before the first server packet the client owns the sequence
			MasterForms.cancel(p);
			p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.setInvulnerableTime(0);
			p.teleportTo(p.level(), .5, 100, .5, Set.of(), -90, 0, false); p.setDeltaMovement(Vec3.ZERO); p.setShiftKeyDown(false);
			p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.SOVEREIGN, 4500, 160, 0));
			p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, 0, 0, false, false));
			int both = FormDashRules.bit(FormDashRules.CINDER_LUNGE) | FormDashRules.bit(FormDashRules.REED_SLIP);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(both, form, 0, 0, 0));
		});
		context.waitTicks(4);
	}
	private boolean press(ServerPlayer p) { return FormDash.request(p, new FormDash.Action(WallTurnRules.PRESS, 0, FormDash.view(p).epoch(), ++sequence)); }
	private void release(ServerPlayer p) { FormDash.request(p, new FormDash.Action(WallTurnRules.RELEASE, 0, FormDash.view(p).epoch(), ++sequence)); }
}
