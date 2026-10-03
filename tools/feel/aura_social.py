"""Steel clashes, wooden spar counts, and restrained mentor ceremony tones."""
from feel.core import sa, event


def voice(index, family):
    def build(v, rng):
        dur = (0.28 + index * 0.035) if family == "clash" else (0.5 + index * 0.085)
        note = sa.note((sa.D, sa.E, sa.FS, sa.A, sa.B)[index % 5], 1 if family == "spar" else 2)
        if family == "clash":
            steel = sa.partials(note, dur, ((1, 1, 1), (2.41, 0.48, 0.45), (3.9, 0.22, 0.25)), 0.13 + index * 0.025)
            knock = sa.thump(240 + index * 32, 90, min(dur, 0.22), 0.018, drive=1.5, knock=0.4)
            x = sa.mix(0.65 * sa.norm(steel), 0.35 * sa.norm(knock))
        elif family == "spar":
            wood = sa.thump(150 + index * 18, 60, dur, 0.035, drive=1.2, knock=0.7)
            answer = sa.partials(note, dur, ((1, 1, 1), (2.7, 0.2, 0.3)), 0.25)
            x = sa.mix(0.6 * sa.norm(wood), (0.035, 0.35 * answer))
        else:
            first = sa.bell(note, dur, 0.7, 2, 0.6, attack=0.02)
            second = sa.bell(note * (1.5 if index % 2 == 0 else 0.75), dur, 0.6, 2, 0.65, attack=0.035)
            x = sa.mix(0.55 * first, (0.12 + index * 0.012, 0.35 * second))
        return sa.finish(sa.highpass(sa.reverb(x, 0.35, 0.10, damp=6500), 60), "ui" if index < 4 else "effect")
    return build


SOCIAL_EVENTS = []
for family, names in (
    ("clash", ("lock", "beat", "perfect", "good", "miss", "win", "even")),
    ("spar", ("salute", "ring", "count", "begin", "win", "out", "end")),
    ("lineage", ("ask", "bind", "seal", "lesson", "release", "graduate", "share")),
):
    for index, name in enumerate(names):
        SOCIAL_EVENTS.append(event(f"aura_{family}_{name}", voice(index, family), role="ui" if index < 4 else "effect", subtitle="hit" if family == "clash" else "tell", attenuation=24))
