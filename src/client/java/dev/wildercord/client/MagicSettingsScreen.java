package dev.wildercord.client;

import dev.wildercord.client.fx.MagicQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Local preferences, accessible from an assignable key in Minecraft's Controls screen: how magic looks
 * (left) and how this player casts (right: sigil tracing, its assist, and whose incantations show). On a
 * screen too narrow for two columns they stack.
 */
public final class MagicSettingsScreen extends Screen {
 public MagicSettingsScreen() { super(Component.literal("Magic visuals")); }

 /** The top of the buttons, where the title sits above them. */
 private int top;

 @Override protected void init() {
  boolean twoColumns = width >= 470;
  int rows = twoColumns ? 7 : 12;
  top = Math.max(34, height / 2 - rows * 25 / 2);
  int x = twoColumns ? width / 2 - 225 : width / 2 - 110, y = top;
  for(int i=0;i<3;i++){String preset=java.util.List.of("performance","balanced","cinematic").get(i);
   addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile."+preset),b->{MagicQuality.preset(preset);rebuildWidgets();}).bounds(x+i*74,y,72,20).build());}
  addRenderableWidget(Button.builder(Component.literal("Your spells: " + MagicQuality.own), b -> {
   MagicQuality.own = MagicQuality.own.next(); MagicQuality.save(); b.setMessage(Component.literal("Your spells: " + MagicQuality.own));
  }).bounds(x, y + 25, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Other spells: " + MagicQuality.others), b -> {
   MagicQuality.others = MagicQuality.others.next(); MagicQuality.save(); b.setMessage(Component.literal("Other spells: " + MagicQuality.others));
  }).bounds(x, y + 50, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Reduced flash: " + MagicQuality.reducedFlash), b -> {
   MagicQuality.reducedFlash = !MagicQuality.reducedFlash; MagicQuality.save(); b.setMessage(Component.literal("Reduced flash: " + MagicQuality.reducedFlash));
  }).bounds(x, y + 75, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Camera motion: " + MagicQuality.cameraShake), b -> {
   MagicQuality.cameraShake = !MagicQuality.cameraShake; MagicQuality.save(); b.setMessage(Component.literal("Camera motion: " + MagicQuality.cameraShake));
  }).bounds(x, y + 100, 220, 20).build());
  // Where the sword string indicator shows (by the crosshair, by the hotbar, or hidden).
  addRenderableWidget(Button.builder(stringIndicator(), b -> {
   MagicQuality.stringIndicator = MagicQuality.stringIndicator.next(); MagicQuality.save(); b.setMessage(stringIndicator());
  }).bounds(x, y + 125, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.wildercord.string_indicator.tip"))).build());
  // Casting: the right-hand column, or under the visuals on a narrow screen.
  int cx = twoColumns ? width / 2 + 5 : x, cy = twoColumns ? y + 25 : y + 150;
  CastingOptions.ensureLoaded();
  addRenderableWidget(Button.builder(CastingOptions.tracingLabel(), b -> {
   CastingOptions.tracing = !CastingOptions.tracing; CastingOptions.save(); b.setMessage(CastingOptions.tracingLabel());
  }).bounds(cx, cy, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.wildercord.casting.tracing.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.assistLabel(), b -> {
   CastingOptions.assist = CastingOptions.assist.next(); CastingOptions.save(); b.setMessage(CastingOptions.assistLabel());
  }).bounds(cx, cy + 25, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.wildercord.casting.assist.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.incantationsLabel(), b -> {
   CastingOptions.incantations = CastingOptions.incantations.next(); CastingOptions.save(); b.setMessage(CastingOptions.incantationsLabel());
  }).bounds(cx, cy + 50, 220, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.wildercord.casting.incantations.tip"))).build());
  addRenderableWidget(Button.builder(titles(), b -> {
   MagicQuality.spellTitles = !MagicQuality.spellTitles; MagicQuality.save(); b.setMessage(titles());
  }).bounds(cx, cy + 75, 220, 20).build());
  int by = twoColumns ? y + 160 : y + 255, bx = width / 2 - 110;
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile.benchmark"),b->{dev.wildercord.client.fx.FrameBenchmark.start();onClose();}).bounds(bx,by,220,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(bx, by + 25, 220, 20).build());
 }
 /** Where the sword string indicator shows. */
 private static Component stringIndicator() {
  return Component.translatable("screen.wildercord.string_indicator",
   Component.translatable("screen.wildercord.string_indicator." + MagicQuality.stringIndicator.name().toLowerCase(java.util.Locale.ROOT)));
 }
 /** The spell titles switch: the names of mastered spells, shown briefly by whoever casts them. */
 private static Component titles() {
  return Component.translatable(MagicQuality.spellTitles ? "screen.wildercord.spell_titles.on" : "screen.wildercord.spell_titles.off");
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
  super.extractRenderState(g, mouseX, mouseY, partial);
  g.centeredText(font, title, width / 2, top - 16, 0xFFE8C46A);
 }
}
