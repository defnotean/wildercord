package dev.wildercord.client;

import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.Lineage;
import dev.wildercord.aura.Spars;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The lineage record, lessons, spar history, and voluntary release of either side of a bond. */
final class LineagePage {
    private static final int GOLD = 0xFFE8C46A, TEXT = 0xFFE8E0F0, DIM = 0xFF9C93AE;
    private final Minecraft mc;
    private final Font font;
    private record Button(int x, int y, int width, String key, UUID other) {}
    private final List<Button> buttons = new ArrayList<>();
    final Map<String, int[]> targets = new HashMap<>();
    private UUID confirming;
    private long confirmUntil;
    private int page;
    private boolean honoured;

    LineagePage(Minecraft mc, Font font) { this.mc = mc; this.font = font; }
    List<Component> draw(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, int top) {
        buttons.clear(); targets.clear();
        long now = player.level().getGameTime();
        if (now > confirmUntil) confirming = null;
        var view = Lineage.view(player);
        var log = Spars.log(player);
        g.text(font, Component.translatable("screen.wildercord.aura.lineage.title"), 14, top + 5, GOLD, true);
        g.text(font, Component.translatable("screen.wildercord.aura.lineage.record", log.wins(), log.losses(), log.evens()), 14, top + 18, DIM, false);
        int y = top + 34;
        g.fill(14, y, 306, y + 43, 0xFF241E31);
        if (view.master().isPresent()) {
            var master = view.master().get();
            g.text(font, Component.translatable("screen.wildercord.aura.lineage.master"), 20, y + 5, GOLD, false);
            entry(g, master, 20, y + 18, 175);
            button(g, mx, my, 242, y + 15, 58, "release", master.id());
            String lesson = view.lessonAt() == Long.MIN_VALUE || view.lessonAt() <= now ? "screen.wildercord.aura.lineage.lesson_ready" : "screen.wildercord.aura.lineage.lesson_wait";
            g.text(font, Component.translatable(lesson, Math.max(0, (view.lessonAt() - now + 1199) / 1200)), 20, y + 31, DIM, false);
        } else {
            g.text(font, Component.translatable("screen.wildercord.aura.lineage.no_master"), 20, y + 7, TEXT, false);
            g.text(font, Component.translatable("screen.wildercord.aura.lineage.ask"), 20, y + 22, DIM, false);
        }
        y += 51;
        button(g, mx, my, 14, y, 144, "disciples", null);
        button(g, mx, my, 162, y, 144, "honoured", null);
        y += 20;
        List<Lineage.Entry> rows = honoured ? view.honoured() : view.disciples();
        page = Math.max(0, Math.min(page, Math.max(0, (rows.size() - 1) / 5)));
        if (rows.isEmpty()) g.text(font, Component.translatable("screen.wildercord.aura.lineage.empty"), 20, y + 8, DIM, false);
        for (int i = page * 5; i < Math.min(rows.size(), page * 5 + 5); i++) {
            var entry = rows.get(i);
            int rowY = y + (i % 5) * 26;
            g.fill(14, rowY, 306, rowY + 23, i % 2 == 0 ? 0xFF282233 : 0xFF211C2D);
            entry(g, entry, 20, rowY + 4, honoured ? 270 : 208);
            g.text(font, Component.translatable(honoured ? "screen.wildercord.aura.lineage.graduated" : "screen.wildercord.aura.lineage.since", entry.since() / 24000 + 1), 20, rowY + 14, DIM, false);
            if (!honoured) button(g, mx, my, 242, rowY + 4, 58, "release", entry.id());
        }
        int footer = top + 237;
        if (rows.size() > 5) {
            button(g, mx, my, 14, footer, 36, "previous", null);
            button(g, mx, my, 270, footer, 36, "next", null);
        }
        g.centeredText(font, Component.translatable("screen.wildercord.aura.lineage.capacity", view.disciples().size(), view.most()), 160, footer + 3, DIM);
        int helpY = footer + 22;
        Component help = Component.translatable(view.on() ? "screen.wildercord.aura.lineage.how" : "screen.wildercord.aura.lineage.off");
        for (var line : font.split(help, 286)) {
            if (helpY > 325) break;
            g.text(font, line, 17, helpY, DIM, false); helpY += 10;
        }
        return null;
    }

    private void entry(GuiGraphicsExtractor g, Lineage.Entry entry, int x, int y, int max) {
        int state = entry.near() ? GOLD : entry.online() ? 0xFF88BD9B : DIM;
        g.fill(x, y + 2, x + 4, y + 6, state);
        String stage = entry.stage() > 0 ? Component.translatable("aura.wildercord.stage." + AuraStages.id(entry.stage())).getString() : "";
        g.text(font, font.plainSubstrByWidth(entry.name() + (stage.isEmpty() ? "" : " · " + stage), max - 9), x + 8, y, 0xFF000000 | entry.color(), false);
    }

    private void button(GuiGraphicsExtractor g, int mx, int my, int x, int y, int width, String key, UUID other) {
        String label = "screen.wildercord.aura.lineage." + (other != null && other.equals(confirming) ? "confirm" : key);
        boolean hover = mx >= x && mx < x + width && my >= y && my < y + 14;
        g.fill(x, y, x + width, y + 14, hover ? 0xFF53415A : 0xFF382D42);
        g.centeredText(font, Component.translatable(label), x + width / 2, y + 3, hover ? GOLD : TEXT);
        buttons.add(new Button(x, y, width, key, other));
        targets.put(other == null ? key : "release:" + other, new int[] {x, y, width, 14});
    }

    boolean click(int x, int y) {
        for (Button button : buttons) {
            if (x < button.x || x >= button.x + button.width || y < button.y || y >= button.y + 14) continue;
            if (button.other != null) {
                if (button.other.equals(confirming) && mc.level.getGameTime() <= confirmUntil) {
                    ClientPlayNetworking.send(new Lineage.End(button.other)); confirming = null;
                } else { confirming = button.other; confirmUntil = mc.level.getGameTime() + 100; }
            } else switch (button.key) {
                case "disciples" -> { honoured = false; page = 0; confirming = null; }
                case "honoured" -> { honoured = true; page = 0; confirming = null; }
                case "previous" -> page = Math.max(0, page - 1);
                case "next" -> page++;
            }
            return true;
        }
        return false;
    }
}
