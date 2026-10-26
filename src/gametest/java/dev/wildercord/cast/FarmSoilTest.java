package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StemBlock;
import java.util.List;
import java.util.Set;

/** Sapling Sow, Coppice and Gourdcall, cast for real through Touch, plant, replant and set fruit on grass, not just on dirt. */
public final class FarmSoilTest implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c) {
        try (var w = c.worldBuilder().create()) {
            c.waitTicks(30);
            w.getServer().runCommand("gamerule spawn_mobs false");
            w.getServer().runCommand("time set 6000");
            w.getServer().runOnServer(s -> {
                var p = p(s);
                var l = s.overworld();
                p.setGameMode(GameType.SURVIVAL);
                Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
                Spellbooks.set(p, Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.SAPLINGSOW.id())
                    .learn(Runes.COPPICE.id()).learn(Runes.GOURDCALL.id()));
                for (int x = -4; x <= 44; x++) for (int z = -4; z <= 4; z++) {
                    l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 101; y <= 108; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                p.getInventory().clearContent();
                p.getInventory().add(new ItemStack(Items.OAK_SAPLING, 16));
            });

            // Sapling Sow on a grass field.
            cast(c, w, Runes.SAPLINGSOW, .5, .5, 0, 90);
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                int planted = 0;
                for (BlockPos q : BlockPos.betweenClosed(-3, 101, -3, 3, 101, 3)) {
                    if (l.getBlockState(q).is(Blocks.OAK_SAPLING)) {
                        planted++;
                        check(l.getBlockState(q.below()).is(Blocks.GRASS_BLOCK), "Each sapling stands on the grass (" + q + ")");
                    }
                }
                check(planted >= 2, "Sapling Sow plants on grass (" + planted + " planted)");
                check(count(p(s)) == 16 - planted, "Each sapling planted comes from the caster's inventory");
            });
            c.waitTicks(5);
            shot(c, "farm_saplingsow_grass");

            // Coppice on an oak standing on grass: the trunk falls and the stump is replanted.
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(20, y, 0), Blocks.OAK_LOG.defaultBlockState(), 2);
                l.setBlock(new BlockPos(20, 105, 0), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false), 2);
            });
            int before = w.getServer().computeOnServer(s -> count(p(s)));
            cast(c, w, Runes.COPPICE, 20.5, -1.5, 0, 29);
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                for (int y = 102; y <= 104; y++) check(l.getBlockState(new BlockPos(20, y, 0)).isAir(), "The trunk is felled at y=" + y);
                check(l.getBlockState(new BlockPos(20, 101, 0)).is(Blocks.OAK_SAPLING), "The stump on grass is replanted");
                check(count(p(s)) == before - 1, "The replanted sapling is the caster's own");
            });

            // Gourdcall: a grown stem whose only open side has grass below sets its pumpkin there.
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                l.setBlock(new BlockPos(40, 100, 0), Blocks.FARMLAND.defaultBlockState(), 2);
                l.setBlock(new BlockPos(40, 101, 0), Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, StemBlock.MAX_AGE), 2);
                for (BlockPos q : List.of(new BlockPos(39, 100, 0), new BlockPos(40, 100, 1), new BlockPos(40, 100, -1))) {
                    l.setBlock(q, Blocks.STONE.defaultBlockState(), 2);
                }
            });
            cast(c, w, Runes.GOURDCALL, 40.5, -1.5, 0, 35);
            w.getServer().runOnServer(s -> {
                var l = s.overworld();
                check(l.getBlockState(new BlockPos(41, 101, 0)).is(Blocks.PUMPKIN), "The pumpkin sets on the side with grass below");
                var stem = l.getBlockState(new BlockPos(40, 101, 0));
                check(stem.is(Blocks.ATTACHED_PUMPKIN_STEM) && stem.getValue(AttachedStemBlock.FACING) == net.minecraft.core.Direction.EAST, "The stem turns to its pumpkin");
                p(s).teleportTo(l, 30.5, 106, -8.5, Set.<Relative>of(), 0, 25, false);
            });
            c.waitTicks(10);
            shot(c, "farm_coppice_gourdcall_grass");
        }
    }

    private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

    private static int count(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(Items.OAK_SAPLING)) n += p.getInventory().getItem(i).getCount();
        return n;
    }

    /** Puts Touch and {@code rune} in the first slot and casts it from (x, 101, z), looking at the given yaw and pitch. */
    private static void cast(ClientGameTestContext c, TestSingleplayerContext w, RuneDef rune, double x, double z, float yaw, float pitch) {
        w.getServer().runOnServer(s -> {
            var p = p(s);
            p.teleportTo(s.overworld(), x, 101, z, Set.<Relative>of(), yaw, pitch, false);
            check(SpellCaster.edit(p, 0, List.of(Runes.TOUCH.id(), rune.id())) == null, "Touch and " + rune.id() + " make a spell");
            Spellbooks.setMana(p, 100);
            Spellbooks.setReadyAt(p, 0, 0);
            float mana = Spellbooks.mana(p);
            SpellCaster.cast(p, 0);
            check(Spellbooks.mana(p) < mana, "The " + rune.id() + " cast goes off and pays mana");
        });
        c.waitTicks(5);
    }

    private static void shot(ClientGameTestContext c, String name) {
        c.runOnClient(mc -> { mc.getWindow().setWindowed(1280, 720); mc.resizeGui(); mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
        c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    private static void check(boolean yes, String why) { if (!yes) throw new AssertionError(why); }
}
