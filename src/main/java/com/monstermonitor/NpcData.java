package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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

    // Increment the total kill count and the kill count towards the limit
    public void incrementKillCount()
    {
        this.totalKillCount++;
        this.killCountForLimit++;
    }

    // Reset the total kill count and progress towards the limit
    public void resetKillCount()
    {
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
    }

    // Reset only the progress towards the limit
    public void resetKillCountForLimit()
    {
        this.killCountForLimit = 0;
    }
}
