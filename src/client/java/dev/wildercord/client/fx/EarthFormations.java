package dev.wildercord.client.fx;
/** Earth preparation preserves the server's shape-dependent assembly anchor and its rear circle. */
final class EarthFormations {
 private EarthFormations() {}
 static boolean supports(String id){return EarthForms.supports(id);}
 static boolean draw(SpellFormations.Canvas c,int beat){
  boolean authored=false;
  for(String id:c.event.runes())if(supports(id)){
   authored=true;EarthForms.prepare(id,beat,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
    c.quality==MagicQuality.Level.MINIMAL,c::emit);
  }
  return authored;
 }
}
