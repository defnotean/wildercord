package dev.wildercord.client;

import dev.wildercord.client.fx.MagicQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Local preferences, accessible from an assignable key in Minecraft's Controls screen: how magic looks (left: the profiles, your
 * spells' and others' detail, flash, camera motion and spell titles) and how aura feels and this player casts (right: blade trails,
 * the body's aura, impacts, technique banners and the sword string indicator, then sigil tracing, its assist and whose
 * incantations show). On a screen too narrow for two columns they stack; on a short one the rows close up.
 */
public final class MagicSettingsScreen extends Screen {
 public MagicSettingsScreen() { super(Component.literal("Magic visuals")); }

 /** The top of the buttons, where the title sits above them. */
 private int top;

 @Override protected void init() {
  boolean twoColumns = width >= 470;
  int leftRows = 6, rightRows = 8;
  int rows = (twoColumns ? Math.max(leftRows, rightRows) : leftRows + rightRows) + 2;
  // Rows close up a little on a short screen, so everything still fits under the title.
  int pitch = height < rows * 25 + 40 ? 22 : 25;
  top = Math.max(24, height / 2 - rows * pitch / 2);
  int x = twoColumns ? width / 2 - 225 : width / 2 - 110, y = top;
  for(int i=0;i<3;i++){String preset=java.util.List.of("performance","balanced","cinematic").get(i);
   addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile."+preset),b->{MagicQuality.preset(preset);rebuildWidgets();}).bounds(x+i*74,y,72,20).build());}
  addRenderableWidget(Button.builder(Component.literal("Your spells: " + MagicQuality.own), b -> {
   MagicQuality.own = MagicQuality.own.next(); MagicQuality.save(); b.setMessage(Component.literal("Your spells: " + MagicQuality.own));
  }).bounds(x, y + pitch, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Other spells: " + MagicQuality.others), b -> {
   MagicQuality.others = MagicQuality.others.next(); MagicQuality.save(); b.setMessage(Component.literal("Other spells: " + MagicQuality.others));
  }).bounds(x, y + pitch * 2, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Reduced flash: " + MagicQuality.reducedFlash), b -> {
   MagicQuality.reducedFlash = !MagicQuality.reducedFlash; MagicQuality.save(); b.setMessage(Component.literal("Reduced flash: " + MagicQuality.reducedFlash));
  }).bounds(x, y + pitch * 3, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Camera motion: " + MagicQuality.cameraShake), b -> {
   MagicQuality.cameraShake = !MagicQuality.cameraShake; MagicQuality.save(); b.setMessage(Component.literal("Camera motion: " + MagicQuality.cameraShake));
  }).bounds(x, y + pitch * 4, 220, 20).build());
  addRenderableWidget(Button.builder(titles(), b -> {
   MagicQuality.spellTitles = !MagicQuality.spellTitles; MagicQuality.save(); b.setMessage(titles());
  }).bounds(x, y + pitch * 5, 220, 20).build());
  // Aura's feel, then casting: the right-hand column, or under the visuals on a narrow screen.
  int cx = twoColumns ? width / 2 + 5 : x, cy = twoColumns ? y : y + pitch * leftRows;
  addRenderableWidget(Button.builder(choice("blade_trails", MagicQuality.bladeTrails), b -> {
   MagicQuality.bladeTrails = MagicQuality.bladeTrails.next(); MagicQuality.save(); b.setMessage(choice("blade_trails", MagicQuality.bladeTrails));
  }).bounds(cx, cy, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.blade_trails.tip"))).build());
  addRenderableWidget(Button.builder(choice("body_aura", MagicQuality.bodyAura), b -> {
   MagicQuality.bodyAura = MagicQuality.bodyAura.next(); MagicQuality.save(); b.setMessage(choice("body_aura", MagicQuality.bodyAura));
  }).bounds(cx, cy + pitch, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.body_aura.tip"))).build());
  addRenderableWidget(Button.builder(choice("impact", MagicQuality.impact), b -> {
   MagicQuality.impact = MagicQuality.impact.next(); MagicQuality.save(); b.setMessage(choice("impact", MagicQuality.impact));
  }).bounds(cx, cy + pitch * 2, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.impact.tip"))).build());
  addRenderableWidget(Button.builder(choice("banners", MagicQuality.banners), b -> {
   MagicQuality.banners = MagicQuality.banners.next(); MagicQuality.save(); b.setMessage(choice("banners", MagicQuality.banners));
  }).bounds(cx, cy + pitch * 3, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.banners.tip"))).build());
  // Where the sword string indicator shows (by the crosshair, by the hotbar, or hidden).
  addRenderableWidget(Button.builder(stringIndicator(), b -> {
   MagicQuality.stringIndicator = MagicQuality.stringIndicator.next(); MagicQuality.save(); b.setMessage(stringIndicator());
  }).bounds(cx, cy + pitch * 4, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.string_indicator.tip"))).build());
  CastingOptions.ensureLoaded();
  addRenderableWidget(Button.builder(CastingOptions.tracingLabel(), b -> {
   CastingOptions.tracing = !CastingOptions.tracing; CastingOptions.save(); b.setMessage(CastingOptions.tracingLabel());
  }).bounds(cx, cy + pitch * 5, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.tracing.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.assistLabel(), b -> {
   CastingOptions.assist = CastingOptions.assist.next(); CastingOptions.save(); b.setMessage(CastingOptions.assistLabel());
  }).bounds(cx, cy + pitch * 6, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.assist.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.incantationsLabel(), b -> {
   CastingOptions.incantations = CastingOptions.incantations.next(); CastingOptions.save(); b.setMessage(CastingOptions.incantationsLabel());
  }).bounds(cx, cy + pitch * 7, 220, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.incantations.tip"))).build());
  int by = twoColumns ? y + pitch * rightRows + 6 : y + pitch * (leftRows + rightRows) + 6, bx = width / 2 - 110;
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile.benchmark"),b->{dev.wildercord.client.fx.FrameBenchmark.start();onClose();}).bounds(bx,by,220,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(bx, by + pitch, 220, 20).build());
 }
 /** One of aura's choices: "Blade trails: full". */
 private static Component choice(String key, Enum<?> value) {
  return Component.translatable("screen.wildercord." + key, Component.translatable("screen.wildercord." + key + "." + value.name().toLowerCase(Locale.ROOT)));
 }
 /** Where the sword string indicator shows. */
 private static Component stringIndicator() {
  return Component.translatable("screen.wildercord.string_indicator",
   Component.translatable("screen.wildercord.string_indicator." + MagicQuality.stringIndicator.name().toLowerCase(Locale.ROOT)));
 }
 /** The spell titles switch: the names of mastered spells, shown briefly by whoever casts them. */
 private static Component titles() {
  return Component.translatable(MagicQuality.spellTitles ? "screen.wildercord.spell_titles.on" : "screen.wildercord.spell_titles.off");
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
  super.extractRenderState(g, mouseX, mouseY, partial);
  g.centeredText(font, title, width / 2, top - 14, 0xFFE8C46A);
 }
}
