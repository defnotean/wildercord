package dev.wildercord.cast;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** In-world checks for runes not named by the older focused gameplay suites. */
public class WildercordRuneGapsTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			context.waitTicks(35);
			world.getServer().runCommand("fill -8 159 -8 8 159 8 minecraft:stone");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
				BlockPos center = new BlockPos(4, 161, 0);
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
					player.level().setBlockAndUpdate(center.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				var node = SpellCompiler.compile(List.of(Runes.BEAM, Runes.EXCAVATE)).root().groups.getFirst().effects.getFirst();
				Effects.apply(new Cast(player), node, new Cast.Hit(List.of(), Vec3.atCenterOf(center), player.getLookAngle(),
					player.position(), center, Direction.UP, false));
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
					check(player.level().getBlockState(center.offset(x, 0, z)).isAir(), "Excavate left stone in its 3x3 face");

				player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, Runes.TWIN_STAR.id());
				var foreign = SpellCompiler.compile(List.of(Runes.SELF, Runes.PHANTOM)).root().groups.getFirst().effects.getFirst();
				Effects.apply(new Cast(player), foreign, new Cast.Hit(List.of(player), player.position(), player.getLookAngle(), player.position(), null, null, true));
				check(!player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "A foreign innate must not apply in Survival");
				for (RuneDef rune : List.of(Runes.TWIN_STAR, Runes.GALE_MANTLE, Runes.FORTUNE, Runes.STORMHEART, Runes.PHANTOM)) {
					// Each scenario represents a caster who awakened this particular heart.
					player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, rune.id());
					var effect = SpellCompiler.compile(List.of(Runes.SELF, rune)).root().groups.getFirst().effects.getFirst();
					Effects.apply(new Cast(player), effect, new Cast.Hit(List.of(player), player.position(), player.getLookAngle(),
						player.position(), null, null, true));
				}
				check(player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "Phantom should hide its caster");
				check(!player.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.Mannequin.class,
					player.getBoundingBox().inflate(4), e -> e.entityTags().contains("wildercord.afterimage")).isEmpty(),
					"Phantom should leave an afterimage");
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, Runes.TWIN_STAR.id());
				check(Innates.consumeTwin(player), "Twin Star should arm the next cast");
				check(!Innates.consumeTwin(player), "Twin Star should duplicate only one cast");
				player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, Runes.FORTUNE.id());
				int lucky = 0;
				for (int i = 0; i < 50; i++) if (Innates.fortune(new Cast(player), player) > 1) lucky++;
				check(lucky > 0, "Fortune should sometimes double a hit while active");
			});
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, Runes.STORMHEART.id());
				Husk husk = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
				check(husk != null, "Could not spawn Stormheart attacker");
				husk.snapTo(2.5, 160, 0.5, 0, 0);
				husk.setNoAi(true);
				husk.addTag("wildercord.rolled");
				player.level().addFreshEntity(husk);
				float before = husk.getHealth();
				player.hurtServer(player.level(), player.damageSources().mobAttack(husk), 3);
				check(husk.getHealth() < before, "Stormheart should strike the creature that hurt its caster");
			});
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
