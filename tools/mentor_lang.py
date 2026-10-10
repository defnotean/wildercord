"""0.13 mentoring: the words for /mentor. The rules live in src/main/java/dev/wildercord/guild/MentorRules.java."""

p = 'message.wildercord.mentor.'
LANG = {
    p + 'none': "You have no mentor and no apprentices. A mage of the %s circle or past it can take an apprentice who hasn't reached the %s with /mentor take <player>.",
    p + 'your_mentor': 'Your mentor: %s (%s)',
    p + 'near': 'close enough to teach: condensing counts +25%%',
    p + 'far': 'too far away to teach',
    p + 'your_apprentices': 'Your apprentices: %s (%s/%s)',
    p + 'offered': '%s offers to take you as their apprentice. The offer lasts a minute.',
    p + 'accept_button': '[Accept]',
    p + 'offer_sent': 'You offered to take %s as your apprentice.',
    p + 'taken_apprentice': '%s is now your mentor. Casting within %s blocks of them condenses a quarter more, until you form the %s circle.',
    p + 'taken_mentor': '%s is now your apprentice. Every circle they form pays you in Mana Crystals.',
    p + 'tutelage': '%s formed their %s circle: your tutelage earns you %s Mana Crystals.',
    p + 'graduated': 'You have graduated: %s has nothing left to teach you.',
    p + 'graduated_mentor': '%s has graduated and goes on alone.',
    p + 'ended': 'You ended your apprenticeship with %s.',
    p + 'ended_by': '%s ended your apprenticeship.',
    p + 'refuse.self': "You can't apprentice yourself.",
    p + 'refuse.mentor_too_young': 'Only a mage of the %1$s circle or past it can take an apprentice.',
    p + 'refuse.apprentice_too_far_along': 'An apprentice has to be taken before the %2$s circle.',
    p + 'refuse.already_apprenticed': 'They already have a mentor.',
    p + 'refuse.mentor_is_apprentice': 'You still have a mentor yourself.',
    p + 'refuse.full': 'A mentor takes at most %3$s apprentices.',
    p + 'refuse.no_offer': 'Nobody has offered to take you on, or the offer ran out.',
    p + 'refuse.mentor_gone': 'Whoever offered has left.',
    p + 'refuse.not_bonded': "You aren't apprenticed to anyone by that name.",
}


def write():
    pass
