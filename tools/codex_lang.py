"""0.13 codex bestiary: the words for the field guide's kill tallies. The rules live in src/main/java/dev/wildercord/spell/CodexRules.java."""

s = 'screen.wildercord.grimoire.codex_'
m = 'message.wildercord.codex.'
LANG = {
    s + 'line': '%s  %s slain',
    s + 'unslain': 'Codex: not slain yet',
    s + 'slain': 'Codex: slain (%s)',
    s + 'studied': 'Codex: studied (%s slain)',
    s + 'mastered': 'Codex: mastered (%s slain)',
    s + 'next': '%s more to learn more',
    s + 'stats': 'Health %s hearts, armour %s, %s',
    s + 'bite': 'hits for %s hearts',
    s + 'harmless': 'harmless',
    m + 'studied': 'Codex: you have studied the %s (%s slain). The Grimoire now knows its strength.',
    m + 'mastered': 'Codex: you have mastered the %s (%s slain).',
}


def write():
    pass
