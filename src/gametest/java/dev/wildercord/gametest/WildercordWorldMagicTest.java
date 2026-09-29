package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.cast.Thaws;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Magic that changes the world, checked in a real world: frost on water makes frosted ice, fire by
 * grass lights fire (with fire spreading on), storm into water shocks a husk standing in that water,
 * wind knocks an arrow out of the air, a Grow beside a Rampart leaves the wall standing, and a spell where
 * the caster may not build changes nothing. Icepath's ice is written down to thaw (it never melts in the
 * dark), a Rampart never rises over a Light spell's light, its blocks are written down to come down even
 * after a crash, and one left past its time is taken down as soon as its ground is loaded; blown up, it drops
 * nothing. The world runes keep their own rules: Icepath never freezes round a swimmer, Harvest replants each
 * crop with one of its own seeds, Grow keeps to the cast's block budget, Blink never lands in lava, and Banish
 * never leaves a creature standing on the ground out over a drop.
 *
 * <p>Spells are applied straight to a hit at a chosen point ({@link CastEngine#onHit}), the same call
 * every shape ends in, so each check is exact. A singleplayer world has no spawn protection (only a
 * dedicated server does), so protected ground is stood in for the two ways {@code Casters.mayEdit}
 * refuses: a claim (a block-break listener saying no, as claim mods do) and a caster who can't build
 * (Adventure mode). A monster's spell is checked too: it never changes blocks.</p>
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY} skip it.</p>
 */
public class WildercordWorldMagicTest implements FabricClientGameTest {
	/** The stand-in claim: while set, breaking (so changing) any block inside it is refused. */
	private static volatile AABB claim;
	private static boolean listening;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!listening) {
			listening = true;
			PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
				AABB box = claim;
				return box == null || !box.contains(Vec3.atCenterOf(pos));
			});
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			var server = world.getServer();
			List<String> failures = new ArrayList<>();

			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				player.setGameMode(GameType.SURVIVAL);
				ServerLevel level = player.level();
				level.getGameRules().set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 128, s);
			});
			context.waitTicks(2);

			// Frost on water: its surface freezes into frosted ice.
			String frost = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos pool = site(player, 12, 0);
				pool(player.level(), pool, 1);
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				BlockState state = player.level().getBlockState(pool.below());
				return state.is(Blocks.FROSTED_ICE) ? null : "frost on water should freeze it into frosted ice (found " + state + ")";
			});
			note(failures, frost);

			// Icepath: its frosted ice is written down to thaw, since in the dark it would never melt.
			String icepath = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos pool = site(player, 24, 0);
				pool(player.level(), pool, 1);
				apply(player, List.of(Runes.TOUCH, Runes.ICEPATH), Vec3.atCenterOf(pool), List.of());
				BlockPos ice = pool.below();
				if (!player.level().getBlockState(ice).is(Blocks.FROSTED_ICE)) {
					return "Icepath should freeze the pool into frosted ice (found " + player.level().getBlockState(ice) + ")";
				}
				return Thaws.waiting(player.level(), ice) ? null : "Icepath's frosted ice should be written down to thaw";
			});
			note(failures, icepath);

			// Icepath never freezes round a swimmer (frost walker's rule): the water a husk stands in stays water.
			String swimmer = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 24, 12);
				pool(level, pool, 1);
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				// On the pool's floor, in the middle: its body fills the water block the ice would take.
				mob.snapTo(pool.getX() + 0.5, pool.getY() - 1, pool.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				apply(player, List.of(Runes.TOUCH, Runes.ICEPATH), Vec3.atCenterOf(pool), List.of());
				BlockState under = level.getBlockState(pool.below());
				BlockState beside = level.getBlockState(pool.below().offset(2, 0, 0));
				mob.discard();
				if (under.is(Blocks.FROSTED_ICE)) {
					return "Icepath shouldn't freeze the water a creature is in: it would be stuck in the ice";
				}
				return beside.is(Blocks.FROSTED_ICE) ? null : "Icepath should still freeze the water round a swimmer (found " + beside + ")";
			});
			note(failures, swimmer);

			// Harvest replants each crop from its own drops: one seed goes back into the ground, never a free one. The
			// same loot is rolled twice over (the wheat's random sequence reset between): once here, to count the seeds
			// 25 ripe wheat drop, then by the Harvest.
			String harvest = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos field = site(player, 24, 24);
				BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(7);
				farm(level, field, 2, ripe);
				Identifier sequence = Identifier.withDefaultNamespace("blocks/wheat");
				s.getRandomSequences().reset(sequence, 5L);
				int dropped = 0;
				for (int i = 0; i < 25; i++) {
					for (ItemStack stack : Block.getDrops(ripe, level, field, null, player, ItemStack.EMPTY)) {
						dropped += stack.is(Items.WHEAT_SEEDS) ? stack.getCount() : 0;
					}
				}
				s.getRandomSequences().reset(sequence, 5L);
				apply(player, List.of(Runes.TOUCH, Runes.HARVEST), Vec3.atBottomCenterOf(field).add(0, 0.5, 0), List.of());
				int seeds = 0;
				for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(field).inflate(5))) {
					seeds += item.getItem().is(Items.WHEAT_SEEDS) ? item.getItem().getCount() : 0;
					item.discard();
				}
				int replanted = count(level, field, 2, state -> state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == 0);
				if (replanted != 25) {
					return "Harvest should replant every crop it cut (" + replanted + " of 25 replanted)";
				}
				return seeds == dropped - 25 ? null
					: "Harvest should replant each crop with one of its own seeds (" + dropped + " dropped, " + seeds + " left on the ground)";
			});
			note(failures, harvest);

			// Grow keeps to the cast's block budget, as every change to the world does: four 3x3 fields of fresh wheat
			// grown by one cast, and whatever the budget doesn't reach stays a seedling.
			String grow = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				List<BlockPos> fields = List.of(site(player, 12, -12), site(player, -12, -12), site(player, 24, -12), site(player, -24, -12));
				for (BlockPos field : fields) {
					farm(level, field, 1, ((CropBlock) Blocks.WHEAT).getStateForAge(0));
				}
				Cast cast = new Cast(player);
				SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.GROW)).root().groups.getFirst();
				for (BlockPos field : fields) {
					Vec3 at = Vec3.atBottomCenterOf(field).add(0, 0.5, 0);
					CastEngine.onHit(cast, group, new Cast.Hit(List.of(), at, new Vec3(1, 0, 0), player.position(), null, null, false), null);
				}
				int seedlings = 0;
				for (BlockPos field : fields) {
					seedlings += count(level, field, 1, state -> state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == 0);
				}
				int left = Math.max(0, fields.size() * 9 - dev.wildercord.config.Config.get().maxBlocks());
				return seedlings == left ? null
					: "one cast's Grow should grow no more blocks than its budget (" + seedlings + " seedlings left, expected " + left + ")";
			});
			note(failures, grow);

			// Blink never lands in lava: aimed at a lava pool (whose floor is still where the spell landed), it stays put.
			String blink = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pit = site(player, 12, 24);
				pool(level, pit, 1);
				// The water out first: lava poured in beside it would turn to obsidian, solid ground Blink may land on.
				for (BlockPos pos : BlockPos.betweenClosed(pit.offset(-2, -1, -2), pit.offset(2, -1, 2))) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
				for (BlockPos pos : BlockPos.betweenClosed(pit.offset(-2, -1, -2), pit.offset(2, -1, 2))) {
					level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
				}
				for (BlockPos pos : BlockPos.betweenClosed(pit.offset(-2, -1, -2), pit.offset(2, -1, 2))) {
					if (!level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
						return "the test's lava pool should be lava (" + pos + " is " + level.getBlockState(pos) + ")";
					}
				}
				Vec3 before = player.position();
				apply(player, List.of(Runes.TOUCH, Runes.BLINK), Vec3.atCenterOf(pit.below()), List.of());
				Vec3 landed = player.position();
				for (BlockPos pos : BlockPos.betweenClosed(pit.offset(-2, -1, -2), pit.offset(2, -1, 2))) {
					level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
				}
				if (landed.distanceTo(before) > 0.01) {
					player.teleportTo(level, before.x, before.y, before.z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
					player.clearFire();
					return "Blink aimed into lava shouldn't take you there (it took you to " + landed + ")";
				}
				return null;
			});
			note(failures, blink);

			// Banish never drops a creature standing on the ground into a chasm or the void: on a lone pillar high over
			// open air, with no ground within reach anywhere it could go, it stays where it is.
			String banish = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos top = site(player, -24, 24).above(40);
				level.setBlockAndUpdate(top.below(), Blocks.STONE.defaultBlockState());
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				mob.snapTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				// Standing on the pillar (a creature without AI never moves, so never finds its footing itself).
				mob.setOnGround(true);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				Vec3 before = mob.position();
				apply(player, List.of(Runes.TOUCH, Runes.BANISH), mob.getBoundingBox().getCenter(), List.of(mob));
				Vec3 after = mob.position();
				mob.discard();
				level.setBlockAndUpdate(top.below(), Blocks.AIR.defaultBlockState());
				return after.distanceTo(before) < 0.01 ? null
					: "Banish shouldn't put a creature standing on the ground out over a drop with nowhere to land (it went to " + after + ")";
			});
			note(failures, banish);

			// Fire by grass, with fire spreading on: the grass catches.
			String fire = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos patch = site(player, 0, 12);
				meadow(player.level(), patch);
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atBottomCenterOf(patch), List.of());
				int fires = count(player.level(), patch, 2, state -> state.is(BlockTags.FIRE));
				douse(player.level(), patch, 3);
				return fires > 0 ? null : "fire by grass should set it alight";
			});
			note(failures, fire);

			// Storm into water: a husk standing in the same water (not struck by the spell itself) is shocked.
			UUID husk = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, -12, 0);
				pool(level, pool, 2);
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				// On the pool's floor, two blocks down: it stands in the water.
				mob.snapTo(pool.getX() + 0.5, pool.getY() - 2, pool.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				mob.setPersistenceRequired();
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				return mob.getUUID();
			});
			// Let it tick, so it knows it's standing in water.
			context.waitTicks(5);
			String storm = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				if (!(level.getEntity(husk) instanceof LivingEntity target) || !target.isAlive()) {
					return "the husk in the pool is gone";
				}
				if (!target.isInWater()) {
					return "the husk should be standing in the pool's water";
				}
				float before = target.getHealth();
				BlockPos pool = site(player, -12, 0);
				// Into the water two blocks from the husk.
				apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(pool.offset(2, 0, 0)), List.of());
				float after = target.isAlive() ? target.getHealth() : 0.0F;
				target.discard();
				return after < before ? null : "storm into water should shock a husk standing in it (health " + before + " -> " + after + ")";
			});
			note(failures, storm);

			// Wind: an arrow in flight near where it lands is knocked away, the way the wind blows.
			String wind = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos spot = site(player, 0, -12);
				Vec3 at = Vec3.atBottomCenterOf(spot).add(0, 1.5, 0);
				Entity arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
				arrow.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
				arrow.setNoGravity(true);
				arrow.setDeltaMovement(-1.5, 0, 0);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				arrow.addTag("wildercord.rolled");
				level.addFreshEntity(arrow);
				// The gust comes from 3 blocks to the west, so it blows east (+x), against the arrow.
				apply(player, List.of(Runes.TOUCH, Runes.PUSH), at, List.of(), at.add(-3, 0, 0));
				double x = arrow.getDeltaMovement().x;
				arrow.discard();
				return x > 0.3 ? null : "wind should knock an arrow flying at it back the other way (its speed along x is " + x + ")";
			});
			note(failures, wind);

			// A Grow beside a Rampart leaves the wall standing: a spell only asking whether it may change a block
			// (offered to claims as a break) must never set off the handler that takes a Rampart down.
			// A light (as a Light spell leaves) where the wall will rise: the wall goes round it, never over it.
			BlockPos wallSite = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos site = site(player, 0, 24);
				meadow(player.level(), site);
				player.level().setBlockAndUpdate(site.above(), Blocks.LIGHT.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.RAMPART), Vec3.atBottomCenterOf(site), List.of());
				return site;
			});
			context.waitTicks(8);
			String rampart = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				int before = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				apply(player, List.of(Runes.TOUCH, Runes.GROW), Vec3.atBottomCenterOf(wallSite), List.of());
				int after = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				if (before == 0) {
					return "a Rampart should raise a wall of packed mud";
				}
				if (!level.getBlockState(wallSite.above()).is(Blocks.LIGHT)) {
					return "a Rampart shouldn't rise over a light (found " + level.getBlockState(wallSite.above()) + "): when it crumbled it would leave the light for good";
				}
				for (BlockPos pos : BlockPos.betweenClosed(wallSite.offset(-3, -3, -3), wallSite.offset(3, 3, 3))) {
					if (level.getBlockState(pos).is(Blocks.PACKED_MUD) && !TemporaryBlocks.recorded(level, pos)) {
						return "every Rampart block should be written down, to come down even if the server doesn't stop cleanly";
					}
				}
				level.setBlockAndUpdate(wallSite.above(), Blocks.AIR.defaultBlockState());
				return after == before ? null : "a Grow beside a Rampart shouldn't take the wall down (" + before + " blocks, then " + after + ")";
			});
			note(failures, rampart);

			// An explosion breaks the wall without a block of packed mud to show for it: a spell's blocks drop nothing, however they go.
			String blast = server.computeOnServer(s -> {
				ServerLevel level = player(s).level();
				int standing = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				// A TNT blast drops every block it breaks (no decay), so each Rampart block would show.
				level.explode(null, wallSite.getX() + 0.5, wallSite.getY() + 1.0, wallSite.getZ() + 0.5, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
				int left = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				int dropped = 0;
				for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(wallSite).inflate(8))) {
					if (item.getItem().is(net.minecraft.world.item.Items.PACKED_MUD)) {
						dropped += item.getItem().getCount();
					}
					item.discard();
				}
				if (left >= standing) {
					return "the explosion should break some of the Rampart (" + standing + " blocks before, " + left + " after)";
				}
				return dropped == 0 ? null : "a Rampart blown up shouldn't drop packed mud (" + dropped + " dropped)";
			});
			note(failures, blast);

			// A spell's block left past its time (by a server that stopped without warning, say) comes down once its ground is loaded.
			BlockPos leftover = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos at = site(player, -24, 0);
				level.setBlockAndUpdate(at, Blocks.PACKED_MUD.defaultBlockState());
				TemporaryBlocks.put(level, at, Blocks.PACKED_MUD.defaultBlockState(), Blocks.AIR.defaultBlockState(), level.getGameTime() - 200);
				return at;
			});
			context.waitTicks(30);
			String safetyNet = server.computeOnServer(s -> {
				ServerLevel level = player(s).level();
				if (!level.getBlockState(leftover).isAir()) {
					return "a spell's block left past its time should be taken down (found " + level.getBlockState(leftover) + ")";
				}
				return TemporaryBlocks.recorded(level, leftover) ? "a block taken down should be crossed off" : null;
			});
			note(failures, safetyNet);

			// Protected ground: frost in a claim freezes nothing, fire from a caster who can't build lights nothing,
			// and a monster's frost never changes blocks.
			String protectedGround = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 12, 12);
				pool(level, pool, 1);
				claim = new AABB(pool).inflate(4);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				} finally {
					claim = null;
				}
				if (count(level, pool, 3, state -> state.is(Blocks.FROSTED_ICE)) > 0) {
					return "frost inside a claim shouldn't freeze the water";
				}
				BlockPos patch = site(player, -12, 12);
				meadow(level, patch);
				// An unlit campfire too: Rune Seals' campfire lighting keeps to the same rules.
				level.setBlockAndUpdate(patch, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
				player.setGameMode(GameType.ADVENTURE);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atBottomCenterOf(patch), List.of());
				} finally {
					player.setGameMode(GameType.SURVIVAL);
				}
				int fires = count(level, patch, 2, state -> state.is(BlockTags.FIRE));
				boolean campfire = level.getBlockState(patch).getOptionalValue(net.minecraft.world.level.block.CampfireBlock.LIT).orElse(false);
				douse(level, patch, 3);
				level.setBlockAndUpdate(patch, Blocks.AIR.defaultBlockState());
				if (fires > 0) {
					return "fire from a caster who can't build there shouldn't light anything";
				}
				if (campfire) {
					return "fire from a caster who can't build there shouldn't light a campfire";
				}
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				mob.snapTo(pool.getX() - 3.5, pool.getY() + 1, pool.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				Cast monster = new Cast(mob);
				SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.FROST)).root().groups.getFirst();
				Vec3 at = Vec3.atCenterOf(pool);
				CastEngine.onHit(monster, group, new Cast.Hit(List.of(), at, new Vec3(1, 0, 0), mob.position(), null, null, false), null);
				mob.discard();
				if (count(level, pool, 3, state -> state.is(Blocks.FROSTED_ICE)) > 0) {
					return "a monster's frost shouldn't freeze the water";
				}
				return null;
			});
			note(failures, protectedGround);

			// Putting something into the air of a claim is asked of it too, not only taking something away: Glimmer's
			// lichen grows on a bare stone outside the claim, and not on one inside it.
			String placing = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos open = site(player, 18, 18);
				BlockPos claimed = site(player, -18, 18);
				level.setBlockAndUpdate(open, Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(claimed, Blocks.STONE.defaultBlockState());
				apply(player, List.of(Runes.TOUCH, Runes.GLIMMER), Vec3.atBottomCenterOf(open.above()), List.of());
				claim = new AABB(claimed).inflate(4);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.GLIMMER), Vec3.atBottomCenterOf(claimed.above()), List.of());
				} finally {
					claim = null;
				}
				int outside = count(level, open, 2, state -> state.is(Blocks.GLOW_LICHEN));
				int inside = count(level, claimed, 2, state -> state.is(Blocks.GLOW_LICHEN));
				for (BlockPos stone : List.of(open, claimed)) {
					for (BlockPos pos : BlockPos.betweenClosed(stone.offset(-2, -2, -2), stone.offset(2, 2, 2))) {
						if (level.getBlockState(pos).is(Blocks.GLOW_LICHEN)) {
							level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
						}
					}
					level.setBlockAndUpdate(stone, Blocks.AIR.defaultBlockState());
				}
				if (outside == 0) {
					return "Glimmer should grow lichen on a bare stone outside a claim";
				}
				return inside == 0 ? null : "Glimmer shouldn't grow lichen into the air inside a claim (" + inside + " grew)";
			});
			note(failures, placing);

			if (!failures.isEmpty()) {
				throw new AssertionError("Magic that changes the world went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			claim = null;
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void note(List<String> failures, String failure) {
		if (failure != null) {
			failures.add(failure);
		}
	}

	/** A spot on the ground {@code dx}, {@code dz} blocks from the player: the air block just above the surface. */
	private static BlockPos site(ServerPlayer player, int dx, int dz) {
		ServerLevel level = player.level();
		BlockPos at = player.blockPosition().offset(dx, 0, dz);
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
		return new BlockPos(at.getX(), top, at.getZ());
	}

	/** Digs a 5x5 pool {@code depth} deep, walled in stone, whose middle surface block is {@code centre.below()} (open air above). */
	private static void pool(ServerLevel level, BlockPos centre, int depth) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
				for (int dy = 1; dy <= depth + 1; dy++) {
					boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == depth + 1;
					level.setBlockAndUpdate(centre.offset(dx, -dy, dz), rim ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
				}
			}
		}
	}

	/** A 5x5 patch of grass blocks with short grass growing on them, around {@code centre} (the air above the surface). */
	private static void meadow(ServerLevel level, BlockPos centre) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.SHORT_GRASS.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/**
	 * A square of farmland {@code 2r + 1} across with {@code crop} growing on it, around {@code centre} (the air above
	 * the surface), and open air over it. Set without block updates, so no crop pops off for want of light under a
	 * tree the site happens to be by.
	 */
	private static void farm(ServerLevel level, BlockPos centre, int r, BlockState crop) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				level.setBlock(centre.offset(dx, -1, dz), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(centre.offset(dx, 0, dz), crop, Block.UPDATE_CLIENTS);
				level.setBlock(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** How many blocks within {@code r} of {@code centre} (in a cube) pass {@code test}. */
	private static int count(ServerLevel level, BlockPos centre, int r, java.util.function.Predicate<BlockState> test) {
		int n = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (test.test(level.getBlockState(pos))) {
				n++;
			}
		}
		return n;
	}

	/** Puts out any fire around {@code centre}, so it doesn't spread through the rest of the test. */
	private static void douse(ServerLevel level, BlockPos centre, int r) {
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (level.getBlockState(pos).is(BlockTags.FIRE)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		}
	}

	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck) {
		apply(player, runes, at, struck, player.position());
	}

	/** Lands a spell's first group on {@code at}, as the player's, with {@code origin} where it came from. */
	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck, Vec3 origin) {
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		Vec3 dir = at.subtract(origin).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : at.subtract(origin).normalize();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(struck, at, dir, origin, null, null, false), null);
	}
}
