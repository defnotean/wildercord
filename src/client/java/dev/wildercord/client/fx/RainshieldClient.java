package dev.wildercord.client.fx;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
/** Reed ribs and mended cloth, finite actual source-identified use/catch; private carrier/recipient display. */
public final class RainshieldClient {
 private static RainshieldFx.Event current;
 private static java.util.UUID armed;
 private static ClientLevel inputWorld;
 private static boolean previousUse;
 private static Player inputPlayer;
 private static long inputGeneration;
 private static java.util.UUID knownDraw;
 private static void release(){if(armed!=null&&ClientPlayNetworking.canSend(RainshieldFx.Release.TYPE))ClientPlayNetworking.send(new RainshieldFx.Release(armed));armed=null;}
 private RainshieldClient(){}
 public static void init(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.level==null||mc.player==null||mc.level!=inputWorld||mc.player!=inputPlayer){armed=null;current=null;knownDraw=null;inputGeneration=0;inputWorld=mc.level;inputPlayer=mc.player;previousUse=mc.options.keyUse.isDown();return;}
  boolean down=mc.options.keyUse.isDown();if(previousUse&&!down)release();previousUse=down;
 });ClientPlayNetworking.registerGlobalReceiver(RainshieldFx.Event.TYPE,(e,context)->receive(e,context.client()));}
 private static void receive(RainshieldFx.Event e,Minecraft mc){if(mc.level==null||mc.player==null||!e.world().equals(mc.level.dimension().identifier().toString()))return;
  // Own release authority survives a delayed/undisplayable recipient cue; cosmetic guards remain below.
  if(e.carrier().equals(mc.player.getUUID())&&e.carrierId()==mc.player.getId()&&mc.level.getEntity(e.carrierId())==mc.player&&(e.beat()==0||e.beat()==1)){
   if(mc.level!=inputWorld||mc.player!=inputPlayer){armed=null;knownDraw=null;inputGeneration=0;inputWorld=mc.level;inputPlayer=mc.player;}
   if(e.generation()>inputGeneration||e.generation()==inputGeneration&&e.draw().equals(knownDraw)){inputGeneration=e.generation();knownDraw=e.draw();armed=e.draw();if(!mc.options.keyUse.isDown())release();}
  }
  if(Math.abs(mc.level.getGameTime()-e.tick())>40||!(mc.level.getEntity(e.carrierId()) instanceof Player p)||!p.getUUID().equals(e.carrier())||!(mc.level.getEntity(e.recipientId()) instanceof Player r)||!r.getUUID().equals(e.recipient())||p.distanceToSqr(r)>9)return;
  if(e.beat()==3){if(current!=null&&current.carrier().equals(e.carrier())&&current.draw().equals(e.draw()))current=null;return;}current=e;boolean minimal=(e.carrier().equals(mc.player.getUUID())?MagicQuality.own:MagicQuality.others)==MagicQuality.Level.MINIMAL;int ribs=minimal?3:7;
  if(e.beat()==2){mc.particleEngine.add(new Piece(mc.level,e,"rainshield_splinter",0,1,10));return;}
  for(int i=0;i<ribs;i++)mc.particleEngine.add(new Piece(mc.level,e,"rainshield_rib",i,ribs,e.beat()==1?40:4));
  if(e.beat()==1)for(int i=0;i<(minimal?1:3);i++)mc.particleEngine.add(new Piece(mc.level,e,"rainshield_cloth",i,minimal?1:3,40));
 }
 private static final class Piece extends SingleQuadParticle implements SigilGroup.Extent {
  private final RainshieldFx.Event e;private final int index,count;private final String material;private final float size;
  Piece(ClientLevel world,RainshieldFx.Event e,String material,int index,int count,int life){super(world,0,0,0,SpellCircleParticle.particleSprite(material));this.e=e;this.index=index;this.count=count;this.material=material;lifetime=life;hasPhysics=false;size=material.equals("rainshield_splinter")?.16F:.26F;quadSize=size;place();xo=x;yo=y;zo=z;}
  private boolean valid(){var mc=Minecraft.getInstance();if(mc.level!=level||!e.world().equals(level.dimension().identifier().toString())||!(level.getEntity(e.carrierId()) instanceof Player p)||!p.getUUID().equals(e.carrier())||!(level.getEntity(e.recipientId()) instanceof Player r)||!r.getUUID().equals(e.recipient())||!p.isAlive()||!r.isAlive()||p.distanceToSqr(r)>9)return false;return e.beat()==2||current==e&&p.isUsingItem()&&p.getUseItem().is(RooksRainshield.ITEM)&&(p==r||r.isShiftKeyDown());}
  private void place(){var r=level.getEntity(e.recipientId());if(r==null)return;var side=new Vec3(-e.normal().z,0,e.normal().x);double spread=material.equals("rainshield_cloth")?1.04:1.56;double angle=count==1?0:(index/(double)(count-1)-.5)*spread;double radius=size*.9375;var at=r.position().add(0,1.0,0).add(e.normal().scale(.45)).add(side.scale(Math.sin(angle)*radius)).add(0,Math.cos(angle)*radius,0);if(e.beat()==2)at=at.add(side.scale(age*.012)).add(0,-age*.012,0);x=at.x;y=at.y;z=at.z;roll=(float)-angle;oRoll=roll;}
  @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime||!valid()){remove();return;}place();alpha=Math.min(1,(lifetime-age)/3F);quadSize=size;}
  @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}@Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
  @Override public double centreX(){return x;}@Override public double centreY(){return y;}@Override public double centreZ(){return z;}@Override public double reach(){return .6;}
 }
}
