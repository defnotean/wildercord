package dev.wildercord.client.fx;

import dev.wildercord.cast.Vfx;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.net.FormationPayload;
import dev.wildercord.spell.ShapeFormation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

/** Decorative formation events; each shape has its own two-beat silhouette before the server releases it. */
public final class SpellFormations {
 private static final Vec3 UP = new Vec3(0, 1, 0);
 private static final class Active {
  final FormationPayload event; final ClientLevel world; final long start;
  boolean circleDrawn;
  Active(FormationPayload event, ClientLevel world, long start) { this.event=event; this.world=world; this.start=start; }
 }
 private static final ArrayList<Active> ACTIVE = new ArrayList<>();
 private static int ownSpent, otherSpent;
 private static long dropped;
 private SpellFormations() {}
 public static void init() {
  MagicQuality.load();
  ClientPlayNetworking.registerGlobalReceiver(FormationPayload.TYPE, (event, context) -> {
   var world = context.client().level;
   if (world == null) return;
   if (ACTIVE.size() >= 128) { dropped++; return; }
   ACTIVE.add(new Active(event, world, world.getGameTime()));
  });
  ClientTickEvents.END_CLIENT_TICK.register(SpellFormations::tick);
 }
 public static long dropped() { return dropped; }
 private static void tick(Minecraft mc) {
  if (mc.level == null) { ACTIVE.clear(); return; }
  if (mc.isPaused()) return;
  ownSpent = otherSpent = 0;
  ACTIVE.removeIf(a -> {
   if (a.world != mc.level) return true;
   long age = mc.level.getGameTime() - a.start;
   var entity = mc.level.getEntity(a.event.caster());
   if (!(entity instanceof LivingEntity caster) || !caster.isAlive() || caster.isRemoved()) return true;
   if (age > 2) return true;
   if (mc.player == null || mc.player.distanceToSqr(caster) > 64 * 64) return true;
   var quality = caster == mc.player ? MagicQuality.own : MagicQuality.others;
   Canvas c = new Canvas(mc.level, caster, a.event, quality);
   // A packet handled before the next game tick can first be drawn at age one.
   // Emit the rear circle once on its first render tick rather than requiring age zero.
   if (!a.circleDrawn) { if(a.event.circle())c.circle(); a.circleDrawn=true; }
   if (age > 0 && a.event.placement()!=FormationPayload.CIRCLE_ONLY) c.draw((int) age);
   return false;
  });
 }
 static final class Canvas {
  final ClientLevel world; final LivingEntity caster; final FormationPayload event;
  final MagicQuality.Level quality; final Vec3 forward, right, up, focus; final float yaw, pitch;
  Canvas(ClientLevel world, LivingEntity caster, FormationPayload event, MagicQuality.Level quality) {
   this.world = world; this.caster = caster; this.event = event; this.quality = quality;
   forward = (event.placement()==FormationPayload.FIXED?event.direction():caster.getLookAngle()).normalize();
   Vec3 cross = forward.cross(UP);
   right = cross.lengthSqr() < 0.0001 ? new Vec3(1, 0, 0) : cross.normalize();
   up = right.cross(forward).normalize();
   // Formation stays below the reticle and beyond the near plane. Track aim until the release tick.
   focus = event.placement()==FormationPayload.FIXED ? event.anchor()
    : event.placement()==FormationPayload.CASTER ? caster.position().add(0,.7,0)
    : event.placement()==FormationPayload.AIMED ? aimed().add(0,event.shape().equals("rain")?12:.12,0)
    : caster.getEyePosition().add(forward.scale(3.2)).subtract(up.scale(0.65));
   yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
   pitch = (float) -Math.toDegrees(Math.asin(forward.y));
  }
  Vec3 aimed() {
   Vec3 from=caster.getEyePosition(),to=from.add(forward.scale(event.aimRange()));
   var hit=world.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
   Vec3 point=hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?to:hit.getLocation();
   if(!world.hasChunkAt(net.minecraft.core.BlockPos.containing(point)))return point;
   var ground=world.clip(new net.minecraft.world.level.ClipContext(point.add(0,.5,0),point.add(0,-16,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
   return ground.getType()==net.minecraft.world.phys.HitResult.Type.MISS?point:ground.getLocation();
  }
  boolean spend() {
   int cap = quality == MagicQuality.Level.FULL ? 512 : quality == MagicQuality.Level.BALANCED ? 256 : 96;
   boolean own = caster == Minecraft.getInstance().player;
   if ((own ? ownSpent++ : otherSpent++) >= cap) { dropped++; return false; }
   return true;
  }
  void emit(ParticleOptions option, Vec3 at) {
   if (!spend()) return;
   world.addParticle(option, at.x, at.y, at.z, 0, 0, 0);
  }
  void circle() {
   Vec3 behind = caster.getEyePosition().subtract(forward.scale(1.8)).add(0, -0.3, 0);
   if (spend()) Minecraft.getInstance().particleEngine.add(new CasterCircle(world,caster,behind,
    new SpellCircleOption(event.glyphs(), event.color(), event.scale() * 0.75F, yaw, pitch, 25)));
  }
  Vec3 point(double x, double y, double z) { return focus.add(right.scale(x)).add(up.scale(y)).add(forward.scale(z)); }
  void ring(Vec3 at, double radius, boolean floor) {
   emit(new LightOption(LightOption.RING, event.color(), (float)(radius * .35), (float)radius, 0, .025F,
     floor ? 0 : yaw, floor ? -90 : pitch, 0, 6), at);
  }
  void orb(Vec3 at, double radius) {
   emit(new LightOption(LightOption.ORB, event.color(), (float)radius, 0, 0, .025F, yaw, pitch, 0, 5), at);
  }
  void line(Vec3 from, Vec3 to, boolean jagged) {
   Vec3 d = to.subtract(from);
   emit(new LightOption(jagged ? LightOption.ARC : LightOption.RAY, event.color(), (float)d.x, (float)d.y, (float)d.z,
     .022F, jagged ? 1 : yaw, jagged ? 0 : pitch, 0, 5), from);
  }
  void slash(Vec3 at, double radius, double span, double turn) {
   emit(new LightOption(LightOption.SLASH, event.color(), (float)radius, (float)span, 2, .035F, yaw, pitch, (float)turn, 6), at);
  }
  void polygon(int corners, double radius, double rotation) {
   for (int i = 0; i < corners; i++) {
    double a = rotation + i * Math.PI * 2 / corners, b = rotation + (i + 1) * Math.PI * 2 / corners;
    line(point(Math.cos(a)*radius, Math.sin(a)*radius, 0), point(Math.cos(b)*radius, Math.sin(b)*radius, 0), false);
   }
  }
  void draw(int beat) {
   double t = beat / 2.0, r = event.scale() * .55, q = r * (1.35 - .35*t);
   Vec3 feet = event.placement()==FormationPayload.FIXED?focus:caster.position().add(0, .12, 0);
   // Fully authored elemental projectiles gather their own body before launch.
   boolean campOnly=event.runes().stream().anyMatch(CampConcordForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .map(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !CampConcordForms.supports(id)).orElse(true));
   boolean fireOnly=event.runes().stream().anyMatch(FireFormations::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !FireFormations.supports(id)).isPresent());
   boolean frostOnly=event.runes().stream().anyMatch(FrostFormations::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !FrostFormations.supports(id)).isPresent());
   boolean stormOnly=event.runes().stream().anyMatch(StormFormations::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !StormFormations.supports(id)).isPresent());
   boolean windOnly=event.runes().stream().anyMatch(WindForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !WindForms.supports(id)).isPresent());
   boolean earthOnly=event.runes().stream().anyMatch(EarthForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !EarthForms.supports(id)).isPresent());
   boolean lifeOnly=event.runes().stream().anyMatch(LifeForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !LifeForms.supports(id)).isPresent());
   boolean voidOnly=event.runes().stream().anyMatch(VoidForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !VoidForms.supports(id)).isPresent());
   boolean fieldOnly=event.runes().stream().anyMatch(FieldFusionForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !FieldFusionForms.supports(id)).isPresent());
   boolean nextOnly=event.runes().stream().anyMatch(NextSignatureForms::supports)
    && event.runes().stream().noneMatch(id -> dev.wildercord.spell.Runes.get(id)
      .filter(rune -> rune.family()==dev.wildercord.spell.RuneFamily.EFFECT && !NextSignatureForms.supports(id)).isPresent());
   boolean materialProjectile=(campOnly || fireOnly || frostOnly || stormOnly || windOnly || earthOnly || lifeOnly || voidOnly || fieldOnly || nextOnly) && switch(ShapeFormation.of(event.shape())) {
    case BOLT,ARC,ORB,SPARK,COMET,RICOCHET,CLUSTER,WISP -> true;
    default -> false;
   };
   // Life Self prepares its tissue, seed or ward material directly on the caster.
   // Uncovered mixed groups retain the ordinary shape scaffold.
   boolean materialSelf=lifeOnly && ShapeFormation.of(event.shape())==ShapeFormation.SELF;
   if(!materialProjectile && !materialSelf && !campOnly) switch (ShapeFormation.of(event.shape())) {
    case SELF -> { ring(feet.add(0, t*.7, 0), .7-.25*t, true); ring(feet.add(0, 1.3-t*.4, 0), .35, true); }
    case TOUCH -> { slash(point(-q*.3, 0, 0), q*.5, Math.PI*.8, 0); line(point(q*.3, -.3, 0), point(q*.3, .3, 0), false); }
    case BOLT -> { orb(point(0, 0, -.35+.35*t), .15*t); line(point(0, 0, -.8), focus, false); }
    case BEAM -> { for (int s : new int[]{-1,1}) line(point(s*q, -.1, -.8), focus, false); ring(focus, .35-.15*t, false); }
    case BURST -> { for(int i=0;i<6;i++) { double a=i*Math.PI/3; line(focus, point(Math.cos(a)*q,Math.sin(a)*q,0),false); } }
    case ZONE -> { ring(event.placement()==FormationPayload.AIMED || event.placement()==FormationPayload.FIXED?focus:feet.add(forward.scale(3)), q*1.6, true); polygon(4, q*.7, Math.PI/4); }
    case RAIN -> {
     if(event.placement()==FormationPayload.AIMED || event.placement()==FormationPayload.FIXED) {
      for(int i=-1;i<=1;i++)line(point(i*.35,0,-.3),point(i*.35,-.9*t,0),false);
      ring(focus,q,true);ring(focus.add(0,-12,0),q*.8,true);
     } else {for(int i=-1;i<=1;i++)line(point(i*.35,1.4,-.3),point(i*.35,.5*t,0),false);ring(point(0,1.5,0),q,false);}
    }
    case ARC -> { line(point(-q,-.15,0),point(0,q,0),true); line(point(0,q,0),point(q,-.15,0),true); }
    case CONE -> { for(int i=-1;i<=1;i++) line(point(0,0,-.8),point(i*q,t*.25,.3),false); slash(focus,q,Math.PI,Math.PI); }
    case TRAIL -> { for(int i=0;i<3;i++) ring(feet.subtract(forward.scale(.4*i)),.15+.12*i,true); }
    case WALL -> { polygon(4,q,Math.PI/4); line(point(-q*.7,0,0),point(q*.7,0,0),false); }
    case ORBIT -> { for(int i=0;i<3;i++){ double a=i*Math.PI*2/3+t; orb(caster.position().add(Math.cos(a)*q,.7,Math.sin(a)*q),.09); } }
    case RING -> { boolean floor=event.placement()==FormationPayload.CASTER || event.placement()==FormationPayload.FIXED;ring(focus,q,floor);ring(focus,q*.6,floor); }
    case PILLAR -> { line(point(-.2,-q,0),point(-.2,q,0),false); line(point(.2,-q,0),point(.2,q,0),false); ring(point(0,-q,0),.3,true); }
    case WAVE -> { for(int i=-2;i<=2;i++) slash(point(i*.3,Math.sin(i+t)*.15,0),.22,Math.PI,Math.PI/2); }
    case MINE -> { polygon(3,q,Math.PI/2); orb(focus,.08); }
    case TOTEM -> { polygon(4,q*.45,0); line(point(0,-q,0),point(0,q,0),false); orb(point(0,q,0),.12); }
    case DOMAIN -> { ring(feet,q*2.2,true); ring(feet.add(0,1.5,0),q*1.4,true); }
    case CRESCENT -> slash(focus,q,Math.PI*1.35,t*.4);
    case BARRAGE -> { for(int i=-2;i<=2;i++) orb(point(i*.25,Math.abs(i)*.1,0),.065+t*.035); }
    case ORB -> { orb(focus,.24*t); ring(focus,q*.8,false); }
    case BLITZ -> { line(point(-q,-.3,0),point(0,.25,0),true); line(point(0,.25,0),point(q,-.3,.4),true); }
    case SPARK -> { for(int i=0;i<3;i++){double a=i*Math.PI*2/3; line(focus,point(Math.cos(a)*.24,Math.sin(a)*.24,0),true);} }
    case RAY -> { line(point(0,0,-.5),point(0,0,.7*t),false); polygon(3,.22,0); }
    case NOVA -> { ring(focus,q*t,false); slash(focus,q*1.3,Math.PI*.4,0); slash(focus,q*1.3,Math.PI*.4,Math.PI); }
    case WISP -> { orb(point(Math.sin(t*2)*.25,t*.2,0),.1); slash(focus,.3,Math.PI*.8,t*2); }
    case COMET -> { orb(point(.2*t,.15*t,0),.16); line(point(-q,-q,-.5),focus,false); line(point(-q*.7,-q,-.4),focus,false); }
    case RICOCHET -> { line(point(-q,.3,-.3),point(0,-.2,0),false); line(point(0,-.2,0),point(q,.3,.3),false); orb(point(0,-.2,0),.08); }
    case CLUSTER -> { orb(focus,.14); for(int i=0;i<4;i++){double a=i*Math.PI/2+t; orb(point(Math.cos(a)*.3,Math.sin(a)*.3,0),.06);} }
    case LANCE -> { line(point(0,-.1,-.8),point(0,-.1,.6),false); line(point(-.2,-.1,.25),point(0,-.1,.6),false); line(point(.2,-.1,.25),point(0,-.1,.6),false); }
    case SWEEP -> { slash(focus,q*1.2,Math.PI*.7,-.8+t); line(point(-q,0,0),point(q,0,0),false); }
    case PRISM -> { polygon(3,q,Math.PI/2); for(int i=-1;i<=1;i++)line(focus,point(i*.35,-.45,.45),false); }
    case STREAM -> { for(int i=0;i<3;i++) line(point(Math.sin(i+t)*.18, i*.13,-.65),point(Math.sin(i+t+.8)*.18,i*.13,.35),false); }
    case VORTEX -> { for(int i=0;i<3;i++) slash(focus,.18+i*.14,Math.PI*1.2,t*2+i*2); }
    case SNARE -> { polygon(4,q,0); line(point(-q,0,0),point(q,0,0),false); line(point(0,-q,0),point(0,q,0),false); }
    case CONSTELLATION -> { polygon(5,q,t*.1); line(point(0,q,0),point(-q*.59,-q*.81,0),false); line(point(-q*.59,-q*.81,0),point(q*.95,q*.31,0),false); }
    case GLAIVE -> { line(point(-q*.6,-q*.6,0),point(q*.6,q*.6,0),false); slash(point(q*.45,q*.45,0),q*.5,Math.PI*1.3,Math.PI/4); }
    case IMPRINT -> { polygon(6,q,0); polygon(3,q*.55,Math.PI/2); }
    case RELAY -> { ring(point(-q*.6,0,0),q*.35,false); ring(point(q*.6,0,0),q*.35*(1-.5*t),false); line(point(-q*.25,0,0),point(q*.25,0,0),false); }
    case LATCH -> { slash(point(-.2,0,0),q*.6,Math.PI*1.4,0); slash(point(.2,0,0),q*.6,Math.PI*1.4,Math.PI); line(point(-.2,0,0),point(.2,0,0),false); }
   }
   for(String id:event.runes())if(NextSignatureForms.supports(id) && !FrostFormations.supports(id) && !VoidForms.supports(id) && !LifeForms.supports(id))
    NextSignatureForms.prepare(id,beat,event.scale(),assembly(),right,up,forward,quality==MagicQuality.Level.MINIMAL,this::emit);
   CampConcordFormations.draw(this,beat);
   boolean authoredFire=FireFormations.draw(this,beat);
   boolean authoredFrost=FrostFormations.draw(this,beat);
   boolean authoredStorm=StormFormations.draw(this,beat);
   boolean authoredWind=WindForms.formation(this,beat);
   boolean authoredEarth=EarthFormations.draw(this,beat);
   boolean authoredLife=LifeFormations.draw(this,beat);
   boolean authoredVoid=VoidFormations.draw(this,beat);
   materials(beat,authoredFire,authoredFrost,frostOnly,authoredStorm,authoredWind,windOnly,authoredEarth,earthOnly,authoredLife,lifeOnly,authoredVoid,voidOnly,fieldOnly,nextOnly,campOnly);
  }
  Vec3 assembly() {
   if(event.placement()==FormationPayload.CASTER || event.placement()==FormationPayload.AIMED || event.placement()==FormationPayload.FIXED)return focus;
   return switch(ShapeFormation.of(event.shape())) {case SELF,DOMAIN,ORBIT,TRAIL -> caster.position().add(0,.7,0);default -> focus;};
  }
  void materials(int beat,boolean authoredFire,boolean authoredFrost,boolean frostOnly,boolean authoredStorm,boolean authoredWind,boolean windOnly,boolean authoredEarth,boolean earthOnly,boolean authoredLife,boolean lifeOnly,boolean authoredVoid,boolean voidOnly,boolean fieldOnly,boolean nextOnly,boolean campOnly) {
   // Materials change the geometry as well as the colour. Each fused ingredient gets its own layer.
   for(int i=0;i<event.elements().size();i++) {
    String element=event.elements().get(i); double a=i*2.39996+beat*.9;
    if(campOnly && element.equals("arcane")) continue;
    if(authoredFire && element.equals("fire")) continue;
    if(authoredFrost && (element.equals("frost") || frostOnly && event.runes().stream().anyMatch(id->NextSignatureForms.ingredients(id).contains(element)))) continue;
    if(authoredStorm && element.equals("storm")) continue;
    if(authoredWind && (element.equals("wind") || windOnly && WindForms.ingredients(event.runes()).contains(element))) continue;
    if(authoredEarth && (element.equals("earth") || earthOnly && EarthForms.ingredients(event.runes()).contains(element))) continue;
    if(authoredLife && (element.equals("life") || lifeOnly && LifeForms.ingredients(event.runes()).contains(element))) continue;
    if(authoredVoid && (element.equals("void") || voidOnly && VoidForms.ingredients(event.runes()).contains(element))) continue;
    if(fieldOnly && FieldFusionForms.ingredients(event.runes()).contains(element)) continue;
    if(nextOnly && event.runes().stream().anyMatch(id->NextSignatureForms.ingredients(id).contains(element))) continue;
    // Caster-centered deliveries must not leave their elemental assembly at the front focus.
    Vec3 anchor=assembly();
    Vec3 at=anchor.add(right.scale(Math.cos(a)*.4)).add(up.scale(Math.sin(a)*.4));
    var theme=Vfx.theme(element);
    emit(theme.mote(),at);
    if(quality==MagicQuality.Level.MINIMAL) continue;
    switch(element) {
     case "fire" -> { emit(theme.mote(),at.add(0,.12,0)); emit(theme.spark(),at.add(0,.24,0)); }
     case "wind" -> { emit(theme.spark(),at.add(right.scale(.18))); emit(theme.mote(),at.subtract(right.scale(.18))); }
     case "frost" -> { emit(theme.spark(),at); emit(theme.mote(),at.add(0,-.18,0)); }
     case "storm" -> { Vec3 d=up.scale(.3); emit(new LightOption(LightOption.ARC,theme.primary(),(float)d.x,(float)d.y,(float)d.z,.018F,1,0,0,4),at); }
     case "earth" -> { emit(theme.spark(),at.add(0,-.2,0)); emit(theme.spark(),at.add(right.scale(.12))); }
     case "life" -> { emit(theme.mote(),at.add(0,.2,0)); emit(theme.spark(),at); }
     case "void" -> { emit(theme.spark(),at); emit(theme.mote(),anchor.subtract(at.subtract(anchor))); }
     case "time" -> { emit(theme.spark(),anchor.add(right.scale(Math.cos(a)*.6)).add(up.scale(Math.sin(a)*.6))); }
     case "blood" -> { emit(theme.spark(),at.add(0,-.1,0)); emit(theme.mote(),at.add(0,-.25,0)); }
     default -> emit(theme.spark(),at);
    }
   }
  }
 }
 /** Keep a turning caster's own assembly glyph behind their shoulders until it fades. */
 private static final class CasterCircle extends SpellCircleParticle {
  final LivingEntity caster;
  final java.util.List<String> runeIds;
  CasterCircle(ClientLevel world, LivingEntity caster, Vec3 at, SpellCircleOption option) {
   super(world,at.x,at.y,at.z,option);this.caster=caster;this.runeIds=option.runes();
  }
  @Override public void tick() {
   if(caster.isRemoved()||!caster.isAlive()||caster.level()!=level){remove();return;}
   super.tick();
   Vec3 at=caster.getEyePosition().subtract(caster.getLookAngle().scale(1.8)).add(0,-.3,0);
   setPos(at.x,at.y,at.z);
  }
  @Override protected org.joml.Quaternionf orientation(float partial) {
   Vec3 look=caster.getLookAngle();
   float liveYaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z));
   float livePitch=(float)-Math.toDegrees(Math.asin(Math.clamp(look.y,-1,1)));
   return new org.joml.Quaternionf().rotationYXZ((float)Math.toRadians(-liveYaw),(float)Math.toRadians(livePitch),0);
  }
  @Override protected Vec3 centre(float partial) {
   // Interpolating between opposite rear anchors would sweep a 180-degree turn through the camera.
   return caster.getEyePosition(partial).subtract(caster.getLookAngle().scale(1.8)).add(0,-.3,0);
  }
  /** The caster's own mastery of this spell (its rank and sigil), from the public look of the spell they have ready. */
  @Override protected dev.wildercord.player.MasteryAttachments.Look look() {
   var look=caster.getAttachedOrElse(dev.wildercord.player.MasteryAttachments.LOOK,dev.wildercord.player.MasteryAttachments.Look.NONE);
   return look.of(runeIds)?look:super.look();
  }
 }
}
