package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.ClashRules;
import dev.wildercord.aura.Clashes;
import dev.wildercord.aura.Lineage;
import dev.wildercord.aura.Spars;
import dev.wildercord.content.WildercordSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Compact, server-confirmed rhythm and consent cues, clear of the aiming area. */
public final class AuraSocialClient {
    private AuraSocialClient() {}
    private static final int GOLD = 0xFFE8C46A, TEXT = 0xFFE8E0F0, DIM = 0xFFA49AB3;
    private static final class Lock {
        final Clashes.Begin cue;
        final ClashRules.Grade[][] grades = new ClashRules.Grade[2][ClashRules.BEATS];
        int a, b, outcome = -1, sounded = -1;
        long ended;
        final boolean[][] pressed = new boolean[2][ClashRules.BEATS];
        Lock(Clashes.Begin cue) { this.cue = cue; }
    }
    private static final Map<Integer, Lock> LOCKS = new HashMap<>();
    public static int showing() { return LOCKS.size(); }
    public static int ownId() { Lock lock = own(); return lock == null || lock.outcome >= 0 ? -1 : lock.cue.id(); }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Clashes.Begin.TYPE, (cue, ctx) -> {
            var player = ctx.client().player;
            if (player == null || cue.a() != player.getId() && cue.b() != player.getId()) return;
            if (LOCKS.size() >= 32) LOCKS.clear();
            LOCKS.put(cue.id(), new Lock(cue));
        });
        ClientPlayNetworking.registerGlobalReceiver(Clashes.Mark.TYPE, (cue, ctx) -> {
            Lock lock = LOCKS.get(cue.id());
            if (lock == null) return;
            lock.a = cue.scoreA(); lock.b = cue.scoreB();
            if (cue.side() >= 0 && cue.side() < 2 && cue.beat() >= 0 && cue.beat() < ClashRules.BEATS)
                lock.grades[cue.side()][cue.beat()] = ClashRules.Grade.of(cue.grade());
        });
        ClientPlayNetworking.registerGlobalReceiver(Clashes.End.TYPE, (cue, ctx) -> {
            Lock lock = LOCKS.get(cue.id());
            if (lock == null || ctx.client().level == null) return;
            lock.a = cue.scoreA(); lock.b = cue.scoreB(); lock.outcome = cue.outcome();
            lock.ended = ctx.client().level.getGameTime();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> LOCKS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null) { LOCKS.clear(); return; }
            long now = mc.level.getGameTime();
            LOCKS.values().removeIf(lock -> lock.outcome >= 0 ? now - lock.ended > 24 : now - lock.cue.start() > ClashRules.serverLength() + 40);
            Lock lock = own();
            if (lock == null || lock.outcome >= 0) return;
            int age = (int)(now - lock.cue.start());
            for (int beat = 0; beat < ClashRules.BEATS; beat++) {
                if (age >= ClashRules.beat(beat) && lock.sounded < beat) {
                    lock.sounded = beat;
                    sound("aura_clash_beat", 0.65F, 0.95F + 0.08F * beat);
                }
            }
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Wildercord.id("aura_social"), AuraSocialClient::draw);
    }

    private static Lock own() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        int id = mc.player.getId();
        return LOCKS.values().stream().filter(lock -> lock.cue.a() == id || lock.cue.b() == id).findFirst().orElse(null);
    }

    /** Consume an attack as one rhythm press, never as an ordinary sword-string swing. */
    public static boolean press() {
        Minecraft mc = Minecraft.getInstance();
        Lock lock = own();
        if (lock == null || lock.outcome >= 0 || mc.player == null || mc.level == null || mc.gui.screen() != null) return false;
        if (mc.level.getGameTime() - lock.cue.start() > ClashRules.serverLength()) return false;
        ClientPlayNetworking.send(new Clashes.Press(lock.cue.id()));
        int side = lock.cue.a() == mc.player.getId() ? 0 : 1;
        boolean edge = (lock.cue.flags() & (side == 0 ? Clashes.Begin.EDGE_A : Clashes.Begin.EDGE_B)) != 0;
        int age = (int)(mc.level.getGameTime() - lock.cue.start());
        int beat = ClashRules.beatFor(age, edge);
        ClashRules.Grade grade = beat < 0 || (lock.grades[side][beat] != null || lock.pressed[side][beat]) ? ClashRules.Grade.FUMBLE : ClashRules.judge(age - ClashRules.beat(beat), edge);
        if (beat >= 0) lock.pressed[side][beat] = true;
        sound(grade == ClashRules.Grade.PERFECT ? "aura_clash_perfect" : grade == ClashRules.Grade.GOOD ? "aura_clash_good" : "aura_clash_miss", 0.7F, 1);
        return true;
    }

    private static void sound(String name, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        var sound = WildercordSounds.kit(name);
        if (sound != null && mc.level != null && mc.player != null)
            mc.level.playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.screen() != null || mc.player.isSpectator()) return;
        Lock lock = own();
        int width = Math.min(238, g.guiWidth() - 16), x = (g.guiWidth() - width) / 2;
        // Clear the vanilla action bar as well as the hotbar and aiming area.
        int y = g.guiHeight() >= 340 ? Math.max(g.guiHeight() / 2 + 30, g.guiHeight() - 142) : Math.max(6, g.guiHeight() / 2 - 100);
        if (lock != null) {
            panel(g, x, y, width, 68);
            int side = lock.cue.a() == mc.player.getId() ? 0 : 1;
            text(g, lock.outcome < 0 ? "aura.wildercord.clash.hint" : lock.outcome == ClashRules.Outcome.EVEN.ordinal() ? "aura.wildercord.clash.even"
                : lock.outcome == side ? "aura.wildercord.clash.won" : "aura.wildercord.clash.lost", x + width / 2, y + 6, GOLD);
            float age = mc.level.getGameTime() - lock.cue.start() + delta.getGameTimeDeltaPartialTick(false);
            for (int beat = 0; beat < ClashRules.BEATS; beat++) {
                int cx = x + width / 2 + (beat - 1) * 54, cy = y + 29;
                float until = ClashRules.beat(beat) - age;
                int distance = Math.round(Mth.clamp(until, 0, ClashRules.GAP) * 2);
                int color = Math.abs(until) <= ClashRules.PERFECT ? GOLD : DIM;
                g.fill(cx - 2, cy - 6, cx + 2, cy + 6, color);
                if (until > 0 && until <= ClashRules.GAP) {
                    g.fill(cx - distance - 4, cy - 5, cx - distance - 2, cy + 5, TEXT);
                    g.fill(cx + distance + 2, cy - 5, cx + distance + 4, cy + 5, TEXT);
                }
                grade(g, lock.grades[side][beat], cx - 22, y + 42);
                grade(g, lock.grades[1 - side][beat], cx - 22, y + 53);
            }
            g.text(mc.font, (side == 0 ? lock.a : lock.b) + " : " + (side == 0 ? lock.b : lock.a), x + 8, y + 29, TEXT, false);
            return;
        }
        var rite = mc.player.getAttached(Lineage.RITE);
        var spar = Spars.bout(mc.player);
        var offer = mc.player.getAttached(Spars.OFFER);
        if (rite != null) {
            panel(g, x, y + 26, width, 40);
            text(g, rite.kind() == Lineage.Rite.LESSON ? "aura.wildercord.lineage.lesson" : "aura.wildercord.lineage.rite", x + width / 2, y + 32, GOLD, rite.other());
            g.fill(x + 8, y + 48, x + width - 8, y + 51, 0xFF40384E);
            g.fill(x + 8, y + 48, x + 8 + Math.round((width - 16) * rite.progress(mc.level.getGameTime())), y + 51, 0xFF000000 | rite.color());
            text(g, "aura.wildercord.lineage.cancel_hint", x + width / 2, y + 55, DIM);
        } else if (spar != null) {
            panel(g, x, y + 26, width, 40);
            long now = mc.level.getGameTime();
            String key = spar.over() ? "aura.wildercord.spar.result." + spar.result() : spar.counting(now) ? "aura.wildercord.spar.countdown" : "aura.wildercord.spar.active";
            text(g, key, x + width / 2, y + 32, GOLD, spar.counting(now) ? (spar.count() - (now - spar.start()) + 19) / 20 : spar.partnerName());
            double distance = mc.player.position().subtract(spar.centre()).multiply(1, 0, 1).length();
            if (!spar.over()) text(g, "aura.wildercord.spar.exit", x + width / 2, y + 47, distance > spar.radius() - 1 ? 0xFFFFBC89 : DIM);
        } else if (offer != null && offer.until() > mc.level.getGameTime()) {
            panel(g, x, y + 26, width, 40);
            text(g, "aura.wildercord.spar.offer", x + width / 2, y + 32, GOLD, offer.name());
            text(g, "aura.wildercord.spar.accept", x + width / 2, y + 47, DIM);
        }
    }

    private static void grade(GuiGraphicsExtractor g, ClashRules.Grade grade, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        Component label = grade == null ? Component.literal("...") : Component.translatable("aura.wildercord.clash.grade." + grade.name().toLowerCase(java.util.Locale.ROOT));
        g.text(mc.font, label, x, y, grade == ClashRules.Grade.PERFECT ? GOLD : grade == ClashRules.Grade.GOOD ? TEXT : DIM, false);
    }
    private static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xD51B1725);
        g.fill(x, y, x + width, y + 1, GOLD);
        g.fill(x, y + height - 1, x + width, y + height, 0xFF65553A);
    }
    private static void text(GuiGraphicsExtractor g, String key, int x, int y, int color, Object... args) {
        Minecraft mc = Minecraft.getInstance();
        String line = mc.font.plainSubstrByWidth(Component.translatable(key, args).getString(), Math.min(222, g.guiWidth() - 32));
        g.centeredText(mc.font, Component.literal(line), x, y, color);
    }
}
