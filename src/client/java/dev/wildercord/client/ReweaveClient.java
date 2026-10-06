package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ReweaveState;
import dev.wildercord.cast.RelayState;
import dev.wildercord.content.LightOption;
import dev.wildercord.net.ReweaveInput;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.ReweaveRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Fresh physical edges and essential paid-field tells. Menus release input, never cancel released magic. */
public final class ReweaveClient {
    private ReweaveClient() {}
    private static final int COUNT = dev.wildercord.gear.SpellSlots.ALL + 1;
    private static final boolean[] DOWN = new boolean[COUNT], BLOCKED = new boolean[COUNT], HANDLED = new boolean[COUNT];
    private static net.minecraft.client.player.LocalPlayer body;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static boolean playing;
    private static long nonce;
    private static KeyMapping lastBinding;
    private static final int INK = 0xB9A0EE, WARNING = 0xF0D6A0;

    public static void beginTick(Minecraft client) {
        boolean active = client.player != null && client.player.isAlive() && !client.player.isSpectator() && client.gui.screen() == null && client.isWindowActive();
        if (body != client.player || world != client.level) {
            body = client.player; world = client.level;
            java.util.Arrays.fill(DOWN, false); java.util.Arrays.fill(HANDLED, false); java.util.Arrays.fill(BLOCKED, true);
            playing = false;
        }
        if (!active) {
            if (playing) for (int i = 0; i < COUNT; i++) if (HANDLED[i]) { send(RelayInputRules.UP, i - 1); HANDLED[i] = false; }
            java.util.Arrays.fill(BLOCKED, true);
        }
        playing = active;
    }
    public static boolean key(Minecraft client, KeyMapping binding, int requested) {
        int index = requested + 1;
        boolean down = binding.isDown(), previous = DOWN[index];
        DOWN[index] = down;
        int slot = client.player == null ? -1 : requested < 0 ? Spellbooks.get(client.player).selected() : requested;
        boolean row = client.player != null && slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL
            && ReweaveRules.containsIds(Spellbooks.get(client.player).spells().get(slot));
        boolean handles = row || HANDLED[index];
        if (!handles) return false;
        if (!playing) return true;
        // A menu/focus transition clears mapped keys. Only an observed in-game release rearms this edge.
        if (!down) BLOCKED[index] = false;
        if (client.player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)) { BLOCKED[index] = down; return true; }
        if (down && !previous && !BLOCKED[index]) {
            HANDLED[index] = true; lastBinding = binding;
            send(client.player.isShiftKeyDown() ? RelayInputRules.CANCEL : RelayInputRules.DOWN, requested);
        } else if (!down && previous && HANDLED[index]) {
            send(RelayInputRules.UP, requested); HANDLED[index] = false;
        }
        return true;
    }
    private static void send(int action, int requested) {
        if (body == null || !ClientPlayNetworking.canSend(ReweaveInput.TYPE)) return;
        ReweaveState field = body.getAttached(ReweaveState.VIEW);
        ClientPlayNetworking.send(new ReweaveInput(action, requested, ++nonce, field == null ? 0 : field.fieldId()));
    }
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ReweaveClient::draw);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("reweave_hud"), (g, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;
            ReweaveState s = client.player.getAttached(ReweaveState.VIEW);
            if (s == null) return;
            long tick = s.clock(client.level.getGameTime());
            if (tick >= s.expires()) return;
            String key = (lastBinding == null ? WildercordKeys.castKey() : lastBinding.getTranslatedKeyMessage()).getString();
            String name = s.phase() == ReweaveState.DISC ? "Reweave: circle" : tick < s.warningUntil() ? "Reweave: silent warning" : "Reweave: fixed lane";
            String hint = s.blocked() ? "Sight or ward blocked; due beats are lost"
                : s.phase() == ReweaveState.DISC && tick <= s.created() + 60 ? "Release, then " + key + " to rewrite"
                : s.phase() == ReweaveState.DISC ? "Too late to rewrite; final beat remains" : "Sneak + " + key + " cancels; no refund";
            String pips = "";
            for (int beat : ReweaveRules.BEATS) pips += tick >= s.created() + beat ? "· " : "◆ ";
            String line = name + "  " + String.format(java.util.Locale.ROOT, "%.1f s", (s.expires() - tick) / 20.0) + "  " + pips;
            int width = Math.min(Math.max(client.font.width(line), client.font.width(hint)) + 12, g.guiWidth() - 8);
            int x = (g.guiWidth() - width) / 2, y = g.guiHeight() - (client.player.hasAttached(RelayState.VIEW) ? 148 : 103);
            g.fill(x, y, x + width, y + 33, 0xCE1C1828);
            g.text(client.font, client.font.plainSubstrByWidth(line, width - 8), x + 4, y + 4, 0xFFF0E7FF, true);
            g.text(client.font, client.font.plainSubstrByWidth(hint, width - 8), x + 4, y + 16, 0xFFD8C5ED, true);
        });
    }
    private static void line(Minecraft client, Vec3 from, Vec3 to, int color) {
        Vec3 d = to.subtract(from);
        client.level.addParticle(new LightOption(LightOption.RAY, color, (float)d.x, (float)d.y, (float)d.z, .024F, 0, 0, 0, 5), from.x, from.y, from.z, 0, 0, 0);
    }
    private static void draw(Minecraft client) {
        if (client.level == null || client.player == null || client.level.getGameTime() % 4 != 0) return;
        var nearby = new java.util.ArrayList<>(client.level.players());
        nearby.sort(java.util.Comparator.comparingDouble(p -> p == client.player ? -1 : p.distanceToSqr(client.player)));
        int shown = 0;
        for (var p : nearby) {
            ReweaveState s = p.getAttached(ReweaveState.VIEW);
            if (s == null || s.clock(client.level.getGameTime()) >= s.expires() || p.distanceToSqr(client.player) > 64 * 64 || shown >= 16) continue;
            shown++;
            Vec3 at = s.center();
            int color = s.blocked() ? 0x887B96 : s.phase() == ReweaveState.WARNING && s.clock(client.level.getGameTime()) < s.warningUntil() ? WARNING : INK;
            if (s.phase() == ReweaveState.DISC) {
                client.level.addParticle(new LightOption(LightOption.RING, color, 2, 2, 0, .022F, 0, -90, 0, 5), at.x, at.y, at.z, 0, 0, 0);
            } else {
                Vec3 d = s.end().subtract(at).normalize(), side = new Vec3(-d.z, 0, d.x).scale(ReweaveRules.LANE_WIDTH / 2);
                line(client, at.add(side), s.end().add(side), color); line(client, at.subtract(side), s.end().subtract(side), color);
                line(client, at.add(side), at.subtract(side), color); line(client, s.end().add(side), s.end().subtract(side), color);
            }
        }
    }
}
