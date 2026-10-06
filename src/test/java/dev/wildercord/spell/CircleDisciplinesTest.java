package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class CircleDisciplinesTest {
	static SpellPlan.Group group(RuneDef circle,RuneDef effect){return SpellCompiler.compile(List.of(Runes.BOLT,circle,effect)).root().groups.getFirst();}
	@Test void ordinaryShapesAcceptDisciplinesAndMasterLessonsExplicitlyRefuse() {
		var circles=Runes.all().stream().filter(CircleDisciplines::isCircle).toList();assertEquals(12,circles.size());
		var designs=new HashSet<CircleDisciplines.Design>();
		for(var circle:circles){designs.add(CircleDisciplines.profile(circle).design());
			for(var shape:Runes.all().stream().filter(r->r.family()==RuneFamily.SHAPE).toList()){
				boolean lesson=shape.equals(Runes.RELAY) || shape.equals(Runes.REWEAVE);
				RuneDef effect=lesson?Runes.HARM:Runes.FIRE;
				var compiled=SpellCompiler.compile(List.of(shape,circle,effect));
				if (lesson) {
					var plain=SpellCompiler.compile(List.of(shape,effect));
					assertFalse(plain.isEmpty(), shape.name()+" accepts its exact Harm payload");
					assertTrue(plain.warnings().isEmpty());
					assertTrue(compiled.isEmpty(), shape.name()+" refuses "+circle.name()+" before payment");
					assertEquals(0, compiled.cost());
					assertTrue(compiled.warnings().contains(shape.equals(Runes.RELAY)
						?RelayRules.GRAMMAR_PROBLEM:ReweaveRules.GRAMMAR_PROBLEM));
					continue;
				}
				assertTrue(compiled.warnings().isEmpty(),circle.name()+" / "+shape.name()+compiled.warnings());
				assertEquals(0,compiled.attachedTo()[1]);assertTrue(compiled.lines().stream().anyMatch(s->s.contains(circle.name())));
			}
		}assertEquals(12,designs.size());
	}
	@Test void duplicateAndDifferentCirclesCannotStackAndAreNotCharged() {
		var plain=SpellCompiler.compile(List.of(Runes.BOLT,Runes.NEEDLE_CIRCLE,Runes.FIRE));
		for(var extra:List.of(Runes.BLOOM_CIRCLE,Runes.NEEDLE_CIRCLE)){
			var two=SpellCompiler.compile(List.of(Runes.BOLT,Runes.NEEDLE_CIRCLE,extra,Runes.FIRE));
			assertEquals(plain.cost(),two.cost(),1e-9);assertEquals(SpellCompiler.UNATTACHED,two.attachedTo()[2]);assertFalse(two.warnings().isEmpty());
		}
	}
	@Test void numericalTradesReachTheSharedRuntimeNumbers() {
		var plain=SpellCompiler.compile(List.of(Runes.BOLT,Runes.FIRE)).root().groups.getFirst();
		assertEquals(SpellNumbers.shapeRadius(plain)*.65,SpellNumbers.shapeRadius(group(Runes.NEEDLE_CIRCLE,Runes.FIRE)),1e-9);
		assertEquals(SpellNumbers.groupPower(plain)*1.2,SpellNumbers.groupPower(group(Runes.NEEDLE_CIRCLE,Runes.FIRE)),1e-9);
		assertEquals(SpellNumbers.boltSpeed(plain)*1.3,SpellNumbers.boltSpeed(group(Runes.GYRE_CIRCLE,Runes.FIRE)),1e-9);
		assertEquals(1.4,CircleDisciplines.profile(group(Runes.ANCHOR_CIRCLE,Runes.FIRE)).duration(),1e-9);
		assertEquals(.75,CircleDisciplines.profile(group(Runes.CRUCIBLE_CIRCLE,Runes.FIRE)).duration(),1e-9);
	}
	@Test void conditionsAndRolesHaveBothBenefitsAndCosts() {
		for(var circle:List.of(Runes.PILGRIM_CIRCLE,Runes.VIGIL_CIRCLE,Runes.TEMPEST_CIRCLE,Runes.ECLIPSE_CIRCLE)){
			var g=group(circle,Runes.FIRE);assertEquals(.9,CircleDisciplines.conditional(g,false,false,false,false),1e-9);
			assertTrue(CircleDisciplines.conditional(g,true,true,true,true)>1);
		}
		assertEquals(1.2,CircleDisciplines.role(group(Runes.MERCY_CIRCLE,Runes.HEAL),EffectKind.HELPFUL),1e-9);
		assertEquals(.75,CircleDisciplines.role(group(Runes.MERCY_CIRCLE,Runes.FIRE),EffectKind.HARMFUL),1e-9);
	}
	@Test void confluenceCountsActualDistinctWovenElementsNotDuplicateRunes() {
		var g=group(Runes.CONFLUENCE_CIRCLE,WovenRunes.bind(Runes.FIRE,Runes.SHOCK));
		assertEquals(1.06,CircleDisciplines.conditional(g,false,false,false,false),1e-9);
		var dup=group(Runes.CONFLUENCE_CIRCLE,WovenRunes.bind(Runes.FIRE,Runes.FIRE));
		assertEquals(.98,CircleDisciplines.conditional(dup,false,false,false,false),1e-9);
	}
	@Test void plainLayoutsCoverTwelveDesignsWithoutChangingGameplay() {
		var designs=new HashSet<CircleDisciplines.Design>();
		for(var shape:Runes.all().stream().filter(r->r.family()==RuneFamily.SHAPE).toList()) designs.add(CircleDisciplines.design(List.of(shape,Runes.FIRE)));
		assertEquals(12,designs.size());
		assertEquals(1,CircleDisciplines.profile((RuneDef)null).power(),1e-9);
	}
	@Test void knotsRetainCirclesAndSeparateLinkedGroupsKeepTheirOwnDisciplines() {
		var knot=Knots.def(Knots.id(List.of(Runes.BOLT,Runes.GYRE_CIRCLE,Runes.FIRE),"Gyre shot")).orElseThrow();
		var tied=SpellCompiler.compile(List.of(knot));assertEquals(Runes.GYRE_CIRCLE,CircleDisciplines.selected(tied.root().groups.getFirst().shapeMods));
		assertEquals(CircleDisciplines.Design.GYRE,CircleDisciplines.design(Knots.flatten(List.of(knot))));
		var linked=SpellCompiler.compile(List.of(Runes.BOLT,Runes.NEEDLE_CIRCLE,Runes.FIRE,Runes.ON_HIT,Runes.BURST,Runes.BLOOM_CIRCLE,Runes.FIRE));
		assertTrue(linked.warnings().isEmpty());
		assertEquals(Runes.NEEDLE_CIRCLE,CircleDisciplines.selected(linked.root().groups.getFirst().shapeMods));
		assertEquals(Runes.BLOOM_CIRCLE,CircleDisciplines.selected(linked.root().link.next.groups.getFirst().shapeMods));
	}
	@Test void twelveAuthoredMechanismsHaveDistinctBoundedFiniteStrokes() {
		var unique=new HashSet<List<String>>();
		for(var design:CircleDisciplines.Design.values()){
			var strokes=new java.util.ArrayList<String>();
			CircleGeometry.draw(design,1,1,.01F,.2F,20,0xFFFFFFFF,List.of(0xFFFF8800,0xFF88FFFF),(x0,y0,x1,y1,w,c)->{
				assertTrue(Float.isFinite(x0)&&Float.isFinite(y0)&&Float.isFinite(x1)&&Float.isFinite(y1)&&w>0);
				assertTrue(Math.hypot(x0,y0)<1.5 && Math.hypot(x1,y1)<1.5);
				strokes.add(x0+","+y0+","+x1+","+y1+","+c);
			});assertFalse(strokes.isEmpty());assertTrue(strokes.size()<=256);assertTrue(unique.add(List.copyOf(strokes)),"duplicate circle "+design);
		}assertEquals(12,unique.size());
	}
	@Test void closedGeometryDrawsNothingAndAllMaterialColoursReachItsCircuit() {
		CircleGeometry.draw(CircleDisciplines.Design.NEEDLE,1,0,.01F,0,0,-1,List.of(),(a,b,c,d,w,ink)->fail("closed circle emitted geometry"));
		var colours=new HashSet<Integer>();
		CircleGeometry.draw(CircleDisciplines.Design.CONFLUENCE,1,1,.01F,0,0,-1,List.of(1,2,3,4,5,6,7,8,9,10),(a,b,c,d,w,ink)->colours.add(ink));
		for(int i=1;i<=10;i++)assertTrue(colours.contains(i));
	}
	@Test void aLaterGroupsCircleDoesNotChangeTheOpeningGroupsLayout() {
		assertEquals(CircleDisciplines.Design.NEEDLE,CircleDisciplines.design(List.of(Runes.BOLT,Runes.FIRE,Runes.ON_HIT,Runes.BURST,Runes.MERCY_CIRCLE,Runes.HEAL)));
		assertEquals(CircleDisciplines.Design.NEEDLE,CircleDisciplines.design(List.of(Runes.BOLT,Runes.FIRE,Runes.BURST,Runes.MERCY_CIRCLE,Runes.HEAL)));
	}
}
