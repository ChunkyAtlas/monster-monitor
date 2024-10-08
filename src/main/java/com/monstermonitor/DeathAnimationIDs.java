package com.monstermonitor;

import java.util.HashSet;
import java.util.Set;

/**
 * The DeathAnimationIDs class stores the animation IDs associated with NPC death animations.
 * It provides a method to check if a given animation ID corresponds to a known death animation.
 */
public class DeathAnimationIDs
{
    // Constants representing the animation IDs for various NPC deaths
    public static final int CAVE_KRAKEN_DEATH = 3993;
    public static final int WIZARD_DEATH = 2553;
    public static final int GARGOYLE_DEATH = 1520;
    public static final int MARBLE_GARGOYLE_DEATH = 7813;
    public static final int LIZARD_DEATH = 2778;
    public static final int ROCKSLUG_DEATH = 1568;
    public static final int ZYGOMITE_DEATH = 3327;
    public static final int IMP_DEATH = 172;
    public static final int CORP_DEATH = 1676;
   public static final int DEATH = 836; // Any "MAN/WOMAN" character (includes guards/black knights, etc.)
    public static final int DWARF_DEATH = 102;
    public static final int ICEFIEND_DEATH = 1580;

    // Newly discovered death animations
    public static final int RAM_DEATH = 5336;
    public static final int COW_DEATH = 5851; // Includes Calf
    public static final int CHICKEN_DEATH = 5389;
    public static final int SKELETON_DEATH = 5491;
    public static final int SCORPION_DEATH = 6256;
    public static final int BLACK_BEAR_DEATH = 4929;
    public static final int GOBLIN_DEATH = 6182;
    public static final int DUCK_DEATH = 3468;
    public static final int RAT_DEATH = 2707;
    public static final int GOBLIN2_DEATH = 6190;
    public static final int GIANT_SPIDER_DEATH = 5329;
    public static final int GIANT_RAT_DEATH = 4935;
    public static final int FROG_DEATH = 1795;
    public static final int SPIDER_DEATH = 6251;

    // Set containing all the death animation IDs
    public static final Set<Integer> DEATH_ANIMATIONS = new HashSet<>();

    // Static block to initialize the DEATH_ANIMATIONS set with the animation IDs
    static {
        DEATH_ANIMATIONS.add(SPIDER_DEATH);
        DEATH_ANIMATIONS.add(GIANT_RAT_DEATH);
        DEATH_ANIMATIONS.add(FROG_DEATH);
        DEATH_ANIMATIONS.add(DUCK_DEATH);
        DEATH_ANIMATIONS.add(RAT_DEATH);
        DEATH_ANIMATIONS.add(GOBLIN2_DEATH);
        DEATH_ANIMATIONS.add(GIANT_SPIDER_DEATH);
        DEATH_ANIMATIONS.add(CAVE_KRAKEN_DEATH);
        DEATH_ANIMATIONS.add(WIZARD_DEATH);
        DEATH_ANIMATIONS.add(COW_DEATH);
        DEATH_ANIMATIONS.add(CHICKEN_DEATH);
        DEATH_ANIMATIONS.add(GARGOYLE_DEATH);
        DEATH_ANIMATIONS.add(MARBLE_GARGOYLE_DEATH);
        DEATH_ANIMATIONS.add(LIZARD_DEATH);
        DEATH_ANIMATIONS.add(ROCKSLUG_DEATH);
        DEATH_ANIMATIONS.add(ZYGOMITE_DEATH);
        DEATH_ANIMATIONS.add(IMP_DEATH);
        DEATH_ANIMATIONS.add(CORP_DEATH);
        DEATH_ANIMATIONS.add(DEATH);
        DEATH_ANIMATIONS.add(DWARF_DEATH);
        DEATH_ANIMATIONS.add(ICEFIEND_DEATH);
        DEATH_ANIMATIONS.add(RAM_DEATH);
        DEATH_ANIMATIONS.add(SKELETON_DEATH);
        DEATH_ANIMATIONS.add(SCORPION_DEATH);
        DEATH_ANIMATIONS.add(BLACK_BEAR_DEATH);
        DEATH_ANIMATIONS.add(GOBLIN_DEATH);
    }

    /**
     * Checks if the given animation ID corresponds to a known death animation.
     *
     * @param animationId the animation ID to check
     * @return true if the animation ID is a known death animation, false otherwise
     */
    public static boolean isDeathAnimation(int animationId)
    {
        return DEATH_ANIMATIONS.contains(animationId) && animationId != -1;
    }
}