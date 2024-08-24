package com.monstermonitor;

import java.util.HashSet;
import java.util.Set;

public class DeathAnimationIDs
{
    public static final int CAVE_KRAKEN_DEATH = 3993;
    public static final int WIZARD_DEATH = 2553;
    public static final int GARGOYLE_DEATH = 1520;
    public static final int MARBLE_GARGOYLE_DEATH = 7813;
    public static final int LIZARD_DEATH = 2778;
    public static final int ROCKSLUG_DEATH = 1568;
    public static final int ZYGOMITE_DEATH = 3327;
    public static final int IMP_DEATH = 172;
    public static final int CORP_DEATH = 1676;
    public static final int DEATH = 836;
    public static final int NEW_DEATH_ANIMATION_102 = 102;
    public static final int NEW_DEATH_ANIMATION_1580 = 1580;

    // Newly discovered death animations
    public static final int RAM_DEATH = 5336;
    public static final int SKELETON_DEATH = 5491;
    public static final int SCORPION_DEATH = 6256;
    public static final int BLACK_BEAR_DEATH = 4929;
    public static final int GOBLIN_DEATH = 6182;

    public static final Set<Integer> DEATH_ANIMATIONS = new HashSet<>();

    static {
        DEATH_ANIMATIONS.add(CAVE_KRAKEN_DEATH);
        DEATH_ANIMATIONS.add(WIZARD_DEATH);
        DEATH_ANIMATIONS.add(GARGOYLE_DEATH);
        DEATH_ANIMATIONS.add(MARBLE_GARGOYLE_DEATH);
        DEATH_ANIMATIONS.add(LIZARD_DEATH);
        DEATH_ANIMATIONS.add(ROCKSLUG_DEATH);
        DEATH_ANIMATIONS.add(ZYGOMITE_DEATH);
        DEATH_ANIMATIONS.add(IMP_DEATH);
        DEATH_ANIMATIONS.add(CORP_DEATH);
        DEATH_ANIMATIONS.add(DEATH);
        DEATH_ANIMATIONS.add(NEW_DEATH_ANIMATION_102);
        DEATH_ANIMATIONS.add(NEW_DEATH_ANIMATION_1580);
        DEATH_ANIMATIONS.add(RAM_DEATH);
        DEATH_ANIMATIONS.add(SKELETON_DEATH);
        DEATH_ANIMATIONS.add(SCORPION_DEATH);
        DEATH_ANIMATIONS.add(BLACK_BEAR_DEATH);
        DEATH_ANIMATIONS.add(GOBLIN_DEATH);
    }

    public static boolean isDeathAnimation(int animationId)
    {
        return DEATH_ANIMATIONS.contains(animationId) && animationId != -1;
    }
}
