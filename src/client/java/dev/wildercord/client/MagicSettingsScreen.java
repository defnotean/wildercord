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
 * incantations show). The columns and preset buttons narrow to fit Minecraft's minimum GUI; rows close up on short screens.
 */
public final class MagicSettingsScreen extends Screen {
 private final Screen returnTo;
 public MagicSettingsScreen(){this(null);}
 public MagicSettingsScreen(Screen returnTo){super(Component.literal("Magic visuals"));this.returnTo=returnTo;}
 @Override public void onClose(){if(returnTo!=null)minecraft.gui.setScreen(returnTo);else super.onClose();}

 /** The top of the buttons, where the title sits above them. */
 private int top;

 @Override protected void init() {
  // Minecraft's minimum GUI is320x240. Stacking fourteen rows cannot fit that height.
  // Keep both columns, narrow their buttons, and use the actual vanilla button text scrolling.
  int columnWidth=Math.min(220,(width-34)/2), columnGap=10;
  int leftRows=7,rightRows=8,rows=Math.max(leftRows,rightRows)+2;
  int footerGap=height>=280?6:2;
  int pitch=Math.max(20,Math.min(25,(height-24-8-20-footerGap)/(rows-1)));
  int contentHeight=(rows-1)*pitch+20+footerGap;
  top=Math.max(24,(height-contentHeight)/2);
  int x=(width-columnWidth*2-columnGap)/2,y=top;
  for(int i=0;i<3;i++){String preset=java.util.List.of("performance","balanced","cinematic").get(i);
   int presetLeft=i*(columnWidth+2)/3,presetRight=(i+1)*(columnWidth+2)/3-2;
   var label=Component.translatable("screen.wildercord.profile."+preset);
   addRenderableWidget(Button.builder(label,b->{MagicQuality.preset(preset);rebuildWidgets();}).bounds(x+presetLeft,y,presetRight-presetLeft,20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.profile.tip"))).build());}
  addRenderableWidget(Button.builder(Component.literal("Your spells: " + MagicQuality.own), b -> {
   MagicQuality.own = MagicQuality.own.next(); MagicQuality.save(); b.setMessage(Component.literal("Your spells: " + MagicQuality.own));
  }).bounds(x, y + pitch, columnWidth, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Other spells: " + MagicQuality.others), b -> {
   MagicQuality.others = MagicQuality.others.next(); MagicQuality.save(); b.setMessage(Component.literal("Other spells: " + MagicQuality.others));
  }).bounds(x, y + pitch * 2, columnWidth, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Reduced flash: " + MagicQuality.reducedFlash), b -> {
   MagicQuality.reducedFlash = !MagicQuality.reducedFlash; MagicQuality.save(); b.setMessage(Component.literal("Reduced flash: " + MagicQuality.reducedFlash));
  }).bounds(x, y + pitch * 3, columnWidth, 20).build());
  addRenderableWidget(Button.builder(Component.literal("Camera motion: " + MagicQuality.cameraShake), b -> {
   MagicQuality.cameraShake = !MagicQuality.cameraShake; dev.wildercord.client.fx.ScreenEffects.clearCameraMotion(); MagicQuality.save(); b.setMessage(Component.literal("Camera motion: " + MagicQuality.cameraShake));
  }).bounds(x, y + pitch * 4, columnWidth, 20).build());
  addRenderableWidget(Button.builder(titles(), b -> {
   MagicQuality.spellTitles = !MagicQuality.spellTitles; MagicQuality.save(); b.setMessage(titles());
  }).bounds(x, y + pitch * 5, columnWidth, 20).build());
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.combat.title"),b->minecraft.gui.setScreen(new CombatPresentationScreen(this))).bounds(x,y+pitch*6,columnWidth,20).build());
  // Aura and casting share the second column at every supported GUI size.
  int cx=x+columnWidth+columnGap,cy=y;
  addRenderableWidget(Button.builder(choice("blade_trails", MagicQuality.bladeTrails), b -> {
   MagicQuality.bladeTrails = MagicQuality.bladeTrails.next(); MagicQuality.save(); b.setMessage(choice("blade_trails", MagicQuality.bladeTrails));
  }).bounds(cx, cy, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.blade_trails.tip"))).build());
  addRenderableWidget(Button.builder(choice("body_aura", MagicQuality.bodyAura), b -> {
   MagicQuality.bodyAura = MagicQuality.bodyAura.next(); MagicQuality.save(); b.setMessage(choice("body_aura", MagicQuality.bodyAura));
  }).bounds(cx, cy + pitch, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.body_aura.tip"))).build());
  addRenderableWidget(Button.builder(choice("impact", MagicQuality.impact), b -> {
   MagicQuality.impact = MagicQuality.impact.next(); dev.wildercord.client.fx.HitStop.clear(); dev.wildercord.client.fx.ScreenEffects.clearCameraMotion(); MagicQuality.save(); b.setMessage(choice("impact", MagicQuality.impact));
  }).bounds(cx, cy + pitch * 2, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.impact.tip"))).build());
  addRenderableWidget(Button.builder(choice("banners", MagicQuality.banners), b -> {
   MagicQuality.banners = MagicQuality.banners.next(); MagicQuality.save(); b.setMessage(choice("banners", MagicQuality.banners));
  }).bounds(cx, cy + pitch * 3, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.banners.tip"))).build());
  // Where the sword string indicator shows (by the crosshair, by the hotbar, or hidden).
  addRenderableWidget(Button.builder(stringIndicator(), b -> {
   MagicQuality.stringIndicator = MagicQuality.stringIndicator.next(); MagicQuality.save(); b.setMessage(stringIndicator());
  }).bounds(cx, cy + pitch * 4, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.string_indicator.tip"))).build());
  CastingOptions.ensureLoaded();
  addRenderableWidget(Button.builder(CastingOptions.tracingLabel(), b -> {
   CastingOptions.tracing = !CastingOptions.tracing; CastingOptions.save(); b.setMessage(CastingOptions.tracingLabel());
  }).bounds(cx, cy + pitch * 5, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.tracing.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.assistLabel(), b -> {
   CastingOptions.assist = CastingOptions.assist.next(); CastingOptions.save(); b.setMessage(CastingOptions.assistLabel());
  }).bounds(cx, cy + pitch * 6, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.assist.tip"))).build());
  addRenderableWidget(Button.builder(CastingOptions.incantationsLabel(), b -> {
   CastingOptions.incantations = CastingOptions.incantations.next(); CastingOptions.save(); b.setMessage(CastingOptions.incantationsLabel());
  }).bounds(cx, cy + pitch * 7, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("screen.wildercord.casting.incantations.tip"))).build());
  int footerWidth=Math.min(220,width-24),by=y+pitch*rightRows+footerGap,bx=(width-footerWidth)/2;
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.profile.benchmark"),b->{dev.wildercord.client.fx.FrameBenchmark.start();minecraft.gui.setScreen(null);}).bounds(bx,by,footerWidth,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(bx, by + pitch, footerWidth, 20).build());
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
