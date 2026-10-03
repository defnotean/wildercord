# Authored fire projectile bodies

Eight effects now have explicit moving flight recipes, following actual RuneBolt position and velocity. These replace the comet for fully covered groups; mixed groups with unsupported effects retain their fallback comet and add supported bodies. Up to eight distinct bounded built-in IDs synchronize with the entity. Reflection retains effect identity while the existing entity ownership and velocity change.

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

## Scope

FireFlightTest casts each through a real paid Survival Cord in Full and Minimal settings; checks exact synchronized identity, moving entities, nearby actual material particles, bounded/distinct recipes and rendered frames. Captures include early discharge and later flight. Existing server travel voices/motes, impact, mechanics and collision remain in charge. This does not complete all fire effects, other deliveries, all modifiers, fusion groups, linked/reflected choreography, remote multiplayer or shaders.
