package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the data associated with an NPC being tracked by Monster Monitor.
 * This class holds information such as the NPC's name, kill counts, kill limits,
 * whether the NPC is ignored (not logged), and whether notifications should be sent when a kill limit is reached.
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
    private boolean limitSet; // Flag to track if the limit is set
    private boolean notifyOnLimit; // Flag to determine if notifications should be sent when the kill limit is reached
    private boolean ignored; // Flag to indicate if the NPC is ignored (true = ignored, false = tracked)

    /**
     * Constructor to create an NpcData object with just the NPC name.
     * Initializes other fields to default values.
     *
     * @param npcName the name of the NPC
     */
    public NpcData(String npcName)
    {
        this.npcName = npcName;
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
        this.killLimit = 0; // No limit by default
        this.limitSet = false; // Default to not set
        this.notifyOnLimit = false; // Default to not notify
        this.ignored = false; // Default to not ignored (tracked)
    }

    /**
     * Increment the total kill count and the kill count towards the limit.
     * This method is typically called when the player kills the tracked NPC.
     * Ignores incrementing counts if the NPC is marked as ignored.
     */
    public void incrementKillCount()
    {
        if (!ignored) {
            this.totalKillCount++;
            this.killCountForLimit++;
        }
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

    /**
     * Checks if the NPC should be tracked in the overlay based on its ignored state.
     * This is useful for ensuring ignored NPCs don't appear in the monitoring section.
     *
     * @return true if the NPC should be tracked, false if it is ignored.
     */
    public boolean isTrackable()
    {
        return !ignored;
    }
}
