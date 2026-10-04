# Observe the saved nursery feeding deadline

GitHub gameplay run `37176424603` passed the ordinary build and failed the fungal investigation at the repeat nursery observation. Its failure recorded a living visitor with dew and a completed second feeding cycle, but no settling pose at the later observation. The original diagnostic did not record its nursery deadline, so the historical cause is not proven.

The test waited an unconditional 1,205 ticks after gathering. Because part of the creature's existing feeding rest had already elapsed, that wait could span the second feeding and its short 80-tick nursery visit. A later observation could then encounter the independent nursery cooldown. The unchanged focused suite passed locally in 145 seconds, consistent with an intermittent observation problem.

The fixture now observes the original saved feeding deadline in five-tick steps, bounded by its existing 1,200-tick maximum. Before that deadline it verifies the exact feeding clock, absence of a renewed dew reserve and the prepared plant's immature state. It then observes the real second feeding and nursery admission using the existing 750-tick awaits. No creature deadline, motion, AI, search radius or reward is changed.

The revised native suite passed in 140 seconds. Actual observed world times were:

- Gathered visitor: time737, feeding deadline1568, nursery deadline1829, no dew.
- Feeding deadline reached: time1572, original clocks unchanged, no dew.
- Actual second feeding observed: time1677, new feeding deadline2875, old nursery deadline1829.
- Actual second nursery admission observed: time1922, independent nursery deadline3122, feeding deadline2875.

The same suite retains earned harvesting, native crafting, equipment wear, refusal, one-time rewards and full saved-world reopen checks. These local results do not establish that the complete GitHub gameplay descriptor is green, or prove which unrecorded nursery deadline occurred in the prior failed run.
