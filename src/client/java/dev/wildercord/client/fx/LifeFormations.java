package dev.wildercord.client.fx;
/** Life preparation preserves the server's shape-dependent assembly anchor and its rear circle. */
final class LifeFormations {
 private LifeFormations() {}
 static boolean supports(String id){return LifeForms.supports(id);}
 static boolean draw(SpellFormations.Canvas c,int beat){
  boolean authored=false;
  for(String id:c.event.runes())if(supports(id)){
   authored=true;LifeForms.prepare(id,beat,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
    c.quality==MagicQuality.Level.MINIMAL,c::emit);
  }
  return authored;
 }
}
