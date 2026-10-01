package dev.wildercord.spell;

/** Authored circle strokes shared by the world renderer and the Cord preview. No game dependencies. */
public final class CircleGeometry {
 public interface Sink { void stroke(float x0,float y0,float x1,float y1,float width,int color); }
 private static final float PI=(float)Math.PI,TWO_PI=PI*2,HALF_PI=PI/2;
 private final Sink sink;
 private CircleGeometry(Sink sink){this.sink=sink;}
 public static void draw(CircleDisciplines.Design design,float radius,float open,float width,float turn,float ticks,int ink,java.util.List<Integer> materials,Sink sink){
  if(radius<=0 || open<=0)return;
  new CircleGeometry(sink).draw(radius,open,width,turn,ticks,ink,materials,design);
 }
 private static float sin(float v){return (float)Math.sin(v);}
 private static float cos(float v){return (float)Math.cos(v);}
 private void line(float x0,float y0,float x1,float y1,float width,int ink,float depth){sink.stroke(x0,y0,x1,y1,width,ink);}
	/** Twelve authored mechanisms, all in the circle's rear plane. Progress changes their geometry. */
	private void draw(float r, float open, float fine, float turn, float time, int ink, java.util.List<Integer> materials, CircleDisciplines.Design design) {
		float width=Math.max(fine,r*.025F), breath=sin(time*.16F);
		switch(design) {
			case NEEDLE -> {
				for(int i=0;i<8;i++) { float a=turn+i*TWO_PI/8,b=a+.55F;
					line(cos(a)*r,sin(a)*r,cos(b)*r*(.18F+.35F*(1-open)),sin(b)*r*(.18F+.35F*(1-open)),width,ink,.005F); }
			}
			case BLOOM -> {
				for(int i=0;i<6;i++) { float a=i*TWO_PI/6+turn;
					float x=cos(a)*r*.55F*open,y=sin(a)*r*.55F*open;
					arc(x,y,r*.38F,a-.8F,4.7F,width,ink); }
			}
			case GYRE -> {
				for(int arm=0;arm<3;arm++) for(int j=0;j<9;j++) {
					float a=arm*TWO_PI/3+turn+j*.24F,b=a+.24F,ra=r*(.15F+j*.08F)*open,rb=ra+r*.08F*open;
					line(cos(a)*ra,sin(a)*ra,cos(b)*rb,sin(b)*rb,width,ink,.005F); }
			}
			case ANCHOR -> {
				polygon(4,r*.95F,PI/4,width,ink);polygon(4,r*.65F,-PI/4,width,ink);
				for(int sign:new int[]{-1,1}) { line(-r,r*.32F*sign,r,r*.32F*sign,width,ink,.005F);line(r*.32F*sign,-r,r*.32F*sign,r,width,ink,.005F); }
			}
			case RESERVOIR -> {
				for(int i=0;i<3;i++) { float rad=r*(.3F+i*.27F);
					arc(0,0,rad,turn*(i%2==0?1:-1),TWO_PI*open,width,ink); }
				line(-r*.35F,-r*.12F*breath,r*.35F,-r*.12F*breath,width,ink,.005F);
			}
			case CRUCIBLE -> {
				polygon(6,r*(.8F+.08F*breath),turn,width,ink);
				polygon(3,r*.65F,-turn,width,ink);polygon(3,r*.65F,PI-turn,width,ink);
			}
			case CONFLUENCE -> {
				int count=Math.max(2,Math.min(6,materials.size()));
				for(int i=0;i<count;i++) { float a=turn+i*TWO_PI/count,x=cos(a)*r*.68F,y=sin(a)*r*.68F;
					int tint=materials.isEmpty()?ink:materials.get(i%materials.size());
					arc(x,y,r*.25F,-turn,TWO_PI,width,tint);line(x,y,0,0,width,tint,.005F); }
			}
			case PILGRIM -> {
				polygon(4,r*.72F,turn,width,ink);
				for(int i=0;i<4;i++) {float a=i*HALF_PI;float x=cos(a)*r,y=sin(a)*r;
					line(x,y,cos(a+.3F)*r*.58F,sin(a+.3F)*r*.58F,width,ink,.005F);line(x,y,cos(a-.3F)*r*.58F,sin(a-.3F)*r*.58F,width,ink,.005F);}
			}
			case VIGIL -> {
				float lid=.3F+.3F*open+.06F*breath;
				for(int sign:new int[]{-1,1}) { line(-r,0,0,sign*r*lid,width,ink,.005F);line(0,sign*r*lid,r,0,width,ink,.005F); }
				arc(r*.12F*sin(time*.09F),0,r*.23F,0,TWO_PI,width,ink);
			}
			case MERCY -> {
				arc(-r*.27F,0,r*.66F,-HALF_PI,PI,width,ink);arc(r*.27F,0,r*.66F,HALF_PI,PI,width,ink);
				float h=r*(.24F+.03F*breath);line(-h,0,h,0,width,ink,.005F);line(0,-h,0,h,width,ink,.005F);
			}
			case TEMPEST -> {
				for(int i=0;i<6;i++) { float a=turn+i*TWO_PI/6;
					float x=cos(a)*r*.4F,y=sin(a)*r*.4F;
					line(0,0,x,y,width,ink,.005F);line(x,y,cos(a+.2F)*r*.72F,sin(a+.2F)*r*.72F,width,ink,.005F);
					line(x,y,cos(a-.3F)*r*.8F,sin(a-.3F)*r*.8F,width,ink,.005F); }
			}
			case ECLIPSE -> {
				arc(0,0,r*.65F,0,TWO_PI,width,ink);
				float x=sin(time*.045F)*r*.45F;arc(x,0,r*.58F,HALF_PI,PI,width,ink);
				for(int i=0;i<8;i++){float a=i*TWO_PI/8-turn;line(cos(a)*r*.8F,sin(a)*r*.8F,cos(a)*r,sin(a)*r,width,ink,.005F);}
			}
		}
		// Each actual ingredient writes a differently coloured moving segment into the outer circuit.
		for(int i=0;i<materials.size();i++) arc(0,0,r*1.12F,turn+i*TWO_PI/materials.size(),TWO_PI/materials.size()*.65F,
			width,materials.get(i));
	}
	private void polygon(int sides,float r,float rot,float width,int ink) {
		for(int i=0;i<sides;i++){float a=rot+i*TWO_PI/sides,b=rot+(i+1)*TWO_PI/sides;
			line(cos(a)*r,sin(a)*r,cos(b)*r,sin(b)*r,width,ink,.005F);}
	}
	private void arc(float x,float y,float r,float start,float span,float width,int ink) {
		int segments=Math.max(3,(int)Math.ceil(Math.abs(span)*3));
		for(int i=0;i<segments;i++){float a=start+span*i/segments,b=start+span*(i+1)/segments;
			line(x+cos(a)*r,y+sin(a)*r,x+cos(b)*r,y+sin(b)*r,width,ink,.005F);}
	}

}
