package dev.wildercord.cast;

import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * The hearth pack, played: its watchers fire on the real thing (a mined block, a picked crop, a catch, a sprint, a splash,
 * a mount, a wake), its conditions hold or refuse, and its modifiers change what a spell does to blocks and creatures.
 */
public final class HearthRunesGameTest implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c) {
        try (var w = c.worldBuilder().create()) {
            c.waitTicks(30);
            for (String rule : List.of("spawn_mobs false", "advance_time false", "advance_weather false", "random_tick_speed 0", "natural_health_regeneration false"))
                w.getServer().runCommand("gamerule " + rule);
            w.getServer().runCommand("time set 6000");
            w.getServer().runCommand("weather clear");
            w.getServer().runOnServer(s -> {
                var p = p(s);
                p.setGameMode(GameType.SURVIVAL);
                var l = s.overworld();
                for (int x = -6; x <= 140; x++) for (int z = -4; z <= 4; z++) {
                    l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 101; y <= 112; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                home(p);
            });
            c.waitTicks(10);

            // ---------------------------------------------------------------- conditions
            w.getServer().runOnServer(s -> {
                var p = p(s);
                p.removeAllEffects();
                CastEngine.cast(p, plan(Runes.IF_NIGHT, Runes.SELF, Runes.SWIFT).root());
                check(!p.hasEffect(MobEffects.SPEED), "If Night holds the rest back by day");
                CastEngine.cast(p, plan(Runes.IF_DAY, Runes.SELF, Runes.SWIFT).root());
                check(p.hasEffect(MobEffects.SPEED), "If Day lets the rest fire by day");
                p.removeAllEffects();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                CastEngine.cast(p, plan(Runes.IF_HOLDING_TOOL, Runes.SELF, Runes.SWIFT).root());
                check(!p.hasEffect(MobEffects.SPEED), "If Holding Tool refuses an empty hand");
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
                CastEngine.cast(p, plan(Runes.IF_HOLDING_TOOL, Runes.SELF, Runes.SWIFT).root());
                check(p.hasEffect(MobEffects.SPEED), "If Holding Tool fires with a pickaxe in hand");
                p.removeAllEffects();
                CastEngine.cast(p, plan(Runes.IF_IN_FIELDS, Runes.SELF, Runes.SWIFT).root());
                check(!p.hasEffect(MobEffects.SPEED), "If In Fields refuses bare stone");
                s.overworld().setBlockAndUpdate(new BlockPos(2, 100, 2), Blocks.FARMLAND.defaultBlockState());
                CastEngine.cast(p, plan(Runes.IF_IN_FIELDS, Runes.SELF, Runes.SWIFT).root());
                check(p.hasEffect(MobEffects.SPEED), "If In Fields fires beside farmland");
                s.overworld().setBlockAndUpdate(new BlockPos(2, 100, 2), Blocks.STONE.defaultBlockState());
                p.removeAllEffects();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });

            // ---------------------------------------------------------------- watchers, on their real triggers
            // On Mine: the next block the player mines grows the young wheat beside it.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                l.setBlockAndUpdate(new BlockPos(10, 101, 0), Blocks.COBBLESTONE.defaultBlockState());
                crop(l, new BlockPos(11, 101, 0), 0);
                int before = HearthLinks.waiting();
                CastEngine.cast(p, plan(Runes.ON_MINE, Runes.GROW).root());
                check(HearthLinks.waiting() == before + 1, "On Mine waits");
                check(age(l, new BlockPos(11, 101, 0)) == 0, "On Mine does nothing until a block is mined");
                check(p.gameMode.destroyBlock(new BlockPos(10, 101, 0)), "Player mines the cobblestone");
                check(HearthLinks.waiting() == before, "On Mine fired on the mined block");
                check(age(l, new BlockPos(11, 101, 0)) > 0, "On Mine's Grow landed where the block was mined");
            });
            // On Harvest: an unripe crop doesn't count; the ripe one does.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                crop(l, new BlockPos(20, 101, 0), 2);
                crop(l, new BlockPos(24, 101, 0), 7);
                crop(l, new BlockPos(25, 101, 0), 0);
                int before = HearthLinks.waiting();
                CastEngine.cast(p, plan(Runes.ON_HARVEST, Runes.GROW).root());
                check(p.gameMode.destroyBlock(new BlockPos(20, 101, 0)), "Player picks the unripe wheat");
                check(HearthLinks.waiting() == before + 1, "On Harvest ignores an unripe crop");
                check(p.gameMode.destroyBlock(new BlockPos(24, 101, 0)), "Player picks the ripe wheat");
                check(HearthLinks.waiting() == before, "On Harvest fired on the ripe crop");
                check(age(l, new BlockPos(25, 101, 0)) > 0, "On Harvest's Grow landed at the picked crop");
            });
            // On Catch: the catch statistic a reeled-in fish awards.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                crop(l, new BlockPos(1, 101, 0), 0);
                CastEngine.cast(p, plan(Runes.ON_CATCH, Runes.GROW).root());
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                check(age(s.overworld(), new BlockPos(1, 101, 0)) == 0, "On Catch waits for a catch");
                p(s).awardStat(Stats.FISH_CAUGHT);
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                check(HearthLinks.waiting() == 0, "On Catch fired");
                check(age(s.overworld(), new BlockPos(1, 101, 0)) > 0, "On Catch's Grow landed where the player stood");
                s.overworld().setBlockAndUpdate(new BlockPos(1, 101, 0), Blocks.AIR.defaultBlockState());
                s.overworld().setBlockAndUpdate(new BlockPos(1, 100, 0), Blocks.STONE.defaultBlockState());
            });
            // On Sprint: heals the player the moment they break into a sprint.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                p.setSprinting(false);
                p.setHealth(6);
                CastEngine.cast(p, plan(Runes.ON_SPRINT, Runes.HEAL).root());
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(p.getHealth() <= 6.01F && HearthLinks.waiting() == 1, "On Sprint waits while walking");
                p.setSprinting(true);
            });
            c.waitTicks(2);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                p.setSprinting(false);
                check(HearthLinks.waiting() == 0, "On Sprint fired");
                check(p.getHealth() > 6.5F, "On Sprint's Heal landed on the sprinter: " + p.getHealth());
            });
            // On Splash: heals the player when they step into water.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                for (int x = 39; x <= 41; x++) for (int z = -1; z <= 1; z++) l.setBlock(new BlockPos(x, 101, z),
                    x == 40 && z == 0 ? Blocks.WATER.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                p.setHealth(6);
                CastEngine.cast(p, plan(Runes.ON_SPLASH, Runes.HEAL).root());
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(HearthLinks.waiting() == 1 && p.getHealth() <= 6.01F, "On Splash waits on dry stone");
                p.teleportTo(s.overworld(), 40.5, 101, 0.5, Set.<Relative>of(), 0, 0, false);
            });
            c.waitTicks(4);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(HearthLinks.waiting() == 0, "On Splash fired in the water");
                check(p.getHealth() > 6.5F, "On Splash's Heal landed: " + p.getHealth());
                home(p);
            });
            c.waitTicks(3);
            // On Mount: grows the wheat beside the pig the player climbs on.
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                crop(l, new BlockPos(51, 101, 0), 0);
                var pig = spawn(l, EntityTypes.PIG, 50.5, 101, 0.5);
                pig.setNoAi(true);
                CastEngine.cast(p, plan(Runes.ON_MOUNT, Runes.GROW).root());
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(HearthLinks.waiting() == 1 && age(s.overworld(), new BlockPos(51, 101, 0)) == 0, "On Mount waits on foot");
                var pig = s.overworld().getEntitiesOfClass(Animal.class, new AABB(new BlockPos(50, 101, 0)).inflate(2)).getFirst();
                check(p.startRiding(pig, true, true), "Player climbs onto the pig");
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(HearthLinks.waiting() == 0, "On Mount fired on mounting");
                check(age(s.overworld(), new BlockPos(51, 101, 0)) > 0, "On Mount's Grow landed at the mount");
                p.stopRiding();
                home(p);
            });
            shot(c, "hearth_runes_day");

            // ---------------------------------------------------------------- modifiers change what happens
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                p.getInventory().clearContent();
                // Tidy: the cobblestone goes into the pack, not onto the ground.
                BlockPos tidy = new BlockPos(100, 101, 0);
                l.setBlockAndUpdate(tidy, Blocks.STONE.defaultBlockState());
                impact(p, tidy, Runes.BREAK, Runes.TIDY);
                check(l.getBlockState(tidy).isAir(), "Tidy's Break took the stone");
                check(p.getInventory().countItem(Items.COBBLESTONE) == 1, "Tidy put the cobblestone in the pack");
                check(items(l, tidy, Items.COBBLESTONE) == 0, "Tidy left nothing on the ground");
                // Kilned: sand drops as glass.
                BlockPos kiln = new BlockPos(104, 101, 0);
                l.setBlockAndUpdate(kiln, Blocks.SAND.defaultBlockState());
                impact(p, kiln, Runes.BREAK, Runes.KILNED);
                check(items(l, kiln, Items.GLASS) == 1 && items(l, kiln, Items.SAND) == 0, "Kilned smelted the sand to glass");
                // Silken: glass drops itself; plain Break drops nothing.
                BlockPos silk = new BlockPos(112, 101, 0), bare = new BlockPos(116, 101, 0);
                l.setBlockAndUpdate(silk, Blocks.GLASS.defaultBlockState());
                l.setBlockAndUpdate(bare, Blocks.GLASS.defaultBlockState());
                impact(p, bare, Runes.BREAK);
                impact(p, silk, Runes.BREAK, Runes.SILKEN);
                check(l.getBlockState(bare).isAir() && items(l, bare, Items.GLASS) == 0, "Plain Break shatters glass");
                check(items(l, silk, Items.GLASS) == 1, "Silken kept the glass whole");
                // Timbering fells the trunk above; plain Break takes one log.
                for (int y = 101; y <= 105; y++) {
                    l.setBlock(new BlockPos(120, y, 0), Blocks.OAK_LOG.defaultBlockState(), 2);
                    l.setBlock(new BlockPos(124, y, 0), Blocks.OAK_LOG.defaultBlockState(), 2);
                }
                impact(p, new BlockPos(124, 101, 0), Runes.BREAK);
                impact(p, new BlockPos(120, 101, 0), Runes.BREAK, Runes.TIMBERING);
                check(l.getBlockState(new BlockPos(124, 102, 0)).is(Blocks.OAK_LOG), "Plain Break leaves the trunk standing");
                for (int y = 101; y <= 105; y++) check(l.getBlockState(new BlockPos(120, y, 0)).isAir(), "Timbering felled the log at y=" + y);
                // Veinfollow follows the coal through the stone.
                List<BlockPos> vein = List.of(new BlockPos(128, 101, 0), new BlockPos(128, 102, 0), new BlockPos(129, 102, 0), new BlockPos(129, 103, 1));
                for (BlockPos v : vein) l.setBlock(v, Blocks.COAL_ORE.defaultBlockState(), 2);
                impact(p, vein.getFirst(), Runes.BREAK, Runes.VEINFOLLOW);
                for (BlockPos v : vein) check(l.getBlockState(v).isAir(), "Veinfollow broke the ore at " + v);
                // Steady: the effect can't break anything at all.
                BlockPos still = new BlockPos(132, 101, 0);
                l.setBlockAndUpdate(still, Blocks.STONE.defaultBlockState());
                impact(p, still, Runes.BREAK, Runes.STEADY);
                check(l.getBlockState(still).is(Blocks.STONE), "Steady kept the block");
                // Level Ground: never below the caster's feet.
                BlockPos below = new BlockPos(136, 100, 0), level = new BlockPos(136, 101, 0);
                l.setBlockAndUpdate(level, Blocks.STONE.defaultBlockState());
                impact(p, below, Runes.BREAK, Runes.LEVEL_GROUND);
                impact(p, level, Runes.BREAK, Runes.LEVEL_GROUND);
                check(l.getBlockState(below).is(Blocks.STONE), "Level Ground refused the block under the caster's feet level");
                check(l.getBlockState(level).isAir(), "Level Ground still breaks at the caster's level");
                // Furrowing tills the dirt around where it lands.
                for (int x = 60; x <= 62; x++) l.setBlockAndUpdate(new BlockPos(x, 100, 0), Blocks.DIRT.defaultBlockState());
                impact(p, new BlockPos(61, 100, 0), Runes.GROW, Runes.FURROWING);
                for (int x = 60; x <= 62; x++) check(l.getBlockState(new BlockPos(x, 100, 0)).is(Blocks.FARMLAND), "Furrowing tilled x=" + x);
            });
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                // Gentle passes over a cow that plain Harm hurts.
                var spared = spawn(l, EntityTypes.COW, 70.5, 101, 2.5);
                var struck = spawn(l, EntityTypes.COW, 74.5, 101, 2.5);
                strike(p, List.of(struck), Runes.HARM);
                strike(p, List.of(spared), Runes.HARM, Runes.GENTLE);
                check(struck.getHealth() < struck.getMaxHealth(), "Plain Harm hurts the cow");
                check(spared.getHealth() >= spared.getMaxHealth(), "Gentle passed over the cow");
                // Culling lands only on the monster.
                var calf = spawn(l, EntityTypes.COW, 78.5, 101, 2.5);
                var husk = spawn(l, EntityTypes.HUSK, 79.5, 101, 2.5);
                strike(p, List.of(calf, husk), Runes.HARM, Runes.CULLING);
                check(calf.getHealth() >= calf.getMaxHealth(), "Culling passed over the cow");
                check(husk.getHealth() < husk.getMaxHealth(), "Culling struck the husk");
                // Matchmaking: a grown cow falls in love.
                var single = spawn(l, EntityTypes.COW, 82.5, 101, 2.5);
                strike(p, List.of(single), Runes.HEAL, Runes.MATCHMAKING);
                check(single.isInLove(), "Matchmaking set the cow in love");
                // Fleecing shears the sheep; the wool lands at the caster's feet.
                var sheep = spawn(l, EntityTypes.SHEEP, 86.5, 101, 2.5);
                check(sheep.readyForShearing(), "The sheep starts woolly");
                strike(p, List.of(sheep), Runes.HEAL, Runes.FLEECING);
                check(!sheep.readyForShearing(), "Fleecing shore the sheep");
                long wool = l.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(2), i -> i.getItem().is(Items.WOOL.white())).size();
                check(wool > 0, "Fleecing's wool landed at the caster's feet");
            });
            c.waitTicks(5);
            shot(c, "hearth_runes_modifiers");

            // ---------------------------------------------------------------- night: If Night, and On Wake
            w.getServer().runCommand("time set 18000");
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                p.removeAllEffects();
                CastEngine.cast(p, plan(Runes.IF_NIGHT, Runes.SELF, Runes.SWIFT).root());
                check(p.hasEffect(MobEffects.SPEED), "If Night fires at night");
                p.removeAllEffects();
                BlockPos foot = new BlockPos(0, 101, 3);
                l.setBlockAndUpdate(foot.north(), Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
                l.setBlockAndUpdate(foot, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
                p.setHealth(6);
                CastEngine.cast(p, plan(Runes.ON_WAKE, Runes.HEAL).root());
                p.startSleeping(foot);
            });
            c.waitTicks(4);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(p.isSleeping() && HearthLinks.waiting() == 1 && p.getHealth() <= 6.01F, "On Wake waits while asleep");
                p.stopSleepInBed(true, true);
            });
            c.waitTicks(3);
            w.getServer().runOnServer(s -> {
                var p = p(s);
                check(HearthLinks.waiting() == 0, "On Wake fired on waking");
                check(p.getHealth() > 6.5F, "On Wake's Heal landed on the sleeper: " + p.getHealth());
            });
        }
    }

    private static ServerPlayer p(MinecraftServer s) {
        return s.getPlayerList().getPlayers().getFirst();
    }

    private static void home(ServerPlayer p) {
        p.teleportTo(p.level().getServer().overworld(), 0.5, 101, 0.5, Set.<Relative>of(), 0, 30, false);
    }

    private static SpellCompiler.Compiled plan(RuneDef... runes) {
        var compiled = SpellCompiler.compile(List.of(runes));
        check(compiled.warnings().isEmpty(), "Compiles cleanly: " + compiled.warnings());
        return compiled;
    }

    private static void impact(ServerPlayer p, BlockPos at, RuneDef... effect) {
        var runes = new ArrayList<RuneDef>(List.of(Runes.TOUCH));
        runes.addAll(List.of(effect));
        var compiled = plan(runes.toArray(RuneDef[]::new));
        CastEngine.onHit(new Cast(p), compiled.root().groups.getFirst(),
            new Cast.Hit(List.of(), Vec3.atCenterOf(at), new Vec3(0, -1, 0), p.position(), at, Direction.UP, false), null);
    }

    private static void strike(ServerPlayer p, List<? extends Entity> targets, RuneDef... effect) {
        var runes = new ArrayList<RuneDef>(List.of(Runes.TOUCH));
        runes.addAll(List.of(effect));
        var compiled = plan(runes.toArray(RuneDef[]::new));
        Entity first = targets.getFirst();
        CastEngine.onHit(new Cast(p), compiled.root().groups.getFirst(),
            new Cast.Hit(List.copyOf(targets), first.position(), new Vec3(0, 0, 1), p.position(), null, null, false), null);
    }

    private static <T extends Entity> T spawn(ServerLevel l, EntityType<T> type, double x, double y, double z) {
        T e = type.create(l, EntitySpawnReason.COMMAND);
        e.snapTo(x, y, z, 0, 0);
        l.addFreshEntity(e);
        return e;
    }

    private static void crop(ServerLevel l, BlockPos pos, int age) {
        l.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        l.setBlockAndUpdate(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(age));
    }

    private static int age(ServerLevel l, BlockPos pos) {
        var state = l.getBlockState(pos);
        return state.getBlock() instanceof CropBlock ? state.getValue(CropBlock.AGE) : -1;
    }

    private static int items(ServerLevel l, BlockPos at, Item item) {
        return l.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2), i -> i.getItem().is(item)).stream().mapToInt(i -> i.getItem().getCount()).sum();
    }

    private static void shot(ClientGameTestContext c, String name) {
        c.runOnClient(mc -> {mc.getWindow().setWindowed(1280, 720); mc.resizeGui(); mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);});
        c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    private static void check(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }
}
