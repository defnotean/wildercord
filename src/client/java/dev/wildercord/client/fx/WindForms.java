package dev.wildercord.client.fx;

import dev.wildercord.cast.FlightBodies;
import dev.wildercord.content.AirflowOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Each wind spell has its own air mechanics. Preparation gathers the same flow that travels. */
final class WindForms {
    private WindForms() {}
    static final List<String> RUNES=FlightBodies.WIND;
    static boolean supports(String id){return FlightBodies.supportsWind(id);}
    static java.util.Set<String> ingredients(java.util.List<String> ids){
        var result=new java.util.HashSet<String>();
        for(String id:ids)if(supports(id)){
            result.add("wind");
            switch(id.substring(11)){
                case "dust_devil","downdraft" -> result.add("earth");
                case "zephyr","thresherwind" -> result.add("life");
                case "skylatch" -> result.add("void");
                case "razorgale" -> result.add("blood");
                case "skyglyph" -> result.add("arcane");
                case "recoil" -> result.add("time");
                default -> { }
            }
        }
        return result;
    }
    static boolean formation(SpellFormations.Canvas c,int beat){
        boolean authored=false;
        for(String id:c.event.runes())if(supports(id)){
            authored=true;draw(id,beat,false,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
                c.quality==MagicQuality.Level.MINIMAL,c::emit);
        }
        return authored;
    }
    static void flight(String id,int age,double scale,double length,Vec3 head,Vec3 velocity,
                       boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
        if(!supports(id)||!Double.isFinite(velocity.lengthSqr()))return;
        if(FieldFusionForms.flight(id,age,scale,length,head,velocity,minimal,emit))return;
        var f=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();
        var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.0001?new Vec3(1,0,0):r.normalize();
        draw(id,age,true,Math.clamp(scale,.4,2)*.55,head,r,r.cross(f).normalize(),f.scale(Math.clamp(length,1,2)),minimal,emit);
    }
    static void draw(String id,int age,boolean flight,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,
                     boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
        if(!supports(id))return;
        if(FieldFusionForms.prepare(id,age,scale,anchor,right,up,forward,minimal,emit))return;
        double t=flight?age*.23:age*.5;
        double gather=flight?1:t;
        var p=new Pen(anchor,right,up,forward,scale,flight?5:8,minimal,emit);
        switch(id.substring(11)){
            case "windcut" -> { // A thin shear sheet sweeps sideways, its trailing eddy peeling off.
                double sweep=flight?.11*Math.sin(t):.22*(1-t);
                p.flow(0xEFFAF4,-.48,-.18+sweep,-.04,0,.36+sweep,.02,.48,-.08+sweep,.17,.06);
                p.flow(0xA6CEC0,-.28,-.23,0,.08,-.42,.02,.35,-.2-.07*Math.sin(t),-.18,.035);
                p.air(0xD7F1E8,.34,-.08+sweep,.14,.065);
            }
            case "push" -> { // A broad compression front advances; a slower return rolls under it.
                double z=flight?.1*Math.sin(t*.8):.2*t;
                p.flow(0xC7E7DC,-.48,-.25,z,0,.48,z+.16,.48,-.25,z,.09);
                p.flow(0x94BDAF,-.3,-.28,z-.15,0,-.44,z-.2,.3,-.28,z-.15,.05);
                p.air(0xD9F2E9,0,.02,z,.095);
            }
            case "repel" -> { // Two opposed pressure fronts push away from a hollow center.
                double spread=flight?.3+.1*Math.sin(t):.25+.22*t;
                for(int side:new int[]{-1,1})p.flow(0xB8D7CA,side*.05,-.28,0,side*spread,.02,.05,side*.12,.35,.05,.085);
                p.air(0xDBEDE5,0,-.1,.06,.11);
            }
            case "disarm" -> { // A hooking snatch curls around an empty grip and pulls sideways.
                double pull=flight?.12*Math.sin(t):.22*t;
                p.flow(0xD5EADC,-.3,.3,0,.5,.35,.04,.12,-.2,.08,.055);
                p.flow(0xA6CBB7,.12,-.2,.08,.03,-.34,.04,-.45-pull,-.1,-.08,.07);
                p.air(0xD5EADC,-.3-pull,-.1,-.08,.075);
            }
            case "recoil" -> { // Outgoing flow and an opposing delayed return visibly cross.
                double back=flight?.17*Math.cos(t):.18*(1-t);
                p.flow(0xEACF98,-.4,-.18,back,0,.28,.2,.43,-.05,.08,.075);
                p.flow(0xB8B492,.43,.16,-.16,0,-.3,-.2,-.35,.02,-back,.045);
                p.air(0xFFE4AB,-.35,.02,-back,.065);
                p.dot(MaterialOption.TIME,0xD6BF82,.2*Math.cos(t*.6),-.1,-.12,.065);
            }
            case "launch" -> { // A compressed pocket opens into a forward-rising jet.
                double y=flight?.16*Math.sin(t):.18*t;
                p.flow(0xC1E2EC,-.25,-.4,0,-.27,.06,.08,-.04,.42+y,.28,.075);
                p.flow(0xDDEFF4,.25,-.4,0,.3,.04,.08,.08,.42+y,.28,.055);
                p.air(0xD2EEF8,.04,.25+y,.2,.085);
            }
            case "levitate" -> { // Lift cradles rise slowly on either side of a stable empty pocket.
                double lift=flight?.08*Math.sin(t*.5):.15*t;
                for(int side:new int[]{-1,1})p.flow(0xB7DCE6,side*.36,-.33,0,side*.12,-.2+lift,.06,side*.28,.28+lift,.02,.06);
                p.air(0xD6F1F5,0,-.17+lift,.03,.09);
            }
            case "updraft" -> { // A twisting rising jet is paired with the returning downward fall.
                double angle=flight?t:Math.PI*t;
                p.flow(0xBCE2EE,-.22,-.4,0,.3*Math.cos(angle),.02,.2*Math.sin(angle),.04,.5,.06,.08);
                p.flow(0x95BCCA,.32,.35,-.05,.46,-.02,-.09,.22,-.4,-.08,.045);
                p.air(0xDDF6FC,.04,.35+.05*Math.sin(t),.06,.09);
            }
            case "downdraft" -> { // A downward wedge collapses into a low spreading groundward gust.
                double drop=flight?.13*Math.sin(t):.24*t;
                for(int side:new int[]{-1,1})p.flow(0x9DB7B1,side*.34,.4,0,side*.12,.06-drop,.07,side*.42,-.3-drop,.1,.08);
                p.air(0xBCD5CF,0,-.22-drop,.09,.1);
                p.dot(MaterialOption.STONE,0x9C9B85,.12,-.22-drop,.1,.08);
            }
            case "summit_wind" -> { // Unequal mountain gusts lift vapor above a narrow upflow.
                double sway=flight?.1*Math.sin(t*.7):.13*(1-t);
                p.flow(0xDFEDF0,-.42,-.3,-.08,-.28,.16,.05,sway,.48,.1,.065);
                p.flow(0xBDD2DA,.42,-.32,-.02,.37,.13,.15,.13+sway,.3,.26,.095);
                p.air(0xEEF8FA,.02+sway,.36,.12,.08);
                p.dot(MaterialOption.VAPOUR,0xCEDDE2,-.25+sway,.3,-.03,.11);
            }
            case "skyglyph" -> { // Three ascending channels write an open wind-lift mark.
                for(int i=-1;i<=1;i++)p.flow(0xC5E6F0,i*.25,-.35,0,i*.2,.05,.12,i*.15,.25+.12*gather,.17,.04);
                p.dot(MaterialOption.ARCANE,0xCDE4F7,0,-.3+.08*Math.sin(t),0,.085);
                p.air(0xDCEEF2,0,.3+.08*Math.sin(t),.1,.065);
            }
            case "cushion" -> { // A shallow landing pillow compresses gently and spreads at the sides.
                double sink=flight?.06*Math.sin(t*.6):.08*t;
                p.flow(0xCFDFEA,-.47,-.12-sink,0,0,.22-sink,.08,.47,-.12-sink,0,.1);
                p.flow(0xA2C3D1,-.38,-.23,0,0,-.1-sink,-.06,.38,-.23,0,.06);
                p.air(0xDAEDF6,0,-.08-sink,.05,.11);
            }
            case "feather_fall" -> { // A long vane rocks while fine barbs fan from its slow-falling shaft.
                double rock=.09*Math.sin(t*.7);
                p.flow(0xD5EAF0,-.1,-.38,0,.03+rock,0,.02,.12,.4,0,.04);
                for(int i=0;i<(minimal?2:3);i++)p.flow(0xB8D9E4,.03+rock,i*.14-.15,0,-.24,i*.14+.02,.05,-.38,i*.14-.11,.08,.035);
                p.air(0xDDEEF5,.03+rock,.08,.02,.055);
            }
            case "soar" -> { // Paired shoulder currents open with the wings, with beating outer tips.
                double beat=flight?.11*Math.sin(t*1.5):.15*gather;
                for(int side:new int[]{-1,1})p.flow(0xC9E8EF,side*.06,-.1,0,side*.3,.2+beat,-.04,side*.52,.05+beat,-.16,.065);
                p.air(0xD8EEF6,0,.04+beat,0,.075);
            }
            case "cyclone" -> { // A narrowing funnel winds about a calm center in three unequal turns.
                for(int i=0;i<(minimal?2:3);i++){
                    double a=t*1.2+i*2.1,r=.24+i*.08;
                    p.flow(0xB1CCC2,Math.cos(a)*r,-.34+i*.2,Math.sin(a)*r,
                        -Math.sin(a)*r*1.5,-.25+i*.2,Math.cos(a)*r*1.5,
                        -Math.cos(a)*r,-.12+i*.2,-Math.sin(a)*r,.06);
                }
                p.air(0xD8E8E0,0,.13,.02,.07);
            }
            case "dust_devil" -> { // A leaning sand funnel carries actual tumbling grit through its turns.
                double lean=.09*Math.sin(t);
                p.flow(0xD0B891,-.12,-.4,0,.55+lean,-.08,.15,.16+lean,.42,-.05,.1);
                p.flow(0xAE9A7A,.12,-.36,.02,-.5+lean,.15,-.13,-.24+lean,.25,.09,.07);
                p.dot(MaterialOption.STONE,0xB69467,Math.cos(t)*.25,.08,Math.sin(t)*.18,.075);
                p.air(0xE6CEA5,lean,-.08,.02,.09);
            }
            case "razorgale" -> { // Two opposing shear blades whirl across a carried bleeding score.
                double a=t*1.7;
                p.flow(0xD7CBD0,-.42*Math.cos(a),-.42*Math.sin(a),0,.1,.12,.15,.42*Math.cos(a),.42*Math.sin(a),.08,.035);
                p.flow(0xB1C2BC,.32*Math.sin(a),-.32*Math.cos(a),-.1,-.12,.08,0,-.32*Math.sin(a),.32*Math.cos(a),.08,.025);
                p.dot(MaterialOption.BLOOD,0xB34C62,.09*Math.sin(t),-.04,.04,.055);
                p.air(0xE0D8D8,0,.04,.09,.065);
            }
            case "swift" -> { // Three unequal slipstreams gather, then sweep backwards like acceleration.
                double slip=flight?.16*Math.sin(t):.2*t;
                for(int i=0;i<3;i++)p.flow(0xBBDFCF,i*.18-.18,i*.08-.08,.3-slip,
                    i*.2-.2,i*.08+.04,-.07,i*.13-.13,i*.08-.1,-.34-slip,.045-i*.008);
                p.air(0xD1F0E2,0,0,.18-slip,.065);
            }
            case "leap" -> { // A bent spring of air releases upward along one arched launch lane.
                double spring=flight?.1*Math.sin(t):.2*(1-t);
                p.flow(0xC7E7D8,-.4,-.27,0,0,-.43+spring,.12,.35,-.14,.18,.07);
                p.flow(0xA3CABB,.35,-.14,.18,.25,.35+spring,.25,-.12,.42,.12,.045);
                p.air(0xDDF2E8,.08,.18+spring,.19,.075);
            }
            case "dash" -> { // A forward spear of air draws two long, backward-streaming tails.
                double speed=flight?.13*Math.sin(t*1.3):.2*t;
                for(int side:new int[]{-1,1})p.flow(0xBBDCCF,0,0,.34+speed,side*.18,.08,-.05,side*.22,-.08,-.4,.05);
                p.air(0xDCF1E6,0,0,.25+speed,.08);
            }
            case "gale_mantle" -> { // A protective cape billows in three separate charges, ready for dashes.
                for(int i=0;i<3;i++){
                    double wave=.07*Math.sin(t+i*1.1),x=(i-1)*.22;
                    p.flow(0xBADACB,x,.3,0,x*1.4,-.08+wave,-.26,x,-.34,-.13,.065);
                }
                p.air(0xD7EFE2,0,.17+.04*Math.sin(t),-.05,.085);
            }
            case "deflect" -> { // Incoming airflow bends across the face and turns back outside the ward.
                double turn=.08*Math.sin(t);
                p.flow(0xCBE5D6,-.35,-.18,.3,-.04,.17,.07,.22,.15+turn,-.26,.055);
                p.flow(0x9DC3B1,.22,.15+turn,-.26,.48,.3,-.04,.38,-.1,.23,.05);
                p.air(0xE0F2E6,.22,.15+turn,-.16,.075);
            }
            case "zephyr" -> { // A warm supporting breeze fans gently outward with fluttering leaves.
                double breathe=.05*Math.sin(t*.5);
                for(int side:new int[]{-1,1})p.flow(0xE5D8B4,0,-.16,0,side*.2,.16+breathe,.12,side*.47,.04+breathe,0,.06);
                p.dot(MaterialOption.PETAL,0xD6DFA1,.19*Math.sin(t*.7),-.04,.09,.075);
                p.air(0xF4EACB,0,.05+breathe,.08,.09);
            }
            case "prune" -> { // A sideways shear strips loose leaves away from an open cutting lane.
                double away=flight?.14*Math.sin(t):.18*t;
                p.flow(0xC8E2CD,-.48,-.05,0,0,.2,.04,.48,.02,.12,.04);
                p.flow(0x9ABB9D,.06,.1,.04,.29,.34,.08,.44+away,.28,.1,.025);
                p.dot(MaterialOption.PETAL,0x7FAD73,.36+away,.2,.06,.07);
                p.air(0xD8EBD3,-.08,.03,.05,.065);
            }
            case "wind_steps" -> { // Lift folds into staggered horizontal treads that rise in sequence.
                for(int i=0;i<3;i++){
                    double y=i*.16-.26,z=i*.12-.15,lift=.04*Math.sin(t+i*1.6);
                    p.flow(0xC5E7DB,-.3,y+lift,z,0,y+.12+lift,z+.03,.3,y+lift,z,.05);
                }
                p.air(0xDCEFE5,0,.12+.03*Math.sin(t),.15,.065);
            }
            default -> { }
        }
    }
    private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,int lifetime,boolean minimal,
                       BiConsumer<ParticleOptions,Vec3> emit){
        Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
        void flow(int color,double x,double y,double z,double cx,double cy,double cz,double ex,double ey,double ez,double width){
            Vec3 start=at(x,y,z);
            emit.accept(new AirflowOption(color,at(cx,cy,cz).subtract(start),at(ex,ey,ez).subtract(start),
                (float)Math.clamp(width*scale,.015,.3),lifetime,minimal),start);
        }
        void air(int color,double x,double y,double z,double size){dot(MaterialOption.WIND,color,x,y,z,size);}
        void dot(int style,int color,double x,double y,double z,double size){
            emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.02,.8),lifetime),at(x,y,z));
        }
    }
}
