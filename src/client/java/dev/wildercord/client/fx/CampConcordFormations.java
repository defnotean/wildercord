package dev.wildercord.client.fx;
import dev.wildercord.spell.ShapeFormation;
/** Actual server assembly placement is retained; pure groups use only the authored material scaffold. */
final class CampConcordFormations {
 private CampConcordFormations(){}
 static boolean draw(SpellFormations.Canvas c,int beat){boolean found=false;for(String id:c.event.runes())if(CampConcordForms.supports(id)){
  found=true;var at=c.assembly();var f=c.forward;double t=Math.clamp(beat/2.,0,1);
  // Instant lines align the material along the forward axis before discharge; actual Self/area anchors remain unchanged.
  if(ShapeFormation.of(c.event.shape())==ShapeFormation.BEAM||ShapeFormation.of(c.event.shape())==ShapeFormation.RAY||ShapeFormation.of(c.event.shape())==ShapeFormation.STREAM)
   at=at.add(f.scale(-.22+.22*t));
  CampConcordForms.prepare(id,beat*4,c.event.scale(),at,c.right,c.up,f,c.quality==MagicQuality.Level.MINIMAL,c::emit);
 }return found;}
}
