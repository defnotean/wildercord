package dev.wildercord.gametest;

import dev.wildercord.spell.RuneDef;

/**
 * One fused rune for the showcase's gallery: cast as {@code shape} + {@code rune} at three husks (or on
 * the caster, for Self), filmed {@code ticks} after the cast, by night if {@code night}.
 */
public record FusedSample(RuneDef rune, RuneDef shape, int ticks, boolean night) {}
