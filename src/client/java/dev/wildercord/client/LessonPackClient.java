package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.LessonPackState;
import dev.wildercord.net.LessonPackInput;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.RelayInputRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Fresh physical edges for the pack lessons, and the owner's readout of the object they placed. */
public final class LessonPackClient {
    private LessonPackClient() {}
    private static final int COUNT = dev.wildercord.gear.SpellSlots.ALL + 1;
    private static final boolean[] DOWN = new boolean[COUNT], BLOCKED = new boolean[COUNT];
    private static net.minecraft.client.player.LocalPlayer body;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static boolean playing;
    private static long nonce;
    private static KeyMapping lastBinding;

    public static void beginTick(Minecraft client) {
        boolean active = client.player != null && client.player.isAlive() && !client.player.isSpectator() && client.gui.screen() == null && client.isWindowActive();
        if (body != client.player || world != client.level) {
            body = client.player; world = client.level;
            java.util.Arrays.fill(DOWN, false); java.util.Arrays.fill(BLOCKED, true);
        }
        if (!active) java.util.Arrays.fill(BLOCKED, true);
        playing = active;
    }
    /** Presses only: a placed gate, thread or rod outlives the key, so each press is sent whole and focus loss sends nothing. */
    public static boolean key(Minecraft client, KeyMapping binding, int requested) {
        int index = requested + 1;
        boolean down = binding.isDown(), previous = DOWN[index];
        DOWN[index] = down;
        int slot = client.player == null ? -1 : requested < 0 ? Spellbooks.get(client.player).selected() : requested;
        boolean row = client.player != null && slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL
            && LessonPackRules.containsIds(Spellbooks.get(client.player).spells().get(slot));
        if (!row) return false;
        if (!playing) { while (binding.consumeClick()) { } return true; }
        // A tap shorter than one tick is down and up between two reads; its click still counts as one press.
        boolean clicked = false;
        while (binding.consumeClick()) clicked = true;
        if (!down) BLOCKED[index] = false;
        if (client.player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)) { BLOCKED[index] = down; return true; }
        if ((down || clicked) && !previous && !BLOCKED[index]) {
            lastBinding = binding;
            if (client.player.isShiftKeyDown()) send(RelayInputRules.CANCEL, requested);
            else { send(RelayInputRules.DOWN, requested); send(RelayInputRules.UP, requested); } // a whole press: the server never waits on a release
        }
        return true;
    }
    private static void send(int action, int requested) {
        if (body != null && ClientPlayNetworking.canSend(LessonPackInput.TYPE))
            ClientPlayNetworking.send(new LessonPackInput(action, requested, ++nonce));
    }
    private static String line(LessonPackRules.Lesson lesson, String key) {
        return switch (lesson) {
            case TOLLGATE -> "Tollgate stands · halts three foes once each";
            case LIFELINE -> "Lifeline threaded · press " + key + " to reel";
            case CONDUIT -> "Conduit rod planted · press " + key + " to arrive";
        };
    }
    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("lesson_pack_hud"), (g, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;
            LessonPackState s = client.player.getAttached(LessonPackState.VIEW);
            if (s == null || s.kind() == null || s.clock(client.level.getGameTime()) >= s.until()) return;
            long left = s.until() - s.clock(client.level.getGameTime());
            String key = (lastBinding == null ? WildercordKeys.castKey() : lastBinding.getTranslatedKeyMessage()).getString();
            String line = line(s.kind(), key) + " · " + (left + 19) / 20 + "s";
            String hint = "Sneak and press " + key + " to lift it";
            int width = Math.min(Math.max(client.font.width(line), client.font.width(hint)) + 12, g.guiWidth() - 8);
            int x = (g.guiWidth() - width) / 2, y = g.guiHeight() - 115;
            g.fill(x, y, x + width, y + 28, 0xDC1B2230);
            g.text(client.font, client.font.plainSubstrByWidth(line, width - 8), x + 4, y + 4, 0xFFE3E6F2, true);
            g.text(client.font, client.font.plainSubstrByWidth(hint, width - 8), x + 4, y + 16, 0xFFBFC6D6, true);
        });
    }
}
