package dev.wildercord.client;

import dev.wildercord.client.fx.MagicQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local preferences, accessible from an assignable key in Minecraft's Controls screen. */
public final class MagicSettingsScreen extends Screen {
 public MagicSettingsScreen() { super(Component.literal("Magic visuals")); }
 @Override protected void init() {
  int x = width / 2 - 110, y = height / 2 - 105;
  for(int i=0;i<3;i++){String preset=java.util.List.of("performance","balanced","cinematic").get(i);
   addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile."+preset),b->{MagicQuality.preset(preset);rebuildWidgets();}).bounds(x+i*74,y-25,72,20).build());}
  addRenderableWidget(Button.builder(Component.literal("Your spells: " + MagicQuality.own), b -> {
   MagicQuality.own = MagicQuality.own.next(); MagicQuality.save(); b.setMessage(Component.literal("Your spells: " + MagicQuality.own));
  }).bounds(x, y, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Other spells: " + MagicQuality.others), b -> {
   MagicQuality.others = MagicQuality.others.next(); MagicQuality.save(); b.setMessage(Component.literal("Other spells: " + MagicQuality.others));
  }).bounds(x, y + 25, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Reduced flash: " + MagicQuality.reducedFlash), b -> {
   MagicQuality.reducedFlash = !MagicQuality.reducedFlash; MagicQuality.save(); b.setMessage(Component.literal("Reduced flash: " + MagicQuality.reducedFlash));
  }).bounds(x, y + 50, 220, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Camera motion: " + MagicQuality.cameraShake), b -> {
   MagicQuality.cameraShake = !MagicQuality.cameraShake; MagicQuality.save(); b.setMessage(Component.literal("Camera motion: " + MagicQuality.cameraShake));
  }).bounds(x, y + 75, 220, 20).build());
  addRenderableWidget(Button.builder(titles(), b -> {
   MagicQuality.spellTitles = !MagicQuality.spellTitles; MagicQuality.save(); b.setMessage(titles());
  }).bounds(x, y + 100, 220, 20).build());
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile.benchmark"),b->{dev.wildercord.client.fx.FrameBenchmark.start();onClose();}).bounds(x,y+135,220,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x, y + 160, 220, 20).build());
 }
 /** The spell titles switch: the names of mastered spells, shown briefly by whoever casts them. */
 private static Component titles() {
  return Component.translatable(MagicQuality.spellTitles ? "screen.wildercord.spell_titles.on" : "screen.wildercord.spell_titles.off");
 }
 @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
  super.extractRenderState(g, mouseX, mouseY, partial);
  g.centeredText(font, title, width / 2, height / 2 - 157, 0xFFE8C46A);
 }
}
