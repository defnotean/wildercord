package dev.wildercord.spell;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ModifierLimitsTest {
 private static SpellCompiler.Compiled stacked(RuneDef shape,RuneDef effect,RuneDef mod,int count,boolean before){var list=new ArrayList<RuneDef>();list.add(shape);if(!before)list.add(effect);for(int i=0;i<count;i++)list.add(mod);if(before)list.add(effect);return SpellCompiler.compile(list);}
 @Test void nineFreeVowsNoLongerMultiplyBy512(){var c=stacked(Runes.TOUCH,Runes.HARM,Runes.VOW_MOD,9,true);var g=c.root().groups.getFirst();assertEquals(2,SpellNumbers.groupPower(g));assertEquals(1,g.count(Runes.VOW_MOD));assertEquals(1,c.warnings().size());assertTrue(c.cost()<200);assertEquals(SpellCompiler.UNATTACHED,c.attachedTo()[2]);}
 @Test void executeAndTrialKeyApplyOnce(){for(var mod:List.of(Runes.EXECUTE_MOD,Runes.TRIAL_KEY)){var c=stacked(Runes.BOLT,Runes.HARM,mod,10,false);var e=c.root().groups.getFirst().effects.getFirst();assertEquals(1,e.count(mod));assertEquals(mod==Runes.EXECUTE_MOD?2:1.6,mod==Runes.EXECUTE_MOD?SpellNumbers.executeBonus(e):SpellNumbers.trialKeyBonus(e),1e-9);}}
 @Test void focusedShapeHasTwoPaidSteps(){var c=stacked(Runes.BURST,Runes.HARM,Runes.FOCUS_MOD,13,true);var g=c.root().groups.getFirst();assertEquals(2,g.count(Runes.FOCUS_MOD));assertEquals(2.25,SpellNumbers.groupPower(g));assertEquals(.25,SpellNumbers.shapeRadius(g));assertEquals(1,c.warnings().size());}
 @Test void knotsCannotHideRepeatedVows(){var held=new ArrayList<RuneDef>();held.add(Runes.TOUCH);for(int i=0;i<9;i++)held.add(Runes.VOW_MOD);held.add(Runes.HARM);var knot=Knots.def(Knots.id(held,"" )).orElseThrow();var c=SpellCompiler.compile(List.of(knot));assertEquals(2,SpellNumbers.groupPower(c.root().groups.getFirst()));assertEquals(1,c.warnings().size());}
 @Test void separateGroupsCanEachHaveOneVow(){var c=SpellCompiler.compile(List.of(Runes.BOLT,Runes.VOW_MOD,Runes.HARM,Runes.BOLT,Runes.VOW_MOD,Runes.HARM));assertTrue(c.warnings().isEmpty());for(var g:c.root().groups)assertEquals(2,SpellNumbers.groupPower(g));}
 @Test void extendedDotHasThreePaidDurationSteps(){var c=stacked(Runes.TOUCH,Runes.BLEED,Runes.EXTEND,9,false);assertEquals(3,c.root().groups.getFirst().effects.getFirst().count(Runes.EXTEND));assertEquals(8,SpellNumbers.duration(c.root().groups.getFirst().effects.getFirst()));assertEquals(1,c.warnings().size());}
 @Test void amplifyStillScalesWithItsMatchingManaPrice(){var c=stacked(Runes.BOLT,Runes.HARM,Runes.AMPLIFY,4,false);assertEquals(Math.pow(1.5,4),SpellNumbers.power(c.root().groups.getFirst().effects.getFirst()),1e-9);assertTrue(c.warnings().isEmpty());}
}
