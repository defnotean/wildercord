package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Set;

/**
 * The fx-explore pack (the wayfarer's runes), cast for real through paid Self spells in Survival: sixteen of them, each with
 * the change it makes in the world asserted (recoloured blocks and the dye paid, lit and snuffed lamps, glowing and carved
 * signs, a posed stand, a veiled frame, the trade helpers on a villager, a hands-free brushing, and the passing blocks of
 * Trail Blaze, Lava Crust and Void Step).
 */
public final class WayfarerPackTest implements FabricClientGameTest {
	private static final BlockPos FEET = new BlockPos(0, 101, 0);
	private static final List<BlockPos> WOOL = List.of(new BlockPos(1, 101, 0), new BlockPos(-1, 101, 0), new BlockPos(0, 101, 1),
		new BlockPos(0, 101, -1), new BlockPos(1, 101, 1));
	private static final BlockPos CANDLE = new BlockPos(3, 101, 0), CAMPFIRE = new BlockPos(-3, 101, 0), SIGN = new BlockPos(0, 101, 3);
	private static final BlockPos WALL = new BlockPos(-3, 101, 3), FRAME = new BlockPos(-3, 101, 2), RELIC = new BlockPos(0, 100, 2);
	private static final BlockPos LAVA = new BlockPos(-2, 100, -1);
	private static final String STAND = "wayfarer_stand", FRAMED = "wayfarer_frame", FOLK = "wayfarer_villager";

	@Override
	public void runTest(ClientGameTestContext c) {
		try (TestSingleplayerContext world = c.worldBuilder().create()) {
			c.waitTicks(40);
			var server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("fill -12 99 -12 12 99 12 deepslate");
			server.runCommand("fill -12 100 -12 12 100 12 polished_deepslate");
			on(c, world, (s, p, level) -> {
				p.setGameMode(GameType.SURVIVAL);
				home(p);
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				var book = Spellbooks.get(p).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(p, book);
				p.getInventory().clearContent();
				for (BlockPos pos : WOOL) {
					level.setBlockAndUpdate(pos, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
				}
				level.setBlockAndUpdate(CANDLE, Blocks.CANDLE.defaultBlockState());
				level.setBlockAndUpdate(CAMPFIRE, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
				level.setBlockAndUpdate(SIGN, Blocks.OAK_SIGN.defaultBlockState());
				level.setBlockAndUpdate(WALL, Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(RELIC, Blocks.SUSPICIOUS_SAND.defaultBlockState());
				ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
				check(stand != null, "armor stand fixture");
				stand.snapTo(2.5, 101, -2.5, 0, 0);
				stand.addTag(STAND);
				level.addFreshEntity(stand);
				ItemFrame frame = new ItemFrame(level, FRAME, Direction.NORTH);
				frame.setItem(new ItemStack(Items.APPLE), false);
				frame.addTag(FRAMED);
				level.addFreshEntity(frame);
				Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				check(villager != null, "villager fixture");
				villager.snapTo(2.5, 101, 2.5, 0, 0);
				villager.setNoAi(true);
				villager.addTag(FOLK);
				villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FARMER).withLevel(1));
				villager.setVillagerXp(1);
				level.addFreshEntity(villager);
			});
			c.waitTicks(5);

			// Dye Wash: every white wool within reach turns red; five blocks cost one dye.
			on(c, world, (s, p, level) -> {
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DYE.pick(DyeColor.RED), 8));
				cast(p, Runes.DYE_WASH);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				for (BlockPos pos : WOOL) {
					check(level.getBlockState(pos).is(Blocks.WOOL.pick(DyeColor.RED)), "Dye Wash recolours " + pos.toShortString() + ": " + level.getBlockState(pos));
				}
				check(p.getOffhandItem().is(Items.DYE.pick(DyeColor.RED)) && p.getOffhandItem().getCount() == 7, "Dye Wash pays one dye for five blocks: " + p.getOffhandItem());
				// Checker Dye: only the even squares take the blue.
				p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DYE.pick(DyeColor.BLUE), 8));
				cast(p, Runes.CHECKER_DYE);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				for (BlockPos pos : WOOL) {
					boolean even = Math.floorMod(pos.getX() + pos.getY() + pos.getZ(), 2) == 0;
					check(level.getBlockState(pos).is(even ? Blocks.WOOL.pick(DyeColor.BLUE) : Blocks.WOOL.pick(DyeColor.RED)), "Checker Dye squares " + pos.toShortString() + ": " + level.getBlockState(pos));
				}
				check(p.getOffhandItem().getCount() == 7, "Checker Dye pays one dye: " + p.getOffhandItem());
				p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				cast(p, Runes.LAMPLIGHTER);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(lit(level, CANDLE) && lit(level, CAMPFIRE), "Lamplighter lights the candle and the campfire");
				cast(p, Runes.SNUFF_OUT);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(!lit(level, CANDLE) && !lit(level, CAMPFIRE), "Snuff Out puts out the candle and the campfire");
				cast(p, Runes.SIGN_GLOW);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				SignBlockEntity sign = sign(level);
				check(sign.getText(SignTextSlot.FRONT).hasGlowingText() && sign.getText(SignTextSlot.BACK).hasGlowingText(), "Sign Glow lights both faces");
				cast(p, Runes.GLYPH_CARVE);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				SignBlockEntity sign = sign(level);
				boolean carved = false;
				for (SignTextSlot slot : List.of(SignTextSlot.FRONT, SignTextSlot.BACK)) {
					carved |= sign.getText(slot).getMessages(false).getFirst().getString().contains("✦");
				}
				check(carved, "Glyph Carve writes its glyph on the first blank line");
				cast(p, Runes.STAND_POSE);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				ArmorStand stand = tagged(level, ArmorStand.class, STAND);
				check(stand.showArms(), "Stand Pose gives the stand arms");
				check(new Rotations(-110, -30, 0).equals(stand.getRightArmPose()), "Stand Pose steps from the default pose to the next: " + stand.getRightArmPose());
				cast(p, Runes.FRAME_VEIL);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(tagged(level, ItemFrame.class, FRAMED).isInvisible(), "Frame Veil hides a frame holding an item");
				cast(p, Runes.HAGGLE);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(p.getEffect(MobEffects.HERO_OF_THE_VILLAGE) != null, "Haggle grants Hero of the Village prices");
				p.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
				cast(p, Runes.POTION_STEEP);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				MobEffectInstance speed = p.getEffect(MobEffects.SPEED);
				check(speed != null && speed.getDuration() > 1300 && speed.getDuration() <= 1500, "Potion Steep lengthens a good effect by a quarter: " + speed);
				Villager villager = tagged(level, Villager.class, FOLK);
				check(!villager.getOffers().isEmpty(), "the farmer has trades");
				villager.getOffers().getFirst().setToOutOfStock();
				cast(p, Runes.TRADE_RENEW);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				Villager villager = tagged(level, Villager.class, FOLK);
				check(villager.getOffers().stream().noneMatch(o -> o.isOutOfStock()), "Restock refills every offer");
				check(villager.entityTags().stream().anyMatch(t -> t.startsWith("wildercord.restocked.")), "Restock marks the day");
				cast(p, Runes.FOLK_CALL);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(tagged(level, Villager.class, FOLK).getBrain().getMemory(MemoryModuleType.WALK_TARGET).isPresent(), "Folk Call gives the villager a walk target");
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
				cast(p, Runes.STEADY_BRUSH);
			});
			c.waitTicks(140);
			on(c, world, (s, p, level) -> {
				check(level.getBlockState(RELIC).is(Blocks.SAND), "Steady Brush brushes the suspicious sand out: " + level.getBlockState(RELIC));
				check(p.getMainHandItem().is(Items.BRUSH) && p.getMainHandItem().getDamageValue() == 1, "the brush wears a point: " + p.getMainHandItem());
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				cast(p, Runes.TRAIL_BLAZE);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(level.getBlockState(FEET).is(Blocks.END_ROD), "Trail Blaze sets an end rod: " + level.getBlockState(FEET));
				level.setBlockAndUpdate(LAVA, Blocks.LAVA.defaultBlockState());
			});
			c.waitTicks(2);
			on(c, world, (s, p, level) -> cast(p, Runes.LAVA_CRUST));
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				check(level.getBlockState(LAVA).is(Blocks.SMOOTH_BASALT), "Lava Crust hardens the lava top: " + level.getBlockState(LAVA));
				// Void Step: out over the open air, a platform forms under the feet.
				p.teleportTo(level, 20.5, 110, 20.5, Set.<Relative>of(), 0, 0, false);
				cast(p, Runes.VOID_STEP);
			});
			c.waitTicks(20);
			on(c, world, (s, p, level) -> {
				int stone = 0;
				for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(17, 80, 17), new BlockPos(23, 112, 23))) {
					if (level.getBlockState(pos).is(Blocks.END_STONE)) {
						stone++;
					}
				}
				check(stone == 9, "Void Step lays a three-by-three platform: " + stone);
				check(p.getY() > 100, "the platform holds the caster up: " + p.getY());
			});
		}
	}

	private interface Step {
		void run(MinecraftServer server, ServerPlayer player, ServerLevel level);
	}

	private static void on(ClientGameTestContext c, TestSingleplayerContext world, Step step) {
		world.getServer().runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			step.run(s, p, s.overworld());
		});
	}

	private static void home(ServerPlayer p) {
		p.teleportTo(p.level(), 0.5, 101, 0.5, Set.<Relative>of(), 0, 0, false);
	}

	/** One paid Self cast of {@code rune}, in Survival, that has to spend mana. */
	private static void cast(ServerPlayer p, RuneDef rune) {
		check(SpellCaster.edit(p, 0, List.of(Runes.SELF.id(), rune.id())) == null, "Accepted Self " + rune.name());
		Spellbooks.setMana(p, 100);
		Spellbooks.setReadyAt(p, 0, 0);
		float before = Spellbooks.mana(p);
		SpellCaster.cast(p, 0);
		check(Spellbooks.mana(p) < before, rune.name() + " spends Survival mana");
	}

	private static boolean lit(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
	}

	private static SignBlockEntity sign(ServerLevel level) {
		check(level.getBlockEntity(SIGN) instanceof SignBlockEntity, "sign fixture");
		return (SignBlockEntity) level.getBlockEntity(SIGN);
	}

	private static <T extends net.minecraft.world.entity.Entity> T tagged(ServerLevel level, Class<T> type, String tag) {
		return level.getEntitiesOfClass(type, new net.minecraft.world.phys.AABB(FEET).inflate(12), e -> e.entityTags().contains(tag)).stream()
			.findFirst().orElseThrow(() -> new AssertionError("fixture missing: " + tag));
	}

	private static void check(boolean yes, String why) {
		if (!yes) {
			throw new AssertionError(why);
		}
	}
}
