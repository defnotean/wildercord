package dev.wildercord.cast;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.PackLessonScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.LessonPackRules.Lesson;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

/** Single-player: an old boss feat recovers each study, three real pages teach it, the Codex equips it and the rebound Cast key places it. */
public final class LessonPackPlayableTest implements FabricClientGameTest {
    private static final Vec3 FEET = new Vec3(.5, 150, .5);
    private static long nonce = 300000;
    private static float manaBefore;
    private static Zombie foe;
    private static Wolf pet;
    private static double tollFrom;
    // Eighteen circles regenerate mana every tick, so payments are read from the cast ledger, not the live balance.
    private static int payments, paid;
    private static float paidBefore, paidAfter;
    private static boolean listening;

    @Override public void runTest(ClientGameTestContext context) {
        KeyMapping[] key = new KeyMapping[1]; InputConstants.Key[] original = new InputConstants.Key[1];
        if (!listening) {
            listening = true;
            dev.wildercord.api.WildercordEvents.BEFORE_CAST.register((p, slot, runes, cost) -> { if (LessonPackRules.contains(runes)) paidBefore = Spellbooks.mana(p); return true; });
            dev.wildercord.api.WildercordEvents.AFTER_CAST.register((p, slot, runes, spent) -> { if (LessonPackRules.contains(runes)) { payments++; paid = spent; paidAfter = Spellbooks.mana(p); } });
        }
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            context.waitTicks(40);
            server.runCommand("gamerule spawn_mobs false"); server.runCommand("gamerule fall_damage false");
            server.runOnServer(s -> {
                var p = player(s); var level = p.level();
                for (int x = -12; x <= 12; x++) for (int z = -6; z <= 20; z++) {
                    level.setBlock(new BlockPos(x, 149, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 150; y <= 156; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
                p.setAttached(WildercordAttachments.CIRCLES, 18); p.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
                // An old save: the three boss feats exist, nothing about the pack does.
                p.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:cinder_warden", "feat:star_eater", "feat:storm_conductor"));
                Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
                Spellbooks.set(p, new Spellbook(List.of(Runes.WALL.id(), Runes.BEAM.id(), Runes.PILLAR.id(), Runes.HARM.id()), List.of(), 0, true));
                stand(p, 0, 35);
                for (Lesson lesson : LessonPackRules.ALL) {
                    check(MasterStudies.hasLesson(p, lesson) && !MasterStudies.knows(p, lesson) && !Spellbooks.knows(p, lesson.id),
                        lesson.name + ": an old boss feat recovers the study but never teaches it");
                }
                Spellbooks.set(p, Spellbooks.get(p).withSpell(3, List.of(dev.wildercord.spell.Knots.PREFIX + "!")));
                String malformed = LessonPackCasting.problem(p, 3);
                check(LessonPackCasting.MALFORMED_ROW.equals(malformed) && LessonPackRules.ALL.stream().noneMatch(l -> malformed.contains(l.name)),
                    "An unreadable row fails closed without being named as any lesson, got " + malformed);
                LessonPackCasting.forgetRefusal(p);
                down(p, 3);
                check(LessonPackCasting.MALFORMED_ROW.equals(LessonPackCasting.lastRefusal(p)) && !LessonPackCasting.standing(p, Lesson.TOLLGATE),
                    "Pressing an unreadable row places nothing and gives the neutral reason");
                Spellbooks.set(p, Spellbooks.get(p).withSpell(3, List.of()));
            });
            context.waitTicks(5);
            for (Lesson lesson : LessonPackRules.ALL) learn(context, lesson);
            server.runOnServer(s -> {
                for (Lesson lesson : LessonPackRules.ALL)
                    check(MasterStudies.knows(player(s), lesson) && Spellbooks.knows(player(s), lesson.id) && !MasterStudies.practiced(player(s), lesson),
                        lesson.name + ": three real pages teach the rune, never the practice");
            });
            equip(context, Lesson.TOLLGATE);
            context.runOnClient(mc -> {
                key[0] = WildercordKeys.castMapping(); original[0] = KeyMappingHelper.getBoundKeyOf(key[0]);
                key[0].setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F8)); KeyMapping.resetMapping();
                check(WildercordKeys.castKey().getString().contains("F8"), "The lesson reads the rebound Cast control");
            });
            try {
                server.runOnServer(s -> {
                    var p = player(s);
                    check(Lesson.TOLLGATE.ids.equals(Spellbooks.get(p).spells().getFirst()), "Codex clicks equip Wall then Tollgate");
                    Spellbooks.setMana(p, 200); payments = 0; stand(p, 0, 35);
                });
                context.waitTicks(3);
                context.getInput().pressKey(key[0]); context.waitTicks(4);
                server.runOnServer(s -> {
                    var p = player(s); long now = LessonPackCasting.now(p);
                    check(LessonPackCasting.standing(p, Lesson.TOLLGATE), "One rebound press sets the gate (selected " + Spellbooks.get(p).selected() + ", problem " + LessonPackCasting.problem(p, 0) + ")");
                    check(payments == 1 && paid == price(p, Lesson.TOLLGATE) && paid <= 30 && paidBefore - paidAfter == paid, "Tollgate pays its circle-scaled price once, got " + payments + " x " + paid + " (" + (paidBefore - paidAfter) + ")");
                    long rest = LessonPackCasting.rest(p, Lesson.TOLLGATE);
                    check(rest > now && rest - now <= 200, "Tollgate owns its ten-second shared rest");
                    check(p.hasAttached(WildercordAttachments.CAST_POSE) && p.hasAttached(dev.wildercord.cast.LessonPackState.VIEW), "Pose and HUD view are published");
                    manaBefore = Spellbooks.mana(p);
                    Vec3 centre = p.getAttached(dev.wildercord.cast.LessonPackState.VIEW).at();
                    foe = EntityTypes.ZOMBIE.create(p.level(), EntitySpawnReason.COMMAND); check(foe != null, "A native foe exists");
                    foe.setNoAi(true); foe.snapTo(centre.x, centre.y, centre.z + 1.6, 0, 0); p.level().addFreshEntity(foe);
                    LessonPackCasting.forgetRefusal(p);
                });
                context.waitTicks(20); // past the paid cast's lock, so the next press reaches the rest check
                context.getInput().pressKey(key[0]); context.waitTicks(2);
                server.runOnServer(s -> {
                    var p = player(s); String refusal = LessonPackCasting.lastRefusal(p);
                    check(payments == 1 && refusal != null && refusal.contains("rests") && refusal.contains("shared across slots"),
                        "A press during rest neither pays nor re-places, and says it rests; got " + refusal);
                    // The same lesson in another slot shares that rest.
                    Spellbooks.set(p, Spellbooks.get(p).withSpell(3, Lesson.TOLLGATE.ids)); LessonPackCasting.forgetRefusal(p);
                    down(p, 3);
                    refusal = LessonPackCasting.lastRefusal(p);
                    check(payments == 1 && refusal != null && refusal.contains("rests"), "Tollgate in another slot shares the rest; got " + refusal);
                    Spellbooks.set(p, Spellbooks.get(p).withSpell(3, List.of()));
                    foe.snapTo(foe.getX(), foe.getY(), foe.getZ() - 1.4, 0, 0); foe.setDeltaMovement(Vec3.ZERO);
                    tollFrom = foe.getZ();
                });
                context.waitTicks(3);
                server.runOnServer(s -> {
                    var p = player(s);
                    check(foe.hasEffect(MobEffects.SLOWNESS), "A walking foe at the threshold is tolled and slowed");
                    check(foe.getZ() - tollFrom > .05 || foe.getDeltaMovement().z > .05,
                        "The toll throws it back to the side it came from: z " + tollFrom + " -> " + foe.getZ() + ", dz " + foe.getDeltaMovement().z);
                    check(MasterStudies.practiced(p, Lesson.TOLLGATE), "The first real toll records practice");
                    foe.discard();
                });
                context.takeScreenshot(TestScreenshotOptions.of("lesson_pack_tollgate_cast").disableCounterPrefix());
                server.runOnServer(s -> Spellbooks.setMana(player(s), 0));
                context.getInput().holdKey((KeyMapping) context.computeOnClient(mc -> mc.options.keyShift)); context.waitTicks(2);
                context.getInput().pressKey(key[0]); context.waitTicks(3);
                context.getInput().releaseKey((KeyMapping) context.computeOnClient(mc -> mc.options.keyShift)); context.waitTicks(2);
                server.runOnServer(s -> check(!LessonPackCasting.standing(player(s), Lesson.TOLLGATE) && payments == 1 && Spellbooks.mana(player(s)) < 30,
                    "Sneak plus Cast lifts the gate without a refund"));
                server.runOnServer(s -> {
                    var p = player(s); clearRest(p, Lesson.TOLLGATE, 0);
                    Spellbooks.setMana(p, 0); LessonPackCasting.forgetRefusal(p);
                    down(p, 0);
                    String refusal = LessonPackCasting.lastRefusal(p);
                    check(!LessonPackCasting.standing(p, Lesson.TOLLGATE) && payments == 1 && Spellbooks.mana(p) == 0 && refusal != null && refusal.contains("mana"),
                        "Without the mana the gate is refused and never overcast; got " + refusal);
                });
                context.waitTicks(5);
                server.runOnServer(s -> {
                    var p = player(s); Spellbooks.setMana(p, 200);
                    down(p, 0);
                    check(LessonPackCasting.standing(p, Lesson.TOLLGATE) && payments == 2, "With the mana back, the gate is placed and paid once");
                });
                context.waitTicks(3);
                server.runOnServer(s -> {
                    var p = player(s); float before = Spellbooks.mana(p);
                    LessonPackCasting.input(p, RelayInputRules.CANCEL, 0, ++nonce);
                    check(!LessonPackCasting.standing(p, Lesson.TOLLGATE) && Spellbooks.mana(p) == before && payments == 2,
                        "Lifting refunds exactly nothing: " + before + " -> " + Spellbooks.mana(p));
                });
            } finally {
                context.runOnClient(mc -> { key[0].setKey(original[0]); KeyMapping.resetMapping(); });
            }

            // Conduit: a foe beside the rod grounds it; once clear, the spark arrives.
            server.runOnServer(s -> {
                var p = player(s);
                Spellbooks.set(p, Spellbooks.get(p).withSpell(1, Lesson.CONDUIT.ids).withSpell(2, Lesson.LIFELINE.ids));
                Spellbooks.setMana(p, 200); manaBefore = Spellbooks.mana(p); stand(p, 0, 14);
                down(p, 1);
                check(LessonPackCasting.standing(p, Lesson.CONDUIT) && manaBefore - Spellbooks.mana(p) == price(p, Lesson.CONDUIT), "Pillar then Conduit plants one rod for its price");
                Vec3 rod = p.getAttached(dev.wildercord.cast.LessonPackState.VIEW).at();
                check(rod.distanceTo(p.position()) > 4, "The rod stands away from the caster");
                foe = EntityTypes.ZOMBIE.create(p.level(), EntitySpawnReason.COMMAND); foe.setNoAi(true);
                foe.snapTo(rod.x + .8, rod.y, rod.z, 0, 0); p.level().addFreshEntity(foe);
            });
            context.waitTicks(20); // past the paid cast's lock
            server.runOnServer(s -> {
                var p = player(s); payments = 0; LessonPackCasting.forgetRefusal(p);
                down(p, 1);
                check(LessonPackCasting.sparking(p) && payments == 0, "A second press on the rod starts a free spark");
            });
            context.waitTicks(LessonPackRules.SPARK_TICKS + 3);
            server.runOnServer(s -> {
                var p = player(s); String refusal = LessonPackCasting.lastRefusal(p);
                check(p.position().distanceTo(FEET) < .5 && LessonPackCasting.standing(p, Lesson.CONDUIT) && !LessonPackCasting.sparking(p)
                    && refusal != null && refusal.contains("grounds"), "A foe beside the rod grounds the spark; the rod stays; got " + refusal);
                foe.discard(); payments = 0; down(p, 1);
            });
            context.waitTicks(LessonPackRules.SPARK_TICKS + 3);
            server.runOnServer(s -> {
                var p = player(s);
                check(p.position().distanceTo(FEET) > 4 && !LessonPackCasting.standing(p, Lesson.CONDUIT), "Sparking arrives on the clear rod");
                check(payments == 0 && MasterStudies.practiced(p, Lesson.CONDUIT), "Arrival is free and records practice");
                stand(p, 0, 0);
                pet = EntityTypes.WOLF.create(p.level(), EntitySpawnReason.COMMAND); check(pet != null, "A native pet exists");
                pet.tame(p); pet.setNoAi(true); pet.snapTo(.5, 150, 9.5, 180, 0); p.level().addFreshEntity(pet);
            });
            context.waitTicks(2);
            // Lifeline: thread your own pet, then reel it to your side.
            server.runOnServer(s -> {
                var p = player(s);
                RelayCircleTest.aim(p, pet.position().add(0, .4, 0));
                manaBefore = Spellbooks.mana(p);
                down(p, 2);
                check(LessonPackCasting.standing(p, Lesson.LIFELINE) && manaBefore - Spellbooks.mana(p) == price(p, Lesson.LIFELINE), "Beam then Lifeline threads the pet for its price");
            });
            context.waitTicks(LessonPackRules.CONSENT_TICKS + 5); // the ally's chance to refuse; also past the paid cast's lock
            server.runOnServer(s -> {
                var p = player(s);
                check(LessonPackCasting.standing(p, Lesson.LIFELINE), "The thread holds while the cast settles");
                payments = 0;
                down(p, 2);
                check(pet.distanceTo(p) < 2 && !LessonPackCasting.standing(p, Lesson.LIFELINE), "A second press reels the pet beside you");
                check(payments == 0 && MasterStudies.practiced(p, Lesson.LIFELINE), "The reel is free and records practice");
                pet.discard();
            });
            context.waitTicks(20);
            // Death clears what the body placed, and the respawned body starts clean.
            server.runOnServer(s -> {
                var p = player(s);
                clearRest(p, Lesson.TOLLGATE, 0); Spellbooks.setMana(p, 200); stand(p, 0, 35);
                down(p, 0);
                check(LessonPackCasting.standing(p, Lesson.TOLLGATE) && p.hasAttached(dev.wildercord.cast.LessonPackState.VIEW), "A gate stands before death");
                var uuid = p.getUUID();
                p.kill(p.level());
                check(!p.isAlive() && !LessonPackCasting.standing(p, Lesson.TOLLGATE), "Death retires the gate");
                p.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                ServerPlayer fresh = s.getPlayerList().getPlayer(uuid);
                check(fresh != null && fresh != p && fresh.isAlive() && !fresh.hasAttached(dev.wildercord.cast.LessonPackState.VIEW)
                    && !LessonPackCasting.standing(fresh, Lesson.TOLLGATE), "The respawned body carries no gate or HUD view");
            });
            context.waitTicks(5);
            context.setScreen(() -> null);
            context.waitTicks(2);
        }
    }
    private static void clearRest(ServerPlayer p, Lesson lesson, int slot) {
        p.removeAttached(LessonPackState.rest(lesson)); Spellbooks.setReadyAt(p, slot, 0);
    }

    private static void learn(ClientGameTestContext c, Lesson lesson) {
        c.setScreen(CordScreen::new); c.waitTicks(3);
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(2)));
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).packLessonPoint(lesson)));
        c.waitTicks(3);
        for (int page = 0; page < 3; page++) {
            int expected = page;
            c.runOnClient(mc -> check(mc.gui.screen() instanceof PackLessonScreen s && s.lesson() == lesson && s.pageNumber() == expected && s.isStudying(),
                lesson.name + " page " + expected + " is a live reading"));
            if (lesson == Lesson.TOLLGATE && page == 2) c.takeScreenshot(TestScreenshotOptions.of("lesson_pack_tollgate_study").disableCounterPrefix());
            c.getInput().pressKey(InputConstants.KEY_END); c.waitTicks(2);
            click(c, c.computeOnClient(mc -> ((PackLessonScreen) mc.gui.screen()).nextPagePoint()));
        }
        c.runOnClient(mc -> check(mc.gui.screen() instanceof PackLessonScreen s && !s.isStudying(), lesson.name + " reading completes"));
        c.runOnClient(mc -> mc.gui.screen().onClose()); c.waitTicks(3);
        c.setScreen(() -> null); c.waitTicks(2);
    }
    private static void equip(ClientGameTestContext c, Lesson lesson) {
        c.setScreen(CordScreen::new); c.waitTicks(3);
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(0)));
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowPoint(0)));
        for (var rune : lesson.runes) {
            c.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor(rune.name())); c.waitTicks(2);
            click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).codexPoint(rune.id())));
        }
        c.takeScreenshot(TestScreenshotOptions.of("lesson_pack_tollgate_equipped").disableCounterPrefix());
        c.setScreen(() -> null); c.waitTicks(3);
    }
    private static void click(ClientGameTestContext c, double[] point) {
        check(point != null, "Requested Grimoire, lesson or Codex control is visible");
        double scale = c.computeOnClient(mc -> mc.getWindow().getGuiScale());
        c.getInput().setCursorPos(point[0] * scale, point[1] * scale); c.waitTicks(1);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); c.waitTicks(5);
    }
    private static void stand(ServerPlayer p, float yaw, float pitch) {
        p.teleportTo(p.level(), FEET.x, FEET.y, FEET.z, Set.<Relative>of(), yaw, pitch, false);
        p.setDeltaMovement(Vec3.ZERO); p.setOnGround(true); p.setShiftKeyDown(false);
    }
    private static void down(ServerPlayer p, int slot) {
        LessonPackCasting.input(p, RelayInputRules.UP, slot, ++nonce); LessonPackCasting.input(p, RelayInputRules.DOWN, slot, ++nonce);
    }
    /** The price the caster actually pays: base mana (unit-tested exactly) after circle and mastery discounts. */
    private static int price(ServerPlayer p, Lesson lesson) {
        return dev.wildercord.player.Heart.manaCost(p, dev.wildercord.spell.SpellCompiler.compile(lesson.runes), Mastery.costFactor(p, lesson.runes));
    }
    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
    private static void check(boolean value, String text) { if (!value) throw new AssertionError(text); }
}
