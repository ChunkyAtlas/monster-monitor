package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the data associated with an NPC being tracked by the Monster Monitor plugin.
 * This class holds information such as the NPC's name, kill counts, kill limits,
 * and whether notifications should be sent when a kill limit is reached.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NpcData
{
    private String npcName; // Name of the NPC
    private int totalKillCount; // Total kills of this NPC
    private int killCountForLimit; // Progress towards the kill limit
    private int killLimit; // The set kill limit for this NPC
    private int deathAnimationId; // Death animation ID of the NPC
    private boolean limitSet; // Flag to track if the limit is set
    private boolean notifyOnLimit; // Flag to determine if notifications should be sent when the kill limit is reached

    /**
     * Constructor to create an NpcData object with just the NPC name and death animation ID.
     * Initializes other fields to default values.
     *
     * @param npcName the name of the NPC
     * @param deathAnimationId the ID of the death animation for the NPC
     */
    public NpcData(String npcName, int deathAnimationId)
    {
        this.npcName = npcName;
        this.deathAnimationId = deathAnimationId;
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
        this.killLimit = 0; // No limit by default
        this.limitSet = false; // Default to not set
        this.notifyOnLimit = false; // Default to not notify
    }

    /**
     * Increment the total kill count and the kill count towards the limit.
     * This method is typically called when the player kills the tracked NPC.
     */
    public void incrementKillCount()
    {
        this.totalKillCount++;
        this.killCountForLimit++;
    }

    /**
     * Reset the total kill count and progress towards the limit.
     * This method is used to reset the tracking data for the NPC, typically when the player
     * wants to restart the tracking from zero.
     */
    public void resetKillCount()
    {
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
    }

    /**
     * Reset only the progress towards the limit.
     * This method resets the kill count related to the limit while retaining the total kills.
     * Useful when the player wants to set a new limit but retain overall kill history.
     */
    public void resetKillCountForLimit()
    {
        this.killCountForLimit = 0;
    }
}