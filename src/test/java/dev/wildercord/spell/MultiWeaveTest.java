package dev.wildercord.spell;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.spell.Runes.*;

class MultiWeaveTest {
	@Test void weavingIsCanonicalFlatAndKeepsAllEffects(){
		var a=WovenRunes.bind(WovenRunes.bind(FIRE,HEAL),SHOCK);
		var b=WovenRunes.bind(FIRE,WovenRunes.bind(SHOCK,HEAL));
		assertEquals(a,b);assertEquals(3,WovenRunes.contents(a).size());assertEquals(3,a.tier());
		assertEquals(FIRE.cost()+HEAL.cost()+SHOCK.cost(),a.cost());
		var compile=SpellCompiler.compile(List.of(BOLT,a));
		assertFalse(compile.isEmpty());assertEquals(3,compile.root().groups.getFirst().effects.size());
		assertEquals(SpellCompiler.compile(List.of(BOLT,FIRE,HEAL,SHOCK)).cost(),compile.cost(),1e-6);
		assertTrue(Fusions.recipe(a,FIRE).isEmpty(),"shards must not discard a weave's other components");
	}
	@Test void limitsAndCostsGrowWithoutRecursiveIds(){
		var weave=WovenRunes.bind(FIRE,FIRE);
		for(int i=3;i<=8;i++){
			var plan=Fusions.plan(List.of(Fusions.Slot.of(weave,2),Fusions.Slot.of(FIRE,3)),Fusions.Catalyst.BLOCK);
			assertTrue(plan.ready());weave=plan.result();assertEquals(3*(i-1),plan.xp());assertEquals(2,plan.rank());
			assertEquals(i>4?4:3,weave.tier());assertEquals(i,WovenRunes.contents(weave).size());
		}
		var full=weave;assertThrows(IllegalArgumentException.class,()->WovenRunes.bind(full,FIRE));
		assertFalse(Fusions.plan(List.of(Fusions.Slot.of(full,1),Fusions.Slot.of(FIRE,1)),Fusions.Catalyst.BLOCK).ready());
		String nested=WovenRunes.PREFIX+Knots.encode((FIRE.id()+"\n"+full.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertTrue(WovenRunes.def(nested).isEmpty());
	}
	@Test void everyRegisteredElementHasBothFusionRoutes(){
		for(var rune:Runes.all())if(Fusions.fusible(rune)){
			assertTrue(Fusions.recipe(rune,FIRE).isPresent(),rune.id());
			assertEquals(List.of(rune,rune),WovenRunes.contents(WovenRunes.bind(rune,rune)),rune.id());
		}
		assertEquals(CINDER_BULWARK,Fusions.recipe(STRATA_RISE,FIRE).orElseThrow().result());
		assertEquals(THUNDER_TIDE,Fusions.recipe(TIDAL_LIFT,SHOCK).orElseThrow().result());
	}
	@Test void everyInnateHasAnExactSoulFusionWithoutRankBoosts(){
		for(var rune:Runes.all())if(Runes.innate(rune)){
			var weave=WovenRunes.bind(rune,FIRE);
			assertEquals(4,weave.tier());assertFalse(Ranks.rankable(weave));
			assertTrue(WovenRunes.contents(weave).contains(rune));
			assertTrue(Fusions.plan(List.of(Fusions.Slot.of(rune,1),Fusions.Slot.of(FIRE,3)),Fusions.Catalyst.BLOCK).ready());
		}
		assertThrows(IllegalArgumentException.class,()->WovenRunes.bind(KINDLING,BLOOD_THREAD),"a heart owns only one distinct innate");
	}
}
