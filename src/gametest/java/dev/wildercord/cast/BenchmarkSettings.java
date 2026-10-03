package dev.wildercord.cast;

import dev.wildercord.client.fx.MagicQuality;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InactivityFpsLimit;

/** A benchmark may select presets temporarily without leaving the user's preferences changed. */
record BenchmarkSettings(MagicQuality.Level own, MagicQuality.Level others, boolean flash, boolean shake,
                         MagicQuality.Trails trails, MagicQuality.BodyAura aura, MagicQuality.Impact impact,
                         MagicQuality.Banners banners, boolean vsync, int fps, InactivityFpsLimit inactivity,
                         boolean hiddenHud) {
    static BenchmarkSettings capture(Minecraft mc) {
        return new BenchmarkSettings(MagicQuality.own, MagicQuality.others, MagicQuality.reducedFlash,
            MagicQuality.cameraShake, MagicQuality.bladeTrails, MagicQuality.bodyAura, MagicQuality.impact,
            MagicQuality.banners, mc.options.enableVsync().get(), mc.options.framerateLimit().get(),
            mc.options.inactivityFpsLimit().get(), mc.gui.hud.isHidden());
    }

    void restore(Minecraft mc) {
        MagicQuality.own = own;
        MagicQuality.others = others;
        MagicQuality.reducedFlash = flash;
        MagicQuality.cameraShake = shake;
        MagicQuality.bladeTrails = trails;
        MagicQuality.bodyAura = aura;
        MagicQuality.impact = impact;
        MagicQuality.banners = banners;
        mc.options.enableVsync().set(vsync);
        mc.options.framerateLimit().set(fps);
        mc.options.inactivityFpsLimit().set(inactivity);
        if (mc.gui.hud.isHidden() != hiddenHud) mc.gui.hud.toggle();
        MagicQuality.save();
    }
}
