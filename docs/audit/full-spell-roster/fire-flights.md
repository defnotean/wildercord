# Authored fire projectile bodies

All twenty-eight built-in fire effects now have explicit moving flight recipes, following actual RuneBolt position and velocity. These replace the comet and duplicate server travel particles for fully covered groups; mixed groups with unsupported effects retain their fallback comet/travel hooks and add supported bodies. Travel voices remain. A shared coverage contract keeps server and client decisions aligned. Up to eight distinct bounded built-in IDs synchronize with the entity. An omission marker preserves fallback when members are foreign, oversized or beyond the limit. Authored emission follows the live entity independently of the comet particle; removed entity IDs retire independently. Pierce narrows and elongates the authored body. Reflection retains effect identity while the existing entity ownership and velocity change.

| Effect | Flight body |
| --- | --- |
| Ember | Coal body and three drifting sparks. |
| Fire | Broad flame nose with two licking tails. |
| Firestorm | Wind rails rotate around a carried flame jet. |
| Steam | Water bead leads expanding heated vapor. |
| Meteor | Angular stone nucleus sheds heated chips. |
| Soulfire | Hollow blue lantern flame trails split spectral tails. |
| Starfire | Turning four-point star surrounds incandescent fire. |
| Phoenix Pyre | Swept feather blades beat around a beak and living embers. |
| Flashfire | A flat heat shutter travels edge first. |
| Explode | Four pressure ribs contain a charged center. |
| Inferno | A five-tooth crown carries a furnace wake. |
| Primer | A dark charge carries a hooked pink fuse. |
| Kindling | Crossed wood splinters carry a small ignition point. |
| Sunscorch | A solar lens carries opposed rotating rays. |
| Blazecall | Three orbiting flame darts surround a calling needle. |
| Cinderbrand | An angular stamp retains a glowing score. |
| Ashen Veil | Split ash curtains roll behind an open seam. |
| Cinderheart | Pulse lobes shelter a forward heart point. |
| Searing Edge | A bright blade bevel aligns along travel. |
| Fireward | Shield shoulders meet a traveling lower point. |
| Smelt | A molten droplet travels through a hot grate. |
| Hellmouth | Burning jaw teeth flank a hollow dark throat. |
| Everburn | Fire follows a broken time fork. |
| Conflagration | Linked ignition fronts surround a stronger center. |
| Seethe | A heated water envelope escapes vapor at its sides. |
| Skyburst | A wind fork supports falling-fire teeth. |
| Cinder Bulwark | Stone courses travel with ember mortar. |
| Boiling Surge | A wet crest curls over a heated lip. |

## Scope

FireFlightTest casts each through a real paid Survival Cord in Full and Minimal settings; checks exact synchronized identity, moving entities, nearby authored five-tick material particles, bounded/distinct recipes, particle-clear recovery, removed-ID cleanup and rendered frames. Kindling first verifies foreign-innate refusal, then uses a matching innate for the paid cast. Captures include early discharge and later flight. Existing cue/preparation layers can still appear in captures; server travel voices, impact, mechanics and collision remain in charge. This does not complete fire release/impact choreography or other deliveries, all modifiers, fusion groups, linked/reflected choreography, remote multiplayer or shaders.

The final native run passes in 105 seconds. The proximity bound includes two emission ticks of travel plus one block for the body offset; network movement can arrive after the last emission. Pure coverage checks reject empty, foreign, unsupported, omission-marked and trailing-empty groups. All 28 later-flight frames were inspected through unscaled aim-region contact sheets; the original 57 screenshots remain unedited in the review package. Bodies read as compact forms at that range; close side/third-person beauty review remains open. Mixed metadata omission and Pierce behavior have code/recipe coverage, not native modifier/group cast coverage.
