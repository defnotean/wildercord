package dev.wildercord.cast.packs;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Single-player, native client: the Tide pack (fx-fish) cast for real through the spell caster, one rune at a time,
 * on a stone stage high above spawn. Every case builds what its rune works on, casts {@code Self} with that rune, and
 * checks what the world or the caster now holds.
 */
public final class TidePackClientTest implements FabricClientGameTest {
	private static final int Y = 150;
	private static final int SLOT = 0;

	/** One rune: what to build, and what must be true after the cast (null when it is, or why not). */
	private record Case(RuneDef rune, Consumer<ServerPlayer> setup, Function<ServerPlayer, String> outcome) {}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			var server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule fall_damage false");
			server.runCommand("gamerule random_tick_speed 0");
			server.runCommand("weather clear");
			List<String> failures = new ArrayList<>();
			for (Case c : cases()) {
				String[] problem = {null};
				server.runOnServer(s -> {
					ServerPlayer p = player(s);
					stage(p);
					c.setup().accept(p);
					Component refused = SpellCaster.edit(p, SLOT, List.of(Runes.SELF.id(), c.rune().id()));
					if (refused != null) {
						problem[0] = "edit refused: " + refused.getString();
						return;
					}
					Spellbooks.setReadyAt(p, SLOT, 0);
					SpellCaster.cast(p, SLOT);
				});
				context.waitTicks(6);
				server.runOnServer(s -> {
					if (problem[0] == null) {
						problem[0] = c.outcome().apply(player(s));
					}
				});
				if (problem[0] != null) {
					failures.add(c.rune().path() + ": " + problem[0]);
				}
				context.waitTicks(20);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("Tide pack casts failed:\n  " + String.join("\n  ", failures));
			}
			dev.wildercord.Wildercord.LOGGER.info("TIDE_PACK_CLIENT passed {} casts", cases().size());
		}
	}

	private static List<Case> cases() {
		return List.of(
			new Case(Runes.BRIMMING, p -> set(p, 2, 0, 0, Blocks.CAULDRON.defaultBlockState()), p -> {
				BlockState s = at(p, 2, 0, 0);
				return s.is(Blocks.WATER_CAULDRON) && s.getValue(LayeredCauldronBlock.LEVEL) == 3 ? null : "cauldron is " + s;
			}),
			new Case(Runes.SLUICE, p -> set(p, -2, 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)), p -> {
				BlockState s = at(p, -2, 0, 0);
				return s.is(Blocks.CAMPFIRE) && !s.getValue(CampfireBlock.LIT) ? null : "campfire is " + s;
			}),
			new Case(Runes.SOAK_THROUGH, p -> {
				set(p, 1, 0, 1, Blocks.CONCRETE_POWDER.white().defaultBlockState());
				set(p, -1, -1, 0, Blocks.DIRT.defaultBlockState());
			}, p -> at(p, 1, 0, 1).is(Blocks.CONCRETE.white()) && at(p, -1, -1, 0).is(Blocks.MUD) ? null
				: "powder " + at(p, 1, 0, 1) + ", dirt " + at(p, -1, -1, 0)),
			new Case(Runes.WRING, p -> set(p, 1, -1, -1, Blocks.WATER.defaultBlockState()),
				p -> at(p, 1, -1, -1).isAir() ? null : "pool is " + at(p, 1, -1, -1)),
			new Case(Runes.CORAL_MEND, p -> {
				set(p, 1, -1, 1, Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState());
				set(p, 2, -1, 1, Blocks.WATER.defaultBlockState());
			}, p -> at(p, 1, -1, 1).is(Blocks.BRAIN_CORAL_BLOCK) ? null : "coral is " + at(p, 1, -1, 1)),
			new Case(Runes.NEST_TEND, p -> {
				set(p, 1, -1, 0, Blocks.SAND.defaultBlockState());
				set(p, 1, 0, 0, Blocks.TURTLE_EGG.defaultBlockState());
			}, p -> {
				BlockState s = at(p, 1, 0, 0);
				return s.is(Blocks.TURTLE_EGG) && s.getValue(TurtleEggBlock.HATCH) == 1 ? null : "egg is " + s;
			}),
			new Case(Runes.KELPSONG, p -> set(p, -1, -1, -1, Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)), p -> {
				BlockState s = at(p, -1, -1, -1);
				return s.is(Blocks.SEA_PICKLE) && s.getValue(SeaPickleBlock.PICKLES) == 2 ? null : "pickle is " + s;
			}),
			new Case(Runes.DEWCATCH, p -> {
				p.getInventory().clearContent();
				p.getInventory().setItem(5, new ItemStack(Items.GLASS_BOTTLE, 2));
			}, p -> {
				int water = 0, empty = 0;
				for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
					ItemStack st = p.getInventory().getItem(i);
					if (st.is(Items.GLASS_BOTTLE)) empty += st.getCount();
					PotionContents potion = st.get(DataComponents.POTION_CONTENTS);
					if (st.is(Items.POTION) && potion != null && potion.is(Potions.WATER)) water += st.getCount();
				}
				return water == 2 && empty == 0 ? null : water + " water bottles, " + empty + " empty";
			}),
			new Case(Runes.SPRING_DRAW, p -> {
				p.getInventory().clearContent();
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
			}, p -> p.getMainHandItem().is(Items.WATER_BUCKET) ? null : "hand holds " + p.getMainHandItem()),
			new Case(Runes.AIR_POCKET, p -> p.setAirSupply(5),
				p -> p.getAirSupply() >= p.getMaxAirSupply() - 10 ? null : "air " + p.getAirSupply()),
			new Case(Runes.SEA_BREEZE, p -> p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 2400, 0)),
				p -> p.hasEffect(MobEffects.MINING_FATIGUE) ? "mining fatigue stayed" : null),
			new Case(Runes.BAIT_BLESSING, p -> { }, p -> p.hasEffect(MobEffects.LUCK) ? null : "no Luck"),
			new Case(Runes.PEARL_SIGHT, p -> { }, p -> p.hasEffect(MobEffects.CONDUIT_POWER) ? null : "no Conduit Power"),
			new Case(Runes.SHELLBACK, p -> { }, p -> p.hasEffect(MobEffects.RESISTANCE) ? null : "no Resistance"),
			new Case(Runes.SKATERS_EDGE, p -> { }, p -> p.hasEffect(MobEffects.SPEED) ? null : "no Speed"),
			new Case(Runes.INKVEIL, p -> { }, p -> p.hasEffect(MobEffects.INVISIBILITY) ? null : "no Invisibility"),
			new Case(Runes.DIVERS_HANDS, p -> { }, p -> {
				double v = p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
				return v >= 0.99 ? null : "submerged mining speed " + v;
			})
		);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** A clean two-deep stone floor at y 148-149 with air above, the player standing in the middle at y 150. */
	private static void stage(ServerPlayer p) {
		ServerLevel level = p.level();
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				level.setBlock(new BlockPos(x, Y - 2, z), Blocks.STONE.defaultBlockState(), 2);
				level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), 2);
				for (int y = Y; y <= Y + 4; y++) {
					level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		p.setGameMode(GameType.CREATIVE);
		p.teleportTo(level, 0.5, Y, 0.5, java.util.Set.of(), 0.0F, 0.0F, false);
		p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		p.removeAllEffects();
		p.setAirSupply(p.getMaxAirSupply());
		p.setAttached(dev.wildercord.player.WildercordAttachments.CIRCLES, 18);
		Spellbooks.setCord(p, new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
		List<String> known = new ArrayList<>(List.of(Runes.SELF.id()));
		cases().forEach(c -> known.add(c.rune().id()));
		Spellbooks.set(p, new dev.wildercord.player.Spellbook(known, List.of(), 0, true));
		Spellbooks.setMana(p, 200);
	}

	private static void set(ServerPlayer p, int dx, int dy, int dz, BlockState state) {
		p.level().setBlock(new BlockPos(dx, Y + dy, dz), state, 2);
	}

	private static BlockState at(ServerPlayer p, int dx, int dy, int dz) {
		return p.level().getBlockState(new BlockPos(dx, Y + dy, dz));
	}
}
